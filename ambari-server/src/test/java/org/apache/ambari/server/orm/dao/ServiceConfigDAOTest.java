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

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;
import junit.framework.Assert;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.cache.ConfigGroupHostMapping;
import org.apache.ambari.server.orm.entities.ClusterConfigEntity;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ConfigGroupConfigMappingEntity;
import org.apache.ambari.server.orm.entities.ConfigGroupEntity;
import org.apache.ambari.server.orm.entities.ConfigGroupHostMappingEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.ServiceConfigEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ServiceConfigDAOTest {
  private Injector injector;
  private ServiceConfigDAO serviceConfigDAO;
  private ClusterDAO clusterDAO;
  private ResourceTypeDAO resourceTypeDAO;


  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);

    clusterDAO = injector.getInstance(ClusterDAO.class);
    serviceConfigDAO = injector.getInstance(ServiceConfigDAO.class);
    resourceTypeDAO = injector.getInstance(ResourceTypeDAO.class);
  }

  @After
  public void teardown() throws AmbariException {
    injector.getInstance(PersistService.class).stop();
  }

  private ServiceConfigEntity createServiceConfig(String serviceName,
         String userName, Long version, Long serviceConfigId,
         Long createTimestamp, List<ClusterConfigEntity> clusterConfigEntities)
    throws Exception {

    // create an admin resource to represent this cluster
    ResourceTypeEntity resourceTypeEntity = resourceTypeDAO.findById(ResourceTypeEntity.CLUSTER_RESOURCE_TYPE);
    if (resourceTypeEntity == null) {
      resourceTypeEntity = new ResourceTypeEntity();
      resourceTypeEntity.setId(ResourceTypeEntity.CLUSTER_RESOURCE_TYPE);
      resourceTypeEntity.setName(ResourceTypeEntity.CLUSTER_RESOURCE_TYPE_NAME);
      resourceTypeEntity = resourceTypeDAO.merge(resourceTypeEntity);
    }
    ResourceEntity resourceEntity = new ResourceEntity();
    resourceEntity.setResourceType(resourceTypeEntity);

    ClusterEntity  clusterEntity = clusterDAO.findByName("c1");
    if (clusterEntity == null) {
      clusterEntity = new ClusterEntity();
      clusterEntity.setClusterName("c1");
      clusterEntity.setResource(resourceEntity);
      clusterDAO.create(clusterEntity);
    }

    ServiceConfigEntity serviceConfigEntity = new ServiceConfigEntity();
    serviceConfigEntity.setServiceName(serviceName);
    serviceConfigEntity.setUser(userName);
    serviceConfigEntity.setVersion(version);
    serviceConfigEntity.setServiceConfigId(serviceConfigId);
    serviceConfigEntity.setClusterId(clusterEntity.getClusterId());
    serviceConfigEntity.setCreateTimestamp(createTimestamp);
    serviceConfigEntity.setClusterConfigEntities(clusterConfigEntities);
    serviceConfigEntity.setClusterEntity(clusterEntity);

    serviceConfigDAO.create(serviceConfigEntity);

    return serviceConfigEntity;
  }

  @Test
  public void testCreateServiceConfigVersion() throws Exception {

    ServiceConfigEntity serviceConfigEntity =
      createServiceConfig("HDFS", "admin", 1L, 1L, 1111L, null);

    Assert.assertNotNull(serviceConfigEntity);
    Assert.assertEquals("c1", serviceConfigEntity.getClusterEntity().getClusterName());
    Assert.assertEquals(Long.valueOf(1), serviceConfigEntity.getClusterEntity()
      .getClusterId());
    Assert.assertEquals("HDFS", serviceConfigEntity.getServiceName());
    Assert.assertEquals(Long.valueOf(1111L), serviceConfigEntity.getCreateTimestamp());
    Assert.assertEquals("admin", serviceConfigEntity.getUser());
    Assert.assertEquals(Long.valueOf(1), serviceConfigEntity.getVersion());
    Assert.assertTrue(serviceConfigEntity.getClusterConfigEntities().isEmpty());
    Assert.assertNotNull(serviceConfigEntity.getServiceConfigId());
  }

  @Test
  public void testFindServiceConfigEntity() throws Exception {
    ServiceConfigEntity sce =
      createServiceConfig("HDFS", "admin", 1L, 1L, 1111L, null);

    ServiceConfigEntity serviceConfigEntity = serviceConfigDAO.find(sce.getServiceConfigId());

    Assert.assertNotNull(serviceConfigEntity);
    Assert.assertEquals("c1", serviceConfigEntity.getClusterEntity().getClusterName());
    Assert.assertEquals(Long.valueOf(1), serviceConfigEntity.getClusterEntity()
      .getClusterId());
    Assert.assertEquals("HDFS", serviceConfigEntity.getServiceName());
    Assert.assertEquals(Long.valueOf(1111L), serviceConfigEntity.getCreateTimestamp());
    Assert.assertEquals("admin", serviceConfigEntity.getUser());
    Assert.assertEquals(Long.valueOf(1), serviceConfigEntity.getVersion());
    Assert.assertTrue(serviceConfigEntity.getClusterConfigEntities().isEmpty());
    Assert.assertNotNull(serviceConfigEntity.getServiceConfigId());
  }

  @Test
  public void testFindByServiceAndVersion() throws Exception {

    createServiceConfig("HDFS", "admin", 1L, 1L, 1111L, null);

    ServiceConfigEntity serviceConfigEntity =
      serviceConfigDAO.findByServiceAndVersion("HDFS", 1L);

    Assert.assertNotNull(serviceConfigEntity);
    Assert.assertEquals("c1", serviceConfigEntity.getClusterEntity().getClusterName());
    Assert.assertEquals(Long.valueOf(1), serviceConfigEntity.getClusterEntity()
      .getClusterId());
    Assert.assertEquals("HDFS", serviceConfigEntity.getServiceName());
    Assert.assertEquals(Long.valueOf(1111L), serviceConfigEntity.getCreateTimestamp());
    Assert.assertEquals("admin", serviceConfigEntity.getUser());
    Assert.assertEquals(Long.valueOf(1), serviceConfigEntity.getVersion());
    Assert.assertTrue(serviceConfigEntity.getClusterConfigEntities().isEmpty());
    Assert.assertNotNull(serviceConfigEntity.getServiceConfigId());
  }

  @Test
  public void testFindMaxVersions() throws Exception {
    createServiceConfig("HDFS", "admin", 1L, 1L, 1111L, null);
    createServiceConfig("HDFS", "admin", 2L, 2L, 2222L, null);
    createServiceConfig("YARN", "admin", 1L, 3L, 3333L, null);


    Map<String,Long> maxVersions = serviceConfigDAO.findMaxVersions(
      clusterDAO.findByName("c1").getClusterId());

    Assert.assertNotNull(maxVersions);

    Assert.assertEquals(2, maxVersions.size());

    Assert.assertEquals(Long.valueOf(2), maxVersions.get("HDFS"));
    Assert.assertEquals(Long.valueOf(1), maxVersions.get("YARN"));
  }

  @Test
  public void testGetLastServiceConfigs() throws Exception {
    createServiceConfig("HDFS", "admin", 1L, 1L, 1111L, null);
    createServiceConfig("HDFS", "admin", 2L, 2L, 2222L, null);
    createServiceConfig("YARN", "admin", 1L, 3L, 3333L, null);

    List<ServiceConfigEntity> serviceConfigEntities =
      serviceConfigDAO.getLastServiceConfigs(clusterDAO.findByName("c1").getClusterId());

    Assert.assertNotNull(serviceConfigEntities);
    Assert.assertEquals(2, serviceConfigEntities.size());

    for (ServiceConfigEntity sce: serviceConfigEntities) {
      if ("HDFS".equals(sce.getServiceName())) {
        Assert.assertEquals("c1", sce.getClusterEntity().getClusterName());
        Assert.assertEquals(Long.valueOf(1), sce.getClusterEntity()
          .getClusterId());
        Assert.assertEquals(Long.valueOf(2222L), sce.getCreateTimestamp());
        Assert.assertEquals(Long.valueOf(2), sce.getVersion());
        Assert.assertTrue(sce.getClusterConfigEntities().isEmpty());
        Assert.assertNotNull(sce.getServiceConfigId());
      }
      if ("YARN".equals(sce.getServiceName())) {
        Assert.assertEquals("c1", sce.getClusterEntity().getClusterName());
        Assert.assertEquals(Long.valueOf(1), sce.getClusterEntity()
          .getClusterId());
        Assert.assertEquals(Long.valueOf(3333L), sce.getCreateTimestamp());
        Assert.assertEquals(Long.valueOf(1), sce.getVersion());
        Assert.assertTrue(sce.getClusterConfigEntities().isEmpty());
        Assert.assertNotNull(sce.getServiceConfigId());
      }

      Assert.assertEquals("admin", sce.getUser());
    }
  }

  @Test
  public void testGetLastServiceConfig() throws Exception {
    createServiceConfig("HDFS", "admin", 1L, 1L, 1111L, null);
    createServiceConfig("HDFS", "admin", 2L, 2L, 2222L, null);
    createServiceConfig("YARN", "admin", 1L, 3L, 3333L, null);

    ServiceConfigEntity serviceConfigEntity =
      serviceConfigDAO.getLastServiceConfig(1L, "HDFS");

    Assert.assertNotNull(serviceConfigEntity);
    Assert.assertEquals("c1", serviceConfigEntity.getClusterEntity().getClusterName());
    Assert.assertEquals(Long.valueOf(1), serviceConfigEntity.getClusterEntity()
      .getClusterId());
    Assert.assertEquals("HDFS", serviceConfigEntity.getServiceName());
    Assert.assertEquals(Long.valueOf(2222L), serviceConfigEntity.getCreateTimestamp());
    Assert.assertEquals("admin", serviceConfigEntity.getUser());
    Assert.assertEquals(Long.valueOf(2), serviceConfigEntity.getVersion());
    Assert.assertTrue(serviceConfigEntity.getClusterConfigEntities().isEmpty());
    Assert.assertNotNull(serviceConfigEntity.getServiceConfigId());
  }

  @Test
  public void testGetServiceConfigs() throws Exception {
    createServiceConfig("HDFS", "admin", 1L, 1L, 1111L, null);
    createServiceConfig("HDFS", "admin", 2L, 2L, 2222L, null);
    createServiceConfig("YARN", "admin", 1L, 3L, 3333L, null);

    List<ServiceConfigEntity> serviceConfigEntities =
      serviceConfigDAO.getServiceConfigs(clusterDAO.findByName("c1").getClusterId());

    Assert.assertNotNull(serviceConfigEntities);
    Assert.assertEquals(3, serviceConfigEntities.size());

    for (ServiceConfigEntity sce: serviceConfigEntities) {
      if ("HDFS".equals(sce.getServiceName()) && (sce.getVersion() == 1)) {
        Assert.assertEquals("c1", sce.getClusterEntity().getClusterName());
        Assert.assertEquals(Long.valueOf(1), sce.getClusterEntity()
          .getClusterId());
        Assert.assertEquals(Long.valueOf(1111L), sce.getCreateTimestamp());
        Assert.assertTrue(sce.getClusterConfigEntities().isEmpty());
        Assert.assertNotNull(sce.getServiceConfigId());
      } else if ("HDFS".equals(sce.getServiceName()) && (sce.getVersion() == 2)) {
        Assert.assertEquals("c1", sce.getClusterEntity().getClusterName());
        Assert.assertEquals(Long.valueOf(1), sce.getClusterEntity()
          .getClusterId());
        Assert.assertEquals(Long.valueOf(2222L), sce.getCreateTimestamp());
        Assert.assertTrue(sce.getClusterConfigEntities().isEmpty());
        Assert.assertNotNull(sce.getServiceConfigId());
      } else if ("YARN".equals(sce.getServiceName())) {
        Assert.assertEquals("c1", sce.getClusterEntity().getClusterName());
        Assert.assertEquals(Long.valueOf(1), sce.getClusterEntity()
          .getClusterId());
        Assert.assertEquals(Long.valueOf(3333L), sce.getCreateTimestamp());
        Assert.assertEquals(Long.valueOf(1), sce.getVersion());
        Assert.assertTrue(sce.getClusterConfigEntities().isEmpty());
        Assert.assertNotNull(sce.getServiceConfigId());

      } else {
        Assert.fail();
      }
      Assert.assertEquals("admin", sce.getUser());
    }
  }

}
