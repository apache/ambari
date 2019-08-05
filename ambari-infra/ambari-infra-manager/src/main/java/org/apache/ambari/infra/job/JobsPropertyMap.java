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

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

public class JobsPropertyMap<TProperties extends JobProperties<TParameters>, TParameters extends Validatable>
        implements JobExecutionListener {

  public static final String PARAMETERS_CONTEXT_KEY = "jobParameters";
  private final Map<String, TProperties> propertyMap;

  public JobsPropertyMap(Map<String, TProperties> propertyMap) {
    this.propertyMap = propertyMap;
  }

  @Override
  public void beforeJob(JobExecution jobExecution) {
    String jobName = jobExecution.getJobInstance().getJobName();
    TProperties defaultProperties = propertyMap.get(jobName);
    if (defaultProperties == null)
      throw new UnsupportedOperationException("Properties not found for job " + jobName);

    new JobPropertiesHolder<>(defaultProperties).beforeJob(jobExecution);
  }

  @Override
  public void afterJob(JobExecution jobExecution) {

  }
}
