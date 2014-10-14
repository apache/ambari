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

package org.apache.ambari.server.orm.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import junit.framework.Assert;

import org.apache.ambari.server.controller.AlertNoticeRequest;
import org.apache.ambari.server.controller.internal.AlertNoticeResourceProvider;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.orm.AlertDaoHelper;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertGroupEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.orm.entities.AlertNoticeEntity;
import org.apache.ambari.server.orm.entities.AlertTargetEntity;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.NotificationState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentFactory;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.alert.Scope;
import org.apache.ambari.server.state.alert.SourceType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

/**
 * Tests {@link AlertDispatchDAO}.
 */
public class AlertDispatchDAOTest {

  private final static String HOSTNAME = "c6401.ambari.apache.org";

  private Clusters m_clusters;
  private Long m_clusterId;
  private Injector m_injector;
  private AlertDispatchDAO m_dao;
  private AlertDefinitionDAO m_definitionDao;
  private AlertsDAO m_alertsDao;
  private OrmTestHelper m_helper;

  private ServiceFactory m_serviceFactory;
  private ServiceComponentFactory m_componentFactory;
  private ServiceComponentHostFactory m_schFactory;
  private AlertDaoHelper m_alertHelper;

  /**
   *
   */
  @Before
  public void setup() throws Exception {
    m_injector = Guice.createInjector(new InMemoryDefaultTestModule());
    m_injector.getInstance(GuiceJpaInitializer.class);
    m_dao = m_injector.getInstance(AlertDispatchDAO.class);
    m_alertsDao = m_injector.getInstance(AlertsDAO.class);
    m_definitionDao = m_injector.getInstance(AlertDefinitionDAO.class);
    m_helper = m_injector.getInstance(OrmTestHelper.class);
    m_serviceFactory = m_injector.getInstance(ServiceFactory.class);
    m_componentFactory = m_injector.getInstance(ServiceComponentFactory.class);
    m_schFactory = m_injector.getInstance(ServiceComponentHostFactory.class);
    m_clusters = m_injector.getInstance(Clusters.class);
    m_alertHelper = m_injector.getInstance(AlertDaoHelper.class);

    m_clusterId = m_helper.createCluster();
    Set<AlertTargetEntity> targets = createTargets();

    for (int i = 0; i < 10; i++) {
      AlertGroupEntity group = new AlertGroupEntity();
      group.setDefault(false);
      group.setGroupName("Group Name " + i);
      group.setClusterId(m_clusterId);
      for (AlertTargetEntity alertTarget : targets) {
        group.addAlertTarget(alertTarget);
      }

      m_dao.create(group);
    }
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
   *
   */
  @Test
  public void testFindAllTargets() throws Exception {
    List<AlertTargetEntity> targets = m_dao.findAllTargets();
    assertNotNull(targets);
    assertEquals(5, targets.size());
  }

  /**
   * @throws Exception
   */
  public void testFindTargetsByIds() throws Exception {
    List<AlertTargetEntity> targets = m_dao.findAllTargets();
    assertNotNull(targets);
    assertEquals(5, targets.size());

    List<Long> ids = new ArrayList<Long>();
    ids.add(targets.get(0).getTargetId());
    ids.add(targets.get(1).getTargetId());
    ids.add(99999L);

    targets = m_dao.findTargetsById(ids);
    assertEquals(2, targets.size());
  }

  /**
   *
   */
  @Test
  public void testFindTargetByName() throws Exception {
    List<AlertTargetEntity> targets = m_dao.findAllTargets();
    assertNotNull(targets);
    AlertTargetEntity target = targets.get(3);

    AlertTargetEntity actual = m_dao.findTargetByName(target.getTargetName());
    assertEquals(target, actual);
  }

  /**
   *
   */
  @Test
  public void testFindAllGroups() throws Exception {
    List<AlertGroupEntity> groups = m_dao.findAllGroups();
    assertNotNull(groups);
    assertEquals(10, groups.size());
  }

  /**
   *
   */
  @Test
  public void testFindGroupByName() throws Exception {
    List<AlertGroupEntity> groups = m_dao.findAllGroups();
    assertNotNull(groups);
    AlertGroupEntity group = groups.get(3);

    AlertGroupEntity actual = m_dao.findGroupByName(group.getClusterId(),
        group.getGroupName());

    assertEquals(group, actual);
  }

  /**
   *
   */
  @Test
  public void testCreateGroup() throws Exception {
    AlertTargetEntity target = m_helper.createAlertTarget();
    Set<AlertTargetEntity> targets = new HashSet<AlertTargetEntity>();
    targets.add(target);

    AlertGroupEntity group = m_helper.createAlertGroup(m_clusterId, targets);
    AlertGroupEntity actual = m_dao.findGroupById(group.getGroupId());
    assertNotNull(group);

    assertEquals(group.getGroupName(), actual.getGroupName());
    assertEquals(group.isDefault(), actual.isDefault());
    assertEquals(group.getAlertTargets(), actual.getAlertTargets());
    assertEquals(group.getAlertDefinitions(), actual.getAlertDefinitions());
  }

  /**
   *
   */
  @Test
  public void testGroupDefinitions() throws Exception {
    List<AlertDefinitionEntity> definitions = createDefinitions();

    AlertGroupEntity group = m_helper.createAlertGroup(m_clusterId, null);

    group = m_dao.findGroupById(group.getGroupId());
    assertNotNull(group);

    for (AlertDefinitionEntity definition : definitions) {
      group.addAlertDefinition(definition);
    }

    m_dao.merge(group);

    group = m_dao.findGroupByName(group.getGroupName());
    assertEquals(definitions.size(), group.getAlertDefinitions().size());

    for (AlertDefinitionEntity definition : definitions) {
      assertTrue(group.getAlertDefinitions().contains(definition));
    }

    m_definitionDao.refresh(definitions.get(0));
    m_definitionDao.remove(definitions.get(0));
    definitions.remove(0);

    group = m_dao.findGroupByName(group.getGroupName());
    assertEquals(definitions.size(), group.getAlertDefinitions().size());

    for (AlertDefinitionEntity definition : definitions) {
      assertTrue(group.getAlertDefinitions().contains(definition));
    }
  }

  /**
   *
   */
  @Test
  public void testCreateTarget() throws Exception {
    int targetCount = m_dao.findAllTargets().size();

    AlertTargetEntity target = m_helper.createAlertTarget();
    Set<AlertTargetEntity> targets = new HashSet<AlertTargetEntity>();
    targets.add(target);

    AlertGroupEntity group = m_helper.createAlertGroup(m_clusterId, targets);
    AlertTargetEntity actual = m_dao.findTargetById(target.getTargetId());
    assertNotNull(actual);

    assertEquals(target.getTargetName(), actual.getTargetName());
    assertEquals(target.getDescription(), actual.getDescription());
    assertEquals(target.getNotificationType(), actual.getNotificationType());
    assertEquals(target.getProperties(), actual.getProperties());

    assertNotNull(actual.getAlertGroups());
    Iterator<AlertGroupEntity> iterator = actual.getAlertGroups().iterator();
    AlertGroupEntity actualGroup = iterator.next();

    assertEquals(group, actualGroup);

    assertEquals(targetCount + 1, m_dao.findAllTargets().size());
  }

  /**
   *
   */
  @Test
  public void testDeleteGroup() throws Exception {
    int targetCount = m_dao.findAllTargets().size();

    AlertGroupEntity group = m_helper.createAlertGroup(m_clusterId, null);
    AlertTargetEntity target = m_helper.createAlertTarget();
    assertEquals(targetCount + 1, m_dao.findAllTargets().size());

    group = m_dao.findGroupById(group.getGroupId());
    assertNotNull(group);
    assertNotNull(group.getAlertTargets());
    assertEquals(0, group.getAlertTargets().size());

    group.addAlertTarget(target);
    m_dao.merge(group);

    group = m_dao.findGroupById(group.getGroupId());
    assertNotNull(group);
    assertNotNull(group.getAlertTargets());
    assertEquals(1, group.getAlertTargets().size());

    m_dao.remove(group);
    group = m_dao.findGroupById(group.getGroupId());
    assertNull(group);

    target = m_dao.findTargetById(target.getTargetId());
    assertNotNull(target);
    assertEquals(targetCount + 1, m_dao.findAllTargets().size());
  }

  /**
   *
   */
  @Test
  public void testDeleteTarget() throws Exception {
    AlertTargetEntity target = m_helper.createAlertTarget();
    target = m_dao.findTargetById(target.getTargetId());
    assertNotNull(target);

    m_dao.remove(target);

    target = m_dao.findTargetById(target.getTargetId());
    assertNull(target);
  }

  /**
   *
   */
  @Test
  public void testDeleteAssociatedTarget() throws Exception {
    AlertTargetEntity target = m_helper.createAlertTarget();
    Set<AlertTargetEntity> targets = new HashSet<AlertTargetEntity>();
    targets.add(target);

    AlertGroupEntity group = m_helper.createAlertGroup(m_clusterId, targets);
    assertEquals(1, group.getAlertTargets().size());

    target = m_dao.findTargetById(target.getTargetId());
    m_dao.refresh(target);

    assertNotNull(target);
    assertEquals(1, target.getAlertGroups().size());

    m_dao.remove(target);
    target = m_dao.findTargetById(target.getTargetId());
    assertNull(target);

    group = m_dao.findGroupById(group.getGroupId());
    assertNotNull(group);

    assertEquals(0, group.getAlertTargets().size());
  }

  /**
   *
   */
  @Test
  public void testUpdateGroup() throws Exception {
    AlertTargetEntity target = m_helper.createAlertTarget();
    Set<AlertTargetEntity> targets = new HashSet<AlertTargetEntity>();
    targets.add(target);

    String groupName = "Group Name " + System.currentTimeMillis();

    AlertGroupEntity group = m_helper.createAlertGroup(m_clusterId, null);

    group = m_dao.findGroupById(group.getGroupId());
    group.setGroupName(groupName + "FOO");
    group.setDefault(true);

    m_dao.merge(group);
    group = m_dao.findGroupById(group.getGroupId());

    assertEquals(groupName + "FOO", group.getGroupName());
    assertEquals(true, group.isDefault());
    assertEquals(0, group.getAlertDefinitions().size());
    assertEquals(0, group.getAlertTargets().size());

    group.addAlertTarget(target);
    m_dao.merge(group);

    group = m_dao.findGroupById(group.getGroupId());
    assertEquals(targets, group.getAlertTargets());
  }

  /**
   * Tests finding groups by a definition ID that they are associatd with.
   *
   * @throws Exception
   */
  @Test
  public void testFindGroupsByDefinition() throws Exception {
    List<AlertDefinitionEntity> definitions = createDefinitions();
    AlertGroupEntity group = m_helper.createAlertGroup(m_clusterId, null);

    group = m_dao.findGroupById(group.getGroupId());
    assertNotNull(group);

    for (AlertDefinitionEntity definition : definitions) {
      group.addAlertDefinition(definition);
    }

    m_dao.merge(group);

    group = m_dao.findGroupByName(group.getGroupName());
    assertEquals(definitions.size(), group.getAlertDefinitions().size());

    for (AlertDefinitionEntity definition : definitions) {
      List<AlertGroupEntity> groups = m_dao.findGroupsByDefinition(definition);
      assertEquals(1, groups.size());
      assertEquals(group.getGroupId(), groups.get(0).getGroupId());
    }
  }

  /**
   * @throws Exception
   */
  @Test
  public void testFindNoticeByUuid() throws Exception {
    List<AlertDefinitionEntity> definitions = createDefinitions();
    AlertDefinitionEntity definition = definitions.get(0);

    AlertHistoryEntity history = new AlertHistoryEntity();
    history.setServiceName(definition.getServiceName());
    history.setClusterId(m_clusterId);
    history.setAlertDefinition(definition);
    history.setAlertLabel("Label");
    history.setAlertState(AlertState.OK);
    history.setAlertText("Alert Text");
    history.setAlertTimestamp(System.currentTimeMillis());
    m_alertsDao.create(history);

    AlertTargetEntity target = m_helper.createAlertTarget();

    AlertNoticeEntity notice = new AlertNoticeEntity();
    notice.setUuid(UUID.randomUUID().toString());
    notice.setAlertTarget(target);
    notice.setAlertHistory(history);
    notice.setNotifyState(NotificationState.PENDING);
    m_dao.create(notice);

    AlertNoticeEntity actual = m_dao.findNoticeByUuid(notice.getUuid());
    assertEquals(notice.getNotificationId(), actual.getNotificationId());
    assertNull(m_dao.findNoticeByUuid("DEADBEEF"));
  }


  /**
   * Tests that the Ambari {@link Predicate} can be converted and submitted to
   * JPA correctly to return a restricted result set.
   *
   * @throws Exception
   */
  @Test
  public void testAlertNoticePredicate() throws Exception {
    Cluster cluster = initializeNewCluster();
    m_alertHelper.populateData(cluster);

    Predicate clusterPredicate = null;
    Predicate hdfsPredicate = null;
    Predicate yarnPredicate = null;
    Predicate adminPredicate = null;
    Predicate adminOrOperatorPredicate = null;
    Predicate pendingPredicate = null;
    Predicate noticeIdPredicate = null;

    clusterPredicate = new PredicateBuilder().property(
        AlertNoticeResourceProvider.ALERT_NOTICE_CLUSTER_NAME).equals("c1").toPredicate();

    hdfsPredicate = new PredicateBuilder().property(
        AlertNoticeResourceProvider.ALERT_NOTICE_SERVICE_NAME).equals("HDFS").toPredicate();

    yarnPredicate = new PredicateBuilder().property(
        AlertNoticeResourceProvider.ALERT_NOTICE_SERVICE_NAME).equals("YARN").toPredicate();

    adminPredicate = new PredicateBuilder().property(
        AlertNoticeResourceProvider.ALERT_NOTICE_TARGET_NAME).equals(
        "Administrators").toPredicate();

    adminOrOperatorPredicate = new PredicateBuilder().property(
        AlertNoticeResourceProvider.ALERT_NOTICE_TARGET_NAME).equals(
        "Administrators").or().property(
        AlertNoticeResourceProvider.ALERT_NOTICE_TARGET_NAME).equals(
        "Operators").toPredicate();

    pendingPredicate = new PredicateBuilder().property(
        AlertNoticeResourceProvider.ALERT_NOTICE_STATE).equals(
        NotificationState.PENDING.name()).toPredicate();

    AlertNoticeRequest request = new AlertNoticeRequest();
    request.Predicate = clusterPredicate;

    List<AlertNoticeEntity> notices = m_dao.findAllNotices(request);
    assertEquals(2, notices.size());

    request.Predicate = hdfsPredicate;
    notices = m_dao.findAllNotices(request);
    assertEquals(1, notices.size());

    request.Predicate = yarnPredicate;
    notices = m_dao.findAllNotices(request);
    assertEquals(1, notices.size());

    request.Predicate = adminPredicate;
    notices = m_dao.findAllNotices(request);
    assertEquals(1, notices.size());

    request.Predicate = adminOrOperatorPredicate;
    notices = m_dao.findAllNotices(request);
    assertEquals(2, notices.size());

    request.Predicate = pendingPredicate;
    notices = m_dao.findAllNotices(request);
    assertEquals(1, notices.size());

    noticeIdPredicate = new PredicateBuilder().property(
        AlertNoticeResourceProvider.ALERT_NOTICE_ID).equals(
        notices.get(0).getNotificationId()).toPredicate();

    request.Predicate = noticeIdPredicate;
    notices = m_dao.findAllNotices(request);
    assertEquals(1, notices.size());
  }

  /**
   * @return
   */
  private List<AlertDefinitionEntity> createDefinitions() throws Exception {
    for (int i = 0; i < 8; i++) {
      AlertDefinitionEntity definition = new AlertDefinitionEntity();
      definition.setDefinitionName("Alert Definition " + i);
      definition.setServiceName("HDFS");
      definition.setComponentName(null);
      definition.setClusterId(m_clusterId);
      definition.setHash(UUID.randomUUID().toString());
      definition.setScheduleInterval(60);
      definition.setScope(Scope.SERVICE);
      definition.setSource("{\"type\" : \"SCRIPT\"}");
      definition.setSourceType(SourceType.SCRIPT);
      m_definitionDao.create(definition);
    }

    List<AlertDefinitionEntity> alertDefinitions = m_definitionDao.findAll();
    assertEquals(8, alertDefinitions.size());
    return alertDefinitions;
  }

  /**
   * @return
   * @throws Exception
   */
  private Set<AlertTargetEntity> createTargets() throws Exception {
    Set<AlertTargetEntity> targets = new HashSet<AlertTargetEntity>();
    for (int i = 0; i < 5; i++) {
      AlertTargetEntity target = new AlertTargetEntity();
      target.setDescription("Target Description " + i);
      target.setNotificationType("EMAIL");
      target.setProperties("Target Properties " + i);
      target.setTargetName("Target Name " + i);
      m_dao.create(target);
      targets.add(target);
    }

    return targets;
  }

  private Cluster initializeNewCluster() throws Exception {
    String clusterName = "cluster-" + System.currentTimeMillis();
    m_clusters.addCluster(clusterName);

    Cluster cluster = m_clusters.getCluster(clusterName);
    cluster.setDesiredStackVersion(new StackId("HDP", "2.0.6"));

    addHost();
    m_clusters.mapHostToCluster(HOSTNAME, cluster.getClusterName());

    installHdfsService(cluster);
    return cluster;
  }

  /**
   * @throws Exception
   */
  private void addHost() throws Exception {
    m_clusters.addHost(HOSTNAME);

    Host host = m_clusters.getHost(HOSTNAME);
    Map<String, String> hostAttributes = new HashMap<String, String>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "6.4");
    host.setHostAttributes(hostAttributes);
    host.setState(HostState.HEALTHY);
    host.persist();
  }

