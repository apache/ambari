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

import com.google.common.collect.Lists;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.AdminSettingDAO;
import org.apache.ambari.server.orm.entities.AdminSettingEntity;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.commons.lang.RandomStringUtils;
import org.easymock.Capture;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.apache.ambari.server.controller.internal.AdminSettingResourceProvider.ADMINSETTING_NAME_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.AdminSettingResourceProvider.ADMINSETTING_SETTING_TYPE_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.AdminSettingResourceProvider.ADMINSETTING_CONTENT_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.AdminSettingResourceProvider.ADMINSETTING_UPDATED_BY_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.AdminSettingResourceProvider.ADMINSETTING_UPDATE_TIMESTAMP_PROPERTY_ID;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.createControl;

import static org.junit.Assert.assertEquals;

public class AdminSettingResourceProviderTest {
  IMocksControl mockControl;
  AdminSettingDAO dao;
  AdminSettingResourceProvider resourceProvider;


  @Before
  public void setUp() throws Exception {
    mockControl = createControl();
    dao = mockControl.createMock(AdminSettingDAO.class);
    resourceProvider = new AdminSettingResourceProvider();
    setPrivateField(resourceProvider, "dao", dao);
  }

  @After
  public void tearDown() {
    mockControl.verify();
    SecurityContextHolder.getContext().setAuthentication(null);
    mockControl.reset();
  }

  @Test(expected = AuthorizationException.class)
  public void testGetResources_instance_noAuth() throws Exception {
    getResources_instance(newEntity("motd"), readRequest());
  }

  @Test
  public void testGetResources_instance_clusterUser() throws Exception {
    setupAuthenticationForClusterUser();
    String name = "motd";
    AdminSettingEntity entity = newEntity(name);

    Set<Resource> response = getResources_instance(entity, readRequest());
    assertEquals(1, response.size());
    Resource resource = response.iterator().next();
    assertEqualsEntityAndResource(entity, resource);
  }

  @Test
  public void testGetResources_instance_admin() throws Exception {
    setupAuthenticationForAdmin();
    AdminSettingEntity entity = newEntity("motd");
    Set<Resource> response = getResources_instance(entity, readRequest());
    assertEquals(1, response.size());
    Resource resource = response.iterator().next();
    assertEqualsEntityAndResource(entity, resource);
  }

  @Test(expected = AuthorizationException.class)
  public void testGetResources_collection_noAuth() throws Exception {
    mockControl.replay();
    Request request = PropertyHelper.getReadRequest(
            ADMINSETTING_NAME_PROPERTY_ID,
            ADMINSETTING_CONTENT_PROPERTY_ID,
            ADMINSETTING_SETTING_TYPE_PROPERTY_ID,
            ADMINSETTING_UPDATED_BY_PROPERTY_ID,
            ADMINSETTING_UPDATE_TIMESTAMP_PROPERTY_ID);
    resourceProvider.getResources(request, null);
  }

  @Test
  public void testGetResources_collection_clusterUser() throws Exception {
    setupAuthenticationForClusterUser();

    AdminSettingEntity entity1 = newEntity("motd");
    AdminSettingEntity entity2 = newEntity("ldap");
    Request request = PropertyHelper.getReadRequest(
            ADMINSETTING_NAME_PROPERTY_ID,
            ADMINSETTING_CONTENT_PROPERTY_ID,
            ADMINSETTING_SETTING_TYPE_PROPERTY_ID,
            ADMINSETTING_UPDATED_BY_PROPERTY_ID,
            ADMINSETTING_UPDATE_TIMESTAMP_PROPERTY_ID);

    expect(dao.findAll()).andReturn(Lists.newArrayList(entity1, entity2));
    mockControl.replay();

    Set<Resource> response = resourceProvider.getResources(request, null);
    assertEquals(2, response.size());
    Map<Object, Resource> resourceMap = new HashMap<>();
    Iterator<Resource> resourceIterator = response.iterator();
    Resource nextResource = resourceIterator.next();
    resourceMap.put(nextResource.getPropertyValue(ADMINSETTING_NAME_PROPERTY_ID), nextResource);
    nextResource = resourceIterator.next();
    resourceMap.put(nextResource.getPropertyValue(ADMINSETTING_NAME_PROPERTY_ID), nextResource);
    assertEqualsEntityAndResource(entity1, resourceMap.get(entity1.getName()));
    assertEqualsEntityAndResource(entity2, resourceMap.get(entity2.getName()));
  }

