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
import org.apache.ambari.server.orm.entities.BlueprintEntity;
import org.apache.ambari.server.state.SecurityType;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Blueprint unit tests.
 */
public class BlueprintImplTest {

  private static final Map<String, Map<String, Map<String, String>>> EMPTY_ATTRIBUTES =
      new HashMap<String, Map<String, Map<String, String>>>();

  private static final Map<String, Map<String, String>> EMPTY_PROPERTIES =
      new HashMap<String, Map<String, String>>();

  private static final Configuration EMPTY_CONFIGURATION = new Configuration(EMPTY_PROPERTIES, EMPTY_ATTRIBUTES);



  @Test
  public void testValidateConfigurations__basic_positive() throws Exception {

    Stack stack = createNiceMock(Stack.class);

    HostGroup group1 = createMock(HostGroup.class);
    HostGroup group2 = createMock(HostGroup.class);
    Collection<HostGroup> hostGroups = new HashSet<HostGroup>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    Collection<String> group1Components = new HashSet<String>();
    group1Components.add("c1");
    group1Components.add("c2");

    Set<String> group2Components = new HashSet<String>();
    group2Components.add("c1");
    group2Components.add("c3");

    Collection<Stack.ConfigProperty> requiredHDFSProperties = new HashSet<Stack.ConfigProperty>();
    requiredHDFSProperties.add(new Stack.ConfigProperty("hdfs-site", "foo", null));
    requiredHDFSProperties.add(new Stack.ConfigProperty("hdfs-site", "bar", null));
    requiredHDFSProperties.add(new Stack.ConfigProperty("hdfs-site", "some_password", null));

    requiredHDFSProperties.add(new Stack.ConfigProperty("category1", "prop1", null));

    Collection<Stack.ConfigProperty> requiredService2Properties = new HashSet<Stack.ConfigProperty>();
    requiredService2Properties.add(new Stack.ConfigProperty("category2", "prop2", null));

    expect(stack.getServiceForComponent("c1")).andReturn("HDFS").atLeastOnce();
    expect(stack.getServiceForComponent("c2")).andReturn("HDFS").atLeastOnce();
    expect(stack.getServiceForComponent("c3")).andReturn("SERVICE2").atLeastOnce();

    expect(stack.getRequiredConfigurationProperties("HDFS")).andReturn(requiredHDFSProperties).atLeastOnce();
    expect(stack.getRequiredConfigurationProperties("SERVICE2")).andReturn(requiredService2Properties).atLeastOnce();

    expect(stack.isPasswordProperty("HDFS", "hdfs-site", "foo")).andReturn(false).atLeastOnce();
    expect(stack.isPasswordProperty("HDFS", "hdfs-site", "bar")).andReturn(false).atLeastOnce();
    expect(stack.isPasswordProperty("HDFS", "hdfs-site", "some_password")).andReturn(true).atLeastOnce();
    expect(stack.isPasswordProperty("HDFS", "category1", "prop1")).andReturn(false).atLeastOnce();
    expect(stack.isPasswordProperty("SERVICE2", "category2", "prop2")).andReturn(false).atLeastOnce();

    expect(group1.getConfiguration()).andReturn(EMPTY_CONFIGURATION).atLeastOnce();
    expect(group1.getName()).andReturn("group1").anyTimes();
    expect(group1.getComponentNames()).andReturn(group1Components).atLeastOnce();
    expect(group1.getCardinality()).andReturn("1").atLeastOnce();
    expect(group1.getComponents()).andReturn(Arrays.asList(new Component("c1"), new Component("c2"))).atLeastOnce();

    expect(group2.getConfiguration()).andReturn(EMPTY_CONFIGURATION).atLeastOnce();
    expect(group2.getName()).andReturn("group2").anyTimes();
    expect(group2.getComponentNames()).andReturn(group2Components).atLeastOnce();
    expect(group2.getCardinality()).andReturn("1").atLeastOnce();
    expect(group2.getComponents()).andReturn(Arrays.asList(new Component("c1"), new Component("c3"))).atLeastOnce();

    replay(stack, group1, group2);

    // Blueprint config
    Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();
    Map<String, String> hdfsProps = new HashMap<String, String>();
    properties.put("hdfs-site", hdfsProps);
    hdfsProps.put("foo", "val");
    hdfsProps.put("bar", "val");

    Map<String, String> category1Props = new HashMap<String, String>();
    properties.put("category1", category1Props);
    category1Props.put("prop1", "val");

    Map<String, String> category2Props = new HashMap<String, String>();
    properties.put("category2", category2Props);
    category2Props.put("prop2", "val");

    Map<String, Map<String, Map<String, String>>> attributes = new HashMap<String, Map<String, Map<String, String>>>();
    // for this basic test not ensuring that stack properties are ignored, this is tested in another test
    Configuration configuration = new Configuration(properties, attributes, EMPTY_CONFIGURATION);

    SecurityConfiguration securityConfiguration = new SecurityConfiguration(SecurityType.KERBEROS, "testRef", null);
    Blueprint blueprint = new BlueprintImpl("test", hostGroups, stack, configuration, securityConfiguration);
    blueprint.validateRequiredProperties();
    BlueprintEntity entity = blueprint.toEntity();

    verify(stack, group1, group2);
    assertTrue(entity.getSecurityType() == SecurityType.KERBEROS);
    assertTrue(entity.getSecurityDescriptorReference().equals("testRef"));
  }

