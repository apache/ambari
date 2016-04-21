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
package org.apache.ambari.server.state.host;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.HostNotFoundException;
import org.apache.ambari.server.agent.AgentEnv;
import org.apache.ambari.server.agent.DiskInfo;
import org.apache.ambari.server.agent.HostInfo;
import org.apache.ambari.server.agent.RecoveryReport;
import org.apache.ambari.server.controller.HostResponse;
import org.apache.ambari.server.events.MaintenanceModeEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.cache.HostConfigMapping;
import org.apache.ambari.server.orm.cache.HostConfigMappingImpl;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.HostConfigMappingDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.HostStateDAO;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.HostStateEntity;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.state.AgentVersion;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostConfig;
import org.apache.ambari.server.state.HostEvent;
import org.apache.ambari.server.state.HostEventType;
import org.apache.ambari.server.state.HostHealthStatus;
import org.apache.ambari.server.state.HostHealthStatus.HealthStatus;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.configgroup.ConfigGroup;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.apache.ambari.server.state.fsm.SingleArcTransition;
import org.apache.ambari.server.state.fsm.StateMachine;
import org.apache.ambari.server.state.fsm.StateMachineFactory;
import org.apache.ambari.server.topology.TopologyManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.persist.Transactional;

public class HostImpl implements Host {

  private static final Log LOG = LogFactory.getLog(HostImpl.class);
  private static final String HARDWAREISA = "hardware_isa";
  private static final String HARDWAREMODEL = "hardware_model";
  private static final String INTERFACES = "interfaces";
  private static final String KERNEL = "kernel";
  private static final String KERNELMAJOREVERSON = "kernel_majorversion";
  private static final String KERNELRELEASE = "kernel_release";
  private static final String KERNELVERSION = "kernel_version";
  private static final String MACADDRESS = "mac_address";
  private static final String NETMASK = "netmask";
  private static final String OSFAMILY = "os_family";
  private static final String PHYSICALPROCESSORCOUNT = "physicalprocessors_count";
  private static final String PROCESSORCOUNT = "processors_count";
  private static final String SELINUXENABLED = "selinux_enabled";
  private static final String SWAPSIZE = "swap_size";
  private static final String SWAPFREE = "swap_free";
  private static final String TIMEZONE = "timezone";
  private static final String OS_RELEASE_VERSION = "os_release_version";


  private final Gson gson;

  private static final Type hostAttributesType =
      new TypeToken<Map<String, String>>() {}.getType();
  private static final Type maintMapType =
      new TypeToken<Map<Long, MaintenanceState>>() {}.getType();

  ReadWriteLock rwLock;
  private final Lock readLock;
  private final Lock writeLock;

  // TODO : caching the JPA entities here causes issues if they become stale and get re-merged.
  private HostEntity hostEntity;
  private HostStateEntity hostStateEntity;

  private HostDAO hostDAO;
  private HostStateDAO hostStateDAO;
  private HostVersionDAO hostVersionDAO;
  private ClusterDAO clusterDAO;
  private Clusters clusters;
  private HostConfigMappingDAO hostConfigMappingDAO;

  private long lastHeartbeatTime = 0L;
  private AgentEnv lastAgentEnv = null;
  private List<DiskInfo> disksInfo = new ArrayList<DiskInfo>();
  private RecoveryReport recoveryReport = new RecoveryReport();
  private boolean persisted = false;
  private Integer currentPingPort = null;

  private final StateMachine<HostState, HostEventType, HostEvent> stateMachine;
  private Map<Long, MaintenanceState> maintMap = null;

  // In-memory status, based on host components states
  private String status = HealthStatus.UNKNOWN.name();

  // In-memory prefix of log file paths that is retrieved when the agent registers with the server
  private String prefix;

  /**
   * Used to publish events relating to host CRUD operations.
   */
  @Inject
  private AmbariEventPublisher eventPublisher;

  private static TopologyManager topologyManager;

  private static final StateMachineFactory
    <HostImpl, HostState, HostEventType, HostEvent>
      stateMachineFactory
        = new StateMachineFactory<HostImpl, HostState, HostEventType, HostEvent>
        (HostState.INIT)

   // define the state machine of a Host

   // Transition from INIT state
   // when the initial registration request is received
   .addTransition(HostState.INIT, HostState.WAITING_FOR_HOST_STATUS_UPDATES,
       HostEventType.HOST_REGISTRATION_REQUEST, new HostRegistrationReceived())
   // when a heartbeat is lost right after registration
   .addTransition(HostState.INIT, HostState.HEARTBEAT_LOST,
       HostEventType.HOST_HEARTBEAT_LOST, new HostHeartbeatLostTransition())

