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

import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;
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
import org.apache.ambari.server.state.stack.upgrade.TransferCoercionType;
import org.apache.ambari.server.state.stack.upgrade.TransferOperation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests upgrade-related server side actions
 */
public class ConfigureActionTest {

  private static final String HDP_2_2_0_0 = "2.2.0.0-2041";
  private static final String HDP_2_2_0_1 = "2.2.0.1-2270";
  private static final StackId HDP_211_STACK = new StackId("HDP-2.1.1");
  private static final StackId HDP_220_STACK = new StackId("HDP-2.2.0");

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
  ConfigHelper m_configHelper;

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

    Cluster c = m_injector.getInstance(Clusters.class).getCluster("c1");
    assertEquals(1, c.getConfigsByType("zoo.cfg").size());

    c.setCurrentStackVersion(HDP_211_STACK);
    c.setDesiredStackVersion(HDP_220_STACK);
    ConfigFactory cf = m_injector.getInstance(ConfigFactory.class);
    Config config = cf.createNew(c, "zoo.cfg", new HashMap<String, String>() {{
          put("initLimit", "10");
        }}, new HashMap<String, Map<String,String>>());
    config.setTag("version2");
    config.persist();

    c.addConfig(config);
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

    ConfigureAction action = m_injector.getInstance(ConfigureAction.class);
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

    Cluster c = m_injector.getInstance(Clusters.class).getCluster("c1");
    assertEquals(1, c.getConfigsByType("zoo.cfg").size());

    c.setDesiredStackVersion(HDP_220_STACK);

    // create a config for zoo.cfg with two values; one is a stack value and the
    // other is custom
    ConfigFactory cf = m_injector.getInstance(ConfigFactory.class);
    Config config = cf.createNew(c, "zoo.cfg", new HashMap<String, String>() {
      {
        put("tickTime", "2000");
        put("foo", "bar");
      }
    }, new HashMap<String, Map<String, String>>());
    config.setTag("version2");
    config.persist();

    c.addConfig(config);
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

    ConfigureAction action = m_injector.getInstance(ConfigureAction.class);
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

    Cluster c = m_injector.getInstance(Clusters.class).getCluster("c1");
    assertEquals(1, c.getConfigsByType("zoo.cfg").size());

    c.setDesiredStackVersion(HDP_220_STACK);
    ConfigFactory cf = m_injector.getInstance(ConfigFactory.class);
    Config config = cf.createNew(c, "zoo.cfg", new HashMap<String, String>() {{
          put("initLimit", "10");
          put("copyIt", "10");
          put("moveIt", "10");
          put("deleteIt", "10");
        }}, new HashMap<String, Map<String,String>>());
    config.setTag("version2");
    config.persist();

    c.addConfig(config);
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

    ConfigureAction action = m_injector.getInstance(ConfigureAction.class);
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

    Cluster c = m_injector.getInstance(Clusters.class).getCluster("c1");
    assertEquals(1, c.getConfigsByType("zoo.cfg").size());

    c.setDesiredStackVersion(HDP_220_STACK);
    ConfigFactory cf = m_injector.getInstance(ConfigFactory.class);
    Config config = cf.createNew(c, "zoo.cfg", new HashMap<String, String>() {
      {
        put("zoo.server.csv", "c6401,c6402,  c6403");
      }
    }, new HashMap<String, Map<String, String>>());
    config.setTag("version2");
    config.persist();

    c.addConfig(config);
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

    ConfigureAction action = m_injector.getInstance(ConfigureAction.class);
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

    Cluster c = m_injector.getInstance(Clusters.class).getCluster("c1");
    assertEquals(1, c.getConfigsByType("zoo.cfg").size());

    c.setDesiredStackVersion(HDP_220_STACK);
    ConfigFactory cf = m_injector.getInstance(ConfigFactory.class);
    Config config = cf.createNew(c, "zoo.cfg", new HashMap<String, String>() {
      {
        put("key_to_replace", "My New Cat");
        put("key_with_no_match", "WxyAndZ");
      }
    }, new HashMap<String, Map<String, String>>());
    config.setTag("version2");
    config.persist();

    c.addConfig(config);
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

    ConfigureAction action = m_injector.getInstance(ConfigureAction.class);
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

    Cluster c = m_injector.getInstance(Clusters.class).getCluster("c1");
    assertEquals(1, c.getConfigsByType("zoo.cfg").size());

