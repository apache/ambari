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

package org.apache.ambari.server.api.services.mpackadvisor;


import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.ambari.server.api.services.mpackadvisor.MpackAdvisorRequest.MpackAdvisorRequestType.CONFIGURATIONS;
import static org.apache.ambari.server.utils.ExceptionUtils.uncheckedVoid;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.MockType.NICE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.api.services.mpackadvisor.recommendations.MpackRecommendationResponse;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.ValueAttributesInfo;
import org.apache.ambari.server.topology.AdvisedConfiguration;
import org.apache.ambari.server.topology.Blueprint;
import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.Component;
import org.apache.ambari.server.topology.ConfigRecommendationStrategy;
import org.apache.ambari.server.topology.Configuration;
import org.apache.ambari.server.topology.HostGroup;
import org.apache.ambari.server.topology.HostGroupImpl;
import org.apache.ambari.server.topology.HostGroupInfo;
import org.apache.ambari.server.topology.MpackInstance;
import org.apache.ambari.server.topology.ResolvedComponent;
import org.apache.ambari.server.topology.ServiceInstance;
import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

@RunWith(EasyMockRunner.class)
public class MpackAdvisorBlueprintProcessorTest {

  private static final Logger LOG = LoggerFactory.getLogger(MpackAdvisorBlueprintProcessorTest.class);

  private MpackAdvisorBlueprintProcessor processor;

  private static final String HOSTGROUP_1 = "host_group_1";

  private static final String HOST_1 = "c7401.ambari.apache.org";

  private static final String NAMENODE = "NAMENODE";
  private static final String DATANODE = "DATANODE";
  private static final String ZOOKEEPER_SERVER = "ZOOKEPER_SERVER";
  private static final String HADOOP_CLIENT = "HADOOP_CLIENT";
  private static final String ZOOKEEPER_CLIENT = "ZOOKEEPER_CLIENT";
  private static final String HBASE_MASTER = "HBASE_MASTER";
  private static final String HBASE_REGIONSERVER = "HBASE_REGIONSERVER";

  private static final StackId STACK_ID_HDP_CORE = new StackId("HDPCORE", "1.0.0");
  private static final StackId STACK_ID_ODS = new StackId("ODS", "1.0.1");

  private static final Map<String, Set<String>> mpackComponents = ImmutableMap.of(
    STACK_ID_HDP_CORE.getStackName(), ImmutableSet.of(NAMENODE, DATANODE, HADOOP_CLIENT, ZOOKEEPER_SERVER, ZOOKEEPER_CLIENT),
    STACK_ID_ODS.getStackName(), ImmutableSet.of(HBASE_MASTER, HBASE_REGIONSERVER, HADOOP_CLIENT, ZOOKEEPER_CLIENT)
  );

  private static final Map<String, String> configTypeToService = ImmutableMap.of(
    "zoo.cfg", "ZOOKEEPER",
    "hdfs-site", "HDFS",
    "hbase-size", "HBASE"
  );

  @Mock(type = NICE)
  private MpackAdvisorHelper helper;

  @Mock(type = NICE)
  private AmbariMetaInfo metaInfo;

  @Mock(type = NICE)
  private ClusterTopology topology;

  @Mock(type = NICE)
  private StackInfo hdpCore;

  @Mock(type = NICE)
  private StackInfo ods;

  // These test data are reconfigurable to allow for flexibility for later unit tests
  private Set<MpackInstance> mpacks = Sets.newHashSet();
  private Map<String, AdvisedConfiguration> advisedConfigurations = new HashMap<>();
  private Map<String, Set<ResolvedComponent>> componentsByHostgroup = new HashMap<>();
  private ConfigRecommendationStrategy recommendationStrategy = ConfigRecommendationStrategy.ALWAYS_APPLY_DONT_OVERRIDE_CUSTOM_VALUES;
  private List<ServiceInfo> hdpCoreServices = new ArrayList<>();
  private List<ServiceInfo> odsServices = new ArrayList<>();
  private Configuration configuration = new Configuration(new HashMap<>(), new HashMap<>());
  private Map<String, HostGroupInfo> hostGroupInfoMap = new HashMap<>();
  private Map<String, HostGroup> hostGroupMap = new HashMap<>();
  private Map<String, Map<String, String>> userConfigs = new HashMap<>();
  private MpackRecommendationResponse response = createRecommendationResponse();

