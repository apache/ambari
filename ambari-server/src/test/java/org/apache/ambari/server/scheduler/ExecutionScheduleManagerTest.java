/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.scheduler;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.Transactional;
import com.google.inject.util.Modules;
import junit.framework.Assert;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.scheduler.Batch;
import org.apache.ambari.server.state.scheduler.BatchRequest;
import org.apache.ambari.server.state.scheduler.BatchSettings;
import org.apache.ambari.server.state.scheduler.RequestExecution;
import org.apache.ambari.server.state.scheduler.RequestExecutionFactory;
import org.apache.ambari.server.state.scheduler.Schedule;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

public class ExecutionScheduleManagerTest {
  private Clusters clusters;
  private Cluster cluster;
  private String clusterName;
  private Injector injector;
  private AmbariMetaInfo metaInfo;
  private ExecutionScheduleManager executionScheduleManager;
  private RequestExecutionFactory requestExecutionFactory;
  private ExecutionScheduler executionScheduler;
  private Scheduler scheduler;
  Properties properties;

  private static final Logger LOG =
    LoggerFactory.getLogger(ExecutionScheduleManagerTest.class);

  @Before
  public void setup() throws Exception {
    InMemoryDefaultTestModule defaultTestModule = new InMemoryDefaultTestModule();
    properties = defaultTestModule.getProperties();
    injector  = Guice.createInjector(Modules.override(defaultTestModule)
      .with(new ExecutionSchedulerTestModule()));
    injector.getInstance(GuiceJpaInitializer.class);
    clusters = injector.getInstance(Clusters.class);
    metaInfo = injector.getInstance(AmbariMetaInfo.class);
    executionScheduleManager = injector.getInstance(ExecutionScheduleManager.class);
    executionScheduler = injector.getInstance(ExecutionScheduler.class);
    requestExecutionFactory = injector.getInstance(RequestExecutionFactory.class);

    metaInfo.init();
    clusterName = "c1";
    clusters.addCluster(clusterName);
    cluster = clusters.getCluster(clusterName);
    cluster.setDesiredStackVersion(new StackId("HDP-0.1"));
    Assert.assertNotNull(cluster);
    assertThat(executionScheduler, instanceOf(TestExecutionScheduler.class));

    TestExecutionScheduler testExecutionScheduler = (TestExecutionScheduler)
      executionScheduler;
    scheduler = testExecutionScheduler.getScheduler();
    Assert.assertNotNull(scheduler);

    executionScheduleManager.start();
  }

  @After
  public void teardown() throws Exception {
    executionScheduleManager.stop();
    injector.getInstance(PersistService.class).stop();
  }

  public static class TestExecutionScheduler extends ExecutionSchedulerImpl {
    @Inject
    public TestExecutionScheduler(Injector injector) {
      super(injector);
      try {
        StdSchedulerFactory factory = new StdSchedulerFactory();
        scheduler = factory.getScheduler();
        isInitialized = true;
      } catch (SchedulerException e) {
        e.printStackTrace();
        throw new ExceptionInInitializerError("Unable to instantiate " +
          "scheduler");
      }
    }

    public Scheduler getScheduler() {
      return scheduler;
    }
  }

  public class ExecutionSchedulerTestModule implements Module {
    @Override
    public void configure(Binder binder) {
      binder.bind(ExecutionScheduler.class).to(TestExecutionScheduler.class);
    }
  }

  @Transactional
  private RequestExecution createRequestExecution(boolean addSchedule)
      throws Exception {
    Batch batches = new Batch();
    Schedule schedule = new Schedule();

    BatchSettings batchSettings = new BatchSettings();
    batchSettings.setTaskFailureToleranceLimit(10);
    batches.setBatchSettings(batchSettings);

    List<BatchRequest> batchRequests = new ArrayList<BatchRequest>();
    BatchRequest batchRequest1 = new BatchRequest();
    batchRequest1.setOrderId(10L);
    batchRequest1.setType(BatchRequest.Type.DELETE);
    batchRequest1.setUri("testUri1");

    BatchRequest batchRequest2 = new BatchRequest();
    batchRequest2.setOrderId(12L);
    batchRequest2.setType(BatchRequest.Type.POST);
    batchRequest2.setUri("testUri2");
    batchRequest2.setBody("testBody");

    batchRequests.add(batchRequest1);
    batchRequests.add(batchRequest2);

    batches.getBatchRequests().addAll(batchRequests);

    schedule.setMinutes("10");
    schedule.setHours("2");
    schedule.setMonth("*");
    schedule.setDaysOfMonth("*");
    schedule.setDayOfWeek("?");

    if (!addSchedule) {
      schedule = null;
    }

    RequestExecution requestExecution = requestExecutionFactory.createNew
      (cluster, batches, schedule);
    requestExecution.setDescription("Test Schedule");

    requestExecution.persist();

    return requestExecution;
  }

