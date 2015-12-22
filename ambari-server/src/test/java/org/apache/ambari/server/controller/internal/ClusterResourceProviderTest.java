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

import com.google.gson.Gson;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ClusterRequest;
import org.apache.ambari.server.controller.ClusterResponse;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.topology.Blueprint;
import org.apache.ambari.server.topology.BlueprintFactory;
import org.apache.ambari.server.topology.InvalidTopologyException;
import org.apache.ambari.server.topology.SecurityConfiguration;
import org.apache.ambari.server.topology.SecurityConfigurationFactory;
import org.apache.ambari.server.topology.TopologyManager;
import org.apache.ambari.server.topology.TopologyRequestFactory;
import org.apache.ambari.server.utils.RetryHelper;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.eclipse.persistence.exceptions.DatabaseException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.easymock.EasyMock.anyBoolean;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;


/**
 * ClusterResourceProvider tests.
 */
public class ClusterResourceProviderTest {
  private static final String CLUSTER_NAME = "cluster_name";
  private static final String BLUEPRINT_NAME = "blueprint_name";

  private ClusterResourceProvider provider;

  private static final AmbariManagementController controller = createNiceMock(AmbariManagementController.class);
  private static final Request request = createNiceMock(Request.class);
  private static final TopologyManager topologyManager = createStrictMock(TopologyManager.class);
  private static final TopologyRequestFactory topologyFactory = createStrictMock(TopologyRequestFactory.class);
  private final static SecurityConfigurationFactory securityFactory = createMock(SecurityConfigurationFactory.class);
  private static final ProvisionClusterRequest topologyRequest = createNiceMock(ProvisionClusterRequest.class);
  private static final BlueprintFactory blueprintFactory = createStrictMock(BlueprintFactory.class);
  private static final Blueprint blueprint = createNiceMock(Blueprint.class);
  private static final RequestStatusResponse requestStatusResponse = createNiceMock(RequestStatusResponse.class);
  private static final Gson gson = new Gson();

  @Before
  public void setup() throws Exception{
    ClusterResourceProvider.init(topologyManager, topologyFactory, securityFactory, gson);
    ProvisionClusterRequest.init(blueprintFactory);
    provider = new ClusterResourceProvider(controller);

    expect(blueprintFactory.getBlueprint(BLUEPRINT_NAME)).andReturn(blueprint).anyTimes();
    expect(securityFactory.createSecurityConfigurationFromRequest(null, false)).andReturn(null).anyTimes();
  }

  @After
  public void tearDown() {
    reset(request, topologyManager, topologyFactory, topologyRequest, blueprintFactory, securityFactory,
      requestStatusResponse, blueprint);
  }

  private void replayAll() {
    replay(request, topologyManager, topologyFactory, topologyRequest, blueprintFactory, securityFactory,
      requestStatusResponse, blueprint);
  }

  private void verifyAll() {
    verify(request, topologyManager, topologyFactory, topologyRequest, blueprintFactory, securityFactory,
      requestStatusResponse, blueprint);
  }

  @Test
  public void testCreateResource_blueprint() throws Exception {
    Set<Map<String, Object>> requestProperties = createBlueprintRequestProperties(CLUSTER_NAME, BLUEPRINT_NAME);
    Map<String, Object> properties = requestProperties.iterator().next();
    Map<String, String> requestInfoProperties = new HashMap<String, String>();
    requestInfoProperties.put(Request.REQUEST_INFO_BODY_PROPERTY, "{}");

    // set expectations
    expect(request.getProperties()).andReturn(requestProperties).anyTimes();
    expect(request.getRequestInfoProperties()).andReturn(requestInfoProperties).anyTimes();

    expect(securityFactory.createSecurityConfigurationFromRequest(anyObject(HashMap.class), anyBoolean())).andReturn(null)
      .once();
    expect(topologyFactory.createProvisionClusterRequest(properties, null)).andReturn(topologyRequest).once();
    expect(topologyManager.provisionCluster(topologyRequest)).andReturn(requestStatusResponse).once();
    expect(requestStatusResponse.getRequestId()).andReturn(5150L).anyTimes();

    replayAll();
    RequestStatus requestStatus = provider.createResources(request);
    assertEquals(5150L, requestStatus.getRequestResource().getPropertyValue(PropertyHelper.getPropertyId("Requests", "id")));
    assertEquals(Resource.Type.Request, requestStatus.getRequestResource().getType());
    assertEquals("Accepted", requestStatus.getRequestResource().getPropertyValue(PropertyHelper.getPropertyId("Requests", "status")));

    verifyAll();
  }

