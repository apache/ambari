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

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.utils.DateUtils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * This class handles scheduling request execution for managed clusters
 */
@Singleton
public class ExecutionScheduleManager {
  private static final Logger LOG = LoggerFactory.getLogger
    (ExecutionScheduleManager.class);
  @Inject
  private ExecutionScheduler executionScheduler;
  @Inject
  private Configuration configuration;

  private volatile boolean schedulerAvailable = false;

  @Inject
  public ExecutionScheduleManager(Injector injector) {
    injector.injectMembers(this);
  }

  public void start() {
    LOG.info("Starting scheduler");
    try {
      executionScheduler.startScheduler();
      schedulerAvailable = true;
    } catch (AmbariException e) {
      LOG.warn("Unable to start scheduler. No recurring tasks will be " +
        "scheduled.");
    }
  }

  public void stop() {
    LOG.info("Stopping scheduler");
    schedulerAvailable = false;
    try {
      executionScheduler.stopScheduler();
    } catch (AmbariException e) {
      LOG.warn("Unable to stop scheduler. No new recurring tasks will be " +
        "scheduled.");
    }
  }

  public boolean isSchedulerAvailable() {
    return schedulerAvailable;
  }

  public void scheduleJob(Trigger trigger) {
    LOG.debug("Scheduling job: " + trigger.getJobKey());
    if (isSchedulerAvailable()) {
      try {
        executionScheduler.scheduleJob(trigger);
      } catch (SchedulerException e) {
        LOG.error("Unable to add trigger for execution job: " + trigger
          .getJobKey(), e);
      }
    } else {
      LOG.error("Scheduler unavailable, cannot schedule jobs.");
    }
  }

  public boolean continueOnMisfire(JobExecutionContext jobExecutionContext) {
    if (jobExecutionContext != null) {
      Date scheduledTime = jobExecutionContext.getScheduledFireTime();
      Long diff = DateUtils.getDateDifferenceInMinutes(scheduledTime);
      return (diff < configuration.getExecutionSchedulerMisfireToleration());
    }
    return true;
  }
}
