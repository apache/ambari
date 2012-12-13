/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.eventdb.db;

import java.io.IOException;
import java.util.List;

import org.apache.ambari.eventdb.model.Jobs.JobDBEntry;
import org.apache.ambari.eventdb.model.TaskAttempt;
import org.apache.ambari.eventdb.model.WorkflowContext;
import org.apache.ambari.eventdb.model.Workflows.WorkflowDBEntry;

public interface DBConnector {
  public void submitJob(JobDBEntry j, WorkflowContext context) throws IOException;
  
  public void updateJob(JobDBEntry j) throws IOException;
  
  public List<WorkflowDBEntry> fetchWorkflows() throws IOException;
  
  public List<JobDBEntry> fetchJobDetails(String workflowID) throws IOException;
  
  public long[] fetchJobStartStopTimes(String jobID) throws IOException;
  
  public List<TaskAttempt> fetchTaskAttempts(String jobID, String taskType) throws IOException;
  
  public void close();
}
