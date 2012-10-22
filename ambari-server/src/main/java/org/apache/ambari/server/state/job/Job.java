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

package org.apache.ambari.server.state.job;

import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;

public interface Job {

  /**
   * Get the Job ID for the action
   * @return JobId
   */
  public JobId getId();

  // TODO requires some form of JobType to ensure only one running
  // job per job type
  // There may be gotchas such as de-commissioning should be allowed to happen
  // on more than one host at a time


  /**
   * Get Start Time of the job
   * @return Start time as a unix timestamp
   */
  public long getStartTime();

  /**
   * Get the last update time of the Job when its progress status
   * was updated
   * @return Last Update Time as a unix timestamp
   */
  public long getLastUpdateTime();

  /**
   * Time when the Job completed
   * @return Completion Time as a unix timestamp
   */
  public long getCompletionTime();

  /**
   * Get the current state of the Job
   * @return JobState
   */
  public JobState getState();

  /**
   * Set the State of the Job
   * @param state JobState
   */
  public void setState(JobState state);

  /**
   * Send a JobEvent to the Job's StateMachine
   * @param event JobEvent
   * @throws InvalidStateTransitionException
   */
  public void handleEvent(JobEvent event)
      throws InvalidStateTransitionException;
}
