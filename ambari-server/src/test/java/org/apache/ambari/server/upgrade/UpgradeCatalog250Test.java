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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariManagementControllerImpl;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.MaintenanceStateHelper;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.ArtifactDAO;
import org.apache.ambari.server.orm.dao.PermissionDAO;
import org.apache.ambari.server.orm.dao.ResourceTypeDAO;
import org.apache.ambari.server.orm.dao.RoleAuthorizationDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.ArtifactEntity;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.RoleAuthorizationEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.kerberos.AbstractKerberosDescriptorContainer;
import org.apache.ambari.server.state.kerberos.KerberosComponentDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptorFactory;
import org.apache.ambari.server.state.kerberos.KerberosIdentityDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosKeytabDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosPrincipalDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor;
import org.apache.ambari.server.state.stack.OsFamily;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.MockType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

/**
 * {@link UpgradeCatalog250} unit tests.
 */
@RunWith(EasyMockRunner.class)
public class UpgradeCatalog250Test {

  //  private Injector injector;
  @Mock(type = MockType.STRICT)
  private Provider<EntityManager> entityManagerProvider;

  @Mock(type = MockType.NICE)
  private EntityManager entityManager;

  @Mock(type = MockType.NICE)
  private DBAccessor dbAccessor;

  @Mock(type = MockType.NICE)
  private Configuration configuration;

  @Mock(type = MockType.NICE)
  private Connection connection;

  @Mock(type = MockType.NICE)
  private Statement statement;

  @Mock(type = MockType.NICE)
  private ResultSet resultSet;

  @Mock(type = MockType.NICE)
  private OsFamily osFamily;

  @Mock(type = MockType.NICE)
  private KerberosHelper kerberosHelper;

  @Mock(type = MockType.NICE)
  private ActionManager actionManager;

  @Mock(type = MockType.NICE)
  private Config config;

  @Mock(type = MockType.STRICT)
  private Service service;

  @Mock(type = MockType.NICE)
  private Clusters clusters;

  @Mock(type = MockType.NICE)
  private Cluster cluster;

  @Mock(type = MockType.NICE)
  private Injector injector;

  private UpgradeCatalog250 upgradeCatalog250;

  @Before
  public void init() {
    reset(entityManagerProvider, injector);

    expect(entityManagerProvider.get()).andReturn(entityManager).anyTimes();

    expect(injector.getInstance(Gson.class)).andReturn(null).anyTimes();
    expect(injector.getInstance(MaintenanceStateHelper.class)).andReturn(null).anyTimes();
    expect(injector.getInstance(KerberosHelper.class)).andReturn(kerberosHelper).anyTimes();

    replay(entityManagerProvider, injector);

    upgradeCatalog250 = new UpgradeCatalog250(injector);
  }

  @After
  public void tearDown() {
  }

  @Test
  public void testExecuteDDLUpdates() throws Exception {
    // !!! setup capture for host_version
    dbAccessor.addUniqueConstraint("host_version", "UQ_host_repo", "repo_version_id", "host_id");

    Capture<DBAccessor.DBColumnInfo> groupGroupType = newCapture();
    dbAccessor.addColumn(eq(UpgradeCatalog250.GROUPS_TABLE), capture(groupGroupType));
    dbAccessor.addUniqueConstraint("groups", "UNQ_groups_0", "group_name", "group_type");

    expectLastCall().once();

    // !!! setup capture for servicecomponent_version
    Capture<List<DBAccessor.DBColumnInfo>> capturedComponentVersionColumns = newCapture();

    dbAccessor.createTable(eq(UpgradeCatalog250.COMPONENT_VERSION_TABLE), capture(capturedComponentVersionColumns),
        eq((String[]) null));

    dbAccessor.addPKConstraint(eq(UpgradeCatalog250.COMPONENT_VERSION_TABLE),
        eq(UpgradeCatalog250.COMPONENT_VERSION_PK), eq("id"));
    dbAccessor.addFKConstraint(eq(UpgradeCatalog250.COMPONENT_VERSION_TABLE),
        eq(UpgradeCatalog250.COMPONENT_VERSION_FK_COMPONENT), eq("component_id"),
        eq(UpgradeCatalog250.COMPONENT_TABLE), eq("id"), eq(false));
    dbAccessor.addFKConstraint(eq(UpgradeCatalog250.COMPONENT_VERSION_TABLE),
        eq(UpgradeCatalog250.COMPONENT_VERSION_FK_REPO_VERSION), eq("repo_version_id"),
        eq("repo_version"), eq("repo_version_id"), eq(false));

    // servicedesiredstate table
    Capture<DBAccessor.DBColumnInfo> capturedCredentialStoreEnabledCol = newCapture();
    dbAccessor.addColumn(eq(UpgradeCatalog250.SERVICE_DESIRED_STATE_TABLE), capture(capturedCredentialStoreEnabledCol));

    expect(dbAccessor.getConnection()).andReturn(connection).anyTimes();
    expect(connection.createStatement()).andReturn(statement).anyTimes();
    expect(statement.executeQuery(anyObject(String.class))).andReturn(resultSet).anyTimes();
    expect(configuration.getDatabaseType()).andReturn(Configuration.DatabaseType.POSTGRES).anyTimes();

    replay(dbAccessor, configuration, connection, statement, resultSet);

    Module module = new Module() {
      @Override
      public void configure(Binder binder) {
        binder.bind(DBAccessor.class).toInstance(dbAccessor);
        binder.bind(OsFamily.class).toInstance(osFamily);
        binder.bind(EntityManager.class).toInstance(entityManager);
        binder.bind(Configuration.class).toInstance(configuration);
      }
    };

    Injector injector = Guice.createInjector(module);
    UpgradeCatalog250 upgradeCatalog250 = injector.getInstance(UpgradeCatalog250.class);
    upgradeCatalog250.executeDDLUpdates();

    DBAccessor.DBColumnInfo capturedGroupTypeColumn = groupGroupType.getValue();
    Assert.assertNotNull(capturedGroupTypeColumn);
    Assert.assertEquals(UpgradeCatalog250.GROUP_TYPE_COL, capturedGroupTypeColumn.getName());
    Assert.assertEquals(String.class, capturedGroupTypeColumn.getType());
    Assert.assertEquals(null, capturedGroupTypeColumn.getLength());
    Assert.assertEquals("LOCAL", capturedGroupTypeColumn.getDefaultValue());
    Assert.assertEquals(false, capturedGroupTypeColumn.isNullable());

    verify(dbAccessor);

    // !!! check the captured for host_version
    // (no checks)

    // !!! check the captured for servicecomponent_version
    Map<String, DBAccessor.DBColumnInfo> expected = new HashMap<>();
    expected.put("id", new DBAccessor.DBColumnInfo("id", Long.class, null, null, false));
    expected.put("component_id", new DBAccessor.DBColumnInfo("component_id", Long.class, null, null, false));
    expected.put("repo_version_id", new DBAccessor.DBColumnInfo("repo_version_id", Long.class, null, null, false));
    expected.put("state", new DBAccessor.DBColumnInfo("state", String.class, 32, null, false));
    expected.put("user_name", new DBAccessor.DBColumnInfo("user_name", String.class, 255, null, false));

    List<DBAccessor.DBColumnInfo> captured = capturedComponentVersionColumns.getValue();
    Assert.assertEquals(5, captured.size());

    for (DBAccessor.DBColumnInfo column : captured) {
      DBAccessor.DBColumnInfo expectedColumn = expected.remove(column.getName());

      Assert.assertNotNull(expectedColumn);
      Assert.assertEquals(expectedColumn.getDefaultValue(), column.getDefaultValue());
      Assert.assertEquals(expectedColumn.getName(), column.getName());
      Assert.assertEquals(expectedColumn.getLength(), column.getLength());
      Assert.assertEquals(expectedColumn.getType(), column.getType());
      Assert.assertEquals(expectedColumn.getClass(), column.getClass());
    }

    // did we get them all?
    Assert.assertEquals(0, expected.size());

    // Verify if credential_store_enabled columns
    // were added to servicedesiredstate table

    DBAccessor.DBColumnInfo capturedCredentialStoreEnabledColValues = capturedCredentialStoreEnabledCol.getValue();
    Assert.assertNotNull(capturedCredentialStoreEnabledColValues);

    Assert.assertEquals(UpgradeCatalog250.CREDENTIAL_STORE_ENABLED_COL, capturedCredentialStoreEnabledColValues.getName());
    Assert.assertEquals(null, capturedCredentialStoreEnabledColValues.getLength());
    Assert.assertEquals(Short.class, capturedCredentialStoreEnabledColValues.getType());
    Assert.assertEquals(0, capturedCredentialStoreEnabledColValues.getDefaultValue());
    Assert.assertEquals(false, capturedCredentialStoreEnabledColValues.isNullable());
  }

  @Test
  public void testExecuteDMLUpdates() throws Exception {
    Method updateAmsConfigs = UpgradeCatalog250.class.getDeclaredMethod("updateAMSConfigs");
    Method updateHadoopEnvConfigs = UpgradeCatalog250.class.getDeclaredMethod("updateHadoopEnvConfigs");
    Method updateKafkaConfigs = UpgradeCatalog250.class.getDeclaredMethod("updateKafkaConfigs");
    Method updateTablesForZeppelinViewRemoval = UpgradeCatalog250.class.getDeclaredMethod("unInstallAllZeppelinViews");
    Method updateZeppelinConfigs = UpgradeCatalog250.class.getDeclaredMethod("updateZeppelinConfigs");
    Method updateAtlasConfigs = UpgradeCatalog250.class.getDeclaredMethod("updateAtlasConfigs");
    Method addNewConfigurationsFromXml = AbstractUpgradeCatalog.class.getDeclaredMethod("addNewConfigurationsFromXml");
    Method updateHIVEInteractiveConfigs = UpgradeCatalog250.class.getDeclaredMethod("updateHIVEInteractiveConfigs");
    Method updateLogSearchConfigs = UpgradeCatalog250.class.getDeclaredMethod("updateLogSearchConfigs");
    Method updateLogSearchAlert = UpgradeCatalog250.class.getDeclaredMethod("updateLogSearchAlert");
    Method updateAmbariInfraConfigs = UpgradeCatalog250.class.getDeclaredMethod("updateAmbariInfraConfigs");
    Method updateRangerUrlConfigs = UpgradeCatalog250.class.getDeclaredMethod("updateRangerUrlConfigs");
    Method addManageServiceAutoStartPermissions = UpgradeCatalog250.class.getDeclaredMethod("addManageServiceAutoStartPermissions");
    Method addManageAlertNotificationsPermissions = UpgradeCatalog250.class.getDeclaredMethod("addManageAlertNotificationsPermissions");
    Method updateTezHistoryUrlBase = UpgradeCatalog250.class.getDeclaredMethod("updateTezHistoryUrlBase");
    Method updateYarnSite = UpgradeCatalog250.class.getDeclaredMethod("updateYarnSite");
    Method updateAlerts = UpgradeCatalog250.class.getDeclaredMethod("updateStormAlerts");
    Method removeAlertDuplicates = UpgradeCatalog250.class.getDeclaredMethod("removeAlertDuplicates");
    Method updateKerberosDescriptorArtifacts = AbstractUpgradeCatalog.class.getDeclaredMethod("updateKerberosDescriptorArtifacts");
    Method fixHBaseMasterCPUUtilizationAlertDefinition = UpgradeCatalog250.class.getDeclaredMethod("fixHBaseMasterCPUUtilizationAlertDefinition");

    UpgradeCatalog250 upgradeCatalog250 = createMockBuilder(UpgradeCatalog250.class)
        .addMockedMethod(updateAmsConfigs)
        .addMockedMethod(updateHadoopEnvConfigs)
        .addMockedMethod(updateKafkaConfigs)
        .addMockedMethod(updateHIVEInteractiveConfigs)
        .addMockedMethod(updateTablesForZeppelinViewRemoval)
        .addMockedMethod(updateZeppelinConfigs)
        .addMockedMethod(updateAtlasConfigs)
        .addMockedMethod(updateLogSearchConfigs)
        .addMockedMethod(updateAmbariInfraConfigs)
        .addMockedMethod(addNewConfigurationsFromXml)
        .addMockedMethod(updateRangerUrlConfigs)
        .addMockedMethod(addManageServiceAutoStartPermissions)
        .addMockedMethod(addManageAlertNotificationsPermissions)
        .addMockedMethod(updateYarnSite)
        .addMockedMethod(updateAlerts)
        .addMockedMethod(updateLogSearchAlert)
        .addMockedMethod(removeAlertDuplicates)
        .addMockedMethod(updateKerberosDescriptorArtifacts)
        .addMockedMethod(fixHBaseMasterCPUUtilizationAlertDefinition)
        .addMockedMethod(updateTezHistoryUrlBase)
        .createMock();


    upgradeCatalog250.updateAMSConfigs();
    expectLastCall().once();

    upgradeCatalog250.updateHadoopEnvConfigs();
    expectLastCall().once();

    upgradeCatalog250.addNewConfigurationsFromXml();
    expectLastCall().once();

    upgradeCatalog250.updateKafkaConfigs();
    expectLastCall().once();

    upgradeCatalog250.updateHIVEInteractiveConfigs();
    expectLastCall().once();

    upgradeCatalog250.unInstallAllZeppelinViews();
    expectLastCall().once();

    upgradeCatalog250.updateZeppelinConfigs();
    expectLastCall().once();

    upgradeCatalog250.updateAtlasConfigs();
    expectLastCall().once();

    upgradeCatalog250.updateLogSearchConfigs();
    expectLastCall().once();

    upgradeCatalog250.updateAmbariInfraConfigs();
    expectLastCall().once();

    upgradeCatalog250.updateRangerUrlConfigs();
    expectLastCall().once();

    upgradeCatalog250.addManageServiceAutoStartPermissions();
    expectLastCall().once();

    upgradeCatalog250.addManageAlertNotificationsPermissions();
    expectLastCall().once();

    upgradeCatalog250.updateTezHistoryUrlBase();
    expectLastCall().once();

    upgradeCatalog250.updateYarnSite();
    expectLastCall().once();

    upgradeCatalog250.updateStormAlerts();
    expectLastCall().once();

    upgradeCatalog250.updateLogSearchAlert();
    expectLastCall().once();

    upgradeCatalog250.removeAlertDuplicates();
    expectLastCall().once();

    upgradeCatalog250.updateKerberosDescriptorArtifacts();
    expectLastCall().once();

    upgradeCatalog250.fixHBaseMasterCPUUtilizationAlertDefinition();
    expectLastCall().once();

    replay(upgradeCatalog250);

    upgradeCatalog250.executeDMLUpdates();

    verify(upgradeCatalog250);
  }

