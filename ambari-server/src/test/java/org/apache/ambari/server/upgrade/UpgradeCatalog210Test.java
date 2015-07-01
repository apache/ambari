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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import com.google.inject.AbstractModule;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ServiceConfigVersionResponse;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.DBAccessor.DBColumnInfo;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ClusterStateDAO;
import org.apache.ambari.server.orm.dao.HostComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.ServiceComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.orm.entities.ClusterStateEntity;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntityPK;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostComponentAdminState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.stack.OsFamily;
import org.easymock.Capture;
import org.easymock.EasyMockSupport;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.persist.PersistService;

/**
 * {@link org.apache.ambari.server.upgrade.UpgradeCatalog210} unit tests.
 */
public class UpgradeCatalog210Test {
  private Injector injector;
  private Provider<EntityManager> entityManagerProvider = createStrictMock(Provider.class);
  private EntityManager entityManager = createNiceMock(EntityManager.class);
  private UpgradeCatalogHelper upgradeCatalogHelper;
  private StackEntity desiredStackEntity;

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
  public void testExecuteDDLUpdates() throws Exception {
    final DBAccessor dbAccessor = createNiceMock(DBAccessor.class);
    Configuration configuration = createNiceMock(Configuration.class);
    Connection connection = createNiceMock(Connection.class);
    Statement statement = createNiceMock(Statement.class);
    ResultSet resultSet = createNiceMock(ResultSet.class);
    expect(configuration.getDatabaseUrl()).andReturn(Configuration.JDBC_IN_MEMORY_URL).anyTimes();
    dbAccessor.getConnection();
    expectLastCall().andReturn(connection).anyTimes();
    connection.createStatement();
    expectLastCall().andReturn(statement).anyTimes();
    statement.executeQuery(anyObject(String.class));
    expectLastCall().andReturn(resultSet).anyTimes();

    // Create DDL sections with their own capture groups
    AlertSectionDDL alertSectionDDL = new AlertSectionDDL();
    HostSectionDDL hostSectionDDL = new HostSectionDDL();
    WidgetSectionDDL widgetSectionDDL = new WidgetSectionDDL();
    ViewSectionDDL viewSectionDDL = new ViewSectionDDL();

    // Execute any DDL schema changes
    alertSectionDDL.execute(dbAccessor);
    hostSectionDDL.execute(dbAccessor);
    widgetSectionDDL.execute(dbAccessor);
    viewSectionDDL.execute(dbAccessor);

    // Replay sections
    replay(dbAccessor, configuration, resultSet, connection, statement);

    AbstractUpgradeCatalog upgradeCatalog = getUpgradeCatalog(dbAccessor);
    Class<?> c = AbstractUpgradeCatalog.class;
    Field f = c.getDeclaredField("configuration");
    f.setAccessible(true);
    f.set(upgradeCatalog, configuration);

    upgradeCatalog.executeDDLUpdates();
    verify(dbAccessor, configuration, resultSet, connection, statement);

    // Verify sections
    alertSectionDDL.verify(dbAccessor);
    hostSectionDDL.verify(dbAccessor);
    widgetSectionDDL.verify(dbAccessor);
    viewSectionDDL.verify(dbAccessor);
  }

  @Test
  public void testExecutePreDMLUpdates() throws Exception {
    Method executeStackPreDMLUpdates = UpgradeCatalog210.class.getDeclaredMethod("executeStackPreDMLUpdates");

    UpgradeCatalog210 upgradeCatalog210 = createMockBuilder(UpgradeCatalog210.class)
        .addMockedMethod( executeStackPreDMLUpdates) .createMock();

    upgradeCatalog210.executeStackPreDMLUpdates();
    expectLastCall().once();

    replay(upgradeCatalog210);

    upgradeCatalog210.executePreDMLUpdates();

    verify(upgradeCatalog210);
  }

