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

package org.apache.ambari.server.state.live.svccomphost;

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

public class ServiceComponentHostImpl implements ServiceComponentHost {

  private static final Log LOG =
      LogFactory.getLog(ServiceComponentHostImpl.class);

  private final Lock readLock;
  private final Lock writeLock;

  private ServiceComponentHostState state;

  private final long clusterId;
  private final String serviceName;
  private final String serviceComponentName;
  private final String hostName;

  private long lastOpStartTime;
  private long lastOpEndTime;
  private long lastOpLastUpdateTime;

  private static final StateMachineFactory
  <ServiceComponentHostImpl, ServiceComponentHostLiveState,
  ServiceComponentHostEventType, ServiceComponentHostEvent>
    daemonStateMachineFactory
      = new StateMachineFactory<ServiceComponentHostImpl,
          ServiceComponentHostLiveState, ServiceComponentHostEventType,
          ServiceComponentHostEvent>
          (ServiceComponentHostLiveState.INIT)

  // define the state machine of a HostServiceComponent for runnable
  // components

     .addTransition(ServiceComponentHostLiveState.INIT,
         ServiceComponentHostLiveState.INSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_INSTALL,
         new ServiceComponentHostOpStartedTransition())
     .addTransition(ServiceComponentHostLiveState.INSTALLING,
         ServiceComponentHostLiveState.INSTALLED,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_SUCCEEDED,
         new ServiceComponentHostOpCompletedTransition())
     .addTransition(ServiceComponentHostLiveState.INSTALLING,
         ServiceComponentHostLiveState.INSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_IN_PROGRESS,
         new ServiceComponentHostOpInProgressTransition())
     .addTransition(ServiceComponentHostLiveState.INSTALLING,
         ServiceComponentHostLiveState.INSTALL_FAILED,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_FAILED,
         new ServiceComponentHostOpCompletedTransition())

     .addTransition(ServiceComponentHostLiveState.INSTALL_FAILED,
         ServiceComponentHostLiveState.INSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_RESTART,
         new ServiceComponentHostOpStartedTransition())

     .addTransition(ServiceComponentHostLiveState.INSTALLED,
         ServiceComponentHostLiveState.STARTING,
         ServiceComponentHostEventType.HOST_SVCCOMP_START,
         new ServiceComponentHostOpStartedTransition())
     .addTransition(ServiceComponentHostLiveState.INSTALLED,
         ServiceComponentHostLiveState.UNINSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_UNINSTALL,
         new ServiceComponentHostOpStartedTransition())
     .addTransition(ServiceComponentHostLiveState.INSTALLED,
         ServiceComponentHostLiveState.INSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_INSTALL,
         new ServiceComponentHostOpStartedTransition())

     .addTransition(ServiceComponentHostLiveState.STARTING,
         ServiceComponentHostLiveState.STARTING,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_IN_PROGRESS,
         new ServiceComponentHostOpInProgressTransition())
     .addTransition(ServiceComponentHostLiveState.STARTING,
         ServiceComponentHostLiveState.STARTED,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_SUCCEEDED,
         new ServiceComponentHostOpCompletedTransition())
     .addTransition(ServiceComponentHostLiveState.STARTING,
         ServiceComponentHostLiveState.START_FAILED,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_FAILED,
         new ServiceComponentHostOpCompletedTransition())

     .addTransition(ServiceComponentHostLiveState.START_FAILED,
         ServiceComponentHostLiveState.STARTING,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_RESTART,
         new ServiceComponentHostOpStartedTransition())

     .addTransition(ServiceComponentHostLiveState.STARTED,
         ServiceComponentHostLiveState.STOPPING,
         ServiceComponentHostEventType.HOST_SVCCOMP_STOP,
         new ServiceComponentHostOpStartedTransition())

     .addTransition(ServiceComponentHostLiveState.STOPPING,
         ServiceComponentHostLiveState.STOPPING,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_IN_PROGRESS,
         new ServiceComponentHostOpInProgressTransition())
     .addTransition(ServiceComponentHostLiveState.STOPPING,
         ServiceComponentHostLiveState.INSTALLED,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_SUCCEEDED,
         new ServiceComponentHostOpCompletedTransition())
     .addTransition(ServiceComponentHostLiveState.STOPPING,
         ServiceComponentHostLiveState.STOP_FAILED,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_FAILED,
         new ServiceComponentHostOpCompletedTransition())

     .addTransition(ServiceComponentHostLiveState.STOP_FAILED,
         ServiceComponentHostLiveState.STOPPING,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_RESTART,
         new ServiceComponentHostOpStartedTransition())

