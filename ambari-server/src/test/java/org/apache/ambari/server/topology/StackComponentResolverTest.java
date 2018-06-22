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

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.ambari.server.controller.internal.StackDefinition;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.commons.lang3.tuple.Pair;
import org.easymock.EasyMock;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.MockType;
import org.easymock.TestSubject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Sets;

public class StackComponentResolverTest extends EasyMockSupport {

  private static final StackId STACK_ID1 = new StackId("HDPCORE", "1.0.0-b123");
  private static final StackId STACK_ID2 = new StackId("ODS", "1.0.0-b1111");
  private static final MpackInstance MPACK_INSTANCE1 = new MpackInstance(STACK_ID1);
  private static final MpackInstance MPACK_INSTANCE2 = new MpackInstance(STACK_ID2);

  private static final Comparator<ResolvedComponent> RESOLVED_COMPONENT_COMPARATOR = comparing((ResolvedComponent comp) -> comp.stackId().toString())
    .thenComparing(ResolvedComponent::effectiveServiceGroupName)
    .thenComparing(ResolvedComponent::effectiveServiceName)
    .thenComparing(ResolvedComponent::componentName);

  private static final Set<ResolvedComponent.Builder> HDP_CORE_COMPONENTS = hdpCoreComponents();
  private static final Set<ResolvedComponent.Builder> ODS_COMPONENTS = odsComponents();

  private static final Set<String> CLIENT_COMPONENTS = Sets.union(HDP_CORE_COMPONENTS, ODS_COMPONENTS).stream()
    .map(ResolvedComponent::componentName)
    .filter(each -> each.contains("_CLIENT"))
    .collect(toSet());

  @Rule
  public EasyMockRule rule = new EasyMockRule(this);

  @TestSubject
  private final ComponentResolver subject = new StackComponentResolver();

  @Mock(type = MockType.NICE)
  private BlueprintBasedClusterProvisionRequest request;

  @Mock(type = MockType.NICE)
  private StackDefinition stack;

  @Before
  public void setup() {
    expect(request.getStack()).andReturn(stack).anyTimes();
  }

  @Test(expected = IllegalArgumentException.class) // THEN
  public void ambiguousComponentName() {
    // GIVEN
    Set<ResolvedComponent> components = build(Sets.union(HDP_CORE_COMPONENTS, ODS_COMPONENTS));
    aStackWith(components);
    defineMpacksAs(MPACK_INSTANCE1, MPACK_INSTANCE2);
    aHostGroupWith(components);
    replayAll();

    // WHEN
    subject.resolveComponents(request);
  }

  @Test(expected = IllegalArgumentException.class) // THEN
  public void unknownComponent() {
    // GIVEN
    Set<ResolvedComponent> components = build(Sets.union(HDP_CORE_COMPONENTS, ODS_COMPONENTS));
    aStackWith(components);
    defineMpacksAs(MPACK_INSTANCE1, MPACK_INSTANCE2);
    aHostGroupWith(new Component("UNKNOWN_COMPONENT"));
    replayAll();

    // WHEN
    subject.resolveComponents(request);
  }

  @Test
  public void withExplicitNamesOnlyForClients() {
    // GIVEN
    Set<ResolvedComponent.Builder> builders = Sets.union(HDP_CORE_COMPONENTS, ODS_COMPONENTS);
    builders.stream().filter(each -> CLIENT_COMPONENTS.contains(each.componentName())).forEach(each -> each.serviceGroupName(each.stackId().getStackName()));
    Set<ResolvedComponent> components = build(builders);
    aStackWith(components);
    defineMpacksAs(MPACK_INSTANCE1, MPACK_INSTANCE2);
    aHostGroupWith(components);
    replayAll();

    // WHEN
    Map<String, Set<ResolvedComponent>> resolved = subject.resolveComponents(request);

    // THEN
    assertHostGroupEquals(ImmutableMap.of("node", components), resolved);
  }

