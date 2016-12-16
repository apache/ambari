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
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.ExecutionCommandWrapper;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.agent.ExecutionCommand.KeyNames;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ExecuteActionRequest;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.ResourceProviderFactory;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.ClusterVersionDAO;
import org.apache.ambari.server.orm.dao.HostComponentStateDAO;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.ResourceTypeDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterVersionEntity;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.serveraction.upgrades.FinalizeUpgradeAction;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.RepositoryType;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.ServiceOsSpecific;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.cluster.ClusterImpl;
import org.apache.ambari.server.state.stack.upgrade.Direction;
import org.apache.ambari.server.topology.TopologyManager;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.io.IOUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;
import com.google.inject.util.Modules;


 /**
 * ClusterStackVersionResourceProvider tests.
 */
public class ClusterStackVersionResourceProviderTest {

  public static final int MAX_TASKS_PER_STAGE = 2;
  private Injector injector;
  private AmbariMetaInfo ambariMetaInfo;
  private RepositoryVersionDAO repositoryVersionDAOMock;
  private ResourceTypeDAO resourceTypeDAO;
  private StackDAO stackDAO;
  private ClusterVersionDAO clusterVersionDAO;
  private ConfigHelper configHelper;
  private Configuration configuration;
  private StageFactory stageFactory;

  private HostVersionDAO hostVersionDAO;
  private HostComponentStateDAO hostComponentStateDAO;

  public static final String OS_JSON = "[\n" +
          "   {\n" +
          "      \"repositories\":[\n" +
          "         {\n" +
          "            \"Repositories/base_url\":\"http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos5/2.x/updates/2.2.0.0\",\n" +
          "            \"Repositories/repo_name\":\"HDP-UTILS\",\n" +
          "            \"Repositories/repo_id\":\"HDP-UTILS-1.1.0.20\"\n" +
          "         },\n" +
          "         {\n" +
          "            \"Repositories/base_url\":\"http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos5/2.x/updates/2.2.0.0\",\n" +
          "            \"Repositories/repo_name\":\"HDP\",\n" +
          "            \"Repositories/repo_id\":\"HDP-2.2\"\n" +
          "         }\n" +
          "      ],\n" +
          "      \"OperatingSystems/os_type\":\"redhat6\"\n" +
          "   }\n" +
          "]";

  @Before
  public void setup() throws Exception {
    // Create instances of mocks
    repositoryVersionDAOMock = createNiceMock(RepositoryVersionDAO.class);
    hostVersionDAO = createNiceMock(HostVersionDAO.class);
    hostComponentStateDAO = createNiceMock(HostComponentStateDAO.class);

    configHelper = createNiceMock(ConfigHelper.class);
    InMemoryDefaultTestModule inMemoryModule = new InMemoryDefaultTestModule();
    Properties properties = inMemoryModule.getProperties();
    properties.setProperty(Configuration.AGENT_PACKAGE_PARALLEL_COMMANDS_LIMIT.getKey(),
            String.valueOf(MAX_TASKS_PER_STAGE));
    configuration = new Configuration(properties);
    stageFactory = createNiceMock(StageFactory.class);
    clusterVersionDAO = createNiceMock(ClusterVersionDAO.class);

    // Initialize injector
    injector = Guice.createInjector(Modules.override(inMemoryModule).with(new MockModule()));
    injector.getInstance(GuiceJpaInitializer.class);
    ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    resourceTypeDAO = injector.getInstance(ResourceTypeDAO.class);
    stackDAO = injector.getInstance(StackDAO.class);
  }

  @After
  public void teardown() {
    injector.getInstance(PersistService.class).stop();

    SecurityContextHolder.getContext().setAuthentication(null);
  }

  @Test
  public void testCreateResourcesAsAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createAdministrator());
  }

   @Test
   public void testCreateResourcesAsClusterAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterAdministrator());
  }

   @Test(expected = AuthorizationException.class)
   public void testCreateResourcesAsClusterOperator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterOperator());
  }

  private void testCreateResources(Authentication authentication) throws Exception {
    Resource.Type type = Resource.Type.ClusterStackVersion;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Map<String, String> hostLevelParams = new HashMap<>();
    StackId stackId = new StackId("HDP", "2.0.1");

    RepositoryVersionEntity repoVersion = new RepositoryVersionEntity();
    repoVersion.setId(1l);
    repoVersion.setOperatingSystems(OS_JSON);

    Map<String, Host> hostsForCluster = new HashMap<String, Host>();
    int hostCount = 10;
    for (int i = 0; i < hostCount; i++) {
      String hostname = "host" + i;
      Host host = createNiceMock(hostname, Host.class);
      expect(host.getHostName()).andReturn(hostname).anyTimes();
      expect(host.getOsFamily()).andReturn("redhat6").anyTimes();
      expect(host.getMaintenanceState(EasyMock.anyLong())).andReturn(
          MaintenanceState.OFF).anyTimes();
      expect(host.getAllHostVersions()).andReturn(
          Collections.<HostVersionEntity>emptyList()).anyTimes();

      replay(host);
      hostsForCluster.put(hostname, host);
    }

    final ServiceComponentHost schDatanode = createMock(ServiceComponentHost.class);
    expect(schDatanode.getServiceName()).andReturn("HDFS").anyTimes();
    expect(schDatanode.getServiceComponentName()).andReturn("DATANODE").anyTimes();
    final ServiceComponentHost schNamenode = createMock(ServiceComponentHost.class);
    expect(schNamenode.getServiceName()).andReturn("HDFS").anyTimes();
    expect(schNamenode.getServiceComponentName()).andReturn("NAMENODE").anyTimes();
    final ServiceComponentHost schAMS = createMock(ServiceComponentHost.class);
    expect(schAMS.getServiceName()).andReturn("AMBARI_METRICS").anyTimes();
    expect(schAMS.getServiceComponentName()).andReturn("METRICS_COLLECTOR").anyTimes();
    // First host contains versionable components
    final List<ServiceComponentHost> schsH1 = new ArrayList<ServiceComponentHost>(){{
      add(schDatanode);
      add(schNamenode);
      add(schAMS);
    }};
    // Second host does not contain versionable components
    final List<ServiceComponentHost> schsH2 = new ArrayList<ServiceComponentHost>(){{
      add(schAMS);
    }};


    ServiceOsSpecific.Package hdfsPackage = new ServiceOsSpecific.Package();
    hdfsPackage.setName("hdfs");
    List<ServiceOsSpecific.Package> packages = Collections.singletonList(hdfsPackage);

    ActionManager actionManager = createNiceMock(ActionManager.class);

    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);
    ResourceProvider csvResourceProvider = createNiceMock(ClusterStackVersionResourceProvider.class);

    AbstractControllerResourceProvider.init(resourceProviderFactory);

    Map<String, Map<String, String>> hostConfigTags = new HashMap<String, Map<String, String>>();
    expect(configHelper.getEffectiveDesiredTags(anyObject(ClusterImpl.class), anyObject(String.class))).andReturn(hostConfigTags);

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getAuthName()).andReturn("admin").anyTimes();
    expect(managementController.getActionManager()).andReturn(actionManager).anyTimes();
    expect(managementController.getJdkResourceUrl()).andReturn("/JdkResourceUrl").anyTimes();
    expect(managementController.getPackagesForServiceHost(anyObject(ServiceInfo.class),
            EasyMock.<Map<String, String>>anyObject(), anyObject(String.class))).
            andReturn(packages).times((hostCount - 1) * 2); // 1 host has no versionable components, other hosts have 2 services