     .addTransition(ServiceComponentHostLiveState.UNINSTALLING,
         ServiceComponentHostLiveState.UNINSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_IN_PROGRESS,
         new ServiceComponentHostOpInProgressTransition())
     .addTransition(ServiceComponentHostLiveState.UNINSTALLING,
         ServiceComponentHostLiveState.UNINSTALLED,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_SUCCEEDED,
         new ServiceComponentHostOpCompletedTransition())
     .addTransition(ServiceComponentHostLiveState.UNINSTALLING,
         ServiceComponentHostLiveState.UNINSTALL_FAILED,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_FAILED,
         new ServiceComponentHostOpCompletedTransition())

     .addTransition(ServiceComponentHostLiveState.UNINSTALL_FAILED,
         ServiceComponentHostLiveState.UNINSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_RESTART,
         new ServiceComponentHostOpStartedTransition())

     .addTransition(ServiceComponentHostLiveState.UNINSTALLED,
         ServiceComponentHostLiveState.INSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_INSTALL,
         new ServiceComponentHostOpStartedTransition())

     .addTransition(ServiceComponentHostLiveState.UNINSTALLED,
         ServiceComponentHostLiveState.WIPING_OUT,
         ServiceComponentHostEventType.HOST_SVCCOMP_WIPEOUT,
         new ServiceComponentHostOpStartedTransition())

     .addTransition(ServiceComponentHostLiveState.WIPING_OUT,
         ServiceComponentHostLiveState.WIPING_OUT,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_IN_PROGRESS,
         new ServiceComponentHostOpInProgressTransition())
     .addTransition(ServiceComponentHostLiveState.WIPING_OUT,
         ServiceComponentHostLiveState.INIT,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_SUCCEEDED,
         new ServiceComponentHostOpCompletedTransition())
     .addTransition(ServiceComponentHostLiveState.WIPING_OUT,
         ServiceComponentHostLiveState.WIPEOUT_FAILED,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_FAILED,
         new ServiceComponentHostOpCompletedTransition())
     .addTransition(ServiceComponentHostLiveState.WIPEOUT_FAILED,
         ServiceComponentHostLiveState.WIPING_OUT,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_RESTART,
         new ServiceComponentHostOpStartedTransition())

     .installTopology();

  private static final StateMachineFactory
  <ServiceComponentHostImpl, ServiceComponentHostLiveState,
  ServiceComponentHostEventType, ServiceComponentHostEvent>
    clientStateMachineFactory
      = new StateMachineFactory<ServiceComponentHostImpl,
          ServiceComponentHostLiveState, ServiceComponentHostEventType,
          ServiceComponentHostEvent>
          (ServiceComponentHostLiveState.INIT)

  // define the state machine of a HostServiceComponent for client only
  // components

     .addTransition(ServiceComponentHostLiveState.INIT,
         ServiceComponentHostLiveState.INSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_INSTALL,
         new ServiceComponentHostOpStartedTransition())

     .addTransition(ServiceComponentHostLiveState.INSTALLING,
         ServiceComponentHostLiveState.INSTALLED,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_SUCCEEDED,
         new ServiceComponentHostOpCompletedTransition())
     .addTransition(ServiceComponentHostLiveState.INSTALLING,
         ServiceComponentHostLiveState.INSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_IN_PROGRESS,
         new ServiceComponentHostOpInProgressTransition())
     .addTransition(ServiceComponentHostLiveState.INSTALLING,
         ServiceComponentHostLiveState.INSTALL_FAILED,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_FAILED,
         new ServiceComponentHostOpCompletedTransition())

     .addTransition(ServiceComponentHostLiveState.INSTALL_FAILED,
         ServiceComponentHostLiveState.INSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_RESTART,
         new ServiceComponentHostOpStartedTransition())

     .addTransition(ServiceComponentHostLiveState.INSTALLED,
         ServiceComponentHostLiveState.UNINSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_UNINSTALL,
         new ServiceComponentHostOpStartedTransition())
     .addTransition(ServiceComponentHostLiveState.INSTALLED,
         ServiceComponentHostLiveState.INSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_INSTALL,
         new ServiceComponentHostOpStartedTransition())

     .addTransition(ServiceComponentHostLiveState.UNINSTALLING,
         ServiceComponentHostLiveState.UNINSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_IN_PROGRESS,
         new ServiceComponentHostOpInProgressTransition())
     .addTransition(ServiceComponentHostLiveState.UNINSTALLING,
         ServiceComponentHostLiveState.UNINSTALLED,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_SUCCEEDED,
         new ServiceComponentHostOpCompletedTransition())
     .addTransition(ServiceComponentHostLiveState.UNINSTALLING,
         ServiceComponentHostLiveState.UNINSTALL_FAILED,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_FAILED,
         new ServiceComponentHostOpCompletedTransition())

     .addTransition(ServiceComponentHostLiveState.UNINSTALL_FAILED,
         ServiceComponentHostLiveState.UNINSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_RESTART,
         new ServiceComponentHostOpStartedTransition())

     .addTransition(ServiceComponentHostLiveState.UNINSTALLED,
         ServiceComponentHostLiveState.INSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_INSTALL,
         new ServiceComponentHostOpStartedTransition())

