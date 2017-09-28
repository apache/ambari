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
package org.apache.ambari.infra.job.dummy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

public class DummyJobListener implements JobExecutionListener {

  private static final Logger LOG = LoggerFactory.getLogger(DummyJobListener.class);

  @Override
  public void beforeJob(JobExecution jobExecution) {
    LOG.info("Dummy - before job execution");
  }

  @Override
  public void afterJob(JobExecution jobExecution) {
    LOG.info("Dummy - after job execution");
    if (jobExecution.getExecutionContext().get("jobOutputLocation") != null) {
      String jobOutputLocation = (String) jobExecution.getExecutionContext().get("jobOutputLocation");
      String exitDescription = "file://" + jobOutputLocation;
      LOG.info("Add exit description '{}'", exitDescription);
      jobExecution.setExitStatus(new ExitStatus(ExitStatus.COMPLETED.getExitCode(), exitDescription));
    }
  }
}
