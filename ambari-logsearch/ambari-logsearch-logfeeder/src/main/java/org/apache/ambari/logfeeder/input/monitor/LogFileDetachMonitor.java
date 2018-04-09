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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Detach log files in case of folders do not exist or monitored files are too old
 */
public class LogFileDetachMonitor extends AbstractLogFileMonitor {

  private Logger LOG = LoggerFactory.getLogger(LogFileDetachMonitor.class);

  public LogFileDetachMonitor(InputFile inputFile, int interval, int detachTime) {
    super(inputFile, interval, detachTime);
  }

  @Override
  public String getStartLog() {
    return "Start file detach monitor thread for " + getInputFile().getFilePath();
  }

  @Override
  protected void monitorAndUpdate() throws Exception {
    File[] logFiles = getInputFile().getActualInputLogFiles();
    Map<String, List<File>> actualFolderMap = FileUtil.getFoldersForFiles(logFiles);

    // create map copies
    Map<String, InputFile> copiedInputFileMap = new HashMap<>(getInputFile().getInputChildMap());
    Map<String, List<File>> copiedFolderMap = new HashMap<>(getInputFile().getFolderMap());
    // detach old entries
    for (Map.Entry<String, List<File>> entry : copiedFolderMap.entrySet()) {
      if (new File(entry.getKey()).exists()) {
        for (Map.Entry<String, InputFile> inputFileEntry : copiedInputFileMap.entrySet()) {
          if (inputFileEntry.getKey().startsWith(entry.getKey())) {
            File monitoredFile = entry.getValue().get(0);
            boolean isFileTooOld = FileUtil.isFileTooOld(monitoredFile, getDetachTime());
            if (isFileTooOld) {
              LOG.info("File ('{}') in folder ('{}') is too old (reached {} minutes), detach input thread.", entry.getKey(), getDetachTime());
              getInputFile().stopChildInputFileThread(entry.getKey());
            }
          }
        }
      } else {
        LOG.info("Folder not exists. ({}) Stop thread.", entry.getKey());
        for (Map.Entry<String, InputFile> inputFileEntry : copiedInputFileMap.entrySet()) {
          if (inputFileEntry.getKey().startsWith(entry.getKey())) {
            getInputFile().stopChildInputFileThread(entry.getKey());
            getInputFile().setFolderMap(actualFolderMap);
          }
        }
      }
    }
  }
}
