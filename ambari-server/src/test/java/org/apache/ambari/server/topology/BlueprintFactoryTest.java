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

import static java.util.stream.Collectors.toSet;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.createStrictMock;
import static org.powermock.api.easymock.PowerMock.replay;
import static org.powermock.api.easymock.PowerMock.reset;
import static org.powermock.api.easymock.PowerMock.verify;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.ambari.server.controller.internal.BlueprintResourceProvider;
import org.apache.ambari.server.controller.internal.BlueprintResourceProviderTest;
import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.orm.dao.BlueprintDAO;
import org.apache.ambari.server.orm.entities.BlueprintConfigEntity;
import org.apache.ambari.server.orm.entities.BlueprintEntity;
import org.apache.ambari.server.state.StackId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.util.Providers;

/**
 * BlueprintFactory unit tests.
 */
public class BlueprintFactoryTest {

  private static final String BLUEPRINT_NAME = "test-blueprint";

  private Stack stack = createNiceMock(Stack.class);
  private BlueprintDAO dao = createStrictMock(BlueprintDAO.class);
  private BlueprintEntity entity = createStrictMock(BlueprintEntity.class);
  private BlueprintConfigEntity configEntity = createStrictMock(BlueprintConfigEntity.class);
  private BlueprintFactory testFactory = new BlueprintFactory(Providers.of(dao));

  @Before
  public void init() throws Exception {
    Set<StackId> stackIds = ImmutableSet.of(new StackId("stack", "0.1"));
    Collection<String> services = ImmutableSet.of("test-service1", "test-service2");
    Collection<String> components = ImmutableSet.of("component1", "component2");

    expect(stack.getServices()).andReturn(services).anyTimes();
    expect(stack.getComponents()).andReturn(components).anyTimes();
    expect(stack.isMasterComponent("component1")).andReturn(true).anyTimes();
    expect(stack.isMasterComponent("component2")).andReturn(false).anyTimes();
    expect(stack.getServiceForComponent("component1")).andReturn("test-service1").anyTimes();
    expect(stack.getServiceForComponent("component2")).andReturn("test-service2").anyTimes();
    expect(stack.getStacksForService(anyString())).andReturn(stackIds).anyTimes();
    expect(stack.getStacksForComponent(anyString())).andReturn(stackIds).anyTimes();
  }

  @After
  public void tearDown() {
    reset(stack, dao, entity, configEntity);
  }

  @Test
  public void testGetMultiInstanceBlueprint() throws Exception {
    // prepare
    Blueprint expectedBlueprint = createMultiInstanceBlueprint();

    reset(dao);
    expect(dao.findByName(BLUEPRINT_NAME)).andReturn(expectedBlueprint.toEntity());
    Stack hdpStack = createNiceMock(Stack.class);
    StackId hdp = new StackId("HDPCORE-3.0", "3.0.0.0");
    StackId edw = new StackId("EDW-3.1", "3.1.0.0");
    expect(hdpStack.getName()).andReturn(hdp.getStackName()).anyTimes();
    expect(hdpStack.getVersion()).andReturn(hdp.getStackVersion()).anyTimes();
    Stack edwStack = createNiceMock(Stack.class);
    expect(edwStack.getName()).andReturn(edw.getStackName()).anyTimes();
    expect(edwStack.getVersion()).andReturn(edw.getStackVersion()).anyTimes();
    replay(hdpStack, edwStack, dao);

    // test
    Blueprint blueprint = testFactory.getBlueprint(BLUEPRINT_NAME);
    Set<String> mpackNames =
      blueprint.getMpacks().stream().map(MpackInstance::getMpackName).collect(Collectors.toSet());
    assertEquals(ImmutableSet.of("HDPCORE-3.0", "EDW-3.1"), mpackNames );
    Optional<MpackInstance> hdpCore = blueprint.getMpacks().stream()
      .filter(mp -> "HDPCORE-3.0".equals(mp.getMpackName()))
      .findAny();
    assertTrue(hdpCore.isPresent());
    MpackInstance mpackInstance = hdpCore.get();
    Set<String> serviceInstanceNames =
      mpackInstance.getServiceInstances().stream().map(ServiceInstance::getName).collect(toSet());
    assertEquals(ImmutableSet.of("ZK1", "ZK2"), serviceInstanceNames);
    Set<String> serviceInstanceTypes =
      mpackInstance.getServiceInstances().stream().map(ServiceInstance::getType).collect(toSet());
    assertEquals(ImmutableSet.of("ZOOKEEPER"), serviceInstanceTypes);
    Set<StackId> stackIds = blueprint.getStackIds();
    assertEquals(ImmutableSet.of(hdp, edw), stackIds);
    assertEquals(1, blueprint.getHostGroups().size());
  }

