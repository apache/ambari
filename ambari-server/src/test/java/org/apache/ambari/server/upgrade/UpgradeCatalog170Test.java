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
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.StackId;
import org.easymock.Capture;
import org.junit.Assert;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

/**
 * UpgradeCatalog170 unit tests.
 */
public class UpgradeCatalog170Test {

  @Test
  public void testExecuteDDLUpdates() throws Exception {

    final DBAccessor dbAccessor = createNiceMock(DBAccessor.class);
    Configuration configuration = createNiceMock(Configuration.class);
    ResultSet resultSet = createNiceMock(ResultSet.class);
    expect(configuration.getDatabaseUrl()).andReturn(Configuration.JDBC_IN_MEMORY_URL).anyTimes();

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

    dbAccessor.executeSelect(anyObject(String.class));
    expectLastCall().andReturn(resultSet).anyTimes();
    resultSet.next();
    expectLastCall().andReturn(false).anyTimes();
    resultSet.close();
    expectLastCall().anyTimes();

    replay(dbAccessor, configuration, resultSet);
    AbstractUpgradeCatalog upgradeCatalog = getUpgradeCatalog(dbAccessor);
    Class<?> c = AbstractUpgradeCatalog.class;
    Field f = c.getDeclaredField("configuration");
    f.setAccessible(true);
    f.set(upgradeCatalog, configuration);

    upgradeCatalog.executeDDLUpdates();
    verify(dbAccessor, configuration, resultSet);

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

    Method m = AbstractUpgradeCatalog.class.getDeclaredMethod
        ("updateConfigurationProperties", String.class, Map.class, boolean.class, boolean.class);

    UpgradeCatalog170 upgradeCatalog = createMockBuilder(UpgradeCatalog170.class)
      .addMockedMethod(m).createMock();

    Map<String, Cluster> clustersMap = new HashMap<String, Cluster>();
    clustersMap.put("c1", cluster);

    Map<String, String> globalConfigs = new HashMap<String, String>();
    globalConfigs.put("prop1", "val1");
    globalConfigs.put("smokeuser_keytab", "val2");

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

    replay(upgradeCatalog, dbAccessor, configuration, injector, cluster, clusters, amc, config, configHelper);

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