  @Test
  public void testGetResources_collection_admin() throws Exception {
    setupAuthenticationForAdmin();

    AdminSettingEntity entity1 = newEntity("motd");
    AdminSettingEntity entity2 = newEntity("ldap");
    Request request = PropertyHelper.getReadRequest(
            ADMINSETTING_NAME_PROPERTY_ID,
            ADMINSETTING_CONTENT_PROPERTY_ID,
            ADMINSETTING_SETTING_TYPE_PROPERTY_ID,
            ADMINSETTING_UPDATED_BY_PROPERTY_ID,
            ADMINSETTING_UPDATE_TIMESTAMP_PROPERTY_ID);
    expect(dao.findAll()).andReturn(Lists.newArrayList(entity1, entity2));
    mockControl.replay();

    Set<Resource> response = resourceProvider.getResources(request, null);
    assertEquals(2, response.size());
    Map<Object, Resource> resourceMap = new HashMap<>();
    Iterator<Resource> resourceIterator = response.iterator();
    Resource nextResource = resourceIterator.next();
    resourceMap.put(nextResource.getPropertyValue(ADMINSETTING_NAME_PROPERTY_ID), nextResource);
    nextResource = resourceIterator.next();
    resourceMap.put(nextResource.getPropertyValue(ADMINSETTING_NAME_PROPERTY_ID), nextResource);
    assertEqualsEntityAndResource(entity1, resourceMap.get(entity1.getName()));
    assertEqualsEntityAndResource(entity2, resourceMap.get(entity2.getName()));
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResource_noAuth() throws Exception {
    mockControl.replay();
    resourceProvider.createResources(createRequest(newEntity("moted")));
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResource_clusterUser() throws Exception {
    setupAuthenticationForClusterUser();
    mockControl.replay();
    resourceProvider.createResources(createRequest(newEntity("motd")));
  }

  @Test
  public void testCreateResource_admin() throws Exception {
    setupAuthenticationForAdmin();
    AdminSettingEntity entity = newEntity("motd");
    Capture<AdminSettingEntity> entityCapture = Capture.newInstance();
    Request request = createRequest(entity);

    dao.create(capture(entityCapture));
    mockControl.replay();

    RequestStatus response = resourceProvider.createResources(request);
    assertEquals(RequestStatus.Status.Complete, response.getStatus());
    Set<Resource> associatedResources = response.getAssociatedResources();
    assertEquals(1, associatedResources.size());
    AdminSettingEntity capturedEntity = entityCapture.getValue();
    assertEquals(entity.getName(), capturedEntity.getName());
    assertEquals(entity.getContent(), capturedEntity.getContent());
    assertEquals(entity.getSettingType(), capturedEntity.getSettingType());
    assertEquals(AuthorizationHelper.getAuthenticatedName(), capturedEntity.getUpdatedBy());
  }


  @Test(expected = AuthorizationException.class)
  public void testUpdateResources_noAuth() throws Exception {
    mockControl.replay();
    resourceProvider.updateResources(updateRequest(newEntity("motd")), null);
  }


  @Test(expected = AuthorizationException.class)
  public void testUpdateResources_clusterUser() throws Exception {
    setupAuthenticationForClusterUser();
    mockControl.replay();
    resourceProvider.updateResources(updateRequest(newEntity("motd")), null);
  }


  @Test
  public void testUpdateResources_admin() throws Exception {
    setupAuthenticationForAdmin();
    String name = "motd";
    AdminSettingEntity oldEntity = newEntity(name);
    AdminSettingEntity updatedEntity = oldEntity.clone();
    updatedEntity.setContent("{text}");
    updatedEntity.setSettingType("new-type");

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate = pb.begin().property(ADMINSETTING_NAME_PROPERTY_ID).equals(name).end().toPredicate();
    Capture<AdminSettingEntity> capture = Capture.newInstance();

    expect(dao.findByName(name)).andReturn(oldEntity);
    expect(dao.merge(capture(capture))).andReturn(updatedEntity);
    mockControl.replay();

    RequestStatus response = resourceProvider.updateResources(updateRequest(updatedEntity), predicate);
    AdminSettingEntity capturedEntity = capture.getValue();
    assertEquals(RequestStatus.Status.Complete, response.getStatus());
    assertEquals(updatedEntity.getId(), capturedEntity.getId());
    assertEquals(updatedEntity.getName(), capturedEntity.getName());
    assertEquals(updatedEntity.getSettingType(), capturedEntity.getSettingType());
    assertEquals(updatedEntity.getContent(), capturedEntity.getContent());
    assertEquals(AuthorizationHelper.getAuthenticatedName(), capturedEntity.getUpdatedBy());
  }

  @Test(expected = AuthorizationException.class)
  public void testDeleteResources_noAuth() throws Exception {
    mockControl.replay();
    resourceProvider.deleteResources(null);
  }


  @Test(expected = AuthorizationException.class)
  public void testDeleteResources_clusterUser() throws Exception {
    setupAuthenticationForClusterUser();
    mockControl.replay();
    resourceProvider.deleteResources(null);
  }

  @Test
  public void testDeleteResources() throws Exception {
    setupAuthenticationForAdmin();

    String name = "motd";
    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate = pb.begin().property(ADMINSETTING_NAME_PROPERTY_ID).equals(name).end().toPredicate();
    dao.removeByName(name);
    mockControl.replay();
    resourceProvider.deleteResources(predicate);
  }

  private Set<Resource> getResources_instance(AdminSettingEntity entity, Request request) throws Exception {
    String name = entity.getName();
    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate = pb.begin().property(ADMINSETTING_NAME_PROPERTY_ID).equals(name).end().toPredicate();

    expect(dao.findByName(name)).andReturn(entity).anyTimes();
    mockControl.replay();
    return resourceProvider.getResources(request, predicate);
  }


  private Request readRequest() {
    return PropertyHelper.getReadRequest(
            ADMINSETTING_NAME_PROPERTY_ID,
            ADMINSETTING_CONTENT_PROPERTY_ID,
            ADMINSETTING_SETTING_TYPE_PROPERTY_ID,
            ADMINSETTING_UPDATED_BY_PROPERTY_ID,
            ADMINSETTING_UPDATE_TIMESTAMP_PROPERTY_ID);
  }

  private Request createRequest(AdminSettingEntity entity) {
    Map<String, Object> properties = new HashMap<>();
    properties.put(ADMINSETTING_NAME_PROPERTY_ID, entity.getName());
    properties.put(ADMINSETTING_CONTENT_PROPERTY_ID, entity.getContent());
    properties.put(ADMINSETTING_UPDATED_BY_PROPERTY_ID, entity.getUpdatedBy());
    properties.put(ADMINSETTING_SETTING_TYPE_PROPERTY_ID, entity.getSettingType());
    return PropertyHelper.getCreateRequest(Collections.singleton(properties), null);
  }

  private Request updateRequest(AdminSettingEntity entity) {
    Map<String, Object> properties = new HashMap<>();
    properties.put(ADMINSETTING_NAME_PROPERTY_ID, entity.getName());
    properties.put(ADMINSETTING_CONTENT_PROPERTY_ID, entity.getContent());
    properties.put(ADMINSETTING_SETTING_TYPE_PROPERTY_ID, entity.getSettingType());
    return PropertyHelper.getUpdateRequest(properties, null);
  }

  private void setupAuthenticationForClusterUser() {
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createClusterUser());
  }

  private void setupAuthenticationForAdmin() {
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());
  }

