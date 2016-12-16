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

import java.util.Collection;

import org.apache.ambari.view.DataStore;
import org.apache.ambari.view.PersistenceException;
import org.apache.oozie.ambari.view.workflowmanager.model.Workflow;

public class WorkflowsRepo {
    private final DataStore dataStore;

    public WorkflowsRepo(DataStore dataStore) {
        super();
        this.dataStore=dataStore;
    }
    public Collection<Workflow> getAllWorkflows(){
        try {
            return dataStore.findAll(Workflow.class,null);
        } catch (PersistenceException e) {
           throw new RuntimeException(e);
        }
    }
    public void deleteWorkflow(String workflowPath){
        try {
            Workflow workflow = this.getWorkflow(workflowPath);
            this.dataStore.remove(workflow);
        } catch (PersistenceException e) {
           throw new RuntimeException(e);
        }
    }
    public void createWorkflow(Workflow wf){
        try {
            this.dataStore.store(wf);
        } catch (PersistenceException e) {
           throw new RuntimeException(e);
        }
    }
    public void updateWorkflow(Workflow wf){
        try {
            this.dataStore.store(wf);
        } catch (PersistenceException e) {
           throw new RuntimeException(e);
        }
    }
    public Workflow getWorkflow(String path) {
        try {
            return this.dataStore.find(Workflow.class, "workflowDefinitionPath='"+path+"'");
        } catch (PersistenceException e) {
            throw new RuntimeException(e);
        }
    }
}