  @Test
  public void testExecuteDMLUpdates() throws Exception {
    Method addNewConfigurationsFromXml =
      AbstractUpgradeCatalog.class.getDeclaredMethod("addNewConfigurationsFromXml");

    Method initializeClusterAndServiceWidgets =
      UpgradeCatalog210.class.getDeclaredMethod("initializeClusterAndServiceWidgets");

    Method addMissingConfigs = UpgradeCatalog210.class.getDeclaredMethod("addMissingConfigs");

    Method updateAlertDefinitions = UpgradeCatalog210.class.getDeclaredMethod("updateAlertDefinitions");

    Method removeStormRestApiServiceComponent =
      UpgradeCatalog210.class.getDeclaredMethod("removeStormRestApiServiceComponent");

    UpgradeCatalog210 upgradeCatalog210 = createMockBuilder(UpgradeCatalog210.class)
      .addMockedMethod(addNewConfigurationsFromXml)
      .addMockedMethod(initializeClusterAndServiceWidgets)
      .addMockedMethod(addMissingConfigs)
      .addMockedMethod(updateAlertDefinitions)
      .addMockedMethod(removeStormRestApiServiceComponent).createMock();

    upgradeCatalog210.addNewConfigurationsFromXml();
    expectLastCall().once();

    upgradeCatalog210.initializeClusterAndServiceWidgets();
    expectLastCall().once();

    upgradeCatalog210.addMissingConfigs();
    expectLastCall().once();

    upgradeCatalog210.updateAlertDefinitions();
    expectLastCall().once();

    upgradeCatalog210.removeStormRestApiServiceComponent();
    expectLastCall().once();

    replay(upgradeCatalog210);

    upgradeCatalog210.executeDMLUpdates();

    verify(upgradeCatalog210);
  }

