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

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ClusterVersionDAO;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.dao.PermissionDAO;
import org.apache.ambari.server.orm.dao.PrincipalDAO;
import org.apache.ambari.server.orm.dao.PrincipalTypeDAO;
import org.apache.ambari.server.orm.dao.PrivilegeDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.ResourceTypeDAO;
import org.apache.ambari.server.orm.dao.RoleAuthorizationDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.PrincipalTypeEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.RoleAuthorizationEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.state.stack.OsFamily;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.persist.PersistService;

import junit.framework.Assert;
/**
 * {@link org.apache.ambari.server.upgrade.UpgradeCatalog242} unit tests.
 */
public class UpgradeCatalog242Test {
  private Injector injector;
  private Provider<EntityManager> entityManagerProvider = createStrictMock(Provider.class);
  private EntityManager entityManager = createNiceMock(EntityManager.class);
  private UpgradeCatalogHelper upgradeCatalogHelper;
  private StackEntity desiredStackEntity;
  private AmbariManagementController amc = createNiceMock(AmbariManagementController.class);
  private AmbariMetaInfo metaInfo = createNiceMock(AmbariMetaInfo.class);
  private StackDAO stackDAO = createNiceMock(StackDAO.class);
  private RepositoryVersionDAO repositoryVersionDAO = createNiceMock(RepositoryVersionDAO.class);
  private ClusterVersionDAO clusterVersionDAO = createNiceMock(ClusterVersionDAO.class);
  private HostVersionDAO hostVersionDAO = createNiceMock(HostVersionDAO.class);
  private ClusterDAO clusterDAO = createNiceMock(ClusterDAO.class);

  private IMocksControl mocksControl = EasyMock.createControl();

  @Before
  public void init() {
    reset(entityManagerProvider);
    expect(entityManagerProvider.get()).andReturn(entityManager).anyTimes();
    replay(entityManagerProvider);
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);

