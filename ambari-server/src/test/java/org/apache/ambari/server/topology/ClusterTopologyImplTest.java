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

import static org.easymock.EasyMock.expect;
import static org.powermock.api.easymock.PowerMock.createNiceMock;
import static org.powermock.api.easymock.PowerMock.replay;
import static org.powermock.api.easymock.PowerMock.reset;
import static org.powermock.api.easymock.PowerMock.verify;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for ClusterTopologyImpl.
 */
@SuppressWarnings("unchecked")
public class ClusterTopologyImplTest {

  private static final String CLUSTER_NAME = "cluster_name";
  private static final long CLUSTER_ID = 1L;
  private static final String predicate = "Hosts/host_name=foo";
  private static final Blueprint blueprint = createNiceMock(Blueprint.class);
  private static final HostGroup group1 = createNiceMock(HostGroup.class);
  private static final HostGroup group2 = createNiceMock(HostGroup.class);
  private static final HostGroup group3 = createNiceMock(HostGroup.class);
  private static final HostGroup group4 = createNiceMock(HostGroup.class);
  private final Map<String, HostGroupInfo> hostGroupInfoMap = new HashMap<>();
  private final Map<String, HostGroup> hostGroupMap = new HashMap<>();

  private Configuration configuration;
  private Configuration bpconfiguration;

  @Before
  public void setUp() throws Exception {

    configuration = new Configuration(new HashMap<>(),
      new HashMap<>());
    bpconfiguration = new Configuration(new HashMap<>(),
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

    hostGroupMap.put("group1", group1);
    hostGroupMap.put("group2", group2);
    hostGroupMap.put("group3", group3);
    hostGroupMap.put("group4", group4);

    Set<Component> group1Components = new HashSet<>();
    group1Components.add(new Component("component1"));
    group1Components.add(new Component("component2"));

    Set<String> group1ComponentNames = new HashSet<>();
    group1ComponentNames.add("component1");
    group1ComponentNames.add("component2");

    Set<Component> group2Components = new HashSet<>();
    group2Components.add(new Component("component3"));
    Set<Component> group3Components = new HashSet<>();
    group3Components.add(new Component("component4"));
    Set<Component> group4Components = new HashSet<>();
    group4Components.add(new Component("component5"));

    expect(blueprint.getHostGroups()).andReturn(hostGroupMap).anyTimes();
    expect(blueprint.getHostGroup("group1")).andReturn(group1).anyTimes();
    expect(blueprint.getHostGroup("group2")).andReturn(group2).anyTimes();
    expect(blueprint.getHostGroup("group3")).andReturn(group3).anyTimes();
    expect(blueprint.getHostGroup("group4")).andReturn(group4).anyTimes();

    expect(group1.getConfiguration()).andReturn(configuration).anyTimes();
    expect(group2.getConfiguration()).andReturn(configuration).anyTimes();
    expect(group3.getConfiguration()).andReturn(configuration).anyTimes();
    expect(group4.getConfiguration()).andReturn(configuration).anyTimes();

    expect(group1.getComponents()).andReturn(group1Components).anyTimes();
    expect(group2.getComponents()).andReturn(group2Components).anyTimes();
    expect(group3.getComponents()).andReturn(group3Components).anyTimes();
    expect(group4.getComponents()).andReturn(group4Components).anyTimes();

    expect(group1.getComponentNames()).andReturn(group1ComponentNames).anyTimes();
    expect(group2.getComponentNames()).andReturn(Collections.singletonList("component3")).anyTimes();
    expect(group3.getComponentNames()).andReturn(Collections.singletonList("component4")).anyTimes();
    expect(group4.getComponentNames()).andReturn(Collections.singletonList("NAMENODE")).anyTimes();
  }

  @After
  public void tearDown() {
    verify(blueprint, group1, group2, group3, group4);
    reset(blueprint, group1, group2, group3, group4);


    hostGroupInfoMap.clear();
    hostGroupMap.clear();
  }

  private void replayAll() {
    replay(blueprint, group1, group2, group3, group4);
  }



  @Test(expected = InvalidTopologyException.class)
  public void testCreate_duplicateHosts() throws Exception {
    // add a duplicate host
    hostGroupInfoMap.get("group2").addHost("host1");

    TestTopologyRequest request = new TestTopologyRequest(TopologyRequest.Type.PROVISION);

    replayAll();
    // should throw exception due to duplicate host
    new ClusterTopologyImpl(null, request);
  }

  @Test
  public void test_GetHostAssigmentForComponents() throws Exception {
    TestTopologyRequest request = new TestTopologyRequest(TopologyRequest.Type.PROVISION);

    replayAll();

    new ClusterTopologyImpl(null, request).getHostAssignmentsForComponent("component1");
  }

  @Test(expected = InvalidTopologyException.class)
  public void testCreate_NNHAInvaid() throws Exception {
    bpconfiguration.setProperty("hdfs-site", "dfs.nameservices", "val");
    expect(group4.getName()).andReturn("group4");
    hostGroupInfoMap.get("group4").removeHost("host5");
    TestTopologyRequest request = new TestTopologyRequest(TopologyRequest.Type.PROVISION);
    replayAll();
    new ClusterTopologyImpl(null, request);
    hostGroupInfoMap.get("group4").addHost("host5");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreate_NNHAHostNameNotCorrectForStandby() throws Exception {
    expect(group4.getName()).andReturn("group4");
    bpconfiguration.setProperty("hdfs-site", "dfs.nameservices", "val");
    bpconfiguration.setProperty("hadoop-env", "dfs_ha_initial_namenode_active", "host4");
    bpconfiguration.setProperty("hadoop-env", "dfs_ha_initial_namenode_standby", "val");
    TestTopologyRequest request = new TestTopologyRequest(TopologyRequest.Type.PROVISION);
    replayAll();
    new ClusterTopologyImpl(null, request);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreate_NNHAHostNameNotCorrectForActive() throws Exception {
    expect(group4.getName()).andReturn("group4");
    bpconfiguration.setProperty("hdfs-site", "dfs.nameservices", "val");
    bpconfiguration.setProperty("hadoop-env", "dfs_ha_initial_namenode_active", "val");
    bpconfiguration.setProperty("hadoop-env", "dfs_ha_initial_namenode_standby", "host5");
    TestTopologyRequest request = new TestTopologyRequest(TopologyRequest.Type.PROVISION);
    replayAll();
    new ClusterTopologyImpl(null, request);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreate_NNHAHostNameNotCorrectForStandbyWithActiveAsVariable() throws Exception {
    expect(group4.getName()).andReturn("group4");
    bpconfiguration.setProperty("hdfs-site", "dfs.nameservices", "val");
    bpconfiguration.setProperty("hadoop-env", "dfs_ha_initial_namenode_active", "%HOSTGROUP::group4%");
    bpconfiguration.setProperty("hadoop-env", "dfs_ha_initial_namenode_standby", "host6");
    TestTopologyRequest request = new TestTopologyRequest(TopologyRequest.Type.PROVISION);
    replayAll();
    new ClusterTopologyImpl(null, request);
  }

  private class TestTopologyRequest implements TopologyRequest {
    private Type type;

    public TestTopologyRequest(Type type) {
      this.type = type;
    }

    public String getClusterName() {
      return CLUSTER_NAME;
    }

    @Override
    public Long getClusterId() {
      return CLUSTER_ID;
    }

    @Override
    public Type getType() {
      return type;
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
  }
}
