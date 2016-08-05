/**
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
import org.apache.ambari.logfeeder.output.Output;
import org.apache.ambari.logfeeder.util.DateUtil;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.Date;

/**
 * A class that manages local storage of log events before they are uploaded to the output destinations.
 *
 * This class should be used by any {@link Output}s that wish to upload log files to an
 * output destination on a periodic batched basis. Log events should be added to an instance
 * of this class to be stored locally. This class determines when to
 * rollover using calls to an interface {@link RolloverCondition}. Likewise, it uses an interface
 * {@link RolloverHandler} to trigger the handling of the rolled over file.
 */
public class LogSpooler {
  static private Logger logger = Logger.getLogger(LogSpooler.class);
  static final String fileDateFormat = "yyyy-MM-dd-HH-mm-ss";

  private String spoolDirectory;
  private String sourceFileNamePrefix;
  private RolloverCondition rolloverCondition;
  private RolloverHandler rolloverHandler;
  private PrintWriter currentSpoolBufferedWriter;
  private File currentSpoolFile;
  private LogSpoolerContext currentSpoolerContext;

  /**
   * Create an instance of the LogSpooler.
   * @param spoolDirectory The directory under which spooler files are created.
   *                       Should be unique per instance of {@link Output}
   * @param sourceFileNamePrefix The prefix with which the locally spooled files are created.
   * @param rolloverCondition An object of type {@link RolloverCondition} that will be used to
   *                          determine when to rollover.
   * @param rolloverHandler An object of type {@link RolloverHandler} that will be called when
   *                        there should be a rollover.
   */
  public LogSpooler(String spoolDirectory, String sourceFileNamePrefix, RolloverCondition rolloverCondition,
                    RolloverHandler rolloverHandler) {
    this.spoolDirectory = spoolDirectory;
    this.sourceFileNamePrefix = sourceFileNamePrefix;
    this.rolloverCondition = rolloverCondition;
    this.rolloverHandler = rolloverHandler;
    initializeSpoolFile();
  }

  private void initializeSpoolDirectory() {
    File spoolDir = new File(spoolDirectory);
    if (!spoolDir.exists()) {
      logger.info("Creating spool directory: " + spoolDir);
      boolean result = spoolDir.mkdirs();
      if (!result) {
        throw new LogSpoolerException("Could not create spool directory: " + spoolDirectory);
      }
    }
  }

  private void initializeSpoolFile() {
    initializeSpoolDirectory();
    currentSpoolFile = new File(spoolDirectory, getCurrentFileName());
    try {
      currentSpoolBufferedWriter = initializeSpoolWriter(currentSpoolFile);
    } catch (IOException e) {
      throw new LogSpoolerException("Could not create buffered writer for spool file: " + currentSpoolFile
          + ", error message: " + e.getLocalizedMessage(), e);
    }
    currentSpoolerContext = new LogSpoolerContext(currentSpoolFile);
    logger.info("Initialized spool file at path: " + currentSpoolFile.getAbsolutePath());
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
  public void add(String logEvent) {
    currentSpoolBufferedWriter.println(logEvent);
    currentSpoolerContext.logEventSpooled();
    if (rolloverCondition.shouldRollover(currentSpoolerContext)) {
      rollover();
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
    logger.info("Rollover condition detected, rolling over file: " + currentSpoolFile);
    currentSpoolBufferedWriter.flush();
    currentSpoolBufferedWriter.close();
    rolloverHandler.handleRollover(currentSpoolFile);
    logger.info("Invoked rollover handler with file: " + currentSpoolFile);
    initializeSpoolFile();
  }

  @VisibleForTesting
  protected String getCurrentFileName() {
    Date currentDate = new Date();
    String dateStr = DateUtil.dateToString(currentDate, fileDateFormat);
    return sourceFileNamePrefix + dateStr;
  }

}