    upgradeCatalogHelper = injector.getInstance(UpgradeCatalogHelper.class);
    // inject AmbariMetaInfo to ensure that stacks get populated in the DB
    injector.getInstance(AmbariMetaInfo.class);
    // load the stack entity
    StackDAO stackDAO = injector.getInstance(StackDAO.class);
    desiredStackEntity = stackDAO.find("HDP", "2.2.0");
  }

  @After
  public void tearDown() {
    injector.getInstance(PersistService.class).stop();
  }

  @Test
  public void testUpdateTablesForMysql() throws Exception{
    final DBAccessor dbAccessor = createNiceMock(DBAccessor.class);
    final Configuration configuration = createNiceMock(Configuration.class);

    Capture<DBAccessor.DBColumnInfo> extensionExtensionNameColumnChangeSize = newCapture();
    Capture<DBAccessor.DBColumnInfo> extensionExtensionVersionColumnChangeSize = newCapture();
    Capture<DBAccessor.DBColumnInfo> usersUserTypeColumnChangeSize = newCapture();
    Capture<DBAccessor.DBColumnInfo> usersUserNameColumnChangeSize = newCapture();
    Capture<DBAccessor.DBColumnInfo> hostRoleCommandRoleColumnChangeSize = newCapture();
    Capture<DBAccessor.DBColumnInfo> hostRoleCommandStatusColumnChangeSize = newCapture();
    Capture<DBAccessor.DBColumnInfo> blueprintBlueprintNameColumnChangeSize = newCapture();


    expect(configuration.getDatabaseType()).andReturn(Configuration.DatabaseType.MYSQL).once();

    dbAccessor.alterColumn(eq(UpgradeCatalog242.EXTENSION_TABLE), capture(extensionExtensionNameColumnChangeSize));
    dbAccessor.alterColumn(eq(UpgradeCatalog242.EXTENSION_TABLE), capture(extensionExtensionVersionColumnChangeSize));

    dbAccessor.alterColumn(eq(UpgradeCatalog242.USERS_TABLE), capture(usersUserTypeColumnChangeSize));
    dbAccessor.alterColumn(eq(UpgradeCatalog242.USERS_TABLE), capture(usersUserNameColumnChangeSize));

    dbAccessor.alterColumn(eq(UpgradeCatalog242.HOST_ROLE_COMMAND_TABLE), capture(hostRoleCommandRoleColumnChangeSize));
    dbAccessor.alterColumn(eq(UpgradeCatalog242.HOST_ROLE_COMMAND_TABLE), capture(hostRoleCommandStatusColumnChangeSize));

    dbAccessor.dropFKConstraint(eq(UpgradeCatalog242.HOST_GROUP_TABLE), eq("FK_hg_blueprint_name"));
    dbAccessor.dropFKConstraint(eq(UpgradeCatalog242.HOST_GROUP_TABLE), eq("FK_hostgroup_blueprint_name"));
    dbAccessor.dropFKConstraint(eq(UpgradeCatalog242.BLUEPRINT_CONFIGURATION), eq("FK_cfg_blueprint_name"));
    dbAccessor.dropFKConstraint(eq(UpgradeCatalog242.BLUEPRINT_CONFIGURATION), eq("FK_blueprint_configuration_blueprint_name"));
    dbAccessor.dropFKConstraint(eq(UpgradeCatalog242.BLUEPRINT_SETTING), eq("FK_blueprint_setting_blueprint_name"));
    dbAccessor.dropFKConstraint(eq(UpgradeCatalog242.BLUEPRINT_SETTING), eq("FK_blueprint_setting_name"));

    dbAccessor.alterColumn(eq(UpgradeCatalog242.BLUEPRINT_TABLE), capture(blueprintBlueprintNameColumnChangeSize));

    dbAccessor.addFKConstraint(eq(UpgradeCatalog242.HOST_GROUP_TABLE), eq("FK_hg_blueprint_name"),
            aryEq(new String[]{"blueprint_name"}), eq(UpgradeCatalog242.BLUEPRINT_TABLE), aryEq(new String[]{"blueprint_name"}), eq(false));

    dbAccessor.addFKConstraint(eq(UpgradeCatalog242.BLUEPRINT_CONFIGURATION), eq("FK_cfg_blueprint_name"),
            aryEq(new String[]{"blueprint_name"}), eq(UpgradeCatalog242.BLUEPRINT_TABLE), aryEq(new String[]{"blueprint_name"}), eq(false));

    dbAccessor.addFKConstraint(eq(UpgradeCatalog242.BLUEPRINT_SETTING), eq("FK_blueprint_setting_name"),
            aryEq(new String[]{"blueprint_name"}), eq(UpgradeCatalog242.BLUEPRINT_TABLE), aryEq(new String[]{"blueprint_name"}), eq(false));

    replay(dbAccessor, configuration);
    Module module = new Module() {
      @Override
      public void configure(Binder binder) {
        binder.bind(DBAccessor.class).toInstance(dbAccessor);
        binder.bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        binder.bind(EntityManager.class).toInstance(entityManager);
        binder.bind(Configuration.class).toInstance(configuration);
      }
    };

    Injector injector = Guice.createInjector(module);
    UpgradeCatalog242 upgradeCatalog242 = injector.getInstance(UpgradeCatalog242.class);
    upgradeCatalog242.executeDDLUpdates();

    DBAccessor.DBColumnInfo extensionExtensionNameInfo = extensionExtensionNameColumnChangeSize.getValue();
    Assert.assertNotNull(extensionExtensionNameInfo);
    Assert.assertEquals(UpgradeCatalog242.EXTENSION_NAME_COLUMN, extensionExtensionNameInfo.getName());
    Assert.assertEquals(new Integer(100), extensionExtensionNameInfo.getLength());
    Assert.assertEquals(String.class, extensionExtensionNameInfo.getType());
    Assert.assertEquals(null, extensionExtensionNameInfo.getDefaultValue());
    Assert.assertEquals(false, extensionExtensionNameInfo.isNullable());

    DBAccessor.DBColumnInfo extensionExtensionVersionInfo = extensionExtensionVersionColumnChangeSize.getValue();
    Assert.assertNotNull(extensionExtensionVersionInfo);
    Assert.assertEquals(UpgradeCatalog242.EXTENSION_VERSION_COLUMN, extensionExtensionVersionInfo.getName());
    Assert.assertEquals(new Integer(100), extensionExtensionVersionInfo.getLength());
    Assert.assertEquals(String.class, extensionExtensionVersionInfo.getType());
    Assert.assertEquals(null, extensionExtensionVersionInfo.getDefaultValue());
    Assert.assertEquals(false, extensionExtensionVersionInfo.isNullable());

    DBAccessor.DBColumnInfo usersUserTypeInfo = usersUserTypeColumnChangeSize.getValue();
    Assert.assertNotNull(usersUserTypeInfo);
    Assert.assertEquals(UpgradeCatalog242.USER_TYPE_COLUMN, usersUserTypeInfo.getName());
    Assert.assertEquals(new Integer(100), usersUserTypeInfo.getLength());
    Assert.assertEquals(String.class, usersUserTypeInfo.getType());
    Assert.assertEquals(null, usersUserTypeInfo.getDefaultValue());
    Assert.assertEquals(false, usersUserTypeInfo.isNullable());

    DBAccessor.DBColumnInfo usersUserNameInfo = usersUserNameColumnChangeSize.getValue();
    Assert.assertNotNull(usersUserNameInfo);
    Assert.assertEquals(UpgradeCatalog242.USER_NAME_COLUMN, usersUserNameInfo.getName());
    Assert.assertEquals(new Integer(100), usersUserNameInfo.getLength());
    Assert.assertEquals(String.class, usersUserNameInfo.getType());
    Assert.assertEquals(null, usersUserNameInfo.getDefaultValue());
    Assert.assertEquals(false, usersUserNameInfo.isNullable());

    DBAccessor.DBColumnInfo hostRoleCommandRoleInfo = hostRoleCommandRoleColumnChangeSize.getValue();
    Assert.assertNotNull(hostRoleCommandRoleInfo);
    Assert.assertEquals(UpgradeCatalog242.ROLE_COLUMN, hostRoleCommandRoleInfo.getName());
    Assert.assertEquals(new Integer(100), hostRoleCommandRoleInfo.getLength());
    Assert.assertEquals(String.class, hostRoleCommandRoleInfo.getType());
    Assert.assertEquals(null, hostRoleCommandRoleInfo.getDefaultValue());
    Assert.assertEquals(true, hostRoleCommandRoleInfo.isNullable());

    DBAccessor.DBColumnInfo hostRoleCommandStatusInfo = hostRoleCommandStatusColumnChangeSize.getValue();
    Assert.assertNotNull(hostRoleCommandStatusInfo);
    Assert.assertEquals(UpgradeCatalog242.STATUS_COLUMN, hostRoleCommandStatusInfo.getName());
    Assert.assertEquals(new Integer(100), hostRoleCommandStatusInfo.getLength());
    Assert.assertEquals(String.class, hostRoleCommandStatusInfo.getType());
    Assert.assertEquals(null, hostRoleCommandStatusInfo.getDefaultValue());
    Assert.assertEquals(true, hostRoleCommandStatusInfo.isNullable());

    DBAccessor.DBColumnInfo blueprintBlueprintNameInfo = blueprintBlueprintNameColumnChangeSize.getValue();
    Assert.assertNotNull(blueprintBlueprintNameInfo);
    Assert.assertEquals(UpgradeCatalog242.BLUEPRINT_NAME_COLUMN, blueprintBlueprintNameInfo.getName());
    Assert.assertEquals(new Integer(100), blueprintBlueprintNameInfo.getLength());
    Assert.assertEquals(String.class, blueprintBlueprintNameInfo.getType());
    Assert.assertEquals(null, blueprintBlueprintNameInfo.getDefaultValue());
    Assert.assertEquals(false, blueprintBlueprintNameInfo.isNullable());


    verify(dbAccessor, configuration);
  }

  @Test
  public void testExecuteDMLUpdates() throws Exception {
    Method addNewConfigurationsFromXml = AbstractUpgradeCatalog.class.getDeclaredMethod("addNewConfigurationsFromXml");
    Method convertRolePrincipals = UpgradeCatalog242.class.getDeclaredMethod("convertRolePrincipals");
    Method createRoleAuthorizations = UpgradeCatalog242.class.getDeclaredMethod("createRoleAuthorizations");

    UpgradeCatalog242 upgradeCatalog242 = createMockBuilder(UpgradeCatalog242.class)
        .addMockedMethod(addNewConfigurationsFromXml)
        .addMockedMethod(convertRolePrincipals)
        .addMockedMethod(createRoleAuthorizations)
        .createMock();


    upgradeCatalog242.addNewConfigurationsFromXml();
    expectLastCall().once();

    upgradeCatalog242.createRoleAuthorizations();
    expectLastCall().once();

    upgradeCatalog242.convertRolePrincipals();
    expectLastCall().once();

    replay(upgradeCatalog242);

    upgradeCatalog242.executeDMLUpdates();

    verify(upgradeCatalog242);
  }

  @Test
  public void testConvertRolePrincipals() throws AmbariException, SQLException {

    EasyMockSupport easyMockSupport = new EasyMockSupport();

    PrincipalEntity clusterAdministratorPrincipalEntity = easyMockSupport.createMock(PrincipalEntity.class);

    PermissionEntity clusterAdministratorPermissionEntity = easyMockSupport.createMock(PermissionEntity.class);
    expect(clusterAdministratorPermissionEntity.getPrincipal())
        .andReturn(clusterAdministratorPrincipalEntity)
        .once();

    PrincipalTypeEntity allClusterAdministratorPrincipalTypeEntity = easyMockSupport.createMock(PrincipalTypeEntity.class);

    PermissionDAO permissionDAO = easyMockSupport.createMock(PermissionDAO.class);
    expect(permissionDAO.findByName("CLUSTER.ADMINISTRATOR"))
        .andReturn(clusterAdministratorPermissionEntity)
        .once();
    expect(permissionDAO.findByName(anyString()))
        .andReturn(null)
        .anyTimes();

    PrincipalTypeDAO principalTypeDAO = easyMockSupport.createMock(PrincipalTypeDAO.class);
    expect(principalTypeDAO.findByName("ALL.CLUSTER.ADMINISTRATOR"))
        .andReturn(allClusterAdministratorPrincipalTypeEntity)
        .once();
    expect(principalTypeDAO.findByName(anyString()))
        .andReturn(null)
        .anyTimes();
    principalTypeDAO.remove(allClusterAdministratorPrincipalTypeEntity);
    expectLastCall().once();

    ResourceEntity allClusterAdministratorPrivilege1Resource = easyMockSupport.createMock(ResourceEntity.class);
    expect(allClusterAdministratorPrivilege1Resource.getId()).andReturn(1L).once();

    PrincipalEntity allClusterAdministratorPrivilege1Principal = easyMockSupport.createMock(PrincipalEntity.class);
    expect(allClusterAdministratorPrivilege1Principal.getId()).andReturn(1L).once();

    PermissionEntity allClusterAdministratorPrivilege1Permission = easyMockSupport.createMock(PermissionEntity.class);
    expect(allClusterAdministratorPrivilege1Permission.getId()).andReturn(1).once();

    PrivilegeEntity allClusterAdministratorPrivilege1  = easyMockSupport.createMock(PrivilegeEntity.class);
    expect(allClusterAdministratorPrivilege1.getId()).andReturn(1).atLeastOnce();
    expect(allClusterAdministratorPrivilege1.getResource()).andReturn(allClusterAdministratorPrivilege1Resource).once();
    expect(allClusterAdministratorPrivilege1.getPrincipal()).andReturn(allClusterAdministratorPrivilege1Principal).once();
    expect(allClusterAdministratorPrivilege1.getPermission()).andReturn(allClusterAdministratorPrivilege1Permission).once();
    allClusterAdministratorPrivilege1.setPrincipal(clusterAdministratorPrincipalEntity);
    expectLastCall().once();

    ResourceEntity allClusterAdministratorPrivilege2Resource = easyMockSupport.createMock(ResourceEntity.class);
    expect(allClusterAdministratorPrivilege2Resource.getId()).andReturn(2L).once();

    PrincipalEntity allClusterAdministratorPrivilege2Principal = easyMockSupport.createMock(PrincipalEntity.class);
    expect(allClusterAdministratorPrivilege2Principal.getId()).andReturn(2L).once();

    PermissionEntity allClusterAdministratorPrivilege2Permission = easyMockSupport.createMock(PermissionEntity.class);
    expect(allClusterAdministratorPrivilege2Permission.getId()).andReturn(2).once();

    PrivilegeEntity allClusterAdministratorPrivilege2  = easyMockSupport.createMock(PrivilegeEntity.class);
    expect(allClusterAdministratorPrivilege2.getId()).andReturn(2).atLeastOnce();
    expect(allClusterAdministratorPrivilege2.getResource()).andReturn(allClusterAdministratorPrivilege2Resource).once();
    expect(allClusterAdministratorPrivilege2.getPrincipal()).andReturn(allClusterAdministratorPrivilege2Principal).once();
    expect(allClusterAdministratorPrivilege2.getPermission()).andReturn(allClusterAdministratorPrivilege2Permission).once();
    allClusterAdministratorPrivilege2.setPrincipal(clusterAdministratorPrincipalEntity);
    expectLastCall().once();

    Set<PrivilegeEntity> allClusterAdministratorPrivileges = new HashSet<PrivilegeEntity>();
    allClusterAdministratorPrivileges.add(allClusterAdministratorPrivilege1);
    allClusterAdministratorPrivileges.add(allClusterAdministratorPrivilege2);

    PrincipalEntity allClusterAdministratorPrincipalEntity = easyMockSupport.createMock(PrincipalEntity.class);
    expect(allClusterAdministratorPrincipalEntity.getPrivileges())
        .andReturn(allClusterAdministratorPrivileges)
        .once();

    List<PrincipalEntity> allClusterAdministratorPrincipals = new ArrayList<PrincipalEntity>();
    allClusterAdministratorPrincipals.add(allClusterAdministratorPrincipalEntity);

    PrincipalDAO principalDAO = easyMockSupport.createMock(PrincipalDAO.class);
    expect(principalDAO.findByPrincipalType("ALL.CLUSTER.ADMINISTRATOR"))
        .andReturn(allClusterAdministratorPrincipals)
        .once();
    principalDAO.remove(allClusterAdministratorPrincipalEntity);
    expectLastCall().once();


    PrivilegeDAO privilegeDAO = easyMockSupport.createMock(PrivilegeDAO.class);
    expect(privilegeDAO.merge(allClusterAdministratorPrivilege1))
        .andReturn(allClusterAdministratorPrivilege1)
        .once();
    expect(privilegeDAO.merge(allClusterAdministratorPrivilege2))
        .andReturn(allClusterAdministratorPrivilege2)
        .once();

    Injector injector = easyMockSupport.createNiceMock(Injector.class);
    expect(injector.getInstance(PrincipalTypeDAO.class)).andReturn(principalTypeDAO).atLeastOnce();
    expect(injector.getInstance(PrincipalDAO.class)).andReturn(principalDAO).atLeastOnce();
    expect(injector.getInstance(PermissionDAO.class)).andReturn(permissionDAO).atLeastOnce();
    expect(injector.getInstance(PrivilegeDAO.class)).andReturn(privilegeDAO).atLeastOnce();

    easyMockSupport.replayAll();
    UpgradeCatalog242 upgradeCatalog = new UpgradeCatalog242(injector);
    injector.injectMembers(upgradeCatalog);
    upgradeCatalog.convertRolePrincipals();
    easyMockSupport.verifyAll();
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
    new UpgradeCatalog242(injector).createRoleAuthorizations();
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
