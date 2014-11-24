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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.alert.Scope;
import org.apache.ambari.server.state.alert.SourceType;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.persist.PersistService;
import com.google.inject.util.Modules;


/**
 * Tests the {@link AlertReceivedListener}.
 */
public class AlertReceivedListenerTest {

  private static final String ALERT_DEFINITION = "alert_definition_";
  private static final String CLUSTER_NAME = "c1";
  private static final String SERVICE = "Service";
  private static final String COMPONENT = "Component";
  private static final String HOST1 = "h1";
  private static final String ALERT_LABEL = "My Label";

  private Long clusterId;
  private Injector injector;
  private OrmTestHelper helper;
  private AlertsDAO dao;
  private AlertDefinitionDAO definitionDao;

  private Clusters clusters;
  private Cluster cluster;

  @Before
  public void setup() throws Exception {
    clusters = EasyMock.createNiceMock(Clusters.class);
    cluster = EasyMock.createNiceMock(Cluster.class);

    injector = Guice.createInjector(Modules.override(
        new InMemoryDefaultTestModule()).with(new MockModule()));

    injector.getInstance(GuiceJpaInitializer.class);
    helper = injector.getInstance(OrmTestHelper.class);
    clusterId = helper.createCluster();
    dao = injector.getInstance(AlertsDAO.class);
    definitionDao = injector.getInstance(AlertDefinitionDAO.class);

    List<Host> hosts = new ArrayList<Host>();
    Host host = EasyMock.createNiceMock(Host.class);
    EasyMock.expect(host.getHostName()).andReturn(HOST1).anyTimes();
    hosts.add(host);

    Map<String,Service> services = new HashMap<String, Service>();
    services.put("Service 1", EasyMock.createNiceMock(Service.class));

    List<ServiceComponentHost> schs = new ArrayList<ServiceComponentHost>();
    ServiceComponentHost sch = EasyMock.createNiceMock(ServiceComponentHost.class);
    EasyMock.expect(sch.getServiceComponentName()).andReturn("Component 1").anyTimes();
    schs.add(sch);

    // setup isValid expectations
    EasyMock.expect(clusters.getCluster(CLUSTER_NAME)).andReturn(cluster).anyTimes();
    EasyMock.expect(clusters.getHosts()).andReturn(hosts).anyTimes();
    EasyMock.expect(cluster.getServices()).andReturn(services).anyTimes();
    EasyMock.expect(cluster.getServiceComponentHosts(HOST1)).andReturn(schs).anyTimes();

    EasyMock.replay(clusters, cluster, sch, host);

    // create 5 definitions
    for (int i = 0; i < 5; i++) {
      AlertDefinitionEntity definition = new AlertDefinitionEntity();
      definition.setDefinitionName(ALERT_DEFINITION + i);
      definition.setServiceName(SERVICE + " " + i);
      definition.setComponentName(COMPONENT + " " + i);
      definition.setClusterId(clusterId);
      definition.setHash(UUID.randomUUID().toString());
      definition.setScheduleInterval(Integer.valueOf(60));
      definition.setScope(Scope.SERVICE);
      definition.setSource("{\"type\" : \"SCRIPT\"}");
      definition.setSourceType(SourceType.SCRIPT);
      definitionDao.create(definition);
    }
  }

  @After
  public void teardown() {
    injector.getInstance(PersistService.class).stop();
    injector = null;
  }

  /**
   * Tests that a disabled definition doesn't record alert events.
   */
  @Test
  public void testDisabledAlert() {
    String definitionName = ALERT_DEFINITION + "1";
    String serviceName = "Service 1";
    String componentName = "Component 1";

    Alert alert1 = new Alert(definitionName, null, serviceName, componentName,
        HOST1, AlertState.OK);

    alert1.setCluster(CLUSTER_NAME);
    alert1.setLabel(ALERT_LABEL);
    alert1.setText(serviceName + " " + componentName + " is OK");
    alert1.setTimestamp(1L);

    // verify that the listener works with a regular alert
    AlertReceivedListener listener = injector.getInstance(AlertReceivedListener.class);
    AlertReceivedEvent event1 = new AlertReceivedEvent(clusterId, alert1);
    listener.onAlertEvent(event1);

    List<AlertCurrentEntity> allCurrent = dao.findCurrent();
    assertEquals(1, allCurrent.size());

    // disable definition
    AlertDefinitionEntity definition = definitionDao.findByName(clusterId, definitionName);
    definition.setEnabled(false);
    definitionDao.merge(definition);

    // remove disabled
    dao.removeCurrentDisabledAlerts();
    allCurrent = dao.findCurrent();
    assertEquals(0, allCurrent.size());

    // verify no new alerts for disabled
    listener.onAlertEvent(event1);
    allCurrent = dao.findCurrent();
    assertEquals(0, allCurrent.size());
  }

