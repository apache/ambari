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

import static org.easymock.EasyMock.anyLong;
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
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariManagementControllerImpl;
import org.apache.ambari.server.controller.ConfigurationRequest;
import org.apache.ambari.server.controller.ConfigurationResponse;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.MaintenanceStateHelper;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ClusterVersionDAO;
import org.apache.ambari.server.orm.dao.DaoUtils;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterVersionEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.stack.StackManagerFactory;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.state.stack.upgrade.RepositoryVersionHelper;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.persist.PersistService;

/**
 * {@link org.apache.ambari.server.upgrade.UpgradeCatalog213} unit tests.
 */
public class UpgradeCatalog213Test {
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
  public void testExecuteUpgradeDDLUpdates() throws Exception{
    final DBAccessor dbAccessor = createNiceMock(DBAccessor.class);

    dbAccessor.addColumn(eq("upgrade"), anyObject(DBAccessor.DBColumnInfo.class));
    expectLastCall().times(3);

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
    UpgradeCatalog213 upgradeCatalog213 = injector.getInstance(UpgradeCatalog213.class);
    upgradeCatalog213.executeUpgradeDDLUpdates();
    verify(dbAccessor);
  }

  @Test
  public void testExecuteStageDDLUpdates() throws Exception {
    final DBAccessor dbAccessor = createNiceMock(DBAccessor.class);

    dbAccessor.addColumn(eq("stage"), anyObject(DBAccessor.DBColumnInfo.class));
    expectLastCall().times(1);

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
    UpgradeCatalog213 upgradeCatalog213 = injector.getInstance(UpgradeCatalog213.class);
    upgradeCatalog213.executeStageDDLUpdates();
    verify(dbAccessor);
  }

  @Test
  public void testExecuteDMLUpdates() throws Exception {
    // TODO AMBARI-13001, readd unit test section.
    /*
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

    // Technically, this is a DDL, but it has to be ran during the DML portion
    // because it requires the persistence layer to be started.
    UpgradeSectionDDL upgradeSectionDDL = new UpgradeSectionDDL();

    // Execute any DDL schema changes
    upgradeSectionDDL.execute(dbAccessor);

    // Begin DML verifications
    verifyBootstrapHDP21();

    // Replay main sections
    replay(dbAccessor, configuration, resultSet, connection, statement);


    AbstractUpgradeCatalog upgradeCatalog = getUpgradeCatalog(dbAccessor);
    Class<?> c = AbstractUpgradeCatalog.class;
    Field f = c.getDeclaredField("configuration");
    f.setAccessible(true);
    f.set(upgradeCatalog, configuration);
    */

    Method updateStormConfigs = UpgradeCatalog213.class.getDeclaredMethod("updateStormConfigs");
    Method updateAMSConfigs = UpgradeCatalog213.class.getDeclaredMethod("updateAMSConfigs");
    Method updateHDFSConfigs = UpgradeCatalog213.class.getDeclaredMethod("updateHDFSConfigs");
    Method updateKafkaConfigs = UpgradeCatalog213.class.getDeclaredMethod("updateKafkaConfigs");
    Method addNewConfigurationsFromXml = AbstractUpgradeCatalog.class.getDeclaredMethod("addNewConfigurationsFromXml");
    Method updateHbaseEnvConfig = UpgradeCatalog213.class.getDeclaredMethod("updateHbaseEnvConfig");
    Method updateFlumeEnvConfig = UpgradeCatalog213.class.getDeclaredMethod("updateFlumeEnvConfig");
    Method updateZookeeperLog4j = UpgradeCatalog213.class.getDeclaredMethod("updateZookeeperLog4j");
    Method updateHadoopEnvConfig = UpgradeCatalog213.class.getDeclaredMethod("updateHadoopEnv");
    Method updateAlertDefinitions = UpgradeCatalog213.class.getDeclaredMethod("updateAlertDefinitions");
    Method updateRangerEnvConfig = UpgradeCatalog213.class.getDeclaredMethod("updateRangerEnvConfig");
    Method updateHiveConfig = UpgradeCatalog213.class.getDeclaredMethod("updateHiveConfig");
    Method updateAccumuloConfigs = UpgradeCatalog213.class.getDeclaredMethod("updateAccumuloConfigs");

    UpgradeCatalog213 upgradeCatalog213 = createMockBuilder(UpgradeCatalog213.class)
      .addMockedMethod(updateAMSConfigs)
      .addMockedMethod(updateHDFSConfigs)
      .addMockedMethod(updateStormConfigs)
      .addMockedMethod(addNewConfigurationsFromXml)
      .addMockedMethod(updateHbaseEnvConfig)
      .addMockedMethod(updateFlumeEnvConfig)
      .addMockedMethod(updateAlertDefinitions)
      .addMockedMethod(updateKafkaConfigs)
      .addMockedMethod(updateZookeeperLog4j)
      .addMockedMethod(updateHadoopEnvConfig)
      .addMockedMethod(updateRangerEnvConfig)
      .addMockedMethod(updateHiveConfig)
      .addMockedMethod(updateAccumuloConfigs)
      .createMock();

    upgradeCatalog213.updateHbaseEnvConfig();
    expectLastCall().once();
    upgradeCatalog213.updateFlumeEnvConfig();
    upgradeCatalog213.addNewConfigurationsFromXml();
    expectLastCall().once();
    upgradeCatalog213.updateStormConfigs();
    expectLastCall().once();
    upgradeCatalog213.updateHadoopEnv();
    expectLastCall().once();
    upgradeCatalog213.updateAMSConfigs();
    expectLastCall().once();
    upgradeCatalog213.updateAlertDefinitions();
    expectLastCall().once();
    upgradeCatalog213.updateKafkaConfigs();
    expectLastCall().once();
    upgradeCatalog213.updateHDFSConfigs();
    expectLastCall().once();
    upgradeCatalog213.updateZookeeperLog4j();
    expectLastCall().once();
    upgradeCatalog213.updateRangerEnvConfig();
    expectLastCall().once();
    upgradeCatalog213.updateHiveConfig();
    expectLastCall().once();
    upgradeCatalog213.updateAccumuloConfigs();
    expectLastCall().once();

    replay(upgradeCatalog213);

    upgradeCatalog213.executeDMLUpdates();

    verify(upgradeCatalog213);
  }

