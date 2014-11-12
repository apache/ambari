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

import static org.apache.ambari.server.configuration.Configuration.JDBC_IN_MEMORY_URL;
import static org.apache.ambari.server.configuration.Configuration.JDBC_IN_MEMROY_DRIVER;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.captureLong;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.persistence.EntityManager;

import org.apache.ambari.server.api.query.render.AlertSummaryGroupedRenderer;
import org.apache.ambari.server.api.query.render.AlertSummaryRenderer;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultImpl;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.DBAccessorImpl;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.orm.entities.AlertCurrentEntity;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
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
 * Test the AlertResourceProvider class
 */
public class AlertResourceProviderTest {

  private static final Long ALERT_VALUE_ID = Long.valueOf(1000L);
  private static final String ALERT_VALUE_LABEL = "My Label";
  private static final Long ALERT_VALUE_TIMESTAMP = Long.valueOf(1L);
  private static final String ALERT_VALUE_TEXT = "My Text";
  private static final String ALERT_VALUE_COMPONENT = "component";
  private static final String ALERT_VALUE_HOSTNAME = "host";
  private static final String ALERT_VALUE_SERVICE = "service";

  private AlertsDAO m_dao;
  private Injector m_injector;
  private AmbariManagementController m_amc;

  @Before
  @SuppressWarnings("boxing")
  public void before() throws Exception {
    m_dao = createStrictMock(AlertsDAO.class);

    m_injector = Guice.createInjector(Modules.override(
        new InMemoryDefaultTestModule()).with(new MockModule()));

    m_amc = m_injector.getInstance(AmbariManagementController.class);

    Cluster cluster = EasyMock.createMock(Cluster.class);
    Clusters clusters = m_injector.getInstance(Clusters.class);

    expect(m_amc.getClusters()).andReturn(clusters).atLeastOnce();
    expect(clusters.getCluster(capture(new Capture<String>()))).andReturn(cluster).atLeastOnce();
    expect(cluster.getClusterId()).andReturn(Long.valueOf(1L));

    replay(m_amc, clusters, cluster);
  }


  /**
   * @throws Exception
   */
  @Test
  public void testGetCluster() throws Exception {
    expect(m_dao.findCurrentByCluster(
        captureLong(new Capture<Long>()))).andReturn(getClusterMockEntities()).anyTimes();

    replay(m_dao);

    Request request = PropertyHelper.getReadRequest(
        AlertResourceProvider.ALERT_ID,
        AlertResourceProvider.ALERT_DEFINITION_NAME,
        AlertResourceProvider.ALERT_LABEL);

    Predicate predicate = new PredicateBuilder().property(
        AlertResourceProvider.ALERT_CLUSTER_NAME).equals("c1").toPredicate();

    AlertResourceProvider provider = createProvider();
    Set<Resource> results = provider.getResources(request, predicate);

    assertEquals(1, results.size());

    Resource r = results.iterator().next();
    assertEquals("c1", r.getPropertyValue(AlertResourceProvider.ALERT_CLUSTER_NAME));

    verify(m_dao);
  }

  /**
   * Test for service
   */
  @Test
  public void testGetService() throws Exception {
    expect(m_dao.findCurrentByService(captureLong(new Capture<Long>()),
        capture(new Capture<String>()))).andReturn(getClusterMockEntities()).anyTimes();

    replay(m_dao);

    Request request = PropertyHelper.getReadRequest(
        AlertResourceProvider.ALERT_ID,
        AlertResourceProvider.ALERT_DEFINITION_NAME,
        AlertResourceProvider.ALERT_LABEL);

    Predicate predicate = new PredicateBuilder().property(
        AlertResourceProvider.ALERT_CLUSTER_NAME).equals("c1").and()
        .property(AlertResourceProvider.ALERT_SERVICE).equals(ALERT_VALUE_SERVICE).toPredicate();

    AlertResourceProvider provider = createProvider();
    Set<Resource> results = provider.getResources(request, predicate);

    assertEquals(1, results.size());

    Resource r = results.iterator().next();
    assertEquals("c1", r.getPropertyValue(AlertResourceProvider.ALERT_CLUSTER_NAME));
    assertEquals(ALERT_VALUE_SERVICE, r.getPropertyValue(AlertResourceProvider.ALERT_SERVICE));

    verify(m_dao);
  }

  /**
   * Test for service
   */
  @Test
  public void testGetHost() throws Exception {
    expect(m_dao.findCurrentByHost(captureLong(new Capture<Long>()),
        capture(new Capture<String>()))).andReturn(getClusterMockEntities()).anyTimes();

    replay(m_dao);

    Request request = PropertyHelper.getReadRequest(
        AlertResourceProvider.ALERT_ID,
        AlertResourceProvider.ALERT_DEFINITION_NAME,
        AlertResourceProvider.ALERT_LABEL);

    Predicate predicate = new PredicateBuilder().property(
        AlertResourceProvider.ALERT_CLUSTER_NAME).equals("c1").and()
        .property(AlertResourceProvider.ALERT_HOST).equals(ALERT_VALUE_HOSTNAME).toPredicate();

    AlertResourceProvider provider = createProvider();
    Set<Resource> results = provider.getResources(request, predicate);

    assertEquals(1, results.size());

    Resource r = results.iterator().next();
    assertEquals("c1", r.getPropertyValue(AlertResourceProvider.ALERT_CLUSTER_NAME));
    assertEquals(ALERT_VALUE_HOSTNAME, r.getPropertyValue(AlertResourceProvider.ALERT_HOST));

    verify(m_dao);
  }

