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

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.ambari.infra.model.wrapper.StepExecutionData;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class StepExecutionInfoResponse {
  private Long id;
  private Long jobExecutionId;
  private String jobName;
  private String name;
  private String startDate = "-";
  private String startTime = "-";
  private String duration = "-";
  private StepExecutionData stepExecutionData;
  private long durationMillis;

  public StepExecutionInfoResponse(String jobName, Long jobExecutionId, String name, TimeZone timeZone) {
    this.jobName = jobName;
    this.jobExecutionId = jobExecutionId;
    this.name = name;
    this.stepExecutionData = new StepExecutionData(new StepExecution(name, new JobExecution(jobExecutionId)));
  }

  public StepExecutionInfoResponse(StepExecution stepExecution, TimeZone timeZone) {
    this.stepExecutionData = new StepExecutionData(stepExecution);
    this.id = stepExecutionData.getId();
    this.name = stepExecutionData.getStepName();
    this.jobName = stepExecutionData.getJobExecution() != null && stepExecutionData.getJobExecution().getJobInstance() != null? stepExecutionData.getJobExecution().getJobInstance().getJobName():"?";
    this.jobExecutionId = stepExecutionData.getJobExecutionId();
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    SimpleDateFormat durationFormat = new SimpleDateFormat("HH:mm:ss");

    durationFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    timeFormat.setTimeZone(timeZone);
    dateFormat.setTimeZone(timeZone);
    if(stepExecutionData.getStartTime() != null) {
      this.startDate = dateFormat.format(stepExecutionData.getStartTime());
      this.startTime = timeFormat.format(stepExecutionData.getStartTime());
      Date endTime = stepExecutionData.getEndTime() != null? stepExecutionData.getEndTime():new Date();
      this.durationMillis = endTime.getTime() - stepExecutionData.getStartTime().getTime();
      this.duration = durationFormat.format(new Date(this.durationMillis));
    }

  }

  public Long getId() {
    return this.id;
  }

  public Long getJobExecutionId() {
    return this.jobExecutionId;
  }

  public String getName() {
    return this.name;
  }

  public String getJobName() {
    return this.jobName;
  }

  public String getStartDate() {
    return this.startDate;
  }

  public String getStartTime() {
    return this.startTime;
  }

  public String getDuration() {
    return this.duration;
  }

  public long getDurationMillis() {
    return this.durationMillis;
  }

  public String getStatus() {
    return this.id != null?this.stepExecutionData.getStatus().toString():"NONE";
  }

  public String getExitCode() {
    return this.id != null?this.stepExecutionData.getExitStatus().getExitCode():"NONE";
  }

  @JsonIgnore
  public StepExecutionData getStepExecution() {
    return this.stepExecutionData;
  }
}
