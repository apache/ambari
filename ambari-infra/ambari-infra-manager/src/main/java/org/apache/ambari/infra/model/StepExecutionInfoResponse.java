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

import static org.apache.ambari.infra.model.DateUtil.toOffsetDateTime;

import java.time.Duration;
import java.time.OffsetDateTime;

import org.apache.ambari.infra.json.DurationToStringConverter;
import org.apache.ambari.infra.json.OffsetDateTimeToStringConverter;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.StepExecution;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.swagger.annotations.ApiModelProperty;

public class StepExecutionInfoResponse {
  private final Long stepExecutionId;
  private final Long jobExecutionId;
  private final String jobName;
  private final String stepName;
  @JsonSerialize(converter = OffsetDateTimeToStringConverter.class)
  private final OffsetDateTime startTime;
  @JsonSerialize(converter = OffsetDateTimeToStringConverter.class)
  private final OffsetDateTime endTime;
  @JsonSerialize(converter = DurationToStringConverter.class)
  @ApiModelProperty(dataType = "java.lang.String", example = "PT5.311S")
  private final Duration duration;
  private final BatchStatus batchStatus;
  @ApiModelProperty(example = "COMPLETED", allowableValues = "UNKNOWN, EXECUTING, COMPLETED, NOOP, FAILED, STOPPED")
  private final String exitCode;
  private final String exitDescription;


  public StepExecutionInfoResponse(StepExecution stepExecution) {
    this.stepExecutionId = stepExecution.getId();
    this.stepName = stepExecution.getStepName();
    this.jobName = stepExecution.getJobExecution() != null && stepExecution.getJobExecution().getJobInstance() != null ? stepExecution.getJobExecution().getJobInstance().getJobName() : "?";
    this.jobExecutionId = stepExecution.getJobExecutionId();
    this.startTime = toOffsetDateTime(stepExecution.getStartTime());
    this.endTime = toOffsetDateTime(stepExecution.getEndTime());

    if(this.startTime != null && this.endTime != null) {
      this.duration = Duration.between(this.startTime, this.endTime);
    }
    else {
      this.duration = null;
    }

    this.batchStatus = stepExecution.getStatus();
    if (stepExecution.getExitStatus() != null) {
      this.exitCode = stepExecution.getExitStatus().getExitCode();
      this.exitDescription = stepExecution.getExitStatus().getExitDescription();
    }
    else {
      this.exitCode = null;
      this.exitDescription = null;
    }
  }

  public Long getStepExecutionId() {
    return this.stepExecutionId;
  }

  public Long getJobExecutionId() {
    return this.jobExecutionId;
  }

  public String getStepName() {
    return this.stepName;
  }

  public String getJobName() {
    return this.jobName;
  }

  public OffsetDateTime getStartTime() {
    return startTime;
  }

  public OffsetDateTime getEndTime() {
    return endTime;
  }

  public Duration getDuration() {
    return duration;
  }

  public BatchStatus getBatchStatus() {
    return batchStatus;
  }

  public String getExitCode() {
    return exitCode;
  }

  public String getExitDescription() {
    return exitDescription;
  }
}
