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

import java.util.*;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ClusterRequest;
import org.apache.ambari.server.controller.ClusterResponse;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

/**
 * ClusterResourceProvider tests.
 */
public class ClusterResourceProviderTest {
  @Test
  public void testCreateResources() throws Exception{
    Resource.Type type = Resource.Type.Cluster;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    managementController.createCluster(
        AbstractResourceProviderTest.Matcher.getClusterRequest(null, "Cluster100", "HDP-0.1", null));
    managementController.createCluster(
        AbstractResourceProviderTest.Matcher.getClusterRequest(99L, null, "HDP-0.1", null));

    // replay
    replay(managementController, response);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();

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
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

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
  public void testGetResources() throws Exception{
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
    expect(managementController.getClusters(EasyMock.<Set<ClusterRequest>>anyObject())).andReturn(allResponse).once();
    expect(managementController.getClusters(EasyMock.<Set<ClusterRequest>>anyObject())).andReturn(nameResponse).once();
    expect(managementController.getClusters(EasyMock.<Set<ClusterRequest>>anyObject())).andReturn(idResponse).once();

    // replay
    replay(managementController);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
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
    Predicate predicate =
        new PredicateBuilder().property(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID).equals("Cluster102").
            toPredicate();
    resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    Assert.assertEquals(102L, resources.iterator().next().
        getPropertyValue(ClusterResourceProvider.CLUSTER_ID_PROPERTY_ID));
    Assert.assertEquals("Cluster102", resources.iterator().next().
        getPropertyValue(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID));

    // get cluster with id == 103
    predicate =
        new PredicateBuilder().property(ClusterResourceProvider.CLUSTER_ID_PROPERTY_ID).equals(103L).toPredicate();
    resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    Assert.assertEquals(103L, resources.iterator().next().
        getPropertyValue(ClusterResourceProvider.CLUSTER_ID_PROPERTY_ID));
    Assert.assertEquals("Cluster103", resources.iterator().next().
        getPropertyValue(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID));

    // verify
    verify(managementController);
  }

  @Test
  public void testUpdateResources() throws Exception{
    Resource.Type type = Resource.Type.Cluster;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    Set<ClusterResponse> nameResponse = new HashSet<ClusterResponse>();
    nameResponse.add(new ClusterResponse(102L, "Cluster102", null, null));

    Map<String, String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "Called from a test");

    // set expectations
    expect(managementController.getClusters(EasyMock.<Set<ClusterRequest>>anyObject())).andReturn(nameResponse).once();
    expect(managementController.updateClusters(
        AbstractResourceProviderTest.Matcher.getClusterRequestSet(102L, "Cluster102", "HDP-0.1", null), eq(mapRequestProps))).
        andReturn(response).once();
    expect(managementController.updateClusters(
        AbstractResourceProviderTest.Matcher.getClusterRequestSet(103L, null, "HDP-0.1", null), eq(mapRequestProps))).
        andReturn(response).once();

    // replay
    replay(managementController, response);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();

    ((ObservableResourceProvider)provider).addObserver(observer);

    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    properties.put(ClusterResourceProvider.CLUSTER_VERSION_PROPERTY_ID, "HDP-0.1");

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, mapRequestProps);

    // update the cluster named Cluster102
    Predicate  predicate = new PredicateBuilder().property(
        ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID).equals("Cluster102").toPredicate();
    provider.updateResources(request, predicate);

    // update the cluster where id == 103
    predicate = new PredicateBuilder().property(
        ClusterResourceProvider.CLUSTER_ID_PROPERTY_ID).equals(103L).toPredicate();
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
  public void testUpdateWithConfiguration() throws Exception {
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    Set<ClusterResponse> nameResponse = new HashSet<ClusterResponse>();
    nameResponse.add(new ClusterResponse(100L, "Cluster100", null, null));

    Map<String, String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "Called from a test");

    // set expectations
    expect(managementController.getClusters(EasyMock.<Set<ClusterRequest>>anyObject())).andReturn(nameResponse).times(2);
    expect(managementController.updateClusters(Collections.singleton(EasyMock.anyObject(ClusterRequest.class)),
        eq(mapRequestProps))).andReturn(response).times(1);

    // replay
    replay(managementController, response);

    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    properties.put(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    properties.put(PropertyHelper.getPropertyId("Clusters.desired_config", "type"), "global");
    properties.put(PropertyHelper.getPropertyId("Clusters.desired_config", "tag"), "version1");
    properties.put(PropertyHelper.getPropertyId("Clusters.desired_config.properties", "a"), "b");
    properties.put(PropertyHelper.getPropertyId("Clusters.desired_config.properties", "x"), "y");


    Map<String, Object> properties2 = new LinkedHashMap<String, Object>();

    properties2.put(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    properties2.put(PropertyHelper.getPropertyId("Clusters.desired_config", "type"), "mapred-site");
    properties2.put(PropertyHelper.getPropertyId("Clusters.desired_config", "tag"), "versio99");
    properties2.put(PropertyHelper.getPropertyId("Clusters.desired_config.properties", "foo"), "A1");
    properties2.put(PropertyHelper.getPropertyId("Clusters.desired_config.properties", "bar"), "B2");

    Set<Map<String, Object>> propertySet = new HashSet<Map<String, Object>>();

    propertySet.add(properties);
    propertySet.add(properties2);

    // create the request
    Request request = new RequestImpl(null, propertySet, mapRequestProps, null);

    Predicate  predicate = new PredicateBuilder().property(
        ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").toPredicate();
    
    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        Resource.Type.Cluster,
        PropertyHelper.getPropertyIds(Resource.Type.Cluster),
        PropertyHelper.getKeyPropertyIds(Resource.Type.Cluster),
        managementController);

    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();

    ((ObservableResourceProvider)provider).addObserver(observer);

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
  public void testDeleteResources() throws Exception{
    Resource.Type type = Resource.Type.Cluster;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    // set expectations
    managementController.deleteCluster(
        AbstractResourceProviderTest.Matcher.getClusterRequest(null, "Cluster102", null, null));
    managementController.deleteCluster(
        AbstractResourceProviderTest.Matcher.getClusterRequest(103L, null, null, null));

    // replay
    replay(managementController, response);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();

    ((ObservableResourceProvider)provider).addObserver(observer);

    // delete the cluster named Cluster102
    Predicate  predicate = new PredicateBuilder().property(
        ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID).equals("Cluster102").toPredicate();
    provider.deleteResources(predicate);

    // delete the cluster where id == 103
    predicate = new PredicateBuilder().property(
        ClusterResourceProvider.CLUSTER_ID_PROPERTY_ID).equals(103L).toPredicate();
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
}