  /**
   * Tests that the {@link AlertSummaryRenderer} correctly transforms the alert
   * data.
   *
   * @throws Exception
   */
  @Test
  public void testGetClusterSummary() throws Exception {
    expect(m_dao.findCurrentByCluster(captureLong(new Capture<Long>()))).andReturn(
        getMockEntitiesManyStates()).anyTimes();

    replay(m_dao);

    Request request = PropertyHelper.getReadRequest(
        AlertResourceProvider.ALERT_ID, AlertResourceProvider.ALERT_DEFINITION_NAME,
        AlertResourceProvider.ALERT_LABEL, AlertResourceProvider.ALERT_STATE,
        AlertResourceProvider.ALERT_ORIGINAL_TIMESTAMP);

    Predicate predicate = new PredicateBuilder().property(
        AlertResourceProvider.ALERT_CLUSTER_NAME).equals("c1").toPredicate();

    AlertResourceProvider provider = createProvider();
    Set<Resource> results = provider.getResources(request, predicate);

    verify(m_dao);

    AlertSummaryRenderer renderer = new AlertSummaryRenderer();
    ResultImpl result = new ResultImpl(true);
    TreeNode<Resource> resources = result.getResultTree();

    AtomicInteger alertResourceId = new AtomicInteger(1);
    for (Resource resource : results) {
      resources.addChild(resource, "Alert " + alertResourceId.getAndIncrement());
    }

    Result summary = renderer.finalizeResult(result);
    Assert.assertNotNull(summary);

    // pull out the alerts_summary child set by the renderer
    TreeNode<Resource> summaryResultTree = summary.getResultTree();
    TreeNode<Resource> summaryResources = summaryResultTree.getChild("alerts_summary");

    Resource summaryResource = summaryResources.getObject();

    Integer okCount = (Integer) summaryResource.getPropertyValue("alerts_summary/OK/count");
    Integer warningCount = (Integer) summaryResource.getPropertyValue("alerts_summary/WARNING/count");
    Integer criticalCount = (Integer) summaryResource.getPropertyValue("alerts_summary/CRITICAL/count");
    Integer unknownCount = (Integer) summaryResource.getPropertyValue("alerts_summary/UNKNOWN/count");

    Assert.assertEquals(10, okCount.intValue());
    Assert.assertEquals(2, warningCount.intValue());
    Assert.assertEquals(1, criticalCount.intValue());
    Assert.assertEquals(3, unknownCount.intValue());
  }

  /**
   * Tests that the {@link AlertSummaryGroupedRenderer} correctly transforms the
   * alert data.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("unchecked")
  public void testGetClusterGroupedSummary() throws Exception {
    expect(m_dao.findCurrentByCluster(captureLong(new Capture<Long>()))).andReturn(
        getMockEntitiesManyStates()).anyTimes();

    replay(m_dao);

    Request request = PropertyHelper.getReadRequest(
        AlertResourceProvider.ALERT_ID, AlertResourceProvider.ALERT_DEFINITION_NAME,
        AlertResourceProvider.ALERT_LABEL, AlertResourceProvider.ALERT_STATE,
        AlertResourceProvider.ALERT_ORIGINAL_TIMESTAMP);

    Predicate predicate = new PredicateBuilder().property(
        AlertResourceProvider.ALERT_CLUSTER_NAME).equals("c1").toPredicate();

    AlertResourceProvider provider = createProvider();
    Set<Resource> results = provider.getResources(request, predicate);

    verify(m_dao);

    AlertSummaryGroupedRenderer renderer = new AlertSummaryGroupedRenderer();
    ResultImpl result = new ResultImpl(true);
    TreeNode<Resource> resources = result.getResultTree();

    AtomicInteger alertResourceId = new AtomicInteger(1);
    for (Resource resource : results) {
      resources.addChild(resource, "Alert " + alertResourceId.getAndIncrement());
    }

    Result groupedSummary = renderer.finalizeResult(result);
    Assert.assertNotNull(groupedSummary);

    // pull out the alerts_summary child set by the renderer
    TreeNode<Resource> summaryResultTree = groupedSummary.getResultTree();
    TreeNode<Resource> summaryResources = summaryResultTree.getChild("alerts_summary_grouped");

    Resource summaryResource = summaryResources.getObject();
    List<Object> summaryList = (List<Object>) summaryResource.getPropertyValue("alerts_summary_grouped");
    Assert.assertEquals(4, summaryList.size());
  }

  private AlertResourceProvider createProvider() {
    return new AlertResourceProvider(m_amc);
  }

  /**
   * @return
   */
  private List<AlertCurrentEntity> getClusterMockEntities() throws Exception {
    AlertCurrentEntity current = new AlertCurrentEntity();
    current.setAlertId(Long.valueOf(1000L));
    current.setLatestTimestamp(Long.valueOf(1L));
    current.setOriginalTimestamp(Long.valueOf(2L));

    AlertHistoryEntity history = new AlertHistoryEntity();
    history.setAlertId(ALERT_VALUE_ID);
    history.setAlertInstance(null);
    history.setAlertLabel(ALERT_VALUE_LABEL);
    history.setAlertState(AlertState.OK);
    history.setAlertText(ALERT_VALUE_TEXT);
    history.setAlertTimestamp(ALERT_VALUE_TIMESTAMP);
    history.setClusterId(Long.valueOf(1L));
    history.setComponentName(ALERT_VALUE_COMPONENT);
    history.setHostName(ALERT_VALUE_HOSTNAME);
    history.setServiceName(ALERT_VALUE_SERVICE);

    AlertDefinitionEntity definition = new AlertDefinitionEntity();

    history.setAlertDefinition(definition);
    current.setAlertHistory(history);

    return Arrays.asList(current);
  }

