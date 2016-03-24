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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.AlertDefinitionCommand;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.ServiceComponentHostResponse;
import org.apache.ambari.server.events.AlertHashInvalidationEvent;
import org.apache.ambari.server.events.MaintenanceModeEvent;
import org.apache.ambari.server.events.ServiceComponentInstalledEvent;
import org.apache.ambari.server.events.ServiceComponentUninstalledEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.dao.HostComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.HostComponentStateDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.ServiceComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntityPK;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntityPK;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostComponentAdminState;
import org.apache.ambari.server.state.HostConfig;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.SecurityState;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceComponentHostEvent;
import org.apache.ambari.server.state.ServiceComponentHostEventType;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.UpgradeState;
import org.apache.ambari.server.state.alert.AlertDefinitionHash;
import org.apache.ambari.server.state.configgroup.ConfigGroup;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.apache.ambari.server.state.fsm.SingleArcTransition;
import org.apache.ambari.server.state.fsm.StateMachine;
import org.apache.ambari.server.state.fsm.StateMachineFactory;
import org.apache.ambari.server.state.stack.upgrade.RepositoryVersionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.persist.Transactional;

public class ServiceComponentHostImpl implements ServiceComponentHost {

  private static final Logger LOG =
      LoggerFactory.getLogger(ServiceComponentHostImpl.class);

  private final ReadWriteLock clusterGlobalLock;
  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
  private final Lock readLock = readWriteLock.readLock();
  private final Lock writeLock = readWriteLock.writeLock();

  private final ServiceComponent serviceComponent;
  private final Host host;
  private volatile boolean persisted = false;

  @Inject
  HostComponentStateDAO hostComponentStateDAO;
  @Inject
  HostComponentDesiredStateDAO hostComponentDesiredStateDAO;
  @Inject
  HostDAO hostDAO;
  @Inject
  RepositoryVersionDAO repositoryVersionDAO;
  @Inject
  ServiceComponentDesiredStateDAO serviceComponentDesiredStateDAO;
  @Inject
  Clusters clusters;
  @Inject
  ConfigHelper helper;
  @Inject
  AmbariMetaInfo ambariMetaInfo;
  @Inject
  RepositoryVersionHelper repositoryVersionHelper;

  /**
   * Used for creating commands to send to the agents when alert definitions are
   * added as the result of a service install.
   */
  @Inject
  private AlertDefinitionHash alertDefinitionHash;

  /**
   * Used to publish events relating to service CRUD operations.
   */
  @Inject
  private AmbariEventPublisher eventPublisher;

  /**
   * Data access object for stack.
   */
  @Inject
  private StackDAO stackDAO;

  // Only used when object state is not persisted
  private HostComponentStateEntity stateEntity;
  private HostComponentDesiredStateEntity desiredStateEntity;

  /**
   * The desired component state entity PK.
   */
  private final HostComponentDesiredStateEntityPK desiredStateEntityPK;

  /**
   * Cache the generated id for host component state for fast lookups.
   */
  private Long hostComponentStateId;

  private long lastOpStartTime;
  private long lastOpEndTime;
  private long lastOpLastUpdateTime;
  private Map<String, HostConfig> actualConfigs = new HashMap<String,
    HostConfig>();
  private List<Map<String, String>> processes = new ArrayList<Map<String, String>>();

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

  .addTransition(State.INSTALLING,
      State.INSTALLED,
      ServiceComponentHostEventType.HOST_SVCCOMP_OP_SUCCEEDED,
      new AlertDefinitionCommandTransition())

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

  // Allow transition on abort
  .addTransition(State.INSTALL_FAILED,
      State.INSTALL_FAILED,
      ServiceComponentHostEventType.HOST_SVCCOMP_OP_FAILED,
      new ServiceComponentHostOpCompletedTransition())

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

  .addTransition(State.INSTALLED,
      State.UPGRADING,
      ServiceComponentHostEventType.HOST_SVCCOMP_UPGRADE,
      new ServiceComponentHostOpStartedTransition())

  .addTransition(State.INSTALLED,
      State.INSTALLED,
      ServiceComponentHostEventType.HOST_SVCCOMP_OP_IN_PROGRESS,
      new ServiceComponentHostOpInProgressTransition())

  .addTransition(State.INSTALLED,
      State.STARTED,
      ServiceComponentHostEventType.HOST_SVCCOMP_STARTED,
      new ServiceComponentHostOpCompletedTransition())

