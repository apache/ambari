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

import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.support.JobRegistryBeanPostProcessor;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

public abstract class AbstractJobsConfiguration<TProperties extends JobProperties<TParameters>, TParameters extends Validatable> {
  private static final Logger logger = LogManager.getLogger(AbstractJobsConfiguration.class);

  private final Map<String, TProperties> propertyMap;
  private final JobScheduler scheduler;
  private final JobBuilderFactory jobs;
  private final JobRegistryBeanPostProcessor jobRegistryBeanPostProcessor;

  protected AbstractJobsConfiguration(Map<String, TProperties> propertyMap, JobScheduler scheduler, JobBuilderFactory jobs, JobRegistryBeanPostProcessor jobRegistryBeanPostProcessor) {
    this.propertyMap = propertyMap;
    this.scheduler = scheduler;
    this.jobs = jobs;
    this.jobRegistryBeanPostProcessor = jobRegistryBeanPostProcessor;
  }

  @PostConstruct
  public void registerJobs() {
    if (propertyMap == null)
      return;

    propertyMap.keySet().stream()
            .filter(key -> propertyMap.get(key).isEnabled())
            .forEach(jobName -> {
              try {
                propertyMap.get(jobName).validate(jobName);
                logger.info("Registering job {}", jobName);
                JobBuilder jobBuilder = jobs.get(jobName).listener(new JobsPropertyMap<>(propertyMap));
                Job job = buildJob(jobBuilder);
                jobRegistryBeanPostProcessor.postProcessAfterInitialization(job, jobName);
              }
              catch (Exception e) {
                logger.warn("Unable to register job " + jobName, e);
                propertyMap.get(jobName).setEnabled(false);
              }
            });
  }

  @EventListener(ApplicationReadyEvent.class)
  public void scheduleJobs() {
    if (propertyMap == null)
      return;

    propertyMap.keySet().stream()
            .filter(key -> propertyMap.get(key).isEnabled())
            .forEach(jobName -> propertyMap.get(jobName).scheduling().ifPresent(
                    schedulingProperties -> scheduler.schedule(jobName, schedulingProperties)));
  }

  protected abstract Job buildJob(JobBuilder jobBuilder);
}
