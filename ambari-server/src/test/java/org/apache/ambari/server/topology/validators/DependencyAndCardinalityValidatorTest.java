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

import static org.junit.Assert.assertSame;

import org.apache.ambari.server.topology.InvalidTopologyException;
import org.apache.ambari.server.topology.ResolvedComponent;
import org.apache.ambari.server.topology.StackBuilder;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class DependencyAndCardinalityValidatorTest extends TopologyValidatorTest {

  public DependencyAndCardinalityValidatorTest() {
    subject = new DependencyAndCardinalityValidator();
  }

  @Test
  public void acceptsSpecifiedNumberOfInstances() throws InvalidTopologyException {
    topologyHas(2, aComponent().withCardinality("2"));
    replayAll();

    assertSame(topology, subject.validate(topology));
  }

  @Test
  public void acceptsComponentWithMpackInstanceSpecified() throws InvalidTopologyException {
    ResolvedComponent component = aComponent().withCardinality("2").lastAddedComponentWith("mpack_instance", "service_instance");
    topologyHas(2, component);
    replayAll();

    assertSame(topology, subject.validate(topology));
  }

  @Test
  public void acceptsMultipleInstances() throws InvalidTopologyException {
    StackBuilder stackBuilder = aComponent().withCardinality("1+");
    ResolvedComponent instance1 = stackBuilder.lastAddedComponentWith(null, "instance1");
    ResolvedComponent instance2 = stackBuilder.lastAddedComponentWith(null, "instance2");
    topologyHas(instance1, instance2);
    replayAll();

    assertSame(topology, subject.validate(topology));
  }

  @Test
  public void acceptsInstancesOnAllHosts() throws InvalidTopologyException {
    topologyHas(3, aComponent().withCardinality("ALL"));
    replayAll();

    assertSame(topology, subject.validate(topology));
  }

  @Test
  public void acceptsAbsentOptionalComponent() throws InvalidTopologyException {
    topologyHas(0, aComponent().withCardinality("0-1"), 1);
    replayAll();

    assertSame(topology, subject.validate(topology));
  }

  @Test(expected = InvalidTopologyException.class)
  public void rejectsComponentMissingFromHostGroup() throws InvalidTopologyException {
    topologyHas(2, aComponent().withCardinality("ALL"), 1);
    replayAll();

    subject.validate(topology);
  }

  @Test(expected = InvalidTopologyException.class)
  public void rejectsTooFewInstances() throws InvalidTopologyException {
    topologyHas(1, aComponent().withCardinality("2"));
    replayAll();

    subject.validate(topology);
  }

  @Test(expected = InvalidTopologyException.class)
  public void rejectsTooManyInstances() throws InvalidTopologyException {
    topologyHas(2, aComponent().withCardinality("1"));
    replayAll();

    subject.validate(topology);
  }

  @Test
  public void addsAutoDeployableComponentToAllHostGroups() throws InvalidTopologyException {
    StackBuilder stack = aComponent().withCardinality("ALL").autoDeploy(true);
    ResolvedComponent autoDeployable = stack.lastAddedComponent();
    topologyHas(2, stack.addComponent("component_in_same_service"));

    verifyAddedToAllHostGroupsWhereMissing(autoDeployable);
  }

  @Test
  public void addsAutoDeployableComponentToAllRemainingHostGroups() throws InvalidTopologyException {
    StackBuilder stack = aComponent().withCardinality("ALL").autoDeploy(true);
    ResolvedComponent component = stack.lastAddedComponent();
    topologyHas(2, stack, 3);

    verifyAddedToAllHostGroupsWhereMissing(component);
  }

  @Test
  public void doesNotAddAutoDeployableComponentIfServiceAbsentFromTopology() throws InvalidTopologyException {
    topologyHas(aComponent().withCardinality("1").autoDeploy(true).coLocateWith("other_service", "other_component").componentToBeCoLocatedWith());
    replayAll();

    assertSame(topology, subject.validate(topology));
  }

  @Test
  public void omitsNotAutoDeployableComponent() throws InvalidTopologyException {
    topologyHas(aComponent().autoDeploy(false).addComponent("other_component").lastAddedComponent());
    replayAll();

    assertSame(topology, subject.validate(topology));
  }

  @Test(expected = InvalidTopologyException.class)
  public void rejectsAutoDeployableComponentIfColocationUnspecified() throws InvalidTopologyException {
    topologyHas(aComponent().withCardinality("1").autoDeploy(true).addComponent("in_same_service").lastAddedComponent());
    replayAll();

    subject.validate(topology);
  }

  @Test
  public void addsAutoDeployableComponentInSameService() throws InvalidTopologyException {
    StackBuilder stack = aComponent().withCardinality("1").autoDeploy(true).coLocateWith("other_component");
    ResolvedComponent toBeAdded = stack.lastAddedComponent();
    ResolvedComponent otherComponent = stack.componentToBeCoLocatedWith();
    topologyHas(otherComponent);

    verifyAddedToFirstHostGroupWith(otherComponent, toBeAdded);
  }

  @Test
  public void addsAutoDeployableComponentToSingleHostGroup() throws InvalidTopologyException {
    String otherService = "other_service", otherComponent = "other_component";
    StackBuilder stack = aComponent().withCardinality("1").autoDeploy(true).coLocateWith(otherService, otherComponent);
    ResolvedComponent toBeAdded = stack.lastAddedComponent();
    ResolvedComponent component = stack.addComponent("in_same_service").lastAddedComponent();
    ResolvedComponent other = stack.componentOfType(otherService, otherComponent);
    topologyHas(ImmutableList.of(
      ImmutableSet.of(other),
      ImmutableSet.of(component),
      ImmutableSet.of()
    ));
    verifyAddedToFirstHostGroupWith(other, toBeAdded);
  }

  @Test
  public void addsAutoDeployableComponentToFirstHostGroup() throws InvalidTopologyException {
    String otherService = "other_service", otherComponent = "other_component";
    StackBuilder stack = aComponent().withCardinality("1").coLocateWith(otherService, otherComponent).autoDeploy(true);
    ResolvedComponent toBeAdded = stack.lastAddedComponent();
    ResolvedComponent component = stack.addComponent("in_same_service").lastAddedComponent();
    ResolvedComponent other = stack.componentOfType(otherService, otherComponent);
    topologyHas(ImmutableList.of(
      ImmutableSet.of(other),
      ImmutableSet.of(other),
      ImmutableSet.of(other),
      ImmutableSet.of(component)
    ));
    verifyAddedToFirstHostGroupWith(other, toBeAdded);
  }

  @Test(expected = InvalidTopologyException.class)
  public void rejectsAutoDeployableWithUnsupportedCardinality() throws InvalidTopologyException {
    ResolvedComponent otherComponent = aComponent().withCardinality("2").autoDeploy(true)
      .addComponent("other_component").lastAddedComponent();
    topologyHas(2, otherComponent);
    replayAll();

    subject.validate(topology);
  }

  @Test(expected = InvalidTopologyException.class)
  public void rejectsAutoDeployableWithoutCoLocatedComponent() throws InvalidTopologyException {
    topologyHas(1, aComponent().withCardinality("1").coLocateWith("missing_from_topology")
      .addComponent("present_in_topology"));
    replayAll();

    subject.validate(topology);
  }

  @Test
  public void addsDependencyToSameHostGroup() throws InvalidTopologyException {
    String dependencyName = "dependency";
    StackBuilder stack = aComponent().dependsOn(dependencyName).withScope("host", true);
    ResolvedComponent dependent = stack.lastAddedComponent();
    ResolvedComponent dependency = stack.componentOfType(dependencyName);
    topologyHas(3, stack, 2);
    verifyAddedToAllHostGroupsWith(dependent, dependency);
  }

  @Test
  public void addsCommonDependencyOnlyOnce() throws InvalidTopologyException {
    String dependency = "common_dependency";
    StackBuilder service = stack(STACK_ID).addService(SERVICE_NAME);
    topologyHas(
      service.addComponent("some_component").dependsOn(dependency).withScope("host", true).lastAddedComponent(),
      service.addComponent("other_component").dependsOn(dependency).withScope("host", true).lastAddedComponent()
    );

    verifyAddedToAllHostGroupsWhereMissing(service.componentOfType(dependency));
  }

  @Test
  public void addsDependencyFromEachStack() throws InvalidTopologyException {
    String dependencyService = "dependency_service", dependencyComponent = "common_dependency";
    StackBuilder someStack = stack(STACK_ID).addService("some_service").addComponent("some_component")
      .dependsOn(dependencyService, dependencyComponent).withScope("host", true);
    StackBuilder otherStack = stack(OTHER_STACK_ID).addService("other_service").addComponent("other_component")
      .dependsOn(dependencyService, dependencyComponent).withScope("host", true);
    topologyHas(
      someStack.lastAddedComponent(),
      otherStack.lastAddedComponent()
    );

    verifyAddedToAllHostGroupsWhereMissing(
      someStack.componentOfType(dependencyService, dependencyComponent),
      otherStack.componentOfType(dependencyService, dependencyComponent)
    );
  }

  @Test(expected = InvalidTopologyException.class)
  public void rejectsMissingDependencyDueToLackOfAutoDeploy() throws InvalidTopologyException {
    topologyHas(1, aComponent().dependsOn("dependency").withScope("host", false));
    replayAll();

    subject.validate(topology);
  }

  @Test(expected = InvalidTopologyException.class)
  public void rejectsMissingClusterLevelDependency() throws InvalidTopologyException {
    topologyHas(1, aComponent().dependsOn("dependency").withScope("cluster", true));
    replayAll();

    subject.validate(topology);
  }

  @Test
  public void acceptsSatisfiedClusterLevelDependency() throws InvalidTopologyException {
    String dependency = "dependency";
    StackBuilder stack = aComponent().dependsOn(dependency).withScope("cluster", false);
    ResolvedComponent dependent = stack.lastAddedComponent();
    ResolvedComponent other = stack.componentOfType(dependency);
    topologyHas(ImmutableList.of(
      ImmutableSet.of(dependent),
      ImmutableSet.of(other)
    ));
    replayAll();

    assertSame(topology, subject.validate(topology));
  }

}
