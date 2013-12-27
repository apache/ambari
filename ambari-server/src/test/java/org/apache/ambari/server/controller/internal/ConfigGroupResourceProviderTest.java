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
import org.apache.ambari.server.controller.ConfigGroupResponse;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.configgroup.ConfigGroup;
import org.apache.ambari.server.state.configgroup.ConfigGroupFactory;
import org.easymock.Capture;
import org.easymock.IAnswer;
import org.junit.Assert;
import org.junit.Test;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

public class ConfigGroupResourceProviderTest {

  ConfigGroupResourceProvider getConfigGroupResourceProvider
    (AmbariManagementController managementController) {

    Resource.Type type = Resource.Type.ConfigGroup;

    return (ConfigGroupResourceProvider) AbstractControllerResourceProvider.getResourceProvider(
      type,
      PropertyHelper.getPropertyIds(type),
      PropertyHelper.getKeyPropertyIds(type),
      managementController);
  }

  @Test
  public void testCreateConfigGroup() throws Exception {
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Host h1 = createNiceMock(Host.class);
    Host h2 = createNiceMock(Host.class);
    ConfigGroupFactory configGroupFactory = createNiceMock(ConfigGroupFactory.class);
    ConfigGroup configGroup = createNiceMock(ConfigGroup.class);

    expect(managementController.getClusters()).andReturn(clusters);
    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();
    expect(clusters.getHost("h1")).andReturn(h1);
    expect(clusters.getHost("h2")).andReturn(h2);
    expect(managementController.getConfigGroupFactory()).andReturn(configGroupFactory);
    expect(managementController.getAuthName()).andReturn("admin").anyTimes();

    Capture<Cluster> clusterCapture = new Capture<Cluster>();
    Capture<String> captureName = new Capture<String>();
    Capture<String> captureDesc = new Capture<String>();
    Capture<String> captureTag = new Capture<String>();
    Capture<Map<String, Config>> captureConfigs = new Capture<Map<String,
      Config>>();
    Capture<Map<String, Host>> captureHosts = new Capture<Map<String, Host>>();

    expect(configGroupFactory.createNew(capture(clusterCapture),
      capture(captureName), capture(captureTag), capture(captureDesc),
      capture(captureConfigs), capture(captureHosts))).andReturn(configGroup);

    replay(managementController, clusters, cluster, configGroupFactory,
      configGroup, response);

    ResourceProvider provider = getConfigGroupResourceProvider
      (managementController);

    Set<Map<String, Object>> propertySet = new LinkedHashSet<Map<String, Object>>();
    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    Set<Map<String, Object>> hostSet = new HashSet<Map<String, Object>>();
    Map<String, Object> host1 = new HashMap<String, Object>();
    host1.put(ConfigGroupResourceProvider.CONFIGGROUP_HOSTNAME_PROPERTY_ID, "h1");
    hostSet.add(host1);
    Map<String, Object> host2 = new HashMap<String, Object>();
    host2.put(ConfigGroupResourceProvider.CONFIGGROUP_HOSTNAME_PROPERTY_ID, "h2");
    hostSet.add(host2);

    Set<Map<String, Object>> configSet = new HashSet<Map<String, Object>>();
    Map<String, String> configMap = new HashMap<String, String>();
    Map<String, Object> configs = new HashMap<String, Object>();
    configs.put("type", "core-site");
    configs.put("tag", "version100");
    configMap.put("key1", "value1");
    configs.put("properties", configMap);
    configSet.add(configs);

    properties.put(ConfigGroupResourceProvider
      .CONFIGGROUP_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    properties.put(ConfigGroupResourceProvider.CONFIGGROUP_NAME_PROPERTY_ID,
      "test-1");
    properties.put(ConfigGroupResourceProvider.CONFIGGROUP_TAG_PROPERTY_ID,
      "tag-1");
    properties.put(ConfigGroupResourceProvider.CONFIGGROUP_HOSTS_PROPERTY_ID,
      hostSet);
    properties.put(ConfigGroupResourceProvider.CONFIGGROUP_CONFIGS_PROPERTY_ID,
      configSet);

    propertySet.add(properties);

    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    provider.createResources(request);

    verify(managementController, clusters, cluster, configGroupFactory,
      configGroup, response);

    assertEquals("version100", captureConfigs.getValue().get("core-site")
      .getVersionTag());
    assertTrue(captureHosts.getValue().containsKey("h1"));
    assertTrue(captureHosts.getValue().containsKey("h2"));
  }

  @Test
  public void testDuplicateNameConfigGroup() throws Exception {
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    ConfigGroupFactory configGroupFactory = createNiceMock(ConfigGroupFactory.class);
    ConfigGroup configGroup = createNiceMock(ConfigGroup.class);
    Map<Long, ConfigGroup> configGroupMap = new HashMap<Long, ConfigGroup>();
    configGroupMap.put(1L, configGroup);

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();
    expect(managementController.getConfigGroupFactory()).andReturn
      (configGroupFactory).anyTimes();
    expect(managementController.getAuthName()).andReturn("admin").anyTimes();
    expect(cluster.getConfigGroups()).andReturn(configGroupMap);

    expect(configGroupFactory.createNew((Cluster) anyObject(), (String) anyObject(),
      (String) anyObject(), (String) anyObject(), (HashMap) anyObject(),
      (HashMap) anyObject())).andReturn(configGroup).anyTimes();

    expect(configGroup.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(configGroup.getName()).andReturn("test-1").anyTimes();
    expect(configGroup.getTag()).andReturn("tag-1").anyTimes();

    replay(managementController, clusters, cluster, configGroupFactory,
      configGroup, response);

    ResourceProvider provider = getConfigGroupResourceProvider
      (managementController);

    Map<String, Object> properties = new LinkedHashMap<String, Object>();
    Set<Map<String, Object>> propertySet = new LinkedHashSet<Map<String, Object>>();

    properties.put(ConfigGroupResourceProvider
      .CONFIGGROUP_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    properties.put(ConfigGroupResourceProvider.CONFIGGROUP_NAME_PROPERTY_ID,
      "test-1");
    properties.put(ConfigGroupResourceProvider.CONFIGGROUP_TAG_PROPERTY_ID,
      "tag-1");

    propertySet.add(properties);
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    Exception exception = null;
    try {
      provider.createResources(request);
    } catch (Exception e) {
      exception = e;
    }

    verify(managementController, clusters, cluster, configGroupFactory,
      configGroup, response);

    assertNotNull(exception);
    assertTrue(exception instanceof ResourceAlreadyExistsException);
  }

  @Test
  public void testUpdateConfigGroup() throws Exception {
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Host h1 = createNiceMock(Host.class);
    Host h2 = createNiceMock(Host.class);
    final ConfigGroup configGroup = createNiceMock(ConfigGroup.class);
    ConfigGroupResponse configGroupResponse = createNiceMock
      (ConfigGroupResponse.class);

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAuthName()).andReturn("admin").anyTimes();
    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();
    expect(clusters.getHost("h1")).andReturn(h1);
    expect(clusters.getHost("h2")).andReturn(h2);

    expect(configGroup.getName()).andReturn("test-1").anyTimes();
    expect(configGroup.getId()).andReturn(25L).anyTimes();
    expect(configGroup.getTag()).andReturn("tag-1").anyTimes();

    expect(configGroup.convertToResponse()).andReturn(configGroupResponse).anyTimes();
    expect(configGroupResponse.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(configGroupResponse.getId()).andReturn(25L).anyTimes();

    expect(cluster.getConfigGroups()).andStubAnswer(new IAnswer<Map<Long, ConfigGroup>>() {
      @Override
      public Map<Long, ConfigGroup> answer() throws Throwable {
        Map<Long, ConfigGroup> configGroupMap = new HashMap<Long, ConfigGroup>();
        configGroupMap.put(configGroup.getId(), configGroup);
        return configGroupMap;
      }
    });

    replay(managementController, clusters, cluster,
      configGroup, response, configGroupResponse);

    ResourceProvider provider = getConfigGroupResourceProvider
      (managementController);

    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    Set<Map<String, Object>> hostSet = new HashSet<Map<String, Object>>();
    Map<String, Object> host1 = new HashMap<String, Object>();
    host1.put(ConfigGroupResourceProvider.CONFIGGROUP_HOSTNAME_PROPERTY_ID, "h1");
    hostSet.add(host1);
    Map<String, Object> host2 = new HashMap<String, Object>();
    host2.put(ConfigGroupResourceProvider.CONFIGGROUP_HOSTNAME_PROPERTY_ID, "h2");
    hostSet.add(host2);

    Set<Map<String, Object>> configSet = new HashSet<Map<String, Object>>();
    Map<String, String> configMap = new HashMap<String, String>();
    Map<String, Object> configs = new HashMap<String, Object>();
    configs.put("type", "core-site");
    configs.put("tag", "version100");
    configMap.put("key1", "value1");
    configs.put("properties", configMap);
    configSet.add(configs);

    properties.put(ConfigGroupResourceProvider
      .CONFIGGROUP_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    properties.put(ConfigGroupResourceProvider.CONFIGGROUP_NAME_PROPERTY_ID,
      "test-1");
    properties.put(ConfigGroupResourceProvider.CONFIGGROUP_TAG_PROPERTY_ID,
      "tag-1");
    properties.put(ConfigGroupResourceProvider.CONFIGGROUP_HOSTS_PROPERTY_ID,
      hostSet );
    properties.put(ConfigGroupResourceProvider.CONFIGGROUP_CONFIGS_PROPERTY_ID,
      configSet);

    Map<String, String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "Called from a test");

    Request request = PropertyHelper.getUpdateRequest(properties, mapRequestProps);

    Predicate predicate = new PredicateBuilder().property
      (ConfigGroupResourceProvider.CONFIGGROUP_CLUSTER_NAME_PROPERTY_ID).equals
      ("Cluster100").and().
      property(ConfigGroupResourceProvider.CONFIGGROUP_ID_PROPERTY_ID).equals
      (25L).toPredicate();

    provider.updateResources(request, predicate);

    verify(managementController, clusters, cluster,
      configGroup, response, configGroupResponse);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testGetConfigGroup() throws Exception {
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Host h1 = createNiceMock(Host.class);

    ConfigGroup configGroup1 = createNiceMock(ConfigGroup.class);
    ConfigGroup configGroup2 = createNiceMock(ConfigGroup.class);
    ConfigGroup configGroup3 = createNiceMock(ConfigGroup.class);
    ConfigGroup configGroup4 = createNiceMock(ConfigGroup.class);
    ConfigGroupResponse response1 = createNiceMock(ConfigGroupResponse.class);
    ConfigGroupResponse response2 = createNiceMock(ConfigGroupResponse.class);
    ConfigGroupResponse response3 = createNiceMock(ConfigGroupResponse.class);
    ConfigGroupResponse response4 = createNiceMock(ConfigGroupResponse.class);

    Map<Long, ConfigGroup> configGroupMap = new HashMap<Long, ConfigGroup>();
    configGroupMap.put(1L, configGroup1);
    configGroupMap.put(2L, configGroup2);
    configGroupMap.put(3L, configGroup3);
    configGroupMap.put(4L, configGroup4);

    Map<Long, ConfigGroup> configGroupByHostname = new HashMap<Long, ConfigGroup>();
    configGroupByHostname.put(4L, configGroup4);

    expect(configGroup1.convertToResponse()).andReturn(response1).anyTimes();
    expect(configGroup2.convertToResponse()).andReturn(response2).anyTimes();
    expect(configGroup3.convertToResponse()).andReturn(response3).anyTimes();
    expect(configGroup4.convertToResponse()).andReturn(response4).anyTimes();

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();
    expect(cluster.getConfigGroups()).andReturn(configGroupMap).anyTimes();

    expect(configGroup1.getName()).andReturn("g1").anyTimes();
    expect(configGroup2.getName()).andReturn("g2").anyTimes();
    expect(configGroup3.getName()).andReturn("g3").anyTimes();
    expect(configGroup4.getName()).andReturn("g4").anyTimes();
    expect(configGroup1.getTag()).andReturn("t1").anyTimes();
    expect(configGroup2.getTag()).andReturn("t2").anyTimes();
    expect(configGroup3.getTag()).andReturn("t3").anyTimes();
    expect(configGroup4.getTag()).andReturn("t4").anyTimes();

    Map<String, Host> hostMap = new HashMap<String, Host>();
    hostMap.put("h1", h1);
    expect(configGroup4.getHosts()).andReturn(hostMap).anyTimes();


    expect(response1.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(response2.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(response3.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(response4.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(response1.getId()).andReturn(1L).anyTimes();
    expect(response2.getId()).andReturn(2L).anyTimes();
    expect(response3.getId()).andReturn(3L).anyTimes();
    expect(response4.getId()).andReturn(4L).anyTimes();
    expect(response2.getGroupName()).andReturn("g2").anyTimes();
    expect(response3.getTag()).andReturn("t3").anyTimes();
    expect(cluster.getConfigGroupsByHostname("h1")).andReturn(configGroupByHostname).anyTimes();

    Set<Map<String, Object>> hostObj = new HashSet<Map<String, Object>>();
    Map<String, Object> hostnames = new HashMap<String, Object>();
    hostnames.put("host_name", "h1");
    hostObj.add(hostnames);
    expect(response4.getHosts()).andReturn(hostObj).anyTimes();

    replay(managementController, clusters, cluster, configGroup1,
      configGroup2, configGroup3, configGroup4, response1, response2,
      response3, response4);

    ResourceProvider resourceProvider = getConfigGroupResourceProvider
      (managementController);

    Set<String> propertyIds = new HashSet<String>();

    propertyIds.add(ConfigGroupResourceProvider.CONFIGGROUP_CLUSTER_NAME_PROPERTY_ID);
    propertyIds.add(ConfigGroupResourceProvider.CONFIGGROUP_ID_PROPERTY_ID);

    // Read all
    Predicate predicate = new PredicateBuilder().property
      (ConfigGroupResourceProvider.CONFIGGROUP_CLUSTER_NAME_PROPERTY_ID)
      .equals("Cluster100").toPredicate();
    Request request = PropertyHelper.getReadRequest(propertyIds);

    Set<Resource> resources = resourceProvider.getResources(request,
      predicate);

    assertEquals(4, resources.size());

    // Read by id
    predicate = new PredicateBuilder().property(ConfigGroupResourceProvider
      .CONFIGGROUP_ID_PROPERTY_ID).equals(1L).and().property
      (ConfigGroupResourceProvider.CONFIGGROUP_CLUSTER_NAME_PROPERTY_ID)
      .equals("Cluster100").toPredicate();

    resources = resourceProvider.getResources(request, predicate);

    assertEquals(1, resources.size());
    assertEquals(1L, resources.iterator().next().getPropertyValue
      (ConfigGroupResourceProvider.CONFIGGROUP_ID_PROPERTY_ID));

    // Read by Name
    predicate = new PredicateBuilder().property(ConfigGroupResourceProvider
      .CONFIGGROUP_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").and()
      .property(ConfigGroupResourceProvider.CONFIGGROUP_NAME_PROPERTY_ID)
      .equals("g2").toPredicate();

    resources = resourceProvider.getResources(request, predicate);

    assertEquals(1, resources.size());
    assertEquals("g2", resources.iterator().next().getPropertyValue
      (ConfigGroupResourceProvider.CONFIGGROUP_NAME_PROPERTY_ID));

    // Read by tag
    predicate = new PredicateBuilder().property(ConfigGroupResourceProvider
      .CONFIGGROUP_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").and()
      .property(ConfigGroupResourceProvider.CONFIGGROUP_TAG_PROPERTY_ID)
      .equals("t3").toPredicate();

    resources = resourceProvider.getResources(request, predicate);

    assertEquals(1, resources.size());
    assertEquals("t3", resources.iterator().next().getPropertyValue
      (ConfigGroupResourceProvider.CONFIGGROUP_TAG_PROPERTY_ID));

    // Read by hostname
    predicate = new PredicateBuilder().property(ConfigGroupResourceProvider
      .CONFIGGROUP_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").and()
      .property(ConfigGroupResourceProvider.CONFIGGROUP_HOSTS_PROPERTY_ID)
      .equals("h1").toPredicate();

    resources = resourceProvider.getResources(request, predicate);

    assertEquals(1, resources.size());
    Set<Map<String, Object>> hostSet = (Set<Map<String, Object>>)
      resources.iterator().next()
      .getPropertyValue(ConfigGroupResourceProvider
        .CONFIGGROUP_HOSTS_PROPERTY_ID);
    assertEquals("h1", hostSet.iterator().next().get
      (ConfigGroupResourceProvider.CONFIGGROUP_HOSTNAME_PROPERTY_ID));


    // Read by tag and hostname - Positive
    predicate = new PredicateBuilder().property(ConfigGroupResourceProvider
      .CONFIGGROUP_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").and()
      .property(ConfigGroupResourceProvider.CONFIGGROUP_TAG_PROPERTY_ID)
      .equals("t4").and().property(ConfigGroupResourceProvider
        .CONFIGGROUP_HOSTS_PROPERTY_ID).equals("h1").toPredicate();

    resources = resourceProvider.getResources(request, predicate);

    assertEquals(1, resources.size());
    hostSet = (Set<Map<String, Object>>)
      resources.iterator().next()
        .getPropertyValue(ConfigGroupResourceProvider
          .CONFIGGROUP_HOSTS_PROPERTY_ID);
    assertEquals("h1", hostSet.iterator().next().get
      (ConfigGroupResourceProvider.CONFIGGROUP_HOSTNAME_PROPERTY_ID));

    verify(managementController, clusters, cluster, configGroup1,
      configGroup2, configGroup3, configGroup4, response1, response2,
      response3, response4);
  }

  @Test
  public void testDeleteConfigGroup() throws Exception {
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);

    expect(managementController.getAuthName()).andReturn("admin").anyTimes();
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();
    cluster.deleteConfigGroup(1L);

    replay(managementController, clusters, cluster);

    ResourceProvider resourceProvider = getConfigGroupResourceProvider
      (managementController);

    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();

    ((ObservableResourceProvider) resourceProvider).addObserver(observer);

    Predicate predicate = new PredicateBuilder().property
      (ConfigGroupResourceProvider.CONFIGGROUP_CLUSTER_NAME_PROPERTY_ID)
      .equals("Cluster100").and().property(ConfigGroupResourceProvider
        .CONFIGGROUP_ID_PROPERTY_ID).equals(1L).toPredicate();

    resourceProvider.deleteResources(predicate);

    ResourceProviderEvent lastEvent = observer.getLastEvent();
    Assert.assertNotNull(lastEvent);
    Assert.assertEquals(Resource.Type.ConfigGroup, lastEvent.getResourceType());
    Assert.assertEquals(ResourceProviderEvent.Type.Delete, lastEvent.getType());
    Assert.assertEquals(predicate, lastEvent.getPredicate());
    Assert.assertNull(lastEvent.getRequest());

    verify(managementController, clusters, cluster);
  }
}
