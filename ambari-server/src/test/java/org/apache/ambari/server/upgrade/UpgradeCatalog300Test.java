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

import static org.apache.ambari.server.upgrade.UpgradeCatalog300.COMPONENT_DESIRED_STATE_TABLE;
import static org.apache.ambari.server.upgrade.UpgradeCatalog300.COMPONENT_STATE_TABLE;
import static org.apache.ambari.server.upgrade.UpgradeCatalog300.SECURITY_STATE_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog300.SERVICE_DESIRED_STATE_TABLE;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.persistence.EntityManager;

import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariManagementControllerImpl;
import org.apache.ambari.server.controller.MaintenanceStateHelper;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.stack.OsFamily;
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
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;

@RunWith(EasyMockRunner.class)
public class UpgradeCatalog300Test {

  @Mock(type = MockType.STRICT)
  private Provider<EntityManager> entityManagerProvider;

  @Mock(type = MockType.NICE)
  private Injector injector;

  @Mock(type = MockType.NICE)
  private EntityManager entityManager;

  @Mock(type = MockType.NICE)
  private DBAccessor dbAccessor;

  @Mock(type = MockType.NICE)
  private OsFamily osFamily;

  @Mock(type = MockType.NICE)
  private Configuration configuration;

  @Mock(type = MockType.NICE)
  private Config config;

  @Mock(type = MockType.NICE)
  private ActionManager actionManager;

  @Mock(type = MockType.NICE)
  private Clusters clusters;

  @Mock(type = MockType.NICE)
  private Cluster cluster;

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
    Method showHcatDeletedUserMessage = UpgradeCatalog300.class.getDeclaredMethod("showHcatDeletedUserMessage");
    Method setStatusOfStagesAndRequests = UpgradeCatalog300.class.getDeclaredMethod("setStatusOfStagesAndRequests");
    Method updateLogSearchConfigs = UpgradeCatalog300.class.getDeclaredMethod("updateLogSearchConfigs");

   UpgradeCatalog300 upgradeCatalog300 = createMockBuilder(UpgradeCatalog300.class)
            .addMockedMethod(showHcatDeletedUserMessage)
            .addMockedMethod(addNewConfigurationsFromXml)
            .addMockedMethod(setStatusOfStagesAndRequests)
            .addMockedMethod(updateLogSearchConfigs)
            .createMock();


    upgradeCatalog300.addNewConfigurationsFromXml();
    upgradeCatalog300.showHcatDeletedUserMessage();
    upgradeCatalog300.setStatusOfStagesAndRequests();

    upgradeCatalog300.updateLogSearchConfigs();
    expectLastCall().once();

    replay(upgradeCatalog300);

    upgradeCatalog300.executeDMLUpdates();

