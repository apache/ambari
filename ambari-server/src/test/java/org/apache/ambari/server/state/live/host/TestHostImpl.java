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

package org.apache.ambari.server.state.live.host;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.server.agent.DiskInfo;
import org.apache.ambari.server.agent.HostInfo;
import org.apache.ambari.server.state.live.AgentVersion;
import org.apache.ambari.server.state.live.host.HostHealthStatus.HealthStatus;
import org.junit.Assert;
import org.junit.Test;

public class TestHostImpl {

  @Test
  public void testHostInfoImport() {
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

    HostImpl host = new HostImpl("foo");
    host.importHostInfo(info);

    Assert.assertEquals(info.getHostName(), host.getHostName());
    Assert.assertEquals(info.getFreeMemory(), host.getAvailableMemBytes());
    Assert.assertEquals(info.getMemoryTotal(), host.getTotalMemBytes());
    Assert.assertEquals(info.getProcessorCount(), host.getCpuCount());
    Assert.assertEquals(info.getMounts().size(), host.getDisksInfo().size());
    Assert.assertEquals(info.getArchitecture(), host.getOsArch());
    Assert.assertEquals(info.getOS(), host.getOsType());
  }

  private void registerHost(HostImpl host) throws Exception {
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

    HostRegistrationRequestEvent e =
        new HostRegistrationRequestEvent("foo", agentVersion, currentTime,
            info);
    host.handleEvent(e);
    Assert.assertEquals(currentTime, host.getLastRegistrationTime());
  }

  private void verifyHost(HostImpl host) throws Exception {
    HostVerifiedEvent e = new HostVerifiedEvent(host.getHostName());
    host.handleEvent(e);
  }

  private void verifyHostState(HostImpl host, HostState state) {
    Assert.assertEquals(state, host.getState());
  }

  private void sendHealthyHeartbeat(HostImpl host, long counter) throws Exception {
    HostHealthyHeartbeatEvent e = new HostHealthyHeartbeatEvent(
        host.getHostName(), counter);
    host.handleEvent(e);
  }

  private void sendUnhealthyHeartbeat(HostImpl host, long counter) throws Exception {
    HostHealthStatus healthStatus = new HostHealthStatus(HealthStatus.UNHEALTHY,
        "Unhealthy server");
    HostUnhealthyHeartbeatEvent e = new HostUnhealthyHeartbeatEvent(
        host.getHostName(), counter, healthStatus);
    host.handleEvent(e);
  }

  private void timeoutHost(HostImpl host) throws Exception {
    HostHeartbeatTimedOutEvent e = new HostHeartbeatTimedOutEvent(
        host.getHostName());
    host.handleEvent(e);
  }

  @Test
  public void testHostFSMInit() {
    HostImpl host = new HostImpl("foo");
    verifyHostState(host, HostState.INIT);
  }

  @Test
  public void testHostRegistrationFlow() throws Exception {
    HostImpl host = new HostImpl("foo");
    registerHost(host);
    verifyHostState(host, HostState.WAITING_FOR_VERIFICATION);

    boolean exceptionThrown = false;
    try {
      registerHost(host);
    } catch (Exception e) {
      // Expected
      exceptionThrown = true;
    }
    if (!exceptionThrown) {
      fail("Expected invalid transition exception to be thrown");
    }

    verifyHost(host);
    verifyHostState(host, HostState.VERIFIED);

    exceptionThrown = false;
    try {
      verifyHost(host);
    } catch (Exception e) {
      // Expected
      exceptionThrown = true;
    }
    if (!exceptionThrown) {
      fail("Expected invalid transition exception to be thrown");
    }
  }

  @Test
  public void testHostHeartbeatFlow() throws Exception {
    HostImpl host = new HostImpl("foo");
    registerHost(host);
    verifyHost(host);

    // TODO need to verify audit logs generated
    // TODO need to verify health status updated properly

    long counter = 0;
    sendHealthyHeartbeat(host, ++counter);
    verifyHostState(host, HostState.HEALTHY);
    Assert.assertEquals(counter, host.getLastHeartbeatTime());

    sendHealthyHeartbeat(host, ++counter);
    verifyHostState(host, HostState.HEALTHY);
    Assert.assertEquals(counter, host.getLastHeartbeatTime());
    Assert.assertEquals(HealthStatus.HEALTHY,
        host.getHealthStatus().getHealthStatus());

    sendUnhealthyHeartbeat(host, ++counter);
    verifyHostState(host, HostState.UNHEALTHY);
    Assert.assertEquals(counter, host.getLastHeartbeatTime());
    Assert.assertEquals(HealthStatus.UNHEALTHY,
        host.getHealthStatus().getHealthStatus());

    sendUnhealthyHeartbeat(host, ++counter);
    verifyHostState(host, HostState.UNHEALTHY);
    Assert.assertEquals(counter, host.getLastHeartbeatTime());
    Assert.assertEquals(HealthStatus.UNHEALTHY,
        host.getHealthStatus().getHealthStatus());

    sendHealthyHeartbeat(host, ++counter);
    verifyHostState(host, HostState.HEALTHY);
    Assert.assertEquals(counter, host.getLastHeartbeatTime());
    Assert.assertEquals(HealthStatus.HEALTHY,
        host.getHealthStatus().getHealthStatus());

    timeoutHost(host);
    verifyHostState(host, HostState.HEARTBEAT_LOST);
    Assert.assertEquals(counter, host.getLastHeartbeatTime());
    Assert.assertEquals(HealthStatus.UNKNOWN,
        host.getHealthStatus().getHealthStatus());

    timeoutHost(host);
    verifyHostState(host, HostState.HEARTBEAT_LOST);
    Assert.assertEquals(counter, host.getLastHeartbeatTime());
    Assert.assertEquals(HealthStatus.UNKNOWN,
        host.getHealthStatus().getHealthStatus());

    sendUnhealthyHeartbeat(host, ++counter);
    verifyHostState(host, HostState.UNHEALTHY);
    Assert.assertEquals(counter, host.getLastHeartbeatTime());
    Assert.assertEquals(HealthStatus.UNHEALTHY,
        host.getHealthStatus().getHealthStatus());

    timeoutHost(host);
    verifyHostState(host, HostState.HEARTBEAT_LOST);
    Assert.assertEquals(counter, host.getLastHeartbeatTime());
    Assert.assertEquals(HealthStatus.UNKNOWN,
        host.getHealthStatus().getHealthStatus());

    sendHealthyHeartbeat(host, ++counter);
    verifyHostState(host, HostState.HEALTHY);
    Assert.assertEquals(counter, host.getLastHeartbeatTime());
    Assert.assertEquals(HealthStatus.HEALTHY,
        host.getHealthStatus().getHealthStatus());

  }
}
