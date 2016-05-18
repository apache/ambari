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
package org.apache.ambari.server.actionmanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.ServiceComponentHostNotFoundException;
import org.apache.ambari.server.actionmanager.ActionScheduler.RoleStats;
import org.apache.ambari.server.agent.ActionQueue;
import org.apache.ambari.server.agent.AgentCommand;
import org.apache.ambari.server.agent.AgentCommand.AgentCommandType;
import org.apache.ambari.server.agent.CancelCommand;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.HostsMap;
import org.apache.ambari.server.events.AmbariEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.RequestEntity;
import org.apache.ambari.server.serveraction.MockServerAction;
import org.apache.ambari.server.serveraction.ServerActionExecutor;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceComponentHostEvent;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostInstallEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostOpFailedEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostServerActionEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostUpgradeEvent;
import org.apache.ambari.server.utils.StageUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.reflect.TypeToken;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.UnitOfWork;

import junit.framework.Assert;

public class TestActionScheduler {

  private static final Logger log = LoggerFactory.getLogger(TestActionScheduler.class);
  private static final String CLUSTER_HOST_INFO = "{all_hosts=[c6403.ambari.apache.org," +
  		" c6401.ambari.apache.org, c6402.ambari.apache.org], slave_hosts=[c6403.ambari.apache.org," +
  		" c6401.ambari.apache.org, c6402.ambari.apache.org]}";
  private static final String CLUSTER_HOST_INFO_UPDATED = "{all_hosts=[c6401.ambari.apache.org,"
      + " c6402.ambari.apache.org], slave_hosts=[c6401.ambari.apache.org,"
      + " c6402.ambari.apache.org]}";

  private static Injector injector;

  private final String hostname = "ahost.ambari.apache.org";
  private final int MAX_CYCLE_ITERATIONS = 100;

  @Inject
  HostRoleCommandFactory hostRoleCommandFactory;

  @Inject
  StageFactory stageFactory;

  @Inject
  StageUtils stageUtils;