     .addTransition(ServiceComponentHostLiveState.UNINSTALLED,
         ServiceComponentHostLiveState.WIPING_OUT,
         ServiceComponentHostEventType.HOST_SVCCOMP_WIPEOUT,
         new ServiceComponentHostOpStartedTransition())

     .addTransition(ServiceComponentHostLiveState.WIPING_OUT,
         ServiceComponentHostLiveState.WIPING_OUT,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_IN_PROGRESS,
         new ServiceComponentHostOpInProgressTransition())
     .addTransition(ServiceComponentHostLiveState.WIPING_OUT,
         ServiceComponentHostLiveState.INIT,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_SUCCEEDED,
         new ServiceComponentHostOpCompletedTransition())
     .addTransition(ServiceComponentHostLiveState.WIPING_OUT,
         ServiceComponentHostLiveState.WIPEOUT_FAILED,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_FAILED,
         new ServiceComponentHostOpCompletedTransition())
     .addTransition(ServiceComponentHostLiveState.WIPEOUT_FAILED,
         ServiceComponentHostLiveState.WIPING_OUT,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_RESTART,
         new ServiceComponentHostOpStartedTransition())

     .installTopology();


  private final StateMachine<ServiceComponentHostLiveState,
      ServiceComponentHostEventType, ServiceComponentHostEvent> stateMachine;

  static class ServiceComponentHostOpCompletedTransition
     implements SingleArcTransition<ServiceComponentHostImpl,
         ServiceComponentHostEvent> {

    @Override
    public void transition(ServiceComponentHostImpl impl,
        ServiceComponentHostEvent event) {
      // TODO Audit logs
      impl.updateLastOpInfo(event.getType(), event.getOpTimestamp());
    }

  }

  static class ServiceComponentHostOpStartedTransition
    implements SingleArcTransition<ServiceComponentHostImpl,
        ServiceComponentHostEvent> {

    @Override
    public void transition(ServiceComponentHostImpl impl,
        ServiceComponentHostEvent event) {
      // TODO Audit logs
      impl.updateLastOpInfo(event.getType(), event.getOpTimestamp());
    }
  }

  static class ServiceComponentHostOpInProgressTransition
    implements SingleArcTransition<ServiceComponentHostImpl,
        ServiceComponentHostEvent> {

    @Override
    public void transition(ServiceComponentHostImpl impl,
        ServiceComponentHostEvent event) {
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

  private void updateLastOpInfo(ServiceComponentHostEventType eventType,
      long time) {
    try {
      writeLock.lock();
      switch (eventType) {
        case HOST_SVCCOMP_INSTALL:
        case HOST_SVCCOMP_START:
        case HOST_SVCCOMP_STOP:
        case HOST_SVCCOMP_UNINSTALL:
        case HOST_SVCCOMP_WIPEOUT:
        case HOST_SVCCOMP_OP_RESTART:
          resetLastOpInfo();
          setLastOpStartTime(time);
          break;
        case HOST_SVCCOMP_OP_FAILED:
        case HOST_SVCCOMP_OP_SUCCEEDED:
          setLastOpLastUpdateTime(time);
          setLastOpEndTime(time);
          break;
        case HOST_SVCCOMP_OP_IN_PROGRESS:
          setLastOpLastUpdateTime(time);
          break;
      }
    }
    finally {
      writeLock.unlock();
    }
  }

  public ServiceComponentHostImpl(long clusterId,
      String serviceName, String serviceComponentName,
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
    this.clusterId = clusterId;
    this.serviceName = serviceName;
    this.serviceComponentName = serviceComponentName;
    this.hostName = hostName;
    this.state = new ServiceComponentHostState();
    this.resetLastOpInfo();
  }

  @Override
  public ServiceComponentHostState getState() {
    try {
      readLock.lock();
      return state;
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public void setState(ServiceComponentHostState state) {
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
  public void handleEvent(ServiceComponentHostEvent event)
      throws InvalidStateTransitonException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Handling ServiceComponentHostEvent event,"
          + " eventType=" + event.getType().name()
          + ", event=" + event.toString());
    }
    ServiceComponentHostState oldState = getState();
    try {
      writeLock.lock();
      try {
        stateMachine.doTransition(event.getType(), event);
        state.setState(stateMachine.getCurrentState());
        // TODO Audit logs
      } catch (InvalidStateTransitonException e) {
        LOG.error("Can't handle ServiceComponentHostEvent event at"
            + " current state"
            + ", serviceComponentName=" + this.getServiceComponentName()
            + ", hostName=" + this.getHostName()
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
        LOG.debug("ServiceComponentHost transitioned to a new state"
            + ", serviceComponentName=" + this.getServiceComponentName()
            + ", hostName=" + this.getHostName()
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
  public String getHostName() {
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

  @Override
  public long getClusterId() {
    return clusterId;
  }

  @Override
  public String getServiceName() {
    return serviceName;
  }

}
