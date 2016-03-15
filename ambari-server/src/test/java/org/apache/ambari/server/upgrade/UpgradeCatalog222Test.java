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


import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.persist.PersistService;
import java.sql.SQLException;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
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
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.dao.WidgetDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.orm.entities.WidgetEntity;
import org.apache.ambari.server.stack.StackManagerFactory;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
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

import javax.persistence.EntityManager;
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class UpgradeCatalog222Test {
  private Injector injector;
  private Provider<EntityManager> entityManagerProvider = createStrictMock(Provider.class);
  private EntityManager entityManager = createNiceMock(EntityManager.class);
  private UpgradeCatalogHelper upgradeCatalogHelper;
  private StackEntity desiredStackEntity;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

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
  public void testExecuteDMLUpdates() throws Exception {
    Method addNewConfigurationsFromXml = AbstractUpgradeCatalog.class.getDeclaredMethod("addNewConfigurationsFromXml");
    Method updateAlerts = UpgradeCatalog222.class.getDeclaredMethod("updateAlerts");
    Method updateStormConfigs = UpgradeCatalog222.class.getDeclaredMethod("updateStormConfigs");
    Method updateAMSConfigs = UpgradeCatalog222.class.getDeclaredMethod("updateAMSConfigs");
    Method updateHiveConfigs = UpgradeCatalog222.class.getDeclaredMethod("updateHiveConfig");
    Method updateHostRoleCommands = UpgradeCatalog222.class.getDeclaredMethod("updateHostRoleCommands");
    Method updateHDFSWidget = UpgradeCatalog222.class.getDeclaredMethod("updateHDFSWidgetDefinition");
    Method updateCorruptedReplicaWidget = UpgradeCatalog222.class.getDeclaredMethod("updateCorruptedReplicaWidget");


    UpgradeCatalog222 upgradeCatalog222 = createMockBuilder(UpgradeCatalog222.class)
      .addMockedMethod(addNewConfigurationsFromXml)
      .addMockedMethod(updateAlerts)
      .addMockedMethod(updateStormConfigs)
      .addMockedMethod(updateAMSConfigs)
      .addMockedMethod(updateHiveConfigs)
      .addMockedMethod(updateHostRoleCommands)
      .addMockedMethod(updateHDFSWidget)
      .addMockedMethod(updateCorruptedReplicaWidget)
      .createMock();

    upgradeCatalog222.addNewConfigurationsFromXml();
    expectLastCall().once();
    upgradeCatalog222.updateAlerts();
    expectLastCall().once();
    upgradeCatalog222.updateStormConfigs();
    expectLastCall().once();
    upgradeCatalog222.updateAMSConfigs();
    expectLastCall().once();
    upgradeCatalog222.updateHostRoleCommands();
    expectLastCall().once();
    upgradeCatalog222.updateHiveConfig();
    expectLastCall().once();
    upgradeCatalog222.updateHDFSWidgetDefinition();
    expectLastCall().once();
    upgradeCatalog222.updateCorruptedReplicaWidget();
    expectLastCall().once();

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
  public void testAmsSiteUpdateConfigs() throws Exception{

    Map<String, String> oldPropertiesAmsSite = new HashMap<String, String>() {
      {
        put("timeline.metrics.host.aggregator.daily.checkpointCutOffMultiplier", String.valueOf(1));
        put("timeline.metrics.cluster.aggregator.daily.checkpointCutOffMultiplier", String.valueOf(1));
        put("timeline.metrics.service.operation.mode", "distributed");
        put("timeline.metrics.host.aggregator.ttl", String.valueOf(86400));
        put("timeline.metrics.host.aggregator.minute.ttl", String.valueOf(604800));
        put("timeline.metrics.host.aggregator.hourly.ttl", String.valueOf(2592000));
        put("timeline.metrics.host.aggregator.daily.ttl", String.valueOf(31536000));
        put("timeline.metrics.cluster.aggregator.second.ttl", String.valueOf(21600)); //Less than 1 day
        put("timeline.metrics.cluster.aggregator.minute.ttl", String.valueOf(7776000));
        put("timeline.metrics.cluster.aggregator.hourly.ttl", String.valueOf(31536000));
        put("timeline.metrics.cluster.aggregator.daily.ttl", String.valueOf(63072000));
      }
    };
    Map<String, String> newPropertiesAmsSite = new HashMap<String, String>() {
      {
        put("timeline.metrics.host.aggregator.daily.checkpointCutOffMultiplier", String.valueOf(2));
        put("timeline.metrics.cluster.aggregator.daily.checkpointCutOffMultiplier", String.valueOf(2));
        put("timeline.metrics.service.watcher.disabled", String.valueOf(false));
        put("timeline.metrics.host.aggregator.ttl", String.valueOf(7 * 86400));
        put("timeline.metrics.host.aggregator.minute.ttl", String.valueOf(7 * 86400));
        put("timeline.metrics.host.aggregator.hourly.ttl", String.valueOf(30 * 86400));
        put("timeline.metrics.host.aggregator.daily.ttl", String.valueOf(365 * 86400));
        put("timeline.metrics.cluster.aggregator.second.ttl", String.valueOf(21600));
        put("timeline.metrics.cluster.aggregator.minute.ttl", String.valueOf(30 * 86400));
        put("timeline.metrics.cluster.aggregator.hourly.ttl", String.valueOf(365 * 86400));
        put("timeline.metrics.cluster.aggregator.daily.ttl", String.valueOf(730 * 86400));
        put("timeline.metrics.service.operation.mode", "distributed");
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
    expect(controller.createConfiguration(capture(configurationRequestCapture))).andReturn(configurationResponseMock)
      .anyTimes();

    replay(controller, injector2, configurationResponseMock);
    new UpgradeCatalog222(injector2).updateAMSConfigs();
    easyMockSupport.verifyAll();

    ConfigurationRequest configurationRequest = configurationRequestCapture.getValue();
    Map<String, String> updatedProperties = configurationRequest.getProperties();
    assertTrue(Maps.difference(newPropertiesAmsSite, updatedProperties).areEqual());
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
    StackId stackId = new StackId("HDP", "2.0.0");
    StackInfo stackInfo = createNiceMock(StackInfo.class);
    ServiceInfo serviceInfo = createNiceMock(ServiceInfo.class);

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
    expect(stackInfo.getService("HDFS")).andReturn(serviceInfo);
    expect(widgetDAO.findByName(1L, "NameNode RPC", "ambari", "HDFS_SUMMARY"))
      .andReturn(Collections.singletonList(widgetEntity));
    expect(cluster.getDesiredStackVersion()).andReturn(stackId);
    expect(metaInfo.getStack("HDP", "2.0.0")).andReturn(stackInfo);
    expect(serviceInfo.getWidgetsDescriptorFile()).andReturn(file);
    expect(widgetDAO.merge(widgetEntity)).andReturn(null);
    expect(widgetEntity.getWidgetName()).andReturn("Namenode RPC").anyTimes();

    replay(clusters, cluster, controller, widgetDAO, metaInfo, widgetEntity, stackInfo, serviceInfo);

    mockInjector.getInstance(UpgradeCatalog222.class).updateHDFSWidgetDefinition();

    verify(clusters, cluster, controller, widgetDAO, widgetEntity, stackInfo, serviceInfo);
  }

  @Test
  public void testUpdateHostRoleCommands() throws Exception {
    final DBAccessor dbAccessor = createNiceMock(DBAccessor.class);
    dbAccessor.createIndex(eq("idx_hrc_status"), eq("host_role_command"), eq("status"), eq("role"));
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

}