  /**
   * Verify that when bootstrapping HDP 2.1, records get inserted into the
   * repo_version, cluster_version, and host_version tables.
   * @throws AmbariException
   */
  private void verifyBootstrapHDP21() throws Exception, AmbariException {
    final String stackName = "HDP";
    final String stackVersion = "2.1";
    final String stackNameAndVersion = stackName + "-" + stackVersion;
    final String buildNumber = "2.1.0.0-0001";
    final String stackAndBuild = stackName + "-" + buildNumber;
    final String clusterName = "c1";

    expect(amc.getAmbariMetaInfo()).andReturn(metaInfo);

    // Mock the actions to bootstrap if using HDP 2.1
    Clusters clusters = createNiceMock(Clusters.class);
    expect(amc.getClusters()).andReturn(clusters);

    Map<String, Cluster> clusterHashMap = new HashMap<String, Cluster>();
    Cluster cluster = createNiceMock(Cluster.class);
    clusterHashMap.put(clusterName, cluster);
    expect(clusters.getClusters()).andReturn(clusterHashMap);

    StackId stackId = new StackId(stackNameAndVersion);
    expect(cluster.getCurrentStackVersion()).andReturn(stackId);

    StackInfo stackInfo = new StackInfo();
    stackInfo.setVersion(buildNumber);
    expect(metaInfo.getStack(stackName, stackVersion)).andReturn(stackInfo);

    StackEntity stackEntity = createNiceMock(StackEntity.class);
    expect(stackEntity.getStackName()).andReturn(stackName);
    expect(stackEntity.getStackVersion()).andReturn(stackVersion);

    expect(stackDAO.find(stackName, stackVersion)).andReturn(stackEntity);

    replay(amc, metaInfo, clusters, cluster, stackEntity, stackDAO);

    // Mock more function calls
    // Repository Version
    RepositoryVersionEntity repositoryVersionEntity = createNiceMock(RepositoryVersionEntity.class);
    expect(repositoryVersionDAO.findByDisplayName(stackAndBuild)).andReturn(null);
    expect(repositoryVersionDAO.findMaxId("id")).andReturn(0L);
    expect(repositoryVersionDAO.findAll()).andReturn(Collections.<RepositoryVersionEntity>emptyList());
    expect(repositoryVersionDAO.create(anyObject(StackEntity.class), anyObject(String.class), anyObject(String.class), anyObject(String.class))).andReturn(repositoryVersionEntity);
    expect(repositoryVersionEntity.getId()).andReturn(1L);
    expect(repositoryVersionEntity.getVersion()).andReturn(buildNumber);
    replay(repositoryVersionDAO, repositoryVersionEntity);

    // Cluster Version
    ClusterVersionEntity clusterVersionEntity = createNiceMock(ClusterVersionEntity.class);
    expect(clusterVersionEntity.getId()).andReturn(1L);
    expect(clusterVersionEntity.getState()).andReturn(RepositoryVersionState.CURRENT);
    expect(clusterVersionEntity.getRepositoryVersion()).andReturn(repositoryVersionEntity);

    expect(clusterVersionDAO.findByClusterAndStackAndVersion(anyObject(String.class), anyObject(StackId.class), anyObject(String.class))).andReturn(null);
    expect(clusterVersionDAO.findMaxId("id")).andReturn(0L);
    expect(clusterVersionDAO.findAll()).andReturn(Collections.<ClusterVersionEntity>emptyList());
    expect(clusterVersionDAO.create(anyObject(ClusterEntity.class), anyObject(RepositoryVersionEntity.class), anyObject(RepositoryVersionState.class), anyLong(), anyLong(), anyObject(String.class))).andReturn(clusterVersionEntity);
    replay(clusterVersionDAO, clusterVersionEntity);

    // Host Version
    ClusterEntity clusterEntity = createNiceMock(ClusterEntity.class);
    expect(clusterEntity.getClusterName()).andReturn(clusterName).anyTimes();
    expect(clusterDAO.findByName(anyObject(String.class))).andReturn(clusterEntity);

    Collection<HostEntity> hostEntities = new ArrayList<HostEntity>();
    HostEntity hostEntity1 = createNiceMock(HostEntity.class);
    HostEntity hostEntity2 = createNiceMock(HostEntity.class);
    expect(hostEntity1.getHostName()).andReturn("host1");
    expect(hostEntity2.getHostName()).andReturn("host2");
    hostEntities.add(hostEntity1);
    hostEntities.add(hostEntity2);
    expect(clusterEntity.getHostEntities()).andReturn(hostEntities);

    expect(hostVersionDAO.findByClusterStackVersionAndHost(anyObject(String.class), anyObject(StackId.class), anyObject(String.class), anyObject(String.class))).andReturn(null);
    expect(hostVersionDAO.findMaxId("id")).andReturn(0L);
    expect(hostVersionDAO.findAll()).andReturn(Collections.<HostVersionEntity>emptyList());

    replay(clusterEntity, clusterDAO, hostVersionDAO, hostEntity1, hostEntity2);
  }

