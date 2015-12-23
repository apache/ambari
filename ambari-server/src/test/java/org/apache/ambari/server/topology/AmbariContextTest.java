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

package org.apache.ambari.server.topology;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ClusterRequest;
import org.apache.ambari.server.controller.ConfigGroupRequest;
import org.apache.ambari.server.controller.ServiceComponentRequest;
import org.apache.ambari.server.controller.ServiceRequest;
import org.apache.ambari.server.controller.internal.ComponentResourceProvider;
import org.apache.ambari.server.controller.internal.ConfigGroupResourceProvider;
import org.apache.ambari.server.controller.internal.HostComponentResourceProvider;
import org.apache.ambari.server.controller.internal.HostResourceProvider;
import org.apache.ambari.server.controller.internal.ServiceResourceProvider;
import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.controller.predicate.EqualsPredicate;
import org.apache.ambari.server.controller.spi.ClusterController;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.configgroup.ConfigGroup;
import org.easymock.Capture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * AmbariContext unit tests
 */
//todo: switch over to EasyMockSupport
public class AmbariContextTest {

  private static final String BP_NAME = "testBP";
  private static final String CLUSTER_NAME = "testCluster";
  private static final long CLUSTER_ID = 1L;
  private static final String STACK_NAME = "testStack";
  private static final String STACK_VERSION = "testVersion";
  private static final String HOST_GROUP_1 = "group1";
  private static final String HOST_GROUP_2 = "group2";
  private static final String HOST1 = "host1";
  private static final String HOST2 = "host2";
  StackId stackId = new StackId(STACK_NAME, STACK_VERSION);

  private static final AmbariContext context = new AmbariContext();
  private static final AmbariManagementController controller = createNiceMock(AmbariManagementController.class);
  private static final ClusterController clusterController = createStrictMock(ClusterController.class);
  private static final HostResourceProvider hostResourceProvider = createStrictMock(HostResourceProvider.class);
  private static final ServiceResourceProvider serviceResourceProvider = createStrictMock(ServiceResourceProvider.class);
  private static final ComponentResourceProvider componentResourceProvider = createStrictMock(ComponentResourceProvider.class);
  private static final HostComponentResourceProvider hostComponentResourceProvider = createStrictMock(HostComponentResourceProvider.class);
  private static final ConfigGroupResourceProvider configGroupResourceProvider = createStrictMock(ConfigGroupResourceProvider.class);
  private static final ClusterTopology topology = createNiceMock(ClusterTopology.class);
  private static final Blueprint blueprint = createNiceMock(Blueprint.class);
  private static final Stack stack = createNiceMock(Stack.class);
  private static final Clusters clusters = createNiceMock(Clusters.class);
  private static final Cluster cluster = createNiceMock(Cluster.class);
  private static final HostGroupInfo group1Info = createNiceMock(HostGroupInfo.class);
  private static final ConfigHelper configHelper = createNiceMock(ConfigHelper.class);
  private static final ConfigGroup configGroup1 = createMock(ConfigGroup.class);
  private static final ConfigGroup configGroup2 = createMock(ConfigGroup.class);
  private static final Host host1 = createNiceMock(Host.class);
  private static final Host host2 = createNiceMock(Host.class);

  private static final Collection<String> blueprintServices = new HashSet<String>();
  private static final Map<String, Service> clusterServices = new HashMap<String, Service>();
  private static final Map<Long, ConfigGroup> configGroups = new HashMap<Long, ConfigGroup>();
  private Configuration bpConfiguration = null;
  private Configuration group1Configuration = null;
  private static final Collection<String> group1Hosts = Arrays.asList(HOST1, HOST2);

  private Capture<Set<ConfigGroupRequest>> configGroupRequestCapture = new Capture<Set<ConfigGroupRequest>>();

