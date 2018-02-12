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

import static org.apache.ambari.server.upgrade.UpgradeCatalog270.ADMINPRIVILEGE_PERMISSION_ID_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.ADMINPRIVILEGE_PRINCIPAL_ID_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.ADMINPRIVILEGE_PRIVILEGE_ID_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.ADMINPRIVILEGE_RESOURCE_ID_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.ADMINPRIVILEGE_TABLE;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.AMBARI_CONFIGURATION_CATEGORY_NAME_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.AMBARI_CONFIGURATION_PROPERTY_NAME_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.AMBARI_CONFIGURATION_PROPERTY_VALUE_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.AMBARI_CONFIGURATION_TABLE;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.AMBARI_INFRA_NEW_NAME;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.AMBARI_INFRA_OLD_NAME;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.COMPONENT_DESIRED_STATE_TABLE;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.COMPONENT_NAME_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.COMPONENT_STATE_TABLE;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.FK_KKP_HOST_ID;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.FK_KKP_KEYTAB_PATH;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.FK_KKP_PRINCIPAL_NAME;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.FK_KKP_SERVICE_PRINCIPAL;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.HOSTS_TABLE;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.HOST_ID_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.KERBEROS_KEYTAB_PRINCIPAL_TABLE;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.KERBEROS_KEYTAB_TABLE;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.KERBEROS_PRINCIPAL_HOST_TABLE;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.KERBEROS_PRINCIPAL_TABLE;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.KEYTAB_PATH_FIELD;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.KKP_ID_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.KKP_MAPPING_SERVICE_TABLE;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.MEMBERS_GROUP_ID_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.MEMBERS_MEMBER_ID_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.MEMBERS_TABLE;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.MEMBERS_USER_ID_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.PK_KERBEROS_KEYTAB;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.PK_KKP;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.PK_KKP_MAPPING_SERVICE;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.PRINCIPAL_NAME_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.REQUEST_DISPLAY_STATUS_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.REQUEST_TABLE;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.SECURITY_STATE_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.SERVICE_DESIRED_STATE_TABLE;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.SERVICE_NAME_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.STAGE_DISPLAY_STATUS_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.STAGE_STATUS_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.STAGE_TABLE;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.UNIQUE_USERS_0_INDEX;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.UNI_KKP;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.USERS_CONSECUTIVE_FAILURES_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.USERS_DISPLAY_NAME_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.USERS_LDAP_USER_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.USERS_LOCAL_USERNAME_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.USERS_TABLE;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.USERS_USER_ID_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.USERS_USER_NAME_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.USERS_USER_PASSWORD_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.USERS_USER_TYPE_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.USERS_VERSION_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.USER_AUTHENTICATION_AUTHENTICATION_KEY_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.USER_AUTHENTICATION_AUTHENTICATION_TYPE_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.USER_AUTHENTICATION_CREATE_TIME_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.USER_AUTHENTICATION_PRIMARY_KEY;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.USER_AUTHENTICATION_TABLE;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.USER_AUTHENTICATION_UPDATE_TIME_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.USER_AUTHENTICATION_USER_AUTHENTICATION_ID_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.USER_AUTHENTICATION_USER_AUTHENTICATION_USERS_FOREIGN_KEY;
import static org.apache.ambari.server.upgrade.UpgradeCatalog270.USER_AUTHENTICATION_USER_ID_COLUMN;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.niceMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.startsWith;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.ActionDBAccessor;
import org.apache.ambari.server.actionmanager.ActionDBAccessorImpl;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactoryImpl;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.actionmanager.StageFactoryImpl;
import org.apache.ambari.server.agent.stomp.AgentConfigsHolder;
import org.apache.ambari.server.agent.stomp.MetadataHolder;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.audit.AuditLogger;
import org.apache.ambari.server.audit.AuditLoggerDefaultImpl;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AbstractRootServiceResponseFactory;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariManagementControllerImpl;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.KerberosHelperImpl;
import org.apache.ambari.server.controller.MaintenanceStateHelper;
import org.apache.ambari.server.controller.RootServiceResponseFactory;
import org.apache.ambari.server.controller.ServiceConfigVersionResponse;
import org.apache.ambari.server.controller.internal.AmbariServerConfigurationCategory;
import org.apache.ambari.server.events.MetadataUpdateEvent;
import org.apache.ambari.server.hooks.HookService;
import org.apache.ambari.server.hooks.users.UserHookService;
import org.apache.ambari.server.ldap.domain.AmbariLdapConfigurationKeys;
import org.apache.ambari.server.metadata.CachedRoleCommandOrderProvider;
import org.apache.ambari.server.metadata.RoleCommandOrderProvider;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.AmbariConfigurationDAO;
import org.apache.ambari.server.scheduler.ExecutionScheduler;
import org.apache.ambari.server.security.SecurityHelper;
import org.apache.ambari.server.security.encryption.CredentialStoreService;
import org.apache.ambari.server.orm.dao.ArtifactDAO;
import org.apache.ambari.server.orm.entities.ArtifactEntity;
import org.apache.ambari.server.serveraction.kerberos.PrepareKerberosIdentitiesServerAction;
import org.apache.ambari.server.stack.StackManagerFactory;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.ServiceImpl;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.UpgradeContextFactory;
import org.apache.ambari.server.state.cluster.ClusterFactory;
import org.apache.ambari.server.state.cluster.ClusterImpl;
import org.apache.ambari.server.state.cluster.ClustersImpl;
import org.apache.ambari.server.state.host.HostFactory;
import org.apache.ambari.server.state.host.HostImpl;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.testutils.PartialNiceMockBinder;
import org.apache.ambari.server.topology.PersistedState;
import org.apache.ambari.server.topology.PersistedStateImpl;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.MockType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.persist.UnitOfWork;

@RunWith(EasyMockRunner.class)
public class UpgradeCatalog270Test {
  public static final Gson GSON = new Gson();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock(type = MockType.STRICT)
  private Provider<EntityManager> entityManagerProvider;

  @Mock(type = MockType.NICE)
  private Injector injector;

  @Mock(type = MockType.NICE)
  private EntityManager entityManager;

  @Mock(type = MockType.DEFAULT)
  private DBAccessor dbAccessor;

  @Mock(type = MockType.NICE)
  private OsFamily osFamily;

  @Mock(type = MockType.NICE)
  private Config config;

  @Mock(type = MockType.NICE)
  private ActionManager actionManager;

  @Mock(type = MockType.NICE)
  private Clusters clusters;

  @Mock(type = MockType.NICE)
  private Cluster cluster;

  @Mock(type = MockType.NICE)
  AmbariConfigurationDAO ambariConfigurationDao;

  @Mock(type = MockType.NICE)
  ArtifactDAO artifactDAO;

  @Mock(type = MockType.NICE)
  private AmbariManagementController ambariManagementController;

  @Before
  public void init() {
    reset(entityManagerProvider, injector);

    expect(entityManagerProvider.get()).andReturn(entityManager).anyTimes();

    expect(injector.getInstance(Gson.class)).andReturn(null).anyTimes();
    expect(injector.getInstance(MaintenanceStateHelper.class)).andReturn(null).anyTimes();

    replay(entityManagerProvider, injector);
  }

  @After
  public void tearDown() {
  }

  @Test
  public void testExecuteDMLUpdates() throws Exception {
    Method addNewConfigurationsFromXml = AbstractUpgradeCatalog.class.getDeclaredMethod("addNewConfigurationsFromXml");
    Method showHcatDeletedUserMessage = UpgradeCatalog270.class.getDeclaredMethod("showHcatDeletedUserMessage");
    Method setStatusOfStagesAndRequests = UpgradeCatalog270.class.getDeclaredMethod("setStatusOfStagesAndRequests");
    Method updateLogSearchConfigs = UpgradeCatalog270.class.getDeclaredMethod("updateLogSearchConfigs");
    Method updateKerberosConfigurations = UpgradeCatalog270.class.getDeclaredMethod("updateKerberosConfigurations");
    Method updateHostComponentLastStateTable = UpgradeCatalog270.class.getDeclaredMethod("updateHostComponentLastStateTable");
    Method upgradeLdapConfiguration = UpgradeCatalog270.class.getDeclaredMethod("upgradeLdapConfiguration");
    Method createRoleAuthorizations = UpgradeCatalog270.class.getDeclaredMethod("createRoleAuthorizations");
    Method addUserAuthenticationSequence = UpgradeCatalog270.class.getDeclaredMethod("addUserAuthenticationSequence");
    Method renameAmbariInfra = UpgradeCatalog270.class.getDeclaredMethod("renameAmbariInfra");
    Method updateKerberosDescriptorArtifacts = UpgradeCatalog270.class.getSuperclass().getDeclaredMethod("updateKerberosDescriptorArtifacts");
    UpgradeCatalog270 upgradeCatalog270 = createMockBuilder(UpgradeCatalog270.class)
        .addMockedMethod(showHcatDeletedUserMessage)
        .addMockedMethod(addNewConfigurationsFromXml)
        .addMockedMethod(setStatusOfStagesAndRequests)
        .addMockedMethod(updateLogSearchConfigs)
        .addMockedMethod(updateKerberosConfigurations)
        .addMockedMethod(updateHostComponentLastStateTable)
        .addMockedMethod(upgradeLdapConfiguration)
        .addMockedMethod(createRoleAuthorizations)
        .addMockedMethod(addUserAuthenticationSequence)
        .addMockedMethod(renameAmbariInfra)
        .addMockedMethod(updateKerberosDescriptorArtifacts)
        .createMock();


    upgradeCatalog270.addNewConfigurationsFromXml();
    expectLastCall().once();

    upgradeCatalog270.showHcatDeletedUserMessage();
    expectLastCall().once();

    upgradeCatalog270.createRoleAuthorizations();
    expectLastCall().once();

    upgradeCatalog270.setStatusOfStagesAndRequests();
    expectLastCall().once();

    upgradeCatalog270.updateLogSearchConfigs();
    upgradeCatalog270.updateHostComponentLastStateTable();
    expectLastCall().once();

    upgradeCatalog270.updateKerberosConfigurations();
    expectLastCall().once();

    upgradeCatalog270.upgradeLdapConfiguration();
    expectLastCall().once();

    upgradeCatalog270.addUserAuthenticationSequence();
    expectLastCall().once();

    upgradeCatalog270.renameAmbariInfra();
    expectLastCall().once();

    upgradeCatalog270.updateKerberosDescriptorArtifacts();
    expectLastCall().once();

    replay(upgradeCatalog270);

    upgradeCatalog270.executeDMLUpdates();

    verify(upgradeCatalog270);
  }

  @Test
  public void testExecuteDDLUpdates() throws Exception {
    // updateStageTable
    Capture<DBAccessor.DBColumnInfo> updateStageTableCaptures = newCapture(CaptureType.ALL);
    dbAccessor.addColumn(eq(STAGE_TABLE), capture(updateStageTableCaptures));
    expectLastCall().once();
    dbAccessor.addColumn(eq(STAGE_TABLE), capture(updateStageTableCaptures));
    expectLastCall().once();
    dbAccessor.addColumn(eq(REQUEST_TABLE), capture(updateStageTableCaptures));
    expectLastCall().once();

    // addOpsDisplayNameColumnToHostRoleCommand
    Capture<DBAccessor.DBColumnInfo> hrcOpsDisplayNameColumn = newCapture();
    dbAccessor.addColumn(eq(UpgradeCatalog270.HOST_ROLE_COMMAND_TABLE), capture(hrcOpsDisplayNameColumn));
    expectLastCall().once();

    Capture<DBAccessor.DBColumnInfo> lastValidColumn = newCapture();
    dbAccessor.addColumn(eq(UpgradeCatalog270.COMPONENT_STATE_TABLE), capture(lastValidColumn));

    // removeSecurityState
    dbAccessor.dropColumn(COMPONENT_DESIRED_STATE_TABLE, SECURITY_STATE_COLUMN);
    expectLastCall().once();
    dbAccessor.dropColumn(COMPONENT_STATE_TABLE, SECURITY_STATE_COLUMN);
    expectLastCall().once();
    dbAccessor.dropColumn(SERVICE_DESIRED_STATE_TABLE, SECURITY_STATE_COLUMN);
    expectLastCall().once();

    // addAmbariConfigurationTable
    Capture<List<DBAccessor.DBColumnInfo>> ambariConfigurationTableColumns = newCapture();
    dbAccessor.createTable(eq(AMBARI_CONFIGURATION_TABLE), capture(ambariConfigurationTableColumns));
    expectLastCall().once();
    dbAccessor.addPKConstraint(AMBARI_CONFIGURATION_TABLE, "PK_ambari_configuration", AMBARI_CONFIGURATION_CATEGORY_NAME_COLUMN, AMBARI_CONFIGURATION_PROPERTY_NAME_COLUMN);
    expectLastCall().once();

    // upgradeUserTable - create user_authentication table
    Capture<List<DBAccessor.DBColumnInfo>> createUserAuthenticationTableCaptures = newCapture(CaptureType.ALL);
    Capture<List<DBAccessor.DBColumnInfo>> createMembersTableCaptures = newCapture(CaptureType.ALL);
    Capture<List<DBAccessor.DBColumnInfo>> createAdminPrincipalTableCaptures = newCapture(CaptureType.ALL);
    Capture<DBAccessor.DBColumnInfo> updateUserTableCaptures = newCapture(CaptureType.ALL);
    Capture<DBAccessor.DBColumnInfo> alterUserTableCaptures = newCapture(CaptureType.ALL);

    // Any return value will work here as long as a SQLException is not thrown.
    expect(dbAccessor.getColumnType(USERS_TABLE, USERS_USER_TYPE_COLUMN)).andReturn(0).anyTimes();

    prepareCreateUserAuthenticationTable(dbAccessor, createUserAuthenticationTableCaptures);
    prepareUpdateGroupMembershipRecords(dbAccessor, createMembersTableCaptures);
    prepareUpdateAdminPrivilegeRecords(dbAccessor, createAdminPrincipalTableCaptures);
    prepareUpdateUsersTable(dbAccessor, updateUserTableCaptures, alterUserTableCaptures);
    // upgradeKerberosTables
    Capture<List<DBAccessor.DBColumnInfo>> kerberosKeytabColumnsCapture = newCapture();
    dbAccessor.createTable(eq(KERBEROS_KEYTAB_TABLE), capture(kerberosKeytabColumnsCapture));
    expectLastCall().once();
    dbAccessor.addPKConstraint(KERBEROS_KEYTAB_TABLE, PK_KERBEROS_KEYTAB, KEYTAB_PATH_FIELD);
    expectLastCall().once();

    Capture<List<DBAccessor.DBColumnInfo>> kerberosKeytabPrincipalColumnsCapture = newCapture();
    dbAccessor.createTable(eq(KERBEROS_KEYTAB_PRINCIPAL_TABLE), capture(kerberosKeytabPrincipalColumnsCapture));
    expectLastCall().once();
    dbAccessor.addPKConstraint(KERBEROS_KEYTAB_PRINCIPAL_TABLE, PK_KKP, KKP_ID_COLUMN);
    expectLastCall().once();
    dbAccessor.addUniqueConstraint(KERBEROS_KEYTAB_PRINCIPAL_TABLE, UNI_KKP, KEYTAB_PATH_FIELD, PRINCIPAL_NAME_COLUMN, HOST_ID_COLUMN);
    expectLastCall().once();

    Capture<List<DBAccessor.DBColumnInfo>> mappingColumnsCapture = newCapture();
    dbAccessor.createTable(eq(KKP_MAPPING_SERVICE_TABLE), capture(mappingColumnsCapture));
    expectLastCall().once();
    dbAccessor.addPKConstraint(KKP_MAPPING_SERVICE_TABLE, PK_KKP_MAPPING_SERVICE, KKP_ID_COLUMN, SERVICE_NAME_COLUMN, COMPONENT_NAME_COLUMN);
    expectLastCall().once();

    dbAccessor.addFKConstraint(KERBEROS_KEYTAB_PRINCIPAL_TABLE, FK_KKP_KEYTAB_PATH, KEYTAB_PATH_FIELD, KERBEROS_KEYTAB_TABLE, KEYTAB_PATH_FIELD, false);
    expectLastCall().once();
    dbAccessor.addFKConstraint(KERBEROS_KEYTAB_PRINCIPAL_TABLE, FK_KKP_HOST_ID,HOST_ID_COLUMN, HOSTS_TABLE, HOST_ID_COLUMN, false);
    expectLastCall().once();
    dbAccessor.addFKConstraint(KERBEROS_KEYTAB_PRINCIPAL_TABLE, FK_KKP_PRINCIPAL_NAME, PRINCIPAL_NAME_COLUMN, KERBEROS_PRINCIPAL_TABLE, PRINCIPAL_NAME_COLUMN, false);
    expectLastCall().once();
    dbAccessor.addFKConstraint(KKP_MAPPING_SERVICE_TABLE, FK_KKP_SERVICE_PRINCIPAL, KKP_ID_COLUMN, KERBEROS_KEYTAB_PRINCIPAL_TABLE, KKP_ID_COLUMN, false);
    expectLastCall().once();

    Connection c = niceMock(Connection.class);
    Statement s = niceMock(Statement.class);
    expect(s.executeQuery(anyString())).andReturn(null).once();
    expect(c.createStatement()).andReturn(s).once();
    expect(dbAccessor.getConnection()).andReturn(c).once();

    dbAccessor.dropTable(KERBEROS_PRINCIPAL_HOST_TABLE);

    replay(dbAccessor);

    Injector injector = Guice.createInjector(getTestGuiceModule());
    UpgradeCatalog270 upgradeCatalog270 = injector.getInstance(UpgradeCatalog270.class);
    upgradeCatalog270.executeDDLUpdates();

    // Validate updateStageTableCaptures
    Assert.assertTrue(updateStageTableCaptures.hasCaptured());
    validateColumns(updateStageTableCaptures.getValues(),
        Arrays.asList(
            new DBAccessor.DBColumnInfo(STAGE_STATUS_COLUMN, String.class, 255, HostRoleStatus.PENDING, false),
            new DBAccessor.DBColumnInfo(STAGE_DISPLAY_STATUS_COLUMN, String.class, 255, HostRoleStatus.PENDING, false),
            new DBAccessor.DBColumnInfo(REQUEST_DISPLAY_STATUS_COLUMN, String.class, 255, HostRoleStatus.PENDING, false))
    );

    DBAccessor.DBColumnInfo capturedOpsDisplayNameColumn = hrcOpsDisplayNameColumn.getValue();
    Assert.assertEquals(UpgradeCatalog270.HRC_OPS_DISPLAY_NAME_COLUMN, capturedOpsDisplayNameColumn.getName());
    Assert.assertEquals(null, capturedOpsDisplayNameColumn.getDefaultValue());
    Assert.assertEquals(String.class, capturedOpsDisplayNameColumn.getType());

    // Ambari configuration table addition...
    Assert.assertTrue(ambariConfigurationTableColumns.hasCaptured());
    validateColumns(ambariConfigurationTableColumns.getValue(),
        Arrays.asList(
            new DBAccessor.DBColumnInfo(AMBARI_CONFIGURATION_CATEGORY_NAME_COLUMN, String.class, 100, null, false),
            new DBAccessor.DBColumnInfo(AMBARI_CONFIGURATION_PROPERTY_NAME_COLUMN, String.class, 100, null, false),
            new DBAccessor.DBColumnInfo(AMBARI_CONFIGURATION_PROPERTY_VALUE_COLUMN, String.class, 255, null, true))
    );

    List<DBAccessor.DBColumnInfo> columns = ambariConfigurationTableColumns.getValue();
    Assert.assertEquals(3, columns.size());

    for (DBAccessor.DBColumnInfo column : columns) {
      String columnName = column.getName();

      if (AMBARI_CONFIGURATION_CATEGORY_NAME_COLUMN.equals(columnName)) {
        Assert.assertEquals(String.class, column.getType());
        Assert.assertEquals(Integer.valueOf(100), column.getLength());
        Assert.assertEquals(null, column.getDefaultValue());
        Assert.assertFalse(column.isNullable());
      } else if (AMBARI_CONFIGURATION_PROPERTY_NAME_COLUMN.equals(columnName)) {
        Assert.assertEquals(String.class, column.getType());
        Assert.assertEquals(Integer.valueOf(100), column.getLength());
        Assert.assertEquals(null, column.getDefaultValue());
        Assert.assertFalse(column.isNullable());
      } else if (AMBARI_CONFIGURATION_PROPERTY_VALUE_COLUMN.equals(columnName)) {
        Assert.assertEquals(String.class, column.getType());
        Assert.assertEquals(Integer.valueOf(255), column.getLength());
        Assert.assertEquals(null, column.getDefaultValue());
        Assert.assertTrue(column.isNullable());
      } else {
        Assert.fail("Unexpected column name: " + columnName);
      }
    }
    // Ambari configuration table addition...

    DBAccessor.DBColumnInfo capturedLastValidColumn = lastValidColumn.getValue();
    Assert.assertEquals(upgradeCatalog270.COMPONENT_LAST_STATE_COLUMN, capturedLastValidColumn.getName());
    Assert.assertEquals(State.UNKNOWN, capturedLastValidColumn.getDefaultValue());
    Assert.assertEquals(String.class, capturedLastValidColumn.getType());

    validateCreateUserAuthenticationTable(createUserAuthenticationTableCaptures);
    validateUpdateGroupMembershipRecords(createMembersTableCaptures);
    validateUpdateAdminPrivilegeRecords(createAdminPrincipalTableCaptures);
    validateUpdateUsersTable(updateUserTableCaptures, alterUserTableCaptures);

    verify(dbAccessor);
  }

