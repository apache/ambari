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
package org.apache.ambari.infra.job.deleting;

import org.apache.ambari.infra.job.JobPropertyMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.configuration.support.JobRegistryBeanPostProcessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

@Configuration
public class DocumentDeletingConfiguration {
  private static final Logger LOG = LoggerFactory.getLogger(DocumentDeletingConfiguration.class);

  @Inject
  private DocumentDeletingPropertyMap propertyMap;

  @Inject
  private StepBuilderFactory steps;

  @Inject
  private JobBuilderFactory jobs;

  @Inject
  private JobRegistryBeanPostProcessor jobRegistryBeanPostProcessor;

  @Inject
  @Qualifier("deleteStep")
  private Step deleteStep;

  @PostConstruct
  public void createJobs() {
    propertyMap.getSolrDataDeleting().values().forEach(DocumentDeletingProperties::validate);

    propertyMap.getSolrDataDeleting().keySet().forEach(jobName -> {
      LOG.info("Registering data deleting job {}", jobName);
      Job job = logDeleteJob(jobName, deleteStep);
      jobRegistryBeanPostProcessor.postProcessAfterInitialization(job, jobName);
    });
  }

  private Job logDeleteJob(String jobName, Step logExportStep) {
    return jobs.get(jobName).listener(new JobPropertyMap<>(propertyMap)).start(logExportStep).build();
  }

  @Bean
  @JobScope
  public Step deleteStep(DocumentWiperTasklet tasklet) {
    return steps.get("delete")
            .tasklet(tasklet)
            .build();
  }

  @Bean
  @StepScope
  public DocumentWiperTasklet documentWiperTasklet(
          @Value("#{stepExecution.jobExecution.executionContext.get('jobProperties')}") DocumentDeletingProperties properties,
          @Value("#{jobParameters[start]}") String start,
          @Value("#{jobParameters[end]}") String end) {
    return new DocumentWiperTasklet(properties, start, end);
  }
}
