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

package org.apache.ambari.server.topology.addservice;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toMap;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorHelper;
import org.apache.ambari.server.api.services.stackadvisor.recommendations.RecommendationResponse;
import org.apache.ambari.server.api.services.stackadvisor.validations.ValidationResponse;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.StackId;
import org.apache.commons.lang3.tuple.Pair;
import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Injector;

@RunWith(EasyMockRunner.class)
public class StackAdvisorAdapterTest {

  @Mock
  private AmbariManagementController managementController;

  @Mock
  private StackAdvisorHelper stackAdvisorHelper;

  @Mock
  private Configuration serverConfig;

  @Mock
  private Injector injector;

  @Mock
  private Stack stack;

  @TestSubject
  private StackAdvisorAdapter adapter = new StackAdvisorAdapter();

  private static final Map<String, Set<String>> COMPONENT_HOST_MAP = ImmutableMap.<String, Set<String>>builder()
    .put("NAMENODE", ImmutableSet.of("c7401", "c7402"))
    .put("DATANODE", ImmutableSet.of("c7403", "c7404", "c7405", "c7406"))
    .put("HDFS_CLIENT", ImmutableSet.of("c7403", "c7404", "c7405", "c7406"))
    .put("ZOOKEEPER_SERVER", ImmutableSet.of("c7401", "c7402"))
    .put("ZOOKEEPER_CLIENT", ImmutableSet.of("c7401", "c7402", "c7403", "c7404", "c7405", "c7406"))
    .build();

  private static final Map<String, Map<String, Set<String>>> SERVICE_COMPONENT_HOST_MAP_1 = ImmutableMap.of(
    "HDFS", ImmutableMap.of(
      "NAMENODE", ImmutableSet.of("c7401", "c7402"),
      "DATANODE", ImmutableSet.of("c7403", "c7404", "c7405", "c7406"),
      "HDFS_CLIENT", ImmutableSet.of("c7403", "c7404", "c7405", "c7406")),
    "ZOOKEEPER", ImmutableMap.of(
      "ZOOKEEPER_SERVER", ImmutableSet.of("c7401", "c7402"),
      "ZOOKEEPER_CLIENT", ImmutableSet.of("c7401", "c7402", "c7403", "c7404", "c7405", "c7406")));

  private static final Map<String, Map<String, Set<String>>> SERVICE_COMPONENT_HOST_MAP_2 = ImmutableMap.<String, Map<String, Set<String>>>builder()
    .putAll(SERVICE_COMPONENT_HOST_MAP_1)
    .put("HIVE", emptyMap())
    .put("SPARK2", emptyMap())
    .build();

  private static final Map<String, Set<String>> HOST_COMPONENT_MAP = ImmutableMap.<String, Set<String>>builder()
    .put("c7401", ImmutableSet.of("NAMENODE", "ZOOKEEPER_SERVER", "ZOOKEEPER_CLIENT"))
    .put("c7402", ImmutableSet.of("NAMENODE", "ZOOKEEPER_SERVER", "ZOOKEEPER_CLIENT"))
    .put("c7403", ImmutableSet.of("DATANODE", "HDFS_CLIENT", "ZOOKEEPER_CLIENT"))
    .put("c7404", ImmutableSet.of("DATANODE", "HDFS_CLIENT", "ZOOKEEPER_CLIENT"))
    .put("c7405", ImmutableSet.of("DATANODE", "HDFS_CLIENT", "ZOOKEEPER_CLIENT"))
    .put("c7406", ImmutableSet.of("DATANODE", "HDFS_CLIENT", "ZOOKEEPER_CLIENT"))
    .build();

  @Test
  public void getHostComponentMap() {
    assertEquals(HOST_COMPONENT_MAP, StackAdvisorAdapter.getHostComponentMap(COMPONENT_HOST_MAP));
  }

  @Test
  public void getComponentHostMap() {
    assertEquals(COMPONENT_HOST_MAP, StackAdvisorAdapter.getComponentHostMap(SERVICE_COMPONENT_HOST_MAP_2));
  }

