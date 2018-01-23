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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Comparator;

import org.apache.ambari.logfeeder.input.reader.LogsearchReaderFactory;
import org.apache.ambari.logfeeder.util.FileUtil;
import org.apache.ambari.logsearch.config.api.model.inputconfig.InputFileDescriptor;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.solr.common.util.Base64;

public class InputFile extends AbstractInputFile {

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

  private File[] getActualFiles(String searchPath) {
    File searchFile = new File(searchPath);
    if (!searchFile.getParentFile().exists()) {
      return new File[0];
    } else if (searchFile.isFile()) {
      return new File[]{searchFile};
    } else {
      FileFilter fileFilter = new WildcardFileFilter(searchFile.getName());
      File[] logFiles = searchFile.getParentFile().listFiles(fileFilter);
      Arrays.sort(logFiles,
          new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
              return o1.getName().compareTo(o2.getName());
            }
      });
      return logFiles;
    }
  }

  @Override
  void start() throws Exception {
    boolean isProcessFile = BooleanUtils.toBooleanDefaultIfNull(((InputFileDescriptor)inputDescriptor).getProcessFile(), true);
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

  @Override
  protected BufferedReader openLogFile(File logFile) throws FileNotFoundException {
    BufferedReader br = new BufferedReader(LogsearchReaderFactory.INSTANCE.getReader(logFile));
    fileKey = getFileKey(logFile);
    base64FileKey = Base64.byteArrayToBase64(fileKey.toString().getBytes());
    LOG.info("fileKey=" + fileKey + ", base64=" + base64FileKey + ". " + getShortDescription());
    return br;
  }

  @Override
  protected Object getFileKey(File logFile) {
    return FileUtil.getFileKey(logFile);
  }

  private void copyFiles(File[] files) {
    boolean isCopyFile = BooleanUtils.toBooleanDefaultIfNull(((InputFileDescriptor)inputDescriptor).getCopyFile(), false);
    if (isCopyFile && files != null) {
      for (File file : files) {
        try {
          InputMarker marker = new InputMarker(this, null, 0);
          outputManager.copyFile(file, marker);
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
}
