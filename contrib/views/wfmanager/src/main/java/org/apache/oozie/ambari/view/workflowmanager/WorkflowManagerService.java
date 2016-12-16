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
package org.apache.oozie.ambari.view.workflowmanager;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;

import org.apache.ambari.view.ViewContext;
import org.apache.oozie.ambari.view.HDFSFileUtils;
import org.apache.oozie.ambari.view.JobType;
import org.apache.oozie.ambari.view.workflowmanager.model.Workflow;

public class WorkflowManagerService {

    private WorkflowsRepo workflowsRepository;
    private HDFSFileUtils hdfsFileUtils;

    public WorkflowManagerService(ViewContext viewContext) {
        workflowsRepository = new WorkflowsRepo(viewContext.getDataStore());
        hdfsFileUtils = new HDFSFileUtils(viewContext);
    }

    public void saveWorkflow(String path, JobType jobType, String descripton,
            String userName) {
        // workflowsRepository.getWorkflow(path);
        Workflow workflowByPath = getWorkflowByPath(path);
        if (workflowByPath == null) {
            Workflow wf = new Workflow();
            wf.setOwner(userName);
            wf.setType(jobType.name());
            wf.setWorkflowDefinitionPath(path);
            Date now = new Date();
            wf.setUpdatedAt(String.valueOf(now.getTime()));
            workflowsRepository.updateWorkflow(wf);
        } else {
            Date now = new Date();
            workflowByPath.setUpdatedAt(String.valueOf(now.getTime()));
            workflowsRepository.updateWorkflow(workflowByPath);
        }
    }

    public Collection<Workflow> getAllWorkflows() {
        return workflowsRepository.getAllWorkflows();
    }

    public Workflow getWorkflowByPath(String path) {
        return workflowsRepository.getWorkflow(path);
    }

    public void deleteWorkflow(String path, Boolean deleteDefinition) {
        if (deleteDefinition) {
            try {
                hdfsFileUtils.deleteFile(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        workflowsRepository.deleteWorkflow(path);
    }
}