  private final MpackInstance hdpCoreMpack = new MpackInstance(
    STACK_ID_HDP_CORE.getStackName(),
    STACK_ID_HDP_CORE.getStackName(),
    STACK_ID_HDP_CORE.getStackVersion(),
    "http://hdpcore.info",
    new Configuration(new HashMap<>(), new HashMap<>()),
    new HashSet<>());

  private final MpackInstance odsMpack = new MpackInstance(
    STACK_ID_ODS.getStackName(),
    STACK_ID_ODS.getStackName(),
    STACK_ID_ODS.getStackVersion(),
    "http://ods.info",
    new Configuration(new HashMap<>(), new HashMap<>()),
    new HashSet<>());


  public void setUp() throws Exception {
    // setup topology
    expect(topology.getMpacks()).andReturn(mpacks).anyTimes();
    expect(topology.getAdvisedConfigurations()).andReturn(advisedConfigurations).anyTimes();
    expect(topology.getComponentsByHostGroup()).andReturn(componentsByHostgroup).anyTimes();
    expect(topology.isValidConfigType(anyString())).andReturn(true).anyTimes();
    expect(topology.getConfigRecommendationStrategy()).andAnswer(() -> recommendationStrategy).anyTimes();
    expect(topology.getConfiguration()).andReturn(configuration).anyTimes();
    expect(topology.getHostGroupInfo()).andReturn(hostGroupInfoMap).anyTimes();
    Blueprint blueprint = createMock(Blueprint.class);
    expect(blueprint.getHostGroups()).andReturn(hostGroupMap).anyTimes();
    replay(blueprint);
    expect(topology.getBlueprint()).andReturn(blueprint).anyTimes();
    replay(topology);

    // setup metainfo
    expect(metaInfo.getStack(STACK_ID_HDP_CORE)).andReturn(hdpCore).anyTimes();
    expect(metaInfo.getStack(STACK_ID_ODS)).andReturn(ods).anyTimes();
    replay(metaInfo);

    // setup stacks
    expect(hdpCore.getServices()).andReturn(hdpCoreServices).anyTimes();
    replay(hdpCore);
    expect(ods.getServices()).andReturn(odsServices).anyTimes();
    replay(ods);

    // setup mpack instances
    mpacks.add(hdpCoreMpack);
    mpacks.add(odsMpack);

    // setup default host groups
    addHostGroup(HOSTGROUP_1,
      ImmutableList.of(HOST_1),
      ImmutableList.of(HADOOP_CLIENT  + "@HDPCORE",
        ZOOKEEPER_CLIENT + "@ODS",
        NAMENODE,
        DATANODE,
        ZOOKEEPER_SERVER,
        HBASE_MASTER,
        HBASE_REGIONSERVER));

    // setup services
    hdpCoreServices.addAll(ImmutableList.of(
      serviceInfo("HDFS", "HADOOP_CLIENT", "NAMENODE", "DATANODE"),
      serviceInfo("ZOOKEEPER", "ZOOKEPER_SERVER"),
      serviceInfo("HADOOP_CLIENTS", "HADOOP_CLIENT"),
      serviceInfo("ZOOKEEPER_CLIENTS", "ZOOKEEPER_CLIENT")
    ));
    odsServices.addAll(ImmutableList.of(
      serviceInfo("HBASE", "HBASE_MASTER", "HBASE_REGIONSERVER"),
      serviceInfo("HADOOP_CLIENTS", "HADOOP_CLIENT"),
      serviceInfo("ZOOKEEPER_CLIENTS", "ZOOKEEPER_CLIENT")
    ));

    // setup user supplied configs
    userConfigs.put("hdfs-site", ImmutableMap.of(
      "dfs.datanode.data.dir", "/grid/0/hdfs/data",
      "dfs.namenode.name.dir", "/grid/0/hdfs/namenode"));
    userConfigs.put("hbase-site", ImmutableMap.of(
      "hbase.regionserver.global.memstore.size", "0.3",
      "hbase.coprocessor.region.classes", "org.apache.hadoop.hbase.security.access.CustomClass"));

    // setup helper
    expect(helper.recommend(anyObject())).andAnswer(() -> {
      checkMpackRecommendationRequest((MpackAdvisorRequest)getCurrentArguments()[0]);
      return response;
    });
    replay(helper);

    MpackAdvisorBlueprintProcessor.init(helper, metaInfo);
    processor = new MpackAdvisorBlueprintProcessor();
  }

