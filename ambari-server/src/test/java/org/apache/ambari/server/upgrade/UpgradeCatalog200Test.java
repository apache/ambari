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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.DBAccessor.DBColumnInfo;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.ClusterServiceDAO;
import org.apache.ambari.server.orm.dao.HostComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.HostComponentStateDAO;
import org.apache.ambari.server.orm.dao.ServiceComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntityPK;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntityPK;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.OperatingSystemInfo;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.ambari.server.state.SecurityState;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.stack.OsFamily;
import org.easymock.Capture;
import org.easymock.EasyMockSupport;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.persist.PersistService;

/**
 * {@link UpgradeCatalog200} unit tests.
 */
public class UpgradeCatalog200Test {
  private final String CLUSTER_NAME = "c1";
  private final String HOST_NAME = "h1";

  private final StackId DESIRED_STACK = new StackId("HDP", "2.0.6");

  private Injector injector;
  private Provider<EntityManager> entityManagerProvider = createStrictMock(Provider.class);
  private EntityManager entityManager = createNiceMock(EntityManager.class);
  private UpgradeCatalogHelper upgradeCatalogHelper;

  @Before
  public void init() {
    reset(entityManagerProvider);
    expect(entityManagerProvider.get()).andReturn(entityManager).anyTimes();
    replay(entityManagerProvider);
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);

