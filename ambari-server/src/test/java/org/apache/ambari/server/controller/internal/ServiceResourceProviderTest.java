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

import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.ServiceRequest;
import org.apache.ambari.server.controller.ServiceResponse;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.easymock.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.verify;

/**
 * ServiceResourceProvider tests.
 */
public class ServiceResourceProviderTest {

  @Test
  public void testCreateResources() throws Exception{
    Resource.Type type = Resource.Type.Service;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    managementController.createServices(AbstractResourceProviderTest.Matcher.getServiceRequestSet("Cluster100", "Service100", null, "DEPLOYED"));

    // replay
    replay(managementController, response);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<Map<String, Object>>();

    // Service 1: create a map of properties for the request
    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    // add properties to the request map
    properties.put(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    properties.put(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID, "Service100");
    properties.put(ServiceResourceProvider.SERVICE_SERVICE_STATE_PROPERTY_ID, "DEPLOYED");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    provider.createResources(request);

    // verify
    verify(managementController, response);
  }

  @Test
  public void testGetResources() throws Exception{
    Resource.Type type = Resource.Type.Service;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    Set<ServiceResponse> allResponse = new HashSet<ServiceResponse>();
    allResponse.add(new ServiceResponse(100L, "Cluster100", "Service100", null, "HDP-0.1", "DEPLOYED"));
    allResponse.add(new ServiceResponse(100L, "Cluster100", "Service101", null, "HDP-0.1", "DEPLOYED"));
    allResponse.add(new ServiceResponse(100L, "Cluster100", "Service102", null, "HDP-0.1", "DEPLOYED"));
    allResponse.add(new ServiceResponse(100L, "Cluster100", "Service103", null, "HDP-0.1", "DEPLOYED"));
    allResponse.add(new ServiceResponse(100L, "Cluster100", "Service104", null, "HDP-0.1", "DEPLOYED"));

    Set<ServiceResponse> nameResponse = new HashSet<ServiceResponse>();
    nameResponse.add(new ServiceResponse(100L, "Cluster100", "Service102", null, "HDP-0.1", "DEPLOYED"));

    Set<ServiceResponse> stateResponse = new HashSet<ServiceResponse>();
    stateResponse.add(new ServiceResponse(100L, "Cluster100", "Service100", null, "HDP-0.1", "DEPLOYED"));
    stateResponse.add(new ServiceResponse(100L, "Cluster100", "Service102", null, "HDP-0.1", "DEPLOYED"));
    stateResponse.add(new ServiceResponse(100L, "Cluster100", "Service104", null, "HDP-0.1", "DEPLOYED"));

    // set expectations
    expect(managementController.getServices(EasyMock.<Set<ServiceRequest>>anyObject())).andReturn(allResponse).once();
    expect(managementController.getServices(EasyMock.<Set<ServiceRequest>>anyObject())).andReturn(nameResponse).once();
    expect(managementController.getServices(EasyMock.<Set<ServiceRequest>>anyObject())).andReturn(stateResponse).once();

    // replay
    replay(managementController);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    Set<String> propertyIds = new HashSet<String>();

    propertyIds.add(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID);
    propertyIds.add(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID);

    // create the request
    Request request = PropertyHelper.getReadRequest(propertyIds);
    // get all ... no predicate
    Set<Resource> resources = provider.getResources(request, null);

    Assert.assertEquals(5, resources.size());
    Set<String> names = new HashSet<String>();
    for (Resource resource : resources) {
      String clusterName = (String) resource.getPropertyValue(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID);
      Assert.assertEquals("Cluster100", clusterName);
      names.add((String) resource.getPropertyValue(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID));
    }
    // Make sure that all of the response objects got moved into resources
    for (ServiceResponse serviceResponse : allResponse ) {
      Assert.assertTrue(names.contains(serviceResponse.getServiceName()));
    }

    // get service named Service102
    Predicate predicate = new PredicateBuilder().property(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID).equals("Service102").toPredicate();
    request = PropertyHelper.getReadRequest("ServiceInfo");
    resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    Assert.assertEquals("Service102", resources.iterator().next().getPropertyValue(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID));

    // get services where state == "DEPLOYED"
    predicate = new PredicateBuilder().property(ServiceResourceProvider.SERVICE_SERVICE_STATE_PROPERTY_ID).equals("DEPLOYED").toPredicate();
    request = PropertyHelper.getReadRequest(propertyIds);
    resources = provider.getResources(request, predicate);

    Assert.assertEquals(3, resources.size());
    names = new HashSet<String>();
    for (Resource resource : resources) {
      String clusterName = (String) resource.getPropertyValue(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID);
      Assert.assertEquals("Cluster100", clusterName);
      names.add((String) resource.getPropertyValue(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID));
    }
    // Make sure that all of the response objects got moved into resources
    for (ServiceResponse serviceResponse : stateResponse ) {
      Assert.assertTrue(names.contains(serviceResponse.getServiceName()));
    }

    // verify
    verify(managementController);
  }

  @Test
  public void testUpdateResources() throws Exception{
    Resource.Type type = Resource.Type.Service;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    Capture<Set<ServiceRequest>> requestsCapture = new Capture<Set<ServiceRequest>>();

    Map<String, String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "Called from a test");

    // set expectations
    expect(managementController.updateServices(capture(requestsCapture),
      eq(mapRequestProps), eq(false), eq(true))).andReturn(response).once();

    // replay
    replay(managementController, response);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    // add the property map to a set for the request.
    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    properties.put(ServiceResourceProvider.SERVICE_SERVICE_STATE_PROPERTY_ID, "DEPLOYED");

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, mapRequestProps);

