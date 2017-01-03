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

import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.api.resources.UpgradeResourceDefinition;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.audit.AuditLogger;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.RequestDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.dao.StageDAO;
import org.apache.ambari.server.orm.dao.UpgradeDAO;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.RequestEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.orm.entities.StageEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.orm.entities.UpgradeGroupEntity;
import org.apache.ambari.server.orm.entities.UpgradeItemEntity;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.serveraction.upgrades.AutoSkipFailedSummaryAction;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.stack.UpgradePack;
import org.apache.ambari.server.state.stack.upgrade.Direction;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
import org.apache.ambari.server.topology.TopologyManager;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.ambari.server.view.ViewRegistry;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.persist.PersistService;
import com.google.inject.util.Modules;

/**
 * UpgradeResourceDefinition tests.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({AuthorizationHelper.class})
@PowerMockIgnore({"javax.management.*", "javax.crypto.*"})
public class UpgradeResourceProviderTest {

  private UpgradeDAO upgradeDao = null;
  private RequestDAO requestDao = null;
  private RepositoryVersionDAO repoVersionDao = null;
  private Injector injector;
  private Clusters clusters;
  private OrmTestHelper helper;
  private AmbariManagementController amc;
  private ConfigHelper configHelper;
  private StackDAO stackDAO;
  private AmbariMetaInfo ambariMetaInfo;
  private TopologyManager topologyManager;
  private ConfigFactory configFactory;

  @Before
  public void before() throws Exception {
    // setup the config helper for placeholder resolution
    configHelper = EasyMock.createNiceMock(ConfigHelper.class);

    expect(
        configHelper.getPlaceholderValueFromDesiredConfigurations(
            EasyMock.anyObject(Cluster.class), EasyMock.eq("{{foo/bar}}"))).andReturn(
        "placeholder-rendered-properly").anyTimes();

    expect(
        configHelper.getDefaultProperties(EasyMock.anyObject(StackId.class),
            EasyMock.anyObject(Cluster.class), EasyMock.anyBoolean())).andReturn(
        new HashMap<String, Map<String, String>>()).anyTimes();


    EasyMock.replay(configHelper);

    // create an injector which will inject the mocks
    injector = Guice.createInjector(Modules.override(
        new InMemoryDefaultTestModule()).with(new MockModule()));

    injector.getInstance(GuiceJpaInitializer.class);


    helper = injector.getInstance(OrmTestHelper.class);

    amc = injector.getInstance(AmbariManagementController.class);
    ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    configFactory = injector.getInstance(ConfigFactory.class);

    Field field = AmbariServer.class.getDeclaredField("clusterController");
    field.setAccessible(true);
    field.set(null, amc);

    stackDAO = injector.getInstance(StackDAO.class);
    upgradeDao = injector.getInstance(UpgradeDAO.class);
    requestDao = injector.getInstance(RequestDAO.class);
    repoVersionDao = injector.getInstance(RepositoryVersionDAO.class);

    AmbariEventPublisher publisher = createNiceMock(AmbariEventPublisher.class);
    replay(publisher);
    ViewRegistry.initInstance(new ViewRegistry(publisher));

    // TODO AMARI-12698, this file is attempting to check RU on version 2.1.1, which doesn't support it
    // because it has no upgrade packs. We should use correct versions that have stacks.
    // For now, Ignore the tests that fail.
    StackEntity stackEntity211 = stackDAO.find("HDP", "2.1.1");
    StackEntity stackEntity220 = stackDAO.find("HDP", "2.2.0");
    StackId stack211 = new StackId("HDP-2.1.1");
    StackId stack220 = new StackId("HDP-2.2.0");

    RepositoryVersionEntity repoVersionEntity = new RepositoryVersionEntity();
    repoVersionEntity.setDisplayName("My New Version 1");
    repoVersionEntity.setOperatingSystems("");
    repoVersionEntity.setStack(stackEntity211);
    repoVersionEntity.setVersion("2.1.1.0");
    repoVersionDao.create(repoVersionEntity);

    repoVersionEntity = new RepositoryVersionEntity();
    repoVersionEntity.setDisplayName("My New Version 2 for patch upgrade");
    repoVersionEntity.setOperatingSystems("");
    repoVersionEntity.setStack(stackEntity211);
    repoVersionEntity.setVersion("2.1.1.1");
    repoVersionDao.create(repoVersionEntity);

    repoVersionEntity = new RepositoryVersionEntity();
    repoVersionEntity.setDisplayName("My New Version 3 for major upgrade");
    repoVersionEntity.setOperatingSystems("");
    repoVersionEntity.setStack(stackEntity220);
    repoVersionEntity.setVersion("2.2.0.0");
    repoVersionDao.create(repoVersionEntity);

    clusters = injector.getInstance(Clusters.class);

    clusters.addCluster("c1", stack211);
    Cluster cluster = clusters.getCluster("c1");

    helper.getOrCreateRepositoryVersion(stack211, stack211.getStackVersion());
    helper.getOrCreateRepositoryVersion(stack220, stack220.getStackVersion());

    cluster.createClusterVersion(stack211, stack211.getStackVersion(), "admin", RepositoryVersionState.INSTALLING);
    cluster.transitionClusterVersion(stack211, stack211.getStackVersion(), RepositoryVersionState.CURRENT);

    clusters.addHost("h1");
    Host host = clusters.getHost("h1");
    Map<String, String> hostAttributes = new HashMap<String, String>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "6.3");
    host.setHostAttributes(hostAttributes);
    host.setState(HostState.HEALTHY);

    clusters.mapHostToCluster("h1", "c1");

    // add a single ZK server
    Service service = cluster.addService("ZOOKEEPER");
    service.setDesiredStackVersion(cluster.getDesiredStackVersion());

    ServiceComponent component = service.addServiceComponent("ZOOKEEPER_SERVER");
    ServiceComponentHost sch = component.addServiceComponentHost("h1");
    sch.setVersion("2.1.1.0");

    component = service.addServiceComponent("ZOOKEEPER_CLIENT");
    sch = component.addServiceComponentHost("h1");
    sch.setVersion("2.1.1.0");

    topologyManager = injector.getInstance(TopologyManager.class);
    StageUtils.setTopologyManager(topologyManager);
    StageUtils.setConfiguration(injector.getInstance(Configuration.class));
    ActionManager.setTopologyManager(topologyManager);
    EasyMock.replay(injector.getInstance(AuditLogger.class));

    Method isAuthorizedMethod = AuthorizationHelper.class.getMethod("isAuthorized", ResourceType.class, Long.class, Set.class);
    PowerMock.mockStatic(AuthorizationHelper.class, isAuthorizedMethod);
    expect(AuthorizationHelper.isAuthorized(eq(ResourceType.CLUSTER), anyLong(),
        eq(EnumSet.of(RoleAuthorization.CLUSTER_UPGRADE_DOWNGRADE_STACK)))).andReturn(true).anyTimes();
    PowerMock.replay(AuthorizationHelper.class);
  }

  @After
  public void after() {
    injector.getInstance(PersistService.class).stop();
    EasyMock.reset(injector.getInstance(AuditLogger.class));
    injector = null;
  }

  /**
   * Obtain request id from the {@code RequestStatus}
   * @param requestStatus reqult of the {@code createResources}
   * @return id of the request
   */
  private long getRequestId(RequestStatus requestStatus){
    assertEquals(1, requestStatus.getAssociatedResources().size());
    Resource r = requestStatus.getAssociatedResources().iterator().next();
    String id = r.getPropertyValue("Upgrade/request_id").toString();
    return Long.parseLong(id);
  }

  @Test
  public void testCreateResourcesWithAutoSkipFailures() throws Exception {
    Cluster cluster = clusters.getCluster("c1");

    Map<String, Object> requestProps = new HashMap<String, Object>();
    requestProps.put(UpgradeResourceProvider.UPGRADE_CLUSTER_NAME, "c1");
    requestProps.put(UpgradeResourceProvider.UPGRADE_VERSION, "2.2.0.0");
    requestProps.put(UpgradeResourceProvider.UPGRADE_PACK, "upgrade_test");
    requestProps.put(UpgradeResourceProvider.UPGRADE_TYPE, UpgradeType.ROLLING.toString());
    requestProps.put(UpgradeResourceProvider.UPGRADE_SKIP_FAILURES, Boolean.TRUE.toString());
    requestProps.put(UpgradeResourceProvider.UPGRADE_SKIP_SC_FAILURES, Boolean.TRUE.toString());
    requestProps.put(UpgradeResourceProvider.UPGRADE_SKIP_MANUAL_VERIFICATION, Boolean.FALSE.toString());
    requestProps.put(UpgradeResourceProvider.UPGRADE_SKIP_PREREQUISITE_CHECKS, Boolean.TRUE.toString());

    ResourceProvider upgradeResourceProvider = createProvider(amc);
    Request request = PropertyHelper.getCreateRequest(Collections.singleton(requestProps), null);
    upgradeResourceProvider.createResources(request);

    List<UpgradeEntity> upgrades = upgradeDao.findUpgrades(cluster.getClusterId());
    assertEquals(1, upgrades.size());

    UpgradeEntity entity = upgrades.get(0);
    assertEquals(cluster.getClusterId(), entity.getClusterId().longValue());

    List<UpgradeGroupEntity> upgradeGroups = entity.getUpgradeGroups();
    assertEquals(3, upgradeGroups.size());

    UpgradeGroupEntity preClusterGroup = upgradeGroups.get(0);
    assertEquals("PRE_CLUSTER", preClusterGroup.getName());

    List<UpgradeItemEntity> preClusterUpgradeItems = preClusterGroup.getItems();
    assertEquals(2, preClusterUpgradeItems.size());
    assertEquals("Foo", parseSingleMessage(preClusterUpgradeItems.get(0).getText()));
    assertEquals("Foo", parseSingleMessage(preClusterUpgradeItems.get(1).getText()));

    UpgradeGroupEntity zookeeperGroup = upgradeGroups.get(1);
    assertEquals("ZOOKEEPER", zookeeperGroup.getName());

    List<UpgradeItemEntity> zookeeperUpgradeItems = zookeeperGroup.getItems();
    assertEquals(5, zookeeperUpgradeItems.size());

    assertEquals("This is a manual task with a placeholder of placeholder-rendered-properly",
        parseSingleMessage(zookeeperUpgradeItems.get(0).getText()));
    assertEquals("Restarting ZooKeeper Server on h1", zookeeperUpgradeItems.get(1).getText());
    assertEquals("Updating configuration zookeeper-newconfig",
        zookeeperUpgradeItems.get(2).getText());
    assertEquals("Service Check ZooKeeper", zookeeperUpgradeItems.get(3).getText());
    assertEquals("Verifying Skipped Failures", zookeeperUpgradeItems.get(4).getText());

    // the last upgrade item is the skipped failure check
    UpgradeItemEntity skippedFailureCheck = zookeeperUpgradeItems.get(zookeeperUpgradeItems.size() - 1);
    skippedFailureCheck.getTasks().contains(AutoSkipFailedSummaryAction.class.getName());

    UpgradeGroupEntity postClusterGroup = upgradeGroups.get(2);
    assertEquals("POST_CLUSTER", postClusterGroup.getName());

    List<UpgradeItemEntity> postClusterUpgradeItems = postClusterGroup.getItems();
    assertEquals(2, postClusterUpgradeItems.size());
    assertEquals("Please confirm you are ready to finalize", parseSingleMessage(postClusterUpgradeItems.get(0).getText()));
    assertEquals("Save Cluster State", postClusterUpgradeItems.get(1).getText());
  }

  @Test
  public void testCreateResourcesWithAutoSkipManualVerification() throws Exception {
    Cluster cluster = clusters.getCluster("c1");

    Map<String, Object> requestProps = new HashMap<String, Object>();
    requestProps.put(UpgradeResourceProvider.UPGRADE_CLUSTER_NAME, "c1");
    requestProps.put(UpgradeResourceProvider.UPGRADE_VERSION, "2.2.0.0");
    requestProps.put(UpgradeResourceProvider.UPGRADE_PACK, "upgrade_test");
    requestProps.put(UpgradeResourceProvider.UPGRADE_TYPE, UpgradeType.ROLLING.toString());
    requestProps.put(UpgradeResourceProvider.UPGRADE_SKIP_MANUAL_VERIFICATION, Boolean.TRUE.toString());
    requestProps.put(UpgradeResourceProvider.UPGRADE_SKIP_PREREQUISITE_CHECKS, Boolean.TRUE.toString());

    ResourceProvider upgradeResourceProvider = createProvider(amc);
    Request request = PropertyHelper.getCreateRequest(Collections.singleton(requestProps), null);
    upgradeResourceProvider.createResources(request);

    List<UpgradeEntity> upgrades = upgradeDao.findUpgrades(cluster.getClusterId());
    assertEquals(1, upgrades.size());

    UpgradeEntity entity = upgrades.get(0);
    assertEquals(cluster.getClusterId(), entity.getClusterId().longValue());

    List<UpgradeGroupEntity> upgradeGroups = entity.getUpgradeGroups();
    assertEquals(2, upgradeGroups.size());

    UpgradeGroupEntity zookeeperGroup = upgradeGroups.get(0);
    assertEquals("ZOOKEEPER", zookeeperGroup.getName());

    List<UpgradeItemEntity> zookeeperUpgradeItems = zookeeperGroup.getItems();
    assertEquals(3, zookeeperUpgradeItems.size());
    assertEquals("Restarting ZooKeeper Server on h1", zookeeperUpgradeItems.get(0).getText());
    assertEquals("Updating configuration zookeeper-newconfig",
        zookeeperUpgradeItems.get(1).getText());
    assertEquals("Service Check ZooKeeper", zookeeperUpgradeItems.get(2).getText());

    UpgradeGroupEntity postClusterGroup = upgradeGroups.get(1);
    assertEquals("POST_CLUSTER", postClusterGroup.getName());

    List<UpgradeItemEntity> postClusterUpgradeItems = postClusterGroup.getItems();
    assertEquals(1, postClusterUpgradeItems.size());
    assertEquals("Save Cluster State", postClusterUpgradeItems.get(0).getText());
  }

  @Test
  public void testCreateResourcesWithAutoSkipAll() throws Exception {
    Cluster cluster = clusters.getCluster("c1");

    Map<String, Object> requestProps = new HashMap<String, Object>();
    requestProps.put(UpgradeResourceProvider.UPGRADE_CLUSTER_NAME, "c1");
    requestProps.put(UpgradeResourceProvider.UPGRADE_VERSION, "2.2.0.0");
    requestProps.put(UpgradeResourceProvider.UPGRADE_PACK, "upgrade_test");
    requestProps.put(UpgradeResourceProvider.UPGRADE_TYPE, UpgradeType.ROLLING.toString());
    requestProps.put(UpgradeResourceProvider.UPGRADE_SKIP_FAILURES, Boolean.TRUE.toString());
    requestProps.put(UpgradeResourceProvider.UPGRADE_SKIP_SC_FAILURES, Boolean.TRUE.toString());
    requestProps.put(UpgradeResourceProvider.UPGRADE_SKIP_MANUAL_VERIFICATION, Boolean.TRUE.toString());
    requestProps.put(UpgradeResourceProvider.UPGRADE_SKIP_PREREQUISITE_CHECKS, Boolean.TRUE.toString());

    ResourceProvider upgradeResourceProvider = createProvider(amc);
    Request request = PropertyHelper.getCreateRequest(Collections.singleton(requestProps), null);
    upgradeResourceProvider.createResources(request);

    List<UpgradeEntity> upgrades = upgradeDao.findUpgrades(cluster.getClusterId());
    assertEquals(1, upgrades.size());

    UpgradeEntity entity = upgrades.get(0);
    assertEquals(cluster.getClusterId(), entity.getClusterId().longValue());

    List<UpgradeGroupEntity> upgradeGroups = entity.getUpgradeGroups();
    assertEquals(2, upgradeGroups.size());

    UpgradeGroupEntity zookeeperGroup = upgradeGroups.get(0);
    assertEquals("ZOOKEEPER", zookeeperGroup.getName());

    List<UpgradeItemEntity> zookeeperUpgradeItems = zookeeperGroup.getItems();
    assertEquals(4, zookeeperUpgradeItems.size());

    assertEquals("Restarting ZooKeeper Server on h1", zookeeperUpgradeItems.get(0).getText());
    assertEquals("Updating configuration zookeeper-newconfig",
        zookeeperUpgradeItems.get(1).getText());
    assertEquals("Service Check ZooKeeper", zookeeperUpgradeItems.get(2).getText());
    assertEquals("Verifying Skipped Failures", zookeeperUpgradeItems.get(3).getText());

    // the last upgrade item is the skipped failure check
    UpgradeItemEntity skippedFailureCheck = zookeeperUpgradeItems.get(zookeeperUpgradeItems.size() - 1);
    skippedFailureCheck.getTasks().contains(AutoSkipFailedSummaryAction.class.getName());

    UpgradeGroupEntity postClusterGroup = upgradeGroups.get(1);
    assertEquals("POST_CLUSTER", postClusterGroup.getName());

    List<UpgradeItemEntity> postClusterUpgradeItems = postClusterGroup.getItems();
    assertEquals(1, postClusterUpgradeItems.size());
    assertEquals("Save Cluster State", postClusterUpgradeItems.get(0).getText());
  }

  @Test
  public void testGetResources() throws Exception {
    RequestStatus status = testCreateResources();

    Set<Resource> createdResources = status.getAssociatedResources();
    assertEquals(1, createdResources.size());
    Resource res = createdResources.iterator().next();
    Long id = (Long) res.getPropertyValue("Upgrade/request_id");
    assertNotNull(id);
    assertEquals(Long.valueOf(1), id);

    // upgrade
    Set<String> propertyIds = new HashSet<String>();
    propertyIds.add("Upgrade");

    Predicate predicate = new PredicateBuilder()
      .property(UpgradeResourceProvider.UPGRADE_REQUEST_ID).equals("1").and()
      .property(UpgradeResourceProvider.UPGRADE_CLUSTER_NAME).equals("c1")
      .toPredicate();
    Request request = PropertyHelper.getReadRequest(propertyIds);

    ResourceProvider upgradeResourceProvider = createProvider(amc);
    Set<Resource> resources = upgradeResourceProvider.getResources(request, predicate);

    assertEquals(1, resources.size());
    res = resources.iterator().next();
    assertNotNull(res.getPropertyValue("Upgrade/progress_percent"));
    assertNotNull(res.getPropertyValue(UpgradeResourceProvider.UPGRADE_DIRECTION));
    assertEquals(Direction.UPGRADE, res.getPropertyValue(UpgradeResourceProvider.UPGRADE_DIRECTION));
    assertEquals(false, res.getPropertyValue(UpgradeResourceProvider.UPGRADE_SKIP_FAILURES));
    assertEquals(false, res.getPropertyValue(UpgradeResourceProvider.UPGRADE_SKIP_SC_FAILURES));
    assertEquals(UpgradeType.ROLLING, res.getPropertyValue(UpgradeResourceProvider.UPGRADE_TYPE));

    // upgrade groups
    propertyIds.clear();
    propertyIds.add("UpgradeGroup");

    predicate = new PredicateBuilder()
      .property(UpgradeGroupResourceProvider.UPGRADE_REQUEST_ID).equals("1").and()
      .property(UpgradeGroupResourceProvider.UPGRADE_CLUSTER_NAME).equals("c1")
      .toPredicate();
    request = PropertyHelper.getReadRequest(propertyIds);

    ResourceProvider upgradeGroupResourceProvider = new UpgradeGroupResourceProvider(amc);
    resources = upgradeGroupResourceProvider.getResources(request, predicate);

    assertEquals(3, resources.size());
    res = resources.iterator().next();
    assertNotNull(res.getPropertyValue("UpgradeGroup/status"));
    assertNotNull(res.getPropertyValue("UpgradeGroup/group_id"));
    assertNotNull(res.getPropertyValue("UpgradeGroup/total_task_count"));
    assertNotNull(res.getPropertyValue("UpgradeGroup/in_progress_task_count"));
    assertNotNull(res.getPropertyValue("UpgradeGroup/completed_task_count"));

    // upgrade items
    propertyIds.clear();
    propertyIds.add("UpgradeItem");

    predicate = new PredicateBuilder()
      .property(UpgradeItemResourceProvider.UPGRADE_GROUP_ID).equals("1").and()
      .property(UpgradeItemResourceProvider.UPGRADE_REQUEST_ID).equals("1").and()
      .property(UpgradeItemResourceProvider.UPGRADE_CLUSTER_NAME).equals("c1")
      .toPredicate();
    request = PropertyHelper.getReadRequest(propertyIds);

    ResourceProvider upgradeItemResourceProvider = new UpgradeItemResourceProvider(amc);
    resources = upgradeItemResourceProvider.getResources(request, predicate);

    assertEquals(2, resources.size());

    res = resources.iterator().next();
    assertNotNull(res.getPropertyValue("UpgradeItem/status"));

    // !!! check for manual stage vs item text
    propertyIds.clear();
    propertyIds.add("UpgradeItem");

    predicate = new PredicateBuilder()
      .property(UpgradeItemResourceProvider.UPGRADE_GROUP_ID).equals("3").and()
      .property(UpgradeItemResourceProvider.UPGRADE_REQUEST_ID).equals("1").and()
      .property(UpgradeItemResourceProvider.UPGRADE_CLUSTER_NAME).equals("c1")
      .toPredicate();
    request = PropertyHelper.getReadRequest(propertyIds);

    upgradeItemResourceProvider = new UpgradeItemResourceProvider(amc);
    resources = upgradeItemResourceProvider.getResources(request, predicate);
    assertEquals(2, resources.size());
    res = resources.iterator().next();

    assertEquals("Confirm Finalize", res.getPropertyValue("UpgradeItem/context"));
    String msgStr = res.getPropertyValue("UpgradeItem/text").toString();
    JsonParser parser = new JsonParser();
    JsonArray msgArray = (JsonArray) parser.parse(msgStr);
    JsonObject msg = (JsonObject) msgArray.get(0);

    assertTrue(msg.get("message").getAsString().startsWith("Please confirm"));
  }

  /**
   * Tests that retrieving an upgrade correctly populates less common upgrade
   * options correctly.
   */
  @Test
  public void testGetResourcesWithSpecialOptions() throws Exception {
    Cluster cluster = clusters.getCluster("c1");

    List<UpgradeEntity> upgrades = upgradeDao.findUpgrades(cluster.getClusterId());
    assertEquals(0, upgrades.size());

    Map<String, Object> requestProps = new HashMap<String, Object>();
    requestProps.put(UpgradeResourceProvider.UPGRADE_CLUSTER_NAME, "c1");
    requestProps.put(UpgradeResourceProvider.UPGRADE_VERSION, "2.1.1.1");
    requestProps.put(UpgradeResourceProvider.UPGRADE_PACK, "upgrade_test");
    requestProps.put(UpgradeResourceProvider.UPGRADE_SKIP_PREREQUISITE_CHECKS, "true");

    // tests skipping SC failure options
    requestProps.put(UpgradeResourceProvider.UPGRADE_SKIP_FAILURES, "true");
    requestProps.put(UpgradeResourceProvider.UPGRADE_SKIP_SC_FAILURES, "true");

    ResourceProvider upgradeResourceProvider = createProvider(amc);

    Request request = PropertyHelper.getCreateRequest(Collections.singleton(requestProps), null);
    RequestStatus status = upgradeResourceProvider.createResources(request);
    assertNotNull(status);

    // upgrade
    Set<String> propertyIds = new HashSet<String>();
    propertyIds.add("Upgrade");

    Predicate predicate = new PredicateBuilder()
      .property(UpgradeResourceProvider.UPGRADE_REQUEST_ID).equals("1").and()
      .property(UpgradeResourceProvider.UPGRADE_CLUSTER_NAME).equals("c1")
      .toPredicate();

    request = PropertyHelper.getReadRequest(propertyIds);
    Set<Resource> resources = upgradeResourceProvider.getResources(request, predicate);

    assertEquals(1, resources.size());
    Resource resource = resources.iterator().next();

    assertEquals(true, resource.getPropertyValue(UpgradeResourceProvider.UPGRADE_SKIP_FAILURES));
    assertEquals(true, resource.getPropertyValue(UpgradeResourceProvider.UPGRADE_SKIP_SC_FAILURES));
  }


  @Test
  public void testCreatePartialDowngrade() throws Exception {
    clusters.addHost("h2");
    Host host = clusters.getHost("h2");
    Map<String, String> hostAttributes = new HashMap<String, String>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "6.3");
    host.setHostAttributes(hostAttributes);

    clusters.mapHostToCluster("h2", "c1");
    Cluster cluster = clusters.getCluster("c1");
    Service service = cluster.getService("ZOOKEEPER");

    // this should get skipped
    ServiceComponent component = service.getServiceComponent("ZOOKEEPER_SERVER");
    ServiceComponentHost sch = component.addServiceComponentHost("h2");
    sch.setVersion("2.2.2.2");

    // start out with 0 (sanity check)
    List<UpgradeEntity> upgrades = upgradeDao.findUpgrades(cluster.getClusterId());
    assertEquals(0, upgrades.size());

    // a downgrade MUST have an upgrade to come from, so populate an upgrade in
    // the DB
    RequestEntity requestEntity = new RequestEntity();
    requestEntity.setRequestId(2L);
    requestEntity.setClusterId(cluster.getClusterId());
    requestEntity.setStatus(HostRoleStatus.PENDING);
    requestEntity.setStages(new ArrayList<StageEntity>());
    requestDao.create(requestEntity);

    UpgradeEntity upgradeEntity = new UpgradeEntity();
    upgradeEntity.setClusterId(cluster.getClusterId());
    upgradeEntity.setDirection(Direction.UPGRADE);
    upgradeEntity.setFromVersion("2.1.1.1");
    upgradeEntity.setToVersion("2.2.2.2");
    upgradeEntity.setUpgradePackage("upgrade_test");
    upgradeEntity.setUpgradeType(UpgradeType.ROLLING);
    upgradeEntity.setRequestId(2L);

    upgradeDao.create(upgradeEntity);
    upgrades = upgradeDao.findUpgrades(cluster.getClusterId());
    assertEquals(1, upgrades.size());

    UpgradeEntity lastUpgrade = upgradeDao.findLastUpgradeForCluster(cluster.getClusterId(), Direction.UPGRADE);
    assertNotNull(lastUpgrade);

    Map<String, Object> requestProps = new HashMap<String, Object>();
    requestProps.put(UpgradeResourceProvider.UPGRADE_CLUSTER_NAME, "c1");
    requestProps.put(UpgradeResourceProvider.UPGRADE_VERSION, "2.1.1.1");
    requestProps.put(UpgradeResourceProvider.UPGRADE_PACK, "upgrade_test");
    requestProps.put(UpgradeResourceProvider.UPGRADE_SKIP_PREREQUISITE_CHECKS, "true");

    Map<String, String> requestInfoProperties = new HashMap<String, String>();
    requestInfoProperties.put(UpgradeResourceDefinition.DOWNGRADE_DIRECTIVE, "true");

    ResourceProvider upgradeResourceProvider = createProvider(amc);

    Request request = PropertyHelper.getCreateRequest(Collections.singleton(requestProps), requestInfoProperties);
    upgradeResourceProvider.createResources(request);

    upgrades = upgradeDao.findUpgrades(cluster.getClusterId());
    assertEquals(2, upgrades.size());

    UpgradeEntity downgrade = upgrades.get(1);
    assertEquals(cluster.getClusterId(), downgrade.getClusterId().longValue());

    List<UpgradeGroupEntity> upgradeGroups = downgrade.getUpgradeGroups();
    assertEquals(3, upgradeGroups.size());

    UpgradeGroupEntity group = upgradeGroups.get(1);
    assertEquals("ZOOKEEPER", group.getName());
    assertEquals(4, group.getItems().size());

  }


  @Test
  public void testDowngradeToBase() throws Exception {
    Cluster cluster = clusters.getCluster("c1");

    Map<String, Object> requestProps = new HashMap<>();
    requestProps.put(UpgradeResourceProvider.UPGRADE_CLUSTER_NAME, "c1");
    requestProps.put(UpgradeResourceProvider.UPGRADE_VERSION, "2.1.1.1");
    requestProps.put(UpgradeResourceProvider.UPGRADE_PACK, "upgrade_test");
    requestProps.put(UpgradeResourceProvider.UPGRADE_SKIP_PREREQUISITE_CHECKS, "true");

    ResourceProvider upgradeResourceProvider = createProvider(amc);

    Request request = PropertyHelper.getCreateRequest(Collections.singleton(requestProps), null);
    upgradeResourceProvider.createResources(request);

    List<UpgradeEntity> upgrades = upgradeDao.findUpgrades(cluster.getClusterId());
    assertEquals(1, upgrades.size());

    requestProps = new HashMap<>();
    requestProps.put(UpgradeResourceProvider.UPGRADE_CLUSTER_NAME, "c1");
    requestProps.put(UpgradeResourceProvider.UPGRADE_VERSION, "2.2");
    requestProps.put(UpgradeResourceProvider.UPGRADE_PACK, "upgrade_test");
    requestProps.put(UpgradeResourceProvider.UPGRADE_SKIP_PREREQUISITE_CHECKS, "true");
    request = PropertyHelper.getCreateRequest(Collections.singleton(requestProps), null);
    try {
      upgradeResourceProvider.createResources(request);
      Assert.fail("Expected an exception going downgrade with no upgrade pack");
    } catch (Exception e) {
      // !!! expected
    }

    requestProps.put(UpgradeResourceProvider.UPGRADE_CLUSTER_NAME, "c1");
    requestProps.put(UpgradeResourceProvider.UPGRADE_VERSION, "2.2.0.0");
    requestProps.put(UpgradeResourceProvider.UPGRADE_PACK, "upgrade_test");
    requestProps.put(UpgradeResourceProvider.UPGRADE_SKIP_PREREQUISITE_CHECKS, "true");
    requestProps.put(UpgradeResourceProvider.UPGRADE_FROM_VERSION, "2.1.1.0");

    Map<String, String> requestInfoProperties = new HashMap<>();
    requestInfoProperties.put(UpgradeResourceDefinition.DOWNGRADE_DIRECTIVE, "true");

    request = PropertyHelper.getCreateRequest(Collections.singleton(requestProps), requestInfoProperties);
    RequestStatus status = upgradeResourceProvider.createResources(request);
    assertEquals(1, status.getAssociatedResources().size());
    Resource r = status.getAssociatedResources().iterator().next();
    String id = r.getPropertyValue("Upgrade/request_id").toString();

    UpgradeEntity entity = upgradeDao.findUpgrade(Long.parseLong(id));
    assertNotNull(entity);
    assertEquals("2.1.1.0", entity.getFromVersion());
    assertEquals("2.2.0.0", entity.getToVersion());
    assertEquals(Direction.DOWNGRADE, entity.getDirection());

    StageDAO dao = injector.getInstance(StageDAO.class);
    List<StageEntity> stages = dao.findByRequestId(entity.getRequestId());

    Gson gson = new Gson();
    for (StageEntity se : stages) {
      Map<String, String> map = gson.<Map<String, String>>fromJson(se.getCommandParamsStage(), Map.class);
      assertTrue(map.containsKey("upgrade_direction"));
      assertEquals("downgrade", map.get("upgrade_direction"));
    }

  }



  /**
   * Test Downgrade from the partially completed upgrade
   */
  @Test
  public void testNotFullDowngrade() throws Exception {
    Cluster cluster = clusters.getCluster("c1");

    // add additional service for the test
    Service service = cluster.addService("HIVE");
    service.setDesiredStackVersion(cluster.getDesiredStackVersion());

    ServiceComponent component = service.addServiceComponent("HIVE_SERVER");
    ServiceComponentHost sch = component.addServiceComponentHost("h1");
    sch.setVersion("2.1.1.0");

    // create upgrade request
    Map<String, Object> requestProps = new HashMap<>();
    requestProps.put(UpgradeResourceProvider.UPGRADE_CLUSTER_NAME, "c1");
    requestProps.put(UpgradeResourceProvider.UPGRADE_VERSION, "2.2.0.0");
    requestProps.put(UpgradeResourceProvider.UPGRADE_PACK, "upgrade_nonrolling_new_stack");
    requestProps.put(UpgradeResourceProvider.UPGRADE_TYPE, "NON_ROLLING");
    requestProps.put(UpgradeResourceProvider.UPGRADE_SKIP_PREREQUISITE_CHECKS, "true");

    ResourceProvider upgradeResourceProvider = createProvider(amc);

    Request request = PropertyHelper.getCreateRequest(Collections.singleton(requestProps), null);
    upgradeResourceProvider.createResources(request);

    // check that upgrade was created and groups for the tested services are on place
    List<UpgradeEntity> upgrades = upgradeDao.findUpgrades(cluster.getClusterId());
    assertEquals(1, upgrades.size());

    List<UpgradeGroupEntity> groups = upgrades.get(0).getUpgradeGroups();
    boolean isHiveGroupFound = false;
    boolean isZKGroupFound = false;

    // look only for testing groups
    for (UpgradeGroupEntity group: groups) {
      if (group.getName().equalsIgnoreCase("hive")) {
        isHiveGroupFound = true;
      } else if (group.getName().equalsIgnoreCase("zookeeper")){
        isZKGroupFound = true;
      }
    }

    assertTrue(isHiveGroupFound);
    assertTrue(isZKGroupFound);

    isHiveGroupFound = false;
    isZKGroupFound = false;
    sch.setVersion("2.2.0.0");

    // create downgrade with one upgraded service
    StackId stackId = new StackId("HDP", "2.2.0");
    cluster.setDesiredStackVersion(stackId, true);

    requestProps.put(UpgradeResourceProvider.UPGRADE_CLUSTER_NAME, "c1");
    requestProps.put(UpgradeResourceProvider.UPGRADE_VERSION, "2.1.1.0");
    requestProps.put(UpgradeResourceProvider.UPGRADE_PACK, "upgrade_nonrolling_new_stack");
    requestProps.put(UpgradeResourceProvider.UPGRADE_SKIP_PREREQUISITE_CHECKS, "true");
    requestProps.put(UpgradeResourceProvider.UPGRADE_FROM_VERSION, "2.2.0.0");

    Map<String, String> requestInfoProperties = new HashMap<>();
    requestInfoProperties.put(UpgradeResourceDefinition.DOWNGRADE_DIRECTIVE, "true");

    request = PropertyHelper.getCreateRequest(Collections.singleton(requestProps), requestInfoProperties);
    RequestStatus status = upgradeResourceProvider.createResources(request);
    UpgradeEntity upgradeEntity = upgradeDao.findUpgradeByRequestId(getRequestId(status));

    for (UpgradeGroupEntity group: upgradeEntity.getUpgradeGroups()) {
      if (group.getName().equalsIgnoreCase("hive")) {
        isHiveGroupFound = true;
      } else if (group.getName().equalsIgnoreCase("zookeeper")){
        isZKGroupFound = true;
      }
    }

    // as services not updated, nothing to downgrade
    assertTrue(isHiveGroupFound);
    assertFalse(isZKGroupFound);
  }


  @Test
  public void testAbort() throws Exception {
    RequestStatus status = testCreateResources();

    Set<Resource> createdResources = status.getAssociatedResources();
    assertEquals(1, createdResources.size());
    Resource res = createdResources.iterator().next();
    Long id = (Long) res.getPropertyValue("Upgrade/request_id");
    assertNotNull(id);
    assertEquals(Long.valueOf(1), id);

    Map<String, Object> requestProps = new HashMap<String, Object>();
    requestProps.put(UpgradeResourceProvider.UPGRADE_REQUEST_ID, id.toString());
    requestProps.put(UpgradeResourceProvider.UPGRADE_CLUSTER_NAME, "c1");
    requestProps.put(UpgradeResourceProvider.UPGRADE_REQUEST_STATUS, "ABORTED");
    requestProps.put(UpgradeResourceProvider.UPGRADE_SUSPENDED, "true");

    UpgradeResourceProvider urp = createProvider(amc);

    // !!! make sure we can.  actual abort is tested elsewhere
    Request req = PropertyHelper.getUpdateRequest(requestProps, null);
    urp.updateResources(req, null);
  }


  @Test
  public void testRetry() throws Exception {
    RequestStatus status = testCreateResources();

    Set<Resource> createdResources = status.getAssociatedResources();
    assertEquals(1, createdResources.size());
    Resource res = createdResources.iterator().next();
    Long id = (Long) res.getPropertyValue("Upgrade/request_id");
    assertNotNull(id);
    assertEquals(Long.valueOf(1), id);

    Map<String, Object> requestProps = new HashMap<String, Object>();
    requestProps.put(UpgradeResourceProvider.UPGRADE_REQUEST_ID, id.toString());
    requestProps.put(UpgradeResourceProvider.UPGRADE_CLUSTER_NAME, "c1");
    requestProps.put(UpgradeResourceProvider.UPGRADE_REQUEST_STATUS, "ABORTED");
    requestProps.put(UpgradeResourceProvider.UPGRADE_SUSPENDED, "true");

    UpgradeResourceProvider urp = createProvider(amc);

    // !!! make sure we can.  actual abort is tested elsewhere
    Request req = PropertyHelper.getUpdateRequest(requestProps, null);
    urp.updateResources(req, null);

    ActionManager am = injector.getInstance(ActionManager.class);

    List<HostRoleCommand> commands = am.getRequestTasks(id);

    boolean foundOne = false;
    for (HostRoleCommand hrc : commands) {
      if (hrc.getRole().equals(Role.AMBARI_SERVER_ACTION)) {
        assertEquals(-1L, hrc.getHostId());
        assertNull(hrc.getHostName());
        foundOne = true;
      }
    }
    assertTrue("Expected at least one server-side action", foundOne);

    HostRoleCommand cmd = commands.get(commands.size()-1);

    HostRoleCommandDAO dao = injector.getInstance(HostRoleCommandDAO.class);
    HostRoleCommandEntity entity = dao.findByPK(cmd.getTaskId());
    entity.setStatus(HostRoleStatus.ABORTED);
    dao.merge(entity);

    requestProps = new HashMap<String, Object>();
    requestProps.put(UpgradeResourceProvider.UPGRADE_REQUEST_ID, id.toString());
    requestProps.put(UpgradeResourceProvider.UPGRADE_REQUEST_STATUS, "PENDING");
    requestProps.put(UpgradeResourceProvider.UPGRADE_CLUSTER_NAME, "c1");
    requestProps.put(UpgradeResourceProvider.UPGRADE_SUSPENDED, "false");

    // !!! make sure we can.  actual reset is tested elsewhere
    req = PropertyHelper.getUpdateRequest(requestProps, null);
    urp.updateResources(req, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAbortWithoutSuspendFlag() throws Exception {
    RequestStatus status = testCreateResources();

    Set<Resource> createdResources = status.getAssociatedResources();
    assertEquals(1, createdResources.size());
    Resource res = createdResources.iterator().next();
    Long id = (Long) res.getPropertyValue("Upgrade/request_id");
    assertNotNull(id);
    assertEquals(Long.valueOf(1), id);

    Map<String, Object> requestProps = new HashMap<String, Object>();
    requestProps.put(UpgradeResourceProvider.UPGRADE_REQUEST_ID, id.toString());
    requestProps.put(UpgradeResourceProvider.UPGRADE_CLUSTER_NAME, "c1");
    requestProps.put(UpgradeResourceProvider.UPGRADE_REQUEST_STATUS, "ABORTED");

    UpgradeResourceProvider urp = createProvider(amc);
    Request req = PropertyHelper.getUpdateRequest(requestProps, null);
    urp.updateResources(req, null);
  }

  @Test
  public void testDirectionUpgrade() throws Exception {
    Cluster cluster = clusters.getCluster("c1");

    StackEntity stackEntity = stackDAO.find("HDP", "2.1.1");
    RepositoryVersionEntity repoVersionEntity = new RepositoryVersionEntity();
    repoVersionEntity.setDisplayName("My New Version 3");
    repoVersionEntity.setOperatingSystems("");
    repoVersionEntity.setStack(stackEntity);
    repoVersionEntity.setVersion("2.2.2.3");
    repoVersionDao.create(repoVersionEntity);

    Map<String, Object> requestProps = new HashMap<String, Object>();
    requestProps.put(UpgradeResourceProvider.UPGRADE_CLUSTER_NAME, "c1");
    requestProps.put(UpgradeResourceProvider.UPGRADE_VERSION, "2.2.2.3");
    requestProps.put(UpgradeResourceProvider.UPGRADE_PACK, "upgrade_direction");
    requestProps.put(UpgradeResourceProvider.UPGRADE_SKIP_PREREQUISITE_CHECKS, "true");

    ResourceProvider upgradeResourceProvider = createProvider(amc);

    Request request = PropertyHelper.getCreateRequest(Collections.singleton(requestProps), null);
    upgradeResourceProvider.createResources(request);

    List<UpgradeEntity> upgrades = upgradeDao.findUpgrades(cluster.getClusterId());
    assertEquals(1, upgrades.size());

    UpgradeEntity upgrade = upgrades.get(0);
    Long id = upgrade.getRequestId();
    assertEquals(3, upgrade.getUpgradeGroups().size());
    // Ensure that there are no items related to downgrade in the upgrade direction
    UpgradeGroupEntity group = upgrade.getUpgradeGroups().get(2);
    Assert.assertEquals("POST_CLUSTER", group.getName());
    Assert.assertTrue(!group.getItems().isEmpty());
    for (UpgradeItemEntity item : group.getItems()) {
      Assert.assertFalse(item.getText().toLowerCase().contains("downgrade"));
    }


    requestProps.clear();
    // Now perform a downgrade
    requestProps.put(UpgradeResourceProvider.UPGRADE_CLUSTER_NAME, "c1");
    requestProps.put(UpgradeResourceProvider.UPGRADE_VERSION, "2.2");
    requestProps.put(UpgradeResourceProvider.UPGRADE_PACK, "upgrade_direction");
    requestProps.put(UpgradeResourceProvider.UPGRADE_SKIP_PREREQUISITE_CHECKS, "true");
    requestProps.put(UpgradeResourceProvider.UPGRADE_FROM_VERSION, "2.2.2.3");

    Map<String, String> requestInfoProps = new HashMap<String, String>();
    requestInfoProps.put("downgrade", "true");

    request = PropertyHelper.getCreateRequest(Collections.singleton(requestProps), requestInfoProps);
    upgradeResourceProvider.createResources(request);

    upgrades = upgradeDao.findUpgrades(cluster.getClusterId());
    assertEquals(2, upgrades.size());

    upgrade = null;
    for (UpgradeEntity u : upgrades) {
      if (!u.getRequestId().equals(id)) {
        upgrade = u;
      }
    }
    assertNotNull(upgrade);
    assertEquals("Downgrade groups reduced from 3 to 2", 2, upgrade.getUpgradeGroups().size());
    group = upgrade.getUpgradeGroups().get(1);
    assertEquals("Execution items increased from 1 to 2", 2, group.getItems().size());
  }



  @Test
  public void testPercents() throws Exception {
    RequestStatus status = testCreateResources();

    Set<Resource> createdResources = status.getAssociatedResources();
    assertEquals(1, createdResources.size());
    Resource res = createdResources.iterator().next();
    Long id = (Long) res.getPropertyValue("Upgrade/request_id");
    assertNotNull(id);
    assertEquals(Long.valueOf(1), id);

    StageDAO stageDao = injector.getInstance(StageDAO.class);
    HostRoleCommandDAO hrcDao = injector.getInstance(HostRoleCommandDAO.class);

    List<StageEntity> stages = stageDao.findByRequestId(id);
    List<HostRoleCommandEntity> tasks = hrcDao.findByRequest(id);

    Set<Long> stageIds = new HashSet<Long>();
    for (StageEntity se : stages) {
      stageIds.add(se.getStageId());
    }

    CalculatedStatus calc = null;
    int i = 0;
    for (HostRoleCommandEntity hrce : tasks) {
      hrce.setStatus(HostRoleStatus.IN_PROGRESS);
      hrcDao.merge(hrce);
      calc = CalculatedStatus.statusFromStageSummary(hrcDao.findAggregateCounts(id), stageIds);
      assertEquals(((i++) + 1) * 4.375d, calc.getPercent(), 0.01d);
      assertEquals(HostRoleStatus.IN_PROGRESS, calc.getStatus());
    }

    i = 0;
    for (HostRoleCommandEntity hrce : tasks) {
      hrce.setStatus(HostRoleStatus.COMPLETED);
      hrcDao.merge(hrce);
      calc = CalculatedStatus.statusFromStageSummary(hrcDao.findAggregateCounts(id), stageIds);
      assertEquals(35 + (((i++) + 1) * 8.125), calc.getPercent(), 0.01d);
      if (i < 8) {
        assertEquals(HostRoleStatus.IN_PROGRESS, calc.getStatus());
      }
    }

    calc = CalculatedStatus.statusFromStageSummary(hrcDao.findAggregateCounts(id), stageIds);
    assertEquals(HostRoleStatus.COMPLETED, calc.getStatus());
    assertEquals(100d, calc.getPercent(), 0.01d);
  }


  @Test
  public void testCreateCrossStackUpgrade() throws Exception {
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


    Config config = configFactory.createNew(cluster, "zoo.cfg", "abcdefg", Collections.singletonMap("a", "b"), null);
    cluster.addDesiredConfig("admin", Collections.singleton(config));

    Map<String, Object> requestProps = new HashMap<String, Object>();
    requestProps.put(UpgradeResourceProvider.UPGRADE_CLUSTER_NAME, "c1");
    requestProps.put(UpgradeResourceProvider.UPGRADE_VERSION, "2.2.0.0");
    requestProps.put(UpgradeResourceProvider.UPGRADE_PACK, "upgrade_test");
    requestProps.put(UpgradeResourceProvider.UPGRADE_SKIP_PREREQUISITE_CHECKS, "true");

    ResourceProvider upgradeResourceProvider = createProvider(amc);

    Request request = PropertyHelper.getCreateRequest(Collections.singleton(requestProps), null);
    upgradeResourceProvider.createResources(request);

    List<UpgradeEntity> upgrades = upgradeDao.findUpgrades(cluster.getClusterId());
    assertEquals(1, upgrades.size());

    UpgradeEntity upgrade = upgrades.get(0);
    assertEquals(3, upgrade.getUpgradeGroups().size());

    UpgradeGroupEntity group = upgrade.getUpgradeGroups().get(2);
    assertEquals(2, group.getItems().size());

    group = upgrade.getUpgradeGroups().get(0);
    assertEquals(2, group.getItems().size());

    assertTrue(cluster.getDesiredConfigs().containsKey("zoo.cfg"));

    StackId newStack = cluster.getDesiredStackVersion();

    assertFalse(oldStack.equals(newStack));

    for (Service s : cluster.getServices().values()) {
      assertEquals(newStack, s.getDesiredStackVersion());

      for (ServiceComponent sc : s.getServiceComponents().values()) {
        assertEquals(newStack, sc.getDesiredStackVersion());

        for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
          assertEquals(newStack, sch.getDesiredStackVersion());
        }
      }
    }
  }

  /**
   * Tests merging configurations between existing and new stack values on
   * upgrade.
   *
   * @throws Exception
   */
  @Test
  public void testMergeConfigurations() throws Exception {
    StackId stack211 = new StackId("HDP-2.1.1");
    StackId stack220 = new StackId("HDP-2.2.0");

    Map<String, Map<String, String>> stack211Configs = new HashMap<String, Map<String, String>>();
    Map<String, String> stack211FooType = new HashMap<String, String>();
    Map<String, String> stack211BarType = new HashMap<String, String>();
    Map<String, String> stack211BazType = new HashMap<String, String>();
    stack211Configs.put("foo-site", stack211FooType);
    stack211Configs.put("bar-site", stack211BarType);
    stack211Configs.put("baz-site", stack211BazType);
    stack211FooType.put("1", "one");
    stack211FooType.put("11", "one-one");
    stack211BarType.put("2", "two");
    stack211BazType.put("3", "three");

    Map<String, Map<String, String>> stack220Configs = new HashMap<String, Map<String, String>>();
    Map<String, String> stack220FooType = new HashMap<String, String>();
    Map<String, String> stack220BazType = new HashMap<String, String>();
    Map<String, String> stack220FlumeEnvType = new HashMap<String, String>();
    stack220Configs.put("foo-site", stack220FooType);
    stack220Configs.put("baz-site", stack220BazType);
    stack220Configs.put("flume-env", stack220FlumeEnvType);
    stack220FooType.put("1", "one-new");
    stack220FooType.put("111", "one-one-one");
    stack220BazType.put("3", "three-new");
    stack220FlumeEnvType.put("flume_env_key", "flume-env-value");

    Map<String, String> clusterFooType = new HashMap<String, String>();
    Map<String, String> clusterBarType = new HashMap<String, String>();
    Map<String, String> clusterBazType = new HashMap<String, String>();

    Config fooConfig = EasyMock.createNiceMock(Config.class);
    Config barConfig = EasyMock.createNiceMock(Config.class);
    Config bazConfig = EasyMock.createNiceMock(Config.class);

    clusterFooType.put("1", "one");
    clusterFooType.put("11", "one-one");
    clusterBarType.put("2", "two");
    clusterBazType.put("3", "three-changed");

    expect(fooConfig.getProperties()).andReturn(clusterFooType);
    expect(barConfig.getProperties()).andReturn(clusterBarType);
    expect(bazConfig.getProperties()).andReturn(clusterBazType);

    Map<String, DesiredConfig> desiredConfigurations = new HashMap<String, DesiredConfig>();
    desiredConfigurations.put("foo-site", null);
    desiredConfigurations.put("bar-site", null);
    desiredConfigurations.put("baz-site", null);

    Cluster cluster = EasyMock.createNiceMock(Cluster.class);
    expect(cluster.getCurrentStackVersion()).andReturn(stack211);
    expect(cluster.getDesiredStackVersion()).andReturn(stack220);
    expect(cluster.getDesiredConfigs()).andReturn(desiredConfigurations);
    expect(cluster.getDesiredConfigByType("foo-site")).andReturn(fooConfig);
    expect(cluster.getDesiredConfigByType("bar-site")).andReturn(barConfig);
    expect(cluster.getDesiredConfigByType("baz-site")).andReturn(bazConfig);

    // setup the config helper for placeholder resolution
    EasyMock.reset(configHelper);

    expect(
        configHelper.getDefaultProperties(EasyMock.eq(stack211), EasyMock.anyObject(Cluster.class), EasyMock.anyBoolean())).andReturn(
        stack211Configs).anyTimes();

    expect(
        configHelper.getDefaultProperties(EasyMock.eq(stack220), EasyMock.anyObject(Cluster.class), EasyMock.anyBoolean())).andReturn(
        stack220Configs).anyTimes();

    Capture<Map<String, Map<String, String>>> expectedConfigurationsCapture = EasyMock.newCapture();

    configHelper.createConfigTypes(EasyMock.anyObject(Cluster.class),
        EasyMock.anyObject(AmbariManagementController.class),
        EasyMock.capture(expectedConfigurationsCapture),
        EasyMock.anyObject(String.class), EasyMock.anyObject(String.class));

    EasyMock.expectLastCall().once();

    EasyMock.replay(configHelper, cluster, fooConfig, barConfig, bazConfig);

    UpgradeResourceProvider upgradeResourceProvider = createProvider(amc);

    Map<String, UpgradePack> upgradePacks = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    UpgradePack upgrade = upgradePacks.get("upgrade_to_new_stack");
    upgradeResourceProvider.applyStackAndProcessConfigurations(stack211.getStackName(), cluster, "2.2.0.0", Direction.UPGRADE, upgrade, "admin");

    Map<String, Map<String, String>> expectedConfigurations = expectedConfigurationsCapture.getValue();
    Map<String, String> expectedFooType = expectedConfigurations.get("foo-site");
    Map<String, String> expectedBarType = expectedConfigurations.get("bar-site");
    Map<String, String> expectedBazType = expectedConfigurations.get("baz-site");

    // As the upgrade pack did not have any Flume updates, its configs should not be updated.
    assertFalse(expectedConfigurations.containsKey("flume-env"));

    // the really important values are one-new and three-changed; one-new
    // indicates that the new stack value is changed since it was not customized
    // while three-changed represents that the customized value was preserved
    // even though the stack value changed
    assertEquals("one-new", expectedFooType.get("1"));
    assertEquals("one-one", expectedFooType.get("11"));
    assertEquals("two", expectedBarType.get("2"));
    assertEquals("three-changed", expectedBazType.get("3"));
  }

  /**
   * @param amc
   * @return the provider
   */
  private UpgradeResourceProvider createProvider(AmbariManagementController amc) {
    return new UpgradeResourceProvider(amc);
  }

  private RequestStatus testCreateResources() throws Exception {

    Cluster cluster = clusters.getCluster("c1");

    List<UpgradeEntity> upgrades = upgradeDao.findUpgrades(cluster.getClusterId());
    assertEquals(0, upgrades.size());

    Map<String, Object> requestProps = new HashMap<String, Object>();
    requestProps.put(UpgradeResourceProvider.UPGRADE_CLUSTER_NAME, "c1");
    requestProps.put(UpgradeResourceProvider.UPGRADE_VERSION, "2.1.1.1");
    requestProps.put(UpgradeResourceProvider.UPGRADE_PACK, "upgrade_test");
    requestProps.put(UpgradeResourceProvider.UPGRADE_SKIP_PREREQUISITE_CHECKS, "true");

    ResourceProvider upgradeResourceProvider = createProvider(amc);

    Request request = PropertyHelper.getCreateRequest(Collections.singleton(requestProps), null);
    RequestStatus status = upgradeResourceProvider.createResources(request);

    upgrades = upgradeDao.findUpgrades(cluster.getClusterId());
    assertEquals(1, upgrades.size());

    UpgradeEntity entity = upgrades.get(0);
    assertEquals(cluster.getClusterId(), entity.getClusterId().longValue());
    assertEquals(UpgradeType.ROLLING, entity.getUpgradeType());

    StageDAO stageDAO = injector.getInstance(StageDAO.class);
    List<StageEntity> stageEntities = stageDAO.findByRequestId(entity.getRequestId());
    Gson gson = new Gson();
    for (StageEntity se : stageEntities) {
      Map<String, String> map = gson.<Map<String, String>> fromJson(se.getCommandParamsStage(),Map.class);
      assertTrue(map.containsKey("upgrade_direction"));
      assertEquals("upgrade", map.get("upgrade_direction"));

      if(map.containsKey("upgrade_type")){
        assertEquals("rolling_upgrade", map.get("upgrade_type"));
      }
    }

    List<UpgradeGroupEntity> upgradeGroups = entity.getUpgradeGroups();
    assertEquals(3, upgradeGroups.size());

    UpgradeGroupEntity group = upgradeGroups.get(1);
    assertEquals(4, group.getItems().size());

    assertTrue(
        group.getItems().get(0).getText().contains("placeholder of placeholder-rendered-properly"));

    assertTrue(group.getItems().get(1).getText().contains("Restarting"));
    assertTrue(group.getItems().get(2).getText().contains("Updating"));
    assertTrue(group.getItems().get(3).getText().contains("Service Check"));

    ActionManager am = injector.getInstance(ActionManager.class);
    List<Long> requests = am.getRequestsByStatus(
        org.apache.ambari.server.actionmanager.RequestStatus.IN_PROGRESS, 100, true);

    assertEquals(1, requests.size());
    assertEquals(requests.get(0), entity.getRequestId());

    List<Stage> stages = am.getRequestStatus(requests.get(0).longValue());

    assertEquals(8, stages.size());

    List<HostRoleCommand> tasks = am.getRequestTasks(requests.get(0).longValue());
    // same number of tasks as stages here
    assertEquals(8, tasks.size());

    Set<Long> slaveStageIds = new HashSet<Long>();

    UpgradeGroupEntity coreSlavesGroup = upgradeGroups.get(1);

    for (UpgradeItemEntity itemEntity : coreSlavesGroup.getItems()) {
      slaveStageIds.add(itemEntity.getStageId());
    }

    for (Stage stage : stages) {

      // For this test the core slaves group stages should be skippable and NOT
      // allow retry.
      assertEquals(slaveStageIds.contains(stage.getStageId()), stage.isSkippable());

      for (Map<String, HostRoleCommand> taskMap : stage.getHostRoleCommands().values()) {

        for (HostRoleCommand task : taskMap.values()) {
          assertEquals(!slaveStageIds.contains(stage.getStageId()), task.isRetryAllowed());
        }
      }
    }
    return status;
  }

  @Test
  public void testUpdateSkipFailures() throws Exception {
    testCreateResourcesWithAutoSkipFailures();

    List<UpgradeEntity> upgrades = upgradeDao.findUpgrades(1);
    assertEquals(1, upgrades.size());

    UpgradeEntity entity = upgrades.get(0);

    HostRoleCommandDAO dao = injector.getInstance(HostRoleCommandDAO.class);

    List<HostRoleCommandEntity> tasks = dao.findByRequest(entity.getRequestId());
    for (HostRoleCommandEntity task : tasks) {
      StageEntity stage = task.getStage();
      if (stage.isSkippable() && stage.isAutoSkipOnFailureSupported()) {
        assertTrue(task.isFailureAutoSkipped());
      } else {
        assertFalse(task.isFailureAutoSkipped());
      }
    }

    Map<String, Object> requestProps = new HashMap<String, Object>();
    requestProps.put(UpgradeResourceProvider.UPGRADE_CLUSTER_NAME, "c1");
    requestProps.put(UpgradeResourceProvider.UPGRADE_VERSION, "2.2.0.0");
    requestProps.put(UpgradeResourceProvider.UPGRADE_SKIP_FAILURES, Boolean.TRUE.toString());
    requestProps.put(UpgradeResourceProvider.UPGRADE_SKIP_SC_FAILURES, Boolean.FALSE.toString());
    requestProps.put(UpgradeResourceProvider.UPGRADE_REQUEST_ID, "" + entity.getRequestId());

    ResourceProvider upgradeResourceProvider = createProvider(amc);
    Request request = PropertyHelper.getUpdateRequest(requestProps, null);
    upgradeResourceProvider.updateResources(request, null);

    tasks = dao.findByRequest(entity.getRequestId());
    for (HostRoleCommandEntity task : tasks) {
      if (task.getRoleCommand() == RoleCommand.SERVICE_CHECK) {
        assertFalse(task.isFailureAutoSkipped());
      } else {
        StageEntity stage = task.getStage();
        if (stage.isSkippable() && stage.isAutoSkipOnFailureSupported()) {
          assertTrue(task.isFailureAutoSkipped());
        } else {
          assertFalse(task.isFailureAutoSkipped());
        }
      }
    }

    requestProps = new HashMap<String, Object>();
    requestProps.put(UpgradeResourceProvider.UPGRADE_CLUSTER_NAME, "c1");
    requestProps.put(UpgradeResourceProvider.UPGRADE_VERSION, "2.2.0.0");
    requestProps.put(UpgradeResourceProvider.UPGRADE_SKIP_FAILURES, Boolean.FALSE.toString());
    requestProps.put(UpgradeResourceProvider.UPGRADE_SKIP_SC_FAILURES, Boolean.TRUE.toString());
    requestProps.put(UpgradeResourceProvider.UPGRADE_REQUEST_ID, "" + entity.getRequestId());

    request = PropertyHelper.getUpdateRequest(requestProps, null);
    upgradeResourceProvider.updateResources(request, null);

    tasks = dao.findByRequest(entity.getRequestId());
    for (HostRoleCommandEntity task : tasks) {
      if (task.getRoleCommand() == RoleCommand.SERVICE_CHECK) {
        StageEntity stage = task.getStage();
        if (stage.isSkippable() && stage.isAutoSkipOnFailureSupported()) {
          assertTrue(task.isFailureAutoSkipped());
        } else {
          assertFalse(task.isFailureAutoSkipped());
        }
      } else {
        assertFalse(task.isFailureAutoSkipped());
      }
    }

    requestProps = new HashMap<String, Object>();
    requestProps.put(UpgradeResourceProvider.UPGRADE_CLUSTER_NAME, "c1");
    requestProps.put(UpgradeResourceProvider.UPGRADE_VERSION, "2.2.0.0");
    requestProps.put(UpgradeResourceProvider.UPGRADE_SKIP_FAILURES, Boolean.FALSE.toString());
    requestProps.put(UpgradeResourceProvider.UPGRADE_SKIP_SC_FAILURES, Boolean.FALSE.toString());
    requestProps.put(UpgradeResourceProvider.UPGRADE_REQUEST_ID, "" + entity.getRequestId());

    request = PropertyHelper.getUpdateRequest(requestProps, null);
    upgradeResourceProvider.updateResources(request, null);

    tasks = dao.findByRequest(entity.getRequestId());
    for (HostRoleCommandEntity task : tasks) {
      assertFalse(task.isFailureAutoSkipped());
    }
  }

  /**
   * Tests that an error while commiting the data cleanly rolls back the transaction so that
   * no request/stage/tasks are created.
   *
   * @throws Exception
   */
  @Test
  public void testRollback() throws Exception {
    Cluster cluster = clusters.getCluster("c1");

    Map<String, Object> requestProps = new HashMap<String, Object>();
    requestProps.put(UpgradeResourceProvider.UPGRADE_CLUSTER_NAME, "c1");
    requestProps.put(UpgradeResourceProvider.UPGRADE_VERSION, "2.2.0.0");
    requestProps.put(UpgradeResourceProvider.UPGRADE_PACK, "upgrade_test");
    requestProps.put(UpgradeResourceProvider.UPGRADE_TYPE, UpgradeType.ROLLING.toString());
    requestProps.put(UpgradeResourceProvider.UPGRADE_SKIP_MANUAL_VERIFICATION, Boolean.FALSE.toString());
    requestProps.put(UpgradeResourceProvider.UPGRADE_SKIP_PREREQUISITE_CHECKS, Boolean.TRUE.toString());

    // this will cause a NPE when creating the upgrade, allowing us to test
    // rollback
    UpgradeResourceProvider upgradeResourceProvider = createProvider(amc);
    upgradeResourceProvider.s_upgradeDAO = null;

    try {
      Request request = PropertyHelper.getCreateRequest(Collections.singleton(requestProps), null);
      upgradeResourceProvider.createResources(request);
      Assert.fail("Expected a NullPointerException");
    } catch (NullPointerException npe) {
      // expected
    }

    List<UpgradeEntity> upgrades = upgradeDao.findUpgrades(cluster.getClusterId());
    assertEquals(0, upgrades.size());

    List<Long> requestIds = requestDao.findAllRequestIds(1, true, cluster.getClusterId());
    assertEquals(0, requestIds.size());
  }

  /**
   * Tests that a {@link UpgradeType#HOST_ORDERED} upgrade throws an exception
   * on missing hosts.
   *
   * @throws Exception
   */
  @Test()
  public void testCreateHostOrderedUpgradeThrowsExceptions() throws Exception {
    Cluster cluster = clusters.getCluster("c1");

    Map<String, Object> requestProps = new HashMap<String, Object>();
    requestProps.put(UpgradeResourceProvider.UPGRADE_CLUSTER_NAME, "c1");
    requestProps.put(UpgradeResourceProvider.UPGRADE_VERSION, "2.2.0.0");
    requestProps.put(UpgradeResourceProvider.UPGRADE_PACK, "upgrade_test_host_ordered");
    requestProps.put(UpgradeResourceProvider.UPGRADE_TYPE, UpgradeType.HOST_ORDERED.toString());
    requestProps.put(UpgradeResourceProvider.UPGRADE_SKIP_PREREQUISITE_CHECKS, Boolean.TRUE.toString());

    ResourceProvider upgradeResourceProvider = createProvider(amc);
    Request request = PropertyHelper.getCreateRequest(Collections.singleton(requestProps), null);

    try {
      upgradeResourceProvider.createResources(request);
      Assert.fail("The request should have failed due to the missing Upgrade/host_order property");
    } catch( SystemException systemException ){
      // expected
    }

    // stick a bad host_ordered_hosts in there which has the wrong hosts
    Set<Map<String, List<String>>> hostsOrder = new LinkedHashSet<>();
    Map<String, List<String>> hostGrouping = new HashMap<>();
    hostGrouping.put("hosts", Lists.newArrayList("invalid-host"));
    hostsOrder.add(hostGrouping);

    requestProps.put(UpgradeResourceProvider.UPGRADE_HOST_ORDERED_HOSTS, hostsOrder);

    try {
      upgradeResourceProvider.createResources(request);
      Assert.fail("The request should have failed due to invalid hosts");
    } catch (SystemException systemException) {
      // expected
    }

    // use correct hosts now
    hostsOrder = new LinkedHashSet<>();
    hostGrouping = new HashMap<>();
    hostGrouping.put("hosts", Lists.newArrayList("h1"));
    hostsOrder.add(hostGrouping);

    requestProps.put(UpgradeResourceProvider.UPGRADE_HOST_ORDERED_HOSTS, hostsOrder);
    upgradeResourceProvider.createResources(request);
  }


  private String parseSingleMessage(String msgStr){
    JsonParser parser = new JsonParser();
    JsonArray msgArray = (JsonArray) parser.parse(msgStr);
    JsonObject msg = (JsonObject) msgArray.get(0);

    return msg.get("message").getAsString();
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
