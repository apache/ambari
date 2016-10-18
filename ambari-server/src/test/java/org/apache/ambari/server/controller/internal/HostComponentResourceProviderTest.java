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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.MaintenanceStateHelper;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.ResourceProviderFactory;
import org.apache.ambari.server.controller.ServiceComponentHostRequest;
import org.apache.ambari.server.controller.ServiceComponentHostResponse;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Resource.Type;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.UpgradeState;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.inject.Injector;

/**
 * HostComponentResourceProvider tests.
 */
public class HostComponentResourceProviderTest {
  @Before
  public void clearAuthentication() {
    SecurityContextHolder.getContext().setAuthentication(null);
  }

  @Test
  public void testCreateResourcesAsAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testCreateResourcesAsClusterAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResourcesAsServiceAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createServiceAdministrator());
  }

  private void testCreateResources(Authentication authentication) throws Exception {
    Resource.Type type = Resource.Type.HostComponent;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);
    Injector injector = createNiceMock(Injector.class);
    HostComponentResourceProvider hostComponentResourceProvider =
        new HostComponentResourceProvider(PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController, injector);

    AbstractControllerResourceProvider.init(resourceProviderFactory);

    managementController.createHostComponents(
        AbstractResourceProviderTest.Matcher.getHostComponentRequestSet(
            "Cluster100", "Service100", "Component100", "Host100", null, null));

    expect(resourceProviderFactory.getHostComponentResourceProvider(EasyMock.<Set<String>>anyObject(),
        EasyMock.<Map<Type,String>>anyObject(),
        eq(managementController))).
        andReturn(hostComponentResourceProvider).anyTimes();


    // replay
    replay(managementController, response, resourceProviderFactory);

    SecurityContextHolder.getContext().setAuthentication(authentication);

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
    properties.put(HostComponentResourceProvider.HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    properties.put(HostComponentResourceProvider.HOST_COMPONENT_SERVICE_NAME_PROPERTY_ID, "Service100");
    properties.put(HostComponentResourceProvider.HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "Component100");
    properties.put(HostComponentResourceProvider.HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "Host100");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    provider.createResources(request);

    // verify
    verify(managementController, response, resourceProviderFactory);
  }

  @Test
  public void testGetResourcesAsAdministrator() throws Exception {
    testGetResources(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testGetResourcesAsClusterAdministrator() throws Exception {
    testGetResources(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test
  public void testGetResourcesAsServiceAdministrator() throws Exception {
    testGetResources(TestAuthenticationFactory.createServiceAdministrator());
  }

  private void testGetResources(Authentication authentication) throws Exception {
    Resource.Type type = Resource.Type.HostComponent;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);
    ResourceProvider hostComponentResourceProvider = createNiceMock(HostComponentResourceProvider.class);

    AbstractControllerResourceProvider.init(resourceProviderFactory);

    Set<ServiceComponentHostResponse> allResponse = new HashSet<ServiceComponentHostResponse>();
    StackId stackId = new StackId("HDP-0.1");
    StackId stackId2 = new StackId("HDP-0.2");
    allResponse.add(new ServiceComponentHostResponse(
        "Cluster100", "Service100", "Component100", "Component 100", "Host100", "Host100", State.INSTALLED.toString(), stackId.getStackId(), State.STARTED.toString(),
        stackId2.getStackId(), null));
    allResponse.add(new ServiceComponentHostResponse(
        "Cluster100", "Service100", "Component101", "Component 101", "Host100", "Host100", State.INSTALLED.toString(), stackId.getStackId(), State.STARTED.toString(),
        stackId2.getStackId(), null));

    allResponse.add(new ServiceComponentHostResponse(
        "Cluster100", "Service100", "Component102", "Component 102", "Host100","Host100", State.INSTALLED.toString(), stackId.getStackId(), State.STARTED.toString(),
        stackId2.getStackId(), null));
    Map<String, String> expectedNameValues = new HashMap<String, String>();
    expectedNameValues.put(
        HostComponentResourceProvider.HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    expectedNameValues.put(
        HostComponentResourceProvider.HOST_COMPONENT_STATE_PROPERTY_ID, State.INSTALLED.toString());
    expectedNameValues.put(
        HostComponentResourceProvider.HOST_COMPONENT_STACK_ID_PROPERTY_ID, stackId.getStackId());
    expectedNameValues.put(
        HostComponentResourceProvider.HOST_COMPONENT_DESIRED_STATE_PROPERTY_ID, State.STARTED.toString());
    expectedNameValues.put(
        HostComponentResourceProvider.HOST_COMPONENT_DESIRED_STACK_ID_PROPERTY_ID, stackId2.getStackId());
    expectedNameValues.put(
        HostComponentResourceProvider.HOST_COMPONENT_UPGRADE_STATE_PROPERTY_ID, UpgradeState.NONE.name());


    // set expectations
    expect(resourceProviderFactory.getHostComponentResourceProvider(EasyMock.<Set<String>>anyObject(),
        EasyMock.<Map<Type,String>>anyObject(),
        eq(managementController))).
        andReturn(hostComponentResourceProvider).anyTimes();

    Set<String> propertyIds = new HashSet<String>();

    propertyIds.add(HostComponentResourceProvider.HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID);
    propertyIds.add(HostComponentResourceProvider.HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID);
    propertyIds.add(HostComponentResourceProvider.HOST_COMPONENT_STATE_PROPERTY_ID);
    propertyIds.add(HostComponentResourceProvider.HOST_COMPONENT_STACK_ID_PROPERTY_ID);
    propertyIds.add(HostComponentResourceProvider.HOST_COMPONENT_DESIRED_STATE_PROPERTY_ID);
    propertyIds.add(HostComponentResourceProvider.HOST_COMPONENT_DESIRED_STACK_ID_PROPERTY_ID);

    Predicate predicate = new PredicateBuilder().property(
        HostComponentResourceProvider.HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").toPredicate();
    Request request = PropertyHelper.getReadRequest(propertyIds);

    Set<Resource> hostsComponentResources = new HashSet<Resource>();

    Resource hostsComponentResource1 = new ResourceImpl(Resource.Type.HostComponent);
    hostsComponentResource1.setProperty(HostComponentResourceProvider.HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    hostsComponentResource1.setProperty(HostComponentResourceProvider.HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "Host100");
    hostsComponentResource1.setProperty(HostComponentResourceProvider.HOST_COMPONENT_SERVICE_NAME_PROPERTY_ID, "Service100");
    hostsComponentResource1.setProperty(HostComponentResourceProvider.HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "Component100");
    hostsComponentResource1.setProperty(HostComponentResourceProvider.HOST_COMPONENT_STATE_PROPERTY_ID, State.INSTALLED.name());
    hostsComponentResource1.setProperty(HostComponentResourceProvider.HOST_COMPONENT_DESIRED_STATE_PROPERTY_ID, State.STARTED.name());
    hostsComponentResource1.setProperty(HostComponentResourceProvider.HOST_COMPONENT_STACK_ID_PROPERTY_ID, stackId.getStackId());
    hostsComponentResource1.setProperty(HostComponentResourceProvider.HOST_COMPONENT_DESIRED_STACK_ID_PROPERTY_ID, stackId2.getStackId());
    hostsComponentResource1.setProperty(HostComponentResourceProvider.HOST_COMPONENT_UPGRADE_STATE_PROPERTY_ID, UpgradeState.NONE.name());

    Resource hostsComponentResource2 = new ResourceImpl(Resource.Type.HostComponent);
    hostsComponentResource2.setProperty(HostComponentResourceProvider.HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    hostsComponentResource2.setProperty(HostComponentResourceProvider.HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "Host100");
    hostsComponentResource2.setProperty(HostComponentResourceProvider.HOST_COMPONENT_SERVICE_NAME_PROPERTY_ID, "Service100");
    hostsComponentResource2.setProperty(HostComponentResourceProvider.HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "Component101");
    hostsComponentResource2.setProperty(HostComponentResourceProvider.HOST_COMPONENT_STATE_PROPERTY_ID, State.INSTALLED.name());
    hostsComponentResource2.setProperty(HostComponentResourceProvider.HOST_COMPONENT_DESIRED_STATE_PROPERTY_ID, State.STARTED.name());
    hostsComponentResource2.setProperty(HostComponentResourceProvider.HOST_COMPONENT_STACK_ID_PROPERTY_ID, stackId.getStackId());
    hostsComponentResource2.setProperty(HostComponentResourceProvider.HOST_COMPONENT_DESIRED_STACK_ID_PROPERTY_ID, stackId2.getStackId());
    hostsComponentResource2.setProperty(HostComponentResourceProvider.HOST_COMPONENT_UPGRADE_STATE_PROPERTY_ID, UpgradeState.NONE.name());

    Resource hostsComponentResource3 = new ResourceImpl(Resource.Type.HostComponent);
    hostsComponentResource3.setProperty(HostComponentResourceProvider.HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    hostsComponentResource3.setProperty(HostComponentResourceProvider.HOST_COMPONENT_HOST_NAME_PROPERTY_ID, "Host100");
    hostsComponentResource3.setProperty(HostComponentResourceProvider.HOST_COMPONENT_SERVICE_NAME_PROPERTY_ID, "Service100");
    hostsComponentResource3.setProperty(HostComponentResourceProvider.HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, "Component102");
    hostsComponentResource3.setProperty(HostComponentResourceProvider.HOST_COMPONENT_STATE_PROPERTY_ID, State.INSTALLED.name());
    hostsComponentResource3.setProperty(HostComponentResourceProvider.HOST_COMPONENT_DESIRED_STATE_PROPERTY_ID, State.STARTED.name());
    hostsComponentResource3.setProperty(HostComponentResourceProvider.HOST_COMPONENT_STACK_ID_PROPERTY_ID, stackId.getStackId());
    hostsComponentResource3.setProperty(HostComponentResourceProvider.HOST_COMPONENT_DESIRED_STACK_ID_PROPERTY_ID, stackId2.getStackId());
    hostsComponentResource3.setProperty(HostComponentResourceProvider.HOST_COMPONENT_UPGRADE_STATE_PROPERTY_ID, UpgradeState.NONE.name());

    hostsComponentResources.add(hostsComponentResource1);
    hostsComponentResources.add(hostsComponentResource2);
    hostsComponentResources.add(hostsComponentResource3);

    expect(hostComponentResourceProvider.getResources(eq(request), eq(predicate))).andReturn(hostsComponentResources).anyTimes();

    // replay
    replay(managementController, resourceProviderFactory, hostComponentResourceProvider);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);


    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(3, resources.size());
    Set<String> names = new HashSet<String>();
    for (Resource resource : resources) {
      for (String key : expectedNameValues.keySet()) {
        Assert.assertEquals(expectedNameValues.get(key), resource.getPropertyValue(key));
      }
      names.add((String) resource.getPropertyValue(
          HostComponentResourceProvider.HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID));
    }
    // Make sure that all of the response objects got moved into resources
    for (ServiceComponentHostResponse response : allResponse) {
      Assert.assertTrue(names.contains(response.getComponentName()));
    }

    // verify
    verify(managementController, resourceProviderFactory, hostComponentResourceProvider);
  }

  @Test
  public void testUpdateResourcesAsAdministrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testUpdateResourcesAsClusterAdministrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test
  public void testUpdateResourcesAsServiceAdministrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createServiceAdministrator());
  }

  private void testUpdateResources(Authentication authentication) throws Exception {
    Resource.Type type = Resource.Type.HostComponent;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    HostVersionDAO hostVersionDAO = createMock(HostVersionDAO.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);
    Injector injector = createNiceMock(Injector.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Service service = createNiceMock(Service.class);
    ServiceComponent component = createNiceMock(ServiceComponent.class);
    ServiceComponentHost componentHost = createNiceMock(ServiceComponentHost.class);
    RequestStageContainer stageContainer = createNiceMock(RequestStageContainer.class);
    MaintenanceStateHelper maintenanceStateHelper = createNiceMock(MaintenanceStateHelper.class);


    Map<String, String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "Called from a test");

    Set<ServiceComponentHostResponse> nameResponse = new HashSet<ServiceComponentHostResponse>();
    nameResponse.add(new ServiceComponentHostResponse(
        "Cluster102", "Service100", "Component100", "Component 100", "Host100", "Host100","INSTALLED", "", "", "", null));

    // set expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.findServiceName(cluster, "Component100")).andReturn("Service100").anyTimes();
    expect(clusters.getCluster("Cluster102")).andReturn(cluster).anyTimes();
    expect(cluster.getClusterId()).andReturn(2L).anyTimes();
    expect(cluster.getService("Service100")).andReturn(service).anyTimes();
    expect(service.getServiceComponent("Component100")).andReturn(component).anyTimes();
    expect(component.getServiceComponentHost("Host100")).andReturn(componentHost).anyTimes();
    expect(component.getName()).andReturn("Component100").anyTimes();
    expect(componentHost.getState()).andReturn(State.INSTALLED).anyTimes();
    expect(response.getMessage()).andReturn("response msg").anyTimes();
    expect(response.getRequestId()).andReturn(1000L);

    //Cluster is default type.  Maintenance mode is not being tested here so the default is returned.
    expect(maintenanceStateHelper.isOperationAllowed(Resource.Type.Cluster, componentHost)).andReturn(true).anyTimes();

    expect(managementController.getHostComponents(
        EasyMock.<Set<ServiceComponentHostRequest>>anyObject())).andReturn(nameResponse).once();

    Map<String, Map<State, List<ServiceComponentHost>>> changedHosts = new HashMap<String, Map<State, List<ServiceComponentHost>>>();
    List<ServiceComponentHost> changedComponentHosts = new ArrayList<ServiceComponentHost>();
    changedComponentHosts.add(componentHost);
    changedHosts.put("Component100", Collections.singletonMap(State.STARTED, changedComponentHosts));

    expect(managementController.addStages(null, cluster, mapRequestProps, null, null, null, changedHosts,
        Collections.<ServiceComponentHost>emptyList(), false, false)).andReturn(stageContainer).once();

    stageContainer.persist();
    expect(stageContainer.getRequestStatusResponse()).andReturn(response).once();

    TestHostComponentResourceProvider provider =
        new TestHostComponentResourceProvider(PropertyHelper.getPropertyIds(type),
            PropertyHelper.getKeyPropertyIds(type),
            managementController, injector);
    provider.setFieldValue("maintenanceStateHelper", maintenanceStateHelper);
    provider.setFieldValue("hostVersionDAO", hostVersionDAO);

    expect(resourceProviderFactory.getHostComponentResourceProvider(EasyMock.<Set<String>>anyObject(),
        EasyMock.<Map<Type,String>>anyObject(),
        eq(managementController))).
        andReturn(provider).anyTimes();

    // replay
    replay(managementController, response, resourceProviderFactory, clusters, cluster, service,
        component, componentHost, stageContainer, maintenanceStateHelper);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    properties.put(HostComponentResourceProvider.HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, mapRequestProps);

    // update the cluster named Cluster102
    Predicate predicate = new PredicateBuilder().property(
        HostComponentResourceProvider.HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID).equals("Cluster102").and().
        property(HostComponentResourceProvider.HOST_COMPONENT_STATE_PROPERTY_ID).equals("INSTALLED").and().
        property(HostComponentResourceProvider.HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID).equals("Component100").toPredicate();
    RequestStatus requestStatus = provider.updateResources(request, predicate);
    Resource responseResource = requestStatus.getRequestResource();
    assertEquals("response msg", responseResource.getPropertyValue(PropertyHelper.getPropertyId("Requests", "message")));
    assertEquals(1000L, responseResource.getPropertyValue(PropertyHelper.getPropertyId("Requests", "id")));
    assertEquals("Accepted", responseResource.getPropertyValue(PropertyHelper.getPropertyId("Requests", "status")));
    assertTrue(requestStatus.getAssociatedResources().isEmpty());

    // verify
    verify(managementController, response, resourceProviderFactory, stageContainer);
  }


  @Test
  public void testDeleteResourcesAsAdministrator() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testDeleteResourcesAsClusterAdministrator() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testDeleteResourcesAsServiceAdministrator() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createServiceAdministrator());
  }

  private void testDeleteResources(Authentication authentication) throws Exception {
    Resource.Type type = Resource.Type.HostComponent;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    DeleteStatusMetaData deleteStatusMetaData = createNiceMock(DeleteStatusMetaData.class);
    Injector injector = createNiceMock(Injector.class);

    HostComponentResourceProvider provider =
        new HostComponentResourceProvider(PropertyHelper.getPropertyIds(type),
            PropertyHelper.getKeyPropertyIds(type),
            managementController, injector);

    // set expectations
    expect(managementController.deleteHostComponents(
        AbstractResourceProviderTest.Matcher.getHostComponentRequestSet(
            null, null, "Component100", "Host100", null, null))).andReturn(deleteStatusMetaData);

    // replay
    replay(managementController, deleteStatusMetaData);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();

    provider.addObserver(observer);

    Predicate predicate = new PredicateBuilder().
        property(HostComponentResourceProvider.HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID).equals("Component100").and().
        property(HostComponentResourceProvider.HOST_COMPONENT_HOST_NAME_PROPERTY_ID).equals("Host100").toPredicate();
    provider.deleteResources(new RequestImpl(null, null, null, null), predicate);


    ResourceProviderEvent lastEvent = observer.getLastEvent();
    Assert.assertNotNull(lastEvent);
    Assert.assertEquals(Resource.Type.HostComponent, lastEvent.getResourceType());
    Assert.assertEquals(ResourceProviderEvent.Type.Delete, lastEvent.getType());
    Assert.assertEquals(predicate, lastEvent.getPredicate());
    Assert.assertNull(lastEvent.getRequest());

    // verify
    verify(managementController, deleteStatusMetaData);
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
    Injector injector = createNiceMock(Injector.class);

    HostComponentResourceProvider provider =
        new HostComponentResourceProvider(propertyIds,
        keyPropertyIds,
        managementController, injector);

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

  @Test
  public void testUpdateResourcesNothingToUpdate() throws Exception {
    Authentication authentication = TestAuthenticationFactory.createServiceAdministrator();
    Resource.Type type = Resource.Type.HostComponent;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    HostVersionDAO hostVersionDAO = createMock(HostVersionDAO.class);
//    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);
    Injector injector = createNiceMock(Injector.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Service service = createNiceMock(Service.class);
    ServiceComponent component = createNiceMock(ServiceComponent.class);
    ServiceComponentHost componentHost = createNiceMock(ServiceComponentHost.class);
    RequestStageContainer stageContainer = createNiceMock(RequestStageContainer.class);
    MaintenanceStateHelper maintenanceStateHelper = createNiceMock(MaintenanceStateHelper.class);


    Map<String, String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "Called from a test");

    Set<ServiceComponentHostResponse> nameResponse = new HashSet<ServiceComponentHostResponse>();
    nameResponse.add(new ServiceComponentHostResponse(
        "Cluster102", "Service100", "Component100", "Component 100", "Host100", "Host100","INSTALLED", "", "", "", null));

    // set expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.findServiceName(cluster, "Component100")).andReturn("Service100").anyTimes();
    expect(clusters.getCluster("Cluster102")).andReturn(cluster).anyTimes();
    expect(cluster.getClusterId()).andReturn(2L).anyTimes();
    expect(cluster.getService("Service100")).andReturn(service).anyTimes();
    expect(service.getServiceComponent("Component100")).andReturn(component).anyTimes();
    expect(component.getServiceComponentHost("Host100")).andReturn(componentHost).anyTimes();
    expect(component.getName()).andReturn("Component100").anyTimes();
    expect(componentHost.getState()).andReturn(State.INSTALLED).anyTimes();

    //Cluster is default type.  Maintenance mode is not being tested here so the default is returned.
    expect(maintenanceStateHelper.isOperationAllowed(Resource.Type.Cluster, componentHost)).andReturn(true).anyTimes();

    expect(managementController.getHostComponents(
        EasyMock.<Set<ServiceComponentHostRequest>>anyObject())).andReturn(Collections.<ServiceComponentHostResponse>emptySet()).once();

    Map<String, Map<State, List<ServiceComponentHost>>> changedHosts = new HashMap<String, Map<State, List<ServiceComponentHost>>>();
    List<ServiceComponentHost> changedComponentHosts = new ArrayList<ServiceComponentHost>();
    changedComponentHosts.add(componentHost);
    changedHosts.put("Component100", Collections.singletonMap(State.STARTED, changedComponentHosts));

    TestHostComponentResourceProvider provider =
        new TestHostComponentResourceProvider(PropertyHelper.getPropertyIds(type),
            PropertyHelper.getKeyPropertyIds(type),
            managementController, injector);
    provider.setFieldValue("maintenanceStateHelper", maintenanceStateHelper);
    provider.setFieldValue("hostVersionDAO", hostVersionDAO);

    expect(resourceProviderFactory.getHostComponentResourceProvider(EasyMock.<Set<String>>anyObject(),
        EasyMock.<Map<Type,String>>anyObject(),
        eq(managementController))).
        andReturn(provider).anyTimes();

    // replay
    replay(managementController, resourceProviderFactory, clusters, cluster, service,
        component, componentHost, stageContainer, maintenanceStateHelper);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    properties.put(HostComponentResourceProvider.HOST_COMPONENT_STATE_PROPERTY_ID, "STARTED");

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, mapRequestProps);

    // update the cluster named Cluster102
    Predicate predicate = new PredicateBuilder().property(
        HostComponentResourceProvider.HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID).equals("Cluster102").and().
        property(HostComponentResourceProvider.HOST_COMPONENT_STATE_PROPERTY_ID).equals("INSTALLED").and().
        property(HostComponentResourceProvider.HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID).equals("Component100").toPredicate();


    try {
      provider.updateResources(request, predicate);
      fail("Expected exception when no resources are found to be updatable");
    } catch (NoSuchResourceException e) {
      // !!! expected
    }


    // verify
    verify(managementController, resourceProviderFactory, stageContainer);
  }

  // Used to directly call updateHostComponents on the resource provider.
  // This exists as a temporary solution as a result of moving updateHostComponents from
  // AmbariManagentControllerImpl to HostComponentResourceProvider.
  public static RequestStatusResponse updateHostComponents(AmbariManagementController controller,
                                                           Injector injector,
                                                           Set<ServiceComponentHostRequest> requests,
                                                           Map<String, String> requestProperties,
                                                           boolean runSmokeTest) throws Exception {
    Resource.Type type = Resource.Type.HostComponent;
    TestHostComponentResourceProvider provider =
        new TestHostComponentResourceProvider(PropertyHelper.getPropertyIds(type),
            PropertyHelper.getKeyPropertyIds(type),
            controller, injector);

    provider.setFieldValue("maintenanceStateHelper", injector.getInstance(MaintenanceStateHelper.class));
    provider.setFieldValue("hostVersionDAO", injector.getInstance(HostVersionDAO.class));

    RequestStageContainer requestStages = provider.updateHostComponents(null, requests, requestProperties, runSmokeTest);
    requestStages.persist();
    return requestStages.getRequestStatusResponse();
  }

  private static class TestHostComponentResourceProvider extends HostComponentResourceProvider {

    /**
     * Create a  new resource provider for the given management controller.
     *
     * @param propertyIds          the property ids
     * @param keyPropertyIds       the key property ids
     * @param managementController the management controller
     */
    public TestHostComponentResourceProvider(Set<String> propertyIds, Map<Resource.Type, String> keyPropertyIds,
                                             AmbariManagementController managementController, Injector injector) throws Exception {
      super(propertyIds, keyPropertyIds, managementController, injector);
    }

    public void setFieldValue(String fieldName, Object fieldValue) throws Exception {
      Class<?> c = getClass().getSuperclass();
      Field f = c.getDeclaredField(fieldName);
      f.setAccessible(true);
      f.set(this, fieldValue);
    }

  }
}
