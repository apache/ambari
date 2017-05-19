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

import java.util.Map;

public class StepExecutionContextResponse {

  private Map<String, Object> executionContextMap;

  private Long jobExecutionId;

  private Long stepExecutionId;

  private String stepName;

  public StepExecutionContextResponse() {
  }

  public StepExecutionContextResponse(Map<String, Object> executionContextMap, Long jobExecutionId, Long stepExecutionId, String stepName) {
    this.executionContextMap = executionContextMap;
    this.jobExecutionId = jobExecutionId;
    this.stepExecutionId = stepExecutionId;
    this.stepName = stepName;
  }

  public Map<String, Object> getExecutionContextMap() {
    return executionContextMap;
  }

  public Long getJobExecutionId() {
    return jobExecutionId;
  }

  public Long getStepExecutionId() {
    return stepExecutionId;
  }

  public String getStepName() {
    return stepName;
  }
}
