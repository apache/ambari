/*
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

import static java.util.Arrays.asList;
import static org.apache.ambari.server.topology.StackComponentResolverTest.builderFor;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.mock;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.createNiceMock;
import static org.powermock.api.easymock.PowerMock.replay;
import static org.powermock.api.easymock.PowerMock.reset;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.internal.ProvisionAction;
import org.apache.ambari.server.controller.internal.StackDefinition;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Unit tests for ClusterTopologyImpl.
 */
@SuppressWarnings("unchecked")
@RunWith(PowerMockRunner.class)
@PrepareForTest({AmbariContext.class})
public class ClusterTopologyImplTest {

  private static final String CLUSTER_NAME = "cluster_name";
  private static final long CLUSTER_ID = 1L;
  private static final String predicate = "Hosts/host_name=foo";
  private static final Blueprint blueprint = createNiceMock(Blueprint.class);
  private static final HostGroup group1 = createNiceMock(HostGroup.class);
  private static final HostGroup group2 = createNiceMock(HostGroup.class);
  private static final HostGroup group3 = createNiceMock(HostGroup.class);
  private static final HostGroup group4 = createNiceMock(HostGroup.class);
  private static final StackId STACK_ID = new StackId("HDP", "2.6");
  private static final ImmutableSet<StackId> STACK_IDS = ImmutableSet.of(STACK_ID);
  private final AmbariContext ambariContext = createNiceMock(AmbariContext.class);
  private final StackDefinition stack = createNiceMock(StackDefinition.class);
  private final Map<String, HostGroupInfo> hostGroupInfoMap = new HashMap<>();
  private final Map<String, HostGroup> hostGroupMap = new HashMap<>();
  private final Map<String, Set<ResolvedComponent>> resolvedComponents = ImmutableMap.of(
    "group1", ImmutableSet.of(
      builderFor("any_service", "component1"),
      builderFor("any_service", "component2")),
    "group2", ImmutableSet.of(
      builderFor("any_service", "component3")),
    "group3", ImmutableSet.of(
      builderFor("any_service", "component4")),
    "group4", ImmutableSet.of(
      builderFor("any_service", "component5"))
  );
  private final AmbariManagementController controller = mock(AmbariManagementController.class);
  private final AmbariMetaInfo metaInfo = mock(AmbariMetaInfo.class);
  private BlueprintBasedClusterProvisionRequest provisionRequest;

  private Configuration configuration;
  private Configuration bpconfiguration;

