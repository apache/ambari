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
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.persist.PersistService;
import junit.framework.Assert;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ConfigurationRequest;
import org.apache.ambari.server.controller.ConfigurationResponse;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.controller.AmbariManagementControllerImpl;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.MaintenanceStateHelper;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.stack.OsFamily;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.easymock.EasyMock.capture;
import static org.junit.Assert.assertTrue;

public class UpgradeCatalog221Test {
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
  public void testExecuteDMLUpdates() throws Exception {
    Method addNewConfigurationsFromXml = AbstractUpgradeCatalog.class.getDeclaredMethod("addNewConfigurationsFromXml");
    Method updateAlerts = UpgradeCatalog221.class.getDeclaredMethod("updateAlerts");
    Method updateOozieConfigs = UpgradeCatalog221.class.getDeclaredMethod("updateOozieConfigs");

    UpgradeCatalog221 upgradeCatalog221 = createMockBuilder(UpgradeCatalog221.class)
      .addMockedMethod(addNewConfigurationsFromXml)
      .addMockedMethod(updateAlerts)
      .addMockedMethod(updateOozieConfigs)
      .createMock();

    upgradeCatalog221.addNewConfigurationsFromXml();
    expectLastCall().once();
    upgradeCatalog221.updateAlerts();
    expectLastCall().once();
    upgradeCatalog221.updateOozieConfigs();
    expectLastCall().once();


    replay(upgradeCatalog221);

    upgradeCatalog221.executeDMLUpdates();

    verify(upgradeCatalog221);
  }

  @Test
  public void test_AddCheckCommandTimeoutParam_ParamsNotAvailable() {

    UpgradeCatalog221 upgradeCatalog221 = new UpgradeCatalog221(injector);
    String inputSource = "{ \"path\" : \"test_path\", \"type\" : \"SCRIPT\"}";
    String expectedSource = "{\"path\":\"test_path\",\"type\":\"SCRIPT\",\"parameters\":[{\"name\":\"check.command.timeout\",\"display_name\":\"Check command timeout\",\"value\":60.0,\"type\":\"NUMERIC\",\"description\":\"The maximum time before check command will be killed by timeout\",\"units\":\"seconds\"}]}";

    String result = upgradeCatalog221.addCheckCommandTimeoutParam(inputSource);
    Assert.assertEquals(result, expectedSource);
  }

  @Test
  public void test_AddCheckCommandTimeoutParam_ParamsAvailable() {

    UpgradeCatalog221 upgradeCatalog221 = new UpgradeCatalog221(injector);
    String inputSource = "{\"path\":\"test_path\",\"type\":\"SCRIPT\",\"parameters\":[{\"name\":\"test\",\"display_name\":\"Test\",\"value\":10.0,\"type\":\"test\",\"description\":\"test\",\"units\":\"test\"}]}";
    String expectedSource = "{\"path\":\"test_path\",\"type\":\"SCRIPT\",\"parameters\":[{\"name\":\"test\",\"display_name\":\"Test\",\"value\":10.0,\"type\":\"test\",\"description\":\"test\",\"units\":\"test\"},{\"name\":\"check.command.timeout\",\"display_name\":\"Check command timeout\",\"value\":60.0,\"type\":\"NUMERIC\",\"description\":\"The maximum time before check command will be killed by timeout\",\"units\":\"seconds\"}]}";

    String result = upgradeCatalog221.addCheckCommandTimeoutParam(inputSource);
    Assert.assertEquals(result, expectedSource);
  }

  @Test
  public void test_AddCheckCommandTimeoutParam_NeededParamAlreadyAdded() {

    UpgradeCatalog221 upgradeCatalog221 = new UpgradeCatalog221(injector);
    String inputSource = "{\"path\":\"test_path\",\"type\":\"SCRIPT\",\"parameters\":[{\"display_name\":\"Test\",\"value\":10.0,\"type\":\"test\",\"description\":\"test\",\"units\":\"test\"},{\"name\":\"check.command.timeout\",\"display_name\":\"Check command timeout\",\"value\":60.0,\"type\":\"NUMERIC\",\"description\":\"The maximum time before check command will be killed by timeout\",\"units\":\"seconds\"}]}";
    String expectedSource = "{\"path\":\"test_path\",\"type\":\"SCRIPT\",\"parameters\":[{\"display_name\":\"Test\",\"value\":10.0,\"type\":\"test\",\"description\":\"test\",\"units\":\"test\"},{\"name\":\"check.command.timeout\",\"display_name\":\"Check command timeout\",\"value\":60.0,\"type\":\"NUMERIC\",\"description\":\"The maximum time before check command will be killed by timeout\",\"units\":\"seconds\"}]}";

    String result = upgradeCatalog221.addCheckCommandTimeoutParam(inputSource);
    Assert.assertEquals(result, expectedSource);
  }