  @Before
  public void setUp() throws Exception {
    // "inject" context state
    Class clazz = AmbariContext.class;
    Field f = clazz.getDeclaredField("controller");
    f.setAccessible(true);
    f.set(null, controller);

    f = clazz.getDeclaredField("clusterController");
    f.setAccessible(true);
    f.set(null, clusterController);

    f = clazz.getDeclaredField("hostResourceProvider");
    f.setAccessible(true);
    f.set(null, hostResourceProvider);

    f = clazz.getDeclaredField("serviceResourceProvider");
    f.setAccessible(true);
    f.set(null, serviceResourceProvider);

    f = clazz.getDeclaredField("componentResourceProvider");
    f.setAccessible(true);
    f.set(null, componentResourceProvider);

    f = clazz.getDeclaredField("hostComponentResourceProvider");
    f.setAccessible(true);
    f.set(null, hostComponentResourceProvider);

    // bp configuration
    Map<String, Map<String, String>> bpProperties = new HashMap<String, Map<String, String>>();
    Map<String, String> bpType1Props = new HashMap<String, String>();
    bpProperties.put("type1", bpType1Props);
    bpType1Props.put("prop1", "val1");
    bpType1Props.put("prop2", "val2");
    bpConfiguration = new Configuration(bpProperties, null);

    // host group 1 configuration
    Map<String, Map<String, String>> group1Properties = new HashMap<String, Map<String, String>>();
    Map<String, String> type1Props = new HashMap<String, String>();
    group1Properties.put("type1", type1Props);
    type1Props.put("prop1", "val1.2");
    type1Props.put("prop3", "val3");
    group1Configuration = new Configuration(group1Properties, null, bpConfiguration);

    // config type -> service mapping
    Map<String, String> configTypeServiceMapping = new HashMap<String, String>();
    configTypeServiceMapping.put("type1", "service1");

    // config groups
    configGroups.put(1L, configGroup1);
    configGroups.put(2L, configGroup2);

    blueprintServices.add("service1");
    blueprintServices.add("service2");

    expect(topology.getClusterId()).andReturn(CLUSTER_ID).anyTimes();
    expect(topology.getBlueprint()).andReturn(blueprint).anyTimes();
    expect(topology.getHostGroupInfo()).andReturn(Collections.singletonMap(HOST_GROUP_1, group1Info)).anyTimes();

    expect(blueprint.getName()).andReturn(BP_NAME).anyTimes();
    expect(blueprint.getStack()).andReturn(stack).anyTimes();
    expect(blueprint.getServices()).andReturn(blueprintServices).anyTimes();
    expect(blueprint.getComponents("service1")).andReturn(Arrays.asList("s1Component1", "s1Component2")).anyTimes();
    expect(blueprint.getComponents("service2")).andReturn(Collections.singleton("s2Component1")).anyTimes();
    expect(blueprint.getConfiguration()).andReturn(bpConfiguration).anyTimes();

    expect(stack.getName()).andReturn(STACK_NAME).anyTimes();
    expect(stack.getVersion()).andReturn(STACK_VERSION).anyTimes();

    for (Map.Entry<String, String> entry : configTypeServiceMapping.entrySet()) {
      expect(stack.getServiceForConfigType(entry.getKey())).andReturn(entry.getValue()).anyTimes();
    }

    expect(controller.getClusters()).andReturn(clusters).anyTimes();
    expect(controller.getConfigHelper()).andReturn(configHelper).anyTimes();

    expect(clusters.getCluster(CLUSTER_NAME)).andReturn(cluster).anyTimes();
    expect(clusters.getClusterById(CLUSTER_ID)).andReturn(cluster).anyTimes();
    expect(clusters.getHost(HOST1)).andReturn(host1).anyTimes();
    expect(clusters.getHost(HOST2)).andReturn(host2).anyTimes();

    expect(cluster.getClusterId()).andReturn(CLUSTER_ID).anyTimes();
    expect(cluster.getClusterName()).andReturn(CLUSTER_NAME).anyTimes();

    expect(host1.getHostId()).andReturn(1L).anyTimes();
    expect(host2.getHostId()).andReturn(2L).anyTimes();

    expect(group1Info.getConfiguration()).andReturn(group1Configuration).anyTimes();
    expect(group1Info.getHostNames()).andReturn(group1Hosts).anyTimes();

    expect(configGroup1.getName()).andReturn(String.format("%s:%s", BP_NAME, HOST_GROUP_1)).anyTimes();
    expect(configGroup2.getName()).andReturn(String.format("%s:%s", BP_NAME, HOST_GROUP_2)).anyTimes();
  }

