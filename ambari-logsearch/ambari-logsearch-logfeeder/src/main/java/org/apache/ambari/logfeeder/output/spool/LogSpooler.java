/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.logfeeder.output.spool;

import com.google.common.annotations.VisibleForTesting;
import org.apache.ambari.logfeeder.util.DateUtil;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A class that manages local storage of log events before they are uploaded to the output destinations.
 *
 * This class should be used by any {@link org.apache.ambari.logfeeder.plugin.output.Output}s that wish to upload log files to an
 * output destination on a periodic batched basis. Log events should be added to an instance
 * of this class to be stored locally. This class determines when to
 * rollover using calls to an interface {@link RolloverCondition}. Likewise, it uses an interface
 * {@link RolloverHandler} to trigger the handling of the rolled over file.
 */
public class LogSpooler {
  
  private static final Logger LOG = Logger.getLogger(LogSpooler.class);
  public static final long TIME_BASED_ROLLOVER_DISABLED_THRESHOLD = 0;
  static final String fileDateFormat = "yyyy-MM-dd-HH-mm-ss";

  private String spoolDirectory;
  private String sourceFileNamePrefix;
  private RolloverCondition rolloverCondition;
  private RolloverHandler rolloverHandler;
  private PrintWriter currentSpoolBufferedWriter;
  private File currentSpoolFile;
  private LogSpoolerContext currentSpoolerContext;
  private Timer rolloverTimer;
  private AtomicBoolean rolloverInProgress = new AtomicBoolean(false);

  /**
   * Create an instance of the LogSpooler.
   * @param spoolDirectory The directory under which spooler files are created.
   *                       Should be unique per instance of {@link org.apache.ambari.logfeeder.plugin.output.Output}
   * @param sourceFileNamePrefix The prefix with which the locally spooled files are created.
   * @param rolloverCondition An object of type {@link RolloverCondition} that will be used to
   *                          determine when to rollover.
   * @param rolloverHandler An object of type {@link RolloverHandler} that will be called when
   *                        there should be a rollover.
   */
  public LogSpooler(String spoolDirectory, String sourceFileNamePrefix, RolloverCondition rolloverCondition,
                    RolloverHandler rolloverHandler) {
    this(spoolDirectory, sourceFileNamePrefix, rolloverCondition, rolloverHandler,
        TIME_BASED_ROLLOVER_DISABLED_THRESHOLD);
  }

  /**
   * Create an instance of the LogSpooler.
   * @param spoolDirectory The directory under which spooler files are created.
   *                       Should be unique per instance of {@link org.apache.ambari.logfeeder.plugin.output.Output}
   * @param sourceFileNamePrefix The prefix with which the locally spooled files are created.
   * @param rolloverCondition An object of type {@link RolloverCondition} that will be used to
   *                          determine when to rollover.
   * @param rolloverHandler An object of type {@link RolloverHandler} that will be called when
   *                        there should be a rollover.
   * @param rolloverTimeThresholdSecs  Setting a non-zero value enables time based rollover of
   *                                   spool files. Sending a 0 value disables this functionality.
   */
  public LogSpooler(String spoolDirectory, String sourceFileNamePrefix, RolloverCondition rolloverCondition,
                    RolloverHandler rolloverHandler, long rolloverTimeThresholdSecs) {
    this.spoolDirectory = spoolDirectory;
    this.sourceFileNamePrefix = sourceFileNamePrefix;
    this.rolloverCondition = rolloverCondition;
    this.rolloverHandler = rolloverHandler;
    if (rolloverTimeThresholdSecs != TIME_BASED_ROLLOVER_DISABLED_THRESHOLD) {
      rolloverTimer = new Timer("log-spooler-timer-" + sourceFileNamePrefix, true);
      rolloverTimer.scheduleAtFixedRate(new LogSpoolerRolloverTimerTask(),
          rolloverTimeThresholdSecs*1000, rolloverTimeThresholdSecs*1000);
    }
    initializeSpoolState();
  }

  private void initializeSpoolDirectory() {
    File spoolDir = new File(spoolDirectory);
    if (!spoolDir.exists()) {
      LOG.info("Creating spool directory: " + spoolDir);
      boolean result = spoolDir.mkdirs();
      if (!result) {
        throw new LogSpoolerException("Could not create spool directory: " + spoolDirectory);
      }
    }
  }

  private void initializeSpoolState() {
    initializeSpoolDirectory();
    currentSpoolFile = initializeSpoolFile();
    try {
      currentSpoolBufferedWriter = initializeSpoolWriter(currentSpoolFile);
    } catch (IOException e) {
      throw new LogSpoolerException("Could not create buffered writer for spool file: " + currentSpoolFile
          + ", error message: " + e.getLocalizedMessage(), e);
    }
    currentSpoolerContext = new LogSpoolerContext(currentSpoolFile);
    LOG.info("Initialized spool file at path: " + currentSpoolFile);
  }

  @VisibleForTesting
  protected File initializeSpoolFile() {
    return new File(spoolDirectory, getCurrentFileName());
  }

  @VisibleForTesting
  protected PrintWriter initializeSpoolWriter(File spoolFile) throws IOException {
    return new PrintWriter(new BufferedWriter(new FileWriter(spoolFile)));
  }

  /**
   * Add an event for spooling.
   *
   * This method adds the event to the current spool file's buffer. On completion, it
   * calls the {@link RolloverCondition#shouldRollover(LogSpoolerContext)} method to determine if
   * it is ready to rollover the file.
   * @param logEvent The log event to spool.
   */
  public synchronized void add(String logEvent) {
    currentSpoolBufferedWriter.println(logEvent);
    currentSpoolerContext.logEventSpooled();
    if (rolloverCondition.shouldRollover(currentSpoolerContext)) {
      LOG.info("Trying to rollover based on rollover condition");
      tryRollover();
    }
  }

  /**
   * Trigger a rollover of the current spool file.
   *
   * This method manages the rollover of the spool file, and then invokes the
   * {@link RolloverHandler#handleRollover(File)} to handle what should be done with the
   * rolled over file.
   */
  public void rollover() {
    LOG.info("Rollover condition detected, rolling over file: " + currentSpoolFile);
    currentSpoolBufferedWriter.flush();
    if (currentSpoolFile.length()==0) {
      LOG.info("No data in file " + currentSpoolFile + ", not doing rollover");
    } else {
      currentSpoolBufferedWriter.close();
      rolloverHandler.handleRollover(currentSpoolFile);
      LOG.info("Invoked rollover handler with file: " + currentSpoolFile);
      initializeSpoolState();
    }
    boolean status = rolloverInProgress.compareAndSet(true, false);
    if (!status) {
      LOG.error("Should have reset rollover flag!!");
    }
  }

  private synchronized void tryRollover() {
    if (rolloverInProgress.compareAndSet(false, true)) {
      rollover();
    } else {
      LOG.warn("Ignoring rollover call as rollover already in progress for file " +
          currentSpoolFile);
    }
  }

  private String getCurrentFileName() {
    Date currentDate = new Date();
    String dateStr = DateUtil.dateToString(currentDate, fileDateFormat);
    return sourceFileNamePrefix + dateStr;
  }

  /**
   * Cancel's any time based rollover task, if started.
   */
  public void close() {
    if (rolloverTimer != null) {
      rolloverTimer.cancel();
    }
  }

  private class LogSpoolerRolloverTimerTask extends TimerTask {
    @Override
    public void run() {
      LOG.info("Trying rollover based on time");
      tryRollover();
    }
  }
}