  @Test
  public void testUpdateOozieConfigs() throws Exception {
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariManagementController mockAmbariManagementController = easyMockSupport.createNiceMock(AmbariManagementController.class);
    final Clusters mockClusters = easyMockSupport.createStrictMock(Clusters.class);
    final Cluster mockClusterExpected = easyMockSupport.createNiceMock(Cluster.class);

    final Config oozieSiteConf = easyMockSupport.createNiceMock(Config.class);
    final Map<String, String> propertiesOozieSite = new HashMap<String, String>() {{
      put("oozie.service.HadoopAccessorService.hadoop.configurations", "*=/etc/hadoop/conf");
    }};

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
    expect(mockClusterExpected.getDesiredConfigByType("oozie-site")).andReturn(oozieSiteConf).atLeastOnce();
    expect(oozieSiteConf.getProperties()).andReturn(propertiesOozieSite).once();

    UpgradeCatalog221 upgradeCatalog221 = createMockBuilder(UpgradeCatalog221.class)
        .withConstructor(Injector.class)
        .withArgs(mockInjector)
        .addMockedMethod("updateConfigurationPropertiesForCluster", Cluster.class, String.class,
            Map.class, boolean.class, boolean.class)
        .createMock();
    upgradeCatalog221.updateConfigurationPropertiesForCluster(mockClusterExpected, "oozie-site",
        Collections.singletonMap("oozie.service.HadoopAccessorService.hadoop.configurations", "*={{hadoop_conf_dir}}"),
        true, false);
    expectLastCall().once();

    easyMockSupport.replayAll();
    replay(upgradeCatalog221);
    upgradeCatalog221.updateOozieConfigs();
    easyMockSupport.verifyAll();
  }

  @Test
  public void testUpdateAmsHbaseSiteConfigs() throws Exception {

    Map<String, String> clusterEnvProperties = new HashMap<String, String>();
    Map<String, String> amsHbaseSecuritySite = new HashMap<String, String>();
    Map<String, String> newPropertiesAmsHbaseSite = new HashMap<String, String>();

    //Unsecure
    amsHbaseSecuritySite.put("zookeeper.znode.parent", "/ams-hbase-unsecure");
    newPropertiesAmsHbaseSite.put("zookeeper.znode.parent", "/ams-hbase-unsecure");
    testAmsHbaseSiteUpdates(new HashMap<String, String>(),
      newPropertiesAmsHbaseSite,
      amsHbaseSecuritySite,
      clusterEnvProperties);

    //Secure
    amsHbaseSecuritySite.put("zookeeper.znode.parent", "/ams-hbase-secure");
    newPropertiesAmsHbaseSite.put("zookeeper.znode.parent", "/ams-hbase-secure");
    testAmsHbaseSiteUpdates(new HashMap<String, String>(),
      newPropertiesAmsHbaseSite,
      amsHbaseSecuritySite,
      clusterEnvProperties);

    //Unsecure with empty value
    clusterEnvProperties.put("security_enabled","false");
    amsHbaseSecuritySite.put("zookeeper.znode.parent", "");
    newPropertiesAmsHbaseSite.put("zookeeper.znode.parent", "/ams-hbase-unsecure");
    testAmsHbaseSiteUpdates(new HashMap<String, String>(),
      newPropertiesAmsHbaseSite,
      amsHbaseSecuritySite,
      clusterEnvProperties);

    //Secure with /hbase value
    clusterEnvProperties.put("security_enabled","true");
    amsHbaseSecuritySite.put("zookeeper.znode.parent", "/hbase");
    newPropertiesAmsHbaseSite.put("zookeeper.znode.parent", "/ams-hbase-secure");
    testAmsHbaseSiteUpdates(new HashMap<String, String>(),
      newPropertiesAmsHbaseSite,
      amsHbaseSecuritySite,
      clusterEnvProperties);

  }