    // update the service named Service102
    Predicate  predicate = new PredicateBuilder().property(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").
        and().property(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID).equals("Service102").toPredicate();
    provider.updateResources(request, predicate);

    // verify
    verify(managementController, response);

    Set<ServiceRequest> setRequests = requestsCapture.getValue();
    assertEquals(1, setRequests.size());
    ServiceRequest sr = setRequests.iterator().next();
    assertEquals("Cluster100", sr.getClusterName());
    assertEquals("Service102", sr.getServiceName());
    assertEquals("DEPLOYED", sr.getDesiredState());
    assertNull(sr.getConfigVersions());
  }

  @Test
  public void testReconfigureClientsFlag() throws Exception {
    Resource.Type type = Resource.Type.Service;

    AmbariManagementController managementController1 = createMock(AmbariManagementController.class);
    AmbariManagementController managementController2 = createMock
      (AmbariManagementController.class);

    RequestStatusResponse response1 = createNiceMock(RequestStatusResponse.class);
    RequestStatusResponse response2 = createNiceMock(RequestStatusResponse
      .class);
    Capture<Set<ServiceRequest>> requestsCapture = new Capture<Set<ServiceRequest>>();

    Map<String, String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "Called from a test");

    Set<ServiceResponse> nameResponse = new HashSet<ServiceResponse>();
    nameResponse.add(new ServiceResponse(100L, "Cluster100", "Service102", null, "HDP-0.1", "DEPLOYED"));

    // set expectations
    expect(managementController1.getServices(EasyMock.<Set<ServiceRequest>>anyObject())).andReturn(nameResponse).once();
    expect(managementController2.getServices(EasyMock.<Set<ServiceRequest>>anyObject())).andReturn(nameResponse).once();

    // set expectations
    expect(managementController1.updateServices(capture(requestsCapture),
      eq(mapRequestProps), eq(false), eq(true))).andReturn(response1).once();

    expect(managementController2.updateServices(capture(requestsCapture),
      eq(mapRequestProps), eq(false), eq(false))).andReturn(response2).once();

    // replay
    replay(managementController1, response1);
    replay(managementController2, response2);

    ResourceProvider provider1 = AbstractControllerResourceProvider.getResourceProvider(
      type,
      PropertyHelper.getPropertyIds(type),
      PropertyHelper.getKeyPropertyIds(type),
      managementController1);

