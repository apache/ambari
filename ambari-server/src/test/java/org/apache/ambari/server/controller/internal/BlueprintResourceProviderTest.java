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

package org.apache.ambari.server.controller.internal;

import com.google.gson.Gson;
import org.apache.ambari.server.controller.predicate.EqualsPredicate;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.BlueprintDAO;
import org.apache.ambari.server.orm.entities.BlueprintConfigEntity;
import org.apache.ambari.server.orm.entities.BlueprintEntity;
import org.apache.ambari.server.orm.entities.HostGroupComponentEntity;
import org.apache.ambari.server.orm.entities.HostGroupEntity;
import org.easymock.Capture;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.fail;

/**
 * BlueprintResourceProvider unit tests.
 */
public class BlueprintResourceProviderTest {

  private static String BLUEPRINT_NAME = "test-blueprint";

  private final static BlueprintDAO dao = createStrictMock(BlueprintDAO.class);
  private final static Gson gson = new Gson();

  @BeforeClass
  public static void initClass() {
    BlueprintResourceProvider.init(dao, gson);
  }

  @Before
  public void resetGlobalMocks() {
    reset(dao);
  }

  @Test
  public void testCreateResources() throws ResourceAlreadyExistsException, SystemException,
                                           UnsupportedPropertyException, NoSuchParentResourceException {

    Set<Map<String, Object>> setProperties = getTestProperties();
    Request request = createMock(Request.class);
    Capture<BlueprintEntity> entityCapture = new Capture<BlueprintEntity>();

    // set expectations
    expect(request.getProperties()).andReturn(setProperties);
    expect(dao.findByName(BLUEPRINT_NAME)).andReturn(null);
    dao.create(capture(entityCapture));

    replay(dao, request);
    // end expectations

    ResourceProvider provider = createProvider();
    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();
    ((ObservableResourceProvider)provider).addObserver(observer);

    provider.createResources(request);

    ResourceProviderEvent lastEvent = observer.getLastEvent();
    assertNotNull(lastEvent);
    assertEquals(Resource.Type.Blueprint, lastEvent.getResourceType());
    assertEquals(ResourceProviderEvent.Type.Create, lastEvent.getType());
    assertEquals(request, lastEvent.getRequest());
    assertNull(lastEvent.getPredicate());

    validateEntity(entityCapture.getValue(), false);

    verify(dao, request);
  }

  @Test
  public void testCreateResources_withConfiguration() throws ResourceAlreadyExistsException, SystemException,
      UnsupportedPropertyException, NoSuchParentResourceException {

    Set<Map<String, Object>> setProperties = getTestProperties();
    setConfigurationProperties(setProperties);
    Request request = createMock(Request.class);
    Capture<BlueprintEntity> entityCapture = new Capture<BlueprintEntity>();

    // set expectations
    expect(request.getProperties()).andReturn(setProperties);
    expect(dao.findByName(BLUEPRINT_NAME)).andReturn(null);
    dao.create(capture(entityCapture));

    replay(dao, request);
    // end expectations

    ResourceProvider provider = createProvider();
    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();
    ((ObservableResourceProvider)provider).addObserver(observer);

    provider.createResources(request);

    ResourceProviderEvent lastEvent = observer.getLastEvent();
    assertNotNull(lastEvent);
    assertEquals(Resource.Type.Blueprint, lastEvent.getResourceType());
    assertEquals(ResourceProviderEvent.Type.Create, lastEvent.getType());
    assertEquals(request, lastEvent.getRequest());
    assertNull(lastEvent.getPredicate());

    validateEntity(entityCapture.getValue(), true);

    verify(dao, request);
  }

