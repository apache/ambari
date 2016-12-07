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
package org.apache.ambari.server.serveraction.upgrades;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ServiceNotFoundException;
import org.apache.ambari.server.actionmanager.ExecutionCommandWrapper;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.ClusterVersionDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.serveraction.ServerAction;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.stack.upgrade.ConfigUpgradeChangeDefinition.ConfigurationKeyValue;
import org.apache.ambari.server.state.stack.upgrade.ConfigUpgradeChangeDefinition.Replace;
import org.apache.ambari.server.state.stack.upgrade.ConfigUpgradeChangeDefinition.Transfer;
import org.apache.ambari.server.state.stack.upgrade.ConfigureTask;
import org.apache.ambari.server.state.stack.upgrade.PropertyKeyState;
import org.apache.ambari.server.state.stack.upgrade.TransferCoercionType;
import org.apache.ambari.server.state.stack.upgrade.TransferOperation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

/**
 * Tests upgrade-related server side actions
 */
public class ConfigureActionTest {

  private static final String HDP_2_2_0_0 = "2.2.0.0-2041";
  private static final String HDP_2_2_0_1 = "2.2.0.1-2270";
  private static final StackId HDP_211_STACK = new StackId("HDP-2.1.1");
  private static final StackId HDP_220_STACK = new StackId("HDP-2.2.0");

  @Inject
  private Injector m_injector;
  @Inject
  private OrmTestHelper m_helper;
  @Inject
  private RepositoryVersionDAO repoVersionDAO;
  @Inject
  private HostVersionDAO hostVersionDAO;
  @Inject
  private HostRoleCommandFactory hostRoleCommandFactory;
  @Inject
  private ServiceFactory serviceFactory;
  @Inject
  private ConfigHelper m_configHelper;
  @Inject
  private Clusters clusters;
  @Inject
  private ClusterVersionDAO clusterVersionDAO;
  @Inject
  private ConfigFactory cf;
  @Inject
  private ConfigureAction action;
  @Inject
  private HostDAO hostDAO;
  @Inject
  private StackDAO stackDAO;

  @Before
  public void setup() throws Exception {
    m_injector = Guice.createInjector(new InMemoryDefaultTestModule());
    m_injector.getInstance(GuiceJpaInitializer.class);
    m_injector.injectMembers(this);
  }

  @After
  public void teardown() throws Exception {
    m_injector.getInstance(PersistService.class).stop();
  }


