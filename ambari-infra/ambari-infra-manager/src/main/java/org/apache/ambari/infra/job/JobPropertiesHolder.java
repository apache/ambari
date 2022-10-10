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

import static org.apache.ambari.infra.job.JobsPropertyMap.PARAMETERS_CONTEXT_KEY;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

public class JobPropertiesHolder<T extends Validatable>
        implements JobExecutionListener {

  private final JobProperties<T> defaultProperties;

  public JobPropertiesHolder(JobProperties<T> defaultProperties) {
    this.defaultProperties = defaultProperties;
  }

  @Override
  public void beforeJob(JobExecution jobExecution) {
    try {
      T parameters = defaultProperties.merge(jobExecution.getJobParameters());
      parameters.validate();
      jobExecution.getExecutionContext().put(PARAMETERS_CONTEXT_KEY, parameters);
    }
    catch (UnsupportedOperationException | IllegalArgumentException ex) {
      jobExecution.stop();
      jobExecution.setExitStatus(new ExitStatus(ExitStatus.FAILED.getExitCode(), ex.getMessage()));
      throw ex;
    }
  }

  @Override
  public void afterJob(JobExecution jobExecution) {

  }
}
