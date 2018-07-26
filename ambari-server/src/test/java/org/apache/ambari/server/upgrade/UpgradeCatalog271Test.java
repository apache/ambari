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
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariManagementControllerImpl;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.StackId;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Assert;
import org.junit.Test;

import com.google.inject.Injector;

public class UpgradeCatalog271Test {

  @Test
  public void testExecuteDMLUpdates() throws Exception {
    Method addNewConfigurationsFromXml = AbstractUpgradeCatalog.class.getDeclaredMethod("addNewConfigurationsFromXml");
    Method updateRangerLogDirConfigs = UpgradeCatalog271.class.getDeclaredMethod("updateRangerLogDirConfigs");
    Method updateRangerKmsDbUrl = UpgradeCatalog271.class.getDeclaredMethod("updateRangerKmsDbUrl");

    UpgradeCatalog271 upgradeCatalog271 = createMockBuilder(UpgradeCatalog271.class)
      .addMockedMethod(updateRangerKmsDbUrl)
      .addMockedMethod(updateRangerLogDirConfigs)
      .addMockedMethod(addNewConfigurationsFromXml)
      .createMock();

    upgradeCatalog271.addNewConfigurationsFromXml();
    expectLastCall().once();

    upgradeCatalog271.updateRangerLogDirConfigs();
    expectLastCall().once();

    upgradeCatalog271.updateRangerKmsDbUrl();
    expectLastCall().once();

    replay(upgradeCatalog271);
    upgradeCatalog271.executeDMLUpdates();
    verify(upgradeCatalog271);
  }

