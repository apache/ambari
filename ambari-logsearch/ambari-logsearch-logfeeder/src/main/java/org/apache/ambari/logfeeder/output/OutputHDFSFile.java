/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ambari.logfeeder.output;

import org.apache.ambari.logfeeder.input.InputMarker;
import org.apache.ambari.logfeeder.output.spool.LogSpooler;
import org.apache.ambari.logfeeder.output.spool.LogSpoolerContext;
import org.apache.ambari.logfeeder.output.spool.RolloverCondition;
import org.apache.ambari.logfeeder.output.spool.RolloverHandler;
import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.ambari.logfeeder.util.LogfeederHDFSUtil;
import org.apache.ambari.logfeeder.util.PlaceholderUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.fs.FileSystem;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * An {@link Output} that records logs to HDFS.
 *
 * The events are spooled on the local file system and uploaded in batches asynchronously.
 */
public class OutputHDFSFile extends Output implements RolloverHandler, RolloverCondition {
  private static final Logger LOG = Logger.getLogger(OutputHDFSFile.class);
  
  private static final long DEFAULT_ROLLOVER_THRESHOLD_TIME_SECONDS = 5 * 60L;// 5 min by default

  private ConcurrentLinkedQueue<File> localReadyFiles = new ConcurrentLinkedQueue<File>();

  private final Object readyMonitor = new Object();

  private Thread hdfsCopyThread = null;

  private String filenamePrefix = "service-logs-";
  private long rolloverThresholdTimeMillis;

  private String hdfsOutDir = null;
  private String hdfsHost = null;
  private String hdfsPort = null;
  private FileSystem fileSystem = null;

  private LogSpooler logSpooler;

  @Override
  public void init() throws Exception {
    super.init();
    hdfsOutDir = getStringValue("hdfs_out_dir");
    hdfsHost = getStringValue("hdfs_host");
    hdfsPort = getStringValue("hdfs_port");
    long rolloverThresholdTimeSeconds = getLongValue("rollover_sec", DEFAULT_ROLLOVER_THRESHOLD_TIME_SECONDS);
    rolloverThresholdTimeMillis = rolloverThresholdTimeSeconds * 1000L;
    filenamePrefix = getStringValue("file_name_prefix", filenamePrefix);
    if (StringUtils.isEmpty(hdfsOutDir)) {
      LOG.error("HDFS config property <hdfs_out_dir> is not set in config file.");
      return;
    }
    if (StringUtils.isEmpty(hdfsHost)) {
      LOG.error("HDFS config property <hdfs_host> is not set in config file.");
      return;
    }
    if (StringUtils.isEmpty(hdfsPort)) {
      LOG.error("HDFS config property <hdfs_port> is not set in config file.");
      return;
    }
    HashMap<String, String> contextParam = buildContextParam();
    hdfsOutDir = PlaceholderUtil.replaceVariables(hdfsOutDir, contextParam);
    LOG.info("hdfs Output dir=" + hdfsOutDir);
    String localFileDir = LogFeederUtil.getLogfeederTempDir() + "hdfs/service/";
    logSpooler = new LogSpooler(localFileDir, filenamePrefix, this, this);
    this.startHDFSCopyThread();
  }

  @Override
  public void close() {
    LOG.info("Closing file." + getShortDescription());
    logSpooler.rollover();
    this.stopHDFSCopyThread();
    isClosed = true;
  }

  @Override
  public synchronized void write(String block, InputMarker inputMarker) throws Exception {
    if (block != null) {
      logSpooler.add(block);
      statMetric.value++;
    }
  }

  
  @Override
  public String getShortDescription() {
    return "output:destination=hdfs,hdfsOutDir=" + hdfsOutDir;
  }

