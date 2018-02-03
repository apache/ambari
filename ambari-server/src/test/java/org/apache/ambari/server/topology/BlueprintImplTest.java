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

import static java.util.Collections.emptySet;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.orm.entities.BlueprintEntity;
import org.apache.ambari.server.state.SecurityType;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;


/**
 * Blueprint unit tests.
 */
public class BlueprintImplTest {
  private static final Map<String, Map<String, Map<String, String>>> EMPTY_ATTRIBUTES = new HashMap<>();
  private static final Configuration EMPTY_CONFIGURATION = Configuration.createEmpty();

  Stack stack = createNiceMock(Stack.class);
  HostGroup group1 = createMock(HostGroup.class);
  HostGroup group2 = createMock(HostGroup.class);
  Set<HostGroup> hostGroups = new HashSet<>();
  Set<String> group1Components = new HashSet<>();
  Set<String> group2Components = new HashSet<>();
  Map<String, Map<String, String>> properties = new HashMap<>();
  Map<String, String> hdfsProps = new HashMap<>();
  Configuration configuration = new Configuration(properties, EMPTY_ATTRIBUTES, EMPTY_CONFIGURATION);
  private final org.apache.ambari.server.configuration.Configuration serverConfig = createNiceMock(org.apache.ambari.server.configuration.Configuration.class);
  private final BlueprintValidator blueprintValidator = new BlueprintValidatorImpl(serverConfig);

  @Before
  public void setup() throws NoSuchFieldException, IllegalAccessException {
    properties.put("hdfs-site", hdfsProps);
    hdfsProps.put("foo", "val");
    hdfsProps.put("bar", "val");
    hdfsProps.put("dfs.nameservices", "val");
    Map<String, String> category1Props = new HashMap<>();
    properties.put("category1", category1Props);
    category1Props.put("prop1", "val");

    hostGroups.add(group1);
    hostGroups.add(group2);
    group1Components.add("c1");
    group1Components.add("c2");

    group2Components.add("c1");
    group2Components.add("c3");

    expect(stack.isPasswordProperty("HDFS", "hdfs-site", "foo")).andReturn(false).anyTimes();
    expect(stack.isPasswordProperty("HDFS", "hdfs-site", "bar")).andReturn(false).anyTimes();
    expect(stack.isPasswordProperty("HDFS", "hdfs-site", "some_password")).andReturn(true).anyTimes();
    expect(stack.isPasswordProperty("HDFS", "category1", "prop1")).andReturn(false).anyTimes();
    expect(stack.isPasswordProperty("SERVICE2", "category2", "prop2")).andReturn(false).anyTimes();
    expect(stack.getServiceForComponent("c1")).andReturn("HDFS").anyTimes();
    expect(stack.getServiceForComponent("c2")).andReturn("HDFS").anyTimes();
    expect(stack.getServiceForComponent("c3")).andReturn("SERVICE2").anyTimes();
    expect(group1.getName()).andReturn("group1").anyTimes();
    expect(group2.getName()).andReturn("group2").anyTimes();
    expect(group1.getConfiguration()).andReturn(EMPTY_CONFIGURATION).anyTimes();
    expect(group1.getComponentNames()).andReturn(group1Components).anyTimes();
    expect(group2.getComponentNames()).andReturn(group2Components).anyTimes();

    Collection<Stack.ConfigProperty> requiredHDFSProperties = new HashSet<>();
    requiredHDFSProperties.add(new Stack.ConfigProperty("hdfs-site", "foo", null));
    requiredHDFSProperties.add(new Stack.ConfigProperty("hdfs-site", "bar", null));
    requiredHDFSProperties.add(new Stack.ConfigProperty("hdfs-site", "some_password", null));
    requiredHDFSProperties.add(new Stack.ConfigProperty("category1", "prop1", null));

    Collection<Stack.ConfigProperty> requiredService2Properties = new HashSet<>();
    requiredService2Properties.add(new Stack.ConfigProperty("category2", "prop2", null));
    expect(stack.getRequiredConfigurationProperties("HDFS")).andReturn(requiredHDFSProperties).anyTimes();
    expect(stack.getRequiredConfigurationProperties("SERVICE2")).andReturn(requiredService2Properties).anyTimes();

    setupConfigurationWithGPLLicense(true);
  }

