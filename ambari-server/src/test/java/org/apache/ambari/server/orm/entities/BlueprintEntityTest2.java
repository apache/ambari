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

package org.apache.ambari.server.orm.entities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.internal.ProvisionAction;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.BlueprintDAO;
import org.apache.ambari.server.state.SecurityType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * Test saving and loading a blueprint with all associated entities.
 */
public class BlueprintEntityTest2 {

  public static final String BLUEPRINT_NAME = "test-blueprint";
  @Inject
  private Injector injector;
  @Inject
  private BlueprintDAO blueprintDAO;


  @Before
  public void setup() {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    injector.injectMembers(this);
    injector.getInstance(AmbariMetaInfo.class);
  }

  @After
  public void teardown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  /**
   * Test loading and saving a blueprint without stack defined (Ambari 3.0 blueprints normally define mpacks, not stacks)
   */
  @Test
  public void testCreateAndLoadBlueprint() {
    blueprintDAO.create(createTestBlueprint());
    verifyBlueprint(blueprintDAO.findByName(BLUEPRINT_NAME));
  }

  private void verifyBlueprint(BlueprintEntity blueprintEntity) {
    assertEquals(1, blueprintEntity.getMpackInstances().size());

    BlueprintMpackInstanceEntity mpackInstanceEntity =
      blueprintEntity.getMpackInstances().iterator().next();
    assertEquals("HDPCORE", mpackInstanceEntity.getMpackName());
    assertEquals(1, mpackInstanceEntity.getConfigurations().size());

    MpackInstanceConfigEntity mpackConfigEntity =
      mpackInstanceEntity.getConfigurations().iterator().next();
    assertEquals("configdata", mpackConfigEntity.getConfigData());
    assertEquals("zk-env.sh", mpackConfigEntity.getType());

    assertEquals(1, blueprintEntity.getHostGroups().size());
    HostGroupEntity hostGroup = blueprintEntity.getHostGroups().iterator().next();

    Collection<HostGroupComponentEntity> hgComponents = hostGroup.getComponents();
    Map<String, HostGroupComponentEntity> hgComponentMap =
      hgComponents.stream().collect(Collectors.toMap(HostGroupComponentEntity::getName, Function.identity()));
    assertEquals(ImmutableSet.of("ZOOKEEPER_CLIENT", "HDFS_CLIENT"), hgComponentMap.keySet());
    assertEquals(ProvisionAction.INSTALL_ONLY.name(), hgComponentMap.get("HDFS_CLIENT").getProvisionAction());
    assertEquals("HDPCORE", hgComponentMap.get("ZOOKEEPER_CLIENT").getMpackName());
    assertEquals("ZK1", hgComponentMap.get("ZOOKEEPER_CLIENT").getServiceName());

    assertEquals(1, hostGroup.getConfigurations().size());
    HostGroupConfigEntity hostGroupConfig = hostGroup.getConfigurations().iterator().next();
    assertEquals("hdfs-site", hostGroupConfig.getType());

    assertEquals(1, mpackInstanceEntity.getServiceInstances().size());
    MpackInstanceServiceEntity service = mpackInstanceEntity.getServiceInstances().iterator().next();
    assertEquals("ZK1", service.getName());
    assertEquals("ZOOKEEPER", service.getType());
    assertSame(mpackInstanceEntity, service.getMpackInstance());

    assertEquals(1, service.getConfigurations().size());
    assertEquals("hadoop-env", service.getConfigurations().iterator().next().getType());
  }

  private BlueprintEntity createTestBlueprint() {
    BlueprintEntity blueprintEntity = new BlueprintEntity();
    blueprintEntity.setBlueprintName(BLUEPRINT_NAME);
    blueprintEntity.setSecurityType(SecurityType.NONE);

    BlueprintMpackInstanceEntity mpackInstanceEntity = new BlueprintMpackInstanceEntity();
    mpackInstanceEntity.setBlueprint(blueprintEntity);
    mpackInstanceEntity.setMpackName("HDPCORE");
    mpackInstanceEntity.setMpackVersion("3.0.0.0");
    mpackInstanceEntity.setMpackUri("http://hdpcore.org/3.0.0.0");

    MpackInstanceConfigEntity mpackConfigEntity = new MpackInstanceConfigEntity();
    mpackConfigEntity.setMpackInstance(mpackInstanceEntity);
    mpackConfigEntity.setConfigAttributes("attributes");
    mpackConfigEntity.setConfigData("configdata");
    mpackConfigEntity.setType("zk-env.sh");
    mpackInstanceEntity.getConfigurations().add(mpackConfigEntity);

    HostGroupEntity hostGroupEntity = new HostGroupEntity();
    hostGroupEntity.setBlueprintEntity(blueprintEntity);
    hostGroupEntity.setName("hostgroup1");

    HostGroupConfigEntity hgConfigEntity = new HostGroupConfigEntity();
    hgConfigEntity.setHostGroupEntity(hostGroupEntity);
    hgConfigEntity.setType("hdfs-site");
    hgConfigEntity.setConfigAttributes("attributes");
    hgConfigEntity.setConfigData("data");
    hostGroupEntity.getConfigurations().add(hgConfigEntity);

    HostGroupComponentEntity hgComponentEntity1 = new HostGroupComponentEntity();
    hgComponentEntity1.setHostGroupEntity(hostGroupEntity);
    hgComponentEntity1.setName("HDFS_CLIENT");
    hgComponentEntity1.setProvisionAction(ProvisionAction.INSTALL_ONLY.name());
    hostGroupEntity.addComponent(hgComponentEntity1);

    HostGroupComponentEntity hgComponentEntity2 = new HostGroupComponentEntity();
    hgComponentEntity2.setHostGroupEntity(hostGroupEntity);
    hgComponentEntity2.setName("ZOOKEEPER_CLIENT");
    hgComponentEntity2.setMpackName("HDPCORE");
    hgComponentEntity2.setMpackVersion("3.0.0.0");
    hgComponentEntity2.setServiceName("ZK1");
    hostGroupEntity.addComponent(hgComponentEntity2);

    MpackInstanceServiceEntity blueprintService = new MpackInstanceServiceEntity();
    blueprintService.setMpackInstance(mpackInstanceEntity);
    blueprintService.setName("ZK1");
    blueprintService.setType("ZOOKEEPER");
    mpackInstanceEntity.getServiceInstances().add(blueprintService);

    MpackServiceConfigEntity blueprintServiceConfigEntity = new MpackServiceConfigEntity();
    blueprintServiceConfigEntity.setService(blueprintService);
    blueprintServiceConfigEntity.setType("hadoop-env");
    blueprintServiceConfigEntity.setConfigAttributes("attributes");
    blueprintServiceConfigEntity.setConfigData("data");
    blueprintService.getConfigurations().add(blueprintServiceConfigEntity);

    blueprintEntity.getMpackInstances().add(mpackInstanceEntity);
    blueprintEntity.getHostGroups().add(hostGroupEntity);

    return blueprintEntity;
  }
}