//            // that's why we don't send commands to it

    expect(resourceProviderFactory.getHostResourceProvider(EasyMock.<Set<String>>anyObject(), EasyMock.<Map<Resource.Type, String>>anyObject(),
            eq(managementController))).andReturn(csvResourceProvider).anyTimes();

    expect(clusters.getCluster(anyObject(String.class))).andReturn(cluster);
    expect(clusters.getHostsForCluster(anyObject(String.class))).andReturn(
        hostsForCluster).anyTimes();

    String clusterName = "Cluster100";
    expect(cluster.getClusterId()).andReturn(1L).anyTimes();
    expect(cluster.getHosts()).andReturn(hostsForCluster.values()).atLeastOnce();
    expect(cluster.getServices()).andReturn(new HashMap<String, Service>()).anyTimes();
    expect(cluster.getCurrentStackVersion()).andReturn(stackId);
    expect(cluster.getServiceComponentHosts(anyObject(String.class))).andAnswer(new IAnswer<List<ServiceComponentHost>>() {
      @Override
      public List<ServiceComponentHost> answer() throws Throwable {
        String hostname = (String) EasyMock.getCurrentArguments()[0];
        if (hostname.equals("host2")) {
          return schsH2;
        } else {
          return schsH1;
        }
      }
    }).anyTimes();

    ExecutionCommand executionCommand = createNiceMock(ExecutionCommand.class);
    ExecutionCommandWrapper executionCommandWrapper = createNiceMock(ExecutionCommandWrapper.class);

    expect(executionCommandWrapper.getExecutionCommand()).andReturn(executionCommand).anyTimes();

    Stage stage = createNiceMock(Stage.class);
    expect(stage.getExecutionCommandWrapper(anyObject(String.class), anyObject(String.class))).
            andReturn(executionCommandWrapper).anyTimes();

    expect(executionCommand.getHostLevelParams()).andReturn(hostLevelParams).anyTimes();

    Map<Role, Float> successFactors = new HashMap<>();
    expect(stage.getSuccessFactors()).andReturn(successFactors).atLeastOnce();

    // Check that we create proper stage count
    expect(stageFactory.createNew(anyLong(), anyObject(String.class),
            anyObject(String.class), anyLong(),
            anyObject(String.class), anyObject(String.class), anyObject(String.class),
            anyObject(String.class))).andReturn(stage).
            times((int) Math.ceil(hostCount / MAX_TASKS_PER_STAGE));

    expect(
            repositoryVersionDAOMock.findByStackAndVersion(
                    anyObject(StackId.class),
                    anyObject(String.class))).andReturn(repoVersion);

    expect(actionManager.getRequestTasks(anyLong())).andReturn(Collections.<HostRoleCommand>emptyList()).anyTimes();

    ClusterEntity clusterEntity = new ClusterEntity();
    clusterEntity.setClusterId(1l);
    clusterEntity.setClusterName(clusterName);
    ClusterVersionEntity cve = new ClusterVersionEntity(clusterEntity,
            repoVersion, RepositoryVersionState.INSTALL_FAILED, 0, "");
    expect(clusterVersionDAO.findByClusterAndStackAndVersion(anyObject(String.class),
            anyObject(StackId.class), anyObject(String.class))).andReturn(cve);

    StageUtils.setTopologyManager(injector.getInstance(TopologyManager.class));
    StageUtils.setConfiguration(injector.getInstance(Configuration.class));

    expect(clusterVersionDAO.findByCluster(anyObject(String.class))).andReturn(Collections.<ClusterVersionEntity>emptyList()).once();

    // replay
    replay(managementController, response, clusters, resourceProviderFactory, csvResourceProvider,
            cluster, repositoryVersionDAOMock, configHelper, schDatanode, schNamenode, schAMS, actionManager,
            executionCommand, executionCommandWrapper,stage, stageFactory, clusterVersionDAO);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    injector.injectMembers(provider);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<Map<String, Object>>();

    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    // add properties to the request map
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_REPOSITORY_VERSION_PROPERTY_ID, "2.2.0.1-885");
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_STACK_PROPERTY_ID, "HDP");
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_VERSION_PROPERTY_ID, "2.1.1");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    RequestStatus status = provider.createResources(request);
    Assert.assertNotNull(status);

    // verify
    verify(managementController, response, clusters, stageFactory, stage);

    // check that the success factor was populated in the stage
    Float successFactor = successFactors.get(Role.INSTALL_PACKAGES);
    Assert.assertEquals(Float.valueOf(0.85f), successFactor);
  }

  @Experimental(feature=ExperimentalFeature.PATCH_UPGRADES)
  @Ignore
  public void testCreateResourcesForPatch() throws Exception {
    Resource.Type type = Resource.Type.ClusterStackVersion;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    StackId stackId = new StackId("HDP", "2.0.1");

    File f = new File("src/test/resources/hbase_version_test.xml");

    RepositoryVersionEntity repoVersion = new RepositoryVersionEntity();
    repoVersion.setId(1l);
    repoVersion.setOperatingSystems(OS_JSON);
    repoVersion.setVersionXml(IOUtils.toString(new FileInputStream(f)));
    repoVersion.setVersionXsd("version_definition.xsd");
    repoVersion.setType(RepositoryType.PATCH);

    ambariMetaInfo.getComponent("HDP", "2.1.1", "HBASE", "HBASE_MASTER").setVersionAdvertised(true);


    Map<String, Host> hostsForCluster = new HashMap<String, Host>();
    int hostCount = 10;
    for (int i = 0; i < hostCount; i++) {
      String hostname = "host" + i;
      Host host = createNiceMock(hostname, Host.class);
      expect(host.getHostName()).andReturn(hostname).anyTimes();
      expect(host.getOsFamily()).andReturn("redhat6").anyTimes();
      expect(host.getMaintenanceState(EasyMock.anyLong())).andReturn(
          MaintenanceState.OFF).anyTimes();
      expect(host.getAllHostVersions()).andReturn(
          Collections.<HostVersionEntity>emptyList()).anyTimes();

      replay(host);
      hostsForCluster.put(hostname, host);
    }

    Service hdfsService = createNiceMock(Service.class);
    Service hbaseService = createNiceMock(Service.class);
    expect(hdfsService.getName()).andReturn("HDFS").anyTimes();
    expect(hbaseService.getName()).andReturn("HBASE").anyTimes();
//    Service metricsService = createNiceMock(Service.class);

    ServiceComponent scNameNode = createNiceMock(ServiceComponent.class);
    ServiceComponent scDataNode = createNiceMock(ServiceComponent.class);
    ServiceComponent scHBaseMaster = createNiceMock(ServiceComponent.class);
    ServiceComponent scMetricCollector = createNiceMock(ServiceComponent.class);

    expect(hdfsService.getServiceComponents()).andReturn(new HashMap<String, ServiceComponent>());
    expect(hbaseService.getServiceComponents()).andReturn(new HashMap<String, ServiceComponent>());
//    expect(metricsService.getServiceComponents()).andReturn(new HashMap<String, ServiceComponent>());


    Map<String, Service> serviceMap = new HashMap<>();
    serviceMap.put("HDFS", hdfsService);
    serviceMap.put("HBASE", hbaseService);


    final ServiceComponentHost schDatanode = createMock(ServiceComponentHost.class);
    expect(schDatanode.getServiceName()).andReturn("HDFS").anyTimes();
    expect(schDatanode.getServiceComponentName()).andReturn("DATANODE").anyTimes();

    final ServiceComponentHost schNamenode = createMock(ServiceComponentHost.class);
    expect(schNamenode.getServiceName()).andReturn("HDFS").anyTimes();
    expect(schNamenode.getServiceComponentName()).andReturn("NAMENODE").anyTimes();

    final ServiceComponentHost schAMS = createMock(ServiceComponentHost.class);
    expect(schAMS.getServiceName()).andReturn("AMBARI_METRICS").anyTimes();
    expect(schAMS.getServiceComponentName()).andReturn("METRICS_COLLECTOR").anyTimes();

    final ServiceComponentHost schHBM = createMock(ServiceComponentHost.class);
    expect(schHBM.getServiceName()).andReturn("HBASE").anyTimes();
    expect(schHBM.getServiceComponentName()).andReturn("HBASE_MASTER").anyTimes();

    // First host contains versionable components
    final List<ServiceComponentHost> schsH1 = Arrays.asList(schDatanode, schNamenode, schAMS);

    // Second host does not contain versionable components
    final List<ServiceComponentHost> schsH2 = Arrays.asList(schAMS);

    // Third host only has hbase
    final List<ServiceComponentHost> schsH3 = Arrays.asList(schHBM);

    ServiceOsSpecific.Package hdfsPackage = new ServiceOsSpecific.Package();
    hdfsPackage.setName("hdfs");

//    ServiceOsSpecific.Package hbasePackage = new ServiceOsSpecific.Package();
//    hbasePackage.setName("hbase");

    List<ServiceOsSpecific.Package> packages = Collections.singletonList(hdfsPackage);

    ActionManager actionManager = createNiceMock(ActionManager.class);

    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);
    ResourceProvider csvResourceProvider = createNiceMock(ClusterStackVersionResourceProvider.class);

    AbstractControllerResourceProvider.init(resourceProviderFactory);

    Map<String, Map<String, String>> hostConfigTags = new HashMap<String, Map<String, String>>();
    expect(configHelper.getEffectiveDesiredTags(anyObject(ClusterImpl.class), anyObject(String.class))).andReturn(hostConfigTags);

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getAuthName()).andReturn("admin").anyTimes();
    expect(managementController.getActionManager()).andReturn(actionManager).anyTimes();
    expect(managementController.getJdkResourceUrl()).andReturn("/JdkResourceUrl").anyTimes();
    expect(managementController.getPackagesForServiceHost(anyObject(ServiceInfo.class),
            EasyMock.<Map<String, String>>anyObject(), anyObject(String.class))).
            andReturn(packages).times(1); // only one host has the versionable component

    expect(resourceProviderFactory.getHostResourceProvider(EasyMock.<Set<String>>anyObject(), EasyMock.<Map<Resource.Type, String>>anyObject(),
            eq(managementController))).andReturn(csvResourceProvider).anyTimes();

    expect(clusters.getCluster(anyObject(String.class))).andReturn(cluster);
    expect(clusters.getHostsForCluster(anyObject(String.class))).andReturn(
        hostsForCluster).anyTimes();

    String clusterName = "Cluster100";
    expect(cluster.getClusterId()).andReturn(1L).anyTimes();
    expect(cluster.getHosts()).andReturn(hostsForCluster.values()).atLeastOnce();
    expect(cluster.getServices()).andReturn(serviceMap).anyTimes();
    expect(cluster.getCurrentStackVersion()).andReturn(stackId);
    expect(cluster.getServiceComponentHosts(anyObject(String.class))).andAnswer(new IAnswer<List<ServiceComponentHost>>() {
      @Override
      public List<ServiceComponentHost> answer() throws Throwable {
        String hostname = (String) EasyMock.getCurrentArguments()[0];
        if (hostname.equals("host2")) {
          return schsH2;
        } else if (hostname.equals("host3")) {
          return schsH3;
        } else {
          return schsH1;
        }
      }
    }).anyTimes();

    ExecutionCommand executionCommand = createNiceMock(ExecutionCommand.class);
    ExecutionCommandWrapper executionCommandWrapper = createNiceMock(ExecutionCommandWrapper.class);

    expect(executionCommandWrapper.getExecutionCommand()).andReturn(executionCommand).anyTimes();

    Stage stage = createNiceMock(Stage.class);
    expect(stage.getExecutionCommandWrapper(anyObject(String.class), anyObject(String.class))).
            andReturn(executionCommandWrapper).anyTimes();

    Map<Role, Float> successFactors = new HashMap<>();
    expect(stage.getSuccessFactors()).andReturn(successFactors).atLeastOnce();

    // Check that we create proper stage count
    expect(stageFactory.createNew(anyLong(), anyObject(String.class),
            anyObject(String.class), anyLong(),
            anyObject(String.class), anyObject(String.class), anyObject(String.class),
            anyObject(String.class))).andReturn(stage).
            times((int) Math.ceil(hostCount / MAX_TASKS_PER_STAGE));

    expect(
            repositoryVersionDAOMock.findByStackAndVersion(
                    anyObject(StackId.class),
                    anyObject(String.class))).andReturn(repoVersion);

    Capture<org.apache.ambari.server.actionmanager.Request> c = Capture.newInstance();
    Capture<ExecuteActionRequest> ear = Capture.newInstance();

    actionManager.sendActions(capture(c), capture(ear));
    expectLastCall().atLeastOnce();
    expect(actionManager.getRequestTasks(anyLong())).andReturn(Collections.<HostRoleCommand>emptyList()).anyTimes();

    ClusterEntity clusterEntity = new ClusterEntity();
    clusterEntity.setClusterId(1l);
    clusterEntity.setClusterName(clusterName);
    ClusterVersionEntity cve = new ClusterVersionEntity(clusterEntity,
            repoVersion, RepositoryVersionState.INSTALL_FAILED, 0, "");
    expect(clusterVersionDAO.findByClusterAndStackAndVersion(anyObject(String.class),
            anyObject(StackId.class), anyObject(String.class))).andReturn(cve);

    TopologyManager topologyManager = injector.getInstance(TopologyManager.class);
    StageUtils.setTopologyManager(topologyManager);

    // replay
    replay(managementController, response, clusters, hdfsService, hbaseService, resourceProviderFactory, csvResourceProvider,
            cluster, repositoryVersionDAOMock, configHelper, schDatanode, schNamenode, schAMS, schHBM, actionManager,
            executionCommand, executionCommandWrapper,stage, stageFactory, clusterVersionDAO);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    injector.injectMembers(provider);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<Map<String, Object>>();

    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    // add properties to the request map
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_REPOSITORY_VERSION_PROPERTY_ID, "2.2.0.1-885");
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_STACK_PROPERTY_ID, "HDP");
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_VERSION_PROPERTY_ID, "2.1.1");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    RequestStatus status = provider.createResources(request);
    Assert.assertNotNull(status);

    // verify
    verify(managementController, response, clusters, stageFactory, stage);

    // check that the success factor was populated in the stage
    Float successFactor = successFactors.get(Role.INSTALL_PACKAGES);
    Assert.assertEquals(Float.valueOf(0.85f), successFactor);
  }

   @Test
   public void testCreateResourcesWithRepoDefinitionAsAdministrator() throws Exception {
     testCreateResourcesWithRepoDefinition(TestAuthenticationFactory.createAdministrator());
   }

   @Test
   public void testCreateResourcesWithRepoDefinitionAsClusterAdministrator() throws Exception {
     testCreateResourcesWithRepoDefinition(TestAuthenticationFactory.createClusterAdministrator());
   }

   @Test(expected = AuthorizationException.class)
   public void testCreateResourcesWithRepoDefinitionAsClusterOperator() throws Exception {
     testCreateResourcesWithRepoDefinition(TestAuthenticationFactory.createClusterOperator());
   }

   private void testCreateResourcesWithRepoDefinition(Authentication authentication) throws Exception {
    Resource.Type type = Resource.Type.ClusterStackVersion;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    StackId stackId = new StackId("HDP", "2.0.1");

    File f = new File("src/test/resources/hbase_version_test.xml");

    RepositoryVersionEntity repoVersion = new RepositoryVersionEntity();
    repoVersion.setId(1l);
    repoVersion.setOperatingSystems(OS_JSON);
    repoVersion.setVersionXml(IOUtils.toString(new FileInputStream(f)));
    repoVersion.setVersionXsd("version_definition.xsd");
    repoVersion.setType(RepositoryType.STANDARD);

    ambariMetaInfo.getComponent("HDP", "2.1.1", "HBASE", "HBASE_MASTER").setVersionAdvertised(true);


    Map<String, Host> hostsForCluster = new HashMap<String, Host>();
    int hostCount = 10;
    for (int i = 0; i < hostCount; i++) {
      String hostname = "host" + i;
      Host host = createNiceMock(hostname, Host.class);
      expect(host.getHostName()).andReturn(hostname).anyTimes();
      expect(host.getOsFamily()).andReturn("redhat6").anyTimes();
      expect(host.getMaintenanceState(EasyMock.anyLong())).andReturn(
          MaintenanceState.OFF).anyTimes();
      expect(host.getAllHostVersions()).andReturn(
          Collections.<HostVersionEntity>emptyList()).anyTimes();

      replay(host);
      hostsForCluster.put(hostname, host);
    }

    Service hdfsService = createNiceMock(Service.class);
    Service hbaseService = createNiceMock(Service.class);
    expect(hdfsService.getName()).andReturn("HDFS").anyTimes();
    expect(hbaseService.getName()).andReturn("HBASE").anyTimes();

    expect(hdfsService.getServiceComponents()).andReturn(new HashMap<String, ServiceComponent>());
    expect(hbaseService.getServiceComponents()).andReturn(new HashMap<String, ServiceComponent>());

    Map<String, Service> serviceMap = new HashMap<>();
    serviceMap.put("HDFS", hdfsService);
    serviceMap.put("HBASE", hbaseService);

    final ServiceComponentHost schDatanode = createMock(ServiceComponentHost.class);
    expect(schDatanode.getServiceName()).andReturn("HDFS").anyTimes();
    expect(schDatanode.getServiceComponentName()).andReturn("DATANODE").anyTimes();

    final ServiceComponentHost schNamenode = createMock(ServiceComponentHost.class);
    expect(schNamenode.getServiceName()).andReturn("HDFS").anyTimes();
    expect(schNamenode.getServiceComponentName()).andReturn("NAMENODE").anyTimes();

    final ServiceComponentHost schHBM = createMock(ServiceComponentHost.class);
    expect(schHBM.getServiceName()).andReturn("HBASE").anyTimes();
    expect(schHBM.getServiceComponentName()).andReturn("HBASE_MASTER").anyTimes();

    // First host contains versionable components
    final List<ServiceComponentHost> schsH1 = Arrays.asList(schDatanode, schNamenode);

    // Second host contains versionable components
    final List<ServiceComponentHost> schsH2 = Arrays.asList(schDatanode);

    // Third host only has hbase
    final List<ServiceComponentHost> schsH3 = Arrays.asList(schHBM);

    ServiceOsSpecific.Package hdfsPackage = new ServiceOsSpecific.Package();
    hdfsPackage.setName("hdfs");

    List<ServiceOsSpecific.Package> packages = Collections.singletonList(hdfsPackage);

    ActionManager actionManager = createNiceMock(ActionManager.class);

    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);
    ResourceProvider csvResourceProvider = createNiceMock(ClusterStackVersionResourceProvider.class);

    AbstractControllerResourceProvider.init(resourceProviderFactory);

    Map<String, Map<String, String>> hostConfigTags = new HashMap<String, Map<String, String>>();
    expect(configHelper.getEffectiveDesiredTags(anyObject(ClusterImpl.class), anyObject(String.class))).andReturn(hostConfigTags);

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getAuthName()).andReturn("admin").anyTimes();
    expect(managementController.getActionManager()).andReturn(actionManager).anyTimes();
    expect(managementController.getJdkResourceUrl()).andReturn("/JdkResourceUrl").anyTimes();
    expect(managementController.getPackagesForServiceHost(anyObject(ServiceInfo.class),
            EasyMock.<Map<String, String>>anyObject(), anyObject(String.class))).
            andReturn(packages).anyTimes(); // only one host has the versionable component

    expect(resourceProviderFactory.getHostResourceProvider(EasyMock.<Set<String>>anyObject(), EasyMock.<Map<Resource.Type, String>>anyObject(),
            eq(managementController))).andReturn(csvResourceProvider).anyTimes();

    expect(clusters.getCluster(anyObject(String.class))).andReturn(cluster);
    expect(clusters.getHostsForCluster(anyObject(String.class))).andReturn(
        hostsForCluster).anyTimes();

    String clusterName = "Cluster100";
    expect(cluster.getClusterId()).andReturn(1L).anyTimes();
    expect(cluster.getHosts()).andReturn(hostsForCluster.values()).atLeastOnce();
    expect(cluster.getServices()).andReturn(serviceMap).anyTimes();
    expect(cluster.getCurrentStackVersion()).andReturn(stackId);
    expect(cluster.getServiceComponentHosts(anyObject(String.class))).andAnswer(new IAnswer<List<ServiceComponentHost>>() {
      @Override
      public List<ServiceComponentHost> answer() throws Throwable {
        String hostname = (String) EasyMock.getCurrentArguments()[0];
        if (hostname.equals("host2")) {
          return schsH2;
        } else if (hostname.equals("host3")) {
          return schsH3;
        } else {
          return schsH1;
        }
      }
    }).anyTimes();

