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
package org.apache.ambari.server.events;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentFactory;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.eventbus.EventBus;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

/**
 * Tests that {@link EventsTest} instances are fired correctly and
 * that alert data is bootstrapped into the database.
 */
public class EventsTest {

  private static final String HOSTNAME = "c6401.ambari.apache.org";

  private Clusters m_clusters;
  private Cluster m_cluster;
  private String m_clusterName;
  private Injector m_injector;
  private ServiceFactory m_serviceFactory;
  private ServiceComponentFactory m_componentFactory;
  private ServiceComponentHostFactory m_schFactory;
  private AmbariEventPublisher m_eventPublisher;
  private MockEventListener m_listener;

  /**
   *
   */
  @Before
  public void setup() throws Exception {
    m_injector = Guice.createInjector(new InMemoryDefaultTestModule());
    m_injector.getInstance(GuiceJpaInitializer.class);

    m_eventPublisher = m_injector.getInstance(AmbariEventPublisher.class);
    EventBus synchronizedBus = new EventBus();

    // register mock listener
    m_listener = m_injector.getInstance(MockEventListener.class);
    synchronizedBus.register(m_listener);

    // !!! need a synchronous op for testing
    Field field = AmbariEventPublisher.class.getDeclaredField("m_eventBus");
    field.setAccessible(true);
    field.set(m_eventPublisher, synchronizedBus);

    m_clusters = m_injector.getInstance(Clusters.class);
    m_serviceFactory = m_injector.getInstance(ServiceFactory.class);
    m_componentFactory = m_injector.getInstance(ServiceComponentFactory.class);
    m_schFactory = m_injector.getInstance(ServiceComponentHostFactory.class);

    m_clusterName = "foo";
    m_clusters.addCluster(m_clusterName);
    m_clusters.addHost(HOSTNAME);

    Host host = m_clusters.getHost(HOSTNAME);
    Map<String, String> hostAttributes = new HashMap<String, String>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "6.4");
    host.setHostAttributes(hostAttributes);
    host.setState(HostState.HEALTHY);
    host.persist();

    m_cluster = m_clusters.getCluster(m_clusterName);
    Assert.assertNotNull(m_cluster);
    StackId stackId = new StackId("HDP", "2.0.6");
    m_cluster.setDesiredStackVersion(stackId);
    m_cluster.createClusterVersion(stackId.getStackName(), stackId.getStackVersion(), "admin", RepositoryVersionState.CURRENT);

    m_clusters.mapHostToCluster(HOSTNAME, m_clusterName);
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
   * Tests that {@link ServiceInstalledEvent}s are fired correctly.
   *
   * @throws Exception
   */
  @Test
  public void testServiceInstalledEvent() throws Exception {
    Class<?> eventClass = ServiceInstalledEvent.class;
    Assert.assertFalse(m_listener.isEventReceived(eventClass));
    installHdfsService();
    Assert.assertTrue(m_listener.isEventReceived(eventClass));
  }

  /**
   * Tests that {@link ServiceRemovedEvent}s are fired correctly.
   *
   * @throws Exception
   */
  @Test
  public void testServiceRemovedEvent() throws Exception {
    Class<?> eventClass = ServiceRemovedEvent.class;
    Assert.assertFalse(m_listener.isEventReceived(eventClass));
    installHdfsService();
    m_cluster.deleteAllServices();
    Assert.assertTrue(m_listener.isEventReceived(eventClass));
  }

  /**
   * Tests that {@link ServiceComponentUninstalledEvent}s are fired correctly.
   *
   * @throws Exception
   */
  @Test
  public void testServiceComponentUninstalledEvent() throws Exception {
    Class<?> eventClass = ServiceComponentUninstalledEvent.class;
    installHdfsService();

    Assert.assertFalse(m_listener.isEventReceived(eventClass));
    m_cluster.getServiceComponentHosts(HOSTNAME).get(0).delete();

    Assert.assertTrue(m_listener.isEventReceived(eventClass));
  }

  /**
   * Tests that {@link MaintenanceModeEvent}s are fired correctly.
   *
   * @throws Exception
   */
  @Test
  public void testMaintenanceModeEvents() throws Exception {
    installHdfsService();
    Service service = m_cluster.getService("HDFS");
    Class<?> eventClass = MaintenanceModeEvent.class;

    Assert.assertFalse(m_listener.isEventReceived(eventClass));
    service.setMaintenanceState(MaintenanceState.ON);
    Assert.assertTrue(m_listener.isEventReceived(eventClass));
    Assert.assertEquals(1, m_listener.getEventReceivedCount(eventClass));

    m_listener.reset();
    Assert.assertFalse(m_listener.isEventReceived(eventClass));

    List<ServiceComponentHost> componentHosts = m_cluster.getServiceComponentHosts(HOSTNAME);
    ServiceComponentHost componentHost = componentHosts.get(0);
    componentHost.setMaintenanceState(MaintenanceState.OFF);

    Assert.assertTrue(m_listener.isEventReceived(eventClass));
    Assert.assertEquals(1, m_listener.getEventReceivedCount(eventClass));

    m_listener.reset();
    Assert.assertFalse(m_listener.isEventReceived(eventClass));

    Host host = m_clusters.getHost(HOSTNAME);
    host.setMaintenanceState(m_cluster.getClusterId(), MaintenanceState.ON);
    host.setMaintenanceState(m_cluster.getClusterId(), MaintenanceState.OFF);

    Assert.assertTrue(m_listener.isEventReceived(eventClass));
    Assert.assertEquals(2, m_listener.getEventReceivedCount(eventClass));
  }

  /**
   * Calls {@link Service#persist()} to mock a service install along with
   * creating a single {@link Host} and {@link ServiceComponentHost}.
   */
  private void installHdfsService() throws Exception {
    String serviceName = "HDFS";
    Service service = m_serviceFactory.createNew(m_cluster, serviceName);
    m_cluster.addService(service);
    service.persist();
    service = m_cluster.getService(serviceName);
    Assert.assertNotNull(service);

    ServiceComponent component = m_componentFactory.createNew(service, "DATANODE");
    service.addServiceComponent(component);
    component.setDesiredState(State.INSTALLED);
    component.persist();

    ServiceComponentHost sch = m_schFactory.createNew(component, HOSTNAME);

    component.addServiceComponentHost(sch);
    sch.setDesiredState(State.INSTALLED);
    sch.setState(State.INSTALLED);
    sch.setDesiredStackVersion(new StackId("HDP-2.0.6"));
    sch.setStackVersion(new StackId("HDP-2.0.6"));

    sch.persist();
  }
}
