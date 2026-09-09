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

package org.apache.ambari.server.controller.internal;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.Role;
import org.apache.ambari.server.actionmanager.ExecutionCommandWrapperFactory;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.ExecutionCommandDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.StageEntity;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.topology.LogicalRequest;
import org.apache.ambari.server.topology.TopologyManager;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * TaskResourceProvider tests.
 */
public class TaskResourceProviderTest {
  @After
  public void clearAuthentication() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void testCreateResources() throws Exception {
    Resource.Type type = Resource.Type.Task;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    // replay
    replay(managementController, response);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<>();

    Map<String, Object> properties = new LinkedHashMap<>();

    // add properties to the request map
    properties.put(TaskResourceProvider.TASK_REQUEST_ID_PROPERTY_ID, 100);
    properties.put(TaskResourceProvider.TASK_ID_PROPERTY_ID, 100);

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    try {
      provider.createResources(request);
      Assert.fail("Expected an UnsupportedOperationException");
    } catch (UnsupportedOperationException e) {
      // expected
    }

    // verify
    verify(managementController, response);
  }

  @Test
  public void testGetResources() throws Exception {
    Resource.Type type = Resource.Type.Task;

    AmbariManagementController amc = createMock(AmbariManagementController.class);
    HostRoleCommandDAO hostRoleCommandDAO = createMock(HostRoleCommandDAO.class);
    Clusters clusters = createMock(Clusters.class);
    Cluster cluster = createMock(Cluster.class);

    Injector m_injector = Guice.createInjector(new InMemoryDefaultTestModule());
    TaskResourceProvider provider = (TaskResourceProvider) AbstractControllerResourceProvider.getResourceProvider(
        type, amc);

    m_injector.injectMembers(provider);
    TaskResourceProvider.s_dao = hostRoleCommandDAO;

    List<HostRoleCommandEntity> entities = new ArrayList<>();
    HostRoleCommandEntity hostRoleCommandEntity = new HostRoleCommandEntity();
    hostRoleCommandEntity.setRequestId(100L);
    hostRoleCommandEntity.setTaskId(100L);
    hostRoleCommandEntity.setStageId(100L);
    hostRoleCommandEntity.setRole(Role.DATANODE);
    hostRoleCommandEntity.setCustomCommandName("customCommandName");
    hostRoleCommandEntity.setCommandDetail("commandDetail");
    hostRoleCommandEntity.setOpsDisplayName("opsDisplayName");
    StageEntity stageEntity = new StageEntity();
    stageEntity.setClusterId(1L);
    stageEntity.setRequestId(100L);
    stageEntity.setStageId(100L);
    hostRoleCommandEntity.setStage(stageEntity);
    entities.add(hostRoleCommandEntity);

    // set expectations
    expect(hostRoleCommandDAO.findAll(EasyMock.anyObject(Request.class),
        EasyMock.anyObject(Predicate.class))).andReturn(entities).once();
    expect(amc.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getClusterById(1L)).andReturn(cluster).once();
    expect(cluster.getResourceId()).andReturn(11L).once();
    expect(cluster.getClusterName()).andReturn("c1").once();
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());

    // replay
    replay(amc, hostRoleCommandDAO, clusters, cluster);

    Set<String> propertyIds = new HashSet<>();

    propertyIds.add(TaskResourceProvider.TASK_ID_PROPERTY_ID);
    propertyIds.add(TaskResourceProvider.TASK_REQUEST_ID_PROPERTY_ID);
    propertyIds.add(TaskResourceProvider.TASK_COMMAND_DET_PROPERTY_ID);
    propertyIds.add(TaskResourceProvider.TASK_COMMAND_OPS_DISPLAY_NAME);