   // Transition from WAITING_FOR_STATUS_UPDATES state
   // when the host has responded to its status update requests
   // TODO this will create problems if the host is not healthy
   // TODO Based on discussion with Jitendra, ignoring this for now
   .addTransition(HostState.WAITING_FOR_HOST_STATUS_UPDATES, HostState.HEALTHY,
       HostEventType.HOST_STATUS_UPDATES_RECEIVED,
       new HostStatusUpdatesReceivedTransition())
   // when a normal heartbeat is received
   .addTransition(HostState.WAITING_FOR_HOST_STATUS_UPDATES,
       HostState.WAITING_FOR_HOST_STATUS_UPDATES,
       HostEventType.HOST_HEARTBEAT_HEALTHY)   // TODO: Heartbeat is ignored here
   // when a heartbeart denoting host as unhealthy is received
   .addTransition(HostState.WAITING_FOR_HOST_STATUS_UPDATES,
       HostState.WAITING_FOR_HOST_STATUS_UPDATES, // Still waiting for component status
       HostEventType.HOST_HEARTBEAT_UNHEALTHY,
       new HostBecameUnhealthyTransition()) // TODO: Not sure
  // when a heartbeat is lost and status update is not received
   .addTransition(HostState.WAITING_FOR_HOST_STATUS_UPDATES,
       HostState.HEARTBEAT_LOST,
       HostEventType.HOST_HEARTBEAT_LOST,
       new HostHeartbeatLostTransition())

   // Transitions from HEALTHY state
   // when a normal heartbeat is received
   .addTransition(HostState.HEALTHY, HostState.HEALTHY,
       HostEventType.HOST_HEARTBEAT_HEALTHY,
       new HostHeartbeatReceivedTransition())
   // when a heartbeat is not received within the configured timeout period
   .addTransition(HostState.HEALTHY, HostState.HEARTBEAT_LOST,
       HostEventType.HOST_HEARTBEAT_LOST,
       new HostHeartbeatLostTransition())
   // when a heartbeart denoting host as unhealthy is received
   .addTransition(HostState.HEALTHY, HostState.UNHEALTHY,
       HostEventType.HOST_HEARTBEAT_UNHEALTHY,
       new HostBecameUnhealthyTransition())
   // if a new registration request is received
   .addTransition(HostState.HEALTHY,
       HostState.WAITING_FOR_HOST_STATUS_UPDATES,
       HostEventType.HOST_REGISTRATION_REQUEST, new HostRegistrationReceived())

   // Transitions from UNHEALTHY state
   // when a normal heartbeat is received
   .addTransition(HostState.UNHEALTHY, HostState.HEALTHY,
       HostEventType.HOST_HEARTBEAT_HEALTHY,
       new HostBecameHealthyTransition())
   // when a heartbeart denoting host as unhealthy is received
   .addTransition(HostState.UNHEALTHY, HostState.UNHEALTHY,
       HostEventType.HOST_HEARTBEAT_UNHEALTHY,
       new HostHeartbeatReceivedTransition())
   // when a heartbeat is not received within the configured timeout period
   .addTransition(HostState.UNHEALTHY, HostState.HEARTBEAT_LOST,
       HostEventType.HOST_HEARTBEAT_LOST,
       new HostHeartbeatLostTransition())
   // if a new registration request is received
   .addTransition(HostState.UNHEALTHY,
       HostState.WAITING_FOR_HOST_STATUS_UPDATES,
       HostEventType.HOST_REGISTRATION_REQUEST, new HostRegistrationReceived())

   // Transitions from HEARTBEAT_LOST state
   // when a heartbeat is not received within the configured timeout period
   .addTransition(HostState.HEARTBEAT_LOST, HostState.HEARTBEAT_LOST,
       HostEventType.HOST_HEARTBEAT_LOST)
   // if a new registration request is received
   .addTransition(HostState.HEARTBEAT_LOST,
       HostState.WAITING_FOR_HOST_STATUS_UPDATES,
       HostEventType.HOST_REGISTRATION_REQUEST, new HostRegistrationReceived())

   .installTopology();

  @Inject
  public HostImpl(@Assisted HostEntity hostEntity,
      @Assisted boolean persisted, Injector injector) {
    stateMachine = stateMachineFactory.make(this);
    rwLock = new ReentrantReadWriteLock();
    readLock = rwLock.readLock();
    writeLock = rwLock.writeLock();

    this.hostEntity = hostEntity;
    this.persisted = persisted;
    hostDAO = injector.getInstance(HostDAO.class);
    hostStateDAO = injector.getInstance(HostStateDAO.class);
    hostVersionDAO = injector.getInstance(HostVersionDAO.class);
    gson = injector.getInstance(Gson.class);
    clusterDAO = injector.getInstance(ClusterDAO.class);
    clusters = injector.getInstance(Clusters.class);
    hostConfigMappingDAO = injector.getInstance(HostConfigMappingDAO.class);
    //todo: proper static injection
    HostImpl.topologyManager = injector.getInstance(TopologyManager.class);

    hostStateEntity = hostEntity.getHostStateEntity();
    if (hostStateEntity == null) {
      hostStateEntity = new HostStateEntity();
      hostStateEntity.setHostEntity(hostEntity);
      hostEntity.setHostStateEntity(hostStateEntity);
      hostStateEntity.setHealthStatus(gson.toJson(new HostHealthStatus(HealthStatus.UNKNOWN, "")));
      if (persisted) {
        hostStateDAO.create(hostStateEntity);
      }
    } else {
      stateMachine.setCurrentState(hostStateEntity.getCurrentState());
    }

  }

