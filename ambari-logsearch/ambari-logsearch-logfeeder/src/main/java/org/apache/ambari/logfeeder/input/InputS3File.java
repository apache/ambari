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
import java.io.IOException;

import org.apache.ambari.logfeeder.util.S3Util;
import org.apache.commons.lang.ArrayUtils;
import org.apache.solr.common.util.Base64;

public class InputS3File extends AbstractInputFile {

  @Override
  public boolean isReady() {
    if (!isReady) {
      // Let's try to check whether the file is available
      logFiles = getActualFiles(logPath);
      if (!ArrayUtils.isEmpty(logFiles)) {
        if (tail && logFiles.length > 1) {
          LOG.warn("Found multiple files (" + logFiles.length + ") for the file filter " + filePath +
              ". Will use only the first one. Using " + logFiles[0].getAbsolutePath());
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
    // TODO search file on s3
    return new File[] { new File(searchPath) };
  }

  @Override
  void start() throws Exception {
    if (ArrayUtils.isEmpty(logFiles)) {
      return;
    }

    if (tail) {
      processFile(logFiles[0]);
    } else {
      for (File s3FilePath : logFiles) {
        try {
          processFile(s3FilePath);
          if (isClosed() || isDrain()) {
            LOG.info("isClosed or isDrain. Now breaking loop.");
            break;
          }
        } catch (Throwable t) {
          LOG.error("Error processing file=" + s3FilePath, t);
        }
      }
    }
    close();
  }

  @Override
  protected BufferedReader openLogFile(File logPathFile) throws IOException {
    String s3AccessKey = getStringValue("s3_access_key");
    String s3SecretKey = getStringValue("s3_secret_key");
    BufferedReader br = S3Util.getReader(logPathFile.getPath(), s3AccessKey, s3SecretKey);
    fileKey = getFileKey(logPathFile);
    base64FileKey = Base64.byteArrayToBase64(fileKey.toString().getBytes());
    LOG.info("fileKey=" + fileKey + ", base64=" + base64FileKey + ". " + getShortDescription());
    return br;
  }

  @Override
  protected Object getFileKey(File logFile) {
    return logFile.getPath();
  }
}
