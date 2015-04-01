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

import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceDataEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.view.ViewRegistry;
import org.apache.ambari.server.view.configuration.ViewConfig;
import org.apache.ambari.view.ViewDefinition;
import org.easymock.Capture;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;

public class ViewInstanceResourceProviderTest {

  private static final ViewRegistry singleton = createMock(ViewRegistry.class);

  static {
    ViewRegistry.initInstance(singleton);
  }

  @Before
  public void before() {
    reset(singleton);
  }

  @Test
  public void testToResource() throws Exception {
    ViewInstanceResourceProvider provider = new ViewInstanceResourceProvider();
    Set<String> propertyIds = new HashSet<String>();
    propertyIds.add(ViewInstanceResourceProvider.PROPERTIES_PROPERTY_ID);
    propertyIds.add(ViewInstanceResourceProvider.CLUSTER_HANDLE_PROPERTY_ID);
    ViewInstanceEntity viewInstanceEntity = createNiceMock(ViewInstanceEntity.class);
    ViewEntity viewEntity = createNiceMock(ViewEntity.class);
    expect(viewInstanceEntity.getViewEntity()).andReturn(viewEntity).anyTimes();

    Map<String, String> propertyMap = new HashMap<String, String>();

    propertyMap.put("par1", "val1");
    propertyMap.put("par2", "val2");

    expect(viewInstanceEntity.getPropertyMap()).andReturn(propertyMap);

    expect(viewInstanceEntity.getData()).andReturn(Collections.<ViewInstanceDataEntity>emptyList()).anyTimes();

    expect(singleton.checkAdmin()).andReturn(true);
    expect(singleton.checkAdmin()).andReturn(false);

    expect(viewInstanceEntity.getClusterHandle()).andReturn("c1");

    replay(singleton, viewEntity, viewInstanceEntity);

    // as admin
    Resource resource = provider.toResource(viewInstanceEntity, propertyIds);
    Map<String, Map<String, Object>> properties = resource.getPropertiesMap();
    assertEquals(2, properties.size());
    Map<String, Object> props = properties.get("ViewInstanceInfo");
    assertNotNull(props);
    assertEquals(1, props.size());
    assertEquals("c1", props.get("cluster_handle"));

    props = properties.get("ViewInstanceInfo/properties");
    assertNotNull(props);
    assertEquals(2, props.size());
    assertEquals("val1", props.get("par1"));
    assertEquals("val2", props.get("par2"));

    // as non-admin
    resource = provider.toResource(viewInstanceEntity, propertyIds);
    properties = resource.getPropertiesMap();
    props = properties.get("ViewInstanceInfo/properties");
    assertNull(props);

    verify(singleton, viewEntity, viewInstanceEntity);
  }

