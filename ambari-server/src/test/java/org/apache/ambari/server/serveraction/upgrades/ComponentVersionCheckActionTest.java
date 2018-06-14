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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.ServiceComponentNotFoundException;
import org.apache.ambari.server.actionmanager.ExecutionCommandWrapper;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.RequestDAO;
import org.apache.ambari.server.orm.dao.ServiceGroupDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.dao.UpgradeDAO;
import org.apache.ambari.server.orm.entities.MpackEntity;
import org.apache.ambari.server.orm.entities.RepoDefinitionEntity;
import org.apache.ambari.server.orm.entities.RepoOsEntity;
import org.apache.ambari.server.orm.entities.RequestEntity;
import org.apache.ambari.server.orm.entities.ServiceGroupEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.orm.entities.UpgradeHistoryEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentFactory;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.ServiceGroup;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
import org.apache.ambari.server.utils.EventBusSynchronizer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.UnitOfWork;

/**
 * Tests upgrade-related server side actions
 */
@Ignore
@Experimental(feature = ExperimentalFeature.UNIT_TEST_REQUIRED)
public class ComponentVersionCheckActionTest {

  private static final String HDP_2_1_1_0 = "2.1.1.0-1";
  private static final String HDP_2_1_1_1 = "2.1.1.1-2";

  private static final String HDP_2_2_1_0 = "2.2.1.0-1";

  private static final StackId HDP_21_STACK = new StackId("HDP-2.1.1");
  private static final StackId HDP_220_STACK = new StackId("HDP-2.2.0");
  private static final StackId HDP_221_STACK = new StackId("HDP-2.2.1");

  private Injector m_injector;

  @Inject
  private OrmTestHelper m_helper;

  @Inject
  private HostRoleCommandFactory hostRoleCommandFactory;

  @Inject
  private ServiceFactory serviceFactory;

  @Inject
  private ServiceComponentFactory serviceComponentFactory;

  @Inject
  private ServiceComponentHostFactory serviceComponentHostFactory;

  @Inject
  private ConfigFactory configFactory;

  @Before
  public void setup() throws Exception {
    m_injector = Guice.createInjector(new InMemoryDefaultTestModule());
    EventBusSynchronizer.synchronizeAmbariEventPublisher(m_injector);
    m_injector.getInstance(GuiceJpaInitializer.class);
    m_injector.injectMembers(this);
    m_injector.getInstance(UnitOfWork.class).begin();
  }

  @After
  public void teardown() throws Exception {
    m_injector.getInstance(UnitOfWork.class).end();
    H2DatabaseCleaner.clearDatabase(m_injector.getProvider(EntityManager.class).get());
  }

  private void makeUpgradeCluster(StackId sourceStack, String sourceRepo, StackId targetStack, String targetRepo) throws Exception {
    String clusterName = "c1";
    String hostName = "h1";

    m_helper.createMpack(sourceStack);

    Clusters clusters = m_injector.getInstance(Clusters.class);
    clusters.addCluster(clusterName, sourceStack);

    StackDAO stackDAO = m_injector.getInstance(StackDAO.class);
    RequestDAO requestDAO = m_injector.getInstance(RequestDAO.class);
    UpgradeDAO upgradeDAO = m_injector.getInstance(UpgradeDAO.class);

    StackEntity stackEntitySource = stackDAO.find(sourceStack.getStackName(), sourceStack.getStackVersion());
    StackEntity stackEntityTarget = stackDAO.find(targetStack.getStackName(), targetStack.getStackVersion());
    assertNotNull(stackEntitySource);
    assertNotNull(stackEntityTarget);

    Cluster c = clusters.getCluster(clusterName);
    c.setDesiredStackVersion(sourceStack);

    // add a host component
    clusters.addHost(hostName);

    Host host = clusters.getHost(hostName);

    Map<String, String> hostAttributes = new HashMap<>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "6");
    host.setHostAttributes(hostAttributes);

