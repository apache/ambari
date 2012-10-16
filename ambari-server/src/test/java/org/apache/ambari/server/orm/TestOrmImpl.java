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
import com.google.inject.persist.jpa.JpaPersistModule;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ClusterServiceDAO;
import org.apache.ambari.server.orm.dao.ServiceConfigDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.orm.entities.ServiceConfigEntity;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.RollbackException;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class TestOrmImpl extends Assert {
  private final static Log log = LogFactory.getLog(TestOrmImpl.class);

  private static Injector injector;

  @BeforeClass
  public static void setUpClass() throws Exception {
    injector = Guice.createInjector(new JpaPersistModule("ambari-javadb")); //used for injecting in-memory DB EntityManager
//    injector = Guice.createInjector(new JpaPersistModule("ambari-postgres")); //for injecting
    injector.getInstance(GuiceJpaInitializer.class); //needed by Guice-persist to work
    injector.getInstance(OrmTestHelper.class).createDefaultData();
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
    ServiceConfigDAO serviceConfigDAO = injector.getInstance(ServiceConfigDAO.class);
    Date currentTime = new Date();
    String serviceName = "MapReduce1";
    String clusterName = "test_cluster1";

    createServiceAndConfig(currentTime, serviceName, clusterName);



    List<ServiceConfigEntity> result = serviceConfigDAO.findAll();
    assertNotNull("inserted config not found", result);
    assertFalse(result.isEmpty());
    ClusterServiceEntity clusterServiceEntity = clusterServiceDAO.findByClusterAndServiceNames(clusterName, serviceName);

    result = serviceConfigDAO.findByClusterService(clusterServiceEntity);

    assertNotNull("config by ClusterService not found", result);
    assertEquals("wrong number of configs", 1, result.size());

    ServiceConfigEntity serviceConfigEntity = result.iterator().next();

    log.info("config version = " + serviceConfigEntity.getConfigVersion());
    assertNotNull("config version is null", serviceConfigEntity.getConfigVersion());

    serviceConfigDAO.remove(serviceConfigEntity);

    result = serviceConfigDAO.findAll();


    assertTrue(result.isEmpty());

    clusterServiceDAO.remove(clusterServiceEntity);

    assertNull(clusterServiceDAO.findByClusterAndServiceNames(clusterName, serviceName));

  }

  private void createServiceAndConfig(Date currentTime, String serviceName, String clusterName) {
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

    ServiceConfigEntity serviceConfigEntity = new ServiceConfigEntity();
    serviceConfigEntity.setConfigSnapshotTime(currentTime);
    serviceConfigEntity.setClusterServiceEntity(clusterServiceEntity);

    Collection<ServiceConfigEntity> list = clusterServiceEntity.getServiceConfigEntities();
    list.add(serviceConfigEntity);
    clusterServiceEntity.setServiceConfigEntities(list);

    ServiceConfigDAO serviceConfigDAO = injector.getInstance(ServiceConfigDAO.class);

    serviceConfigDAO.create(serviceConfigEntity);
    clusterServiceDAO.merge(clusterServiceEntity);
  }

  /**
   * to clarify: are cascade operations allowed?
   */
  @Test(expected = RollbackException.class)
  public void testCascadeRemoveFail() {
    ClusterServiceDAO clusterServiceDAO = injector.getInstance(ClusterServiceDAO.class);
    Date currentTime = new Date();
    String serviceName = "MapReduce2";
    String clusterName = "test_cluster1";

    createServiceAndConfig(currentTime, serviceName, clusterName);

    ClusterServiceEntity clusterServiceEntity = clusterServiceDAO.findByClusterAndServiceNames(clusterName, serviceName);
    clusterServiceDAO.remove(clusterServiceEntity);
  }

}
