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

package org.apache.ambari.server.state.cluster;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;
import junit.framework.Assert;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.DiskInfo;
import org.apache.ambari.server.agent.HostInfo;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.controller.ClusterResponse;
import org.apache.ambari.server.state.AgentVersion;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackVersion;
import org.apache.ambari.server.state.cluster.ClustersImpl;
import org.apache.ambari.server.state.fsm.InvalidStateTransitonException;
import org.apache.ambari.server.state.host.HostHealthyHeartbeatEvent;
import org.apache.ambari.server.state.host.HostRegistrationRequestEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ClusterTest {

  private Clusters clusters;
  private Cluster c1;
  String h1 = "h1";
  String s1 = "s1";
  String sc1 = "sc1";
  private Injector injector;

  @Before
  public void setup() throws AmbariException {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    clusters = injector.getInstance(Clusters.class);
    clusters.addCluster("c1");
    c1 = clusters.getCluster("c1");
    Assert.assertEquals("c1", c1.getClusterName());
    clusters.addHost(h1);
    clusters.mapHostToCluster(h1, "c1");
  }

  @After
  public void teardown() {
    injector.getInstance(PersistService.class).stop();
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
  public void testBasicClusterSetup() throws AmbariException {
    String clusterName = "c2";

    try {
      clusters.getCluster(clusterName);
      fail("Exception expected for invalid cluster");
    } catch (Exception e) {
      // Expected
    }

    clusters.addCluster(clusterName);
    Cluster c2 = clusters.getCluster(clusterName);

    Assert.assertNotNull(c2);

//    Assert.assertEquals(clusterName.hashCode(), c2.getClusterId()); This is not true
    Assert.assertEquals(clusterName, c2.getClusterName());

    c2.setClusterName("foo2");
    Assert.assertEquals("foo2", c2.getClusterName());
//    Assert.assertEquals(clusterName.hashCode(), c2.getClusterId());

    Assert.assertNotNull(c2.getDesiredStackVersion());
    Assert.assertEquals("", c2.getDesiredStackVersion().getStackVersion());

    StackVersion stackVersion = new StackVersion("1.0");
    c2.setDesiredStackVersion(stackVersion);
    Assert.assertEquals("1.0", c2.getDesiredStackVersion().getStackVersion());
  }

  @Test
  public void testAddAndGetServices() {
    // TODO write unit tests for
    // public void addService(Service service) throws AmbariException;
    // public Service getService(String serviceName) throws AmbariException;
    // public Map<String, Service> getServices();
  }


  @Test
  public void testGetServiceComponentHosts() {
    // TODO write unit tests
    // public List<ServiceComponentHost> getServiceComponentHosts(String hostname);
  }


  @Test
  public void testGetAndSetConfigs() {
    // TODO write unit tests
    // public Map<String, Config> getConfigsByType(String configType);
    // public Config getConfig(String configType, String versionTag);
    // public void addConfig(Config config);
  }

  @Test
  public void testConvertToResponse() {
    // TODO write unit tests
  }

  @Test
  public void testDebugDump() {
    // TODO write unit tests
  }

}
