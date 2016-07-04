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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.ambari.logfeeder.LogFeederUtil;
import org.apache.ambari.logfeeder.input.InputMarker;
import org.apache.ambari.logfeeder.util.DateUtil;
import org.apache.ambari.logfeeder.util.LogfeederHDFSUtil;
import org.apache.ambari.logfeeder.util.PlaceholderUtil;
import org.apache.hadoop.fs.FileSystem;
import org.apache.log4j.Logger;

public class OutputHDFSFile extends Output {
  private final static Logger logger = Logger.getLogger(OutputHDFSFile.class);

  private ConcurrentLinkedQueue<File> localReadyFiles = new ConcurrentLinkedQueue<File>();

  private final Object readyMonitor = new Object();

  private Thread hdfsCopyThread = null;

  private PrintWriter outWriter = null;
  // local writer variables
  private String localFilePath = null;
  private String filenamePrefix = "service-logs-";
  private String localFileDir = null;
  private File localcurrentFile = null;
  private Date localFileCreateTime = null;
  private long localFileRolloverSec = 5 * 1 * 60;// 5 min by default

  private String hdfsOutDir = null;
  private String hdfsHost = null;
  private String hdfsPort = null;
  private FileSystem fileSystem = null;

  private String fileDateFormat = "yyyy-MM-dd-HH-mm-ss";

  @Override
  public void init() throws Exception {
    super.init();
    hdfsOutDir = getStringValue("hdfs_out_dir");
    hdfsHost = getStringValue("hdfs_host");
    hdfsPort = getStringValue("hdfs_port");
    localFileRolloverSec = getLongValue("rollover_sec", localFileRolloverSec);
    filenamePrefix = getStringValue("file_name_prefix", filenamePrefix);
    if (hdfsOutDir == null || hdfsOutDir.isEmpty()) {
      logger
          .error("Filepath config property <path> is not set in config file.");
      return;
    }
    HashMap<String, String> contextParam = buildContextParam();
    hdfsOutDir = PlaceholderUtil.replaceVariables(hdfsOutDir, contextParam);
    logger.info("hdfs Output dir=" + hdfsOutDir);
    localFileDir = LogFeederUtil.getLogfeederTempDir() + "hdfs/service/";
    localFilePath = localFileDir;
    this.startHDFSCopyThread();
  }

  @Override
  public void close() {
    logger.info("Closing file." + getShortDescription());
    if (outWriter != null) {
      try {
        outWriter.flush();
        outWriter.close();
        addFileInReadyList(localcurrentFile);
      } catch (Throwable t) {
        // Ignore this exception
      }
    }
    this.stopHDFSCopyThread();
    isClosed = true;
  }

  @Override
  synchronized public void write(String block, InputMarker inputMarker)
      throws Exception {
    if (block != null) {
      buildOutWriter();
      if (outWriter != null) {
        statMetric.count++;
        outWriter.println(block);
        closeFileIfNeeded();
      }
    }
  }

  
  @Override
  public String getShortDescription() {
    return "output:destination=hdfs,hdfsOutDir=" + hdfsOutDir;
  }

  private synchronized void closeFileIfNeeded() throws FileNotFoundException,
      IOException {
    if (outWriter == null) {
      return;
    }
    // TODO: Close the file on absolute time. Currently it is implemented as
    // relative time
    if (System.currentTimeMillis() - localFileCreateTime.getTime() > localFileRolloverSec * 1000) {
      logger.info("Closing file. Rolling over. name="
          + localcurrentFile.getName() + ", filePath="
          + localcurrentFile.getAbsolutePath());
      try {
        outWriter.flush();
        outWriter.close();
        addFileInReadyList(localcurrentFile);
      } catch (Throwable t) {
        logger
            .error("Error on closing output writter. Exception will be ignored. name="
                + localcurrentFile.getName()
                + ", filePath="
                + localcurrentFile.getAbsolutePath());
      }

      outWriter = null;
      localcurrentFile = null;
    }
  }

  public synchronized void buildOutWriter() {
    if (outWriter == null) {
      String currentFilePath = localFilePath + getCurrentFileName();
      localcurrentFile = new File(currentFilePath);
      if (localcurrentFile.getParentFile() != null) {
        File parentDir = localcurrentFile.getParentFile();
        if (!parentDir.isDirectory()) {
          parentDir.mkdirs();
        }
      }
      try {
        outWriter = new PrintWriter(new BufferedWriter(new FileWriter(
            localcurrentFile, true)));
      } catch (IOException e) {
        logger.error("= OutputHDFSFile.buidOutWriter failed for file :  "
            + localcurrentFile.getAbsolutePath() + " Desc: "
            + getShortDescription() + " errorMsg: " + e.getLocalizedMessage(),
            e);
      }
      localFileCreateTime = new Date();
      logger.info("Create file is successful. localFilePath="
          + localcurrentFile.getAbsolutePath());
    }
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
              fileSystem = LogfeederHDFSUtil.INSTANCE.buildFileSystem(hdfsHost,
                  hdfsPort);
              if (fileSystem != null && localFile.exists()) {
                String destFilePath = hdfsOutDir + "/" + localFile.getName();
                String localPath = localFile.getAbsolutePath();
                boolean overWrite = true;
                boolean delSrc = true;
                boolean isCopied = LogfeederHDFSUtil.INSTANCE.copyFromLocal(
                    localFile.getAbsolutePath(), destFilePath, fileSystem,
                    overWrite, delSrc);
                if (isCopied) {
                  logger.debug("File copy to hdfs hdfspath :" + destFilePath
                      + " and deleted local file :" + localPath);
                } else {
                  // TODO Need to write retry logic, in next release we can
                  // handle it
                  logger.error("Hdfs file copy  failed for hdfspath :"
                      + destFilePath + " and localpath :" + localPath);
                }

              }

            }
            try {
              // wait till new file comes in reayList
              synchronized (readyMonitor) {
                if (localReadyFiles.size() == 0) {
                  readyMonitor.wait();
                }
              }
            } catch (InterruptedException e) {
              // ignore
            }
          }
        } catch (Exception e) {
          logger
              .error(
                  "Exception in hdfsCopyThread errorMsg:"
                      + e.getLocalizedMessage(), e);
        }
      }
    };
    hdfsCopyThread.setDaemon(true);
    hdfsCopyThread.start();
  }

  private void stopHDFSCopyThread() {
    if (hdfsCopyThread != null) {
      logger.info("waiting till copy all local files to hdfs.......");
      while (localReadyFiles.size() != 0) {

      }
      logger.info("calling interrupt method for hdfsCopyThread to stop it.");
      hdfsCopyThread.interrupt();
      LogfeederHDFSUtil.INSTANCE.closeFileSystem(fileSystem);
    }
  }

  public String getCurrentFileName() {
    Date currentDate = new Date();
    String dateStr = DateUtil.dateToString(currentDate, fileDateFormat);
    String fileName = filenamePrefix + dateStr;
    return fileName;
  }

  public HashMap<String, String> buildContextParam() {
    HashMap<String, String> contextParam = new HashMap<String, String>();
    contextParam.put("host", LogFeederUtil.hostName);
    return contextParam;
  }

  public void addFileInReadyList(File localFile) {
    localReadyFiles.add(localFile);
    try {
      synchronized (readyMonitor) {
        readyMonitor.notifyAll();
      }
    } catch (Exception exception) {
      // ignore
    }
  }

  @Override
  public void copyFile(File inputFile, InputMarker inputMarker)
      throws UnsupportedOperationException {
    throw new UnsupportedOperationException(
        "copyFile method is not yet supported for output=hdfs");     
  }
}
