/*
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

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.ServiceComponentNotFoundException;
import org.apache.ambari.server.ServiceNotFoundException;
import org.apache.ambari.server.actionmanager.ExecutionCommandWrapper;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.RequestDAO;
import org.apache.ambari.server.orm.dao.ServiceGroupDAO;
import org.apache.ambari.server.orm.dao.UpgradeDAO;
import org.apache.ambari.server.orm.entities.MpackEntity;
import org.apache.ambari.server.orm.entities.RequestEntity;
import org.apache.ambari.server.orm.entities.ServiceGroupEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.orm.entities.UpgradeHistoryEntity;
import org.apache.ambari.server.serveraction.ServerAction;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Mpack;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentFactory;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.ServiceGroup;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.stack.upgrade.ConfigUpgradeChangeDefinition.ConfigurationKeyValue;
import org.apache.ambari.server.state.stack.upgrade.CreateAndConfigureTask;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * Tests upgrade-related server side actions
 */
public class CreateAndConfigureActionTest {

  @Inject
  private Injector m_injector;

  @Inject
  private OrmTestHelper m_helper;

  @Inject
  private HostRoleCommandFactory hostRoleCommandFactory;

  @Inject
  private ServiceFactory serviceFactory;

  @Inject
  private ConfigHelper m_configHelper;

  @Inject
  private Clusters clusters;

  @Inject
  private ConfigFactory configFactory;

  @Inject
  private CreateAndConfigureAction action;

  @Inject
  private RequestDAO requestDAO;

  @Inject
  private UpgradeDAO upgradeDAO;

  @Inject
  private ServiceComponentFactory serviceComponentFactory;

  @Inject
  private ServiceComponentHostFactory serviceComponentHostFactory;

  @Inject
  private AmbariMetaInfo metaInfo;

  private MpackEntity mpack211;
  private MpackEntity mpack220;
  private MpackEntity mpack221;

  private final Map<String, Map<String, String>> NO_ATTRIBUTES = new HashMap<>();

  @Before
  public void setup() throws Exception {
    m_injector = Guice.createInjector(new InMemoryDefaultTestModule());
    m_injector.getInstance(GuiceJpaInitializer.class);
    m_injector.injectMembers(this);

    mpack211 = m_helper.createMpack(new StackId("HDP-2.1.1"));
    mpack220 = m_helper.createMpack(new StackId("HDP-2.2.0"));
    mpack221 = m_helper.createMpack(new StackId("HDP-2.2.1"));

    makeUpgradeCluster();
  }

  @After
  public void teardown() throws Exception {
    H2DatabaseCleaner.clearDatabase(m_injector.getProvider(EntityManager.class).get());
  }


  /**
   * Tests that a new configuration is created when upgrading across stack when
   * there is no existing configuration with the correct target stack.
   */
  @Test
  public void testNewConfigCreatedWhenUpgradingWithoutChaningStack() throws Exception {
    Cluster c = clusters.getCluster("c1");
    assertEquals(1, c.getConfigsByType("zoo.cfg").size());

    Map<String, String> properties = new HashMap<String, String>() {
      {
        put("initLimit", "10");
      }
    };

    Config config = createConfig(c, "zoo.cfg", "version2", properties);

    c.addDesiredConfig("user", Collections.singleton(config));
    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    List<ConfigurationKeyValue> configurations = new ArrayList<>();
    ConfigurationKeyValue keyValue = new ConfigurationKeyValue();
    configurations.add(keyValue);
    keyValue.key = "initLimit";
    keyValue.value = "11";
    c.setCurrentStackVersion(mpack211.getStackId());
    c.setDesiredStackVersion(mpack220.getStackId());

    createUpgrade(c, mpack220);

    Mpack.MpackChangeSummary mpackChangeSummary = createNiceMock(Mpack.MpackChangeSummary.class);
    Mpack mpack = createNiceMock("myMock", Mpack.class);
    Mpack mpack2 = createNiceMock("myMock2", Mpack.class);
    expect(mpack.getChangeSummary(mpack2)).andReturn(mpackChangeSummary).anyTimes();
    expect(mpackChangeSummary.getSource()).andReturn(mpack).anyTimes();
    expect(mpackChangeSummary.getTarget()).andReturn(mpack2).anyTimes();
    expect(mpack.getStackId()).andReturn(mpack211.getStackId()).anyTimes();
    expect(mpack2.getStackId()).andReturn(mpack211.getStackId()).anyTimes();
    replay(mpackChangeSummary, mpack, mpack2);
    metaInfo.getMpackManager().getMpackMap().put(1L, mpack);
    metaInfo.getMpackManager().getMpackMap().put(2L, mpack2);

    Map<String, String> commandParams = new HashMap<>();
    commandParams.put("clusterName", "c1");
    commandParams.put(CreateAndConfigureTask.PARAMETER_CONFIG_TYPE, "zoo.cfg");
    commandParams.put(CreateAndConfigureTask.PARAMETER_KEY_VALUE_PAIRS, new Gson().toJson(configurations));

    ExecutionCommand executionCommand = getExecutionCommand(commandParams);
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
    assertFalse(StringUtils.equals("version2", config.getTag()));
    assertEquals("11", config.getProperties().get("initLimit"));
  }

