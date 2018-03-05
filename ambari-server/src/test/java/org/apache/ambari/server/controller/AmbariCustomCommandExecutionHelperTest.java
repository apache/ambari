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
package org.apache.ambari.server.controller;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.ExecutionCommandWrapper;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.actionmanager.Request;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.agent.CommandRepository;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.internal.ComponentResourceProviderTest;
import org.apache.ambari.server.controller.internal.RequestOperationLevel;
import org.apache.ambari.server.controller.internal.RequestResourceFilter;
import org.apache.ambari.server.controller.internal.ServiceResourceProviderTest;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.ServiceComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.RepoDefinitionEntity;
import org.apache.ambari.server.orm.entities.RepoOsEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.UserGroupInfo;
import org.apache.ambari.server.state.ValueAttributesInfo;
import org.apache.ambari.server.state.stack.upgrade.RepositoryVersionHelper;
import org.apache.ambari.server.topology.TopologyManager;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.collections.MapUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockRule;
import org.easymock.Mock;
import org.easymock.MockType;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.Assert;


public class AmbariCustomCommandExecutionHelperTest {
  private static final String REQUEST_CONTEXT_PROPERTY = "context";

  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);

  @Mock(type = MockType.NICE)
  private ActionManager actionManager;

  @Mock(type = MockType.NICE)
  private HostRoleCommand hostRoleCommand;

  @Mock(type = MockType.NICE)
  private ConfigHelper configHelper;

  private Injector injector;
  private Clusters clusters;
  private AmbariManagementController ambariManagementController;
  private Capture<Request> requestCapture = EasyMock.newCapture();
  private static final String OVERRIDDEN_SERVICE_CHECK_TIMEOUT_VALUE = "550";


  @Before
  public void setup() throws Exception {
    EasyMock.reset(actionManager, hostRoleCommand, configHelper);

    InMemoryDefaultTestModule module = new InMemoryDefaultTestModule(){
      @Override
      protected void configure() {
        getProperties().setProperty(Configuration.AGENT_SERVICE_CHECK_TASK_TIMEOUT.getKey(),
          OVERRIDDEN_SERVICE_CHECK_TIMEOUT_VALUE);
        super.configure();
        bind(ActionManager.class).toInstance(actionManager);
        bind(ConfigHelper.class).toInstance(configHelper);
      }
    };

    injector = Guice.createInjector(module);
    injector.getInstance(GuiceJpaInitializer.class);
    ambariManagementController = injector.getInstance(AmbariManagementController.class);
    clusters = injector.getInstance(Clusters.class);

    StageUtils.setTopologyManager(injector.getInstance(TopologyManager.class));
    StageUtils.setConfiguration(injector.getInstance(Configuration.class));

    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());
    createClusterFixture("c1", new StackId("HDP-2.0.6"), "2.0.6-1234", "c1");

    EasyMock.expect(hostRoleCommand.getTaskId()).andReturn(1L);
    EasyMock.expect(hostRoleCommand.getStageId()).andReturn(1L);
    EasyMock.expect(hostRoleCommand.getRoleCommand()).andReturn(RoleCommand.CUSTOM_COMMAND);
    EasyMock.expect(hostRoleCommand.getRole()).andReturn(Role.AMBARI_SERVER_ACTION);
    EasyMock.expect(hostRoleCommand.getStatus()).andReturn(HostRoleStatus.PENDING);

    EasyMock.expect(actionManager.getNextRequestId()).andReturn(1L).anyTimes();
    EasyMock.expect(actionManager.getRequestTasks(1L)).andReturn(Collections.singletonList(hostRoleCommand));

    StackInfo stackInfo = new StackInfo();
    stackInfo.setName("HDP");
    stackInfo.setVersion("2.0.6");
    StackId stackId = new StackId(stackInfo);
    Map<String, DesiredConfig> desiredConfigMap = new HashMap<>();
    Map<PropertyInfo, String> userProperties = new HashMap<>();
    Map<PropertyInfo, String> groupProperties = new HashMap<>();
    PropertyInfo userProperty = new PropertyInfo();
    userProperty.setFilename("zookeeper-env.xml");
    userProperty.setName("zookeeper-user");
    userProperty.setValue("zookeeperUser");
    PropertyInfo groupProperty = new PropertyInfo();
    groupProperty.setFilename("zookeeper-env.xml");
    groupProperty.setName("zookeeper-group");
    groupProperty.setValue("zookeeperGroup");
    ValueAttributesInfo valueAttributesInfo = new ValueAttributesInfo();
    valueAttributesInfo.setType("user");
    Set<UserGroupInfo> userGroupEntries = new HashSet<>();
    UserGroupInfo userGroupInfo = new UserGroupInfo();
    userGroupInfo.setType("zookeeper-env");
    userGroupInfo.setName("zookeeper-group");
    userGroupEntries.add(userGroupInfo);
    valueAttributesInfo.setUserGroupEntries(userGroupEntries);
    userProperty.setPropertyValueAttributes(valueAttributesInfo);
    userProperties.put(userProperty, "zookeeperUser");
    groupProperties.put(groupProperty, "zookeeperGroup");
    Map<String, Set<String>> userGroupsMap = new HashMap<>();
    userGroupsMap.put("zookeeperUser", new HashSet<>(Arrays.asList("zookeeperGroup")));
    Cluster cluster = clusters.getCluster("c1");
    EasyMock.expect(configHelper.getPropertiesWithPropertyType(
      stackId, PropertyInfo.PropertyType.USER, cluster, desiredConfigMap)).andReturn(userProperties).anyTimes();
    EasyMock.expect(configHelper.getPropertiesWithPropertyType(
      stackId, PropertyInfo.PropertyType.GROUP, cluster, desiredConfigMap)).andReturn(groupProperties).anyTimes();
    EasyMock.expect(configHelper.createUserGroupsMap(stackId, cluster, desiredConfigMap)).andReturn(userGroupsMap).anyTimes();

    actionManager.sendActions(EasyMock.capture(requestCapture), EasyMock.anyObject(ExecuteActionRequest.class));
    EasyMock.expectLastCall();

  }

  @After
  public void teardown() throws AmbariException, SQLException {
    SecurityContextHolder.getContext().setAuthentication(null);
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  @Test
  public void testRefreshQueueCustomCommand() throws Exception {
    Map<String, String> requestProperties = new HashMap<String, String>() {
      {
        put(REQUEST_CONTEXT_PROPERTY, "Refresh YARN Capacity Scheduler");
        put("command", "REFRESHQUEUES");
      }
    };

    ExecuteActionRequest actionRequest = new ExecuteActionRequest("c1", "REFRESHQUEUES",
        new HashMap<String, String>() {
          {
            put("forceRefreshConfigTagsBeforeExecution", "true");
          }
        }, false);
    actionRequest.getResourceFilters().add(new RequestResourceFilter("CORE", "YARN", "RESOURCEMANAGER", Collections.singletonList("c1-c6401")));

    EasyMock.replay(hostRoleCommand, actionManager, configHelper);

    createServiceComponentHosts("c1", "CORE", "c1");

    ambariManagementController.createAction(actionRequest, requestProperties);

    Request request = requestCapture.getValue();
    Assert.assertNotNull(request);
    Assert.assertNotNull(request.getStages());
    Assert.assertEquals(1, request.getStages().size());
    Stage stage = request.getStages().iterator().next();

    Assert.assertEquals(1, stage.getHosts().size());

    List<ExecutionCommandWrapper> commands = stage.getExecutionCommands("c1-c6401");
    Assert.assertEquals(1, commands.size());

    ExecutionCommand command = commands.get(0).getExecutionCommand();
    Assert.assertNotNull(command.getHostLevelParams());
    Assert.assertTrue(command.getHostLevelParams().containsKey(ExecutionCommand.KeyNames.USER_GROUPS));
    Assert.assertEquals("{\"zookeeperUser\":[\"zookeeperGroup\"]}", command.getHostLevelParams().get(ExecutionCommand.KeyNames.USER_GROUPS));
    Assert.assertEquals(true, command.getForceRefreshConfigTagsBeforeExecution());
  }

  @Test
  public void testHostsFilterHealthy() throws Exception {

    Map<String, String> requestProperties = new HashMap<String, String>() {
      {
        put("context" , "Restart all components for GANGLIA");
        put("operation_level/level", "SERVICE");
        put("operation_level/service_group_name", "CORE");
        put("operation_level/service_name", "GANGLIA");
        put("operation_level/cluster_name", "c1");
      }
    };

    ExecuteActionRequest actionRequest = new ExecuteActionRequest(
       "c1", "RESTART", null,
       Arrays.asList(
           new RequestResourceFilter("CORE", "GANGLIA", "GANGLIA_SERVER", Collections.singletonList("c1-c6401")),
           new RequestResourceFilter("CORE", "GANGLIA", "GANGLIA_MONITOR", Collections.singletonList("c1-c6401")),
           new RequestResourceFilter("CORE", "GANGLIA", "GANGLIA_MONITOR", Collections.singletonList("c1-c6402"))
       ),
       new RequestOperationLevel(Resource.Type.Service, "c1", "CORE", "GANGLIA", null, null),
      new HashMap<>(), false);

    EasyMock.replay(hostRoleCommand, actionManager, configHelper);

    createServiceComponentHosts("c1", "CORE", "c1");

    ambariManagementController.createAction(actionRequest, requestProperties);

    Request request = requestCapture.getValue();
    Assert.assertNotNull(request);
    Assert.assertNotNull(request.getStages());
    Assert.assertEquals(1, request.getStages().size());
    Stage stage = request.getStages().iterator().next();

     // Check if was generated command, one for each host
    Assert.assertEquals(2, stage.getHostRoleCommands().size());
  }

  @Test
  public void testHostsFilterUnhealthyHost() throws Exception {
    // Set custom status to host
    clusters.getHost("c1-c6402").setState(HostState.HEARTBEAT_LOST);
    Map<String, String> requestProperties = new HashMap<String, String>() {
      {
        put("context", "Restart all components for GANGLIA");
        put("operation_level/level", "SERVICE");
        put("operation_level/service_group_name", "CORE");
        put("operation_level/service_name", "GANGLIA");
        put("operation_level/cluster_name", "c1");
      }
    };

    ExecuteActionRequest actionRequest = new ExecuteActionRequest("c1", "RESTART", null,
        Arrays.asList(
            new RequestResourceFilter("CORE", "GANGLIA", "GANGLIA_SERVER", Collections.singletonList("c1-c6401")),
            new RequestResourceFilter("CORE", "GANGLIA", "GANGLIA_MONITOR", Collections.singletonList("c1-c6401")),
            new RequestResourceFilter("CORE", "GANGLIA", "GANGLIA_MONITOR", Collections.singletonList("c1-c6402"))),
        new RequestOperationLevel(Resource.Type.Service, "c1", "CORE", "GANGLIA", null, null),
      new HashMap<>(), false);

    EasyMock.replay(hostRoleCommand, actionManager, configHelper);

    createServiceComponentHosts("c1", "CORE", "c1");

    ambariManagementController.createAction(actionRequest, requestProperties);

    Request request = requestCapture.getValue();
    Assert.assertNotNull(request);
    Assert.assertNotNull(request.getStages());
    Assert.assertEquals(1, request.getStages().size());
    Stage stage = request.getStages().iterator().next();

    // Check if was generated command for one health host
    Assert.assertEquals(1, stage.getHostRoleCommands().size());
  }

  @Test
  public void testHostsFilterUnhealthyComponent() throws Exception {
    Map<String, String> requestProperties = new HashMap<String, String>() {
      {
        put("context", "Restart all components for GANGLIA");
        put("operation_level/level", "SERVICE");
        put("operation_level/service_group_name", "CORE");
        put("operation_level/service_name", "GANGLIA");
        put("operation_level/cluster_name", "c1");
      }
    };

    ExecuteActionRequest actionRequest = new ExecuteActionRequest("c1", "RESTART", null,
        Arrays.asList(
            new RequestResourceFilter("CORE", "GANGLIA", "GANGLIA_SERVER", Collections.singletonList("c1-c6401")),
            new RequestResourceFilter("CORE", "GANGLIA", "GANGLIA_MONITOR", Collections.singletonList("c1-c6401")),
            new RequestResourceFilter("CORE", "GANGLIA", "GANGLIA_MONITOR", Collections.singletonList("c1-c6402"))),
        new RequestOperationLevel(Resource.Type.Host, "c1", "CORE", "GANGLIA", null, null),
      new HashMap<>(), false);

    EasyMock.replay(hostRoleCommand, actionManager, configHelper);

    createServiceComponentHosts("c1", "CORE", "c1");

    // Set custom status to host
    clusters.getCluster("c1").getService("GANGLIA").getServiceComponent(
        "GANGLIA_MONITOR").getServiceComponentHost("c1-c6402").setState(State.UNKNOWN);

    ambariManagementController.createAction(actionRequest, requestProperties);

    Request request = requestCapture.getValue();
    Assert.assertNotNull(request);
    Assert.assertNotNull(request.getStages());
    Assert.assertEquals(1, request.getStages().size());
    Stage stage = request.getStages().iterator().next();

    // Check if was generated command for one health host
    Assert.assertEquals(1, stage.getHostRoleCommands().size());
  }

  /**
   * Tests that trying to run a service check when there are no available hosts
   * will throw an exception.
   */
  @Test(expected = AmbariException.class)
  public void testNoCandidateHostThrowsException() throws Exception {
    long clusterId = clusters.getCluster("c1").getClusterId();

    // put host into MM
    clusters.getHost("c6402").setMaintenanceState(clusterId, MaintenanceState.ON);

    // ensure that service check is added for ZOOKEEPER
    injector.getInstance(ActionMetadata.class).addServiceCheckAction("ZOOKEEPER");

    Map<String, String> requestProperties = new HashMap<String, String>() {
      {
        put("context", "Service Check ZooKeeper");
        put("operation_level/level", "SERVICE");
        put("operation_level/service_group_name", "CORE");
        put("operation_level/service_name", "ZOOKEEPER");
        put("operation_level/cluster_name", "c1");
      }
    };

    // create the service check on the host in MM
    ExecuteActionRequest actionRequest = new ExecuteActionRequest("c1",
        "ZOOKEEPER_QUORUM_SERVICE_CHECK",
        null, Collections.singletonList(new RequestResourceFilter("CORE", "ZOOKEEPER", "ZOOKEEPER_CLIENT",
        Collections.singletonList("c6402"))),

        new RequestOperationLevel(Resource.Type.Service, "c1", "CORE", "ZOOKEEPER", null, null),
      new HashMap<>(), false);

    EasyMock.replay(hostRoleCommand, actionManager, configHelper);
    ambariManagementController.createAction(actionRequest, requestProperties);
    Assert.fail(
        "Expected an exception since there are no hosts which can run the ZK service check");
  }

  /**
   * Tests that client-only services like TEZ are not run on hosts which are in
   * MM. The client-only service is a special path since a component is
   * typically not specified in the request.
   */
  @Test(expected = AmbariException.class)
  public void testServiceCheckMaintenanceModeWithMissingComponentName() throws Exception {
    long clusterId = clusters.getCluster("c1").getClusterId();

    // put host into MM
    clusters.getHost("c6402").setMaintenanceState(clusterId, MaintenanceState.ON);

    // ensure that service check is added for ZOOKEEPER
    injector.getInstance(ActionMetadata.class).addServiceCheckAction("ZOOKEEPER");

    // !!! use a null operation level to have us guess at the component
    Map<String, String> requestProperties = new HashMap<String, String>() {
      {
        put("context", "Service Check ZooKeeper");
        put("operation_level/level", null);
        put("operation_level/service_name", "ZOOKEEPER");
        put("operation_level/cluster_name", "c1");
      }
    };

    // create the service check on the host in MM, passing in null for the
    // component name
    ExecuteActionRequest actionRequest = new ExecuteActionRequest("c1",
        "ZOOKEEPER_QUORUM_SERVICE_CHECK", null, Collections.singletonList(new RequestResourceFilter("CORE", "ZOOKEEPER", null,
        Collections.singletonList("c6402"))),

        new RequestOperationLevel(Resource.Type.Service, "c1", "CORE", "ZOOKEEPER", null, null),
      new HashMap<>(), false);

    EasyMock.replay(hostRoleCommand, actionManager, configHelper);
    ambariManagementController.createAction(actionRequest, requestProperties);
    Assert.fail("Expected an exception since there are no hosts which can run the ZK service check");
  }

  @Test(expected = AmbariException.class)
  public void testServiceCheckComponentWithEmptyHosts() throws Exception {

    AmbariCustomCommandExecutionHelper ambariCustomCommandExecutionHelper = injector.getInstance(AmbariCustomCommandExecutionHelper.class);

    List<RequestResourceFilter> requestResourceFilter = new ArrayList<RequestResourceFilter>() {{
      add(new RequestResourceFilter("CORE", "FLUME", null, null));
    }};
    ActionExecutionContext actionExecutionContext = new ActionExecutionContext("c1", "SERVICE_CHECK", requestResourceFilter);
    Stage stage = EasyMock.niceMock(Stage.class);

    ambariCustomCommandExecutionHelper.addExecutionCommandsToStage(actionExecutionContext, stage, new HashMap<>(), null);

    Assert.fail("Expected an exception since there are no hosts which can run the Flume service check");
  }

  @Test
  public void testServiceCheckWithOverriddenTimeoutAndHostFiltering() throws Exception {
    AmbariCustomCommandExecutionHelper ambariCustomCommandExecutionHelper = injector.getInstance(AmbariCustomCommandExecutionHelper.class);

    // There are multiple hosts with ZK Client.
    List<RequestResourceFilter> requestResourceFilter = new ArrayList<RequestResourceFilter>() {{
      add(new RequestResourceFilter("CORE", "ZOOKEEPER", "ZOOKEEPER_CLIENT", Arrays.asList(new String[]{"c1-c6401"})));
    }};
    ActionExecutionContext actionExecutionContext = new ActionExecutionContext("c1", "SERVICE_CHECK", requestResourceFilter);
    Stage stage = EasyMock.niceMock(Stage.class);
    ExecutionCommandWrapper execCmdWrapper = EasyMock.niceMock(ExecutionCommandWrapper.class);
    ExecutionCommand execCmd = EasyMock.niceMock(ExecutionCommand.class);
    Capture<Map<String,String>> timeOutCapture = EasyMock.newCapture();

    EasyMock.expect(stage.getClusterName()).andReturn("c1");

    EasyMock.expect(stage.getExecutionCommandWrapper(EasyMock.eq("c1-c6401"), EasyMock.anyString())).andReturn(execCmdWrapper);
    EasyMock.expect(execCmdWrapper.getExecutionCommand()).andReturn(execCmd);
    execCmd.setCommandParams(EasyMock.capture(timeOutCapture));
    EasyMock.expectLastCall();

    HashSet<String> localComponents = new HashSet<>();
    EasyMock.expect(execCmd.getLocalComponents()).andReturn(localComponents).anyTimes();
    EasyMock.replay(configHelper, stage, execCmdWrapper, execCmd);

    createServiceComponentHosts("c1", "CORE", "c1");

    Cluster c1 = clusters.getCluster("c1");
    Service s = c1.getService("ZOOKEEPER");
    ServiceComponent sc = s.getServiceComponent("ZOOKEEPER_CLIENT");
    Assert.assertTrue(sc.getServiceComponentHosts().keySet().size() > 1);

    ambariCustomCommandExecutionHelper.addExecutionCommandsToStage(actionExecutionContext, stage, new HashMap<>(), null);
    Map<String, String> configMap = timeOutCapture.getValues().get(0);
    for (Map.Entry<String, String> config : configMap.entrySet()) {
      if (config.getKey().equals(ExecutionCommand.KeyNames.COMMAND_TIMEOUT)) {
        Assert.assertEquals("Service check timeout should be equal to populated in configs",
          OVERRIDDEN_SERVICE_CHECK_TIMEOUT_VALUE,
          config.getValue());
        return;
      }
    }
    Assert.fail("Expected \"command_timeout\" config not found in execution command configs");
  }

  /**
   * Perform a Service Check for ZOOKEEPER/ZOOKEEPER_CLIENT without specifying a host to run in the request.
   * This should cause Ambari to randomly pick one of the ZOOKEEPER_CLIENT hosts.
   * The current logic first excludes hosts in maintenance mode or that are not healthy (i.e., not heartbeating).
   * From that candidate list, if any hosts have 0 IN-PROGRESS tasks, it randomly picks from that set.
   * Otherwise, it picks from all candidate hosts.
   * @throws Exception
   */
  @Test
  public void testServiceCheckPicksRandomHost() throws Exception {
    AmbariCustomCommandExecutionHelper ambariCustomCommandExecutionHelper = injector.getInstance(AmbariCustomCommandExecutionHelper.class);

    // There are multiple hosts with ZK Client.
    List<RequestResourceFilter> requestResourceFilter = new ArrayList<RequestResourceFilter>() {{
      add(new RequestResourceFilter("CORE", "ZOOKEEPER", "ZOOKEEPER_CLIENT", null));
    }};
    ActionExecutionContext actionExecutionContext = new ActionExecutionContext("c1", "SERVICE_CHECK", requestResourceFilter);
    Stage stage = EasyMock.niceMock(Stage.class);
    ExecutionCommandWrapper execCmdWrapper = EasyMock.niceMock(ExecutionCommandWrapper.class);
    ExecutionCommand execCmd = EasyMock.niceMock(ExecutionCommand.class);

    EasyMock.expect(stage.getClusterName()).andReturn("c1");
    //
    EasyMock.expect(stage.getExecutionCommandWrapper(EasyMock.eq("c1-c6401"), EasyMock.anyString())).andReturn(execCmdWrapper);
    EasyMock.expect(stage.getExecutionCommandWrapper(EasyMock.eq("c1-c6402"), EasyMock.anyString())).andReturn(execCmdWrapper);
    EasyMock.expect(execCmdWrapper.getExecutionCommand()).andReturn(execCmd);
    EasyMock.expect(execCmd.getForceRefreshConfigTagsBeforeExecution()).andReturn(true);

    HashSet<String> localComponents = new HashSet<>();
    EasyMock.expect(execCmd.getLocalComponents()).andReturn(localComponents).anyTimes();
    EasyMock.replay(configHelper, stage, execCmdWrapper, execCmd);

    createServiceComponentHosts("c1", "CORE", "c1");

    Cluster c1 = clusters.getCluster("c1");
    Service s = c1.getService("ZOOKEEPER");
    ServiceComponent sc = s.getServiceComponent("ZOOKEEPER_CLIENT");
    Assert.assertTrue(sc.getServiceComponentHosts().keySet().size() > 1);

    ambariCustomCommandExecutionHelper.addExecutionCommandsToStage(actionExecutionContext, stage, new HashMap<>(), null);
  }



  /**
   * Perform a Service Check for HDFS on the HADOOP_CLIENTS/SOME_CLIENT_FOR_SERVICE_CHECK service component.
   * The HADOOP_CLIENTS service is the service on which HDFS depends.
   * The SOME_CLIENT_FOR_SERVICE_CHECK component is defined in HDFS's metainfo file.
   * This should cause Ambari to execute service check on dependent service client component.
   *
   * Also assures that service check doesn't works without dependency defined
   * @throws Exception
   */
  @Test
  public void testServiceCheckRunsOnDependentClientService() throws Exception {
    AmbariCustomCommandExecutionHelper ambariCustomCommandExecutionHelper = injector.getInstance(AmbariCustomCommandExecutionHelper.class);

    List<RequestResourceFilter> requestResourceFilter = new ArrayList<RequestResourceFilter>() {{
      add(new RequestResourceFilter("CORE", "HDFS", null, null));
    }};
    ActionExecutionContext actionExecutionContext = new ActionExecutionContext("c1", "SERVICE_CHECK", requestResourceFilter);
    Stage stage = EasyMock.niceMock(Stage.class);
    ExecutionCommandWrapper execCmdWrapper = EasyMock.niceMock(ExecutionCommandWrapper.class);
    ExecutionCommand execCmd = EasyMock.niceMock(ExecutionCommand.class);

    EasyMock.expect(stage.getClusterName()).andReturn("c1");
    //
    EasyMock.expect(stage.getExecutionCommandWrapper(EasyMock.eq("c1-c6403"), EasyMock.anyString())).andReturn(execCmdWrapper);
    EasyMock.expect(execCmdWrapper.getExecutionCommand()).andReturn(execCmd);
    EasyMock.expect(execCmd.getForceRefreshConfigTagsBeforeExecution()).andReturn(true);

    HashSet<String> localComponents = new HashSet<>();
    EasyMock.expect(execCmd.getLocalComponents()).andReturn(localComponents).anyTimes();
    EasyMock.replay(configHelper, stage, execCmdWrapper, execCmd);

    createServiceComponentHosts("c1", "CORE", "c1");

    //add host with client only
    addHost("c1-c6403", "c1");

    //create client service
    OrmTestHelper ormTestHelper = injector.getInstance(OrmTestHelper.class);
    RepositoryVersionEntity repositoryVersion = ormTestHelper.getOrCreateRepositoryVersion(new StackId("HDP-2.0.6"), "2.0.6-1234");
    createService("c1", "CORE", "HADOOP_CLIENTS", repositoryVersion);
    createServiceComponent("c1", "CORE", "HADOOP_CLIENTS", "SOME_CLIENT_FOR_SERVICE_CHECK", "SOME_CLIENT_FOR_SERVICE_CHECK", State.INIT);
    createServiceComponentHost("c1", "CORE", "HADOOP_CLIENTS", 1L, "SOME_CLIENT_FOR_SERVICE_CHECK", "SOME_CLIENT_FOR_SERVICE_CHECK", "c1-c6403", State.INIT);

    //make sure there are no HDFS_CLIENT components from HDFS service
    Cluster c1 = clusters.getCluster("c1");
    Service s = c1.getService("HDFS");
    try {
      ServiceComponent sc = s.getServiceComponent("HDFS_CLIENT");
      Assert.assertEquals(0, sc.getServiceComponentHosts().keySet().size());
    } catch (AmbariException e) {
      //ignore
    }

    //make sure the SOME_CLIENT_FOR_SERVICE_CHECK component exists on some hosts
    Service clientService = c1.getService("HADOOP_CLIENTS");
    ServiceComponent clientServiceComponent = clientService.getServiceComponent("SOME_CLIENT_FOR_SERVICE_CHECK");
    Assert.assertEquals(1, clientServiceComponent.getServiceComponentHosts().keySet().size());

    //Check if service check works without dependency defined
    try {
      ambariCustomCommandExecutionHelper.addExecutionCommandsToStage(actionExecutionContext, stage, new HashMap<>(), null);
      Assert.fail("Previous method call should have thrown the exception");
    } catch (AmbariException e) {
      Assert.assertTrue(e.getMessage().contains("Couldn't find any client components SOME_CLIENT_FOR_SERVICE_CHECK in the dependent services"));
    }

    //add dependency from HDFS to HADOOP_CLIENTS
    c1.addDependencyToService("CORE", "HDFS", clientService.getServiceId());

    ambariCustomCommandExecutionHelper.addExecutionCommandsToStage(actionExecutionContext, stage, new HashMap<>(), null);
  }

  @Test
  public void testIsTopologyRefreshRequired() throws Exception {
    AmbariCustomCommandExecutionHelper helper = injector.getInstance(AmbariCustomCommandExecutionHelper.class);

    createClusterFixture("c2", new StackId("HDP-2.1.1"), "2.1.1.0-1234", "c2");

    Assert.assertTrue(helper.isTopologyRefreshRequired("START", "c2", "CORE", "HDFS"));
    Assert.assertTrue(helper.isTopologyRefreshRequired("RESTART", "c2", "CORE", "HDFS"));
    Assert.assertFalse(helper.isTopologyRefreshRequired("STOP", "c2", "CORE", "HDFS"));
  }

  @Test
  public void testAvailableServicesMapContainsVersions() throws Exception {

    Map<String, String> requestProperties = new HashMap<String, String>() {
      {
        put(REQUEST_CONTEXT_PROPERTY, "Refresh YARN Capacity Scheduler");
        put("command", "REFRESHQUEUES");
      }
    };
    ExecuteActionRequest actionRequest = new ExecuteActionRequest("c1", "REFRESHQUEUES",
            new HashMap<String, String>() {
              {
                put("forceRefreshConfigTags", "capacity-scheduler");
              }
            }, false);
    actionRequest.getResourceFilters().add(new RequestResourceFilter("CORE", "YARN", "RESOURCEMANAGER", Collections.singletonList("c1-c6401")));
    EasyMock.replay(hostRoleCommand, actionManager, configHelper);

    createServiceComponentHosts("c1", "CORE", "c1");

    ambariManagementController.createAction(actionRequest, requestProperties);

    Request request = requestCapture.getValue();
    Stage stage = request.getStages().iterator().next();
    List<ExecutionCommandWrapper> commands = stage.getExecutionCommands("c1-c6401");
    ExecutionCommand command = commands.get(0).getExecutionCommand();

    // ZK is the only service that is versionable
    Assert.assertFalse(MapUtils.isEmpty(command.getComponentVersionMap()));
    Assert.assertEquals(1, command.getComponentVersionMap().size());
    Assert.assertTrue(command.getComponentVersionMap().containsKey("ZOOKEEPER"));
  }

  /**
   * Tests that if a component's repository is not resolved, then the repo
   * version map does not get populated.
   *
   * @throws Exception
   */
  @Test
  public void testAvailableServicesMapIsEmptyWhenRepositoriesNotResolved() throws Exception {

    // set all repos to resolve=false to verify that we don't get a
    // component version map
    RepositoryVersionDAO repositoryVersionDAO = injector.getInstance(RepositoryVersionDAO.class);
    List<RepositoryVersionEntity> repoVersions = repositoryVersionDAO.findAll();
    for (RepositoryVersionEntity repoVersion : repoVersions) {
      repoVersion.setResolved(false);
      repositoryVersionDAO.merge(repoVersion);
    }

    Map<String, String> requestProperties = new HashMap<String, String>() {
      {
        put(REQUEST_CONTEXT_PROPERTY, "Refresh YARN Capacity Scheduler");
        put("command", "REFRESHQUEUES");
      }
    };

    ExecuteActionRequest actionRequest = new ExecuteActionRequest("c1", "REFRESHQUEUES",
        new HashMap<String, String>() {
          {
            put("forceRefreshConfigTags", "capacity-scheduler");
          }
        }, false);

    actionRequest.getResourceFilters().add(new RequestResourceFilter("CORE", "YARN", "RESOURCEMANAGER",
        Collections.singletonList("c1-c6401")));

    EasyMock.replay(hostRoleCommand, actionManager, configHelper);

    createServiceComponentHosts("c1", "CORE", "c1");

    ambariManagementController.createAction(actionRequest, requestProperties);
    Request request = requestCapture.getValue();
    Stage stage = request.getStages().iterator().next();
    List<ExecutionCommandWrapper> commands = stage.getExecutionCommands("c1-c6401");
    ExecutionCommand command = commands.get(0).getExecutionCommand();

    Assert.assertTrue(MapUtils.isEmpty(command.getComponentVersionMap()));
  }

  @Test
  public void testCommandRepository() throws Exception {
    Cluster cluster = clusters.getCluster("c1");
    Service serviceYARN = cluster.getService("YARN");
    Service serviceZK = cluster.getService("ZOOKEEPER");
    ServiceComponent componentRM = serviceYARN.getServiceComponent("RESOURCEMANAGER");
    ServiceComponent componentZKC = serviceZK.getServiceComponent("ZOOKEEPER_CLIENT");
    Host host = clusters.getHost("c1-c6401");

    AmbariCustomCommandExecutionHelper helper = injector.getInstance(AmbariCustomCommandExecutionHelper.class);
    RepositoryVersionHelper repoHelper = injector.getInstance(RepositoryVersionHelper.class);
    StackDAO stackDAO = injector.getInstance(StackDAO.class);
    RepositoryVersionDAO repoVersionDAO = injector.getInstance(RepositoryVersionDAO.class);
    ServiceComponentDesiredStateDAO componentDAO = injector.getInstance(ServiceComponentDesiredStateDAO.class);
    RepositoryVersionHelper repoVersionHelper = injector.getInstance(RepositoryVersionHelper.class);

    CommandRepository commandRepo = repoHelper.getCommandRepository(cluster, componentRM, host);
    Assert.assertEquals(2, commandRepo.getRepositories().size());


    List<RepoOsEntity> operatingSystems = new ArrayList<>();
    RepoDefinitionEntity repoDefinitionEntity1 = new RepoDefinitionEntity();
    repoDefinitionEntity1.setRepoID("new-id");
    repoDefinitionEntity1.setBaseUrl("http://foo");
    repoDefinitionEntity1.setRepoName("HDP");
    RepoOsEntity repoOsEntity = new RepoOsEntity();
    repoOsEntity.setFamily("redhat6");
    repoOsEntity.setAmbariManaged(true);
    repoOsEntity.addRepoDefinition(repoDefinitionEntity1);
    operatingSystems.add(repoOsEntity);

    StackEntity stackEntity = stackDAO.find(cluster.getDesiredStackVersion().getStackName(),
        cluster.getDesiredStackVersion().getStackVersion());

    RepositoryVersionEntity repositoryVersion = new RepositoryVersionEntity(stackEntity,
        "2.1.1.1-1234", "2.1.1.1-1234", operatingSystems);
    repositoryVersion = repoVersionDAO.merge(repositoryVersion);

    // add a repo version associated with a component
    ServiceComponentDesiredStateEntity componentEntity = componentDAO.findByName(cluster.getClusterId(), serviceYARN.getServiceGroupId(),
        serviceYARN.getServiceId(), componentRM.getName(), componentRM.getType());

    componentEntity.setDesiredRepositoryVersion(repositoryVersion);
    componentDAO.merge(componentEntity);

    // !!! make sure the override is set
    commandRepo = repoHelper.getCommandRepository(cluster, componentRM, host);

    Assert.assertEquals(1, commandRepo.getRepositories().size());
    CommandRepository.Repository repo = commandRepo.getRepositories().iterator().next();
    Assert.assertEquals("http://foo", repo.getBaseUrl());

    // verify that ZK has no repositories, since we haven't defined a repo version for ZKC
    commandRepo = repoHelper.getCommandRepository(cluster, componentZKC, host);
    Assert.assertEquals(2, commandRepo.getRepositories().size());
  }

  private void createClusterFixture(String clusterName, StackId stackId,
    String respositoryVersion, String hostPrefix) throws AmbariException, AuthorizationException {

    String hostC6401 = hostPrefix + "-c6401";
    String hostC6402 = hostPrefix + "-c6402";

    OrmTestHelper ormTestHelper = injector.getInstance(OrmTestHelper.class);
    RepositoryVersionEntity repositoryVersion = ormTestHelper.getOrCreateRepositoryVersion(stackId,
        respositoryVersion);

    createCluster(clusterName, stackId.getStackId());

    addHost(hostC6401, clusterName);
    addHost(hostC6402, clusterName);

    Cluster cluster = clusters.getCluster(clusterName);
    Assert.assertNotNull(cluster);

    String serviceGroupName = "CORE";
    cluster.addServiceGroup(serviceGroupName, stackId.getStackId());

    createService(clusterName, serviceGroupName, "HDFS", repositoryVersion);
    createService(clusterName, serviceGroupName, "YARN", repositoryVersion);
    createService(clusterName, serviceGroupName, "GANGLIA", repositoryVersion);
    createService(clusterName, serviceGroupName, "ZOOKEEPER", repositoryVersion);
    createService(clusterName, serviceGroupName, "FLUME", repositoryVersion);

    createServiceComponent(clusterName, serviceGroupName, "YARN", "RESOURCEMANAGER", "RESOURCEMANAGER", State.INIT);
    createServiceComponent(clusterName, serviceGroupName, "YARN", "NODEMANAGER", "NODEMANAGER", State.INIT);
    createServiceComponent(clusterName, serviceGroupName, "GANGLIA", "GANGLIA_SERVER", "GANGLIA_SERVER", State.INIT);
    createServiceComponent(clusterName, serviceGroupName, "GANGLIA", "GANGLIA_MONITOR", "GANGLIA_MONITOR", State.INIT);
    createServiceComponent(clusterName, serviceGroupName, "ZOOKEEPER", "ZOOKEEPER_CLIENT", "ZOOKEEPER_CLIENT", State.INIT);

    // this component should be not installed on any host
    createServiceComponent(clusterName, serviceGroupName, "FLUME", "FLUME_HANDLER", "FLUME_HANDLER", State.INIT);
  }


  private void createServiceComponentHosts(String clusterName, String serviceGroupName, String hostPrefix) throws AmbariException, AuthorizationException {
    String hostC6401 = hostPrefix + "-c6401";
    String hostC6402 = hostPrefix + "-c6402";
    // TODO : Numbers for component Id may not be correct.
    createServiceComponentHost(clusterName, serviceGroupName, "YARN", 1L, "RESOURCEMANAGER", "RESOURCEMANAGER", hostC6401, null);
    createServiceComponentHost(clusterName, serviceGroupName, "YARN", 2L, "NODEMANAGER", "NODEMANAGER", hostC6401, null);
    createServiceComponentHost(clusterName, serviceGroupName, "GANGLIA", 3L, "GANGLIA_SERVER", "GANGLIA_SERVER", hostC6401, State.INIT);
    createServiceComponentHost(clusterName, serviceGroupName, "GANGLIA", 4L, "GANGLIA_MONITOR", "GANGLIA_MONITOR", hostC6401, State.INIT);
    createServiceComponentHost(clusterName, serviceGroupName, "ZOOKEEPER", 5L, "ZOOKEEPER_CLIENT", "ZOOKEEPER_CLIENT", hostC6401, State.INIT);

    createServiceComponentHost(clusterName, serviceGroupName, "YARN", 6L,"NODEMANAGER", "NODEMANAGER", hostC6402, null);
    createServiceComponentHost(clusterName, serviceGroupName, "GANGLIA", 7L,"GANGLIA_MONITOR", "GANGLIA_MONITOR", hostC6402, State.INIT);
    createServiceComponentHost(clusterName, serviceGroupName, "ZOOKEEPER", 8L, "ZOOKEEPER_CLIENT", "ZOOKEEPER_CLIENT", hostC6402, State.INIT);
  }
  private void addHost(String hostname, String clusterName) throws AmbariException {
    clusters.addHost(hostname);
    setOsFamily(clusters.getHost(hostname), "redhat", "6.3");
    clusters.getHost(hostname).setState(HostState.HEALTHY);
    if (null != clusterName) {
      clusters.mapHostToCluster(hostname, clusterName);
    }
  }
  private void setOsFamily(Host host, String osFamily, String osVersion) {
    Map<String, String> hostAttributes = new HashMap<>();
    hostAttributes.put("os_family", osFamily);
    hostAttributes.put("os_release_version", osVersion);

    host.setHostAttributes(hostAttributes);
  }

  private void createCluster(String clusterName, String stackVersion) throws AmbariException, AuthorizationException {
    ClusterRequest r = new ClusterRequest(null, clusterName, State.INSTALLED.name(),
        SecurityType.NONE, stackVersion, null);
    ambariManagementController.createCluster(r);
  }

  private void createService(
    String clusterName, String serviceGroupName, String serviceName, RepositoryVersionEntity repositoryVersion
  ) throws AmbariException, AuthorizationException {
    ServiceRequest request = new ServiceRequest(clusterName, serviceGroupName, serviceName, serviceName, repositoryVersion.getId(), null, null, null);
    ServiceResourceProviderTest.createServices(ambariManagementController,
        injector.getInstance(RepositoryVersionDAO.class), Collections.singleton(request));
  }

  private void createServiceComponent(
    String clusterName, String serviceGroupName, String serviceName, String componentName, String componentType, State desiredState
  ) throws AmbariException, AuthorizationException {
    ServiceComponentRequest r = new ServiceComponentRequest(clusterName, serviceGroupName, serviceName, componentName, componentType, desiredState != null ? desiredState.name() : null);
    ComponentResourceProviderTest.createComponents(ambariManagementController, Collections.singleton(r));
  }

  private void createServiceComponentHost(
    String clusterName, String serviceGroupName, String serviceName, Long componentId, String componentName, String componentType, String hostname, State desiredState
  ) throws AmbariException, AuthorizationException {
    ServiceComponentHostRequest r = new ServiceComponentHostRequest(clusterName, serviceGroupName, serviceName, componentId, componentName, componentType,
            hostname, desiredState != null ? desiredState.name() : null);
    ambariManagementController.createHostComponents(Collections.singleton(r));
  }

}
