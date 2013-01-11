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

package org.apache.ambari.server.state.svccomphost;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.ServiceComponentHostResponse;
import org.apache.ambari.server.orm.dao.HostComponentConfigMappingDAO;
import org.apache.ambari.server.orm.dao.HostComponentDesiredConfigMappingDAO;
import org.apache.ambari.server.orm.dao.HostComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.HostComponentStateDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.ServiceComponentDesiredStateDAO;
import org.apache.ambari.server.orm.entities.HostComponentConfigMappingEntity;
import org.apache.ambari.server.orm.entities.HostComponentDesiredConfigMappingEntity;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntityPK;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.orm.entities.HostComponentStateEntityPK;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntityPK;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceComponentHostEvent;
import org.apache.ambari.server.state.ServiceComponentHostEventType;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.apache.ambari.server.state.fsm.SingleArcTransition;
import org.apache.ambari.server.state.fsm.StateMachine;
import org.apache.ambari.server.state.fsm.StateMachineFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.persist.Transactional;

public class ServiceComponentHostImpl implements ServiceComponentHost {

  private static final Logger LOG =
      LoggerFactory.getLogger(ServiceComponentHostImpl.class);

  // FIXME need more debug logs

  private final Lock readLock;
  private final Lock writeLock;

  private final ServiceComponent serviceComponent;
  private final Host host;
  private boolean persisted = false;

  @Inject
  Gson gson;
  @Inject
  HostComponentStateDAO hostComponentStateDAO;
  @Inject
  HostComponentDesiredStateDAO hostComponentDesiredStateDAO;
  @Inject
  HostDAO hostDAO;
  @Inject
  ServiceComponentDesiredStateDAO serviceComponentDesiredStateDAO;
  @Inject
  Clusters clusters;
  @Inject
  HostComponentDesiredConfigMappingDAO
      hostComponentDesiredConfigMappingDAO;
  @Inject
  HostComponentConfigMappingDAO
      hostComponentConfigMappingDAO;

  private HostComponentStateEntity stateEntity;
  private HostComponentDesiredStateEntity desiredStateEntity;

  private Map<String, String> configs;
  private Map<String, String> desiredConfigs;

  private long lastOpStartTime;
  private long lastOpEndTime;
  private long lastOpLastUpdateTime;

  private static final StateMachineFactory
  <ServiceComponentHostImpl, State,
  ServiceComponentHostEventType, ServiceComponentHostEvent>
    daemonStateMachineFactory
      = new StateMachineFactory<ServiceComponentHostImpl,
          State, ServiceComponentHostEventType,
          ServiceComponentHostEvent>
          (State.INIT)

  // define the state machine of a HostServiceComponent for runnable
  // components

     .addTransition(State.INIT,
         State.INSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_INSTALL,
         new ServiceComponentHostOpStartedTransition())
     .addTransition(State.INSTALLING,
         State.INSTALLED,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_SUCCEEDED,
         new ServiceComponentHostOpCompletedTransition())
         
     .addTransition(State.INSTALLED,
         State.INSTALLED,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_SUCCEEDED,
         new ServiceComponentHostOpCompletedTransition())
         
     .addTransition(State.INSTALLING,
         State.INSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_IN_PROGRESS,
         new ServiceComponentHostOpInProgressTransition())
     .addTransition(State.INSTALLING,
         State.INSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_INSTALL,
         new ServiceComponentHostOpStartedTransition())

     .addTransition(State.INSTALLING,
         State.INSTALL_FAILED,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_FAILED,
         new ServiceComponentHostOpCompletedTransition())

     .addTransition(State.INSTALL_FAILED,
         State.INSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_RESTART,
         new ServiceComponentHostOpStartedTransition())
     .addTransition(State.INSTALL_FAILED,
         State.INSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_INSTALL,
         new ServiceComponentHostOpStartedTransition())