  private Module getTestGuiceModule() {
    Module module = new AbstractModule() {
      @Override
      public void configure() {
        PartialNiceMockBinder.newBuilder().addConfigsBindings().addFactoriesInstallBinding().build().configure(binder());

        bind(DBAccessor.class).toInstance(dbAccessor);
        bind(OsFamily.class).toInstance(osFamily);
        bind(EntityManager.class).toInstance(entityManager);
        bind(AmbariConfigurationDAO.class).toInstance(ambariConfigurationDao);
        bind(PersistedState.class).toInstance(mock(PersistedStateImpl.class));
        bind(Clusters.class).toInstance(mock(ClustersImpl.class));
        bind(SecurityHelper.class).toInstance(mock(SecurityHelper.class));
        bind(HostRoleCommandFactory.class).to(HostRoleCommandFactoryImpl.class);
        bind(ActionDBAccessor.class).toInstance(createNiceMock(ActionDBAccessorImpl.class));
        bind(UnitOfWork.class).toInstance(createNiceMock(UnitOfWork.class));
        bind(RoleCommandOrderProvider.class).to(CachedRoleCommandOrderProvider.class);
        bind(StageFactory.class).to(StageFactoryImpl.class);
        bind(AuditLogger.class).toInstance(createNiceMock(AuditLoggerDefaultImpl.class));
        bind(PasswordEncoder.class).toInstance(new StandardPasswordEncoder());
        bind(HookService.class).to(UserHookService.class);
        bind(ServiceComponentHostFactory.class).toInstance(createNiceMock(ServiceComponentHostFactory.class));
        bind(AbstractRootServiceResponseFactory.class).to(RootServiceResponseFactory.class);
        bind(CredentialStoreService.class).toInstance(createNiceMock(CredentialStoreService.class));
        bind(AmbariManagementController.class).toInstance(createNiceMock(AmbariManagementControllerImpl.class));
        bind(ExecutionScheduler.class).toInstance(createNiceMock(ExecutionScheduler.class));
        bind(AmbariMetaInfo.class).toInstance(createNiceMock(AmbariMetaInfo.class));
        bind(KerberosHelper.class).toInstance(createNiceMock(KerberosHelperImpl.class));
        bind(StackManagerFactory.class).toInstance(createNiceMock(StackManagerFactory.class));

        install(new FactoryModuleBuilder().implement(
            Host.class, HostImpl.class).build(HostFactory.class));
        install(new FactoryModuleBuilder().implement(
            Cluster.class, ClusterImpl.class).build(ClusterFactory.class));
        install(new FactoryModuleBuilder().build(UpgradeContextFactory.class));
        install(new FactoryModuleBuilder().implement(
            Service.class, ServiceImpl.class).build(ServiceFactory.class));
//        binder.bind(Configuration.class).toInstance(configuration);
//        binder.bind(AmbariManagementController.class).toInstance(ambariManagementController);
      }
    };
    return module;
  }

  private void prepareCreateUserAuthenticationTable(DBAccessor dbAccessor, Capture<List<DBAccessor.DBColumnInfo>> capturedData)
      throws SQLException {

    String temporaryTableName = USER_AUTHENTICATION_TABLE + "_tmp";

    dbAccessor.dropTable(eq(temporaryTableName));
    expectLastCall().times(2);
    dbAccessor.createTable(eq(temporaryTableName), capture(capturedData));
    expectLastCall().once();

    expect(dbAccessor.executeUpdate(startsWith("insert into " + temporaryTableName))).andReturn(1).once();
    expect(dbAccessor.executeUpdate(startsWith("update " + temporaryTableName))).andReturn(1).once();

    dbAccessor.createTable(eq(USER_AUTHENTICATION_TABLE), capture(capturedData));
    expectLastCall().once();
    dbAccessor.addPKConstraint(USER_AUTHENTICATION_TABLE, USER_AUTHENTICATION_PRIMARY_KEY, USER_AUTHENTICATION_USER_AUTHENTICATION_ID_COLUMN);
    expectLastCall().once();
    dbAccessor.addFKConstraint(USER_AUTHENTICATION_TABLE, USER_AUTHENTICATION_USER_AUTHENTICATION_USERS_FOREIGN_KEY, USER_AUTHENTICATION_USER_ID_COLUMN, USERS_TABLE, USERS_USER_ID_COLUMN, false);
    expectLastCall().once();

    expect(dbAccessor.executeUpdate(startsWith("insert into " + USER_AUTHENTICATION_TABLE))).andReturn(1).once();
  }

  private void validateCreateUserAuthenticationTable(Capture<List<DBAccessor.DBColumnInfo>> capturedData) {
    Assert.assertTrue(capturedData.hasCaptured());
    List<List<DBAccessor.DBColumnInfo>> capturedValues = capturedData.getValues();
    Assert.assertEquals(2, capturedValues.size());
    for (List<DBAccessor.DBColumnInfo> capturedValue : capturedValues) {
      validateColumns(capturedValue,
          Arrays.asList(
              new DBAccessor.DBColumnInfo(USER_AUTHENTICATION_USER_AUTHENTICATION_ID_COLUMN, Long.class, null, null, false),
              new DBAccessor.DBColumnInfo(USER_AUTHENTICATION_USER_ID_COLUMN, Long.class, null, null, false),
              new DBAccessor.DBColumnInfo(USER_AUTHENTICATION_AUTHENTICATION_TYPE_COLUMN, String.class, 50, null, false),
              new DBAccessor.DBColumnInfo(USER_AUTHENTICATION_AUTHENTICATION_KEY_COLUMN, Clob.class, null, null, true),
              new DBAccessor.DBColumnInfo(USER_AUTHENTICATION_CREATE_TIME_COLUMN, Timestamp.class, null, null, true),
              new DBAccessor.DBColumnInfo(USER_AUTHENTICATION_UPDATE_TIME_COLUMN, Timestamp.class, null, null, true)
          )
      );
    }
  }

  private void prepareUpdateGroupMembershipRecords(DBAccessor dbAccessor, Capture<List<DBAccessor.DBColumnInfo>> capturedData)
      throws SQLException {
    String temporaryTableName = MEMBERS_TABLE + "_tmp";

    dbAccessor.dropTable(eq(temporaryTableName));
    expectLastCall().times(2);
    dbAccessor.createTable(eq(temporaryTableName), capture(capturedData));
    expectLastCall().once();

    expect(dbAccessor.executeUpdate(startsWith("insert into " + temporaryTableName))).andReturn(1).once();

    dbAccessor.truncateTable(MEMBERS_TABLE);
    expectLastCall().once();

    expect(dbAccessor.executeUpdate(startsWith("insert into " + MEMBERS_TABLE))).andReturn(1).once();
  }

  private void validateUpdateGroupMembershipRecords(Capture<List<DBAccessor.DBColumnInfo>> capturedData) {
    Assert.assertTrue(capturedData.hasCaptured());
    List<List<DBAccessor.DBColumnInfo>> capturedValues = capturedData.getValues();
    Assert.assertEquals(1, capturedValues.size());
    for (List<DBAccessor.DBColumnInfo> capturedValue : capturedValues) {
      validateColumns(capturedValue,
          Arrays.asList(
              new DBAccessor.DBColumnInfo(MEMBERS_MEMBER_ID_COLUMN, Long.class, null, null, false),
              new DBAccessor.DBColumnInfo(MEMBERS_USER_ID_COLUMN, Long.class, null, null, false),
              new DBAccessor.DBColumnInfo(MEMBERS_GROUP_ID_COLUMN, Long.class, null, null, false)
          )
      );
    }
  }

  private void prepareUpdateAdminPrivilegeRecords(DBAccessor dbAccessor, Capture<List<DBAccessor.DBColumnInfo>> capturedData)
      throws SQLException {
    String temporaryTableName = ADMINPRIVILEGE_TABLE + "_tmp";

    dbAccessor.dropTable(eq(temporaryTableName));
    expectLastCall().times(2);
    dbAccessor.createTable(eq(temporaryTableName), capture(capturedData));
    expectLastCall().once();

    expect(dbAccessor.executeUpdate(startsWith("insert into " + temporaryTableName))).andReturn(1).once();

    dbAccessor.truncateTable(ADMINPRIVILEGE_TABLE);
    expectLastCall().once();

    expect(dbAccessor.executeUpdate(startsWith("insert into " + ADMINPRIVILEGE_TABLE))).andReturn(1).once();
  }

  private void validateUpdateAdminPrivilegeRecords(Capture<List<DBAccessor.DBColumnInfo>> capturedData) {
    Assert.assertTrue(capturedData.hasCaptured());
    List<List<DBAccessor.DBColumnInfo>> capturedValues = capturedData.getValues();
    Assert.assertEquals(1, capturedValues.size());
    for (List<DBAccessor.DBColumnInfo> capturedValue : capturedValues) {
      validateColumns(capturedValue,
          Arrays.asList(
              new DBAccessor.DBColumnInfo(ADMINPRIVILEGE_PRIVILEGE_ID_COLUMN, Long.class, null, null, false),
              new DBAccessor.DBColumnInfo(ADMINPRIVILEGE_PERMISSION_ID_COLUMN, Long.class, null, null, false),
              new DBAccessor.DBColumnInfo(ADMINPRIVILEGE_RESOURCE_ID_COLUMN, Long.class, null, null, false),
              new DBAccessor.DBColumnInfo(ADMINPRIVILEGE_PRINCIPAL_ID_COLUMN, Long.class, null, null, false)
          )
      );
    }
  }

  private void prepareUpdateUsersTable(DBAccessor dbAccessor, Capture<DBAccessor.DBColumnInfo> updateUserTableCaptures, Capture<DBAccessor.DBColumnInfo> alterUserTableCaptures)
      throws SQLException {

    expect(dbAccessor.executeUpdate(startsWith("delete from " + USERS_TABLE))).andReturn(1).once();

    dbAccessor.dropUniqueConstraint(USERS_TABLE, UNIQUE_USERS_0_INDEX);
    expectLastCall().once();
    dbAccessor.dropColumn(USERS_TABLE, USERS_USER_TYPE_COLUMN);
    expectLastCall().once();
    dbAccessor.dropColumn(USERS_TABLE, USERS_LDAP_USER_COLUMN);
    expectLastCall().once();
    dbAccessor.dropColumn(USERS_TABLE, USERS_USER_PASSWORD_COLUMN);
    expectLastCall().once();

    dbAccessor.addColumn(eq(USERS_TABLE), capture(updateUserTableCaptures));
    expectLastCall().atLeastOnce();

    expect(dbAccessor.executeUpdate(startsWith("update " + USERS_TABLE))).andReturn(1).once();


    dbAccessor.alterColumn(eq(USERS_TABLE), capture(alterUserTableCaptures));
    expectLastCall().atLeastOnce();

    dbAccessor.addUniqueConstraint(USERS_TABLE, UNIQUE_USERS_0_INDEX, USERS_USER_NAME_COLUMN);
    expectLastCall().once();
  }

  private void validateUpdateUsersTable(Capture<DBAccessor.DBColumnInfo> updateUserTableCaptures, Capture<DBAccessor.DBColumnInfo> alterUserTableCaptures) {
    Assert.assertTrue(updateUserTableCaptures.hasCaptured());
    validateColumns(updateUserTableCaptures.getValues(),
        Arrays.asList(
            new DBAccessor.DBColumnInfo(USERS_CONSECUTIVE_FAILURES_COLUMN, Integer.class, null, 0, false),
            new DBAccessor.DBColumnInfo(USERS_DISPLAY_NAME_COLUMN, String.class, 255, null, true),
            new DBAccessor.DBColumnInfo(USERS_LOCAL_USERNAME_COLUMN, String.class, 255, null, true),
            new DBAccessor.DBColumnInfo(USERS_VERSION_COLUMN, Long.class, null, 0, false)
        )
    );

    Assert.assertTrue(alterUserTableCaptures.hasCaptured());
    validateColumns(alterUserTableCaptures.getValues(),
        Arrays.asList(
            new DBAccessor.DBColumnInfo(USERS_DISPLAY_NAME_COLUMN, String.class, 255, null, false),
            new DBAccessor.DBColumnInfo(USERS_LOCAL_USERNAME_COLUMN, String.class, 255, null, false)
        )
    );
  }

