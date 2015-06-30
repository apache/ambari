/**
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
package org.apache.ambari.server.state;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ClusterRequest;
import org.apache.ambari.server.controller.ConfigurationRequest;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.stack.HostsType;
import org.apache.ambari.server.stack.MasterHostResolver;
import org.apache.ambari.server.state.UpgradeHelper.UpgradeGroupHolder;
import org.apache.ambari.server.state.stack.UpgradePack;
import org.apache.ambari.server.state.stack.upgrade.ConfigureTask;
import org.apache.ambari.server.state.stack.upgrade.Direction;
import org.apache.ambari.server.state.stack.upgrade.ManualTask;
import org.apache.ambari.server.state.stack.upgrade.StageWrapper;
import org.apache.ambari.server.state.stack.upgrade.Task;
import org.apache.ambari.server.state.stack.upgrade.TaskWrapper;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.persist.PersistService;
import com.google.inject.util.Modules;

/**
 * Tests the {@link UpgradeHelper} class
 */
public class UpgradeHelperTest {

  private static final StackId HDP_21 = new StackId("HPD-2.1.1");
  private static final StackId HDP_22 = new StackId("HPD-2.2.0");
  private static final String UPGRADE_VERSION = "2.2.1.0-1234";
  private static final String DOWNGRADE_VERSION = "2.2.0.0-1234";

  private Injector injector;
  private AmbariMetaInfo ambariMetaInfo;
  private OrmTestHelper helper;
  private MasterHostResolver m_masterHostResolver;
  private UpgradeHelper m_upgradeHelper;
  private ConfigHelper m_configHelper;
  private AmbariManagementController m_managementController;
  private Gson m_gson = new Gson();

  @Before
  public void before() throws Exception {
    // configure the mock to return data given a specific placeholder
    m_configHelper = EasyMock.createNiceMock(ConfigHelper.class);

    expect(
        m_configHelper.getPlaceholderValueFromDesiredConfigurations(
            EasyMock.anyObject(Cluster.class), EasyMock.eq("{{foo/bar}}"))).andReturn(
        "placeholder-rendered-properly").anyTimes();

    replay(m_configHelper);

    // create an injector which will inject the mocks
    injector = Guice.createInjector(Modules.override(
        new InMemoryDefaultTestModule()).with(new MockModule()));

    injector.getInstance(GuiceJpaInitializer.class);

    helper = injector.getInstance(OrmTestHelper.class);
    ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);