  @After
  public void tearDown() throws Exception {
    verify(controller, clusterController, hostResourceProvider, serviceResourceProvider, componentResourceProvider,
        hostComponentResourceProvider, configGroupResourceProvider, topology, blueprint, stack, clusters,
        cluster, group1Info, configHelper, configGroup1, configGroup2, host1, host2);

    reset(controller, clusterController, hostResourceProvider, serviceResourceProvider, componentResourceProvider,
        hostComponentResourceProvider, configGroupResourceProvider, topology, blueprint, stack, clusters,
        cluster, group1Info, configHelper, configGroup1, configGroup2, host1, host2);
  }

  private void replayAll() {
    replay(controller, clusterController, hostResourceProvider, serviceResourceProvider, componentResourceProvider,
        hostComponentResourceProvider, configGroupResourceProvider, topology, blueprint, stack, clusters,
        cluster, group1Info, configHelper, configGroup1, configGroup2, host1, host2);
  }

  @Test
  public void testCreateAmbariResources() throws Exception {
    // expectations
    Capture<ClusterRequest> clusterRequestCapture = new Capture<ClusterRequest>();
    controller.createCluster(capture(clusterRequestCapture));
    expectLastCall().once();
    expect(cluster.getServices()).andReturn(clusterServices).anyTimes();

    Capture<Set<ServiceRequest>> serviceRequestCapture = new Capture<Set<ServiceRequest>>();
    Capture<Set<ServiceComponentRequest>> serviceComponentRequestCapture = new Capture<Set<ServiceComponentRequest>>();

    serviceResourceProvider.createServices(capture(serviceRequestCapture));
    expectLastCall().once();
    componentResourceProvider.createComponents(capture(serviceComponentRequestCapture));
    expectLastCall().once();

    Capture<Request> serviceInstallRequestCapture = new Capture<Request>();
    Capture<Request> serviceStartRequestCapture = new Capture<Request>();
    Capture<Predicate> installPredicateCapture = new Capture<Predicate>();
    Capture<Predicate> startPredicateCapture = new Capture<Predicate>();

    expect(serviceResourceProvider.updateResources(capture(serviceInstallRequestCapture),
        capture(installPredicateCapture))).andReturn(null).once();
    expect(serviceResourceProvider.updateResources(capture(serviceStartRequestCapture),
        capture(startPredicateCapture))).andReturn(null).once();

    replayAll();

    // test
    context.createAmbariResources(topology, CLUSTER_NAME, null);

    // assertions
    ClusterRequest clusterRequest = clusterRequestCapture.getValue();
    assertEquals(CLUSTER_NAME, clusterRequest.getClusterName());
    assertEquals(String.format("%s-%s", STACK_NAME, STACK_VERSION), clusterRequest.getStackVersion());

    Collection<ServiceRequest> serviceRequests = serviceRequestCapture.getValue();
    assertEquals(2, serviceRequests.size());
    Collection<String> servicesFound = new HashSet<String>();
    for (ServiceRequest serviceRequest : serviceRequests) {
      servicesFound.add(serviceRequest.getServiceName());
      assertEquals(CLUSTER_NAME, serviceRequest.getClusterName());
    }
    assertTrue(servicesFound.size() == 2 &&
        servicesFound.containsAll(Arrays.asList("service1", "service2")));

    Collection<ServiceComponentRequest> serviceComponentRequests = serviceComponentRequestCapture.getValue();
    assertEquals(3, serviceComponentRequests.size());
    Map<String, Collection<String>> foundServiceComponents = new HashMap<String, Collection<String>>();
    for (ServiceComponentRequest componentRequest : serviceComponentRequests) {
      assertEquals(CLUSTER_NAME, componentRequest.getClusterName());
      String serviceName = componentRequest.getServiceName();
      Collection<String> serviceComponents = foundServiceComponents.get(serviceName);
      if (serviceComponents == null) {
        serviceComponents = new HashSet<String>();
        foundServiceComponents.put(serviceName, serviceComponents);
      }
      serviceComponents.add(componentRequest.getComponentName());
    }
    assertEquals(2, foundServiceComponents.size());

    Collection<String> service1Components = foundServiceComponents.get("service1");
    assertEquals(2, service1Components.size());
    assertTrue(service1Components.containsAll(Arrays.asList("s1Component1", "s1Component2")));

    Collection<String> service2Components = foundServiceComponents.get("service2");
    assertEquals(1, service2Components.size());
    assertTrue(service2Components.contains("s2Component1"));

    Request installRequest = serviceInstallRequestCapture.getValue();
    Set<Map<String, Object>> installPropertiesSet = installRequest.getProperties();
    assertEquals(1, installPropertiesSet.size());
    Map<String, Object> installProperties = installPropertiesSet.iterator().next();
    assertEquals(CLUSTER_NAME, installProperties.get(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID));
    assertEquals("INSTALLED", installProperties.get(ServiceResourceProvider.SERVICE_SERVICE_STATE_PROPERTY_ID));
    assertEquals(new EqualsPredicate<String>(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID, CLUSTER_NAME),
        installPredicateCapture.getValue());

    Request startRequest = serviceStartRequestCapture.getValue();
    Set<Map<String, Object>> startPropertiesSet = startRequest.getProperties();
    assertEquals(1, startPropertiesSet.size());
    Map<String, Object> startProperties = startPropertiesSet.iterator().next();
    assertEquals(CLUSTER_NAME, startProperties.get(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID));
    assertEquals("STARTED", startProperties.get(ServiceResourceProvider.SERVICE_SERVICE_STATE_PROPERTY_ID));
    assertEquals(new EqualsPredicate<String>(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID, CLUSTER_NAME),
        installPredicateCapture.getValue());
  }

