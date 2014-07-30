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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertGroupEntity;
import org.apache.ambari.server.orm.entities.AlertTargetEntity;
import org.apache.ambari.server.state.alert.Scope;
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

  Long clusterId;
  Injector injector;
  AlertDispatchDAO dao;
  AlertDefinitionDAO definitionDao;
  OrmTestHelper helper;

  /**
   * 
   */
  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    dao = injector.getInstance(AlertDispatchDAO.class);
    definitionDao = injector.getInstance(AlertDefinitionDAO.class);
    helper = injector.getInstance(OrmTestHelper.class);

    clusterId = helper.createCluster();
    Set<AlertTargetEntity> targets = createTargets();

    for (int i = 0; i < 10; i++) {
      AlertGroupEntity group = new AlertGroupEntity();
      group.setDefault(false);
      group.setGroupName("Group Name " + i);
      group.setClusterId(clusterId);
      for (AlertTargetEntity alertTarget : targets) {
        group.addAlertTarget(alertTarget);
      }

      dao.create(group);
    }
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
   * 
   */
  @Test
  public void testFindAllTargets() throws Exception {
    List<AlertTargetEntity> targets = dao.findAllTargets();
    assertNotNull(targets);
    assertEquals(5, targets.size());
  }

  /**
   * 
   */
  @Test
  public void testFindTargetByName() throws Exception {
    List<AlertTargetEntity> targets = dao.findAllTargets();
    assertNotNull(targets);
    AlertTargetEntity target = targets.get(3);

    AlertTargetEntity actual = dao.findTargetByName(target.getTargetName());
    assertEquals(target, actual);
  }

  /**
   * 
   */
  @Test
  public void testFindAllGroups() throws Exception {
    List<AlertGroupEntity> groups = dao.findAllGroups();
    assertNotNull(groups);
    assertEquals(10, groups.size());
  }

  /**
   * 
   */
  @Test
  public void testFindGroupByName() throws Exception {
    List<AlertGroupEntity> groups = dao.findAllGroups();
    assertNotNull(groups);
    AlertGroupEntity group = groups.get(3);

    AlertGroupEntity actual = dao.findGroupByName(group.getClusterId(),
        group.getGroupName());

    assertEquals(group, actual);
  }

  /**
   * 
   */
  @Test
  public void testCreateGroup() throws Exception {
    AlertTargetEntity target = helper.createAlertTarget();
    Set<AlertTargetEntity> targets = new HashSet<AlertTargetEntity>();
    targets.add(target);

    AlertGroupEntity group = helper.createAlertGroup(clusterId, targets);
    AlertGroupEntity actual = dao.findGroupById(group.getGroupId());
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

    AlertGroupEntity group = helper.createAlertGroup(clusterId, null);

    group = dao.findGroupById(group.getGroupId());
    assertNotNull(group);

    group.getAlertDefinitions().addAll(definitions);
    dao.merge(group);

    group = dao.findGroupByName(group.getGroupName());
    assertEquals(definitions.size(), group.getAlertDefinitions().size());

    for (AlertDefinitionEntity definition : definitions) {
      assertTrue(group.getAlertDefinitions().contains(definition));
    }

    definitionDao.refresh(definitions.get(0));
    definitionDao.remove(definitions.get(0));
    definitions.remove(0);

    group = dao.findGroupByName(group.getGroupName());
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
    int targetCount = dao.findAllTargets().size();

    AlertTargetEntity target = helper.createAlertTarget();
    Set<AlertTargetEntity> targets = new HashSet<AlertTargetEntity>();
    targets.add(target);

    AlertGroupEntity group = helper.createAlertGroup(clusterId, targets);
    AlertTargetEntity actual = dao.findTargetById(target.getTargetId());
    assertNotNull(actual);

    assertEquals(target.getTargetName(), actual.getTargetName());
    assertEquals(target.getDescription(), actual.getDescription());
    assertEquals(target.getNotificationType(), actual.getNotificationType());
    assertEquals(target.getProperties(), actual.getProperties());

    assertNotNull(actual.getAlertGroups());
    Iterator<AlertGroupEntity> iterator = actual.getAlertGroups().iterator();
    AlertGroupEntity actualGroup = iterator.next();

    assertEquals(group, actualGroup);

    assertEquals(targetCount + 1, dao.findAllTargets().size());
  }

  /**
   * 
   */
  @Test
  public void testDeleteGroup() throws Exception {
    int targetCount = dao.findAllTargets().size();    
    
    AlertGroupEntity group = helper.createAlertGroup(clusterId, null);
    AlertTargetEntity target = helper.createAlertTarget();
    assertEquals(targetCount + 1, dao.findAllTargets().size());

    group = dao.findGroupById(group.getGroupId());
    assertNotNull(group);
    assertNotNull(group.getAlertTargets());
    assertEquals(0, group.getAlertTargets().size());

    group.addAlertTarget(target);
    dao.merge(group);

    group = dao.findGroupById(group.getGroupId());
    assertNotNull(group);
    assertNotNull(group.getAlertTargets());
    assertEquals(1, group.getAlertTargets().size());

    dao.remove(group);
    group = dao.findGroupById(group.getGroupId());
    assertNull(group);

    target = dao.findTargetById(target.getTargetId());
    assertNotNull(target);
    assertEquals(targetCount + 1, dao.findAllTargets().size());
  }

  /**
   * 
   */
  @Test
  public void testDeleteTarget() throws Exception {
    AlertTargetEntity target = helper.createAlertTarget();
    target = dao.findTargetById(target.getTargetId());
    assertNotNull(target);

    dao.remove(target);

    target = dao.findTargetById(target.getTargetId());
    assertNull(target);
  }

  /**
   * 
   */
  @Test
  public void testDeleteAssociatedTarget() throws Exception {
    AlertTargetEntity target = helper.createAlertTarget();
    Set<AlertTargetEntity> targets = new HashSet<AlertTargetEntity>();
    targets.add(target);

    AlertGroupEntity group = helper.createAlertGroup(clusterId, targets);
    assertEquals(1, group.getAlertTargets().size());

    target = dao.findTargetById(target.getTargetId());
    dao.refresh(target);

    assertNotNull(target);
    assertEquals(1, target.getAlertGroups().size());

    dao.remove(target);
    target = dao.findTargetById(target.getTargetId());
    assertNull(target);

    group = dao.findGroupById(group.getGroupId());
    assertNotNull(group);

    assertEquals(0, group.getAlertTargets().size());
  }

  /**
   * 
   */
  @Test
  public void testUpdateGroup() throws Exception {
    AlertTargetEntity target = helper.createAlertTarget();
    Set<AlertTargetEntity> targets = new HashSet<AlertTargetEntity>();
    targets.add(target);

    String groupName = "Group Name " + System.currentTimeMillis();

    AlertGroupEntity group = helper.createAlertGroup(clusterId, null);

    group = dao.findGroupById(group.getGroupId());
    group.setGroupName(groupName + "FOO");
    group.setDefault(true);

    dao.merge(group);
    group = dao.findGroupById(group.getGroupId());

    assertEquals(groupName + "FOO", group.getGroupName());
    assertEquals(true, group.isDefault());
    assertNotNull(group.getAlertDefinitions());
    assertNotNull(group.getAlertTargets());
    assertEquals(0, group.getAlertDefinitions().size());
    assertEquals(0, group.getAlertTargets().size());

    group.addAlertTarget(target);
    dao.merge(group);

    group = dao.findGroupById(group.getGroupId());
    assertEquals(targets, group.getAlertTargets());
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
      definition.setClusterId(clusterId);
      definition.setHash(UUID.randomUUID().toString());
      definition.setScheduleInterval(60);
      definition.setScope(Scope.SERVICE);
      definition.setSource("Source " + i);
      definition.setSourceType("SCRIPT");
      definitionDao.create(definition);
    }

    List<AlertDefinitionEntity> alertDefinitions = definitionDao.findAll();
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
      dao.create(target);
      targets.add(target);
    }

    return targets;
  }
}