  .addTransition(State.INSTALLED,
      State.INSTALLED,
      ServiceComponentHostEventType.HOST_SVCCOMP_STOPPED,
      new ServiceComponentHostOpCompletedTransition())

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
      ServiceComponentHostEventType.HOST_SVCCOMP_STARTED,
      new ServiceComponentHostOpCompletedTransition())

  .addTransition(State.STARTING,
      State.INSTALLED,
      ServiceComponentHostEventType.HOST_SVCCOMP_OP_FAILED,
      new ServiceComponentHostOpCompletedTransition())

  .addTransition(State.INSTALLED,
      State.STARTING,
      ServiceComponentHostEventType.HOST_SVCCOMP_OP_RESTART,
      new ServiceComponentHostOpStartedTransition())

  .addTransition(State.STARTED,
      State.STARTED,
      ServiceComponentHostEventType.HOST_SVCCOMP_STARTED,
      new ServiceComponentHostOpCompletedTransition())

  .addTransition(State.STARTED,
      State.STOPPING,
      ServiceComponentHostEventType.HOST_SVCCOMP_STOP,
      new ServiceComponentHostOpStartedTransition())

  .addTransition(State.STARTED,
      State.STARTED,
      ServiceComponentHostEventType.HOST_SVCCOMP_OP_IN_PROGRESS,
      new ServiceComponentHostOpInProgressTransition())

  .addTransition(State.STARTED,
      State.INSTALLED,
      ServiceComponentHostEventType.HOST_SVCCOMP_STOPPED,
      new ServiceComponentHostOpCompletedTransition())

  .addTransition(State.STOPPING,
      State.STOPPING,
      ServiceComponentHostEventType.HOST_SVCCOMP_OP_IN_PROGRESS,
      new ServiceComponentHostOpInProgressTransition())

  .addTransition(State.STOPPING,
      State.INSTALLED,
      ServiceComponentHostEventType.HOST_SVCCOMP_STOPPED,
      new ServiceComponentHostOpCompletedTransition())

  .addTransition(State.STOPPING,
      State.STARTED,
      ServiceComponentHostEventType.HOST_SVCCOMP_OP_FAILED,
      new ServiceComponentHostOpCompletedTransition())

  .addTransition(State.STARTED,
      State.STOPPING,
      ServiceComponentHostEventType.HOST_SVCCOMP_OP_RESTART,
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
      State.UNINSTALLING,
      ServiceComponentHostEventType.HOST_SVCCOMP_OP_FAILED,
      new ServiceComponentHostOpCompletedTransition())

  .addTransition(State.UPGRADING,
      State.UPGRADING,
      ServiceComponentHostEventType.HOST_SVCCOMP_OP_IN_PROGRESS,
      new ServiceComponentHostOpInProgressTransition())

  .addTransition(State.UPGRADING,
      State.INSTALLED,
      ServiceComponentHostEventType.HOST_SVCCOMP_OP_SUCCEEDED,
      new ServiceComponentHostOpCompletedTransition())

  .addTransition(State.UPGRADING,
      State.UPGRADING,
      ServiceComponentHostEventType.HOST_SVCCOMP_OP_FAILED,
      new ServiceComponentHostOpCompletedTransition())

  .addTransition(State.UPGRADING,
      State.UPGRADING,
      ServiceComponentHostEventType.HOST_SVCCOMP_UPGRADE,
      new ServiceComponentHostOpStartedTransition())

  .addTransition(State.UPGRADING,
      State.UPGRADING,
      ServiceComponentHostEventType.HOST_SVCCOMP_UPGRADE,
      new ServiceComponentHostOpInProgressTransition())

  .addTransition(State.UNINSTALLING,
      State.UNINSTALLING,
      ServiceComponentHostEventType.HOST_SVCCOMP_OP_RESTART,
      new ServiceComponentHostOpStartedTransition())

  .addTransition(State.UNINSTALLING,
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
      State.WIPING_OUT,
      ServiceComponentHostEventType.HOST_SVCCOMP_OP_FAILED,
      new ServiceComponentHostOpCompletedTransition())

  .addTransition(State.WIPING_OUT,
      State.WIPING_OUT,
      ServiceComponentHostEventType.HOST_SVCCOMP_OP_RESTART,
      new ServiceComponentHostOpStartedTransition())

  .addTransition(State.WIPING_OUT,
      State.WIPING_OUT,
      ServiceComponentHostEventType.HOST_SVCCOMP_WIPEOUT,
      new ServiceComponentHostOpStartedTransition())

  .addTransition(State.INSTALLED,
      State.DISABLED,
      ServiceComponentHostEventType.HOST_SVCCOMP_DISABLE,
      new ServiceComponentHostOpCompletedTransition())

  .addTransition(State.DISABLED,
      State.DISABLED,
      ServiceComponentHostEventType.HOST_SVCCOMP_DISABLE,
      new ServiceComponentHostOpCompletedTransition())

  .addTransition(State.UNKNOWN,
      State.DISABLED,
      ServiceComponentHostEventType.HOST_SVCCOMP_DISABLE,
      new ServiceComponentHostOpCompletedTransition())

  .addTransition(State.UNKNOWN,
      State.UNKNOWN,
      ServiceComponentHostEventType.HOST_SVCCOMP_OP_IN_PROGRESS,
      new ServiceComponentHostOpCompletedTransition())

  .addTransition(State.INSTALL_FAILED,
      State.DISABLED,
      ServiceComponentHostEventType.HOST_SVCCOMP_DISABLE,
      new ServiceComponentHostOpCompletedTransition())

  .addTransition(State.DISABLED,
      State.INSTALLED,
      ServiceComponentHostEventType.HOST_SVCCOMP_RESTORE,
      new ServiceComponentHostOpCompletedTransition())

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

     .addTransition(State.INSTALLING,
         State.INSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_INSTALL,
         new ServiceComponentHostOpStartedTransition())

     .addTransition(State.INSTALLING,
         State.INSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_IN_PROGRESS,
         new ServiceComponentHostOpInProgressTransition())
     .addTransition(State.INSTALLED,
         State.INSTALLED,
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
      // Allow transition on abort
     .addTransition(State.INSTALL_FAILED, State.INSTALL_FAILED,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_FAILED,
         new ServiceComponentHostOpCompletedTransition())

     .addTransition(State.INSTALL_FAILED,
         State.INSTALL_FAILED,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_IN_PROGRESS,
         new ServiceComponentHostOpInProgressTransition())

    .addTransition(State.INSTALLED,
         State.INSTALLED,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_SUCCEEDED,
         new ServiceComponentHostOpCompletedTransition())
     .addTransition(State.INSTALLED,
         State.UNINSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_UNINSTALL,
         new ServiceComponentHostOpStartedTransition())
     .addTransition(State.INSTALLED,
         State.INSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_INSTALL,
         new ServiceComponentHostOpStartedTransition())
    .addTransition(State.INSTALLED,
         State.UPGRADING,
         ServiceComponentHostEventType.HOST_SVCCOMP_UPGRADE,
         new ServiceComponentHostOpStartedTransition())

     .addTransition(State.UPGRADING,
         State.UPGRADING,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_IN_PROGRESS,
         new ServiceComponentHostOpInProgressTransition())
     .addTransition(State.UPGRADING,
         State.INSTALLED,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_SUCCEEDED,
         new ServiceComponentHostOpCompletedTransition())
     .addTransition(State.UPGRADING,
         State.UPGRADING,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_FAILED,
         new ServiceComponentHostOpCompletedTransition())
     .addTransition(State.UPGRADING,
         State.UPGRADING,
         ServiceComponentHostEventType.HOST_SVCCOMP_UPGRADE,
         new ServiceComponentHostOpStartedTransition())
     .addTransition(State.UPGRADING,
         State.UPGRADING,
         ServiceComponentHostEventType.HOST_SVCCOMP_UPGRADE,
         new ServiceComponentHostOpInProgressTransition())

     .addTransition(State.UNINSTALLING,
         State.UNINSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_IN_PROGRESS,
         new ServiceComponentHostOpInProgressTransition())
     .addTransition(State.UNINSTALLING,
         State.UNINSTALLED,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_SUCCEEDED,
         new ServiceComponentHostOpCompletedTransition())
     .addTransition(State.UNINSTALLING,
         State.UNINSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_FAILED,
         new ServiceComponentHostOpCompletedTransition())

     .addTransition(State.UNINSTALLING,
         State.UNINSTALLING,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_RESTART,
         new ServiceComponentHostOpStartedTransition())
     .addTransition(State.UNINSTALLING,
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
         State.WIPING_OUT,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_FAILED,
         new ServiceComponentHostOpCompletedTransition())

     .addTransition(State.WIPING_OUT,
         State.WIPING_OUT,
         ServiceComponentHostEventType.HOST_SVCCOMP_OP_RESTART,
         new ServiceComponentHostOpStartedTransition())
     .addTransition(State.WIPING_OUT,
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

  /**
   * The {@link AlertDefinitionCommandTransition} is used to capture the
   * transition from {@link State#INSTALLING} to {@link State#INSTALLED} so that
   * the host affected will have new {@link AlertDefinitionCommand}s pushed to
   * it.
   */
  static class AlertDefinitionCommandTransition implements
      SingleArcTransition<ServiceComponentHostImpl, ServiceComponentHostEvent> {

    @Override
    public void transition(ServiceComponentHostImpl impl,
        ServiceComponentHostEvent event) {
      if (event.getType() != ServiceComponentHostEventType.HOST_SVCCOMP_OP_SUCCEEDED) {
        return;
      }

      // invalidate the host
      String hostName = impl.getHostName();
      impl.alertDefinitionHash.invalidate(impl.getClusterName(), hostName);

      // publish the event
      AlertHashInvalidationEvent hashInvalidationEvent = new AlertHashInvalidationEvent(
          impl.getClusterId(), Collections.singletonList(hostName));

      impl.eventPublisher.publish(hashInvalidationEvent);
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
      if (event.getType() ==
          ServiceComponentHostEventType.HOST_SVCCOMP_INSTALL) {
        ServiceComponentHostInstallEvent e =
            (ServiceComponentHostInstallEvent) event;
        if (LOG.isDebugEnabled()) {
          LOG.debug("Updating live stack version during INSTALL event"
              + ", new stack version=" + e.getStackId());
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
    } finally {
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
        case HOST_SVCCOMP_STOPPED:
        case HOST_SVCCOMP_STARTED:
          setLastOpLastUpdateTime(time);
          setLastOpEndTime(time);
          break;
        case HOST_SVCCOMP_OP_IN_PROGRESS:
          setLastOpLastUpdateTime(time);
          break;
      }
    } finally {
      writeLock.unlock();
    }
  }

  @AssistedInject
  public ServiceComponentHostImpl(@Assisted ServiceComponent serviceComponent,
                                  @Assisted String hostName, Injector injector) {
    injector.injectMembers(this);

    if (serviceComponent.isClientComponent()) {
      stateMachine = clientStateMachineFactory.make(this);
    } else {
      stateMachine = daemonStateMachineFactory.make(this);
    }

    this.serviceComponent = serviceComponent;
    clusterGlobalLock = serviceComponent.getClusterGlobalLock();

    HostEntity hostEntity = null;
    try {
      host = clusters.getHost(hostName);
      hostEntity = hostDAO.findByName(hostName);
      if (hostEntity == null) {
        throw new AmbariException("Could not find host " + hostName);
      }
    } catch (AmbariException e) {
      LOG.error("Host '{}' was not found" + hostName);
      throw new RuntimeException(e);
    }

    StackId stackId = serviceComponent.getDesiredStackVersion();
    StackEntity stackEntity = stackDAO.find(stackId.getStackName(),
        stackId.getStackVersion());

    stateEntity = new HostComponentStateEntity();
    stateEntity.setClusterId(serviceComponent.getClusterId());
    stateEntity.setComponentName(serviceComponent.getName());
    stateEntity.setServiceName(serviceComponent.getServiceName());
    stateEntity.setVersion(State.UNKNOWN.toString());
    stateEntity.setHostEntity(hostEntity);
    stateEntity.setCurrentState(stateMachine.getCurrentState());
    stateEntity.setUpgradeState(UpgradeState.NONE);
    stateEntity.setCurrentStack(stackEntity);

    desiredStateEntity = new HostComponentDesiredStateEntity();
    desiredStateEntity.setClusterId(serviceComponent.getClusterId());
    desiredStateEntity.setComponentName(serviceComponent.getName());
    desiredStateEntity.setServiceName(serviceComponent.getServiceName());
    desiredStateEntity.setHostEntity(hostEntity);
    desiredStateEntity.setDesiredState(State.INIT);
    desiredStateEntity.setDesiredStack(stackEntity);

    if(!serviceComponent.isMasterComponent() && !serviceComponent.isClientComponent()) {
      desiredStateEntity.setAdminState(HostComponentAdminState.INSERVICE);
    } else {
      desiredStateEntity.setAdminState(null);
    }

    desiredStateEntityPK = getHostComponentDesiredStateEntityPK(desiredStateEntity);

    resetLastOpInfo();
  }

  @AssistedInject
  public ServiceComponentHostImpl(@Assisted ServiceComponent serviceComponent,
                                  @Assisted HostComponentStateEntity stateEntity,
                                  @Assisted HostComponentDesiredStateEntity desiredStateEntity,
                                  Injector injector) {
    injector.injectMembers(this);
    this.serviceComponent = serviceComponent;
    clusterGlobalLock = serviceComponent.getClusterGlobalLock();

    this.desiredStateEntity = desiredStateEntity;
    this.stateEntity = stateEntity;

    desiredStateEntityPK = getHostComponentDesiredStateEntityPK(desiredStateEntity);
    hostComponentStateId = stateEntity.getId();

    //TODO implement State Machine init as now type choosing is hardcoded in above code
    if (serviceComponent.isClientComponent()) {
      stateMachine = clientStateMachineFactory.make(this);
    } else {
      stateMachine = daemonStateMachineFactory.make(this);
    }
    stateMachine.setCurrentState(stateEntity.getCurrentState());

    try {
      host = clusters.getHost(stateEntity.getHostName());
    } catch (AmbariException e) {
      //TODO exception? impossible due to database restrictions
      LOG.error("Host '{}' was not found " + stateEntity.getHostName());
      throw new RuntimeException(e);
    }

    persisted = true;
  }

  @Override
  public State getState() {
    // there's no reason to lock around the state machine for this SCH since
    // the state machine is synchronized
    return stateMachine.getCurrentState();
  }

  @Override
  public void setState(State state) {
    writeLock.lock();
    try {
      stateMachine.setCurrentState(state);
      HostComponentStateEntity stateEntity = getStateEntity();
      if (stateEntity != null) {
        getStateEntity().setCurrentState(state);
        saveComponentStateEntityIfPersisted();
      } else {
        LOG.warn("Setting a member on an entity object that may have been " +
          "previously deleted, serviceName = " + getServiceName() + ", " +
          "componentName = " + getServiceComponentName() + ", " +
          "hostName = " + getHostName());
      }

    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public String getVersion() {
    readLock.lock();
    try {
      HostComponentStateEntity stateEntity = getStateEntity();
      if (stateEntity != null) {
        return stateEntity.getVersion();
      } else {
        LOG.warn("Trying to fetch a member from an entity object that may " +
          "have been previously deleted, serviceName = " + getServiceName() + ", " +
          "componentName = " + getServiceComponentName() + ", " +
          "hostName = " + getHostName());
      }
    } finally {
      readLock.unlock();
    }
    return null;
  }

  @Override
  public void setVersion(String version) {
    writeLock.lock();
    try {
      HostComponentStateEntity stateEntity = getStateEntity();
      if (stateEntity != null) {
        getStateEntity().setVersion(version);
        saveComponentStateEntityIfPersisted();
      } else {
        LOG.warn("Setting a member on an entity object that may have been " +
          "previously deleted, serviceName = " + getServiceName() + ", " +
          "componentName = " + getServiceComponentName() + ", " +
          "hostName = " + getHostName());
      }
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public SecurityState getSecurityState() {
    readLock.lock();
    try {
      HostComponentStateEntity stateEntity = getStateEntity();
      if (stateEntity != null) {
        return getStateEntity().getSecurityState();
      } else {
        LOG.warn("Trying to fetch a member from an entity object that may " +
          "have been previously deleted, serviceName = " + getServiceName() + ", " +
          "componentName = " + getServiceComponentName() + ", " +
          "hostName = " + getHostName());
      }

    } finally {
      readLock.unlock();
    }
    return null;
  }

  @Override
  public void setSecurityState(SecurityState securityState) {
    writeLock.lock();
    try {
      HostComponentStateEntity stateEntity = getStateEntity();
      if (stateEntity != null) {
        getStateEntity().setSecurityState(securityState);
        saveComponentStateEntityIfPersisted();
      } else {
        LOG.warn("Setting a member on an entity object that may have been " +
          "previously deleted, serviceName = " + getServiceName() + ", " +
          "componentName = " + getServiceComponentName() + ", " +
          "hostName = " + getHostName());
      }

    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public SecurityState getDesiredSecurityState() {
    readLock.lock();
    try {
      HostComponentDesiredStateEntity desiredStateEntity = getDesiredStateEntity();
      if (desiredStateEntity != null) {
        return getDesiredStateEntity().getSecurityState();
      } else {
        LOG.warn("Trying to fetch a member from an entity object that may " +
          "have been previously deleted, serviceName = " + getServiceName() + ", " +
          "componentName = " + getServiceComponentName() + ", " +
          "hostName = " + getHostName());
      }

    } finally {
      readLock.unlock();
    }
    return null;
  }

  @Override
  public void setDesiredSecurityState(SecurityState securityState) throws AmbariException {
    if(!securityState.isEndpoint()) {
      throw new AmbariException("The security state must be an endpoint state");
    }

    writeLock.lock();
    try {
      LOG.debug("Set DesiredSecurityState on serviceName = {} componentName = {} hostName = {} to {}",
        getServiceName(), getServiceComponentName(), getHostName(), securityState);

      getDesiredStateEntity().setSecurityState(securityState);
      saveComponentDesiredStateEntityIfPersisted();
    } finally {
      writeLock.unlock();
    }
  }

  /**
   * To be called during the upgrade of a specific Component in a host.
   * The potential upgrade states are NONE (default), PENDING, IN_PROGRESS, FAILED.
   * If the upgrade completes successfully, the upgradeState should be set back to NONE.
   * If the upgrade fails, then the user can retry to set it back into PENDING or IN_PROGRESS.
   * If the upgrade is aborted, then the upgradeState should be set back to NONE.
   *
   * @param upgradeState  the upgrade state
   */
  @Override
  public void setUpgradeState(UpgradeState upgradeState) {
    writeLock.lock();
    try {
      HostComponentStateEntity stateEntity = getStateEntity();
      if (stateEntity != null) {
        stateEntity.setUpgradeState(upgradeState);
        saveComponentStateEntityIfPersisted();
      } else {
        LOG.warn("Setting a member on an entity object that may have been " +
          "previously deleted, serviceName = " + getServiceName() + ", " +
          "componentName = " + getServiceComponentName() + ", " +
          "hostName = " + getHostName());
      }

    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public UpgradeState getUpgradeState() {
    readLock.lock();

    try {
      HostComponentStateEntity stateEntity = getStateEntity();
      if (stateEntity != null) {
        return stateEntity.getUpgradeState();
      } else {
        LOG.warn("Trying to fetch a state entity from an object that may " +
          "have been previously deleted, serviceName = " + getServiceName() + ", " +
          "componentName = " + getServiceComponentName() + ", " +
          "hostName = " + getHostName());
      }

    } finally {
      readLock.unlock();
    }

    return UpgradeState.NONE;
  }


  @Override
  public void handleEvent(ServiceComponentHostEvent event)
      throws InvalidStateTransitionException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Handling ServiceComponentHostEvent event,"
          + " eventType=" + event.getType().name()
          + ", event=" + event.toString());
    }
    State oldState = getState();
    clusterGlobalLock.readLock().lock();
    try {
      try {
        writeLock.lock();
        try {
          stateMachine.doTransition(event.getType(), event);
          getStateEntity().setCurrentState(stateMachine.getCurrentState());
          saveComponentStateEntityIfPersisted();
          // TODO Audit logs
        } catch (InvalidStateTransitionException e) {
          LOG.error("Can't handle ServiceComponentHostEvent event at"
              + " current state"
              + ", serviceComponentName=" + getServiceComponentName()
              + ", hostName=" + getHostName()
            + ", currentState=" + oldState
              + ", eventType=" + event.getType()
              + ", event=" + event);
          throw e;
        }
      } finally {
        writeLock.unlock();
      }
    } finally {
      clusterGlobalLock.readLock().unlock();
    }

    if (!oldState.equals(getState())) {
      LOG.info("Host role transitioned to a new state"
               + ", serviceComponentName=" + getServiceComponentName()
               + ", hostName=" + getHostName()
               + ", oldState=" + oldState
               + ", currentState=" + getState());
      if (LOG.isDebugEnabled()) {
        LOG.debug("ServiceComponentHost transitioned to a new state"
            + ", serviceComponentName=" + getServiceComponentName()
            + ", hostName=" + getHostName()
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

  @Override
  public Host getHost() {
    return host;
  }

  /**
   * @return the lastOpStartTime
   */
  public long getLastOpStartTime() {
    readLock.lock();
    try {
      return lastOpStartTime;
    } finally {
      readLock.unlock();
    }
  }

  /**
   * @param lastOpStartTime the lastOpStartTime to set
   */
  public void setLastOpStartTime(long lastOpStartTime) {
    writeLock.lock();
    try {
      this.lastOpStartTime = lastOpStartTime;
    } finally {
      writeLock.unlock();
    }
  }

  /**
   * @return the lastOpEndTime
   */
  public long getLastOpEndTime() {
    readLock.lock();
    try {
      return lastOpEndTime;
    } finally {
      readLock.unlock();
    }
  }

  /**
   * @param lastOpEndTime the lastOpEndTime to set
   */
  public void setLastOpEndTime(long lastOpEndTime) {
    writeLock.lock();
    try {
      this.lastOpEndTime = lastOpEndTime;
    } finally {
      writeLock.unlock();
    }
  }

  /**
   * @return the lastOpLastUpdateTime
   */
  public long getLastOpLastUpdateTime() {
    readLock.lock();
    try {
      return lastOpLastUpdateTime;
    } finally {
      readLock.unlock();
    }
  }

  /**
   * @param lastOpLastUpdateTime the lastOpLastUpdateTime to set
   */
  public void setLastOpLastUpdateTime(long lastOpLastUpdateTime) {
    writeLock.lock();
    try {
      this.lastOpLastUpdateTime = lastOpLastUpdateTime;
    } finally {
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

  @Override
  public StackId getStackVersion() {
    readLock.lock();
    try {
      HostComponentStateEntity schStateEntity = stateEntity;
      if (schStateEntity == null) {
        return new StackId();
      }

      StackEntity currentStackEntity = schStateEntity.getCurrentStack();
      return new StackId(currentStackEntity.getStackName(),
          currentStackEntity.getStackVersion());
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void setStackVersion(StackId stackId) {
    StackEntity stackEntity = stackDAO.find(stackId.getStackName(), stackId.getStackVersion());

    writeLock.lock();
    try {
      HostComponentStateEntity stateEntity = getStateEntity();
      if (stateEntity != null) {
        stateEntity.setCurrentStack(stackEntity);
        saveComponentStateEntityIfPersisted();
      } else {
        LOG.warn("Setting a member on an entity object that may have been " +
          "previously deleted, serviceName = " + getServiceName() + ", " +
          "componentName = " + getServiceComponentName() + ", " +
          "hostName = " + getHostName());
      }

    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public State getDesiredState() {
    readLock.lock();
    try {
      HostComponentDesiredStateEntity desiredStateEntity = getDesiredStateEntity();
      if (desiredStateEntity != null) {
        return desiredStateEntity.getDesiredState();
      } else {
        LOG.warn("Trying to fetch a member from an entity object that may " +
          "have been previously deleted, serviceName = " + getServiceName() + ", " +
          "componentName = " + getServiceComponentName() + ", " +
          "hostName = " + getHostName());
      }

    } finally {
      readLock.unlock();
    }
    return null;
  }

  @Override
  public void setDesiredState(State state) {
    writeLock.lock();
    try {
      LOG.debug("Set DesiredState on serviceName = {} componentName = {} hostName = {} to {} ",
        getServiceName(), getServiceComponentName(), getHostName(), state);

      HostComponentDesiredStateEntity desiredStateEntity = getDesiredStateEntity();
      if (desiredStateEntity != null) {
        desiredStateEntity.setDesiredState(state);
        saveComponentDesiredStateEntityIfPersisted();
      } else {
        LOG.warn("Setting a member on an entity object that may have been " +
          "previously deleted, serviceName = " + getServiceName() + ", " +
          "componentName = " + getServiceComponentName() +
          "hostName = " + getHostName());
      }
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public StackId getDesiredStackVersion() {
    readLock.lock();
    try {
      HostComponentDesiredStateEntity desiredStateEntity = getDesiredStateEntity();
      if (desiredStateEntity != null) {
        StackEntity desiredStackEntity = desiredStateEntity.getDesiredStack();
        return new StackId(desiredStackEntity.getStackName(),
          desiredStackEntity.getStackVersion());
      } else {
        LOG.warn("Trying to fetch a member from an entity object that may " +
          "have been previously deleted, serviceName = " + getServiceName() + ", " +
          "componentName = " + getServiceComponentName() + ", " +
          "hostName = " + getHostName());
      }

    } finally {
      readLock.unlock();
    }
    return null;
  }

  @Override
  public void setDesiredStackVersion(StackId stackId) {
    writeLock.lock();
    try {
      LOG.debug("Set DesiredStackVersion on serviceName = {} componentName = {} hostName = {} to {}",
        getServiceName(), getServiceComponentName(), getHostName(), stackId);

      HostComponentDesiredStateEntity desiredStateEntity = getDesiredStateEntity();
      if (desiredStateEntity != null) {
        StackEntity stackEntity = stackDAO.find(stackId.getStackName(),
          stackId.getStackVersion());

        desiredStateEntity.setDesiredStack(stackEntity);
        saveComponentDesiredStateEntityIfPersisted();
      }
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public HostComponentAdminState getComponentAdminState() {
    readLock.lock();
    try {
      HostComponentDesiredStateEntity desiredStateEntity = getDesiredStateEntity();
      if (desiredStateEntity != null) {
        HostComponentAdminState adminState = desiredStateEntity.getAdminState();
        if (adminState == null && !serviceComponent.isClientComponent()
          && !serviceComponent.isMasterComponent()) {
          adminState = HostComponentAdminState.INSERVICE;
        }
        return adminState;
      }

    } finally {
      readLock.unlock();
    }
    return null;
  }

  @Override
  public void setComponentAdminState(HostComponentAdminState attribute) {
    writeLock.lock();
    try {
      LOG.debug("Set ComponentAdminState on serviceName = {} componentName = {} hostName = {} to {}",
        getServiceName(), getServiceComponentName(), getHostName(), attribute);

      HostComponentDesiredStateEntity desiredStateEntity = getDesiredStateEntity();
      if (desiredStateEntity != null) {
        desiredStateEntity.setAdminState(attribute);
        saveComponentDesiredStateEntityIfPersisted();
      } else {
        LOG.warn("Setting a member on an entity object that may have been " +
          "previously deleted, serviceName = " + getServiceName() + ", " +
          "componentName = " + getServiceComponentName() +
          "hostName = " + getHostName());
      }
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public ServiceComponentHostResponse convertToResponse() {
    clusterGlobalLock.readLock().lock();
    try {
      readLock.lock();
      try {
        HostComponentStateEntity hostComponentStateEntity = getStateEntity();
        if (null == hostComponentStateEntity) {
          LOG.warn("Could not convert ServiceComponentHostResponse to a response. It's possible that Host " + getHostName() + " was deleted.");
          return null;
        }

        String clusterName = serviceComponent.getClusterName();
        String serviceName = serviceComponent.getServiceName();
        String serviceComponentName = serviceComponent.getName();
        String hostName = getHostName();
        String state = getState().toString();
        String stackId = getStackVersion().getStackId();
        String desiredState = getDesiredState().toString();
        String desiredStackId = getDesiredStackVersion().getStackId();
        HostComponentAdminState componentAdminState = getComponentAdminState();
        UpgradeState upgradeState = hostComponentStateEntity.getUpgradeState();

        String displayName = null;
        try {
          ComponentInfo compInfo = ambariMetaInfo.getComponent(getStackVersion().getStackName(),
            getStackVersion().getStackVersion(), serviceName, serviceComponentName);
          displayName = compInfo.getDisplayName();
        } catch (AmbariException e) {
          displayName = serviceComponentName;
        }

        ServiceComponentHostResponse r = new ServiceComponentHostResponse(
            clusterName, serviceName,
            serviceComponentName, displayName, hostName, state,
            stackId, desiredState,
            desiredStackId, componentAdminState);

        r.setActualConfigs(actualConfigs);
        r.setUpgradeState(upgradeState);

        try {
          r.setStaleConfig(helper.isStaleConfigs(this));
        } catch (Exception e) {
          LOG.error("Could not determine stale config", e);
        }

        return r;
      } finally {
        readLock.unlock();
      }
    } finally {
      clusterGlobalLock.readLock().unlock();
    }
  }

  @Override
  public String getClusterName() {
    return serviceComponent.getClusterName();
  }

  @Override
  public void debugDump(StringBuilder sb) {
    readLock.lock();
    try {
      sb.append("ServiceComponentHost={ hostname=").append(getHostName()).append(
          ", serviceComponentName=").append(serviceComponent.getName()).append(
          ", clusterName=").append(serviceComponent.getClusterName()).append(
          ", serviceName=").append(serviceComponent.getServiceName()).append(
          ", desiredStackVersion=").append(getDesiredStackVersion()).append(
          ", desiredState=").append(getDesiredState()).append(", stackVersion=").append(
          getStackVersion()).append(", state=").append(getState()).append(
          ", securityState=").append(getSecurityState()).append(
          ", desiredSecurityState=").append(getDesiredSecurityState()).append(
        " }");
    } finally {
      readLock.unlock();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isPersisted() {
    // a lock around this internal state variable is not required since we
    // have appropriate locks in the persist() method and this member is
    // only ever false under the condition that the object is new
    return persisted;
  }

  /**
   * {@inheritDoc}
   * <p/>
   * This method uses Java locks and then delegates to internal methods which
   * perform the JPA merges inside of a transaction. Because of this, a
   * transaction is not necessary before this calling this method.
   */
  @Override
  public void persist() {
    boolean clusterWriteLockAcquired = false;
    if (!persisted) {
      clusterGlobalLock.writeLock().lock();
      clusterWriteLockAcquired = true;
    }

    try {
      writeLock.lock();
      try {
        if (!persisted) {
          // persist the new cluster topology and then release the cluster lock
          // as it has no more bearing on the rest of this persist() method
          persistEntities();
          persisted = true;

          clusterGlobalLock.writeLock().unlock();
          clusterWriteLockAcquired = false;

          // these should still be done with the internal lock
          refresh();
          // There refresh calls are no longer needed with cached references
          // not used on getters/setters
          // NOTE: Refreshing parents is a bad pattern.
          //host.refresh();
          //serviceComponent.refresh();

          // publish the service component installed event
          StackId stackId = getDesiredStackVersion();

          ServiceComponentInstalledEvent event = new ServiceComponentInstalledEvent(
              getClusterId(), stackId.getStackName(),
              stackId.getStackVersion(), getServiceName(), getServiceComponentName(), getHostName());

          eventPublisher.publish(event);
        } else {
          saveComponentStateEntityIfPersisted();
          saveComponentDesiredStateEntityIfPersisted();
        }
      } finally {
        writeLock.unlock();
      }
    } finally {
      if (clusterWriteLockAcquired) {
        clusterGlobalLock.writeLock().unlock();
      }
    }
  }

  @Transactional
  protected void persistEntities() {
    HostEntity hostEntity = hostDAO.findByName(getHostName());
    hostEntity.addHostComponentStateEntity(stateEntity);
    hostEntity.addHostComponentDesiredStateEntity(desiredStateEntity);

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

    HostComponentStateEntity stateEntity = hostComponentStateDAO.findByIndex(serviceComponent.getClusterId(),
      serviceComponent.getServiceName(), serviceComponent.getName(), hostEntity.getHostId());

    hostComponentStateId = stateEntity.getId();

    serviceComponentDesiredStateDAO.merge(serviceComponentDesiredStateEntity);
    hostDAO.merge(hostEntity);
  }

  @Override
  public void refresh() {
    writeLock.lock();
    try {
      getDesiredStateEntity();
      getStateEntity();
    } finally {
      writeLock.unlock();
    }
  }

  /**
   * Merges the encapsulated {@link HostComponentStateEntity} inside of a new transaction. This
   * method assumes that the appropriate write lock has already been acquired
   * from {@link #readWriteLock}.
   */
  @Transactional
  void saveComponentStateEntityIfPersisted() {
    if (isPersisted()) {
      hostComponentStateDAO.merge(stateEntity);
    }
  }

  /**
   * Merges the encapsulated {@link HostComponentDesiredStateEntity} inside of a new transaction. This
   * method assumes that the appropriate write lock has already been acquired
   * from {@link #readWriteLock}.
   */
  @Transactional
  void saveComponentDesiredStateEntityIfPersisted() {
    if (isPersisted()) {
      LOG.debug("Save desiredStateEntity serviceName = {} componentName = {} hostName = {} desiredState = {}",
        getServiceName(), getServiceComponentName(), getHostName(), desiredStateEntity.getDesiredState());

      hostComponentDesiredStateDAO.merge(desiredStateEntity);
    }
  }


  @Override
  public boolean canBeRemoved() {
    clusterGlobalLock.readLock().lock();
    boolean schLockAcquired = false;
    try {
      // if unable to read, then writers are writing; cannot remove SCH
      schLockAcquired = readLock.tryLock();

      return schLockAcquired && (getState().isRemovableState());

    } finally {
      if (schLockAcquired) {
        readLock.unlock();
      }
      clusterGlobalLock.readLock().unlock();
    }
  }

  @Override
  public void delete() {
    boolean fireRemovalEvent = false;

    clusterGlobalLock.writeLock().lock();
    try {
      writeLock.lock();
      try {
        if (persisted) {
          removeEntities();

          // host must be re-loaded from db to refresh the cached JPA HostEntity
          // that references HostComponentDesiredStateEntity
          // and HostComponentStateEntity JPA entities
          host.refresh();

          persisted = false;
          fireRemovalEvent = true;
        }

        clusters.getCluster(getClusterName()).removeServiceComponentHost(this);
      } catch (AmbariException ex) {
        LOG.error("Unable to remove a service component from a host", ex);
      } finally {
        writeLock.unlock();
      }
    } finally {
      clusterGlobalLock.writeLock().unlock();
    }

    // publish event for the removal of the SCH after the removal is
    // completed, but only if it was persisted
    if (fireRemovalEvent) {
      long clusterId = getClusterId();
      StackId stackId = getStackVersion();
      String stackVersion = stackId.getStackVersion();
      String stackName = stackId.getStackName();
      String serviceName = getServiceName();
      String componentName = getServiceComponentName();
      String hostName = getHostName();

      ServiceComponentUninstalledEvent event = new ServiceComponentUninstalledEvent(
          clusterId, stackName, stackVersion, serviceName, componentName,
          hostName);

      eventPublisher.publish(event);
    }
  }

  @Transactional
  protected void removeEntities() {
    HostComponentStateEntity stateEntity = getStateEntity();
    if (stateEntity != null) {
      // make sure that the state entities are removed from the associated (detached) host entity
      // Also refresh before delete
      stateEntity.getHostEntity().removeHostComponentStateEntity(stateEntity);
      HostComponentDesiredStateEntity desiredStateEntity = getDesiredStateEntity();
      desiredStateEntity.getHostEntity().removeHostComponentDesiredStateEntity(desiredStateEntity);

      hostComponentDesiredStateDAO.remove(desiredStateEntity);

      hostComponentStateDAO.remove(stateEntity);
    }
  }

  @Override
  public void updateActualConfigs(Map<String, Map<String, String>> configTags) {
    Map<Long, ConfigGroup> configGroupMap;
    String clusterName = getClusterName();
    try {
      Cluster cluster = clusters.getCluster(clusterName);
      configGroupMap = cluster.getConfigGroups();
    } catch (AmbariException e) {
      LOG.warn("Unable to find cluster, " + clusterName);
      return;
    }

    writeLock.lock();
    try {
      LOG.debug("Updating actual config tags: " + configTags);
      actualConfigs = new HashMap<String, HostConfig>();

      for (Entry<String, Map<String, String>> entry : configTags.entrySet()) {
        String type = entry.getKey();
        Map<String, String> values = new HashMap<String, String>(
            entry.getValue());

        String tag = values.get(ConfigHelper.CLUSTER_DEFAULT_TAG);
        values.remove(ConfigHelper.CLUSTER_DEFAULT_TAG);

        HostConfig hc = new HostConfig();
        hc.setDefaultVersionTag(tag);
        actualConfigs.put(type, hc);

        if (!values.isEmpty()) {
          for (Entry<String, String> overrideEntry : values.entrySet()) {
            Long groupId = Long.parseLong(overrideEntry.getKey());
            hc.getConfigGroupOverrides().put(groupId, overrideEntry.getValue());
            if (!configGroupMap.containsKey(groupId)) {
              LOG.debug("Config group does not exist, id = " + groupId);
            }
          }
        }
      }
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public Map<String, HostConfig> getActualConfigs() {
    readLock.lock();
    try {
      return actualConfigs;
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public HostState getHostState() {
    readLock.lock();
    try {
      return host.getState();
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void setMaintenanceState(MaintenanceState state) {
    writeLock.lock();
    try {
      LOG.debug("Set MaintenanceState on serviceName = {} componentName = {} hostName = {} to {}",
        getServiceName(), getServiceComponentName(), getHostName(), state);

      HostComponentDesiredStateEntity desiredStateEntity = getDesiredStateEntity();
      if (desiredStateEntity != null) {
        desiredStateEntity.setMaintenanceState(state);
        saveComponentDesiredStateEntityIfPersisted();

        // broadcast the maintenance mode change
        MaintenanceModeEvent event = new MaintenanceModeEvent(state, this);
        eventPublisher.publish(event);
      } else {
        LOG.warn("Setting a member on an entity object that may have been " +
          "previously deleted, serviceName = " + getServiceName() + ", " +
          "componentName = " + getServiceComponentName() +
          ", hostName = " + getHostName());
      }
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public MaintenanceState getMaintenanceState() {
    readLock.lock();
    try {
      return getDesiredStateEntity().getMaintenanceState();
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void setProcesses(List<Map<String, String>> procs) {
    writeLock.lock();
    try {
      processes = Collections.unmodifiableList(procs);
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public List<Map<String, String>> getProcesses() {
    readLock.lock();
    try {
      return processes;
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public boolean isRestartRequired() {
    readLock.lock();
    try {
      return getDesiredStateEntity().isRestartRequired();
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void setRestartRequired(boolean restartRequired) {
    writeLock.lock();
    try {
      LOG.debug("Set RestartRequired on serviceName = {} componentName = {} hostName = {} to {}",
        getServiceName(), getServiceComponentName(), getHostName(), restartRequired);

      HostComponentDesiredStateEntity desiredStateEntity = getDesiredStateEntity();
      if (desiredStateEntity != null) {
        desiredStateEntity.setRestartRequired(restartRequired);
        saveComponentDesiredStateEntityIfPersisted();
        helper.invalidateStaleConfigsCache(this);
      } else {
        LOG.warn("Setting a member on an entity object that may have been " +
          "previously deleted, serviceName = " + getServiceName() + ", " +
          "componentName = " + getServiceComponentName() +
          ", hostName = " + getHostName());
      }

    } finally {
      writeLock.unlock();
    }
  }

  @Transactional
  RepositoryVersionEntity createRepositoryVersion(String version, final StackId stackId, final StackInfo stackInfo) throws AmbariException {
    // During an Ambari Upgrade from 1.7.0 -> 2.0.0, the Repo Version will not exist, so bootstrap it.
    LOG.info("Creating new repository version " + stackId.getStackName() + "-" + version);

    StackEntity stackEntity = stackDAO.find(stackId.getStackName(),
      stackId.getStackVersion());

    // Ensure that the version provided is part of the Stack.
    // E.g., version 2.3.0.0 is part of HDP 2.3, so is 2.3.0.0-1234
    if (null == version) {
      throw new AmbariException(MessageFormat.format("Cannot create Repository Version for Stack {0}-{1} if the version is empty",
          stackId.getStackName(), stackId.getStackVersion()));
    }

    return repositoryVersionDAO.create(
        stackEntity,
        version,
        stackId.getStackName() + "-" + version,
        repositoryVersionHelper.serializeOperatingSystems(stackInfo.getRepositories()));
  }

  /**
   * Bootstrap any Repo Version, and potentially transition the Host Version across states.
   * If a Host Component has a valid version, then create a Host Version if it does not already exist.
   * If a Host Component does not have a version, return right away because no information is known.
   * @return Return the Repository Version object
   * @throws AmbariException
   */
  @Override
  public RepositoryVersionEntity recalculateHostVersionState() throws AmbariException {
    RepositoryVersionEntity repositoryVersion = null;
    String version = getVersion();
    if (version == null || version.isEmpty() || version.equalsIgnoreCase(State.UNKNOWN.toString())) {
      // Recalculate only if some particular version is set
      return null;
    }

    final String hostName = getHostName();
    final long hostId = getHost().getHostId();
    final Set<Cluster> clustersForHost = clusters.getClustersForHost(hostName);
    if (clustersForHost.size() != 1) {
      throw new AmbariException("Host " + hostName + " should be assigned only to one cluster");
    }
    final Cluster cluster = clustersForHost.iterator().next();
    final StackId stackId = cluster.getDesiredStackVersion();
    final StackInfo stackInfo = ambariMetaInfo.getStack(stackId.getStackName(), stackId.getStackVersion());

    writeLock.lock();
    try {
      // Check if there is a Repo Version already for the version.
      // If it doesn't exist, will have to create it.
      repositoryVersion = repositoryVersionDAO.findByStackNameAndVersion(stackId.getStackName(), version);

      if (null == repositoryVersion) {
        repositoryVersion = createRepositoryVersion(version, stackId, stackInfo);
      }

      final HostEntity host = hostDAO.findById(hostId);
      cluster.transitionHostVersionState(host, repositoryVersion, stackId);
    } finally {
      writeLock.unlock();
    }
    return repositoryVersion;
  }

  // Get the cached desired state entity or load it fresh through the DAO.
  private HostComponentDesiredStateEntity getDesiredStateEntity() {
    if (isPersisted()) {
      desiredStateEntity = hostComponentDesiredStateDAO.findByPK(desiredStateEntityPK);
    }
    return desiredStateEntity;
  }

  /**
   * Gets the state entity for this {@link ServiceComponentHost}.
   *
   * @return the {@link HostComponentStateEntity} for this
   *         {@link ServiceComponentHost}, or {@code null} if there is none.
   */
  private HostComponentStateEntity getStateEntity() {
    if (isPersisted()) {
      stateEntity = hostComponentStateDAO.findById(hostComponentStateId);
    }

    return stateEntity;
  }

  // create a PK object from the given desired component state entity.
  private static HostComponentDesiredStateEntityPK getHostComponentDesiredStateEntityPK(
      HostComponentDesiredStateEntity desiredStateEntity) {

    HostComponentDesiredStateEntityPK dpk = new HostComponentDesiredStateEntityPK();
    dpk.setClusterId(desiredStateEntity.getClusterId());
    dpk.setComponentName(desiredStateEntity.getComponentName());
    dpk.setServiceName(desiredStateEntity.getServiceName());
    dpk.setHostId(desiredStateEntity.getHostId());
    return dpk;
  }
}
