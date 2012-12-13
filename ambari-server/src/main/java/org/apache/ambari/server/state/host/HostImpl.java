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
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.persist.Transactional;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.DiskInfo;
import org.apache.ambari.server.agent.HostInfo;
import org.apache.ambari.server.controller.HostResponse;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.HostStateDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.HostStateEntity;
import org.apache.ambari.server.state.*;
import org.apache.ambari.server.state.HostHealthStatus.HealthStatus;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.apache.ambari.server.state.fsm.SingleArcTransition;
import org.apache.ambari.server.state.fsm.StateMachine;
import org.apache.ambari.server.state.fsm.StateMachineFactory;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class HostImpl implements Host {

  private static final Log LOG = LogFactory.getLog(HostImpl.class);
  private final Gson gson;

  private static final Type diskInfoType =
      new TypeToken<List<DiskInfo>>() {}.getType();
  private static final Type hostAttributesType =
      new TypeToken<Map<String, String>>() {}.getType();

  private final Lock readLock;
  private final Lock writeLock;

  private HostEntity hostEntity;
  private HostStateEntity hostStateEntity;
  private Injector injector;
  private HostDAO hostDAO;
  private HostStateDAO hostStateDAO;
  private ClusterDAO clusterDAO;
  private Clusters clusters;

  private long lastHeartbeatTime = 0L;
  private boolean persisted = false;

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
  private static final String PHYSICALPROCESSORCOUNT =
      "physicalprocessors_count";
  private static final String PROCESSORCOUNT = "processors_count";
  private static final String SELINUXENABLED = "selinux_enabled";
  private static final String SWAPSIZE = "swap_size";
  private static final String SWAPFREE = "swap_free";
  private static final String TIMEZONE = "timezone";

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

  private final StateMachine<HostState, HostEventType, HostEvent> stateMachine;

  @Inject
  public HostImpl(@Assisted HostEntity hostEntity,
      @Assisted boolean persisted, Injector injector) {
    this.stateMachine = stateMachineFactory.make(this);
    ReadWriteLock rwLock = new ReentrantReadWriteLock();
    this.readLock = rwLock.readLock();
    this.writeLock = rwLock.writeLock();

    this.hostEntity = hostEntity;
    this.injector = injector;
    this.persisted = persisted;
    this.hostDAO = injector.getInstance(HostDAO.class);
    this.hostStateDAO = injector.getInstance(HostStateDAO.class);
    this.gson = injector.getInstance(Gson.class);
    this.clusterDAO = injector.getInstance(ClusterDAO.class);
    this.clusters = injector.getInstance(Clusters.class);

    hostStateEntity = hostEntity.getHostStateEntity();
    if (hostStateEntity == null) {
      hostStateEntity = new HostStateEntity();
      hostStateEntity.setHostEntity(hostEntity);
      hostEntity.setHostStateEntity(hostStateEntity);
      setHealthStatus(new HostHealthStatus(HealthStatus.UNKNOWN, ""));
      if (persisted) {
        persist();
      }
    } else {
      this.stateMachine.setCurrentState(hostStateEntity.getCurrentState());
    }

  }

//  //TODO remove
//  public HostImpl(String hostname) {
//    this.stateMachine = stateMachineFactory.make(this);
//    ReadWriteLock rwLock = new ReentrantReadWriteLock();
//    this.readLock = rwLock.readLock();
//    this.writeLock = rwLock.writeLock();
//    setHostName(hostname);
//    setHealthStatus(new HostHealthStatus(HealthStatus.UNKNOWN, ""));
//  }

  static class HostRegistrationReceived
      implements SingleArcTransition<HostImpl, HostEvent> {

    @Override
    public void transition(HostImpl host, HostEvent event) {
      HostRegistrationRequestEvent e = (HostRegistrationRequestEvent) event;
      host.importHostInfo(e.hostInfo);
      host.setLastRegistrationTime(e.registrationTime);
      //Initialize heartbeat time and timeInState with registration time.
      host.setLastHeartbeatTime(e.registrationTime);
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
          heartbeatTime =
            ((HostHealthyHeartbeatEvent)event).getHeartbeatTime();
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
    }
  }

  @Override
  public void importHostInfo(HostInfo hostInfo) {
    try {
      writeLock.lock();
      String fqdn = hostInfo.getFQDN();
      if (fqdn != null
          && !fqdn.isEmpty()
          && !fqdn.equals(getHostName())) {
        if (! isPersisted()) {
          setHostName(hostInfo.getHostName());
        } else {
          LOG.info("Could not modify hostname of the host that is already persisted to DB");
        }
      }

      if (hostInfo.getIPAddress() != null
          && !hostInfo.getIPAddress().isEmpty()) {
        setIPv4(hostInfo.getIPAddress());
        setIPv6(hostInfo.getIPAddress());
      }

      setCpuCount(hostInfo.getPhysicalProcessorCount());
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
      this.setAgentVersion(new AgentVersion(
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
      setHostAttributes(attrs);

      saveIfPersisted();
    }
    finally {
      writeLock.unlock();
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
      hostStateEntity.setCurrentState(state);
      hostStateEntity.setTimeInState(System.currentTimeMillis());
      saveIfPersisted();
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
            + ", host=" + this.getHostName()
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
            + ", host=" + this.getHostName()
            + ", oldState=" + oldState
            + ", currentState=" + getState()
            + ", eventType=" + event.getType().name()
            + ", event=" + event);
      }
    }
  }

  @Override
  public String getHostName() {
    try {
      readLock.lock();
      return hostEntity.getHostName();
    } finally {
      readLock.unlock();
    }
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
  public void setPublicHostName(String hostName) {
    try {
      writeLock.lock();
      hostEntity.setPublicHostName(hostName);
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
      return hostEntity.getPublicHostName();
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public String getIPv4() {
    try {
      readLock.lock();
      return hostEntity.getIpv4();
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void setIPv4(String ip) {
    try {
      writeLock.lock();
      hostEntity.setIpv4(ip);
      saveIfPersisted();
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public String getIPv6() {
    try {
      readLock.lock();
      return hostEntity.getIpv6();
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void setIPv6(String ip) {
    try {
      writeLock.lock();
      hostEntity.setIpv6(ip);
      saveIfPersisted();
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public int getCpuCount() {
    try {
      readLock.lock();
      return hostEntity.getCpuCount();
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void setCpuCount(int cpuCount) {
    try {
      writeLock.lock();
      hostEntity.setCpuCount(cpuCount);
      saveIfPersisted();
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public long getTotalMemBytes() {
    try {
      readLock.lock();
      return hostEntity.getTotalMem();
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void setTotalMemBytes(long totalMemBytes) {
    try {
      writeLock.lock();
      hostEntity.setTotalMem(totalMemBytes);
      saveIfPersisted();
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public long getAvailableMemBytes() {
    try {
      readLock.lock();
      return hostStateEntity.getAvailableMem();
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public void setAvailableMemBytes(long availableMemBytes) {
    try {
      writeLock.lock();
      hostStateEntity.setAvailableMem(availableMemBytes);
      saveIfPersisted();
    }
    finally {
      writeLock.unlock();
    }
  }

  @Override
  public String getOsArch() {
    try {
      readLock.lock();
      return hostEntity.getOsArch();
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void setOsArch(String osArch) {
    try {
      writeLock.lock();
      hostEntity.setOsArch(osArch);
      saveIfPersisted();
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public String getOsInfo() {
    try {
      readLock.lock();
      return hostEntity.getOsInfo();
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void setOsInfo(String osInfo) {
    try {
      writeLock.lock();
      hostEntity.setOsInfo(osInfo);
      saveIfPersisted();
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public String getOsType() {
    try {
      readLock.lock();
      return hostEntity.getOsType();
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void setOsType(String osType) {
    try {
      writeLock.lock();
      hostEntity.setOsType(osType);
      saveIfPersisted();
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public List<DiskInfo> getDisksInfo() {
    try {
      readLock.lock();
      return gson.<List<DiskInfo>>fromJson(
                hostEntity.getDisksInfo(), diskInfoType);
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void setDisksInfo(List<DiskInfo> disksInfo) {
    try {
      writeLock.lock();
      hostEntity.setDisksInfo(gson.toJson(disksInfo, diskInfoType));
      saveIfPersisted();
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public HostHealthStatus getHealthStatus() {
    try {
      readLock.lock();
      return gson.fromJson(hostStateEntity.getHealthStatus(),
          HostHealthStatus.class);
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void setHealthStatus(HostHealthStatus healthStatus) {
    try {
      writeLock.lock();
      hostStateEntity.setHealthStatus(gson.toJson(healthStatus));
      saveIfPersisted();
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public Map<String, String> getHostAttributes() {
    try {
      readLock.lock();
      return gson.<Map<String, String>>fromJson(hostEntity.getHostAttributes(),
          hostAttributesType);
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void setHostAttributes(Map<String, String> hostAttributes) {
    try {
      writeLock.lock();
      Map<String, String> hostAttrs = gson.<Map<String, String>>
          fromJson(hostEntity.getHostAttributes(), hostAttributesType);
      if (hostAttrs == null) {
        hostAttrs = new HashMap<String, String>();
      }
      hostAttrs.putAll(hostAttributes);
      hostEntity.setHostAttributes(gson.toJson(hostAttrs,
          hostAttributesType));
      saveIfPersisted();
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public String getRackInfo() {
    try {
      readLock.lock();
      return hostEntity.getRackInfo();
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void setRackInfo(String rackInfo) {
    try {
      writeLock.lock();
      hostEntity.setRackInfo(rackInfo);
      saveIfPersisted();
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public long getLastRegistrationTime() {
    try {
      readLock.lock();
      return hostEntity.getLastRegistrationTime();
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void setLastRegistrationTime(long lastRegistrationTime) {
    try {
      writeLock.lock();
      this.hostEntity.setLastRegistrationTime(lastRegistrationTime);
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
      return gson.fromJson(hostStateEntity.getAgentVersion(),
          AgentVersion.class);
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public void setAgentVersion(AgentVersion agentVersion) {
    try {
      writeLock.lock();
      hostStateEntity.setAgentVersion(gson.toJson(agentVersion));
      saveIfPersisted();
    }
    finally {
      writeLock.unlock();
    }
  }

  @Override
  public long getTimeInState() {
    return hostStateEntity.getTimeInState();
  }

  @Override
  public void setTimeInState(long timeInState) {
    try {
      writeLock.lock();
      hostStateEntity.setTimeInState(timeInState);
      saveIfPersisted();
    }
    finally {
      writeLock.unlock();
    }
  }

  @Override
  public HostResponse convertToResponse() {
    try {
      readLock.lock();
      HostResponse r = new HostResponse(getHostName());

      r.setAgentVersion(getAgentVersion());
      r.setAvailableMemBytes(getAvailableMemBytes());
      r.setCpuCount(getCpuCount());
      r.setDisksInfo(getDisksInfo());
      r.setHealthStatus(getHealthStatus());
      r.setHostAttributes(getHostAttributes());
      r.setIpv4(getIPv4());
      r.setIpv6(getIPv6());
      r.setLastHeartbeatTime(getLastHeartbeatTime());
      r.setLastRegistrationTime(getLastRegistrationTime());
      r.setOsArch(getOsArch());
      r.setOsInfo(getOsInfo());
      r.setOsType(getOsType());
      r.setRackInfo(getRackInfo());
      r.setTotalMemBytes(getTotalMemBytes());
      r.setPublicHostName(getPublicHostName());
      r.setHostState(getState().toString());

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
    try {
      readLock.lock();
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
    try {
      writeLock.lock();
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
        saveIfPersisted();
      }
    } finally {
      writeLock.unlock();
    }
  }

  @Transactional
  protected void persistEntities() {
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
    try {
      writeLock.lock();
      if (isPersisted()) {
        hostEntity = hostDAO.findByName(hostEntity.getHostName());
        hostStateEntity = hostEntity.getHostStateEntity();
        hostDAO.refresh(hostEntity);
        hostStateDAO.refresh(hostStateEntity);
      }
    } finally {
      writeLock.unlock();
    }
  }

  @Transactional
  private void saveIfPersisted() {
    if (isPersisted()) {
      hostDAO.merge(hostEntity);
      hostStateDAO.merge(hostStateEntity);
    }
  }
}
