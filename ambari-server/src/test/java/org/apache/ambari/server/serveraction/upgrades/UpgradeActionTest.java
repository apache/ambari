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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ServiceComponentNotFoundException;
import org.apache.ambari.server.ServiceNotFoundException;
import org.apache.ambari.server.actionmanager.ExecutionCommandWrapper;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariCustomCommandExecutionHelper;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.ClusterVersionDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.ClusterVersionEntity;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigImpl;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentFactory;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.UnitOfWork;

/**
 * Tests upgrade-related server side actions
 */
public class UpgradeActionTest {
  private static final String HDP_2_1_1_0 = "2.1.1.0-118";
  private static final String HDP_2_2_1_0 = "2.2.1.0-2270";
  private static final String HDP_2_2_0_0 = "2.2.0.0-2041";
  private static final StackId HDP_21_STACK = new StackId("HDP-2.1.1");
  private static final StackId HDP_22_STACK = new StackId("HDP-2.2.0");

  private Injector m_injector;

  @Inject
  private OrmTestHelper m_helper;

  @Inject
  private RepositoryVersionDAO repoVersionDAO;

  @Inject
  private ClusterVersionDAO clusterVersionDAO;

  @Inject
  private HostVersionDAO hostVersionDAO;

  @Inject
  private HostDAO hostDAO;

  @Inject
  private HostRoleCommandFactory hostRoleCommandFactory;

  @Inject
  private ServiceFactory serviceFactory;

  @Inject
  private ServiceComponentFactory serviceComponentFactory;

  @Inject
  private ServiceComponentHostFactory serviceComponentHostFactory;

  @Before
  public void setup() throws Exception {
    m_injector = Guice.createInjector(new InMemoryDefaultTestModule());
    m_injector.getInstance(GuiceJpaInitializer.class);
    m_injector.injectMembers(this);
    m_injector.getInstance(UnitOfWork.class).begin();
  }

  @After
  public void teardown() throws Exception {
    m_injector.getInstance(UnitOfWork.class).end();
    m_injector.getInstance(PersistService.class).stop();
  }

  private void makeDowngradeCluster() throws Exception {
    String clusterName = "c1";
    String hostName = "h1";

    Clusters clusters = m_injector.getInstance(Clusters.class);
    clusters.addCluster(clusterName, HDP_21_STACK);

    Cluster c = clusters.getCluster(clusterName);

    // add a host component
    clusters.addHost(hostName);

    Host host = clusters.getHost(hostName);

    Map<String, String> hostAttributes = new HashMap<String, String>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "6");
    host.setHostAttributes(hostAttributes);
    host.persist();

    m_helper.getOrCreateRepositoryVersion(HDP_21_STACK, HDP_2_2_0_0);
    m_helper.getOrCreateRepositoryVersion(HDP_21_STACK, HDP_2_2_1_0);

    c.createClusterVersion(HDP_21_STACK, HDP_2_2_0_0, "admin", RepositoryVersionState.UPGRADING);
    c.createClusterVersion(HDP_21_STACK, HDP_2_2_1_0, "admin", RepositoryVersionState.INSTALLING);

    c.transitionClusterVersion(HDP_21_STACK, HDP_2_2_0_0, RepositoryVersionState.CURRENT);
    c.transitionClusterVersion(HDP_21_STACK, HDP_2_2_1_0, RepositoryVersionState.INSTALLED);
    c.transitionClusterVersion(HDP_21_STACK, HDP_2_2_1_0, RepositoryVersionState.UPGRADING);

    c.mapHostVersions(Collections.singleton(hostName), c.getCurrentClusterVersion(),
        RepositoryVersionState.CURRENT);

