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

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

import junit.framework.Assert;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ClusterServiceDAO;
import org.apache.ambari.server.orm.dao.HostComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.KeyValueDAO;
import org.apache.ambari.server.orm.entities.ClusterConfigEntity;
import org.apache.ambari.server.orm.entities.ClusterConfigEntityPK;
import org.apache.ambari.server.orm.entities.ClusterConfigMappingEntity;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.KeyValueEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.ServiceDesiredStateEntity;
import org.apache.ambari.server.state.HostComponentAdminState;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;

import javax.persistence.EntityManager;

public class UpgradeCatalog150Test {
  private Injector injector;
  private final String CLUSTER_NAME = "c1";
  private final String SERVICE_NAME = "HDFS";
  private final String HOST_NAME = "h1";
  private final String DESIRED_STACK_VERSION = "{\"stackName\":\"HDP\",\"stackVersion\":\"1.3.4\"}";

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
  }

  @After
  public void tearDown() {
    injector.getInstance(PersistService.class).stop();
  }

  private ClusterEntity createCluster() {
    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    ClusterEntity clusterEntity = new ClusterEntity();
    clusterEntity.setClusterId(1L);
    clusterEntity.setClusterName(CLUSTER_NAME);
    clusterEntity.setDesiredStackVersion(DESIRED_STACK_VERSION);
    clusterDAO.create(clusterEntity);
    return clusterEntity;
  }

  private ClusterServiceEntity createService(ClusterEntity clusterEntity) {
    ClusterServiceDAO clusterServiceDAO = injector.getInstance(ClusterServiceDAO.class);
    ClusterServiceEntity clusterServiceEntity = new ClusterServiceEntity();
    clusterServiceEntity.setClusterId(1L);
    clusterServiceEntity.setClusterEntity(clusterEntity);
    clusterServiceEntity.setServiceName(SERVICE_NAME);
    clusterServiceDAO.create(clusterServiceEntity);
    return clusterServiceEntity;
  }
  
  private ClusterServiceEntity addService(ClusterEntity clusterEntity, String serviceName) {
    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    
    ClusterServiceEntity clusterServiceEntity = new ClusterServiceEntity();
    clusterServiceEntity.setClusterEntity(clusterEntity);
    clusterServiceEntity.setServiceName(serviceName);
    
    ServiceDesiredStateEntity serviceDesiredStateEntity = new ServiceDesiredStateEntity();
    serviceDesiredStateEntity.setDesiredStackVersion(DESIRED_STACK_VERSION);
    serviceDesiredStateEntity.setClusterServiceEntity(clusterServiceEntity);
    
    clusterServiceEntity.setServiceDesiredStateEntity(serviceDesiredStateEntity);
    clusterEntity.getClusterServiceEntities().add(clusterServiceEntity);
    
    clusterDAO.merge(clusterEntity);
    
    return clusterServiceEntity;
  }


  private HostEntity createHost(ClusterEntity clusterEntity) {
    HostDAO hostDAO = injector.getInstance(HostDAO.class);
    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    HostEntity hostEntity = new HostEntity();
    hostEntity.setHostName(HOST_NAME);
    hostEntity.setClusterEntities(Collections.singletonList(clusterEntity));
    hostDAO.create(hostEntity);
    clusterEntity.getHostEntities().add(hostEntity);
    clusterDAO.merge(clusterEntity);
    return hostEntity;
  }
  
  private void addComponent(ClusterEntity clusterEntity, ClusterServiceEntity clusterServiceEntity, HostEntity hostEntity, String componentName) {
    ServiceComponentDesiredStateEntity componentDesiredStateEntity = new ServiceComponentDesiredStateEntity();
    componentDesiredStateEntity.setClusterServiceEntity(clusterServiceEntity);
    componentDesiredStateEntity.setComponentName(componentName);
    componentDesiredStateEntity.setHostComponentStateEntities(new ArrayList<HostComponentStateEntity>());
    
    HostComponentDesiredStateEntity hostComponentDesiredStateEntity = new HostComponentDesiredStateEntity();
    hostComponentDesiredStateEntity.setAdminState(HostComponentAdminState.INSERVICE);
    hostComponentDesiredStateEntity.setServiceComponentDesiredStateEntity(componentDesiredStateEntity);
    hostComponentDesiredStateEntity.setHostEntity(hostEntity);
    
    HostComponentStateEntity hostComponentStateEntity = new HostComponentStateEntity();
    hostComponentStateEntity.setHostEntity(hostEntity);
    hostComponentStateEntity.setHostName(hostEntity.getHostName());
    hostComponentStateEntity.setCurrentStackVersion(clusterEntity.getDesiredStackVersion());
    hostComponentStateEntity.setServiceComponentDesiredStateEntity(componentDesiredStateEntity);
    
    componentDesiredStateEntity.getHostComponentStateEntities().add(hostComponentStateEntity);
    componentDesiredStateEntity.setHostComponentDesiredStateEntities(new ArrayList<HostComponentDesiredStateEntity>());
    componentDesiredStateEntity.getHostComponentDesiredStateEntities().add(hostComponentDesiredStateEntity);
    
    hostEntity.getHostComponentStateEntities().add(hostComponentStateEntity);
    hostEntity.getHostComponentDesiredStateEntities().add(hostComponentDesiredStateEntity);
    clusterServiceEntity.getServiceComponentDesiredStateEntities().add(componentDesiredStateEntity);
    
    ClusterServiceDAO clusterServiceDAO = injector.getInstance(ClusterServiceDAO.class);
    clusterServiceDAO.merge(clusterServiceEntity);
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
    final ClusterEntity clusterEntity = createCluster();
    final ClusterServiceEntity clusterServiceEntityMR = addService(clusterEntity, "MAPREDUCE");
    final HostEntity hostEntity = createHost(clusterEntity);
    
    executeInTransaction(new Runnable() {
      @Override
      public void run() {
        addComponent(clusterEntity, clusterServiceEntityMR, hostEntity, "JOBTRACKER");
      }
    });
    
    UpgradeCatalog150 upgradeCatalog150 = injector.getInstance(UpgradeCatalog150.class);
    upgradeCatalog150.addHistoryServer();
  }

  @Test
  public void testProcessDecommissionedDatanodes() throws Exception {
    ClusterEntity clusterEntity = createCluster();
    ClusterServiceEntity clusterServiceEntity = createService(clusterEntity);
    HostEntity hostEntity = createHost(clusterEntity);

    ServiceComponentDesiredStateEntity componentDesiredStateEntity =
      new ServiceComponentDesiredStateEntity();
    componentDesiredStateEntity.setClusterId(clusterEntity.getClusterId());
    componentDesiredStateEntity.setServiceName(clusterServiceEntity.getServiceName());
    componentDesiredStateEntity.setClusterServiceEntity(clusterServiceEntity);
    componentDesiredStateEntity.setComponentName("DATANODE");

    //componentDesiredStateDAO.create(componentDesiredStateEntity);

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
    hostComponentDesiredStateEntity.setHostName(hostEntity.getHostName());

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

    ClusterEntity clusterEntity = createCluster();
    ClusterServiceEntity clusterServiceEntityMR = addService(clusterEntity, "HDFS");

    Long clusterId = clusterEntity.getClusterId();

    ClusterConfigEntityPK configEntityPK = new ClusterConfigEntityPK();
    configEntityPK.setClusterId(clusterId);
    configEntityPK.setType("hdfs-log4j");
    configEntityPK.setTag("version1");
    ClusterConfigEntity configEntity = clusterDAO.findConfig(configEntityPK);
    Assert.assertNull(configEntity);

    for (ClusterConfigMappingEntity ccme : clusterEntity.getConfigMappingEntities()) {
      if ("hdfs-log4j".equals(ccme.getType())) {
        Assert.fail();
      }
    }

    UpgradeCatalog150 upgradeCatalog150 = injector.getInstance(UpgradeCatalog150.class);
    upgradeCatalog150.addMissingLog4jConfigs();

    configEntity = clusterDAO.findConfig(configEntityPK);
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
}
