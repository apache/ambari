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

import org.apache.ambari.server.AmbariException;
import org.quartz.DateBuilder;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.PersistJobDataAfterExecution;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.quartz.DateBuilder.futureDate;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * Job that knows how to get the job name and group out of the JobDataMap using
 * pre-defined keys (constants) and contains code to schedule the identified job.
 * This abstract Job's implementation of execute() delegates to an abstract
 * template method "doWork()" (where the extending Job class's real work goes)
 * and then it schedules the follow-up job.
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public abstract class AbstractLinearExecutionJob implements ExecutionJob {
  private static Logger LOG = LoggerFactory.getLogger(AbstractLinearExecutionJob.class);
  protected ExecutionScheduleManager executionScheduleManager;

  public AbstractLinearExecutionJob(ExecutionScheduleManager executionScheduleManager) {
    this.executionScheduleManager = executionScheduleManager;
  }

  /**
   * Do the actual work of the fired job.
   * @throws AmbariException
   * @param properties
   */
  protected abstract void doWork(Map<String, Object> properties) throws
    AmbariException;

  /**
   * Get the next job id from context and create a trigger to fire the next
   * job.
   * @param context
   * @throws JobExecutionException
   */
  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    JobKey jobKey = context.getJobDetail().getKey();
    LOG.debug("Executing linear job: " + jobKey);

    if (!executionScheduleManager.continueOnMisfire(context)) {
      throw new JobExecutionException("Canceled execution based on misfire"
        + " toleration threshold, job: " + jobKey
        + ", scheduleTime = " + context.getScheduledFireTime());
    }

    // Perform work and exit if failure reported
    try {
      doWork(context.getMergedJobDataMap().getWrappedMap());
    } catch (AmbariException e) {
      LOG.error("Exception caught on job execution. Exiting linear chain...", e);
      throw new JobExecutionException(e);
    }

    JobDataMap jobDataMap = context.getMergedJobDataMap();
    String nextJobName = jobDataMap.getString(NEXT_EXECUTION_JOB_NAME_KEY);
    String nextJobGroup = jobDataMap.getString(NEXT_EXECUTION_JOB_GROUP_KEY);
    Integer separationSeconds = jobDataMap.getIntegerFromString(
      (NEXT_EXECUTION_SEPARATION_SECONDS));

    if (separationSeconds == null) {
      separationSeconds = 0;
    }

    // Create trigger for next job execution
    Trigger trigger = newTrigger()
      .forJob(nextJobName, nextJobGroup)
      .withIdentity("TriggerForJob-" + nextJobName, LINEAR_EXECUTION_TRIGGER_GROUP)
      .withSchedule(simpleSchedule().withMisfireHandlingInstructionFireNow())
      .startAt(futureDate(separationSeconds, DateBuilder.IntervalUnit.SECOND))
      .build();

    executionScheduleManager.scheduleJob(trigger);
  }

}