  /**
   * Gets a bunch of alerts with various values for state and timestamp.
   *
   * @return
   */
  private List<AlertCurrentEntity> getMockEntitiesManyStates() throws Exception {
    // yesterday
    AtomicLong timestamp = new AtomicLong(System.currentTimeMillis() - 86400000);
    AtomicLong alertId = new AtomicLong(1);

    int ok = 10;
    int warning = 2;
    int critical = 1;
    int unknown = 3;
    int total = ok + warning + critical + unknown;

    List<AlertCurrentEntity> currents = new ArrayList<AlertCurrentEntity>(total);

    for (int i = 0; i < total; i++) {
      AlertState state = AlertState.OK;
      String service = "HDFS";
      String component = "NAMENODE";
      String definitionName = "hdfs_namenode";

      if (i >= ok && i < ok + warning) {
        state = AlertState.WARNING;
        service = "YARN";
        component = "RESOURCEMANAGER";
        definitionName = "yarn_resourcemanager";
      } else if (i >= ok + warning & i < ok + warning + critical) {
        state = AlertState.CRITICAL;
        service = "HIVE";
        component = "HIVE_SERVER";
        definitionName = "hive_server";
      } else if (i >= ok + warning + critical) {
        state = AlertState.UNKNOWN;
        service = "FLUME";
        component = "FLUME_HANDLER";
        definitionName = "flume_handler";
      }

      AlertCurrentEntity current = new AlertCurrentEntity();
      current.setAlertId(alertId.getAndIncrement());
      current.setOriginalTimestamp(timestamp.getAndAdd(10000));
      current.setLatestTimestamp(timestamp.getAndAdd(10000));

      AlertHistoryEntity history = new AlertHistoryEntity();
      history.setAlertId(alertId.getAndIncrement());
      history.setAlertInstance(null);
      history.setAlertLabel(ALERT_VALUE_LABEL);
      history.setAlertState(state);
      history.setAlertText(ALERT_VALUE_TEXT);
      history.setAlertTimestamp(current.getOriginalTimestamp());
      history.setClusterId(Long.valueOf(1L));
      history.setComponentName(component);
      history.setHostName(ALERT_VALUE_HOSTNAME);
      history.setServiceName(service);

      AlertDefinitionEntity definition = new AlertDefinitionEntity();
      definition.setDefinitionId(Long.valueOf(i));
      definition.setDefinitionName(definitionName);
      history.setAlertDefinition(definition);
      current.setAlertHistory(history);
      currents.add(current);
    }

    return currents;
  }


  /**
  *
  */
  private class MockModule implements Module {
    @Override
    public void configure(Binder binder) {
      binder.bind(EntityManager.class).toInstance(EasyMock.createMock(EntityManager.class));
      binder.bind(AlertsDAO.class).toInstance(m_dao);
      binder.bind(AmbariManagementController.class).toInstance(createMock(AmbariManagementController.class));
      binder.bind(DBAccessor.class).to(DBAccessorImpl.class);

      Clusters clusters = EasyMock.createNiceMock(Clusters.class);
      Configuration configuration = EasyMock.createMock(Configuration.class);

      binder.bind(Clusters.class).toInstance(clusters);
      binder.bind(Configuration.class).toInstance(configuration);

      expect(configuration.getDatabaseUrl()).andReturn(JDBC_IN_MEMORY_URL).anyTimes();
      expect(configuration.getDatabaseDriver()).andReturn(JDBC_IN_MEMROY_DRIVER).anyTimes();
      expect(configuration.getDatabaseUser()).andReturn("test").anyTimes();
      expect(configuration.getDatabasePassword()).andReturn("test").anyTimes();
      replay(configuration);
    }
  }
}
