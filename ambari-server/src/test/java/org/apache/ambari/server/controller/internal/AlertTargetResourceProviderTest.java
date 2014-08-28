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

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.resetToStrict;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

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

  private AlertDispatchDAO m_dao;
  private Injector m_injector;

  @Before
  public void before() {
    m_dao = createStrictMock(AlertDispatchDAO.class);

    m_injector = Guice.createInjector(Modules.override(
        new InMemoryDefaultTestModule()).with(new MockModule()));

    AlertTargetResourceProvider.init(m_injector);
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

    AmbariManagementController amc = createMock(AmbariManagementController.class);
    AlertTargetResourceProvider provider = createProvider(amc);
    Set<Resource> results = provider.getResources(request, null);

    assertEquals(1, results.size());

    Resource r = results.iterator().next();
    Assert.assertEquals(ALERT_TARGET_NAME,
        r.getPropertyValue(AlertTargetResourceProvider.ALERT_TARGET_NAME));

    verify(m_dao);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testGetSingleResource() throws Exception {
    Request request = PropertyHelper.getReadRequest(
        AlertTargetResourceProvider.ALERT_TARGET_DESCRIPTION,
        AlertTargetResourceProvider.ALERT_TARGET_ID,
        AlertTargetResourceProvider.ALERT_TARGET_NAME,
        AlertTargetResourceProvider.ALERT_TARGET_NOTIFICATION_TYPE);

    AmbariManagementController amc = createMock(AmbariManagementController.class);

    Predicate predicate = new PredicateBuilder().property(
        AlertTargetResourceProvider.ALERT_TARGET_ID).equals(
        ALERT_TARGET_ID.toString()).toPredicate();

    expect(m_dao.findTargetById(ALERT_TARGET_ID.longValue())).andReturn(
        getMockEntities().get(0));

    replay(amc, m_dao);

    AlertTargetResourceProvider provider = createProvider(amc);
    Set<Resource> results = provider.getResources(request, predicate);

    assertEquals(1, results.size());

    Resource r = results.iterator().next();
    Assert.assertEquals(ALERT_TARGET_ID,
        r.getPropertyValue(AlertTargetResourceProvider.ALERT_TARGET_ID));

    Assert.assertEquals(ALERT_TARGET_NAME,
        r.getPropertyValue(AlertTargetResourceProvider.ALERT_TARGET_NAME));

    verify(amc, m_dao);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testCreateResources() throws Exception {
    AmbariManagementController amc = createMock(AmbariManagementController.class);
    Capture<List<AlertTargetEntity>> listCapture = new Capture<List<AlertTargetEntity>>();

    m_dao.createTargets(capture(listCapture));
    expectLastCall();

    replay(amc, m_dao);

    AlertTargetResourceProvider provider = createProvider(amc);
    Map<String, Object> requestProps = new HashMap<String, Object>();
    requestProps.put(AlertTargetResourceProvider.ALERT_TARGET_NAME,
        ALERT_TARGET_NAME);

    requestProps.put(AlertTargetResourceProvider.ALERT_TARGET_DESCRIPTION,
        ALERT_TARGET_DESC);

    requestProps.put(
        AlertTargetResourceProvider.ALERT_TARGET_NOTIFICATION_TYPE,
        ALERT_TARGET_TYPE);

    Request request = PropertyHelper.getCreateRequest(Collections.singleton(requestProps), null);
    provider.createResources(request);

    Assert.assertTrue(listCapture.hasCaptured());
    AlertTargetEntity entity = listCapture.getValue().get(0);
    Assert.assertNotNull(entity);

    Assert.assertEquals(ALERT_TARGET_NAME, entity.getTargetName());
    Assert.assertEquals(ALERT_TARGET_DESC, entity.getDescription());
    Assert.assertEquals(ALERT_TARGET_TYPE, entity.getNotificationType());

    verify(amc, m_dao);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testUpdateResources() throws Exception {
  }

  /**
   * @throws Exception
   */
  @Test
  public void testDeleteResources() throws Exception {
    AmbariManagementController amc = createMock(AmbariManagementController.class);
    Capture<AlertTargetEntity> entityCapture = new Capture<AlertTargetEntity>();
    Capture<List<AlertTargetEntity>> listCapture = new Capture<List<AlertTargetEntity>>();

    m_dao.createTargets(capture(listCapture));
    expectLastCall();

    replay(amc, m_dao);

    AlertTargetResourceProvider provider = createProvider(amc);

    Map<String, Object> requestProps = new HashMap<String, Object>();
    requestProps.put(AlertTargetResourceProvider.ALERT_TARGET_NAME,
        ALERT_TARGET_NAME);

    requestProps.put(AlertTargetResourceProvider.ALERT_TARGET_DESCRIPTION,
        ALERT_TARGET_DESC);

    requestProps.put(
        AlertTargetResourceProvider.ALERT_TARGET_NOTIFICATION_TYPE,
        ALERT_TARGET_TYPE);

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

    verify(amc, m_dao);
  }

  /**
   * @param amc
   * @return
   */
  private AlertTargetResourceProvider createProvider(
      AmbariManagementController amc) {
    return new AlertTargetResourceProvider(
        PropertyHelper.getPropertyIds(Resource.Type.AlertTarget),
        PropertyHelper.getKeyPropertyIds(Resource.Type.AlertTarget), amc);
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
    return Arrays.asList(entity);
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
