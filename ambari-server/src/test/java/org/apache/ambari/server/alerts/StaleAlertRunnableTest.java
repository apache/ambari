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

package org.apache.ambari.server.alerts;

import static junit.framework.Assert.assertEquals;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.apache.ambari.server.events.AlertEvent;
import org.apache.ambari.server.events.AlertReceivedEvent;
import org.apache.ambari.server.events.MockEventListener;
import org.apache.ambari.server.events.publishers.AlertEventPublisher;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.orm.entities.AlertCurrentEntity;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.state.Alert;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.stack.OsFamily;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.eventbus.EventBus;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

/**
 * Tests {@link StaleAlertRunnableTest}.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ StaleAlertRunnable.class, ManagementFactory.class })
public class StaleAlertRunnableTest {

  private final static long CLUSTER_ID = 1;
  private final static String CLUSTER_NAME = "c1";

  private final static String DEFINITION_NAME = "ambari_server_stale_alerts";
  private final static String DEFINITION_SERVICE = "AMBARI";
  private final static String DEFINITION_COMPONENT = "AMBARI_SERVER";
  private final static String DEFINITION_LABEL = "Mock Definition";
  private final static int DEFINITION_INTERVAL = 1;

  private Clusters m_clusters;
  private Cluster m_cluster;
  private Injector m_injector;
  private AlertsDAO m_alertsDao;
  private AlertDefinitionDAO m_definitionDao;
  private AlertDefinitionEntity m_definition;
  private List<AlertCurrentEntity> m_currentAlerts = new ArrayList<AlertCurrentEntity>();
  private MockEventListener m_listener;

  private AlertEventPublisher m_eventPublisher;
  private EventBus m_synchronizedBus;
  private RuntimeMXBean m_runtimeMXBean;

  /**
   *
   */
  @Before
  public void setup() throws Exception {
    m_injector = Guice.createInjector(new MockModule());
    m_alertsDao = m_injector.getInstance(AlertsDAO.class);
    m_definitionDao = m_injector.getInstance(AlertDefinitionDAO.class);
    m_clusters = m_injector.getInstance(Clusters.class);
    m_cluster = m_injector.getInstance(Cluster.class);
    m_eventPublisher = m_injector.getInstance(AlertEventPublisher.class);
    m_listener = m_injector.getInstance(MockEventListener.class);
    m_definition = EasyMock.createNiceMock(AlertDefinitionEntity.class);

    // !!! need a synchronous op for testing
    m_synchronizedBus = new EventBus();
    Field field = AlertEventPublisher.class.getDeclaredField("m_eventBus");
    field.setAccessible(true);
    field.set(m_eventPublisher, m_synchronizedBus);

    // register mock listener
    m_synchronizedBus.register(m_listener);

    // create the cluster map
    Map<String,Cluster> clusterMap = new HashMap<String, Cluster>();
    clusterMap.put(CLUSTER_NAME, m_cluster);

    // mock the definition for the alert
    expect(m_definition.getDefinitionId()).andReturn(1L).atLeastOnce();
    expect(m_definition.getDefinitionName()).andReturn(DEFINITION_NAME).atLeastOnce();
    expect(m_definition.getServiceName()).andReturn(DEFINITION_SERVICE).atLeastOnce();
    expect(m_definition.getComponentName()).andReturn(DEFINITION_COMPONENT).atLeastOnce();
    expect(m_definition.getLabel()).andReturn(DEFINITION_LABEL).atLeastOnce();
    expect(m_definition.getEnabled()).andReturn(true).atLeastOnce();
    expect(m_definition.getScheduleInterval()).andReturn(DEFINITION_INTERVAL).atLeastOnce();
    expect(m_definition.getClusterId()).andReturn(CLUSTER_ID).atLeastOnce();

    expect(m_definition.getSource()).andReturn("{\"type\" : \"SERVER\"}").anyTimes();

    // mock the cluster
    expect(m_cluster.getClusterId()).andReturn(CLUSTER_ID).atLeastOnce();

    // mock clusters
    expect(m_clusters.getClusters()).andReturn(clusterMap).atLeastOnce();

    // mock the definition DAO
    expect(m_definitionDao.findByName(CLUSTER_ID, DEFINITION_NAME)).andReturn(
        m_definition).atLeastOnce();

    // mock the current dao
    expect(m_alertsDao.findCurrentByCluster(CLUSTER_ID)).andReturn(
        m_currentAlerts).atLeastOnce();

    // mock out the uptime to be a while (since most tests are not testing
    // system uptime)
    m_runtimeMXBean = EasyMock.createNiceMock(RuntimeMXBean.class);
    PowerMock.mockStatic(ManagementFactory.class);
    expect(ManagementFactory.getRuntimeMXBean()).andReturn(m_runtimeMXBean).atLeastOnce();
    PowerMock.replay(ManagementFactory.class);
    expect(m_runtimeMXBean.getUptime()).andReturn(360000L);

    replay(m_definition, m_cluster, m_clusters,
        m_definitionDao, m_alertsDao, m_runtimeMXBean);
    }

  /**
   * @throws Exception
   */
  @After
  public void teardown() throws Exception {
  }

  /**
   * Tests that the event is triggerd with a status of OK.
   */
  @Test
  public void testAllAlertsAreCurrent() {
    // create current alerts that are not stale
    AlertDefinitionEntity definition = new AlertDefinitionEntity();
    definition.setClusterId(CLUSTER_ID);
    definition.setDefinitionName("foo-definition");
    definition.setServiceName("HDFS");
    definition.setComponentName("NAMENODE");
    definition.setEnabled(true);
    definition.setScheduleInterval(1);

    AlertCurrentEntity current1 = createNiceMock(AlertCurrentEntity.class);
    AlertHistoryEntity history1 = createNiceMock(AlertHistoryEntity.class);

    expect(current1.getAlertHistory()).andReturn(history1).atLeastOnce();
    expect(history1.getAlertDefinition()).andReturn(definition).atLeastOnce();

    expect(current1.getMaintenanceState()).andReturn(MaintenanceState.OFF).atLeastOnce();
    expect(current1.getLatestTimestamp()).andReturn(System.currentTimeMillis()).atLeastOnce();

    replay(current1, history1);

    m_currentAlerts.add(current1);

    // precondition that no events were fired
    assertEquals(0,
        m_listener.getAlertEventReceivedCount(AlertReceivedEvent.class));

    // instantiate and inject mocks
    StaleAlertRunnable runnable = new StaleAlertRunnable(m_definition.getDefinitionName());
    m_injector.injectMembers(runnable);

    // run the alert
    runnable.run();

    assertEquals(1,
        m_listener.getAlertEventReceivedCount(AlertReceivedEvent.class));

    List<AlertEvent> events = m_listener.getAlertEventInstances(AlertReceivedEvent.class);
    assertEquals(1, events.size());

    AlertReceivedEvent event = (AlertReceivedEvent) events.get(0);
    Alert alert = event.getAlert();
    assertEquals("AMBARI", alert.getService());
    assertEquals("AMBARI_SERVER", alert.getComponent());
    assertEquals(AlertState.OK, alert.getState());
    assertEquals(DEFINITION_NAME, alert.getName());

    verify(m_cluster, m_clusters, m_definitionDao);
  }

  /**
   * Tests that a stale alert triggers the event with a status of CRITICAL.
   */
  @Test
  public void testStaleAlert() {
    // create current alerts that are not stale
    AlertDefinitionEntity definition = new AlertDefinitionEntity();
    definition.setClusterId(CLUSTER_ID);
    definition.setDefinitionName("foo-definition");
    definition.setServiceName("HDFS");
    definition.setComponentName("NAMENODE");
    definition.setEnabled(true);
    definition.setScheduleInterval(1);

    // create current alerts that are stale
    AlertCurrentEntity current1 = createNiceMock(AlertCurrentEntity.class);
    AlertHistoryEntity history1 = createNiceMock(AlertHistoryEntity.class);

    expect(current1.getAlertHistory()).andReturn(history1).atLeastOnce();
    expect(history1.getAlertDefinition()).andReturn(definition).atLeastOnce();

    // a really old timestampt to trigger the alert
    expect(current1.getMaintenanceState()).andReturn(MaintenanceState.OFF).atLeastOnce();
    expect(current1.getLatestTimestamp()).andReturn(1L).atLeastOnce();

    replay(current1, history1);

    m_currentAlerts.add(current1);

    // precondition that no events were fired
    assertEquals(0,
        m_listener.getAlertEventReceivedCount(AlertReceivedEvent.class));

    // instantiate and inject mocks
    StaleAlertRunnable runnable = new StaleAlertRunnable(m_definition.getDefinitionName());
    m_injector.injectMembers(runnable);

    // run the alert
    runnable.run();

    assertEquals(1,
        m_listener.getAlertEventReceivedCount(AlertReceivedEvent.class));

    List<AlertEvent> events = m_listener.getAlertEventInstances(AlertReceivedEvent.class);
    assertEquals(1, events.size());

    AlertReceivedEvent event = (AlertReceivedEvent) events.get(0);
    Alert alert = event.getAlert();
    assertEquals("AMBARI", alert.getService());
    assertEquals("AMBARI_SERVER", alert.getComponent());
    assertEquals(AlertState.CRITICAL, alert.getState());
    assertEquals(DEFINITION_NAME, alert.getName());

    verify(m_cluster, m_clusters, m_definitionDao);
  }

  /**
   * Tests that a stale alert in maintenance mode doesn't trigger the event.
   */
  @Test
  public void testStaleAlertInMaintenaceMode() {
    // create current alerts that are stale
    AlertDefinitionEntity definition = new AlertDefinitionEntity();
    definition.setClusterId(CLUSTER_ID);
    definition.setDefinitionName("foo-definition");
    definition.setServiceName("HDFS");
    definition.setComponentName("NAMENODE");
    definition.setEnabled(true);
    definition.setScheduleInterval(1);

    // create current alerts where 1 is stale but in maintence mode
    AlertCurrentEntity current1 = createNiceMock(AlertCurrentEntity.class);
    AlertHistoryEntity history1 = createNiceMock(AlertHistoryEntity.class);
    AlertCurrentEntity current2 = createNiceMock(AlertCurrentEntity.class);
    AlertHistoryEntity history2 = createNiceMock(AlertHistoryEntity.class);

    expect(current1.getAlertHistory()).andReturn(history1).atLeastOnce();
    expect(history1.getAlertDefinition()).andReturn(definition).atLeastOnce();

    expect(current2.getAlertHistory()).andReturn(history2).atLeastOnce();
    expect(history2.getAlertDefinition()).andReturn(definition).atLeastOnce();

    // maintenance mode with a really old timestamp
    expect(current1.getMaintenanceState()).andReturn(MaintenanceState.ON).atLeastOnce();
    expect(current1.getLatestTimestamp()).andReturn(1L).atLeastOnce();

    // an that that is not stale
    expect(current2.getMaintenanceState()).andReturn(MaintenanceState.OFF).atLeastOnce();
    expect(current2.getLatestTimestamp()).andReturn(System.currentTimeMillis()).atLeastOnce();

    replay(current1, history1, current2, history2);

    m_currentAlerts.add(current1);
    m_currentAlerts.add(current2);

    // precondition that no events were fired
    assertEquals(0,
        m_listener.getAlertEventReceivedCount(AlertReceivedEvent.class));

    // instantiate and inject mocks
    StaleAlertRunnable runnable = new StaleAlertRunnable(m_definition.getDefinitionName());
    m_injector.injectMembers(runnable);

    // run the alert
    runnable.run();

    assertEquals(1,
        m_listener.getAlertEventReceivedCount(AlertReceivedEvent.class));

    List<AlertEvent> events = m_listener.getAlertEventInstances(AlertReceivedEvent.class);
    assertEquals(1, events.size());

    AlertReceivedEvent event = (AlertReceivedEvent) events.get(0);
    Alert alert = event.getAlert();
    assertEquals("AMBARI", alert.getService());
    assertEquals("AMBARI_SERVER", alert.getComponent());
    assertEquals(AlertState.OK, alert.getState());
    assertEquals(DEFINITION_NAME, alert.getName());

    verify(m_cluster, m_clusters, m_definitionDao);
  }

  /**
   * Tests that stale alerts are not reported if the server has not be running
   * long enough.
   */
  @Test
  public void testStaleAlertWithServerUptime() {
    // reset the Runtime MX bean to a low value
    reset(m_runtimeMXBean);
    expect(m_runtimeMXBean.getUptime()).andReturn(1000L);
    replay(m_runtimeMXBean);

    // create current alerts that are stale (5 minute interval)
    AlertDefinitionEntity definition = new AlertDefinitionEntity();
    definition.setClusterId(CLUSTER_ID);
    definition.setDefinitionName("foo-definition");
    definition.setServiceName("HDFS");
    definition.setComponentName("NAMENODE");
    definition.setEnabled(true);
    definition.setScheduleInterval(5);

    // create current alerts that are stale
    AlertCurrentEntity current1 = createNiceMock(AlertCurrentEntity.class);
    AlertHistoryEntity history1 = createNiceMock(AlertHistoryEntity.class);

    expect(current1.getAlertHistory()).andReturn(history1).atLeastOnce();
    expect(history1.getAlertDefinition()).andReturn(definition).atLeastOnce();

    // use a timestamp that would trigger the alert, say 3x the interval ago (so
    // 15 minutes ago)
    long now = System.currentTimeMillis();
    long staleTime = now - (definition.getScheduleInterval() * 60 * 1000 * 3);

    expect(current1.getMaintenanceState()).andReturn(MaintenanceState.OFF).atLeastOnce();
    expect(current1.getLatestTimestamp()).andReturn(staleTime).atLeastOnce();

    replay(current1, history1);

    m_currentAlerts.add(current1);

    // precondition that no events were fired
    assertEquals(0, m_listener.getAlertEventReceivedCount(AlertReceivedEvent.class));

    // instantiate and inject mocks
    StaleAlertRunnable runnable = new StaleAlertRunnable(m_definition.getDefinitionName());
    m_injector.injectMembers(runnable);

    // run the alert
    runnable.run();

    // ensure that our mock MX bean was used
    verify(m_runtimeMXBean);

    // verify that our uptime was too short so nothing should have been
    // triggered
    assertEquals(1, m_listener.getAlertEventReceivedCount(AlertReceivedEvent.class));
    List<AlertEvent> events = m_listener.getAlertEventInstances(AlertReceivedEvent.class);
    assertEquals(1, events.size());

    AlertReceivedEvent event = (AlertReceivedEvent) events.get(0);
    Alert alert = event.getAlert();
    assertEquals("AMBARI", alert.getService());
    assertEquals("AMBARI_SERVER", alert.getComponent());
    assertEquals(AlertState.OK, alert.getState());
    assertEquals(DEFINITION_NAME, alert.getName());

    // now reset the mocks to indicate that Ambari has been up long enough
    m_listener.reset();
    long uptime = definition.getScheduleInterval() * 60 * 1000 * 4;
    reset(m_runtimeMXBean);
    expect(m_runtimeMXBean.getUptime()).andReturn(uptime);
    replay(m_runtimeMXBean);

    // run the alert again and verify that the same stale alert caused a
    // CRITICAL
    runnable.run();

    // recheck for the stale alert
    events = m_listener.getAlertEventInstances(AlertReceivedEvent.class);
    assertEquals(1, events.size());

    event = (AlertReceivedEvent) events.get(0);
    alert = event.getAlert();
    assertEquals("AMBARI", alert.getService());
    assertEquals("AMBARI_SERVER", alert.getComponent());
    assertEquals(AlertState.CRITICAL, alert.getState());
    assertEquals(DEFINITION_NAME, alert.getName());

    assertEquals(1, m_listener.getAlertEventReceivedCount(AlertReceivedEvent.class));
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
      Cluster cluster = EasyMock.createNiceMock(Cluster.class);

      binder.bind(Clusters.class).toInstance(createNiceMock(Clusters.class));
      binder.bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
      binder.bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
      binder.bind(Cluster.class).toInstance(cluster);
      binder.bind(AlertDefinitionDAO.class).toInstance(createNiceMock(AlertDefinitionDAO.class));
      binder.bind(AlertsDAO.class).toInstance(createNiceMock(AlertsDAO.class));
      binder.bind(EntityManager.class).toInstance(createNiceMock(EntityManager.class));
    }
  }
}