  @Test
  public void testValidateConfigurations__basic_negative() throws Exception {

    Stack stack = createNiceMock(Stack.class);

    HostGroup group1 = createNiceMock(HostGroup.class);
    HostGroup group2 = createNiceMock(HostGroup.class);
    Collection<HostGroup> hostGroups = new HashSet<HostGroup>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    Collection<String> group1Components = new HashSet<String>();
    group1Components.add("c1");
    group1Components.add("c2");

    Collection<String> group2Components = new HashSet<String>();
    group2Components.add("c1");
    group2Components.add("c3");

    Collection<Stack.ConfigProperty> requiredHDFSProperties = new HashSet<Stack.ConfigProperty>();
    requiredHDFSProperties.add(new Stack.ConfigProperty("hdfs-site", "foo", null));
    requiredHDFSProperties.add(new Stack.ConfigProperty("hdfs-site", "bar", null));
    requiredHDFSProperties.add(new Stack.ConfigProperty("hdfs-site", "some_password", null));

    requiredHDFSProperties.add(new Stack.ConfigProperty("category1", "prop1", null));

    Collection<Stack.ConfigProperty> requiredService2Properties = new HashSet<Stack.ConfigProperty>();
    requiredService2Properties.add(new Stack.ConfigProperty("category2", "prop2", null));

    expect(stack.getServiceForComponent("c1")).andReturn("HDFS").atLeastOnce();
    expect(stack.getServiceForComponent("c2")).andReturn("HDFS").atLeastOnce();
    expect(stack.getServiceForComponent("c3")).andReturn("SERVICE2").atLeastOnce();

    expect(stack.getRequiredConfigurationProperties("HDFS")).andReturn(requiredHDFSProperties).atLeastOnce();
    expect(stack.getRequiredConfigurationProperties("SERVICE2")).andReturn(requiredService2Properties).atLeastOnce();

    expect(stack.isPasswordProperty("HDFS", "hdfs-site", "foo")).andReturn(false).atLeastOnce();
    expect(stack.isPasswordProperty("HDFS", "hdfs-site", "bar")).andReturn(false).atLeastOnce();
    expect(stack.isPasswordProperty("HDFS", "hdfs-site", "some_password")).andReturn(true).atLeastOnce();
    expect(stack.isPasswordProperty("HDFS", "category1", "prop1")).andReturn(false).atLeastOnce();
    expect(stack.isPasswordProperty("SERVICE2", "category2", "prop2")).andReturn(false).atLeastOnce();

    expect(group1.getConfiguration()).andReturn(EMPTY_CONFIGURATION).atLeastOnce();
    expect(group1.getName()).andReturn("group1").anyTimes();
    expect(group1.getComponentNames()).andReturn(group1Components).atLeastOnce();

    expect(group2.getConfiguration()).andReturn(EMPTY_CONFIGURATION).atLeastOnce();
    expect(group2.getName()).andReturn("group2").anyTimes();
    expect(group2.getComponentNames()).andReturn(group2Components).atLeastOnce();

    replay(stack, group1, group2);

    // Blueprint config
    Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();
    Map<String, String> hdfsProps = new HashMap<String, String>();
    properties.put("hdfs-site", hdfsProps);
    hdfsProps.put("foo", "val");
    hdfsProps.put("bar", "val");
    Map<String, String> category1Props = new HashMap<String, String>();
    properties.put("category1", category1Props);
    category1Props.put("prop1", "val");

    Map<String, Map<String, Map<String, String>>> attributes = new HashMap<String, Map<String, Map<String, String>>>();
    // for this basic test not ensuring that stack properties are ignored, this is tested in another test
    Configuration configuration = new Configuration(properties, attributes, EMPTY_CONFIGURATION);

    Blueprint blueprint = new BlueprintImpl("test", hostGroups, stack, configuration, null);
    try {
      blueprint.validateRequiredProperties();
      fail("Expected exception to be thrown for missing config property");
    } catch (InvalidTopologyException e) {
      System.out.println("****" + e.getMessage() + "***");
    }

    verify(stack, group1, group2);
  }

  @Test
  public void testValidateConfigurations__hostGroupConfig() throws Exception {

    Stack stack = createNiceMock(Stack.class);

    HostGroup group1 = createMock(HostGroup.class);
    HostGroup group2 = createMock(HostGroup.class);
    Collection<HostGroup> hostGroups = new HashSet<HostGroup>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    Set<String> group1Components = new HashSet<String>();
    group1Components.add("c1");
    group1Components.add("c2");

    Set<String> group2Components = new HashSet<String>();
    group2Components.add("c1");
    group2Components.add("c3");

    Map<String, Map<String, String>> group2Props = new HashMap<String, Map<String, String>>();
    Map<String, String> group2Category2Props = new HashMap<String, String>();
    group2Props.put("category2", group2Category2Props);
    group2Category2Props.put("prop2", "val");

    Collection<Stack.ConfigProperty> requiredHDFSProperties = new HashSet<Stack.ConfigProperty>();
    requiredHDFSProperties.add(new Stack.ConfigProperty("hdfs-site", "foo", null));
    requiredHDFSProperties.add(new Stack.ConfigProperty("hdfs-site", "bar", null));
    requiredHDFSProperties.add(new Stack.ConfigProperty("hdfs-site", "some_password", null));

    requiredHDFSProperties.add(new Stack.ConfigProperty("category1", "prop1", null));

    Collection<Stack.ConfigProperty> requiredService2Properties = new HashSet<Stack.ConfigProperty>();
    requiredService2Properties.add(new Stack.ConfigProperty("category2", "prop2", null));

    expect(stack.getServiceForComponent("c1")).andReturn("HDFS").atLeastOnce();
    expect(stack.getServiceForComponent("c2")).andReturn("HDFS").atLeastOnce();
    expect(stack.getServiceForComponent("c3")).andReturn("SERVICE2").atLeastOnce();

    expect(stack.getRequiredConfigurationProperties("HDFS")).andReturn(requiredHDFSProperties).atLeastOnce();
    expect(stack.getRequiredConfigurationProperties("SERVICE2")).andReturn(requiredService2Properties).atLeastOnce();

    expect(stack.isPasswordProperty("HDFS", "hdfs-site", "foo")).andReturn(false).atLeastOnce();
    expect(stack.isPasswordProperty("HDFS", "hdfs-site", "bar")).andReturn(false).atLeastOnce();
    expect(stack.isPasswordProperty("HDFS", "hdfs-site", "some_password")).andReturn(true).atLeastOnce();
    expect(stack.isPasswordProperty("HDFS", "category1", "prop1")).andReturn(false).atLeastOnce();
    expect(stack.isPasswordProperty("SERVICE2", "category2", "prop2")).andReturn(false).atLeastOnce();

    expect(group1.getConfiguration()).andReturn(EMPTY_CONFIGURATION).atLeastOnce();
    expect(group1.getName()).andReturn("group1").anyTimes();
    expect(group1.getComponentNames()).andReturn(group1Components).atLeastOnce();
    expect(group1.getCardinality()).andReturn("1").atLeastOnce();
    expect(group1.getComponents()).andReturn(Arrays.asList(new Component("c1"), new Component("c2"))).atLeastOnce();

    expect(group2.getName()).andReturn("group2").anyTimes();
    expect(group2.getComponentNames()).andReturn(group2Components).atLeastOnce();
    expect(group2.getCardinality()).andReturn("1").atLeastOnce();
    expect(group2.getComponents()).andReturn(Arrays.asList(new Component("c1"), new Component("c3"))).atLeastOnce();

    // Blueprint config
    Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();
    Map<String, String> hdfsProps = new HashMap<String, String>();
    properties.put("hdfs-site", hdfsProps);
    hdfsProps.put("foo", "val");
    hdfsProps.put("bar", "val");

    Map<String, String> category1Props = new HashMap<String, String>();
    properties.put("category1", category1Props);
    category1Props.put("prop1", "val");

    Map<String, Map<String, Map<String, String>>> attributes = new HashMap<String, Map<String, Map<String, String>>>();
    Configuration configuration = new Configuration(properties, attributes, EMPTY_CONFIGURATION);
    // set config for group2 which contains a required property
    Configuration group2Configuration = new Configuration(group2Props, EMPTY_ATTRIBUTES, configuration);
    expect(group2.getConfiguration()).andReturn(group2Configuration).atLeastOnce();

    replay(stack, group1, group2);

    Blueprint blueprint = new BlueprintImpl("test", hostGroups, stack, configuration, null);
    blueprint.validateRequiredProperties();
    BlueprintEntity entity = blueprint.toEntity();

    verify(stack, group1, group2);
    assertTrue(entity.getSecurityType() == SecurityType.NONE);
    assertTrue(entity.getSecurityDescriptorReference() == null);
  }

