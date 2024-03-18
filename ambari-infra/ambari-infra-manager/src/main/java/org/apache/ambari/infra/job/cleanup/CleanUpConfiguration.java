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
package org.apache.ambari.infra.job.cleanup;

import static org.apache.ambari.infra.job.JobsPropertyMap.PARAMETERS_CONTEXT_KEY;

import javax.inject.Inject;

import org.apache.ambari.infra.job.InfraJobExecutionDao;
import org.apache.ambari.infra.job.JobPropertiesHolder;
import org.apache.ambari.infra.job.JobScheduler;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Configuration
public class CleanUpConfiguration {

  public static final String JOB_NAME = "clean_up";
  private final StepBuilderFactory steps;
  private final JobBuilderFactory jobs;
  private final JobScheduler scheduler;
  private final CleanUpProperties cleanUpProperties;

  @Inject
  public CleanUpConfiguration(StepBuilderFactory steps, JobBuilderFactory jobs, CleanUpProperties cleanUpProperties, JobScheduler scheduler) {
    this.steps = steps;
    this.jobs = jobs;
    this.scheduler = scheduler;
    this.cleanUpProperties = cleanUpProperties;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void scheduleJob() {
    cleanUpProperties.scheduling().ifPresent(schedulingProperties -> scheduler.schedule(JOB_NAME, schedulingProperties));
  }

  @Bean(name = "cleanUpJob")
  public Job job(@Qualifier("cleanUpStep") Step cleanUpStep) {
    return jobs.get(JOB_NAME).listener(new JobPropertiesHolder<>(cleanUpProperties)).start(cleanUpStep).build();
  }

  @Bean(name = "cleanUpStep")
  protected Step cleanUpStep(TaskHistoryWiper taskHistoryWiper) {
    return steps.get("cleanUpStep").tasklet(taskHistoryWiper).build();
  }

  @Bean
  @StepScope
  protected TaskHistoryWiper taskHistoryWiper(
          InfraJobExecutionDao infraJobExecutionDao,
          @Value("#{stepExecution.jobExecution.executionContext.get('" + PARAMETERS_CONTEXT_KEY + "')}") CleanUpProperties cleanUpProperties) {
    return new TaskHistoryWiper(infraJobExecutionDao, cleanUpProperties.getTtl());
  }
}
