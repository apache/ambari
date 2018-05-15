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

package org.apache.ambari.view.hive20.resources.jobs.viewJobs;

public class JobInfo {
  private String jobId;
  private String hiveId;
  private String dagId;
  private String operationId;

  public JobInfo() {
  }

  public JobInfo(String jobId, String hiveId, String dagId, String operationId) {
    this.jobId = jobId;
    this.hiveId = hiveId;
    this.dagId = dagId;
    this.operationId = operationId;
  }

  public String getJobId() {
    return jobId;
  }

  public void setJobId(String jobId) {
    this.jobId = jobId;
  }

  public String getHiveId() {
    return hiveId;
  }

  public void setHiveId(String hiveId) {
    this.hiveId = hiveId;
  }

  public String getDagId() {
    return dagId;
  }

  public void setDagId(String dagId) {
    this.dagId = dagId;
  }

  public String getOperationId() {
    return operationId;
  }

  public void setOperationId(String operationId) {
    this.operationId = operationId;
  }

  @Override
  public String toString() {
    return new StringBuilder().append("JobInfo{" )
      .append("jobId=").append(jobId)
      .append(", hiveId=").append(hiveId)
      .append(", dagId=").append(dagId)
      .append(", operationId=").append(operationId)
      .append('}').toString();
  }
}
