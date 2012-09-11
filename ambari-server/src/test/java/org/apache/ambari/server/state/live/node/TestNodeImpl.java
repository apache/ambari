package org.apache.ambari.server.state.live.node;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.server.agent.DiskInfo;
import org.apache.ambari.server.agent.NodeInfo;
import org.apache.ambari.server.state.live.AgentVersion;
import org.apache.ambari.server.state.live.node.NodeHealthStatus.HealthStatus;
import org.junit.Assert;
import org.junit.Test;

public class TestNodeImpl {

  @Test
  public void testNodeInfoImport() {
    NodeInfo info = new NodeInfo();
    info.setMemorySize(100);
    info.setProcessorCount(10);
    List<DiskInfo> mounts = new ArrayList<DiskInfo>();
    mounts.add(new DiskInfo("/dev/sda", "/mnt/disk1",
        "5000000", "4000000", "10%", "size"));
    info.setMounts(mounts);
    
    info.setHostName("foo");
    info.setInterfaces("fip_4");
    info.setArchitecture("os_arch");
    info.setOS("os_type");
    info.setMemoryTotal(10);

    NodeImpl node = new NodeImpl();
    node.importNodeInfo(info);
   
    Assert.assertEquals(info.getHostName(), node.getHostName());
    Assert.assertEquals(info.getFreeMemory(), node.getAvailableMemBytes());
    Assert.assertEquals(info.getMemoryTotal(), node.getTotalMemBytes());
    Assert.assertEquals(info.getProcessorCount(), node.getCpuCount());
    Assert.assertEquals(info.getMounts().size(), node.getDisksInfo().size());
    Assert.assertEquals(info.getArchitecture(), node.getOsArch());
    Assert.assertEquals(info.getOS(), node.getOsType());
  }

  private void registerNode(NodeImpl node) throws Exception {
    NodeInfo info = new NodeInfo();
    info.setMemorySize(100);
    info.setProcessorCount(10);
    List<DiskInfo> mounts = new ArrayList<DiskInfo>();
    mounts.add(new DiskInfo("/dev/sda", "/mnt/disk1",
        "5000000", "4000000", "10%", "size"));
    info.setMounts(mounts);
    
    info.setHostName("foo");
    info.setInterfaces("fip_4");
    info.setArchitecture("os_arch");
    info.setOS("os_type");
    info.setMemoryTotal(10);

    AgentVersion agentVersion = null;
    long currentTime = System.currentTimeMillis();

    NodeRegistrationRequestEvent e =
        new NodeRegistrationRequestEvent("foo", agentVersion, currentTime,
            info);
    node.handleEvent(e);
    Assert.assertEquals(node.getLastRegistrationTime(), currentTime);
  }

  private void verifyNode(NodeImpl node) throws Exception {
    NodeVerifiedEvent e = new NodeVerifiedEvent(node.getHostName());
    node.handleEvent(e);
  }

  private void verifyNodeState(NodeImpl node, NodeState state) {
    Assert.assertEquals(node.getState(), state);
  }

  private void sendHealthyHeartbeat(NodeImpl node, long counter) throws Exception {
    NodeHealthyHeartbeatEvent e = new NodeHealthyHeartbeatEvent(
        node.getHostName(), counter);
    node.handleEvent(e);
  }

  private void sendUnhealthyHeartbeat(NodeImpl node, long counter) throws Exception {
    NodeHealthStatus healthStatus = new NodeHealthStatus(HealthStatus.UNHEALTHY,
        "Unhealthy server");
    NodeUnhealthyHeartbeatEvent e = new NodeUnhealthyHeartbeatEvent(
        node.getHostName(), counter, healthStatus);
    node.handleEvent(e);
  }

  private void timeoutNode(NodeImpl node) throws Exception {
    NodeHeartbeatTimedOutEvent e = new NodeHeartbeatTimedOutEvent(
        node.getHostName());
    node.handleEvent(e);
  }

  @Test
  public void testNodeFSMInit() {
    NodeImpl node = new NodeImpl();
    verifyNodeState(node, NodeState.INIT);
  }

