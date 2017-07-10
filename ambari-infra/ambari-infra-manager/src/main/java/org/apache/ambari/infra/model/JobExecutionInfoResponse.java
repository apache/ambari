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

import org.apache.ambari.infra.model.wrapper.JobExecutionData;
import org.springframework.batch.admin.web.JobParametersExtractor;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.batch.core.converter.JobParametersConverter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

public class JobExecutionInfoResponse {
  private Long id;
  private int stepExecutionCount;
  private Long jobId;
  private String jobName;
  private String startDate = "";
  private String startTime = "";
  private String duration = "";
  private JobExecutionData jobExecutionData;
  private Properties jobParameters;
  private String jobParametersString;
  private boolean restartable = false;
  private boolean abandonable = false;
  private boolean stoppable = false;
  private final TimeZone timeZone;


  public JobExecutionInfoResponse(JobExecution jobExecution, TimeZone timeZone) {
    JobParametersConverter converter = new DefaultJobParametersConverter();
    this.jobExecutionData = new JobExecutionData(jobExecution);
    this.timeZone = timeZone;
    this.id = jobExecutionData.getId();
    this.jobId = jobExecutionData.getJobId();
    this.stepExecutionCount = jobExecutionData.getStepExecutions().size();
    this.jobParameters = converter.getProperties(jobExecutionData.getJobParameters());
    this.jobParametersString = (new JobParametersExtractor()).fromJobParameters(jobExecutionData.getJobParameters());
    JobInstance jobInstance = jobExecutionData.getJobInstance();
    if(jobInstance != null) {
      this.jobName = jobInstance.getJobName();
      BatchStatus endTime = jobExecutionData.getStatus();
      this.restartable = endTime.isGreaterThan(BatchStatus.STOPPING) && endTime.isLessThan(BatchStatus.ABANDONED);
      this.abandonable = endTime.isGreaterThan(BatchStatus.STARTED) && endTime != BatchStatus.ABANDONED;
      this.stoppable = endTime.isLessThan(BatchStatus.STOPPING);
    } else {
      this.jobName = "?";
    }

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    SimpleDateFormat durationFormat = new SimpleDateFormat("HH:mm:ss");

    durationFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    timeFormat.setTimeZone(timeZone);
    dateFormat.setTimeZone(timeZone);
    if(jobExecutionData.getStartTime() != null) {
      this.startDate = dateFormat.format(jobExecutionData.getStartTime());
      this.startTime = timeFormat.format(jobExecutionData.getStartTime());
      Date endTime1 = jobExecutionData.getEndTime() != null? jobExecutionData.getEndTime():new Date();
      this.duration = durationFormat.format(new Date(endTime1.getTime() - jobExecutionData.getStartTime().getTime()));
    }
  }

  public Long getId() {
    return id;
  }

  public int getStepExecutionCount() {
    return stepExecutionCount;
  }

  public Long getJobId() {
    return jobId;
  }

  public String getJobName() {
    return jobName;
  }

  public String getStartDate() {
    return startDate;
  }

  public String getStartTime() {
    return startTime;
  }

  public String getDuration() {
    return duration;
  }

  public JobExecutionData getJobExecutionData() {
    return jobExecutionData;
  }

  public Properties getJobParameters() {
    return jobParameters;
  }

  public String getJobParametersString() {
    return jobParametersString;
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

  public TimeZone getTimeZone() {
    return timeZone;
  }
}
