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

import org.apache.ambari.server.actionmanager.ActionManager;
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
import org.easymock.Capture;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

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
  public void testGetResources() throws Exception {
    Resource.Type type = Resource.Type.Request;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    ActionManager actionManager = createNiceMock(ActionManager.class);
    HostRoleCommand hostRoleCommand = createNiceMock(HostRoleCommand.class);

    List<HostRoleCommand> hostRoleCommands = new LinkedList<HostRoleCommand>();
    hostRoleCommands.add(hostRoleCommand);

    Capture<Collection<Long>> requestIdsCapture = new Capture<Collection<Long>>();
    Capture<List<Long>> requestIdListCapture = new Capture<List<Long>>();

    Map<Long, String> requestContexts  = new HashMap<Long, String>();
    requestContexts.put(100L, "this is a context");

    // set expectations
    expect(managementController.getActionManager()).andReturn(actionManager);
    expect(actionManager.getAllTasksByRequestIds(capture(requestIdsCapture))).andReturn(hostRoleCommands);
    expect(actionManager.getRequestContext(capture(requestIdListCapture))).andReturn(requestContexts);
    expect(hostRoleCommand.getRequestId()).andReturn(100L).anyTimes();
    expect(hostRoleCommand.getStatus()).andReturn(HostRoleStatus.IN_PROGRESS);

    // replay
    replay(managementController, actionManager, hostRoleCommand);

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
  public void testGetResourcesOrPredicate() throws Exception {
    Resource.Type type = Resource.Type.Request;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    ActionManager actionManager = createNiceMock(ActionManager.class);
    HostRoleCommand hostRoleCommand = createNiceMock(HostRoleCommand.class);

    List<HostRoleCommand> hostRoleCommands = new LinkedList<HostRoleCommand>();
    hostRoleCommands.add(hostRoleCommand);

    Capture<Collection<Long>> requestIdsCapture = new Capture<Collection<Long>>();
    Capture<List<Long>> requestIdListCapture = new Capture<List<Long>>();

    Map<Long, String> requestContexts  = new HashMap<Long, String>();
    requestContexts.put(100L, "this is a context");

    // set expectations
    expect(managementController.getActionManager()).andReturn(actionManager).anyTimes();
    expect(actionManager.getAllTasksByRequestIds(capture(requestIdsCapture))).andReturn(hostRoleCommands).anyTimes();
    expect(actionManager.getRequestContext(capture(requestIdListCapture))).andReturn(requestContexts).anyTimes();
    expect(hostRoleCommand.getRequestId()).andReturn(100L);
    expect(hostRoleCommand.getRequestId()).andReturn(101L);
    expect(hostRoleCommand.getStatus()).andReturn(HostRoleStatus.IN_PROGRESS).anyTimes();

    // replay
    replay(managementController, actionManager, hostRoleCommand);

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

    Capture<Collection<Long>> requestIdsCapture = new Capture<Collection<Long>>();
    Capture<List<Long>> requestIdListCapture = new Capture<List<Long>>();

    Map<Long, String> requestContexts0  = new HashMap<Long, String>();
    requestContexts0.put(100L, "this is a context");

    Map<Long, String> requestContexts1  = new HashMap<Long, String>();
    requestContexts1.put(101L, "this is a context");

    // set expectations
    expect(managementController.getActionManager()).andReturn(actionManager).anyTimes();
    expect(actionManager.getAllTasksByRequestIds(capture(requestIdsCapture))).andReturn(hostRoleCommands0);
    expect(actionManager.getAllTasksByRequestIds(capture(requestIdsCapture))).andReturn(hostRoleCommands1);
    expect(actionManager.getRequestContext(capture(requestIdListCapture))).andReturn(requestContexts0);
    expect(actionManager.getRequestContext(capture(requestIdListCapture))).andReturn(requestContexts1);
    expect(hostRoleCommand0.getRequestId()).andReturn(100L).anyTimes();
    expect(hostRoleCommand1.getRequestId()).andReturn(100L).anyTimes();
    expect(hostRoleCommand2.getRequestId()).andReturn(101L).anyTimes();
    expect(hostRoleCommand3.getRequestId()).andReturn(101L).anyTimes();
    expect(hostRoleCommand0.getStatus()).andReturn(HostRoleStatus.COMPLETED).anyTimes();
    expect(hostRoleCommand1.getStatus()).andReturn(HostRoleStatus.COMPLETED).anyTimes();
    expect(hostRoleCommand2.getStatus()).andReturn(HostRoleStatus.COMPLETED).anyTimes();
    expect(hostRoleCommand3.getStatus()).andReturn(HostRoleStatus.COMPLETED).anyTimes();

    // replay
    replay(managementController, actionManager, hostRoleCommand0, hostRoleCommand1, hostRoleCommand2, hostRoleCommand3);

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

    Capture<Collection<Long>> requestIdsCapture = new Capture<Collection<Long>>();
    Capture<List<Long>> requestIdListCapture = new Capture<List<Long>>();

    Map<Long, String> requestContexts0  = new HashMap<Long, String>();
    requestContexts0.put(100L, "this is a context");

    Map<Long, String> requestContexts1  = new HashMap<Long, String>();
    requestContexts1.put(101L, "this is a context");

    // set expectations
    expect(managementController.getActionManager()).andReturn(actionManager).anyTimes();
    expect(actionManager.getAllTasksByRequestIds(capture(requestIdsCapture))).andReturn(hostRoleCommands0);
    expect(actionManager.getAllTasksByRequestIds(capture(requestIdsCapture))).andReturn(hostRoleCommands1);
    expect(actionManager.getRequestContext(capture(requestIdListCapture))).andReturn(requestContexts0);
    expect(actionManager.getRequestContext(capture(requestIdListCapture))).andReturn(requestContexts1);
    expect(hostRoleCommand0.getRequestId()).andReturn(100L).anyTimes();
    expect(hostRoleCommand1.getRequestId()).andReturn(100L).anyTimes();
    expect(hostRoleCommand2.getRequestId()).andReturn(101L).anyTimes();
    expect(hostRoleCommand3.getRequestId()).andReturn(101L).anyTimes();
    expect(hostRoleCommand0.getStatus()).andReturn(HostRoleStatus.IN_PROGRESS).anyTimes();
    expect(hostRoleCommand1.getStatus()).andReturn(HostRoleStatus.COMPLETED).anyTimes();
    expect(hostRoleCommand2.getStatus()).andReturn(HostRoleStatus.IN_PROGRESS).anyTimes();
    expect(hostRoleCommand3.getStatus()).andReturn(HostRoleStatus.QUEUED).anyTimes();

    // replay
    replay(managementController, actionManager, hostRoleCommand0, hostRoleCommand1, hostRoleCommand2, hostRoleCommand3);

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
      Assert.assertEquals("IN_PROGRESS", resource.getPropertyValue(RequestResourceProvider.REQUEST_STATUS_PROPERTY_ID));
      Assert.assertEquals(2, resource.getPropertyValue(RequestResourceProvider.REQUEST_TASK_CNT_ID));
      Assert.assertEquals(0, resource.getPropertyValue(RequestResourceProvider.REQUEST_FAILED_TASK_CNT_ID));

      if (id == 100L) {
        Assert.assertEquals(1, resource.getPropertyValue(RequestResourceProvider.REQUEST_COMPLETED_TASK_CNT_ID));
        Assert.assertEquals(67.5, resource.getPropertyValue(RequestResourceProvider.REQUEST_PROGRESS_PERCENT_ID));
      } else {
        Assert.assertEquals(0, resource.getPropertyValue(RequestResourceProvider.REQUEST_COMPLETED_TASK_CNT_ID));
        Assert.assertEquals(21.999999999999996, resource.getPropertyValue(RequestResourceProvider.REQUEST_PROGRESS_PERCENT_ID));
      }
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

    Capture<Collection<Long>> requestIdsCapture = new Capture<Collection<Long>>();
    Capture<List<Long>> requestIdListCapture = new Capture<List<Long>>();

    Map<Long, String> requestContexts0  = new HashMap<Long, String>();
    requestContexts0.put(100L, "this is a context");

    Map<Long, String> requestContexts1  = new HashMap<Long, String>();
    requestContexts1.put(101L, "this is a context");

    // set expectations
    expect(managementController.getActionManager()).andReturn(actionManager).anyTimes();
    expect(actionManager.getAllTasksByRequestIds(capture(requestIdsCapture))).andReturn(hostRoleCommands0);
    expect(actionManager.getAllTasksByRequestIds(capture(requestIdsCapture))).andReturn(hostRoleCommands1);
    expect(actionManager.getRequestContext(capture(requestIdListCapture))).andReturn(requestContexts0);
    expect(actionManager.getRequestContext(capture(requestIdListCapture))).andReturn(requestContexts1);
    expect(hostRoleCommand0.getRequestId()).andReturn(100L).anyTimes();
    expect(hostRoleCommand1.getRequestId()).andReturn(100L).anyTimes();
    expect(hostRoleCommand2.getRequestId()).andReturn(101L).anyTimes();
    expect(hostRoleCommand3.getRequestId()).andReturn(101L).anyTimes();
    expect(hostRoleCommand0.getStatus()).andReturn(HostRoleStatus.FAILED).anyTimes();
    expect(hostRoleCommand1.getStatus()).andReturn(HostRoleStatus.COMPLETED).anyTimes();
    expect(hostRoleCommand2.getStatus()).andReturn(HostRoleStatus.ABORTED).anyTimes();
    expect(hostRoleCommand3.getStatus()).andReturn(HostRoleStatus.TIMEDOUT).anyTimes();

    // replay
    replay(managementController, actionManager, hostRoleCommand0, hostRoleCommand1, hostRoleCommand2, hostRoleCommand3);

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
  public void testUpdateResources() throws Exception {
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

    // add the property map to a set for the request.
    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, null);

    Predicate predicate = new PredicateBuilder().property(RequestResourceProvider.REQUEST_ID_PROPERTY_ID).
        equals("Request100").toPredicate();

    try {
      provider.updateResources(request, predicate);
      Assert.fail("Expected an UnsupportedOperationException");
    } catch (UnsupportedOperationException e) {
      // expected
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

//  public static RequestResourceProvider getServiceProvider(AmbariManagementController managementController) {
//    Resource.Type type = Resource.Type.Request;
//
//    return (RequestResourceProvider) AbstractControllerResourceProvider.getResourceProvider(
//        type,
//        PropertyHelper.getPropertyIds(type),
//        PropertyHelper.getKeyPropertyIds(type),
//        managementController);
//  }
//
//  public static Set<RequestStatusResponse> getRequestStatus(
//      AmbariManagementController controller, RequestStatusRequest request)
//      throws AmbariException {
//    RequestResourceProvider provider = getServiceProvider(controller);
//    return provider.getRequestStatus(request);
//  }
}
