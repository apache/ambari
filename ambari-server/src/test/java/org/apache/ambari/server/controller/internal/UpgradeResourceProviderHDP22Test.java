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
package org.apache.ambari.server.controller.internal;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.ExecutionCommandWrapper;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.agent.ExecutionCommand.KeyNames;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.ExecutionCommandDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.dao.UpgradeDAO;
import org.apache.ambari.server.orm.entities.ExecutionCommandEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.orm.entities.UpgradeGroupEntity;
import org.apache.ambari.server.orm.entities.UpgradeItemEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.ConfigImpl;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.topology.TopologyManager;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.ambari.server.view.ViewRegistry;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.persist.PersistService;
import com.google.inject.util.Modules;

/**
 * UpgradeResourceDefinition tests.
 */
public class UpgradeResourceProviderHDP22Test {

  private UpgradeDAO upgradeDao = null;
  private RepositoryVersionDAO repoVersionDao = null;
  private Injector injector;
  private Clusters clusters;
  private OrmTestHelper helper;
  private AmbariManagementController amc;
  private ConfigHelper configHelper;
  private StackDAO stackDAO;
  private TopologyManager topologyManager;

  private static final String configTagVersion1 = "version1";
  private static final String configTagVersion2 = "version2";
  @SuppressWarnings("serial")
  private static final Map<String, String> configTagVersion1Properties = new HashMap<String, String>() {
    {
      put("hive.server2.thrift.port", "10000");
    }
  };

  private static final Map<String, String> configTagVersion2Properties = new HashMap<String, String>() {
    {
      put("hive.server2.thrift.port", "10010");
    }
  };

  @SuppressWarnings({ "serial", "unchecked" })
  @Before
  public void before() throws Exception {
    // setup the config helper for placeholder resolution
    configHelper = EasyMock.createNiceMock(ConfigHelper.class);

    expect(configHelper.getPlaceholderValueFromDesiredConfigurations(EasyMock.anyObject(Cluster.class), EasyMock.eq("{{foo/bar}}"))).andReturn(
        "placeholder-rendered-properly").anyTimes();

    expect(configHelper.getDefaultProperties(EasyMock.anyObject(StackId.class), EasyMock.anyObject(Cluster.class))).andReturn(
        new HashMap<String, Map<String, String>>()).anyTimes();

    expect(configHelper.getEffectiveConfigAttributes(EasyMock.anyObject(Cluster.class), EasyMock.anyObject(Map.class))).andReturn(
        new HashMap<String, Map<String, Map<String, String>>>()).anyTimes();

    expect(configHelper.getEffectiveDesiredTags(EasyMock.anyObject(Cluster.class), EasyMock.eq("h1"))).andReturn(new HashMap<String, Map<String, String>>() {
      {
        put("hive-site", new HashMap<String, String>() {
          {
            put("tag", configTagVersion1);
          }
        });
      }
    }).times(3);

    expect(configHelper.getEffectiveDesiredTags(EasyMock.anyObject(Cluster.class), EasyMock.eq("h1"))).andReturn(new HashMap<String, Map<String, String>>() {
      {
        put("hive-site", new HashMap<String, String>() {
          {
            put("tag", configTagVersion2);
          }
        });
      }
    }).times(2);

    expect(configHelper.getEffectiveConfigProperties(EasyMock.anyObject(Cluster.class), EasyMock.anyObject(Map.class))).andReturn(
        new HashMap<String, Map<String, String>>() {
          {
            put("hive-site", configTagVersion1Properties);
          }
        }).times(1);

    expect(configHelper.getEffectiveConfigProperties(EasyMock.anyObject(Cluster.class), EasyMock.anyObject(Map.class))).andReturn(
        new HashMap<String, Map<String, String>>() {
          {
            put("hive-site", configTagVersion2Properties);
          }
        }).times(2);

    expect(configHelper.getMergedConfig(EasyMock.anyObject(Map.class),
        EasyMock.anyObject(Map.class))).andReturn(new HashMap<String, String>()).anyTimes();

    EasyMock.replay(configHelper);

    // create an injector which will inject the mocks
    injector = Guice.createInjector(Modules.override(new InMemoryDefaultTestModule()).with(new MockModule()));

    injector.getInstance(GuiceJpaInitializer.class);

    helper = injector.getInstance(OrmTestHelper.class);

    amc = injector.getInstance(AmbariManagementController.class);

    Field field = AmbariServer.class.getDeclaredField("clusterController");
    field.setAccessible(true);
    field.set(null, amc);

    stackDAO = injector.getInstance(StackDAO.class);
    upgradeDao = injector.getInstance(UpgradeDAO.class);
    repoVersionDao = injector.getInstance(RepositoryVersionDAO.class);

    AmbariEventPublisher publisher = createNiceMock(AmbariEventPublisher.class);
    replay(publisher);
    ViewRegistry.initInstance(new ViewRegistry(publisher));

    StackEntity stackEntity = stackDAO.find("HDP", "2.2.0");

    RepositoryVersionEntity repoVersionEntity = new RepositoryVersionEntity();
    repoVersionEntity.setDisplayName("For Stack Version 2.2.0");
    repoVersionEntity.setOperatingSystems("");
    repoVersionEntity.setStack(stackEntity);
    repoVersionEntity.setVersion("2.2.0.0");
    repoVersionDao.create(repoVersionEntity);

    repoVersionEntity = new RepositoryVersionEntity();
    repoVersionEntity.setDisplayName("For Stack Version 2.2.4.2");
    repoVersionEntity.setOperatingSystems("");
    repoVersionEntity.setStack(stackEntity);
    repoVersionEntity.setVersion("2.2.4.2");
    repoVersionDao.create(repoVersionEntity);

    clusters = injector.getInstance(Clusters.class);

    StackId stackId = new StackId("HDP-2.2.0");
    clusters.addCluster("c1", stackId);
    Cluster cluster = clusters.getCluster("c1");

    helper.getOrCreateRepositoryVersion(stackId, stackId.getStackVersion());
    cluster.createClusterVersion(stackId, stackId.getStackVersion(), "admin", RepositoryVersionState.UPGRADING);
    cluster.transitionClusterVersion(stackId, stackId.getStackVersion(), RepositoryVersionState.CURRENT);

    clusters.addHost("h1");
    Host host = clusters.getHost("h1");
    Map<String, String> hostAttributes = new HashMap<String, String>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "6.3");
    host.setHostAttributes(hostAttributes);
    host.setState(HostState.HEALTHY);
    host.persist();