  @Test
  public void testValidateConfigurations__secretReference(){
    Stack stack = createNiceMock(Stack.class);

    HostGroup group1 = createNiceMock(HostGroup.class);
    HostGroup group2 = createNiceMock(HostGroup.class);
    Collection<HostGroup> hostGroups = new HashSet<HostGroup>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    Set<String> group1Components = new HashSet<String>();
    group1Components.add("c1");
    group1Components.add("c2");

    Set<String> group2Components = new HashSet<String>();
    group2Components.add("c1");
    group2Components.add("c3");

    Map<String, Map<String, String>> group2Props = new HashMap<String, Map<String, String>>();
    Map<String, String> group2Category2Props = new HashMap<String, String>();
    group2Props.put("category2", group2Category2Props);
    group2Category2Props.put("prop2", "val");

    Collection<Stack.ConfigProperty> requiredHDFSProperties = new HashSet<Stack.ConfigProperty>();
    requiredHDFSProperties.add(new Stack.ConfigProperty("hdfs-site", "foo", null));
    requiredHDFSProperties.add(new Stack.ConfigProperty("hdfs-site", "bar", null));
    requiredHDFSProperties.add(new Stack.ConfigProperty("hdfs-site", "some_password", null));

    requiredHDFSProperties.add(new Stack.ConfigProperty("category1", "prop1", null));

    Collection<Stack.ConfigProperty> requiredService2Properties = new HashSet<Stack.ConfigProperty>();
    requiredService2Properties.add(new Stack.ConfigProperty("category2", "prop2", null));


    // Blueprint config
    Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();
    Map<String, String> hdfsProps = new HashMap<String, String>();
    properties.put("hdfs-site", hdfsProps);
    hdfsProps.put("foo", "val");
    hdfsProps.put("bar", "val");
    hdfsProps.put("secret", "SECRET:hdfs-site:1:test");

    Map<String, String> category1Props = new HashMap<String, String>();
    properties.put("category1", category1Props);
    category1Props.put("prop1", "val");

    Map<String, Map<String, Map<String, String>>> attributes = new HashMap<String, Map<String, Map<String, String>>>();
    Configuration configuration = new Configuration(properties, attributes, EMPTY_CONFIGURATION);
    // set config for group2 which contains a required property

    replay(stack, group1, group2);

    Blueprint blueprint = new BlueprintImpl("test", hostGroups, stack, configuration, null);
    try {
      blueprint.validateRequiredProperties();
      fail("Expected exception to be thrown for using secret reference");
    } catch (InvalidTopologyException e) {
      System.out.println("****" + e.getMessage() + "***");
    }

  }

  //todo: ensure coverage for these existing tests

  //  private void validateEntity(BlueprintEntity entity, boolean containsConfig) {
//    assertEquals(BLUEPRINT_NAME, entity.getBlueprintName());
//
//    StackEntity stackEntity = entity.getStack();
//    assertEquals("test-stack-name", stackEntity.getStackName());
//    assertEquals("test-stack-version", stackEntity.getStackVersion());
//
//    Collection<HostGroupEntity> hostGroupEntities = entity.getHostGroups();
//
//    assertEquals(2, hostGroupEntities.size());
//    for (HostGroupEntity hostGroup : hostGroupEntities) {
//      assertEquals(BLUEPRINT_NAME, hostGroup.getBlueprintName());
//      assertNotNull(hostGroup.getBlueprintEntity());
//      Collection<HostGroupComponentEntity> componentEntities = hostGroup.getComponents();
//      if (hostGroup.getName().equals("group1")) {
//        assertEquals("1", hostGroup.getCardinality());
//        assertEquals(2, componentEntities.size());
//        Iterator<HostGroupComponentEntity> componentIterator = componentEntities.iterator();
//        String name = componentIterator.next().getName();
//        assertTrue(name.equals("component1") || name.equals("component2"));
//        String name2 = componentIterator.next().getName();
//        assertFalse(name.equals(name2));
//        assertTrue(name2.equals("component1") || name2.equals("component2"));
//      } else if (hostGroup.getName().equals("group2")) {
//        assertEquals("2", hostGroup.getCardinality());
//        assertEquals(1, componentEntities.size());
//        HostGroupComponentEntity componentEntity = componentEntities.iterator().next();
//        assertEquals("component1", componentEntity.getName());
//
//        if (containsConfig) {
//          Collection<HostGroupConfigEntity> configurations = hostGroup.getConfigurations();
//          assertEquals(1, configurations.size());
//          HostGroupConfigEntity hostGroupConfigEntity = configurations.iterator().next();
//          assertEquals(BLUEPRINT_NAME, hostGroupConfigEntity.getBlueprintName());
//          assertSame(hostGroup, hostGroupConfigEntity.getHostGroupEntity());
//          assertEquals("core-site", hostGroupConfigEntity.getType());
//          Map<String, String> properties = gson.<Map<String, String>>fromJson(
//              hostGroupConfigEntity.getConfigData(), Map.class);
//          assertEquals(1, properties.size());
//          assertEquals("anything", properties.get("my.custom.hg.property"));
//        }
//      } else {
//        fail("Unexpected host group name");
//      }
//    }
//    Collection<BlueprintConfigEntity> configurations = entity.getConfigurations();
//    if (containsConfig) {
//      assertEquals(1, configurations.size());
//      BlueprintConfigEntity blueprintConfigEntity = configurations.iterator().next();
//      assertEquals(BLUEPRINT_NAME, blueprintConfigEntity.getBlueprintName());
//      assertSame(entity, blueprintConfigEntity.getBlueprintEntity());
//      assertEquals("core-site", blueprintConfigEntity.getType());
//      Map<String, String> properties = gson.<Map<String, String>>fromJson(
//          blueprintConfigEntity.getConfigData(), Map.class);
//      assertEquals(2, properties.size());
//      assertEquals("480", properties.get("fs.trash.interval"));
//      assertEquals("8500", properties.get("ipc.client.idlethreshold"));
//    } else {
//      assertEquals(0, configurations.size());
//    }
//  }



