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
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.logfeeder.conf.LogFeederProps;
import org.apache.ambari.logfeeder.util.FileUtil;
import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.ambari.logsearch.config.api.model.inputconfig.InputFileBaseDescriptor;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;

public abstract class AbstractInputFile extends Input {
  private static final int DEFAULT_CHECKPOINT_INTERVAL_MS = 5 * 1000;

  protected File[] logFiles;
  protected String logPath;
  protected Object fileKey;
  protected String base64FileKey;

  protected boolean isReady;

  private String checkPointExtension;
  private int checkPointIntervalMS;
  
  private Map<String, File> checkPointFiles = new HashMap<>();
  private Map<String, Long> lastCheckPointTimeMSs = new HashMap<>();
  private Map<String, Map<String, Object>> jsonCheckPoints = new HashMap<>();
  private Map<String, InputMarker> lastCheckPointInputMarkers = new HashMap<>();

  private LogFeederProps logFeederProps;

  @Override
  protected String getStatMetricName() {
    return "input.files.read_lines";
  }
  
  @Override
  protected String getReadBytesMetricName() {
    return "input.files.read_bytes";
  }
  
  @Override
  public void init(LogFeederProps logFeederProps) throws Exception {
    this.logFeederProps = logFeederProps;
    LOG.info("init() called");
    
    checkPointExtension = logFeederProps.getCheckPointExtension();

    // Let's close the file and set it to true after we start monitoring it
    setClosed(true);
    logPath = inputDescriptor.getPath();
    checkPointIntervalMS = (int) ObjectUtils.defaultIfNull(((InputFileBaseDescriptor)inputDescriptor).getCheckpointIntervalMs(), DEFAULT_CHECKPOINT_INTERVAL_MS);

    if (StringUtils.isEmpty(logPath)) {
      LOG.error("path is empty for file input. " + getShortDescription());
      return;
    }

    setFilePath(logPath);
    boolean isFileReady = isReady();

    LOG.info("File to monitor " + logPath + ", tail=" + tail + ", isReady=" + isFileReady);

    super.init(logFeederProps);
  }

  protected void processFile(File logPathFile, boolean follow) throws FileNotFoundException, IOException {
    LOG.info("Monitoring logPath=" + logPath + ", logPathFile=" + logPathFile);
    BufferedReader br = null;

    int lineCount = 0;
    try {
      setFilePath(logPathFile.getAbsolutePath());
      
      br = openLogFile(logPathFile);

      boolean resume = true;
      int resumeFromLineNumber = getResumeFromLineNumber();
      if (resumeFromLineNumber > 0) {
        LOG.info("Resuming log file " + logPathFile.getAbsolutePath() + " from line number " + resumeFromLineNumber);
        resume = false;
      }
      
      setClosed(false);
      int sleepStep = 2;
      int sleepIteration = 0;
      while (true) {
        try {
          if (isDrain()) {
            break;
          }

          String line = br.readLine();
          if (line == null) {
            if (!resume) {
              resume = true;
            }
            sleepIteration++;
            if (sleepIteration == 2) {
              flush();
              if (!follow) {
                LOG.info("End of file. Done with filePath=" + logPathFile.getAbsolutePath() + ", lineCount=" + lineCount);
                break;
              }
            } else if (sleepIteration > 4) {
              Object newFileKey = getFileKey(logPathFile);
              if (newFileKey != null && (fileKey == null || !newFileKey.equals(fileKey))) {
                LOG.info("File key is different. Marking this input file for rollover. oldKey=" + fileKey + ", newKey=" +
                    newFileKey + ". " + getShortDescription());
                
                try {
                  LOG.info("File is rolled over. Closing current open file." + getShortDescription() + ", lineCount=" +
                      lineCount);
                  br.close();
                } catch (Exception ex) {
                  LOG.error("Error closing file" + getShortDescription(), ex);
                  break;
                }
                
                try {
                  LOG.info("Opening new rolled over file." + getShortDescription());
                  br = openLogFile(logPathFile);
                  lineCount = 0;
                } catch (Exception ex) {
                  LOG.error("Error opening rolled over file. " + getShortDescription(), ex);
                  LOG.info("Added input to not ready list." + getShortDescription());
                  isReady = false;
                  inputManager.addToNotReady(this);
                  break;
                }
                LOG.info("File is successfully rolled over. " + getShortDescription());
                continue;
              }
            }
            try {
              Thread.sleep(sleepStep * 1000);
              sleepStep = Math.min(sleepStep * 2, 10);
            } catch (InterruptedException e) {
              LOG.info("Thread interrupted." + getShortDescription());
            }
          } else {
            lineCount++;
            sleepStep = 1;
            sleepIteration = 0;

            if (!resume && lineCount > resumeFromLineNumber) {
              LOG.info("Resuming to read from last line. lineCount=" + lineCount + ", input=" + getShortDescription());
              resume = true;
            }
            if (resume) {
              InputMarker marker = new InputMarker(this, base64FileKey, lineCount);
              outputLine(line, marker);
            }
          }
        } catch (Throwable t) {
          String logMessageKey = this.getClass().getSimpleName() + "_READ_LOOP_EXCEPTION";
          LogFeederUtil.logErrorMessageByInterval(logMessageKey, "Caught exception in read loop. lineNumber=" + lineCount +
              ", input=" + getShortDescription(), t, LOG, Level.ERROR);
        }
      }
    } finally {
      if (br != null) {
        LOG.info("Closing reader." + getShortDescription() + ", lineCount=" + lineCount);
        try {
          br.close();
        } catch (Throwable t) {
          // ignore
        }
      }
    }
  }