    // Create the starting mpack
    m_helper.createMpack(sourceStack);

    // Create the new repo version
    List<RepoOsEntity> urlInfo = new ArrayList<>();
    RepoDefinitionEntity repoDefinitionEntity1 = new RepoDefinitionEntity();
    repoDefinitionEntity1.setRepoID(targetStack.getStackId());
    repoDefinitionEntity1.setBaseUrl("http://foo1");
    repoDefinitionEntity1.setRepoName("HDP");
    RepoOsEntity repoOsEntity = new RepoOsEntity();
    repoOsEntity.setFamily("redhat6");
    repoOsEntity.setAmbariManaged(true);
    repoOsEntity.addRepoDefinition(repoDefinitionEntity1);
    urlInfo.add(repoOsEntity);


    m_helper.createMpack(targetStack);

    // Start upgrading the newer repo
    c.setCurrentStackVersion(targetStack);

    RequestEntity requestEntity = new RequestEntity();
    requestEntity.setClusterId(c.getClusterId());
    requestEntity.setRequestId(1L);
    requestEntity.setStartTime(System.currentTimeMillis());
    requestEntity.setCreateTime(System.currentTimeMillis());
    requestDAO.create(requestEntity);

    UpgradeEntity upgradeEntity = new UpgradeEntity();
    upgradeEntity.setId(1L);
    upgradeEntity.setClusterId(c.getClusterId());
    upgradeEntity.setRequestEntity(requestEntity);
    upgradeEntity.setUpgradePackage("");
    upgradeEntity.setUpgradeType(UpgradeType.EXPRESS);
    upgradeDAO.create(upgradeEntity);

