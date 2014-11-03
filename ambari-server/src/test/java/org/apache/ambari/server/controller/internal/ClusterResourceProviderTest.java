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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ClusterRequest;
import org.apache.ambari.server.controller.ClusterResponse;
import org.apache.ambari.server.controller.ConfigGroupRequest;
import org.apache.ambari.server.controller.ConfigurationRequest;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.StackConfigurationRequest;
import org.apache.ambari.server.controller.StackConfigurationResponse;
import org.apache.ambari.server.controller.StackLevelConfigurationRequest;
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
import org.apache.ambari.server.orm.entities.HostGroupConfigEntity;
import org.apache.ambari.server.orm.entities.HostGroupEntity;
import org.apache.ambari.server.state.AutoDeployInfo;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.DependencyInfo;
import org.apache.ambari.server.state.State;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;

/**
 * ClusterResourceProvider tests.
 */
public class ClusterResourceProviderTest {

  @Before
  public void setup() throws Exception {
    // reset this static field, to allow unit tests to function independently
    BaseBlueprintProcessor.stackInfo = null;
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
  public void testCreateResource_blueprint() throws Exception {
    String blueprintName = "test-blueprint";
    String stackName = "test";
    String stackVersion = "1.23";
    String clusterName = "c1";

    ConfigHelper configHelper = createMock(ConfigHelper.class);
    BlueprintDAO blueprintDAO = createStrictMock(BlueprintDAO.class);
    AmbariMetaInfo metaInfo = createMock(AmbariMetaInfo.class);
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
    StackServiceComponentResponse stackServiceComponentResponse4 = createNiceMock(StackServiceComponentResponse.class);
    Capture<Set<StackServiceComponentRequest>> serviceComponentRequestCapture1 = new Capture<Set<StackServiceComponentRequest>>();
    Capture<Set<StackServiceComponentRequest>> serviceComponentRequestCapture2 = new Capture<Set<StackServiceComponentRequest>>();

    StackConfigurationResponse stackConfigurationResponse1 = createNiceMock(StackConfigurationResponse.class);
    StackConfigurationResponse stackConfigurationResponse2 = createNiceMock(StackConfigurationResponse.class);
    StackConfigurationResponse stackConfigurationResponse3 = createNiceMock(StackConfigurationResponse.class);
    StackConfigurationResponse stackConfigurationResponse4 = createNiceMock(StackConfigurationResponse.class);
    StackConfigurationResponse stackConfigurationResponse5 = createNiceMock(StackConfigurationResponse.class);
    StackConfigurationResponse stackConfigurationResponse6 = createNiceMock(StackConfigurationResponse.class);
    StackConfigurationResponse stackConfigurationResponse7 = createNiceMock(StackConfigurationResponse.class);
    Capture<Set<StackConfigurationRequest>> serviceConfigurationRequestCapture1 = new Capture<Set<StackConfigurationRequest>>();
    Capture<Set<StackConfigurationRequest>> serviceConfigurationRequestCapture2 = new Capture<Set<StackConfigurationRequest>>();
    Capture<Set<StackLevelConfigurationRequest>> serviceLevelConfigurationRequestCapture1 = new Capture<Set<StackLevelConfigurationRequest>>();

    Map<String, String> coreSiteAttributes = new HashMap<String, String>();
    coreSiteAttributes.put("final", "true");
    Map<String, String> hdfsSiteAttributes = new HashMap<String, String>();
    hdfsSiteAttributes.put("final", "true");

    BlueprintConfigEntity blueprintConfig = createNiceMock(BlueprintConfigEntity.class);
    BlueprintConfigEntity blueprintConfig2 = createNiceMock(BlueprintConfigEntity.class);
    BlueprintConfigEntity blueprintConfig3 = createNiceMock(BlueprintConfigEntity.class);
    BlueprintConfigEntity blueprintConfig4 = createNiceMock(BlueprintConfigEntity.class);
    BlueprintConfigEntity blueprintConfig5 = createNiceMock(BlueprintConfigEntity.class);
    BlueprintConfigEntity blueprintConfig6 = createNiceMock(BlueprintConfigEntity.class);

    HostGroupEntity hostGroup = createNiceMock(HostGroupEntity.class);
    HostGroupComponentEntity hostGroupComponent1 = createNiceMock(HostGroupComponentEntity.class);
    HostGroupComponentEntity hostGroupComponent2 = createNiceMock(HostGroupComponentEntity.class);
    HostGroupComponentEntity hostGroupComponent3 = createNiceMock(HostGroupComponentEntity.class);
    HostGroupComponentEntity hostGroupComponent4 = createNiceMock(HostGroupComponentEntity.class);

    HostGroupConfigEntity hostGroupConfig = createNiceMock(HostGroupConfigEntity.class);

    ServiceResourceProvider serviceResourceProvider = createStrictMock(ServiceResourceProvider.class);
    ResourceProvider componentResourceProvider = createStrictMock(ResourceProvider.class);
    ResourceProvider hostResourceProvider = createStrictMock(ResourceProvider.class);
    ResourceProvider hostComponentResourceProvider = createStrictMock(ResourceProvider.class);
    ConfigGroupResourceProvider configGroupResourceProvider = createStrictMock(ConfigGroupResourceProvider.class);

    Capture<ClusterRequest> createClusterRequestCapture = new Capture<ClusterRequest>();
    Capture<Set<ClusterRequest>> updateClusterRequestCapture = new Capture<Set<ClusterRequest>>();
    Capture<Map<String, String>> updateClusterPropertyMapCapture = new Capture<Map<String, String>>();
    Capture<Set<ClusterRequest>> updateClusterRequestCapture2 = new Capture<Set<ClusterRequest>>();
    Capture<Map<String, String>> updateClusterPropertyMapCapture2 = new Capture<Map<String, String>>();
    Capture<Set<ClusterRequest>> updateClusterRequestCapture3 = new Capture<Set<ClusterRequest>>();
    Capture<Map<String, String>> updateClusterPropertyMapCapture3 = new Capture<Map<String, String>>();
    Capture<Set<ClusterRequest>> persistUIStateRequestCapture = new Capture<Set<ClusterRequest>>();
    Capture<Map<String, String>> persistUIStatePropertyMapCapture = new Capture<Map<String, String>>();

    Capture<Request> serviceRequestCapture = new Capture<Request>();
    Capture<Request> componentRequestCapture = new Capture<Request>();
    Capture<Request> componentRequestCapture2 = new Capture<Request>();
    Capture<Request> hostRequestCapture = new Capture<Request>();
    Capture<Request> hostComponentRequestCapture = new Capture<Request>();
    Capture<Set<ConfigGroupRequest>> configGroupRequestCapture = new Capture<Set<ConfigGroupRequest>>();

    Set<StackServiceResponse> stackServiceResponses = new LinkedHashSet<StackServiceResponse>();
    stackServiceResponses.add(stackServiceResponse1);
    stackServiceResponses.add(stackServiceResponse2);

    // service1 has 3 components
    Set<StackServiceComponentResponse> stackServiceComponentResponses1 = new LinkedHashSet<StackServiceComponentResponse>();
    stackServiceComponentResponses1.add(stackServiceComponentResponse1);
    stackServiceComponentResponses1.add(stackServiceComponentResponse2);
    stackServiceComponentResponses1.add(stackServiceComponentResponse4);

    // service2 has 1 components
    Set<StackServiceComponentResponse> stackServiceComponentResponses2 = new LinkedHashSet<StackServiceComponentResponse>();
    stackServiceComponentResponses2.add(stackServiceComponentResponse3);

    // service1 has 2 config
    Set<StackConfigurationResponse> stackConfigurationResponses1 = new LinkedHashSet<StackConfigurationResponse>();
    stackConfigurationResponses1.add(stackConfigurationResponse1);
    stackConfigurationResponses1.add(stackConfigurationResponse5);

    // service2 has 3 config
    Set<StackConfigurationResponse> stackConfigurationResponses2 = new LinkedHashSet<StackConfigurationResponse>();
    stackConfigurationResponses2.add(stackConfigurationResponse2);
    stackConfigurationResponses2.add(stackConfigurationResponse3);
    stackConfigurationResponses2.add(stackConfigurationResponse4);
    stackConfigurationResponses2.add(stackConfigurationResponse7);
    
    Set<StackConfigurationResponse> stackConfigurationResponses3 = new LinkedHashSet<StackConfigurationResponse>();
    stackConfigurationResponses3.add(stackConfigurationResponse6);
    
    Collection<HostGroupComponentEntity> hostGroupComponents = new LinkedHashSet<HostGroupComponentEntity>();
    hostGroupComponents.add(hostGroupComponent1);
    hostGroupComponents.add(hostGroupComponent2);
    hostGroupComponents.add(hostGroupComponent3);
    hostGroupComponents.add(hostGroupComponent4);

    // request properties
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

    Collection<BlueprintConfigEntity> configurations = new HashSet<BlueprintConfigEntity>();
    configurations.add(blueprintConfig);
    configurations.add(blueprintConfig2);
    configurations.add(blueprintConfig3);
    configurations.add(blueprintConfig4);
    configurations.add(blueprintConfig5);
    configurations.add(blueprintConfig6);

    // expectations
    expect(request.getProperties()).andReturn(propertySet).anyTimes();
    expect(blueprintDAO.findByName(blueprintName)).andReturn(blueprint);
    expect(blueprint.getStackName()).andReturn(stackName);
    expect(blueprint.getStackVersion()).andReturn(stackVersion);
    expect(blueprint.getConfigurations()).andReturn(configurations).anyTimes();
    expect(blueprint.validateConfigurations(metaInfo, true)).andReturn(
        Collections.<String, Map<String, Collection<String>>>emptyMap());

    expect(metaInfo.getComponentDependencies("test", "1.23", "service1", "component1")).
        andReturn(Collections.<DependencyInfo>emptyList());
    expect(metaInfo.getComponentDependencies("test", "1.23", "service1", "component2")).
        andReturn(Collections.<DependencyInfo>emptyList());
    expect(metaInfo.getComponentDependencies("test", "1.23", "service1", "MYSQL_SERVER")).
        andReturn(Collections.<DependencyInfo>emptyList());
    expect(metaInfo.getComponentDependencies("test", "1.23", "service2", "component3")).
        andReturn(Collections.<DependencyInfo>emptyList());

    expect(managementController.getStackServices(capture(stackServiceRequestCapture))).andReturn(stackServiceResponses);
    expect(stackServiceResponse1.getServiceName()).andReturn("service1");
    expect(stackServiceResponse2.getServiceName()).andReturn("service2");

    expect(managementController.getStackComponents(capture(serviceComponentRequestCapture1))).
        andReturn(stackServiceComponentResponses1);
    expect(stackServiceComponentResponse1.getComponentName()).andReturn("component1");
    expect(stackServiceComponentResponse2.getComponentName()).andReturn("component2");
    expect(stackServiceComponentResponse4.getComponentName()).andReturn("MYSQL_SERVER");

    expect(managementController.getStackConfigurations(capture(serviceConfigurationRequestCapture1))).
        andReturn(stackConfigurationResponses1);
    
    expect(managementController.getStackLevelConfigurations(capture(serviceLevelConfigurationRequestCapture1))).
        andReturn(stackConfigurationResponses3);
    
    expect(stackConfigurationResponse1.getType()).andReturn("core-site.xml");
    expect(stackConfigurationResponse1.getPropertyName()).andReturn("property1");
    expect(stackConfigurationResponse1.getPropertyValue()).andReturn("value1");
    expect(stackConfigurationResponse1.getPropertyAttributes()).andReturn(coreSiteAttributes);

    expect(managementController.getStackComponents(capture(serviceComponentRequestCapture2))).
        andReturn(stackServiceComponentResponses2);
    
    expect(stackServiceComponentResponse3.getComponentName()).andReturn("component3");

    expect(managementController.getStackConfigurations(capture(serviceConfigurationRequestCapture2))).
        andReturn(stackConfigurationResponses2);
    
    expect(managementController.getStackLevelConfigurations(capture(serviceLevelConfigurationRequestCapture1))).
        andReturn(stackConfigurationResponses3);
    
    expect(stackConfigurationResponse2.getType()).andReturn("hdfs-site.xml");
    expect(stackConfigurationResponse2.getPropertyName()).andReturn("property2");
    expect(stackConfigurationResponse2.getPropertyValue()).andReturn("value2");
    expect(stackConfigurationResponse2.getPropertyAttributes()).andReturn(hdfsSiteAttributes);

    expect(stackConfigurationResponse3.getType()).andReturn("oozie-env.xml");
    expect(stackConfigurationResponse3.getPropertyName()).andReturn("oozie_user");
    expect(stackConfigurationResponse3.getPropertyValue()).andReturn("oozie");

    expect(stackConfigurationResponse4.getType()).andReturn("core-site.xml");
    expect(stackConfigurationResponse4.getPropertyName()).andReturn("property3");
    expect(stackConfigurationResponse4.getPropertyValue()).andReturn("value3");

    expect(stackConfigurationResponse5.getType()).andReturn("hive-site.xml");
    expect(stackConfigurationResponse5.getPropertyName()).andReturn("javax.jdo.option.ConnectionURL");
    expect(stackConfigurationResponse5.getPropertyValue()).andReturn("localhost:12345");
    
    expect(stackConfigurationResponse6.getType()).andReturn("cluster-env.xml").anyTimes();
    expect(stackConfigurationResponse6.getPropertyName()).andReturn("rqw").anyTimes();
    expect(stackConfigurationResponse6.getPropertyValue()).andReturn("aaaa").anyTimes();

    expect(stackConfigurationResponse7.getType()).andReturn("hive-env.xml").anyTimes();
    expect(stackConfigurationResponse7.getPropertyName()).andReturn("test-property-one");
    expect(stackConfigurationResponse7.getPropertyValue()).andReturn("test-value-one");


    expect(blueprintConfig.getBlueprintName()).andReturn("test-blueprint").anyTimes();
    expect(blueprintConfig.getType()).andReturn("core-site").anyTimes();
    expect(blueprintConfig.getConfigData()).andReturn(new Gson().toJson(blueprintCoreConfigProperties)).anyTimes();
    expect(blueprintConfig2.getBlueprintName()).andReturn("test-blueprint").anyTimes();
    expect(blueprintConfig2.getType()).andReturn("hive-env").anyTimes();
    expect(blueprintConfig2.getConfigData()).andReturn(new Gson().toJson(blueprintGlobalConfigProperties)).anyTimes();
    expect(blueprintConfig3.getBlueprintName()).andReturn("test-blueprint").anyTimes();
    expect(blueprintConfig3.getType()).andReturn("oozie-env").anyTimes();
    expect(blueprintConfig3.getConfigData()).andReturn(new Gson().toJson(oozieEnvConfigProperties)).anyTimes();
    expect(blueprintConfig4.getBlueprintName()).andReturn("test-blueprint").anyTimes();
    expect(blueprintConfig4.getType()).andReturn("falcon-env").anyTimes();
    expect(blueprintConfig4.getConfigData()).andReturn(new Gson().toJson(falconEnvConfigProperties)).anyTimes();
    expect(blueprintConfig5.getBlueprintName()).andReturn("test-blueprint").anyTimes();
    expect(blueprintConfig5.getType()).andReturn("global").anyTimes();
    expect(blueprintConfig5.getConfigData()).andReturn(new Gson().toJson(hbaseEnvConfigProperties)).anyTimes();
    expect(blueprintConfig6.getBlueprintName()).andReturn("test-blueprint").anyTimes();
    expect(blueprintConfig6.getType()).andReturn("cluster-env").anyTimes();
    expect(blueprintConfig6.getConfigData()).andReturn(new Gson().toJson(hbaseEnvConfigProperties)).anyTimes();


    expect(blueprint.getHostGroups()).andReturn(Collections.singleton(hostGroup)).anyTimes();
    expect(hostGroup.getName()).andReturn("group1").anyTimes();
    expect(hostGroup.getComponents()).andReturn(hostGroupComponents).anyTimes();
    expect(hostGroupComponent1.getName()).andReturn("component1").anyTimes();
    expect(hostGroupComponent2.getName()).andReturn("component2").anyTimes();
    expect(hostGroupComponent3.getName()).andReturn("component3").anyTimes();
    expect(hostGroupComponent4.getName()).andReturn("MYSQL_SERVER").anyTimes();
    expect(hostGroup.getConfigurations()).andReturn(
        Collections.<HostGroupConfigEntity>singleton(hostGroupConfig)).anyTimes();

    expect(hostGroupConfig.getType()).andReturn("core-site").anyTimes();
    expect(hostGroupConfig.getConfigData()).andReturn(new Gson().toJson(mapGroupConfigProperties)).anyTimes();

    managementController.createCluster(capture(createClusterRequestCapture));
    expect(managementController.updateClusters(capture(updateClusterRequestCapture),
        capture(updateClusterPropertyMapCapture))).andReturn(null);
    expect(managementController.updateClusters(capture(updateClusterRequestCapture2),
        capture(updateClusterPropertyMapCapture2))).andReturn(null);
    expect(managementController.updateClusters(capture(updateClusterRequestCapture3),
        capture(updateClusterPropertyMapCapture3))).andReturn(null);

    // set state for UI
    expect(managementController.updateClusters(capture(persistUIStateRequestCapture),
        capture(persistUIStatePropertyMapCapture))).andReturn(null);

    expect(serviceResourceProvider.createResources(capture(serviceRequestCapture))).andReturn(null);
    expect(componentResourceProvider.createResources(capture(componentRequestCapture))).andReturn(null);
    expect(componentResourceProvider.createResources(capture(componentRequestCapture2))).andReturn(null);
    expect(hostResourceProvider.createResources(capture(hostRequestCapture))).andReturn(null);
    expect(hostComponentResourceProvider.createResources(capture(hostComponentRequestCapture))).andReturn(null);

    expect(serviceResourceProvider.installAndStart(clusterName)).andReturn(response);

    expect(configGroupResourceProvider.createResources(
        capture(configGroupRequestCapture))).andReturn(null);

    replay(blueprintDAO, managementController, request, response, blueprint, stackServiceResponse1, stackServiceResponse2,
           stackServiceComponentResponse1, stackServiceComponentResponse2, stackServiceComponentResponse3,
           stackServiceComponentResponse4, stackConfigurationResponse1, stackConfigurationResponse2,
           stackConfigurationResponse3, stackConfigurationResponse4, stackConfigurationResponse5, stackConfigurationResponse6,
           stackConfigurationResponse7, blueprintConfig,
           blueprintConfig2, blueprintConfig3, blueprintConfig4, blueprintConfig5, blueprintConfig6, hostGroup, hostGroupComponent1, hostGroupComponent2, hostGroupComponent3, hostGroupComponent4,
           hostGroupConfig, serviceResourceProvider, componentResourceProvider, hostResourceProvider,
           hostComponentResourceProvider, configGroupResourceProvider, metaInfo);

    // test
    ClusterResourceProvider.init(blueprintDAO, metaInfo, configHelper);
    ResourceProvider provider = new TestClusterResourceProvider(
        managementController, serviceResourceProvider, componentResourceProvider,
        hostResourceProvider, hostComponentResourceProvider, configGroupResourceProvider) {
      @Override
      protected boolean isServiceIncluded(String serviceName, Map<String, HostGroupImpl> blueprintHostGroups) {
        return true;
      }
    };

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
    assertEquals(clusterName, clusterRequest.getClusterName());
    assertEquals("test-1.23", clusterRequest.getStackVersion());

    Set<ClusterRequest> updateClusterRequest1 = updateClusterRequestCapture.getValue();
    Set<ClusterRequest> updateClusterRequest2 = updateClusterRequestCapture2.getValue();
    Set<ClusterRequest> updateClusterRequest3 = updateClusterRequestCapture3.getValue();
    Set<ClusterRequest> updateClusterRequest4 = persistUIStateRequestCapture.getValue();



    assertEquals(1, updateClusterRequest1.size());
    assertEquals(1, updateClusterRequest2.size());
    assertEquals(1, updateClusterRequest3.size());
    assertEquals(1, updateClusterRequest4.size());

    ClusterRequest ucr1 = updateClusterRequest1.iterator().next();
    ClusterRequest ucr2 = updateClusterRequest2.iterator().next();
    ClusterRequest ucr3 = updateClusterRequest3.iterator().next();
    ClusterRequest ucr4 = updateClusterRequest4.iterator().next();

    assertEquals(clusterName, ucr1.getClusterName());
    assertEquals(clusterName, ucr2.getClusterName());
    assertEquals(clusterName, ucr3.getClusterName());
    assertEquals(clusterName, ucr4.getClusterName());

    assertEquals("INSTALLED", ucr4.getProvisioningState());
    ConfigurationRequest cr1 = ucr1.getDesiredConfig().get(0);
    ConfigurationRequest cr2 = ucr2.getDesiredConfig().get(0);
    ConfigurationRequest cr3 = ucr3.getDesiredConfig().get(0);

    assertEquals("1", cr1.getVersionTag());
    assertEquals("1", cr2.getVersionTag());
    assertEquals("1", cr3.getVersionTag());

    Map<String, ConfigurationRequest> mapConfigRequests = new HashMap<String, ConfigurationRequest>();

    ClusterRequest[] arrayOfClusterRequests = { ucr1, ucr2, ucr3 };

    // add all the associated config types to the test map
    // with the config versioning change, there could be
    // more than one config type associated with a ClusterRequest
    for (ClusterRequest testClusterRequest : arrayOfClusterRequests) {
      for (ConfigurationRequest configRequest : testClusterRequest.getDesiredConfig()) {
        mapConfigRequests.put(configRequest.getType(), configRequest);
      }
    }


    assertEquals(6, mapConfigRequests.size());
    ConfigurationRequest hiveEnvConfigRequest = mapConfigRequests.get("hive-env");
    assertEquals("New MySQL Database", hiveEnvConfigRequest.getProperties().get("hive_database"));
    ConfigurationRequest hdfsConfigRequest = mapConfigRequests.get("hdfs-site");
    assertEquals(1, hdfsConfigRequest.getProperties().size());
    assertEquals("value2", hdfsConfigRequest.getProperties().get("property2"));
    Map<String, Map<String, String>> hdfsAttributes = hdfsConfigRequest.getPropertiesAttributes();
    assertTrue(hdfsAttributes.containsKey("final"));
    assertEquals(1, hdfsAttributes.get("final").size());
    assertEquals("true", hdfsAttributes.get("final").get("property2"));
    ConfigurationRequest coreConfigRequest = mapConfigRequests.get("core-site");
    assertEquals(5, coreConfigRequest.getProperties().size());
    assertEquals("value2", coreConfigRequest.getProperties().get("property1"));
    assertEquals("value3", coreConfigRequest.getProperties().get("property3"));
    assertEquals("*", coreConfigRequest.getProperties().get("hadoop.proxyuser.oozie.hosts"));
    assertEquals("users", coreConfigRequest.getProperties().get("hadoop.proxyuser.oozie.groups"));
    assertEquals("new.property.value", coreConfigRequest.getProperties().get("new.property"));
    Map<String, Map<String, String>> coreAttributes = coreConfigRequest.getPropertiesAttributes();
    assertTrue(coreAttributes.containsKey("final"));
    assertEquals(1, coreAttributes.get("final").size());
    assertEquals("true", coreAttributes.get("final").get("property1"));
    ConfigurationRequest hiveConfigRequest = mapConfigRequests.get("hive-site");
    assertEquals(1, hiveConfigRequest.getProperties().size());
    assertEquals("host.domain:12345", hiveConfigRequest.getProperties().get("javax.jdo.option.ConnectionURL"));

    assertNull(updateClusterPropertyMapCapture.getValue());
    assertNull(updateClusterPropertyMapCapture2.getValue());
    assertNull(updateClusterPropertyMapCapture3.getValue());
    //assertNull(updateClusterPropertyMapCapture4.getValue());

    Request serviceRequest = serviceRequestCapture.getValue();
    assertEquals(2, serviceRequest.getProperties().size());
    Request componentRequest = componentRequestCapture.getValue();
    Request componentRequest2 = componentRequestCapture2.getValue();
    assertEquals(3, componentRequest.getProperties().size());
    Set<String> componentRequest1Names = new HashSet<String>();
    for (Map<String, Object> componentRequest1Properties : componentRequest.getProperties()) {
      assertEquals(3, componentRequest1Properties.size());
      assertEquals(clusterName, componentRequest1Properties.get("ServiceComponentInfo/cluster_name"));
      assertEquals("service1", componentRequest1Properties.get("ServiceComponentInfo/service_name"));
      componentRequest1Names.add((String) componentRequest1Properties.get("ServiceComponentInfo/component_name"));
    }
    assertTrue(componentRequest1Names.contains("component1") && componentRequest1Names.contains("component2")
        && componentRequest1Names.contains("MYSQL_SERVER"));
    assertEquals(1, componentRequest2.getProperties().size());
    Map<String, Object> componentRequest2Properties = componentRequest2.getProperties().iterator().next();
    assertEquals(clusterName, componentRequest2Properties.get("ServiceComponentInfo/cluster_name"));
    assertEquals("service2", componentRequest2Properties.get("ServiceComponentInfo/service_name"));
    assertEquals("component3", componentRequest2Properties.get("ServiceComponentInfo/component_name"));
    Request hostRequest = hostRequestCapture.getValue();
    assertEquals(1, hostRequest.getProperties().size());
    assertEquals(clusterName, hostRequest.getProperties().iterator().next().get("Hosts/cluster_name"));
    assertEquals("host.domain", hostRequest.getProperties().iterator().next().get("Hosts/host_name"));
    Request hostComponentRequest = hostComponentRequestCapture.getValue();
    assertEquals(4, hostComponentRequest.getProperties().size());
    Set<String> componentNames = new HashSet<String>();
    for (Map<String, Object> hostComponentProperties : hostComponentRequest.getProperties()) {
      assertEquals(3, hostComponentProperties.size());
      assertEquals(clusterName, hostComponentProperties.get("HostRoles/cluster_name"));
      assertEquals("host.domain", hostComponentProperties.get("HostRoles/host_name"));
      componentNames.add((String) hostComponentProperties.get("HostRoles/component_name"));
    }
    assertTrue(componentNames.contains("component1") && componentNames.contains("component2") &&
        componentNames.contains("component3") && componentNames.contains("MYSQL_SERVER"));

    Set<ConfigGroupRequest> configGroupRequests = configGroupRequestCapture.getValue();
    assertEquals(1, configGroupRequests.size());
    ConfigGroupRequest configGroupRequest = configGroupRequests.iterator().next();
    assertEquals(clusterName, configGroupRequest.getClusterName());
    assertEquals("group1", configGroupRequest.getGroupName());
    assertEquals("service1", configGroupRequest.getTag());
    assertEquals("Host Group Configuration", configGroupRequest.getDescription());
    Set<String> hosts = configGroupRequest.getHosts();
    assertEquals(1, hosts.size());
    assertEquals("host.domain", hosts.iterator().next());
    assertEquals(1, configGroupRequest.getConfigs().size());

    verify(blueprintDAO, managementController, request, response, blueprint, stackServiceResponse1, stackServiceResponse2,
        stackServiceComponentResponse1, stackServiceComponentResponse2, stackServiceComponentResponse3,
        stackServiceComponentResponse4, stackConfigurationResponse1, stackConfigurationResponse2,
        stackConfigurationResponse3, stackConfigurationResponse4, stackConfigurationResponse5, stackConfigurationResponse6,
        stackConfigurationResponse7, blueprintConfig,
        blueprintConfig2, hostGroup, hostGroupComponent1, hostGroupComponent2, hostGroupComponent3, hostGroupComponent4,
        hostGroupConfig, serviceResourceProvider, componentResourceProvider, hostResourceProvider,
        hostComponentResourceProvider, configGroupResourceProvider, metaInfo);
  }

  @Test
  public void testCreateResource_blueprint__missingPasswords() throws Exception {
    String blueprintName = "test-blueprint";
    String stackName = "test";
    String stackVersion = "1.23";
    String clusterName = "c1";

    BlueprintDAO blueprintDAO = createStrictMock(BlueprintDAO.class);
    AmbariMetaInfo metaInfo = createMock(AmbariMetaInfo.class);
    AmbariManagementController managementController = createStrictMock(AmbariManagementController.class);
    Request request = createNiceMock(Request.class);
    ConfigHelper configHelper = createNiceMock(ConfigHelper.class);
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
    StackConfigurationResponse stackConfigurationResponse5 = createNiceMock(StackConfigurationResponse.class);
    StackConfigurationResponse stackConfigurationResponse6 = createNiceMock(StackConfigurationResponse.class);
    StackConfigurationResponse stackConfigurationResponse7 = createNiceMock(StackConfigurationResponse.class);
    StackConfigurationResponse stackConfigurationResponse8 = createNiceMock(StackConfigurationResponse.class);
    Capture<Set<StackConfigurationRequest>> serviceConfigurationRequestCapture1 = new Capture<Set<StackConfigurationRequest>>();
    Capture<Set<StackConfigurationRequest>> serviceConfigurationRequestCapture2 = new Capture<Set<StackConfigurationRequest>>();
    Capture<Set<StackLevelConfigurationRequest>> serviceLevelConfigurationRequestCapture1 = new Capture<Set<StackLevelConfigurationRequest>>();

    BlueprintConfigEntity blueprintConfig = createNiceMock(BlueprintConfigEntity.class);

    HostGroupEntity hostGroup = createNiceMock(HostGroupEntity.class);
    HostGroupComponentEntity hostGroupComponent1 = createNiceMock(HostGroupComponentEntity.class);
    HostGroupComponentEntity hostGroupComponent2 = createNiceMock(HostGroupComponentEntity.class);
    HostGroupComponentEntity hostGroupComponent3 = createNiceMock(HostGroupComponentEntity.class);

    HostGroupConfigEntity hostGroupConfig = createNiceMock(HostGroupConfigEntity.class);

    ServiceResourceProvider serviceResourceProvider = createStrictMock(ServiceResourceProvider.class);
    ResourceProvider componentResourceProvider = createStrictMock(ResourceProvider.class);
    ResourceProvider hostResourceProvider = createStrictMock(ResourceProvider.class);
    ResourceProvider hostComponentResourceProvider = createStrictMock(ResourceProvider.class);
    ConfigGroupResourceProvider configGroupResourceProvider = createStrictMock(ConfigGroupResourceProvider.class);

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
    stackConfigurationResponses2.add(stackConfigurationResponse5);
    stackConfigurationResponses2.add(stackConfigurationResponse6);
    stackConfigurationResponses2.add(stackConfigurationResponse7);

    Set<StackConfigurationResponse> stackConfigurationResponses3 = new LinkedHashSet<StackConfigurationResponse>();
    stackConfigurationResponses2.add(stackConfigurationResponse8);
    
    Collection<HostGroupComponentEntity> hostGroupComponents = new LinkedHashSet<HostGroupComponentEntity>();
    hostGroupComponents.add(hostGroupComponent1);
    hostGroupComponents.add(hostGroupComponent2);
    hostGroupComponents.add(hostGroupComponent3);

    // request properties
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

    // blueprint cluster configuration properties
    Map<String, String> blueprintConfigProperties = new HashMap<String, String>();
    blueprintConfigProperties.put("property1", "value2");
    blueprintConfigProperties.put("new.property", "new.property.value");

    Map<String, Map<String, Collection<String>>> allMissingPasswords = new HashMap<String, Map<String, Collection<String>>>();
    Map<String, Collection<String>> missingHGPasswords = new HashMap<String, Collection<String>>();
    Collection<String> missingPasswords = new ArrayList<String>();
    missingPasswords.add("my.missing.password");
    missingHGPasswords.put("core-site", missingPasswords);
    allMissingPasswords.put("group1", missingHGPasswords);

    // expectations
    expect(request.getProperties()).andReturn(propertySet).anyTimes();
    expect(blueprintDAO.findByName(blueprintName)).andReturn(blueprint);
    expect(blueprint.getStackName()).andReturn(stackName);
    expect(blueprint.getStackVersion()).andReturn(stackVersion);
    expect(blueprint.getConfigurations()).andReturn(Collections.<BlueprintConfigEntity>singletonList(blueprintConfig)).anyTimes();
    expect(blueprint.validateConfigurations(metaInfo, true)).andReturn(allMissingPasswords);

    expect(metaInfo.getComponentDependencies("test", "1.23", "service1", "component1")).
        andReturn(Collections.<DependencyInfo>emptyList());
    expect(metaInfo.getComponentDependencies("test", "1.23", "service1", "component2")).
        andReturn(Collections.<DependencyInfo>emptyList());
    expect(metaInfo.getComponentDependencies("test", "1.23", "service2", "component3")).
        andReturn(Collections.<DependencyInfo>emptyList());

    expect(managementController.getStackServices(capture(stackServiceRequestCapture))).andReturn(stackServiceResponses);
    expect(stackServiceResponse1.getServiceName()).andReturn("service1");
    expect(stackServiceResponse2.getServiceName()).andReturn("service2");

    expect(managementController.getStackComponents(capture(serviceComponentRequestCapture1))).
        andReturn(stackServiceComponentResponses1);
    expect(stackServiceComponentResponse1.getComponentName()).andReturn("component1");
    expect(stackServiceComponentResponse2.getComponentName()).andReturn("component2");

    expect(managementController.getStackConfigurations(capture(serviceConfigurationRequestCapture1))).
        andReturn(stackConfigurationResponses1);
    
    expect(managementController.getStackLevelConfigurations(capture(serviceLevelConfigurationRequestCapture1))).
        andReturn(stackConfigurationResponses3);
    
    expect(stackConfigurationResponse1.getType()).andReturn("core-site.xml");
    expect(stackConfigurationResponse1.getPropertyName()).andReturn("property1");
    expect(stackConfigurationResponse1.getPropertyValue()).andReturn("value1");

    expect(managementController.getStackComponents(capture(serviceComponentRequestCapture2))).
        andReturn(stackServiceComponentResponses2);
    expect(stackServiceComponentResponse3.getComponentName()).andReturn("component3");

    expect(managementController.getStackConfigurations(capture(serviceConfigurationRequestCapture2))).
        andReturn(stackConfigurationResponses2);
    
    expect(managementController.getStackLevelConfigurations(capture(serviceLevelConfigurationRequestCapture1))).
        andReturn(stackConfigurationResponses3); 
    
    expect(stackConfigurationResponse8.getType()).andReturn("cluster-env.xml").anyTimes();
    expect(stackConfigurationResponse8.getPropertyName()).andReturn("rqw").anyTimes();
    expect(stackConfigurationResponse8.getPropertyValue()).andReturn("aaaa").anyTimes();
    
    expect(stackConfigurationResponse2.getType()).andReturn("hdfs-site.xml");
    expect(stackConfigurationResponse2.getPropertyName()).andReturn("property2");
    expect(stackConfigurationResponse2.getPropertyValue()).andReturn("value2");

    expect(stackConfigurationResponse3.getType()).andReturn("oozie-env.xml");
    expect(stackConfigurationResponse3.getPropertyName()).andReturn("oozie_user");
    expect(stackConfigurationResponse3.getPropertyValue()).andReturn("oozie");

    expect(stackConfigurationResponse4.getType()).andReturn("core-site.xml");
    expect(stackConfigurationResponse4.getPropertyName()).andReturn("property3");
    expect(stackConfigurationResponse4.getPropertyValue()).andReturn("value3");
    
    expect(stackConfigurationResponse5.getType()).andReturn("hive-env.xml");
    expect(stackConfigurationResponse5.getPropertyName()).andReturn("property3");
    expect(stackConfigurationResponse5.getPropertyValue()).andReturn("value3");
    
    expect(stackConfigurationResponse6.getType()).andReturn("hbase-env.xml");
    expect(stackConfigurationResponse6.getPropertyName()).andReturn("property3");
    expect(stackConfigurationResponse6.getPropertyValue()).andReturn("value3");
    
    expect(stackConfigurationResponse7.getType()).andReturn("falcon-env.xml");
    expect(stackConfigurationResponse7.getPropertyName()).andReturn("property3");
    expect(stackConfigurationResponse7.getPropertyValue()).andReturn("value3");

    expect(blueprintConfig.getBlueprintName()).andReturn("test-blueprint").anyTimes();
    expect(blueprintConfig.getType()).andReturn("core-site").anyTimes();
    expect(blueprintConfig.getConfigData()).andReturn(new Gson().toJson(blueprintConfigProperties));

    expect(blueprint.getHostGroups()).andReturn(Collections.singleton(hostGroup)).anyTimes();
    expect(hostGroup.getName()).andReturn("group1").anyTimes();
    expect(hostGroup.getComponents()).andReturn(hostGroupComponents).anyTimes();
    expect(hostGroupComponent1.getName()).andReturn("component1").anyTimes();
    expect(hostGroupComponent2.getName()).andReturn("component2").anyTimes();
    expect(hostGroupComponent3.getName()).andReturn("component3").anyTimes();
    expect(hostGroup.getConfigurations()).andReturn(
        Collections.<HostGroupConfigEntity>singleton(hostGroupConfig)).anyTimes();

    expect(hostGroupConfig.getType()).andReturn("core-site").anyTimes();
    expect(hostGroupConfig.getConfigData()).andReturn(new Gson().toJson(mapGroupConfigProperties)).anyTimes();

    replay(blueprintDAO, managementController, request, response, blueprint, stackServiceResponse1, stackServiceResponse2,
        stackServiceComponentResponse1, stackServiceComponentResponse2, stackServiceComponentResponse3,
        stackConfigurationResponse1, stackConfigurationResponse2, stackConfigurationResponse3, stackConfigurationResponse4,
        stackConfigurationResponse5, stackConfigurationResponse6, stackConfigurationResponse7, stackConfigurationResponse8,
        blueprintConfig, hostGroup, hostGroupComponent1, hostGroupComponent2, hostGroupComponent3, hostGroupConfig,
        serviceResourceProvider, componentResourceProvider, hostResourceProvider, hostComponentResourceProvider,
        configGroupResourceProvider, metaInfo);

    // test
    ClusterResourceProvider.init(blueprintDAO, metaInfo, configHelper);
    ResourceProvider provider = new TestClusterResourceProvider(
        managementController, serviceResourceProvider, componentResourceProvider,
        hostResourceProvider, hostComponentResourceProvider, configGroupResourceProvider);

    try {
      provider.createResources(request);
      fail("Expected exception for missing password property");
    } catch (IllegalArgumentException e) {
      //expected
    }

    verify(blueprintDAO, managementController, request, response, blueprint, stackServiceResponse1, stackServiceResponse2,
        stackServiceComponentResponse1, stackServiceComponentResponse2, stackServiceComponentResponse3,
        stackConfigurationResponse1, stackConfigurationResponse2, stackConfigurationResponse3, stackConfigurationResponse4,
        blueprintConfig, hostGroup, hostGroupComponent1, hostGroupComponent2, hostGroupComponent3, hostGroupConfig,
        serviceResourceProvider, componentResourceProvider, hostResourceProvider, hostComponentResourceProvider,
        configGroupResourceProvider);
  }

  @Test
  public void testCreateResource_blueprint__noHostGroups() throws Exception {
    String blueprintName = "test-blueprint";
    String stackName = "test";
    String stackVersion = "1.23";
    String clusterName = "c1";

    ConfigHelper configHelper = createNiceMock(ConfigHelper.class);
    BlueprintDAO blueprintDAO = createStrictMock(BlueprintDAO.class);
    AmbariMetaInfo metaInfo = createMock(AmbariMetaInfo.class);
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
    StackConfigurationResponse stackConfigurationResponse5 = createNiceMock(StackConfigurationResponse.class);
    Capture<Set<StackConfigurationRequest>> serviceConfigurationRequestCapture1 = new Capture<Set<StackConfigurationRequest>>();
    Capture<Set<StackConfigurationRequest>> serviceConfigurationRequestCapture2 = new Capture<Set<StackConfigurationRequest>>();
    Capture<Set<StackLevelConfigurationRequest>> serviceLevelConfigurationRequestCapture1 = new Capture<Set<StackLevelConfigurationRequest>>();


    BlueprintConfigEntity blueprintConfig = createNiceMock(BlueprintConfigEntity.class);

    HostGroupEntity hostGroup = createNiceMock(HostGroupEntity.class);
    HostGroupComponentEntity hostGroupComponent1 = createNiceMock(HostGroupComponentEntity.class);
    HostGroupComponentEntity hostGroupComponent2 = createNiceMock(HostGroupComponentEntity.class);
    HostGroupComponentEntity hostGroupComponent3 = createNiceMock(HostGroupComponentEntity.class);

    HostGroupConfigEntity hostGroupConfig = createNiceMock(HostGroupConfigEntity.class);

    ServiceResourceProvider serviceResourceProvider = createStrictMock(ServiceResourceProvider.class);
    ResourceProvider componentResourceProvider = createStrictMock(ResourceProvider.class);
    ResourceProvider hostResourceProvider = createStrictMock(ResourceProvider.class);
    ResourceProvider hostComponentResourceProvider = createStrictMock(ResourceProvider.class);
    ConfigGroupResourceProvider configGroupResourceProvider = createStrictMock(ConfigGroupResourceProvider.class);

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
    
    Set<StackConfigurationResponse> stackConfigurationResponses3 = new LinkedHashSet<StackConfigurationResponse>();
    stackConfigurationResponses2.add(stackConfigurationResponse5);

    Collection<HostGroupComponentEntity> hostGroupComponents = new LinkedHashSet<HostGroupComponentEntity>();
    hostGroupComponents.add(hostGroupComponent1);
    hostGroupComponents.add(hostGroupComponent2);
    hostGroupComponents.add(hostGroupComponent3);

    // request properties
    Set<Map<String, Object>> propertySet = new LinkedHashSet<Map<String, Object>>();
    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    properties.put(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID, clusterName);
    properties.put(ClusterResourceProvider.BLUEPRINT_PROPERTY_ID, blueprintName);
    propertySet.add(properties);

    Map<String, String> mapGroupConfigProperties = new HashMap<String, String>();
    mapGroupConfigProperties.put("myGroupProp", "awesomeValue");

    // blueprint cluster configuration properties
    Map<String, String> blueprintConfigProperties = new HashMap<String, String>();
    blueprintConfigProperties.put("property1", "value2");
    blueprintConfigProperties.put("new.property", "new.property.value");

    // expectations
    expect(request.getProperties()).andReturn(propertySet).anyTimes();
    expect(blueprintDAO.findByName(blueprintName)).andReturn(blueprint);
    expect(blueprint.getStackName()).andReturn(stackName);
    expect(blueprint.getStackVersion()).andReturn(stackVersion);

    expect(metaInfo.getComponentDependencies("test", "1.23", "service1", "component1")).
        andReturn(Collections.<DependencyInfo>emptyList());
    expect(metaInfo.getComponentDependencies("test", "1.23", "service1", "component2")).
        andReturn(Collections.<DependencyInfo>emptyList());
    expect(metaInfo.getComponentDependencies("test", "1.23", "service2", "component3")).
        andReturn(Collections.<DependencyInfo>emptyList());

    expect(managementController.getStackServices(capture(stackServiceRequestCapture))).andReturn(stackServiceResponses);
    expect(stackServiceResponse1.getServiceName()).andReturn("service1");
    expect(stackServiceResponse2.getServiceName()).andReturn("service2");

    expect(managementController.getStackComponents(capture(serviceComponentRequestCapture1))).
        andReturn(stackServiceComponentResponses1);
    expect(stackServiceComponentResponse1.getComponentName()).andReturn("component1");
    expect(stackServiceComponentResponse2.getComponentName()).andReturn("component2");

    expect(managementController.getStackConfigurations(capture(serviceConfigurationRequestCapture1))).
        andReturn(stackConfigurationResponses1);
    
    expect(managementController.getStackLevelConfigurations(capture(serviceLevelConfigurationRequestCapture1))).
        andReturn(stackConfigurationResponses3);
    
    expect(stackConfigurationResponse1.getType()).andReturn("core-site.xml");
    expect(stackConfigurationResponse1.getPropertyName()).andReturn("property1");
    expect(stackConfigurationResponse1.getPropertyValue()).andReturn("value1");

    expect(managementController.getStackComponents(capture(serviceComponentRequestCapture2))).
        andReturn(stackServiceComponentResponses2);
    expect(stackServiceComponentResponse3.getComponentName()).andReturn("component3");

    expect(managementController.getStackConfigurations(capture(serviceConfigurationRequestCapture2))).
        andReturn(stackConfigurationResponses2);
    expect(managementController.getStackLevelConfigurations(capture(serviceLevelConfigurationRequestCapture1))).
        andReturn(stackConfigurationResponses3);

    expect(stackConfigurationResponse2.getType()).andReturn("hdfs-site.xml");
    expect(stackConfigurationResponse2.getPropertyName()).andReturn("property2");
    expect(stackConfigurationResponse2.getPropertyValue()).andReturn("value2");

    expect(stackConfigurationResponse3.getType()).andReturn("global.xml");
    expect(stackConfigurationResponse3.getPropertyName()).andReturn("oozie_user");
    expect(stackConfigurationResponse3.getPropertyValue()).andReturn("oozie");

    expect(stackConfigurationResponse4.getType()).andReturn("core-site.xml");
    expect(stackConfigurationResponse4.getPropertyName()).andReturn("property3");
    expect(stackConfigurationResponse4.getPropertyValue()).andReturn("value3");
    
    expect(stackConfigurationResponse5.getType()).andReturn("cluster-env.xml").anyTimes();
    expect(stackConfigurationResponse5.getPropertyName()).andReturn("rqw").anyTimes();
    expect(stackConfigurationResponse5.getPropertyValue()).andReturn("aaaa").anyTimes();


    expect(blueprintConfig.getBlueprintName()).andReturn("test-blueprint").anyTimes();
    expect(blueprintConfig.getType()).andReturn("core-site").anyTimes();

    expect(blueprint.getHostGroups()).andReturn(Collections.singleton(hostGroup)).anyTimes();
    expect(hostGroup.getName()).andReturn("group1").anyTimes();
    expect(hostGroup.getComponents()).andReturn(hostGroupComponents).anyTimes();
    expect(hostGroupComponent1.getName()).andReturn("component1").anyTimes();
    expect(hostGroupComponent2.getName()).andReturn("component2").anyTimes();
    expect(hostGroupComponent3.getName()).andReturn("component3").anyTimes();
    expect(hostGroup.getConfigurations()).andReturn(
        Collections.<HostGroupConfigEntity>singleton(hostGroupConfig)).anyTimes();

    expect(hostGroupConfig.getType()).andReturn("core-site").anyTimes();
    expect(hostGroupConfig.getConfigData()).andReturn(new Gson().toJson(mapGroupConfigProperties)).anyTimes();

    replay(blueprintDAO, managementController, request, response, blueprint, stackServiceResponse1, stackServiceResponse2,
        stackServiceComponentResponse1, stackServiceComponentResponse2, stackServiceComponentResponse3,
        stackConfigurationResponse1, stackConfigurationResponse2, stackConfigurationResponse3, stackConfigurationResponse4,stackConfigurationResponse5,
        blueprintConfig, hostGroup, hostGroupComponent1, hostGroupComponent2, hostGroupComponent3, hostGroupConfig,
        serviceResourceProvider, componentResourceProvider, hostResourceProvider, hostComponentResourceProvider,
        configGroupResourceProvider, metaInfo);

    // test
    ClusterResourceProvider.init(blueprintDAO, metaInfo, configHelper);
    ResourceProvider provider = new TestClusterResourceProvider(
        managementController, serviceResourceProvider, componentResourceProvider,
        hostResourceProvider, hostComponentResourceProvider, configGroupResourceProvider);

    try {
      provider.createResources(request);
      fail("Expected exception for missing password property");
    } catch (IllegalArgumentException e) {
      //expected
    }
  }

  @Test
  public void testCreateResource_blueprint__hostGroupMissingName() throws Exception {
    String blueprintName = "test-blueprint";
    String stackName = "test";
    String stackVersion = "1.23";
    String clusterName = "c1";

    ConfigHelper configHelper = createNiceMock(ConfigHelper.class);
    BlueprintDAO blueprintDAO = createStrictMock(BlueprintDAO.class);
    AmbariMetaInfo metaInfo = createMock(AmbariMetaInfo.class);
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
    StackConfigurationResponse stackConfigurationResponse5 = createNiceMock(StackConfigurationResponse.class);
    Capture<Set<StackConfigurationRequest>> serviceConfigurationRequestCapture1 = new Capture<Set<StackConfigurationRequest>>();
    Capture<Set<StackConfigurationRequest>> serviceConfigurationRequestCapture2 = new Capture<Set<StackConfigurationRequest>>();
    Capture<Set<StackLevelConfigurationRequest>> serviceLevelConfigurationRequestCapture1 = new Capture<Set<StackLevelConfigurationRequest>>();
    
    BlueprintConfigEntity blueprintConfig = createNiceMock(BlueprintConfigEntity.class);

    HostGroupEntity hostGroup = createNiceMock(HostGroupEntity.class);
    HostGroupComponentEntity hostGroupComponent1 = createNiceMock(HostGroupComponentEntity.class);
    HostGroupComponentEntity hostGroupComponent2 = createNiceMock(HostGroupComponentEntity.class);
    HostGroupComponentEntity hostGroupComponent3 = createNiceMock(HostGroupComponentEntity.class);

    HostGroupConfigEntity hostGroupConfig = createNiceMock(HostGroupConfigEntity.class);

    ServiceResourceProvider serviceResourceProvider = createStrictMock(ServiceResourceProvider.class);
    ResourceProvider componentResourceProvider = createStrictMock(ResourceProvider.class);
    ResourceProvider hostResourceProvider = createStrictMock(ResourceProvider.class);
    ResourceProvider hostComponentResourceProvider = createStrictMock(ResourceProvider.class);
    ConfigGroupResourceProvider configGroupResourceProvider = createStrictMock(ConfigGroupResourceProvider.class);

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
    
    Set<StackConfigurationResponse> stackConfigurationResponses3 = new LinkedHashSet<StackConfigurationResponse>();
    stackConfigurationResponses2.add(stackConfigurationResponse5);

    Collection<HostGroupComponentEntity> hostGroupComponents = new LinkedHashSet<HostGroupComponentEntity>();
    hostGroupComponents.add(hostGroupComponent1);
    hostGroupComponents.add(hostGroupComponent2);
    hostGroupComponents.add(hostGroupComponent3);

    // request properties
    Set<Map<String, Object>> propertySet = new LinkedHashSet<Map<String, Object>>();
    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    properties.put(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID, clusterName);
    properties.put(ClusterResourceProvider.BLUEPRINT_PROPERTY_ID, blueprintName);
    propertySet.add(properties);

    Collection<Map<String, Object>> hostGroups = new ArrayList<Map<String, Object>>();
    Map<String, Object> hostGroupProperties = new HashMap<String, Object>();
    hostGroups.add(hostGroupProperties);
    Collection<Map<String, String>> hostGroupHosts = new ArrayList<Map<String, String>>();
    hostGroupProperties.put("hosts", hostGroupHosts);
    Map<String, String> hostGroupHostProperties = new HashMap<String, String>();
    hostGroupHostProperties.put("fqdn", "host.domain");
    hostGroupHosts.add(hostGroupHostProperties);
    properties.put("host_groups", hostGroups);

    Map<String, String> mapGroupConfigProperties = new HashMap<String, String>();
    mapGroupConfigProperties.put("myGroupProp", "awesomeValue");

    // blueprint cluster configuration properties
    Map<String, String> blueprintConfigProperties = new HashMap<String, String>();
    blueprintConfigProperties.put("property1", "value2");
    blueprintConfigProperties.put("new.property", "new.property.value");

    // expectations
    expect(request.getProperties()).andReturn(propertySet).anyTimes();
    expect(blueprintDAO.findByName(blueprintName)).andReturn(blueprint);
    expect(blueprint.getStackName()).andReturn(stackName);
    expect(blueprint.getStackVersion()).andReturn(stackVersion);

    expect(metaInfo.getComponentDependencies("test", "1.23", "service1", "component1")).
        andReturn(Collections.<DependencyInfo>emptyList());
    expect(metaInfo.getComponentDependencies("test", "1.23", "service1", "component2")).
        andReturn(Collections.<DependencyInfo>emptyList());
    expect(metaInfo.getComponentDependencies("test", "1.23", "service2", "component3")).
        andReturn(Collections.<DependencyInfo>emptyList());

    expect(managementController.getStackServices(capture(stackServiceRequestCapture))).andReturn(stackServiceResponses);
    expect(stackServiceResponse1.getServiceName()).andReturn("service1");
    expect(stackServiceResponse2.getServiceName()).andReturn("service2");

    expect(managementController.getStackComponents(capture(serviceComponentRequestCapture1))).
        andReturn(stackServiceComponentResponses1);
    expect(stackServiceComponentResponse1.getComponentName()).andReturn("component1");
    expect(stackServiceComponentResponse2.getComponentName()).andReturn("component2");

    expect(managementController.getStackConfigurations(capture(serviceConfigurationRequestCapture1))).
        andReturn(stackConfigurationResponses1);
    
    expect(managementController.getStackLevelConfigurations(capture(serviceLevelConfigurationRequestCapture1))).
        andReturn(stackConfigurationResponses3);

    expect(stackConfigurationResponse1.getType()).andReturn("core-site.xml");
    expect(stackConfigurationResponse1.getPropertyName()).andReturn("property1");
    expect(stackConfigurationResponse1.getPropertyValue()).andReturn("value1");

    expect(managementController.getStackComponents(capture(serviceComponentRequestCapture2))).
        andReturn(stackServiceComponentResponses2);
    expect(stackServiceComponentResponse3.getComponentName()).andReturn("component3");

    expect(managementController.getStackConfigurations(capture(serviceConfigurationRequestCapture2))).
        andReturn(stackConfigurationResponses2);
    expect(managementController.getStackLevelConfigurations(capture(serviceLevelConfigurationRequestCapture1))).
        andReturn(stackConfigurationResponses3);
    
    expect(stackConfigurationResponse2.getType()).andReturn("hdfs-site.xml");
    expect(stackConfigurationResponse2.getPropertyName()).andReturn("property2");
    expect(stackConfigurationResponse2.getPropertyValue()).andReturn("value2");

    expect(stackConfigurationResponse3.getType()).andReturn("global.xml");
    expect(stackConfigurationResponse3.getPropertyName()).andReturn("oozie_user");
    expect(stackConfigurationResponse3.getPropertyValue()).andReturn("oozie");

    expect(stackConfigurationResponse4.getType()).andReturn("core-site.xml");
    expect(stackConfigurationResponse4.getPropertyName()).andReturn("property3");
    expect(stackConfigurationResponse4.getPropertyValue()).andReturn("value3");
    
    expect(stackConfigurationResponse5.getType()).andReturn("cluster-env.xml").anyTimes();
    expect(stackConfigurationResponse5.getPropertyName()).andReturn("rqw").anyTimes();
    expect(stackConfigurationResponse5.getPropertyValue()).andReturn("aaaa").anyTimes();

    expect(blueprintConfig.getBlueprintName()).andReturn("test-blueprint").anyTimes();
    expect(blueprintConfig.getType()).andReturn("core-site").anyTimes();

    expect(blueprint.getHostGroups()).andReturn(Collections.singleton(hostGroup)).anyTimes();
    expect(hostGroup.getName()).andReturn("group1").anyTimes();
    expect(hostGroup.getComponents()).andReturn(hostGroupComponents).anyTimes();
    expect(hostGroupComponent1.getName()).andReturn("component1").anyTimes();
    expect(hostGroupComponent2.getName()).andReturn("component2").anyTimes();
    expect(hostGroupComponent3.getName()).andReturn("component3").anyTimes();
    expect(hostGroup.getConfigurations()).andReturn(
        Collections.<HostGroupConfigEntity>singleton(hostGroupConfig)).anyTimes();

    expect(hostGroupConfig.getType()).andReturn("core-site").anyTimes();
    expect(hostGroupConfig.getConfigData()).andReturn(new Gson().toJson(mapGroupConfigProperties)).anyTimes();

    replay(blueprintDAO, managementController, request, response, blueprint, stackServiceResponse1, stackServiceResponse2,
        stackServiceComponentResponse1, stackServiceComponentResponse2, stackServiceComponentResponse3,
        stackConfigurationResponse1, stackConfigurationResponse2, stackConfigurationResponse3, stackConfigurationResponse4,stackConfigurationResponse5,
        blueprintConfig, hostGroup, hostGroupComponent1, hostGroupComponent2, hostGroupComponent3, hostGroupConfig,
        serviceResourceProvider, componentResourceProvider, hostResourceProvider, hostComponentResourceProvider,
        configGroupResourceProvider, metaInfo);

    // test
    ClusterResourceProvider.init(blueprintDAO, metaInfo, configHelper);
    ResourceProvider provider = new TestClusterResourceProvider(
        managementController, serviceResourceProvider, componentResourceProvider,
        hostResourceProvider, hostComponentResourceProvider, configGroupResourceProvider);

    try {
      provider.createResources(request);
      fail("Expected exception for missing password property");
    } catch (IllegalArgumentException e) {
      //expected
    }
  }

  @Test
  public void testCreateResource_blueprint__hostGroupMissingFQDN() throws Exception {
    String blueprintName = "test-blueprint";
    String stackName = "test";
    String stackVersion = "1.23";
    String clusterName = "c1";

    ConfigHelper configHelper = createNiceMock(ConfigHelper.class);
    BlueprintDAO blueprintDAO = createStrictMock(BlueprintDAO.class);
    AmbariMetaInfo metaInfo = createMock(AmbariMetaInfo.class);
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
    StackConfigurationResponse stackConfigurationResponse5 = createNiceMock(StackConfigurationResponse.class);
    Capture<Set<StackConfigurationRequest>> serviceConfigurationRequestCapture1 = new Capture<Set<StackConfigurationRequest>>();
    Capture<Set<StackConfigurationRequest>> serviceConfigurationRequestCapture2 = new Capture<Set<StackConfigurationRequest>>();
    Capture<Set<StackLevelConfigurationRequest>> serviceLevelConfigurationRequestCapture1 = new Capture<Set<StackLevelConfigurationRequest>>();
    
    BlueprintConfigEntity blueprintConfig = createNiceMock(BlueprintConfigEntity.class);

    HostGroupEntity hostGroup = createNiceMock(HostGroupEntity.class);
    HostGroupComponentEntity hostGroupComponent1 = createNiceMock(HostGroupComponentEntity.class);
    HostGroupComponentEntity hostGroupComponent2 = createNiceMock(HostGroupComponentEntity.class);
    HostGroupComponentEntity hostGroupComponent3 = createNiceMock(HostGroupComponentEntity.class);

    HostGroupConfigEntity hostGroupConfig = createNiceMock(HostGroupConfigEntity.class);

    ServiceResourceProvider serviceResourceProvider = createStrictMock(ServiceResourceProvider.class);
    ResourceProvider componentResourceProvider = createStrictMock(ResourceProvider.class);
    ResourceProvider hostResourceProvider = createStrictMock(ResourceProvider.class);
    ResourceProvider hostComponentResourceProvider = createStrictMock(ResourceProvider.class);
    ConfigGroupResourceProvider configGroupResourceProvider = createStrictMock(ConfigGroupResourceProvider.class);

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
    
    Set<StackConfigurationResponse> stackConfigurationResponses3 = new LinkedHashSet<StackConfigurationResponse>();
    stackConfigurationResponses3.add(stackConfigurationResponse5);

    Collection<HostGroupComponentEntity> hostGroupComponents = new LinkedHashSet<HostGroupComponentEntity>();
    hostGroupComponents.add(hostGroupComponent1);
    hostGroupComponents.add(hostGroupComponent2);
    hostGroupComponents.add(hostGroupComponent3);

    // request properties
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
        hostGroupHosts.add(hostGroupHostProperties);
    properties.put("host_groups", hostGroups);

    Map<String, String> mapGroupConfigProperties = new HashMap<String, String>();
    mapGroupConfigProperties.put("myGroupProp", "awesomeValue");

    // blueprint cluster configuration properties
    Map<String, String> blueprintConfigProperties = new HashMap<String, String>();
    blueprintConfigProperties.put("property1", "value2");
    blueprintConfigProperties.put("new.property", "new.property.value");

    // expectations
    expect(request.getProperties()).andReturn(propertySet).anyTimes();
    expect(blueprintDAO.findByName(blueprintName)).andReturn(blueprint);
    expect(blueprint.getStackName()).andReturn(stackName);
    expect(blueprint.getStackVersion()).andReturn(stackVersion);

    expect(metaInfo.getComponentDependencies("test", "1.23", "service1", "component1")).
        andReturn(Collections.<DependencyInfo>emptyList());
    expect(metaInfo.getComponentDependencies("test", "1.23", "service1", "component2")).
        andReturn(Collections.<DependencyInfo>emptyList());
    expect(metaInfo.getComponentDependencies("test", "1.23", "service2", "component3")).
        andReturn(Collections.<DependencyInfo>emptyList());

    expect(managementController.getStackServices(capture(stackServiceRequestCapture))).andReturn(stackServiceResponses);
    expect(stackServiceResponse1.getServiceName()).andReturn("service1");
    expect(stackServiceResponse2.getServiceName()).andReturn("service2");

    expect(managementController.getStackComponents(capture(serviceComponentRequestCapture1))).
        andReturn(stackServiceComponentResponses1);
    expect(stackServiceComponentResponse1.getComponentName()).andReturn("component1");
    expect(stackServiceComponentResponse2.getComponentName()).andReturn("component2");

    expect(managementController.getStackConfigurations(capture(serviceConfigurationRequestCapture1))).
        andReturn(stackConfigurationResponses1);
    expect(managementController.getStackLevelConfigurations(capture(serviceLevelConfigurationRequestCapture1))).
        andReturn(stackConfigurationResponses3);

    expect(stackConfigurationResponse1.getType()).andReturn("core-site.xml");
    expect(stackConfigurationResponse1.getPropertyName()).andReturn("property1");
    expect(stackConfigurationResponse1.getPropertyValue()).andReturn("value1");

    expect(managementController.getStackComponents(capture(serviceComponentRequestCapture2))).
        andReturn(stackServiceComponentResponses2);
    expect(stackServiceComponentResponse3.getComponentName()).andReturn("component3");

    expect(managementController.getStackConfigurations(capture(serviceConfigurationRequestCapture2))).
        andReturn(stackConfigurationResponses2);
    expect(managementController.getStackLevelConfigurations(capture(serviceLevelConfigurationRequestCapture1))).
        andReturn(stackConfigurationResponses3);
    expect(stackConfigurationResponse2.getType()).andReturn("hdfs-site.xml");
    expect(stackConfigurationResponse2.getPropertyName()).andReturn("property2");
    expect(stackConfigurationResponse2.getPropertyValue()).andReturn("value2");

    expect(stackConfigurationResponse3.getType()).andReturn("global.xml");
    expect(stackConfigurationResponse3.getPropertyName()).andReturn("oozie_user");
    expect(stackConfigurationResponse3.getPropertyValue()).andReturn("oozie");

    expect(stackConfigurationResponse4.getType()).andReturn("core-site.xml");
    expect(stackConfigurationResponse4.getPropertyName()).andReturn("property3");
    expect(stackConfigurationResponse4.getPropertyValue()).andReturn("value3");
    
    expect(stackConfigurationResponse5.getType()).andReturn("cluster-env.xml").anyTimes();
    expect(stackConfigurationResponse5.getPropertyName()).andReturn("rqw").anyTimes();
    expect(stackConfigurationResponse5.getPropertyValue()).andReturn("aaaa").anyTimes();

    expect(blueprintConfig.getBlueprintName()).andReturn("test-blueprint").anyTimes();
    expect(blueprintConfig.getType()).andReturn("core-site").anyTimes();

    expect(blueprint.getHostGroups()).andReturn(Collections.singleton(hostGroup)).anyTimes();
    expect(hostGroup.getName()).andReturn("group1").anyTimes();
    expect(hostGroup.getComponents()).andReturn(hostGroupComponents).anyTimes();
    expect(hostGroupComponent1.getName()).andReturn("component1").anyTimes();
    expect(hostGroupComponent2.getName()).andReturn("component2").anyTimes();
    expect(hostGroupComponent3.getName()).andReturn("component3").anyTimes();
    expect(hostGroup.getConfigurations()).andReturn(
        Collections.<HostGroupConfigEntity>singleton(hostGroupConfig)).anyTimes();

    expect(hostGroupConfig.getType()).andReturn("core-site").anyTimes();
    expect(hostGroupConfig.getConfigData()).andReturn(new Gson().toJson(mapGroupConfigProperties)).anyTimes();

    replay(blueprintDAO, managementController, request, response, blueprint, stackServiceResponse1, stackServiceResponse2,
        stackServiceComponentResponse1, stackServiceComponentResponse2, stackServiceComponentResponse3,
        stackConfigurationResponse1, stackConfigurationResponse2, stackConfigurationResponse3, stackConfigurationResponse4,stackConfigurationResponse5,
        blueprintConfig, hostGroup, hostGroupComponent1, hostGroupComponent2, hostGroupComponent3, hostGroupConfig,
        serviceResourceProvider, componentResourceProvider, hostResourceProvider, hostComponentResourceProvider,
        configGroupResourceProvider, metaInfo);

    // test
    ClusterResourceProvider.init(blueprintDAO, metaInfo, configHelper);
    ResourceProvider provider = new TestClusterResourceProvider(
        managementController, serviceResourceProvider, componentResourceProvider,
        hostResourceProvider, hostComponentResourceProvider, configGroupResourceProvider);

    try {
      provider.createResources(request);
      fail("Expected exception for missing password property");
    } catch (IllegalArgumentException e) {
      //expected
    }
  }

  @Test
  public void testCreateResource_blueprint__defaultPassword() throws Exception {
    String blueprintName = "test-blueprint";
    String stackName = "test";
    String stackVersion = "1.23";
    String clusterName = "c1";

    ConfigHelper configHelper = createNiceMock(ConfigHelper.class);
    BlueprintDAO blueprintDAO = createStrictMock(BlueprintDAO.class);
    AmbariMetaInfo metaInfo = createMock(AmbariMetaInfo.class);
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
    StackConfigurationResponse stackConfigurationResponse5 = createNiceMock(StackConfigurationResponse.class);
    StackConfigurationResponse stackConfigurationResponse6 = createNiceMock(StackConfigurationResponse.class);
    StackConfigurationResponse stackConfigurationResponse7 = createNiceMock(StackConfigurationResponse.class);
    StackConfigurationResponse stackConfigurationResponse8 = createNiceMock(StackConfigurationResponse.class);
    Capture<Set<StackServiceComponentRequest>> serviceComponentRequestCapture1 = new Capture<Set<StackServiceComponentRequest>>();
    Capture<Set<StackServiceComponentRequest>> serviceComponentRequestCapture2 = new Capture<Set<StackServiceComponentRequest>>();
    Capture<Set<StackLevelConfigurationRequest>> serviceLevelConfigurationRequestCapture1 = new Capture<Set<StackLevelConfigurationRequest>>();
    
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

    HostGroupConfigEntity hostGroupConfig = createNiceMock(HostGroupConfigEntity.class);

    ServiceResourceProvider serviceResourceProvider = createStrictMock(ServiceResourceProvider.class);
    ResourceProvider componentResourceProvider = createStrictMock(ResourceProvider.class);
    ResourceProvider hostResourceProvider = createStrictMock(ResourceProvider.class);
    ResourceProvider hostComponentResourceProvider = createStrictMock(ResourceProvider.class);
    ConfigGroupResourceProvider configGroupResourceProvider = createStrictMock(ConfigGroupResourceProvider.class);

    Capture<ClusterRequest> createClusterRequestCapture = new Capture<ClusterRequest>();
    Capture<Set<ClusterRequest>> updateClusterRequestCapture = new Capture<Set<ClusterRequest>>();
    Capture<Map<String, String>> updateClusterPropertyMapCapture = new Capture<Map<String, String>>();
    Capture<Set<ClusterRequest>> updateClusterRequestCapture2 = new Capture<Set<ClusterRequest>>();
    Capture<Map<String, String>> updateClusterPropertyMapCapture2 = new Capture<Map<String, String>>();
    Capture<Set<ClusterRequest>> updateClusterRequestCapture3 = new Capture<Set<ClusterRequest>>();
    Capture<Map<String, String>> updateClusterPropertyMapCapture3 = new Capture<Map<String, String>>();

    Capture<Set<ClusterRequest>> persistUIStateRequestCapture = new Capture<Set<ClusterRequest>>();
    Capture<Map<String, String>> persistUIStatePropertyMapCapture = new Capture<Map<String, String>>();
    
    Capture<Request> serviceRequestCapture = new Capture<Request>();
    Capture<Request> componentRequestCapture = new Capture<Request>();
    Capture<Request> componentRequestCapture2 = new Capture<Request>();
    Capture<Request> hostRequestCapture = new Capture<Request>();
    Capture<Request> hostComponentRequestCapture = new Capture<Request>();
    Capture<Set<ConfigGroupRequest>> configGroupRequestCapture = new Capture<Set<ConfigGroupRequest>>();

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
    stackConfigurationResponses2.add(stackConfigurationResponse5);
    stackConfigurationResponses2.add(stackConfigurationResponse6);
    stackConfigurationResponses2.add(stackConfigurationResponse7);

    Set<StackConfigurationResponse> stackConfigurationResponses3 = new LinkedHashSet<StackConfigurationResponse>();
    stackConfigurationResponses3.add(stackConfigurationResponse8);
    
    Collection<HostGroupComponentEntity> hostGroupComponents = new LinkedHashSet<HostGroupComponentEntity>();
    hostGroupComponents.add(hostGroupComponent1);
    hostGroupComponents.add(hostGroupComponent2);
    hostGroupComponents.add(hostGroupComponent3);

    // request properties
    Set<Map<String, Object>> propertySet = new LinkedHashSet<Map<String, Object>>();
    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    properties.put(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID, clusterName);
    properties.put(ClusterResourceProvider.BLUEPRINT_PROPERTY_ID, blueprintName);
    properties.put("default_password", "foo");
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

    // blueprint cluster configuration properties
    Map<String, String> blueprintConfigProperties = new HashMap<String, String>();
    blueprintConfigProperties.put("property1", "value2");
    blueprintConfigProperties.put("new.property", "new.property.value");

    Map<String, Map<String, Collection<String>>> allMissingPasswords = new HashMap<String, Map<String, Collection<String>>>();
    Map<String, Collection<String>> missingHGPasswords = new HashMap<String, Collection<String>>();
    Collection<String> missingPasswords = new ArrayList<String>();
    missingPasswords.add("my.missing.password");
    missingPasswords.add("my.missing.password2");
    missingHGPasswords.put("core-site", missingPasswords);
    allMissingPasswords.put("group1", missingHGPasswords);

    // expectations
    expect(request.getProperties()).andReturn(propertySet).anyTimes();
    expect(blueprintDAO.findByName(blueprintName)).andReturn(blueprint);
    expect(blueprint.getStackName()).andReturn(stackName);
    expect(blueprint.getStackVersion()).andReturn(stackVersion);
    expect(blueprint.getConfigurations()).andReturn(Collections.<BlueprintConfigEntity>singletonList(blueprintConfig)).anyTimes();
    expect(blueprint.validateConfigurations(metaInfo, true)).andReturn(allMissingPasswords);

    expect(metaInfo.getComponentDependencies("test", "1.23", "service1", "component1")).
        andReturn(Collections.<DependencyInfo>emptyList());
    expect(metaInfo.getComponentDependencies("test", "1.23", "service1", "component2")).
        andReturn(Collections.<DependencyInfo>emptyList());
    expect(metaInfo.getComponentDependencies("test", "1.23", "service2", "component3")).
        andReturn(Collections.<DependencyInfo>emptyList());

    expect(managementController.getStackServices(capture(stackServiceRequestCapture))).andReturn(stackServiceResponses);
    expect(stackServiceResponse1.getServiceName()).andReturn("service1");
    expect(stackServiceResponse2.getServiceName()).andReturn("service2");

    expect(managementController.getStackComponents(capture(serviceComponentRequestCapture1))).
        andReturn(stackServiceComponentResponses1);
    expect(stackServiceComponentResponse1.getComponentName()).andReturn("component1");
    expect(stackServiceComponentResponse2.getComponentName()).andReturn("component2");

    expect(managementController.getStackConfigurations(capture(serviceConfigurationRequestCapture1))).
        andReturn(stackConfigurationResponses1);
    expect(managementController.getStackLevelConfigurations(capture(serviceLevelConfigurationRequestCapture1))).
        andReturn(stackConfigurationResponses3);

    expect(stackConfigurationResponse1.getType()).andReturn("core-site.xml");
    expect(stackConfigurationResponse1.getPropertyName()).andReturn("property1");
    expect(stackConfigurationResponse1.getPropertyValue()).andReturn("value1");

    expect(managementController.getStackComponents(capture(serviceComponentRequestCapture2))).
        andReturn(stackServiceComponentResponses2);
    expect(stackServiceComponentResponse3.getComponentName()).andReturn("component3");

    expect(managementController.getStackConfigurations(capture(serviceConfigurationRequestCapture2))).
        andReturn(stackConfigurationResponses2);
    expect(managementController.getStackLevelConfigurations(capture(serviceLevelConfigurationRequestCapture1))).
        andReturn(stackConfigurationResponses3);
    expect(stackConfigurationResponse2.getType()).andReturn("hdfs-site.xml");
    expect(stackConfigurationResponse2.getPropertyName()).andReturn("property2");
    expect(stackConfigurationResponse2.getPropertyValue()).andReturn("value2");

    expect(stackConfigurationResponse3.getType()).andReturn("hive-env.xml");
    expect(stackConfigurationResponse3.getPropertyName()).andReturn("property3");
    expect(stackConfigurationResponse3.getPropertyValue()).andReturn("value3");

    expect(stackConfigurationResponse4.getType()).andReturn("core-site.xml");
    expect(stackConfigurationResponse4.getPropertyName()).andReturn("property3");
    expect(stackConfigurationResponse4.getPropertyValue()).andReturn("value3");
    
    expect(stackConfigurationResponse5.getType()).andReturn("hbase-env.xml");
    expect(stackConfigurationResponse5.getPropertyName()).andReturn("property3");
    expect(stackConfigurationResponse5.getPropertyValue()).andReturn("value3");
    
    expect(stackConfigurationResponse6.getType()).andReturn("falcon-env.xml");
    expect(stackConfigurationResponse6.getPropertyName()).andReturn("property3");
    expect(stackConfigurationResponse6.getPropertyValue()).andReturn("value3");
    
    expect(stackConfigurationResponse7.getType()).andReturn("oozie-env.xml");
    expect(stackConfigurationResponse7.getPropertyName()).andReturn("oozie_user");
    expect(stackConfigurationResponse7.getPropertyValue()).andReturn("oozie");
    
    expect(stackConfigurationResponse8.getType()).andReturn("cluster-env.xml").anyTimes();
    expect(stackConfigurationResponse8.getPropertyName()).andReturn("rqw").anyTimes();
    expect(stackConfigurationResponse8.getPropertyValue()).andReturn("aaaa").anyTimes();

    expect(blueprintConfig.getBlueprintName()).andReturn("test-blueprint").anyTimes();
    expect(blueprintConfig.getType()).andReturn("core-site").anyTimes();
    expect(blueprintConfig.getConfigData()).andReturn(new Gson().toJson(blueprintConfigProperties));

    expect(blueprint.getHostGroups()).andReturn(Collections.singleton(hostGroup)).anyTimes();
    expect(hostGroup.getName()).andReturn("group1").anyTimes();
    expect(hostGroup.getComponents()).andReturn(hostGroupComponents).anyTimes();
    expect(hostGroupComponent1.getName()).andReturn("component1").anyTimes();
    expect(hostGroupComponent2.getName()).andReturn("component2").anyTimes();
    expect(hostGroupComponent3.getName()).andReturn("component3").anyTimes();
    expect(hostGroup.getConfigurations()).andReturn(
        Collections.<HostGroupConfigEntity>singleton(hostGroupConfig)).anyTimes();

    expect(hostGroupConfig.getType()).andReturn("core-site").anyTimes();
    expect(hostGroupConfig.getConfigData()).andReturn(new Gson().toJson(mapGroupConfigProperties)).anyTimes();

    managementController.createCluster(capture(createClusterRequestCapture));
    // expect three ClusterRequests to be generated for configuration
    expect(managementController.updateClusters(capture(updateClusterRequestCapture),
        capture(updateClusterPropertyMapCapture))).andReturn(null);
    expect(managementController.updateClusters(capture(updateClusterRequestCapture2),
        capture(updateClusterPropertyMapCapture2))).andReturn(null);
    expect(managementController.updateClusters(capture(updateClusterRequestCapture3),
        capture(updateClusterPropertyMapCapture3))).andReturn(null);


    // set state for UI
    expect(managementController.updateClusters(capture(persistUIStateRequestCapture),
        capture(persistUIStatePropertyMapCapture))).andReturn(null);

    expect(serviceResourceProvider.createResources(capture(serviceRequestCapture))).andReturn(null);
    expect(componentResourceProvider.createResources(capture(componentRequestCapture))).andReturn(null);
    expect(componentResourceProvider.createResources(capture(componentRequestCapture2))).andReturn(null);
    expect(hostResourceProvider.createResources(capture(hostRequestCapture))).andReturn(null);
    expect(hostComponentResourceProvider.createResources(capture(hostComponentRequestCapture))).andReturn(null);

    expect(serviceResourceProvider.installAndStart(clusterName)).andReturn(response);

    expect(configGroupResourceProvider.createResources(
        capture(configGroupRequestCapture))).andReturn(null);


    replay(blueprintDAO, managementController, request, response, blueprint, stackServiceResponse1, stackServiceResponse2,
        stackServiceComponentResponse1, stackServiceComponentResponse2, stackServiceComponentResponse3,
        stackConfigurationResponse1, stackConfigurationResponse2, stackConfigurationResponse3, stackConfigurationResponse4,
        stackConfigurationResponse5, stackConfigurationResponse6, stackConfigurationResponse7, stackConfigurationResponse8,
        blueprintConfig, hostGroup, hostGroupComponent1, hostGroupComponent2, hostGroupComponent3, hostGroupConfig,
        serviceResourceProvider, componentResourceProvider, hostResourceProvider, hostComponentResourceProvider,
        configGroupResourceProvider, metaInfo);

    // test
    ClusterResourceProvider.init(blueprintDAO, metaInfo, configHelper);
    ResourceProvider provider = new TestClusterResourceProvider(
        managementController, serviceResourceProvider, componentResourceProvider,
        hostResourceProvider, hostComponentResourceProvider, configGroupResourceProvider) {
      @Override
      protected boolean isServiceIncluded(String serviceName, Map<String, HostGroupImpl> blueprintHostGroups) {
        return true;
      }
    };

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
    assertEquals(clusterName, clusterRequest.getClusterName());
    assertEquals("test-1.23", clusterRequest.getStackVersion());

    Set<ClusterRequest> updateClusterRequest1 = updateClusterRequestCapture.getValue();
    Set<ClusterRequest> updateClusterRequest2 = updateClusterRequestCapture2.getValue();
    Set<ClusterRequest> updateClusterRequest3 = updateClusterRequestCapture3.getValue();
    Set<ClusterRequest> persistUIStateRequest = persistUIStateRequestCapture.getValue();
    
    assertEquals(1, updateClusterRequest1.size());
    assertEquals(1, updateClusterRequest2.size());
    assertEquals(1, updateClusterRequest3.size());
    assertEquals(1, persistUIStateRequest.size());
    ClusterRequest ucr1 = updateClusterRequest1.iterator().next();
    ClusterRequest ucr2 = updateClusterRequest2.iterator().next();
    ClusterRequest ucr3 = updateClusterRequest3.iterator().next();
    ClusterRequest ucr4 = persistUIStateRequest.iterator().next();
    assertEquals(clusterName, ucr1.getClusterName());
    assertEquals(clusterName, ucr2.getClusterName());
    assertEquals(clusterName, ucr3.getClusterName());

    assertEquals("INSTALLED", ucr4.getProvisioningState());

    ClusterRequest[] arrayOfClusterRequests =
      { ucr1, ucr2, ucr3 };

    // assert that all ConfigRequests instances have version of "1"
    for (ClusterRequest testRequest : arrayOfClusterRequests) {
      for (ConfigurationRequest testConfigRequest : testRequest.getDesiredConfig()) {
        assertEquals("1", testConfigRequest.getVersionTag());
      }
    }

    Map<String, ConfigurationRequest> mapConfigRequests = new HashMap<String, ConfigurationRequest>();

    // add all the associated config types to the test map
    // with the config versioning change, there could be
    // more than one config type associated with a ClusterRequest
    for (ClusterRequest testClusterRequest : arrayOfClusterRequests) {
      for (ConfigurationRequest configRequest : testClusterRequest.getDesiredConfig()) {
        mapConfigRequests.put(configRequest.getType(), configRequest);
      }
    }

    assertEquals(7, mapConfigRequests.size());
    
    ConfigurationRequest hdfsConfigRequest = mapConfigRequests.get("hdfs-site");
    assertEquals(1, hdfsConfigRequest.getProperties().size());
    assertEquals("value2", hdfsConfigRequest.getProperties().get("property2"));
    ConfigurationRequest coreConfigRequest = mapConfigRequests.get("core-site");
    assertEquals(7, coreConfigRequest.getProperties().size());
    assertEquals("value2", coreConfigRequest.getProperties().get("property1"));
    assertEquals("value3", coreConfigRequest.getProperties().get("property3"));
    assertEquals("*", coreConfigRequest.getProperties().get("hadoop.proxyuser.oozie.hosts"));
    assertEquals("users", coreConfigRequest.getProperties().get("hadoop.proxyuser.oozie.groups"));
    assertEquals("new.property.value", coreConfigRequest.getProperties().get("new.property"));
    assertEquals("foo", coreConfigRequest.getProperties().get("my.missing.password"));
    assertEquals("foo", coreConfigRequest.getProperties().get("my.missing.password2"));
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
      assertEquals(clusterName, componentRequest1Properties.get("ServiceComponentInfo/cluster_name"));
      assertEquals("service1", componentRequest1Properties.get("ServiceComponentInfo/service_name"));
      componentRequest1Names.add((String) componentRequest1Properties.get("ServiceComponentInfo/component_name"));
    }
    assertTrue(componentRequest1Names.contains("component1") && componentRequest1Names.contains("component2"));
    assertEquals(1, componentRequest2.getProperties().size());
    Map<String, Object> componentRequest2Properties = componentRequest2.getProperties().iterator().next();
    assertEquals(clusterName, componentRequest2Properties.get("ServiceComponentInfo/cluster_name"));
    assertEquals("service2", componentRequest2Properties.get("ServiceComponentInfo/service_name"));
    assertEquals("component3", componentRequest2Properties.get("ServiceComponentInfo/component_name"));
    Request hostRequest = hostRequestCapture.getValue();
    assertEquals(1, hostRequest.getProperties().size());
    assertEquals(clusterName, hostRequest.getProperties().iterator().next().get("Hosts/cluster_name"));
    assertEquals("host.domain", hostRequest.getProperties().iterator().next().get("Hosts/host_name"));
    Request hostComponentRequest = hostComponentRequestCapture.getValue();
    assertEquals(3, hostComponentRequest.getProperties().size());
    Set<String> componentNames = new HashSet<String>();
    for (Map<String, Object> hostComponentProperties : hostComponentRequest.getProperties()) {
      assertEquals(3, hostComponentProperties.size());
      assertEquals(clusterName, hostComponentProperties.get("HostRoles/cluster_name"));
      assertEquals("host.domain", hostComponentProperties.get("HostRoles/host_name"));
      componentNames.add((String) hostComponentProperties.get("HostRoles/component_name"));
    }
    assertTrue(componentNames.contains("component1") && componentNames.contains("component2") &&
        componentNames.contains("component3"));

    Set<ConfigGroupRequest> configGroupRequests = configGroupRequestCapture.getValue();
    assertEquals(1, configGroupRequests.size());
    ConfigGroupRequest configGroupRequest = configGroupRequests.iterator().next();
    assertEquals(clusterName, configGroupRequest.getClusterName());
    assertEquals("group1", configGroupRequest.getGroupName());
    assertEquals("service1", configGroupRequest.getTag());
    assertEquals("Host Group Configuration", configGroupRequest.getDescription());
    Set<String> hosts = configGroupRequest.getHosts();
    assertEquals(1, hosts.size());
    assertEquals("host.domain", hosts.iterator().next());
    assertEquals(1, configGroupRequest.getConfigs().size());

    verify(blueprintDAO, managementController, request, response, blueprint, stackServiceResponse1, stackServiceResponse2,
        stackServiceComponentResponse1, stackServiceComponentResponse2, stackServiceComponentResponse3,
        stackConfigurationResponse1, stackConfigurationResponse2, stackConfigurationResponse3, stackConfigurationResponse4,
        stackConfigurationResponse5, stackConfigurationResponse6, stackConfigurationResponse7,
        blueprintConfig, hostGroup, hostGroupComponent1, hostGroupComponent2, hostGroupComponent3, hostGroupConfig,
        serviceResourceProvider, componentResourceProvider, hostResourceProvider, hostComponentResourceProvider,
        configGroupResourceProvider);
  }

