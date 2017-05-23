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
package org.apache.ambari.infra.model.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Wrapper for #{{@link JobExecution}}
 */
public class JobExecutionData {

  private JobExecution jobExecution;

  public JobExecutionData(JobExecution jobExecution) {
    this.jobExecution = jobExecution;
  }

  @JsonIgnore
  public JobExecution getJobExecution() {
    return jobExecution;
  }

  @JsonIgnore
  public Collection<StepExecution> getStepExecutions() {
    return jobExecution.getStepExecutions();
  }

  public JobParameters getJobParameters() {
    return jobExecution.getJobParameters();
  }

  public JobInstance getJobInstance() {
    return jobExecution.getJobInstance();
  }

  public Collection<StepExecutionData> getStepExecutionDataList() {
    List<StepExecutionData> stepExecutionDataList = Lists.newArrayList();
    Collection<StepExecution> stepExecutions = getStepExecutions();
    if (stepExecutions != null) {
      for (StepExecution stepExecution : stepExecutions) {
        stepExecutionDataList.add(new StepExecutionData(stepExecution));
      }
    }
    return stepExecutionDataList;
  }

  public BatchStatus getStatus() {
    return jobExecution.getStatus();
  }

  public Date getStartTime() {
    return jobExecution.getStartTime();
  }

  public Date getCreateTime() {
    return jobExecution.getCreateTime();
  }

  public Date getEndTime() {
    return jobExecution.getEndTime();
  }

  public Date getLastUpdated() {
    return jobExecution.getLastUpdated();
  }

  public ExitStatus getExitStatus() {
    return jobExecution.getExitStatus();
  }

  public ExecutionContext getExecutionContext() {
    return jobExecution.getExecutionContext();
  }

  public List<Throwable> getFailureExceptions() {
    return jobExecution.getFailureExceptions();
  }

  public String getJobConfigurationName() {
    return jobExecution.getJobConfigurationName();
  }

  public Long getId() {
    return jobExecution.getId();
  }

  public Long getJobId() {
    return jobExecution.getJobId();
  }
}
