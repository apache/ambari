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

package org.apache.ambari.view.pig.resources.jobs.utils;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.pig.persistence.utils.ItemNotFound;
import org.apache.ambari.view.pig.resources.jobs.JobResourceManager;
import org.apache.ambari.view.pig.resources.jobs.models.PigJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Polling manager
 * Makes scheduled repeated polling of templeton to
 * be aware of happen events like job finished,
 * killed, changed progress and so on.
 */
public class JobPolling implements Runnable {
  private final static Logger LOG =
      LoggerFactory.getLogger(JobPolling.class);

  /**
   * We should limit count of concurrent calls to templeton
   * to avoid high load on component
   */
  private static final int WORKER_COUNT = 2;

  private static final int POLLING_DELAY = 10*60;  // 10 minutes

  /**
   * In LONG_JOB_THRESHOLD seconds job reschedules polling from POLLING_DELAY to LONG_POLLING_DELAY
   */
  private static final int LONG_POLLING_DELAY = 60*60; // 1 hour
  private static final int LONG_JOB_THRESHOLD = 60*60; // 1 hour

  private static final ScheduledExecutorService pollWorkersPool = Executors.newScheduledThreadPool(WORKER_COUNT);

  private static final Map<String, JobPolling> jobPollers = new HashMap<String, JobPolling>();

  private JobResourceManager resourceManager = null;
  private final ViewContext context;
  private PigJob job;
  private volatile ScheduledFuture<?> thisFuture;

  private JobPolling(ViewContext context, PigJob job) {
    this.context = context;
    this.job = job;
  }

  protected synchronized JobResourceManager getResourceManager() {
    if (resourceManager == null) {
      resourceManager = new JobResourceManager(context);
    }
    return resourceManager;
  }

  /**
   * Do polling
   */
  public void run() {
    LOG.debug("Polling job status " + job.getJobId() + " #" + job.getId());
    try {
      job = getResourceManager().read(job.getId());
    } catch (ItemNotFound itemNotFound) {
      LOG.error("Job " + job.getJobId() + " does not exist! Polling canceled");
      thisFuture.cancel(false);
      return;
    }
    getResourceManager().retrieveJobStatus(job);

    Long time = System.currentTimeMillis() / 1000L;
    if (time - job.getDateStarted() > LONG_JOB_THRESHOLD) {
      LOG.debug("Job becomes long.. Rescheduling polling to longer period");
      // If job running longer than LONG_JOB_THRESHOLD, reschedule
      // it to poll every LONG_POLLING_DELAY instead of POLLING_DELAY
      thisFuture.cancel(false);
      scheduleJobPolling(true);
    }

    switch (job.getStatus()) {
      case SUBMIT_FAILED:
      case COMPLETED:
      case FAILED:
      case KILLED:
        LOG.debug("Job finished. Polling canceled");
        thisFuture.cancel(false);
        break;
      default:
    }
  }

  private void scheduleJobPolling(boolean longDelay) {
    if (!longDelay) {
      thisFuture = pollWorkersPool.scheduleWithFixedDelay(this,
          POLLING_DELAY, POLLING_DELAY, TimeUnit.SECONDS);
    } else {
      thisFuture = pollWorkersPool.scheduleWithFixedDelay(this,
          LONG_POLLING_DELAY, LONG_POLLING_DELAY, TimeUnit.SECONDS);
    }
  }

  private void scheduleJobPolling() {
    scheduleJobPolling(false);
  }

  /**
   * Schedule job polling
   * @param context ViewContext of web app
   * @param job job instance
   * @return returns false if already scheduled
   */
  public static boolean pollJob(ViewContext context, PigJob job) {
    if (jobPollers.get(job.getJobId()) == null) {
      LOG.debug("Setting up polling for " + job.getJobId());
      JobPolling polling = new JobPolling(context, job);
      polling.scheduleJobPolling();
      jobPollers.put(job.getJobId(), polling);
      return true;
    }
    return false;
  }
}
