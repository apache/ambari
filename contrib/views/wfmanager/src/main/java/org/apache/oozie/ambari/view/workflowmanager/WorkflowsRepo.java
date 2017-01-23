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
package org.apache.oozie.ambari.view.workflowmanager;

import org.apache.ambari.view.DataStore;
import org.apache.ambari.view.PersistenceException;
import org.apache.oozie.ambari.view.repo.BaseRepo;
import org.apache.oozie.ambari.view.workflowmanager.model.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class WorkflowsRepo extends BaseRepo<Workflow> {
  private final static Logger LOGGER = LoggerFactory
          .getLogger(WorkflowsRepo.class);
  public WorkflowsRepo(DataStore dataStore) {
    super(Workflow.class, dataStore);

  }

  public Workflow getWorkflowByPath(String path) {
    try {
      Collection<Workflow> workflows = this.dataStore.findAll(Workflow.class,
              "workflowDefinitionPath='" + path + "'");
      if (workflows == null || workflows.isEmpty()) {
        return null;
      } else if (workflows.size() > 1) {
        LOGGER.error("Duplicate workflows found having same path");
        throw new RuntimeException("Duplicate workflows");
      } else {
        return workflows.iterator().next();
      }
    } catch (PersistenceException e) {
      throw new RuntimeException(e);
    }
  }
}
