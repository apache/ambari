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

import junit.framework.Assert;

import org.apache.ambari.server.events.AlertReceivedEvent;
import org.apache.ambari.server.events.InitialAlertEvent;
import org.apache.ambari.server.events.MockEventListener;
import org.apache.ambari.server.events.publishers.AlertEventPublisher;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.state.Alert;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.utils.EventBusSynchronizer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.eventbus.EventBus;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.persist.PersistService;
import com.google.inject.util.Modules;

/**
 * Tests that {@link InitialAlertEventTest} instances are fired correctly.
 */
public class InitialAlertEventTest {

  private AlertsDAO m_alertsDao;
  private AlertEventPublisher m_eventPublisher;
  private Injector m_injector;
  private MockEventListener m_listener;

  private AlertDefinitionDAO m_definitionDao;
  private Clusters m_clusters;
  private Cluster m_cluster;
  private String m_clusterName;
  private ServiceFactory m_serviceFactory;

  /**
   *
   */
  @Before
  public void setup() throws Exception {
    m_injector = Guice.createInjector(Modules.override(
        new InMemoryDefaultTestModule()).with(new MockModule()));

    m_injector.getInstance(GuiceJpaInitializer.class);

    // get a mock listener
    m_listener = m_injector.getInstance(MockEventListener.class);

    // create the publisher and mock listener
    m_eventPublisher = m_injector.getInstance(AlertEventPublisher.class);

    // register listeners needed
    EventBus synchronizedBus = EventBusSynchronizer.synchronizeAlertEventPublisher(m_injector);
    synchronizedBus.register(m_listener);

    m_definitionDao = m_injector.getInstance(AlertDefinitionDAO.class);
    m_clusters = m_injector.getInstance(Clusters.class);
    m_serviceFactory = m_injector.getInstance(ServiceFactory.class);

    m_alertsDao = m_injector.getInstance(AlertsDAO.class);

    m_clusterName = "c1";
    m_clusters.addCluster(m_clusterName, new StackId("HDP", "2.0.6"));
    m_cluster = m_clusters.getCluster(m_clusterName);
    Assert.assertNotNull(m_cluster);

    // install HDFS to get 6 definitions
    installHdfsService();
    Assert.assertEquals(1, m_cluster.getServices().size());
    Assert.assertEquals(6, m_definitionDao.findAll().size());
  }

  /**
   * @throws Exception
   */
  @After
  public void teardown() throws Exception {
    m_injector.getInstance(PersistService.class).stop();
    m_injector = null;
  }

  /**
   * Tests that when a new alert is received that an {@link InitialAlertEvent}
   * is fired.
   *
   * @throws Exception
   */
  @Test
  public void testInitialAlertEvent() throws Exception {
    // ensure there are no historical items
    Assert.assertEquals(0, m_alertsDao.findAll().size());
    Assert.assertEquals(0,
        m_listener.getAlertEventReceivedCount(InitialAlertEvent.class));

    // get a definition to use for the incoming alert
    AlertDefinitionEntity definition = m_definitionDao.findAll(
        m_cluster.getClusterId()).get(0);

    // create the "first" alert
    Alert alert = new Alert(definition.getDefinitionName(), null,
        definition.getServiceName(), definition.getComponentName(), null,
        AlertState.OK);
    alert.setCluster(m_clusterName);

    AlertReceivedEvent event = new AlertReceivedEvent(m_cluster.getClusterId(),
        alert);

    // public the received event
    m_eventPublisher.publish(event);

    // ensure we now have a history item
    Assert.assertEquals(1, m_alertsDao.findAll().size());

    // verify that the initial alert event was triggered
    Assert.assertEquals(1,
        m_listener.getAlertEventReceivedCount(InitialAlertEvent.class));

    // clear the initial alert event that was recorded
    m_listener.reset();

    // alert the alert and re-fire
    alert.setState(AlertState.WARNING);
    m_eventPublisher.publish(event);

    // ensure that the initial alert event was NOT received again
    Assert.assertEquals(0,
        m_listener.getAlertEventReceivedCount(InitialAlertEvent.class));
  }

  /**
   * Calls {@link Service#persist()} to mock a service install.
   */
  private void installHdfsService() throws Exception {
    String serviceName = "HDFS";
    Service service = m_serviceFactory.createNew(m_cluster, serviceName);
    m_cluster.addService(service);
    service.persist();
    service = m_cluster.getService(serviceName);

    Assert.assertNotNull(service);
  }

  /**
   *
   */
  private static class MockModule implements Module {
    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(Binder binder) {
      // sychronize on the ambari event bus for this test to work properly
      EventBusSynchronizer.synchronizeAmbariEventPublisher(binder);
    }
  }
}