  @Test
  public void getRecommendedLayout() {
    Map<String, Set<String>> hostGroups = ImmutableMap.of(
      "host_group1", ImmutableSet.of("c7401", "c7402"),
      "host_group2", ImmutableSet.of("c7403", "c7404", "c7405", "c7406"));

    Map<String, Set<String>> hostGroupComponents = ImmutableMap.of(
      "host_group1", ImmutableSet.of("NAMENODE", "ZOOKEEPER_SERVER", "ZOOKEEPER_CLIENT"),
      "host_group2", ImmutableSet.of("DATANODE", "HDFS_CLIENT", "ZOOKEEPER_CLIENT"));

    Map<String, String> serviceToComponent = ImmutableMap.<String, String>builder()
      .put("NAMENODE", "HDFS")
      .put("DATANODE", "HDFS")
      .put("HDFS_CLIENT", "HDFS")
      .put("ZOOKEEPER_SERVER", "ZOOKEEPER")
      .put("ZOOKEEPER_CLIENT", "ZOOKEEPER")
      .build();

    assertEquals(SERVICE_COMPONENT_HOST_MAP_1,
      StackAdvisorAdapter.getRecommendedLayout(hostGroups, hostGroupComponents, serviceToComponent::get));
  }

  @Test
  public void mergeDisjunctMaps() {
    Map<String, String> map1 = ImmutableMap.of("key1", "value1", "key2", "value2");
    Map<String, String> map2 = ImmutableMap.of("key3", "value3", "key4", "value4");
    assertEquals(
      ImmutableMap.of("key1", "value1", "key2", "value2", "key3", "value3", "key4", "value4"),
      StackAdvisorAdapter.mergeDisjunctMaps(map1, map2));
  }

  @Test(expected = IllegalArgumentException.class)
  public void mergeDisjunctMaps_invalidInput() {
    Map<String, String> map1 = ImmutableMap.of("key1", "value1", "key2", "value2");
    Map<String, String> map2 = ImmutableMap.of("key2", "value2", "key3", "value3");
    StackAdvisorAdapter.mergeDisjunctMaps(map1, map2);
  }

  @Test
  public void keepNewServicesOnly() {
    Map<String, Map<String, Set<String>>> newServices = ImmutableMap.of(
      "KAFKA", emptyMap(),
      "PIG", emptyMap());

    Map<String, Map<String, Set<String>>> recommendationForNewServices = ImmutableMap.of(
      "KAFKA", ImmutableMap.of("KAFKA_BROKER", ImmutableSet.of("c7405")),
      "PIG", ImmutableMap.of("PIG_CLIENT", ImmutableSet.of("c7405", "c7406")));

    Map<String, Map<String, Set<String>>> recommendations = new HashMap<>(SERVICE_COMPONENT_HOST_MAP_1);
    recommendations.putAll(recommendationForNewServices);

    StackAdvisorAdapter.keepNewServicesOnly(recommendations, newServices);
    assertEquals(recommendationForNewServices, recommendations);
  }