  @Test
  public void testGetResourcesNoPredicate() throws SystemException, UnsupportedPropertyException,
                                                   NoSuchParentResourceException, NoSuchResourceException {
    Request request = createNiceMock(Request.class);

    ResourceProvider provider = createProvider();
    BlueprintEntity entity = ((BlueprintResourceProvider) provider).toEntity(
        getTestProperties().iterator().next());

    List<BlueprintEntity> results = new ArrayList<BlueprintEntity>();
    results.add(entity);

    // set expectations
    expect(dao.findAll()).andReturn(results);
    replay(dao, request);

    Set<Resource> setResults = provider.getResources(request, null);
    assertEquals(1, setResults.size());

    verify(dao);
    validateResource(setResults.iterator().next(), false);
  }

  @Test
  public void testGetResourcesNoPredicate_withConfiguration() throws SystemException, UnsupportedPropertyException,
      NoSuchParentResourceException, NoSuchResourceException {
    Request request = createNiceMock(Request.class);

    ResourceProvider provider = createProvider();
    Set<Map<String, Object>> testProperties = getTestProperties();
    setConfigurationProperties(testProperties);
    BlueprintEntity entity = ((BlueprintResourceProvider) provider).toEntity(
        testProperties.iterator().next());

    List<BlueprintEntity> results = new ArrayList<BlueprintEntity>();
    results.add(entity);

    // set expectations
    expect(dao.findAll()).andReturn(results);
    replay(dao, request);

    Set<Resource> setResults = provider.getResources(request, null);
    assertEquals(1, setResults.size());

    verify(dao);
    validateResource(setResults.iterator().next(), true);
  }


  @Test
  public void testDeleteResources() throws SystemException, UnsupportedPropertyException,
                                           NoSuchParentResourceException, NoSuchResourceException {

    Capture<BlueprintEntity> entityCapture = new Capture<BlueprintEntity>();

    ResourceProvider provider = createProvider();
    BlueprintEntity blueprintEntity = ((BlueprintResourceProvider) provider).toEntity(
        getTestProperties().iterator().next());

    // set expectations
    expect(dao.findByName(BLUEPRINT_NAME)).andReturn(blueprintEntity);
    dao.remove(capture(entityCapture));
    replay(dao);

    Predicate predicate = new EqualsPredicate<String>(
        BlueprintResourceProvider.BLUEPRINT_NAME_PROPERTY_ID, BLUEPRINT_NAME);

    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();
    ((ObservableResourceProvider)provider).addObserver(observer);

    provider.deleteResources(predicate);

    ResourceProviderEvent lastEvent = observer.getLastEvent();
    assertNotNull(lastEvent);
    assertEquals(Resource.Type.Blueprint, lastEvent.getResourceType());
    assertEquals(ResourceProviderEvent.Type.Delete, lastEvent.getType());
    assertNotNull(lastEvent.getPredicate());

    verify(dao);

    validateEntity(entityCapture.getValue(), false);
  }