  static class HostRegistrationReceived
      implements SingleArcTransition<HostImpl, HostEvent> {

    @Override
    public void transition(HostImpl host, HostEvent event) {
      HostRegistrationRequestEvent e = (HostRegistrationRequestEvent) event;
      host.importHostInfo(e.hostInfo);
      host.setLastRegistrationTime(e.registrationTime);
      //Initialize heartbeat time and timeInState with registration time.
      host.setLastHeartbeatTime(e.registrationTime);
      host.setLastAgentEnv(e.agentEnv);
      host.setTimeInState(e.registrationTime);
      host.setAgentVersion(e.agentVersion);
      host.setPublicHostName(e.publicHostName);

      String agentVersion = null;
      if (e.agentVersion != null) {
        agentVersion = e.agentVersion.getVersion();
      }
      LOG.info("Received host registration, host="
        + e.hostInfo.toString()
        + ", registrationTime=" + e.registrationTime
        + ", agentVersion=" + agentVersion);

      host.persist();
      host.clusters.updateHostMappings(host);

      //todo: proper host joined notification
      boolean associatedWithCluster = false;
      try {
        associatedWithCluster = host.clusters.getClustersForHost(host.getPublicHostName()).size() > 0;
      } catch (HostNotFoundException e1) {
        associatedWithCluster = false;
      } catch (AmbariException e1) {
        // only HostNotFoundException is thrown
        e1.printStackTrace();
      }

      topologyManager.onHostRegistered(host, associatedWithCluster);
    }
  }

  static class HostStatusUpdatesReceivedTransition
      implements SingleArcTransition<HostImpl, HostEvent> {

    @Override
    public void transition(HostImpl host, HostEvent event) {
      HostStatusUpdatesReceivedEvent e = (HostStatusUpdatesReceivedEvent)event;
      // TODO Audit logs
      LOG.debug("Host transition to host status updates received state"
          + ", host=" + e.getHostName()
          + ", heartbeatTime=" + e.getTimestamp());
      host.setHealthStatus(new HostHealthStatus(HealthStatus.HEALTHY,
        host.getHealthStatus().getHealthReport()));
    }
  }

  static class HostHeartbeatReceivedTransition
    implements SingleArcTransition<HostImpl, HostEvent> {

    @Override
    public void transition(HostImpl host, HostEvent event) {
      long heartbeatTime = 0;
      switch (event.getType()) {
        case HOST_HEARTBEAT_HEALTHY:
          HostHealthyHeartbeatEvent hhevent = (HostHealthyHeartbeatEvent) event;
          heartbeatTime = hhevent.getHeartbeatTime();
          if (null != hhevent.getAgentEnv()) {
            host.setLastAgentEnv(hhevent.getAgentEnv());
          }
          if (null != hhevent.getMounts() && !hhevent.getMounts().isEmpty()) {
            host.setDisksInfo(hhevent.getMounts());
          }
          break;
        case HOST_HEARTBEAT_UNHEALTHY:
          heartbeatTime =
            ((HostUnhealthyHeartbeatEvent)event).getHeartbeatTime();
          break;
        default:
          break;
      }
      if (0 == heartbeatTime) {
        LOG.error("heartbeatTime = 0 !!!");
        // TODO handle error
      }
      // host.setLastHeartbeatState(new Object());
      host.setLastHeartbeatTime(heartbeatTime);
    }
  }

  static class HostBecameHealthyTransition
      implements SingleArcTransition<HostImpl, HostEvent> {

    @Override
    public void transition(HostImpl host, HostEvent event) {
      HostHealthyHeartbeatEvent e = (HostHealthyHeartbeatEvent) event;
      host.setLastHeartbeatTime(e.getHeartbeatTime());
      // TODO Audit logs
      LOG.debug("Host transitioned to a healthy state"
              + ", host=" + e.getHostName()
              + ", heartbeatTime=" + e.getHeartbeatTime());
      host.setHealthStatus(new HostHealthStatus(HealthStatus.HEALTHY, host.getHealthStatus().getHealthReport()));
    }
  }

  static class HostBecameUnhealthyTransition
      implements SingleArcTransition<HostImpl, HostEvent> {

    @Override
    public void transition(HostImpl host, HostEvent event) {
      HostUnhealthyHeartbeatEvent e = (HostUnhealthyHeartbeatEvent) event;
      host.setLastHeartbeatTime(e.getHeartbeatTime());
      // TODO Audit logs
      LOG.debug("Host transitioned to an unhealthy state"
          + ", host=" + e.getHostName()
          + ", heartbeatTime=" + e.getHeartbeatTime()
          + ", healthStatus=" + e.getHealthStatus());
      host.setHealthStatus(e.getHealthStatus());
    }
  }