  @Test
  public void testValidateConfigurations__basic_positive() throws Exception {
    expect(group1.getCardinality()).andReturn("1").atLeastOnce();
    expect(group1.getComponents()).andReturn(Arrays.asList(new Component("c1"), new Component("c2"))).atLeastOnce();
    expect(group2.getCardinality()).andReturn("1").atLeastOnce();
    expect(group2.getComponents()).andReturn(Arrays.asList(new Component("c1"), new Component("c3"))).atLeastOnce();
    expect(group2.getConfiguration()).andReturn(EMPTY_CONFIGURATION).atLeastOnce();

    replay(stack, group1, group2, serverConfig);

    Map<String, String> category2Props = new HashMap<>();
    properties.put("category2", category2Props);
    category2Props.put("prop2", "val");

    SecurityConfiguration securityConfiguration = new SecurityConfiguration(SecurityType.KERBEROS, "testRef", null);
    Blueprint blueprint = new BlueprintImpl("test", hostGroups, stack, emptySet(), emptySet(), configuration, securityConfiguration, null);
    blueprintValidator.validateRequiredProperties(blueprint);
    BlueprintEntity entity = blueprint.toEntity();

    verify(stack, group1, group2, serverConfig);
    assertTrue(entity.getSecurityType() == SecurityType.KERBEROS);
    assertTrue(entity.getSecurityDescriptorReference().equals("testRef"));
  }