  private Set<Map<String, Object>> getTestProperties() {
    Map<String, String> mapHostGroupComponentProperties = new HashMap<String, String>();
    mapHostGroupComponentProperties.put(BlueprintResourceProvider.COMPONENT_NAME_PROPERTY_ID, "component1");

    Map<String, String> mapHostGroupComponentProperties2 = new HashMap<String, String>();
    mapHostGroupComponentProperties2.put(BlueprintResourceProvider.COMPONENT_NAME_PROPERTY_ID, "component2");

    Set<Map<String, String>> setComponentProperties = new HashSet<Map<String, String>>();
    setComponentProperties.add(mapHostGroupComponentProperties);
    setComponentProperties.add(mapHostGroupComponentProperties2);

    Set<Map<String, String>> setComponentProperties2 = new HashSet<Map<String, String>>();
    setComponentProperties2.add(mapHostGroupComponentProperties);

    Map<String, Object> mapHostGroupProperties = new HashMap<String, Object>();
    mapHostGroupProperties.put(BlueprintResourceProvider.HOST_GROUP_NAME_PROPERTY_ID, "group1");
    mapHostGroupProperties.put(BlueprintResourceProvider.HOST_GROUP_CARDINALITY_PROPERTY_ID, "1");
    mapHostGroupProperties.put(BlueprintResourceProvider.COMPONENT_PROPERTY_ID, setComponentProperties);

    Map<String, Object> mapHostGroupProperties2 = new HashMap<String, Object>();
    mapHostGroupProperties2.put(BlueprintResourceProvider.HOST_GROUP_NAME_PROPERTY_ID, "group2");
    mapHostGroupProperties2.put(BlueprintResourceProvider.HOST_GROUP_CARDINALITY_PROPERTY_ID, "2");
    mapHostGroupProperties2.put(BlueprintResourceProvider.COMPONENT_PROPERTY_ID, setComponentProperties2);

    Set<Map<String, Object>> setHostGroupProperties = new HashSet<Map<String, Object>>();
    setHostGroupProperties.add(mapHostGroupProperties);
    setHostGroupProperties.add(mapHostGroupProperties2);

    Map<String, Object> mapProperties = new HashMap<String, Object>();
    mapProperties.put(BlueprintResourceProvider.BLUEPRINT_NAME_PROPERTY_ID, BLUEPRINT_NAME);
    mapProperties.put(BlueprintResourceProvider.STACK_NAME_PROPERTY_ID, "test-stack-name");
    mapProperties.put(BlueprintResourceProvider.STACK_VERSION_PROPERTY_ID, "test-stack-version");
    mapProperties.put(BlueprintResourceProvider.STACK_VERSION_PROPERTY_ID, "test-stack-version");
    mapProperties.put(BlueprintResourceProvider.HOST_GROUP_PROPERTY_ID, setHostGroupProperties);

    return Collections.singleton(mapProperties);
  }

  private void setConfigurationProperties(Set<Map<String, Object>> properties ) {
    Map<String, String> mapConfigsProps = new HashMap<String, String>();
    mapConfigsProps.put("core-site/fs.trash.interval", "480");
    mapConfigsProps.put("core-site/ipc.client.idlethreshold", "8500");

    // single entry in set which was created in getTestProperties
    Map<String, Object> mapProperties = properties.iterator().next();
    mapProperties.put("configurations", Collections.singleton(mapConfigsProps));
  }

  private void validateEntity(BlueprintEntity entity, boolean containsConfig) {
    assertEquals(BLUEPRINT_NAME, entity.getBlueprintName());
    assertEquals("test-stack-name", entity.getStackName());
    assertEquals("test-stack-version", entity.getStackVersion());

    Collection<HostGroupEntity> hostGroupEntities = entity.getHostGroups();

    assertEquals(2, hostGroupEntities.size());
    for (HostGroupEntity hostGroup : hostGroupEntities) {
      assertEquals(BLUEPRINT_NAME, hostGroup.getBlueprintName());
      assertNotNull(hostGroup.getBlueprintEntity());
      Collection<HostGroupComponentEntity> componentEntities = hostGroup.getComponents();
      if (hostGroup.getName().equals("group1")) {
        assertEquals("1", hostGroup.getCardinality());
        assertEquals(2, componentEntities.size());
        Iterator<HostGroupComponentEntity> componentIterator = componentEntities.iterator();
        String name = componentIterator.next().getName();
        assertTrue(name.equals("component1") || name.equals("component2"));
        String name2 = componentIterator.next().getName();
        assertFalse(name.equals(name2));
        assertTrue(name2.equals("component1") || name2.equals("component2"));
      } else if (hostGroup.getName().equals("group2")) {
        assertEquals("2", hostGroup.getCardinality());
        assertEquals(1, componentEntities.size());
        HostGroupComponentEntity componentEntity = componentEntities.iterator().next();
        assertEquals("component1", componentEntity.getName());
      } else {
        fail("Unexpected host group name");
      }
    }
    Collection<BlueprintConfigEntity> configurations = entity.getConfigurations();
    if (containsConfig) {
      assertEquals(1, configurations.size());
      BlueprintConfigEntity blueprintConfigEntity = configurations.iterator().next();
      assertEquals(BLUEPRINT_NAME, blueprintConfigEntity.getBlueprintName());
      assertSame(entity, blueprintConfigEntity.getBlueprintEntity());
      assertEquals("core-site", blueprintConfigEntity.getType());
      Map<String, String> properties = gson.<Map<String, String>>fromJson(
          blueprintConfigEntity.getConfigData(), Map.class);
      assertEquals(2, properties.size());
      assertEquals("480", properties.get("fs.trash.interval"));
      assertEquals("8500", properties.get("ipc.client.idlethreshold"));
    } else {
      assertEquals(0, configurations.size());
    }
  }

