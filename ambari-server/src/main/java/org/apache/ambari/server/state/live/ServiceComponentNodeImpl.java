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

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.ambari.server.state.fsm.InvalidStateTransitonException;
import org.apache.ambari.server.state.fsm.SingleArcTransition;
import org.apache.ambari.server.state.fsm.StateMachine;
import org.apache.ambari.server.state.fsm.StateMachineFactory;
import org.apache.ambari.server.state.live.job.Job;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ServiceComponentNodeImpl implements ServiceComponentNode {

  private static final Log LOG =
      LogFactory.getLog(ServiceComponentNodeImpl.class);

  private final Lock readLock;
  private final Lock writeLock;

  private ServiceComponentNodeState state;

  private final String serviceComponentName;
  private final String hostName;

  private long lastOpStartTime;
  private long lastOpEndTime;
  private long lastOpLastUpdateTime;

  private static final StateMachineFactory
  <ServiceComponentNodeImpl, ServiceComponentNodeLiveState,
  ServiceComponentNodeEventType, ServiceComponentNodeEvent>
    daemonStateMachineFactory
      = new StateMachineFactory<ServiceComponentNodeImpl,
          ServiceComponentNodeLiveState, ServiceComponentNodeEventType,
          ServiceComponentNodeEvent>
          (ServiceComponentNodeLiveState.INIT)

  // define the state machine of a NodeServiceComponent for runnable
  // components

     .addTransition(ServiceComponentNodeLiveState.INIT,
         ServiceComponentNodeLiveState.INSTALLING,
         ServiceComponentNodeEventType.NODE_SVCCOMP_INSTALL,
         new ServiceComponentNodeOpStartedTransition())
     .addTransition(ServiceComponentNodeLiveState.INSTALLING,
         ServiceComponentNodeLiveState.INSTALLED,
         ServiceComponentNodeEventType.NODE_SVCCOMP_OP_SUCCEEDED,
         new ServiceComponentNodeOpCompletedTransition())
     .addTransition(ServiceComponentNodeLiveState.INSTALLING,
         ServiceComponentNodeLiveState.INSTALLING,
         ServiceComponentNodeEventType.NODE_SVCCOMP_OP_IN_PROGRESS,
         new ServiceComponentNodeOpInProgressTransition())
     .addTransition(ServiceComponentNodeLiveState.INSTALLING,
         ServiceComponentNodeLiveState.INSTALL_FAILED,
         ServiceComponentNodeEventType.NODE_SVCCOMP_OP_FAILED,
         new ServiceComponentNodeOpCompletedTransition())

     .addTransition(ServiceComponentNodeLiveState.INSTALL_FAILED,
         ServiceComponentNodeLiveState.INSTALLING,
         ServiceComponentNodeEventType.NODE_SVCCOMP_OP_RESTART,
         new ServiceComponentNodeOpStartedTransition())

     .addTransition(ServiceComponentNodeLiveState.INSTALLED,
         ServiceComponentNodeLiveState.STARTING,
         ServiceComponentNodeEventType.NODE_SVCCOMP_START,
         new ServiceComponentNodeOpStartedTransition())
     .addTransition(ServiceComponentNodeLiveState.INSTALLED,
         ServiceComponentNodeLiveState.UNINSTALLING,
         ServiceComponentNodeEventType.NODE_SVCCOMP_UNINSTALL,
         new ServiceComponentNodeOpStartedTransition())
     .addTransition(ServiceComponentNodeLiveState.INSTALLED,
         ServiceComponentNodeLiveState.INSTALLING,
         ServiceComponentNodeEventType.NODE_SVCCOMP_INSTALL,
         new ServiceComponentNodeOpStartedTransition())

     .addTransition(ServiceComponentNodeLiveState.STARTING,
         ServiceComponentNodeLiveState.STARTING,
         ServiceComponentNodeEventType.NODE_SVCCOMP_OP_IN_PROGRESS,
         new ServiceComponentNodeOpInProgressTransition())
     .addTransition(ServiceComponentNodeLiveState.STARTING,
         ServiceComponentNodeLiveState.STARTED,
         ServiceComponentNodeEventType.NODE_SVCCOMP_OP_SUCCEEDED,
         new ServiceComponentNodeOpCompletedTransition())
     .addTransition(ServiceComponentNodeLiveState.STARTING,
         ServiceComponentNodeLiveState.START_FAILED,
         ServiceComponentNodeEventType.NODE_SVCCOMP_OP_FAILED,
         new ServiceComponentNodeOpCompletedTransition())

     .addTransition(ServiceComponentNodeLiveState.START_FAILED,
         ServiceComponentNodeLiveState.STARTING,
         ServiceComponentNodeEventType.NODE_SVCCOMP_OP_RESTART,
         new ServiceComponentNodeOpStartedTransition())

     .addTransition(ServiceComponentNodeLiveState.STARTED,
         ServiceComponentNodeLiveState.STOPPING,
         ServiceComponentNodeEventType.NODE_SVCCOMP_STOP,
         new ServiceComponentNodeOpStartedTransition())

     .addTransition(ServiceComponentNodeLiveState.STOPPING,
         ServiceComponentNodeLiveState.STOPPING,
         ServiceComponentNodeEventType.NODE_SVCCOMP_OP_IN_PROGRESS,
         new ServiceComponentNodeOpInProgressTransition())
     .addTransition(ServiceComponentNodeLiveState.STOPPING,
         ServiceComponentNodeLiveState.INSTALLED,
         ServiceComponentNodeEventType.NODE_SVCCOMP_OP_SUCCEEDED,
         new ServiceComponentNodeOpCompletedTransition())
     .addTransition(ServiceComponentNodeLiveState.STOPPING,
         ServiceComponentNodeLiveState.STOP_FAILED,
         ServiceComponentNodeEventType.NODE_SVCCOMP_OP_FAILED,
         new ServiceComponentNodeOpCompletedTransition())

     .addTransition(ServiceComponentNodeLiveState.STOP_FAILED,
         ServiceComponentNodeLiveState.STOPPING,
         ServiceComponentNodeEventType.NODE_SVCCOMP_OP_RESTART,
         new ServiceComponentNodeOpStartedTransition())

     .addTransition(ServiceComponentNodeLiveState.UNINSTALLING,
         ServiceComponentNodeLiveState.UNINSTALLING,
         ServiceComponentNodeEventType.NODE_SVCCOMP_OP_IN_PROGRESS,
         new ServiceComponentNodeOpInProgressTransition())
     .addTransition(ServiceComponentNodeLiveState.UNINSTALLING,
         ServiceComponentNodeLiveState.UNINSTALLED,
         ServiceComponentNodeEventType.NODE_SVCCOMP_OP_SUCCEEDED,
         new ServiceComponentNodeOpCompletedTransition())
     .addTransition(ServiceComponentNodeLiveState.UNINSTALLING,
         ServiceComponentNodeLiveState.UNINSTALL_FAILED,
         ServiceComponentNodeEventType.NODE_SVCCOMP_OP_FAILED,
         new ServiceComponentNodeOpCompletedTransition())

     .addTransition(ServiceComponentNodeLiveState.UNINSTALL_FAILED,
         ServiceComponentNodeLiveState.UNINSTALLING,
         ServiceComponentNodeEventType.NODE_SVCCOMP_OP_RESTART,
         new ServiceComponentNodeOpStartedTransition())

     .addTransition(ServiceComponentNodeLiveState.UNINSTALLED,
         ServiceComponentNodeLiveState.INSTALLING,
         ServiceComponentNodeEventType.NODE_SVCCOMP_INSTALL,
         new ServiceComponentNodeOpStartedTransition())

     .addTransition(ServiceComponentNodeLiveState.UNINSTALLED,
         ServiceComponentNodeLiveState.WIPING_OUT,
         ServiceComponentNodeEventType.NODE_SVCCOMP_WIPEOUT,
         new ServiceComponentNodeOpStartedTransition())

     .addTransition(ServiceComponentNodeLiveState.WIPING_OUT,
         ServiceComponentNodeLiveState.WIPING_OUT,
         ServiceComponentNodeEventType.NODE_SVCCOMP_OP_IN_PROGRESS,
         new ServiceComponentNodeOpInProgressTransition())
     .addTransition(ServiceComponentNodeLiveState.WIPING_OUT,
         ServiceComponentNodeLiveState.INIT,
         ServiceComponentNodeEventType.NODE_SVCCOMP_OP_SUCCEEDED,
         new ServiceComponentNodeOpCompletedTransition())
     .addTransition(ServiceComponentNodeLiveState.WIPING_OUT,
         ServiceComponentNodeLiveState.WIPEOUT_FAILED,
         ServiceComponentNodeEventType.NODE_SVCCOMP_OP_FAILED,
         new ServiceComponentNodeOpCompletedTransition())
     .addTransition(ServiceComponentNodeLiveState.WIPEOUT_FAILED,
         ServiceComponentNodeLiveState.WIPING_OUT,
         ServiceComponentNodeEventType.NODE_SVCCOMP_OP_RESTART,
         new ServiceComponentNodeOpStartedTransition())

     .installTopology();

  private static final StateMachineFactory
  <ServiceComponentNodeImpl, ServiceComponentNodeLiveState,
  ServiceComponentNodeEventType, ServiceComponentNodeEvent>
    clientStateMachineFactory
      = new StateMachineFactory<ServiceComponentNodeImpl,
          ServiceComponentNodeLiveState, ServiceComponentNodeEventType,
          ServiceComponentNodeEvent>
          (ServiceComponentNodeLiveState.INIT)

  // define the state machine of a NodeServiceComponent for client only
  // components

     .addTransition(ServiceComponentNodeLiveState.INIT,
         ServiceComponentNodeLiveState.INSTALLING,
         ServiceComponentNodeEventType.NODE_SVCCOMP_INSTALL,
         new ServiceComponentNodeOpStartedTransition())

     .addTransition(ServiceComponentNodeLiveState.INSTALLING,
         ServiceComponentNodeLiveState.INSTALLED,
         ServiceComponentNodeEventType.NODE_SVCCOMP_OP_SUCCEEDED,
         new ServiceComponentNodeOpCompletedTransition())
     .addTransition(ServiceComponentNodeLiveState.INSTALLING,
         ServiceComponentNodeLiveState.INSTALLING,
         ServiceComponentNodeEventType.NODE_SVCCOMP_OP_IN_PROGRESS,
         new ServiceComponentNodeOpInProgressTransition())
     .addTransition(ServiceComponentNodeLiveState.INSTALLING,
         ServiceComponentNodeLiveState.INSTALL_FAILED,
         ServiceComponentNodeEventType.NODE_SVCCOMP_OP_FAILED,
         new ServiceComponentNodeOpCompletedTransition())

     .addTransition(ServiceComponentNodeLiveState.INSTALL_FAILED,
         ServiceComponentNodeLiveState.INSTALLING,
         ServiceComponentNodeEventType.NODE_SVCCOMP_OP_RESTART,
         new ServiceComponentNodeOpStartedTransition())

     .addTransition(ServiceComponentNodeLiveState.INSTALLED,
         ServiceComponentNodeLiveState.UNINSTALLING,
         ServiceComponentNodeEventType.NODE_SVCCOMP_UNINSTALL,
         new ServiceComponentNodeOpStartedTransition())
     .addTransition(ServiceComponentNodeLiveState.INSTALLED,
         ServiceComponentNodeLiveState.INSTALLING,
         ServiceComponentNodeEventType.NODE_SVCCOMP_INSTALL,
         new ServiceComponentNodeOpStartedTransition())

     .addTransition(ServiceComponentNodeLiveState.UNINSTALLING,
         ServiceComponentNodeLiveState.UNINSTALLING,
         ServiceComponentNodeEventType.NODE_SVCCOMP_OP_IN_PROGRESS,
         new ServiceComponentNodeOpInProgressTransition())
     .addTransition(ServiceComponentNodeLiveState.UNINSTALLING,
         ServiceComponentNodeLiveState.UNINSTALLED,
         ServiceComponentNodeEventType.NODE_SVCCOMP_OP_SUCCEEDED,
         new ServiceComponentNodeOpCompletedTransition())
     .addTransition(ServiceComponentNodeLiveState.UNINSTALLING,
         ServiceComponentNodeLiveState.UNINSTALL_FAILED,
         ServiceComponentNodeEventType.NODE_SVCCOMP_OP_FAILED,
         new ServiceComponentNodeOpCompletedTransition())

     .addTransition(ServiceComponentNodeLiveState.UNINSTALL_FAILED,
         ServiceComponentNodeLiveState.UNINSTALLING,
         ServiceComponentNodeEventType.NODE_SVCCOMP_OP_RESTART,
         new ServiceComponentNodeOpStartedTransition())

     .addTransition(ServiceComponentNodeLiveState.UNINSTALLED,
         ServiceComponentNodeLiveState.INSTALLING,
         ServiceComponentNodeEventType.NODE_SVCCOMP_INSTALL,
         new ServiceComponentNodeOpStartedTransition())

     .addTransition(ServiceComponentNodeLiveState.UNINSTALLED,
         ServiceComponentNodeLiveState.WIPING_OUT,
         ServiceComponentNodeEventType.NODE_SVCCOMP_WIPEOUT,
         new ServiceComponentNodeOpStartedTransition())

     .addTransition(ServiceComponentNodeLiveState.WIPING_OUT,
         ServiceComponentNodeLiveState.WIPING_OUT,
         ServiceComponentNodeEventType.NODE_SVCCOMP_OP_IN_PROGRESS,
         new ServiceComponentNodeOpInProgressTransition())
     .addTransition(ServiceComponentNodeLiveState.WIPING_OUT,
         ServiceComponentNodeLiveState.INIT,
         ServiceComponentNodeEventType.NODE_SVCCOMP_OP_SUCCEEDED,
         new ServiceComponentNodeOpCompletedTransition())
     .addTransition(ServiceComponentNodeLiveState.WIPING_OUT,
         ServiceComponentNodeLiveState.WIPEOUT_FAILED,
         ServiceComponentNodeEventType.NODE_SVCCOMP_OP_FAILED,
         new ServiceComponentNodeOpCompletedTransition())
     .addTransition(ServiceComponentNodeLiveState.WIPEOUT_FAILED,
         ServiceComponentNodeLiveState.WIPING_OUT,
         ServiceComponentNodeEventType.NODE_SVCCOMP_OP_RESTART,
         new ServiceComponentNodeOpStartedTransition())

     .installTopology();


  private final StateMachine<ServiceComponentNodeLiveState,
      ServiceComponentNodeEventType, ServiceComponentNodeEvent> stateMachine;

  static class ServiceComponentNodeOpCompletedTransition
     implements SingleArcTransition<ServiceComponentNodeImpl,
         ServiceComponentNodeEvent> {

    @Override
    public void transition(ServiceComponentNodeImpl impl,
        ServiceComponentNodeEvent event) {
      // TODO Audit logs
      impl.updateLastOpInfo(event.getType(), event.getOpTimestamp());
    }

  }

  static class ServiceComponentNodeOpStartedTransition
    implements SingleArcTransition<ServiceComponentNodeImpl,
        ServiceComponentNodeEvent> {

    @Override
    public void transition(ServiceComponentNodeImpl impl,
        ServiceComponentNodeEvent event) {
      // TODO Audit logs
      impl.updateLastOpInfo(event.getType(), event.getOpTimestamp());
    }
  }

  static class ServiceComponentNodeOpInProgressTransition
    implements SingleArcTransition<ServiceComponentNodeImpl,
        ServiceComponentNodeEvent> {

    @Override
    public void transition(ServiceComponentNodeImpl impl,
        ServiceComponentNodeEvent event) {
      // TODO Audit logs
      impl.updateLastOpInfo(event.getType(), event.getOpTimestamp());
    }
  }


  private void resetLastOpInfo() {
    try {
      writeLock.lock();
      setLastOpStartTime(-1);
      setLastOpLastUpdateTime(-1);
      setLastOpEndTime(-1);
    }
    finally {
      writeLock.unlock();
    }
  }

  private void updateLastOpInfo(ServiceComponentNodeEventType eventType,
      long time) {
    try {
      writeLock.lock();
      switch (eventType) {
        case NODE_SVCCOMP_INSTALL:
        case NODE_SVCCOMP_START:
        case NODE_SVCCOMP_STOP:
        case NODE_SVCCOMP_UNINSTALL:
        case NODE_SVCCOMP_WIPEOUT:
        case NODE_SVCCOMP_OP_RESTART:
          resetLastOpInfo();
          setLastOpStartTime(time);
          break;
        case NODE_SVCCOMP_OP_FAILED:
        case NODE_SVCCOMP_OP_SUCCEEDED:
          setLastOpLastUpdateTime(time);
          setLastOpEndTime(time);
          break;
        case NODE_SVCCOMP_OP_IN_PROGRESS:
          setLastOpLastUpdateTime(time);
          break;
      }
    }
    finally {
      writeLock.unlock();
    }
  }

  public ServiceComponentNodeImpl(String serviceComponentName,
      String hostName, boolean isClient) {
    super();
    if (isClient) {
      this.stateMachine = clientStateMachineFactory.make(this);
    } else {
      this.stateMachine = daemonStateMachineFactory.make(this);
    }
    ReadWriteLock rwLock = new ReentrantReadWriteLock();
    this.readLock = rwLock.readLock();
    this.writeLock = rwLock.writeLock();
    this.serviceComponentName = serviceComponentName;
    this.hostName = hostName;
    this.state = new ServiceComponentNodeState();
    this.resetLastOpInfo();
  }

  @Override
  public ServiceComponentNodeState getState() {
    try {
      readLock.lock();
      return state;
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public void setState(ServiceComponentNodeState state) {
    try {
      writeLock.lock();
      this.state = state;
      stateMachine.setCurrentState(state.getLiveState());
    }
    finally {
      writeLock.unlock();
    }
  }

  @Override
  public void handleEvent(ServiceComponentNodeEvent event)
      throws InvalidStateTransitonException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Handling ServiceComponentNodeEvent event,"
          + " eventType=" + event.getType().name()
          + ", event=" + event.toString());
    }
    ServiceComponentNodeState oldState = getState();
    try {
      writeLock.lock();
      try {
        stateMachine.doTransition(event.getType(), event);
        state.setState(stateMachine.getCurrentState());
        // TODO Audit logs
      } catch (InvalidStateTransitonException e) {
        LOG.error("Can't handle ServiceComponentNodeEvent event at"
            + " current state"
            + ", serviceComponentName=" + this.getServiceComponentName()
            + ", hostName=" + this.getNodeName()
            + ", currentState=" + oldState
            + ", eventType=" + event.getType()
            + ", event=" + event);
        throw e;
      }
    }
    finally {
      writeLock.unlock();
    }
    if (oldState.getLiveState() != getState().getLiveState()) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("ServiceComponentNode transitioned to a new state"
            + ", serviceComponentName=" + this.getServiceComponentName()
            + ", hostName=" + this.getNodeName()
            + ", oldState=" + oldState
            + ", currentState=" + getState()
            + ", eventType=" + event.getType().name()
            + ", event=" + event);
      }
    }
  }

  @Override
  public List<Job> getJobs() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getServiceComponentName() {
    return serviceComponentName;
  }

  @Override
  public String getNodeName() {
    return hostName;
  }

  /**
   * @return the lastOpStartTime
   */
  public long getLastOpStartTime() {
    try {
      readLock.lock();
      return lastOpStartTime;
    }
    finally {
      readLock.unlock();
    }
  }

  /**
   * @param lastOpStartTime the lastOpStartTime to set
   */
  public void setLastOpStartTime(long lastOpStartTime) {
    try {
      writeLock.lock();
      this.lastOpStartTime = lastOpStartTime;
    }
    finally {
      writeLock.unlock();
    }
  }

  /**
   * @return the lastOpEndTime
   */
  public long getLastOpEndTime() {
    try {
      readLock.lock();
      return lastOpEndTime;
    }
    finally {
      readLock.unlock();
    }
  }

  /**
   * @param lastOpEndTime the lastOpEndTime to set
   */
  public void setLastOpEndTime(long lastOpEndTime) {
    try {
      writeLock.lock();
      this.lastOpEndTime = lastOpEndTime;
    }
    finally {
      writeLock.unlock();
    }
  }

  /**
   * @return the lastOpLastUpdateTime
   */
  public long getLastOpLastUpdateTime() {
    try {
      readLock.lock();
      return lastOpLastUpdateTime;
    }
    finally {
      readLock.unlock();
    }
  }

  /**
   * @param lastOpLastUpdateTime the lastOpLastUpdateTime to set
   */
  public void setLastOpLastUpdateTime(long lastOpLastUpdateTime) {
    try {
      writeLock.lock();
      this.lastOpLastUpdateTime = lastOpLastUpdateTime;
    }
    finally {
      writeLock.unlock();
    }
  }

}