    upgradeCatalogHelper = injector.getInstance(UpgradeCatalogHelper.class);
  }

  @After
  public void tearDown() {
    injector.getInstance(PersistService.class).stop();
  }

  @Test
  public void testExecuteDDLUpdates() throws Exception {
    final DBAccessor dbAccessor = createNiceMock(DBAccessor.class);
    Configuration configuration = createNiceMock(Configuration.class);
    Connection connection = createNiceMock(Connection.class);
    Statement statement = createNiceMock(Statement.class);
    ResultSet resultSet = createNiceMock(ResultSet.class);

    expect(configuration.getDatabaseUrl()).andReturn(Configuration.JDBC_IN_MEMORY_URL).anyTimes();

    Capture<DBAccessor.DBColumnInfo> alertDefinitionIgnoreColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
    Capture<DBAccessor.DBColumnInfo> alertDefinitionDescriptionColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
    Capture<DBAccessor.DBColumnInfo> alertTargetGlobalColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
    Capture<DBAccessor.DBColumnInfo> hostComponentStateColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
    Capture<DBAccessor.DBColumnInfo> hostComponentVersionColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
    Capture<DBAccessor.DBColumnInfo> clustersSecurityTypeColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
    Capture<DBAccessor.DBColumnInfo> hostComponentStateSecurityStateColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
    Capture<DBAccessor.DBColumnInfo> hostComponentDesiredStateSecurityStateColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
    Capture<DBAccessor.DBColumnInfo> hostRoleCommandRetryColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
    Capture<DBAccessor.DBColumnInfo> stageSkippableColumnCapture = new Capture<DBAccessor.DBColumnInfo>();

    Capture<DBAccessor.DBColumnInfo> viewparameterLabelColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
    Capture<DBAccessor.DBColumnInfo> viewparameterPlaceholderColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
    Capture<DBAccessor.DBColumnInfo> viewparameterDefaultValueColumnCapture = new Capture<DBAccessor.DBColumnInfo>();

    Capture<DBAccessor.DBColumnInfo> serviceDesiredStateSecurityStateColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
    Capture<List<DBAccessor.DBColumnInfo>> clusterVersionCapture = new Capture<List<DBAccessor.DBColumnInfo>>();
    Capture<List<DBAccessor.DBColumnInfo>> hostVersionCapture = new Capture<List<DBAccessor.DBColumnInfo>>();
    Capture<DBAccessor.DBColumnInfo> valueColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
    Capture<DBAccessor.DBColumnInfo> dataValueColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
    Capture<List<DBAccessor.DBColumnInfo>> alertTargetStatesCapture = new Capture<List<DBAccessor.DBColumnInfo>>();
    Capture<List<DBAccessor.DBColumnInfo>> artifactCapture = new Capture<List<DBAccessor.DBColumnInfo>>();
    Capture<List<DBAccessor.DBColumnInfo>> kerberosPrincipalCapture = new Capture<List<DBAccessor.DBColumnInfo>>();
    Capture<List<DBAccessor.DBColumnInfo>> kerberosPrincipalHostCapture = new Capture<List<DBAccessor.DBColumnInfo>>();

    Capture<List<DBAccessor.DBColumnInfo>> upgradeCapture = new Capture<List<DBAccessor.DBColumnInfo>>();
    Capture<List<DBAccessor.DBColumnInfo>> upgradeGroupCapture = new Capture<List<DBAccessor.DBColumnInfo>>();
    Capture<List<DBAccessor.DBColumnInfo>> upgradeItemCapture = new Capture<List<DBAccessor.DBColumnInfo>>();

    // Alert Definition
    dbAccessor.addColumn(eq("alert_definition"),
                          capture(alertDefinitionIgnoreColumnCapture));

    dbAccessor.addColumn(eq("alert_definition"),
        capture(alertDefinitionDescriptionColumnCapture));

    dbAccessor.createTable(eq("alert_target_states"),
        capture(alertTargetStatesCapture));

    // alert target
    dbAccessor.addColumn(eq("alert_target"),
        capture(alertTargetGlobalColumnCapture));

    // Host Component State
    dbAccessor.addColumn(eq("hostcomponentstate"),
        capture(hostComponentStateColumnCapture));

    // Host Component Version
    dbAccessor.addColumn(eq("hostcomponentstate"),
        capture(hostComponentVersionColumnCapture));

    // Host Role Command retry allowed
    dbAccessor.addColumn(eq("host_role_command"),
        capture(hostRoleCommandRetryColumnCapture));

    // Stage skippable
    dbAccessor.addColumn(eq("stage"),
        capture(stageSkippableColumnCapture));

    // Clusters: security type
    dbAccessor.addColumn(eq("clusters"),
        capture(clustersSecurityTypeColumnCapture));

    // Host Component State: security State
    dbAccessor.addColumn(eq("hostcomponentstate"),
        capture(hostComponentStateSecurityStateColumnCapture));

    // Host Component Desired State: security State
    dbAccessor.addColumn(eq("hostcomponentdesiredstate"),
        capture(hostComponentDesiredStateSecurityStateColumnCapture));

    dbAccessor.addColumn(eq("viewparameter"), capture(viewparameterLabelColumnCapture));
    dbAccessor.addColumn(eq("viewparameter"), capture(viewparameterPlaceholderColumnCapture));
    dbAccessor.addColumn(eq("viewparameter"), capture(viewparameterDefaultValueColumnCapture));

    // Service Desired State: security State
    dbAccessor.addColumn(eq("servicedesiredstate"),
        capture(serviceDesiredStateSecurityStateColumnCapture));

    // Cluster Version
    dbAccessor.createTable(eq("cluster_version"),
        capture(clusterVersionCapture), eq("id"));

    // Host Version
    dbAccessor.createTable(eq("host_version"),
        capture(hostVersionCapture), eq("id"));

    // Upgrade
    dbAccessor.createTable(eq("upgrade"), capture(upgradeCapture), eq("upgrade_id"));

    // Upgrade Group item
    dbAccessor.createTable(eq("upgrade_group"), capture(upgradeGroupCapture), eq("upgrade_group_id"));

    // Upgrade item
    dbAccessor.createTable(eq("upgrade_item"), capture(upgradeItemCapture), eq("upgrade_item_id"));

    // artifact
    dbAccessor.createTable(eq("artifact"), capture(artifactCapture),
        eq("artifact_name"), eq("foreign_keys"));

    // kerberos_principal
    dbAccessor.createTable(eq("kerberos_principal"), capture(kerberosPrincipalCapture),
        eq("principal_name"));

    // kerberos_principal_host
    dbAccessor.createTable(eq("kerberos_principal_host"), capture(kerberosPrincipalHostCapture),
        eq("principal_name"), eq("host_name"));

    expect(dbAccessor.tableHasColumn("kerberos_principal_host", "host_name")).andReturn(true).atLeastOnce();

    dbAccessor.addFKConstraint(eq("kerberos_principal_host"), eq("FK_krb_pr_host_hostname"),
        eq("host_name"), eq("hosts"), eq("host_name"), eq(true), eq(false));

    dbAccessor.addFKConstraint(eq("kerberos_principal_host"), eq("FK_krb_pr_host_principalname"),
        eq("principal_name"), eq("kerberos_principal"), eq("principal_name"), eq(true), eq(false));

    setViewInstancePropertyExpectations(dbAccessor, valueColumnCapture);
    setViewInstanceDataExpectations(dbAccessor, dataValueColumnCapture);

    // AbstractUpgradeCatalog.addSequence()
    dbAccessor.getConnection();
    expectLastCall().andReturn(connection).anyTimes();
    connection.createStatement();
    expectLastCall().andReturn(statement).anyTimes();
    statement.executeQuery(anyObject(String.class));
    expectLastCall().andReturn(resultSet).anyTimes();

    replay(dbAccessor, configuration, resultSet, statement, connection);

    AbstractUpgradeCatalog upgradeCatalog = getUpgradeCatalog(dbAccessor);
    Class<?> c = AbstractUpgradeCatalog.class;
    Field f = c.getDeclaredField("configuration");
    f.setAccessible(true);
    f.set(upgradeCatalog, configuration);

    upgradeCatalog.executeDDLUpdates();
    verify(dbAccessor, configuration, resultSet, statement, connection);

    // verify columns for alert_definition
    verifyAlertDefinitionIgnoreColumn(alertDefinitionIgnoreColumnCapture);
    verifyAlertDefinitionDescriptionColumn(alertDefinitionDescriptionColumnCapture);

    // verify alert target column for is_global
    verifyAlertTargetGlobal(alertTargetGlobalColumnCapture);

    // verify new table for alert target states
    verifyAlertTargetStatesTable(alertTargetStatesCapture);

    // Verify added column in hostcomponentstate table
    DBAccessor.DBColumnInfo upgradeStateColumn = hostComponentStateColumnCapture.getValue();
    assertEquals("upgrade_state", upgradeStateColumn.getName());
    assertEquals(32, (int) upgradeStateColumn.getLength());
    assertEquals(String.class, upgradeStateColumn.getType());
    assertEquals("NONE", upgradeStateColumn.getDefaultValue());
    assertFalse(upgradeStateColumn.isNullable());

    // Verify added column in hostcomponentstate table
    DBAccessor.DBColumnInfo upgradeVersionColumn = hostComponentVersionColumnCapture.getValue();
    assertEquals("version", upgradeVersionColumn.getName());
    assertEquals(32, (int) upgradeVersionColumn.getLength());
    assertEquals(String.class, upgradeVersionColumn.getType());
    assertEquals("UNKNOWN", upgradeVersionColumn.getDefaultValue());
    assertFalse(upgradeVersionColumn.isNullable());

    // Verify added column in host_role_command table
    DBAccessor.DBColumnInfo upgradeRetryColumn = hostRoleCommandRetryColumnCapture.getValue();
    assertEquals("retry_allowed", upgradeRetryColumn.getName());
    assertEquals(1, (int) upgradeRetryColumn.getLength());
    assertEquals(Integer.class, upgradeRetryColumn.getType());
    assertEquals(0, upgradeRetryColumn.getDefaultValue());
    assertFalse(upgradeRetryColumn.isNullable());

    // Verify added column in host_role_command table
    DBAccessor.DBColumnInfo upgradeSkippableColumn = stageSkippableColumnCapture.getValue();
    assertEquals("skippable", upgradeSkippableColumn.getName());
    assertEquals(1, (int) upgradeSkippableColumn.getLength());
    assertEquals(Integer.class, upgradeSkippableColumn.getType());
    assertEquals(0, upgradeSkippableColumn.getDefaultValue());
    assertFalse(upgradeSkippableColumn.isNullable());

    // verify security_type column
    verifyClustersSecurityType(clustersSecurityTypeColumnCapture);

    // verify security_state columns
    verifyComponentSecurityStateColumn(hostComponentStateSecurityStateColumnCapture);
    verifyComponentSecurityStateColumn(hostComponentDesiredStateSecurityStateColumnCapture);
    verifyServiceSecurityStateColumn(serviceDesiredStateSecurityStateColumnCapture);

    verifyViewParameterColumns(viewparameterLabelColumnCapture, viewparameterPlaceholderColumnCapture,
        viewparameterDefaultValueColumnCapture);

    // verify artifact columns
    List<DBAccessor.DBColumnInfo> artifactColumns = artifactCapture.getValue();
    testCreateArtifactTable(artifactColumns);

    // verify kerberos_principal columns
    testCreateKerberosPrincipalTable(kerberosPrincipalCapture.getValue());

    // verify kerberos_principal_host columns
    testCreateKerberosPrincipalHostTable(kerberosPrincipalHostCapture.getValue());

    // Verify capture group sizes
    assertEquals(7, clusterVersionCapture.getValue().size());
    assertEquals(4, hostVersionCapture.getValue().size());

    assertViewInstancePropertyColumns(valueColumnCapture);
    assertViewInstanceDataColumns(dataValueColumnCapture);

    assertEquals(6, upgradeCapture.getValue().size());
    assertEquals(4, upgradeGroupCapture.getValue().size());
    assertEquals(7, upgradeItemCapture.getValue().size());
  }

  /**
   * Tests that each DML method is invoked.
   *
   * @throws Exception
   */
  @Test
  public void testExecuteDMLUpdates() throws Exception {
    Method removeNagiosService = UpgradeCatalog200.class.getDeclaredMethod("removeNagiosService");
    Method updateHiveDatabaseType = UpgradeCatalog200.class.getDeclaredMethod("updateHiveDatabaseType");
    Method addNewConfigurationsFromXml = AbstractUpgradeCatalog.class.getDeclaredMethod
        ("addNewConfigurationsFromXml");
    Method updateTezConfiguration = UpgradeCatalog200.class.getDeclaredMethod("updateTezConfiguration");
    Method updateFlumeEnvConfig = UpgradeCatalog200.class.getDeclaredMethod("updateFlumeEnvConfig");
    Method updateClusterEnvConfiguration = UpgradeCatalog200.class.getDeclaredMethod("updateClusterEnvConfiguration");
    Method updateConfigurationProperties = AbstractUpgradeCatalog.class.getDeclaredMethod
            ("updateConfigurationProperties", String.class, Map.class, boolean.class, boolean.class);
    Method persistHDPRepo = UpgradeCatalog200.class.getDeclaredMethod("persistHDPRepo");

    UpgradeCatalog200 upgradeCatalog = createMockBuilder(UpgradeCatalog200.class)
        .addMockedMethod(removeNagiosService)
        .addMockedMethod(updateHiveDatabaseType)
        .addMockedMethod(addNewConfigurationsFromXml)
        .addMockedMethod(updateTezConfiguration)
        .addMockedMethod(updateFlumeEnvConfig)
        .addMockedMethod(updateConfigurationProperties)
        .addMockedMethod(updateClusterEnvConfiguration)
        .addMockedMethod(persistHDPRepo)
        .createMock();

    upgradeCatalog.removeNagiosService();
    expectLastCall().once();
    upgradeCatalog.addNewConfigurationsFromXml();
    expectLastCall();

    upgradeCatalog.updateHiveDatabaseType();
    expectLastCall().once();

    upgradeCatalog.updateTezConfiguration();
    expectLastCall().once();

    upgradeCatalog.updateFlumeEnvConfig();
    expectLastCall().once();

    upgradeCatalog.updateConfigurationProperties("hive-site",
            Collections.singletonMap("hive.server2.transport.mode", "binary"), false, false);
    expectLastCall();

    upgradeCatalog.persistHDPRepo();
    expectLastCall().once();

    upgradeCatalog.updateClusterEnvConfiguration();
    expectLastCall();

    replay(upgradeCatalog);

    upgradeCatalog.executeDMLUpdates();

    verify(upgradeCatalog);
  }

  @Test
  public void testUpdateFlumeEnvConfig() throws AmbariException {
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariManagementController mockAmbariManagementController = easyMockSupport.createNiceMock(AmbariManagementController.class);
    final Clusters mockClusters = easyMockSupport.createStrictMock(Clusters.class);
    final Cluster mockClusterExpected = easyMockSupport.createNiceMock(Cluster.class);
    final Map<String, String> propertiesFlumeEnv = new HashMap<String, String>() {
      {
        put("content", "test");
      }
    };

    final Config mockFlumeEnv = easyMockSupport.createNiceMock(Config.class);
    expect(mockFlumeEnv.getProperties()).andReturn(propertiesFlumeEnv).once();

    final Injector mockInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(AmbariManagementController.class).toInstance(mockAmbariManagementController);
        bind(Clusters.class).toInstance(mockClusters);
        bind(EntityManager.class).toInstance(entityManager);

        bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
      }
    });

    expect(mockAmbariManagementController.getClusters()).andReturn(mockClusters).once();
    expect(mockClusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", mockClusterExpected);
    }}).atLeastOnce();

    expect(mockClusterExpected.getDesiredConfigByType("flume-env")).andReturn(mockFlumeEnv).atLeastOnce();
    expect(mockFlumeEnv.getProperties()).andReturn(propertiesFlumeEnv).atLeastOnce();

    easyMockSupport.replayAll();
    mockInjector.getInstance(UpgradeCatalog200.class).updateFlumeEnvConfig();
    easyMockSupport.verifyAll();
  }

  @Test
  public void testPersistHDPRepo() throws Exception {
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariManagementController  mockAmbariManagementController = easyMockSupport.createStrictMock(AmbariManagementController.class);
    final AmbariMetaInfo mockAmbariMetaInfo = easyMockSupport.createNiceMock(AmbariMetaInfo.class);
    final StackInfo mockStackInfo = easyMockSupport.createNiceMock(StackInfo.class);
    final Clusters mockClusters = easyMockSupport.createStrictMock(Clusters.class);
    final Cluster mockCluster = easyMockSupport.createStrictMock(Cluster.class);
    final Map<String, Cluster> clusterMap = new HashMap<String, Cluster>();
    clusterMap.put("c1",mockCluster);
    OperatingSystemInfo osi = new OperatingSystemInfo("redhat6");
    HashSet<OperatingSystemInfo> osiSet = new HashSet<OperatingSystemInfo>();
    osiSet.add(osi);
    StackId stackId = new StackId("HDP","2.2");
    final RepositoryInfo mockRepositoryInfo = easyMockSupport.createNiceMock(RepositoryInfo.class);

    final Injector mockInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(AmbariManagementController.class).toInstance(mockAmbariManagementController);
        bind(Clusters.class).toInstance(mockClusters);
        bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
        bind(EntityManager.class).toInstance(createNiceMock(EntityManager.class));
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
      }
    });

    expect(mockAmbariManagementController.getAmbariMetaInfo()).andReturn(mockAmbariMetaInfo);
    expect(mockAmbariManagementController.getClusters()).andReturn(mockClusters).once();
    expect(mockClusters.getClusters()).andReturn(clusterMap).once();
    expect(mockCluster.getCurrentStackVersion()).andReturn(stackId).once();
    expect(mockCluster.getClusterName()).andReturn("cc").anyTimes();
    expect(mockAmbariMetaInfo.getOperatingSystems("HDP", "2.2")).andReturn(osiSet).once();
    expect(mockAmbariMetaInfo.getRepository("HDP", "2.2", "redhat6", "HDP-2.2")).andReturn(mockRepositoryInfo).once();
    expect(mockAmbariMetaInfo.getStack("HDP", "2.2")).andReturn(mockStackInfo);
    expect(mockStackInfo.getRepositories()).andReturn(new ArrayList<RepositoryInfo>() {{
      add(mockRepositoryInfo);
    }});
    expect(mockRepositoryInfo.getDefaultBaseUrl()).andReturn("http://baseurl").once();
    mockAmbariMetaInfo.updateRepoBaseURL("HDP", "2.2", "redhat6", "HDP-2.2", "http://baseurl");
    expectLastCall().once();

    easyMockSupport.replayAll();
    mockInjector.getInstance(UpgradeCatalog200.class).persistHDPRepo();
    easyMockSupport.verifyAll();
  }

  @Test
  public void testRepositoryTable() {
    final RepositoryInfo repositoryInfo1 = new RepositoryInfo();
    repositoryInfo1.setOsType("redhat6");
    repositoryInfo1.setRepoId("HDP-2.2");
    repositoryInfo1.setBaseUrl("http://public-repo-1.hortonworks.com/HDP/centos6/2.x/updates/2.2.6.0");

    final RepositoryInfo repositoryInfo2 = new RepositoryInfo();
    repositoryInfo2.setOsType("suse11");
    repositoryInfo2.setRepoId("HDP-UTILS-1.1.0.20");
    repositoryInfo2.setBaseUrl("http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.20/repos/suse11sp3");

    List<RepositoryInfo> repos = new ArrayList<RepositoryInfo>() {{
      add(repositoryInfo1);
      add(repositoryInfo2);
    }};
    String output = UpgradeCatalog200.repositoryTable(repos);
    assertEquals("  redhat6 |            HDP-2.2 | http://public-repo-1.hortonworks.com/HDP/centos6/2.x/updates/2.2.6.0 \n" +
                 "   suse11 | HDP-UTILS-1.1.0.20 | http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.20/repos/suse11sp3 \n",
      output);
  }

  @Test
  public void testUpdateClusterEnvConfiguration() throws Exception {
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariManagementController  mockAmbariManagementController = easyMockSupport.createStrictMock(AmbariManagementController.class);
    final ConfigHelper mockConfigHelper = easyMockSupport.createMock(ConfigHelper.class);

    final Clusters mockClusters = easyMockSupport.createStrictMock(Clusters.class);
    final Cluster mockClusterExpected = easyMockSupport.createStrictMock(Cluster.class);
    final Cluster mockClusterMissingSmokeUser = easyMockSupport.createStrictMock(Cluster.class);
    final Cluster mockClusterMissingConfig = easyMockSupport.createStrictMock(Cluster.class);

    final Config mockClusterEnvExpected = easyMockSupport.createStrictMock(Config.class);
    final Config mockClusterEnvMissingSmokeUser = easyMockSupport.createStrictMock(Config.class);

    final Map<String, String> propertiesExpectedT0 = new HashMap<String, String>();
    propertiesExpectedT0.put("kerberos_domain", "EXAMPLE.COM");
    propertiesExpectedT0.put("user_group", "hadoop");
    propertiesExpectedT0.put("kinit_path_local", "/usr/bin");
    propertiesExpectedT0.put("security_enabled", "true");
    propertiesExpectedT0.put("smokeuser", "ambari-qa");
    propertiesExpectedT0.put("smokeuser_keytab", "/etc/security/keytabs/smokeuser.headless.keytab");
    propertiesExpectedT0.put("ignore_groupsusers_create", "false");

    final Map<String, String> propertiesExpectedT1 = new HashMap<String, String>(propertiesExpectedT0);
    propertiesExpectedT1.put("smokeuser_principal_name", "ambari-qa");

    final Map<String, String> propertiesMissingSmokeUserT0 = new HashMap<String, String>(propertiesExpectedT0);
    propertiesMissingSmokeUserT0.remove("smokeuser");

    final Map<String, String> propertiesMissingSmokeUserT1 = new HashMap<String, String>(propertiesMissingSmokeUserT0);
    propertiesMissingSmokeUserT1.put("smokeuser_principal_name", "ambari-qa");

    final PropertyInfo mockSmokeUserPropertyInfo = easyMockSupport.createStrictMock(PropertyInfo.class);

    final Injector mockInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(AmbariManagementController.class).toInstance(mockAmbariManagementController);
        bind(ConfigHelper.class).toInstance(mockConfigHelper);
        bind(Clusters.class).toInstance(mockClusters);

        bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
      }
    });

    expect(mockAmbariManagementController.getClusters()).andReturn(mockClusters).once();
    expect(mockClusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", mockClusterExpected);
      put("missing_smokeuser", mockClusterMissingSmokeUser);
      put("missing_cluster-env", mockClusterMissingConfig);

    }}).once();

      // Expected operation
    expect(mockClusterExpected.getDesiredConfigByType("cluster-env")).andReturn(mockClusterEnvExpected).once();
    expect(mockClusterEnvExpected.getProperties()).andReturn(propertiesExpectedT0).once();

    mockConfigHelper.createConfigType(mockClusterExpected, mockAmbariManagementController,
        "cluster-env", propertiesExpectedT1, UpgradeCatalog200.AUTHENTICATED_USER_NAME, "Upgrading to Ambari 2.0");
    expectLastCall().once();

    // Missing smokeuser
    expect(mockClusterMissingSmokeUser.getDesiredConfigByType("cluster-env")).andReturn(mockClusterEnvMissingSmokeUser).once();
    expect(mockClusterEnvMissingSmokeUser.getProperties()).andReturn(propertiesMissingSmokeUserT0).once();

    expect(mockConfigHelper.getStackProperties(mockClusterMissingSmokeUser)).andReturn(Collections.singleton(mockSmokeUserPropertyInfo)).once();

    expect(mockSmokeUserPropertyInfo.getFilename()).andReturn("cluster-env.xml").once();
    expect(mockSmokeUserPropertyInfo.getValue()).andReturn("ambari-qa").once();

    mockConfigHelper.createConfigType(mockClusterMissingSmokeUser, mockAmbariManagementController,
        "cluster-env", propertiesMissingSmokeUserT1, UpgradeCatalog200.AUTHENTICATED_USER_NAME, "Upgrading to Ambari 2.0");
    expectLastCall().once();

    // Missing cluster-env config
    expect(mockClusterMissingConfig.getDesiredConfigByType("cluster-env")).andReturn(null).once();

    easyMockSupport.replayAll();
    mockInjector.getInstance(UpgradeCatalog200.class).updateClusterEnvConfiguration();
    easyMockSupport.verifyAll();
  }

  /**
   * Tests that Nagios is correctly removed.
   *
   * @throws Exception
   */
  @Test
  public void testDeleteNagiosService() throws Exception {
    UpgradeCatalog200 upgradeCatalog200 = injector.getInstance(UpgradeCatalog200.class);
    ServiceComponentDesiredStateDAO serviceComponentDesiredStateDAO = injector.getInstance(ServiceComponentDesiredStateDAO.class);
    HostComponentDesiredStateDAO hostComponentDesiredStateDAO = injector.getInstance(HostComponentDesiredStateDAO.class);
    HostComponentStateDAO hostComponentStateDAO = injector.getInstance(HostComponentStateDAO.class);
    ClusterServiceDAO clusterServiceDao = injector.getInstance(ClusterServiceDAO.class);
    StackDAO stackDAO = injector.getInstance(StackDAO.class);

    // inject AmbariMetaInfo to ensure that stacks get populated in the DB
    injector.getInstance(AmbariMetaInfo.class);

    StackEntity stackEntity = stackDAO.find(DESIRED_STACK.getStackName(),
        DESIRED_STACK.getStackVersion());

    assertNotNull(stackEntity);

    final ClusterEntity clusterEntity = upgradeCatalogHelper.createCluster(
        injector, CLUSTER_NAME, stackEntity);

    final ClusterServiceEntity clusterServiceEntityNagios = upgradeCatalogHelper.addService(
        injector, clusterEntity, "NAGIOS", stackEntity);

    final HostEntity hostEntity = upgradeCatalogHelper.createHost(injector,
        clusterEntity, HOST_NAME);

    upgradeCatalogHelper.addComponent(injector, clusterEntity,
        clusterServiceEntityNagios, hostEntity, "NAGIOS_SERVER", stackEntity);

    ServiceComponentDesiredStateEntityPK pkNagiosServer = new ServiceComponentDesiredStateEntityPK();
    pkNagiosServer.setComponentName("NAGIOS_SERVER");
    pkNagiosServer.setClusterId(clusterEntity.getClusterId());
    pkNagiosServer.setServiceName("NAGIOS");
    ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity = serviceComponentDesiredStateDAO.findByPK(pkNagiosServer);
    assertNotNull(serviceComponentDesiredStateEntity);

    HostComponentDesiredStateEntityPK hcDesiredStateEntityPk = new HostComponentDesiredStateEntityPK();
    hcDesiredStateEntityPk.setServiceName("NAGIOS");
    hcDesiredStateEntityPk.setClusterId(clusterEntity.getClusterId());
    hcDesiredStateEntityPk.setComponentName("NAGIOS_SERVER");
    hcDesiredStateEntityPk.setHostId(hostEntity.getHostId());
    HostComponentDesiredStateEntity hcDesiredStateEntity = hostComponentDesiredStateDAO.findByPK(hcDesiredStateEntityPk);
    assertNotNull(hcDesiredStateEntity);

    HostComponentStateEntity hcStateEntity = hostComponentStateDAO.findByIndex(
        clusterEntity.getClusterId(), "NAGIOS", "NAGIOS_SERVER", hostEntity.getHostId());

    assertNotNull(hcStateEntity);

    ClusterServiceEntity clusterService = clusterServiceDao.findByClusterAndServiceNames(
        CLUSTER_NAME, "NAGIOS");

    upgradeCatalog200.removeNagiosService();

    clusterService = clusterServiceDao.findByClusterAndServiceNames(
        CLUSTER_NAME, "NAGIOS");

    assertNull(clusterService);
  }

  /**
   * @param dbAccessor
   * @return
   */
  private AbstractUpgradeCatalog getUpgradeCatalog(final DBAccessor dbAccessor) {
    Module module = new Module() {
      @Override
      public void configure(Binder binder) {
        binder.bind(DBAccessor.class).toInstance(dbAccessor);
        binder.bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
      }
    };

    Injector injector = Guice.createInjector(module);
    return injector.getInstance(UpgradeCatalog200.class);
  }

  /**
   * Verifies new ignore column for alert definition.
   *
   * @param alertDefinitionIgnoreColumnCapture
   */
  private void verifyAlertDefinitionIgnoreColumn(
      Capture<DBAccessor.DBColumnInfo> alertDefinitionIgnoreColumnCapture) {
    DBColumnInfo column = alertDefinitionIgnoreColumnCapture.getValue();
    Assert.assertEquals(Integer.valueOf(0), column.getDefaultValue());
    Assert.assertEquals(Integer.valueOf(1), column.getLength());
    Assert.assertEquals(Short.class, column.getType());
    Assert.assertEquals("ignore_host", column.getName());
  }

  /**
   * Verifies new description column for alert definition.
   *
   * @param alertDefinitionDescriptionColumnCapture
   */
  private void verifyAlertDefinitionDescriptionColumn(
      Capture<DBAccessor.DBColumnInfo> alertDefinitionDescriptionColumnCapture) {
    DBColumnInfo column = alertDefinitionDescriptionColumnCapture.getValue();
    Assert.assertEquals(null, column.getDefaultValue());
    Assert.assertEquals(char[].class, column.getType());
    Assert.assertEquals("description", column.getName());
  }

  /**
   * Verifies alert_target_states table.
   *
   * @param alertTargetStatesCapture
   */
  private void verifyAlertTargetStatesTable(
      Capture<List<DBAccessor.DBColumnInfo>> alertTargetStatesCapture) {
    Assert.assertEquals(2, alertTargetStatesCapture.getValue().size());
  }

  /**
   * Verifies is_global added to alert target table.
   *
   * @param alertTargetGlobalCapture
   */
  private void verifyAlertTargetGlobal(
      Capture<DBAccessor.DBColumnInfo> alertTargetGlobalCapture) {
    DBColumnInfo column = alertTargetGlobalCapture.getValue();
    Assert.assertEquals(0, column.getDefaultValue());
    Assert.assertEquals(Short.class, column.getType());
    Assert.assertEquals("is_global", column.getName());
  }

  /**
   * Verifies new security_state column in servicedesiredsstate table.
   *
   * @param securityStateColumnCapture
   */
  private void verifyServiceSecurityStateColumn(
      Capture<DBAccessor.DBColumnInfo> securityStateColumnCapture) {
    DBColumnInfo column = securityStateColumnCapture.getValue();
    Assert.assertEquals(SecurityState.UNSECURED.toString(), column.getDefaultValue());
    Assert.assertEquals(Integer.valueOf(32), column.getLength());
    Assert.assertEquals(String.class, column.getType());
    Assert.assertEquals("security_state", column.getName());
  }

  /**
   * Verifies new security_type column in clusters table
   *
   * @param securityTypeColumnCapture
   */
  private void verifyClustersSecurityType(
      Capture<DBAccessor.DBColumnInfo> securityTypeColumnCapture) {
    DBColumnInfo column = securityTypeColumnCapture.getValue();
    Assert.assertEquals(SecurityType.NONE.toString(), column.getDefaultValue());
    Assert.assertEquals(Integer.valueOf(32), column.getLength());
    Assert.assertEquals(String.class, column.getType());
    Assert.assertEquals("security_type", column.getName());
  }

  /**
   * Verifies new security_state column in hostcomponentdesiredstate and hostcomponentstate tables
   *
   * @param securityStateColumnCapture
   */
  private void verifyComponentSecurityStateColumn(
      Capture<DBAccessor.DBColumnInfo> securityStateColumnCapture) {
    DBColumnInfo column = securityStateColumnCapture.getValue();
    Assert.assertEquals(SecurityState.UNSECURED.toString(), column.getDefaultValue());
    Assert.assertEquals(Integer.valueOf(32), column.getLength());
    Assert.assertEquals(String.class, column.getType());
    Assert.assertEquals("security_state", column.getName());
  }

  private void verifyViewParameterColumns(
      Capture<DBAccessor.DBColumnInfo> labelColumnCapture,
      Capture<DBAccessor.DBColumnInfo> placeholderColumnCapture,
      Capture<DBAccessor.DBColumnInfo> defaultValueColumnCapture) {


    DBColumnInfo column = labelColumnCapture.getValue();
    assertNull(column.getDefaultValue());
    Assert.assertEquals(Integer.valueOf(255), column.getLength());
    Assert.assertEquals(String.class, column.getType());
    Assert.assertEquals("label", column.getName());

    column = placeholderColumnCapture.getValue();
    assertNull(column.getDefaultValue());
    Assert.assertEquals(Integer.valueOf(255), column.getLength());
    Assert.assertEquals(String.class, column.getType());
    Assert.assertEquals("placeholder", column.getName());

    column = defaultValueColumnCapture.getValue();
    assertNull(column.getDefaultValue());
    Assert.assertEquals(Integer.valueOf(2000), column.getLength());
    Assert.assertEquals(String.class, column.getType());
    Assert.assertEquals("default_value", column.getName());
  }

  @Test
  public void testGetSourceVersion() {
    final DBAccessor dbAccessor = createNiceMock(DBAccessor.class);
    UpgradeCatalog upgradeCatalog = getUpgradeCatalog(dbAccessor);
    Assert.assertEquals("1.7.0", upgradeCatalog.getSourceVersion());
  }

  @Test
  public void testGetTargetVersion() throws Exception {
    final DBAccessor dbAccessor = createNiceMock(DBAccessor.class);
    UpgradeCatalog upgradeCatalog = getUpgradeCatalog(dbAccessor);

    Assert.assertEquals("2.0.0", upgradeCatalog.getTargetVersion());
  }

  private void setViewInstancePropertyExpectations(DBAccessor dbAccessor,
                                                   Capture<DBAccessor.DBColumnInfo> valueColumnCapture)
      throws SQLException {

    dbAccessor.alterColumn(eq("viewinstanceproperty"), capture(valueColumnCapture));
  }

  private void setViewInstanceDataExpectations(DBAccessor dbAccessor,
                                               Capture<DBAccessor.DBColumnInfo> dataValueColumnCapture)
      throws SQLException {

    dbAccessor.alterColumn(eq("viewinstancedata"), capture(dataValueColumnCapture));
  }

  private void assertViewInstancePropertyColumns(
      Capture<DBAccessor.DBColumnInfo> valueColumnCapture) {
    DBAccessor.DBColumnInfo column = valueColumnCapture.getValue();
    assertEquals("value", column.getName());
    assertEquals(2000, (int) column.getLength());
    assertEquals(String.class, column.getType());
    assertNull(column.getDefaultValue());
    assertTrue(column.isNullable());
  }

  private void assertViewInstanceDataColumns(
      Capture<DBAccessor.DBColumnInfo> dataValueColumnCapture) {
    DBAccessor.DBColumnInfo column = dataValueColumnCapture.getValue();
    assertEquals("value", column.getName());
    assertEquals(2000, (int) column.getLength());
    assertEquals(String.class, column.getType());
    assertNull(column.getDefaultValue());
    assertTrue(column.isNullable());
  }

  /**
   * assert artifact table creation
   *
   * @param artifactColumns artifact table columns
   */
  private void testCreateArtifactTable(List<DBColumnInfo> artifactColumns) {
    assertEquals(3, artifactColumns.size());
    for (DBColumnInfo column : artifactColumns) {
      if (column.getName().equals("artifact_name")) {
        assertNull(column.getDefaultValue());
        assertEquals(String.class, column.getType());
        assertEquals(255, (int) column.getLength());
        assertEquals(false, column.isNullable());
      } else if (column.getName().equals("foreign_keys")) {
        assertNull(column.getDefaultValue());
        assertEquals(String.class, column.getType());
        assertEquals(255, (int) column.getLength());
        assertEquals(false, column.isNullable());
      } else if (column.getName().equals("artifact_data")) {
        assertNull(column.getDefaultValue());
        assertEquals(char[].class, column.getType());
        assertEquals(false, column.isNullable());
      } else {
        fail("unexpected column name");
      }
    }
  }

  private void testCreateKerberosPrincipalTable(List<DBColumnInfo> columns) {
    assertEquals(3, columns.size());
    for (DBColumnInfo column : columns) {
      if (column.getName().equals("principal_name")) {
        assertNull(column.getDefaultValue());
        assertEquals(String.class, column.getType());
        assertEquals(255, (int) column.getLength());
        assertEquals(false, column.isNullable());
      } else if (column.getName().equals("is_service")) {
        assertEquals(1, column.getDefaultValue());
        assertEquals(Short.class, column.getType());
        assertEquals(1, (int) column.getLength());
        assertEquals(false, column.isNullable());
      } else if (column.getName().equals("cached_keytab_path")) {
        assertNull(column.getDefaultValue());
        assertEquals(String.class, column.getType());
        assertEquals(255, (int) column.getLength());
        assertEquals(true, column.isNullable());
      } else {
        fail("unexpected column name");
      }
    }
  }

  private void testCreateKerberosPrincipalHostTable(List<DBColumnInfo> columns) {
    assertEquals(2, columns.size());
    for (DBColumnInfo column : columns) {
      if (column.getName().equals("principal_name")) {
        assertNull(column.getDefaultValue());
        assertEquals(String.class, column.getType());
        assertEquals(255, (int) column.getLength());
        assertEquals(false, column.isNullable());
      } else if (column.getName().equals("host_name")) {
        assertNull(column.getDefaultValue());
        assertEquals(String.class, column.getType());
        assertEquals(255, (int) column.getLength());
        assertEquals(false, column.isNullable());
      } else {
        fail("unexpected column name");
      }
    }
  }
}
