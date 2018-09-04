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
package org.apache.ambari.logfeeder.input.file.checkpoint;

import org.apache.ambari.logfeeder.conf.LogFeederProps;
import org.apache.ambari.logfeeder.input.InputFile;
import org.apache.ambari.logfeeder.input.InputFileMarker;
import org.apache.ambari.logfeeder.input.file.checkpoint.util.FileCheckInHelper;
import org.apache.ambari.logfeeder.input.file.checkpoint.util.FileCheckpointCleanupHelper;
import org.apache.ambari.logfeeder.input.file.checkpoint.util.ResumeLineNumberHelper;
import org.apache.ambari.logfeeder.input.monitor.CheckpointCleanupMonitor;
import org.apache.ambari.logfeeder.plugin.manager.CheckpointManager;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class FileCheckpointManager implements CheckpointManager<InputFile, InputFileMarker, LogFeederProps> {

  private static final Logger LOG = LoggerFactory.getLogger(FileCheckpointManager.class);

  private static final String CHECKPOINT_SUBFOLDER_NAME = "logfeeder_checkpoints";

  private String checkPointExtension;
  private String checkPointFolder;
  private File checkPointFolderFile;

  @Override
  public void init(LogFeederProps logFeederProps) {
    checkPointExtension = logFeederProps.getCheckPointExtension();
    LOG.info("Determining valid checkpoint folder");
    boolean isCheckPointFolderValid = false;
    // We need to keep track of the files we are reading.
    checkPointFolder = logFeederProps.getCheckpointFolder();
    if (!StringUtils.isEmpty(checkPointFolder)) {
      checkPointFolderFile = new File(checkPointFolder);
      isCheckPointFolderValid = verifyCheckPointFolder(checkPointFolderFile);
    }

    if (!isCheckPointFolderValid) {
      // Let's use tmp folder
      checkPointFolderFile = new File(logFeederProps.getTmpDir(), CHECKPOINT_SUBFOLDER_NAME);
      LOG.info("Checking if tmp folder can be used for checkpoints. Folder=" + checkPointFolderFile);
      isCheckPointFolderValid = verifyCheckPointFolder(checkPointFolderFile);
      if (isCheckPointFolderValid) {
        LOG.warn("Using tmp folder " + checkPointFolderFile + " to store check points. This is not recommended." +
          "Please set logfeeder.checkpoint.folder property");
      }
    }

    if (isCheckPointFolderValid) {
      LOG.info("Using folder " + checkPointFolderFile + " for storing checkpoints");
      // check checkpoint cleanup every 2000 min
      Thread checkpointCleanupThread = new Thread(new CheckpointCleanupMonitor(this, 2000),"checkpoint_cleanup");
      checkpointCleanupThread.setDaemon(true);
      checkpointCleanupThread.start();
    } else {
      throw new IllegalStateException("Could not determine the checkpoint folder.");
    }
  }

  @Override
  public void checkIn(InputFile inputFile, InputFileMarker inputMarker) {
    FileCheckInHelper.checkIn(inputFile, inputMarker);
  }

  @Override
  public int resumeLineNumber(InputFile inputFile) {
    return ResumeLineNumberHelper.getResumeFromLineNumber(inputFile, checkPointFolderFile);
  }

  @Override
  public void cleanupCheckpoints() {
    FileCheckpointCleanupHelper.cleanCheckPointFiles(checkPointFolderFile, checkPointExtension);
  }

  private boolean verifyCheckPointFolder(File folderPathFile) {
    if (!folderPathFile.exists()) {
      try {
        if (!folderPathFile.mkdir()) {
          LOG.warn("Error creating folder for check point. folder=" + folderPathFile);
        }
      } catch (Throwable t) {
        LOG.warn("Error creating folder for check point. folder=" + folderPathFile, t);
      }
    }

    if (folderPathFile.exists() && folderPathFile.isDirectory()) {
      // Let's check whether we can create a file
      File testFile = new File(folderPathFile, UUID.randomUUID().toString());
      try {
        testFile.createNewFile();
        return testFile.delete();
      } catch (IOException e) {
        LOG.warn("Couldn't create test file in " + folderPathFile.getAbsolutePath() + " for checkPoint", e);
      }
    }
    return false;
  }

}
