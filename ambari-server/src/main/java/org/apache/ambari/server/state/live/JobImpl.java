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

package org.apache.ambari.server.state.live;

import org.apache.ambari.server.state.fsm.InvalidStateTransitonException;
import org.apache.ambari.server.state.fsm.StateMachine;
import org.apache.ambari.server.state.fsm.StateMachineFactory;

public class JobImpl implements Job {

  private static final StateMachineFactory
    <JobImpl, JobState, JobEventType, JobEvent>
      stateMachineFactory
        = new StateMachineFactory<JobImpl, JobState,
          JobEventType, JobEvent>
            (JobState.INIT)

    // define the state machine of a Job

    .addTransition(JobState.INIT, JobState.IN_PROGRESS,
        JobEventType.JOB_IN_PROGRESS)
    .addTransition(JobState.IN_PROGRESS, JobState.IN_PROGRESS,
        JobEventType.JOB_IN_PROGRESS)
    .addTransition(JobState.IN_PROGRESS, JobState.COMPLETED,
        JobEventType.JOB_COMPLETED)
    .addTransition(JobState.IN_PROGRESS, JobState.FAILED,
        JobEventType.JOB_FAILED)
    .addTransition(JobState.COMPLETED, JobState.INIT,
        JobEventType.JOB_INIT)
    .addTransition(JobState.FAILED, JobState.INIT,
        JobEventType.JOB_INIT)
    .installTopology();

  private final StateMachine<JobState, JobEventType, JobEvent>
      stateMachine;

  public JobImpl() {
    super();
    this.stateMachine = stateMachineFactory.make(this);
  }

  @Override
  public JobState getState() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setState(JobState state) {
    // TODO Auto-generated method stub

  }

  @Override
  public void handleEvent(JobEvent event)
      throws InvalidStateTransitonException {
    // TODO
    stateMachine.doTransition(event.getType(), event);
  }

  @Override
  public JobId getId() {
    // TODO Auto-generated method stub
    return null;
  }

}