//    ExecutionCommand executionCommand = createNiceMock(ExecutionCommand.class);
    ExecutionCommand executionCommand = new ExecutionCommand();
    ExecutionCommandWrapper executionCommandWrapper = createNiceMock(ExecutionCommandWrapper.class);

//    expect(executionCommand.getHostLevelParams()).andReturn(new HashMap<String, String>()).atLeastOnce();
    expect(executionCommandWrapper.getExecutionCommand()).andReturn(executionCommand).anyTimes();

    Stage stage = createNiceMock(Stage.class);
    expect(stage.getExecutionCommandWrapper(anyObject(String.class), anyObject(String.class))).
            andReturn(executionCommandWrapper).anyTimes();

    Map<Role, Float> successFactors = new HashMap<>();
    expect(stage.getSuccessFactors()).andReturn(successFactors).atLeastOnce();

    // Check that we create proper stage count
    expect(stageFactory.createNew(anyLong(), anyObject(String.class),
            anyObject(String.class), anyLong(),
            anyObject(String.class), anyObject(String.class), anyObject(String.class),
            anyObject(String.class))).andReturn(stage).
            times((int) Math.ceil(hostCount / MAX_TASKS_PER_STAGE));

    expect(
            repositoryVersionDAOMock.findByStackAndVersion(
                    anyObject(StackId.class),
                    anyObject(String.class))).andReturn(repoVersion);

    expect(clusterVersionDAO.findByCluster(anyObject(String.class))).andReturn(Collections.<ClusterVersionEntity>emptyList()).once();

    Capture<org.apache.ambari.server.actionmanager.Request> c = Capture.newInstance();
    Capture<ExecuteActionRequest> ear = Capture.newInstance();

    actionManager.sendActions(capture(c), capture(ear));
    expectLastCall().atLeastOnce();
    expect(actionManager.getRequestTasks(anyLong())).andReturn(Collections.<HostRoleCommand>emptyList()).anyTimes();

    ClusterEntity clusterEntity = new ClusterEntity();
    clusterEntity.setClusterId(1l);
    clusterEntity.setClusterName(clusterName);
    ClusterVersionEntity cve = new ClusterVersionEntity(clusterEntity,
            repoVersion, RepositoryVersionState.INSTALL_FAILED, 0, "");
    expect(clusterVersionDAO.findByClusterAndStackAndVersion(anyObject(String.class),
            anyObject(StackId.class), anyObject(String.class))).andReturn(cve);


    StageUtils.setTopologyManager(injector.getInstance(TopologyManager.class));
    StageUtils.setConfiguration(injector.getInstance(Configuration.class));

    // replay
    replay(managementController, response, clusters, hdfsService, hbaseService, resourceProviderFactory, csvResourceProvider,
            cluster, repositoryVersionDAOMock, configHelper, schDatanode, schNamenode, schHBM, actionManager,
            executionCommandWrapper,stage, stageFactory, clusterVersionDAO);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    injector.injectMembers(provider);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<Map<String, Object>>();

    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    // add properties to the request map
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_REPOSITORY_VERSION_PROPERTY_ID, "2.2.0.1-885");
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_STACK_PROPERTY_ID, "HDP");
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_VERSION_PROPERTY_ID, "2.1.1");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    RequestStatus status = provider.createResources(request);
    Assert.assertNotNull(status);

    // verify
    verify(managementController, response, clusters, stageFactory, stage);

    // check that the success factor was populated in the stage
    Float successFactor = successFactors.get(Role.INSTALL_PACKAGES);
    Assert.assertEquals(Float.valueOf(0.85f), successFactor);

    Assert.assertTrue(executionCommand.getRoleParams().containsKey(KeyNames.PACKAGE_VERSION));
  }

   @Test
   public void testCreateResourcesWithNonManagedOSAsAdministrator() throws Exception {
     testCreateResourcesWithNonManagedOS(TestAuthenticationFactory.createAdministrator());
   }

   @Test
   public void testCreateResourcesWithNonManagedOSAsClusterAdministrator() throws Exception {
     testCreateResourcesWithNonManagedOS(TestAuthenticationFactory.createClusterAdministrator());
   }

   @Test(expected = AuthorizationException.class)
   public void testCreateResourcesWithNonManagedOSAsClusterOperator() throws Exception {
     testCreateResourcesWithNonManagedOS(TestAuthenticationFactory.createClusterOperator());
   }

   private void testCreateResourcesWithNonManagedOS(Authentication authentication) throws Exception {
    JsonArray json = new JsonParser().parse(OS_JSON).getAsJsonArray();

    JsonObject jsonObj = json.get(0).getAsJsonObject();
    jsonObj.addProperty(OperatingSystemResourceProvider.OPERATING_SYSTEM_AMBARI_MANAGED_REPOS, false);

    String os_json = json.toString();

    Resource.Type type = Resource.Type.ClusterStackVersion;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    StackId stackId = new StackId("HDP", "2.0.1");

    File f = new File("src/test/resources/hbase_version_test.xml");

    RepositoryVersionEntity repoVersion = new RepositoryVersionEntity();
    repoVersion.setId(1l);
    repoVersion.setOperatingSystems(os_json);
    repoVersion.setVersionXml(IOUtils.toString(new FileInputStream(f)));
    repoVersion.setVersionXsd("version_definition.xsd");
    repoVersion.setType(RepositoryType.STANDARD);

    ambariMetaInfo.getComponent("HDP", "2.1.1", "HBASE", "HBASE_MASTER").setVersionAdvertised(true);

    Map<String, Host> hostsForCluster = new HashMap<String, Host>();
    int hostCount = 10;
    for (int i = 0; i < hostCount; i++) {
      String hostname = "host" + i;
      Host host = createNiceMock(hostname, Host.class);
      expect(host.getHostName()).andReturn(hostname).anyTimes();
      expect(host.getOsFamily()).andReturn("redhat6").anyTimes();
      expect(host.getMaintenanceState(EasyMock.anyLong())).andReturn(
          MaintenanceState.OFF).anyTimes();
      expect(host.getAllHostVersions()).andReturn(
          Collections.<HostVersionEntity>emptyList()).anyTimes();

      replay(host);
      hostsForCluster.put(hostname, host);
    }

    Service hdfsService = createNiceMock(Service.class);
    Service hbaseService = createNiceMock(Service.class);
    expect(hdfsService.getName()).andReturn("HDFS").anyTimes();
    expect(hbaseService.getName()).andReturn("HBASE").anyTimes();

    expect(hdfsService.getServiceComponents()).andReturn(new HashMap<String, ServiceComponent>());
    expect(hbaseService.getServiceComponents()).andReturn(new HashMap<String, ServiceComponent>());

    Map<String, Service> serviceMap = new HashMap<>();
    serviceMap.put("HDFS", hdfsService);
    serviceMap.put("HBASE", hbaseService);

    final ServiceComponentHost schDatanode = createMock(ServiceComponentHost.class);
    expect(schDatanode.getServiceName()).andReturn("HDFS").anyTimes();
    expect(schDatanode.getServiceComponentName()).andReturn("DATANODE").anyTimes();

    final ServiceComponentHost schNamenode = createMock(ServiceComponentHost.class);
    expect(schNamenode.getServiceName()).andReturn("HDFS").anyTimes();
    expect(schNamenode.getServiceComponentName()).andReturn("NAMENODE").anyTimes();

    final ServiceComponentHost schHBM = createMock(ServiceComponentHost.class);
    expect(schHBM.getServiceName()).andReturn("HBASE").anyTimes();
    expect(schHBM.getServiceComponentName()).andReturn("HBASE_MASTER").anyTimes();

    // First host contains versionable components
    final List<ServiceComponentHost> schsH1 = Arrays.asList(schDatanode, schNamenode);

    // Second host contains versionable components
    final List<ServiceComponentHost> schsH2 = Arrays.asList(schDatanode);

    // Third host only has hbase
    final List<ServiceComponentHost> schsH3 = Arrays.asList(schHBM);

    ServiceOsSpecific.Package hdfsPackage = new ServiceOsSpecific.Package();
    hdfsPackage.setName("hdfs");

    List<ServiceOsSpecific.Package> packages = Collections.singletonList(hdfsPackage);

    ActionManager actionManager = createNiceMock(ActionManager.class);

    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);
    ResourceProvider csvResourceProvider = createNiceMock(ClusterStackVersionResourceProvider.class);

    AbstractControllerResourceProvider.init(resourceProviderFactory);

    Map<String, Map<String, String>> hostConfigTags = new HashMap<String, Map<String, String>>();
    expect(configHelper.getEffectiveDesiredTags(anyObject(ClusterImpl.class), anyObject(String.class))).andReturn(hostConfigTags);

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getAuthName()).andReturn("admin").anyTimes();
    expect(managementController.getActionManager()).andReturn(actionManager).anyTimes();
    expect(managementController.getJdkResourceUrl()).andReturn("/JdkResourceUrl").anyTimes();
    expect(managementController.getPackagesForServiceHost(anyObject(ServiceInfo.class),
            EasyMock.<Map<String, String>>anyObject(), anyObject(String.class))).
            andReturn(packages).anyTimes(); // only one host has the versionable component

    expect(resourceProviderFactory.getHostResourceProvider(EasyMock.<Set<String>>anyObject(), EasyMock.<Map<Resource.Type, String>>anyObject(),
            eq(managementController))).andReturn(csvResourceProvider).anyTimes();

    expect(clusters.getCluster(anyObject(String.class))).andReturn(cluster);
    expect(clusters.getHostsForCluster(anyObject(String.class))).andReturn(
        hostsForCluster).anyTimes();

    String clusterName = "Cluster100";
    expect(cluster.getClusterId()).andReturn(1L).anyTimes();
    expect(cluster.getHosts()).andReturn(hostsForCluster.values()).atLeastOnce();
    expect(cluster.getServices()).andReturn(serviceMap).anyTimes();
    expect(cluster.getCurrentStackVersion()).andReturn(stackId);
    expect(cluster.getServiceComponentHosts(anyObject(String.class))).andAnswer(new IAnswer<List<ServiceComponentHost>>() {
      @Override
      public List<ServiceComponentHost> answer() throws Throwable {
        String hostname = (String) EasyMock.getCurrentArguments()[0];
        if (hostname.equals("host2")) {
          return schsH2;
        } else if (hostname.equals("host3")) {
          return schsH3;
        } else {
          return schsH1;
        }
      }
    }).anyTimes();

