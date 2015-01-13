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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.stack.HostsType;
import org.apache.ambari.server.stack.MasterHostResolver;
import org.apache.ambari.server.state.UpgradeHelper.UpgradeGroupHolder;
import org.apache.ambari.server.state.stack.UpgradePack;
import org.apache.ambari.server.state.stack.upgrade.ManualTask;
import org.apache.ambari.server.state.stack.upgrade.StageWrapper;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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

  private static final String UPGRADE_VERSION = "2.2.1.0-1234";
  private static final String DOWNGRADE_VERSION = "2.2.0.0-1234";

  private Injector injector;
  private AmbariMetaInfo ambariMetaInfo;
  private OrmTestHelper helper;
  private MasterHostResolver m_masterHostResolver;
  private UpgradeHelper m_upgradeHelper;
  private ConfigHelper m_configHelper;

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
    ambariMetaInfo.init();

    m_upgradeHelper = injector.getInstance(UpgradeHelper.class);
    m_masterHostResolver = EasyMock.createMock(MasterHostResolver.class);
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
    assertTrue(upgrades.containsKey("upgrade_test"));
    UpgradePack upgrade = upgrades.get("upgrade_test");
    assertNotNull(upgrade);

    Cluster cluster = makeCluster();

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createUpgrade(
        m_masterHostResolver, upgrade, UPGRADE_VERSION);

    assertEquals(5, groups.size());

    assertEquals("PRE_CLUSTER", groups.get(0).name);
    assertEquals("ZOOKEEPER", groups.get(1).name);
    assertEquals("CORE_MASTER", groups.get(2).name);
    assertEquals("CORE_SLAVES", groups.get(3).name);

    UpgradeGroupHolder postGroup = groups.get(4);
    assertEquals(postGroup.name, "POST_CLUSTER");
    assertEquals(postGroup.title, "Finalize Upgrade");
    assertEquals(postGroup.items.size(), 3);
    assertEquals(postGroup.items.get(0).getText(), "Confirm Finalize");
    assertEquals(postGroup.items.get(1).getText(), "Execute HDFS Finalize");
    assertEquals(postGroup.items.get(2).getText(), "Save Cluster State");
    assertEquals(postGroup.items.get(2).getType(), StageWrapper.Type.SERVER_SIDE_ACTION);

    assertEquals(6, groups.get(1).items.size());
    assertEquals(8, groups.get(2).items.size());
    assertEquals(7, groups.get(3).items.size());
  }

  @Test
  public void testDowngradeOrchestration() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("foo", "bar");
    assertTrue(upgrades.isEmpty());

    upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.containsKey("upgrade_test"));
    UpgradePack upgrade = upgrades.get("upgrade_test");
    assertNotNull(upgrade);

    Cluster cluster = makeCluster();

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createDowngrade(
        m_masterHostResolver, upgrade, DOWNGRADE_VERSION);

    assertEquals(5, groups.size());

    UpgradeGroupHolder preGroup = groups.get(0);
    assertEquals("PRE_CLUSTER", preGroup.name);

    assertEquals("CORE_SLAVES", groups.get(1).name);
    assertEquals("CORE_MASTER", groups.get(2).name);
    assertEquals("ZOOKEEPER", groups.get(3).name);

    UpgradeGroupHolder postGroup = groups.get(4);
    assertEquals(postGroup.name, "POST_CLUSTER");
    assertEquals(postGroup.title, "Finalize Upgrade");
    assertEquals(postGroup.items.size(), 3);
    assertEquals(postGroup.items.get(0).getText(), "Confirm Finalize");
    assertEquals(postGroup.items.get(1).getText(), "Execute HDFS Finalize");
    assertEquals(postGroup.items.get(2).getText(), "Save Cluster State");
    assertEquals(postGroup.items.get(2).getType(), StageWrapper.Type.SERVER_SIDE_ACTION);

    assertEquals(9, groups.get(1).items.size());
    assertEquals(8, groups.get(2).items.size());
    assertEquals(6, groups.get(3).items.size());
  }

  @Test
  public void testBuckets() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("foo", "bar");
    assertTrue(upgrades.isEmpty());

    upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.containsKey("upgrade_bucket_test"));
    UpgradePack upgrade = upgrades.get("upgrade_bucket_test");
    assertNotNull(upgrade);

    Cluster cluster = makeCluster();

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createUpgrade(
        m_masterHostResolver, upgrade, UPGRADE_VERSION);

    assertEquals(1, groups.size());
    UpgradeGroupHolder group = groups.iterator().next();

    assertEquals(6, group.items.size());
  }

  @Test
  public void testManualTaskPostProcessing() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("foo", "bar");
    assertTrue(upgrades.isEmpty());

    upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.containsKey("upgrade_test"));
    UpgradePack upgrade = upgrades.get("upgrade_test");
    assertNotNull(upgrade);

    Cluster cluster = makeCluster();

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createUpgrade(
        m_masterHostResolver, upgrade, UPGRADE_VERSION);

    assertEquals(5, groups.size());

    // grab the manual task out of ZK which has placeholder text
    UpgradeGroupHolder zookeeperGroup = groups.get(1);
    assertEquals("ZOOKEEPER", zookeeperGroup.name);
    ManualTask manualTask = (ManualTask) zookeeperGroup.items.get(0).getTasks().get(
        0).getTasks().get(0);

    assertEquals(
        "This is a manual task with a placeholder of placeholder-rendered-properly",
        manualTask.message);
  }

  /**
   * Create an HA cluster
   * @throws AmbariException
   */
  public Cluster makeCluster() throws AmbariException {
    Clusters clusters = injector.getInstance(Clusters.class);
    ServiceFactory serviceFactory = injector.getInstance(ServiceFactory.class);
    String clusterName = "c1";

    clusters.addCluster(clusterName);
    Cluster c = clusters.getCluster(clusterName);
    c.setDesiredStackVersion(new StackId("HDP-2.1.1"));
    helper.getOrCreateRepositoryVersion(c.getDesiredStackVersion().getStackName(),
        c.getDesiredStackVersion().getStackVersion());
    c.createClusterVersion(c.getDesiredStackVersion().getStackName(),
        c.getDesiredStackVersion().getStackVersion(), "admin", RepositoryVersionState.CURRENT);
    for (int i = 0; i < 3; i++) {
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

    Service s = c.getService("HDFS");
    ServiceComponent sc = s.addServiceComponent("NAMENODE");
    sc.addServiceComponentHost("h1");
    sc.addServiceComponentHost("h2");
    sc = s.addServiceComponent("DATANODE");
    sc.addServiceComponentHost("h2");
    sc.addServiceComponentHost("h3");

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

    HostsType type = new HostsType();
    type.hosts = new HashSet<String>(Arrays.asList("h1", "h2", "h3"));
    expect(m_masterHostResolver.getMasterAndHosts("ZOOKEEPER", "ZOOKEEPER_SERVER")).andReturn(type).anyTimes();

    type = new HostsType();
    type.hosts = new HashSet<String>(Arrays.asList("h1", "h2"));
    type.master = "h1";
    type.secondary = "h2";
    expect(m_masterHostResolver.getMasterAndHosts("HDFS", "NAMENODE")).andReturn(type).anyTimes();

    type = new HostsType();
    type.hosts = new HashSet<String>(Arrays.asList("h2", "h3"));
    expect(m_masterHostResolver.getMasterAndHosts("HDFS", "DATANODE")).andReturn(type).anyTimes();

    type = new HostsType();
    type.hosts = new HashSet<String>(Arrays.asList("h2"));
    expect(m_masterHostResolver.getMasterAndHosts("YARN", "RESOURCEMANAGER")).andReturn(type).anyTimes();

    type = new HostsType();
    type.hosts = new HashSet<String>(Arrays.asList("h1", "h3"));
    expect(m_masterHostResolver.getMasterAndHosts("YARN", "NODEMANAGER")).andReturn(type).anyTimes();

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