  @Test
  public void testRegisterHostWithConfigGroup_createNewConfigGroup() throws Exception {
    // test specific expectations
    expect(cluster.getConfigGroups()).andReturn(Collections.<Long, ConfigGroup>emptyMap()).once();
    expect(clusterController.ensureResourceProvider(Resource.Type.ConfigGroup)).andReturn(configGroupResourceProvider).once();
    //todo: for now not using return value so just returning null
    expect(configGroupResourceProvider.createResources(capture(configGroupRequestCapture))).andReturn(null).once();
    configHelper.moveDeprecatedGlobals(stackId, group1Configuration.getFullProperties(1), CLUSTER_NAME);
    // replay all mocks
    replayAll();

    // test
    context.registerHostWithConfigGroup(HOST1, topology, HOST_GROUP_1);

    // assertions
    Set<ConfigGroupRequest> configGroupRequests = configGroupRequestCapture.getValue();
    assertEquals(1, configGroupRequests.size());
    ConfigGroupRequest configGroupRequest = configGroupRequests.iterator().next();
    assertEquals(CLUSTER_NAME, configGroupRequest.getClusterName());
    assertEquals("testBP:group1", configGroupRequest.getGroupName());
    assertEquals("service1", configGroupRequest.getTag());
    assertEquals("Host Group Configuration", configGroupRequest.getDescription());
    Collection<String> requestHosts = configGroupRequest.getHosts();
    requestHosts.retainAll(group1Hosts);
    assertEquals(group1Hosts.size(), requestHosts.size());

    Map<String, Config> requestConfig = configGroupRequest.getConfigs();
    assertEquals(1, requestConfig.size());
    Config type1Config = requestConfig.get("type1");
    //todo: other properties such as cluster name are not currently being explicitly set on config
    assertEquals("type1", type1Config.getType());
    assertEquals("group1", type1Config.getTag());
    Map<String, String> requestProps = type1Config.getProperties();
    assertEquals(3, requestProps.size());
    // 1.2 is overridden value
    assertEquals("val1.2", requestProps.get("prop1"));
    assertEquals("val2", requestProps.get("prop2"));
    assertEquals("val3", requestProps.get("prop3"));
  }

