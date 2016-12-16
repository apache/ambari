/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
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

public class WorkflowFilesService {
	private HDFSFileUtils hdfsFileUtils;

	public WorkflowFilesService(HDFSFileUtils hdfsFileUtils) {
		super();
		this.hdfsFileUtils = hdfsFileUtils;
	}

	public String createWorkflowFile(String appPath, String content,
			boolean overwrite) throws IOException {
		return hdfsFileUtils.writeToFile(getWorkflowFileName(appPath), content,
				overwrite);
	}
	
	public String createAssetFile(String appPath, String content,
			boolean overwrite) throws IOException {
		return hdfsFileUtils.writeToFile(appPath, content,
				overwrite);
	}

	public String saveDraft(String appPath, String content, boolean overwrite)
			throws IOException {
		return hdfsFileUtils.writeToFile(getWorkflowDrafFileName(appPath),
				content, overwrite);
	}

	public InputStream readDraft(String appPath) throws IOException {
		return hdfsFileUtils.read(getWorkflowDrafFileName(appPath));
	}
	public InputStream readWorkflowXml(String appPath) throws IOException {
		return hdfsFileUtils.read(getWorkflowFileName(appPath));
	}

	private String getWorkflowDrafFileName(String appPath) {
		return getWorkflowFileName(appPath).concat(".draft.json");
	}

	private String getWorkflowFileName(String appPath) {
		String workflowFile = null;
		if (appPath.endsWith(".xml")) {
			workflowFile = appPath;
		} else {
			workflowFile = appPath + (appPath.endsWith("/") ? "" : "/")
					+ "workflow.xml";
		}
		return workflowFile;
	}
	
	public String getAssetFileName(String appPath) {
		String assetFile = null;
		if (appPath.endsWith(".xml")) {
			assetFile = appPath;
		} else {
			assetFile = appPath + (appPath.endsWith("/") ? "" : "/")
					+ "asset.xml";
		}
		return assetFile;
	}

	public void discardDraft(String workflowPath) throws IOException {
		hdfsFileUtils.deleteFile(getWorkflowDrafFileName(workflowPath));

	}

	public WorkflowFileInfo getWorkflowDetails(String appPath) {
		WorkflowFileInfo workflowInfo = new WorkflowFileInfo();
		workflowInfo.setWorkflowPath(getWorkflowFileName(appPath));
		boolean draftExists = hdfsFileUtils
				.fileExists(getWorkflowDrafFileName(appPath));
		workflowInfo.setDraftExists(draftExists);
		boolean workflowExists = hdfsFileUtils.fileExists(getWorkflowFileName(appPath));
		FileStatus workflowFileStatus = null;
		if (workflowExists){
			workflowFileStatus = hdfsFileUtils
					.getFileStatus(getWorkflowFileName(appPath));
			workflowInfo.setWorkflowModificationTime(workflowFileStatus
					.getModificationTime());
		}
		if (draftExists) {
			FileStatus draftFileStatus = hdfsFileUtils
					.getFileStatus(getWorkflowDrafFileName(appPath));
			workflowInfo.setDraftModificationTime(draftFileStatus
					.getModificationTime());
			if (!workflowExists){
				workflowInfo.setIsDraftCurrent(true);
			}else{
				workflowInfo.setIsDraftCurrent(draftFileStatus.getModificationTime()
						- workflowFileStatus.getModificationTime() > 0);
			}
		}
		return workflowInfo;
	}
}
