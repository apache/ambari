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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.google.common.reflect.TypeToken;
import com.google.inject.persist.UnitOfWork;
import junit.framework.Assert;
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
import org.apache.ambari.server.serveraction.ServerAction;
import org.apache.ambari.server.serveraction.ServerActionManager;
import org.apache.ambari.server.serveraction.ServerActionManagerImpl;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceComponentHostEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostInstallEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostOpFailedEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostUpgradeEvent;
import org.apache.ambari.server.utils.StageUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestActionScheduler {

  private static final Logger log = LoggerFactory.getLogger(TestActionScheduler.class);
  private static final String CLUSTER_HOST_INFO = "{all_hosts=[c6403.ambari.apache.org," +
  		" c6401.ambari.apache.org, c6402.ambari.apache.org], slave_hosts=[c6403.ambari.apache.org," +
  		" c6401.ambari.apache.org, c6402.ambari.apache.org]}";
  private static final String CLUSTER_HOST_INFO_UPDATED = "{all_hosts=[c6401.ambari.apache.org,"
      + " c6402.ambari.apache.org], slave_hosts=[c6401.ambari.apache.org,"
      + " c6402.ambari.apache.org]}";
  private final String hostname = "ahost.ambari.apache.org";
  private final int MAX_CYCLE_ITERATIONS = 100;


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
    when(scomp.getServiceComponentHosts()).thenReturn(hosts);

    when(fsm.getHost(anyString())).thenReturn(host);
    when(host.getState()).thenReturn(HostState.HEALTHY);
    when(host.getHostName()).thenReturn(hostname);

    ActionDBAccessor db = mock(ActionDBAccessorImpl.class);
    List<Stage> stages = new ArrayList<Stage>();
    Stage s = StageUtils.getATestStage(1, 977, hostname, CLUSTER_HOST_INFO,
      "{\"host_param\":\"param_value\"}", "{\"stage_param\":\"param_value\"}");
    stages.add(s);
    when(db.getStagesInProgress()).thenReturn(stages);

    Request request = mock(Request.class);
    when(request.isExclusive()).thenReturn(false);
    when(db.getRequest(anyLong())).thenReturn(request);

    //Keep large number of attempts so that the task is not expired finally
    //Small action timeout to test rescheduling
    ActionScheduler scheduler = new ActionScheduler(100, 5, db, aq, fsm,
        10000, new HostsMap((String) null), null, unitOfWork, conf);
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

    List<Stage> stages = new ArrayList<Stage>();
    final Stage s = StageUtils.getATestStage(1, 977, hostname, CLUSTER_HOST_INFO,
      "{\"host_param\":\"param_value\"}", "{\"stage_param\":\"param_value\"}");
    stages.add(s);

    ActionDBAccessor db = mock(ActionDBAccessor.class);
    when(db.getStagesInProgress()).thenReturn(stages);
    Request request = mock(Request.class);
    when(request.isExclusive()).thenReturn(false);
    when(db.getRequest(anyLong())).thenReturn(request);

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        String host = (String) invocation.getArguments()[0];
        String role = (String) invocation.getArguments()[3];
        HostRoleCommand command = s.getHostRoleCommand(host, role);
        command.setStatus(HostRoleStatus.TIMEDOUT);
        return null;
      }
    }).when(db).timeoutHostRole(anyString(), anyLong(), anyLong(), anyString());


    //Small action timeout to test rescheduling
    ActionScheduler scheduler = new ActionScheduler(100, 0, db, aq, fsm, 3,
        new HostsMap((String) null), null, unitOfWork, conf);
    scheduler.setTaskTimeoutAdjustment(false);
    // Start the thread

    int cycleCount = 0;
    while (!stages.get(0).getHostRoleStatus(hostname, "NAMENODE")
        .equals(HostRoleStatus.TIMEDOUT) && cycleCount++ <= MAX_CYCLE_ITERATIONS) {
      scheduler.doWork();
    }
    assertEquals(stages.get(0).getHostRoleStatus(hostname, "NAMENODE"),
        HostRoleStatus.TIMEDOUT);

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

    Request request = mock(Request.class);
    when(request.isExclusive()).thenReturn(false);
    when(db.getRequest(anyLong())).thenReturn(request);

    when(db.getStagesInProgress()).thenReturn(stages);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        String host = (String) invocation.getArguments()[0];
        String role = (String) invocation.getArguments()[3];
        HostRoleCommand command = s.getHostRoleCommand(host, role);
        command.setStatus(HostRoleStatus.TIMEDOUT);
        return null;
      }
    }).when(db).timeoutHostRole(anyString(), anyLong(), anyLong(), anyString());

    ServerActionManager sam = EasyMock.createNiceMock(ServerActionManager.class);

    //Small action timeout to test rescheduling
    ActionScheduler scheduler = EasyMock.createMockBuilder(ActionScheduler.class).
        withConstructor((long) 100, (long) 50, db, aq, fsm, 3,
                new HostsMap((String) null), sam, unitOfWork, conf).
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
    Stage stage = new Stage(1, "/tmp", "cluster1", 1L, "stageWith2Tasks",
      CLUSTER_HOST_INFO, "{\"command_param\":\"param_value\"}", "{\"host_param\":\"param_value\"}");
    addInstallTaskToStage(stage, hostname1, "cluster1", Role.DATANODE,
      RoleCommand.INSTALL, Service.Type.HDFS, 1);
    addInstallTaskToStage(stage, hostname2, "cluster1", Role.NAMENODE,
      RoleCommand.INSTALL, Service.Type.HDFS, 2);
    stages.add(stage);

    ActionDBAccessor db = mock(ActionDBAccessor.class);

    Request request = mock(Request.class);
    when(request.isExclusive()).thenReturn(false);
    when(db.getRequest(anyLong())).thenReturn(request);

    when(db.getStagesInProgress()).thenReturn(stages);

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
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
    }).when(db).timeoutHostRole(anyString(), anyLong(), anyLong(), anyString());

    doAnswer(new Answer() {
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

        return null;
      }
    }).when(db).abortOperation(anyLong());

    ArgumentCaptor<ServiceComponentHostEvent> eventsCapture1 =
      ArgumentCaptor.forClass(ServiceComponentHostEvent.class);
    ArgumentCaptor<ServiceComponentHostEvent> eventsCapture2 =
      ArgumentCaptor.forClass(ServiceComponentHostEvent.class);

    // Make sure the NN install doesn't timeout
    ActionScheduler scheduler = new ActionScheduler(100, 50000, db, aq, fsm, 3,
      new HostsMap((String) null), null, unitOfWork, conf);
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
    hosts.put(hostname, sch);
    when(scomp.getServiceComponentHosts()).thenReturn(hosts);

    List<Stage> stages = new ArrayList<Stage>();
    Map<String, String> payload = new HashMap<String, String>();
    payload.put(ServerAction.PayloadName.CLUSTER_NAME, "cluster1");
    payload.put(ServerAction.PayloadName.CURRENT_STACK_VERSION, "HDP-0.2");
    final Stage s = getStageWithServerAction(1, 977, hostname, payload, "test");
    stages.add(s);

    ActionDBAccessor db = mock(ActionDBAccessor.class);

    Request request = mock(Request.class);
    when(request.isExclusive()).thenReturn(false);
    when(db.getRequest(anyLong())).thenReturn(request);

    when(db.getStagesInProgress()).thenReturn(stages);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        String host = (String) invocation.getArguments()[0];
        String role = (String) invocation.getArguments()[3];
        CommandReport commandReport = (CommandReport) invocation.getArguments()[4];
        HostRoleCommand command = s.getHostRoleCommand(host, role);
        command.setStatus(HostRoleStatus.valueOf(commandReport.getStatus()));
        return null;
      }
    }).when(db).updateHostRoleState(anyString(), anyLong(), anyLong(), anyString(), any(CommandReport.class));


    ActionScheduler scheduler = new ActionScheduler(100, 50, db, aq, fsm, 3,
        new HostsMap((String) null), new ServerActionManagerImpl(fsm),
        unitOfWork, conf);

    int cycleCount=0;
    while (!stages.get(0).getHostRoleStatus(hostname, "AMBARI_SERVER_ACTION")
        .equals(HostRoleStatus.COMPLETED) && cycleCount++ <= MAX_CYCLE_ITERATIONS) {
      scheduler.doWork();
    }

    assertEquals(stages.get(0).getHostRoleStatus(hostname, "AMBARI_SERVER_ACTION"),
        HostRoleStatus.COMPLETED);


  }

  @Test
  public void testServerActionFailed() throws Exception {
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

    HashMap<String, ServiceComponentHost> hosts =
            new HashMap<String, ServiceComponentHost>();
    hosts.put(hostname, sch);
    when(scomp.getServiceComponentHosts()).thenReturn(hosts);

    List<Stage> stages = new ArrayList<Stage>();
    Map<String, String> payload = new HashMap<String, String>();
    payload.put(ServerAction.PayloadName.CURRENT_STACK_VERSION, "HDP-0.2");
    final Stage s = getStageWithServerAction(1, 977, hostname, payload, "test");
    stages.add(s);

    ActionDBAccessor db = mock(ActionDBAccessor.class);

    Request request = mock(Request.class);
    when(request.isExclusive()).thenReturn(false);
    when(db.getRequest(anyLong())).thenReturn(request);

    when(db.getStagesInProgress()).thenReturn(stages);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        String host = (String) invocation.getArguments()[0];
        String role = (String) invocation.getArguments()[3];
        CommandReport commandReport = (CommandReport) invocation.getArguments()[4];
        HostRoleCommand command = s.getHostRoleCommand(host, role);
        command.setStatus(HostRoleStatus.valueOf(commandReport.getStatus()));
        return null;
      }
    }).when(db).updateHostRoleState(anyString(), anyLong(), anyLong(), anyString(), any(CommandReport.class));


    ActionScheduler scheduler = new ActionScheduler(100, 50, db, aq, fsm, 3,
        new HostsMap((String) null), new ServerActionManagerImpl(fsm), unitOfWork, conf);

    int cycleCount = 0;
    while (!stages.get(0).getHostRoleStatus(hostname, "AMBARI_SERVER_ACTION")
        .equals(HostRoleStatus.FAILED) && cycleCount++ <= MAX_CYCLE_ITERATIONS) {
      scheduler.doWork();
    }
    assertEquals(stages.get(0).getHostRoleStatus(hostname, "AMBARI_SERVER_ACTION"),
        HostRoleStatus.FAILED);
    assertEquals("test", stages.get(0).getRequestContext());
  }

  private static Stage getStageWithServerAction(long requestId, long stageId, String hostName,
                                                Map<String, String> payload, String requestContext) {
    Stage stage = new Stage(requestId, "/tmp", "cluster1", 1L, requestContext, CLUSTER_HOST_INFO,
      "", "");
    stage.setStageId(stageId);
    long now = System.currentTimeMillis();
    stage.addServerActionCommand(ServerAction.Command.FINALIZE_UPGRADE, Role.AMBARI_SERVER_ACTION,
        RoleCommand.EXECUTE, "cluster1",
        new ServiceComponentHostUpgradeEvent("AMBARI_SERVER_ACTION", hostName, now, "HDP-0.2"),
        hostName);
    ExecutionCommand execCmd = stage.getExecutionCommandWrapper(hostName,
        Role.AMBARI_SERVER_ACTION.toString()).getExecutionCommand();

    execCmd.setCommandParams(payload);
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
    RequestFactory requestFactory = mock(RequestFactory.class);
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

    Request request = mock(Request.class);
    when(request.isExclusive()).thenReturn(false);
    when(db.getRequest(anyLong())).thenReturn(request);

    when(db.getStagesInProgress()).thenReturn(stages);

    Properties properties = new Properties();
    Configuration conf = new Configuration(properties);
    ActionScheduler scheduler = new ActionScheduler(100, 50, db, aq, fsm, 3,
            new HostsMap((String) null), new ServerActionManagerImpl(fsm),
            unitOfWork, conf);

    ActionManager am = new ActionManager(
            2, 2, aq, fsm, db, new HostsMap((String) null),
            new ServerActionManagerImpl(fsm), unitOfWork, requestFactory, conf);

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
    RequestFactory requestFactory = mock(RequestFactory.class);
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

    Request request = mock(Request.class);
    when(request.isExclusive()).thenReturn(false);
    when(db.getRequest(anyLong())).thenReturn(request);

    when(db.getStagesInProgress()).thenReturn(stages);

    Properties properties = new Properties();
    properties.put(Configuration.PARALLEL_STAGE_EXECUTION_KEY, "false");
    Configuration conf = new Configuration(properties);
    ActionScheduler scheduler = new ActionScheduler(100, 50, db, aq, fsm, 3,
            new HostsMap((String) null), new ServerActionManagerImpl(fsm),
            unitOfWork, conf);

    ActionManager am = new ActionManager(
            2, 2, aq, fsm, db, new HostsMap((String) null),
            new ServerActionManagerImpl(fsm), unitOfWork,
            requestFactory, conf);

    scheduler.doWork();

    Assert.assertEquals(HostRoleStatus.QUEUED, stages.get(0).getHostRoleStatus(hostname1, "DATANODE"));
    Assert.assertEquals(HostRoleStatus.PENDING, stages.get(1).getHostRoleStatus(hostname1, "GANGLIA_MONITOR"));
    Assert.assertEquals(HostRoleStatus.PENDING, stages.get(2).getHostRoleStatus(hostname2, "DATANODE"));
    Assert.assertEquals(HostRoleStatus.PENDING, stages.get(3).getHostRoleStatus(hostname3, "DATANODE"));
    Assert.assertEquals(HostRoleStatus.PENDING, stages.get(4).getHostRoleStatus(hostname4, "GANGLIA_MONITOR"));
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
    RequestFactory requestFactory = mock(RequestFactory.class);
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

    Request request = mock(Request.class);
    when(request.isExclusive()).thenReturn(false);
    when(db.getRequest(anyLong())).thenReturn(request);

    when(db.getStagesInProgress()).thenReturn(stages);
    
    Properties properties = new Properties();
    properties.put(Configuration.PARALLEL_STAGE_EXECUTION_KEY, "true");
    Configuration conf = new Configuration(properties);
    ActionScheduler scheduler = new ActionScheduler(100, 50, db, aq, fsm, 3,
        new HostsMap((String) null), new ServerActionManagerImpl(fsm),
        unitOfWork, conf);
    
    ActionManager am = new ActionManager(
        2, 2, aq, fsm, db, new HostsMap((String) null),
        new ServerActionManagerImpl(fsm), unitOfWork,
        requestFactory, conf);
    
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

    Request request = mock(Request.class);
    when(request.isExclusive()).thenReturn(false);
    when(db.getRequest(anyLong())).thenReturn(request);

    when(db.getStagesInProgress()).thenReturn(stages);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        List<CommandReport> reports = (List<CommandReport>) invocation.getArguments()[0];
        for (CommandReport report : reports) {
          String actionId = report.getActionId();
          long[] requestStageIds = StageUtils.getRequestStage(actionId);
          Long requestId = requestStageIds[0];
          Long stageId = requestStageIds[1];
          String role = report.getRole();
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
    doAnswer(new Answer() {
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

        return null;
      }
    }).when(db).abortOperation(anyLong());

    Properties properties = new Properties();
    Configuration conf = new Configuration(properties);
    ServerActionManagerImpl serverActionManager = new ServerActionManagerImpl(fsm);

    Capture<Collection<HostRoleCommand>> cancelCommandList = new Capture<Collection<HostRoleCommand>>();
    ActionScheduler scheduler = EasyMock.createMockBuilder(ActionScheduler.class).
        withConstructor((long)100, (long)50, db, aq, fsm, 3,
          new HostsMap((String) null), serverActionManager,
          unitOfWork, conf).
          addMockedMethod("cancelHostRoleCommands").
          createMock();
    scheduler.cancelHostRoleCommands(EasyMock.capture(cancelCommandList),
            EasyMock.eq(ActionScheduler.FAILED_TASK_ABORT_REASONING));
    EasyMock.expectLastCall().once();
    EasyMock.replay(scheduler);

    ActionManager am = new ActionManager(
        2, 2, aq, fsm, db, new HostsMap((String) null), serverActionManager, unitOfWork, requestFactory, conf);

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
    RequestFactory requestFactory = mock(RequestFactory.class);
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

    final List<Stage> stages = new ArrayList<Stage>();

    long now = System.currentTimeMillis();
    Stage stage = new Stage(1, "/tmp", "cluster1", 1L,
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

    Request request = mock(Request.class);
    when(request.isExclusive()).thenReturn(false);
    when(db.getRequest(anyLong())).thenReturn(request);

    when(db.getStagesInProgress()).thenReturn(stages);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
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
    doAnswer(new Answer() {
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

        return null;
      }
    }).when(db).abortOperation(anyLong());

    Properties properties = new Properties();
    Configuration conf = new Configuration(properties);
    ActionScheduler scheduler = new ActionScheduler(100, 10000, db, aq, fsm, 3,
        new HostsMap((String) null), new ServerActionManagerImpl(fsm),
        unitOfWork, conf);
    ActionManager am = new ActionManager(
        2, 10000, aq, fsm, db, new HostsMap((String) null), new ServerActionManagerImpl(fsm), unitOfWork, requestFactory, conf);

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
        cluster, service.toString());
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
    Stage stage = new Stage(1, "/tmp", "cluster1", 1L, "testRequestFailureBasedOnSuccessFactor",
      CLUSTER_HOST_INFO, "", "");
    stage.setStageId(1);
    stage.addHostRoleExecutionCommand("host1", Role.DATANODE, RoleCommand.UPGRADE,
        new ServiceComponentHostUpgradeEvent(Role.DATANODE.toString(), "host1", now, "HDP-0.2"),
        "cluster1", Service.Type.HDFS.toString());
    stage.getExecutionCommandWrapper("host1",
        Role.DATANODE.toString()).getExecutionCommand();

    stage.addHostRoleExecutionCommand("host2", Role.DATANODE, RoleCommand.UPGRADE,
        new ServiceComponentHostUpgradeEvent(Role.DATANODE.toString(), "host2", now, "HDP-0.2"),
        "cluster1", Service.Type.HDFS.toString());
    stage.getExecutionCommandWrapper("host2",
        Role.DATANODE.toString()).getExecutionCommand();

    stage.addHostRoleExecutionCommand("host3", Role.DATANODE, RoleCommand.UPGRADE,
        new ServiceComponentHostUpgradeEvent(Role.DATANODE.toString(), "host3", now, "HDP-0.2"),
        "cluster1", Service.Type.HDFS.toString());
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

    Request request = mock(Request.class);
    when(request.isExclusive()).thenReturn(false);
    when(db.getRequest(anyLong())).thenReturn(request);

    when(db.getStagesInProgress()).thenReturn(stages);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        List<CommandReport> reports = (List<CommandReport>) invocation.getArguments()[0];
        for (CommandReport report : reports) {
          String actionId = report.getActionId();
          long[] requestStageIds = StageUtils.getRequestStage(actionId);
          Long requestId = requestStageIds[0];
          Long stageId = requestStageIds[1];
          String role = report.getRole();
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
    doAnswer(new Answer() {
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

        return null;
      }
    }).when(db).abortOperation(anyLong());

    Properties properties = new Properties();
    Configuration conf = new Configuration(properties);
    ActionScheduler scheduler = new ActionScheduler(100, 50, db, aq, fsm, 3,
        new HostsMap((String) null),
        new ServerActionManagerImpl(fsm), unitOfWork, conf);
    ActionManager am = new ActionManager(
        2, 2, aq, fsm, db, new HostsMap((String) null), new ServerActionManagerImpl(fsm), unitOfWork, requestFactory, conf);

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

  private Stage getStageWithSingleTask(String hostname, String clusterName, Role role,
                                       RoleCommand roleCommand, Service.Type service, int taskId,
                                       int stageId, int requestId) {
    Stage stage = new Stage(requestId, "/tmp", clusterName, 1L, "getStageWithSingleTask",
      CLUSTER_HOST_INFO, "{\"host_param\":\"param_value\"}", "{\"stage_param\":\"param_value\"}");
    stage.setStageId(stageId);
    stage.addHostRoleExecutionCommand(hostname, role, roleCommand,
        new ServiceComponentHostUpgradeEvent(role.toString(), hostname, System.currentTimeMillis(), "HDP-0.2"),
        clusterName, service.toString());
    stage.getExecutionCommandWrapper(hostname,
        role.toString()).getExecutionCommand();
    stage.getOrderedHostRoleCommands().get(0).setTaskId(taskId);
    return stage;
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
        System.currentTimeMillis(), "HDP-0.2"), clusterName, service.toString());
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

    Request request = mock(Request.class);
    when(request.isExclusive()).thenReturn(false);
    when(db.getRequest(anyLong())).thenReturn(request);

    Stage s1 = StageUtils.getATestStage(requestId1, stageId, hostname, CLUSTER_HOST_INFO,
      "{\"host_param\":\"param_value\"}", "{\"stage_param\":\"param_value\"}");
    Stage s2 = StageUtils.getATestStage(requestId2, stageId, hostname, CLUSTER_HOST_INFO_UPDATED,
      "{\"host_param\":\"param_value\"}", "{\"stage_param\":\"param_value\"}");
    when(db.getStagesInProgress()).thenReturn(Collections.singletonList(s1));

    //Keep large number of attempts so that the task is not expired finally
    //Small action timeout to test rescheduling
    ActionScheduler scheduler = new ActionScheduler(100, 100, db, aq, fsm,
        10000, new HostsMap((String) null), null, unitOfWork, conf);
    scheduler.setTaskTimeoutAdjustment(false);

    List<AgentCommand> ac = waitForQueueSize(hostname, aq, 1, scheduler);

    assertTrue(ac.get(0) instanceof ExecutionCommand);
    assertEquals(String.valueOf(requestId1) + "-" + stageId, ((ExecutionCommand) (ac.get(0))).getCommandId());
    
    assertEquals(clusterHostInfo1, ((ExecutionCommand) (ac.get(0))).getClusterHostInfo());
    

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
    Stage stage1 = new Stage(1, "/tmp", "cluster1", 1L, "stageWith2Tasks",
            CLUSTER_HOST_INFO, "", "");
    addInstallTaskToStage(stage1, hostname1, "cluster1", Role.HBASE_MASTER,
            RoleCommand.INSTALL, Service.Type.HBASE, 1);
    addInstallTaskToStage(stage1, hostname1, "cluster1", Role.HBASE_REGIONSERVER,
            RoleCommand.INSTALL, Service.Type.HBASE, 2);
    stages.add(stage1);

    ActionDBAccessor db = mock(ActionDBAccessor.class);

    Request request = mock(Request.class);
    when(request.isExclusive()).thenReturn(false);
    when(db.getRequest(anyLong())).thenReturn(request);

    when(db.getStagesInProgress()).thenReturn(stages);

    ActionScheduler scheduler = new ActionScheduler(100, 50000, db, aq, fsm, 3,
            new HostsMap((String) null), null, unitOfWork, conf);

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
    hosts.put(hostname, sch);
    when(scomp.getServiceComponentHosts()).thenReturn(hosts);

    List<Stage> stages = new ArrayList<Stage>();
    Map<String, String> payload = new HashMap<String, String>();
    payload.put(ServerAction.PayloadName.CLUSTER_NAME, "cluster1");
    payload.put(ServerAction.PayloadName.CURRENT_STACK_VERSION, "HDP-0.2");
    final Stage s = getStageWithServerAction(1, 977, hostname, payload, "test");
    s.getExecutionCommands().get("ahost.ambari.apache.org").get(0).getExecutionCommand().setServiceName(null);
    stages.add(s);

    ActionDBAccessor db = mock(ActionDBAccessor.class);

    Request request = mock(Request.class);
    when(request.isExclusive()).thenReturn(false);
    when(db.getRequest(anyLong())).thenReturn(request);

    when(db.getStagesInProgress()).thenReturn(stages);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        String host = (String) invocation.getArguments()[0];
        String role = (String) invocation.getArguments()[3];
        CommandReport commandReport = (CommandReport) invocation.getArguments()[4];
        HostRoleCommand command = s.getHostRoleCommand(host, role);
        command.setStatus(HostRoleStatus.valueOf(commandReport.getStatus()));
        return null;
      }
    }).when(db).updateHostRoleState(anyString(), anyLong(), anyLong(), anyString(), any(CommandReport.class));


    ActionScheduler scheduler = new ActionScheduler(100, 50, db, aq, fsm, 3,
            new HostsMap((String) null), new ServerActionManagerImpl(fsm),
            unitOfWork, conf);

    int cycleCount = 0;
    while (!stages.get(0).getHostRoleStatus(hostname, "AMBARI_SERVER_ACTION")
            .equals(HostRoleStatus.COMPLETED) && cycleCount <= MAX_CYCLE_ITERATIONS) {
      scheduler.doWork();
    }

    assertEquals(stages.get(0).getHostRoleStatus(hostname, "AMBARI_SERVER_ACTION"),
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

    Request request = mock(Request.class);
    when(request.isExclusive()).thenReturn(false);
    when(db.getRequest(anyLong())).thenReturn(request);

    when(db.getStagesInProgress()).thenReturn(stages);

    List<HostRoleCommand> requestTasks = new ArrayList<HostRoleCommand>();
    for (Stage stage : stages) {
      requestTasks.addAll(stage.getOrderedHostRoleCommands());
    }
    when(db.getRequestTasks(anyLong())).thenReturn(requestTasks);
    when(db.getAllStages(anyLong())).thenReturn(stages);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        List<CommandReport> reports = (List<CommandReport>) invocation.getArguments()[0];
        for (CommandReport report : reports) {
          String actionId = report.getActionId();
          long[] requestStageIds = StageUtils.getRequestStage(actionId);
          Long requestId = requestStageIds[0];
          Long stageId = requestStageIds[1];
          String role = report.getRole();
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
    doAnswer(new Answer() {
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

        return null;
      }
    }).when(db).abortOperation(anyLong());

    Properties properties = new Properties();
    Configuration conf = new Configuration(properties);
    ServerActionManagerImpl serverActionManager = new ServerActionManagerImpl(fsm);

    ActionScheduler scheduler =new ActionScheduler(100, 50, db, aq, fsm, 3,
                    new HostsMap((String) null), serverActionManager, unitOfWork, conf);

    ActionManager am = new ActionManager(
            2, 2, aq, fsm, db, new HostsMap((String) null),
            serverActionManager, unitOfWork, requestFactory, conf);

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
    RequestFactory requestFactory = mock(RequestFactory.class);
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

    when(db.getStagesInProgress()).thenReturn(stagesInProgress);

    List<HostRoleCommand> requestTasks = new ArrayList<HostRoleCommand>();
    for (Stage stage : stagesInProgress) {
      requestTasks.addAll(stage.getOrderedHostRoleCommands());
    }
    when(db.getRequestTasks(anyLong())).thenReturn(requestTasks);
    when(db.getAllStages(anyLong())).thenReturn(stagesInProgress);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        List<CommandReport> reports = (List<CommandReport>) invocation.getArguments()[0];
        for (CommandReport report : reports) {
          String actionId = report.getActionId();
          long[] requestStageIds = StageUtils.getRequestStage(actionId);
          Long requestId = requestStageIds[0];
          Long stageId = requestStageIds[1];
          String role = report.getRole();
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
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        startedRequests.put((Long)invocation.getArguments()[0], true);
        return null;
      }
    }).when(db).startRequest(anyLong());

    Request request1 = mock(Request.class);
    when(request1.isExclusive()).thenReturn(false);
    Request request2 = mock(Request.class);
    when(request2.isExclusive()).thenReturn(true);
    Request request3 = mock(Request.class);
    when(request3.isExclusive()).thenReturn(false);

    when(db.getRequest(requestId1)).thenReturn(request1);
    when(db.getRequest(requestId2)).thenReturn(request2);
    when(db.getRequest(requestId3)).thenReturn(request3);

    Properties properties = new Properties();
    Configuration conf = new Configuration(properties);
    ServerActionManagerImpl serverActionManager = new ServerActionManagerImpl(fsm);

    ActionScheduler scheduler =new ActionScheduler(100, 50, db, aq, fsm, 3,
            new HostsMap((String) null), serverActionManager, unitOfWork, conf);

    ActionManager am = new ActionManager(
            2, 2, aq, fsm, db, new HostsMap((String) null),
            serverActionManager, unitOfWork, requestFactory, conf);

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

}
