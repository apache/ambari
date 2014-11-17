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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.state.stack.UpgradePack.ProcessingComponent;
import org.apache.ambari.server.state.stack.upgrade.Batch;
import org.apache.ambari.server.state.stack.upgrade.ConditionalBatch;
import org.apache.ambari.server.state.stack.upgrade.ConfigureTask;
import org.apache.ambari.server.state.stack.upgrade.CountBatch;
import org.apache.ambari.server.state.stack.upgrade.ExecuteTask;
import org.apache.ambari.server.state.stack.upgrade.ManualTask;
import org.apache.ambari.server.state.stack.upgrade.Task;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

/**
 * Tests for the upgrade pack
 */
public class UpgradePackTest {

  private Injector injector;
  private AmbariMetaInfo ambariMetaInfo;

  @Before
  public void before() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);

    ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    ambariMetaInfo.init();
  }

  @After
  public void teardown() {
    injector.getInstance(PersistService.class).stop();
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

    Map<String, List<String>> expectedOrder = new LinkedHashMap<String, List<String>>() {{
      put("ZOOKEEPER", Arrays.asList("ZOOKEEPER_SERVER", "ZOOKEEPER_CLIENT"));
      put("HDFS", Arrays.asList("JOURNALNODE", "NAMENODE", "DATANODE"));
    }};


    UpgradePack up = upgrades.values().iterator().next();
    assertEquals("2.2.*", up.getTarget());

    // !!! test the orders
    assertEquals(expectedOrder.size(), up.getOrder().size());

    int i = 0;
    for (Entry<String, List<String>> entry : expectedOrder.entrySet()) {
      assertTrue(up.getOrder().containsKey(entry.getKey()));
      assertEquals(i++, indexOf(up.getOrder(), entry.getKey()));

      int j = 0;
      for (String comp : entry.getValue()) {
        assertEquals(comp, up.getOrder().get(entry.getKey()).get(j++));
      }
    }

    Map<String, List<String>> expectedStages = new LinkedHashMap<String, List<String>>() {{
      put("ZOOKEEPER", Arrays.asList("ZOOKEEPER_SERVER"));
      put("HDFS", Arrays.asList("NAMENODE", "DATANODE"));
    }};

    // !!! test the tasks
    i = 0;
    for (Entry<String, List<String>> entry : expectedStages.entrySet()) {
      assertTrue(up.getTasks().containsKey(entry.getKey()));
      assertEquals(i++, indexOf(up.getTasks(), entry.getKey()));

      // check that the number of components matches
      assertEquals(entry.getValue().size(), up.getTasks().get(entry.getKey()).size());

      // check component ordering
      int j = 0;
      for (String comp : entry.getValue()) {
        assertEquals(j++, indexOf(up.getTasks().get(entry.getKey()), comp));
      }
    }

    // !!! test specific tasks
    assertTrue(up.getTasks().containsKey("HDFS"));
    assertTrue(up.getTasks().get("HDFS").containsKey("NAMENODE"));

    ProcessingComponent pc = up.getTasks().get("HDFS").get("NAMENODE");
    assertNull(pc.batch);
    assertNull(pc.preTasks);
    assertNull(pc.postTasks);
    assertNotNull(pc.tasks);
    assertEquals(3, pc.tasks.size());

    assertEquals(Task.Type.EXECUTE, pc.tasks.get(0).getType());
    assertEquals(ExecuteTask.class, pc.tasks.get(0).getClass());
    assertEquals("su - {hdfs-user} -c 'dosomething'",
        ExecuteTask.class.cast(pc.tasks.get(0)).command);

    assertEquals(Task.Type.CONFIGURE, pc.tasks.get(1).getType());
    assertEquals(ConfigureTask.class, pc.tasks.get(1).getClass());
    assertEquals("hdfs-site",
        ConfigureTask.class.cast(pc.tasks.get(1)).configType);
    assertEquals("myproperty",
        ConfigureTask.class.cast(pc.tasks.get(1)).key);
    assertEquals("mynewvalue",
        ConfigureTask.class.cast(pc.tasks.get(1)).value);

    assertEquals(Task.Type.MANUAL, pc.tasks.get(2).getType());
    assertEquals(ManualTask.class, pc.tasks.get(2).getClass());
    assertEquals("Update your database",
        ManualTask.class.cast(pc.tasks.get(2)).message);

    assertTrue(up.getTasks().containsKey("ZOOKEEPER"));
    assertTrue(up.getTasks().get("ZOOKEEPER").containsKey("ZOOKEEPER_SERVER"));

    pc = up.getTasks().get("HDFS").get("DATANODE");
    assertNotNull(pc.batch);
    assertEquals(Batch.Type.CONDITIONAL, pc.batch.getType());
    assertEquals(15, ConditionalBatch.class.cast(pc.batch).initial);
    assertEquals(50, ConditionalBatch.class.cast(pc.batch).remaining);

    pc = up.getTasks().get("ZOOKEEPER").get("ZOOKEEPER_SERVER");
    assertNotNull(pc.preTasks);
    assertEquals(1, pc.preTasks.size());
    assertNotNull(pc.postTasks);
    assertEquals(1, pc.postTasks.size());
    assertNotNull(pc.tasks);
    assertEquals(1, pc.tasks.size());
    assertNotNull(pc.batch);
    assertEquals(Batch.Type.COUNT, pc.batch.getType());
    assertEquals(2, CountBatch.class.cast(pc.batch).count);

  }


  private int indexOf(Map<String, ?> map, String keyToFind) {
    int result = -1;

    int i = 0;
    for (Entry<String, ?> entry : map.entrySet()) {
      if (entry.getKey().equals(keyToFind))
        return i;
      i++;
    }

    return result;
  }


}
