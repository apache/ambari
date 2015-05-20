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
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyCurrentPingPort;
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
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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

import junit.framework.Assert;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.ActionDBAccessor;
import org.apache.ambari.server.actionmanager.ActionDBAccessorImpl;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.actionmanager.Request;
import org.apache.ambari.server.actionmanager.RequestFactory;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.agent.HostStatus.Status;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.HostsMap;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.ResourceTypeDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.serveraction.kerberos.KerberosIdentityDataFileWriter;
import org.apache.ambari.server.serveraction.kerberos.KerberosIdentityDataFileWriterFactory;
import org.apache.ambari.server.serveraction.kerberos.KerberosServerAction;
import org.apache.ambari.server.state.Alert;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostHealthStatus;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.SecurityState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostInstallEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStartEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostUpgradeEvent;
import org.apache.ambari.server.utils.EventBusSynchronizer;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.UnitOfWork;

public class TestHeartbeatHandler {

  private static final Logger log = LoggerFactory.getLogger(TestHeartbeatHandler.class);
  private Injector injector;
  private Clusters clusters;
  long requestId = 23;
  long stageId = 31;

  private final static StackId HDP_22_STACK = new StackId("HDP", "2.2.0");

  @Inject
  AmbariMetaInfo metaInfo;

  @Inject
  Configuration config;

  @Inject
  ActionDBAccessor actionDBAccessor;

  @Inject
  OrmTestHelper helper;

  @Inject
  ResourceTypeDAO resourceTypeDAO;

  @Inject
  StackDAO stackDAO;

  @Inject
  ClusterDAO clusterDAO;

  @Inject
  HostDAO hostDAO;

  @Inject
  StageFactory stageFactory;

  @Inject
  HostRoleCommandFactory hostRoleCommandFactory;

  private UnitOfWork unitOfWork;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();


  @Before
  public void setup() throws Exception {
    InMemoryDefaultTestModule module = new InMemoryDefaultTestModule(){

      @Override
      protected void configure() {
        getProperties().put("recovery.type", "FULL");
        getProperties().put("recovery.lifetime_max_count", "10");
        getProperties().put("recovery.max_count", "4");
        getProperties().put("recovery.window_in_minutes", "23");
        getProperties().put("recovery.retry_interval", "2");
        super.configure();
      }
    };
    injector = Guice.createInjector(module);
    injector.getInstance(GuiceJpaInitializer.class);
    clusters = injector.getInstance(Clusters.class);
    injector.injectMembers(this);
    log.debug("Using server os type=" + config.getServerOsType());
    unitOfWork = injector.getInstance(UnitOfWork.class);
  }

