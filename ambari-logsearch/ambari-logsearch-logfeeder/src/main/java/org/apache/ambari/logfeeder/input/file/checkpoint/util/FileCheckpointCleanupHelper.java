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
package org.apache.ambari.logfeeder.input.file.checkpoint.util;

import org.apache.ambari.logfeeder.util.FileUtil;
import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.solr.common.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.Map;

public class FileCheckpointCleanupHelper {

  private static final Logger LOG = LoggerFactory.getLogger(FileCheckpointCleanupHelper.class);

  private FileCheckpointCleanupHelper() {
  }

  public static void cleanCheckPointFiles(File checkPointFolderFile, String checkPointExtension) {
    if (checkPointFolderFile == null) {
      LOG.info("Will not clean checkPoint files. checkPointFolderFile=null");
      return;
    }
    LOG.info("Cleaning checkPoint files. checkPointFolderFile=" + checkPointFolderFile.getAbsolutePath());
    try {
      // Loop over the check point files and if filePath is not present, then move to closed
      File[] checkPointFiles = CheckpointFileReader.getFiles(checkPointFolderFile, checkPointExtension);
      int totalCheckFilesDeleted = 0;
      if (checkPointFiles != null) {
        for (File checkPointFile : checkPointFiles) {
          if (checkCheckPointFile(checkPointFile)) {
            totalCheckFilesDeleted++;
          }
        }
        LOG.info("Deleted " + totalCheckFilesDeleted + " checkPoint file(s). checkPointFolderFile=" +
          checkPointFolderFile.getAbsolutePath());
      }
    } catch (Throwable t) {
      LOG.error("Error while cleaning checkPointFiles", t);
    }
  }

  private static boolean checkCheckPointFile(File checkPointFile) {
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

  private static boolean wasFileRenamed(File folder, String searchFileBase64) {
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


}
