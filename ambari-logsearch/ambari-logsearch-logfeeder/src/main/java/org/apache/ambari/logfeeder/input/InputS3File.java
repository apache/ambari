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

import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.ambari.logfeeder.util.S3Util;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.solr.common.util.Base64;

public class InputS3File extends Input {
  private static final Logger logger = Logger.getLogger(InputS3File.class);

  private String logPath = null;
  private boolean isStartFromBegining = true;

  private boolean isReady = false;
  private String[] s3LogPathFiles = null;
  private Object fileKey = null;
  private String base64FileKey = null;

  private boolean isRolledOver = false;
  private boolean addWildCard = false;

  private long lastCheckPointTimeMS = 0;
  private int checkPointIntervalMS = 5 * 1000; // 5 seconds
  private RandomAccessFile checkPointWriter = null;
  private Map<String, Object> jsonCheckPoint = null;

  private File checkPointFile = null;

  private InputMarker lastCheckPointInputMarker = null;

  private String checkPointExtension = ".cp";


  @Override
  public void init() throws Exception {
    logger.info("init() called");
    statMetric.metricsName = "input.files.read_lines";
    readBytesMetric.metricsName = "input.files.read_bytes";
    checkPointExtension = LogFeederUtil.getStringProperty(
        "logfeeder.checkpoint.extension", checkPointExtension);

    // Let's close the file and set it to true after we start monitoring it
    setClosed(true);
    logPath = getStringValue("path");
    tail = getBooleanValue("tail", tail);
    addWildCard = getBooleanValue("add_wild_card", addWildCard);
    checkPointIntervalMS = getIntValue("checkpoint.interval.ms",
        checkPointIntervalMS);
    if (logPath == null || logPath.isEmpty()) {
      logger.error("path is empty for file input. " + getShortDescription());
      return;
    }

    String startPosition = getStringValue("start_position");
    if (StringUtils.isEmpty(startPosition)
        || startPosition.equalsIgnoreCase("beginning")
        || startPosition.equalsIgnoreCase("begining")) {
      isStartFromBegining = true;
    }

    if (!tail) {
      // start position end doesn't apply if we are not tailing
      isStartFromBegining = true;
    }

    setFilePath(logPath);
    boolean isFileReady = isReady();

    logger.info("File to monitor " + logPath + ", tail=" + tail
        + ", addWildCard=" + addWildCard + ", isReady=" + isFileReady);

    super.init();
  }

  @Override
  public boolean isReady() {
    if (!isReady) {
      // Let's try to check whether the file is available
      s3LogPathFiles = getActualFiles(logPath);
      if (s3LogPathFiles != null && s3LogPathFiles.length > 0) {
        if (isTail() && s3LogPathFiles.length > 1) {
          logger.warn("Found multiple files (" + s3LogPathFiles.length
              + ") for the file filter " + filePath
              + ". Will use only the first one. Using " + s3LogPathFiles[0]);
        }
        logger.info("File filter " + filePath + " expanded to "
            + s3LogPathFiles[0]);
        isReady = true;
      } else {
        logger.debug(logPath + " file doesn't exist. Ignoring for now");
      }
    }
    return isReady;
  }

  private String[] getActualFiles(String searchPath) {
    // TODO search file on s3
    return new String[] { searchPath };
  }

  @Override
  synchronized public void checkIn(InputMarker inputMarker) {
    super.checkIn(inputMarker);
    if (checkPointWriter != null) {
      try {
        int lineNumber = LogFeederUtil.objectToInt(
            jsonCheckPoint.get("line_number"), 0, "line_number");
        if (lineNumber > inputMarker.lineNumber) {
          // Already wrote higher line number for this input
          return;
        }
        // If interval is greater than last checkPoint time, then write
        long currMS = System.currentTimeMillis();
        if (!isClosed()
            && (currMS - lastCheckPointTimeMS) < checkPointIntervalMS) {
          // Let's save this one so we can update the check point file
          // on flush
          lastCheckPointInputMarker = inputMarker;
          return;
        }
        lastCheckPointTimeMS = currMS;

        jsonCheckPoint.put("line_number", ""
            + new Integer(inputMarker.lineNumber));
        jsonCheckPoint.put("last_write_time_ms", "" + new Long(currMS));
        jsonCheckPoint.put("last_write_time_date", new Date());

        String jsonStr = LogFeederUtil.getGson().toJson(jsonCheckPoint);

        // Let's rewind
        checkPointWriter.seek(0);
        checkPointWriter.writeInt(jsonStr.length());
        checkPointWriter.write(jsonStr.getBytes());

        if (isClosed()) {
          final String LOG_MESSAGE_KEY = this.getClass().getSimpleName()
              + "_FINAL_CHECKIN";
          LogFeederUtil.logErrorMessageByInterval(LOG_MESSAGE_KEY,
              "Wrote final checkPoint, input=" + getShortDescription()
                  + ", checkPointFile=" + checkPointFile.getAbsolutePath()
                  + ", checkPoint=" + jsonStr, null, logger, Level.INFO);
        }
      } catch (Throwable t) {
        final String LOG_MESSAGE_KEY = this.getClass().getSimpleName()
            + "_CHECKIN_EXCEPTION";
        LogFeederUtil.logErrorMessageByInterval(LOG_MESSAGE_KEY,
            "Caught exception checkIn. , input=" + getShortDescription(), t,
            logger, Level.ERROR);
      }
    }

  }