  @Test
  public void testUpdateRangerHiveConfigs() throws Exception{
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariManagementController  mockAmbariManagementController = easyMockSupport.createNiceMock(
        AmbariManagementController.class);
    final ConfigHelper mockConfigHelper = easyMockSupport.createMock(ConfigHelper.class);

    final Clusters mockClusters = easyMockSupport.createStrictMock(Clusters.class);
    final Cluster mockClusterExpected = easyMockSupport.createNiceMock(Cluster.class);

    final Config mockRangerPlugin = easyMockSupport.createNiceMock(Config.class);
    final Config mockHiveEnv = easyMockSupport.createNiceMock(Config.class);
    final Config mockHiveServer = easyMockSupport.createNiceMock(Config.class);

    final Map<String, String> propertiesExpectedRangerPlugin = new HashMap<String, String>();
    propertiesExpectedRangerPlugin.put("ranger-hive-plugin-enabled", "yes");
    final Map<String, String> propertiesExpectedHiveEnv = new HashMap<String, String>();
    final Map<String, String> propertiesExpectedHiveServer2 = new HashMap<String, String>();
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
    }}).once();

    expect(mockClusterExpected.getDesiredConfigByType("ranger-hive-plugin-properties")).andReturn(mockRangerPlugin).atLeastOnce();
    expect(mockClusterExpected.getDesiredConfigByType("hive-env")).andReturn(mockHiveEnv).atLeastOnce();
    expect(mockClusterExpected.getDesiredConfigByType("hiveserver2-site")).andReturn(mockHiveServer).atLeastOnce();
    expect(mockRangerPlugin.getProperties()).andReturn(propertiesExpectedRangerPlugin).anyTimes();
    expect(mockHiveEnv.getProperties()).andReturn(propertiesExpectedHiveEnv).anyTimes();
    expect(mockHiveServer.getProperties()).andReturn(propertiesExpectedHiveServer2).anyTimes();

    ServiceConfigVersionResponse r = null;
    expect(mockClusterExpected.getConfig(anyObject(String.class), anyObject(String.class))).
        andReturn(mockHiveServer).anyTimes();
    expect(mockClusterExpected.addDesiredConfig("ambari-upgrade", Collections.singleton(mockHiveServer))).
        andReturn(r).times(3);

    easyMockSupport.replayAll();
    mockInjector.getInstance(UpgradeCatalog210.class).updateRangerHiveConfigs();
    easyMockSupport.verifyAll();
  }

  @Test
  public void testUpdateHiveConfigs() throws Exception {
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariManagementController  mockAmbariManagementController = easyMockSupport.createNiceMock(AmbariManagementController.class);
    final ConfigHelper mockConfigHelper = easyMockSupport.createMock(ConfigHelper.class);

    final Clusters mockClusters = easyMockSupport.createStrictMock(Clusters.class);
    final Cluster mockClusterExpected = easyMockSupport.createNiceMock(Cluster.class);
    final Config mockHiveEnv = easyMockSupport.createNiceMock(Config.class);

    final Map<String, String> propertiesExpectedHiveEnv = new HashMap<String, String>();
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
    }}).once();

    expect(mockClusterExpected.getDesiredConfigByType("hive-env")).andReturn(mockHiveEnv).atLeastOnce();
    expect(mockHiveEnv.getProperties()).andReturn(propertiesExpectedHiveEnv).anyTimes();
    expect(mockClusterExpected.getConfig(anyObject(String.class), anyObject(String.class))).
        andReturn(mockHiveEnv).anyTimes();
    ServiceConfigVersionResponse r = null;
    expect(mockClusterExpected.addDesiredConfig("ambari-upgrade", Collections.singleton(mockHiveEnv))).
        andReturn(r).once();

    easyMockSupport.replayAll();
    mockInjector.getInstance(UpgradeCatalog210.class).updateHiveConfigs();
    easyMockSupport.verifyAll();
  }

  @Test
  public void TestUpdateHiveEnvContent() {
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariManagementController  mockAmbariManagementController = easyMockSupport.createNiceMock(AmbariManagementController.class);
    final ConfigHelper mockConfigHelper = easyMockSupport.createMock(ConfigHelper.class);
    final Clusters mockClusters = easyMockSupport.createStrictMock(Clusters.class);

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
    String content = "# Start HIVE_AUX_JARS_PATH \n" +
        "if [ \"${HIVE_AUX_JARS_PATH}\" != \"\" ]; then\n" +
        "  export HIVE_AUX_JARS_PATH=${HIVE_AUX_JARS_PATH}\n" +
        "elif [ -d \"/usr/hdp/current/hive-webhcat/share/hcatalog\" ]; then \n" +
        "  export HIVE_AUX_JARS_PATH=/usr/hdp/current/hive-webhcat/share/hcatalog\n" +
        "fi\n" +
        "#End HIVE_AUX_JARS_PATH";
    String expectedContent = "# Start HIVE_AUX_JARS_PATH \n" +
        "if [ \"${HIVE_AUX_JARS_PATH}\" != \"\" ]; then\n" +
        "  if [ -f \"${HIVE_AUX_JARS_PATH}\" ]; then    \n" +
        "    export HIVE_AUX_JARS_PATH=${HIVE_AUX_JARS_PATH}\n" +
        "  elif [ -d \"/usr/hdp/current/hive-webhcat/share/hcatalog\" ]; then\n" +
        "    export HIVE_AUX_JARS_PATH=/usr/hdp/current/hive-webhcat/share/hcatalog/hive-hcatalog-core.jar\n" +
        "  fi\n" +
        "elif [ -d \"/usr/hdp/current/hive-webhcat/share/hcatalog\" ]; then\n" +
        "  export HIVE_AUX_JARS_PATH=/usr/hdp/current/hive-webhcat/share/hcatalog/hive-hcatalog-core.jar\n" +
        "fi\n" +
        "#End HIVE_AUX_JARS_PATH";

    String modifiedContent = mockInjector.getInstance(UpgradeCatalog210.class).updateHiveEnvContent(content);
    Assert.assertEquals(modifiedContent, expectedContent);
  }

  @Test
  public void testInitializeClusterAndServiceWidgets() throws Exception {
    final AmbariManagementController controller = createStrictMock(AmbariManagementController.class);
    final Clusters clusters = createStrictMock(Clusters.class);
    final Cluster cluster = createStrictMock(Cluster.class);
    final Service service = createStrictMock(Service.class);
    final Map<String, Cluster> clusterMap = Collections.singletonMap("c1", cluster);
    final Map<String, Service> services = Collections.singletonMap("HBASE", service);


    Module module = new Module() {
      @Override
      public void configure(Binder binder) {
        binder.bind(AmbariManagementController.class).toInstance(controller);
        binder.bind(Clusters.class).toInstance(clusters);
        binder.bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
        binder.bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
      }
    };

    expect(controller.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getClusters()).andReturn(clusterMap).anyTimes();
    controller.initializeWidgetsAndLayouts(cluster, null);
    expectLastCall().once();

    expect(cluster.getServices()).andReturn(services).once();
    controller.initializeWidgetsAndLayouts(cluster, service);
    expectLastCall().once();

    replay(controller, clusters, cluster);

    Injector injector = Guice.createInjector(module);
    injector.getInstance(UpgradeCatalog210.class).initializeClusterAndServiceWidgets();

    verify(controller, clusters, cluster);
  }

  @Test
  public void testUpdateStormConfiguration() throws Exception {
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariManagementController  mockAmbariManagementController = easyMockSupport.createNiceMock(
        AmbariManagementController.class);
    final ConfigHelper mockConfigHelper = easyMockSupport.createMock(ConfigHelper.class);

    final Clusters mockClusters = easyMockSupport.createStrictMock(Clusters.class);
    final Cluster mockClusterExpected = easyMockSupport.createNiceMock(Cluster.class);

    final Config mockClusterEnv = easyMockSupport.createNiceMock(Config.class);
    final Config mockStormSite = easyMockSupport.createNiceMock(Config.class);

    final Map<String, String> propertiesExpectedClusterEnv = new HashMap<String, String>();
    propertiesExpectedClusterEnv.put("security_enabled", "true");
    final Map<String, String> propertiesExpectedStormSite = new HashMap<String, String>();
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
    }}).once();

    expect(mockClusterExpected.getDesiredConfigByType("cluster-env")).andReturn(mockClusterEnv).atLeastOnce();
    expect(mockClusterExpected.getDesiredConfigByType("storm-site")).andReturn(mockStormSite).atLeastOnce();
    expect(mockClusterEnv.getProperties()).andReturn(propertiesExpectedClusterEnv).anyTimes();
    expect(mockStormSite.getProperties()).andReturn(propertiesExpectedStormSite).anyTimes();

    easyMockSupport.replayAll();
    mockInjector.getInstance(UpgradeCatalog210.class).updateStormConfigs();
    easyMockSupport.verifyAll();
  }

  @Test
  public void testUpdateHBaseConfiguration() throws Exception {
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariManagementController  mockAmbariManagementController = easyMockSupport.createNiceMock(AmbariManagementController.class);
    final ConfigHelper mockConfigHelper = easyMockSupport.createMock(ConfigHelper.class);

    final Clusters mockClusters = easyMockSupport.createStrictMock(Clusters.class);
    final Cluster mockClusterExpected = easyMockSupport.createNiceMock(Cluster.class);
    final Host mockHost = easyMockSupport.createNiceMock(Host.class);

    final Config mockHBaseSite = easyMockSupport.createNiceMock(Config.class);
    final Config mockHBaseEnv = easyMockSupport.createNiceMock(Config.class);

    final Map<String, String> propertiesExpectedHBaseSite = new HashMap<String, String>();
    propertiesExpectedHBaseSite.put("hbase.region.server.rpc.scheduler.factory.class",
                                    "org.apache.phoenix.hbase.index.ipc.PhoenixIndexRpcSchedulerFactory");
    propertiesExpectedHBaseSite.put("hbase.security.authorization", "true");

    final Map<String, String> propertiesExpectedHBaseEnv = new HashMap<String, String>();
    propertiesExpectedHBaseEnv.put("phoenix_sql_enabled", "false");

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
    }}).once();

    expect(mockClusterExpected.getDesiredConfigByType("hbase-site")).andReturn(mockHBaseSite).atLeastOnce();
    expect(mockClusterExpected.getDesiredConfigByType("hbase-env")).andReturn(mockHBaseEnv).atLeastOnce();
    expect(mockHBaseSite.getProperties()).andReturn(propertiesExpectedHBaseSite).anyTimes();
    expect(mockHBaseEnv.getProperties()).andReturn(propertiesExpectedHBaseEnv).anyTimes();

    expect(mockClusterExpected.getHosts(anyObject(String.class), anyObject(String.class))).andReturn(new HashSet<String>() {{
      add("host_1");
    }}).atLeastOnce();
    expect(mockClusterExpected.getHosts()).andReturn(new HashSet<Host>(){{add(mockHost);}}).atLeastOnce();
    expect(mockHost.getHostName()).andReturn("host_1");
    expect(mockHost.getTotalMemBytes()).andReturn(16777216L);

    Capture<String> configType = new Capture<String>();
    Capture<String> configTag = new Capture<String>();
    expect(mockClusterExpected.getConfig(capture(configType), capture(configTag))).
            andReturn(mockHBaseSite).atLeastOnce();

    easyMockSupport.replayAll();
    mockInjector.getInstance(UpgradeCatalog210.class).updateHBaseConfigs();
    easyMockSupport.verifyAll();
  }

  @Test
  public void testDeleteStormRestApiServiceComponent() throws Exception {
    ClusterEntity clusterEntity = upgradeCatalogHelper.createCluster(injector,
      "c1", desiredStackEntity);
    ClusterServiceEntity clusterServiceEntity = upgradeCatalogHelper.createService(
      injector, clusterEntity, "STORM");
    HostEntity hostEntity = upgradeCatalogHelper.createHost(injector,
      clusterEntity, "h1");

    // Set current stack version
    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    ClusterStateDAO clusterStateDAO = injector.getInstance(ClusterStateDAO.class);
    ClusterStateEntity clusterStateEntity = new ClusterStateEntity();
    clusterStateEntity.setClusterId(clusterEntity.getClusterId());
    clusterStateEntity.setClusterEntity(clusterEntity);
    clusterStateEntity.setCurrentStack(desiredStackEntity);
    clusterStateDAO.create(clusterStateEntity);
    clusterEntity.setClusterStateEntity(clusterStateEntity);
    clusterDAO.merge(clusterEntity);

    ServiceComponentDesiredStateEntity componentDesiredStateEntity = new ServiceComponentDesiredStateEntity();
    componentDesiredStateEntity.setClusterId(clusterEntity.getClusterId());
    componentDesiredStateEntity.setServiceName(clusterServiceEntity.getServiceName());
    componentDesiredStateEntity.setClusterServiceEntity(clusterServiceEntity);
    componentDesiredStateEntity.setComponentName("STORM_REST_API");
    componentDesiredStateEntity.setDesiredStack(desiredStackEntity);

    HostComponentDesiredStateDAO hostComponentDesiredStateDAO =
      injector.getInstance(HostComponentDesiredStateDAO.class);

    HostComponentDesiredStateEntity hostComponentDesiredStateEntity = new HostComponentDesiredStateEntity();

    hostComponentDesiredStateEntity.setClusterId(clusterEntity.getClusterId());
    hostComponentDesiredStateEntity.setComponentName("STORM_REST_API");
    hostComponentDesiredStateEntity.setAdminState(HostComponentAdminState.INSERVICE);
    hostComponentDesiredStateEntity.setServiceName(clusterServiceEntity.getServiceName());
    hostComponentDesiredStateEntity.setServiceComponentDesiredStateEntity(componentDesiredStateEntity);
    hostComponentDesiredStateEntity.setHostEntity(hostEntity);
    hostComponentDesiredStateEntity.setDesiredStack(desiredStackEntity);

    hostComponentDesiredStateDAO.create(hostComponentDesiredStateEntity);

    HostComponentDesiredStateEntity entity = hostComponentDesiredStateDAO.findAll().get(0);

    Assert.assertEquals(HostComponentAdminState.INSERVICE.name(), entity.getAdminState().name());

    UpgradeCatalog210 upgradeCatalog210 = injector.getInstance(UpgradeCatalog210.class);
    upgradeCatalog210.removeStormRestApiServiceComponent();

    ServiceComponentDesiredStateDAO componentDesiredStateDAO =
      injector.getInstance(ServiceComponentDesiredStateDAO.class);

    ServiceComponentDesiredStateEntityPK entityPK = new ServiceComponentDesiredStateEntityPK();
    entityPK.setClusterId(clusterEntity.getClusterId());
    entityPK.setServiceName("STORM");
    entityPK.setComponentName("STORM_REST_API");
    Assert.assertNull(componentDesiredStateDAO.findByPK(entityPK));
  }


  @Test
  public void testUpdateHDFSConfiguration() throws Exception {
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariManagementController  mockAmbariManagementController = easyMockSupport.createNiceMock(AmbariManagementController.class);
    final ConfigHelper mockConfigHelper = easyMockSupport.createMock(ConfigHelper.class);

    final Clusters mockClusters = easyMockSupport.createStrictMock(Clusters.class);
    final Cluster mockClusterExpected = easyMockSupport.createNiceMock(Cluster.class);

    final Config mockHdfsSite = easyMockSupport.createNiceMock(Config.class);
    final Config mockCoreSite = easyMockSupport.createStrictMock(Config.class);

    final Map<String, String> propertiesExpectedHdfs = new HashMap<String, String>();
    final Map<String, String> propertiesExpectedCoreSite = new HashMap<String, String>();
    propertiesExpectedCoreSite.put("fs.defaultFS", "hdfs://EXAMPLE.COM:8020");
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
    }}).once();

    // Expected operation
    expect(mockClusterExpected.getDesiredConfigByType("hadoop-env")).andReturn(null).once();

    // Expected operation
    expect(mockClusterExpected.getDesiredConfigByType("hdfs-site")).andReturn(mockHdfsSite).atLeastOnce();
    expect(mockClusterExpected.getHosts("HDFS", "NAMENODE")).andReturn( new HashSet<String>() {{
      add("host1");
    }}).atLeastOnce();
    expect(mockHdfsSite.getProperties()).andReturn(propertiesExpectedHdfs).anyTimes();

    expect(mockClusterExpected.getDesiredConfigByType("core-site")).andReturn(mockCoreSite).anyTimes();
    expect(mockCoreSite.getProperties()).andReturn(propertiesExpectedCoreSite).anyTimes();

    easyMockSupport.replayAll();
    mockInjector.getInstance(UpgradeCatalog210.class).updateHdfsConfigs();
    easyMockSupport.verifyAll();
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
        binder.bind(EntityManager.class).toInstance(entityManager);
        binder.bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
      }
    };

    Injector injector = Guice.createInjector(module);
    return injector.getInstance(UpgradeCatalog210.class);
  }

  @Test
  public void testGetSourceVersion() {
    final DBAccessor dbAccessor = createNiceMock(DBAccessor.class);
    UpgradeCatalog upgradeCatalog = getUpgradeCatalog(dbAccessor);
    Assert.assertEquals("2.0.0", upgradeCatalog.getSourceVersion());
  }

  @Test
  public void testGetTargetVersion() throws Exception {
    final DBAccessor dbAccessor = createNiceMock(DBAccessor.class);
    UpgradeCatalog upgradeCatalog = getUpgradeCatalog(dbAccessor);

    Assert.assertEquals("2.1.0", upgradeCatalog.getTargetVersion());
  }

  // *********** Inner Classes that represent sections of the DDL ***********
  // ************************************************************************

  /**
   * Verify that all of the host-related tables added a column for the host_id
   */
  class HostSectionDDL implements SectionDDL {

    HashMap<String, Capture<DBColumnInfo>> captures;

    public HostSectionDDL() {
      // Capture all tables that will have the host_id column added to it.
      captures = new HashMap<String, Capture<DBColumnInfo>>();

      // Column Capture section
      // Hosts
      Capture<DBAccessor.DBColumnInfo> clusterHostMappingColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
      Capture<DBAccessor.DBColumnInfo> configGroupHostMappingColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
      Capture<DBAccessor.DBColumnInfo> hostConfigMappingColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
      Capture<DBAccessor.DBColumnInfo> hostsColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
      Capture<DBAccessor.DBColumnInfo> hostComponentStateColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
      Capture<DBAccessor.DBColumnInfo> hostComponentDesiredStateColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
      Capture<DBAccessor.DBColumnInfo> hostRoleCommandColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
      Capture<DBAccessor.DBColumnInfo> hostStateColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
      Capture<DBAccessor.DBColumnInfo> hostVersionColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
      Capture<DBAccessor.DBColumnInfo> kerberosPrincipalHostColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
      Capture<DBAccessor.DBColumnInfo> requestOperationLevelColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
      Capture<DBAccessor.DBColumnInfo> serviceConfigHostsColumnCapture = new Capture<DBAccessor.DBColumnInfo>();

      captures.put("ClusterHostMapping", clusterHostMappingColumnCapture);
      captures.put("configgrouphostmapping", configGroupHostMappingColumnCapture);
      captures.put("hostconfigmapping", hostConfigMappingColumnCapture);
      captures.put("hosts", hostsColumnCapture);
      captures.put("hostcomponentstate", hostComponentStateColumnCapture);
      captures.put("hostcomponentdesiredstate", hostComponentDesiredStateColumnCapture);
      captures.put("host_role_command", hostRoleCommandColumnCapture);
      captures.put("hoststate", hostStateColumnCapture);
      captures.put("host_version", hostVersionColumnCapture);
      captures.put("kerberos_principal_host", kerberosPrincipalHostColumnCapture);
      captures.put("requestoperationlevel", requestOperationLevelColumnCapture);
      captures.put("serviceconfighosts", serviceConfigHostsColumnCapture);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(DBAccessor dbAccessor) throws SQLException {
      // Add columns and alter table section
      dbAccessor.addColumn(eq("ClusterHostMapping"), capture(captures.get("ClusterHostMapping")));
      dbAccessor.addColumn(eq("configgrouphostmapping"), capture(captures.get("configgrouphostmapping")));
      dbAccessor.addColumn(eq("hostconfigmapping"), capture(captures.get("hostconfigmapping")));
      dbAccessor.addColumn(eq("hosts"), capture(captures.get("hosts")));
      dbAccessor.addColumn(eq("hostcomponentstate"), capture(captures.get("hostcomponentstate")));
      dbAccessor.addColumn(eq("hostcomponentdesiredstate"), capture(captures.get("hostcomponentdesiredstate")));
      dbAccessor.addColumn(eq("host_role_command"), capture(captures.get("host_role_command")));
      dbAccessor.addColumn(eq("hoststate"), capture(captures.get("hoststate")));
      dbAccessor.addColumn(eq("host_version"), capture(captures.get("host_version")));
      dbAccessor.addColumn(eq("kerberos_principal_host"), capture(captures.get("kerberos_principal_host")));
      dbAccessor.addColumn(eq("requestoperationlevel"), capture(captures.get("requestoperationlevel")));
      dbAccessor.addColumn(eq("serviceconfighosts"), capture(captures.get("serviceconfighosts")));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void verify(DBAccessor dbAccessor) throws SQLException {
      // Verification section
      for (Capture<DBColumnInfo> columnCapture : captures.values()) {
        verifyContainsHostIdColumn(columnCapture);
      }
    }

    /**
     * Verify that the column capture of the table contains a host_id column of type Long.
     * This is needed for all of the host-related tables that are switching from the
     * host_name to the host_id.
     * @param columnCapture
     */
    private void verifyContainsHostIdColumn(Capture<DBAccessor.DBColumnInfo> columnCapture) {
      DBColumnInfo idColumn = columnCapture.getValue();
      Assert.assertEquals(Long.class, idColumn.getType());
      Assert.assertEquals("host_id", idColumn.getName());
    }
  }

  /**
   * Verify that the widget, widget_layout, and widget_layout_user_widget tables are created correctly.
   */
  class WidgetSectionDDL implements SectionDDL {

    HashMap<String, Capture<List<DBColumnInfo>>> captures;
    Capture<DBColumnInfo> userActiveLayoutsColumnCapture;

    public WidgetSectionDDL() {
      captures = new HashMap<String, Capture<List<DBColumnInfo>>>();

      Capture<List<DBColumnInfo>> userWidgetColumnsCapture = new Capture<List<DBColumnInfo>>();
      Capture<List<DBColumnInfo>> widgetLayoutColumnsCapture = new Capture<List<DBColumnInfo>>();
      Capture<List<DBColumnInfo>> widgetLayoutUserWidgetColumnsCapture = new Capture<List<DBColumnInfo>>();

      captures.put("widget", userWidgetColumnsCapture);
      captures.put("widget_layout", widgetLayoutColumnsCapture);
      captures.put("widget_layout_user_widget", widgetLayoutUserWidgetColumnsCapture);
      userActiveLayoutsColumnCapture = new Capture<DBColumnInfo>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(DBAccessor dbAccessor) throws SQLException {
      Capture<List<DBColumnInfo>> userWidgetColumnsCapture = captures.get("widget");
      Capture<List<DBColumnInfo>> widgetLayoutColumnsCapture = captures.get("widget_layout");
      Capture<List<DBColumnInfo>> widgetLayoutUserWidgetColumnsCapture = captures.get("widget_layout_user_widget");

      // User Widget
      dbAccessor.createTable(eq("widget"),
          capture(userWidgetColumnsCapture), eq("id"));

      // Widget Layout
      dbAccessor.createTable(eq("widget_layout"),
          capture(widgetLayoutColumnsCapture), eq("id"));

      // Widget Layout User Widget
      dbAccessor.createTable(eq("widget_layout_user_widget"),
          capture(widgetLayoutUserWidgetColumnsCapture), eq("widget_layout_id"), eq("widget_id"));

      dbAccessor.addColumn(eq("users"), capture(userActiveLayoutsColumnCapture));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void verify(DBAccessor dbAccessor) throws SQLException {
      Capture<List<DBColumnInfo>> widgetColumnsCapture = captures.get("widget");
      Capture<List<DBColumnInfo>> widgetLayoutColumnsCapture = captures.get("widget_layout");
      Capture<List<DBColumnInfo>> widgetLayoutUserWidgetColumnsCapture = captures.get("widget_layout_user_widget");

      // Verify widget tables
      assertEquals(12, widgetColumnsCapture.getValue().size());
      assertEquals(7, widgetLayoutColumnsCapture.getValue().size());
      assertEquals(3, widgetLayoutUserWidgetColumnsCapture.getValue().size());

      DBColumnInfo idColumn = userActiveLayoutsColumnCapture.getValue();
      Assert.assertEquals(String.class, idColumn.getType());
      Assert.assertEquals("active_widget_layouts", idColumn.getName());
    }
  }

  /**
   * Verify view changes
   */
  class ViewSectionDDL implements SectionDDL {

    HashMap<String, Capture<DBColumnInfo>> captures;

    public ViewSectionDDL() {
      captures = new HashMap<String, Capture<DBColumnInfo>>();

      Capture<DBAccessor.DBColumnInfo> viewInstanceColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
      Capture<DBAccessor.DBColumnInfo> viewInstanceAlterNamesColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
      Capture<DBAccessor.DBColumnInfo> viewParamColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
      Capture<DBAccessor.DBColumnInfo> viewBuildColumnCapture = new Capture<DBAccessor.DBColumnInfo>();

      captures.put("viewinstance", viewInstanceColumnCapture);
      captures.put("viewinstance_alter_names", viewInstanceAlterNamesColumnCapture);
      captures.put("viewparameter", viewParamColumnCapture);
      captures.put("viewmain", viewBuildColumnCapture);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(DBAccessor dbAccessor) throws SQLException {
      Capture<DBColumnInfo> viewInstanceColumnCapture = captures.get("viewinstance");
      Capture<DBColumnInfo> viewInstanceAlterNamesColumnCapture = captures.get("viewinstance_alter_names");
      Capture<DBColumnInfo> viewParamColumnCapture = captures.get("viewparameter");
      Capture<DBColumnInfo> viewBuildColumnCapture = captures.get("viewmain");

      dbAccessor.addColumn(eq("viewinstance"), capture(viewInstanceColumnCapture));
      dbAccessor.addColumn(eq("viewinstance"), capture(viewInstanceAlterNamesColumnCapture));
      dbAccessor.addColumn(eq("viewparameter"), capture(viewParamColumnCapture));
      dbAccessor.addColumn(eq("viewmain"), capture(viewBuildColumnCapture));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void verify(DBAccessor dbAccessor) throws SQLException {
      verifyViewInstance(captures.get("viewinstance"));
      verifyViewInstanceAlterNames(captures.get("viewinstance_alter_names"));
      verifyViewParameter(captures.get("viewparameter"));
      verifyViewBuild(captures.get("viewmain"));
    }

    private void verifyViewInstance(Capture<DBAccessor.DBColumnInfo> viewInstanceColumnCapture) {
      DBColumnInfo clusterIdColumn = viewInstanceColumnCapture.getValue();
      Assert.assertEquals(String.class, clusterIdColumn.getType());
      Assert.assertEquals("cluster_handle", clusterIdColumn.getName());
    }

    private void verifyViewInstanceAlterNames(Capture<DBAccessor.DBColumnInfo> viewInstanceAlterNamesColumnCapture) {
      DBColumnInfo clusterIdColumn = viewInstanceAlterNamesColumnCapture.getValue();
      Assert.assertEquals(Integer.class, clusterIdColumn.getType());
      Assert.assertEquals("alter_names", clusterIdColumn.getName());
    }

    private void verifyViewParameter(Capture<DBAccessor.DBColumnInfo> viewParamColumnCapture) {
      DBColumnInfo clusterConfigColumn = viewParamColumnCapture.getValue();
      Assert.assertEquals(String.class, clusterConfigColumn.getType());
      Assert.assertEquals("cluster_config", clusterConfigColumn.getName());
    }

    private void verifyViewBuild(Capture<DBAccessor.DBColumnInfo> viewBuildColumnCapture) {
      DBColumnInfo clusterConfigColumn = viewBuildColumnCapture.getValue();
      Assert.assertEquals(String.class, clusterConfigColumn.getType());
      Assert.assertEquals("build", clusterConfigColumn.getName());
    }
  }

  /**
   * Verify alert changes
   */
  class AlertSectionDDL implements SectionDDL {
    HashMap<String, Capture<String>> stringCaptures;
    HashMap<String, Capture<Class>> classCaptures;


    public AlertSectionDDL() {
      stringCaptures = new HashMap<String, Capture<String>>();
      classCaptures = new HashMap<String, Capture<Class>>();

      Capture<String> textCaptureC = new Capture<String>();
      Capture<String> textCaptureH = new Capture<String>();
      Capture<Class>  classFromC = new Capture<Class>();
      Capture<Class>  classFromH = new Capture<Class>();
      Capture<Class>  classToC = new Capture<Class>();
      Capture<Class>  classToH = new Capture<Class>();

      stringCaptures.put("textCaptureC", textCaptureC);
      stringCaptures.put("textCaptureH", textCaptureH);
      classCaptures.put("classFromC", classFromC);
      classCaptures.put("classFromH", classFromH);
      classCaptures.put("classToC", classToC);
      classCaptures.put("classToH", classToH);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(DBAccessor dbAccessor) throws SQLException {
      Capture<String> textCaptureC = stringCaptures.get("textCaptureC");
      Capture<String> textCaptureH = stringCaptures.get("textCaptureH");
      Capture<Class>  classFromC = classCaptures.get("classFromC");
      Capture<Class>  classFromH = classCaptures.get("classFromH");
      Capture<Class>  classToC = classCaptures.get("classToC");
      Capture<Class>  classToH = classCaptures.get("classToH");

      dbAccessor.changeColumnType(eq("alert_current"), capture(textCaptureC), capture(classFromC), capture(classToC));
      dbAccessor.changeColumnType(eq("alert_history"), capture(textCaptureH), capture(classFromH), capture(classToH));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void verify(DBAccessor dbAccessor) throws SQLException {
      Capture<String> textCaptureC = stringCaptures.get("textCaptureC");
      Capture<String> textCaptureH = stringCaptures.get("textCaptureH");
      Capture<Class>  classFromC = classCaptures.get("classFromC");
      Capture<Class>  classFromH = classCaptures.get("classFromH");
      Capture<Class>  classToC = classCaptures.get("classToC");
      Capture<Class>  classToH = classCaptures.get("classToH");

      Assert.assertEquals("latest_text", textCaptureC.getValue());
      Assert.assertEquals(String.class, classFromC.getValue());
      Assert.assertEquals(char[].class, classToC.getValue());

      Assert.assertEquals("alert_text", textCaptureH.getValue());
      Assert.assertEquals(String.class, classFromH.getValue());
      Assert.assertEquals(char[].class, classToH.getValue());
    }
  }
}