  @Test
  public void testCreateResources() throws Exception {
    ViewInstanceResourceProvider provider = new ViewInstanceResourceProvider();

    Set<Map<String, Object>> properties = new HashSet<Map<String, Object>>();

    Map<String, Object> propertyMap = new HashMap<String, Object>();

    propertyMap.put(ViewInstanceResourceProvider.VIEW_NAME_PROPERTY_ID, "V1");
    propertyMap.put(ViewInstanceResourceProvider.VIEW_VERSION_PROPERTY_ID, "1.0.0");
    propertyMap.put(ViewInstanceResourceProvider.INSTANCE_NAME_PROPERTY_ID, "I1");
    propertyMap.put(ViewInstanceResourceProvider.PROPERTIES_PROPERTY_ID + "/test_property", "test_value");

    properties.add(propertyMap);

    ViewInstanceEntity viewInstanceEntity = new ViewInstanceEntity();
    viewInstanceEntity.setViewName("V1{1.0.0}");
    viewInstanceEntity.setName("I1");

    ViewInstanceEntity viewInstanceEntity2 = new ViewInstanceEntity();
    viewInstanceEntity2.setViewName("V1{1.0.0}");
    viewInstanceEntity2.setName("I1");

    ViewEntity viewEntity = new ViewEntity();
    viewEntity.setStatus(ViewDefinition.ViewStatus.DEPLOYED);
    viewEntity.setName("V1{1.0.0}");

    viewInstanceEntity.setViewEntity(viewEntity);
    viewInstanceEntity2.setViewEntity(viewEntity);

    expect(singleton.instanceExists(viewInstanceEntity)).andReturn(false);
    expect(singleton.instanceExists(viewInstanceEntity2)).andReturn(false);
    expect(singleton.getDefinition("V1", "1.0.0")).andReturn(viewEntity).anyTimes();
    expect(singleton.getDefinition("V1", null)).andReturn(viewEntity).anyTimes();

    Capture<Map<String, String>> captureProperties = new Capture<Map<String, String>>();

    singleton.setViewInstanceProperties(eq(viewInstanceEntity), capture(captureProperties),
        anyObject(ViewConfig.class), anyObject(ClassLoader.class));

    Capture<ViewInstanceEntity> instanceEntityCapture = new Capture<ViewInstanceEntity>();
    singleton.installViewInstance(capture(instanceEntityCapture));
    expectLastCall().anyTimes();

    expect(singleton.checkAdmin()).andReturn(true);
    expect(singleton.checkAdmin()).andReturn(false);

    replay(singleton);

    // as admin
    provider.createResources(PropertyHelper.getCreateRequest(properties, null));
    assertEquals(viewInstanceEntity, instanceEntityCapture.getValue());
    Map<String, String> props = captureProperties.getValue();
    assertEquals(1, props.size());
    assertEquals("test_value", props.get("test_property"));

    // as non-admin
    provider.createResources(PropertyHelper.getCreateRequest(properties, null));
    assertEquals(viewInstanceEntity2, instanceEntityCapture.getValue());
    props = viewInstanceEntity2.getPropertyMap();
    assertTrue(props.isEmpty());

    verify(singleton);
  }

  @Test
  public void testCreateResources_existingInstance() throws Exception {
    ViewInstanceResourceProvider provider = new ViewInstanceResourceProvider();

    Set<Map<String, Object>> properties = new HashSet<Map<String, Object>>();

    Map<String, Object> propertyMap = new HashMap<String, Object>();

    propertyMap.put(ViewInstanceResourceProvider.VIEW_NAME_PROPERTY_ID, "V1");
    propertyMap.put(ViewInstanceResourceProvider.VIEW_VERSION_PROPERTY_ID, "1.0.0");
    propertyMap.put(ViewInstanceResourceProvider.INSTANCE_NAME_PROPERTY_ID, "I1");

    properties.add(propertyMap);

    ViewInstanceEntity viewInstanceEntity = new ViewInstanceEntity();
    viewInstanceEntity.setViewName("V1{1.0.0}");
    viewInstanceEntity.setName("I1");

    ViewEntity viewEntity = new ViewEntity();
    viewEntity.setStatus(ViewDefinition.ViewStatus.DEPLOYED);
    viewEntity.setName("V1{1.0.0}");

    viewInstanceEntity.setViewEntity(viewEntity);

    expect(singleton.instanceExists(viewInstanceEntity)).andReturn(true);
    expect(singleton.getDefinition("V1", "1.0.0")).andReturn(viewEntity).anyTimes();
    expect(singleton.getDefinition("V1", null)).andReturn(viewEntity);

    expect(singleton.checkAdmin()).andReturn(true);

    replay(singleton);

    try {
      provider.createResources(PropertyHelper.getCreateRequest(properties, null));
      fail("Expected ResourceAlreadyExistsException.");
    } catch (ResourceAlreadyExistsException e) {
      // expected
    }

    verify(singleton);
  }

