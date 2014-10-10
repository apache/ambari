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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.ambari.server.controller.AlertHistoryRequest;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
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
 * {@link AlertHistoryResourceProvider} tests.
 */
public class AlertHistoryResourceProviderTest {

  private AlertsDAO m_dao = null;
  private Injector m_injector;

  @Before
  public void before() {
    m_dao = createStrictMock(AlertsDAO.class);

    m_injector = Guice.createInjector(Modules.override(
        new InMemoryDefaultTestModule()).with(new MockModule()));

    AlertHistoryResourceProvider.init(m_injector);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testGetResourcesNoPredicate() throws Exception {
    AlertHistoryResourceProvider provider = createProvider(null);

    Request request = PropertyHelper.getReadRequest(
        "AlertHistory/cluster_name", "AlertHistory/id");

    Set<Resource> results = provider.getResources(request, null);
    assertEquals(0, results.size());
  }

  /**
   * @throws Exception
   */
  @Test
  public void testGetResourcesClusterPredicate() throws Exception {
    Request request = PropertyHelper.getReadRequest(
        AlertHistoryResourceProvider.ALERT_HISTORY_CLUSTER_NAME,
        AlertHistoryResourceProvider.ALERT_HISTORY_DEFINITION_ID,
        AlertHistoryResourceProvider.ALERT_HISTORY_DEFINITION_NAME,
        AlertHistoryResourceProvider.ALERT_HISTORY_COMPONENT_NAME,
        AlertHistoryResourceProvider.ALERT_HISTORY_HOSTNAME,
        AlertHistoryResourceProvider.ALERT_HISTORY_STATE);

    AmbariManagementController amc = createMock(AmbariManagementController.class);
    Clusters clusters = createMock(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    expect(amc.getClusters()).andReturn(clusters).atLeastOnce();
    expect(clusters.getCluster((String) anyObject())).andReturn(cluster).atLeastOnce();
    expect(cluster.getClusterId()).andReturn(Long.valueOf(1)).anyTimes();
    expect(cluster.getClusterName()).andReturn("c1").atLeastOnce();

    Predicate predicate = new PredicateBuilder().property(
        AlertHistoryResourceProvider.ALERT_HISTORY_CLUSTER_NAME).equals("c1").toPredicate();

    expect(m_dao.findAll(EasyMock.anyObject(AlertHistoryRequest.class))).andReturn(
        getMockEntities());

    replay(amc, clusters, cluster, m_dao);

    AlertHistoryResourceProvider provider = createProvider(amc);
    Set<Resource> results = provider.getResources(request, predicate);

    assertEquals(1, results.size());

    Resource r = results.iterator().next();

    Assert.assertEquals(
        "namenode_definition",
        r.getPropertyValue(AlertHistoryResourceProvider.ALERT_HISTORY_DEFINITION_NAME));

    Assert.assertEquals(AlertState.WARNING,
        r.getPropertyValue(AlertHistoryResourceProvider.ALERT_HISTORY_STATE));

    verify(amc, clusters, cluster, m_dao);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testGetSingleResource() throws Exception {
    Request request = PropertyHelper.getReadRequest(
        AlertHistoryResourceProvider.ALERT_HISTORY_CLUSTER_NAME,
        AlertHistoryResourceProvider.ALERT_HISTORY_DEFINITION_ID,
        AlertHistoryResourceProvider.ALERT_HISTORY_DEFINITION_NAME,
        AlertHistoryResourceProvider.ALERT_HISTORY_COMPONENT_NAME,
        AlertHistoryResourceProvider.ALERT_HISTORY_HOSTNAME,
        AlertHistoryResourceProvider.ALERT_HISTORY_STATE);

    AmbariManagementController amc = createMock(AmbariManagementController.class);
    Clusters clusters = createMock(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    expect(amc.getClusters()).andReturn(clusters).atLeastOnce();
    expect(clusters.getCluster((String) anyObject())).andReturn(cluster).atLeastOnce();
    expect(cluster.getClusterId()).andReturn(Long.valueOf(1)).anyTimes();

    Predicate predicate = new PredicateBuilder().property(
        AlertHistoryResourceProvider.ALERT_HISTORY_CLUSTER_NAME).equals("c1").and().property(
        AlertHistoryResourceProvider.ALERT_HISTORY_ID).equals("1").toPredicate();

    expect(m_dao.findById(1L)).andReturn(getMockEntities().get(0));

    replay(amc, clusters, cluster, m_dao);

    AlertHistoryResourceProvider provider = createProvider(amc);
    Set<Resource> results = provider.getResources(request, predicate);

    assertEquals(1, results.size());

    Resource r = results.iterator().next();

    Assert.assertEquals(
        "namenode_definition",
        r.getPropertyValue(AlertHistoryResourceProvider.ALERT_HISTORY_DEFINITION_NAME));

    Assert.assertEquals(AlertState.WARNING,
        r.getPropertyValue(AlertHistoryResourceProvider.ALERT_HISTORY_STATE));
  }

  /**
   * @param amc
   * @return
   */
  private AlertHistoryResourceProvider createProvider(
      AmbariManagementController amc) {
    return new AlertHistoryResourceProvider(amc);
  }

  /**
   * @return
   */
  private List<AlertHistoryEntity> getMockEntities() throws Exception {
    AlertDefinitionEntity definition = new AlertDefinitionEntity();
    definition.setClusterId(1L);
    definition.setComponentName("NAMENODE");
    definition.setDefinitionName("namenode_definition");
    definition.setEnabled(true);
    definition.setServiceName("HDFS");

    AlertHistoryEntity entity = new AlertHistoryEntity();
    entity.setAlertId(1L);
    entity.setAlertDefinition(definition);
    entity.setClusterId(Long.valueOf(1L));
    entity.setComponentName(null);
    entity.setAlertText("Mock Label");
    entity.setServiceName("HDFS");
    entity.setAlertState(AlertState.WARNING);
    entity.setAlertTimestamp(System.currentTimeMillis());
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
      binder.bind(AlertsDAO.class).toInstance(m_dao);
      binder.bind(Clusters.class).toInstance(
          EasyMock.createNiceMock(Clusters.class));
      binder.bind(Cluster.class).toInstance(
          EasyMock.createNiceMock(Cluster.class));
      binder.bind(ActionMetadata.class);
    }
  }
}
