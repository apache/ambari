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

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.easymock.EasyMock.anyObject;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.api.services.BaseRequest;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ExecuteActionRequest;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.easymock.Capture;
import org.junit.Assert;
import org.junit.Test;

/**
 * RequestResourceProvider tests.
 */
public class RequestResourceProviderTest {
  @Test
  public void testCreateResources() throws Exception {
    Resource.Type type = Resource.Type.Request;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    // replay
    replay(managementController, response);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<Map<String, Object>>();

    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    // add properties to the request map
    properties.put(RequestResourceProvider.REQUEST_ID_PROPERTY_ID, "Request100");

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
  public void testGetResourcesWithRequestInfo() throws Exception {
    Resource.Type type = Resource.Type.Request;

    ActionManager actionManager = createNiceMock(ActionManager.class);

    Clusters clusters = createNiceMock(Clusters.class);
    expect(clusters.getCluster("foo_cluster")).andReturn(null).anyTimes();

    AmbariManagementController managementController =
      createMock(AmbariManagementController.class);
    expect(managementController.getActionManager()).andReturn(actionManager)
      .anyTimes();
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();

    replay(managementController, clusters);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(type,
      PropertyHelper.getPropertyIds(type), PropertyHelper.getKeyPropertyIds(type),
      managementController);

    Map<String, String> requestInfoProperties = new HashMap<String, String>();
    Request request = null;
    Predicate predicate = new PredicateBuilder()
      .property(RequestResourceProvider.REQUEST_CLUSTER_NAME_PROPERTY_ID)
      .equals("foo_cluster")
      .and().property(RequestResourceProvider.REQUEST_ID_PROPERTY_ID)
      .equals(null)
      .and().property(RequestResourceProvider.REQUEST_STATUS_PROPERTY_ID)
      .equals(null)
      .toPredicate();

    request = PropertyHelper.getReadRequest(new HashSet<String>(),
      requestInfoProperties, null);
    expect(actionManager.getRequests(Collections.<Long> emptyList()))
      .andReturn(Collections.<org.apache.ambari.server.actionmanager.Request> emptyList());
    expect(actionManager.getRequestsByStatus(null,
      BaseRequest.DEFAULT_PAGE_SIZE, false))
      .andReturn(Collections.<Long> emptyList());

    replay(actionManager);
    provider.getResources(request, predicate);
    verify(actionManager);
    reset(actionManager);

    requestInfoProperties.put(BaseRequest.PAGE_SIZE_PROPERTY_KEY, "20");
    request = PropertyHelper.getReadRequest(new HashSet<String>(),
      requestInfoProperties, null);
    expect(actionManager.getRequests(Collections.<Long> emptyList()))
      .andReturn(Collections.<org.apache.ambari.server.actionmanager.Request> emptyList());
    expect(actionManager.getRequestsByStatus(null, 20, false))
      .andReturn(Collections.<Long> emptyList());
    replay(actionManager);
    provider.getResources(request, predicate);
    verify(actionManager);
    reset(actionManager);

    requestInfoProperties.put(BaseRequest.ASC_ORDER_PROPERTY_KEY, "true");
    request = PropertyHelper.getReadRequest(new HashSet<String>(),
      requestInfoProperties, null);
    expect(actionManager.getRequests(Collections.<Long> emptyList()))
      .andReturn(Collections.<org.apache.ambari.server.actionmanager.Request> emptyList());
    expect(actionManager.getRequestsByStatus(null, 20, true))
      .andReturn(Collections.<Long> emptyList());
    replay(actionManager);
    provider.getResources(request, predicate);
    verify(actionManager);
    reset(actionManager);
  }

  @Test
  public void testGetResources() throws Exception {
    Resource.Type type = Resource.Type.Request;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    ActionManager actionManager = createNiceMock(ActionManager.class);
    HostRoleCommand hostRoleCommand = createNiceMock(HostRoleCommand.class);


    List<HostRoleCommand> hostRoleCommands = new LinkedList<HostRoleCommand>();
    hostRoleCommands.add(hostRoleCommand);

    org.apache.ambari.server.actionmanager.Request requestMock =
        createNiceMock(org.apache.ambari.server.actionmanager.Request.class);
    expect(requestMock.getCommands()).andReturn(hostRoleCommands).anyTimes();
    expect(requestMock.getRequestContext()).andReturn("this is a context").anyTimes();
    expect(requestMock.getRequestId()).andReturn(100L).anyTimes();

    Capture<Collection<Long>> requestIdsCapture = new Capture<Collection<Long>>();

    // set expectations
    expect(managementController.getActionManager()).andReturn(actionManager);
    expect(actionManager.getRequests(capture(requestIdsCapture))).andReturn(Collections.singletonList(requestMock)).anyTimes();
    expect(hostRoleCommand.getRequestId()).andReturn(100L).anyTimes();
    expect(hostRoleCommand.getStatus()).andReturn(HostRoleStatus.IN_PROGRESS);

    // replay
    replay(managementController, actionManager, hostRoleCommand, requestMock);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    Set<String> propertyIds = new HashSet<String>();

    propertyIds.add(RequestResourceProvider.REQUEST_ID_PROPERTY_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_STATUS_PROPERTY_ID);

    Predicate predicate = new PredicateBuilder().property(RequestResourceProvider.REQUEST_ID_PROPERTY_ID).equals("100").
        toPredicate();
    Request request = PropertyHelper.getReadRequest(propertyIds);
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      Assert.assertEquals(100L, (long) (Long) resource.getPropertyValue(RequestResourceProvider.REQUEST_ID_PROPERTY_ID));
      Assert.assertEquals("IN_PROGRESS", resource.getPropertyValue(RequestResourceProvider.REQUEST_STATUS_PROPERTY_ID));
    }

    // verify
    verify(managementController, actionManager, hostRoleCommand);
  }