    ResourceProvider provider2 = AbstractControllerResourceProvider
      .getResourceProvider(
      type,
      PropertyHelper.getPropertyIds(type),
      PropertyHelper.getKeyPropertyIds(type),
      managementController2);

    // add the property map to a set for the request.
    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    properties.put(ServiceResourceProvider.SERVICE_SERVICE_STATE_PROPERTY_ID,
      "STARTED");

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, mapRequestProps);

    // update the service named Service102
    Predicate  predicate1 = new PredicateBuilder().property
      (ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").
      and().property(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID).
      equals("Service102").and().property("params/reconfigure_client").
      equals("true").toPredicate();

    Predicate  predicate2 = new PredicateBuilder().property
      (ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").
      and().property(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID).
      equals("Service102").and().property("params/reconfigure_client").equals
      ("false").toPredicate();

    provider1.updateResources(request, predicate1);
    provider2.updateResources(request, predicate2);

    // verify
    verify(managementController1, response1);
    verify(managementController2, response2);
  }

  @Test
  public void testDeleteResources() throws Exception{
    Resource.Type type = Resource.Type.Service;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    // set expectations
    expect(managementController.deleteServices(AbstractResourceProviderTest.Matcher.getServiceRequestSet(null, "Service100", null, null))).andReturn(response);

    // replay
    replay(managementController, response);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();

    ((ObservableResourceProvider)provider).addObserver(observer);

    // delete the service named Service100
    Predicate  predicate = new PredicateBuilder().property(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID).equals("Service100").toPredicate();
    provider.deleteResources(predicate);


    ResourceProviderEvent lastEvent = observer.getLastEvent();
    Assert.assertNotNull(lastEvent);
    Assert.assertEquals(Resource.Type.Service, lastEvent.getResourceType());
    Assert.assertEquals(ResourceProviderEvent.Type.Delete, lastEvent.getType());
    Assert.assertEquals(predicate, lastEvent.getPredicate());
    Assert.assertNull(lastEvent.getRequest());

    // verify
    verify(managementController, response);
  }

  @Test
  public void testCheckPropertyIds() throws Exception {
    Set<String> propertyIds = new HashSet<String>();
    propertyIds.add("foo");
    propertyIds.add("cat1/foo");
    propertyIds.add("cat2/bar");
    propertyIds.add("cat2/baz");
    propertyIds.add("cat3/sub1/bam");
    propertyIds.add("cat4/sub2/sub3/bat");
    propertyIds.add("cat5/subcat5/map");

    Map<Resource.Type, String> keyPropertyIds = new HashMap<Resource.Type, String>();

    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    AbstractResourceProvider provider =
        (AbstractResourceProvider) AbstractControllerResourceProvider.getResourceProvider(
            Resource.Type.Service,
            propertyIds,
            keyPropertyIds,
            managementController);

    Set<String> unsupported = provider.checkPropertyIds(Collections.singleton("foo"));
    Assert.assertTrue(unsupported.isEmpty());

    // note that key is not in the set of known property ids.  We allow it if its parent is a known property.
    // this allows for Map type properties where we want to treat the entries as individual properties
    Assert.assertTrue(provider.checkPropertyIds(Collections.singleton("cat5/subcat5/map/key")).isEmpty());

    unsupported = provider.checkPropertyIds(Collections.singleton("bar"));
    Assert.assertEquals(1, unsupported.size());
    Assert.assertTrue(unsupported.contains("bar"));

    unsupported = provider.checkPropertyIds(Collections.singleton("cat1/foo"));
    Assert.assertTrue(unsupported.isEmpty());

    unsupported = provider.checkPropertyIds(Collections.singleton("cat1"));
    Assert.assertTrue(unsupported.isEmpty());

    unsupported = provider.checkPropertyIds(Collections.singleton("config"));
    Assert.assertTrue(unsupported.isEmpty());

    unsupported = provider.checkPropertyIds(Collections.singleton("config/unknown_property"));
    Assert.assertTrue(unsupported.isEmpty());
  }
}
