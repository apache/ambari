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

import static org.easymock.EasyMock.anyBoolean;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.ServiceComponentNotFoundException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariManagementControllerImpl;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.ServiceComponentRequest;
import org.apache.ambari.server.controller.ServiceComponentResponse;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentFactory;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.easymock.Capture;
import org.junit.Assert;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.inject.Injector;

/**
 * Tests for the component resource provider.
 */
public class ComponentResourceProviderTest {
  @Test
  public void testCreateResources() throws Exception {
    Resource.Type type = Resource.Type.Component;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Service service = createNiceMock(Service.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    ServiceComponentFactory serviceComponentFactory = createNiceMock(ServiceComponentFactory.class);
    ServiceComponent serviceComponent = createNiceMock(ServiceComponent.class);
    StackId stackId = createNiceMock(StackId.class);

    expect(managementController.getClusters()).andReturn(clusters);
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo);
    expect(managementController.getServiceComponentFactory()).andReturn(serviceComponentFactory);

    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();
    expect(cluster.getService("Service100")).andReturn(service).anyTimes();

    expect(service.getDesiredStackVersion()).andReturn(stackId).anyTimes();
    expect(service.getName()).andReturn("Service100").anyTimes();

    expect(stackId.getStackName()).andReturn("HDP").anyTimes();
    expect(stackId.getStackVersion()).andReturn("99").anyTimes();

    expect(ambariMetaInfo.isValidServiceComponent("HDP", "99", "Service100", "Component100")).andReturn(true).anyTimes();

    expect(serviceComponentFactory.createNew(service, "Component100")).andReturn(serviceComponent);

    // replay
    replay(managementController, response, clusters, cluster, service, stackId, ambariMetaInfo,
        serviceComponentFactory, serviceComponent);

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
    properties.put(ComponentResourceProvider.COMPONENT_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    properties.put(ComponentResourceProvider.COMPONENT_SERVICE_NAME_PROPERTY_ID, "Service100");
    properties.put(ComponentResourceProvider.COMPONENT_COMPONENT_NAME_PROPERTY_ID, "Component100");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    provider.createResources(request);

    // verify
    verify(managementController, response, clusters, cluster, service, stackId, ambariMetaInfo,
        serviceComponentFactory, serviceComponent);
  }

  @Test
  public void testGetResources() throws Exception {
    Resource.Type type = Resource.Type.Component;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    Service service = createNiceMock(Service.class);
    ServiceComponent serviceComponent1 = createNiceMock(ServiceComponent.class);
    ServiceComponent serviceComponent2 = createNiceMock(ServiceComponent.class);
    ServiceComponent serviceComponent3 = createNiceMock(ServiceComponent.class);
    StackId stackId = createNiceMock(StackId.class);
    ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);


    Map<String, ServiceComponent> serviceComponentMap = new HashMap<String, ServiceComponent>();
    serviceComponentMap.put("Component101", serviceComponent1);
    serviceComponentMap.put("Component102", serviceComponent2);
    serviceComponentMap.put("Component103", serviceComponent3);

    // set expectations
    expect(managementController.getClusters()).andReturn(clusters);
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo);
    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();
    expect(cluster.getDesiredStackVersion()).andReturn(stackId);

    expect(cluster.getServices()).andReturn(Collections.singletonMap("Service100", service)).anyTimes();

    expect(service.getServiceComponents()).andReturn(serviceComponentMap).anyTimes();

    expect(serviceComponent1.convertToResponse()).andReturn(new ServiceComponentResponse(100L, "Cluster100", "Service100", "Component100", null, ""));
    expect(serviceComponent2.convertToResponse()).andReturn(new ServiceComponentResponse(100L, "Cluster100", "Service100", "Component101", null, ""));
    expect(serviceComponent3.convertToResponse()).andReturn(new ServiceComponentResponse(100L, "Cluster100", "Service100", "Component102", null, ""));


    expect(ambariMetaInfo.getComponentCategory((String) anyObject(), (String) anyObject(),
        (String) anyObject(), (String) anyObject())).andReturn(componentInfo).anyTimes();

    expect(componentInfo.getCategory()).andReturn("MASTER").anyTimes();

    // replay
    replay(managementController, clusters, cluster, ambariMetaInfo, service,
        serviceComponent1, serviceComponent2, serviceComponent3, stackId, componentInfo);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    Set<String> propertyIds = new HashSet<String>();

    propertyIds.add(ComponentResourceProvider.COMPONENT_CLUSTER_NAME_PROPERTY_ID);
    propertyIds.add(ComponentResourceProvider.COMPONENT_COMPONENT_NAME_PROPERTY_ID);
    propertyIds.add(ComponentResourceProvider.COMPONENT_CATEGORY_PROPERTY_ID);