  @Test
  public void testValidateConfigurations__hostGroupConfig() throws Exception {
    Map<String, Map<String, String>> group2Props = new HashMap<>();
    Map<String, String> group2Category2Props = new HashMap<>();
    group2Props.put("category2", group2Category2Props);
    group2Category2Props.put("prop2", "val");

    // set config for group2 which contains a required property
    Configuration group2Configuration = new Configuration(group2Props, EMPTY_ATTRIBUTES, configuration);
    expect(group2.getConfiguration()).andReturn(group2Configuration).atLeastOnce();

    expect(group1.getCardinality()).andReturn("1").atLeastOnce();
    expect(group1.getComponents()).andReturn(Arrays.asList(new Component("NAMENODE"))).atLeastOnce();
    expect(group2.getCardinality()).andReturn("1").atLeastOnce();
    expect(group2.getComponents()).andReturn(Arrays.asList(new Component("NAMENODE"))).atLeastOnce();
    Map<String, String> category2Props = new HashMap<>();
    properties.put("category2", category2Props);
    category2Props.put("prop2", "val");
    group1Components.add("NAMENODE");
    group2Components.add("NAMENODE");
    Map<String, String> hdfsProps = new HashMap<>();
    properties.put("hdfs-site", hdfsProps);
    hdfsProps.put("foo", "val");
    hdfsProps.put("bar", "val");
    Map<String, String> hadoopProps = new HashMap<>();
    properties.put("hadoop-env", hadoopProps);
    hadoopProps.put("dfs_ha_initial_namenode_active", "%HOSTGROUP:group1%");
    hadoopProps.put("dfs_ha_initial_namenode_standby", "%HOSTGROUP:group2%");
    replay(stack, group1, group2, serverConfig);
    Blueprint blueprint = new BlueprintImpl("test", hostGroups, stack, emptySet(), emptySet(), configuration, null, null);
    blueprintValidator.validateRequiredProperties(blueprint);
    BlueprintEntity entity = blueprint.toEntity();
    verify(stack, group1, group2, serverConfig);
    assertTrue(entity.getSecurityType() == SecurityType.NONE);
    assertTrue(entity.getSecurityDescriptorReference() == null);
  }
  @Test
  public void testValidateConfigurations__hostGroupConfigForNameNodeHAPositive() throws Exception {
    Map<String, Map<String, String>> group2Props = new HashMap<>();
    Map<String, String> group2Category2Props = new HashMap<>();
    group2Props.put("category2", group2Category2Props);
    group2Category2Props.put("prop2", "val");
    // set config for group2 which contains a required property
    Configuration group2Configuration = new Configuration(group2Props, EMPTY_ATTRIBUTES, configuration);
    expect(group2.getConfiguration()).andReturn(group2Configuration).atLeastOnce();


    expect(group1.getCardinality()).andReturn("1").atLeastOnce();
    expect(group1.getComponents()).andReturn(Arrays.asList(new Component("NAMENODE"),new Component("ZKFC"))).atLeastOnce();
    expect(group2.getCardinality()).andReturn("1").atLeastOnce();
    expect(group2.getComponents()).andReturn(Arrays.asList(new Component("NAMENODE"),new Component("ZKFC"))).atLeastOnce();
    Map<String, String> category2Props = new HashMap<>();
    properties.put("category2", category2Props);
    category2Props.put("prop2", "val");
    group1Components.add("NAMENODE");
    group1Components.add("ZKFC");
    group2Components.add("NAMENODE");
    group2Components.add("ZKFC");
    Map<String, String> hdfsProps = new HashMap<>();
    properties.put("hdfs-site", hdfsProps);
    hdfsProps.put("foo", "val");
    hdfsProps.put("bar", "val");
    hdfsProps.put("dfs.nameservices", "val");
    Map<String, String> hadoopProps = new HashMap<>();
    properties.put("hadoop-env", hadoopProps);
    hadoopProps.put("dfs_ha_initial_namenode_active", "%HOSTGROUP::group1%");
    hadoopProps.put("dfs_ha_initial_namenode_standby", "%HOSTGROUP::group2%");
    replay(stack, group1, group2, serverConfig);

    Blueprint blueprint = new BlueprintImpl("test", hostGroups, stack, emptySet(), emptySet(), configuration, null, null);
    blueprintValidator.validateRequiredProperties(blueprint);
    BlueprintEntity entity = blueprint.toEntity();

    verify(stack, group1, group2, serverConfig);
    assertTrue(entity.getSecurityType() == SecurityType.NONE);
    assertTrue(entity.getSecurityDescriptorReference() == null);
  }