  /**
   * Creates a cluster using {@link #mpack211} with ZooKeeper installed.
   */
  private void makeUpgradeCluster() throws Exception {
    String clusterName = "c1";
    String hostName = "h1";

    clusters.addCluster(clusterName, mpack211.getStackId());

    Cluster c = clusters.getCluster(clusterName);

    // add a host component
    clusters.addHost(hostName);
    Host host = clusters.getHost(hostName);
    Map<String, String> hostAttributes = new HashMap<>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "6");
    host.setHostAttributes(hostAttributes);

    clusters.mapHostToCluster(hostName, clusterName);
    clusters.updateHostMappings(host);

    // !!! very important, otherwise the loops that walk the list of installed
    // service properties will not run!
    ServiceGroup serviceGroup = c.addServiceGroup("CORE", mpack211.getStackId().getStackId());
    Service zk = installService(c, serviceGroup, "ZOOKEEPER");
    addServiceComponent(zk, "ZOOKEEPER_SERVER");
    addServiceComponent(zk, "ZOOKEEPER_CLIENT");
    createNewServiceComponentHost(c, "ZOOKEEPER", "ZOOKEEPER_SERVER", hostName);
    createNewServiceComponentHost(c, "ZOOKEEPER", "ZOOKEEPER_CLIENT", hostName);

    Map<String, String> properties = new HashMap<String, String>() {
      {
        put("initLimit", "10");
      }
    };

    Config config = createConfig(c, "zoo.cfg", "version1", properties);

    c.addDesiredConfig("user", Collections.singleton(config));

    // verify that our configs are there
    String tickTime = m_configHelper.getPropertyValueFromStackDefinitions(c, "zoo.cfg", "tickTime");
    assertNotNull(tickTime);
  }

  /**
   * Installs a service in the cluster.
   */
  private Service installService(Cluster cluster, ServiceGroup serviceGroup, String serviceName)
      throws AmbariException {
    Service service;

    try {
      service = cluster.getService(serviceName);
    } catch (ServiceNotFoundException e) {
      service = serviceFactory.createNew(cluster, serviceGroup, Collections.emptyList(), serviceName, serviceName);
      cluster.addService(service);
    }

    return service;
  }

  private ServiceComponent addServiceComponent(Service service,
    String componentName
  ) throws AmbariException {
    ServiceComponent serviceComponent;
    try {
      serviceComponent = service.getServiceComponent(componentName);
    } catch (ServiceComponentNotFoundException e) {
      serviceComponent = serviceComponentFactory.createNew(service, componentName, componentName);
      service.addServiceComponent(serviceComponent);
      serviceComponent.setDesiredState(State.INSTALLED);
    }

    return serviceComponent;
  }

  private ServiceComponentHost createNewServiceComponentHost(Cluster cluster, String serviceName,
      String svcComponent, String hostName) throws AmbariException {
    Assert.assertNotNull(cluster.getConfigGroups());
    Service s = cluster.getService(serviceName);
    ServiceComponent sc = addServiceComponent(s, svcComponent);

    ServiceComponentHost sch = serviceComponentHostFactory.createNew(sc, hostName);

    sc.addServiceComponentHost(sch);
    sch.setDesiredState(State.INSTALLED);
    sch.setState(State.INSTALLED);
    return sch;
  }

  /**
   * Creates an upgrade and associates it with the cluster.
   */
  private UpgradeEntity createUpgrade(Cluster cluster, MpackEntity mpackEntity)
      throws Exception {

    // create some entities for the finalize action to work with for patch
    // history
    RequestEntity requestEntity = new RequestEntity();
    requestEntity.setClusterId(cluster.getClusterId());
    requestEntity.setRequestId(1L);
    requestEntity.setStartTime(System.currentTimeMillis());
    requestEntity.setCreateTime(System.currentTimeMillis());
    requestDAO.create(requestEntity);

    UpgradeEntity upgradeEntity = new UpgradeEntity();
    upgradeEntity.setId(1L);
    upgradeEntity.setClusterId(cluster.getClusterId());
    upgradeEntity.setRequestEntity(requestEntity);
    upgradeEntity.setUpgradeType(UpgradeType.EXPRESS);

    ServiceGroupDAO serviceGroupDAO = m_injector.getInstance(ServiceGroupDAO.class);
    for( ServiceGroup serviceGroup : cluster.getServiceGroups().values() ) {
      ServiceGroupEntity serviceGroupEntity = serviceGroupDAO.findByPK(serviceGroup.getServiceGroupId());
      UpgradeHistoryEntity history = new UpgradeHistoryEntity(upgradeEntity, serviceGroupEntity,
          mpack211, mpackEntity);

      upgradeEntity.addHistory(history);
    }

    upgradeDAO.create(upgradeEntity);
    cluster.setUpgradeEntity(upgradeEntity);
    return upgradeEntity;
  }

  private ExecutionCommand getExecutionCommand(Map<String, String> commandParams) {
    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setClusterName("c1");
    executionCommand.setCommandParams(commandParams);
    executionCommand.setRoleParams(new HashMap<>());
    executionCommand.getRoleParams().put(ServerAction.ACTION_USER_NAME, "username");

    return executionCommand;
  }

  private Config createConfig(Cluster cluster, String type, String tag,
      Map<String, String> properties) {
    return configFactory.createNew(cluster, type, tag, properties,
        NO_ATTRIBUTES);
  }
}