  static class HostHeartbeatLostTransition
      implements SingleArcTransition<HostImpl, HostEvent> {

    @Override
    public void transition(HostImpl host, HostEvent event) {
      HostHeartbeatLostEvent e = (HostHeartbeatLostEvent) event;
      // TODO Audit logs
      LOG.debug("Host transitioned to heartbeat lost state"
          + ", host=" + e.getHostName()
          + ", lastHeartbeatTime=" + host.getLastHeartbeatTime());
      host.setHealthStatus(new HostHealthStatus(HealthStatus.UNKNOWN, host.getHealthStatus().getHealthReport()));

      topologyManager.onHostHeartBeatLost(host);
    }
  }

  /**
   * @param hostInfo  the host information
   */
  @Override
  public void importHostInfo(HostInfo hostInfo) {
    try {
      writeLock.lock();

      if (hostInfo.getIPAddress() != null
          && !hostInfo.getIPAddress().isEmpty()) {
        setIPv4(hostInfo.getIPAddress());
        setIPv6(hostInfo.getIPAddress());
      }

      setCpuCount(hostInfo.getProcessorCount());
      setPhCpuCount(hostInfo.getPhysicalProcessorCount());
      setTotalMemBytes(hostInfo.getMemoryTotal());
      setAvailableMemBytes(hostInfo.getFreeMemory());

      if (hostInfo.getArchitecture() != null
          && !hostInfo.getArchitecture().isEmpty()) {
        setOsArch(hostInfo.getArchitecture());
      }

      if (hostInfo.getOS() != null
          && !hostInfo.getOS().isEmpty()) {
        String osType = hostInfo.getOS();
        if (hostInfo.getOSRelease() != null) {
          String[] release = hostInfo.getOSRelease().split("\\.");
          if (release.length > 0) {
            osType += release[0];
          }
        }
        setOsType(osType.toLowerCase());
      }

      if (hostInfo.getMounts() != null
          && !hostInfo.getMounts().isEmpty()) {
        setDisksInfo(hostInfo.getMounts());
      }

      // FIXME add all other information into host attributes
      setAgentVersion(new AgentVersion(
          hostInfo.getAgentUserId()));

      Map<String, String> attrs = new HashMap<String, String>();
      if (hostInfo.getHardwareIsa() != null) {
        attrs.put(HARDWAREISA, hostInfo.getHardwareIsa());
      }
      if (hostInfo.getHardwareModel() != null) {
        attrs.put(HARDWAREMODEL, hostInfo.getHardwareModel());
      }
      if (hostInfo.getInterfaces() != null) {
        attrs.put(INTERFACES, hostInfo.getInterfaces());
      }
      if (hostInfo.getKernel() != null) {
        attrs.put(KERNEL, hostInfo.getKernel());
      }
      if (hostInfo.getKernelMajVersion() != null) {
        attrs.put(KERNELMAJOREVERSON, hostInfo.getKernelMajVersion());
      }
      if (hostInfo.getKernelRelease() != null) {
        attrs.put(KERNELRELEASE, hostInfo.getKernelRelease());
      }
      if (hostInfo.getKernelVersion() != null) {
        attrs.put(KERNELVERSION, hostInfo.getKernelVersion());
      }
      if (hostInfo.getMacAddress() != null) {
        attrs.put(MACADDRESS, hostInfo.getMacAddress());
      }
      if (hostInfo.getNetMask() != null) {
        attrs.put(NETMASK, hostInfo.getNetMask());
      }
      if (hostInfo.getOSFamily() != null) {
        attrs.put(OSFAMILY, hostInfo.getOSFamily());
      }
      if (hostInfo.getPhysicalProcessorCount() != 0) {
        attrs.put(PHYSICALPROCESSORCOUNT,
          Long.toString(hostInfo.getPhysicalProcessorCount()));
      }
      if (hostInfo.getProcessorCount() != 0) {
        attrs.put(PROCESSORCOUNT,
          Long.toString(hostInfo.getProcessorCount()));
      }
      if (Boolean.toString(hostInfo.getSeLinux()) != null) {
        attrs.put(SELINUXENABLED, Boolean.toString(hostInfo.getSeLinux()));
      }
      if (hostInfo.getSwapSize() != null) {
        attrs.put(SWAPSIZE, hostInfo.getSwapSize());
      }
      if (hostInfo.getSwapFree() != null) {
        attrs.put(SWAPFREE, hostInfo.getSwapFree());
      }
      if (hostInfo.getTimeZone() != null) {
        attrs.put(TIMEZONE, hostInfo.getTimeZone());
      }
      if (hostInfo.getOSRelease() != null) {
        attrs.put(OS_RELEASE_VERSION, hostInfo.getOSRelease());
      }

      setHostAttributes(attrs);

      saveIfPersisted();
    }
    finally {
      writeLock.unlock();
    }
  }

  @Override
  public void setLastAgentEnv(AgentEnv env) {
    writeLock.lock();
    try {
      lastAgentEnv = env;
    } finally {
      writeLock.unlock();
    }

  }

  @Override
  public AgentEnv getLastAgentEnv() {
    readLock.lock();
    try {
      return lastAgentEnv;
    } finally {
      readLock.unlock();
    }

  }

