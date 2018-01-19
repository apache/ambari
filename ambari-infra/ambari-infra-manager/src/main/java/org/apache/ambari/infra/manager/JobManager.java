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
package org.apache.ambari.infra.manager;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.apache.ambari.infra.model.ExecutionContextResponse;
import org.apache.ambari.infra.model.JobDetailsResponse;
import org.apache.ambari.infra.model.JobExecutionDetailsResponse;
import org.apache.ambari.infra.model.JobExecutionInfoResponse;
import org.apache.ambari.infra.model.JobInstanceDetailsResponse;
import org.apache.ambari.infra.model.JobOperationParams;
import org.apache.ambari.infra.model.StepExecutionContextResponse;
import org.apache.ambari.infra.model.StepExecutionInfoResponse;
import org.apache.ambari.infra.model.StepExecutionProgressResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.admin.history.StepExecutionHistory;
import org.springframework.batch.admin.service.JobService;
import org.springframework.batch.admin.service.NoSuchStepExecutionException;
import org.springframework.batch.admin.web.JobInfo;
import org.springframework.batch.admin.web.StepExecutionProgress;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyExistsException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.launch.NoSuchJobInstanceException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

@Named
public class JobManager {

  private static final Logger LOG = LoggerFactory.getLogger(JobManager.class);

  @Inject
  private JobService jobService;

  @Inject
  private JobOperator jobOperator;

  private TimeZone timeZone = TimeZone.getDefault();

  public Set<String> getAllJobNames() {
    return jobOperator.getJobNames();
  }

  /**
   * Launch a new job instance (based on job name) and applies customized parameters to it.
   * Also add a new date parameter to make sure the job instance will be unique
   */
  public JobExecutionInfoResponse launchJob(String jobName, String params)
    throws JobParametersInvalidException, JobInstanceAlreadyExistsException, NoSuchJobException,
    JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException {
    JobParametersBuilder jobParametersBuilder = new JobParametersBuilder();
    if (params != null) {
      LOG.info("Parsing parameters of job {} '{}'", jobName, params);
      Splitter.on(',')
              .trimResults()
              .withKeyValueSeparator(Splitter.on('=').limit(2).trimResults())
              .split(params).entrySet().forEach(entry -> jobParametersBuilder.addString(entry.getKey(), entry.getValue()));
    }
    return new JobExecutionInfoResponse(jobService.launch(jobName, jobParametersBuilder.toJobParameters()), timeZone);
  }

  /**
   * Get all executions ids that mapped to specific job name,
   */
  public Set<Long> getExecutionIdsByJobName(String jobName) throws NoSuchJobException {
    return jobOperator.getRunningExecutions(jobName);
  }

  /**
   * Stop all running job executions and returns with the number of stopped jobs.
   */
  public Integer stopAllJobs() {
    return jobService.stopAll();
  }

