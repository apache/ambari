package org.apache.ambari.infra.job;

import org.apache.ambari.infra.manager.Jobs;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import javax.batch.operations.NoSuchJobException;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;

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
@RunWith(EasyMockRunner.class)
public class JobSchedulerTest extends EasyMockSupport {

  @Mock
  private TaskScheduler taskScheduler;
  @Mock
  private Jobs jobs;
  @Mock
  private ScheduledFuture scheduledFuture;
  private JobScheduler jobScheduler;

  @Before
  public void setUp() throws Exception {
    jobScheduler = new JobScheduler(taskScheduler, jobs);
  }

  @After
  public void tearDown() throws Exception {
    verifyAll();
  }

  @Test(expected = NoSuchJobException.class)
  public void testScheduleWhenJobNotExistsThrowsException() throws Exception {
    String jobName = "notFoundJob";
    expect(jobs.lastRun(jobName)).andThrow(new NoSuchJobException());
    replayAll();

    jobScheduler.schedule(jobName, null);
  }

  @Test
  public void testScheduleWhenNoPreviousExecutionExistsJobIsScheduled() throws Exception {
    String jobName = "job0";
    SchedulingProperties schedulingProperties = new SchedulingProperties();
    schedulingProperties.setCron("* * * * * ?");
    expect(jobs.lastRun(jobName)).andReturn(Optional.empty());
    expect(taskScheduler.schedule(isA(Runnable.class), eq(new CronTrigger(schedulingProperties.getCron())))).andReturn(scheduledFuture);
    replayAll();

    jobScheduler.schedule(jobName, schedulingProperties);
  }

  @Test
  public void testScheduleWhenPreviousExecutionWasSuccessfulJobIsScheduled() throws Exception {
    String jobName = "job0";
    SchedulingProperties schedulingProperties = new SchedulingProperties();
    schedulingProperties.setCron("* * * * * ?");
    JobExecution jobExecution = new JobExecution(1L, new JobParameters());
    jobExecution.setExitStatus(ExitStatus.COMPLETED);
    expect(jobs.lastRun(jobName)).andReturn(Optional.of(jobExecution));
    expect(taskScheduler.schedule(isA(Runnable.class), eq(new CronTrigger(schedulingProperties.getCron())))).andReturn(scheduledFuture);
    replayAll();

    jobScheduler.schedule(jobName, schedulingProperties);
  }

  @Test
  public void testScheduleWhenPreviousExecutionFailedJobIsRestartedAndScheduled() throws Exception {
    String jobName = "job0";
    SchedulingProperties schedulingProperties = new SchedulingProperties();
    schedulingProperties.setCron("* * * * * ?");
    JobExecution jobExecution = new JobExecution(1L, new JobParameters());
    jobExecution.setExitStatus(ExitStatus.FAILED);
    expect(jobs.lastRun(jobName)).andReturn(Optional.of(jobExecution));
    jobs.restart(1L); expectLastCall();
    expect(taskScheduler.schedule(isA(Runnable.class), eq(new CronTrigger(schedulingProperties.getCron())))).andReturn(scheduledFuture);
    replayAll();

    jobScheduler.schedule(jobName, schedulingProperties);
  }
}