  @After
  public void tearDown() {
    reset(helper, metaInfo, topology, hdpCore, ods);
  }

  @Test
  public void testAdviseConfiguration_OverrideUserConfig() throws Exception {
    // GIVEN
    setUp();
    recommendationStrategy = ConfigRecommendationStrategy.ALWAYS_APPLY;
    LOG.info("Testing using recommendation strategy: {}", recommendationStrategy);

    // WHEN
    processor.adviseConfiguration(topology, userConfigs);

    // THEN
    Map<String, AdvisedConfiguration> advisedConfigurations = topology.getAdvisedConfigurations();
    assertEquals(ImmutableSet.of("hdfs-site", "zoo.cfg", "hbase-site"), advisedConfigurations.keySet());

    // all recommended configs coming from the advisor must make it into the topology
    Set<String> expectedAdvisedZooCfgKeys = ImmutableSet.of("dataDir");
    Set<String> expectedAdvisedHdfsSiteKeys = ImmutableSet.of("dfs.namenode.checkpoint.dir", "dfs.datanode.data.dir");
    Set<String> expectedAdvisedHbaseSiteKeys = ImmutableSet.of("hbase.regionserver.wal.codec", "hbase.regionserver.global.memstore.size");

    assertEquals(expectedAdvisedZooCfgKeys, advisedConfigurations.get("zoo.cfg").getProperties().keySet());
    assertEquals(expectedAdvisedHdfsSiteKeys, advisedConfigurations.get("hdfs-site").getProperties().keySet());
    assertEquals(expectedAdvisedHdfsSiteKeys, advisedConfigurations.get("hdfs-site").getPropertyValueAttributes().keySet());
    assertEquals(expectedAdvisedHbaseSiteKeys, advisedConfigurations.get("hbase-site").getProperties().keySet());
    assertEquals(expectedAdvisedHbaseSiteKeys, advisedConfigurations.get("hbase-site").getPropertyValueAttributes().keySet());
  }

  @Test
  public void testAdviseConfiguration_KeepUserConfig() throws Exception {
    // GIVEN
    EnumSet.of(
      ConfigRecommendationStrategy.ONLY_STACK_DEFAULTS_APPLY,
      ConfigRecommendationStrategy.ALWAYS_APPLY_DONT_OVERRIDE_CUSTOM_VALUES).forEach(
        strategy -> {
          LOG.info("Testing using recommendation strategy: {}", strategy);
          recommendationStrategy = strategy;
          uncheckedVoid(() -> {
            setUp();
            // WHEN
            processor.adviseConfiguration(topology, userConfigs);
          });
          // THEN

          // only keys not in the user supplied configs should be among the advised configs.
          Set<String> expectedAdvisedZooCfgKeys = ImmutableSet.of("dataDir");
          Set<String> expectedAdvisedHdfsSiteKeys = ImmutableSet.of("dfs.namenode.checkpoint.dir");
          Set<String> expectedAdvisedHbaseSiteKeys = ImmutableSet.of("hbase.regionserver.wal.codec");

          assertEquals(expectedAdvisedZooCfgKeys, advisedConfigurations.get("zoo.cfg").getProperties().keySet());
          assertEquals(expectedAdvisedHdfsSiteKeys, advisedConfigurations.get("hdfs-site").getProperties().keySet());
          assertEquals(expectedAdvisedHdfsSiteKeys, advisedConfigurations.get("hdfs-site").getPropertyValueAttributes().keySet());
          assertEquals(expectedAdvisedHbaseSiteKeys, advisedConfigurations.get("hbase-site").getProperties().keySet());
          assertEquals(expectedAdvisedHbaseSiteKeys, advisedConfigurations.get("hbase-site").getPropertyValueAttributes().keySet());

          tearDown();
        }
      );
  }


