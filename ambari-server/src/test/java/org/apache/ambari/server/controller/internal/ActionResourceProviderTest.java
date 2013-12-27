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


package org.apache.ambari.server.controller.internal;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.ActionDefinition;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.ActionType;
import org.apache.ambari.server.actionmanager.TargetHostType;
import org.apache.ambari.server.controller.ActionResponse;
import org.apache.ambari.server.controller.ActionRequest;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

public class ActionResourceProviderTest {

  private Injector injector;

  @Before
  public void setup() throws Exception {
    InMemoryDefaultTestModule module = new InMemoryDefaultTestModule();
    injector = Guice.createInjector(module);
    injector.getInstance(GuiceJpaInitializer.class);
  }

  @After
  public void teardown() {
    injector.getInstance(PersistService.class).stop();
  }

  @Test
  public void testGetResources() throws Exception {
    Resource.Type type = Resource.Type.Action;
    ActionManager am = createNiceMock(ActionManager.class);
    AmbariManagementController managementController = createNiceMock(AmbariManagementController.class);
    expect(managementController.getActionManager()).andReturn(am).anyTimes();

    List<ActionDefinition> allDefinition = new ArrayList<ActionDefinition>();
    allDefinition.add(new ActionDefinition(
        "a1", ActionType.SYSTEM, "fileName", "HDFS", "DATANODE", "Does file exist", TargetHostType.ANY,
        Short.valueOf("100")));
    allDefinition.add(new ActionDefinition(
        "a2", ActionType.SYSTEM, "fileName", "HDFS", "DATANODE", "Does file exist", TargetHostType.ANY,
        Short.valueOf("100")));
    allDefinition.add(new ActionDefinition(
        "a3", ActionType.SYSTEM, "fileName", "HDFS", "DATANODE", "Does file exist", TargetHostType.ANY,
        Short.valueOf("100")));

    Set<ActionResponse> allResponse = new HashSet<ActionResponse>();
    for (ActionDefinition definition : allDefinition) {
      allResponse.add(new ActionResponse(definition));
    }

    ActionDefinition namedDefinition = new ActionDefinition(
        "a1", ActionType.SYSTEM, "fileName", "HDFS", "DATANODE", "Does file exist", TargetHostType.ANY,
        Short.valueOf("100"));

    Set<ActionResponse> nameResponse = new HashSet<ActionResponse>();
    nameResponse.add(new ActionResponse(namedDefinition));

    expect(am.getAllActionDefinition()).andReturn(allDefinition).once();
    expect(am.getActionDefinition("a1")).andReturn(namedDefinition).once();

    replay(managementController, am);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    Set<String> propertyIds = new HashSet<String>();

    propertyIds.add(ActionResourceProvider.ACTION_NAME_PROPERTY_ID);
    propertyIds.add(ActionResourceProvider.ACTION_TYPE_PROPERTY_ID);
    propertyIds.add(ActionResourceProvider.DEFAULT_TIMEOUT_PROPERTY_ID);
    propertyIds.add(ActionResourceProvider.DESCRIPTION_PROPERTY_ID);
    propertyIds.add(ActionResourceProvider.INPUTS_PROPERTY_ID);
    propertyIds.add(ActionResourceProvider.TARGET_COMPONENT_PROPERTY_ID);
    propertyIds.add(ActionResourceProvider.TARGET_HOST_PROPERTY_ID);
    propertyIds.add(ActionResourceProvider.TARGET_SERVICE_PROPERTY_ID);


    // create the request
    Request request = PropertyHelper.getReadRequest(propertyIds);

    // get all ... no predicate
    Set<Resource> resources = provider.getResources(request, null);

    Assert.assertEquals(allResponse.size(), resources.size());
    for (Resource resource : resources) {
      String actionName = (String) resource.getPropertyValue(ActionResourceProvider.ACTION_NAME_PROPERTY_ID);
      String actionType = (String) resource.getPropertyValue(ActionResourceProvider.ACTION_TYPE_PROPERTY_ID);
      String defaultTimeout = (String) resource.getPropertyValue(ActionResourceProvider.DEFAULT_TIMEOUT_PROPERTY_ID);
      String description = (String) resource.getPropertyValue(ActionResourceProvider.DESCRIPTION_PROPERTY_ID);
      String inputs = (String) resource.getPropertyValue(ActionResourceProvider.INPUTS_PROPERTY_ID);
      String comp = (String) resource.getPropertyValue(ActionResourceProvider.TARGET_COMPONENT_PROPERTY_ID);
      String svc = (String) resource.getPropertyValue(ActionResourceProvider.TARGET_SERVICE_PROPERTY_ID);
      String host = (String) resource.getPropertyValue(ActionResourceProvider.TARGET_HOST_PROPERTY_ID);
      Assert.assertTrue(allResponse.contains(new ActionResponse(actionName, actionType,
          inputs, svc, comp, description, host, defaultTimeout)));
    }

    // get actions named a1
    Predicate predicate =
        new PredicateBuilder().property(ActionResourceProvider.ACTION_NAME_PROPERTY_ID).
            equals("a1").toPredicate();
    resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    Assert.assertEquals("a1", resources.iterator().next().
        getPropertyValue(ActionResourceProvider.ACTION_NAME_PROPERTY_ID));


    // verify
    verify(managementController);
  }

