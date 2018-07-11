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

import org.apache.ambari.logfeeder.conf.LogEntryCacheConfig;
import org.apache.ambari.logfeeder.conf.LogFeederProps;
import org.apache.ambari.logfeeder.input.monitor.LogFileDetachMonitor;
import org.apache.ambari.logfeeder.input.monitor.LogFilePathUpdateMonitor;
import org.apache.ambari.logfeeder.input.reader.LogsearchReaderFactory;
import org.apache.ambari.logfeeder.input.file.FileCheckInHelper;
import org.apache.ambari.logfeeder.input.file.ProcessFileHelper;
import org.apache.ambari.logfeeder.input.file.ResumeLineNumberHelper;
import org.apache.ambari.logfeeder.plugin.filter.Filter;
import org.apache.ambari.logfeeder.plugin.input.Input;
import org.apache.ambari.logfeeder.util.FileUtil;
import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.ambari.logsearch.config.api.model.inputconfig.InputFileBaseDescriptor;
import org.apache.ambari.logsearch.config.api.model.inputconfig.InputFileDescriptor;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.util.*;

public class InputFile extends Input<LogFeederProps, InputFileMarker> {

  private static final Logger LOG = LoggerFactory.getLogger(InputFile.class);

  private static final boolean DEFAULT_TAIL = true;
  private static final boolean DEFAULT_USE_EVENT_MD5 = false;
  private static final boolean DEFAULT_GEN_EVENT_MD5 = true;
  private static final int DEFAULT_CHECKPOINT_INTERVAL_MS = 5 * 1000;

  private static final int DEFAULT_DETACH_INTERVAL_MIN = 300;
  private static final int DEFAULT_DETACH_TIME_MIN = 2000;
  private static final int DEFAULT_LOG_PATH_UPDATE_INTERVAL_MIN = 5;

  private boolean isReady;

  private boolean tail;

  private String filePath;
  private File[] logFiles;
  private String logPath;
  private Object fileKey;
  private String base64FileKey;
  private String checkPointExtension;
  private int checkPointIntervalMS;
  private int detachIntervalMin;
  private int detachTimeMin;
  private int pathUpdateIntervalMin;
  private Integer maxAgeMin;

  private Map<String, File> checkPointFiles = new HashMap<>();
  private Map<String, Long> lastCheckPointTimeMSs = new HashMap<>();
  private Map<String, Map<String, Object>> jsonCheckPoints = new HashMap<>();
  private Map<String, InputFileMarker> lastCheckPointInputMarkers = new HashMap<>();

  private Thread thread;
  private Thread logFileDetacherThread;
  private Thread logFilePathUpdaterThread;
  private ThreadGroup threadGroup;

  private boolean multiFolder = false;
  private Map<String, List<File>> folderMap;
  private Map<String, InputFile> inputChildMap = new HashMap<>();

  @Override
  public boolean isReady() {
    if (!isReady) {
      // Let's try to check whether the file is available
      logFiles = getActualInputLogFiles();
      Map<String, List<File>> foldersMap = FileUtil.getFoldersForFiles(logFiles);
      setFolderMap(foldersMap);
      if (!ArrayUtils.isEmpty(logFiles) && logFiles[0].isFile()) {
        if (tail && logFiles.length > 1) {
          LOG.warn("Found multiple files (" + logFiles.length + ") for the file filter " + filePath +
            ". Will follow only the first one. Using " + logFiles[0].getAbsolutePath());
        }
        LOG.info("File filter " + filePath + " expanded to " + logFiles[0].getAbsolutePath());
        isReady = true;
      } else {
        LOG.debug(logPath + " file doesn't exist. Ignoring for now");
      }
    }
    return isReady;
  }

  @Override
  public void setReady(boolean isReady) {
    this.isReady = isReady;
  }

  @Override
  public String getNameForThread() {
    if (filePath != null) {
      try {
        return (getType() + "=" + (new File(filePath)).getName());
      } catch (Throwable ex) {
        LOG.warn("Couldn't get basename for filePath=" + filePath, ex);
      }
    }
    return super.getNameForThread() + ":" + getType();
  }

  @Override
  public synchronized void checkIn(InputFileMarker inputMarker) {
    FileCheckInHelper.checkIn(this, inputMarker);
  }

  @Override
  public void lastCheckIn() {
    for (InputFileMarker lastCheckPointInputMarker : lastCheckPointInputMarkers.values()) {
      checkIn(lastCheckPointInputMarker);
    }
  }

  @Override
  public String getStatMetricName() {
    return "input.files.read_lines";
  }

