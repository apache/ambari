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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.ambari.server.controller.AlertNoticeRequest;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.AlertDispatchDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.orm.entities.AlertNoticeEntity;
import org.apache.ambari.server.orm.entities.AlertTargetEntity;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.NotificationState;
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
 * {@link AlertNoticeResourceProvider} tests.
 */
public class AlertNoticeResourceProviderTest {

  private AlertDispatchDAO m_dao = null;
  private Injector m_injector;

  @Before
  public void before() {
    m_dao = createStrictMock(AlertDispatchDAO.class);

    // create an injector which will inject the mocks
    m_injector = Guice.createInjector(Modules.override(
        new InMemoryDefaultTestModule()).with(new MockModule()));

    Assert.assertNotNull(m_injector);
  }

  /**
   * @throws Exception
   */
  @Test
  @SuppressWarnings("unchecked")
  public void testGetResourcesNoPredicate() throws Exception {
    AlertNoticeResourceProvider provider = createProvider();

    Request request = PropertyHelper.getReadRequest(
        "AlertHistory/cluster_name", "AlertHistory/id");

    expect(m_dao.findAllNotices(EasyMock.anyObject(AlertNoticeRequest.class))).andReturn(
        Collections.EMPTY_LIST);

    replay(m_dao);

    Set<Resource> results = provider.getResources(request, null);
    assertEquals(0, results.size());
  }

  /**
   * @throws Exception
   */
  @Test
  public void testGetResourcesClusterPredicate() throws Exception {
    Request request = PropertyHelper.getReadRequest(
        AlertNoticeResourceProvider.ALERT_NOTICE_CLUSTER_NAME,
        AlertNoticeResourceProvider.ALERT_NOTICE_ID,
        AlertNoticeResourceProvider.ALERT_NOTICE_HISTORY_ID,
        AlertNoticeResourceProvider.ALERT_NOTICE_SERVICE_NAME,
        AlertNoticeResourceProvider.ALERT_NOTICE_TARGET_ID,
        AlertNoticeResourceProvider.ALERT_NOTICE_TARGET_NAME,
        AlertNoticeResourceProvider.ALERT_NOTICE_STATE);

    AmbariManagementController amc = createMock(AmbariManagementController.class);

    Predicate predicate = new PredicateBuilder().property(
        AlertNoticeResourceProvider.ALERT_NOTICE_CLUSTER_NAME).equals("c1").toPredicate();

    expect(m_dao.findAllNotices(EasyMock.anyObject(AlertNoticeRequest.class))).andReturn(
        getMockEntities());

    replay(amc, m_dao);

    AlertNoticeResourceProvider provider = createProvider();
    Set<Resource> results = provider.getResources(request, predicate);

    assertEquals(1, results.size());

    Resource r = results.iterator().next();

    Assert.assertEquals(
        "Administrators",
        r.getPropertyValue(AlertNoticeResourceProvider.ALERT_NOTICE_TARGET_NAME));

    Assert.assertEquals(
        NotificationState.FAILED,
        r.getPropertyValue(AlertNoticeResourceProvider.ALERT_NOTICE_STATE));

    verify(amc, m_dao);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testGetSingleResource() throws Exception {
    Request request = PropertyHelper.getReadRequest(
        AlertNoticeResourceProvider.ALERT_NOTICE_CLUSTER_NAME,
        AlertNoticeResourceProvider.ALERT_NOTICE_ID,
        AlertNoticeResourceProvider.ALERT_NOTICE_HISTORY_ID,
        AlertNoticeResourceProvider.ALERT_NOTICE_SERVICE_NAME,
        AlertNoticeResourceProvider.ALERT_NOTICE_TARGET_ID,
        AlertNoticeResourceProvider.ALERT_NOTICE_TARGET_NAME,
        AlertNoticeResourceProvider.ALERT_NOTICE_STATE);

    Predicate predicate = new PredicateBuilder().property(
        AlertNoticeResourceProvider.ALERT_NOTICE_CLUSTER_NAME).equals("c1").and().property(
        AlertNoticeResourceProvider.ALERT_NOTICE_ID).equals("1").toPredicate();

    expect(m_dao.findAllNotices(EasyMock.anyObject(AlertNoticeRequest.class))).andReturn(
        getMockEntities());

    replay(m_dao);

    AlertNoticeResourceProvider provider = createProvider();
    Set<Resource> results = provider.getResources(request, predicate);

    assertEquals(1, results.size());

    Resource r = results.iterator().next();

    Assert.assertEquals(
        "Administrators",
        r.getPropertyValue(AlertNoticeResourceProvider.ALERT_NOTICE_TARGET_NAME));

    Assert.assertEquals(NotificationState.FAILED,
        r.getPropertyValue(AlertNoticeResourceProvider.ALERT_NOTICE_STATE));
  }

  /**
   * @param amc
   * @return
   */
  private AlertNoticeResourceProvider createProvider() {
    return new AlertNoticeResourceProvider();
  }

  /**
   * @return
   */
  private List<AlertNoticeEntity> getMockEntities() throws Exception {
    ClusterEntity cluster = new ClusterEntity();
    cluster.setClusterName("c1");
    cluster.setClusterId(1L);

    AlertDefinitionEntity definition = new AlertDefinitionEntity();
    definition.setClusterId(1L);
    definition.setComponentName("NAMENODE");
    definition.setDefinitionName("namenode_definition");
    definition.setEnabled(true);
    definition.setServiceName("HDFS");
    definition.setCluster(cluster);

    AlertHistoryEntity history = new AlertHistoryEntity();
    history.setAlertId(1L);
    history.setAlertDefinition(definition);
    history.setClusterId(Long.valueOf(1L));
    history.setComponentName(null);
    history.setAlertText("Mock Label");
    history.setServiceName("HDFS");
    history.setAlertState(AlertState.WARNING);
    history.setAlertTimestamp(System.currentTimeMillis());

    AlertTargetEntity administrators = new AlertTargetEntity();
    administrators.setDescription("The Administrators");
    administrators.setNotificationType("EMAIL");
    administrators.setTargetName("Administrators");

    AlertNoticeEntity entity = new AlertNoticeEntity();
    entity.setAlertHistory(history);
    entity.setAlertTarget(administrators);
    entity.setNotifyState(NotificationState.FAILED);
    entity.setUuid(UUID.randomUUID().toString());
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
