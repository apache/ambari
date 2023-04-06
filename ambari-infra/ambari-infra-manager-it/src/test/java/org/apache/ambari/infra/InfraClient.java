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
package org.apache.ambari.infra;

import java.util.List;

import org.apache.ambari.infra.client.api.JobsApi;
import org.apache.ambari.infra.client.invoker.ApiClient;
import org.apache.ambari.infra.client.invoker.ApiException;
import org.apache.ambari.infra.client.model.JobExecutionInfoResponse;

public class InfraClient {
  private final JobsApi jobsApi;

  public InfraClient(String baseUrl) {
    ApiClient apiClient = new ApiClient().setBasePath(baseUrl);
    apiClient.setUsername("admin");
    apiClient.setPassword("admin");
    this.jobsApi = new JobsApi(apiClient);
  }

  public List<String> getJobs() {
    try {
      return jobsApi.getAllJobNames();
    } catch (ApiException e) {
      throw new RuntimeException(e);
    }
  }

  public JobExecutionInfoResponse startJob(String jobName, String parameters) {
    try {
      return jobsApi.startJob(jobName, parameters);
    } catch (ApiException e) {
      throw new RuntimeException(e);
    }
  }

  public void restartJob(String jobName, long jobId) {
    try {
      jobsApi.restartJobInstance(jobName, jobId, "RESTART");
    } catch (ApiException e) {
      throw new RuntimeException(e);
    }
  }

  public void stopJob(long jobExecutionId) {
    try {
      jobsApi.stopOrAbandonJobExecution(jobExecutionId, "STOP");
    } catch (ApiException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean isRunning(String jobName) {
    try {
      return !jobsApi.getExecutionIdsByJobName(jobName).isEmpty();
    } catch (ApiException e) {
      throw new RuntimeException(e);
    }
  }
}
