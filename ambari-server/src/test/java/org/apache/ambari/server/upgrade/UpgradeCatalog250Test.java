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

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

import javax.persistence.EntityManager;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariManagementControllerImpl;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.MaintenanceStateHelper;
import org.apache.ambari.server.orm.DBAccessor;
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

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMockBuilder;
import static org.junit.Assert.assertTrue;
/**
 * {@link UpgradeCatalog250} unit tests.
 */
public class UpgradeCatalog250Test {

//  private Injector injector;
  private Provider<EntityManager> entityManagerProvider = createStrictMock(Provider.class);
  private EntityManager entityManager = createNiceMock(EntityManager.class);

  @Before
  public void init() {
    reset(entityManagerProvider);
    expect(entityManagerProvider.get()).andReturn(entityManager).anyTimes();
    replay(entityManagerProvider);

  }

  @After
  public void tearDown() {
  }

  @Test
  public void testExecuteDDLUpdates() throws Exception {
    final DBAccessor dbAccessor = createNiceMock(DBAccessor.class);

    dbAccessor.addUniqueConstraint("host_version", "UQ_host_repo", "repo_version_id", "host_id");
    expectLastCall().once();

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
    UpgradeCatalog250 upgradeCatalog250 = injector.getInstance(UpgradeCatalog250.class);
    upgradeCatalog250.executeDDLUpdates();

    verify(dbAccessor);
  }

  @Test
  public void testExecuteDMLUpdates() throws Exception {
    Method updateAmsConfigs = UpgradeCatalog250.class.getDeclaredMethod("updateAMSConfigs");

    UpgradeCatalog250 upgradeCatalog250 = createMockBuilder(UpgradeCatalog250.class)
      .addMockedMethod(updateAmsConfigs)
      .createMock();

    upgradeCatalog250.updateAMSConfigs();
    expectLastCall().once();

    replay(upgradeCatalog250);

    upgradeCatalog250.executeDMLUpdates();

    verify(upgradeCatalog250);
  }

  @Test
  public void testAmsEnvUpdateConfigs() throws Exception{

    Map<String, String> oldPropertiesAmsEnv = new HashMap<String, String>() {
      {
        put("content", "\n" +
          "# AMS Collector heapsize\n" +
          "export AMS_COLLECTOR_HEAPSIZE={{metrics_collector_heapsize}}\n" +
          "\n" +
          "# HBase normalizer enabled\n" +
          "export AMS_HBASE_NORMALIZER_ENABLED={{ams_hbase_normalizer_enabled}}\n" +
          "\n" +
          "# HBase compaction policy enabled\n" +
          "export HBASE_FIFO_COMPACTION_POLICY_ENABLED={{ams_hbase_fifo_compaction_policy_enabled}}\n" +
          "\n" +
          "# HBase Tables Initialization check enabled\n" +
          "export AMS_HBASE_INIT_CHECK_ENABLED={{ams_hbase_init_check_enabled}}\n");
      }
    };
    Map<String, String> newPropertiesAmsEnv = new HashMap<String, String>() {
      {
        put("content", "\n" +
          "# AMS Collector heapsize\n" +
          "export AMS_COLLECTOR_HEAPSIZE={{metrics_collector_heapsize}}\n" +
          "\n" +
          "# HBase Tables Initialization check enabled\n" +
          "export AMS_HBASE_INIT_CHECK_ENABLED={{ams_hbase_init_check_enabled}}\n");
      }
    };
    EasyMockSupport easyMockSupport = new EasyMockSupport();

    Clusters clusters = easyMockSupport.createNiceMock(Clusters.class);
    final Cluster cluster = easyMockSupport.createNiceMock(Cluster.class);
    Config mockAmsEnv = easyMockSupport.createNiceMock(Config.class);

    expect(clusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", cluster);
    }}).once();
    expect(cluster.getDesiredConfigByType("ams-env")).andReturn(mockAmsEnv).atLeastOnce();
    expect(mockAmsEnv.getProperties()).andReturn(oldPropertiesAmsEnv).anyTimes();

    Injector injector = easyMockSupport.createNiceMock(Injector.class);
    expect(injector.getInstance(Gson.class)).andReturn(null).anyTimes();
    expect(injector.getInstance(MaintenanceStateHelper.class)).andReturn(null).anyTimes();
    expect(injector.getInstance(KerberosHelper.class)).andReturn(createNiceMock(KerberosHelper.class)).anyTimes();

    replay(injector, clusters, mockAmsEnv, cluster);

    AmbariManagementControllerImpl controller = createMockBuilder(AmbariManagementControllerImpl.class)
      .addMockedMethod("createConfiguration")
      .addMockedMethod("getClusters", new Class[] { })
      .addMockedMethod("createConfig")
      .withConstructor(createNiceMock(ActionManager.class), clusters, injector)
      .createNiceMock();

    Injector injector2 = easyMockSupport.createNiceMock(Injector.class);
    Capture<Map> propertiesCapture = EasyMock.newCapture();

    expect(injector2.getInstance(AmbariManagementController.class)).andReturn(controller).anyTimes();
    expect(controller.getClusters()).andReturn(clusters).anyTimes();
    expect(controller.createConfig(anyObject(Cluster.class), anyString(), capture(propertiesCapture), anyString(),
      anyObject(Map.class))).andReturn(createNiceMock(Config.class)).once();

    replay(controller, injector2);
    new UpgradeCatalog250(injector2).updateAMSConfigs();
    easyMockSupport.verifyAll();

    Map<String, String> updatedProperties = propertiesCapture.getValue();
    assertTrue(Maps.difference(newPropertiesAmsEnv, updatedProperties).areEqual());
  }
}