  /**
   * Check if the MpackAdvisorRequest was created correctly
   */
  private void checkMpackRecommendationRequest(MpackAdvisorRequest request) {
    assertEquals(CONFIGURATIONS, request.getRequestType());

    List<String> expectedHosts =
      hostGroupInfoMap.values().stream().flatMap(hg -> hg.getHostNames().stream()).collect(toList());
    assertEquals(expectedHosts, request.getHosts());

    // check mpack -> component -> hosts map
    assertTrue(!request.getMpackComponentHostsMap().isEmpty());
    request.getMpackComponentHostsMap().entrySet().forEach(
      e -> {
        String mpackName = e.getKey();
        Map<String, Set<String>> compentsToHosts = e.getValue();
        assertTrue(!compentsToHosts.isEmpty()); // at least one component is used from the mpack
        Set<String> validMpackComponents = mpackComponents.get(mpackName);
        compentsToHosts.entrySet().forEach( compToHosts -> {
          assertTrue(validMpackComponents.contains(compToHosts.getKey())); // component is valid for that mpack
          assertTrue(!compToHosts.getValue().isEmpty()); // component is mapped to at least one host
        });
      });

    Map<String, Set<String>> expectedHostBindings =
      hostGroupInfoMap.entrySet().stream().collect( toMap(
        Map.Entry::getKey,
        hostGroupToHosts -> hostGroupToHosts.getValue().getHostNames()));
    assertEquals(expectedHostBindings, request.getRecommendation().getBlueprintClusterBinding());

    MpackAdvisorRequest.Blueprint requestBlueprint = request.getRecommendation().getBlueprint();

    // verify that mpack instances have been enriched with service instances (mpack advisor requires this even when
    // no explicit service instances have been defined in the blueprint)
    Set<String> validHdpServiceNames = hdpCoreServices.stream().map(ServiceInfo::getName).collect(toSet());
    Set<String> validOdsServiceNames = odsServices.stream().map(ServiceInfo::getName).collect(toSet());
    requestBlueprint.getMpackInstances().forEach( mpack -> {
      Set<String> validServiceNames =
        mpack.getMpackType().equals(STACK_ID_HDP_CORE.getStackName()) ? validHdpServiceNames : validOdsServiceNames;
      Set<String> serviceInstanceNames = mpack.getServiceInstances().stream().map(ServiceInstance::getName).collect(toSet());
      assertTrue("No service instances has been added to mpack " + mpack.getMpackName(), !serviceInstanceNames.isEmpty());
      Set<String> unexpectedServices = Sets.difference(serviceInstanceNames, validServiceNames);
      assertTrue("Unexected service instances: " + unexpectedServices + " for mpack instance: " + mpack.getMpackName(),
        unexpectedServices.isEmpty());
    });
  }

