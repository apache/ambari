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
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.PermissionDAO;
import org.apache.ambari.server.orm.dao.PrincipalDAO;
import org.apache.ambari.server.orm.dao.PrincipalTypeDAO;
import org.apache.ambari.server.orm.dao.PrivilegeDAO;
import org.apache.ambari.server.orm.dao.ResourceDAO;
import org.apache.ambari.server.orm.dao.ResourceTypeDAO;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.dao.ViewDAO;
import org.apache.ambari.server.orm.dao.ViewInstanceDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.StackId;
import org.easymock.Capture;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;

/**
 * UpgradeCatalog170 unit tests.
 */
public class UpgradeCatalog170Test {

  Provider<EntityManager> entityManagerProvider = createStrictMock(Provider.class);
  EntityManager entityManager = createStrictMock(EntityManager.class);

  @Before
  public void init() {
    reset(entityManagerProvider);
    expect(entityManagerProvider.get()).andReturn(entityManager).anyTimes();
    replay(entityManagerProvider);
  }

  @Test
  public void testExecuteDDLUpdates() throws Exception {

    final DBAccessor dbAccessor = createNiceMock(DBAccessor.class);
    Connection connection = createNiceMock(Connection.class);
    PreparedStatement stmt = createNiceMock(PreparedStatement.class);
    Configuration configuration = createNiceMock(Configuration.class);
    ResultSet resultSet = createNiceMock(ResultSet.class);
    expect(configuration.getDatabaseUrl()).andReturn(Configuration.JDBC_IN_MEMORY_URL).anyTimes();
    expect(dbAccessor.getNewConnection()).andReturn(connection);
    expect(connection.prepareStatement("SELECT config_id FROM clusterconfig " +
      "WHERE type_name = ? ORDER BY create_timestamp")).andReturn(stmt);
    expect(connection.prepareStatement("UPDATE clusterconfig SET version = ? " +
      "WHERE config_id = ?")).andReturn(stmt);
    stmt.close();
    expectLastCall().times(2);
    connection.close();
    expectLastCall();

    Capture<DBAccessor.DBColumnInfo> clusterConfigAttributesColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
    Capture<DBAccessor.DBColumnInfo> maskColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
    Capture<DBAccessor.DBColumnInfo> maskedColumnCapture = new Capture<DBAccessor.DBColumnInfo>();
    Capture<List<DBAccessor.DBColumnInfo>> alertDefinitionColumnCapture = new Capture<List<DBAccessor.DBColumnInfo>>();
    Capture<List<DBAccessor.DBColumnInfo>> alertHistoryColumnCapture = new Capture<List<DBAccessor.DBColumnInfo>>();
    Capture<List<DBAccessor.DBColumnInfo>> alertCurrentColumnCapture = new Capture<List<DBAccessor.DBColumnInfo>>();
    Capture<List<DBAccessor.DBColumnInfo>> alertGroupColumnCapture = new Capture<List<DBAccessor.DBColumnInfo>>();
    Capture<List<DBAccessor.DBColumnInfo>> alertTargetCapture = new Capture<List<DBAccessor.DBColumnInfo>>();
    Capture<List<DBAccessor.DBColumnInfo>> alertGroupTargetCapture = new Capture<List<DBAccessor.DBColumnInfo>>();
    Capture<List<DBAccessor.DBColumnInfo>> alertGroupingCapture = new Capture<List<DBAccessor.DBColumnInfo>>();
    Capture<List<DBAccessor.DBColumnInfo>> alertNoticeCapture = new Capture<List<DBAccessor.DBColumnInfo>>();
    Capture<List<DBAccessor.DBColumnInfo>> serviceConfigCapture = new Capture<List<DBAccessor.DBColumnInfo>>();
    Capture<List<DBAccessor.DBColumnInfo>> serviceConfigMappingCapture = new Capture<List<DBAccessor.DBColumnInfo>>();

    setViewExpectations(dbAccessor, maskColumnCapture);
    setViewParameterExpectations(dbAccessor, maskedColumnCapture);
    setClusterConfigExpectations(dbAccessor, clusterConfigAttributesColumnCapture);

    dbAccessor.createTable(eq("alert_definition"),
        capture(alertDefinitionColumnCapture), eq("definition_id"));

    dbAccessor.createTable(eq("alert_history"),
        capture(alertHistoryColumnCapture), eq("alert_id"));

    dbAccessor.createTable(eq("alert_current"),
        capture(alertCurrentColumnCapture), eq("alert_id"));

    dbAccessor.createTable(eq("alert_group"), capture(alertGroupColumnCapture),
        eq("group_id"));

    dbAccessor.createTable(eq("alert_target"), capture(alertTargetCapture),
        eq("target_id"));

    dbAccessor.createTable(eq("alert_group_target"),
        capture(alertGroupTargetCapture), eq("group_id"), eq("target_id"));

    dbAccessor.createTable(eq("alert_grouping"), capture(alertGroupingCapture),
        eq("group_id"), eq("definition_id"));

    dbAccessor.createTable(eq("alert_notice"), capture(alertNoticeCapture),
        eq("notification_id"));

    dbAccessor.createTable(eq("serviceconfig"), capture(serviceConfigCapture),
        eq("service_config_id"));

    dbAccessor.createTable(eq("serviceconfigmapping"),
        capture(serviceConfigMappingCapture), eq("service_config_id"),
        eq("config_id"));

    dbAccessor.executeSelect(anyObject(String.class));
    expectLastCall().andReturn(resultSet).anyTimes();
    resultSet.next();
    expectLastCall().andReturn(false).anyTimes();
    resultSet.close();
    expectLastCall().anyTimes();

    replay(dbAccessor, configuration, resultSet, connection, stmt);
    AbstractUpgradeCatalog upgradeCatalog = getUpgradeCatalog(dbAccessor);
    Class<?> c = AbstractUpgradeCatalog.class;
    Field f = c.getDeclaredField("configuration");
    f.setAccessible(true);
    f.set(upgradeCatalog, configuration);

    upgradeCatalog.executeDDLUpdates();
    verify(dbAccessor, configuration, resultSet, connection, stmt);

    assertClusterConfigColumns(clusterConfigAttributesColumnCapture);
    assertViewColumns(maskColumnCapture);
    assertViewParameterColumns(maskedColumnCapture);

    assertEquals(11, alertDefinitionColumnCapture.getValue().size());
    assertEquals(11, alertHistoryColumnCapture.getValue().size());
    assertEquals(6, alertCurrentColumnCapture.getValue().size());
    assertEquals(4, alertGroupColumnCapture.getValue().size());
    assertEquals(5, alertTargetCapture.getValue().size());
    assertEquals(2, alertGroupTargetCapture.getValue().size());
    assertEquals(2, alertGroupingCapture.getValue().size());
    assertEquals(4, alertNoticeCapture.getValue().size());
    assertEquals(2, serviceConfigCapture.getValue().size());
    assertEquals(2, serviceConfigMappingCapture.getValue().size());
  }