    HostVersionEntity entity = new HostVersionEntity();
    entity.setHostEntity(hostDAO.findByName(hostName));
    entity.setRepositoryVersion(repoVersionDAO.findByStackAndVersion(HDP_21_STACK, HDP_2_2_1_0));
    entity.setState(RepositoryVersionState.UPGRADING);
    hostVersionDAO.create(entity);
  }

  private void makeUpgradeCluster() throws Exception {
    String clusterName = "c1";
    String hostName = "h1";

    Clusters clusters = m_injector.getInstance(Clusters.class);
    clusters.addCluster(clusterName, HDP_21_STACK);

    StackDAO stackDAO = m_injector.getInstance(StackDAO.class);
    StackEntity stackEntity = stackDAO.find(HDP_21_STACK.getStackName(),
        HDP_21_STACK.getStackVersion());

    assertNotNull(stackEntity);

    Cluster c = clusters.getCluster(clusterName);
    c.setDesiredStackVersion(HDP_21_STACK);

    // add a host component
    clusters.addHost(hostName);

    Host host = clusters.getHost(hostName);

    Map<String, String> hostAttributes = new HashMap<String, String>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "6");
    host.setHostAttributes(hostAttributes);
    host.persist();

    String urlInfo = "[{'repositories':["
        + "{'Repositories/base_url':'http://foo1','Repositories/repo_name':'HDP','Repositories/repo_id':'HDP-2.1.1'}"
        + "], 'OperatingSystems/os_type':'redhat6'}]";

    m_helper.getOrCreateRepositoryVersion(HDP_21_STACK, HDP_2_2_0_0);
    repoVersionDAO.create(stackEntity, HDP_2_2_1_0, String.valueOf(System.currentTimeMillis()),
        "pack", urlInfo);

    c.createClusterVersion(HDP_21_STACK, HDP_2_2_0_0, "admin", RepositoryVersionState.UPGRADING);
    c.createClusterVersion(HDP_21_STACK, HDP_2_2_1_0, "admin", RepositoryVersionState.INSTALLING);

    c.transitionClusterVersion(HDP_21_STACK, HDP_2_2_0_0, RepositoryVersionState.CURRENT);
    c.transitionClusterVersion(HDP_21_STACK, HDP_2_2_1_0, RepositoryVersionState.INSTALLED);
    c.transitionClusterVersion(HDP_21_STACK, HDP_2_2_1_0, RepositoryVersionState.UPGRADING);
    c.transitionClusterVersion(HDP_21_STACK, HDP_2_2_1_0, RepositoryVersionState.UPGRADED);
    c.setCurrentStackVersion(HDP_21_STACK);

    c.mapHostVersions(Collections.singleton(hostName), c.getCurrentClusterVersion(),
        RepositoryVersionState.CURRENT);

    HostDAO hostDAO = m_injector.getInstance(HostDAO.class);

    HostVersionEntity entity = new HostVersionEntity();
    entity.setHostEntity(hostDAO.findByName(hostName));
    entity.setRepositoryVersion(repoVersionDAO.findByStackAndVersion(HDP_21_STACK, HDP_2_2_1_0));
    entity.setState(RepositoryVersionState.UPGRADED);
    hostVersionDAO.create(entity);
  }

  private void makeCrossStackUpgradeCluster() throws Exception {
    String clusterName = "c1";
    String hostName = "h1";

    Clusters clusters = m_injector.getInstance(Clusters.class);
    clusters.addCluster(clusterName, HDP_21_STACK);

    StackDAO stackDAO = m_injector.getInstance(StackDAO.class);
    StackEntity stackEntity = stackDAO.find(HDP_21_STACK.getStackName(),
        HDP_21_STACK.getStackVersion());

    assertNotNull(stackEntity);

    Cluster c = clusters.getCluster(clusterName);
    c.setCurrentStackVersion(HDP_21_STACK);
    c.setDesiredStackVersion(HDP_21_STACK);

    // add a host component
    clusters.addHost(hostName);
    Host host = clusters.getHost(hostName);

    Map<String, String> hostAttributes = new HashMap<String, String>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "6");
    host.setHostAttributes(hostAttributes);
    host.persist();

    clusters.mapHostToCluster(hostName, clusterName);

    String urlInfo = "[{'repositories':["
        + "{'Repositories/base_url':'http://foo1','Repositories/repo_name':'HDP','Repositories/repo_id':'HDP-2.1.1'}"
        + "], 'OperatingSystems/os_type':'redhat6'}]";

    m_helper.getOrCreateRepositoryVersion(HDP_21_STACK, HDP_2_1_1_0);
    m_helper.getOrCreateRepositoryVersion(HDP_22_STACK, HDP_2_2_1_0);

    repoVersionDAO.create(stackEntity, HDP_2_2_1_0, String.valueOf(System.currentTimeMillis()),
        "pack", urlInfo);

    c.createClusterVersion(HDP_21_STACK, HDP_2_1_1_0, "admin", RepositoryVersionState.UPGRADING);
    c.createClusterVersion(HDP_22_STACK, HDP_2_2_1_0, "admin", RepositoryVersionState.INSTALLING);

    c.transitionClusterVersion(HDP_21_STACK, HDP_2_1_1_0, RepositoryVersionState.CURRENT);
    c.transitionClusterVersion(HDP_22_STACK, HDP_2_2_1_0, RepositoryVersionState.INSTALLED);
    c.transitionClusterVersion(HDP_22_STACK, HDP_2_2_1_0, RepositoryVersionState.UPGRADING);
    c.transitionClusterVersion(HDP_22_STACK, HDP_2_2_1_0, RepositoryVersionState.UPGRADED);

    c.mapHostVersions(Collections.singleton(hostName), c.getCurrentClusterVersion(),
        RepositoryVersionState.CURRENT);

    HostDAO hostDAO = m_injector.getInstance(HostDAO.class);

    HostVersionEntity entity = new HostVersionEntity();
    entity.setHostEntity(hostDAO.findByName(hostName));
    entity.setRepositoryVersion(repoVersionDAO.findByStackAndVersion(HDP_22_STACK, HDP_2_2_1_0));
    entity.setState(RepositoryVersionState.UPGRADED);
    hostVersionDAO.create(entity);
  }

  @Test
  public void testFinalizeDowngrade() throws Exception {
    makeDowngradeCluster();

    Map<String, String> commandParams = new HashMap<String, String>();
    commandParams.put(FinalizeUpgradeAction.UPGRADE_DIRECTION_KEY, "downgrade");
    commandParams.put(FinalizeUpgradeAction.VERSION_KEY, HDP_2_2_0_0);

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName("c1");

    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null, null, null);
    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(executionCommand));

    FinalizeUpgradeAction action = m_injector.getInstance(FinalizeUpgradeAction.class);
    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    CommandReport report = action.execute(null);
    assertNotNull(report);
    assertEquals(HostRoleStatus.COMPLETED.name(), report.getStatus());

    for (HostVersionEntity entity : hostVersionDAO.findByClusterAndHost("c1", "h1")) {
      if (entity.getRepositoryVersion().getVersion().equals(HDP_2_2_0_0)) {
        assertEquals(RepositoryVersionState.CURRENT, entity.getState());
      } else if (entity.getRepositoryVersion().getVersion().equals(HDP_2_2_1_0)) {
        assertEquals(RepositoryVersionState.INSTALLED, entity.getState());
      }
    }

    for (ClusterVersionEntity entity : clusterVersionDAO.findByCluster("c1")) {
      if (entity.getRepositoryVersion().getVersion().equals(HDP_2_2_0_0)) {
        assertEquals(RepositoryVersionState.CURRENT, entity.getState());
      } else if (entity.getRepositoryVersion().getVersion().equals(HDP_2_2_1_0)) {
        assertEquals(RepositoryVersionState.INSTALLED, entity.getState());
      }
    }
  }

  @Test
  public void testFinalizeUpgrade() throws Exception {
    makeUpgradeCluster();

    Map<String, String> commandParams = new HashMap<String, String>();
    commandParams.put(FinalizeUpgradeAction.UPGRADE_DIRECTION_KEY, "upgrade");
    commandParams.put(FinalizeUpgradeAction.VERSION_KEY, HDP_2_2_1_0);

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName("c1");

    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null, null, null);
    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(executionCommand));

    FinalizeUpgradeAction action = m_injector.getInstance(FinalizeUpgradeAction.class);
    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    CommandReport report = action.execute(null);
    assertNotNull(report);
    assertEquals(HostRoleStatus.COMPLETED.name(), report.getStatus());

    // !!! verify the metainfo url has not been updated, but an output command
    // has
    AmbariMetaInfo metaInfo = m_injector.getInstance(AmbariMetaInfo.class);
    RepositoryInfo repo = metaInfo.getRepository("HDP", "2.1.1", "redhat6", "HDP-2.1.1");
    assertEquals("http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/BUILDS/2.1.1.0-118",
        repo.getBaseUrl());

    // !!! verify that a command will return the correct host info
    AmbariCustomCommandExecutionHelper helper = m_injector.getInstance(AmbariCustomCommandExecutionHelper.class);
    Clusters clusters = m_injector.getInstance(Clusters.class);
    Host host = clusters.getHost("h1");
    Cluster cluster = clusters.getCluster("c1");

    String repoInfo = helper.getRepoInfo(cluster, host);

    Gson gson = new Gson();

    JsonElement element = gson.fromJson(repoInfo, JsonElement.class);
    assertTrue(element.isJsonArray());

    JsonArray list = JsonArray.class.cast(element);
    assertEquals(1, list.size());

    JsonObject o = list.get(0).getAsJsonObject();
    assertTrue(o.has("baseUrl"));
    assertEquals("http://foo1", o.get("baseUrl").getAsString());
  }

  @Test
  public void testFinalizeUpgradeAcrossStacks() throws Exception {
    makeCrossStackUpgradeCluster();

    Clusters clusters = m_injector.getInstance(Clusters.class);
    Cluster cluster = clusters.getCluster("c1");

    // setup the cluster for the upgrade across stacks
    cluster.setCurrentStackVersion(HDP_21_STACK);
    cluster.setDesiredStackVersion(HDP_22_STACK);

    Map<String, String> commandParams = new HashMap<String, String>();
    commandParams.put(FinalizeUpgradeAction.UPGRADE_DIRECTION_KEY, "upgrade");
    commandParams.put(FinalizeUpgradeAction.VERSION_KEY, HDP_2_2_1_0);
    commandParams.put(FinalizeUpgradeAction.ORIGINAL_STACK_KEY, HDP_21_STACK.getStackId());
    commandParams.put(FinalizeUpgradeAction.TARGET_STACK_KEY, HDP_22_STACK.getStackId());

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName("c1");

    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null, null, null);

    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(executionCommand));

    FinalizeUpgradeAction action = m_injector.getInstance(FinalizeUpgradeAction.class);
    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    CommandReport report = action.execute(null);
    assertNotNull(report);
    assertEquals(HostRoleStatus.COMPLETED.name(), report.getStatus());

    StackId currentStackId = cluster.getCurrentStackVersion();
    StackId desiredStackId = cluster.getDesiredStackVersion();

    // verify current/desired stacks are updated to the new stack
    assertEquals(desiredStackId, currentStackId);
    assertEquals(HDP_22_STACK, currentStackId);
    assertEquals(HDP_22_STACK, desiredStackId);
  }

  /**
   * Tests some of the action items are completed when finalizing downgrade
   * across stacks (HDP 2.2 -> HDP 2.3).
   *
   * @throws Exception
   */
  @Test
  public void testFinalizeDowngradeAcrossStacks() throws Exception {
    makeCrossStackUpgradeCluster();

    Clusters clusters = m_injector.getInstance(Clusters.class);
    Cluster cluster = clusters.getCluster("c1");

    // install HDFS with some components
    Service service = installService(cluster, "HDFS");
    addServiceComponent(cluster, service, "NAMENODE");
    addServiceComponent(cluster, service, "DATANODE");
    createNewServiceComponentHost(cluster, "HDFS", "NAMENODE", "h1");
    createNewServiceComponentHost(cluster, "HDFS", "DATANODE", "h1");

    // create some configs
    createConfigs(cluster);

    // setup the cluster for the upgrade across stacks
    cluster.setCurrentStackVersion(HDP_21_STACK);
    cluster.setDesiredStackVersion(HDP_22_STACK);

    // now that the desired version is set, we can create some new configs in
    // the new stack version
    createConfigs(cluster);

    // verify we have configs in both HDP stacks
    cluster = clusters.getCluster("c1");
    Collection<Config> configs = cluster.getAllConfigs();
    assertEquals(6, configs.size());

    Map<String, String> commandParams = new HashMap<String, String>();
    commandParams.put(FinalizeUpgradeAction.UPGRADE_DIRECTION_KEY, "downgrade");
    commandParams.put(FinalizeUpgradeAction.VERSION_KEY, HDP_2_1_1_0);
    commandParams.put(FinalizeUpgradeAction.ORIGINAL_STACK_KEY, HDP_21_STACK.getStackId());
    commandParams.put(FinalizeUpgradeAction.TARGET_STACK_KEY, HDP_22_STACK.getStackId());

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName("c1");

    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null, null, null);

    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(executionCommand));


    HostVersionDAO dao = m_injector.getInstance(HostVersionDAO.class);

    List<HostVersionEntity> hosts = dao.findByClusterStackAndVersion(
        "c1", HDP_22_STACK, HDP_2_2_1_0);
    assertFalse(hosts.isEmpty());
    for (HostVersionEntity hve : hosts) {
      assertFalse(hve.getState() == RepositoryVersionState.INSTALLED);
    }

    FinalizeUpgradeAction action = m_injector.getInstance(FinalizeUpgradeAction.class);
    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    CommandReport report = action.execute(null);
    assertNotNull(report);
    assertEquals(HostRoleStatus.COMPLETED.name(), report.getStatus());

    StackId currentStackId = cluster.getCurrentStackVersion();
    StackId desiredStackId = cluster.getDesiredStackVersion();

    // verify current/desired stacks are back to normal
    assertEquals(desiredStackId, currentStackId);
    assertEquals(HDP_21_STACK, currentStackId);
    assertEquals(HDP_21_STACK, desiredStackId);

    // verify we have configs in only 1 stack
    cluster = clusters.getCluster("c1");
    configs = cluster.getAllConfigs();
    assertEquals(3, configs.size());

    hosts = dao.findByClusterStackAndVersion(
        "c1", HDP_22_STACK, HDP_2_2_1_0);

    hosts = dao.findByClusterStackAndVersion("c1", HDP_22_STACK, HDP_2_2_1_0);
    assertFalse(hosts.isEmpty());
    for (HostVersionEntity hve : hosts) {
      assertTrue(hve.getState() == RepositoryVersionState.INSTALLED);
    }

  }

  private ServiceComponentHost createNewServiceComponentHost(Cluster cluster, String svc,
      String svcComponent, String hostName) throws AmbariException {
    Assert.assertNotNull(cluster.getConfigGroups());
    Service s = installService(cluster, svc);
    ServiceComponent sc = addServiceComponent(cluster, s, svcComponent);

    ServiceComponentHost sch = serviceComponentHostFactory.createNew(sc, hostName);

    sc.addServiceComponentHost(sch);
    sch.setDesiredState(State.INSTALLED);
    sch.setState(State.INSTALLED);
    sch.setDesiredStackVersion(cluster.getDesiredStackVersion());
    sch.setStackVersion(cluster.getCurrentStackVersion());

    sch.persist();
    return sch;
  }

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

  private ServiceComponent addServiceComponent(Cluster cluster, Service service,
      String componentName) throws AmbariException {
    ServiceComponent serviceComponent = null;
    try {
      serviceComponent = service.getServiceComponent(componentName);
    } catch (ServiceComponentNotFoundException e) {
      serviceComponent = serviceComponentFactory.createNew(service, componentName);
      service.addServiceComponent(serviceComponent);
      serviceComponent.setDesiredState(State.INSTALLED);
      serviceComponent.persist();
    }

    return serviceComponent;
  }

  private void createConfigs(Cluster cluster) {
    Map<String, String> properties = new HashMap<String, String>();
    Map<String, Map<String, String>> propertiesAttributes = new HashMap<String, Map<String, String>>();
    properties.put("a", "a1");
    properties.put("b", "b1");

    Config c1 = new ConfigImpl(cluster, "hdfs-site", properties, propertiesAttributes, m_injector);
    properties.put("c", "c1");
    properties.put("d", "d1");

    Config c2 = new ConfigImpl(cluster, "core-site", properties, propertiesAttributes, m_injector);
    Config c3 = new ConfigImpl(cluster, "foo-site", properties, propertiesAttributes, m_injector);

    cluster.addConfig(c1);
    cluster.addConfig(c2);
    cluster.addConfig(c3);
    c1.persist();
    c2.persist();
    c3.persist();
  }
}