  private void validateColumns(List<DBAccessor.DBColumnInfo> capturedColumns, List<DBAccessor.DBColumnInfo> expectedColumns) {
    Assert.assertEquals(expectedColumns.size(), capturedColumns.size());

    // copy these so we can alter them...
    expectedColumns = new ArrayList<>(expectedColumns);
    capturedColumns = new ArrayList<>(capturedColumns);

    Iterator<DBAccessor.DBColumnInfo> capturedColumnIterator = capturedColumns.iterator();
    while (capturedColumnIterator.hasNext()) {
      DBAccessor.DBColumnInfo capturedColumnInfo = capturedColumnIterator.next();

      Iterator<DBAccessor.DBColumnInfo> expectedColumnIterator = expectedColumns.iterator();
      while (expectedColumnIterator.hasNext()) {
        DBAccessor.DBColumnInfo expectedColumnInfo = expectedColumnIterator.next();

        if (expectedColumnInfo.equals(capturedColumnInfo)) {
          expectedColumnIterator.remove();
          capturedColumnIterator.remove();
          break;
        }
      }
    }

    assertTrue("Not all captured columns were expected", capturedColumns.isEmpty());
    assertTrue("Not all expected columns were captured", expectedColumns.isEmpty());
  }


  @Test
  public void testLogSearchUpdateConfigs() throws Exception {
    reset(clusters, cluster);
    expect(clusters.getClusters()).andReturn(ImmutableMap.of("normal", cluster)).once();

    EasyMockSupport easyMockSupport = new EasyMockSupport();

    Injector injector2 = easyMockSupport.createNiceMock(Injector.class);
    AmbariManagementControllerImpl controller = createMockBuilder(AmbariManagementControllerImpl.class)
        .addMockedMethod("createConfiguration")
        .addMockedMethod("getClusters", new Class[]{})
        .addMockedMethod("createConfig")
        .createNiceMock();
    ConfigHelper configHelper = createMockBuilder(ConfigHelper.class)
        .addMockedMethod("removeConfigsByType")
        .addMockedMethod("createConfigType", Cluster.class, StackId.class, AmbariManagementController.class,
            String.class, Map.class, String.class, String.class)
        .createMock();

    expect(injector2.getInstance(AmbariManagementController.class)).andReturn(controller).anyTimes();
    expect(injector2.getInstance(ConfigHelper.class)).andReturn(configHelper).anyTimes();
    expect(controller.getClusters()).andReturn(clusters).anyTimes();

    Config confSomethingElse1 = easyMockSupport.createNiceMock(Config.class);
    expect(confSomethingElse1.getType()).andReturn("something-else-1");
    Config confSomethingElse2 = easyMockSupport.createNiceMock(Config.class);
    expect(confSomethingElse2.getType()).andReturn("something-else-2");
    Config confLogSearchConf1 = easyMockSupport.createNiceMock(Config.class);
    expect(confLogSearchConf1.getType()).andReturn("service-1-logsearch-conf");
    Config confLogSearchConf2 = easyMockSupport.createNiceMock(Config.class);
    expect(confLogSearchConf2.getType()).andReturn("service-2-logsearch-conf");

    Collection<Config> configs = Arrays.asList(confSomethingElse1, confLogSearchConf1, confSomethingElse2, confLogSearchConf2);

    expect(cluster.getAllConfigs()).andReturn(configs).atLeastOnce();
    configHelper.removeConfigsByType(cluster, "service-1-logsearch-conf");
    expectLastCall().once();
    configHelper.removeConfigsByType(cluster, "service-2-logsearch-conf");
    expectLastCall().once();
    configHelper.createConfigType(anyObject(Cluster.class), anyObject(StackId.class), eq(controller),
        eq("logsearch-common-properties"), eq(Collections.emptyMap()), eq("ambari-upgrade"),
        eq("Updated logsearch-common-properties during Ambari Upgrade from 2.6.0 to 3.0.0"));
    expectLastCall().once();

    Map<String, String> oldLogSearchProperties = ImmutableMap.of(
        "logsearch.logfeeder.include.default.level", "FATAL,ERROR,WARN"
    );

    Map<String, String> expectedLogFeederProperties = ImmutableMap.of(
        "logfeeder.include.default.level", "FATAL,ERROR,WARN"
    );

    Config logFeederPropertiesConf = easyMockSupport.createNiceMock(Config.class);
    expect(cluster.getDesiredConfigByType("logfeeder-properties")).andReturn(logFeederPropertiesConf).times(2);
    expect(logFeederPropertiesConf.getProperties()).andReturn(Collections.emptyMap()).once();
    Capture<Map<String, String>> logFeederPropertiesCapture = EasyMock.newCapture();
    expect(controller.createConfig(anyObject(Cluster.class), anyObject(StackId.class), eq("logfeeder-properties"), capture(logFeederPropertiesCapture),
        anyString(), EasyMock.anyObject())).andReturn(config).once();

    Config logSearchPropertiesConf = easyMockSupport.createNiceMock(Config.class);
    expect(cluster.getDesiredConfigByType("logsearch-properties")).andReturn(logSearchPropertiesConf).times(2);
    expect(logSearchPropertiesConf.getProperties()).andReturn(oldLogSearchProperties).times(2);
    Capture<Map<String, String>> logSearchPropertiesCapture = EasyMock.newCapture();
    expect(controller.createConfig(anyObject(Cluster.class), anyObject(StackId.class), eq("logsearch-properties"), capture(logSearchPropertiesCapture),
        anyString(), EasyMock.anyObject())).andReturn(config).once();

    Map<String, String> oldLogFeederLog4j = ImmutableMap.of(
        "content", "<!DOCTYPE log4j:configuration SYSTEM \"log4j.dtd\">");

    Map<String, String> expectedLogFeederLog4j = ImmutableMap.of(
        "content", "<!DOCTYPE log4j:configuration SYSTEM \"http://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/xml/doc-files/log4j.dtd\">");

    Config logFeederLog4jConf = easyMockSupport.createNiceMock(Config.class);
    expect(cluster.getDesiredConfigByType("logfeeder-log4j")).andReturn(logFeederLog4jConf).atLeastOnce();
    expect(logFeederLog4jConf.getProperties()).andReturn(oldLogFeederLog4j).anyTimes();
    Capture<Map<String, String>> logFeederLog4jCapture = EasyMock.newCapture();
    expect(controller.createConfig(anyObject(Cluster.class), anyObject(StackId.class), anyString(), capture(logFeederLog4jCapture), anyString(),
        EasyMock.anyObject())).andReturn(config).once();

    Map<String, String> oldLogSearchLog4j = ImmutableMap.of(
        "content", "<!DOCTYPE log4j:configuration SYSTEM \"log4j.dtd\">");

    Map<String, String> expectedLogSearchLog4j = ImmutableMap.of(
        "content", "<!DOCTYPE log4j:configuration SYSTEM \"http://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/xml/doc-files/log4j.dtd\">");

    Config logSearchLog4jConf = easyMockSupport.createNiceMock(Config.class);
    expect(cluster.getDesiredConfigByType("logsearch-log4j")).andReturn(logSearchLog4jConf).atLeastOnce();
    expect(logSearchLog4jConf.getProperties()).andReturn(oldLogSearchLog4j).anyTimes();
    Capture<Map<String, String>> logSearchLog4jCapture = EasyMock.newCapture();
    expect(controller.createConfig(anyObject(Cluster.class), anyObject(StackId.class), anyString(), capture(logSearchLog4jCapture), anyString(),
        EasyMock.anyObject())).andReturn(config).once();

    Map<String, String> oldLogSearchServiceLogsConf = ImmutableMap.of(
        "content", "<before/><requestHandler name=\"/admin/\"   class=\"solr.admin.AdminHandlers\" /><after/>");

    Map<String, String> expectedLogSearchServiceLogsConf = ImmutableMap.of(
        "content", "<before/><after/>");

    Config logSearchServiceLogsConf = easyMockSupport.createNiceMock(Config.class);
    expect(cluster.getDesiredConfigByType("logsearch-service_logs-solrconfig")).andReturn(logSearchServiceLogsConf).atLeastOnce();
    expect(logSearchServiceLogsConf.getProperties()).andReturn(oldLogSearchServiceLogsConf).anyTimes();
    Capture<Map<String, String>> logSearchServiceLogsConfCapture = EasyMock.newCapture();
    expect(controller.createConfig(anyObject(Cluster.class), anyObject(StackId.class), anyString(), capture(logSearchServiceLogsConfCapture), anyString(),
        EasyMock.anyObject())).andReturn(config).once();

    Map<String, String> oldLogSearchAuditLogsConf = ImmutableMap.of(
        "content", "<before/><requestHandler name=\"/admin/\"   class=\"solr.admin.AdminHandlers\" /><after/>");

    Map<String, String> expectedLogSearchAuditLogsConf = ImmutableMap.of(
        "content", "<before/><after/>");

    Config logSearchAuditLogsConf = easyMockSupport.createNiceMock(Config.class);
    expect(cluster.getDesiredConfigByType("logsearch-audit_logs-solrconfig")).andReturn(logSearchAuditLogsConf).atLeastOnce();
    expect(logSearchAuditLogsConf.getProperties()).andReturn(oldLogSearchAuditLogsConf).anyTimes();
    Capture<Map<String, String>> logSearchAuditLogsConfCapture = EasyMock.newCapture();
    expect(controller.createConfig(anyObject(Cluster.class), anyObject(StackId.class), anyString(), capture(logSearchAuditLogsConfCapture), anyString(),
        EasyMock.anyObject())).andReturn(config).once();

    Map<String, String> oldLogFeederOutputConf = ImmutableMap.of(
        "content",
        "      \"zk_connect_string\":\"{{logsearch_solr_zk_quorum}}{{logsearch_solr_zk_znode}}\",\n" +
            "      \"collection\":\"{{logsearch_solr_collection_service_logs}}\",\n" +
            "      \"number_of_shards\": \"{{logsearch_collection_service_logs_numshards}}\",\n" +
            "      \"splits_interval_mins\": \"{{logsearch_service_logs_split_interval_mins}}\",\n" +
            "\n" +
            "      \"zk_connect_string\":\"{{logsearch_solr_zk_quorum}}{{logsearch_solr_zk_znode}}\",\n" +
            "      \"collection\":\"{{logsearch_solr_collection_audit_logs}}\",\n" +
            "      \"number_of_shards\": \"{{logsearch_collection_audit_logs_numshards}}\",\n" +
            "      \"splits_interval_mins\": \"{{logsearch_audit_logs_split_interval_mins}}\",\n"
    );

    Map<String, String> expectedLogFeederOutputConf = ImmutableMap.of(
        "content",
        "      \"zk_connect_string\":\"{{logsearch_solr_zk_quorum}}{{logsearch_solr_zk_znode}}\",\n" +
            "      \"type\": \"service\",\n" +
            "\n" +
            "      \"zk_connect_string\":\"{{logsearch_solr_zk_quorum}}{{logsearch_solr_zk_znode}}\",\n" +
            "      \"type\": \"audit\",\n"
    );

    Config logFeederOutputConf = easyMockSupport.createNiceMock(Config.class);
    expect(cluster.getDesiredConfigByType("logfeeder-output-config")).andReturn(logFeederOutputConf).atLeastOnce();
    expect(logFeederOutputConf.getProperties()).andReturn(oldLogFeederOutputConf).anyTimes();
    Capture<Map<String, String>> logFeederOutputConfCapture = EasyMock.newCapture();
    expect(controller.createConfig(anyObject(Cluster.class), anyObject(StackId.class), anyString(), capture(logFeederOutputConfCapture), anyString(),
        EasyMock.anyObject())).andReturn(config).once();

    replay(clusters, cluster);
    replay(controller, injector2);
    replay(confSomethingElse1, confSomethingElse2, confLogSearchConf1, confLogSearchConf2);
    replay(logSearchPropertiesConf, logFeederPropertiesConf);
    replay(logFeederLog4jConf, logSearchLog4jConf);
    replay(logSearchServiceLogsConf, logSearchAuditLogsConf);
    replay(logFeederOutputConf);
    new UpgradeCatalog270(injector2).updateLogSearchConfigs();
    easyMockSupport.verifyAll();

    Map<String, String> newLogFeederProperties = logFeederPropertiesCapture.getValue();
    assertTrue(Maps.difference(expectedLogFeederProperties, newLogFeederProperties).areEqual());

    Map<String, String> newLogSearchProperties = logSearchPropertiesCapture.getValue();
    assertTrue(Maps.difference(Collections.emptyMap(), newLogSearchProperties).areEqual());

    Map<String, String> updatedLogFeederLog4j = logFeederLog4jCapture.getValue();
    assertTrue(Maps.difference(expectedLogFeederLog4j, updatedLogFeederLog4j).areEqual());

    Map<String, String> updatedLogSearchLog4j = logSearchLog4jCapture.getValue();
    assertTrue(Maps.difference(expectedLogSearchLog4j, updatedLogSearchLog4j).areEqual());

    Map<String, String> updatedServiceLogsConf = logSearchServiceLogsConfCapture.getValue();
    assertTrue(Maps.difference(expectedLogSearchServiceLogsConf, updatedServiceLogsConf).areEqual());

    Map<String, String> updatedAuditLogsConf = logSearchAuditLogsConfCapture.getValue();
    assertTrue(Maps.difference(expectedLogSearchAuditLogsConf, updatedAuditLogsConf).areEqual());

    Map<String, String> updatedLogFeederOutputConf = logFeederOutputConfCapture.getValue();
    assertTrue(Maps.difference(expectedLogFeederOutputConf, updatedLogFeederOutputConf).areEqual());
  }

  @Test
  public void testUpdateKerberosConfigurations() throws AmbariException, NoSuchFieldException, IllegalAccessException {
    StackId stackId = new StackId("HDP", "2.6.0.0");

    Map<String, Cluster> clusterMap = new HashMap<>();

    Map<String, String> propertiesWithGroup = new HashMap<>();
    propertiesWithGroup.put("group", "ambari_managed_identities");
    propertiesWithGroup.put("kdc_host", "host1.example.com");
    propertiesWithGroup.put("realm", "example.com");

    Config newConfig = createMock(Config.class);
    expect(newConfig.getTag()).andReturn("version2").atLeastOnce();
    expect(newConfig.getType()).andReturn("kerberos-env").atLeastOnce();

    ServiceConfigVersionResponse response = createMock(ServiceConfigVersionResponse.class);

    Config configWithGroup = createMock(Config.class);
    expect(configWithGroup.getProperties()).andReturn(propertiesWithGroup).atLeastOnce();
    expect(configWithGroup.getPropertiesAttributes()).andReturn(Collections.emptyMap()).atLeastOnce();
    expect(configWithGroup.getTag()).andReturn("version1").atLeastOnce();

    Cluster cluster1 = createMock(Cluster.class);
    expect(cluster1.getDesiredConfigByType("kerberos-env")).andReturn(configWithGroup).atLeastOnce();
    expect(cluster1.getConfigsByType("kerberos-env")).andReturn(Collections.singletonMap("v1", configWithGroup)).atLeastOnce();
    expect(cluster1.getServiceByConfigType("kerberos-env")).andReturn("KERBEROS").atLeastOnce();
    expect(cluster1.getClusterName()).andReturn("c1").atLeastOnce();
    expect(cluster1.getClusterId()).andReturn(1L).atLeastOnce();
    expect(cluster1.getDesiredStackVersion()).andReturn(stackId).atLeastOnce();
    expect(cluster1.getConfig(eq("kerberos-env"), anyString())).andReturn(newConfig).atLeastOnce();
    expect(cluster1.addDesiredConfig("ambari-upgrade", Collections.singleton(newConfig), "Updated kerberos-env during Ambari Upgrade from 2.6.2 to 2.7.0.")).andReturn(response).once();

    Map<String, String> propertiesWithoutGroup = new HashMap<>();
    propertiesWithoutGroup.put("kdc_host", "host2.example.com");
    propertiesWithoutGroup.put("realm", "example.com");

    Config configWithoutGroup = createMock(Config.class);
    expect(configWithoutGroup.getProperties()).andReturn(propertiesWithoutGroup).atLeastOnce();

    Cluster cluster2 = createMock(Cluster.class);
    expect(cluster2.getDesiredConfigByType("kerberos-env")).andReturn(configWithoutGroup).atLeastOnce();

    Cluster cluster3 = createMock(Cluster.class);
    expect(cluster3.getDesiredConfigByType("kerberos-env")).andReturn(null).atLeastOnce();

    clusterMap.put("c1", cluster1);
    clusterMap.put("c2", cluster2);
    clusterMap.put("c3", cluster3);

    Clusters clusters = createMock(Clusters.class);
    expect(clusters.getClusters()).andReturn(clusterMap).anyTimes();

    Capture<Map<String, String>> capturedProperties = newCapture();

    AmbariManagementControllerImpl controller = createMockBuilder(AmbariManagementControllerImpl.class)
        .addMockedMethod("createConfiguration")
        .addMockedMethod("getClusters", new Class[]{})
        .addMockedMethod("createConfig")
        .addMockedMethod("getClusterMetadataOnConfigsUpdate", Cluster.class)
        .createMock();
    expect(controller.getClusters()).andReturn(clusters).anyTimes();
    expect(controller.createConfig(eq(cluster1), eq(stackId), eq("kerberos-env"), capture(capturedProperties), anyString(), anyObject(Map.class))).andReturn(newConfig).once();
    expect(controller.getClusterMetadataOnConfigsUpdate(eq(cluster1))).andReturn(createNiceMock(MetadataUpdateEvent.class)).once();


    Injector injector = createNiceMock(Injector.class);
    expect(injector.getInstance(AmbariManagementController.class)).andReturn(controller).anyTimes();
    expect(injector.getInstance(MetadataHolder.class)).andReturn(createNiceMock(MetadataHolder.class)).anyTimes();
    expect(injector.getInstance(AgentConfigsHolder.class)).andReturn(createNiceMock(AgentConfigsHolder.class)).anyTimes();
    expect(injector.getInstance(AmbariServer.class)).andReturn(createNiceMock(AmbariServer.class)).anyTimes();
    KerberosHelper kerberosHelperMock = createNiceMock(KerberosHelper.class);
    expect(kerberosHelperMock.createTemporaryDirectory()).andReturn(new File("/invalid/file/path")).times(2);
    expect(injector.getInstance(KerberosHelper.class)).andReturn(kerberosHelperMock).anyTimes();

    replay(controller, clusters, cluster1, cluster2, configWithGroup, configWithoutGroup, newConfig, response, injector, kerberosHelperMock);

    Field field = AbstractUpgradeCatalog.class.getDeclaredField("configuration");

    UpgradeCatalog270 upgradeCatalog270 = createMockBuilder(UpgradeCatalog270.class).addMockedMethod("getPrepareIdentityServerAction").addMockedMethod("executeInTransaction").createMock();
    PrepareKerberosIdentitiesServerAction mockAction = createNiceMock(PrepareKerberosIdentitiesServerAction.class);
    expect(upgradeCatalog270.getPrepareIdentityServerAction()).andReturn(mockAction).times(2);
    upgradeCatalog270.executeInTransaction(anyObject());
    expectLastCall().times(2);
    upgradeCatalog270.injector = injector;

    replay(upgradeCatalog270);

    field.set(upgradeCatalog270, createNiceMock(Configuration.class));
    upgradeCatalog270.updateKerberosConfigurations();

    verify(controller, clusters, cluster1, cluster2, configWithGroup, configWithoutGroup, newConfig, response, injector, upgradeCatalog270);


    Assert.assertEquals(1, capturedProperties.getValues().size());

    Map<String, String> properties = capturedProperties.getValue();
    Assert.assertEquals(3, properties.size());
    Assert.assertEquals("ambari_managed_identities", properties.get("ipa_user_group"));
    Assert.assertEquals("host1.example.com", properties.get("kdc_host"));
    Assert.assertEquals("example.com", properties.get("realm"));

    Assert.assertEquals(3, propertiesWithGroup.size());
    Assert.assertEquals("ambari_managed_identities", propertiesWithGroup.get("group"));
    Assert.assertEquals("host1.example.com", propertiesWithGroup.get("kdc_host"));
    Assert.assertEquals("example.com", propertiesWithGroup.get("realm"));
  }

