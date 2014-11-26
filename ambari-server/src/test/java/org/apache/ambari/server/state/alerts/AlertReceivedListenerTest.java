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
package org.apache.ambari.server.state.alerts;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.UUID;

import org.apache.ambari.server.events.AlertReceivedEvent;
import org.apache.ambari.server.events.listeners.alerts.AlertReceivedListener;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.orm.entities.AlertCurrentEntity;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.state.Alert;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ServiceComponentFactory;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.alert.Scope;
import org.apache.ambari.server.state.alert.SourceType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

/**
 * Tests the {@link AlertReceivedListener}.
 */
public class AlertReceivedListenerTest {

  private static final String ALERT_DEFINITION = "alert_definition_";
  private static final String HOST1 = "h1";
  private static final String ALERT_LABEL = "My Label";
  private Injector m_injector;
  private AlertsDAO m_dao;
  private AlertDefinitionDAO m_definitionDao;

  private Clusters m_clusters;
  private Cluster m_cluster;

  private OrmTestHelper m_helper;
  private ServiceFactory m_serviceFactory;
  private ServiceComponentFactory m_componentFactory;
  private ServiceComponentHostFactory m_schFactory;

  @Before
  public void setup() throws Exception {
    m_injector = Guice.createInjector(new InMemoryDefaultTestModule());
    m_injector.getInstance(GuiceJpaInitializer.class);

    m_helper = m_injector.getInstance(OrmTestHelper.class);
    m_clusters = m_injector.getInstance(Clusters.class);
    m_serviceFactory = m_injector.getInstance(ServiceFactory.class);
    m_componentFactory = m_injector.getInstance(ServiceComponentFactory.class);
    m_schFactory = m_injector.getInstance(ServiceComponentHostFactory.class);

    // install YARN so there is at least 1 service installed and no
    // unexpected alerts since the test YARN service doesn't have any alerts
    m_cluster = m_helper.buildNewCluster(m_clusters, m_serviceFactory,
        m_componentFactory, m_schFactory, HOST1);

    m_dao = m_injector.getInstance(AlertsDAO.class);
    m_definitionDao = m_injector.getInstance(AlertDefinitionDAO.class);

    // create 5 definitions, some with HDFS and some with YARN
    for (int i = 0; i < 5; i++) {
      String serviceName = "HDFS";
      String componentName = "DATANODE";
      if (i >= 3) {
        serviceName = "YARN";
        componentName = "RESOURCEMANAGER";
      }

      AlertDefinitionEntity definition = new AlertDefinitionEntity();
      definition.setDefinitionName(ALERT_DEFINITION + i);
      definition.setServiceName(serviceName);
      definition.setComponentName(componentName);
      definition.setClusterId(m_cluster.getClusterId());
      definition.setHash(UUID.randomUUID().toString());
      definition.setScheduleInterval(Integer.valueOf(60));
      definition.setScope(Scope.SERVICE);
      definition.setSource("{\"type\" : \"SCRIPT\"}");
      definition.setSourceType(SourceType.SCRIPT);
      m_definitionDao.create(definition);
    }
  }

  @After
  public void teardown() {
    m_injector.getInstance(PersistService.class).stop();
    m_injector = null;
  }

  /**
   * Tests that a disabled definition doesn't record alert events.
   */
  @Test
  public void testDisabledAlert() {
    String definitionName = ALERT_DEFINITION + "1";
    String componentName = "DATANODE";

    Alert alert1 = new Alert(definitionName, null, "HDFS", componentName,
        HOST1, AlertState.OK);

    alert1.setCluster(m_cluster.getClusterName());
    alert1.setLabel(ALERT_LABEL);
    alert1.setText("HDFS " + componentName + " is OK");
    alert1.setTimestamp(1L);

    // verify that the listener works with a regular alert
    AlertReceivedListener listener = m_injector.getInstance(AlertReceivedListener.class);
    AlertReceivedEvent event1 = new AlertReceivedEvent(
        m_cluster.getClusterId(), alert1);
    listener.onAlertEvent(event1);

    List<AlertCurrentEntity> allCurrent = m_dao.findCurrent();
    assertEquals(1, allCurrent.size());

    // disable definition
    AlertDefinitionEntity definition = m_definitionDao.findByName(
        m_cluster.getClusterId(), definitionName);
    definition.setEnabled(false);
    m_definitionDao.merge(definition);

    // remove disabled
    m_dao.removeCurrentDisabledAlerts();
    allCurrent = m_dao.findCurrent();
    assertEquals(0, allCurrent.size());

    // verify no new alerts for disabled
    listener.onAlertEvent(event1);
    allCurrent = m_dao.findCurrent();
    assertEquals(0, allCurrent.size());
  }

