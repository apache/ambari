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
import org.apache.ambari.server.orm.entities.ClusterConfigEntity;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ConfigGroupConfigMappingEntity;
import org.apache.ambari.server.orm.entities.ConfigGroupEntity;
import org.apache.ambari.server.orm.entities.ConfigGroupHostMappingEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ConfigGroupDAOTest {
  private Injector injector;
  private ConfigGroupDAO configGroupDAO;
  private ClusterDAO clusterDAO;
  private ConfigGroupConfigMappingDAO configGroupConfigMappingDAO;
  private ConfigGroupHostMappingDAO configGroupHostMappingDAO;
  private HostDAO hostDAO;

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);

    clusterDAO = injector.getInstance(ClusterDAO.class);
    configGroupDAO = injector.getInstance(ConfigGroupDAO.class);
    configGroupConfigMappingDAO = injector.getInstance
      (ConfigGroupConfigMappingDAO.class);
    configGroupHostMappingDAO = injector.getInstance
      (ConfigGroupHostMappingDAO.class);
    hostDAO = injector.getInstance(HostDAO.class);
  }

  @After
  public void teardown() throws AmbariException {
    injector.getInstance(PersistService.class).stop();
  }

  private ConfigGroupEntity createConfigGroup(String clusterName,
         String groupName, String tag, String desc, List<HostEntity> hosts,
         List<ClusterConfigEntity> configs) throws Exception {
    ConfigGroupEntity configGroupEntity = new ConfigGroupEntity();

    ClusterEntity clusterEntity = new ClusterEntity();
    clusterEntity.setClusterName(clusterName);
    clusterDAO.create(clusterEntity);

    configGroupEntity.setClusterEntity(clusterEntity);
    configGroupEntity.setClusterId(clusterEntity.getClusterId());
    configGroupEntity.setGroupName(groupName);
    configGroupEntity.setDescription(desc);
    configGroupEntity.setTag(tag);

    configGroupDAO.create(configGroupEntity);

    if (hosts != null && !hosts.isEmpty()) {
      List<ConfigGroupHostMappingEntity> hostMappingEntities = new
        ArrayList<ConfigGroupHostMappingEntity>();

      for (HostEntity host : hosts) {
        hostDAO.create(host);

        ConfigGroupHostMappingEntity hostMappingEntity = new
          ConfigGroupHostMappingEntity();
        hostMappingEntity.setHostname(host.getHostName());
        hostMappingEntity.setHostEntity(host);
        hostMappingEntity.setConfigGroupEntity(configGroupEntity);
        hostMappingEntity.setConfigGroupId(configGroupEntity.getGroupId());
        hostMappingEntities.add(hostMappingEntity);
        configGroupHostMappingDAO.create(hostMappingEntity);
      }
      configGroupEntity.setConfigGroupHostMappingEntities(hostMappingEntities);
      configGroupDAO.merge(configGroupEntity);
    }

    if (configs != null && !configs.isEmpty()) {
      List<ConfigGroupConfigMappingEntity> configMappingEntities = new
        ArrayList<ConfigGroupConfigMappingEntity>();

      for (ClusterConfigEntity config : configs) {
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
    return configGroupEntity;
  }

  @Test
  public void testCreatePlaneJaneCG() throws Exception {
    ConfigGroupEntity configGroupEntity = createConfigGroup("c1", "hdfs-1",
      "HDFS", "some description", null, null);

    Assert.assertNotNull(configGroupEntity);
    Assert.assertEquals("c1", configGroupEntity.getClusterEntity().getClusterName());
    Assert.assertEquals(Long.valueOf(1), configGroupEntity.getClusterEntity()
      .getClusterId());
    Assert.assertEquals("hdfs-1", configGroupEntity.getGroupName());
    Assert.assertEquals("HDFS", configGroupEntity.getTag());
    Assert.assertEquals("some description", configGroupEntity.getDescription());
  }

  @Test
  public void testFindByTag() throws Exception {
    createConfigGroup("c1", "hdfs-1", "HDFS", "some description", null, null);

    List<ConfigGroupEntity> configGroupEntities = configGroupDAO.findAllByTag
      ("HDFS");

    Assert.assertNotNull(configGroupEntities);
    ConfigGroupEntity configGroupEntity = configGroupEntities.get(0);
    Assert.assertNotNull(configGroupEntity);
    Assert.assertEquals("c1", configGroupEntity.getClusterEntity().getClusterName());
    Assert.assertEquals(Long.valueOf(1), configGroupEntity.getClusterEntity()
      .getClusterId());
    Assert.assertEquals("hdfs-1", configGroupEntity.getGroupName());
    Assert.assertEquals("HDFS", configGroupEntity.getTag());
    Assert.assertEquals("some description", configGroupEntity.getDescription());
  }

  @Test
  public void testFindByName() throws Exception {
    createConfigGroup("c1", "hdfs-1", "HDFS", "some description", null, null);

    ConfigGroupEntity configGroupEntity = configGroupDAO.findByName("hdfs-1");

    Assert.assertNotNull(configGroupEntity);
    Assert.assertEquals("c1", configGroupEntity.getClusterEntity().getClusterName());
    Assert.assertEquals(Long.valueOf(1), configGroupEntity.getClusterEntity()
      .getClusterId());
    Assert.assertEquals("hdfs-1", configGroupEntity.getGroupName());
    Assert.assertEquals("HDFS", configGroupEntity.getTag());
    Assert.assertEquals("some description", configGroupEntity.getDescription());
  }

  @Test
  public void testFindByHost() throws Exception {
    List<HostEntity> hosts = new ArrayList<HostEntity>();
    HostEntity hostEntity = new HostEntity();
    hostEntity.setHostName("h1");
    hostEntity.setOsType("centOS");
    hosts.add(hostEntity);
    ConfigGroupEntity configGroupEntity =
      createConfigGroup("c1", "hdfs-1", "HDFS", "some description", hosts, null);

    Assert.assertNotNull(configGroupEntity);
    Assert.assertTrue(configGroupEntity.getConfigGroupHostMappingEntities()
      .size() > 0);
    Assert.assertNotNull(configGroupEntity
      .getConfigGroupHostMappingEntities().iterator().next());

    List<ConfigGroupHostMappingEntity> hostMappingEntities = configGroupHostMappingDAO
      .findByHost("h1");

    Assert.assertNotNull(hostMappingEntities);
    Assert.assertEquals("h1", hostMappingEntities.get(0).getHostname());
    Assert.assertEquals("centOS", hostMappingEntities.get(0).getHostEntity()
      .getOsType());
  }

  @Test
  public void testFindConfigsByGroup() throws Exception {
    ClusterConfigEntity configEntity = new ClusterConfigEntity();
    configEntity.setType("core-site");
    configEntity.setTag("version1");
    configEntity.setData("someData");

    List<ClusterConfigEntity> configEntities = new
      ArrayList<ClusterConfigEntity>();
    configEntities.add(configEntity);

    ConfigGroupEntity configGroupEntity =
      createConfigGroup("c1", "hdfs-1", "HDFS", "some description", null,
        configEntities);

    Assert.assertNotNull(configGroupEntity);
    Assert.assertTrue(configGroupEntity.getConfigGroupConfigMappingEntities()
      .size() > 0);

    List<ConfigGroupConfigMappingEntity> configMappingEntities =
      configGroupConfigMappingDAO.findByGroup(configGroupEntity.getGroupId());

    Assert.assertNotNull(configEntities);
    Assert.assertEquals("core-site", configEntities.get(0).getType());
    Assert.assertEquals("version1", configEntities.get(0).getTag());
    Assert.assertEquals("someData", configEntities.get(0).getData());
  }
}