  @Override
  public HostState getState() {
    try {
      readLock.lock();
      return stateMachine.getCurrentState();
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public void setState(HostState state) {
    try {
      writeLock.lock();
      stateMachine.setCurrentState(state);

      HostStateEntity hostStateEntity = getHostStateEntity();

      if (hostStateEntity != null) {
        hostStateEntity.setCurrentState(state);
        hostStateEntity.setTimeInState(System.currentTimeMillis());
        saveIfPersisted();
      }
    }
    finally {
      writeLock.unlock();
    }
  }

  @Override
  public void handleEvent(HostEvent event)
      throws InvalidStateTransitionException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Handling Host event, eventType=" + event.getType().name()
          + ", event=" + event.toString());
    }
    HostState oldState = getState();
    try {
      writeLock.lock();
      try {
        stateMachine.doTransition(event.getType(), event);
      } catch (InvalidStateTransitionException e) {
        LOG.error("Can't handle Host event at current state"
            + ", host=" + getHostName()
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
        LOG.debug("Host transitioned to a new state"
            + ", host=" + getHostName()
            + ", oldState=" + oldState
            + ", currentState=" + getState()
            + ", eventType=" + event.getType().name()
            + ", event=" + event);
      }
    }
  }

  @Override
  public String getHostName() {
    // Not an updatable attribute - No locking necessary
    return hostEntity.getHostName();
  }

  @Override
  public Long getHostId() {
    return hostEntity.getHostId();
  }