  @Test(expected= IllegalArgumentException.class)
  public void testValidateConfigurations__hostGroupConfigForNameNodeHAInCorrectHostGroups() throws Exception {
    Map<String, Map<String, String>> group2Props = new HashMap<>();
    Map<String, String> group2Category2Props = new HashMap<>();
    group2Props.put("category2", group2Category2Props);
    group2Category2Props.put("prop2", "val");
    // set config for group2 which contains a required property
    Configuration group2Configuration = new Configuration(group2Props, EMPTY_ATTRIBUTES, configuration);
    expect(group2.getConfiguration()).andReturn(group2Configuration).atLeastOnce();
    expect(group1.getCardinality()).andReturn("1").atLeastOnce();
    expect(group1.getComponents()).andReturn(Arrays.asList(new Component("NAMENODE"),new Component("ZKFC"))).atLeastOnce();
    expect(group2.getCardinality()).andReturn("1").atLeastOnce();
    expect(group2.getComponents()).andReturn(Arrays.asList(new Component("NAMENODE"),new Component("ZKFC"))).atLeastOnce();
    Map<String, String> category2Props = new HashMap<>();
    properties.put("category2", category2Props);
    category2Props.put("prop2", "val");
    group1Components.add("NAMENODE");
    group1Components.add("ZKFC");
    group2Components.add("NAMENODE");
    group2Components.add("ZKFC");
    expect(stack.getServiceForComponent("NAMENODE")).andReturn("SERVICE2").atLeastOnce();
    expect(stack.getServiceForComponent("ZKFC")).andReturn("SERVICE2").atLeastOnce();
    Map<String, String> hdfsProps = new HashMap<>();
    properties.put("hdfs-site", hdfsProps);
    hdfsProps.put("foo", "val");
    hdfsProps.put("bar", "val");
    hdfsProps.put("dfs.nameservices", "val");
    Map<String, String> hadoopProps = new HashMap<>();
    properties.put("hadoop-env", hadoopProps);
    hadoopProps.put("dfs_ha_initial_namenode_active", "%HOSTGROUP::group2%");
    hadoopProps.put("dfs_ha_initial_namenode_standby", "%HOSTGROUP::group3%");
    replay(stack, group1, group2, serverConfig);
    Blueprint blueprint = new BlueprintImpl("test", hostGroups, stack, emptySet(), emptySet(), configuration, null, null);
    blueprintValidator.validateRequiredProperties(blueprint);
    verify(stack, group1, group2, serverConfig);
  }
  @Test(expected= IllegalArgumentException.class)
  public void testValidateConfigurations__hostGroupConfigForNameNodeHAMappedSameHostGroup() throws Exception {
    Map<String, Map<String, String>> group2Props = new HashMap<>();
    Map<String, String> group2Category2Props = new HashMap<>();
    group2Props.put("category2", group2Category2Props);
    group2Category2Props.put("prop2", "val");
    // set config for group2 which contains a required property
    Configuration group2Configuration = new Configuration(group2Props, EMPTY_ATTRIBUTES, configuration);
    expect(group2.getConfiguration()).andReturn(group2Configuration).atLeastOnce();
    expect(group1.getCardinality()).andReturn("1").atLeastOnce();
    expect(group1.getComponents()).andReturn(Arrays.asList(new Component("NAMENODE"),new Component("ZKFC"))).atLeastOnce();
    expect(group2.getCardinality()).andReturn("1").atLeastOnce();
    expect(group2.getComponents()).andReturn(Arrays.asList(new Component("NAMENODE"),new Component("ZKFC"))).atLeastOnce();
    Map<String, String> category2Props = new HashMap<>();
    properties.put("category2", category2Props);
    category2Props.put("prop2", "val");
    group1Components.add("NAMENODE");
    group1Components.add("ZKFC");
    group2Components.add("NAMENODE");
    group2Components.add("ZKFC");
    expect(stack.getServiceForComponent("NAMENODE")).andReturn("SERVICE2").atLeastOnce();
    expect(stack.getServiceForComponent("ZKFC")).andReturn("SERVICE2").atLeastOnce();
    Map<String, String> hdfsProps = new HashMap<>();
    properties.put("hdfs-site", hdfsProps);
    hdfsProps.put("foo", "val");
    hdfsProps.put("bar", "val");
    hdfsProps.put("dfs.nameservices", "val");
    Map<String, String> hadoopProps = new HashMap<>();
    properties.put("hadoop-env", hadoopProps);
    hadoopProps.put("dfs_ha_initial_namenode_active", "%HOSTGROUP::group2%");
    hadoopProps.put("dfs_ha_initial_namenode_standby", "%HOSTGROUP::group2%");
    replay(stack, group1, group2, serverConfig);
    Blueprint blueprint = new BlueprintImpl("test", hostGroups, stack, emptySet(), emptySet(), configuration, null, null);
    blueprintValidator.validateRequiredProperties(blueprint);
    verify(stack, group1, group2, serverConfig);
  }
  @Test(expected = InvalidTopologyException.class)
  public void testValidateConfigurations__secretReference() throws InvalidTopologyException,
      GPLLicenseNotAcceptedException, NoSuchFieldException, IllegalAccessException {
    Map<String, Map<String, String>> group2Props = new HashMap<>();
    Map<String, String> group2Category2Props = new HashMap<>();

    group2Props.put("category2", group2Category2Props);
    group2Category2Props.put("prop2", "val");
    hdfsProps.put("secret", "SECRET:hdfs-site:1:test");
    replay(stack, group1, group2, serverConfig);

    Blueprint blueprint = new BlueprintImpl("test", hostGroups, stack, emptySet(), emptySet(), configuration, null, null);
    blueprintValidator.validateRequiredProperties(blueprint);
    verify(stack, group1, group2, serverConfig);
  }

