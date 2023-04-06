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

import javax.ws.rs.PathParam;

public class JobExecutionRequest {

  @PathParam("jobName")
  private String jobName;

  @PathParam("jobInstanceId")
  private Long jobInstanceId;

  public String getJobName() {
    return jobName;
  }

  public Long getJobInstanceId() {
    return jobInstanceId;
  }

  public void setJobName(String jobName) {
    this.jobName = jobName;
  }

  public void setJobInstanceId(Long jobInstanceId) {
    this.jobInstanceId = jobInstanceId;
  }
}