    c.setDesiredStackVersion(HDP_220_STACK);
    ConfigFactory cf = m_injector.getInstance(ConfigFactory.class);
    Config config = cf.createNew(c, "zoo.cfg", new HashMap<String, String>() {
      {
        put("existing", "This exists!");
        put("missing", null);
      }
    }, new HashMap<String, Map<String, String>>());
    config.setTag("version2");
    config.persist();

    c.addConfig(config);
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

    ConfigureAction action = m_injector.getInstance(ConfigureAction.class);
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

    Cluster c = m_injector.getInstance(Clusters.class).getCluster("c1");
    assertEquals(1, c.getConfigsByType("zoo.cfg").size());

    c.setCurrentStackVersion(HDP_211_STACK);
    c.setDesiredStackVersion(HDP_220_STACK);
    ConfigFactory cf = m_injector.getInstance(ConfigFactory.class);
    Config config = cf.createNew(c, "zoo.cfg", new HashMap<String, String>() {
      {
        put("fooKey", "barValue");
      }
    }, new HashMap<String, Map<String, String>>());

    config.setTag("version2");
    config.persist();

    c.addConfig(config);
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

    ConfigureAction action = m_injector.getInstance(ConfigureAction.class);
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

  private void makeUpgradeCluster() throws Exception {
    String clusterName = "c1";
    String hostName = "h1";

    Clusters clusters = m_injector.getInstance(Clusters.class);
    clusters.addCluster(clusterName, HDP_220_STACK);

    StackDAO stackDAO = m_injector.getInstance(StackDAO.class);
    StackEntity stackEntity = stackDAO.find(HDP_220_STACK.getStackName(),
        HDP_220_STACK.getStackVersion());

    assertNotNull(stackEntity);

    Cluster c = clusters.getCluster(clusterName);
    c.setDesiredStackVersion(HDP_220_STACK);

    // !!! very important, otherwise the loops that walk the list of installed
    // service properties will not run!
    installService(c, "ZOOKEEPER");

    ConfigFactory cf = m_injector.getInstance(ConfigFactory.class);
    Config config = cf.createNew(c, "zoo.cfg", new HashMap<String, String>() {
      {
        put("initLimit", "10");
      }
    }, new HashMap<String, Map<String, String>>());
    config.setTag("version1");
    config.persist();

    c.addConfig(config);
    c.addDesiredConfig("user", Collections.singleton(config));

    // add a host component
    clusters.addHost(hostName);

    Host host = clusters.getHost(hostName);

    Map<String, String> hostAttributes = new HashMap<String, String>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "6");
    host.setHostAttributes(hostAttributes);
    host.persist();

    // Creating starting repo
    m_helper.getOrCreateRepositoryVersion(HDP_220_STACK, HDP_2_2_0_0);
    c.createClusterVersion(HDP_220_STACK, HDP_2_2_0_0, "admin", RepositoryVersionState.UPGRADING);
    c.transitionClusterVersion(HDP_220_STACK, HDP_2_2_0_0, RepositoryVersionState.CURRENT);

    String urlInfo = "[{'repositories':["
        + "{'Repositories/base_url':'http://foo1','Repositories/repo_name':'HDP','Repositories/repo_id':'HDP-2.2.0'}"
        + "], 'OperatingSystems/os_type':'redhat6'}]";
    repoVersionDAO.create(stackEntity, HDP_2_2_0_1, String.valueOf(System.currentTimeMillis()), urlInfo);


    c.createClusterVersion(HDP_220_STACK, HDP_2_2_0_1, "admin", RepositoryVersionState.INSTALLING);
    c.transitionClusterVersion(HDP_220_STACK, HDP_2_2_0_1, RepositoryVersionState.INSTALLED);
    c.transitionClusterVersion(HDP_220_STACK, HDP_2_2_0_1, RepositoryVersionState.UPGRADING);
    c.transitionClusterVersion(HDP_220_STACK, HDP_2_2_0_1, RepositoryVersionState.UPGRADED);
    c.setCurrentStackVersion(HDP_220_STACK);

    c.mapHostVersions(Collections.singleton(hostName), c.getCurrentClusterVersion(),
        RepositoryVersionState.CURRENT);

    HostDAO hostDAO = m_injector.getInstance(HostDAO.class);

    HostVersionEntity entity = new HostVersionEntity();
    entity.setHostEntity(hostDAO.findByName(hostName));
    entity.setRepositoryVersion(repoVersionDAO.findByStackAndVersion(HDP_220_STACK, HDP_2_2_0_1));
    entity.setState(RepositoryVersionState.UPGRADED);
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
      service.persist();
    }

    return service;
  }
}