  private AdminSettingEntity newEntity(String name) {
    AdminSettingEntity entity = new AdminSettingEntity();
    entity.setName(name);
    entity.setContent(RandomStringUtils.randomAlphabetic(10));
    entity.setSettingType(RandomStringUtils.randomAlphabetic(5));
    entity.setUpdatedBy("ambari");
    entity.setUpdateTimestamp(System.currentTimeMillis());
    return entity;
  }

  private void assertEqualsEntityAndResource(AdminSettingEntity entity, Resource resource) {
    assertEquals(entity.getName(), resource.getPropertyValue(ADMINSETTING_NAME_PROPERTY_ID));
    assertEquals(entity.getSettingType(), resource.getPropertyValue(ADMINSETTING_SETTING_TYPE_PROPERTY_ID));
    assertEquals(entity.getContent(), resource.getPropertyValue(ADMINSETTING_CONTENT_PROPERTY_ID));
    assertEquals(entity.getUpdatedBy(), resource.getPropertyValue(ADMINSETTING_UPDATED_BY_PROPERTY_ID));
    assertEquals(entity.getUpdateTimestamp(), resource.getPropertyValue(ADMINSETTING_UPDATE_TIMESTAMP_PROPERTY_ID));
  }

  private void setPrivateField(Object o, String field, Object value) throws Exception{
    Class<?> c = o.getClass();
    Field f = c.getDeclaredField(field);
    f.setAccessible(true);
    f.set(o, value);
  }
}
