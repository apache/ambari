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
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.RequestEntity;
import org.apache.ambari.server.orm.entities.StageEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.orm.entities.UpgradeGroupEntity;
import org.apache.ambari.server.orm.entities.UpgradeItemEntity;
import org.apache.ambari.server.state.UpgradeState;
import org.apache.ambari.server.state.stack.upgrade.Direction;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

/**
 * Tests {@link AlertDefinitionDAO} for interacting with
 * {@link AlertDefinitionEntity}.
 */
public class UpgradeDAOTest {


  private Injector injector;
  private Long clusterId;
  private UpgradeDAO dao;
  private RequestDAO requestDAO;

  private OrmTestHelper helper;

  /**
   *
   */
  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);

    dao = injector.getInstance(UpgradeDAO.class);
    requestDAO = injector.getInstance(RequestDAO.class);
    helper = injector.getInstance(OrmTestHelper.class);
    clusterId = helper.createCluster();

    // create upgrade entities
    UpgradeEntity entity = new UpgradeEntity();
    entity.setClusterId(Long.valueOf(1));
    entity.setRequestId(Long.valueOf(1));
    entity.setFromVersion("");
    entity.setToVersion("");
    entity.setUpgradeType(UpgradeType.ROLLING);
    entity.setUpgradePackage("test-upgrade");
    entity.setDowngradeAllowed(true);

    UpgradeGroupEntity group = new UpgradeGroupEntity();
    group.setName("group_name");
    group.setTitle("group title");

    // create 2 items
    List<UpgradeItemEntity> items = new ArrayList<UpgradeItemEntity>();
    UpgradeItemEntity item = new UpgradeItemEntity();
    item.setState(UpgradeState.IN_PROGRESS);
    item.setStageId(Long.valueOf(1L));
    items.add(item);

    item = new UpgradeItemEntity();
    item.setState(UpgradeState.PENDING);
    item.setStageId(Long.valueOf(1L));
    items.add(item);

    group.setItems(items);
    entity.setUpgradeGroups(Collections.singletonList(group));
    dao.create(entity);
  }

  @After
  public void teardown() {
    injector.getInstance(PersistService.class).stop();
    injector = null;
  }

  @Test
  public void testFindForCluster() throws Exception {
    List<UpgradeEntity> items = dao.findUpgrades(clusterId.longValue());

    assertEquals(1, items.size());
  }

  @Test
  public void testFindUpgrade() throws Exception {
    List<UpgradeEntity> items = dao.findUpgrades(clusterId.longValue());
    assertTrue(items.size() > 0);

    UpgradeEntity entity = dao.findUpgrade(items.get(0).getId().longValue());
    assertNotNull(entity);
    assertEquals(1, entity.getUpgradeGroups().size());

    UpgradeGroupEntity group = dao.findUpgradeGroup(
        entity.getUpgradeGroups().get(0).getId().longValue());

    assertNotNull(group);
    Assert.assertNotSame(entity.getUpgradeGroups().get(0), group);
    assertEquals("group_name", group.getName());
    assertEquals("group title", group.getTitle());
  }

  /**
   * Create upgrades and downgrades and verify only latest upgrade is given
   *
   * @throws Exception
   */
  @Test
  public void testFindLastUpgradeForCluster() throws Exception {
    // create upgrade entities
    RequestEntity requestEntity = new RequestEntity();
    requestEntity.setRequestId(1L);
    requestEntity.setClusterId(1L);
    requestEntity.setStatus(HostRoleStatus.PENDING);
    requestEntity.setStages(new ArrayList<StageEntity>());
    requestDAO.create(requestEntity);

    UpgradeEntity entity1 = new UpgradeEntity();
    entity1.setId(11L);
    entity1.setClusterId(1L);
    entity1.setDirection(Direction.UPGRADE);
    entity1.setRequestId(1L);
    entity1.setFromVersion("2.2.0.0-1234");
    entity1.setToVersion("2.3.0.0-4567");
    entity1.setUpgradeType(UpgradeType.ROLLING);
    entity1.setUpgradePackage("test-upgrade");
    entity1.setDowngradeAllowed(true);
    dao.create(entity1);
    UpgradeEntity entity2 = new UpgradeEntity();
    entity2.setId(22L);
    entity2.setClusterId(1L);
    entity2.setDirection(Direction.DOWNGRADE);
    entity2.setRequestId(1L);
    entity2.setFromVersion("2.3.0.0-4567");
    entity2.setToVersion("2.2.0.0-1234");
    entity2.setUpgradeType(UpgradeType.ROLLING);
    entity2.setUpgradePackage("test-upgrade");
    entity2.setDowngradeAllowed(true);
    dao.create(entity2);
    UpgradeEntity entity3 = new UpgradeEntity();
    entity3.setId(33L);
    entity3.setClusterId(1L);
    entity3.setDirection(Direction.UPGRADE);
    entity3.setRequestId(1L);
    entity3.setFromVersion("2.2.0.0-1234");
    entity3.setToVersion("2.3.1.1-4567");
    entity3.setUpgradeType(UpgradeType.ROLLING);
    entity3.setUpgradePackage("test-upgrade");
    entity3.setDowngradeAllowed(true);
    dao.create(entity3);
    UpgradeEntity lastUpgradeForCluster = dao.findLastUpgradeForCluster(1);
    assertNotNull(lastUpgradeForCluster);
    assertEquals(33L, (long)lastUpgradeForCluster.getId());
  }

  /**
   * Tests that certain columns in an {@link UpgradeEntity} are updatable.
   *
   * @throws Exception
   */
  @Test
  public void testUpdatableColumns() throws Exception {
    RequestEntity requestEntity = new RequestEntity();
    requestEntity.setRequestId(1L);
    requestEntity.setClusterId(1L);
    requestEntity.setStatus(HostRoleStatus.PENDING);
    requestEntity.setStages(new ArrayList<StageEntity>());
    requestDAO.create(requestEntity);
    
    UpgradeEntity upgradeEntity = new UpgradeEntity();
    upgradeEntity.setId(11L);
    upgradeEntity.setClusterId(1L);
    upgradeEntity.setDirection(Direction.UPGRADE);
    upgradeEntity.setRequestId(1L);
    upgradeEntity.setFromVersion("2.2.0.0-1234");
    upgradeEntity.setToVersion("2.3.0.0-4567");
    upgradeEntity.setUpgradeType(UpgradeType.ROLLING);
    upgradeEntity.setUpgradePackage("test-upgrade");
    dao.create(upgradeEntity);

    UpgradeEntity lastUpgradeForCluster = dao.findLastUpgradeForCluster(1);
    Assert.assertFalse(lastUpgradeForCluster.isComponentFailureAutoSkipped());
    Assert.assertFalse(lastUpgradeForCluster.isServiceCheckFailureAutoSkipped());

    lastUpgradeForCluster.setAutoSkipComponentFailures(true);
    lastUpgradeForCluster.setAutoSkipServiceCheckFailures(true);
    dao.merge(lastUpgradeForCluster);

    lastUpgradeForCluster = dao.findLastUpgradeForCluster(1);
    Assert.assertTrue(lastUpgradeForCluster.isComponentFailureAutoSkipped());
    Assert.assertTrue(lastUpgradeForCluster.isServiceCheckFailureAutoSkipped());
  }
}
