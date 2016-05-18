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
import org.apache.ambari.server.state.PropertyInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.powermock.api.easymock.PowerMock.createNiceMock;
import static org.powermock.api.easymock.PowerMock.verify;

/**
 * Unit tests for RequiredPasswordValidator.
 */
public class RequiredPasswordValidatorTest {

  private static final ClusterTopology topology = createNiceMock(ClusterTopology.class);
  private static final Blueprint blueprint = createNiceMock(Blueprint.class);
  private static final Stack stack = createNiceMock(Stack.class);
  private static final HostGroup group1 = createNiceMock(HostGroup.class);
  private static final HostGroup group2 = createNiceMock(HostGroup.class);

  private static Configuration stackDefaults;
  private static Configuration bpClusterConfig;
  private static Configuration topoClusterConfig;
  private static Configuration bpGroup1Config;
  private static Configuration bpGroup2Config;
  private static Configuration topoGroup1Config;
  private static Configuration topoGroup2Config;

  private static final Map<String, HostGroup> hostGroups = new HashMap<String, HostGroup>();
  private static final Map<String, HostGroupInfo> hostGroupInfo = new HashMap<String, HostGroupInfo>();

  private static final Collection<String> group1Components = new HashSet<String>();
  private static final Collection<String> group2Components = new HashSet<String>();
  private static final Collection<String> service1Components = new HashSet<String>();
  private static final Collection<String> service2Components = new HashSet<String>();
  private static final Collection<String> service3Components = new HashSet<String>();

  private static final Collection<Stack.ConfigProperty> service1RequiredPwdConfigs = new HashSet<Stack.ConfigProperty>();
  private static final Collection<Stack.ConfigProperty> service2RequiredPwdConfigs = new HashSet<Stack.ConfigProperty>();
  private static final Collection<Stack.ConfigProperty> service3RequiredPwdConfigs = new HashSet<Stack.ConfigProperty>();


  @Before
  public void setup() {

    stackDefaults = new Configuration(new HashMap<String, Map<String, String>>(),
        new HashMap<String, Map<String, Map<String, String>>>());

    bpClusterConfig = new Configuration(new HashMap<String, Map<String, String>>(),
        new HashMap<String, Map<String, Map<String, String>>>(), stackDefaults);

    topoClusterConfig = new Configuration(new HashMap<String, Map<String, String>>(),
        new HashMap<String, Map<String, Map<String, String>>>(), bpClusterConfig);

    bpGroup1Config = new Configuration(new HashMap<String, Map<String, String>>(),
        new HashMap<String, Map<String, Map<String, String>>>(), topoClusterConfig);

    bpGroup2Config = new Configuration(new HashMap<String, Map<String, String>>(),
        new HashMap<String, Map<String, Map<String, String>>>(), topoClusterConfig);

    topoGroup1Config = new Configuration(new HashMap<String, Map<String, String>>(),
        new HashMap<String, Map<String, Map<String, String>>>(), bpGroup1Config);

    topoGroup2Config = new Configuration(new HashMap<String, Map<String, String>>(),
        new HashMap<String, Map<String, Map<String, String>>>(), bpGroup2Config);

    service1RequiredPwdConfigs.clear();
    service2RequiredPwdConfigs.clear();
    service3RequiredPwdConfigs.clear();

    hostGroups.put("group1", group1);
    hostGroups.put("group2", group2);

    group1Components.add("component1");
    group1Components.add("component2");
    group1Components.add("component3");

    group2Components.add("component1");
    group2Components.add("component4");

    service1Components.add("component1");
    service1Components.add("component2");
    service2Components.add("component3");
    service3Components.add("component4");

    HostGroupInfo hostGroup1Info = new HostGroupInfo("group1");
    hostGroup1Info.setConfiguration(topoGroup1Config);
    HostGroupInfo hostGroup2Info = new HostGroupInfo("group2");
    hostGroup2Info.setConfiguration(topoGroup2Config);
    hostGroupInfo.put("group1", hostGroup1Info);
    hostGroupInfo.put("group2", hostGroup2Info);

    expect(topology.getConfiguration()).andReturn(topoClusterConfig).anyTimes();
    expect(topology.getBlueprint()).andReturn(blueprint).anyTimes();
    expect(topology.getHostGroupInfo()).andReturn(hostGroupInfo).anyTimes();

    expect(blueprint.getHostGroups()).andReturn(hostGroups).anyTimes();
    expect(blueprint.getHostGroup("group1")).andReturn(group1).anyTimes();
    expect(blueprint.getHostGroup("group2")).andReturn(group2).anyTimes();
    expect(blueprint.getStack()).andReturn(stack).anyTimes();

    expect(group1.getComponentNames()).andReturn(group1Components).anyTimes();
    expect(group2.getComponentNames()).andReturn(group2Components).anyTimes();
    expect(group1.getComponents("service1")).andReturn(Arrays.asList("component1", "component2")).anyTimes();
    expect(group1.getComponents("service2")).andReturn(Arrays.asList("component3")).anyTimes();
    expect(group1.getComponents("service3")).andReturn(Collections.<String>emptySet()).anyTimes();
    expect(group2.getComponents("service1")).andReturn(Arrays.asList("component1")).anyTimes();
    expect(group2.getComponents("service2")).andReturn(Collections.<String>emptySet()).anyTimes();
    expect(group2.getComponents("service3")).andReturn(Arrays.asList("component4")).anyTimes();

    expect(stack.getServiceForComponent("component1")).andReturn("service1").anyTimes();
    expect(stack.getServiceForComponent("component2")).andReturn("service1").anyTimes();
    expect(stack.getServiceForComponent("component3")).andReturn("service2").anyTimes();
    expect(stack.getServiceForComponent("component4")).andReturn("service3").anyTimes();

    expect(stack.getRequiredConfigurationProperties("service1", PropertyInfo.PropertyType.PASSWORD)).andReturn(service1RequiredPwdConfigs).anyTimes();
    expect(stack.getRequiredConfigurationProperties("service2", PropertyInfo.PropertyType.PASSWORD)).andReturn(service2RequiredPwdConfigs).anyTimes();
    expect(stack.getRequiredConfigurationProperties("service3", PropertyInfo.PropertyType.PASSWORD)).andReturn(service3RequiredPwdConfigs).anyTimes();

    replay(topology, blueprint, stack, group1, group2);
  }

