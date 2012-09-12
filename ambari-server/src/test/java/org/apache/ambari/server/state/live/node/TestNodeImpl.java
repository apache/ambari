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

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.server.agent.DiskInfo;
import org.apache.ambari.server.agent.HostInfo;
import org.apache.ambari.server.state.live.AgentVersion;
import org.apache.ambari.server.state.live.node.NodeHealthStatus.HealthStatus;
import org.junit.Assert;
import org.junit.Test;

public class TestNodeImpl {

  @Test
  public void testNodeInfoImport() {
    HostInfo info = new HostInfo();
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
    HostInfo info = new HostInfo();
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
    Assert.assertEquals(currentTime, node.getLastRegistrationTime());
  }

  private void verifyNode(NodeImpl node) throws Exception {
    NodeVerifiedEvent e = new NodeVerifiedEvent(node.getHostName());
    node.handleEvent(e);
  }

  private void verifyNodeState(NodeImpl node, NodeState state) {
    Assert.assertEquals(state, node.getState());
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
    Assert.assertEquals(counter, node.getLastHeartbeatTime());

    sendHealthyHeartbeat(node, ++counter);
    verifyNodeState(node, NodeState.HEALTHY);
    Assert.assertEquals(counter, node.getLastHeartbeatTime());
    Assert.assertEquals(HealthStatus.HEALTHY,
        node.getHealthStatus().getHealthStatus());

    sendUnhealthyHeartbeat(node, ++counter);
    verifyNodeState(node, NodeState.UNHEALTHY);
    Assert.assertEquals(counter, node.getLastHeartbeatTime());
    Assert.assertEquals(HealthStatus.UNHEALTHY,
        node.getHealthStatus().getHealthStatus());

    sendUnhealthyHeartbeat(node, ++counter);
    verifyNodeState(node, NodeState.UNHEALTHY);
    Assert.assertEquals(counter, node.getLastHeartbeatTime());
    Assert.assertEquals(HealthStatus.UNHEALTHY,
        node.getHealthStatus().getHealthStatus());

    sendHealthyHeartbeat(node, ++counter);
    verifyNodeState(node, NodeState.HEALTHY);
    Assert.assertEquals(counter, node.getLastHeartbeatTime());
    Assert.assertEquals(HealthStatus.HEALTHY,
        node.getHealthStatus().getHealthStatus());

    timeoutNode(node);
    verifyNodeState(node, NodeState.HEARTBEAT_LOST);
    Assert.assertEquals(counter, node.getLastHeartbeatTime());
    Assert.assertEquals(HealthStatus.UNKNOWN,
        node.getHealthStatus().getHealthStatus());

    timeoutNode(node);
    verifyNodeState(node, NodeState.HEARTBEAT_LOST);
    Assert.assertEquals(counter, node.getLastHeartbeatTime());
    Assert.assertEquals(HealthStatus.UNKNOWN,
        node.getHealthStatus().getHealthStatus());

    sendUnhealthyHeartbeat(node, ++counter);
    verifyNodeState(node, NodeState.UNHEALTHY);
    Assert.assertEquals(counter, node.getLastHeartbeatTime());
    Assert.assertEquals(HealthStatus.UNHEALTHY,
        node.getHealthStatus().getHealthStatus());

    timeoutNode(node);
    verifyNodeState(node, NodeState.HEARTBEAT_LOST);
    Assert.assertEquals(counter, node.getLastHeartbeatTime());
    Assert.assertEquals(HealthStatus.UNKNOWN,
        node.getHealthStatus().getHealthStatus());

    sendHealthyHeartbeat(node, ++counter);
    verifyNodeState(node, NodeState.HEALTHY);
    Assert.assertEquals(counter, node.getLastHeartbeatTime());
    Assert.assertEquals(HealthStatus.HEALTHY,
        node.getHealthStatus().getHealthStatus());

  }
}
