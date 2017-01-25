/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.oozie.ambari.view;

import java.io.IOException;
import java.io.InputStream;

import org.apache.hadoop.fs.FileStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkflowFilesService {
  private final static Logger LOGGER = LoggerFactory
          .getLogger(WorkflowFilesService.class);
  private HDFSFileUtils hdfsFileUtils;

  public WorkflowFilesService(HDFSFileUtils hdfsFileUtils) {
    super();
    this.hdfsFileUtils = hdfsFileUtils;
  }

  public String createFile(String appPath, String content,
                           boolean overwrite) throws IOException {
    return hdfsFileUtils.writeToFile(appPath, content,
            overwrite);
  }

  public String createAssetFile(String appPath, String content,
                                boolean overwrite) throws IOException {
    return hdfsFileUtils.writeToFile(appPath, content,
            overwrite);
  }

  public InputStream readDraft(String appPath) throws IOException {
    return hdfsFileUtils.read(getWorkflowDraftFileName(appPath));
  }

  public InputStream readWorkflowXml(String appPath) throws IOException {
    return hdfsFileUtils.read(getWorkflowFileName(appPath));
  }

  public InputStream readAssset(String assetPath) throws IOException {
    return hdfsFileUtils.read(getAssetFileName(assetPath));
  }

  public String getWorkflowDraftFileName(String appPath) {
    if (appPath.endsWith(Constants.WF_DRAFT_EXTENSION)) {
      return appPath;
    } else if (appPath.endsWith(Constants.WF_EXTENSION)) {
      String folderPath = appPath.substring(0, appPath.lastIndexOf(Constants.WF_EXTENSION));
      return folderPath + Constants.WF_DRAFT_EXTENSION;
    } else if (appPath.endsWith("/")) {
      return appPath + Constants.DEFAULT_DRAFT_FILENAME;
    } else {
      return appPath + "/" + Constants.DEFAULT_DRAFT_FILENAME;
    }
  }

  public String getWorkflowFileName(String appPath) {
    if (appPath.endsWith(Constants.WF_EXTENSION)) {
      return appPath;
    } else if (appPath.endsWith(Constants.WF_DRAFT_EXTENSION)) {
      String folderPath = appPath.substring(0, appPath.lastIndexOf(Constants.WF_DRAFT_EXTENSION));
      return folderPath + Constants.WF_EXTENSION;
    } else if (appPath.endsWith("/")) {
      return appPath + Constants.DEFAULT_WORKFLOW_FILENAME;
    } else {
      return appPath + "/" + Constants.DEFAULT_WORKFLOW_FILENAME;
    }
  }

  public String getAssetFileName(String appPath) {
    String assetFile = null;
    if (appPath.endsWith(Constants.WF_ASSET_EXTENSION)) {
      assetFile = appPath;
    } else {
      assetFile = appPath + (appPath.endsWith("/") ? "" : "/")
              + Constants.DEFAULT_WORKFLOW_ASSET_FILENAME;
    }
    return assetFile;
  }

  public void discardDraft(String workflowPath) throws IOException {
    hdfsFileUtils.deleteFile(getWorkflowDraftFileName(workflowPath));
  }

  public WorkflowFileInfo getWorkflowDetails(String appPath) {
    WorkflowFileInfo workflowInfo = new WorkflowFileInfo();
    workflowInfo.setWorkflowPath(getWorkflowFileName(appPath));
    boolean draftExists = hdfsFileUtils
            .fileExists(getWorkflowDraftFileName(appPath));
    workflowInfo.setDraftExists(draftExists);
    boolean workflowExists = hdfsFileUtils.fileExists(getWorkflowFileName(appPath));
    FileStatus workflowFileStatus = null;
    if (workflowExists) {
      workflowFileStatus = hdfsFileUtils
              .getFileStatus(getWorkflowFileName(appPath));
      workflowInfo.setWorkflowModificationTime(workflowFileStatus
              .getModificationTime());
    }
    if (draftExists) {
      FileStatus draftFileStatus = hdfsFileUtils
              .getFileStatus(getWorkflowDraftFileName(appPath));
      workflowInfo.setDraftModificationTime(draftFileStatus
              .getModificationTime());
      if (!workflowExists) {
        workflowInfo.setIsDraftCurrent(true);
      } else {
        workflowInfo.setIsDraftCurrent(draftFileStatus.getModificationTime()
                - workflowFileStatus.getModificationTime() > 0);
      }
    }
    return workflowInfo;
  }
  public void deleteWorkflowFile(String fullWorkflowFilePath){
    try {
      hdfsFileUtils.deleteFile(fullWorkflowFilePath);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}