  /**
   * Tests an invalid host is being reported in an alert.
   */
  @Test
  public void testInvalidHost() {
    String definitionName = ALERT_DEFINITION + "1";
    String serviceName = "Service 1";
    String componentName = "Component 1";

    Alert alert1 = new Alert(definitionName, null, serviceName, componentName,
        HOST1, AlertState.OK);

    alert1.setCluster(CLUSTER_NAME);
    alert1.setLabel(ALERT_LABEL);
    alert1.setText(serviceName + " " + componentName + " is OK");
    alert1.setTimestamp(1L);

    // verify that the listener works with a regular alert
    AlertReceivedListener listener = injector.getInstance(AlertReceivedListener.class);
    AlertReceivedEvent event1 = new AlertReceivedEvent(clusterId, alert1);
    listener.onAlertEvent(event1);

    List<AlertCurrentEntity> allCurrent = dao.findCurrent();
    assertEquals(1, allCurrent.size());

    // invalid host
    alert1.setHost("INVALID");

    // remove all
    dao.removeCurrentByHost(HOST1);
    allCurrent = dao.findCurrent();
    assertEquals(0, allCurrent.size());

    // verify no new alerts for disabled
    listener.onAlertEvent(event1);
    allCurrent = dao.findCurrent();
    assertEquals(0, allCurrent.size());
  }

  /**
   * Tests that a disabled definition doesn't record alert events.
   */
  @Test
  public void testInvalidAlertDefinition() {
    String serviceName = "Service 1";
    String componentName = "Component 1";

    Alert alert1 = new Alert("missing_alert_definition_name", null,
        serviceName, componentName, HOST1, AlertState.OK);

    alert1.setLabel(ALERT_LABEL);
    alert1.setText(serviceName + " " + componentName + " is OK");
    alert1.setTimestamp(1L);

    // bad alert definition name means no current alerts
    AlertReceivedListener listener = injector.getInstance(AlertReceivedListener.class);
    AlertReceivedEvent event1 = new AlertReceivedEvent(clusterId, alert1);
    listener.onAlertEvent(event1);

    List<AlertCurrentEntity> allCurrent = dao.findCurrent();
    assertEquals(0, allCurrent.size());
  }

  /**
   * Tests an invalid pairing of component to host.
   */
  @Test
  public void testInvalidServiceComponentHost() {
    String definitionName = ALERT_DEFINITION + "1";
    String serviceName = "Service 1";
    String componentName = "Component 1";

    Alert alert1 = new Alert(definitionName, null, serviceName, componentName,
        HOST1, AlertState.OK);

    alert1.setCluster(CLUSTER_NAME);
    alert1.setLabel(ALERT_LABEL);
    alert1.setText(serviceName + " " + componentName + " is OK");
    alert1.setTimestamp(1L);

    // verify that the listener works with a regular alert
    AlertReceivedListener listener = injector.getInstance(AlertReceivedListener.class);
    AlertReceivedEvent event1 = new AlertReceivedEvent(clusterId, alert1);
    listener.onAlertEvent(event1);

    List<AlertCurrentEntity> allCurrent = dao.findCurrent();
    assertEquals(1, allCurrent.size());

    // invalid host
    alert1.setComponent("INVALID");

    // remove all
    dao.removeCurrentByHost(HOST1);
    allCurrent = dao.findCurrent();
    assertEquals(0, allCurrent.size());

    // verify no new alerts for disabled
    listener.onAlertEvent(event1);
    allCurrent = dao.findCurrent();
    assertEquals(0, allCurrent.size());
  }

  /**
   *
   */
  private class MockModule implements Module {
    @Override
    public void configure(Binder binder) {
      binder.bind(Clusters.class).toInstance(clusters);
      binder.bind(Cluster.class).toInstance(cluster);
    }
  }
}