  /**
   * Tests an invalid host is being reported in an alert.
   */
  @Test
  public void testInvalidHost() {
    String definitionName = ALERT_DEFINITION + "1";
    String componentName = "DATANODE";

    Alert alert1 = new Alert(definitionName, null, "HDFS", componentName,
        HOST1, AlertState.OK);

    alert1.setCluster(m_cluster.getClusterName());
    alert1.setLabel(ALERT_LABEL);
    alert1.setText("HDFS " + componentName + " is OK");
    alert1.setTimestamp(1L);

    // verify that the listener works with a regular alert
    AlertReceivedListener listener = m_injector.getInstance(AlertReceivedListener.class);
    AlertReceivedEvent event1 = new AlertReceivedEvent(
        m_cluster.getClusterId(), alert1);
    listener.onAlertEvent(event1);

    List<AlertCurrentEntity> allCurrent = m_dao.findCurrent();
    assertEquals(1, allCurrent.size());

    // invalid host
    alert1.setHost("INVALID");

    // remove all
    m_dao.removeCurrentByHost(HOST1);
    allCurrent = m_dao.findCurrent();
    assertEquals(0, allCurrent.size());

    // verify no new alerts for disabled
    listener.onAlertEvent(event1);
    allCurrent = m_dao.findCurrent();
    assertEquals(0, allCurrent.size());
  }

  /**
   * Tests that a disabled definition doesn't record alert events.
   */
  @Test
  public void testInvalidAlertDefinition() {
    String componentName = "DATANODE";

    Alert alert1 = new Alert("missing_alert_definition_name", null, "HDFS",
        componentName, HOST1, AlertState.OK);

    alert1.setLabel(ALERT_LABEL);
    alert1.setText("HDFS " + componentName + " is OK");
    alert1.setTimestamp(1L);

    // bad alert definition name means no current alerts
    AlertReceivedListener listener = m_injector.getInstance(AlertReceivedListener.class);
    AlertReceivedEvent event1 = new AlertReceivedEvent(
        m_cluster.getClusterId(), alert1);
    listener.onAlertEvent(event1);

    List<AlertCurrentEntity> allCurrent = m_dao.findCurrent();
    assertEquals(0, allCurrent.size());
  }

  /**
   * Tests an invalid pairing of component to host.
   */
  @Test
  public void testInvalidServiceComponentHost() {
    String definitionName = ALERT_DEFINITION + "1";
    String componentName = "DATANODE";

    Alert alert1 = new Alert(definitionName, null, "HDFS", componentName,
        HOST1, AlertState.OK);

    alert1.setCluster(m_cluster.getClusterName());
    alert1.setLabel(ALERT_LABEL);
    alert1.setText("HDFS " + componentName + " is OK");
    alert1.setTimestamp(1L);

    // verify that the listener works with a regular alert
    AlertReceivedListener listener = m_injector.getInstance(AlertReceivedListener.class);
    AlertReceivedEvent event1 = new AlertReceivedEvent(
        m_cluster.getClusterId(), alert1);
    listener.onAlertEvent(event1);

    List<AlertCurrentEntity> allCurrent = m_dao.findCurrent();
    assertEquals(1, allCurrent.size());

    // invalid host
    alert1.setComponent("INVALID");

    // remove all
    m_dao.removeCurrentByHost(HOST1);
    allCurrent = m_dao.findCurrent();
    assertEquals(0, allCurrent.size());

    // verify no new alerts for disabled
    listener.onAlertEvent(event1);
    allCurrent = m_dao.findCurrent();
    assertEquals(0, allCurrent.size());
  }
}