  @Test
  public void testGetResourcesWithRequestSchedule() throws Exception {
    Resource.Type type = Resource.Type.Request;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    ActionManager actionManager = createNiceMock(ActionManager.class);
    HostRoleCommand hostRoleCommand = createNiceMock(HostRoleCommand.class);


    List<HostRoleCommand> hostRoleCommands = new LinkedList<HostRoleCommand>();
    hostRoleCommands.add(hostRoleCommand);

    org.apache.ambari.server.actionmanager.Request requestMock =
        createNiceMock(org.apache.ambari.server.actionmanager.Request.class);
    expect(requestMock.getCommands()).andReturn(hostRoleCommands).anyTimes();
    expect(requestMock.getRequestContext()).andReturn("this is a context").anyTimes();
    expect(requestMock.getRequestId()).andReturn(100L).anyTimes();
    expect(requestMock.getRequestScheduleId()).andReturn(11L).anyTimes();


    Capture<Collection<Long>> requestIdsCapture = new Capture<Collection<Long>>();

    // set expectations
    expect(managementController.getActionManager()).andReturn(actionManager);
    expect(actionManager.getRequests(capture(requestIdsCapture))).andReturn(Collections.singletonList(requestMock)).anyTimes();
    expect(hostRoleCommand.getRequestId()).andReturn(100L).anyTimes();
    expect(hostRoleCommand.getStatus()).andReturn(HostRoleStatus.IN_PROGRESS);

    // replay
    replay(managementController, actionManager, hostRoleCommand, requestMock);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    Set<String> propertyIds = new HashSet<String>();

    propertyIds.add(RequestResourceProvider.REQUEST_ID_PROPERTY_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_STATUS_PROPERTY_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_SOURCE_SCHEDULE);


    Predicate predicate = new PredicateBuilder().property(RequestResourceProvider.REQUEST_ID_PROPERTY_ID).equals("100").
        toPredicate();
    Request request = PropertyHelper.getReadRequest(propertyIds);
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      Assert.assertEquals(100L, (long) (Long) resource.getPropertyValue(RequestResourceProvider.REQUEST_ID_PROPERTY_ID));
      Assert.assertEquals("IN_PROGRESS", resource.getPropertyValue(RequestResourceProvider.REQUEST_STATUS_PROPERTY_ID));
      Assert.assertEquals(11L, (long) (Long) resource.getPropertyValue(RequestResourceProvider.REQUEST_SOURCE_SCHEDULE_ID));
    }

