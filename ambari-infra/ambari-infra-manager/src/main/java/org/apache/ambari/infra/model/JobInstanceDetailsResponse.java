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
package org.apache.ambari.infra.model;

import org.springframework.batch.core.JobInstance;

import java.util.List;

public class JobInstanceDetailsResponse {

  private JobInstance jobInstance;

  private List<JobExecutionInfoResponse> jobExecutionInfoResponseList;

  public JobInstanceDetailsResponse() {
  }

  public JobInstanceDetailsResponse(JobInstance jobInstance, List<JobExecutionInfoResponse> jobExecutionInfoResponseList) {
    this.jobInstance = jobInstance;
    this.jobExecutionInfoResponseList = jobExecutionInfoResponseList;
  }

  public JobInstance getJobInstance() {
    return jobInstance;
  }

  public void setJobInstance(JobInstance jobInstance) {
    this.jobInstance = jobInstance;
  }

  public List<JobExecutionInfoResponse> getJobExecutionInfoResponseList() {
    return jobExecutionInfoResponseList;
  }

  public void setJobExecutionInfoResponseList(List<JobExecutionInfoResponse> jobExecutionInfoResponseList) {
    this.jobExecutionInfoResponseList = jobExecutionInfoResponseList;
  }
}
