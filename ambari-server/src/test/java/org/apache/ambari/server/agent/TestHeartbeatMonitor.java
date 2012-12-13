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
package org.apache.ambari.server.agent;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.cluster.ClustersImpl;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TestHeartbeatMonitor {

  private static Injector injector;

  private String hostname1 = "host1";
  private String hostname2 = "host2";
  private String clusterName = "cluster1";
  private String serviceName = "HDFS";
  private int heartbeatMonitorWakeupIntervalMS = 30;
  private AmbariMetaInfo ambariMetaInfo;

  private static final Logger LOG =
          LoggerFactory.getLogger(TestHeartbeatMonitor.class);

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    //injector.getInstance(OrmTestHelper.class).createDefaultData();
    ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    ambariMetaInfo.init();
  }

  @After
  public void teardown() {
    injector.getInstance(PersistService.class).stop();
  }

  @Test
  public void testStateCommandsGeneration() throws AmbariException, InterruptedException,
          InvalidStateTransitionException {
    Clusters clusters = injector.getInstance(Clusters.class);
    clusters.addHost(hostname1);
    clusters.getHost(hostname1).setOsType("centos6");
    clusters.getHost(hostname1).persist();
    clusters.addHost(hostname2);
    clusters.getHost(hostname2).setOsType("centos6");
    clusters.getHost(hostname2).persist();
    clusters.addCluster(clusterName);
    Cluster cluster = clusters.getCluster(clusterName);
    cluster.setDesiredStackVersion(new StackId("HDP-0.1"));
    Set<String> hostNames = new HashSet<String>(){{
      add(hostname1);
      add(hostname2);
    }};
    clusters.mapHostsToCluster(hostNames, clusterName);
    Service hdfs = cluster.addService(serviceName);
    hdfs.persist();
    hdfs.addServiceComponent(Role.DATANODE.name()).persist();
    hdfs.getServiceComponent(Role.DATANODE.name()).addServiceComponentHost(hostname1).persist();
    hdfs.addServiceComponent(Role.NAMENODE.name()).persist();
    hdfs.getServiceComponent(Role.NAMENODE.name()).addServiceComponentHost(hostname1).persist();
    hdfs.addServiceComponent(Role.SECONDARY_NAMENODE.name()).persist();
    hdfs.getServiceComponent(Role.SECONDARY_NAMENODE.name()).addServiceComponentHost(hostname1).persist();

    ActionQueue aq = new ActionQueue();
    ActionManager am = mock(ActionManager.class);
    HeartbeatMonitor hm = new HeartbeatMonitor(clusters, aq, am, heartbeatMonitorWakeupIntervalMS);
    HeartBeatHandler handler = new HeartBeatHandler(clusters, aq, am, injector);
    Register reg = new Register();
    reg.setHostname(hostname1);
    reg.setResponseId(12);
    reg.setTimestamp(System.currentTimeMillis() - 300);
    HostInfo hi = new HostInfo();
    hi.setOS("Centos5");
    reg.setHardwareProfile(hi);
    handler.handleRegistration(reg);

    HeartBeat hb = new HeartBeat();
    hb.setHostname(hostname1);
    hb.setNodeStatus(new HostStatus(HostStatus.Status.HEALTHY, "cool"));
    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(12);
    handler.handleHeartBeat(hb);

    List<StatusCommand> cmds = hm.generateStatusCommands(hostname1);
    assertTrue("HeartbeatMonitor should generate StatusCommands for host1", cmds.size() == 3);
    assertEquals("HDFS", cmds.get(0).getServiceName());
    boolean  containsDATANODEStatus = false;
    boolean  containsNAMENODEStatus = false;
    boolean  containsSECONDARY_NAMENODEStatus = false;
    for (StatusCommand cmd : cmds) {
      containsDATANODEStatus |= cmd.getComponentName().equals("DATANODE");
      containsNAMENODEStatus |= cmd.getComponentName().equals("NAMENODE");
      containsSECONDARY_NAMENODEStatus |= cmd.getComponentName().equals("SECONDARY_NAMENODE");
    }
    assertEquals(true, containsDATANODEStatus);
    assertEquals(true, containsNAMENODEStatus);
    assertEquals(true, containsSECONDARY_NAMENODEStatus);
    cmds = hm.generateStatusCommands(hostname2);
    assertTrue("HeartbeatMonitor should not generate StatusCommands for host2 because it has no services", cmds.isEmpty());
  }


  @Test
  public void testHeartbeatStateCommandsEnqueueing() throws AmbariException, InterruptedException,
          InvalidStateTransitionException {
    Clusters clusters = injector.getInstance(Clusters.class);
    clusters.addHost(hostname1);
    clusters.getHost(hostname1).setOsType("centos5");
    clusters.getHost(hostname1).persist();
    clusters.addCluster(clusterName);
    Cluster cluster = clusters.getCluster(clusterName);
    cluster.setDesiredStackVersion(new StackId("HDP-0.1"));

    Set<String> hostNames = new HashSet<String>(){{
      add(hostname1);
     }};

    clusters.mapHostsToCluster(hostNames, clusterName);

    Service hdfs = cluster.addService(serviceName);
    hdfs.persist();
    hdfs.addServiceComponent(Role.DATANODE.name()).persist();
    hdfs.getServiceComponent(Role.DATANODE.name()).addServiceComponentHost(hostname1).persist();
    hdfs.addServiceComponent(Role.NAMENODE.name()).persist();
    hdfs.getServiceComponent(Role.NAMENODE.name()).addServiceComponentHost(hostname1).persist();
    hdfs.addServiceComponent(Role.SECONDARY_NAMENODE.name()).persist();
    hdfs.getServiceComponent(Role.SECONDARY_NAMENODE.name()).addServiceComponentHost(hostname1).persist();

    ActionQueue aqMock = mock(ActionQueue.class);
    ArgumentCaptor<AgentCommand> commandCaptor=ArgumentCaptor.
            forClass(AgentCommand.class);

    ActionManager am = mock(ActionManager.class);
    HeartbeatMonitor hm = new HeartbeatMonitor(clusters, aqMock, am, heartbeatMonitorWakeupIntervalMS);
    HeartBeatHandler handler = new HeartBeatHandler(clusters, aqMock, am,
        injector);
    Register reg = new Register();
    reg.setHostname(hostname1);
    reg.setResponseId(12);
    reg.setTimestamp(System.currentTimeMillis() - 15);
    HostInfo hi = new HostInfo();
    hi.setOS("Centos5");
    reg.setHardwareProfile(hi);
    handler.handleRegistration(reg);
    HeartBeat hb = new HeartBeat();
    hb.setHostname(hostname1);
    hb.setNodeStatus(new HostStatus(HostStatus.Status.HEALTHY, "cool"));
    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(13);
    handler.handleHeartBeat(hb);
    LOG.info("YYY");
    clusters.getHost(hostname1).setLastHeartbeatTime(System.currentTimeMillis() - 15);
    hm.start();
    Thread.sleep(3 * heartbeatMonitorWakeupIntervalMS);
    hm.shutdown();
    hm.join(2*heartbeatMonitorWakeupIntervalMS);
    if (hm.isAlive()) {
      fail("HeartbeatMonitor should be already stopped");
    }
    verify(aqMock, atLeast(2)).enqueue(eq(hostname1), commandCaptor.capture());  // After registration and by HeartbeatMonitor

    List<AgentCommand> cmds = commandCaptor.getAllValues();
    assertTrue("HeartbeatMonitor should generate StatusCommands for host1", cmds.size() >= 2);
    for(AgentCommand command: cmds) {
      assertEquals("HDFS", ((StatusCommand)command).getServiceName());
    }

  }

  @Test
  public void testHeartbeatLoss() throws AmbariException, InterruptedException,
          InvalidStateTransitionException {
    Clusters fsm = injector.getInstance(Clusters.class);
    String hostname = "host1";
    fsm.addHost(hostname);
    ActionQueue aq = new ActionQueue();
    ActionManager am = mock(ActionManager.class);
    HeartbeatMonitor hm = new HeartbeatMonitor(fsm, aq, am, 10);
    HeartBeatHandler handler = new HeartBeatHandler(fsm, aq, am, injector);
    Register reg = new Register();
    reg.setHostname(hostname);
    reg.setResponseId(12);
    reg.setTimestamp(System.currentTimeMillis() - 300);
    HostInfo hi = new HostInfo();
    hi.setOS("Centos5");
    reg.setHardwareProfile(hi);
    handler.handleRegistration(reg);
    HeartBeat hb = new HeartBeat();
    hb.setHostname(hostname);
    hb.setNodeStatus(new HostStatus(HostStatus.Status.HEALTHY, "cool"));
    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(12);
    handler.handleHeartBeat(hb);
    hm.start();
    aq.enqueue(hostname, new ExecutionCommand());
    //Heartbeat will expire and action queue will be flushed
    while (aq.size(hostname) != 0) {
      Thread.sleep(1);
    }
    assertEquals(fsm.getHost(hostname).getState(), HostState.HEARTBEAT_LOST);
  }
}