  @Test
  public void testExecuteDMLUpdates() throws Exception {
    Configuration configuration = createNiceMock(Configuration.class);
    DBAccessor dbAccessor = createNiceMock(DBAccessor.class);
    Injector injector = createNiceMock(Injector.class);
    ConfigHelper configHelper = createNiceMock(ConfigHelper.class);
    AmbariManagementController amc = createNiceMock(AmbariManagementController.class);
    Cluster cluster = createStrictMock(Cluster.class);
    Clusters clusters = createStrictMock(Clusters.class);
    Config config = createStrictMock(Config.class);
    Config pigConfig = createStrictMock(Config.class);

    UserDAO userDAO = createNiceMock(UserDAO.class);
    PrincipalDAO principalDAO = createNiceMock(PrincipalDAO.class);
    PrincipalTypeDAO principalTypeDAO = createNiceMock(PrincipalTypeDAO.class);
    ClusterDAO clusterDAO = createNiceMock(ClusterDAO.class);
    ResourceTypeDAO resourceTypeDAO = createNiceMock(ResourceTypeDAO.class);
    ResourceDAO resourceDAO = createNiceMock(ResourceDAO.class);
    ViewDAO viewDAO = createNiceMock(ViewDAO.class);
    ViewInstanceDAO viewInstanceDAO = createNiceMock(ViewInstanceDAO.class);
    PermissionDAO permissionDAO = createNiceMock(PermissionDAO.class);
    PrivilegeDAO privilegeDAO = createNiceMock(PrivilegeDAO.class);

    EntityTransaction trans = createNiceMock(EntityTransaction.class);
    CriteriaBuilder cb = createNiceMock(CriteriaBuilder.class);
    CriteriaQuery<HostRoleCommandEntity> cq = createNiceMock(CriteriaQuery.class);
    Root<HostRoleCommandEntity> hrc = createNiceMock(Root.class);
    Path<Long> taskId = null;
    Path<String> outputLog = null;
    Path<String> errorLog = null;
    Order o = createNiceMock(Order.class);
    TypedQuery<HostRoleCommandEntity> q = createNiceMock(TypedQuery.class);
    List<HostRoleCommandEntity> r = new ArrayList<HostRoleCommandEntity>();

    Method m = AbstractUpgradeCatalog.class.getDeclaredMethod
        ("updateConfigurationProperties", String.class, Map.class, boolean.class, boolean.class);
    Method n = AbstractUpgradeCatalog.class.getDeclaredMethod("getEntityManagerProvider");

    UpgradeCatalog170 upgradeCatalog = createMockBuilder(UpgradeCatalog170.class)
      .addMockedMethod(m).addMockedMethod(n).createMock();

    Map<String, Cluster> clustersMap = new HashMap<String, Cluster>();
    clustersMap.put("c1", cluster);

    Map<String, String> globalConfigs = new HashMap<String, String>();
    globalConfigs.put("prop1", "val1");
    globalConfigs.put("smokeuser_keytab", "val2");

    Map<String, String> pigSettings = new HashMap<String, String>();
    pigSettings.put("pig-content", "foo");

    Set<String> envDicts = new HashSet<String>();
    envDicts.add("hadoop-env");
    envDicts.add("global");

    Map<String, String> contentOfHadoopEnv = new HashMap<String, String>();
    contentOfHadoopEnv.put("content", "env file contents");

    upgradeCatalog.updateConfigurationProperties("hadoop-env",
        globalConfigs, true, true);
    expectLastCall();

    upgradeCatalog.updateConfigurationProperties("hadoop-env",
        contentOfHadoopEnv, true, true);
    expectLastCall();

    upgradeCatalog.updateConfigurationProperties("hbase-env",
        Collections.singletonMap("hbase_regionserver_xmn_max", "512"), false, false);
    expectLastCall();

    upgradeCatalog.updateConfigurationProperties("hbase-env",
        Collections.singletonMap("hbase_regionserver_xmn_ratio", "0.2"), false, false);
    expectLastCall();
    
    upgradeCatalog.updateConfigurationProperties("yarn-env",
        Collections.singletonMap("min_user_id", "1000"), false, false);
    expectLastCall();

    expect(entityManager.getTransaction()).andReturn(trans).anyTimes();
    expect(entityManager.getCriteriaBuilder()).andReturn(cb).anyTimes();
    expect(entityManager.createQuery(cq)).andReturn(q).anyTimes();
    expect(trans.isActive()).andReturn(true).anyTimes();
    expect(upgradeCatalog.getEntityManagerProvider()).andReturn(entityManagerProvider).anyTimes();
    expect(cb.createQuery(HostRoleCommandEntity.class)).andReturn(cq).anyTimes();
    expect(cb.desc(taskId)).andReturn(o).anyTimes();
    expect(cq.from(HostRoleCommandEntity.class)).andReturn(hrc).anyTimes();
    expect(cq.select(hrc)).andReturn(cq).anyTimes();
    expect(cq.where(anyObject(Predicate.class))).andReturn(cq).anyTimes();
    expect(hrc.get(isA(SingularAttribute.class))).andReturn(taskId).times(2);
    expect(hrc.get(isA(SingularAttribute.class))).andReturn(outputLog).once();
    expect(hrc.get(isA(SingularAttribute.class))).andReturn(errorLog).once();
    expect(q.setMaxResults(1000)).andReturn(q).anyTimes();
    expect(q.getResultList()).andReturn(r).anyTimes();

    expect(configuration.getDatabaseUrl()).andReturn(Configuration.JDBC_IN_MEMORY_URL).anyTimes();
    expect(injector.getInstance(ConfigHelper.class)).andReturn(configHelper).anyTimes();
    expect(injector.getInstance(AmbariManagementController.class)).andReturn(amc).anyTimes();
    expect(amc.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getClusters()).andReturn(clustersMap).anyTimes();
    expect(cluster.getDesiredConfigByType("global")).andReturn(config).anyTimes();
    expect(config.getProperties()).andReturn(globalConfigs).anyTimes();
    expect(cluster.getCurrentStackVersion()).andReturn(new StackId("HDP", "2.1")).anyTimes();
    expect(configHelper.findConfigTypesByPropertyName(new StackId("HDP", "2.1"), "prop1")).andReturn(envDicts).once();
    expect(configHelper.findConfigTypesByPropertyName(new StackId("HDP", "2.1"), "smokeuser_keytab")).andReturn(new HashSet<String>()).once();
    expect(configHelper.findConfigTypesByPropertyName(new StackId("HDP", "2.1"), "content")).andReturn(envDicts).once();
    expect(configHelper.getPropertyValueFromStackDefenitions(cluster, "hadoop-env", "content")).andReturn("env file contents").once();

    expect(injector.getInstance(UserDAO.class)).andReturn(userDAO).once();
    expect(injector.getInstance(PrincipalDAO.class)).andReturn(principalDAO).once();
    expect(injector.getInstance(PrincipalTypeDAO.class)).andReturn(principalTypeDAO).once();
    expect(injector.getInstance(ClusterDAO.class)).andReturn(clusterDAO).once();
    expect(injector.getInstance(ResourceTypeDAO.class)).andReturn(resourceTypeDAO).once();
    expect(injector.getInstance(ResourceDAO.class)).andReturn(resourceDAO).once();
    expect(injector.getInstance(ViewDAO.class)).andReturn(viewDAO).once();
    expect(injector.getInstance(ViewInstanceDAO.class)).andReturn(viewInstanceDAO).once();
    expect(injector.getInstance(PermissionDAO.class)).andReturn(permissionDAO).once();
    expect(injector.getInstance(PrivilegeDAO.class)).andReturn(privilegeDAO).once();

    expect(userDAO.findAll()).andReturn(Collections.<UserEntity> emptyList()).anyTimes();
    expect(clusterDAO.findAll()).andReturn(Collections.<ClusterEntity> emptyList()).anyTimes();
    expect(viewDAO.findAll()).andReturn(Collections.<ViewEntity> emptyList()).anyTimes();
    expect(viewInstanceDAO.findAll()).andReturn(Collections.<ViewInstanceEntity> emptyList()).anyTimes();
    expect(permissionDAO.findAmbariAdminPermission()).andReturn(null);
    expect(permissionDAO.findClusterOperatePermission()).andReturn(null);
    expect(permissionDAO.findClusterReadPermission()).andReturn(null);

    expect(cluster.getDesiredConfigByType("pig-properties")).andReturn(pigConfig).anyTimes();
    expect(pigConfig.getProperties()).andReturn(pigSettings).anyTimes();

    replay(entityManager, trans, upgradeCatalog, cb, cq, hrc, q);
    replay(dbAccessor, configuration, injector, cluster, clusters, amc, config, configHelper, pigConfig);
    replay(userDAO, clusterDAO, viewDAO, viewInstanceDAO, permissionDAO);

    Class<?> c = AbstractUpgradeCatalog.class;
    Field f = c.getDeclaredField("configuration");
    f.setAccessible(true);
    f.set(upgradeCatalog, configuration);
    f = c.getDeclaredField("dbAccessor");
    f.setAccessible(true);
    f.set(upgradeCatalog, dbAccessor);
    f = c.getDeclaredField("injector");
    f.setAccessible(true);
    f.set(upgradeCatalog, injector);

    upgradeCatalog.executeDMLUpdates();

    verify(upgradeCatalog, dbAccessor, configuration, injector, cluster, clusters, amc, config, configHelper);
  }