  @After
  public void teardown() throws AmbariException {
    injector.getInstance(PersistService.class).stop();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testHeartbeat() throws Exception {
    ActionManager am = getMockActionManager();
    expect(am.getTasks(anyObject(List.class))).andReturn(new ArrayList<HostRoleCommand>());
    replay(am);
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
    execCmd.setRequestAndStage(2, 34);
    execCmd.setHostname(DummyHostname1);
    aq.enqueue(DummyHostname1, new ExecutionCommand());
    HeartBeat hb = new HeartBeat();
    hb.setResponseId(0);
    HostStatus hs = new HostStatus(Status.HEALTHY, DummyHostStatus);
    List<Alert> al = new ArrayList<Alert>();
    al.add(new Alert());
    hb.setNodeStatus(hs);
    hb.setHostname(DummyHostname1);

    handler.handleHeartBeat(hb);
    assertEquals(HostState.HEALTHY, hostObject.getState());
    assertEquals(0, aq.dequeueAll(DummyHostname1).size());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testHeartbeatWithConfigs() throws Exception {
    Cluster cluster = getDummyCluster();
    Service hdfs = cluster.addService(HDFS);
    hdfs.persist();
    hdfs.addServiceComponent(DATANODE).persist();
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1).persist();
    hdfs.addServiceComponent(NAMENODE).persist();
    hdfs.getServiceComponent(NAMENODE).addServiceComponentHost(DummyHostname1).persist();
    hdfs.addServiceComponent(SECONDARY_NAMENODE).persist();
    hdfs.getServiceComponent(SECONDARY_NAMENODE).addServiceComponentHost(DummyHostname1).persist();

    ActionQueue aq = new ActionQueue();

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

    cr.setConfigurationTags(new HashMap<String, Map<String, String>>() {{
      put("global", new HashMap<String, String>() {{
        put("tag", "version1");
      }});
    }});

    reports.add(cr);
    hb.setReports(reports);

    HostEntity host1 = hostDAO.findByName(DummyHostname1);
    Assert.assertNotNull(host1);
    final HostRoleCommand command = hostRoleCommandFactory.create(DummyHostname1,
            Role.DATANODE, null, null);

    ActionManager am = getMockActionManager();
    expect(am.getTasks(anyObject(List.class))).andReturn(
            new ArrayList<HostRoleCommand>() {{
              add(command);
            }});
    replay(am);

    HeartBeatHandler handler = getHeartBeatHandler(am, aq);
    handler.handleHeartBeat(hb);

    // the heartbeat test passed if actual configs is populated
    Assert.assertNotNull(serviceComponentHost1.getActualConfigs());
    Assert.assertEquals(serviceComponentHost1.getActualConfigs().size(), 1);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testRestartRequiredAfterInstallClient() throws Exception {
    Cluster cluster = getDummyCluster();
    Service hdfs = cluster.addService(HDFS);
    hdfs.persist();
    hdfs.addServiceComponent(HDFS_CLIENT).persist();
    hdfs.getServiceComponent(HDFS_CLIENT).addServiceComponentHost(DummyHostname1).persist();

    ActionQueue aq = new ActionQueue();

    ServiceComponentHost serviceComponentHost = clusters.getCluster(DummyCluster).getService(HDFS).
        getServiceComponent(HDFS_CLIENT).getServiceComponentHost(DummyHostname1);

    serviceComponentHost.setState(State.INSTALLED);
    serviceComponentHost.setRestartRequired(true);

    HeartBeat hb = new HeartBeat();
    hb.setResponseId(0);
    hb.setNodeStatus(new HostStatus(Status.HEALTHY, DummyHostStatus));
    hb.setHostname(DummyHostname1);


    List<CommandReport> reports = new ArrayList<CommandReport>();
    CommandReport cr = new CommandReport();
    cr.setActionId(StageUtils.getActionId(requestId, stageId));
    cr.setServiceName(HDFS);
    cr.setRoleCommand("INSTALL");
    cr.setCustomCommand("EXECUTION_COMMAND");
    cr.setTaskId(1);
    cr.setRole(HDFS_CLIENT);
    cr.setStatus("COMPLETED");
    cr.setStdErr("");
    cr.setStdOut("");
    cr.setExitCode(215);
    cr.setClusterName(DummyCluster);
    cr.setConfigurationTags(new HashMap<String, Map<String, String>>() {{
      put("global", new HashMap<String, String>() {{
        put("tag", "version1");
      }});
    }});
    reports.add(cr);
    hb.setReports(reports);

    final HostRoleCommand command = hostRoleCommandFactory.create(DummyHostname1,
        Role.DATANODE, null, null);

    ActionManager am = getMockActionManager();
    expect(am.getTasks(anyObject(List.class))).andReturn(
      new ArrayList<HostRoleCommand>() {{
        add(command);
        add(command);
      }});
    replay(am);

    HeartBeatHandler handler = getHeartBeatHandler(am, aq);
    handler.handleHeartBeat(hb);

    Assert.assertNotNull(serviceComponentHost.getActualConfigs());
    Assert.assertFalse(serviceComponentHost.isRestartRequired());
    Assert.assertEquals(serviceComponentHost.getActualConfigs().size(), 1);

  }

  @Test
  @SuppressWarnings("unchecked")
  public void testHeartbeatCustomCommandWithConfigs() throws Exception {
    Cluster cluster = getDummyCluster();
    Service hdfs = cluster.addService(HDFS);
    hdfs.persist();
    hdfs.addServiceComponent(DATANODE).persist();
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1).persist();
    hdfs.addServiceComponent(NAMENODE).persist();
    hdfs.getServiceComponent(NAMENODE).addServiceComponentHost(DummyHostname1).persist();
    hdfs.addServiceComponent(SECONDARY_NAMENODE).persist();
    hdfs.getServiceComponent(SECONDARY_NAMENODE).addServiceComponentHost(DummyHostname1).persist();

    ActionQueue aq = new ActionQueue();

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
    cr.setRoleCommand("CUSTOM_COMMAND");
    cr.setCustomCommand("RESTART");
    cr.setTaskId(1);
    cr.setRole(DATANODE);
    cr.setStatus("COMPLETED");
    cr.setStdErr("");
    cr.setStdOut("");
    cr.setExitCode(215);
    cr.setClusterName(DummyCluster);
    cr.setConfigurationTags(new HashMap<String, Map<String,String>>() {{
      put("global", new HashMap<String,String>() {{ put("tag", "version1"); }});
    }});
    CommandReport crn = new CommandReport();
    crn.setActionId(StageUtils.getActionId(requestId, stageId));
    crn.setServiceName(HDFS);
    crn.setRoleCommand("CUSTOM_COMMAND");
    crn.setCustomCommand("START");
    crn.setTaskId(1);
    crn.setRole(NAMENODE);
    crn.setStatus("COMPLETED");
    crn.setStdErr("");
    crn.setStdOut("");
    crn.setExitCode(215);
    crn.setClusterName(DummyCluster);
    crn.setConfigurationTags(new HashMap<String, Map<String,String>>() {{
      put("global", new HashMap<String,String>() {{ put("tag", "version1"); }});
    }});

    reports.add(cr);
    reports.add(crn);
    hb.setReports(reports);

    final HostRoleCommand command = hostRoleCommandFactory.create(DummyHostname1,
      Role.DATANODE, null, null);

    ActionManager am = getMockActionManager();
    expect(am.getTasks(anyObject(List.class))).andReturn(
            new ArrayList<HostRoleCommand>() {{
              add(command);
              add(command);
            }});
    replay(am);

    HeartBeatHandler handler = getHeartBeatHandler(am, aq);
    handler.handleHeartBeat(hb);

    // the heartbeat test passed if actual configs is populated
    Assert.assertNotNull(serviceComponentHost1.getActualConfigs());
    Assert.assertEquals(serviceComponentHost1.getActualConfigs().size(), 1);
    Assert.assertNotNull(serviceComponentHost2.getActualConfigs());
    Assert.assertEquals(serviceComponentHost2.getActualConfigs().size(), 1);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testHeartbeatCustomStartStop() throws Exception {
    Cluster cluster = getDummyCluster();
    Service hdfs = cluster.addService(HDFS);
    hdfs.persist();
    hdfs.addServiceComponent(DATANODE).persist();
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1).persist();
    hdfs.addServiceComponent(NAMENODE).persist();
    hdfs.getServiceComponent(NAMENODE).addServiceComponentHost(DummyHostname1).persist();
    hdfs.addServiceComponent(SECONDARY_NAMENODE).persist();
    hdfs.getServiceComponent(SECONDARY_NAMENODE).addServiceComponentHost(DummyHostname1).persist();

    ActionQueue aq = new ActionQueue();

    ServiceComponentHost serviceComponentHost1 = clusters.getCluster(DummyCluster).getService(HDFS).
        getServiceComponent(DATANODE).getServiceComponentHost(DummyHostname1);
    ServiceComponentHost serviceComponentHost2 = clusters.getCluster(DummyCluster).getService(HDFS).
        getServiceComponent(NAMENODE).getServiceComponentHost(DummyHostname1);
    serviceComponentHost1.setState(State.INSTALLED);
    serviceComponentHost2.setState(State.STARTED);
    serviceComponentHost1.setRestartRequired(true);
    serviceComponentHost2.setRestartRequired(true);

    HeartBeat hb = new HeartBeat();
    hb.setResponseId(0);
    hb.setNodeStatus(new HostStatus(Status.HEALTHY, DummyHostStatus));
    hb.setHostname(DummyHostname1);

    List<CommandReport> reports = new ArrayList<CommandReport>();
    CommandReport cr = new CommandReport();
    cr.setActionId(StageUtils.getActionId(requestId, stageId));
    cr.setServiceName(HDFS);
    cr.setRoleCommand("CUSTOM_COMMAND");
    cr.setCustomCommand("START");
    cr.setTaskId(1);
    cr.setRole(DATANODE);
    cr.setStatus("COMPLETED");
    cr.setStdErr("");
    cr.setStdOut("");
    cr.setExitCode(215);
    cr.setClusterName(DummyCluster);
    CommandReport crn = new CommandReport();
    crn.setActionId(StageUtils.getActionId(requestId, stageId));
    crn.setServiceName(HDFS);
    crn.setRoleCommand("CUSTOM_COMMAND");
    crn.setCustomCommand("STOP");
    crn.setTaskId(1);
    crn.setRole(NAMENODE);
    crn.setStatus("COMPLETED");
    crn.setStdErr("");
    crn.setStdOut("");
    crn.setExitCode(215);
    crn.setClusterName(DummyCluster);

    reports.add(cr);
    reports.add(crn);
    hb.setReports(reports);

    assertTrue(serviceComponentHost1.isRestartRequired());

    final HostRoleCommand command = hostRoleCommandFactory.create(DummyHostname1,
      Role.DATANODE, null, null);

    ActionManager am = getMockActionManager();
    expect(am.getTasks(anyObject(List.class))).andReturn(
      new ArrayList<HostRoleCommand>() {{
        add(command);
        add(command);
      }});
    replay(am);

    HeartBeatHandler handler = getHeartBeatHandler(am, aq);
    handler.handleHeartBeat(hb);

    // the heartbeat test passed if actual configs is populated
    State componentState1 = serviceComponentHost1.getState();
    assertEquals(State.STARTED, componentState1);
    assertFalse(serviceComponentHost1.isRestartRequired());
    State componentState2 = serviceComponentHost2.getState();
    assertEquals(State.INSTALLED, componentState2);
    assertTrue(serviceComponentHost2.isRestartRequired());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testStatusHeartbeat() throws Exception {
    Cluster cluster = getDummyCluster();
    Service hdfs = cluster.addService(HDFS);
    hdfs.persist();
    hdfs.addServiceComponent(DATANODE).persist();
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1).persist();
    hdfs.addServiceComponent(NAMENODE).persist();
    hdfs.getServiceComponent(NAMENODE).addServiceComponentHost(DummyHostname1).persist();
    hdfs.addServiceComponent(SECONDARY_NAMENODE).persist();
    hdfs.getServiceComponent(SECONDARY_NAMENODE).addServiceComponentHost(DummyHostname1).persist();

    ActionQueue aq = new ActionQueue();

    ServiceComponentHost serviceComponentHost1 = clusters.getCluster(DummyCluster).getService(HDFS).
            getServiceComponent(DATANODE).getServiceComponentHost(DummyHostname1);
    ServiceComponentHost serviceComponentHost2 = clusters.getCluster(DummyCluster).getService(HDFS).
            getServiceComponent(NAMENODE).getServiceComponentHost(DummyHostname1);
    ServiceComponentHost serviceComponentHost3 = clusters.getCluster(DummyCluster).getService(HDFS).
        getServiceComponent(SECONDARY_NAMENODE).getServiceComponentHost(DummyHostname1);
    serviceComponentHost1.setState(State.INSTALLED);
    serviceComponentHost1.setSecurityState(SecurityState.UNSECURED);
    serviceComponentHost2.setState(State.INSTALLED);
    serviceComponentHost2.setSecurityState(SecurityState.SECURING);
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
    componentStatus1.setSecurityState(SecurityState.SECURED_KERBEROS.name());
    componentStatus1.setComponentName(DATANODE);
    componentStatuses.add(componentStatus1);
    ComponentStatus componentStatus2 = new ComponentStatus();
    componentStatus2.setClusterName(DummyCluster);
    componentStatus2.setServiceName(HDFS);
    componentStatus2.setMessage(DummyHostStatus);
    componentStatus2.setStatus(State.STARTED.name());
    componentStatus2.setSecurityState(SecurityState.UNSECURED.name());
    componentStatus2.setComponentName(SECONDARY_NAMENODE);
    componentStatuses.add(componentStatus2);
    hb.setComponentStatus(componentStatuses);

    final HostRoleCommand command = hostRoleCommandFactory.create(DummyHostname1,
            Role.DATANODE, null, null);

    ActionManager am = getMockActionManager();
    expect(am.getTasks(anyObject(List.class))).andReturn(
      new ArrayList<HostRoleCommand>() {{
        add(command);
        add(command);
      }});
    replay(am);

    HeartBeatHandler handler = getHeartBeatHandler(am, aq);
    handler.handleHeartBeat(hb);
    State componentState1 = serviceComponentHost1.getState();
    State componentState2 = serviceComponentHost2.getState();
    State componentState3 = serviceComponentHost3.getState();
    assertEquals(State.STARTED, componentState1);
    assertEquals(SecurityState.SECURED_KERBEROS, serviceComponentHost1.getSecurityState());
    assertEquals(State.INSTALLED, componentState2);
    assertEquals(SecurityState.SECURING, serviceComponentHost2.getSecurityState());
    assertEquals(State.STARTED, componentState3);
    assertEquals(SecurityState.UNSECURED, serviceComponentHost3.getSecurityState());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testStatusHeartbeatWithAnnotation() throws Exception {
    Cluster cluster = getDummyCluster();
    Service hdfs = cluster.addService(HDFS);
    hdfs.persist();
    hdfs.addServiceComponent(DATANODE).persist();
    hdfs.addServiceComponent(NAMENODE).persist();
    hdfs.addServiceComponent(SECONDARY_NAMENODE).persist();

    ActionQueue aq = new ActionQueue();

    HeartBeat hb = new HeartBeat();
    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(0);
    hb.setHostname(DummyHostname1);
    hb.setNodeStatus(new HostStatus(Status.HEALTHY, DummyHostStatus));
    hb.setReports(new ArrayList<CommandReport>());
    ArrayList<ComponentStatus> componentStatuses = new ArrayList<ComponentStatus>();
    hb.setComponentStatus(componentStatuses);

    final HostRoleCommand command = hostRoleCommandFactory.create(DummyHostname1,
            Role.DATANODE, null, null);

    ActionManager am = getMockActionManager();
    expect(am.getTasks(anyObject(List.class))).andReturn(
            new ArrayList<HostRoleCommand>() {{
              add(command);
            }}).anyTimes();
    replay(am);

    HeartBeatHandler handler = getHeartBeatHandler(am, aq);
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
  @SuppressWarnings("unchecked")
  public void testLiveStatusUpdateAfterStopFailed() throws Exception {
    Cluster cluster = getDummyCluster();
    Service hdfs = cluster.addService(HDFS);
    hdfs.persist();
    hdfs.addServiceComponent(DATANODE).persist();
    hdfs.getServiceComponent(DATANODE).
            addServiceComponentHost(DummyHostname1).persist();
    hdfs.addServiceComponent(NAMENODE).persist();
    hdfs.getServiceComponent(NAMENODE).
            addServiceComponentHost(DummyHostname1).persist();

    ActionQueue aq = new ActionQueue();

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
    componentStatus1.setSecurityState(SecurityState.UNSECURED.name());
    componentStatus1.setComponentName(DATANODE);
    componentStatuses.add(componentStatus1);

    ComponentStatus componentStatus2 = new ComponentStatus();
    componentStatus2.setClusterName(DummyCluster);
    componentStatus2.setServiceName(HDFS);
    componentStatus2.setMessage(DummyHostStatus);
    componentStatus2.setStatus(State.INSTALLED.name());
    componentStatus2.setSecurityState(SecurityState.UNSECURED.name());
    componentStatus2.setComponentName(NAMENODE);
    componentStatuses.add(componentStatus2);

    hb.setComponentStatus(componentStatuses);

    final HostRoleCommand command = hostRoleCommandFactory.create(DummyHostname1,
            Role.DATANODE, null, null);

    ActionManager am = getMockActionManager();
    expect(am.getTasks(anyObject(List.class))).andReturn(
            new ArrayList<HostRoleCommand>() {{
              add(command);
              add(command);
            }});
    replay(am);

    HeartBeatHandler handler = getHeartBeatHandler(am, aq);
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

    StackId dummyStackId = new StackId(DummyStackId);
    clusters.addCluster(DummyCluster, dummyStackId);

    ActionDBAccessor db = injector.getInstance(ActionDBAccessorImpl.class);
    ActionManager am = new ActionManager(5000, 1200000, new ActionQueue(), clusters, db,
        new HostsMap((String) null), unitOfWork, injector.getInstance(RequestFactory.class), null, null);
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
    am.processTaskResponse(DummyHostname1, reports, stage.getOrderedHostRoleCommands());
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

  private void populateActionDB(ActionDBAccessor db, String DummyHostname1) throws AmbariException {
    Stage s = stageFactory.createNew(requestId, "/a/b", DummyCluster, 1L, "heartbeat handler test",
      "clusterHostInfo", "commandParamsStage", "hostParamsStage");
    s.setStageId(stageId);
    String filename = null;
    s.addHostRoleExecutionCommand(DummyHostname1, Role.HBASE_MASTER,
        RoleCommand.START,
        new ServiceComponentHostStartEvent(Role.HBASE_MASTER.toString(),
            DummyHostname1, System.currentTimeMillis()), DummyCluster, HBASE, false);
    List<Stage> stages = new ArrayList<Stage>();
    stages.add(s);
    Request request = new Request(stages, clusters);
    db.persistActions(request);
  }

  @Test
  public void testRegistration() throws AmbariException,
      InvalidStateTransitionException {
    ActionManager am = getMockActionManager();
    replay(am);
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
    reg.setPrefix(Configuration.PREFIX_DIR);
    handler.handleRegistration(reg);
    assertEquals(hostObject.getState(), HostState.HEALTHY);
    assertEquals(DummyOsType, hostObject.getOsType());
    assertEquals(DummyCurrentPingPort, hostObject.getCurrentPingPort());
    assertTrue(hostObject.getLastRegistrationTime() != 0);
    assertEquals(hostObject.getLastHeartbeatTime(),
        hostObject.getLastRegistrationTime());
  }

  @Test
  public void testRegistrationRecoveryConfig() throws AmbariException,
      InvalidStateTransitionException {
    ActionManager am = getMockActionManager();
    replay(am);
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
    reg.setPrefix(Configuration.PREFIX_DIR);
    RegistrationResponse rr = handler.handleRegistration(reg);
    RecoveryConfig rc = rr.getRecoveryConfig();
    assertEquals(rc.getMaxCount(), "4");
    assertEquals(rc.getType(), "FULL");
    assertEquals(rc.getMaxLifetimeCount(), "10");
    assertEquals(rc.getRetryGap(), "2");
    assertEquals(rc.getWindowInMinutes(), "23");

    rc = RecoveryConfig.getRecoveryConfig(new Configuration());
    assertEquals(rc.getMaxCount(), "6");
    assertEquals(rc.getType(), "DEFAULT");
    assertEquals(rc.getMaxLifetimeCount(), "12");
    assertEquals(rc.getRetryGap(), "5");
    assertEquals(rc.getWindowInMinutes(), "60");
  }

  @Test
  public void testRegistrationAgentConfig() throws AmbariException,
      InvalidStateTransitionException {
    ActionManager am = getMockActionManager();
    replay(am);
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
    reg.setPrefix(Configuration.PREFIX_DIR);
    RegistrationResponse rr = handler.handleRegistration(reg);
    Map<String, String> config = rr.getAgentConfig();
    assertFalse(config.isEmpty());
    assertTrue(config.containsKey(Configuration.CHECK_REMOTE_MOUNTS_KEY));
    assertTrue("true".equals(config.get(Configuration.CHECK_REMOTE_MOUNTS_KEY)));
    assertTrue(config.containsKey(Configuration.CHECK_MOUNTS_TIMEOUT_KEY));
    assertTrue("0".equals(config.get(Configuration.CHECK_MOUNTS_TIMEOUT_KEY)));
  }

  @Test
  public void testRegistrationWithBadVersion() throws AmbariException,
      InvalidStateTransitionException {

    ActionManager am = getMockActionManager();
    replay(am);
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
    reg.setPrefix(Configuration.PREFIX_DIR);
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
    replay(am);
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
    replay(am);
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
    replay(am);
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
    replay(am);
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
    reg.setPrefix(Configuration.PREFIX_DIR);
    RegistrationResponse response = handler.handleRegistration(reg);

    assertEquals(hostObject.getState(), HostState.HEALTHY);
    assertEquals("redhat5", hostObject.getOsType());
    assertEquals(RegistrationStatus.OK, response.getResponseStatus());
    assertEquals(0, response.getResponseId());
    assertEquals(reg.getPrefix(), hostObject.getPrefix());
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
    replay(am);
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
  @SuppressWarnings("unchecked")
  public void testTaskInProgressHandling() throws AmbariException, InvalidStateTransitionException {
    Cluster cluster = getDummyCluster();
    Service hdfs = cluster.addService(HDFS);
    hdfs.persist();
    hdfs.addServiceComponent(DATANODE).persist();
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1).persist();
    hdfs.addServiceComponent(NAMENODE).persist();
    hdfs.getServiceComponent(NAMENODE).addServiceComponentHost(DummyHostname1).persist();
    hdfs.addServiceComponent(SECONDARY_NAMENODE).persist();
    hdfs.getServiceComponent(SECONDARY_NAMENODE).addServiceComponentHost(DummyHostname1).persist();

    ActionQueue aq = new ActionQueue();

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
    cr.setRoleCommand("INSTALL");
    cr.setStatus("IN_PROGRESS");
    cr.setStdErr("none");
    cr.setStdOut("dummy output");
    cr.setExitCode(777);
    reports.add(cr);
    hb.setReports(reports);
    hb.setComponentStatus(new ArrayList<ComponentStatus>());

    final HostRoleCommand command = hostRoleCommandFactory.create(DummyHostname1,
            Role.DATANODE, null, RoleCommand.INSTALL);

    ActionManager am = getMockActionManager();
    expect(am.getTasks(anyObject(List.class))).andReturn(
            new ArrayList<HostRoleCommand>() {{
              add(command);
            }});
    replay(am);

    HeartBeatHandler handler = getHeartBeatHandler(am, aq);
    handler.handleHeartBeat(hb);
    State componentState1 = serviceComponentHost1.getState();
    assertEquals("Host state should still be installing", State.INSTALLING, componentState1);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testOPFailedEventForAbortedTask() throws AmbariException, InvalidStateTransitionException {
    Cluster cluster = getDummyCluster();
    Service hdfs = cluster.addService(HDFS);
    hdfs.persist();
    hdfs.addServiceComponent(DATANODE).persist();
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1).persist();
    hdfs.addServiceComponent(NAMENODE).persist();
    hdfs.getServiceComponent(NAMENODE).addServiceComponentHost(DummyHostname1).persist();
    hdfs.addServiceComponent(SECONDARY_NAMENODE).persist();
    hdfs.getServiceComponent(SECONDARY_NAMENODE).addServiceComponentHost(DummyHostname1).persist();

    ActionQueue aq = new ActionQueue();

    ServiceComponentHost serviceComponentHost1 = clusters.getCluster(DummyCluster).getService(HDFS).
      getServiceComponent(DATANODE).getServiceComponentHost(DummyHostname1);
    serviceComponentHost1.setState(State.INSTALLING);

    Stage s = stageFactory.createNew(1, "/a/b", "cluster1", 1L, "action manager test",
      "clusterHostInfo", "commandParamsStage", "hostParamsStage");
    s.setStageId(1);
    s.addHostRoleExecutionCommand(DummyHostname1, Role.DATANODE, RoleCommand.INSTALL,
      new ServiceComponentHostInstallEvent(Role.DATANODE.toString(),
        DummyHostname1, System.currentTimeMillis(), "HDP-1.3.0"),
          DummyCluster, "HDFS", false);
    List<Stage> stages = new ArrayList<Stage>();
    stages.add(s);
    Request request = new Request(stages, clusters);
    actionDBAccessor.persistActions(request);
    actionDBAccessor.abortHostRole(DummyHostname1, 1, 1, Role.DATANODE.name());

    HeartBeat hb = new HeartBeat();
    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(0);
    hb.setHostname(DummyHostname1);
    hb.setNodeStatus(new HostStatus(Status.HEALTHY, DummyHostStatus));

    List<CommandReport> reports = new ArrayList<CommandReport>();
    CommandReport cr = new CommandReport();
    cr.setActionId(StageUtils.getActionId(1, 1));
    cr.setTaskId(1);
    cr.setClusterName(DummyCluster);
    cr.setServiceName(HDFS);
    cr.setRole(DATANODE);
    cr.setRoleCommand("INSTALL");
    cr.setStatus("FAILED");
    cr.setStdErr("none");
    cr.setStdOut("dummy output");
    cr.setExitCode(777);
    reports.add(cr);
    hb.setReports(reports);
    hb.setComponentStatus(new ArrayList<ComponentStatus>());

    final HostRoleCommand command = hostRoleCommandFactory.create(DummyHostname1,
            Role.DATANODE, null, null);

    ActionManager am = getMockActionManager();
    expect(am.getTasks(anyObject(List.class))).andReturn(
            new ArrayList<HostRoleCommand>() {{
              add(command);
            }});
    replay(am);

    HeartBeatHandler handler = getHeartBeatHandler(am, aq);
    handler.handleHeartBeat(hb);
    State componentState1 = serviceComponentHost1.getState();
    assertEquals("Host state should still be installing", State.INSTALLING,
      componentState1);
  }

  /**
   * Tests the fact that when START and STOP commands are in progress, and heartbeat
   * forces the host component state to STARTED or INSTALLED, there are no undesired
   * side effects.
   * @throws AmbariException
   * @throws InvalidStateTransitionException
   */
  @Test
  @SuppressWarnings("unchecked")
  public void testCommandReportOnHeartbeatUpdatedState()
      throws AmbariException, InvalidStateTransitionException {
    Cluster cluster = getDummyCluster();
    Service hdfs = cluster.addService(HDFS);
    hdfs.persist();
    hdfs.addServiceComponent(DATANODE).persist();
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1).persist();

    ActionQueue aq = new ActionQueue();

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

    final HostRoleCommand command = hostRoleCommandFactory.create(DummyHostname1,
            Role.DATANODE, null, null);

    ActionManager am = getMockActionManager();
    expect(am.getTasks(anyObject(List.class))).andReturn(
            new ArrayList<HostRoleCommand>() {{
              add(command);
            }}).anyTimes();
    replay(am);

    HeartBeatHandler handler = getHeartBeatHandler(am, aq);
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
  @SuppressWarnings("unchecked")
  public void testUpgradeSpecificHandling() throws AmbariException, InvalidStateTransitionException {
    Cluster cluster = getDummyCluster();
    Service hdfs = cluster.addService(HDFS);
    hdfs.persist();
    hdfs.addServiceComponent(DATANODE).persist();
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1).persist();

    ActionQueue aq = new ActionQueue();

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
    cr.setRoleCommand("INSTALL");
    cr.setStatus(HostRoleStatus.IN_PROGRESS.toString());
    cr.setStdErr("none");
    cr.setStdOut("dummy output");
    cr.setExitCode(777);
    reports.add(cr);
    hb.setReports(reports);
    hb.setComponentStatus(new ArrayList<ComponentStatus>());

    final HostRoleCommand command = hostRoleCommandFactory.create(DummyHostname1,
            Role.DATANODE, null, null);

    ActionManager am = getMockActionManager();
    expect(am.getTasks(anyObject(List.class))).andReturn(
            new ArrayList<HostRoleCommand>() {{
              add(command);
            }}).anyTimes();
    replay(am);

    HeartBeatHandler handler = getHeartBeatHandler(am, aq);
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
  @SuppressWarnings("unchecked")
  public void testStatusHeartbeatWithVersion() throws Exception {
    Cluster cluster = getDummyCluster();
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
    StackId stack120 = new StackId("HDP-1.2.0");

    serviceComponentHost1.setState(State.INSTALLED);
    serviceComponentHost2.setState(State.STARTED);
    serviceComponentHost3.setState(State.STARTED);
    serviceComponentHost1.setStackVersion(stack130);
    serviceComponentHost2.setStackVersion(stack120);
    serviceComponentHost3.setStackVersion(stack120);

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
        SecurityState.UNSECURED, DATANODE, "{\"stackName\":\"HDP\",\"stackVersion\":\"1.3.0\"}");
    ComponentStatus componentStatus2 =
        createComponentStatus(DummyCluster, HDFS, DummyHostStatus, State.STARTED, SecurityState.UNSECURED, NAMENODE, "");
    ComponentStatus componentStatus3 = createComponentStatus(DummyCluster, HDFS, DummyHostStatus, State.INSTALLED,
        SecurityState.UNSECURED, HDFS_CLIENT, "{\"stackName\":\"HDP\",\"stackVersion\":\"1.3.0\"}");

    componentStatuses.add(componentStatus1);
    componentStatuses.add(componentStatus2);
    componentStatuses.add(componentStatus3);
    hb.setComponentStatus(componentStatuses);

    ActionQueue aq = new ActionQueue();
    ActionManager am = getMockActionManager();
    expect(am.getTasks(anyObject(List.class))).andReturn(
            new ArrayList<HostRoleCommand>() {{
            }});
    replay(am);
    HeartBeatHandler handler = getHeartBeatHandler(am, aq);
    handler.handleHeartBeat(hb);
    assertEquals("Matching value " + serviceComponentHost1.getStackVersion(),
        stack130, serviceComponentHost1.getStackVersion());
    assertEquals("Matching value " + serviceComponentHost2.getStackVersion(),
        stack120, serviceComponentHost2.getStackVersion());
    assertEquals("Matching value " + serviceComponentHost3.getStackVersion(),
        stack130, serviceComponentHost3.getStackVersion());
    assertTrue(hb.getAgentEnv().getHostHealth().getServerTimeStampAtReporting() >= hb.getTimestamp());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testComponentUpgradeCompleteReport() throws AmbariException, InvalidStateTransitionException {
    Cluster cluster = getDummyCluster();
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
    StackId stack120 = new StackId("HDP-1.2.0");

    serviceComponentHost1.setState(State.UPGRADING);
    serviceComponentHost2.setState(State.INSTALLING);

    serviceComponentHost1.setStackVersion(stack120);
    serviceComponentHost1.setDesiredStackVersion(stack130);
    serviceComponentHost2.setStackVersion(stack120);

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
    final HostRoleCommand command = hostRoleCommandFactory.create(DummyHostname1,
            Role.DATANODE, null, null);

    ActionManager am = getMockActionManager();
    expect(am.getTasks(anyObject(List.class))).andReturn(
            new ArrayList<HostRoleCommand>() {{
              add(command);
              add(command);
            }});
    replay(am);

    HeartBeatHandler handler = getHeartBeatHandler(am, aq);
    handler.handleHeartBeat(hb);
    assertEquals("Stack version for SCH should be updated to " +
            serviceComponentHost1.getDesiredStackVersion(),
            stack130, serviceComponentHost1.getStackVersion());
    assertEquals("Stack version for SCH should not change ",
            stack120, serviceComponentHost2.getStackVersion());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testComponentUpgradeInProgressReport() throws AmbariException, InvalidStateTransitionException {
    Cluster cluster = getDummyCluster();
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
    StackId stack120 = new StackId("HDP-1.2.0");

    serviceComponentHost1.setState(State.UPGRADING);
    serviceComponentHost2.setState(State.INSTALLING);

    serviceComponentHost1.setStackVersion(stack120);
    serviceComponentHost1.setDesiredStackVersion(stack130);
    serviceComponentHost2.setStackVersion(stack120);

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
    cr1.setRoleCommand("INSTALL");
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
    cr2.setRoleCommand("INSTALL");
    cr2.setStatus(HostRoleStatus.IN_PROGRESS.toString());
    cr2.setStdErr("none");
    cr2.setStdOut("dummy output");
    cr2.setExitCode(777);
    ArrayList<CommandReport> reports = new ArrayList<CommandReport>();
    reports.add(cr1);
    reports.add(cr2);
    hb.setReports(reports);

    ActionQueue aq = new ActionQueue();
    final HostRoleCommand command = hostRoleCommandFactory.create(DummyHostname1,
            Role.DATANODE, null, null);

    ActionManager am = getMockActionManager();
    expect(am.getTasks(anyObject(List.class))).andReturn(
            new ArrayList<HostRoleCommand>() {{
              add(command);
              add(command);
            }});
    replay(am);

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
  @SuppressWarnings("unchecked")
  public void testComponentUpgradeFailReport() throws AmbariException, InvalidStateTransitionException {
    Cluster cluster = getDummyCluster();
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
    StackId stack120 = new StackId("HDP-1.2.0");

    serviceComponentHost1.setState(State.UPGRADING);
    serviceComponentHost2.setState(State.INSTALLING);

    serviceComponentHost1.setStackVersion(stack120);
    serviceComponentHost1.setDesiredStackVersion(stack130);
    serviceComponentHost2.setStackVersion(stack120);

    Stage s = stageFactory.createNew(requestId, "/a/b", "cluster1", 1L, "action manager test",
        "clusterHostInfo", "commandParamsStage", "hostParamsStage");
    s.setStageId(stageId);
    s.addHostRoleExecutionCommand(DummyHostname1, Role.DATANODE, RoleCommand.UPGRADE,
      new ServiceComponentHostUpgradeEvent(Role.DATANODE.toString(),
        DummyHostname1, System.currentTimeMillis(), "HDP-1.3.0"),
      DummyCluster, "HDFS", false);
    s.addHostRoleExecutionCommand(DummyHostname1, Role.NAMENODE, RoleCommand.INSTALL,
      new ServiceComponentHostInstallEvent(Role.NAMENODE.toString(),
        DummyHostname1, System.currentTimeMillis(), "HDP-1.3.0"),
          DummyCluster, "HDFS", false);
    List<Stage> stages = new ArrayList<Stage>();
    stages.add(s);
    Request request = new Request(stages, clusters);
    actionDBAccessor.persistActions(request);
    CommandReport cr = new CommandReport();
    cr.setActionId(StageUtils.getActionId(requestId, stageId));
    cr.setTaskId(1);
    cr.setClusterName(DummyCluster);
    cr.setServiceName(HDFS);
    cr.setRole(DATANODE);
    cr.setStatus(HostRoleStatus.IN_PROGRESS.toString());
    cr.setStdErr("none");
    cr.setStdOut("dummy output");
    actionDBAccessor.updateHostRoleState(DummyHostname1, requestId, stageId,
      Role.DATANODE.name(), cr);
    cr.setRole(NAMENODE);
    cr.setTaskId(2);
    actionDBAccessor.updateHostRoleState(DummyHostname1, requestId, stageId,
      Role.NAMENODE.name(), cr);

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
    cr1.setRoleCommand("INSTALL");
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
    cr2.setRoleCommand("INSTALL");
    cr2.setStatus(HostRoleStatus.FAILED.toString());
    cr2.setStdErr("none");
    cr2.setStdOut("dummy output");
    cr2.setExitCode(0);
    ArrayList<CommandReport> reports = new ArrayList<CommandReport>();
    reports.add(cr1);
    reports.add(cr2);
    hb.setReports(reports);

    ActionQueue aq = new ActionQueue();
    final HostRoleCommand command = hostRoleCommandFactory.create(DummyHostname1,
            Role.DATANODE, null, null);

    ActionManager am = getMockActionManager();
    expect(am.getTasks(anyObject(List.class))).andReturn(
            new ArrayList<HostRoleCommand>() {{
              add(command);
              add(command);
            }});
    replay(am);

    HeartBeatHandler handler = getHeartBeatHandler(am, aq);
    handler.handleHeartBeat(hb);
    assertEquals("State of SCH should change after fail report",
        State.UPGRADING, serviceComponentHost1.getState());
    assertEquals("State of SCH should change after fail report",
            State.INSTALL_FAILED, serviceComponentHost2.getState());
    assertEquals("Stack version of SCH should not change after fail report",
            stack120, serviceComponentHost1.getStackVersion());
    assertEquals("Stack version of SCH should not change after fail report",
            stack130, serviceComponentHost1.getDesiredStackVersion());
    assertEquals("Stack version of SCH should not change after fail report",
            State.INSTALL_FAILED, serviceComponentHost2.getState());
  }


  @Test
  @SuppressWarnings("unchecked")
  public void testRecoveryStatusReports() throws Exception {
    Clusters fsm = clusters;

    Cluster cluster = getDummyCluster();
    Host hostObject = clusters.getHost(DummyHostname1);
    Service hdfs = cluster.addService(HDFS);
    hdfs.persist();
    hdfs.addServiceComponent(DATANODE).persist();
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1).persist();
    hdfs.addServiceComponent(NAMENODE).persist();
    hdfs.getServiceComponent(NAMENODE).addServiceComponentHost(DummyHostname1).persist();
    hdfs.getServiceComponent(NAMENODE).getServiceComponentHost(DummyHostname1).setState(State.STARTED);
    hdfs.getServiceComponent(DATANODE).getServiceComponentHost(DummyHostname1).setState(State.STARTED);

    ActionQueue aq = new ActionQueue();

    final HostRoleCommand command = hostRoleCommandFactory.create(DummyHostname1, Role.DATANODE, null, null);
    ActionManager am = getMockActionManager();
    expect(am.getTasks(anyObject(List.class))).andReturn(
        new ArrayList<HostRoleCommand>() {{
          add(command);
          add(command);
        }}).anyTimes();
    replay(am);
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

    aq.enqueue(DummyHostname1, new StatusCommand());

    //All components are up
    HeartBeat hb1 = new HeartBeat();
    hb1.setResponseId(0);
    hb1.setNodeStatus(new HostStatus(Status.HEALTHY, DummyHostStatus));
    hb1.setHostname(DummyHostname1);
    RecoveryReport rr = new RecoveryReport();
    rr.setSummary("RECOVERABLE");
    List<ComponentRecoveryReport> compRecReports = new ArrayList<ComponentRecoveryReport>();
    ComponentRecoveryReport compRecReport = new ComponentRecoveryReport();
    compRecReport.setLimitReached(Boolean.FALSE);
    compRecReport.setName("DATANODE");
    compRecReport.setNumAttempts(2);
    compRecReports.add(compRecReport);
    rr.setComponentReports(compRecReports);
    hb1.setRecoveryReport(rr);
    handler.handleHeartBeat(hb1);
    assertEquals("RECOVERABLE", hostObject.getRecoveryReport().getSummary());
    assertEquals(1, hostObject.getRecoveryReport().getComponentReports().size());
    assertEquals(2, hostObject.getRecoveryReport().getComponentReports().get(0).getNumAttempts());

    HeartBeat hb2 = new HeartBeat();
    hb2.setResponseId(1);
    hb2.setNodeStatus(new HostStatus(Status.HEALTHY, DummyHostStatus));
    hb2.setHostname(DummyHostname1);
    rr = new RecoveryReport();
    rr.setSummary("UNRECOVERABLE");
    compRecReports = new ArrayList<ComponentRecoveryReport>();
    compRecReport = new ComponentRecoveryReport();
    compRecReport.setLimitReached(Boolean.TRUE);
    compRecReport.setName("DATANODE");
    compRecReport.setNumAttempts(5);
    compRecReports.add(compRecReport);
    rr.setComponentReports(compRecReports);
    hb2.setRecoveryReport(rr);
    handler.handleHeartBeat(hb2);
    assertEquals("UNRECOVERABLE", hostObject.getRecoveryReport().getSummary());
    assertEquals(1, hostObject.getRecoveryReport().getComponentReports().size());
    assertEquals(5, hostObject.getRecoveryReport().getComponentReports().get(0).getNumAttempts());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testProcessStatusReports() throws Exception {
    Clusters fsm = clusters;

    Cluster cluster = getDummyCluster();
    Host hostObject = clusters.getHost(DummyHostname1);
    Service hdfs = cluster.addService(HDFS);
    hdfs.persist();
    hdfs.addServiceComponent(DATANODE).persist();
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1).persist();
    hdfs.addServiceComponent(NAMENODE).persist();
    hdfs.getServiceComponent(NAMENODE).addServiceComponentHost(DummyHostname1).persist();
    hdfs.getServiceComponent(NAMENODE).getServiceComponentHost(DummyHostname1).setState(State.STARTED);
    hdfs.getServiceComponent(DATANODE).getServiceComponentHost(DummyHostname1).setState(State.STARTED);

    ActionQueue aq = new ActionQueue();

    final HostRoleCommand command = hostRoleCommandFactory.create(DummyHostname1,
            Role.DATANODE, null, null);
    ActionManager am = getMockActionManager();
    expect(am.getTasks(anyObject(List.class))).andReturn(
            new ArrayList<HostRoleCommand>() {{
              add(command);
              add(command);
            }}).anyTimes();
    replay(am);
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

    aq.enqueue(DummyHostname1, new StatusCommand());

    //All components are up
    HeartBeat hb1 = new HeartBeat();
    hb1.setResponseId(0);
    hb1.setNodeStatus(new HostStatus(Status.HEALTHY, DummyHostStatus));
    hb1.setHostname(DummyHostname1);
    List<ComponentStatus> componentStatus = new ArrayList<ComponentStatus>();
    ComponentStatus dataNodeStatus = new ComponentStatus();
    dataNodeStatus.setClusterName(cluster.getClusterName());
    dataNodeStatus.setServiceName(HDFS);
    dataNodeStatus.setComponentName(DATANODE);
    dataNodeStatus.setStatus("STARTED");
    dataNodeStatus.setSecurityState(SecurityState.UNSECURED.name());
    componentStatus.add(dataNodeStatus);
    ComponentStatus nameNodeStatus = new ComponentStatus();
    nameNodeStatus.setClusterName(cluster.getClusterName());
    nameNodeStatus.setServiceName(HDFS);
    nameNodeStatus.setComponentName(NAMENODE);
    nameNodeStatus.setStatus("STARTED");
    nameNodeStatus.setSecurityState(SecurityState.UNSECURED.name());
    componentStatus.add(nameNodeStatus);
    hb1.setComponentStatus(componentStatus);
    handler.handleHeartBeat(hb1);
    assertEquals(HostHealthStatus.HealthStatus.HEALTHY.name(), hostObject.getStatus());

    //Some slaves are down, masters are up
    HeartBeat hb2 = new HeartBeat();
    hb2.setResponseId(1);
    hb2.setNodeStatus(new HostStatus(Status.HEALTHY, DummyHostStatus));
    hb2.setHostname(DummyHostname1);
    componentStatus = new ArrayList<ComponentStatus>();
    dataNodeStatus = new ComponentStatus();
    dataNodeStatus.setClusterName(cluster.getClusterName());
    dataNodeStatus.setServiceName(HDFS);
    dataNodeStatus.setComponentName(DATANODE);
    dataNodeStatus.setStatus("INSTALLED");
    dataNodeStatus.setSecurityState(SecurityState.UNSECURED.name());
    componentStatus.add(dataNodeStatus);
    nameNodeStatus = new ComponentStatus();
    nameNodeStatus.setClusterName(cluster.getClusterName());
    nameNodeStatus.setServiceName(HDFS);
    nameNodeStatus.setComponentName(NAMENODE);
    nameNodeStatus.setStatus("STARTED");
    nameNodeStatus.setSecurityState(SecurityState.UNSECURED.name());
    componentStatus.add(nameNodeStatus);
    hb2.setComponentStatus(componentStatus);
    handler.handleHeartBeat(hb2);
    assertEquals(HostHealthStatus.HealthStatus.ALERT.name(), hostObject.getStatus());

    // mark the installed DN as maintenance
    hdfs.getServiceComponent(DATANODE).getServiceComponentHost(
        DummyHostname1).setMaintenanceState(MaintenanceState.ON);
    HeartBeat hb2a = new HeartBeat();
    hb2a.setResponseId(2);
    hb2a.setNodeStatus(new HostStatus(Status.HEALTHY, DummyHostStatus));
    hb2a.setHostname(DummyHostname1);
    componentStatus = new ArrayList<ComponentStatus>();
    dataNodeStatus = new ComponentStatus();
    dataNodeStatus.setClusterName(cluster.getClusterName());
    dataNodeStatus.setServiceName(HDFS);
    dataNodeStatus.setComponentName(DATANODE);
    dataNodeStatus.setStatus("INSTALLED");
    dataNodeStatus.setSecurityState(SecurityState.UNSECURED.name());
    componentStatus.add(dataNodeStatus);
    nameNodeStatus = new ComponentStatus();
    nameNodeStatus.setClusterName(cluster.getClusterName());
    nameNodeStatus.setServiceName(HDFS);
    nameNodeStatus.setComponentName(NAMENODE);
    nameNodeStatus.setStatus("STARTED");
    nameNodeStatus.setSecurityState(SecurityState.UNSECURED.name());
    componentStatus.add(nameNodeStatus);
    hb2a.setComponentStatus(componentStatus);
    handler.handleHeartBeat(hb2a);
    assertEquals(HostHealthStatus.HealthStatus.HEALTHY.name(), hostObject.getStatus());

    hdfs.getServiceComponent(DATANODE).getServiceComponentHost(
        DummyHostname1).setMaintenanceState(MaintenanceState.OFF);

    //Some masters are down
    HeartBeat hb3 = new HeartBeat();
    hb3.setResponseId(3);
    hb3.setNodeStatus(new HostStatus(Status.HEALTHY, DummyHostStatus));
    hb3.setHostname(DummyHostname1);
    componentStatus = new ArrayList<ComponentStatus>();
    dataNodeStatus = new ComponentStatus();
    dataNodeStatus.setClusterName(cluster.getClusterName());
    dataNodeStatus.setServiceName(HDFS);
    dataNodeStatus.setComponentName(DATANODE);
    dataNodeStatus.setStatus("INSTALLED");
    dataNodeStatus.setSecurityState(SecurityState.UNSECURED.name());
    componentStatus.add(dataNodeStatus);
    nameNodeStatus = new ComponentStatus();
    nameNodeStatus.setClusterName(cluster.getClusterName());
    nameNodeStatus.setServiceName(HDFS);
    nameNodeStatus.setComponentName(NAMENODE);
    nameNodeStatus.setStatus("INSTALLED");
    nameNodeStatus.setSecurityState(SecurityState.UNSECURED.name());
    componentStatus.add(nameNodeStatus);
    hb3.setComponentStatus(componentStatus);
    handler.handleHeartBeat(hb3);
    assertEquals(HostHealthStatus.HealthStatus.UNHEALTHY.name(), hostObject.getStatus());

    //All are up
    hb1.setResponseId(4);
    handler.handleHeartBeat(hb1);
    assertEquals(HostHealthStatus.HealthStatus.HEALTHY.name(), hostObject.getStatus());

    reset(am);
    expect(am.getTasks(anyObject(List.class))).andReturn(
            new ArrayList<HostRoleCommand>() {{
              add(command);
            }}).anyTimes();
    replay(am);

    //Only one component reported status
    hdfs.getServiceComponent(NAMENODE).getServiceComponentHost(DummyHostname1).setState(State.INSTALLED);
    HeartBeat hb4 = new HeartBeat();
    hb4.setResponseId(5);
    hb4.setNodeStatus(new HostStatus(Status.HEALTHY, DummyHostStatus));
    hb4.setHostname(DummyHostname1);
    componentStatus = new ArrayList<ComponentStatus>();
    dataNodeStatus = new ComponentStatus();
    dataNodeStatus.setClusterName(cluster.getClusterName());
    dataNodeStatus.setServiceName(HDFS);
    dataNodeStatus.setComponentName(DATANODE);
    dataNodeStatus.setStatus("STARTED");
    dataNodeStatus.setSecurityState(SecurityState.UNSECURED.name());
    componentStatus.add(dataNodeStatus);
    hb4.setComponentStatus(componentStatus);
    handler.handleHeartBeat(hb4);
    assertEquals(HostHealthStatus.HealthStatus.UNHEALTHY.name(), hostObject.getStatus());

    hb1.setResponseId(6);
    handler.handleHeartBeat(hb1);
    assertEquals(HostHealthStatus.HealthStatus.HEALTHY.name(), hostObject.getStatus());

    //Some command reports
    HeartBeat hb5 = new HeartBeat();
    hb5.setResponseId(7);
    hb5.setNodeStatus(new HostStatus(Status.HEALTHY, DummyHostStatus));
    hb5.setHostname(DummyHostname1);
    CommandReport cr1 = new CommandReport();
    cr1.setActionId(StageUtils.getActionId(requestId, stageId));
    cr1.setServiceName(HDFS);
    cr1.setTaskId(1);
    cr1.setRole(DATANODE);
    cr1.setStatus("COMPLETED");
    cr1.setStdErr("");
    cr1.setStdOut("");
    cr1.setExitCode(215);
    cr1.setRoleCommand("STOP");
    cr1.setClusterName(DummyCluster);
    ArrayList<CommandReport> reports = new ArrayList<CommandReport>();
    reports.add(cr1);
    hb5.setReports(reports);
    handler.handleHeartBeat(hb5);
    assertEquals(HostHealthStatus.HealthStatus.ALERT.name(), hostObject.getStatus());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testIgnoreCustomActionReport() throws AmbariException, InvalidStateTransitionException {
    CommandReport cr1 = new CommandReport();
    cr1.setActionId(StageUtils.getActionId(requestId, stageId));
    cr1.setTaskId(1);
    cr1.setClusterName(DummyCluster);
    cr1.setServiceName(HDFS);
    cr1.setRole(NAMENODE);
    cr1.setStatus(HostRoleStatus.FAILED.toString());
    cr1.setRoleCommand("CUSTOM_COMMAND");
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
    cr2.setRoleCommand("ACTIONEXECUTE");
    cr2.setStdErr("none");
    cr2.setStdOut("dummy output");
    cr2.setExitCode(0);

    ArrayList<CommandReport> reports = new ArrayList<CommandReport>();
    reports.add(cr1);
    reports.add(cr2);

    HeartBeat hb = new HeartBeat();
    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(0);
    hb.setHostname(DummyHostname1);
    hb.setNodeStatus(new HostStatus(Status.HEALTHY, DummyHostStatus));
    hb.setReports(reports);

    ActionQueue aq = new ActionQueue();

    final HostRoleCommand command = hostRoleCommandFactory.create(DummyHostname1,
            Role.DATANODE, null, null);

    ActionManager am = getMockActionManager();
    expect(am.getTasks(anyObject(List.class))).andReturn(
            new ArrayList<HostRoleCommand>() {{
              add(command);
              add(command);
            }});
    replay(am);

    HeartBeatHandler handler = getHeartBeatHandler(am, aq);

    // CUSTOM_COMMAND and ACTIONEXECUTE reports are ignored
    // they should not change the host component state
    try {
      handler.handleHeartBeat(hb);
    } catch (Exception e) {
      fail();
    }

  }

  @Test
  public void testComponents() throws AmbariException,
      InvalidStateTransitionException {
    ComponentsResponse expected = new ComponentsResponse();
    StackId dummyStackId = new StackId(DummyStackId);
    Map<String, Map<String, String>> dummyComponents = new HashMap<String, Map<String, String>>();

    Map<String, String> dummyCategoryMap = new HashMap<String, String>();
    dummyCategoryMap.put("PIG", "CLIENT");
    dummyComponents.put("PIG", dummyCategoryMap);

    dummyCategoryMap = new HashMap<String, String>();
    dummyCategoryMap.put("MAPREDUCE_CLIENT", "CLIENT");
    dummyCategoryMap.put("JOBTRACKER", "MASTER");
    dummyCategoryMap.put("TASKTRACKER", "SLAVE");
    dummyComponents.put("MAPREDUCE", dummyCategoryMap);

    dummyCategoryMap = new HashMap<String, String>();
    dummyCategoryMap.put("DATANODE2", "SLAVE");
    dummyCategoryMap.put("NAMENODE", "MASTER");
    dummyCategoryMap.put("HDFS_CLIENT", "CLIENT");
    dummyCategoryMap.put("DATANODE1", "SLAVE");
    dummyCategoryMap.put("SECONDARY_NAMENODE", "MASTER");
    dummyCategoryMap.put("DATANODE", "SLAVE");
    dummyComponents.put("HDFS", dummyCategoryMap);

    expected.setClusterName(DummyCluster);
    expected.setStackName(dummyStackId.getStackName());
    expected.setStackVersion(dummyStackId.getStackVersion());
    expected.setComponents(dummyComponents);

    getDummyCluster();
    HeartBeatHandler handler = getHeartBeatHandler(getMockActionManager(),
        new ActionQueue());

    ComponentsResponse actual = handler.handleComponents(DummyCluster);

    if (log.isDebugEnabled()) {
      log.debug(actual.toString());
    }

    assertEquals(expected.getClusterName(), actual.getClusterName());
    assertEquals(expected.getStackName(), actual.getStackName());
    assertEquals(expected.getStackVersion(), actual.getStackVersion());
    assertEquals(expected.getComponents(), actual.getComponents());
  }

  private ActionManager getMockActionManager() {
    ActionQueue actionQueueMock = createNiceMock(ActionQueue.class);
    Clusters clustersMock = createNiceMock(Clusters.class);
    Configuration configurationMock = createNiceMock(Configuration.class);

    ActionManager actionManager = createMockBuilder(ActionManager.class).
            addMockedMethod("getTasks").
            withConstructor((long)0, (long)0, actionQueueMock, clustersMock,
                    actionDBAccessor, new HostsMap((String) null), unitOfWork,
                    injector.getInstance(RequestFactory.class), configurationMock, createNiceMock(AmbariEventPublisher.class)).
            createMock();
    return actionManager;
  }


  private ComponentStatus createComponentStatus(String clusterName, String serviceName, String message,
                                                State state, SecurityState securityState,
                                                String componentName, String stackVersion) {
    ComponentStatus componentStatus1 = new ComponentStatus();
    componentStatus1.setClusterName(clusterName);
    componentStatus1.setServiceName(serviceName);
    componentStatus1.setMessage(message);
    componentStatus1.setStatus(state.name());
    componentStatus1.setSecurityState(securityState.name());
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
    StackEntity stackEntity = stackDAO.find(HDP_22_STACK.getStackName(), HDP_22_STACK.getStackVersion());
    org.junit.Assert.assertNotNull(stackEntity);

    // Create the cluster
    ResourceTypeEntity resourceTypeEntity = new ResourceTypeEntity();
    resourceTypeEntity.setId(ResourceTypeEntity.CLUSTER_RESOURCE_TYPE);
    resourceTypeEntity.setName(ResourceTypeEntity.CLUSTER_RESOURCE_TYPE_NAME);
    resourceTypeEntity = resourceTypeDAO.merge(resourceTypeEntity);

    ResourceEntity resourceEntity = new ResourceEntity();
    resourceEntity.setResourceType(resourceTypeEntity);

    ClusterEntity clusterEntity = new ClusterEntity();
    clusterEntity.setClusterName(DummyCluster);
    clusterEntity.setClusterInfo("test_cluster_info1");
    clusterEntity.setResource(resourceEntity);
    clusterEntity.setDesiredStack(stackEntity);

    clusterDAO.create(clusterEntity);

    StackId stackId = new StackId(DummyStackId);

    Cluster cluster = clusters.getCluster(DummyCluster);

    cluster.setDesiredStackVersion(stackId);
    cluster.setCurrentStackVersion(stackId);
    helper.getOrCreateRepositoryVersion(stackId, stackId.getStackVersion());
    cluster.createClusterVersion(stackId, stackId.getStackVersion(), "admin",
        RepositoryVersionState.UPGRADING);

    Set<String> hostNames = new HashSet<String>(){{
      add(DummyHostname1);
    }};

    Map<String, String> hostAttributes = new HashMap<String, String>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "6.3");

    List<HostEntity> hostEntities = new ArrayList<HostEntity>();
    for(String hostName : hostNames) {
      clusters.addHost(hostName);
      Host host = clusters.getHost(hostName);
      host.setHostAttributes(hostAttributes);
      host.persist();

      HostEntity hostEntity = hostDAO.findByName(hostName);
      Assert.assertNotNull(hostEntity);
      hostEntities.add(hostEntity);
    }
    clusterEntity.setHostEntities(hostEntities);
    clusters.mapHostsToCluster(hostNames, DummyCluster);

    return cluster;
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testCommandStatusProcesses() throws Exception {
    Cluster cluster = getDummyCluster();
    Service hdfs = cluster.addService(HDFS);
    hdfs.persist();
    hdfs.addServiceComponent(DATANODE).persist();
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1).persist();
    hdfs.getServiceComponent(DATANODE).getServiceComponentHost(DummyHostname1).setState(State.STARTED);

    ActionQueue aq = new ActionQueue();

    HeartBeat hb = new HeartBeat();
    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(0);
    hb.setHostname(DummyHostname1);
    hb.setNodeStatus(new HostStatus(Status.HEALTHY, DummyHostStatus));
    hb.setReports(new ArrayList<CommandReport>());

    List<Map<String, String>> procs = new ArrayList<Map<String, String>>();
    Map<String, String> proc1info = new HashMap<String, String>();
    proc1info.put("name", "a");
    proc1info.put("status", "RUNNING");
    procs.add(proc1info);

    Map<String, String> proc2info = new HashMap<String, String>();
    proc2info.put("name", "b");
    proc2info.put("status", "NOT_RUNNING");
    procs.add(proc2info);

    Map<String, Object> extra = new HashMap<String, Object>();
    extra.put("processes", procs);

    ArrayList<ComponentStatus> componentStatuses = new ArrayList<ComponentStatus>();
    ComponentStatus componentStatus1 = new ComponentStatus();
    componentStatus1.setClusterName(DummyCluster);
    componentStatus1.setServiceName(HDFS);
    componentStatus1.setMessage(DummyHostStatus);
    componentStatus1.setStatus(State.STARTED.name());
    componentStatus1.setSecurityState(SecurityState.UNSECURED.name());
    componentStatus1.setComponentName(DATANODE);

    componentStatus1.setExtra(extra);
    componentStatuses.add(componentStatus1);
    hb.setComponentStatus(componentStatuses);

    final HostRoleCommand command = hostRoleCommandFactory.create(DummyHostname1,
            Role.DATANODE, null, null);

    ActionManager am = getMockActionManager();
    expect(am.getTasks(anyObject(List.class))).andReturn(
            new ArrayList<HostRoleCommand>() {{
              add(command);
            }}).anyTimes();
    replay(am);

    HeartBeatHandler handler = getHeartBeatHandler(am, aq);
    handler.handleHeartBeat(hb);
    ServiceComponentHost sch = hdfs.getServiceComponent(DATANODE).getServiceComponentHost(DummyHostname1);

    Assert.assertEquals(Integer.valueOf(2), Integer.valueOf(sch.getProcesses().size()));

    hb = new HeartBeat();
    hb.setTimestamp(System.currentTimeMillis());
    hb.setResponseId(1);
    hb.setHostname(DummyHostname1);
    hb.setNodeStatus(new HostStatus(Status.HEALTHY, DummyHostStatus));
    hb.setReports(new ArrayList<CommandReport>());

    componentStatus1 = new ComponentStatus();
    componentStatus1.setClusterName(DummyCluster);
    componentStatus1.setServiceName(HDFS);
    componentStatus1.setMessage(DummyHostStatus);
    componentStatus1.setStatus(State.STARTED.name());
    componentStatus1.setSecurityState(SecurityState.UNSECURED.name());
    componentStatus1.setComponentName(DATANODE);
    hb.setComponentStatus(Collections.singletonList(componentStatus1));

    handler.handleHeartBeat(hb);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testCommandStatusProcesses_empty() throws Exception {
    Cluster cluster = getDummyCluster();
    Service hdfs = cluster.addService(HDFS);
    hdfs.persist();
    hdfs.addServiceComponent(DATANODE).persist();
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1).persist();
    hdfs.getServiceComponent(DATANODE).getServiceComponentHost(DummyHostname1).setState(State.STARTED);

    ActionQueue aq = new ActionQueue();
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
    componentStatus1.setSecurityState(SecurityState.UNSECURED.name());
    componentStatus1.setComponentName(DATANODE);

    componentStatuses.add(componentStatus1);
    hb.setComponentStatus(componentStatuses);

    final HostRoleCommand command = hostRoleCommandFactory.create(DummyHostname1,
            Role.DATANODE, null, null);

    ActionManager am = getMockActionManager();
    expect(am.getTasks(anyObject(List.class))).andReturn(
            new ArrayList<HostRoleCommand>() {{
              add(command);
            }});
    replay(am);

    HeartBeatHandler handler = getHeartBeatHandler(am, aq);
    ServiceComponentHost sch = hdfs.getServiceComponent(DATANODE).getServiceComponentHost(DummyHostname1);

    Assert.assertEquals(Integer.valueOf(0), Integer.valueOf(sch.getProcesses().size()));
  }

  /**
   * Tests that if there is an invalid cluster in heartbeat data, the heartbeat
   * doesn't fail.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("unchecked")
  public void testHeartBeatWithAlertAndInvalidCluster() throws Exception {
    ActionManager am = getMockActionManager();

    expect(am.getTasks(anyObject(List.class))).andReturn(
        new ArrayList<HostRoleCommand>());

    replay(am);

    Cluster cluster = getDummyCluster();
    Clusters fsm = clusters;
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
    execCmd.setRequestAndStage(2, 34);
    execCmd.setHostname(DummyHostname1);
    aq.enqueue(DummyHostname1, new ExecutionCommand());

    HeartBeat hb = new HeartBeat();
    HostStatus hs = new HostStatus(Status.HEALTHY, DummyHostStatus);

    hb.setResponseId(0);
    hb.setNodeStatus(hs);
    hb.setHostname(DummyHostname1);

    Alert alert = new Alert("foo", "bar", "baz", "foobar", "foobarbaz",
        AlertState.OK);

    alert.setCluster("BADCLUSTER");

    List<Alert> alerts = Collections.singletonList(alert);
    hb.setAlerts(alerts);

    // should NOT throw AmbariException from alerts.
    handler.handleHeartBeat(hb);
  }

  @Test
  public void testInstallPackagesWithVersion() throws Exception {
    // required since this test method checks the DAO result of handling a
    // heartbeat which performs some async tasks
    EventBusSynchronizer.synchronizeAmbariEventPublisher(injector);

    final HostRoleCommand command = hostRoleCommandFactory.create(DummyHostname1,
        Role.DATANODE, null, null);

    ActionManager am = getMockActionManager();
    expect(am.getTasks(anyObject(List.class))).andReturn(
        Collections.singletonList(command)).anyTimes();
    replay(am);

    Cluster cluster = getDummyCluster();
    HeartBeatHandler handler = getHeartBeatHandler(am, new ActionQueue());
    HeartBeat hb = new HeartBeat();

    JsonObject json = new JsonObject();
    json.addProperty("actual_version", "2.2.1.0-2222");
    json.addProperty("package_installation_result", "SUCCESS");
    json.addProperty("installed_repository_version", "0.1");
    json.addProperty("stack_id", cluster.getDesiredStackVersion().getStackId());


    CommandReport cmdReport = new CommandReport();
    cmdReport.setActionId(StageUtils.getActionId(requestId, stageId));
    cmdReport.setTaskId(1);
    cmdReport.setCustomCommand("install_packages");
    cmdReport.setStructuredOut(json.toString());
    cmdReport.setRoleCommand(RoleCommand.ACTIONEXECUTE.name());
    cmdReport.setStatus(HostRoleStatus.COMPLETED.name());
    cmdReport.setRole("install_packages");
    cmdReport.setClusterName(DummyCluster);

    hb.setReports(Collections.singletonList(cmdReport));
    hb.setTimestamp(0L);
    hb.setResponseId(0);
    hb.setNodeStatus(new HostStatus(Status.HEALTHY, DummyHostStatus));
    hb.setHostname(DummyHostname1);
    hb.setComponentStatus(new ArrayList<ComponentStatus>());

    StackId stackId = new StackId("HDP", "0.1");

    RepositoryVersionDAO dao = injector.getInstance(RepositoryVersionDAO.class);
    RepositoryVersionEntity entity = dao.findByStackAndVersion(stackId, "0.1");
    Assert.assertNotNull(entity);

    handler.handleHeartBeat(hb);

    entity = dao.findByStackAndVersion(stackId, "0.1");
    Assert.assertNull(entity);

    entity = dao.findByStackAndVersion(stackId, "2.2.1.0-2222");
    Assert.assertNotNull(entity);
  }

  @Test
  public void testInjectKeytabApplicableHost() throws Exception {
    List<Map<String, String>> kcp;
    Map<String, String> properties;

    kcp = testInjectKeytabSetKeytab("c6403.ambari.apache.org");
    Assert.assertNotNull(kcp);
    Assert.assertEquals(1, kcp.size());

    properties = kcp.get(0);
    Assert.assertNotNull(properties);
    Assert.assertEquals("c6403.ambari.apache.org", properties.get(KerberosIdentityDataFileWriter.HOSTNAME));
    Assert.assertEquals("HDFS", properties.get(KerberosIdentityDataFileWriter.SERVICE));
    Assert.assertEquals("DATANODE", properties.get(KerberosIdentityDataFileWriter.COMPONENT));
    Assert.assertEquals("dn/_HOST@_REALM", properties.get(KerberosIdentityDataFileWriter.PRINCIPAL));
    Assert.assertEquals("/etc/security/keytabs/dn.service.keytab", properties.get(KerberosIdentityDataFileWriter.KEYTAB_FILE_PATH));
    Assert.assertEquals("hdfs", properties.get(KerberosIdentityDataFileWriter.KEYTAB_FILE_OWNER_NAME));
    Assert.assertEquals("r", properties.get(KerberosIdentityDataFileWriter.KEYTAB_FILE_OWNER_ACCESS));
    Assert.assertEquals("hadoop", properties.get(KerberosIdentityDataFileWriter.KEYTAB_FILE_GROUP_NAME));
    Assert.assertEquals("", properties.get(KerberosIdentityDataFileWriter.KEYTAB_FILE_GROUP_ACCESS));

    Assert.assertEquals(Base64.encodeBase64String("hello".getBytes()), kcp.get(0).get(KerberosServerAction.KEYTAB_CONTENT_BASE64));


    kcp = testInjectKeytabRemoveKeytab("c6403.ambari.apache.org");

    Assert.assertNotNull(kcp);
    Assert.assertEquals(1, kcp.size());

    properties = kcp.get(0);
    Assert.assertNotNull(properties);
    Assert.assertEquals("c6403.ambari.apache.org", properties.get(KerberosIdentityDataFileWriter.HOSTNAME));
    Assert.assertEquals("HDFS", properties.get(KerberosIdentityDataFileWriter.SERVICE));
    Assert.assertEquals("DATANODE", properties.get(KerberosIdentityDataFileWriter.COMPONENT));
    Assert.assertEquals("dn/_HOST@_REALM", properties.get(KerberosIdentityDataFileWriter.PRINCIPAL));
    Assert.assertEquals("/etc/security/keytabs/dn.service.keytab", properties.get(KerberosIdentityDataFileWriter.KEYTAB_FILE_PATH));
    Assert.assertFalse(properties.containsKey(KerberosIdentityDataFileWriter.KEYTAB_FILE_OWNER_NAME));
    Assert.assertFalse(properties.containsKey(KerberosIdentityDataFileWriter.KEYTAB_FILE_OWNER_ACCESS));
    Assert.assertFalse(properties.containsKey(KerberosIdentityDataFileWriter.KEYTAB_FILE_GROUP_NAME));
    Assert.assertFalse(properties.containsKey(KerberosIdentityDataFileWriter.KEYTAB_FILE_GROUP_ACCESS));
    Assert.assertFalse(properties.containsKey(KerberosServerAction.KEYTAB_CONTENT_BASE64));
  }

  @Test
  public void testInjectKeytabNotApplicableHost() throws Exception {
    List<Map<String, String>> kcp;
    kcp = testInjectKeytabSetKeytab("c6401.ambari.apache.org");
    Assert.assertNotNull(kcp);
    Assert.assertTrue(kcp.isEmpty());

    kcp = testInjectKeytabRemoveKeytab("c6401.ambari.apache.org");
    Assert.assertNotNull(kcp);
    Assert.assertTrue(kcp.isEmpty());
  }

  private List<Map<String, String>> testInjectKeytabSetKeytab(String targetHost) throws Exception {

    ExecutionCommand executionCommand = new ExecutionCommand();

    Map<String, String> hlp = new HashMap<String, String>();
    hlp.put("custom_command", "SET_KEYTAB");
    executionCommand.setHostLevelParams(hlp);

    Map<String, String> commandparams = new HashMap<String, String>();
    commandparams.put(KerberosServerAction.AUTHENTICATED_USER_NAME, "admin");
    commandparams.put(KerberosServerAction.DATA_DIRECTORY, createTestKeytabData().getAbsolutePath());
    executionCommand.setCommandParams(commandparams);

    ActionQueue aq = new ActionQueue();

    final HostRoleCommand command = hostRoleCommandFactory.create(DummyHostname1,
        Role.DATANODE, null, null);

    ActionManager am = getMockActionManager();
    expect(am.getTasks(anyObject(List.class))).andReturn(
        new ArrayList<HostRoleCommand>() {{
          add(command);
        }});
    replay(am);

    getHeartBeatHandler(am, aq).injectKeytab(executionCommand, "SET_KEYTAB", targetHost);

    return executionCommand.getKerberosCommandParams();
  }


  private List<Map<String, String>> testInjectKeytabRemoveKeytab(String targetHost) throws Exception {

    ExecutionCommand executionCommand = new ExecutionCommand();

    Map<String, String> hlp = new HashMap<String, String>();
    hlp.put("custom_command", "REMOVE_KEYTAB");
    executionCommand.setHostLevelParams(hlp);

    Map<String, String> commandparams = new HashMap<String, String>();
    commandparams.put(KerberosServerAction.AUTHENTICATED_USER_NAME, "admin");
    commandparams.put(KerberosServerAction.DATA_DIRECTORY, createTestKeytabData().getAbsolutePath());
    executionCommand.setCommandParams(commandparams);

    ActionQueue aq = new ActionQueue();

    final HostRoleCommand command = hostRoleCommandFactory.create(DummyHostname1,
        Role.DATANODE, null, null);

    ActionManager am = getMockActionManager();
    expect(am.getTasks(anyObject(List.class))).andReturn(
        new ArrayList<HostRoleCommand>() {{
          add(command);
        }});
    replay(am);

    getHeartBeatHandler(am, aq).injectKeytab(executionCommand, "REMOVE_KEYTAB", targetHost);

    return executionCommand.getKerberosCommandParams();
  }


  private File createTestKeytabData() throws Exception {
    File dataDirectory = temporaryFolder.newFolder();
    File identityDataFile = new File(dataDirectory, KerberosIdentityDataFileWriter.DATA_FILE_NAME);
    KerberosIdentityDataFileWriter kerberosIdentityDataFileWriter = injector.getInstance(KerberosIdentityDataFileWriterFactory.class).createKerberosIdentityDataFileWriter(identityDataFile);
    File hostDirectory = new File(dataDirectory, "c6403.ambari.apache.org");

    File keytabFile;
    if(hostDirectory.mkdirs()) {
      keytabFile = new File(hostDirectory, DigestUtils.sha1Hex("/etc/security/keytabs/dn.service.keytab"));
    } else {
      throw new Exception("Failed to create " + hostDirectory.getAbsolutePath());
    }

    kerberosIdentityDataFileWriter.writeRecord("c6403.ambari.apache.org", "HDFS", "DATANODE",
        "dn/_HOST@_REALM", "service",
        "/etc/security/keytabs/dn.service.keytab",
        "hdfs", "r", "hadoop", "", "false");

    kerberosIdentityDataFileWriter.close();

    // Ensure the host directory exists...
    FileWriter fw = new FileWriter(keytabFile);
    BufferedWriter bw = new BufferedWriter(fw);
    bw.write("hello");
    bw.close();

    return dataDirectory;
  }

}
