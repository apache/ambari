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
package org.apache.ambari.logfeeder.input.file;

import org.apache.ambari.logfeeder.input.InputFile;
import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

public class ResumeLineNumberHelper {

  private static final Logger LOG = LoggerFactory.getLogger(ResumeLineNumberHelper.class);

  private ResumeLineNumberHelper() {
  }

  public static int getResumeFromLineNumber(InputFile inputFile) {
    int resumeFromLineNumber = 0;

    File checkPointFile = null;
    try {
      LOG.info("Checking existing checkpoint file. " + inputFile.getShortDescription());

      String checkPointFileName = inputFile.getBase64FileKey() + inputFile.getCheckPointExtension();
      File checkPointFolder = inputFile.getInputManager().getCheckPointFolderFile();
      checkPointFile = new File(checkPointFolder, checkPointFileName);
      inputFile.getCheckPointFiles().put(inputFile.getBase64FileKey(), checkPointFile);
      Map<String, Object> jsonCheckPoint = null;
      if (!checkPointFile.exists()) {
        LOG.info("Checkpoint file for log file " + inputFile.getFilePath() + " doesn't exist, starting to read it from the beginning");
      } else {
        try (RandomAccessFile checkPointWriter = new RandomAccessFile(checkPointFile, "rw")) {
          int contentSize = checkPointWriter.readInt();
          byte b[] = new byte[contentSize];
          int readSize = checkPointWriter.read(b, 0, contentSize);
          if (readSize != contentSize) {
            LOG.error("Couldn't read expected number of bytes from checkpoint file. expected=" + contentSize + ", read=" +
              readSize + ", checkPointFile=" + checkPointFile + ", input=" + inputFile.getShortDescription());
          } else {
            String jsonCheckPointStr = new String(b, 0, readSize);
            jsonCheckPoint = LogFeederUtil.toJSONObject(jsonCheckPointStr);

            resumeFromLineNumber = LogFeederUtil.objectToInt(jsonCheckPoint.get("line_number"), 0, "line_number");

            LOG.info("CheckPoint. checkPointFile=" + checkPointFile + ", json=" + jsonCheckPointStr +
              ", resumeFromLineNumber=" + resumeFromLineNumber);
          }
        } catch (EOFException eofEx) {
          LOG.info("EOFException. Will reset checkpoint file " + checkPointFile.getAbsolutePath() + " for " +
            inputFile.getShortDescription(), eofEx);
        }
      }
      if (jsonCheckPoint == null) {
        // This seems to be first time, so creating the initial checkPoint object
        jsonCheckPoint = new HashMap<String, Object>();
        jsonCheckPoint.put("file_path", inputFile.getFilePath());
        jsonCheckPoint.put("file_key", inputFile.getBase64FileKey());
      }

      inputFile.getJsonCheckPoints().put(inputFile.getBase64FileKey(), jsonCheckPoint);

    } catch (Throwable t) {
      LOG.error("Error while configuring checkpoint file. Will reset file. checkPointFile=" + checkPointFile, t);
    }

    return resumeFromLineNumber;
  }

}