     .addTransition(State.INSTALLED,
         State.STARTING,
         ServiceComponentHostEventType.HOST_SVCCOMP_START,
         new ServiceComponentHostOpStartedTransition())
     .addTransition(State.INSTALLED,
         State.UNINSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_UNINSTALL,
         new ServiceComponentHostOpStartedTransition())
     .addTransition(State.INSTALLED,
         State.INSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_INSTALL,
         new ServiceComponentHostOpStartedTransition())
     .addTransition(State.INSTALLED,
         State.STOPPING,
         ServiceComponentHostEventType.HOST_SVCCOMP_STOP,
         new ServiceComponentHostOpStartedTransition())

     .addTransition(State.STARTING,
         State.STARTING,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_IN_PROGRESS,
         new ServiceComponentHostOpInProgressTransition())
         
     .addTransition(State.STARTING,
         State.STARTING,
         ServiceComponentHostEventType.HOST_SVCCOMP_START,
         new ServiceComponentHostOpStartedTransition())
         
     .addTransition(State.STARTING,
         State.STARTED,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_SUCCEEDED,
         new ServiceComponentHostOpCompletedTransition())
         
     .addTransition(State.STARTING,
         State.START_FAILED,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_FAILED,
         new ServiceComponentHostOpCompletedTransition())

     .addTransition(State.START_FAILED,
         State.STARTING,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_RESTART,
         new ServiceComponentHostOpStartedTransition())
     .addTransition(State.START_FAILED,
         State.STARTING,
         ServiceComponentHostEventType.HOST_SVCCOMP_START,
         new ServiceComponentHostOpStartedTransition())
     .addTransition(State.START_FAILED,
         State.STOPPING,
         ServiceComponentHostEventType.HOST_SVCCOMP_STOP,
         new ServiceComponentHostOpStartedTransition())

     .addTransition(State.STARTED,
         State.STOPPING,
         ServiceComponentHostEventType.HOST_SVCCOMP_STOP,
         new ServiceComponentHostOpStartedTransition())

     .addTransition(State.STOPPING,
         State.STOPPING,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_IN_PROGRESS,
         new ServiceComponentHostOpInProgressTransition())
     .addTransition(State.STOPPING,
         State.INSTALLED,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_SUCCEEDED,
         new ServiceComponentHostOpCompletedTransition())
     .addTransition(State.STOPPING,
         State.STOP_FAILED,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_FAILED,
         new ServiceComponentHostOpCompletedTransition())

     .addTransition(State.STOP_FAILED,
         State.STOPPING,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_RESTART,
         new ServiceComponentHostOpStartedTransition())
     .addTransition(State.STOP_FAILED,
         State.STOPPING,
         ServiceComponentHostEventType.HOST_SVCCOMP_STOP,
         new ServiceComponentHostOpStartedTransition())

     .addTransition(State.UNINSTALLING,
         State.UNINSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_IN_PROGRESS,
         new ServiceComponentHostOpInProgressTransition())
     .addTransition(State.UNINSTALLING,
         State.UNINSTALLED,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_SUCCEEDED,
         new ServiceComponentHostOpCompletedTransition())
     .addTransition(State.UNINSTALLING,
         State.UNINSTALL_FAILED,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_FAILED,
         new ServiceComponentHostOpCompletedTransition())

     .addTransition(State.UNINSTALL_FAILED,
         State.UNINSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_RESTART,
         new ServiceComponentHostOpStartedTransition())
     .addTransition(State.UNINSTALL_FAILED,
         State.UNINSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_UNINSTALL,
         new ServiceComponentHostOpStartedTransition())

     .addTransition(State.UNINSTALLED,
         State.INSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_INSTALL,
         new ServiceComponentHostOpStartedTransition())

     .addTransition(State.UNINSTALLED,
         State.WIPING_OUT,
         ServiceComponentHostEventType.HOST_SVCCOMP_WIPEOUT,
         new ServiceComponentHostOpStartedTransition())

     .addTransition(State.WIPING_OUT,
         State.WIPING_OUT,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_IN_PROGRESS,
         new ServiceComponentHostOpInProgressTransition())
     .addTransition(State.WIPING_OUT,
         State.INIT,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_SUCCEEDED,
         new ServiceComponentHostOpCompletedTransition())
     .addTransition(State.WIPING_OUT,
         State.WIPEOUT_FAILED,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_FAILED,
         new ServiceComponentHostOpCompletedTransition())

