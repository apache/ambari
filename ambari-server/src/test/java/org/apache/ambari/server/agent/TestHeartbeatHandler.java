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

import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DATANODE;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyCluster;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyHostStatus;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyHostname1;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyOSRelease;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyOs;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyOsType;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyStackId;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.HBASE;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.HBASE_MASTER;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.HDFS;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.HDFS_CLIENT;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.NAMENODE;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.SECONDARY_NAMENODE;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyCurrentPingPort;
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
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;

import com.google.inject.persist.UnitOfWork;
import junit.framework.Assert;

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
import org.apache.ambari.server.controller.HostsMap;
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
import org.codehaus.jackson.JsonGenerationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

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
  private UnitOfWork unitOfWork;

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    clusters = injector.getInstance(Clusters.class);
    injector.injectMembers(this);
    metaInfo.init();
    log.debug("Using server os type=" + config.getServerOsType());
    unitOfWork = injector.getInstance(UnitOfWork.class);
  }

  @After
  public void teardown() throws AmbariException {
    injector.getInstance(PersistService.class).stop();
  }

  @Test
  public void testHeartbeat() throws Exception {
    ActionManager am = getMockActionManager();
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
    reg.setAgentVersion(metaInfo.getServerVersion());
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
  public void testHeartbeatWithConfigs() throws Exception {
    ActionManager am = getMockActionManager();

    Cluster cluster = getDummyCluster();
    
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
    HeartBeatHandler handler = getHeartBeatHandler(am, aq);
    
    ServiceComponentHost serviceComponentHost1 = clusters.getCluster(DummyCluster).getService(HDFS).
            getServiceComponent(DATANODE).getServiceComponentHost(DummyHostname1);
    ServiceComponentHost serviceComponentHost2 = clusters.getCluster(DummyCluster).getService(HDFS).
            getServiceComponent(NAMENODE).getServiceComponentHost(DummyHostname1);
    serviceComponentHost1.setState(State.INSTALLED);
    serviceComponentHost2.setState(State.INSTALLED);


    HeartBeat hb = new HeartBeat();
    hb.setResponseId(0);
    hb.setNodeStatus(new HostStatus(Status.HEALTHY, DummyHostStatus));
    hb.setHostname(DummyHostname1);
    
    List<CommandReport> reports = new ArrayList<CommandReport>();
    CommandReport cr = new CommandReport();
    cr.setActionId(StageUtils.getActionId(requestId, stageId));
    cr.setServiceName(HDFS);
    cr.setTaskId(1);
    cr.setRole(DATANODE);
    cr.setStatus("COMPLETED");
    cr.setStdErr("");
    cr.setStdOut("");
    cr.setExitCode(215);
    cr.setRoleCommand("START");
    cr.setClusterName(DummyCluster);
    
    cr.setConfigurationTags(new HashMap<String, Map<String,String>>() {{
      put("global", new HashMap<String,String>() {{ put("tag", "version1"); }});
    }});
    
    reports.add(cr);
    hb.setReports(reports);
    
    handler.handleHeartBeat(hb);

    // the heartbeat test passed if actual configs is populated
    Assert.assertNotNull(serviceComponentHost1.getActualConfigs());
    Assert.assertEquals(serviceComponentHost1.getActualConfigs().size(), 1);
  }

  @Test
  public void testStatusHeartbeat() throws Exception {
    ActionManager am = getMockActionManager();

    Cluster cluster = getDummyCluster();

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
    HeartBeatHandler handler = getHeartBeatHandler(am, aq);

    ServiceComponentHost serviceComponentHost1 = clusters.getCluster(DummyCluster).getService(HDFS).
            getServiceComponent(DATANODE).getServiceComponentHost(DummyHostname1);
    ServiceComponentHost serviceComponentHost2 = clusters.getCluster(DummyCluster).getService(HDFS).
            getServiceComponent(NAMENODE).getServiceComponentHost(DummyHostname1);
    ServiceComponentHost serviceComponentHost3 = clusters.getCluster(DummyCluster).getService(HDFS).
        getServiceComponent(SECONDARY_NAMENODE).getServiceComponentHost(DummyHostname1);
    serviceComponentHost1.setState(State.INSTALLED);
    serviceComponentHost2.setState(State.INSTALLED);
    serviceComponentHost3.setState(State.STARTING);

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
    componentStatus2.setStatus(State.STARTED.name());
    componentStatus2.setComponentName(SECONDARY_NAMENODE);
    componentStatuses.add(componentStatus2);
    hb.setComponentStatus(componentStatuses);

    handler.handleHeartBeat(hb);
    State componentState1 = serviceComponentHost1.getState();
    State componentState2 = serviceComponentHost2.getState();
    State componentState3 = serviceComponentHost3.getState();
    assertEquals(State.STARTED, componentState1);
    assertEquals(State.INSTALLED, componentState2);
    assertEquals(State.STARTED, componentState3);
  }

  @Test
  public void testStatusHeartbeatWithAnnotation() throws Exception {
    ActionManager am = getMockActionManager();

    Cluster cluster = getDummyCluster();

    @SuppressWarnings("serial")
    Set<String> hostNames = new HashSet<String>(){{
      add(DummyHostname1);
    }};
    clusters.mapHostsToCluster(hostNames, DummyCluster);
    Service hdfs = cluster.addService(HDFS);
    hdfs.persist();
    hdfs.addServiceComponent(DATANODE).persist();
    hdfs.addServiceComponent(NAMENODE).persist();
    hdfs.addServiceComponent(SECONDARY_NAMENODE).persist();

    ActionQueue aq = new ActionQueue();
    HeartBeatHandler handler = getHeartBeatHandler(am, aq);

    HeartBeat hb = new HeartBeat();
    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(0);
    hb.setHostname(DummyHostname1);
    hb.setNodeStatus(new HostStatus(Status.HEALTHY, DummyHostStatus));
    hb.setReports(new ArrayList<CommandReport>());
    ArrayList<ComponentStatus> componentStatuses = new ArrayList<ComponentStatus>();
    hb.setComponentStatus(componentStatuses);

    HeartBeatResponse resp = handler.handleHeartBeat(hb);
    Assert.assertFalse(resp.hasMappedComponents());

    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1).persist();
    ServiceComponentHost serviceComponentHost1 = clusters.getCluster(DummyCluster).getService(HDFS).
        getServiceComponent(DATANODE).getServiceComponentHost(DummyHostname1);
    serviceComponentHost1.setState(State.INIT);

    hb = new HeartBeat();
    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(1);
    hb.setHostname(DummyHostname1);
    hb.setNodeStatus(new HostStatus(Status.HEALTHY, DummyHostStatus));
    hb.setReports(new ArrayList<CommandReport>());
    hb.setComponentStatus(componentStatuses);

    resp = handler.handleHeartBeat(hb);
    Assert.assertTrue(resp.hasMappedComponents());
  }

  @Test
  public void testLiveStatusUpdateAfterStopFailed() throws Exception {
    ActionManager am = getMockActionManager();
    Cluster cluster = getDummyCluster();

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
    HeartBeatHandler handler = getHeartBeatHandler(am, aq);

    ServiceComponentHost serviceComponentHost1 = clusters.
            getCluster(DummyCluster).getService(HDFS).
            getServiceComponent(DATANODE).
            getServiceComponentHost(DummyHostname1);
    ServiceComponentHost serviceComponentHost2 = clusters.
            getCluster(DummyCluster).getService(HDFS).
            getServiceComponent(NAMENODE).
            getServiceComponentHost(DummyHostname1);
    serviceComponentHost1.setState(State.STARTED);
    serviceComponentHost2.setState(State.STARTED);

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
        new HostsMap((String) null), null, unitOfWork, null);
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
    
    cr.setConfigurationTags(new HashMap<String, Map<String,String>>() {{
        put("global", new HashMap<String,String>() {{ put("tag", "version1"); }});
      }});
    
    
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
    Stage s = new Stage(requestId, "/a/b", DummyCluster, "heartbeat handler test", "clusterHostInfo");
    s.setStageId(stageId);
    String filename = null;
    s.addHostRoleExecutionCommand(DummyHostname1, Role.HBASE_MASTER,
        RoleCommand.START,
        new ServiceComponentHostStartEvent(Role.HBASE_MASTER.toString(),
            DummyHostname1, System.currentTimeMillis()), DummyCluster, HBASE);
    List<Stage> stages = new ArrayList<Stage>();
    stages.add(s);
    db.persistActions(stages);
  }

  @Test
  public void testRegistration() throws AmbariException,
      InvalidStateTransitionException {
    ActionManager am = getMockActionManager();
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
    reg.setCurrentPingPort(DummyCurrentPingPort);
    reg.setHardwareProfile(hi);
    reg.setAgentVersion(metaInfo.getServerVersion());
    handler.handleRegistration(reg);
    assertEquals(hostObject.getState(), HostState.HEALTHY);
    assertEquals(DummyOsType, hostObject.getOsType());
    assertEquals(DummyCurrentPingPort, hostObject.getCurrentPingPort());
    assertTrue(hostObject.getLastRegistrationTime() != 0);
    assertEquals(hostObject.getLastHeartbeatTime(),
        hostObject.getLastRegistrationTime());
  }

  @Test
  public void testRegistrationWithBadVersion() throws AmbariException,
      InvalidStateTransitionException {

    ActionManager am = getMockActionManager();
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
    reg.setAgentVersion(""); // Invalid agent version
    try {
      handler.handleRegistration(reg);
      fail ("Expected failure for non compatible agent version");
    } catch (AmbariException e) {
      log.debug("Error:" + e.getMessage());
      Assert.assertTrue(e.getMessage().contains(
          "Cannot register host with non compatible agent version"));
    }

    reg.setAgentVersion(null); // Invalid agent version
    try {
      handler.handleRegistration(reg);
      fail ("Expected failure for non compatible agent version");
    } catch (AmbariException e) {
      log.debug("Error:" + e.getMessage());
      Assert.assertTrue(e.getMessage().contains(
          "Cannot register host with non compatible agent version"));
    }
  }

  @Test
  public void testRegistrationPublicHostname() throws AmbariException, InvalidStateTransitionException {
    ActionManager am = getMockActionManager();
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
    reg.setAgentVersion(metaInfo.getServerVersion());
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
    ActionManager am = getMockActionManager();
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
    reg.setAgentVersion(metaInfo.getServerVersion());
    try {
      handler.handleRegistration(reg);
      fail ("Expected failure for non matching os type");
    } catch (AmbariException e) {
      // Expected
    }
  }

  @Test
  public void testIncompatibleAgentRegistration() throws AmbariException,
          InvalidStateTransitionException {

    ActionManager am = getMockActionManager();
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
    reg.setAgentVersion("0.0.0"); // Invalid agent version
    try {
      handler.handleRegistration(reg);
      fail ("Expected failure for non compatible agent version");
    } catch (AmbariException e) {
      // Expected
    }
  }

  @Test
  public void testRegisterNewNode()
      throws AmbariException, InvalidStateTransitionException {
    ActionManager am = getMockActionManager();
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
    reg.setAgentVersion(metaInfo.getServerVersion());
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
    register.setAgentVersion(metaInfo.getServerVersion());
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

    ActionManager am = getMockActionManager();
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
    reg.setAgentVersion(metaInfo.getServerVersion());
    RegistrationResponse registrationResponse = handler.handleRegistration(reg);
    registrationResponse.getStatusCommands();
    assertTrue(registrationResponse.getStatusCommands().size() == 1);
    assertTrue(registrationResponse.getStatusCommands().get(0).equals(statusCmd1));
  }

  @Test
  public void testTaskInProgressHandling() throws AmbariException, InvalidStateTransitionException {
    ActionManager am = getMockActionManager();
    Cluster cluster = getDummyCluster();

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
    HeartBeatHandler handler = getHeartBeatHandler(am, aq);

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

  /**
   * Tests the fact that when START and STOP commands are in progress, and heartbeat
   * forces the host component state to STARTED or INSTALLED, there are no undesired
   * side effects.
   * @throws AmbariException
   * @throws InvalidStateTransitionException
   */
  @Test
  public void testCommandReportOnHeartbeatUpdatedState()
      throws AmbariException, InvalidStateTransitionException {
    ActionManager am = getMockActionManager();
    Cluster cluster = getDummyCluster();

    @SuppressWarnings("serial")
    Set<String> hostNames = new HashSet<String>() {{
      add(DummyHostname1);
    }};
    clusters.mapHostsToCluster(hostNames, DummyCluster);
    Service hdfs = cluster.addService(HDFS);
    hdfs.persist();
    hdfs.addServiceComponent(DATANODE).persist();
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1).persist();

    ActionQueue aq = new ActionQueue();
    HeartBeatHandler handler = getHeartBeatHandler(am, aq);

    ServiceComponentHost serviceComponentHost1 = clusters.getCluster(DummyCluster).getService(HDFS).
        getServiceComponent(DATANODE).getServiceComponentHost(DummyHostname1);
    serviceComponentHost1.setState(State.INSTALLED);

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
    cr.setStatus(HostRoleStatus.IN_PROGRESS.toString());
    cr.setStdErr("none");
    cr.setStdOut("dummy output");
    cr.setExitCode(777);
    cr.setRoleCommand("START");
    reports.add(cr);
    hb.setReports(reports);
    hb.setComponentStatus(new ArrayList<ComponentStatus>());

    handler.handleHeartBeat(hb);
    assertEquals("Host state should  be " + State.INSTALLED,
        State.INSTALLED, serviceComponentHost1.getState());

    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(1);
    cr.setStatus(HostRoleStatus.COMPLETED.toString());
    cr.setExitCode(0);

    handler.handleHeartBeat(hb);
    assertEquals("Host state should be " + State.STARTED,
        State.STARTED, serviceComponentHost1.getState());

    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(2);
    cr.setStatus(HostRoleStatus.IN_PROGRESS.toString());
    cr.setRoleCommand("STOP");
    cr.setExitCode(777);

    handler.handleHeartBeat(hb);
    assertEquals("Host state should be " + State.STARTED,
        State.STARTED, serviceComponentHost1.getState());

    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(3);
    cr.setStatus(HostRoleStatus.COMPLETED.toString());
    cr.setExitCode(0);

    handler.handleHeartBeat(hb);
    assertEquals("Host state should be " + State.INSTALLED,
        State.INSTALLED, serviceComponentHost1.getState());

    // validate the transitions when there is no heartbeat
    serviceComponentHost1.setState(State.STARTING);
    cr.setStatus(HostRoleStatus.IN_PROGRESS.toString());
    cr.setExitCode(777);
    cr.setRoleCommand("START");
    hb.setResponseId(4);

    handler.handleHeartBeat(hb);
    assertEquals("Host state should be " + State.STARTING,
        State.STARTING, serviceComponentHost1.getState());

    cr.setStatus(HostRoleStatus.COMPLETED.toString());
    cr.setExitCode(0);
    hb.setResponseId(5);

    handler.handleHeartBeat(hb);
    assertEquals("Host state should be " + State.STARTED,
        State.STARTED, serviceComponentHost1.getState());

    serviceComponentHost1.setState(State.STOPPING);
    cr.setStatus(HostRoleStatus.IN_PROGRESS.toString());
    cr.setExitCode(777);
    cr.setRoleCommand("STOP");
    hb.setResponseId(6);

    handler.handleHeartBeat(hb);
    assertEquals("Host state should be " + State.STOPPING,
        State.STOPPING, serviceComponentHost1.getState());

    cr.setStatus(HostRoleStatus.COMPLETED.toString());
    cr.setExitCode(0);
    hb.setResponseId(7);

    handler.handleHeartBeat(hb);
    assertEquals("Host state should be " + State.INSTALLED,
        State.INSTALLED, serviceComponentHost1.getState());
  }

  @Test
  public void testUpgradeSpecificHandling() throws AmbariException, InvalidStateTransitionException {
    ActionManager am = getMockActionManager();
    Cluster cluster = getDummyCluster();

    @SuppressWarnings("serial")
    Set<String> hostNames = new HashSet<String>() {{
      add(DummyHostname1);
    }};
    clusters.mapHostsToCluster(hostNames, DummyCluster);
    Service hdfs = cluster.addService(HDFS);
    hdfs.persist();
    hdfs.addServiceComponent(DATANODE).persist();
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1).persist();

    ActionQueue aq = new ActionQueue();
    HeartBeatHandler handler = getHeartBeatHandler(am, aq);

    ServiceComponentHost serviceComponentHost1 = clusters.getCluster(DummyCluster).getService(HDFS).
        getServiceComponent(DATANODE).getServiceComponentHost(DummyHostname1);
    serviceComponentHost1.setState(State.UPGRADING);

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
    cr.setStatus(HostRoleStatus.IN_PROGRESS.toString());
    cr.setStdErr("none");
    cr.setStdOut("dummy output");
    cr.setExitCode(777);
    reports.add(cr);
    hb.setReports(reports);
    hb.setComponentStatus(new ArrayList<ComponentStatus>());

    handler.handleHeartBeat(hb);
    assertEquals("Host state should  be " + State.UPGRADING,
        State.UPGRADING, serviceComponentHost1.getState());

    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(1);
    cr.setStatus(HostRoleStatus.COMPLETED.toString());
    cr.setExitCode(0);

    handler.handleHeartBeat(hb);
    assertEquals("Host state should be " + State.INSTALLED,
        State.INSTALLED, serviceComponentHost1.getState());

    serviceComponentHost1.setState(State.UPGRADING);
    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(2);
    cr.setStatus(HostRoleStatus.FAILED.toString());
    cr.setExitCode(3);

    handler.handleHeartBeat(hb);
    assertEquals("Host state should be " + State.UPGRADING,
        State.UPGRADING, serviceComponentHost1.getState());

    // TODO What happens when there is a TIMEDOUT

    serviceComponentHost1.setState(State.UPGRADING);
    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(3);
    cr.setStatus(HostRoleStatus.PENDING.toString());
    cr.setExitCode(55);

    handler.handleHeartBeat(hb);
    assertEquals("Host state should be " + State.UPGRADING,
        State.UPGRADING, serviceComponentHost1.getState());

    serviceComponentHost1.setState(State.UPGRADING);
    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(4);
    cr.setStatus(HostRoleStatus.QUEUED.toString());
    cr.setExitCode(55);

    handler.handleHeartBeat(hb);
    assertEquals("Host state should be " + State.UPGRADING,
        State.UPGRADING, serviceComponentHost1.getState());
  }

  @Test
  public void testStatusHeartbeatWithVersion() throws Exception {
    ActionManager am = getMockActionManager();
    Cluster cluster = getDummyCluster();

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
    hdfs.addServiceComponent(HDFS_CLIENT).persist();
    hdfs.getServiceComponent(HDFS_CLIENT).addServiceComponentHost(DummyHostname1).persist();

    ServiceComponentHost serviceComponentHost1 = clusters.getCluster(DummyCluster).getService(HDFS).
        getServiceComponent(DATANODE).getServiceComponentHost(DummyHostname1);
    ServiceComponentHost serviceComponentHost2 = clusters.getCluster(DummyCluster).getService(HDFS).
        getServiceComponent(NAMENODE).getServiceComponentHost(DummyHostname1);
    ServiceComponentHost serviceComponentHost3 = clusters.getCluster(DummyCluster).getService(HDFS).
        getServiceComponent(HDFS_CLIENT).getServiceComponentHost(DummyHostname1);

    StackId stack130 = new StackId("HDP-1.3.0");
    StackId stack122 = new StackId("HDP-1.2.2");

    serviceComponentHost1.setState(State.INSTALLED);
    serviceComponentHost2.setState(State.STARTED);
    serviceComponentHost3.setState(State.STARTED);
    serviceComponentHost1.setStackVersion(stack130);
    serviceComponentHost2.setStackVersion(stack122);
    serviceComponentHost3.setStackVersion(stack122);

    HeartBeat hb = new HeartBeat();
    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(0);
    hb.setHostname(DummyHostname1);
    hb.setNodeStatus(new HostStatus(Status.HEALTHY, DummyHostStatus));
    hb.setReports(new ArrayList<CommandReport>());
    hb.setAgentEnv(new AgentEnv());
    hb.setMounts(new ArrayList<DiskInfo>());

    ArrayList<ComponentStatus> componentStatuses = new ArrayList<ComponentStatus>();
    ComponentStatus componentStatus1 = createComponentStatus(DummyCluster, HDFS, DummyHostStatus, State.STARTED,
        DATANODE, "{\"stackName\":\"HDP\",\"stackVersion\":\"1.3.0\"}");
    ComponentStatus componentStatus2 =
        createComponentStatus(DummyCluster, HDFS, DummyHostStatus, State.STARTED, NAMENODE, "");
    ComponentStatus componentStatus3 = createComponentStatus(DummyCluster, HDFS, DummyHostStatus, State.INSTALLED,
        HDFS_CLIENT, "{\"stackName\":\"HDP\",\"stackVersion\":\"1.3.0\"}");

    componentStatuses.add(componentStatus1);
    componentStatuses.add(componentStatus2);
    componentStatuses.add(componentStatus3);
    hb.setComponentStatus(componentStatuses);

    ActionQueue aq = new ActionQueue();
    HeartBeatHandler handler = getHeartBeatHandler(am, aq);
    handler.handleHeartBeat(hb);
    assertEquals("Matching value " + serviceComponentHost1.getStackVersion(),
        stack130, serviceComponentHost1.getStackVersion());
    assertEquals("Matching value " + serviceComponentHost2.getStackVersion(),
        stack122, serviceComponentHost2.getStackVersion());
    assertEquals("Matching value " + serviceComponentHost3.getStackVersion(),
        stack130, serviceComponentHost3.getStackVersion());
    assertTrue(hb.getAgentEnv().getHostHealth().getServerTimeStampAtReporting() >= hb.getTimestamp());
  }

  @Test
  public void testComponentUpgradeCompleteReport() throws AmbariException, InvalidStateTransitionException {
    ActionManager am = getMockActionManager();
    Cluster cluster = getDummyCluster();

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
    hdfs.addServiceComponent(HDFS_CLIENT).persist();
    hdfs.getServiceComponent(HDFS_CLIENT).addServiceComponentHost(DummyHostname1).persist();

    ServiceComponentHost serviceComponentHost1 = clusters.getCluster(DummyCluster).getService(HDFS).
            getServiceComponent(DATANODE).getServiceComponentHost(DummyHostname1);
    ServiceComponentHost serviceComponentHost2 = clusters.getCluster(DummyCluster).getService(HDFS).
            getServiceComponent(NAMENODE).getServiceComponentHost(DummyHostname1);

    StackId stack130 = new StackId("HDP-1.3.0");
    StackId stack122 = new StackId("HDP-1.2.2");

    serviceComponentHost1.setState(State.UPGRADING);
    serviceComponentHost2.setState(State.INSTALLING);

    serviceComponentHost1.setStackVersion(stack122);
    serviceComponentHost1.setDesiredStackVersion(stack130);
    serviceComponentHost2.setStackVersion(stack122);

    HeartBeat hb = new HeartBeat();
    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(0);
    hb.setHostname(DummyHostname1);
    hb.setNodeStatus(new HostStatus(Status.HEALTHY, DummyHostStatus));
    CommandReport cr1 = new CommandReport();
    cr1.setActionId(StageUtils.getActionId(requestId, stageId));
    cr1.setTaskId(1);
    cr1.setClusterName(DummyCluster);
    cr1.setServiceName(HDFS);
    cr1.setRole(DATANODE);
    cr1.setStatus(HostRoleStatus.COMPLETED.toString());
    cr1.setStdErr("none");
    cr1.setStdOut("dummy output");
    cr1.setExitCode(0);
    cr1.setRoleCommand(RoleCommand.UPGRADE.toString());

    CommandReport cr2 = new CommandReport();
    cr2.setActionId(StageUtils.getActionId(requestId, stageId));
    cr2.setTaskId(2);
    cr2.setClusterName(DummyCluster);
    cr2.setServiceName(HDFS);
    cr2.setRole(NAMENODE);
    cr2.setStatus(HostRoleStatus.COMPLETED.toString());
    cr2.setStdErr("none");
    cr2.setStdOut("dummy output");
    cr2.setExitCode(0);
    cr2.setRoleCommand(RoleCommand.UPGRADE.toString());
    ArrayList<CommandReport> reports = new ArrayList<CommandReport>();
    reports.add(cr1);
    reports.add(cr2);
    hb.setReports(reports);

    ActionQueue aq = new ActionQueue();
    HeartBeatHandler handler = getHeartBeatHandler(am, aq);
    handler.handleHeartBeat(hb);
    assertEquals("Stack version for SCH should be updated to " +
            serviceComponentHost1.getDesiredStackVersion(),
            stack130, serviceComponentHost1.getStackVersion());
    assertEquals("Stack version for SCH should not change ",
            stack122, serviceComponentHost2.getStackVersion());
  }

  @Test
  public void testComponentUpgradeInProgressReport() throws AmbariException, InvalidStateTransitionException {
    ActionManager am = getMockActionManager();
    Cluster cluster = getDummyCluster();

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
    hdfs.addServiceComponent(HDFS_CLIENT).persist();
    hdfs.getServiceComponent(HDFS_CLIENT).addServiceComponentHost(DummyHostname1).persist();

    ServiceComponentHost serviceComponentHost1 = clusters.getCluster(DummyCluster).getService(HDFS).
            getServiceComponent(DATANODE).getServiceComponentHost(DummyHostname1);
    ServiceComponentHost serviceComponentHost2 = clusters.getCluster(DummyCluster).getService(HDFS).
            getServiceComponent(NAMENODE).getServiceComponentHost(DummyHostname1);

    StackId stack130 = new StackId("HDP-1.3.0");
    StackId stack122 = new StackId("HDP-1.2.2");

    serviceComponentHost1.setState(State.UPGRADING);
    serviceComponentHost2.setState(State.INSTALLING);

    serviceComponentHost1.setStackVersion(stack122);
    serviceComponentHost1.setDesiredStackVersion(stack130);
    serviceComponentHost2.setStackVersion(stack122);

    HeartBeat hb = new HeartBeat();
    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(0);
    hb.setHostname(DummyHostname1);
    hb.setNodeStatus(new HostStatus(Status.HEALTHY, DummyHostStatus));
    CommandReport cr1 = new CommandReport();
    cr1.setActionId(StageUtils.getActionId(requestId, stageId));
    cr1.setTaskId(1);
    cr1.setClusterName(DummyCluster);
    cr1.setServiceName(HDFS);
    cr1.setRole(DATANODE);
    cr1.setStatus(HostRoleStatus.IN_PROGRESS.toString());
    cr1.setStdErr("none");
    cr1.setStdOut("dummy output");
    cr1.setExitCode(777);

    CommandReport cr2 = new CommandReport();
    cr2.setActionId(StageUtils.getActionId(requestId, stageId));
    cr2.setTaskId(2);
    cr2.setClusterName(DummyCluster);
    cr2.setServiceName(HDFS);
    cr2.setRole(NAMENODE);
    cr2.setStatus(HostRoleStatus.IN_PROGRESS.toString());
    cr2.setStdErr("none");
    cr2.setStdOut("dummy output");
    cr2.setExitCode(777);
    ArrayList<CommandReport> reports = new ArrayList<CommandReport>();
    reports.add(cr1);
    reports.add(cr2);
    hb.setReports(reports);

    ActionQueue aq = new ActionQueue();
    HeartBeatHandler handler = getHeartBeatHandler(am, aq);
    handler.handleHeartBeat(hb);
    assertEquals("State of SCH not change while operation is in progress",
            State.UPGRADING, serviceComponentHost1.getState());
    assertEquals("Stack version of SCH should not change after in progress report",
            stack130, serviceComponentHost1.getDesiredStackVersion());
    assertEquals("State of SCH not change while operation is  in progress",
            State.INSTALLING, serviceComponentHost2.getState());
  }


  @Test
  public void testComponentUpgradeFailReport() throws AmbariException, InvalidStateTransitionException {
    ActionManager am = getMockActionManager();
    Cluster cluster = getDummyCluster();

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
    hdfs.addServiceComponent(HDFS_CLIENT).persist();
    hdfs.getServiceComponent(HDFS_CLIENT).addServiceComponentHost(DummyHostname1).persist();

    ServiceComponentHost serviceComponentHost1 = clusters.getCluster(DummyCluster).getService(HDFS).
            getServiceComponent(DATANODE).getServiceComponentHost(DummyHostname1);
    ServiceComponentHost serviceComponentHost2 = clusters.getCluster(DummyCluster).getService(HDFS).
            getServiceComponent(NAMENODE).getServiceComponentHost(DummyHostname1);

    StackId stack130 = new StackId("HDP-1.3.0");
    StackId stack122 = new StackId("HDP-1.2.2");

    serviceComponentHost1.setState(State.UPGRADING);
    serviceComponentHost2.setState(State.INSTALLING);

    serviceComponentHost1.setStackVersion(stack122);
    serviceComponentHost1.setDesiredStackVersion(stack130);
    serviceComponentHost2.setStackVersion(stack122);

    HeartBeat hb = new HeartBeat();
    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(0);
    hb.setHostname(DummyHostname1);
    hb.setNodeStatus(new HostStatus(Status.HEALTHY, DummyHostStatus));
    CommandReport cr1 = new CommandReport();
    cr1.setActionId(StageUtils.getActionId(requestId, stageId));
    cr1.setTaskId(1);
    cr1.setClusterName(DummyCluster);
    cr1.setServiceName(HDFS);
    cr1.setRole(DATANODE);
    cr1.setStatus(HostRoleStatus.FAILED.toString());
    cr1.setStdErr("none");
    cr1.setStdOut("dummy output");
    cr1.setExitCode(0);

    CommandReport cr2 = new CommandReport();
    cr2.setActionId(StageUtils.getActionId(requestId, stageId));
    cr2.setTaskId(2);
    cr2.setClusterName(DummyCluster);
    cr2.setServiceName(HDFS);
    cr2.setRole(NAMENODE);
    cr2.setStatus(HostRoleStatus.FAILED.toString());
    cr2.setStdErr("none");
    cr2.setStdOut("dummy output");
    cr2.setExitCode(0);
    ArrayList<CommandReport> reports = new ArrayList<CommandReport>();
    reports.add(cr1);
    reports.add(cr2);
    hb.setReports(reports);

    ActionQueue aq = new ActionQueue();
    HeartBeatHandler handler = getHeartBeatHandler(am, aq);
    handler.handleHeartBeat(hb);
    assertEquals("State of SCH should change after fail report",
        State.UPGRADING, serviceComponentHost1.getState());
    assertEquals("State of SCH should change after fail report",
            State.INSTALL_FAILED, serviceComponentHost2.getState());
    assertEquals("Stack version of SCH should not change after fail report",
            stack122, serviceComponentHost1.getStackVersion());
    assertEquals("Stack version of SCH should not change after fail report",
            stack130, serviceComponentHost1.getDesiredStackVersion());
    assertEquals("Stack version of SCH should not change after fail report",
            State.INSTALL_FAILED, serviceComponentHost2.getState());
  }

  private ActionManager getMockActionManager() {
    return new ActionManager(0, 0, null, null,
              new ActionDBInMemoryImpl(), new HostsMap((String) null), null, unitOfWork, null);
  }


  private ComponentStatus createComponentStatus(String clusterName, String serviceName, String message,
                                                State state, String componentName, String stackVersion) {
    ComponentStatus componentStatus1 = new ComponentStatus();
    componentStatus1.setClusterName(clusterName);
    componentStatus1.setServiceName(serviceName);
    componentStatus1.setMessage(message);
    componentStatus1.setStatus(state.name());
    componentStatus1.setComponentName(componentName);
    componentStatus1.setStackVersion(stackVersion);
    return componentStatus1;
  }

  private HeartBeatHandler getHeartBeatHandler(ActionManager am, ActionQueue aq)
      throws InvalidStateTransitionException, AmbariException {
    HeartBeatHandler handler = new HeartBeatHandler(clusters, aq, am, injector);
    Register reg = new Register();
    HostInfo hi = new HostInfo();
    hi.setHostName(DummyHostname1);
    hi.setOS(DummyOs);
    hi.setOSRelease(DummyOSRelease);
    reg.setHostname(DummyHostname1);
    reg.setResponseId(0);
    reg.setHardwareProfile(hi);
    reg.setAgentVersion(metaInfo.getServerVersion());
    handler.handleRegistration(reg);
    return handler;
  }

  private Cluster getDummyCluster()
      throws AmbariException {
    clusters.addHost(DummyHostname1);
    clusters.getHost(DummyHostname1).setOsType(DummyOsType);
    clusters.getHost(DummyHostname1).persist();
    clusters.addCluster(DummyCluster);

    Cluster cluster = clusters.getCluster(DummyCluster);
    cluster.setDesiredStackVersion(new StackId(DummyStackId));
    return cluster;
  }
}
