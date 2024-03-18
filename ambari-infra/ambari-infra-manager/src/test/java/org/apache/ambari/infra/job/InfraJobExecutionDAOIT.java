package org.apache.ambari.infra.job;

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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.time.OffsetDateTime;
import java.util.Date;

import javax.inject.Inject;

import org.apache.ambari.infra.env.TestAppConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.admin.service.SearchableJobExecutionDao;
import org.springframework.batch.admin.service.SearchableJobInstanceDao;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.support.TransactionTemplate;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestAppConfig.class})
public class InfraJobExecutionDAOIT {

  private static int jobCounter = 0;

  @Inject
  private JdbcTemplate jdbcTemplate;
  @Inject
  private TransactionTemplate transactionTemplate;
  @Inject
  private JobRepository jobRepository;
  @Inject
  private SearchableJobExecutionDao searchableJobExecutionDao;
  @Inject
  private SearchableJobInstanceDao searchableJobInstanceDao;
  private InfraJobExecutionDao infraJobExecutionDao;

  @Before
  public void setUp() {
    infraJobExecutionDao = new InfraJobExecutionDao(jdbcTemplate, transactionTemplate);
  }

  @Test
  public void testDeleteJobExecutions() throws Exception {
    JobExecution yesterdayJob = newJobAt(OffsetDateTime.now().minusDays(1));
    JobExecution todayJob = newJobAt(OffsetDateTime.now());

    infraJobExecutionDao.deleteJobExecutions(OffsetDateTime.now().minusHours(1));

    assertThat(searchableJobExecutionDao.getJobExecution(todayJob.getId()), is(not(nullValue())));
    assertThat(searchableJobExecutionDao.getJobExecution(yesterdayJob.getId()), is(nullValue()));

    assertThat(searchableJobInstanceDao.getJobInstance(todayJob.getJobId()), is(not(nullValue())));
    assertThat(searchableJobInstanceDao.getJobInstance(yesterdayJob.getJobId()), is(nullValue()));
  }

  private JobExecution newJobAt(OffsetDateTime createdAt) throws JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException {
    JobParameters jobParameters = new JobParametersBuilder().addString("test param", "test value").toJobParameters();
    JobExecution jobExecution = jobRepository.createJobExecution("test job" + jobCounter++ , jobParameters);
    jobExecution.setCreateTime(Date.from(createdAt.toInstant()));
    jobRepository.update(jobExecution);

    StepExecution stepExecution = new StepExecution("step1", jobExecution);
    jobRepository.add(stepExecution);

    return jobExecution;
  }
}