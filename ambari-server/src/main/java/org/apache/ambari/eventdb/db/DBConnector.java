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

import org.apache.ambari.eventdb.model.DataTable;
import org.apache.ambari.eventdb.model.Jobs.JobDBEntry;
import org.apache.ambari.eventdb.model.TaskAttempt;
import org.apache.ambari.eventdb.model.WorkflowContext;
import org.apache.ambari.eventdb.model.Workflows;
import org.apache.ambari.eventdb.model.Workflows.WorkflowDBEntry.WorkflowFields;

public interface DBConnector {
  public void submitJob(JobDBEntry j, WorkflowContext context) throws IOException;
  
  public void updateJob(JobDBEntry j) throws IOException;
  
  public Workflows fetchWorkflows() throws IOException;
  
  public Workflows fetchWorkflows(WorkflowFields field, boolean sortAscending, int offset, int limit) throws IOException;
  
  public DataTable fetchWorkflows(int offset, int limit, String searchTerm, int echo, WorkflowFields field, boolean sortAscending, String searchWorkflowId,
      String searchWorkflowName, String searchWorkflowType, String searchUserName, int minJobs, int maxJobs, long minInputBytes, long maxInputBytes,
      long minOutputBytes, long maxOutputBytes, long minDuration, long maxDuration, long minStartTime, long maxStartTime, long minFinishTime, long maxFinishTime)
      throws IOException;
  
  public List<JobDBEntry> fetchJobDetails(String workflowID) throws IOException;
  
  public List<JobDBEntry> fetchJobDetails(long minFinishTime, long maxStartTime) throws IOException;
  
  public long[] fetchJobStartStopTimes(String jobID) throws IOException;
  
  public List<TaskAttempt> fetchJobTaskAttempts(String jobID) throws IOException;
  
  public List<TaskAttempt> fetchWorkflowTaskAttempts(String workflowID) throws IOException;
  
  public List<TaskAttempt> fetchTaskAttempts(long minFinishTime, long maxStartTime) throws IOException;
  
  public void close();
}