  @Override
  public String getReadBytesMetricName() {
    return "input.files.read_bytes";
  }

  @Override
  public boolean monitor() {
    if (isReady()) {
      if (multiFolder) {
        try {
          threadGroup = new ThreadGroup(getNameForThread());
          if (getFolderMap() != null) {
            for (Map.Entry<String, List<File>> folderFileEntry : getFolderMap().entrySet()) {
              startNewChildInputFileThread(folderFileEntry);
            }
            logFilePathUpdaterThread = new Thread(new LogFilePathUpdateMonitor((InputFile) this, pathUpdateIntervalMin, detachTimeMin), "logfile_path_updater=" + filePath);
            logFilePathUpdaterThread.setDaemon(true);
            logFileDetacherThread = new Thread(new LogFileDetachMonitor((InputFile) this, detachIntervalMin, detachTimeMin), "logfile_detacher=" + filePath);
            logFileDetacherThread.setDaemon(true);

            logFilePathUpdaterThread.start();
            logFileDetacherThread.start();
          }
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      } else {
        LOG.info("Starting thread. " + getShortDescription());
        thread = new Thread(this, getNameForThread());
        thread.start();
      }
      return true;
    } else {
      return false;
    }
  }

  @Override
  public InputFileMarker getInputMarker() {
    return null;
  }

  @Override
  public void init(LogFeederProps logFeederProps) throws Exception {
    super.init(logFeederProps);
    LOG.info("init() called");

    checkPointExtension = logFeederProps.getCheckPointExtension();

    // Let's close the file and set it to true after we start monitoring it
    setClosed(true);
    logPath = getInputDescriptor().getPath();
    checkPointIntervalMS = (int) ObjectUtils.defaultIfNull(((InputFileBaseDescriptor)getInputDescriptor()).getCheckpointIntervalMs(), DEFAULT_CHECKPOINT_INTERVAL_MS);
    detachIntervalMin = (int) ObjectUtils.defaultIfNull(((InputFileDescriptor)getInputDescriptor()).getDetachIntervalMin(), DEFAULT_DETACH_INTERVAL_MIN * 60);
    detachTimeMin = (int) ObjectUtils.defaultIfNull(((InputFileDescriptor)getInputDescriptor()).getDetachTimeMin(), DEFAULT_DETACH_TIME_MIN * 60);
    pathUpdateIntervalMin = (int) ObjectUtils.defaultIfNull(((InputFileDescriptor)getInputDescriptor()).getPathUpdateIntervalMin(), DEFAULT_LOG_PATH_UPDATE_INTERVAL_MIN * 60);
    maxAgeMin = (int) ObjectUtils.defaultIfNull(((InputFileDescriptor)getInputDescriptor()).getMaxAgeMin(), 0);
    boolean initDefaultFields = BooleanUtils.toBooleanDefaultIfNull(getInputDescriptor().isInitDefaultFields(), false);
    setInitDefaultFields(initDefaultFields);
    if (StringUtils.isEmpty(logPath)) {
      LOG.error("path is empty for file input. " + getShortDescription());
      return;
    }

    setFilePath(logPath);
    // Check there can have pattern in folder
    if (getFilePath() != null && getFilePath().contains("/")) {
      int lastIndexOfSlash = getFilePath().lastIndexOf("/");
      String folderBeforeLogName = getFilePath().substring(0, lastIndexOfSlash);
      if (folderBeforeLogName.contains("*")) {
        LOG.info("Found regex in folder path ('" + getFilePath() + "'), will check against multiple folders.");
        setMultiFolder(true);
      }
    }
    boolean isFileReady = isReady();
    LOG.info("File to monitor " + logPath + ", tail=" + tail + ", isReady=" + isFileReady);

    LogEntryCacheConfig cacheConfig = logFeederProps.getLogEntryCacheConfig();
    initCache(
      cacheConfig.isCacheEnabled(),
      cacheConfig.getCacheKeyField(),
      cacheConfig.getCacheSize(),
      cacheConfig.isCacheLastDedupEnabled(),
      cacheConfig.getCacheDedupInterval(),
      getFilePath());

    tail = BooleanUtils.toBooleanDefaultIfNull(getInputDescriptor().isTail(), DEFAULT_TAIL);
    setUseEventMD5(BooleanUtils.toBooleanDefaultIfNull(getInputDescriptor().isUseEventMd5AsId(), DEFAULT_USE_EVENT_MD5));
    setGenEventMD5(BooleanUtils.toBooleanDefaultIfNull(getInputDescriptor().isGenEventMd5(), DEFAULT_GEN_EVENT_MD5));
  }

  @Override
  public void start() throws Exception {
    boolean isProcessFile = BooleanUtils.toBooleanDefaultIfNull(((InputFileDescriptor)getInputDescriptor()).getProcessFile(), true);
    if (isProcessFile) {
      for (int i = logFiles.length - 1; i >= 0; i--) {
        File file = logFiles[i];
        if (i == 0 || !tail) {
          try {
            processFile(file, i == 0);
            if (isClosed() || isDrain()) {
              LOG.info("isClosed or isDrain. Now breaking loop.");
              break;
            }
          } catch (Throwable t) {
            LOG.error("Error processing file=" + file.getAbsolutePath(), t);
          }
        }
      }
      close();
    } else {
      copyFiles(logFiles);
    }
  }

  public int getResumeFromLineNumber() {
    return ResumeLineNumberHelper.getResumeFromLineNumber(this);
  }

  public void processFile(File logPathFile, boolean follow) throws Exception {
    ProcessFileHelper.processFile(this, logPathFile, follow);
  }

  public BufferedReader openLogFile(File logFile) throws Exception {
    BufferedReader br = new BufferedReader(LogsearchReaderFactory.INSTANCE.getReader(logFile));
    fileKey = getFileKeyFromLogFile(logFile);
    base64FileKey = Base64.byteArrayToBase64(fileKey.toString().getBytes());
    LOG.info("fileKey=" + fileKey + ", base64=" + base64FileKey + ". " + getShortDescription());
    return br;
  }

  public Object getFileKeyFromLogFile(File logFile) {
    return FileUtil.getFileKey(logFile);
  }

  private void copyFiles(File[] files) {
    boolean isCopyFile = BooleanUtils.toBooleanDefaultIfNull(((InputFileDescriptor)getInputDescriptor()).getCopyFile(), false);
    if (isCopyFile && files != null) {
      for (File file : files) {
        try {
          InputFileMarker marker = new InputFileMarker(this, null, 0);
          getOutputManager().copyFile(file, marker);
          if (isClosed() || isDrain()) {
            LOG.info("isClosed or isDrain. Now breaking loop.");
            break;
          }
        } catch (Throwable t) {
          LOG.error("Error processing file=" + file.getAbsolutePath(), t);
        }
      }
    }
  }

  public void startNewChildInputFileThread(Map.Entry<String, List<File>> folderFileEntry) throws CloneNotSupportedException {
    LOG.info("Start child input thread - " + folderFileEntry.getKey());
    InputFile clonedObject = (InputFile) this.clone();
    String folderPath = folderFileEntry.getKey();
    String filePath = new File(getFilePath()).getName();
    String fullPathWithWildCard = String.format("%s/%s", folderPath, filePath);
    if (clonedObject.getMaxAgeMin() != 0 && FileUtil.isFileTooOld(new File(fullPathWithWildCard), clonedObject.getMaxAgeMin().longValue())) {
      LOG.info(String.format("File ('%s') is too old (max age min: %d), monitor thread not starting...", getFilePath(), clonedObject.getMaxAgeMin()));
    } else {
      clonedObject.setMultiFolder(false);
      clonedObject.logFiles = folderFileEntry.getValue().toArray(new File[0]); // TODO: works only with tail
      clonedObject.logPath = fullPathWithWildCard;
      clonedObject.setLogFileDetacherThread(null);
      clonedObject.setLogFilePathUpdaterThread(null);
      clonedObject.setInputChildMap(new HashMap<>());
      copyFilters(clonedObject, getFirstFilter());
      Thread thread = new Thread(threadGroup, clonedObject, "file=" + fullPathWithWildCard);
      clonedObject.setThread(thread);
      inputChildMap.put(fullPathWithWildCard, clonedObject);
      thread.start();
    }
  }

  private void copyFilters(InputFile clonedInput, Filter firstFilter) {
    if (firstFilter != null) {
      try {
        LOG.info("Cloning filters for input=" + clonedInput.logPath);
        Filter newFilter = (Filter) firstFilter.clone();
        newFilter.setInput(clonedInput);
        clonedInput.setFirstFilter(newFilter);
        Filter actFilter = firstFilter;
        Filter actClonedFilter = newFilter;
        while (actFilter != null) {
          if (actFilter.getNextFilter() != null) {
            actFilter = actFilter.getNextFilter();
            Filter newClonedFilter = (Filter) actFilter.clone();
            newClonedFilter.setInput(clonedInput);
            actClonedFilter.setNextFilter(newClonedFilter);
            actClonedFilter = newClonedFilter;
          } else {
            actClonedFilter.setNextFilter(null);
            actFilter = null;
          }
        }
        LOG.info("Cloning filters has finished for input=" + clonedInput.logPath);
      } catch (Exception e) {
        LOG.error("Could not clone filters for input=" + clonedInput.logPath);
      }
    }
  }

  public void stopChildInputFileThread(String folderPathKey) {
    LOG.info("Stop child input thread - " + folderPathKey);
    String filePath = new File(getFilePath()).getName();
    String fullPathWithWildCard = String.format("%s/%s", folderPathKey, filePath);
    if (inputChildMap.containsKey(fullPathWithWildCard)) {
      InputFile inputFile = inputChildMap.get(fullPathWithWildCard);
      inputFile.setClosed(true);
      if (inputFile.getThread() != null && inputFile.getThread().isAlive()) {
        inputFile.getThread().interrupt();
      }
      inputChildMap.remove(fullPathWithWildCard);
    } else {
      LOG.warn(fullPathWithWildCard + " not found as an input child.");
    }
  }

  @Override
  public boolean isEnabled() {
    return BooleanUtils.isNotFalse(getInputDescriptor().isEnabled());
  }

  @Override
  public String getShortDescription() {
    return "input:source=" + getInputDescriptor().getSource() + ", path=" +
      (!ArrayUtils.isEmpty(logFiles) ? logFiles[0].getAbsolutePath() : logPath);
  }

  @Override
  public boolean logConfigs() {
    LOG.info("Printing Input=" + getShortDescription());
    LOG.info("description=" + getInputDescriptor().getPath());
    return true;
  }

  @Override
  public void close() {
    super.close();
    LOG.info("close() calling checkPoint checkIn(). " + getShortDescription());
    lastCheckIn();
    setClosed(true);
  }

  public File[] getActualInputLogFiles() {
    return FileUtil.getInputFilesByPattern(logPath);
  }

  public String getFilePath() {
    return filePath;
  }

  public void setFilePath(String filePath) {
    this.filePath = filePath;
  }

  public String getLogPath() {
    return logPath;
  }

  public Object getFileKey() {
    return fileKey;
  }

  public String getBase64FileKey() throws Exception {
    return base64FileKey;
  }

  public void setFileKey(Object fileKey) {
    this.fileKey = fileKey;
  }

  public boolean isTail() {
    return tail;
  }

  public File[] getLogFiles() {
    return logFiles;
  }

  public void setBase64FileKey(String base64FileKey) {
    this.base64FileKey = base64FileKey;
  }

  public void setLogFiles(File[] logFiles) {
    this.logFiles = logFiles;
  }

  public String getCheckPointExtension() {
    return checkPointExtension;
  }

  public int getCheckPointIntervalMS() {
    return checkPointIntervalMS;
  }

  public Map<String, File> getCheckPointFiles() {
    return checkPointFiles;
  }

  public Map<String, Long> getLastCheckPointTimeMSs() {
    return lastCheckPointTimeMSs;
  }

  public Map<String, Map<String, Object>> getJsonCheckPoints() {
    return jsonCheckPoints;
  }

  public Map<String, InputFileMarker> getLastCheckPointInputMarkers() {
    return lastCheckPointInputMarkers;
  }

  public boolean isMultiFolder() {
    return multiFolder;
  }

  public void setMultiFolder(boolean multiFolder) {
    this.multiFolder = multiFolder;
  }

  public Map<String, List<File>> getFolderMap() {
    return folderMap;
  }

  public void setFolderMap(Map<String, List<File>> folderMap) {
    this.folderMap = folderMap;
  }

  public Map<String, InputFile> getInputChildMap() {
    return inputChildMap;
  }

  public void setInputChildMap(Map<String, InputFile> inputChildMap) {
    this.inputChildMap = inputChildMap;
  }

  @Override
  public Thread getThread() {
    return thread;
  }

  @Override
  public void setThread(Thread thread) {
    this.thread = thread;
  }

  public Thread getLogFileDetacherThread() {
    return logFileDetacherThread;
  }

  public void setLogFileDetacherThread(Thread logFileDetacherThread) {
    this.logFileDetacherThread = logFileDetacherThread;
  }

  public Thread getLogFilePathUpdaterThread() {
    return logFilePathUpdaterThread;
  }

  public void setLogFilePathUpdaterThread(Thread logFilePathUpdaterThread) {
    this.logFilePathUpdaterThread = logFilePathUpdaterThread;
  }

  public Integer getMaxAgeMin() {
    return maxAgeMin;
  }

}