  @Test
  public void testCreateResources_viewNotLoaded() throws Exception {
    ViewInstanceResourceProvider provider = new ViewInstanceResourceProvider();

    Set<Map<String, Object>> properties = new HashSet<Map<String, Object>>();

    Map<String, Object> propertyMap = new HashMap<String, Object>();

    propertyMap.put(ViewInstanceResourceProvider.VIEW_NAME_PROPERTY_ID, "V1");
    propertyMap.put(ViewInstanceResourceProvider.VIEW_VERSION_PROPERTY_ID, "1.0.0");
    propertyMap.put(ViewInstanceResourceProvider.INSTANCE_NAME_PROPERTY_ID, "I1");

    properties.add(propertyMap);

    ViewEntity viewEntity = new ViewEntity();
    viewEntity.setName("V1{1.0.0}");
    viewEntity.setStatus(ViewDefinition.ViewStatus.DEPLOYING);
    ViewInstanceEntity viewInstanceEntity = new ViewInstanceEntity();
    viewInstanceEntity.setViewName("V1{1.0.0}");
    viewInstanceEntity.setName("I1");
    viewInstanceEntity.setViewEntity(viewEntity);

    expect(singleton.getDefinition("V1", "1.0.0")).andReturn(viewEntity).anyTimes();
    expect(singleton.getDefinition("V1", null)).andReturn(viewEntity);

    expect(singleton.checkAdmin()).andReturn(true);

    replay(singleton);

    try {
      provider.createResources(PropertyHelper.getCreateRequest(properties, null));
      fail("Expected IllegalStateException.");
    } catch (IllegalStateException e) {
      // expected
    }

    verify(singleton);
  }

  @Test
  public void testUpdateResources_viewNotLoaded() throws Exception {
    ViewInstanceResourceProvider provider = new ViewInstanceResourceProvider();

    Set<Map<String, Object>> properties = new HashSet<Map<String, Object>>();

    Map<String, Object> propertyMap = new HashMap<String, Object>();

    propertyMap.put(ViewInstanceResourceProvider.ICON_PATH_ID, "path");

    properties.add(propertyMap);

    PredicateBuilder predicateBuilder = new PredicateBuilder();
    Predicate predicate =
        predicateBuilder.property(ViewInstanceResourceProvider.VIEW_NAME_PROPERTY_ID).equals("V1").toPredicate();
    ViewEntity viewEntity = new ViewEntity();
    viewEntity.setName("V1{1.0.0}");
    viewEntity.setStatus(ViewDefinition.ViewStatus.DEPLOYING);
    ViewInstanceEntity viewInstanceEntity = new ViewInstanceEntity();
    viewInstanceEntity.setViewName("V1{1.0.0}");
    viewInstanceEntity.setName("I1");
    viewInstanceEntity.setViewEntity(viewEntity);

    expect(singleton.getDefinitions()).andReturn(Collections.singleton(viewEntity));

    replay(singleton);

    provider.updateResources(PropertyHelper.getCreateRequest(properties, null), predicate);

    Assert.assertNull(viewInstanceEntity.getIcon());

    verify(singleton);
  }

  @Test
  public void testDeleteResources_viewNotLoaded() throws Exception {
    ViewInstanceResourceProvider provider = new ViewInstanceResourceProvider();

    PredicateBuilder predicateBuilder = new PredicateBuilder();
    Predicate predicate =
        predicateBuilder.property(ViewInstanceResourceProvider.VIEW_NAME_PROPERTY_ID).equals("V1").toPredicate();

    ViewEntity viewEntity = new ViewEntity();
    viewEntity.setName("V1{1.0.0}");
    viewEntity.setStatus(ViewDefinition.ViewStatus.DEPLOYING);
    ViewInstanceEntity viewInstanceEntity = new ViewInstanceEntity();
    viewInstanceEntity.setViewName("V1{1.0.0}");
    viewInstanceEntity.setName("I1");
    viewInstanceEntity.setViewEntity(viewEntity);

    expect(singleton.getDefinitions()).andReturn(Collections.singleton(viewEntity));

    replay(singleton);

    provider.deleteResources(predicate);

    verify(singleton);
  }
}