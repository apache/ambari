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
package org.apache.ambari.logfeeder.input.monitor;

import org.apache.ambari.logfeeder.input.InputFile;
import org.apache.ambari.logfeeder.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Update log file paths periodically, useful if the log file name pattern format is like: mylog-2017-10-09.log (so the tail one can change)
 */
public class LogFilePathUpdateMonitor extends AbstractLogFileMonitor {

  private Logger LOG = LoggerFactory.getLogger(LogFilePathUpdateMonitor.class);

  public LogFilePathUpdateMonitor(InputFile inputFile, int interval, int detachTime) {
    super(inputFile, interval, detachTime);
  }

  @Override
  public String getStartLog() {
    return "Start file path update monitor thread for " + getInputFile().getFilePath();
  }

  @Override
  protected void monitorAndUpdate() throws Exception {
    File[] logFiles = getInputFile().getActualInputLogFiles();
    Map<String, List<File>> foldersMap = FileUtil.getFoldersForFiles(logFiles);
    Map<String, List<File>> originalFoldersMap = getInputFile().getFolderMap();
    for (Map.Entry<String, List<File>> entry : foldersMap.entrySet()) {
      if (originalFoldersMap.keySet().contains(entry.getKey())) {
        List<File> originalLogFiles = originalFoldersMap.get(entry.getKey());
        if (!entry.getValue().isEmpty()) { // check tail only for now
          File lastFile = entry.getValue().get(0);
          if (!originalLogFiles.get(0).getAbsolutePath().equals(lastFile.getAbsolutePath())) {
            LOG.info("New file found (old: '{}', new: {}), reload thread for {}",
              lastFile.getAbsolutePath(), originalLogFiles.get(0).getAbsolutePath(), entry.getKey());
            getInputFile().stopChildInputFileThread(entry.getKey());
            getInputFile().startNewChildInputFileThread(entry);
          }
        }
      } else {
        LOG.info("New log file folder found: {}, start a new thread if tail file is not too old.", entry.getKey());
        File monitoredFile = entry.getValue().get(0);
        if (FileUtil.isFileTooOld(monitoredFile, getDetachTime())) {
          LOG.info("'{}' file is too old. No new thread start needed.", monitoredFile.getAbsolutePath());
        } else {
          getInputFile().startNewChildInputFileThread(entry);
        }
      }
    }
  }
}