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

import java.io.File;
import java.io.RandomAccessFile;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.logfeeder.input.InputFile;
import org.apache.ambari.logfeeder.input.InputFileMarker;
import org.apache.ambari.logfeeder.util.FileUtil;
import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.log4j.Logger;

public class FileCheckInHelper {

  private static final Logger LOG = Logger.getLogger(FileCheckInHelper.class);

  private FileCheckInHelper() {
  }

  public static void checkIn(InputFile inputFile, InputFileMarker inputMarker) {
    try {
      Map<String, Object> jsonCheckPoint = inputFile.getJsonCheckPoints().get(inputMarker.getBase64FileKey());
      if (jsonCheckPoint == null) {
        jsonCheckPoint = createNewCheckpointObject(inputFile);
        attachCheckpointToInput(inputFile, jsonCheckPoint);
      }
      File checkPointFile = inputFile.getCheckPointFiles().get(inputMarker.getBase64FileKey());
      if (checkPointFile == null || !checkPointFile.exists()) {
        checkPointFile = FileCheckInHelper.attachCheckpointFileToInput(inputFile);
      }

      int lineNumber = LogFeederUtil.objectToInt(jsonCheckPoint.get("line_number"), 0, "line_number");
      if (lineNumber > inputMarker.getLineNumber()) {
        // Already wrote higher line number for this input
        return;
      }
      // If interval is greater than last checkPoint time, then write
      long currMS = System.currentTimeMillis();
      long lastCheckPointTimeMs = inputFile.getLastCheckPointTimeMSs().containsKey(inputMarker.getBase64FileKey()) ?
        inputFile.getLastCheckPointTimeMSs().get(inputMarker.getBase64FileKey()) : 0;
      if (!inputFile.isClosed() && (currMS - lastCheckPointTimeMs < inputFile.getCheckPointIntervalMS())) {
        // Let's save this one so we can update the check point file on flush
        inputFile.getLastCheckPointInputMarkers().put(inputMarker.getBase64FileKey(), inputMarker);
        return;
      }
      inputFile.getLastCheckPointTimeMSs().put(inputMarker.getBase64FileKey(), currMS);

      if (inputFile.getMaxAgeMin() != 0) {
        jsonCheckPoint.put("max_age_min", inputFile.getMaxAgeMin().toString());
      }
      jsonCheckPoint.put("line_number", "" + new Integer(inputMarker.getLineNumber()));
      jsonCheckPoint.put("last_write_time_ms", "" + new Long(currMS));
      jsonCheckPoint.put("last_write_time_date", new Date());

      String jsonStr = LogFeederUtil.getGson().toJson(jsonCheckPoint);

      File tmpCheckPointFile = new File(checkPointFile.getAbsolutePath() + ".tmp");
      if (tmpCheckPointFile.exists()) {
        tmpCheckPointFile.delete();
      }
      RandomAccessFile tmpRaf = new RandomAccessFile(tmpCheckPointFile, "rws");
      tmpRaf.writeInt(jsonStr.length());
      tmpRaf.write(jsonStr.getBytes());
      tmpRaf.getFD().sync();
      tmpRaf.close();

      FileUtil.move(tmpCheckPointFile, checkPointFile);

      if (inputFile.isClosed()) {
        LOG.info(String.format("Wrote final checkPoint, input=%s, checkPointFile=%s, checkPoint=%s", inputFile.getShortDescription(), checkPointFile.getAbsolutePath(), jsonStr));
      }
    } catch (Throwable t) {
      LOG.error("Caught exception checkIn. , input=" + inputFile.getShortDescription(), t);
    }
  }

  /**
   * Create new checkpoint object
   * @param inputFile file object which is used to fill the checkpoint defaults
   * @return Created checkpoint object
   */
  static Map<String, Object> createNewCheckpointObject(final InputFile inputFile) {
    Map<String, Object> jsonCheckPoint = new HashMap<>();
    jsonCheckPoint.put("file_path", inputFile.getFilePath());
    try {
      jsonCheckPoint.put("file_key", inputFile.getBase64FileKey());
    } catch (Exception e) {
      LOG.error(String.format("Error during checkpoint object (path: %s) creationg: %s", inputFile.getFilePath(), e.getMessage()));
    }
    return jsonCheckPoint;
  }

  /**
   * Attach a json checkpoint object to an input file
   * @param inputFile input file object that will have the new checkpoint
   * @param jsonCheckPoint holds checkpoint related data
   */
  static void attachCheckpointToInput(final InputFile inputFile, final Map<String, Object> jsonCheckPoint) throws Exception {
    inputFile.getJsonCheckPoints().put(inputFile.getBase64FileKey(), jsonCheckPoint);
  }

  /**
   * Create a new file object for input checkpoint
   * @param inputFile input file object that will have the new checkpoint file
   * @return Newly created checkpoint file
   */
  static File attachCheckpointFileToInput(final InputFile inputFile) throws Exception {
    String checkPointFileName = inputFile.getBase64FileKey() + inputFile.getCheckPointExtension();
    File checkPointFolder = inputFile.getInputManager().getCheckPointFolderFile();
    File checkPointFile = new File(checkPointFolder, checkPointFileName);
    inputFile.getCheckPointFiles().put(inputFile.getBase64FileKey(), checkPointFile);
    return checkPointFile;
  }


}