    Predicate predicate = new PredicateBuilder().property(ComponentResourceProvider.COMPONENT_CLUSTER_NAME_PROPERTY_ID).
        equals("Cluster100").toPredicate();
    Request request = PropertyHelper.getReadRequest(propertyIds);
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(3, resources.size());
    for (Resource resource : resources) {
      String clusterName = (String) resource.getPropertyValue(
          ComponentResourceProvider.COMPONENT_CLUSTER_NAME_PROPERTY_ID);
      Assert.assertEquals("Cluster100", clusterName);
      Assert.assertEquals("MASTER", resource.getPropertyValue(
          ComponentResourceProvider.COMPONENT_CATEGORY_PROPERTY_ID));
    }

    // verify
    verify(managementController, clusters, cluster, ambariMetaInfo, service,
        serviceComponent1, serviceComponent2, serviceComponent3, stackId, componentInfo);
  }

  @Test
  public void testUpdateResources() throws Exception {
    Resource.Type type = Resource.Type.Component;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    Service service = createNiceMock(Service.class);
    ServiceComponent serviceComponent1 = createNiceMock(ServiceComponent.class);
    ServiceComponent serviceComponent2 = createNiceMock(ServiceComponent.class);
    ServiceComponent serviceComponent3 = createNiceMock(ServiceComponent.class);
    ServiceComponentHost serviceComponentHost = createNiceMock(ServiceComponentHost.class);
    RequestStatusResponse requestStatusResponse = createNiceMock(RequestStatusResponse.class);
    StackId stackId = createNiceMock(StackId.class);

    Map<String, ServiceComponent> serviceComponentMap = new HashMap<String, ServiceComponent>();
    serviceComponentMap.put("Component101", serviceComponent1);
    serviceComponentMap.put("Component102", serviceComponent2);
    serviceComponentMap.put("Component103", serviceComponent3);

    // set expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();
    expect(cluster.getDesiredStackVersion()).andReturn(stackId);

    expect(cluster.getService("Service100")).andReturn(service).anyTimes();
    expect(service.getServiceComponent("Component101")).andReturn(serviceComponent1).anyTimes();
    expect(service.getServiceComponent("Component102")).andReturn(serviceComponent1).anyTimes();
    expect(service.getServiceComponent("Component103")).andReturn(serviceComponent2).anyTimes();


    expect(cluster.getServices()).andReturn(Collections.singletonMap("Service100", service)).anyTimes();

    expect(service.getServiceComponents()).andReturn(serviceComponentMap).anyTimes();

    expect(serviceComponent1.convertToResponse()).andReturn(new ServiceComponentResponse(100L, "Cluster100", "Service100", "Component101", null, ""));
    expect(serviceComponent2.convertToResponse()).andReturn(new ServiceComponentResponse(100L, "Cluster100", "Service100", "Component102", null, ""));
    expect(serviceComponent3.convertToResponse()).andReturn(new ServiceComponentResponse(100L, "Cluster100", "Service100", "Component103", null, ""));
    expect(serviceComponent1.getDesiredState()).andReturn(State.INSTALLED).anyTimes();
    expect(serviceComponent2.getDesiredState()).andReturn(State.INSTALLED).anyTimes();
    expect(serviceComponent3.getDesiredState()).andReturn(State.INSTALLED).anyTimes();

    expect(serviceComponentHost.getState()).andReturn(State.INSTALLED).anyTimes();

    Map<String, ServiceComponentHost> serviceComponentHosts = Collections.singletonMap("Host100", serviceComponentHost);

    expect(serviceComponent1.getServiceComponentHosts()).andReturn(serviceComponentHosts).anyTimes();
    expect(serviceComponent2.getServiceComponentHosts()).andReturn(serviceComponentHosts).anyTimes();
    expect(serviceComponent3.getServiceComponentHosts()).andReturn(serviceComponentHosts).anyTimes();


    Capture<Map<String, String>> requestPropertiesCapture = new Capture<Map<String, String>>();
    Capture<Map<State, List<Service>>> changedServicesCapture = new Capture<Map<State, List<Service>>>();
    Capture<Map<State, List<ServiceComponent>>> changedCompsCapture = new Capture<Map<State, List<ServiceComponent>>>();
    Capture<Map<String, Map<State, List<ServiceComponentHost>>>> changedScHostsCapture = new Capture<Map<String, Map<State, List<ServiceComponentHost>>>>();
    Capture<Map<String, String>> requestParametersCapture = new Capture<Map<String, String>>();
    Capture<Collection<ServiceComponentHost>> ignoredScHostsCapture = new Capture<Collection<ServiceComponentHost>>();
    Capture<Cluster> clusterCapture = new Capture<Cluster>();

    expect(managementController.createStages(capture(clusterCapture), capture(requestPropertiesCapture), capture(requestParametersCapture), capture(changedServicesCapture), capture(changedCompsCapture), capture(changedScHostsCapture), capture(ignoredScHostsCapture), anyBoolean(), anyBoolean()
    )).andReturn(requestStatusResponse);



    Map<String, String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "Called from a test");


    // replay
    replay(managementController, clusters, cluster, ambariMetaInfo, service,
        serviceComponent1, serviceComponent2, serviceComponent3, serviceComponentHost,
        requestStatusResponse, stackId);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    properties.put(ComponentResourceProvider.COMPONENT_STATE_PROPERTY_ID, "STARTED");
    properties.put(ComponentResourceProvider.COMPONENT_CLUSTER_NAME_PROPERTY_ID, "Cluster100");

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, mapRequestProps);

    // update the cluster named Cluster102
    Predicate predicate = new PredicateBuilder().property(ComponentResourceProvider.COMPONENT_CLUSTER_NAME_PROPERTY_ID).
        equals("Cluster100").toPredicate();
    provider.updateResources(request, predicate);

    // verify
    verify(managementController, clusters, cluster, ambariMetaInfo, service,
        serviceComponent1, serviceComponent2, serviceComponent3, serviceComponentHost,
        requestStatusResponse, stackId);
  }

  @Test
  public void testDeleteResources() throws Exception {
    Resource.Type type = Resource.Type.Component;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    // replay
    replay(managementController);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();

    ((ObservableResourceProvider)provider).addObserver(observer);

    Predicate  predicate = new PredicateBuilder().property(
        ComponentResourceProvider.COMPONENT_COMPONENT_NAME_PROPERTY_ID).equals("Component100").toPredicate();
    try {
      provider.deleteResources(predicate);
      Assert.fail("Expected exception.");
    } catch (Exception e) {
      // expected
    }

    // verify
    verify(managementController);
  }

  @Test
  public void testGetComponents() throws Exception {
    // member state mocks
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    StackId stackId = createNiceMock(StackId.class);

    Cluster cluster = createNiceMock(Cluster.class);
    Service service = createNiceMock(Service.class);
    ServiceComponent component = createNiceMock(ServiceComponent.class);
    ServiceComponentResponse response = createNiceMock(ServiceComponentResponse.class);

    // requests
    ServiceComponentRequest request1 = new ServiceComponentRequest("cluster1", "service1", "component1",
        null);

    Set<ServiceComponentRequest> setRequests = new HashSet<ServiceComponentRequest>();
    setRequests.add(request1);

    // expectations
    // constructor init
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();

    // getComponents
    expect(clusters.getCluster("cluster1")).andReturn(cluster);
    expect(cluster.getService("service1")).andReturn(service);
    expect(cluster.getDesiredStackVersion()).andReturn(stackId).anyTimes();
    expect(service.getServiceComponent("component1")).andReturn(component);

    expect(component.convertToResponse()).andReturn(response);
    // replay mocks
    replay(clusters, cluster, service, component, response, ambariMetaInfo, stackId, managementController);

    //test
//    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);
    Set<ServiceComponentResponse> setResponses = getComponentResourceProvider(managementController).getComponents(setRequests);

    // assert and verify
    assertEquals(1, setResponses.size());
    assertTrue(setResponses.contains(response));

    verify(clusters, cluster, service, component, response, ambariMetaInfo, stackId, managementController);
  }

  /**
   * Ensure that ServiceComponentNotFoundException is handled where there are multiple requests as would be the
   * case when an OR predicate is provided in the query.
   */
  @Test
  public void testGetComponents___OR_Predicate_ServiceComponentNotFoundException() throws Exception {
    // member state mocks
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    StackId stackId = createNiceMock(StackId.class);

    Cluster cluster = createNiceMock(Cluster.class);
    Service service = createNiceMock(Service.class);
    ServiceComponent component1 = createNiceMock(ServiceComponent.class);
    ServiceComponent component2 = createNiceMock(ServiceComponent.class);
    ServiceComponentResponse response1 = createNiceMock(ServiceComponentResponse.class);
    ServiceComponentResponse response2 = createNiceMock(ServiceComponentResponse.class);

    // requests
    ServiceComponentRequest request1 = new ServiceComponentRequest("cluster1", "service1", "component1",
        null);
    ServiceComponentRequest request2 = new ServiceComponentRequest("cluster1", "service1", "component2",
        null);
    ServiceComponentRequest request3 = new ServiceComponentRequest("cluster1", "service1", "component3",
        null);
    ServiceComponentRequest request4 = new ServiceComponentRequest("cluster1", "service1", "component4",
        null);
    ServiceComponentRequest request5 = new ServiceComponentRequest("cluster1", "service2", null, null);

    Set<ServiceComponentRequest> setRequests = new HashSet<ServiceComponentRequest>();
    setRequests.add(request1);
    setRequests.add(request2);
    setRequests.add(request3);
    setRequests.add(request4);
    setRequests.add(request5);

    // expectations
    // constructor init
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();

    // getComponents
    expect(clusters.getCluster("cluster1")).andReturn(cluster).times(5);
    expect(cluster.getDesiredStackVersion()).andReturn(stackId).anyTimes();
    expect(cluster.getService("service1")).andReturn(service).times(4);
    expect(cluster.getService("service2")).andThrow(new ObjectNotFoundException("service2"));

    expect(service.getServiceComponent("component1")).andThrow(new ServiceComponentNotFoundException("cluster1", "service1", "component1"));
    expect(service.getServiceComponent("component2")).andThrow(new ServiceComponentNotFoundException("cluster1", "service1", "component2"));
    expect(service.getServiceComponent("component3")).andReturn(component1);
    expect(service.getServiceComponent("component4")).andReturn(component2);

    expect(component1.convertToResponse()).andReturn(response1);
    expect(component2.convertToResponse()).andReturn(response2);
    // replay mocks
    replay(clusters, cluster, service, component1,  component2, response1, response2, ambariMetaInfo, stackId, managementController);

    //test
    Set<ServiceComponentResponse> setResponses = getComponentResourceProvider(managementController).getComponents(setRequests);

    // assert and verify
    assertEquals(2, setResponses.size());
    assertTrue(setResponses.contains(response1));
    assertTrue(setResponses.contains(response2));

    verify(clusters, cluster, service, component1,  component2, response1, response2, ambariMetaInfo, stackId, managementController);
  }

  public static ComponentResourceProvider getComponentResourceProvider(AmbariManagementController managementController) {
    Resource.Type type = Resource.Type.Component;

    return (ComponentResourceProvider) AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);
  }

  /**
   * Ensure that ServiceComponentNotFoundException is propagated in case where there is a single request.
   */
  @Test
  public void testGetComponents___ServiceComponentNotFoundException() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = new Capture<AmbariManagementController>();
    Clusters clusters = createNiceMock(Clusters.class);

    Cluster cluster = createNiceMock(Cluster.class);
    Service service = createNiceMock(Service.class);

    // requests
    ServiceComponentRequest request1 = new ServiceComponentRequest("cluster1", "service1", "component1",
        null);

    Set<ServiceComponentRequest> setRequests = new HashSet<ServiceComponentRequest>();
    setRequests.add(request1);

    // expectations
    // constructor init
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(null);

    // getComponents
    expect(clusters.getCluster("cluster1")).andReturn(cluster);
    expect(cluster.getService("service1")).andReturn(service);
    expect(service.getServiceComponent("component1")).andThrow(
        new ServiceComponentNotFoundException("cluster1", "service1", "component1"));
    // replay mocks
    replay(injector, clusters, cluster, service);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);

    // assert that exception is thrown in case where there is a single request
    try {
      getComponentResourceProvider(controller).getComponents(setRequests);
      fail("expected ServiceComponentNotFoundException");
    } catch (ServiceComponentNotFoundException e) {
      // expected
    }

    assertSame(controller, controllerCapture.getValue());
    verify(injector, clusters, cluster, service);
  }
  
  public static void createComponents(AmbariManagementController controller, Set<ServiceComponentRequest> requests) throws AmbariException {
    ComponentResourceProvider provider = getComponentResourceProvider(controller);
    provider.createComponents(requests);
  }

  public static Set<ServiceComponentResponse> getComponents(AmbariManagementController controller,
                                                 Set<ServiceComponentRequest> requests) throws AmbariException {
    ComponentResourceProvider provider = getComponentResourceProvider(controller);
    return provider.getComponents(requests);
  }

  public static RequestStatusResponse updateComponents(AmbariManagementController controller,
                                                     Set<ServiceComponentRequest> requests,
                                                     Map<String, String> requestProperties, boolean runSmokeTest) throws AmbariException
  {
    ComponentResourceProvider provider = getComponentResourceProvider(controller);
    return provider.updateComponents(requests, requestProperties, runSmokeTest);
  }
}