  @Test
  public void shouldSaveLdapConfigurationIfPropertyIsSetInAmbariProperties() throws Exception {
    final Module module = getTestGuiceModule();

    expect(entityManager.find(anyObject(), anyObject())).andReturn(null).anyTimes();
    final Map<String, String> properties = new HashMap<>();
    properties.put(AmbariLdapConfigurationKeys.LDAP_ENABLED.key(), "true");
    expect(ambariConfigurationDao.reconcileCategory(AmbariServerConfigurationCategory.LDAP_CONFIGURATION.getCategoryName(), properties, false)).andReturn(true).once();
    replay(entityManager, ambariConfigurationDao);

    final Injector injector = Guice.createInjector(module);
    injector.getInstance(Configuration.class).setProperty("ambari.ldap.isConfigured", "true");
    final UpgradeCatalog270 upgradeCatalog270 = new UpgradeCatalog270(injector);
    upgradeCatalog270.upgradeLdapConfiguration();
    verify(entityManager, ambariConfigurationDao);
  }

  @Test
  public void shouldNotSaveLdapConfigurationIfPropertyIsNotSetInAmbariProperties() throws Exception {
    final Module module = getTestGuiceModule();
    expect(entityManager.find(anyObject(), anyObject())).andReturn(null).anyTimes();
    final Map<String, String> properties = new HashMap<>();
    properties.put(AmbariLdapConfigurationKeys.LDAP_ENABLED.key(), "true");
    expect(ambariConfigurationDao.reconcileCategory(AmbariServerConfigurationCategory.LDAP_CONFIGURATION.getCategoryName(), properties, false)).andReturn(true).once();
    replay(entityManager, ambariConfigurationDao);

    final Injector injector = Guice.createInjector(module);
    final UpgradeCatalog270 upgradeCatalog270 = new UpgradeCatalog270(injector);
    upgradeCatalog270.upgradeLdapConfiguration();

    expectedException.expect(AssertionError.class);
    expectedException.expectMessage("Expectation failure on verify");
    verify(entityManager, ambariConfigurationDao);
  }