    clusters.mapHostToCluster("h1", "c1");

    // add a single HIVE server
    Service service = cluster.addService("HIVE");
    service.setDesiredStackVersion(cluster.getDesiredStackVersion());
    service.persist();

    ServiceComponent component = service.addServiceComponent("HIVE_SERVER");
    ServiceComponentHost sch = component.addServiceComponentHost("h1");
    sch.setVersion("2.2.0.0");

    component = service.addServiceComponent("HIVE_CLIENT");
    sch = component.addServiceComponentHost("h1");
    sch.setVersion("2.2.0.0");
    topologyManager = injector.getInstance(TopologyManager.class);
    StageUtils.setTopologyManager(topologyManager);
    ActionManager.setTopologyManager(topologyManager);
  }

  @After
  public void after() {
    injector.getInstance(PersistService.class).stop();
    injector = null;
  }

  private void setupConfigHelper() {

  }

  /**
   * Tests upgrades from HDP-2.2.x to HDP-2.2.y
   *
   * @throws Exception
   */
  @SuppressWarnings("serial")
  @Test
  public void testCreateIntraStackUpgrade() throws Exception {
    // We want to use the HDP-2.2 'upgrade_test' catalog
    // Create HDP-2.2 stack

    Cluster cluster = clusters.getCluster("c1");
    StackId oldStack = cluster.getDesiredStackVersion();

    for (Service s : cluster.getServices().values()) {
      assertEquals(oldStack, s.getDesiredStackVersion());

      for (ServiceComponent sc : s.getServiceComponents().values()) {
        assertEquals(oldStack, sc.getDesiredStackVersion());

        for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
          assertEquals(oldStack, sch.getDesiredStackVersion());
        }
      }
    }

    Config config = new ConfigImpl("hive-site");
    config.setProperties(configTagVersion1Properties);
    config.setTag(configTagVersion1);

    cluster.addConfig(config);
    cluster.addDesiredConfig("admin", Collections.singleton(config));

    Map<String, Object> requestProps = new HashMap<String, Object>();
    requestProps.put(UpgradeResourceProvider.UPGRADE_CLUSTER_NAME, "c1");
    requestProps.put(UpgradeResourceProvider.UPGRADE_VERSION, "2.2.4.2");
    requestProps.put(UpgradeResourceProvider.UPGRADE_SKIP_PREREQUISITE_CHECKS, "true");

    ResourceProvider upgradeResourceProvider = createProvider(amc);

    Request request = PropertyHelper.getCreateRequest(Collections.singleton(requestProps), null);
    upgradeResourceProvider.createResources(request);

    List<UpgradeEntity> upgrades = upgradeDao.findUpgrades(cluster.getClusterId());
    assertEquals(1, upgrades.size());

    UpgradeEntity upgrade = upgrades.get(0);
    assertEquals("upgrade_test", upgrade.getUpgradePackage());
    assertEquals(3, upgrade.getUpgradeGroups().size());

    UpgradeGroupEntity group = upgrade.getUpgradeGroups().get(2);
    assertEquals(3, group.getItems().size());

    group = upgrade.getUpgradeGroups().get(0);
    assertEquals(2, group.getItems().size());
    UpgradeItemEntity item = group.getItems().get(1);
    assertEquals("Value is set for the source stack upgrade pack", "Goo", item.getText());

    assertTrue(cluster.getDesiredConfigs().containsKey("hive-site"));

    StackId newStack = cluster.getDesiredStackVersion();

    assertTrue(oldStack.equals(newStack));

    for (Service s : cluster.getServices().values()) {
      assertEquals(newStack, s.getDesiredStackVersion());

      for (ServiceComponent sc : s.getServiceComponents().values()) {
        assertEquals(newStack, sc.getDesiredStackVersion());

        for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
          assertEquals(newStack, sch.getDesiredStackVersion());
        }
      }
    }

    // Hive service checks have generated the ExecutionCommands by now.
    // Change the new desired config tag and verify execution command picks up new tag
    assertEquals(configTagVersion1, cluster.getDesiredConfigByType("hive-site").getTag());
    final Config newConfig = new ConfigImpl("hive-site");
    newConfig.setProperties(configTagVersion2Properties);
    newConfig.setTag(configTagVersion2);
    Set<Config> desiredConfigs = new HashSet<Config>() {
      {
        add(newConfig);
      }
    };

    cluster.addConfig(newConfig);
    cluster.addDesiredConfig("admin", desiredConfigs);
    assertEquals(configTagVersion2, cluster.getDesiredConfigByType("hive-site").getTag());
    Gson gson = new Gson();

    List<ExecutionCommandEntity> currentExecutionCommands = injector.getInstance(ExecutionCommandDAO.class).findAll();
    for (ExecutionCommandEntity ece : currentExecutionCommands) {
      String executionCommandJson = new String(ece.getCommand());
      Map<String, Object> commandMap = gson.<Map<String, Object>> fromJson(executionCommandJson, Map.class);

      // ensure that the latest tag is being used and that "*" is forcing a
      // refresh - this is absolutely required for upgrades
      Set<String> roleCommandsThatMustHaveRefresh = new HashSet<String>();
      roleCommandsThatMustHaveRefresh.add("SERVICE_CHECK");
      roleCommandsThatMustHaveRefresh.add("RESTART");
      roleCommandsThatMustHaveRefresh.add("ACTIONEXECUTE");

      String roleCommand = (String) commandMap.get("roleCommand");
      if (roleCommandsThatMustHaveRefresh.contains(roleCommand)) {
        assertTrue(commandMap.containsKey(KeyNames.REFRESH_CONFIG_TAGS_BEFORE_EXECUTION));
        Object object = commandMap.get(KeyNames.REFRESH_CONFIG_TAGS_BEFORE_EXECUTION);
        assertTrue(object instanceof List);

        @SuppressWarnings("unchecked")
        List<String> tags = (List<String>) commandMap.get(KeyNames.REFRESH_CONFIG_TAGS_BEFORE_EXECUTION);
        assertEquals(1, tags.size());
        assertEquals("*", tags.get(0));

        ExecutionCommandWrapper executionCommandWrapper = new ExecutionCommandWrapper(executionCommandJson);
        ExecutionCommand executionCommand = executionCommandWrapper.getExecutionCommand();
        Map<String, Map<String, String>> configurationTags = executionCommand.getConfigurationTags();
        assertEquals(configTagVersion2, configurationTags.get("hive-site").get("tag"));
        Map<String, Map<String, String>> configurations = executionCommand.getConfigurations();
        assertEquals("10010", configurations.get("hive-site").get("hive.server2.thrift.port"));
      }
    }
  }

  /**
   * @param amc
   * @return the provider
   */
  private UpgradeResourceProvider createProvider(AmbariManagementController amc) {
    return new UpgradeResourceProvider(amc);
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
      binder.bind(ConfigHelper.class).toInstance(configHelper);
    }
  }
}