  @Test
  public void testRegisterHostWithConfigGroup_registerWithExistingConfigGroup() throws Exception {
    // test specific expectations
    expect(cluster.getConfigGroups()).andReturn(configGroups).once();

    expect(configGroup1.getHosts()).andReturn(Collections.singletonMap(2L, host2)).once();
    configGroup1.addHost(host1);
    configGroup1.persistHostMapping();

    // replay all mocks
    replayAll();

    // test
    context.registerHostWithConfigGroup(HOST1, topology, HOST_GROUP_1);
  }

  @Test
  public void testRegisterHostWithConfigGroup_registerWithExistingConfigGroup_hostAlreadyRegistered() throws Exception {
    // test specific expectations
    expect(cluster.getConfigGroups()).andReturn(configGroups).once();

    expect(configGroup1.getHosts()).andReturn(Collections.singletonMap(1L, host1)).once();
    // addHost and persistHostMapping shouldn't be called since host is already registerd with group

    // replay all mocks
    replayAll();

    // test
    context.registerHostWithConfigGroup(HOST1, topology, HOST_GROUP_1);
  }

  @Test
  public void testWaitForTopologyResolvedStateWithEmptyUpdatedSet() throws Exception {
    replayAll();

    // verify that wait returns successfully with empty updated list passed in
    context.waitForConfigurationResolution(CLUSTER_NAME, Collections.<String>emptySet());
  }

  @Test
  public void testWaitForTopologyResolvedStateWithRequiredUpdatedSet() throws Exception {
    final String topologyResolvedState = "TOPOLOGY_RESOLVED";
    DesiredConfig testHdfsDesiredConfig =
      new DesiredConfig();
    testHdfsDesiredConfig.setTag(topologyResolvedState);
    DesiredConfig testCoreSiteDesiredConfig =
      new DesiredConfig();
    testCoreSiteDesiredConfig.setTag(topologyResolvedState);
    DesiredConfig testClusterEnvDesiredConfig =
      new DesiredConfig();
    testClusterEnvDesiredConfig.setTag(topologyResolvedState);

    Map<String, DesiredConfig> testDesiredConfigs =
      new HashMap<String, DesiredConfig>();
    testDesiredConfigs.put("hdfs-site", testHdfsDesiredConfig);
    testDesiredConfigs.put("core-site", testCoreSiteDesiredConfig);
    testDesiredConfigs.put("cluster-env", testClusterEnvDesiredConfig);

    expect(cluster.getDesiredConfigs()).andReturn(testDesiredConfigs).atLeastOnce();

    replayAll();

    Set<String> testUpdatedConfigTypes =
      new HashSet<String>();
    testUpdatedConfigTypes.add("hdfs-site");
    testUpdatedConfigTypes.add("core-site");
    testUpdatedConfigTypes.add("cluster-env");

    // verify that wait returns successfully with non-empty list
    // with all configuration types tagged as "TOPOLOGY_RESOLVED"
    context.waitForConfigurationResolution(CLUSTER_NAME, testUpdatedConfigTypes);
  }

