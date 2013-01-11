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

import org.apache.ambari.server.controller.*;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Resource provider tests.
 */
public class ResourceProviderImplTest {

  @Test
  public void testCreateClusterResources() throws Exception{
    Resource.Type type = Resource.Type.Cluster;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    managementController.createCluster(Matchers.clusterRequest(null, "Cluster100", "HDP-0.1", null));
    managementController.createCluster(Matchers.clusterRequest(99L, null, "HDP-0.1", null));

    // replay
    replay(managementController, response);

    ResourceProvider provider = ResourceProviderImpl.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    TestObserver observer = new TestObserver();

    ((ObservableResourceProvider)provider).addObserver(observer);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<Map<String, Object>>();

    // Cluster 1: create a map of properties for the request
    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    // add the cluster name to the properties map
    properties.put(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID, "Cluster100");

    // add the version to the properties map
    properties.put(ClusterResourceProvider.CLUSTER_VERSION_PROPERTY_ID, "HDP-0.1");

    propertySet.add(properties);

    // Cluster 2: create a map of properties for the request
    properties = new LinkedHashMap<String, Object>();

    // add the cluster id to the properties map
    properties.put(ClusterResourceProvider.CLUSTER_ID_PROPERTY_ID, 99L);

    // add the version to the properties map
    properties.put(ClusterResourceProvider.CLUSTER_VERSION_PROPERTY_ID, "HDP-0.1");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet);

    provider.createResources(request);

    ResourceProviderEvent lastEvent = observer.getLastEvent();
    Assert.assertNotNull(lastEvent);
    Assert.assertEquals(Resource.Type.Cluster, lastEvent.getResourceType());
    Assert.assertEquals(ResourceProviderEvent.Type.Create, lastEvent.getType());
    Assert.assertEquals(request, lastEvent.getRequest());
    Assert.assertNull(lastEvent.getPredicate());

