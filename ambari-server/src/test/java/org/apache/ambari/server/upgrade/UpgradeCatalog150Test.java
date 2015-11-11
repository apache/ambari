/*
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
package org.apache.ambari.server.upgrade;

import javax.persistence.EntityManager;

import junit.framework.Assert;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.HostComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.KeyValueDAO;
import org.apache.ambari.server.orm.dao.ServiceComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.ClusterConfigEntity;
import org.apache.ambari.server.orm.entities.ClusterConfigMappingEntity;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.KeyValueEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.state.HostComponentAdminState;
import org.apache.ambari.server.state.StackId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

import java.util.ArrayList;
import java.util.Collections;

public class UpgradeCatalog150Test {
  private Injector injector;
  private final String CLUSTER_NAME = "c1";
  private final String HOST_NAME = "h1";

  public static final StackId DESIRED_STACK = new StackId("HDP", "1.3.4");

  private UpgradeCatalogHelper upgradeCatalogHelper;
  private StackEntity desiredStackEntity;

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    upgradeCatalogHelper = injector.getInstance(UpgradeCatalogHelper.class);

    // inject AmbariMetaInfo to ensure that stacks get populated in the DB
    injector.getInstance(AmbariMetaInfo.class);

    // load the stack entity
    StackDAO stackDAO = injector.getInstance(StackDAO.class);

    desiredStackEntity = stackDAO.find(DESIRED_STACK.getStackName(),
        DESIRED_STACK.getStackVersion());

    Assert.assertNotNull(desiredStackEntity);
  }

  @After
  public void tearDown() {
    injector.getInstance(PersistService.class).stop();
  }

  protected void executeInTransaction(Runnable func) {
    EntityManager entityManager = injector.getProvider(EntityManager.class).get();
    if (entityManager.getTransaction().isActive()) { //already started, reuse
      func.run();
    } else {
      entityManager.getTransaction().begin();
      try {
        func.run();
        entityManager.getTransaction().commit();
      } catch (Exception e) {
        //LOG.error("Error in transaction ", e);
        if (entityManager.getTransaction().isActive()) {
          entityManager.getTransaction().rollback();
        }
        throw new RuntimeException(e);
      }

    }
  }

  @Test
  public void testAddHistoryServer() throws AmbariException {
    final ClusterEntity clusterEntity = upgradeCatalogHelper.createCluster(
        injector, CLUSTER_NAME, desiredStackEntity);
    final ClusterServiceEntity clusterServiceEntityMR = upgradeCatalogHelper.addService(
        injector, clusterEntity, "MAPREDUCE", desiredStackEntity);
    final HostEntity hostEntity = upgradeCatalogHelper.createHost(injector,
        clusterEntity, HOST_NAME);

    executeInTransaction(new Runnable() {
      @Override
      public void run() {
        upgradeCatalogHelper.addComponent(injector, clusterEntity,
            clusterServiceEntityMR, hostEntity, "JOBTRACKER",
            desiredStackEntity);
      }
    });

    UpgradeCatalog150 upgradeCatalog150 = injector.getInstance(UpgradeCatalog150.class);
    upgradeCatalog150.addHistoryServer();
  }

  @Test
  public void testProcessDecommissionedDatanodes() throws Exception {
    ClusterEntity clusterEntity = upgradeCatalogHelper.createCluster(injector,
        CLUSTER_NAME, desiredStackEntity);
    ClusterServiceEntity clusterServiceEntity = upgradeCatalogHelper.createService(
        injector, clusterEntity, "HDFS");
    HostEntity hostEntity = upgradeCatalogHelper.createHost(injector,
        clusterEntity, HOST_NAME);

    ServiceComponentDesiredStateEntity componentDesiredStateEntity =
      new ServiceComponentDesiredStateEntity();
    componentDesiredStateEntity.setClusterId(clusterEntity.getClusterId());
    componentDesiredStateEntity.setServiceName(clusterServiceEntity.getServiceName());
    componentDesiredStateEntity.setClusterServiceEntity(clusterServiceEntity);
    componentDesiredStateEntity.setComponentName("DATANODE");
    componentDesiredStateEntity.setDesiredStack(desiredStackEntity);

    ServiceComponentDesiredStateDAO componentDesiredStateDAO =
      injector.getInstance(ServiceComponentDesiredStateDAO.class);

    componentDesiredStateDAO.create(componentDesiredStateEntity);

    HostComponentDesiredStateDAO hostComponentDesiredStateDAO =
      injector.getInstance(HostComponentDesiredStateDAO.class);

    HostComponentDesiredStateEntity hostComponentDesiredStateEntity =
      new HostComponentDesiredStateEntity();

    hostComponentDesiredStateEntity.setClusterId(clusterEntity.getClusterId());
    hostComponentDesiredStateEntity.setComponentName("DATANODE");
    hostComponentDesiredStateEntity.setAdminState(HostComponentAdminState.INSERVICE);
    hostComponentDesiredStateEntity.setServiceName(clusterServiceEntity.getServiceName());
    hostComponentDesiredStateEntity.setServiceComponentDesiredStateEntity(componentDesiredStateEntity);
    hostComponentDesiredStateEntity.setHostEntity(hostEntity);
    hostComponentDesiredStateEntity.setDesiredStack(desiredStackEntity);
    componentDesiredStateEntity.setHostComponentDesiredStateEntities(
      Collections.singletonList(hostComponentDesiredStateEntity));

    hostComponentDesiredStateDAO.create(hostComponentDesiredStateEntity);

    HostComponentDesiredStateEntity entity = hostComponentDesiredStateDAO.findAll().get(0);

    Assert.assertEquals(HostComponentAdminState.INSERVICE.name(), entity.getAdminState().name());

    KeyValueDAO keyValueDAO = injector.getInstance(KeyValueDAO.class);
    KeyValueEntity keyValueEntity = new KeyValueEntity();
    keyValueEntity.setKey("decommissionDataNodesTag");
    keyValueEntity.setValue("1394147791230");
    keyValueDAO.create(keyValueEntity);

    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    ClusterConfigEntity configEntity = new ClusterConfigEntity();
    configEntity.setClusterEntity(clusterEntity);
    configEntity.setClusterId(clusterEntity.getClusterId());
    configEntity.setType("hdfs-exclude-file");
    configEntity.setTag("1394147791230");
    configEntity.setData("{\"datanodes\":\"" + HOST_NAME + "\"}");
    configEntity.setTimestamp(System.currentTimeMillis());
    configEntity.setStack(desiredStackEntity);
    configEntity.setStack(clusterEntity.getDesiredStack());

    clusterDAO.createConfig(configEntity);

    UpgradeCatalog150 upgradeCatalog150 = injector.getInstance(UpgradeCatalog150.class);

    upgradeCatalog150.processDecommissionedDatanodes();

    entity = hostComponentDesiredStateDAO.findAll().get(0);

    Assert.assertEquals(HostComponentAdminState.DECOMMISSIONED.name(), entity.getAdminState().name());

    keyValueEntity = keyValueDAO.findByKey("decommissionDataNodesTag");
    Assert.assertNull(keyValueEntity);
    keyValueEntity = keyValueDAO.findByKey("decommissionDataNodesTag-Moved");
    Assert.assertNotNull(keyValueEntity);
    Assert.assertEquals("1394147791230", keyValueEntity.getValue());
  }

  @Test
  public void testAddMissingLog4jConfigs() throws Exception {
    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);

    ClusterEntity clusterEntity = upgradeCatalogHelper.createCluster(injector,
        CLUSTER_NAME, desiredStackEntity);

    ClusterServiceEntity clusterServiceEntityMR = upgradeCatalogHelper.addService(
        injector, clusterEntity, "HDFS", desiredStackEntity);

    Long clusterId = clusterEntity.getClusterId();

    ClusterConfigEntity configEntity = clusterDAO.findConfig(clusterId, "hdfs-log4j", "version1");
    Assert.assertNull(configEntity);

    for (ClusterConfigMappingEntity ccme : clusterEntity.getConfigMappingEntities()) {
      if ("hdfs-log4j".equals(ccme.getType())) {
        Assert.fail();
      }
    }

    UpgradeCatalog150 upgradeCatalog150 = injector.getInstance(UpgradeCatalog150.class);
    upgradeCatalog150.addMissingLog4jConfigs();

    configEntity = clusterDAO.findConfig(clusterId, "hdfs-log4j", "version1");
    Assert.assertNotNull(configEntity);

    //Get updated cluster
    clusterEntity = clusterDAO.findById(1L);

    boolean failFlag = true;
    for (ClusterConfigMappingEntity ccme : clusterEntity.getConfigMappingEntities()) {
      if ("hdfs-log4j".equals(ccme.getType())) {
        failFlag = false;
      }
    }
    Assert.assertFalse(failFlag);
  }

  @Test
  public void testGetSourceVersion() {
    UpgradeCatalog150 upgradeCatalog150 = injector.getInstance(UpgradeCatalog150.class);
    Assert.assertNull(upgradeCatalog150.getSourceVersion());
  }
}