  @Test
  public void testGetTargetVersion() throws Exception {
    final DBAccessor dbAccessor     = createNiceMock(DBAccessor.class);
    UpgradeCatalog   upgradeCatalog = getUpgradeCatalog(dbAccessor);

    Assert.assertEquals("1.7.0", upgradeCatalog.getTargetVersion());
  }

  private AbstractUpgradeCatalog getUpgradeCatalog(final DBAccessor dbAccessor) {
    Module module = new Module() {
      @Override
      public void configure(Binder binder) {
        binder.bind(DBAccessor.class).toInstance(dbAccessor);
      }
    };
    Injector injector = Guice.createInjector(module);
    return injector.getInstance(UpgradeCatalog170.class);
  }

  private void assertClusterConfigColumns(Capture<DBAccessor.DBColumnInfo> clusterConfigAttributesColumnCapture) {
    DBAccessor.DBColumnInfo column = clusterConfigAttributesColumnCapture.getValue();
    assertEquals("config_attributes", column.getName());
    assertEquals(32000, (int) column.getLength());
    assertEquals(String.class, column.getType());
    assertEquals(null, column.getDefaultValue());
    assertTrue(column.isNullable());
  }

  private void setClusterConfigExpectations(DBAccessor dbAccessor,
                                   Capture<DBAccessor.DBColumnInfo> clusterConfigAttributesColumnCapture)
      throws SQLException {
    dbAccessor.addColumn(eq("clusterconfig"),
        capture(clusterConfigAttributesColumnCapture));
  }