  @Test
  public void testCreateResource_blueprint_With_ProvisionAction() throws Exception {
    Set<Map<String, Object>> requestProperties = createBlueprintRequestProperties(CLUSTER_NAME, BLUEPRINT_NAME);
    Map<String, Object> properties = requestProperties.iterator().next();
    properties.put(ProvisionClusterRequest.PROVISION_ACTION_PROPERTY, "INSTALL_ONLY");
    Map<String, String> requestInfoProperties = new HashMap<String, String>();
    requestInfoProperties.put(Request.REQUEST_INFO_BODY_PROPERTY, "{}");

    // set expectations
    expect(request.getProperties()).andReturn(requestProperties).anyTimes();
    expect(request.getRequestInfoProperties()).andReturn(requestInfoProperties).anyTimes();

    expect(securityFactory.createSecurityConfigurationFromRequest(anyObject(HashMap.class), anyBoolean())).andReturn(null)
      .once();
    expect(topologyFactory.createProvisionClusterRequest(properties, null)).andReturn(topologyRequest).once();
    expect(topologyManager.provisionCluster(topologyRequest)).andReturn(requestStatusResponse).once();
    expect(requestStatusResponse.getRequestId()).andReturn(5150L).anyTimes();

    replayAll();
    RequestStatus requestStatus = provider.createResources(request);
    assertEquals(5150L, requestStatus.getRequestResource().getPropertyValue(PropertyHelper.getPropertyId("Requests", "id")));
    assertEquals(Resource.Type.Request, requestStatus.getRequestResource().getType());
    assertEquals("Accepted", requestStatus.getRequestResource().getPropertyValue(PropertyHelper.getPropertyId("Requests", "status")));

    verifyAll();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateResource_blueprint_withInvalidSecurityConfiguration() throws Exception {
    Set<Map<String, Object>> requestProperties = createBlueprintRequestProperties(CLUSTER_NAME, BLUEPRINT_NAME);
    Map<String, Object> properties = requestProperties.iterator().next();
    Map<String, String> requestInfoProperties = new HashMap<String, String>();
    requestInfoProperties.put(Request.REQUEST_INFO_BODY_PROPERTY, "{\"security\" : {\n\"type\" : \"NONE\"," +
      "\n\"kerberos_descriptor_reference\" : " + "\"testRef\"\n}}");
    SecurityConfiguration blueprintSecurityConfiguration = new SecurityConfiguration(SecurityType.KERBEROS, "testRef",
      null);
    SecurityConfiguration securityConfiguration = new SecurityConfiguration(SecurityType.NONE, null, null);

    // set expectations
    expect(request.getProperties()).andReturn(requestProperties).anyTimes();
    expect(request.getRequestInfoProperties()).andReturn(requestInfoProperties).anyTimes();

    expect(securityFactory.createSecurityConfigurationFromRequest(anyObject(HashMap.class), anyBoolean())).andReturn
      (securityConfiguration).once();
    expect(topologyFactory.createProvisionClusterRequest(properties, securityConfiguration)).andReturn(topologyRequest).once();
    expect(topologyRequest.getBlueprint()).andReturn(blueprint).anyTimes();
    expect(blueprint.getSecurity()).andReturn(blueprintSecurityConfiguration).anyTimes();
    expect(requestStatusResponse.getRequestId()).andReturn(5150L).anyTimes();

    replayAll();
    RequestStatus requestStatus = provider.createResources(request);
  }

  @Test
  public void testCreateResource_blueprint_withSecurityConfiguration() throws Exception {
    Set<Map<String, Object>> requestProperties = createBlueprintRequestProperties(CLUSTER_NAME, BLUEPRINT_NAME);
    Map<String, Object> properties = requestProperties.iterator().next();
    SecurityConfiguration securityConfiguration = new SecurityConfiguration(SecurityType.KERBEROS, "testRef", null);

    Map<String, String> requestInfoProperties = new HashMap<String, String>();
    requestInfoProperties.put(Request.REQUEST_INFO_BODY_PROPERTY, "{\"security\" : {\n\"type\" : \"KERBEROS\",\n\"kerberos_descriptor_reference\" : " +
      "\"testRef\"\n}}");

        // set expectations
    expect(request.getProperties()).andReturn(requestProperties).anyTimes();
    expect(request.getRequestInfoProperties()).andReturn(requestInfoProperties).anyTimes();

    expect(topologyFactory.createProvisionClusterRequest(properties, securityConfiguration)).andReturn(topologyRequest).once();
    expect(securityFactory.createSecurityConfigurationFromRequest(anyObject(HashMap.class), anyBoolean())).andReturn
      (securityConfiguration).once();
    expect(topologyManager.provisionCluster(topologyRequest)).andReturn(requestStatusResponse).once();
    expect(requestStatusResponse.getRequestId()).andReturn(5150L).anyTimes();

    replayAll();
    RequestStatus requestStatus = provider.createResources(request);
    assertEquals(5150L, requestStatus.getRequestResource().getPropertyValue(PropertyHelper.getPropertyId("Requests", "id")));
    assertEquals(Resource.Type.Request, requestStatus.getRequestResource().getType());
    assertEquals("Accepted", requestStatus.getRequestResource().getPropertyValue(PropertyHelper.getPropertyId("Requests", "status")));

    verifyAll();
  }

  @Test(expected=IllegalArgumentException.class)
  public void testCreateResource_blueprint__InvalidRequest() throws Exception {
    Set<Map<String, Object>> requestProperties = createBlueprintRequestProperties(CLUSTER_NAME, BLUEPRINT_NAME);
    Map<String, Object> properties = requestProperties.iterator().next();

    // set expectations
    expect(request.getProperties()).andReturn(requestProperties).anyTimes();
    // throw exception from topology request factory an assert that the correct exception is thrown from resource provider
    expect(topologyFactory.createProvisionClusterRequest(properties, null)).andThrow(new InvalidTopologyException
      ("test"));

    replayAll();
    provider.createResources(request);
  }

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
  public void testCreateResourcesWithRetry() throws Exception {
    RetryHelper.init(3);
    Resource.Type type = Resource.Type.Cluster;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    managementController.createCluster(
        AbstractResourceProviderTest.Matcher.getClusterRequest(null, "Cluster100", "HDP-0.1", null));
    expectLastCall().andThrow(new DatabaseException("test"){}).once().andVoid().atLeastOnce();

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

    RetryHelper.init(0);

  }

  @Test
  public void testGetResources() throws Exception{
    Resource.Type type = Resource.Type.Cluster;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createMock(Clusters.class);

    Set<ClusterResponse> allResponse = new HashSet<ClusterResponse>();
    allResponse.add(new ClusterResponse(100L, "Cluster100", State.INSTALLED, SecurityType.NONE, null, null, null, null));
    allResponse.add(new ClusterResponse(101L, "Cluster101", State.INSTALLED, SecurityType.NONE, null, null, null, null));
    allResponse.add(new ClusterResponse(102L, "Cluster102", State.INSTALLED, SecurityType.NONE, null, null, null, null));
    allResponse.add(new ClusterResponse(103L, "Cluster103", State.INSTALLED, SecurityType.NONE, null, null, null, null));
    allResponse.add(new ClusterResponse(104L, "Cluster104", State.INSTALLED, SecurityType.NONE, null, null, null, null));

    Set<ClusterResponse> nameResponse = new HashSet<ClusterResponse>();
    nameResponse.add(new ClusterResponse(102L, "Cluster102", State.INSTALLED, SecurityType.NONE, null, null, null, null));

    Set<ClusterResponse> idResponse = new HashSet<ClusterResponse>();
    idResponse.add(new ClusterResponse(103L, "Cluster103", State.INSTALLED, SecurityType.NONE, null, null, null, null));

    // set expectations
    Capture<Set<ClusterRequest>> captureClusterRequests = new Capture<Set<ClusterRequest>>();

    expect(managementController.getClusters(capture(captureClusterRequests))).andReturn(allResponse).once();
    expect(managementController.getClusters(capture(captureClusterRequests))).andReturn(nameResponse).once();
    expect(managementController.getClusters(capture(captureClusterRequests))).andReturn(idResponse).once();

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();

    expect(clusters.checkPermission("Cluster100", true)).andReturn(true).anyTimes();
    expect(clusters.checkPermission("Cluster101", true)).andReturn(true).anyTimes();
    expect(clusters.checkPermission("Cluster102", true)).andReturn(true).anyTimes();
    expect(clusters.checkPermission("Cluster103", true)).andReturn(true).anyTimes();
    expect(clusters.checkPermission("Cluster104", true)).andReturn(false).anyTimes();

    // replay
    replay(managementController, clusters);

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

    Assert.assertEquals(4, resources.size());
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
    verify(managementController, clusters);
  }

  @Test
  public void testUpdateResources() throws Exception{
    Resource.Type type = Resource.Type.Cluster;

    Clusters clusters = createMock(Clusters.class);

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    Set<ClusterResponse> nameResponse = new HashSet<ClusterResponse>();
    nameResponse.add(new ClusterResponse(102L, "Cluster102", State.INIT, SecurityType.NONE, null, null, null, null));

    Map<String, String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "Called from a test");

    // set expectations
    expect(managementController.getClusters(EasyMock.<Set<ClusterRequest>>anyObject())).andReturn(nameResponse).once();
    expect(managementController.updateClusters(
        AbstractResourceProviderTest.Matcher.getClusterRequestSet(102L, "Cluster102", State.INSTALLED.name(), SecurityType.NONE, "HDP-0.1", null), eq(mapRequestProps))).
        andReturn(response).once();

    expect(managementController.updateClusters(
        AbstractResourceProviderTest.Matcher.getClusterRequestSet(103L, null, null, null, "HDP-0.1", null), eq(mapRequestProps))).
        andReturn(response).once();

    expect(managementController.getClusterUpdateResults(anyObject(ClusterRequest.class))).andReturn(null).anyTimes();

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();

    expect(clusters.checkPermission("Cluster102", false)).andReturn(true).anyTimes();
    expect(clusters.checkPermission("Cluster102", true)).andReturn(true).anyTimes();
    expect(clusters.checkPermission("Cluster103", false)).andReturn(true).anyTimes();
    expect(clusters.checkPermission("Cluster103", true)).andReturn(true).anyTimes();
    expect(clusters.checkPermission(null, false)).andReturn(true).anyTimes();

    // replay
    replay(managementController, response, clusters);

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
    verify(managementController, response, clusters);
  }

