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

import java.lang.reflect.Field;
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
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.stack.HostsType;
import org.apache.ambari.server.stack.MasterHostResolver;
import org.apache.ambari.server.stack.StackManagerMock;
import org.apache.ambari.server.state.UpgradeHelper.UpgradeGroupHolder;
import org.apache.ambari.server.state.stack.ConfigUpgradePack;
import org.apache.ambari.server.state.stack.UpgradePack;
import org.apache.ambari.server.state.stack.upgrade.ConfigUpgradeChangeDefinition;
import org.apache.ambari.server.state.stack.upgrade.ConfigUpgradeChangeDefinition.ConfigurationKeyValue;
import org.apache.ambari.server.state.stack.upgrade.ConfigureTask;
import org.apache.ambari.server.state.stack.upgrade.Direction;
import org.apache.ambari.server.state.stack.upgrade.ExecuteTask;
import org.apache.ambari.server.state.stack.upgrade.Grouping;
import org.apache.ambari.server.state.stack.upgrade.HostOrderGrouping;
import org.apache.ambari.server.state.stack.upgrade.HostOrderItem;
import org.apache.ambari.server.state.stack.upgrade.HostOrderItem.HostOrderActionType;
import org.apache.ambari.server.state.stack.upgrade.ManualTask;
import org.apache.ambari.server.state.stack.upgrade.SecurityCondition;
import org.apache.ambari.server.state.stack.upgrade.StageWrapper;
import org.apache.ambari.server.state.stack.upgrade.StopGrouping;
import org.apache.ambari.server.state.stack.upgrade.Task;
import org.apache.ambari.server.state.stack.upgrade.TaskWrapper;
import org.apache.ambari.server.state.stack.upgrade.UpgradeScope;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
import org.apache.ambari.server.utils.EventBusSynchronizer;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
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

  private static final StackId HDP_21 = new StackId("HDP-2.1.1");
  private static final StackId HDP_22 = new StackId("HDP-2.2.0");
  private static final String UPGRADE_VERSION = "2.2.1.0-1234";
  private static final String DOWNGRADE_VERSION = "2.2.0.0-1234";

  private Injector injector;
  private AmbariMetaInfo ambariMetaInfo;
  private StackManagerMock stackManagerMock;
  private OrmTestHelper helper;
  private MasterHostResolver m_masterHostResolver;
  private UpgradeHelper m_upgradeHelper;
  private ConfigHelper m_configHelper;
  private AmbariManagementController m_managementController;
  private Gson m_gson = new Gson();

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

    MockModule mockModule = new MockModule();
    // create an injector which will inject the mocks
    injector = Guice.createInjector(Modules.override(injectorModule).with(mockModule));
    injector.getInstance(GuiceJpaInitializer.class);
    EventBusSynchronizer.synchronizeAmbariEventPublisher(injector);
    EventBusSynchronizer.synchronizeAlertEventPublisher(injector);

    helper = injector.getInstance(OrmTestHelper.class);
    ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    stackManagerMock = (StackManagerMock) ambariMetaInfo.getStackManager();
    m_upgradeHelper = injector.getInstance(UpgradeHelper.class);
    m_masterHostResolver = EasyMock.createMock(MasterHostResolver.class);
    m_managementController = injector.getInstance(AmbariManagementController.class);

    // Set the authenticated user
    // TODO: remove this or replace the authenticated user to test authorization rules
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator("admin"));
  }

  @After
  public void teardown() {
    injector.getInstance(PersistService.class).stop();

    // Clear the authenticated user
    SecurityContextHolder.getContext().setAuthentication(null);
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

    Cluster cluster = makeCluster();

    UpgradeContext context = new UpgradeContext(cluster, UpgradeType.ROLLING, Direction.UPGRADE, null);
    context.setSourceAndTargetStacks(HDP_21, HDP_21);
    context.setVersion(UPGRADE_VERSION);
    context.setResolver(m_masterHostResolver);

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
    assertTrue(mt.messages.get(0).contains("DataNode and NodeManager"));
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

    // Do stacks cleanup
    stackManagerMock.invalidateCurrentPaths();
    ambariMetaInfo.init();
  }

  @Test
  public void testPartialUpgradeOrchestration() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("foo", "bar");
    assertTrue(upgrades.isEmpty());

    upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");

    ServiceInfo si = ambariMetaInfo.getService("HDP", "2.1.1", "ZOOKEEPER");
    si.setDisplayName("Zk");
    ComponentInfo ci = si.getComponentByName("ZOOKEEPER_SERVER");
    ci.setDisplayName("ZooKeeper1 Server2");

    assertTrue(upgrades.containsKey("upgrade_test_partial"));
    UpgradePack upgrade = upgrades.get("upgrade_test_partial");
    assertNotNull(upgrade);

    Cluster cluster = makeCluster();

    UpgradeContext context = new UpgradeContext(cluster, UpgradeType.ROLLING, Direction.UPGRADE, null);
    context.setSourceAndTargetStacks(HDP_21, HDP_21);
    context.setVersion(UPGRADE_VERSION);
    context.setResolver(m_masterHostResolver);
    context.setSupportedServices(Collections.singleton("ZOOKEEPER"));
    context.setScope(UpgradeScope.PARTIAL);

    List<Grouping> groupings = upgrade.getGroups(Direction.UPGRADE);
    assertEquals(8, groupings.size());
    assertEquals(UpgradeScope.COMPLETE, groupings.get(6).scope);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    assertEquals(3, groups.size());

    assertEquals("PRE_CLUSTER", groups.get(0).name);
    assertEquals("ZOOKEEPER", groups.get(1).name);
    assertEquals("POST_CLUSTER", groups.get(2).name);

    UpgradeGroupHolder group = groups.get(1);
    // check that the display name is being used
    assertTrue(group.items.get(1).getText().contains("ZooKeeper1 Server2"));
    assertEquals("Service Check Zk", group.items.get(6).getText());

    UpgradeGroupHolder postGroup = groups.get(2);
    assertEquals("POST_CLUSTER", postGroup.name);
    assertEquals("Finalize Upgrade", postGroup.title);
    assertEquals(2, postGroup.items.size());
    assertEquals("Confirm Finalize", postGroup.items.get(0).getText());
    assertEquals("Save Cluster State", postGroup.items.get(1).getText());
    assertEquals(StageWrapper.Type.SERVER_SIDE_ACTION, postGroup.items.get(1).getType());

    assertEquals(3, groups.get(0).items.size());
    assertEquals(7, groups.get(1).items.size());
    assertEquals(2, groups.get(2).items.size());

    // Do stacks cleanup
    stackManagerMock.invalidateCurrentPaths();
    ambariMetaInfo.init();
  }

  @Test
  public void testCompleteUpgradeOrchestration() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("foo", "bar");
    assertTrue(upgrades.isEmpty());

    upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");

    ServiceInfo si = ambariMetaInfo.getService("HDP", "2.1.1", "ZOOKEEPER");
    si.setDisplayName("Zk");
    ComponentInfo ci = si.getComponentByName("ZOOKEEPER_SERVER");
    ci.setDisplayName("ZooKeeper1 Server2");

    assertTrue(upgrades.containsKey("upgrade_test_partial"));
    UpgradePack upgrade = upgrades.get("upgrade_test_partial");
    assertNotNull(upgrade);

    Cluster cluster = makeCluster();

    UpgradeContext context = new UpgradeContext(cluster, UpgradeType.ROLLING, Direction.UPGRADE, null);
    context.setSourceAndTargetStacks(HDP_21, HDP_21);
    context.setVersion(UPGRADE_VERSION);
    context.setResolver(m_masterHostResolver);
    context.setSupportedServices(Collections.singleton("ZOOKEEPER"));
    context.setScope(UpgradeScope.COMPLETE);

    List<Grouping> groupings = upgrade.getGroups(Direction.UPGRADE);
    assertEquals(8, groupings.size());
    assertEquals(UpgradeScope.COMPLETE, groupings.get(6).scope);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    assertEquals(4, groups.size());

    assertEquals("PRE_CLUSTER", groups.get(0).name);
    assertEquals("ZOOKEEPER", groups.get(1).name);
    assertEquals("ALL_HOSTS", groups.get(2).name);
    assertEquals("POST_CLUSTER", groups.get(3).name);

    UpgradeGroupHolder group = groups.get(1);
    // check that the display name is being used
    assertTrue(group.items.get(1).getText().contains("ZooKeeper1 Server2"));
    assertEquals("Service Check Zk", group.items.get(5).getText());

    UpgradeGroupHolder postGroup = groups.get(3);
    assertEquals("POST_CLUSTER", postGroup.name);
    assertEquals("Finalize Upgrade", postGroup.title);
    assertEquals(2, postGroup.items.size());
    assertEquals("Confirm Finalize", postGroup.items.get(0).getText());
    assertEquals("Save Cluster State", postGroup.items.get(1).getText());
    assertEquals(StageWrapper.Type.SERVER_SIDE_ACTION, postGroup.items.get(1).getType());

    assertEquals(3, groups.get(0).items.size());
    assertEquals(6, groups.get(1).items.size());
    assertEquals(1, groups.get(2).items.size());

    // Do stacks cleanup
    stackManagerMock.invalidateCurrentPaths();
    ambariMetaInfo.init();
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

    Cluster cluster = makeCluster();

    UpgradeContext context = new UpgradeContext(cluster, UpgradeType.ROLLING, Direction.UPGRADE, null);
    context.setSourceAndTargetStacks(HDP_21, HDP_21);
    context.setVersion(UPGRADE_VERSION);
    context.setResolver(m_masterHostResolver);

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

    // Do stacks cleanup
    stackManagerMock.invalidateCurrentPaths();
    ambariMetaInfo.init();
  }

  /**
   * Tests that hosts in MM are not included in the upgrade.
   *
   * @throws Exception
   */
  @Test
  public void testUpgradeOrchestrationWithHostsInMM() throws Exception {
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

    // turn on MM for the first host
    Cluster cluster = makeCluster();
    Host hostInMaintenanceMode = cluster.getHosts().iterator().next();
    hostInMaintenanceMode.setMaintenanceState(cluster.getClusterId(), MaintenanceState.ON);

    // use a "real" master host resolver here so that we can actually test MM
    MasterHostResolver masterHostResolver = new MasterHostResolver(null, cluster, "");

    UpgradeContext context = new UpgradeContext(cluster, UpgradeType.ROLLING, Direction.UPGRADE, null);
    context.setSourceAndTargetStacks(HDP_21, HDP_21);
    context.setVersion(UPGRADE_VERSION);
    context.setResolver(masterHostResolver);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);
    assertEquals(7, groups.size());

    for (UpgradeGroupHolder group : groups) {
      for (StageWrapper stageWrapper : group.items) {
        Set<String> hosts = stageWrapper.getHosts();
        assertFalse(hosts.contains(hostInMaintenanceMode.getHostName()));
      }
    }

    // Do stacks cleanup
    stackManagerMock.invalidateCurrentPaths();
    ambariMetaInfo.init();
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

    Cluster cluster = makeCluster();

    UpgradeContext context = new UpgradeContext(cluster, UpgradeType.ROLLING, Direction.UPGRADE, null);
    context.setSourceAndTargetStacks(HDP_21, HDP_21);
    context.setVersion(UPGRADE_VERSION);
    context.setResolver(m_masterHostResolver);

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

    List<ServiceComponentHost> schs = cluster.getServiceComponentHosts("h4");
    assertEquals(1, schs.size());
    assertEquals(HostState.HEARTBEAT_LOST, schs.get(0).getHostState());

    UpgradeContext context = new UpgradeContext(cluster, UpgradeType.ROLLING, Direction.UPGRADE, null);
    context.setSourceAndTargetStacks(HDP_21, HDP_21);
    context.setVersion(UPGRADE_VERSION);
    context.setResolver(m_masterHostResolver);

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

    Cluster cluster = makeCluster();

    UpgradeContext context = new UpgradeContext(cluster, UpgradeType.ROLLING, Direction.DOWNGRADE, null);
    context.setSourceAndTargetStacks(HDP_21, HDP_21);
    context.setVersion(DOWNGRADE_VERSION);
    context.setResolver(m_masterHostResolver);

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
    assertEquals(5, groups.get(2).items.size());
    assertEquals(8, groups.get(3).items.size());
    assertEquals(8, groups.get(4).items.size());
  }

  @Test
  public void testBuckets() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.containsKey("upgrade_bucket_test"));
    UpgradePack upgrade = upgrades.get("upgrade_bucket_test");
    assertNotNull(upgrade);

    Cluster cluster = makeCluster();

    UpgradeContext context = new UpgradeContext(cluster, UpgradeType.ROLLING, Direction.UPGRADE, null);
    context.setSourceAndTargetStacks(HDP_21, HDP_21);
    context.setVersion(UPGRADE_VERSION);
    context.setResolver(m_masterHostResolver);

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

    Cluster cluster = makeCluster();

    UpgradeContext context = new UpgradeContext(cluster, UpgradeType.ROLLING, Direction.UPGRADE, null);
    context.setSourceAndTargetStacks(HDP_21, HDP_21);
    context.setVersion(UPGRADE_VERSION);
    context.setResolver(m_masterHostResolver);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    assertEquals(7, groups.size());

    // grab the manual task out of ZK which has placeholder text
    UpgradeGroupHolder zookeeperGroup = groups.get(1);
    assertEquals("ZOOKEEPER", zookeeperGroup.name);
    ManualTask manualTask = (ManualTask) zookeeperGroup.items.get(0).getTasks().get(
        0).getTasks().get(0);

    assertEquals(1, manualTask.messages.size());
    assertEquals("This is a manual task with a placeholder of placeholder-rendered-properly",
        manualTask.messages.get(0));
  }

  @Test
  public void testConditionalDeleteTask() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.containsKey("upgrade_test"));
    UpgradePack upgrade = upgrades.get("upgrade_test");
    ConfigUpgradePack cup = ambariMetaInfo.getConfigUpgradePack("HDP", "2.1.1");
    assertNotNull(upgrade);

    Cluster cluster = makeCluster();

    UpgradeContext context = new UpgradeContext(cluster, UpgradeType.ROLLING, Direction.UPGRADE, null);
    context.setSourceAndTargetStacks(HDP_21, HDP_21);
    context.setVersion(UPGRADE_VERSION);
    context.setResolver(m_masterHostResolver);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    assertEquals(7, groups.size());

    // grab the configure task out of Hive
    UpgradeGroupHolder hiveGroup = groups.get(4);
    assertEquals("HIVE", hiveGroup.name);
    ConfigureTask configureTask = (ConfigureTask) hiveGroup.items.get(1).getTasks().get(0).getTasks().get(0);

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
    System.out.println(">> transfers"+transfers);

    assertEquals(6, transfers.size());
    assertEquals("copy-key", transfers.get(0).fromKey);
    assertEquals("copy-key-to", transfers.get(0).toKey);

    assertEquals("move-key", transfers.get(1).fromKey);
    assertEquals("move-key-to", transfers.get(1).toKey);

    assertEquals("delete-key", transfers.get(2).deleteKey);
    assertEquals("delete-http-1", transfers.get(3).deleteKey);
    assertEquals("delete-http-2", transfers.get(4).deleteKey);
    assertEquals("delete-http-3", transfers.get(5).deleteKey);


  }

  @Test
  public void testConfigTaskConditionMet() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.containsKey("upgrade_test"));
    UpgradePack upgrade = upgrades.get("upgrade_test");
    ConfigUpgradePack cup = ambariMetaInfo.getConfigUpgradePack("HDP", "2.1.1");
    assertNotNull(upgrade);

    Cluster cluster = makeCluster();

    UpgradeContext context = new UpgradeContext(cluster, UpgradeType.ROLLING, Direction.UPGRADE, null);
    context.setSourceAndTargetStacks(HDP_21, HDP_21);
    context.setVersion(UPGRADE_VERSION);
    context.setResolver(m_masterHostResolver);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade,
        context);

    assertEquals(7, groups.size());

    // grab the configure task out of Hive
    UpgradeGroupHolder hiveGroup = groups.get(4);
    assertEquals("HIVE", hiveGroup.name);

    //Condition is met
    ConfigureTask configureTask = (ConfigureTask) hiveGroup.items.get(2).getTasks().get(
        0).getTasks().get(0);
    Map<String, String> configProperties = configureTask.getConfigurationChanges(cluster, cup);

    assertFalse(configProperties.isEmpty());
    assertEquals(configProperties.get(ConfigureTask.PARAMETER_CONFIG_TYPE), "hive-site");

    assertTrue(configProperties.containsKey(ConfigureTask.PARAMETER_KEY_VALUE_PAIRS));
    assertTrue(configProperties.containsKey(ConfigureTask.PARAMETER_REPLACEMENTS));
    assertTrue(configProperties.containsKey(ConfigureTask.PARAMETER_TRANSFERS));

    String configurationJson = configProperties.get(ConfigureTask.PARAMETER_KEY_VALUE_PAIRS);
    String transferJson = configProperties.get(ConfigureTask.PARAMETER_TRANSFERS);
    String replacementJson = configProperties.get(ConfigureTask.PARAMETER_REPLACEMENTS);
    assertNotNull(configurationJson);
    assertNotNull(transferJson);
    assertNotNull(replacementJson);

    //if conditions for sets...
    List<ConfigUpgradeChangeDefinition.ConfigurationKeyValue> keyValuePairs = m_gson.fromJson(configurationJson,
        new TypeToken<List<ConfigUpgradeChangeDefinition.ConfigurationKeyValue>>() {
        }.getType());
    assertEquals("setKeyOne", keyValuePairs.get(0).key);
    assertEquals("1", keyValuePairs.get(0).value);

    assertEquals("setKeyTwo", keyValuePairs.get(1).key);
    assertEquals("2", keyValuePairs.get(1).value);

    assertEquals("setKeyThree", keyValuePairs.get(2).key);
    assertEquals("3", keyValuePairs.get(2).value);

    assertEquals("setKeyFour", keyValuePairs.get(3).key);
    assertEquals("4", keyValuePairs.get(3).value);

    //if conditions for transfer
    List<ConfigUpgradeChangeDefinition.Transfer> transfers = m_gson.fromJson(transferJson,
        new TypeToken<List<ConfigUpgradeChangeDefinition.Transfer>>() {
        }.getType());

    System.out.println(" testConfigTaskConditionMet >> transfer"+transfers);

    assertEquals("copy-key-one", transfers.get(0).fromKey);
    assertEquals("copy-to-key-one", transfers.get(0).toKey);

    assertEquals("copy-key-two", transfers.get(1).fromKey);
    assertEquals("copy-to-key-two", transfers.get(1).toKey);

    assertEquals("copy-key-three", transfers.get(2).fromKey);
    assertEquals("copy-to-key-three", transfers.get(2).toKey);

    assertEquals("copy-key-four", transfers.get(3).fromKey);
    assertEquals("copy-to-key-four", transfers.get(3).toKey);

    assertEquals("move-key-one", transfers.get(4).fromKey);
    assertEquals("move-to-key-one", transfers.get(4).toKey);

    assertEquals("move-key-two", transfers.get(5).fromKey);
    assertEquals("move-to-key-two", transfers.get(5).toKey);

    assertEquals("move-key-three", transfers.get(6).fromKey);
    assertEquals("move-to-key-three", transfers.get(6).toKey);

    assertEquals("move-key-four", transfers.get(7).fromKey);
    assertEquals("move-to-key-four", transfers.get(7).toKey);

    assertEquals("delete-key-one", transfers.get(8).deleteKey);
    assertEquals("delete-key-two", transfers.get(9).deleteKey);
    assertEquals("delete-key-three", transfers.get(10).deleteKey);
    assertEquals("delete-key-four", transfers.get(11).deleteKey);

    //if conditions for replace
    List<ConfigUpgradeChangeDefinition.Replace> replacements = m_gson.fromJson(replacementJson,
        new TypeToken<List<ConfigUpgradeChangeDefinition.Replace>>() {
        }.getType());
    assertEquals("replace-key-one", replacements.get(0).key);
    assertEquals("abc", replacements.get(0).find);
    assertEquals("abc-replaced", replacements.get(0).replaceWith);
    assertEquals("replace-key-two", replacements.get(1).key);
    assertEquals("efg", replacements.get(1).find);
    assertEquals("efg-replaced", replacements.get(1).replaceWith);
    assertEquals("replace-key-three", replacements.get(2).key);
    assertEquals("ijk", replacements.get(2).find);
    assertEquals("ijk-replaced", replacements.get(2).replaceWith);
    assertEquals("replace-key-four", replacements.get(3).key);
    assertEquals("lmn", replacements.get(3).find);
    assertEquals("lmn-replaced", replacements.get(3).replaceWith);
  }

  @Test
  public void testConfigTaskConditionSkipped() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.containsKey("upgrade_test"));
    UpgradePack upgrade = upgrades.get("upgrade_test");
    ConfigUpgradePack cup = ambariMetaInfo.getConfigUpgradePack("HDP", "2.1.1");
    assertNotNull(upgrade);

    Cluster cluster = makeCluster();

    UpgradeContext context = new UpgradeContext(cluster, UpgradeType.ROLLING, Direction.UPGRADE, null);
    context.setSourceAndTargetStacks(HDP_21, HDP_21);
    context.setVersion(UPGRADE_VERSION);
    context.setResolver(m_masterHostResolver);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade,
        context);

    assertEquals(7, groups.size());

    UpgradeGroupHolder hiveGroup = groups.get(4);
    assertEquals("HIVE", hiveGroup.name);

    //Condition is not met, so no config operations should be present in the configureTask...
    ConfigureTask configureTask = (ConfigureTask) hiveGroup.items.get(3).getTasks().get(0).getTasks().get(0);
    Map<String, String> configProperties = configureTask.getConfigurationChanges(cluster, cup);

    assertFalse(configProperties.isEmpty());
    assertEquals(configProperties.get(ConfigureTask.PARAMETER_CONFIG_TYPE), "hive-site");

    assertTrue(configProperties.containsKey(ConfigureTask.PARAMETER_KEY_VALUE_PAIRS));
    assertTrue(configProperties.containsKey(ConfigureTask.PARAMETER_REPLACEMENTS));
    assertTrue(configProperties.containsKey(ConfigureTask.PARAMETER_TRANSFERS));

    String configurationJson = configProperties.get(ConfigureTask.PARAMETER_KEY_VALUE_PAIRS);
    String transferJson = configProperties.get(ConfigureTask.PARAMETER_TRANSFERS);

    String replacementJson = configProperties.get(ConfigureTask.PARAMETER_REPLACEMENTS);
    assertNotNull(configurationJson);
    assertNotNull(transferJson);
    assertNotNull(replacementJson);

    List<ConfigUpgradeChangeDefinition.ConfigurationKeyValue> keyValuePairs = m_gson.fromJson(configurationJson,
        new TypeToken<List<ConfigUpgradeChangeDefinition.ConfigurationKeyValue>>() {
        }.getType());
    assertTrue(keyValuePairs.isEmpty());

    List<ConfigUpgradeChangeDefinition.Replace> replacements = m_gson.fromJson(replacementJson,
        new TypeToken<List<ConfigUpgradeChangeDefinition.Replace>>() {
        }.getType());
    assertTrue(replacements.isEmpty());

    List<ConfigUpgradeChangeDefinition.Transfer> transfers = m_gson.fromJson(transferJson,
        new TypeToken<List<ConfigUpgradeChangeDefinition.Transfer>>() {
        }.getType());
    assertTrue(transfers.isEmpty());
  }

  /**
   * Tests that {@link ConfigurationKeyValue} pairs on a {@link ConfigureTask}
   * are correctly returned based on the if-conditions.
   *
   * @throws Exception
   */
  @Test
  public void testConfigureTask() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.containsKey("upgrade_test"));
    UpgradePack upgrade = upgrades.get("upgrade_test");
    ConfigUpgradePack cup = ambariMetaInfo.getConfigUpgradePack("HDP", "2.1.1");
    assertNotNull(upgrade);

    Cluster cluster = makeCluster();

    UpgradeContext context = new UpgradeContext(cluster, UpgradeType.ROLLING, Direction.UPGRADE, null);
    context.setSourceAndTargetStacks(HDP_21, HDP_21);
    context.setVersion(UPGRADE_VERSION);
    context.setResolver(m_masterHostResolver);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade,
        context);

    assertEquals(7, groups.size());

    // grab the first configure task out of Hive
    UpgradeGroupHolder hiveGroup = groups.get(4);
    assertEquals("HIVE", hiveGroup.name);
    ConfigureTask configureTask = (ConfigureTask) hiveGroup.items.get(1).getTasks().get(0).getTasks().get(0);

    Map<String, String> configProperties = configureTask.getConfigurationChanges(cluster, cup);
    assertFalse(configProperties.isEmpty());
    assertEquals(configProperties.get(ConfigureTask.PARAMETER_CONFIG_TYPE), "hive-site");

    // now set the property in the if-check in the set element so that we have a match
    Map<String, String> hiveConfigs = new HashMap<String, String>();
    hiveConfigs.put("fooKey", "THIS-BETTER-CHANGE");
    hiveConfigs.put("ifFooKey", "ifFooValue");
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

    // the configure task should now return different properties to set based on
    // the if-condition checks
    configProperties = configureTask.getConfigurationChanges(cluster, cup);
    assertFalse(configProperties.isEmpty());
    assertEquals( configProperties.get(ConfigureTask.PARAMETER_CONFIG_TYPE), "hive-site");

    String configurationJson = configProperties.get(ConfigureTask.PARAMETER_KEY_VALUE_PAIRS);
    assertNotNull(configurationJson);

    List<ConfigUpgradeChangeDefinition.ConfigurationKeyValue> keyValuePairs = m_gson.fromJson(
        configurationJson,
        new TypeToken<List<ConfigUpgradeChangeDefinition.ConfigurationKeyValue>>() {
        }.getType());

    assertEquals("fooKey", keyValuePairs.get(0).key);
    assertEquals("fooValue", keyValuePairs.get(0).value);
  }

  @Test
  public void testConfigureTaskWithMultipleConfigurations() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.containsKey("upgrade_test"));
    UpgradePack upgrade = upgrades.get("upgrade_test");
    ConfigUpgradePack cup = ambariMetaInfo.getConfigUpgradePack("HDP", "2.1.1");
    assertNotNull(upgrade);
    Cluster cluster = makeCluster();

    UpgradeContext context = new UpgradeContext(cluster, UpgradeType.ROLLING, Direction.UPGRADE, null);
    context.setSourceAndTargetStacks(HDP_21, HDP_21);
    context.setVersion(UPGRADE_VERSION);
    context.setResolver(m_masterHostResolver);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    assertEquals(7, groups.size());

    // grab the configure task out of Hive
    UpgradeGroupHolder hiveGroup = groups.get(4);
    assertEquals("HIVE", hiveGroup.name);
    ConfigureTask configureTask = (ConfigureTask) hiveGroup.items.get(1).getTasks().get(0).getTasks().get(0);

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

    UpgradeContext context = new UpgradeContext(c, UpgradeType.ROLLING, Direction.UPGRADE, null);
    context.setSourceAndTargetStacks(HDP_21, HDP_22);
    context.setVersion(UPGRADE_VERSION);
    context.setResolver(m_masterHostResolver);

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

    assertEquals(1, manualTask.messages.size());
    assertEquals(
        "This is a manual task with a placeholder of placeholder-rendered-properly",
        manualTask.messages.get(0));
  }

  @Test
  public void testServiceCheckDowngradeStages() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.containsKey("upgrade_test_checks"));
    UpgradePack upgrade = upgrades.get("upgrade_test_checks");
    assertNotNull(upgrade);

    Cluster cluster = makeCluster();

    UpgradeContext context = new UpgradeContext(cluster, UpgradeType.ROLLING, Direction.DOWNGRADE, null);
    context.setSourceAndTargetStacks(HDP_21, HDP_21);
    context.setVersion(DOWNGRADE_VERSION);
    context.setResolver(m_masterHostResolver);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    assertEquals(5, groups.size());

    // grab the manual task out of ZK which has placeholder text

    UpgradeGroupHolder zookeeperGroup = groups.get(3);
    assertEquals("ZOOKEEPER", zookeeperGroup.name);
    ManualTask manualTask = (ManualTask) zookeeperGroup.items.get(0).getTasks().get(
        0).getTasks().get(0);

    assertEquals(1, manualTask.messages.size());
    assertEquals(
        "This is a manual task with a placeholder of placeholder-rendered-properly",
        manualTask.messages.get(0));
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

    Cluster cluster = makeCluster();

    UpgradeContext context = new UpgradeContext(cluster, UpgradeType.ROLLING, Direction.UPGRADE, null);
    context.setSourceAndTargetStacks(HDP_21, HDP_21);
    context.setVersion(UPGRADE_VERSION);
    context.setResolver(m_masterHostResolver);

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
    assertTrue(mt.messages.get(0).contains("DataNode and NodeManager"));
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

    // Do stacks cleanup
    stackManagerMock.invalidateCurrentPaths();
    ambariMetaInfo.init();
  }


  private Cluster makeCluster() throws AmbariException, AuthorizationException {
    return makeCluster(true);
  }


  /**
   * Create an HA cluster
   * @throws AmbariException
   */
  private Cluster makeCluster(boolean clean) throws AmbariException, AuthorizationException {
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
        RepositoryVersionState.INSTALLING);

    for (int i = 0; i < 4; i++) {
      String hostName = "h" + (i+1);
      clusters.addHost(hostName);
      Host host = clusters.getHost(hostName);

      Map<String, String> hostAttributes = new HashMap<String, String>();
      hostAttributes.put("os_family", "redhat");
      hostAttributes.put("os_release_version", "6");

      host.setHostAttributes(hostAttributes);

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

    Cluster cluster = makeCluster();

    UpgradeContext context = new UpgradeContext(cluster, UpgradeType.ROLLING, Direction.UPGRADE, null);
    context.setSourceAndTargetStacks(HDP_21, HDP_21);
    context.setVersion(UPGRADE_VERSION);
    context.setResolver(m_masterHostResolver);

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
        RepositoryVersionState.INSTALLING);

    for (int i = 0; i < 2; i++) {
      String hostName = "h" + (i+1);
      clusters.addHost(hostName);
      Host host = clusters.getHost(hostName);

      Map<String, String> hostAttributes = new HashMap<String, String>();
      hostAttributes.put("os_family", "redhat");
      hostAttributes.put("os_release_version", "6");

      host.setHostAttributes(hostAttributes);

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

    UpgradeContext context = new UpgradeContext(c, UpgradeType.ROLLING, Direction.DOWNGRADE, null);
    context.setSourceAndTargetStacks(HDP_21, HDP_21);
    context.setVersion(DOWNGRADE_VERSION);
    context.setResolver(m_masterHostResolver);

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
        RepositoryVersionState.INSTALLING);

    for (int i = 0; i < 2; i++) {
      String hostName = "h" + (i+1);
      clusters.addHost(hostName);
      Host host = clusters.getHost(hostName);

      Map<String, String> hostAttributes = new HashMap<String, String>();
      hostAttributes.put("os_family", "redhat");
      hostAttributes.put("os_release_version", "6");

      host.setHostAttributes(hostAttributes);

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
    MasterHostResolver mhr = new MasterHostResolver(null, c, "2.1.1.0-1234");

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
        RepositoryVersionState.INSTALLING);

    for (int i = 0; i < 2; i++) {
      String hostName = "h" + (i+1);
      clusters.addHost(hostName);
      Host host = clusters.getHost(hostName);

      Map<String, String> hostAttributes = new HashMap<String, String>();
      hostAttributes.put("os_family", "redhat");
      hostAttributes.put("os_release_version", "6");

      host.setHostAttributes(hostAttributes);

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
    expect(m_configHelper.getValueFromDesiredConfigurations(c, "hdfs-site", "dfs.internal.nameservices")).andReturn("ha").anyTimes();
    expect(m_configHelper.getValueFromDesiredConfigurations(c, "hdfs-site", "dfs.ha.namenodes.ha")).andReturn("nn1,nn2").anyTimes();
    expect(m_configHelper.getValueFromDesiredConfigurations(c, "hdfs-site", "dfs.http.policy")).andReturn("HTTP_ONLY").anyTimes();

    // Notice that these names are all caps.
    expect(m_configHelper.getValueFromDesiredConfigurations(c, "hdfs-site", "dfs.namenode.http-address.ha.nn1")).andReturn("H1:50070").anyTimes();
    expect(m_configHelper.getValueFromDesiredConfigurations(c, "hdfs-site", "dfs.namenode.http-address.ha.nn2")).andReturn("H2:50070").anyTimes();
    replay(m_configHelper);

    MasterHostResolver mhr = new MockMasterHostResolver(m_configHelper, c, version);

    HostsType ht = mhr.getMasterAndHosts("HDFS", "NAMENODE");
    assertNotNull(ht.master);
    assertNotNull(ht.secondary);
    assertEquals(2, ht.hosts.size());

    // Should be stored in lowercase.
    assertTrue(ht.hosts.contains("h1"));
    assertTrue(ht.hosts.contains("h1"));
  }

  @Test
  public void testResolverBadJmx() throws Exception {
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
        RepositoryVersionState.INSTALLING);

    for (int i = 0; i < 2; i++) {
      String hostName = "h" + (i+1);
      clusters.addHost(hostName);
      Host host = clusters.getHost(hostName);

      Map<String, String> hostAttributes = new HashMap<String, String>();
      hostAttributes.put("os_family", "redhat");
      hostAttributes.put("os_release_version", "6");

      host.setHostAttributes(hostAttributes);

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
    expect(m_configHelper.getValueFromDesiredConfigurations(c, "hdfs-site", "dfs.internal.nameservices")).andReturn("ha").anyTimes();
    expect(m_configHelper.getValueFromDesiredConfigurations(c, "hdfs-site", "dfs.ha.namenodes.ha")).andReturn("nn1,nn2").anyTimes();
    expect(m_configHelper.getValueFromDesiredConfigurations(c, "hdfs-site", "dfs.http.policy")).andReturn("HTTP_ONLY").anyTimes();

    // Notice that these names are all caps.
    expect(m_configHelper.getValueFromDesiredConfigurations(c, "hdfs-site", "dfs.namenode.http-address.ha.nn1")).andReturn("H1:50070").anyTimes();
    expect(m_configHelper.getValueFromDesiredConfigurations(c, "hdfs-site", "dfs.namenode.http-address.ha.nn2")).andReturn("H2:50070").anyTimes();
    replay(m_configHelper);

    MasterHostResolver mhr = new BadMasterHostResolver(m_configHelper, c, version);

    HostsType ht = mhr.getMasterAndHosts("HDFS", "NAMENODE");
    assertNotNull(ht.master);
    assertNotNull(ht.secondary);
    assertEquals(2, ht.hosts.size());

    // Should be stored in lowercase.
    assertTrue(ht.hosts.contains("h1"));
    assertTrue(ht.hosts.contains("h2"));
  }


  /**
   * Tests that advanced {@link Grouping} instances like {@link StopGrouping}
   * work with rolling upgrade packs.
   *
   * @throws Exception
   */
  @Test
  public void testRollingUpgradesCanUseAdvancedGroupings() throws Exception {
    final String clusterName = "c1";
    final String upgradeFromVersion = "2.1.1";
    final String upgradeToVersion = "2.2.0";
    final Direction upgradeDirection = Direction.UPGRADE;
    final UpgradeType upgradeType = UpgradeType.ROLLING;

    Cluster cluster = makeCluster();

    // grab the right pack
    String preferredUpgradePackName = "upgrade_grouping_rolling";
    UpgradePack upgradePack = m_upgradeHelper.suggestUpgradePack(clusterName, upgradeFromVersion,
        upgradeToVersion, upgradeDirection, upgradeType, preferredUpgradePackName);

    assertEquals(upgradeType, upgradePack.getType());

    // get an upgrade
    UpgradeContext context = new UpgradeContext(cluster, UpgradeType.ROLLING, Direction.UPGRADE, null);
    context.setSourceAndTargetStacks(HDP_21, HDP_21);
    context.setVersion(UPGRADE_VERSION);
    context.setResolver(m_masterHostResolver);

    context.setSupportedServices(Collections.singleton("ZOOKEEPER"));
    context.setScope(UpgradeScope.COMPLETE);

    List<Grouping> groupings = upgradePack.getGroups(Direction.UPGRADE);
    assertEquals(2, groupings.size());
    assertEquals("STOP_ZOOKEEPER", groupings.get(0).name);
    assertEquals("RESTART_ZOOKEEPER", groupings.get(1).name);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgradePack, context);

    assertEquals(2, groups.size());

    assertEquals("STOP_ZOOKEEPER", groups.get(0).name);
    assertEquals("RESTART_ZOOKEEPER", groups.get(1).name);

    // STOP_ZOOKEEPER GROUP
    UpgradeGroupHolder group = groups.get(0);

    // Check that the upgrade framework properly expanded the STOP grouping into
    // STOP tasks
    assertEquals("Stopping ZooKeeper Server on h1 (Batch 1 of 3)", group.items.get(0).getText());
  }

  @Test
  public void testOrchestrationNoServerSideOnDowngrade() throws Exception {
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
        RepositoryVersionState.INSTALLING);

    for (int i = 0; i < 2; i++) {
      String hostName = "h" + (i+1);
      clusters.addHost(hostName);
      Host host = clusters.getHost(hostName);

      Map<String, String> hostAttributes = new HashMap<String, String>();
      hostAttributes.put("os_family", "redhat");
      hostAttributes.put("os_release_version", "6");

      host.setHostAttributes(hostAttributes);

      clusters.mapHostToCluster(hostName, clusterName);
    }

    // !!! add storm
    c.addService(serviceFactory.createNew(c, "STORM"));

    Service s = c.getService("STORM");
    ServiceComponent sc = s.addServiceComponent("NIMBUS");
    ServiceComponentHost sch1 = sc.addServiceComponentHost("h1");
    ServiceComponentHost sch2 = sc.addServiceComponentHost("h2");

    UpgradePack upgradePack = new UpgradePack() {
      @Override
      public List<Grouping> getGroups(Direction direction) {

        Grouping g = new Grouping();

        OrderService orderService = new OrderService();
        orderService.serviceName = "STORM";
        orderService.components = Collections.singletonList("NIMBUS");

        g.name = "GROUP1";
        g.title = "Nimbus Group";
        g.services.add(orderService);

        return Lists.newArrayList(g);
      }

      @Override
      public Map<String, Map<String, ProcessingComponent>> getTasks() {
        ManualTask mt = new ManualTask();
        mt.messages = Lists.newArrayList("My New Message");

        ProcessingComponent pc = new ProcessingComponent();
        pc.name = "NIMBUS_MESSAGE";
        pc.preTasks = Lists.<Task>newArrayList(mt);

        return Collections.singletonMap("STORM", Collections.singletonMap("NIMBUS", pc));
      }

    };

    MasterHostResolver resolver = new MasterHostResolver(m_configHelper, c);

    UpgradeContext context = new UpgradeContext(c, UpgradeType.NON_ROLLING, Direction.UPGRADE, null);
    context.setSourceAndTargetStacks(stackId, stackId2);
    context.setVersion("2.2.0");
    context.setResolver(resolver);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgradePack, context);

    assertEquals(1, groups.size());

    sch1.setVersion("2.1.1");
    sch2.setVersion("2.1.1");
    resolver = new MasterHostResolver(m_configHelper, c, "2.1.1");

    context = new UpgradeContext(c, UpgradeType.NON_ROLLING, Direction.DOWNGRADE, null);
    context.setSourceAndTargetStacks(stackId2, stackId);
    context.setVersion("2.1.1");
    context.setResolver(resolver);

    groups = m_upgradeHelper.createSequence(upgradePack, context);

    assertTrue(groups.isEmpty());
  }

  @Test
  public void testHostGroupingOrchestration() throws Exception {

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
        RepositoryVersionState.INSTALLING);

    for (int i = 0; i < 2; i++) {
      String hostName = "h" + (i+1);
      clusters.addHost(hostName);
      Host host = clusters.getHost(hostName);

      Map<String, String> hostAttributes = new HashMap<String, String>();
      hostAttributes.put("os_family", "redhat");
      hostAttributes.put("os_release_version", "6");

      host.setHostAttributes(hostAttributes);

      clusters.mapHostToCluster(hostName, clusterName);
    }

    // !!! add storm
    c.addService(serviceFactory.createNew(c, "ZOOKEEPER"));

    Service s = c.getService("ZOOKEEPER");
    ServiceComponent sc = s.addServiceComponent("ZOOKEEPER_SERVER");
    ServiceComponentHost sch1 = sc.addServiceComponentHost("h1");
    ServiceComponentHost sch2 = sc.addServiceComponentHost("h2");

    // !!! make a custom grouping
    HostOrderItem hostItem = new HostOrderItem(HostOrderActionType.HOST_UPGRADE,
        Lists.newArrayList("h1", "h2"));
    HostOrderItem checkItem = new HostOrderItem(HostOrderActionType.SERVICE_CHECK,
        Lists.newArrayList("ZOOKEEPER", "STORM"));

    Grouping g = new HostOrderGrouping();
    ((HostOrderGrouping) g).setHostOrderItems(Lists.newArrayList(hostItem, checkItem));
    g.title = "Some Title";

    UpgradePack upgradePack = new UpgradePack();

    // !!! set the groups directly; allow the logic in getGroups(Direction) to happen
    Field field = UpgradePack.class.getDeclaredField("groups");
    field.setAccessible(true);
    field.set(upgradePack, Lists.newArrayList(g));

    field = UpgradePack.class.getDeclaredField("type" );
    field.setAccessible(true);
    field.set(upgradePack, UpgradeType.HOST_ORDERED);


    MasterHostResolver resolver = new MasterHostResolver(m_configHelper, c);
    UpgradeContext context = new UpgradeContext(c, UpgradeType.HOST_ORDERED, Direction.UPGRADE, new HashMap<String, Object>());
    context.setResolver(resolver);
    context.setSourceAndTargetStacks(stackId, stackId2);
    context.setVersion("2.2.0");
    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgradePack, context);

    assertEquals(1, groups.size());

    UpgradeGroupHolder holder = groups.get(0);
    assertEquals(7, holder.items.size());

    for (int i = 0; i < 6; i++) {
      StageWrapper w = holder.items.get(i);
      if (i == 0 || i == 3) {
        assertEquals(StageWrapper.Type.STOP, w.getType());
      } else if (i == 1 || i == 4) {
        assertEquals(StageWrapper.Type.SERVER_SIDE_ACTION, w.getType());
        assertEquals(1, w.getTasks().size());
        assertEquals(1, w.getTasks().get(0).getTasks().size());
        Task t = w.getTasks().get(0).getTasks().get(0);
        assertEquals(ManualTask.class, t.getClass());
        ManualTask mt = (ManualTask) t;
        assertNotNull(mt.structuredOut);
        assertTrue(mt.structuredOut.contains("type"));
        assertTrue(mt.structuredOut.contains(HostOrderItem.HostOrderActionType.HOST_UPGRADE.toString()));
        assertTrue(mt.structuredOut.contains("host"));
        assertTrue(mt.structuredOut.contains(i == 1 ? "h1" : "h2"));
      } else {
        assertEquals(StageWrapper.Type.RESTART, w.getType());
      }
    }
    assertEquals(StageWrapper.Type.SERVICE_CHECK, holder.items.get(6).getType());

    // !!! test downgrade when all host components have failed
    sch1.setVersion("2.1.1");
    sch2.setVersion("2.1.1");
    resolver = new MasterHostResolver(m_configHelper, c, "2.1.1");
    context = new UpgradeContext(c, UpgradeType.HOST_ORDERED, Direction.DOWNGRADE, new HashMap<String, Object>());
    context.setResolver(resolver);
    context.setSourceAndTargetStacks(stackId2, stackId);
    context.setVersion("2.1.1");
    groups = m_upgradeHelper.createSequence(upgradePack, context);

    assertEquals(1, groups.size());
    assertEquals(1, groups.get(0).items.size());

    // !!! test downgrade when one of the hosts had failed
    sch1.setVersion("2.1.1");
    sch2.setVersion("2.2.0");
    resolver = new MasterHostResolver(m_configHelper, c, "2.1.1");
    context = new UpgradeContext(c, UpgradeType.HOST_ORDERED, Direction.DOWNGRADE, new HashMap<String, Object>());
    context.setResolver(resolver);
    context.setSourceAndTargetStacks(stackId2, stackId);
    context.setVersion("2.1.1");
    groups = m_upgradeHelper.createSequence(upgradePack, context);

    assertEquals(1, groups.size());
    assertEquals(4, groups.get(0).items.size());
  }

  /**
   * Tests that the {@link SecurityCondition} element correctly restricts the groups in
   * an upgrade.
   *
   * @throws Exception
   */
  @Test
  public void testUpgradeConditions() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.2.0");
    assertTrue(upgrades.containsKey("upgrade_test_conditions"));
    UpgradePack upgrade = upgrades.get("upgrade_test_conditions");
    assertNotNull(upgrade);

    Cluster cluster = makeCluster();

    UpgradeContext context = new UpgradeContext(cluster, UpgradeType.ROLLING, Direction.UPGRADE, null);
    context.setSourceAndTargetStacks(HDP_22, HDP_22);
    context.setVersion(UPGRADE_VERSION);
    context.setResolver(m_masterHostResolver);

    // initially, no conditions should be met
    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);
    assertEquals(0, groups.size());

    // set the configuration property and try again
    Map<String, String> fooConfigs = new HashMap<String, String>();
    fooConfigs.put("foo-property", "foo-value");
    ConfigurationRequest configurationRequest = new ConfigurationRequest();
    configurationRequest.setClusterName(cluster.getClusterName());
    configurationRequest.setType("foo-site");
    configurationRequest.setVersionTag("version1");
    configurationRequest.setProperties(fooConfigs);

    final ClusterRequest clusterRequest = new ClusterRequest(cluster.getClusterId(),
        cluster.getClusterName(), cluster.getDesiredStackVersion().getStackVersion(), null);

    clusterRequest.setDesiredConfig(Collections.singletonList(configurationRequest));
    m_managementController.updateClusters(Sets.newHashSet(clusterRequest), null);

    // the config condition should now be set
    groups = m_upgradeHelper.createSequence(upgrade, context);
    assertEquals(1, groups.size());
    assertEquals("ZOOKEEPER_CONFIG_CONDITION_TEST", groups.get(0).name);

    // now change the cluster security so the other conditions come back too
    cluster.setSecurityType(SecurityType.KERBEROS);

    groups = m_upgradeHelper.createSequence(upgrade, context);
    assertEquals(3, groups.size());
  }

  /**
   * Extend {@link org.apache.ambari.server.stack.MasterHostResolver} in order
   * to overwrite the JMX methods.
   */
  private class MockMasterHostResolver extends MasterHostResolver {

    public MockMasterHostResolver(ConfigHelper configHelper, Cluster cluster, String version) {
      super(configHelper, cluster, version);
    }

    /**
     * Mock the call to get JMX Values.
     * @param hostname host name
     * @param port port number
     * @param beanName if asQuery is false, then search for this bean name
     * @param attributeName if asQuery is false, then search for this attribute name
     * @param asQuery whether to search bean or query
     * @param encrypted true if using https instead of http.
     * @return
     */
    @Override
    public String queryJmxBeanValue(String hostname, int port, String beanName, String attributeName,
                                    boolean asQuery, boolean encrypted) {

      if (beanName.equalsIgnoreCase("Hadoop:service=NameNode,name=NameNodeStatus") && attributeName.equalsIgnoreCase("State") && asQuery) {
        switch (hostname) {
          case "H1":
            return Status.ACTIVE.toString();
          case "H2":
            return Status.STANDBY.toString();
          default:
            return "UNKNOWN_NAMENODE_STATUS_FOR_THIS_HOST";
        }
      }
      return  "NOT_MOCKED";
    }
  }

  private class MockModule implements Module {

    @Override
    public void configure(Binder binder) {
      binder.bind(ConfigHelper.class).toInstance(m_configHelper);
    }
  }

  private static class BadMasterHostResolver extends MasterHostResolver {

    public BadMasterHostResolver(ConfigHelper configHelper, Cluster cluster, String version) {
      super(configHelper, cluster, version);
    }

    @Override
    protected String queryJmxBeanValue(String hostname, int port, String beanName,
        String attributeName, boolean asQuery, boolean encrypted) {
      return null;
    }

  }
}
