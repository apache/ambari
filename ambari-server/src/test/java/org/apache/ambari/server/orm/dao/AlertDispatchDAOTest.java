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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.entities.AlertGroupEntity;
import org.apache.ambari.server.orm.entities.AlertTargetEntity;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

/**
 * Tests {@link AlertDispatchDAO}.
 */
public class AlertDispatchDAOTest {

  static Long clusterId;
  static Injector injector;
  AlertDispatchDAO dao;

  @BeforeClass
  public static void beforeClass() {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    clusterId = injector.getInstance(OrmTestHelper.class).createCluster();
    AlertDispatchDAO alertDispatchDAO = injector.getInstance(AlertDispatchDAO.class);

    Set<AlertTargetEntity> targets = new HashSet<AlertTargetEntity>();
    for (int i = 0; i < 5; i++) {
      AlertTargetEntity target = new AlertTargetEntity();
      target.setDescription("Target Description " + i);
      target.setNotificationType("EMAIL");
      target.setProperties("Target Properties " + i);
      target.setTargetName("Target Name " + i);
      alertDispatchDAO.create(target);
      targets.add(target);
    }

    for (int i = 0; i < 10; i++) {
      AlertGroupEntity group = new AlertGroupEntity();
      group.setDefault(false);
      group.setGroupName("Group Name " + i);
      group.setClusterId(clusterId);
      group.setAlertTargets(targets);
      alertDispatchDAO.create(group);
    }

  }

  /**
   * 
   */
  @AfterClass
  public static void afterClass() {
    injector.getInstance(PersistService.class).stop();
    injector = null;
  }

  /**
   * 
   */
  @Before
  public void setup() {
    dao = new AlertDispatchDAO();
    injector.injectMembers(dao);
  }

  /**
   * 
   */
  @Test
  public void testFindAllTargets() {
    List<AlertTargetEntity> targets = dao.findAllTargets();
    Assert.assertNotNull(targets);
    Assert.assertEquals(5, targets.size());
  }

  /**
   * 
   */
  @Test
  public void testFindTargetByName() {
    List<AlertTargetEntity> targets = dao.findAllTargets();
    Assert.assertNotNull(targets);
    AlertTargetEntity target = targets.get(3);

    AlertTargetEntity actual = dao.findTargetByName(target.getTargetName());
    Assert.assertEquals(target, actual);
  }

  /**
   * 
   */
  @Test
  public void testFindAllGroups() {
    List<AlertGroupEntity> groups = dao.findAllGroups();
    Assert.assertNotNull(groups);
    Assert.assertEquals(10, groups.size());
  }

  /**
   * 
   */
  @Test
  public void testFindGroupByName() {
    List<AlertGroupEntity> groups = dao.findAllGroups();
    Assert.assertNotNull(groups);
    AlertGroupEntity group = groups.get(3);

    AlertGroupEntity actual = dao.findGroupByName(group.getClusterId(),
        group.getGroupName());

    Assert.assertEquals(group, actual);
  }

}