  @After
  public void tearDown() {
    verify(topology, blueprint, stack, group1, group2);
    reset(topology, blueprint, stack, group1, group2);
  }


  @Test
  public void testValidate_noRequiredProps__noDefaultPwd() throws Exception {
    TopologyValidator validator = new RequiredPasswordValidator(null);
    // no required pwd properties so shouldn't throw an exception
    validator.validate(topology);
  }

  @Test
  public void testValidate_noRequiredProps__defaultPwd() throws Exception {
    TopologyValidator validator = new RequiredPasswordValidator("pwd");
    // no required pwd properties so shouldn't throw an exception
    validator.validate(topology);
  }

  @Test(expected = InvalidTopologyException.class)
  public void testValidate_missingPwd__NoDefaultPwd() throws Exception {
    Stack.ConfigProperty pwdProp = new Stack.ConfigProperty("test-type", "pwdProp", null);
    service1RequiredPwdConfigs.add(pwdProp);

    TopologyValidator validator = new RequiredPasswordValidator(null);
    validator.validate(topology);
  }

  @Test
  public void testValidate_missingPwd__defaultPwd() throws Exception {
    Stack.ConfigProperty pwdProp = new Stack.ConfigProperty("test-type", "pwdProp", null);
    service1RequiredPwdConfigs.add(pwdProp);

    TopologyValidator validator = new RequiredPasswordValidator("default-pwd");
    // default value should be set
    validator.validate(topology);

    assertEquals(1, topoClusterConfig.getProperties().size());
    assertEquals("default-pwd", topoClusterConfig.getProperties().get("test-type").get("pwdProp"));
  }

  @Test
  public void testValidate_pwdPropertyInTopoGroupConfig__NoDefaultPwd() throws Exception {
    Stack.ConfigProperty pwdProp = new Stack.ConfigProperty("test-type", "pwdProp", null);
    service3RequiredPwdConfigs.add(pwdProp);
    // group2 has a component from service 3
    topoGroup2Config.getProperties().put("test-type", Collections.singletonMap("pwdProp", "secret"));

    TopologyValidator validator = new RequiredPasswordValidator(null);
    validator.validate(topology);
  }

  @Test
  public void testValidate_pwdPropertyInTopoClusterConfig__NoDefaultPwd() throws Exception {
    Stack.ConfigProperty pwdProp = new Stack.ConfigProperty("test-type", "pwdProp", null);
    service3RequiredPwdConfigs.add(pwdProp);
    // group2 has a component from service 3
    topoClusterConfig.getProperties().put("test-type", Collections.singletonMap("pwdProp", "secret"));

    TopologyValidator validator = new RequiredPasswordValidator(null);
    validator.validate(topology);
  }

