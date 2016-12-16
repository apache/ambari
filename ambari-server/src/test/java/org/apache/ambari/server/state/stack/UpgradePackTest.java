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
package org.apache.ambari.server.state.stack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.stack.ModuleFileUnmarshaller;
import org.apache.ambari.server.state.stack.UpgradePack.PrerequisiteCheckConfig;
import org.apache.ambari.server.state.stack.UpgradePack.ProcessingComponent;
import org.apache.ambari.server.state.stack.upgrade.ClusterGrouping;
import org.apache.ambari.server.state.stack.upgrade.ClusterGrouping.ExecuteStage;
import org.apache.ambari.server.state.stack.upgrade.ConfigureTask;
import org.apache.ambari.server.state.stack.upgrade.Direction;
import org.apache.ambari.server.state.stack.upgrade.Grouping;
import org.apache.ambari.server.state.stack.upgrade.HostOrderGrouping;
import org.apache.ambari.server.state.stack.upgrade.ParallelScheduler;
import org.apache.ambari.server.state.stack.upgrade.RestartGrouping;
import org.apache.ambari.server.state.stack.upgrade.RestartTask;
import org.apache.ambari.server.state.stack.upgrade.ServiceCheckGrouping;
import org.apache.ambari.server.state.stack.upgrade.StopGrouping;
import org.apache.ambari.server.state.stack.upgrade.Task;
import org.apache.ambari.server.state.stack.upgrade.UpdateStackGrouping;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

/**
 * Tests for the upgrade pack
 */
@Category({ category.StackUpgradeTest.class})
public class UpgradePackTest {

  private Injector injector;
  private AmbariMetaInfo ambariMetaInfo;

  private static final Logger LOG = LoggerFactory.getLogger(UpgradePackTest.class);