  @Test
  public void testExecuteUpgradePreDMLUpdates() throws Exception {
    Method executeStackPreDMLUpdates = UpgradeCatalog213.class.getDeclaredMethod("executeUpgradePreDMLUpdates");
    Method executeStackUpgradeDDLUpdates = UpgradeCatalog213.class.getDeclaredMethod("executeStackUpgradeDDLUpdates");
    Method bootstrapRepoVersionForHDP21 = UpgradeCatalog213.class.getDeclaredMethod("bootstrapRepoVersionForHDP21");

    final UpgradeCatalog213 upgradeCatalog213 = createMockBuilder(UpgradeCatalog213.class)
      .addMockedMethod(executeStackUpgradeDDLUpdates)
      .addMockedMethod(bootstrapRepoVersionForHDP21)
      .addMockedMethod(executeStackPreDMLUpdates).createMock();

    final Injector mockInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(UpgradeCatalog213.class).toInstance(upgradeCatalog213);
        bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(EntityManager.class).toInstance(entityManager);
      }
    });

    upgradeCatalog213.executeUpgradePreDMLUpdates();
    expectLastCall().once();

    upgradeCatalog213.executeStackUpgradeDDLUpdates();
    expectLastCall().once();

    upgradeCatalog213.bootstrapRepoVersionForHDP21();
    expectLastCall().once();

    replay(upgradeCatalog213);
    mockInjector.getInstance(UpgradeCatalog213.class).executePreDMLUpdates();

    verify(upgradeCatalog213);
  }

  @Test
  public void testUpdateStormSiteConfigs() throws Exception {
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariManagementController mockAmbariManagementController = easyMockSupport.createNiceMock(AmbariManagementController.class);
    final ConfigHelper mockConfigHelper = easyMockSupport.createMock(ConfigHelper.class);

    final Clusters mockClusters = easyMockSupport.createStrictMock(Clusters.class);
    final Cluster mockClusterExpected = easyMockSupport.createNiceMock(Cluster.class);
    final Map<String, String> propertiesStormSite = new HashMap<String, String>() {
      {
        put("nimbus.monitor.freq.secs", "10");
      }
    };

    final Config mockStormSite = easyMockSupport.createNiceMock(Config.class);
    expect(mockStormSite.getProperties()).andReturn(propertiesStormSite).once();

    final Injector mockInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(AmbariManagementController.class).toInstance(mockAmbariManagementController);
        bind(ConfigHelper.class).toInstance(mockConfigHelper);
        bind(Clusters.class).toInstance(mockClusters);
        bind(EntityManager.class).toInstance(entityManager);

        bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
      }
    });

    expect(mockAmbariManagementController.getClusters()).andReturn(mockClusters).once();
    expect(mockClusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", mockClusterExpected);
    }}).once();

    expect(mockClusterExpected.getDesiredConfigByType("storm-site")).andReturn(mockStormSite).atLeastOnce();
    expect(mockStormSite.getProperties()).andReturn(propertiesStormSite).atLeastOnce();

    easyMockSupport.replayAll();
    mockInjector.getInstance(UpgradeCatalog213.class).updateStormConfigs();
    easyMockSupport.verifyAll();
  }

  @Test
  public void testUpdateHbaseEnvConfig() throws AmbariException {
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariManagementController mockAmbariManagementController = easyMockSupport.createNiceMock(AmbariManagementController.class);
    final Clusters mockClusters = easyMockSupport.createStrictMock(Clusters.class);
    final Cluster mockClusterExpected = easyMockSupport.createNiceMock(Cluster.class);
    final Map<String, String> propertiesHbaseEnv = new HashMap<String, String>() {
      {
        put("content", "test");
      }
    };

    final Config mockHbaseEnv = easyMockSupport.createNiceMock(Config.class);
    expect(mockHbaseEnv.getProperties()).andReturn(propertiesHbaseEnv).once();

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
    expect(mockClusterExpected.getCurrentStackVersion()).andReturn(new StackId("HDP", "2.2"));

    expect(mockClusterExpected.getDesiredConfigByType("hbase-env")).andReturn(mockHbaseEnv).atLeastOnce();
    expect(mockHbaseEnv.getProperties()).andReturn(propertiesHbaseEnv).atLeastOnce();

    easyMockSupport.replayAll();
    mockInjector.getInstance(UpgradeCatalog213.class).updateHbaseEnvConfig();
    easyMockSupport.verifyAll();

  }

  @Test
  public void testUpdateHDFSConfiguration() throws Exception {
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariManagementController mockAmbariManagementController = easyMockSupport.createNiceMock(AmbariManagementController.class);
    final ConfigHelper mockConfigHelper = easyMockSupport.createMock(ConfigHelper.class);

    final Clusters mockClusters = easyMockSupport.createStrictMock(Clusters.class);
    final Cluster mockClusterExpected = easyMockSupport.createNiceMock(Cluster.class);

    final Config mockHdfsSite = easyMockSupport.createNiceMock(Config.class);

    final Map<String, String> propertiesExpectedHdfs = new HashMap<String, String>();
    propertiesExpectedHdfs.put("dfs.namenode.rpc-address", "nn.rpc.address");
    propertiesExpectedHdfs.put("dfs.nameservices", "nn1");
    propertiesExpectedHdfs.put("dfs.ha.namenodes.nn1", "value");

    final Injector mockInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(AmbariManagementController.class).toInstance(mockAmbariManagementController);
        bind(ConfigHelper.class).toInstance(mockConfigHelper);
        bind(Clusters.class).toInstance(mockClusters);
        bind(EntityManager.class).toInstance(entityManager);

        bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
      }
    });

    expect(mockAmbariManagementController.getClusters()).andReturn(mockClusters).once();
    expect(mockClusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", mockClusterExpected);
    }}).once();

    // Expected operation
    expect(mockClusterExpected.getDesiredConfigByType("hdfs-site")).andReturn(mockHdfsSite).atLeastOnce();
    expect(mockHdfsSite.getProperties()).andReturn(propertiesExpectedHdfs).anyTimes();

    easyMockSupport.replayAll();
    mockInjector.getInstance(UpgradeCatalog213.class).updateHDFSConfigs();
    easyMockSupport.verifyAll();
  }

  @Test
  public void testUpdateAmsHbaseEnvContent() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method updateAmsHbaseEnvContent = UpgradeCatalog213.class.getDeclaredMethod("updateAmsHbaseEnvContent", String.class);
    UpgradeCatalog213 upgradeCatalog213 = new UpgradeCatalog213(injector);
    String oldContent = "export HBASE_CLASSPATH=${HBASE_CLASSPATH}\n" +
      "\n" +
      "# The maximum amount of heap to use, in MB. Default is 1000.\n" +
      "export HBASE_HEAPSIZE={{hbase_heapsize}}\n" +
      "\n" +
      "{% if java_version &lt; 8 %}\n" +
      "export HBASE_MASTER_OPTS=\" -XX:PermSize=64m -XX:MaxPermSize={{hbase_master_maxperm_size}} -Xms{{hbase_heapsize}} -Xmx{{hbase_heapsize}} -Xmn{{hbase_master_xmn_size}} -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseCMSInitiatingOccupancyOnly\"\n" +
      "export HBASE_REGIONSERVER_OPTS=\"-XX:MaxPermSize=128m -Xmn{{regionserver_xmn_size}} -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseCMSInitiatingOccupancyOnly -Xms{{regionserver_heapsize}} -Xmx{{regionserver_heapsize}}\"\n" +
      "{% else %}\n" +
      "export HBASE_MASTER_OPTS=\" -Xms{{hbase_heapsize}} -Xmx{{hbase_heapsize}} -Xmn{{hbase_master_xmn_size}} -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseCMSInitiatingOccupancyOnly\"\n" +
      "export HBASE_REGIONSERVER_OPTS=\" -Xmn{{regionserver_xmn_size}} -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseCMSInitiatingOccupancyOnly -Xms{{regionserver_heapsize}} -Xmx{{regionserver_heapsize}}\"\n" +
      "{% endif %}\n";
    String expectedContent = "export HBASE_CLASSPATH=${HBASE_CLASSPATH}\n" +
      "\n" +
      "# The maximum amount of heap to use, in MB. Default is 1000.\n" +
      "export HBASE_HEAPSIZE={{hbase_heapsize}}m\n" +
      "\n" +
      "{% if java_version &lt; 8 %}\n" +
      "export HBASE_MASTER_OPTS=\" -XX:PermSize=64m -XX:MaxPermSize={{hbase_master_maxperm_size}}m -Xms{{hbase_heapsize}}m -Xmx{{hbase_heapsize}}m -Xmn{{hbase_master_xmn_size}}m -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseCMSInitiatingOccupancyOnly\"\n" +
      "export HBASE_REGIONSERVER_OPTS=\"-XX:MaxPermSize=128m -Xmn{{regionserver_xmn_size}}m -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseCMSInitiatingOccupancyOnly -Xms{{regionserver_heapsize}}m -Xmx{{regionserver_heapsize}}m\"\n" +
      "{% else %}\n" +
      "export HBASE_MASTER_OPTS=\" -Xms{{hbase_heapsize}}m -Xmx{{hbase_heapsize}}m -Xmn{{hbase_master_xmn_size}}m -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseCMSInitiatingOccupancyOnly\"\n" +
      "export HBASE_REGIONSERVER_OPTS=\" -Xmn{{regionserver_xmn_size}}m -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseCMSInitiatingOccupancyOnly -Xms{{regionserver_heapsize}}m -Xmx{{regionserver_heapsize}}m\"\n" +
      "{% endif %}\n";
    String result = (String) updateAmsHbaseEnvContent.invoke(upgradeCatalog213, oldContent);
    Assert.assertEquals(expectedContent, result);
  }

  @Test
  public void testUpdateAmsEnvContent() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method updateAmsEnvContent = UpgradeCatalog213.class.getDeclaredMethod("updateAmsEnvContent", String.class);
    UpgradeCatalog213 upgradeCatalog213 = new UpgradeCatalog213(injector);
    String oldContent = "# AMS Collector heapsize\n" +
      "export AMS_COLLECTOR_HEAPSIZE={{metrics_collector_heapsize}}\n";
    String expectedContent = "# AMS Collector heapsize\n" +
      "export AMS_COLLECTOR_HEAPSIZE={{metrics_collector_heapsize}}m\n";
    String result = (String) updateAmsEnvContent.invoke(upgradeCatalog213, oldContent);
    Assert.assertEquals(expectedContent, result);
  }

  @Test
  public void testUpdateAmsConfigs() throws Exception {
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariManagementController mockAmbariManagementController = easyMockSupport.createNiceMock(AmbariManagementController.class);
    final ConfigHelper mockConfigHelper = easyMockSupport.createMock(ConfigHelper.class);

    final Clusters mockClusters = easyMockSupport.createStrictMock(Clusters.class);
    final Cluster mockClusterExpected = easyMockSupport.createNiceMock(Cluster.class);
    final Map<String, String> propertiesAmsEnv = new HashMap<String, String>() {
      {
        put("metrics_collector_heapsize", "512m");
      }
    };

    final Map<String, String> propertiesAmsHbaseEnv = new HashMap<String, String>() {
      {
        put("hbase_regionserver_heapsize", "512m");
        put("regionserver_xmn_size", "512m");
        put("hbase_master_xmn_size", "512m");
        put("hbase_master_maxperm_size", "512");
      }
    };

    final Config mockAmsEnv = easyMockSupport.createNiceMock(Config.class);
    final Config mockAmsHbaseEnv = easyMockSupport.createNiceMock(Config.class);

    final Injector mockInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(AmbariManagementController.class).toInstance(mockAmbariManagementController);
        bind(ConfigHelper.class).toInstance(mockConfigHelper);
        bind(Clusters.class).toInstance(mockClusters);
        bind(EntityManager.class).toInstance(entityManager);

        bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
      }
    });

    expect(mockAmbariManagementController.getClusters()).andReturn(mockClusters).once();
    expect(mockClusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", mockClusterExpected);
    }}).once();

    expect(mockClusterExpected.getDesiredConfigByType("ams-env")).andReturn(mockAmsEnv).atLeastOnce();
    expect(mockClusterExpected.getDesiredConfigByType("ams-hbase-env")).andReturn(mockAmsHbaseEnv).atLeastOnce();
    expect(mockAmsEnv.getProperties()).andReturn(propertiesAmsEnv).atLeastOnce();
    expect(mockAmsHbaseEnv.getProperties()).andReturn(propertiesAmsHbaseEnv).atLeastOnce();

    easyMockSupport.replayAll();
    mockInjector.getInstance(UpgradeCatalog213.class).updateAMSConfigs();
    easyMockSupport.verifyAll();
  }

  @Test
  public void testAmsSiteUpdateConfigs() throws Exception{

    Map<String, String> oldPropertiesAmsSite = new HashMap<String, String>() {
      {
        //Including only those properties that might be present in an older version.
        put("timeline.metrics.service.default.result.limit", String.valueOf(5760));
        put("timeline.metrics.cluster.aggregator.minute.interval", String.valueOf(1000));
        put("timeline.metrics.host.aggregator.minute.interval", String.valueOf(1000));
        put("timeline.metrics.cluster.aggregator.minute.ttl", String.valueOf(1000));
      }
    };
    Map<String, String> newPropertiesAmsSite = new HashMap<String, String>() {
      {
        put("timeline.metrics.service.default.result.limit", String.valueOf(15840));
        put("timeline.metrics.cluster.aggregator.second.interval", String.valueOf(120));
        put("timeline.metrics.cluster.aggregator.minute.interval", String.valueOf(300));
        put("timeline.metrics.host.aggregator.minute.interval", String.valueOf(300));
        put("timeline.metrics.cluster.aggregator.second.ttl", String.valueOf(2592000));
        put("timeline.metrics.cluster.aggregator.minute.ttl", String.valueOf(7776000));
        put("timeline.metrics.cluster.aggregator.second.checkpointCutOffMultiplier", String.valueOf(2));
        put("timeline.metrics.cluster.aggregator.second.disabled", String.valueOf(false));
      }
    };
    EasyMockSupport easyMockSupport = new EasyMockSupport();

    Clusters clusters = easyMockSupport.createNiceMock(Clusters.class);
    final Cluster cluster = easyMockSupport.createNiceMock(Cluster.class);
    Config mockAmsSite = easyMockSupport.createNiceMock(Config.class);

    expect(clusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", cluster);
    }}).once();
    expect(cluster.getDesiredConfigByType("ams-site")).andReturn(mockAmsSite).atLeastOnce();
    expect(mockAmsSite.getProperties()).andReturn(oldPropertiesAmsSite).times(1);

    Injector injector = easyMockSupport.createNiceMock(Injector.class);
    expect(injector.getInstance(Gson.class)).andReturn(null).anyTimes();
    expect(injector.getInstance(MaintenanceStateHelper.class)).andReturn(null).anyTimes();
    expect(injector.getInstance(KerberosHelper.class)).andReturn(createNiceMock(KerberosHelper.class)).anyTimes();

    replay(injector, clusters, mockAmsSite, cluster);

    AmbariManagementControllerImpl controller = createMockBuilder(AmbariManagementControllerImpl.class)
      .addMockedMethod("createConfiguration")
      .addMockedMethod("getClusters", new Class[] { })
      .withConstructor(createNiceMock(ActionManager.class), clusters, injector)
      .createNiceMock();

    Injector injector2 = easyMockSupport.createNiceMock(Injector.class);
    Capture<ConfigurationRequest> configurationRequestCapture = EasyMock.newCapture();
    ConfigurationResponse configurationResponseMock = easyMockSupport.createMock(ConfigurationResponse.class);

    expect(injector2.getInstance(AmbariManagementController.class)).andReturn(controller).anyTimes();
    expect(controller.getClusters()).andReturn(clusters).anyTimes();
    expect(controller.createConfiguration(capture(configurationRequestCapture))).andReturn(configurationResponseMock).once();

    replay(controller, injector2, configurationResponseMock);
    new UpgradeCatalog213(injector2).updateAMSConfigs();
    easyMockSupport.verifyAll();

    ConfigurationRequest configurationRequest = configurationRequestCapture.getValue();
    Map<String, String> updatedProperties = configurationRequest.getProperties();
    assertTrue(Maps.difference(newPropertiesAmsSite, updatedProperties).areEqual());

  }

  @Test
  public void testUpdateAlertDefinitions() {
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    UpgradeCatalog213 upgradeCatalog213 = new UpgradeCatalog213(injector);
    long clusterId = 1;

    final AmbariManagementController mockAmbariManagementController = easyMockSupport.createNiceMock(AmbariManagementController.class);
    final AlertDefinitionDAO mockAlertDefinitionDAO = easyMockSupport.createNiceMock(AlertDefinitionDAO.class);
    final Clusters mockClusters = easyMockSupport.createStrictMock(Clusters.class);
    final Cluster mockClusterExpected = easyMockSupport.createNiceMock(Cluster.class);
    final AlertDefinitionEntity mockJournalNodeProcessAlertDefinitionEntity = easyMockSupport.createNiceMock(AlertDefinitionEntity.class);
    final AlertDefinitionEntity mockHostDiskUsageAlertDefinitionEntity = easyMockSupport.createNiceMock(AlertDefinitionEntity.class);

    final String journalNodeProcessAlertSource = "{\"uri\":\"{{hdfs-site/dfs.journalnode.http-address}}\",\"default_port\":8480," +
        "\"type\":\"PORT\",\"reporting\":{\"ok\":{\"text\":\"TCP OK - {0:.3f}s response on port {1}\"}," +
        "\"warning\":{\"text\":\"TCP OK - {0:.3f}s response on port {1}\",\"value\":1.5}," +
        "\"critical\":{\"text\":\"Connection failed: {0} to {1}:{2}\",\"value\":5.0}}}";
    final String journalNodeProcessAlertSourceExpected = "{\"reporting\":{\"ok\":{\"text\":\"HTTP {0} response in {2:.3f}s\"}," +
        "\"warning\":{\"text\":\"HTTP {0} response from {1} in {2:.3f}s ({3})\"}," +
        "\"critical\":{\"text\":\"Connection failed to {1} ({3})\"}},\"type\":\"WEB\"," +
        "\"uri\":{\"http\":\"{{hdfs-site/dfs.journalnode.http-address}}\"," +
        "\"https\":\"{{hdfs-site/dfs.journalnode.https-address}}\"," +
        "\"kerberos_keytab\":\"{{hdfs-site/dfs.web.authentication.kerberos.keytab}}\","+
        "\"kerberos_principal\":\"{{hdfs-site/dfs.web.authentication.kerberos.principal}}\"," +
        "\"https_property\":\"{{hdfs-site/dfs.http.policy}}\"," +
        "\"https_property_value\":\"HTTPS_ONLY\",\"connection_timeout\":5.0}}";

    final Injector mockInjector = Guice.createInjector(new AbstractModule() {
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

    expect(mockAmbariManagementController.getClusters()).andReturn(mockClusters).once();
    expect(mockClusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", mockClusterExpected);
    }}).atLeastOnce();

    expect(mockClusterExpected.getClusterId()).andReturn(clusterId).anyTimes();

    expect(mockAlertDefinitionDAO.findByName(eq(clusterId), eq("journalnode_process"))).andReturn(mockJournalNodeProcessAlertDefinitionEntity).atLeastOnce();
    expect(mockAlertDefinitionDAO.findByName(eq(clusterId), eq("ambari_agent_disk_usage"))).andReturn(mockHostDiskUsageAlertDefinitionEntity).atLeastOnce();

    expect(mockJournalNodeProcessAlertDefinitionEntity.getSource()).andReturn(journalNodeProcessAlertSource).atLeastOnce();
    Assert.assertEquals(journalNodeProcessAlertSourceExpected, upgradeCatalog213.modifyJournalnodeProcessAlertSource(journalNodeProcessAlertSource));

    mockHostDiskUsageAlertDefinitionEntity.setDescription(eq("This host-level alert is triggered if the amount of disk space " +
        "used goes above specific thresholds. The default threshold values are 50% for WARNING and 80% for CRITICAL."));
    expectLastCall().atLeastOnce();
    mockHostDiskUsageAlertDefinitionEntity.setLabel(eq("Host Disk Usage"));
    expectLastCall().atLeastOnce();

    easyMockSupport.replayAll();
    mockInjector.getInstance(UpgradeCatalog213.class).updateAlertDefinitions();
    easyMockSupport.verifyAll();
  }

  @Test
  public void testUpdateKafkaConfigs() throws Exception {
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariManagementController mockAmbariManagementController = easyMockSupport.createNiceMock(AmbariManagementController.class);
    final ConfigurationResponse mockConfigurationResponse = easyMockSupport.createMock(ConfigurationResponse.class);
    final ConfigHelper mockConfigHelper = easyMockSupport.createMock(ConfigHelper.class);

    final Clusters mockClusters = easyMockSupport.createStrictMock(Clusters.class);
    final Cluster mockClusterExpected = easyMockSupport.createNiceMock(Cluster.class);

    final Map<String, String> propertiesKafkaEnv = new HashMap<String, String>() {
      {
        put("content", "test");
      }
    };
    Map<String, String> updates = Collections.singletonMap("content", "test\n\nexport KAFKA_KERBEROS_PARAMS=\"$KAFKA_KERBEROS_PARAMS {{kafka_kerberos_params}}");

    final Map<String, String> propertiesAmsEnv = new HashMap<String, String>() {
      {
        put("kafka.metrics.reporters", "{{kafka_metrics_reporters}}");
      }
    };
    final Map<String, Service> installedServices = new HashMap<String, Service>() {
      {
        put("KAFKA", null);
        put("AMBARI_METRICS", null);
      }
    };

    final Config mockAmsEnv = easyMockSupport.createNiceMock(Config.class);
    final Config mockKafkaEnv = easyMockSupport.createNiceMock(Config.class);

    final Injector mockInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(AmbariManagementController.class).toInstance(mockAmbariManagementController);
        bind(ConfigHelper.class).toInstance(mockConfigHelper);
        bind(Clusters.class).toInstance(mockClusters);
        bind(EntityManager.class).toInstance(entityManager);

        bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
      }
    });

    expect(mockAmbariManagementController.getClusters()).andReturn(mockClusters).once();
    expect(mockClusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", mockClusterExpected);
    }}).once();

    expect(mockClusterExpected.getServices()).andReturn(installedServices).atLeastOnce();
    expect(mockClusterExpected.getDesiredConfigByType("kafka-broker")).andReturn(mockAmsEnv).atLeastOnce();
    expect(mockAmsEnv.getProperties()).andReturn(propertiesAmsEnv).atLeastOnce();

    expect(mockClusterExpected.getDesiredConfigByType("kafka-env")).andReturn(mockKafkaEnv).atLeastOnce();
    expect(mockKafkaEnv.getProperties()).andReturn(propertiesKafkaEnv).atLeastOnce();

    UpgradeCatalog213 upgradeCatalog213 = createMockBuilder(UpgradeCatalog213.class)
        .withConstructor(Injector.class)
        .withArgs(mockInjector)
        .addMockedMethod("updateConfigurationPropertiesForCluster", Cluster.class, String.class,
            Map.class, boolean.class, boolean.class)
        .createMock();
    upgradeCatalog213.updateConfigurationPropertiesForCluster(mockClusterExpected,
        "kafka-env", updates, true, false);
    expectLastCall().once();

    expect(mockAmbariManagementController.createConfiguration(EasyMock.<ConfigurationRequest>anyObject())).andReturn(mockConfigurationResponse);

    easyMockSupport.replayAll();
    mockInjector.getInstance(UpgradeCatalog213.class).updateKafkaConfigs();
    easyMockSupport.verifyAll();
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
    mockInjector.getInstance(UpgradeCatalog213.class).updateFlumeEnvConfig();
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
        binder.bind(DaoUtils.class).toInstance(createNiceMock(DaoUtils.class));
        binder.bind(ClusterDAO.class).toInstance(clusterDAO);
        binder.bind(RepositoryVersionHelper.class).toInstance(createNiceMock(RepositoryVersionHelper.class));
        binder.bind(Clusters.class).toInstance(createNiceMock(Clusters.class));
        binder.bind(AmbariManagementController.class).toInstance(amc);
        binder.bind(AmbariMetaInfo.class).toInstance(metaInfo);
        binder.bind(StackManagerFactory.class).toInstance(createNiceMock(StackManagerFactory.class));
        binder.bind(StackDAO.class).toInstance(stackDAO);
        binder.bind(RepositoryVersionDAO.class).toInstance(repositoryVersionDAO);
        binder.bind(ClusterVersionDAO.class).toInstance(clusterVersionDAO);
        binder.bind(HostVersionDAO.class).toInstance(hostVersionDAO);
      }
    };

    Injector injector = Guice.createInjector(module);
    return injector.getInstance(UpgradeCatalog213.class);
  }

  @Test
  public void testUpdateZookeeperLog4jConfig() throws AmbariException {
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariManagementController mockAmbariManagementController = easyMockSupport.createNiceMock(AmbariManagementController.class);
    final Clusters mockClusters = easyMockSupport.createStrictMock(Clusters.class);
    final Cluster mockClusterExpected = easyMockSupport.createNiceMock(Cluster.class);
    final Map<String, String> propertiesZookeeperLog4j = new HashMap<String, String>() {
      {
        put("content", "log4j.rootLogger=INFO, CONSOLE");
      }
    };

    final Config mockZookeeperLog4j = easyMockSupport.createNiceMock(Config.class);
    expect(mockZookeeperLog4j.getProperties()).andReturn(propertiesZookeeperLog4j).once();

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

    expect(mockClusterExpected.getDesiredConfigByType("zookeeper-log4j")).andReturn(mockZookeeperLog4j).atLeastOnce();
    expect(mockZookeeperLog4j.getProperties()).andReturn(propertiesZookeeperLog4j).atLeastOnce();

    easyMockSupport.replayAll();
    mockInjector.getInstance(UpgradeCatalog213.class).updateZookeeperLog4j();
    easyMockSupport.verifyAll();

  }

  @Test
  public void testUpdateRangerEnvConfig() throws Exception {
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariManagementController mockAmbariManagementController = easyMockSupport.createNiceMock(AmbariManagementController.class);
    final Clusters mockClusters = easyMockSupport.createStrictMock(Clusters.class);
    final Cluster mockClusterExpected = easyMockSupport.createNiceMock(Cluster.class);
    final Map<String, String> propertiesHiveEnv = new HashMap<String, String>() {{
        put("hive_security_authorization", "Ranger");
    }};
    final Map<String, String> propertiesRangerHdfsPlugin = new HashMap<String, String>() {{
      put("ranger-hdfs-plugin-enabled", "Yes");
    }};
    final Map<String, String> propertiesRangerHbasePlugin = new HashMap<String, String>() {{
      put("ranger-hbase-plugin-enabled", "Yes");
    }};
    final Map<String, String> propertiesRangerKafkaPlugin = new HashMap<String, String>() {{
      put("ranger-kafka-plugin-enabled", "Yes");
    }};
    final Map<String, String> propertiesRangerYarnPlugin = new HashMap<String, String>() {{
      put("ranger-yarn-plugin-enabled", "No");
    }};

    final Config mockHiveEnvConf = easyMockSupport.createNiceMock(Config.class);
    final Config mockRangerHdfsPluginConf = easyMockSupport.createNiceMock(Config.class);
    final Config mockRangerHbasePluginConf = easyMockSupport.createNiceMock(Config.class);
    final Config mockRangerKafkaPluginConf = easyMockSupport.createNiceMock(Config.class);
    final Config mockRangerYarnPluginConf = easyMockSupport.createNiceMock(Config.class);
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
    expect(mockClusterExpected.getDesiredConfigByType("hive-env")).andReturn(mockHiveEnvConf).atLeastOnce();
    expect(mockClusterExpected.getDesiredConfigByType("ranger-hdfs-plugin-properties")).andReturn(mockRangerHdfsPluginConf).atLeastOnce();
    expect(mockClusterExpected.getDesiredConfigByType("ranger-hbase-plugin-properties")).andReturn(mockRangerHbasePluginConf).atLeastOnce();
    expect(mockClusterExpected.getDesiredConfigByType("ranger-kafka-plugin-properties")).andReturn(mockRangerKafkaPluginConf).atLeastOnce();
    expect(mockClusterExpected.getDesiredConfigByType("ranger-yarn-plugin-properties")).andReturn(mockRangerYarnPluginConf).atLeastOnce();

    expect(mockHiveEnvConf.getProperties()).andReturn(propertiesHiveEnv).times(2);
    expect(mockRangerHdfsPluginConf.getProperties()).andReturn(propertiesRangerHdfsPlugin).times(2);
    expect(mockRangerHbasePluginConf.getProperties()).andReturn(propertiesRangerHbasePlugin).times(2);
    expect(mockRangerKafkaPluginConf.getProperties()).andReturn(propertiesRangerKafkaPlugin).times(2);
    expect(mockRangerYarnPluginConf.getProperties()).andReturn(propertiesRangerYarnPlugin).times(2);

    easyMockSupport.replayAll();
    mockInjector.getInstance(UpgradeCatalog213.class).updateRangerEnvConfig();
    easyMockSupport.verifyAll();

  }

  @Test
  public void testGetSourceVersion() {
    final DBAccessor dbAccessor = createNiceMock(DBAccessor.class);
    UpgradeCatalog upgradeCatalog = getUpgradeCatalog(dbAccessor);
    Assert.assertEquals("2.1.2.1", upgradeCatalog.getSourceVersion());
  }

  @Test
  public void testGetTargetVersion() throws Exception {
    final DBAccessor dbAccessor = createNiceMock(DBAccessor.class);
    UpgradeCatalog upgradeCatalog = getUpgradeCatalog(dbAccessor);

    Assert.assertEquals("2.1.3", upgradeCatalog.getTargetVersion());
  }

  // *********** Inner Classes that represent sections of the DDL ***********
  // ************************************************************************

  /**
   * Verify that the upgrade table has two columns added to it.
   */
  class UpgradeSectionDDL implements SectionDDL {

    Capture<DBAccessor.DBColumnInfo> upgradeTablePackageNameColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
    Capture<DBAccessor.DBColumnInfo> upgradeTableUpgradeTypeColumnCapture = new Capture<DBAccessor.DBColumnInfo>();

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(DBAccessor dbAccessor) throws SQLException {
      // Add columns
      dbAccessor.addColumn(eq("upgrade"), capture(upgradeTablePackageNameColumnCapture));
      dbAccessor.addColumn(eq("upgrade"), capture(upgradeTableUpgradeTypeColumnCapture));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void verify(DBAccessor dbAccessor) throws SQLException {
      // Verification section
      DBAccessor.DBColumnInfo packageNameCol = upgradeTablePackageNameColumnCapture.getValue();
      Assert.assertEquals(String.class, packageNameCol.getType());
      Assert.assertEquals("upgrade_package", packageNameCol.getName());

      DBAccessor.DBColumnInfo upgradeTypeCol = upgradeTableUpgradeTypeColumnCapture.getValue();
      Assert.assertEquals(String.class, upgradeTypeCol.getType());
      Assert.assertEquals("upgrade_type", upgradeTypeCol.getName());
    }
  }

  @Test
  public void testShouldDDLsBeExecutedOnUpgrade() throws Exception {
    // GIVEN
    Injector mockedInjector = mocksControl.createMock(Injector.class);
    DBAccessor mockedDbAccessor = mocksControl.createMock(DBAccessor.class);
    DaoUtils mockedDaoUtils = mocksControl.createMock(DaoUtils.class);
    Configuration mockedConfiguration = mocksControl.createMock(Configuration.class);
    StackUpgradeUtil mockedStackUpgradeUtil = mocksControl.createMock(StackUpgradeUtil.class);

    Capture<String> capturedTableName = EasyMock.newCapture();
    Capture<String> capturedPKColumn = EasyMock.newCapture();
    Capture<List<DBAccessor.DBColumnInfo>> capturedColumns = EasyMock.newCapture();
    Capture<DBAccessor.DBColumnInfo> capturedColumn = EasyMock.newCapture();
    Capture<DBAccessor.DBColumnInfo> capturedHostRoleCommandColumn = EasyMock.newCapture();

    Capture<String> capturedBlueprintTableName = EasyMock.newCapture();
    Capture<DBAccessor.DBColumnInfo> capturedNewBlueprintColumn1 = EasyMock.newCapture();
    Capture<DBAccessor.DBColumnInfo> capturedNewBlueprintColumn2 = EasyMock.newCapture();

    Capture<DBAccessor.DBColumnInfo> stageSkipColumnCapture = EasyMock.newCapture();

    EasyMock.expect(mockedInjector.getInstance(DaoUtils.class)).andReturn(mockedDaoUtils);
    mockedInjector.injectMembers(anyObject(UpgradeCatalog.class));
    EasyMock.expect(mockedConfiguration.getDatabaseType()).andReturn(Configuration.DatabaseType.POSTGRES).anyTimes();
    EasyMock.expect(mockedConfiguration.getDatabaseUser()).andReturn("ambari");
    EasyMock.expect(mockedConfiguration.getServerJDBCPostgresSchemaName()).andReturn("fo");


    mockedDbAccessor.executeQuery("ALTER SCHEMA fo OWNER TO \"ambari\";");
    mockedDbAccessor.executeQuery("ALTER ROLE \"ambari\" SET search_path to 'fo';");

    // executeUpgradeDDLUpdates
    mockedDbAccessor.addColumn(eq("upgrade"), capture(capturedColumn));
    mockedDbAccessor.addColumn(eq("upgrade"), capture(capturedColumn));
    mockedDbAccessor.addColumn(eq("upgrade"), capture(capturedColumn));

    // addKerberosDescriptorTable
    mockedDbAccessor.createTable(capture(capturedTableName), capture(capturedColumns), capture(capturedPKColumn));
    mockedDbAccessor.alterColumn(eq("host_role_command"), capture(capturedHostRoleCommandColumn));

    mockedDbAccessor.addColumn(capture(capturedBlueprintTableName), capture(capturedNewBlueprintColumn1));
    mockedDbAccessor.addColumn(capture(capturedBlueprintTableName), capture(capturedNewBlueprintColumn2));

    mockedDbAccessor.addColumn(eq("stage"), capture(stageSkipColumnCapture));

    mocksControl.replay();

    UpgradeCatalog213 testSubject = new UpgradeCatalog213(mockedInjector);
    EasyMockSupport.injectMocks(testSubject);

    //todo refactor the DI approach, don't directly access these members!!!
    testSubject.stackUpgradeUtil = mockedStackUpgradeUtil;
    testSubject.dbAccessor = mockedDbAccessor;
    testSubject.configuration = mockedConfiguration;

    // WHEN
    testSubject.upgradeSchema();

    // THEN
    Assert.assertEquals("The table name is wrong!", "kerberos_descriptor", capturedTableName.getValue());
    Assert.assertEquals("The primary key is wrong!", "kerberos_descriptor_name", capturedPKColumn.getValue());
    Assert.assertTrue("Ther number of columns is wrong!", capturedColumns.getValue().size() == 2);

    Assert.assertEquals("The table name is wrong!", "blueprint", capturedBlueprintTableName.getValue());

    Assert.assertEquals("The column name is wrong!", "security_type", capturedNewBlueprintColumn1.getValue().getName());
    Assert.assertEquals("The column name is wrong!", "security_descriptor_reference", capturedNewBlueprintColumn2
      .getValue().getName());

    Assert.assertEquals("The column name is wrong!", "supports_auto_skip_failure",
        stageSkipColumnCapture.getValue().getName());
  }

  @Test
  public void testUpdateHiveConfig() throws Exception {
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariManagementController mockAmbariManagementController = easyMockSupport.createNiceMock(AmbariManagementController.class);
    final Clusters mockClusters = easyMockSupport.createStrictMock(Clusters.class);
    final Cluster mockClusterExpected = easyMockSupport.createNiceMock(Cluster.class);
    final Map<String, String> propertiesHiveSite = new HashMap<String, String>() {{
      put("hive.server2.logging.operation.log.location", "${system:java.io.tmpdir}/${system:user.name}/operation_logs");
    }};
    final Map<String, String> propertiesHiveSiteExpected = new HashMap<String, String>() {{
      put("hive.server2.logging.operation.log.location", "/tmp/hive/operation_logs");
    }};
    final Map<String, String> propertiesHiveEnv = new HashMap<String, String>() {{
      put("content", "test content");
    }};
    final Config hiveSiteConf = easyMockSupport.createNiceMock(Config.class);
    final Config hiveEnvConf = easyMockSupport.createNiceMock(Config.class);
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
    expect(mockClusterExpected.getDesiredConfigByType("hive-site")).andReturn(hiveSiteConf).atLeastOnce();
    expect(mockClusterExpected.getDesiredConfigByType("hive-env")).andReturn(hiveEnvConf).atLeastOnce();

    expect(hiveSiteConf.getProperties()).andReturn(propertiesHiveSite).once();
    expect(hiveEnvConf.getProperties()).andReturn(propertiesHiveEnv).once();

    UpgradeCatalog213 upgradeCatalog213 = createMockBuilder(UpgradeCatalog213.class)
            .withConstructor(Injector.class)
            .withArgs(mockInjector)
            .addMockedMethod("updateConfigurationPropertiesForCluster", Cluster.class, String.class,
                    Map.class, boolean.class, boolean.class)
            .createMock();
    upgradeCatalog213.updateConfigurationPropertiesForCluster(mockClusterExpected,
            "hive-site", propertiesHiveSiteExpected, true, false);
    expectLastCall().once();
    upgradeCatalog213.updateConfigurationPropertiesForCluster(mockClusterExpected,
            "hive-env", propertiesHiveEnv, true, true);
    expectLastCall().once();

    easyMockSupport.replayAll();
    replay(upgradeCatalog213);
    upgradeCatalog213.updateHiveConfig();
    easyMockSupport.verifyAll();

  }

  @Test
  public void testUpdateHiveEnvContentHDP23() throws Exception {
    UpgradeCatalog213 upgradeCatalog213 = new UpgradeCatalog213(injector);
    String testContent = "# The heap size of the jvm stared by hive shell script can be controlled via:\n" +
            "\n" +
            "# Larger heap size may be required when running queries over large number of files or partitions.\n";
    String expectedResult = "# The heap size of the jvm stared by hive shell script can be controlled via:\n" +
            "\n" +
            "if [ \"$SERVICE\" = \"metastore\" ]; then\n" +
            "  export HADOOP_HEAPSIZE={{hive_metastore_heapsize}} # Setting for HiveMetastore\n" +
            "else\n" +
            "  export HADOOP_HEAPSIZE={{hive_heapsize}} # Setting for HiveServer2 and Client\n" +
            "fi\n" +
            "\n" +
            "export HADOOP_CLIENT_OPTS=\"$HADOOP_CLIENT_OPTS  -Xmx${HADOOP_HEAPSIZE}m\"\n" +
            "\n" +
            "# Larger heap size may be required when running queries over large number of files or partitions.\n";
    Assert.assertEquals(expectedResult, upgradeCatalog213.updateHiveEnvContentHDP23(testContent));
  }


  @Test
  public void testUpdateHiveEnvContent() throws Exception {
    UpgradeCatalog213 upgradeCatalog213 = new UpgradeCatalog213(injector);
    // Test first case
    String testContent = "# The heap size of the jvm stared by hive shell script can be controlled via:\n" +
            "\n" +
            "if [ \"$SERVICE\" = \"metastore\" ]; then\n" +
            "  export HADOOP_HEAPSIZE=\"{{hive_metastore_heapsize}}\"\n" +
            "else\n" +
            "  export HADOOP_HEAPSIZE=\"{{hive_heapsize}}\"\n" +
            "fi\n" +
            "\n" +
            "export HADOOP_CLIENT_OPTS=\"-Xmx${HADOOP_HEAPSIZE}m $HADOOP_CLIENT_OPTS\"\n" +
            "\n" +
            "# Larger heap size may be required when running queries over large number of files or partitions.\n";
    String expectedResult = "# The heap size of the jvm stared by hive shell script can be controlled via:\n" +
            "\n" +
            "if [ \"$SERVICE\" = \"metastore\" ]; then\n" +
            "  export HADOOP_HEAPSIZE=\"{{hive_metastore_heapsize}}\"\n" +
            "else\n" +
            "  export HADOOP_HEAPSIZE=\"{{hive_heapsize}}\"\n" +
            "fi\n" +
            "\n" +
            "export HADOOP_CLIENT_OPTS=\"$HADOOP_CLIENT_OPTS  -Xmx${HADOOP_HEAPSIZE}m\"\n" +
            "\n" +
            "# Larger heap size may be required when running queries over large number of files or partitions.\n";
    Assert.assertEquals(expectedResult, upgradeCatalog213.updateHiveEnvContent(testContent));
    // Test second case
    testContent = "# The heap size of the jvm stared by hive shell script can be controlled via:\n" +
            "export SERVICE=$SERVICE\n" +
            "if [ \"$SERVICE\" = \"metastore\" ]; then\n" +
            "  export HADOOP_HEAPSIZE=\"{{hive_metastore_heapsize}}\"\n" +
            "else\n" +
            "  export HADOOP_HEAPSIZE=\"{{hive_heapsize}}\"\n" +
            "fi\n" +
            "\n" +
            "# Larger heap size may be required when running queries over large number of files or partitions.\n";
    expectedResult = "# The heap size of the jvm stared by hive shell script can be controlled via:\n" +
            "export SERVICE=$SERVICE\n" +
            "if [ \"$SERVICE\" = \"metastore\" ]; then\n" +
            "  export HADOOP_HEAPSIZE=\"{{hive_metastore_heapsize}}\"\n" +
            "else\n" +
            "  export HADOOP_HEAPSIZE={{hive_heapsize}} # Setting for HiveServer2 and Client\n" +
            "fi\n" +
            "\n" +
            "export HADOOP_CLIENT_OPTS=\"$HADOOP_CLIENT_OPTS  -Xmx${HADOOP_HEAPSIZE}m\"\n" +
            "# Larger heap size may be required when running queries over large number of files or partitions.\n";
    Assert.assertEquals(expectedResult, upgradeCatalog213.updateHiveEnvContent(testContent));
  }

  @Test
  public void testUpdateAccumuloConfigs() throws Exception {
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariManagementController mockAmbariManagementController = easyMockSupport.createNiceMock(AmbariManagementController.class);
    final Clusters mockClusters = easyMockSupport.createStrictMock(Clusters.class);
    final Cluster mockClusterExpected = easyMockSupport.createNiceMock(Cluster.class);

    // We start with no client properties (< 2.1.3).
    final Map<String, String> originalClientProperties = new HashMap<String, String>();
    // And should get the following property on upgrade.
    final Map<String, String> updatedClientProperties = new HashMap<String, String>() {
      {
        put("kerberos.server.primary", "{{bare_accumulo_principal}}");
      }
    };
    
    final Config clientConfig = easyMockSupport.createNiceMock(Config.class);

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
    }}).once();

    // Enable KERBEROS
    expect(mockClusterExpected.getSecurityType()).andReturn(SecurityType.KERBEROS).once();
    // Mock out our empty original properties
    expect(mockClusterExpected.getDesiredConfigByType("client")).andReturn(clientConfig).atLeastOnce();
    expect(clientConfig.getProperties()).andReturn(originalClientProperties).atLeastOnce();

    UpgradeCatalog213 upgradeCatalog213 = createMockBuilder(UpgradeCatalog213.class)
            .withConstructor(Injector.class)
            .withArgs(mockInjector)
            .addMockedMethod("updateConfigurationPropertiesForCluster", Cluster.class, String.class,
                    Map.class, boolean.class, boolean.class)
            .createMock();
    // Verify that we get this method called with the updated properties
    upgradeCatalog213.updateConfigurationPropertiesForCluster(mockClusterExpected,
            "client", updatedClientProperties, true, false);
    expectLastCall().once();

    // Run it
    easyMockSupport.replayAll();
    replay(upgradeCatalog213);
    upgradeCatalog213.updateAccumuloConfigs();
    easyMockSupport.verifyAll();
  }
}
