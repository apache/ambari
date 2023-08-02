/*
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
package org.apache.ambari.server.agent.stomp;

import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DATANODE;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyCluster;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyClusterId;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyHostname1;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.HDFS;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.NAMENODE;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.SECONDARY_NAMENODE;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.ActionDBAccessor;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.ActionManagerTestHelper;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.actionmanager.Request;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.agent.AgentEnv;
import org.apache.ambari.server.agent.AgentReportsProcessor;
import org.apache.ambari.server.agent.AgentSessionManager;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.agent.DiskInfo;
import org.apache.ambari.server.agent.HeartBeat;
import org.apache.ambari.server.agent.HeartbeatTestHelper;
import org.apache.ambari.server.agent.Register;
import org.apache.ambari.server.agent.stomp.dto.CommandStatusReports;
import org.apache.ambari.server.agent.stomp.dto.ComponentStatusReport;
import org.apache.ambari.server.agent.stomp.dto.ComponentStatusReports;
import org.apache.ambari.server.agent.stomp.dto.ComponentVersionReport;
import org.apache.ambari.server.agent.stomp.dto.ComponentVersionReports;
import org.apache.ambari.server.agent.stomp.dto.HostStatusReport;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.audit.AuditLogger;
import org.apache.ambari.server.events.publishers.STOMPUpdatePublisher;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostInstallEvent;
import org.apache.ambari.server.utils.EventBusSynchronizer;
import org.apache.ambari.server.utils.StageUtils;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import junit.framework.Assert;

public class AgentReportsControllerTest {

  private static final Logger LOG = LoggerFactory.getLogger(AgentReportsControllerTest.class);
  private Injector injector;
  private Clusters clusters;
  private AgentReportsController agentReportsController;
  private AgentReportsProcessor agentReportsProcessor;
  private HeartbeatController heartbeatController;
  long requestId = 23;
  long stageId = 31;

  @Inject
  private AmbariMetaInfo metaInfo;

  @Inject
  private ActionDBAccessor actionDBAccessor;

  @Inject
  private StageFactory stageFactory;

  @Inject
  private HostRoleCommandFactory hostRoleCommandFactory;

  @Inject
  private HeartbeatTestHelper heartbeatTestHelper;

  @Inject
  private ActionManagerTestHelper actionManagerTestHelper;

  @Inject
  private AuditLogger auditLogger;

  @Inject
  private OrmTestHelper helper;

  private InMemoryDefaultTestModule module;

  @Before
  public void setup() throws Exception {
    module = HeartbeatTestHelper.getTestModule();
    injector = Guice.createInjector(module);
    injector.getInstance(GuiceJpaInitializer.class);
    clusters = injector.getInstance(Clusters.class);
    injector.injectMembers(this);
    agentReportsController = new AgentReportsController(injector);
    heartbeatController = new HeartbeatController(injector);
    agentReportsProcessor = injector.getInstance(AgentReportsProcessor.class);
    EasyMock.replay(auditLogger, injector.getInstance(STOMPUpdatePublisher.class));

    EventBusSynchronizer.synchronizeAmbariEventPublisher(injector);
    EventBusSynchronizer.synchronizeAlertEventPublisher(injector);
    EventBusSynchronizer.synchronizeSTOMPUpdatePublisher(injector);

    heartbeatTestHelper.injectAgentsRegistrationQueue(heartbeatController);
  }

  @After
  public void teardown() throws Exception {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
    EasyMock.reset(auditLogger);
  }

  @Test
  public void testHandleComponentReportStatus() throws Exception {
    Cluster cluster = heartbeatTestHelper.getDummyCluster();

    AgentSessionManager agentSessionManager = injector.getInstance(AgentSessionManager.class);
    for (Host host : cluster.getHosts()) {
      agentSessionManager.register(host.getHostName(), host);
    }

    Service hdfs = heartbeatTestHelper.addService(cluster, HDFS);
    hdfs.addServiceComponent(DATANODE);
    hdfs.getServiceComponent(DATANODE).
        addServiceComponentHost(DummyHostname1);
    hdfs.addServiceComponent(NAMENODE);
    hdfs.getServiceComponent(NAMENODE).
        addServiceComponentHost(DummyHostname1);

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

    TreeMap<String, List<ComponentStatusReport>> componentStatusReportsMap = new TreeMap<>();
    ComponentStatusReport statusReport1 = new ComponentStatusReport();
    statusReport1.setClusterId(cluster.getClusterId());
    statusReport1.setServiceName(HDFS);
    statusReport1.setCommand(null);
    statusReport1.setStatus(State.STARTED.name());
    statusReport1.setComponentName(DATANODE);

    ComponentStatusReport statusReport2 = new ComponentStatusReport();
    statusReport2.setClusterId(cluster.getClusterId());
    statusReport2.setServiceName(HDFS);
    statusReport2.setCommand(null);
    statusReport2.setStatus(State.INSTALLED.name());
    statusReport2.setComponentName(NAMENODE);

    componentStatusReportsMap.put(DummyCluster, Arrays.asList(statusReport1, statusReport2));

    ComponentStatusReports componentStatusReports = new ComponentStatusReports(componentStatusReportsMap);

    final HostRoleCommand command = hostRoleCommandFactory.create(DummyHostname1,
        Role.DATANODE, null, null);

    ActionManager am = actionManagerTestHelper.getMockActionManager();
    expect(am.getTasks(EasyMock.<List<Long>>anyObject())).andReturn(
        new ArrayList<HostRoleCommand>() {{
          add(command);
          add(command);
        }});
    replay(am);

    agentReportsController.handleComponentReportStatus(DummyHostname1, componentStatusReports);
    waitForReportsProcessing(agentReportsProcessor);

    State componentState1 = serviceComponentHost1.getState();
    State componentState2 = serviceComponentHost2.getState();
    assertEquals(State.STARTED, componentState1);
    assertEquals(State.INSTALLED, componentState2);
  }

  @Test
  public void testHandleCommandReportStatus() throws Exception {
    Cluster cluster = heartbeatTestHelper.getDummyCluster();

    AgentSessionManager agentSessionManager = injector.getInstance(AgentSessionManager.class);
    for (Host host : cluster.getHosts()) {
      agentSessionManager.register(host.getHostName(), host);
    }

    Service hdfs = heartbeatTestHelper.addService(cluster, HDFS);
    hdfs.addServiceComponent(DATANODE);
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1);
    hdfs.addServiceComponent(NAMENODE);
    hdfs.getServiceComponent(NAMENODE).addServiceComponentHost(DummyHostname1);
    hdfs.addServiceComponent(SECONDARY_NAMENODE);
    hdfs.getServiceComponent(SECONDARY_NAMENODE).addServiceComponentHost(DummyHostname1);

    ServiceComponentHost serviceComponentHost1 = clusters.getCluster(DummyCluster).getService(HDFS).
        getServiceComponent(DATANODE).getServiceComponentHost(DummyHostname1);
    serviceComponentHost1.setState(State.INSTALLING);

    CommandReport commandReport = new CommandReport();
    commandReport.setActionId(StageUtils.getActionId(requestId, stageId));
    commandReport.setTaskId(1);
    commandReport.setClusterId(DummyClusterId);
    commandReport.setServiceName(HDFS);
    commandReport.setRole(DATANODE);
    commandReport.setRoleCommand("INSTALL");
    commandReport.setStatus("IN_PROGRESS");
    commandReport.setStdErr("none");
    commandReport.setStdOut("dummy output");
    commandReport.setExitCode(777);

    TreeMap<String, List<CommandReport>> clustersComponentReports = new TreeMap<>();
    clustersComponentReports.put(DummyCluster, Collections.singletonList(commandReport));

    CommandStatusReports commandStatusReports = new CommandStatusReports();
    commandStatusReports.setClustersComponentReports(clustersComponentReports);

    final HostRoleCommand command = hostRoleCommandFactory.create(DummyHostname1,
        Role.DATANODE, null, RoleCommand.INSTALL);

    ActionManager am = actionManagerTestHelper.getMockActionManager();
    expect(am.getTasks(EasyMock.<List<Long>>anyObject())).andReturn(
        new ArrayList<HostRoleCommand>() {{
          add(command);
        }});
    replay(am);

    agentReportsController.handleCommandReportStatus(DummyHostname1, commandStatusReports);
    waitForReportsProcessing(agentReportsProcessor);

    State componentState1 = serviceComponentHost1.getState();
    assertEquals("Host state should still be installing", State.INSTALLING, componentState1);
  }

  @Test
  public void testHandleCommandReportStatusOPFailedEventForAbortedTask() throws Exception {
    Cluster cluster = heartbeatTestHelper.getDummyCluster();

    AgentSessionManager agentSessionManager = injector.getInstance(AgentSessionManager.class);
    for (Host host : cluster.getHosts()) {
      agentSessionManager.register(host.getHostName(), host);
    }

    Service hdfs = heartbeatTestHelper.addService(cluster, HDFS);
    hdfs.addServiceComponent(DATANODE);
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1);
    hdfs.addServiceComponent(NAMENODE);
    hdfs.getServiceComponent(NAMENODE).addServiceComponentHost(DummyHostname1);
    hdfs.addServiceComponent(SECONDARY_NAMENODE);
    hdfs.getServiceComponent(SECONDARY_NAMENODE).addServiceComponentHost(DummyHostname1);

    ServiceComponentHost serviceComponentHost1 = clusters.getCluster(DummyCluster).getService(HDFS).
        getServiceComponent(DATANODE).getServiceComponentHost(DummyHostname1);
    serviceComponentHost1.setState(State.INSTALLING);

    Stage s = stageFactory.createNew(1, "/a/b", "cluster1", 1L, "action manager test",
        "commandParamsStage", "hostParamsStage");
    s.setStageId(1);
    s.addHostRoleExecutionCommand(DummyHostname1, Role.DATANODE, RoleCommand.INSTALL,
        new ServiceComponentHostInstallEvent(Role.DATANODE.toString(),
            DummyHostname1, System.currentTimeMillis(), "HDP-1.3.0"),
        DummyCluster, "HDFS", false, false);
    List<Stage> stages = new ArrayList<>();
    stages.add(s);
    Request request = new Request(stages, "clusterHostInfo", clusters);
    actionDBAccessor.persistActions(request);
    actionDBAccessor.abortHostRole(DummyHostname1, 1, 1, Role.DATANODE.name());

    CommandReport commandReport = new CommandReport();
    commandReport.setActionId(StageUtils.getActionId(1, 1));
    commandReport.setTaskId(1);
    commandReport.setClusterId(DummyClusterId);
    commandReport.setServiceName(HDFS);
    commandReport.setRole(DATANODE);
    commandReport.setRoleCommand("INSTALL");
    commandReport.setStatus("FAILED");
    commandReport.setStdErr("none");
    commandReport.setStdOut("dummy output");
    commandReport.setExitCode(777);

    TreeMap<String, List<CommandReport>> clustersComponentReports = new TreeMap<>();
    clustersComponentReports.put(DummyCluster, Collections.singletonList(commandReport));

    CommandStatusReports commandStatusReports = new CommandStatusReports();
    commandStatusReports.setClustersComponentReports(clustersComponentReports);

    final HostRoleCommand command = hostRoleCommandFactory.create(DummyHostname1,
        Role.DATANODE, null, null);

    ActionManager am = actionManagerTestHelper.getMockActionManager();
    expect(am.getTasks(EasyMock.<List<Long>>anyObject())).andReturn(
        new ArrayList<HostRoleCommand>() {{
          add(command);
        }});
    replay(am);

    agentReportsController.handleCommandReportStatus(DummyHostname1, commandStatusReports);
    waitForReportsProcessing(agentReportsProcessor);

    State componentState1 = serviceComponentHost1.getState();
    assertEquals("Host state should still be installing", State.INSTALLING,
        componentState1);
  }

  @Test
  public void testCommandStatusProcesses_empty() throws Exception {
    Cluster cluster = heartbeatTestHelper.getDummyCluster();

    AgentSessionManager agentSessionManager = injector.getInstance(AgentSessionManager.class);
    for (Host host : cluster.getHosts()) {
      agentSessionManager.register(host.getHostName(), host);
    }

    Service hdfs = heartbeatTestHelper.addService(cluster, HDFS);
    hdfs.addServiceComponent(DATANODE);
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1);
    hdfs.getServiceComponent(DATANODE).getServiceComponentHost(DummyHostname1).setState(State.STARTED);

    final HostRoleCommand command = hostRoleCommandFactory.create(DummyHostname1,
        Role.DATANODE, null, null);

    ActionManager am = actionManagerTestHelper.getMockActionManager();
    expect(am.getTasks(EasyMock.<List<Long>>anyObject())).andReturn(
        new ArrayList<HostRoleCommand>() {{
          add(command);
        }});
    replay(am);

    ServiceComponentHost sch = hdfs.getServiceComponent(DATANODE).getServiceComponentHost(DummyHostname1);

    Assert.assertEquals(Integer.valueOf(0), Integer.valueOf(sch.getProcesses().size()));
  }

  @Test
  public void testIgnoreCustomActionReport() throws Exception {
    Cluster cluster = heartbeatTestHelper.getDummyCluster();

    AgentSessionManager agentSessionManager = injector.getInstance(AgentSessionManager.class);
    for (Host host : cluster.getHosts()) {
      agentSessionManager.register(host.getHostName(), host);
    }

    CommandReport commandReport1 = new CommandReport();
    commandReport1.setActionId(StageUtils.getActionId(requestId, stageId));
    commandReport1.setTaskId(1);
    commandReport1.setClusterId(DummyClusterId);
    commandReport1.setServiceName(HDFS);
    commandReport1.setRole(NAMENODE);
    commandReport1.setStatus(HostRoleStatus.FAILED.toString());
    commandReport1.setRoleCommand("CUSTOM_COMMAND");
    commandReport1.setStdErr("none");
    commandReport1.setStdOut("dummy output");
    commandReport1.setExitCode(0);

    CommandReport commandReport2 = new CommandReport();
    commandReport2.setActionId(StageUtils.getActionId(requestId, stageId));
    commandReport2.setTaskId(2);
    commandReport2.setClusterId(DummyClusterId);
    commandReport2.setServiceName(HDFS);
    commandReport2.setRole(NAMENODE);
    commandReport2.setStatus(HostRoleStatus.FAILED.toString());
    commandReport2.setRoleCommand("ACTIONEXECUTE");
    commandReport2.setStdErr("none");
    commandReport2.setStdOut("dummy output");
    commandReport2.setExitCode(0);

    TreeMap<String, List<CommandReport>> clustersComponentReports = new TreeMap<>();
    clustersComponentReports.put(DummyCluster, Arrays.asList(commandReport1, commandReport2));

    CommandStatusReports commandStatusReports = new CommandStatusReports();
    commandStatusReports.setClustersComponentReports(clustersComponentReports);

    final HostRoleCommand command = hostRoleCommandFactory.create(DummyHostname1,
        Role.DATANODE, null, null);

    ActionManager am = actionManagerTestHelper.getMockActionManager();
    expect(am.getTasks(EasyMock.<List<Long>>anyObject())).andReturn(
        new ArrayList<HostRoleCommand>() {{
          add(command);
          add(command);
        }});
    replay(am);

    // CUSTOM_COMMAND and ACTIONEXECUTE reports are ignored
    // they should not change the host component state
    try {
      agentReportsController.handleCommandReportStatus(DummyHostname1, commandStatusReports);
      waitForReportsProcessing(agentReportsProcessor);
    } catch (Exception e) {
      fail();
    }
  }

  @Test
  public void testHandleComponentVersionReport() throws Exception {
    String fakeVersion = "fakeVersion";

    // we need 2.0.5 stack because from this stack HDFS becomes to use stack version advertising for components.
    Cluster cluster = heartbeatTestHelper.getDummyCluster(DummyCluster, Long.valueOf(DummyClusterId),
        new StackId("HDP-2.0.5"), null, Collections.emptyMap(),
        Collections.singleton(DummyHostname1));

    AgentSessionManager agentSessionManager = injector.getInstance(AgentSessionManager.class);
    for (Host host : cluster.getHosts()) {
      agentSessionManager.register(host.getHostName(), host);
    }

    Service hdfs = heartbeatTestHelper.addService(cluster, HDFS);
    hdfs.addServiceComponent(DATANODE);
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1);

    ServiceComponentHost serviceComponentHost1 = clusters.getCluster(DummyCluster).getService(HDFS).
        getServiceComponent(DATANODE).getServiceComponentHost(DummyHostname1);
    serviceComponentHost1.setState(State.INSTALLING);

    ComponentVersionReport componentVersionReport = new ComponentVersionReport();
    componentVersionReport.setClusterId(Long.valueOf(DummyClusterId));
    componentVersionReport.setComponentName(DATANODE);
    componentVersionReport.setServiceName(HDFS);
    componentVersionReport.setVersion(fakeVersion);

    TreeMap<String, List<ComponentVersionReport>> componentVersionReportMap = new TreeMap<>();
    componentVersionReportMap.put(DummyClusterId, Collections.singletonList(componentVersionReport));

    ComponentVersionReports componentVersionReports = new ComponentVersionReports();
    componentVersionReports.setComponentVersionReports(componentVersionReportMap);

    agentReportsController.handleComponentVersionReport(DummyHostname1, componentVersionReports);
    waitForReportsProcessing(agentReportsProcessor);

    String componentVersion = serviceComponentHost1.getVersion();
    assertEquals(String.format("Host state should still be '%s'", fakeVersion), fakeVersion, componentVersion);
  }

  @Test
  public void testHandleHostReportStatus() throws Exception {
    Cluster cluster = heartbeatTestHelper.getDummyCluster();

    Register register = heartbeatTestHelper.createRegister();
    heartbeatTestHelper.registerAgent(heartbeatController, register);

    // we need to send a heartbeat for first, because first WAITING_FOR_HOST_STATUS_UPDATES->HOST_HEARTBEAT_HEALTHY transition
    HeartBeat heartBeat = new HeartBeat();
    heartBeat.setResponseId(0);
    heartbeatController.heartbeat(DummyHostname1, heartBeat);

    Service hdfs = heartbeatTestHelper.addService(cluster, HDFS);
    hdfs.addServiceComponent(DATANODE);
    hdfs.getServiceComponent(DATANODE).addServiceComponentHost(DummyHostname1);

    ServiceComponentHost serviceComponentHost1 = clusters.getCluster(DummyCluster).getService(HDFS).
        getServiceComponent(DATANODE).getServiceComponentHost(DummyHostname1);
    serviceComponentHost1.setState(State.INSTALLING);

    HostStatusReport hostStatusReport = new HostStatusReport();
    AgentEnv agentEnv = new AgentEnv();
    agentEnv.setFirewallName("fakeFwName");
    DiskInfo diskInfo = new DiskInfo();
    diskInfo.setDevice("/dev/sdb1");
    diskInfo.setSize("100");
    hostStatusReport.setAgentEnv(agentEnv);
    hostStatusReport.setMounts(Collections.singletonList(diskInfo));

    agentReportsController.handleHostReportStatus(DummyHostname1, hostStatusReport);
    waitForReportsProcessing(agentReportsProcessor);

    AgentEnv appliedAgentEnv = clusters.getHost(DummyHostname1).getLastAgentEnv();
    List<DiskInfo> appliedDiskInfos = clusters.getHost(DummyHostname1).getDisksInfo();

    assertNotNull(appliedAgentEnv);
    assertEquals("fakeFwName", appliedAgentEnv.getFirewallName());

    assertNotNull(appliedDiskInfos);
    assertEquals(1, appliedDiskInfos.size());
    assertEquals(diskInfo, appliedDiskInfos.get(0));
  }

  private void waitForReportsProcessing(AgentReportsProcessor agentReportsProcessor) throws NoSuchFieldException, IllegalAccessException {
    Field executorsField = AgentReportsProcessor.class.getDeclaredField("executors");
    executorsField.setAccessible(true);
    List<ExecutorService> executors = (List) executorsField.get(agentReportsProcessor);

    for (ExecutorService executorService : executors) {
      executorService.shutdown();
      try {
        if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
          executorService.shutdownNow();
        }
      } catch (InterruptedException ex) {
        executorService.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
  }
}