  @Test
  public void testGetBlueprint_NotFound() throws Exception {
    expect(dao.findByName(BLUEPRINT_NAME)).andReturn(null).once();
    replay(dao, entity, configEntity);

    BlueprintFactory factory = new BlueprintFactory(Providers.of(dao));
    assertNull(factory.getBlueprint(BLUEPRINT_NAME));
  }

  @Test
  public void testCreateBlueprint() throws Exception {
    Map<String, Object> props = BlueprintResourceProviderTest.getBlueprintTestProperties().iterator().next();

    replay(stack, dao, entity, configEntity);
    Blueprint blueprint = testFactory.createBlueprint(props, null);

    assertEquals(BLUEPRINT_NAME, blueprint.getName());
    assertEquals(2, blueprint.getHostGroups().size());

    Map<String, HostGroup> hostGroups = blueprint.getHostGroups();
    HostGroup group1 = hostGroups.get("group1");
    assertEquals("group1", group1.getName());
    assertEquals("1", group1.getCardinality());
    Collection<String> components = group1.getComponentNames();
    assertEquals(2, components.size());
    assertTrue(components.contains("component1"));
    assertTrue(components.contains("component2"));
    //todo: add configurations/attributes to properties
    Configuration configuration = group1.getConfiguration();
    assertTrue(configuration.getProperties().isEmpty());
    assertTrue(configuration.getAttributes().isEmpty());

    HostGroup group2 = hostGroups.get("group2");
    assertEquals("group2", group2.getName());
    assertEquals("2", group2.getCardinality());
    components = group2.getComponentNames();
    assertEquals(1, components.size());
    assertTrue(components.contains("component1"));
    //todo: add configurations/attributes to properties
    //todo: test both v1 and v2 config syntax
    configuration = group2.getConfiguration();
    assertTrue(configuration.getProperties().isEmpty());
    assertTrue(configuration.getAttributes().isEmpty());

    verify(dao, entity, configEntity);
  }

  @Test
  public void testCreateMultiInstanceBlueprint() throws Exception {
    createMultiInstanceBlueprint();
  }

  public Blueprint createMultiInstanceBlueprint() throws Exception {
    Collection<String> allComponents = ImmutableSet.of("NAMENODE", "SECONDARY_NAMENODE", "ZOOKEEPER_SERVER");
    Collection<String> services = ImmutableSet.of("HDFS", "ZOOKEEPER");
    Set<StackId> stackIds = ImmutableSet.of(new StackId("HDPCORE-3.0", "3.0.0.0"));
    reset(stack);
    expect(stack.getServices()).andReturn(services).anyTimes();
    expect(stack.getComponents()).andReturn(allComponents).anyTimes();
    expect(stack.isMasterComponent("NAMENODE")).andReturn(true).anyTimes();
    expect(stack.isMasterComponent("ZOOKEEPER_SERVER")).andReturn(true).anyTimes();
    expect(stack.isMasterComponent("SECONDAY_NAMENODE")).andReturn(false).anyTimes();
    expect(stack.getServiceForComponent("NAMENODE")).andReturn("HDFS").anyTimes();
    expect(stack.getServiceForComponent("SECONDARY_NAMENODE")).andReturn("HDFS").anyTimes();
    expect(stack.getServiceForComponent("ZOOKEEPER_SERVER")).andReturn("ZOOKEEPER").anyTimes();
    expect(stack.getStacksForService(anyString())).andReturn(stackIds).anyTimes();
    expect(stack.getStacksForComponent(anyString())).andReturn(stackIds).anyTimes();

    replay(stack, dao, entity, configEntity);

    Map<String, Object> props = BlueprintTestUtil.getMultiInstanceBlueprintAsMap();
    Blueprint blueprint = testFactory.createBlueprint(props, null);

    assertEquals(2, blueprint.getMpacks().size());
    assertEquals(Sets.newHashSet("HDPCORE-3.0", "EDW-3.1"),
      blueprint.getMpacks().stream().map(MpackInstance::getMpackName).collect(toSet()));
    MpackInstance hdpCore =
      blueprint.getMpacks().stream().filter( mpack -> "HDPCORE-3.0".equals(mpack.getMpackName()) ).findFirst().get();
    assertEquals(2, hdpCore.getServiceInstances().size());
    ServiceInstance zk1 =
      hdpCore.getServiceInstances().stream().filter( si -> "ZK1".equals(si.getName()) ).findFirst().get();
    assertEquals("ZOOKEEPER", zk1.getType());
    assertEquals("/zookeeper1", zk1.getConfiguration().getProperties().get("zoo.cfg").get("dataDir"));
    return blueprint;
  }

