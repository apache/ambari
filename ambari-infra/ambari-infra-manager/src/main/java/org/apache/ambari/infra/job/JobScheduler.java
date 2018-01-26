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

import org.apache.ambari.infra.manager.Jobs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import javax.inject.Inject;
import javax.inject.Named;
import java.time.Duration;
import java.time.OffsetDateTime;

import static org.apache.ambari.infra.job.archive.FileNameSuffixFormatter.SOLR_DATETIME_FORMATTER;
import static org.apache.commons.lang.StringUtils.isBlank;

@Named
public class JobScheduler {
  private static final Logger LOG = LoggerFactory.getLogger(JobScheduler.class);

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

    scheduler.schedule(() -> launchJob(jobName, schedulingProperties.getIntervalEndDelta()), new CronTrigger(schedulingProperties.getCron()));
    LOG.info("Job {} scheduled for running. Cron: {}", jobName, schedulingProperties.getCron());
  }

  private void restartIfFailed(JobExecution jobExecution) {
    if (jobExecution.getExitStatus() == ExitStatus.FAILED) {
      try {
        jobs.restart(jobExecution.getId());
      } catch (JobInstanceAlreadyCompleteException | NoSuchJobException | JobExecutionAlreadyRunningException | JobRestartException | JobParametersInvalidException | NoSuchJobExecutionException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private void launchJob(String jobName, String endDelta) {
    try {
      JobParametersBuilder jobParametersBuilder = new JobParametersBuilder();
      if (!isBlank(endDelta))
        jobParametersBuilder.addString("end", SOLR_DATETIME_FORMATTER.format(OffsetDateTime.now().minus(Duration.parse(endDelta))));

      jobs.launchJob(jobName, jobParametersBuilder.toJobParameters());
    } catch (JobParametersInvalidException | NoSuchJobException | JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException e) {
      throw new RuntimeException(e);
    }
  }
}