  @Before
  public void before() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);

    ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
  }

  @After
  public void teardown() {
    injector.getInstance(PersistService.class).stop();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void findAndValidateUpgradePacks() throws Exception {

    IOFileFilter filter = new IOFileFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return false;
      }

      @Override
      public boolean accept(File file) {
        // file has the folder named 'upgrades', ends with '.xml' and is NOT 'config-upgrade.xml'
        if (file.getAbsolutePath().contains("upgrades") &&
            file.getAbsolutePath().endsWith(".xml") &&
            !file.getAbsolutePath().contains("config-upgrade.xml")) {

          return true;
        }

        return false;
      }
    };

    List<File> files = new ArrayList<>();

    files.addAll(FileUtils.listFiles(new File("src/main/resources/stacks"), filter,
      FileFilterUtils.directoryFileFilter()));

    files.addAll(FileUtils.listFiles(new File("src/test/resources/stacks"), filter,
        FileFilterUtils.directoryFileFilter()));

    files.addAll(FileUtils.listFiles(new File("src/test/resources/stacks_with_upgrade_cycle"), filter,
        FileFilterUtils.directoryFileFilter()));

    ModuleFileUnmarshaller unmarshaller = new ModuleFileUnmarshaller();

    for (File file : files) {
      String fileContent = FileUtils.readFileToString(file, "UTF-8");

      // these things must be in upgrade packs for them to work anyway
      if (fileContent.contains("<upgrade") && fileContent.contains("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"")) {
        if (!fileContent.contains("xsi:noNamespaceSchemaLocation=\"upgrade-pack.xsd\"")) {
          String msg = String.format("File %s appears to be an upgrade pack, but does not define 'upgrade-pack.xsd' as its schema",
              file.getAbsolutePath());
          Assert.fail(msg);
        } else {
          unmarshaller.unmarshal(UpgradePack.class, file, true);
        }
      }
    }
  }

  @Test
  public void testExistence() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("foo", "bar");
    assertTrue(upgrades.isEmpty());

    upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.size() > 0);
    assertTrue(upgrades.containsKey("upgrade_test"));
  }

  @Test
  public void testUpgradeParsing() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.size() > 0);
    assertTrue(upgrades.containsKey("upgrade_test"));
    UpgradePack upgrade = upgrades.get("upgrade_test");
    assertEquals("2.2.*.*", upgrade.getTarget());

    Map<String, List<String>> expectedStages = new LinkedHashMap<String, List<String>>() {{
      put("ZOOKEEPER", Arrays.asList("ZOOKEEPER_SERVER"));
      put("HDFS", Arrays.asList("NAMENODE", "DATANODE"));
    }};

    // !!! test the tasks
    int i = 0;
    for (Entry<String, List<String>> entry : expectedStages.entrySet()) {
      assertTrue(upgrade.getTasks().containsKey(entry.getKey()));
      assertEquals(i++, indexOf(upgrade.getTasks(), entry.getKey()));

      // check that the number of components matches
      assertEquals(entry.getValue().size(), upgrade.getTasks().get(entry.getKey()).size());

      // check component ordering
      int j = 0;
      for (String comp : entry.getValue()) {
        assertEquals(j++, indexOf(upgrade.getTasks().get(entry.getKey()), comp));
      }
    }

    // !!! test specific tasks
    assertTrue(upgrade.getTasks().containsKey("HDFS"));
    assertTrue(upgrade.getTasks().get("HDFS").containsKey("NAMENODE"));

    ProcessingComponent pc = upgrade.getTasks().get("HDFS").get("NAMENODE");
    assertNotNull(pc.preTasks);
    assertNotNull(pc.postTasks);
    assertNotNull(pc.tasks);
    assertNull(pc.preDowngradeTasks);
    assertNull(pc.postDowngradeTasks);
    assertEquals(1, pc.tasks.size());

    assertEquals(Task.Type.RESTART, pc.tasks.get(0).getType());
    assertEquals(RestartTask.class, pc.tasks.get(0).getClass());


    assertTrue(upgrade.getTasks().containsKey("ZOOKEEPER"));
    assertTrue(upgrade.getTasks().get("ZOOKEEPER").containsKey("ZOOKEEPER_SERVER"));

    pc = upgrade.getTasks().get("HDFS").get("DATANODE");
    assertNotNull(pc.preDowngradeTasks);
    assertEquals(0, pc.preDowngradeTasks.size());
    assertNotNull(pc.postDowngradeTasks);
    assertEquals(1, pc.postDowngradeTasks.size());


    pc = upgrade.getTasks().get("ZOOKEEPER").get("ZOOKEEPER_SERVER");
    assertNotNull(pc.preTasks);
    assertEquals(1, pc.preTasks.size());
    assertNotNull(pc.postTasks);
    assertEquals(1, pc.postTasks.size());
    assertNotNull(pc.tasks);
    assertEquals(1, pc.tasks.size());

    pc = upgrade.getTasks().get("YARN").get("NODEMANAGER");
    assertNotNull(pc.preTasks);
    assertEquals(2, pc.preTasks.size());
    Task t = pc.preTasks.get(1);
    assertEquals(ConfigureTask.class, t.getClass());
    ConfigureTask ct = (ConfigureTask) t;
    // check that the Configure task successfully parsed id
    assertEquals("hdp_2_1_1_nm_pre_upgrade", ct.getId());
  }

  @Test
  public void testGroupOrdersForRolling() {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.size() > 0);
    assertTrue(upgrades.containsKey("upgrade_test_checks"));
    UpgradePack upgrade = upgrades.get("upgrade_test_checks");

    PrerequisiteCheckConfig prerequisiteCheckConfig = upgrade.getPrerequisiteCheckConfig();
    assertNotNull(prerequisiteCheckConfig);
    assertNotNull(prerequisiteCheckConfig.globalProperties);
    assertTrue(prerequisiteCheckConfig.getGlobalProperties().containsKey("global-property-1"));
    assertEquals("global-value-1", prerequisiteCheckConfig.getGlobalProperties().get("global-property-1"));
    assertNotNull(prerequisiteCheckConfig.prerequisiteCheckProperties);
    assertEquals(2, prerequisiteCheckConfig.prerequisiteCheckProperties.size());
    assertNotNull(prerequisiteCheckConfig.getCheckProperties(
        "org.apache.ambari.server.checks.ServicesMapReduceDistributedCacheCheck"));
    assertTrue(prerequisiteCheckConfig.getCheckProperties(
        "org.apache.ambari.server.checks.ServicesMapReduceDistributedCacheCheck").containsKey("dfs-protocols-regex"));
    assertEquals("^([^:]*dfs|wasb|ecs):.*", prerequisiteCheckConfig.getCheckProperties(
        "org.apache.ambari.server.checks.ServicesMapReduceDistributedCacheCheck").get("dfs-protocols-regex"));
    assertNotNull(prerequisiteCheckConfig.getCheckProperties(
        "org.apache.ambari.server.checks.ServicesTezDistributedCacheCheck"));
    assertTrue(prerequisiteCheckConfig.getCheckProperties(
        "org.apache.ambari.server.checks.ServicesTezDistributedCacheCheck").containsKey("dfs-protocols-regex"));
    assertEquals("^([^:]*dfs|wasb|ecs):.*", prerequisiteCheckConfig.getCheckProperties(
        "org.apache.ambari.server.checks.ServicesTezDistributedCacheCheck").get("dfs-protocols-regex"));


    List<String> expected_up = Arrays.asList(
        "PRE_CLUSTER",
        "ZOOKEEPER",
        "CORE_MASTER",
        "SERVICE_CHECK_1",
        "CORE_SLAVES",
        "SERVICE_CHECK_2",
        "POST_CLUSTER");

    List<String> expected_down = Arrays.asList(
        "PRE_CLUSTER",
        "CORE_SLAVES",
        "SERVICE_CHECK_2",
        "CORE_MASTER",
        "SERVICE_CHECK_1",
        "ZOOKEEPER",
        "POST_CLUSTER");

    Grouping serviceCheckGroup = null;

    int i = 0;
    List<Grouping> groups = upgrade.getGroups(Direction.UPGRADE);
    for (Grouping g : groups) {
      assertEquals(expected_up.get(i), g.name);
      i++;

      if (g.name.equals("SERVICE_CHECK_1")) {
        serviceCheckGroup = g;
      }
    }

    List<String> expected_priority = Arrays.asList("HDFS", "HBASE", "YARN");

    assertNotNull(serviceCheckGroup);
    assertEquals(ServiceCheckGrouping.class, serviceCheckGroup.getClass());
    ServiceCheckGrouping scg = (ServiceCheckGrouping) serviceCheckGroup;

    Set<String> priorities = scg.getPriorities();
    assertEquals(3, priorities.size());

    i = 0;
    for (String s : priorities) {
      assertEquals(expected_priority.get(i++), s);
    }


    i = 0;
    groups = upgrade.getGroups(Direction.DOWNGRADE);
    for (Grouping g : groups) {
      assertEquals(expected_down.get(i), g.name);
      i++;
    }

  }


  // TODO AMBARI-12698, add the Downgrade case
  @Test
  public void testGroupOrdersForNonRolling() {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.size() > 0);
    assertTrue(upgrades.containsKey("upgrade_test_nonrolling"));
    UpgradePack upgrade = upgrades.get("upgrade_test_nonrolling");

    List<String> expected_up = Arrays.asList(
      "PRE_CLUSTER",
      "Stop High-Level Daemons",
      "Backups",
      "Stop Low-Level Daemons",
      "UPDATE_DESIRED_STACK_ID",
      "ALL_HOST_OPS",
      "ZOOKEEPER",
      "HDFS",
      "MR and YARN",
      "POST_CLUSTER");

    List<String> expected_down = Arrays.asList(
      "Restore Backups",
      "UPDATE_DESIRED_STACK_ID",
      "ALL_HOST_OPS",
      "ZOOKEEPER",
      "HDFS",
      "MR and YARN",
      "POST_CLUSTER");


    Iterator<String> itr_up = expected_up.iterator();
    List<Grouping> upgrade_groups = upgrade.getGroups(Direction.UPGRADE);
    for (Grouping g : upgrade_groups) {
      assertEquals(true, itr_up.hasNext());
      assertEquals(itr_up.next(), g.name);
    }

    Iterator<String> itr_down = expected_down.iterator();
    List<Grouping> downgrade_groups = upgrade.getGroups(Direction.DOWNGRADE);
    for (Grouping g : downgrade_groups) {
      assertEquals(true, itr_down.hasNext());
      assertEquals(itr_down.next(), g.name);
    }
  }


  @Test
  public void testDirectionForRolling() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.size() > 0);
    assertTrue(upgrades.containsKey("upgrade_direction"));
    UpgradePack upgrade = upgrades.get("upgrade_direction");
    assertTrue(upgrade.getType() == UpgradeType.ROLLING);

    List<Grouping> groups = upgrade.getGroups(Direction.UPGRADE);
    assertEquals(4, groups.size());
    Grouping group = groups.get(2);
    assertEquals(ClusterGrouping.class, group.getClass());
    ClusterGrouping cluster_group = (ClusterGrouping) group;
    assertEquals("Run on All", group.title);

    cluster_group = (ClusterGrouping) groups.get(3);
    List<ExecuteStage> stages = cluster_group.executionStages;
    assertEquals(3, stages.size());
    assertNotNull(stages.get(0).intendedDirection);
    assertEquals(Direction.DOWNGRADE, stages.get(0).intendedDirection);

    groups = upgrade.getGroups(Direction.DOWNGRADE);
    assertEquals(3, groups.size());
    // there are two clustergroupings at the end
    group = groups.get(1);
    assertEquals(ClusterGrouping.class, group.getClass());
    assertEquals("Run on All", group.title);

    group = groups.get(2);
    assertEquals(ClusterGrouping.class, group.getClass());
    assertEquals("Finalize Upgrade", group.title);
  }

  @Test
  public void testSkippableFailures() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    Set<String> keys = upgrades.keySet();
    for (String key : keys) {
      Assert.assertFalse(upgrades.get(key).isComponentFailureAutoSkipped());
      Assert.assertFalse(upgrades.get(key).isServiceCheckFailureAutoSkipped());
    }

    upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.2.0");
    UpgradePack upgradePack = upgrades.get("upgrade_test_skip_failures");
    Assert.assertTrue(upgradePack.isComponentFailureAutoSkipped());
    Assert.assertTrue(upgradePack.isServiceCheckFailureAutoSkipped());
  }

  /**
   * Tests that the XML for not auto skipping skippable failures works.
   *
   * @throws Exception
   */
  @Test
  public void testNoAutoSkipFailure() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.2.0");
    UpgradePack upgradePack = upgrades.get("upgrade_test_skip_failures");

    List<Grouping> groups = upgradePack.getGroups(Direction.UPGRADE);
    for (Grouping grouping : groups) {
      if (grouping.name.equals("SKIPPABLE_BUT_NOT_AUTO_SKIPPABLE")) {
        Assert.assertFalse(grouping.supportsAutoSkipOnFailure);
      } else {
        Assert.assertTrue(grouping.supportsAutoSkipOnFailure);
      }
    }
  }

  @Test
  public void testDirectionForNonRolling() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.size() > 0);
    assertTrue(upgrades.containsKey("upgrade_test_nonrolling"));
    UpgradePack upgrade = upgrades.get("upgrade_test_nonrolling");
    assertTrue(upgrade.getType() == UpgradeType.NON_ROLLING);

    List<Grouping> groups = upgrade.getGroups(Direction.UPGRADE);
    assertEquals(10, groups.size());

    Grouping group = null;
    ClusterGrouping clusterGroup = null;
    UpdateStackGrouping updateStackGroup = null;
    StopGrouping stopGroup = null;
    RestartGrouping restartGroup = null;

    group = groups.get(0);
    assertEquals(ClusterGrouping.class, group.getClass());
    clusterGroup = (ClusterGrouping) group;
    assertEquals("Prepare Upgrade", clusterGroup.title);
    assertNull(clusterGroup.parallelScheduler);

    group = groups.get(1);
    assertEquals(StopGrouping.class, group.getClass());
    stopGroup = (StopGrouping) group;
    assertEquals("Stop Daemons for High-Level Services", stopGroup.title);
    assertNotNull(stopGroup.parallelScheduler);
    assertEquals(ParallelScheduler.DEFAULT_MAX_DEGREE_OF_PARALLELISM, stopGroup.parallelScheduler.maxDegreeOfParallelism);

    group = groups.get(2);
    assertEquals(ClusterGrouping.class, group.getClass());
    clusterGroup = (ClusterGrouping) group;
    assertEquals("Take Backups", clusterGroup.title);
    assertNull(clusterGroup.parallelScheduler);

    group = groups.get(3);
    assertEquals(StopGrouping.class, group.getClass());
    stopGroup = (StopGrouping) group;
    assertEquals("Stop Daemons for Low-Level Services", stopGroup.title);
    assertNotNull(stopGroup.parallelScheduler);
    assertEquals(ParallelScheduler.DEFAULT_MAX_DEGREE_OF_PARALLELISM, stopGroup.parallelScheduler.maxDegreeOfParallelism);

    group = groups.get(4);
    assertEquals(UpdateStackGrouping.class, group.getClass());
    updateStackGroup = (UpdateStackGrouping) group;
    assertEquals("Update Desired Stack Id", updateStackGroup.title);
    assertNull(updateStackGroup.parallelScheduler);

    group = groups.get(5);
    assertEquals(ClusterGrouping.class, group.getClass());
    clusterGroup = (ClusterGrouping) group;
    assertEquals("Set Version On All Hosts", clusterGroup.title);
    assertNull(clusterGroup.parallelScheduler);

    group = groups.get(6);
    assertEquals(RestartGrouping.class, group.getClass());
    restartGroup = (RestartGrouping) group;
    assertEquals("Zookeeper", restartGroup.title);
    assertNull(restartGroup.parallelScheduler);

    group = groups.get(7);
    assertEquals(RestartGrouping.class, group.getClass());
    restartGroup = (RestartGrouping) group;
    assertEquals("HDFS", restartGroup.title);
    assertNotNull(restartGroup.parallelScheduler);
    assertEquals(2, restartGroup.parallelScheduler.maxDegreeOfParallelism);

    group = groups.get(8);
    assertEquals(RestartGrouping.class, group.getClass());
    restartGroup = (RestartGrouping) group;
    assertEquals("MR and YARN", restartGroup.title);
    assertNotNull(restartGroup.parallelScheduler);
    assertEquals(ParallelScheduler.DEFAULT_MAX_DEGREE_OF_PARALLELISM, restartGroup.parallelScheduler.maxDegreeOfParallelism);

    group = groups.get(9);
    assertEquals(ClusterGrouping.class, group.getClass());
    clusterGroup = (ClusterGrouping) group;
    assertEquals("Finalize {{direction.text.proper}}", clusterGroup.title);
    assertNull(clusterGroup.parallelScheduler);
  }


  /**
   * Tests that the service level XML merges correctly for 2.0.5/HDFS/HDP/2.2.0.
   *
   * @throws Exception
   */
  @Test
  public void testServiceLevelUpgradePackMerge() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.2.0");
    assertTrue(upgrades.containsKey("upgrade_test_15388"));

    UpgradePack upgradePack = upgrades.get("upgrade_test_15388");

    List<String> checks = upgradePack.getPrerequisiteChecks();
    assertEquals(11, checks.size());
    assertTrue(checks.contains("org.apache.ambari.server.checks.FooCheck"));

    List<Grouping> groups = upgradePack.getGroups(Direction.UPGRADE);
    assertEquals(8, groups.size());
    Grouping group = groups.get(0);
    assertEquals(ClusterGrouping.class, group.getClass());
    ClusterGrouping cluster_group = (ClusterGrouping) group;
    assertEquals("Pre {{direction.text.proper}}", cluster_group.title);

    List<ExecuteStage> stages = cluster_group.executionStages;
    assertEquals(5, stages.size());
    ExecuteStage stage = stages.get(3);
    assertEquals("Backup FOO", stage.title);

    group = groups.get(2);
    assertEquals("Core Masters", group.title);
    List<UpgradePack.OrderService> services = group.services;
    assertEquals(3, services.size());
    UpgradePack.OrderService service = services.get(2);
    assertEquals("HBASE", service.serviceName);

    group = groups.get(3);
    assertEquals("Core Slaves", group.title);
    services = group.services;
    assertEquals(3, services.size());
    service = services.get(1);
    assertEquals("HBASE", service.serviceName);

    group = groups.get(4);
    assertEquals(ServiceCheckGrouping.class, group.getClass());
    ServiceCheckGrouping scGroup = (ServiceCheckGrouping) group;
    Set<String> priorityServices = scGroup.getPriorities();
    assertEquals(4, priorityServices.size());
    Iterator serviceIterator = priorityServices.iterator();
    assertEquals("ZOOKEEPER", serviceIterator.next());
    assertEquals("HBASE", serviceIterator.next());

    group = groups.get(5);
    assertEquals("Hive", group.title);

    group = groups.get(6);
    assertEquals("Foo", group.title);
    services = group.services;
    assertEquals(2, services.size());
    service = services.get(1);
    assertEquals("FOO2", service.serviceName);

    Map<String, Map<String, ProcessingComponent>> tasks = upgradePack.getTasks();
    assertTrue(tasks.containsKey("HBASE"));

    // !!! generalized upgrade pack shouldn't be in this
    boolean found = false;
    for (Grouping grouping : upgradePack.getAllGroups()) {
      if (grouping.name.equals("GANGLIA_UPGRADE")) {
        found = true;
        break;
      }
    }
    assertFalse(found);

    // !!! test merge of a generalized upgrade pack
    upgradePack = upgrades.get("upgrade_test_conditions");
    assertNotNull(upgradePack);
    for (Grouping grouping : upgradePack.getAllGroups()) {
      if (grouping.name.equals("GANGLIA_UPGRADE")) {
        found = true;
        break;
      }
    }
    assertTrue(found);
  }


  @Test
  public void testPackWithHostGroup() {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.2.0");
    UpgradePack upgradePack = upgrades.get("upgrade_test_host_ordered");

    assertNotNull(upgradePack);
    assertEquals(upgradePack.getType(), UpgradeType.HOST_ORDERED);
    assertEquals(3, upgradePack.getAllGroups().size());

    assertEquals(HostOrderGrouping.class, upgradePack.getAllGroups().get(0).getClass());
    assertEquals(Grouping.class, upgradePack.getAllGroups().get(1).getClass());
  }


  private int indexOf(Map<String, ?> map, String keyToFind) {
    int result = -1;

    int i = 0;
    for (Entry<String, ?> entry : map.entrySet()) {
      if (entry.getKey().equals(keyToFind)) {
        return i;
      }
      i++;
    }

    return result;
  }
}
