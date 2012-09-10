package org.apache.ambari.server.state.live.node;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;

import org.apache.ambari.server.state.live.AgentVersion;
import org.apache.ambari.server.state.live.DiskInfo;
import org.apache.ambari.server.state.live.node.NodeHealthStatus.HealthStatus;

public class TestNodeImpl {


  @Test
  public void testNodeInfoImport() {
    NodeInfo info = new NodeInfo();
    info.availableMemBytes = 100;
    info.cpuCount = 10;
    info.disksInfo = new ArrayList<DiskInfo>();
    info.disksInfo.add(new DiskInfo("/dev/sda", "ext3", "/mnt/disk1",
        5000000, 4000000));
    info.hostAttributes = new HashMap<String, String>();
    info.hostName = "foo";
    info.ipv4 = "fip_4";
    info.osArch = "os_arch";
    info.osType = "os_type";
    info.osInfo = "os_info";
    info.rackInfo = "/default-rack";
    info.totalMemBytes = 200;

    NodeImpl node = new NodeImpl();
    node.importNodeInfo(info);

    Assert.assertEquals(info.hostName, node.getHostName());
    Assert.assertEquals(info.ipv4, node.getIPv4());
    Assert.assertEquals(info.ipv6, node.getIPv6());
    Assert.assertEquals(info.availableMemBytes, node.getAvailableMemBytes());
    Assert.assertEquals(info.totalMemBytes, node.getTotalMemBytes());
    Assert.assertEquals(info.cpuCount, node.getCpuCount());
    Assert.assertEquals(info.disksInfo.size(), node.getDisksInfo().size());
    Assert.assertEquals(info.hostAttributes.size(),
        node.getHostAttributes().size());
    Assert.assertEquals(info.osArch, node.getOsArch());
    Assert.assertEquals(info.osType, node.getOsType());
    Assert.assertEquals(info.osInfo, node.getOsInfo());
    Assert.assertEquals(info.rackInfo, node.getRackInfo());

  }

  private void registerNode(NodeImpl node) throws Exception {
    NodeInfo info = new NodeInfo();
    info.availableMemBytes = 100;
    info.cpuCount = 10;
    info.disksInfo = new ArrayList<DiskInfo>();
    info.disksInfo.add(new DiskInfo("/dev/sda", "ext3", "/mnt/disk1",
        5000000, 4000000));
    info.hostAttributes = new HashMap<String, String>();
    info.hostName = "foo";
    info.ipv4 = "fip_4";
    info.osArch = "os_arch";
    info.osType = "os_type";
    info.osInfo = "os_info";
    info.rackInfo = "/default-rack";
    info.totalMemBytes = 200;
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