  /**
   * Calls {@link Service#persist()} to mock a service install along with
   * creating a single {@link Host} and {@link ServiceComponentHost}.
   */
  private void installHdfsService(Cluster cluster) throws Exception {
    String serviceName = "HDFS";
    Service service = m_serviceFactory.createNew(cluster, serviceName);
    cluster.addService(service);
    service.persist();
    service = cluster.getService(serviceName);
    Assert.assertNotNull(service);

    ServiceComponent datanode = m_componentFactory.createNew(service,
        "DATANODE");

    service.addServiceComponent(datanode);
    datanode.setDesiredState(State.INSTALLED);
    datanode.persist();

    ServiceComponentHost sch = m_schFactory.createNew(datanode, HOSTNAME);

    datanode.addServiceComponentHost(sch);
    sch.setDesiredState(State.INSTALLED);
    sch.setState(State.INSTALLED);
    sch.setDesiredStackVersion(new StackId("HDP-2.0.6"));
    sch.setStackVersion(new StackId("HDP-2.0.6"));

    sch.persist();

    ServiceComponent namenode = m_componentFactory.createNew(service,
        "NAMENODE");

    service.addServiceComponent(namenode);
    namenode.setDesiredState(State.INSTALLED);
    namenode.persist();

    sch = m_schFactory.createNew(namenode, HOSTNAME);
    namenode.addServiceComponentHost(sch);
    sch.setDesiredState(State.INSTALLED);
    sch.setState(State.INSTALLED);
    sch.setDesiredStackVersion(new StackId("HDP-2.0.6"));
    sch.setStackVersion(new StackId("HDP-2.0.6"));

    sch.persist();
  }
}
