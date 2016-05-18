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

import java.util.UUID;

import junit.framework.Assert;

import org.apache.ambari.server.events.AlertDefinitionChangedEvent;
import org.apache.ambari.server.events.AlertDefinitionDeleteEvent;
import org.apache.ambari.server.events.AmbariEvent;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.AlertDispatchDAO;
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.orm.entities.AlertCurrentEntity;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertGroupEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.alert.AggregateDefinitionMapping;
import org.apache.ambari.server.state.alert.AggregateSource;
import org.apache.ambari.server.state.alert.AlertDefinition;
import org.apache.ambari.server.state.alert.Reporting;
import org.apache.ambari.server.state.alert.Reporting.ReportTemplate;
import org.apache.ambari.server.state.alert.Scope;
import org.apache.ambari.server.state.alert.SourceType;
import org.apache.ambari.server.utils.EventBusSynchronizer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
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
  private AlertsDAO alertsDao;
  private Clusters clusters;
  private Cluster cluster;
  private String clusterName;
  private Injector injector;
  private ServiceFactory serviceFactory;
  private OrmTestHelper ormHelper;
  private AggregateDefinitionMapping aggregateMapping;

  /**
   *
   */
  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);

    EventBusSynchronizer.synchronizeAmbariEventPublisher(injector);

    dispatchDao = injector.getInstance(AlertDispatchDAO.class);
    definitionDao = injector.getInstance(AlertDefinitionDAO.class);
    alertsDao = injector.getInstance(AlertsDAO.class);
    clusters = injector.getInstance(Clusters.class);
    serviceFactory = injector.getInstance(ServiceFactory.class);
    ormHelper = injector.getInstance(OrmTestHelper.class);
    aggregateMapping = injector.getInstance(AggregateDefinitionMapping.class);

    clusterName = "foo";
    clusters.addCluster(clusterName, new StackId("HDP", "2.0.6"));
    cluster = clusters.getCluster(clusterName);
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
   * Tests that {@link AlertDefinitionChangedEvent} instances are fired when a
   * definition is updated.
   *
   * @throws Exception
   */
  @Test
  public void testAlertDefinitionChanged() throws Exception {
    installHdfsService();

    int definitionCount = definitionDao.findAll().size();
    AlertDefinitionEntity definition = ormHelper.createAlertDefinition(1L);
    Assert.assertEquals(definitionCount + 1, definitionDao.findAll().size());

    AggregateSource source = new AggregateSource();
    Reporting reporting = new Reporting();
    ReportTemplate okTemplate = new ReportTemplate();
    okTemplate.setValue(50.0d);
    okTemplate.setText("foo");
    reporting.setOk(okTemplate);
    source.setReporting(reporting);
    source.setAlertName(definition.getDefinitionName());
    source.setType(SourceType.AGGREGATE);

    AlertDefinitionEntity aggregateEntity = new AlertDefinitionEntity();
    aggregateEntity.setClusterId(1L);
    aggregateEntity.setComponentName("DATANODE");
    aggregateEntity.setEnabled(true);
    aggregateEntity.setDefinitionName("datanode_aggregate");
    aggregateEntity.setScope(Scope.ANY);
    aggregateEntity.setServiceName("HDFS");
    aggregateEntity.setSource(new Gson().toJson(source));
    aggregateEntity.setHash(UUID.randomUUID().toString());
    aggregateEntity.setScheduleInterval(1);
    aggregateEntity.setSourceType(SourceType.AGGREGATE);

    // creating the aggregate alert will register it with the mapping
    definitionDao.create(aggregateEntity);

    // pull it out of the mapping and compare fields
    AlertDefinition aggregate = aggregateMapping.getAggregateDefinition(1L,
        source.getAlertName());

    Assert.assertNotNull(aggregate);
    Assert.assertEquals("foo",
        aggregate.getSource().getReporting().getOk().getText());

    // change something about the aggregate's reporting
    String sourceText = aggregateEntity.getSource();
    sourceText = sourceText.replace("foo", "bar");
    aggregateEntity.setSource(sourceText);

    // save the aggregate; this should trigger the event,
    // causing the updated aggregate definition to be mapped
    definitionDao.merge(aggregateEntity);

    // check the aggregate mapping for the new value
    aggregate = aggregateMapping.getAggregateDefinition(1L,
        source.getAlertName());

    Assert.assertNotNull(aggregate);
    Assert.assertEquals("bar",
        aggregate.getSource().getReporting().getOk().getText());
  }

  @Test
  public void testAlertDefinitionNameChangeEvent() throws Exception {
    installHdfsService();
    AlertDefinitionEntity definition = definitionDao.findAll().get(0);

    // create 2 historical entries; one will be current
    AlertHistoryEntity history = new AlertHistoryEntity();
    history.setServiceName(definition.getServiceName());
    history.setClusterId(cluster.getClusterId());
    history.setAlertDefinition(definition);
    history.setAlertLabel(definition.getLabel());
    history.setAlertText(definition.getDefinitionName());
    history.setAlertTimestamp(Long.valueOf(1L));
    history.setHostName(null);
    history.setAlertState(AlertState.OK);
    alertsDao.create(history);

    // this one will be current
    AlertHistoryEntity history2 = new AlertHistoryEntity();
    history2.setServiceName(definition.getServiceName());
    history2.setClusterId(cluster.getClusterId());
    history2.setAlertDefinition(definition);
    history2.setAlertLabel(definition.getLabel());
    history2.setAlertText(definition.getDefinitionName());
    history2.setAlertTimestamp(Long.valueOf(1L));
    history2.setHostName(null);
    history2.setAlertState(AlertState.CRITICAL);

    // current for the history
    AlertCurrentEntity current = new AlertCurrentEntity();
    current.setOriginalTimestamp(1L);
    current.setLatestTimestamp(2L);
    current.setAlertHistory(history2);
    alertsDao.create(current);

    // change the definition name
    definition.setLabel("testAlertDefinitionNameChangeEvent");
    definitionDao.merge(definition);

    // the older history item will not have the label changed while
    // the new one will
    history = alertsDao.findById(history.getAlertId());
    history2 = alertsDao.findById(history2.getAlertId());

    Assert.assertFalse(definition.getLabel().equals(history.getAlertLabel()));
    Assert.assertEquals(definition.getLabel(), history2.getAlertLabel());
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
    service.persist();
    service = cluster.getService(serviceName);

    Assert.assertNotNull(service);
  }
}
