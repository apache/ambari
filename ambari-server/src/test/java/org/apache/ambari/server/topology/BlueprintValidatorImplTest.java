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

import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.state.AutoDeployInfo;
import org.apache.ambari.server.state.DependencyInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

/**
 * BlueprintValidatorImpl unit tests.
 */
public class BlueprintValidatorImplTest{

  private final Blueprint blueprint = createNiceMock(Blueprint.class);
  private final Stack stack = createNiceMock(Stack.class);
  private final HostGroup group1 = createNiceMock(HostGroup.class);
  private final HostGroup group2 = createNiceMock(HostGroup.class);
  private final Map<String, HostGroup> hostGroups = new LinkedHashMap<>();

  private final Collection<String> group1Components = new ArrayList<String>();
  private final Collection<String> group2Components = new ArrayList<String>();
  private final Collection<String> services = new ArrayList<String>();
  private final DependencyInfo dependency1 = createNiceMock(DependencyInfo.class);
  private Collection<DependencyInfo> dependencies1 = new ArrayList<DependencyInfo>();

  private AutoDeployInfo autoDeploy = new AutoDeployInfo();

  Map<String, Map<String, String>> configProperties = new HashMap<String, Map<String, String>>();
  private Configuration configuration = new Configuration(configProperties,
      Collections.<String, Map<String, Map<String, String>>>emptyMap());


  @Before
  public void setup() {
    hostGroups.put("group1", group1);
    hostGroups.put("group2", group2);

    autoDeploy.setEnabled(true);
    autoDeploy.setCoLocate("service1/component2");

    expect(blueprint.getStack()).andReturn(stack).anyTimes();
    expect(blueprint.getHostGroups()).andReturn(hostGroups).anyTimes();
    expect(blueprint.getServices()).andReturn(services).anyTimes();

    expect(group1.getComponentNames()).andReturn(group1Components).anyTimes();
    expect(group2.getComponentNames()).andReturn(group2Components).anyTimes();

    expect(stack.getDependenciesForComponent("component1")).andReturn(dependencies1).anyTimes();
    expect(stack.getDependenciesForComponent("component2")).andReturn(dependencies1).anyTimes();
    expect(stack.getDependenciesForComponent("component3")).andReturn(dependencies1).anyTimes();
    expect(stack.getDependenciesForComponent("component4")).andReturn(dependencies1).anyTimes();

    expect(stack.getCardinality("component1")).andReturn(new Cardinality("1"));
    expect(stack.getCardinality("component2")).andReturn(new Cardinality("1+"));
    expect(stack.getCardinality("component3")).andReturn(new Cardinality("1+"));

    expect(blueprint.getConfiguration()).andReturn(configuration).anyTimes();
  }

  @After
  public void tearDown() {
    reset(blueprint, stack, group1, group2, dependency1);
  }

  @Test
  public void testValidateTopology_basic() throws Exception {
    group1Components.add("component1");
    group1Components.add("component1");

    services.addAll(Arrays.asList("service1", "service2"));

    expect(stack.getComponents("service1")).andReturn(Collections.singleton("component1")).anyTimes();
    expect(stack.getComponents("service2")).andReturn(Collections.singleton("component2")).anyTimes();

    expect(blueprint.getHostGroupsForComponent("component1")).andReturn(Collections.singleton(group1)).anyTimes();
    expect(blueprint.getHostGroupsForComponent("component2")).andReturn(Arrays.asList(group1, group2)).anyTimes();

    replay(blueprint, stack, group1, group2, dependency1);
    BlueprintValidator validator = new BlueprintValidatorImpl(blueprint);
    validator.validateTopology();
  }

  @Test(expected = InvalidTopologyException.class)
  public void testValidateTopology_basic_negative() throws Exception {
    group1Components.add("component2");

    services.addAll(Collections.singleton("service1"));

    expect(stack.getComponents("service1")).andReturn(Arrays.asList("component1", "component2")).anyTimes();

    expect(blueprint.getHostGroupsForComponent("component1")).andReturn(Collections.<HostGroup>emptyList()).anyTimes();
    expect(blueprint.getHostGroupsForComponent("component2")).andReturn(Arrays.asList(group1, group2)).anyTimes();

    replay(blueprint, stack, group1, group2, dependency1);
    BlueprintValidator validator = new BlueprintValidatorImpl(blueprint);
    validator.validateTopology();
  }

