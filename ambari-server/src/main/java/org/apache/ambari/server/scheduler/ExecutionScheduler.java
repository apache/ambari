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
import org.apache.ambari.server.state.scheduler.RequestExecution;
import org.apache.ambari.server.state.scheduler.Schedule;
import org.quartz.Job;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

public interface ExecutionScheduler {
  /**
   * Initialize and start the scheduler to accept jobs.
   * @throws AmbariException
   */
  public void startScheduler() throws AmbariException;

  /**
   * Shutdown the scheduler threads and do not accept any more jobs.
   * @throws AmbariException
   */
  public void stopScheduler() throws AmbariException;

  /**
   * Create a job based on the @RequestExecution and add a trigger for the
   * created job based on the @Schedule. Schedule the job with the scheduler.
   * @param requestExecution
   * @param schedule
   * @throws AmbariException
   */
  public void scheduleJob(RequestExecution requestExecution,
                          Schedule schedule) throws AmbariException;

  public void scheduleJob(Trigger trigger) throws SchedulerException;
}
