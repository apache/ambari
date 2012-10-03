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

package org.apache.ambari.server.state.live;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.DiskInfo;
import org.apache.ambari.server.agent.HostInfo;
import org.apache.ambari.server.state.AgentVersion;
import org.apache.ambari.server.state.ConfigVersion;
import org.apache.ambari.server.state.DeployState;
import org.apache.ambari.server.state.StackVersion;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.fsm.InvalidStateTransitonException;
import org.apache.ambari.server.state.live.host.HostHealthyHeartbeatEvent;
import org.apache.ambari.server.state.live.host.HostRegistrationRequestEvent;
import org.apache.ambari.server.state.live.host.HostState;
import org.apache.ambari.server.state.live.svccomphost.ServiceComponentHostInstallEvent;
import org.apache.ambari.server.state.live.svccomphost.ServiceComponentHostOpRestartedEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestClusterImpl {

  private Clusters clusters;
  private Cluster c1;
  String h1 = "h1";
  String s1 = "s1";
  String sc1 = "sc1";

  @Before
  public void setup() throws AmbariException {
    clusters = new ClustersImpl();
    clusters.addCluster("c1");
    c1 = clusters.getCluster("c1");
    Assert.assertEquals("c1", c1.getClusterName());
    clusters.addHost(h1);
    clusters.mapHostToCluster(h1, "c1");
    c1.addServiceComponentHost(s1, sc1, h1, false);
  }

  @After
  public void teardown() {
    clusters = null;
    c1 = null;
  }

  @Test
  public void testAddHost() throws AmbariException {
    clusters.addHost("h2");

    try {
      clusters.addHost("h2");
      fail("Duplicate add should fail");
    }
    catch (AmbariException e) {
      // Expected
    }

  }

  @Test
  public void testAddServiceComponentHost() throws AmbariException {
    try {
      c1.addServiceComponentHost("s2", "sc2", "h2", false);
      fail("Expected failure on invalid cluster/host");
    } catch (Exception e) {
      // Expected
    }

    clusters.addCluster("c2");
    clusters.addHost("h2");
    clusters.mapHostToCluster("h2", "c2");
    c1.addServiceComponentHost("s2", "sc2", "h2", false);

    try {
      c1.addServiceComponentHost("s2", "sc2", "h2", false);
      fail("Duplicate add should fail");
    }
    catch (AmbariException e) {
      // Expected
    }

  }

  @Test
  public void testGetHostState() throws AmbariException {
    Assert.assertEquals(HostState.INIT, clusters.getHost(h1).getState());
  }

  @Test
  public void testSetHostState() throws AmbariException {
    clusters.getHost(h1).setState(HostState.HEARTBEAT_LOST);
    Assert.assertEquals(HostState.HEARTBEAT_LOST,
        clusters.getHost(h1).getState());
  }

  @Test
  public void testHostEvent() throws AmbariException,
      InvalidStateTransitonException {
    HostInfo hostInfo = new HostInfo();
    hostInfo.setHostName(h1);
    hostInfo.setInterfaces("fip_4");
    hostInfo.setArchitecture("os_arch");
    hostInfo.setOS("os_type");
    hostInfo.setMemoryTotal(10);
    hostInfo.setMemorySize(100);
    hostInfo.setProcessorCount(10);
    List<DiskInfo> mounts = new ArrayList<DiskInfo>();
    mounts.add(new DiskInfo("/dev/sda", "/mnt/disk1",
        "5000000", "4000000", "10%", "size"));
    hostInfo.setMounts(mounts);

    AgentVersion agentVersion = new AgentVersion("0.0.x");
    long currentTime = 1001;

    clusters.getHost(h1).handleEvent(new HostRegistrationRequestEvent(
        h1, agentVersion, currentTime, hostInfo));

    Assert.assertEquals(HostState.WAITING_FOR_HOST_STATUS_UPDATES,
        clusters.getHost(h1).getState());

    clusters.getHost(h1).setState(HostState.HEARTBEAT_LOST);

    try {
      clusters.getHost(h1).handleEvent(
          new HostHealthyHeartbeatEvent(h1, currentTime));
      fail("Exception should be thrown on invalid event");
    }
    catch (InvalidStateTransitonException e) {
      // Expected
    }

  }

  @Test
  public void testGetServiceComponentHostState() throws AmbariException {
    Assert.assertNotNull(c1.getServiceComponentHostState(s1, sc1, h1));
    Assert.assertEquals(DeployState.INIT,
        c1.getServiceComponentHostState(s1, sc1, h1).getLiveState());
  }

  @Test
  public void testSetServiceComponentHostState() throws AmbariException {
    ConfigVersion cVersion = new ConfigVersion("0.0.c");
    StackVersion sVersion = new StackVersion("hadoop-x.y.z");
    DeployState liveState =
        DeployState.INSTALL_FAILED;
    State expected =
        new State(cVersion, sVersion, liveState);
    c1.setServiceComponentHostState(s1, sc1, h1, expected);

    State actual =
        c1.getServiceComponentHostState(s1, sc1, h1);

    Assert.assertEquals(expected, actual);
    Assert.assertEquals(DeployState.INSTALL_FAILED,
        actual.getLiveState());

  }

  @Test
  public void testServiceComponentHostEvent()
      throws AmbariException, InvalidStateTransitonException {
    ConfigVersion cVersion = new ConfigVersion("0.0.c");
    StackVersion sVersion = new StackVersion("hadoop-x.y.z");
    DeployState liveState =
        DeployState.INSTALL_FAILED;
    State expected =
        new State(cVersion, sVersion, liveState);
    c1.setServiceComponentHostState(s1, sc1, h1, expected);

    try {
      c1.handleServiceComponentHostEvent(s1, sc1, h1,
          new ServiceComponentHostInstallEvent(sc1, h1, 1001));
      fail("Exception should be thrown on invalid event");
    }
    catch (InvalidStateTransitonException e) {
      // Expected
    }

    c1.handleServiceComponentHostEvent(s1, sc1, h1,
        new ServiceComponentHostOpRestartedEvent(sc1, h1, 1002));

    Assert.assertEquals(DeployState.INSTALLING,
        c1.getServiceComponentHostState(s1, sc1, h1).getLiveState());

  }

}