  @Test
  public void testScheduleBatch() throws Exception {
    RequestExecution requestExecution = createRequestExecution(true);
    Assert.assertNotNull(requestExecution);

    executionScheduleManager.scheduleBatch(requestExecution);

    String jobName1 = executionScheduleManager.getJobName(requestExecution
      .getId(), 10L);
    String jobName2 = executionScheduleManager.getJobName(requestExecution
      .getId(), 12L);
    JobDetail jobDetail1 = null;
    JobDetail jobDetail2 = null;
    Trigger trigger1 = null;
    Trigger trigger2 = null;

    // enumerate each job group
    for(String group: scheduler.getJobGroupNames()) {
      // enumerate each job in group
      for(JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals
          (ExecutionJob.LINEAR_EXECUTION_JOB_GROUP))) {
        LOG.info("Found job identified by: " + jobKey);

        String jobName = jobKey.getName();
        String jobGroup = jobKey.getGroup();

        List<Trigger> triggers = (List<Trigger>) scheduler.getTriggersOfJob(jobKey);
        Trigger trigger = triggers != null && !triggers.isEmpty() ?
          triggers.get(0) : null;
        Date nextFireTime = trigger != null ? trigger.getNextFireTime() : null;

        LOG.info("[jobName] : " + jobName + " [groupName] : "
          + jobGroup + " - " + nextFireTime);

        if (jobName.equals(jobName1)) {
          jobDetail1 = scheduler.getJobDetail(jobKey);
          trigger1 = trigger;
        } else if (jobName.equals(jobName2)) {
          jobDetail2 = scheduler.getJobDetail(jobKey);
          trigger2 = trigger;
        }
      }
    }

    Assert.assertNotNull(jobDetail1);
    Assert.assertNotNull(trigger1);
    Assert.assertNotNull(jobDetail2);
    Assert.assertNull(trigger2);

    CronTrigger cronTrigger = (CronTrigger) trigger1;
    Schedule schedule = new Schedule();
    schedule.setMinutes("10");
    schedule.setHours("2");
    schedule.setMonth("*");
    schedule.setDaysOfMonth("*");
    schedule.setDayOfWeek("?");

    Assert.assertEquals(schedule.getScheduleExpression(),
      cronTrigger.getCronExpression());

    Assert.assertEquals(jobName1, jobDetail1.getKey().getName());
    Assert.assertEquals(jobName2, jobDetail2.getKey().getName());
  }

  @Test
  public void testDeleteAllJobs() throws Exception {
    RequestExecution requestExecution = createRequestExecution(true);
    Assert.assertNotNull(requestExecution);

    executionScheduleManager.scheduleBatch(requestExecution);

    String jobName1 = executionScheduleManager.getJobName(requestExecution
      .getId(), 10L);
    String jobName2 = executionScheduleManager.getJobName(requestExecution
      .getId(), 12L);

    JobDetail jobDetail1 = scheduler.getJobDetail(JobKey.jobKey(jobName1,
      ExecutionJob.LINEAR_EXECUTION_JOB_GROUP));
    JobDetail jobDetail2 = scheduler.getJobDetail(JobKey.jobKey(jobName2,
      ExecutionJob.LINEAR_EXECUTION_JOB_GROUP));

    Assert.assertNotNull(jobDetail1);
    Assert.assertNotNull(jobDetail2);
    Assert.assertTrue(!scheduler.getTriggersOfJob(JobKey.jobKey(jobName1,
      ExecutionJob.LINEAR_EXECUTION_JOB_GROUP)).isEmpty());

    executionScheduleManager.deleteAllJobs(requestExecution);

    Assert.assertTrue(scheduler.getTriggersOfJob(JobKey.jobKey(jobName1,
      ExecutionJob.LINEAR_EXECUTION_JOB_GROUP)).isEmpty());
  }

  @Test
  public void testPointInTimeExecutionJob() throws Exception {
    RequestExecution requestExecution = createRequestExecution(false);
    Assert.assertNotNull(requestExecution);

    executionScheduleManager.scheduleBatch(requestExecution);

    String jobName1 = executionScheduleManager.getJobName(requestExecution
      .getId(), 10L);
    String jobName2 = executionScheduleManager.getJobName(requestExecution
      .getId(), 12L);

    JobDetail jobDetail1 = scheduler.getJobDetail(JobKey.jobKey(jobName1,
      ExecutionJob.LINEAR_EXECUTION_JOB_GROUP));
    JobDetail jobDetail2 = scheduler.getJobDetail(JobKey.jobKey(jobName2,
      ExecutionJob.LINEAR_EXECUTION_JOB_GROUP));

    Assert.assertNotNull(jobDetail1);
    Assert.assertNotNull(jobDetail2);

    List<? extends Trigger> triggers = scheduler.getTriggersOfJob
      (JobKey.jobKey(jobName1, ExecutionJob.LINEAR_EXECUTION_JOB_GROUP));

    Assert.assertNotNull(triggers);
    Assert.assertEquals(1, triggers.size());
    assertThat(triggers.get(0), instanceOf(SimpleTrigger.class));

    int waitCount = 0;
    while (scheduler.getCurrentlyExecutingJobs().size() != 0 && waitCount < 10) {
      Thread.sleep(100);
      waitCount++;
    }
  }
}