     .addTransition(State.WIPEOUT_FAILED,
         State.WIPING_OUT,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_RESTART,
         new ServiceComponentHostOpStartedTransition())
     .addTransition(State.WIPEOUT_FAILED,
         State.WIPING_OUT,
         ServiceComponentHostEventType.HOST_SVCCOMP_WIPEOUT,
         new ServiceComponentHostOpStartedTransition())

     .installTopology();

  private static final StateMachineFactory
  <ServiceComponentHostImpl, State,
  ServiceComponentHostEventType, ServiceComponentHostEvent>
    clientStateMachineFactory
      = new StateMachineFactory<ServiceComponentHostImpl,
          State, ServiceComponentHostEventType,
          ServiceComponentHostEvent>
          (State.INIT)

  // define the state machine of a HostServiceComponent for client only
  // components

     .addTransition(State.INIT,
         State.INSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_INSTALL,
         new ServiceComponentHostOpStartedTransition())

     .addTransition(State.INSTALLING,
         State.INSTALLED,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_SUCCEEDED,
         new ServiceComponentHostOpCompletedTransition())
    
     .addTransition(State.INSTALLED,
         State.INSTALLED,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_SUCCEEDED,
         new ServiceComponentHostOpCompletedTransition())
    
     .addTransition(State.INSTALLING,
         State.INSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_INSTALL,
         new ServiceComponentHostOpStartedTransition())
     
     .addTransition(State.INSTALLING,
         State.INSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_IN_PROGRESS,
         new ServiceComponentHostOpInProgressTransition())
     .addTransition(State.INSTALLING,
         State.INSTALL_FAILED,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_FAILED,
         new ServiceComponentHostOpCompletedTransition())

     .addTransition(State.INSTALL_FAILED,
         State.INSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_RESTART,
         new ServiceComponentHostOpStartedTransition())
     .addTransition(State.INSTALL_FAILED,
         State.INSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_INSTALL,
         new ServiceComponentHostOpStartedTransition())

     .addTransition(State.INSTALLED,
         State.UNINSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_UNINSTALL,
         new ServiceComponentHostOpStartedTransition())
     .addTransition(State.INSTALLED,
         State.INSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_INSTALL,
         new ServiceComponentHostOpStartedTransition())

     .addTransition(State.UNINSTALLING,
         State.UNINSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_IN_PROGRESS,
         new ServiceComponentHostOpInProgressTransition())
     .addTransition(State.UNINSTALLING,
         State.UNINSTALLED,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_SUCCEEDED,
         new ServiceComponentHostOpCompletedTransition())
     .addTransition(State.UNINSTALLING,
         State.UNINSTALL_FAILED,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_FAILED,
         new ServiceComponentHostOpCompletedTransition())

     .addTransition(State.UNINSTALL_FAILED,
         State.UNINSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_RESTART,
         new ServiceComponentHostOpStartedTransition())
     .addTransition(State.UNINSTALL_FAILED,
         State.UNINSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_UNINSTALL,
         new ServiceComponentHostOpStartedTransition())

     .addTransition(State.UNINSTALLED,
         State.INSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_INSTALL,
         new ServiceComponentHostOpStartedTransition())

     .addTransition(State.UNINSTALLED,
         State.WIPING_OUT,
         ServiceComponentHostEventType.HOST_SVCCOMP_WIPEOUT,
         new ServiceComponentHostOpStartedTransition())

     .addTransition(State.WIPING_OUT,
         State.WIPING_OUT,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_IN_PROGRESS,
         new ServiceComponentHostOpInProgressTransition())
     .addTransition(State.WIPING_OUT,
         State.INIT,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_SUCCEEDED,
         new ServiceComponentHostOpCompletedTransition())
     .addTransition(State.WIPING_OUT,
         State.WIPEOUT_FAILED,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_FAILED,
         new ServiceComponentHostOpCompletedTransition())

     .addTransition(State.WIPEOUT_FAILED,
         State.WIPING_OUT,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_RESTART,
         new ServiceComponentHostOpStartedTransition())
     .addTransition(State.WIPEOUT_FAILED,
         State.WIPING_OUT,
         ServiceComponentHostEventType.HOST_SVCCOMP_WIPEOUT,
         new ServiceComponentHostOpStartedTransition())

