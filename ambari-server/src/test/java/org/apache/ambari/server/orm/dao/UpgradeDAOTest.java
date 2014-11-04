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
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.ambari.server.controller.RootServiceResponseFactory;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.entities.AlertCurrentEntity;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertGroupEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.orm.entities.AlertNoticeEntity;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.orm.entities.UpgradeItemEntity;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.NotificationState;
import org.apache.ambari.server.state.UpgradeState;
import org.apache.ambari.server.state.alert.Scope;
import org.apache.ambari.server.state.alert.SourceType;
import org.junit.After;
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

  private OrmTestHelper helper;

  /**
   *
   */
  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);

    dao = injector.getInstance(UpgradeDAO.class);
    helper = injector.getInstance(OrmTestHelper.class);
    clusterId = helper.createCluster();


    // create upgrade entities
    UpgradeEntity entity = new UpgradeEntity();
    dao.create(entity);

    // create 2 items
    List<UpgradeItemEntity> items = new ArrayList<UpgradeItemEntity>();
    UpgradeItemEntity item = new UpgradeItemEntity();
    item.setId(Long.valueOf(entity.getId().longValue() + 1000));
    item.setState(UpgradeState.IN_PROGRESS);
    items.add(item);

    item = new UpgradeItemEntity();
    item.setId(Long.valueOf(entity.getId().longValue() + 1001));
    item.setState(UpgradeState.PENDING);
    items.add(item);

    entity.setUpgradeItems(items);
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
  public void test() throws Exception {
    List<UpgradeEntity> items = dao.findUpgrades(clusterId.longValue());
    assertTrue(items.size() > 0);

    UpgradeEntity entity = dao.findUpgrade(items.get(0).getId().longValue());
    assertNotNull(entity);
  }

}
