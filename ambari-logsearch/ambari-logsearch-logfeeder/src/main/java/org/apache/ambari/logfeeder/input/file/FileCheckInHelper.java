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
import org.apache.ambari.logfeeder.input.InputFileMarker;
import org.apache.ambari.logfeeder.util.FileUtil;
import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.Date;
import java.util.Map;

public class FileCheckInHelper {

  private static final Logger LOG = Logger.getLogger(FileCheckInHelper.class);

  private FileCheckInHelper() {
  }

  public static void checkIn(InputFile inputFile, InputFileMarker inputMarker) {
    try {
      Map<String, Object> jsonCheckPoint = inputFile.getJsonCheckPoints().get(inputMarker.getBase64FileKey());
      File checkPointFile = inputFile.getCheckPointFiles().get(inputMarker.getBase64FileKey());

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
        String logMessageKey = inputFile.getClass().getSimpleName() + "_FINAL_CHECKIN";
        LogFeederUtil.logErrorMessageByInterval(logMessageKey, "Wrote final checkPoint, input=" + inputFile.getShortDescription() +
          ", checkPointFile=" + checkPointFile.getAbsolutePath() + ", checkPoint=" + jsonStr, null, LOG, Level.INFO);
      }
    } catch (Throwable t) {
      String logMessageKey = inputFile.getClass().getSimpleName() + "_CHECKIN_EXCEPTION";
      LogFeederUtil.logErrorMessageByInterval(logMessageKey, "Caught exception checkIn. , input=" + inputFile.getShortDescription(), t,
        LOG, Level.ERROR);
    }
  }


}