  /**
   * Gather job execution details by job execution id.
   */
  public JobExecutionDetailsResponse getExectionInfo(Long jobExecutionId) throws NoSuchJobExecutionException {
    JobExecution jobExecution = jobService.getJobExecution(jobExecutionId);
    List<StepExecutionInfoResponse> stepExecutionInfos = new ArrayList<StepExecutionInfoResponse>();
    for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
      stepExecutionInfos.add(new StepExecutionInfoResponse(stepExecution, timeZone));
    }
    Collections.sort(stepExecutionInfos, new Comparator<StepExecutionInfoResponse>() {
      @Override
      public int compare(StepExecutionInfoResponse o1, StepExecutionInfoResponse o2) {
        return o1.getId().compareTo(o2.getId());
      }
    });
    return new JobExecutionDetailsResponse(new JobExecutionInfoResponse(jobExecution, timeZone), stepExecutionInfos);
  }

  /**
   * Stop or abandon a running job execution by job execution id
   */
  public JobExecutionInfoResponse stopOrAbandonJobByExecutionId(Long jobExecutionId, JobOperationParams.JobStopOrAbandonOperationParam operation)
    throws NoSuchJobExecutionException, JobExecutionNotRunningException, JobExecutionAlreadyRunningException {
    JobExecution jobExecution;
    if (JobOperationParams.JobStopOrAbandonOperationParam.STOP.equals(operation)) {
      jobExecution = jobService.stop(jobExecutionId);
    } else if (JobOperationParams.JobStopOrAbandonOperationParam.ABANDON.equals(operation)) {
      jobExecution = jobService.abandon(jobExecutionId);
    } else {
      throw new UnsupportedOperationException("Unsupported operaration");
    }
    return new JobExecutionInfoResponse(jobExecution, timeZone);
  }

  /**
   * Get execution context for a job execution instance. (context can be shipped between job executions)
   */
  public ExecutionContextResponse getExecutionContextByJobExecutionId(Long executionId) throws NoSuchJobExecutionException {
    JobExecution jobExecution = jobService.getJobExecution(executionId);
    Map<String, Object> executionMap = new HashMap<>();
    for (Map.Entry<String, Object> entry : jobExecution.getExecutionContext().entrySet()) {
      executionMap.put(entry.getKey(), entry.getValue());
    }
    return new ExecutionContextResponse(executionId, executionMap);
  }

  /**
   * Restart a specific job instance with the same parameters. (only restart operation is supported here)
   */
  public JobExecutionInfoResponse restart(Long jobInstanceId, String jobName,
                                          JobOperationParams.JobRestartOperationParam operation) throws NoSuchJobException, JobParametersInvalidException,
    JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException, NoSuchJobExecutionException {
    if (JobOperationParams.JobRestartOperationParam.RESTART.equals(operation)) {
      Collection<JobExecution> jobExecutions = jobService.getJobExecutionsForJobInstance(jobName, jobInstanceId);
      JobExecution jobExecution = jobExecutions.iterator().next();
      Long jobExecutionId = jobExecution.getId();
      return new JobExecutionInfoResponse(jobService.restart(jobExecutionId), timeZone);
    } else {
      throw new UnsupportedOperationException("Unsupported operation (try: RESTART)");
    }
  }

  /**
   * Get all job details. (paged)
   */
  public List<JobInfo> getAllJobs(int start, int pageSize) {
    List<JobInfo> jobs = new ArrayList<>();
    Collection<String> names = jobService.listJobs(start, pageSize);
    for (String name : names) {
      int count = 0;
      try {
        count = jobService.countJobExecutionsForJob(name);
      }
      catch (NoSuchJobException e) {
        // shouldn't happen
      }
      boolean launchable = jobService.isLaunchable(name);
      boolean incrementable = jobService.isIncrementable(name);
      jobs.add(new JobInfo(name, count, null, launchable, incrementable));
    }
    return jobs;
  }

  /**
   * Get all executions for unique job instance.
   */
  public List<JobExecutionInfoResponse> getExecutionsForJobInstance(String jobName, Long jobInstanceId) throws NoSuchJobInstanceException, NoSuchJobException {
    List<JobExecutionInfoResponse> result = Lists.newArrayList();
    JobInstance jobInstance = jobService.getJobInstance(jobInstanceId);
    Collection<JobExecution> jobExecutions = jobService.getJobExecutionsForJobInstance(jobName, jobInstance.getInstanceId());
    for (JobExecution jobExecution : jobExecutions) {
      result.add(new JobExecutionInfoResponse(jobExecution, timeZone));
    }
    return result;
  }

  /**
   * Get job details for a specific job. (paged)
   */
  public JobDetailsResponse getJobDetails(String jobName, int page, int size) throws NoSuchJobException {
    List<JobInstanceDetailsResponse> jobInstanceResponses = Lists.newArrayList();
    Collection<JobInstance> jobInstances = jobService.listJobInstances(jobName, page, size);

    int count = jobService.countJobExecutionsForJob(jobName);
    boolean launchable = jobService.isLaunchable(jobName);
    boolean isIncrementable = jobService.isIncrementable(jobName);

    for (JobInstance jobInstance: jobInstances) {
      List<JobExecutionInfoResponse> executionInfos = Lists.newArrayList();
      Collection<JobExecution> jobExecutions = jobService.getJobExecutionsForJobInstance(jobName, jobInstance.getId());
      if (jobExecutions != null) {
        for (JobExecution jobExecution : jobExecutions) {
          executionInfos.add(new JobExecutionInfoResponse(jobExecution, timeZone));
        }
      }
      jobInstanceResponses.add(new JobInstanceDetailsResponse(jobInstance, executionInfos));
    }
    return new JobDetailsResponse(new JobInfo(jobName, count, launchable, isIncrementable), jobInstanceResponses);
  }

  /**
   * Get step execution details based for job execution id and step execution id.
   */
  public StepExecutionInfoResponse getStepExecution(Long jobExecutionId, Long stepExecutionId) throws NoSuchStepExecutionException, NoSuchJobExecutionException {
    StepExecution stepExecution = jobService.getStepExecution(jobExecutionId, stepExecutionId);
    return new StepExecutionInfoResponse(stepExecution, timeZone);
  }

  /**
   * Get step execution context details. (execution context can be shipped between steps)
   */
  public StepExecutionContextResponse getStepExecutionContext(Long jobExecutionId, Long stepExecutionId) throws NoSuchStepExecutionException, NoSuchJobExecutionException {
    StepExecution stepExecution = jobService.getStepExecution(jobExecutionId, stepExecutionId);
    Map<String, Object> executionMap = new HashMap<>();
    for (Map.Entry<String, Object> entry : stepExecution.getExecutionContext().entrySet()) {
      executionMap.put(entry.getKey(), entry.getValue());
    }
    return new StepExecutionContextResponse(executionMap, jobExecutionId, stepExecutionId, stepExecution.getStepName());
  }

  /**
   * Get step execution progress status detauls.
   */
  public StepExecutionProgressResponse getStepExecutionProgress(Long jobExecutionId, Long stepExecutionId) throws NoSuchStepExecutionException, NoSuchJobExecutionException {
    StepExecution stepExecution = jobService.getStepExecution(jobExecutionId, stepExecutionId);
    StepExecutionInfoResponse stepExecutionInfoResponse = new StepExecutionInfoResponse(stepExecution, timeZone);
    String stepName = stepExecution.getStepName();
    if (stepName.contains(":partition")) {
      stepName = stepName.replaceAll("(:partition).*", "$1*");
    }
    String jobName = stepExecution.getJobExecution().getJobInstance().getJobName();
    StepExecutionHistory stepExecutionHistory = computeHistory(jobName, stepName);
    StepExecutionProgress stepExecutionProgress = new StepExecutionProgress(stepExecution, stepExecutionHistory);

    return new StepExecutionProgressResponse(stepExecutionProgress, stepExecutionHistory, stepExecutionInfoResponse);

  }

  private StepExecutionHistory computeHistory(String jobName, String stepName) {
    int total = jobService.countStepExecutionsForStep(jobName, stepName);
    StepExecutionHistory stepExecutionHistory = new StepExecutionHistory(stepName);
    for (int i = 0; i < total; i += 1000) {
      for (StepExecution stepExecution : jobService.listStepExecutionsForStep(jobName, stepName, i, 1000)) {
        stepExecutionHistory.append(stepExecution);
      }
    }
    return stepExecutionHistory;
  }
}