  @Test
  public void testUpdateRangerLogDirConfigs() throws Exception {

    Map<String, Service> installedServices = new HashMap<String, Service>() {
      {
        put("RANGER", null);
      }
    };

    Map<String, String> rangerEnvConfig = new HashMap<String, String>() {
      {
        put("ranger_admin_log_dir", "/var/log/ranger/admin");
        put("ranger_usersync_log_dir", "/var/log/ranger/usersync");
      }
    };

    Map<String, String> oldRangerUgsyncSiteConfig = new HashMap<String, String>() {
      {
        put("ranger.usersync.logdir", "{{usersync_log_dir}}");
      }
    };

    EasyMockSupport easyMockSupport = new EasyMockSupport();

    Clusters clusters = easyMockSupport.createNiceMock(Clusters.class);
    final Cluster cluster = easyMockSupport.createNiceMock(Cluster.class);

    Injector injector = easyMockSupport.createNiceMock(Injector.class);
    AmbariManagementControllerImpl controller = createMockBuilder(AmbariManagementControllerImpl.class)
      .addMockedMethod("createConfiguration")
      .addMockedMethod("getClusters", new Class[] { })
      .addMockedMethod("createConfig")
      .createNiceMock();

    expect(injector.getInstance(AmbariManagementController.class)).andReturn(controller).anyTimes();
    expect(controller.getClusters()).andReturn(clusters).anyTimes();

    expect(clusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", cluster);
    }}).once();
    expect(cluster.getClusterName()).andReturn("cl1").anyTimes();
    expect(cluster.getServices()).andReturn(installedServices).atLeastOnce();

    Config mockRangerEnvConfig = easyMockSupport.createNiceMock(Config.class);
    expect(cluster.getDesiredConfigByType("ranger-env")).andReturn(mockRangerEnvConfig).atLeastOnce();
    expect(mockRangerEnvConfig.getProperties()).andReturn(rangerEnvConfig).anyTimes();

    Config mockRangerAdminSiteConfig = easyMockSupport.createNiceMock(Config.class);
    expect(cluster.getDesiredConfigByType("ranger-admin-site")).andReturn(mockRangerAdminSiteConfig).atLeastOnce();
    expect(mockRangerAdminSiteConfig.getProperties()).andReturn(Collections.emptyMap()).anyTimes();

    Config mockRangerUgsyncSiteConfig = easyMockSupport.createNiceMock(Config.class);
    expect(cluster.getDesiredConfigByType("ranger-ugsync-site")).andReturn(mockRangerUgsyncSiteConfig).atLeastOnce();
    expect(mockRangerUgsyncSiteConfig.getProperties()).andReturn(oldRangerUgsyncSiteConfig).anyTimes();

    Capture<Map> rangerAdminpropertiesCapture = EasyMock.newCapture();
    expect(controller.createConfig(anyObject(Cluster.class), anyObject(StackId.class), anyString(), capture(rangerAdminpropertiesCapture), anyString(),
      anyObject(Map.class))).andReturn(createNiceMock(Config.class)).once();

    Capture<Map> rangerUgsyncPropertiesCapture = EasyMock.newCapture();
    expect(controller.createConfig(anyObject(Cluster.class), anyObject(StackId.class), anyString(), capture(rangerUgsyncPropertiesCapture), anyString(),
      anyObject(Map.class))).andReturn(createNiceMock(Config.class)).once();

    Capture<Map> rangerEnvPropertiesCapture = EasyMock.newCapture();
    expect(controller.createConfig(anyObject(Cluster.class), anyObject(StackId.class), anyString(), capture(rangerEnvPropertiesCapture), anyString(),
      anyObject(Map.class))).andReturn(createNiceMock(Config.class)).once();

    replay(controller, injector, clusters, mockRangerEnvConfig, mockRangerAdminSiteConfig, mockRangerUgsyncSiteConfig, cluster);
    new UpgradeCatalog271(injector).updateRangerLogDirConfigs();
    easyMockSupport.verifyAll();

    Map<String, String> updatedRangerAdminConfig = rangerAdminpropertiesCapture.getValue();
    Assert.assertEquals(updatedRangerAdminConfig.get("ranger.logs.base.dir"), "/var/log/ranger/admin");

    Map<String, String> updatedRangerUgsyncSiteConfig = rangerUgsyncPropertiesCapture.getValue();
    Assert.assertEquals(updatedRangerUgsyncSiteConfig.get("ranger.usersync.logdir"), "/var/log/ranger/usersync");

    Map<String, String> updatedRangerEnvConfig = rangerEnvPropertiesCapture.getValue();
    Assert.assertFalse(updatedRangerEnvConfig.containsKey("ranger_admin_log_dir"));
    Assert.assertFalse(updatedRangerEnvConfig.containsKey("ranger_usersync_log_dir"));
  }

  @Test
  public void testUpdateRangerKmsDbUrl() throws Exception {

    Map<String, Service> installedServices = new HashMap<String, Service>() {
      {
        put("RANGER_KMS", null);
      }
    };

    Map<String, String> rangerKmsPropertiesConfig = new HashMap<String, String>() {
      {
        put("DB_FLAVOR", "MYSQL");
        put("db_host", "c6401.ambari.apache.org");
      }
    };

    Map<String, String> rangerKmsDbksPropertiesConfig = new HashMap<String, String>() {
      {
        put("ranger.ks.jpa.jdbc.url", "jdbc:mysql://c6401.ambari.apache.org:3546");
      }
    };

    EasyMockSupport easyMockSupport = new EasyMockSupport();

    Clusters clusters = easyMockSupport.createNiceMock(Clusters.class);
    final Cluster cluster = easyMockSupport.createNiceMock(Cluster.class);

    Injector injector = easyMockSupport.createNiceMock(Injector.class);
    AmbariManagementControllerImpl controller = createMockBuilder(AmbariManagementControllerImpl.class)
      .addMockedMethod("createConfiguration")
      .addMockedMethod("getClusters", new Class[] { })
      .addMockedMethod("createConfig")
      .createNiceMock();

    expect(injector.getInstance(AmbariManagementController.class)).andReturn(controller).anyTimes();
    expect(controller.getClusters()).andReturn(clusters).anyTimes();

    expect(clusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", cluster);
    }}).once();
    expect(cluster.getClusterName()).andReturn("cl1").once();
    expect(cluster.getServices()).andReturn(installedServices).atLeastOnce();

    Config mockRangerKmsPropertiesConfig = easyMockSupport.createNiceMock(Config.class);
    expect(cluster.getDesiredConfigByType("kms-properties")).andReturn(mockRangerKmsPropertiesConfig).atLeastOnce();

    Config mockRangerKmsEnvConfig = easyMockSupport.createNiceMock(Config.class);
    expect(cluster.getDesiredConfigByType("kms-env")).andReturn(mockRangerKmsEnvConfig).atLeastOnce();

    Config mockRangerKmsDbksConfig = easyMockSupport.createNiceMock(Config.class);
    expect(cluster.getDesiredConfigByType("dbks-site")).andReturn(mockRangerKmsDbksConfig).atLeastOnce();

    expect(mockRangerKmsPropertiesConfig.getProperties()).andReturn(rangerKmsPropertiesConfig).anyTimes();
    expect(mockRangerKmsEnvConfig.getProperties()).andReturn(Collections.emptyMap()).anyTimes();
    expect(mockRangerKmsDbksConfig.getProperties()).andReturn(rangerKmsDbksPropertiesConfig).anyTimes();

    Capture<Map> propertiesCapture = EasyMock.newCapture();
    expect(controller.createConfig(anyObject(Cluster.class), anyObject(StackId.class), anyString(), capture(propertiesCapture), anyString(),
      anyObject(Map.class))).andReturn(createNiceMock(Config.class)).once();

    replay(controller, injector, clusters, mockRangerKmsPropertiesConfig, mockRangerKmsEnvConfig, mockRangerKmsDbksConfig, cluster);
    new UpgradeCatalog271(injector).updateRangerKmsDbUrl();
    easyMockSupport.verifyAll();

    Map<String, String> updatedRangerKmsEnvConfig = propertiesCapture.getValue();
    Assert.assertEquals(updatedRangerKmsEnvConfig.get("ranger_kms_privelege_user_jdbc_url"), "jdbc:mysql://c6401.ambari.apache.org:3546");
  }

}