  @Test
  public void testConfigActionUpgradeAcrossStack() throws Exception {
    makeUpgradeCluster();

    Cluster c = clusters.getCluster("c1");
    assertEquals(1, c.getConfigsByType("zoo.cfg").size());

    c.setCurrentStackVersion(HDP_211_STACK);
    c.setDesiredStackVersion(HDP_220_STACK);
    Config config = cf.createNew(c, "zoo.cfg", "version2", new HashMap<String, String>() {{
          put("initLimit", "10");
        }}, new HashMap<String, Map<String,String>>());

    c.addDesiredConfig("user", Collections.singleton(config));
    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    List<ConfigurationKeyValue> configurations = new ArrayList<ConfigurationKeyValue>();
    ConfigurationKeyValue keyValue = new ConfigurationKeyValue();
    configurations.add(keyValue);
    keyValue.key = "initLimit";
    keyValue.value = "11";

    Map<String, String> commandParams = new HashMap<String, String>();
    commandParams.put("upgrade_direction", "upgrade");
    commandParams.put("version", HDP_2_2_0_1);
    commandParams.put("clusterName", "c1");
    commandParams.put(ConfigureTask.PARAMETER_CONFIG_TYPE, "zoo.cfg");
    commandParams.put(ConfigureTask.PARAMETER_KEY_VALUE_PAIRS, new Gson().toJson(configurations));

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName("c1");

    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null,
        null, null);

    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(
        executionCommand));


    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    config = c.getDesiredConfigByType("zoo.cfg");
    assertNotNull(config);
    assertEquals("version2", config.getTag());
    assertEquals("11", config.getProperties().get("initLimit"));
  }

  /**
   * Tests that DELETE "*" with edit preserving works correctly.
   *
   * @throws Exception
   */
  @Test
  public void testDeletePreserveChanges() throws Exception {
    makeUpgradeCluster();

    Cluster c = clusters.getCluster("c1");
    assertEquals(1, c.getConfigsByType("zoo.cfg").size());

    c.setDesiredStackVersion(HDP_220_STACK);

    // create a config for zoo.cfg with two values; one is a stack value and the
    // other is custom
    Config config = cf.createNew(c, "zoo.cfg", "version2", new HashMap<String, String>() {
      {
        put("tickTime", "2000");
        put("foo", "bar");
      }
    }, new HashMap<String, Map<String, String>>());

    c.addDesiredConfig("user", Collections.singleton(config));
    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    Map<String, String> commandParams = new HashMap<String, String>();
    commandParams.put("upgrade_direction", "upgrade");
    commandParams.put("version", HDP_2_2_0_1);
    commandParams.put("clusterName", "c1");
    commandParams.put(ConfigureTask.PARAMETER_CONFIG_TYPE, "zoo.cfg");

    // delete all keys, preserving edits or additions
    List<Transfer> transfers = new ArrayList<>();
    Transfer transfer = new Transfer();
    transfer.operation = TransferOperation.DELETE;
    transfer.deleteKey = "*";
    transfer.preserveEdits = true;
    transfers.add(transfer);

    commandParams.put(ConfigureTask.PARAMETER_TRANSFERS, new Gson().toJson(transfers));

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName("c1");
    executionCommand.setRoleParams(new HashMap<String, String>());
    executionCommand.getRoleParams().put(ServerAction.ACTION_USER_NAME, "username");

    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null, null, null);
    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(executionCommand));


    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    // make sure there are now 3 versions after the merge
    assertEquals(3, c.getConfigsByType("zoo.cfg").size());
    config = c.getDesiredConfigByType("zoo.cfg");
    assertNotNull(config);
    assertFalse("version2".equals(config.getTag()));

    // time to check our values; there should only be 1 left since tickTime was
    // removed
    Map<String, String> map = config.getProperties();
    assertEquals("bar", map.get("foo"));
    assertFalse(map.containsKey("tickTime"));
  }

  @Test
  public void testConfigTransferCopy() throws Exception {
    makeUpgradeCluster();

    Cluster c = clusters.getCluster("c1");
    assertEquals(1, c.getConfigsByType("zoo.cfg").size());

    c.setDesiredStackVersion(HDP_220_STACK);
    Config config = cf.createNew(c, "zoo.cfg", "version2", new HashMap<String, String>() {{
          put("initLimit", "10");
          put("copyIt", "10");
          put("moveIt", "10");
          put("deleteIt", "10");
        }}, new HashMap<String, Map<String,String>>());

    c.addDesiredConfig("user", Collections.singleton(config));
    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    List<ConfigurationKeyValue> configurations = new ArrayList<>();
    ConfigurationKeyValue keyValue = new ConfigurationKeyValue();
    configurations.add(keyValue);
    keyValue.key = "initLimit";
    keyValue.value = "11";

    Map<String, String> commandParams = new HashMap<String, String>();
    commandParams.put("upgrade_direction", "upgrade");
    commandParams.put("version", HDP_2_2_0_1);
    commandParams.put("clusterName", "c1");
    commandParams.put(ConfigureTask.PARAMETER_CONFIG_TYPE, "zoo.cfg");
    commandParams.put(ConfigureTask.PARAMETER_KEY_VALUE_PAIRS, new Gson().toJson(configurations));

    // normal copy
    List<Transfer> transfers = new ArrayList<>();
    Transfer transfer = new Transfer();
    transfer.operation = TransferOperation.COPY;
    transfer.fromKey = "copyIt";
    transfer.toKey = "copyKey";
    transfers.add(transfer);

    // copy with default
    transfer = new Transfer();
    transfer.operation = TransferOperation.COPY;
    transfer.fromKey = "copiedFromMissingKeyWithDefault";
    transfer.toKey = "copiedToMissingKeyWithDefault";
    transfer.defaultValue = "defaultValue";
    transfers.add(transfer);

    // normal move
    transfer = new Transfer();
    transfer.operation = TransferOperation.MOVE;
    transfer.fromKey = "moveIt";
    transfer.toKey = "movedKey";
    transfers.add(transfer);

    // move with default
    transfer = new Transfer();
    transfer.operation = TransferOperation.MOVE;
    transfer.fromKey = "movedFromKeyMissingWithDefault";
    transfer.toKey = "movedToMissingWithDefault";
    transfer.defaultValue = "defaultValue2";
    transfer.mask = true;
    transfers.add(transfer);

    transfer = new Transfer();
    transfer.operation = TransferOperation.DELETE;
    transfer.deleteKey = "deleteIt";
    transfers.add(transfer);

    commandParams.put(ConfigureTask.PARAMETER_TRANSFERS, new Gson().toJson(transfers));

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName("c1");
    executionCommand.setRoleParams(new HashMap<String, String>());
    executionCommand.getRoleParams().put(ServerAction.ACTION_USER_NAME, "username");

    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null,
        null, null);

    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(
        executionCommand));


    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    assertEquals(3, c.getConfigsByType("zoo.cfg").size());

    config = c.getDesiredConfigByType("zoo.cfg");
    assertNotNull(config);
    assertFalse("version2".equals(config.getTag()));

    Map<String, String> map = config.getProperties();
    assertEquals("11", map.get("initLimit"));
    assertEquals("10", map.get("copyIt"));
    assertTrue(map.containsKey("copyKey"));
    assertEquals(map.get("copyIt"), map.get("copyKey"));
    assertFalse(map.containsKey("moveIt"));
    assertTrue(map.containsKey("movedKey"));
    assertFalse(map.containsKey("deletedKey"));
    assertTrue(map.containsKey("copiedToMissingKeyWithDefault"));
    assertEquals("defaultValue", map.get("copiedToMissingKeyWithDefault"));
    assertTrue(map.containsKey("movedToMissingWithDefault"));
    assertEquals("defaultValue2", map.get("movedToMissingWithDefault"));

    transfers.clear();
    transfer = new Transfer();
    transfer.operation = TransferOperation.DELETE;
    transfer.deleteKey = "*";
    transfer.preserveEdits = true;
    transfer.keepKeys.add("copyKey");
    // The below key should be ignored/not added as it doesn't exist originally as part of transfer.
    transfer.keepKeys.add("keyNotExisting");
    // The 'null' passed as part of key should be ignored as part of transfer operation.
    transfer.keepKeys.add(null);


    transfers.add(transfer);
    commandParams.put(ConfigureTask.PARAMETER_TRANSFERS, new Gson().toJson(transfers));

    report = action.execute(null);
    assertNotNull(report);

    assertEquals(4, c.getConfigsByType("zoo.cfg").size());
    config = c.getDesiredConfigByType("zoo.cfg");
    map = config.getProperties();
    assertEquals(6, map.size());
    assertTrue(map.containsKey("initLimit")); // it just changed to 11 from 10
    assertTrue(map.containsKey("copyKey")); // is new
    // Below two keys should not have been added in the map.
    assertFalse(map.containsKey("keyNotExisting"));
    assertFalse(map.containsKey(null));
  }

  @Test
  public void testCoerceValueOnCopy() throws Exception {
    makeUpgradeCluster();

    Cluster c = clusters.getCluster("c1");
    assertEquals(1, c.getConfigsByType("zoo.cfg").size());

    c.setDesiredStackVersion(HDP_220_STACK);
    Config config = cf.createNew(c, "zoo.cfg", "version2", new HashMap<String, String>() {
      {
        put("zoo.server.csv", "c6401,c6402,  c6403");
      }
    }, new HashMap<String, Map<String, String>>());

    c.addDesiredConfig("user", Collections.singleton(config));
    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    Map<String, String> commandParams = new HashMap<String, String>();
    commandParams.put("upgrade_direction", "upgrade");
    commandParams.put("version", HDP_2_2_0_1);
    commandParams.put("clusterName", "c1");
    commandParams.put(ConfigureTask.PARAMETER_CONFIG_TYPE, "zoo.cfg");

    // copy with coerce
    List<Transfer> transfers = new ArrayList<Transfer>();
    Transfer transfer = new Transfer();
    transfer.operation = TransferOperation.COPY;
    transfer.coerceTo = TransferCoercionType.YAML_ARRAY;
    transfer.fromKey = "zoo.server.csv";
    transfer.toKey = "zoo.server.array";
    transfer.defaultValue = "['foo','bar']";
    transfers.add(transfer);

    commandParams.put(ConfigureTask.PARAMETER_TRANSFERS, new Gson().toJson(transfers));

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName("c1");
    executionCommand.setRoleParams(new HashMap<String, String>());
    executionCommand.getRoleParams().put(ServerAction.ACTION_USER_NAME, "username");

    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null, null, null);

    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(executionCommand));


    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    assertEquals(3, c.getConfigsByType("zoo.cfg").size());

    config = c.getDesiredConfigByType("zoo.cfg");
    assertNotNull(config);
    assertFalse("version2".equals(config.getTag()));

    Map<String, String> map = config.getProperties();
    assertEquals("c6401,c6402,  c6403", map.get("zoo.server.csv"));
    assertEquals("['c6401','c6402','c6403']", map.get("zoo.server.array"));
  }

  @Test
  public void testValueReplacement() throws Exception {
    makeUpgradeCluster();

    Cluster c = clusters.getCluster("c1");
    assertEquals(1, c.getConfigsByType("zoo.cfg").size());

    c.setDesiredStackVersion(HDP_220_STACK);
    Config config = cf.createNew(c, "zoo.cfg", "version2", new HashMap<String, String>() {
      {
        put("key_to_replace", "My New Cat");
        put("key_with_no_match", "WxyAndZ");
      }
    }, new HashMap<String, Map<String, String>>());

    c.addDesiredConfig("user", Collections.singleton(config));
    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    Map<String, String> commandParams = new HashMap<String, String>();
    commandParams.put("upgrade_direction", "upgrade");
    commandParams.put("version", HDP_2_2_0_1);
    commandParams.put("clusterName", "c1");
    commandParams.put(ConfigureTask.PARAMETER_CONFIG_TYPE, "zoo.cfg");

    // Replacement task
    List<Replace> replacements = new ArrayList<Replace>();
    Replace replace = new Replace();
    replace.key = "key_to_replace";
    replace.find = "New Cat";
    replace.replaceWith = "Wet Dog";
    replacements.add(replace);

    replace = new Replace();
    replace.key = "key_with_no_match";
    replace.find = "abc";
    replace.replaceWith = "def";
    replacements.add(replace);

    commandParams.put(ConfigureTask.PARAMETER_REPLACEMENTS, new Gson().toJson(replacements));

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName("c1");
    executionCommand.setRoleParams(new HashMap<String, String>());
    executionCommand.getRoleParams().put(ServerAction.ACTION_USER_NAME, "username");

    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null, null, null);

    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(executionCommand));


    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    assertEquals(3, c.getConfigsByType("zoo.cfg").size());

    config = c.getDesiredConfigByType("zoo.cfg");
    assertNotNull(config);
    assertFalse("version2".equals(config.getTag()));

    assertEquals("My Wet Dog", config.getProperties().get("key_to_replace"));
    assertEquals("WxyAndZ", config.getProperties().get("key_with_no_match"));
  }

  /**
   * Tests that replacing a {@code null} value works.
   *
   * @throws Exception
   */
  @Test
  public void testValueReplacementWithMissingConfigurations() throws Exception {
    makeUpgradeCluster();

    Cluster c = clusters.getCluster("c1");
    assertEquals(1, c.getConfigsByType("zoo.cfg").size());

    c.setDesiredStackVersion(HDP_220_STACK);
    Config config = cf.createNew(c, "zoo.cfg", "version2", new HashMap<String, String>() {
      {
        put("existing", "This exists!");
        put("missing", null);
      }
    }, new HashMap<String, Map<String, String>>());

    c.addDesiredConfig("user", Collections.singleton(config));
    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    Map<String, String> commandParams = new HashMap<String, String>();
    commandParams.put("upgrade_direction", "upgrade");
    commandParams.put("version", HDP_2_2_0_1);
    commandParams.put("clusterName", "c1");
    commandParams.put(ConfigureTask.PARAMETER_CONFIG_TYPE, "zoo.cfg");

    // Replacement task
    List<Replace> replacements = new ArrayList<Replace>();
    Replace replace = new Replace();
    replace.key = "missing";
    replace.find = "foo";
    replace.replaceWith = "bar";
    replacements.add(replace);

    commandParams.put(ConfigureTask.PARAMETER_REPLACEMENTS, new Gson().toJson(replacements));

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName("c1");
    executionCommand.setRoleParams(new HashMap<String, String>());
    executionCommand.getRoleParams().put(ServerAction.ACTION_USER_NAME, "username");

    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null, null, null);

    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(executionCommand));


    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    assertEquals(3, c.getConfigsByType("zoo.cfg").size());

    config = c.getDesiredConfigByType("zoo.cfg");
    assertEquals(null, config.getProperties().get("missing"));
  }

  @Test
  public void testMultipleKeyValuesPerTask() throws Exception {
    makeUpgradeCluster();

    Cluster c = clusters.getCluster("c1");
    assertEquals(1, c.getConfigsByType("zoo.cfg").size());

    c.setCurrentStackVersion(HDP_211_STACK);
    c.setDesiredStackVersion(HDP_220_STACK);
    Config config = cf.createNew(c, "zoo.cfg", "version2", new HashMap<String, String>() {
      {
        put("fooKey", "barValue");
      }
    }, new HashMap<String, Map<String, String>>());

    c.addDesiredConfig("user", Collections.singleton(config));
    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    // create several configurations
    List<ConfigurationKeyValue> configurations = new ArrayList<ConfigurationKeyValue>();
    ConfigurationKeyValue fooKey2 = new ConfigurationKeyValue();
    configurations.add(fooKey2);
    fooKey2.key = "fooKey2";
    fooKey2.value = "barValue2";

    ConfigurationKeyValue fooKey3 = new ConfigurationKeyValue();
    configurations.add(fooKey3);
    fooKey3.key = "fooKey3";
    fooKey3.value = "barValue3";
    fooKey3.mask = true;

    Map<String, String> commandParams = new HashMap<String, String>();
    commandParams.put("upgrade_direction", "upgrade");
    commandParams.put("version", HDP_2_2_0_1);
    commandParams.put("clusterName", "c1");
    commandParams.put(ConfigureTask.PARAMETER_CONFIG_TYPE, "zoo.cfg");
    commandParams.put(ConfigureTask.PARAMETER_KEY_VALUE_PAIRS, new Gson().toJson(configurations));

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName("c1");

    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null, null, null);
    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(executionCommand));


    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    config = c.getDesiredConfigByType("zoo.cfg");
    assertNotNull(config);
    assertEquals("barValue", config.getProperties().get("fooKey"));
    assertEquals("barValue2", config.getProperties().get("fooKey2"));
    assertEquals("barValue3", config.getProperties().get("fooKey3"));
    assertTrue(report.getStdOut().contains("******"));

  }

  @Test
  public void testAllowedSet() throws Exception {
    makeUpgradeCluster();

    Cluster c = clusters.getCluster("c1");
    assertEquals(1, c.getConfigsByType("zoo.cfg").size());

    c.setCurrentStackVersion(HDP_211_STACK);
    c.setDesiredStackVersion(HDP_220_STACK);
    Config config = cf.createNew(c, "zoo.cfg", "version2", new HashMap<String, String>() {
      {
        put("set.key.1", "s1");
        put("set.key.2", "s2");
        put("set.key.3", "s3");
        put("set.key.4", "s4");
      }
    }, new HashMap<String, Map<String, String>>());

    c.addDesiredConfig("user", Collections.singleton(config));
    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    // create several configurations
    List<ConfigurationKeyValue> configurations = new ArrayList<ConfigurationKeyValue>();
    ConfigurationKeyValue fooKey1 = new ConfigurationKeyValue();
    configurations.add(fooKey1);
    fooKey1.key = "fooKey1";
    fooKey1.value = "barValue1";

    ConfigurationKeyValue fooKey2 = new ConfigurationKeyValue();
    configurations.add(fooKey2);
    fooKey2.key = "fooKey2";
    fooKey2.value = "barValue2";

    ConfigurationKeyValue fooKey3 = new ConfigurationKeyValue();
    configurations.add(fooKey3);
    fooKey3.key = "fooKey3";
    fooKey3.value = "barValue3";
    fooKey3.ifKey = "set.key.1";
    fooKey3.ifType = "zoo.cfg";
    fooKey3.ifValue = "s1";

    ConfigurationKeyValue fooKey4 = new ConfigurationKeyValue();
    configurations.add(fooKey4);
    fooKey4.key = "fooKey4";
    fooKey4.value = "barValue4";
    fooKey4.ifKey = "set.key.2";
    fooKey4.ifType = "zoo.cfg";
    fooKey4.ifKeyState= PropertyKeyState.PRESENT;

    ConfigurationKeyValue fooKey5 = new ConfigurationKeyValue();
    configurations.add(fooKey5);
    fooKey5.key = "fooKey5";
    fooKey5.value = "barValue5";
    fooKey5.ifKey = "abc";
    fooKey5.ifType = "zoo.cfg";
    fooKey5.ifKeyState= PropertyKeyState.ABSENT;


    Map<String, String> commandParams = new HashMap<String, String>();
    commandParams.put("upgrade_direction", "upgrade");
    commandParams.put("version", HDP_2_2_0_1);
    commandParams.put("clusterName", "c1");
    commandParams.put(ConfigureTask.PARAMETER_CONFIG_TYPE, "zoo.cfg");
    commandParams.put(ConfigureTask.PARAMETER_KEY_VALUE_PAIRS, new Gson().toJson(configurations));

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName("c1");

    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null, null, null);
    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(executionCommand));


    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    config = c.getDesiredConfigByType("zoo.cfg");
    assertNotNull(config);
    assertEquals("barValue1", config.getProperties().get("fooKey1"));
    assertEquals("barValue2", config.getProperties().get("fooKey2"));
    assertEquals("barValue3", config.getProperties().get("fooKey3"));
    assertEquals("barValue4", config.getProperties().get("fooKey4"));
    assertEquals("barValue5", config.getProperties().get("fooKey5"));
    assertEquals("s1", config.getProperties().get("set.key.1"));
    assertEquals("s2", config.getProperties().get("set.key.2"));
    assertEquals("s3", config.getProperties().get("set.key.3"));
    assertEquals("s4", config.getProperties().get("set.key.4"));
  }

  @Test
  public void testDisallowedSet() throws Exception {
    makeUpgradeCluster();

    Cluster c = clusters.getCluster("c1");
    assertEquals(1, c.getConfigsByType("zoo.cfg").size());

    c.setCurrentStackVersion(HDP_211_STACK);
    c.setDesiredStackVersion(HDP_220_STACK);
    Config config = cf.createNew(c, "zoo.cfg", "version2", new HashMap<String, String>() {
      {
        put("set.key.1", "s1");
        put("set.key.2", "s2");
        put("set.key.3", "s3");
        put("set.key.4", "s4");
      }
    }, new HashMap<String, Map<String, String>>());

    c.addDesiredConfig("user", Collections.singleton(config));
    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    // create several configurations
    List<ConfigurationKeyValue> configurations = new ArrayList<ConfigurationKeyValue>();
    ConfigurationKeyValue fooKey3 = new ConfigurationKeyValue();
    configurations.add(fooKey3);
    fooKey3.key = "fooKey3";
    fooKey3.value = "barValue3";
    fooKey3.ifKey = "set.key.1";
    fooKey3.ifType = "zoo.cfg";
    fooKey3.ifValue = "no-such-value";

    ConfigurationKeyValue fooKey4 = new ConfigurationKeyValue();
    configurations.add(fooKey4);
    fooKey4.key = "fooKey4";
    fooKey4.value = "barValue4";
    fooKey4.ifKey = "set.key.2";
    fooKey4.ifType = "zoo.cfg";
    fooKey4.ifKeyState= PropertyKeyState.ABSENT;

    ConfigurationKeyValue fooKey5 = new ConfigurationKeyValue();
    configurations.add(fooKey5);
    fooKey5.key = "fooKey5";
    fooKey5.value = "barValue5";
    fooKey5.ifKey = "abc";
    fooKey5.ifType = "zoo.cfg";
    fooKey5.ifKeyState= PropertyKeyState.PRESENT;


    Map<String, String> commandParams = new HashMap<String, String>();
    commandParams.put("upgrade_direction", "upgrade");
    commandParams.put("version", HDP_2_2_0_1);
    commandParams.put("clusterName", "c1");
    commandParams.put(ConfigureTask.PARAMETER_CONFIG_TYPE, "zoo.cfg");
    commandParams.put(ConfigureTask.PARAMETER_KEY_VALUE_PAIRS, new Gson().toJson(configurations));

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName("c1");

    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null, null, null);
    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(executionCommand));


    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    config = c.getDesiredConfigByType("zoo.cfg");
    assertNotNull(config);
    assertEquals("s1", config.getProperties().get("set.key.1"));
    assertEquals("s2", config.getProperties().get("set.key.2"));
    assertEquals("s3", config.getProperties().get("set.key.3"));
    assertEquals("s4", config.getProperties().get("set.key.4"));
    assertFalse(config.getProperties().containsKey("fooKey3"));
    assertFalse(config.getProperties().containsKey("fooKey4"));
    assertFalse(config.getProperties().containsKey("fooKey5"));
  }

  @Test
  public void testAllowedReplacment() throws Exception {
    makeUpgradeCluster();

    Cluster c = clusters.getCluster("c1");
    assertEquals(1, c.getConfigsByType("zoo.cfg").size());

    c.setCurrentStackVersion(HDP_211_STACK);
    c.setDesiredStackVersion(HDP_220_STACK);
    Config config = cf.createNew(c, "zoo.cfg", "version2", new HashMap<String, String>() {
      {
        put("replace.key.1", "r1");
        put("replace.key.2", "r2");
        put("replace.key.3", "r3a1");
        put("replace.key.4", "r4");
        put("replace.key.5", "r5");
      }
    }, new HashMap<String, Map<String, String>>());

    c.addDesiredConfig("user", Collections.singleton(config));
    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    // create several configurations
    List<Replace> replacements = new ArrayList<Replace>();
    Replace replace = new Replace();
    replace.key = "replace.key.3";
    replace.find = "a";
    replace.replaceWith = "A";
    replacements.add(replace);

    Replace replace2 = new Replace();
    replacements.add(replace2);
    replace2.key = "replace.key.4";
    replace2.find = "r";
    replace2.replaceWith = "R";
    replace2.ifKey = "replace.key.1";
    replace2.ifType = "zoo.cfg";
    replace2.ifValue = "r1";
    replacements.add(replace2);

    Replace replace3 = new Replace();
    replacements.add(replace3);
    replace3.key = "replace.key.2";
    replace3.find = "r";
    replace3.replaceWith = "R";
    replace3.ifKey = "replace.key.1";
    replace3.ifType = "zoo.cfg";
    replace3.ifKeyState = PropertyKeyState.PRESENT;
    replacements.add(replace3);

    Replace replace4 = new Replace();
    replacements.add(replace3);
    replace4.key = "replace.key.5";
    replace4.find = "r";
    replace4.replaceWith = "R";
    replace4.ifKey = "no.such.key";
    replace4.ifType = "zoo.cfg";
    replace4.ifKeyState = PropertyKeyState.ABSENT;
    replacements.add(replace4);

    Map<String, String> commandParams = new HashMap<String, String>();
    commandParams.put("upgrade_direction", "upgrade");
    commandParams.put("version", HDP_2_2_0_1);
    commandParams.put("clusterName", "c1");
    commandParams.put(ConfigureTask.PARAMETER_CONFIG_TYPE, "zoo.cfg");
    commandParams.put(ConfigureTask.PARAMETER_REPLACEMENTS, new Gson().toJson(replacements));

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName("c1");

    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null, null, null);
    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(executionCommand));


    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    config = c.getDesiredConfigByType("zoo.cfg");
    assertNotNull(config);
    assertEquals("r1", config.getProperties().get("replace.key.1"));
    assertEquals("R2", config.getProperties().get("replace.key.2"));
    assertEquals("r3A1", config.getProperties().get("replace.key.3"));
    assertEquals("R4", config.getProperties().get("replace.key.4"));
    assertEquals("R5", config.getProperties().get("replace.key.5"));
  }

  @Test
  public void testDisallowedReplacment() throws Exception {
    makeUpgradeCluster();

    Cluster c = clusters.getCluster("c1");
    assertEquals(1, c.getConfigsByType("zoo.cfg").size());

    c.setCurrentStackVersion(HDP_211_STACK);
    c.setDesiredStackVersion(HDP_220_STACK);
    Config config = cf.createNew(c, "zoo.cfg", "version2", new HashMap<String, String>() {
      {
        put("replace.key.1", "r1");
        put("replace.key.2", "r2");
        put("replace.key.3", "r3a1");
        put("replace.key.4", "r4");
        put("replace.key.5", "r5");
      }
    }, new HashMap<String, Map<String, String>>());

    c.addDesiredConfig("user", Collections.singleton(config));
    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    // create several configurations
    List<Replace> replacements = new ArrayList<Replace>();

    Replace replace2 = new Replace();
    replacements.add(replace2);
    replace2.key = "replace.key.4";
    replace2.find = "r";
    replace2.replaceWith = "R";
    replace2.ifKey = "replace.key.1";
    replace2.ifType = "zoo.cfg";
    replace2.ifValue = "not-this-value";
    replacements.add(replace2);

    Replace replace3 = new Replace();
    replacements.add(replace3);
    replace3.key = "replace.key.2";
    replace3.find = "r";
    replace3.replaceWith = "R";
    replace3.ifKey = "replace.key.1";
    replace3.ifType = "zoo.cfg";
    replace3.ifKeyState = PropertyKeyState.ABSENT;
    replacements.add(replace3);

    Replace replace4 = new Replace();
    replacements.add(replace3);
    replace4.key = "replace.key.5";
    replace4.find = "r";
    replace4.replaceWith = "R";
    replace4.ifKey = "no.such.key";
    replace4.ifType = "zoo.cfg";
    replace4.ifKeyState = PropertyKeyState.PRESENT;
    replacements.add(replace4);

    Map<String, String> commandParams = new HashMap<String, String>();
    commandParams.put("upgrade_direction", "upgrade");
    commandParams.put("version", HDP_2_2_0_1);
    commandParams.put("clusterName", "c1");
    commandParams.put(ConfigureTask.PARAMETER_CONFIG_TYPE, "zoo.cfg");
    commandParams.put(ConfigureTask.PARAMETER_REPLACEMENTS, new Gson().toJson(replacements));

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName("c1");

    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null, null, null);
    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(executionCommand));


    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    config = c.getDesiredConfigByType("zoo.cfg");
    assertNotNull(config);
    assertEquals("r1", config.getProperties().get("replace.key.1"));
    assertEquals("r2", config.getProperties().get("replace.key.2"));
    assertEquals("r3a1", config.getProperties().get("replace.key.3"));
    assertEquals("r4", config.getProperties().get("replace.key.4"));
    assertEquals("r5", config.getProperties().get("replace.key.5"));
  }

  @Test
  public void testAllowedTransferCopy() throws Exception {
    makeUpgradeCluster();

    Cluster c = clusters.getCluster("c1");
    assertEquals(1, c.getConfigsByType("zoo.cfg").size());

    c.setDesiredStackVersion(HDP_220_STACK);
    Config config = cf.createNew(c, "zoo.cfg", "version2", new HashMap<String, String>() {{
          put("initLimit", "10");
          put("copy.key.1", "c1");
          put("copy.key.2", "c2");
        }}, new HashMap<String, Map<String,String>>());

    c.addDesiredConfig("user", Collections.singleton(config));
    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    List<ConfigurationKeyValue> configurations = new ArrayList<>();
    ConfigurationKeyValue keyValue = new ConfigurationKeyValue();
    configurations.add(keyValue);
    keyValue.key = "initLimit";
    keyValue.value = "11";

    Map<String, String> commandParams = new HashMap<String, String>();
    commandParams.put("upgrade_direction", "upgrade");
    commandParams.put("version", HDP_2_2_0_1);
    commandParams.put("clusterName", "c1");
    commandParams.put(ConfigureTask.PARAMETER_CONFIG_TYPE, "zoo.cfg");
    commandParams.put(ConfigureTask.PARAMETER_KEY_VALUE_PAIRS, new Gson().toJson(configurations));

    // normal copy
    List<Transfer> transfers = new ArrayList<>();
    Transfer transfer1 = new Transfer();
    transfer1.operation = TransferOperation.COPY;
    transfer1.fromKey = "copy.key.1";
    transfer1.toKey = "copy.to.key.1";
    transfers.add(transfer1);

    // copy with default
    Transfer transfer2 = new Transfer();
    transfer2.operation = TransferOperation.COPY;
    transfer2.fromKey = "copy.key.no.need.to.exit.1";
    transfer2.toKey = "copy.to.key.with.default.1";
    transfer2.defaultValue = "defaultValue";
    transfers.add(transfer2);

    Transfer transfer3 = new Transfer();
    transfer3.operation = TransferOperation.COPY;
    transfer3.fromKey = "copy.key.2";
    transfer3.toKey = "copy.to.key.2";
    transfer3.ifKey = "initLimit";
    transfer3.ifType = "zoo.cfg";
    transfer3.ifValue = "10";
    transfers.add(transfer3);

    Transfer transfer4 = new Transfer();
    transfer4.operation = TransferOperation.COPY;
    transfer4.fromKey = "copy.key.2";
    transfer4.toKey = "copy.to.key.3";
    transfer4.ifKey = "initLimit";
    transfer4.ifType = "zoo.cfg";
    transfer4.ifKeyState = PropertyKeyState.PRESENT;
    transfers.add(transfer4);

    Transfer transfer5 = new Transfer();
    transfer5.operation = TransferOperation.COPY;
    transfer5.fromKey = "copy.key.no.need.to.exist.2";
    transfer5.toKey = "copy.to.key.with.default.2";
    transfer5.defaultValue = "defaultValue2";
    transfer5.ifKey = "no.such.key";
    transfer5.ifType = "zoo.cfg";
    transfer5.ifKeyState = PropertyKeyState.ABSENT;
    transfers.add(transfer5);

    commandParams.put(ConfigureTask.PARAMETER_TRANSFERS, new Gson().toJson(transfers));

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName("c1");
    executionCommand.setRoleParams(new HashMap<String, String>());
    executionCommand.getRoleParams().put(ServerAction.ACTION_USER_NAME, "username");

    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null,
        null, null);

    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(
        executionCommand));


    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    assertEquals(3, c.getConfigsByType("zoo.cfg").size());

    config = c.getDesiredConfigByType("zoo.cfg");
    assertNotNull(config);
    assertFalse("version2".equals(config.getTag()));

    Map<String, String> map = config.getProperties();
    assertEquals(8, map.size());
    assertEquals("11", map.get("initLimit"));
    assertEquals(map.get("copy.key.1"), map.get("copy.to.key.1"));
    assertTrue(!map.containsKey("copy.key.no.need.to.exit.1"));
    assertEquals("defaultValue", map.get("copy.to.key.with.default.1"));
    assertTrue(!map.containsKey("copy.key.no.need.to.exit.2"));
    assertEquals("defaultValue2", map.get("copy.to.key.with.default.2"));
    assertEquals(map.get("copy.key.2"), map.get("copy.to.key.2"));
    assertEquals(map.get("copy.key.2"), map.get("copy.to.key.3"));
  }

  @Test
  public void testDisallowedTransferCopy() throws Exception {
    makeUpgradeCluster();

    Cluster c = clusters.getCluster("c1");
    assertEquals(1, c.getConfigsByType("zoo.cfg").size());

    c.setDesiredStackVersion(HDP_220_STACK);
    Config config = cf.createNew(c, "zoo.cfg", "version2", new HashMap<String, String>() {{
          put("initLimit", "10");
          put("copy.key.1", "c1");
          put("copy.key.2", "c2");
        }}, new HashMap<String, Map<String,String>>());

    c.addDesiredConfig("user", Collections.singleton(config));
    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    List<ConfigurationKeyValue> configurations = new ArrayList<>();
    ConfigurationKeyValue keyValue = new ConfigurationKeyValue();
    configurations.add(keyValue);
    keyValue.key = "initLimit";
    keyValue.value = "11";

    Map<String, String> commandParams = new HashMap<String, String>();
    commandParams.put("upgrade_direction", "upgrade");
    commandParams.put("version", HDP_2_2_0_1);
    commandParams.put("clusterName", "c1");
    commandParams.put(ConfigureTask.PARAMETER_CONFIG_TYPE, "zoo.cfg");
    commandParams.put(ConfigureTask.PARAMETER_KEY_VALUE_PAIRS, new Gson().toJson(configurations));

    List<Transfer> transfers = new ArrayList<>();
    Transfer transfer = new Transfer();
    transfer.operation = TransferOperation.COPY;
    transfer.fromKey = "copy.key.2";
    transfer.toKey = "copy.to.key.2";
    transfer.ifKey = "initLimit";
    transfer.ifType = "zoo.cfg";
    transfer.ifValue = "not-the-real-value";
    transfers.add(transfer);

    transfer = new Transfer();
    transfer.operation = TransferOperation.COPY;
    transfer.fromKey = "copy.key.2";
    transfer.toKey = "copy.to.key.3";
    transfer.ifKey = "initLimit";
    transfer.ifType = "zoo.cfg";
    transfer.ifKeyState = PropertyKeyState.ABSENT;
    transfers.add(transfer);

    transfer = new Transfer();
    transfer.operation = TransferOperation.COPY;
    transfer.fromKey = "copy.key.no.need.to.exist.2";
    transfer.toKey = "copy.to.key.with.default.2";
    transfer.defaultValue = "defaultValue2";
    transfer.ifKey = "no.such.key";
    transfer.ifType = "zoo.cfg";
    transfer.ifKeyState = PropertyKeyState.PRESENT;
    transfers.add(transfer);

    commandParams.put(ConfigureTask.PARAMETER_TRANSFERS, new Gson().toJson(transfers));

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName("c1");
    executionCommand.setRoleParams(new HashMap<String, String>());
    executionCommand.getRoleParams().put(ServerAction.ACTION_USER_NAME, "username");

    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null,
        null, null);

    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(
        executionCommand));


    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    assertEquals(3, c.getConfigsByType("zoo.cfg").size());

    config = c.getDesiredConfigByType("zoo.cfg");
    assertNotNull(config);
    assertFalse("version2".equals(config.getTag()));

    Map<String, String> map = config.getProperties();
    assertEquals(3, map.size());
    assertEquals("11", map.get("initLimit"));
    assertEquals("c1", map.get("copy.key.1"));
    assertEquals("c2", map.get("copy.key.2"));
  }

  @Test
  public void testAllowedTransferMove() throws Exception {
    makeUpgradeCluster();

    Cluster c = clusters.getCluster("c1");
    assertEquals(1, c.getConfigsByType("zoo.cfg").size());

    c.setDesiredStackVersion(HDP_220_STACK);
    Config config = cf.createNew(c, "zoo.cfg", "version2", new HashMap<String, String>() {{
          put("initLimit", "10");
          put("move.key.1", "m1");
          put("move.key.2", "m2");
          put("move.key.3", "m3");
          put("move.key.4", "m4");
        }}, new HashMap<String, Map<String,String>>());

    c.addDesiredConfig("user", Collections.singleton(config));
    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    List<ConfigurationKeyValue> configurations = new ArrayList<>();
    ConfigurationKeyValue keyValue = new ConfigurationKeyValue();
    configurations.add(keyValue);
    keyValue.key = "initLimit";
    keyValue.value = "11";

    Map<String, String> commandParams = new HashMap<String, String>();
    commandParams.put("upgrade_direction", "upgrade");
    commandParams.put("version", HDP_2_2_0_1);
    commandParams.put("clusterName", "c1");
    commandParams.put(ConfigureTask.PARAMETER_CONFIG_TYPE, "zoo.cfg");
    commandParams.put(ConfigureTask.PARAMETER_KEY_VALUE_PAIRS, new Gson().toJson(configurations));

    List<Transfer> transfers = new ArrayList<>();
    Transfer transfer1 = new Transfer();
    transfer1.operation = TransferOperation.MOVE;
    transfer1.fromKey = "move.key.1";
    transfer1.toKey = "move.to.key.1";
    transfers.add(transfer1);

    Transfer transfer2 = new Transfer();
    transfer2.operation = TransferOperation.MOVE;
    transfer2.fromKey = "move.key.2";
    transfer2.toKey = "move.to.key.2";
    transfer2.ifKey = "initLimit";
    transfer2.ifType = "zoo.cfg";
    transfer2.ifValue = "10";
    transfers.add(transfer2);

    Transfer transfer3 = new Transfer();
    transfer3.operation = TransferOperation.MOVE;
    transfer3.fromKey = "move.key.3";
    transfer3.toKey = "move.to.key.3";
    transfer3.ifKey = "initLimit";
    transfer3.ifType = "zoo.cfg";
    transfer3.ifKeyState = PropertyKeyState.PRESENT;
    transfers.add(transfer3);

    Transfer transfer4 = new Transfer();
    transfer4.operation = TransferOperation.MOVE;
    transfer4.fromKey = "move.key.4";
    transfer4.toKey = "move.to.key.4";
    transfer4.ifKey = "no.such.key";
    transfer4.ifType = "zoo.cfg";
    transfer4.ifKeyState = PropertyKeyState.ABSENT;
    transfers.add(transfer4);

    commandParams.put(ConfigureTask.PARAMETER_TRANSFERS, new Gson().toJson(transfers));

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName("c1");
    executionCommand.setRoleParams(new HashMap<String, String>());
    executionCommand.getRoleParams().put(ServerAction.ACTION_USER_NAME, "username");

    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null,
        null, null);

    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(
        executionCommand));


    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    assertEquals(3, c.getConfigsByType("zoo.cfg").size());

    config = c.getDesiredConfigByType("zoo.cfg");
    assertNotNull(config);
    assertFalse("version2".equals(config.getTag()));

    Map<String, String> map = config.getProperties();
    assertEquals(5, map.size());
    String[] shouldNotExitKeys = new String[]{"move.key.1", "move.key.2", "move.key.3", "move.key.4"};
    String[] shouldExitKeys = new String[]{"move.to.key.1", "move.to.key.2", "move.to.key.3", "move.to.key.4"};
    for(String key: shouldNotExitKeys){
      assertFalse(map.containsKey(key));
    }

    for(String key: shouldExitKeys){
      assertTrue(map.containsKey(key));
    }
  }

  @Test
  public void testDisallowedTransferMove() throws Exception {
    makeUpgradeCluster();

    Cluster c = clusters.getCluster("c1");
    assertEquals(1, c.getConfigsByType("zoo.cfg").size());

    c.setDesiredStackVersion(HDP_220_STACK);
    Config config = cf.createNew(c, "zoo.cfg", "version2",
        new HashMap<String, String>() {{
          put("initLimit", "10");
          put("move.key.1", "m1");
          put("move.key.2", "m2");
          put("move.key.3", "m3");
          put("move.key.4", "m4");
        }}, new HashMap<String, Map<String,String>>());

    c.addDesiredConfig("user", Collections.singleton(config));
    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    List<ConfigurationKeyValue> configurations = new ArrayList<>();
    ConfigurationKeyValue keyValue = new ConfigurationKeyValue();
    configurations.add(keyValue);
    keyValue.key = "initLimit";
    keyValue.value = "11";

    Map<String, String> commandParams = new HashMap<String, String>();
    commandParams.put("upgrade_direction", "upgrade");
    commandParams.put("version", HDP_2_2_0_1);
    commandParams.put("clusterName", "c1");
    commandParams.put(ConfigureTask.PARAMETER_CONFIG_TYPE, "zoo.cfg");
    commandParams.put(ConfigureTask.PARAMETER_KEY_VALUE_PAIRS, new Gson().toJson(configurations));

    List<Transfer> transfers = new ArrayList<>();
    Transfer transfer2 = new Transfer();
    transfer2.operation = TransferOperation.MOVE;
    transfer2.fromKey = "move.key.2";
    transfer2.toKey = "move.to.key.2";
    transfer2.ifKey = "initLimit";
    transfer2.ifType = "zoo.cfg";
    transfer2.ifValue = "not-real-value";
    transfers.add(transfer2);

    Transfer transfer3 = new Transfer();
    transfer3.operation = TransferOperation.MOVE;
    transfer3.fromKey = "move.key.3";
    transfer3.toKey = "move.to.key.3";
    transfer3.ifKey = "initLimit";
    transfer3.ifType = "zoo.cfg";
    transfer3.ifKeyState = PropertyKeyState.ABSENT;
    transfers.add(transfer3);

    Transfer transfer4 = new Transfer();
    transfer4.operation = TransferOperation.MOVE;
    transfer4.fromKey = "move.key.4";
    transfer4.toKey = "move.to.key.4";
    transfer4.ifKey = "no.such.key";
    transfer4.ifType = "zoo.cfg";
    transfer4.ifKeyState = PropertyKeyState.PRESENT;
    transfers.add(transfer3);

    commandParams.put(ConfigureTask.PARAMETER_TRANSFERS, new Gson().toJson(transfers));

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName("c1");
    executionCommand.setRoleParams(new HashMap<String, String>());
    executionCommand.getRoleParams().put(ServerAction.ACTION_USER_NAME, "username");

    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null,
        null, null);

    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(
        executionCommand));


    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    assertEquals(3, c.getConfigsByType("zoo.cfg").size());

    config = c.getDesiredConfigByType("zoo.cfg");
    assertNotNull(config);
    assertFalse("version2".equals(config.getTag()));

    Map<String, String> map = config.getProperties();
    assertEquals(5, map.size());

    String[] shouldExitKeys = new String[]{"move.key.1", "move.key.2", "move.key.3", "move.key.4"};
    String[] shouldNotExitKeys = new String[]{"move.to.key.1", "move.to.key.2", "move.to.key.3", "move.to.key.4"};
    for(String key: shouldNotExitKeys){
      assertFalse(map.containsKey(key));
    }

    for(String key: shouldExitKeys){
      assertTrue(map.containsKey(key));
    }
  }

  @Test
  public void testAllowedTransferDelete() throws Exception {
    makeUpgradeCluster();

    Cluster c = clusters.getCluster("c1");
    assertEquals(1, c.getConfigsByType("zoo.cfg").size());

    c.setDesiredStackVersion(HDP_220_STACK);
    Config config = cf.createNew(c, "zoo.cfg", "version2", new HashMap<String, String>() {{
          put("initLimit", "10");
          put("delete.key.1", "d1");
          put("delete.key.2", "d2");
          put("delete.key.3", "d3");
          put("delete.key.4", "d4");
        }}, new HashMap<String, Map<String,String>>());

    c.addDesiredConfig("user", Collections.singleton(config));
    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    List<ConfigurationKeyValue> configurations = new ArrayList<>();
    ConfigurationKeyValue keyValue = new ConfigurationKeyValue();
    configurations.add(keyValue);
    keyValue.key = "initLimit";
    keyValue.value = "11";

    Map<String, String> commandParams = new HashMap<String, String>();
    commandParams.put("upgrade_direction", "upgrade");
    commandParams.put("version", HDP_2_2_0_1);
    commandParams.put("clusterName", "c1");
    commandParams.put(ConfigureTask.PARAMETER_CONFIG_TYPE, "zoo.cfg");
    commandParams.put(ConfigureTask.PARAMETER_KEY_VALUE_PAIRS, new Gson().toJson(configurations));

    List<Transfer> transfers = new ArrayList<>();
    Transfer transfer1 = new Transfer();
    transfer1.operation = TransferOperation.DELETE;
    transfer1.deleteKey = "delete.key.1";
    transfers.add(transfer1);

    Transfer transfer2 = new Transfer();
    transfer2.operation = TransferOperation.DELETE;
    transfer2.deleteKey = "delete.key.2";
    transfer2.ifKey = "initLimit";
    transfer2.ifType = "zoo.cfg";
    transfer2.ifValue = "10";
    transfers.add(transfer2);

    Transfer transfer3 = new Transfer();
    transfer3.operation = TransferOperation.DELETE;
    transfer3.deleteKey = "delete.key.3";
    transfer3.ifKey = "initLimit";
    transfer3.ifType = "zoo.cfg";
    transfer3.ifKeyState = PropertyKeyState.PRESENT;
    transfers.add(transfer3);

    Transfer transfer4 = new Transfer();
    transfer4.operation = TransferOperation.DELETE;
    transfer4.deleteKey = "delete.key.4";
    transfer4.ifKey = "no.such.key";
    transfer4.ifType = "zoo.cfg";
    transfer4.ifKeyState = PropertyKeyState.ABSENT;
    transfers.add(transfer4);

    commandParams.put(ConfigureTask.PARAMETER_TRANSFERS, new Gson().toJson(transfers));

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName("c1");
    executionCommand.setRoleParams(new HashMap<String, String>());
    executionCommand.getRoleParams().put(ServerAction.ACTION_USER_NAME, "username");

    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null,
        null, null);

    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(
        executionCommand));


    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    assertEquals(3, c.getConfigsByType("zoo.cfg").size());

    config = c.getDesiredConfigByType("zoo.cfg");
    assertNotNull(config);
    assertFalse("version2".equals(config.getTag()));

    Map<String, String> map = config.getProperties();
    assertEquals(1, map.size());
    assertEquals("11", map.get("initLimit"));
    String[] shouldNotExitKeys = new String[]{"delete.key.1","delete.key.2","delete.key.3","delete.key.4"};
    for(String key: shouldNotExitKeys){
      assertFalse(map.containsKey(key));
    }
  }

  @Test
  public void testDisallowedTransferDelete() throws Exception {
    makeUpgradeCluster();

    Cluster c = clusters.getCluster("c1");
    assertEquals(1, c.getConfigsByType("zoo.cfg").size());

    c.setDesiredStackVersion(HDP_220_STACK);
    Config config = cf.createNew(c, "zoo.cfg", "version2", new HashMap<String, String>() {{
          put("initLimit", "10");
          put("delete.key.1", "d1");
          put("delete.key.2", "d2");
          put("delete.key.3", "d3");
          put("delete.key.4", "d4");
        }}, new HashMap<String, Map<String,String>>());

    c.addDesiredConfig("user", Collections.singleton(config));
    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    List<ConfigurationKeyValue> configurations = new ArrayList<>();
    ConfigurationKeyValue keyValue = new ConfigurationKeyValue();
    configurations.add(keyValue);
    keyValue.key = "initLimit";
    keyValue.value = "11";

    Map<String, String> commandParams = new HashMap<String, String>();
    commandParams.put("upgrade_direction", "upgrade");
    commandParams.put("version", HDP_2_2_0_1);
    commandParams.put("clusterName", "c1");
    commandParams.put(ConfigureTask.PARAMETER_CONFIG_TYPE, "zoo.cfg");
    commandParams.put(ConfigureTask.PARAMETER_KEY_VALUE_PAIRS, new Gson().toJson(configurations));

    List<Transfer> transfers = new ArrayList<>();
    Transfer transfer2 = new Transfer();
    transfer2.operation = TransferOperation.DELETE;
    transfer2.deleteKey = "delete.key.2";
    transfer2.ifKey = "initLimit";
    transfer2.ifType = "zoo.cfg";
    transfer2.ifValue = "not.real.value";
    transfers.add(transfer2);

    Transfer transfer3 = new Transfer();
    transfer3.operation = TransferOperation.DELETE;
    transfer3.deleteKey = "delete.key.3";
    transfer3.ifKey = "initLimit";
    transfer3.ifType = "zoo.cfg";
    transfer3.ifKeyState = PropertyKeyState.ABSENT;
    transfers.add(transfer3);

    Transfer transfer4 = new Transfer();
    transfer4.operation = TransferOperation.DELETE;
    transfer4.deleteKey = "delete.key.4";
    transfer4.ifKey = "no.such.key";
    transfer4.ifType = "zoo.cfg";
    transfer4.ifKeyState = PropertyKeyState.PRESENT;
    transfers.add(transfer4);

    commandParams.put(ConfigureTask.PARAMETER_TRANSFERS, new Gson().toJson(transfers));

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName("c1");
    executionCommand.setRoleParams(new HashMap<String, String>());
    executionCommand.getRoleParams().put(ServerAction.ACTION_USER_NAME, "username");

    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null,
        null, null);

    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(
        executionCommand));


    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    assertEquals(3, c.getConfigsByType("zoo.cfg").size());

    config = c.getDesiredConfigByType("zoo.cfg");
    assertNotNull(config);
    assertFalse("version2".equals(config.getTag()));

    Map<String, String> map = config.getProperties();
    assertEquals(5, map.size());
    assertEquals("11", map.get("initLimit"));
    String[] shouldExitKeys = new String[]{"delete.key.1","delete.key.2","delete.key.3","delete.key.4"};
    for(String key: shouldExitKeys){
      assertTrue(map.containsKey(key));
    }
  }

  private void makeUpgradeCluster() throws Exception {
    String clusterName = "c1";
    String hostName = "h1";

    clusters.addCluster(clusterName, HDP_220_STACK);


    StackEntity stackEntity = stackDAO.find(HDP_220_STACK.getStackName(),
        HDP_220_STACK.getStackVersion());

    assertNotNull(stackEntity);

    Cluster c = clusters.getCluster(clusterName);
    c.setDesiredStackVersion(HDP_220_STACK);

    // !!! very important, otherwise the loops that walk the list of installed
    // service properties will not run!
    installService(c, "ZOOKEEPER");

    Config config = cf.createNew(c, "zoo.cfg", "version1", new HashMap<String, String>() {
      {
        put("initLimit", "10");
      }
    }, new HashMap<String, Map<String, String>>());

    c.addDesiredConfig("user", Collections.singleton(config));

    // add a host component
    clusters.addHost(hostName);

    Host host = clusters.getHost(hostName);

    Map<String, String> hostAttributes = new HashMap<String, String>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "6");
    host.setHostAttributes(hostAttributes);

    // Creating starting repo
    m_helper.getOrCreateRepositoryVersion(HDP_220_STACK, HDP_2_2_0_0);
    c.createClusterVersion(HDP_220_STACK, HDP_2_2_0_0, "admin", RepositoryVersionState.INSTALLING);
    c.transitionClusterVersion(HDP_220_STACK, HDP_2_2_0_0, RepositoryVersionState.CURRENT);

    String urlInfo = "[{'repositories':["
        + "{'Repositories/base_url':'http://foo1','Repositories/repo_name':'HDP','Repositories/repo_id':'HDP-2.2.0'}"
        + "], 'OperatingSystems/os_type':'redhat6'}]";
    repoVersionDAO.create(stackEntity, HDP_2_2_0_1, String.valueOf(System.currentTimeMillis()), urlInfo);


    c.createClusterVersion(HDP_220_STACK, HDP_2_2_0_1, "admin", RepositoryVersionState.INSTALLING);
    c.transitionClusterVersion(HDP_220_STACK, HDP_2_2_0_1, RepositoryVersionState.INSTALLED);
    c.setCurrentStackVersion(HDP_220_STACK);

    c.mapHostVersions(Collections.singleton(hostName), c.getCurrentClusterVersion(),
        RepositoryVersionState.CURRENT);


    HostVersionEntity entity = new HostVersionEntity();
    entity.setHostEntity(hostDAO.findByName(hostName));
    entity.setRepositoryVersion(repoVersionDAO.findByStackAndVersion(HDP_220_STACK, HDP_2_2_0_1));
    entity.setState(RepositoryVersionState.INSTALLED);
    hostVersionDAO.create(entity);

    // verify that our configs are there
    String tickTime = m_configHelper.getPropertyValueFromStackDefinitions(c, "zoo.cfg", "tickTime");
    assertNotNull(tickTime);
  }

  /**
   * Installs a service in the cluster.
   *
   * @param cluster
   * @param serviceName
   * @return
   * @throws AmbariException
   */
  private Service installService(Cluster cluster, String serviceName) throws AmbariException {
    Service service = null;

    try {
      service = cluster.getService(serviceName);
    } catch (ServiceNotFoundException e) {
      service = serviceFactory.createNew(cluster, serviceName);
      cluster.addService(service);
    }

    return service;
  }
}