  @Test(expected=IllegalArgumentException.class)
  public void testCreate_NoBlueprintName() throws Exception {
    Map<String, Object> props = BlueprintResourceProviderTest.getBlueprintTestProperties().iterator().next();
    props.remove(BlueprintResourceProvider.BLUEPRINT_NAME_PROPERTY_ID);

    replay(stack, dao, entity, configEntity);
    testFactory.createBlueprint(props, null);
  }

  @Test(expected=IllegalArgumentException.class)
  public void testCreate_NoHostGroups() throws Exception {
    Map<String, Object> props = BlueprintResourceProviderTest.getBlueprintTestProperties().iterator().next();
    // remove all host groups
    ((Set<Map<String, Object>>) props.get(BlueprintResourceProvider.HOST_GROUP_PROPERTY_ID)).clear();

    replay(stack, dao, entity, configEntity);
    testFactory.createBlueprint(props, null);
  }

  @Test(expected=IllegalArgumentException.class)
  public void testCreate_MissingHostGroupName() throws Exception {
    Map<String, Object> props = BlueprintResourceProviderTest.getBlueprintTestProperties().iterator().next();
    // remove the name property for one of the host groups
    ((Set<Map<String, Object>>) props.get(BlueprintResourceProvider.HOST_GROUP_PROPERTY_ID)).iterator().next().remove("name");

    replay(stack, dao, entity, configEntity);
    testFactory.createBlueprint(props, null);
  }

  @Test(expected=IllegalArgumentException.class)
  public void testCreate_HostGroupWithNoComponents() throws Exception {
    Map<String, Object> props = BlueprintResourceProviderTest.getBlueprintTestProperties().iterator().next();
    // remove the components for one of the host groups
    ((Set<Map<String, Object>>) props.get(BlueprintResourceProvider.HOST_GROUP_PROPERTY_ID)).
        iterator().next().remove(BlueprintResourceProvider.COMPONENT_PROPERTY_ID);

    replay(stack, dao, entity, configEntity);
    testFactory.createBlueprint(props, null);
  }

  @Test(expected = IllegalArgumentException.class) // THEN
  public void verifyDefinitionsDisjointShouldRejectDuplication() {
    // GIVEN
    final String service1 = "unique service";
    final String service2 = "duplicated service";
    StackId stack1 = new StackId("a_stack", "1.0");
    StackId stack2 = new StackId("another_stack", "0.9");
    Stream<String> stream = ImmutableSet.of(service1, service2).stream();

    // WHEN
    BlueprintFactory.verifyStackDefinitionsAreDisjoint(stream, "Services", service -> {
      switch (service) {
        case service1: return ImmutableSet.of(stack1);
        case service2: return ImmutableSet.of(stack1, stack2);
        default: return null;
      }
    });
  }

  @Test
  public void verifyStackDefinitionsAreDisjointShouldAllowDisjointStacks() {
    // GIVEN
    final String service1 = "unique service";
    final String service2 = "another service";
    StackId stack1 = new StackId("a_stack", "1.0");
    StackId stack2 = new StackId("another_stack", "0.9");
    Stream<String> stream = ImmutableSet.of(service1, service2).stream();

    // WHEN
    BlueprintFactory.verifyStackDefinitionsAreDisjoint(stream, "Services", service -> {
      switch (service) {
        case service1: return ImmutableSet.of(stack1);
        case service2: return ImmutableSet.of(stack2);
        default: return null;
      }
    });

    // THEN
    // no exception expected
  }

}
