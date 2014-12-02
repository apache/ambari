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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.DBAccessor.DBColumnInfo;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.ClusterServiceDAO;
import org.apache.ambari.server.orm.dao.HostComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.HostComponentStateDAO;
import org.apache.ambari.server.orm.dao.ServiceComponentDesiredStateDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntityPK;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.orm.entities.HostComponentStateEntityPK;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntityPK;
import org.apache.ambari.server.state.SecurityState;
import org.easymock.Capture;
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
 * {@link UpgradeCatalog200} unit tests.
 */
public class UpgradeCatalog200Test {
  private final String CLUSTER_NAME = "c1";
  private final String HOST_NAME = "h1";
  private final String DESIRED_STACK_VERSION = "{\"stackName\":\"HDP\",\"stackVersion\":\"2.0.6\"}";

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
    ResultSet resultSet = createNiceMock(ResultSet.class);

    expect(configuration.getDatabaseUrl()).andReturn(Configuration.JDBC_IN_MEMORY_URL).anyTimes();

    Capture<DBAccessor.DBColumnInfo> alertDefinitionIgnoreColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
    Capture<DBAccessor.DBColumnInfo> alertDefinitionDescriptionColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
    Capture<DBAccessor.DBColumnInfo> hostComponentStateColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
    Capture<DBAccessor.DBColumnInfo> hostComponentStateSecurityStateColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
    Capture<DBAccessor.DBColumnInfo> hostComponentDesiredStateSecurityStateColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
    Capture<DBAccessor.DBColumnInfo> serviceDesiredStateSecurityStateColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
    Capture<List<DBAccessor.DBColumnInfo>> clusterVersionCapture = new Capture<List<DBAccessor.DBColumnInfo>>();
    Capture<List<DBAccessor.DBColumnInfo>> hostVersionCapture = new Capture<List<DBAccessor.DBColumnInfo>>();
    Capture<DBAccessor.DBColumnInfo> valueColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
    Capture<DBAccessor.DBColumnInfo> dataValueColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
    Capture<List<DBAccessor.DBColumnInfo>> alertTargetStatesCapture = new Capture<List<DBAccessor.DBColumnInfo>>();

    Capture<List<DBAccessor.DBColumnInfo>> upgradeCapture = new Capture<List<DBAccessor.DBColumnInfo>>();
    Capture<List<DBAccessor.DBColumnInfo>> upgradeGroupCapture = new Capture<List<DBAccessor.DBColumnInfo>>();
    Capture<List<DBAccessor.DBColumnInfo>> upgradeItemCapture = new Capture<List<DBAccessor.DBColumnInfo>>();

    // Alert Definition
    dbAccessor.addColumn(eq("alert_definition"),
        capture(alertDefinitionIgnoreColumnCapture));

    dbAccessor.addColumn(eq("alert_definition"),
        capture(alertDefinitionDescriptionColumnCapture));

    dbAccessor.createTable(eq("alert_target_states"),
        capture(alertTargetStatesCapture), eq("target_id"));

    // Host Component State
    dbAccessor.addColumn(eq("hostcomponentstate"),
        capture(hostComponentStateColumnCapture));

    // Host Component State: security State
    dbAccessor.addColumn(eq("hostcomponentstate"),
        capture(hostComponentStateSecurityStateColumnCapture));

    // Host Component Desired State: security State
    dbAccessor.addColumn(eq("hostcomponentdesiredstate"),
        capture(hostComponentDesiredStateSecurityStateColumnCapture));

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


    setViewInstancePropertyExpectations(dbAccessor, valueColumnCapture);
    setViewInstanceDataExpectations(dbAccessor, dataValueColumnCapture);

    replay(dbAccessor, configuration, resultSet);

    AbstractUpgradeCatalog upgradeCatalog = getUpgradeCatalog(dbAccessor);
    Class<?> c = AbstractUpgradeCatalog.class;
    Field f = c.getDeclaredField("configuration");
    f.setAccessible(true);
    f.set(upgradeCatalog, configuration);

    upgradeCatalog.executeDDLUpdates();
    verify(dbAccessor, configuration, resultSet);

    // verify columns for alert_definition
    verifyAlertDefinitionIgnoreColumn(alertDefinitionIgnoreColumnCapture);
    verifyAlertDefinitionDescriptionColumn(alertDefinitionDescriptionColumnCapture);

    // verify new table for alert target states
    verifyAlertTargetStatesTable(alertTargetStatesCapture);

    // Verify added column in hostcomponentstate table
    DBAccessor.DBColumnInfo upgradeStateColumn = hostComponentStateColumnCapture.getValue();
    assertEquals("upgrade_state", upgradeStateColumn.getName());
    assertEquals(32, (int) upgradeStateColumn.getLength());
    assertEquals(String.class, upgradeStateColumn.getType());
    assertEquals("NONE", upgradeStateColumn.getDefaultValue());
    assertFalse(upgradeStateColumn.isNullable());