  @Test
  public void testUpdateAlerts_StormUIWebAlert() {
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariManagementController mockAmbariManagementController = easyMockSupport.createNiceMock(AmbariManagementController.class);
    final Clusters mockClusters = easyMockSupport.createStrictMock(Clusters.class);
    final Cluster mockClusterExpected = easyMockSupport.createNiceMock(Cluster.class);
    final AlertDefinitionDAO mockAlertDefinitionDAO = easyMockSupport.createNiceMock(AlertDefinitionDAO.class);
    final AlertDefinitionEntity stormWebUIAlertMock = easyMockSupport.createNiceMock(AlertDefinitionEntity.class);

    final Injector mockInjector = createInjector(mockAmbariManagementController, mockClusters, mockAlertDefinitionDAO);
    long clusterId = 1;

    expect(mockAmbariManagementController.getClusters()).andReturn(mockClusters).once();
    expect(mockClusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", mockClusterExpected);
    }}).atLeastOnce();
    expect(mockClusterExpected.getClusterId()).andReturn(clusterId).anyTimes();
    expect(mockAlertDefinitionDAO.findByName(eq(clusterId), eq("storm_webui")))
        .andReturn(stormWebUIAlertMock).atLeastOnce();
    expect(stormWebUIAlertMock.getSource()).andReturn("{\"uri\": {\n" +
        "            \"http\": \"{{storm-site/ui.port}}\",\n" +
        "            \"kerberos_keytab\": \"{{storm-env/storm_ui_keytab}}\",\n" +
        "            \"kerberos_principal\": \"{{storm-env/storm_ui_principal_name}}\",\n" +
        "            \"connection_timeout\": 5.0\n" +
        "          } }");

    stormWebUIAlertMock.setSource("{\"uri\":{\"http\":\"{{storm-site/ui.port}}\",\"kerberos_keytab\":\"{{storm-env/storm_ui_keytab}}\",\"kerberos_principal\":\"{{storm-env/storm_ui_principal_name}}\",\"connection_timeout\":5.0,\"https\":\"{{storm-site/ui.https.port}}\",\"https_property\":\"{{storm-site/ui.https.keystore.type}}\",\"https_property_value\":\"jks\"}}");

    expectLastCall().once();

    easyMockSupport.replayAll();
    mockInjector.getInstance(UpgradeCatalog250.class).updateStormAlerts();
    easyMockSupport.verifyAll();
  }

  @Test
  public void testUpdateAlerts_StormUIPortAlert() {
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariManagementController mockAmbariManagementController = easyMockSupport.createNiceMock(AmbariManagementController.class);
    final Clusters mockClusters = easyMockSupport.createStrictMock(Clusters.class);
    final Cluster mockClusterExpected = easyMockSupport.createNiceMock(Cluster.class);
    final AlertDefinitionDAO mockAlertDefinitionDAO = easyMockSupport.createNiceMock(AlertDefinitionDAO.class);
    final AlertDefinitionEntity stormUIPortAlertMock = easyMockSupport.createNiceMock(AlertDefinitionEntity.class);

    final Injector mockInjector = createInjector(mockAmbariManagementController, mockClusters, mockAlertDefinitionDAO);
    long clusterId = 1;

    expect(mockAmbariManagementController.getClusters()).andReturn(mockClusters).once();
    expect(mockClusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", mockClusterExpected);
    }}).atLeastOnce();
    expect(mockClusterExpected.getClusterId()).andReturn(clusterId).anyTimes();
    expect(mockAlertDefinitionDAO.findByName(eq(clusterId), eq("storm_server_process")))
        .andReturn(stormUIPortAlertMock).atLeastOnce();

    mockAlertDefinitionDAO.remove(stormUIPortAlertMock);
    expectLastCall().once();

    easyMockSupport.replayAll();

    mockInjector.getInstance(UpgradeCatalog250.class).updateStormAlerts();
    easyMockSupport.verifyAll();
  }

  @Test
  public void testUpdateAlerts_LogSearchUIWebAlert() {
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariManagementController mockAmbariManagementController = easyMockSupport.createNiceMock(AmbariManagementController.class);
    final Clusters mockClusters = easyMockSupport.createStrictMock(Clusters.class);
    final Cluster mockClusterExpected = easyMockSupport.createNiceMock(Cluster.class);
    final AlertDefinitionDAO mockAlertDefinitionDAO = easyMockSupport.createNiceMock(AlertDefinitionDAO.class);
    final AlertDefinitionEntity logSearchWebUIAlertMock = easyMockSupport.createNiceMock(AlertDefinitionEntity.class);

    final Injector mockInjector = createInjector(mockAmbariManagementController, mockClusters, mockAlertDefinitionDAO);
    long clusterId = 1;

    expect(mockAmbariManagementController.getClusters()).andReturn(mockClusters).once();
    expect(mockClusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", mockClusterExpected);
    }}).atLeastOnce();
    expect(mockClusterExpected.getClusterId()).andReturn(clusterId).anyTimes();
    expect(mockAlertDefinitionDAO.findByName(eq(clusterId), eq("logsearch_ui")))
      .andReturn(logSearchWebUIAlertMock).atLeastOnce();
    expect(logSearchWebUIAlertMock.getSource()).andReturn("{\"uri\": {\n" +
      "            \"http\": \"{{logsearch-env/logsearch_ui_port}}\",\n" +
      "            \"https\": \"{{logsearch-env/logsearch_ui_port}}\"\n" +
      "          } }");

    logSearchWebUIAlertMock.setSource("{\"uri\":{\"http\":\"{{logsearch-env/logsearch_ui_port}}\",\"https\":\"{{logsearch-env/logsearch_ui_port}}\",\"https_property\":\"{{logsearch-env/logsearch_ui_protocol}}\",\"https_property_value\":\"https\"}}");

    expectLastCall().once();

    easyMockSupport.replayAll();
    mockInjector.getInstance(UpgradeCatalog250.class).updateLogSearchAlert();
    easyMockSupport.verifyAll();
  }

  @Test
  public void testFixHBaseMasterCPUUtilizationAlertDefinition() {
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariManagementController mockAmbariManagementController = easyMockSupport.createNiceMock(AmbariManagementController.class);
    final Clusters mockClusters = easyMockSupport.createStrictMock(Clusters.class);
    final Cluster mockClusterExpected = easyMockSupport.createNiceMock(Cluster.class);
    final AlertDefinitionDAO mockAlertDefinitionDAO = easyMockSupport.createNiceMock(AlertDefinitionDAO.class);
    final AlertDefinitionEntity hbaseMasterCPUAlertMock = easyMockSupport.createNiceMock(AlertDefinitionEntity.class);

    String brokenSource = "{\"uri\":{\"http\":\"{{hbase-site/hbase.master.info.port}}\",\"kerberos_keytab\":\"{{hbase-site/hbase.security.authentication.spnego.kerberos.principal}}\",\"kerberos_principal\":\"{{hbase-site/hbase.security.authentication.spnego.kerberos.keytab}}\",\"default_port\":60010,\"connection_timeout\":5.0},\"jmx\":{\"property_list\":[\"java.lang:type\\u003dOperatingSystem/SystemCpuLoad\",\"java.lang:type\\u003dOperatingSystem/AvailableProcessors\"],\"value\":\"{0} * 100\"},\"type\":\"METRIC\",\"reporting\":{\"ok\":{\"text\":\"{1} CPU, load {0:.1%}\"},\"warning\":{\"text\":\"{1} CPU, load {0:.1%}\",\"value\":200.0},\"critical\":{\"text\":\"{1} CPU, load {0:.1%}\",\"value\":250.0},\"units\":\"%\",\"type\":\"PERCENT\"}}";

    Capture<String> capturedFixedSource = newCapture();

    final Injector mockInjector = createInjector(mockAmbariManagementController, mockClusters, mockAlertDefinitionDAO);
    long clusterId = 1;

    expect(mockAmbariManagementController.getClusters()).andReturn(mockClusters).once();
    expect(mockClusters.getClusters()).andReturn(Collections.singletonMap("normal", mockClusterExpected)).atLeastOnce();
    expect(mockClusterExpected.getClusterId()).andReturn(clusterId).anyTimes();
    expect(mockAlertDefinitionDAO.findByName(eq(clusterId), eq("hbase_master_cpu"))).andReturn(hbaseMasterCPUAlertMock).atLeastOnce();
    expect(hbaseMasterCPUAlertMock.getDefinitionName()).andReturn("hbase_master_cpu").once();
    expect(hbaseMasterCPUAlertMock.getSource()).andReturn(brokenSource).once();

    hbaseMasterCPUAlertMock.setSource(capture(capturedFixedSource));
    expectLastCall().once();

    hbaseMasterCPUAlertMock.setHash(anyString());
    expectLastCall().once();

    expect(mockAlertDefinitionDAO.merge(hbaseMasterCPUAlertMock)).andReturn(hbaseMasterCPUAlertMock).once();

    easyMockSupport.replayAll();

    mockInjector.getInstance(UpgradeCatalog250.class).fixHBaseMasterCPUUtilizationAlertDefinition();
    easyMockSupport.verifyAll();

    String fixedSource = capturedFixedSource.getValue();
    Assert.assertNotNull(fixedSource);

    JsonObject sourceJson = new JsonParser().parse(fixedSource).getAsJsonObject();
    Assert.assertNotNull(sourceJson);

    JsonObject uriJson = sourceJson.get("uri").getAsJsonObject();
    Assert.assertNotNull(uriJson);

    JsonPrimitive primitive;
    primitive = uriJson.getAsJsonPrimitive("kerberos_keytab");
    Assert.assertTrue(primitive.isString());
    Assert.assertEquals("{{hbase-site/hbase.security.authentication.spnego.kerberos.keytab}}", primitive.getAsString());

    primitive = uriJson.getAsJsonPrimitive("kerberos_principal");
    Assert.assertTrue(primitive.isString());
    Assert.assertEquals("{{hbase-site/hbase.security.authentication.spnego.kerberos.principal}}", primitive.getAsString());
  }

  @Test
  public void testFixHBaseMasterCPUUtilizationAlertDefinitionMissingKerberosInfo() {
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariManagementController mockAmbariManagementController = easyMockSupport.createNiceMock(AmbariManagementController.class);
    final Clusters mockClusters = easyMockSupport.createStrictMock(Clusters.class);
    final Cluster mockClusterExpected = easyMockSupport.createNiceMock(Cluster.class);
    final AlertDefinitionDAO mockAlertDefinitionDAO = easyMockSupport.createNiceMock(AlertDefinitionDAO.class);
    final AlertDefinitionEntity hbaseMasterCPUAlertMock = easyMockSupport.createNiceMock(AlertDefinitionEntity.class);

    String brokenSource = "{\"uri\":{\"http\":\"{{hbase-site/hbase.master.info.port}}\",\"default_port\":60010,\"connection_timeout\":5.0},\"jmx\":{\"property_list\":[\"java.lang:type\\u003dOperatingSystem/SystemCpuLoad\",\"java.lang:type\\u003dOperatingSystem/AvailableProcessors\"],\"value\":\"{0} * 100\"},\"type\":\"METRIC\",\"reporting\":{\"ok\":{\"text\":\"{1} CPU, load {0:.1%}\"},\"warning\":{\"text\":\"{1} CPU, load {0:.1%}\",\"value\":200.0},\"critical\":{\"text\":\"{1} CPU, load {0:.1%}\",\"value\":250.0},\"units\":\"%\",\"type\":\"PERCENT\"}}";

    Capture<String> capturedFixedSource = newCapture();

    final Injector mockInjector = createInjector(mockAmbariManagementController, mockClusters, mockAlertDefinitionDAO);
    long clusterId = 1;

    expect(mockAmbariManagementController.getClusters()).andReturn(mockClusters).once();
    expect(mockClusters.getClusters()).andReturn(Collections.singletonMap("normal", mockClusterExpected)).atLeastOnce();
    expect(mockClusterExpected.getClusterId()).andReturn(clusterId).anyTimes();
    expect(mockAlertDefinitionDAO.findByName(eq(clusterId), eq("hbase_master_cpu"))).andReturn(hbaseMasterCPUAlertMock).atLeastOnce();
    expect(hbaseMasterCPUAlertMock.getDefinitionName()).andReturn("hbase_master_cpu").once();
    expect(hbaseMasterCPUAlertMock.getSource()).andReturn(brokenSource).once();

    expect(mockAlertDefinitionDAO.merge(hbaseMasterCPUAlertMock)).andReturn(hbaseMasterCPUAlertMock).anyTimes();

    easyMockSupport.replayAll();

    mockInjector.getInstance(UpgradeCatalog250.class).fixHBaseMasterCPUUtilizationAlertDefinition();
    easyMockSupport.verifyAll();

    Assert.assertFalse(capturedFixedSource.hasCaptured());
  }

  @Test
  public void testUpdateYarnSite() throws Exception {
    EasyMockSupport easyMockSupport = new EasyMockSupport();

    final String propertyToRemove = "yarn.nodemanager.linux-container-executor.cgroups.mount-path";
    final AmbariManagementController ambariManagementController = createNiceMock(AmbariManagementController.class);
    Config mockYarnEnv = easyMockSupport.createNiceMock(Config.class);
    Config mockYarnSite = easyMockSupport.createNiceMock(Config.class);

    HashMap<String, String> yarnEnv = new HashMap<String, String>() {{
      put("yarn_cgroups_enabled", "false");
    }};

    HashMap<String, String> yarnSite = new HashMap<String, String>() {{
      put(propertyToRemove, "");
    }};

    reset(clusters, cluster, injector);

    expect(injector.getInstance(AmbariManagementController.class)).andReturn(ambariManagementController).atLeastOnce();
    expect(ambariManagementController.getClusters()).andReturn(clusters).atLeastOnce();
    expect(clusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", cluster);
    }}).once();
    expect(cluster.getDesiredConfigByType("yarn-env")).andReturn(mockYarnEnv).atLeastOnce();
    expect(mockYarnEnv.getProperties()).andReturn(yarnEnv).anyTimes();
    expect(cluster.getDesiredConfigByType("yarn-site")).andReturn(mockYarnSite).atLeastOnce();
    expect(mockYarnSite.getProperties()).andReturn(yarnSite).anyTimes();

    replay(clusters, cluster, injector, ambariManagementController, mockYarnEnv, mockYarnSite);

    UpgradeCatalog250 upgradeCatalog250 = createMockBuilder(UpgradeCatalog250.class)
        .addMockedMethod("removeConfigurationPropertiesFromCluster")
        .withConstructor(injector)
        .createNiceMock();

    Capture<HashSet<String>> removeConfigName = EasyMock.newCapture();

    upgradeCatalog250.removeConfigurationPropertiesFromCluster(anyObject(Cluster.class), eq("yarn-site"), capture(removeConfigName));
    EasyMock.expectLastCall();

    replay(upgradeCatalog250);

    upgradeCatalog250.updateYarnSite();

    easyMockSupport.verifyAll();

    Set<String> updatedProperties = removeConfigName.getValue();
    assertTrue(updatedProperties.contains(propertyToRemove));

    reset(injector);
  }

  @Test
  public void testUpdateYarnSiteWithEnabledCGroups() throws Exception {
    EasyMockSupport easyMockSupport = new EasyMockSupport();

    final String propertyToRemove = "yarn.nodemanager.linux-container-executor.cgroups.mount-path";
    final AmbariManagementController ambariManagementController = createNiceMock(AmbariManagementController.class);
    Config mockYarnEnv = easyMockSupport.createNiceMock(Config.class);
    Config mockYarnSite = easyMockSupport.createNiceMock(Config.class);

    HashMap<String, String> yarnEnv = new HashMap<String, String>() {{
      put("yarn_cgroups_enabled", "true");
    }};

    HashMap<String, String> yarnSite = new HashMap<String, String>() {{
      put(propertyToRemove, "");
    }};

    reset(clusters, cluster, injector);

    expect(injector.getInstance(AmbariManagementController.class)).andReturn(ambariManagementController).atLeastOnce();
    expect(ambariManagementController.getClusters()).andReturn(clusters).atLeastOnce();
    expect(clusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", cluster);
    }}).once();
    expect(cluster.getDesiredConfigByType("yarn-env")).andReturn(mockYarnEnv).atLeastOnce();
    expect(mockYarnEnv.getProperties()).andReturn(yarnEnv).anyTimes();
    expect(cluster.getDesiredConfigByType("yarn-site")).andReturn(mockYarnSite).atLeastOnce();
    expect(mockYarnSite.getProperties()).andReturn(yarnSite).anyTimes();

    replay(clusters, cluster, injector, ambariManagementController, mockYarnEnv, mockYarnSite);

    UpgradeCatalog250 upgradeCatalog250 = createMockBuilder(UpgradeCatalog250.class)
        .addMockedMethod("removeConfigurationPropertiesFromCluster")
        .withConstructor(injector)
        .createNiceMock();

    Capture<HashSet<String>> removeConfigName = EasyMock.newCapture();

    upgradeCatalog250.removeConfigurationPropertiesFromCluster(anyObject(Cluster.class), eq("yarn-site"), capture(removeConfigName));
    EasyMock.expectLastCall().andThrow(new AssertionFailedError()).anyTimes();

    replay(upgradeCatalog250);

    upgradeCatalog250.updateYarnSite();

    reset(injector);
  }

  @Test
  public void testAmsEnvUpdateConfigs() throws Exception {

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

    Config mockAmsEnv = easyMockSupport.createNiceMock(Config.class);

    reset(clusters, cluster);
    expect(clusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", cluster);
    }}).once();
    expect(cluster.getDesiredConfigByType("ams-env")).andReturn(mockAmsEnv).atLeastOnce();
    expect(mockAmsEnv.getProperties()).andReturn(oldPropertiesAmsEnv).anyTimes();

    replay(clusters, mockAmsEnv, cluster);

    AmbariManagementControllerImpl controller = createMockBuilder(AmbariManagementControllerImpl.class)
        .addMockedMethod("createConfiguration")
        .addMockedMethod("getClusters", new Class[]{})
        .addMockedMethod("createConfig")
        .withConstructor(actionManager, clusters, injector)
        .createNiceMock();

    Injector injector2 = easyMockSupport.createNiceMock(Injector.class);
    Capture<Map<String, String>> propertiesCapture = EasyMock.newCapture();

    expect(injector2.getInstance(AmbariManagementController.class)).andReturn(controller).anyTimes();
    expect(controller.getClusters()).andReturn(clusters).anyTimes();
    expect(controller.createConfig(anyObject(Cluster.class), anyObject(StackId.class), anyString(), capture(propertiesCapture), anyString(),
        EasyMock.<Map<String, Map<String, String>>>anyObject())).andReturn(config).once();

    replay(controller, injector2);
    new UpgradeCatalog250(injector2).updateAMSConfigs();
    easyMockSupport.verifyAll();

    Map<String, String> updatedProperties = propertiesCapture.getValue();
    assertTrue(Maps.difference(newPropertiesAmsEnv, updatedProperties).areEqual());
  }

  @Test
  public void testAmsGrafanaIniUpdateConfigs() throws Exception {

    Map<String, String> oldProperties = new HashMap<String, String>() {
      {
        put("content", "[security]\n" +
          "# default admin user, created on startup\n" +
          "admin_user = {{ams_grafana_admin_user}}\n" +
          "\n" +
          "# default admin password, can be changed before first start of grafana,  or in profile settings\n" +
          "admin_password = {{ams_grafana_admin_pwd}}\n" +
          "\n" +
          "# used for signing\n" +
          ";secret_key = SW2YcwTIb9zpOOhoPsMm\n" +
          "\n" +
          "# Auto-login remember days\n" +
          ";login_remember_days = 7\n" +
          ";cookie_username = grafana_user\n" +
          ";cookie_remember_name = grafana_remember\n" +
          "\n" +
          "# disable gravatar profile images\n" +
          ";disable_gravatar = false\n" +
          "\n" +
          "# data source proxy whitelist (ip_or_domain:port seperated by spaces)\n" +
          ";data_source_proxy_whitelist =\n");
      }
    };
    Map<String, String> newProperties = new HashMap<String, String>() {
      {
        put("content", "[security]\n" +
          "# default admin user, created on startup\n" +
          "admin_user = {{ams_grafana_admin_user}}\n" +
          "\n" +
          "# default admin password, can be changed before first start of grafana,  or in profile settings\n" +
          ";admin_password =\n" +
          "\n" +
          "# used for signing\n" +
          ";secret_key = SW2YcwTIb9zpOOhoPsMm\n" +
          "\n" +
          "# Auto-login remember days\n" +
          ";login_remember_days = 7\n" +
          ";cookie_username = grafana_user\n" +
          ";cookie_remember_name = grafana_remember\n" +
          "\n" +
          "# disable gravatar profile images\n" +
          ";disable_gravatar = false\n" +
          "\n" +
          "# data source proxy whitelist (ip_or_domain:port seperated by spaces)\n" +
          ";data_source_proxy_whitelist =\n");
      }
    };
    EasyMockSupport easyMockSupport = new EasyMockSupport();

    Config mockAmsGrafanaIni = easyMockSupport.createNiceMock(Config.class);

    reset(clusters, cluster);
    expect(clusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", cluster);
    }}).once();
    expect(cluster.getDesiredConfigByType("ams-grafana-ini")).andReturn(mockAmsGrafanaIni).atLeastOnce();
    expect(mockAmsGrafanaIni.getProperties()).andReturn(oldProperties).anyTimes();

    replay(clusters, mockAmsGrafanaIni, cluster);

    AmbariManagementControllerImpl controller = createMockBuilder(AmbariManagementControllerImpl.class)
      .addMockedMethod("createConfiguration")
      .addMockedMethod("getClusters", new Class[]{})
      .addMockedMethod("createConfig")
      .withConstructor(actionManager, clusters, injector)
      .createNiceMock();

    Injector injector2 = easyMockSupport.createNiceMock(Injector.class);
    Capture<Map<String, String>> propertiesCapture = EasyMock.newCapture();

    expect(injector2.getInstance(AmbariManagementController.class)).andReturn(controller).anyTimes();
    expect(controller.getClusters()).andReturn(clusters).anyTimes();
    expect(controller.createConfig(anyObject(Cluster.class), anyObject(StackId.class), anyString(), capture(propertiesCapture), anyString(),
      EasyMock.<Map<String, Map<String, String>>>anyObject())).andReturn(config).once();

    replay(controller, injector2);
    new UpgradeCatalog250(injector2).updateAMSConfigs();
    easyMockSupport.verifyAll();

    Map<String, String> updatedProperties = propertiesCapture.getValue();
    assertTrue(Maps.difference(newProperties, updatedProperties).areEqual());
  }

  @Test
  public void testAmsHbaseSiteUpdateConfigs() throws Exception {

    Map<String, String> newProperties = new HashMap<String, String>() {
      {
        put("hbase.rootdir", "/user/ams/hbase");
      }
    };

    Map<String, String> oldProperties = new HashMap<String, String>() {
      {
        put("hbase.rootdir", "hdfs://namenodehost.domain.com:8020/user/ams/hbase");
      }
    };

    testAmsHbaseRootDir(oldProperties, newProperties);

    oldProperties = new HashMap<String, String>() {
      {
        put("hbase.rootdir", "hdfs://nameservice/user/ams/hbase");
      }
    };

    testAmsHbaseRootDir(oldProperties, newProperties);

  }

  private void testAmsHbaseRootDir(Map<String, String> oldProperties, Map<String, String> newProperties) throws AmbariException {
    Map<String, String> amsSite = new HashMap<String, String>() {
      {
        put("timeline.metrics.service.operation.mode", "distributed");
        put("timeline.metrics.hbase.fifo.compaction.enabled", "true");
      }
    };

    Map<String, String> newAmsSite = new HashMap<String, String>() {
      {
        put("timeline.metrics.service.operation.mode", "distributed");
      }
    };

    EasyMockSupport easyMockSupport = new EasyMockSupport();

    Config mockAmsHbaseSite = easyMockSupport.createNiceMock(Config.class);
    Config mockAmsSite = easyMockSupport.createNiceMock(Config.class);

    reset(clusters, cluster);
    expect(clusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", cluster);
    }}).once();

    expect(cluster.getDesiredConfigByType("ams-site")).andReturn(mockAmsSite).atLeastOnce();
    expect(mockAmsSite.getProperties()).andReturn(amsSite).anyTimes();
    expect(cluster.getDesiredConfigByType("ams-hbase-site")).andReturn(mockAmsHbaseSite).atLeastOnce();
    expect(mockAmsHbaseSite.getProperties()).andReturn(oldProperties).anyTimes();

    replay(clusters, mockAmsHbaseSite, mockAmsSite, cluster);

    AmbariManagementControllerImpl controller = createMockBuilder(AmbariManagementControllerImpl.class)
        .addMockedMethod("createConfiguration")
        .addMockedMethod("getClusters", new Class[]{})
        .addMockedMethod("createConfig")
        .withConstructor(actionManager, clusters, injector)
        .createNiceMock();

    Injector injector2 = easyMockSupport.createNiceMock(Injector.class);
    Capture<Map<String, String>> propertiesCapture = EasyMock.newCapture(CaptureType.ALL);

    expect(injector2.getInstance(AmbariManagementController.class)).andReturn(controller).anyTimes();
    expect(controller.getClusters()).andReturn(clusters).anyTimes();
    expect(controller.createConfig(anyObject(Cluster.class), anyObject(StackId.class), anyString(), capture(propertiesCapture), anyString(),
        EasyMock.<Map<String, Map<String, String>>>anyObject())).andReturn(config).times(2);

    replay(controller, injector2);
    new UpgradeCatalog250(injector2).updateAMSConfigs();
    easyMockSupport.verifyAll();

    assertTrue(propertiesCapture.getValues().size() == 2);

    Map<String, String> updatedProperties = propertiesCapture.getValues().get(0);
    assertTrue(Maps.difference(newAmsSite, updatedProperties).areEqual());

    updatedProperties = propertiesCapture.getValues().get(1);
    assertTrue(Maps.difference(newProperties, updatedProperties).areEqual());
  }

  @Test
  public void testKafkaUpdateConfigs() throws Exception {

    Map<String, String> oldProperties = new HashMap<String, String>() {
      {
        put("kafka.timeline.metrics.host", "{{metric_collector_host}}");
        put("kafka.timeline.metrics.port", "{{metric_collector_port}}");
      }
    };
    Map<String, String> newProperties = new HashMap<String, String>() {
      {
        put("kafka.timeline.metrics.port", "{{metric_collector_port}}");
      }
    };
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    Config mockKafkaBroker = easyMockSupport.createNiceMock(Config.class);

    reset(clusters, cluster);
    expect(clusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", cluster);
    }}).once();
    expect(cluster.getDesiredConfigByType("kafka-broker")).andReturn(mockKafkaBroker).atLeastOnce();
    expect(mockKafkaBroker.getProperties()).andReturn(oldProperties).anyTimes();

    replay(clusters, mockKafkaBroker, cluster);

    AmbariManagementControllerImpl controller = createMockBuilder(AmbariManagementControllerImpl.class)
        .addMockedMethod("createConfiguration")
        .addMockedMethod("getClusters", new Class[]{})
        .addMockedMethod("createConfig")
        .withConstructor(actionManager, clusters, injector)
        .createNiceMock();

    Injector injector2 = easyMockSupport.createNiceMock(Injector.class);
    Capture<Map<String, String>> propertiesCapture = EasyMock.newCapture();

    expect(injector2.getInstance(AmbariManagementController.class)).andReturn(controller).anyTimes();
    expect(controller.getClusters()).andReturn(clusters).anyTimes();
    expect(controller.createConfig(anyObject(Cluster.class), anyObject(StackId.class), anyString(), capture(propertiesCapture), anyString(),
        EasyMock.<Map<String, Map<String, String>>>anyObject())).andReturn(config).once();

    replay(controller, injector2);
    new UpgradeCatalog250(injector2).updateKafkaConfigs();
    easyMockSupport.verifyAll();

    Map<String, String> updatedProperties = propertiesCapture.getValue();
    assertTrue(Maps.difference(newProperties, updatedProperties).areEqual());
  }

  @Test
  public void testAmsLog4jUpdateConfigs() throws Exception {
    reset(clusters, cluster);
    expect(clusters.getClusters()).andReturn(ImmutableMap.of("normal", cluster)).once();

    EasyMockSupport easyMockSupport = new EasyMockSupport();

    Injector injector2 = easyMockSupport.createNiceMock(Injector.class);
    AmbariManagementControllerImpl controller = createMockBuilder(AmbariManagementControllerImpl.class)
        .addMockedMethod("createConfiguration")
        .addMockedMethod("getClusters", new Class[]{})
        .addMockedMethod("createConfig")
        .withConstructor(actionManager, clusters, injector)
        .createNiceMock();

    expect(injector2.getInstance(AmbariManagementController.class)).andReturn(controller).anyTimes();
    expect(controller.getClusters()).andReturn(clusters).anyTimes();

    Map<String, String> oldAmsLog4j = ImmutableMap.of(
        "content",
        "#\n" +
            "# Licensed to the Apache Software Foundation (ASF) under one\n" +
            "# or more contributor license agreements.  See the NOTICE file\n" +
            "# distributed with this work for additional information\n" +
            "# regarding copyright ownership.  The ASF licenses this file\n" +
            "# to you under the Apache License, Version 2.0 (the\n" +
            "# \"License\"); you may not use this file except in compliance\n" +
            "# with the License.  You may obtain a copy of the License at\n" +
            "#\n" +
            "#     http://www.apache.org/licenses/LICENSE-2.0\n" +
            "#\n" +
            "# Unless required by applicable law or agreed to in writing, software\n" +
            "# distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
            "# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
            "# See the License for the specific language governing permissions and\n" +
            "# limitations under the License.\n" +
            "#\n" +
            "\n" +
            "# Define some default values that can be overridden by system properties\n" +
            "ams.log.dir=.\n" +
            "ams.log.file=ambari-metrics-collector.log\n" +
            "\n" +
            "# Root logger option\n" +
            "log4j.rootLogger=INFO,file\n" +
            "\n" +
            "# Direct log messages to a log file\n" +
            "log4j.appender.file=org.apache.log4j.RollingFileAppender\n" +
            "log4j.appender.file.File=${ams.log.dir}/${ams.log.file}\n" +
            "log4j.appender.file.MaxFileSize=10MB\n" +
            "log4j.appender.file.MaxBackupIndex=12\n" +
            "log4j.appender.file.layout=org.apache.log4j.PatternLayout\n" +
            "log4j.appender.file.layout.ConversionPattern=%d{ISO8601} %p %c: %m%n");

    Map<String, String> expectedAmsLog4j = new HashMap<>();
    expectedAmsLog4j.put("content", "#\n" +
        "# Licensed to the Apache Software Foundation (ASF) under one\n" +
        "# or more contributor license agreements.  See the NOTICE file\n" +
        "# distributed with this work for additional information\n" +
        "# regarding copyright ownership.  The ASF licenses this file\n" +
        "# to you under the Apache License, Version 2.0 (the\n" +
        "# \"License\"); you may not use this file except in compliance\n" +
        "# with the License.  You may obtain a copy of the License at\n" +
        "#\n" +
        "#     http://www.apache.org/licenses/LICENSE-2.0\n" +
        "#\n" +
        "# Unless required by applicable law or agreed to in writing, software\n" +
        "# distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
        "# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
        "# See the License for the specific language governing permissions and\n" +
        "# limitations under the License.\n" +
        "#\n" +
        "\n" +
        "# Define some default values that can be overridden by system properties\n" +
        "ams.log.dir=.\n" +
        "ams.log.file=ambari-metrics-collector.log\n" +
        "\n" +
        "# Root logger option\n" +
        "log4j.rootLogger=INFO,file\n" +
        "\n" +
        "# Direct log messages to a log file\n" +
        "log4j.appender.file=org.apache.log4j.RollingFileAppender\n" +
        "log4j.appender.file.File=${ams.log.dir}/${ams.log.file}\n" +
        "log4j.appender.file.MaxFileSize={{ams_log_max_backup_size}}MB\n" +
        "log4j.appender.file.MaxBackupIndex={{ams_log_number_of_backup_files}}\n" +
        "log4j.appender.file.layout=org.apache.log4j.PatternLayout\n" +
        "log4j.appender.file.layout.ConversionPattern=%d{ISO8601} %p %c: %m%n");
    expectedAmsLog4j.put("ams_log_max_backup_size", "10");
    expectedAmsLog4j.put("ams_log_number_of_backup_files", "12");


    Config mockAmsLog4j = easyMockSupport.createNiceMock(Config.class);
    expect(cluster.getDesiredConfigByType("ams-log4j")).andReturn(mockAmsLog4j).atLeastOnce();
    expect(mockAmsLog4j.getProperties()).andReturn(oldAmsLog4j).anyTimes();
    Capture<Map<String, String>> AmsLog4jCapture = EasyMock.newCapture();
    expect(controller.createConfig(anyObject(Cluster.class), anyObject(StackId.class), anyString(), capture(AmsLog4jCapture), anyString(),
        anyObject(Map.class))).andReturn(config).once();

    Map<String, String> oldAmsHbaseLog4j = ImmutableMap.of(
        "content", "# Licensed to the Apache Software Foundation (ASF) under one\n" +
            "# or more contributor license agreements.  See the NOTICE file\n" +
            "# distributed with this work for additional information\n" +
            "# regarding copyright ownership.  The ASF licenses this file\n" +
            "# to you under the Apache License, Version 2.0 (the\n" +
            "# \"License\"); you may not use this file except in compliance\n" +
            "# with the License.  You may obtain a copy of the License at\n" +
            "#\n" +
            "#     http://www.apache.org/licenses/LICENSE-2.0\n" +
            "#\n" +
            "# Unless required by applicable law or agreed to in writing, software\n" +
            "# distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
            "# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
            "# See the License for the specific language governing permissions and\n" +
            "# limitations under the License.\n" +
            "\n" +
            "\n" +
            "# Define some default values that can be overridden by system properties\n" +
            "hbase.root.logger=INFO,console\n" +
            "hbase.security.logger=INFO,console\n" +
            "hbase.log.dir=.\n" +
            "hbase.log.file=hbase.log\n" +
            "\n" +
            "# Define the root logger to the system property \"hbase.root.logger\".\n" +
            "log4j.rootLogger=${hbase.root.logger}\n" +
            "\n" +
            "# Logging Threshold\n" +
            "log4j.threshold=ALL\n" +
            "\n" +
            "#\n" +
            "# Daily Rolling File Appender\n" +
            "#\n" +
            "log4j.appender.DRFA=org.apache.log4j.DailyRollingFileAppender\n" +
            "log4j.appender.DRFA.File=${hbase.log.dir}/${hbase.log.file}\n" +
            "\n" +
            "# Rollver at midnight\n" +
            "log4j.appender.DRFA.DatePattern=.yyyy-MM-dd\n" +
            "\n" +
            "# 30-day backup\n" +
            "#log4j.appender.DRFA.MaxBackupIndex=30\n" +
            "log4j.appender.DRFA.layout=org.apache.log4j.PatternLayout\n" +
            "\n" +
            "# Pattern format: Date LogLevel LoggerName LogMessage\n" +
            "log4j.appender.DRFA.layout.ConversionPattern=%d{ISO8601} %-5p [%t] %c{2}: %m%n\n" +
            "\n" +
            "# Rolling File Appender properties\n" +
            "hbase.log.maxfilesize=256MB\n" +
            "hbase.log.maxbackupindex=20\n" +
            "\n" +
            "# Rolling File Appender\n" +
            "log4j.appender.RFA=org.apache.log4j.RollingFileAppender\n" +
            "log4j.appender.RFA.File=${hbase.log.dir}/${hbase.log.file}\n" +
            "\n" +
            "log4j.appender.RFA.MaxFileSize=${hbase.log.maxfilesize}\n" +
            "log4j.appender.RFA.MaxBackupIndex=${hbase.log.maxbackupindex}\n" +
            "\n" +
            "log4j.appender.RFA.layout=org.apache.log4j.PatternLayout\n" +
            "log4j.appender.RFA.layout.ConversionPattern=%d{ISO8601} %-5p [%t] %c{2}: %m%n\n" +
            "\n" +
            "#\n" +
            "# Security audit appender\n" +
            "#\n" +
            "hbase.security.log.file=SecurityAuth.audit\n" +
            "hbase.security.log.maxfilesize=256MB\n" +
            "hbase.security.log.maxbackupindex=20\n" +
            "log4j.appender.RFAS=org.apache.log4j.RollingFileAppender\n" +
            "log4j.appender.RFAS.File=${hbase.log.dir}/${hbase.security.log.file}\n" +
            "log4j.appender.RFAS.MaxFileSize=${hbase.security.log.maxfilesize}\n" +
            "log4j.appender.RFAS.MaxBackupIndex=${hbase.security.log.maxbackupindex}\n" +
            "log4j.appender.RFAS.layout=org.apache.log4j.PatternLayout\n" +
            "log4j.appender.RFAS.layout.ConversionPattern=%d{ISO8601} %p %c: %m%n\n" +
            "log4j.category.SecurityLogger=${hbase.security.logger}\n" +
            "log4j.additivity.SecurityLogger=false\n" +
            "#log4j.logger.SecurityLogger.org.apache.hadoop.hbase.security.access.AccessController=TRACE\n" +
            "\n" +
            "#\n" +
            "# Null Appender\n" +
            "#\n" +
            "log4j.appender.NullAppender=org.apache.log4j.varia.NullAppender\n" +
            "\n" +
            "#\n" +
            "# console\n" +
            "# Add \"console\" to rootlogger above if you want to use this\n" +
            "#\n" +
            "log4j.appender.console=org.apache.log4j.ConsoleAppender\n" +
            "log4j.appender.console.target=System.err\n" +
            "log4j.appender.console.layout=org.apache.log4j.PatternLayout\n" +
            "log4j.appender.console.layout.ConversionPattern=%d{ISO8601} %-5p [%t] %c{2}: %m%n\n" +
            "\n" +
            "# Custom Logging levels\n" +
            "\n" +
            "log4j.logger.org.apache.zookeeper=INFO\n" +
            "#log4j.logger.org.apache.hadoop.fs.FSNamesystem=DEBUG\n" +
            "log4j.logger.org.apache.hadoop.hbase=INFO\n" +
            "# Make these two classes INFO-level. Make them DEBUG to see more zk debug.\n" +
            "log4j.logger.org.apache.hadoop.hbase.zookeeper.ZKUtil=INFO\n" +
            "log4j.logger.org.apache.hadoop.hbase.zookeeper.ZooKeeperWatcher=INFO\n" +
            "#log4j.logger.org.apache.hadoop.dfs=DEBUG\n" +
            "# Set this class to log INFO only otherwise its OTT\n" +
            "# Enable this to get detailed connection error/retry logging.\n" +
            "# log4j.logger.org.apache.hadoop.hbase.client.HConnectionManager$HConnectionImplementation=TRACE\n" +
            "\n" +
            "\n" +
            "# Uncomment this line to enable tracing on _every_ RPC call (this can be a lot of output)\n" +
            "#log4j.logger.org.apache.hadoop.ipc.HBaseServer.trace=DEBUG\n" +
            "\n" +
            "# Uncomment the below if you want to remove logging of client region caching'\n" +
            "# and scan of .META. messages\n" +
            "# log4j.logger.org.apache.hadoop.hbase.client.HConnectionManager$HConnectionImplementation=INFO\n" +
            "# log4j.logger.org.apache.hadoop.hbase.client.MetaScanner=INFO\n");

    Map<String, String> expectedAmsHbaseLog4j = new HashMap<>();
    expectedAmsHbaseLog4j.put("content", "# Licensed to the Apache Software Foundation (ASF) under one\n" +
        "# or more contributor license agreements.  See the NOTICE file\n" +
        "# distributed with this work for additional information\n" +
        "# regarding copyright ownership.  The ASF licenses this file\n" +
        "# to you under the Apache License, Version 2.0 (the\n" +
        "# \"License\"); you may not use this file except in compliance\n" +
        "# with the License.  You may obtain a copy of the License at\n" +
        "#\n" +
        "#     http://www.apache.org/licenses/LICENSE-2.0\n" +
        "#\n" +
        "# Unless required by applicable law or agreed to in writing, software\n" +
        "# distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
        "# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
        "# See the License for the specific language governing permissions and\n" +
        "# limitations under the License.\n" +
        "\n" +
        "\n" +
        "# Define some default values that can be overridden by system properties\n" +
        "hbase.root.logger=INFO,console\n" +
        "hbase.security.logger=INFO,console\n" +
        "hbase.log.dir=.\n" +
        "hbase.log.file=hbase.log\n" +
        "\n" +
        "# Define the root logger to the system property \"hbase.root.logger\".\n" +
        "log4j.rootLogger=${hbase.root.logger}\n" +
        "\n" +
        "# Logging Threshold\n" +
        "log4j.threshold=ALL\n" +
        "\n" +
        "#\n" +
        "# Daily Rolling File Appender\n" +
        "#\n" +
        "log4j.appender.DRFA=org.apache.log4j.DailyRollingFileAppender\n" +
        "log4j.appender.DRFA.File=${hbase.log.dir}/${hbase.log.file}\n" +
        "\n" +
        "# Rollver at midnight\n" +
        "log4j.appender.DRFA.DatePattern=.yyyy-MM-dd\n" +
        "\n" +
        "# 30-day backup\n" +
        "#log4j.appender.DRFA.MaxBackupIndex=30\n" +
        "log4j.appender.DRFA.layout=org.apache.log4j.PatternLayout\n" +
        "\n" +
        "# Pattern format: Date LogLevel LoggerName LogMessage\n" +
        "log4j.appender.DRFA.layout.ConversionPattern=%d{ISO8601} %-5p [%t] %c{2}: %m%n\n" +
        "\n" +
        "# Rolling File Appender properties\n" +
        "hbase.log.maxfilesize={{ams_hbase_log_maxfilesize}}MB\n" +
        "hbase.log.maxbackupindex={{ams_hbase_log_maxbackupindex}}\n" +
        "\n" +
        "# Rolling File Appender\n" +
        "log4j.appender.RFA=org.apache.log4j.RollingFileAppender\n" +
        "log4j.appender.RFA.File=${hbase.log.dir}/${hbase.log.file}\n" +
        "\n" +
        "log4j.appender.RFA.MaxFileSize=${hbase.log.maxfilesize}\n" +
        "log4j.appender.RFA.MaxBackupIndex=${hbase.log.maxbackupindex}\n" +
        "\n" +
        "log4j.appender.RFA.layout=org.apache.log4j.PatternLayout\n" +
        "log4j.appender.RFA.layout.ConversionPattern=%d{ISO8601} %-5p [%t] %c{2}: %m%n\n" +
        "\n" +
        "#\n" +
        "# Security audit appender\n" +
        "#\n" +
        "hbase.security.log.file=SecurityAuth.audit\n" +
        "hbase.security.log.maxfilesize={{ams_hbase_security_log_maxfilesize}}MB\n" +
        "hbase.security.log.maxbackupindex={{ams_hbase_security_log_maxbackupindex}}\n" +
        "log4j.appender.RFAS=org.apache.log4j.RollingFileAppender\n" +
        "log4j.appender.RFAS.File=${hbase.log.dir}/${hbase.security.log.file}\n" +
        "log4j.appender.RFAS.MaxFileSize=${hbase.security.log.maxfilesize}\n" +
        "log4j.appender.RFAS.MaxBackupIndex=${hbase.security.log.maxbackupindex}\n" +
        "log4j.appender.RFAS.layout=org.apache.log4j.PatternLayout\n" +
        "log4j.appender.RFAS.layout.ConversionPattern=%d{ISO8601} %p %c: %m%n\n" +
        "log4j.category.SecurityLogger=${hbase.security.logger}\n" +
        "log4j.additivity.SecurityLogger=false\n" +
        "#log4j.logger.SecurityLogger.org.apache.hadoop.hbase.security.access.AccessController=TRACE\n" +
        "\n" +
        "#\n" +
        "# Null Appender\n" +
        "#\n" +
        "log4j.appender.NullAppender=org.apache.log4j.varia.NullAppender\n" +
        "\n" +
        "#\n" +
        "# console\n" +
        "# Add \"console\" to rootlogger above if you want to use this\n" +
        "#\n" +
        "log4j.appender.console=org.apache.log4j.ConsoleAppender\n" +
        "log4j.appender.console.target=System.err\n" +
        "log4j.appender.console.layout=org.apache.log4j.PatternLayout\n" +
        "log4j.appender.console.layout.ConversionPattern=%d{ISO8601} %-5p [%t] %c{2}: %m%n\n" +
        "\n" +
        "# Custom Logging levels\n" +
        "\n" +
        "log4j.logger.org.apache.zookeeper=INFO\n" +
        "#log4j.logger.org.apache.hadoop.fs.FSNamesystem=DEBUG\n" +
        "log4j.logger.org.apache.hadoop.hbase=INFO\n" +
        "# Make these two classes INFO-level. Make them DEBUG to see more zk debug.\n" +
        "log4j.logger.org.apache.hadoop.hbase.zookeeper.ZKUtil=INFO\n" +
        "log4j.logger.org.apache.hadoop.hbase.zookeeper.ZooKeeperWatcher=INFO\n" +
        "#log4j.logger.org.apache.hadoop.dfs=DEBUG\n" +
        "# Set this class to log INFO only otherwise its OTT\n" +
        "# Enable this to get detailed connection error/retry logging.\n" +
        "# log4j.logger.org.apache.hadoop.hbase.client.HConnectionManager$HConnectionImplementation=TRACE\n" +
        "\n" +
        "\n" +
        "# Uncomment this line to enable tracing on _every_ RPC call (this can be a lot of output)\n" +
        "#log4j.logger.org.apache.hadoop.ipc.HBaseServer.trace=DEBUG\n" +
        "\n" +
        "# Uncomment the below if you want to remove logging of client region caching'\n" +
        "# and scan of .META. messages\n" +
        "# log4j.logger.org.apache.hadoop.hbase.client.HConnectionManager$HConnectionImplementation=INFO\n" +
        "# log4j.logger.org.apache.hadoop.hbase.client.MetaScanner=INFO\n");
    expectedAmsHbaseLog4j.put("ams_hbase_log_maxfilesize", "256");
    expectedAmsHbaseLog4j.put("ams_hbase_log_maxbackupindex", "20");
    expectedAmsHbaseLog4j.put("ams_hbase_security_log_maxfilesize", "256");
    expectedAmsHbaseLog4j.put("ams_hbase_security_log_maxbackupindex", "20");

    Config mockAmsHbaseLog4j = easyMockSupport.createNiceMock(Config.class);
    expect(cluster.getDesiredConfigByType("ams-hbase-log4j")).andReturn(mockAmsHbaseLog4j).atLeastOnce();
    expect(mockAmsHbaseLog4j.getProperties()).andReturn(oldAmsHbaseLog4j).anyTimes();
    Capture<Map<String, String>> AmsHbaseLog4jCapture = EasyMock.newCapture();
    expect(controller.createConfig(anyObject(Cluster.class), anyObject(StackId.class), anyString(), capture(AmsHbaseLog4jCapture), anyString(),
        anyObject(Map.class))).andReturn(config).once();

    replay(clusters, cluster);
    replay(controller, injector2);
    replay(mockAmsLog4j, mockAmsHbaseLog4j);
    new UpgradeCatalog250(injector2).updateAMSConfigs();
    easyMockSupport.verifyAll();

    Map<String, String> updatedAmsLog4jProperties = AmsLog4jCapture.getValue();
    assertTrue(Maps.difference(expectedAmsLog4j, updatedAmsLog4jProperties).areEqual());

    Map<String, String> updatedAmsHbaseLog4jProperties = AmsHbaseLog4jCapture.getValue();
    assertTrue(Maps.difference(expectedAmsHbaseLog4j, updatedAmsHbaseLog4jProperties).areEqual());

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
        .withConstructor(actionManager, clusters, injector)
        .createNiceMock();

    expect(injector2.getInstance(AmbariManagementController.class)).andReturn(controller).anyTimes();
    expect(controller.getClusters()).andReturn(clusters).anyTimes();

    Map<String, String> oldLogSearchProperties = ImmutableMap.of(
        "logsearch.external.auth.enabled", "true",
        "logsearch.external.auth.host_url", "host_url",
        "logsearch.external.auth.login_url", "login_url");

    Map<String, String> expectedLogSearchProperties = ImmutableMap.of(
        "logsearch.auth.external_auth.enabled", "true",
        "logsearch.auth.external_auth.host_url", "host_url",
        "logsearch.auth.external_auth.login_url", "login_url");

    Config mockLogSearchProperties = easyMockSupport.createNiceMock(Config.class);
    expect(cluster.getDesiredConfigByType("logsearch-properties")).andReturn(mockLogSearchProperties).atLeastOnce();
    expect(mockLogSearchProperties.getProperties()).andReturn(oldLogSearchProperties).anyTimes();
    Capture<Map<String, String>> logSearchPropertiesCapture = EasyMock.newCapture();
    expect(controller.createConfig(anyObject(Cluster.class), anyObject(StackId.class), anyString(), capture(logSearchPropertiesCapture), anyString(),
        EasyMock.<Map<String, Map<String, String>>>anyObject())).andReturn(config).once();

    Map<String, String> oldLogFeederEnv = ImmutableMap.of(
        "content", "infra_solr_ssl_enabled");

    Map<String, String> expectedLogFeederEnv = ImmutableMap.of(
        "content", "logfeeder_use_ssl");

    Config mockLogFeederEnv = easyMockSupport.createNiceMock(Config.class);
    expect(cluster.getDesiredConfigByType("logfeeder-env")).andReturn(mockLogFeederEnv).atLeastOnce();
    expect(mockLogFeederEnv.getProperties()).andReturn(oldLogFeederEnv).anyTimes();
    Capture<Map<String, String>> logFeederEnvCapture = EasyMock.newCapture();
    expect(controller.createConfig(anyObject(Cluster.class), anyObject(StackId.class), anyString(), capture(logFeederEnvCapture), anyString(),
        EasyMock.<Map<String, Map<String, String>>>anyObject())).andReturn(config).once();

    Map<String, String> oldLogSearchEnv = new HashMap<>();
    oldLogSearchEnv.put("logsearch_solr_audit_logs_use_ranger", "false");
    oldLogSearchEnv.put("logsearch_solr_audit_logs_zk_node", "zk_node");
    oldLogSearchEnv.put("logsearch_solr_audit_logs_zk_quorum", "zk_quorum");
    oldLogSearchEnv.put("logsearch_ui_protocol", "http");
    oldLogSearchEnv.put("logsearch_truststore_location", "/etc/security/serverKeys/logsearch.trustStore.jks");
    oldLogSearchEnv.put("logsearch_keystore_location", "/etc/security/serverKeys/logsearch.keyStore.jks");
    oldLogSearchEnv.put("content", "infra_solr_ssl_enabled or logsearch_ui_protocol == 'https'");

    Map<String, String> expectedLogSearchEnv = ImmutableMap.of(
        "logsearch_ui_protocol", "http",
        "logsearch_truststore_location", "/etc/ambari-logsearch-portal/conf/keys/logsearch.jks",
        "logsearch_keystore_location", "/etc/ambari-logsearch-portal/conf/keys/logsearch.jks",
        "content", "logsearch_use_ssl");

    Config mockLogSearchEnv = easyMockSupport.createNiceMock(Config.class);
    expect(cluster.getDesiredConfigByType("logsearch-env")).andReturn(mockLogSearchEnv).atLeastOnce();
    expect(mockLogSearchEnv.getProperties()).andReturn(oldLogSearchEnv).anyTimes();
    Capture<Map<String, String>> logSearchEnvCapture = EasyMock.newCapture();
    expect(controller.createConfig(anyObject(Cluster.class), anyObject(StackId.class), anyString(), capture(logSearchEnvCapture), anyString(),
        EasyMock.<Map<String, Map<String, String>>>anyObject())).andReturn(config).once();

    Map<String, String> oldLogFeederLog4j = ImmutableMap.of(
        "content",
        "    <appender name=\"rolling_file\" class=\"org.apache.log4j.RollingFileAppender\">\n" +
            "    <param name=\"file\" value=\"{{logfeeder_log_dir}}/logfeeder.log\"/>\n" +
            "    <param name=\"append\" value=\"true\"/>\n" +
            "    <param name=\"maxFileSize\" value=\"11MB\"/>\n" +
            "    <param name=\"maxBackupIndex\" value=\"12\"/>\n" +
            "    <layout class=\"org.apache.log4j.PatternLayout\">\n" +
            "      <param name=\"ConversionPattern\" value=\"%d [%t] %-5p %C{6} (%F:%L) - %m%n\"/>\n" +
            "    </layout>\n" +
            "  </appender>\n" +
            "\n" +
            "  <appender name=\"rolling_file_json\"\n" +
            "    class=\"org.apache.ambari.logsearch.appender.LogsearchRollingFileAppender\">\n" +
            "    <param name=\"file\" value=\"{{logfeeder_log_dir}}/logsearch-logfeeder.json\" />\n" +
            "    <param name=\"append\" value=\"true\" />\n" +
            "    <param name=\"maxFileSize\" value=\"13MB\" />\n" +
            "    <param name=\"maxBackupIndex\" value=\"14\" />\n" +
            "    <layout class=\"org.apache.ambari.logsearch.appender.LogsearchConversion\" />\n" +
            "  </appender>");

    Map<String, String> expectedLogFeederLog4j = ImmutableMap.of(
        "content",
        "    <appender name=\"rolling_file\" class=\"org.apache.log4j.RollingFileAppender\">\n" +
            "    <param name=\"file\" value=\"{{logfeeder_log_dir}}/logfeeder.log\"/>\n" +
            "    <param name=\"append\" value=\"true\"/>\n" +
            "    <param name=\"maxFileSize\" value=\"{{logfeeder_log_maxfilesize}}MB\"/>\n" +
            "    <param name=\"maxBackupIndex\" value=\"{{logfeeder_log_maxbackupindex}}\"/>\n" +
            "    <layout class=\"org.apache.log4j.PatternLayout\">\n" +
            "      <param name=\"ConversionPattern\" value=\"%d [%t] %-5p %C{6} (%F:%L) - %m%n\"/>\n" +
            "    </layout>\n" +
            "  </appender>\n" +
            "\n" +
            "  <appender name=\"rolling_file_json\"\n" +
            "    class=\"org.apache.ambari.logsearch.appender.LogsearchRollingFileAppender\">\n" +
            "    <param name=\"file\" value=\"{{logfeeder_log_dir}}/logsearch-logfeeder.json\" />\n" +
            "    <param name=\"append\" value=\"true\" />\n" +
            "    <param name=\"maxFileSize\" value=\"{{logfeeder_json_log_maxfilesize}}MB\" />\n" +
            "    <param name=\"maxBackupIndex\" value=\"{{logfeeder_json_log_maxbackupindex}}\" />\n" +
            "    <layout class=\"org.apache.ambari.logsearch.appender.LogsearchConversion\" />\n" +
            "  </appender>",
        "logfeeder_log_maxfilesize", "11",
        "logfeeder_log_maxbackupindex", "12",
        "logfeeder_json_log_maxfilesize", "13",
        "logfeeder_json_log_maxbackupindex", "14");

    Config mockLogFeederLog4j = easyMockSupport.createNiceMock(Config.class);
    expect(cluster.getDesiredConfigByType("logfeeder-log4j")).andReturn(mockLogFeederLog4j).atLeastOnce();
    expect(mockLogFeederLog4j.getProperties()).andReturn(oldLogFeederLog4j).anyTimes();
    Capture<Map<String, String>> logFeederLog4jCapture = EasyMock.newCapture();
    expect(controller.createConfig(anyObject(Cluster.class), anyObject(StackId.class), anyString(), capture(logFeederLog4jCapture), anyString(),
        EasyMock.<Map<String, Map<String, String>>>anyObject())).andReturn(config).once();

    Map<String, String> oldLogSearchLog4j = ImmutableMap.of(
        "content",
        "  <appender name=\"rolling_file\" class=\"org.apache.log4j.RollingFileAppender\">\n" +
            "    <param name=\"file\" value=\"{{logsearch_log_dir}}/logsearch.err\" />\n" +
            "    <param name=\"Threshold\" value=\"info\" />\n" +
            "    <param name=\"append\" value=\"true\" />\n" +
            "    <param name=\"maxFileSize\" value=\"11MB\" />\n" +
            "    <param name=\"maxBackupIndex\" value=\"12\" />\n" +
            "    <layout class=\"org.apache.log4j.PatternLayout\">\n" +
            "      <param name=\"ConversionPattern\" value=\"%d [%t] %-5p %C{6} (%F:%L) - %m%n\" />\n" +
            "    </layout>\n" +
            "  </appender>\n" +
            "\n" +
            "  <appender name=\"rolling_file_json\" class=\"org.apache.ambari.logsearch.appender.LogsearchRollingFileAppender\">\n" +
            "    <param name=\"file\" value=\"{{logsearch_log_dir}}/logsearch.json\"/>\n" +
            "    <param name=\"append\" value=\"true\"/>\n" +
            "    <param name=\"maxFileSize\" value=\"13MB\"/>\n" +
            "    <param name=\"maxBackupIndex\" value=\"14\"/>\n" +
            "    <layout class=\"org.apache.ambari.logsearch.appender.LogsearchConversion\"/>\n" +
            "  </appender>\n" +
            "\n" +
            "  <appender name=\"audit_rolling_file_json\" class=\"org.apache.ambari.logsearch.appender.LogsearchRollingFileAppender\">\n" +
            "    <param name=\"file\" value=\"{{logsearch_log_dir}}/logsearch-audit.json\"/>\n" +
            "    <param name=\"append\" value=\"true\"/>\n" +
            "    <param name=\"maxFileSize\" value=\"15MB\"/>\n" +
            "    <param name=\"maxBackupIndex\" value=\"16\"/>\n" +
            "    <layout class=\"org.apache.ambari.logsearch.appender.LogsearchConversion\"/>\n" +
            "  </appender>\n" +
            "\n" +
            "  <appender name=\"performance_analyzer_json\" class=\"org.apache.ambari.logsearch.appender.LogsearchRollingFileAppender\">\n" +
            "    <param name=\"file\" value=\"{{logsearch_log_dir}}/logsearch-performance.json\"/>\n" +
            "    <param name=\"Threshold\" value=\"info\"/>\n" +
            "    <param name=\"append\" value=\"true\"/>\n" +
            "    <param name=\"maxFileSize\" value=\"17MB\"/>\n" +
            "    <param name=\"maxBackupIndex\" value=\"18\"/>\n" +
            "    <layout class=\"org.apache.ambari.logsearch.appender.LogsearchConversion\"/>\n" +
            "  </appender>\n" +
            "\n" +
            "  <logger name=\"org.apache.ambari.logsearch.audit\" additivity=\"true\">\n" +
            "     <appender-ref ref=\"audit_rolling_file_json\"/>\n" +
            "  </logger>\n" +
            "\n" +
            "  <logger name=\"org.apache.ambari.logsearch.performance\" additivity=\"false\">\n" +
            "    <appender-ref ref=\"performance_analyzer_json\"/>\n" +
            "  </logger>\n" +
            "\n" +
            "  <category name=\"org.apache.ambari.logsearch\" additivity=\"false\">\n" +
            "    <priority value=\"warn\"/>\n" +
            "    <appender-ref ref=\"rolling_file_json\"/>\n" +
            "  </category>");

    Map<String, String> expectedLogSearchLog4j = new HashMap<>();
    expectedLogSearchLog4j.put("content",
        "  <appender name=\"rolling_file\" class=\"org.apache.log4j.RollingFileAppender\">\n" +
            "    <param name=\"file\" value=\"{{logsearch_log_dir}}/logsearch.log\" />\n" +
            "    <param name=\"Threshold\" value=\"info\" />\n" +
            "    <param name=\"append\" value=\"true\" />\n" +
            "    <param name=\"maxFileSize\" value=\"{{logsearch_log_maxfilesize}}MB\" />\n" +
            "    <param name=\"maxBackupIndex\" value=\"{{logsearch_log_maxbackupindex}}\" />\n" +
            "    <layout class=\"org.apache.log4j.PatternLayout\">\n" +
            "      <param name=\"ConversionPattern\" value=\"%d [%t] %-5p %C{6} (%F:%L) - %m%n\" />\n" +
            "    </layout>\n" +
            "  </appender>\n" +
            "\n" +
            "  <appender name=\"rolling_file_json\" class=\"org.apache.ambari.logsearch.appender.LogsearchRollingFileAppender\">\n" +
            "    <param name=\"file\" value=\"{{logsearch_log_dir}}/logsearch.json\"/>\n" +
            "    <param name=\"append\" value=\"true\"/>\n" +
            "    <param name=\"maxFileSize\" value=\"{{logsearch_json_log_maxfilesize}}MB\"/>\n" +
            "    <param name=\"maxBackupIndex\" value=\"{{logsearch_json_log_maxbackupindex}}\"/>\n" +
            "    <layout class=\"org.apache.ambari.logsearch.appender.LogsearchConversion\"/>\n" +
            "  </appender>\n" +
            "\n" +
            "  <appender name=\"audit_rolling_file_json\" class=\"org.apache.ambari.logsearch.appender.LogsearchRollingFileAppender\">\n" +
            "    <param name=\"file\" value=\"{{logsearch_log_dir}}/logsearch-audit.json\"/>\n" +
            "    <param name=\"append\" value=\"true\"/>\n" +
            "    <param name=\"maxFileSize\" value=\"{{logsearch_audit_log_maxfilesize}}MB\"/>\n" +
            "    <param name=\"maxBackupIndex\" value=\"{{logsearch_audit_log_maxbackupindex}}\"/>\n" +
            "    <layout class=\"org.apache.ambari.logsearch.appender.LogsearchConversion\"/>\n" +
            "  </appender>\n" +
            "\n" +
            "  <appender name=\"performance_analyzer_json\" class=\"org.apache.ambari.logsearch.appender.LogsearchRollingFileAppender\">\n" +
            "    <param name=\"file\" value=\"{{logsearch_log_dir}}/logsearch-performance.json\"/>\n" +
            "    <param name=\"Threshold\" value=\"info\"/>\n" +
            "    <param name=\"append\" value=\"true\"/>\n" +
            "    <param name=\"maxFileSize\" value=\"{{logsearch_perf_log_maxfilesize}}MB\"/>\n" +
            "    <param name=\"maxBackupIndex\" value=\"{{logsearch_perf_log_maxbackupindex}}\"/>\n" +
            "    <layout class=\"org.apache.ambari.logsearch.appender.LogsearchConversion\"/>\n" +
            "  </appender>\n" +
            "\n" +
            "  <logger name=\"org.apache.ambari.logsearch.audit\" additivity=\"true\">\n" +
            "     <appender-ref ref=\"audit_rolling_file_json\"/>\n" +
            "  </logger>\n" +
            "\n" +
            "  <logger name=\"org.apache.ambari.logsearch.performance\" additivity=\"false\">\n" +
            "    <appender-ref ref=\"performance_analyzer_json\"/>\n" +
            "  </logger>\n" +
            "\n" +
            "  <category name=\"org.apache.ambari.logsearch\" additivity=\"false\">\n" +
            "    <priority value=\"info\"/>\n" +
            "    <appender-ref ref=\"rolling_file_json\"/>\n" +
            "  </category>");

    expectedLogSearchLog4j.put("logsearch_log_maxfilesize", "11");
    expectedLogSearchLog4j.put("logsearch_log_maxbackupindex", "12");
    expectedLogSearchLog4j.put("logsearch_json_log_maxfilesize", "13");
    expectedLogSearchLog4j.put("logsearch_json_log_maxbackupindex", "14");
    expectedLogSearchLog4j.put("logsearch_audit_log_maxfilesize", "15");
    expectedLogSearchLog4j.put("logsearch_audit_log_maxbackupindex", "16");
    expectedLogSearchLog4j.put("logsearch_perf_log_maxfilesize", "17");
    expectedLogSearchLog4j.put("logsearch_perf_log_maxbackupindex", "18");

    Config mockLogSearchLog4j = easyMockSupport.createNiceMock(Config.class);
    expect(cluster.getDesiredConfigByType("logsearch-log4j")).andReturn(mockLogSearchLog4j).atLeastOnce();
    expect(mockLogSearchLog4j.getProperties()).andReturn(oldLogSearchLog4j).anyTimes();
    Capture<Map<String, String>> logSearchLog4jCapture = EasyMock.newCapture();
    expect(controller.createConfig(anyObject(Cluster.class), anyObject(StackId.class), anyString(), capture(logSearchLog4jCapture), anyString(),
        EasyMock.<Map<String, Map<String, String>>>anyObject())).andReturn(config).once();

    replay(clusters, cluster);
    replay(controller, injector2);
    replay(mockLogSearchProperties, mockLogFeederEnv, mockLogSearchEnv, mockLogFeederLog4j, mockLogSearchLog4j);
    new UpgradeCatalog250(injector2).updateLogSearchConfigs();
    easyMockSupport.verifyAll();

    Map<String, String> updatedLogSearchProperties = logSearchPropertiesCapture.getValue();
    assertTrue(Maps.difference(expectedLogSearchProperties, updatedLogSearchProperties).areEqual());

    Map<String, String> updatedLogFeederEnv = logFeederEnvCapture.getValue();
    assertTrue(Maps.difference(expectedLogFeederEnv, updatedLogFeederEnv).areEqual());

    Map<String, String> updatedLogSearchEnv = logSearchEnvCapture.getValue();
    assertTrue(Maps.difference(expectedLogSearchEnv, updatedLogSearchEnv).areEqual());

    Map<String, String> updatedLogFeederLog4j = logFeederLog4jCapture.getValue();
    assertTrue(Maps.difference(expectedLogFeederLog4j, updatedLogFeederLog4j).areEqual());

    Map<String, String> updatedLogSearchLog4j = logSearchLog4jCapture.getValue();
    assertTrue(Maps.difference(expectedLogSearchLog4j, updatedLogSearchLog4j).areEqual());
  }

  @Test
  public void testAmbariInfraUpdateConfigs() throws Exception {
    reset(clusters, cluster);
    expect(clusters.getClusters()).andReturn(ImmutableMap.of("normal", cluster)).once();

    EasyMockSupport easyMockSupport = new EasyMockSupport();

    Injector injector2 = easyMockSupport.createNiceMock(Injector.class);
    AmbariManagementControllerImpl controller = createMockBuilder(AmbariManagementControllerImpl.class)
        .addMockedMethod("createConfiguration")
        .addMockedMethod("getClusters", new Class[]{})
        .addMockedMethod("createConfig")
        .withConstructor(actionManager, clusters, injector)
        .createNiceMock();

    expect(injector2.getInstance(AmbariManagementController.class)).andReturn(controller).anyTimes();
    expect(controller.getClusters()).andReturn(clusters).anyTimes();

    Map<String, String> oldInfraSolrEnv = ImmutableMap.of(
        "content", "SOLR_SSL_TRUST_STORE={{infra_solr_keystore_location}}\n" +
            "SOLR_SSL_TRUST_STORE_PASSWORD={{infra_solr_keystore_password}}\n" +
            "SOLR_KERB_NAME_RULES={{infra_solr_kerberos_name_rules}}\n" +
            "SOLR_AUTHENTICATION_OPTS=\" -DauthenticationPlugin=org.apache.solr.security.KerberosPlugin -Djava.security.auth.login.config=$SOLR_JAAS_FILE -Dsolr.kerberos.principal=${SOLR_KERB_PRINCIPAL} -Dsolr.kerberos.keytab=${SOLR_KERB_KEYTAB} -Dsolr.kerberos.cookie.domain=${SOLR_HOST} -Dsolr.kerberos.name.rules=${SOLR_KERB_NAME_RULES}\"");

    Map<String, String> expectedInfraSolrEnv = ImmutableMap.of(
        "content", "SOLR_SSL_TRUST_STORE={{infra_solr_truststore_location}}\n" +
            "SOLR_SSL_TRUST_STORE_PASSWORD={{infra_solr_truststore_password}}\n" +
            "SOLR_KERB_NAME_RULES=\"{{infra_solr_kerberos_name_rules}}\"\n" +
            "SOLR_AUTHENTICATION_OPTS=\" -DauthenticationPlugin=org.apache.solr.security.KerberosPlugin -Djava.security.auth.login.config=$SOLR_JAAS_FILE -Dsolr.kerberos.principal=${SOLR_KERB_PRINCIPAL} -Dsolr.kerberos.keytab=${SOLR_KERB_KEYTAB} -Dsolr.kerberos.cookie.domain=${SOLR_HOST}\"");

    Config mockInfraSolrEnv = easyMockSupport.createNiceMock(Config.class);
    expect(cluster.getDesiredConfigByType("infra-solr-env")).andReturn(mockInfraSolrEnv).atLeastOnce();
    expect(mockInfraSolrEnv.getProperties()).andReturn(oldInfraSolrEnv).anyTimes();
    Capture<Map<String, String>> infraSolrEnvCapture = EasyMock.newCapture();
    expect(controller.createConfig(anyObject(Cluster.class), anyObject(StackId.class),  anyString(), capture(infraSolrEnvCapture), anyString(),
        EasyMock.<Map<String, Map<String, String>>>anyObject())).andReturn(config).once();

    Map<String, String> oldInfraSolrLog4j = ImmutableMap.of(
        "content", "log4j.appender.file.MaxFileSize=15MB\n" +
            "log4j.appender.file.MaxBackupIndex=5\n");

    Map<String, String> expectedInfraSolrLog4j = ImmutableMap.of(
        "content", "log4j.appender.file.MaxFileSize={{infra_log_maxfilesize}}MB\n" +
            "log4j.appender.file.MaxBackupIndex={{infra_log_maxbackupindex}}\n",
        "infra_log_maxfilesize", "15",
        "infra_log_maxbackupindex", "5");

    Config mockInfraSolrLog4j = easyMockSupport.createNiceMock(Config.class);
    expect(cluster.getDesiredConfigByType("infra-solr-log4j")).andReturn(mockInfraSolrLog4j).atLeastOnce();
    expect(mockInfraSolrLog4j.getProperties()).andReturn(oldInfraSolrLog4j).anyTimes();
    Capture<Map<String, String>> infraSolrLog4jCapture = EasyMock.newCapture();
    expect(controller.createConfig(anyObject(Cluster.class), anyObject(StackId.class), anyString(), capture(infraSolrLog4jCapture), anyString(),
        EasyMock.<Map<String, Map<String, String>>>anyObject())).andReturn(config).once();

    Map<String, String> oldInfraSolrClientLog4j = ImmutableMap.of(
        "content", "log4j.appender.file.File\u003d{{infra_client_log|default(\u0027/var/log/ambari-infra-solr-client/solr-client.log\u0027)}}\n" +
            "log4j.appender.file.MaxFileSize=55MB\n" +
            "log4j.appender.file.MaxBackupIndex=10\n");

    Map<String, String> expectedInfraSolrClientLog4j = ImmutableMap.of(
        "content", "log4j.appender.file.File\u003d{{solr_client_log|default(\u0027/var/log/ambari-infra-solr-client/solr-client.log\u0027)}}\n" +
            "log4j.appender.file.MaxFileSize={{solr_client_log_maxfilesize}}MB\n" +
            "log4j.appender.file.MaxBackupIndex={{solr_client_log_maxbackupindex}}\n",
        "infra_client_log_maxfilesize", "55",
        "infra_client_log_maxbackupindex", "10");

    Config mockInfraSolrClientLog4j = easyMockSupport.createNiceMock(Config.class);
    expect(cluster.getDesiredConfigByType("infra-solr-client-log4j")).andReturn(mockInfraSolrClientLog4j).atLeastOnce();
    expect(mockInfraSolrClientLog4j.getProperties()).andReturn(oldInfraSolrClientLog4j).anyTimes();
    Capture<Map<String, String>> infraSolrClientLog4jCapture = EasyMock.newCapture();
    expect(controller.createConfig(anyObject(Cluster.class), anyObject(StackId.class), anyString(), capture(infraSolrClientLog4jCapture), anyString(),
        EasyMock.<Map<String, Map<String, String>>>anyObject())).andReturn(config).once();

    replay(clusters, cluster);
    replay(controller, injector2);
    replay(mockInfraSolrEnv, mockInfraSolrLog4j, mockInfraSolrClientLog4j);
    new UpgradeCatalog250(injector2).updateAmbariInfraConfigs();
    easyMockSupport.verifyAll();

    Map<String, String> updatedInfraSolrEnv = infraSolrEnvCapture.getValue();
    assertTrue(Maps.difference(expectedInfraSolrEnv, updatedInfraSolrEnv).areEqual());

    Map<String, String> updatedInfraSolrLog4j = infraSolrLog4jCapture.getValue();
    assertTrue(Maps.difference(expectedInfraSolrLog4j, updatedInfraSolrLog4j).areEqual());

    Map<String, String> updatedInfraSolrClientLog4j = infraSolrClientLog4jCapture.getValue();
    assertTrue(Maps.difference(expectedInfraSolrClientLog4j, updatedInfraSolrClientLog4j).areEqual());
  }

  @Test
  public void testUpdateHiveConfigs() throws Exception {
    reset(clusters, cluster);
    expect(clusters.getClusters()).andReturn(ImmutableMap.of("normal", cluster)).anyTimes();

    EasyMockSupport easyMockSupport = new EasyMockSupport();

    Injector injector2 = easyMockSupport.createNiceMock(Injector.class);
    AmbariManagementControllerImpl controller = createMockBuilder(AmbariManagementControllerImpl.class)
        .addMockedMethod("createConfiguration")
        .addMockedMethod("getClusters", new Class[]{})
        .addMockedMethod("createConfig")
        .withConstructor(actionManager, clusters, injector)
        .createNiceMock();

    expect(injector2.getInstance(AmbariManagementController.class)).andReturn(controller).anyTimes();
    expect(controller.getClusters()).andReturn(clusters).anyTimes();

    Map<String, String> oldHsiEnv = ImmutableMap.of(
        "llap_app_name", "llap0");

    Map<String, String> expectedHsiEnv = ImmutableMap.of(
        "llap_app_name", "llap0",
        "hive_heapsize", "1082");

    Map<String, String> oldHiveEnv = ImmutableMap.of(
        "hive.client.heapsize", "1024",
        "hive.heapsize", "1082",
        "hive.metastore.heapsize", "512",
        "hive_ambari_database", "MySQL");


    Config mockHiveEnv = easyMockSupport.createNiceMock(Config.class);
    expect(cluster.getDesiredConfigByType("hive-env")).andReturn(mockHiveEnv).atLeastOnce();
    expect(mockHiveEnv.getProperties()).andReturn(oldHiveEnv).anyTimes();

    Config mockHsiEnv = easyMockSupport.createNiceMock(Config.class);
    expect(cluster.getDesiredConfigByType("hive-interactive-env")).andReturn(mockHsiEnv).atLeastOnce();
    expect(mockHsiEnv.getProperties()).andReturn(oldHsiEnv).anyTimes();
    Capture<Map<String, String>> hsiEnvCapture = EasyMock.newCapture();
    expect(controller.createConfig(anyObject(Cluster.class), anyObject(StackId.class), anyString(), capture(hsiEnvCapture), anyString(),
        EasyMock.<Map<String, Map<String, String>>>anyObject())).andReturn(config).once();

    replay(clusters, cluster);
    replay(controller, injector2);
    replay(mockHsiEnv, mockHiveEnv);
    new UpgradeCatalog250(injector2).updateHIVEInteractiveConfigs();
    easyMockSupport.verifyAll();

    Map<String, String> updatedHsiEnv = hsiEnvCapture.getValue();
    assertTrue(Maps.difference(expectedHsiEnv, updatedHsiEnv).areEqual());
  }

  @Test
  public void testUpdateAtlasConfigs() throws Exception {

    Map<String, String> oldHiveProperties = new HashMap<>();
    Map<String, String> newHiveProperties = new HashMap<>();

    oldHiveProperties.put("hive.atlas.hook", "false");
    newHiveProperties.put("hive.atlas.hook", "true");
    testUpdateAtlasHookConfig(oldHiveProperties, newHiveProperties, "hive-env");

    Map<String, String> oldStormProperties = new HashMap<>();
    Map<String, String> newStormProperties = new HashMap<>();
    oldStormProperties.put("storm.atlas.hook", "false");
    newStormProperties.put("storm.atlas.hook", "true");
    testUpdateAtlasHookConfig(oldStormProperties, newStormProperties, "storm-env");

    Map<String, String> oldFalconProperties = new HashMap<>();
    Map<String, String> newFalconProperties = new HashMap<>();
    oldFalconProperties.put("falcon.atlas.hook", "false");
    newFalconProperties.put("falcon.atlas.hook", "true");
    testUpdateAtlasHookConfig(oldFalconProperties, newFalconProperties, "falcon-env");

    Map<String, String> oldSqoopProperties = new HashMap<>();
    Map<String, String> newSqoopProperties = new HashMap<>();
    oldSqoopProperties.put("sqoop.atlas.hook", "false");
    newSqoopProperties.put("sqoop.atlas.hook", "true");
    testUpdateAtlasHookConfig(oldSqoopProperties, newSqoopProperties, "sqoop-env");
  }

  public void testUpdateAtlasHookConfig(Map<String, String> oldProperties, Map<String, String> newProperties, String configType) throws Exception {

    Map<String, Service> installedServices = new HashMap<String, Service>() {
      {
        put("ATLAS", null);
        put("HIVE", null);
        put("STORM", null);
        put("FALCON", null);
        put("SQOOP", null);
      }
    };

    EasyMockSupport easyMockSupport = new EasyMockSupport();

    reset(clusters, cluster);
    expect(clusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", cluster);
    }}).once();
    expect(cluster.getClusterName()).andReturn("cl1").once();
    expect(cluster.getServices()).andReturn(installedServices).atLeastOnce();

    Config mockAtlasConfig = easyMockSupport.createNiceMock(Config.class);
    expect(cluster.getDesiredConfigByType(configType)).andReturn(mockAtlasConfig).atLeastOnce();
    expect(mockAtlasConfig.getProperties()).andReturn(oldProperties).anyTimes();

    replay(clusters, mockAtlasConfig, cluster);

    AmbariManagementControllerImpl controller = createMockBuilder(AmbariManagementControllerImpl.class)
        .addMockedMethod("createConfiguration")
        .addMockedMethod("getClusters", new Class[]{})
        .addMockedMethod("createConfig")
        .withConstructor(actionManager, clusters, injector)
        .createNiceMock();

    Injector injector2 = easyMockSupport.createNiceMock(Injector.class);
    Capture<Map<String, String>> propertiesCapture = EasyMock.newCapture();

    expect(injector2.getInstance(AmbariManagementController.class)).andReturn(controller).anyTimes();
    expect(controller.getClusters()).andReturn(clusters).anyTimes();
    expect(controller.createConfig(anyObject(Cluster.class), anyObject(StackId.class), anyString(), capture(propertiesCapture), anyString(),
        EasyMock.<Map<String, Map<String, String>>>anyObject())).andReturn(config).once();

    replay(controller, injector2);
    new UpgradeCatalog250(injector2).updateAtlasConfigs();
    easyMockSupport.verifyAll();

    Map<String, String> updatedProperties = propertiesCapture.getValue();
    assertTrue(Maps.difference(newProperties, updatedProperties).areEqual());
  }

  @Test
  public void testUpdateKerberosDescriptorArtifact() throws Exception {
    final String propertyToRemove = "yarn.nodemanager.linux-container-executor.cgroups.mount-path";
    final KerberosDescriptorFactory kerberosDescriptorFactory = new KerberosDescriptorFactory();

    KerberosServiceDescriptor serviceDescriptor;

    URL systemResourceURL = ClassLoader.getSystemResource("kerberos/test_kerberos_descriptor_2_5_infra_solr.json");
    Assert.assertNotNull(systemResourceURL);

    final KerberosDescriptor kerberosDescriptorOrig = kerberosDescriptorFactory.createInstance(new File(systemResourceURL.getFile()));

    serviceDescriptor = kerberosDescriptorOrig.getService("LOGSEARCH");
    Assert.assertNotNull(serviceDescriptor);
    Assert.assertNotNull(serviceDescriptor.getComponent("LOGSEARCH_SERVER"));
    Assert.assertNotNull(serviceDescriptor.getComponent("LOGSEARCH_SERVER").getIdentity("logsearch"));
    Assert.assertNotNull(serviceDescriptor.getComponent("LOGSEARCH_SERVER").getIdentity("/AMBARI_INFRA/INFRA_SOLR/infra-solr"));

    serviceDescriptor = kerberosDescriptorOrig.getService("ATLAS");
    Assert.assertNotNull(serviceDescriptor);
    Assert.assertNotNull(serviceDescriptor.getComponent("ATLAS_SERVER"));

    serviceDescriptor = kerberosDescriptorOrig.getService("RANGER");
    Assert.assertNotNull(serviceDescriptor);
    Assert.assertNotNull(serviceDescriptor.getComponent("RANGER_ADMIN"));

    serviceDescriptor = kerberosDescriptorOrig.getService("STORM");
    Assert.assertNotNull(serviceDescriptor);
    Assert.assertNotNull(serviceDescriptor.getComponent("NIMBUS"));

    UpgradeCatalog250 upgradeMock = createMockBuilder(UpgradeCatalog250.class).withConstructor(injector).createMock();

    ArtifactEntity artifactEntity = createNiceMock(ArtifactEntity.class);
    expect(artifactEntity.getArtifactData())
        .andReturn(kerberosDescriptorOrig.toMap())
        .once();

    Capture<Map<String, Object>> updateData = Capture.newInstance(CaptureType.ALL);
    artifactEntity.setArtifactData(capture(updateData));
    expectLastCall().times(1);

    ArtifactDAO artifactDAO = createNiceMock(ArtifactDAO.class);
    expect(artifactDAO.merge(anyObject(ArtifactEntity.class))).andReturn(artifactEntity).times(1);

    replay(artifactEntity, artifactDAO, upgradeMock);
    upgradeMock.updateKerberosDescriptorArtifact(artifactDAO, artifactEntity);
    verify(artifactEntity, artifactDAO, upgradeMock);

    KerberosDescriptor kerberosDescriptorUpdated = new KerberosDescriptorFactory().createInstance(updateData.getValue());

    getIdentity(kerberosDescriptorUpdated,null, null, "spnego");
    getIdentity(kerberosDescriptorUpdated,"LOGSEARCH", "LOGSEARCH_SERVER", "/AMBARI_INFRA/INFRA_SOLR/infra-solr");
    getIdentity(kerberosDescriptorUpdated,"ATLAS", "ATLAS_SERVER", "/AMBARI_INFRA/INFRA_SOLR/infra-solr");
    getIdentity(kerberosDescriptorUpdated,"RANGER", "RANGER_ADMIN", "/AMBARI_INFRA/INFRA_SOLR/infra-solr");
    getIdentity(kerberosDescriptorUpdated,"STORM", "NIMBUS", "/STORM/storm_components");

    Assert.assertFalse(kerberosDescriptorUpdated.getService("YARN").getConfigurations().get("yarn-site").getProperties().containsKey(propertyToRemove));

    KerberosIdentityDescriptor rangerHbaseAuditIdentityDescriptor = getIdentity(kerberosDescriptorUpdated,"HBASE", "HBASE_MASTER", "ranger_hbase_audit");

    KerberosPrincipalDescriptor rangerHbaseAuditPrincipalDescriptor = rangerHbaseAuditIdentityDescriptor.getPrincipalDescriptor();
    Assert.assertNotNull(rangerHbaseAuditPrincipalDescriptor);
    Assert.assertNull(rangerHbaseAuditPrincipalDescriptor.getValue());

    KerberosKeytabDescriptor rangerHbaseAuditKeytabDescriptor = rangerHbaseAuditIdentityDescriptor.getKeytabDescriptor();
    Assert.assertNotNull(rangerHbaseAuditKeytabDescriptor);
    Assert.assertNull(rangerHbaseAuditKeytabDescriptor.getFile());
  }

  @Test
  public void testCreateRoleAuthorizations() throws AmbariException, SQLException {

    EasyMockSupport easyMockSupport = new EasyMockSupport();

    ResourceTypeEntity ambariResourceTypeEntity = easyMockSupport.createMock(ResourceTypeEntity.class);

    ResourceTypeEntity clusterResourceTypeEntity = easyMockSupport.createMock(ResourceTypeEntity.class);

    PermissionEntity clusterAdministratorPermissionEntity = new PermissionEntity();
    clusterAdministratorPermissionEntity.setId(1);
    PermissionEntity ambariAdministratorPermissionEntity = new PermissionEntity();
    ambariAdministratorPermissionEntity.setId(2);

    PermissionDAO permissionDAO = easyMockSupport.createMock(PermissionDAO.class);
    expect(permissionDAO.findPermissionByNameAndType("AMBARI.ADMINISTRATOR", ambariResourceTypeEntity))
        .andReturn(ambariAdministratorPermissionEntity).atLeastOnce();
    expect(permissionDAO.findPermissionByNameAndType("CLUSTER.ADMINISTRATOR", clusterResourceTypeEntity))
        .andReturn(clusterAdministratorPermissionEntity).atLeastOnce();
    expect(permissionDAO.merge(ambariAdministratorPermissionEntity))
        .andReturn(ambariAdministratorPermissionEntity).atLeastOnce();
    expect(permissionDAO.merge(clusterAdministratorPermissionEntity))
        .andReturn(clusterAdministratorPermissionEntity).atLeastOnce();

    ResourceTypeDAO resourceTypeDAO = easyMockSupport.createMock(ResourceTypeDAO.class);
    expect(resourceTypeDAO.findByName("AMBARI")).andReturn(ambariResourceTypeEntity).atLeastOnce();
    expect(resourceTypeDAO.findByName("CLUSTER")).andReturn(clusterResourceTypeEntity).atLeastOnce();

    RoleAuthorizationDAO roleAuthorizationDAO = easyMockSupport.createMock(RoleAuthorizationDAO.class);
    expect(roleAuthorizationDAO.findById("CLUSTER.RUN_CUSTOM_COMMAND")).andReturn(null).atLeastOnce();
    expect(roleAuthorizationDAO.findById("AMBARI.RUN_CUSTOM_COMMAND")).andReturn(null).atLeastOnce();

    Capture<RoleAuthorizationEntity> captureClusterRunCustomCommandEntity = newCapture();
    roleAuthorizationDAO.create(capture(captureClusterRunCustomCommandEntity));
    expectLastCall().once();

    Capture<RoleAuthorizationEntity> captureAmbariRunCustomCommandEntity = newCapture();
    roleAuthorizationDAO.create(capture(captureAmbariRunCustomCommandEntity));
    expectLastCall().once();

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

    Assert.assertEquals(2, ambariAdministratorPermissionEntity.getAuthorizations().size());
    Assert.assertTrue(ambariAdministratorPermissionEntity.getAuthorizations().contains(clusterRunCustomCommandEntity));
    Assert.assertTrue(ambariAdministratorPermissionEntity.getAuthorizations().contains(ambariRunCustomCommandEntity));

    Assert.assertEquals(1, clusterAdministratorPermissionEntity.getAuthorizations().size());
    Assert.assertTrue(clusterAdministratorPermissionEntity.getAuthorizations().contains(clusterRunCustomCommandEntity));
  }


  @Test
  public void testAddingRoleAuthorizationIsIdempotent() throws Exception {
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    ResourceTypeEntity ambariResourceTypeEntity = new ResourceTypeEntity();
    PermissionEntity ambariAdministratorPermissionEntity = new PermissionEntity();

    final PermissionDAO permissionDAO = easyMockSupport.createNiceMock(PermissionDAO.class);
    expect(permissionDAO.findPermissionByNameAndType("AMBARI.ADMINISTRATOR", ambariResourceTypeEntity))
        .andReturn(ambariAdministratorPermissionEntity)
        .anyTimes();

    final ResourceTypeDAO resourceTypeDAO = easyMockSupport.createNiceMock(ResourceTypeDAO.class);
    expect(resourceTypeDAO.findByName("AMBARI")).andReturn(ambariResourceTypeEntity).anyTimes();

    final RoleAuthorizationDAO roleAuthorizationDAO = easyMockSupport.createNiceMock(RoleAuthorizationDAO.class);
    expect(roleAuthorizationDAO.findById("CLUSTER.RUN_CUSTOM_COMMAND")).andReturn(null).anyTimes();

    Capture<RoleAuthorizationEntity> captureAmbariRunCustomCommandEntity = newCapture();
    roleAuthorizationDAO.create(capture(captureAmbariRunCustomCommandEntity));
    expectLastCall().times(2);

    Injector injector = easyMockSupport.createNiceMock(Injector.class);
    expect(injector.getInstance(RoleAuthorizationDAO.class)).andReturn(roleAuthorizationDAO).atLeastOnce();
    expect(injector.getInstance(PermissionDAO.class)).andReturn(permissionDAO).atLeastOnce();
    expect(injector.getInstance(ResourceTypeDAO.class)).andReturn(resourceTypeDAO).atLeastOnce();

    easyMockSupport.replayAll();

    new UpgradeCatalog242(injector).createRoleAuthorizations();
    new UpgradeCatalog242(injector).createRoleAuthorizations();
    easyMockSupport.verifyAll();

    Assert.assertEquals(2, ambariAdministratorPermissionEntity.getAuthorizations().size());

  }

  @Test(expected = AmbariException.class)
  public void shouldThrowExceptionWhenOldTezViewUrlIsInvalid() throws Exception {
    upgradeCatalog250.getUpdatedTezHistoryUrlBase("Invalid URL");
  }

  @Test
  public void shouldCreateRightTezAutoUrl() throws Exception {
    String newUrl = upgradeCatalog250.getUpdatedTezHistoryUrlBase("http://hostname:8080/#/main/views/TEZ/0.7.0.2.6.0.0-561/tez1");
    Assert.assertEquals("incorrect tez view url create.", "http://hostname:8080/#/main/view/TEZ/tez_cluster_instance", newUrl);
  }

  @Test
  public void testUpdateRangerUrlConfigs() throws Exception {
    Map<String, String> oldHdfsProperties = new HashMap<>();
    Map<String, String> newHdfsProperties = new HashMap<>();
    oldHdfsProperties.put("ranger.plugin.hdfs.policy.rest.url", "{{policymgr_mgr_url}}");
    newHdfsProperties.put("ranger.plugin.hdfs.policy.rest.url", "http://localhost:6080");
    testUpdateRangerUrl(oldHdfsProperties, newHdfsProperties, "ranger-hdfs-security");

    Map<String, String> oldHiveProperties = new HashMap<>();
    Map<String, String> newHiveProperties = new HashMap<>();
    oldHiveProperties.put("ranger.plugin.hive.policy.rest.url", "{{policymgr_mgr_url}}");
    newHiveProperties.put("ranger.plugin.hive.policy.rest.url", "http://localhost:6080");
    testUpdateRangerUrl(oldHiveProperties, newHiveProperties, "ranger-hive-security");

    Map<String, String> oldHbaseProperties = new HashMap<>();
    Map<String, String> newHbaseProperties = new HashMap<>();
    oldHbaseProperties.put("ranger.plugin.hbase.policy.rest.url", "{{policymgr_mgr_url}}");
    newHbaseProperties.put("ranger.plugin.hbase.policy.rest.url", "http://localhost:6080");
    testUpdateRangerUrl(oldHbaseProperties, newHbaseProperties, "ranger-hbase-security");

    Map<String, String> oldKnoxProperties = new HashMap<>();
    Map<String, String> newKnoxProperties = new HashMap<>();
    oldKnoxProperties.put("ranger.plugin.knox.policy.rest.url", "{{policymgr_mgr_url}}");
    newKnoxProperties.put("ranger.plugin.knox.policy.rest.url", "http://localhost:6080");
    testUpdateRangerUrl(oldKnoxProperties, newKnoxProperties, "ranger-knox-security");

    Map<String, String> oldStormProperties = new HashMap<>();
    Map<String, String> newStormProperties = new HashMap<>();
    oldStormProperties.put("ranger.plugin.storm.policy.rest.url", "{{policymgr_mgr_url}}");
    newStormProperties.put("ranger.plugin.storm.policy.rest.url", "http://localhost:6080");
    testUpdateRangerUrl(oldStormProperties, newStormProperties, "ranger-storm-security");

    Map<String, String> oldYarnProperties = new HashMap<>();
    Map<String, String> newYarnProperties = new HashMap<>();
    oldYarnProperties.put("ranger.plugin.yarn.policy.rest.url", "{{policymgr_mgr_url}}");
    newYarnProperties.put("ranger.plugin.yarn.policy.rest.url", "http://localhost:6080");
    testUpdateRangerUrl(oldYarnProperties, newYarnProperties, "ranger-yarn-security");

    Map<String, String> oldKafkaProperties = new HashMap<>();
    Map<String, String> newKafkaProperties = new HashMap<>();
    oldKafkaProperties.put("ranger.plugin.kafka.policy.rest.url", "{{policymgr_mgr_url}}");
    newKafkaProperties.put("ranger.plugin.kafka.policy.rest.url", "http://localhost:6080");
    testUpdateRangerUrl(oldKafkaProperties, newKafkaProperties, "ranger-kafka-security");

    Map<String, String> oldAtlasProperties = new HashMap<>();
    Map<String, String> newAtlasProperties = new HashMap<>();
    oldAtlasProperties.put("ranger.plugin.atlas.policy.rest.url", "{{policymgr_mgr_url}}");
    newAtlasProperties.put("ranger.plugin.atlas.policy.rest.url", "http://localhost:6080");
    testUpdateRangerUrl(oldAtlasProperties, newAtlasProperties, "ranger-atlas-security");

    Map<String, String> oldKmsProperties = new HashMap<>();
    Map<String, String> newKmsProperties = new HashMap<>();
    oldKmsProperties.put("ranger.plugin.kms.policy.rest.url", "{{policymgr_mgr_url}}");
    newKmsProperties.put("ranger.plugin.kms.policy.rest.url", "http://localhost:6080");
    testUpdateRangerUrl(oldKmsProperties, newKmsProperties, "ranger-kms-security");
  }

  public void testUpdateRangerUrl(Map<String, String> oldProperties, Map<String, String> newProperties, String configType) throws Exception {
    Map<String, String> adminProperties = new HashMap<String, String>() {
      {
        put("policymgr_external_url", "http://localhost:6080");
      }
    };

    EasyMockSupport easyMockSupport = new EasyMockSupport();

    reset(clusters, cluster);

    expect(clusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", cluster);
    }}).once();

    Config mockRangerPluginConfig = easyMockSupport.createNiceMock(Config.class);
    Config mockRangerAdminProperties = easyMockSupport.createNiceMock(Config.class);

    expect(cluster.getDesiredConfigByType("admin-properties")).andReturn(mockRangerAdminProperties).anyTimes();
    expect(mockRangerAdminProperties.getProperties()).andReturn(adminProperties).anyTimes();

    expect(cluster.getDesiredConfigByType(configType)).andReturn(mockRangerPluginConfig).anyTimes();
    expect(mockRangerPluginConfig.getProperties()).andReturn(oldProperties).anyTimes();

    replay(clusters, mockRangerPluginConfig, mockRangerAdminProperties, cluster);

    AmbariManagementControllerImpl controller = createMockBuilder(AmbariManagementControllerImpl.class)
        .addMockedMethod("createConfiguration")
        .addMockedMethod("getClusters", new Class[]{})
        .addMockedMethod("createConfig")
        .withConstructor(actionManager, clusters, injector)
        .createNiceMock();

    Injector injector2 = easyMockSupport.createNiceMock(Injector.class);
    Capture<Map<String, String>> propertiesCapture = EasyMock.newCapture();

    expect(injector2.getInstance(AmbariManagementController.class)).andReturn(controller).anyTimes();
    expect(controller.getClusters()).andReturn(clusters).anyTimes();
    expect(controller.createConfig(anyObject(Cluster.class), anyObject(StackId.class), anyString(), capture(propertiesCapture), anyString(),
        EasyMock.<Map<String, Map<String, String>>>anyObject())).andReturn(config).once();

    replay(controller, injector2);
    new UpgradeCatalog250(injector2).updateRangerUrlConfigs();
    easyMockSupport.verifyAll();

    Map<String, String> updatedProperties = propertiesCapture.getValue();
    assertTrue(Maps.difference(newProperties, updatedProperties).areEqual());
  }

  private Injector createInjector(final AmbariManagementController mockAmbariManagementController,
                                  final Clusters mockClusters,
                                  final AlertDefinitionDAO mockAlertDefinitionDAO) {
    return Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(AmbariManagementController.class).toInstance(mockAmbariManagementController);
        bind(Clusters.class).toInstance(mockClusters);
        bind(EntityManager.class).toInstance(entityManager);
        bind(AlertDefinitionDAO.class).toInstance(mockAlertDefinitionDAO);
        bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
      }
    });
  }

  private KerberosIdentityDescriptor getIdentity(KerberosDescriptor kerberosDescriptor, String serviceName, String componentName, String identityName) {
    KerberosIdentityDescriptor identityDescriptor = null;
    AbstractKerberosDescriptorContainer container = kerberosDescriptor;

    if(serviceName != null) {
      KerberosServiceDescriptor serviceDescriptor = kerberosDescriptor.getService(serviceName);
      Assert.assertNotNull(serviceDescriptor);
      container = serviceDescriptor;

      if(componentName != null) {
        KerberosComponentDescriptor componentDescriptor = serviceDescriptor.getComponent(componentName);
        Assert.assertNotNull(componentDescriptor);
        container = componentDescriptor;
      }
    }

    if(identityName != null) {
      identityDescriptor = container.getIdentity(identityName);
      Assert.assertNotNull(identityDescriptor);
    }

    return identityDescriptor;
  }
}