    Predicate predicate = new PredicateBuilder().property(TaskResourceProvider.TASK_ID_PROPERTY_ID).equals("100").
                          and().property(TaskResourceProvider.TASK_REQUEST_ID_PROPERTY_ID).equals("100").toPredicate();
    Request request = PropertyHelper.getReadRequest(propertyIds);
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      long taskId = (Long) resource.getPropertyValue(TaskResourceProvider.TASK_ID_PROPERTY_ID);
      Assert.assertEquals(100L, taskId);
      Assert.assertEquals(null, resource.getPropertyValue(TaskResourceProvider
          .TASK_CUST_CMD_NAME_PROPERTY_ID));
      Assert.assertEquals("commandDetail", resource.getPropertyValue(TaskResourceProvider
          .TASK_COMMAND_DET_PROPERTY_ID));
      Assert.assertEquals("opsDisplayName",resource.getPropertyValue(TaskResourceProvider
          .TASK_COMMAND_OPS_DISPLAY_NAME));
    }

    // verify
    verify(amc, hostRoleCommandDAO, clusters, cluster);
  }

  @Test
  public void testGetResourcesForTopology() throws Exception {
    Resource.Type type = Resource.Type.Task;

    AmbariManagementController amc = createMock(AmbariManagementController.class);
    HostRoleCommandDAO hostRoleCommandDAO = createMock(HostRoleCommandDAO.class);
    TopologyManager topologyManager = createMock(TopologyManager.class);
    HostDAO hostDAO = createMock(HostDAO.class);
    ExecutionCommandDAO executionCommandDAO = createMock(ExecutionCommandDAO.class);
    ExecutionCommandWrapperFactory ecwFactory = createMock(ExecutionCommandWrapperFactory.class);
    Clusters clusters = createMock(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    LogicalRequest logicalRequest = createMock(LogicalRequest.class);

    Injector m_injector = Guice.createInjector(new InMemoryDefaultTestModule());
    TaskResourceProvider provider = (TaskResourceProvider) AbstractControllerResourceProvider.getResourceProvider(
      type, amc);

    m_injector.injectMembers(provider);
    TaskResourceProvider.s_dao = hostRoleCommandDAO;
    TaskResourceProvider.s_topologyManager = topologyManager;

    List<HostRoleCommandEntity> entities = new ArrayList<>();

    List<HostRoleCommand> commands = new ArrayList<>();
    HostRoleCommandEntity hostRoleCommandEntity = new HostRoleCommandEntity();
    hostRoleCommandEntity.setRequestId(100L);
    hostRoleCommandEntity.setTaskId(100L);
    hostRoleCommandEntity.setStageId(100L);
    hostRoleCommandEntity.setRole(Role.DATANODE);
    hostRoleCommandEntity.setCustomCommandName("customCommandName");
    hostRoleCommandEntity.setCommandDetail("commandDetail");
    hostRoleCommandEntity.setOpsDisplayName("opsDisplayName");
    StageEntity stageEntity = new StageEntity();
    stageEntity.setClusterId(1L);
    stageEntity.setRequestId(100L);
    stageEntity.setStageId(100L);
    hostRoleCommandEntity.setStage(stageEntity);
    commands.add(new HostRoleCommand(hostRoleCommandEntity, hostDAO, executionCommandDAO, ecwFactory));
    HostRoleCommandEntity siblingEntity = new HostRoleCommandEntity();
    siblingEntity.setRequestId(100L);
    siblingEntity.setTaskId(101L);
    siblingEntity.setStageId(101L);
    siblingEntity.setRole(Role.DATANODE);
    siblingEntity.setStage(stageEntity);
    commands.add(new HostRoleCommand(siblingEntity, hostDAO, executionCommandDAO, ecwFactory));

    // set expectations
    expect(hostRoleCommandDAO.findAll(EasyMock.anyObject(Request.class),
      EasyMock.anyObject(Predicate.class))).andReturn(entities).once();
    expect(topologyManager.getRequests(EasyMock.<Collection<Long>>anyObject()))
        .andReturn(List.of(logicalRequest)).once();
    expect(logicalRequest.getClusterId()).andReturn(1L).once();
    expect(logicalRequest.getRequestId()).andReturn(100L).anyTimes();
    expect(logicalRequest.getCommands()).andReturn(commands).once();
    expect(amc.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getClusterById(1L)).andReturn(cluster).once();
    expect(cluster.getResourceId()).andReturn(11L).once();
    expect(cluster.getClusterName()).andReturn("c1").times(2);
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());

    // replay
    replay(amc, hostRoleCommandDAO, topologyManager, clusters, cluster, logicalRequest);

    Set<String> propertyIds = new HashSet<>();

    propertyIds.add(TaskResourceProvider.TASK_ID_PROPERTY_ID);
    propertyIds.add(TaskResourceProvider.TASK_REQUEST_ID_PROPERTY_ID);
    propertyIds.add(TaskResourceProvider.TASK_COMMAND_DET_PROPERTY_ID);
    propertyIds.add(TaskResourceProvider.TASK_COMMAND_OPS_DISPLAY_NAME);

    Predicate predicate = new PredicateBuilder().property(TaskResourceProvider.TASK_ID_PROPERTY_ID).equals("100").
      and().property(TaskResourceProvider.TASK_REQUEST_ID_PROPERTY_ID).equals("100").toPredicate();
    Request request = PropertyHelper.getReadRequest(propertyIds);

    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      long taskId = (Long) resource.getPropertyValue(TaskResourceProvider.TASK_ID_PROPERTY_ID);
      Assert.assertEquals(100L, taskId);
      Assert.assertEquals(null, resource.getPropertyValue(TaskResourceProvider
        .TASK_CUST_CMD_NAME_PROPERTY_ID));
      Assert.assertEquals("commandDetail", resource.getPropertyValue(TaskResourceProvider
        .TASK_COMMAND_DET_PROPERTY_ID));
      Assert.assertEquals("opsDisplayName",resource.getPropertyValue(TaskResourceProvider
          .TASK_COMMAND_OPS_DISPLAY_NAME));
    }

    // verify
    verify(amc, hostRoleCommandDAO, topologyManager, clusters, cluster, logicalRequest);
  }

  @Test(expected = AuthorizationException.class)
  public void testTaskRouteCannotSelectTaskFromAnotherCluster() throws Exception {
    AmbariManagementController amc = createMock(AmbariManagementController.class);
    HostRoleCommandDAO hostRoleCommandDAO = createMock(HostRoleCommandDAO.class);
    Clusters clusters = createMock(Clusters.class);
    Cluster clusterA = createMock(Cluster.class);
    Cluster clusterB = createMock(Cluster.class);

    Injector injector = Guice.createInjector(new InMemoryDefaultTestModule());
    TaskResourceProvider provider = (TaskResourceProvider)
        AbstractControllerResourceProvider.getResourceProvider(Resource.Type.Task, amc);
    injector.injectMembers(provider);
    TaskResourceProvider.s_dao = hostRoleCommandDAO;

    StageEntity stageB = new StageEntity();
    stageB.setClusterId(2L);
    stageB.setRequestId(100L);
    stageB.setStageId(1L);
    HostRoleCommandEntity taskB = new HostRoleCommandEntity();
    taskB.setRequestId(100L);
    taskB.setTaskId(100L);
    taskB.setStageId(1L);
    taskB.setStage(stageB);

    Predicate predicate = new PredicateBuilder()
        .property(TaskResourceProvider.TASK_CLUSTER_NAME_PROPERTY_ID).equals("cluster-a")
        .and().property(TaskResourceProvider.TASK_REQUEST_ID_PROPERTY_ID).equals("100")
        .and().property(TaskResourceProvider.TASK_ID_PROPERTY_ID).equals("100").toPredicate();
    Request request = PropertyHelper.getReadRequest(Set.of(TaskResourceProvider.TASK_ID_PROPERTY_ID));
    expect(hostRoleCommandDAO.findAll(request, predicate)).andReturn(List.of(taskB));
    expect(amc.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getClusterById(2L)).andReturn(clusterB).once();
    expect(clusterB.getResourceId()).andReturn(22L).once();
    SecurityContextHolder.getContext().setAuthentication(
        TestAuthenticationFactory.createClusterAdministrator("alice", 11L));
    replay(amc, hostRoleCommandDAO, clusters, clusterA, clusterB);

    provider.getResources(request, predicate);
  }

  @Test(expected = AuthorizationException.class)
  public void testTaskMustBelongToSelectedRequest() throws Exception {
    AmbariManagementController amc = createMock(AmbariManagementController.class);
    HostRoleCommandDAO hostRoleCommandDAO = createMock(HostRoleCommandDAO.class);
    Injector injector = Guice.createInjector(new InMemoryDefaultTestModule());
    TaskResourceProvider provider = (TaskResourceProvider)
        AbstractControllerResourceProvider.getResourceProvider(Resource.Type.Task, amc);
    injector.injectMembers(provider);
    TaskResourceProvider.s_dao = hostRoleCommandDAO;

    StageEntity stage = new StageEntity();
    stage.setClusterId(1L);
    stage.setRequestId(200L);
    stage.setStageId(1L);
    HostRoleCommandEntity task = new HostRoleCommandEntity();
    task.setRequestId(200L);
    task.setStageId(1L);
    task.setTaskId(9L);
    task.setStage(stage);
    Predicate predicate = new PredicateBuilder()
        .property(TaskResourceProvider.TASK_REQUEST_ID_PROPERTY_ID).equals("100")
        .and().property(TaskResourceProvider.TASK_ID_PROPERTY_ID).equals("9").toPredicate();
    Request request = PropertyHelper.getReadRequest(Set.of(TaskResourceProvider.TASK_ID_PROPERTY_ID));
    expect(hostRoleCommandDAO.findAll(request, predicate)).andReturn(List.of(task));
    replay(amc, hostRoleCommandDAO);

    provider.getResources(request, predicate);
  }

  @Test
  public void testClusterTaskCollectionFiltersOtherClusters() throws Exception {
    AmbariManagementController amc = createMock(AmbariManagementController.class);
    HostRoleCommandDAO hostRoleCommandDAO = createMock(HostRoleCommandDAO.class);
    TopologyManager topologyManager = createMock(TopologyManager.class);
    Clusters clusters = createMock(Clusters.class);
    Cluster clusterA = createMock(Cluster.class);
    Cluster clusterB = createMock(Cluster.class);

    Injector injector = Guice.createInjector(new InMemoryDefaultTestModule());
    TaskResourceProvider provider = (TaskResourceProvider)
        AbstractControllerResourceProvider.getResourceProvider(Resource.Type.Task, amc);
    injector.injectMembers(provider);
    TaskResourceProvider.s_dao = hostRoleCommandDAO;
    TaskResourceProvider.s_topologyManager = topologyManager;

    HostRoleCommandEntity taskA = createTask(1L, 100L, 1L, 10L);
    HostRoleCommandEntity taskB = createTask(2L, 200L, 1L, 20L);
    Predicate predicate = new PredicateBuilder()
        .property(TaskResourceProvider.TASK_CLUSTER_NAME_PROPERTY_ID).equals("cluster-a")
        .toPredicate();
    Request request = PropertyHelper.getReadRequest(Set.of(
        TaskResourceProvider.TASK_CLUSTER_NAME_PROPERTY_ID,
        TaskResourceProvider.TASK_ID_PROPERTY_ID));

    expect(hostRoleCommandDAO.findAll(request, predicate)).andReturn(List.of(taskA, taskB));
    expect(topologyManager.getRequests(EasyMock.<Collection<Long>>anyObject()))
        .andReturn(List.of());
    expect(amc.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getClusterById(1L)).andReturn(clusterA).once();
    expect(clusters.getClusterById(2L)).andReturn(clusterB).once();
    expect(clusterA.getResourceId()).andReturn(11L).anyTimes();
    expect(clusterA.getClusterName()).andReturn("cluster-a").once();
    expect(clusterB.getResourceId()).andReturn(22L).once();
    SecurityContextHolder.getContext().setAuthentication(
        TestAuthenticationFactory.createClusterAdministrator("alice", 11L));
    replay(amc, hostRoleCommandDAO, topologyManager, clusters, clusterA, clusterB);

    Set<Resource> resources = provider.getResources(request, predicate);

    assertEquals(1, resources.size());
    Resource resource = resources.iterator().next();
    assertEquals(10L, resource.getPropertyValue(TaskResourceProvider.TASK_ID_PROPERTY_ID));
    assertEquals("cluster-a",
        resource.getPropertyValue(TaskResourceProvider.TASK_CLUSTER_NAME_PROPERTY_ID));
    verify(amc, hostRoleCommandDAO, topologyManager, clusters, clusterA, clusterB);
  }

  @Test
  public void testTaskQuerySupportsOrRequestParents() throws Exception {
    AmbariManagementController amc = createMock(AmbariManagementController.class);
    HostRoleCommandDAO hostRoleCommandDAO = createMock(HostRoleCommandDAO.class);
    Clusters clusters = createMock(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    Injector injector = Guice.createInjector(new InMemoryDefaultTestModule());
    TaskResourceProvider provider = (TaskResourceProvider)
        AbstractControllerResourceProvider.getResourceProvider(Resource.Type.Task, amc);
    injector.injectMembers(provider);
    TaskResourceProvider.s_dao = hostRoleCommandDAO;

    Predicate predicate = new PredicateBuilder()
        .property(TaskResourceProvider.TASK_REQUEST_ID_PROPERTY_ID).equals("100")
        .and().property(TaskResourceProvider.TASK_ID_PROPERTY_ID).equals("9")
        .or().property(TaskResourceProvider.TASK_REQUEST_ID_PROPERTY_ID).equals("200")
        .toPredicate();
    Request request = PropertyHelper.getReadRequest(Set.of(TaskResourceProvider.TASK_ID_PROPERTY_ID));
    HostRoleCommandEntity first = createTask(1L, 100L, 1L, 9L);
    HostRoleCommandEntity second = createTask(1L, 200L, 2L, 10L);
    expect(hostRoleCommandDAO.findAll(request, predicate)).andReturn(List.of(first, second));
    expect(amc.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getClusterById(1L)).andReturn(cluster).times(2);
    expect(cluster.getResourceId()).andReturn(11L).times(2);
    expect(cluster.getClusterName()).andReturn("cluster-a").times(2);
    SecurityContextHolder.getContext().setAuthentication(
        TestAuthenticationFactory.createClusterAdministrator("alice", 11L));
    replay(amc, hostRoleCommandDAO, clusters, cluster);

    Set<Resource> resources = provider.getResources(request, predicate);

    assertEquals(2, resources.size());
    verify(amc, hostRoleCommandDAO, clusters, cluster);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidTaskParentIsRejectedBeforeDaoQuery() throws Exception {
    AmbariManagementController amc = createMock(AmbariManagementController.class);
    HostRoleCommandDAO hostRoleCommandDAO = createMock(HostRoleCommandDAO.class);
    Injector injector = Guice.createInjector(new InMemoryDefaultTestModule());
    TaskResourceProvider provider = (TaskResourceProvider)
        AbstractControllerResourceProvider.getResourceProvider(Resource.Type.Task, amc);
    injector.injectMembers(provider);
    TaskResourceProvider.s_dao = hostRoleCommandDAO;
    Predicate predicate = new PredicateBuilder()
        .property(TaskResourceProvider.TASK_REQUEST_ID_PROPERTY_ID).equals("invalid")
        .toPredicate();
    Request request = PropertyHelper.getReadRequest(Set.of(TaskResourceProvider.TASK_ID_PROPERTY_ID));
    replay(amc, hostRoleCommandDAO);

    provider.getResources(request, predicate);
  }

  @Test
  public void testTaskQueryMergesPersistedAndLogicalRequests() throws Exception {
    AmbariManagementController amc = createMock(AmbariManagementController.class);
    HostRoleCommandDAO hostRoleCommandDAO = createMock(HostRoleCommandDAO.class);
    TopologyManager topologyManager = createMock(TopologyManager.class);
    Clusters clusters = createMock(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    LogicalRequest logicalRequest = createMock(LogicalRequest.class);
    HostRoleCommand logicalTask = createNiceMock(HostRoleCommand.class);
    Injector injector = Guice.createInjector(new InMemoryDefaultTestModule());
    TaskResourceProvider provider = (TaskResourceProvider)
        AbstractControllerResourceProvider.getResourceProvider(Resource.Type.Task, amc);
    injector.injectMembers(provider);
    TaskResourceProvider.s_dao = hostRoleCommandDAO;
    TaskResourceProvider.s_topologyManager = topologyManager;

    Predicate predicate = new PredicateBuilder()
        .property(TaskResourceProvider.TASK_REQUEST_ID_PROPERTY_ID).equals("100")
        .and().property(TaskResourceProvider.TASK_ID_PROPERTY_ID).equals("10")
        .or().property(TaskResourceProvider.TASK_REQUEST_ID_PROPERTY_ID).equals("200")
        .and().property(TaskResourceProvider.TASK_ID_PROPERTY_ID).equals("20")
        .toPredicate();
    Request request = PropertyHelper.getReadRequest(Set.of(TaskResourceProvider.TASK_ID_PROPERTY_ID));
    HostRoleCommandEntity persistedTask = createTask(1L, 100L, 1L, 10L);
    expect(hostRoleCommandDAO.findAll(request, predicate)).andReturn(List.of(persistedTask));
    expect(topologyManager.getRequests(EasyMock.<Collection<Long>>anyObject()))
        .andReturn(List.of(logicalRequest));
    expect(logicalRequest.getClusterId()).andReturn(1L);
    expect(logicalRequest.getRequestId()).andReturn(200L).anyTimes();
    expect(logicalRequest.getCommands()).andReturn(List.of(logicalTask));
    expect(logicalTask.getRequestId()).andReturn(200L).anyTimes();
    expect(logicalTask.getTaskId()).andReturn(20L).anyTimes();
    expect(logicalTask.getStageId()).andReturn(2L).anyTimes();
    expect(logicalTask.getRole()).andReturn(Role.DATANODE).anyTimes();
    expect(amc.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getClusterById(1L)).andReturn(cluster).times(2);
    expect(cluster.getResourceId()).andReturn(11L).times(2);
    expect(cluster.getClusterName()).andReturn("cluster-a").times(2);
    SecurityContextHolder.getContext().setAuthentication(
        TestAuthenticationFactory.createClusterAdministrator("alice", 11L));
    replay(amc, hostRoleCommandDAO, topologyManager, clusters, cluster, logicalRequest, logicalTask);

    Set<Resource> resources = provider.getResources(request, predicate);

    assertEquals(2, resources.size());
    verify(amc, hostRoleCommandDAO, topologyManager, clusters, cluster, logicalRequest, logicalTask);
  }

  @Test
  public void testDirectTaskStatusFilterCanReturnNoMatch() throws Exception {
    AmbariManagementController amc = createMock(AmbariManagementController.class);
    HostRoleCommandDAO hostRoleCommandDAO = createMock(HostRoleCommandDAO.class);
    Clusters clusters = createMock(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    Injector injector = Guice.createInjector(new InMemoryDefaultTestModule());
    TaskResourceProvider provider = (TaskResourceProvider)
        AbstractControllerResourceProvider.getResourceProvider(Resource.Type.Task, amc);
    injector.injectMembers(provider);
    TaskResourceProvider.s_dao = hostRoleCommandDAO;

    Predicate predicate = new PredicateBuilder()
        .property(TaskResourceProvider.TASK_CLUSTER_NAME_PROPERTY_ID).equals("cluster-a")
        .and().property(TaskResourceProvider.TASK_REQUEST_ID_PROPERTY_ID).equals("100")
        .and().property(TaskResourceProvider.TASK_ID_PROPERTY_ID).equals("10")
        .and().property(TaskResourceProvider.TASK_STATUS_PROPERTY_ID).equals(HostRoleStatus.FAILED)
        .toPredicate();
    Request request = PropertyHelper.getReadRequest(Set.of(TaskResourceProvider.TASK_ID_PROPERTY_ID));
    HostRoleCommandEntity task = createTask(1L, 100L, 1L, 10L);
    task.setStatus(HostRoleStatus.IN_PROGRESS);
    expect(hostRoleCommandDAO.findAll(request, predicate)).andReturn(List.of(task));
    expect(amc.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getClusterById(1L)).andReturn(cluster);
    expect(cluster.getResourceId()).andReturn(11L);
    expect(cluster.getClusterName()).andReturn("cluster-a").times(2);
    SecurityContextHolder.getContext().setAuthentication(
        TestAuthenticationFactory.createClusterAdministrator("alice", 11L));
    replay(amc, hostRoleCommandDAO, clusters, cluster);

    Assert.assertTrue(provider.getResources(request, predicate).isEmpty());
    verify(amc, hostRoleCommandDAO, clusters, cluster);
  }

  private HostRoleCommandEntity createTask(long clusterId, long requestId,
      long stageId, long taskId) {
    StageEntity stage = new StageEntity();
    stage.setClusterId(clusterId);
    stage.setRequestId(requestId);
    stage.setStageId(stageId);
    HostRoleCommandEntity task = new HostRoleCommandEntity();
    task.setRequestId(requestId);
    task.setStageId(stageId);
    task.setTaskId(taskId);
    task.setRole(Role.DATANODE);
    task.setStage(stage);
    return task;
  }


  @Test
  public void testUpdateResources() throws Exception {
    Resource.Type type = Resource.Type.Task;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    // replay
    replay(managementController, response);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);

    // add the property map to a set for the request.
    Map<String, Object> properties = new LinkedHashMap<>();

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, null);

    Predicate predicate = new PredicateBuilder().property(TaskResourceProvider.TASK_ID_PROPERTY_ID).equals("Task100").
        toPredicate();

    try {
      provider.updateResources(request, predicate);
      Assert.fail("Expected an UnsupportedOperationException");
    } catch (UnsupportedOperationException e) {
      // expected
    }

    // verify
    verify(managementController, response);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testDeleteResources() throws Exception {
    Resource.Type type = Resource.Type.Task;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    // replay
    replay(managementController);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);

    Predicate predicate = new PredicateBuilder().property(TaskResourceProvider.TASK_ID_PROPERTY_ID).equals("Task100").toPredicate();
    provider.deleteResources(new RequestImpl(null, null, null, null), predicate);
    // verify
    verify(managementController);
  }

  @Test
  public void testParseStructuredOutput() {
    Resource.Type type = Resource.Type.Task;
    // Test general case
    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    TaskResourceProvider taskResourceProvider = new TaskResourceProvider(managementController);

    replay(managementController);

    // Check parsing of nested JSON
    Map<?, ?> result = taskResourceProvider
        .parseStructuredOutput("{\"a\":\"b\", \"c\": {\"d\":\"e\",\"f\": [\"g\",\"h\"],\"i\": {\"k\":\"l\"}}}");
    assertEquals(result.size(), 2);
    Map<?, ?> submap = (Map<?, ?>) result.get("c");
    assertEquals(submap.size(), 3);
    List sublist = (List) submap.get("f");
    assertEquals(sublist.size(), 2);
    Map<?, ?> subsubmap = (Map<?, ?>) submap.get("i");
    assertEquals(subsubmap.size(), 1);
    assertEquals(subsubmap.get("k"), "l");

    // Check negative case - invalid JSON
    result = taskResourceProvider.parseStructuredOutput("{\"a\": invalid JSON}");
    assertNull(result);

    // ensure that integers come back as integers
    result = taskResourceProvider.parseStructuredOutput("{\"a\": 5}");
    assertEquals(result.get("a"), 5);

    verify(managementController);
  }

  @Test
  public void testParseStructuredOutputForHostCheck() {
    Resource.Type type = Resource.Type.Task;

    // Test general case
    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    TaskResourceProvider taskResourceProvider = new TaskResourceProvider(managementController);

    replay(managementController);

    Map<?, ?> result = taskResourceProvider.parseStructuredOutput("{\"host_resolution_check\": {\"failures\": [{\"cause\": [-2, \"Name or service not known\"], \"host\": \"foobar\", \"type\": \"FORWARD_LOOKUP\"}], \"message\": \"There were 1 host(s) that could not resolve to an IP address.\", \"failed_count\": 1, \"success_count\": 3, \"exit_code\": 0}}");

    Assert.assertNotNull(result);
    Map<?,?> host_resolution_check = (Map<?,?>)result.get("host_resolution_check");

    assertEquals(host_resolution_check.get("success_count"), 3);
    assertEquals(host_resolution_check.get("failed_count"), 1);

    verify(managementController);
  }

  @Test
  public void testInvalidStructuredOutput() {
    Resource.Type type = Resource.Type.Task;

    // Test general case
    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    TaskResourceProvider taskResourceProvider = new TaskResourceProvider(managementController);

    replay(managementController);

    Map<?, ?> result = taskResourceProvider.parseStructuredOutput(null);
    Assert.assertNull(result);

    result = taskResourceProvider.parseStructuredOutput("This is some bad JSON");
    Assert.assertNull(result);

    verify(managementController);
  }

}
