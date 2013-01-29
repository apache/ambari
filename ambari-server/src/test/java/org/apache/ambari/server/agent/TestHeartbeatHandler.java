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

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;
import junit.framework.Assert;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.*;
import org.apache.ambari.server.agent.HostStatus.Status;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.HostsMap;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.state.*;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStartEvent;
import org.apache.ambari.server.utils.StageUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.*;

import static org.apache.ambari.server.agent.DummyHeartbeatConstants.HDFS;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.apache.ambari.server.agent.DummyHeartbeatConstants.*;

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
        new ActionDBInMemoryImpl(), new HostsMap((String) null));
    Clusters fsm = clusters;
    fsm.addHost(DummyHostname1);
    Host hostObject = clusters.getHost(DummyHostname1);
    hostObject.setIPv4("ipv4");
    hostObject.setIPv6("ipv6");
    hostObject.setOsType(DummyOsType);

    ActionQueue aq = new ActionQueue();

    HeartBeatHandler handler = new HeartBeatHandler(fsm, aq, am, injector);
    Register reg = new Register();
    HostInfo hi = new HostInfo();
    hi.setHostName(DummyHostname1);
    hi.setOS(DummyOs);
    hi.setOSRelease(DummyOSRelease);
    reg.setHostname(DummyHostname1);
    reg.setHardwareProfile(hi);
    handler.handleRegistration(reg);

    hostObject.setState(HostState.UNHEALTHY);

    ExecutionCommand execCmd = new ExecutionCommand();
    execCmd.setCommandId("2-34");
    execCmd.setHostname(DummyHostname1);
    aq.enqueue(DummyHostname1, new ExecutionCommand());
    HeartBeat hb = new HeartBeat();
    hb.setResponseId(0);
    hb.setNodeStatus(new HostStatus(Status.HEALTHY, DummyHostStatus));
    hb.setHostname(DummyHostname1);

    handler.handleHeartBeat(hb);
    assertEquals(HostState.HEALTHY, hostObject.getState());
    assertEquals(0, aq.dequeueAll(DummyHostname1).size());
  }

  @Test
  public void testStatusHeartbeat() throws Exception {
    ActionManager am = new ActionManager(0, 0, null, null,
            new ActionDBInMemoryImpl(), new HostsMap((String) null));

    clusters.addHost(DummyHostname1);
    clusters.getHost(DummyHostname1).setOsType(DummyOsType);
    clusters.getHost(DummyHostname1).persist();
    clusters.addCluster(DummyCluster);

    Cluster cluster = clusters.getCluster(DummyCluster);
    cluster.setDesiredStackVersion(new StackId(DummyStackId));

    @SuppressWarnings("serial")
    Set<String> hostNames = new HashSet<String>(){{
      add(DummyHostname1);
    }};
    clusters.mapHostsToCluster(hostNames, DummyCluster);
    Service hdfs = cluster.addService(HDFS);
    hdfs.persist();
    hdfs.addServiceComponent(DATANODE).persist();
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1).persist();
    hdfs.addServiceComponent(NAMENODE).persist();
    hdfs.getServiceComponent(NAMENODE).addServiceComponentHost(DummyHostname1).persist();
    hdfs.addServiceComponent(SECONDARY_NAMENODE).persist();
    hdfs.getServiceComponent(SECONDARY_NAMENODE).addServiceComponentHost(DummyHostname1).persist();

    ActionQueue aq = new ActionQueue();
    HeartBeatHandler handler = new HeartBeatHandler(clusters, aq, am, injector);

    Register reg = new Register();
    HostInfo hi = new HostInfo();
    hi.setHostName(DummyHostname1);
    hi.setOS(DummyOs);
    hi.setOSRelease(DummyOSRelease);
    reg.setHostname(DummyHostname1);
    reg.setResponseId(0);
    reg.setHardwareProfile(hi);
    handler.handleRegistration(reg);

    ServiceComponentHost serviceComponentHost1 = clusters.getCluster(DummyCluster).getService(HDFS).
            getServiceComponent(DATANODE).getServiceComponentHost(DummyHostname1);
    ServiceComponentHost serviceComponentHost2 = clusters.getCluster(DummyCluster).getService(HDFS).
            getServiceComponent(NAMENODE).getServiceComponentHost(DummyHostname1);
    serviceComponentHost1.setState(State.INSTALLED);
    serviceComponentHost2.setState(State.INSTALLED);


    HeartBeat hb = new HeartBeat();
    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(0);
    hb.setHostname(DummyHostname1);
    hb.setNodeStatus(new HostStatus(Status.HEALTHY, DummyHostStatus));
    hb.setReports(new ArrayList<CommandReport>());
    ArrayList<ComponentStatus> componentStatuses = new ArrayList<ComponentStatus>();
    ComponentStatus componentStatus1 = new ComponentStatus();
    componentStatus1.setClusterName(DummyCluster);
    componentStatus1.setServiceName(HDFS);
    componentStatus1.setMessage(DummyHostStatus);
    componentStatus1.setStatus(State.STARTED.name());
    componentStatus1.setComponentName(DATANODE);
    componentStatuses.add(componentStatus1);
    hb.setComponentStatus(componentStatuses);

    handler.handleHeartBeat(hb);
    State componentState1 = serviceComponentHost1.getState();
    State componentState2 = serviceComponentHost2.getState();
    assertEquals(State.STARTED, componentState1);
    assertEquals(State.INSTALLED, componentState2);
  }

  @Test
  public void testStartFailedStatusHeartbeat() throws Exception {
    ActionManager am = new ActionManager(0, 0, null, null,
            new ActionDBInMemoryImpl(), new HostsMap((String) null));

    clusters.addHost(DummyHostname1);
    clusters.getHost(DummyHostname1).setOsType(DummyOsType);
    clusters.getHost(DummyHostname1).persist();
    clusters.addCluster(DummyCluster);

    Cluster cluster = clusters.getCluster(DummyCluster);
    cluster.setDesiredStackVersion(new StackId(DummyStackId));

    @SuppressWarnings("serial")
    Set<String> hostNames = new HashSet<String>(){{
      add(DummyHostname1);
    }};
    clusters.mapHostsToCluster(hostNames, DummyCluster);
    Service hdfs = cluster.addService(HDFS);
    hdfs.persist();
    hdfs.addServiceComponent(DATANODE).persist();
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1).persist();
    hdfs.addServiceComponent(NAMENODE).persist();
    hdfs.getServiceComponent(NAMENODE).addServiceComponentHost(DummyHostname1).persist();
    hdfs.addServiceComponent(SECONDARY_NAMENODE).persist();
    hdfs.getServiceComponent(SECONDARY_NAMENODE).addServiceComponentHost(DummyHostname1).persist();

    ActionQueue aq = new ActionQueue();
    HeartBeatHandler handler = new HeartBeatHandler(clusters, aq, am, injector);

    Register reg = new Register();
    HostInfo hi = new HostInfo();
    hi.setHostName(DummyHostname1);
    hi.setOS(DummyOs);
    hi.setOSRelease(DummyOSRelease);
    reg.setHostname(DummyHostname1);
    reg.setResponseId(0);
    reg.setHardwareProfile(hi);
    handler.handleRegistration(reg);

    ServiceComponentHost serviceComponentHost1 = clusters.getCluster(DummyCluster).getService(HDFS).
            getServiceComponent(DATANODE).getServiceComponentHost(DummyHostname1);
    ServiceComponentHost serviceComponentHost2 = clusters.getCluster(DummyCluster).getService(HDFS).
            getServiceComponent(NAMENODE).getServiceComponentHost(DummyHostname1);
    ServiceComponentHost serviceComponentHost3 = clusters.getCluster(DummyCluster).getService(HDFS).
            getServiceComponent(SECONDARY_NAMENODE).getServiceComponentHost(DummyHostname1);
    serviceComponentHost1.setState(State.INSTALLED);
    serviceComponentHost2.setState(State.START_FAILED);
    serviceComponentHost3.setState(State.STARTED);

    HeartBeat hb = new HeartBeat();
    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(0);
    hb.setHostname(DummyHostname1);
    hb.setNodeStatus(new HostStatus(Status.HEALTHY, DummyHostStatus));
    hb.setReports(new ArrayList<CommandReport>());
    ArrayList<ComponentStatus> componentStatuses = new ArrayList<ComponentStatus>();
    ComponentStatus componentStatus1 = new ComponentStatus();
    componentStatus1.setClusterName(DummyCluster);
    componentStatus1.setServiceName(HDFS);
    componentStatus1.setMessage(DummyHostStatus);
    componentStatus1.setStatus(State.START_FAILED.name());
    componentStatus1.setComponentName(DATANODE);
    componentStatuses.add(componentStatus1);

    ComponentStatus componentStatus2 = new ComponentStatus();
    componentStatus2.setClusterName(DummyCluster);
    componentStatus2.setServiceName(HDFS);
    componentStatus2.setMessage(DummyHostStatus);
    componentStatus2.setStatus(State.INSTALLED.name());
    componentStatus2.setComponentName(NAMENODE);
    componentStatuses.add(componentStatus2);

    ComponentStatus componentStatus3 = new ComponentStatus();
    componentStatus3.setClusterName(DummyCluster);
    componentStatus3.setServiceName(HDFS);
    componentStatus3.setMessage(DummyHostStatus);
    componentStatus3.setStatus(State.INSTALLED.name());
    componentStatus3.setComponentName(SECONDARY_NAMENODE);
    componentStatuses.add(componentStatus3);

    hb.setComponentStatus(componentStatuses);

    handler.handleHeartBeat(hb);
    State componentState1 = serviceComponentHost1.getState();
    State componentState2 = serviceComponentHost2.getState();
    State componentState3 = serviceComponentHost3.getState();
    assertEquals(State.START_FAILED, componentState1);
    assertEquals(State.START_FAILED, componentState2);
    assertEquals(State.INSTALLED, componentState3);
  }

  @Test
  public void testLiveStatusUpdateAfterStopFailed() throws Exception {
    ActionManager am = new ActionManager(0, 0, null, null,
            new ActionDBInMemoryImpl(), new HostsMap((String) null));
    clusters.addHost(DummyHostname1);
    clusters.getHost(DummyHostname1).setOsType(DummyOsType);
    clusters.getHost(DummyHostname1).persist();
    clusters.addCluster(DummyCluster);

    Cluster cluster = clusters.getCluster(DummyCluster);
    cluster.setDesiredStackVersion(new StackId(DummyStackId));

    @SuppressWarnings("serial")
    Set<String> hostNames = new HashSet<String>(){{
      add(DummyHostname1);
    }};
    clusters.mapHostsToCluster(hostNames, DummyCluster);
    Service hdfs = cluster.addService(HDFS);
    hdfs.persist();
    hdfs.addServiceComponent(DATANODE).persist();
    hdfs.getServiceComponent(DATANODE).
            addServiceComponentHost(DummyHostname1).persist();
    hdfs.addServiceComponent(NAMENODE).persist();
    hdfs.getServiceComponent(NAMENODE).
            addServiceComponentHost(DummyHostname1).persist();

    ActionQueue aq = new ActionQueue();
    HeartBeatHandler handler = new HeartBeatHandler(clusters, aq, am, injector);

    Register reg = new Register();
    HostInfo hi = new HostInfo();
    hi.setHostName(DummyHostname1);
    hi.setOS(DummyOs);
    hi.setOSRelease(DummyOSRelease);
    reg.setHostname(DummyHostname1);
    reg.setResponseId(0);
    reg.setHardwareProfile(hi);
    handler.handleRegistration(reg);

    ServiceComponentHost serviceComponentHost1 = clusters.
            getCluster(DummyCluster).getService(HDFS).
            getServiceComponent(DATANODE).
            getServiceComponentHost(DummyHostname1);
    ServiceComponentHost serviceComponentHost2 = clusters.
            getCluster(DummyCluster).getService(HDFS).
            getServiceComponent(NAMENODE).
            getServiceComponentHost(DummyHostname1);
    serviceComponentHost1.setState(State.STOP_FAILED);
    serviceComponentHost2.setState(State.STOP_FAILED);

    HeartBeat hb = new HeartBeat();
    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(0);
    hb.setHostname(DummyHostname1);
    hb.setNodeStatus(new HostStatus(Status.HEALTHY, DummyHostStatus));
    hb.setReports(new ArrayList<CommandReport>());
    ArrayList<ComponentStatus> componentStatuses = new ArrayList<ComponentStatus>();

    ComponentStatus componentStatus1 = new ComponentStatus();
    componentStatus1.setClusterName(DummyCluster);
    componentStatus1.setServiceName(HDFS);
    componentStatus1.setMessage(DummyHostStatus);
    componentStatus1.setStatus(State.STARTED.name());
    componentStatus1.setComponentName(DATANODE);
    componentStatuses.add(componentStatus1);

    ComponentStatus componentStatus2 = new ComponentStatus();
    componentStatus2.setClusterName(DummyCluster);
    componentStatus2.setServiceName(HDFS);
    componentStatus2.setMessage(DummyHostStatus);
    componentStatus2.setStatus(State.INSTALLED.name());
    componentStatus2.setComponentName(NAMENODE);
    componentStatuses.add(componentStatus2);

    hb.setComponentStatus(componentStatuses);

    handler.handleHeartBeat(hb);
    State componentState1 = serviceComponentHost1.getState();
    State componentState2 = serviceComponentHost2.getState();
    assertEquals(State.STARTED, componentState1);
    assertEquals(State.INSTALLED, componentState2);
  }

  @Test
  public void testCommandReport() throws AmbariException {
    injector.injectMembers(this);
    clusters.addHost(DummyHostname1);
    clusters.getHost(DummyHostname1).persist();
    clusters.addCluster(DummyCluster);
    ActionDBAccessor db = injector.getInstance(ActionDBAccessorImpl.class);
    ActionManager am = new ActionManager(5000, 1200000, new ActionQueue(), clusters, db,
        new HostsMap((String) null));
    populateActionDB(db, DummyHostname1);
    Stage stage = db.getAllStages(requestId).get(0);
    Assert.assertEquals(stageId, stage.getStageId());
    stage.setHostRoleStatus(DummyHostname1, HBASE_MASTER, HostRoleStatus.QUEUED);
    db.hostRoleScheduled(stage, DummyHostname1, HBASE_MASTER);
    List<CommandReport> reports = new ArrayList<CommandReport>();
    CommandReport cr = new CommandReport();
    cr.setActionId(StageUtils.getActionId(requestId, stageId));
    cr.setTaskId(1);
    cr.setRole(HBASE_MASTER);
    cr.setStatus("COMPLETED");
    cr.setStdErr("");
    cr.setStdOut("");
    cr.setExitCode(215);
    reports.add(cr);
    am.processTaskResponse(DummyHostname1, reports);
    assertEquals(215,
            am.getAction(requestId, stageId).getExitCode(DummyHostname1, HBASE_MASTER));
    assertEquals(HostRoleStatus.COMPLETED, am.getAction(requestId, stageId)
            .getHostRoleStatus(DummyHostname1, HBASE_MASTER));
    Stage s = db.getAllStages(requestId).get(0);
    assertEquals(HostRoleStatus.COMPLETED,
            s.getHostRoleStatus(DummyHostname1, HBASE_MASTER));
    assertEquals(215,
            s.getExitCode(DummyHostname1, HBASE_MASTER));
  }

  private void populateActionDB(ActionDBAccessor db, String DummyHostname1) {
    Stage s = new Stage(requestId, "/a/b", DummyCluster);
    s.setStageId(stageId);
    String filename = null;
    s.addHostRoleExecutionCommand(DummyHostname1, Role.HBASE_MASTER,
        RoleCommand.START,
        new ServiceComponentHostStartEvent(Role.HBASE_MASTER.toString(),
            DummyHostname1, System.currentTimeMillis(),
            new HashMap<String, String>()), DummyCluster, HBASE);
    List<Stage> stages = new ArrayList<Stage>();
    stages.add(s);
    db.persistActions(stages);
  }

  @Test
  public void testRegistration() throws AmbariException,
      InvalidStateTransitionException {
    ActionManager am = new ActionManager(0, 0, null, null,
        new ActionDBInMemoryImpl(), new HostsMap((String) null));
    Clusters fsm = clusters;
    HeartBeatHandler handler = new HeartBeatHandler(fsm, new ActionQueue(), am,
        injector);
    clusters.addHost(DummyHostname1);
    Host hostObject = clusters.getHost(DummyHostname1);
    hostObject.setIPv4("ipv4");
    hostObject.setIPv6("ipv6");

    Register reg = new Register();
    HostInfo hi = new HostInfo();
    hi.setHostName(DummyHostname1);
    hi.setOS(DummyOsType);
    reg.setHostname(DummyHostname1);
    reg.setHardwareProfile(hi);
    handler.handleRegistration(reg);
    assertEquals(hostObject.getState(), HostState.HEALTHY);
    assertEquals(DummyOsType, hostObject.getOsType());
    assertTrue(hostObject.getLastRegistrationTime() != 0);
    assertEquals(hostObject.getLastHeartbeatTime(),
        hostObject.getLastRegistrationTime());
  }
  
  @Test
  public void testRegistrationPublicHostname() throws AmbariException, InvalidStateTransitionException {
    ActionManager am = new ActionManager(0, 0, null, null,
        new ActionDBInMemoryImpl(), new HostsMap((String) null));
    Clusters fsm = clusters;
    HeartBeatHandler handler = new HeartBeatHandler(fsm, new ActionQueue(), am,
        injector);
    clusters.addHost(DummyHostname1);
    Host hostObject = clusters.getHost(DummyHostname1);
    hostObject.setIPv4("ipv4");
    hostObject.setIPv6("ipv6");

    Register reg = new Register();
    HostInfo hi = new HostInfo();
    hi.setHostName(DummyHostname1);
    hi.setOS(DummyOsType);
    reg.setHostname(DummyHostname1);
    reg.setHardwareProfile(hi);
    reg.setPublicHostname(DummyHostname1 + "-public");
    handler.handleRegistration(reg);
    assertEquals(hostObject.getState(), HostState.HEALTHY);
    assertEquals(DummyOsType, hostObject.getOsType());
    assertTrue(hostObject.getLastRegistrationTime() != 0);
    assertEquals(hostObject.getLastHeartbeatTime(),
        hostObject.getLastRegistrationTime());
    
    Host verifyHost = clusters.getHost(DummyHostname1);
    assertEquals(verifyHost.getPublicHostName(), reg.getPublicHostname());
  }
  

  @Test
  public void testInvalidOSRegistration() throws AmbariException,
      InvalidStateTransitionException {
    ActionManager am = new ActionManager(0, 0, null, null,
        new ActionDBInMemoryImpl(), new HostsMap((String) null));
    Clusters fsm = clusters;
    HeartBeatHandler handler = new HeartBeatHandler(fsm, new ActionQueue(), am,
        injector);
    clusters.addHost(DummyHostname1);
    Host hostObject = clusters.getHost(DummyHostname1);
    hostObject.setIPv4("ipv4");
    hostObject.setIPv6("ipv6");

    Register reg = new Register();
    HostInfo hi = new HostInfo();
    hi.setHostName(DummyHostname1);
    hi.setOS("MegaOperatingSystem");
    reg.setHostname(DummyHostname1);
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
        new ActionDBInMemoryImpl(), new HostsMap((String) null));
    Clusters fsm = clusters;
    fsm.addHost(DummyHostname1);
    Host hostObject = clusters.getHost(DummyHostname1);
    hostObject.setIPv4("ipv4");
    hostObject.setIPv6("ipv6");

    HeartBeatHandler handler = new HeartBeatHandler(fsm, new ActionQueue(), am,
        injector);
    Register reg = new Register();
    HostInfo hi = new HostInfo();
    hi.setHostName(DummyHostname1);
    hi.setOS("redhat5");
    reg.setHostname(DummyHostname1);
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
    hi.setHostName(DummyHostname1);
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
    statusCmd1.setClusterName(DummyCluster);
    statusCmd1.setServiceName(HDFS);
    dummyCmds.add(statusCmd1);
    HeartbeatMonitor hm = mock(HeartbeatMonitor.class);
    when(hm.generateStatusCommands(anyString())).thenReturn(dummyCmds);

    ActionManager am = new ActionManager(0, 0, null, null,
            new ActionDBInMemoryImpl(), new HostsMap((String) null));
    Clusters fsm = clusters;
    ActionQueue actionQueue = new ActionQueue();
    HeartBeatHandler handler = new HeartBeatHandler(fsm, actionQueue, am,
        injector);
    handler.setHeartbeatMonitor(hm);
    clusters.addHost(DummyHostname1);
    Host hostObject = clusters.getHost(DummyHostname1);
    hostObject.setIPv4("ipv4");
    hostObject.setIPv6("ipv6");

    Register reg = new Register();
    HostInfo hi = new HostInfo();
    hi.setHostName(DummyHostname1);
    hi.setOS(DummyOsType);
    reg.setHostname(DummyHostname1);
    reg.setHardwareProfile(hi);
    RegistrationResponse registrationResponse = handler.handleRegistration(reg);
    registrationResponse.getStatusCommands();
    assertTrue(registrationResponse.getStatusCommands().size() == 1);
    assertTrue(registrationResponse.getStatusCommands().get(0).equals(statusCmd1));
  }

  @Test
  public void testTaskInProgressHandling() throws AmbariException, InvalidStateTransitionException {
    ActionManager am = new ActionManager(0, 0, null, null,
            new ActionDBInMemoryImpl(), new HostsMap((String) null));
    clusters.addHost(DummyHostname1);
    clusters.getHost(DummyHostname1).setOsType(DummyOsType);
    clusters.getHost(DummyHostname1).persist();
    clusters.addCluster(DummyCluster);

    Cluster cluster = clusters.getCluster(DummyCluster);
    cluster.setDesiredStackVersion(new StackId(DummyStackId));

    @SuppressWarnings("serial")
    Set<String> hostNames = new HashSet<String>(){{
      add(DummyHostname1);
    }};
    clusters.mapHostsToCluster(hostNames, DummyCluster);
    Service hdfs = cluster.addService(HDFS);
    hdfs.persist();
    hdfs.addServiceComponent(DATANODE).persist();
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1).persist();
    hdfs.addServiceComponent(NAMENODE).persist();
    hdfs.getServiceComponent(NAMENODE).addServiceComponentHost(DummyHostname1).persist();
    hdfs.addServiceComponent(SECONDARY_NAMENODE).persist();
    hdfs.getServiceComponent(SECONDARY_NAMENODE).addServiceComponentHost(DummyHostname1).persist();

    ActionQueue aq = new ActionQueue();
    HeartBeatHandler handler = new HeartBeatHandler(clusters, aq, am, injector);

    Register reg = new Register();
    HostInfo hi = new HostInfo();
    hi.setHostName(DummyHostname1);
    hi.setOS(DummyOs);
    hi.setOSRelease(DummyOSRelease);
    reg.setHostname(DummyHostname1);
    reg.setResponseId(0);
    reg.setHardwareProfile(hi);
    handler.handleRegistration(reg);

    ServiceComponentHost serviceComponentHost1 = clusters.getCluster(DummyCluster).getService(HDFS).
            getServiceComponent(DATANODE).getServiceComponentHost(DummyHostname1);
    serviceComponentHost1.setState(State.INSTALLING);


    HeartBeat hb = new HeartBeat();
    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(0);
    hb.setHostname(DummyHostname1);
    hb.setNodeStatus(new HostStatus(Status.HEALTHY, DummyHostStatus));

    List<CommandReport> reports = new ArrayList<CommandReport>();
    CommandReport cr = new CommandReport();
    cr.setActionId(StageUtils.getActionId(requestId, stageId));
    cr.setTaskId(1);
    cr.setClusterName(DummyCluster);
    cr.setServiceName(HDFS);
    cr.setRole(DATANODE);
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