  @Test
  public void testValidate_pwdPropertyInBPGroupConfig__NoDefaultPwd() throws Exception {
    Stack.ConfigProperty pwdProp = new Stack.ConfigProperty("test-type", "pwdProp", null);
    service3RequiredPwdConfigs.add(pwdProp);
    // group2 has a component from service 3
    bpGroup2Config.getProperties().put("test-type", Collections.singletonMap("pwdProp", "secret"));

    TopologyValidator validator = new RequiredPasswordValidator(null);
    validator.validate(topology);
  }

  @Test
  public void testValidate_pwdPropertyInBPClusterConfig__NoDefaultPwd() throws Exception {
    Stack.ConfigProperty pwdProp = new Stack.ConfigProperty("test-type", "pwdProp", null);
    service3RequiredPwdConfigs.add(pwdProp);
    // group2 has a component from service 3
    bpClusterConfig.getProperties().put("test-type", Collections.singletonMap("pwdProp", "secret"));

    TopologyValidator validator = new RequiredPasswordValidator(null);
    validator.validate(topology);
  }

  @Test(expected = InvalidTopologyException.class)
  public void testValidate_pwdPropertyInStackConfig__NoDefaultPwd() throws Exception {
    Stack.ConfigProperty pwdProp = new Stack.ConfigProperty("test-type", "pwdProp", null);
    service3RequiredPwdConfigs.add(pwdProp);
    // group2 has a component from service 3
    stackDefaults.getProperties().put("test-type", Collections.singletonMap("pwdProp", "secret"));

    TopologyValidator validator = new RequiredPasswordValidator(null);
    // because stack config is ignored for validation, an exception should be thrown
    validator.validate(topology);
  }

  @Test
  public void testValidate_twoRequiredPwdOneSpecified__defaultPwd() throws Exception {
    Stack.ConfigProperty pwdProp = new Stack.ConfigProperty("test-type", "pwdProp", null);
    Stack.ConfigProperty pwdProp2 = new Stack.ConfigProperty("test2-type", "pwdProp2", null);
    service1RequiredPwdConfigs.add(pwdProp);
    service3RequiredPwdConfigs.add(pwdProp2);

    topoClusterConfig.getProperties().put("test2-type", Collections.singletonMap("pwdProp2", "secret"));

    TopologyValidator validator = new RequiredPasswordValidator("default-pwd");
    // default value should be set
    validator.validate(topology);

    assertEquals(2, topoClusterConfig.getProperties().size());
    assertEquals("default-pwd", topoClusterConfig.getProperties().get("test-type").get("pwdProp"));
    assertEquals("secret", topoClusterConfig.getProperties().get("test2-type").get("pwdProp2"));
  }

  @Test
  public void testValidate_twoRequiredPwdTwoSpecified__noDefaultPwd() throws Exception {
    Stack.ConfigProperty pwdProp = new Stack.ConfigProperty("test-type", "pwdProp", null);
    Stack.ConfigProperty pwdProp2 = new Stack.ConfigProperty("test2-type", "pwdProp2", null);
    service1RequiredPwdConfigs.add(pwdProp);
    service3RequiredPwdConfigs.add(pwdProp2);

    topoClusterConfig.getProperties().put("test2-type", Collections.singletonMap("pwdProp2", "secret2"));
    topoClusterConfig.getProperties().put("test-type", Collections.singletonMap("pwdProp", "secret1"));

    TopologyValidator validator = new RequiredPasswordValidator(null);
    // default value should be set
    validator.validate(topology);

    assertEquals(2, topoClusterConfig.getProperties().size());
    assertEquals("secret1", topoClusterConfig.getProperties().get("test-type").get("pwdProp"));
    assertEquals("secret2", topoClusterConfig.getProperties().get("test2-type").get("pwdProp2"));
  }

  @Test
  public void testValidate_multipleMissingPwd__defaultPwd() throws Exception {
    Stack.ConfigProperty pwdProp = new Stack.ConfigProperty("test-type", "pwdProp", null);
    Stack.ConfigProperty pwdProp2 = new Stack.ConfigProperty("test2-type", "pwdProp2", null);
    service1RequiredPwdConfigs.add(pwdProp);
    service3RequiredPwdConfigs.add(pwdProp2);

    TopologyValidator validator = new RequiredPasswordValidator("default-pwd");
    // default value should be set
    validator.validate(topology);

    assertEquals(2, topoClusterConfig.getProperties().size());
    assertEquals("default-pwd", topoClusterConfig.getProperties().get("test-type").get("pwdProp"));
    assertEquals("default-pwd", topoClusterConfig.getProperties().get("test2-type").get("pwdProp2"));
  }

}