  @Test
  public void withExplicitDefaultNames() {
    // GIVEN
    String mpackInstanceName1 = STACK_ID1.getStackName(), mpackInstanceName2 = STACK_ID2.getStackName();
    Set<ResolvedComponent> components = build(Sets.union(
      inServiceGroup(mpackInstanceName1, HDP_CORE_COMPONENTS),
      inServiceGroup(mpackInstanceName2, ODS_COMPONENTS)
    ));

    aStackWith(components);

    defineMpacksAs(
      withCustomName(MPACK_INSTANCE1, mpackInstanceName1),
      withCustomName(MPACK_INSTANCE2, mpackInstanceName2)
    );

    aHostGroupWith(components);
    replayAll();

    // WHEN
    Map<String, Set<ResolvedComponent>> resolved = subject.resolveComponents(request);

    // THEN
    assertHostGroupEquals(ImmutableMap.of("node", components), resolved);
  }

  @Test
  public void withCustomNames() {
    // GIVEN
    String mpackInstanceName1 = "hdp-core", mpackInstanceName2 = "ods!";
    Set<ResolvedComponent> components = build(Sets.union(
      inServiceGroup(mpackInstanceName1, HDP_CORE_COMPONENTS),
      inServiceGroup(mpackInstanceName2, ODS_COMPONENTS)
    ));

    aStackWith(components);

    defineMpacksAs(
      withCustomName(MPACK_INSTANCE1, mpackInstanceName1),
      withCustomName(MPACK_INSTANCE2, mpackInstanceName2)
    );

    aHostGroupWith(components);
    replayAll();

    // WHEN
    Map<String, Set<ResolvedComponent>> resolved = subject.resolveComponents(request);

    // THEN
    assertHostGroupEquals(ImmutableMap.of("node", components), resolved);
  }

  private void defineMpacksAs(MpackInstance... mpacks) {
    expect(request.getMpacks()).andReturn(ImmutableSet.copyOf(mpacks)).anyTimes();
  }

  private void aStackWith(Set<ResolvedComponent> components) {
    Map<Pair<String, String>, List<ResolvedComponent>> byServiceName = components.stream()
      .collect(groupingBy(each -> Pair.of(each.effectiveServiceGroupName(), each.effectiveServiceName())));

    Map<ResolvedComponent, ServiceInfo> services = new HashMap<>();
    for (Map.Entry<Pair<String, String>, List<ResolvedComponent>> entry : byServiceName.entrySet()) {
      ServiceInfo service = createNiceMock(ServiceInfo.class);
      expect(service.getName()).andReturn(entry.getValue().iterator().next().serviceType()).anyTimes();
      for (ResolvedComponent component : entry.getValue()) {
        services.put(component, service);
        ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);
        expect(service.getComponentByName(component.componentName())).andReturn(componentInfo).anyTimes();
      }
    }

    Map<String, List<Pair<ResolvedComponent, ServiceInfo>>> byComponentName = components.stream()
      .map(each -> Pair.of(each, services.get(each)))
      .collect(groupingBy(each -> each.getLeft().componentName()));

