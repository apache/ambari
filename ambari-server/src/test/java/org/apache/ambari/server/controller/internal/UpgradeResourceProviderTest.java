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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.actionmanager.RequestStatus;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.api.resources.UpgradeResourceDefinition;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.dao.StageDAO;
import org.apache.ambari.server.orm.dao.UpgradeDAO;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.orm.entities.StageEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.orm.entities.UpgradeGroupEntity;
import org.apache.ambari.server.orm.entities.UpgradeItemEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.ConfigImpl;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.stack.upgrade.Direction;
import org.apache.ambari.server.topology.TopologyManager;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.ambari.server.view.ViewRegistry;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
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
public class UpgradeResourceProviderTest {

  private UpgradeDAO upgradeDao = null;
  private RepositoryVersionDAO repoVersionDao = null;
  private Injector injector;
  private Clusters clusters;
  private OrmTestHelper helper;
  private AmbariManagementController amc;
  private ConfigHelper configHelper;
  private StackDAO stackDAO;

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
            EasyMock.anyObject(Cluster.class))).andReturn(
        new HashMap<String, Map<String, String>>()).anyTimes();


    EasyMock.replay(configHelper);

    // create an injector which will inject the mocks
    injector = Guice.createInjector(Modules.override(
        new InMemoryDefaultTestModule()).with(new MockModule()));

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

    StackEntity stackEntity = stackDAO.find("HDP", "2.1.1");

    RepositoryVersionEntity repoVersionEntity = new RepositoryVersionEntity();
    repoVersionEntity.setDisplayName("My New Version 1");
    repoVersionEntity.setOperatingSystems("");
    repoVersionEntity.setStack(stackEntity);
    repoVersionEntity.setUpgradePackage("upgrade_test");
    repoVersionEntity.setVersion("2.1.1.0");
    repoVersionDao.create(repoVersionEntity);

    repoVersionEntity = new RepositoryVersionEntity();
    repoVersionEntity.setDisplayName("My New Version 2");
    repoVersionEntity.setOperatingSystems("");
    repoVersionEntity.setStack(stackEntity);
    repoVersionEntity.setUpgradePackage("upgrade_test");
    repoVersionEntity.setVersion("2.1.1.1");
    repoVersionDao.create(repoVersionEntity);

    repoVersionEntity = new RepositoryVersionEntity();
    repoVersionEntity.setDisplayName("For Stack Version 2.2.0");
    repoVersionEntity.setOperatingSystems("");
    repoVersionEntity.setStack(stackDAO.find("HDP", "2.2.0"));
    repoVersionEntity.setUpgradePackage("upgrade_test");
    repoVersionEntity.setVersion("2.2.0.0");
    repoVersionDao.create(repoVersionEntity);

    clusters = injector.getInstance(Clusters.class);

    StackId stackId = new StackId("HDP-2.1.1");
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

    // add a single ZK server
    Service service = cluster.addService("ZOOKEEPER");
    service.setDesiredStackVersion(cluster.getDesiredStackVersion());
    service.persist();

    ServiceComponent component = service.addServiceComponent("ZOOKEEPER_SERVER");
    ServiceComponentHost sch = component.addServiceComponentHost("h1");
    sch.setVersion("2.1.1.0");

    component = service.addServiceComponent("ZOOKEEPER_CLIENT");
    sch = component.addServiceComponentHost("h1");
    sch.setVersion("2.1.1.0");

    TopologyManager topologyManager = new TopologyManager();
    StageUtils.setTopologyManager(topologyManager);
    ActionManager.setTopologyManager(topologyManager);
  }

  @After
  public void after() {
    injector.getInstance(PersistService.class).stop();
    injector = null;
  }

  private org.apache.ambari.server.controller.spi.RequestStatus testCreateResources() throws Exception {

    Cluster cluster = clusters.getCluster("c1");

    List<UpgradeEntity> upgrades = upgradeDao.findUpgrades(cluster.getClusterId());
    assertEquals(0, upgrades.size());

    Map<String, Object> requestProps = new HashMap<String, Object>();
    requestProps.put(UpgradeResourceProvider.UPGRADE_CLUSTER_NAME, "c1");
    requestProps.put(UpgradeResourceProvider.UPGRADE_VERSION, "2.1.1.1");

    ResourceProvider upgradeResourceProvider = createProvider(amc);

    Request request = PropertyHelper.getCreateRequest(Collections.singleton(requestProps), null);
    org.apache.ambari.server.controller.spi.RequestStatus status = upgradeResourceProvider.createResources(request);

    upgrades = upgradeDao.findUpgrades(cluster.getClusterId());
    assertEquals(1, upgrades.size());

    UpgradeEntity entity = upgrades.get(0);
    assertEquals(cluster.getClusterId(), entity.getClusterId().longValue());

    StageDAO stageDAO = injector.getInstance(StageDAO.class);
    List<StageEntity> stageEntities = stageDAO.findByRequestId(entity.getRequestId());
    Gson gson = new Gson();
    for (StageEntity se : stageEntities) {
      Map<String, String> map = gson.<Map<String, String>>fromJson(se.getCommandParamsStage(), Map.class);
      assertTrue(map.containsKey("upgrade_direction"));
      assertEquals("upgrade", map.get("upgrade_direction"));
    }

    List<UpgradeGroupEntity> upgradeGroups = entity.getUpgradeGroups();
    assertEquals(3, upgradeGroups.size());

    UpgradeGroupEntity group = upgradeGroups.get(1);
    assertEquals(4, group.getItems().size());

    assertTrue(group.getItems().get(0).getText().contains(
        "placeholder of placeholder-rendered-properly"));

    assertTrue(group.getItems().get(1).getText().contains("Restarting"));
    assertTrue(group.getItems().get(2).getText().contains("Skipping"));
    assertTrue(group.getItems().get(3).getText().contains("Service Check"));

    ActionManager am = injector.getInstance(ActionManager.class);
    List<Long> requests = am.getRequestsByStatus(RequestStatus.IN_PROGRESS, 100, true);

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

      // For this test the core slaves group stages should be skippable and NOT allow retry.
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
  public void testGetResources() throws Exception {
    org.apache.ambari.server.controller.spi.RequestStatus status = testCreateResources();

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
    assertNotNull(res.getPropertyValue("Upgrade/direction"));
    assertEquals(Direction.UPGRADE, res.getPropertyValue("Upgrade/direction"));

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
    assertTrue(res.getPropertyValue("UpgradeItem/text").toString().startsWith("Please confirm"));
  }

  @Test
  public void testCreatePartialDowngrade() throws Exception {

    clusters.addHost("h2");
    Host host = clusters.getHost("h2");
    Map<String, String> hostAttributes = new HashMap<String, String>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "6.3");
    host.setHostAttributes(hostAttributes);
    host.persist();

    clusters.mapHostToCluster("h2", "c1");
    Cluster cluster = clusters.getCluster("c1");
    Service service = cluster.getService("ZOOKEEPER");

    // this should get skipped
    ServiceComponent component = service.getServiceComponent("ZOOKEEPER_SERVER");
    ServiceComponentHost sch = component.addServiceComponentHost("h2");
    sch.setVersion("2.2.2.2");

    List<UpgradeEntity> upgrades = upgradeDao.findUpgrades(cluster.getClusterId());
    assertEquals(0, upgrades.size());

    Map<String, Object> requestProps = new HashMap<String, Object>();
    requestProps.put(UpgradeResourceProvider.UPGRADE_CLUSTER_NAME, "c1");
    requestProps.put(UpgradeResourceProvider.UPGRADE_VERSION, "2.1.1.1");

    Map<String, String> requestInfoProperties = new HashMap<String, String>();
    requestInfoProperties.put(UpgradeResourceDefinition.DOWNGRADE_DIRECTIVE, "true");

    ResourceProvider upgradeResourceProvider = createProvider(amc);

    Request request = PropertyHelper.getCreateRequest(Collections.singleton(requestProps), requestInfoProperties);
    upgradeResourceProvider.createResources(request);

    upgrades = upgradeDao.findUpgrades(cluster.getClusterId());
    assertEquals(1, upgrades.size());


    UpgradeEntity entity = upgrades.get(0);
    assertEquals(cluster.getClusterId(), entity.getClusterId().longValue());

    List<UpgradeGroupEntity> upgradeGroups = entity.getUpgradeGroups();
    assertEquals(3, upgradeGroups.size());

    UpgradeGroupEntity group = upgradeGroups.get(1);
    assertEquals("ZOOKEEPER", group.getName());
    assertEquals(4, group.getItems().size());

  }

  @SuppressWarnings("unchecked")
  @Test
  public void testDowngradeToBase() throws Exception {
    Cluster cluster = clusters.getCluster("c1");

    Map<String, Object> requestProps = new HashMap<String, Object>();
    requestProps.put(UpgradeResourceProvider.UPGRADE_CLUSTER_NAME, "c1");
    requestProps.put(UpgradeResourceProvider.UPGRADE_VERSION, "2.1.1.1");

    ResourceProvider upgradeResourceProvider = createProvider(amc);

    Request request = PropertyHelper.getCreateRequest(Collections.singleton(requestProps), null);
    org.apache.ambari.server.controller.spi.RequestStatus status = upgradeResourceProvider.createResources(request);

    List<UpgradeEntity> upgrades = upgradeDao.findUpgrades(cluster.getClusterId());
    assertEquals(1, upgrades.size());

    requestProps = new HashMap<String, Object>();
    requestProps.put(UpgradeResourceProvider.UPGRADE_CLUSTER_NAME, "c1");
    requestProps.put(UpgradeResourceProvider.UPGRADE_VERSION, "2.2");
    request = PropertyHelper.getCreateRequest(Collections.singleton(requestProps), null);
    try {
      status = upgradeResourceProvider.createResources(request);
      Assert.fail("Expected an exception going downgrade with no upgrade pack");
    } catch (Exception e) {
      // !!! expected
    }

    requestProps.put(UpgradeResourceProvider.UPGRADE_CLUSTER_NAME, "c1");
    requestProps.put(UpgradeResourceProvider.UPGRADE_VERSION, "2.2");
    requestProps.put(UpgradeResourceProvider.UPGRADE_FROM_VERSION, "2.1.1.0");

    Map<String, String> requestInfoProperties = new HashMap<String, String>();
    requestInfoProperties.put(UpgradeResourceDefinition.DOWNGRADE_DIRECTIVE, "true");

    request = PropertyHelper.getCreateRequest(Collections.singleton(requestProps), requestInfoProperties);
    status = upgradeResourceProvider.createResources(request);
    assertEquals(1, status.getAssociatedResources().size());
    Resource r = status.getAssociatedResources().iterator().next();
    String id = r.getPropertyValue("Upgrade/request_id").toString();

    UpgradeEntity entity = upgradeDao.findUpgrade(Long.parseLong(id));
    assertNotNull(entity);
    assertEquals("2.1.1", entity.getFromVersion());
    assertEquals("2.2", entity.getToVersion());
    assertEquals(Direction.DOWNGRADE, entity.getDirection());

    StageDAO dao = injector.getInstance(StageDAO.class);
    List<StageEntity> stages = dao.findByRequestId(entity.getRequestId());

    Gson gson = new Gson();
    for (StageEntity se : stages) {
      Map<String, String> map = gson.fromJson(se.getCommandParamsStage(), Map.class);
      assertTrue(map.containsKey("upgrade_direction"));
      assertEquals("downgrade", map.get("upgrade_direction"));
    }

  }

  @Test
  public void testAbort() throws Exception {
    org.apache.ambari.server.controller.spi.RequestStatus status = testCreateResources();

    Set<Resource> createdResources = status.getAssociatedResources();
    assertEquals(1, createdResources.size());
    Resource res = createdResources.iterator().next();
    Long id = (Long) res.getPropertyValue("Upgrade/request_id");
    assertNotNull(id);
    assertEquals(Long.valueOf(1), id);

    Map<String, Object> requestProps = new HashMap<String, Object>();
    requestProps.put(UpgradeResourceProvider.UPGRADE_REQUEST_ID, id.toString());
    requestProps.put(UpgradeResourceProvider.UPGRADE_REQUEST_STATUS, "ABORTED");

    UpgradeResourceProvider urp = createProvider(amc);

    // !!! make sure we can.  actual abort is tested elsewhere
    Request req = PropertyHelper.getUpdateRequest(requestProps, null);
    urp.updateResources(req, null);
  }

  @Test
  public void testRetry() throws Exception {
    org.apache.ambari.server.controller.spi.RequestStatus status = testCreateResources();

    Set<Resource> createdResources = status.getAssociatedResources();
    assertEquals(1, createdResources.size());
    Resource res = createdResources.iterator().next();
    Long id = (Long) res.getPropertyValue("Upgrade/request_id");
    assertNotNull(id);
    assertEquals(Long.valueOf(1), id);

    Map<String, Object> requestProps = new HashMap<String, Object>();
    requestProps.put(UpgradeResourceProvider.UPGRADE_REQUEST_ID, id.toString());
    requestProps.put(UpgradeResourceProvider.UPGRADE_REQUEST_STATUS, "ABORTED");

    UpgradeResourceProvider urp = createProvider(amc);

    // !!! make sure we can.  actual abort is tested elsewhere
    Request req = PropertyHelper.getUpdateRequest(requestProps, null);
    urp.updateResources(req, null);

    ActionManager am = injector.getInstance(ActionManager.class);

    List<HostRoleCommand> commands = am.getRequestTasks(id);

    HostRoleCommand cmd = commands.get(commands.size()-1);

    HostRoleCommandDAO dao = injector.getInstance(HostRoleCommandDAO.class);
    HostRoleCommandEntity entity = dao.findByPK(cmd.getTaskId());
    entity.setStatus(HostRoleStatus.ABORTED);
    dao.merge(entity);

    requestProps = new HashMap<String, Object>();
    requestProps.put(UpgradeResourceProvider.UPGRADE_REQUEST_ID, id.toString());
    requestProps.put(UpgradeResourceProvider.UPGRADE_REQUEST_STATUS, "PENDING");

    // !!! make sure we can.  actual reset is tested elsewhere
    req = PropertyHelper.getUpdateRequest(requestProps, null);
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
    repoVersionEntity.setUpgradePackage("upgrade_direction");
    repoVersionEntity.setVersion("2.2.2.3");
    repoVersionDao.create(repoVersionEntity);

    Map<String, Object> requestProps = new HashMap<String, Object>();
    requestProps.put(UpgradeResourceProvider.UPGRADE_CLUSTER_NAME, "c1");
    requestProps.put(UpgradeResourceProvider.UPGRADE_VERSION, "2.2.2.3");

    ResourceProvider upgradeResourceProvider = createProvider(amc);

    Request request = PropertyHelper.getCreateRequest(Collections.singleton(requestProps), null);
    upgradeResourceProvider.createResources(request);

    List<UpgradeEntity> upgrades = upgradeDao.findUpgrades(cluster.getClusterId());
    assertEquals(1, upgrades.size());

    UpgradeEntity upgrade = upgrades.get(0);
    Long id = upgrade.getRequestId();
    assertEquals(3, upgrade.getUpgradeGroups().size());
    UpgradeGroupEntity group = upgrade.getUpgradeGroups().get(2);
    assertEquals(1, group.getItems().size());

    requestProps.clear();
    requestProps.put(UpgradeResourceProvider.UPGRADE_CLUSTER_NAME, "c1");
    requestProps.put(UpgradeResourceProvider.UPGRADE_VERSION, "2.2");
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
    org.apache.ambari.server.controller.spi.RequestStatus status = testCreateResources();

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


    Config config = new ConfigImpl("zoo.cfg");
    config.setProperties(new HashMap<String, String>() {{
      put("a", "b");
    }});
    config.setTag("abcdefg");

    cluster.addConfig(config);
    cluster.addDesiredConfig("admin", Collections.singleton(config));


    Map<String, Object> requestProps = new HashMap<String, Object>();
    requestProps.put(UpgradeResourceProvider.UPGRADE_CLUSTER_NAME, "c1");
    requestProps.put(UpgradeResourceProvider.UPGRADE_VERSION, "2.2.0.0");

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
    UpgradeItemEntity item = group.getItems().get(1);
    assertEquals("Value is set for the source stack upgrade pack",
        "Foo", item.getText());

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
    stack220Configs.put("foo-site", stack220FooType);
    stack220Configs.put("baz-site", stack220BazType);
    stack220FooType.put("1", "one-new");
    stack220FooType.put("111", "one-one-one");
    stack220BazType.put("3", "three-new");

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
        configHelper.getDefaultProperties(EasyMock.eq(stack211), EasyMock.anyObject(Cluster.class))).andReturn(
        stack211Configs).anyTimes();

    expect(
        configHelper.getDefaultProperties(EasyMock.eq(stack220), EasyMock.anyObject(Cluster.class))).andReturn(
        stack220Configs).anyTimes();

    Capture<Map<String, Map<String, String>>> expectedConfigurationsCapture = new Capture<Map<String, Map<String, String>>>();

    configHelper.createConfigTypes(EasyMock.anyObject(Cluster.class),
        EasyMock.anyObject(AmbariManagementController.class),
        EasyMock.capture(expectedConfigurationsCapture),
        EasyMock.anyObject(String.class), EasyMock.anyObject(String.class));

    EasyMock.expectLastCall().once();

    EasyMock.replay(configHelper, cluster, fooConfig, barConfig, bazConfig);

    UpgradeResourceProvider upgradeResourceProvider = createProvider(amc);

    upgradeResourceProvider.processConfigurations(cluster, "2.2.0.0", Direction.UPGRADE);

    Map<String, Map<String, String>> expectedConfigurations = expectedConfigurationsCapture.getValue();
    Map<String, String> expectedFooType = expectedConfigurations.get("foo-site");
    Map<String, String> expectedBarType = expectedConfigurations.get("bar-site");
    Map<String, String> expectedBazType = expectedConfigurations.get("baz-site");

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
