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
import org.apache.ambari.server.stack.JmxQuery;
import org.apache.ambari.server.stack.MasterHostResolver;
import org.apache.ambari.server.state.UpgradeHelper.UpgradeGroupHolder;
import org.apache.ambari.server.state.stack.ConfigUpgradePack;
import org.apache.ambari.server.state.stack.UpgradePack;
import org.apache.ambari.server.state.stack.upgrade.ConfigUpgradeChangeDefinition;
import org.apache.ambari.server.state.stack.upgrade.ConfigureTask;
import org.apache.ambari.server.state.stack.upgrade.Direction;
import org.apache.ambari.server.state.stack.upgrade.ExecuteTask;
import org.apache.ambari.server.state.stack.upgrade.Grouping;
import org.apache.ambari.server.state.stack.upgrade.ManualTask;
import org.apache.ambari.server.state.stack.upgrade.StageWrapper;
import org.apache.ambari.server.state.stack.upgrade.Task;
import org.apache.ambari.server.state.stack.upgrade.TaskWrapper;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
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
  private JmxQuery m_jmxQuery;

  /**
   * Because test cases need to share config mocks, put common ones in this function.
   * @throws Exception
   */
  private void setConfigMocks() throws Exception {
    // configure the mock to return data given a specific placeholder
    m_configHelper = EasyMock.createNiceMock(ConfigHelper.class);
    expect(m_configHelper.getPlaceholderValueFromDesiredConfigurations(
        EasyMock.anyObject(Cluster.class), EasyMock.eq("{{foo/bar}}"))).andReturn("placeholder-rendered-properly").anyTimes();
    expect(m_configHelper.getEffectiveDesiredTags(
        EasyMock.anyObject(Cluster.class), EasyMock.anyObject(String.class))).andReturn(new HashMap<String, Map<String, String>>()).anyTimes();
  }

  @Before
  public void before() throws Exception {
    setConfigMocks();
    // Most test cases can replay the common config mocks. If any test case needs custom ones, it can re-initialize m_configHelper;
    replay(m_configHelper);

    final InMemoryDefaultTestModule injectorModule = new InMemoryDefaultTestModule() {
      @Override
      protected void configure() {
        super.configure();
      }
    };

    m_jmxQuery = EasyMock.createNiceMock(JmxQuery.class);

    MockModule mockModule = new MockModule();
    // create an injector which will inject the mocks
    injector = Guice.createInjector(Modules.override(injectorModule).with(mockModule));
    injector.getInstance(GuiceJpaInitializer.class);

    helper = injector.getInstance(OrmTestHelper.class);
    ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    m_upgradeHelper = injector.getInstance(UpgradeHelper.class);
    m_masterHostResolver = EasyMock.createMock(MasterHostResolver.class);
    m_managementController = injector.getInstance(AmbariManagementController.class);

//    StackDAO stackDAO = injector.getInstance(StackDAO.class);
//    StackEntity stackEntity = new StackEntity();
//    stackEntity.setStackName("HDP");
//    stackEntity.setStackVersion("2.1");
//    stackDAO.create(stackEntity);
//
//    StackEntity stackEntityTo = new StackEntity();
//    stackEntityTo.setStackName("HDP");
//    stackEntityTo.setStackVersion("2.2");
//    stackDAO.create(stackEntityTo);
//
//    Clusters clusters = injector.getInstance(Clusters.class);
//    clusters.addCluster("c1", new StackId("HDP", "2.1"));
//
//    RepositoryVersionDAO repositoryVersionDAO = injector.getInstance(RepositoryVersionDAO.class);
//    repositoryVersionDAO.create(stackEntity, "2.1.1", "2.1.1", "");
//    repositoryVersionDAO.create(stackEntityTo, "2.2.0", "2.2.0", "");
//
//    replay(m_configHelper);
  }

  @After
  public void teardown() {
    injector.getInstance(PersistService.class).stop();
  }

  @Test
  public void testSuggestUpgradePack() throws Exception{
    final String clusterName = "c1";
    final String upgradeFromVersion = "2.1.1";
    final String upgradeToVersion = "2.2.0";
    final Direction upgradeDirection = Direction.UPGRADE;
    final UpgradeType upgradeType = UpgradeType.ROLLING;

    makeCluster();
    try {
      String preferredUpgradePackName = "upgrade_test";
      UpgradePack up = m_upgradeHelper.suggestUpgradePack(clusterName, upgradeFromVersion, upgradeToVersion, upgradeDirection, upgradeType, preferredUpgradePackName);
      assertEquals(upgradeType, up.getType());
    } catch (AmbariException e){
      assertTrue(false);
    }
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
        HDP_21, UPGRADE_VERSION, Direction.UPGRADE, UpgradeType.ROLLING);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    assertEquals(7, groups.size());

    assertEquals("PRE_CLUSTER", groups.get(0).name);
    assertEquals("ZOOKEEPER", groups.get(1).name);
    assertEquals("CORE_MASTER", groups.get(2).name);
    assertEquals("CORE_SLAVES", groups.get(3).name);
    assertEquals("HIVE", groups.get(4).name);
    assertEquals("OOZIE", groups.get(5).name);

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

    UpgradeGroupHolder postGroup = groups.get(6);
    assertEquals("POST_CLUSTER", postGroup.name);
    assertEquals("Finalize Upgrade", postGroup.title);
    assertEquals(3, postGroup.items.size());
    assertEquals("Confirm Finalize", postGroup.items.get(0).getText());
    assertEquals("Execute HDFS Finalize", postGroup.items.get(1).getText());
    assertEquals("Save Cluster State", postGroup.items.get(2).getText());
    assertEquals(StageWrapper.Type.SERVER_SIDE_ACTION, postGroup.items.get(2).getType());

    assertEquals(4, groups.get(0).items.size());
    assertEquals(6, groups.get(1).items.size());
    assertEquals(9, groups.get(2).items.size());
    assertEquals(8, groups.get(3).items.size());
  }

  @Test
  public void testUpgradeServerActionOrchestration() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");

    ServiceInfo si = ambariMetaInfo.getService("HDP", "2.1.1", "ZOOKEEPER");
    si.setDisplayName("Zk");
    ComponentInfo ci = si.getComponentByName("ZOOKEEPER_SERVER");
    ci.setDisplayName("ZooKeeper1 Server2");

    assertTrue(upgrades.containsKey("upgrade_server_action_test"));
    UpgradePack upgrade = upgrades.get("upgrade_server_action_test");
    assertNotNull(upgrade);

    makeCluster();

    UpgradeContext context = new UpgradeContext(m_masterHostResolver, HDP_21,
        HDP_21, UPGRADE_VERSION, Direction.UPGRADE, UpgradeType.ROLLING);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    assertEquals(1, groups.size());
    UpgradeGroupHolder group = groups.get(0);
    assertEquals("CLUSTER_SERVER_ACTIONS", group.name);
    List<StageWrapper> stageWrappers = group.items;
    assertEquals(6, stageWrappers.size());
    assertEquals("Pre Upgrade", stageWrappers.get(0).getText());
    assertEquals("Pre Upgrade Zookeeper", stageWrappers.get(1).getText());
    assertEquals("Configuring", stageWrappers.get(2).getText());
    assertEquals("Configuring HDFS", stageWrappers.get(3).getText());
    assertEquals("Calculating Properties", stageWrappers.get(4).getText());
    assertEquals("Calculating HDFS Properties", stageWrappers.get(5).getText());

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
        HDP_21, UPGRADE_VERSION, Direction.UPGRADE, UpgradeType.ROLLING);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    assertEquals(7, groups.size());

    UpgradeGroupHolder mastersGroup = groups.get(2);
    assertEquals("CORE_MASTER", mastersGroup.name);

    List<String> orderedNameNodes = new LinkedList<String>();
    for (StageWrapper sw : mastersGroup.items) {
      if (sw.getType().equals(StageWrapper.Type.RESTART) && sw.getText().toLowerCase().contains("NameNode".toLowerCase())) {
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
        HDP_21, UPGRADE_VERSION, Direction.UPGRADE, UpgradeType.ROLLING);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    assertEquals(7, groups.size());

    assertEquals("PRE_CLUSTER", groups.get(0).name);
    assertEquals("ZOOKEEPER", groups.get(1).name);
    assertEquals("CORE_MASTER", groups.get(2).name);
    assertEquals("CORE_SLAVES", groups.get(3).name);
    assertEquals("HIVE", groups.get(4).name);
    assertEquals("OOZIE", groups.get(5).name);

    UpgradeGroupHolder postGroup = groups.get(6);
    assertEquals("POST_CLUSTER", postGroup.name);
    assertEquals("Finalize Upgrade", postGroup.title);
    assertEquals(3, postGroup.items.size());
    assertEquals("Confirm Finalize", postGroup.items.get(0).getText());
    assertEquals("Execute HDFS Finalize", postGroup.items.get(1).getText());
    assertEquals("Save Cluster State", postGroup.items.get(2).getText());
    assertEquals(StageWrapper.Type.SERVER_SIDE_ACTION, postGroup.items.get(2).getType());

    assertEquals(6, groups.get(1).items.size());
    assertEquals(9, groups.get(2).items.size());
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
        HDP_21, DOWNGRADE_VERSION, Direction.DOWNGRADE, UpgradeType.ROLLING);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    assertEquals(7, groups.size());

    assertEquals("PRE_CLUSTER", groups.get(0).name);
    assertEquals("OOZIE", groups.get(1).name);
    assertEquals("HIVE", groups.get(2).name);
    assertEquals("CORE_SLAVES", groups.get(3).name);
    assertEquals("CORE_MASTER", groups.get(4).name);
    assertEquals("ZOOKEEPER", groups.get(5).name);


    UpgradeGroupHolder postGroup = groups.get(6);
    assertEquals("POST_CLUSTER", postGroup.name);
    assertEquals("Finalize Downgrade", postGroup.title);
    assertEquals(3, postGroup.items.size());
    assertEquals("Confirm Finalize", postGroup.items.get(0).getText());
    assertEquals("Execute HDFS Finalize", postGroup.items.get(1).getText());
    assertEquals("Save Cluster State", postGroup.items.get(2).getText());
    assertEquals(StageWrapper.Type.SERVER_SIDE_ACTION, postGroup.items.get(2).getType());

    assertEquals(4, groups.get(0).items.size());
    assertEquals(8, groups.get(1).items.size());
    assertEquals(3, groups.get(2).items.size());
    assertEquals(8, groups.get(3).items.size());
    assertEquals(8, groups.get(4).items.size());
  }

  @Test
  public void testBuckets() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.containsKey("upgrade_bucket_test"));
    UpgradePack upgrade = upgrades.get("upgrade_bucket_test");
    assertNotNull(upgrade);

    makeCluster();

    UpgradeContext context = new UpgradeContext(m_masterHostResolver, HDP_21,
        HDP_21, UPGRADE_VERSION, Direction.UPGRADE, UpgradeType.ROLLING);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    assertEquals(1, groups.size());
    UpgradeGroupHolder group = groups.iterator().next();

    // Pre:
    //   Manual task = 1
    //   2x - Execute task on all 3 = 6

    // Post:
    //   Execute task on all 3 = 3
    //   2x - Manual task = 2
    //   3x - Execute task on all 3 = 9

    // Service Check = 1
    assertEquals(22, group.items.size());
  }

  @Test
  public void testManualTaskPostProcessing() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.containsKey("upgrade_test"));
    UpgradePack upgrade = upgrades.get("upgrade_test");
    assertNotNull(upgrade);

    makeCluster();

    UpgradeContext context = new UpgradeContext(m_masterHostResolver, HDP_21,
        HDP_21, UPGRADE_VERSION, Direction.UPGRADE, UpgradeType.ROLLING);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    assertEquals(7, groups.size());

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
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.containsKey("upgrade_test"));
    UpgradePack upgrade = upgrades.get("upgrade_test");
    ConfigUpgradePack cup = ambariMetaInfo.getConfigUpgradePack("HDP", "2.1.1");
    assertNotNull(upgrade);

    Cluster cluster = makeCluster();

    UpgradeContext context = new UpgradeContext(m_masterHostResolver, HDP_21,
                                                HDP_21, UPGRADE_VERSION, Direction.UPGRADE, UpgradeType.ROLLING);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    assertEquals(7, groups.size());

    // grab the configure task out of Hive
    UpgradeGroupHolder hiveGroup = groups.get(4);
    assertEquals("HIVE", hiveGroup.name);
    ConfigureTask configureTask = (ConfigureTask) hiveGroup.items.get(2).getTasks().get(0).getTasks().get(0);

    // now change the thrift port to http to have the 2nd condition invoked
    Map<String, String> hiveConfigs = new HashMap<String, String>();
    hiveConfigs.put("hive.server2.transport.mode", "http");
    hiveConfigs.put("hive.server2.thrift.port", "10001");
    hiveConfigs.put("condition", "1");

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

    Map<String, String> configProperties = configureTask.getConfigurationChanges(cluster, cup);
    assertFalse(configProperties.isEmpty());
    assertEquals(configProperties.get(ConfigureTask.PARAMETER_CONFIG_TYPE), "hive-site");

    String configurationJson = configProperties.get(ConfigureTask.PARAMETER_TRANSFERS);
    assertNotNull(configurationJson);

    List<ConfigUpgradeChangeDefinition.Transfer> transfers = m_gson.fromJson(configurationJson,
            new TypeToken<List<ConfigUpgradeChangeDefinition.Transfer>>() { }.getType());

    assertEquals(10, transfers.size());
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

    assertEquals("delete-if-key-present", transfers.get(8).deleteKey);
    assertEquals("delete-if-key-absent", transfers.get(9).deleteKey);
  }

  @Test
  public void testConfigureTask() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.containsKey("upgrade_test"));
    UpgradePack upgrade = upgrades.get("upgrade_test");
    ConfigUpgradePack cup = ambariMetaInfo.getConfigUpgradePack("HDP", "2.1.1");
    assertNotNull(upgrade);

    Cluster cluster = makeCluster();

    UpgradeContext context = new UpgradeContext(m_masterHostResolver, HDP_21,
        HDP_21, UPGRADE_VERSION, Direction.UPGRADE, UpgradeType.ROLLING);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade,
        context);

    assertEquals(7, groups.size());

    // grab the configure task out of Hive
    UpgradeGroupHolder hiveGroup = groups.get(4);
    assertEquals("HIVE", hiveGroup.name);
    ConfigureTask configureTask = (ConfigureTask) hiveGroup.items.get(1).getTasks().get(
        0).getTasks().get(0);

    Map<String, String> configProperties = configureTask.getConfigurationChanges(cluster, cup);
    assertFalse(configProperties.isEmpty());
    assertEquals(configProperties.get(ConfigureTask.PARAMETER_CONFIG_TYPE), "hive-site");

    String configurationJson = configProperties.get(ConfigureTask.PARAMETER_KEY_VALUE_PAIRS);
    assertNotNull(configurationJson);

    List<ConfigUpgradeChangeDefinition.ConfigurationKeyValue> keyValuePairs = m_gson.fromJson(configurationJson,
        new TypeToken<List<ConfigUpgradeChangeDefinition.ConfigurationKeyValue>>() {
        }.getType());

    assertEquals("hive.server2.thrift.port", keyValuePairs.get(0).key);
    assertEquals("10010", keyValuePairs.get(0).value);

    // now change the thrift port to http to have the 2nd condition invoked
    Map<String, String> hiveConfigs = new HashMap<String, String>();
    hiveConfigs.put("hive.server2.transport.mode", "http");
    hiveConfigs.put("hive.server2.thrift.port", "10001");
    hiveConfigs.put("condition", "1");

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
    configProperties = configureTask.getConfigurationChanges(cluster, cup);
    assertFalse(configProperties.isEmpty());
    assertEquals( configProperties.get(ConfigureTask.PARAMETER_CONFIG_TYPE), "hive-site");

    configurationJson = configProperties.get(ConfigureTask.PARAMETER_KEY_VALUE_PAIRS);
    assertNotNull(configurationJson);

    keyValuePairs = m_gson.fromJson(configurationJson,
        new TypeToken<List<ConfigUpgradeChangeDefinition.ConfigurationKeyValue>>() {
        }.getType());

    assertEquals("hive.server2.http.port", keyValuePairs.get(0).key);
    assertEquals("10011", keyValuePairs.get(0).value);
  }

  @Test
  public void testConfigureTaskWithMultipleConfigurations() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.containsKey("upgrade_test"));
    UpgradePack upgrade = upgrades.get("upgrade_test");
    ConfigUpgradePack cup = ambariMetaInfo.getConfigUpgradePack("HDP", "2.1.1");
    assertNotNull(upgrade);
    Cluster cluster = makeCluster();

    UpgradeContext context = new UpgradeContext(m_masterHostResolver, HDP_21, HDP_21,
        UPGRADE_VERSION, Direction.UPGRADE, UpgradeType.ROLLING);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    assertEquals(7, groups.size());

    // grab the configure task out of Hive
    UpgradeGroupHolder hiveGroup = groups.get(4);
    assertEquals("HIVE", hiveGroup.name);
    ConfigureTask configureTask = (ConfigureTask) hiveGroup.items.get(2).getTasks().get(0).getTasks().get(0);

    Map<String, String> configProperties = configureTask.getConfigurationChanges(cluster, cup);
    assertFalse(configProperties.isEmpty());
    assertEquals(configProperties.get(ConfigureTask.PARAMETER_CONFIG_TYPE), "hive-site");

    String configurationJson = configProperties.get(ConfigureTask.PARAMETER_KEY_VALUE_PAIRS);
    String transferJson = configProperties.get(ConfigureTask.PARAMETER_TRANSFERS);
    assertNotNull(configurationJson);
    assertNotNull(transferJson);

    List<ConfigUpgradeChangeDefinition.ConfigurationKeyValue> keyValuePairs = m_gson.fromJson(configurationJson,
        new TypeToken<List<ConfigUpgradeChangeDefinition.ConfigurationKeyValue>>() {
        }.getType());

    List<ConfigUpgradeChangeDefinition.Transfer> transfers = m_gson.fromJson(transferJson,
        new TypeToken<List<ConfigUpgradeChangeDefinition.Transfer>>() {
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
        HDP_22, UPGRADE_VERSION, Direction.UPGRADE, UpgradeType.ROLLING);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    assertEquals(7, groups.size());

    UpgradeGroupHolder holder = groups.get(3);
    assertEquals(holder.name, "SERVICE_CHECK_1");
    assertEquals(7, holder.items.size());
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
        HDP_21, DOWNGRADE_VERSION, Direction.DOWNGRADE, UpgradeType.ROLLING);

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
        HDP_21, UPGRADE_VERSION, Direction.UPGRADE, UpgradeType.ROLLING);

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
    assertEquals(group.items.get(4).getText(), "Service Check Zk");

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
    assertEquals(5, groups.get(1).items.size());
    assertEquals(9, groups.get(2).items.size());
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
    StackId stackId2 = new StackId("HDP-2.2.0");
    clusters.addCluster(clusterName, stackId);
    Cluster c = clusters.getCluster(clusterName);

    helper.getOrCreateRepositoryVersion(stackId,
        c.getDesiredStackVersion().getStackVersion());
    helper.getOrCreateRepositoryVersion(stackId2,"2.2.0");

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
    c.addService(serviceFactory.createNew(c, "OOZIE"));

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

    s = c.getService("OOZIE");
    // Oozie Server HA
    sc = s.addServiceComponent("OOZIE_SERVER");
    sc.addServiceComponentHost("h2");
    sc.addServiceComponentHost("h3");
    sc = s.addServiceComponent("OOZIE_CLIENT");
    sc.addServiceComponentHost("h1");
    sc.addServiceComponentHost("h2");
    sc.addServiceComponentHost("h3");

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
    expect(m_masterHostResolver.getMasterAndHosts("YARN", "APP_TIMELINE_SERVER")).andReturn(type).anyTimes();

    type = new HostsType();
    type.hosts.addAll(Arrays.asList("h1", "h3"));
    expect(m_masterHostResolver.getMasterAndHosts("YARN", "NODEMANAGER")).andReturn(type).anyTimes();

    expect(m_masterHostResolver.getMasterAndHosts("HIVE", "HIVE_SERVER")).andReturn(
        type).anyTimes();

    type = new HostsType();
    type.hosts.addAll(Arrays.asList("h2", "h3"));
    expect(m_masterHostResolver.getMasterAndHosts("OOZIE", "OOZIE_SERVER")).andReturn(type).anyTimes();

    type = new HostsType();
    type.hosts.addAll(Arrays.asList("h1", "h2", "h3"));
    expect(m_masterHostResolver.getMasterAndHosts("OOZIE", "OOZIE_CLIENT")).andReturn(type).anyTimes();

    expect(m_masterHostResolver.getCluster()).andReturn(c).anyTimes();

    replay(m_masterHostResolver);

    return c;
  }

  /**
   * Test that multiple execute tasks with an annotation of synchronized="true" each run in their own stage.
   */
  @Test
  public void testUpgradeWithMultipleTasksInOwnStage() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");

    assertTrue(upgrades.containsKey("upgrade_test"));
    UpgradePack upgrade = upgrades.get("upgrade_test");
    assertNotNull(upgrade);
    assertTrue(upgrade.getType() == UpgradeType.ROLLING);

    List<Grouping> upgradePackGroups = upgrade.getGroups(Direction.UPGRADE);

    boolean foundService = false;
    for (Grouping group : upgradePackGroups) {
      if (group.title.equals("Oozie")) {
        foundService = true;
      }
    }
    assertTrue(foundService);

    makeCluster();

    UpgradeContext context = new UpgradeContext(m_masterHostResolver, HDP_21,
        HDP_21, UPGRADE_VERSION, Direction.UPGRADE, UpgradeType.ROLLING);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    // The upgrade pack has 2 tasks for Oozie in the pre-upgrade group.
    // The first task runs on "all", i.e., both Oozie Servers, whereas the
    // second task runs on "any", i.e., exactly one.
    int numPrepareStages = 0;
    for (UpgradeGroupHolder group : groups) {
      if (group.name.equals("OOZIE")) {
        assertTrue(group.items.size() > 0);
        for (StageWrapper sw : group.items) {

          if (sw.getText().equalsIgnoreCase("Preparing Oozie Server on h2 (Batch 1 of 2)") ||
              sw.getText().equalsIgnoreCase("Preparing Oozie Server on h3 (Batch 2 of 2)")) {
            numPrepareStages++;
            List<TaskWrapper> taskWrappers = sw.getTasks();
            assertEquals(1, taskWrappers.size());
            List<Task> tasks = taskWrappers.get(0).getTasks();
            assertEquals(1, taskWrappers.get(0).getHosts().size());
            assertEquals(1, tasks.size());

            ExecuteTask task = (ExecuteTask) tasks.get(0);
            assertTrue("scripts/oozie_server.py".equalsIgnoreCase(task.script));
            assertTrue("stop".equalsIgnoreCase(task.function));
          }

          if (sw.getText().equalsIgnoreCase("Preparing Oozie Server on h2")) {
            numPrepareStages++;
            List<TaskWrapper> taskWrappers = sw.getTasks();
            assertEquals(1, taskWrappers.size());
            List<Task> tasks = taskWrappers.get(0).getTasks();
            assertEquals(1, taskWrappers.get(0).getHosts().size());
            assertEquals(1, tasks.size());

            ExecuteTask task = (ExecuteTask) tasks.get(0);
            assertTrue("scripts/oozie_server_upgrade.py".equalsIgnoreCase(task.script));
            assertTrue("upgrade_oozie_database_and_sharelib".equalsIgnoreCase(task.function));
          }
        }
      }
    }
    assertEquals(3, numPrepareStages);
  }

  @Test
  public void testDowngradeAfterPartialUpgrade() throws Exception {

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

    for (int i = 0; i < 2; i++) {
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

    Service s = c.getService("HDFS");
    ServiceComponent sc = s.addServiceComponent("NAMENODE");
    sc.addServiceComponentHost("h1");
    sc.addServiceComponentHost("h2");

    List<ServiceComponentHost> schs = c.getServiceComponentHosts("HDFS", "NAMENODE");
    assertEquals(2, schs.size());

    HostsType type = new HostsType();
    type.master = "h1";
    type.secondary = "h2";

    expect(m_masterHostResolver.getMasterAndHosts("ZOOKEEPER", "ZOOKEEPER_SERVER")).andReturn(null).anyTimes();
    expect(m_masterHostResolver.getMasterAndHosts("HDFS", "NAMENODE")).andReturn(type).anyTimes();
    expect(m_masterHostResolver.getCluster()).andReturn(c).anyTimes();
    replay(m_masterHostResolver);

    UpgradeContext context = new UpgradeContext(m_masterHostResolver, HDP_21, HDP_21, DOWNGRADE_VERSION,
        Direction.DOWNGRADE, UpgradeType.ROLLING);

    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.containsKey("upgrade_direction"));
    UpgradePack upgrade = upgrades.get("upgrade_direction");
    assertNotNull(upgrade);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);
    assertEquals(2, groups.size());

    UpgradeGroupHolder group = groups.get(0);
    assertEquals(1, group.items.size());
    assertEquals("PRE_POST_CLUSTER", group.name);

    group = groups.get(1);
    assertEquals("POST_CLUSTER", group.name);
    assertEquals(3, group.items.size());


    StageWrapper stage = group.items.get(1);
    assertEquals("NameNode Finalize", stage.getText());
    assertEquals(1, stage.getTasks().size());
    TaskWrapper task = stage.getTasks().get(0);
    assertEquals(1, task.getHosts().size());
  }

  @Test
  public void testResolverWithFailedUpgrade() throws Exception {
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

    for (int i = 0; i < 2; i++) {
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
    c.addService(serviceFactory.createNew(c, "ZOOKEEPER"));

    Service s = c.getService("ZOOKEEPER");
    ServiceComponent sc = s.addServiceComponent("ZOOKEEPER_SERVER");

    ServiceComponentHost sch1 = sc.addServiceComponentHost("h1");
    sch1.setVersion("2.1.1.0-1234");

    ServiceComponentHost sch2 = sc.addServiceComponentHost("h2");
    sch2.setVersion("2.1.1.0-1234");

    List<ServiceComponentHost> schs = c.getServiceComponentHosts("ZOOKEEPER", "ZOOKEEPER_SERVER");
    assertEquals(2, schs.size());

    JmxQuery jmx = new JmxQuery();
    MasterHostResolver mhr = new MasterHostResolver(null, jmx, c, "2.1.1.0-1234");

    HostsType ht = mhr.getMasterAndHosts("ZOOKEEPER", "ZOOKEEPER_SERVER");
    assertEquals(0, ht.hosts.size());

    // !!! if one of them is failed, it should be scheduled
    sch2.setUpgradeState(UpgradeState.FAILED);

    ht = mhr.getMasterAndHosts("ZOOKEEPER", "ZOOKEEPER_SERVER");

    assertEquals(1, ht.hosts.size());
    assertEquals("h2", ht.hosts.iterator().next());
  }

  /**
   * Test that MasterHostResolver is case-insensitive even if configs have hosts in upper case for NameNode.
   * @throws Exception
   */
  @Test
  public void testResolverCaseInsensitive() throws Exception {
    Clusters clusters = injector.getInstance(Clusters.class);
    ServiceFactory serviceFactory = injector.getInstance(ServiceFactory.class);

    String clusterName = "c1";
    String version = "2.1.1.0-1234";

    StackId stackId = new StackId("HDP-2.1.1");
    clusters.addCluster(clusterName, stackId);
    Cluster c = clusters.getCluster(clusterName);

    helper.getOrCreateRepositoryVersion(stackId,
        c.getDesiredStackVersion().getStackVersion());

    c.createClusterVersion(stackId,
        c.getDesiredStackVersion().getStackVersion(), "admin",
        RepositoryVersionState.UPGRADING);

    for (int i = 0; i < 2; i++) {
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

    // Add services
    c.addService(serviceFactory.createNew(c, "HDFS"));

    Service s = c.getService("HDFS");
    ServiceComponent sc = s.addServiceComponent("NAMENODE");
    sc.addServiceComponentHost("h1");
    sc.addServiceComponentHost("h2");

    List<ServiceComponentHost> schs = c.getServiceComponentHosts("HDFS", "NAMENODE");
    assertEquals(2, schs.size());

    setConfigMocks();
    expect(m_configHelper.getValueFromDesiredConfigurations(c, "hdfs-site", "dfs.nameservices")).andReturn("ha").anyTimes();
    expect(m_configHelper.getValueFromDesiredConfigurations(c, "hdfs-site", "dfs.ha.namenodes.ha")).andReturn("nn1,nn2").anyTimes();
    expect(m_configHelper.getValueFromDesiredConfigurations(c, "hdfs-site", "dfs.http.policy")).andReturn("HTTP_ONLY").anyTimes();

    // Notice that these names are all caps.
    expect(m_configHelper.getValueFromDesiredConfigurations(c, "hdfs-site", "dfs.namenode.http-address.ha.nn1")).andReturn("H1:50070").anyTimes();
    expect(m_configHelper.getValueFromDesiredConfigurations(c, "hdfs-site", "dfs.namenode.http-address.ha.nn2")).andReturn("H2:50070").anyTimes();
    replay(m_configHelper);

    // Mock the calls to JMX
    expect(m_jmxQuery.queryJmxBeanValue("H1", 50070, "Hadoop:service=NameNode,name=NameNodeStatus", "State", true, false)).andReturn("ACTIVE").anyTimes();
    expect(m_jmxQuery.queryJmxBeanValue("H2", 50070, "Hadoop:service=NameNode,name=NameNodeStatus", "State", true, false)).andReturn("STANDBY").anyTimes();
    replay(m_jmxQuery);

    MasterHostResolver mhr = new MasterHostResolver(m_configHelper, m_jmxQuery, c, version);

    HostsType ht = mhr.getMasterAndHosts("HDFS", "NAMENODE");
    assertEquals(2, ht.hosts.size());

    // Should be stored in lowercase.
    assertTrue(ht.hosts.contains("h1"));
    assertTrue(ht.hosts.contains("h1"));
  }


  private class MockModule implements Module {

    @Override
    public void configure(Binder binder) {
      binder.bind(ConfigHelper.class).toInstance(m_configHelper);
    }
  }
}
