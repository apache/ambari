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
package org.apache.ambari.server.orm.dao;

import static java.util.Collections.singletonList;
import static org.apache.ambari.server.orm.OrmTestHelper.CLUSTER_NAME;
import static org.apache.ambari.server.orm.OrmTestHelper.SERVICE_GROUP_NAME;
import static org.apache.ambari.server.orm.OrmTestHelper.SERVICE_NAME;
import static org.apache.ambari.server.orm.OrmTestHelper.STACK_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.entities.ClusterConfigEntity;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.orm.entities.ConfigGroupConfigMappingEntity;
import org.apache.ambari.server.orm.entities.ConfigGroupEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.ServiceConfigEntity;
import org.apache.ambari.server.orm.entities.ServiceGroupEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.state.StackId;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class ServiceConfigDAOTest {
  private static final StackId HDP_01 = new StackId("HDP", "0.1");
  private static final StackId HDP_02 = new StackId("HDP", "0.2");

  private Injector injector;
  private ServiceConfigDAO serviceConfigDAO;
  private ClusterDAO clusterDAO;
  private ServiceGroupDAO serviceGroupDAO;
  private ClusterServiceDAO serviceDAO;
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
    serviceGroupDAO = injector.getInstance(ServiceGroupDAO.class);
    serviceDAO = injector.getInstance(ClusterServiceDAO.class);
    stackDAO = injector.getInstance(StackDAO.class);
    serviceConfigDAO = injector.getInstance(ServiceConfigDAO.class);
    resourceTypeDAO = injector.getInstance(ResourceTypeDAO.class);
    configGroupDAO = injector.getInstance(ConfigGroupDAO.class);
    configGroupConfigMappingDAO = injector.getInstance(ConfigGroupConfigMappingDAO.class);

    OrmTestHelper helper = injector.getInstance(OrmTestHelper.class);
    helper.createDefaultData();
  }

  @After
  public void teardown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  private ServiceConfigEntity createServiceConfig(String serviceName,
    String userName, Long version, Long serviceConfigId,
    Long createTimestamp
  )
    throws Exception {
    ServiceConfigEntity serviceConfigEntity = prepareServiceConfig(serviceName,
        userName, version, serviceConfigId, createTimestamp
    );
    serviceConfigDAO.create(serviceConfigEntity);

    return serviceConfigEntity;
  }

  private ServiceConfigEntity createServiceConfigWithGroup(String serviceName,
    String userName, Long version, Long serviceConfigId,
    Long createTimestamp, Long groupId
  )
      throws Exception {
    ServiceConfigEntity serviceConfigEntity = prepareServiceConfig(serviceName,
        userName, version, serviceConfigId, createTimestamp
    );
    serviceConfigEntity.setGroupId(groupId);
    serviceConfigDAO.create(serviceConfigEntity);

    return serviceConfigEntity;
  }


  private ServiceConfigEntity prepareServiceConfig(String serviceName,
    String userName, Long version, Long serviceConfigId,
    Long createTimestamp
  )
      throws Exception {

    // create an admin resource to represent this cluster
    ResourceTypeEntity resourceTypeEntity = resourceTypeDAO.findById(ResourceType.CLUSTER.getId());
    if (resourceTypeEntity == null) {
      resourceTypeEntity = new ResourceTypeEntity();
      resourceTypeEntity.setId(ResourceType.CLUSTER.getId());
      resourceTypeEntity.setName(ResourceType.CLUSTER.name());
      resourceTypeEntity = resourceTypeDAO.merge(resourceTypeEntity);
    }

    ResourceEntity resourceEntity = new ResourceEntity();
    resourceEntity.setResourceType(resourceTypeEntity);

    ClusterEntity clusterEntity = clusterDAO.findByName(CLUSTER_NAME);
    ServiceGroupEntity serviceGroupEntity = serviceGroupDAO.find(clusterEntity.getClusterId(), SERVICE_GROUP_NAME);
    ClusterServiceEntity serviceEntity = serviceDAO.findByName(clusterEntity.getClusterName(), serviceGroupEntity.getServiceGroupName(), serviceName);
    if (serviceEntity == null) {
      serviceEntity = new ClusterServiceEntity();
      serviceEntity.setClusterEntity(clusterEntity);
      serviceEntity.setServiceGroupEntity(serviceGroupEntity);
      serviceEntity.setServiceName(serviceName);
      serviceEntity.setServiceType(serviceName);
      serviceDAO.create(serviceEntity);
    }

    ServiceConfigEntity serviceConfigEntity = new ServiceConfigEntity();
    serviceConfigEntity.setUser(userName);
    serviceConfigEntity.setVersion(version);
    serviceConfigEntity.setServiceConfigId(serviceConfigId);
    serviceConfigEntity.setClusterId(clusterEntity.getClusterId());
    serviceConfigEntity.setCreateTimestamp(createTimestamp);
    serviceConfigEntity.setClusterEntity(clusterEntity);
    serviceConfigEntity.setServiceGroupId(serviceGroupEntity.getServiceGroupId());
    serviceConfigEntity.setServiceId(serviceEntity.getServiceId());
    serviceConfigEntity.setClusterServiceEntity(serviceEntity);
    serviceConfigEntity.setStack(clusterEntity.getDesiredStack());
    return serviceConfigEntity;
  }


  @Test
  public void testCreateServiceConfigVersion() throws Exception {

    ServiceConfigEntity serviceConfigEntity =
      createServiceConfig(SERVICE_NAME, "admin", 1L, 1L, 1111L);

    Long clusterId = clusterDAO.findByName(CLUSTER_NAME).getClusterId();

    assertNotNull(serviceConfigEntity);
    assertEquals(CLUSTER_NAME, serviceConfigEntity.getClusterEntity().getClusterName());
    assertEquals(clusterId, serviceConfigEntity.getClusterEntity()
      .getClusterId());
    assertEquals(SERVICE_NAME, serviceConfigEntity.getServiceName());
    assertEquals(Long.valueOf(1111L), serviceConfigEntity.getCreateTimestamp());
    assertEquals("admin", serviceConfigEntity.getUser());
    assertEquals(Long.valueOf(1), serviceConfigEntity.getVersion());
    assertTrue(serviceConfigEntity.getClusterConfigEntities().isEmpty());
    assertNotNull(serviceConfigEntity.getServiceConfigId());
  }

  @Test
  public void testFindServiceConfigEntity() throws Exception {
    ServiceConfigEntity sce =
      createServiceConfig(SERVICE_NAME, "admin", 1L, 1L, 1111L);

    ServiceConfigEntity serviceConfigEntity = serviceConfigDAO.find(sce.getServiceConfigId());

    Long clusterId = clusterDAO.findByName(CLUSTER_NAME).getClusterId();

    assertNotNull(serviceConfigEntity);
    assertEquals(CLUSTER_NAME, serviceConfigEntity.getClusterEntity().getClusterName());
    assertEquals(clusterId, serviceConfigEntity.getClusterEntity().getClusterId());
    assertEquals(SERVICE_NAME, serviceConfigEntity.getServiceName());
    assertEquals(Long.valueOf(1111L), serviceConfigEntity.getCreateTimestamp());
    assertEquals("admin", serviceConfigEntity.getUser());
    assertEquals(Long.valueOf(1), serviceConfigEntity.getVersion());
    assertTrue(serviceConfigEntity.getClusterConfigEntities().isEmpty());
    assertNotNull(serviceConfigEntity.getServiceConfigId());
  }

  @Test
  public void testFindByServiceAndVersion() throws Exception {
    ServiceConfigEntity serviceConfig = createServiceConfig(SERVICE_NAME, "admin", 1L, 1L, 1111L);

    ServiceConfigEntity serviceConfigEntity =
      serviceConfigDAO.findByServiceAndVersion(serviceConfig.getServiceId(), 1L);

    Long clusterId = clusterDAO.findByName(CLUSTER_NAME).getClusterId();

    assertNotNull(serviceConfigEntity);
    assertEquals(CLUSTER_NAME, serviceConfigEntity.getClusterEntity().getClusterName());
    assertEquals(clusterId, serviceConfigEntity.getClusterEntity().getClusterId());
    assertEquals(SERVICE_NAME, serviceConfigEntity.getServiceName());
    assertEquals(Long.valueOf(1111L), serviceConfigEntity.getCreateTimestamp());
    assertEquals("admin", serviceConfigEntity.getUser());
    assertEquals(Long.valueOf(1), serviceConfigEntity.getVersion());
    assertTrue(serviceConfigEntity.getClusterConfigEntities().isEmpty());
    assertNotNull(serviceConfigEntity.getServiceConfigId());
  }

  @Test
  public void testFindMaxVersions() throws Exception {
    createServiceConfig(SERVICE_NAME, "admin", 1L, 1L, 1111L);
    createServiceConfig(SERVICE_NAME, "admin", 2L, 2L, 2222L);
    createServiceConfig("YARN", "admin", 1L, 3L, 3333L);

    long hdfsVersion = serviceConfigDAO.findNextServiceConfigVersion(
        clusterDAO.findByName(CLUSTER_NAME).getClusterId(), 1L);

    long yarnVersion = serviceConfigDAO.findNextServiceConfigVersion(
        clusterDAO.findByName(CLUSTER_NAME).getClusterId(), 2L);

    assertEquals(3, hdfsVersion);
    assertEquals(2, yarnVersion);
  }

  @Test
  public void testGetLastServiceConfigs() throws Exception {
    createServiceConfig(SERVICE_NAME, "admin", 1L, 1L, 1111L);
    createServiceConfig(SERVICE_NAME, "admin", 2L, 2L, 2222L);
    createServiceConfig("YARN", "admin", 1L, 3L, 3333L);

    List<ServiceConfigEntity> serviceConfigEntities =
      serviceConfigDAO.getLastServiceConfigs(clusterDAO.findByName(CLUSTER_NAME).getClusterId());

    assertNotNull(serviceConfigEntities);
    assertEquals(2, serviceConfigEntities.size());

    Long clusterId = clusterDAO.findByName(CLUSTER_NAME).getClusterId();

    for (ServiceConfigEntity sce: serviceConfigEntities) {
      if (SERVICE_NAME.equals(sce.getServiceName())) {
        assertEquals(CLUSTER_NAME, sce.getClusterEntity().getClusterName());
        assertEquals(clusterId, sce.getClusterEntity()
          .getClusterId());
        assertEquals(Long.valueOf(2222L), sce.getCreateTimestamp());
        assertEquals(Long.valueOf(2), sce.getVersion());
        assertTrue(sce.getClusterConfigEntities().isEmpty());
        assertNotNull(sce.getServiceConfigId());
      }
      if ("YARN".equals(sce.getServiceName())) {
        assertEquals(CLUSTER_NAME, sce.getClusterEntity().getClusterName());
        assertEquals(clusterId, sce.getClusterEntity()
          .getClusterId());
        assertEquals(Long.valueOf(3333L), sce.getCreateTimestamp());
        assertEquals(Long.valueOf(1), sce.getVersion());
        assertTrue(sce.getClusterConfigEntities().isEmpty());
        assertNotNull(sce.getServiceConfigId());
      }

      assertEquals("admin", sce.getUser());
    }
  }

  @Test
  public void testGetLastServiceConfigsForService() throws Exception {
    String serviceName = SERVICE_NAME;
    ConfigGroupEntity configGroupEntity1 = new ConfigGroupEntity();
    ClusterEntity clusterEntity = clusterDAO.findByName(CLUSTER_NAME);
    configGroupEntity1.setClusterEntity(clusterEntity);
    configGroupEntity1.setClusterId(clusterEntity.getClusterId());
    configGroupEntity1.setGroupName("group1");
    configGroupEntity1.setDescription("group1_desc");
    configGroupEntity1.setTag(SERVICE_NAME);
    //configGroupEntity1.setServiceName(OrmTestHelper.SERVICE_NAME);
    configGroupDAO.create(configGroupEntity1);
    ConfigGroupEntity group1 = configGroupDAO.findByName("group1");
    ConfigGroupEntity configGroupEntity2 = new ConfigGroupEntity();
    configGroupEntity2.setClusterEntity(clusterEntity);
    configGroupEntity2.setClusterId(clusterEntity.getClusterId());
    configGroupEntity2.setGroupName("group2");
    configGroupEntity2.setDescription("group2_desc");
    configGroupEntity2.setTag(SERVICE_NAME);
    //configGroupEntity2.setServiceName(OrmTestHelper.SERVICE_NAME);
    configGroupDAO.create(configGroupEntity2);
    ConfigGroupEntity group2 = configGroupDAO.findByName("group2");
    createServiceConfig(serviceName, "admin", 1L, 1L, 1111L);
    createServiceConfig(serviceName, "admin", 2L, 2L, 1010L);
    createServiceConfigWithGroup(serviceName, "admin", 3L, 3L, 2222L, group1.getGroupId());
    createServiceConfigWithGroup(serviceName, "admin", 5L, 5L, 3333L, group2.getGroupId());
    createServiceConfigWithGroup(serviceName, "admin", 4L, 4L, 3330L, group2.getGroupId());

    List<ServiceConfigEntity> serviceConfigEntities = serviceConfigDAO
        .getLastServiceConfigsForService(clusterDAO.findByName(CLUSTER_NAME).getClusterId(), 1L);
    assertNotNull(serviceConfigEntities);
    assertEquals(3, serviceConfigEntities.size());

    for (ServiceConfigEntity sce : serviceConfigEntities) {
      if (sce.getGroupId() != null && sce.getGroupId().equals(group2.getGroupId())) {
        // Group ID with the highest version should be selected
        assertEquals(sce.getVersion(), Long.valueOf(5L));
      }
    }
  }


  @Test
  public void testGetLastServiceConfig() throws Exception {
    createServiceConfig(SERVICE_NAME, "admin", 1L, 1L, 1111L);
    createServiceConfig(SERVICE_NAME, "admin", 2L, 2L, 2222L);
    createServiceConfig("YARN", "admin", 1L, 3L, 3333L);

    Long clusterId = clusterDAO.findByName(CLUSTER_NAME).getClusterId();

    ServiceConfigEntity serviceConfigEntity =
      serviceConfigDAO.getLastServiceConfig(clusterId, 1L);

    assertNotNull(serviceConfigEntity);
    assertEquals(CLUSTER_NAME, serviceConfigEntity.getClusterEntity().getClusterName());
    assertEquals(clusterId, serviceConfigEntity.getClusterEntity()
      .getClusterId());
    assertEquals(SERVICE_NAME, serviceConfigEntity.getServiceName());
    assertEquals(Long.valueOf(2222L), serviceConfigEntity.getCreateTimestamp());
    assertEquals("admin", serviceConfigEntity.getUser());
    assertEquals(Long.valueOf(2), serviceConfigEntity.getVersion());
    assertTrue(serviceConfigEntity.getClusterConfigEntities().isEmpty());
    assertNotNull(serviceConfigEntity.getServiceConfigId());
  }

  @Test
  public void testGetServiceConfigs() throws Exception {
    createServiceConfig(SERVICE_NAME, "admin", 1L, 1L, 1111L);
    createServiceConfig(SERVICE_NAME, "admin", 2L, 2L, 2222L);
    createServiceConfig("YARN", "admin", 1L, 3L, 3333L);

    Long clusterId = clusterDAO.findByName(CLUSTER_NAME).getClusterId();

    List<ServiceConfigEntity> serviceConfigEntities =
      serviceConfigDAO.getServiceConfigs(clusterId);

    assertNotNull(serviceConfigEntities);
    assertEquals(3, serviceConfigEntities.size());

    for (ServiceConfigEntity sce: serviceConfigEntities) {
      if (SERVICE_NAME.equals(sce.getServiceName()) && (sce.getVersion() == 1)) {
        assertEquals(CLUSTER_NAME, sce.getClusterEntity().getClusterName());
        assertEquals(clusterId, sce.getClusterEntity()
          .getClusterId());
        assertEquals(Long.valueOf(1111L), sce.getCreateTimestamp());
        assertTrue(sce.getClusterConfigEntities().isEmpty());
        assertNotNull(sce.getServiceConfigId());
      } else if (SERVICE_NAME.equals(sce.getServiceName()) && (sce.getVersion() == 2)) {
        assertEquals(CLUSTER_NAME, sce.getClusterEntity().getClusterName());
        assertEquals(clusterId, sce.getClusterEntity()
          .getClusterId());
        assertEquals(Long.valueOf(2222L), sce.getCreateTimestamp());
        assertTrue(sce.getClusterConfigEntities().isEmpty());
        assertNotNull(sce.getServiceConfigId());
      } else if ("YARN".equals(sce.getServiceName())) {
        assertEquals(CLUSTER_NAME, sce.getClusterEntity().getClusterName());
        assertEquals(clusterId, sce.getClusterEntity()
          .getClusterId());
        assertEquals(Long.valueOf(3333L), sce.getCreateTimestamp());
        assertEquals(Long.valueOf(1), sce.getVersion());
        assertTrue(sce.getClusterConfigEntities().isEmpty());
        assertNotNull(sce.getServiceConfigId());

      } else {
        Assert.fail();
      }
      assertEquals("admin", sce.getUser());
    }
  }

  @Test
  public void testGetAllServiceConfigs() throws Exception {
    createServiceConfig(SERVICE_NAME, "admin", 1L, 1L, 10L);
    createServiceConfig(SERVICE_NAME, "admin", 2L, 2L, 20L);
    createServiceConfig(SERVICE_NAME, "admin", 3L, 3L, 30L);
    createServiceConfig("YARN", "admin", 1L, 4L, 40L);

    Long clusterId = clusterDAO.findByName(CLUSTER_NAME).getClusterId();
    Long hdfsServiceId = serviceDAO.findByName(CLUSTER_NAME, SERVICE_GROUP_NAME, SERVICE_NAME).getServiceId();
    Long yarnServiceId = serviceDAO.findByName(CLUSTER_NAME, SERVICE_GROUP_NAME, "YARN").getServiceId();

    assertEquals(3, serviceConfigDAO.getServiceConfigsForServiceAndStack(clusterId, STACK_ID, hdfsServiceId).size());
    assertEquals(1, serviceConfigDAO.getServiceConfigsForServiceAndStack(clusterId, STACK_ID, yarnServiceId).size());
    assertEquals(0, serviceConfigDAO.getServiceConfigsForServiceAndStack(clusterId, HDP_01, hdfsServiceId).size());
  }

  @Test
  public void testGetLatestServiceConfigs() throws Exception {
    createServiceConfig(SERVICE_NAME, "admin", 1L, 1L, 10L);
    createServiceConfig(SERVICE_NAME, "admin", 2L, 2L, 20L);
    createServiceConfig(SERVICE_NAME, "admin", 3L, 3L, 30L);
    createServiceConfig("YARN", "admin", 1L, 4L, 40L);

    StackEntity stackEntity = stackDAO.find(HDP_02);
    ClusterEntity clusterEntity = clusterDAO.findByName(CLUSTER_NAME);
    clusterEntity.setDesiredStack(stackEntity);
    clusterDAO.merge(clusterEntity);

    Long clusterId = clusterEntity.getClusterId();
    ConfigGroupEntity configGroupEntity1 = new ConfigGroupEntity();
    configGroupEntity1.setClusterEntity(clusterEntity);
    configGroupEntity1.setClusterId(clusterId);
    configGroupEntity1.setGroupName("group1");
    configGroupEntity1.setDescription("group1_desc");
    configGroupEntity1.setTag(SERVICE_NAME);
    //configGroupEntity1.setServiceName(OrmTestHelper.SERVICE_NAME);
    configGroupDAO.create(configGroupEntity1);
    ConfigGroupEntity group1 = configGroupDAO.findByName("group1");
    createServiceConfigWithGroup(SERVICE_NAME, "admin", 3L, 8L, 2222L, group1.getGroupId());
    // create some for HDP 0.2
    createServiceConfig(SERVICE_NAME, "admin", 4L, 5L, 50L);
    createServiceConfig(SERVICE_NAME, "admin", 5L, 6L, 60L);
    createServiceConfig("YARN", "admin", 2L, 7L, 70L);

    List<ServiceConfigEntity> serviceConfigs = serviceConfigDAO.getLatestServiceConfigs(clusterId, STACK_ID);
    assertEquals(3, serviceConfigs.size());
    configGroupDAO.remove(configGroupEntity1);

    serviceConfigs = serviceConfigDAO.getLatestServiceConfigs(clusterId, HDP_02);
    assertEquals(2, serviceConfigs.size());
  }

  @Test
  public void testConfiguration() throws Exception{
    initClusterEntities();
    ClusterEntity clusterEntity = clusterDAO.findByName(CLUSTER_NAME);

    assertTrue(!clusterEntity.getClusterConfigEntities().isEmpty());

    assertEquals(5, clusterEntity.getClusterConfigEntities().size());
  }

  /**
   * Tests the ability to find the latest configuration by stack, regardless of
   * whether that configuration is enabled.
   */
  @Test
  public void testGetLatestClusterConfigsByStack() throws Exception {
    initClusterEntities();

    ClusterEntity clusterEntity = clusterDAO.findByName(CLUSTER_NAME);

    // there should be 3 configs in HDP-0.1 for this cluster, none selected
    List<ClusterConfigEntity> clusterConfigEntities = clusterDAO.getLatestConfigurations(clusterEntity.getClusterId(), STACK_ID);
    assertEquals(1, clusterConfigEntities.size());

    ClusterConfigEntity entity = clusterConfigEntities.get(0);
    assertEquals("version3", entity.getTag());
    assertEquals("oozie-site", entity.getType());
    Assert.assertFalse(entity.isSelected());

    // there should be 2 configs in HDP-0.2 for this cluster, the latest being
    // selected
    clusterConfigEntities = clusterDAO.getLatestConfigurations(clusterEntity.getClusterId(), HDP_02);
    assertEquals(1, clusterConfigEntities.size());

    entity = clusterConfigEntities.get(0);
    assertEquals("version5", entity.getTag());
    assertEquals("oozie-site", entity.getType());
    assertTrue(entity.isSelected());
  }

  @Test
  public void testGetLatestClusterConfigsWithTypes() throws Exception {
    initClusterEntities();
    ClusterEntity clusterEntity = clusterDAO.findByName(CLUSTER_NAME);
    List<ClusterConfigEntity> entities = clusterDAO.getLatestConfigurationsWithTypes(clusterEntity.getClusterId(), STACK_ID, singletonList("oozie-site"));
    assertEquals(1, entities.size());
    entities = clusterDAO.getLatestConfigurationsWithTypes(clusterEntity.getClusterId(), STACK_ID, singletonList("no-such-type"));
    assertTrue(entities.isEmpty());
  }

  /**
   * Tests getting latest and enabled configurations when there is a
   * configuration group. Configurations for configuration groups are not
   * "selected" as they are merged in with the selected configuration. This can
   * cause problems if searching simply for the "latest" since it will pickup
   * the wrong configuration.
   *
   */
  @Test
  public void testGetClusterConfigsByStackCG() throws Exception {
    initClusterEntitiesWithConfigGroups();
    ClusterEntity clusterEntity = clusterDAO.findByName(CLUSTER_NAME);

    List<ConfigGroupEntity> configGroupEntities = configGroupDAO.findAllByTag("OOZIE");

    Long clusterId = clusterDAO.findByName(CLUSTER_NAME).getClusterId();

    assertNotNull(configGroupEntities);
    ConfigGroupEntity configGroupEntity = configGroupEntities.get(0);
    assertNotNull(configGroupEntity);
    assertEquals(CLUSTER_NAME, configGroupEntity.getClusterEntity().getClusterName());
    assertEquals(clusterId, configGroupEntity.getClusterEntity().getClusterId());
    assertEquals("oozie_server", configGroupEntity.getGroupName());
    assertEquals("OOZIE", configGroupEntity.getTag());
    assertEquals("oozie server", configGroupEntity.getDescription());

    // all 3 are HDP-0.1, but only the 2nd one is enabled
    List<ClusterConfigEntity> clusterConfigEntities = clusterDAO.getEnabledConfigsByStack(
        clusterEntity.getClusterId(), STACK_ID);

    assertEquals(1, clusterConfigEntities.size());

    ClusterConfigEntity configEntity = clusterConfigEntities.get(0);
    assertEquals("version2", configEntity.getTag());
    assertEquals("oozie-site", configEntity.getType());
    assertTrue(configEntity.isSelected());

    // this should still return the 2nd one since the 3rd one has never been
    // selected as its only for configuration groups
    clusterConfigEntities = clusterDAO.getLatestConfigurations(clusterEntity.getClusterId(),
        STACK_ID);

    configEntity = clusterConfigEntities.get(0);
    assertEquals("version2", configEntity.getTag());
    assertEquals("oozie-site", configEntity.getType());
    assertTrue(configEntity.isSelected());
  }


  /**
   * Tests that when there are multiple configurations for a stack, only the
   * selected ones get returned.
   */
  @Test
  public void testGetEnabledClusterConfigByStack() throws Exception {
    initClusterEntities();

    ClusterEntity cluster = clusterDAO.findByName(CLUSTER_NAME);
    Collection<ClusterConfigEntity> latestConfigs = clusterDAO.getEnabledConfigsByStack(cluster.getClusterId(), HDP_02);

    assertEquals(1, latestConfigs.size());
    for (ClusterConfigEntity e : latestConfigs) {
      assertEquals("version5", e.getTag());
      assertEquals("oozie-site", e.getType());
    }
  }

  /**
   * When the last configuration of a given configuration type to be stored into
   * the clusterconfig table is for a configuration group, that configuration is
   * not enabled. Therefore, it should be skipped when getting the enabled
   * configurations for a stack.
   */
  @Test
  public void testGetLatestClusterConfigByStackCG() throws Exception {
    initClusterEntitiesWithConfigGroups();

    ClusterEntity cluster = clusterDAO.findByName(CLUSTER_NAME);
    Collection<ClusterConfigEntity> latestConfigs = clusterDAO.getEnabledConfigsByStack(cluster.getClusterId(), STACK_ID);

    assertEquals(1, latestConfigs.size());
    for (ClusterConfigEntity e : latestConfigs) {
      assertEquals("version2", e.getTag());
      assertEquals("oozie-site", e.getType());
    }
  }

  @Test
  public void testGetLastServiceConfigsForServiceWhenAConfigGroupIsDeleted() throws Exception {
    initClusterEntitiesWithConfigGroups();
    ConfigGroupEntity configGroupEntity1 = new ConfigGroupEntity();
    ClusterEntity clusterEntity = clusterDAO.findByName(CLUSTER_NAME);
    Long clusterId = clusterEntity.getClusterId();
    configGroupEntity1.setClusterEntity(clusterEntity);
    configGroupEntity1.setClusterId(clusterEntity.getClusterId());
    configGroupEntity1.setGroupName("toTestDeleteGroup_OOZIE");
    configGroupEntity1.setDescription("toTestDeleteGroup_OOZIE_DESC");
    configGroupEntity1.setTag("OOZIE");
    //configGroupEntity1.setServiceName("OOZIE");
    configGroupDAO.create(configGroupEntity1);
    ConfigGroupEntity testDeleteGroup_OOZIE = configGroupDAO.findByName("toTestDeleteGroup_OOZIE");
    ServiceConfigEntity serviceConfigEntity = createServiceConfigWithGroup("OOZIE", "", 2L, 2L, System.currentTimeMillis(), testDeleteGroup_OOZIE.getGroupId());
    Collection<ServiceConfigEntity> serviceConfigEntityList = serviceConfigDAO.getLastServiceConfigsForService(clusterId, serviceConfigEntity.getServiceId());
    assertEquals(2, serviceConfigEntityList.size());
    configGroupDAO.remove(configGroupEntity1);
    serviceConfigEntityList = serviceConfigDAO.getLastServiceConfigsForService(clusterId, serviceConfigEntity.getServiceId());
    assertEquals(1, serviceConfigEntityList.size());
  }

  /**
   * Create a cluster with 5 configurations for Oozie. Each configuration will
   * have a tag of "version" plus a count. 3 configs will be for
   * {@link OrmTestHelper#STACK_ID}, and 2 will be for {@link #HDP_02}. Only the most recent
   * configuration, {@code version5}, will be enabled.
   */
  private void initClusterEntities() throws Exception{
    String userName = "admin";

    ServiceConfigEntity oozieServiceConfigEntity = createServiceConfig("OOZIE", userName, 1L, 1L, System.currentTimeMillis());
    ClusterEntity  clusterEntity = oozieServiceConfigEntity.getClusterEntity();
    StackEntity stackEntityHDP01 = stackDAO.find(STACK_ID);
    StackEntity stackEntityHDP02 = stackDAO.find(HDP_02);

    String oozieSite = "oozie-site";

    // create 5 Oozie Configs, with only the latest from HDP-0.2 being enabled
    int configsToCreate = 5;
    for (long i = 1; i <= configsToCreate; i++) {
      Thread.sleep(1);
      ClusterConfigEntity entity = new ClusterConfigEntity();
      entity.setClusterEntity(clusterEntity);
      entity.setClusterId(clusterEntity.getClusterId());
      entity.setType(oozieSite);
      entity.setVersion(i);
      entity.setTag("version"+i);
      entity.setTimestamp(new Date().getTime());

      // set selected to true to get the last selected timestamp populated
      entity.setSelected(true);

      // now set it to false
      entity.setSelected(false);

      entity.setStack(stackEntityHDP01);
      if (i >= 4) {
        entity.setStack(stackEntityHDP02);
        if (i == configsToCreate) {
          entity.setSelected(true);
        }
      }

      entity.setData("");
      clusterDAO.createConfig(entity);
      clusterEntity.getClusterConfigEntities().add(entity);
      clusterDAO.merge(clusterEntity);
    }
  }

  /**
   * Create a cluster with 3 configurations for Oozie in the {@link OrmTestHelper#STACK_ID}
   * stack. Only {@code version2}, will be enabled. {@code version3} will be for
   * a new configuration group.
   */
  private void initClusterEntitiesWithConfigGroups() throws Exception{
    String userName = "admin";

    ServiceConfigEntity oozieServiceConfigEntity = createServiceConfig("OOZIE", userName, 1L, 1L, System.currentTimeMillis());
    ClusterEntity  clusterEntity = oozieServiceConfigEntity.getClusterEntity();
    StackEntity stackEntityHDP01 = stackDAO.find(STACK_ID);
    String oozieSite = "oozie-site";

    // create 2 configurations for HDP-0.1
    int count = 2;
    for (long i = 1; i <= count; i++) {
      Thread.sleep(1);
      ClusterConfigEntity entity = new ClusterConfigEntity();
      entity.setClusterEntity(clusterEntity);
      entity.setClusterId(clusterEntity.getClusterId());
      entity.setType(oozieSite);
      entity.setVersion(i);
      entity.setTag("version"+i);
      entity.setTimestamp(new Date().getTime());
      entity.setStack(stackEntityHDP01);
      entity.setData("");
      entity.setSelected(false);

      if (i == count) {
        entity.setSelected(true);
      }

      clusterDAO.createConfig(entity);
      clusterEntity.getClusterConfigEntities().add(entity);
      clusterDAO.merge(clusterEntity);
    }

    ResourceTypeEntity resourceTypeEntity = resourceTypeDAO.findById(ResourceType.CLUSTER.getId());
    if (resourceTypeEntity == null) {
      resourceTypeEntity = new ResourceTypeEntity();
      resourceTypeEntity.setId(ResourceType.CLUSTER.getId());
      resourceTypeEntity.setName(ResourceType.CLUSTER.name());
      resourceTypeEntity = resourceTypeDAO.merge(resourceTypeEntity);
    }

    ResourceEntity resourceEntity = new ResourceEntity();
    resourceEntity.setResourceType(resourceTypeEntity);

    // create a configuration group for oozie
    ConfigGroupEntity configGroupEntity = new ConfigGroupEntity();
    configGroupEntity.setClusterEntity(clusterEntity);
    configGroupEntity.setClusterId(clusterEntity.getClusterId());
    configGroupEntity.setGroupName("oozie_server");
    configGroupEntity.setDescription("oozie server");
    configGroupEntity.setTag("OOZIE");
    configGroupDAO.create(configGroupEntity);

    // create a new configuration for oozie, for the config group
    ClusterConfigEntity configEntityForGroup = new ClusterConfigEntity();
    configEntityForGroup.setSelected(false);
    configEntityForGroup.setType("oozie-site");
    configEntityForGroup.setTag("version3");
    configEntityForGroup.setData("someData");
    configEntityForGroup.setAttributes("someAttributes");
    configEntityForGroup.setStack(stackEntityHDP01);

    List<ClusterConfigEntity> configEntitiesForGroup = new ArrayList<>();
    configEntitiesForGroup.add(configEntityForGroup);
    List<ConfigGroupConfigMappingEntity> configMappingEntities = new ArrayList<>();

    for (ClusterConfigEntity config : configEntitiesForGroup) {
      config.setClusterEntity(clusterEntity);
      config.setClusterId(clusterEntity.getClusterId());
      clusterDAO.createConfig(config);

      Thread.sleep(1);
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