  @Test
  public void testGetSourceVersion() {
    final DBAccessor dbAccessor     = createNiceMock(DBAccessor.class);
    UpgradeCatalog upgradeCatalog = getUpgradeCatalog(dbAccessor);
    Assert.assertEquals("1.6.1", upgradeCatalog.getSourceVersion());
  }

  private void setViewExpectations(DBAccessor dbAccessor,
                                   Capture<DBAccessor.DBColumnInfo> maskColumnCapture)
    throws SQLException {

    dbAccessor.addColumn(eq("viewmain"), capture(maskColumnCapture));
  }

  private void setViewParameterExpectations(DBAccessor dbAccessor,
                                            Capture<DBAccessor.DBColumnInfo> maskedColumnCapture)
    throws SQLException {

    dbAccessor.addColumn(eq("viewparameter"), capture(maskedColumnCapture));
  }

  private void assertViewColumns(
    Capture<DBAccessor.DBColumnInfo> maskColumnCapture) {
    DBAccessor.DBColumnInfo column = maskColumnCapture.getValue();
    assertEquals("mask", column.getName());
    assertEquals(255, (int) column.getLength());
    assertEquals(String.class, column.getType());
    assertNull(column.getDefaultValue());
    assertTrue(column.isNullable());
  }

  private void assertViewParameterColumns(
    Capture<DBAccessor.DBColumnInfo> maskedColumnCapture) {
    DBAccessor.DBColumnInfo column = maskedColumnCapture.getValue();
    assertEquals("masked", column.getName());
    assertEquals(1, (int) column.getLength());
    assertEquals(Character.class, column.getType());
    assertNull(column.getDefaultValue());
    assertTrue(column.isNullable());
  }
}