    for (Map.Entry<String, List<Pair<ResolvedComponent, ServiceInfo>>> entry : byComponentName.entrySet()) {
      expect(stack.getServicesForComponent(entry.getKey()))
        .andAnswer(() -> entry.getValue().stream().map(each -> Pair.of(each.getLeft().stackId(), each.getRight()))).anyTimes();
    }
    expect(stack.getServicesForComponent(anyString())).andAnswer(Stream::empty).anyTimes();
  }

  private void aHostGroupWith(Component component) {
    hostGroupWith(ImmutableSet.of(component));
  }

  private void aHostGroupWith(Collection<ResolvedComponent> components) {
    hostGroupWith(components.stream()
      .map(each -> new Component(each.componentName(), each.serviceGroupName().orElse(null), each.effectiveServiceName(), null))
      .collect(toSet()));
  }

  private void hostGroupWith(Set<Component> components) {
    HostGroup hostGroup = new HostGroupImpl("node", components, Configuration.createEmpty(), "1+");
    Map<String, HostGroup> hostGroups = ImmutableMap.of(hostGroup.getName(), hostGroup);
    expect(request.getHostGroups()).andReturn(hostGroups).anyTimes();
  }

  private static Set<ResolvedComponent> build(Set<ResolvedComponent.Builder> builders) {
    builders.forEach(each ->
      each.component(new Component(each.componentName(), each.effectiveServiceGroupName(), each.effectiveServiceName(), null)));

    return builders.stream()
      .map(ResolvedComponent.Builder::buildPartial)
      .collect(toSet());
  }

  private static MpackInstance withCustomName(MpackInstance mpack, String name) {
    return new MpackInstance(name, mpack.getMpackType(), mpack.getMpackVersion(), mpack.getUrl(), mpack.getConfiguration());
  }

  private static Set<ResolvedComponent.Builder> inServiceGroup(String name, Collection<ResolvedComponent.Builder> components) {
    return components.stream().map(each -> each.serviceGroupName(name)).collect(toSet());
  }

  private static void assertHostGroupEquals(Map<String, Set<ResolvedComponent>> expected, Map<String, Set<ResolvedComponent>> actual) {
    assertEquals(expected.keySet(), actual.keySet());
    for (String hostGroupName : expected.keySet()) {
      Set<ResolvedComponent> expectedComponents = ImmutableSortedSet.copyOf(RESOLVED_COMPONENT_COMPARATOR, expected.get(hostGroupName));
      Set<ResolvedComponent> actualComponents = ImmutableSortedSet.copyOf(RESOLVED_COMPONENT_COMPARATOR, actual.get(hostGroupName));
      assertEquals(expectedComponents, actualComponents);
    }
  }

  private static Set<ResolvedComponent.Builder> hdpCoreComponents() {
    Set<ResolvedComponent.Builder> builders = ImmutableSet.of(
      builderFor("HADOOP_CLIENTS", "HADOOP_CLIENT"),
      builderFor("HDFS", "DATANODE"),
      builderFor("HDFS", "NAMENODE"),
      builderFor("ZOOKEEPER_CLIENTS", "ZOOKEEPER_CLIENT"),
      builderFor("ZOOKEEPER", "ZOOKEEPER_SERVER")
    );
    builders.forEach(each -> each.stackId(STACK_ID1));
    return builders;
  }

  private static Set<ResolvedComponent.Builder> odsComponents() {
    Set<ResolvedComponent.Builder> builders = ImmutableSet.of(
      builderFor("HADOOP_CLIENTS", "HADOOP_CLIENT"),
      builderFor("HBASE", "HBASE_MASTER"),
      builderFor("HBASE", "HBASE_REGIONSERVER"),
      builderFor("HBASE_CLIENTS", "HBASE_CLIENT"),
      builderFor("ZOOKEEPER_CLIENTS", "ZOOKEEPER_CLIENT")
    );
    builders.forEach(each -> each.stackId(STACK_ID2));
    return builders;
  }

  public static ResolvedComponent.Builder builderFor(String serviceType, String componentType) {
    return builderFor(serviceType, componentType, false);
  }

  public static ResolvedComponent.Builder builderFor(String serviceType, String componentType, boolean masterComponent) {
    ComponentInfo componentInfo = EasyMock.createNiceMock(ComponentInfo.class);
    expect(componentInfo.getName()).andReturn(componentType).anyTimes();
    expect(componentInfo.isMaster()).andReturn(masterComponent).anyTimes();

    ServiceInfo serviceInfo = EasyMock.createNiceMock(ServiceInfo.class);
    expect(serviceInfo.getName()).andReturn(serviceType).anyTimes();
    expect(serviceInfo.getComponentByName(componentType)).andReturn(componentInfo).anyTimes();
    expect(serviceInfo.getComponents()).andReturn(ImmutableList.of(componentInfo)).anyTimes();

    replay(componentInfo, serviceInfo);

    return ResolvedComponent.builder(new Component(componentType))
      .serviceInfo(serviceInfo)
      .componentInfo(componentInfo);
  }

}
