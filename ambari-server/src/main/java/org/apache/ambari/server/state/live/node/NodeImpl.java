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


package org.apache.ambari.server.state.live.node;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.ambari.server.state.fsm.InvalidStateTransitonException;
import org.apache.ambari.server.state.fsm.SingleArcTransition;
import org.apache.ambari.server.state.fsm.StateMachine;
import org.apache.ambari.server.state.fsm.StateMachineFactory;
import org.apache.ambari.server.state.live.AgentVersion;
import org.apache.ambari.server.state.live.DiskInfo;
import org.apache.ambari.server.state.live.Job;
import org.apache.ambari.server.state.live.node.NodeHealthStatus.HealthStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class NodeImpl implements Node {

  private static final Log LOG = LogFactory.getLog(NodeImpl.class);

  private final Lock readLock;
  private final Lock writeLock;
  
  /**
   * Node hostname
   */
  private String hostName;

  /**
   * Node IP if ipv4 interface available
   */
  private String ipv4;

  /**
   * Node IP if ipv6 interface available
   */
  private String ipv6;
  
  /**
   * Count of cores on Node
   */
  private int cpuCount;
  
  /**
   * Os Architecture
   */
  private String osArch;
  
  /**
   * OS Type
   */
  private String osType;

  /**
   * OS Information
   */
  private String osInfo;

  /**
   * Amount of available memory for the Node
   */
  private long availableMemBytes;
  
  /**
   * Amount of physical memory for the Node
   */
  private long totalMemBytes;

  /**
   * Disks mounted on the Node
   */
  private List<DiskInfo> disksInfo;

  /**
   * Last heartbeat timestamp from the Node
   */
  private long lastHeartbeatTime;

  /**
   * Last registration timestamp for the Node
   */
  private long lastRegistrationTime;

  /**
   * Rack to which the Node belongs to
   */
  private String rackInfo;
  
  /**
   * Additional Node attributes
   */
  private Map<String, String> hostAttributes;

  /**
   * Version of agent running on the Node
   */
  private AgentVersion agentVersion;
  
  /**
   * Node Health Status
   */
  private NodeHealthStatus healthStatus;
  
  private static final StateMachineFactory
    <NodeImpl, NodeState, NodeEventType, NodeEvent>
      stateMachineFactory
        = new StateMachineFactory<NodeImpl, NodeState, NodeEventType, NodeEvent>
        (NodeState.INIT)

   // define the state machine of a Node

   // Transition from INIT state
   // when the initial registration request is received        
   .addTransition(NodeState.INIT, NodeState.WAITING_FOR_VERIFICATION,
       NodeEventType.NODE_REGISTRATION_REQUEST, new NodeRegistrationReceived())

   // Transition from WAITING_FOR_VERIFICATION state
   // when the node is authenticated    
   .addTransition(NodeState.WAITING_FOR_VERIFICATION, NodeState.VERIFIED,
       NodeEventType.NODE_VERIFIED, new NodeVerifiedTransition())

   // Transitions from VERIFIED state
   // when a normal heartbeat is received    
   .addTransition(NodeState.VERIFIED, NodeState.HEALTHY,
       NodeEventType.NODE_HEARTBEAT_HEALTHY,
       new NodeBecameHealthyTransition())
   // when a heartbeat is not received within the configured timeout period
   .addTransition(NodeState.VERIFIED, NodeState.HEARTBEAT_LOST,
       NodeEventType.NODE_HEARTBEAT_TIMED_OUT,
       new NodeHeartbeatTimedOutTransition())
   // when a heartbeart denoting node as unhealthy is received    
   .addTransition(NodeState.VERIFIED, NodeState.UNHEALTHY,
       NodeEventType.NODE_HEARTBEAT_UNHEALTHY,
       new NodeBecameUnhealthyTransition())       
       
   // Transitions from HEALTHY state
   // when a normal heartbeat is received    
   .addTransition(NodeState.HEALTHY, NodeState.HEALTHY,
       NodeEventType.NODE_HEARTBEAT_HEALTHY,
       new NodeHeartbeatReceivedTransition())
   // when a heartbeat is not received within the configured timeout period
   .addTransition(NodeState.HEALTHY, NodeState.HEARTBEAT_LOST,
       NodeEventType.NODE_HEARTBEAT_TIMED_OUT,
       new NodeHeartbeatTimedOutTransition())
   // when a heartbeart denoting node as unhealthy is received    
   .addTransition(NodeState.HEALTHY, NodeState.UNHEALTHY,
       NodeEventType.NODE_HEARTBEAT_UNHEALTHY,
       new NodeBecameUnhealthyTransition())

   // Transitions from UNHEALTHY state
   // when a normal heartbeat is received    
   .addTransition(NodeState.UNHEALTHY, NodeState.HEALTHY,
       NodeEventType.NODE_HEARTBEAT_HEALTHY,
       new NodeBecameHealthyTransition())
   // when a heartbeart denoting node as unhealthy is received    
   .addTransition(NodeState.UNHEALTHY, NodeState.UNHEALTHY,
       NodeEventType.NODE_HEARTBEAT_UNHEALTHY,
       new NodeHeartbeatReceivedTransition())
   // when a heartbeat is not received within the configured timeout period
   .addTransition(NodeState.UNHEALTHY, NodeState.HEARTBEAT_LOST,
       NodeEventType.NODE_HEARTBEAT_TIMED_OUT,
       new NodeHeartbeatTimedOutTransition())

   // Transitions from HEARTBEAT_LOST state
   // when a normal heartbeat is received    
   .addTransition(NodeState.HEARTBEAT_LOST, NodeState.HEALTHY,
       NodeEventType.NODE_HEARTBEAT_HEALTHY,
       new NodeBecameHealthyTransition())
   // when a heartbeart denoting node as unhealthy is received    
   .addTransition(NodeState.HEARTBEAT_LOST, NodeState.UNHEALTHY,
       NodeEventType.NODE_HEARTBEAT_UNHEALTHY,
       new NodeBecameUnhealthyTransition())
   // when a heartbeat is not received within the configured timeout period
   .addTransition(NodeState.HEARTBEAT_LOST, NodeState.HEARTBEAT_LOST,
       NodeEventType.NODE_HEARTBEAT_TIMED_OUT)
   .installTopology();

  private final StateMachine<NodeState, NodeEventType, NodeEvent> stateMachine;

  public NodeImpl() {
    super();
    this.stateMachine = stateMachineFactory.make(this);
    ReadWriteLock rwLock = new ReentrantReadWriteLock();
    this.readLock = rwLock.readLock();
    this.writeLock = rwLock.writeLock();
    this.healthStatus = new NodeHealthStatus(HealthStatus.UNKNOWN, "");
  }

  static class NodeRegistrationReceived
      implements SingleArcTransition<NodeImpl, NodeEvent> {

    @Override
    public void transition(NodeImpl node, NodeEvent event) {
      NodeRegistrationRequestEvent e = (NodeRegistrationRequestEvent) event;
      node.importNodeInfo(e.nodeInfo);
      node.setLastRegistrationTime(e.registrationTime);
      node.setAgentVersion(e.agentVersion);
    }
  }
  
  static class NodeVerifiedTransition
      implements SingleArcTransition<NodeImpl, NodeEvent> {

    @Override
    public void transition(NodeImpl node, NodeEvent event) {
      // TODO Auto-generated method stub
    }
  }
  
  static class NodeHeartbeatReceivedTransition
    implements SingleArcTransition<NodeImpl, NodeEvent> {

    @Override
    public void transition(NodeImpl node, NodeEvent event) {
      long heartbeatTime = 0;
      switch (event.getType()) {
        case NODE_HEARTBEAT_HEALTHY:
          heartbeatTime = ((NodeHealthyHeartbeatEvent)event).getHeartbeatTime();
          break;
        case NODE_HEARTBEAT_UNHEALTHY:
          heartbeatTime = ((NodeUnhealthyHeartbeatEvent)event).getHeartbeatTime();
          break;
        default:
          break;
      }
      if (0 == heartbeatTime) {
        // TODO handle error        
      }
      node.setLastHeartbeatTime(heartbeatTime);
    }
  }  
  
  static class NodeBecameHealthyTransition
      implements SingleArcTransition<NodeImpl, NodeEvent> {

    @Override
    public void transition(NodeImpl node, NodeEvent event) {
      NodeHealthyHeartbeatEvent e = (NodeHealthyHeartbeatEvent) event;
      node.setLastHeartbeatTime(e.getHeartbeatTime());
      // TODO Audit logs
      LOG.info("Node transitioned to a healthy state"
          + ", node=" + e.nodeName
          + ", heartbeatTime=" + e.getHeartbeatTime());
      node.getHealthStatus().setHealthStatus(HealthStatus.HEALTHY);      
    }
  }

  static class NodeBecameUnhealthyTransition
      implements SingleArcTransition<NodeImpl, NodeEvent> {

    @Override
    public void transition(NodeImpl node, NodeEvent event) {
      NodeUnhealthyHeartbeatEvent e = (NodeUnhealthyHeartbeatEvent) event;
      node.setLastHeartbeatTime(e.getHeartbeatTime());
      // TODO Audit logs
      LOG.info("Node transitioned to an unhealthy state"
          + ", node=" + e.nodeName
          + ", heartbeatTime=" + e.getHeartbeatTime()
          + ", healthStatus=" + e.getHealthStatus());
      node.setHealthStatus(e.getHealthStatus());
    }
  }

  static class NodeHeartbeatTimedOutTransition
      implements SingleArcTransition<NodeImpl, NodeEvent> {

    @Override
    public void transition(NodeImpl node, NodeEvent event) {
      NodeHeartbeatTimedOutEvent e = (NodeHeartbeatTimedOutEvent) event;
      // TODO Audit logs
      LOG.info("Node transitioned to heartbeat timed out state"
          + ", node=" + e.nodeName
          + ", lastHeartbeatTime=" + node.getLastHeartbeatTime());
      node.getHealthStatus().setHealthStatus(HealthStatus.UNKNOWN);
    }
  } 

  void importNodeInfo(NodeInfo nodeInfo) {
    try {
      writeLock.lock();
      this.hostName = nodeInfo.hostName;
      this.ipv4 = nodeInfo.ipv4;
      this.ipv6 = nodeInfo.ipv6;
      this.availableMemBytes = nodeInfo.availableMemBytes;
      this.totalMemBytes = nodeInfo.totalMemBytes;
      this.cpuCount = nodeInfo.cpuCount;
      this.osArch = nodeInfo.osArch;
      this.osType = nodeInfo.osType;
      this.osInfo = nodeInfo.osInfo;
      this.disksInfo = nodeInfo.disksInfo;
      this.rackInfo = nodeInfo.rackInfo;
      this.hostAttributes = nodeInfo.hostAttributes;
    }
    finally {
      writeLock.unlock();      
    }
  }
   
  @Override
  public NodeState getState() {
    try {
      readLock.lock();
      return stateMachine.getCurrentState();
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public void setState(NodeState state) {
    try {
      writeLock.lock();
      stateMachine.setCurrentState(state);
    }
    finally {
      writeLock.unlock();
    }
  }

  @Override
  public void handleEvent(NodeEvent event)
      throws InvalidStateTransitonException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Handling Node event, eventType=" + event.getType().name()
          + ", event=" + event.toString());
    }
    NodeState oldState = getState();
    try {
      writeLock.lock();
      try {
        stateMachine.doTransition(event.getType(), event);
      } catch (InvalidStateTransitonException e) {
        LOG.error("Can't handle Node event at current state"
            + ", node=" + this.getHostName()
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
        LOG.debug("Node transitioned to a new state"
            + ", node=" + this.getHostName()
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
      return hostName;
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public void setHostName(String hostName) {
    try {
      writeLock.lock();
      this.hostName = hostName;
    }
    finally {
      writeLock.unlock();
    }
  }

  @Override
  public String getIPv4() {
    try {
      readLock.lock();
      return ipv4;
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public void setIPv4(String ip) {
    try {
      writeLock.lock();
      this.ipv4 = ip;
    }
    finally {
      writeLock.unlock();
    }
  }

  @Override
  public String getIPv6() {
    try {
      readLock.lock();
      return ipv6;
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public void setIPv6(String ip) {
    try {
      writeLock.lock();
      this.ipv6 = ip;
    }
    finally {
      writeLock.unlock();
    }
  }

  @Override
  public int getCpuCount() {
    try {
      readLock.lock();
      return cpuCount;
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public void setCpuCount(int cpuCount) {
    try {
      writeLock.lock();
      this.cpuCount = cpuCount;
    }
    finally {
      writeLock.unlock();
    }
  }

  @Override
  public long getTotalMemBytes() {
    try {
      readLock.lock();
      return totalMemBytes;
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public void setTotalMemBytes(long totalMemBytes) {
    try {
      writeLock.lock();
      this.totalMemBytes = totalMemBytes;
    }
    finally {
      writeLock.unlock();
    }
  }

  @Override
  public long getAvailableMemBytes() {
    try {
      readLock.lock();
      return availableMemBytes;
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public void setAvailableMemBytes(long availableMemBytes) {
    try {
      writeLock.lock();
      this.availableMemBytes = availableMemBytes;
    }
    finally {
      writeLock.unlock();
    }
  }

  @Override
  public String getOsArch() {
    try {
      readLock.lock();
      return osArch;
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public void setOsArch(String osArch) {
    try {
      writeLock.lock();
      this.osArch = osArch;
    }
    finally {
      writeLock.unlock();
    }
  }

  @Override
  public String getOsInfo() {
    try {
      readLock.lock();
      return osInfo;
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public void setOsInfo(String osInfo) {
    try {
      writeLock.lock();
      this.osInfo = osInfo;
    }
    finally {
      writeLock.unlock();
    }
  }

  @Override
  public String getOsType() {
    try {
      readLock.lock();
      return osType;
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public void setOsType(String osType) {
    try {
      writeLock.lock();
      this.osType = osType;
    }
    finally {
      writeLock.unlock();
    }
  }

  @Override
  public List<DiskInfo> getDisksInfo() {
    try {
      readLock.lock();
      return Collections.unmodifiableList(disksInfo);
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public void setDisksInfo(List<DiskInfo> disksInfo) {
    try {
      writeLock.lock();
      this.disksInfo = disksInfo;
    }
    finally {
      writeLock.unlock();
    }
  }

  @Override
  public NodeHealthStatus getHealthStatus() {
    try {
      readLock.lock();
      return healthStatus;
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public void setHealthStatus(NodeHealthStatus healthStatus) {
    try {
      writeLock.lock();
      this.healthStatus = healthStatus;
    }
    finally {
      writeLock.unlock();
    }
  }

  @Override
  public Map<String, String> getHostAttributes() {
    try {
      readLock.lock();
      return Collections.unmodifiableMap(hostAttributes);
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public void setHostAttributes(Map<String, String> hostAttributes) {
    try {
      writeLock.lock();
      this.hostAttributes = hostAttributes;
    }
    finally {
      writeLock.unlock();
    }
  }

  @Override
  public String getRackInfo() {
    try {
      readLock.lock();
      return rackInfo;
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public void setRackInfo(String rackInfo) {
    try {
      writeLock.lock();
      this.rackInfo = rackInfo;
    }
    finally {
      writeLock.unlock();
    }
  }

  @Override
  public long getLastRegistrationTime() {
    try {
      readLock.lock();
      return lastRegistrationTime;
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public void setLastRegistrationTime(long lastRegistrationTime) {
    try {
      writeLock.lock();
      this.lastRegistrationTime = lastRegistrationTime;
    }
    finally {
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
      return agentVersion;
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public void setAgentVersion(AgentVersion agentVersion) {
    try {
      writeLock.lock();
      this.agentVersion = agentVersion;
    }
    finally {
      writeLock.unlock();
    }
  }

  @Override
  public List<Job> getJobs() {
    // TODO Auto-generated method stub
    return null;
  }

}
