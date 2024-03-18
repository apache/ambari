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
package org.apache.ambari.infra.job;

import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.ambari.infra.manager.Jobs;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

@Named
public class JobScheduler {
  private static final Logger logger = LogManager.getLogger(JobScheduler.class);

  private final TaskScheduler scheduler;
  private final Jobs jobs;

  @Inject
  public JobScheduler(TaskScheduler scheduler, Jobs jobs) {
    this.scheduler = scheduler;
    this.jobs = jobs;
  }

  public void schedule(String jobName, SchedulingProperties schedulingProperties) {
    try {
      jobs.lastRun(jobName).ifPresent(this::restartIfFailed);
    } catch (NoSuchJobException | NoSuchJobExecutionException e) {
      throw new RuntimeException(e);
    }

    scheduler.schedule(() -> launchJob(jobName), new CronTrigger(schedulingProperties.getCron()));
    logger.info("Job {} scheduled for running. Cron: {}", jobName, schedulingProperties.getCron());
  }

  private void restartIfFailed(JobExecution jobExecution) {
    try {
      if (ExitStatus.FAILED.compareTo(jobExecution.getExitStatus()) == 0) {
        jobs.restart(jobExecution.getId());
      } else if (ExitStatus.UNKNOWN.compareTo(jobExecution.getExitStatus()) == 0) {
        jobs.stopAndAbandon(jobExecution.getId());
      }
    } catch (JobInstanceAlreadyCompleteException | NoSuchJobException | JobExecutionAlreadyRunningException | JobRestartException | JobParametersInvalidException | NoSuchJobExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  private void launchJob(String jobName) {
    try {
      JobParametersBuilder jobParametersBuilder = new JobParametersBuilder();
      jobParametersBuilder.addDate("scheduledLaunchAt", new Date());
      jobs.launchJob(jobName, jobParametersBuilder.toJobParameters());
    } catch (JobParametersInvalidException | NoSuchJobException | JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException e) {
      throw new RuntimeException(e);
    }
  }
}
