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
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;

import java.util.Date;
import java.util.List;

/**
 * Wrapper for #{{@link StepExecution}}
 */
public class StepExecutionData {

  @JsonIgnore
  private final JobExecution jobExecution;

  @JsonIgnore
  private final StepExecution stepExecution;


  public StepExecutionData(StepExecution stepExecution) {
    this.stepExecution = stepExecution;
    this.jobExecution = stepExecution.getJobExecution();
  }

  @JsonIgnore
  public JobExecution getJobExecution() {
    return jobExecution;
  }

  @JsonIgnore
  public StepExecution getStepExecution() {
    return stepExecution;
  }

  public String getStepName() {
    return stepExecution.getStepName();
  }

  public int getReadCount() {
    return stepExecution.getReadCount();
  }

  public BatchStatus getStatus() {
    return stepExecution.getStatus();
  }

  public int getWriteCount() {
    return stepExecution.getWriteCount();
  }

  public int getCommitCount() {
    return stepExecution.getCommitCount();
  }

  public int getRollbackCount() {
    return stepExecution.getRollbackCount();
  }

  public int getReadSkipCount() {
    return stepExecution.getReadSkipCount();
  }

  public int getProcessSkipCount() {
    return stepExecution.getProcessSkipCount();
  }

  public Date getStartTime() {
    return stepExecution.getStartTime();
  }

  public int getWriteSkipCount() {
    return stepExecution.getWriteSkipCount();
  }

  public Date getEndTime() {
    return stepExecution.getEndTime();
  }

  public Date getLastUpdated() {
    return stepExecution.getLastUpdated();
  }

  public ExecutionContext getExecutionContext() {
    return stepExecution.getExecutionContext();
  }

  public ExitStatus getExitStatus() {
    return stepExecution.getExitStatus();
  }

  public boolean isTerminateOnly() {
    return stepExecution.isTerminateOnly();
  }

  public int getFilterCount() {
    return stepExecution.getFilterCount();
  }

  public List<Throwable> getFailureExceptions() {
    return stepExecution.getFailureExceptions();
  }

  public Long getId() {
    return stepExecution.getId();
  }

  public Long getJobExecutionId() {
    return stepExecution.getJobExecutionId();
  }
}