     .installTopology();


  private final StateMachine<State,
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
      // FIXME handle restartOp event
      impl.updateLastOpInfo(event.getType(), event.getOpTimestamp());
      if (event.getType() == ServiceComponentHostEventType.HOST_SVCCOMP_START) {
        ServiceComponentHostStartEvent e =
            (ServiceComponentHostStartEvent) event;
        if (LOG.isDebugEnabled()) {
          LOG.debug("Updating live state configs during START event"
              + ", updated configs set size=" + e.getConfigs().size());
        }
        impl.setConfigs(e.getConfigs());
      } else if (event.getType() ==
          ServiceComponentHostEventType.HOST_SVCCOMP_INSTALL) {
        ServiceComponentHostInstallEvent e =
            (ServiceComponentHostInstallEvent) event;
        if (LOG.isDebugEnabled()) {
          LOG.debug("Updating live stack version during INSTALL event"
              + ", new stack verion=" + e.getStackId());
        }
        impl.setStackVersion(new StackId(e.getStackId()));
      }
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

  @AssistedInject
  public ServiceComponentHostImpl(@Assisted ServiceComponent serviceComponent,
                                  @Assisted String hostName, @Assisted boolean isClient, Injector injector) {
    injector.injectMembers(this);

    if (isClient) {
      this.stateMachine = clientStateMachineFactory.make(this);
    } else {
      this.stateMachine = daemonStateMachineFactory.make(this);
    }

    ReadWriteLock rwLock = new ReentrantReadWriteLock();
    this.readLock = rwLock.readLock();
    this.writeLock = rwLock.writeLock();
    this.serviceComponent = serviceComponent;

    stateEntity = new HostComponentStateEntity();
    stateEntity.setClusterId(serviceComponent.getClusterId());
    stateEntity.setComponentName(serviceComponent.getName());
    stateEntity.setServiceName(serviceComponent.getServiceName());
    stateEntity.setHostName(hostName);
    stateEntity.setCurrentState(stateMachine.getCurrentState());
    stateEntity.setCurrentStackVersion(gson.toJson(new StackId()));

    desiredStateEntity = new HostComponentDesiredStateEntity();
    desiredStateEntity.setClusterId(serviceComponent.getClusterId());
    desiredStateEntity.setComponentName(serviceComponent.getName());
    desiredStateEntity.setServiceName(serviceComponent.getServiceName());
    desiredStateEntity.setHostName(hostName);
    desiredStateEntity.setDesiredState(State.INIT);
    desiredStateEntity.setDesiredStackVersion(
        gson.toJson(serviceComponent.getDesiredStackVersion()));

    try {
      this.host = clusters.getHost(hostName);
    } catch (AmbariException e) {
      //TODO exception?
      LOG.error("Host '{}' was not found" + hostName);
      throw new RuntimeException(e);
    }

    this.resetLastOpInfo();
    this.desiredConfigs = new HashMap<String, String>();
    this.configs = new HashMap<String, String>();
  }

  @AssistedInject
  public ServiceComponentHostImpl(@Assisted ServiceComponent serviceComponent,
                                  @Assisted HostComponentStateEntity stateEntity,
                                  @Assisted HostComponentDesiredStateEntity desiredStateEntity,
                                  Injector injector) {
    injector.injectMembers(this);
    ReadWriteLock rwLock = new ReentrantReadWriteLock();
    this.readLock = rwLock.readLock();
    this.writeLock = rwLock.writeLock();
    this.serviceComponent = serviceComponent;


    this.desiredStateEntity = desiredStateEntity;
    this.stateEntity = stateEntity;
    //TODO implement State Machine init as now type choosing is hardcoded in above code
    if (serviceComponent.isClientComponent()) {
      this.stateMachine = clientStateMachineFactory.make(this);
    } else {
      this.stateMachine = daemonStateMachineFactory.make(this);
    }
    this.stateMachine.setCurrentState(stateEntity.getCurrentState());

    try {
      this.host = clusters.getHost(stateEntity.getHostName());
    } catch (AmbariException e) {
      //TODO exception? impossible due to database restrictions
      LOG.error("Host '{}' was not found " + stateEntity.getHostName());
      throw new RuntimeException(e);
    }

    desiredConfigs = new HashMap<String, String>();
    configs = new HashMap<String, String>();

    for (HostComponentDesiredConfigMappingEntity entity : desiredStateEntity.getHostComponentDesiredConfigMappingEntities()) {
      desiredConfigs.put(entity.getConfigType(), entity.getVersionTag());
    }
    // FIXME no configs in live state being persisted in DB??

    persisted = true;
  }

