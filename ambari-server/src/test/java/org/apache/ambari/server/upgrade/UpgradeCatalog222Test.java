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
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.EntityManager;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariManagementControllerImpl;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.MaintenanceStateHelper;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.dao.WidgetDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.orm.entities.WidgetEntity;
import org.apache.ambari.server.stack.StackManagerFactory;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.commons.io.FileUtils;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.persist.PersistService;

public class UpgradeCatalog222Test {
  private Injector injector;
  private Provider<EntityManager> entityManagerProvider = createStrictMock(Provider.class);
  private EntityManager entityManager = createNiceMock(EntityManager.class);

  @Inject
  private UpgradeCatalogHelper upgradeCatalogHelper;

  private StackEntity desiredStackEntity;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  public UpgradeCatalog222Test(){
    injector = Guice.createInjector(new InMemoryDefaultTestModule());

  }

  @Before
  public void init() {
    reset(entityManagerProvider);
    expect(entityManagerProvider.get()).andReturn(entityManager).anyTimes();
    replay(entityManagerProvider);

    injector.getInstance(GuiceJpaInitializer.class);

    // inject AmbariMetaInfo to ensure that stacks get populated in the DB
    injector.getInstance(AmbariMetaInfo.class);

    injector.injectMembers(this);

    // load the stack entity
    StackDAO stackDAO = injector.getInstance(StackDAO.class);
    desiredStackEntity = stackDAO.find("HDP", "2.2.0");
  }

  @After
  public void tearDown() {
    injector.getInstance(PersistService.class).stop();
  }

  @Test
  public void testExecuteDMLUpdates() throws Exception {
    Method addNewConfigurationsFromXml = AbstractUpgradeCatalog.class.getDeclaredMethod("addNewConfigurationsFromXml");
    Method updateAlerts = UpgradeCatalog222.class.getDeclaredMethod("updateAlerts");
    Method updateStormConfigs = UpgradeCatalog222.class.getDeclaredMethod("updateStormConfigs");
    Method updateAMSConfigs = UpgradeCatalog222.class.getDeclaredMethod("updateAMSConfigs");
    Method updateHiveConfigs = UpgradeCatalog222.class.getDeclaredMethod("updateHiveConfig");
    Method updateHostRoleCommands = UpgradeCatalog222.class.getDeclaredMethod("updateHostRoleCommands");
    Method updateHDFSWidget = UpgradeCatalog222.class.getDeclaredMethod("updateHDFSWidgetDefinition");
    Method updateYARNWidget = UpgradeCatalog222.class.getDeclaredMethod("updateYARNWidgetDefinition");
    Method updateHBASEWidget = UpgradeCatalog222.class.getDeclaredMethod("updateHBASEWidgetDefinition");
    Method updateHbaseEnvConfig = UpgradeCatalog222.class.getDeclaredMethod("updateHbaseEnvConfig");
    Method updateCorruptedReplicaWidget = UpgradeCatalog222.class.getDeclaredMethod("updateCorruptedReplicaWidget");
    Method createNewSliderConfigVersion = UpgradeCatalog222.class.getDeclaredMethod("createNewSliderConfigVersion");
    Method updateZookeeperConfigs = UpgradeCatalog222.class.getDeclaredMethod("updateZookeeperConfigs");
    Method updateHBASEConfigs = UpgradeCatalog222.class.getDeclaredMethod("updateHBASEConfigs");
    Method initializeStromAnsKafkaWidgets = UpgradeCatalog222.class.getDeclaredMethod("initializeStromAndKafkaWidgets");

    UpgradeCatalog222 upgradeCatalog222 = createMockBuilder(UpgradeCatalog222.class)
      .addMockedMethod(addNewConfigurationsFromXml)
      .addMockedMethod(updateAlerts)
      .addMockedMethod(updateStormConfigs)
      .addMockedMethod(updateAMSConfigs)
      .addMockedMethod(updateHiveConfigs)
      .addMockedMethod(updateHostRoleCommands)
      .addMockedMethod(updateHDFSWidget)
      .addMockedMethod(updateYARNWidget)
      .addMockedMethod(updateHBASEWidget)
      .addMockedMethod(updateHbaseEnvConfig)
      .addMockedMethod(updateCorruptedReplicaWidget)
      .addMockedMethod(createNewSliderConfigVersion)
      .addMockedMethod(updateZookeeperConfigs)
      .addMockedMethod(updateHBASEConfigs)
      .addMockedMethod(initializeStromAnsKafkaWidgets)
      .createMock();

    upgradeCatalog222.addNewConfigurationsFromXml();
    upgradeCatalog222.updateAlerts();
    upgradeCatalog222.updateStormConfigs();
    upgradeCatalog222.updateAMSConfigs();
    upgradeCatalog222.updateHostRoleCommands();
    upgradeCatalog222.updateHiveConfig();
    upgradeCatalog222.updateHDFSWidgetDefinition();
    upgradeCatalog222.updateHbaseEnvConfig();
    upgradeCatalog222.updateYARNWidgetDefinition();
    upgradeCatalog222.updateHBASEWidgetDefinition();
    upgradeCatalog222.updateCorruptedReplicaWidget();
    upgradeCatalog222.updateZookeeperConfigs();
    upgradeCatalog222.updateHBASEConfigs();
    upgradeCatalog222.createNewSliderConfigVersion();
    upgradeCatalog222.initializeStromAndKafkaWidgets();

    replay(upgradeCatalog222);

    upgradeCatalog222.executeDMLUpdates();

    verify(upgradeCatalog222);
  }

