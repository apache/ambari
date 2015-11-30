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

package org.apache.ambari.server.api.services.stackadvisor;

import com.google.common.collect.Maps;
import org.apache.ambari.server.controller.internal.ConfigurationTopologyException;
import org.apache.ambari.server.topology.Configuration;
import org.apache.ambari.server.topology.HostGroup;
import org.apache.ambari.server.topology.HostGroupImpl;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.ambari.server.api.services.stackadvisor.recommendations.RecommendationResponse;
import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.state.ValueAttributesInfo;
import org.apache.ambari.server.topology.AdvisedConfiguration;
import org.apache.ambari.server.topology.BlueprintImpl;
import org.apache.ambari.server.topology.HostGroupInfo;

import org.apache.ambari.server.topology.ClusterTopology;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class StackAdvisorBlueprintProcessorTest {
  private StackAdvisorBlueprintProcessor underTest = new StackAdvisorBlueprintProcessor();

  private ClusterTopology clusterTopology = createMock(ClusterTopology.class);
  private BlueprintImpl blueprint = createMock(BlueprintImpl.class);
  private Stack stack = createMock(Stack.class);
  private HostGroup hostGroup = createMock(HostGroup.class);
  private Configuration configuration = createMock(Configuration.class);

  private static StackAdvisorHelper stackAdvisorHelper = createMock(StackAdvisorHelper.class);

  @BeforeClass
  public static void initClass() {
    StackAdvisorBlueprintProcessor.init(stackAdvisorHelper);
  }

  @Before
  public void setUp() {
    reset(clusterTopology, blueprint, stack, stackAdvisorHelper);
  }

  @Test
  public void testAdviseConfiguration() throws StackAdvisorException, ConfigurationTopologyException {
    // GIVEN
    Map<String, AdvisedConfiguration> advisedConfigurations = new HashMap<String, AdvisedConfiguration>();
    expect(clusterTopology.getBlueprint()).andReturn(blueprint).anyTimes();
    expect(clusterTopology.getHostGroupInfo()).andReturn(createHostGroupInfo()).anyTimes();
    expect(clusterTopology.getAdvisedConfigurations()).andReturn(advisedConfigurations).anyTimes();
    expect(clusterTopology.getConfiguration()).andReturn(configuration).anyTimes();
    expect(blueprint.getStack()).andReturn(stack).anyTimes();
    expect(stack.getVersion()).andReturn("2.3").anyTimes();
    expect(stack.getName()).andReturn("HDP").anyTimes();
    expect(blueprint.getServices()).andReturn(Arrays.asList("HDFS", "YARN", "HIVE")).anyTimes();
    expect(blueprint.getHostGroups()).andReturn(createHostGroupMap()).anyTimes();
    expect(hostGroup.getComponents()).andReturn(Arrays.asList("comp1", "comp2")).anyTimes();
    expect(stackAdvisorHelper.recommend(anyObject(StackAdvisorRequest.class))).andReturn(createRecommendationResponse());
    expect(configuration.getFullProperties()).andReturn(createProps());

    replay(clusterTopology, blueprint, stack, hostGroup, configuration, stackAdvisorHelper);
    // WHEN
    underTest.adviseConfiguration(clusterTopology);
    // THEN
    assertTrue(advisedConfigurations.get("core-site").getProperties().containsKey("dummyKey1"));
    assertTrue(advisedConfigurations.get("core-site").getPropertyValueAttributes().containsKey("dummyKey2"));
    assertEquals("dummyValue", advisedConfigurations.get("core-site").getProperties().get("dummyKey1"));
    assertEquals(Boolean.toString(true), advisedConfigurations.get("core-site")
      .getPropertyValueAttributes().get("dummyKey2").getDelete());
  }

  @Test
  public void testAdviseConfigurationWhenConfigurationRecommendFails() throws StackAdvisorException, ConfigurationTopologyException {
    // GIVEN
    Map<String, AdvisedConfiguration> advisedConfigurations = new HashMap<String, AdvisedConfiguration>();
    expect(clusterTopology.getBlueprint()).andReturn(blueprint).anyTimes();
    expect(clusterTopology.getHostGroupInfo()).andReturn(createHostGroupInfo()).anyTimes();
    expect(clusterTopology.getAdvisedConfigurations()).andReturn(advisedConfigurations).anyTimes();
    expect(clusterTopology.getConfiguration()).andReturn(configuration).anyTimes();
    expect(blueprint.getStack()).andReturn(stack).anyTimes();
    expect(stack.getVersion()).andReturn("2.3").anyTimes();
    expect(stack.getName()).andReturn("HDP").anyTimes();
    expect(blueprint.getHostGroups()).andReturn(createHostGroupMap()).anyTimes();
    expect(hostGroup.getComponents()).andReturn(Arrays.asList("comp1", "comp2")).anyTimes();
    expect(blueprint.getServices()).andReturn(Arrays.asList("HDFS", "YARN", "HIVE")).anyTimes();
    expect(stackAdvisorHelper.recommend(anyObject(StackAdvisorRequest.class))).andThrow(new StackAdvisorException("ex"));
    expect(configuration.getFullProperties()).andReturn(createProps());

    replay(clusterTopology, blueprint, stack, hostGroup, configuration, stackAdvisorHelper);
    // WHEN
    try {
      underTest.adviseConfiguration(clusterTopology);
      fail("Invalid state");
    } catch (ConfigurationTopologyException e) {
      assertEquals(StackAdvisorBlueprintProcessor.RECOMMENDATION_FAILED, e.getMessage());
    }
  }

  @Test
  public void testAdviseConfigurationWhenConfigurationRecommendHasInvalidResponse() throws StackAdvisorException, ConfigurationTopologyException {
    // GIVEN
    Map<String, AdvisedConfiguration> advisedConfigurations = new HashMap<String, AdvisedConfiguration>();
    expect(clusterTopology.getBlueprint()).andReturn(blueprint).anyTimes();
    expect(clusterTopology.getHostGroupInfo()).andReturn(createHostGroupInfo()).anyTimes();
    expect(clusterTopology.getAdvisedConfigurations()).andReturn(advisedConfigurations).anyTimes();
    expect(clusterTopology.getConfiguration()).andReturn(configuration).anyTimes();
    expect(blueprint.getStack()).andReturn(stack).anyTimes();
    expect(stack.getVersion()).andReturn("2.3").anyTimes();
    expect(stack.getName()).andReturn("HDP").anyTimes();
    expect(blueprint.getServices()).andReturn(Arrays.asList("HDFS", "YARN", "HIVE")).anyTimes();
    expect(blueprint.getHostGroups()).andReturn(createHostGroupMap()).anyTimes();
    expect(hostGroup.getComponents()).andReturn(Arrays.asList("comp1", "comp2")).anyTimes();
    expect(stackAdvisorHelper.recommend(anyObject(StackAdvisorRequest.class))).andReturn(new RecommendationResponse());
    expect(configuration.getFullProperties()).andReturn(createProps());

    replay(clusterTopology, blueprint, stack, hostGroup, configuration, stackAdvisorHelper);
    // WHEN
    try {
      underTest.adviseConfiguration(clusterTopology);
      fail("Invalid state");
    } catch (ConfigurationTopologyException e) {
      assertEquals(StackAdvisorBlueprintProcessor.INVALID_RESPONSE, e.getMessage());
    }
  }

  private Map<String, Map<String, String>> createProps() {
    Map<String, Map<String, String>> props = Maps.newHashMap();
    Map<String, String> siteProps = Maps.newHashMap();
    siteProps.put("myprop", "myvalue");
    props.put("core-site", siteProps);
    return props;
  }

  private Map<String, HostGroup> createHostGroupMap() {
    Map<String, HostGroup> hgMap = Maps.newHashMap();
    hgMap.put("hg1", hostGroup);
    hgMap.put("hg2", hostGroup);
    hgMap.put("hg3", hostGroup);
    return hgMap;
  }

  private Map<String, HostGroupInfo> createHostGroupInfo() {
    Map<String, HostGroupInfo> hostGroupInfoMap = new HashMap<String, HostGroupInfo>();
    HostGroupInfo hgi1 = new HostGroupInfo("hostGroup1");
    HostGroupInfo hgi2 = new HostGroupInfo("hostGroup2");
    hostGroupInfoMap.put("hg1", hgi1);
    hostGroupInfoMap.put("hg2", hgi2);
    return hostGroupInfoMap;
  }

  private RecommendationResponse createRecommendationResponse() {
    RecommendationResponse response = new RecommendationResponse();
    RecommendationResponse.Recommendation recommendations = new RecommendationResponse.Recommendation();
    RecommendationResponse.Blueprint blueprint = new RecommendationResponse.Blueprint();
    Map<String, RecommendationResponse.BlueprintConfigurations> blueprintConfigurationsMap =
      new HashMap<String, RecommendationResponse.BlueprintConfigurations>();
    RecommendationResponse.BlueprintConfigurations blueprintConfig =
      new RecommendationResponse.BlueprintConfigurations();
    Map<String, String> properties = new HashMap<String, String>();
    properties.put("dummyKey1", "dummyValue");
    blueprintConfig.setProperties(properties);
    Map<String, ValueAttributesInfo> propAttributes = new HashMap<String, ValueAttributesInfo>();
    ValueAttributesInfo valueAttributesInfo = new ValueAttributesInfo();
    valueAttributesInfo.setDelete("true");
    propAttributes.put("dummyKey2", valueAttributesInfo);
    blueprintConfig.setPropertyAttributes(propAttributes);
    blueprintConfigurationsMap.put("core-site", blueprintConfig);
    blueprint.setConfigurations(blueprintConfigurationsMap);
    recommendations.setBlueprint(blueprint);
    response.setRecommendations(recommendations);
    return response;
  }


}