  private void startHDFSCopyThread() {

    hdfsCopyThread = new Thread("hdfsCopyThread") {
      @Override
      public void run() {
        try {
          while (true) {
            Iterator<File> localFileIterator = localReadyFiles.iterator();
            while (localFileIterator.hasNext()) {
              File localFile = localFileIterator.next();
              fileSystem = LogfeederHDFSUtil.buildFileSystem(hdfsHost, hdfsPort);
              if (fileSystem != null && localFile.exists()) {
                String destFilePath = hdfsOutDir + "/" + localFile.getName();
                String localPath = localFile.getAbsolutePath();
                boolean overWrite = true;
                boolean delSrc = true;
                boolean isCopied = LogfeederHDFSUtil.copyFromLocal(localFile.getAbsolutePath(), destFilePath, fileSystem,
                    overWrite, delSrc);
                if (isCopied) {
                  LOG.debug("File copy to hdfs hdfspath :" + destFilePath + " and deleted local file :" + localPath);
                } else {
                  // TODO Need to write retry logic, in next release we can handle it
                  LOG.error("Hdfs file copy  failed for hdfspath :" + destFilePath + " and localpath :" + localPath);
                }
              }
              localFileIterator.remove();
            }
            try {
              // wait till new file comes in reayList
              synchronized (readyMonitor) {
                if (localReadyFiles.isEmpty()) {
                  readyMonitor.wait();
                }
              }
            } catch (InterruptedException e) {
              LOG.error(e.getLocalizedMessage(),e);
            }
          }
        } catch (Exception e) {
          LOG.error("Exception in hdfsCopyThread errorMsg:" + e.getLocalizedMessage(), e);
        }
      }
    };
    hdfsCopyThread.setDaemon(true);
    hdfsCopyThread.start();
  }

  private void stopHDFSCopyThread() {
    if (hdfsCopyThread != null) {
      LOG.info("waiting till copy all local files to hdfs.......");
      while (!localReadyFiles.isEmpty()) {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          LOG.error(e.getLocalizedMessage(), e);
        }
        LOG.debug("still waiting to copy all local files to hdfs.......");
      }
      LOG.info("calling interrupt method for hdfsCopyThread to stop it.");
      try {
        hdfsCopyThread.interrupt();
      } catch (SecurityException exception) {
        LOG.error(" Current thread : '" + Thread.currentThread().getName() +
            "' does not have permission to interrupt the Thread: '" + hdfsCopyThread.getName() + "'");
      }
      LogfeederHDFSUtil.closeFileSystem(fileSystem);
    }
  }

  private HashMap<String, String> buildContextParam() {
    HashMap<String, String> contextParam = new HashMap<String, String>();
    contextParam.put("host", LogFeederUtil.hostName);
    return contextParam;
  }

  private void addFileInReadyList(File localFile) {
    localReadyFiles.add(localFile);
    try {
      synchronized (readyMonitor) {
        readyMonitor.notifyAll();
      }
    } catch (Exception e) {
      LOG.error(e.getLocalizedMessage(),e);
    }
  }

  @Override
  public void copyFile(File inputFile, InputMarker inputMarker) throws UnsupportedOperationException {
    throw new UnsupportedOperationException("copyFile method is not yet supported for output=hdfs");
  }

  /**
   * Add the rollover file to a daemon thread for uploading to HDFS
   * @param rolloverFile the file to be uploaded to HDFS
   */
  @Override
  public void handleRollover(File rolloverFile) {
    addFileInReadyList(rolloverFile);
  }

  /**
   * Determines whether it is time to handleRollover the current spool file.
   *
   * The file will handleRollover if the time since creation of the file is more than
   * the timeout specified in rollover_sec configuration.
   * @param currentSpoolerContext {@link LogSpoolerContext} that holds state of active Spool file
   * @return true if time since creation is greater than value specified in rollover_sec,
   *          false otherwise.
   */
  @Override
  public boolean shouldRollover(LogSpoolerContext currentSpoolerContext) {
    long timeSinceCreation = new Date().getTime() - currentSpoolerContext.getActiveLogCreationTime().getTime();
    boolean shouldRollover = timeSinceCreation > rolloverThresholdTimeMillis;
    if (shouldRollover) {
      LOG.info("Detecting that time since file creation time " + currentSpoolerContext.getActiveLogCreationTime() +
          " has crossed threshold (msecs) " + rolloverThresholdTimeMillis);
    }
    return shouldRollover;
  }
}
