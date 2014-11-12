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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.resetToStrict;
import static org.easymock.EasyMock.verify;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.AlertDispatchDAO;
import org.apache.ambari.server.orm.entities.AlertTargetEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.alert.TargetType;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;

/**
 * {@link AlertTargetResourceProvider} tests.
 */
public class AlertTargetResourceProviderTest {

  private static final Long ALERT_TARGET_ID = Long.valueOf(28);
  private static final String ALERT_TARGET_NAME = "The Administrators";
  private static final String ALERT_TARGET_DESC = "Admins and Others";
  private static final String ALERT_TARGET_TYPE = TargetType.EMAIL.name();
  private static final String ALERT_TARGET_PROPS = "{\"foo\":\"bar\",\"foobar\":\"baz\"}";
  private static final String ALERT_TARGET_PROPS2 = "{\"foobar\":\"baz\"}";

  private AlertDispatchDAO m_dao;
  private Injector m_injector;
  private AmbariManagementController m_amc;

  @Before
  public void before() {
    m_dao = createStrictMock(AlertDispatchDAO.class);
    m_amc = createMock(AmbariManagementController.class);

    // create an injector which will inject the mocks
    m_injector = Guice.createInjector(Modules.override(
        new InMemoryDefaultTestModule()).with(new MockModule()));

    Assert.assertNotNull(m_injector);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testGetResourcesNoPredicate() throws Exception {
    Request request = PropertyHelper.getReadRequest(
        AlertTargetResourceProvider.ALERT_TARGET_DESCRIPTION,
        AlertTargetResourceProvider.ALERT_TARGET_ID,
        AlertTargetResourceProvider.ALERT_TARGET_NAME,
        AlertTargetResourceProvider.ALERT_TARGET_NOTIFICATION_TYPE);

    expect(m_dao.findAllTargets()).andReturn(getMockEntities());
    replay(m_dao);

    AlertTargetResourceProvider provider = createProvider(m_amc);
    Set<Resource> results = provider.getResources(request, null);

    assertEquals(1, results.size());

    Resource resource = results.iterator().next();
    Assert.assertEquals(ALERT_TARGET_NAME,
        resource.getPropertyValue(AlertTargetResourceProvider.ALERT_TARGET_NAME));

    // ensure that properties is null if not requested
    Map<String, String> properties = (Map<String, String>) resource.getPropertyValue(
        AlertTargetResourceProvider.ALERT_TARGET_PROPERTIES);

    Assert.assertNull(properties);

    verify(m_dao);
  }

  /**
   * @throws Exception
   */
  @Test
  @SuppressWarnings("unchecked")
  public void testGetSingleResource() throws Exception {
    Request request = PropertyHelper.getReadRequest(
        AlertTargetResourceProvider.ALERT_TARGET_DESCRIPTION,
        AlertTargetResourceProvider.ALERT_TARGET_ID,
        AlertTargetResourceProvider.ALERT_TARGET_NAME,
        AlertTargetResourceProvider.ALERT_TARGET_NOTIFICATION_TYPE);

    Predicate predicate = new PredicateBuilder().property(
        AlertTargetResourceProvider.ALERT_TARGET_ID).equals(
        ALERT_TARGET_ID.toString()).toPredicate();

    expect(m_dao.findTargetById(ALERT_TARGET_ID.longValue())).andReturn(
        getMockEntities().get(0)).atLeastOnce();

    replay(m_amc, m_dao);

    AlertTargetResourceProvider provider = createProvider(m_amc);
    Set<Resource> results = provider.getResources(request, predicate);
    assertEquals(1, results.size());
    Resource resource = results.iterator().next();

    Assert.assertEquals(ALERT_TARGET_ID,
        resource.getPropertyValue(AlertTargetResourceProvider.ALERT_TARGET_ID));

    Assert.assertEquals(ALERT_TARGET_NAME,
        resource.getPropertyValue(AlertTargetResourceProvider.ALERT_TARGET_NAME));

    // properties were not requested, they should not be included
    Map<String, String> properties = (Map<String, String>) resource.getPropertyValue(
        AlertTargetResourceProvider.ALERT_TARGET_PROPERTIES);

    Assert.assertNull(properties);

    // ask for all fields
    request = PropertyHelper.getReadRequest();
    results = provider.getResources(request, predicate);
    assertEquals(1, results.size());
    resource = results.iterator().next();

    // ensure properties is included
    properties = (Map<String, String>) resource.getPropertyValue(
        AlertTargetResourceProvider.ALERT_TARGET_PROPERTIES);

    assertEquals("bar", properties.get("foo"));
    assertEquals( "baz", properties.get("foobar") );

    verify(m_amc, m_dao);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testCreateResources() throws Exception {
    Capture<List<AlertTargetEntity>> listCapture = new Capture<List<AlertTargetEntity>>();

    m_dao.createTargets(capture(listCapture));
    expectLastCall();

    replay(m_amc, m_dao);

    AlertTargetResourceProvider provider = createProvider(m_amc);
    Map<String, Object> requestProps = getCreationProperties();

    Request request = PropertyHelper.getCreateRequest(Collections.singleton(requestProps), null);
    provider.createResources(request);

    Assert.assertTrue(listCapture.hasCaptured());
    AlertTargetEntity entity = listCapture.getValue().get(0);
    Assert.assertNotNull(entity);

    assertEquals(ALERT_TARGET_NAME, entity.getTargetName());
    assertEquals(ALERT_TARGET_DESC, entity.getDescription());
    assertEquals(ALERT_TARGET_TYPE, entity.getNotificationType());
    assertEquals(ALERT_TARGET_PROPS, entity.getProperties());

    verify(m_amc, m_dao);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testCreateWithRecipientArray() throws Exception {
    Capture<List<AlertTargetEntity>> listCapture = new Capture<List<AlertTargetEntity>>();

    m_dao.createTargets(capture(listCapture));
    expectLastCall();

    replay(m_amc, m_dao);

    AlertTargetResourceProvider provider = createProvider(m_amc);
    Map<String, Object> requestProps = getRecipientCreationProperties();

    Request request = PropertyHelper.getCreateRequest(
        Collections.singleton(requestProps), null);
    provider.createResources(request);

    Assert.assertTrue(listCapture.hasCaptured());
    AlertTargetEntity entity = listCapture.getValue().get(0);
    Assert.assertNotNull(entity);

    assertEquals(ALERT_TARGET_NAME, entity.getTargetName());
    assertEquals(ALERT_TARGET_DESC, entity.getDescription());
    assertEquals(ALERT_TARGET_TYPE, entity.getNotificationType());
    assertEquals(
        "{\"ambari.dispatch.recipients\":\"[\\\"ambari@ambari.apache.org\\\"]\"}",
        entity.getProperties());

    verify(m_amc, m_dao);
  }

  /**
   * @throws Exception
   */
  @Test
  @SuppressWarnings("unchecked")
  public void testUpdateResources() throws Exception {
    Capture<AlertTargetEntity> entityCapture = new Capture<AlertTargetEntity>();

    m_dao.createTargets(EasyMock.anyObject(List.class));
    expectLastCall().times(1);

    AlertTargetEntity target = new AlertTargetEntity();
    expect(m_dao.findTargetById(ALERT_TARGET_ID)).andReturn(target).times(1);

    expect(m_dao.merge(capture(entityCapture))).andReturn(target).once();

    replay(m_amc, m_dao);

    AlertTargetResourceProvider provider = createProvider(m_amc);
    Map<String, Object> requestProps = getCreationProperties();
    Request request = PropertyHelper.getCreateRequest(
        Collections.singleton(requestProps), null);
    provider.createResources(request);

    // create new properties, and include the ID since we're not going through
    // a service layer which would add it for us automatically
    requestProps = new HashMap<String, Object>();
    requestProps.put(AlertTargetResourceProvider.ALERT_TARGET_ID,
        ALERT_TARGET_ID.toString());

    String newName = ALERT_TARGET_NAME + " Foo";
    requestProps.put(AlertTargetResourceProvider.ALERT_TARGET_NAME, newName);

    requestProps.put(AlertTargetResourceProvider.ALERT_TARGET_PROPERTIES
        + "/foobar", "baz");

    Predicate predicate = new PredicateBuilder().property(
        AlertTargetResourceProvider.ALERT_TARGET_ID).equals(
        ALERT_TARGET_ID.toString()).toPredicate();

    request = PropertyHelper.getUpdateRequest(requestProps, null);
    provider.updateResources(request, predicate);

    assertTrue(entityCapture.hasCaptured());

    AlertTargetEntity entity = entityCapture.getValue();
    assertEquals(newName, entity.getTargetName());
    assertEquals(ALERT_TARGET_PROPS2, entity.getProperties());
    verify(m_amc, m_dao);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testDeleteResources() throws Exception {
    Capture<AlertTargetEntity> entityCapture = new Capture<AlertTargetEntity>();
    Capture<List<AlertTargetEntity>> listCapture = new Capture<List<AlertTargetEntity>>();

    m_dao.createTargets(capture(listCapture));
    expectLastCall();

    replay(m_amc, m_dao);

    AlertTargetResourceProvider provider = createProvider(m_amc);

    Map<String, Object> requestProps = getCreationProperties();
    Request request = PropertyHelper.getCreateRequest(Collections.singleton(requestProps), null);
    provider.createResources(request);

    Assert.assertTrue(listCapture.hasCaptured());
    AlertTargetEntity entity = listCapture.getValue().get(0);
    Assert.assertNotNull(entity);

    Predicate p = new PredicateBuilder().property(
        AlertTargetResourceProvider.ALERT_TARGET_ID).equals(
        ALERT_TARGET_ID.toString()).toPredicate();

    // everything is mocked, there is no DB
    entity.setTargetId(ALERT_TARGET_ID);

    resetToStrict(m_dao);
    expect(m_dao.findTargetById(ALERT_TARGET_ID.longValue())).andReturn(entity).anyTimes();
    m_dao.remove(capture(entityCapture));
    expectLastCall();
    replay(m_dao);

    provider.deleteResources(p);

    AlertTargetEntity entity1 = entityCapture.getValue();
    Assert.assertEquals(ALERT_TARGET_ID, entity1.getTargetId());

    verify(m_amc, m_dao);
  }

  /**
   * @param amc
   * @return
   */
  private AlertTargetResourceProvider createProvider(
      AmbariManagementController amc) {
    return new AlertTargetResourceProvider();
  }

  /**
   * @return
   */
  private List<AlertTargetEntity> getMockEntities() throws Exception {
    AlertTargetEntity entity = new AlertTargetEntity();
    entity.setTargetId(ALERT_TARGET_ID);
    entity.setDescription(ALERT_TARGET_DESC);
    entity.setTargetName(ALERT_TARGET_NAME);
    entity.setNotificationType(ALERT_TARGET_TYPE);
    entity.setProperties(ALERT_TARGET_PROPS);
    return Arrays.asList(entity);
  }

  /**
   * Gets the maps of properties that simulate a deserialzied JSON request to
   * create an alert target.
   *
   * @return
   * @throws Exception
   */
  private Map<String, Object> getCreationProperties() throws Exception {
    Map<String, Object> requestProps = new HashMap<String, Object>();
    requestProps.put(AlertTargetResourceProvider.ALERT_TARGET_NAME,
        ALERT_TARGET_NAME);

    requestProps.put(AlertTargetResourceProvider.ALERT_TARGET_DESCRIPTION,
        ALERT_TARGET_DESC);

    requestProps.put(
        AlertTargetResourceProvider.ALERT_TARGET_NOTIFICATION_TYPE,
        ALERT_TARGET_TYPE);

    requestProps.put(AlertTargetResourceProvider.ALERT_TARGET_PROPERTIES
        + "/foo", "bar");

    requestProps.put(AlertTargetResourceProvider.ALERT_TARGET_PROPERTIES
        + "/foobar", "baz");

    return requestProps;
  }

  /**
   * Gets the maps of properties that simulate a deserialzied JSON request with
   * a nested JSON array.
   *
   * @return
   * @throws Exception
   */
  private Map<String, Object> getRecipientCreationProperties() throws Exception {
    Map<String, Object> requestProps = new HashMap<String, Object>();
    requestProps.put(AlertTargetResourceProvider.ALERT_TARGET_NAME,
        ALERT_TARGET_NAME);

    requestProps.put(AlertTargetResourceProvider.ALERT_TARGET_DESCRIPTION,
        ALERT_TARGET_DESC);

    requestProps.put(
        AlertTargetResourceProvider.ALERT_TARGET_NOTIFICATION_TYPE,
        ALERT_TARGET_TYPE);

    requestProps.put(AlertTargetResourceProvider.ALERT_TARGET_PROPERTIES
        + "/ambari.dispatch.recipients", "[\"ambari@ambari.apache.org\"]");

    return requestProps;
  }

  /**
  *
  */
  private class MockModule implements Module {
    /**
    *
    */
    @Override
    public void configure(Binder binder) {
      binder.bind(AlertDispatchDAO.class).toInstance(m_dao);
      binder.bind(Clusters.class).toInstance(
          EasyMock.createNiceMock(Clusters.class));
      binder.bind(Cluster.class).toInstance(
          EasyMock.createNiceMock(Cluster.class));
      binder.bind(ActionMetadata.class);
    }
  }
}