    // verify security_state columns
    verifyComponentSecurityStateColumn(hostComponentStateSecurityStateColumnCapture);
    verifyComponentSecurityStateColumn(hostComponentDesiredStateSecurityStateColumnCapture);
    verifyServiceSecurityStateColumn(serviceDesiredStateSecurityStateColumnCapture);

    // Verify capture group sizes
    assertEquals(8, clusterVersionCapture.getValue().size());
    assertEquals(5, hostVersionCapture.getValue().size());

    assertViewInstancePropertyColumns(valueColumnCapture);
    assertViewInstanceDataColumns(dataValueColumnCapture);

    assertEquals(4, upgradeCapture.getValue().size());
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

    UpgradeCatalog200 upgradeCatalog = createMockBuilder(
        UpgradeCatalog200.class).addMockedMethod(removeNagiosService).createMock();

    upgradeCatalog.removeNagiosService();
    expectLastCall().once();

    replay(upgradeCatalog);

    upgradeCatalog.executeDMLUpdates();

    verify(upgradeCatalog);
  }

  /**
   * Tests that Nagios is correctly removed.
   *
   * @throws Exception
   */
  @Test
  public void testDeleteNagiosService() throws Exception {
    final ClusterEntity clusterEntity = upgradeCatalogHelper.createCluster(
        injector, CLUSTER_NAME, DESIRED_STACK_VERSION);

    final ClusterServiceEntity clusterServiceEntityNagios = upgradeCatalogHelper.addService(
        injector, clusterEntity, "NAGIOS", DESIRED_STACK_VERSION);

    final HostEntity hostEntity = upgradeCatalogHelper.createHost(injector,
        clusterEntity, HOST_NAME);

    upgradeCatalogHelper.addComponent(injector, clusterEntity,
        clusterServiceEntityNagios, hostEntity, "NAGIOS_SERVER",
        DESIRED_STACK_VERSION);

    UpgradeCatalog200 upgradeCatalog200 = injector.getInstance(UpgradeCatalog200.class);

    ServiceComponentDesiredStateDAO serviceComponentDesiredStateDAO = injector.getInstance(ServiceComponentDesiredStateDAO.class);
    ServiceComponentDesiredStateEntityPK pkNagiosServer = new ServiceComponentDesiredStateEntityPK();
    pkNagiosServer.setComponentName("NAGIOS_SERVER");
    pkNagiosServer.setClusterId(clusterEntity.getClusterId());
    pkNagiosServer.setServiceName("NAGIOS");
    ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity = serviceComponentDesiredStateDAO.findByPK(pkNagiosServer);
    assertNotNull(serviceComponentDesiredStateEntity);

    HostComponentDesiredStateDAO hostComponentDesiredStateDAO = injector.getInstance(HostComponentDesiredStateDAO.class);
    HostComponentDesiredStateEntityPK hcDesiredStateEntityPk = new HostComponentDesiredStateEntityPK();
    hcDesiredStateEntityPk.setServiceName("NAGIOS");
    hcDesiredStateEntityPk.setClusterId(clusterEntity.getClusterId());
    hcDesiredStateEntityPk.setComponentName("NAGIOS_SERVER");
    hcDesiredStateEntityPk.setHostName(HOST_NAME);
    HostComponentDesiredStateEntity hcDesiredStateEntity = hostComponentDesiredStateDAO.findByPK(hcDesiredStateEntityPk);
    assertNotNull(hcDesiredStateEntity);

    HostComponentStateDAO hostComponentStateDAO = injector.getInstance(HostComponentStateDAO.class);
    HostComponentStateEntityPK hcStateEntityPk = new HostComponentStateEntityPK();
    hcStateEntityPk.setServiceName("NAGIOS");
    hcStateEntityPk.setClusterId(clusterEntity.getClusterId());
    hcStateEntityPk.setComponentName("NAGIOS_SERVER");
    hcStateEntityPk.setHostName(HOST_NAME);
    HostComponentStateEntity hcStateEntity = hostComponentStateDAO.findByPK(hcStateEntityPk);
    assertNotNull(hcStateEntity);

    ClusterServiceDAO clusterServiceDao = injector.getInstance(ClusterServiceDAO.class);
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
   * @param alertDefinitionIgnoreColumnCapture
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
   * @param alertTargetStatesColumnCapture
   */
  private void verifyAlertTargetStatesTable(
      Capture<List<DBAccessor.DBColumnInfo>> alertTargetStatesCapture) {
    Assert.assertEquals(2, alertTargetStatesCapture.getValue().size());
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
}