//    ExecutionCommand executionCommand = createNiceMock(ExecutionCommand.class);
    ExecutionCommand executionCommand = new ExecutionCommand();
    ExecutionCommandWrapper executionCommandWrapper = createNiceMock(ExecutionCommandWrapper.class);

//    expect(executionCommand.getHostLevelParams()).andReturn(new HashMap<String, String>()).atLeastOnce();
    expect(executionCommandWrapper.getExecutionCommand()).andReturn(executionCommand).anyTimes();

    Stage stage = createNiceMock(Stage.class);
    expect(stage.getExecutionCommandWrapper(anyObject(String.class), anyObject(String.class))).
            andReturn(executionCommandWrapper).anyTimes();

    Map<Role, Float> successFactors = new HashMap<>();
    expect(stage.getSuccessFactors()).andReturn(successFactors).atLeastOnce();

    // Check that we create proper stage count
    expect(stageFactory.createNew(anyLong(), anyObject(String.class),
            anyObject(String.class), anyLong(),
            anyObject(String.class), anyObject(String.class), anyObject(String.class),
            anyObject(String.class))).andReturn(stage).
            times((int) Math.ceil(hostCount / MAX_TASKS_PER_STAGE));

    expect(
            repositoryVersionDAOMock.findByStackAndVersion(
                    anyObject(StackId.class),
                    anyObject(String.class))).andReturn(repoVersion);

    Capture<org.apache.ambari.server.actionmanager.Request> c = Capture.newInstance();
    Capture<ExecuteActionRequest> ear = Capture.newInstance();

    actionManager.sendActions(capture(c), capture(ear));
    expectLastCall().atLeastOnce();
    expect(actionManager.getRequestTasks(anyLong())).andReturn(Collections.<HostRoleCommand>emptyList()).anyTimes();

    ClusterEntity clusterEntity = new ClusterEntity();
    clusterEntity.setClusterId(1l);
    clusterEntity.setClusterName(clusterName);
    ClusterVersionEntity cve = new ClusterVersionEntity(clusterEntity,
            repoVersion, RepositoryVersionState.INSTALL_FAILED, 0, "");
    expect(clusterVersionDAO.findByClusterAndStackAndVersion(anyObject(String.class),
            anyObject(StackId.class), anyObject(String.class))).andReturn(cve);

    StageUtils.setTopologyManager(injector.getInstance(TopologyManager.class));
    StageUtils.setConfiguration(injector.getInstance(Configuration.class));

    expect(clusterVersionDAO.findByCluster(anyObject(String.class))).andReturn(Collections.<ClusterVersionEntity>emptyList()).once();

    // replay
    replay(managementController, response, clusters, hdfsService, hbaseService, resourceProviderFactory, csvResourceProvider,
            cluster, repositoryVersionDAOMock, configHelper, schDatanode, schNamenode, schHBM, actionManager,
            executionCommandWrapper,stage, stageFactory, clusterVersionDAO);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    injector.injectMembers(provider);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<Map<String, Object>>();

    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    // add properties to the request map
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_REPOSITORY_VERSION_PROPERTY_ID, "2.2.0.1-885");
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_STACK_PROPERTY_ID, "HDP");
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_VERSION_PROPERTY_ID, "2.1.1");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    RequestStatus status = provider.createResources(request);
    Assert.assertNotNull(status);

    // verify
    verify(managementController, response, clusters, stageFactory, stage);

    // check that the success factor was populated in the stage
    Float successFactor = successFactors.get(Role.INSTALL_PACKAGES);
    Assert.assertEquals(Float.valueOf(0.85f), successFactor);

    Assert.assertTrue(executionCommand.getRoleParams().containsKey(KeyNames.PACKAGE_VERSION));
    Assert.assertTrue(executionCommand.getRoleParams().containsKey("base_urls"));
    Assert.assertEquals("[]", executionCommand.getRoleParams().get("base_urls"));
  }

   @Test
   public void testUpdateResourcesAsAdministrator() throws Exception {
     testUpdateResources(TestAuthenticationFactory.createAdministrator());
   }

   @Test
   public void testUpdateResourcesAsClusterAdministrator() throws Exception {
     testUpdateResources(TestAuthenticationFactory.createClusterAdministrator());
   }

   @Test(expected = AuthorizationException.class)
   public void testUpdateResourcesAsClusterOperator() throws Exception {
     testUpdateResources(TestAuthenticationFactory.createClusterOperator());
   }

   private void testUpdateResources(Authentication authentication) throws Exception {
    Resource.Type type = Resource.Type.ClusterStackVersion;
    String clusterName = "Cluster100";

    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    StackId stackId = new StackId("HDP", "2.0.1");
    StackEntity stackEntity = stackDAO.find(stackId.getStackName(), stackId.getStackVersion());
    Assert.assertNotNull(stackEntity);

    ResourceTypeEntity resourceTypeEntity = resourceTypeDAO.findById(ResourceType.CLUSTER.getId());
    if (resourceTypeEntity == null) {
      resourceTypeEntity = new ResourceTypeEntity();
      resourceTypeEntity.setId(ResourceType.CLUSTER.getId());
      resourceTypeEntity.setName(ResourceType.CLUSTER.name());
      resourceTypeEntity = resourceTypeDAO.merge(resourceTypeEntity);
    }
    ResourceEntity resourceEntity = new ResourceEntity();
    resourceEntity.setResourceType(resourceTypeEntity);

    final Host host1 = createNiceMock("host1", Host.class);
    final Host host2 = createNiceMock("host2", Host.class);

    expect(host1.getHostName()).andReturn("host1").anyTimes();
    expect(host2.getHostName()).andReturn("host2").anyTimes();
    replay(host1, host2);

    ServiceComponentHost sch = createMock(ServiceComponentHost.class);
    List<ServiceComponentHost> schs = Collections.singletonList(sch);

    Cluster cluster = createNiceMock(Cluster.class);
    cluster.setClusterName(clusterName);

    ArrayList<Host> hosts = new ArrayList<Host>() {{
      add(host1);
      add(host2);
    }};

    Clusters clusters = createNiceMock(Clusters.class);
    expect(clusters.getCluster(anyObject(String.class))).andReturn(cluster);

    RepositoryVersionEntity repoVersion = new RepositoryVersionEntity();
    repoVersion.setOperatingSystems(OS_JSON);
    StackEntity newDesiredStack = stackDAO.find("HDP", "2.0.1");
    repoVersion.setStack(newDesiredStack);

    final ServiceOsSpecific.Package hivePackage = new ServiceOsSpecific.Package();
    hivePackage.setName("hive");
    final ServiceOsSpecific.Package mysqlPackage = new ServiceOsSpecific.Package();
    mysqlPackage.setName("mysql");
    mysqlPackage.setSkipUpgrade(Boolean.TRUE);
    List<ServiceOsSpecific.Package> packages = new ArrayList<ServiceOsSpecific.Package>() {{
      add(hivePackage);
      add(mysqlPackage);
    }};

    ActionManager actionManager = createNiceMock(ActionManager.class);

    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);
    ResourceProvider csvResourceProvider = createNiceMock(ClusterStackVersionResourceProvider.class);

    CommandReport report = createNiceMock(CommandReport.class);
    FinalizeUpgradeAction finalizeUpgradeAction = createNiceMock(FinalizeUpgradeAction.class);

    AbstractControllerResourceProvider.init(resourceProviderFactory);

    Map<String, Map<String, String>> hostConfigTags = new HashMap<String, Map<String, String>>();
    expect(configHelper.getEffectiveDesiredTags(anyObject(ClusterImpl.class), anyObject(String.class))).andReturn(hostConfigTags);

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getAuthName()).andReturn("admin").anyTimes();
    expect(managementController.getActionManager()).andReturn(actionManager).anyTimes();
    expect(managementController.getJdkResourceUrl()).andReturn("/JdkResourceUrl").anyTimes();
    expect(managementController.getPackagesForServiceHost(anyObject(ServiceInfo.class),
            EasyMock.<Map<String, String>>anyObject(), anyObject(String.class))).andReturn(packages).anyTimes();

    expect(resourceProviderFactory.getHostResourceProvider(EasyMock.<Set<String>>anyObject(), EasyMock.<Map<Resource.Type, String>>anyObject(),
            eq(managementController))).andReturn(csvResourceProvider).anyTimes();

    expect(cluster.getCurrentStackVersion()).andReturn(stackId);
    expect(cluster.getServiceComponentHosts(anyObject(String.class))).andReturn(schs).anyTimes();

    Capture<StackId> capturedStackId = EasyMock.newCapture();
    cluster.setDesiredStackVersion(capture(capturedStackId));
      expectLastCall().once();
    expect(cluster.getHosts()).andReturn(hosts).anyTimes();


    expect(sch.getServiceName()).andReturn("HIVE").anyTimes();

    expect(repositoryVersionDAOMock.findByDisplayName(anyObject(String.class))).andReturn(repoVersion);

    expect(actionManager.getRequestTasks(anyLong())).andReturn(Collections.<HostRoleCommand>emptyList()).anyTimes();

    expect(finalizeUpgradeAction.execute(null)).andReturn(report);

    expect(report.getStdOut()).andReturn("Dummy stdout");
    expect(report.getStdErr()).andReturn("Dummy stderr");
    expect(report.getStatus()).andReturn("COMPLETED");

    // replay
    replay(managementController, response, clusters, resourceProviderFactory, csvResourceProvider,
            cluster, repositoryVersionDAOMock, configHelper, sch, actionManager, finalizeUpgradeAction, report,
            stageFactory);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
            type,
            PropertyHelper.getPropertyIds(type),
            PropertyHelper.getKeyPropertyIds(type),
            managementController);

    injector.injectMembers(provider);

    // Have to inject instance manually because injection via DI fails
    Field field = ClusterStackVersionResourceProvider.class.getDeclaredField("finalizeUpgradeAction");
    field.setAccessible(true);
    field.set(provider, finalizeUpgradeAction);

    // add the property map to a set for the request.  add more maps for multiple creates
    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    // add properties to the request map
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID, clusterName);
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_STATE_PROPERTY_ID, "CURRENT");
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_REPOSITORY_VERSION_PROPERTY_ID, "HDP-2.2.2.0-2561");

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, null);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    provider.updateResources(request, null);

    // verify
    verify(managementController, response);
    Assert.assertEquals(capturedStackId.getValue(),
            new StackId(newDesiredStack.getStackName(), newDesiredStack.getStackVersion()));
  }

   @Test
   public void testUpdateResourcesWithForceAsAdministrator() throws Exception {
     testUpdateResourcesWithForce(TestAuthenticationFactory.createAdministrator());
   }

   @Test
   public void testUpdateResourcesWithForceAsClusterAdministrator() throws Exception {
     testUpdateResourcesWithForce(TestAuthenticationFactory.createClusterAdministrator());
   }

   @Test(expected = AuthorizationException.class)
   public void testUpdateResourcesWithForceAsClusterOperator() throws Exception {
     testUpdateResourcesWithForce(TestAuthenticationFactory.createClusterOperator());
   }

   private void testUpdateResourcesWithForce(Authentication authentication) throws Exception {
    Resource.Type type = Resource.Type.ClusterStackVersion;
    String clusterName = "Cluster100";

    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    StackId stackId = new StackId("HDP", "2.0.1");
    StackEntity stackEntity = stackDAO.find(stackId.getStackName(), stackId.getStackVersion());
    Assert.assertNotNull(stackEntity);

    ResourceTypeEntity resourceTypeEntity = resourceTypeDAO.findById(ResourceType.CLUSTER.getId());
    if (resourceTypeEntity == null) {
      resourceTypeEntity = new ResourceTypeEntity();
      resourceTypeEntity.setId(ResourceType.CLUSTER.getId());
      resourceTypeEntity.setName(ResourceType.CLUSTER.name());
      resourceTypeEntity = resourceTypeDAO.merge(resourceTypeEntity);
    }
    ResourceEntity resourceEntity = new ResourceEntity();
    resourceEntity.setResourceType(resourceTypeEntity);

    final Host host1 = createNiceMock("host1", Host.class);
    final Host host2 = createNiceMock("host2", Host.class);

    expect(host1.getHostName()).andReturn("host1").anyTimes();
    expect(host2.getHostName()).andReturn("host2").anyTimes();
    replay(host1, host2);

    ServiceComponentHost sch = createMock(ServiceComponentHost.class);
    List<ServiceComponentHost> schs = Collections.singletonList(sch);

    Cluster cluster = createNiceMock(Cluster.class);
    cluster.setClusterName(clusterName);

    ArrayList<Host> hosts = new ArrayList<Host>() {{
      add(host1);
      add(host2);
    }};

    Clusters clusters = createNiceMock(Clusters.class);
    expect(clusters.getCluster(anyObject(String.class))).andReturn(cluster);

    RepositoryVersionEntity repoVersion = new RepositoryVersionEntity();
    repoVersion.setOperatingSystems(OS_JSON);
    StackEntity newDesiredStack = stackDAO.find("HDP", "2.0.1");
    repoVersion.setStack(newDesiredStack);

    final ServiceOsSpecific.Package hivePackage = new ServiceOsSpecific.Package();
    hivePackage.setName("hive");
    final ServiceOsSpecific.Package mysqlPackage = new ServiceOsSpecific.Package();
    mysqlPackage.setName("mysql");
    mysqlPackage.setSkipUpgrade(Boolean.TRUE);
    List<ServiceOsSpecific.Package> packages = new ArrayList<ServiceOsSpecific.Package>() {{
      add(hivePackage);
      add(mysqlPackage);
    }};

    ActionManager actionManager = createNiceMock(ActionManager.class);

    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);
    ResourceProvider csvResourceProvider = createNiceMock(ClusterStackVersionResourceProvider.class);

    AbstractControllerResourceProvider.init(resourceProviderFactory);

    Map<String, Map<String, String>> hostConfigTags = new HashMap<String, Map<String, String>>();
    expect(configHelper.getEffectiveDesiredTags(anyObject(ClusterImpl.class), anyObject(String.class))).andReturn(hostConfigTags);

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getAuthName()).andReturn("admin").anyTimes();
    expect(managementController.getActionManager()).andReturn(actionManager).anyTimes();
    expect(managementController.getJdkResourceUrl()).andReturn("/JdkResourceUrl").anyTimes();
    expect(managementController.getPackagesForServiceHost(anyObject(ServiceInfo.class),
            EasyMock.<Map<String, String>>anyObject(), anyObject(String.class))).andReturn(packages).anyTimes();

    expect(resourceProviderFactory.getHostResourceProvider(EasyMock.<Set<String>>anyObject(), EasyMock.<Map<Resource.Type, String>>anyObject(),
            eq(managementController))).andReturn(csvResourceProvider).anyTimes();

    expect(cluster.getCurrentStackVersion()).andReturn(stackId);
    expect(cluster.getServiceComponentHosts(anyObject(String.class))).andReturn(schs).anyTimes();

    RepositoryVersionEntity currentRepo = new RepositoryVersionEntity();
    currentRepo.setVersion("2.2.2.0-2122");
    ClusterVersionEntity current = new ClusterVersionEntity();
    current.setRepositoryVersion(currentRepo);

    Capture<StackId> capturedStackId = EasyMock.newCapture();
    cluster.setDesiredStackVersion(capture(capturedStackId));
      expectLastCall().once();
    expect(cluster.getHosts()).andReturn(hosts).anyTimes();
    expect(cluster.getCurrentClusterVersion()).andReturn(current).anyTimes();

    expect(sch.getServiceName()).andReturn("HIVE").anyTimes();

    expect(repositoryVersionDAOMock.findByDisplayName(anyObject(String.class))).andReturn(repoVersion);

    clusterVersionDAO.updateVersions((Long) anyObject(),
        (RepositoryVersionEntity) anyObject(), (RepositoryVersionEntity) anyObject());
    expectLastCall().once();

    hostVersionDAO.updateVersions((RepositoryVersionEntity) anyObject(), (RepositoryVersionEntity) anyObject());
    expectLastCall().once();

    hostComponentStateDAO.updateVersions((String) anyObject());
    expectLastCall().once();

    // replay
    replay(managementController, response, clusters, resourceProviderFactory, csvResourceProvider,
            cluster, repositoryVersionDAOMock, configHelper, sch, actionManager, clusterVersionDAO,
            hostVersionDAO, hostComponentStateDAO);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
            type,
            PropertyHelper.getPropertyIds(type),
            PropertyHelper.getKeyPropertyIds(type),
            managementController);

    injector.injectMembers(provider);


    // add the property map to a set for the request.  add more maps for multiple creates
    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    // add properties to the request map
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID, clusterName);
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_STATE_PROPERTY_ID, "CURRENT");
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_REPOSITORY_VERSION_PROPERTY_ID, "HDP-2.2.2.0-2561");
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_FORCE, "true");

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, null);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    provider.updateResources(request, null);

    // verify
    verify(managementController, response, clusterVersionDAO, hostVersionDAO, hostComponentStateDAO);
    Assert.assertEquals(capturedStackId.getValue(),
            new StackId(newDesiredStack.getStackName(), newDesiredStack.getStackVersion()));
  }

   @Test
   public void testCreateResourcesMixedAsAdministrator() throws Exception {
     testCreateResourcesMixed(TestAuthenticationFactory.createAdministrator());
   }

   @Test
   public void testCreateResourcesMixedAsClusterAdministrator() throws Exception {
     testCreateResourcesMixed(TestAuthenticationFactory.createClusterAdministrator());
   }

   @Test(expected = AuthorizationException.class)
   public void testCreateResourcesMixedAsClusterOperator() throws Exception {
     testCreateResourcesMixed(TestAuthenticationFactory.createClusterOperator());
   }

   private void testCreateResourcesMixed(Authentication authentication) throws Exception {
    Resource.Type type = Resource.Type.ClusterStackVersion;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Map<String, String> hostLevelParams = new HashMap<>();
    StackId stackId = new StackId("HDP", "2.0.1");

    File f = new File("src/test/resources/hbase_version_test.xml");
    String xml = IOUtils.toString(new FileInputStream(f));
    // munge it
    xml = xml.replace("<package-version>2_3_4_0_3396</package-version>", "");

    StackEntity stack = new StackEntity();
    stack.setStackName("HDP");

    RepositoryVersionEntity repoVersion = new RepositoryVersionEntity();
    repoVersion.setStack(stack);
    repoVersion.setId(1l);
    repoVersion.setOperatingSystems(OS_JSON);
    repoVersion.setVersionXml(xml);
    repoVersion.setVersionXsd("version_definition.xsd");
    repoVersion.setType(RepositoryType.STANDARD);


    Map<String, Host> hostsForCluster = new HashMap<String, Host>();
    int hostCount = 10;
    for (int i = 0; i < hostCount; i++) {
      String hostname = "host" + i;
      Host host = createNiceMock(hostname, Host.class);
      expect(host.getHostName()).andReturn(hostname).anyTimes();
      expect(host.getOsFamily()).andReturn("redhat6").anyTimes();
      expect(host.getMaintenanceState(EasyMock.anyLong())).andReturn(
          MaintenanceState.OFF).anyTimes();
      expect(host.getAllHostVersions()).andReturn(
          Collections.<HostVersionEntity>emptyList()).anyTimes();

      replay(host);
      hostsForCluster.put(hostname, host);
    }

    final ServiceComponentHost schDatanode = createMock(ServiceComponentHost.class);
    expect(schDatanode.getServiceName()).andReturn("HDFS").anyTimes();
    expect(schDatanode.getServiceComponentName()).andReturn("DATANODE").anyTimes();
    final ServiceComponentHost schNamenode = createMock(ServiceComponentHost.class);
    expect(schNamenode.getServiceName()).andReturn("HDFS").anyTimes();
    expect(schNamenode.getServiceComponentName()).andReturn("NAMENODE").anyTimes();
    final ServiceComponentHost schAMS = createMock(ServiceComponentHost.class);
    expect(schAMS.getServiceName()).andReturn("AMBARI_METRICS").anyTimes();
    expect(schAMS.getServiceComponentName()).andReturn("METRICS_COLLECTOR").anyTimes();
    // First host contains versionable components
    final List<ServiceComponentHost> schsH1 = new ArrayList<ServiceComponentHost>(){{
      add(schDatanode);
      add(schNamenode);
      add(schAMS);
    }};
    // Second host does not contain versionable components
    final List<ServiceComponentHost> schsH2 = new ArrayList<ServiceComponentHost>(){{
      add(schAMS);
    }};


    ServiceOsSpecific.Package hdfsPackage = new ServiceOsSpecific.Package();
    hdfsPackage.setName("hdfs");
    List<ServiceOsSpecific.Package> packages = Collections.singletonList(hdfsPackage);

    ActionManager actionManager = createNiceMock(ActionManager.class);

    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);
    ResourceProvider csvResourceProvider = createNiceMock(ClusterStackVersionResourceProvider.class);

    AbstractControllerResourceProvider.init(resourceProviderFactory);

    Map<String, Map<String, String>> hostConfigTags = new HashMap<String, Map<String, String>>();
    expect(configHelper.getEffectiveDesiredTags(anyObject(ClusterImpl.class), anyObject(String.class))).andReturn(hostConfigTags);

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getAuthName()).andReturn("admin").anyTimes();
    expect(managementController.getActionManager()).andReturn(actionManager).anyTimes();
    expect(managementController.getJdkResourceUrl()).andReturn("/JdkResourceUrl").anyTimes();
    expect(managementController.getPackagesForServiceHost(anyObject(ServiceInfo.class),
            EasyMock.<Map<String, String>>anyObject(), anyObject(String.class))).
            andReturn(packages).times((hostCount - 1) * 2); // 1 host has no versionable components, other hosts have 2 services