  private void testAmsHbaseSiteUpdates(Map<String, String> oldPropertiesAmsHbaseSite,
                                       Map<String, String> newPropertiesAmsHbaseSite,
                                       Map<String, String> amsHbaseSecuritySiteProperties,
                                       Map<String, String> clusterEnvProperties ) throws AmbariException {

    EasyMockSupport easyMockSupport = new EasyMockSupport();
    Clusters clusters = easyMockSupport.createNiceMock(Clusters.class);
    final Cluster cluster = easyMockSupport.createNiceMock(Cluster.class);
    expect(clusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", cluster);
    }}).once();

    Config mockAmsHbaseSite = easyMockSupport.createNiceMock(Config.class);
    expect(cluster.getDesiredConfigByType("ams-hbase-site")).andReturn(mockAmsHbaseSite).atLeastOnce();
    expect(mockAmsHbaseSite.getProperties()).andReturn(oldPropertiesAmsHbaseSite).times(2);

    Config mockAmsHbaseSecuritySite = easyMockSupport.createNiceMock(Config.class);
    expect(cluster.getDesiredConfigByType("ams-hbase-security-site")).andReturn(mockAmsHbaseSecuritySite).anyTimes();
    expect(mockAmsHbaseSecuritySite.getProperties()).andReturn(amsHbaseSecuritySiteProperties).anyTimes();

    Config clusterEnv = easyMockSupport.createNiceMock(Config.class);
    expect(cluster.getDesiredConfigByType("cluster-env")).andReturn(clusterEnv).anyTimes();
    expect(clusterEnv.getProperties()).andReturn(clusterEnvProperties).anyTimes();

    Injector injector = easyMockSupport.createNiceMock(Injector.class);
    expect(injector.getInstance(Gson.class)).andReturn(null).anyTimes();
    expect(injector.getInstance(MaintenanceStateHelper.class)).andReturn(null).anyTimes();
    expect(injector.getInstance(KerberosHelper.class)).andReturn(createNiceMock(KerberosHelper.class)).anyTimes();

    replay(injector, clusters, mockAmsHbaseSite, mockAmsHbaseSecuritySite, clusterEnv, cluster);

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
    expect(controller.createConfiguration(capture(configurationRequestCapture)))
      .andReturn(configurationResponseMock).anyTimes();

    replay(controller, injector2, configurationResponseMock);
    new UpgradeCatalog221(injector2).updateAMSConfigs();
    easyMockSupport.verifyAll();

    ConfigurationRequest configurationRequest = configurationRequestCapture.getValue();
    Map<String, String> updatedProperties = configurationRequest.getProperties();
    assertTrue(Maps.difference(newPropertiesAmsHbaseSite, updatedProperties).areEqual());
  }

  @Test
  public void testUpdateAmsHbaseSecuritySiteConfigs() throws Exception{

    Map<String, String> oldPropertiesAmsHbaseSecuritySite = new HashMap<String, String>() {
      {
        put("zookeeper.znode.parent", "/ams-hbase-secure");
      }
    };

    Map<String, String> newPropertiesAmsHbaseSecuritySite = new HashMap<String, String>() {
      {
      }
    };

    EasyMockSupport easyMockSupport = new EasyMockSupport();
    Clusters clusters = easyMockSupport.createNiceMock(Clusters.class);
    final Cluster cluster = easyMockSupport.createNiceMock(Cluster.class);
    Config mockAmsHbaseSecuritySite = easyMockSupport.createNiceMock(Config.class);

    expect(clusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", cluster);
    }}).once();

    expect(cluster.getDesiredConfigByType("ams-hbase-security-site")).andReturn(mockAmsHbaseSecuritySite).atLeastOnce();
    expect(mockAmsHbaseSecuritySite.getProperties()).andReturn(oldPropertiesAmsHbaseSecuritySite).times(2);

    Injector injector = easyMockSupport.createNiceMock(Injector.class);
    expect(injector.getInstance(Gson.class)).andReturn(null).anyTimes();
    expect(injector.getInstance(MaintenanceStateHelper.class)).andReturn(null).anyTimes();
    expect(injector.getInstance(KerberosHelper.class)).andReturn(createNiceMock(KerberosHelper.class)).anyTimes();

    replay(injector, clusters, mockAmsHbaseSecuritySite, cluster);

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
    new UpgradeCatalog221(injector2).updateAMSConfigs();
    easyMockSupport.verifyAll();

    ConfigurationRequest configurationRequest = configurationRequestCapture.getValue();
    Map<String, String> updatedProperties = configurationRequest.getProperties();
    assertTrue(Maps.difference(newPropertiesAmsHbaseSecuritySite, updatedProperties).areEqual());

  }

  @Test
  public void testUpdateAmsHbaseEnvContent() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method updateAmsHbaseEnvContent = UpgradeCatalog221.class.getDeclaredMethod("updateAmsHbaseEnvContent", String.class);
    UpgradeCatalog221 upgradeCatalog221 = new UpgradeCatalog221(injector);
    String oldContent = "some_content\n" +
      "{% if security_enabled %}\n" +
      "export HBASE_OPTS=\"$HBASE_OPTS -Djava.security.auth.login.config={{client_jaas_config_file}} -Dzookeeper.sasl.client.username={{zk_servicename}}\"\n" +
      "export HBASE_MASTER_OPTS=\"$HBASE_MASTER_OPTS -Djava.security.auth.login.config={{master_jaas_config_file}} -Dzookeeper.sasl.client.username={{zk_servicename}}\"\n" +
      "export HBASE_REGIONSERVER_OPTS=\"$HBASE_REGIONSERVER_OPTS -Djava.security.auth.login.config={{regionserver_jaas_config_file}} -Dzookeeper.sasl.client.username={{zk_servicename}}\"\n" +
      "export HBASE_ZOOKEEPER_OPTS=\"$HBASE_ZOOKEEPER_OPTS -Djava.security.auth.login.config={{ams_zookeeper_jaas_config_file}} -Dzookeeper.sasl.client.username={{zk_servicename}}\"\n" +
      "{% endif %}";

    String expectedContent = "some_content\n" +
      "{% if security_enabled %}\n" +
      "export HBASE_OPTS=\"$HBASE_OPTS -Djava.security.auth.login.config={{client_jaas_config_file}}\"\n" +
      "export HBASE_MASTER_OPTS=\"$HBASE_MASTER_OPTS -Djava.security.auth.login.config={{master_jaas_config_file}}\"\n" +
      "export HBASE_REGIONSERVER_OPTS=\"$HBASE_REGIONSERVER_OPTS -Djava.security.auth.login.config={{regionserver_jaas_config_file}}\"\n" +
      "export HBASE_ZOOKEEPER_OPTS=\"$HBASE_ZOOKEEPER_OPTS -Djava.security.auth.login.config={{ams_zookeeper_jaas_config_file}}\"\n" +
      "{% endif %}";

    String result = (String) updateAmsHbaseEnvContent.invoke(upgradeCatalog221, oldContent);
    Assert.assertEquals(expectedContent, result);
  }

  @Test
  public void testUpdateAmsEnvContent() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
  {
    Method updateAmsEnvContent = UpgradeCatalog221.class.getDeclaredMethod("updateAmsEnvContent", String.class);
    UpgradeCatalog221 upgradeCatalog221 = new UpgradeCatalog221(injector);
    String oldContent = "some_content\n" +
      "# AMS Collector options\n" +
      "export AMS_COLLECTOR_OPTS=\"-Djava.library.path=/usr/lib/ams-hbase/lib/hadoop-native\"\n" +
      "{% if security_enabled %}\n" +
      "export AMS_COLLECTOR_OPTS=\"$AMS_COLLECTOR_OPTS -Djava.security.auth.login.config={{ams_collector_jaas_config_file}} " +
      "-Dzookeeper.sasl.client.username={{zk_servicename}}\"\n" +
      "{% endif %}";

    String expectedContent = "some_content\n" +
      "# AMS Collector options\n" +
      "export AMS_COLLECTOR_OPTS=\"-Djava.library.path=/usr/lib/ams-hbase/lib/hadoop-native\"\n" +
      "{% if security_enabled %}\n" +
      "export AMS_COLLECTOR_OPTS=\"$AMS_COLLECTOR_OPTS -Djava.security.auth.login.config={{ams_collector_jaas_config_file}}\"\n" +
      "{% endif %}";

    String result = (String) updateAmsEnvContent.invoke(upgradeCatalog221, oldContent);
    Assert.assertEquals(expectedContent, result);
  }
}
