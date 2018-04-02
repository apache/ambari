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
package org.apache.ambari.logfeeder.input;

import com.google.common.annotations.VisibleForTesting;
import org.apache.ambari.logfeeder.conf.LogFeederProps;
import org.apache.ambari.logfeeder.input.monitor.CheckpointCleanupMonitor;
import org.apache.ambari.logfeeder.plugin.common.MetricData;
import org.apache.ambari.logfeeder.plugin.input.Input;
import org.apache.ambari.logfeeder.plugin.manager.InputManager;
import org.apache.ambari.logfeeder.util.FileUtil;
import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.common.util.Base64;

import javax.inject.Inject;
import java.io.EOFException;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class InputManagerImpl extends InputManager {

  private static final Logger LOG = Logger.getLogger(InputManagerImpl.class);

  private static final String CHECKPOINT_SUBFOLDER_NAME = "logfeeder_checkpoints";

  private Map<String, List<Input>> inputs = new HashMap<>();
  private Set<Input> notReadyList = new HashSet<>();

  private boolean isDrain = false;

  private String checkPointExtension;
  private File checkPointFolderFile;

  private MetricData filesCountMetric = new MetricData("input.files.count", true);

  private Thread inputIsReadyMonitor;

  @Inject
  private LogFeederProps logFeederProps;

  public List<Input> getInputList(String serviceName) {
    return inputs.get(serviceName);
  }

  @Override
  public void add(String serviceName, Input input) {
    List<Input> inputList = inputs.get(serviceName);
    if (inputList == null) {
      inputList = new ArrayList<>();
      inputs.put(serviceName, inputList);
    }
    inputList.add(input);
  }

  @Override
  public void removeInputsForService(String serviceName) {
    List<Input> inputList = inputs.get(serviceName);
    for (Input input : inputList) {
      input.setDrain(true);
    }
    for (Input input : inputList) {
      while (!input.isClosed()) {
        try { Thread.sleep(100); } catch (InterruptedException e) {}
      }
    }
    inputList.clear();
    inputs.remove(serviceName);
  }

  @Override
  public void removeInput(Input input) {
    LOG.info("Trying to remove from inputList. " + input.getShortDescription());
    for (List<Input> inputList : inputs.values()) {
      Iterator<Input> iter = inputList.iterator();
      while (iter.hasNext()) {
        Input iterInput = iter.next();
        if (iterInput.equals(input)) {
          LOG.info("Removing Input from inputList. " + input.getShortDescription());
          iter.remove();
        }
      }
    }
  }

  private int getActiveFilesCount() {
    int count = 0;
    for (List<Input> inputList : inputs.values()) {
      for (Input input : inputList) {
        if (input.isReady()) {
          count++;
        }
      }
    }
    return count;
  }

  @Override
  public void init() throws Exception {
    initCheckPointSettings();
    startMonitorThread();
  }

  private void initCheckPointSettings() {
    checkPointExtension = logFeederProps.getCheckPointExtension();
    LOG.info("Determining valid checkpoint folder");
    boolean isCheckPointFolderValid = false;
    // We need to keep track of the files we are reading.
    String checkPointFolder = logFeederProps.getCheckpointFolder();
    if (!StringUtils.isEmpty(checkPointFolder)) {
      checkPointFolderFile = new File(checkPointFolder);
      isCheckPointFolderValid = verifyCheckPointFolder(checkPointFolderFile);
    }

    if (!isCheckPointFolderValid) {
      // Let's use tmp folder
      checkPointFolderFile = new File(logFeederProps.getTmpDir(), CHECKPOINT_SUBFOLDER_NAME);
      LOG.info("Checking if tmp folder can be used for checkpoints. Folder=" + checkPointFolderFile);
      isCheckPointFolderValid = verifyCheckPointFolder(checkPointFolderFile);
      if (isCheckPointFolderValid) {
        LOG.warn("Using tmp folder " + checkPointFolderFile + " to store check points. This is not recommended." +
          "Please set logfeeder.checkpoint.folder property");
      }
    }

    if (isCheckPointFolderValid) {
      LOG.info("Using folder " + checkPointFolderFile + " for storing checkpoints");
      // check checkpoint cleanup every 2000 min
      Thread checkpointCleanupThread = new Thread(new CheckpointCleanupMonitor(this, 2000),"checkpoint_cleanup");
      checkpointCleanupThread.setDaemon(true);
      checkpointCleanupThread.start();
    } else {
      throw new IllegalStateException("Could not determine the checkpoint folder.");
    }
  }

  private void startMonitorThread() {
    inputIsReadyMonitor = new Thread("InputIsReadyMonitor") {
      @Override
      public void run() {
        LOG.info("Going to monitor for these missing files: " + notReadyList.toString());
        while (true) {
          if (isDrain) {
            LOG.info("Exiting missing file monitor.");
            break;
          }
          try {
            Iterator<Input> iter = notReadyList.iterator();
            while (iter.hasNext()) {
              Input input = iter.next();
              try {
                if (input.isReady()) {
                  input.monitor();
                  iter.remove();
                }
              } catch (Throwable t) {
                LOG.error("Error while enabling monitoring for input. " + input.getShortDescription());
              }
            }
            Thread.sleep(30 * 1000);
          } catch (Throwable t) {
            // Ignore
          }
        }
      }
    };

    inputIsReadyMonitor.start();
  }

  public void startInputs(String serviceName) {
    for (Input input : inputs.get(serviceName)) {
      try {
        input.init(logFeederProps);
        if (input.isReady()) {
          input.monitor();
        } else {
          LOG.info("Adding input to not ready list. Note, it is possible this component is not run on this host. " +
            "So it might not be an issue. " + input.getShortDescription());
          notReadyList.add(input);
        }
      } catch (Exception e) {
        LOG.error("Error initializing input. " + input.getShortDescription(), e);
      }
    }
  }

  private boolean verifyCheckPointFolder(File folderPathFile) {
    if (!folderPathFile.exists()) {
      try {
        if (!folderPathFile.mkdir()) {
          LOG.warn("Error creating folder for check point. folder=" + folderPathFile);
        }
      } catch (Throwable t) {
        LOG.warn("Error creating folder for check point. folder=" + folderPathFile, t);
      }
    }

    if (folderPathFile.exists() && folderPathFile.isDirectory()) {
      // Let's check whether we can create a file
      File testFile = new File(folderPathFile, UUID.randomUUID().toString());
      try {
        testFile.createNewFile();
        return testFile.delete();
      } catch (IOException e) {
        LOG.warn("Couldn't create test file in " + folderPathFile.getAbsolutePath() + " for checkPoint", e);
      }
    }
    return false;
  }

  public File getCheckPointFolderFile() {
    return checkPointFolderFile;
  }

  @Override
  public void addToNotReady(Input notReadyInput) {
    notReadyList.add(notReadyInput);
  }

  @Override
  public void addMetricsContainers(List<MetricData> metricsList) {
    for (List<Input> inputList : inputs.values()) {
      for (Input input : inputList) {
        input.addMetricsContainers(metricsList);
      }
    }
    filesCountMetric.value = getActiveFilesCount();
    metricsList.add(filesCountMetric);
  }

  public void logStats() {
    for (List<Input> inputList : inputs.values()) {
      for (Input input : inputList) {
        input.logStat();
      }
    }

    filesCountMetric.value = getActiveFilesCount();
    // TODO: logStatForMetric(filesCountMetric, "Stat: Files Monitored Count", "");
  }


  public void cleanCheckPointFiles() {
    if (checkPointFolderFile == null) {
      LOG.info("Will not clean checkPoint files. checkPointFolderFile=" + checkPointFolderFile);
      return;
    }
    LOG.info("Cleaning checkPoint files. checkPointFolderFile=" + checkPointFolderFile.getAbsolutePath());
    try {
      // Loop over the check point files and if filePath is not present, then move to closed
      String searchPath = "*" + checkPointExtension;
      FileFilter fileFilter = new WildcardFileFilter(searchPath);
      File[] checkPointFiles = checkPointFolderFile.listFiles(fileFilter);
      int totalCheckFilesDeleted = 0;
      for (File checkPointFile : checkPointFiles) {
        if (checkCheckPointFile(checkPointFile)) {
          totalCheckFilesDeleted++;
        }
      }
      LOG.info("Deleted " + totalCheckFilesDeleted + " checkPoint file(s). checkPointFolderFile=" +
        checkPointFolderFile.getAbsolutePath());

    } catch (Throwable t) {
      LOG.error("Error while cleaning checkPointFiles", t);
    }
  }

  private boolean checkCheckPointFile(File checkPointFile) {
    boolean deleted = false;
    try (RandomAccessFile checkPointReader = new RandomAccessFile(checkPointFile, "r")) {
      int contentSize = checkPointReader.readInt();
      byte b[] = new byte[contentSize];
      int readSize = checkPointReader.read(b, 0, contentSize);
      if (readSize != contentSize) {
        LOG.error("Couldn't read expected number of bytes from checkpoint file. expected=" + contentSize + ", read="
          + readSize + ", checkPointFile=" + checkPointFile);
      } else {
        String jsonCheckPointStr = new String(b, 0, readSize);
        Map<String, Object> jsonCheckPoint = LogFeederUtil.toJSONObject(jsonCheckPointStr);

        String logFilePath = (String) jsonCheckPoint.get("file_path");
        String logFileKey = (String) jsonCheckPoint.get("file_key");
        Integer maxAgeMin = null;
        if (jsonCheckPoint.containsKey("max_age_min")) {
          maxAgeMin = Integer.parseInt(jsonCheckPoint.get("max_age_min").toString());
        }
        if (logFilePath != null && logFileKey != null) {
          boolean deleteCheckPointFile = false;
          File logFile = new File(logFilePath);
          if (logFile.exists()) {
            Object fileKeyObj = FileUtil.getFileKey(logFile);
            String fileBase64 = Base64.byteArrayToBase64(fileKeyObj.toString().getBytes());
            if (!logFileKey.equals(fileBase64)) {
              LOG.info("CheckPoint clean: File key has changed. old=" + logFileKey + ", new=" + fileBase64 + ", filePath=" +
                logFilePath + ", checkPointFile=" + checkPointFile.getAbsolutePath());
              deleteCheckPointFile = !wasFileRenamed(logFile.getParentFile(), logFileKey);
            } else if (maxAgeMin != null && maxAgeMin != 0 && FileUtil.isFileTooOld(logFile, maxAgeMin)) {
              deleteCheckPointFile = true;
              LOG.info("Checkpoint clean: File reached max age minutes (" + maxAgeMin + "):" + logFilePath);
            }
          } else {
            LOG.info("CheckPoint clean: Log file doesn't exist. filePath=" + logFilePath + ", checkPointFile=" +
              checkPointFile.getAbsolutePath());
            deleteCheckPointFile = !wasFileRenamed(logFile.getParentFile(), logFileKey);
          }
          if (deleteCheckPointFile) {
            LOG.info("Deleting CheckPoint file=" + checkPointFile.getAbsolutePath() + ", logFile=" + logFilePath);
            checkPointFile.delete();
            deleted = true;
          }
        }
      }
    } catch (EOFException eof) {
      LOG.warn("Caught EOFException. Ignoring reading existing checkPoint file. " + checkPointFile);
    } catch (Throwable t) {
      LOG.error("Error while checking checkPoint file. " + checkPointFile, t);
    }

    return deleted;
  }

  private boolean wasFileRenamed(File folder, String searchFileBase64) {
    for (File file : folder.listFiles()) {
      Object fileKeyObj = FileUtil.getFileKey(file);
      String fileBase64 = Base64.byteArrayToBase64(fileKeyObj.toString().getBytes());
      if (searchFileBase64.equals(fileBase64)) {
        // even though the file name in the checkpoint file is different from the one it was renamed to, checkpoint files are
        // identified by their name, which is generated from the file key, which would be the same for the renamed file
        LOG.info("CheckPoint clean: File key matches file " + file.getAbsolutePath() + ", it must have been renamed");
        return true;
      }
    }
    return false;
  }

  public void waitOnAllInputs() {
    //wait on inputs
    for (List<Input> inputList : inputs.values()) {
      for (Input input : inputList) {
        if (input != null) {
          Thread inputThread = input.getThread();
          if (inputThread != null) {
            try {
              inputThread.join();
            } catch (InterruptedException e) {
              // ignore
            }
          }
        }
      }
    }
    // wait on monitor
    if (inputIsReadyMonitor != null) {
      try {
        this.close();
        inputIsReadyMonitor.join();
      } catch (InterruptedException e) {
        // ignore
      }
    }
  }

  public void checkInAll() {
    for (List<Input> inputList : inputs.values()) {
      for (Input input : inputList) {
        input.lastCheckIn();
      }
    }
  }

  public void close() {
    for (List<Input> inputList : inputs.values()) {
      for (Input input : inputList) {
        try {
          input.setDrain(true);
        } catch (Throwable t) {
          LOG.error("Error while draining. input=" + input.getShortDescription(), t);
        }
      }
    }
    isDrain = true;

    // Need to get this value from property
    int iterations = 30;
    int waitTimeMS = 1000;
    for (int i = 0; i < iterations; i++) {
      boolean allClosed = true;
      for (List<Input> inputList : inputs.values()) {
        for (Input input : inputList) {
          if (!input.isClosed()) {
            try {
              allClosed = false;
              LOG.warn("Waiting for input to close. " + input.getShortDescription() + ", " + (iterations - i) + " more seconds");
              Thread.sleep(waitTimeMS);
            } catch (Throwable t) {
              // Ignore
            }
          }
        }
      }
      if (allClosed) {
        LOG.info("All inputs are closed. Iterations=" + i);
        return;
      }
    }

    LOG.warn("Some inputs were not closed after " + iterations + " iterations");
    for (List<Input> inputList : inputs.values()) {
      for (Input input : inputList) {
        if (!input.isClosed()) {
          LOG.warn("Input not closed. Will ignore it." + input.getShortDescription());
        }
      }
    }
  }

  @VisibleForTesting
  public void setLogFeederProps(LogFeederProps logFeederProps) {
    this.logFeederProps = logFeederProps;
  }

  public LogFeederProps getLogFeederProps() {
    return logFeederProps;
  }


}