  /**
   * Creates a recommendation response
   */
  private MpackRecommendationResponse createRecommendationResponse() {
    MpackRecommendationResponse response = new MpackRecommendationResponse();
    MpackRecommendationResponse.Recommendation recommendation = new MpackRecommendationResponse.Recommendation();
    MpackRecommendationResponse.Blueprint blueprint = new MpackRecommendationResponse.Blueprint();
    recommendation.setBlueprint(blueprint);
    response.setRecommendations(recommendation);

    MpackRecommendationResponse.MpackInstance responseHdpCoreMpack = responseMpack(STACK_ID_HDP_CORE,
      ImmutableSet.of(
        serviceInstance("HDFS",
          ImmutableMap.of(
            "hdfs-site",
            config(ImmutableMap.of(
              "dfs.namenode.checkpoint.dir", "/hadoop/hdfs/namesecondary",
              "dfs.datanode.data.dir", "/hadoop/hdfs/data")))
        ),
        serviceInstance("ZOOKEEPER",
          ImmutableMap.of(
            "zoo.cfg",
            config(ImmutableMap.of("dataDir", "/hadoop/zookeeper"))))
      )
    );

    MpackRecommendationResponse.MpackInstance responseOdsMpack = responseMpack(STACK_ID_ODS,
      ImmutableSet.of(
        serviceInstance("HBASE",
          ImmutableMap.of(
            "hbase-site",
            config(ImmutableMap.of(
              "hbase.regionserver.wal.codec", "org.apache.hadoop.hbase.regionserver.wal.WALCellCodec",
              "hbase.regionserver.global.memstore.size", "0.4")))
        )
      )
    );
    blueprint.setMpackInstances(ImmutableSet.of(responseHdpCoreMpack, responseOdsMpack));
    return response;
  }

  private void addHostGroup(String name, List<String> hosts, List<String> components) {
    hostGroupInfoMap.put(name, hostGroupInfo(name, hosts));
    hostGroupMap.put(name, hostGroup(name, components));
  }


  // ----- FACTORY METHODS FOR TEST DATA -----

  private MpackRecommendationResponse.MpackInstance responseMpack(StackId stackId,
                                                                  Set<MpackRecommendationResponse.ServiceInstance> services) {
    MpackRecommendationResponse.MpackInstance responseMpack = new MpackRecommendationResponse.MpackInstance();
    responseMpack.setName(stackId.getStackName());
    responseMpack.setVersion(stackId.getStackVersion());
    responseMpack.setServiceInstances(services);
    return responseMpack;
  }

  private MpackRecommendationResponse.ServiceInstance serviceInstance(String name, Map<String, MpackRecommendationResponse.BlueprintConfigurations> configs) {
    MpackRecommendationResponse.ServiceInstance instance = new MpackRecommendationResponse.ServiceInstance();
    instance.setName(name);
    instance.setType(name);
    instance.setConfigurations(configs);
    return instance;
  }

  /**
   * @param properties Properties in key-value format. The same keys will be added as attributes as well
   *                   with the value {@code delete=true} to test the handling of advised attributes
   * @return an instance of {@link MpackRecommendationResponse.BlueprintConfigurations} object with the given data
   */
  private MpackRecommendationResponse.BlueprintConfigurations config(Map<String, String> properties) {
    MpackRecommendationResponse.BlueprintConfigurations config = new MpackRecommendationResponse.BlueprintConfigurations();
    config.setProperties(properties);
    Map<String, ValueAttributesInfo> propertyAttributes = properties.keySet().stream().collect(toMap(
      Function.identity(),
      __ -> {
        ValueAttributesInfo valueAttributesInfo = new ValueAttributesInfo();
        valueAttributesInfo.setDelete("true");
        return valueAttributesInfo;
      }
    ));
    config.setPropertyAttributes(propertyAttributes);
    return config;
  }

  private ServiceInfo serviceInfo(String name, String... components) {
    ServiceInfo service = new ServiceInfo();
    service.setName(name);
    service.setComponents(
      Arrays.stream(components).map(cName -> {
        ComponentInfo component = new ComponentInfo();
        component.setName(cName);
        return component;
      }).collect(toList()));
    return service;
  }

  private HostGroupInfo hostGroupInfo(String name, List<String> hosts) {
    HostGroupInfo info = new HostGroupInfo(name);
    hosts.forEach(info::addHost);
    return info;
  }

  private Component component(String name) {
    if (name.contains("@")) {
      List<String> nameAndMpack = Splitter.on('@').splitToList(name);
      return new Component(nameAndMpack.get(0), nameAndMpack.get(1), null, null);
    }
    else {
      return new Component(name);
    }
  }

  private HostGroup hostGroup(String name, List<String> components) {
    return new HostGroupImpl(name,
      components.stream().map(cName -> component(cName)).collect(toList()),
      new Configuration(new HashMap<>(), new HashMap<>()),
      "1+");
  }

}