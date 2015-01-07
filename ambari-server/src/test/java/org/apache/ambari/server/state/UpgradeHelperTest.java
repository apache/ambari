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
import org.apache.ambari.server.state.stack.upgrade.StageWrapper;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

/**
 * Tests the {@link UpgradeHelper} class
 */
public class UpgradeHelperTest {

  private Injector injector;
  private AmbariMetaInfo ambariMetaInfo;
  private OrmTestHelper helper;
  private MasterHostResolver m_masterHostResolver;

  @Before
  public void before() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());

    injector.getInstance(GuiceJpaInitializer.class);

    helper = injector.getInstance(OrmTestHelper.class);
    ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    ambariMetaInfo.init();

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

    UpgradeHelper helper = new UpgradeHelper();
    List<UpgradeGroupHolder> groups = helper.createUpgrade(cluster, m_masterHostResolver, upgrade);

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

    UpgradeHelper helper = new UpgradeHelper();
    List<UpgradeGroupHolder> groups = helper.createDowngrade(cluster, m_masterHostResolver, upgrade);

    assertEquals(5, groups.size());

    assertEquals("PRE_CLUSTER", groups.get(0).name);
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

    UpgradeHelper helper = new UpgradeHelper();
    List<UpgradeGroupHolder> groups = helper.createUpgrade(cluster, m_masterHostResolver, upgrade);

    assertEquals(1, groups.size());
    UpgradeGroupHolder group = groups.iterator().next();

    assertEquals(7, group.items.size());
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


    replay(m_masterHostResolver);

    return c;
  }


}
