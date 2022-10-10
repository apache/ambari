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

import static java.util.Collections.unmodifiableList;
import static org.apache.ambari.infra.model.DateUtil.toOffsetDateTime;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Properties;

import org.apache.ambari.infra.json.DurationToStringConverter;
import org.apache.ambari.infra.json.OffsetDateTimeToStringConverter;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.swagger.annotations.ApiModelProperty;

public class JobExecutionInfoResponse {
  private static final DefaultJobParametersConverter DEFAULT_JOB_PARAMETERS_CONVERTER = new DefaultJobParametersConverter();

  static {
    DEFAULT_JOB_PARAMETERS_CONVERTER.setDateFormat(new ISO8601DateFormatter());
  }

  private final Long jobExecutionId;
  private final Long jobInstanceId;
  private final String jobName;
  @JsonSerialize(converter = OffsetDateTimeToStringConverter.class)
  private final OffsetDateTime creationTime;
  @JsonSerialize(converter = OffsetDateTimeToStringConverter.class)
  private final OffsetDateTime startTime;
  @JsonSerialize(converter = OffsetDateTimeToStringConverter.class)
  private final OffsetDateTime lastUpdatedTime;
  @JsonSerialize(converter = OffsetDateTimeToStringConverter.class)
  private final OffsetDateTime endTime;
  @JsonSerialize(converter = DurationToStringConverter.class)
  @ApiModelProperty(dataType = "java.lang.String", example = "PT5.311S")
  private final Duration duration;
  private final Properties jobParameters;
  private final BatchStatus batchStatus;
  @ApiModelProperty(example = "COMPLETED", allowableValues = "UNKNOWN, EXECUTING, COMPLETED, NOOP, FAILED, STOPPED")
  private final String exitCode;
  private final String exitDescription;
  private final boolean restartable;
  private final boolean abandonable;
  private final boolean stoppable;
  private final List<Throwable> failureExceptions;
  private final String jobConfigurationName;


  public JobExecutionInfoResponse(JobExecution jobExecution) {
    this.jobExecutionId = jobExecution.getId();
    this.jobInstanceId = jobExecution.getJobId();
    this.jobParameters = DEFAULT_JOB_PARAMETERS_CONVERTER.getProperties(jobExecution.getJobParameters());
    this.creationTime = toOffsetDateTime(jobExecution.getCreateTime());
    this.startTime = toOffsetDateTime(jobExecution.getStartTime());
    this.lastUpdatedTime = toOffsetDateTime(jobExecution.getLastUpdated());
    this.endTime = toOffsetDateTime(jobExecution.getEndTime());
    JobInstance jobInstance = jobExecution.getJobInstance();
    this.batchStatus = jobExecution.getStatus();
    this.restartable = batchStatus.isGreaterThan(BatchStatus.STOPPING) && batchStatus.isLessThan(BatchStatus.ABANDONED);
    this.abandonable = batchStatus.isGreaterThan(BatchStatus.STARTED) && batchStatus != BatchStatus.ABANDONED;
    this.stoppable = batchStatus.isLessThan(BatchStatus.STOPPING);

    if (jobExecution.getExitStatus() != null) {
      this.exitCode = jobExecution.getExitStatus().getExitCode();
      this.exitDescription = jobExecution.getExitStatus().getExitDescription();
    }
    else {
      this.exitCode = null;
      this.exitDescription = null;
    }

    if(jobInstance != null) {
      this.jobName = jobInstance.getJobName();
    } else {
      this.jobName = "?";
    }

    if(startTime != null && endTime != null) {
      this.duration = Duration.between(startTime, endTime);
    }
    else {
      this.duration = null;
    }

    this.failureExceptions = unmodifiableList(jobExecution.getFailureExceptions());
    this.jobConfigurationName = jobExecution.getJobConfigurationName();
  }

  public Long getJobExecutionId() {
    return jobExecutionId;
  }

  public Long getJobInstanceId() {
    return jobInstanceId;
  }

  public String getJobName() {
    return jobName;
  }

  public OffsetDateTime getStartTime() {
    return startTime;
  }

  public Duration getDuration() {
    return duration;
  }

  public Properties getJobParameters() {
    return jobParameters;
  }

  public boolean isRestartable() {
    return restartable;
  }

  public boolean isAbandonable() {
    return abandonable;
  }

  public boolean isStoppable() {
    return stoppable;
  }

  public BatchStatus getBatchStatus() {
    return batchStatus;
  }

  public OffsetDateTime getCreationTime() {
    return creationTime;
  }

  public OffsetDateTime getEndTime() {
    return endTime;
  }

  public OffsetDateTime getLastUpdatedTime() {
    return lastUpdatedTime;
  }

  public String getExitCode() {
    return exitCode;
  }

  public String getExitDescription() {
    return exitDescription;
  }

  public List<Throwable> getFailureExceptions() {
    return this.failureExceptions;
  }

  public String getJobConfigurationName() {
    return this.jobConfigurationName;
  }
}
