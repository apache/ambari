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
import org.apache.ambari.logfeeder.input.reader.LogsearchReaderFactory;
import org.apache.ambari.logfeeder.input.file.FileCheckInHelper;
import org.apache.ambari.logfeeder.input.file.ProcessFileHelper;
import org.apache.ambari.logfeeder.input.file.ResumeLineNumberHelper;
import org.apache.ambari.logfeeder.plugin.input.Input;
import org.apache.ambari.logfeeder.util.FileUtil;
import org.apache.ambari.logsearch.config.api.model.inputconfig.InputFileBaseDescriptor;
import org.apache.ambari.logsearch.config.api.model.inputconfig.InputFileDescriptor;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InputFile extends Input<LogFeederProps, InputFileMarker> {

  private static final Logger LOG = LoggerFactory.getLogger(InputFile.class);

  private static final boolean DEFAULT_TAIL = true;
  private static final boolean DEFAULT_USE_EVENT_MD5 = false;
  private static final boolean DEFAULT_GEN_EVENT_MD5 = true;
  private static final int DEFAULT_CHECKPOINT_INTERVAL_MS = 5 * 1000;

  private boolean isReady;

  private boolean tail;

  private String filePath;
  private File[] logFiles;
  private String logPath;
  private Object fileKey;
  private String base64FileKey;
  private String checkPointExtension;
  private int checkPointIntervalMS;

  private Map<String, File> checkPointFiles = new HashMap<>();
  private Map<String, Long> lastCheckPointTimeMSs = new HashMap<>();
  private Map<String, Map<String, Object>> jsonCheckPoints = new HashMap<>();
  private Map<String, InputFileMarker> lastCheckPointInputMarkers = new HashMap<>();

  private Thread thread;

  @Override
  public boolean isReady() {
    if (!isReady) {
      // Let's try to check whether the file is available
      logFiles = getActualFiles(logPath);
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

  private File[] getActualFiles(String searchPath) {
    File searchFile = new File(searchPath);
    if (!searchFile.getParentFile().exists()) {
      return new File[0];
    } else if (searchFile.isFile()) {
      return new File[]{searchFile};
    } else {
      FileFilter fileFilter = new WildcardFileFilter(searchFile.getName());
      File[] logFiles = searchFile.getParentFile().listFiles(fileFilter);
      Arrays.sort(logFiles, Comparator.comparing(File::getName));
      return logFiles;
    }
  }

  @Override
  public boolean monitor() {
    if (isReady()) {
      LOG.info("Starting thread. " + getShortDescription());
      thread = new Thread(this, getNameForThread());
      thread.start();
      return true;
    } else {
      return false;
    }
  }

  @Override
  public List<InputFile> getChildInputs() {
    return null;
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

    if (StringUtils.isEmpty(logPath)) {
      LOG.error("path is empty for file input. " + getShortDescription());
      return;
    }

    setFilePath(logPath);
    boolean isFileReady = isReady();
    LOG.info("File to monitor " + logPath + ", tail=" + tail + ", isReady=" + isReady());

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
}