  @Override
  public State getState() {
    try {
      readLock.lock();
      return stateMachine.getCurrentState();
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public void setState(State state) {
    try {
      writeLock.lock();
      stateMachine.setCurrentState(state);
      stateEntity.setCurrentState(state);
      saveIfPersisted();
    }
    finally {
      writeLock.unlock();
    }
  }

  @Override
  @Transactional
  public void handleEvent(ServiceComponentHostEvent event)
      throws InvalidStateTransitionException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Handling ServiceComponentHostEvent event,"
          + " eventType=" + event.getType().name()
          + ", event=" + event.toString());
    }
    State oldState = getState();
    try {
      writeLock.lock();
      try {
        stateMachine.doTransition(event.getType(), event);
        stateEntity.setCurrentState(stateMachine.getCurrentState());
        saveIfPersisted();
        // TODO Audit logs
      } catch (InvalidStateTransitionException e) {
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
    if (!oldState.equals(getState())) {
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
  public String getServiceComponentName() {
    return serviceComponent.getName();
  }

  @Override
  public String getHostName() {
    return host.getHostName();
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
    return serviceComponent.getClusterId();
  }

  @Override
  public String getServiceName() {
    return serviceComponent.getServiceName();
  }

  Map<String, String> getConfigVersions() {
    try {
      readLock.lock();
      if (this.configs != null) {
        return Collections.unmodifiableMap(configs);
      } else {
        return new HashMap<String, String>();
      }
    }
    finally {
      readLock.unlock();
    }

  }

  @Override
  public Map<String, Config> getConfigs() throws AmbariException {
    try {
      readLock.lock();
      Map<String, Config> map = new HashMap<String, Config>();
      Cluster cluster = clusters.getClusterById(getClusterId());
      for (Entry<String, String> entry : configs.entrySet()) {
        Config config = cluster.getDesiredConfig(
            entry.getKey(), entry.getValue());
        if (null != config) {
          map.put(entry.getKey(), config);
        }
      }
      return map;
    }
    finally {
      readLock.unlock();
    }
  }

  @Transactional
  void setConfigs(Map<String, String> configs) {
    try {
      writeLock.lock();

      Set<String> deletedTypes = new HashSet<String>();
      for (String type : this.configs.keySet()) {
        if (!configs.containsKey(type)) {
          deletedTypes.add(type);
        }
      }

      long now = Long.valueOf(new java.util.Date().getTime());

      for (Entry<String,String> entry : configs.entrySet()) {

        boolean contains = false;
        for (HostComponentConfigMappingEntity mappingEntity : stateEntity.getHostComponentConfigMappingEntities()) {
          if (entry.getKey().equals(mappingEntity.getConfigType())) {
            if (LOG.isDebugEnabled()) {
              LOG.debug("Updating live config to ServiceComponentHost"
                  + ", clusterId=" + stateEntity.getClusterId()
                  + ", serviceName=" + stateEntity.getServiceName()
                  + ", componentName=" + stateEntity.getComponentName()
                  + ", hostname=" + stateEntity.getHostName()
                  + ", configType=" + entry.getKey()
                  + ", configVersionTag=" + entry.getValue());
            }
            contains = true;
            mappingEntity.setVersionTag(entry.getValue());
            mappingEntity.setTimestamp(now);
            break;
          }
        }

        if (!contains) {
          HostComponentConfigMappingEntity newEntity =
              new HostComponentConfigMappingEntity();
          newEntity.setClusterId(stateEntity.getClusterId());
          newEntity.setServiceName(stateEntity.getServiceName());
          newEntity.setComponentName(stateEntity.getComponentName());
          newEntity.setHostName(stateEntity.getHostName());
          newEntity.setConfigType(entry.getKey());
          newEntity.setVersionTag(entry.getValue());
          newEntity.setTimestamp(now);

          if (LOG.isDebugEnabled()) {
            LOG.debug("Adding new live config to ServiceComponentHost"
                + ", clusterId=" + stateEntity.getClusterId()
                + ", serviceName=" + stateEntity.getServiceName()
                + ", componentName=" + stateEntity.getComponentName()
                + ", hostname=" + stateEntity.getHostName()
                + ", configType=" + entry.getKey()
                + ", configVersionTag=" + entry.getValue());
          }
          stateEntity.getHostComponentConfigMappingEntities().add(newEntity);
          newEntity.setHostComponentStateEntity(stateEntity);

        }
      }

      if (!deletedTypes.isEmpty()) {
        List<HostComponentConfigMappingEntity> deleteEntities =
            hostComponentConfigMappingDAO.findByHostComponentAndType(
                stateEntity.getClusterId(), stateEntity.getServiceName(),
                stateEntity.getComponentName(),
                stateEntity.getHostName(), deletedTypes);
        for (HostComponentConfigMappingEntity deleteEntity : deleteEntities) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Deleting live config to ServiceComponentHost"
                + ", clusterId="  + stateEntity.getClusterId()
                + ", serviceName=" + stateEntity.getServiceName()
                + ", componentName=" + stateEntity.getComponentName()
                + ", hostname=" + stateEntity.getHostName()
                + ", configType=" + deleteEntity.getConfigType()
                + ", configVersionTag=" + deleteEntity.getVersionTag());
          }
          stateEntity.getHostComponentConfigMappingEntities().remove(
              deleteEntity);
          if (persisted) {
            hostComponentConfigMappingDAO.remove(deleteEntity);
          }
        }
      }
      this.configs = configs;
      saveIfPersisted();
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public StackId getStackVersion() {
    try {
      readLock.lock();
      return gson.fromJson(stateEntity.getCurrentStackVersion(), StackId.class);
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public void setStackVersion(StackId stackVersion) {
    try {
      writeLock.lock();
      stateEntity.setCurrentStackVersion(gson.toJson(stackVersion));
      saveIfPersisted();
    }
    finally {
      writeLock.unlock();
    }
  }


  @Override
  public State getDesiredState() {
    try {
      readLock.lock();
      return desiredStateEntity.getDesiredState();
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public void setDesiredState(State state) {
    try {
      writeLock.lock();
      desiredStateEntity.setDesiredState(state);
      saveIfPersisted();
    }
    finally {
      writeLock.unlock();
    }
  }

  @Override
  public Map<String, String> getDesiredConfigVersionsRecursive() {
    try {
      readLock.lock();
      Map<String, String> fullDesiredConfigVersions =
          new HashMap<String, String>();
      Map<String, Config> desiredConfs = getDesiredConfigs();
      for (Config c : desiredConfs.values()) {
        fullDesiredConfigVersions.put(c.getType(), c.getVersionTag());
      }
      return fullDesiredConfigVersions;
    }
    finally {
      readLock.unlock();
    }
  }


  @Override
  public Map<String, Config> getDesiredConfigs() {
    Map<String, Config> map = new HashMap<String, Config>();
    try {
      readLock.lock();
      for (Entry<String, String> entry : desiredConfigs.entrySet()) {
        Config config = clusters.getClusterById(getClusterId()).getDesiredConfig(
            entry.getKey(), entry.getValue());
        if (null != config) {
          map.put(entry.getKey(), config);
        }
      }
    }
    catch (AmbariException e) {
      // TODO do something
      return null;
    }
    finally {
      readLock.unlock();
    }
    // do a union with component level configs
    Map<String, Config> compConfigs = serviceComponent.getDesiredConfigs();
    for (Entry<String, Config> entry : compConfigs.entrySet()) {
      if (!map.containsKey(entry.getKey())) {
        map.put(entry.getKey(), entry.getValue());
      }
    }
    return Collections.unmodifiableMap(map);
  }

  @Override
  @Transactional
  public void updateDesiredConfigs(Map<String, Config> configs) {
    try {
      writeLock.lock();

      Set<String> deletedTypes = new HashSet<String>();
      for (String type : this.desiredConfigs.keySet()) {
        if (!configs.containsKey(type)) {
          deletedTypes.add(type);
        }
      }

      for (Entry<String,Config> entry : configs.entrySet()) {

        boolean contains = false;
        for (HostComponentDesiredConfigMappingEntity desiredConfigMappingEntity : desiredStateEntity.getHostComponentDesiredConfigMappingEntities()) {
          if (entry.getKey().equals(desiredConfigMappingEntity.getConfigType())) {
            contains = true;
            desiredConfigMappingEntity.setVersionTag(entry.getValue().getVersionTag());
            desiredConfigMappingEntity.setTimestamp(new Date().getTime());
            break;
          }
        }

        if (!contains) {
          HostComponentDesiredConfigMappingEntity newEntity = new HostComponentDesiredConfigMappingEntity();
          newEntity.setClusterId(desiredStateEntity.getClusterId());
          newEntity.setServiceName(desiredStateEntity.getServiceName());
          newEntity.setComponentName(desiredStateEntity.getComponentName());
          newEntity.setHostName(desiredStateEntity.getHostName());
          newEntity.setConfigType(entry.getKey());
          newEntity.setVersionTag(entry.getValue().getVersionTag());
          newEntity.setTimestamp(new Date().getTime());
          newEntity.setHostComponentDesiredStateEntity(desiredStateEntity);
          desiredStateEntity.getHostComponentDesiredConfigMappingEntities().add(newEntity);
        }

        this.desiredConfigs.put(entry.getKey(), entry.getValue().getVersionTag());
      }

      if (!deletedTypes.isEmpty()) {
        if (persisted) {
          List<HostComponentDesiredConfigMappingEntity> deleteEntities =
              hostComponentDesiredConfigMappingDAO.findByHostComponentAndType(
                  stateEntity.getClusterId(), stateEntity.getServiceName(),
                  stateEntity.getComponentName(),
                  stateEntity.getHostName(), deletedTypes);
          for (HostComponentDesiredConfigMappingEntity deleteEntity : deleteEntities) {
            if (LOG.isDebugEnabled()) {
              LOG.debug("Deleting desired config to ServiceComponentHost"
                  + ", clusterId=" + stateEntity.getClusterId()
                  + ", serviceName=" + stateEntity.getServiceName()
                  + ", componentName=" + stateEntity.getComponentName()
                  + ", hostname=" + stateEntity.getHostName()
                  + ", configType=" + deleteEntity.getConfigType()
                  + ", configVersionTag=" + deleteEntity.getVersionTag());
            }
            desiredStateEntity.getHostComponentDesiredConfigMappingEntities().remove(
                deleteEntity);
            hostComponentDesiredConfigMappingDAO.remove(deleteEntity);
          }
        } else {
          for (String deletedType : deletedTypes) {
            desiredConfigs.remove(deletedType);
          }
        }
      }

      saveIfPersisted();
    }
    finally {
      writeLock.unlock();
    }
  }

  @Override
  public StackId getDesiredStackVersion() {
    try {
      readLock.lock();
      return gson.fromJson(desiredStateEntity.getDesiredStackVersion(), StackId.class);
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public void setDesiredStackVersion(StackId stackVersion) {
    try {
      writeLock.lock();
      desiredStateEntity.setDesiredStackVersion(gson.toJson(stackVersion));
      saveIfPersisted();
    }
    finally {
      writeLock.unlock();
    }
  }

  @Override
  public ServiceComponentHostResponse convertToResponse() {
    try {
      readLock.lock();
      ServiceComponentHostResponse r = new ServiceComponentHostResponse(
          serviceComponent.getClusterName(),
          serviceComponent.getServiceName(),
          serviceComponent.getName(),
          getHostName(),
          configs,
          desiredConfigs,
          getState().toString(),
          getStackVersion().getStackId(),
          getDesiredState().toString());
      return r;
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public String getClusterName() {
    return serviceComponent.getClusterName();
  }

  @Override
  public void debugDump(StringBuilder sb) {
    try {
      readLock.lock();
      sb.append("ServiceComponentHost={ hostname=" + getHostName()
          + ", serviceComponentName=" + serviceComponent.getName()
          + ", clusterName=" + serviceComponent.getClusterName()
          + ", serviceName=" + serviceComponent.getServiceName()
          + ", desiredStackVersion=" + getDesiredStackVersion()
          + ", desiredState=" + getDesiredState()
          + ", stackVersion=" + getStackVersion()
          + ", state=" + getState()
          + " }");
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public boolean isPersisted() {
    try {
      readLock.lock();
      return persisted;
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void persist() {
    try {
      writeLock.lock();
      if (!persisted) {
        persistEntities();
        refresh();
        host.refresh();
        serviceComponent.refresh();
        persisted = true;
      } else {
        saveIfPersisted();
      }
    } finally {
      writeLock.unlock();
    }
  }

  @Transactional
  protected void persistEntities() {
    HostEntity hostEntity = hostDAO.findByName(getHostName());
    hostEntity.getHostComponentStateEntities().add(stateEntity);
    hostEntity.getHostComponentDesiredStateEntities().add(desiredStateEntity);

    ServiceComponentDesiredStateEntityPK dpk = new ServiceComponentDesiredStateEntityPK();
    dpk.setClusterId(serviceComponent.getClusterId());
    dpk.setServiceName(serviceComponent.getServiceName());
    dpk.setComponentName(serviceComponent.getName());

    ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity = serviceComponentDesiredStateDAO.findByPK(dpk);
    serviceComponentDesiredStateEntity.getHostComponentDesiredStateEntities().add(desiredStateEntity);

    desiredStateEntity.setServiceComponentDesiredStateEntity(serviceComponentDesiredStateEntity);
    desiredStateEntity.setHostEntity(hostEntity);
    stateEntity.setServiceComponentDesiredStateEntity(serviceComponentDesiredStateEntity);
    stateEntity.setHostEntity(hostEntity);

    hostComponentStateDAO.create(stateEntity);
    hostComponentDesiredStateDAO.create(desiredStateEntity);

    serviceComponentDesiredStateDAO.merge(serviceComponentDesiredStateEntity);
    hostDAO.merge(hostEntity);
  }

  @Override
  @Transactional
  public synchronized void refresh() {
    if (isPersisted()) {
      HostComponentStateEntityPK pk = new HostComponentStateEntityPK();
      HostComponentDesiredStateEntityPK dpk = new HostComponentDesiredStateEntityPK();
      pk.setClusterId(getClusterId());
      pk.setComponentName(getServiceComponentName());
      pk.setServiceName(getServiceName());
      pk.setHostName(getHostName());
      dpk.setClusterId(getClusterId());
      dpk.setComponentName(getServiceComponentName());
      dpk.setServiceName(getServiceName());
      dpk.setHostName(getHostName());
      stateEntity = hostComponentStateDAO.findByPK(pk);
      desiredStateEntity = hostComponentDesiredStateDAO.findByPK(dpk);
      hostComponentStateDAO.refresh(stateEntity);
      hostComponentDesiredStateDAO.refresh(desiredStateEntity);
    }
  }

  @Transactional
  private void saveIfPersisted() {
    if (isPersisted()) {
      hostComponentStateDAO.merge(stateEntity);
      hostComponentDesiredStateDAO.merge(desiredStateEntity);
    }
  }

  @Override
  public synchronized boolean canBeRemoved() {
    try {
      readLock.lock();
      State desiredState = getDesiredState();
      State liveState = getState();
      if ((desiredState == State.INIT || desiredState == State.UNINSTALLED)
          && (liveState == State.INIT || liveState == State.UNINSTALLED)) {
        return true;
      }
    } finally {
      readLock.unlock();
    }
    return false;
  }

  @Override
  public void deleteDesiredConfigs(Set<String> configTypes) {
    try {
      writeLock.lock();
      for (String configType : configTypes) {
        desiredConfigs.remove(configType);
      }
      hostComponentDesiredConfigMappingDAO.removeByType(configTypes);
    } finally {
      writeLock.unlock();
    }
  }

}
