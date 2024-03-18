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

import org.apache.ambari.logfeeder.util.S3Util;
import org.apache.ambari.logsearch.config.api.model.inputconfig.InputS3FileDescriptor;
import org.apache.commons.lang.ArrayUtils;
import org.apache.solr.common.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;

public class InputS3File extends InputFile {

  private static final Logger LOG = LoggerFactory.getLogger(InputS3File.class);

  @Override
  public boolean isReady() {
    if (!isReady()) {
      // Let's try to check whether the file is available
      setLogFiles(getActualFiles(getLogPath()));
      if (!ArrayUtils.isEmpty(getLogFiles())) {
        if (isTail() && getLogFiles().length > 1) {
          LOG.warn("Found multiple files (" + getLogFiles().length + ") for the file filter " + getFilePath() +
              ". Will use only the first one. Using " + getLogFiles()[0].getAbsolutePath());
        }
        LOG.info("File filter " + getFilePath() + " expanded to " + getLogFiles()[0].getAbsolutePath());
        setReady(true);
      } else {
        LOG.debug(getLogPath() + " file doesn't exist. Ignoring for now");
      }
    }
    return isReady();
  }

  private File[] getActualFiles(String searchPath) {
    // TODO search file on s3
    return new File[] { new File(searchPath) };
  }

  @Override
  public void start() throws Exception {
    if (ArrayUtils.isEmpty(getLogFiles())) {
      return;
    }
    for (int i = getLogFiles().length - 1; i >= 0; i--) {
      File file = getLogFiles()[i];
      if (i == 0 || !isTail()) {
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
  }

  @Override
  public BufferedReader openLogFile(File logPathFile) throws Exception {
    String s3AccessKey = ((InputS3FileDescriptor)getInputDescriptor()).getS3AccessKey();
    String s3SecretKey = ((InputS3FileDescriptor)getInputDescriptor()).getS3SecretKey();
    BufferedReader br = S3Util.getReader(logPathFile.getPath(), s3AccessKey, s3SecretKey);
    Object fileKey = getFileKey(logPathFile);
    setFileKey(fileKey);
    String base64FileKey = Base64.byteArrayToBase64(getFileKey().toString().getBytes());
    setBase64FileKey(base64FileKey);
    LOG.info("fileKey=" + fileKey + ", base64=" + base64FileKey + ". " + getShortDescription());
    return br;
  }

  private Object getFileKey(File logFile) {
    return logFile.getPath();
  }
  
  @Override
  public void close() {
    super.close();
    setClosed(true);
  }
}