    c.setUpgradeEntity(upgradeEntity);
  }

  /**
   * Creates a cluster with a running upgrade. The upgrade will have no services
   * attached to it, so those will need to be set after this is called.
   */
  private void makeCrossStackUpgradeCluster(StackId sourceStack, String sourceRepo,
      StackId targetStack, String targetRepo, String clusterName, String hostName)
      throws Exception {

    m_helper.createMpack(sourceStack);
    m_helper.createMpack(targetStack);

    Clusters clusters = m_injector.getInstance(Clusters.class);
    clusters.addCluster(clusterName, sourceStack);

    StackDAO stackDAO = m_injector.getInstance(StackDAO.class);
    RequestDAO requestDAO = m_injector.getInstance(RequestDAO.class);
    UpgradeDAO upgradeDAO = m_injector.getInstance(UpgradeDAO.class);

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

    Map<String, String> hostAttributes = new HashMap<>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "6");
    host.setHostAttributes(hostAttributes);

    clusters.mapHostToCluster(hostName, clusterName);

    // Create the starting mpack
    m_helper.createMpack(sourceStack);

    // create the new mpack
    m_helper.createMpack(targetStack);

    RequestEntity requestEntity = new RequestEntity();
    requestEntity.setClusterId(c.getClusterId());
    requestEntity.setRequestId(1L);
    requestEntity.setStartTime(System.currentTimeMillis());
    requestEntity.setCreateTime(System.currentTimeMillis());
    requestDAO.create(requestEntity);

    UpgradeEntity upgradeEntity = new UpgradeEntity();
    upgradeEntity.setId(1L);
    upgradeEntity.setClusterId(c.getClusterId());
    upgradeEntity.setRequestEntity(requestEntity);
    upgradeEntity.setUpgradePackage("");
    upgradeEntity.setUpgradeType(UpgradeType.EXPRESS);
    upgradeDAO.create(upgradeEntity);

    c.setUpgradeEntity(upgradeEntity);
  }

  @Test
  public void testMatchingVersions() throws Exception {
    StackId sourceStack = HDP_21_STACK;
    StackId targetStack = HDP_21_STACK;
    String sourceRepo = HDP_2_1_1_0;
    String targetRepo = HDP_2_1_1_1;

    makeUpgradeCluster(sourceStack, sourceRepo, targetStack, targetRepo);

    // Finalize the upgrade
    Map<String, String> commandParams = new HashMap<>();
    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName("c1");

    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null, null, null);
    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(executionCommand));

    ComponentVersionCheckAction action = m_injector.getInstance(ComponentVersionCheckAction.class);
    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    CommandReport report = action.execute(null);
    assertNotNull(report);
    assertEquals(HostRoleStatus.COMPLETED.name(), report.getStatus());
    assertEquals(0, report.getExitCode());
  }

  @Test
  public void testMixedComponentVersions() throws Exception {
    StackId sourceStack = HDP_21_STACK;
    StackId targetStack = HDP_220_STACK;
    String sourceVersion = HDP_2_1_1_0;
    String targetVersion = HDP_2_2_1_0;
    String clusterName = "c1";
    String hostName = "h1";

    makeCrossStackUpgradeCluster(sourceStack, sourceVersion, targetStack, targetVersion,
        clusterName, hostName);

    Clusters clusters = m_injector.getInstance(Clusters.class);
    Cluster cluster = clusters.getCluster("c1");
    ServiceGroup serviceGroup = cluster.addServiceGroup("CORE", targetStack.getStackId());

    MpackEntity sourceMpack = m_helper.createMpack(HDP_21_STACK);
    MpackEntity targetMpack = m_helper.createMpack(HDP_220_STACK);

    Service service = installService(cluster, serviceGroup, "HDFS", sourceMpack);
    addServiceComponent(service, "NAMENODE");
    addServiceComponent(service, "DATANODE");
    createNewServiceComponentHost(cluster, "HDFS", "NAMENODE", hostName);
    createNewServiceComponentHost(cluster, "HDFS", "DATANODE", hostName);

    // create some configs
    createConfigs(cluster);

    // setup the cluster for the upgrade across stacks
    cluster.setCurrentStackVersion(sourceStack);
    cluster.setDesiredStackVersion(targetStack);

    ServiceGroupDAO serviceGroupDAO = m_injector.getInstance(ServiceGroupDAO.class);
    ServiceGroupEntity serviceGroupEntity = serviceGroupDAO.findByPK(serviceGroup.getServiceGroupId());

    // tell the upgrade that HDFS is upgrading - without this, no services will
    // be participating in the upgrade
    UpgradeEntity upgrade = cluster.getUpgradeInProgress();
    UpgradeHistoryEntity history = new UpgradeHistoryEntity(upgrade, serviceGroupEntity, sourceMpack, targetMpack);
    upgrade.addHistory(history);

    UpgradeDAO upgradeDAO = m_injector.getInstance(UpgradeDAO.class);
    upgrade = upgradeDAO.merge(upgrade);

    // set the SCH versions to the new stack so that the finalize action is
    // happy - don't update DATANODE - we want to make the action complain
    cluster.getServiceComponentHosts("HDFS", "NAMENODE").get(0).setVersion(targetVersion);

    // now finalize and ensure we can transition from UPGRADING to UPGRADED
    // automatically before CURRENT
    Map<String, String> commandParams = new HashMap<>();
    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName("c1");

    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null, null, null);

    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(executionCommand));

    ComponentVersionCheckAction action = m_injector.getInstance(ComponentVersionCheckAction.class);
    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    CommandReport report = action.execute(null);
    assertNotNull(report);
    assertEquals(HostRoleStatus.FAILED.name(), report.getStatus());
    assertEquals(-1, report.getExitCode());

    // OK, now set the datanode so it completes
    cluster.getServiceComponentHosts("HDFS", "DATANODE").get(0).setVersion(targetVersion);

    report = action.execute(null);
    assertNotNull(report);
    assertEquals(HostRoleStatus.COMPLETED.name(), report.getStatus());
    assertEquals(0, report.getExitCode());
  }

  @Test
  public void testMatchingPartialVersions() throws Exception {
    StackId sourceStack = HDP_21_STACK;
    StackId targetStack = HDP_21_STACK;
    String sourceRepo = HDP_2_1_1_0;
    String targetRepo = HDP_2_1_1_1;

    makeUpgradeCluster(sourceStack, sourceRepo, targetStack, targetRepo);

    Clusters clusters = m_injector.getInstance(Clusters.class);

    Host host = clusters.getHost("h1");
    Assert.assertNotNull(host);
    host.setOsInfo("redhat6");

    Cluster cluster = clusters.getCluster("c1");
    ServiceGroup serviceGroup = cluster.addServiceGroup("CORE", targetStack.getStackId());
    clusters.mapHostToCluster("h1", "c1");

    MpackEntity sourceMpack = m_helper.createMpack(HDP_220_STACK);
    MpackEntity targetMpack = m_helper.createMpack(HDP_221_STACK);


    Service service = installService(cluster, serviceGroup, "HDFS", sourceMpack);
    addServiceComponent(service, "NAMENODE");
    addServiceComponent(service, "DATANODE");

    ServiceComponentHost sch = createNewServiceComponentHost(cluster, "HDFS", "NAMENODE", "h1");
    sch.setVersion(HDP_2_1_1_0);
    sch = createNewServiceComponentHost(cluster, "HDFS", "DATANODE", "h1");
    sch.setVersion(HDP_2_1_1_0);

    service = installService(cluster, serviceGroup, "ZOOKEEPER", targetMpack);
    addServiceComponent(service, "ZOOKEEPER_SERVER");

    sch = createNewServiceComponentHost(cluster, "ZOOKEEPER", "ZOOKEEPER_SERVER", "h1");
    sch.setVersion(HDP_2_1_1_1);

    // Finalize the upgrade
    Map<String, String> commandParams = new HashMap<>();
    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName("c1");

    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null, null, null);
    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(executionCommand));

    ComponentVersionCheckAction action = m_injector.getInstance(ComponentVersionCheckAction.class);
    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    CommandReport report = action.execute(null);
    assertNotNull(report);
    assertEquals(HostRoleStatus.COMPLETED.name(), report.getStatus());
    assertEquals(0, report.getExitCode());

  }

  private ServiceComponentHost createNewServiceComponentHost(Cluster cluster, String svc,
                                                             String svcComponent, String hostName) throws AmbariException {
    Assert.assertNotNull(cluster.getConfigGroups());
    Service s = cluster.getService(svc);
    ServiceComponent sc = addServiceComponent(s, svcComponent);

    ServiceComponentHost sch = serviceComponentHostFactory.createNew(sc, hostName);

    sc.addServiceComponentHost(sch);
    sch.setDesiredState(State.INSTALLED);
    sch.setState(State.INSTALLED);

    return sch;
  }

  private Service installService(Cluster cluster, ServiceGroup serviceGroup, String serviceName, MpackEntity mpackEntity) throws AmbariException {
    Service service = serviceFactory.createNew(cluster, serviceGroup, new ArrayList<>(), serviceName, serviceName);
    cluster.addService(service);
    return service;
  }

  private ServiceComponent addServiceComponent(Service service, String componentName) throws AmbariException {
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

  private void createConfigs(Cluster cluster) {
    Map<String, String> properties = new HashMap<>();
    Map<String, Map<String, String>> propertiesAttributes = new HashMap<>();
    properties.put("a", "a1");
    properties.put("b", "b1");

    configFactory.createNew(cluster, "hdfs-site", "version1", properties, propertiesAttributes);
    properties.put("c", "c1");
    properties.put("d", "d1");

    configFactory.createNew(cluster, "core-site", "version1", properties, propertiesAttributes);
    configFactory.createNew(cluster, "foo-site", "version1", properties, propertiesAttributes);
  }
}
