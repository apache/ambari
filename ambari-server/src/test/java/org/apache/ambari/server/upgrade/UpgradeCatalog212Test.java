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

import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.persist.PersistService;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.DBAccessor.DBColumnInfo;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.stack.OsFamily;
import org.easymock.Capture;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.MockType;
import org.easymock.TestSubject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
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

/**
 * {@link org.apache.ambari.server.upgrade.UpgradeCatalog212} unit tests.
 */
public class UpgradeCatalog212Test {

  private static final String TOPOLOGY_REQUEST_TABLE = "topology_request";
  private static final String TOPOLOGY_REQUEST_CLUSTER_NAME_COLUMN = "cluster_name";

  private Injector injector;

  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);

  @Mock(type = MockType.STRICT)
  private Provider<EntityManager> entityManagerProvider;

  @Mock(type = MockType.NICE)
  private EntityManager entityManager;

  @Mock(type = MockType.NICE)
  private DBAccessor dbAccessor;

  @Mock
  private Injector mockedInjector;

  @Mock(type = MockType.NICE)
  private Connection connection;

  @Mock
  private Statement statement;

  @Mock
  private ResultSet resultSet;

  @TestSubject
  private UpgradeCatalog212 testSubject = new UpgradeCatalog212();


  private UpgradeCatalogHelper upgradeCatalogHelper;
  private StackEntity desiredStackEntity;


  // This method to be called only when an IOC is needed - typically by functional tests
  public void setupIoCContext() {
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
    if (injector != null) {
      injector.getInstance(PersistService.class).stop();
    }
  }


  @Test
  public void testFinilizeTopologyDDL() throws Exception {
    setupIoCContext();
    final DBAccessor dbAccessor = createNiceMock(DBAccessor.class);
    dbAccessor.dropColumn(eq("topology_request"), eq("cluster_name"));
    dbAccessor.setColumnNullable(eq("topology_request"), eq("cluster_id"), eq(false));
    dbAccessor.addFKConstraint(eq("topology_request"), eq("FK_topology_request_cluster_id"), eq("cluster_id"),
      eq("clusters"), eq("cluster_id"), eq(false));

    replay(dbAccessor);
    Module module = new Module() {
      @Override
      public void configure(Binder binder) {
        binder.bind(DBAccessor.class).toInstance(dbAccessor);
        binder.bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
      }
    };

    Injector injector = Guice.createInjector(module);
    UpgradeCatalog212 upgradeCatalog212 = injector.getInstance(UpgradeCatalog212.class);
    upgradeCatalog212.finilizeTopologyDDL();

    verify(dbAccessor);
  }

  @Test
  public void testExecuteDDLUpdates() throws Exception {
    setupIoCContext();
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
    HostRoleCommandDDL hostRoleCommandDDL = new HostRoleCommandDDL();

    // Execute any DDL schema changes
    hostRoleCommandDDL.execute(dbAccessor);

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
    hostRoleCommandDDL.verify(dbAccessor);
  }

  @Test
  public void testExecuteDMLUpdates() throws Exception {
    setupIoCContext();
    Method addMissingConfigs = UpgradeCatalog212.class.getDeclaredMethod("addMissingConfigs");
    Method addNewConfigurationsFromXml = AbstractUpgradeCatalog.class.getDeclaredMethod("addNewConfigurationsFromXml");

    UpgradeCatalog212 upgradeCatalog212 = createMockBuilder(UpgradeCatalog212.class)
            .addMockedMethod(addNewConfigurationsFromXml)
            .addMockedMethod(addMissingConfigs)
            .createMock();

    upgradeCatalog212.addNewConfigurationsFromXml();
    expectLastCall().once();

    upgradeCatalog212.addMissingConfigs();
    expectLastCall().once();

    replay(upgradeCatalog212);

    upgradeCatalog212.executeDMLUpdates();

    verify(upgradeCatalog212);
  }

  @Test
  public void testUpdateHBaseAdnClusterConfigs() throws Exception {
    setupIoCContext();
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariManagementController mockAmbariManagementController = easyMockSupport.createNiceMock(AmbariManagementController.class);
    final ConfigHelper mockConfigHelper = easyMockSupport.createMock(ConfigHelper.class);

    final Clusters mockClusters = easyMockSupport.createStrictMock(Clusters.class);
    final Cluster mockClusterExpected = easyMockSupport.createNiceMock(Cluster.class);


    final Map<String, String> propertiesHbaseSite = new HashMap<String, String>() {
      {
        put("hbase.bucketcache.size", "1024m");
      }
    };

    final Map<String, String> propertiesHbaseEnv = new HashMap<String, String>() {
      {
        put("override_hbase_uid", "false");
      }
    };

    final Config mockHbaseEnv = easyMockSupport.createNiceMock(Config.class);
    expect(mockHbaseEnv.getProperties()).andReturn(propertiesHbaseEnv).once();
    final Config mockHbaseSite = easyMockSupport.createNiceMock(Config.class);
    expect(mockHbaseSite.getProperties()).andReturn(propertiesHbaseSite).once();
    final Config mockClusterEnv = easyMockSupport.createNiceMock(Config.class);

    final Map<String, String> propertiesExpectedHbaseEnv = new HashMap<String, String>();
    final Map<String, String> propertiesExpectedClusterEnv = new HashMap<String, String>() {{
      put("override_uid", "false");
    }};
    final Map<String, String> propertiesExpectedHbaseSite = new HashMap<String, String>() {{
      put("hbase.bucketcache.size", "1024");
    }};

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
    expect(mockClusterExpected.getDesiredConfigByType("hbase-env")).andReturn(mockHbaseEnv).atLeastOnce();
    expect(mockClusterExpected.getDesiredConfigByType("hbase-site")).andReturn(mockHbaseSite).atLeastOnce();

    expect(mockClusterEnv.getProperties()).andReturn(propertiesExpectedClusterEnv).atLeastOnce();
    expect(mockHbaseEnv.getProperties()).andReturn(propertiesExpectedHbaseEnv).atLeastOnce();
    expect(mockHbaseSite.getProperties()).andReturn(propertiesExpectedHbaseSite).atLeastOnce();

    easyMockSupport.replayAll();
    mockInjector.getInstance(UpgradeCatalog212.class).updateHbaseAndClusterConfigurations();
    easyMockSupport.verifyAll();

  }

  @Test
  public void testUpdateHiveConfigs() throws Exception {
    setupIoCContext();
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariManagementController  mockAmbariManagementController = easyMockSupport.createNiceMock(AmbariManagementController.class);
    final ConfigHelper mockConfigHelper = easyMockSupport.createMock(ConfigHelper.class);

    final Clusters mockClusters = easyMockSupport.createStrictMock(Clusters.class);
    final Cluster mockClusterExpected = easyMockSupport.createNiceMock(Cluster.class);
    final Config mockHiveEnv = easyMockSupport.createNiceMock(Config.class);
    final Config mockHiveSite = easyMockSupport.createNiceMock(Config.class);

    final Map<String, String> propertiesExpectedHiveEnv = new HashMap<String, String>();
    final Map<String, String> propertiesExpectedHiveSite = new HashMap<String, String>() {{
      put("hive.heapsize", "512");
      put("hive.server2.custom.authentication.class", "");
    }};

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

    expect(mockClusterExpected.getDesiredConfigByType("hive-site")).andReturn(mockHiveSite).atLeastOnce();
    expect(mockHiveSite.getProperties()).andReturn(propertiesExpectedHiveSite).atLeastOnce();

    StackId stackId = new StackId("HDP-2.2");
    expect(mockClusterExpected.getCurrentStackVersion()).andReturn(stackId).atLeastOnce();

    easyMockSupport.replayAll();
    mockInjector.getInstance(UpgradeCatalog212.class).updateHiveConfigs();
    easyMockSupport.verifyAll();

  }

  @Test
  public void testUpdateOozieConfigs() throws Exception {
    setupIoCContext();
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariManagementController  mockAmbariManagementController = easyMockSupport.createNiceMock(AmbariManagementController.class);
    final ConfigHelper mockConfigHelper = easyMockSupport.createMock(ConfigHelper.class);

    final Clusters mockClusters = easyMockSupport.createStrictMock(Clusters.class);
    final Cluster mockClusterExpected = easyMockSupport.createNiceMock(Cluster.class);
    final Config mockOozieEnv = easyMockSupport.createNiceMock(Config.class);

    final Map<String, String> propertiesExpectedOozieEnv = new HashMap<String, String>() {{
      put("oozie_hostname", "");
      put("oozie_database", "123");
    }};

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

    expect(mockClusterExpected.getDesiredConfigByType("oozie-env")).andReturn(mockOozieEnv).atLeastOnce();
    expect(mockOozieEnv.getProperties()).andReturn(propertiesExpectedOozieEnv).atLeastOnce();

    easyMockSupport.replayAll();
    mockInjector.getInstance(UpgradeCatalog212.class).updateOozieConfigs();
    easyMockSupport.verifyAll();
  }

  @Test
  public void testUpdateHiveEnvContent() throws Exception {
    setupIoCContext();
    final Injector mockInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
      }
    });
    String testContent = " if [ \"$SERVICE\" = \"cli\" ]; then\n" +
            "   if [ -z \"$DEBUG\" ]; then\n" +
            "     export HADOOP_OPTS=\"$HADOOP_OPTS -XX:NewRatio=12 -Xms10m -XX:MaxHeapFreeRatio=40 -XX:MinHeapFreeRatio=15 -XX:+UseParNewGC -XX:-UseGCOverheadLimit\"\n" +
            "   else\n" +
            "     export HADOOP_OPTS=\"$HADOOP_OPTS -XX:NewRatio=12 -Xms10m -XX:MaxHeapFreeRatio=40 -XX:MinHeapFreeRatio=15 -XX:-UseGCOverheadLimit\"\n" +
            "   fi\n" +
            " fi\n" +
            "\n" +
            "export HADOOP_HEAPSIZE=\"{{hive_heapsize}}\"\n" +
            "export HADOOP_CLIENT_OPTS=\"-Xmx${HADOOP_HEAPSIZE}m $HADOOP_CLIENT_OPTS\"\n" +
            "\n" +
            "# Set HADOOP_HOME to point to a specific hadoop install directory\n" +
            "HADOOP_HOME=${HADOOP_HOME:-{{hadoop_home}}}\n";
    String expectedResult = " if [ \"$SERVICE\" = \"cli\" ]; then\n" +
            "   if [ -z \"$DEBUG\" ]; then\n" +
            "     export HADOOP_OPTS=\"$HADOOP_OPTS -XX:NewRatio=12 -Xms10m -XX:MaxHeapFreeRatio=40 -XX:MinHeapFreeRatio=15 -XX:+UseParNewGC -XX:-UseGCOverheadLimit\"\n" +
            "   else\n" +
            "     export HADOOP_OPTS=\"$HADOOP_OPTS -XX:NewRatio=12 -Xms10m -XX:MaxHeapFreeRatio=40 -XX:MinHeapFreeRatio=15 -XX:-UseGCOverheadLimit\"\n" +
            "   fi\n" +
            " fi\n" +
            "\n" +
            "\n" +
            "\n" +
            "# Set HADOOP_HOME to point to a specific hadoop install directory\n" +
            "HADOOP_HOME=${HADOOP_HOME:-{{hadoop_home}}}\n";
    assertEquals(expectedResult, mockInjector.getInstance(UpgradeCatalog212.class).updateHiveEnvContent(testContent));
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
    return injector.getInstance(UpgradeCatalog212.class);
  }

  @Test
  public void testGetSourceVersion() {
    final DBAccessor dbAccessor = createNiceMock(DBAccessor.class);
    UpgradeCatalog upgradeCatalog = getUpgradeCatalog(dbAccessor);
    Assert.assertEquals("2.1.1", upgradeCatalog.getSourceVersion());
  }

  @Test
  public void testGetTargetVersion() throws Exception {
    final DBAccessor dbAccessor = createNiceMock(DBAccessor.class);
    UpgradeCatalog upgradeCatalog = getUpgradeCatalog(dbAccessor);

    Assert.assertEquals("2.1.2", upgradeCatalog.getTargetVersion());
  }

  /**
   * Verify alert changes
   */
  class HostRoleCommandDDL implements SectionDDL {
    HashMap<String, Capture<DBColumnInfo>> captures;

    public HostRoleCommandDDL() {
      captures = new HashMap<String, Capture<DBColumnInfo>>();

      Capture<DBAccessor.DBColumnInfo> hostRoleCommandAutoSkipColumnCapture = new Capture<DBAccessor.DBColumnInfo>();

      captures.put("host_role_command", hostRoleCommandAutoSkipColumnCapture);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(DBAccessor dbAccessor) throws SQLException {
      Capture<DBColumnInfo> hostRoleCommandAuotSkipColumnCapture = captures.get(
          "host_role_command");

      dbAccessor.addColumn(eq("host_role_command"),
          capture(hostRoleCommandAuotSkipColumnCapture));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void verify(DBAccessor dbAccessor) throws SQLException {
      verifyHostRoleCommandSkipCapture(captures.get("host_role_command"));
    }

    private void verifyHostRoleCommandSkipCapture(
        Capture<DBAccessor.DBColumnInfo> hostRoleCommandAuotSkipColumnCapture) {
      DBColumnInfo clusterIdColumn = hostRoleCommandAuotSkipColumnCapture.getValue();
      Assert.assertEquals(Integer.class, clusterIdColumn.getType());
      Assert.assertEquals("auto_skip_on_failure", clusterIdColumn.getName());
    }
  }

  @Test
  public void testShouldSkipPreDMLLogicIfClusterNameColumnDoesNotExist() throws Exception {
    // GIVEN
    reset(dbAccessor);
    Capture<String> tableNameCaptor = newCapture();
    Capture<String> columnNameCaptor = newCapture();

    // the column used by the logic is already deleted
    // this could happen as a result of previously running the update
    expect(dbAccessor.tableHasColumn(capture(tableNameCaptor), capture(columnNameCaptor))).andReturn(false);
    replay(dbAccessor);

    // WHEN
    testSubject.executePreDMLUpdates();

    // THEN
    Assert.assertNotNull("The table name hasn't been captured", tableNameCaptor.getValue());
    Assert.assertEquals("The table name is not as expected", TOPOLOGY_REQUEST_TABLE, tableNameCaptor.getValue());

    Assert.assertNotNull("The column name hasn't been captured", columnNameCaptor.getValue());
    Assert.assertEquals("The column name is not as expected", TOPOLOGY_REQUEST_CLUSTER_NAME_COLUMN,
        columnNameCaptor.getValue());
  }


  @Test
  public void testShouldPerformPreDMLLogicIfClusterNameColumnExists() throws Exception {
    // GIVEN
    reset(dbAccessor);
    expect(dbAccessor.getConnection()).andReturn(connection).anyTimes();
    expect(connection.createStatement()).andReturn(statement);

    Capture<String> tableNameCaptor = newCapture();
    Capture<String> columnNameCaptor = newCapture();

    expect(dbAccessor.tableHasColumn(capture(tableNameCaptor), capture(columnNameCaptor))).andReturn(true);

    expect(statement.executeQuery(anyString())).andReturn(resultSet);
    statement.close();

    expect(resultSet.next()).andReturn(false);
    resultSet.close();

    replay(dbAccessor, connection, statement, resultSet);

    // WHEN
    testSubject.executePreDMLUpdates();

    // THEN
    Assert.assertNotNull("The table name hasn't been captured", tableNameCaptor.getValue());
    Assert.assertEquals("The table name is not as expected", TOPOLOGY_REQUEST_TABLE, tableNameCaptor.getValue());

    Assert.assertNotNull("The column name hasn't been captured", columnNameCaptor.getValue());
    Assert.assertEquals("The column name is not as expected", TOPOLOGY_REQUEST_CLUSTER_NAME_COLUMN,
        columnNameCaptor.getValue());

    verify(dbAccessor, statement, resultSet);
  }

}
