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

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.ServiceComponentRequest;
import org.apache.ambari.server.controller.ServiceComponentResponse;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

/**
 * Tests for the component resource provider.
 */
public class ComponentResourceProviderTest {
  @Test
  public void testCreateResources() throws Exception {
    Resource.Type type = Resource.Type.Component;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    managementController.createComponents(
        AbstractResourceProviderTest.Matcher.getComponentRequestSet(
            "Cluster100", "Service100", "Component100", null, null));

    // replay
    replay(managementController, response);

    ResourceProvider provider = AbstractResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<Map<String, Object>>();

    // Service 1: create a map of properties for the request
    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    // add properties to the request map
    properties.put(ComponentResourceProvider.COMPONENT_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    properties.put(ComponentResourceProvider.COMPONENT_SERVICE_NAME_PROPERTY_ID, "Service100");
    properties.put(ComponentResourceProvider.COMPONENT_COMPONENT_NAME_PROPERTY_ID, "Component100");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet);

    provider.createResources(request);

    // verify
    verify(managementController, response);
  }

  @Test
  public void testGetResources() throws Exception {
    Resource.Type type = Resource.Type.Component;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    Set<ServiceComponentResponse> allResponse = new HashSet<ServiceComponentResponse>();
    allResponse.add(new ServiceComponentResponse(100L, "Cluster100", "Service100", "Component100", null, null, ""));
    allResponse.add(new ServiceComponentResponse(100L, "Cluster100", "Service100", "Component101", null, null, ""));
    allResponse.add(new ServiceComponentResponse(100L, "Cluster100", "Service100", "Component102", null, null, ""));

    // set expectations
    expect(managementController.getComponents(
        AbstractResourceProviderTest.Matcher.getComponentRequestSet(
            "Cluster100", null, null, null, null))).andReturn(allResponse).once();

    // replay
    replay(managementController);

    ResourceProvider provider = AbstractResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    Set<String> propertyIds = new HashSet<String>();

    propertyIds.add(ComponentResourceProvider.COMPONENT_CLUSTER_NAME_PROPERTY_ID);
    propertyIds.add(ComponentResourceProvider.COMPONENT_COMPONENT_NAME_PROPERTY_ID);

    Predicate predicate = new PredicateBuilder().property(ComponentResourceProvider.COMPONENT_CLUSTER_NAME_PROPERTY_ID).
        equals("Cluster100").toPredicate();
    Request request = PropertyHelper.getReadRequest(propertyIds);
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(3, resources.size());
    Set<String> names = new HashSet<String>();
    for (Resource resource : resources) {
      String clusterName = (String) resource.getPropertyValue(
          ComponentResourceProvider.COMPONENT_CLUSTER_NAME_PROPERTY_ID);
      Assert.assertEquals("Cluster100", clusterName);
      names.add((String) resource.getPropertyValue(ComponentResourceProvider.COMPONENT_COMPONENT_NAME_PROPERTY_ID));
    }
    // Make sure that all of the response objects got moved into resources
    for (ServiceComponentResponse response : allResponse ) {
      Assert.assertTrue(names.contains(response.getComponentName()));
    }

    // verify
    verify(managementController);
  }

  @Test
  public void testUpdateResources() throws Exception {
    Resource.Type type = Resource.Type.Component;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    Set<ServiceComponentResponse> nameResponse = new HashSet<ServiceComponentResponse>();
    nameResponse.add(new ServiceComponentResponse(102L, "Cluster102", "Service", "Component", null, "1", "STARTED"));

    // set expectations
    expect(managementController.getComponents(EasyMock.<Set<ServiceComponentRequest>>anyObject())).
        andReturn(nameResponse).once();
    expect(managementController.updateComponents(
        AbstractResourceProviderTest.Matcher.getComponentRequestSet(
            "Cluster102", "Service", "Component", null, "STARTED"))).andReturn(response).once();

    // replay
    replay(managementController, response);

    ResourceProvider provider = AbstractResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    properties.put(ComponentResourceProvider.COMPONENT_STATE_PROPERTY_ID, "STARTED");

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties);

    // update the cluster named Cluster102
    Predicate predicate = new PredicateBuilder().property(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID).
        equals("Cluster102").toPredicate();
    provider.updateResources(request, predicate);

    // verify
    verify(managementController, response);
  }

  @Test
  public void testDeleteResources() throws Exception {
    Resource.Type type = Resource.Type.Component;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    // set expectations
    expect(managementController.deleteComponents(AbstractResourceProviderTest.Matcher.
        getComponentRequestSet(null, null, "Component100", null, null))).andReturn(response);

    // replay
    replay(managementController, response);

    ResourceProvider provider = AbstractResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();

    ((ObservableResourceProvider)provider).addObserver(observer);

    Predicate  predicate = new PredicateBuilder().property(
        ComponentResourceProvider.COMPONENT_COMPONENT_NAME_PROPERTY_ID).equals("Component100").toPredicate();
    provider.deleteResources(predicate);


    ResourceProviderEvent lastEvent = observer.getLastEvent();
    Assert.assertNotNull(lastEvent);
    Assert.assertEquals(Resource.Type.Component, lastEvent.getResourceType());
    Assert.assertEquals(ResourceProviderEvent.Type.Delete, lastEvent.getType());
    Assert.assertEquals(predicate, lastEvent.getPredicate());
    Assert.assertNull(lastEvent.getRequest());

    // verify
    verify(managementController, response);
  }
}