  @Test(expected = GPLLicenseNotAcceptedException.class)
  public void testValidateConfigurations__gplIsNotAllowedCodecsProperty() throws InvalidTopologyException,
      GPLLicenseNotAcceptedException, NoSuchFieldException, IllegalAccessException {
    Map<String, Map<String, String>> lzoProperties = new HashMap<>();
    lzoProperties.put("core-site", ImmutableMap.of(BlueprintValidatorImpl.CODEC_CLASSES_PROPERTY_NAME, "OtherCodec, " + BlueprintValidatorImpl.LZO_CODEC_CLASS));
    Configuration lzoUsageConfiguration = new Configuration(lzoProperties, EMPTY_ATTRIBUTES, EMPTY_CONFIGURATION);

    setupConfigurationWithGPLLicense(false);
    replay(stack, group1, group2, serverConfig);

    Blueprint blueprint = new BlueprintImpl("test", hostGroups, stack, emptySet(), emptySet(), lzoUsageConfiguration, null, null);
    blueprintValidator.validateRequiredProperties(blueprint);
    verify(stack, group1, group2, serverConfig);
  }

  @Test(expected = GPLLicenseNotAcceptedException.class)
  public void testValidateConfigurations__gplIsNotAllowedLZOProperty() throws InvalidTopologyException,
      GPLLicenseNotAcceptedException, NoSuchFieldException, IllegalAccessException {
    Map<String, Map<String, String>> lzoProperties = new HashMap<>();
    lzoProperties.put("core-site", ImmutableMap.of(BlueprintValidatorImpl.LZO_CODEC_CLASS_PROPERTY_NAME, BlueprintValidatorImpl.LZO_CODEC_CLASS));
    Configuration lzoUsageConfiguration = new Configuration(lzoProperties, EMPTY_ATTRIBUTES, EMPTY_CONFIGURATION);

    setupConfigurationWithGPLLicense(false);
    replay(stack, group1, group2, serverConfig);

    Blueprint blueprint = new BlueprintImpl("test", hostGroups, stack, emptySet(), emptySet(), lzoUsageConfiguration, null, null);
    blueprintValidator.validateRequiredProperties(blueprint);
    verify(stack, group1, group2, serverConfig);
  }

  @Test
  public void testValidateConfigurations__gplISAllowed() throws InvalidTopologyException,
      GPLLicenseNotAcceptedException, NoSuchFieldException, IllegalAccessException {
    Map<String, Map<String, String>> lzoProperties = new HashMap<>();
    lzoProperties.put("core-site", ImmutableMap.of(
      BlueprintValidatorImpl.LZO_CODEC_CLASS_PROPERTY_NAME, BlueprintValidatorImpl.LZO_CODEC_CLASS,
      BlueprintValidatorImpl.CODEC_CLASSES_PROPERTY_NAME, "OtherCodec, " + BlueprintValidatorImpl.LZO_CODEC_CLASS));
    Configuration lzoUsageConfiguration = new Configuration(lzoProperties, EMPTY_ATTRIBUTES, EMPTY_CONFIGURATION);

    expect(group2.getConfiguration()).andReturn(EMPTY_CONFIGURATION).atLeastOnce();
    replay(stack, group1, group2, serverConfig);

    Blueprint blueprint = new BlueprintImpl("test", hostGroups, stack, emptySet(), emptySet(), lzoUsageConfiguration, null, null);
    blueprintValidator.validateRequiredProperties(blueprint);
    verify(stack, group1, group2, serverConfig);
  }

  private void setupConfigurationWithGPLLicense(boolean isGPLAllowed) {
    reset(serverConfig);
    expect(serverConfig.getGplLicenseAccepted()).andReturn(isGPLAllowed).atLeastOnce();
  }

}
