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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;

import com.google.inject.AbstractModule;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.stack.OsFamily;
import org.easymock.EasyMock;
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
 * {@link org.apache.ambari.server.upgrade.UpgradeCatalog213} unit tests.
 */
public class UpgradeCatalog213Test {
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
    Method addMissingConfigs = UpgradeCatalog213.class.getDeclaredMethod("addMissingConfigs");
    Method updateAMSConfigs = UpgradeCatalog213.class.getDeclaredMethod("updateAMSConfigs");
    Method updateAlertDefinitions = UpgradeCatalog213.class.getDeclaredMethod("updateAlertDefinitions");

    UpgradeCatalog213 upgradeCatalog213 = createMockBuilder(UpgradeCatalog213.class)
        .addMockedMethod(addMissingConfigs)
        .addMockedMethod(updateAMSConfigs)
        .addMockedMethod(updateAlertDefinitions)
        .createMock();

    upgradeCatalog213.addMissingConfigs();
    expectLastCall().once();
    upgradeCatalog213.updateAMSConfigs();
    expectLastCall().once();
    upgradeCatalog213.updateAlertDefinitions();
    expectLastCall().once();

    replay(upgradeCatalog213);

    upgradeCatalog213.executeDMLUpdates();

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

    final Map<String, String> propertiesExpectedHiveSite = new HashMap<String, String>() {{
      put("nimbus.monitor.freq.secs", "210");
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

    expect(mockClusterExpected.getDesiredConfigByType("storm-site")).andReturn(mockStormSite).atLeastOnce();
    expect(mockStormSite.getProperties()).andReturn(propertiesExpectedHiveSite).atLeastOnce();

    easyMockSupport.replayAll();
    mockInjector.getInstance(UpgradeCatalog213.class).updateStormConfigs();
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
  public void testModifyJournalnodeProcessAlertSource() throws Exception {
    UpgradeCatalog213 upgradeCatalog213 = new UpgradeCatalog213(injector);
    String alertSource = "{\"uri\":\"{{hdfs-site/dfs.journalnode.http-address}}\",\"default_port\":8480," +
            "\"type\":\"PORT\",\"reporting\":{\"ok\":{\"text\":\"TCP OK - {0:.3f}s response on port {1}\"}," +
            "\"warning\":{\"text\":\"TCP OK - {0:.3f}s response on port {1}\",\"value\":1.5}," +
            "\"critical\":{\"text\":\"Connection failed: {0} to {1}:{2}\",\"value\":5.0}}}";
    String expected = "{\"reporting\":{\"ok\":{\"text\":\"HTTP {0} response in {2:.3f}s\"}," +
            "\"warning\":{\"text\":\"HTTP {0} response from {1} in {2:.3f}s ({3})\"}," +
            "\"critical\":{\"text\":\"Connection failed to {1} ({3})\"}},\"type\":\"WEB\"," +
            "\"uri\":{\"http\":\"{{hdfs-site/dfs.journalnode.http-address}}\"," +
            "\"https\":\"{{hdfs-site/dfs.journalnode.https-address}}\"," +
            "\"kerberos_keytab\":\"{{hdfs-site/dfs.web.authentication.kerberos.keytab}}\"," +
            "\"kerberos_principal\":\"{{hdfs-site/dfs.web.authentication.kerberos.principal}}\"," +
            "\"https_property\":\"{{hdfs-site/dfs.http.policy}}\"," +
            "\"https_property_value\":\"HTTPS_ONLY\",\"connection_timeout\":5.0}}";
    Assert.assertEquals(expected, upgradeCatalog213.modifyJournalnodeProcessAlertSource(alertSource));
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
    return injector.getInstance(UpgradeCatalog213.class);
  }

  @Test
  public void testGetSourceVersion() {
    final DBAccessor dbAccessor = createNiceMock(DBAccessor.class);
    UpgradeCatalog upgradeCatalog = getUpgradeCatalog(dbAccessor);
    Assert.assertEquals("2.1.2", upgradeCatalog.getSourceVersion());
  }

  @Test
  public void testGetTargetVersion() throws Exception {
    final DBAccessor dbAccessor = createNiceMock(DBAccessor.class);
    UpgradeCatalog upgradeCatalog = getUpgradeCatalog(dbAccessor);

    Assert.assertEquals("2.1.3", upgradeCatalog.getTargetVersion());
  }
}
