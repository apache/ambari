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

import static org.easymock.EasyMock.createMockBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.entities.ClusterConfigEntity;
import org.apache.ambari.server.orm.entities.ClusterConfigMappingEntity;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ConfigGroupConfigMappingEntity;
import org.apache.ambari.server.orm.entities.ConfigGroupEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.ServiceConfigEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.cluster.ClusterImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;
import com.sun.research.ws.wadl.ResourceType;

import junit.framework.Assert;

public class ServiceConfigDAOTest {
  private static final StackId HDP_01 = new StackId("HDP", "0.1");
  private static final StackId HDP_02 = new StackId("HDP", "0.2");

  private Injector injector;
  private ServiceConfigDAO serviceConfigDAO;
  private ClusterDAO clusterDAO;
  private ResourceTypeDAO resourceTypeDAO;
  private StackDAO stackDAO;
  private ConfigGroupDAO configGroupDAO;
  private ConfigGroupConfigMappingDAO configGroupConfigMappingDAO;

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);

    // required to load stack information into the DB
    injector.getInstance(AmbariMetaInfo.class);

    clusterDAO = injector.getInstance(ClusterDAO.class);
    stackDAO = injector.getInstance(StackDAO.class);
    serviceConfigDAO = injector.getInstance(ServiceConfigDAO.class);
    resourceTypeDAO = injector.getInstance(ResourceTypeDAO.class);
    configGroupDAO = injector.getInstance(ConfigGroupDAO.class);
    configGroupConfigMappingDAO = injector.getInstance(ConfigGroupConfigMappingDAO.class);
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
      StackEntity stackEntity = stackDAO.find(HDP_01.getStackName(),
          HDP_01.getStackVersion());

      clusterEntity = new ClusterEntity();
      clusterEntity.setClusterName("c1");
      clusterEntity.setResource(resourceEntity);
      clusterEntity.setDesiredStack(stackEntity);

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
    serviceConfigEntity.setStack(clusterEntity.getDesiredStack());

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

    long hdfsVersion = serviceConfigDAO.findNextServiceConfigVersion(
        clusterDAO.findByName("c1").getClusterId(), "HDFS");

    long yarnVersion = serviceConfigDAO.findNextServiceConfigVersion(
        clusterDAO.findByName("c1").getClusterId(), "YARN");

    Assert.assertEquals(3, hdfsVersion);
    Assert.assertEquals(2, yarnVersion);
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

  @Test
  public void testGetAllServiceConfigs() throws Exception {
    ServiceConfigEntity serviceConfigEntity = null;
    serviceConfigEntity = createServiceConfig("HDFS", "admin", 1L, 1L, 10L, null);
    serviceConfigEntity = createServiceConfig("HDFS", "admin", 2L, 2L, 20L, null);
    serviceConfigEntity = createServiceConfig("HDFS", "admin", 3L, 3L, 30L, null);
    serviceConfigEntity = createServiceConfig("YARN", "admin", 1L, 4L, 40L, null);

    long clusterId = serviceConfigEntity.getClusterId();

    List<ServiceConfigEntity> serviceConfigs = serviceConfigDAO.getAllServiceConfigsForClusterAndStack(clusterId, HDP_01);
    Assert.assertEquals(4, serviceConfigs.size());

    serviceConfigs = serviceConfigDAO.getAllServiceConfigsForClusterAndStack(clusterId, HDP_02);
    Assert.assertEquals(0, serviceConfigs.size());
  }

  @Test
  public void testGetLatestServiceConfigs() throws Exception {
    ServiceConfigEntity serviceConfigEntity = null;
    serviceConfigEntity = createServiceConfig("HDFS", "admin", 1L, 1L, 10L, null);
    serviceConfigEntity = createServiceConfig("HDFS", "admin", 2L, 2L, 20L, null);
    serviceConfigEntity = createServiceConfig("HDFS", "admin", 3L, 3L, 30L, null);
    serviceConfigEntity = createServiceConfig("YARN", "admin", 1L, 4L, 40L, null);

    StackEntity stackEntity = stackDAO.find(HDP_02.getStackName(),
        HDP_02.getStackVersion());

    ClusterEntity clusterEntity = serviceConfigEntity.getClusterEntity();
    clusterEntity.setDesiredStack(stackEntity);
    clusterDAO.merge(clusterEntity);

    // create some for HDP 0.2
    serviceConfigEntity = createServiceConfig("HDFS", "admin", 4L, 5L, 50L, null);
    serviceConfigEntity = createServiceConfig("HDFS", "admin", 5L, 6L, 60L, null);
    serviceConfigEntity = createServiceConfig("YARN", "admin", 2L, 7L, 70L, null);

    long clusterId = serviceConfigEntity.getClusterId();

    List<ServiceConfigEntity> serviceConfigs = serviceConfigDAO.getLatestServiceConfigs(clusterId, HDP_01);
    Assert.assertEquals(2, serviceConfigs.size());

    serviceConfigs = serviceConfigDAO.getLatestServiceConfigs(clusterId, HDP_02);
    Assert.assertEquals(2, serviceConfigs.size());
  }

  @Test
  public void testConfiguration() throws Exception{
    initClusterEntities();
    ClusterEntity clusterEntity = clusterDAO.findByName("c1");

    Assert.assertTrue(!clusterEntity.getClusterConfigEntities().isEmpty());
    Assert.assertTrue(!clusterEntity.getConfigMappingEntities().isEmpty());

    Assert.assertEquals(5, clusterEntity.getClusterConfigEntities().size());
    Assert.assertEquals(3, clusterEntity.getConfigMappingEntities().size());
  }

  @Test
  public void testGetClusterConfigMappingByStack() throws Exception{
    initClusterEntities();

    ClusterEntity clusterEntity = clusterDAO.findByName("c1");

    List<ClusterConfigMappingEntity> clusterConfigMappingEntities = clusterDAO.getClusterConfigMappingsByStack(clusterEntity.getClusterId(), HDP_01);
    Assert.assertEquals(2, clusterConfigMappingEntities .size());

    ClusterConfigMappingEntity e1 = clusterConfigMappingEntities.get(0);
    String tag1 = e1.getTag();
    Assert.assertEquals("version1", tag1);
    String type1 = e1.getType();
    Assert.assertEquals("oozie-site", type1);

    ClusterConfigMappingEntity e2 = clusterConfigMappingEntities.get(1);
    String tag2 = e2.getTag();
    Assert.assertEquals("version2", tag2);
    String type2 = e2.getType();
    Assert.assertEquals("oozie-site", type2);
  }

  /**
   * Test the get latest configuration query against clusterconfig table with configuration groups inserted
   * */
  @Test
  public void testGetClusterConfigMappingByStackCG() throws Exception{
    initClusterEntitiesWithConfigGroups();
    ClusterEntity clusterEntity = clusterDAO.findByName("c1");

    List<ConfigGroupEntity> configGroupEntities = configGroupDAO.findAllByTag("OOZIE");

    Assert.assertNotNull(configGroupEntities);
    ConfigGroupEntity configGroupEntity = configGroupEntities.get(0);
    Assert.assertNotNull(configGroupEntity);
    Assert.assertEquals("c1", configGroupEntity.getClusterEntity().getClusterName());
    Assert.assertEquals(Long.valueOf(1), configGroupEntity.getClusterEntity()
      .getClusterId());
    Assert.assertEquals("oozie_server", configGroupEntity.getGroupName());
    Assert.assertEquals("OOZIE", configGroupEntity.getTag());
    Assert.assertEquals("oozie server", configGroupEntity.getDescription());

    List<ClusterConfigMappingEntity> clusterConfigMappingEntities = clusterDAO.getClusterConfigMappingsByStack(clusterEntity.getClusterId(), HDP_01);
    Assert.assertEquals(2, clusterConfigMappingEntities .size());

    ClusterConfigMappingEntity e1 = clusterConfigMappingEntities.get(0);
    String tag1 = e1.getTag();
    Assert.assertEquals("version1", tag1);
    String type1 = e1.getType();
    Assert.assertEquals("oozie-site", type1);

    ClusterConfigMappingEntity e2 = clusterConfigMappingEntities.get(1);
    String tag2 = e2.getTag();
    Assert.assertEquals("version2", tag2);
    String type2 = e2.getType();
    Assert.assertEquals("oozie-site", type2);
  }

  /**
   * Test
   *
   * When the last configuration of a given configuration type to be stored into the clusterconfig table is
   * for a configuration group, there is no corresponding entry generated in the clusterconfigmapping.
   *
   * Therefore, the getlatestconfiguration query should skip configuration groups stored in the clusterconfig table.
   *
   * Test to determine the latest configuration of a given type whose version_tag
   * exists in the clusterconfigmapping table.
   *
   * */
  @Test
  public void testGetLatestClusterConfigMappingByStack() throws Exception{
    ClusterImpl cluster =
        createMockBuilder(ClusterImpl.class).
          addMockedMethod("getSessionManager").
          addMockedMethod("getClusterName").
          addMockedMethod("getSessionAttributes").
          createMock();

    initClusterEntities();
    ClusterEntity clusterEntity = clusterDAO.findByName("c1");
    List<ClusterConfigMappingEntity> clusterConfigMappingEntities = clusterDAO.getClusterConfigMappingsByStack(clusterEntity.getClusterId(), HDP_01);
    Collection<ClusterConfigMappingEntity> latestMapingEntities = cluster.getLatestConfigMapping(clusterConfigMappingEntities);
    Assert.assertEquals(1, latestMapingEntities.size());
    for(ClusterConfigMappingEntity e: latestMapingEntities){
      Assert.assertEquals("version2", e.getTag());
      Assert.assertEquals("oozie-site", e.getType());
    }
  }

  /**
   * Test
   *
   * When the last configuration of a given configuration type to be stored into the clusterconfig table is
   * for a configuration group, there is no corresponding entry generated in the clusterconfigmapping.
   *
   * Therefore, the getlatestconfiguration query should skip configuration groups stored in the clusterconfig table.
   *
   * Test to determine the latest configuration of a given type whose version_tag
   * exists in the clusterconfigmapping table.
   *
   * */
  @Test
  public void testGetLatestClusterConfigMappingByStackCG() throws Exception{
    ClusterImpl cluster =
        createMockBuilder(ClusterImpl.class).
          addMockedMethod("getSessionManager").
          addMockedMethod("getClusterName").
          addMockedMethod("getSessionAttributes").
          createMock();

    initClusterEntitiesWithConfigGroups();
    ClusterEntity clusterEntity = clusterDAO.findByName("c1");
    List<ClusterConfigMappingEntity> clusterConfigMappingEntities = clusterDAO.getClusterConfigMappingsByStack(clusterEntity.getClusterId(), HDP_01);
    Collection<ClusterConfigMappingEntity> latestMapingEntities = cluster.getLatestConfigMapping(clusterConfigMappingEntities);
    Assert.assertEquals(1, latestMapingEntities.size());
    for(ClusterConfigMappingEntity e: latestMapingEntities){
      Assert.assertEquals("version2", e.getTag());
      Assert.assertEquals("oozie-site", e.getType());
    }
  }

  private void initClusterEntities() throws Exception{
    String userName = "admin";

    ServiceConfigEntity oozieServiceConfigEntity = createServiceConfig("OOZIE", userName, 1L, 1L, System.currentTimeMillis(), null);
    ClusterEntity  clusterEntity = oozieServiceConfigEntity.getClusterEntity();

    Long clusterId = clusterEntity.getClusterId();

    if(null == clusterId){
      clusterId = 1L;
      clusterEntity.setClusterId(clusterId);
      clusterEntity = clusterDAO.merge(clusterEntity);
    }

    StackEntity stackEntityHDP01 = stackDAO.find(HDP_01.getStackName(),HDP_01.getStackVersion());
    StackEntity stackEntityHDP02 = stackDAO.find(HDP_02.getStackName(),HDP_02.getStackVersion());

    String oozieSite = "oozie-site";

    for (int i = 1; i < 6; i++){
      ClusterConfigEntity entity = new ClusterConfigEntity();
      entity.setClusterEntity(clusterEntity);
      entity.setClusterId(clusterEntity.getClusterId());
      entity.setType(oozieSite);
      entity.setVersion(Long.valueOf(i));
      entity.setTag("version"+i);
      entity.setTimestamp(new Date().getTime());
      if(i < 4) {
        entity.setStack(stackEntityHDP01);
      } else {
        entity.setStack(stackEntityHDP02);
      }
      entity.setData("");
      clusterDAO.createConfig(entity);
      clusterEntity.getClusterConfigEntities().add(entity);
      clusterDAO.merge(clusterEntity);
    }

    Collection<ClusterConfigMappingEntity> entities = clusterEntity.getConfigMappingEntities();
    if(null == entities){
      entities = new ArrayList<ClusterConfigMappingEntity>();
      clusterEntity.setConfigMappingEntities(entities);
    }

    ClusterConfigMappingEntity e1 = new ClusterConfigMappingEntity();
    e1.setClusterEntity(clusterEntity);
    e1.setClusterId(clusterEntity.getClusterId());
    e1.setCreateTimestamp(System.currentTimeMillis());
    e1.setSelected(0);
    e1.setUser(userName);
    e1.setType(oozieSite);
    e1.setTag("version1");
    entities.add(e1);
    clusterDAO.merge(clusterEntity);

    ClusterConfigMappingEntity e2 = new ClusterConfigMappingEntity();
    e2.setClusterEntity(clusterEntity);
    e2.setClusterId(clusterEntity.getClusterId());
    e2.setCreateTimestamp(System.currentTimeMillis());
    e2.setSelected(0);
    e2.setUser(userName);
    e2.setType(oozieSite);
    e2.setTag("version2");
    entities.add(e2);
    clusterDAO.merge(clusterEntity);

    ClusterConfigMappingEntity e3 = new ClusterConfigMappingEntity();
    e3.setClusterEntity(clusterEntity);
    e3.setClusterId(clusterEntity.getClusterId());
    e3.setCreateTimestamp(System.currentTimeMillis());
    e3.setSelected(1);
    e3.setUser(userName);
    e3.setType(oozieSite);
    e3.setTag("version4");
    entities.add(e3);
    clusterDAO.merge(clusterEntity);
  }

  private void initClusterEntitiesWithConfigGroups() throws Exception{
    String userName = "admin";

    ServiceConfigEntity oozieServiceConfigEntity = createServiceConfig("OOZIE", userName, 1L, 1L, System.currentTimeMillis(), null);
    ClusterEntity  clusterEntity = oozieServiceConfigEntity.getClusterEntity();

    Long clusterId = clusterEntity.getClusterId();

    if(null == clusterId){
      clusterId = 1L;
      clusterEntity.setClusterId(clusterId);
      clusterEntity = clusterDAO.merge(clusterEntity);
    }

    StackEntity stackEntityHDP01 = stackDAO.find(HDP_01.getStackName(),HDP_01.getStackVersion());
    String oozieSite = "oozie-site";

    int count = 3;
    for (int i = 1; i < count; i++){
      ClusterConfigEntity entity = new ClusterConfigEntity();
      entity.setClusterEntity(clusterEntity);
      entity.setClusterId(clusterEntity.getClusterId());
      entity.setType(oozieSite);
      entity.setVersion(Long.valueOf(i));
      entity.setTag("version"+i);
      entity.setTimestamp(new Date().getTime());
      entity.setStack(stackEntityHDP01);
      entity.setData("");
      clusterDAO.createConfig(entity);
      clusterEntity.getClusterConfigEntities().add(entity);
      clusterDAO.merge(clusterEntity);
    }

    Collection<ClusterConfigMappingEntity> entities = clusterEntity.getConfigMappingEntities();
    if(null == entities){
      entities = new ArrayList<ClusterConfigMappingEntity>();
      clusterEntity.setConfigMappingEntities(entities);
    }

    ClusterConfigMappingEntity e1 = new ClusterConfigMappingEntity();
    e1.setClusterEntity(clusterEntity);
    e1.setClusterId(clusterEntity.getClusterId());
    e1.setCreateTimestamp(System.currentTimeMillis());
    e1.setSelected(0);
    e1.setUser(userName);
    e1.setType(oozieSite);
    e1.setTag("version1");
    entities.add(e1);
    clusterDAO.merge(clusterEntity);

    ClusterConfigMappingEntity e2 = new ClusterConfigMappingEntity();
    e2.setClusterEntity(clusterEntity);
    e2.setClusterId(clusterEntity.getClusterId());
    e2.setCreateTimestamp(System.currentTimeMillis());
    e2.setSelected(1);
    e2.setUser(userName);
    e2.setType(oozieSite);
    e2.setTag("version2");
    entities.add(e2);
    clusterDAO.merge(clusterEntity);

    ConfigGroupEntity configGroupEntity = new ConfigGroupEntity();

    ResourceTypeEntity resourceTypeEntity = resourceTypeDAO.findById(ResourceTypeEntity.CLUSTER_RESOURCE_TYPE);
    if (resourceTypeEntity == null) {
      resourceTypeEntity = new ResourceTypeEntity();
      resourceTypeEntity.setId(ResourceTypeEntity.CLUSTER_RESOURCE_TYPE);
      resourceTypeEntity.setName(ResourceTypeEntity.CLUSTER_RESOURCE_TYPE_NAME);
      resourceTypeEntity = resourceTypeDAO.merge(resourceTypeEntity);
    }

    ResourceEntity resourceEntity = new ResourceEntity();
    resourceEntity.setResourceType(resourceTypeEntity);

    configGroupEntity.setClusterEntity(clusterEntity);
    configGroupEntity.setClusterId(clusterEntity.getClusterId());
    configGroupEntity.setGroupName("oozie_server");
    configGroupEntity.setDescription("oozie server");
    configGroupEntity.setTag("OOZIE");

    ClusterConfigEntity configEntity = new ClusterConfigEntity();
    configEntity.setType("oozie-site");
    configEntity.setTag("version3");
    configEntity.setData("someData");
    configEntity.setAttributes("someAttributes");
    configEntity.setStack(stackEntityHDP01);

    List<ClusterConfigEntity> configEntities = new
      ArrayList<ClusterConfigEntity>();
    configEntities.add(configEntity);

    configGroupDAO.create(configGroupEntity);

    if (configEntities != null && !configEntities.isEmpty()) {
      List<ConfigGroupConfigMappingEntity> configMappingEntities = new
        ArrayList<ConfigGroupConfigMappingEntity>();

      for (ClusterConfigEntity config : configEntities) {
        config.setClusterEntity(clusterEntity);
        config.setClusterId(clusterEntity.getClusterId());
        clusterDAO.createConfig(config);

        ConfigGroupConfigMappingEntity configMappingEntity = new
          ConfigGroupConfigMappingEntity();
        configMappingEntity.setClusterId(clusterEntity.getClusterId());
        configMappingEntity.setClusterConfigEntity(config);
        configMappingEntity.setConfigGroupEntity(configGroupEntity);
        configMappingEntity.setConfigGroupId(configGroupEntity.getGroupId());
        configMappingEntity.setVersionTag(config.getTag());
        configMappingEntity.setConfigType(config.getType());
        configMappingEntity.setTimestamp(System.currentTimeMillis());
        configMappingEntities.add(configMappingEntity);
        configGroupConfigMappingDAO.create(configMappingEntity);
      }

      configGroupEntity.setConfigGroupConfigMappingEntities(configMappingEntities);
      configGroupDAO.merge(configGroupEntity);
    }
  }
}
