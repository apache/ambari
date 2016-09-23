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

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import junit.framework.Assert;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariManagementControllerImpl;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.MaintenanceStateHelper;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.PermissionDAO;
import org.apache.ambari.server.orm.dao.ResourceTypeDAO;
import org.apache.ambari.server.orm.dao.RoleAuthorizationDAO;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.RoleAuthorizationEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.stack.OsFamily;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertTrue;

/**
 * {@link UpgradeCatalog250} unit tests.
 */
public class UpgradeCatalog250Test {

//  private Injector injector;
  private Provider<EntityManager> entityManagerProvider = createStrictMock(Provider.class);
  private EntityManager entityManager = createNiceMock(EntityManager.class);

  @Before
  public void init() {
    reset(entityManagerProvider);
    expect(entityManagerProvider.get()).andReturn(entityManager).anyTimes();
    replay(entityManagerProvider);

  }

  @After
  public void tearDown() {
  }

  @Test
  public void testExecuteDDLUpdates() throws Exception {
    final DBAccessor dbAccessor = createNiceMock(DBAccessor.class);

    dbAccessor.addUniqueConstraint("host_version", "UQ_host_repo", "repo_version_id", "host_id");
    expectLastCall().once();

    replay(dbAccessor);

    Module module = new Module() {
      @Override
      public void configure(Binder binder) {
        binder.bind(DBAccessor.class).toInstance(dbAccessor);
        binder.bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        binder.bind(EntityManager.class).toInstance(entityManager);
      }
    };

    Injector injector = Guice.createInjector(module);
    UpgradeCatalog250 upgradeCatalog250 = injector.getInstance(UpgradeCatalog250.class);
    upgradeCatalog250.executeDDLUpdates();

    verify(dbAccessor);
  }

  @Test
  public void testExecuteDMLUpdates() throws Exception {
    Method updateAmsConfigs = UpgradeCatalog250.class.getDeclaredMethod("updateAMSConfigs");
    Method createRoleAuthorizations = UpgradeCatalog250.class.getDeclaredMethod("createRoleAuthorizations");

    UpgradeCatalog250 upgradeCatalog250 = createMockBuilder(UpgradeCatalog250.class)
        .addMockedMethod(updateAmsConfigs)
        .addMockedMethod(createRoleAuthorizations)
        .createMock();

    upgradeCatalog250.updateAMSConfigs();
    expectLastCall().once();

    upgradeCatalog250.createRoleAuthorizations();
    expectLastCall().once();

    replay(upgradeCatalog250);

    upgradeCatalog250.executeDMLUpdates();

    verify(upgradeCatalog250);
  }