//            // that's why we don't send commands to it

    expect(resourceProviderFactory.getHostResourceProvider(EasyMock.<Set<String>>anyObject(), EasyMock.<Map<Resource.Type, String>>anyObject(),
            eq(managementController))).andReturn(csvResourceProvider).anyTimes();

    expect(clusters.getCluster(anyObject(String.class))).andReturn(cluster);
    expect(clusters.getHostsForCluster(anyObject(String.class))).andReturn(
        hostsForCluster).anyTimes();

    String clusterName = "Cluster100";
    expect(cluster.getClusterId()).andReturn(1L).anyTimes();
    expect(cluster.getHosts()).andReturn(hostsForCluster.values()).atLeastOnce();
    expect(cluster.getServices()).andReturn(new HashMap<String, Service>()).anyTimes();
    expect(cluster.getCurrentStackVersion()).andReturn(stackId);
    expect(cluster.getServiceComponentHosts(anyObject(String.class))).andAnswer(new IAnswer<List<ServiceComponentHost>>() {
      @Override
      public List<ServiceComponentHost> answer() throws Throwable {
        String hostname = (String) EasyMock.getCurrentArguments()[0];
        if (hostname.equals("host2")) {
          return schsH2;
        } else {
          return schsH1;
        }
      }
    }).anyTimes();

    ExecutionCommand executionCommand = createNiceMock(ExecutionCommand.class);
    ExecutionCommandWrapper executionCommandWrapper = createNiceMock(ExecutionCommandWrapper.class);

    expect(executionCommandWrapper.getExecutionCommand()).andReturn(executionCommand).anyTimes();

    Stage stage = createNiceMock(Stage.class);
    expect(stage.getExecutionCommandWrapper(anyObject(String.class), anyObject(String.class))).
            andReturn(executionCommandWrapper).anyTimes();

    expect(executionCommand.getHostLevelParams()).andReturn(hostLevelParams).anyTimes();

    Map<Role, Float> successFactors = new HashMap<>();
    expect(stage.getSuccessFactors()).andReturn(successFactors).atLeastOnce();

    // Check that we create proper stage count
    expect(stageFactory.createNew(anyLong(), anyObject(String.class),
        anyObject(String.class), anyLong(),
        anyObject(String.class), anyObject(String.class), anyObject(String.class),
        anyObject(String.class))).andReturn(stage).
        times((int) Math.ceil(hostCount / MAX_TASKS_PER_STAGE));

    expect(repositoryVersionDAOMock.findByStackAndVersion(anyObject(StackId.class),
        anyObject(String.class))).andReturn(repoVersion);

    expect(actionManager.getRequestTasks(anyLong())).andReturn(Collections.<HostRoleCommand>emptyList()).anyTimes();

    ClusterEntity clusterEntity = new ClusterEntity();
    clusterEntity.setClusterId(1l);
    clusterEntity.setClusterName(clusterName);
    ClusterVersionEntity cve = new ClusterVersionEntity(clusterEntity,
            repoVersion, RepositoryVersionState.INSTALL_FAILED, 0, "");
    expect(clusterVersionDAO.findByClusterAndStackAndVersion(anyObject(String.class),
            anyObject(StackId.class), anyObject(String.class))).andReturn(cve);

    TopologyManager topologyManager = injector.getInstance(TopologyManager.class);
    StageUtils.setTopologyManager(topologyManager);
    StageUtils.setConfiguration(injector.getInstance(Configuration.class));


    // !!! make it look like there is already a versioned installed that is less than the one being installed
    ClusterVersionEntity bad = new ClusterVersionEntity();
    RepositoryVersionEntity badRve = new RepositoryVersionEntity();
    badRve.setStack(stack);
    badRve.setVersion("2.2.1.0-1000");
    bad.setRepositoryVersion(badRve);

    expect(clusterVersionDAO.findByCluster(anyObject(String.class))).andReturn(Collections.<ClusterVersionEntity>singletonList(bad)).once();

    // replay
    replay(managementController, response, clusters, resourceProviderFactory, csvResourceProvider,
            cluster, repositoryVersionDAOMock, configHelper, schDatanode, schNamenode, schAMS, actionManager,
            executionCommand, executionCommandWrapper,stage, stageFactory, clusterVersionDAO);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    injector.injectMembers(provider);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<Map<String, Object>>();

    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    // add properties to the request map
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_REPOSITORY_VERSION_PROPERTY_ID, "2.2.0.1-885");
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_STACK_PROPERTY_ID, "HDP");
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_VERSION_PROPERTY_ID, "2.1.1");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    try {
      provider.createResources(request);
      Assert.fail("Expecting the create to fail due to an already installed version");
    } catch (IllegalArgumentException iae) {
      // !!! expected
    }

  }

   @Test
   public void testCreateResourcesExistingUpgradeAsAdministrator() throws Exception {
     testCreateResourcesExistingUpgrade(TestAuthenticationFactory.createAdministrator());
   }

   @Test
   public void testCreateResourcesExistingUpgradeAsClusterAdministrator() throws Exception {
     testCreateResourcesExistingUpgrade(TestAuthenticationFactory.createClusterAdministrator());
   }

   @Test(expected = AuthorizationException.class)
   public void testCreateResourcesExistingUpgradeAsClusterOperator() throws Exception {
     testCreateResourcesExistingUpgrade(TestAuthenticationFactory.createClusterOperator());
   }

  /**
   * Tests that forcing the host versions into
   * {@link RepositoryVersionState#INSTALLED}
   *
   * @throws Exception
   */
  @Test
  public void testCreateResourcesInInstalledState() throws Exception {
    Resource.Type type = Resource.Type.ClusterStackVersion;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    StackId stackId = new StackId("HDP", "2.2.0");
    String repoVersion = "2.2.0.1-885";

    File f = new File("src/test/resources/hbase_version_test.xml");

    RepositoryVersionEntity repoVersionEntity = new RepositoryVersionEntity();
    repoVersionEntity.setId(1l);
    repoVersionEntity.setOperatingSystems(OS_JSON);
    repoVersionEntity.setVersionXml(IOUtils.toString(new FileInputStream(f)));
    repoVersionEntity.setVersionXsd("version_definition.xsd");
    repoVersionEntity.setType(RepositoryType.STANDARD);

    Map<String, Host> hostsForCluster = new HashMap<String, Host>();
    List<HostVersionEntity> hostVersionEntitiesMergedWithNotRequired = new ArrayList<>();
    int hostCount = 10;

    for (int i = 0; i < hostCount; i++) {
      String hostname = "host" + i;
      Host host = createNiceMock(hostname, Host.class);
      expect(host.getHostName()).andReturn(hostname).anyTimes();
      expect(host.getOsFamily()).andReturn("redhat6").anyTimes();
      expect(host.getMaintenanceState(EasyMock.anyLong())).andReturn(MaintenanceState.OFF).anyTimes();

      // ensure that 2 hosts don't have versionable components so they
      // transition correct into the not required state
      if (i < hostCount - 2) {
        expect(host.hasComponentsAdvertisingVersions(eq(stackId))).andReturn(true).atLeastOnce();
      } else {
        expect(host.hasComponentsAdvertisingVersions(eq(stackId))).andReturn(false).atLeastOnce();

        // mock out the host versions so that we can test hosts being
        // transitioned into NOT_REQUIRED
        HostVersionEntity hostVersionEntity = EasyMock.createNiceMock(HostVersionEntity.class);
        expect(hostVersionEntity.getRepositoryVersion()).andReturn(repoVersionEntity).atLeastOnce();
        replay(hostVersionEntity);

        hostVersionEntitiesMergedWithNotRequired.add(hostVersionEntity);
        expect(host.getAllHostVersions()).andReturn(hostVersionEntitiesMergedWithNotRequired).anyTimes();
      }

      replay(host);

      hostsForCluster.put(hostname, host);
    }

    Service hdfsService = createNiceMock(Service.class);
    expect(hdfsService.getName()).andReturn("HDFS").anyTimes();
    expect(hdfsService.getServiceComponents()).andReturn(new HashMap<String, ServiceComponent>());

    Map<String, Service> serviceMap = new HashMap<>();
    serviceMap.put("HDFS", hdfsService);

    final ServiceComponentHost schDatanode = createMock(ServiceComponentHost.class);
    expect(schDatanode.getServiceName()).andReturn("HDFS").anyTimes();
    expect(schDatanode.getServiceComponentName()).andReturn("DATANODE").anyTimes();

    final List<ServiceComponentHost> serviceComponentHosts = Arrays.asList(schDatanode);

    ServiceOsSpecific.Package hdfsPackage = new ServiceOsSpecific.Package();
    hdfsPackage.setName("hdfs");

    List<ServiceOsSpecific.Package> packages = Collections.singletonList(hdfsPackage);

    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);
    ResourceProvider csvResourceProvider = createNiceMock(
        ClusterStackVersionResourceProvider.class);

    AbstractControllerResourceProvider.init(resourceProviderFactory);

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getAuthName()).andReturn("admin").anyTimes();
    expect(managementController.getJdkResourceUrl()).andReturn("/JdkResourceUrl").anyTimes();
    expect(managementController.getPackagesForServiceHost(anyObject(ServiceInfo.class),
        EasyMock.<Map<String, String>>anyObject(), anyObject(String.class))).andReturn(
            packages).anyTimes(); // only one host has the versionable component

    expect(resourceProviderFactory.getHostResourceProvider(EasyMock.<Set<String>>anyObject(),
        EasyMock.<Map<Resource.Type, String>>anyObject(), eq(managementController))).andReturn(csvResourceProvider).anyTimes();

    expect(clusters.getCluster(anyObject(String.class))).andReturn(cluster);
    expect(clusters.getHostsForCluster(anyObject(String.class))).andReturn(
        hostsForCluster).anyTimes();

    String clusterName = "Cluster100";
    expect(cluster.getClusterId()).andReturn(1L).anyTimes();
    expect(cluster.getClusterName()).andReturn(clusterName).atLeastOnce();
    expect(cluster.getHosts()).andReturn(hostsForCluster.values()).atLeastOnce();
    expect(cluster.getServices()).andReturn(serviceMap).anyTimes();
    expect(cluster.getServiceComponentHosts(anyObject(String.class))).andReturn(
        serviceComponentHosts).anyTimes();

    expect(repositoryVersionDAOMock.findByStackAndVersion(anyObject(StackId.class),
        anyObject(String.class))).andReturn(repoVersionEntity);

    expect(clusterVersionDAO.findByCluster(anyObject(String.class))).andReturn(
        Collections.<ClusterVersionEntity> emptyList()).once();

    ClusterEntity clusterEntity = new ClusterEntity();
    clusterEntity.setClusterId(1l);
    clusterEntity.setClusterName(clusterName);

    ClusterVersionEntity cve = new ClusterVersionEntity(clusterEntity, repoVersionEntity,
        RepositoryVersionState.INSTALL_FAILED, 0, "");

    // first expect back a null to make the code think it needs to create one,
    // then return the real one it's going to use
    expect(clusterVersionDAO.findByClusterAndStackAndVersion(anyObject(String.class),
        anyObject(StackId.class), anyObject(String.class))).andReturn(null).once();
    expect(clusterVersionDAO.findByClusterAndStackAndVersion(anyObject(String.class),
        anyObject(StackId.class), anyObject(String.class))).andReturn(cve).once();

    // now the important expectations - that the cluster transition methods were
    // called correctly
    cluster.transitionHosts(cve, RepositoryVersionState.INSTALLED);
    for (HostVersionEntity hostVersionEntity : hostVersionEntitiesMergedWithNotRequired) {
      expect(hostVersionDAO.merge(hostVersionEntity)).andReturn(hostVersionEntity).once();
    }

    // replay
    replay(managementController, response, clusters, hdfsService, resourceProviderFactory,
        csvResourceProvider, cluster, repositoryVersionDAOMock, configHelper, schDatanode,
        stageFactory, clusterVersionDAO, hostVersionDAO);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(type,
        PropertyHelper.getPropertyIds(type), PropertyHelper.getKeyPropertyIds(type),
        managementController);

    injector.injectMembers(provider);

    // add the property map to a set for the request. add more maps for multiple
    // creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<Map<String, Object>>();

    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    // add properties to the request map
    properties.put(
        ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID,
        clusterName);

    properties.put(
        ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_REPOSITORY_VERSION_PROPERTY_ID,
        repoVersion);

    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_STACK_PROPERTY_ID,
        stackId.getStackName());

    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_VERSION_PROPERTY_ID,
        stackId.getStackVersion());

    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_FORCE, "true");

    propertySet.add(properties);

    // set the security auth
    SecurityContextHolder.getContext().setAuthentication(
        TestAuthenticationFactory.createAdministrator());

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    RequestStatus status = provider.createResources(request);
    Assert.assertNotNull(status);

    // verify
    verify(managementController, response, clusters, cluster, hostVersionDAO);
   }


   private void testCreateResourcesExistingUpgrade(Authentication authentication) throws Exception {
    Resource.Type type = Resource.Type.ClusterStackVersion;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();

    expect(clusters.getCluster(anyObject(String.class))).andReturn(cluster);

    UpgradeEntity upgrade = new UpgradeEntity();
    upgrade.setDirection(Direction.UPGRADE);

    expect(cluster.getClusterName()).andReturn("c1").atLeastOnce();
    expect(cluster.getClusterId()).andReturn(1L).anyTimes();
    expect(cluster.getUpgradeInProgress()).andReturn(upgrade).once();

    // replay
    replay(managementController, clusters, cluster);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    injector.injectMembers(provider);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<Map<String, Object>>();

    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    // add properties to the request map
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_REPOSITORY_VERSION_PROPERTY_ID, "2.2.0.1-885");
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_STACK_PROPERTY_ID, "HDP");
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_VERSION_PROPERTY_ID, "2.1.1");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    try {
      provider.createResources(request);
      Assert.fail("Expecting the create to fail due to an already installed version");
    } catch (IllegalArgumentException iae) {
      // !!! expected
      Assert.assertEquals("Cluster c1 upgrade is in progress.  Cannot install packages.", iae.getMessage());
    }

    verify(cluster);

  }
  private class MockModule extends AbstractModule {
    @Override
    protected void configure() {
      bind(RepositoryVersionDAO.class).toInstance(repositoryVersionDAOMock);
      bind(ConfigHelper.class).toInstance(configHelper);
      bind(Configuration.class).toInstance(configuration);
      bind(StageFactory.class).toInstance(stageFactory);
      bind(ClusterVersionDAO.class).toInstance(clusterVersionDAO);
      bind(HostVersionDAO.class).toInstance(hostVersionDAO);
      bind(HostComponentStateDAO.class).toInstance(hostComponentStateDAO);
    }
  }


}