  private static final String KERBEROS_DESCRIPTOR_JSON = "{\"identities\":[{\"keytab\":{\"configuration\":\"cluster-env/smokeuser_keytab\",\"file\":\"${keytab_dir}/smokeuser.headless.keytab\",\"group\":{\"access\":\"r\",\"name\":\"${cluster-env/user_group}\"},\"owner\":{\"access\":\"r\",\"name\":\"${cluster-env/smokeuser}\"}},\"name\":\"smokeuser\",\"principal\":{\"configuration\":\"cluster-env/smokeuser_principal_name\",\"local_username\":\"${cluster-env/smokeuser}\",\"type\":\"user\",\"value\":\"${cluster-env/smokeuser}${principal_suffix}@${realm}\"}},{\"keytab\":{\"file\":\"${keytab_dir}/spnego.service.keytab\",\"group\":{\"access\":\"r\",\"name\":\"${cluster-env/user_group}\"},\"owner\":{\"access\":\"r\",\"name\":\"root\"}},\"name\":\"spnego\",\"principal\":{\"type\":\"service\",\"value\":\"HTTP/_HOST@${realm}\"}}],\"properties\":{\"additional_realms\":\"\",\"keytab_dir\":\"/etc/security/keytabs\",\"principal_suffix\":\"-${cluster_name|toLower()}\",\"realm\":\"EXAMPLE.COM\"},\"services\":[{\"components\":[{\"name\":\"ACCUMULO_CLIENT\"},{\"name\":\"ACCUMULO_GC\"},{\"identities\":[{\"name\":\"accumulo_accumulo_master_hdfs\",\"reference\":\"/HDFS/NAMENODE/hdfs\"}],\"name\":\"ACCUMULO_MASTER\"},{\"name\":\"ACCUMULO_MONITOR\"},{\"name\":\"ACCUMULO_TRACER\"},{\"name\":\"ACCUMULO_TSERVER\"}],\"configurations\":[{\"accumulo-site\":{\"general.delegation.token.lifetime\":\"7d\",\"general.delegation.token.update.interval\":\"1d\",\"instance.rpc.sasl.enabled\":\"true\",\"instance.security.authenticator\":\"org.apache.accumulo.server.security.handler.KerberosAuthenticator\",\"instance.security.authorizor\":\"org.apache.accumulo.server.security.handler.KerberosAuthorizor\",\"instance.security.permissionHandler\":\"org.apache.accumulo.server.security.handler.KerberosPermissionHandler\",\"trace.token.type\":\"org.apache.accumulo.core.client.security.tokens.KerberosToken\"}},{\"client\":{\"kerberos.server.primary\":\"{{bare_accumulo_principal}}\"}}],\"identities\":[{\"keytab\":{\"configuration\":\"accumulo-env/accumulo_user_keytab\",\"file\":\"${keytab_dir}/accumulo.headless.keytab\",\"group\":{\"access\":\"r\",\"name\":\"${cluster-env/user_group}\"},\"owner\":{\"access\":\"r\",\"name\":\"${accumulo-env/accumulo_user}\"}},\"name\":\"accumulo\",\"principal\":{\"configuration\":\"accumulo-env/accumulo_principal_name\",\"local_username\":\"${accumulo-env/accumulo_user}\",\"type\":\"user\",\"value\":\"${accumulo-env/accumulo_user}${principal_suffix}@${realm}\"}},{\"keytab\":{\"configuration\":\"accumulo-site/general.kerberos.keytab\",\"file\":\"${keytab_dir}/accumulo.service.keytab\",\"group\":{\"access\":\"\",\"name\":\"${cluster-env/user_group}\"},\"owner\":{\"access\":\"r\",\"name\":\"${accumulo-env/accumulo_user}\"}},\"name\":\"accumulo_service\",\"principal\":{\"configuration\":\"accumulo-site/general.kerberos.principal\",\"local_username\":\"${accumulo-env/accumulo_user}\",\"type\":\"service\",\"value\":\"${accumulo-env/accumulo_user}/_HOST@${realm}\"}},{\"name\":\"accumulo_smokeuser\",\"reference\":\"/smokeuser\"},{\"keytab\":{\"configuration\":\"accumulo-site/trace.token.property.keytab\",\"file\":\"${keytab_dir}/accumulo-tracer.headless.keytab\",\"group\":{\"access\":\"\",\"name\":\"${cluster-env/user_group}\"},\"owner\":{\"access\":\"r\",\"name\":\"${accumulo-env/accumulo_user}\"}},\"name\":\"accumulo_tracer\",\"principal\":{\"configuration\":\"accumulo-site/trace.user\",\"local_username\":\"${accumulo-env/accumulo_user}\",\"type\":\"user\",\"value\":\"tracer${principal_suffix}@${realm}\"}}],\"name\":\"ACCUMULO\"},{\"components\":[{\"identities\":[{\"keytab\":{\"file\":\"${keytab_dir}/ambari.server.keytab\",\"group\":{},\"owner\":{\"access\":\"r\"}},\"name\":\"ambari-server\",\"principal\":{\"configuration\":\"cluster-env/ambari_principal_name\",\"type\":\"user\",\"value\":\"ambari-server${principal_suffix}@${realm}\"}},{\"name\":\"ambari-server_spnego\",\"reference\":\"/spnego\"}],\"name\":\"AMBARI_SERVER\"}],\"name\":\"AMBARI\"},{\"components\":[{\"identities\":[{\"keytab\":{\"configuration\":\"infra-solr-env/infra_solr_kerberos_keytab\",\"file\":\"${keytab_dir}/ambari-infra-solr.service.keytab\",\"group\":{\"access\":\"\",\"name\":\"${cluster-env/user_group}\"},\"owner\":{\"access\":\"r\",\"name\":\"${infra-solr-env/infra_solr_user}\"}},\"name\":\"infra-solr\",\"principal\":{\"configuration\":\"infra-solr-env/infra_solr_kerberos_principal\",\"type\":\"service\",\"value\":\"infra-solr/_HOST@${realm}\"}}],\"name\":\"INFRA_SOLR\"},{\"name\":\"INFRA_SOLR_CLIENT\"}],\"identities\":[{\"name\":\"ambari_infra_smokeuser\",\"reference\":\"/smokeuser\"},{\"keytab\":{\"configuration\":\"infra-solr-env/infra_solr_web_kerberos_keytab\"},\"name\":\"ambari_infra_spnego\",\"principal\":{\"configuration\":\"infra-solr-env/infra_solr_web_kerberos_principal\"},\"reference\":\"/spnego\"}],\"name\":\"AMBARI_INFRA\"},{\"components\":[{\"configurations\":[{\"ams-hbase-security-site\":{\"hadoop.security.authentication\":\"kerberos\",\"hbase.coprocessor.master.classes\":\"org.apache.hadoop.hbase.security.access.AccessController\",\"hbase.coprocessor.region.classes\":\"org.apache.hadoop.hbase.security.token.TokenProvider,org.apache.hadoop.hbase.security.access.AccessController\",\"hbase.security.authentication\":\"kerberos\",\"hbase.security.authorization\":\"true\",\"hbase.zookeeper.property.authProvider.1\":\"org.apache.zookeeper.server.auth.SASLAuthenticationProvider\",\"hbase.zookeeper.property.jaasLoginRenew\":\"3600000\",\"hbase.zookeeper.property.kerberos.removeHostFromPrincipal\":\"true\",\"hbase.zookeeper.property.kerberos.removeRealmFromPrincipal\":\"true\"}},{\"ams-hbase-site\":{\"zookeeper.znode.parent\":\"/ams-hbase-secure\"}}],\"identities\":[{\"keytab\":{\"configuration\":\"ams-hbase-security-site/hbase.myclient.keytab\",\"file\":\"${keytab_dir}/ams.collector.keytab\",\"group\":{\"access\":\"\",\"name\":\"${cluster-env/user_group}\"},\"owner\":{\"access\":\"r\",\"name\":\"${ams-env/ambari_metrics_user}\"}},\"name\":\"ams_collector\",\"principal\":{\"configuration\":\"ams-hbase-security-site/hbase.myclient.principal\",\"local_username\":\"${ams-env/ambari_metrics_user}\",\"type\":\"service\",\"value\":\"amshbase/_HOST@${realm}\"}},{\"keytab\":{\"configuration\":\"ams-hbase-security-site/hbase.master.keytab.file\",\"file\":\"${keytab_dir}/ams-hbase.master.keytab\",\"group\":{\"access\":\"\",\"name\":\"${cluster-env/user_group}\"},\"owner\":{\"access\":\"r\",\"name\":\"${ams-env/ambari_metrics_user}\"}},\"name\":\"ams_hbase_master_hbase\",\"principal\":{\"configuration\":\"ams-hbase-security-site/hbase.master.kerberos.principal\",\"local_username\":\"${ams-env/ambari_metrics_user}\",\"type\":\"service\",\"value\":\"amshbase/_HOST@${realm}\"}},{\"keytab\":{\"configuration\":\"ams-hbase-security-site/hbase.regionserver.keytab.file\",\"file\":\"${keytab_dir}/ams-hbase.regionserver.keytab\",\"group\":{\"access\":\"\",\"name\":\"${cluster-env/user_group}\"},\"owner\":{\"access\":\"r\",\"name\":\"${ams-env/ambari_metrics_user}\"}},\"name\":\"ams_hbase_regionserver_hbase\",\"principal\":{\"configuration\":\"ams-hbase-security-site/hbase.regionserver.kerberos.principal\",\"local_username\":\"${ams-env/ambari_metrics_user}\",\"type\":\"service\",\"value\":\"amshbase/_HOST@${realm}\"}},{\"keytab\":{\"configuration\":\"ams-hbase-security-site/ams.zookeeper.keytab\",\"file\":\"${keytab_dir}/ams-zk.service.keytab\",\"group\":{\"access\":\"\",\"name\":\"${cluster-env/user_group}\"},\"owner\":{\"access\":\"r\",\"name\":\"${ams-env/ambari_metrics_user}\"}},\"name\":\"ams_zookeeper\",\"principal\":{\"configuration\":\"ams-hbase-security-site/ams.zookeeper.principal\",\"local_username\":\"${ams-env/ambari_metrics_user}\",\"type\":\"service\",\"value\":\"amszk/_HOST@${realm}\"}}],\"name\":\"METRICS_COLLECTOR\"}],\"identities\":[{\"name\":\"ambari_metrics_spnego\",\"reference\":\"/spnego\"}],\"name\":\"AMBARI_METRICS\"},{\"auth_to_local_properties\":[\"application-properties/atlas.authentication.method.kerberos.name.rules|new_lines_escaped\"],\"components\":[{\"identities\":[{\"keytab\":{\"configuration\":\"application-properties/atlas.jaas.KafkaClient.option.keyTab\",\"file\":\"${keytab_dir}/atlas.service.keytab\",\"group\":{\"access\":\"\",\"name\":\"${cluster-env/user_group}\"},\"owner\":{\"access\":\"r\",\"name\":\"${atlas-env/metadata_user}\"}},\"name\":\"atlas\",\"principal\":{\"configuration\":\"application-properties/atlas.jaas.KafkaClient.option.principal\",\"local_username\":\"${atlas-env/metadata_user}\",\"type\":\"service\",\"value\":\"atlas/_HOST@${realm}\"}},{\"name\":\"atlas_atlas_server_infra-solr\",\"reference\":\"/AMBARI_INFRA/INFRA_SOLR/infra-solr\"},{\"name\":\"atlas_atlas_server_kafka_broker\",\"reference\":\"/KAFKA/KAFKA_BROKER/kafka_broker\"},{\"keytab\":{\"configuration\":\"application-properties/atlas.authentication.method.kerberos.keytab\"},\"name\":\"atlas_atlas_server_spnego\",\"principal\":{\"configuration\":\"application-properties/atlas.authentication.method.kerberos.principal\",\"value\":\"HTTP/_HOST@${realm}\"},\"reference\":\"/spnego\"},{\"keytab\":{\"configuration\":\"application-properties/atlas.authentication.keytab\"},\"name\":\"atlas_auth\",\"principal\":{\"configuration\":\"application-properties/atlas.authentication.principal\"},\"reference\":\"/ATLAS/ATLAS_SERVER/atlas\"},{\"keytab\":{\"configuration\":\"ranger-atlas-audit/xasecure.audit.jaas.Client.option.keyTab\"},\"name\":\"ranger_atlas_audit\",\"principal\":{\"configuration\":\"ranger-atlas-audit/xasecure.audit.jaas.Client.option.principal\"},\"reference\":\"/ATLAS/ATLAS_SERVER/atlas\"}],\"name\":\"ATLAS_SERVER\"}],\"configurations\":[{\"application-properties\":{\"atlas.authentication.method.kerberos\":\"true\",\"atlas.jaas.KafkaClient.loginModuleControlFlag\":\"required\",\"atlas.jaas.KafkaClient.loginModuleName\":\"com.sun.security.auth.module.Krb5LoginModule\",\"atlas.jaas.KafkaClient.option.serviceName\":\"${kafka-env/kafka_user}\",\"atlas.jaas.KafkaClient.option.storeKey\":\"true\",\"atlas.jaas.KafkaClient.option.useKeyTab\":\"true\",\"atlas.kafka.sasl.kerberos.service.name\":\"${kafka-env/kafka_user}\",\"atlas.kafka.security.protocol\":\"PLAINTEXTSASL\",\"atlas.server.ha.zookeeper.acl\":\"auth:\",\"atlas.solr.kerberos.enable\":\"true\"}},{\"ranger-atlas-audit\":{\"xasecure.audit.destination.solr.force.use.inmemory.jaas.config\":\"true\",\"xasecure.audit.jaas.Client.loginModuleControlFlag\":\"required\",\"xasecure.audit.jaas.Client.loginModuleName\":\"com.sun.security.auth.module.Krb5LoginModule\",\"xasecure.audit.jaas.Client.option.serviceName\":\"solr\",\"xasecure.audit.jaas.Client.option.storeKey\":\"false\",\"xasecure.audit.jaas.Client.option.useKeyTab\":\"true\"}}],\"name\":\"ATLAS\"},{\"auth_to_local_properties\":[\"druid-common/druid.hadoop.security.spnego.authToLocal|new_lines_escaped\"],\"components\":[{\"name\":\"DRUID_BROKER\"},{\"name\":\"DRUID_COORDINATOR\"},{\"name\":\"DRUID_HISTORICAL\"},{\"name\":\"DRUID_MIDDLEMANAGER\"},{\"name\":\"DRUID_OVERLORD\"},{\"name\":\"DRUID_ROUTER\"}],\"configurations\":[{\"druid-common\":{\"druid.hadoop.security.spnego.excludedPaths\":\"[\\\"/status\\\"]\",\"druid.security.extensions.loadList\":\"[\\\"druid-kerberos\\\"]\"}}],\"identities\":[{\"keytab\":{\"configuration\":\"druid-common/druid.hadoop.security.kerberos.keytab\",\"file\":\"${keytab_dir}/druid.headless.keytab\",\"group\":{\"access\":\"r\",\"name\":\"${cluster-env/user_group}\"},\"owner\":{\"access\":\"r\",\"name\":\"${druid-env/druid_user}\"}},\"name\":\"druid\",\"principal\":{\"configuration\":\"druid-common/druid.hadoop.security.kerberos.principal\",\"local_username\":\"${druid-env/druid_user}\",\"type\":\"user\",\"value\":\"${druid-env/druid_user}${principal_suffix}@${realm}\"}},{\"name\":\"druid_smokeuser\",\"reference\":\"/smokeuser\"},{\"keytab\":{\"configuration\":\"druid-common/druid.hadoop.security.spnego.keytab\"},\"name\":\"druid_spnego\",\"principal\":{\"configuration\":\"druid-common/druid.hadoop.security.spnego.principal\"},\"reference\":\"/spnego\"}],\"name\":\"DRUID\"},{\"auth_to_local_properties\":[\"falcon-startup.properties/*.falcon.http.authentication.kerberos.name.rules|new_lines_escaped\"],\"components\":[{\"identities\":[{\"keytab\":{\"configuration\":\"falcon-atlas-application.properties/atlas.jaas.KafkaClient.option.keyTab\"},\"name\":\"falcon_falcon_server_falcon_server\",\"principal\":{\"configuration\":\"falcon-atlas-application.properties/atlas.jaas.KafkaClient.option.principal\"},\"reference\":\"/FALCON/FALCON_SERVER/falcon_server\"},{\"keytab\":{\"configuration\":\"falcon-startup.properties/*.falcon.http.authentication.kerberos.keytab\"},\"name\":\"falcon_falcon_server_falcon_spnego\",\"principal\":{\"configuration\":\"falcon-startup.properties/*.falcon.http.authentication.kerberos.principal\",\"value\":\"HTTP/_HOST@${realm}\"},\"reference\":\"/spnego\"},{\"name\":\"falcon_falcon_server_hdfs\",\"reference\":\"/HDFS/NAMENODE/hdfs\"},{\"keytab\":{\"configuration\":\"falcon-startup.properties/*.falcon.service.authentication.kerberos.keytab\",\"file\":\"${keytab_dir}/falcon.service.keytab\",\"group\":{\"access\":\"\",\"name\":\"${cluster-env/user_group}\"},\"owner\":{\"access\":\"r\",\"name\":\"${falcon-env/falcon_user}\"}},\"name\":\"falcon_server\",\"principal\":{\"configuration\":\"falcon-startup.properties/*.falcon.service.authentication.kerberos.principal\",\"local_username\":\"${falcon-env/falcon_user}\",\"type\":\"service\",\"value\":\"falcon/_HOST@${realm}\"}}],\"name\":\"FALCON_SERVER\"}],\"configurations\":[{\"falcon-startup.properties\":{\"*.dfs.namenode.kerberos.principal\":\"nn/_HOST@${realm}\",\"*.falcon.authentication.type\":\"kerberos\",\"*.falcon.http.authentication.type\":\"kerberos\"}}],\"identities\":[{\"name\":\"falcon_smokeuser\",\"reference\":\"/smokeuser\"},{\"name\":\"falcon_spnego\",\"reference\":\"/spnego\"}],\"name\":\"FALCON\"},{\"components\":[{\"identities\":[{\"name\":\"hbase_hbase_master_hdfs\",\"reference\":\"/HDFS/NAMENODE/hdfs\"},{\"keytab\":{\"configuration\":\"hbase-site/hbase.security.authentication.spnego.kerberos.keytab\"},\"name\":\"hbase_hbase_master_spnego\",\"principal\":{\"configuration\":\"hbase-site/hbase.security.authentication.spnego.kerberos.principal\"},\"reference\":\"/spnego\"},{\"keytab\":{\"configuration\":\"hbase-site/hbase.master.keytab.file\",\"file\":\"${keytab_dir}/hbase.service.keytab\",\"group\":{\"access\":\"\",\"name\":\"${cluster-env/user_group}\"},\"owner\":{\"access\":\"r\",\"name\":\"${hbase-env/hbase_user}\"}},\"name\":\"hbase_master_hbase\",\"principal\":{\"configuration\":\"hbase-site/hbase.master.kerberos.principal\",\"local_username\":\"${hbase-env/hbase_user}\",\"type\":\"service\",\"value\":\"hbase/_HOST@${realm}\"}},{\"keytab\":{\"configuration\":\"ranger-hbase-audit/xasecure.audit.jaas.Client.option.keyTab\"},\"name\":\"ranger_hbase_audit\",\"principal\":{\"configuration\":\"ranger-hbase-audit/xasecure.audit.jaas.Client.option.principal\"},\"reference\":\"/HBASE/HBASE_MASTER/hbase_master_hbase\"}],\"name\":\"HBASE_MASTER\"},{\"identities\":[{\"keytab\":{\"configuration\":\"hbase-site/hbase.security.authentication.spnego.kerberos.keytab\"},\"name\":\"hbase_hbase_regionserver_spnego\",\"principal\":{\"configuration\":\"hbase-site/hbase.security.authentication.spnego.kerberos.principal\"},\"reference\":\"/spnego\"},{\"keytab\":{\"configuration\":\"hbase-site/hbase.regionserver.keytab.file\",\"file\":\"${keytab_dir}/hbase.service.keytab\",\"group\":{\"access\":\"\",\"name\":\"${cluster-env/user_group}\"},\"owner\":{\"access\":\"r\",\"name\":\"${hbase-env/hbase_user}\"}},\"name\":\"hbase_regionserver_hbase\",\"principal\":{\"configuration\":\"hbase-site/hbase.regionserver.kerberos.principal\",\"local_username\":\"${hbase-env/hbase_user}\",\"type\":\"service\",\"value\":\"hbase/_HOST@${realm}\"}}],\"name\":\"HBASE_REGIONSERVER\"},{\"identities\":[{\"keytab\":{\"configuration\":\"hbase-site/phoenix.queryserver.keytab.file\"},\"name\":\"phoenix_spnego\",\"principal\":{\"configuration\":\"hbase-site/phoenix.queryserver.kerberos.principal\"},\"reference\":\"/spnego\"}],\"name\":\"PHOENIX_QUERY_SERVER\"}],\"configurations\":[{\"hbase-site\":{\"hbase.bulkload.staging.dir\":\"/apps/hbase/staging\",\"hbase.coprocessor.master.classes\":\"{{hbase_coprocessor_master_classes}}\",\"hbase.coprocessor.region.classes\":\"{{hbase_coprocessor_region_classes}}\",\"hbase.coprocessor.regionserver.classes\":\"{{hbase_coprocessor_regionserver_classes}}\",\"hbase.master.ui.readonly\":\"true\",\"hbase.security.authentication\":\"kerberos\",\"hbase.security.authorization\":\"true\",\"zookeeper.znode.parent\":\"/hbase-secure\"}},{\"ranger-hbase-audit\":{\"xasecure.audit.destination.solr.force.use.inmemory.jaas.config\":\"true\",\"xasecure.audit.jaas.Client.loginModuleControlFlag\":\"required\",\"xasecure.audit.jaas.Client.loginModuleName\":\"com.sun.security.auth.module.Krb5LoginModule\",\"xasecure.audit.jaas.Client.option.serviceName\":\"solr\",\"xasecure.audit.jaas.Client.option.storeKey\":\"false\",\"xasecure.audit.jaas.Client.option.useKeyTab\":\"true\"}}],\"identities\":[{\"keytab\":{\"configuration\":\"hbase-env/hbase_user_keytab\",\"file\":\"${keytab_dir}/hbase.headless.keytab\",\"group\":{\"access\":\"r\",\"name\":\"${cluster-env/user_group}\"},\"owner\":{\"access\":\"r\",\"name\":\"${hbase-env/hbase_user}\"}},\"name\":\"hbase\",\"principal\":{\"configuration\":\"hbase-env/hbase_principal_name\",\"local_username\":\"${hbase-env/hbase_user}\",\"type\":\"user\",\"value\":\"${hbase-env/hbase_user}${principal_suffix}@${realm}\"}},{\"name\":\"hbase_smokeuser\",\"reference\":\"/smokeuser\"},{\"name\":\"hbase_spnego\",\"reference\":\"/spnego\"}],\"name\":\"HBASE\"},{\"auth_to_local_properties\":[\"core-site/hadoop.security.auth_to_local\"],\"components\":[{\"configurations\":[{\"hdfs-site\":{\"dfs.datanode.address\":\"0.0.0.0:1019\",\"dfs.datanode.http.address\":\"0.0.0.0:1022\"}}],\"identities\":[{\"keytab\":{\"configuration\":\"hdfs-site/dfs.datanode.keytab.file\",\"file\":\"${keytab_dir}/dn.service.keytab\",\"group\":{\"access\":\"\",\"name\":\"${cluster-env/user_group}\"},\"owner\":{\"access\":\"r\",\"name\":\"${hadoop-env/hdfs_user}\"}},\"name\":\"datanode_dn\",\"principal\":{\"configuration\":\"hdfs-site/dfs.datanode.kerberos.principal\",\"local_username\":\"${hadoop-env/hdfs_user}\",\"type\":\"service\",\"value\":\"dn/_HOST@${realm}\"}}],\"name\":\"DATANODE\"},{\"identities\":[{\"name\":\"hdfs_hdfs_client_hdfs\",\"reference\":\"/HDFS/NAMENODE/hdfs\"}],\"name\":\"HDFS_CLIENT\"},{\"identities\":[{\"name\":\"hdfs_journalnode_spnego\",\"principal\":{\"configuration\":\"hdfs-site/dfs.journalnode.kerberos.internal.spnego.principal\"},\"reference\":\"/spnego\"},{\"keytab\":{\"configuration\":\"hdfs-site/dfs.journalnode.keytab.file\",\"file\":\"${keytab_dir}/jn.service.keytab\",\"group\":{\"access\":\"\",\"name\":\"${cluster-env/user_group}\"},\"owner\":{\"access\":\"r\",\"name\":\"${hadoop-env/hdfs_user}\"}},\"name\":\"journalnode_jn\",\"principal\":{\"configuration\":\"hdfs-site/dfs.journalnode.kerberos.principal\",\"local_username\":\"${hadoop-env/hdfs_user}\",\"type\":\"service\",\"value\":\"jn/_HOST@${realm}\"}}],\"name\":\"JOURNALNODE\"},{\"configurations\":[{\"hdfs-site\":{\"dfs.block.access.token.enable\":\"true\"}}],\"identities\":[{\"keytab\":{\"configuration\":\"hadoop-env/hdfs_user_keytab\",\"file\":\"${keytab_dir}/hdfs.headless.keytab\",\"group\":{\"access\":\"\",\"name\":\"${cluster-env/user_group}\"},\"owner\":{\"access\":\"r\",\"name\":\"${hadoop-env/hdfs_user}\"}},\"name\":\"hdfs\",\"principal\":{\"configuration\":\"hadoop-env/hdfs_principal_name\",\"local_username\":\"${hadoop-env/hdfs_user}\",\"type\":\"user\",\"value\":\"${hadoop-env/hdfs_user}${principal_suffix}@${realm}\"}},{\"keytab\":{\"configuration\":\"ranger-hdfs-audit/xasecure.audit.jaas.Client.option.keyTab\"},\"name\":\"hdfs_namenode_namenode_nn\",\"principal\":{\"configuration\":\"ranger-hdfs-audit/xasecure.audit.jaas.Client.option.principal\"},\"reference\":\"/HDFS/NAMENODE/namenode_nn\"},{\"name\":\"hdfs_namenode_spnego\",\"principal\":{\"configuration\":\"hdfs-site/dfs.namenode.kerberos.internal.spnego.principal\"},\"reference\":\"/spnego\"},{\"keytab\":{\"configuration\":\"hdfs-site/dfs.namenode.keytab.file\",\"file\":\"${keytab_dir}/nn.service.keytab\",\"group\":{\"access\":\"\",\"name\":\"${cluster-env/user_group}\"},\"owner\":{\"access\":\"r\",\"name\":\"${hadoop-env/hdfs_user}\"}},\"name\":\"namenode_nn\",\"principal\":{\"configuration\":\"hdfs-site/dfs.namenode.kerberos.principal\",\"local_username\":\"${hadoop-env/hdfs_user}\",\"type\":\"service\",\"value\":\"nn/_HOST@${realm}\"}}],\"name\":\"NAMENODE\"},{\"identities\":[{\"keytab\":{\"configuration\":\"hdfs-site/nfs.keytab.file\",\"file\":\"${keytab_dir}/nfs.service.keytab\",\"group\":{\"access\":\"\",\"name\":\"${cluster-env/user_group}\"},\"owner\":{\"access\":\"r\",\"name\":\"${hadoop-env/hdfs_user}\"}},\"name\":\"nfsgateway\",\"principal\":{\"configuration\":\"hdfs-site/nfs.kerberos.principal\",\"local_username\":\"${hadoop-env/hdfs_user}\",\"type\":\"service\",\"value\":\"nfs/_HOST@${realm}\"}}],\"name\":\"NFS_GATEWAY\"},{\"identities\":[{\"name\":\"hdfs_secondary_namenode_spnego\",\"principal\":{\"configuration\":\"hdfs-site/dfs.secondary.namenode.kerberos.internal.spnego.principal\"},\"reference\":\"/spnego\"},{\"keytab\":{\"configuration\":\"hdfs-site/dfs.secondary.namenode.keytab.file\",\"file\":\"${keytab_dir}/nn.service.keytab\",\"group\":{\"access\":\"\",\"name\":\"${cluster-env/user_group}\"},\"owner\":{\"access\":\"r\",\"name\":\"${hadoop-env/hdfs_user}\"}},\"name\":\"secondary_namenode_nn\",\"principal\":{\"configuration\":\"hdfs-site/dfs.secondary.namenode.kerberos.principal\",\"local_username\":\"${hadoop-env/hdfs_user}\",\"type\":\"service\",\"value\":\"nn/_HOST@${realm}\"}}],\"name\":\"SECONDARY_NAMENODE\"}],\"configurations\":[{\"core-site\":{\"ha.zookeeper.acl\":\"sasl:nn:rwcda\",\"hadoop.proxyuser.HTTP.groups\":\"${hadoop-env/proxyuser_group}\",\"hadoop.security.authentication\":\"kerberos\",\"hadoop.security.authorization\":\"true\"}},{\"ranger-hdfs-audit\":{\"xasecure.audit.destination.solr.force.use.inmemory.jaas.config\":\"true\",\"xasecure.audit.jaas.Client.loginModuleControlFlag\":\"required\",\"xasecure.audit.jaas.Client.loginModuleName\":\"com.sun.security.auth.module.Krb5LoginModule\",\"xasecure.audit.jaas.Client.option.serviceName\":\"solr\",\"xasecure.audit.jaas.Client.option.storeKey\":\"false\",\"xasecure.audit.jaas.Client.option.useKeyTab\":\"true\"}}],\"identities\":[{\"name\":\"hdfs_smokeuser\",\"reference\":\"/smokeuser\"},{\"keytab\":{\"configuration\":\"hdfs-site/dfs.web.authentication.kerberos.keytab\"},\"name\":\"hdfs_spnego\",\"principal\":{\"configuration\":\"hdfs-site/dfs.web.authentication.kerberos.principal\"},\"reference\":\"/spnego\"}],\"name\":\"HDFS\"},{\"components\":[{\"identities\":[{\"keytab\":{\"configuration\":\"hive-site/hive.metastore.kerberos.keytab.file\"},\"name\":\"hive_hive_metastore_hive_server_hive\",\"principal\":{\"configuration\":\"hive-site/hive.metastore.kerberos.principal\"},\"reference\":\"/HIVE/HIVE_SERVER/hive_server_hive\"}],\"name\":\"HIVE_METASTORE\"},{\"identities\":[{\"keytab\":{\"configuration\":\"hive-atlas-application.properties/atlas.jaas.KafkaClient.option.keyTab\"},\"name\":\"atlas_kafka\",\"principal\":{\"configuration\":\"hive-atlas-application.properties/atlas.jaas.KafkaClient.option.principal\"},\"reference\":\"/HIVE/HIVE_SERVER/hive_server_hive\"},{\"name\":\"hive_hive_server_hdfs\",\"reference\":\"/HDFS/NAMENODE/hdfs\"},{\"keytab\":{\"configuration\":\"hive-site/hive.server2.authentication.spnego.keytab\"},\"name\":\"hive_hive_server_spnego\",\"principal\":{\"configuration\":\"hive-site/hive.server2.authentication.spnego.principal\"},\"reference\":\"/spnego\"},{\"keytab\":{\"configuration\":\"hive-site/hive.server2.authentication.kerberos.keytab\",\"file\":\"${keytab_dir}/hive.service.keytab\",\"group\":{\"access\":\"r\",\"name\":\"${cluster-env/user_group}\"},\"owner\":{\"access\":\"r\",\"name\":\"${hive-env/hive_user}\"}},\"name\":\"hive_server_hive\",\"principal\":{\"configuration\":\"hive-site/hive.server2.authentication.kerberos.principal\",\"local_username\":\"${hive-env/hive_user}\",\"type\":\"service\",\"value\":\"hive/_HOST@${realm}\"}},{\"keytab\":{\"configuration\":\"ranger-hive-audit/xasecure.audit.jaas.Client.option.keyTab\"},\"name\":\"ranger_audit\",\"principal\":{\"configuration\":\"ranger-hive-audit/xasecure.audit.jaas.Client.option.principal\"},\"reference\":\"/HIVE/HIVE_SERVER/hive_server_hive\"}],\"name\":\"HIVE_SERVER\"},{\"identities\":[{\"name\":\"hive_hive_server_interactive_hdfs\",\"reference\":\"/HDFS/NAMENODE/hdfs\"},{\"name\":\"hive_hive_server_interactive_hive_server_hive\",\"reference\":\"/HIVE/HIVE_SERVER/hive_server_hive\"},{\"name\":\"hive_hive_server_interactive_spnego\",\"reference\":\"/HIVE/HIVE_SERVER/spnego\"}],\"name\":\"HIVE_SERVER_INTERACTIVE\"},{\"configurations\":[{\"core-site\":{\"hadoop.proxyuser.HTTP.hosts\":\"${clusterHostInfo/webhcat_server_host|append(core-site/hadoop.proxyuser.HTTP.hosts, \\\\\\\\,, true)}\"}},{\"webhcat-site\":{\"templeton.hive.properties\":\"hive.metastore.local\\u003dfalse,hive.metastore.uris\\u003d${clusterHostInfo/hive_metastore_host|each(thrift://%s:9083, \\\\\\\\,, \\\\s*\\\\,\\\\s*)},hive.metastore.sasl.enabled\\u003dtrue,hive.metastore.execute.setugi\\u003dtrue,hive.metastore.warehouse.dir\\u003d/apps/hive/warehouse,hive.exec.mode.local.auto\\u003dfalse,hive.metastore.kerberos.principal\\u003dhive/_HOST@${realm}\",\"templeton.kerberos.secret\":\"secret\"}}],\"identities\":[{\"keytab\":{\"configuration\":\"webhcat-site/templeton.kerberos.keytab\"},\"name\":\"hive_webhcat_server_spnego\",\"principal\":{\"configuration\":\"webhcat-site/templeton.kerberos.principal\"},\"reference\":\"/spnego\"}],\"name\":\"WEBHCAT_SERVER\"}],\"configurations\":[{\"hive-site\":{\"hive.metastore.sasl.enabled\":\"true\",\"hive.server2.authentication\":\"KERBEROS\"}},{\"ranger-hive-audit\":{\"xasecure.audit.destination.solr.force.use.inmemory.jaas.config\":\"true\",\"xasecure.audit.jaas.Client.loginModuleControlFlag\":\"required\",\"xasecure.audit.jaas.Client.loginModuleName\":\"com.sun.security.auth.module.Krb5LoginModule\",\"xasecure.audit.jaas.Client.option.serviceName\":\"solr\",\"xasecure.audit.jaas.Client.option.storeKey\":\"false\",\"xasecure.audit.jaas.Client.option.useKeyTab\":\"true\"}}],\"identities\":[{\"name\":\"hive_smokeuser\",\"reference\":\"/smokeuser\"},{\"name\":\"hive_spnego\",\"reference\":\"/spnego\"}],\"name\":\"HIVE\"},{\"auth_to_local_properties\":[\"kafka-broker/sasl.kerberos.principal.to.local.rules|comma\"],\"components\":[{\"identities\":[{\"keytab\":{\"configuration\":\"kafka-env/kafka_keytab\",\"file\":\"${keytab_dir}/kafka.service.keytab\",\"group\":{\"access\":\"\",\"name\":\"${cluster-env/user_group}\"},\"owner\":{\"access\":\"r\",\"name\":\"${kafka-env/kafka_user}\"}},\"name\":\"kafka_broker\",\"principal\":{\"configuration\":\"kafka-env/kafka_principal_name\",\"type\":\"service\",\"value\":\"${kafka-env/kafka_user}/_HOST@${realm}\"}},{\"keytab\":{\"configuration\":\"ranger-kafka-audit/xasecure.audit.jaas.Client.option.keyTab\"},\"name\":\"kafka_kafka_broker_kafka_broker\",\"principal\":{\"configuration\":\"ranger-kafka-audit/xasecure.audit.jaas.Client.option.principal\"},\"reference\":\"/KAFKA/KAFKA_BROKER/kafka_broker\"}],\"name\":\"KAFKA_BROKER\"}],\"configurations\":[{\"kafka-broker\":{\"authorizer.class.name\":\"kafka.security.auth.SimpleAclAuthorizer\",\"principal.to.local.class\":\"kafka.security.auth.KerberosPrincipalToLocal\",\"security.inter.broker.protocol\":\"PLAINTEXTSASL\",\"super.users\":\"user:${kafka-env/kafka_user}\",\"zookeeper.set.acl\":\"true\"}},{\"ranger-kafka-audit\":{\"xasecure.audit.destination.solr.force.use.inmemory.jaas.config\":\"true\",\"xasecure.audit.jaas.Client.loginModuleControlFlag\":\"required\",\"xasecure.audit.jaas.Client.loginModuleName\":\"com.sun.security.auth.module.Krb5LoginModule\",\"xasecure.audit.jaas.Client.option.serviceName\":\"solr\",\"xasecure.audit.jaas.Client.option.storeKey\":\"false\",\"xasecure.audit.jaas.Client.option.useKeyTab\":\"true\"}}],\"identities\":[{\"name\":\"kafka_smokeuser\",\"reference\":\"/smokeuser\"}],\"name\":\"KAFKA\"},{\"components\":[{\"name\":\"KERBEROS_CLIENT\"}],\"identities\":[{\"name\":\"kerberos_smokeuser\",\"reference\":\"/smokeuser\"}],\"name\":\"KERBEROS\"},{\"components\":[{\"configurations\":[{\"core-site\":{\"hadoop.proxyuser.${knox-env/knox_user}.groups\":\"${hadoop-env/proxyuser_group}\",\"hadoop.proxyuser.${knox-env/knox_user}.hosts\":\"${clusterHostInfo/knox_gateway_hosts}\"}},{\"gateway-site\":{\"gateway.hadoop.kerberos.secured\":\"true\",\"java.security.krb5.conf\":\"/etc/krb5.conf\"}},{\"oozie-site\":{\"oozie.service.ProxyUserService.proxyuser.${knox-env/knox_user}.groups\":\"${hadoop-env/proxyuser_group}\",\"oozie.service.ProxyUserService.proxyuser.${knox-env/knox_user}.hosts\":\"${clusterHostInfo/knox_gateway_hosts}\"}},{\"ranger-knox-audit\":{\"xasecure.audit.destination.solr.force.use.inmemory.jaas.config\":\"true\",\"xasecure.audit.jaas.Client.loginModuleControlFlag\":\"required\",\"xasecure.audit.jaas.Client.loginModuleName\":\"com.sun.security.auth.module.Krb5LoginModule\",\"xasecure.audit.jaas.Client.option.serviceName\":\"solr\",\"xasecure.audit.jaas.Client.option.storeKey\":\"false\",\"xasecure.audit.jaas.Client.option.useKeyTab\":\"true\"}},{\"webhcat-site\":{\"webhcat.proxyuser.${knox-env/knox_user}.groups\":\"${hadoop-env/proxyuser_group}\",\"webhcat.proxyuser.${knox-env/knox_user}.hosts\":\"${clusterHostInfo/knox_gateway_hosts}\"}}],\"identities\":[{\"keytab\":{\"configuration\":\"ranger-knox-audit/xasecure.audit.jaas.Client.option.keyTab\"},\"name\":\"knox_knox_gateway_knox_principal\",\"principal\":{\"configuration\":\"ranger-knox-audit/xasecure.audit.jaas.Client.option.principal\"},\"reference\":\"/KNOX/KNOX_GATEWAY/knox_principal\"},{\"keytab\":{\"configuration\":\"knox-env/knox_keytab_path\",\"file\":\"${keytab_dir}/knox.service.keytab\",\"group\":{\"access\":\"\",\"name\":\"${cluster-env/user_group}\"},\"owner\":{\"access\":\"r\",\"name\":\"${knox-env/knox_user}\"}},\"name\":\"knox_principal\",\"principal\":{\"configuration\":\"knox-env/knox_principal_name\",\"local_username\":\"${knox-env/knox_user}\",\"type\":\"service\",\"value\":\"${knox-env/knox_user}/_HOST@${realm}\"}}],\"name\":\"KNOX_GATEWAY\"}],\"name\":\"KNOX\"},{\"components\":[{\"identities\":[{\"keytab\":{\"configuration\":\"logfeeder-env/logfeeder_kerberos_keytab\",\"file\":\"${keytab_dir}/logfeeder.service.keytab\",\"group\":{\"access\":\"\",\"name\":\"${cluster-env/user_group}\"},\"owner\":{\"access\":\"r\",\"name\":\"root\"}},\"name\":\"logfeeder\",\"principal\":{\"configuration\":\"logfeeder-env/logfeeder_kerberos_principal\",\"type\":\"service\",\"value\":\"logfeeder/_HOST@${realm}\"}}],\"name\":\"LOGSEARCH_LOGFEEDER\"},{\"identities\":[{\"keytab\":{\"configuration\":\"logsearch-env/logsearch_kerberos_keytab\",\"file\":\"${keytab_dir}/logsearch.service.keytab\",\"group\":{\"access\":\"\",\"name\":\"${cluster-env/user_group}\"},\"owner\":{\"access\":\"r\",\"name\":\"${logsearch-env/logsearch_user}\"}},\"name\":\"logsearch\",\"principal\":{\"configuration\":\"logsearch-env/logsearch_kerberos_principal\",\"type\":\"service\",\"value\":\"logsearch/_HOST@${realm}\"}},{\"name\":\"logsearch_logsearch_server_infra-solr\",\"reference\":\"/AMBARI_INFRA/INFRA_SOLR/infra-solr\",\"when\":{\"contains\":[\"services\",\"AMBARI_INFRA\"]}}],\"name\":\"LOGSEARCH_SERVER\"}],\"identities\":[{\"name\":\"logsearch_smokeuser\",\"reference\":\"/smokeuser\"}],\"name\":\"LOGSEARCH\"},{\"components\":[{\"identities\":[{\"name\":\"mahout_mahout_hdfs\",\"reference\":\"/HDFS/NAMENODE/hdfs\"}],\"name\":\"MAHOUT\"}],\"identities\":[{\"name\":\"mahout_smokeuser\",\"reference\":\"/smokeuser\"}],\"name\":\"MAHOUT\"},{\"components\":[{\"identities\":[{\"keytab\":{\"configuration\":\"mapred-site/mapreduce.jobhistory.keytab\",\"file\":\"${keytab_dir}/jhs.service.keytab\",\"group\":{\"access\":\"\",\"name\":\"${cluster-env/user_group}\"},\"owner\":{\"access\":\"r\",\"name\":\"${mapred-env/mapred_user}\"}},\"name\":\"history_server_jhs\",\"principal\":{\"configuration\":\"mapred-site/mapreduce.jobhistory.principal\",\"local_username\":\"${mapred-env/mapred_user}\",\"type\":\"service\",\"value\":\"jhs/_HOST@${realm}\"}},{\"name\":\"mapreduce2_historyserver_hdfs\",\"reference\":\"/HDFS/NAMENODE/hdfs\"},{\"keytab\":{\"configuration\":\"mapred-site/mapreduce.jobhistory.webapp.spnego-keytab-file\"},\"name\":\"mapreduce2_historyserver_spnego\",\"principal\":{\"configuration\":\"mapred-site/mapreduce.jobhistory.webapp.spnego-principal\"},\"reference\":\"/spnego\"}],\"name\":\"HISTORYSERVER\"}],\"identities\":[{\"name\":\"mapreduce2_smokeuser\",\"reference\":\"/smokeuser\"},{\"name\":\"mapreduce2_spnego\",\"reference\":\"/spnego\"}],\"name\":\"MAPREDUCE2\"},{\"auth_to_local_properties\":[\"oozie-site/oozie.authentication.kerberos.name.rules\"],\"components\":[{\"identities\":[{\"name\":\"oozie_oozie_server_hdfs\",\"reference\":\"/HDFS/NAMENODE/hdfs\"},{\"keytab\":{\"configuration\":\"oozie-site/oozie.authentication.kerberos.keytab\"},\"name\":\"oozie_oozie_server_spnego\",\"principal\":{\"configuration\":\"oozie-site/oozie.authentication.kerberos.principal\"},\"reference\":\"/spnego\"},{\"keytab\":{\"configuration\":\"oozie-site/oozie.service.HadoopAccessorService.keytab.file\",\"file\":\"${keytab_dir}/oozie.service.keytab\",\"group\":{\"access\":\"\",\"name\":\"${cluster-env/user_group}\"},\"owner\":{\"access\":\"r\",\"name\":\"${oozie-env/oozie_user}\"}},\"name\":\"oozie_server\",\"principal\":{\"configuration\":\"oozie-site/oozie.service.HadoopAccessorService.kerberos.principal\",\"local_username\":\"${oozie-env/oozie_user}\",\"type\":\"service\",\"value\":\"oozie/_HOST@${realm}\"}}],\"name\":\"OOZIE_SERVER\"}],\"configurations\":[{\"oozie-site\":{\"local.realm\":\"${realm}\",\"oozie.authentication.type\":\"kerberos\",\"oozie.credentials.credentialclasses\":\"hcat\\u003dorg.apache.oozie.action.hadoop.HCatCredentials,hive2\\u003dorg.apache.oozie.action.hadoop.Hive2Credentials\",\"oozie.service.AuthorizationService.authorization.enabled\":\"true\",\"oozie.service.HadoopAccessorService.kerberos.enabled\":\"true\",\"oozie.zookeeper.secure\":\"true\"}}],\"identities\":[{\"name\":\"oozie_smokeuser\",\"reference\":\"/smokeuser\"},{\"name\":\"oozie_spnego\",\"reference\":\"/spnego\"}],\"name\":\"OOZIE\"},{\"components\":[{\"identities\":[{\"name\":\"pig_pig_hdfs\",\"reference\":\"/HDFS/NAMENODE/hdfs\"}],\"name\":\"PIG\"}],\"name\":\"PIG\"},{\"components\":[{\"identities\":[{\"name\":\"ranger_ranger_admin_infra-solr\",\"reference\":\"/AMBARI_INFRA/INFRA_SOLR/infra-solr\",\"when\":{\"contains\":[\"services\",\"AMBARI_INFRA\"]}},{\"keytab\":{\"configuration\":\"ranger-admin-site/xasecure.audit.jaas.Client.option.keyTab\"},\"name\":\"ranger_ranger_admin_rangeradmin\",\"principal\":{\"configuration\":\"ranger-admin-site/xasecure.audit.jaas.Client.option.principal\"},\"reference\":\"/RANGER/RANGER_ADMIN/rangeradmin\"},{\"keytab\":{\"configuration\":\"ranger-admin-site/ranger.spnego.kerberos.keytab\"},\"name\":\"ranger_ranger_admin_spnego\",\"reference\":\"/spnego\"},{\"keytab\":{\"configuration\":\"ranger-admin-site/ranger.admin.kerberos.keytab\",\"file\":\"${keytab_dir}/rangeradmin.service.keytab\",\"group\":{},\"owner\":{\"access\":\"r\",\"name\":\"${ranger-env/ranger_user}\"}},\"name\":\"rangeradmin\",\"principal\":{\"configuration\":\"ranger-admin-site/ranger.admin.kerberos.principal\",\"local_username\":\"${ranger-env/ranger_user}\",\"type\":\"service\",\"value\":\"rangeradmin/_HOST@${realm}\"}},{\"keytab\":{\"configuration\":\"ranger-admin-site/ranger.lookup.kerberos.keytab\",\"file\":\"${keytab_dir}/rangerlookup.service.keytab\",\"group\":{},\"owner\":{\"access\":\"r\",\"name\":\"${ranger-env/ranger_user}\"}},\"name\":\"rangerlookup\",\"principal\":{\"configuration\":\"ranger-admin-site/ranger.lookup.kerberos.principal\",\"type\":\"service\",\"value\":\"rangerlookup/_HOST@${realm}\"}}],\"name\":\"RANGER_ADMIN\"},{\"configurations\":[{\"tagsync-application-properties\":{\"atlas.jaas.KafkaClient.loginModuleControlFlag\":\"required\",\"atlas.jaas.KafkaClient.loginModuleName\":\"com.sun.security.auth.module.Krb5LoginModule\",\"atlas.jaas.KafkaClient.option.serviceName\":\"kafka\",\"atlas.jaas.KafkaClient.option.storeKey\":\"true\",\"atlas.jaas.KafkaClient.option.useKeyTab\":\"true\",\"atlas.kafka.sasl.kerberos.service.name\":\"kafka\",\"atlas.kafka.security.protocol\":\"PLAINTEXTSASL\"}}],\"identities\":[{\"keytab\":{\"configuration\":\"tagsync-application-properties/atlas.jaas.KafkaClient.option.keyTab\"},\"name\":\"ranger_ranger_tagsync_rangertagsync\",\"principal\":{\"configuration\":\"tagsync-application-properties/atlas.jaas.KafkaClient.option.principal\"},\"reference\":\"/RANGER/RANGER_TAGSYNC/rangertagsync\"},{\"keytab\":{\"configuration\":\"ranger-tagsync-site/ranger.tagsync.kerberos.keytab\",\"file\":\"${keytab_dir}/rangertagsync.service.keytab\",\"group\":{},\"owner\":{\"access\":\"r\",\"name\":\"${ranger-env/ranger_user}\"}},\"name\":\"rangertagsync\",\"principal\":{\"configuration\":\"ranger-tagsync-site/ranger.tagsync.kerberos.principal\",\"local_username\":\"rangertagsync\",\"type\":\"service\",\"value\":\"rangertagsync/_HOST@${realm}\"}}],\"name\":\"RANGER_TAGSYNC\"},{\"identities\":[{\"keytab\":{\"configuration\":\"ranger-ugsync-site/ranger.usersync.kerberos.keytab\",\"file\":\"${keytab_dir}/rangerusersync.service.keytab\",\"group\":{},\"owner\":{\"access\":\"r\",\"name\":\"${ranger-env/ranger_user}\"}},\"name\":\"rangerusersync\",\"principal\":{\"configuration\":\"ranger-ugsync-site/ranger.usersync.kerberos.principal\",\"local_username\":\"rangerusersync\",\"type\":\"service\",\"value\":\"rangerusersync/_HOST@${realm}\"}}],\"name\":\"RANGER_USERSYNC\"}],\"configurations\":[{\"ranger-admin-site\":{\"xasecure.audit.jaas.Client.loginModuleControlFlag\":\"required\",\"xasecure.audit.jaas.Client.loginModuleName\":\"com.sun.security.auth.module.Krb5LoginModule\",\"xasecure.audit.jaas.Client.option.serviceName\":\"solr\",\"xasecure.audit.jaas.Client.option.storeKey\":\"false\",\"xasecure.audit.jaas.Client.option.useKeyTab\":\"true\"}}],\"identities\":[{\"name\":\"ranger_smokeuser\",\"reference\":\"/smokeuser\"},{\"name\":\"ranger_spnego\",\"reference\":\"/spnego\"}],\"name\":\"RANGER\"},{\"auth_to_local_properties\":[\"kms-site/hadoop.kms.authentication.kerberos.name.rules\"],\"components\":[{\"identities\":[{\"keytab\":{\"configuration\":\"ranger-kms-audit/xasecure.audit.jaas.Client.option.keyTab\"},\"name\":\"ranger_kms_ranger_kms_server_rangerkms\",\"principal\":{\"configuration\":\"ranger-kms-audit/xasecure.audit.jaas.Client.option.principal\"},\"reference\":\"/RANGER_KMS/RANGER_KMS_SERVER/rangerkms\"},{\"keytab\":{\"configuration\":\"kms-site/hadoop.kms.authentication.signer.secret.provider.zookeeper.kerberos.keytab\"},\"name\":\"ranger_kms_ranger_kms_server_spnego\",\"principal\":{\"configuration\":\"kms-site/hadoop.kms.authentication.signer.secret.provider.zookeeper.kerberos.principal\"},\"reference\":\"/spnego\"},{\"keytab\":{\"configuration\":\"dbks-site/ranger.ks.kerberos.keytab\",\"file\":\"${keytab_dir}/rangerkms.service.keytab\",\"group\":{},\"owner\":{\"access\":\"r\",\"name\":\"${kms-env/kms_user}\"}},\"name\":\"rangerkms\",\"principal\":{\"configuration\":\"dbks-site/ranger.ks.kerberos.principal\",\"local_username\":\"keyadmin\",\"type\":\"service\",\"value\":\"rangerkms/_HOST@${realm}\"}}],\"name\":\"RANGER_KMS_SERVER\"}],\"configurations\":[{\"kms-site\":{\"hadoop.kms.authentication.kerberos.principal\":\"*\",\"hadoop.kms.authentication.type\":\"kerberos\"}},{\"ranger-kms-audit\":{\"xasecure.audit.destination.solr.force.use.inmemory.jaas.config\":\"true\",\"xasecure.audit.jaas.Client.loginModuleControlFlag\":\"required\",\"xasecure.audit.jaas.Client.loginModuleName\":\"com.sun.security.auth.module.Krb5LoginModule\",\"xasecure.audit.jaas.Client.option.serviceName\":\"solr\",\"xasecure.audit.jaas.Client.option.storeKey\":\"false\",\"xasecure.audit.jaas.Client.option.useKeyTab\":\"true\"}}],\"identities\":[{\"keytab\":{\"configuration\":\"kms-site/hadoop.kms.authentication.kerberos.keytab\"},\"name\":\"ranger_kms_spnego\",\"reference\":\"/spnego\"}],\"name\":\"RANGER_KMS\"},{\"components\":[{\"identities\":[{\"name\":\"slider_slider_hdfs\",\"reference\":\"/HDFS/NAMENODE/hdfs\"}],\"name\":\"SLIDER\"}],\"name\":\"SLIDER\"},{\"components\":[{\"configurations\":[{\"ams-hbase-site\":{\"hbase.superuser\":\"${activity-conf/global.activity.analyzer.user},${activity-conf/activity.explorer.user},${ams-env/ambari_metrics_user}\"}},{\"yarn-site\":{\"yarn.admin.acl\":\"${activity-conf/global.activity.analyzer.user},dr.who,${yarn-env/yarn_user}\"}}],\"identities\":[{\"keytab\":{\"configuration\":\"activity-conf/global.activity.analyzer.user.keytab\",\"file\":\"${keytab_dir}/activity-analyzer.headless.keytab\",\"group\":{},\"owner\":{\"access\":\"r\"}},\"name\":\"activity_analyzer\",\"principal\":{\"configuration\":\"activity-conf/global.activity.analyzer.user.principal\",\"local_username\":\"${activity-conf/global.activity.analyzer.user}\",\"type\":\"service\",\"value\":\"${activity-conf/global.activity.analyzer.user}/_HOST@${realm}\"}}],\"name\":\"ACTIVITY_ANALYZER\"},{\"configurations\":[{\"ams-hbase-site\":{\"hbase.superuser\":\"${activity-conf/global.activity.analyzer.user},${activity-conf/activity.explorer.user},${ams-env/ambari_metrics_user}\"}},{\"yarn-site\":{\"yarn.admin.acl\":\"${activity-conf/global.activity.analyzer.user},dr.who,${yarn-env/yarn_user}\"}}],\"identities\":[{\"keytab\":{\"configuration\":\"activity-conf/activity.explorer.user.keytab\",\"file\":\"${keytab_dir}/activity-explorer.headless.keytab\",\"group\":{},\"owner\":{\"access\":\"r\"}},\"name\":\"activity_explorer\",\"principal\":{\"configuration\":\"activity-conf/activity.explorer.user.principal\",\"local_username\":\"${activity-conf/activity.explorer.user}\",\"type\":\"service\",\"value\":\"${activity-conf/activity.explorer.user}/_HOST@${realm}\"}}],\"name\":\"ACTIVITY_EXPLORER\"}],\"name\":\"SMARTSENSE\"},{\"components\":[{\"identities\":[{\"name\":\"hdfs\",\"reference\":\"/HDFS/NAMENODE/hdfs\"},{\"keytab\":{\"configuration\":\"livy-conf/livy.server.auth.kerberos.keytab\"},\"name\":\"livy_spnego\",\"principal\":{\"configuration\":\"livy-conf/livy.server.auth.kerberos.principal\"},\"reference\":\"/spnego\"},{\"keytab\":{\"configuration\":\"livy-conf/livy.server.launch.kerberos.keytab\",\"file\":\"${keytab_dir}/livy.service.keytab\",\"group\":{\"access\":\"\",\"name\":\"${cluster-env/user_group}\"},\"owner\":{\"access\":\"r\",\"name\":\"${livy-env/livy_user}\"}},\"name\":\"livyuser\",\"principal\":{\"configuration\":\"livy-conf/livy.server.launch.kerberos.principal\",\"local_username\":\"${livy-env/livy_user}\",\"type\":\"service\",\"value\":\"${livy-env/livy_user}/_HOST@${realm}\"}}],\"name\":\"LIVY_SERVER\"},{\"name\":\"SPARK_CLIENT\"},{\"identities\":[{\"name\":\"hdfs\",\"reference\":\"/HDFS/NAMENODE/hdfs\"}],\"name\":\"SPARK_JOBHISTORYSERVER\"},{\"identities\":[{\"name\":\"hdfs\",\"reference\":\"/HDFS/NAMENODE/hdfs\"},{\"name\":\"hive_server_hive\",\"reference\":\"/HIVE/HIVE_SERVER/hive_server_hive\"}],\"name\":\"SPARK_THRIFTSERVER\"}],\"configurations\":[{\"core-site\":{\"hadoop.proxyuser.${livy-env/livy_user}.groups\":\"*\",\"hadoop.proxyuser.${livy-env/livy_user}.hosts\":\"*\"}},{\"livy-conf\":{\"livy.impersonation.enabled\":\"true\",\"livy.server.auth.type\":\"kerberos\"}},{\"spark-defaults\":{\"spark.history.kerberos.enabled\":\"true\"}},{\"spark-thrift-sparkconf\":{\"spark.yarn.keytab\":\"${spark-env/hive_kerberos_keytab}\",\"spark.yarn.principal\":\"${spark-env/hive_kerberos_principal}\"}}],\"identities\":[{\"name\":\"spark_smokeuser\",\"reference\":\"/smokeuser\"},{\"keytab\":{\"configuration\":\"spark-defaults/spark.history.kerberos.keytab\",\"file\":\"${keytab_dir}/spark.headless.keytab\",\"group\":{\"access\":\"\",\"name\":\"${cluster-env/user_group}\"},\"owner\":{\"access\":\"r\",\"name\":\"${spark-env/spark_user}\"}},\"name\":\"sparkuser\",\"principal\":{\"configuration\":\"spark-defaults/spark.history.kerberos.principal\",\"local_username\":\"${spark-env/spark_user}\",\"type\":\"user\",\"value\":\"${spark-env/spark_user}${principal_suffix}@${realm}\"}}],\"name\":\"SPARK\"},{\"components\":[{\"identities\":[{\"name\":\"hdfs\",\"reference\":\"/HDFS/NAMENODE/hdfs\"},{\"keytab\":{\"configuration\":\"livy2-conf/livy.server.auth.kerberos.keytab\"},\"name\":\"livy_spnego\",\"principal\":{\"configuration\":\"livy2-conf/livy.server.auth.kerberos.principal\"},\"reference\":\"/spnego\"},{\"keytab\":{\"configuration\":\"livy2-conf/livy.server.launch.kerberos.keytab\",\"file\":\"${keytab_dir}/livy.service.keytab\",\"group\":{\"access\":\"\",\"name\":\"${cluster-env/user_group}\"},\"owner\":{\"access\":\"r\",\"name\":\"${livy2-env/livy2_user}\"}},\"name\":\"livyuser\",\"principal\":{\"configuration\":\"livy2-conf/livy.server.launch.kerberos.principal\",\"local_username\":\"${livy2-env/livy2_user}\",\"type\":\"service\",\"value\":\"${livy2-env/livy2_user}/_HOST@${realm}\"}}],\"name\":\"LIVY2_SERVER\"},{\"name\":\"SPARK2_CLIENT\"},{\"identities\":[{\"name\":\"hdfs\",\"reference\":\"/HDFS/NAMENODE/hdfs\"}],\"name\":\"SPARK2_JOBHISTORYSERVER\"},{\"identities\":[{\"name\":\"hdfs\",\"reference\":\"/HDFS/NAMENODE/hdfs\"},{\"name\":\"hive_server_hive\",\"reference\":\"/HIVE/HIVE_SERVER/hive_server_hive\"}],\"name\":\"SPARK2_THRIFTSERVER\"}],\"configurations\":[{\"core-site\":{\"hadoop.proxyuser.${livy2-env/livy2_user}.groups\":\"*\",\"hadoop.proxyuser.${livy2-env/livy2_user}.hosts\":\"*\"}},{\"livy2-conf\":{\"livy.impersonation.enabled\":\"true\",\"livy.server.auth.type\":\"kerberos\"}},{\"spark2-defaults\":{\"spark.history.kerberos.enabled\":\"true\"}},{\"spark2-thrift-sparkconf\":{\"spark.yarn.keytab\":\"${spark2-env/hive_kerberos_keytab}\",\"spark.yarn.principal\":\"${spark2-env/hive_kerberos_principal}\"}}],\"identities\":[{\"name\":\"spark2_smokeuser\",\"reference\":\"/smokeuser\"},{\"keytab\":{\"configuration\":\"spark2-defaults/spark.history.kerberos.keytab\",\"file\":\"${keytab_dir}/spark.headless.keytab\",\"group\":{\"access\":\"\",\"name\":\"${cluster-env/user_group}\"},\"owner\":{\"access\":\"r\",\"name\":\"${spark2-env/spark_user}\"}},\"name\":\"spark2user\",\"principal\":{\"configuration\":\"spark2-defaults/spark.history.kerberos.principal\",\"local_username\":\"${spark2-env/spark_user}\",\"type\":\"user\",\"value\":\"${spark2-env/spark_user}${principal_suffix}@${realm}\"}}],\"name\":\"SPARK2\"},{\"components\":[{\"name\":\"SQOOP\"}],\"configurations\":[{\"sqoop-atlas-application.properties\":{\"atlas.jaas.KafkaClient.option.renewTicket\":\"true\",\"atlas.jaas.KafkaClient.option.useTicketCache\":\"true\"}}],\"name\":\"SQOOP\"},{\"components\":[{\"identities\":[{\"name\":\"drpc_server\",\"reference\":\"/STORM/NIMBUS/nimbus_server\"}],\"name\":\"DRPC_SERVER\"},{\"identities\":[{\"keytab\":{\"configuration\":\"storm-env/nimbus_keytab\",\"file\":\"${keytab_dir}/nimbus.service.keytab\",\"group\":{\"access\":\"\",\"name\":\"${cluster-env/user_group}\"},\"owner\":{\"access\":\"r\",\"name\":\"${storm-env/storm_user}\"}},\"name\":\"nimbus_server\",\"principal\":{\"configuration\":\"storm-env/nimbus_principal_name\",\"type\":\"service\",\"value\":\"nimbus/_HOST@${realm}\"}},{\"keytab\":{\"configuration\":\"ranger-storm-audit/xasecure.audit.jaas.Client.option.keyTab\"},\"name\":\"storm_numbus_storm_components\",\"principal\":{\"configuration\":\"ranger-storm-audit/xasecure.audit.jaas.Client.option.principal\"},\"reference\":\"/STORM/storm_components\"}],\"name\":\"NIMBUS\"},{\"identities\":[{\"keytab\":{\"configuration\":\"storm-env/storm_ui_keytab\"},\"name\":\"storm_storm_ui_server_spnego\",\"principal\":{\"configuration\":\"storm-env/storm_ui_principal_name\"},\"reference\":\"/spnego\"}],\"name\":\"STORM_UI_SERVER\"},{\"name\":\"SUPERVISOR\"}],\"configurations\":[{\"ranger-storm-audit\":{\"xasecure.audit.destination.solr.force.use.inmemory.jaas.config\":\"true\",\"xasecure.audit.jaas.Client.loginModuleControlFlag\":\"required\",\"xasecure.audit.jaas.Client.loginModuleName\":\"com.sun.security.auth.module.Krb5LoginModule\",\"xasecure.audit.jaas.Client.option.serviceName\":\"solr\",\"xasecure.audit.jaas.Client.option.storeKey\":\"false\",\"xasecure.audit.jaas.Client.option.useKeyTab\":\"true\"}},{\"storm-site\":{\"drpc.authorizer\":\"org.apache.storm.security.auth.authorizer.DRPCSimpleACLAuthorizer\",\"java.security.auth.login.config\":\"{{conf_dir}}/storm_jaas.conf\",\"nimbus.admins\":\"[\\u0027{{storm_bare_jaas_principal}}\\u0027, \\u0027{{ambari_bare_jaas_principal}}\\u0027]\",\"nimbus.authorizer\":\"org.apache.storm.security.auth.authorizer.SimpleACLAuthorizer\",\"nimbus.impersonation.acl\":\"{ {{storm_bare_jaas_principal}} : {hosts: [\\u0027*\\u0027], groups: [\\u0027*\\u0027]}}\",\"nimbus.impersonation.authorizer\":\"org.apache.storm.security.auth.authorizer.ImpersonationAuthorizer\",\"nimbus.supervisor.users\":\"[\\u0027{{storm_bare_jaas_principal}}\\u0027]\",\"storm.principal.tolocal\":\"org.apache.storm.security.auth.KerberosPrincipalToLocal\",\"storm.zookeeper.superACL\":\"sasl:{{storm_bare_jaas_principal}}\",\"supervisor.enable\":\"true\",\"ui.filter\":\"org.apache.hadoop.security.authentication.server.AuthenticationFilter\",\"ui.filter.params\":\"{\\u0027type\\u0027: \\u0027kerberos\\u0027, \\u0027kerberos.principal\\u0027: \\u0027{{storm_ui_jaas_principal}}\\u0027, \\u0027kerberos.keytab\\u0027: \\u0027{{storm_ui_keytab_path}}\\u0027, \\u0027kerberos.name.rules\\u0027: \\u0027DEFAULT\\u0027}\"}}],\"identities\":[{\"keytab\":{\"configuration\":\"storm-env/storm_keytab\",\"file\":\"${keytab_dir}/storm.headless.keytab\",\"group\":{\"access\":\"\",\"name\":\"${cluster-env/user_group}\"},\"owner\":{\"access\":\"r\",\"name\":\"${storm-env/storm_user}\"}},\"name\":\"storm_components\",\"principal\":{\"configuration\":\"storm-env/storm_principal_name\",\"type\":\"user\",\"value\":\"${storm-env/storm_user}${principal_suffix}@${realm}\"}},{\"name\":\"storm_smokeuser\",\"reference\":\"/smokeuser\"},{\"name\":\"storm_spnego\",\"reference\":\"/spnego\"},{\"keytab\":{\"configuration\":\"storm-atlas-application.properties/atlas.jaas.KafkaClient.option.keyTab\"},\"name\":\"storm_storm_components\",\"principal\":{\"configuration\":\"storm-atlas-application.properties/atlas.jaas.KafkaClient.option.principal\"},\"reference\":\"/STORM/storm_components\"}],\"name\":\"STORM\"},{\"components\":[{\"name\":\"SUPERSET\"}],\"configurations\":[{\"superset\":{\"ENABLE_KERBEROS_AUTHENTICATION\":\"True\",\"KERBEROS_REINIT_TIME_SEC\":\"3600\"}}],\"identities\":[{\"keytab\":{\"configuration\":\"superset/KERBEROS_KEYTAB\",\"file\":\"${keytab_dir}/superset.headless.keytab\",\"group\":{\"access\":\"r\",\"name\":\"${cluster-env/user_group}\"},\"owner\":{\"access\":\"r\",\"name\":\"${superset-env/superset_user}\"}},\"name\":\"superset\",\"principal\":{\"configuration\":\"superset/KERBEROS_PRINCIPAL\",\"local_username\":\"${superset-env/superset_user}\",\"type\":\"user\",\"value\":\"${superset-env/superset_user}${principal_suffix}@${realm}\"}},{\"name\":\"superset_smokeuser\",\"reference\":\"/smokeuser\"}],\"name\":\"SUPERSET\"},{\"components\":[{\"configurations\":[{\"tez-site\":{\"tez.am.view-acls\":\"\"}}],\"identities\":[{\"name\":\"tez_tez_client_hdfs\",\"reference\":\"/HDFS/NAMENODE/hdfs\"}],\"name\":\"TEZ_CLIENT\"}],\"name\":\"TEZ\"},{\"components\":[{\"identities\":[{\"keytab\":{\"configuration\":\"yarn-site/yarn.timeline-service.keytab\",\"file\":\"${keytab_dir}/yarn.service.keytab\",\"group\":{\"access\":\"\",\"name\":\"${cluster-env/user_group}\"},\"owner\":{\"access\":\"r\",\"name\":\"${yarn-env/yarn_user}\"}},\"name\":\"app_timeline_server_yarn\",\"principal\":{\"configuration\":\"yarn-site/yarn.timeline-service.principal\",\"local_username\":\"${yarn-env/yarn_user}\",\"type\":\"service\",\"value\":\"yarn/_HOST@${realm}\"}},{\"name\":\"yarn_app_timeline_server_hdfs\",\"reference\":\"/HDFS/NAMENODE/hdfs\"},{\"keytab\":{\"configuration\":\"yarn-site/yarn.timeline-service.http-authentication.kerberos.keytab\"},\"name\":\"yarn_app_timeline_server_spnego\",\"principal\":{\"configuration\":\"yarn-site/yarn.timeline-service.http-authentication.kerberos.principal\"},\"reference\":\"/spnego\"}],\"name\":\"APP_TIMELINE_SERVER\"},{\"configurations\":[{\"yarn-site\":{\"yarn.nodemanager.container-executor.class\":\"org.apache.hadoop.yarn.server.nodemanager.LinuxContainerExecutor\"}}],\"identities\":[{\"keytab\":{\"configuration\":\"yarn-site/yarn.nodemanager.keytab\",\"file\":\"${keytab_dir}/nm.service.keytab\",\"group\":{\"access\":\"\",\"name\":\"${cluster-env/user_group}\"},\"owner\":{\"access\":\"r\",\"name\":\"${yarn-env/yarn_user}\"}},\"name\":\"nodemanager_nm\",\"principal\":{\"configuration\":\"yarn-site/yarn.nodemanager.principal\",\"local_username\":\"${yarn-env/yarn_user}\",\"type\":\"service\",\"value\":\"nm/_HOST@${realm}\"}},{\"keytab\":{\"configuration\":\"yarn-site/yarn.nodemanager.webapp.spnego-keytab-file\"},\"name\":\"yarn_nodemanager_spnego\",\"principal\":{\"configuration\":\"yarn-site/yarn.nodemanager.webapp.spnego-principal\"},\"reference\":\"/spnego\"}],\"name\":\"NODEMANAGER\"},{\"identities\":[{\"keytab\":{\"configuration\":\"yarn-site/yarn.resourcemanager.keytab\",\"file\":\"${keytab_dir}/rm.service.keytab\",\"group\":{\"access\":\"\",\"name\":\"${cluster-env/user_group}\"},\"owner\":{\"access\":\"r\",\"name\":\"${yarn-env/yarn_user}\"}},\"name\":\"resource_manager_rm\",\"principal\":{\"configuration\":\"yarn-site/yarn.resourcemanager.principal\",\"local_username\":\"${yarn-env/yarn_user}\",\"type\":\"service\",\"value\":\"rm/_HOST@${realm}\"}},{\"keytab\":{\"configuration\":\"ranger-yarn-audit/xasecure.audit.jaas.Client.option.keyTab\"},\"name\":\"yarn_resourcemanager_resource_manager_rm\",\"principal\":{\"configuration\":\"ranger-yarn-audit/xasecure.audit.jaas.Client.option.principal\"},\"reference\":\"/YARN/RESOURCEMANAGER/resource_manager_rm\"},{\"keytab\":{\"configuration\":\"yarn-site/yarn.resourcemanager.webapp.spnego-keytab-file\"},\"name\":\"yarn_resourcemanager_spnego\",\"principal\":{\"configuration\":\"yarn-site/yarn.resourcemanager.webapp.spnego-principal\"},\"reference\":\"/spnego\"}],\"name\":\"RESOURCEMANAGER\"}],\"configurations\":[{\"capacity-scheduler\":{\"yarn.scheduler.capacity.root.acl_administer_jobs\":\"${yarn-env/yarn_user}\",\"yarn.scheduler.capacity.root.acl_administer_queue\":\"${yarn-env/yarn_user}\",\"yarn.scheduler.capacity.root.default.acl_administer_jobs\":\"${yarn-env/yarn_user}\",\"yarn.scheduler.capacity.root.default.acl_administer_queue\":\"${yarn-env/yarn_user}\",\"yarn.scheduler.capacity.root.default.acl_submit_applications\":\"${yarn-env/yarn_user}\"}},{\"core-site\":{\"hadoop.proxyuser.${yarn-env/yarn_user}.groups\":\"*\",\"hadoop.proxyuser.${yarn-env/yarn_user}.hosts\":\"${clusterHostInfo/rm_host}\"}},{\"ranger-yarn-audit\":{\"xasecure.audit.destination.solr.force.use.inmemory.jaas.config\":\"true\",\"xasecure.audit.jaas.Client.loginModuleControlFlag\":\"required\",\"xasecure.audit.jaas.Client.loginModuleName\":\"com.sun.security.auth.module.Krb5LoginModule\",\"xasecure.audit.jaas.Client.option.serviceName\":\"solr\",\"xasecure.audit.jaas.Client.option.storeKey\":\"false\",\"xasecure.audit.jaas.Client.option.useKeyTab\":\"true\"}},{\"yarn-site\":{\"hadoop.registry.client.auth\":\"kerberos\",\"hadoop.registry.jaas.context\":\"Client\",\"hadoop.registry.secure\":\"true\",\"hadoop.registry.system.accounts\":\"sasl:${principals/YARN/APP_TIMELINE_SERVER/app_timeline_server_yarn|principalPrimary()},sasl:${principals/MAPREDUCE2/HISTORYSERVER/history_server_jhs|principalPrimary()},sasl:${principals/HDFS/NAMENODE/hdfs|principalPrimary()},sasl:${principals/YARN/RESOURCEMANAGER/resource_manager_rm|principalPrimary()},sasl:${principals/HIVE/HIVE_SERVER/hive_server_hive|principalPrimary()}\",\"yarn.acl.enable\":\"true\",\"yarn.admin.acl\":\"${yarn-env/yarn_user},dr.who\",\"yarn.resourcemanager.proxy-user-privileges.enabled\":\"true\",\"yarn.resourcemanager.proxyuser.*.groups\":\"\",\"yarn.resourcemanager.proxyuser.*.hosts\":\"\",\"yarn.resourcemanager.proxyuser.*.users\":\"\",\"yarn.resourcemanager.zk-acl\":\"sasl:${principals/YARN/RESOURCEMANAGER/resource_manager_rm|principalPrimary()}:rwcda\",\"yarn.timeline-service.enabled\":\"true\",\"yarn.timeline-service.http-authentication.cookie.domain\":\"\",\"yarn.timeline-service.http-authentication.cookie.path\":\"\",\"yarn.timeline-service.http-authentication.kerberos.name.rules\":\"\",\"yarn.timeline-service.http-authentication.proxyuser.*.groups\":\"\",\"yarn.timeline-service.http-authentication.proxyuser.*.hosts\":\"\",\"yarn.timeline-service.http-authentication.proxyuser.*.users\":\"\",\"yarn.timeline-service.http-authentication.signature.secret\":\"\",\"yarn.timeline-service.http-authentication.signature.secret.file\":\"\",\"yarn.timeline-service.http-authentication.signer.secret.provider\":\"\",\"yarn.timeline-service.http-authentication.signer.secret.provider.object\":\"\",\"yarn.timeline-service.http-authentication.token.validity\":\"\",\"yarn.timeline-service.http-authentication.type\":\"kerberos\"}}],\"identities\":[{\"name\":\"yarn_smokeuser\",\"reference\":\"/smokeuser\"},{\"name\":\"yarn_spnego\",\"reference\":\"/spnego\"}],\"name\":\"YARN\"},{\"components\":[{\"name\":\"ZEPPELIN_MASTER\"}],\"configurations\":[{\"core-site\":{\"hadoop.proxyuser.${zeppelin-env/zeppelin_user}.groups\":\"*\",\"hadoop.proxyuser.${zeppelin-env/zeppelin_user}.hosts\":\"*\"}},{\"zeppelin-env\":{\"zeppelin.kerberos.enabled\":\"true\"}}],\"identities\":[{\"name\":\"zeppelin_smokeuser\",\"reference\":\"/smokeuser\"},{\"keytab\":{\"configuration\":\"zeppelin-env/zeppelin.server.kerberos.keytab\",\"file\":\"${keytab_dir}/zeppelin.server.kerberos.keytab\",\"group\":{\"access\":\"\",\"name\":\"${cluster-env/user_group}\"},\"owner\":{\"access\":\"r\",\"name\":\"${zeppelin-env/zeppelin_user}\"}},\"name\":\"zeppelin_user\",\"principal\":{\"configuration\":\"zeppelin-env/zeppelin.server.kerberos.principal\",\"local_username\":\"${zeppelin-env/zeppelin_user}\",\"type\":\"user\",\"value\":\"${zeppelin-env/zeppelin_user}${principal_suffix}@${realm}\"}}],\"name\":\"ZEPPELIN\"},{\"components\":[{\"identities\":[{\"keytab\":{\"configuration\":\"zookeeper-env/zookeeper_keytab_path\",\"file\":\"${keytab_dir}/zk.service.keytab\",\"group\":{\"access\":\"\",\"name\":\"${cluster-env/user_group}\"},\"owner\":{\"access\":\"r\",\"name\":\"${zookeeper-env/zk_user}\"}},\"name\":\"zookeeper_zk\",\"principal\":{\"configuration\":\"zookeeper-env/zookeeper_principal_name\",\"type\":\"service\",\"value\":\"zookeeper/_HOST@${realm}\"}}],\"name\":\"ZOOKEEPER_SERVER\"}],\"identities\":[{\"name\":\"zookeeper_smokeuser\",\"reference\":\"/smokeuser\"}],\"name\":\"ZOOKEEPER\"}]}";