    // verify
    verify(managementController, actionManager, hostRoleCommand);
  }

  @Test
  public void testGetResourcesWithoutRequestSchedule() throws Exception {
    Resource.Type type = Resource.Type.Request;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    ActionManager actionManager = createNiceMock(ActionManager.class);
    HostRoleCommand hostRoleCommand = createNiceMock(HostRoleCommand.class);


    List<HostRoleCommand> hostRoleCommands = new LinkedList<HostRoleCommand>();
    hostRoleCommands.add(hostRoleCommand);

    org.apache.ambari.server.actionmanager.Request requestMock =
        createNiceMock(org.apache.ambari.server.actionmanager.Request.class);
    expect(requestMock.getCommands()).andReturn(hostRoleCommands).anyTimes();
    expect(requestMock.getRequestContext()).andReturn("this is a context").anyTimes();
    expect(requestMock.getRequestId()).andReturn(100L).anyTimes();
    expect(requestMock.getRequestScheduleId()).andReturn(null).anyTimes();


    Capture<Collection<Long>> requestIdsCapture = new Capture<Collection<Long>>();

    // set expectations
    expect(managementController.getActionManager()).andReturn(actionManager);
    expect(actionManager.getRequests(capture(requestIdsCapture))).andReturn(Collections.singletonList(requestMock)).anyTimes();
    expect(hostRoleCommand.getRequestId()).andReturn(100L).anyTimes();
    expect(hostRoleCommand.getStatus()).andReturn(HostRoleStatus.IN_PROGRESS);

    // replay
    replay(managementController, actionManager, hostRoleCommand, requestMock);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    Set<String> propertyIds = new HashSet<String>();

    propertyIds.add(RequestResourceProvider.REQUEST_ID_PROPERTY_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_STATUS_PROPERTY_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_SOURCE_SCHEDULE);


    Predicate predicate = new PredicateBuilder().property(RequestResourceProvider.REQUEST_ID_PROPERTY_ID).equals("100").
        toPredicate();
    Request request = PropertyHelper.getReadRequest(propertyIds);
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      Assert.assertEquals(100L, (long) (Long) resource.getPropertyValue(RequestResourceProvider.REQUEST_ID_PROPERTY_ID));
      Assert.assertEquals("IN_PROGRESS", resource.getPropertyValue(RequestResourceProvider.REQUEST_STATUS_PROPERTY_ID));
      Assert.assertEquals(null, resource.getPropertyValue(RequestResourceProvider.REQUEST_SOURCE_SCHEDULE));
    }

    // verify
    verify(managementController, actionManager, hostRoleCommand);
  }

  @Test
  public void testGetResourcesWithCluster() throws Exception {
    Resource.Type type = Resource.Type.Request;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    ActionManager actionManager = createNiceMock(ActionManager.class);
    HostRoleCommand hostRoleCommand = createNiceMock(HostRoleCommand.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);

    List<HostRoleCommand> hostRoleCommands = new LinkedList<HostRoleCommand>();
    hostRoleCommands.add(hostRoleCommand);

    org.apache.ambari.server.actionmanager.Request requestMock =
        createNiceMock(org.apache.ambari.server.actionmanager.Request.class);
    expect(requestMock.getCommands()).andReturn(hostRoleCommands).anyTimes();
    expect(requestMock.getRequestContext()).andReturn("this is a context").anyTimes();
    expect(requestMock.getClusterName()).andReturn("c1").anyTimes();
    expect(requestMock.getRequestId()).andReturn(100L).anyTimes();

    Capture<Collection<Long>> requestIdsCapture = new Capture<Collection<Long>>();

    // set expectations
    expect(managementController.getActionManager()).andReturn(actionManager).anyTimes();
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("c1")).andReturn(cluster).anyTimes();
    expect(clusters.getCluster("bad-cluster")).andThrow(new AmbariException("bad cluster!")).anyTimes();
    expect(actionManager.getRequests(capture(requestIdsCapture))).andReturn(Collections.singletonList(requestMock));
    expect(hostRoleCommand.getRequestId()).andReturn(100L).anyTimes();
    expect(hostRoleCommand.getStatus()).andReturn(HostRoleStatus.IN_PROGRESS);

    // replay
    replay(managementController, actionManager, hostRoleCommand, clusters, cluster, requestMock);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    Set<String> propertyIds = new HashSet<String>();

    propertyIds.add(RequestResourceProvider.REQUEST_ID_PROPERTY_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_STATUS_PROPERTY_ID);

    Predicate predicate = new PredicateBuilder().
        property(RequestResourceProvider.REQUEST_CLUSTER_NAME_PROPERTY_ID).equals("c1").and().
        property(RequestResourceProvider.REQUEST_ID_PROPERTY_ID).equals("100").
        toPredicate();
    Request request = PropertyHelper.getReadRequest(propertyIds);
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      Assert.assertEquals(100L, (long) (Long) resource.getPropertyValue(RequestResourceProvider.REQUEST_ID_PROPERTY_ID));
      Assert.assertEquals("IN_PROGRESS", resource.getPropertyValue(RequestResourceProvider.REQUEST_STATUS_PROPERTY_ID));
    }

    // try again with a bad cluster name
    predicate = new PredicateBuilder().
        property(RequestResourceProvider.REQUEST_CLUSTER_NAME_PROPERTY_ID).equals("bad-cluster").and().
        property(RequestResourceProvider.REQUEST_ID_PROPERTY_ID).equals("100").
        toPredicate();
    try {
      provider.getResources(request, predicate);
    } catch (NoSuchParentResourceException e) {
      e.printStackTrace();
    }

    // verify
    verify(managementController, actionManager, hostRoleCommand, clusters, cluster);
  }

  @Test
  public void testGetResourcesOrPredicate() throws Exception {
    Resource.Type type = Resource.Type.Request;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    ActionManager actionManager = createNiceMock(ActionManager.class);
    HostRoleCommand hostRoleCommand = createNiceMock(HostRoleCommand.class);

    List<HostRoleCommand> hostRoleCommands = new LinkedList<HostRoleCommand>();
    hostRoleCommands.add(hostRoleCommand);

    org.apache.ambari.server.actionmanager.Request requestMock =
        createNiceMock(org.apache.ambari.server.actionmanager.Request.class);
    expect(requestMock.getCommands()).andReturn(hostRoleCommands).anyTimes();
    expect(requestMock.getRequestContext()).andReturn("this is a context").anyTimes();
    expect(requestMock.getRequestId()).andReturn(100L).anyTimes();

    org.apache.ambari.server.actionmanager.Request requestMock1 =
        createNiceMock(org.apache.ambari.server.actionmanager.Request.class);
    expect(requestMock1.getCommands()).andReturn(hostRoleCommands).anyTimes();
    expect(requestMock1.getRequestContext()).andReturn("this is a context").anyTimes();
    expect(requestMock1.getRequestId()).andReturn(101L).anyTimes();

    Capture<Collection<Long>> requestIdsCapture = new Capture<Collection<Long>>();

    // set expectations
    expect(managementController.getActionManager()).andReturn(actionManager).anyTimes();
    expect(actionManager.getRequests(capture(requestIdsCapture))).
        andReturn(Arrays.asList(requestMock, requestMock1)).anyTimes();
    expect(hostRoleCommand.getStatus()).andReturn(HostRoleStatus.IN_PROGRESS).anyTimes();

    // replay
    replay(managementController, actionManager, hostRoleCommand, requestMock, requestMock1);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    Set<String> propertyIds = new HashSet<String>();

    propertyIds.add(RequestResourceProvider.REQUEST_ID_PROPERTY_ID);

    Predicate predicate = new PredicateBuilder().property(RequestResourceProvider.REQUEST_ID_PROPERTY_ID).equals("100").
        or().property(RequestResourceProvider.REQUEST_ID_PROPERTY_ID).equals("101").
        toPredicate();
    Request request = PropertyHelper.getReadRequest(propertyIds);
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(2, resources.size());
    for (Resource resource : resources) {
      long id = (Long) resource.getPropertyValue(RequestResourceProvider.REQUEST_ID_PROPERTY_ID);
      Assert.assertTrue(id == 100L || id == 101L);
    }

    // verify
    verify(managementController, actionManager, hostRoleCommand);
  }

  @Test
  public void testGetResourcesCompleted() throws Exception {
    Resource.Type type = Resource.Type.Request;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    ActionManager actionManager = createNiceMock(ActionManager.class);
    HostRoleCommand hostRoleCommand0 = createNiceMock(HostRoleCommand.class);
    HostRoleCommand hostRoleCommand1 = createNiceMock(HostRoleCommand.class);
    HostRoleCommand hostRoleCommand2 = createNiceMock(HostRoleCommand.class);
    HostRoleCommand hostRoleCommand3 = createNiceMock(HostRoleCommand.class);

    List<HostRoleCommand> hostRoleCommands0 = new LinkedList<HostRoleCommand>();
    hostRoleCommands0.add(hostRoleCommand0);
    hostRoleCommands0.add(hostRoleCommand1);

    List<HostRoleCommand> hostRoleCommands1 = new LinkedList<HostRoleCommand>();
    hostRoleCommands1.add(hostRoleCommand2);
    hostRoleCommands1.add(hostRoleCommand3);

    org.apache.ambari.server.actionmanager.Request requestMock0 =
        createNiceMock(org.apache.ambari.server.actionmanager.Request.class);
    expect(requestMock0.getCommands()).andReturn(hostRoleCommands0).anyTimes();
    expect(requestMock0.getRequestContext()).andReturn("this is a context").anyTimes();
    expect(requestMock0.getRequestId()).andReturn(100L).anyTimes();

    org.apache.ambari.server.actionmanager.Request requestMock1 =
        createNiceMock(org.apache.ambari.server.actionmanager.Request.class);
    expect(requestMock1.getCommands()).andReturn(hostRoleCommands1).anyTimes();
    expect(requestMock1.getRequestContext()).andReturn("this is a context").anyTimes();
    expect(requestMock1.getRequestId()).andReturn(101L).anyTimes();

    Capture<Collection<Long>> requestIdsCapture = new Capture<Collection<Long>>();

    // set expectations
    expect(managementController.getActionManager()).andReturn(actionManager).anyTimes();
    expect(actionManager.getRequests(capture(requestIdsCapture))).andReturn(Arrays.asList(requestMock0));
    expect(actionManager.getRequests(capture(requestIdsCapture))).andReturn(Arrays.asList(requestMock1));
    expect(hostRoleCommand0.getRequestId()).andReturn(100L).anyTimes();
    expect(hostRoleCommand1.getRequestId()).andReturn(100L).anyTimes();
    expect(hostRoleCommand2.getRequestId()).andReturn(101L).anyTimes();
    expect(hostRoleCommand3.getRequestId()).andReturn(101L).anyTimes();
    expect(hostRoleCommand0.getStatus()).andReturn(HostRoleStatus.COMPLETED).anyTimes();
    expect(hostRoleCommand1.getStatus()).andReturn(HostRoleStatus.COMPLETED).anyTimes();
    expect(hostRoleCommand2.getStatus()).andReturn(HostRoleStatus.COMPLETED).anyTimes();
    expect(hostRoleCommand3.getStatus()).andReturn(HostRoleStatus.COMPLETED).anyTimes();

    // replay
    replay(managementController, actionManager, hostRoleCommand0, hostRoleCommand1, hostRoleCommand2, hostRoleCommand3,
        requestMock0, requestMock1);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    Set<String> propertyIds = new HashSet<String>();

    propertyIds.add(RequestResourceProvider.REQUEST_ID_PROPERTY_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_STATUS_PROPERTY_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_TASK_CNT_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_COMPLETED_TASK_CNT_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_FAILED_TASK_CNT_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_PROGRESS_PERCENT_ID);

    Predicate predicate = new PredicateBuilder().property(RequestResourceProvider.REQUEST_ID_PROPERTY_ID).equals("100").or().
        property(RequestResourceProvider.REQUEST_ID_PROPERTY_ID).equals("101").toPredicate();
    Request request = PropertyHelper.getReadRequest(propertyIds);
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(2, resources.size());
    for (Resource resource : resources) {
      long id = (Long) resource.getPropertyValue(RequestResourceProvider.REQUEST_ID_PROPERTY_ID);
      Assert.assertTrue(id == 100L || id == 101L);
      Assert.assertEquals("COMPLETED", resource.getPropertyValue(RequestResourceProvider.REQUEST_STATUS_PROPERTY_ID));
      Assert.assertEquals(2, resource.getPropertyValue(RequestResourceProvider.REQUEST_TASK_CNT_ID));
      Assert.assertEquals(0, resource.getPropertyValue(RequestResourceProvider.REQUEST_FAILED_TASK_CNT_ID));
      Assert.assertEquals(2, resource.getPropertyValue(RequestResourceProvider.REQUEST_COMPLETED_TASK_CNT_ID));

      Assert.assertEquals(100.0, resource.getPropertyValue(RequestResourceProvider.REQUEST_PROGRESS_PERCENT_ID));
    }

    // verify
    verify(managementController, actionManager, hostRoleCommand0, hostRoleCommand1, hostRoleCommand2, hostRoleCommand3);
  }

  @Test
  public void testGetResourcesInProgress() throws Exception {
    Resource.Type type = Resource.Type.Request;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    ActionManager actionManager = createNiceMock(ActionManager.class);
    HostRoleCommand hostRoleCommand0 = createNiceMock(HostRoleCommand.class);
    HostRoleCommand hostRoleCommand1 = createNiceMock(HostRoleCommand.class);
    HostRoleCommand hostRoleCommand2 = createNiceMock(HostRoleCommand.class);
    HostRoleCommand hostRoleCommand3 = createNiceMock(HostRoleCommand.class);

    List<HostRoleCommand> hostRoleCommands0 = new LinkedList<HostRoleCommand>();
    hostRoleCommands0.add(hostRoleCommand0);
    hostRoleCommands0.add(hostRoleCommand1);

    List<HostRoleCommand> hostRoleCommands1 = new LinkedList<HostRoleCommand>();
    hostRoleCommands1.add(hostRoleCommand2);
    hostRoleCommands1.add(hostRoleCommand3);

    org.apache.ambari.server.actionmanager.Request requestMock0 =
        createNiceMock(org.apache.ambari.server.actionmanager.Request.class);
    expect(requestMock0.getCommands()).andReturn(hostRoleCommands0).anyTimes();
    expect(requestMock0.getRequestContext()).andReturn("this is a context").anyTimes();
    expect(requestMock0.getRequestId()).andReturn(100L).anyTimes();

    org.apache.ambari.server.actionmanager.Request requestMock1 =
        createNiceMock(org.apache.ambari.server.actionmanager.Request.class);
    expect(requestMock1.getCommands()).andReturn(hostRoleCommands1).anyTimes();
    expect(requestMock1.getRequestContext()).andReturn("this is a context").anyTimes();
    expect(requestMock1.getRequestId()).andReturn(101L).anyTimes();

    Capture<Collection<Long>> requestIdsCapture = new Capture<Collection<Long>>();

    // set expectations
    expect(managementController.getActionManager()).andReturn(actionManager).anyTimes();
    expect(actionManager.getRequests(capture(requestIdsCapture))).andReturn(Arrays.asList(requestMock0));
    expect(actionManager.getRequests(capture(requestIdsCapture))).andReturn(Arrays.asList(requestMock1));
    expect(hostRoleCommand0.getRequestId()).andReturn(100L).anyTimes();
    expect(hostRoleCommand1.getRequestId()).andReturn(100L).anyTimes();
    expect(hostRoleCommand2.getRequestId()).andReturn(101L).anyTimes();
    expect(hostRoleCommand3.getRequestId()).andReturn(101L).anyTimes();
    expect(hostRoleCommand0.getStatus()).andReturn(HostRoleStatus.IN_PROGRESS).anyTimes();
    expect(hostRoleCommand1.getStatus()).andReturn(HostRoleStatus.PENDING).anyTimes();
    expect(hostRoleCommand2.getStatus()).andReturn(HostRoleStatus.IN_PROGRESS).anyTimes();
    expect(hostRoleCommand3.getStatus()).andReturn(HostRoleStatus.QUEUED).anyTimes();

    // replay
    replay(managementController, actionManager, hostRoleCommand0, hostRoleCommand1, hostRoleCommand2, hostRoleCommand3,
        requestMock0, requestMock1);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    Set<String> propertyIds = new HashSet<String>();

    propertyIds.add(RequestResourceProvider.REQUEST_ID_PROPERTY_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_STATUS_PROPERTY_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_TASK_CNT_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_COMPLETED_TASK_CNT_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_FAILED_TASK_CNT_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_QUEUED_TASK_CNT_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_PROGRESS_PERCENT_ID);

    Predicate predicate = new PredicateBuilder().property(RequestResourceProvider.REQUEST_ID_PROPERTY_ID).equals("100").or().
        property(RequestResourceProvider.REQUEST_ID_PROPERTY_ID).equals("101").toPredicate();
    Request request = PropertyHelper.getReadRequest(propertyIds);
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(2, resources.size());
    for (Resource resource : resources) {
      long id = (Long) resource.getPropertyValue(RequestResourceProvider.REQUEST_ID_PROPERTY_ID);
      Assert.assertTrue(id == 100L || id == 101L);
      Assert.assertEquals("IN_PROGRESS", resource.getPropertyValue(RequestResourceProvider.REQUEST_STATUS_PROPERTY_ID));
      Assert.assertEquals(2, resource.getPropertyValue(RequestResourceProvider.REQUEST_TASK_CNT_ID));
      Assert.assertEquals(0, resource.getPropertyValue(RequestResourceProvider.REQUEST_FAILED_TASK_CNT_ID));

      if (id == 100L) {
        Assert.assertEquals(0, resource.getPropertyValue(RequestResourceProvider.REQUEST_QUEUED_TASK_CNT_ID));
        int progressPercent = ((Double) resource.getPropertyValue(RequestResourceProvider.REQUEST_PROGRESS_PERCENT_ID)).intValue();
        Assert.assertEquals(17, progressPercent);
      } else {
        Assert.assertEquals(1, resource.getPropertyValue(RequestResourceProvider.REQUEST_QUEUED_TASK_CNT_ID));
        int progressPercent = ((Double) resource.getPropertyValue(RequestResourceProvider.REQUEST_PROGRESS_PERCENT_ID)).intValue();
        Assert.assertEquals(21, progressPercent);
      }
      Assert.assertEquals(0, resource.getPropertyValue(RequestResourceProvider.REQUEST_COMPLETED_TASK_CNT_ID));
    }

    // verify
    verify(managementController, actionManager, hostRoleCommand0, hostRoleCommand1, hostRoleCommand2, hostRoleCommand3);
  }

  @Test
  public void testGetResourcesFailed() throws Exception {
    Resource.Type type = Resource.Type.Request;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    ActionManager actionManager = createNiceMock(ActionManager.class);
    HostRoleCommand hostRoleCommand0 = createNiceMock(HostRoleCommand.class);
    HostRoleCommand hostRoleCommand1 = createNiceMock(HostRoleCommand.class);
    HostRoleCommand hostRoleCommand2 = createNiceMock(HostRoleCommand.class);
    HostRoleCommand hostRoleCommand3 = createNiceMock(HostRoleCommand.class);

    List<HostRoleCommand> hostRoleCommands0 = new LinkedList<HostRoleCommand>();
    hostRoleCommands0.add(hostRoleCommand0);
    hostRoleCommands0.add(hostRoleCommand1);

    List<HostRoleCommand> hostRoleCommands1 = new LinkedList<HostRoleCommand>();
    hostRoleCommands1.add(hostRoleCommand2);
    hostRoleCommands1.add(hostRoleCommand3);

    org.apache.ambari.server.actionmanager.Request requestMock0 =
        createNiceMock(org.apache.ambari.server.actionmanager.Request.class);
    expect(requestMock0.getCommands()).andReturn(hostRoleCommands0).anyTimes();
    expect(requestMock0.getRequestContext()).andReturn("this is a context").anyTimes();
    expect(requestMock0.getRequestId()).andReturn(100L).anyTimes();

    org.apache.ambari.server.actionmanager.Request requestMock1 =
        createNiceMock(org.apache.ambari.server.actionmanager.Request.class);
    expect(requestMock1.getCommands()).andReturn(hostRoleCommands1).anyTimes();
    expect(requestMock1.getRequestContext()).andReturn("this is a context").anyTimes();
    expect(requestMock1.getRequestId()).andReturn(101L).anyTimes();

    Capture<Collection<Long>> requestIdsCapture = new Capture<Collection<Long>>();

    // set expectations
    expect(managementController.getActionManager()).andReturn(actionManager).anyTimes();
    expect(actionManager.getRequests(capture(requestIdsCapture))).andReturn(Arrays.asList(requestMock0));
    expect(actionManager.getRequests(capture(requestIdsCapture))).andReturn(Arrays.asList(requestMock1));
    expect(hostRoleCommand0.getRequestId()).andReturn(100L).anyTimes();
    expect(hostRoleCommand1.getRequestId()).andReturn(100L).anyTimes();
    expect(hostRoleCommand2.getRequestId()).andReturn(101L).anyTimes();
    expect(hostRoleCommand3.getRequestId()).andReturn(101L).anyTimes();
    expect(hostRoleCommand0.getStatus()).andReturn(HostRoleStatus.FAILED).anyTimes();
    expect(hostRoleCommand1.getStatus()).andReturn(HostRoleStatus.COMPLETED).anyTimes();
    expect(hostRoleCommand2.getStatus()).andReturn(HostRoleStatus.ABORTED).anyTimes();
    expect(hostRoleCommand3.getStatus()).andReturn(HostRoleStatus.TIMEDOUT).anyTimes();

    // replay
    replay(managementController, actionManager, hostRoleCommand0, hostRoleCommand1, hostRoleCommand2, hostRoleCommand3,
        requestMock0, requestMock1);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    Set<String> propertyIds = new HashSet<String>();

    propertyIds.add(RequestResourceProvider.REQUEST_ID_PROPERTY_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_STATUS_PROPERTY_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_TASK_CNT_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_COMPLETED_TASK_CNT_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_FAILED_TASK_CNT_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_ABORTED_TASK_CNT_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_TIMED_OUT_TASK_CNT_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_PROGRESS_PERCENT_ID);

    Predicate predicate = new PredicateBuilder().property(RequestResourceProvider.REQUEST_ID_PROPERTY_ID).equals("100").or().
        property(RequestResourceProvider.REQUEST_ID_PROPERTY_ID).equals("101").toPredicate();
    Request request = PropertyHelper.getReadRequest(propertyIds);
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(2, resources.size());
    for (Resource resource : resources) {
      long id = (Long) resource.getPropertyValue(RequestResourceProvider.REQUEST_ID_PROPERTY_ID);
      Assert.assertTrue(id == 100L || id == 101L);
      Assert.assertEquals(2, resource.getPropertyValue(RequestResourceProvider.REQUEST_TASK_CNT_ID));
      if (id == 100L) {
        Assert.assertEquals("FAILED", resource.getPropertyValue(RequestResourceProvider.REQUEST_STATUS_PROPERTY_ID));
        Assert.assertEquals(1, resource.getPropertyValue(RequestResourceProvider.REQUEST_FAILED_TASK_CNT_ID));
        Assert.assertEquals(0, resource.getPropertyValue(RequestResourceProvider.REQUEST_ABORTED_TASK_CNT_ID));
        Assert.assertEquals(0, resource.getPropertyValue(RequestResourceProvider.REQUEST_TIMED_OUT_TASK_CNT_ID));
      } else {
        Assert.assertEquals("ABORTED", resource.getPropertyValue(RequestResourceProvider.REQUEST_STATUS_PROPERTY_ID));
        Assert.assertEquals(0, resource.getPropertyValue(RequestResourceProvider.REQUEST_FAILED_TASK_CNT_ID));
        Assert.assertEquals(1, resource.getPropertyValue(RequestResourceProvider.REQUEST_ABORTED_TASK_CNT_ID));
        Assert.assertEquals(1, resource.getPropertyValue(RequestResourceProvider.REQUEST_TIMED_OUT_TASK_CNT_ID));
      }
      Assert.assertEquals(2, resource.getPropertyValue(RequestResourceProvider.REQUEST_COMPLETED_TASK_CNT_ID));
      Assert.assertEquals(100.0, resource.getPropertyValue(RequestResourceProvider.REQUEST_PROGRESS_PERCENT_ID));
    }

    // verify
    verify(managementController, actionManager, hostRoleCommand0, hostRoleCommand1, hostRoleCommand2, hostRoleCommand3);
  }

  @Test
  public void testUpdateResources_CancelRequest() throws Exception {
    Resource.Type type = Resource.Type.Request;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    ActionManager actionManager = createNiceMock(ActionManager.class);
    HostRoleCommand hostRoleCommand = createNiceMock(HostRoleCommand.class);
    Clusters clusters = createNiceMock(Clusters.class);

    List<HostRoleCommand> hostRoleCommands = new LinkedList<HostRoleCommand>();
    hostRoleCommands.add(hostRoleCommand);

    org.apache.ambari.server.actionmanager.Request requestMock =
            createNiceMock(org.apache.ambari.server.actionmanager.Request.class);
    expect(requestMock.getCommands()).andReturn(hostRoleCommands).anyTimes();

    Capture<Collection<Long>> requestIdsCapture = new Capture<Collection<Long>>();

    // set expectations
    expect(managementController.getActionManager()).andReturn(actionManager).anyTimes();
    expect(actionManager.getRequests(capture(requestIdsCapture))).
            andReturn(Collections.singletonList(requestMock)).anyTimes();
    expect(hostRoleCommand.getStatus()).andReturn(HostRoleStatus.IN_PROGRESS).anyTimes();

    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    // replay
    replay(managementController, actionManager, hostRoleCommand, clusters, requestMock, response);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    // TEST CASE: Check update request validation (abort reason not specified)
    // add the property map to a set for the request.
    Map<String, Object> properties = new LinkedHashMap<String, Object>();
    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, null);
    Predicate predicate = new PredicateBuilder().property(RequestResourceProvider.REQUEST_ID_PROPERTY_ID).
        equals("100").toPredicate();

    try {
      provider.updateResources(request, predicate);
      Assert.fail("Expected an java.lang.IllegalArgumentException: Abort reason can not be empty.");
    } catch (IllegalArgumentException e) {
      // expected
    }

    // Add abort reason to previous request
    properties.put(RequestResourceProvider.REQUEST_ABORT_REASON_PROPERTY_ID, "Some reason");

    // TEST CASE: Check update request validation (new state is not specified)
    request = PropertyHelper.getUpdateRequest(properties, null);
    try {
      provider.updateResources(request, predicate);
      Assert.fail("Expected an java.lang.IllegalArgumentException: null is wrong value.");
    } catch (IllegalArgumentException e) {
      // expected
    }

    // TEST CASE: Check update request validation (new state is wrong)
    properties.put(RequestResourceProvider.REQUEST_STATUS_PROPERTY_ID, "COMPLETED");
    request = PropertyHelper.getUpdateRequest(properties, null);
    try {
      provider.updateResources(request, predicate);
      Assert.fail("Expected an java.lang.IllegalArgumentException: COMPLETED is wrong value. " +
              "The only allowed value for updating request status is ABORTED");
    } catch (IllegalArgumentException e) {
      // expected
    }

    // TEST CASE: Check update request validation (request is in wrong state)
    // Put valid request status
    properties.put(RequestResourceProvider.REQUEST_STATUS_PROPERTY_ID, "ABORTED");
    for (HostRoleStatus status : HostRoleStatus.values()) {
      reset(hostRoleCommand);
      expect(hostRoleCommand.getStatus()).andReturn(status).anyTimes();
      replay(hostRoleCommand);
      request = PropertyHelper.getUpdateRequest(properties, null);
      if (status == HostRoleStatus.IN_PROGRESS ||
              status == HostRoleStatus.PENDING ||
              status == HostRoleStatus.QUEUED) { // the only valid cases
        provider.updateResources(request, predicate);
      } else {  // In other cases, should error out
        try {
          provider.updateResources(request, predicate);
          Assert.fail("Expected an java.lang.IllegalArgumentException: null is wrong value.");
        } catch (IllegalArgumentException e) {
          // expected
        }
      }
    }
    // verify
    verify(managementController, response);
  }

  @Test
  public void testDeleteResources() throws Exception {
    Resource.Type type = Resource.Type.Request;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    // replay
    replay(managementController);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    Predicate predicate = new PredicateBuilder().property(RequestResourceProvider.REQUEST_ID_PROPERTY_ID).
        equals("Request100").toPredicate();
    try {
      provider.deleteResources(predicate);
      Assert.fail("Expected an UnsupportedOperationException");
    } catch (UnsupportedOperationException e) {
      // expected
    }

    // verify
    verify(managementController);
  }

  @Test
  public void testCreateResourcesForCommands() throws Exception {
    Resource.Type type = Resource.Type.Request;

    Capture<ExecuteActionRequest> actionRequest = new Capture<ExecuteActionRequest>();
    Capture<HashMap<String, String>> propertyMap = new Capture<HashMap<String, String>>();

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    expect(managementController.createAction(capture(actionRequest), capture(propertyMap)))
        .andReturn(response).anyTimes();
    expect(response.getMessage()).andReturn("Message").anyTimes();

    // replay
    replay(managementController, response);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<Map<String, Object>>();

    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    properties.put(RequestResourceProvider.REQUEST_CLUSTER_NAME_PROPERTY_ID, "c1");

    Set<Map<String, Object>> filterSet = new HashSet<Map<String, Object>>();
    Map<String, Object> filterMap = new HashMap<String, Object>();
    filterMap.put(RequestResourceProvider.SERVICE_ID, "HDFS");
    filterSet.add(filterMap);

    properties.put(RequestResourceProvider.REQUEST_RESOURCE_FILTER_ID, filterSet);

    propertySet.add(properties);

    Map<String, String> requestInfoProperties = new HashMap<String, String>();
    requestInfoProperties.put(RequestResourceProvider.COMMAND_ID, "HDFS_SERVICE_CHECK");



    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, requestInfoProperties);
    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    provider.createResources(request);
    ExecuteActionRequest capturedRequest = actionRequest.getValue();

    Assert.assertTrue(actionRequest.hasCaptured());
    Assert.assertTrue(capturedRequest.isCommand());
    Assert.assertEquals(null, capturedRequest.getActionName());
    Assert.assertEquals("HDFS_SERVICE_CHECK", capturedRequest.getCommandName());
    Assert.assertNotNull(capturedRequest.getResourceFilters());
    Assert.assertEquals(1, capturedRequest.getResourceFilters().size());
    RequestResourceFilter capturedResourceFilter = capturedRequest.getResourceFilters().get(0);
    Assert.assertEquals("HDFS", capturedResourceFilter.getServiceName());
    Assert.assertEquals(null, capturedResourceFilter.getComponentName());
    Assert.assertNotNull(capturedResourceFilter.getHostNames());
    Assert.assertEquals(0, capturedResourceFilter.getHostNames().size());
    Assert.assertEquals(0, actionRequest.getValue().getParameters().size());
  }

  @Test
  public void testCreateResourcesForCommandsWithParams() throws Exception {
    Resource.Type type = Resource.Type.Request;

    Capture<ExecuteActionRequest> actionRequest = new Capture<ExecuteActionRequest>();
    Capture<HashMap<String, String>> propertyMap = new Capture<HashMap<String, String>>();

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    expect(managementController.createAction(capture(actionRequest), capture(propertyMap)))
        .andReturn(response).anyTimes();
    expect(response.getMessage()).andReturn("Message").anyTimes();
    // replay
    replay(managementController, response);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<Map<String, Object>>();

    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    properties.put(RequestResourceProvider.REQUEST_CLUSTER_NAME_PROPERTY_ID, "c1");

    Set<Map<String, Object>> filterSet = new HashSet<Map<String, Object>>();
    Map<String, Object> filterMap = new HashMap<String, Object>();
    filterMap.put(RequestResourceProvider.SERVICE_ID, "HDFS");
    filterMap.put(RequestResourceProvider.HOSTS_ID, "host1,host2,host3");
    filterSet.add(filterMap);

    properties.put(RequestResourceProvider.REQUEST_RESOURCE_FILTER_ID, filterSet);

    propertySet.add(properties);

    Map<String, String> requestInfoProperties = new HashMap<String, String>();

    requestInfoProperties.put("parameters/param1", "value1");
    requestInfoProperties.put("parameters/param2", "value2");

    String[] expectedHosts = new String[]{"host1", "host2", "host3"};
    Map<String, String> expectedParams = new HashMap<String, String>() {{
      put("param1", "value1");
      put("param2", "value2");
    }};

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, requestInfoProperties);
    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    // Neither action nor commands are specified
    try {
      provider.createResources(request);
    } catch (UnsupportedOperationException ex) {
      Assert.assertTrue(ex.getMessage().contains("Either command or action must be specified"));
    }

    // Both action and command are specified
    requestInfoProperties.put(RequestResourceProvider.COMMAND_ID, "HDFS_SERVICE_CHECK");
    requestInfoProperties.put(RequestResourceProvider.ACTION_ID, "a1");
    try {
      provider.createResources(request);
    } catch (UnsupportedOperationException ex) {
      Assert.assertTrue(ex.getMessage().contains("Both command and action cannot be specified"));
    }
    requestInfoProperties.remove(RequestResourceProvider.ACTION_ID);

    provider.createResources(request);
    Assert.assertTrue(actionRequest.hasCaptured());
    ExecuteActionRequest capturedRequest = actionRequest.getValue();
    Assert.assertTrue(capturedRequest.isCommand());
    Assert.assertEquals(null, capturedRequest.getActionName());
    Assert.assertEquals("HDFS_SERVICE_CHECK", capturedRequest.getCommandName());
    Assert.assertEquals(1, capturedRequest.getResourceFilters().size());
    RequestResourceFilter capturedResourceFilter = capturedRequest.getResourceFilters().get(0);
    Assert.assertEquals("HDFS", capturedResourceFilter.getServiceName());
    Assert.assertEquals(null, capturedResourceFilter.getComponentName());
    Assert.assertEquals(3, capturedResourceFilter.getHostNames().size());
    Assert.assertArrayEquals(expectedHosts, capturedResourceFilter.getHostNames().toArray());
    Assert.assertEquals(2, capturedRequest.getParameters().size());
    for(String key : expectedParams.keySet()) {
      Assert.assertEquals(expectedParams.get(key), capturedRequest.getParameters().get(key));
    }
  }

  @Test
  public void testCreateResourcesForCommandsWithOpLvl() throws Exception {
    Resource.Type type = Resource.Type.Request;

    Capture<ExecuteActionRequest> actionRequest = new Capture<ExecuteActionRequest>();
    Capture<HashMap<String, String>> propertyMap = new Capture<HashMap<String, String>>();

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    expect(managementController.createAction(capture(actionRequest), capture(propertyMap)))
            .andReturn(response).anyTimes();
    expect(response.getMessage()).andReturn("Message").anyTimes();

    // replay
    replay(managementController, response);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<Map<String, Object>>();

    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    String c1 = "c1";
    String host_component = "HOST_COMPONENT";
    String service_id = "HDFS";
    String hostcomponent_id = "Namenode";
    String host_id = "host1";

    properties.put(RequestResourceProvider.REQUEST_CLUSTER_NAME_PROPERTY_ID, c1);

    Set<Map<String, Object>> filterSet = new HashSet<Map<String, Object>>();
    Map<String, Object> filterMap = new HashMap<String, Object>();
    filterMap.put(RequestResourceProvider.SERVICE_ID, service_id);
    filterMap.put(RequestResourceProvider.HOSTS_ID, host_id);
    filterSet.add(filterMap);

    properties.put(RequestResourceProvider.REQUEST_RESOURCE_FILTER_ID, filterSet);

    propertySet.add(properties);

    Map<String, String> requestInfoProperties = new HashMap<String, String>();
    requestInfoProperties.put(RequestResourceProvider.COMMAND_ID, "RESTART");

    requestInfoProperties.put(RequestOperationLevel.OPERATION_LEVEL_ID,
            host_component);
    requestInfoProperties.put(RequestOperationLevel.OPERATION_CLUSTER_ID, c1);
    requestInfoProperties.put(RequestOperationLevel.OPERATION_SERVICE_ID,
            service_id);
    requestInfoProperties.put(RequestOperationLevel.OPERATION_HOSTCOMPONENT_ID,
            hostcomponent_id);
    requestInfoProperties.put(RequestOperationLevel.OPERATION_HOST_ID,
            host_id);

    Request request = PropertyHelper.getCreateRequest(propertySet, requestInfoProperties);
    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
            type,
            PropertyHelper.getPropertyIds(type),
            PropertyHelper.getKeyPropertyIds(type),
            managementController);

    requestInfoProperties.put(RequestOperationLevel.OPERATION_CLUSTER_ID, c1);

    // create request in a normal way (positive scenario)
    provider.createResources(request);
    Assert.assertTrue(actionRequest.hasCaptured());
    ExecuteActionRequest capturedRequest = actionRequest.getValue();
    RequestOperationLevel level = capturedRequest.getOperationLevel();
    Assert.assertEquals(level.getLevel().toString(), "HostComponent");
    Assert.assertEquals(level.getClusterName(), c1);
    Assert.assertEquals(level.getServiceName(), service_id);
    Assert.assertEquals(level.getHostComponentName(), hostcomponent_id);
    Assert.assertEquals(level.getHostName(), host_id);
  }

  @Test
  public void testCreateResourcesForNonCluster() throws Exception {
    Resource.Type type = Resource.Type.Request;

    Capture<ExecuteActionRequest> actionRequest = new Capture<ExecuteActionRequest>();
    Capture<HashMap<String, String>> propertyMap = new Capture<HashMap<String, String>>();

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    expect(managementController.createAction(capture(actionRequest), capture(propertyMap)))
        .andReturn(response).anyTimes();
    expect(response.getMessage()).andReturn("Message").anyTimes();

    // replay
    replay(managementController, response);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<Map<String, Object>>();

    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    Set<Map<String, Object>> filterSet = new HashSet<Map<String, Object>>();
    Map<String, Object> filterMap = new HashMap<String, Object>();
    filterMap.put(RequestResourceProvider.HOSTS_ID, "h1,h2");
    filterSet.add(filterMap);

    properties.put(RequestResourceProvider.REQUEST_RESOURCE_FILTER_ID, filterSet);

    propertySet.add(properties);

    Map<String, String> requestInfoProperties = new HashMap<String, String>();
    requestInfoProperties.put(RequestResourceProvider.ACTION_ID, "check_java");

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, requestInfoProperties);
    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);
    provider.createResources(request);
    ExecuteActionRequest capturedRequest = actionRequest.getValue();

    Assert.assertTrue(actionRequest.hasCaptured());
    Assert.assertFalse("expected an action", Boolean.valueOf(capturedRequest.isCommand()));
    Assert.assertEquals("check_java", capturedRequest.getActionName());
    Assert.assertEquals(null, capturedRequest.getCommandName());
    Assert.assertNotNull(capturedRequest.getResourceFilters());
    Assert.assertEquals(1, capturedRequest.getResourceFilters().size());
    RequestResourceFilter capturedResourceFilter = capturedRequest.getResourceFilters().get(0);
    Assert.assertEquals(null, capturedResourceFilter.getServiceName());
    Assert.assertEquals(null, capturedResourceFilter.getComponentName());
    Assert.assertNotNull(capturedResourceFilter.getHostNames());
    Assert.assertEquals(2, capturedResourceFilter.getHostNames().size());
    Assert.assertEquals(0, actionRequest.getValue().getParameters().size());
    }
  
  @Test
  public void testGetResourcesWithoutCluster() throws Exception {
    Resource.Type type = Resource.Type.Request;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    ActionManager actionManager = createNiceMock(ActionManager.class);
    HostRoleCommand hostRoleCommand = createNiceMock(HostRoleCommand.class);
    Clusters clusters = createNiceMock(Clusters.class);

    List<HostRoleCommand> hostRoleCommands = new LinkedList<HostRoleCommand>();
    hostRoleCommands.add(hostRoleCommand);

    org.apache.ambari.server.actionmanager.Request requestMock =
        createNiceMock(org.apache.ambari.server.actionmanager.Request.class);
    expect(requestMock.getCommands()).andReturn(hostRoleCommands).anyTimes();
    expect(requestMock.getRequestContext()).andReturn("this is a context").anyTimes();
    expect(requestMock.getClusterName()).andReturn(null).anyTimes();
    expect(requestMock.getRequestId()).andReturn(100L).anyTimes();

    Capture<Collection<Long>> requestIdsCapture = new Capture<Collection<Long>>();

    // set expectations
    expect(managementController.getActionManager()).andReturn(actionManager).anyTimes();
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster(anyObject(String.class))).andReturn(null).anyTimes();
    expect(actionManager.getRequests(capture(requestIdsCapture))).andReturn(Collections.singletonList(requestMock));
    expect(hostRoleCommand.getRequestId()).andReturn(100L).anyTimes();
    expect(hostRoleCommand.getStatus()).andReturn(HostRoleStatus.IN_PROGRESS);

    // replay
    replay(managementController, actionManager, hostRoleCommand, clusters, requestMock);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    Set<String> propertyIds = new HashSet<String>();

    propertyIds.add(RequestResourceProvider.REQUEST_ID_PROPERTY_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_STATUS_PROPERTY_ID);

    Predicate predicate = new PredicateBuilder().
        property(RequestResourceProvider.REQUEST_ID_PROPERTY_ID).equals("100").
        toPredicate();
    Request request = PropertyHelper.getReadRequest(propertyIds);
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      Assert.assertEquals(100L, (long) (Long) resource.getPropertyValue(RequestResourceProvider.REQUEST_ID_PROPERTY_ID));
      Assert.assertEquals("IN_PROGRESS", resource.getPropertyValue(RequestResourceProvider.REQUEST_STATUS_PROPERTY_ID));
      Assert.assertNull(resource.getPropertyValue(RequestResourceProvider.REQUEST_CLUSTER_NAME_PROPERTY_ID));
    }

    // verify
    verify(managementController, actionManager, hostRoleCommand, clusters);    
  }

}
