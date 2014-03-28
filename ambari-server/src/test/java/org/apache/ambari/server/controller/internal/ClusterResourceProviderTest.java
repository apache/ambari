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

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.*;

import com.google.gson.Gson;
import org.apache.ambari.server.api.services.PersistKeyValueImpl;
import org.apache.ambari.server.api.services.PersistKeyValueService;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ClusterRequest;
import org.apache.ambari.server.controller.ClusterResponse;
import org.apache.ambari.server.controller.ConfigurationRequest;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.StackConfigurationRequest;
import org.apache.ambari.server.controller.StackConfigurationResponse;
import org.apache.ambari.server.controller.StackServiceComponentRequest;
import org.apache.ambari.server.controller.StackServiceComponentResponse;
import org.apache.ambari.server.controller.StackServiceRequest;
import org.apache.ambari.server.controller.StackServiceResponse;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.BlueprintDAO;
import org.apache.ambari.server.orm.entities.BlueprintConfigEntity;
import org.apache.ambari.server.orm.entities.BlueprintEntity;
import org.apache.ambari.server.orm.entities.HostGroupComponentEntity;
import org.apache.ambari.server.orm.entities.HostGroupEntity;
import org.easymock.Capture;
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
  public void testCreateResource_blueprint() throws Exception {
    String blueprintName = "test-blueprint";
    String stackName = "test";
    String stackVersion = "1.23";

    BlueprintDAO blueprintDAO = createStrictMock(BlueprintDAO.class);
    AmbariManagementController managementController = createStrictMock(AmbariManagementController.class);
    Request request = createNiceMock(Request.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    BlueprintEntity blueprint = createNiceMock(BlueprintEntity.class);
    StackServiceResponse stackServiceResponse1 = createNiceMock(StackServiceResponse.class);
    StackServiceResponse stackServiceResponse2 = createNiceMock(StackServiceResponse.class);
    Capture<Set<StackServiceRequest>> stackServiceRequestCapture = new Capture<Set<StackServiceRequest>>();

    StackServiceComponentResponse stackServiceComponentResponse1 = createNiceMock(StackServiceComponentResponse.class);
    StackServiceComponentResponse stackServiceComponentResponse2 = createNiceMock(StackServiceComponentResponse.class);
    StackServiceComponentResponse stackServiceComponentResponse3 = createNiceMock(StackServiceComponentResponse.class);
    Capture<Set<StackServiceComponentRequest>> serviceComponentRequestCapture1 = new Capture<Set<StackServiceComponentRequest>>();
    Capture<Set<StackServiceComponentRequest>> serviceComponentRequestCapture2 = new Capture<Set<StackServiceComponentRequest>>();

    StackConfigurationResponse stackConfigurationResponse1 = createNiceMock(StackConfigurationResponse.class);
    StackConfigurationResponse stackConfigurationResponse2 = createNiceMock(StackConfigurationResponse.class);
    StackConfigurationResponse stackConfigurationResponse3 = createNiceMock(StackConfigurationResponse.class);
    StackConfigurationResponse stackConfigurationResponse4 = createNiceMock(StackConfigurationResponse.class);
    Capture<Set<StackConfigurationRequest>> serviceConfigurationRequestCapture1 = new Capture<Set<StackConfigurationRequest>>();
    Capture<Set<StackConfigurationRequest>> serviceConfigurationRequestCapture2 = new Capture<Set<StackConfigurationRequest>>();

    BlueprintConfigEntity blueprintConfig = createNiceMock(BlueprintConfigEntity.class);

    HostGroupEntity hostGroup = createNiceMock(HostGroupEntity.class);
    HostGroupComponentEntity hostGroupComponent1 = createNiceMock(HostGroupComponentEntity.class);
    HostGroupComponentEntity hostGroupComponent2 = createNiceMock(HostGroupComponentEntity.class);
    HostGroupComponentEntity hostGroupComponent3 = createNiceMock(HostGroupComponentEntity.class);

    ServiceResourceProvider serviceResourceProvider = createStrictMock(ServiceResourceProvider.class);
    ResourceProvider componentResourceProvider = createStrictMock(ResourceProvider.class);
    ResourceProvider hostResourceProvider = createStrictMock(ResourceProvider.class);
    ResourceProvider hostComponentResourceProvider = createStrictMock(ResourceProvider.class);
    PersistKeyValueImpl persistKeyValue = createNiceMock(PersistKeyValueImpl.class);

    Capture<ClusterRequest> createClusterRequestCapture = new Capture<ClusterRequest>();
    Capture<Set<ClusterRequest>> updateClusterRequestCapture = new Capture<Set<ClusterRequest>>();
    Capture<Map<String, String>> updateClusterPropertyMapCapture = new Capture<Map<String, String>>();
    Capture<Set<ClusterRequest>> updateClusterRequestCapture2 = new Capture<Set<ClusterRequest>>();
    Capture<Map<String, String>> updateClusterPropertyMapCapture2 = new Capture<Map<String, String>>();
    Capture<Set<ClusterRequest>> updateClusterRequestCapture3 = new Capture<Set<ClusterRequest>>();
    Capture<Map<String, String>> updateClusterPropertyMapCapture3 = new Capture<Map<String, String>>();

    Capture<Request> serviceRequestCapture = new Capture<Request>();
    Capture<Request> componentRequestCapture = new Capture<Request>();
    Capture<Request> componentRequestCapture2 = new Capture<Request>();
    Capture<Request> hostRequestCapture = new Capture<Request>();
    Capture<Request> hostComponentRequestCapture = new Capture<Request>();

    Set<StackServiceResponse> stackServiceResponses = new LinkedHashSet<StackServiceResponse>();
    stackServiceResponses.add(stackServiceResponse1);
    stackServiceResponses.add(stackServiceResponse2);

    // service1 has 2 components
    Set<StackServiceComponentResponse> stackServiceComponentResponses1 = new LinkedHashSet<StackServiceComponentResponse>();
    stackServiceComponentResponses1.add(stackServiceComponentResponse1);
    stackServiceComponentResponses1.add(stackServiceComponentResponse2);

    // service2 has 1 components
    Set<StackServiceComponentResponse> stackServiceComponentResponses2 = new LinkedHashSet<StackServiceComponentResponse>();
    stackServiceComponentResponses2.add(stackServiceComponentResponse3);

    // service1 has 1 config
    Set<StackConfigurationResponse> stackConfigurationResponses1 = new LinkedHashSet<StackConfigurationResponse>();
    stackConfigurationResponses1.add(stackConfigurationResponse1);

    // service2 has 3 config
    Set<StackConfigurationResponse> stackConfigurationResponses2 = new LinkedHashSet<StackConfigurationResponse>();
    stackConfigurationResponses2.add(stackConfigurationResponse2);
    stackConfigurationResponses2.add(stackConfigurationResponse3);
    stackConfigurationResponses2.add(stackConfigurationResponse4);

    Collection<HostGroupComponentEntity> hostGroupComponents = new LinkedHashSet<HostGroupComponentEntity>();
    hostGroupComponents.add(hostGroupComponent1);
    hostGroupComponents.add(hostGroupComponent2);
    hostGroupComponents.add(hostGroupComponent3);

    // request properties
    Set<Map<String, Object>> propertySet = new LinkedHashSet<Map<String, Object>>();
    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    properties.put(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID, "c1");
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
    properties.put("host-groups", hostGroups);

    // blueprint cluster configuration properties
    Map<String, String> blueprintConfigProperties = new HashMap<String, String>();
    blueprintConfigProperties.put("property1", "value2");
    blueprintConfigProperties.put("new.property", "new.property.value");

    // expectations
    expect(request.getProperties()).andReturn(propertySet).anyTimes();
    expect(blueprintDAO.findByName(blueprintName)).andReturn(blueprint);
    expect(blueprint.getStackName()).andReturn(stackName);
    expect(blueprint.getStackVersion()).andReturn(stackVersion);
    expect(blueprint.getConfigurations()).andReturn(Collections.<BlueprintConfigEntity>singletonList(blueprintConfig));

    expect(managementController.getStackServices(capture(stackServiceRequestCapture))).andReturn(stackServiceResponses);
    expect(stackServiceResponse1.getServiceName()).andReturn("service1");
    expect(stackServiceResponse2.getServiceName()).andReturn("service2");

    expect(managementController.getStackComponents(capture(serviceComponentRequestCapture1))).
        andReturn(stackServiceComponentResponses1);
    expect(stackServiceComponentResponse1.getComponentName()).andReturn("component1");
    expect(stackServiceComponentResponse2.getComponentName()).andReturn("component2");

    expect(managementController.getStackConfigurations(capture(serviceConfigurationRequestCapture1))).
        andReturn(stackConfigurationResponses1);
    expect(stackConfigurationResponse1.getType()).andReturn("core-site.xml");
    expect(stackConfigurationResponse1.getPropertyName()).andReturn("property1");
    expect(stackConfigurationResponse1.getPropertyValue()).andReturn("value1");

    expect(managementController.getStackComponents(capture(serviceComponentRequestCapture2))).
        andReturn(stackServiceComponentResponses2);
    expect(stackServiceComponentResponse3.getComponentName()).andReturn("component3");

    expect(managementController.getStackConfigurations(capture(serviceConfigurationRequestCapture2))).
        andReturn(stackConfigurationResponses2);
    expect(stackConfigurationResponse2.getType()).andReturn("hdfs-site.xml");
    expect(stackConfigurationResponse2.getPropertyName()).andReturn("property2");
    expect(stackConfigurationResponse2.getPropertyValue()).andReturn("value2");

    expect(stackConfigurationResponse3.getType()).andReturn("global.xml");
    expect(stackConfigurationResponse3.getPropertyName()).andReturn("oozie_user");
    expect(stackConfigurationResponse3.getPropertyValue()).andReturn("oozie");

    expect(stackConfigurationResponse4.getType()).andReturn("core-site.xml");
    expect(stackConfigurationResponse4.getPropertyName()).andReturn("property3");
    expect(stackConfigurationResponse4.getPropertyValue()).andReturn("value3");

    expect(blueprintConfig.getBlueprintName()).andReturn("test-blueprint").anyTimes();
    expect(blueprintConfig.getType()).andReturn("core-site").anyTimes();
    expect(blueprintConfig.getConfigData()).andReturn(new Gson().toJson(blueprintConfigProperties));

    expect(blueprint.getHostGroups()).andReturn(Collections.singleton(hostGroup));
    expect(hostGroup.getName()).andReturn("group1");
    expect(hostGroup.getComponents()).andReturn(hostGroupComponents);
    expect(hostGroupComponent1.getName()).andReturn("component1");
    expect(hostGroupComponent2.getName()).andReturn("component2");
    expect(hostGroupComponent3.getName()).andReturn("component3");

    managementController.createCluster(capture(createClusterRequestCapture));
    expect(managementController.updateClusters(capture(updateClusterRequestCapture),
        capture(updateClusterPropertyMapCapture))).andReturn(null);
    expect(managementController.updateClusters(capture(updateClusterRequestCapture2),
        capture(updateClusterPropertyMapCapture2))).andReturn(null);
    expect(managementController.updateClusters(capture(updateClusterRequestCapture3),
        capture(updateClusterPropertyMapCapture3))).andReturn(null);

    expect(serviceResourceProvider.createResources(capture(serviceRequestCapture))).andReturn(null);
    expect(componentResourceProvider.createResources(capture(componentRequestCapture))).andReturn(null);
    expect(componentResourceProvider.createResources(capture(componentRequestCapture2))).andReturn(null);
    expect(hostResourceProvider.createResources(capture(hostRequestCapture))).andReturn(null);
    expect(hostComponentResourceProvider.createResources(capture(hostComponentRequestCapture))).andReturn(null);

    expect(serviceResourceProvider.installAndStart("c1")).andReturn(response);

    persistKeyValue.put("CLUSTER_CURRENT_STATUS", "{\"clusterState\":\"CLUSTER_STARTED_5\"}");

    replay(blueprintDAO, managementController, request, response, blueprint, stackServiceResponse1, stackServiceResponse2,
           stackServiceComponentResponse1, stackServiceComponentResponse2, stackServiceComponentResponse3,
           stackConfigurationResponse1, stackConfigurationResponse2, stackConfigurationResponse3, stackConfigurationResponse4,
           blueprintConfig, hostGroup, hostGroupComponent1, hostGroupComponent2, hostGroupComponent3, serviceResourceProvider,
           componentResourceProvider, hostResourceProvider, hostComponentResourceProvider, persistKeyValue);

    // test
    ClusterResourceProvider.injectBlueprintDAO(blueprintDAO);
    PersistKeyValueService.init(persistKeyValue);
    ResourceProvider provider = new TestClusterResourceProvider(
        managementController, serviceResourceProvider, componentResourceProvider,
        hostResourceProvider, hostComponentResourceProvider);

    RequestStatus requestStatus = provider.createResources(request);

    assertEquals(RequestStatus.Status.InProgress, requestStatus.getStatus());

    Set<StackServiceRequest> stackServiceRequests = stackServiceRequestCapture.getValue();
    assertEquals(1, stackServiceRequests.size());
    StackServiceRequest ssr = stackServiceRequests.iterator().next();
    assertNull(ssr.getServiceName());
    assertEquals("test", ssr.getStackName());
    assertEquals("1.23", ssr.getStackVersion());

    Set<StackServiceComponentRequest> stackServiceComponentRequests1 = serviceComponentRequestCapture1.getValue();
    Set<StackServiceComponentRequest> stackServiceComponentRequests2 = serviceComponentRequestCapture2.getValue();
    assertEquals(1, stackServiceComponentRequests1.size());
    assertEquals(1, stackServiceComponentRequests2.size());
    StackServiceComponentRequest scr1 = stackServiceComponentRequests1.iterator().next();
    StackServiceComponentRequest scr2 = stackServiceComponentRequests2.iterator().next();
    assertNull(scr1.getComponentName());
    assertNull(scr2.getComponentName());
    assertEquals("1.23", scr1.getStackVersion());
    assertEquals("1.23", scr2.getStackVersion());
    assertEquals("test", scr1.getStackName());
    assertEquals("test", scr2.getStackName());
    assertTrue(scr1.getServiceName().equals("service1") || scr1.getServiceName().equals("service2"));
    assertTrue(scr2.getServiceName().equals("service1") || scr2.getServiceName().equals("service2") &&
        ! scr2.getServiceName().equals(scr1.getServiceName()));

    Set<StackConfigurationRequest> serviceConfigurationRequest1 = serviceConfigurationRequestCapture1.getValue();
    Set<StackConfigurationRequest> serviceConfigurationRequest2 = serviceConfigurationRequestCapture2.getValue();
    assertEquals(1, serviceConfigurationRequest1.size());
    assertEquals(1, serviceConfigurationRequest2.size());
    StackConfigurationRequest configReq1 = serviceConfigurationRequest1.iterator().next();
    StackConfigurationRequest configReq2 = serviceConfigurationRequest2.iterator().next();
    assertNull(configReq1.getPropertyName());
    assertNull(configReq2.getPropertyName());
    assertEquals("1.23", configReq1.getStackVersion());
    assertEquals("1.23", configReq2.getStackVersion());
    assertEquals("test", configReq1.getStackName());
    assertEquals("test", configReq2.getStackName());
    assertTrue(configReq1.getServiceName().equals("service1") || configReq1.getServiceName().equals("service2"));
    assertTrue(configReq2.getServiceName().equals("service1") || configReq2.getServiceName().equals("service2") &&
        ! configReq2.getServiceName().equals(configReq1.getServiceName()));

    ClusterRequest clusterRequest = createClusterRequestCapture.getValue();
    assertEquals("c1", clusterRequest.getClusterName());
    assertEquals("test-1.23", clusterRequest.getStackVersion());

    Set<ClusterRequest> updateClusterRequest1 = updateClusterRequestCapture.getValue();
    Set<ClusterRequest> updateClusterRequest2 = updateClusterRequestCapture2.getValue();
    Set<ClusterRequest> updateClusterRequest3 = updateClusterRequestCapture3.getValue();
    assertEquals(1, updateClusterRequest1.size());
    assertEquals(1, updateClusterRequest2.size());
    assertEquals(1, updateClusterRequest3.size());
    ClusterRequest ucr1 = updateClusterRequest1.iterator().next();
    ClusterRequest ucr2 = updateClusterRequest2.iterator().next();
    ClusterRequest ucr3 = updateClusterRequest3.iterator().next();
    assertEquals("c1", ucr1.getClusterName());
    assertEquals("c1", ucr2.getClusterName());
    assertEquals("c1", ucr3.getClusterName());
    ConfigurationRequest cr1 = ucr1.getDesiredConfig();
    ConfigurationRequest cr2 = ucr2.getDesiredConfig();
    ConfigurationRequest cr3 = ucr3.getDesiredConfig();
    assertEquals("1", cr1.getVersionTag());
    assertEquals("1", cr2.getVersionTag());
    assertEquals("1", cr3.getVersionTag());
    Map<String, ConfigurationRequest> mapConfigRequests = new HashMap<String, ConfigurationRequest>();
    mapConfigRequests.put(cr1.getType(), cr1);
    mapConfigRequests.put(cr2.getType(), cr2);
    mapConfigRequests.put(cr3.getType(), cr3);
    assertEquals(3, mapConfigRequests.size());
    ConfigurationRequest globalConfigRequest = mapConfigRequests.get("global");
    assertEquals(5, globalConfigRequest.getProperties().size());
    assertEquals("hadoop", globalConfigRequest.getProperties().get("user_group"));
    assertEquals("ambari-qa", globalConfigRequest.getProperties().get("smokeuser"));
    assertEquals("default@REPLACEME.NOWHERE", globalConfigRequest.getProperties().get("nagios_contact"));
    assertEquals("admin", globalConfigRequest.getProperties().get("nagios_web_password"));
    assertEquals("oozie", globalConfigRequest.getProperties().get("oozie_user"));
    ConfigurationRequest hdfsConfigRequest = mapConfigRequests.get("hdfs-site");
    assertEquals(1, hdfsConfigRequest.getProperties().size());
    assertEquals("value2", hdfsConfigRequest.getProperties().get("property2"));
    ConfigurationRequest coreConfigRequest = mapConfigRequests.get("core-site");
    assertEquals(5, coreConfigRequest.getProperties().size());
    assertEquals("value2", coreConfigRequest.getProperties().get("property1"));
    assertEquals("value3", coreConfigRequest.getProperties().get("property3"));
    assertEquals("*", coreConfigRequest.getProperties().get("hadoop.proxyuser.oozie.hosts"));
    assertEquals("users", coreConfigRequest.getProperties().get("hadoop.proxyuser.oozie.groups"));
    assertEquals("new.property.value", coreConfigRequest.getProperties().get("new.property"));
    assertNull(updateClusterPropertyMapCapture.getValue());
    assertNull(updateClusterPropertyMapCapture2.getValue());
    assertNull(updateClusterPropertyMapCapture3.getValue());

    Request serviceRequest = serviceRequestCapture.getValue();
    assertEquals(2, serviceRequest.getProperties().size());
    Request componentRequest = componentRequestCapture.getValue();
    Request componentRequest2 = componentRequestCapture2.getValue();
    assertEquals(2, componentRequest.getProperties().size());
    Set<String> componentRequest1Names = new HashSet<String>();
    for (Map<String, Object> componentRequest1Properties : componentRequest.getProperties()) {
      assertEquals(3, componentRequest1Properties.size());
      assertEquals("c1", componentRequest1Properties.get("ServiceComponentInfo/cluster_name"));
      assertEquals("service1", componentRequest1Properties.get("ServiceComponentInfo/service_name"));
      componentRequest1Names.add((String) componentRequest1Properties.get("ServiceComponentInfo/component_name"));
    }
    assertTrue(componentRequest1Names.contains("component1") && componentRequest1Names.contains("component2"));
    assertEquals(1, componentRequest2.getProperties().size());
    Map<String, Object> componentRequest2Properties = componentRequest2.getProperties().iterator().next();
    assertEquals("c1", componentRequest2Properties.get("ServiceComponentInfo/cluster_name"));
    assertEquals("service2", componentRequest2Properties.get("ServiceComponentInfo/service_name"));
    assertEquals("component3", componentRequest2Properties.get("ServiceComponentInfo/component_name"));
    Request hostRequest = hostRequestCapture.getValue();
    assertEquals(1, hostRequest.getProperties().size());
    assertEquals("c1", hostRequest.getProperties().iterator().next().get("Hosts/cluster_name"));
    assertEquals("host.domain", hostRequest.getProperties().iterator().next().get("Hosts/host_name"));
    Request hostComponentRequest = hostComponentRequestCapture.getValue();
    assertEquals(3, hostComponentRequest.getProperties().size());
    Set<String> componentNames = new HashSet<String>();
    for (Map<String, Object> hostComponentProperties : hostComponentRequest.getProperties()) {
      assertEquals(3, hostComponentProperties.size());
      assertEquals("c1", hostComponentProperties.get("HostRoles/cluster_name"));
      assertEquals("host.domain", hostComponentProperties.get("HostRoles/host_name"));
      componentNames.add((String) hostComponentProperties.get("HostRoles/component_name"));
    }
    assertTrue(componentNames.contains("component1") && componentNames.contains("component2") &&
        componentNames.contains("component3"));

    verify(blueprintDAO, managementController, request, response, blueprint, stackServiceResponse1, stackServiceResponse2,
        stackServiceComponentResponse1, stackServiceComponentResponse2, stackServiceComponentResponse3,
        stackConfigurationResponse1, stackConfigurationResponse2, stackConfigurationResponse3, stackConfigurationResponse4,
        blueprintConfig, hostGroup, hostGroupComponent1, hostGroupComponent2, hostGroupComponent3, serviceResourceProvider,
        componentResourceProvider, hostResourceProvider, hostComponentResourceProvider, persistKeyValue);
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

  private class TestClusterResourceProvider extends ClusterResourceProvider {

    private ResourceProvider serviceResourceProvider;
    private ResourceProvider componentResourceProvider;
    private ResourceProvider hostResourceProvider;
    private ResourceProvider hostComponentResourceProvider;

    TestClusterResourceProvider(AmbariManagementController managementController,
                                ResourceProvider serviceResourceProvider,
                                ResourceProvider componentResourceProvider,
                                ResourceProvider hostResourceProvider,
                                ResourceProvider hostComponentResourceProvider) {

      super(PropertyHelper.getPropertyIds(Resource.Type.Cluster),
            PropertyHelper.getKeyPropertyIds(Resource.Type.Cluster),
            managementController);

      this.serviceResourceProvider = serviceResourceProvider;
      this.componentResourceProvider = componentResourceProvider;
      this.hostResourceProvider = hostResourceProvider;
      this.hostComponentResourceProvider = hostComponentResourceProvider;
    }

    @Override
    ResourceProvider getResourceProviderByType(Resource.Type type) {
      if (type == Resource.Type.Service) {
        return this.serviceResourceProvider;
      } else if (type == Resource.Type.Component) {
        return this.componentResourceProvider;
      } else if (type == Resource.Type.Host) {
        return this.hostResourceProvider;
      } else if (type == Resource.Type.HostComponent) {
        return this.hostComponentResourceProvider;
      } else {
        fail("Unexpected resource provider type requested");
      }
      return null;
    }
  }
}