  @Test
  public void testValidateTopology_autoDeploy() throws Exception {
    group1Components.add("component2");
    services.addAll(Collections.singleton("service1"));

    expect(blueprint.getHostGroupsForComponent("component1")).andReturn(Collections.<HostGroup>emptyList()).anyTimes();
    expect(blueprint.getHostGroupsForComponent("component2")).andReturn(Arrays.asList(group1, group2)).anyTimes();

    expect(stack.getComponents("service1")).andReturn(Arrays.asList("component1", "component2")).anyTimes();
    expect(stack.getAutoDeployInfo("component1")).andReturn(autoDeploy).anyTimes();

    expect(group1.addComponent("component1")).andReturn(true).once();

    replay(blueprint, stack, group1, group2, dependency1);
    BlueprintValidator validator = new BlueprintValidatorImpl(blueprint);
    validator.validateTopology();

    verify(group1);
  }

  @Test
  public void testValidateTopology_autoDeploy_hasDependency() throws Exception {
    group1Components.add("component2");
    dependencies1.add(dependency1);
    services.addAll(Collections.singleton("service1"));

    expect(blueprint.getHostGroupsForComponent("component1")).andReturn(Collections.<HostGroup>emptyList()).anyTimes();
    expect(blueprint.getHostGroupsForComponent("component2")).andReturn(Arrays.asList(group1, group2)).anyTimes();
    expect(blueprint.getHostGroupsForComponent("component3")).andReturn(Collections.<HostGroup>emptyList()).anyTimes();

    expect(stack.getComponents("service1")).andReturn(Arrays.asList("component1", "component2")).anyTimes();
    expect(stack.getComponents("service2")).andReturn(Collections.singleton("component3")).anyTimes();
    expect(stack.getAutoDeployInfo("component1")).andReturn(autoDeploy).anyTimes();

    expect(dependency1.getScope()).andReturn("host").anyTimes();
    AutoDeployInfo dependencyAutoDeploy = new AutoDeployInfo();
    dependencyAutoDeploy.setEnabled(true);
    dependencyAutoDeploy.setCoLocate("service1/component1");
    expect(dependency1.getAutoDeploy()).andReturn(dependencyAutoDeploy).anyTimes();
    expect(dependency1.getComponentName()).andReturn("component3").anyTimes();

    expect(group1.addComponent("component1")).andReturn(true).once();
    expect(group1.addComponent("component3")).andReturn(true).once();

    replay(blueprint, stack, group1, group2, dependency1);
    BlueprintValidator validator = new BlueprintValidatorImpl(blueprint);
    validator.validateTopology();

    verify(group1);
  }

  @Test(expected=InvalidTopologyException.class)
  public void testValidateRequiredProperties_SqlaInHiveStackHdp22() throws Exception {
    Map<String, String> hiveEnvConfig = new HashMap<String, String>();
    hiveEnvConfig.put("hive_database","Existing SQL Anywhere Database");
    configProperties.put("hive-env", hiveEnvConfig);

    group1Components.add("HIVE_METASTORE");

    services.addAll(Arrays.asList("HIVE"));

    expect(group1.getConfiguration()).andReturn(new Configuration(new HashMap(), new HashMap())).anyTimes();

    expect(stack.getComponents("HIVE")).andReturn(Collections.singleton("HIVE_METASTORE")).anyTimes();
    expect(stack.getVersion()).andReturn("2.2").once();
    expect(stack.getName()).andReturn("HDP").once();

    expect(blueprint.getHostGroupsForComponent("HIVE_METASTORE")).andReturn(Collections.singleton(group1)).anyTimes();

    replay(blueprint, stack, group1, group2, dependency1);
    BlueprintValidator validator = new BlueprintValidatorImpl(blueprint);
    validator.validateRequiredProperties();
  }

  @Test(expected=InvalidTopologyException.class)
  public void testValidateRequiredProperties_SqlaInOozieStackHdp22() throws Exception {
    Map<String, String> hiveEnvConfig = new HashMap<String, String>();
    hiveEnvConfig.put("oozie_database","Existing SQL Anywhere Database");
    configProperties.put("oozie-env", hiveEnvConfig);

    group1Components.add("OOZIE_SERVER");

    services.addAll(Arrays.asList("OOZIE"));

    expect(group1.getConfiguration()).andReturn(new Configuration(new HashMap(), new HashMap())).anyTimes();

    expect(stack.getComponents("OOZIE")).andReturn(Collections.singleton("OOZIE_SERVER")).anyTimes();
    expect(stack.getVersion()).andReturn("2.2").once();
    expect(stack.getName()).andReturn("HDP").once();

    expect(blueprint.getHostGroupsForComponent("OOZIE_SERVER")).andReturn(Collections.singleton(group1)).anyTimes();

    replay(blueprint, stack, group1, group2, dependency1);
    BlueprintValidator validator = new BlueprintValidatorImpl(blueprint);
    validator.validateRequiredProperties();
  }
}