  @Test
  public void testUpdateAlerts_ATSAlert() {
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariManagementController mockAmbariManagementController = easyMockSupport.createNiceMock(AmbariManagementController.class);
    final Clusters mockClusters = easyMockSupport.createStrictMock(Clusters.class);
    final Cluster mockClusterExpected = easyMockSupport.createNiceMock(Cluster.class);
    final AlertDefinitionDAO mockAlertDefinitionDAO = easyMockSupport.createNiceMock(AlertDefinitionDAO.class);
    final AlertDefinitionEntity mockATSWebAlert = easyMockSupport.createNiceMock(AlertDefinitionEntity.class);

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

    long clusterId = 1;

    expect(mockAmbariManagementController.getClusters()).andReturn(mockClusters).once();
    expect(mockClusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", mockClusterExpected);
    }}).atLeastOnce();
    expect(mockClusterExpected.getClusterId()).andReturn(clusterId).anyTimes();
    expect(mockAlertDefinitionDAO.findByName(eq(clusterId), eq("yarn_app_timeline_server_webui")))
            .andReturn(mockATSWebAlert).atLeastOnce();
    expect(mockATSWebAlert.getSource()).andReturn("{\"uri\": {\n" +
      "            \"http\": \"{{yarn-site/yarn.timeline-service.webapp.address}}/ws/v1/timeline\",\n" +
      "            \"https\": \"{{yarn-site/yarn.timeline-service.webapp.https.address}}/ws/v1/timeline\" } }");

    mockATSWebAlert.setSource("{\"uri\":{\"http\":\"{{yarn-site/yarn.timeline-service.webapp.address}}/ws/v1/timeline\",\"https\":\"{{yarn-site/yarn.timeline-service.webapp.https.address}}/ws/v1/timeline\"}}");
    expectLastCall().once();

    easyMockSupport.replayAll();
    mockInjector.getInstance(UpgradeCatalog222.class).updateAlerts();
    easyMockSupport.verifyAll();
  }

  @Test
  public void testHiveSiteUpdateConfigs() throws AmbariException {
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariManagementController mockAmbariManagementController = easyMockSupport.createNiceMock(AmbariManagementController.class);
    final Clusters mockClusters = easyMockSupport.createStrictMock(Clusters.class);
    final Cluster mockClusterExpected = easyMockSupport.createNiceMock(Cluster.class);

    final Config hiveSiteConfigs = easyMockSupport.createNiceMock(Config.class);
    final Config AtlasSiteConfigs = easyMockSupport.createNiceMock(Config.class);

    final ServiceComponentHost atlasHost = easyMockSupport.createNiceMock(ServiceComponentHost.class);
    final List<ServiceComponentHost> atlasHosts = new ArrayList<>();
    atlasHosts.add(atlasHost);

    StackId stackId = new StackId("HDP","2.3");

    final Map<String, String> propertiesAtlasSiteConfigs = new HashMap<String, String>() {{
      put("atlas.enableTLS", "true");
      put("atlas.server.https.port", "21443");
    }};

    final Injector mockInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(AmbariManagementController.class).toInstance(mockAmbariManagementController);
        bind(Clusters.class).toInstance(mockClusters);
        bind(EntityManager.class).toInstance(entityManager);
        bind(ServiceComponentHost.class).toInstance(atlasHost);

        bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
      }
    });

    expect(mockClusterExpected.getCurrentStackVersion()).andReturn(stackId).once();
    expect(mockClusterExpected.getServiceComponentHosts("ATLAS", "ATLAS_SERVER")).andReturn(atlasHosts).once();
    expect(atlasHost.getHostName()).andReturn("c6401").once();
    expect(mockAmbariManagementController.getClusters()).andReturn(mockClusters).once();
    expect(mockClusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", mockClusterExpected);
    }}).atLeastOnce();
    expect(mockClusterExpected.getDesiredConfigByType("hive-site")).andReturn(hiveSiteConfigs).atLeastOnce();
    expect(mockClusterExpected.getDesiredConfigByType("application-properties")).andReturn(AtlasSiteConfigs).anyTimes();
    expect(AtlasSiteConfigs.getProperties()).andReturn(propertiesAtlasSiteConfigs).anyTimes();

    UpgradeCatalog222 upgradeCatalog222 = createMockBuilder(UpgradeCatalog222.class)
      .withConstructor(Injector.class)
      .withArgs(mockInjector)
      .addMockedMethod("updateConfigurationPropertiesForCluster", Cluster.class, String.class,
        Map.class, boolean.class, boolean.class)
      .createMock();

    Map<String, String> expectedUpdates = new HashMap<>();
    expectedUpdates.put("atlas.hook.hive.minThreads", "1");
    expectedUpdates.put("atlas.hook.hive.maxThreads", "1");
    expectedUpdates.put("atlas.cluster.name", "primary");
    expectedUpdates.put("atlas.rest.address", "https://c6401:21443");

    upgradeCatalog222.updateConfigurationPropertiesForCluster(mockClusterExpected, "hive-site", expectedUpdates,
      false, false);
    expectLastCall().once();

    easyMockSupport.replayAll();
    replay(upgradeCatalog222);
    upgradeCatalog222.updateHiveConfig();
    easyMockSupport.verifyAll();
  }

  @Test
  public void testUpdateZookeeperConfigs() throws Exception{
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariManagementController mockAmbariManagementController = easyMockSupport.createNiceMock(AmbariManagementController.class);
    final Clusters mockClusters = easyMockSupport.createStrictMock(Clusters.class);
    final Cluster mockClusterExpected = easyMockSupport.createNiceMock(Cluster.class);

    final Config zookeeperEnv = easyMockSupport.createNiceMock(Config.class);
    expect(zookeeperEnv.getProperties()).andReturn(new HashMap<String, String>(){{
      put("zk_server_heapsize", "1024");
    }}
    ).anyTimes();

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
    expect(mockClusterExpected.getDesiredConfigByType("zookeeper-env")).andReturn(zookeeperEnv).atLeastOnce();

    UpgradeCatalog222 upgradeCatalog222 = createMockBuilder(UpgradeCatalog222.class)
        .withConstructor(Injector.class)
        .withArgs(mockInjector)
        .addMockedMethod("updateConfigurationPropertiesForCluster", Cluster.class, String.class,
            Map.class, boolean.class, boolean.class)
        .createMock();

    Map<String, String> expectedUpdates = new HashMap<>();
    expectedUpdates.put("zk_server_heapsize", "1024m");

    upgradeCatalog222.updateConfigurationPropertiesForCluster(mockClusterExpected, "zookeeper-env", expectedUpdates,
        true, false);
    expectLastCall().once();

    easyMockSupport.replayAll();
    replay(upgradeCatalog222);
    upgradeCatalog222.updateZookeeperConfigs();
    easyMockSupport.verifyAll();
  }

  @Test
  public void testUpdateHBASEConfigs() throws Exception{
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariManagementController mockAmbariManagementController = easyMockSupport.createNiceMock(AmbariManagementController.class);
    final Clusters mockClusters = easyMockSupport.createStrictMock(Clusters.class);
    final Cluster mockClusterExpected = easyMockSupport.createNiceMock(Cluster.class);

    final Config hbaseSite = easyMockSupport.createNiceMock(Config.class);
    expect(hbaseSite.getProperties()).andReturn(new HashMap<String, String>(){{
                                                     put(UpgradeCatalog222.HBASE_SITE_HBASE_COPROCESSOR_MASTER_CLASSES, "test1");
                                                     put(UpgradeCatalog222.HBASE_SITE_HBASE_COPROCESSOR_REGION_CLASSES, "test2");
                                                     put(UpgradeCatalog222.HBASE_SITE_HBASE_COPROCESSOR_REGIONSERVER_CLASSES, "test3");
                                                   }}
    ).anyTimes();

    final Config rangerHbasePluginProperties = easyMockSupport.createNiceMock(Config.class);
    expect(rangerHbasePluginProperties.getProperties()).andReturn(new HashMap<String, String>(){{
                                                  put(AbstractUpgradeCatalog.PROPERTY_RANGER_HBASE_PLUGIN_ENABLED, "yes");
                                                }}
    ).anyTimes();


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

    expect(mockAmbariManagementController.getClusters()).andReturn(mockClusters).anyTimes();
    expect(mockClusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", mockClusterExpected);
    }}).atLeastOnce();

    UpgradeCatalog222 upgradeCatalog222 = createMockBuilder(UpgradeCatalog222.class)
      .withConstructor(Injector.class)
      .withArgs(mockInjector)
      .addMockedMethod("updateConfigurationPropertiesForCluster", Cluster.class, String.class,
        Map.class, boolean.class, boolean.class)
      .createStrictMock();

    // CASE 1 - Ranger enabled, Cluster version is 2.2
    expect(mockClusterExpected.getCurrentStackVersion()).andReturn(new StackId("HDP", "2.2")).atLeastOnce();
    expect(mockClusterExpected.getDesiredConfigByType("hbase-site")).andReturn(hbaseSite).atLeastOnce();
    expect(mockClusterExpected.getDesiredConfigByType(AbstractUpgradeCatalog.CONFIGURATION_TYPE_RANGER_HBASE_PLUGIN_PROPERTIES)).
      andReturn(rangerHbasePluginProperties).once();

    Map<String, String> expectedUpdates = new HashMap<>();
    expectedUpdates.put(UpgradeCatalog222.HBASE_SITE_HBASE_COPROCESSOR_MASTER_CLASSES, "com.xasecure.authorization.hbase.XaSecureAuthorizationCoprocessor");
    expectedUpdates.put(UpgradeCatalog222.HBASE_SITE_HBASE_COPROCESSOR_REGIONSERVER_CLASSES, "com.xasecure.authorization.hbase.XaSecureAuthorizationCoprocessor");
    expectedUpdates.put(UpgradeCatalog222.HBASE_SITE_HBASE_COPROCESSOR_REGION_CLASSES,
      "org.apache.hadoop.hbase.security.token.TokenProvider,org.apache.hadoop.hbase.security.access.SecureBulkLoadEndpoint," +
        "com.xasecure.authorization.hbase.XaSecureAuthorizationCoprocessor");

    upgradeCatalog222.updateConfigurationPropertiesForCluster(mockClusterExpected, "hbase-site", expectedUpdates,
      true, false);
    expectLastCall().once();

    easyMockSupport.replayAll();
    upgradeCatalog222.updateHBASEConfigs();
    easyMockSupport.verifyAll();

    // CASE 2 - Ranger enabled, Cluster version is 2.3
    reset(mockClusterExpected, upgradeCatalog222);
    expect(mockClusterExpected.getCurrentStackVersion()).andReturn(new StackId("HDP", "2.3")).atLeastOnce();
    expect(mockClusterExpected.getDesiredConfigByType("hbase-site")).andReturn(hbaseSite).atLeastOnce();
    expect(mockClusterExpected.getDesiredConfigByType(AbstractUpgradeCatalog.CONFIGURATION_TYPE_RANGER_HBASE_PLUGIN_PROPERTIES)).
      andReturn(rangerHbasePluginProperties).once();

    expectedUpdates = new HashMap<>();
    expectedUpdates.put(UpgradeCatalog222.HBASE_SITE_HBASE_COPROCESSOR_MASTER_CLASSES, "org.apache.ranger.authorization.hbase.RangerAuthorizationCoprocessor ");
    expectedUpdates.put(UpgradeCatalog222.HBASE_SITE_HBASE_COPROCESSOR_REGIONSERVER_CLASSES, "org.apache.ranger.authorization.hbase.RangerAuthorizationCoprocessor");
    expectedUpdates.put(UpgradeCatalog222.HBASE_SITE_HBASE_COPROCESSOR_REGION_CLASSES,
      "org.apache.hadoop.hbase.security.token.TokenProvider,org.apache.hadoop.hbase.security.access.SecureBulkLoadEndpoint," +
        "org.apache.ranger.authorization.hbase.RangerAuthorizationCoprocessor");

    upgradeCatalog222.updateConfigurationPropertiesForCluster(mockClusterExpected, "hbase-site", expectedUpdates,
      true, false);
    expectLastCall().once();

    replay(mockClusterExpected, upgradeCatalog222);
    upgradeCatalog222.updateHBASEConfigs();
    easyMockSupport.verifyAll();

    // CASE 3 - Ranger enabled, Cluster version is 2.1
    reset(mockClusterExpected, upgradeCatalog222);
    expect(mockClusterExpected.getCurrentStackVersion()).andReturn(new StackId("HDP", "2.1")).atLeastOnce();
    expect(mockClusterExpected.getDesiredConfigByType("hbase-site")).andReturn(hbaseSite).atLeastOnce();
    expect(mockClusterExpected.getDesiredConfigByType(AbstractUpgradeCatalog.CONFIGURATION_TYPE_RANGER_HBASE_PLUGIN_PROPERTIES)).
      andReturn(rangerHbasePluginProperties).once();

    replay(mockClusterExpected, upgradeCatalog222);
    upgradeCatalog222.updateHBASEConfigs();
    easyMockSupport.verifyAll();

    // CASE 4 - Ranger disabled
    reset(mockClusterExpected, upgradeCatalog222);
    expect(mockClusterExpected.getDesiredConfigByType("hbase-site")).andReturn(hbaseSite).atLeastOnce();
    expect(mockClusterExpected.getDesiredConfigByType(AbstractUpgradeCatalog.CONFIGURATION_TYPE_RANGER_HBASE_PLUGIN_PROPERTIES)).
      andReturn(null).once();

    replay(mockClusterExpected, upgradeCatalog222);
    upgradeCatalog222.updateHBASEConfigs();
    easyMockSupport.verifyAll();
  }

  @Test
  public void testAmsSiteUpdateConfigs() throws Exception{

    Map<String, String> oldPropertiesAmsSite = new HashMap<String, String>() {
      {
        put("timeline.metrics.host.aggregator.daily.checkpointCutOffMultiplier", String.valueOf(1));
        put("timeline.metrics.cluster.aggregator.daily.checkpointCutOffMultiplier", String.valueOf(1));
        put("timeline.metrics.service.operation.mode", "distributed");
        put("timeline.metrics.host.aggregator.ttl", String.valueOf(86400));
        put("timeline.metrics.cluster.aggregator.second.ttl", String.valueOf(21600)); //Less than 1 day
        put("timeline.metrics.cluster.aggregator.minute.ttl", String.valueOf(7776000));
        put("timeline.metrics.service.webapp.address", "0.0.0.0:6188");
        put("timeline.metrics.sink.collection.period", "60");
      }
    };
    Map<String, String> newPropertiesAmsSite = new HashMap<String, String>() {
      {
        put("timeline.metrics.host.aggregator.daily.checkpointCutOffMultiplier", String.valueOf(2));
        put("timeline.metrics.cluster.aggregator.daily.checkpointCutOffMultiplier", String.valueOf(2));
        put("timeline.metrics.service.watcher.disabled", String.valueOf(false));
        put("timeline.metrics.host.aggregator.ttl", String.valueOf(3 * 86400));
        put("timeline.metrics.cluster.aggregator.second.ttl", String.valueOf(21600));
        put("timeline.metrics.cluster.aggregator.minute.ttl", String.valueOf(30 * 86400));
        put("timeline.metrics.service.operation.mode", "distributed");
        put("timeline.metrics.service.webapp.address", "host1:6188");
        put("timeline.metrics.cluster.aggregator.interpolation.enabled", String.valueOf(true));
        put("timeline.metrics.sink.collection.period", "10");
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
    expect(mockAmsSite.getProperties()).andReturn(oldPropertiesAmsSite).anyTimes();

    Injector injector = easyMockSupport.createNiceMock(Injector.class);
    expect(injector.getInstance(Gson.class)).andReturn(null).anyTimes();
    expect(injector.getInstance(MaintenanceStateHelper.class)).andReturn(null).anyTimes();
    expect(injector.getInstance(KerberosHelper.class)).andReturn(createNiceMock(KerberosHelper.class)).anyTimes();
    expect(cluster.getHosts("AMBARI_METRICS", "METRICS_COLLECTOR")).andReturn( new HashSet<String>() {{
      add("host1");
    }}).atLeastOnce();

    replay(injector, clusters, mockAmsSite, cluster);

    AmbariManagementControllerImpl controller = createMockBuilder(AmbariManagementControllerImpl.class)
      .addMockedMethod("createConfiguration")
      .addMockedMethod("getClusters", new Class[] { })
      .addMockedMethod("createConfig")
      .withConstructor(createNiceMock(ActionManager.class), clusters, injector)
      .createNiceMock();

    Injector injector2 = easyMockSupport.createNiceMock(Injector.class);
    Capture<Map<String, String>> propertiesCapture = EasyMock.newCapture();

    expect(injector2.getInstance(AmbariManagementController.class)).andReturn(controller).anyTimes();
    expect(controller.getClusters()).andReturn(clusters).anyTimes();
    expect(controller.createConfig(anyObject(Cluster.class), anyString(), capture(propertiesCapture), anyString(),
      EasyMock.<Map<String, Map<String, String>>>anyObject())).andReturn(createNiceMock(Config.class)).once();

    replay(controller, injector2);
    new UpgradeCatalog222(injector2).updateAMSConfigs();
    easyMockSupport.verifyAll();

    Map<String, String> updatedProperties = propertiesCapture.getValue();
    assertTrue(Maps.difference(newPropertiesAmsSite, updatedProperties).areEqual());
  }

  @Test
  public void testAmsHbaseSiteUpdateConfigs() throws Exception{

    Map<String, String> oldPropertiesAmsHbaseSite = new HashMap<String, String>() {
      {
        put("hbase.client.scanner.timeout.period", String.valueOf(900000));
        put("phoenix.query.timeoutMs", String.valueOf(1200000));
      }
    };
    Map<String, String> newPropertiesAmsHbaseSite = new HashMap<String, String>() {
      {
        put("hbase.client.scanner.timeout.period", String.valueOf(300000));
        put("hbase.rpc.timeout", String.valueOf(300000));
        put("phoenix.query.timeoutMs", String.valueOf(300000));
        put("phoenix.query.keepAliveMs", String.valueOf(300000));
      }
    };
    EasyMockSupport easyMockSupport = new EasyMockSupport();

    Clusters clusters = easyMockSupport.createNiceMock(Clusters.class);
    final Cluster cluster = easyMockSupport.createNiceMock(Cluster.class);
    Config mockAmsHbaseSite = easyMockSupport.createNiceMock(Config.class);

    expect(clusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", cluster);
    }}).once();
    expect(cluster.getDesiredConfigByType("ams-hbase-site")).andReturn(mockAmsHbaseSite).atLeastOnce();
    expect(mockAmsHbaseSite.getProperties()).andReturn(oldPropertiesAmsHbaseSite).anyTimes();

    Injector injector = easyMockSupport.createNiceMock(Injector.class);
    expect(injector.getInstance(Gson.class)).andReturn(null).anyTimes();
    expect(injector.getInstance(MaintenanceStateHelper.class)).andReturn(null).anyTimes();
    expect(injector.getInstance(KerberosHelper.class)).andReturn(createNiceMock(KerberosHelper.class)).anyTimes();

    replay(injector, clusters, mockAmsHbaseSite, cluster);

    AmbariManagementControllerImpl controller = createMockBuilder(AmbariManagementControllerImpl.class)
      .addMockedMethod("createConfiguration")
      .addMockedMethod("getClusters", new Class[] { })
      .addMockedMethod("createConfig")
      .withConstructor(createNiceMock(ActionManager.class), clusters, injector)
      .createNiceMock();

    Injector injector2 = easyMockSupport.createNiceMock(Injector.class);
    Capture<Map<String, String>> propertiesCapture = EasyMock.newCapture();

    expect(injector2.getInstance(AmbariManagementController.class)).andReturn(controller).anyTimes();
    expect(controller.getClusters()).andReturn(clusters).anyTimes();
    expect(controller.createConfig(anyObject(Cluster.class), anyString(), capture(propertiesCapture), anyString(),
      EasyMock.<Map<String, Map<String, String>>>anyObject())).andReturn(createNiceMock(Config.class)).once();

    replay(controller, injector2);
    new UpgradeCatalog222(injector2).updateAMSConfigs();
    easyMockSupport.verifyAll();

    Map<String, String> updatedProperties = propertiesCapture.getValue();
    assertTrue(Maps.difference(newPropertiesAmsHbaseSite, updatedProperties).areEqual());
  }

  @Test
  public void testHDFSWidgetUpdateWithOnlyZkService() throws Exception {
    final Clusters clusters = createNiceMock(Clusters.class);
    final Cluster cluster = createNiceMock(Cluster.class);
    final AmbariManagementController controller = createNiceMock(AmbariManagementController.class);
    final Gson gson = new Gson();
    final WidgetDAO widgetDAO = createNiceMock(WidgetDAO.class);
    final AmbariMetaInfo metaInfo = createNiceMock(AmbariMetaInfo.class);
    StackInfo stackInfo = createNiceMock(StackInfo.class);
    StackId stackId = new StackId("HDP", "2.0.0");

    String widgetStr = "{\"layouts\":[{\"layout_name\":\"default_hdfs_dashboard\",\"display_name\":\"Standard HDFS Dashboard\",\"section_name\":\"HDFS_SUMMARY\",\"widgetLayoutInfo\":[{\"widget_name\":\"NameNode RPC\",\"metrics\":[],\"values\":[]}]}]}";

    File dataDirectory = temporaryFolder.newFolder();
    File file = new File(dataDirectory, "hdfs_widget.json");
    FileUtils.writeStringToFile(file, widgetStr);

    final Injector mockInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(EntityManager.class).toInstance(createNiceMock(EntityManager.class));
        bind(AmbariManagementController.class).toInstance(controller);
        bind(Clusters.class).toInstance(clusters);
        bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(Gson.class).toInstance(gson);
        bind(WidgetDAO.class).toInstance(widgetDAO);
        bind(StackManagerFactory.class).toInstance(createNiceMock(StackManagerFactory.class));
        bind(AmbariMetaInfo.class).toInstance(metaInfo);
      }
    });
    expect(controller.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", cluster);
    }}).anyTimes();
    expect(cluster.getClusterId()).andReturn(1L).anyTimes();
    expect(stackInfo.getService("HDFS")).andReturn(null);
    expect(cluster.getDesiredStackVersion()).andReturn(stackId);
    expect(metaInfo.getStack("HDP", "2.0.0")).andReturn(stackInfo);

    replay(clusters, cluster, controller, widgetDAO, metaInfo, stackInfo);

    UpgradeCatalog222 upgradeCatalog222 = createMockBuilder(UpgradeCatalog222.class)
            .withConstructor(Injector.class)
            .withArgs(mockInjector)
            .createMock();
    upgradeCatalog222.updateHDFSWidgetDefinition();

  }

  @Test
  public void testHDFSWidgetUpdate() throws Exception {
    final Clusters clusters = createNiceMock(Clusters.class);
    final Cluster cluster = createNiceMock(Cluster.class);
    final AmbariManagementController controller = createNiceMock(AmbariManagementController.class);
    final Gson gson = new Gson();
    final WidgetDAO widgetDAO = createNiceMock(WidgetDAO.class);
    final AmbariMetaInfo metaInfo = createNiceMock(AmbariMetaInfo.class);
    WidgetEntity widgetEntity = createNiceMock(WidgetEntity.class);
    WidgetEntity widgetEntity2 = createNiceMock(WidgetEntity.class);
    StackId stackId = new StackId("HDP", "2.0.0");
    StackInfo stackInfo = createNiceMock(StackInfo.class);
    ServiceInfo serviceInfo = createNiceMock(ServiceInfo.class);

    String widgetStr = "{\n" +
      "  \"layouts\": [\n" +
      "    {\n" +
      "      \"layout_name\": \"default_hdfs_dashboard\",\n" +
      "      \"display_name\": \"Standard HDFS Dashboard\",\n" +
      "      \"section_name\": \"HDFS_SUMMARY\",\n" +
      "      \"widgetLayoutInfo\": [\n" +
      "        {\n" +
      "          \"widget_name\": \"NameNode RPC\",\n" +
      "          \"metrics\": [],\n" +
      "          \"values\": []\n" +
      "        }\n" +
      "      ]\n" +
      "    },\n" +
      "        {\n" +
      "      \"layout_name\": \"default_hdfs_heatmap\",\n" +
      "      \"display_name\": \"Standard HDFS HeatMaps\",\n" +
      "      \"section_name\": \"HDFS_HEATMAPS\",\n" +
      "      \"widgetLayoutInfo\": [\n" +
      "        {\n" +
      "          \"widget_name\": \"HDFS Bytes Read\",\n" +
      "          \"metrics\": [],\n" +
      "          \"values\": []\n" +
      "        }\n" +
      "      ]\n" +
      "    }\n" +
      "  ]\n" +
      "}";

    File dataDirectory = temporaryFolder.newFolder();
    File file = new File(dataDirectory, "hdfs_widget.json");
    FileUtils.writeStringToFile(file, widgetStr);

    final Injector mockInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(EntityManager.class).toInstance(createNiceMock(EntityManager.class));
        bind(AmbariManagementController.class).toInstance(controller);
        bind(Clusters.class).toInstance(clusters);
        bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(Gson.class).toInstance(gson);
        bind(WidgetDAO.class).toInstance(widgetDAO);
        bind(StackManagerFactory.class).toInstance(createNiceMock(StackManagerFactory.class));
        bind(AmbariMetaInfo.class).toInstance(metaInfo);
      }
    });
    expect(controller.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", cluster);
    }}).anyTimes();
    expect(cluster.getClusterId()).andReturn(1L).anyTimes();
    expect(stackInfo.getService("HDFS")).andReturn(serviceInfo);
    expect(cluster.getDesiredStackVersion()).andReturn(stackId).anyTimes();
    expect(metaInfo.getStack("HDP", "2.0.0")).andReturn(stackInfo).anyTimes();
    expect(serviceInfo.getWidgetsDescriptorFile()).andReturn(file).anyTimes();

    expect(widgetDAO.findByName(1L, "NameNode RPC", "ambari", "HDFS_SUMMARY"))
      .andReturn(Collections.singletonList(widgetEntity));
    expect(widgetDAO.merge(widgetEntity)).andReturn(null);
    expect(widgetEntity.getWidgetName()).andReturn("Namenode RPC").anyTimes();

    expect(widgetDAO.findByName(1L, "HDFS Bytes Read", "ambari", "HDFS_HEATMAPS"))
      .andReturn(Collections.singletonList(widgetEntity2));
    expect(widgetDAO.merge(widgetEntity2)).andReturn(null);
    expect(widgetEntity2.getWidgetName()).andReturn("HDFS Bytes Read").anyTimes();

    replay(clusters, cluster, controller, widgetDAO, metaInfo, widgetEntity, widgetEntity2, stackInfo, serviceInfo);

    mockInjector.getInstance(UpgradeCatalog222.class).updateHDFSWidgetDefinition();

    verify(clusters, cluster, controller, widgetDAO, widgetEntity, widgetEntity2, stackInfo, serviceInfo);
  }

  @Test
  public void testYARNWidgetUpdate() throws Exception {
    final Clusters clusters = createNiceMock(Clusters.class);
    final Cluster cluster = createNiceMock(Cluster.class);
    final AmbariManagementController controller = createNiceMock(AmbariManagementController.class);
    final Gson gson = new Gson();
    final WidgetDAO widgetDAO = createNiceMock(WidgetDAO.class);
    final AmbariMetaInfo metaInfo = createNiceMock(AmbariMetaInfo.class);
    WidgetEntity widgetEntity = createNiceMock(WidgetEntity.class);
    WidgetEntity widgetEntity2 = createNiceMock(WidgetEntity.class);
    StackId stackId = new StackId("HDP", "2.0.0");
    StackInfo stackInfo = createNiceMock(StackInfo.class);
    ServiceInfo serviceInfo = createNiceMock(ServiceInfo.class);

    String widgetStr = "{\n" +
      "  \"layouts\": [\n" +
      "    {\n" +
      "      \"layout_name\": \"default_yarn_dashboard\",\n" +
      "      \"display_name\": \"Standard YARN Dashboard\",\n" +
      "      \"section_name\": \"YARN_SUMMARY\",\n" +
      "      \"widgetLayoutInfo\": [\n" +
      "        {\n" +
      "          \"widget_name\": \"Container Failures\",\n" +
      "          \"metrics\": [],\n" +
      "          \"values\": []\n" +
      "        }\n" +
      "      ]\n" +
      "    },\n" +
      "        {\n" +
      "      \"layout_name\": \"default_yarn_heatmap\",\n" +
      "      \"display_name\": \"Standard YARN HeatMaps\",\n" +
      "      \"section_name\": \"YARN_HEATMAPS\",\n" +
      "      \"widgetLayoutInfo\": [\n" +
      "        {\n" +
      "          \"widget_name\": \"Container Failures\",\n" +
      "          \"metrics\": [],\n" +
      "          \"values\": []\n" +
      "        }\n" +
      "      ]\n" +
      "    }\n" +
      "  ]\n" +
      "}";

    File dataDirectory = temporaryFolder.newFolder();
    File file = new File(dataDirectory, "yarn_widget.json");
    FileUtils.writeStringToFile(file, widgetStr);

    final Injector mockInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(EntityManager.class).toInstance(createNiceMock(EntityManager.class));
        bind(AmbariManagementController.class).toInstance(controller);
        bind(Clusters.class).toInstance(clusters);
        bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(Gson.class).toInstance(gson);
        bind(WidgetDAO.class).toInstance(widgetDAO);
        bind(StackManagerFactory.class).toInstance(createNiceMock(StackManagerFactory.class));
        bind(AmbariMetaInfo.class).toInstance(metaInfo);
      }
    });
    expect(controller.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", cluster);
    }}).anyTimes();
    expect(cluster.getClusterId()).andReturn(1L).anyTimes();
    expect(stackInfo.getService("YARN")).andReturn(serviceInfo);
    expect(cluster.getDesiredStackVersion()).andReturn(stackId).anyTimes();
    expect(metaInfo.getStack("HDP", "2.0.0")).andReturn(stackInfo).anyTimes();
    expect(serviceInfo.getWidgetsDescriptorFile()).andReturn(file).anyTimes();

    expect(widgetDAO.findByName(1L, "Container Failures", "ambari", "YARN_SUMMARY"))
      .andReturn(Collections.singletonList(widgetEntity));
    expect(widgetDAO.merge(widgetEntity)).andReturn(null);
    expect(widgetEntity.getWidgetName()).andReturn("Container Failures").anyTimes();

    expect(widgetDAO.findByName(1L, "Container Failures", "ambari", "YARN_HEATMAPS"))
      .andReturn(Collections.singletonList(widgetEntity2));
    expect(widgetDAO.merge(widgetEntity2)).andReturn(null);
    expect(widgetEntity2.getWidgetName()).andReturn("Container Failures").anyTimes();

    replay(clusters, cluster, controller, widgetDAO, metaInfo, widgetEntity, widgetEntity2, stackInfo, serviceInfo);

    mockInjector.getInstance(UpgradeCatalog222.class).updateYARNWidgetDefinition();

    verify(clusters, cluster, controller, widgetDAO, widgetEntity, widgetEntity2, stackInfo, serviceInfo);
  }


  @Test
  public void testHBASEWidgetUpdate() throws Exception {
    final Clusters clusters = createNiceMock(Clusters.class);
    final Cluster cluster = createNiceMock(Cluster.class);
    final AmbariManagementController controller = createNiceMock(AmbariManagementController.class);
    final Gson gson = new Gson();
    final WidgetDAO widgetDAO = createNiceMock(WidgetDAO.class);
    final AmbariMetaInfo metaInfo = createNiceMock(AmbariMetaInfo.class);
    WidgetEntity widgetEntity = createNiceMock(WidgetEntity.class);
    StackId stackId = new StackId("HDP", "2.0.0");
    StackInfo stackInfo = createNiceMock(StackInfo.class);
    ServiceInfo serviceInfo = createNiceMock(ServiceInfo.class);

    String widgetStr = "{\n" +
      "  \"layouts\": [\n" +
      "    {\n" +
      "      \"layout_name\": \"default_hbase_dashboard\",\n" +
      "      \"display_name\": \"Standard HBASE Dashboard\",\n" +
      "      \"section_name\": \"HBASE_SUMMARY\",\n" +
      "      \"widgetLayoutInfo\": [\n" +
      "        {\n" +
      "          \"widget_name\": \"Blocked Updates\",\n" +
      "          \"metrics\": [],\n" +
      "          \"values\": []\n" +
      "        }\n" +
      "      ]\n" +
      "    } " +
      "]\n" +
      "}";

    File dataDirectory = temporaryFolder.newFolder();
    File file = new File(dataDirectory, "hbase_widget.json");
    FileUtils.writeStringToFile(file, widgetStr);

    final Injector mockInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(EntityManager.class).toInstance(createNiceMock(EntityManager.class));
        bind(AmbariManagementController.class).toInstance(controller);
        bind(Clusters.class).toInstance(clusters);
        bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(Gson.class).toInstance(gson);
        bind(WidgetDAO.class).toInstance(widgetDAO);
        bind(StackManagerFactory.class).toInstance(createNiceMock(StackManagerFactory.class));
        bind(AmbariMetaInfo.class).toInstance(metaInfo);
      }
    });
    expect(controller.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", cluster);
    }}).anyTimes();
    expect(cluster.getClusterId()).andReturn(1L).anyTimes();
    expect(stackInfo.getService("HBASE")).andReturn(serviceInfo);
    expect(cluster.getDesiredStackVersion()).andReturn(stackId).anyTimes();
    expect(metaInfo.getStack("HDP", "2.0.0")).andReturn(stackInfo).anyTimes();
    expect(serviceInfo.getWidgetsDescriptorFile()).andReturn(file).anyTimes();

    expect(widgetDAO.findByName(1L, "Blocked Updates", "ambari", "HBASE_SUMMARY"))
      .andReturn(Collections.singletonList(widgetEntity));
    expect(widgetDAO.merge(widgetEntity)).andReturn(null);
    expect(widgetEntity.getWidgetName()).andReturn("Blocked Updates").anyTimes();

    replay(clusters, cluster, controller, widgetDAO, metaInfo, widgetEntity, stackInfo, serviceInfo);

    mockInjector.getInstance(UpgradeCatalog222.class).updateHBASEWidgetDefinition();

    verify(clusters, cluster, controller, widgetDAO, widgetEntity, stackInfo, serviceInfo);
  }

  @Test
  public void testGetUpdatedHbaseEnvProperties_BadConfig() {
    String badContent = "export HBASE_HEAPSIZE=1000;\n\n" +
            "export HBASE_OPTS=\"-Djava.io.tmpdir={{java_io_tmpdir}}\"\n\n" +
            "export HBASE_LOG_DIR={{log_dir}}";
    String expectedContent = "export HBASE_HEAPSIZE=1000;\n\n" +
            "export HBASE_OPTS=\"${HBASE_OPTS} -Djava.io.tmpdir={{java_io_tmpdir}}\"\n\n" +
            "export HBASE_LOG_DIR={{log_dir}}";
    testGetUpdatedHbaseEnvProperties(badContent, expectedContent);
  }

  @Test
  public void testGetUpdatedHbaseEnvProperties_GoodConfig() {

    String goodContent = "export HBASE_HEAPSIZE=1000;\n\n" +
            "export HBASE_OPTS=\"${HBASE_OPTS} -Djava.io.tmpdir={{java_io_tmpdir}}\"\n\n" +
            "export HBASE_LOG_DIR={{log_dir}}";
    testGetUpdatedHbaseEnvProperties(goodContent, null);
  }

  @Test
  public void testGetUpdatedHbaseEnvProperties_NoConfig() {
    String content = "export HBASE_HEAPSIZE=1000;\n\n" +
            "export HBASE_LOG_DIR={{log_dir}}";
    testGetUpdatedHbaseEnvProperties(content, null);
  }

  private void testGetUpdatedHbaseEnvProperties(String content, String expectedContent) {
    Module module = new Module() {
      @Override
      public void configure(Binder binder) {
        binder.bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
        binder.bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
      }
    };

    Injector injector = Guice.createInjector(module);
    UpgradeCatalog222 upgradeCatalog222 = injector.getInstance(UpgradeCatalog222.class);
    Map<String, String> update = upgradeCatalog222.getUpdatedHbaseEnvProperties(content);
    assertEquals(expectedContent, update.get("content"));
  }

  @Test
  public void testUpdateHostRoleCommands() throws Exception {
    final DBAccessor dbAccessor = createNiceMock(DBAccessor.class);
    dbAccessor.createIndex(eq("idx_hrc_status_role"), eq("host_role_command"), eq("status"), eq("role"));
    expectLastCall().once();

    replay(dbAccessor);

    Module module = new Module() {
      @Override
      public void configure(Binder binder) {
        binder.bind(DBAccessor.class).toInstance(dbAccessor);
        binder.bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
      }
    };

    Injector injector = Guice.createInjector(module);
    UpgradeCatalog222 upgradeCatalog222 = injector.getInstance(UpgradeCatalog222.class);
    upgradeCatalog222.updateHostRoleCommands();


    verify(dbAccessor);
  }

  @Test
  public void testUpdateAlerts_AtlasAlert() {
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariManagementController mockAmbariManagementController = easyMockSupport.createNiceMock(AmbariManagementController.class);
    final Clusters mockClusters = easyMockSupport.createStrictMock(Clusters.class);
    final Cluster mockClusterExpected = easyMockSupport.createNiceMock(Cluster.class);
    final AlertDefinitionDAO mockAlertDefinitionDAO = easyMockSupport.createNiceMock(AlertDefinitionDAO.class);
    final AlertDefinitionEntity atlasMetadataServerWebUIMock = easyMockSupport.createNiceMock(AlertDefinitionEntity.class);

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

    long clusterId = 1;

    expect(mockAmbariManagementController.getClusters()).andReturn(mockClusters).once();
    expect(mockClusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", mockClusterExpected);
    }}).atLeastOnce();
    expect(mockClusterExpected.getClusterId()).andReturn(clusterId).anyTimes();
    expect(mockAlertDefinitionDAO.findByName(eq(clusterId), eq("metadata_server_webui")))
            .andReturn(atlasMetadataServerWebUIMock).atLeastOnce();
    expect(atlasMetadataServerWebUIMock.getSource()).andReturn("{\"uri\": {\n" +
            "            \"http\": \"{{hostname}}:{{application-properties/atlas.server.http.port}}\",\n" +
            "            \"https\": \"{{hostname}}:{{application-properties/atlas.server.https.port}}\" } }");

    atlasMetadataServerWebUIMock.setSource("{\"uri\":{\"http\":\"{{application-properties/atlas.server.http.port}}\",\"https\":\"{{application-properties/atlas.server.https.port}}\"}}");
    expectLastCall().once();

    easyMockSupport.replayAll();
    mockInjector.getInstance(UpgradeCatalog222.class).updateAlerts();
    easyMockSupport.verifyAll();
  }

  @Test
  public void testUpdateCorruptedReplicaWidget() throws SQLException{
    final DBAccessor dbAccessor = createStrictMock(DBAccessor.class);
    Module module = new Module() {
      @Override
      public void configure(Binder binder) {
        binder.bind(DBAccessor.class).toInstance(dbAccessor);
        binder.bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        binder.bind(EntityManager.class).toInstance(entityManager);
      }
    };

    Injector injector = Guice.createInjector(module);

    String expectedWidgetUpdate = "UPDATE widget SET widget_name='%s', description='%s', " +
      "widget_values='[{\"name\": \"%s\", \"value\": \"%s\"}]' WHERE widget_name='%s'";
    Capture<String> capturedStatements = Capture.newInstance(CaptureType.ALL);

    expect(dbAccessor.executeUpdate(capture(capturedStatements))).andReturn(1);

    UpgradeCatalog222 upgradeCatalog222 = injector.getInstance(UpgradeCatalog222.class);
    replay(dbAccessor);

    upgradeCatalog222.updateCorruptedReplicaWidget();

    List<String> statements = capturedStatements.getValues();

    assertTrue(statements.contains(String.format(expectedWidgetUpdate,
      UpgradeCatalog222.WIDGET_CORRUPT_REPLICAS,
      UpgradeCatalog222.WIDGET_CORRUPT_REPLICAS_DESCRIPTION,
      UpgradeCatalog222.WIDGET_CORRUPT_REPLICAS,
      UpgradeCatalog222.WIDGET_VALUES_VALUE,
      UpgradeCatalog222.WIDGET_CORRUPT_BLOCKS)));

  }

  @Test
  public void testCreateNewSliderConfigVersion() throws AmbariException {
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariManagementController mockAmbariManagementController = easyMockSupport.createNiceMock(AmbariManagementController.class);
    final Clusters mockClusters = easyMockSupport.createStrictMock(Clusters.class);
    final Cluster mockClusterExpected = easyMockSupport.createNiceMock(Cluster.class);
    final Service mockSliderService = easyMockSupport.createNiceMock(Service.class);

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
    expect(mockClusterExpected.getService("SLIDER")).andReturn(mockSliderService);
    expect(mockClusterExpected.createServiceConfigVersion("SLIDER", "ambari-upgrade", "Creating new service config version for SLIDER service.", null)).andReturn(null).once();

    easyMockSupport.replayAll();
    mockInjector.getInstance(UpgradeCatalog222.class).createNewSliderConfigVersion();
    easyMockSupport.verifyAll();
  }

  @Test
  public void testInitializeStromAndKafkaWidgets() throws AmbariException {

    String stormServiceName = "STORM";
    String kafkaServiceName = "KAFKA";
    String hbaseServiceName = "HBASE";

    final AmbariManagementController controller = createStrictMock(AmbariManagementController.class);
    final Clusters clusters = createStrictMock(Clusters.class);
    final Cluster cluster = createStrictMock(Cluster.class);
    final Service stormService = createStrictMock(Service.class);
    final Service kafkaService = createStrictMock(Service.class);
    final Service hbaseService = createStrictMock(Service.class);
    final Map<String, Cluster> clusterMap = Collections.singletonMap("c1", cluster);
    // Use a TreeMap so we can assume a particular order when iterating over the services.
    final Map<String, Service> services = new TreeMap<>();
    services.put(stormServiceName, stormService);
    services.put(kafkaServiceName, kafkaService);
    services.put(hbaseServiceName, hbaseService);


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

    expect(cluster.getServices()).andReturn(services).once();
    expect(stormService.getName()).andReturn(stormServiceName).atLeastOnce();
    expect(kafkaService.getName()).andReturn(kafkaServiceName).atLeastOnce();
    expect(hbaseService.getName()).andReturn(hbaseServiceName).atLeastOnce();

    controller.initializeWidgetsAndLayouts(cluster, kafkaService);
    expectLastCall().once();
    controller.initializeWidgetsAndLayouts(cluster, stormService);
    expectLastCall().once();
    // but no controller call for HBase

    replay(controller, clusters, cluster, stormService, kafkaService, hbaseService);

    Injector injector = Guice.createInjector(module);
    injector.getInstance(UpgradeCatalog222.class).initializeStromAndKafkaWidgets();

    verify(controller, clusters, cluster, stormService, kafkaService, hbaseService);
  }

}