  @Test
  public void testUpdateWithConfiguration() throws Exception {
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createMock(Clusters.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    Set<ClusterResponse> nameResponse = new HashSet<ClusterResponse>();
    nameResponse.add(new ClusterResponse(100L, "Cluster100", State.INSTALLED, SecurityType.NONE, null, null, null, null));

    Map<String, String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "Called from a test");

    // set expectations
    expect(managementController.getClusters(EasyMock.<Set<ClusterRequest>>anyObject())).andReturn(nameResponse).times(2);
    expect(managementController.updateClusters(Collections.singleton(EasyMock.anyObject(ClusterRequest.class)),
        eq(mapRequestProps))).andReturn(response).times(1);
    expect(managementController.getClusterUpdateResults(anyObject(ClusterRequest.class))).andReturn(null).anyTimes();

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();

    expect(clusters.checkPermission("Cluster100", true)).andReturn(true).anyTimes();
    expect(clusters.checkPermission("Cluster100", false)).andReturn(true).anyTimes();

    // replay
    replay(managementController, response, clusters);

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
    verify(managementController, response, clusters);
  }

  @Test
  public void testDeleteResources() throws Exception{
    Resource.Type type = Resource.Type.Cluster;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createMock(Clusters.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    // set expectations
    managementController.deleteCluster(
        AbstractResourceProviderTest.Matcher.getClusterRequest(null, "Cluster102", null, null));
    managementController.deleteCluster(
        AbstractResourceProviderTest.Matcher.getClusterRequest(103L, null, null, null));

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();

    expect(clusters.checkPermission("Cluster102", false)).andReturn(true).anyTimes();
    expect(clusters.checkPermission(null, false)).andReturn(true).anyTimes();

    // replay
    replay(managementController, response, clusters);

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
    verify(managementController, response, clusters);
  }

  //todo: What are these testing and where do they go?
  //todo: these were added when the new security type property was added to enable/disable kerberos
//  @Test
//  public void testSetMissingConfigurationsOozieIncluded() throws Exception {
//    EasyMockSupport mockSupport = new EasyMockSupport();
//
//    AmbariManagementController mockMgmtController =
//      mockSupport.createMock(AmbariManagementController.class);
//    ResourceProvider mockServiceProvider =
//      mockSupport.createMock(ResourceProvider.class);
//    ResourceProvider mockComponentProvider =
//      mockSupport.createMock(ResourceProvider.class);
//    ResourceProvider mockHostProvider =
//      mockSupport.createMock(ResourceProvider.class);
//    ResourceProvider mockHostComponentProvider =
//      mockSupport.createMock(ResourceProvider.class);
//    ResourceProvider mockConfigGroupProvider =
//      mockSupport.createMock(ResourceProvider.class);
//    AmbariManagementController mockManagementController =
//      mockSupport.createMock(AmbariManagementController.class);
//    StackServiceResponse mockStackServiceResponseOne =
//      mockSupport.createMock(StackServiceResponse.class);
//    StackServiceComponentResponse mockStackComponentResponse =
//      mockSupport.createMock(StackServiceComponentResponse.class);
//    AmbariMetaInfo mockAmbariMetaInfo =
//      mockSupport.createMock(AmbariMetaInfo.class);
//
//    expect(mockStackComponentResponse.getComponentName()).andReturn("OOZIE_SERVER").atLeastOnce();
//    expect(mockStackComponentResponse.getCardinality()).andReturn("1");
//    expect(mockStackComponentResponse.getAutoDeploy()).andReturn(new AutoDeployInfo());
//
//
//    expect(mockStackServiceResponseOne.getServiceName()).andReturn("OOZIE").atLeastOnce();
//    expect(mockStackServiceResponseOne.getExcludedConfigTypes()).andReturn(Collections.<String>emptySet()).atLeastOnce();
//    expect(mockManagementController.getStackServices(isA(Set.class))).andReturn(Collections.singleton(mockStackServiceResponseOne));
//    expect(mockManagementController.getStackComponents(isA(Set.class))).andReturn(Collections.singleton(mockStackComponentResponse));
//    expect(mockManagementController.getStackConfigurations(isA(Set.class))).andReturn(Collections.<StackConfigurationResponse>emptySet());
//    expect(mockManagementController.getStackLevelConfigurations(isA(Set.class))).andReturn(Collections.<StackConfigurationResponse>emptySet());
//
//    expect(mockAmbariMetaInfo.getComponentDependencies("HDP", "2.1", "OOZIE", "OOZIE_SERVER")).andReturn(Collections.<DependencyInfo>emptyList());
//
//    mockSupport.replayAll();
//
//
//    ClusterResourceProvider.init(null, mockAmbariMetaInfo, null);
//
//    Stack stack =
//      new Stack("HDP", "2.1", mockManagementController);
//
//    ClusterResourceProvider clusterResourceProvider =
//      new TestClusterResourceProvider(mockMgmtController, mockServiceProvider,
//        mockComponentProvider, mockHostProvider, mockHostComponentProvider, mockConfigGroupProvider);
//
//
//    HostGroupEntity hostGroup = new HostGroupEntity();
//    hostGroup.setComponents(Collections.<HostGroupComponentEntity>emptyList());
//    HostGroupConfigEntity configEntity = new HostGroupConfigEntity();
//    configEntity.setConfigData("");
//
//    hostGroup.setConfigurations(Collections.singletonList(configEntity));
//    BaseBlueprintProcessor.HostGroupImpl hostGroupImpl =
//      new BaseBlueprintProcessor.HostGroupImpl(hostGroup, stack, null);
//    hostGroupImpl.addComponent("OOZIE_SERVER");
//
//    // add empty map for core-site, to simulate this configuration entry
//    clusterResourceProvider.getClusterConfigurations().put("core-site", new HashMap<String, String>());
//    clusterResourceProvider.getClusterConfigurations().put("oozie-env", new HashMap<String, String>());
//    clusterResourceProvider.getClusterConfigurations().get("oozie-env").put("oozie_user", "oozie");
//
//    //clusterResourceProvider.setMissingConfigurations(Collections.singletonMap("host_group_one", hostGroupImpl));
//
//    Map<String, String> mapCoreSiteConfig =
//      clusterResourceProvider.getClusterConfigurations().get("core-site");
//
//    assertNotNull("core-site map was null.", mapCoreSiteConfig);
//    assertEquals("Incorrect number of entries in the core-site config map",
//                 2, mapCoreSiteConfig.size());
//    assertEquals("Incorrect value for proxy hosts",
//                 "*", mapCoreSiteConfig.get("hadoop.proxyuser.oozie.hosts"));
//    assertEquals("Incorrect value for proxy hosts",
//      "users", mapCoreSiteConfig.get("hadoop.proxyuser.oozie.groups"));
//
//    mockSupport.verifyAll();
//  }
//
//
//  @Test
//  public void testSetMissingConfigurationsFalconIncluded() throws Exception {
//    EasyMockSupport mockSupport = new EasyMockSupport();
//
//    AmbariManagementController mockMgmtController =
//      mockSupport.createMock(AmbariManagementController.class);
//    ResourceProvider mockServiceProvider =
//      mockSupport.createMock(ResourceProvider.class);
//    ResourceProvider mockComponentProvider =
//      mockSupport.createMock(ResourceProvider.class);
//    ResourceProvider mockHostProvider =
//      mockSupport.createMock(ResourceProvider.class);
//    ResourceProvider mockHostComponentProvider =
//      mockSupport.createMock(ResourceProvider.class);
//    ResourceProvider mockConfigGroupProvider =
//      mockSupport.createMock(ResourceProvider.class);
//    AmbariManagementController mockManagementController =
//      mockSupport.createMock(AmbariManagementController.class);
//    StackServiceResponse mockStackServiceResponseOne =
//      mockSupport.createMock(StackServiceResponse.class);
//    StackServiceComponentResponse mockStackComponentResponse =
//      mockSupport.createMock(StackServiceComponentResponse.class);
//    AmbariMetaInfo mockAmbariMetaInfo =
//      mockSupport.createMock(AmbariMetaInfo.class);
//
//    expect(mockStackComponentResponse.getComponentName()).andReturn("FALCON_SERVER").atLeastOnce();
//    expect(mockStackComponentResponse.getCardinality()).andReturn("1");
//    expect(mockStackComponentResponse.getAutoDeploy()).andReturn(new AutoDeployInfo());
//
//
//    expect(mockStackServiceResponseOne.getServiceName()).andReturn("FALCON").atLeastOnce();
//    expect(mockStackServiceResponseOne.getExcludedConfigTypes()).andReturn(Collections.<String>emptySet()).atLeastOnce();
//    expect(mockManagementController.getStackServices(isA(Set.class))).andReturn(Collections.singleton(mockStackServiceResponseOne));
//    expect(mockManagementController.getStackComponents(isA(Set.class))).andReturn(Collections.singleton(mockStackComponentResponse));
//    expect(mockManagementController.getStackConfigurations(isA(Set.class))).andReturn(Collections.<StackConfigurationResponse>emptySet());
//    expect(mockManagementController.getStackLevelConfigurations(isA(Set.class))).andReturn(Collections.<StackConfigurationResponse>emptySet());
//
//    expect(mockAmbariMetaInfo.getComponentDependencies("HDP", "2.1", "FALCON", "FALCON_SERVER")).andReturn(Collections.<DependencyInfo>emptyList());
//
//    mockSupport.replayAll();
//
//    ClusterResourceProvider.init(null, mockAmbariMetaInfo, null);
//
//    Stack stack =
//      new Stack("HDP", "2.1", mockManagementController);
//
//    ClusterResourceProvider clusterResourceProvider =
//      new TestClusterResourceProvider(mockMgmtController, mockServiceProvider,
//        mockComponentProvider, mockHostProvider, mockHostComponentProvider, mockConfigGroupProvider);
//
//    HostGroupEntity hostGroup = new HostGroupEntity();
//    hostGroup.setComponents(Collections.<HostGroupComponentEntity>emptyList());
//    HostGroupConfigEntity configEntity = new HostGroupConfigEntity();
//    configEntity.setConfigData("");
//
//    hostGroup.setConfigurations(Collections.singletonList(configEntity));
//    BaseBlueprintProcessor.HostGroupImpl hostGroupImpl =
//      new BaseBlueprintProcessor.HostGroupImpl(hostGroup, stack, null);
//    hostGroupImpl.addComponent("FALCON_SERVER");
//
//    // add empty map for core-site, to simulate this configuration entry
//    clusterResourceProvider.getClusterConfigurations().put("core-site", new HashMap<String, String>());
//    clusterResourceProvider.getClusterConfigurations().put("falcon-env", new HashMap<String, String>());
//    clusterResourceProvider.getClusterConfigurations().get("falcon-env").put("falcon_user", "falcon");
//
//    //clusterResourceProvider.setMissingConfigurations(Collections.singletonMap("host_group_one", hostGroupImpl));
//
//    Map<String, String> mapCoreSiteConfig =
//      clusterResourceProvider.getClusterConfigurations().get("core-site");
//
//    assertNotNull("core-site map was null.", mapCoreSiteConfig);
//    assertEquals("Incorrect number of entries in the core-site config map",
//      2, mapCoreSiteConfig.size());
//    assertEquals("Incorrect value for proxy hosts",
//      "*", mapCoreSiteConfig.get("hadoop.proxyuser.falcon.hosts"));
//    assertEquals("Incorrect value for proxy hosts",
//      "users", mapCoreSiteConfig.get("hadoop.proxyuser.falcon.groups"));
//
//    mockSupport.verifyAll();
//  }
//
//
//  @Test
//  public void testSetMissingConfigurationsOozieNotIncluded() throws Exception {
//    EasyMockSupport mockSupport = new EasyMockSupport();
//
//    AmbariManagementController mockMgmtController =
//      mockSupport.createMock(AmbariManagementController.class);
//    ResourceProvider mockServiceProvider =
//      mockSupport.createMock(ResourceProvider.class);
//    ResourceProvider mockComponentProvider =
//      mockSupport.createMock(ResourceProvider.class);
//    ResourceProvider mockHostProvider =
//      mockSupport.createMock(ResourceProvider.class);
//    ResourceProvider mockHostComponentProvider =
//      mockSupport.createMock(ResourceProvider.class);
//    ResourceProvider mockConfigGroupProvider =
//      mockSupport.createMock(ResourceProvider.class);
//    AmbariManagementController mockManagementController =
//      mockSupport.createMock(AmbariManagementController.class);
//    StackServiceResponse mockStackServiceResponseOne =
//      mockSupport.createMock(StackServiceResponse.class);
//    StackServiceComponentResponse mockStackComponentResponse =
//      mockSupport.createMock(StackServiceComponentResponse.class);
//    AmbariMetaInfo mockAmbariMetaInfo =
//      mockSupport.createMock(AmbariMetaInfo.class);
//
//    expect(mockStackComponentResponse.getComponentName()).andReturn("OOZIE_SERVER");
//    expect(mockStackComponentResponse.getCardinality()).andReturn("1");
//    expect(mockStackComponentResponse.getAutoDeploy()).andReturn(new AutoDeployInfo());
//
//
//    expect(mockStackServiceResponseOne.getServiceName()).andReturn("OOZIE").atLeastOnce();
//    expect(mockStackServiceResponseOne.getExcludedConfigTypes()).andReturn(Collections.<String>emptySet()).atLeastOnce();
//    expect(mockManagementController.getStackServices(isA(Set.class))).andReturn(Collections.singleton(mockStackServiceResponseOne));
//    expect(mockManagementController.getStackComponents(isA(Set.class))).andReturn(Collections.singleton(mockStackComponentResponse));
//    expect(mockManagementController.getStackConfigurations(isA(Set.class))).andReturn(Collections.<StackConfigurationResponse>emptySet());
//    expect(mockManagementController.getStackLevelConfigurations(isA(Set.class))).andReturn(Collections.<StackConfigurationResponse>emptySet());
//
//    expect(mockAmbariMetaInfo.getComponentDependencies("HDP", "2.1", "OOZIE", "OOZIE_SERVER")).andReturn(Collections.<DependencyInfo>emptyList());
//
//    mockSupport.replayAll();
//
//    ClusterResourceProvider.init(null, mockAmbariMetaInfo, null);
//
//    Stack stack =
//      new Stack("HDP", "2.1", mockManagementController);
//
//    ClusterResourceProvider clusterResourceProvider =
//      new TestClusterResourceProvider(mockMgmtController, mockServiceProvider,
//        mockComponentProvider, mockHostProvider, mockHostComponentProvider, mockConfigGroupProvider);
//
//
//    HostGroupEntity hostGroup = new HostGroupEntity();
//    hostGroup.setComponents(Collections.<HostGroupComponentEntity>emptyList());
//    HostGroupConfigEntity configEntity = new HostGroupConfigEntity();
//    configEntity.setConfigData("");
//
//    hostGroup.setConfigurations(Collections.singletonList(configEntity));
//    BaseBlueprintProcessor.HostGroupImpl hostGroupImpl =
//      new BaseBlueprintProcessor.HostGroupImpl(hostGroup, stack, null);
//    hostGroupImpl.addComponent("COMPONENT_ONE");
//
//    // add empty map for core-site, to simulate this configuration entry
//    clusterResourceProvider.getClusterConfigurations().put("core-site", new HashMap<String, String>());
//
//    //clusterResourceProvider.setMissingConfigurations(Collections.singletonMap("host_group_one", hostGroupImpl));
//
//    Map<String, String> mapCoreSiteConfig =
//      clusterResourceProvider.getClusterConfigurations().get("core-site");
//
//    assertNotNull("core-site map was null.", mapCoreSiteConfig);
//    assertEquals("Incorrect number of entries in the core-site config map",
//                0, mapCoreSiteConfig.size());
//
//    mockSupport.verifyAll();
//
//  }
//
//
//  @Test
//  public void testSetMissingConfigurationsFalconNotIncluded() throws Exception {
//    EasyMockSupport mockSupport = new EasyMockSupport();
//
//    AmbariManagementController mockMgmtController =
//      mockSupport.createMock(AmbariManagementController.class);
//    ResourceProvider mockServiceProvider =
//      mockSupport.createMock(ResourceProvider.class);
//    ResourceProvider mockComponentProvider =
//      mockSupport.createMock(ResourceProvider.class);
//    ResourceProvider mockHostProvider =
//      mockSupport.createMock(ResourceProvider.class);
//    ResourceProvider mockHostComponentProvider =
//      mockSupport.createMock(ResourceProvider.class);
//    ResourceProvider mockConfigGroupProvider =
//      mockSupport.createMock(ResourceProvider.class);
//    AmbariManagementController mockManagementController =
//      mockSupport.createMock(AmbariManagementController.class);
//    StackServiceResponse mockStackServiceResponseOne =
//      mockSupport.createMock(StackServiceResponse.class);
//    StackServiceComponentResponse mockStackComponentResponse =
//      mockSupport.createMock(StackServiceComponentResponse.class);
//    AmbariMetaInfo mockAmbariMetaInfo =
//      mockSupport.createMock(AmbariMetaInfo.class);
//
//    expect(mockStackComponentResponse.getComponentName()).andReturn("FALCON_SERVER");
//    expect(mockStackComponentResponse.getCardinality()).andReturn("1");
//    expect(mockStackComponentResponse.getAutoDeploy()).andReturn(new AutoDeployInfo());
//
//    expect(mockStackServiceResponseOne.getServiceName()).andReturn("FALCON").atLeastOnce();
//    expect(mockStackServiceResponseOne.getExcludedConfigTypes()).andReturn(Collections.<String>emptySet()).atLeastOnce();
//
//    expect(mockManagementController.getStackServices(isA(Set.class))).andReturn(Collections.singleton(mockStackServiceResponseOne));
//    expect(mockManagementController.getStackComponents(isA(Set.class))).andReturn(Collections.singleton(mockStackComponentResponse));
//    expect(mockManagementController.getStackConfigurations(isA(Set.class))).andReturn(Collections.<StackConfigurationResponse>emptySet());
//    expect(mockManagementController.getStackLevelConfigurations(isA(Set.class))).andReturn(Collections.<StackConfigurationResponse>emptySet());
//
//    expect(mockAmbariMetaInfo.getComponentDependencies("HDP", "2.1", "FALCON", "FALCON_SERVER")).andReturn(Collections.<DependencyInfo>emptyList());
//
//    mockSupport.replayAll();
//
//    ClusterResourceProvider.init(null, mockAmbariMetaInfo, null);
//
//    Stack stack =
//      new Stack("HDP", "2.1", mockManagementController);
//
//    ClusterResourceProvider clusterResourceProvider =
//      new TestClusterResourceProvider(mockMgmtController, mockServiceProvider,
//        mockComponentProvider, mockHostProvider, mockHostComponentProvider, mockConfigGroupProvider);
//
//    HostGroupEntity hostGroup = new HostGroupEntity();
//    hostGroup.setComponents(Collections.<HostGroupComponentEntity>emptyList());
//    HostGroupConfigEntity configEntity = new HostGroupConfigEntity();
//    configEntity.setConfigData("");
//
//    hostGroup.setConfigurations(Collections.singletonList(configEntity));
//    BaseBlueprintProcessor.HostGroupImpl hostGroupImpl =
//      new BaseBlueprintProcessor.HostGroupImpl(hostGroup, stack, null);
//    // blueprint request will not include a reference to FALCON_SERVER
//    hostGroupImpl.addComponent("COMPONENT_ONE");
//
//    // add empty map for core-site, to simulate this configuration entry
//    clusterResourceProvider.getClusterConfigurations().put("core-site", new HashMap<String, String>());
//
//    //clusterResourceProvider.setMissingConfigurations(Collections.singletonMap("host_group_one", hostGroupImpl));
//
//    Map<String, String> mapCoreSiteConfig =
//      clusterResourceProvider.getClusterConfigurations().get("core-site");
//
//    assertNotNull("core-site map was null.", mapCoreSiteConfig);
//    assertEquals("Incorrect number of entries in the core-site config map",
//      0, mapCoreSiteConfig.size());
//
//    mockSupport.verifyAll();
//
//  }
//
//
//  @Test
//  public void testSetMissingConfigurationsHiveNotIncluded() throws Exception {
//    EasyMockSupport mockSupport = new EasyMockSupport();
//
//    AmbariManagementController mockMgmtController =
//      mockSupport.createMock(AmbariManagementController.class);
//    ResourceProvider mockServiceProvider =
//      mockSupport.createMock(ResourceProvider.class);
//    ResourceProvider mockComponentProvider =
//      mockSupport.createMock(ResourceProvider.class);
//    ResourceProvider mockHostProvider =
//      mockSupport.createMock(ResourceProvider.class);
//    ResourceProvider mockHostComponentProvider =
//      mockSupport.createMock(ResourceProvider.class);
//    ResourceProvider mockConfigGroupProvider =
//      mockSupport.createMock(ResourceProvider.class);
//    AmbariManagementController mockManagementController =
//      mockSupport.createMock(AmbariManagementController.class);
//    StackServiceResponse mockStackServiceResponseOne =
//      mockSupport.createMock(StackServiceResponse.class);
//    StackServiceComponentResponse mockStackComponentResponse =
//      mockSupport.createMock(StackServiceComponentResponse.class);
//    AmbariMetaInfo mockAmbariMetaInfo =
//      mockSupport.createMock(AmbariMetaInfo.class);
//
//    expect(mockStackComponentResponse.getComponentName()).andReturn("HIVE_SERVER");
//    expect(mockStackComponentResponse.getCardinality()).andReturn("1");
//    expect(mockStackComponentResponse.getAutoDeploy()).andReturn(new AutoDeployInfo());
//
//    expect(mockStackServiceResponseOne.getServiceName()).andReturn("HIVE").atLeastOnce();
//    expect(mockStackServiceResponseOne.getExcludedConfigTypes()).andReturn(Collections.<String>emptySet()).atLeastOnce();
//    expect(mockManagementController.getStackServices(isA(Set.class))).andReturn(Collections.singleton(mockStackServiceResponseOne));
//    expect(mockManagementController.getStackComponents(isA(Set.class))).andReturn(Collections.singleton(mockStackComponentResponse));
//    expect(mockManagementController.getStackConfigurations(isA(Set.class))).andReturn(Collections.<StackConfigurationResponse>emptySet());
//    expect(mockManagementController.getStackLevelConfigurations(isA(Set.class))).andReturn(Collections.<StackConfigurationResponse>emptySet());
//
//    expect(mockAmbariMetaInfo.getComponentDependencies("HDP", "2.1", "HIVE", "HIVE_SERVER")).andReturn(Collections.<DependencyInfo>emptyList());
//
//    mockSupport.replayAll();
//
//    ClusterResourceProvider.init(null, mockAmbariMetaInfo, null);
//
//    Stack stack =
//      new Stack("HDP", "2.1", mockManagementController);
//
//    ClusterResourceProvider clusterResourceProvider =
//      new TestClusterResourceProvider(mockMgmtController, mockServiceProvider,
//        mockComponentProvider, mockHostProvider, mockHostComponentProvider, mockConfigGroupProvider);
//
//    HostGroupEntity hostGroup = new HostGroupEntity();
//    hostGroup.setComponents(Collections.<HostGroupComponentEntity>emptyList());
//    HostGroupConfigEntity configEntity = new HostGroupConfigEntity();
//    configEntity.setConfigData("");
//
//    hostGroup.setConfigurations(Collections.singletonList(configEntity));
//    BaseBlueprintProcessor.HostGroupImpl hostGroupImpl =
//      new BaseBlueprintProcessor.HostGroupImpl(hostGroup, stack, null);
//    // blueprint request will not include a reference to a HIVE component
//    hostGroupImpl.addComponent("COMPONENT_ONE");
//
//    // add empty map for core-site, to simulate this configuration entry
//    clusterResourceProvider.getClusterConfigurations().put("core-site", new HashMap<String, String>());
//
//    //clusterResourceProvider.setMissingConfigurations(Collections.singletonMap("host_group_one", hostGroupImpl));
//
//    Map<String, String> mapCoreSiteConfig =
//      clusterResourceProvider.getClusterConfigurations().get("core-site");
//
//    assertNotNull("core-site map was null.", mapCoreSiteConfig);
//    assertEquals("Incorrect number of entries in the core-site config map",
//      0, mapCoreSiteConfig.size());
//
//    mockSupport.verifyAll();
//
//  }
//
//
//  @Test
//  public void testSetMissingConfigurationsHBaseNotIncluded() throws Exception {
//    EasyMockSupport mockSupport = new EasyMockSupport();
//
//    AmbariManagementController mockMgmtController =
//      mockSupport.createMock(AmbariManagementController.class);
//    ResourceProvider mockServiceProvider =
//      mockSupport.createMock(ResourceProvider.class);
//    ResourceProvider mockComponentProvider =
//      mockSupport.createMock(ResourceProvider.class);
//    ResourceProvider mockHostProvider =
//      mockSupport.createMock(ResourceProvider.class);
//    ResourceProvider mockHostComponentProvider =
//      mockSupport.createMock(ResourceProvider.class);
//    ResourceProvider mockConfigGroupProvider =
//      mockSupport.createMock(ResourceProvider.class);
//    AmbariManagementController mockManagementController =
//      mockSupport.createMock(AmbariManagementController.class);
//    StackServiceResponse mockStackServiceResponseOne =
//      mockSupport.createMock(StackServiceResponse.class);
//    StackServiceComponentResponse mockStackComponentResponse =
//      mockSupport.createMock(StackServiceComponentResponse.class);
//    AmbariMetaInfo mockAmbariMetaInfo =
//      mockSupport.createMock(AmbariMetaInfo.class);
//
//    expect(mockStackComponentResponse.getComponentName()).andReturn("HBASE_SERVER");
//    expect(mockStackComponentResponse.getCardinality()).andReturn("1");
//    expect(mockStackComponentResponse.getAutoDeploy()).andReturn(new AutoDeployInfo());
//
//    expect(mockStackServiceResponseOne.getServiceName()).andReturn("HBASE").atLeastOnce();
//    expect(mockStackServiceResponseOne.getExcludedConfigTypes()).andReturn(Collections.<String>emptySet()).atLeastOnce();
//
//    expect(mockManagementController.getStackServices(isA(Set.class))).andReturn(Collections.singleton(mockStackServiceResponseOne));
//    expect(mockManagementController.getStackComponents(isA(Set.class))).andReturn(Collections.singleton(mockStackComponentResponse));
//    expect(mockManagementController.getStackConfigurations(isA(Set.class))).andReturn(Collections.<StackConfigurationResponse>emptySet());
//    expect(mockManagementController.getStackLevelConfigurations(isA(Set.class))).andReturn(Collections.<StackConfigurationResponse>emptySet());
//
//    expect(mockAmbariMetaInfo.getComponentDependencies("HDP", "2.1", "HBASE", "HBASE_SERVER")).andReturn(Collections.<DependencyInfo>emptyList());
//
//    mockSupport.replayAll();
//
//    ClusterResourceProvider.init(null, mockAmbariMetaInfo, null);
//
//    Stack stack =
//      new Stack("HDP", "2.1", mockManagementController);
//
//    ClusterResourceProvider clusterResourceProvider =
//      new TestClusterResourceProvider(mockMgmtController, mockServiceProvider,
//        mockComponentProvider, mockHostProvider, mockHostComponentProvider, mockConfigGroupProvider);
//
//    HostGroupEntity hostGroup = new HostGroupEntity();
//    hostGroup.setComponents(Collections.<HostGroupComponentEntity>emptyList());
//    HostGroupConfigEntity configEntity = new HostGroupConfigEntity();
//    configEntity.setConfigData("");
//
//    hostGroup.setConfigurations(Collections.singletonList(configEntity));
//    BaseBlueprintProcessor.HostGroupImpl hostGroupImpl =
//      new BaseBlueprintProcessor.HostGroupImpl(hostGroup, stack, null);
//    // blueprint request will not include a reference to an HBASE component
//    hostGroupImpl.addComponent("COMPONENT_ONE");
//
//    // add empty map for core-site, to simulate this configuration entry
//    clusterResourceProvider.getClusterConfigurations().put("core-site", new HashMap<String, String>());
//
//    //clusterResourceProvider.setMissingConfigurations(Collections.singletonMap("host_group_one", hostGroupImpl));
//
//    Map<String, String> mapCoreSiteConfig =
//      clusterResourceProvider.getClusterConfigurations().get("core-site");
//
//    assertNotNull("core-site map was null.", mapCoreSiteConfig);
//    assertEquals("Incorrect number of entries in the core-site config map",
//      0, mapCoreSiteConfig.size());
//
//    mockSupport.verifyAll();
//
//  }
//
//  @Test
//  public void testSetConfigurationsOnClusterWithExcludedTypes() throws Exception {
//    EasyMockSupport mockSupport = new EasyMockSupport();
//    AmbariManagementController mockMgmtController =
//      mockSupport.createMock(AmbariManagementController.class);
//    ResourceProvider mockServiceProvider =
//      mockSupport.createMock(ResourceProvider.class);
//    ResourceProvider mockComponentProvider =
//      mockSupport.createMock(ResourceProvider.class);
//    ResourceProvider mockHostProvider =
//      mockSupport.createMock(ResourceProvider.class);
//    ResourceProvider mockHostComponentProvider =
//      mockSupport.createMock(ResourceProvider.class);
//    ResourceProvider mockConfigGroupProvider =
//      mockSupport.createMock(ResourceProvider.class);
//    Stack mockStack =
//      mockSupport.createMock(Stack.class);
//    BaseBlueprintProcessor.HostGroupImpl mockHostGroupOne =
//      mockSupport.createMock(BaseBlueprintProcessor.HostGroupImpl.class);
//
//    ArrayList<Capture<Set<ClusterRequest>>> listOfRequestCaptures =
//      new ArrayList<Capture<Set<ClusterRequest>>>();
//    for (int i = 0; i < 2; i++) {
//      listOfRequestCaptures.add(new Capture<Set<ClusterRequest>>());
//    }
//
//    ArrayList<Capture<Map<String, String>>> listOfPropertiesCaptures =
//      new ArrayList<Capture<Map<String, String>>>();
//    for (int i = 0; i < 2; i++) {
//      listOfPropertiesCaptures.add(new Capture<Map<String, String>>());
//    }
//
//    expect(mockHostGroupOne.getHostInfo()).andReturn(Collections.singleton("c6401.ambari.apache.org")).atLeastOnce();
//    expect(mockHostGroupOne.getComponents()).andReturn(Arrays.asList("FALCON_SERVER", "FALCON_CLIENT"));
//    expect(mockStack.getServicesForComponents(Arrays.asList("FALCON_SERVER", "FALCON_CLIENT")))
//      .andReturn(Arrays.asList("FALCON")).atLeastOnce();
//    expect(mockStack.getConfigurationTypes("FALCON")).andReturn(Arrays.asList("falcon-site", "falcon-env", "oozie-site")).atLeastOnce();
//    // configure falcon to include a single excluded config type
//    expect(mockStack.getExcludedConfigurationTypes("FALCON")).andReturn(Collections.<String>singleton("oozie-site")).atLeastOnce();
//
//    // setup expectations for controller.updateClusters() calls
//    for (int i = 0; i < 2; i++) {
//      expect(mockMgmtController.updateClusters(capture(listOfRequestCaptures.get(i)), capture(listOfPropertiesCaptures.get(i)))).andReturn(null);
//    }
//
//    Map<String, BaseBlueprintProcessor.HostGroupImpl> testMapOfHostGroups =
//      new HashMap<String, BaseBlueprintProcessor.HostGroupImpl>();
//    testMapOfHostGroups.put("host-group-one", mockHostGroupOne);
//
//    mockSupport.replayAll();
//
//    ClusterResourceProvider clusterResourceProvider =
//      new TestClusterResourceProvider(mockMgmtController, mockServiceProvider,
//        mockComponentProvider, mockHostProvider, mockHostComponentProvider, mockConfigGroupProvider);
//
//    Map<String, Map<String, String>> clusterConfig =
//      clusterResourceProvider.getClusterConfigurations();
//    clusterConfig.put("falcon-site", Collections.singletonMap("key1", "value1"));
//    clusterConfig.put("falcon-env", Collections.singletonMap("envKey1", "envValue1"));
//    clusterConfig.put("oozie-site", Collections.singletonMap("oozie-key-one", "oozie-value-one"));
//    clusterConfig.put("cluster-env", Collections.<String, String>emptyMap());
//
//    // call the method being tested
//    clusterResourceProvider.setConfigurationsOnCluster(clusterConfig, Collections.<String, Map<String,
//        Map<String, String>>>emptyMap(),"clusterone", mockStack, testMapOfHostGroups, "1");
//
//    // verify that the ClusterRequest's passed to the controller include the expected information
//    for (Capture<Set<ClusterRequest>> requestCapture : listOfRequestCaptures) {
//      Set<ClusterRequest> request = requestCapture.getValue();
//      assertEquals("Incorrect number of cluster requests in this update",
//                   1, request.size());
//    }
//
//
//    for (Capture<Map<String, String>> propertiesCapture : listOfPropertiesCaptures) {
//      assertNull("Incorrect request properties sent with this update",
//                 propertiesCapture.getValue());
//    }
//
//    // verify that the config requests include the expected information
//    ClusterRequest requestOne = listOfRequestCaptures.get(0).getValue().iterator().next();
//    ClusterRequest requestTwo = listOfRequestCaptures.get(1).getValue().iterator().next();
//
//    if (requestOne.getDesiredConfig().size() == 1) {
//      verifyClusterRequest(requestOne, "cluster-env");
//      // verify that the falcon config does not include oozie-site, since it is excluded
//      verifyClusterRequest(requestTwo, "falcon-site", "falcon-env");
//    } else {
//      verifyClusterRequest(requestTwo, "cluster-env");
//      // verify that the falcon config does not include oozie-site, since it is excluded
//      verifyClusterRequest(requestOne, "falcon-site", "falcon-env");
//    }
//
//    mockSupport.verifyAll();
//  }
//
//  @Test
//  public void testSetConfigurationsOnClusterWithNoExcludedTypes() throws Exception {
//    EasyMockSupport mockSupport = new EasyMockSupport();
//    AmbariManagementController mockMgmtController =
//      mockSupport.createMock(AmbariManagementController.class);
//    ResourceProvider mockServiceProvider =
//      mockSupport.createMock(ResourceProvider.class);
//    ResourceProvider mockComponentProvider =
//      mockSupport.createMock(ResourceProvider.class);
//    ResourceProvider mockHostProvider =
//      mockSupport.createMock(ResourceProvider.class);
//    ResourceProvider mockHostComponentProvider =
//      mockSupport.createMock(ResourceProvider.class);
//    ResourceProvider mockConfigGroupProvider =
//      mockSupport.createMock(ResourceProvider.class);
//    Stack mockStack =
//      mockSupport.createMock(Stack.class);
//    org.apache.ambari.server.topology.HostGroup mockHostGroupOne =
//      mockSupport.createMock(org.apache.ambari.server.topology.HostGroup.class);
//
//    ArrayList<Capture<Set<ClusterRequest>>> listOfRequestCaptures =
//      new ArrayList<Capture<Set<ClusterRequest>>>();
//    for (int i = 0; i < 2; i++) {
//      listOfRequestCaptures.add(new Capture<Set<ClusterRequest>>());
//    }
//
//    ArrayList<Capture<Map<String, String>>> listOfPropertiesCaptures =
//      new ArrayList<Capture<Map<String, String>>>();
//    for (int i = 0; i < 2; i++) {
//      listOfPropertiesCaptures.add(new Capture<Map<String, String>>());
//    }
//
//    expect(mockHostGroupOne.getHostInfo()).andReturn(Collections.singleton("c6401.ambari.apache.org")).atLeastOnce();
//    expect(mockHostGroupOne.getComponents()).andReturn(Arrays.asList("FALCON_SERVER", "FALCON_CLIENT"));
//    expect(mockStack.getServicesForComponents(Arrays.asList("FALCON_SERVER", "FALCON_CLIENT")))
//      .andReturn(Arrays.asList("FALCON")).atLeastOnce();
//    expect(mockStack.getConfigurationTypes("FALCON")).andReturn(Arrays.asList("falcon-site", "falcon-env", "oozie-site")).atLeastOnce();
//    // configure falcon to NOT have any excluded types
//    expect(mockStack.getExcludedConfigurationTypes("FALCON")).andReturn(Collections.<String>emptySet()).atLeastOnce();
//
//    // setup expectations for controller.updateClusters() calls
//    for (int i = 0; i < 2; i++) {
//      expect(mockMgmtController.updateClusters(capture(listOfRequestCaptures.get(i)), capture(listOfPropertiesCaptures.get(i)))).andReturn(null);
//    }
//
//    Map<String, org.apache.ambari.server.topology.HostGroup> testMapOfHostGroups =
//      new HashMap<String, org.apache.ambari.server.topology.HostGroup>();
//    testMapOfHostGroups.put("host-group-one", mockHostGroupOne);
//
//    mockSupport.replayAll();
//
//    ClusterResourceProvider clusterResourceProvider =
//      new TestClusterResourceProvider(mockMgmtController, mockServiceProvider,
//        mockComponentProvider, mockHostProvider, mockHostComponentProvider, mockConfigGroupProvider);
//
//    Map<String, Map<String, String>> clusterConfig =
//      clusterResourceProvider.getClusterConfigurations();
//
//    clusterConfig.put("falcon-site", Collections.singletonMap("key1", "value1"));
//    clusterConfig.put("falcon-env", Collections.singletonMap("envKey1", "envValue1"));
//    clusterConfig.put("oozie-site", Collections.singletonMap("oozie-key-one", "oozie-value-one"));
//    clusterConfig.put("cluster-env", Collections.<String, String>emptyMap());
//
//    // call the method being tested
//    clusterResourceProvider.setAllConfigurationsOnCluster(clusterConfig, Collections.<String, Map<String, Map<String, String>>>emptyMap(),
//        "clusterone", mockStack, testMapOfHostGroups, "1");
//
//    // verify that the ClusterRequest's passed to the controller include the expected information
//    for (Capture<Set<ClusterRequest>> requestCapture : listOfRequestCaptures) {
//      Set<ClusterRequest> request = requestCapture.getValue();
//      assertEquals("Incorrect number of cluster requests in this update",
//        1, request.size());
//    }
//
//    for (Capture<Map<String, String>> propertiesCapture : listOfPropertiesCaptures) {
//      assertNull("Incorrect request properties sent with this update",
//        propertiesCapture.getValue());
//    }
//
//    // verify that the config requests include the expected information
//    ClusterRequest requestOne = listOfRequestCaptures.get(0).getValue().iterator().next();
//    ClusterRequest requestTwo = listOfRequestCaptures.get(1).getValue().iterator().next();
//
//    if (requestOne.getDesiredConfig().size() == 1) {
//      verifyClusterRequest(requestOne, "cluster-env");
//      // verify that the falcon config includes oozie-site, since nothing is excluded in this test
//      verifyClusterRequest(requestTwo, "falcon-site", "falcon-env", "oozie-site");
//    } else {
//      verifyClusterRequest(requestTwo, "cluster-env");
//      // verify that the falcon config includes oozie-site, since nothing is excluded in this test
//      verifyClusterRequest(requestOne, "falcon-site", "falcon-env", "oozie-site");
//    }
//
//    mockSupport.verifyAll();
//  }
//
//  private static void verifyClusterRequest(ClusterRequest request, String... expectedConfigTypes) throws Exception {
//    assertEquals("Incorrect number of cluster requests ",
//                 expectedConfigTypes.length, request.getDesiredConfig().size());
//
//    Set<String> foundConfigTypes = new HashSet<String>();
//    // build set of config types listed in this request
//    for (ConfigurationRequest configRequest : request.getDesiredConfig()) {
//      foundConfigTypes.add(configRequest.getType());
//    }
//
//    // verify that the expected types are found
//    for (String expectedType : expectedConfigTypes) {
//      assertTrue("Expected config type not found in this config request",
//                 foundConfigTypes.contains(expectedType));
//    }
//
//  }

  //todo: configuration properties are not being added to props
  private Set<Map<String, Object>> createBlueprintRequestProperties(String clusterName, String blueprintName) {
    Set<Map<String, Object>> propertySet = new LinkedHashSet<Map<String, Object>>();
    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    properties.put(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID, clusterName);
    properties.put(ClusterResourceProvider.BLUEPRINT_PROPERTY_ID, blueprintName);
    propertySet.add(properties);

    Collection<Map<String, Object>> hostGroups = new ArrayList<Map<String, Object>>();
    Map<String, Object> hostGroupProperties = new HashMap<String, Object>();
    hostGroups.add(hostGroupProperties);
    hostGroupProperties.put("name", "group1");
    Collection<Map<String, String>> hostGroupHosts = new ArrayList<Map<String, String>>();
    hostGroupProperties.put("hosts", hostGroupHosts);
    Map<String, String> hostGroupHostProperties = new HashMap<String, String>();
    hostGroupHostProperties.put("fqdn", "host.domain");
    hostGroupHosts.add(hostGroupHostProperties);
    properties.put("host_groups", hostGroups);

    Map<String, String> mapGroupConfigProperties = new HashMap<String, String>();
    mapGroupConfigProperties.put("myGroupProp", "awesomeValue");

    // blueprint core-site cluster configuration properties
    Map<String, String> blueprintCoreConfigProperties = new HashMap<String, String>();
    blueprintCoreConfigProperties.put("property1", "value2");
    blueprintCoreConfigProperties.put("new.property", "new.property.value");

    Map<String, String> blueprintGlobalConfigProperties = new HashMap<String, String>();
    blueprintGlobalConfigProperties.put("hive_database", "New MySQL Database");

    Map<String, String> oozieEnvConfigProperties = new HashMap<String, String>();
    oozieEnvConfigProperties.put("property1","value2");
    Map<String, String> hbaseEnvConfigProperties = new HashMap<String, String>();
    hbaseEnvConfigProperties.put("property1","value2");
    Map<String, String> falconEnvConfigProperties = new HashMap<String, String>();
    falconEnvConfigProperties.put("property1","value2");

    return propertySet;
  }
}