  @Test
  public void testIsTopologyResolved_True() throws Exception {

    // Given
    DesiredConfig testHdfsDesiredConfig1 = new DesiredConfig();
    testHdfsDesiredConfig1.setTag(TopologyManager.INITIAL_CONFIG_TAG);
    testHdfsDesiredConfig1.setVersion(1L);

    DesiredConfig testHdfsDesiredConfig2 = new DesiredConfig();
    testHdfsDesiredConfig2.setTag(TopologyManager.TOPOLOGY_RESOLVED_TAG);
    testHdfsDesiredConfig2.setVersion(2L);

    DesiredConfig testHdfsDesiredConfig3 = new DesiredConfig();
    testHdfsDesiredConfig3.setTag("ver123");
    testHdfsDesiredConfig3.setVersion(3L);

    DesiredConfig testCoreSiteDesiredConfig = new DesiredConfig();
    testCoreSiteDesiredConfig.setTag("ver123");
    testCoreSiteDesiredConfig.setVersion(1L);


    Map<String, Set<DesiredConfig>> testDesiredConfigs = ImmutableMap.<String, Set<DesiredConfig>>builder()
      .put("hdfs-site", ImmutableSet.of(testHdfsDesiredConfig2, testHdfsDesiredConfig3, testHdfsDesiredConfig1))
      .put("core-site", ImmutableSet.of(testCoreSiteDesiredConfig))
      .build();

    expect(cluster.getAllDesiredConfigVersions()).andReturn(testDesiredConfigs).atLeastOnce();

    replayAll();

    // When
    boolean topologyResolved = context.isTopologyResolved(CLUSTER_ID);

    // Then
    assertTrue(topologyResolved);
  }

  @Test
  public void testIsTopologyResolved_WrongOrder_False() throws Exception {

    // Given
    DesiredConfig testHdfsDesiredConfig1 = new DesiredConfig();
    testHdfsDesiredConfig1.setTag(TopologyManager.INITIAL_CONFIG_TAG);
    testHdfsDesiredConfig1.setVersion(2L);

    DesiredConfig testHdfsDesiredConfig2 = new DesiredConfig();
    testHdfsDesiredConfig2.setTag(TopologyManager.TOPOLOGY_RESOLVED_TAG);
    testHdfsDesiredConfig2.setVersion(1L);

    DesiredConfig testHdfsDesiredConfig3 = new DesiredConfig();
    testHdfsDesiredConfig3.setTag("ver123");
    testHdfsDesiredConfig3.setVersion(3L);

    DesiredConfig testCoreSiteDesiredConfig = new DesiredConfig();
    testCoreSiteDesiredConfig.setTag("ver123");
    testCoreSiteDesiredConfig.setVersion(1L);


    Map<String, Set<DesiredConfig>> testDesiredConfigs = ImmutableMap.<String, Set<DesiredConfig>>builder()
      .put("hdfs-site", ImmutableSet.of(testHdfsDesiredConfig2, testHdfsDesiredConfig3, testHdfsDesiredConfig1))
      .put("core-site", ImmutableSet.of(testCoreSiteDesiredConfig))
      .build();

    expect(cluster.getAllDesiredConfigVersions()).andReturn(testDesiredConfigs).atLeastOnce();

    replayAll();

    // When
    boolean topologyResolved = context.isTopologyResolved(CLUSTER_ID);

    // Then due to INITIAL -> TOPOLOGY_RESOLVED not honored
    assertFalse(topologyResolved);
  }

  @Test
  public void testIsTopologyResolved_False() throws Exception {

    // Given
    DesiredConfig testHdfsDesiredConfig1 = new DesiredConfig();
    testHdfsDesiredConfig1.setTag("ver1222");
    testHdfsDesiredConfig1.setVersion(1L);


    DesiredConfig testCoreSiteDesiredConfig = new DesiredConfig();
    testCoreSiteDesiredConfig.setTag("ver123");
    testCoreSiteDesiredConfig.setVersion(1L);


    Map<String, Set<DesiredConfig>> testDesiredConfigs = ImmutableMap.<String, Set<DesiredConfig>>builder()
      .put("hdfs-site", ImmutableSet.of(testHdfsDesiredConfig1))
      .put("core-site", ImmutableSet.of(testCoreSiteDesiredConfig))
      .build();

    expect(cluster.getAllDesiredConfigVersions()).andReturn(testDesiredConfigs).atLeastOnce();

    replayAll();

    // When
    boolean topologyResolved = context.isTopologyResolved(CLUSTER_ID);

    // Then
    assertFalse(topologyResolved);
  }


}