    // verify
    verify(managementController, response);
  }

  @Test
  public void testGetClusterResources() throws Exception{
    Resource.Type type = Resource.Type.Cluster;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    Set<ClusterResponse> allResponse = new HashSet<ClusterResponse>();
    allResponse.add(new ClusterResponse(100L, "Cluster100", null, null));
    allResponse.add(new ClusterResponse(101L, "Cluster101", null, null));
    allResponse.add(new ClusterResponse(102L, "Cluster102", null, null));
    allResponse.add(new ClusterResponse(103L, "Cluster103", null, null));
    allResponse.add(new ClusterResponse(104L, "Cluster104", null, null));

    Set<ClusterResponse> nameResponse = new HashSet<ClusterResponse>();
    nameResponse.add(new ClusterResponse(102L, "Cluster102", null, null));

    Set<ClusterResponse> idResponse = new HashSet<ClusterResponse>();
    idResponse.add(new ClusterResponse(103L, "Cluster103", null, null));

    // set expectations
    expect(managementController.getClusters(anyObject(Set.class))).andReturn(allResponse).once();
    expect(managementController.getClusters(anyObject(Set.class))).andReturn(nameResponse).once();
    expect(managementController.getClusters(anyObject(Set.class))).andReturn(idResponse).once();

    // replay
    replay(managementController);

    ResourceProvider provider = ResourceProviderImpl.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    Set<String> propertyIds = new HashSet<String>();

    propertyIds.add(ClusterResourceProvider.CLUSTER_ID_PROPERTY_ID);
    propertyIds.add(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID);

    // create the request
    Request request = PropertyHelper.getReadRequest(propertyIds);

    // get all ... no predicate
    Set<Resource> resources = provider.getResources(request, null);

    Assert.assertEquals(5, resources.size());
    for (Resource resource : resources) {
      Long id = (Long) resource.getPropertyValue(ClusterResourceProvider.CLUSTER_ID_PROPERTY_ID);
      String name = (String) resource.getPropertyValue(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID);
      Assert.assertEquals(name, "Cluster" + id);
    }

    // get cluster named Cluster102
    Predicate  predicate = new PredicateBuilder().property(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID).equals("Cluster102").toPredicate();
    resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    Assert.assertEquals(102L, resources.iterator().next().getPropertyValue(ClusterResourceProvider.CLUSTER_ID_PROPERTY_ID));
    Assert.assertEquals("Cluster102", resources.iterator().next().getPropertyValue(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID));

    // get cluster with id == 103
    predicate = new PredicateBuilder().property(ClusterResourceProvider.CLUSTER_ID_PROPERTY_ID).equals(103L).toPredicate();
    resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    Assert.assertEquals(103L, resources.iterator().next().getPropertyValue(ClusterResourceProvider.CLUSTER_ID_PROPERTY_ID));
    Assert.assertEquals("Cluster103", resources.iterator().next().getPropertyValue(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID));

    // verify
    verify(managementController);
  }

  @Test
  public void testUpdateClusterResources() throws Exception{
    Resource.Type type = Resource.Type.Cluster;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    Set<ClusterResponse> nameResponse = new HashSet<ClusterResponse>();
    nameResponse.add(new ClusterResponse(102L, "Cluster102", null, null));

    // set expectations
    expect(managementController.getClusters(anyObject(Set.class))).andReturn(nameResponse).once();
    expect(managementController.updateCluster(Matchers.clusterRequest(102L, "Cluster102", "HDP-0.1", null))).andReturn(response).once();
    expect(managementController.updateCluster(Matchers.clusterRequest(103L, null, "HDP-0.1", null))).andReturn(response).once();

    // replay
    replay(managementController, response);

    ResourceProvider provider = ResourceProviderImpl.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    TestObserver observer = new TestObserver();

    ((ObservableResourceProvider)provider).addObserver(observer);

    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    properties.put(ClusterResourceProvider.CLUSTER_VERSION_PROPERTY_ID, "HDP-0.1");

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties);

    // update the cluster named Cluster102
    Predicate  predicate = new PredicateBuilder().property(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID).equals("Cluster102").toPredicate();
    provider.updateResources(request, predicate);

    // update the cluster where id == 103
    predicate = new PredicateBuilder().property(ClusterResourceProvider.CLUSTER_ID_PROPERTY_ID).equals(103L).toPredicate();
    provider.updateResources(request, predicate);

    ResourceProviderEvent lastEvent = observer.getLastEvent();
    Assert.assertNotNull(lastEvent);
    Assert.assertEquals(Resource.Type.Cluster, lastEvent.getResourceType());
    Assert.assertEquals(ResourceProviderEvent.Type.Update, lastEvent.getType());
    Assert.assertEquals(request, lastEvent.getRequest());
    Assert.assertEquals(predicate, lastEvent.getPredicate());

    // verify
    verify(managementController, response);
  }

  @Test
  public void testDeleteClusterResources() throws Exception{
    Resource.Type type = Resource.Type.Cluster;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    Set<ClusterResponse> nameResponse = new HashSet<ClusterResponse>();
    nameResponse.add(new ClusterResponse(102L, "Cluster102", null, null));

    // set expectations
    managementController.deleteCluster(Matchers.clusterRequest(null, "Cluster102", null, null));
    managementController.deleteCluster(Matchers.clusterRequest(103L, null, null, null));

    // replay
    replay(managementController, response);

    ResourceProvider provider = ResourceProviderImpl.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    TestObserver observer = new TestObserver();

    ((ObservableResourceProvider)provider).addObserver(observer);

    // delete the cluster named Cluster102
    Predicate  predicate = new PredicateBuilder().property(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID).equals("Cluster102").toPredicate();
    provider.deleteResources(predicate);

    // delete the cluster where id == 103
    predicate = new PredicateBuilder().property(ClusterResourceProvider.CLUSTER_ID_PROPERTY_ID).equals(103L).toPredicate();
    provider.deleteResources(predicate);

    ResourceProviderEvent lastEvent = observer.getLastEvent();
    Assert.assertNotNull(lastEvent);
    Assert.assertEquals(Resource.Type.Cluster, lastEvent.getResourceType());
    Assert.assertEquals(ResourceProviderEvent.Type.Delete, lastEvent.getType());
    Assert.assertEquals(predicate, lastEvent.getPredicate());
    Assert.assertNull(lastEvent.getRequest());

    // verify
    verify(managementController, response);
  }

  @Test
  public void testCreateServiceResources() throws Exception{
    Resource.Type type = Resource.Type.Service;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

//    Set<ServiceRequest> requests = new HashSet<ServiceRequest>();
//    requests.add(Matchers.serviceRequest("Cluster100", "Service100", null, "DEPLOYED"));
    managementController.createServices(anyObject(Set.class));

    // replay
    replay(managementController, response);

    ResourceProvider provider = ResourceProviderImpl.getResourceProvider(
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
    Request request = PropertyHelper.getCreateRequest(propertySet);

    provider.createResources(request);

    // verify
    verify(managementController, response);
  }

  @Test
  public void testGetServiceResources() throws Exception{
    Resource.Type type = Resource.Type.Service;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    Set<ServiceResponse> allResponse = new HashSet<ServiceResponse>();
    allResponse.add(new ServiceResponse(100L, "Cluster100", "Service100", null, "HDP-0.1", null));
    allResponse.add(new ServiceResponse(100L, "Cluster100", "Service101", null, "HDP-0.1", null));
    allResponse.add(new ServiceResponse(100L, "Cluster100", "Service102", null, "HDP-0.1", null));
    allResponse.add(new ServiceResponse(100L, "Cluster100", "Service103", null, "HDP-0.1", null));
    allResponse.add(new ServiceResponse(100L, "Cluster100", "Service104", null, "HDP-0.1", null));

    Set<ServiceResponse> nameResponse = new HashSet<ServiceResponse>();
    nameResponse.add(new ServiceResponse(100L, "Cluster100", "Service102", null, "HDP-0.1", null));

    Set<ServiceResponse> stateResponse = new HashSet<ServiceResponse>();
    stateResponse.add(new ServiceResponse(100L, "Cluster100", "Service100", null, "HDP-0.1", null));
    stateResponse.add(new ServiceResponse(100L, "Cluster100", "Service102", null, "HDP-0.1", null));
    stateResponse.add(new ServiceResponse(100L, "Cluster100", "Service104", null, "HDP-0.1", null));

    // set expectations
    expect(managementController.getServices(anyObject(Set.class))).andReturn(allResponse).once();
    expect(managementController.getServices(anyObject(Set.class))).andReturn(nameResponse).once();
    expect(managementController.getServices(anyObject(Set.class))).andReturn(stateResponse).once();

    // replay
    replay(managementController);

    ResourceProvider provider = ResourceProviderImpl.getResourceProvider(
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
    Predicate  predicate = new PredicateBuilder().property(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID).equals("Service102").toPredicate();
    resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    Assert.assertEquals("Cluster100", resources.iterator().next().getPropertyValue(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID));
    Assert.assertEquals("Service102", resources.iterator().next().getPropertyValue(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID));

    // get services where state == "DEPLOYED"
    predicate = new PredicateBuilder().property(ServiceResourceProvider.SERVICE_SERVICE_STATE_PROPERTY_ID).equals("DEPLOYED").toPredicate();
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
  public void testUpdateServiceResources() throws Exception{
    Resource.Type type = Resource.Type.Service;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    // set expectations
    expect(managementController.updateServices(anyObject(Set.class))).andReturn(response).once();

    // replay
    replay(managementController, response);

    ResourceProvider provider = ResourceProviderImpl.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    // add the property map to a set for the request.
    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    properties.put(ServiceResourceProvider.SERVICE_SERVICE_STATE_PROPERTY_ID, "DEPLOYED");

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties);

    // update the service named Service102
    Predicate  predicate = new PredicateBuilder().property(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").
        and().property(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID).equals("Service102").toPredicate();
    provider.updateResources(request, predicate);

    // verify
    verify(managementController, response);
  }


  // ----- helper methods ----------------------------------------------------

  public static class Matchers
  {
    public static ClusterRequest clusterRequest(Long clusterId, String clusterName, String stackVersion, Set<String> hostNames)
    {
      EasyMock.reportMatcher(new ClusterRequestMatcher(clusterId, clusterName, stackVersion, hostNames));
      return null;
    }

    public static ServiceRequest serviceRequest(String clusterName, String serviceName, Map<String, String> configVersions, String desiredState)
    {
      EasyMock.reportMatcher(new ServiceRequestMatcher(clusterName, serviceName, configVersions, desiredState));
      return null;
    }
  }

  public static boolean eq(Object left, Object right) {
    return  left == null ? right == null : right != null && left.equals(right);
  }


  // ----- innner classes ----------------------------------------------------

  public static class ClusterRequestMatcher extends ClusterRequest implements IArgumentMatcher {

    public ClusterRequestMatcher(Long clusterId, String clusterName, String stackVersion, Set<String> hostNames) {
      super(clusterId, clusterName, stackVersion, hostNames);
    }

    @Override
    public boolean matches(Object o) {
      return o instanceof ClusterRequest &&
          eq(((ClusterRequest) o).getClusterId(), getClusterId()) &&
          eq(((ClusterRequest) o).getClusterName(), getClusterName()) &&
          eq(((ClusterRequest) o).getStackVersion(), getStackVersion()) &&
          eq(((ClusterRequest) o).getHostNames(), getHostNames());
    }

    @Override
    public void appendTo(StringBuffer stringBuffer) {
      stringBuffer.append("ClusterRequestMatcher(" + "" + ")");
    }
  }

  public static class ServiceRequestMatcher extends ServiceRequest implements IArgumentMatcher {

    public ServiceRequestMatcher(String clusterName, String serviceName, Map<String, String> configVersions, String desiredState) {
      super(clusterName, serviceName, configVersions, desiredState);
    }

    @Override
    public boolean matches(Object o) {
      return o instanceof ServiceRequest &&
          eq(((ServiceRequest) o).getClusterName(), getClusterName()) &&
          eq(((ServiceRequest) o).getServiceName(), getServiceName()) &&
          eq(((ServiceRequest) o).getConfigVersions(), getConfigVersions()) &&
          eq(((ServiceRequest) o).getDesiredState(), getDesiredState());

    }

    @Override
    public void appendTo(StringBuffer stringBuffer) {
      stringBuffer.append("ClusterRequestMatcher(" + "" + ")");
    }
  }

  public class TestObserver implements ResourceProviderObserver {

    ResourceProviderEvent lastEvent = null;

    @Override
    public void update(ResourceProviderEvent event) {
      lastEvent = event;
    }

    public ResourceProviderEvent getLastEvent() {
      return lastEvent;
    }
  }

}