  @Test
  public void testCreateResource_blueprint_hostMappedToMultipleHG() throws Exception {
    String blueprintName = "test-blueprint";
    String stackName = "test";
    String stackVersion = "1.23";
    String clusterName = "c1";

    ConfigHelper configHelper = createNiceMock(ConfigHelper.class);
    BlueprintDAO blueprintDAO = createStrictMock(BlueprintDAO.class);
    AmbariMetaInfo metaInfo = createMock(AmbariMetaInfo.class);
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
    StackConfigurationResponse stackConfigurationResponse5 = createNiceMock(StackConfigurationResponse.class);
    Capture<Set<StackConfigurationRequest>> serviceConfigurationRequestCapture1 = new Capture<Set<StackConfigurationRequest>>();
    Capture<Set<StackConfigurationRequest>> serviceConfigurationRequestCapture2 = new Capture<Set<StackConfigurationRequest>>();
    Capture<Set<StackLevelConfigurationRequest>> serviceLevelConfigurationRequestCapture1 = new Capture<Set<StackLevelConfigurationRequest>>();

    BlueprintConfigEntity blueprintConfig = createNiceMock(BlueprintConfigEntity.class);

    HostGroupEntity hostGroup = createNiceMock(HostGroupEntity.class);
    HostGroupComponentEntity hostGroupComponent1 = createNiceMock(HostGroupComponentEntity.class);
    HostGroupComponentEntity hostGroupComponent2 = createNiceMock(HostGroupComponentEntity.class);
    HostGroupComponentEntity hostGroupComponent3 = createNiceMock(HostGroupComponentEntity.class);

    HostGroupConfigEntity hostGroupConfig = createNiceMock(HostGroupConfigEntity.class);

    ServiceResourceProvider serviceResourceProvider = createStrictMock(ServiceResourceProvider.class);
    ResourceProvider componentResourceProvider = createStrictMock(ResourceProvider.class);
    ResourceProvider hostResourceProvider = createStrictMock(ResourceProvider.class);
    ResourceProvider hostComponentResourceProvider = createStrictMock(ResourceProvider.class);
    ConfigGroupResourceProvider configGroupResourceProvider = createStrictMock(ConfigGroupResourceProvider.class);

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
    Capture<Set<ConfigGroupRequest>> configGroupRequestCapture = new Capture<Set<ConfigGroupRequest>>();

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
    
    Set<StackConfigurationResponse> stackConfigurationResponses3 = new LinkedHashSet<StackConfigurationResponse>();
    stackConfigurationResponses3.add(stackConfigurationResponse5);

    Collection<HostGroupComponentEntity> hostGroupComponents = new LinkedHashSet<HostGroupComponentEntity>();
    hostGroupComponents.add(hostGroupComponent1);
    hostGroupComponents.add(hostGroupComponent2);
    hostGroupComponents.add(hostGroupComponent3);

    // request properties
    Set<Map<String, Object>> propertySet = new LinkedHashSet<Map<String, Object>>();
    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    properties.put(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID, clusterName);
    properties.put(ClusterResourceProvider.BLUEPRINT_PROPERTY_ID, blueprintName);
    propertySet.add(properties);

    Collection<Map<String, Object>> hostGroups = new ArrayList<Map<String, Object>>();
    Map<String, Object> hostGroupProperties1 = new HashMap<String, Object>();
    Map<String, Object> hostGroupProperties2 = new HashMap<String, Object>();
    hostGroups.add(hostGroupProperties1);
    hostGroups.add(hostGroupProperties2);
    hostGroupProperties1.put("name", "group1");
    hostGroupProperties2.put("name", "group2");
    Collection<Map<String, String>> hostGroupHosts = new ArrayList<Map<String, String>>();
    hostGroupProperties1.put("hosts", hostGroupHosts);
    hostGroupProperties2.put("hosts", hostGroupHosts);
    Map<String, String> hostGroupHostProperties = new HashMap<String, String>();
    hostGroupHostProperties.put("fqdn", "host.domain");
    hostGroupHosts.add(hostGroupHostProperties);
    properties.put("host_groups", hostGroups);

    Map<String, String> mapGroupConfigProperties = new HashMap<String, String>();
    mapGroupConfigProperties.put("myGroupProp", "awesomeValue");

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
    expect(blueprint.validateConfigurations(metaInfo, true)).andReturn(
        Collections.<String, Map<String, Collection<String>>>emptyMap());

    expect(metaInfo.getComponentDependencies("test", "1.23", "service1", "component1")).
        andReturn(Collections.<DependencyInfo>emptyList());
    expect(metaInfo.getComponentDependencies("test", "1.23", "service1", "component2")).
        andReturn(Collections.<DependencyInfo>emptyList());
    expect(metaInfo.getComponentDependencies("test", "1.23", "service2", "component3")).
        andReturn(Collections.<DependencyInfo>emptyList());

    expect(managementController.getStackServices(capture(stackServiceRequestCapture))).andReturn(stackServiceResponses);
    expect(stackServiceResponse1.getServiceName()).andReturn("service1");
    expect(stackServiceResponse2.getServiceName()).andReturn("service2");

    expect(managementController.getStackComponents(capture(serviceComponentRequestCapture1))).
        andReturn(stackServiceComponentResponses1);
    expect(stackServiceComponentResponse1.getComponentName()).andReturn("component1");
    expect(stackServiceComponentResponse2.getComponentName()).andReturn("component2");

    expect(managementController.getStackConfigurations(capture(serviceConfigurationRequestCapture1))).
        andReturn(stackConfigurationResponses1);
    expect(managementController.getStackLevelConfigurations(capture(serviceLevelConfigurationRequestCapture1))).
        andReturn(stackConfigurationResponses3);
    expect(stackConfigurationResponse1.getType()).andReturn("core-site.xml");
    expect(stackConfigurationResponse1.getPropertyName()).andReturn("property1");
    expect(stackConfigurationResponse1.getPropertyValue()).andReturn("value1");

    expect(managementController.getStackComponents(capture(serviceComponentRequestCapture2))).
        andReturn(stackServiceComponentResponses2);
    expect(stackServiceComponentResponse3.getComponentName()).andReturn("component3");

    expect(managementController.getStackConfigurations(capture(serviceConfigurationRequestCapture2))).
        andReturn(stackConfigurationResponses2);
    expect(managementController.getStackLevelConfigurations(capture(serviceLevelConfigurationRequestCapture1))).
        andReturn(stackConfigurationResponses3);
    expect(stackConfigurationResponse2.getType()).andReturn("hdfs-site.xml");
    expect(stackConfigurationResponse2.getPropertyName()).andReturn("property2");
    expect(stackConfigurationResponse2.getPropertyValue()).andReturn("value2");

    expect(stackConfigurationResponse3.getType()).andReturn("global.xml");
    expect(stackConfigurationResponse3.getPropertyName()).andReturn("oozie_user");
    expect(stackConfigurationResponse3.getPropertyValue()).andReturn("oozie");

    expect(stackConfigurationResponse4.getType()).andReturn("core-site.xml");
    expect(stackConfigurationResponse4.getPropertyName()).andReturn("property3");
    expect(stackConfigurationResponse4.getPropertyValue()).andReturn("value3");
    
    expect(stackConfigurationResponse5.getType()).andReturn("cluster-env.xml").anyTimes();
    expect(stackConfigurationResponse5.getPropertyName()).andReturn("rqw").anyTimes();
    expect(stackConfigurationResponse5.getPropertyValue()).andReturn("aaaa").anyTimes();


    expect(blueprintConfig.getBlueprintName()).andReturn("test-blueprint").anyTimes();
    expect(blueprintConfig.getType()).andReturn("core-site").anyTimes();
    expect(blueprintConfig.getConfigData()).andReturn(new Gson().toJson(blueprintConfigProperties));

    expect(blueprint.getHostGroups()).andReturn(Collections.singleton(hostGroup)).anyTimes();
    expect(hostGroup.getName()).andReturn("group1").anyTimes();
    expect(hostGroup.getComponents()).andReturn(hostGroupComponents).anyTimes();
    expect(hostGroupComponent1.getName()).andReturn("component1").anyTimes();
    expect(hostGroupComponent2.getName()).andReturn("component2").anyTimes();
    expect(hostGroupComponent3.getName()).andReturn("component3").anyTimes();
    expect(hostGroup.getConfigurations()).andReturn(
        Collections.<HostGroupConfigEntity>singleton(hostGroupConfig)).anyTimes();

    expect(hostGroupConfig.getType()).andReturn("core-site").anyTimes();
    expect(hostGroupConfig.getConfigData()).andReturn(new Gson().toJson(mapGroupConfigProperties)).anyTimes();

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

    expect(serviceResourceProvider.installAndStart(clusterName)).andReturn(response);

    expect(configGroupResourceProvider.createResources(
        capture(configGroupRequestCapture))).andReturn(null);

    replay(blueprintDAO, managementController, request, response, blueprint, stackServiceResponse1, stackServiceResponse2,
        stackServiceComponentResponse1, stackServiceComponentResponse2, stackServiceComponentResponse3,
        stackConfigurationResponse1, stackConfigurationResponse2, stackConfigurationResponse3, stackConfigurationResponse4,stackConfigurationResponse5,
        blueprintConfig, hostGroup, hostGroupComponent1, hostGroupComponent2, hostGroupComponent3, hostGroupConfig,
        serviceResourceProvider, componentResourceProvider, hostResourceProvider, hostComponentResourceProvider,
        configGroupResourceProvider, metaInfo);

    // test
    ClusterResourceProvider.init(blueprintDAO, metaInfo, configHelper);
    ResourceProvider provider = new TestClusterResourceProvider(
        managementController, serviceResourceProvider, componentResourceProvider,
        hostResourceProvider, hostComponentResourceProvider, configGroupResourceProvider);

    try {
      provider.createResources(request);
      fail("Exception excepted");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  @Test
  public void testCreateResource_blueprint_attrbiutesProvided() throws Exception {
    String blueprintName = "test-blueprint";
    String stackName = "test";
    String stackVersion = "1.23";
    String clusterName = "c1";

    ConfigHelper configHelper = createNiceMock(ConfigHelper.class);
    BlueprintDAO blueprintDAO = createStrictMock(BlueprintDAO.class);
    AmbariMetaInfo metaInfo = createMock(AmbariMetaInfo.class);
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
    StackServiceComponentResponse stackServiceComponentResponse4 = createNiceMock(StackServiceComponentResponse.class);
    Capture<Set<StackServiceComponentRequest>> serviceComponentRequestCapture1 = new Capture<Set<StackServiceComponentRequest>>();
    Capture<Set<StackServiceComponentRequest>> serviceComponentRequestCapture2 = new Capture<Set<StackServiceComponentRequest>>();

    StackConfigurationResponse stackConfigurationResponse1 = createNiceMock(StackConfigurationResponse.class);
    StackConfigurationResponse stackConfigurationResponse2 = createNiceMock(StackConfigurationResponse.class);
    StackConfigurationResponse stackConfigurationResponse3 = createNiceMock(StackConfigurationResponse.class);
    StackConfigurationResponse stackConfigurationResponse4 = createNiceMock(StackConfigurationResponse.class);
    StackConfigurationResponse stackConfigurationResponse5 = createNiceMock(StackConfigurationResponse.class);
    StackConfigurationResponse stackConfigurationResponse6 = createNiceMock(StackConfigurationResponse.class);
    StackConfigurationResponse stackConfigurationResponse7 = createNiceMock(StackConfigurationResponse.class);
    StackConfigurationResponse stackConfigurationResponse8 = createNiceMock(StackConfigurationResponse.class);
    StackConfigurationResponse stackConfigurationResponse9 = createNiceMock(StackConfigurationResponse.class);
    Capture<Set<StackConfigurationRequest>> serviceConfigurationRequestCapture1 = new Capture<Set<StackConfigurationRequest>>();
    Capture<Set<StackConfigurationRequest>> serviceConfigurationRequestCapture2 = new Capture<Set<StackConfigurationRequest>>();
    Capture<Set<StackLevelConfigurationRequest>> serviceLevelConfigurationRequestCapture1 = new Capture<Set<StackLevelConfigurationRequest>>();

    Map<String, String> coreSiteAttributes = new HashMap<String, String>();
    coreSiteAttributes.put("final", "true");
    Map<String, String> hdfsSiteAttributes = new HashMap<String, String>();
    hdfsSiteAttributes.put("final", "true");

    BlueprintConfigEntity blueprintConfig = createNiceMock(BlueprintConfigEntity.class);
    BlueprintConfigEntity blueprintConfig2 = createNiceMock(BlueprintConfigEntity.class);

    HostGroupEntity hostGroup = createNiceMock(HostGroupEntity.class);
    HostGroupComponentEntity hostGroupComponent1 = createNiceMock(HostGroupComponentEntity.class);
    HostGroupComponentEntity hostGroupComponent2 = createNiceMock(HostGroupComponentEntity.class);
    HostGroupComponentEntity hostGroupComponent3 = createNiceMock(HostGroupComponentEntity.class);
    HostGroupComponentEntity hostGroupComponent4 = createNiceMock(HostGroupComponentEntity.class);

    HostGroupConfigEntity hostGroupConfig = createNiceMock(HostGroupConfigEntity.class);

    ServiceResourceProvider serviceResourceProvider = createStrictMock(ServiceResourceProvider.class);
    ResourceProvider componentResourceProvider = createStrictMock(ResourceProvider.class);
    ResourceProvider hostResourceProvider = createStrictMock(ResourceProvider.class);
    ResourceProvider hostComponentResourceProvider = createStrictMock(ResourceProvider.class);
    ConfigGroupResourceProvider configGroupResourceProvider = createStrictMock(ConfigGroupResourceProvider.class);

    Capture<ClusterRequest> createClusterRequestCapture = new Capture<ClusterRequest>();
    Capture<Set<ClusterRequest>> updateClusterRequestCapture = new Capture<Set<ClusterRequest>>();
    Capture<Map<String, String>> updateClusterPropertyMapCapture = new Capture<Map<String, String>>();
    Capture<Set<ClusterRequest>> updateClusterRequestCapture2 = new Capture<Set<ClusterRequest>>();
    Capture<Map<String, String>> updateClusterPropertyMapCapture2 = new Capture<Map<String, String>>();
    Capture<Set<ClusterRequest>> updateClusterRequestCapture3 = new Capture<Set<ClusterRequest>>();
    Capture<Map<String, String>> updateClusterPropertyMapCapture3 = new Capture<Map<String, String>>();
    Capture<Set<ClusterRequest>> persistUIStateRequestCapture = new Capture<Set<ClusterRequest>>();
    Capture<Map<String, String>> persistUIStatePropertyMapCapture = new Capture<Map<String, String>>();

    Capture<Request> serviceRequestCapture = new Capture<Request>();
    Capture<Request> componentRequestCapture = new Capture<Request>();
    Capture<Request> componentRequestCapture2 = new Capture<Request>();
    Capture<Request> hostRequestCapture = new Capture<Request>();
    Capture<Request> hostComponentRequestCapture = new Capture<Request>();
    Capture<Set<ConfigGroupRequest>> configGroupRequestCapture = new Capture<Set<ConfigGroupRequest>>();

    Set<StackServiceResponse> stackServiceResponses = new LinkedHashSet<StackServiceResponse>();
    stackServiceResponses.add(stackServiceResponse1);
    stackServiceResponses.add(stackServiceResponse2);

    // service1 has 3 components
    Set<StackServiceComponentResponse> stackServiceComponentResponses1 = new LinkedHashSet<StackServiceComponentResponse>();
    stackServiceComponentResponses1.add(stackServiceComponentResponse1);
    stackServiceComponentResponses1.add(stackServiceComponentResponse2);
    stackServiceComponentResponses1.add(stackServiceComponentResponse4);

    // service2 has 1 components
    Set<StackServiceComponentResponse> stackServiceComponentResponses2 = new LinkedHashSet<StackServiceComponentResponse>();
    stackServiceComponentResponses2.add(stackServiceComponentResponse3);

    // service1 has 2 config
    Set<StackConfigurationResponse> stackConfigurationResponses1 = new LinkedHashSet<StackConfigurationResponse>();
    stackConfigurationResponses1.add(stackConfigurationResponse1);
    stackConfigurationResponses1.add(stackConfigurationResponse5);

    // service2 has 3 config
    Set<StackConfigurationResponse> stackConfigurationResponses2 = new LinkedHashSet<StackConfigurationResponse>();
    stackConfigurationResponses2.add(stackConfigurationResponse2);
    stackConfigurationResponses2.add(stackConfigurationResponse3);
    stackConfigurationResponses2.add(stackConfigurationResponse4);
    stackConfigurationResponses2.add(stackConfigurationResponse6);
    stackConfigurationResponses2.add(stackConfigurationResponse7);
    stackConfigurationResponses2.add(stackConfigurationResponse8);
    
    Set<StackConfigurationResponse> stackConfigurationResponses3 = new LinkedHashSet<StackConfigurationResponse>();
    stackConfigurationResponses3.add(stackConfigurationResponse9);

    Collection<HostGroupComponentEntity> hostGroupComponents = new LinkedHashSet<HostGroupComponentEntity>();
    hostGroupComponents.add(hostGroupComponent1);
    hostGroupComponents.add(hostGroupComponent2);
    hostGroupComponents.add(hostGroupComponent3);
    hostGroupComponents.add(hostGroupComponent4);

    // request properties
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

    // blueprint core-site cluster configuration properties
    Map<String, Map<String, String>> blueprintCoreConfigAttributes = new HashMap<String, Map<String, String>>();
    Map<String, String> coreFinalAttributes = new HashMap<String, String>();
    coreFinalAttributes.put("property1", "false");
    coreFinalAttributes.put("new.property", "true");
    blueprintCoreConfigAttributes.put("final", coreFinalAttributes);

    Map<String, Map<String, String>> blueprintHiveEnvConfigAttributes = new HashMap<String, Map<String, String>>();
    Map<String, String> globalFinalAttributes = new HashMap<String, String>();
    globalFinalAttributes.put("hive_database", "true");
    blueprintHiveEnvConfigAttributes.put("final", globalFinalAttributes);

    Collection<BlueprintConfigEntity> configurations = new HashSet<BlueprintConfigEntity>();
    configurations.add(blueprintConfig);
    configurations.add(blueprintConfig2);

    // expectations
    expect(request.getProperties()).andReturn(propertySet).anyTimes();
    expect(blueprintDAO.findByName(blueprintName)).andReturn(blueprint);
    expect(blueprint.getStackName()).andReturn(stackName);
    expect(blueprint.getStackVersion()).andReturn(stackVersion);
    expect(blueprint.getConfigurations()).andReturn(configurations).times(3);
    expect(blueprint.validateConfigurations(metaInfo, true)).andReturn(
        Collections.<String, Map<String, Collection<String>>>emptyMap());

    expect(metaInfo.getComponentDependencies("test", "1.23", "service1", "component1")).
        andReturn(Collections.<DependencyInfo>emptyList());
    expect(metaInfo.getComponentDependencies("test", "1.23", "service1", "component2")).
        andReturn(Collections.<DependencyInfo>emptyList());
    expect(metaInfo.getComponentDependencies("test", "1.23", "service1", "MYSQL_SERVER")).
        andReturn(Collections.<DependencyInfo>emptyList());
    expect(metaInfo.getComponentDependencies("test", "1.23", "service2", "component3")).
        andReturn(Collections.<DependencyInfo>emptyList());

    expect(managementController.getStackServices(capture(stackServiceRequestCapture))).andReturn(stackServiceResponses);
    expect(stackServiceResponse1.getServiceName()).andReturn("service1");
    expect(stackServiceResponse2.getServiceName()).andReturn("service2");

    expect(managementController.getStackComponents(capture(serviceComponentRequestCapture1))).
        andReturn(stackServiceComponentResponses1);
    expect(stackServiceComponentResponse1.getComponentName()).andReturn("component1");
    expect(stackServiceComponentResponse2.getComponentName()).andReturn("component2");
    expect(stackServiceComponentResponse4.getComponentName()).andReturn("MYSQL_SERVER");

    expect(managementController.getStackConfigurations(capture(serviceConfigurationRequestCapture1))).
        andReturn(stackConfigurationResponses1);
    expect(managementController.getStackLevelConfigurations(capture(serviceLevelConfigurationRequestCapture1))).
        andReturn(stackConfigurationResponses3);

    expect(stackConfigurationResponse1.getType()).andReturn("core-site.xml");
    expect(stackConfigurationResponse1.getPropertyName()).andReturn("property1");
    expect(stackConfigurationResponse1.getPropertyValue()).andReturn("value1");
    expect(stackConfigurationResponse1.getPropertyAttributes()).andReturn(coreSiteAttributes);

    expect(managementController.getStackComponents(capture(serviceComponentRequestCapture2))).
        andReturn(stackServiceComponentResponses2);
    expect(stackServiceComponentResponse3.getComponentName()).andReturn("component3");

    expect(managementController.getStackConfigurations(capture(serviceConfigurationRequestCapture2))).
        andReturn(stackConfigurationResponses2);
    expect(managementController.getStackLevelConfigurations(capture(serviceLevelConfigurationRequestCapture1))).
        andReturn(stackConfigurationResponses3);
    expect(stackConfigurationResponse2.getType()).andReturn("hdfs-site.xml");
    expect(stackConfigurationResponse2.getPropertyName()).andReturn("property2");
    expect(stackConfigurationResponse2.getPropertyValue()).andReturn("value2");
    expect(stackConfigurationResponse2.getPropertyAttributes()).andReturn(hdfsSiteAttributes);

    expect(stackConfigurationResponse3.getType()).andReturn("oozie-env.xml");
    expect(stackConfigurationResponse3.getPropertyName()).andReturn("oozie_user");
    expect(stackConfigurationResponse3.getPropertyValue()).andReturn("oozie");

    expect(stackConfigurationResponse4.getType()).andReturn("core-site.xml");
    expect(stackConfigurationResponse4.getPropertyName()).andReturn("property3");
    expect(stackConfigurationResponse4.getPropertyValue()).andReturn("value3");

    expect(stackConfigurationResponse5.getType()).andReturn("hive-site.xml");
    expect(stackConfigurationResponse5.getPropertyName()).andReturn("javax.jdo.option.ConnectionURL");
    expect(stackConfigurationResponse5.getPropertyValue()).andReturn("localhost:12345");

    expect(stackConfigurationResponse6.getType()).andReturn("hbase-env.xml");
    expect(stackConfigurationResponse6.getPropertyName()).andReturn("property3");
    expect(stackConfigurationResponse6.getPropertyValue()).andReturn("value3");
   
    expect(stackConfigurationResponse7.getType()).andReturn("falcon-env.xml");
    expect(stackConfigurationResponse7.getPropertyName()).andReturn("property3");
    expect(stackConfigurationResponse7.getPropertyValue()).andReturn("value3");
    
    expect(stackConfigurationResponse8.getType()).andReturn("hive-env.xml");
    expect(stackConfigurationResponse8.getPropertyName()).andReturn("property3");
    expect(stackConfigurationResponse8.getPropertyValue()).andReturn("value3");
    
    expect(stackConfigurationResponse9.getType()).andReturn("cluster-env.xml").anyTimes();
    expect(stackConfigurationResponse9.getPropertyName()).andReturn("rqw").anyTimes();
    expect(stackConfigurationResponse9.getPropertyValue()).andReturn("aaaa").anyTimes();

    expect(blueprintConfig.getBlueprintName()).andReturn("test-blueprint").anyTimes();
    expect(blueprintConfig.getType()).andReturn("core-site").anyTimes();
    expect(blueprintConfig.getConfigData()).andReturn(new Gson().toJson(blueprintCoreConfigProperties)).anyTimes();
    expect(blueprintConfig.getConfigAttributes()).andReturn(new Gson().toJson(blueprintCoreConfigAttributes)).anyTimes();
    expect(blueprintConfig2.getBlueprintName()).andReturn("test-blueprint").anyTimes();
    expect(blueprintConfig2.getType()).andReturn("hive-env").anyTimes();
    expect(blueprintConfig2.getConfigData()).andReturn(new Gson().toJson(blueprintGlobalConfigProperties)).anyTimes();
    expect(blueprintConfig2.getConfigAttributes()).andReturn(new Gson().toJson(blueprintHiveEnvConfigAttributes)).anyTimes();

    expect(blueprint.getHostGroups()).andReturn(Collections.singleton(hostGroup)).anyTimes();
    expect(hostGroup.getName()).andReturn("group1").anyTimes();
    expect(hostGroup.getComponents()).andReturn(hostGroupComponents).anyTimes();
    expect(hostGroupComponent1.getName()).andReturn("component1").anyTimes();
    expect(hostGroupComponent2.getName()).andReturn("component2").anyTimes();
    expect(hostGroupComponent3.getName()).andReturn("component3").anyTimes();
    expect(hostGroupComponent4.getName()).andReturn("MYSQL_SERVER").anyTimes();
    expect(hostGroup.getConfigurations()).andReturn(
        Collections.<HostGroupConfigEntity>singleton(hostGroupConfig)).anyTimes();

    expect(hostGroupConfig.getType()).andReturn("core-site").anyTimes();
    expect(hostGroupConfig.getConfigData()).andReturn(new Gson().toJson(mapGroupConfigProperties)).anyTimes();

    managementController.createCluster(capture(createClusterRequestCapture));
    expect(managementController.updateClusters(capture(updateClusterRequestCapture),
        capture(updateClusterPropertyMapCapture))).andReturn(null);
    expect(managementController.updateClusters(capture(updateClusterRequestCapture2),
        capture(updateClusterPropertyMapCapture2))).andReturn(null);
    expect(managementController.updateClusters(capture(updateClusterRequestCapture3),
        capture(updateClusterPropertyMapCapture3))).andReturn(null);

    // set state for UI
    expect(managementController.updateClusters(capture(persistUIStateRequestCapture),
        capture(persistUIStatePropertyMapCapture))).andReturn(null);

    expect(serviceResourceProvider.createResources(capture(serviceRequestCapture))).andReturn(null);
    expect(componentResourceProvider.createResources(capture(componentRequestCapture))).andReturn(null);
    expect(componentResourceProvider.createResources(capture(componentRequestCapture2))).andReturn(null);
    expect(hostResourceProvider.createResources(capture(hostRequestCapture))).andReturn(null);
    expect(hostComponentResourceProvider.createResources(capture(hostComponentRequestCapture))).andReturn(null);

    expect(serviceResourceProvider.installAndStart(clusterName)).andReturn(response);

    expect(configGroupResourceProvider.createResources(
        capture(configGroupRequestCapture))).andReturn(null);



    replay(blueprintDAO, managementController, request, response, blueprint, stackServiceResponse1, stackServiceResponse2,
        stackServiceComponentResponse1, stackServiceComponentResponse2, stackServiceComponentResponse3,
        stackServiceComponentResponse4, stackConfigurationResponse1, stackConfigurationResponse2,
        stackConfigurationResponse3, stackConfigurationResponse4, stackConfigurationResponse5, stackConfigurationResponse6, 
        stackConfigurationResponse7, stackConfigurationResponse8, stackConfigurationResponse9, blueprintConfig,
        blueprintConfig2, hostGroup, hostGroupComponent1, hostGroupComponent2, hostGroupComponent3, hostGroupComponent4,
        hostGroupConfig, serviceResourceProvider, componentResourceProvider, hostResourceProvider,
        hostComponentResourceProvider, configGroupResourceProvider, metaInfo);

    // test
    ClusterResourceProvider.init(blueprintDAO, metaInfo, configHelper);
    ResourceProvider provider = new TestClusterResourceProvider(
        managementController, serviceResourceProvider, componentResourceProvider,
        hostResourceProvider, hostComponentResourceProvider, configGroupResourceProvider) {
      @Override
      protected boolean isServiceIncluded(String serviceName, Map<String, HostGroupImpl> blueprintHostGroups) {
        return true;
      }
    };

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
    assertEquals(clusterName, clusterRequest.getClusterName());
    assertEquals("test-1.23", clusterRequest.getStackVersion());

    Set<ClusterRequest> updateClusterRequest1 = updateClusterRequestCapture.getValue();
    Set<ClusterRequest> updateClusterRequest2 = updateClusterRequestCapture2.getValue();
    Set<ClusterRequest> updateClusterRequest3 = updateClusterRequestCapture3.getValue();
    Set<ClusterRequest> persistUIStateRequest = persistUIStateRequestCapture.getValue();

    assertEquals(1, updateClusterRequest1.size());
    assertEquals(1, updateClusterRequest2.size());
    assertEquals(1, updateClusterRequest3.size());

    assertEquals(1, persistUIStateRequest.size());

    ClusterRequest ucr1 = updateClusterRequest1.iterator().next();
    ClusterRequest ucr2 = updateClusterRequest2.iterator().next();
    ClusterRequest ucr3 = updateClusterRequest3.iterator().next();

    ClusterRequest ucr9 = persistUIStateRequest.iterator().next();
    assertEquals(clusterName, ucr1.getClusterName());
    assertEquals(clusterName, ucr2.getClusterName());
    assertEquals(clusterName, ucr3.getClusterName());
    assertEquals("INSTALLED", ucr9.getProvisioningState());
    assertEquals(clusterName, ucr9.getClusterName());

    ClusterRequest[] arrayOfClusterRequests =
      { ucr1, ucr2, ucr3 };

    // assert that all ConfigRequests instances have version of "1"
    for (ClusterRequest testRequest : arrayOfClusterRequests) {
      for (ConfigurationRequest testConfigRequest : testRequest.getDesiredConfig()) {
        assertEquals("1", testConfigRequest.getVersionTag());
      }
    }

    Map<String, ConfigurationRequest> mapConfigRequests = new HashMap<String, ConfigurationRequest>();
    // add all the associated config types to the test map
    // with the config versioning change, there could be
    // more than one config type associated with a ClusterRequest
    for (ClusterRequest testClusterRequest : arrayOfClusterRequests) {
      for (ConfigurationRequest configRequest : testClusterRequest.getDesiredConfig()) {
        mapConfigRequests.put(configRequest.getType(), configRequest);
      }
    }


    assertEquals(8, mapConfigRequests.size());
    ConfigurationRequest hiveEnvConfigRequest = mapConfigRequests.get("hive-env");
    assertEquals("New MySQL Database", hiveEnvConfigRequest.getProperties().get("hive_database"));
    assertNotNull(hiveEnvConfigRequest.getPropertiesAttributes());
    assertEquals(1, hiveEnvConfigRequest.getPropertiesAttributes().size());
    Map<String, String> hiveEnvFinalAttrs = hiveEnvConfigRequest.getPropertiesAttributes().get("final");
    assertEquals(1, hiveEnvFinalAttrs.size());
    assertEquals("true", hiveEnvFinalAttrs.get("hive_database"));
    ConfigurationRequest hdfsConfigRequest = mapConfigRequests.get("hdfs-site");
    assertEquals(1, hdfsConfigRequest.getProperties().size());
    assertEquals("value2", hdfsConfigRequest.getProperties().get("property2"));
    Map<String, Map<String, String>> hdfsAttributes = hdfsConfigRequest.getPropertiesAttributes();
    assertTrue(hdfsAttributes.containsKey("final"));
    assertEquals(1, hdfsAttributes.get("final").size());
    assertEquals("true", hdfsAttributes.get("final").get("property2"));
    ConfigurationRequest coreConfigRequest = mapConfigRequests.get("core-site");
    assertEquals(5, coreConfigRequest.getProperties().size());
    assertEquals("value2", coreConfigRequest.getProperties().get("property1"));
    assertEquals("value3", coreConfigRequest.getProperties().get("property3"));
    assertEquals("*", coreConfigRequest.getProperties().get("hadoop.proxyuser.oozie.hosts"));
    assertEquals("users", coreConfigRequest.getProperties().get("hadoop.proxyuser.oozie.groups"));
    assertEquals("new.property.value", coreConfigRequest.getProperties().get("new.property"));
    assertNotNull(coreConfigRequest.getPropertiesAttributes());
    assertEquals(1, coreConfigRequest.getPropertiesAttributes().size());
    Map<String, String> coreFinalAttrs = coreConfigRequest.getPropertiesAttributes().get("final");
    assertEquals(2, coreFinalAttrs.size());
    assertEquals("false", coreFinalAttrs.get("property1"));
    assertEquals("true", coreFinalAttrs.get("new.property"));
    ConfigurationRequest hiveConfigRequest = mapConfigRequests.get("hive-site");
    assertEquals(1, hiveConfigRequest.getProperties().size());
    assertEquals("host.domain:12345", hiveConfigRequest.getProperties().get("javax.jdo.option.ConnectionURL"));
    assertNotNull(hiveConfigRequest.getPropertiesAttributes());
    assertEquals(0, hiveConfigRequest.getPropertiesAttributes().size());

    assertNull(updateClusterPropertyMapCapture.getValue());
    assertNull(updateClusterPropertyMapCapture2.getValue());
    assertNull(updateClusterPropertyMapCapture3.getValue());
    assertNull(persistUIStatePropertyMapCapture.getValue());

    Request serviceRequest = serviceRequestCapture.getValue();
    assertEquals(2, serviceRequest.getProperties().size());
    Request componentRequest = componentRequestCapture.getValue();
    Request componentRequest2 = componentRequestCapture2.getValue();
    assertEquals(3, componentRequest.getProperties().size());
    Set<String> componentRequest1Names = new HashSet<String>();
    for (Map<String, Object> componentRequest1Properties : componentRequest.getProperties()) {
      assertEquals(3, componentRequest1Properties.size());
      assertEquals(clusterName, componentRequest1Properties.get("ServiceComponentInfo/cluster_name"));
      assertEquals("service1", componentRequest1Properties.get("ServiceComponentInfo/service_name"));
      componentRequest1Names.add((String) componentRequest1Properties.get("ServiceComponentInfo/component_name"));
    }
    assertTrue(componentRequest1Names.contains("component1") && componentRequest1Names.contains("component2")
        && componentRequest1Names.contains("MYSQL_SERVER"));
    assertEquals(1, componentRequest2.getProperties().size());
    Map<String, Object> componentRequest2Properties = componentRequest2.getProperties().iterator().next();
    assertEquals(clusterName, componentRequest2Properties.get("ServiceComponentInfo/cluster_name"));
    assertEquals("service2", componentRequest2Properties.get("ServiceComponentInfo/service_name"));
    assertEquals("component3", componentRequest2Properties.get("ServiceComponentInfo/component_name"));
    Request hostRequest = hostRequestCapture.getValue();
    assertEquals(1, hostRequest.getProperties().size());
    assertEquals(clusterName, hostRequest.getProperties().iterator().next().get("Hosts/cluster_name"));
    assertEquals("host.domain", hostRequest.getProperties().iterator().next().get("Hosts/host_name"));
    Request hostComponentRequest = hostComponentRequestCapture.getValue();
    assertEquals(4, hostComponentRequest.getProperties().size());
    Set<String> componentNames = new HashSet<String>();
    for (Map<String, Object> hostComponentProperties : hostComponentRequest.getProperties()) {
      assertEquals(3, hostComponentProperties.size());
      assertEquals(clusterName, hostComponentProperties.get("HostRoles/cluster_name"));
      assertEquals("host.domain", hostComponentProperties.get("HostRoles/host_name"));
      componentNames.add((String) hostComponentProperties.get("HostRoles/component_name"));
    }
    assertTrue(componentNames.contains("component1") && componentNames.contains("component2") &&
        componentNames.contains("component3") && componentNames.contains("MYSQL_SERVER"));

    Set<ConfigGroupRequest> configGroupRequests = configGroupRequestCapture.getValue();
    assertEquals(1, configGroupRequests.size());
    ConfigGroupRequest configGroupRequest = configGroupRequests.iterator().next();
    assertEquals(clusterName, configGroupRequest.getClusterName());
    assertEquals("group1", configGroupRequest.getGroupName());
    assertEquals("service1", configGroupRequest.getTag());
    assertEquals("Host Group Configuration", configGroupRequest.getDescription());
    Set<String> hosts = configGroupRequest.getHosts();
    assertEquals(1, hosts.size());
    assertEquals("host.domain", hosts.iterator().next());
    assertEquals(1, configGroupRequest.getConfigs().size());

    verify(blueprintDAO, managementController, request, response, blueprint, stackServiceResponse1, stackServiceResponse2,
        stackServiceComponentResponse1, stackServiceComponentResponse2, stackServiceComponentResponse3,
        stackServiceComponentResponse4, stackConfigurationResponse1, stackConfigurationResponse2,
        stackConfigurationResponse3, stackConfigurationResponse4, stackConfigurationResponse5, blueprintConfig,
        blueprintConfig2, hostGroup, hostGroupComponent1, hostGroupComponent2, hostGroupComponent3, hostGroupComponent4,
        hostGroupConfig, serviceResourceProvider, componentResourceProvider, hostResourceProvider,
        hostComponentResourceProvider, configGroupResourceProvider, metaInfo);
  }


  @Test
  public void testGetResources() throws Exception{
    Resource.Type type = Resource.Type.Cluster;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createMock(Clusters.class);

    Set<ClusterResponse> allResponse = new HashSet<ClusterResponse>();
    allResponse.add(new ClusterResponse(100L, "Cluster100", State.INSTALLED, null, null, null, null));
    allResponse.add(new ClusterResponse(101L, "Cluster101", State.INSTALLED, null, null, null, null));
    allResponse.add(new ClusterResponse(102L, "Cluster102", State.INSTALLED, null, null, null, null));
    allResponse.add(new ClusterResponse(103L, "Cluster103", State.INSTALLED, null, null, null, null));
    allResponse.add(new ClusterResponse(104L, "Cluster104", State.INSTALLED, null, null, null, null));

    Set<ClusterResponse> nameResponse = new HashSet<ClusterResponse>();
    nameResponse.add(new ClusterResponse(102L, "Cluster102", State.INSTALLED, null, null, null, null));

    Set<ClusterResponse> idResponse = new HashSet<ClusterResponse>();
    idResponse.add(new ClusterResponse(103L, "Cluster103", State.INSTALLED, null, null, null, null));

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
    nameResponse.add(new ClusterResponse(102L, "Cluster102", State.INIT, null, null, null, null));

    Map<String, String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "Called from a test");

    // set expectations
    expect(managementController.getClusters(EasyMock.<Set<ClusterRequest>>anyObject())).andReturn(nameResponse).once();
    expect(managementController.updateClusters(
        AbstractResourceProviderTest.Matcher.getClusterRequestSet(102L, "Cluster102", State.INSTALLED.name(), "HDP-0.1", null), eq(mapRequestProps))).
        andReturn(response).once();
    
    expect(managementController.updateClusters(
        AbstractResourceProviderTest.Matcher.getClusterRequestSet(103L, null, null, "HDP-0.1", null), eq(mapRequestProps))).
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
    nameResponse.add(new ClusterResponse(100L, "Cluster100", State.INSTALLED, null, null, null, null));

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

  @Test
  public void testSetMissingConfigurationsOozieIncluded() throws Exception {
    EasyMockSupport mockSupport = new EasyMockSupport();

    AmbariManagementController mockMgmtController =
      mockSupport.createMock(AmbariManagementController.class);
    ResourceProvider mockServiceProvider =
      mockSupport.createMock(ResourceProvider.class);
    ResourceProvider mockComponentProvider =
      mockSupport.createMock(ResourceProvider.class);
    ResourceProvider mockHostProvider =
      mockSupport.createMock(ResourceProvider.class);
    ResourceProvider mockHostComponentProvider =
      mockSupport.createMock(ResourceProvider.class);
    ResourceProvider mockConfigGroupProvider =
      mockSupport.createMock(ResourceProvider.class);
    AmbariManagementController mockManagementController =
      mockSupport.createMock(AmbariManagementController.class);
    StackServiceResponse mockStackServiceResponseOne =
      mockSupport.createMock(StackServiceResponse.class);
    StackServiceComponentResponse mockStackComponentResponse =
      mockSupport.createMock(StackServiceComponentResponse.class);
    AmbariMetaInfo mockAmbariMetaInfo =
      mockSupport.createMock(AmbariMetaInfo.class);

    expect(mockStackComponentResponse.getComponentName()).andReturn("OOZIE_SERVER").atLeastOnce();
    expect(mockStackComponentResponse.getCardinality()).andReturn("1");
    expect(mockStackComponentResponse.getAutoDeploy()).andReturn(new AutoDeployInfo());


    expect(mockStackServiceResponseOne.getServiceName()).andReturn("OOZIE").atLeastOnce();
    expect(mockStackServiceResponseOne.getExcludedConfigTypes()).andReturn(Collections.<String>emptySet()).atLeastOnce();
    expect(mockManagementController.getStackServices(isA(Set.class))).andReturn(Collections.singleton(mockStackServiceResponseOne));
    expect(mockManagementController.getStackComponents(isA(Set.class))).andReturn(Collections.singleton(mockStackComponentResponse));
    expect(mockManagementController.getStackConfigurations(isA(Set.class))).andReturn(Collections.<StackConfigurationResponse>emptySet());
    expect(mockManagementController.getStackLevelConfigurations(isA(Set.class))).andReturn(Collections.<StackConfigurationResponse>emptySet());

    expect(mockAmbariMetaInfo.getComponentDependencies("HDP", "2.1", "OOZIE", "OOZIE_SERVER")).andReturn(Collections.<DependencyInfo>emptyList());

    mockSupport.replayAll();


    ClusterResourceProvider.init(null, mockAmbariMetaInfo, null);

    Stack stack =
      new Stack("HDP", "2.1", mockManagementController);

    ClusterResourceProvider clusterResourceProvider =
      new TestClusterResourceProvider(mockMgmtController, mockServiceProvider,
        mockComponentProvider, mockHostProvider, mockHostComponentProvider, mockConfigGroupProvider);


    HostGroupEntity hostGroup = new HostGroupEntity();
    hostGroup.setComponents(Collections.<HostGroupComponentEntity>emptyList());
    HostGroupConfigEntity configEntity = new HostGroupConfigEntity();
    configEntity.setConfigData("");

    hostGroup.setConfigurations(Collections.singletonList(configEntity));
    BaseBlueprintProcessor.HostGroupImpl hostGroupImpl =
      new BaseBlueprintProcessor.HostGroupImpl(hostGroup, stack, null);
    hostGroupImpl.addComponent("OOZIE_SERVER");

    // add empty map for core-site, to simulate this configuration entry
    clusterResourceProvider.getClusterConfigurations().put("core-site", new HashMap<String, String>());
    clusterResourceProvider.getClusterConfigurations().put("oozie-env", new HashMap<String, String>());
    clusterResourceProvider.getClusterConfigurations().get("oozie-env").put("oozie_user", "oozie");

    clusterResourceProvider.setMissingConfigurations(Collections.singletonMap("host_group_one", hostGroupImpl));

    Map<String, String> mapCoreSiteConfig =
      clusterResourceProvider.getClusterConfigurations().get("core-site");

    assertNotNull("core-site map was null.", mapCoreSiteConfig);
    assertEquals("Incorrect number of entries in the core-site config map",
                 2, mapCoreSiteConfig.size());
    assertEquals("Incorrect value for proxy hosts",
                 "*", mapCoreSiteConfig.get("hadoop.proxyuser.oozie.hosts"));
    assertEquals("Incorrect value for proxy hosts",
      "users", mapCoreSiteConfig.get("hadoop.proxyuser.oozie.groups"));

    mockSupport.verifyAll();
  }


  @Test
  public void testSetMissingConfigurationsFalconIncluded() throws Exception {
    EasyMockSupport mockSupport = new EasyMockSupport();

    AmbariManagementController mockMgmtController =
      mockSupport.createMock(AmbariManagementController.class);
    ResourceProvider mockServiceProvider =
      mockSupport.createMock(ResourceProvider.class);
    ResourceProvider mockComponentProvider =
      mockSupport.createMock(ResourceProvider.class);
    ResourceProvider mockHostProvider =
      mockSupport.createMock(ResourceProvider.class);
    ResourceProvider mockHostComponentProvider =
      mockSupport.createMock(ResourceProvider.class);
    ResourceProvider mockConfigGroupProvider =
      mockSupport.createMock(ResourceProvider.class);
    AmbariManagementController mockManagementController =
      mockSupport.createMock(AmbariManagementController.class);
    StackServiceResponse mockStackServiceResponseOne =
      mockSupport.createMock(StackServiceResponse.class);
    StackServiceComponentResponse mockStackComponentResponse =
      mockSupport.createMock(StackServiceComponentResponse.class);
    AmbariMetaInfo mockAmbariMetaInfo =
      mockSupport.createMock(AmbariMetaInfo.class);

    expect(mockStackComponentResponse.getComponentName()).andReturn("FALCON_SERVER").atLeastOnce();
    expect(mockStackComponentResponse.getCardinality()).andReturn("1");
    expect(mockStackComponentResponse.getAutoDeploy()).andReturn(new AutoDeployInfo());


    expect(mockStackServiceResponseOne.getServiceName()).andReturn("FALCON").atLeastOnce();
    expect(mockStackServiceResponseOne.getExcludedConfigTypes()).andReturn(Collections.<String>emptySet()).atLeastOnce();
    expect(mockManagementController.getStackServices(isA(Set.class))).andReturn(Collections.singleton(mockStackServiceResponseOne));
    expect(mockManagementController.getStackComponents(isA(Set.class))).andReturn(Collections.singleton(mockStackComponentResponse));
    expect(mockManagementController.getStackConfigurations(isA(Set.class))).andReturn(Collections.<StackConfigurationResponse>emptySet());
    expect(mockManagementController.getStackLevelConfigurations(isA(Set.class))).andReturn(Collections.<StackConfigurationResponse>emptySet());
    
    expect(mockAmbariMetaInfo.getComponentDependencies("HDP", "2.1", "FALCON", "FALCON_SERVER")).andReturn(Collections.<DependencyInfo>emptyList());

    mockSupport.replayAll();

    ClusterResourceProvider.init(null, mockAmbariMetaInfo, null);

    Stack stack =
      new Stack("HDP", "2.1", mockManagementController);

    ClusterResourceProvider clusterResourceProvider =
      new TestClusterResourceProvider(mockMgmtController, mockServiceProvider,
        mockComponentProvider, mockHostProvider, mockHostComponentProvider, mockConfigGroupProvider);

    HostGroupEntity hostGroup = new HostGroupEntity();
    hostGroup.setComponents(Collections.<HostGroupComponentEntity>emptyList());
    HostGroupConfigEntity configEntity = new HostGroupConfigEntity();
    configEntity.setConfigData("");

    hostGroup.setConfigurations(Collections.singletonList(configEntity));
    BaseBlueprintProcessor.HostGroupImpl hostGroupImpl =
      new BaseBlueprintProcessor.HostGroupImpl(hostGroup, stack, null);
    hostGroupImpl.addComponent("FALCON_SERVER");

    // add empty map for core-site, to simulate this configuration entry
    clusterResourceProvider.getClusterConfigurations().put("core-site", new HashMap<String, String>());
    clusterResourceProvider.getClusterConfigurations().put("falcon-env", new HashMap<String, String>());
    clusterResourceProvider.getClusterConfigurations().get("falcon-env").put("falcon_user", "falcon");

    clusterResourceProvider.setMissingConfigurations(Collections.singletonMap("host_group_one", hostGroupImpl));

    Map<String, String> mapCoreSiteConfig =
      clusterResourceProvider.getClusterConfigurations().get("core-site");

    assertNotNull("core-site map was null.", mapCoreSiteConfig);
    assertEquals("Incorrect number of entries in the core-site config map",
      2, mapCoreSiteConfig.size());
    assertEquals("Incorrect value for proxy hosts",
      "*", mapCoreSiteConfig.get("hadoop.proxyuser.falcon.hosts"));
    assertEquals("Incorrect value for proxy hosts",
      "users", mapCoreSiteConfig.get("hadoop.proxyuser.falcon.groups"));

    mockSupport.verifyAll();
  }


  @Test
  public void testSetMissingConfigurationsOozieNotIncluded() throws Exception {
    EasyMockSupport mockSupport = new EasyMockSupport();

    AmbariManagementController mockMgmtController =
      mockSupport.createMock(AmbariManagementController.class);
    ResourceProvider mockServiceProvider =
      mockSupport.createMock(ResourceProvider.class);
    ResourceProvider mockComponentProvider =
      mockSupport.createMock(ResourceProvider.class);
    ResourceProvider mockHostProvider =
      mockSupport.createMock(ResourceProvider.class);
    ResourceProvider mockHostComponentProvider =
      mockSupport.createMock(ResourceProvider.class);
    ResourceProvider mockConfigGroupProvider =
      mockSupport.createMock(ResourceProvider.class);
    AmbariManagementController mockManagementController =
      mockSupport.createMock(AmbariManagementController.class);
    StackServiceResponse mockStackServiceResponseOne =
      mockSupport.createMock(StackServiceResponse.class);
    StackServiceComponentResponse mockStackComponentResponse =
      mockSupport.createMock(StackServiceComponentResponse.class);
    AmbariMetaInfo mockAmbariMetaInfo =
      mockSupport.createMock(AmbariMetaInfo.class);

    expect(mockStackComponentResponse.getComponentName()).andReturn("OOZIE_SERVER");
    expect(mockStackComponentResponse.getCardinality()).andReturn("1");
    expect(mockStackComponentResponse.getAutoDeploy()).andReturn(new AutoDeployInfo());


    expect(mockStackServiceResponseOne.getServiceName()).andReturn("OOZIE").atLeastOnce();
    expect(mockStackServiceResponseOne.getExcludedConfigTypes()).andReturn(Collections.<String>emptySet()).atLeastOnce();
    expect(mockManagementController.getStackServices(isA(Set.class))).andReturn(Collections.singleton(mockStackServiceResponseOne));
    expect(mockManagementController.getStackComponents(isA(Set.class))).andReturn(Collections.singleton(mockStackComponentResponse));
    expect(mockManagementController.getStackConfigurations(isA(Set.class))).andReturn(Collections.<StackConfigurationResponse>emptySet());
    expect(mockManagementController.getStackLevelConfigurations(isA(Set.class))).andReturn(Collections.<StackConfigurationResponse>emptySet());
    
    expect(mockAmbariMetaInfo.getComponentDependencies("HDP", "2.1", "OOZIE", "OOZIE_SERVER")).andReturn(Collections.<DependencyInfo>emptyList());

    mockSupport.replayAll();

    ClusterResourceProvider.init(null, mockAmbariMetaInfo, null);

    Stack stack =
      new Stack("HDP", "2.1", mockManagementController);

    ClusterResourceProvider clusterResourceProvider =
      new TestClusterResourceProvider(mockMgmtController, mockServiceProvider,
        mockComponentProvider, mockHostProvider, mockHostComponentProvider, mockConfigGroupProvider);


    HostGroupEntity hostGroup = new HostGroupEntity();
    hostGroup.setComponents(Collections.<HostGroupComponentEntity>emptyList());
    HostGroupConfigEntity configEntity = new HostGroupConfigEntity();
    configEntity.setConfigData("");

    hostGroup.setConfigurations(Collections.singletonList(configEntity));
    BaseBlueprintProcessor.HostGroupImpl hostGroupImpl =
      new BaseBlueprintProcessor.HostGroupImpl(hostGroup, stack, null);
    hostGroupImpl.addComponent("COMPONENT_ONE");

    // add empty map for core-site, to simulate this configuration entry
    clusterResourceProvider.getClusterConfigurations().put("core-site", new HashMap<String, String>());

    clusterResourceProvider.setMissingConfigurations(Collections.singletonMap("host_group_one", hostGroupImpl));

    Map<String, String> mapCoreSiteConfig =
      clusterResourceProvider.getClusterConfigurations().get("core-site");

    assertNotNull("core-site map was null.", mapCoreSiteConfig);
    assertEquals("Incorrect number of entries in the core-site config map",
                0, mapCoreSiteConfig.size());

    mockSupport.verifyAll();

  }


  @Test
  public void testSetMissingConfigurationsFalconNotIncluded() throws Exception {
    EasyMockSupport mockSupport = new EasyMockSupport();

    AmbariManagementController mockMgmtController =
      mockSupport.createMock(AmbariManagementController.class);
    ResourceProvider mockServiceProvider =
      mockSupport.createMock(ResourceProvider.class);
    ResourceProvider mockComponentProvider =
      mockSupport.createMock(ResourceProvider.class);
    ResourceProvider mockHostProvider =
      mockSupport.createMock(ResourceProvider.class);
    ResourceProvider mockHostComponentProvider =
      mockSupport.createMock(ResourceProvider.class);
    ResourceProvider mockConfigGroupProvider =
      mockSupport.createMock(ResourceProvider.class);
    AmbariManagementController mockManagementController =
      mockSupport.createMock(AmbariManagementController.class);
    StackServiceResponse mockStackServiceResponseOne =
      mockSupport.createMock(StackServiceResponse.class);
    StackServiceComponentResponse mockStackComponentResponse =
      mockSupport.createMock(StackServiceComponentResponse.class);
    AmbariMetaInfo mockAmbariMetaInfo =
      mockSupport.createMock(AmbariMetaInfo.class);

    expect(mockStackComponentResponse.getComponentName()).andReturn("FALCON_SERVER");
    expect(mockStackComponentResponse.getCardinality()).andReturn("1");
    expect(mockStackComponentResponse.getAutoDeploy()).andReturn(new AutoDeployInfo());


    expect(mockStackServiceResponseOne.getServiceName()).andReturn("FALCON").atLeastOnce();
    expect(mockStackServiceResponseOne.getExcludedConfigTypes()).andReturn(Collections.<String>emptySet()).atLeastOnce();

    expect(mockManagementController.getStackServices(isA(Set.class))).andReturn(Collections.singleton(mockStackServiceResponseOne));
    expect(mockManagementController.getStackComponents(isA(Set.class))).andReturn(Collections.singleton(mockStackComponentResponse));
    expect(mockManagementController.getStackConfigurations(isA(Set.class))).andReturn(Collections.<StackConfigurationResponse>emptySet());
    expect(mockManagementController.getStackLevelConfigurations(isA(Set.class))).andReturn(Collections.<StackConfigurationResponse>emptySet());
    
    expect(mockAmbariMetaInfo.getComponentDependencies("HDP", "2.1", "FALCON", "FALCON_SERVER")).andReturn(Collections.<DependencyInfo>emptyList());

    mockSupport.replayAll();

    ClusterResourceProvider.init(null, mockAmbariMetaInfo, null);

    Stack stack =
      new Stack("HDP", "2.1", mockManagementController);

    ClusterResourceProvider clusterResourceProvider =
      new TestClusterResourceProvider(mockMgmtController, mockServiceProvider,
        mockComponentProvider, mockHostProvider, mockHostComponentProvider, mockConfigGroupProvider);

    HostGroupEntity hostGroup = new HostGroupEntity();
    hostGroup.setComponents(Collections.<HostGroupComponentEntity>emptyList());
    HostGroupConfigEntity configEntity = new HostGroupConfigEntity();
    configEntity.setConfigData("");

    hostGroup.setConfigurations(Collections.singletonList(configEntity));
    BaseBlueprintProcessor.HostGroupImpl hostGroupImpl =
      new BaseBlueprintProcessor.HostGroupImpl(hostGroup, stack, null);
    // blueprint request will not include a reference to FALCON_SERVER
    hostGroupImpl.addComponent("COMPONENT_ONE");

    // add empty map for core-site, to simulate this configuration entry
    clusterResourceProvider.getClusterConfigurations().put("core-site", new HashMap<String, String>());

    clusterResourceProvider.setMissingConfigurations(Collections.singletonMap("host_group_one", hostGroupImpl));

    Map<String, String> mapCoreSiteConfig =
      clusterResourceProvider.getClusterConfigurations().get("core-site");

    assertNotNull("core-site map was null.", mapCoreSiteConfig);
    assertEquals("Incorrect number of entries in the core-site config map",
      0, mapCoreSiteConfig.size());

    mockSupport.verifyAll();

  }


  @Test
  public void testSetMissingConfigurationsHiveNotIncluded() throws Exception {
    EasyMockSupport mockSupport = new EasyMockSupport();

    AmbariManagementController mockMgmtController =
      mockSupport.createMock(AmbariManagementController.class);
    ResourceProvider mockServiceProvider =
      mockSupport.createMock(ResourceProvider.class);
    ResourceProvider mockComponentProvider =
      mockSupport.createMock(ResourceProvider.class);
    ResourceProvider mockHostProvider =
      mockSupport.createMock(ResourceProvider.class);
    ResourceProvider mockHostComponentProvider =
      mockSupport.createMock(ResourceProvider.class);
    ResourceProvider mockConfigGroupProvider =
      mockSupport.createMock(ResourceProvider.class);
    AmbariManagementController mockManagementController =
      mockSupport.createMock(AmbariManagementController.class);
    StackServiceResponse mockStackServiceResponseOne =
      mockSupport.createMock(StackServiceResponse.class);
    StackServiceComponentResponse mockStackComponentResponse =
      mockSupport.createMock(StackServiceComponentResponse.class);
    AmbariMetaInfo mockAmbariMetaInfo =
      mockSupport.createMock(AmbariMetaInfo.class);

    expect(mockStackComponentResponse.getComponentName()).andReturn("HIVE_SERVER");
    expect(mockStackComponentResponse.getCardinality()).andReturn("1");
    expect(mockStackComponentResponse.getAutoDeploy()).andReturn(new AutoDeployInfo());

    expect(mockStackServiceResponseOne.getServiceName()).andReturn("HIVE").atLeastOnce();
    expect(mockStackServiceResponseOne.getExcludedConfigTypes()).andReturn(Collections.<String>emptySet()).atLeastOnce();
    expect(mockManagementController.getStackServices(isA(Set.class))).andReturn(Collections.singleton(mockStackServiceResponseOne));
    expect(mockManagementController.getStackComponents(isA(Set.class))).andReturn(Collections.singleton(mockStackComponentResponse));
    expect(mockManagementController.getStackConfigurations(isA(Set.class))).andReturn(Collections.<StackConfigurationResponse>emptySet());
    expect(mockManagementController.getStackLevelConfigurations(isA(Set.class))).andReturn(Collections.<StackConfigurationResponse>emptySet());
    
    expect(mockAmbariMetaInfo.getComponentDependencies("HDP", "2.1", "HIVE", "HIVE_SERVER")).andReturn(Collections.<DependencyInfo>emptyList());

    mockSupport.replayAll();

    ClusterResourceProvider.init(null, mockAmbariMetaInfo, null);

    Stack stack =
      new Stack("HDP", "2.1", mockManagementController);

    ClusterResourceProvider clusterResourceProvider =
      new TestClusterResourceProvider(mockMgmtController, mockServiceProvider,
        mockComponentProvider, mockHostProvider, mockHostComponentProvider, mockConfigGroupProvider);

    HostGroupEntity hostGroup = new HostGroupEntity();
    hostGroup.setComponents(Collections.<HostGroupComponentEntity>emptyList());
    HostGroupConfigEntity configEntity = new HostGroupConfigEntity();
    configEntity.setConfigData("");

    hostGroup.setConfigurations(Collections.singletonList(configEntity));
    BaseBlueprintProcessor.HostGroupImpl hostGroupImpl =
      new BaseBlueprintProcessor.HostGroupImpl(hostGroup, stack, null);
    // blueprint request will not include a reference to a HIVE component
    hostGroupImpl.addComponent("COMPONENT_ONE");

    // add empty map for core-site, to simulate this configuration entry
    clusterResourceProvider.getClusterConfigurations().put("core-site", new HashMap<String, String>());

    clusterResourceProvider.setMissingConfigurations(Collections.singletonMap("host_group_one", hostGroupImpl));

    Map<String, String> mapCoreSiteConfig =
      clusterResourceProvider.getClusterConfigurations().get("core-site");

    assertNotNull("core-site map was null.", mapCoreSiteConfig);
    assertEquals("Incorrect number of entries in the core-site config map",
      0, mapCoreSiteConfig.size());

    mockSupport.verifyAll();

  }


  @Test
  public void testSetMissingConfigurationsHBaseNotIncluded() throws Exception {
    EasyMockSupport mockSupport = new EasyMockSupport();

    AmbariManagementController mockMgmtController =
      mockSupport.createMock(AmbariManagementController.class);
    ResourceProvider mockServiceProvider =
      mockSupport.createMock(ResourceProvider.class);
    ResourceProvider mockComponentProvider =
      mockSupport.createMock(ResourceProvider.class);
    ResourceProvider mockHostProvider =
      mockSupport.createMock(ResourceProvider.class);
    ResourceProvider mockHostComponentProvider =
      mockSupport.createMock(ResourceProvider.class);
    ResourceProvider mockConfigGroupProvider =
      mockSupport.createMock(ResourceProvider.class);
    AmbariManagementController mockManagementController =
      mockSupport.createMock(AmbariManagementController.class);
    StackServiceResponse mockStackServiceResponseOne =
      mockSupport.createMock(StackServiceResponse.class);
    StackServiceComponentResponse mockStackComponentResponse =
      mockSupport.createMock(StackServiceComponentResponse.class);
    AmbariMetaInfo mockAmbariMetaInfo =
      mockSupport.createMock(AmbariMetaInfo.class);

    expect(mockStackComponentResponse.getComponentName()).andReturn("HBASE_SERVER");
    expect(mockStackComponentResponse.getCardinality()).andReturn("1");
    expect(mockStackComponentResponse.getAutoDeploy()).andReturn(new AutoDeployInfo());

    expect(mockStackServiceResponseOne.getServiceName()).andReturn("HBASE").atLeastOnce();
    expect(mockStackServiceResponseOne.getExcludedConfigTypes()).andReturn(Collections.<String>emptySet()).atLeastOnce();

    expect(mockManagementController.getStackServices(isA(Set.class))).andReturn(Collections.singleton(mockStackServiceResponseOne));
    expect(mockManagementController.getStackComponents(isA(Set.class))).andReturn(Collections.singleton(mockStackComponentResponse));
    expect(mockManagementController.getStackConfigurations(isA(Set.class))).andReturn(Collections.<StackConfigurationResponse>emptySet());
    expect(mockManagementController.getStackLevelConfigurations(isA(Set.class))).andReturn(Collections.<StackConfigurationResponse>emptySet());
    
    expect(mockAmbariMetaInfo.getComponentDependencies("HDP", "2.1", "HBASE", "HBASE_SERVER")).andReturn(Collections.<DependencyInfo>emptyList());

    mockSupport.replayAll();

    ClusterResourceProvider.init(null, mockAmbariMetaInfo, null);

    Stack stack =
      new Stack("HDP", "2.1", mockManagementController);

    ClusterResourceProvider clusterResourceProvider =
      new TestClusterResourceProvider(mockMgmtController, mockServiceProvider,
        mockComponentProvider, mockHostProvider, mockHostComponentProvider, mockConfigGroupProvider);

    HostGroupEntity hostGroup = new HostGroupEntity();
    hostGroup.setComponents(Collections.<HostGroupComponentEntity>emptyList());
    HostGroupConfigEntity configEntity = new HostGroupConfigEntity();
    configEntity.setConfigData("");

    hostGroup.setConfigurations(Collections.singletonList(configEntity));
    BaseBlueprintProcessor.HostGroupImpl hostGroupImpl =
      new BaseBlueprintProcessor.HostGroupImpl(hostGroup, stack, null);
    // blueprint request will not include a reference to an HBASE component
    hostGroupImpl.addComponent("COMPONENT_ONE");

    // add empty map for core-site, to simulate this configuration entry
    clusterResourceProvider.getClusterConfigurations().put("core-site", new HashMap<String, String>());

    clusterResourceProvider.setMissingConfigurations(Collections.singletonMap("host_group_one", hostGroupImpl));

    Map<String, String> mapCoreSiteConfig =
      clusterResourceProvider.getClusterConfigurations().get("core-site");

    assertNotNull("core-site map was null.", mapCoreSiteConfig);
    assertEquals("Incorrect number of entries in the core-site config map",
      0, mapCoreSiteConfig.size());

    mockSupport.verifyAll();

  }


  private class TestClusterResourceProvider extends ClusterResourceProvider {

    private ResourceProvider serviceResourceProvider;
    private ResourceProvider componentResourceProvider;
    private ResourceProvider hostResourceProvider;
    private ResourceProvider hostComponentResourceProvider;
    private ResourceProvider configGroupResourceProvider;

    TestClusterResourceProvider(AmbariManagementController managementController,
                                ResourceProvider serviceResourceProvider,
                                ResourceProvider componentResourceProvider,
                                ResourceProvider hostResourceProvider,
                                ResourceProvider hostComponentResourceProvider,
                                ResourceProvider configGroupResourceProvider) {

      super(PropertyHelper.getPropertyIds(Resource.Type.Cluster),
            PropertyHelper.getKeyPropertyIds(Resource.Type.Cluster),
            managementController);

      this.serviceResourceProvider = serviceResourceProvider;
      this.componentResourceProvider = componentResourceProvider;
      this.hostResourceProvider = hostResourceProvider;
      this.hostComponentResourceProvider = hostComponentResourceProvider;
      this.configGroupResourceProvider = configGroupResourceProvider;
    }

    @Override
    ResourceProvider getResourceProvider(Resource.Type type) {
      if (type == Resource.Type.Service) {
        return this.serviceResourceProvider;
      } else if (type == Resource.Type.Component) {
        return this.componentResourceProvider;
      } else if (type == Resource.Type.Host) {
        return this.hostResourceProvider;
      } else if (type == Resource.Type.HostComponent) {
        return this.hostComponentResourceProvider;
      } else if (type == Resource.Type.ConfigGroup) {
        return this.configGroupResourceProvider;
      } else {
        fail("Unexpected resource provider type requested");
      }
      return null;
    }
  }
}