  @Inject
  HostDAO hostDAO;

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    injector.injectMembers(this);
  }

  @After
  public void teardown() {
    injector.getInstance(PersistService.class).stop();
    injector = null;
  }

  /**
   * This test sends a new action to the action scheduler and verifies that the action
   * shows up in the action queue.
   */
  @Test
  public void testActionSchedule() throws Exception {
    Type type = new TypeToken<Map<String, Set<String>>>() {}.getType();
    Map<String, List<String>> clusterHostInfo = StageUtils.getGson().fromJson(CLUSTER_HOST_INFO, type);

    ActionQueue aq = new ActionQueue();
    Properties properties = new Properties();
    Configuration conf = new Configuration(properties);
    Clusters fsm = mock(Clusters.class);
    Cluster oneClusterMock = mock(Cluster.class);
    Service serviceObj = mock(Service.class);
    ServiceComponent scomp = mock(ServiceComponent.class);
    ServiceComponentHost sch = mock(ServiceComponentHost.class);
    UnitOfWork unitOfWork = mock(UnitOfWork.class);
    when(fsm.getCluster(anyString())).thenReturn(oneClusterMock);
    when(fsm.getClusterById(anyLong())).thenReturn(oneClusterMock);
    when(oneClusterMock.getService(anyString())).thenReturn(serviceObj);
    when(oneClusterMock.getClusterId()).thenReturn(Long.valueOf(1L));
    when(serviceObj.getServiceComponent(anyString())).thenReturn(scomp);
    when(scomp.getServiceComponentHost(anyString())).thenReturn(sch);
    when(serviceObj.getCluster()).thenReturn(oneClusterMock);

    Host host = mock(Host.class);
    HashMap<String, ServiceComponentHost> hosts =
            new HashMap<String, ServiceComponentHost>();
    hosts.put(hostname, sch);
    HostEntity hostEntity = new HostEntity();
    hostEntity.setHostName(hostname);
    hostDAO.merge(hostEntity);
    when(scomp.getServiceComponentHosts()).thenReturn(hosts);

    when(fsm.getHost(anyString())).thenReturn(host);
    when(host.getState()).thenReturn(HostState.HEALTHY);
    when(host.getHostName()).thenReturn(hostname);

    ActionDBAccessor db = mock(ActionDBAccessorImpl.class);
    List<Stage> stages = new ArrayList<Stage>();
    Stage s = StageUtils.getATestStage(1, 977, hostname, CLUSTER_HOST_INFO,
      "{\"host_param\":\"param_value\"}", "{\"stage_param\":\"param_value\"}");
    stages.add(s);

    when(db.getCommandsInProgressCount()).thenReturn(stages.size());
    when(db.getStagesInProgress()).thenReturn(stages);

    RequestEntity request = mock(RequestEntity.class);
    when(request.isExclusive()).thenReturn(false);
    when(db.getRequestEntity(anyLong())).thenReturn(request);

    //Keep large number of attempts so that the task is not expired finally
    //Small action timeout to test rescheduling
    ActionScheduler scheduler = new ActionScheduler(100, 5, db, aq, fsm,
        10000, new HostsMap((String) null), unitOfWork, null, conf);
    scheduler.setTaskTimeoutAdjustment(false);

    List<AgentCommand> ac = waitForQueueSize(hostname, aq, 1, scheduler);
    assertTrue(ac.get(0) instanceof ExecutionCommand);
    assertEquals("1-977", ((ExecutionCommand) (ac.get(0))).getCommandId());
    assertEquals(clusterHostInfo, ((ExecutionCommand) (ac.get(0))).getClusterHostInfo());

    //The action status has not changed, it should be queued again.
    ac = waitForQueueSize(hostname, aq, 1, scheduler);
    assertTrue(ac.get(0) instanceof ExecutionCommand);
    assertEquals("1-977", ((ExecutionCommand) (ac.get(0))).getCommandId());
    assertEquals(clusterHostInfo, ((ExecutionCommand) (ac.get(0))).getClusterHostInfo());

    //Now change the action status
    s.setHostRoleStatus(hostname, "NAMENODE", HostRoleStatus.COMPLETED);
    ac = aq.dequeueAll(hostname);

    //Wait for sometime, it shouldn't be scheduled this time.
    ac = waitForQueueSize(hostname, aq, 0, scheduler);
  }

  private List<AgentCommand> waitForQueueSize(String hostname, ActionQueue aq,
      int expectedQueueSize, ActionScheduler scheduler) {
    int cycleCount = 0;
    while (cycleCount++ <= MAX_CYCLE_ITERATIONS) {
      List<AgentCommand> ac = aq.dequeueAll(hostname);
      if (ac != null) {
        if (ac.size() == expectedQueueSize) {
          return ac;
        } else if (ac.size() > expectedQueueSize) {
          Assert.fail("Expected size : " + expectedQueueSize + " Actual size="
              + ac.size());
        }
      }
      try {
        scheduler.doWork();
      } catch (AmbariException e) {
        Assert.fail("Ambari exception : " + e.getMessage() + e.getStackTrace());
      }
    }
    return null;
  }

  /**
   * Test whether scheduler times out an action
   */
  @Test
  public void testActionTimeout() throws Exception {
    ActionQueue aq = new ActionQueue();
    Properties properties = new Properties();
    Configuration conf = new Configuration(properties);
    Clusters fsm = mock(Clusters.class);
    Cluster oneClusterMock = mock(Cluster.class);
    Service serviceObj = mock(Service.class);
    ServiceComponent scomp = mock(ServiceComponent.class);
    ServiceComponentHost sch = mock(ServiceComponentHost.class);
    UnitOfWork unitOfWork = mock(UnitOfWork.class);
    when(fsm.getCluster(anyString())).thenReturn(oneClusterMock);
    when(oneClusterMock.getService(anyString())).thenReturn(serviceObj);
    when(serviceObj.getServiceComponent(anyString())).thenReturn(scomp);
    when(scomp.getServiceComponentHost(anyString())).thenReturn(sch);
    when(serviceObj.getCluster()).thenReturn(oneClusterMock);
    Host host = mock(Host.class);
    HashMap<String, ServiceComponentHost> hosts =
            new HashMap<String, ServiceComponentHost>();
    hosts.put(hostname, sch);
    when(scomp.getServiceComponentHosts()).thenReturn(hosts);

    when(fsm.getHost(anyString())).thenReturn(host);
    when(host.getState()).thenReturn(HostState.HEALTHY);
    when(host.getHostName()).thenReturn(hostname);

    HostEntity hostEntity = new HostEntity();
    hostEntity.setHostName(hostname);
    hostDAO.create(hostEntity);

    List<Stage> stages = new ArrayList<Stage>();
    final Stage s = StageUtils.getATestStage(1, 977, hostname, CLUSTER_HOST_INFO,
      "{\"host_param\":\"param_value\"}", "{\"stage_param\":\"param_value\"}");
    s.addHostRoleExecutionCommand(hostname, Role.SECONDARY_NAMENODE, RoleCommand.INSTALL,
            new ServiceComponentHostInstallEvent("SECONDARY_NAMENODE", hostname, System.currentTimeMillis(), "HDP-1.2.0"),
            "cluster1", "HDFS", false, false);
    s.setHostRoleStatus(hostname, "SECONDARY_NAMENODE", HostRoleStatus.IN_PROGRESS);
    stages.add(s);

    ActionDBAccessor db = mock(ActionDBAccessor.class);
    when(db.getCommandsInProgressCount()).thenReturn(stages.size());
    when(db.getStagesInProgress()).thenReturn(stages);

    RequestEntity request = mock(RequestEntity.class);
    when(request.isExclusive()).thenReturn(false);
    when(db.getRequestEntity(anyLong())).thenReturn(request);

    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        String host = (String) invocation.getArguments()[0];
        String role = (String) invocation.getArguments()[3];
        HostRoleCommand command = s.getHostRoleCommand(host, role);
        command.setStatus(HostRoleStatus.TIMEDOUT);
        return null;
      }
    }).when(db).timeoutHostRole(anyString(), anyLong(), anyLong(), anyString(), anyBoolean());


    //Small action timeout to test rescheduling
    ActionScheduler scheduler = new ActionScheduler(100, 0, db, aq, fsm, 3,
        new HostsMap((String) null), unitOfWork, null, conf);
    scheduler.setTaskTimeoutAdjustment(false);
    // Start the thread

    int cycleCount = 0;
    scheduler.doWork();
    //Check that in_progress command is rescheduled
    assertEquals(HostRoleStatus.QUEUED, stages.get(0).getHostRoleStatus(hostname, "SECONDARY_NAMENODE"));

    //Switch command back to IN_PROGRESS status and check that other command is not rescheduled
    stages.get(0).setHostRoleStatus(hostname, "SECONDARY_NAMENODE", HostRoleStatus.IN_PROGRESS);
    scheduler.doWork();
    assertEquals(1, stages.get(0).getAttemptCount(hostname, "NAMENODE"));
    assertEquals(2, stages.get(0).getAttemptCount(hostname, "SECONDARY_NAMENODE"));

    while (!stages.get(0).getHostRoleStatus(hostname, "SECONDARY_NAMENODE")
        .equals(HostRoleStatus.TIMEDOUT) && cycleCount++ <= MAX_CYCLE_ITERATIONS) {
      scheduler.doWork();
    }
    assertEquals(HostRoleStatus.TIMEDOUT,
            stages.get(0).getHostRoleStatus(hostname, "SECONDARY_NAMENODE"));

    verify(db, times(1)).startRequest(eq(1L));
    verify(db, times(1)).abortOperation(1L);

  }

  @Test
  public void testActionTimeoutForLostHost() throws Exception {
    ActionQueue aq = new ActionQueue();
    Properties properties = new Properties();
    Configuration conf = new Configuration(properties);
    Clusters fsm = mock(Clusters.class);
    Cluster oneClusterMock = mock(Cluster.class);
    Service serviceObj = mock(Service.class);
    ServiceComponent scomp = mock(ServiceComponent.class);
    ServiceComponentHost sch = mock(ServiceComponentHost.class);
    UnitOfWork unitOfWork = mock(UnitOfWork.class);
    when(fsm.getCluster(anyString())).thenReturn(oneClusterMock);
    when(oneClusterMock.getService(anyString())).thenReturn(serviceObj);
    when(serviceObj.getServiceComponent(anyString())).thenReturn(scomp);
    when(scomp.getServiceComponentHost(anyString())).thenReturn(sch);
    when(serviceObj.getCluster()).thenReturn(oneClusterMock);
    Host host = mock(Host.class);
    HashMap<String, ServiceComponentHost> hosts =
            new HashMap<String, ServiceComponentHost>();
    hosts.put(hostname, sch);
    when(scomp.getServiceComponentHosts()).thenReturn(hosts);

    when(fsm.getHost(anyString())).thenReturn(host);
    when(host.getState()).thenReturn(HostState.HEARTBEAT_LOST);
    when(host.getHostName()).thenReturn(hostname);

    List<Stage> stages = new ArrayList<Stage>();
    final Stage s = StageUtils.getATestStage(1, 977, hostname, CLUSTER_HOST_INFO,
      "{\"host_param\":\"param_value\"}", "{\"stage_param\":\"param_value\"}");
    stages.add(s);

    ActionDBAccessor db = mock(ActionDBAccessor.class);

    RequestEntity request = mock(RequestEntity.class);
    when(request.isExclusive()).thenReturn(false);
    when(db.getRequestEntity(anyLong())).thenReturn(request);

    when(db.getCommandsInProgressCount()).thenReturn(stages.size());
    when(db.getStagesInProgress()).thenReturn(stages);
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        String host = (String) invocation.getArguments()[0];
        String role = (String) invocation.getArguments()[3];
        HostRoleCommand command = s.getHostRoleCommand(host, role);
        command.setStatus(HostRoleStatus.TIMEDOUT);
        return null;
      }
    }).when(db).timeoutHostRole(anyString(), anyLong(), anyLong(), anyString(), anyBoolean());

    //Small action timeout to test rescheduling
    AmbariEventPublisher aep = EasyMock.createNiceMock(AmbariEventPublisher.class);
    ActionScheduler scheduler = EasyMock.createMockBuilder(ActionScheduler.class).
        withConstructor((long) 100, (long) 50, db, aq, fsm, 3,
            new HostsMap((String) null), unitOfWork, aep, conf).
        addMockedMethod("cancelHostRoleCommands").
        createMock();
    scheduler.cancelHostRoleCommands((Collection<HostRoleCommand>)EasyMock.anyObject(),EasyMock.anyObject(String.class));
    EasyMock.expectLastCall();
    EasyMock.replay(scheduler);
    scheduler.setTaskTimeoutAdjustment(false);

    int cycleCount=0;
    while (!stages.get(0).getHostRoleStatus(hostname, "NAMENODE")
      .equals(HostRoleStatus.TIMEDOUT) && cycleCount++ <= MAX_CYCLE_ITERATIONS) {
      scheduler.doWork();
    }

    Assert.assertEquals(HostRoleStatus.TIMEDOUT,stages.get(0).getHostRoleStatus(hostname, "NAMENODE"));

    EasyMock.verify(scheduler);
  }

  @Test
  public void testOpFailedEventRaisedForAbortedHostRole() throws Exception {
    ActionQueue aq = new ActionQueue();
    Properties properties = new Properties();
    Configuration conf = new Configuration(properties);
    Clusters fsm = mock(Clusters.class);
    Cluster oneClusterMock = mock(Cluster.class);
    Service serviceObj = mock(Service.class);
    ServiceComponent scomp = mock(ServiceComponent.class);
    ServiceComponentHost sch1 = mock(ServiceComponentHost.class);
    ServiceComponentHost sch2 = mock(ServiceComponentHost.class);
    String hostname1 = "host1";
    String hostname2 = "host2";
    Host host1 = mock(Host.class);
    Host host2 = mock(Host.class);
    HostEntity hostEntity1 = new HostEntity();
    hostEntity1.setHostName(hostname1);
    HostEntity hostEntity2 = new HostEntity();
    hostEntity2.setHostName(hostname2);
    hostDAO.merge(hostEntity1);
    hostDAO.merge(hostEntity2);

    HashMap<String, ServiceComponentHost> hosts =
            new HashMap<String, ServiceComponentHost>();
    hosts.put(hostname1, sch1);
    hosts.put(hostname2, sch2);
    when(scomp.getServiceComponentHosts()).thenReturn(hosts);

    when(fsm.getHost(hostname1)).thenReturn(host1);
    when(fsm.getHost(hostname2)).thenReturn(host2);
    when(host1.getState()).thenReturn(HostState.HEARTBEAT_LOST);
    when(host2.getState()).thenReturn(HostState.HEALTHY);
    when(host1.getHostName()).thenReturn(hostname1);
    when(host2.getHostName()).thenReturn(hostname2);
    when(scomp.getServiceComponentHost(hostname1)).thenReturn(sch1);
    when(scomp.getServiceComponentHost(hostname2)).thenReturn(sch2);

    UnitOfWork unitOfWork = mock(UnitOfWork.class);
    when(fsm.getCluster(anyString())).thenReturn(oneClusterMock);
    when(oneClusterMock.getService(anyString())).thenReturn(serviceObj);
    when(serviceObj.getServiceComponent(anyString())).thenReturn(scomp);
    when(serviceObj.getCluster()).thenReturn(oneClusterMock);

    final List<Stage> stages = new ArrayList<Stage>();
    Stage stage = stageFactory.createNew(1, "/tmp", "cluster1", 1L, "stageWith2Tasks",
      CLUSTER_HOST_INFO, "{\"command_param\":\"param_value\"}", "{\"host_param\":\"param_value\"}");
    addInstallTaskToStage(stage, hostname1, "cluster1", Role.DATANODE,
      RoleCommand.INSTALL, Service.Type.HDFS, 1);
    addInstallTaskToStage(stage, hostname2, "cluster1", Role.NAMENODE,
      RoleCommand.INSTALL, Service.Type.HDFS, 2);
    stages.add(stage);

    ActionDBAccessor db = mock(ActionDBAccessor.class);

    RequestEntity request = mock(RequestEntity.class);
    when(request.isExclusive()).thenReturn(false);
    when(db.getRequestEntity(anyLong())).thenReturn(request);

    when(db.getCommandsInProgressCount()).thenReturn(stages.size());
    when(db.getStagesInProgress()).thenReturn(stages);

    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        String host = (String) invocation.getArguments()[0];
        String role = (String) invocation.getArguments()[3];
        //HostRoleCommand command = stages.get(0).getHostRoleCommand(host, role);
        for (HostRoleCommand command : stages.get(0).getOrderedHostRoleCommands()) {
          if (command.getHostName().equals(host) && command.getRole().name()
              .equals(role)) {
            command.setStatus(HostRoleStatus.TIMEDOUT);
          }
        }
        return null;
      }
    }).when(db).timeoutHostRole(anyString(), anyLong(), anyLong(), anyString(), anyBoolean());

    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        Long requestId = (Long) invocation.getArguments()[0];
        for (Stage stage : stages) {
          if (requestId.equals(stage.getRequestId())) {
            for (HostRoleCommand command : stage.getOrderedHostRoleCommands()) {
              if (command.getStatus() == HostRoleStatus.QUEUED ||
                command.getStatus() == HostRoleStatus.IN_PROGRESS ||
                command.getStatus() == HostRoleStatus.PENDING) {
                command.setStatus(HostRoleStatus.ABORTED);
              }
            }
          }
        }

        return null;
      }
    }).when(db).abortOperation(anyLong());

    ArgumentCaptor<ServiceComponentHostEvent> eventsCapture1 =
      ArgumentCaptor.forClass(ServiceComponentHostEvent.class);
    ArgumentCaptor<ServiceComponentHostEvent> eventsCapture2 =
      ArgumentCaptor.forClass(ServiceComponentHostEvent.class);

    // Make sure the NN install doesn't timeout
    ActionScheduler scheduler = new ActionScheduler(100, 50000, db, aq, fsm, 3,
        new HostsMap((String) null), unitOfWork, null, conf);
    scheduler.setTaskTimeoutAdjustment(false);

    int cycleCount=0;
    while (!(stages.get(0).getHostRoleStatus(hostname1, "DATANODE")
      .equals(HostRoleStatus.TIMEDOUT) && stages.get(0).getHostRoleStatus
      (hostname2, "NAMENODE").equals(HostRoleStatus.ABORTED)) && cycleCount++ <= MAX_CYCLE_ITERATIONS) {
      scheduler.doWork();
    }

    Assert.assertEquals(HostRoleStatus.TIMEDOUT,
      stages.get(0).getHostRoleStatus(hostname1, "DATANODE"));
    Assert.assertEquals(HostRoleStatus.ABORTED,
      stages.get(0).getHostRoleStatus(hostname2, "NAMENODE"));

    verify(sch1, atLeastOnce()).handleEvent(eventsCapture1.capture());
    verify(sch2, atLeastOnce()).handleEvent(eventsCapture2.capture());

    List<ServiceComponentHostEvent> eventTypes = eventsCapture1.getAllValues();
    eventTypes.addAll(eventsCapture2.getAllValues());

    Assert.assertNotNull(eventTypes);

    ServiceComponentHostOpFailedEvent datanodeFailedEvent = null;
    ServiceComponentHostOpFailedEvent namenodeFailedEvent = null;

    for (ServiceComponentHostEvent eventType : eventTypes) {
      if (eventType instanceof ServiceComponentHostOpFailedEvent) {
        ServiceComponentHostOpFailedEvent event =
          (ServiceComponentHostOpFailedEvent) eventType;

        if (event.getServiceComponentName().equals("DATANODE")) {
          datanodeFailedEvent = event;
        } else if (event.getServiceComponentName().equals("NAMENODE")) {
          namenodeFailedEvent = event;
        }
      }
    }

    Assert.assertNotNull("Datanode should be in Install failed state.",
      datanodeFailedEvent);
    Assert.assertNotNull("Namenode should be in Install failed state.",
      namenodeFailedEvent);

  }

  /**
   * Test server action
   */
  @Test
  public void testServerAction() throws Exception {
    ActionQueue aq = new ActionQueue();
    Properties properties = new Properties();
    Configuration conf = new Configuration(properties);
    Clusters fsm = mock(Clusters.class);
    UnitOfWork unitOfWork = mock(UnitOfWork.class);

    List<Stage> stages = new ArrayList<Stage>();
    Map<String, String> payload = new HashMap<String, String>();
    final Stage s = getStageWithServerAction(1, 977, payload, "test", 1200);
    stages.add(s);

    ActionDBAccessor db = mock(ActionDBAccessor.class);

    RequestEntity request = mock(RequestEntity.class);
    when(request.isExclusive()).thenReturn(false);
    when(db.getRequestEntity(anyLong())).thenReturn(request);

    when(db.getCommandsInProgressCount()).thenReturn(stages.size());
    when(db.getStagesInProgress()).thenReturn(stages);
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        String host = (String) invocation.getArguments()[0];
        String role = (String) invocation.getArguments()[3];
        CommandReport commandReport = (CommandReport) invocation.getArguments()[4];

        HostRoleCommand command = null;
        if (null == host) {
          command = s.getHostRoleCommand(null, role);
        } else {
          command = s.getHostRoleCommand(host, role);
        }

        command.setStatus(HostRoleStatus.valueOf(commandReport.getStatus()));
        return null;
      }
    }).when(db).updateHostRoleState(anyString(), anyLong(), anyLong(), anyString(), any(CommandReport.class));

    doAnswer(new Answer<HostRoleCommand>() {
      @Override
      public HostRoleCommand answer(InvocationOnMock invocation) throws Throwable {
        return s.getHostRoleCommand(null, "AMBARI_SERVER_ACTION");
      }
    }).when(db).getTask(anyLong());

    doAnswer(new Answer<List<HostRoleCommand>>() {
      @Override
      public List<HostRoleCommand> answer(InvocationOnMock invocation) throws Throwable {
        String role = (String) invocation.getArguments()[0];
        HostRoleStatus status = (HostRoleStatus) invocation.getArguments()[1];

        HostRoleCommand task = s.getHostRoleCommand(null, role);

        if (task.getStatus() == status) {
          return Arrays.asList(task);
        } else {
          return Collections.emptyList();
        }
      }
    }).when(db).getTasksByRoleAndStatus(anyString(), any(HostRoleStatus.class));

    ServerActionExecutor.init(injector);
    ActionScheduler scheduler = new ActionScheduler(100, 50, db, aq, fsm, 3,
        new HostsMap((String) null), unitOfWork, null, conf);

    int cycleCount = 0;
    while (!stages.get(0).getHostRoleStatus(null, "AMBARI_SERVER_ACTION")
        .equals(HostRoleStatus.COMPLETED) && cycleCount++ <= MAX_CYCLE_ITERATIONS) {
      scheduler.doWork();
      scheduler.getServerActionExecutor().doWork();
    }

    assertEquals(stages.get(0).getHostRoleStatus(null, "AMBARI_SERVER_ACTION"),
        HostRoleStatus.COMPLETED);
  }

  /**
   * Test server actions in multiple requests.
   *
   * This is used to make sure the server-side actions do not get filtered out from
   * {@link org.apache.ambari.server.actionmanager.ActionScheduler#filterParallelPerHostStages(java.util.List)}
   */
  @Test
  public void testServerActionInMultipleRequests() throws Exception {
    ActionQueue aq = new ActionQueue();
    Clusters fsm = mock(Clusters.class);
    Cluster oneClusterMock = mock(Cluster.class);
    Service serviceObj = mock(Service.class);
    ServiceComponent scomp = mock(ServiceComponent.class);
    ServiceComponentHost sch = mock(ServiceComponentHost.class);
    UnitOfWork unitOfWork = mock(UnitOfWork.class);
    when(fsm.getCluster(anyString())).thenReturn(oneClusterMock);
    when(oneClusterMock.getService(anyString())).thenReturn(serviceObj);
    when(serviceObj.getServiceComponent(anyString())).thenReturn(scomp);
    when(scomp.getServiceComponentHost(anyString())).thenReturn(sch);
    when(serviceObj.getCluster()).thenReturn(oneClusterMock);

    String clusterName = "cluster1";
    String hostname1 = "ahost.ambari.apache.org";
    String hostname2 = "bhost.ambari.apache.org";
    HashMap<String, ServiceComponentHost> hosts =
        new HashMap<String, ServiceComponentHost>();
    hosts.put(hostname1, sch);
    hosts.put(hostname2, sch);
    hosts.put(Stage.INTERNAL_HOSTNAME, sch);
    when(scomp.getServiceComponentHosts()).thenReturn(hosts);

    List<Stage> stages = new ArrayList<Stage>();
    Stage stage01 = createStage(clusterName, 0, 1);
    addTask(stage01, Stage.INTERNAL_HOSTNAME, clusterName, Role.AMBARI_SERVER_ACTION, RoleCommand.ACTIONEXECUTE, "AMBARI", 1);

    Stage stage11 = createStage("cluster1", 1, 1);
    addTask(stage11, hostname1, clusterName, Role.KERBEROS_CLIENT, RoleCommand.CUSTOM_COMMAND, "KERBEROS", 2);

    Stage stage02 = createStage("cluster1", 0, 2);
    addTask(stage02, Stage.INTERNAL_HOSTNAME, clusterName, Role.AMBARI_SERVER_ACTION, RoleCommand.ACTIONEXECUTE, "AMBARI", 3);

    Stage stage12 = createStage("cluster1", 1, 2);
    addTask(stage12, hostname2, clusterName, Role.KERBEROS_CLIENT, RoleCommand.CUSTOM_COMMAND, "KERBEROS", 4);

    stages.add(stage01);
    stages.add(stage11);
    stages.add(stage02);
    stages.add(stage12);

    ActionDBAccessor db = mock(ActionDBAccessor.class);

    RequestEntity request = mock(RequestEntity.class);
    when(request.isExclusive()).thenReturn(false);
    when(db.getRequestEntity(anyLong())).thenReturn(request);

    when(db.getCommandsInProgressCount()).thenReturn(stages.size());
    when(db.getStagesInProgress()).thenReturn(stages);

    Properties properties = new Properties();
    properties.put(Configuration.PARALLEL_STAGE_EXECUTION_KEY, "true");
    Configuration conf = new Configuration(properties);
    ActionScheduler scheduler = new ActionScheduler(100, 50, db, aq, fsm, 3,
        new HostsMap((String) null),
        unitOfWork, EasyMock.createNiceMock(AmbariEventPublisher.class), conf);

    scheduler.doWork();

    Assert.assertEquals(HostRoleStatus.QUEUED, stages.get(0).getHostRoleStatus(Stage.INTERNAL_HOSTNAME, Role.AMBARI_SERVER_ACTION.name()));
    Assert.assertEquals(HostRoleStatus.PENDING, stages.get(1).getHostRoleStatus(hostname1, Role.KERBEROS_CLIENT.name()));
    Assert.assertEquals(HostRoleStatus.QUEUED, stages.get(2).getHostRoleStatus(Stage.INTERNAL_HOSTNAME, Role.AMBARI_SERVER_ACTION.name()));
    Assert.assertEquals(HostRoleStatus.PENDING, stages.get(3).getHostRoleStatus(hostname2, Role.KERBEROS_CLIENT.name()));
  }

  /**
   * Test server action
   */
  @Test
  public void testServerActionTimeOut() throws Exception {
    ActionQueue aq = new ActionQueue();
    Properties properties = new Properties();
    Configuration conf = new Configuration(properties);
    Clusters fsm = mock(Clusters.class);
    UnitOfWork unitOfWork = mock(UnitOfWork.class);

    List<Stage> stages = new ArrayList<Stage>();
    Map<String, String> payload = new HashMap<String, String>();
    payload.put(MockServerAction.PAYLOAD_FORCE_FAIL, "timeout");
    final Stage s = getStageWithServerAction(1, 977, payload, "test", 2);
    stages.add(s);

    ActionDBAccessor db = mock(ActionDBAccessor.class);

    RequestEntity request = mock(RequestEntity.class);
    when(request.isExclusive()).thenReturn(false);
    when(db.getRequestEntity(anyLong())).thenReturn(request);

    when(db.getCommandsInProgressCount()).thenReturn(stages.size());
    when(db.getStagesInProgress()).thenReturn(stages);
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        String host = (String) invocation.getArguments()[0];
        String role = (String) invocation.getArguments()[3];
        CommandReport commandReport = (CommandReport) invocation.getArguments()[4];

        HostRoleCommand command = null;
        if (null == host) {
          command = s.getHostRoleCommand(null, role);
        } else {
          command = s.getHostRoleCommand(host, role);
        }

        command.setStatus(HostRoleStatus.valueOf(commandReport.getStatus()));
        return null;
      }
    }).when(db).updateHostRoleState(anyString(), anyLong(), anyLong(), anyString(), any(CommandReport.class));

    doAnswer(new Answer<HostRoleCommand>() {
      @Override
      public HostRoleCommand answer(InvocationOnMock invocation) throws Throwable {
        return s.getHostRoleCommand(null, "AMBARI_SERVER_ACTION");
      }
    }).when(db).getTask(anyLong());

    doAnswer(new Answer<List<HostRoleCommand>>() {
      @Override
      public List<HostRoleCommand> answer(InvocationOnMock invocation) throws Throwable {
        String role = (String) invocation.getArguments()[0];
        HostRoleStatus status = (HostRoleStatus) invocation.getArguments()[1];

        HostRoleCommand task = s.getHostRoleCommand(null, role);

        if (task.getStatus() == status) {
          return Arrays.asList(task);
        } else {
          return Collections.emptyList();
        }

      }
    }).when(db).getTasksByRoleAndStatus(anyString(), any(HostRoleStatus.class));

    ServerActionExecutor.init(injector);
    ActionScheduler scheduler = new ActionScheduler(100, 50, db, aq, fsm, 3,
        new HostsMap((String) null), unitOfWork, null, conf);

    int cycleCount = 0;
    while (!stages.get(0).getHostRoleStatus(null, "AMBARI_SERVER_ACTION").isCompletedState()
        && cycleCount++ <= MAX_CYCLE_ITERATIONS) {
      scheduler.doWork();
      scheduler.getServerActionExecutor().doWork();
    }

    assertEquals(HostRoleStatus.TIMEDOUT,
        stages.get(0).getHostRoleStatus(null, "AMBARI_SERVER_ACTION"));
  }

  @Test
  public void testTimeOutWithHostNull() throws AmbariException {
    Stage s = getStageWithServerAction(1, 977, null, "test", 2);
    s.setHostRoleStatus(null, Role.AMBARI_SERVER_ACTION.toString(), HostRoleStatus.IN_PROGRESS);

    ActionScheduler scheduler = EasyMock.createMockBuilder(ActionScheduler.class)
      .withConstructor(long.class, long.class, ActionDBAccessor.class, ActionQueue.class, Clusters.class, int.class,
        HostsMap.class, UnitOfWork.class, AmbariEventPublisher.class, Configuration.class)
      .withArgs(100L, 50L, null, null, null, -1, null, null, null, null)
      .createNiceMock();

    EasyMock.replay(scheduler);

    // currentTime should be set to -1 and taskTimeout to 1 because it is needed for timeOutActionNeeded method will return false value
    Assert.assertEquals(false, scheduler.timeOutActionNeeded(HostRoleStatus.IN_PROGRESS, s, null, Role.AMBARI_SERVER_ACTION.toString(), -1L, 1L));

    EasyMock.verify(scheduler);
  }

  @Test
  public void testTimeoutRequestDueAgentRestartExecuteCommand() throws Exception {
    testTimeoutRequest(RoleCommand.EXECUTE);
  }

  @Test
  public void testTimeoutRequestDueAgentRestartCustomCommand() throws Exception {
    testTimeoutRequest(RoleCommand.CUSTOM_COMMAND);
  }

  @Test
  public void testTimeoutRequestDueAgentRestartActionExecute() throws Exception {
    testTimeoutRequest(RoleCommand.ACTIONEXECUTE);
  }

  @Test
  public void testTimeoutRequestDueAgentRestartServiceCheck() throws Exception {
    testTimeoutRequest(RoleCommand.SERVICE_CHECK);
  }

  private void testTimeoutRequest(RoleCommand roleCommand) throws AmbariException, InvalidStateTransitionException {
    final long HOST_REGISTRATION_TIME = 100L;
    final long STAGE_TASK_START_TIME = HOST_REGISTRATION_TIME - 1L;

    ActionQueue aq = new ActionQueue();
    Clusters fsm = EasyMock.createMock(Clusters.class);
    Cluster cluster = EasyMock.createMock(Cluster.class);
    Service service = EasyMock.createMock(Service.class);
    ServiceComponent serviceComponent = EasyMock.createMock(ServiceComponent.class);
    ServiceComponentHost serviceComponentHost = EasyMock.createMock(ServiceComponentHost.class);
    Host host = EasyMock.createMock(Host.class);
    ActionDBAccessor db = EasyMock.createMock(ActionDBAccessor.class);
    AmbariEventPublisher ambariEventPublisher = EasyMock.createMock(AmbariEventPublisher.class);

    EasyMock.expect(fsm.getCluster(EasyMock.anyString())).andReturn(cluster).anyTimes();
    EasyMock.expect(fsm.getHost(EasyMock.anyString())).andReturn(host);
    EasyMock.expect(cluster.getService(EasyMock.anyString())).andReturn(null);
    EasyMock.expect(host.getLastRegistrationTime()).andReturn(HOST_REGISTRATION_TIME);
    EasyMock.expect(host.getHostName()).andReturn(Stage.INTERNAL_HOSTNAME).anyTimes();
    EasyMock.expect(host.getState()).andReturn(HostState.HEALTHY);

    if (RoleCommand.ACTIONEXECUTE.equals(roleCommand)) {
      EasyMock.expect(cluster.getClusterName()).andReturn("clusterName").anyTimes();
      EasyMock.expect(cluster.getClusterId()).andReturn(1L);

      ambariEventPublisher.publish(EasyMock.anyObject(AmbariEvent.class));
      EasyMock.expectLastCall();
    } else if (RoleCommand.EXECUTE.equals(roleCommand)) {
      EasyMock.expect(cluster.getClusterName()).andReturn("clusterName");
      EasyMock.expect(cluster.getService(EasyMock.anyString())).andReturn(service);
      EasyMock.expect(service.getServiceComponent(EasyMock.anyString())).andReturn(serviceComponent);
      EasyMock.expect(serviceComponent.getServiceComponentHost(EasyMock.anyString())).andReturn(serviceComponentHost);

      serviceComponentHost.handleEvent(EasyMock.anyObject(ServiceComponentHostEvent.class));
      EasyMock.expectLastCall();
    }

    Stage s = getStageWithServerAction(1, 977, null, "test", 2);
    s.setStartTime(null, Role.AMBARI_SERVER_ACTION.toString(), STAGE_TASK_START_TIME);
    s.setHostRoleStatus(null, Role.AMBARI_SERVER_ACTION.toString(), HostRoleStatus.IN_PROGRESS);
    s.getExecutionCommands(null).get(0).getExecutionCommand().setServiceName("Service name");
    s.getExecutionCommands(null).get(0).getExecutionCommand().setRoleCommand(roleCommand);

    aq.enqueue(Stage.INTERNAL_HOSTNAME, s.getExecutionCommands(null).get(0).getExecutionCommand());
    List<ExecutionCommand> commandsToSchedule = new ArrayList<ExecutionCommand>();

    db.timeoutHostRole(EasyMock.anyString(), EasyMock.anyLong(), EasyMock.anyLong(), EasyMock.anyString(), EasyMock.anyBoolean());
    EasyMock.expectLastCall();

    ActionScheduler scheduler = EasyMock.createMockBuilder(ActionScheduler.class)
      .withConstructor(long.class, long.class, ActionDBAccessor.class, ActionQueue.class, Clusters.class, int.class,
        HostsMap.class, UnitOfWork.class, AmbariEventPublisher.class, Configuration.class)
      .withArgs(100L, 50L, db, aq, fsm, -1, null, null, ambariEventPublisher, null)
      .createNiceMock();

    EasyMock.replay(scheduler, fsm, host, db, cluster, ambariEventPublisher, service, serviceComponent, serviceComponentHost);

    scheduler.processInProgressStage(s, commandsToSchedule);

    EasyMock.verify(scheduler, fsm, host, db, cluster, ambariEventPublisher, service, serviceComponent, serviceComponentHost);

    Assert.assertTrue("ActionQueue should be empty after request was timeout", aq.size(Stage.INTERNAL_HOSTNAME) == 0);
  }

  @Test
  public void testServerActionFailed() throws Exception {
    ActionQueue aq = new ActionQueue();
    Properties properties = new Properties();
    Configuration conf = new Configuration(properties);
    Clusters fsm = mock(Clusters.class);
    UnitOfWork unitOfWork = mock(UnitOfWork.class);

    List<Stage> stages = new ArrayList<Stage>();
    Map<String, String> payload = new HashMap<String, String>();
    payload.put(MockServerAction.PAYLOAD_FORCE_FAIL, "exception");
    final Stage s = getStageWithServerAction(1, 977, payload, "test", 300);
    stages.add(s);

    ActionDBAccessor db = mock(ActionDBAccessor.class);

    RequestEntity request = mock(RequestEntity.class);
    when(request.isExclusive()).thenReturn(false);
    when(db.getRequestEntity(anyLong())).thenReturn(request);

    when(db.getCommandsInProgressCount()).thenReturn(stages.size());
    when(db.getStagesInProgress()).thenReturn(stages);

    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        String host = (String) invocation.getArguments()[0];
        String role = (String) invocation.getArguments()[3];
        CommandReport commandReport = (CommandReport) invocation.getArguments()[4];

        HostRoleCommand command = null;
        if (null == host) {
          command = s.getHostRoleCommand(null, role);
        } else {
          command = s.getHostRoleCommand(host, role);
        }

        command.setStatus(HostRoleStatus.valueOf(commandReport.getStatus()));
        return null;
      }
    }).when(db).updateHostRoleState(anyString(), anyLong(), anyLong(), anyString(), any(CommandReport.class));

    doAnswer(new Answer<HostRoleCommand>() {
      @Override
      public HostRoleCommand answer(InvocationOnMock invocation) throws Throwable {
        return s.getHostRoleCommand(null, "AMBARI_SERVER_ACTION");
      }
    }).when(db).getTask(anyLong());

    doAnswer(new Answer<List<HostRoleCommand>>() {
      @Override
      public List<HostRoleCommand> answer(InvocationOnMock invocation) throws Throwable {
        String role = (String) invocation.getArguments()[0];
        HostRoleStatus status = (HostRoleStatus) invocation.getArguments()[1];

        HostRoleCommand task = s.getHostRoleCommand(null, role);

        if (task.getStatus() == status) {
          return Arrays.asList(task);
        } else {
          return Collections.emptyList();
        }
      }
    }).when(db).getTasksByRoleAndStatus(anyString(), any(HostRoleStatus.class));

    ActionScheduler scheduler = new ActionScheduler(100, 50, db, aq, fsm, 3,
        new HostsMap((String) null), unitOfWork, null, conf);

    int cycleCount = 0;
    while (!stages.get(0).getHostRoleStatus(null, "AMBARI_SERVER_ACTION")
        .equals(HostRoleStatus.FAILED) && cycleCount++ <= MAX_CYCLE_ITERATIONS) {
      scheduler.doWork();
      scheduler.getServerActionExecutor().doWork();
    }
    assertEquals(stages.get(0).getHostRoleStatus(null, "AMBARI_SERVER_ACTION"),
        HostRoleStatus.FAILED);
    assertEquals("test", stages.get(0).getRequestContext());
  }

  private Stage getStageWithServerAction(long requestId, long stageId,
                                                Map<String, String> payload, String requestContext,
                                                int timeout) {

    Stage stage = stageFactory.createNew(requestId, "/tmp", "cluster1", 1L, requestContext, CLUSTER_HOST_INFO,
      "{}", "{}");
    stage.setStageId(stageId);

    stage.addServerActionCommand(MockServerAction.class.getName(), null,
        Role.AMBARI_SERVER_ACTION,
        RoleCommand.EXECUTE, "cluster1",
        new ServiceComponentHostServerActionEvent(null, System.currentTimeMillis()),
        payload,
        null, null, timeout, false, false);

    return stage;
  }


  /**
   * Verifies that stages that are executed on different hosts and
   * rely to different requests are scheduled to be  executed in parallel
   */
  @Test
  public void testIndependentStagesExecution() throws Exception {
    ActionQueue aq = new ActionQueue();
    Clusters fsm = mock(Clusters.class);
    Cluster oneClusterMock = mock(Cluster.class);
    Service serviceObj = mock(Service.class);
    ServiceComponent scomp = mock(ServiceComponent.class);
    ServiceComponentHost sch = mock(ServiceComponentHost.class);
    UnitOfWork unitOfWork = mock(UnitOfWork.class);
    when(fsm.getCluster(anyString())).thenReturn(oneClusterMock);
    when(oneClusterMock.getService(anyString())).thenReturn(serviceObj);
    when(serviceObj.getServiceComponent(anyString())).thenReturn(scomp);
    when(scomp.getServiceComponentHost(anyString())).thenReturn(sch);
    when(serviceObj.getCluster()).thenReturn(oneClusterMock);

    String hostname1 = "ahost.ambari.apache.org";
    String hostname2 = "bhost.ambari.apache.org";
    String hostname3 = "chost.ambari.apache.org";
    String hostname4 = "chost.ambari.apache.org";
    HashMap<String, ServiceComponentHost> hosts =
            new HashMap<String, ServiceComponentHost>();
    hosts.put(hostname1, sch);
    hosts.put(hostname2, sch);
    hosts.put(hostname3, sch);
    hosts.put(hostname4, sch);
    when(scomp.getServiceComponentHosts()).thenReturn(hosts);

    List<Stage> stages = new ArrayList<Stage>();
    stages.add(
            getStageWithSingleTask(
                    hostname1, "cluster1", Role.DATANODE,
                    RoleCommand.START, Service.Type.HDFS, 1, 1, 1));
    stages.add( // Stage with the same hostname, should not be scheduled
            getStageWithSingleTask(
                    hostname1, "cluster1", Role.GANGLIA_MONITOR,
                    RoleCommand.START, Service.Type.GANGLIA, 2, 2, 2));

    stages.add(
            getStageWithSingleTask(
                    hostname2, "cluster1", Role.DATANODE,
                    RoleCommand.START, Service.Type.HDFS, 3, 3, 3));

    stages.add(
        getStageWithSingleTask(
            hostname3, "cluster1", Role.DATANODE,
            RoleCommand.START, Service.Type.HDFS, 4, 4, 4));

    stages.add( // Stage with the same request id, should not be scheduled
        getStageWithSingleTask(
            hostname4, "cluster1", Role.GANGLIA_MONITOR,
            RoleCommand.START, Service.Type.GANGLIA, 5, 5, 4));

    ActionDBAccessor db = mock(ActionDBAccessor.class);

    RequestEntity request = mock(RequestEntity.class);
    when(request.isExclusive()).thenReturn(false);
    when(db.getRequestEntity(anyLong())).thenReturn(request);

    when(db.getCommandsInProgressCount()).thenReturn(stages.size());
    when(db.getStagesInProgress()).thenReturn(stages);

    Properties properties = new Properties();
    Configuration conf = new Configuration(properties);
    ActionScheduler scheduler = spy(new ActionScheduler(100, 50, db, aq, fsm, 3,
            new HostsMap((String) null), unitOfWork, null, conf));

    doReturn(false).when(scheduler).wasAgentRestartedDuringOperation(any(Host.class), any(Stage.class), anyString());

    scheduler.doWork();

    Assert.assertEquals(HostRoleStatus.QUEUED, stages.get(0).getHostRoleStatus(hostname1, "DATANODE"));
    Assert.assertEquals(HostRoleStatus.PENDING, stages.get(1).getHostRoleStatus(hostname1, "GANGLIA_MONITOR"));
    Assert.assertEquals(HostRoleStatus.QUEUED, stages.get(2).getHostRoleStatus(hostname2, "DATANODE"));
    Assert.assertEquals(HostRoleStatus.QUEUED, stages.get(3).getHostRoleStatus(hostname3, "DATANODE"));
    Assert.assertEquals(HostRoleStatus.PENDING, stages.get(4).getHostRoleStatus(hostname4, "GANGLIA_MONITOR"));
  }


  /**
   * Verifies that ActionScheduler respects "disable parallel stage execution option"
   */
  @Test
  public void testIndependentStagesExecutionDisabled() throws Exception {
    ActionQueue aq = new ActionQueue();
    Clusters fsm = mock(Clusters.class);
    Cluster oneClusterMock = mock(Cluster.class);
    Service serviceObj = mock(Service.class);
    ServiceComponent scomp = mock(ServiceComponent.class);
    ServiceComponentHost sch = mock(ServiceComponentHost.class);
    UnitOfWork unitOfWork = mock(UnitOfWork.class);
    when(fsm.getCluster(anyString())).thenReturn(oneClusterMock);
    when(oneClusterMock.getService(anyString())).thenReturn(serviceObj);
    when(serviceObj.getServiceComponent(anyString())).thenReturn(scomp);
    when(scomp.getServiceComponentHost(anyString())).thenReturn(sch);
    when(serviceObj.getCluster()).thenReturn(oneClusterMock);

    String hostname1 = "ahost.ambari.apache.org";
    String hostname2 = "bhost.ambari.apache.org";
    String hostname3 = "chost.ambari.apache.org";
    String hostname4 = "chost.ambari.apache.org";
    HashMap<String, ServiceComponentHost> hosts =
            new HashMap<String, ServiceComponentHost>();
    hosts.put(hostname1, sch);
    hosts.put(hostname2, sch);
    hosts.put(hostname3, sch);
    hosts.put(hostname4, sch);
    when(scomp.getServiceComponentHosts()).thenReturn(hosts);

    List<Stage> stages = new ArrayList<Stage>();
    Stage stage = getStageWithSingleTask(
            hostname1, "cluster1", Role.HIVE_CLIENT,
            RoleCommand.INSTALL, Service.Type.HIVE, 1, 1, 1);
    Map<String, String> hiveSite = new TreeMap<String, String>();
    hiveSite.put("javax.jdo.option.ConnectionPassword", "password");
    hiveSite.put("hive.server2.thrift.port", "10000");
    Map<String, Map<String, String>> configurations =
            new TreeMap<String, Map<String, String>>();
    configurations.put("hive-site", hiveSite);
    stage.getExecutionCommands(hostname1).get(0).getExecutionCommand().setConfigurations(configurations);
    stages.add(stage);

    stages.add( // Stage with the same hostname, should not be scheduled
        getStageWithSingleTask(
            hostname1, "cluster1", Role.GANGLIA_MONITOR,
            RoleCommand.START, Service.Type.GANGLIA, 2, 2, 2));

    stages.add(
        getStageWithSingleTask(
            hostname2, "cluster1", Role.HIVE_CLIENT,
            RoleCommand.INSTALL, Service.Type.HIVE, 3, 3, 3));

    stages.add(
        getStageWithSingleTask(
            hostname3, "cluster1", Role.DATANODE,
            RoleCommand.START, Service.Type.HDFS, 4, 4, 4));

    stages.add( // Stage with the same request id, should not be scheduled
        getStageWithSingleTask(
            hostname4, "cluster1", Role.GANGLIA_MONITOR,
            RoleCommand.START, Service.Type.GANGLIA, 5, 5, 4));

    ActionDBAccessor db = mock(ActionDBAccessor.class);

    RequestEntity request = mock(RequestEntity.class);
    when(request.isExclusive()).thenReturn(false);
    when(db.getRequestEntity(anyLong())).thenReturn(request);

    when(db.getCommandsInProgressCount()).thenReturn(stages.size());
    when(db.getStagesInProgress()).thenReturn(stages);

    Properties properties = new Properties();
    properties.put(Configuration.PARALLEL_STAGE_EXECUTION_KEY, "false");
    Configuration conf = new Configuration(properties);
    ActionScheduler scheduler = spy(new ActionScheduler(100, 50, db, aq, fsm, 3,
            new HostsMap((String) null),
            unitOfWork, null, conf));


    doReturn(false).when(scheduler).wasAgentRestartedDuringOperation(any(Host.class), any(Stage.class), anyString());

    scheduler.doWork();

    Assert.assertEquals(HostRoleStatus.QUEUED, stages.get(0).getHostRoleStatus(hostname1, "HIVE_CLIENT"));
    Assert.assertEquals(HostRoleStatus.PENDING, stages.get(1).getHostRoleStatus(hostname1, "GANGLIA_MONITOR"));
    Assert.assertEquals(HostRoleStatus.PENDING, stages.get(2).getHostRoleStatus(hostname2, "HIVE_CLIENT"));
    Assert.assertEquals(HostRoleStatus.PENDING, stages.get(3).getHostRoleStatus(hostname3, "DATANODE"));
    Assert.assertEquals(HostRoleStatus.PENDING, stages.get(4).getHostRoleStatus(hostname4, "GANGLIA_MONITOR"));
    Assert.assertFalse(stages.get(0).getExecutionCommands(hostname1).get(0).getExecutionCommand().
            getConfigurations().containsKey("javax.jdo.option.ConnectionPassword"));
  }
  /**
   * Verifies that ActionScheduler allows to execute background tasks in parallel
   */
  @Test
  public void testBackgroundStagesExecutionEnable() throws Exception {
    ActionQueue aq = new ActionQueue();
    Clusters fsm = mock(Clusters.class);
    Cluster oneClusterMock = mock(Cluster.class);
    Service serviceObj = mock(Service.class);
    ServiceComponent scomp = mock(ServiceComponent.class);
    ServiceComponentHost sch = mock(ServiceComponentHost.class);
    UnitOfWork unitOfWork = mock(UnitOfWork.class);
    when(fsm.getCluster(anyString())).thenReturn(oneClusterMock);
    when(oneClusterMock.getService(anyString())).thenReturn(serviceObj);
    when(serviceObj.getServiceComponent(anyString())).thenReturn(scomp);
    when(scomp.getServiceComponentHost(anyString())).thenReturn(sch);
    when(serviceObj.getCluster()).thenReturn(oneClusterMock);

    String hostname1 = "ahost.ambari.apache.org";
    String hostname2 = "bhost.ambari.apache.org";
    HashMap<String, ServiceComponentHost> hosts =
        new HashMap<String, ServiceComponentHost>();
    hosts.put(hostname1, sch);
    hosts.put(hostname2, sch);
    when(scomp.getServiceComponentHosts()).thenReturn(hosts);

    List<Stage> stages = new ArrayList<Stage>();
    Stage backgroundStage = null;
    stages.add(//stage with background command
        backgroundStage = getStageWithSingleTask(
            hostname1, "cluster1", Role.NAMENODE, RoleCommand.CUSTOM_COMMAND, "REBALANCEHDFS", Service.Type.HDFS, 1, 1, 1));

    Assert.assertEquals(AgentCommandType.BACKGROUND_EXECUTION_COMMAND ,backgroundStage.getExecutionCommands(hostname1).get(0).getExecutionCommand().getCommandType());

    stages.add( // Stage with the same hostname, should be scheduled
        getStageWithSingleTask(
            hostname1, "cluster1", Role.GANGLIA_MONITOR,
            RoleCommand.START, Service.Type.GANGLIA, 2, 2, 2));

    stages.add(
        getStageWithSingleTask(
            hostname2, "cluster1", Role.DATANODE,
            RoleCommand.START, Service.Type.HDFS, 3, 3, 3));


    ActionDBAccessor db = mock(ActionDBAccessor.class);

    RequestEntity request = mock(RequestEntity.class);
    when(request.isExclusive()).thenReturn(false);
    when(db.getRequestEntity(anyLong())).thenReturn(request);

    when(db.getCommandsInProgressCount()).thenReturn(stages.size());
    when(db.getStagesInProgress()).thenReturn(stages);

    Properties properties = new Properties();
    properties.put(Configuration.PARALLEL_STAGE_EXECUTION_KEY, "true");
    Configuration conf = new Configuration(properties);
    ActionScheduler scheduler = spy(new ActionScheduler(100, 50, db, aq, fsm, 3,
        new HostsMap((String) null),
        unitOfWork, null, conf));

    doReturn(false).when(scheduler).wasAgentRestartedDuringOperation(any(Host.class), any(Stage.class), anyString());

    scheduler.doWork();

    Assert.assertEquals(HostRoleStatus.QUEUED, stages.get(0).getHostRoleStatus(hostname1, "NAMENODE"));
    Assert.assertEquals(HostRoleStatus.QUEUED, stages.get(2).getHostRoleStatus(hostname2, "DATANODE"));

    Assert.assertEquals(HostRoleStatus.QUEUED, stages.get(1).getHostRoleStatus(hostname1, "GANGLIA_MONITOR"));
  }

  @Test
  public void testRequestFailureOnStageFailure() throws Exception {
    ActionQueue aq = new ActionQueue();
    Clusters fsm = mock(Clusters.class);
    Cluster oneClusterMock = mock(Cluster.class);
    Service serviceObj = mock(Service.class);
    ServiceComponent scomp = mock(ServiceComponent.class);
    ServiceComponentHost sch = mock(ServiceComponentHost.class);
    UnitOfWork unitOfWork = mock(UnitOfWork.class);
    RequestFactory requestFactory = mock(RequestFactory.class);
    when(fsm.getCluster(anyString())).thenReturn(oneClusterMock);
    when(oneClusterMock.getService(anyString())).thenReturn(serviceObj);
    when(serviceObj.getServiceComponent(anyString())).thenReturn(scomp);
    when(scomp.getServiceComponentHost(anyString())).thenReturn(sch);
    when(serviceObj.getCluster()).thenReturn(oneClusterMock);

    HashMap<String, ServiceComponentHost> hosts =
            new HashMap<String, ServiceComponentHost>();
    hosts.put(hostname, sch);
    when(scomp.getServiceComponentHosts()).thenReturn(hosts);

    final List<Stage> stages = new ArrayList<Stage>();
    stages.add(
        getStageWithSingleTask(
            hostname, "cluster1", Role.NAMENODE, RoleCommand.UPGRADE, Service.Type.HDFS, 1, 1, 1));
    stages.add(
        getStageWithSingleTask(
            hostname, "cluster1", Role.DATANODE, RoleCommand.UPGRADE, Service.Type.HDFS, 2, 2, 1));

    Host host = mock(Host.class);
    when(fsm.getHost(anyString())).thenReturn(host);
    when(host.getState()).thenReturn(HostState.HEALTHY);
    when(host.getHostName()).thenReturn(hostname);

    ActionDBAccessor db = mock(ActionDBAccessor.class);

    RequestEntity request = mock(RequestEntity.class);
    when(request.isExclusive()).thenReturn(false);
    when(db.getRequestEntity(anyLong())).thenReturn(request);

    when(db.getCommandsInProgressCount()).thenReturn(stages.size());
    when(db.getStagesInProgress()).thenReturn(stages);
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        List<CommandReport> reports = (List<CommandReport>) invocation.getArguments()[0];
        for (CommandReport report : reports) {
          String actionId = report.getActionId();
          long[] requestStageIds = StageUtils.getRequestStage(actionId);
          Long requestId = requestStageIds[0];
          Long stageId = requestStageIds[1];
          Long id = report.getTaskId();
          for (Stage stage : stages) {
            if (requestId.equals(stage.getRequestId()) && stageId.equals(stage.getStageId())) {
              for (HostRoleCommand hostRoleCommand : stage.getOrderedHostRoleCommands()) {
                if (hostRoleCommand.getTaskId() == id) {
                  hostRoleCommand.setStatus(HostRoleStatus.valueOf(report.getStatus()));
                }
              }
            }
          }

        }

        return null;
      }
    }).when(db).updateHostRoleStates(anyCollectionOf(CommandReport.class));

    when(db.getTask(anyLong())).thenAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        Long taskId = (Long) invocation.getArguments()[0];
        for (Stage stage : stages) {
          for (HostRoleCommand command : stage.getOrderedHostRoleCommands()) {
            if (taskId.equals(command.getTaskId())) {
              return command;
            }
          }
        }
        return null;
      }
    });
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        Long requestId = (Long) invocation.getArguments()[0];
        for (Stage stage : stages) {
          if (requestId.equals(stage.getRequestId())) {
            for (HostRoleCommand command : stage.getOrderedHostRoleCommands()) {
              if (command.getStatus() == HostRoleStatus.QUEUED ||
                  command.getStatus() == HostRoleStatus.IN_PROGRESS ||
                  command.getStatus() == HostRoleStatus.PENDING) {
                command.setStatus(HostRoleStatus.ABORTED);
              }
            }
          }
        }

        return null;
      }
    }).when(db).abortOperation(anyLong());

    Properties properties = new Properties();
    Configuration conf = new Configuration(properties);

    Capture<Collection<HostRoleCommand>> cancelCommandList = new Capture<Collection<HostRoleCommand>>();
    ActionScheduler scheduler = EasyMock.createMockBuilder(ActionScheduler.class).
        withConstructor((long)100, (long)50, db, aq, fsm, 3,
          new HostsMap((String) null),
          unitOfWork, EasyMock.createNiceMock(AmbariEventPublisher.class), conf).
          addMockedMethod("cancelHostRoleCommands").
          createMock();
    scheduler.cancelHostRoleCommands(EasyMock.capture(cancelCommandList),
            EasyMock.eq(ActionScheduler.FAILED_TASK_ABORT_REASONING));
    EasyMock.expectLastCall().once();
    EasyMock.replay(scheduler);

    ActionManager am = new ActionManager(
        2, 2, aq, fsm, db, new HostsMap((String) null), unitOfWork, requestFactory, conf,
            EasyMock.createNiceMock(AmbariEventPublisher.class));

    scheduler.doWork();

    List<CommandReport> reports = new ArrayList<CommandReport>();
    reports.add(getCommandReport(HostRoleStatus.FAILED, Role.NAMENODE, Service.Type.HDFS, "1-1", 1));
    am.processTaskResponse(hostname, reports, stages.get(0).getOrderedHostRoleCommands());

    scheduler.doWork();
    Assert.assertEquals(HostRoleStatus.FAILED, stages.get(0).getHostRoleStatus(hostname, "NAMENODE"));
    Assert.assertEquals(HostRoleStatus.ABORTED, stages.get(1).getHostRoleStatus(hostname, "DATANODE"));
    Assert.assertEquals(cancelCommandList.getValue().size(), 1);
    EasyMock.verify(scheduler);
  }

  /**
   * Tests that the whole request is aborted when there are no QUEUED tasks for a role and
   * success factor is not met. As long as there is one QUEUED task the request is not
   * aborted.
   * @throws Exception
   */
  @Test
  public void testRequestAbortsOnlyWhenNoQueuedTaskAndSuccessFactorUnmet() throws Exception {
    ActionQueue aq = new ActionQueue();
    Clusters fsm = mock(Clusters.class);
    Cluster oneClusterMock = mock(Cluster.class);
    Service serviceObj = mock(Service.class);
    ServiceComponent scomp = mock(ServiceComponent.class);
    ServiceComponentHost sch = mock(ServiceComponentHost.class);
    UnitOfWork unitOfWork = mock(UnitOfWork.class);
    when(fsm.getCluster(anyString())).thenReturn(oneClusterMock);
    when(oneClusterMock.getService(anyString())).thenReturn(serviceObj);
    when(serviceObj.getServiceComponent(anyString())).thenReturn(scomp);
    when(scomp.getServiceComponentHost(anyString())).thenReturn(sch);
    when(serviceObj.getCluster()).thenReturn(oneClusterMock);

    String host1 = "host1";
    String host2 = "host2";
    Host host = mock(Host.class);
    HashMap<String, ServiceComponentHost> hosts =
            new HashMap<String, ServiceComponentHost>();
    hosts.put(host1, sch);
    hosts.put(host2, sch);
    when(scomp.getServiceComponentHosts()).thenReturn(hosts);

    when(fsm.getHost(anyString())).thenReturn(host);
    when(host.getState()).thenReturn(HostState.HEALTHY);
    when(host.getHostName()).thenReturn(host1);

    HostEntity hostEntity1 = new HostEntity();
    HostEntity hostEntity2 = new HostEntity();
    hostEntity1.setHostName(host1);
    hostEntity2.setHostName(host2);
    hostDAO.create(hostEntity1);
    hostDAO.create(hostEntity2);

    final List<Stage> stages = new ArrayList<Stage>();

    long now = System.currentTimeMillis();
    Stage stage = stageFactory.createNew(1, "/tmp", "cluster1", 1L,
        "testRequestFailureBasedOnSuccessFactor", CLUSTER_HOST_INFO, "", "");
    stage.setStageId(1);

    addHostRoleExecutionCommand(now, stage, Role.SQOOP, Service.Type.SQOOP,
        RoleCommand.INSTALL, host1, "cluster1");

    addHostRoleExecutionCommand(now, stage, Role.OOZIE_CLIENT, Service.Type.OOZIE,
        RoleCommand.INSTALL, host1, "cluster1");

    addHostRoleExecutionCommand(now, stage, Role.MAPREDUCE_CLIENT, Service.Type.MAPREDUCE,
        RoleCommand.INSTALL, host1, "cluster1");

    addHostRoleExecutionCommand(now, stage, Role.HBASE_CLIENT, Service.Type.HBASE,
        RoleCommand.INSTALL, host1, "cluster1");

    addHostRoleExecutionCommand(now, stage, Role.GANGLIA_MONITOR, Service.Type.GANGLIA,
        RoleCommand.INSTALL, host1, "cluster1");

    addHostRoleExecutionCommand(now, stage, Role.HBASE_CLIENT, Service.Type.HBASE,
        RoleCommand.INSTALL, host2, "cluster1");

    addHostRoleExecutionCommand(now, stage, Role.GANGLIA_MONITOR, Service.Type.GANGLIA,
        RoleCommand.INSTALL, host2, "cluster1");

    stages.add(stage);

    HostRoleStatus[] statusesAtIterOne = {HostRoleStatus.QUEUED, HostRoleStatus.QUEUED,
        HostRoleStatus.QUEUED, HostRoleStatus.QUEUED, HostRoleStatus.FAILED,
        HostRoleStatus.FAILED, HostRoleStatus.QUEUED, HostRoleStatus.QUEUED};
    for (int index = 0; index < stage.getOrderedHostRoleCommands().size(); index++) {
      stage.getOrderedHostRoleCommands().get(index).setTaskId(index + 1);
      stage.getOrderedHostRoleCommands().get(index).setStatus(statusesAtIterOne[index]);
    }

    stage.setLastAttemptTime(host1, Role.SQOOP.toString(), now);
    stage.setLastAttemptTime(host1, Role.MAPREDUCE_CLIENT.toString(), now);
    stage.setLastAttemptTime(host1, Role.OOZIE_CLIENT.toString(), now);
    stage.setLastAttemptTime(host1, Role.GANGLIA_MONITOR.toString(), now);
    stage.setLastAttemptTime(host1, Role.HBASE_CLIENT.toString(), now);
    stage.setLastAttemptTime(host2, Role.GANGLIA_MONITOR.toString(), now);
    stage.setLastAttemptTime(host2, Role.HBASE_CLIENT.toString(), now);

    ActionDBAccessor db = mock(ActionDBAccessor.class);

    RequestEntity request = mock(RequestEntity.class);
    when(request.isExclusive()).thenReturn(false);
    when(db.getRequestEntity(anyLong())).thenReturn(request);

    when(db.getCommandsInProgressCount()).thenReturn(stages.size());
    when(db.getStagesInProgress()).thenReturn(stages);
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        String host = (String) invocation.getArguments()[0];
        Long requestId = (Long) invocation.getArguments()[1];
        Long stageId = (Long) invocation.getArguments()[2];
        String role = (String) invocation.getArguments()[3];
        CommandReport commandReport = (CommandReport) invocation.getArguments()[4];
        for (Stage stage : stages) {
          if (requestId.equals(stage.getRequestId()) && stageId.equals(stage.getStageId())) {
            HostRoleCommand command = stage.getHostRoleCommand(host, role);
            command.setStatus(HostRoleStatus.valueOf(commandReport.getStatus()));
          }
        }

        return null;
      }
    }).when(db).updateHostRoleState(anyString(), anyLong(), anyLong(), anyString(), any(CommandReport.class));

    when(db.getTask(anyLong())).thenAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        Long taskId = (Long) invocation.getArguments()[0];
        for (Stage stage : stages) {
          for (HostRoleCommand command : stage.getOrderedHostRoleCommands()) {
            if (taskId.equals(command.getTaskId())) {
              return command;
            }
          }
        }
        return null;
      }
    });
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        Long requestId = (Long) invocation.getArguments()[0];
        for (Stage stage : stages) {
          if (requestId.equals(stage.getRequestId())) {
            for (HostRoleCommand command : stage.getOrderedHostRoleCommands()) {
              if (command.getStatus() == HostRoleStatus.QUEUED ||
                  command.getStatus() == HostRoleStatus.IN_PROGRESS ||
                  command.getStatus() == HostRoleStatus.PENDING) {
                command.setStatus(HostRoleStatus.ABORTED);
              }
            }
          }
        }

        return null;
      }
    }).when(db).abortOperation(anyLong());

    Properties properties = new Properties();
    Configuration conf = new Configuration(properties);
    ActionScheduler scheduler = new ActionScheduler(100, 10000, db, aq, fsm, 3,
        new HostsMap((String) null),
        unitOfWork, null, conf);

    scheduler.doWork();

    // Request is not aborted because all roles are in progress
    HostRoleStatus[] expectedStatusesAtIterOne = {HostRoleStatus.QUEUED, HostRoleStatus.QUEUED,
        HostRoleStatus.QUEUED, HostRoleStatus.QUEUED, HostRoleStatus.FAILED,
        HostRoleStatus.FAILED, HostRoleStatus.QUEUED, HostRoleStatus.QUEUED};
    for (int index = 0; index < stage.getOrderedHostRoleCommands().size(); index++) {
      log.info(stage.getOrderedHostRoleCommands().get(index).toString());
      Assert.assertEquals(expectedStatusesAtIterOne[index],
          stage.getOrderedHostRoleCommands().get(index).getStatus());
    }

    HostRoleStatus[] statusesAtIterTwo = {HostRoleStatus.QUEUED, HostRoleStatus.QUEUED,
        HostRoleStatus.QUEUED, HostRoleStatus.QUEUED, HostRoleStatus.FAILED,
        HostRoleStatus.FAILED, HostRoleStatus.QUEUED, HostRoleStatus.COMPLETED};
    for (int index = 0; index < stage.getOrderedHostRoleCommands().size(); index++) {
      stage.getOrderedHostRoleCommands().get(index).setStatus(statusesAtIterTwo[index]);
    }

    scheduler.doWork();

    // Request is not aborted because GANGLIA_MONITOR's success factor (0.5) is met
    HostRoleStatus[] expectedStatusesAtIterTwo = {HostRoleStatus.QUEUED, HostRoleStatus.QUEUED,
        HostRoleStatus.QUEUED, HostRoleStatus.QUEUED, HostRoleStatus.FAILED,
        HostRoleStatus.FAILED, HostRoleStatus.QUEUED, HostRoleStatus.COMPLETED};
    for (int index = 0; index < stage.getOrderedHostRoleCommands().size(); index++) {
      log.info(stage.getOrderedHostRoleCommands().get(index).toString());
      Assert.assertEquals(expectedStatusesAtIterTwo[index],
          stage.getOrderedHostRoleCommands().get(index).getStatus());
    }

    HostRoleStatus[] statusesAtIterThree = {HostRoleStatus.QUEUED, HostRoleStatus.QUEUED,
        HostRoleStatus.QUEUED, HostRoleStatus.QUEUED, HostRoleStatus.FAILED,
        HostRoleStatus.FAILED, HostRoleStatus.FAILED, HostRoleStatus.COMPLETED};
    for (int index = 0; index < stage.getOrderedHostRoleCommands().size(); index++) {
      stage.getOrderedHostRoleCommands().get(index).setStatus(statusesAtIterThree[index]);
    }

    // Fails becuse HostRoleCommand doesn't have a hostName
    scheduler.doWork();

    // Request is aborted because HBASE_CLIENT's success factor (1) is not met
    HostRoleStatus[] expectedStatusesAtIterThree = {HostRoleStatus.ABORTED, HostRoleStatus.ABORTED,
        HostRoleStatus.ABORTED, HostRoleStatus.ABORTED, HostRoleStatus.FAILED,
        HostRoleStatus.FAILED, HostRoleStatus.FAILED, HostRoleStatus.COMPLETED};
    for (int index = 0; index < stage.getOrderedHostRoleCommands().size(); index++) {
      log.info(stage.getOrderedHostRoleCommands().get(index).toString());
      Assert.assertEquals(expectedStatusesAtIterThree[index],
          stage.getOrderedHostRoleCommands().get(index).getStatus());
    }
  }

  private void addHostRoleExecutionCommand(long now, Stage stage, Role role, Service.Type service,
                                           RoleCommand command, String host, String cluster) {
    stage.addHostRoleExecutionCommand(host, role, command,
        new ServiceComponentHostInstallEvent(role.toString(), host, now, "HDP-0.2"),
        cluster, service.toString(), false, false);
    stage.getExecutionCommandWrapper(host,
        role.toString()).getExecutionCommand();
  }

  @Test
  public void testRequestFailureBasedOnSuccessFactor() throws Exception {
    ActionQueue aq = new ActionQueue();
    Clusters fsm = mock(Clusters.class);
    Cluster oneClusterMock = mock(Cluster.class);
    Service serviceObj = mock(Service.class);
    ServiceComponent scomp = mock(ServiceComponent.class);
    ServiceComponentHost sch = mock(ServiceComponentHost.class);
    UnitOfWork unitOfWork = mock(UnitOfWork.class);
    RequestFactory requestFactory = mock(RequestFactory.class);
    when(fsm.getCluster(anyString())).thenReturn(oneClusterMock);
    when(oneClusterMock.getService(anyString())).thenReturn(serviceObj);
    when(serviceObj.getServiceComponent(anyString())).thenReturn(scomp);
    when(scomp.getServiceComponentHost(anyString())).thenReturn(sch);
    when(serviceObj.getCluster()).thenReturn(oneClusterMock);

    final List<Stage> stages = new ArrayList<Stage>();

    long now = System.currentTimeMillis();
    Stage stage = stageFactory.createNew(1, "/tmp", "cluster1", 1L, "testRequestFailureBasedOnSuccessFactor",
      CLUSTER_HOST_INFO, "", "");
    stage.setStageId(1);
    stage.addHostRoleExecutionCommand("host1", Role.DATANODE, RoleCommand.UPGRADE,
        new ServiceComponentHostUpgradeEvent(Role.DATANODE.toString(), "host1", now, "HDP-0.2"),
        "cluster1", Service.Type.HDFS.toString(), false, false);
    stage.getExecutionCommandWrapper("host1",
        Role.DATANODE.toString()).getExecutionCommand();

    stage.addHostRoleExecutionCommand("host2", Role.DATANODE, RoleCommand.UPGRADE,
        new ServiceComponentHostUpgradeEvent(Role.DATANODE.toString(), "host2", now, "HDP-0.2"),
        "cluster1", Service.Type.HDFS.toString(), false, false);
    stage.getExecutionCommandWrapper("host2",
        Role.DATANODE.toString()).getExecutionCommand();

    stage.addHostRoleExecutionCommand("host3", Role.DATANODE, RoleCommand.UPGRADE,
        new ServiceComponentHostUpgradeEvent(Role.DATANODE.toString(), "host3", now, "HDP-0.2"),
        "cluster1", Service.Type.HDFS.toString(), false, false);
    stage.getExecutionCommandWrapper("host3",
        Role.DATANODE.toString()).getExecutionCommand();
    stages.add(stage);

    stage.getOrderedHostRoleCommands().get(0).setTaskId(1);
    stage.getOrderedHostRoleCommands().get(1).setTaskId(2);
    stage.getOrderedHostRoleCommands().get(2).setTaskId(3);

    stages.add(
        getStageWithSingleTask(
            "host1", "cluster1", Role.HDFS_CLIENT, RoleCommand.UPGRADE, Service.Type.HDFS, 4, 2, 1));

    ActionDBAccessor db = mock(ActionDBAccessor.class);

    RequestEntity request = mock(RequestEntity.class);
    when(request.isExclusive()).thenReturn(false);
    when(db.getRequestEntity(anyLong())).thenReturn(request);

    when(db.getCommandsInProgressCount()).thenReturn(stages.size());
    when(db.getStagesInProgress()).thenReturn(stages);
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        List<CommandReport> reports = (List<CommandReport>) invocation.getArguments()[0];
        for (CommandReport report : reports) {
          String actionId = report.getActionId();
          long[] requestStageIds = StageUtils.getRequestStage(actionId);
          Long requestId = requestStageIds[0];
          Long stageId = requestStageIds[1];
          Long id = report.getTaskId();
          for (Stage stage : stages) {
            if (requestId.equals(stage.getRequestId()) && stageId.equals(stage.getStageId())) {
              for (HostRoleCommand hostRoleCommand : stage.getOrderedHostRoleCommands()) {
                if (hostRoleCommand.getTaskId() == id) {
                  hostRoleCommand.setStatus(HostRoleStatus.valueOf(report.getStatus()));
                }
              }
            }
          }

        }

        return null;
      }
    }).when(db).updateHostRoleStates(anyCollectionOf(CommandReport.class));

    when(db.getTask(anyLong())).thenAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        Long taskId = (Long) invocation.getArguments()[0];
        for (Stage stage : stages) {
          for (HostRoleCommand command : stage.getOrderedHostRoleCommands()) {
            if (taskId.equals(command.getTaskId())) {
              return command;
            }
          }
        }
        return null;
      }
    });
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        Long requestId = (Long) invocation.getArguments()[0];
        for (Stage stage : stages) {
          if (requestId.equals(stage.getRequestId())) {
            for (HostRoleCommand command : stage.getOrderedHostRoleCommands()) {
              if (command.getStatus() == HostRoleStatus.QUEUED ||
                  command.getStatus() == HostRoleStatus.IN_PROGRESS ||
                  command.getStatus() == HostRoleStatus.PENDING) {
                command.setStatus(HostRoleStatus.ABORTED);
              }
            }
          }
        }

        return null;
      }
    }).when(db).abortOperation(anyLong());

    Properties properties = new Properties();
    Configuration conf = new Configuration(properties);
    ActionScheduler scheduler = new ActionScheduler(100, 50, db, aq, fsm, 3,
        new HostsMap((String) null),
        unitOfWork, null, conf);
    ActionManager am = new ActionManager(
        2, 2, aq, fsm, db, new HostsMap((String) null), unitOfWork, requestFactory, conf, null);

    scheduler.doWork();

    List<CommandReport> reports = new ArrayList<CommandReport>();
    reports.add(getCommandReport(HostRoleStatus.FAILED, Role.DATANODE, Service.Type.HDFS, "1-1", 1));
    am.processTaskResponse("host1", reports, stage.getOrderedHostRoleCommands());

    reports.clear();
    reports.add(getCommandReport(HostRoleStatus.FAILED, Role.DATANODE, Service.Type.HDFS, "1-1", 2));
    am.processTaskResponse("host2", reports, stage.getOrderedHostRoleCommands());

    reports.clear();
    reports.add(getCommandReport(HostRoleStatus.COMPLETED, Role.DATANODE, Service.Type.HDFS, "1-1", 3));
    am.processTaskResponse("host3", reports, stage.getOrderedHostRoleCommands());

    scheduler.doWork();
    Assert.assertEquals(HostRoleStatus.ABORTED, stages.get(1).getHostRoleStatus("host1", "HDFS_CLIENT"));
  }

  private CommandReport getCommandReport(HostRoleStatus status, Role role, Service.Type service, String actionId,
                                         int taskId) {
    CommandReport report = new CommandReport();
    report.setExitCode(999);
    report.setStdErr("");
    report.setStdOut("");
    report.setStatus(status.toString());
    report.setRole(role.toString());
    report.setServiceName(service.toString());
    report.setActionId(actionId);
    report.setTaskId(taskId);
    return report;
  }

  private Stage createStage(String clusterName, int stageId, int requestId) {
    Stage stage = stageFactory.createNew(requestId, "/tmp", clusterName, 1L, "getStageWithSingleTask",
      CLUSTER_HOST_INFO, "{\"host_param\":\"param_value\"}", "{\"stage_param\":\"param_value\"}");
    stage.setStageId(stageId);
    //stage.setAutoSkipFailureSupported(true);
    return stage;
  }

  private Stage addTask(Stage stage, String hostname, String clusterName, Role role,
                        RoleCommand roleCommand, String serviceName, int taskId) {
    stage.addHostRoleExecutionCommand(hostname, role, roleCommand,
        new ServiceComponentHostUpgradeEvent(role.toString(), hostname, System.currentTimeMillis(), "HDP-0.2"),
        clusterName, serviceName, false, false);
    stage.getExecutionCommandWrapper(hostname,
        role.toString()).getExecutionCommand();
    stage.getOrderedHostRoleCommands().get(0).setTaskId(taskId);
    return stage;
  }

  private Stage getStageWithSingleTask(String hostname, String clusterName, Role role,
                                       RoleCommand roleCommand, Service.Type service, int taskId,
                                       int stageId, int requestId) {
    Stage stage = createStage(clusterName, stageId, requestId);
    return addTask(stage, hostname, clusterName, role, roleCommand, service.name(), taskId);
  }


  private Stage getStageWithSingleTask(String hostname, String clusterName, Role role, RoleCommand roleCommand,
      String customCommandName, Service.Type service, int taskId, int stageId, int requestId) {
    Stage stage = getStageWithSingleTask(hostname, clusterName, role, roleCommand, service, taskId, stageId, requestId);

    HostRoleCommand cmd = stage.getHostRoleCommand(hostname, role.name());
    if (cmd != null) {
      cmd.setCustomCommandName(customCommandName);
    }

    stage.getExecutionCommandWrapper(hostname, role.toString()).getExecutionCommand().setCommandType(AgentCommandType.BACKGROUND_EXECUTION_COMMAND);
    return stage;
  }

  private void addInstallTaskToStage(Stage stage, String hostname,
                              String clusterName, Role role,
                              RoleCommand roleCommand, Service.Type service,
                              int taskId) {

    stage.addHostRoleExecutionCommand(hostname, role, roleCommand,
      new ServiceComponentHostInstallEvent(role.toString(), hostname,
        System.currentTimeMillis(), "HDP-0.2"), clusterName, service.toString(), false, false);
    ExecutionCommand command = stage.getExecutionCommandWrapper
      (hostname, role.toString()).getExecutionCommand();
    command.setTaskId(taskId);
    for (HostRoleCommand cmd :stage.getOrderedHostRoleCommands()) {
      if (cmd.getHostName().equals(hostname) && cmd.getRole().equals(role)) {
        cmd.setTaskId(taskId);
      }
    }
  }

  @Test
  public void testSuccessFactors() {
    Stage s = StageUtils.getATestStage(1, 1, CLUSTER_HOST_INFO,
      "{\"host_param\":\"param_value\"}", "{\"stage_param\":\"param_value\"}");
    assertEquals(new Float(0.5), new Float(s.getSuccessFactor(Role.DATANODE)));
    assertEquals(new Float(0.5), new Float(s.getSuccessFactor(Role.TASKTRACKER)));
    assertEquals(new Float(0.5), new Float(s.getSuccessFactor(Role.GANGLIA_MONITOR)));
    assertEquals(new Float(0.5), new Float(s.getSuccessFactor(Role.HBASE_REGIONSERVER)));
    assertEquals(new Float(1.0), new Float(s.getSuccessFactor(Role.NAMENODE)));
    assertEquals(new Float(1.0), new Float(s.getSuccessFactor(Role.GANGLIA_SERVER)));
  }

  @Test
  public void testSuccessCriteria() {
    RoleStats rs1 = new RoleStats(1, (float)0.5);
    rs1.numSucceeded = 1;
    assertTrue(rs1.isSuccessFactorMet());
    rs1.numSucceeded = 0;
    assertFalse(rs1.isSuccessFactorMet());

    RoleStats rs2 = new RoleStats(2, (float)0.5);
    rs2.numSucceeded = 1;
    assertTrue(rs2.isSuccessFactorMet());

    RoleStats rs3 = new RoleStats(3, (float)0.5);
    rs3.numSucceeded = 2;
    assertTrue(rs2.isSuccessFactorMet());
    rs3.numSucceeded = 1;
    assertFalse(rs3.isSuccessFactorMet());

    RoleStats rs4 = new RoleStats(3, (float)1.0);
    rs4.numSucceeded = 2;
    assertFalse(rs3.isSuccessFactorMet());
  }

  /**
   * This test sends verifies that ActionScheduler returns up-to-date cluster host info and caching works correctly.
   */
  @Test
  public void testClusterHostInfoCache() throws Exception {

    Type type = new TypeToken<Map<String, Set<String>>>() {}.getType();

    //Data for stages
    Map<String, Set<String>> clusterHostInfo1 = StageUtils.getGson().fromJson(CLUSTER_HOST_INFO, type);
    Map<String, Set<String>> clusterHostInfo2 = StageUtils.getGson().fromJson(CLUSTER_HOST_INFO_UPDATED, type);
    int stageId = 1;
    int requestId1 = 1;
    int requestId2 = 2;

    ActionQueue aq = new ActionQueue();
    Properties properties = new Properties();
    Configuration conf = new Configuration(properties);
    Clusters fsm = mock(Clusters.class);
    Cluster oneClusterMock = mock(Cluster.class);
    Service serviceObj = mock(Service.class);
    ServiceComponent scomp = mock(ServiceComponent.class);
    ServiceComponentHost sch = mock(ServiceComponentHost.class);
    UnitOfWork unitOfWork = mock(UnitOfWork.class);
    when(fsm.getCluster(anyString())).thenReturn(oneClusterMock);
    when(oneClusterMock.getService(anyString())).thenReturn(serviceObj);
    when(serviceObj.getServiceComponent(anyString())).thenReturn(scomp);
    when(scomp.getServiceComponentHost(anyString())).thenReturn(sch);
    when(serviceObj.getCluster()).thenReturn(oneClusterMock);
    Host host = mock(Host.class);
    HashMap<String, ServiceComponentHost> hosts =
            new HashMap<String, ServiceComponentHost>();
    hosts.put(hostname, sch);
    when(scomp.getServiceComponentHosts()).thenReturn(hosts);

    when(fsm.getHost(anyString())).thenReturn(host);
    when(host.getState()).thenReturn(HostState.HEALTHY);
    when(host.getHostName()).thenReturn(hostname);

    ActionDBAccessor db = mock(ActionDBAccessorImpl.class);

    RequestEntity request = mock(RequestEntity.class);
    when(request.isExclusive()).thenReturn(false);
    when(db.getRequestEntity(anyLong())).thenReturn(request);

    Stage s1 = StageUtils.getATestStage(requestId1, stageId, hostname, CLUSTER_HOST_INFO,
      "{\"host_param\":\"param_value\"}", "{\"stage_param\":\"param_value\"}");
    Stage s2 = StageUtils.getATestStage(requestId2, stageId, hostname, CLUSTER_HOST_INFO_UPDATED,
      "{\"host_param\":\"param_value\"}", "{\"stage_param\":\"param_value\"}");

    when(db.getCommandsInProgressCount()).thenReturn(1);
    when(db.getStagesInProgress()).thenReturn(Collections.singletonList(s1));

    //Keep large number of attempts so that the task is not expired finally
    //Small action timeout to test rescheduling
    ActionScheduler scheduler = new ActionScheduler(100, 100, db, aq, fsm,
        10000, new HostsMap((String) null), unitOfWork, null, conf);
    scheduler.setTaskTimeoutAdjustment(false);

    List<AgentCommand> ac = waitForQueueSize(hostname, aq, 1, scheduler);

    assertTrue(ac.get(0) instanceof ExecutionCommand);
    assertEquals(String.valueOf(requestId1) + "-" + stageId, ((ExecutionCommand) (ac.get(0))).getCommandId());

    assertEquals(clusterHostInfo1, ((ExecutionCommand) (ac.get(0))).getClusterHostInfo());

    when(db.getCommandsInProgressCount()).thenReturn(1);
    when(db.getStagesInProgress()).thenReturn(Collections.singletonList(s2));

    //Verify that ActionSheduler does not return cached value of cluster host info for new requestId
    ac = waitForQueueSize(hostname, aq, 1, scheduler);
    assertTrue(ac.get(0) instanceof ExecutionCommand);
    assertEquals(String.valueOf(requestId2) + "-" + stageId, ((ExecutionCommand) (ac.get(0))).getCommandId());
    assertEquals(clusterHostInfo2, ((ExecutionCommand) (ac.get(0))).getClusterHostInfo());
  }


  /**
   * Checks what happens when stage has an execution command for
   * host component that has been recently deleted
   * @throws Exception
   */
  @Test
  public void testCommandAbortForDeletedComponent() throws Exception {
    ActionQueue aq = new ActionQueue();
    Properties properties = new Properties();
    Configuration conf = new Configuration(properties);
    Clusters fsm = mock(Clusters.class);
    Cluster oneClusterMock = mock(Cluster.class);
    Service serviceObj = mock(Service.class);
    ServiceComponent scomp = mock(ServiceComponent.class);
    ServiceComponent scWithDeletedSCH = mock(ServiceComponent.class);
    ServiceComponentHost sch1 = mock(ServiceComponentHost.class);
    String hostname1 = "host1";
    Host host1 = mock(Host.class);
    when(fsm.getHost(hostname1)).thenReturn(host1);
    when(host1.getState()).thenReturn(HostState.HEALTHY);
    when(host1.getHostName()).thenReturn(hostname1);
    when(scomp.getServiceComponentHost(hostname1)).thenReturn(sch1);
    HashMap<String, ServiceComponentHost> hosts =
            new HashMap<String, ServiceComponentHost>();
    hosts.put(hostname1, sch1);
    when(scomp.getServiceComponentHosts()).thenReturn(hosts);

    HostEntity hostEntity = new HostEntity();
    hostEntity.setHostName(hostname1);
    hostDAO.create(hostEntity);

    UnitOfWork unitOfWork = mock(UnitOfWork.class);
    when(fsm.getCluster(anyString())).thenReturn(oneClusterMock);
    when(oneClusterMock.getService(anyString())).thenReturn(serviceObj);
    when(serviceObj.getServiceComponent(Role.HBASE_MASTER.toString())).
            thenReturn(scWithDeletedSCH);
    when(serviceObj.getServiceComponent(Role.HBASE_REGIONSERVER.toString())).
            thenReturn(scomp);
    when(scWithDeletedSCH.getServiceComponentHost(anyString())).
            thenThrow(new ServiceComponentHostNotFoundException("dummyCluster",
                "dummyService", "dummyComponent", "dummyHostname"));
    when(serviceObj.getCluster()).thenReturn(oneClusterMock);

    final List<Stage> stages = new ArrayList<Stage>();
    Stage stage1 = stageFactory.createNew(1, "/tmp", "cluster1", 1L, "stageWith2Tasks",
            CLUSTER_HOST_INFO, "", "");
    addInstallTaskToStage(stage1, hostname1, "cluster1", Role.HBASE_MASTER,
            RoleCommand.INSTALL, Service.Type.HBASE, 1);
    addInstallTaskToStage(stage1, hostname1, "cluster1", Role.HBASE_REGIONSERVER,
            RoleCommand.INSTALL, Service.Type.HBASE, 2);
    stages.add(stage1);

    ActionDBAccessor db = mock(ActionDBAccessor.class);

    RequestEntity request = mock(RequestEntity.class);
    when(request.isExclusive()).thenReturn(false);
    when(db.getRequestEntity(anyLong())).thenReturn(request);

    when(db.getCommandsInProgressCount()).thenReturn(stages.size());
    when(db.getStagesInProgress()).thenReturn(stages);

    ActionScheduler scheduler = new ActionScheduler(100, 50000, db, aq, fsm, 3,
            new HostsMap((String) null), unitOfWork, null, conf);

    final CountDownLatch abortCalls = new CountDownLatch(2);

    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
      Long requestId = (Long) invocation.getArguments()[0];
      for (Stage stage : stages) {
        if (requestId.equals(stage.getRequestId())) {
          for (HostRoleCommand command : stage.getOrderedHostRoleCommands()) {
            if (command.getStatus() == HostRoleStatus.QUEUED ||
                    command.getStatus() == HostRoleStatus.IN_PROGRESS ||
                    command.getStatus() == HostRoleStatus.PENDING) {
              command.setStatus(HostRoleStatus.ABORTED);
            }
          }
        }
      }
      abortCalls.countDown();
      return null;
      }
    }).when(db).abortOperation(anyLong());

    scheduler.setTaskTimeoutAdjustment(false);
    // Start the thread
    scheduler.start();

    long timeout = 60;
    abortCalls.await(timeout, TimeUnit.SECONDS);

    Assert.assertEquals(HostRoleStatus.ABORTED,
            stages.get(0).getHostRoleStatus(hostname1, "HBASE_MASTER"));
    Assert.assertEquals(HostRoleStatus.ABORTED,
            stages.get(0).getHostRoleStatus(hostname1, "HBASE_REGIONSERVER"));

    // If regression occured, scheduler thread would fail with an exception
    // instead of aborting request
    verify(db, times(2)).abortOperation(anyLong());

    scheduler.stop();
  }


  @Test
  public void testServerActionWOService() throws Exception {
    ActionQueue aq = new ActionQueue();
    Properties properties = new Properties();
    Configuration conf = new Configuration(properties);
    Clusters fsm = mock(Clusters.class);
    UnitOfWork unitOfWork = mock(UnitOfWork.class);

    List<Stage> stages = new ArrayList<Stage>();
    Map<String, String> payload = new HashMap<String, String>();
    final Stage s = getStageWithServerAction(1, 977, payload, "test", 300);
    stages.add(s);

    ActionDBAccessor db = mock(ActionDBAccessor.class);

    RequestEntity request = mock(RequestEntity.class);
    when(request.isExclusive()).thenReturn(false);
    when(db.getRequestEntity(anyLong())).thenReturn(request);

    when(db.getCommandsInProgressCount()).thenReturn(stages.size());
    when(db.getStagesInProgress()).thenReturn(stages);
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        String host = (String) invocation.getArguments()[0];
        String role = (String) invocation.getArguments()[3];
        CommandReport commandReport = (CommandReport) invocation.getArguments()[4];

        HostRoleCommand command = null;
        if (null == host) {
          command = s.getHostRoleCommand(null, role);
        } else {
          command = s.getHostRoleCommand(host, role);
        }

        command.setStatus(HostRoleStatus.valueOf(commandReport.getStatus()));
        return null;
      }
    }).when(db).updateHostRoleState(anyString(), anyLong(), anyLong(), anyString(), any(CommandReport.class));

    doAnswer(new Answer<List<HostRoleCommand>>() {
      @Override
      public List<HostRoleCommand> answer(InvocationOnMock invocation) throws Throwable {
        String role = (String) invocation.getArguments()[0];
        HostRoleStatus status = (HostRoleStatus) invocation.getArguments()[1];

        HostRoleCommand task = s.getHostRoleCommand(null, role);

        if (task.getStatus() == status) {
          return Arrays.asList(task);
        } else {
          return Collections.emptyList();
        }
      }
    }).when(db).getTasksByRoleAndStatus(anyString(), any(HostRoleStatus.class));

    doAnswer(new Answer<HostRoleCommand>() {
      @Override
      public HostRoleCommand answer(InvocationOnMock invocation) throws Throwable {
        return s.getHostRoleCommand(null, "AMBARI_SERVER_ACTION");
      }
    }).when(db).getTask(anyLong());

    ServerActionExecutor.init(injector);
    ActionScheduler scheduler = new ActionScheduler(100, 50, db, aq, fsm, 3,
        new HostsMap((String) null), unitOfWork, null, conf);

    int cycleCount = 0;
    while (!stages.get(0).getHostRoleStatus(null, "AMBARI_SERVER_ACTION")
        .equals(HostRoleStatus.COMPLETED) && cycleCount++ <= MAX_CYCLE_ITERATIONS) {
      scheduler.doWork();
      scheduler.getServerActionExecutor().doWork();
    }

    assertEquals(stages.get(0).getHostRoleStatus(null, "AMBARI_SERVER_ACTION"),
            HostRoleStatus.COMPLETED);
  }

  @Test
  public void testCancelRequests() throws Exception {
    ActionQueue aq = new ActionQueue();
    Clusters fsm = mock(Clusters.class);
    Cluster oneClusterMock = mock(Cluster.class);
    Service serviceObj = mock(Service.class);
    ServiceComponent scomp = mock(ServiceComponent.class);
    ServiceComponentHost sch = mock(ServiceComponentHost.class);
    UnitOfWork unitOfWork = mock(UnitOfWork.class);
    when(fsm.getCluster(anyString())).thenReturn(oneClusterMock);
    when(oneClusterMock.getService(anyString())).thenReturn(serviceObj);
    when(serviceObj.getServiceComponent(anyString())).thenReturn(scomp);
    when(scomp.getServiceComponentHost(anyString())).thenReturn(sch);
    when(serviceObj.getCluster()).thenReturn(oneClusterMock);

    HostEntity hostEntity = new HostEntity();
    hostEntity.setHostName(hostname);
    hostDAO.create(hostEntity);

    HashMap<String, ServiceComponentHost> hosts =
            new HashMap<String, ServiceComponentHost>();
    hosts.put(hostname, sch);
    when(scomp.getServiceComponentHosts()).thenReturn(hosts);

    long requestId = 1;

    final List<Stage> stages = new ArrayList<Stage>();
    int namenodeCmdTaskId = 1;
    stages.add(
            getStageWithSingleTask(
                    hostname, "cluster1", Role.NAMENODE, RoleCommand.START,
                    Service.Type.HDFS, namenodeCmdTaskId, 1, (int)requestId));
    stages.add(
            getStageWithSingleTask(
                    hostname, "cluster1", Role.DATANODE, RoleCommand.START,
                    Service.Type.HDFS, 2, 2, (int)requestId));

    Host host = mock(Host.class);
    when(fsm.getHost(anyString())).thenReturn(host);
    when(host.getState()).thenReturn(HostState.HEALTHY);
    when(host.getHostName()).thenReturn(hostname);

    ActionDBAccessor db = mock(ActionDBAccessor.class);

    RequestEntity request = mock(RequestEntity.class);
    when(request.isExclusive()).thenReturn(false);
    when(db.getRequestEntity(anyLong())).thenReturn(request);

    when(db.getCommandsInProgressCount()).thenReturn(stages.size());
    when(db.getStagesInProgress()).thenReturn(stages);

    List<HostRoleCommand> requestTasks = new ArrayList<HostRoleCommand>();
    for (Stage stage : stages) {
      requestTasks.addAll(stage.getOrderedHostRoleCommands());
    }
    when(db.getRequestTasks(anyLong())).thenReturn(requestTasks);
    when(db.getAllStages(anyLong())).thenReturn(stages);
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        List<CommandReport> reports = (List<CommandReport>) invocation.getArguments()[0];
        for (CommandReport report : reports) {
          String actionId = report.getActionId();
          long[] requestStageIds = StageUtils.getRequestStage(actionId);
          Long requestId = requestStageIds[0];
          Long stageId = requestStageIds[1];
          Long id = report.getTaskId();
          for (Stage stage : stages) {
            if (requestId.equals(stage.getRequestId()) && stageId.equals(stage.getStageId())) {
              for (HostRoleCommand hostRoleCommand : stage.getOrderedHostRoleCommands()) {
                if (hostRoleCommand.getTaskId() == id) {
                  hostRoleCommand.setStatus(HostRoleStatus.valueOf(report.getStatus()));
                }
              }
            }
          }

        }

        return null;
      }
    }).when(db).updateHostRoleStates(anyCollectionOf(CommandReport.class));

    when(db.getTask(anyLong())).thenAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        Long taskId = (Long) invocation.getArguments()[0];
        for (Stage stage : stages) {
          for (HostRoleCommand command : stage.getOrderedHostRoleCommands()) {
            if (taskId.equals(command.getTaskId())) {
              return command;
            }
          }
        }
        return null;
      }
    });
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        Long requestId = (Long) invocation.getArguments()[0];
        for (Stage stage : stages) {
          if (requestId.equals(stage.getRequestId())) {
            for (HostRoleCommand command : stage.getOrderedHostRoleCommands()) {
              if (command.getStatus() == HostRoleStatus.QUEUED ||
                      command.getStatus() == HostRoleStatus.IN_PROGRESS ||
                      command.getStatus() == HostRoleStatus.PENDING) {
                command.setStatus(HostRoleStatus.ABORTED);
              }
            }
          }
        }

        return null;
      }
    }).when(db).abortOperation(anyLong());

    Properties properties = new Properties();
    Configuration conf = new Configuration(properties);

    ActionScheduler scheduler = new ActionScheduler(100, 50, db, aq, fsm, 3,
        new HostsMap((String) null), unitOfWork, null, conf);

    scheduler.doWork();

    String reason = "Some reason";

    scheduler.scheduleCancellingRequest(requestId, reason);

    scheduler.doWork();
    Assert.assertEquals(HostRoleStatus.ABORTED, stages.get(0).getHostRoleStatus(hostname, "NAMENODE"));
    Assert.assertEquals(HostRoleStatus.ABORTED, stages.get(1).getHostRoleStatus(hostname, "DATANODE"));
    Assert.assertEquals(aq.size(hostname), 1); // Cancel commands should be generated only for 1 stage

    CancelCommand cancelCommand = (CancelCommand) aq.dequeue(hostname);
    Assert.assertEquals(cancelCommand.getTargetTaskId(), namenodeCmdTaskId);
    Assert.assertEquals(cancelCommand.getReason(), reason);
  }


  @Test
  public void testExclusiveRequests() throws Exception {
    ActionQueue aq = new ActionQueue();
    Clusters fsm = mock(Clusters.class);
    Cluster oneClusterMock = mock(Cluster.class);
    Service serviceObj = mock(Service.class);
    ServiceComponent scomp = mock(ServiceComponent.class);
    ServiceComponentHost sch = mock(ServiceComponentHost.class);
    UnitOfWork unitOfWork = mock(UnitOfWork.class);
    when(fsm.getCluster(anyString())).thenReturn(oneClusterMock);
    when(oneClusterMock.getService(anyString())).thenReturn(serviceObj);
    when(serviceObj.getServiceComponent(anyString())).thenReturn(scomp);
    when(scomp.getServiceComponentHost(anyString())).thenReturn(sch);
    when(serviceObj.getCluster()).thenReturn(oneClusterMock);

    HashMap<String, ServiceComponentHost> hosts =
            new HashMap<String, ServiceComponentHost>();
    String hostname1 = "hostname1";
    String hostname2 = "hostname2";
    String hostname3 = "hostname3";

    hosts.put(hostname1, sch);
    hosts.put(hostname2, sch);
    hosts.put(hostname3, sch);
    when(scomp.getServiceComponentHosts()).thenReturn(hosts);

    long requestId1 = 1;
    long requestId2 = 2;
    long requestId3 = 3;

    final List<Stage> stagesInProgress = new ArrayList<Stage>();
    int namenodeCmdTaskId = 1;
    stagesInProgress.add(
            getStageWithSingleTask(
                    hostname1, "cluster1", Role.NAMENODE, RoleCommand.START,
                    Service.Type.HDFS, namenodeCmdTaskId, 1, (int) requestId1));
    stagesInProgress.add(
            getStageWithSingleTask(
                    hostname1, "cluster1", Role.DATANODE, RoleCommand.START,
                    Service.Type.HDFS, 2, 2, (int) requestId1));
    stagesInProgress.add(
            getStageWithSingleTask(
                    hostname2, "cluster1", Role.DATANODE, RoleCommand.STOP, //Exclusive
                    Service.Type.HDFS, 3, 3, (int) requestId2));

    stagesInProgress.add(
            getStageWithSingleTask(
                    hostname3, "cluster1", Role.DATANODE, RoleCommand.START,
                    Service.Type.HDFS, 4, 4, (int) requestId3));


    Host host1 = mock(Host.class);
    when(fsm.getHost(anyString())).thenReturn(host1);
    when(host1.getState()).thenReturn(HostState.HEALTHY);
    when(host1.getHostName()).thenReturn(hostname);

    Host host2 = mock(Host.class);
    when(fsm.getHost(anyString())).thenReturn(host2);
    when(host2.getState()).thenReturn(HostState.HEALTHY);
    when(host2.getHostName()).thenReturn(hostname);

    Host host3 = mock(Host.class);
    when(fsm.getHost(anyString())).thenReturn(host3);
    when(host3.getState()).thenReturn(HostState.HEALTHY);
    when(host3.getHostName()).thenReturn(hostname);

    ActionDBAccessor db = mock(ActionDBAccessor.class);
    when(db.getCommandsInProgressCount()).thenReturn(stagesInProgress.size());
    when(db.getStagesInProgress()).thenReturn(stagesInProgress);

    List<HostRoleCommand> requestTasks = new ArrayList<HostRoleCommand>();
    for (Stage stage : stagesInProgress) {
      requestTasks.addAll(stage.getOrderedHostRoleCommands());
    }
    when(db.getRequestTasks(anyLong())).thenReturn(requestTasks);
    when(db.getAllStages(anyLong())).thenReturn(stagesInProgress);
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        List<CommandReport> reports = (List<CommandReport>) invocation.getArguments()[0];
        for (CommandReport report : reports) {
          String actionId = report.getActionId();
          long[] requestStageIds = StageUtils.getRequestStage(actionId);
          Long requestId = requestStageIds[0];
          Long stageId = requestStageIds[1];
          Long id = report.getTaskId();
          for (Stage stage : stagesInProgress) {
            if (requestId.equals(stage.getRequestId()) && stageId.equals(stage.getStageId())) {
              for (HostRoleCommand hostRoleCommand : stage.getOrderedHostRoleCommands()) {
                if (hostRoleCommand.getTaskId() == id) {
                  hostRoleCommand.setStatus(HostRoleStatus.valueOf(report.getStatus()));
                }
              }
            }
          }

        }

        return null;
      }
    }).when(db).updateHostRoleStates(anyCollectionOf(CommandReport.class));

    when(db.getTask(anyLong())).thenAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        Long taskId = (Long) invocation.getArguments()[0];
        for (Stage stage : stagesInProgress) {
          for (HostRoleCommand command : stage.getOrderedHostRoleCommands()) {
            if (taskId.equals(command.getTaskId())) {
              return command;
            }
          }
        }
        return null;
      }
    });

    final Map<Long, Boolean> startedRequests = new HashMap<Long, Boolean>();
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        startedRequests.put((Long)invocation.getArguments()[0], true);
        return null;
      }
    }).when(db).startRequest(anyLong());

    RequestEntity request1 = mock(RequestEntity.class);
    when(request1.isExclusive()).thenReturn(false);
    RequestEntity request2 = mock(RequestEntity.class);
    when(request2.isExclusive()).thenReturn(true);
    RequestEntity request3 = mock(RequestEntity.class);
    when(request3.isExclusive()).thenReturn(false);

    when(db.getRequestEntity(requestId1)).thenReturn(request1);
    when(db.getRequestEntity(requestId2)).thenReturn(request2);
    when(db.getRequestEntity(requestId3)).thenReturn(request3);

    Properties properties = new Properties();
    Configuration conf = new Configuration(properties);

    ActionScheduler scheduler = spy(new ActionScheduler(100, 50, db, aq, fsm, 3,
        new HostsMap((String) null), unitOfWork, null, conf));

    doReturn(false).when(scheduler).wasAgentRestartedDuringOperation(any(Host.class), any(Stage.class), anyString());

    // Execution of request 1

    scheduler.doWork();

    Assert.assertTrue(startedRequests.containsKey(requestId1));
    Assert.assertFalse(startedRequests.containsKey(requestId2));
    Assert.assertFalse(startedRequests.containsKey(requestId3));

    stagesInProgress.remove(0);

    scheduler.doWork();

    Assert.assertTrue(startedRequests.containsKey(requestId1));
    Assert.assertFalse(startedRequests.containsKey(requestId2));
    Assert.assertFalse(startedRequests.containsKey(requestId3));

    // Execution of request 2

    stagesInProgress.remove(0);

    scheduler.doWork();

    Assert.assertTrue(startedRequests.containsKey(requestId1));
    Assert.assertTrue(startedRequests.containsKey(requestId2));
    Assert.assertFalse(startedRequests.containsKey(requestId3));

    // Execution of request 3

    stagesInProgress.remove(0);

    scheduler.doWork();

    Assert.assertTrue(startedRequests.containsKey(requestId1));
    Assert.assertTrue(startedRequests.containsKey(requestId2));
    Assert.assertTrue(startedRequests.containsKey(requestId3));

  }

  @Test
  public void testAbortHolding() {
    UnitOfWork unitOfWork = EasyMock.createMock(UnitOfWork.class);
    ActionDBAccessor db = EasyMock.createMock(ActionDBAccessor.class);
    ActionQueue aq = new ActionQueue();
    Clusters fsm = EasyMock.createMock(Clusters.class);
    Configuration conf = new Configuration(new Properties());
    HostEntity hostEntity1 = new HostEntity();
    hostEntity1.setHostName("h1");
    hostDAO.merge(hostEntity1);

    db.abortHostRole("h1", -1L, -1L, "AMBARI_SERVER_ACTION");
    EasyMock.expectLastCall();

    EasyMock.replay(db);

    ActionScheduler scheduler = new ActionScheduler(100, 50, db, aq, fsm, 3,
        new HostsMap((String) null),
        unitOfWork, null, conf);

    HostRoleCommand hrc1 = hostRoleCommandFactory.create("h1", Role.NAMENODE, null, RoleCommand.EXECUTE);
    hrc1.setStatus(HostRoleStatus.COMPLETED);
    HostRoleCommand hrc3 = hostRoleCommandFactory.create("h1", Role.AMBARI_SERVER_ACTION, null, RoleCommand.CUSTOM_COMMAND);
    hrc3.setStatus(HostRoleStatus.HOLDING);
    HostRoleCommand hrc4 = hostRoleCommandFactory.create("h1", Role.FLUME_HANDLER, null, RoleCommand.EXECUTE);
    hrc4.setStatus(HostRoleStatus.PENDING);

    List<HostRoleCommand> hostRoleCommands = Arrays.asList(hrc1, hrc3, hrc4);

    scheduler.cancelHostRoleCommands(hostRoleCommands, "foo");

    EasyMock.verify(db);

  }

  /**
   * Tests that command failures in skippable stages do not cause the request to
   * be aborted.
   */
  @Test
  public void testSkippableCommandFailureDoesNotAbortRequest() throws Exception {
    Properties properties = new Properties();
    Configuration conf = new Configuration(properties);
    ActionQueue aq = new ActionQueue();
    Clusters fsm = mock(Clusters.class);
    Cluster oneClusterMock = mock(Cluster.class);
    Service serviceObj = mock(Service.class);
    ServiceComponent scomp = mock(ServiceComponent.class);
    ServiceComponentHost sch = mock(ServiceComponentHost.class);
    UnitOfWork unitOfWork = mock(UnitOfWork.class);
    when(fsm.getCluster(anyString())).thenReturn(oneClusterMock);
    when(oneClusterMock.getService(anyString())).thenReturn(serviceObj);
    when(serviceObj.getServiceComponent(anyString())).thenReturn(scomp);
    when(scomp.getServiceComponentHost(anyString())).thenReturn(sch);
    when(serviceObj.getCluster()).thenReturn(oneClusterMock);

    String hostname1 = "ahost.ambari.apache.org";

    HashMap<String, ServiceComponentHost> hosts = new HashMap<String, ServiceComponentHost>();

    hosts.put(hostname1, sch);

    when(scomp.getServiceComponentHosts()).thenReturn(hosts);

    // create 1 stage with 2 commands and then another stage with 1 command
    Stage stage = null;
    Stage stage2 = null;
    final List<Stage> stages = new ArrayList<Stage>();
    stages.add(stage = getStageWithSingleTask(hostname1, "cluster1", Role.NAMENODE,
        RoleCommand.STOP, Service.Type.HDFS, 1, 1, 1));

    addInstallTaskToStage(stage, hostname1, "cluster1", Role.HBASE_MASTER, RoleCommand.INSTALL,
        Service.Type.HBASE, 1);

    stages.add(stage2 = getStageWithSingleTask(hostname1, "cluster1", Role.DATANODE,
        RoleCommand.STOP, Service.Type.HDFS, 1, 1, 1));

    // !!! this is the test; make the stages skippable so that when their
    // commands fail, the entire request is not aborted
    for (Stage stageToMakeSkippable : stages) {
      stageToMakeSkippable.setSkippable(true);
    }

    // fail the first task - normally this would cause an abort, exception that our stages
    // are skippable now so it should not
    HostRoleCommand command = stage.getOrderedHostRoleCommands().iterator().next();
    command.setStatus(HostRoleStatus.FAILED);

    ActionDBAccessor db = mock(ActionDBAccessor.class);

    RequestEntity request = mock(RequestEntity.class);
    when(request.isExclusive()).thenReturn(false);
    when(db.getRequestEntity(anyLong())).thenReturn(request);

    when(db.getCommandsInProgressCount()).thenReturn(stages.size());
    when(db.getStagesInProgress()).thenReturn(stages);

    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        List<CommandReport> reports = (List<CommandReport>) invocation.getArguments()[0];
        for (CommandReport report : reports) {
          String actionId = report.getActionId();
          long[] requestStageIds = StageUtils.getRequestStage(actionId);
          Long requestId = requestStageIds[0];
          Long stageId = requestStageIds[1];
          Long id = report.getTaskId();
          for (Stage stage : stages) {
            if (requestId.equals(stage.getRequestId()) && stageId.equals(stage.getStageId())) {
              for (HostRoleCommand hostRoleCommand : stage.getOrderedHostRoleCommands()) {
                if (hostRoleCommand.getTaskId() == id) {
                  hostRoleCommand.setStatus(HostRoleStatus.valueOf(report.getStatus()));
                }
              }
            }
          }

        }

        return null;
      }
    }).when(db).updateHostRoleStates(anyCollectionOf(CommandReport.class));

    when(db.getTask(anyLong())).thenAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        Long taskId = (Long) invocation.getArguments()[0];
        for (Stage stage : stages) {
          for (HostRoleCommand command : stage.getOrderedHostRoleCommands()) {
            if (taskId.equals(command.getTaskId())) {
              return command;
            }
          }
        }
        return null;
      }
    });
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        Long requestId = (Long) invocation.getArguments()[0];
        for (Stage stage : stages) {
          if (requestId.equals(stage.getRequestId())) {
            for (HostRoleCommand command : stage.getOrderedHostRoleCommands()) {
              if (command.getStatus() == HostRoleStatus.QUEUED
                  || command.getStatus() == HostRoleStatus.IN_PROGRESS
                  || command.getStatus() == HostRoleStatus.PENDING) {
                command.setStatus(HostRoleStatus.ABORTED);
              }
            }
          }
        }

        return null;
      }
    }).when(db).abortOperation(anyLong());

    ActionScheduler scheduler = spy(new ActionScheduler(100, 50, db, aq, fsm, 3,
        new HostsMap((String) null), unitOfWork, null, conf));

    doReturn(false).when(scheduler).wasAgentRestartedDuringOperation(any(Host.class), any(Stage.class), anyString());

    scheduler.doWork();

    Assert.assertEquals(HostRoleStatus.FAILED,
        stages.get(0).getHostRoleStatus(hostname1, "NAMENODE"));

    // the remaining tasks should NOT have been aborted since the stage is
    // skippable - these tasks would normally be ABORTED if the stage was not
    // skippable
    Assert.assertEquals(HostRoleStatus.QUEUED,
        stages.get(0).getHostRoleStatus(hostname1, "HBASE_MASTER"));

    Assert.assertEquals(HostRoleStatus.PENDING,
        stages.get(1).getHostRoleStatus(hostname1, "DATANODE"));

  }

  public static class MockModule extends AbstractModule {
    @Override
    protected void configure() {
      bind(Clusters.class).toInstance(mock(Clusters.class));
    }
  }

}
