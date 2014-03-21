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

package org.apache.ambari.server.orm;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.jpa.AmbariJpaPersistService;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.orm.dao.*;
import org.apache.ambari.server.orm.entities.*;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.RollbackException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class TestOrmImpl extends Assert {
  private static final Logger log = LoggerFactory.getLogger(TestOrmImpl.class);

  private static Injector injector;

  @Before
  public void setup() {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    injector.getInstance(OrmTestHelper.class).createDefaultData();
  }

  @After
  public void teardown() {
    injector.getInstance(PersistService.class).stop();
  }

  /**
   * persistence provider is responsible for returning empty collection if relation doesn't exists
   */
  @Test
  public void testEmptyPersistentCollection() {
    String testClusterName = "test_cluster2";

    ClusterEntity clusterEntity = new ClusterEntity();
    clusterEntity.setClusterName(testClusterName);
    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    clusterDAO.create(clusterEntity);
    clusterEntity = clusterDAO.findByName(clusterEntity.getClusterName());

    assertTrue("empty relation wasn't instantiated", clusterEntity.getHostEntities() != null);
  }

  /**
   * Transaction marked for rollback should not be allowed for commit
   * @throws Throwable
   */
  @Test(expected = RollbackException.class)
  public void testRollbackException() throws Throwable{
    injector.getInstance(OrmTestHelper.class).performTransactionMarkedForRollback();
  }

  /**
   * Rollback test
   */
  @Test
  public void testSafeRollback() {
    String testClusterName = "don't save";

    EntityManager entityManager = injector.getInstance(OrmTestHelper.class).getEntityManager();
    entityManager.getTransaction().begin();
    ClusterEntity clusterEntity = new ClusterEntity();
    clusterEntity.setClusterName(testClusterName);
    entityManager.persist(clusterEntity);
    entityManager.getTransaction().rollback();

    assertNull("transaction was not rolled back", injector.getInstance(ClusterDAO.class).findByName(testClusterName));
  }

  /**
   * Test auto incremented field and custom query example
   */
  @Test
  public void testAutoIncrementedField() {
    ClusterServiceDAO clusterServiceDAO = injector.getInstance(ClusterServiceDAO.class);
    Date currentTime = new Date();
    String serviceName = "MapReduce1";
    String clusterName = "test_cluster1";

    createService(currentTime, serviceName, clusterName);

    ClusterServiceEntity clusterServiceEntity = clusterServiceDAO.findByClusterAndServiceNames(clusterName, serviceName);

    clusterServiceDAO.remove(clusterServiceEntity);

    assertNull(clusterServiceDAO.findByClusterAndServiceNames(clusterName, serviceName));

  }

  private void createService(Date currentTime, String serviceName, String clusterName) {
    ClusterServiceDAO clusterServiceDAO = injector.getInstance(ClusterServiceDAO.class);
    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    ClusterEntity cluster = clusterDAO.findByName(clusterName);

    ClusterServiceEntity clusterServiceEntity = new ClusterServiceEntity();
    clusterServiceEntity.setClusterEntity(cluster);
    clusterServiceEntity.setServiceName(serviceName);

    cluster.getClusterServiceEntities().add(clusterServiceEntity);

    clusterServiceDAO.create(clusterServiceEntity);
    clusterDAO.merge(cluster);

    clusterServiceEntity = clusterServiceDAO.findByClusterAndServiceNames(clusterName, serviceName);
    assertNotNull(clusterServiceEntity);

    clusterServiceDAO.merge(clusterServiceEntity);
  }

  /**
   * to clarify: are cascade operations allowed?
   */
  @Test
  public void testCascadeRemoveFail() {
    ClusterServiceDAO clusterServiceDAO = injector.getInstance(ClusterServiceDAO.class);
    Date currentTime = new Date();
    String serviceName = "MapReduce2";
    String clusterName = "test_cluster1";

    createService(currentTime, serviceName, clusterName);

    ClusterServiceEntity clusterServiceEntity = clusterServiceDAO.findByClusterAndServiceNames(clusterName, serviceName);
    clusterServiceDAO.remove(clusterServiceEntity);
    
    Assert.assertNull(
        clusterServiceDAO.findByClusterAndServiceNames(clusterName,
            serviceName));
  }

  @Test
  public void testSortedCommands() {
    injector.getInstance(OrmTestHelper.class).createStageCommands();
    HostRoleCommandDAO hostRoleCommandDAO = injector.getInstance(HostRoleCommandDAO.class);
    HostDAO hostDAO = injector.getInstance(HostDAO.class);
    StageDAO stageDAO = injector.getInstance(StageDAO.class);

    List<HostRoleCommandEntity> list =
        hostRoleCommandDAO.findSortedCommandsByStageAndHost(
            stageDAO.findByActionId("1-1"), hostDAO.findByName("test_host1"));
    log.info("command '{}' - taskId '{}' ", list.get(0).getRoleCommand(),
        list.get(0).getTaskId());
    log.info("command '{}' - taskId '{}'", list.get(1).getRoleCommand(),
        list.get(1).getTaskId());
   assertTrue(list.get(0).getTaskId() < list.get(1).getTaskId());
  }

  @Test
  public void testFindHostsByStage() {
    injector.getInstance(OrmTestHelper.class).createStageCommands();
    HostDAO hostDAO = injector.getInstance(HostDAO.class);
    StageDAO stageDAO = injector.getInstance(StageDAO.class);
    StageEntity stageEntity = stageDAO.findByActionId("1-1");
    log.info("StageEntity {} {}" + stageEntity.getRequestId() + " "
        + stageEntity.getStageId());
    List<HostEntity> hosts = hostDAO.findByStage(stageEntity);
    assertEquals(2, hosts.size());
  }

  @Test
  public void testAbortHostRoleCommands() {
    injector.getInstance(OrmTestHelper.class).createStageCommands();
    HostRoleCommandDAO hostRoleCommandDAO = injector.getInstance(HostRoleCommandDAO.class);
    int result = hostRoleCommandDAO.updateStatusByRequestId(
        1L, HostRoleStatus.ABORTED, Arrays.asList(HostRoleStatus.QUEUED,
        HostRoleStatus.IN_PROGRESS, HostRoleStatus.PENDING));
    //result always 1 in batch mode
    List<HostRoleCommandEntity> commandEntities = hostRoleCommandDAO.findByRequest(1L);
    int count = 0;
    for (HostRoleCommandEntity commandEntity : commandEntities) {
      if (commandEntity.getStatus() == HostRoleStatus.ABORTED) {
        count++;
      }
    }
    assertEquals("Exactly two commands should be in aborted state", 2, count);
  }

  @Test
  public void testFindStageByHostRole() {
    injector.getInstance(OrmTestHelper.class).createStageCommands();
    HostRoleCommandDAO hostRoleCommandDAO = injector.getInstance(HostRoleCommandDAO.class);
    List<HostRoleCommandEntity> list = hostRoleCommandDAO.findByHostRole("test_host1", 1L, 1L, Role.DATANODE.toString());
    assertEquals(1, list.size());
  }

  @Test
  public void testLastRequestId() {
    injector.getInstance(OrmTestHelper.class).createStageCommands();
    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    StageDAO stageDAO = injector.getInstance(StageDAO.class);
    RequestDAO requestDAO = injector.getInstance(RequestDAO.class);

    RequestEntity requestEntity = requestDAO.findByPK(1L);
    List<StageEntity> stageEntities = new ArrayList<StageEntity>();

    StageEntity stageEntity = new StageEntity();
    stageEntity.setCluster(clusterDAO.findByName("test_cluster1"));
    stageEntity.setRequest(requestEntity);
    stageEntity.setStageId(2L);
    stageDAO.create(stageEntity);
    StageEntity stageEntity2 = new StageEntity();
    stageEntity2.setCluster(clusterDAO.findByName("test_cluster1"));
    stageEntity2.setRequest(requestEntity);
    stageEntity2.setRequestId(1L);
    stageEntity2.setStageId(3L);
    stageDAO.create(stageEntity2);

    stageEntities.add(stageEntity);
    stageEntities.add(stageEntity2);
    requestEntity.setStages(stageEntities);
    requestDAO.merge(requestEntity);
    assertEquals(1L, stageDAO.getLastRequestId());
  }

  @Test
  public void testConcurrentModification() throws InterruptedException {
    final ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    final AmbariJpaPersistService ambariJpaPersistService = injector.getInstance(AmbariJpaPersistService.class);
    ClusterEntity clusterEntity = new ClusterEntity();
    clusterEntity.setClusterName("cluster1");
    clusterDAO.create(clusterEntity);
//    assertFalse(ambariJpaPersistService.isWorking());

    clusterEntity = clusterDAO.findById(clusterEntity.getClusterId());
    assertEquals("cluster1", clusterEntity.getClusterName());
//    assertFalse(ambariJpaPersistService.isWorking());

    Thread thread = new Thread(){
      @Override
      public void run() {
        ClusterEntity clusterEntity1 = clusterDAO.findByName("cluster1");
        clusterEntity1.setClusterName("anotherName");
        clusterDAO.merge(clusterEntity1);
//        assertFalse(ambariJpaPersistService.isWorking());
      }
    };

    thread.start();
    thread.join();

    clusterEntity = clusterDAO.findById(clusterEntity.getClusterId());
//    assertFalse(ambariJpaPersistService.isWorking());
    assertEquals("anotherName", clusterEntity.getClusterName());

    thread = new Thread(){
      @Override
      public void run() {
        clusterDAO.removeByName("anotherName");
      }
    };

    thread.start();
    thread.join();

    assertNull(clusterDAO.findById(clusterEntity.getClusterId()));

    List<ClusterEntity> result = clusterDAO.findAll();

    thread = new Thread(){
      @Override
      public void run() {
        ClusterEntity temp = new ClusterEntity();
        temp.setClusterName("temp_cluster");
        clusterDAO.create(temp);
      }
    };

    thread.start();
    thread.join();

    assertEquals(result.size() + 1, (result = clusterDAO.findAll()).size());


    thread = new Thread(){
      @Override
      public void run() {
        ClusterEntity temp = new ClusterEntity();
        temp.setClusterName("temp_cluster2");
        clusterDAO.create(temp);
      }
    };

    thread.start();
    thread.join();

    assertEquals(result.size() + 1, (clusterDAO.findAll()).size());

  }

}
