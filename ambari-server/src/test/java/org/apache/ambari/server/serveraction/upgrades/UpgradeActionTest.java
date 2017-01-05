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

import java.lang.reflect.Field;
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
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.ServiceConfigVersionResponse;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.ClusterVersionDAO;
import org.apache.ambari.server.orm.dao.HostComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.HostComponentStateDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.RequestDAO;
import org.apache.ambari.server.orm.dao.ServiceComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.dao.UpgradeDAO;
import org.apache.ambari.server.orm.entities.ClusterVersionEntity;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.RequestEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentHistoryEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.serveraction.ServerAction;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
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
import org.apache.ambari.server.state.stack.UpgradePack;
import org.apache.ambari.server.state.stack.upgrade.Direction;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
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
  private static final String clusterName = "c1";

  private static final String HDP_2_1_1_0 = "2.1.1.0-1";
  private static final String HDP_2_1_1_1 = "2.1.1.1-2";

  private static final String HDP_2_2_0_1 = "2.2.0.1-3";
  private static final String HDP_2_2_0_2 = "2.2.0.2-4";

  private static final StackId HDP_21_STACK = new StackId("HDP-2.1.1");
  private static final StackId HDP_22_STACK = new StackId("HDP-2.2.0");

  private static final String HDP_211_CENTOS6_REPO_URL = "http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/BUILDS/2.1.1.0-118";

  private Injector m_injector;

  private AmbariManagementController amc;
  @Inject
  private OrmTestHelper m_helper;
  @Inject
  private RepositoryVersionDAO repoVersionDAO;
  @Inject
  private Clusters clusters;
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
  @Inject
  private RequestDAO requestDAO;
  @Inject
  private UpgradeDAO upgradeDAO;
  @Inject
  private ServiceComponentDesiredStateDAO serviceComponentDesiredStateDAO;
  @Inject
  private HostComponentDesiredStateDAO hostComponentDesiredStateDAO;
  @Inject
  private HostComponentStateDAO hostComponentStateDAO;
  @Inject
  private StackDAO stackDAO;
  @Inject
  private AmbariMetaInfo ambariMetaInfo;
  @Inject
  private FinalizeUpgradeAction finalizeUpgradeAction;
  @Inject
  private ConfigFactory configFactory;

  @Before
  public void setup() throws Exception {
    m_injector = Guice.createInjector(new InMemoryDefaultTestModule());
    m_injector.getInstance(GuiceJpaInitializer.class);
    m_injector.injectMembers(this);
    m_injector.getInstance(UnitOfWork.class).begin();

    // Initialize AmbariManagementController
    amc = m_injector.getInstance(AmbariManagementController.class);

    Field field = AmbariServer.class.getDeclaredField("clusterController");
    field.setAccessible(true);
    field.set(null, amc);
  }

  @After
  public void teardown() throws Exception {
    m_injector.getInstance(UnitOfWork.class).end();
    m_injector.getInstance(PersistService.class).stop();
  }

  private void makeDowngradeCluster(StackId sourceStack, String sourceRepo, StackId targetStack, String targetRepo) throws Exception {
    String hostName = "h1";

    clusters.addCluster(clusterName, sourceStack);

    Cluster c = clusters.getCluster(clusterName);

    // add a host component
    clusters.addHost(hostName);

    Host host = clusters.getHost(hostName);

    Map<String, String> hostAttributes = new HashMap<String, String>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "6");
    host.setHostAttributes(hostAttributes);

    // Create the starting repo version
    m_helper.getOrCreateRepositoryVersion(sourceStack, sourceRepo);
    c.createClusterVersion(sourceStack, sourceRepo, "admin", RepositoryVersionState.INSTALLING);
    c.transitionClusterVersion(sourceStack, sourceRepo, RepositoryVersionState.CURRENT);

    // Start upgrading the newer repo
    m_helper.getOrCreateRepositoryVersion(targetStack, targetRepo);
    c.createClusterVersion(targetStack, targetRepo, "admin", RepositoryVersionState.INSTALLING);
    c.transitionClusterVersion(targetStack, targetRepo, RepositoryVersionState.INSTALLED);

    c.mapHostVersions(Collections.singleton(hostName), c.getCurrentClusterVersion(),
            RepositoryVersionState.CURRENT);

    HostVersionEntity entity = new HostVersionEntity();
    entity.setHostEntity(hostDAO.findByName(hostName));
    entity.setRepositoryVersion(repoVersionDAO.findByStackAndVersion(targetStack, targetRepo));
    entity.setState(RepositoryVersionState.INSTALLING);
    hostVersionDAO.create(entity);
  }

  private void makeTwoUpgradesWhereLastDidNotComplete(StackId sourceStack, String sourceRepo, StackId midStack, String midRepo, StackId targetStack, String targetRepo) throws Exception {
    String hostName = "h1";

    clusters.addCluster(clusterName, sourceStack);

    Cluster c = clusters.getCluster(clusterName);

    // add a host component
    clusters.addHost(hostName);

    Host host = clusters.getHost(hostName);

    Map<String, String> hostAttributes = new HashMap<String, String>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "6");
    host.setHostAttributes(hostAttributes);

    // Create the starting repo version
    m_helper.getOrCreateRepositoryVersion(sourceStack, sourceRepo);
    c.createClusterVersion(sourceStack, sourceRepo, "admin", RepositoryVersionState.INSTALLING);
    c.transitionClusterVersion(sourceStack, sourceRepo, RepositoryVersionState.CURRENT);

    // Start upgrading the mid repo
    m_helper.getOrCreateRepositoryVersion(midStack, midRepo);
    c.setDesiredStackVersion(midStack);
    c.createClusterVersion(midStack, midRepo, "admin", RepositoryVersionState.INSTALLING);
    c.transitionClusterVersion(midStack, midRepo, RepositoryVersionState.INSTALLED);
    c.transitionClusterVersion(midStack, midRepo, RepositoryVersionState.CURRENT);

    // Set original version as INSTALLED
    c.transitionClusterVersion(sourceStack, sourceRepo, RepositoryVersionState.INSTALLED);

    // Notice that we have not yet changed the cluster current stack to the mid stack to simulate
    // the user skipping this step.

    m_helper.getOrCreateRepositoryVersion(targetStack, targetRepo);
    c.setDesiredStackVersion(targetStack);
    c.createClusterVersion(targetStack, targetRepo, "admin", RepositoryVersionState.INSTALLING);
    c.transitionClusterVersion(targetStack, targetRepo, RepositoryVersionState.INSTALLED);

    // Create a host version for the starting repo in INSTALLED
    HostVersionEntity entitySource = new HostVersionEntity();
    entitySource.setHostEntity(hostDAO.findByName(hostName));
    entitySource.setRepositoryVersion(repoVersionDAO.findByStackAndVersion(sourceStack, sourceRepo));
    entitySource.setState(RepositoryVersionState.INSTALL_FAILED);
    hostVersionDAO.create(entitySource);

    // Create a host version for the mid repo in CURRENT
    c.mapHostVersions(Collections.singleton(hostName), c.getCurrentClusterVersion(),
            RepositoryVersionState.CURRENT);

    // Create a host version for the target repo in UPGRADED
    HostVersionEntity entityTarget = new HostVersionEntity();
    entityTarget.setHostEntity(hostDAO.findByName(hostName));
    entityTarget.setRepositoryVersion(repoVersionDAO.findByStackAndVersion(targetStack, targetRepo));
    entityTarget.setState(RepositoryVersionState.INSTALLED);
    hostVersionDAO.create(entityTarget);
  }

  private void makeUpgradeCluster(StackId sourceStack, String sourceRepo, StackId targetStack, String targetRepo) throws Exception {
    String hostName = "h1";

    clusters.addCluster(clusterName, sourceStack);

    StackEntity stackEntitySource = stackDAO.find(sourceStack.getStackName(), sourceStack.getStackVersion());
    StackEntity stackEntityTarget = stackDAO.find(targetStack.getStackName(), targetStack.getStackVersion());
    assertNotNull(stackEntitySource);
    assertNotNull(stackEntityTarget);

    Cluster c = clusters.getCluster(clusterName);
    c.setDesiredStackVersion(sourceStack);

    // add a host component
    clusters.addHost(hostName);

    Host host = clusters.getHost(hostName);

    Map<String, String> hostAttributes = new HashMap<String, String>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "6");
    host.setHostAttributes(hostAttributes);

    // without this, HostEntity will not have a relation to ClusterEntity
    clusters.mapHostToCluster(hostName, clusterName);

    // Create the starting repo version
    RepositoryVersionEntity repoEntity = m_helper.getOrCreateRepositoryVersion(sourceStack, sourceRepo);
    repoEntity.setOperatingSystems("[\n" +
            "   {\n" +
            "      \"repositories\":[\n" +
            "         {\n" +
            "            \"Repositories/base_url\":\"http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos5/2.x/updates/2.2.0.0\",\n" +
            "            \"Repositories/repo_name\":\"HDP\",\n" +
            "            \"Repositories/repo_id\":\"HDP-2.2\"\n" +
            "         }\n" +
            "      ],\n" +
            "      \"OperatingSystems/os_type\":\"redhat6\"\n" +
            "   }\n" +
            "]");
    repoVersionDAO.merge(repoEntity);

    c.createClusterVersion(sourceStack, sourceRepo, "admin", RepositoryVersionState.INSTALLING);
    c.transitionClusterVersion(sourceStack, sourceRepo, RepositoryVersionState.CURRENT);

    // Create the new repo version
    String urlInfo = "[{'repositories':["
            + "{'Repositories/base_url':'http://foo1','Repositories/repo_name':'HDP','Repositories/repo_id':'" + targetStack.getStackId() + "'}"
            + "], 'OperatingSystems/os_type':'redhat6'}]";

    repoVersionDAO.create(stackEntityTarget, targetRepo, String.valueOf(System.currentTimeMillis()), urlInfo);

    // Start upgrading the newer repo
    c.createClusterVersion(targetStack, targetRepo, "admin", RepositoryVersionState.INSTALLING);
    c.transitionClusterVersion(targetStack, targetRepo, RepositoryVersionState.INSTALLED);
    c.setCurrentStackVersion(targetStack);

    c.mapHostVersions(Collections.singleton(hostName), c.getCurrentClusterVersion(),
            RepositoryVersionState.CURRENT);

    // create a single host with the UPGRADED HostVersionEntity
    HostDAO hostDAO = m_injector.getInstance(HostDAO.class);

    RepositoryVersionEntity repositoryVersionEntity = repoVersionDAO.findByStackAndVersion(
            targetStack, targetRepo);

    HostVersionEntity entity = new HostVersionEntity(hostDAO.findByName(hostName),
            repositoryVersionEntity, RepositoryVersionState.INSTALLED);

    hostVersionDAO.create(entity);

    // verify the UPGRADED host versions were created successfully
    List<HostVersionEntity> hostVersions = hostVersionDAO.findByClusterStackAndVersion(clusterName,
            targetStack, targetRepo);

    assertEquals(1, hostVersions.size());
    assertEquals(RepositoryVersionState.INSTALLED, hostVersions.get(0).getState());
  }

  private void makeCrossStackUpgradeCluster(StackId sourceStack, String sourceRepo, StackId targetStack, String targetRepo) throws Exception {
    String hostName = "h1";

    clusters.addCluster(clusterName, sourceStack);

    StackEntity stackEntitySource = stackDAO.find(sourceStack.getStackName(), sourceStack.getStackVersion());
    StackEntity stackEntityTarget = stackDAO.find(targetStack.getStackName(), targetStack.getStackVersion());

    assertNotNull(stackEntitySource);
    assertNotNull(stackEntityTarget);

    Cluster c = clusters.getCluster(clusterName);
    c.setCurrentStackVersion(sourceStack);
    c.setDesiredStackVersion(sourceStack);

    // add a host component
    clusters.addHost(hostName);
    Host host = clusters.getHost(hostName);

    Map<String, String> hostAttributes = new HashMap<String, String>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "6");
    host.setHostAttributes(hostAttributes);

    clusters.mapHostToCluster(hostName, clusterName);

    // Create the starting repo version
    m_helper.getOrCreateRepositoryVersion(sourceStack, sourceRepo);
    c.createClusterVersion(sourceStack, sourceRepo, "admin", RepositoryVersionState.INSTALLING);
    c.transitionClusterVersion(sourceStack, sourceRepo, RepositoryVersionState.CURRENT);

    // Create the new repo version
    String urlInfo = "[{'repositories':["
            + "{'Repositories/base_url':'http://foo1','Repositories/repo_name':'HDP','Repositories/repo_id':'" + targetRepo + "'}"
            + "], 'OperatingSystems/os_type':'redhat6'}]";
    repoVersionDAO.create(stackEntityTarget, targetRepo, String.valueOf(System.currentTimeMillis()), urlInfo);

    // Start upgrading the newer repo
    c.createClusterVersion(targetStack, targetRepo, "admin", RepositoryVersionState.INSTALLING);
    c.transitionClusterVersion(targetStack, targetRepo, RepositoryVersionState.INSTALLED);

    c.mapHostVersions(Collections.singleton(hostName), c.getCurrentClusterVersion(),
            RepositoryVersionState.CURRENT);

    HostDAO hostDAO = m_injector.getInstance(HostDAO.class);

    HostVersionEntity entity = new HostVersionEntity();
    entity.setHostEntity(hostDAO.findByName(hostName));
    entity.setRepositoryVersion(repoVersionDAO.findByStackAndVersion(targetStack, targetRepo));
    entity.setState(RepositoryVersionState.INSTALLED);
    hostVersionDAO.create(entity);
  }

  /***
   * During an Express Upgrade that crosses a stack version, Ambari calls UpdateDesiredStackAction
   * in order to change the stack and apply configs.
   * The configs that are applied must be saved with the username that is passed in the role params.
   */
  @Test
  public void testExpressUpgradeUpdateDesiredStackAction() throws Exception {
    StackId sourceStack = HDP_21_STACK;
    StackId targetStack = HDP_22_STACK;
    String sourceRepo = HDP_2_1_1_0;
    String targetRepo = HDP_2_2_0_1;

    // Must be a NON_ROLLING upgrade that jumps stacks in order for it to apply config changes.
    // That upgrade pack has changes for ZK and NameNode.
    String upgradePackName = "upgrade_nonrolling_new_stack";

    Map<String, UpgradePack> packs = ambariMetaInfo.getUpgradePacks(sourceStack.getStackName(), sourceStack.getStackVersion());
    Assert.assertTrue(packs.containsKey(upgradePackName));

    makeCrossStackUpgradeCluster(sourceStack, sourceRepo, targetStack, targetRepo);

    RepositoryVersionEntity targetRve = repoVersionDAO.findByStackNameAndVersion("HDP", targetRepo);
    Assert.assertNotNull(targetRve);

    Cluster cluster = clusters.getCluster(clusterName);

    // Install ZK and HDFS with some components
    Service zk = installService(cluster, "ZOOKEEPER");
    addServiceComponent(cluster, zk, "ZOOKEEPER_SERVER");
    addServiceComponent(cluster, zk, "ZOOKEEPER_CLIENT");
    createNewServiceComponentHost(cluster, "ZOOKEEPER", "ZOOKEEPER_SERVER", "h1");
    createNewServiceComponentHost(cluster, "ZOOKEEPER", "ZOOKEEPER_CLIENT", "h1");

    Service hdfs = installService(cluster, "HDFS");
    addServiceComponent(cluster, hdfs, "NAMENODE");
    addServiceComponent(cluster, hdfs, "DATANODE");
    createNewServiceComponentHost(cluster, "HDFS", "NAMENODE", "h1");
    createNewServiceComponentHost(cluster, "HDFS", "DATANODE", "h1");

    // Create some configs
    createConfigs(cluster);
    Collection<Config> configs = cluster.getAllConfigs();
    Assert.assertFalse(configs.isEmpty());

    Map<String, String> commandParams = new HashMap<String, String>();
    commandParams.put(UpdateDesiredStackAction.COMMAND_PARAM_ORIGINAL_STACK, sourceStack.getStackId());
    commandParams.put(UpdateDesiredStackAction.COMMAND_PARAM_TARGET_STACK, targetStack.getStackId());
    commandParams.put(UpdateDesiredStackAction.COMMAND_PARAM_DIRECTION, Direction.UPGRADE.toString());
    commandParams.put(UpdateDesiredStackAction.COMMAND_PARAM_VERSION, targetRepo);
    commandParams.put(UpdateDesiredStackAction.COMMAND_PARAM_UPGRADE_PACK, upgradePackName);

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    Map<String, String> roleParams = new HashMap<>();

    // User that is performing the config changes
    String userName = "admin";
    roleParams.put(ServerAction.ACTION_USER_NAME, userName);
    executionCommand.setRoleParams(roleParams);
    executionCommand.setClusterName(clusterName);

    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null, null, null);
    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(executionCommand));

    // Call the action to change the desired stack and apply the configs from the Config Pack called by the Upgrade Pack.
    UpdateDesiredStackAction action = m_injector.getInstance(UpdateDesiredStackAction.class);
    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    List<ServiceConfigVersionResponse> configVersionsBefore = cluster.getServiceConfigVersions();

    CommandReport report = action.execute(null);
    assertNotNull(report);
    assertEquals(HostRoleStatus.COMPLETED.name(), report.getStatus());

    List<ServiceConfigVersionResponse> configVersionsAfter = cluster.getServiceConfigVersions();
    Assert.assertFalse(configVersionsAfter.isEmpty());

    assertTrue(configVersionsAfter.size() - configVersionsBefore.size() >= 1);
  }

  @Test
  public void testFinalizeDowngrade() throws Exception {
    StackId sourceStack = HDP_21_STACK;
    StackId targetStack = HDP_21_STACK;
    String sourceRepo = HDP_2_1_1_0;
    String targetRepo = HDP_2_1_1_1;

    makeDowngradeCluster(sourceStack, sourceRepo, targetStack, targetRepo);

    Map<String, String> commandParams = new HashMap<String, String>();
    commandParams.put(FinalizeUpgradeAction.UPGRADE_DIRECTION_KEY, "downgrade");
    commandParams.put(FinalizeUpgradeAction.VERSION_KEY, sourceRepo);
    commandParams.put(FinalizeUpgradeAction.ORIGINAL_STACK_KEY, sourceStack.getStackId());
    commandParams.put(FinalizeUpgradeAction.TARGET_STACK_KEY, targetStack.getStackId());

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName(clusterName);

    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null, null, null);
    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(executionCommand));

    finalizeUpgradeAction.setExecutionCommand(executionCommand);
    finalizeUpgradeAction.setHostRoleCommand(hostRoleCommand);

    CommandReport report = finalizeUpgradeAction.execute(null);
    assertNotNull(report);
    assertEquals(HostRoleStatus.COMPLETED.name(), report.getStatus());

    for (HostVersionEntity entity : hostVersionDAO.findByClusterAndHost(clusterName, "h1")) {
      if (entity.getRepositoryVersion().getVersion().equals(sourceRepo)) {
        assertEquals(RepositoryVersionState.CURRENT, entity.getState());
      } else if (entity.getRepositoryVersion().getVersion().equals(targetRepo)) {
        assertEquals(RepositoryVersionState.INSTALLED, entity.getState());
      }
    }

    for (ClusterVersionEntity entity : clusterVersionDAO.findByCluster(clusterName)) {
      if (entity.getRepositoryVersion().getVersion().equals(sourceRepo)) {
        assertEquals(RepositoryVersionState.CURRENT, entity.getState());
      } else if (entity.getRepositoryVersion().getVersion().equals(targetRepo)) {
        assertEquals(RepositoryVersionState.INSTALLED, entity.getState());
      }
    }
  }

  /**
   * Test a case in which a customer performs an upgrade from HDP 2.1 to 2.2 (e.g., 2.2.0.0), but skips the step to
   * finalize, which calls "Save DB State". Therefore, the cluster's current stack is still on HDP 2.1.
   * They can still modify the database manually to mark HDP 2.2 as CURRENT in the cluster_version and then begin
   * another upgrade to 2.2.0.2 and then downgrade.
   * In the downgrade, the original stack is still 2.1 but the stack for the version marked as CURRENT is 2.2; this
   * mismatch means that the downgrade should not delete configs and will report a warning.
   * @throws Exception
   */
  @Test
  public void testFinalizeDowngradeWhenDidNotFinalizePreviousUpgrade() throws Exception {
    StackId sourceStack = HDP_21_STACK;
    StackId midStack = HDP_22_STACK;
    StackId targetStack = HDP_22_STACK;

    String sourceRepo = HDP_2_1_1_0;
    String midRepo = HDP_2_2_0_1;
    String targetRepo = HDP_2_2_0_2;

    makeTwoUpgradesWhereLastDidNotComplete(sourceStack, sourceRepo, midStack, midRepo, targetStack, targetRepo);

    Map<String, String> commandParams = new HashMap<String, String>();
    commandParams.put(FinalizeUpgradeAction.UPGRADE_DIRECTION_KEY, "downgrade");
    commandParams.put(FinalizeUpgradeAction.VERSION_KEY, midRepo);
    commandParams.put(FinalizeUpgradeAction.ORIGINAL_STACK_KEY, sourceStack.getStackId());
    commandParams.put(FinalizeUpgradeAction.TARGET_STACK_KEY, targetStack.getStackId());

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName(clusterName);

    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null, null, null);
    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(executionCommand));

    finalizeUpgradeAction.setExecutionCommand(executionCommand);
    finalizeUpgradeAction.setHostRoleCommand(hostRoleCommand);

    CommandReport report = finalizeUpgradeAction.execute(null);
    assertNotNull(report);
    assertEquals(HostRoleStatus.FAILED.name(), report.getStatus());
    assertTrue(report.getStdErr().contains(FinalizeUpgradeAction.PREVIOUS_UPGRADE_NOT_COMPLETED_MSG));
  }

  @Test
  public void testFinalizeUpgrade() throws Exception {
    StackId sourceStack = HDP_21_STACK;
    StackId targetStack = HDP_21_STACK;
    String sourceRepo = HDP_2_1_1_0;
    String targetRepo = HDP_2_1_1_1;

    makeUpgradeCluster(sourceStack, sourceRepo, targetStack, targetRepo);

    // Verify the repo before calling Finalize
    AmbariCustomCommandExecutionHelper helper = m_injector.getInstance(AmbariCustomCommandExecutionHelper.class);
    Host host = clusters.getHost("h1");
    Cluster cluster = clusters.getCluster(clusterName);

    RepositoryInfo repo = ambariMetaInfo.getRepository(sourceStack.getStackName(), sourceStack.getStackVersion(), "redhat6", sourceStack.getStackId());
    assertEquals(HDP_211_CENTOS6_REPO_URL, repo.getBaseUrl());
    verifyBaseRepoURL(helper, cluster, host, HDP_211_CENTOS6_REPO_URL);

    // Finalize the upgrade
    Map<String, String> commandParams = new HashMap<String, String>();
    commandParams.put(FinalizeUpgradeAction.UPGRADE_DIRECTION_KEY, "upgrade");
    commandParams.put(FinalizeUpgradeAction.VERSION_KEY, targetRepo);

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName(clusterName);

    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null, null, null);
    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(executionCommand));

    finalizeUpgradeAction.setExecutionCommand(executionCommand);
    finalizeUpgradeAction.setHostRoleCommand(hostRoleCommand);

    CommandReport report = finalizeUpgradeAction.execute(null);
    assertNotNull(report);
    assertEquals(HostRoleStatus.COMPLETED.name(), report.getStatus());

    // Verify the metainfo url
    verifyBaseRepoURL(helper, cluster, host, "http://foo1");
  }

  /**
   * Tests that finalize still works when there are hosts which are already
   * {@link RepositoryVersionState#CURRENT}.
   *
   * @throws Exception
   */
  @Test
  public void testFinalizeWithHostsAlreadyCurrent() throws Exception {
    StackId sourceStack = HDP_21_STACK;
    StackId targetStack = HDP_21_STACK;
    String sourceRepo = HDP_2_1_1_0;
    String targetRepo = HDP_2_1_1_1;

    makeUpgradeCluster(sourceStack, sourceRepo, targetStack, targetRepo);

    // move the old version from CURRENT to INSTALLED and the new version from
    // UPGRADED to CURRENT - this will simulate what happens when a host is
    // removed before finalization and all hosts transition to CURRENT
    List<HostVersionEntity> hostVersions = hostVersionDAO.findAll();
    for (HostVersionEntity hostVersion : hostVersions) {
      if (hostVersion.getState() == RepositoryVersionState.CURRENT) {
        hostVersion.setState(RepositoryVersionState.INSTALLED);
      } else {
        hostVersion.setState(RepositoryVersionState.CURRENT);
      }

      hostVersionDAO.merge(hostVersion);
    }

    // Verify the repo before calling Finalize
    AmbariCustomCommandExecutionHelper helper = m_injector.getInstance(AmbariCustomCommandExecutionHelper.class);
    Host host = clusters.getHost("h1");
    Cluster cluster = clusters.getCluster(clusterName);

    RepositoryInfo repo = ambariMetaInfo.getRepository(sourceStack.getStackName(),
            sourceStack.getStackVersion(), "redhat6", sourceStack.getStackId());
    assertEquals(HDP_211_CENTOS6_REPO_URL, repo.getBaseUrl());
    verifyBaseRepoURL(helper, cluster, host, HDP_211_CENTOS6_REPO_URL);

    // Finalize the upgrade
    Map<String, String> commandParams = new HashMap<String, String>();
    commandParams.put(FinalizeUpgradeAction.UPGRADE_DIRECTION_KEY, "upgrade");
    commandParams.put(FinalizeUpgradeAction.VERSION_KEY, targetRepo);

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName(clusterName);

    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null, null, null);
    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(executionCommand));

    finalizeUpgradeAction.setExecutionCommand(executionCommand);
    finalizeUpgradeAction.setHostRoleCommand(hostRoleCommand);

    CommandReport report = finalizeUpgradeAction.execute(null);
    assertNotNull(report);
    assertEquals(HostRoleStatus.COMPLETED.name(), report.getStatus());
  }

  private void verifyBaseRepoURL(AmbariCustomCommandExecutionHelper helper, Cluster cluster, Host host, String expectedRepoBaseURL) throws AmbariException {
    String repoInfo = helper.getRepoInfo(cluster, host);
    Gson gson = new Gson();
    JsonElement element = gson.fromJson(repoInfo, JsonElement.class);
    assertTrue(element.isJsonArray());
    JsonArray list = JsonArray.class.cast(element);
    assertEquals(1, list.size());

    JsonObject o = list.get(0).getAsJsonObject();
    assertTrue(o.has("baseUrl"));
    assertEquals(expectedRepoBaseURL, o.get("baseUrl").getAsString());
  }

  @Test
  public void testFinalizeUpgradeAcrossStacks() throws Exception {
    StackId sourceStack = HDP_21_STACK;
    StackId targetStack = HDP_22_STACK;
    String sourceRepo = HDP_2_1_1_0;
    String targetRepo = HDP_2_2_0_1;

    makeCrossStackUpgradeCluster(sourceStack, sourceRepo, targetStack, targetRepo);

    Cluster cluster = clusters.getCluster(clusterName);

    // setup the cluster for the upgrade across stacks
    cluster.setCurrentStackVersion(sourceStack);
    cluster.setDesiredStackVersion(targetStack);

    Map<String, String> commandParams = new HashMap<String, String>();
    commandParams.put(FinalizeUpgradeAction.UPGRADE_DIRECTION_KEY, "upgrade");
    commandParams.put(FinalizeUpgradeAction.VERSION_KEY, targetRepo);
    commandParams.put(FinalizeUpgradeAction.ORIGINAL_STACK_KEY, sourceStack.getStackId());
    commandParams.put(FinalizeUpgradeAction.TARGET_STACK_KEY, targetStack.getStackId());

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName(clusterName);

    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null, null, null);

    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(executionCommand));

    finalizeUpgradeAction.setExecutionCommand(executionCommand);
    finalizeUpgradeAction.setHostRoleCommand(hostRoleCommand);

    CommandReport report = finalizeUpgradeAction.execute(null);
    assertNotNull(report);
    assertEquals(HostRoleStatus.COMPLETED.name(), report.getStatus());

    StackId currentStackId = cluster.getCurrentStackVersion();
    StackId desiredStackId = cluster.getDesiredStackVersion();

    // verify current/desired stacks are updated to the new stack
    assertEquals(desiredStackId, currentStackId);
    assertEquals(targetStack, currentStackId);
    assertEquals(targetStack, desiredStackId);
  }

  /**
   * Tests some of the action items are completed when finalizing downgrade
   * across stacks (HDP 2.2 -> HDP 2.3).
   *
   * @throws Exception
   */
  @Test
  public void testFinalizeDowngradeAcrossStacks() throws Exception {
    StackId sourceStack = HDP_21_STACK;
    StackId targetStack = HDP_22_STACK;
    String sourceRepo = HDP_2_1_1_0;
    String targetRepo = HDP_2_2_0_1;

    makeCrossStackUpgradeCluster(sourceStack, sourceRepo, targetStack, targetRepo);
    Cluster cluster = clusters.getCluster(clusterName);

    // install HDFS with some components
    Service service = installService(cluster, "HDFS");
    addServiceComponent(cluster, service, "NAMENODE");
    addServiceComponent(cluster, service, "DATANODE");
    createNewServiceComponentHost(cluster, "HDFS", "NAMENODE", "h1");
    createNewServiceComponentHost(cluster, "HDFS", "DATANODE", "h1");

    // create some configs
    createConfigs(cluster);

    // setup the cluster for the upgrade across stacks
    cluster.setCurrentStackVersion(sourceStack);
    cluster.setDesiredStackVersion(targetStack);

    // now that the desired version is set, we can create some new configs in
    // the new stack version
    createConfigs(cluster);

    // verify we have configs in both HDP stacks
    cluster = clusters.getCluster(clusterName);
    Collection<Config> configs = cluster.getAllConfigs();
    assertEquals(8, configs.size());

    Map<String, String> commandParams = new HashMap<String, String>();
    commandParams.put(FinalizeUpgradeAction.UPGRADE_DIRECTION_KEY, "downgrade");
    commandParams.put(FinalizeUpgradeAction.VERSION_KEY, sourceRepo);
    commandParams.put(FinalizeUpgradeAction.ORIGINAL_STACK_KEY, sourceStack.getStackId());
    commandParams.put(FinalizeUpgradeAction.TARGET_STACK_KEY, targetStack.getStackId());

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName(clusterName);

    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null, null, null);

    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(executionCommand));

    HostVersionDAO dao = m_injector.getInstance(HostVersionDAO.class);

    List<HostVersionEntity> hosts = dao.findByClusterStackAndVersion(clusterName, targetStack, targetRepo);
    assertFalse(hosts.isEmpty());
    for (HostVersionEntity hve : hosts) {
      assertTrue(hve.getState() == RepositoryVersionState.INSTALLED);
    }

    finalizeUpgradeAction.setExecutionCommand(executionCommand);
    finalizeUpgradeAction.setHostRoleCommand(hostRoleCommand);

    CommandReport report = finalizeUpgradeAction.execute(null);
    assertNotNull(report);
    assertEquals(HostRoleStatus.COMPLETED.name(), report.getStatus());

    StackId currentStackId = cluster.getCurrentStackVersion();
    StackId desiredStackId = cluster.getDesiredStackVersion();

    // verify current/desired stacks are back to normal
    assertEquals(desiredStackId, currentStackId);
    assertEquals(sourceStack, currentStackId);
    assertEquals(sourceStack, desiredStackId);

    // verify we have configs in only 1 stack
    cluster = clusters.getCluster(clusterName);
    configs = cluster.getAllConfigs();
    assertEquals(4, configs.size());

    hosts = dao.findByClusterStackAndVersion(clusterName, targetStack, targetRepo);
    assertFalse(hosts.isEmpty());
    for (HostVersionEntity hve : hosts) {
      assertTrue(hve.getState() == RepositoryVersionState.INSTALLED);
    }
  }

  /**
   * Tests that finalization can occur when the cluster state is
   * {@link RepositoryVersionState#UPGRADING} if all of the hosts and components
   * are reporting correct versions and states.
   *
   * @throws Exception
   */
  @Test
  public void testFinalizeUpgradeWithClusterStateInconsistencies() throws Exception {
    StackId sourceStack = HDP_21_STACK;
    StackId targetStack = HDP_22_STACK;
    String sourceRepo = HDP_2_1_1_0;
    String targetRepo = HDP_2_2_0_1;

    makeCrossStackUpgradeCluster(sourceStack, sourceRepo, targetStack, targetRepo);

    Cluster cluster = clusters.getCluster(clusterName);

    Service service = installService(cluster, "HDFS");
    addServiceComponent(cluster, service, "NAMENODE");
    addServiceComponent(cluster, service, "DATANODE");
    createNewServiceComponentHost(cluster, "HDFS", "NAMENODE", "h1");
    createNewServiceComponentHost(cluster, "HDFS", "DATANODE", "h1");

    // create some configs
    createConfigs(cluster);

    // setup the cluster for the upgrade across stacks
    cluster.setCurrentStackVersion(sourceStack);
    cluster.setDesiredStackVersion(targetStack);

    // set the SCH versions to the new stack so that the finalize action is
    // happy
    cluster.getServiceComponentHosts("HDFS", "NAMENODE").get(0).setVersion(targetRepo);
    cluster.getServiceComponentHosts("HDFS", "DATANODE").get(0).setVersion(targetRepo);

    // inject an unhappy path where the cluster repo version is still UPGRADING
    // even though all of the hosts are UPGRADED
    ClusterVersionEntity upgradingClusterVersion = clusterVersionDAO.findByClusterAndStackAndVersion(
            clusterName, HDP_22_STACK, targetRepo);

    upgradingClusterVersion.setState(RepositoryVersionState.INSTALLING);
    upgradingClusterVersion = clusterVersionDAO.merge(upgradingClusterVersion);

    // verify the conditions for the test are met properly
    upgradingClusterVersion = clusterVersionDAO.findByClusterAndStackAndVersion(clusterName, HDP_22_STACK, targetRepo);
    List<HostVersionEntity> hostVersions = hostVersionDAO.findByClusterStackAndVersion(clusterName, HDP_22_STACK, targetRepo);

    assertEquals(RepositoryVersionState.INSTALLING, upgradingClusterVersion.getState());
    assertTrue(hostVersions.size() > 0);
    for (HostVersionEntity hostVersion : hostVersions) {
      assertEquals(RepositoryVersionState.INSTALLED, hostVersion.getState());
    }

    // now finalize and ensure we can transition from UPGRADING to UPGRADED
    // automatically before CURRENT
    Map<String, String> commandParams = new HashMap<String, String>();
    commandParams.put(FinalizeUpgradeAction.UPGRADE_DIRECTION_KEY, "upgrade");
    commandParams.put(FinalizeUpgradeAction.VERSION_KEY, targetRepo);
    commandParams.put(FinalizeUpgradeAction.ORIGINAL_STACK_KEY, sourceStack.getStackId());
    commandParams.put(FinalizeUpgradeAction.TARGET_STACK_KEY, targetStack.getStackId());

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName(clusterName);

    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null, null, null);

    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(executionCommand));

    finalizeUpgradeAction.setExecutionCommand(executionCommand);
    finalizeUpgradeAction.setHostRoleCommand(hostRoleCommand);

    CommandReport report = finalizeUpgradeAction.execute(null);
    assertNotNull(report);
    assertEquals(HostRoleStatus.COMPLETED.name(), report.getStatus());

    StackId currentStackId = cluster.getCurrentStackVersion();
    StackId desiredStackId = cluster.getDesiredStackVersion();

    // verify current/desired stacks are updated to the new stack
    assertEquals(desiredStackId, currentStackId);
    assertEquals(targetStack, currentStackId);
    assertEquals(targetStack, desiredStackId);
  }

  @Test
  public void testUpgradeHistory() throws Exception {
    StackId sourceStack = HDP_21_STACK;
    StackId targetStack = HDP_21_STACK;
    String sourceRepo = HDP_2_1_1_0;
    String targetRepo = HDP_2_1_1_1;

    makeUpgradeCluster(sourceStack, sourceRepo, targetStack, targetRepo);

    // Verify the repo before calling Finalize
    AmbariCustomCommandExecutionHelper helper = m_injector.getInstance(AmbariCustomCommandExecutionHelper.class);
    Host host = clusters.getHost("h1");
    Cluster cluster = clusters.getCluster(clusterName);

    // install HDFS with some components
    Service service = installService(cluster, "HDFS");
    addServiceComponent(cluster, service, "NAMENODE");
    addServiceComponent(cluster, service, "DATANODE");
    ServiceComponentHost nnSCH = createNewServiceComponentHost(cluster, "HDFS", "NAMENODE", "h1");
    ServiceComponentHost dnSCH = createNewServiceComponentHost(cluster, "HDFS", "DATANODE", "h1");

    // fake their upgrade
    nnSCH.setStackVersion(nnSCH.getDesiredStackVersion());
    nnSCH.setVersion(targetRepo);
    dnSCH.setStackVersion(nnSCH.getDesiredStackVersion());
    dnSCH.setVersion(targetRepo);

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
    upgradeEntity.setRequestId(requestEntity.getRequestId());
    upgradeEntity.setUpgradePackage("");
    upgradeEntity.setFromVersion(sourceRepo);
    upgradeEntity.setToVersion(targetRepo);
    upgradeEntity.setUpgradeType(UpgradeType.NON_ROLLING);
    upgradeDAO.create(upgradeEntity);

    // verify that no history exist exists yet
    List<ServiceComponentHistoryEntity> historyEntites = serviceComponentDesiredStateDAO.findHistory(
            cluster.getClusterId(), nnSCH.getServiceName(),
            nnSCH.getServiceComponentName());

    assertEquals(0, historyEntites.size());

    RepositoryInfo repo = ambariMetaInfo.getRepository(sourceStack.getStackName(), sourceStack.getStackVersion(), "redhat6", sourceStack.getStackId());
    assertEquals(HDP_211_CENTOS6_REPO_URL, repo.getBaseUrl());
    verifyBaseRepoURL(helper, cluster, host, HDP_211_CENTOS6_REPO_URL);

    // Finalize the upgrade, passing in the request ID so that history is
    // created
    Map<String, String> commandParams = new HashMap<String, String>();
    commandParams.put(FinalizeUpgradeAction.REQUEST_ID, String.valueOf(requestEntity.getRequestId()));
    commandParams.put(FinalizeUpgradeAction.UPGRADE_DIRECTION_KEY, "upgrade");
    commandParams.put(FinalizeUpgradeAction.VERSION_KEY, targetRepo);

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName(clusterName);

    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null, null, null);
    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(executionCommand));

    finalizeUpgradeAction.setExecutionCommand(executionCommand);
    finalizeUpgradeAction.setHostRoleCommand(hostRoleCommand);

    CommandReport report = finalizeUpgradeAction.execute(null);
    assertNotNull(report);
    assertEquals(HostRoleStatus.COMPLETED.name(), report.getStatus());

    // Verify the metainfo url
    verifyBaseRepoURL(helper, cluster, host, "http://foo1");

    // ensure that history now exists
    historyEntites = serviceComponentDesiredStateDAO.findHistory(cluster.getClusterId(),
            nnSCH.getServiceName(), nnSCH.getServiceComponentName());

    assertEquals(1, historyEntites.size());
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
    return sch;
  }

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

  private ServiceComponent addServiceComponent(Cluster cluster, Service service,
                                               String componentName) throws AmbariException {
    ServiceComponent serviceComponent = null;
    try {
      serviceComponent = service.getServiceComponent(componentName);
    } catch (ServiceComponentNotFoundException e) {
      serviceComponent = serviceComponentFactory.createNew(service, componentName);
      service.addServiceComponent(serviceComponent);
      serviceComponent.setDesiredState(State.INSTALLED);
    }

    return serviceComponent;
  }

  private void createConfigs(Cluster cluster) {
    Map<String, String> properties = new HashMap<String, String>();
    Map<String, Map<String, String>> propertiesAttributes = new HashMap<String, Map<String, String>>();
    properties.put("a", "a1");
    properties.put("b", "b1");

    configFactory.createNew(cluster, "zookeeper-env", "version-" + System.currentTimeMillis(),
        properties, propertiesAttributes);

    properties.put("zookeeper_a", "value_1");
    properties.put("zookeeper_b", "value_2");

    configFactory.createNew(cluster, "hdfs-site", "version-" + System.currentTimeMillis(),
        properties, propertiesAttributes);

    properties.put("hdfs_a", "value_3");
    properties.put("hdfs_b", "value_4");

    configFactory.createNew(cluster, "core-site", "version-" + System.currentTimeMillis(),
        properties, propertiesAttributes);

    configFactory.createNew(cluster, "foo-site", "version-" + System.currentTimeMillis(),
        properties, propertiesAttributes);
  }
}