    m_upgradeHelper = injector.getInstance(UpgradeHelper.class);
    m_masterHostResolver = EasyMock.createMock(MasterHostResolver.class);
    m_managementController = injector.getInstance(AmbariManagementController.class);
  }

  @After
  public void teardown() {
    injector.getInstance(PersistService.class).stop();
  }

  @Test
  public void testUpgradeOrchestration() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("foo", "bar");
    assertTrue(upgrades.isEmpty());

    upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");

    ServiceInfo si = ambariMetaInfo.getService("HDP", "2.1.1", "ZOOKEEPER");
    si.setDisplayName("Zk");
    ComponentInfo ci = si.getComponentByName("ZOOKEEPER_SERVER");
    ci.setDisplayName("ZooKeeper1 Server2");


    assertTrue(upgrades.containsKey("upgrade_test"));
    UpgradePack upgrade = upgrades.get("upgrade_test");
    assertNotNull(upgrade);

    makeCluster();

    UpgradeContext context = new UpgradeContext(m_masterHostResolver, HDP_21,
        HDP_21, UPGRADE_VERSION, Direction.UPGRADE);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    assertEquals(6, groups.size());

    assertEquals("PRE_CLUSTER", groups.get(0).name);
    assertEquals("ZOOKEEPER", groups.get(1).name);
    assertEquals("CORE_MASTER", groups.get(2).name);
    assertEquals("CORE_SLAVES", groups.get(3).name);
    assertEquals("HIVE", groups.get(4).name);

    UpgradeGroupHolder holder = groups.get(2);
    boolean found = false;
    for (StageWrapper sw : holder.items) {
      if (sw.getTasksJson().contains("Upgrading your database")) {
        found = true;
      }
    }
    assertTrue("Expected to find replaced text for Upgrading", found);

    UpgradeGroupHolder group = groups.get(1);
    // check that the display name is being used
    assertTrue(group.items.get(1).getText().contains("ZooKeeper1 Server2"));
    assertEquals(group.items.get(5).getText(), "Service Check Zk");

    group = groups.get(3);
    assertEquals(8, group.items.size());
    StageWrapper sw = group.items.get(3);
    assertEquals("Validate Partial Upgrade", sw.getText());
    assertEquals(1, sw.getTasks().size());
    assertEquals(1, sw.getTasks().get(0).getTasks().size());
    Task t = sw.getTasks().get(0).getTasks().get(0);
    assertEquals(ManualTask.class, t.getClass());
    ManualTask mt = (ManualTask) t;
    assertTrue(mt.message.contains("DataNode and NodeManager"));
    assertNotNull(mt.structuredOut);
    assertTrue(mt.structuredOut.contains("DATANODE"));
    assertTrue(mt.structuredOut.contains("NODEMANAGER"));

    UpgradeGroupHolder postGroup = groups.get(5);
    assertEquals(postGroup.name, "POST_CLUSTER");
    assertEquals(postGroup.title, "Finalize Upgrade");
    assertEquals(postGroup.items.size(), 3);
    assertEquals(postGroup.items.get(0).getText(), "Confirm Finalize");
    assertEquals(postGroup.items.get(1).getText(), "Execute HDFS Finalize");
    assertEquals(postGroup.items.get(2).getText(), "Save Cluster State");
    assertEquals(postGroup.items.get(2).getType(), StageWrapper.Type.SERVER_SIDE_ACTION);

    assertEquals(4, groups.get(0).items.size());
    assertEquals(6, groups.get(1).items.size());
    assertEquals(8, groups.get(2).items.size());
    assertEquals(8, groups.get(3).items.size());
  }

  /**
   * Verify that a Rolling Upgrades restarts the NameNodes in the following order: standby, active.
   * @throws Exception
   */
  @Test
  public void testNamenodeOrder() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.containsKey("upgrade_test"));
    UpgradePack upgrade = upgrades.get("upgrade_test");
    assertNotNull(upgrade);

    makeCluster();

    UpgradeContext context = new UpgradeContext(m_masterHostResolver, HDP_21,
        HDP_21, UPGRADE_VERSION, Direction.UPGRADE);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    assertEquals(6, groups.size());

    UpgradeGroupHolder mastersGroup = groups.get(2);
    assertEquals("CORE_MASTER", mastersGroup.name);

    List<String> orderedNameNodes = new LinkedList<String>();
    for (StageWrapper sw : mastersGroup.items) {
      if (sw.getType().equals(StageWrapper.Type.RESTART)) {
        for (TaskWrapper tw : sw.getTasks()) {
          for (String hostName : tw.getHosts()) {
            orderedNameNodes.add(hostName);
          }
        }
      }
    }

    assertEquals(2, orderedNameNodes.size());
    // Order is standby, then active.
    assertEquals("h2", orderedNameNodes.get(0));
    assertEquals("h1", orderedNameNodes.get(1));
  }

  @Test
  public void testUpgradeOrchestrationWithNoHeartbeat() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("foo", "bar");
    assertTrue(upgrades.isEmpty());

    upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.containsKey("upgrade_test"));
    UpgradePack upgrade = upgrades.get("upgrade_test");
    assertNotNull(upgrade);

    Cluster cluster = makeCluster(false);

    Clusters clusters = injector.getInstance(Clusters.class);
    Host h4 = clusters.getHost("h4");
    h4.setState(HostState.HEARTBEAT_LOST);
    h4.persist();


    List<ServiceComponentHost> schs = cluster.getServiceComponentHosts("h4");
    assertEquals(1, schs.size());
    assertEquals(HostState.HEARTBEAT_LOST, schs.get(0).getHostState());

    UpgradeContext context = new UpgradeContext(m_masterHostResolver, HDP_21,
        HDP_21, UPGRADE_VERSION, Direction.UPGRADE);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    assertEquals(6, groups.size());

    assertEquals("PRE_CLUSTER", groups.get(0).name);
    assertEquals("ZOOKEEPER", groups.get(1).name);
    assertEquals("CORE_MASTER", groups.get(2).name);
    assertEquals("CORE_SLAVES", groups.get(3).name);

    UpgradeGroupHolder postGroup = groups.get(5);
    assertEquals("POST_CLUSTER", postGroup.name);
    assertEquals("Finalize Upgrade", postGroup.title);
    assertEquals(4, postGroup.items.size());
    assertEquals("Check Unhealthy Hosts", postGroup.items.get(0).getText());
    assertEquals("Confirm Finalize", postGroup.items.get(1).getText());
    assertEquals("Execute HDFS Finalize", postGroup.items.get(2).getText());
    assertEquals("Save Cluster State", postGroup.items.get(3).getText());
    assertEquals(StageWrapper.Type.SERVER_SIDE_ACTION, postGroup.items.get(3).getType());

    assertEquals(6, groups.get(1).items.size());
    assertEquals(8, groups.get(2).items.size());
    assertEquals(7, groups.get(3).items.size());
  }

  @Test
  public void testDowngradeOrchestration() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.containsKey("upgrade_test"));
    UpgradePack upgrade = upgrades.get("upgrade_test");
    assertNotNull(upgrade);

    makeCluster();

    UpgradeContext context = new UpgradeContext(m_masterHostResolver, HDP_21,
        HDP_21, DOWNGRADE_VERSION, Direction.DOWNGRADE);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    assertEquals(6, groups.size());

    UpgradeGroupHolder preGroup = groups.get(0);
    assertEquals("PRE_CLUSTER", preGroup.name);
    assertEquals("HIVE", groups.get(1).name);
    assertEquals("CORE_SLAVES", groups.get(2).name);
    assertEquals("CORE_MASTER", groups.get(3).name);
    assertEquals("ZOOKEEPER", groups.get(4).name);


    UpgradeGroupHolder postGroup = groups.get(5);
    assertEquals("POST_CLUSTER", postGroup.name);
    assertEquals("Finalize Downgrade", postGroup.title);
    assertEquals(3, postGroup.items.size());
    assertEquals("Confirm Finalize", postGroup.items.get(0).getText());
    assertEquals("Execute HDFS Finalize", postGroup.items.get(1).getText());
    assertEquals("Save Cluster State", postGroup.items.get(2).getText());
    assertEquals(StageWrapper.Type.SERVER_SIDE_ACTION, postGroup.items.get(2).getType());

    assertEquals(4, groups.get(0).items.size());
    assertEquals(2, groups.get(1).items.size());
    assertEquals(8, groups.get(2).items.size());
    assertEquals(7, groups.get(3).items.size());
    assertEquals(5, groups.get(4).items.size());
  }

  @Test
  public void testBuckets() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.containsKey("upgrade_bucket_test"));
    UpgradePack upgrade = upgrades.get("upgrade_bucket_test");
    assertNotNull(upgrade);

    makeCluster();

    UpgradeContext context = new UpgradeContext(m_masterHostResolver, HDP_21,
        HDP_21, UPGRADE_VERSION, Direction.UPGRADE);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    assertEquals(1, groups.size());
    UpgradeGroupHolder group = groups.iterator().next();

    assertEquals(6, group.items.size());
  }

  @Test
  public void testManualTaskPostProcessing() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.containsKey("upgrade_test"));
    UpgradePack upgrade = upgrades.get("upgrade_test");
    assertNotNull(upgrade);

    makeCluster();

    UpgradeContext context = new UpgradeContext(m_masterHostResolver, HDP_21,
        HDP_21, UPGRADE_VERSION, Direction.UPGRADE);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    assertEquals(6, groups.size());

    // grab the manual task out of ZK which has placeholder text
    UpgradeGroupHolder zookeeperGroup = groups.get(1);
    assertEquals("ZOOKEEPER", zookeeperGroup.name);
    ManualTask manualTask = (ManualTask) zookeeperGroup.items.get(0).getTasks().get(
        0).getTasks().get(0);

    assertEquals(
        "This is a manual task with a placeholder of placeholder-rendered-properly",
        manualTask.message);
  }

  @Test
  public void testConditionalDeleteTask() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP",
                                                                       "2.1.1");

    assertTrue(upgrades.containsKey("upgrade_test"));
    UpgradePack upgrade = upgrades.get("upgrade_test");
    assertNotNull(upgrade);

    Cluster cluster = makeCluster();

    UpgradeContext context = new UpgradeContext(m_masterHostResolver, HDP_21,
                                                HDP_21, UPGRADE_VERSION, Direction.UPGRADE);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade,
                                                                     context);

    assertEquals(6, groups.size());

    // grab the configure task out of Hive
    UpgradeGroupHolder hiveGroup = groups.get(4);
    assertEquals("HIVE", hiveGroup.name);
    ConfigureTask configureTask = (ConfigureTask) hiveGroup.items.get(1).getTasks().get(
        1).getTasks().get(0);

    // now change the thrift port to http to have the 2nd condition invoked
    Map<String, String> hiveConfigs = new HashMap<String, String>();
    hiveConfigs.put("hive.server2.transport.mode", "http");
    hiveConfigs.put("hive.server2.thrift.port", "10001");
    ConfigurationRequest configurationRequest = new ConfigurationRequest();
    configurationRequest.setClusterName(cluster.getClusterName());
    configurationRequest.setType("hive-site");
    configurationRequest.setVersionTag("version2");
    configurationRequest.setProperties(hiveConfigs);

    final ClusterRequest clusterRequest = new ClusterRequest(
        cluster.getClusterId(), cluster.getClusterName(),
        cluster.getDesiredStackVersion().getStackVersion(), null);

    clusterRequest.setDesiredConfig(Collections.singletonList(configurationRequest));
    m_managementController.updateClusters(new HashSet<ClusterRequest>() {
      {
        add(clusterRequest);
      }
    }, null);

    Map<String, String> configProperties = configureTask.getConfigurationChanges(cluster);
    assertFalse(configProperties.isEmpty());
    assertEquals(configProperties.get(ConfigureTask.PARAMETER_CONFIG_TYPE), "hive-site");

    String configurationJson = configProperties.get(ConfigureTask.PARAMETER_TRANSFERS);
    assertNotNull(configurationJson);

    List<ConfigureTask.Transfer> transfers = m_gson.fromJson(configurationJson,
                                                                              new TypeToken<List<ConfigureTask.Transfer>>() {
                                                                              }.getType());

    assertEquals(8, transfers.size());
    assertEquals("copy-key", transfers.get(0).fromKey);
    assertEquals("copy-key-to", transfers.get(0).toKey);

    assertEquals("move-key", transfers.get(1).fromKey);
    assertEquals("move-key-to", transfers.get(1).toKey);

    assertEquals("delete-key", transfers.get(2).deleteKey);

    assertEquals("delete-http", transfers.get(3).deleteKey);
    assertEquals("delete-null-if-value", transfers.get(4).deleteKey);
    assertEquals("delete-blank-if-key", transfers.get(5).deleteKey);
    assertEquals("delete-blank-if-type", transfers.get(6).deleteKey);
    assertEquals("delete-thrift", transfers.get(7).deleteKey);
  }


  @Test
  public void testConfigureTask() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP",
        "2.1.1");

    assertTrue(upgrades.containsKey("upgrade_test"));
    UpgradePack upgrade = upgrades.get("upgrade_test");
    assertNotNull(upgrade);

    Cluster cluster = makeCluster();

    UpgradeContext context = new UpgradeContext(m_masterHostResolver, HDP_21,
        HDP_21, UPGRADE_VERSION, Direction.UPGRADE);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade,
        context);

    assertEquals(6, groups.size());

    // grab the configure task out of Hive
    UpgradeGroupHolder hiveGroup = groups.get(4);
    assertEquals("HIVE", hiveGroup.name);
    ConfigureTask configureTask = (ConfigureTask) hiveGroup.items.get(1).getTasks().get(
        0).getTasks().get(0);

    Map<String, String> configProperties = configureTask.getConfigurationChanges(cluster);
    assertFalse(configProperties.isEmpty());
    assertEquals(configProperties.get(ConfigureTask.PARAMETER_CONFIG_TYPE), "hive-site");

    String configurationJson = configProperties.get(ConfigureTask.PARAMETER_KEY_VALUE_PAIRS);
    assertNotNull(configurationJson);

    List<ConfigureTask.ConfigurationKeyValue> keyValuePairs = m_gson.fromJson(configurationJson,
        new TypeToken<List<ConfigureTask.ConfigurationKeyValue>>() {
        }.getType());

    assertEquals("hive.server2.thrift.port", keyValuePairs.get(0).key);
    assertEquals("10010", keyValuePairs.get(0).value);

    // now change the thrift port to http to have the 2nd condition invoked
    Map<String, String> hiveConfigs = new HashMap<String, String>();
    hiveConfigs.put("hive.server2.transport.mode", "http");
    hiveConfigs.put("hive.server2.thrift.port", "10001");
    ConfigurationRequest configurationRequest = new ConfigurationRequest();
    configurationRequest.setClusterName(cluster.getClusterName());
    configurationRequest.setType("hive-site");
    configurationRequest.setVersionTag("version2");
    configurationRequest.setProperties(hiveConfigs);

    final ClusterRequest clusterRequest = new ClusterRequest(
        cluster.getClusterId(), cluster.getClusterName(),
        cluster.getDesiredStackVersion().getStackVersion(), null);

    clusterRequest.setDesiredConfig(Collections.singletonList(configurationRequest));
    m_managementController.updateClusters(new HashSet<ClusterRequest>() {
      {
        add(clusterRequest);
      }
    }, null);

    // the configure task should now return different properties
    configProperties = configureTask.getConfigurationChanges(cluster);
    assertFalse(configProperties.isEmpty());
    assertEquals( configProperties.get(ConfigureTask.PARAMETER_CONFIG_TYPE), "hive-site");

    configurationJson = configProperties.get(ConfigureTask.PARAMETER_KEY_VALUE_PAIRS);
    assertNotNull(configurationJson);

    keyValuePairs = m_gson.fromJson(configurationJson,
        new TypeToken<List<ConfigureTask.ConfigurationKeyValue>>() {
        }.getType());

    assertEquals("hive.server2.http.port", keyValuePairs.get(0).key);
    assertEquals("10011", keyValuePairs.get(0).value);
  }

  @Test
  public void testConfigureTaskWithMultipleConfigurations() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");

    assertTrue(upgrades.containsKey("upgrade_test"));
    UpgradePack upgrade = upgrades.get("upgrade_test");
    assertNotNull(upgrade);

    Cluster cluster = makeCluster();

    UpgradeContext context = new UpgradeContext(m_masterHostResolver, HDP_21, HDP_21,
        UPGRADE_VERSION, Direction.UPGRADE);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    assertEquals(6, groups.size());

    // grab the configure task out of Hive
    UpgradeGroupHolder hiveGroup = groups.get(4);
    assertEquals("HIVE", hiveGroup.name);
    ConfigureTask configureTask = (ConfigureTask) hiveGroup.items.get(1).getTasks().get(1).getTasks().get(0);

    Map<String, String> configProperties = configureTask.getConfigurationChanges(cluster);
    assertFalse(configProperties.isEmpty());
    assertEquals(configProperties.get(ConfigureTask.PARAMETER_CONFIG_TYPE), "hive-site");

    String configurationJson = configProperties.get(ConfigureTask.PARAMETER_KEY_VALUE_PAIRS);
    String transferJson = configProperties.get(ConfigureTask.PARAMETER_TRANSFERS);
    assertNotNull(configurationJson);
    assertNotNull(transferJson);

    List<ConfigureTask.ConfigurationKeyValue> keyValuePairs = m_gson.fromJson(configurationJson,
        new TypeToken<List<ConfigureTask.ConfigurationKeyValue>>() {
        }.getType());

    List<ConfigureTask.Transfer> transfers = m_gson.fromJson(transferJson,
        new TypeToken<List<ConfigureTask.Transfer>>() {
        }.getType());

    assertEquals("fooKey", keyValuePairs.get(0).key);
    assertEquals("fooValue", keyValuePairs.get(0).value);
    assertEquals("fooKey2", keyValuePairs.get(1).key);
    assertEquals("fooValue2", keyValuePairs.get(1).value);
    assertEquals("fooKey3", keyValuePairs.get(2).key);
    assertEquals("fooValue3", keyValuePairs.get(2).value);

    assertEquals("copy-key", transfers.get(0).fromKey);
    assertEquals("copy-key-to", transfers.get(0).toKey);

    assertEquals("move-key", transfers.get(1).fromKey);
    assertEquals("move-key-to", transfers.get(1).toKey);
  }


  @Test
  public void testServiceCheckUpgradeStages() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.2.0");
    assertTrue(upgrades.containsKey("upgrade_test_checks"));
    UpgradePack upgrade = upgrades.get("upgrade_test_checks");
    assertNotNull(upgrade);

    Cluster c = makeCluster();
    // HBASE and PIG have service checks, but not TEZ.
    Set<String> additionalServices = new HashSet<String>() {{ add("HBASE"); add("PIG"); add("TEZ"); add("AMBARI_METRICS"); }};
    for(String service : additionalServices) {
      c.addService(service);
    }

    int numServiceChecksExpected = 0;
    Collection<Service> services = c.getServices().values();
    for(Service service : services) {
      ServiceInfo si = ambariMetaInfo.getService(c.getCurrentStackVersion().getStackName(),
          c.getCurrentStackVersion().getStackVersion(), service.getName());
      if (null == si.getCommandScript()) {
        continue;
      }
      if (service.getName().equalsIgnoreCase("TEZ")) {
        assertTrue("Expect Tez to not have any service checks", false);
      }

      // Expect AMS to not run any service checks because it is excluded
      if (service.getName().equalsIgnoreCase("AMBARI_METRICS")) {
        continue;
      }
      numServiceChecksExpected++;
    }

    UpgradeContext context = new UpgradeContext(m_masterHostResolver, HDP_21,
        HDP_22, UPGRADE_VERSION, Direction.UPGRADE);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    assertEquals(7, groups.size());

    UpgradeGroupHolder holder = groups.get(3);
    assertEquals(holder.name, "SERVICE_CHECK_1");
    assertEquals(6, holder.items.size());
    int numServiceChecksActual = 0;
    for (StageWrapper sw : holder.items) {
      for(Service service : services) {
        Pattern p = Pattern.compile(".*" + service.getName(), Pattern.CASE_INSENSITIVE);
        Matcher matcher = p.matcher(sw.getText());
        if (matcher.matches()) {
          numServiceChecksActual++;
          continue;
        }
      }
    }

    assertEquals(numServiceChecksActual, numServiceChecksExpected);

    // grab the manual task out of ZK which has placeholder text
    UpgradeGroupHolder zookeeperGroup = groups.get(1);
    assertEquals("ZOOKEEPER", zookeeperGroup.name);
    ManualTask manualTask = (ManualTask) zookeeperGroup.items.get(0).getTasks().get(
        0).getTasks().get(0);

    assertEquals(
        "This is a manual task with a placeholder of placeholder-rendered-properly",
        manualTask.message);
  }

  @Test
  public void testServiceCheckDowngradeStages() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.containsKey("upgrade_test_checks"));
    UpgradePack upgrade = upgrades.get("upgrade_test_checks");
    assertNotNull(upgrade);

    makeCluster();

    UpgradeContext context = new UpgradeContext(m_masterHostResolver, HDP_21,
        HDP_21, DOWNGRADE_VERSION, Direction.DOWNGRADE);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    assertEquals(5, groups.size());

    // grab the manual task out of ZK which has placeholder text

    UpgradeGroupHolder zookeeperGroup = groups.get(3);
    assertEquals("ZOOKEEPER", zookeeperGroup.name);
    ManualTask manualTask = (ManualTask) zookeeperGroup.items.get(0).getTasks().get(
        0).getTasks().get(0);

    assertEquals(
        "This is a manual task with a placeholder of placeholder-rendered-properly",
        manualTask.message);
  }

  @Test
  public void testUpgradeOrchestrationFullTask() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");

    ServiceInfo si = ambariMetaInfo.getService("HDP", "2.1.1", "ZOOKEEPER");
    si.setDisplayName("Zk");
    ComponentInfo ci = si.getComponentByName("ZOOKEEPER_SERVER");
    ci.setDisplayName("ZooKeeper1 Server2");

    assertTrue(upgrades.containsKey("upgrade_to_new_stack"));
    UpgradePack upgrade = upgrades.get("upgrade_to_new_stack");
    assertNotNull(upgrade);

    makeCluster();

    UpgradeContext context = new UpgradeContext(m_masterHostResolver, HDP_21,
        HDP_21, UPGRADE_VERSION, Direction.UPGRADE);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    assertEquals(6, groups.size());

    assertEquals("PRE_CLUSTER", groups.get(0).name);
    assertEquals("ZOOKEEPER", groups.get(1).name);
    assertEquals("CORE_MASTER", groups.get(2).name);
    assertEquals("CORE_SLAVES", groups.get(3).name);
    assertEquals("HIVE", groups.get(4).name);

    UpgradeGroupHolder holder = groups.get(2);
    boolean found = false;
    for (StageWrapper sw : holder.items) {
      if (sw.getTasksJson().contains("Upgrading your database")) {
        found = true;
      }
    }
    assertTrue("Expected to find replaced text for Upgrading", found);

    UpgradeGroupHolder group = groups.get(1);
    // check that the display name is being used
    assertTrue(group.items.get(1).getText().contains("ZooKeeper1 Server2"));
    assertEquals(group.items.get(5).getText(), "Service Check Zk");

    group = groups.get(3);
    assertEquals(8, group.items.size());
    StageWrapper sw = group.items.get(3);
    assertEquals("Validate Partial Upgrade", sw.getText());
    assertEquals(1, sw.getTasks().size());
    assertEquals(1, sw.getTasks().get(0).getTasks().size());
    Task t = sw.getTasks().get(0).getTasks().get(0);
    assertEquals(ManualTask.class, t.getClass());
    ManualTask mt = (ManualTask) t;
    assertTrue(mt.message.contains("DataNode and NodeManager"));
    assertNotNull(mt.structuredOut);
    assertTrue(mt.structuredOut.contains("DATANODE"));
    assertTrue(mt.structuredOut.contains("NODEMANAGER"));

    UpgradeGroupHolder postGroup = groups.get(5);
    assertEquals(postGroup.name, "POST_CLUSTER");
    assertEquals(postGroup.title, "Finalize Upgrade");
    assertEquals(4, postGroup.items.size());
    assertEquals("Confirm Finalize", postGroup.items.get(0).getText());
    assertEquals("Execute HDFS Finalize", postGroup.items.get(1).getText());
    assertEquals("Save Cluster State", postGroup.items.get(2).getText());
    assertEquals(StageWrapper.Type.SERVER_SIDE_ACTION, postGroup.items.get(2).getType());
    assertEquals("Run On All 2.2.1.0-1234", postGroup.items.get(3).getText());

    assertEquals(1, postGroup.items.get(3).getTasks().size());
    Set<String> hosts = postGroup.items.get(3).getTasks().get(0).getHosts();
    assertNotNull(hosts);
    assertEquals(4, hosts.size());

    assertEquals(4, groups.get(0).items.size());
    assertEquals(6, groups.get(1).items.size());
    assertEquals(8, groups.get(2).items.size());
    assertEquals(8, groups.get(3).items.size());
  }


  private Cluster makeCluster() throws AmbariException {
    return makeCluster(true);
  }


  /**
   * Create an HA cluster
   * @throws AmbariException
   */
  private Cluster makeCluster(boolean clean) throws AmbariException {
    Clusters clusters = injector.getInstance(Clusters.class);
    ServiceFactory serviceFactory = injector.getInstance(ServiceFactory.class);

    String clusterName = "c1";

    StackId stackId = new StackId("HDP-2.1.1");
    clusters.addCluster(clusterName, stackId);
    Cluster c = clusters.getCluster(clusterName);

    helper.getOrCreateRepositoryVersion(stackId,
        c.getDesiredStackVersion().getStackVersion());

    c.createClusterVersion(stackId,
        c.getDesiredStackVersion().getStackVersion(), "admin",
        RepositoryVersionState.UPGRADING);

    for (int i = 0; i < 4; i++) {
      String hostName = "h" + (i+1);
      clusters.addHost(hostName);
      Host host = clusters.getHost(hostName);

      Map<String, String> hostAttributes = new HashMap<String, String>();
      hostAttributes.put("os_family", "redhat");
      hostAttributes.put("os_release_version", "6");

      host.setHostAttributes(hostAttributes);

      host.persist();
      clusters.mapHostToCluster(hostName, clusterName);
    }

    // !!! add services
    c.addService(serviceFactory.createNew(c, "HDFS"));
    c.addService(serviceFactory.createNew(c, "YARN"));
    c.addService(serviceFactory.createNew(c, "ZOOKEEPER"));
    c.addService(serviceFactory.createNew(c, "HIVE"));

    Service s = c.getService("HDFS");
    ServiceComponent sc = s.addServiceComponent("NAMENODE");
    sc.addServiceComponentHost("h1");
    sc.addServiceComponentHost("h2");
    sc = s.addServiceComponent("DATANODE");
    sc.addServiceComponentHost("h2");
    sc.addServiceComponentHost("h3");
    ServiceComponentHost sch = sc.addServiceComponentHost("h4");

    s = c.getService("ZOOKEEPER");
    sc = s.addServiceComponent("ZOOKEEPER_SERVER");
    sc.addServiceComponentHost("h1");
    sc.addServiceComponentHost("h2");
    sc.addServiceComponentHost("h3");

    s = c.getService("YARN");
    sc = s.addServiceComponent("RESOURCEMANAGER");
    sc.addServiceComponentHost("h2");

    sc = s.addServiceComponent("NODEMANAGER");
    sc.addServiceComponentHost("h1");
    sc.addServiceComponentHost("h3");

    s = c.getService("HIVE");
    sc = s.addServiceComponent("HIVE_SERVER");
    sc.addServiceComponentHost("h2");

    // set some desired configs
    Map<String, String> hiveConfigs = new HashMap<String, String>();
    hiveConfigs.put("hive.server2.transport.mode", "binary");
    hiveConfigs.put("hive.server2.thrift.port", "10001");

    ConfigurationRequest configurationRequest = new ConfigurationRequest();
    configurationRequest.setClusterName(clusterName);
    configurationRequest.setType("hive-site");
    configurationRequest.setVersionTag("version1");
    configurationRequest.setProperties(hiveConfigs);

    final ClusterRequest clusterRequest = new ClusterRequest(c.getClusterId(),
        clusterName, c.getDesiredStackVersion().getStackVersion(), null);

    clusterRequest.setDesiredConfig(Collections.singletonList(configurationRequest));
    m_managementController.updateClusters(new HashSet<ClusterRequest>() {
      {
        add(clusterRequest);
      }
    }, null);

    HostsType type = new HostsType();
    type.hosts.addAll(Arrays.asList("h1", "h2", "h3"));
    expect(m_masterHostResolver.getMasterAndHosts("ZOOKEEPER", "ZOOKEEPER_SERVER")).andReturn(type).anyTimes();

    type = new HostsType();
    type.hosts.addAll(Arrays.asList("h1", "h2"));
    type.master = "h1";
    type.secondary = "h2";
    expect(m_masterHostResolver.getMasterAndHosts("HDFS", "NAMENODE")).andReturn(type).anyTimes();

    type = new HostsType();
    if (clean) {
      type.hosts.addAll(Arrays.asList("h2", "h3", "h4"));
    } else {
      type.unhealthy = Collections.singletonList(sch);
      type.hosts.addAll(Arrays.asList("h2", "h3"));
    }
    expect(m_masterHostResolver.getMasterAndHosts("HDFS", "DATANODE")).andReturn(type).anyTimes();

    type = new HostsType();
    type.hosts.addAll(Arrays.asList("h2"));
    expect(m_masterHostResolver.getMasterAndHosts("YARN", "RESOURCEMANAGER")).andReturn(type).anyTimes();

    type = new HostsType();
    type.hosts.addAll(Arrays.asList("h1", "h3"));
    expect(m_masterHostResolver.getMasterAndHosts("YARN", "NODEMANAGER")).andReturn(type).anyTimes();

    expect(m_masterHostResolver.getMasterAndHosts("HIVE", "HIVE_SERVER")).andReturn(
        type).anyTimes();

    expect(m_masterHostResolver.getCluster()).andReturn(c).anyTimes();

    replay(m_masterHostResolver);

    return c;
  }



  /**
   *
   */
  private class MockModule implements Module {
    /**
    *
    */
    @Override
    public void configure(Binder binder) {
      binder.bind(ConfigHelper.class).toInstance(m_configHelper);
    }
  }
}