    verify(upgradeCatalog300);
  }

  @Test
  public void testExecuteDDLUpdates() throws Exception {
    Module module = new Module() {
      @Override
      public void configure(Binder binder) {
        binder.bind(DBAccessor.class).toInstance(dbAccessor);
        binder.bind(OsFamily.class).toInstance(osFamily);
        binder.bind(EntityManager.class).toInstance(entityManager);
        binder.bind(Configuration.class).toInstance(configuration);
      }
    };

    Capture<DBAccessor.DBColumnInfo> hrcOpsDisplayNameColumn = newCapture();
    dbAccessor.addColumn(eq(UpgradeCatalog300.HOST_ROLE_COMMAND_TABLE), capture(hrcOpsDisplayNameColumn));

    dbAccessor.dropColumn(COMPONENT_DESIRED_STATE_TABLE, SECURITY_STATE_COLUMN); expectLastCall().once();
    dbAccessor.dropColumn(COMPONENT_STATE_TABLE, SECURITY_STATE_COLUMN); expectLastCall().once();
    dbAccessor.dropColumn(SERVICE_DESIRED_STATE_TABLE, SECURITY_STATE_COLUMN); expectLastCall().once();

    replay(dbAccessor, configuration);

    Injector injector = Guice.createInjector(module);
    UpgradeCatalog300 upgradeCatalog300 = injector.getInstance(UpgradeCatalog300.class);
    upgradeCatalog300.executeDDLUpdates();

    DBAccessor.DBColumnInfo capturedOpsDisplayNameColumn = hrcOpsDisplayNameColumn.getValue();
    Assert.assertEquals(UpgradeCatalog300.HRC_OPS_DISPLAY_NAME_COLUMN, capturedOpsDisplayNameColumn.getName());
    Assert.assertEquals(null, capturedOpsDisplayNameColumn.getDefaultValue());
    Assert.assertEquals(String.class, capturedOpsDisplayNameColumn.getType());

    verify(dbAccessor);
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
    configHelper.removeConfigsByType(cluster,"service-1-logsearch-conf");
    expectLastCall().once();
    configHelper.removeConfigsByType(cluster,"service-2-logsearch-conf");
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
    new UpgradeCatalog300(injector2).updateLogSearchConfigs();
    easyMockSupport.verifyAll();

    Map<String,String> newLogFeederProperties = logFeederPropertiesCapture.getValue();
    assertTrue(Maps.difference(expectedLogFeederProperties, newLogFeederProperties).areEqual());

    Map<String,String> newLogSearchProperties = logSearchPropertiesCapture.getValue();
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

    Config confSomethingElse1 = easyMockSupport.createNiceMock(Config.class);
    expect(confSomethingElse1.getType()).andReturn("something-else-1");
    Config confSomethingElse2 = easyMockSupport.createNiceMock(Config.class);
    expect(confSomethingElse2.getType()).andReturn("something-else-2");
    Config confLogSearchConf1 = easyMockSupport.createNiceMock(Config.class);
    expect(confLogSearchConf1.getType()).andReturn("service-1-logsearch-conf");
    Config confLogSearchConf2 = easyMockSupport.createNiceMock(Config.class);
    expect(confLogSearchConf2.getType()).andReturn("service-2-logsearch-conf");

    Map<String, String> oldLogSearchConf = ImmutableMap.of(
        "service_name", "Service",
        "component_mappings", "Component Mappings",
        "content", "Content");

    Collection<Config> configs = Arrays.asList(confSomethingElse1, confLogSearchConf1, confSomethingElse2, confLogSearchConf2);

    expect(cluster.getAllConfigs()).andReturn(configs).atLeastOnce();
    expect(cluster.getDesiredConfigByType("service-1-logsearch-conf")).andReturn(confLogSearchConf1).once();
    expect(cluster.getDesiredConfigByType("service-2-logsearch-conf")).andReturn(confLogSearchConf2).once();
    expect(confLogSearchConf1.getProperties()).andReturn(oldLogSearchConf).once();
    expect(confLogSearchConf2.getProperties()).andReturn(oldLogSearchConf).once();
    Capture<Map<String, String>> logSearchConfCapture = EasyMock.newCapture(CaptureType.ALL);
    expect(controller.createConfig(anyObject(Cluster.class), anyString(), capture(logSearchConfCapture), anyString(),
        EasyMock.<Map<String, Map<String, String>>>anyObject())).andReturn(config).times(2);

    replay(clusters, cluster);
    replay(controller, injector2);
    replay(confSomethingElse1, confSomethingElse2, confLogSearchConf1, confLogSearchConf2);
    new UpgradeCatalog300(injector2).updateLogSearchConfigs();
    easyMockSupport.verifyAll();

    List<Map<String, String>> updatedLogSearchConfs = logSearchConfCapture.getValues();
    assertEquals(updatedLogSearchConfs.size(), 2);
    for (Map<String, String> updatedLogSearchConf : updatedLogSearchConfs) {
      assertTrue(Maps.difference(Collections.<String, String> emptyMap(), updatedLogSearchConf).areEqual());
    }
  }
}
