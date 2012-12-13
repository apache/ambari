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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;

import junit.framework.Assert;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.ActionDBAccessor;
import org.apache.ambari.server.actionmanager.ActionDBAccessorImpl;
import org.apache.ambari.server.actionmanager.ActionDBInMemoryImpl;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.agent.HostStatus.Status;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStartEvent;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestHeartbeatHandler {

  private static final Logger log = LoggerFactory.getLogger(TestHeartbeatHandler.class);
  private Injector injector;
  private Clusters clusters;
  long requestId = 23;
  long stageId = 31;

  @Inject
  AmbariMetaInfo metaInfo;
  @Inject
  Configuration config;

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    clusters = injector.getInstance(Clusters.class);
    injector.injectMembers(this);
    metaInfo.init();
    log.debug("Using server os type=" + config.getServerOsType());
  }

  @After
  public void teardown() throws AmbariException {
    injector.getInstance(PersistService.class).stop();
  }

  @Test
  public void testHeartbeat() throws Exception {
    ActionManager am = new ActionManager(0, 0, null, null,
        new ActionDBInMemoryImpl());
    Clusters fsm = clusters;
    String hostname = "host1";
    fsm.addHost(hostname);
    Host hostObject = clusters.getHost(hostname);
    hostObject.setIPv4("ipv4");
    hostObject.setIPv6("ipv6");
    hostObject.setOsType("centos5");

    ActionQueue aq = new ActionQueue();

    HeartBeatHandler handler = new HeartBeatHandler(fsm, aq, am, injector);
    Register reg = new Register();
    HostInfo hi = new HostInfo();
    hi.setHostName("host1");
    hi.setOS("CentOS");
    hi.setOSRelease("5.8");
    reg.setHostname(hostname);
    reg.setHardwareProfile(hi);
    handler.handleRegistration(reg);

    hostObject.setState(HostState.UNHEALTHY);

    ExecutionCommand execCmd = new ExecutionCommand();
    execCmd.setCommandId("2-34");
    execCmd.setHostname(hostname);
    aq.enqueue(hostname, new ExecutionCommand());
    HeartBeat hb = new HeartBeat();
    hb.setResponseId(0);
    hb.setNodeStatus(new HostStatus(Status.HEALTHY, "I am ok"));
    hb.setHostname(hostname);

    handler.handleHeartBeat(hb);
    assertEquals(HostState.HEALTHY, hostObject.getState());
    assertEquals(0, aq.dequeueAll(hostname).size());
  }

  @Test
  public void testStatusHeartbeat() throws Exception {
    ActionManager am = new ActionManager(0, 0, null, null,
            new ActionDBInMemoryImpl());
    final String hostname = "host1";
    String clusterName = "cluster1";
    String serviceName = "HDFS";
    String componentName1 = "DATANODE";
    String componentName2 = "NAMENODE";
    //injector.injectMembers(this);

    clusters.addHost(hostname);
    clusters.getHost(hostname).setOsType("centos5");
    clusters.getHost(hostname).persist();
//    Host hostObject = clusters.getHost(hostname);
//    hostObject.setIPv4("ipv4");
//    hostObject.setIPv6("ipv6");
//    hostObject.setOsType("centos5");
//    hostObject.persist();
    clusters.addCluster(clusterName);

    Cluster cluster = clusters.getCluster(clusterName);
    cluster.setDesiredStackVersion(new StackId("HDP-0.1"));

    @SuppressWarnings("serial")
    Set<String> hostNames = new HashSet<String>(){{
      add(hostname);
    }};
    clusters.mapHostsToCluster(hostNames, clusterName);
    Service hdfs = cluster.addService(serviceName);
    hdfs.persist();
    hdfs.addServiceComponent(Role.DATANODE.name()).persist();
    hdfs.getServiceComponent(Role.DATANODE.name()).addServiceComponentHost(hostname).persist();
    hdfs.addServiceComponent(Role.NAMENODE.name()).persist();
    hdfs.getServiceComponent(Role.NAMENODE.name()).addServiceComponentHost(hostname).persist();
    hdfs.addServiceComponent(Role.SECONDARY_NAMENODE.name()).persist();
    hdfs.getServiceComponent(Role.SECONDARY_NAMENODE.name()).addServiceComponentHost(hostname).persist();

    ActionQueue aq = new ActionQueue();
    HeartBeatHandler handler = new HeartBeatHandler(clusters, aq, am, injector);

    Register reg = new Register();
    HostInfo hi = new HostInfo();
    hi.setHostName("host1");
    hi.setOS("CentOS");
    hi.setOSRelease("5.8");
    reg.setHostname(hostname);
    reg.setResponseId(0);
    reg.setHardwareProfile(hi);
    handler.handleRegistration(reg);

    ServiceComponentHost serviceComponentHost1 = clusters.getCluster(clusterName).getService(serviceName).
            getServiceComponent(componentName1).getServiceComponentHost(hostname);
    ServiceComponentHost serviceComponentHost2 = clusters.getCluster(clusterName).getService(serviceName).
            getServiceComponent(componentName2).getServiceComponentHost(hostname);
    serviceComponentHost1.setState(State.INSTALLED);
    serviceComponentHost2.setState(State.INSTALLED);


    HeartBeat hb = new HeartBeat();
    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(0);
    hb.setHostname(hostname);
    hb.setNodeStatus(new HostStatus(Status.HEALTHY, "I am ok"));
    hb.setReports(new ArrayList<CommandReport>());
    ArrayList<ComponentStatus> componentStatuses = new ArrayList<ComponentStatus>();
    ComponentStatus componentStatus1 = new ComponentStatus();
    componentStatus1.setClusterName(clusterName);
    componentStatus1.setServiceName(serviceName);
    componentStatus1.setMessage("I am ok");
    componentStatus1.setStatus(State.STARTED.name());
    componentStatus1.setComponentName(componentName1);
    componentStatuses.add(componentStatus1);
    hb.setComponentStatus(componentStatuses);

    handler.handleHeartBeat(hb);
    State componentState1 = serviceComponentHost1.getState();
    State componentState2 = serviceComponentHost2.getState();
    assertEquals(State.STARTED, componentState1);
    assertEquals(State.INSTALLED, componentState2);
  }

  @Test
  public void testCommandReport() throws AmbariException {
    String hostname = "host1";
    String clusterName = "cluster1";
    injector.injectMembers(this);
    clusters.addHost(hostname);
    clusters.getHost(hostname).persist();
    clusters.addCluster(clusterName);
    ActionDBAccessor db = injector.getInstance(ActionDBAccessorImpl.class);
    ActionManager am = new ActionManager(5000, 1200000, new ActionQueue(), clusters, db);
    populateActionDB(db, hostname);
    Stage stage = db.getAllStages(requestId).get(0);
    Assert.assertEquals(stageId, stage.getStageId());
    stage.setHostRoleStatus(hostname, "HBASE_MASTER", HostRoleStatus.QUEUED);
    db.hostRoleScheduled(stage, hostname, "HBASE_MASTER");
    List<CommandReport> reports = new ArrayList<CommandReport>();
    CommandReport cr = new CommandReport();
    cr.setActionId(StageUtils.getActionId(requestId, stageId));
    cr.setTaskId(1);
    cr.setRole("HBASE_MASTER");
    cr.setStatus("COMPLETED");
    cr.setStdErr("");
    cr.setStdOut("");
    cr.setExitCode(215);
    reports.add(cr);
    am.processTaskResponse(hostname, reports);
    assertEquals(215,
            am.getAction(requestId, stageId).getExitCode(hostname, "HBASE_MASTER"));
    assertEquals(HostRoleStatus.COMPLETED, am.getAction(requestId, stageId)
            .getHostRoleStatus(hostname, "HBASE_MASTER"));
    Stage s = db.getAllStages(requestId).get(0);
    assertEquals(HostRoleStatus.COMPLETED,
            s.getHostRoleStatus(hostname, "HBASE_MASTER"));
    assertEquals(215,
            s.getExitCode(hostname, "HBASE_MASTER"));
  }

  private void populateActionDB(ActionDBAccessor db, String hostname) {
    Stage s = new Stage(requestId, "/a/b", "cluster1");
    s.setStageId(stageId);
    s.addHostRoleExecutionCommand(hostname, Role.HBASE_MASTER,
        RoleCommand.START,
        new ServiceComponentHostStartEvent(Role.HBASE_MASTER.toString(),
            hostname, System.currentTimeMillis(),
            new HashMap<String, String>()), "cluster1", "HBASE");
    List<Stage> stages = new ArrayList<Stage>();
    stages.add(s);
    db.persistActions(stages);
  }

  @Test
  public void testRegistration() throws AmbariException,
      InvalidStateTransitionException {
    ActionManager am = new ActionManager(0, 0, null, null,
        new ActionDBInMemoryImpl());
    Clusters fsm = clusters;
    String hostname = "host1";
    HeartBeatHandler handler = new HeartBeatHandler(fsm, new ActionQueue(), am,
        injector);
    clusters.addHost(hostname);
    Host hostObject = clusters.getHost(hostname);
    hostObject.setIPv4("ipv4");
    hostObject.setIPv6("ipv6");

    Register reg = new Register();
    HostInfo hi = new HostInfo();
    hi.setHostName("host1");
    hi.setOS("centos5");
    reg.setHostname(hostname);
    reg.setHardwareProfile(hi);
    handler.handleRegistration(reg);
    assertEquals(hostObject.getState(), HostState.HEALTHY);
    assertEquals("centos5", hostObject.getOsType());
    assertTrue(hostObject.getLastRegistrationTime() != 0);
    assertEquals(hostObject.getLastHeartbeatTime(),
        hostObject.getLastRegistrationTime());
  }
  
  @Test
  public void testRegistrationPublicHostname() throws AmbariException, InvalidStateTransitionException {
    ActionManager am = new ActionManager(0, 0, null, null,
        new ActionDBInMemoryImpl());
    Clusters fsm = clusters;
    String hostname = "host1";
    HeartBeatHandler handler = new HeartBeatHandler(fsm, new ActionQueue(), am,
        injector);
    clusters.addHost(hostname);
    Host hostObject = clusters.getHost(hostname);
    hostObject.setIPv4("ipv4");
    hostObject.setIPv6("ipv6");

    Register reg = new Register();
    HostInfo hi = new HostInfo();
    hi.setHostName("host1");
    hi.setOS("centos5");
    reg.setHostname(hostname);
    reg.setHardwareProfile(hi);
    reg.setPublicHostname(hostname + "-public");
    handler.handleRegistration(reg);
    assertEquals(hostObject.getState(), HostState.HEALTHY);
    assertEquals("centos5", hostObject.getOsType());
    assertTrue(hostObject.getLastRegistrationTime() != 0);
    assertEquals(hostObject.getLastHeartbeatTime(),
        hostObject.getLastRegistrationTime());
    
    Host verifyHost = clusters.getHost(hostname);
    assertEquals(verifyHost.getPublicHostName(), reg.getPublicHostname());
  }
  

  @Test
  public void testInvalidOSRegistration() throws AmbariException,
      InvalidStateTransitionException {
    ActionManager am = new ActionManager(0, 0, null, null,
        new ActionDBInMemoryImpl());
    Clusters fsm = clusters;
    String hostname = "host1";
    HeartBeatHandler handler = new HeartBeatHandler(fsm, new ActionQueue(), am,
        injector);
    clusters.addHost(hostname);
    Host hostObject = clusters.getHost(hostname);
    hostObject.setIPv4("ipv4");
    hostObject.setIPv6("ipv6");

    Register reg = new Register();
    HostInfo hi = new HostInfo();
    hi.setHostName("host1");
    hi.setOS("MegaOperatingSystem");
    reg.setHostname(hostname);
    reg.setHardwareProfile(hi);
    try {
      handler.handleRegistration(reg);
      fail ("Expected failure for non matching os type");
    } catch (AmbariException e) {
      // Expected
    }
  }


  @Test
  public void testRegisterNewNode()
      throws AmbariException, InvalidStateTransitionException {
    ActionManager am = new ActionManager(0, 0, null, null,
        new ActionDBInMemoryImpl());
    Clusters fsm = clusters;
    String hostname = "host1";
    fsm.addHost(hostname);
    Host hostObject = clusters.getHost(hostname);
    hostObject.setIPv4("ipv4");
    hostObject.setIPv6("ipv6");

    HeartBeatHandler handler = new HeartBeatHandler(fsm, new ActionQueue(), am,
        injector);
    Register reg = new Register();
    HostInfo hi = new HostInfo();
    hi.setHostName("host1");
    hi.setOS("redhat5");
    reg.setHostname(hostname);
    reg.setHardwareProfile(hi);
    RegistrationResponse response = handler.handleRegistration(reg);

    assertEquals(hostObject.getState(), HostState.HEALTHY);
    assertEquals("redhat5", hostObject.getOsType());
    assertEquals(RegistrationStatus.OK, response.getResponseStatus());
    assertEquals(0, response.getResponseId());
    assertTrue(response.getStatusCommands().isEmpty());
  }

  @Test
  public void testRequestId() throws IOException,
      InvalidStateTransitionException, JsonGenerationException, JAXBException {
    HeartBeatHandler heartBeatHandler = injector.getInstance(
        HeartBeatHandler.class);

    Register register = new Register();
    register.setHostname("newHost");
    register.setTimestamp(new Date().getTime());
    register.setResponseId(123);
    HostInfo hi = new HostInfo();
    hi.setHostName("host1");
    hi.setOS("redhat5");
    register.setHardwareProfile(hi);
    RegistrationResponse registrationResponse = heartBeatHandler.handleRegistration(register);

    assertEquals("ResponseId should start from zero", 0L, registrationResponse.getResponseId());

    HeartBeat heartBeat = constructHeartBeat("newHost", registrationResponse.getResponseId(), Status.HEALTHY);
    HeartBeatResponse hbResponse = heartBeatHandler.handleHeartBeat(heartBeat);

    assertEquals("responseId was not incremented", 1L, hbResponse.getResponseId());
    assertTrue("Not cached response returned", hbResponse == heartBeatHandler.handleHeartBeat(heartBeat));

    heartBeat.setResponseId(1L);
    hbResponse = heartBeatHandler.handleHeartBeat(heartBeat);
    assertEquals("responseId was not incremented", 2L, hbResponse.getResponseId());
    assertFalse("Agent is flagged for restart", hbResponse.isRestartAgent());

    log.debug(StageUtils.jaxbToString(hbResponse));

    heartBeat.setResponseId(20L);
    hbResponse = heartBeatHandler.handleHeartBeat(heartBeat);
//    assertEquals("responseId was not incremented", 2L, hbResponse.getResponseId());
    assertTrue("Agent is not flagged for restart", hbResponse.isRestartAgent());

    log.debug(StageUtils.jaxbToString(hbResponse));

  }

  private HeartBeat constructHeartBeat(String hostName, long responseId, Status status) {
    HeartBeat heartBeat = new HeartBeat();
    heartBeat.setHostname(hostName);
    heartBeat.setTimestamp(new Date().getTime());
    heartBeat.setResponseId(responseId);
    HostStatus hs = new HostStatus();
    hs.setCause("");
    hs.setStatus(status);
    heartBeat.setNodeStatus(hs);
    heartBeat.setReports(Collections.<CommandReport>emptyList());

    return heartBeat;
  }

  @Test
  public void testStateCommandsAtRegistration() throws AmbariException, InvalidStateTransitionException {
    List<StatusCommand> dummyCmds = new ArrayList<StatusCommand>();
    StatusCommand statusCmd1 = new StatusCommand();
    statusCmd1.setClusterName("Cluster");
    statusCmd1.setServiceName("HDFS");
    dummyCmds.add(statusCmd1);
    HeartbeatMonitor hm = mock(HeartbeatMonitor.class);
    when(hm.generateStatusCommands(anyString())).thenReturn(dummyCmds);

    ActionManager am = new ActionManager(0, 0, null, null,
            new ActionDBInMemoryImpl());
    Clusters fsm = clusters;
    String hostname = "host1";
    ActionQueue actionQueue = new ActionQueue();
    HeartBeatHandler handler = new HeartBeatHandler(fsm, actionQueue, am,
        injector);
    handler.setHeartbeatMonitor(hm);
    clusters.addHost(hostname);
    Host hostObject = clusters.getHost(hostname);
    hostObject.setIPv4("ipv4");
    hostObject.setIPv6("ipv6");

    Register reg = new Register();
    HostInfo hi = new HostInfo();
    hi.setHostName("host1");
    hi.setOS("redhat5");
    reg.setHostname(hostname);
    reg.setHardwareProfile(hi);
    RegistrationResponse registrationResponse = handler.handleRegistration(reg);
    registrationResponse.getStatusCommands();
    assertTrue(registrationResponse.getStatusCommands().size() == 1);
    assertTrue(registrationResponse.getStatusCommands().get(0).equals(statusCmd1));
  }

  @Test
  public void testTaskInProgressHandling() throws AmbariException, InvalidStateTransitionException {
    ActionManager am = new ActionManager(0, 0, null, null,
            new ActionDBInMemoryImpl());
    final String hostname = "host1";
    String clusterName = "cluster1";
    String serviceName = "HDFS";
    String componentName1 = "DATANODE";
    String componentName2 = "NAMENODE";

    clusters.addHost(hostname);
    clusters.getHost(hostname).setOsType("centos5");
    clusters.getHost(hostname).persist();
    clusters.addCluster(clusterName);

    Cluster cluster = clusters.getCluster(clusterName);
    cluster.setDesiredStackVersion(new StackId("HDP-0.1"));

    @SuppressWarnings("serial")
    Set<String> hostNames = new HashSet<String>(){{
      add(hostname);
    }};
    clusters.mapHostsToCluster(hostNames, clusterName);
    Service hdfs = cluster.addService(serviceName);
    hdfs.persist();
    hdfs.addServiceComponent(Role.DATANODE.name()).persist();
    hdfs.getServiceComponent(Role.DATANODE.name()).addServiceComponentHost(hostname).persist();
    hdfs.addServiceComponent(Role.NAMENODE.name()).persist();
    hdfs.getServiceComponent(Role.NAMENODE.name()).addServiceComponentHost(hostname).persist();
    hdfs.addServiceComponent(Role.SECONDARY_NAMENODE.name()).persist();
    hdfs.getServiceComponent(Role.SECONDARY_NAMENODE.name()).addServiceComponentHost(hostname).persist();

    ActionQueue aq = new ActionQueue();
    HeartBeatHandler handler = new HeartBeatHandler(clusters, aq, am, injector);

    Register reg = new Register();
    HostInfo hi = new HostInfo();
    hi.setHostName("host1");
    hi.setOS("CentOS");
    hi.setOSRelease("5.8");
    reg.setHostname(hostname);
    reg.setResponseId(0);
    reg.setHardwareProfile(hi);
    handler.handleRegistration(reg);

    ServiceComponentHost serviceComponentHost1 = clusters.getCluster(clusterName).getService(serviceName).
            getServiceComponent(componentName1).getServiceComponentHost(hostname);
    ServiceComponentHost serviceComponentHost2 = clusters.getCluster(clusterName).getService(serviceName).
            getServiceComponent(componentName2).getServiceComponentHost(hostname);
    serviceComponentHost1.setState(State.INSTALLING);


    HeartBeat hb = new HeartBeat();
    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(0);
    hb.setHostname(hostname);
    hb.setNodeStatus(new HostStatus(Status.HEALTHY, "I am ok"));

    List<CommandReport> reports = new ArrayList<CommandReport>();
    CommandReport cr = new CommandReport();
    cr.setActionId(StageUtils.getActionId(requestId, stageId));
    cr.setTaskId(1);
    cr.setClusterName(clusterName);
    cr.setServiceName(serviceName);
    cr.setRole(componentName1);
    cr.setStatus("IN_PROGRESS");
    cr.setStdErr("none");
    cr.setStdOut("dummy output");
    cr.setExitCode(777);
    reports.add(cr);
    hb.setReports(reports);
    hb.setComponentStatus(new ArrayList<ComponentStatus>());

    handler.handleHeartBeat(hb);
    State componentState1 = serviceComponentHost1.getState();
    assertEquals("Host state should still be installing", State.INSTALLING, componentState1);
  }
}
