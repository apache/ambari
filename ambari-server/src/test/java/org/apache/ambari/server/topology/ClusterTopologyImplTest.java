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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.notNull;
import static org.powermock.api.easymock.PowerMock.createNiceMock;
import static org.powermock.api.easymock.PowerMock.createStrictMock;
import static org.powermock.api.easymock.PowerMock.replay;
import static org.powermock.api.easymock.PowerMock.reset;
import static org.powermock.api.easymock.PowerMock.verify;

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
  private final Map<String, HostGroupInfo> hostGroupInfoMap = new HashMap<String, HostGroupInfo>();
  private final Map<String, HostGroup> hostGroupMap = new HashMap<String, HostGroup>();
  private final List<TopologyValidator> topologyValidators = new ArrayList<TopologyValidator>();
  private static Configuration configuration;

  @Before
  public void setUp() throws Exception {

    configuration = new Configuration(new HashMap<String, Map<String, String>>(),
        new HashMap<String, Map<String, Map<String, String>>>());

    HostGroupInfo group1Info = new HostGroupInfo("group1");
    HostGroupInfo group2Info = new HostGroupInfo("group2");
    HostGroupInfo group3Info = new HostGroupInfo("group3");
    HostGroupInfo group4Info = new HostGroupInfo("group4");
    hostGroupInfoMap.put("group1", group1Info);
    hostGroupInfoMap.put("group2", group2Info);
    hostGroupInfoMap.put("group3", group3Info);
    hostGroupInfoMap.put("group4", group4Info);

    group1Info.setConfiguration(configuration);
    Collection<String> group1Hosts = new HashSet<String>();
    group1Hosts.add("host1");
    group1Hosts.add("host2");
    group1Info.addHosts(group1Hosts);

    group2Info.setConfiguration(configuration);
    Collection<String> group2Hosts = new HashSet<String>();
    group2Hosts.add("host3");
    group2Info.addHosts(group2Hosts);

    group3Info.setConfiguration(configuration);
    group3Info.setRequestedCount(5);

    group4Info.setConfiguration(configuration);
    group4Info.setRequestedCount(5);
    group4Info.setPredicate(predicate);

    expect(blueprint.getConfiguration()).andReturn(configuration).anyTimes();

    hostGroupMap.put("group1", group1);
    hostGroupMap.put("group2", group2);
    hostGroupMap.put("group3", group3);
    hostGroupMap.put("group4", group4);

    Set<Component> group1Components = new HashSet<Component>();
    group1Components.add(new Component("component1"));
    group1Components.add(new Component("component2"));

    Set<String> group1ComponentNames = new HashSet<String>();
    group1ComponentNames.add("component1");
    group1ComponentNames.add("component2");

    Set<Component> group2Components = new HashSet<Component>();
    group2Components.add(new Component("component3"));
    Set<Component> group3Components = new HashSet<Component>();
    group3Components.add(new Component("component4"));
    Set<Component> group4Components = new HashSet<Component>();
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
    expect(group4.getComponentNames()).andReturn(Collections.singletonList("component5")).anyTimes();
  }

  @After
  public void tearDown() {
    verify(blueprint, group1, group2, group3, group4);
    reset(blueprint, group1, group2, group3, group4);

    topologyValidators.clear();
    hostGroupInfoMap.clear();
    hostGroupMap.clear();
  }

  private void replayAll() {
    replay(blueprint, group1, group2, group3, group4);
  }

  @Test(expected = InvalidTopologyException.class)
  public void testCreate_validatorFails() throws Exception {
    TestTopologyRequest request = new TestTopologyRequest(TopologyRequest.Type.PROVISION);

    TopologyValidator validator = createStrictMock(TopologyValidator.class);
    topologyValidators.add(validator);

    validator.validate((ClusterTopology) notNull());
    expectLastCall().andThrow(new InvalidTopologyException("test"));

    replayAll();
    replay(validator);
    // should throw exception due to validation failure
    new ClusterTopologyImpl(null, request);
  }

  @Test
     public void testCreate_validatorSuccess() throws Exception {
    TestTopologyRequest request = new TestTopologyRequest(TopologyRequest.Type.PROVISION);

    TopologyValidator validator = createStrictMock(TopologyValidator.class);
    topologyValidators.add(validator);

    validator.validate((ClusterTopology) notNull());

    replayAll();
    replay(validator);

    new ClusterTopologyImpl(null, request);
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

    TopologyValidator validator = createStrictMock(TopologyValidator.class);
    topologyValidators.add(validator);

    validator.validate((ClusterTopology) notNull());

    replayAll();
    replay(validator);

    new ClusterTopologyImpl(null, request).getHostAssignmentsForComponent("component1");
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
      return configuration;
    }

    @Override
    public Map<String, HostGroupInfo> getHostGroupInfo() {
      return hostGroupInfoMap;
    }

    @Override
    public List<TopologyValidator> getTopologyValidators() {
      return topologyValidators;
    }

    @Override
    public String getDescription() {
      return "Test Request";
    }
  }
}