  @Override
  public void checkIn() {
    super.checkIn();
    if (lastCheckPointInputMarker != null) {
      checkIn(lastCheckPointInputMarker);
    }
  }

  @Override
  public void rollOver() {
    logger.info("Marking this input file for rollover. "
        + getShortDescription());
    isRolledOver = true;
  }

  @Override
  void start() throws Exception {
    if (s3LogPathFiles == null || s3LogPathFiles.length == 0) {
      return;
    }

    if (isTail()) {
      processFile(s3LogPathFiles[0]);
    } else {
      for (String s3FilePath : s3LogPathFiles) {
        try {
          processFile(s3FilePath);
          if (isClosed() || isDrain()) {
            logger.info("isClosed or isDrain. Now breaking loop.");
            break;
          }
        } catch (Throwable t) {
          logger.error("Error processing file=" + s3FilePath, t);
        }
      }
    }
    close();
  }

  @Override
  public void close() {
    super.close();
    logger.info("close() calling checkPoint checkIn(). "
        + getShortDescription());
    checkIn();
  }

  private void processFile(String logPathFile) throws FileNotFoundException,
      IOException {
    logger.info("Monitoring logPath=" + logPath + ", logPathFile="
        + logPathFile);
    BufferedReader br = null;
    checkPointFile = null;
    checkPointWriter = null;
    jsonCheckPoint = null;
    int resumeFromLineNumber = 0;

    int lineCount = 0;
    try {
      setFilePath(logPathFile);
      String s3AccessKey = getStringValue("s3_access_key");
      String s3SecretKey = getStringValue("s3_secret_key");
      br = S3Util.INSTANCE.getReader(logPathFile,s3AccessKey,s3SecretKey);
      if(br==null){
        //log err
        return;
      }
      
      // Whether to send to output from the beginning.
      boolean resume = isStartFromBegining;

      // Seems FileWatch is not reliable, so let's only use file key comparison
      fileKey = getFileKey(logPathFile);
      base64FileKey = Base64.byteArrayToBase64(fileKey.toString().getBytes());
      logger.info("fileKey=" + fileKey + ", base64=" + base64FileKey + ". "
          + getShortDescription());

      if (isTail()) {
        try {
          // Let's see if there is a checkpoint for this file
          logger.info("Checking existing checkpoint file. "
              + getShortDescription());

          String fileBase64 = Base64.byteArrayToBase64(fileKey.toString()
              .getBytes());
          String checkPointFileName = fileBase64 + checkPointExtension;
          File checkPointFolder = inputMgr.getCheckPointFolderFile();
          checkPointFile = new File(checkPointFolder, checkPointFileName);
          checkPointWriter = new RandomAccessFile(checkPointFile, "rw");

          try {
            int contentSize = checkPointWriter.readInt();
            byte b[] = new byte[contentSize];
            int readSize = checkPointWriter.read(b, 0, contentSize);
            if (readSize != contentSize) {
              logger
                  .error("Couldn't read expected number of bytes from checkpoint file. expected="
                      + contentSize
                      + ", read="
                      + readSize
                      + ", checkPointFile="
                      + checkPointFile
                      + ", input="
                      + getShortDescription());
            } else {
              String jsonCheckPointStr = new String(b, 0, readSize);
              jsonCheckPoint = LogFeederUtil.toJSONObject(jsonCheckPointStr);

              resumeFromLineNumber = LogFeederUtil.objectToInt(
                  jsonCheckPoint.get("line_number"), 0, "line_number");

              if (resumeFromLineNumber > 0) {
                // Let's read from last line read
                resume = false;
              }
              logger.info("CheckPoint. checkPointFile=" + checkPointFile
                  + ", json=" + jsonCheckPointStr + ", resumeFromLineNumber="
                  + resumeFromLineNumber + ", resume=" + resume);
            }
          } catch (EOFException eofEx) {
            logger.info("EOFException. Will reset checkpoint file "
                + checkPointFile.getAbsolutePath() + " for "
                + getShortDescription());
          }
          if (jsonCheckPoint == null) {
            // This seems to be first time, so creating the initial
            // checkPoint object
            jsonCheckPoint = new HashMap<String, Object>();
            jsonCheckPoint.put("file_path", filePath);
            jsonCheckPoint.put("file_key", fileBase64);
          }

        } catch (Throwable t) {
          logger.error(
              "Error while configuring checkpoint file. Will reset file. checkPointFile="
                  + checkPointFile, t);
        }
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
            try {
              // Since FileWatch service is not reliable, we will check
              // file inode every n seconds after no write
              if (sleepIteration > 4) {
                Object newFileKey = getFileKey(logPathFile);
                if (newFileKey != null) {
                  if (fileKey == null || !newFileKey.equals(fileKey)) {
                    logger
                        .info("File key is different. Calling rollover. oldKey="
                            + fileKey
                            + ", newKey="
                            + newFileKey
                            + ". "
                            + getShortDescription());
                    // File has rotated.
                    rollOver();
                  }
                }
              }
              // Flush on the second iteration
              if (!tail && sleepIteration >= 2) {
                logger.info("End of file. Done with filePath=" + logPathFile
                    + ", lineCount=" + lineCount);
                flush();
                break;
              } else if (sleepIteration == 2) {
                flush();
              } else if (sleepIteration >= 2) {
                if (isRolledOver) {
                  isRolledOver = false;
                  // Close existing file
                  try {
                    logger
                        .info("File is rolled over. Closing current open file."
                            + getShortDescription() + ", lineCount="
                            + lineCount);
                    br.close();
                  } catch (Exception ex) {
                    logger.error("Error closing file" + getShortDescription());
                    break;
                  }
                  try {
                    // Open new file
                    logger.info("Opening new rolled over file."
                        + getShortDescription());
                    br = S3Util.INSTANCE.getReader(logPathFile,s3AccessKey,s3SecretKey);
                    lineCount = 0;
                    fileKey = getFileKey(logPathFile);
                    base64FileKey = Base64.byteArrayToBase64(fileKey.toString()
                        .getBytes());
                    logger.info("fileKey=" + fileKey + ", base64="
                        + base64FileKey + ", " + getShortDescription());
                  } catch (Exception ex) {
                    logger.error("Error opening rolled over file. "
                        + getShortDescription());
                    // Let's add this to monitoring and exit this thread
                    logger.info("Added input to not ready list."
                        + getShortDescription());
                    isReady = false;
                    inputMgr.addToNotReady(this);
                    break;
                  }
                  logger.info("File is successfully rolled over. "
                      + getShortDescription());
                  continue;
                }
              }
              Thread.sleep(sleepStep * 1000);
              sleepStep = (sleepStep * 2);
              sleepStep = sleepStep > 10 ? 10 : sleepStep;
            } catch (InterruptedException e) {
              logger.info("Thread interrupted." + getShortDescription());
            }
          } else {
            lineCount++;
            sleepStep = 1;
            sleepIteration = 0;

            if (!resume && lineCount > resumeFromLineNumber) {
              logger.info("Resuming to read from last line. lineCount="
                  + lineCount + ", input=" + getShortDescription());
              resume = true;
            }
            if (resume) {
              InputMarker marker = new InputMarker();
              marker.base64FileKey = base64FileKey;
              marker.input = this;
              marker.lineNumber = lineCount;
              outputLine(line, marker);
            }
          }
        } catch (Throwable t) {
          final String LOG_MESSAGE_KEY = this.getClass().getSimpleName()
              + "_READ_LOOP_EXCEPTION";
          LogFeederUtil.logErrorMessageByInterval(LOG_MESSAGE_KEY,
              "Caught exception in read loop. lineNumber=" + lineCount
                  + ", input=" + getShortDescription(), t, logger, Level.ERROR);

        }
      }
    } finally {
      if (br != null) {
        logger.info("Closing reader." + getShortDescription() + ", lineCount="
            + lineCount);
        try {
          br.close();
        } catch (Throwable t) {
          // ignore
        }
      }
    }
  }

  static public Object getFileKey(String s3FilePath) {
    return s3FilePath.toString();
  }

  @Override
  public String getShortDescription() {
    return "input:source="
        + getStringValue("source")
        + ", path="
        + (s3LogPathFiles != null && s3LogPathFiles.length > 0 ? s3LogPathFiles[0]
            : getStringValue("path"));
  }

}
