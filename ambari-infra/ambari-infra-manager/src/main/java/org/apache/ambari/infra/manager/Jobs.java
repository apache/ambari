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

import java.util.Optional;

import org.apache.ambari.infra.model.JobExecutionInfoResponse;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;

public interface Jobs {
  JobExecutionInfoResponse launchJob(String jobName, JobParameters params)
          throws JobParametersInvalidException, NoSuchJobException,
          JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException;
  void restart(Long jobExecutionId)
          throws JobInstanceAlreadyCompleteException, NoSuchJobException, JobExecutionAlreadyRunningException,
          JobParametersInvalidException, JobRestartException, NoSuchJobExecutionException;

  Optional<JobExecution> lastRun(String jobName) throws NoSuchJobException, NoSuchJobExecutionException;

  void stopAndAbandon(Long jobExecution) throws NoSuchJobExecutionException, JobExecutionAlreadyRunningException;
}