  private void validateResource(Resource resource, boolean containsConfig) {
    assertEquals(BLUEPRINT_NAME, resource.getPropertyValue(BlueprintResourceProvider.BLUEPRINT_NAME_PROPERTY_ID));
    assertEquals("test-stack-name", resource.getPropertyValue(BlueprintResourceProvider.STACK_NAME_PROPERTY_ID));
    assertEquals("test-stack-version", resource.getPropertyValue(BlueprintResourceProvider.STACK_VERSION_PROPERTY_ID));

    Collection<Map<String, Object>> hostGroupProperties = (Collection<Map<String, Object>>)
        resource.getPropertyValue(BlueprintResourceProvider.HOST_GROUP_PROPERTY_ID);

    assertEquals(2, hostGroupProperties.size());
    for (Map<String, Object> hostGroupProps : hostGroupProperties) {
      String name = (String) hostGroupProps.get(BlueprintResourceProvider.HOST_GROUP_NAME_PROPERTY_ID);
      assertTrue(name.equals("group1") || name.equals("group2"));
      List<Map<String, String>> listComponents = (List<Map<String, String>>)
          hostGroupProps.get(BlueprintResourceProvider.COMPONENT_PROPERTY_ID);
      if (name.equals("group1")) {
        assertEquals("1", hostGroupProps.get(BlueprintResourceProvider.HOST_GROUP_CARDINALITY_PROPERTY_ID));
        assertEquals(2, listComponents.size());
        Map<String, String> mapComponent = listComponents.get(0);
        String componentName = mapComponent.get(BlueprintResourceProvider.COMPONENT_NAME_PROPERTY_ID);
        assertTrue(componentName.equals("component1") || componentName.equals("component2"));
        mapComponent = listComponents.get(1);
        String componentName2 = mapComponent.get(BlueprintResourceProvider.COMPONENT_NAME_PROPERTY_ID);
        assertFalse(componentName2.equals(componentName));
        assertTrue(componentName2.equals("component1") || componentName2.equals("component2"));
      } else if (name.equals("group2")) {
        assertEquals("2", hostGroupProps.get(BlueprintResourceProvider.HOST_GROUP_CARDINALITY_PROPERTY_ID));
        assertEquals(1, listComponents.size());
        Map<String, String> mapComponent = listComponents.get(0);
        String componentName = mapComponent.get(BlueprintResourceProvider.COMPONENT_NAME_PROPERTY_ID);
        assertEquals("component1", componentName);
      } else {
        fail("Unexpected host group name");
      }
    }

    if (containsConfig) {
      Collection<Map<String, Object>> blueprintConfigurations = (Collection<Map<String, Object>>)
          resource.getPropertyValue(BlueprintResourceProvider.CONFIGURATION_PROPERTY_ID);
      assertEquals(1, blueprintConfigurations.size());

      Map<String, Object> typeConfigs = blueprintConfigurations.iterator().next();
      assertEquals(1, typeConfigs.size());
      Map<String, String> properties = (Map<String, String>) typeConfigs.get("core-site");
      assertEquals(2, properties.size());
      assertEquals("480", properties.get("fs.trash.interval"));
      assertEquals("8500", properties.get("ipc.client.idlethreshold"));
    }
  }

  private BlueprintResourceProvider createProvider() {
    return new BlueprintResourceProvider(
        PropertyHelper.getPropertyIds(Resource.Type.Blueprint),
        PropertyHelper.getKeyPropertyIds(Resource.Type.Blueprint));
  }
}

