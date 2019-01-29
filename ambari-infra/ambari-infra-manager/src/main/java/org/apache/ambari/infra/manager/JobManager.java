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

import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.ambari.infra.model.ExecutionContextResponse;
import org.apache.ambari.infra.model.JobExecutionDetailsResponse;
import org.apache.ambari.infra.model.JobExecutionInfoResponse;
import org.apache.ambari.infra.model.JobInstanceDetailsResponse;
import org.apache.ambari.infra.model.JobOperationParams;
import org.apache.ambari.infra.model.StepExecutionContextResponse;
import org.apache.ambari.infra.model.StepExecutionInfoResponse;
import org.apache.ambari.infra.model.StepExecutionProgressResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.admin.history.StepExecutionHistory;
import org.springframework.batch.admin.service.JobService;
import org.springframework.batch.admin.service.NoSuchStepExecutionException;
import org.springframework.batch.admin.web.JobInfo;
import org.springframework.batch.admin.web.StepExecutionProgress;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.launch.NoSuchJobInstanceException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;

import com.google.common.collect.Lists;

@Named
public class JobManager implements Jobs {

  private static final Logger logger = LogManager.getLogger(JobManager.class);

  @Inject
  private JobService jobService;

  @Inject
  private JobOperator jobOperator;

  @Inject
  private JobExplorer jobExplorer;

  public Set<String> getAllJobNames() {
    return jobOperator.getJobNames();
  }

  /**
   * Launch a new job instance (based on job name) and applies customized parameters to it.
   * Also add a new date parameter to make sure the job instance will be unique
   */
  @Override
  public JobExecutionInfoResponse launchJob(String jobName, JobParameters jobParameters)
    throws JobParametersInvalidException, NoSuchJobException,
    JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException {

    Set<JobExecution> running = jobExplorer.findRunningJobExecutions(jobName);
    if (!running.isEmpty())
      throw new JobExecutionAlreadyRunningException("An instance of this job is already active: "+jobName);

    return new JobExecutionInfoResponse(jobService.launch(jobName, jobParameters));
  }

  @Override
  public void restart(Long jobExecutionId)
          throws JobInstanceAlreadyCompleteException, NoSuchJobException, JobExecutionAlreadyRunningException,
          JobParametersInvalidException, JobRestartException, NoSuchJobExecutionException {
    jobService.restart(jobExecutionId);
  }

  @Override
  public Optional<JobExecution> lastRun(String jobName) throws NoSuchJobException {
    return jobService.listJobExecutionsForJob(jobName, 0, 1).stream().findFirst();
  }

  @Override
  public void stopAndAbandon(Long jobExecutionId) throws NoSuchJobExecutionException, JobExecutionAlreadyRunningException {
    try {
      jobService.stop(jobExecutionId);
    } catch (JobExecutionNotRunningException e) {
      logger.warn(String.format("Job is not running jobExecutionId=%d", jobExecutionId), e.getMessage());
    }
    jobService.abandon(jobExecutionId);
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
  public JobExecutionDetailsResponse getExecutionInfo(Long jobExecutionId) throws NoSuchJobExecutionException {
    JobExecution jobExecution = jobService.getJobExecution(jobExecutionId);
    List<StepExecutionInfoResponse> stepExecutionInfoList = new ArrayList<>();
    for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
      stepExecutionInfoList.add(new StepExecutionInfoResponse(stepExecution));
    }
    stepExecutionInfoList.sort(Comparator.comparing(StepExecutionInfoResponse::getStepExecutionId));
    return new JobExecutionDetailsResponse(new JobExecutionInfoResponse(jobExecution), stepExecutionInfoList);
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
    logger.info("Job {} was marked {}", jobExecution.getJobInstance().getJobName(), operation.name());
    return new JobExecutionInfoResponse(jobExecution);
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
      return new JobExecutionInfoResponse(jobService.restart(jobExecutionId));
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
      result.add(new JobExecutionInfoResponse(jobExecution));
    }
    return result;
  }

  /**
   * Get job details for a specific job. (paged)
   */
  public List<JobInstanceDetailsResponse> getJobDetails(String jobName, int page, int size) throws NoSuchJobException {
    List<JobInstanceDetailsResponse> jobInstanceResponses = Lists.newArrayList();
    Collection<JobInstance> jobInstances = jobService.listJobInstances(jobName, page, size);

    boolean launchable = jobService.isLaunchable(jobName);
    boolean incrementable = jobService.isIncrementable(jobName);

    for (JobInstance jobInstance: jobInstances) {
      List<JobExecutionInfoResponse> executionInfoResponses = Lists.newArrayList();
      Collection<JobExecution> jobExecutions = jobService.getJobExecutionsForJobInstance(jobName, jobInstance.getId());
      if (jobExecutions != null) {
        for (JobExecution jobExecution : jobExecutions) {
          executionInfoResponses.add(new JobExecutionInfoResponse(jobExecution));
        }
      }
      jobInstanceResponses.add(new JobInstanceDetailsResponse(
              new JobInfo(jobName, executionInfoResponses.size(), jobInstance.getInstanceId(), launchable, incrementable),
              executionInfoResponses));
    }
    return unmodifiableList(jobInstanceResponses);
  }

  /**
   * Get step execution details based for job execution id and step execution id.
   */
  public StepExecutionInfoResponse getStepExecution(Long jobExecutionId, Long stepExecutionId) throws NoSuchStepExecutionException, NoSuchJobExecutionException {
    StepExecution stepExecution = jobService.getStepExecution(jobExecutionId, stepExecutionId);
    return new StepExecutionInfoResponse(stepExecution);
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
    StepExecutionInfoResponse stepExecutionInfoResponse = new StepExecutionInfoResponse(stepExecution);
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
