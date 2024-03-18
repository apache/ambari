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
import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;

public class ProcessFileHelper {

  private static final Logger LOG = Logger.getLogger(ProcessFileHelper.class);

  private ProcessFileHelper() {
  }

  public static void processFile(InputFile inputFile, File logPathFile, boolean follow) throws Exception {
    LOG.info("Monitoring logPath=" + inputFile.getLogPath() + ", logPathFile=" + logPathFile);
    BufferedReader br = null;

    int lineCount = 0;
    try {
      inputFile.setFilePath(logPathFile.getAbsolutePath());

      br = inputFile.openLogFile(logPathFile);

      boolean resume = true;
      int resumeFromLineNumber = inputFile.getResumeFromLineNumber();
      if (resumeFromLineNumber > 0) {
        LOG.info("Resuming log file " + logPathFile.getAbsolutePath() + " from line number " + resumeFromLineNumber);
        resume = false;
      }

      inputFile.setClosed(false);
      int sleepStep = 2;
      int sleepIteration = 0;
      while (true) {
        try {
          if (inputFile.isDrain()) {
            break;
          }

          String line = br.readLine();
          if (line == null) {
            if (!resume) {
              resume = true;
            }
            sleepIteration++;
            if (sleepIteration == 2) {
              inputFile.flush();
              if (!follow) {
                LOG.info("End of file. Done with filePath=" + logPathFile.getAbsolutePath() + ", lineCount=" + lineCount);
                break;
              }
            } else if (sleepIteration > 4) {
              Object newFileKey = inputFile.getFileKeyFromLogFile(logPathFile);
              if (newFileKey != null && (inputFile.getFileKey() == null || !newFileKey.equals(inputFile.getFileKey()))) {
                LOG.info("File key is different. Marking this input file for rollover. oldKey=" + inputFile.getFileKey() + ", newKey=" +
                  newFileKey + ". " + inputFile.getShortDescription());

                try {
                  LOG.info("File is rolled over. Closing current open file." + inputFile.getShortDescription() + ", lineCount=" +
                    lineCount);
                  br.close();
                } catch (Exception ex) {
                  LOG.error("Error closing file" + inputFile.getShortDescription(), ex);
                  break;
                }

                try {
                  LOG.info("Opening new rolled over file." + inputFile.getShortDescription());
                  br = inputFile.openLogFile(logPathFile);
                  lineCount = 0;
                } catch (Exception ex) {
                  LOG.error("Error opening rolled over file. " + inputFile.getShortDescription(), ex);
                  LOG.info("Added input to not ready list." + inputFile.getShortDescription());
                  inputFile.setReady(false);
                  inputFile.getInputManager().addToNotReady(inputFile);
                  break;
                }
                LOG.info("File is successfully rolled over. " + inputFile.getShortDescription());
                continue;
              }
            }
            try {
              Thread.sleep(sleepStep * 1000);
              sleepStep = Math.min(sleepStep * 2, 10);
            } catch (InterruptedException e) {
              LOG.info("Thread interrupted." + inputFile.getShortDescription());
            }
          } else {
            lineCount++;
            sleepStep = 1;
            sleepIteration = 0;

            if (!resume && lineCount > resumeFromLineNumber) {
              LOG.info("Resuming to read from last line. lineCount=" + lineCount + ", input=" + inputFile.getShortDescription());
              resume = true;
            }
            if (resume) {
              InputFileMarker marker = new InputFileMarker(inputFile, inputFile.getBase64FileKey(), lineCount);
              inputFile.outputLine(line, marker);
            }
          }
        } catch (Throwable t) {
          String logMessageKey = inputFile.getClass().getSimpleName() + "_READ_LOOP_EXCEPTION";
          LogFeederUtil.logErrorMessageByInterval(logMessageKey, "Caught exception in read loop. lineNumber=" + lineCount +
            ", input=" + inputFile.getShortDescription(), t, LOG, Level.ERROR);
        }
      }
    } finally {
      if (br != null) {
        LOG.info("Closing reader." + inputFile.getShortDescription() + ", lineCount=" + lineCount);
        try {
          br.close();
        } catch (Throwable t) {
          // ignore
        }
      }
    }
  }

}
