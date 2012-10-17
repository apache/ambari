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

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.DiskInfo;
import org.apache.ambari.server.agent.HostInfo;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.state.*;
import org.apache.ambari.server.state.HostHealthStatus.HealthStatus;
import org.apache.ambari.server.state.host.HostHealthyHeartbeatEvent;
import org.apache.ambari.server.state.host.HostHeartbeatLostEvent;
import org.apache.ambari.server.state.host.HostRegistrationRequestEvent;
import org.apache.ambari.server.state.host.HostStatusUpdatesReceivedEvent;
import org.apache.ambari.server.state.host.HostUnhealthyHeartbeatEvent;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class HostTest {

  private Injector injector;
  private Clusters clusters;

  @Before
   public void setup() throws AmbariException{
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    clusters = injector.getInstance(Clusters.class);
  }

  @After
  public void teardown() throws AmbariException {
    injector.getInstance(PersistService.class).stop();
  }

  @Test
  public void testHostInfoImport() throws AmbariException{
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

    clusters.addHost("foo");
    Host host = clusters.getHost("foo");

    host.importHostInfo(info);

    Assert.assertEquals(info.getHostName(), host.getHostName());
    Assert.assertEquals(info.getFreeMemory(), host.getAvailableMemBytes());
    Assert.assertEquals(info.getMemoryTotal(), host.getTotalMemBytes());
    Assert.assertEquals(info.getProcessorCount(), host.getCpuCount());
    Assert.assertEquals(info.getMounts().size(), host.getDisksInfo().size());
    Assert.assertEquals(info.getArchitecture(), host.getOsArch());
    Assert.assertEquals(info.getOS(), host.getOsType());
  }

  private void registerHost(Host host) throws Exception {
    registerHost(host, true);
  }

  private void registerHost(Host host, boolean firstReg) throws Exception {
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
    if (!firstReg) {
      Assert.assertTrue(host.isPersisted());
    }
    host.handleEvent(e);
    Assert.assertEquals(currentTime, host.getLastRegistrationTime());
  }

  private void ensureHostUpdatesReceived(Host host) throws Exception {
    HostStatusUpdatesReceivedEvent e =
        new HostStatusUpdatesReceivedEvent(host.getHostName(), 1);
    host.handleEvent(e);
  }

  private void verifyHostState(Host host, HostState state) {
    Assert.assertEquals(state, host.getState());
  }

  private void sendHealthyHeartbeat(Host host, long counter)
      throws Exception {
    HostHealthyHeartbeatEvent e = new HostHealthyHeartbeatEvent(
        host.getHostName(), counter);
    host.handleEvent(e);
  }

  private void sendUnhealthyHeartbeat(Host host, long counter)
      throws Exception {
    HostHealthStatus healthStatus = new HostHealthStatus(HealthStatus.UNHEALTHY,
        "Unhealthy server");
    HostUnhealthyHeartbeatEvent e = new HostUnhealthyHeartbeatEvent(
        host.getHostName(), counter, healthStatus);
    host.handleEvent(e);
  }

  private void timeoutHost(Host host) throws Exception {
    HostHeartbeatLostEvent e = new HostHeartbeatLostEvent(
        host.getHostName());
    host.handleEvent(e);
  }

  @Test
  public void testHostFSMInit() throws AmbariException{
    clusters.addHost("foo");
    Host host = clusters.getHost("foo");
    verifyHostState(host, HostState.INIT);
  }

  @Test
  public void testHostRegistrationFlow() throws Exception {
    clusters.addHost("foo");
    Host host = clusters.getHost("foo");
    registerHost(host);
    verifyHostState(host, HostState.WAITING_FOR_HOST_STATUS_UPDATES);

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

    ensureHostUpdatesReceived(host);
    verifyHostState(host, HostState.HEALTHY);

    exceptionThrown = false;
    try {
      ensureHostUpdatesReceived(host);
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
    clusters.addHost("foo");
    Host host = clusters.getHost("foo");
    registerHost(host);
    ensureHostUpdatesReceived(host);

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

    try {
      sendUnhealthyHeartbeat(host, ++counter);
      fail("Invalid event should have triggered an exception");
    } catch (Exception e) {
      // Expected
    }
    verifyHostState(host, HostState.HEARTBEAT_LOST);

    try {
      sendHealthyHeartbeat(host, ++counter);
      fail("Invalid event should have triggered an exception");
    } catch (Exception e) {
      // Expected
    }
    verifyHostState(host, HostState.HEARTBEAT_LOST);
  }

  @Test
  public void testHostRegistrationsInAnyState() throws Exception {
    clusters.addHost("foo");
    Host host = clusters.getHost("foo");
    host.setIPv4("ipv4");
    host.setIPv6("ipv6");

    long counter = 0;

    registerHost(host);

    ensureHostUpdatesReceived(host);
    registerHost(host, false);

    ensureHostUpdatesReceived(host);
    sendHealthyHeartbeat(host, ++counter);
    verifyHostState(host, HostState.HEALTHY);
    registerHost(host, false);
    ensureHostUpdatesReceived(host);

    sendUnhealthyHeartbeat(host, ++counter);
    verifyHostState(host, HostState.UNHEALTHY);
    registerHost(host, false);
    ensureHostUpdatesReceived(host);

    timeoutHost(host);
    verifyHostState(host, HostState.HEARTBEAT_LOST);
    registerHost(host, false);
    ensureHostUpdatesReceived(host);

    host.setState(HostState.INIT);
    registerHost(host, false);

  }
}