  @Override
  public void setHostName(String hostName) {
    try {
      writeLock.lock();
      if (!isPersisted()) {
        hostEntity.setHostName(hostName);
      } else {
        throw new UnsupportedOperationException("PK of persisted entity cannot be modified");
      }
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public Integer getCurrentPingPort() {
    try {
      readLock.lock();
      return currentPingPort;
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public void setCurrentPingPort(Integer currentPingPort) {
    try {
      writeLock.lock();
      this.currentPingPort = currentPingPort;
    }
    finally {
      writeLock.unlock();
    }
  }

  @Override
  public void setPublicHostName(String hostName) {
    try {
      writeLock.lock();
      getHostEntity().setPublicHostName(hostName);
      saveIfPersisted();
    }
    finally {
      writeLock.unlock();
    }
  }

  @Override
  public String getPublicHostName() {
    try {
      readLock.lock();
      return getHostEntity().getPublicHostName();
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public String getIPv4() {
    try {
      readLock.lock();
      return getHostEntity().getIpv4();
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void setIPv4(String ip) {
    try {
      writeLock.lock();
      getHostEntity().setIpv4(ip);
      saveIfPersisted();
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public String getIPv6() {
    try {
      readLock.lock();
      return getHostEntity().getIpv6();
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void setIPv6(String ip) {
    try {
      writeLock.lock();
      getHostEntity().setIpv6(ip);
      saveIfPersisted();
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public int getCpuCount() {
    try {
      readLock.lock();
      return getHostEntity().getCpuCount();
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void setCpuCount(int cpuCount) {
    try {
      writeLock.lock();
      getHostEntity().setCpuCount(cpuCount);
      saveIfPersisted();
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public int getPhCpuCount() {
    try {
      readLock.lock();
      return getHostEntity().getPhCpuCount();
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void setPhCpuCount(int phCpuCount) {
    try {
      writeLock.lock();
      getHostEntity().setPhCpuCount(phCpuCount);
      saveIfPersisted();
    } finally {
      writeLock.unlock();
    }
  }


  @Override
  public long getTotalMemBytes() {
    try {
      readLock.lock();
      return getHostEntity().getTotalMem();
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void setTotalMemBytes(long totalMemBytes) {
    try {
      writeLock.lock();
      getHostEntity().setTotalMem(totalMemBytes);
      saveIfPersisted();
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public long getAvailableMemBytes() {
    try {
      readLock.lock();
      HostStateEntity hostStateEntity = getHostStateEntity();
      return hostStateEntity != null ? hostStateEntity.getAvailableMem() : null;
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public void setAvailableMemBytes(long availableMemBytes) {
    try {
      writeLock.lock();
      HostStateEntity hostStateEntity = getHostStateEntity();
      if (hostStateEntity != null) {
        getHostStateEntity().setAvailableMem(availableMemBytes);
        saveIfPersisted();
      }
    }
    finally {
      writeLock.unlock();
    }
  }

  @Override
  public String getOsArch() {
    try {
      readLock.lock();
      return getHostEntity().getOsArch();
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void setOsArch(String osArch) {
    try {
      writeLock.lock();
      getHostEntity().setOsArch(osArch);
      saveIfPersisted();
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public String getOsInfo() {
    try {
      readLock.lock();
      return getHostEntity().getOsInfo();
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void setOsInfo(String osInfo) {
    try {
      writeLock.lock();
      getHostEntity().setOsInfo(osInfo);
      saveIfPersisted();
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public String getOsType() {
    try {
      readLock.lock();
      return getHostEntity().getOsType();
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void setOsType(String osType) {
    try {
      writeLock.lock();
      getHostEntity().setOsType(osType);
      saveIfPersisted();
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public String getOsFamily() {
    Map<String, String> hostAttributes = getHostAttributes();
    String majorVersion = hostAttributes.get(OS_RELEASE_VERSION).split("\\.")[0];
	  return hostAttributes.get(OSFAMILY) + majorVersion;
  }

  @Override
  public List<DiskInfo> getDisksInfo() {
    try {
      readLock.lock();
      return disksInfo;
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void setDisksInfo(List<DiskInfo> disksInfo) {
    try {
      writeLock.lock();
      this.disksInfo = disksInfo;
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public RecoveryReport getRecoveryReport() {
    try {
      readLock.lock();
      return recoveryReport;
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void setRecoveryReport(RecoveryReport recoveryReport) {
    try {
      writeLock.lock();
      this.recoveryReport = recoveryReport;
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public HostHealthStatus getHealthStatus() {
    try {
      readLock.lock();
      HostStateEntity hostStateEntity = getHostStateEntity();
      if (hostStateEntity != null) {
        return gson.fromJson(hostStateEntity.getHealthStatus(), HostHealthStatus.class);
      }
      return null;
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void setHealthStatus(HostHealthStatus healthStatus) {
    try {
      writeLock.lock();
      HostStateEntity hostStateEntity = getHostStateEntity();
      if (hostStateEntity != null) {
        hostStateEntity.setHealthStatus(gson.toJson(healthStatus));

        if (healthStatus.getHealthStatus().equals(HealthStatus.UNKNOWN)) {
          setStatus(HealthStatus.UNKNOWN.name());
        }

        saveIfPersisted();
      }
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public String getPrefix() { return prefix; }

  @Override
  public void setPrefix(String prefix) {
    if (prefix != null && !prefix.equals(this.prefix)) {
      try {
        writeLock.lock();
        this.prefix = prefix;
      } finally {
        writeLock.unlock();
      }
    }
  }

  @Override
  public Map<String, String> getHostAttributes() {
    try {
      readLock.lock();
      return gson.fromJson(getHostEntity().getHostAttributes(),
          hostAttributesType);
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void setHostAttributes(Map<String, String> hostAttributes) {
    try {
      writeLock.lock();
      HostEntity hostEntity = getHostEntity();
      Map<String, String> hostAttrs = gson.fromJson(hostEntity.getHostAttributes(), hostAttributesType);
      if (hostAttrs == null) {
        hostAttrs = new HashMap<String, String>();
      }
      hostAttrs.putAll(hostAttributes);
      hostEntity.setHostAttributes(gson.toJson(hostAttrs,hostAttributesType));
      saveIfPersisted();
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public String getRackInfo() {
    try {
      readLock.lock();
      return getHostEntity().getRackInfo();
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void setRackInfo(String rackInfo) {
    try {
      writeLock.lock();
      getHostEntity().setRackInfo(rackInfo);
      saveIfPersisted();
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public long getLastRegistrationTime() {
    try {
      readLock.lock();
      return getHostEntity().getLastRegistrationTime();
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void setLastRegistrationTime(long lastRegistrationTime) {
    try {
      writeLock.lock();
      getHostEntity().setLastRegistrationTime(lastRegistrationTime);
      saveIfPersisted();
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public long getLastHeartbeatTime() {
    try {
      readLock.lock();
      return lastHeartbeatTime;
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public void setLastHeartbeatTime(long lastHeartbeatTime) {
    try {
      writeLock.lock();
      this.lastHeartbeatTime = lastHeartbeatTime;
    }
    finally {
      writeLock.unlock();
    }
  }

  @Override
  public AgentVersion getAgentVersion() {
    try {
      readLock.lock();
      HostStateEntity hostStateEntity = getHostStateEntity();
      if (hostStateEntity != null) {
        return gson.fromJson(getHostStateEntity().getAgentVersion(),
            AgentVersion.class);
      }
      return null;
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public void setAgentVersion(AgentVersion agentVersion) {
    try {
      writeLock.lock();
      HostStateEntity hostStateEntity = getHostStateEntity();
      if (hostStateEntity != null) {
        getHostStateEntity().setAgentVersion(gson.toJson(agentVersion));
        saveIfPersisted();
      }
    }
    finally {
      writeLock.unlock();
    }
  }

  @Override
  public long getTimeInState() {
    HostStateEntity hostStateEntity = getHostStateEntity();
    return hostStateEntity != null ? hostStateEntity.getTimeInState() :  null;
  }

  @Override
  public void setTimeInState(long timeInState) {
    try {
      writeLock.lock();
      HostStateEntity hostStateEntity = getHostStateEntity();
      if (hostStateEntity != null) {
        getHostStateEntity().setTimeInState(timeInState);
        saveIfPersisted();
      }
    }
    finally {
      writeLock.unlock();
    }
  }


  @Override
  public String getStatus() {
    return status;
  }

  @Override
  public void setStatus(String status) {
    if (status != null && !status.equals(this.status)) {
      try {
        writeLock.lock();
        this.status = status;
      } finally {
        writeLock.unlock();
      }
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Host that = (Host) o;

    return getHostName().equals(that.getHostName());
  }

  @Override
  public int hashCode() {
    return (null == getHostName() ? 0 : getHostName().hashCode());
  }

  public int compareTo(HostEntity other) {
    return getHostName().compareTo(other.getHostName());
  }

  @Override
  public HostResponse convertToResponse() {
    try {
      readLock.lock();
      HostResponse r = new HostResponse(getHostName());

      r.setAgentVersion(getAgentVersion());
      r.setAvailableMemBytes(getAvailableMemBytes());
      r.setPhCpuCount(getPhCpuCount());
      r.setCpuCount(getCpuCount());
      r.setDisksInfo(getDisksInfo());
      r.setHealthStatus(getHealthStatus());
      r.setHostAttributes(getHostAttributes());
      r.setIpv4(getIPv4());
      r.setIpv6(getIPv6());
      r.setLastHeartbeatTime(getLastHeartbeatTime());
      r.setLastAgentEnv(lastAgentEnv);
      r.setLastRegistrationTime(getLastRegistrationTime());
      r.setOsArch(getOsArch());
      r.setOsInfo(getOsInfo());
      r.setOsType(getOsType());
      r.setRackInfo(getRackInfo());
      r.setTotalMemBytes(getTotalMemBytes());
      r.setPublicHostName(getPublicHostName());
      r.setHostState(getState().toString());
      r.setStatus(getStatus());
      r.setRecoveryReport(getRecoveryReport());
      r.setRecoverySummary(getRecoveryReport().getSummary());

      return r;
    }
    finally {
      readLock.unlock();
    }
  }

  /**
   * Shows if Host is persisted to database
   *
   * @return true if persisted
   */
  @Override
  public boolean isPersisted() {
    readLock.lock();
    try {
      return persisted;
    } finally {
      readLock.unlock();
    }
  }

  /**
   * Save host to database and make all changes to be saved afterwards
   */
  @Override
  public void persist() {
    writeLock.lock();
    try {
      if (!persisted) {
        persistEntities();
        refresh();
        for (ClusterEntity clusterEntity : hostEntity.getClusterEntities()) {
          try {
            clusters.getClusterById(clusterEntity.getClusterId()).refresh();
          } catch (AmbariException e) {
            LOG.error(e);
            throw new RuntimeException("Cluster '" + clusterEntity.getClusterId() + "' was removed", e);
          }
        }
        persisted = true;
      } else {
        //refresh entities from active session
        getHostEntity();
        getHostStateEntity();
        saveIfPersisted();
      }
    } finally {
      writeLock.unlock();
    }
  }

  @Transactional
  void persistEntities() {
    hostDAO.create(hostEntity);
    hostStateDAO.create(hostStateEntity);
    if (!hostEntity.getClusterEntities().isEmpty()) {
      for (ClusterEntity clusterEntity : hostEntity.getClusterEntities()) {
        clusterEntity.getHostEntities().add(hostEntity);
        clusterDAO.merge(clusterEntity);
      }
    }
  }

  @Override
  @Transactional
  public void refresh() {
    writeLock.lock();
    try {
      getHostEntity();
    } finally {
      writeLock.unlock();
    }
  }

  @Transactional
  void saveIfPersisted() {
    if (isPersisted()) {
      hostDAO.merge(hostEntity);
      hostStateDAO.merge(hostStateEntity);
    }
  }

  @Override
  @Transactional
  public boolean addDesiredConfig(long clusterId, boolean selected, String user, Config config) {
    if (null == user) {
      throw new NullPointerException("User must be specified.");
    }

    HostConfigMapping exist = getDesiredConfigEntity(clusterId, config.getType());
    if (null != exist && exist.getVersion().equals(config.getTag())) {
      if (!selected) {
        exist.setSelected(0);
        hostConfigMappingDAO.merge(exist);
      }
      return false;
    }

    writeLock.lock();

    HostEntity hostEntity = getHostEntity();

    try {
      // set all old mappings for this type to empty
      for (HostConfigMapping e : hostConfigMappingDAO.findByType(clusterId,
          hostEntity.getHostId(), config.getType())) {
        e.setSelected(0);
        hostConfigMappingDAO.merge(e);
      }

      HostConfigMapping hostConfigMapping = new HostConfigMappingImpl();
      hostConfigMapping.setClusterId(clusterId);
      hostConfigMapping.setCreateTimestamp(System.currentTimeMillis());
      hostConfigMapping.setHostId(hostEntity.getHostId());
      hostConfigMapping.setSelected(1);
      hostConfigMapping.setUser(user);
      hostConfigMapping.setType(config.getType());
      hostConfigMapping.setVersion(config.getTag());

      hostConfigMappingDAO.create(hostConfigMapping);
    }
    finally {
      writeLock.unlock();
    }

    hostDAO.merge(hostEntity);

    return true;
  }

  @Override
  public Map<String, DesiredConfig> getDesiredConfigs(long clusterId) {
    Map<String, DesiredConfig> map = new HashMap<String, DesiredConfig>();

    for (HostConfigMapping e : hostConfigMappingDAO.findSelected(
        clusterId, hostEntity.getHostId())) {

      DesiredConfig dc = new DesiredConfig();
      dc.setTag(e.getVersion());
      dc.setServiceName(e.getServiceName());
      dc.setUser(e.getUser());
      map.put(e.getType(), dc);

    }
    return map;
  }

  /**
   * Get a map of configType with all applicable config tags.
   *
   * @param cluster  the cluster
   *
   * @return Map of configType -> HostConfig
   */
  @Override
  public Map<String, HostConfig> getDesiredHostConfigs(Cluster cluster) throws AmbariException {
    return getDesiredHostConfigs(cluster, false);
  }

  /**
   * Get a map of configType with all applicable config tags.
   *
   * @param cluster  the cluster
   * @param bypassCache don't use cached values
   *
   * @return Map of configType -> HostConfig
   */
  @Override
  public Map<String, HostConfig> getDesiredHostConfigs(Cluster cluster, boolean bypassCache) throws AmbariException {
    Map<String, HostConfig> hostConfigMap = new HashMap<String, HostConfig>();
    Map<String, DesiredConfig> clusterDesiredConfigs = (cluster == null) ? new HashMap<String, DesiredConfig>() : cluster.getDesiredConfigs(bypassCache);

    if (clusterDesiredConfigs != null) {
      for (Map.Entry<String, DesiredConfig> desiredConfigEntry
          : clusterDesiredConfigs.entrySet()) {
        HostConfig hostConfig = new HostConfig();
        hostConfig.setDefaultVersionTag(desiredConfigEntry.getValue().getTag());
        hostConfigMap.put(desiredConfigEntry.getKey(), hostConfig);
      }
    }

    Map<Long, ConfigGroup> configGroups = (cluster == null) ? new HashMap<Long, ConfigGroup>() : cluster.getConfigGroupsByHostname(getHostName());

    if (configGroups != null && !configGroups.isEmpty()) {
      for (ConfigGroup configGroup : configGroups.values()) {
        for (Map.Entry<String, Config> configEntry : configGroup
            .getConfigurations().entrySet()) {

          String configType = configEntry.getKey();
          // HostConfig config holds configType -> versionTag, per config group
          HostConfig hostConfig = hostConfigMap.get(configType);
          if (hostConfig == null) {
            hostConfig = new HostConfig();
            hostConfigMap.put(configType, hostConfig);
            if (cluster != null) {
              Config conf = cluster.getDesiredConfigByType(configType);
              if(conf == null) {
                LOG.error("Config inconsistency exists:"+
                    " unknown configType="+configType);
              } else {
                hostConfig.setDefaultVersionTag(conf.getTag());
              }
            }
          }
          Config config = configEntry.getValue();
          hostConfig.getConfigGroupOverrides().put(configGroup.getId(),
              config.getTag());
        }
      }
    }
    return hostConfigMap;
  }

  private HostConfigMapping getDesiredConfigEntity(long clusterId, String type) {
    return hostConfigMappingDAO.findSelectedByType(clusterId, hostEntity.getHostId(), type);
  }

  private void ensureMaintMap() {
    if (null == maintMap) {
      HostStateEntity hostStateEntity = getHostStateEntity();
      if (hostStateEntity != null) {
        String entity = hostStateEntity.getMaintenanceState();
        if (null == entity) {
          maintMap = new HashMap<Long, MaintenanceState>();
        } else {
          try {
            maintMap = gson.fromJson(entity, maintMapType);
          } catch (Exception e) {
            maintMap = new HashMap<Long, MaintenanceState>();
          }
        }
      }
    }
  }

  @Override
  public void setMaintenanceState(long clusterId, MaintenanceState state) {
    try {
      writeLock.lock();

      ensureMaintMap();

      maintMap.put(clusterId, state);
      String json = gson.toJson(maintMap, maintMapType);

      HostStateEntity hostStateEntity = getHostStateEntity();
      if (hostStateEntity != null) {
        getHostStateEntity().setMaintenanceState(json);
        saveIfPersisted();

        // broadcast the maintenance mode change
        MaintenanceModeEvent event = new MaintenanceModeEvent(state, this);
        eventPublisher.publish(event);
      }
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public MaintenanceState getMaintenanceState(long clusterId) {
    try {
      readLock.lock();

      ensureMaintMap();

      if (!maintMap.containsKey(clusterId)) {
        maintMap.put(clusterId, MaintenanceState.OFF);
      }

      return maintMap.get(clusterId);
    } finally {
      readLock.unlock();
    }
  }

  /**
   * Get all of the HostVersionEntity objects for the host.
   *
   * @return all of the HostVersionEntity objects for the host
   */
  @Override
  public List<HostVersionEntity> getAllHostVersions() {
    return hostVersionDAO.findByHost(getHostName());
  }

  // Get the cached host entity or load it fresh through the DAO.
  public HostEntity getHostEntity() {
    if (isPersisted()) {
      hostEntity = hostDAO.findById(hostEntity.getHostId());
    }
    return hostEntity;
  }

  // Get the cached host state entity or load it fresh through the DAO.
  public HostStateEntity getHostStateEntity() {
    if (isPersisted()) {
      hostStateEntity = hostStateDAO.findByHostId(hostEntity.getHostId()) ;
    }
    return hostStateEntity;
  }
}