  @Before
  public void setUp() throws Exception {
    configuration = new Configuration(new HashMap<>(),
      new HashMap<>());
    bpconfiguration = new Configuration(
      Maps.newHashMap(ImmutableMap.of(
        "cluster-env",
        Maps.newHashMap(ImmutableMap.of(
          "commands_to_retry", "INSTALL",
          "command_retry_max_time_in_sec", "500",
          "unknown_property_that_should_not_become_cluster_setting", "some_value"))
      )),
      new HashMap<>());
    HostGroupInfo group1Info = new HostGroupInfo("group1");
    HostGroupInfo group2Info = new HostGroupInfo("group2");
    HostGroupInfo group3Info = new HostGroupInfo("group3");
    HostGroupInfo group4Info = new HostGroupInfo("group4");
    hostGroupInfoMap.put("group1", group1Info);
    hostGroupInfoMap.put("group2", group2Info);
    hostGroupInfoMap.put("group3", group3Info);
    hostGroupInfoMap.put("group4", group4Info);

    group1Info.setConfiguration(configuration);
    Collection<String> group1Hosts = new HashSet<>();
    group1Hosts.add("host1");
    group1Hosts.add("host2");
    group1Info.addHosts(group1Hosts);

    group2Info.setConfiguration(configuration);
    Collection<String> group2Hosts = new HashSet<>();
    group2Hosts.add("host3");
    group2Info.addHosts(group2Hosts);
    Collection<String> group4Hosts = new HashSet<>();
    group4Hosts.add("host4");
    group4Hosts.add("host5");
    group4Info.addHosts(group4Hosts);

    group3Info.setConfiguration(configuration);
    group3Info.setRequestedCount(5);

    group4Info.setConfiguration(configuration);
    group4Info.setRequestedCount(5);
    group4Info.setPredicate(predicate);

    expect(blueprint.getConfiguration()).andReturn(bpconfiguration).anyTimes();
    expect(blueprint.getSecurity()).andReturn(SecurityConfiguration.NONE).anyTimes();
    expect(blueprint.getStackIds()).andReturn(STACK_IDS).anyTimes();

    hostGroupMap.put("group1", group1);
    hostGroupMap.put("group2", group2);
    hostGroupMap.put("group3", group3);
    hostGroupMap.put("group4", group4);

    expect(stack.getServicesForComponent("ONEFS_CLIENT")).andAnswer(() -> Stream.of(Pair.of(STACK_ID, aHCFSWith(aComponent("ONEFS_CLIENT"))))).anyTimes();
    expect(stack.getServicesForComponent("ZOOKEEPER_CLIENT")).andAnswer(() -> Stream.of(Pair.of(STACK_ID, aServiceWith(aComponent("ZOOKEEPER_CLIENT"))))).anyTimes();

    expect(ambariContext.composeStacks(STACK_IDS)).andReturn(stack).anyTimes();

    expect(blueprint.getMpacks()).andReturn(ImmutableSet.of()).anyTimes();
    expect(blueprint.getHostGroups()).andReturn(hostGroupMap).anyTimes();
    for (Map.Entry<String, HostGroup> entry : hostGroupMap.entrySet()) {
      String name = entry.getKey();
      HostGroup hostGroup = entry.getValue();
      expect(hostGroup.getName()).andReturn(name).anyTimes();
      expect(blueprint.getHostGroup(name)).andReturn(hostGroup).anyTimes();
    }
    expect(blueprint.getSetting()).andReturn(new Setting(new HashMap<>()));

    expect(group1.getConfiguration()).andReturn(configuration).anyTimes();
    expect(group2.getConfiguration()).andReturn(configuration).anyTimes();
    expect(group3.getConfiguration()).andReturn(configuration).anyTimes();
    expect(group4.getConfiguration()).andReturn(configuration).anyTimes();

    expect(controller.getAmbariMetaInfo()).andReturn(metaInfo).anyTimes();
    expect(metaInfo.getClusterProperties()).andReturn(
      Sets.newHashSet(
        propertyInfo("command_retry_enabled", "true"),
        propertyInfo("commands_to_retry", "INSTALL,START"),
        propertyInfo("command_retry_max_time_in_sec", "600"))).anyTimes();

    replayAll();

    PowerMock.mockStatic(AmbariContext.class);
    AmbariContext.getController();
    expectLastCall().andReturn(controller).anyTimes();
    PowerMock.replay(AmbariContext.class);


    provisionRequest = new BlueprintBasedClusterProvisionRequest(ambariContext, null, blueprint, new TestTopologyRequest());
  }

  @After
  public void tearDown() {
    reset(ambariContext, stack, blueprint, group1, group2, group3, group4);

    hostGroupInfoMap.clear();
    hostGroupMap.clear();
  }

  private void replayAll() {
    replay(ambariContext, stack, blueprint, group1, group2, group3, group4, controller, metaInfo);
  }



  @Test(expected = InvalidTopologyException.class)
  public void testCreate_duplicateHosts() throws Exception {
    // add a duplicate host
    hostGroupInfoMap.get("group2").addHost("host1");

    new ClusterTopologyImpl(ambariContext, provisionRequest, resolvedComponents);
  }

  @Test(expected = InvalidTopologyException.class)
  public void testUpdate_duplicateHosts() throws Exception {
    ClusterTopology topology = new ClusterTopologyImpl(ambariContext, provisionRequest, resolvedComponents);
    // add a duplicate host
    hostGroupInfoMap.get("group2").addHost("host1");

    // should throw exception due to duplicate host
    topology.update(provisionRequest);
  }

  @Test
  public void test_GetHostAssignmentForComponents() throws Exception {
    ClusterTopologyImpl topology = new ClusterTopologyImpl(ambariContext, provisionRequest, resolvedComponents);

    Collection<String> assignments = topology.getHostAssignmentsForComponent("component1");
    assertEquals(ImmutableSet.of("host1", "host2"), ImmutableSet.copyOf(assignments));
  }

  @Ignore
  @Test(expected = InvalidTopologyException.class)
  public void testCreate_NNHAInvaid() throws Exception {
    bpconfiguration.setProperty("hdfs-site", "dfs.nameservices", "val");
    hostGroupInfoMap.get("group4").removeHost("host5");
    replayAll();

    new ClusterTopologyImpl(ambariContext, provisionRequest, resolvedComponents);
  }