  @Test
  public void testNodeRegistrationFlow() throws Exception {
    NodeImpl node = new NodeImpl();
    registerNode(node);
    verifyNodeState(node, NodeState.WAITING_FOR_VERIFICATION);

    boolean exceptionThrown = false;
    try {
      registerNode(node);
    } catch (Exception e) {
      // Expected
      exceptionThrown = true;
    }
    if (!exceptionThrown) {
      fail("Expected invalid transition exception to be thrown");
    }

    verifyNode(node);
    verifyNodeState(node, NodeState.VERIFIED);

    exceptionThrown = false;
    try {
      verifyNode(node);
    } catch (Exception e) {
      // Expected
      exceptionThrown = true;
    }
    if (!exceptionThrown) {
      fail("Expected invalid transition exception to be thrown");
    }
  }

  @Test
  public void testNodeHeartbeatFlow() throws Exception {
    NodeImpl node = new NodeImpl();
    registerNode(node);
    verifyNode(node);

    // TODO need to verify audit logs generated
    // TODO need to verify health status updated properly

    long counter = 0;
    sendHealthyHeartbeat(node, ++counter);
    verifyNodeState(node, NodeState.HEALTHY);
    Assert.assertEquals(node.getLastHeartbeatTime(), counter);

    sendHealthyHeartbeat(node, ++counter);
    verifyNodeState(node, NodeState.HEALTHY);
    Assert.assertEquals(node.getLastHeartbeatTime(), counter);
    Assert.assertEquals(node.getHealthStatus().getHealthStatus(),
        HealthStatus.HEALTHY);

    sendUnhealthyHeartbeat(node, ++counter);
    verifyNodeState(node, NodeState.UNHEALTHY);
    Assert.assertEquals(node.getLastHeartbeatTime(), counter);
    Assert.assertEquals(node.getHealthStatus().getHealthStatus(),
        HealthStatus.UNHEALTHY);

    sendUnhealthyHeartbeat(node, ++counter);
    verifyNodeState(node, NodeState.UNHEALTHY);
    Assert.assertEquals(node.getLastHeartbeatTime(), counter);
    Assert.assertEquals(node.getHealthStatus().getHealthStatus(),
        HealthStatus.UNHEALTHY);

    sendHealthyHeartbeat(node, ++counter);
    verifyNodeState(node, NodeState.HEALTHY);
    Assert.assertEquals(node.getLastHeartbeatTime(), counter);
    Assert.assertEquals(node.getHealthStatus().getHealthStatus(),
        HealthStatus.HEALTHY);

    timeoutNode(node);
    verifyNodeState(node, NodeState.HEARTBEAT_LOST);
    Assert.assertEquals(node.getLastHeartbeatTime(), counter);
    Assert.assertEquals(node.getHealthStatus().getHealthStatus(),
        HealthStatus.UNKNOWN);

    timeoutNode(node);
    verifyNodeState(node, NodeState.HEARTBEAT_LOST);
    Assert.assertEquals(node.getLastHeartbeatTime(), counter);
    Assert.assertEquals(node.getHealthStatus().getHealthStatus(),
        HealthStatus.UNKNOWN);

    sendUnhealthyHeartbeat(node, ++counter);
    verifyNodeState(node, NodeState.UNHEALTHY);
    Assert.assertEquals(node.getLastHeartbeatTime(), counter);
    Assert.assertEquals(node.getHealthStatus().getHealthStatus(),
        HealthStatus.UNHEALTHY);

    timeoutNode(node);
    verifyNodeState(node, NodeState.HEARTBEAT_LOST);
    Assert.assertEquals(node.getLastHeartbeatTime(), counter);
    Assert.assertEquals(node.getHealthStatus().getHealthStatus(),
        HealthStatus.UNKNOWN);

    sendHealthyHeartbeat(node, ++counter);
    verifyNodeState(node, NodeState.HEALTHY);
    Assert.assertEquals(node.getLastHeartbeatTime(), counter);
    Assert.assertEquals(node.getHealthStatus().getHealthStatus(),
        HealthStatus.HEALTHY);

  }
}