  //  @Test
//  public void testCreateResource_Validate__Cardinality__ExternalComponent() throws Exception {
//
//    Set<Map<String, Object>> setProperties = getTestProperties();
//    setConfigurationProperties(setProperties);
//    ((Set<Map<String, String>>) setProperties.iterator().next().get("configurations")).
//        add(Collections.singletonMap("global/hive_database", "Existing MySQL Database"));
//
//    Iterator iter = ((HashSet<Map<String, HashSet<Map<String, String>>>>) setProperties.iterator().next().
//        get(BlueprintResourceProvider.HOST_GROUP_PROPERTY_ID)).
//        iterator().next().get("components").iterator();
//    iter.next();
//    iter.remove();
//
//    AmbariManagementController managementController = createMock(AmbariManagementController.class);
//    Capture<Set<StackServiceRequest>> stackServiceRequestCapture = new Capture<Set<StackServiceRequest>>();
//    Capture<Set<StackServiceComponentRequest>> serviceComponentRequestCapture = new Capture<Set<StackServiceComponentRequest>>();
//    Capture<StackConfigurationRequest> stackConfigurationRequestCapture = new Capture<StackConfigurationRequest>();
//    Capture<StackLevelConfigurationRequest> stackLevelConfigurationRequestCapture = new Capture<StackLevelConfigurationRequest>();
//    Request request = createMock(Request.class);
//    StackServiceResponse stackServiceResponse = createMock(StackServiceResponse.class);
//    StackServiceComponentResponse stackServiceComponentResponse = createNiceMock(StackServiceComponentResponse.class);
//    StackServiceComponentResponse stackServiceComponentResponse2 = createNiceMock(StackServiceComponentResponse.class);
//    Set<StackServiceComponentResponse> setServiceComponents = new HashSet<StackServiceComponentResponse>();
//    setServiceComponents.add(stackServiceComponentResponse);
//    setServiceComponents.add(stackServiceComponentResponse2);
//
//    Map<String, ServiceInfo> services = new HashMap<String, ServiceInfo>();
//    ServiceInfo service = new ServiceInfo();
//    service.setName("test-service");
//    services.put("test-service", service);
//
//    List<ComponentInfo> serviceComponents = new ArrayList<ComponentInfo>();
//    ComponentInfo component1 = new ComponentInfo();
//    component1.setName("component1");
//    ComponentInfo component2 = new ComponentInfo();
//    component2.setName("MYSQL_SERVER");
//    serviceComponents.add(component1);
//    serviceComponents.add(component2);
//
//    Capture<BlueprintEntity> entityCapture = new Capture<BlueprintEntity>();
//
//    // set expectations
//    expect(blueprintFactory.createBlueprint(setProperties.iterator().next())).andReturn(blueprint).once();
//    expect(blueprint.validateRequiredProperties()).andReturn(Collections.<String, Map<String, Collection<String>>>emptyMap()).once();
//    expect(blueprint.toEntity()).andReturn(entity);
//    expect(blueprint.getName()).andReturn(BLUEPRINT_NAME).atLeastOnce();
//    expect(managementController.getStackServices(capture(stackServiceRequestCapture))).andReturn(
//        Collections.<StackServiceResponse>singleton(stackServiceResponse));
//    expect(stackServiceResponse.getServiceName()).andReturn("test-service").anyTimes();
//    expect(stackServiceResponse.getStackName()).andReturn("test-stack-name").anyTimes();
//    expect(stackServiceResponse.getStackVersion()).andReturn("test-stack-version").anyTimes();
//    expect(stackServiceResponse.getExcludedConfigTypes()).andReturn(Collections.<String>emptySet());
//
//    expect(managementController.getStackComponents(capture(serviceComponentRequestCapture))).andReturn(setServiceComponents).anyTimes();
//    expect(stackServiceComponentResponse.getCardinality()).andReturn("2").anyTimes();
//    expect(stackServiceComponentResponse.getComponentName()).andReturn("component1").anyTimes();
//    expect(stackServiceComponentResponse.getServiceName()).andReturn("test-service").anyTimes();
//    expect(stackServiceComponentResponse.getStackName()).andReturn("test-stack-name").anyTimes();
//    expect(stackServiceComponentResponse.getStackVersion()).andReturn("test-stack-version").anyTimes();
//    expect(stackServiceComponentResponse2.getCardinality()).andReturn("1").anyTimes();
//    expect(stackServiceComponentResponse2.getComponentName()).andReturn("MYSQL_SERVER").anyTimes();
//    expect(stackServiceComponentResponse2.getServiceName()).andReturn("test-service").anyTimes();
//    expect(stackServiceComponentResponse2.getStackName()).andReturn("test-stack-name").anyTimes();
//    expect(stackServiceComponentResponse2.getStackVersion()).andReturn("test-stack-version").anyTimes();
//
//    expect(managementController.getStackConfigurations(Collections.singleton(capture(stackConfigurationRequestCapture)))).
//        andReturn(Collections.<StackConfigurationResponse>emptySet());
//    expect(managementController.getStackLevelConfigurations(Collections.singleton(capture(stackLevelConfigurationRequestCapture)))).
//    andReturn(Collections.<StackConfigurationResponse>emptySet());
//
//    expect(metaInfo.getComponentDependencies("test-stack-name", "test-stack-version", "test-service", "MYSQL_SERVER")).
//        andReturn(Collections.<DependencyInfo>emptyList()).anyTimes();
//    expect(metaInfo.getComponentDependencies("test-stack-name", "test-stack-version", "test-service", "component1")).
//        andReturn(Collections.<DependencyInfo>emptyList()).anyTimes();
//
//    expect(request.getProperties()).andReturn(setProperties);
//    expect(request.getRequestInfoProperties()).andReturn(Collections.<String, String>emptyMap());
//    expect(dao.findByName(BLUEPRINT_NAME)).andReturn(null);
//    expect(metaInfo.getServices("test-stack-name", "test-stack-version")).andReturn(services).anyTimes();
//    expect(metaInfo.getComponentsByService("test-stack-name", "test-stack-version", "test-service")).
//        andReturn(serviceComponents).anyTimes();
//    expect(metaInfo.getComponentToService("test-stack-name", "test-stack-version", "component1")).
//        andReturn("test-service").anyTimes();
//    expect(metaInfo.getComponentToService("test-stack-name", "test-stack-version", "component2")).
//        andReturn("test-service").anyTimes();
//    expect(metaInfo.getService("test-stack-name", "test-stack-version", "test-service")).andReturn(service).anyTimes();
//    dao.create(capture(entityCapture));
//
//    replay(dao, metaInfo, request, managementController, stackServiceResponse,
//        stackServiceComponentResponse, stackServiceComponentResponse2);
//    // end expectations
//
//    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
//        Resource.Type.Blueprint,
//        PropertyHelper.getPropertyIds(Resource.Type.Blueprint),
//        PropertyHelper.getKeyPropertyIds(Resource.Type.Blueprint),
//        managementController);
//
//    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();
//    ((ObservableResourceProvider)provider).addObserver(observer);
//
//    provider.createResources(request);
//
//    ResourceProviderEvent lastEvent = observer.getLastEvent();
//    assertNotNull(lastEvent);
//    assertEquals(Resource.Type.Blueprint, lastEvent.getResourceType());
//    assertEquals(ResourceProviderEvent.Type.Create, lastEvent.getType());
//    assertEquals(request, lastEvent.getRequest());
//    assertNull(lastEvent.getPredicate());
//
//    verify(dao, metaInfo, request, managementController, stackServiceResponse,
//        stackServiceComponentResponse, stackServiceComponentResponse2);
//  }

//  @Test
//   public void testCreateResource_Validate__Cardinality__MultipleDependencyInstances() throws AmbariException, ResourceAlreadyExistsException,
//      SystemException, UnsupportedPropertyException, NoSuchParentResourceException {
//
//    Set<Map<String, Object>> setProperties = getTestProperties();
//    setConfigurationProperties(setProperties);
//
//    AmbariManagementController managementController = createMock(AmbariManagementController.class);
//    Capture<Set<StackServiceRequest>> stackServiceRequestCapture = new Capture<Set<StackServiceRequest>>();
//    Capture<Set<StackServiceComponentRequest>> serviceComponentRequestCapture = new Capture<Set<StackServiceComponentRequest>>();
//    Capture<StackConfigurationRequest> stackConfigurationRequestCapture = new Capture<StackConfigurationRequest>();
//    Capture<StackLevelConfigurationRequest> stackLevelConfigurationRequestCapture = new Capture<StackLevelConfigurationRequest>();
//    Request request = createMock(Request.class);
//    StackServiceResponse stackServiceResponse = createMock(StackServiceResponse.class);
//    StackServiceComponentResponse stackServiceComponentResponse = createNiceMock(StackServiceComponentResponse.class);
//    StackServiceComponentResponse stackServiceComponentResponse2 = createNiceMock(StackServiceComponentResponse.class);
//    Set<StackServiceComponentResponse> setServiceComponents = new HashSet<StackServiceComponentResponse>();
//    setServiceComponents.add(stackServiceComponentResponse);
//    setServiceComponents.add(stackServiceComponentResponse2);
//
//    DependencyInfo dependencyInfo = new DependencyInfo();
//    AutoDeployInfo autoDeployInfo = new AutoDeployInfo();
//    autoDeployInfo.setEnabled(false);
//    dependencyInfo.setAutoDeploy(autoDeployInfo);
//    dependencyInfo.setScope("cluster");
//    dependencyInfo.setName("test-service/component1");
//
//    Map<String, ServiceInfo> services = new HashMap<String, ServiceInfo>();
//    ServiceInfo service = new ServiceInfo();
//    service.setName("test-service");
//    services.put("test-service", service);
//
//    List<ComponentInfo> serviceComponents = new ArrayList<ComponentInfo>();
//    ComponentInfo component1 = new ComponentInfo();
//    component1.setName("component1");
//    ComponentInfo component2 = new ComponentInfo();
//    component2.setName("component2");
//    serviceComponents.add(component1);
//    serviceComponents.add(component2);
//
//    Capture<BlueprintEntity> entityCapture = new Capture<BlueprintEntity>();
//
//    // set expectations
//    expect(managementController.getStackServices(capture(stackServiceRequestCapture))).andReturn(
//        Collections.<StackServiceResponse>singleton(stackServiceResponse));
//    expect(stackServiceResponse.getServiceName()).andReturn("test-service").anyTimes();
//    expect(stackServiceResponse.getStackName()).andReturn("test-stack-name").anyTimes();
//    expect(stackServiceResponse.getStackVersion()).andReturn("test-stack-version").anyTimes();
//    expect(stackServiceResponse.getExcludedConfigTypes()).andReturn(Collections.<String>emptySet());
//
//    expect(managementController.getStackComponents(capture(serviceComponentRequestCapture))).andReturn(setServiceComponents).anyTimes();
//    expect(stackServiceComponentResponse.getCardinality()).andReturn("2").anyTimes();
//    expect(stackServiceComponentResponse.getComponentName()).andReturn("component1").anyTimes();
//    expect(stackServiceComponentResponse.getServiceName()).andReturn("test-service").anyTimes();
//    expect(stackServiceComponentResponse.getStackName()).andReturn("test-stack-name").anyTimes();
//    expect(stackServiceComponentResponse.getStackVersion()).andReturn("test-stack-version").anyTimes();
//    expect(stackServiceComponentResponse2.getCardinality()).andReturn("1").anyTimes();
//    expect(stackServiceComponentResponse2.getComponentName()).andReturn("component2").anyTimes();
//    expect(stackServiceComponentResponse2.getServiceName()).andReturn("test-service").anyTimes();
//    expect(stackServiceComponentResponse2.getStackName()).andReturn("test-stack-name").anyTimes();
//    expect(stackServiceComponentResponse2.getStackVersion()).andReturn("test-stack-version").anyTimes();
//
//    expect(managementController.getStackConfigurations(Collections.singleton(capture(stackConfigurationRequestCapture)))).
//        andReturn(Collections.<StackConfigurationResponse>emptySet());
//    expect(managementController.getStackLevelConfigurations(Collections.singleton(capture(stackLevelConfigurationRequestCapture)))).
//        andReturn(Collections.<StackConfigurationResponse>emptySet());
//
//    expect(metaInfo.getComponentDependencies("test-stack-name", "test-stack-version", "test-service", "component2")).
//        andReturn(Collections.<DependencyInfo>singletonList(dependencyInfo)).anyTimes();
//    expect(metaInfo.getComponentDependencies("test-stack-name", "test-stack-version", "test-service", "component1")).
//        andReturn(Collections.<DependencyInfo>emptyList()).anyTimes();
//
//    expect(request.getProperties()).andReturn(setProperties);
//    expect(request.getRequestInfoProperties()).andReturn(Collections.<String, String>emptyMap());
//    expect(dao.findByName(BLUEPRINT_NAME)).andReturn(null);
//    expect(metaInfo.getServices("test-stack-name", "test-stack-version")).andReturn(services).anyTimes();
//    expect(metaInfo.getComponentsByService("test-stack-name", "test-stack-version", "test-service")).
//        andReturn(serviceComponents).anyTimes();
//    expect(metaInfo.getComponentToService("test-stack-name", "test-stack-version", "component1")).
//        andReturn("test-service").anyTimes();
//    expect(metaInfo.getComponentToService("test-stack-name", "test-stack-version", "component2")).
//        andReturn("test-service").anyTimes();
//    expect(metaInfo.getService("test-stack-name", "test-stack-version", "test-service")).andReturn(service).anyTimes();
//    dao.create(capture(entityCapture));
//
//    replay(dao, metaInfo, request, managementController, stackServiceResponse,
//        stackServiceComponentResponse, stackServiceComponentResponse2);
//    // end expectations
//
//    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
//        Resource.Type.Blueprint,
//        PropertyHelper.getPropertyIds(Resource.Type.Blueprint),
//        PropertyHelper.getKeyPropertyIds(Resource.Type.Blueprint),
//        managementController);
//
//    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();
//    ((ObservableResourceProvider)provider).addObserver(observer);
//
//    provider.createResources(request);
//
//    ResourceProviderEvent lastEvent = observer.getLastEvent();
//    assertNotNull(lastEvent);
//    assertEquals(Resource.Type.Blueprint, lastEvent.getResourceType());
//    assertEquals(ResourceProviderEvent.Type.Create, lastEvent.getType());
//    assertEquals(request, lastEvent.getRequest());
//    assertNull(lastEvent.getPredicate());
//
//    verify(dao, metaInfo, request, managementController, stackServiceResponse,
//        stackServiceComponentResponse, stackServiceComponentResponse2);
//  }

//  @Test
//  public void testCreateResource_Validate__Cardinality__AutoCommit() throws AmbariException, ResourceAlreadyExistsException,
//      SystemException, UnsupportedPropertyException, NoSuchParentResourceException {
//
//    Set<Map<String, Object>> setProperties = getTestProperties();
//    setConfigurationProperties(setProperties);
//
//    // remove component2 from BP
//    Iterator iter = ((HashSet<Map<String, HashSet<Map<String, String>>>>) setProperties.iterator().next().
//        get(BlueprintResourceProvider.HOST_GROUP_PROPERTY_ID)).
//        iterator().next().get("components").iterator();
//    iter.next();
//    iter.remove();
//
//    AmbariManagementController managementController = createMock(AmbariManagementController.class);
//    Capture<Set<StackServiceRequest>> stackServiceRequestCapture = new Capture<Set<StackServiceRequest>>();
//    Capture<Set<StackServiceComponentRequest>> serviceComponentRequestCapture = new Capture<Set<StackServiceComponentRequest>>();
//    Capture<StackConfigurationRequest> stackConfigurationRequestCapture = new Capture<StackConfigurationRequest>();
//    Capture<StackLevelConfigurationRequest> stackLevelConfigurationRequestCapture = new Capture<StackLevelConfigurationRequest>();
//    Request request = createMock(Request.class);
//    StackServiceResponse stackServiceResponse = createMock(StackServiceResponse.class);
//    StackServiceComponentResponse stackServiceComponentResponse = createNiceMock(StackServiceComponentResponse.class);
//    StackServiceComponentResponse stackServiceComponentResponse2 = createNiceMock(StackServiceComponentResponse.class);
//    Set<StackServiceComponentResponse> setServiceComponents = new HashSet<StackServiceComponentResponse>();
//    setServiceComponents.add(stackServiceComponentResponse);
//    setServiceComponents.add(stackServiceComponentResponse2);
//
//    DependencyInfo dependencyInfo = new DependencyInfo();
//    AutoDeployInfo autoDeployInfo = new AutoDeployInfo();
//    autoDeployInfo.setEnabled(true);
//    autoDeployInfo.setCoLocate("test-service/component1");
//    dependencyInfo.setAutoDeploy(autoDeployInfo);
//    dependencyInfo.setScope("cluster");
//    dependencyInfo.setName("test-service/component2");
//
//    Map<String, ServiceInfo> services = new HashMap<String, ServiceInfo>();
//    ServiceInfo service = new ServiceInfo();
//    service.setName("test-service");
//    services.put("test-service", service);
//
//    List<ComponentInfo> serviceComponents = new ArrayList<ComponentInfo>();
//    ComponentInfo component1 = new ComponentInfo();
//    component1.setName("component1");
//    ComponentInfo component2 = new ComponentInfo();
//    component2.setName("component2");
//    serviceComponents.add(component1);
//    serviceComponents.add(component2);
//
//    Capture<BlueprintEntity> entityCapture = new Capture<BlueprintEntity>();
//
//    // set expectations
//    expect(managementController.getStackServices(capture(stackServiceRequestCapture))).andReturn(
//        Collections.<StackServiceResponse>singleton(stackServiceResponse));
//    expect(stackServiceResponse.getServiceName()).andReturn("test-service").anyTimes();
//    expect(stackServiceResponse.getStackName()).andReturn("test-stack-name").anyTimes();
//    expect(stackServiceResponse.getStackVersion()).andReturn("test-stack-version").anyTimes();
//    expect(stackServiceResponse.getExcludedConfigTypes()).andReturn(Collections.<String>emptySet());
//
//    expect(managementController.getStackComponents(capture(serviceComponentRequestCapture))).andReturn(setServiceComponents).anyTimes();
//    expect(stackServiceComponentResponse.getCardinality()).andReturn("2").anyTimes();
//    expect(stackServiceComponentResponse.getComponentName()).andReturn("component1").anyTimes();
//    expect(stackServiceComponentResponse.getServiceName()).andReturn("test-service").anyTimes();
//    expect(stackServiceComponentResponse.getStackName()).andReturn("test-stack-name").anyTimes();
//    expect(stackServiceComponentResponse.getStackVersion()).andReturn("test-stack-version").anyTimes();
//    expect(stackServiceComponentResponse2.getCardinality()).andReturn("1").anyTimes();
//    expect(stackServiceComponentResponse2.getComponentName()).andReturn("component2").anyTimes();
//    expect(stackServiceComponentResponse2.getServiceName()).andReturn("test-service").anyTimes();
//    expect(stackServiceComponentResponse2.getStackName()).andReturn("test-stack-name").anyTimes();
//    expect(stackServiceComponentResponse2.getStackVersion()).andReturn("test-stack-version").anyTimes();
//
//    expect(managementController.getStackConfigurations(Collections.singleton(capture(stackConfigurationRequestCapture)))).
//        andReturn(Collections.<StackConfigurationResponse>emptySet());
//    expect(managementController.getStackLevelConfigurations(Collections.singleton(capture(stackLevelConfigurationRequestCapture)))).
//        andReturn(Collections.<StackConfigurationResponse>emptySet());
//
//    expect(metaInfo.getComponentDependencies("test-stack-name", "test-stack-version", "test-service", "component2")).
//        andReturn(Collections.<DependencyInfo>emptyList()).anyTimes();
//    expect(metaInfo.getComponentDependencies("test-stack-name", "test-stack-version", "test-service", "component1")).
//        andReturn(Collections.<DependencyInfo>singletonList(dependencyInfo)).anyTimes();
//
//    expect(request.getProperties()).andReturn(setProperties);
//    expect(request.getRequestInfoProperties()).andReturn(Collections.<String, String>emptyMap());
//    expect(dao.findByName(BLUEPRINT_NAME)).andReturn(null);
//    expect(metaInfo.getServices("test-stack-name", "test-stack-version")).andReturn(services).anyTimes();
//    expect(metaInfo.getComponentsByService("test-stack-name", "test-stack-version", "test-service")).
//        andReturn(serviceComponents).anyTimes();
//    expect(metaInfo.getComponentToService("test-stack-name", "test-stack-version", "component1")).
//        andReturn("test-service").anyTimes();
//    expect(metaInfo.getComponentToService("test-stack-name", "test-stack-version", "component2")).
//        andReturn("test-service").anyTimes();
//    expect(metaInfo.getService("test-stack-name", "test-stack-version", "test-service")).andReturn(service).anyTimes();
//    dao.create(capture(entityCapture));
//
//    replay(dao, metaInfo, request, managementController, stackServiceResponse,
//        stackServiceComponentResponse, stackServiceComponentResponse2);
//    // end expectations
//
//    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
//        Resource.Type.Blueprint,
//        PropertyHelper.getPropertyIds(Resource.Type.Blueprint),
//        PropertyHelper.getKeyPropertyIds(Resource.Type.Blueprint),
//        managementController);
//
//    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();
//    ((ObservableResourceProvider)provider).addObserver(observer);
//
//    provider.createResources(request);
//
//    ResourceProviderEvent lastEvent = observer.getLastEvent();
//    assertNotNull(lastEvent);
//    assertEquals(Resource.Type.Blueprint, lastEvent.getResourceType());
//    assertEquals(ResourceProviderEvent.Type.Create, lastEvent.getType());
//    assertEquals(request, lastEvent.getRequest());
//    assertNull(lastEvent.getPredicate());
//
//    verify(dao, metaInfo, request, managementController, stackServiceResponse,
//        stackServiceComponentResponse, stackServiceComponentResponse2);
//  }

//  @Test
//  public void testCreateResource_Validate__Cardinality__Fail() throws AmbariException, ResourceAlreadyExistsException,
//      SystemException, UnsupportedPropertyException, NoSuchParentResourceException {
//
//    Set<Map<String, Object>> setProperties = getTestProperties();
//    setConfigurationProperties(setProperties);
//
//    Iterator iter = ((HashSet<Map<String, HashSet<Map<String, String>>>>) setProperties.iterator().next().
//        get(BlueprintResourceProvider.HOST_GROUP_PROPERTY_ID)).
//        iterator().next().get("components").iterator();
//    iter.next();
//    iter.remove();
//
//    AmbariManagementController managementController = createMock(AmbariManagementController.class);
//    Capture<Set<StackServiceRequest>> stackServiceRequestCapture = new Capture<Set<StackServiceRequest>>();
//    Capture<Set<StackServiceComponentRequest>> serviceComponentRequestCapture = new Capture<Set<StackServiceComponentRequest>>();
//    Capture<StackConfigurationRequest> stackConfigurationRequestCapture = new Capture<StackConfigurationRequest>();
//    Capture<StackLevelConfigurationRequest> stackLevelConfigurationRequestCapture = new Capture<StackLevelConfigurationRequest>();
//    Request request = createMock(Request.class);
//    StackServiceResponse stackServiceResponse = createMock(StackServiceResponse.class);
//    StackServiceComponentResponse stackServiceComponentResponse = createNiceMock(StackServiceComponentResponse.class);
//    StackServiceComponentResponse stackServiceComponentResponse2 = createNiceMock(StackServiceComponentResponse.class);
//    Set<StackServiceComponentResponse> setServiceComponents = new HashSet<StackServiceComponentResponse>();
//    setServiceComponents.add(stackServiceComponentResponse);
//    setServiceComponents.add(stackServiceComponentResponse2);
//
//    Map<String, ServiceInfo> services = new HashMap<String, ServiceInfo>();
//    ServiceInfo service = new ServiceInfo();
//    service.setName("test-service");
//    services.put("test-service", service);
//
//    List<ComponentInfo> serviceComponents = new ArrayList<ComponentInfo>();
//    ComponentInfo component1 = new ComponentInfo();
//    component1.setName("component1");
//    ComponentInfo component2 = new ComponentInfo();
//    component2.setName("MYSQL_SERVER");
//    serviceComponents.add(component1);
//    serviceComponents.add(component2);
//
//    // set expectations
//    expect(managementController.getStackServices(capture(stackServiceRequestCapture))).andReturn(
//        Collections.<StackServiceResponse>singleton(stackServiceResponse));
//    expect(stackServiceResponse.getServiceName()).andReturn("test-service").anyTimes();
//    expect(stackServiceResponse.getStackName()).andReturn("test-stack-name").anyTimes();
//    expect(stackServiceResponse.getStackVersion()).andReturn("test-stack-version").anyTimes();
//    expect(stackServiceResponse.getExcludedConfigTypes()).andReturn(Collections.<String>emptySet());
//
//    expect(managementController.getStackComponents(capture(serviceComponentRequestCapture))).andReturn(setServiceComponents).anyTimes();
//    expect(stackServiceComponentResponse.getCardinality()).andReturn("2").anyTimes();
//    expect(stackServiceComponentResponse.getComponentName()).andReturn("component1").anyTimes();
//    expect(stackServiceComponentResponse.getServiceName()).andReturn("test-service").anyTimes();
//    expect(stackServiceComponentResponse.getStackName()).andReturn("test-stack-name").anyTimes();
//    expect(stackServiceComponentResponse.getStackVersion()).andReturn("test-stack-version").anyTimes();
//    expect(stackServiceComponentResponse2.getCardinality()).andReturn("1").anyTimes();
//    expect(stackServiceComponentResponse2.getComponentName()).andReturn("MYSQL_SERVER").anyTimes();
//    expect(stackServiceComponentResponse2.getServiceName()).andReturn("test-service").anyTimes();
//    expect(stackServiceComponentResponse2.getStackName()).andReturn("test-stack-name").anyTimes();
//    expect(stackServiceComponentResponse2.getStackVersion()).andReturn("test-stack-version").anyTimes();
//
//    expect(managementController.getStackConfigurations(Collections.singleton(capture(stackConfigurationRequestCapture)))).
//        andReturn(Collections.<StackConfigurationResponse>emptySet());
//    expect(managementController.getStackLevelConfigurations(Collections.singleton(capture(stackLevelConfigurationRequestCapture)))).
//        andReturn(Collections.<StackConfigurationResponse>emptySet());
//
//    expect(metaInfo.getComponentDependencies("test-stack-name", "test-stack-version", "test-service", "MYSQL_SERVER")).
//        andReturn(Collections.<DependencyInfo>emptyList()).anyTimes();
//    expect(metaInfo.getComponentDependencies("test-stack-name", "test-stack-version", "test-service", "component1")).
//        andReturn(Collections.<DependencyInfo>emptyList()).anyTimes();
//
//    expect(request.getProperties()).andReturn(setProperties);
//    expect(request.getRequestInfoProperties()).andReturn(Collections.<String, String>emptyMap());
//    expect(dao.findByName(BLUEPRINT_NAME)).andReturn(null);
//    expect(metaInfo.getServices("test-stack-name", "test-stack-version")).andReturn(services).anyTimes();
//    expect(metaInfo.getComponentsByService("test-stack-name", "test-stack-version", "test-service")).
//        andReturn(serviceComponents).anyTimes();
//    expect(metaInfo.getComponentToService("test-stack-name", "test-stack-version", "component1")).
//        andReturn("test-service").anyTimes();
//    expect(metaInfo.getComponentToService("test-stack-name", "test-stack-version", "component2")).
//        andReturn("test-service").anyTimes();
//    expect(metaInfo.getService("test-stack-name", "test-stack-version", "test-service")).andReturn(service).anyTimes();
//
//    replay(dao, metaInfo, request, managementController, stackServiceResponse,
//        stackServiceComponentResponse, stackServiceComponentResponse2);
//    // end expectations
//
//    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
//        Resource.Type.Blueprint,
//        PropertyHelper.getPropertyIds(Resource.Type.Blueprint),
//        PropertyHelper.getKeyPropertyIds(Resource.Type.Blueprint),
//        managementController);
//
//    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();
//    ((ObservableResourceProvider)provider).addObserver(observer);
//
//    try {
//      provider.createResources(request);
//      fail("Expected validation failure for MYSQL_SERVER");
//    } catch (IllegalArgumentException e) {
//      // expected
//    }
//
//    verify(dao, metaInfo, request, managementController, stackServiceResponse,
//        stackServiceComponentResponse, stackServiceComponentResponse2);
//  }

//  @Test
//  public void testCreateResource_Validate__AmbariServerComponent() throws AmbariException, ResourceAlreadyExistsException,
//      SystemException, UnsupportedPropertyException, NoSuchParentResourceException
//  {
//    Request request = createMock(Request.class);
//    AmbariManagementController managementController = createMock(AmbariManagementController.class);
//    Capture<Set<StackServiceRequest>> stackServiceRequestCapture = new Capture<Set<StackServiceRequest>>();
//
//    Map<String, ServiceInfo> services = new HashMap<String, ServiceInfo>();
//    ServiceInfo service = new ServiceInfo();
//    service.setName("test-service");
//    services.put("test-service", service);
//
//    List<ComponentInfo> serviceComponents = new ArrayList<ComponentInfo>();
//    ComponentInfo component1 = new ComponentInfo();
//    component1.setName("component1");
//    ComponentInfo component2 = new ComponentInfo();
//    component2.setName("component2");
//    serviceComponents.add(component1);
//    serviceComponents.add(component2);
//
//
//    Set<Map<String, Object>> setProperties = getTestProperties();
//    ((HashSet<Map<String, String>>) ((HashSet<Map<String, Object>>) setProperties.iterator().next().get(
//        BlueprintResourceProvider.HOST_GROUP_PROPERTY_ID)).iterator().next().get("components")).
//        iterator().next().put("name", "AMBARI_SERVER");
//
//    Capture<BlueprintEntity> entityCapture = new Capture<BlueprintEntity>();
//
//    // set expectations
//    expect(managementController.getStackServices(capture(stackServiceRequestCapture))).andReturn(
//        Collections.<StackServiceResponse>emptySet());
//    expect(request.getProperties()).andReturn(setProperties);
//    expect(request.getRequestInfoProperties()).andReturn(Collections.<String, String>emptyMap());
//    expect(dao.findByName(BLUEPRINT_NAME)).andReturn(null);
//    expect(metaInfo.getServices("test-stack-name", "test-stack-version")).andReturn(services).anyTimes();
//    expect(metaInfo.getComponentsByService("test-stack-name", "test-stack-version", "test-service")).
//        andReturn(serviceComponents).anyTimes();
//    expect(metaInfo.getComponentToService("test-stack-name", "test-stack-version", "component1")).
//        andReturn("test-service").anyTimes();
//    expect(metaInfo.getService("test-stack-name", "test-stack-version", "test-service")).andReturn(service).anyTimes();
//
//    dao.create(capture(entityCapture));
//
//    replay(dao, metaInfo, request, managementController);
//    // end expectations
//
//    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
//        Resource.Type.Blueprint,
//        PropertyHelper.getPropertyIds(Resource.Type.Blueprint),
//        PropertyHelper.getKeyPropertyIds(Resource.Type.Blueprint),
//        managementController);
//
//    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();
//    ((ObservableResourceProvider)provider).addObserver(observer);
//
//    provider.createResources(request);
//
//    ResourceProviderEvent lastEvent = observer.getLastEvent();
//    assertNotNull(lastEvent);
//    assertEquals(Resource.Type.Blueprint, lastEvent.getResourceType());
//    assertEquals(ResourceProviderEvent.Type.Create, lastEvent.getType());
//    assertEquals(request, lastEvent.getRequest());
//    assertNull(lastEvent.getPredicate());
//
//    verify(dao, metaInfo, request, managementController);
//  }


}