  protected abstract BufferedReader openLogFile(File logFile) throws IOException;

  protected abstract Object getFileKey(File logFile);
  
  private int getResumeFromLineNumber() {
    int resumeFromLineNumber = 0;
    
    File checkPointFile = null;
    try {
      LOG.info("Checking existing checkpoint file. " + getShortDescription());

      String checkPointFileName = base64FileKey + checkPointExtension;
      File checkPointFolder = inputManager.getCheckPointFolderFile();
      checkPointFile = new File(checkPointFolder, checkPointFileName);
      checkPointFiles.put(base64FileKey, checkPointFile);
      Map<String, Object> jsonCheckPoint = null;
      if (!checkPointFile.exists()) {
        LOG.info("Checkpoint file for log file " + filePath + " doesn't exist, starting to read it from the beginning");
      } else {
        try (RandomAccessFile checkPointWriter = new RandomAccessFile(checkPointFile, "rw")) {
          int contentSize = checkPointWriter.readInt();
          byte b[] = new byte[contentSize];
          int readSize = checkPointWriter.read(b, 0, contentSize);
          if (readSize != contentSize) {
            LOG.error("Couldn't read expected number of bytes from checkpoint file. expected=" + contentSize + ", read=" +
                readSize + ", checkPointFile=" + checkPointFile + ", input=" + getShortDescription());
          } else {
            String jsonCheckPointStr = new String(b, 0, readSize);
            jsonCheckPoint = LogFeederUtil.toJSONObject(jsonCheckPointStr);

            resumeFromLineNumber = LogFeederUtil.objectToInt(jsonCheckPoint.get("line_number"), 0, "line_number");

            LOG.info("CheckPoint. checkPointFile=" + checkPointFile + ", json=" + jsonCheckPointStr +
                ", resumeFromLineNumber=" + resumeFromLineNumber);
         }
        } catch (EOFException eofEx) {
          LOG.info("EOFException. Will reset checkpoint file " + checkPointFile.getAbsolutePath() + " for " +
              getShortDescription(), eofEx);
        }
      }
      if (jsonCheckPoint == null) {
        // This seems to be first time, so creating the initial checkPoint object
        jsonCheckPoint = new HashMap<String, Object>();
        jsonCheckPoint.put("file_path", filePath);
        jsonCheckPoint.put("file_key", base64FileKey);
      }
      
      jsonCheckPoints.put(base64FileKey, jsonCheckPoint);

    } catch (Throwable t) {
      LOG.error("Error while configuring checkpoint file. Will reset file. checkPointFile=" + checkPointFile, t);
    }
    
    return resumeFromLineNumber;
  }

  @Override
  public synchronized void checkIn(InputMarker inputMarker) {
    try {
      Map<String, Object> jsonCheckPoint = jsonCheckPoints.get(inputMarker.base64FileKey);
      File checkPointFile = checkPointFiles.get(inputMarker.base64FileKey);
      
      int lineNumber = LogFeederUtil.objectToInt(jsonCheckPoint.get("line_number"), 0, "line_number");
      if (lineNumber > inputMarker.lineNumber) {
        // Already wrote higher line number for this input
        return;
      }
      // If interval is greater than last checkPoint time, then write
      long currMS = System.currentTimeMillis();
      long lastCheckPointTimeMs = lastCheckPointTimeMSs.containsKey(inputMarker.base64FileKey) ?
          lastCheckPointTimeMSs.get(inputMarker.base64FileKey) : 0;
      if (!isClosed() && (currMS - lastCheckPointTimeMs < checkPointIntervalMS)) {
        // Let's save this one so we can update the check point file on flush
        lastCheckPointInputMarkers.put(inputMarker.base64FileKey, inputMarker);
        return;
      }
      lastCheckPointTimeMSs.put(inputMarker.base64FileKey, currMS);

      jsonCheckPoint.put("line_number", "" + new Integer(inputMarker.lineNumber));
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

      if (isClosed()) {
        String logMessageKey = this.getClass().getSimpleName() + "_FINAL_CHECKIN";
        LogFeederUtil.logErrorMessageByInterval(logMessageKey, "Wrote final checkPoint, input=" + getShortDescription() +
            ", checkPointFile=" + checkPointFile.getAbsolutePath() + ", checkPoint=" + jsonStr, null, LOG, Level.INFO);
      }
    } catch (Throwable t) {
      String logMessageKey = this.getClass().getSimpleName() + "_CHECKIN_EXCEPTION";
      LogFeederUtil.logErrorMessageByInterval(logMessageKey, "Caught exception checkIn. , input=" + getShortDescription(), t,
          LOG, Level.ERROR);
    }
  }

  @Override
  public void lastCheckIn() {
    for (InputMarker lastCheckPointInputMarker : lastCheckPointInputMarkers.values()) {
      checkIn(lastCheckPointInputMarker);
    }
  }

  @Override
  public void close() {
    super.close();
    LOG.info("close() calling checkPoint checkIn(). " + getShortDescription());
    lastCheckIn();
    isClosed = true;
  }

  @Override
  public String getShortDescription() {
    return "input:source=" + inputDescriptor.getSource() + ", path=" +
        (!ArrayUtils.isEmpty(logFiles) ? logFiles[0].getAbsolutePath() : logPath);
  }
}
