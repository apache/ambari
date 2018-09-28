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
package org.apache.ambari.server.topology.validators;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.newCapture;
import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.controller.internal.StackDefinition;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.ClusterTopologyImpl;
import org.apache.ambari.server.topology.Configuration;
import org.apache.ambari.server.topology.InvalidTopologyException;
import org.apache.ambari.server.topology.ResolvedComponent;
import org.apache.ambari.server.topology.StackBuilder;
import org.easymock.Capture;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.IAnswer;
import org.easymock.Mock;
import org.easymock.MockType;
import org.easymock.TestSubject;
import org.junit.Before;
import org.junit.Rule;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * Base class for TopologyValidator tests.
 */
public class TopologyValidatorTest extends EasyMockSupport {

  protected static final StackId STACK_ID = new StackId("HDP", "2.6");
  protected static final StackId OTHER_STACK_ID = new StackId("BigData", "1.0");
  protected static final String SERVICE_NAME = "service";
  protected static final String COMPONENT_NAME = "component";
  private final Map<StackId, StackBuilder> stackMap = new LinkedHashMap<>();

  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);

  @Mock(type = MockType.NICE)
  protected ClusterTopology topology;

  @TestSubject
  protected TopologyValidator subject;

  @Before
  public void setup() throws Exception {
    stackMap.clear();

    expect(topology.getBlueprintName()).andReturn("blue").anyTimes();
    expect(topology.getConfiguration()).andReturn(Configuration.createEmpty()).anyTimes();
    expect(topology.withAdditionalComponents(ImmutableMap.of())).andReturn(topology).anyTimes();
  }

  protected StackBuilder aComponent() {
    return stack(STACK_ID).addService(SERVICE_NAME).addComponent(COMPONENT_NAME);
  }

  /**
   * Creates a topology with one kind of host group: {@code hostGroupsWith} groups have {@code onlyComponent}.
   */
  protected void topologyHas(int hostGroupsWith, ResolvedComponent onlyComponent) {
    topologyHas(hostGroupsWith, ImmutableSet.of(onlyComponent), 0, ImmutableSet.of());
  }

  /**
   * Creates a topology with 2 kinds of host groups: {@code hostGroupsWith} groups have {@code components},
   * while {@code otherHostGroups} groups have {@code otherComponents}.
   */
  protected void topologyHas(int hostGroupsWith, Set<ResolvedComponent> components, int otherHostGroups, Set<ResolvedComponent> otherComponents) {
    Stream<Set<ResolvedComponent>> hostGroupStream = IntStream.range(0, hostGroupsWith)
      .mapToObj(__ -> components);

    if (otherHostGroups > 0) {
      hostGroupStream = Stream.concat(hostGroupStream,
        IntStream.range(0, otherHostGroups)
          .mapToObj(__ -> otherComponents));
    }

    topologyHas(hostGroupStream.collect(toList()));
  }

  /**
   * Creates a topology with one kind of host group: {@code hostGroupsWith} groups have the last component
   * added to the {@code stack}.
   */
  protected void topologyHas(int hostGroupsWith, StackBuilder stack) {
    topologyHas(hostGroupsWith, stack.lastAddedComponent());
  }

  /**
   * Creates a topology with 2 kinds of host groups: {@code hostGroupsWith} groups have the last component
   * added to the {@code stack}, while {@code otherHostGroups} groups have some newly defined component
   * from the same service.
   */
  protected void topologyHas(int hostGroupsWith, StackBuilder stack, int otherHostGroups) {
    ResolvedComponent component = stack.lastAddedComponent();
    ResolvedComponent other = stack.addComponent("don't_care").lastAddedComponent();
    topologyHas(hostGroupsWith, ImmutableSet.of(component), otherHostGroups, ImmutableSet.of(other));
  }

  /**
   * Creates a topology with a single host group with the given components.
   */
  protected void topologyHas(ResolvedComponent... components) {
    topologyHas(ImmutableSet.copyOf(components));
  }

  /**
   * Creates a topology with a single host group with the given set of components.
   */
  protected void topologyHas(Set<ResolvedComponent> components) {
    topologyHas(ImmutableList.of(ImmutableSet.copyOf(components)));
  }

  /**
   * Sets up expectations for the ClusterTopology to be validated.
   * Each host group is given with a set of components.  Group names will be assigned sequentially.
   */
  protected void topologyHas(List<Set<ResolvedComponent>> components) {
    Map<String, Set<ResolvedComponent>> hostGroups = IntStream.range(0, components.size())
      .collect(HashMap::new, (map, i) -> map.put("host_group_" + i, components.get(i)), Map::putAll);

    expect(topology.getComponents())
      .andAnswer(() -> components.stream().flatMap(Collection::stream))
      .anyTimes();
    Capture<ResolvedComponent> componentCapture = newCapture();
    expect(topology.getHostGroupsForComponent(capture(componentCapture)))
      .andAnswer(() -> ClusterTopologyImpl.getHostGroupsForComponent(hostGroups, componentCapture.getValue()))
      .anyTimes();
    expect(topology.getHostGroups()).andReturn(hostGroups.keySet()).anyTimes();

    Capture<String> hostGroupCapture = newCapture();
    expect(topology.getComponentsInHostGroup(capture(hostGroupCapture)))
      .andAnswer(() -> hostGroups.get(hostGroupCapture.getValue()).stream())
      .anyTimes();

    Set<StackId> stackIds = components.stream()
      .flatMap(Collection::stream)
      .map(ResolvedComponent::stackId)
      .collect(toSet());
    expect(topology.getStack()).andAnswer(cachedStackDefinition(stackIds)).anyTimes();
  }

  // verification helpers

  protected void verifyAddedToAllHostGroupsWhereMissing(ResolvedComponent... components) throws InvalidTopologyException {
    verifyTopologyUpdated(() -> {
      Map<String, Set<ResolvedComponent>> expectedAdditionalComponents = new TreeMap<>();
      for (ResolvedComponent component : components) {
        Set<String> hostGroupsWhereMissing = Sets.difference(topology.getHostGroups(), topology.getHostGroupsForComponent(component));
        for (String hostGroup : hostGroupsWhereMissing) {
          expectedAdditionalComponents.computeIfAbsent(hostGroup, __ -> new LinkedHashSet<>())
            .add(component);
        }
      }
      return expectedAdditionalComponents;
    });
  }

  protected void verifyAddedToFirstHostGroupWith(ResolvedComponent existingComponent, ResolvedComponent toBeAdded) throws InvalidTopologyException {
    verifyTopologyUpdated(() -> {
      String firstHostGroup = topology.getHostGroupsForComponent(existingComponent).iterator().next();
      return ImmutableMap.of(firstHostGroup, ImmutableSet.of(toBeAdded));
    });
  }

  protected void verifyAddedToAllHostGroupsWith(ResolvedComponent existingComponent, ResolvedComponent toBeAdded) throws InvalidTopologyException {
    verifyTopologyUpdated(() ->
      topology.getHostGroupsForComponent(existingComponent).stream()
        .collect(toMap(Function.identity(), __ -> ImmutableSet.of(toBeAdded)))
    );
  }

  protected void verifyTopologyUpdated(Supplier<Map<String, Set<ResolvedComponent>>> topologyUpdateSupplier) throws InvalidTopologyException {
    // WHEN
    ClusterTopology updatedTopology = createNiceMock(ClusterTopology.class);
    Capture<Map<String, Set<ResolvedComponent>>> additionalComponentsCapture = newCapture();
    expect(topology.withAdditionalComponents(capture(additionalComponentsCapture))).andReturn(updatedTopology);
    replayAll();
    // expected update needs to be calculated after mock topology was replayed
    Map<String, Set<ResolvedComponent>> expectedAdditionalComponents = topologyUpdateSupplier.get();

    // WHEN
    subject.validate(topology);

    // THEN
    assertEquals(expectedAdditionalComponents, additionalComponentsCapture.getValue());
  }

  protected IAnswer<StackDefinition> cachedStackDefinition(Set<StackId> stackIds) {
    // stack definition needs to be created lazily,
    // because Stack copies data from StackInfo,
    // and StackInfo is only populated later in each test case
    return new IAnswer<StackDefinition>() {
      private StackDefinition stackDefinition;
      @Override
      public StackDefinition answer() {
        if (stackDefinition == null) {
          Set<Stack> stacks = stackMap.entrySet().stream()
            .filter(each -> stackIds.contains(each.getKey()))
            .map(each -> new Stack(each.getValue().stackInfo()))
            .collect(toSet());
          stackDefinition = StackDefinition.of(stacks);
        }
        return stackDefinition;
      }
    };
  }

  /**
   * Creates a new builder for the given stack, if not already present.
   */
  protected StackBuilder stack(StackId stackId) {
    return stackMap.computeIfAbsent(stackId, __ -> new StackBuilder(stackId));
  }

}