  @Before
  public void setUp() throws Exception {
    Cluster cluster = mock(Cluster.class);
    expect(cluster.getHostNames()).andReturn(ImmutableSet.of("c7401", "c7402"));
    expect(cluster.getServices()).andReturn(ImmutableMap.of(
      "HDFS",
      service("HDFS", ImmutableMap.of("NAMENODE", ImmutableSet.of("c7401"), "HDFS_CLIENT", ImmutableSet.of("c7401", "c7402"))),
      "ZOOKEEPER",
      service("ZOOKEEPER", ImmutableMap.of("ZOOKEEPER_SERVER", ImmutableSet.of("c7401"), "ZOOKEEPER_CLIENT", ImmutableSet.of("c7401", "c7402")))));
    Clusters clusters = mock(Clusters.class);
    expect(clusters.getCluster(anyString())).andReturn(cluster).anyTimes();
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    replay(clusters, cluster, managementController);

    expect(serverConfig.getGplLicenseAccepted()).andReturn(Boolean.FALSE).anyTimes();
    expect(serverConfig.getAddServiceHostGroupStrategyClass()).andReturn((Class)GroupByComponentsStrategy.class).anyTimes();
    replay(serverConfig);

    expect(injector.getInstance(GroupByComponentsStrategy.class)).andReturn(new GroupByComponentsStrategy()).anyTimes();
    replay(injector);

    RecommendationResponse response = new RecommendationResponse();
    RecommendationResponse.Recommendation recommendation = new RecommendationResponse.Recommendation();
    response.setRecommendations(recommendation);
    RecommendationResponse.BlueprintClusterBinding binding = RecommendationResponse.BlueprintClusterBinding.fromHostGroupHostMap(
      ImmutableMap.of(
        "hostgroup-1", ImmutableSet.of("c7401"),
        "hostgroup-2", ImmutableSet.of("c7402")));
    recommendation.setBlueprintClusterBinding(binding);
    RecommendationResponse.Blueprint blueprint = new RecommendationResponse.Blueprint();
    blueprint.setHostGroups(RecommendationResponse.HostGroup.fromHostGroupComponents(
      ImmutableMap.of(
        "hostgroup-1", ImmutableSet.of("NAMENODE", "HDFS_CLIENT", "ZOOKEEPER_SERVER", "ZOOKEEPER_CLIENT"),
        "hostgroup-2", ImmutableSet.of("HDFS_CLIENT", "ZOOKEEPER_CLIENT", "KAFKA_BROKER"))
    ));
    recommendation.setBlueprint(blueprint);
    expect(stackAdvisorHelper.recommend(anyObject())).andReturn(response);

    ValidationResponse validationResponse = new ValidationResponse();
    validationResponse.setItems(emptySet());
    expect(stackAdvisorHelper.validate(anyObject())).andReturn(validationResponse);
    replay(stackAdvisorHelper);

    expect(stack.getStackId()).andReturn(new StackId("HDP", "3.0")).anyTimes();
    ImmutableMap<String, String> serviceComponentMap = ImmutableMap.<String, String>builder()
      .put("KAFKA_BROKER", "KAFKA")
      .put("NAMENODE", "HDFS")
      .put("HDFS_CLIENT", "HDFS")
      .put("ZOOKEEPER_SERVER", "ZOOKEEPER")
      .put("ZOOKEEPER_CLIENT", "ZOOKEEPER")
      .build();
    expect(stack.getServiceForComponent(anyString())).andAnswer(() -> serviceComponentMap.get(getCurrentArguments()[0])).anyTimes();
    replay(stack);
  }

  private static Service service(String name, ImmutableMap<String,ImmutableSet<String>> componentHostMap) {
    Service service = mock(Service.class);
    expect(service.getName()).andReturn(name).anyTimes();
    Map<String, ServiceComponent> serviceComponents = componentHostMap.entrySet().stream()
      .map(entry -> {
        ServiceComponent component = mock(ServiceComponent.class);
        expect(component.getName()).andReturn(entry.getKey()).anyTimes();
        expect(component.getServiceComponentsHosts()).andReturn(entry.getValue()).anyTimes();
        replay(component);
        return Pair.of(entry.getKey(), component);
      })
      .collect(toMap(Pair::getKey, Pair::getValue));
    expect(service.getServiceComponents()).andReturn(serviceComponents).anyTimes();
    replay(service);
    return service;
  }

  @Test
  public void recommendLayout() {
    Map<String, Map<String, Set<String>>> newServices = ImmutableMap.of(
      "KAFKA",
      ImmutableMap.of("KAFKA_BROKER", emptySet()));

    AddServiceInfo info = new AddServiceInfo(null, "c1", stack, org.apache.ambari.server.topology.Configuration.newEmpty(), null, null, newServices);
    AddServiceInfo infoWithRecommendations = adapter.recommendLayout(info);

    Map<String, Map<String, Set<String>>> expectedNewLayout = ImmutableMap.of(
      "KAFKA",
      ImmutableMap.of("KAFKA_BROKER", ImmutableSet.of("c7402"))
    );

    assertEquals(expectedNewLayout, infoWithRecommendations.newServices());
  }


  private static Map<String, Map<String, Set<String>>> mutableCopy(Map<String, Map<String, Set<String>>> map) {
    Map<String, Map<String, Set<String>>> copy = new HashMap<>();
    map.entrySet().forEach( outer -> {
      Map<String, Set<String>> innerCopy = new HashMap<>(outer.getValue());
      copy.put(outer.getKey(), innerCopy);
    });
    return copy;
  }
}