  @Test
  public void testAmsEnvUpdateConfigs() throws Exception{

    Map<String, String> oldPropertiesAmsEnv = new HashMap<String, String>() {
      {
        put("content", "\n" +
          "# AMS Collector heapsize\n" +
          "export AMS_COLLECTOR_HEAPSIZE={{metrics_collector_heapsize}}\n" +
          "\n" +
          "# HBase normalizer enabled\n" +
          "export AMS_HBASE_NORMALIZER_ENABLED={{ams_hbase_normalizer_enabled}}\n" +
          "\n" +
          "# HBase compaction policy enabled\n" +
          "export HBASE_FIFO_COMPACTION_POLICY_ENABLED={{ams_hbase_fifo_compaction_policy_enabled}}\n" +
          "\n" +
          "# HBase Tables Initialization check enabled\n" +
          "export AMS_HBASE_INIT_CHECK_ENABLED={{ams_hbase_init_check_enabled}}\n");
      }
    };
    Map<String, String> newPropertiesAmsEnv = new HashMap<String, String>() {
      {
        put("content", "\n" +
          "# AMS Collector heapsize\n" +
          "export AMS_COLLECTOR_HEAPSIZE={{metrics_collector_heapsize}}\n" +
          "\n" +
          "# HBase Tables Initialization check enabled\n" +
          "export AMS_HBASE_INIT_CHECK_ENABLED={{ams_hbase_init_check_enabled}}\n");
      }
    };
    EasyMockSupport easyMockSupport = new EasyMockSupport();

    Clusters clusters = easyMockSupport.createNiceMock(Clusters.class);
    final Cluster cluster = easyMockSupport.createNiceMock(Cluster.class);
    Config mockAmsEnv = easyMockSupport.createNiceMock(Config.class);

    expect(clusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", cluster);
    }}).once();
    expect(cluster.getDesiredConfigByType("ams-env")).andReturn(mockAmsEnv).atLeastOnce();
    expect(mockAmsEnv.getProperties()).andReturn(oldPropertiesAmsEnv).anyTimes();

    Injector injector = easyMockSupport.createNiceMock(Injector.class);
    expect(injector.getInstance(Gson.class)).andReturn(null).anyTimes();
    expect(injector.getInstance(MaintenanceStateHelper.class)).andReturn(null).anyTimes();
    expect(injector.getInstance(KerberosHelper.class)).andReturn(createNiceMock(KerberosHelper.class)).anyTimes();

    replay(injector, clusters, mockAmsEnv, cluster);

    AmbariManagementControllerImpl controller = createMockBuilder(AmbariManagementControllerImpl.class)
      .addMockedMethod("createConfiguration")
      .addMockedMethod("getClusters", new Class[] { })
      .addMockedMethod("createConfig")
      .withConstructor(createNiceMock(ActionManager.class), clusters, injector)
      .createNiceMock();

    Injector injector2 = easyMockSupport.createNiceMock(Injector.class);
    Capture<Map> propertiesCapture = EasyMock.newCapture();

    expect(injector2.getInstance(AmbariManagementController.class)).andReturn(controller).anyTimes();
    expect(controller.getClusters()).andReturn(clusters).anyTimes();
    expect(controller.createConfig(anyObject(Cluster.class), anyString(), capture(propertiesCapture), anyString(),
      anyObject(Map.class))).andReturn(createNiceMock(Config.class)).once();

    replay(controller, injector2);
    new UpgradeCatalog250(injector2).updateAMSConfigs();
    easyMockSupport.verifyAll();

    Map<String, String> updatedProperties = propertiesCapture.getValue();
    assertTrue(Maps.difference(newPropertiesAmsEnv, updatedProperties).areEqual());
  }

  @Test
  public void testCreateRoleAuthorizations() throws AmbariException, SQLException {

    EasyMockSupport easyMockSupport = new EasyMockSupport();

    ResourceTypeEntity ambariResourceTypeEntity = easyMockSupport.createMock(ResourceTypeEntity.class);

    ResourceTypeEntity clusterResourceTypeEntity = easyMockSupport.createMock(ResourceTypeEntity.class);

    Collection<RoleAuthorizationEntity> ambariAdministratorAuthorizations = new ArrayList<RoleAuthorizationEntity>();
    Collection<RoleAuthorizationEntity> clusterAdministratorAuthorizations = new ArrayList<RoleAuthorizationEntity>();

    PermissionEntity clusterAdministratorPermissionEntity = easyMockSupport.createMock(PermissionEntity.class);
    expect(clusterAdministratorPermissionEntity.getAuthorizations())
        .andReturn(clusterAdministratorAuthorizations)
        .times(1);

    PermissionEntity ambariAdministratorPermissionEntity = easyMockSupport.createMock(PermissionEntity.class);
    expect(ambariAdministratorPermissionEntity.getAuthorizations())
        .andReturn(ambariAdministratorAuthorizations)
        .times(2);

    PermissionDAO permissionDAO = easyMockSupport.createMock(PermissionDAO.class);
    expect(permissionDAO.findPermissionByNameAndType("AMBARI.ADMINISTRATOR", ambariResourceTypeEntity))
        .andReturn(ambariAdministratorPermissionEntity)
        .times(2);
    expect(permissionDAO.findPermissionByNameAndType("CLUSTER.ADMINISTRATOR", clusterResourceTypeEntity))
        .andReturn(clusterAdministratorPermissionEntity)
        .times(1);
    expect(permissionDAO.merge(ambariAdministratorPermissionEntity))
        .andReturn(ambariAdministratorPermissionEntity)
        .times(2);
    expect(permissionDAO.merge(clusterAdministratorPermissionEntity))
        .andReturn(clusterAdministratorPermissionEntity)
        .times(1);

    ResourceTypeDAO resourceTypeDAO = easyMockSupport.createMock(ResourceTypeDAO.class);
    expect(resourceTypeDAO.findByName("AMBARI")).andReturn(ambariResourceTypeEntity).times(2);
    expect(resourceTypeDAO.findByName("CLUSTER")).andReturn(clusterResourceTypeEntity).times(1);

    RoleAuthorizationDAO roleAuthorizationDAO = easyMockSupport.createMock(RoleAuthorizationDAO.class);
    expect(roleAuthorizationDAO.findById("CLUSTER.RUN_CUSTOM_COMMAND")).andReturn(null).times(1);
    expect(roleAuthorizationDAO.findById("AMBARI.RUN_CUSTOM_COMMAND")).andReturn(null).times(1);

    Capture<RoleAuthorizationEntity> captureClusterRunCustomCommandEntity = newCapture();
    roleAuthorizationDAO.create(capture(captureClusterRunCustomCommandEntity));
    expectLastCall().times(1);

    Capture<RoleAuthorizationEntity> captureAmbariRunCustomCommandEntity = newCapture();
    roleAuthorizationDAO.create(capture(captureAmbariRunCustomCommandEntity));
    expectLastCall().times(1);

    Injector injector = easyMockSupport.createNiceMock(Injector.class);
    expect(injector.getInstance(RoleAuthorizationDAO.class)).andReturn(roleAuthorizationDAO).atLeastOnce();
    expect(injector.getInstance(PermissionDAO.class)).andReturn(permissionDAO).atLeastOnce();
    expect(injector.getInstance(ResourceTypeDAO.class)).andReturn(resourceTypeDAO).atLeastOnce();

    easyMockSupport.replayAll();
    new UpgradeCatalog250(injector).createRoleAuthorizations();
    easyMockSupport.verifyAll();

    RoleAuthorizationEntity ambariRunCustomCommandEntity = captureAmbariRunCustomCommandEntity.getValue();
    RoleAuthorizationEntity clusterRunCustomCommandEntity = captureClusterRunCustomCommandEntity.getValue();

    Assert.assertEquals("AMBARI.RUN_CUSTOM_COMMAND", ambariRunCustomCommandEntity.getAuthorizationId());
    Assert.assertEquals("Perform custom administrative actions", ambariRunCustomCommandEntity.getAuthorizationName());

    Assert.assertEquals("CLUSTER.RUN_CUSTOM_COMMAND", clusterRunCustomCommandEntity.getAuthorizationId());
    Assert.assertEquals("Perform custom cluster-level actions", clusterRunCustomCommandEntity.getAuthorizationName());

    Assert.assertEquals(2, ambariAdministratorAuthorizations.size());
    Assert.assertTrue(ambariAdministratorAuthorizations.contains(clusterRunCustomCommandEntity));
    Assert.assertTrue(ambariAdministratorAuthorizations.contains(ambariRunCustomCommandEntity));

    Assert.assertEquals(1, clusterAdministratorAuthorizations.size());
    Assert.assertTrue(clusterAdministratorAuthorizations.contains(clusterRunCustomCommandEntity));
  }
}