  @Ignore
  @Test(expected = IllegalArgumentException.class)
  public void testCreate_NNHAHostNameNotCorrectForStandby() throws Exception {
    bpconfiguration.setProperty("hdfs-site", "dfs.nameservices", "val");
    bpconfiguration.setProperty("hadoop-env", "dfs_ha_initial_namenode_active", "host4");
    bpconfiguration.setProperty("hadoop-env", "dfs_ha_initial_namenode_standby", "val");

    new ClusterTopologyImpl(ambariContext, provisionRequest, resolvedComponents);
  }

  @Ignore
  @Test(expected = IllegalArgumentException.class)
  public void testCreate_NNHAHostNameNotCorrectForActive() throws Exception {
    bpconfiguration.setProperty("hdfs-site", "dfs.nameservices", "val");
    bpconfiguration.setProperty("hadoop-env", "dfs_ha_initial_namenode_active", "val");
    bpconfiguration.setProperty("hadoop-env", "dfs_ha_initial_namenode_standby", "host5");

    new ClusterTopologyImpl(ambariContext, provisionRequest, resolvedComponents);
  }

  @Ignore
  @Test(expected = IllegalArgumentException.class)
  public void testCreate_NNHAHostNameNotCorrectForStandbyWithActiveAsVariable() throws Exception {
    bpconfiguration.setProperty("hdfs-site", "dfs.nameservices", "val");
    bpconfiguration.setProperty("hadoop-env", "dfs_ha_initial_namenode_active", "%HOSTGROUP::group4%");
    bpconfiguration.setProperty("hadoop-env", "dfs_ha_initial_namenode_standby", "host6");

    new ClusterTopologyImpl(ambariContext, provisionRequest, resolvedComponents);
  }

  @Test
  public void testDecidingIfComponentIsHadoopCompatible() throws Exception {
    ClusterTopologyImpl topology = new ClusterTopologyImpl(ambariContext, provisionRequest, resolvedComponents);

    assertTrue(topology.isComponentHadoopCompatible("ONEFS_CLIENT"));
    assertFalse(topology.isComponentHadoopCompatible("ZOOKEEPER_CLIENT"));
  }

  @Test
  public void testAdjustTopology() throws Exception {
    ClusterTopologyImpl topology = new ClusterTopologyImpl(ambariContext, provisionRequest, resolvedComponents);
    Map<String, String> clusterSettings = topology.getSetting().getClusterSettings();
    assertEquals(
      ImmutableMap.of(
        "commands_to_retry", "INSTALL",
        "command_retry_max_time_in_sec", "500"),
      clusterSettings
    );
    assertEquals(
      ImmutableMap.of("", ""),
      topology.getConfiguration().getFullProperties().get("cluster-env")
    );
  }


  private ServiceInfo aHCFSWith(ComponentInfo... components) {
    ServiceInfo service = aServiceWith(components);
    service.setServiceType(ServiceInfo.HADOOP_COMPATIBLE_FS);
    return service;
  }

  private ServiceInfo aServiceWith(ComponentInfo... components) {
    ServiceInfo service = new ServiceInfo();
    service.getComponents().addAll(asList(components));
    return service;
  }

  private ComponentInfo aComponent(String name) {
    ComponentInfo component = new ComponentInfo();
    component.setName(name);
    return component;
  }

  private static PropertyInfo propertyInfo(String name, String value) {
    PropertyInfo info = new PropertyInfo();
    info.setName(name);
    info.setValue(value);
    return info;
  }

  private class TestTopologyRequest implements ProvisionRequest {

    @Override
    public String getClusterName() {
      return CLUSTER_NAME;
    }

    @Override
    public Long getClusterId() {
      return CLUSTER_ID;
    }

    @Override
    public Type getType() {
      return Type.PROVISION;
    }

    @Override
    public Blueprint getBlueprint() {
      return blueprint;
    }

    @Override
    public Configuration getConfiguration() {
      return bpconfiguration;
    }

    @Override
    public Map<String, HostGroupInfo> getHostGroupInfo() {
      return hostGroupInfoMap;
    }

    @Override
    public String getDescription() {
      return "Test Request";
    }

    @Override
    public ConfigRecommendationStrategy getConfigRecommendationStrategy() {
      return ConfigRecommendationStrategy.NEVER_APPLY;
    }

    @Override
    public ProvisionAction getProvisionAction() {
      return ProvisionAction.INSTALL_AND_START;
    }

    @Override
    public String getDefaultPassword() {
      return "password";
    }

    @Override
    public Set<StackId> getStackIds() {
      return ImmutableSet.of();
    }

    @Override
    public Collection<MpackInstance> getMpacks() {
      return ImmutableSet.of();
    }

    @Override
    public SecurityConfiguration getSecurityConfiguration() {
      return SecurityConfiguration.NONE;
    }
  }
}