  @Test
  public void testCreateResources() throws Exception {
    Resource.Type type = Resource.Type.Action;

    AmbariManagementController managementController = createNiceMock(AmbariManagementController.class);
    ActionManager am = createMock(ActionManager.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    expect(managementController.getActionManager()).andReturn(am).anyTimes();

    am.createActionDefinition(eq("a1"), eq(ActionType.SYSTEM), eq("fileName"), eq("desc"), eq((String)null),
        eq((String)null), eq(TargetHostType.ANY), eq(Short.valueOf("60")));
    // replay
    replay(managementController, am, response);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);
    ((ActionResourceProvider) provider).setEnableExperimental(true);

    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();

    ((ObservableResourceProvider) provider).addObserver(observer);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<Map<String, Object>>();

    // Cluster 1: create a map of properties for the request
    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    properties.put(ActionResourceProvider.ACTION_NAME_PROPERTY_ID, "a1");
    properties.put(ActionResourceProvider.ACTION_TYPE_PROPERTY_ID, "SYSTEM");
    properties.put(ActionResourceProvider.TARGET_HOST_PROPERTY_ID, "ANY");
    properties.put(ActionResourceProvider.DESCRIPTION_PROPERTY_ID, "desc");
    properties.put(ActionResourceProvider.INPUTS_PROPERTY_ID, "fileName");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    provider.createResources(request);

    ResourceProviderEvent lastEvent = observer.getLastEvent();
    Assert.assertNotNull(lastEvent);
    Assert.assertEquals(Resource.Type.Action, lastEvent.getResourceType());
    Assert.assertEquals(ResourceProviderEvent.Type.Create, lastEvent.getType());
    Assert.assertEquals(request, lastEvent.getRequest());
    Assert.assertNull(lastEvent.getPredicate());

    // verify
    verify(managementController, response);
  }

  @Test
  public void testUpdateResources() throws Exception {
    Resource.Type type = Resource.Type.Action;

    AmbariManagementController managementController = createNiceMock(AmbariManagementController.class);
    ActionManager am = createMock(ActionManager.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    expect(managementController.getActionManager()).andReturn(am).anyTimes();
    am.updateActionDefinition(eq("a2"), eq((ActionType) null), eq("Updated description"),
        eq((TargetHostType) null), eq((Short)null));

    Map<String, String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "Called from a test");

    // replay
    replay(managementController, response, am);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);
    ((ActionResourceProvider) provider).setEnableExperimental(true);

    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    properties.put(ActionResourceProvider.DESCRIPTION_PROPERTY_ID, "Updated description");

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, mapRequestProps);

    // update the action named a2
    Predicate predicate = new PredicateBuilder().property(
        ActionResourceProvider.ACTION_NAME_PROPERTY_ID).equals("a2").toPredicate();
    provider.updateResources(request, predicate);

    // verify
    verify(managementController, response, am);
  }

  @Test
  public void testEnsureLockedOperations() throws Exception {
    Resource.Type type = Resource.Type.Action;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    Map<String, Object> properties = new LinkedHashMap<String, Object>();
    Map<String, String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "Called from a test");
    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, mapRequestProps);

    Predicate predicate = new PredicateBuilder().property(
        ActionResourceProvider.ACTION_NAME_PROPERTY_ID).equals("a2").toPredicate();
    try {
      provider.updateResources(request, predicate);
      Assert.fail("Update call must fail.");
    } catch (UnsupportedOperationException ex) {
      Assert.assertTrue(ex.getMessage().contains("Not currently supported"));
    }

    try {
      provider.createResources(request);
      Assert.fail("Create call must fail.");
    } catch (UnsupportedOperationException ex) {
      Assert.assertTrue(ex.getMessage().contains("Not currently supported"));
    }

    try {
      provider.deleteResources(predicate);
      Assert.fail("Delete call must fail.");
    } catch (UnsupportedOperationException ex) {
      Assert.assertTrue(ex.getMessage().contains("Not currently supported"));
    }
  }

  public static ActionResourceProvider getActionDefinitionResourceProvider(
      AmbariManagementController managementController) {
    Resource.Type type = Resource.Type.Action;

    return (ActionResourceProvider) AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);
  }

  public static void createAction(AmbariManagementController controller, ActionRequest request)
      throws AmbariException {
    ActionResourceProvider provider = getActionDefinitionResourceProvider(controller);
    provider.createActionDefinition(request);
  }

  public static Set<ActionResponse> getActions(AmbariManagementController controller,
                                                         Set<ActionRequest> requests)
      throws AmbariException {
    ActionResourceProvider provider = getActionDefinitionResourceProvider(controller);
    return provider.getActionDefinitions(requests);
  }

  public static RequestStatusResponse updateAction(AmbariManagementController controller,
                                                   Set<ActionRequest> requests,
                                                   Map<String, String> requestProperties)
      throws AmbariException {
    ActionResourceProvider provider = getActionDefinitionResourceProvider(controller);
    return provider.updateActionDefinitions(requests, requestProperties);
  }

  public static void deleteAction(AmbariManagementController controller, ActionRequest request)
      throws AmbariException {
    ActionResourceProvider provider = getActionDefinitionResourceProvider(controller);
    provider.deleteActionDefinition(request);
  }
}