  @Test
  public void testupdateKerberosDescriptorArtifact() throws Exception {
    ArtifactEntity artifactEntity = new ArtifactEntity();
    artifactEntity.setArtifactName("kerberos_descriptor");
    artifactEntity.setArtifactData(GSON.<Map<String, Object>>fromJson(KERBEROS_DESCRIPTOR_JSON, Map.class));

    UpgradeCatalog270 upgradeCatalog270 = createMockBuilder(UpgradeCatalog270.class)
            .createMock();

    expect(artifactDAO.merge(artifactEntity)).andReturn(artifactEntity);

    replay(upgradeCatalog270);

    upgradeCatalog270.updateKerberosDescriptorArtifact(artifactDAO, artifactEntity);

    int oldCount = substringCount(KERBEROS_DESCRIPTOR_JSON, AMBARI_INFRA_OLD_NAME);
    int newCount = substringCount(GSON.toJson(artifactEntity.getArtifactData()), AMBARI_INFRA_NEW_NAME);
    assertThat(newCount, is(oldCount));

    verify(upgradeCatalog270);
  }

  private int substringCount(String source, String substring) {
    int count = 0;
    int i = -1;
    while ((i = source.indexOf(substring, i + 1)) != -1) {
      ++count;
    }
    return count;
  }
}
