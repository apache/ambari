package org.apache.ambari.server.state.live;

import java.util.List;
import java.util.Map;

import org.apache.ambari.server.state.fsm.InvalidStateTransitonException;
import org.apache.ambari.server.state.fsm.SingleArcTransition;
import org.apache.ambari.server.state.fsm.StateMachine;
import org.apache.ambari.server.state.fsm.StateMachineFactory;

public class NodeImpl implements Node {

  private static final StateMachineFactory
    <NodeImpl, NodeState, NodeEventType, NodeEvent>
      stateMachineFactory
        = new StateMachineFactory<NodeImpl, NodeState, NodeEventType, NodeEvent>
        (NodeState.INIT)

   // define the state machine of a Node

   .addTransition(NodeState.INIT, NodeState.WAITING_FOR_VERIFICATION,
       NodeEventType.NODE_REGISTRATION_REQUEST)

   .addTransition(NodeState.WAITING_FOR_VERIFICATION, NodeState.HEALTHY,
       NodeEventType.NODE_VERIFIED, new NodeVerifiedTransition())

   // TODO - should be able to combine multiple into a single multi-arc
   // transition
   .addTransition(NodeState.HEALTHY, NodeState.HEALTHY,
       NodeEventType.NODE_HEARTBEAT_HEALTHY)
   .addTransition(NodeState.HEALTHY, NodeState.HEARTBEAT_LOST,
       NodeEventType.NODE_HEARTBEAT_TIMED_OUT)
   .addTransition(NodeState.HEALTHY, NodeState.UNHEALTHY,
       NodeEventType.NODE_HEARTBEAT_UNHEALTHY)

   .addTransition(NodeState.UNHEALTHY, NodeState.HEALTHY,
       NodeEventType.NODE_HEARTBEAT_HEALTHY)
   .addTransition(NodeState.UNHEALTHY, NodeState.UNHEALTHY,
       NodeEventType.NODE_HEARTBEAT_UNHEALTHY)
   .addTransition(NodeState.UNHEALTHY, NodeState.HEARTBEAT_LOST,
       NodeEventType.NODE_HEARTBEAT_TIMED_OUT)

   .addTransition(NodeState.HEARTBEAT_LOST, NodeState.HEALTHY,
       NodeEventType.NODE_HEARTBEAT_HEALTHY)
   .addTransition(NodeState.HEARTBEAT_LOST, NodeState.UNHEALTHY,
       NodeEventType.NODE_HEARTBEAT_UNHEALTHY)
   .addTransition(NodeState.HEARTBEAT_LOST, NodeState.HEARTBEAT_LOST,
       NodeEventType.NODE_HEARTBEAT_TIMED_OUT)
   .installTopology();

  private final StateMachine<NodeState, NodeEventType, NodeEvent> stateMachine;

  public NodeImpl() {
    super();
    this.stateMachine = stateMachineFactory.make(this);
  }

  static class NodeVerifiedTransition
      implements SingleArcTransition<NodeImpl, NodeEvent> {

    @Override
    public void transition(NodeImpl node, NodeEvent event) {
      // TODO Auto-generated method stub
    }

  }

  @Override
  public NodeState getState() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setState(NodeState state) {
    // TODO Auto-generated method stub

  }

  @Override
  public void handleEvent(NodeEvent event)
      throws InvalidStateTransitonException {
    // TODO Auto-generated method stub
    stateMachine.doTransition(event.getType(), event);
  }

  @Override
  public String getHostName() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setHostName(String hostName) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public String getIPv4() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setIPv4(String ip) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public String getIPv6() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setIPv6(String ip) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public int getCpuCount() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public void setCpuCount(int cpuCount) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public int getTotalMemBytes() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public void setTotalMemBytes(int totalMemBytes) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public int getAvailableMemBytes() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public void setAvailableMemBytes(int availableMemBytes) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public String getOsArch() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setOsArch(String osArch) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public String getOsInfo() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setOsInfo(String osInfo) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public String getOsType() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setOsType(String osType) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public List<DiskInfo> getDisksInfo() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setDisksInfo(List<DiskInfo> disksInfo) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public NodeHealthStatus getHealthStatus() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setHealthStatus(NodeHealthStatus healthStatus) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public Map<String, String> getHostAttributes() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setHostAttributes(Map<String, String> hostAttributes) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public String getRackInfo() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setRackInfo(String rackInfo) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public int getLastRegistrationTime() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public void setLastRegistrationTime(int lastRegistrationTime) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public int getLastHeartbeatTime() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public void setLastHeartbeatTime(int lastHeartbeatTime) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public AgentVersion getAgentVersion() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setAgentVersion(AgentVersion agentVersion) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public NodeState getNodeState() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<Job> getJobs() {
    // TODO Auto-generated method stub
    return null;
  }

}
