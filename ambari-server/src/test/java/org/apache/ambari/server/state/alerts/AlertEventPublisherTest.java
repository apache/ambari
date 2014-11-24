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

import java.lang.reflect.Field;

import junit.framework.Assert;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.events.AlertDefinitionDeleteEvent;
import org.apache.ambari.server.events.AmbariEvent;
import org.apache.ambari.server.events.listeners.alerts.AlertLifecycleListener;
import org.apache.ambari.server.events.listeners.alerts.AlertServiceStateListener;
import org.apache.ambari.server.events.listeners.alerts.AlertStateChangedListener;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.AlertDispatchDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertGroupEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.alert.AggregateDefinitionMapping;
import org.apache.ambari.server.state.alert.AggregateSource;
import org.apache.ambari.server.state.alert.AlertDefinition;
import org.apache.ambari.server.state.alert.Scope;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.eventbus.EventBus;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

/**
 * Tests that {@link AmbariEvent} instances are fired correctly and that alert
 * data is bootstrapped into the database.
 */
public class AlertEventPublisherTest {

  private AlertDispatchDAO dispatchDao;
  private AlertDefinitionDAO definitionDao;
  private Clusters clusters;
  private Cluster cluster;
  private String clusterName;
  private Injector injector;
  private ServiceFactory serviceFactory;
  private AmbariMetaInfo metaInfo;
  private OrmTestHelper ormHelper;
  private AggregateDefinitionMapping aggregateMapping;
  private AmbariEventPublisher eventPublisher;

  /**
   *
   */
  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);

    eventPublisher = injector.getInstance(AmbariEventPublisher.class);
    EventBus synchronizedBus = new EventBus();

    // force singleton init via Guice so the listener registers with the bus
    synchronizedBus.register(injector.getInstance(AlertLifecycleListener.class));
    synchronizedBus.register(injector.getInstance(AlertStateChangedListener.class));
    synchronizedBus.register(injector.getInstance(AlertServiceStateListener.class));

    // !!! need a synchronous op for testing
    Field field = AmbariEventPublisher.class.getDeclaredField("m_eventBus");
    field.setAccessible(true);
    field.set(eventPublisher, synchronizedBus);

    dispatchDao = injector.getInstance(AlertDispatchDAO.class);
    definitionDao = injector.getInstance(AlertDefinitionDAO.class);
    clusters = injector.getInstance(Clusters.class);
    serviceFactory = injector.getInstance(ServiceFactory.class);
    ormHelper = injector.getInstance(OrmTestHelper.class);
    aggregateMapping = injector.getInstance(AggregateDefinitionMapping.class);

    metaInfo = injector.getInstance(AmbariMetaInfo.class);
    metaInfo.init();

    clusterName = "foo";
    clusters.addCluster(clusterName);
    cluster = clusters.getCluster(clusterName);
    cluster.setDesiredStackVersion(new StackId("HDP", "2.0.6"));
    Assert.assertNotNull(cluster);
  }

  /**
   * @throws Exception
   */
  @After
  public void teardown() throws Exception {
    injector.getInstance(PersistService.class).stop();
    injector = null;
  }

  /**
   * Tests that a default {@link AlertGroupEntity} is created when a service is
   * installed.
   *
   * @throws Exception
   */
  @Test
  public void testDefaultAlertGroupCreation() throws Exception {
    Assert.assertEquals(0, dispatchDao.findAllGroups().size());
    installHdfsService();
    Assert.assertEquals(1, dispatchDao.findAllGroups().size());
  }

  /**
   * Tests that a default {@link AlertGroupEntity} is removed when a service is
   * removed.
   *
   * @throws Exception
   */
  @Test
  public void testDefaultAlertGroupRemoved() throws Exception {
    Assert.assertEquals(0, dispatchDao.findAllGroups().size());
    installHdfsService();
    Assert.assertEquals(1, dispatchDao.findAllGroups().size());
    cluster.getService("HDFS").delete();
    Assert.assertEquals(0, dispatchDao.findAllGroups().size());
  }

  /**
   * Tests that all {@link AlertDefinitionEntity} instances are created for the
   * installed service.
   *
   * @throws Exception
   */
  @Test
  public void testAlertDefinitionInsertion() throws Exception {
    Assert.assertEquals(0, definitionDao.findAll().size());
    installHdfsService();
    Assert.assertEquals(6, definitionDao.findAll().size());
  }

  /**
   * Tests that {@link AlertDefinitionDeleteEvent} instances are fired when a
   * definition is removed.
   *
   * @throws Exception
   */
  @Test
  public void testAlertDefinitionRemoval() throws Exception {
    Assert.assertEquals(0, definitionDao.findAll().size());
    AlertDefinitionEntity definition = ormHelper.createAlertDefinition(1L);
    Assert.assertEquals(1, definitionDao.findAll().size());

    AggregateSource source = new AggregateSource();
    source.setAlertName(definition.getDefinitionName());

    AlertDefinition aggregate = new AlertDefinition();
    aggregate.setClusterId(1L);
    aggregate.setComponentName("DATANODE");
    aggregate.setEnabled(true);
    aggregate.setInterval(1);
    aggregate.setLabel("DataNode Aggregate");
    aggregate.setName("datanode_aggregate");
    aggregate.setScope(Scope.ANY);
    aggregate.setServiceName("HDFS");
    aggregate.setSource(source);
    aggregate.setUuid("uuid");

    aggregateMapping.registerAggregate(1L, aggregate);
    Assert.assertNotNull(aggregateMapping.getAggregateDefinition(1L,
        source.getAlertName()));

    definitionDao.remove(definition);

    Assert.assertNull(aggregateMapping.getAggregateDefinition(1L,
        source.getAlertName()));
  }

  /**
   * Calls {@link Service#persist()} to mock a service install.
   */
  private void installHdfsService() throws Exception {
    String serviceName = "HDFS";
    Service service = serviceFactory.createNew(cluster, serviceName);
    cluster.addService(service);
    service.persist();
    service = cluster.getService(serviceName);

    Assert.assertNotNull(service);
  }
}
