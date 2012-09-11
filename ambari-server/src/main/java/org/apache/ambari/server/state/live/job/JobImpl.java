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

package org.apache.ambari.server.state.live.job;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.ambari.server.state.fsm.InvalidStateTransitonException;
import org.apache.ambari.server.state.fsm.SingleArcTransition;
import org.apache.ambari.server.state.fsm.StateMachine;
import org.apache.ambari.server.state.fsm.StateMachineFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class JobImpl implements Job {

  private static final Log LOG = LogFactory.getLog(JobImpl.class);

  private final Lock readLock;
  private final Lock writeLock;

  private JobId id;

  private long startTime;
  private long lastUpdateTime;
  private long completionTime;

  // TODO
  // need to add job report

  private static final StateMachineFactory
    <JobImpl, JobState, JobEventType, JobEvent>
      stateMachineFactory
        = new StateMachineFactory<JobImpl, JobState,
          JobEventType, JobEvent>
            (JobState.INIT)

    // define the state machine of a Job

    .addTransition(JobState.INIT, JobState.IN_PROGRESS,
        JobEventType.JOB_IN_PROGRESS, new JobProgressUpdateTransition())
    .addTransition(JobState.INIT, JobState.COMPLETED,
        JobEventType.JOB_COMPLETED, new JobCompletedTransition())
    .addTransition(JobState.INIT, JobState.FAILED,
        JobEventType.JOB_FAILED, new JobFailedTransition())
    .addTransition(JobState.INIT, JobState.IN_PROGRESS,
        JobEventType.JOB_IN_PROGRESS, new JobProgressUpdateTransition())
    .addTransition(JobState.IN_PROGRESS, JobState.IN_PROGRESS,
        JobEventType.JOB_IN_PROGRESS, new JobProgressUpdateTransition())
    .addTransition(JobState.IN_PROGRESS, JobState.COMPLETED,
        JobEventType.JOB_COMPLETED, new JobCompletedTransition())
    .addTransition(JobState.IN_PROGRESS, JobState.FAILED,
        JobEventType.JOB_FAILED, new JobFailedTransition())
    .addTransition(JobState.COMPLETED, JobState.INIT,
        JobEventType.JOB_INIT, new NewJobTransition())
    .addTransition(JobState.FAILED, JobState.INIT,
        JobEventType.JOB_INIT, new NewJobTransition())
    .installTopology();

  private final StateMachine<JobState, JobEventType, JobEvent>
      stateMachine;

  public JobImpl(JobId id, long startTime) {
    super();
    this.id = id;
    this.stateMachine = stateMachineFactory.make(this);
    ReadWriteLock rwLock = new ReentrantReadWriteLock();
    this.readLock = rwLock.readLock();
    this.writeLock = rwLock.writeLock();
    this.startTime = startTime;
    this.lastUpdateTime = -1;
    this.completionTime = -1;
  }

  private void reset() {
    try {
      writeLock.lock();
      this.startTime = -1;
      this.lastUpdateTime = -1;
      this.completionTime = -1;
    }
    finally {
      writeLock.unlock();
    }
  }

  static class NewJobTransition
     implements SingleArcTransition<JobImpl, JobEvent> {

    @Override
    public void transition(JobImpl job, JobEvent event) {
      NewJobEvent e = (NewJobEvent) event;
      // TODO audit logs
      job.reset();
      job.setId(e.getJobId());
      job.setStartTime(e.getStartTime());
      LOG.info("Launching a new Job"
          + ", jobId=" + job.getId()
          + ", startTime=" + job.getStartTime());
    }
  }

  static class JobProgressUpdateTransition
      implements SingleArcTransition<JobImpl, JobEvent> {

    @Override
    public void transition(JobImpl job, JobEvent event) {
      JobProgressUpdateEvent e = (JobProgressUpdateEvent) event;
      job.setLastUpdateTime(e.getProgressUpdateTime());
      if (LOG.isDebugEnabled()) {
        LOG.debug("Progress update for Job"
            + ", jobId=" + job.getId()
            + ", startTime=" + job.getStartTime()
            + ", lastUpdateTime=" + job.getLastUpdateTime());
      }
    }
  }

  static class JobCompletedTransition
     implements SingleArcTransition<JobImpl, JobEvent> {

    @Override
    public void transition(JobImpl job, JobEvent event) {
      // TODO audit logs
      JobCompletedEvent e = (JobCompletedEvent) event;
      job.setCompletionTime(e.getCompletionTime());
      job.setLastUpdateTime(e.getCompletionTime());

      LOG.info("Job completed successfully"
          + ", jobId=" + job.getId()
          + ", startTime=" + job.getStartTime()
          + ", completionTime=" + job.getCompletionTime());
    }
  }

  static class JobFailedTransition
      implements SingleArcTransition<JobImpl, JobEvent> {

    @Override
    public void transition(JobImpl job, JobEvent event) {
      // TODO audit logs
      JobFailedEvent e = (JobFailedEvent) event;
      job.setCompletionTime(e.getCompletionTime());
      job.setLastUpdateTime(e.getCompletionTime());
      LOG.info("Job failed to complete"
          + ", jobId=" + job.getId()
          + ", startTime=" + job.getStartTime()
          + ", completionTime=" + job.getCompletionTime());
    }
  }


  @Override
  public JobState getState() {
    try {
      readLock.lock();
      return stateMachine.getCurrentState();
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public void setState(JobState state) {
    try {
      writeLock.lock();
      stateMachine.setCurrentState(state);
    }
    finally {
      writeLock.unlock();
    }
  }

  @Override
  public void handleEvent(JobEvent event)
      throws InvalidStateTransitonException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Handling Job event, eventType=" + event.getType().name()
          + ", event=" + event.toString());
    }
    JobState oldState = getState();
    try {
      writeLock.lock();
      try {
        stateMachine.doTransition(event.getType(), event);
      } catch (InvalidStateTransitonException e) {
        LOG.error("Can't handle Job event at current state"
            + ", jobId=" + this.getId()
            + ", currentState=" + oldState
            + ", eventType=" + event.getType()
            + ", event=" + event);
        throw e;
      }
    }
    finally {
      writeLock.unlock();
    }
    if (oldState != getState()) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Job transitioned to a new state"
            + ", jobId=" + this.getId()
            + ", oldState=" + oldState
            + ", currentState=" + getState()
            + ", eventType=" + event.getType().name()
            + ", event=" + event);
      }
    }
  }

  @Override
  public JobId getId() {
    try {
      readLock.lock();
      return id;
    }
    finally {
      readLock.unlock();
    }
  }

  private void setId(JobId id) {
    try {
      writeLock.lock();
      this.id = id;
    }
    finally {
      writeLock.unlock();
    }
  }

  @Override
  public long getStartTime() {
    try {
      readLock.lock();
      return startTime;
    }
    finally {
      readLock.unlock();
    }
  }

  public void setStartTime(long startTime) {
    try {
      writeLock.lock();
      this.startTime = startTime;
    }
    finally {
      writeLock.unlock();
    }
  }

  @Override
  public long getLastUpdateTime() {
    try {
      readLock.lock();
      return lastUpdateTime;
    }
    finally {
      readLock.unlock();
    }
  }

  public void setLastUpdateTime(long lastUpdateTime) {
    try {
      writeLock.lock();
      this.lastUpdateTime = lastUpdateTime;
    }
    finally {
      writeLock.unlock();
    }

  }

  @Override
  public long getCompletionTime() {
    try {
      readLock.lock();
      return completionTime;
    }
    finally {
      readLock.unlock();
    }
  }

  public void setCompletionTime(long completionTime) {
    try {
      writeLock.lock();
      this.completionTime = completionTime;
    }
    finally {
      writeLock.unlock();
    }
  }


}
