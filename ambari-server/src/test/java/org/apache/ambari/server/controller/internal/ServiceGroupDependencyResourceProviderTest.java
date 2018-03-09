/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.controller.internal;


import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import javax.persistence.EntityManager;

import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.actionmanager.ActionDBAccessor;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.audit.AuditLogger;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ClusterRequest;
import org.apache.ambari.server.controller.MaintenanceStateHelper;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.ServiceComponentHostRequest;
import org.apache.ambari.server.controller.ServiceComponentRequest;
import org.apache.ambari.server.controller.ServiceGroupDependencyRequest;
import org.apache.ambari.server.controller.ServiceGroupDependencyResponse;
import org.apache.ambari.server.controller.ServiceRequest;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.ClusterServiceDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.ServiceGroupDAO;
import org.apache.ambari.server.orm.dao.TopologyHostInfoDAO;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.stack.StackManagerMock;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.ServiceComponentFactory;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.ServiceGroup;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.configgroup.ConfigGroupFactory;
import org.apache.ambari.server.topology.TopologyManager;
import org.apache.ambari.server.utils.EventBusSynchronizer;
import org.apache.ambari.server.utils.StageUtils;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.inject.Guice;
import com.google.inject.Injector;


public class ServiceGroupDependencyResourceProviderTest {

  private static final String SERVICE_GROUP_NAME_CORE = "CORE";
  private static final String SERVICE_GROUP_NAME_TEST = "TEST";

  private static AmbariManagementController controller;
  private static Clusters clusters;
  private ActionDBAccessor actionDB;
  private static Injector injector;
  private ServiceFactory serviceFactory;
  private ServiceComponentFactory serviceComponentFactory;
  private ServiceComponentHostFactory serviceComponentHostFactory;
  private static AmbariMetaInfo ambariMetaInfo;
  private EntityManager entityManager;
  private static Properties backingProperties;
  private Configuration configuration;
  private ConfigHelper configHelper;
  private ConfigGroupFactory configGroupFactory;
  private OrmTestHelper helper;
  private StageFactory stageFactory;
  private HostDAO hostDAO;
  private ServiceGroupDAO serviceGroupDAO;
  private ClusterServiceDAO serviceDAO;
  private TopologyHostInfoDAO topologyHostInfoDAO;
  private HostRoleCommandDAO hostRoleCommandDAO;
  private StackManagerMock stackManagerMock;
  private RepositoryVersionDAO repositoryVersionDAO;

  RepositoryVersionEntity repositoryVersion206;


  @BeforeClass
  public static void beforeClass() throws Exception {
    InMemoryDefaultTestModule module = new InMemoryDefaultTestModule();
    backingProperties = module.getProperties();
    injector = Guice.createInjector(module);
    H2DatabaseCleaner.resetSequences(injector);
    injector.getInstance(GuiceJpaInitializer.class);
    ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    ambariMetaInfo.init();
    clusters = injector.getInstance(Clusters.class);
    controller = injector.getInstance(AmbariManagementController.class);
    TopologyManager topologyManager = injector.getInstance(TopologyManager.class);
    StageUtils.setTopologyManager(topologyManager);
    Configuration configuration = injector.getInstance(Configuration.class);
    StageUtils.setConfiguration(configuration);
    ActionManager.setTopologyManager(topologyManager);

    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());
  }

  @Before
  public void setup() throws Exception {
    EventBusSynchronizer.synchronizeAmbariEventPublisher(injector);

    entityManager = injector.getProvider(EntityManager.class).get();
    actionDB = injector.getInstance(ActionDBAccessor.class);
    serviceFactory = injector.getInstance(ServiceFactory.class);
    serviceComponentFactory = injector.getInstance(
            ServiceComponentFactory.class);
    serviceComponentHostFactory = injector.getInstance(
            ServiceComponentHostFactory.class);
    configuration = injector.getInstance(Configuration.class);
    configHelper = injector.getInstance(ConfigHelper.class);
    configGroupFactory = injector.getInstance(ConfigGroupFactory.class);
    helper = injector.getInstance(OrmTestHelper.class);
    stageFactory = injector.getInstance(StageFactory.class);
    hostDAO = injector.getInstance(HostDAO.class);
    serviceGroupDAO = injector.getInstance(ServiceGroupDAO.class);
    serviceDAO = injector.getInstance(ClusterServiceDAO.class);
    topologyHostInfoDAO = injector.getInstance(TopologyHostInfoDAO.class);
    hostRoleCommandDAO = injector.getInstance(HostRoleCommandDAO.class);
    stackManagerMock = (StackManagerMock) ambariMetaInfo.getStackManager();
    EasyMock.replay(injector.getInstance(AuditLogger.class));

    repositoryVersion206 = helper.getOrCreateRepositoryVersion(
            new StackId("HDP-2.0.6"), "2.0.6-1234");



    repositoryVersionDAO = injector.getInstance(RepositoryVersionDAO.class);
  }

  @After
  public void teardown() {
    actionDB = null;
    EasyMock.reset(injector.getInstance(AuditLogger.class));
  }

  @AfterClass
  public static void afterClass() throws Exception {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  private static String getUniqueName() {
    return UUID.randomUUID().toString();
  }

  private void setOsFamily(Host host, String osFamily, String osVersion) {
    Map<String, String> hostAttributes = new HashMap<>();
    hostAttributes.put("os_family", osFamily);
    hostAttributes.put("os_release_version", osVersion);

    host.setHostAttributes(hostAttributes);
  }

  private void addHost(String hostname) throws Exception {
    addHostToCluster(hostname, null);
  }

  private void addHostToCluster(String hostname, String clusterName) throws Exception {

    if (!clusters.hostExists(hostname)) {
      clusters.addHost(hostname);
      setOsFamily(clusters.getHost(hostname), "redhat", "6.3");
      clusters.getHost(hostname).setState(HostState.HEALTHY);
    }

    if (null != clusterName) {
      clusters.mapHostToCluster(hostname, clusterName);
    }
  }


  /**
   * Creates a Cluster object, along with its corresponding ClusterVersion based on the stack.
   * @param clusterName Cluster name
   * @throws Exception
   */
  private void createCluster(String clusterName) throws Exception{
    ClusterRequest r = new ClusterRequest(null, clusterName, State.INSTALLED.name(), SecurityType.NONE, "HDP-0.1", null);
    controller.createCluster(r);
  }

  private void createService(String clusterName, String serviceGroupName, String serviceName, State desiredState) throws Exception {
    createService(clusterName, serviceGroupName, serviceName, repositoryVersion206, desiredState);
  }

  private void createService(String clusterName, String serviceGroupName, String serviceName,
                             RepositoryVersionEntity repositoryVersion, State desiredState
  )
          throws Exception {
    String dStateStr = null;
    if (desiredState != null) {
      dStateStr = desiredState.toString();
    }

    ServiceRequest r1 = new ServiceRequest(clusterName, serviceGroupName, serviceName,
            repositoryVersion.getId(), dStateStr,
            null);

    ServiceResourceProviderTest.createServices(controller, repositoryVersionDAO, Collections.singleton(r1));
  }

  private void createServiceComponent(String clusterName,
                                      String serviceGroupName, String serviceName, String componentName, State desiredState
  )
          throws Exception {
    String dStateStr = null;
    if (desiredState != null) {
      dStateStr = desiredState.toString();
    }
    ServiceComponentRequest r = new ServiceComponentRequest(clusterName, serviceGroupName,
            serviceName, componentName, componentName, dStateStr);
    ComponentResourceProviderTest.createComponents(controller, Collections.singleton(r));
  }

  private void createServiceComponentHost(String clusterName,
                                          String serviceGroupName, String serviceName, String componentName, String hostname,
                                          State desiredState
  ) throws Exception {
    String dStateStr = null;
    if (desiredState != null) {
      dStateStr = desiredState.toString();
    }
    ServiceComponentHostRequest r = new ServiceComponentHostRequest(clusterName, serviceGroupName,
            serviceName, componentName, componentName, hostname, dStateStr);
    controller.createHostComponents(Collections.singleton(r));
  }


  private long installService(String clusterName, String serviceGroupName, String serviceName, boolean runSmokeTests, boolean reconfigureClients)
          throws Exception {
    return installService(clusterName, serviceGroupName, serviceName, runSmokeTests, reconfigureClients, null, null);
  }


  /**
   * Allows to set maintenanceStateHelper. For use when there is anything to test
   * with maintenance mode.
   */
  private long installService(String clusterName, String serviceGroupName, String serviceName,
                              boolean runSmokeTests, boolean reconfigureClients,
                              MaintenanceStateHelper maintenanceStateHelper,
                              Map<String, String> mapRequestPropsInput
  )
          throws Exception {

    ServiceRequest r = new ServiceRequest(clusterName, serviceGroupName, serviceName, repositoryVersion206.getId(),
            State.INSTALLED.toString(), null);

    Set<ServiceRequest> requests = new HashSet<>();
    requests.add(r);

    Map<String, String> mapRequestProps = new HashMap<>();
    mapRequestProps.put("context", "Called from a test");
    if(mapRequestPropsInput != null) {
      mapRequestProps.putAll(mapRequestPropsInput);
    }

    RequestStatusResponse resp = ServiceResourceProviderTest.updateServices(controller, requests,
            mapRequestProps, runSmokeTests, reconfigureClients, maintenanceStateHelper);

    Assert.assertEquals(State.INSTALLED,
            clusters.getCluster(clusterName).getService(serviceName)
                    .getDesiredState());

    if (resp != null) {
      // manually change live state to stopped as no running action manager
      List<HostRoleCommand> commands = actionDB.getRequestTasks(resp.getRequestId());
      for (HostRoleCommand cmd : commands) {
        clusters.getCluster(clusterName).getService(serviceName).getServiceComponent(cmd.getRole().name())
                .getServiceComponentHost(cmd.getHostName()).setState(State.INSTALLED);
      }
      return resp.getRequestId();
    } else {
      return -1;
    }
  }


  private Cluster setupClusterWithHosts(String clusterName, String stackId, List<String> hosts,
                                        String osType) throws Exception {
    ClusterRequest r = new ClusterRequest(null, clusterName, stackId, null);
    controller.createCluster(r);
    Cluster c1 = clusters.getCluster(clusterName);
    for (String host : hosts) {
      addHostToCluster(host, clusterName);
    }
    return c1;
  }

  private Cluster createDefaultCluster(String clusterName) throws Exception {
    // !!! weird, but the assertions are banking on alphabetical order
    final String host1 = "a" + getUniqueName();
    final String host2 = "b" + getUniqueName();
    final String host3 = "c" + getUniqueName();

    setupClusterWithHosts(clusterName, "HDP-2.0.6", Arrays.asList(host1, host2, host3), "centos6");

    Cluster cluster = clusters.getCluster(clusterName);
    cluster.setDesiredStackVersion(new StackId("HDP-2.0.6"));
    cluster.setCurrentStackVersion(new StackId("HDP-2.0.6"));

    RepositoryVersionEntity repositoryVersion = repositoryVersion206;

    ServiceGroup serviceGroupCore = cluster.addServiceGroup(SERVICE_GROUP_NAME_CORE, "HDP-2.0.6");
    ServiceGroup serviceGroupTest = cluster.addServiceGroup(SERVICE_GROUP_NAME_TEST, "HDP-2.0.6");


    return cluster;
  }



  @Test
  public void testDefaultRequestForServiceGroupDependencyCreation() throws Exception {
    final String cluster1 = getUniqueName();

    Cluster cluster = createDefaultCluster(cluster1);

    ServiceGroup serviceGroupCore = cluster.getServiceGroup(SERVICE_GROUP_NAME_CORE);
    ServiceGroup serviceGroupTest = cluster.getServiceGroup(SERVICE_GROUP_NAME_TEST);

    Set<ServiceGroupDependencyRequest> requests = new HashSet<>();
    ServiceGroupDependencyRequest request = new ServiceGroupDependencyRequest(cluster1, serviceGroupCore.getServiceGroupName(),
            cluster1, serviceGroupTest.getServiceGroupName(), null);
    requests.add(request);

    ServiceGroupDependencyResourceProvider serviceGroupDependencyResourceProvider = new ServiceGroupDependencyResourceProvider(controller);
    Set<ServiceGroupDependencyResponse> responses = serviceGroupDependencyResourceProvider.createServiceGroupDependencies(requests);

    Assert.assertEquals(responses.size(), 1);

    ServiceGroupDependencyResponse serviceGroupDependencyResponse = responses.iterator().next();

    Assert.assertEquals(serviceGroupDependencyResponse.getServiceGroupName(), serviceGroupCore.getServiceGroupName());
    Assert.assertEquals(serviceGroupDependencyResponse.getDependencyGroupName(), serviceGroupTest.getServiceGroupName());
    Assert.assertEquals(serviceGroupDependencyResponse.getClusterName(), cluster1);
    Assert.assertEquals(serviceGroupDependencyResponse.getDependencyClusterName(), cluster1);
  }



  @Test
  public void testRequestWithInvalidServiceGroupName() throws Exception {
    final String cluster1 = getUniqueName();

    Cluster cluster = createDefaultCluster(cluster1);

    ServiceGroup serviceGroupCore = cluster.getServiceGroup(SERVICE_GROUP_NAME_CORE);


    Set<ServiceGroupDependencyRequest> requests = new HashSet<>();
    ServiceGroupDependencyRequest request = new ServiceGroupDependencyRequest(cluster1, serviceGroupCore.getServiceGroupName(),
            cluster1, "invalid_service_group", null);
    requests.add(request);

    ServiceGroupDependencyResourceProvider serviceGroupDependencyResourceProvider = new ServiceGroupDependencyResourceProvider(controller);
    boolean isFailed = false;
    Exception exception = null;
    try {
      Set<ServiceGroupDependencyResponse> responses = serviceGroupDependencyResourceProvider.createServiceGroupDependencies(requests);
    } catch (Exception e) {
      isFailed = true;
      exception = e;
    }

    Assert.assertTrue(isFailed);
    Assert.assertTrue(exception.getMessage().contains("Unable to find dependent service group"));

  }

  @Test
  public void testRequestWithInvalidClusterName() throws Exception {
    final String cluster1 = getUniqueName();

    Cluster cluster = createDefaultCluster(cluster1);

    ServiceGroup serviceGroupCore = cluster.getServiceGroup(SERVICE_GROUP_NAME_CORE);
    ServiceGroup serviceGroupTest = cluster.getServiceGroup(SERVICE_GROUP_NAME_TEST);


    Set<ServiceGroupDependencyRequest> requests = new HashSet<>();
    ServiceGroupDependencyRequest request = new ServiceGroupDependencyRequest(cluster1, serviceGroupCore.getServiceGroupName(),
            "invalid_cluster_name", serviceGroupTest.getServiceGroupName(), null);
    requests.add(request);

    ServiceGroupDependencyResourceProvider serviceGroupDependencyResourceProvider = new ServiceGroupDependencyResourceProvider(controller);
    boolean isFailed = false;
    Exception exception = null;
    try {
      Set<ServiceGroupDependencyResponse> responses = serviceGroupDependencyResourceProvider.createServiceGroupDependencies(requests);
    } catch (Exception e) {
      isFailed = true;
      exception = e;
    }

    Assert.assertTrue(isFailed);
    Assert.assertTrue(exception.getMessage().contains("Cluster not found"));

  }


  @Test
  public void testDeleteRequestForServiceDependency() throws Exception {
    final String cluster1 = getUniqueName();

    Cluster cluster = createDefaultCluster(cluster1);

    ServiceGroup serviceGroupCore = cluster.getServiceGroup(SERVICE_GROUP_NAME_CORE);
    ServiceGroup serviceGroupTest = cluster.getServiceGroup(SERVICE_GROUP_NAME_TEST);

    Set<ServiceGroupDependencyRequest> requests = new HashSet<>();
    ServiceGroupDependencyRequest request = new ServiceGroupDependencyRequest(cluster1, serviceGroupCore.getServiceGroupName(),
            cluster1, serviceGroupTest.getServiceGroupName(), null);
    requests.add(request);

    ServiceGroupDependencyResourceProvider serviceGroupDependencyResourceProvider = new ServiceGroupDependencyResourceProvider(controller);
    Set<ServiceGroupDependencyResponse> responses = serviceGroupDependencyResourceProvider.createServiceGroupDependencies(requests);

    Assert.assertEquals(responses.size(), 1);

    ServiceGroupDependencyResponse serviceGroupDependencyResponse = responses.iterator().next();

    Assert.assertEquals(serviceGroupDependencyResponse.getServiceGroupName(), serviceGroupCore.getServiceGroupName());
    Assert.assertEquals(serviceGroupDependencyResponse.getDependencyGroupName(), serviceGroupTest.getServiceGroupName());
    Assert.assertEquals(serviceGroupDependencyResponse.getClusterName(), cluster1);
    Assert.assertEquals(serviceGroupDependencyResponse.getDependencyClusterName(), cluster1);


    Set<ServiceGroupDependencyRequest> deleteRequests = new HashSet<>();
    ServiceGroupDependencyRequest deleteRequest = new ServiceGroupDependencyRequest(cluster1, serviceGroupCore.getServiceGroupName(),
            null, null, serviceGroupDependencyResponse.getDependencyId());
    deleteRequests.add(deleteRequest);

    serviceGroupDependencyResourceProvider.deleteServiceGroupDependencies(deleteRequests);
    ServiceGroup updatedCoreServiceGroup = cluster.getServiceGroup(SERVICE_GROUP_NAME_CORE);

    Assert.assertTrue(updatedCoreServiceGroup.getServiceGroupDependencies().isEmpty());

  }

  @Test
  public void testGetRequestForServiceDependencyCreation() throws Exception {
    final String cluster1 = getUniqueName();

    Cluster cluster = createDefaultCluster(cluster1);

    ServiceGroup serviceGroupCore = cluster.getServiceGroup(SERVICE_GROUP_NAME_CORE);
    ServiceGroup serviceGroupTest = cluster.getServiceGroup(SERVICE_GROUP_NAME_TEST);

    Set<ServiceGroupDependencyRequest> requests = new HashSet<>();
    ServiceGroupDependencyRequest request = new ServiceGroupDependencyRequest(cluster1, serviceGroupCore.getServiceGroupName(),
            cluster1, serviceGroupTest.getServiceGroupName(), null);
    requests.add(request);

    ServiceGroupDependencyResourceProvider serviceGroupDependencyResourceProvider = new ServiceGroupDependencyResourceProvider(controller);
    Set<ServiceGroupDependencyResponse> responses = serviceGroupDependencyResourceProvider.createServiceGroupDependencies(requests);

    Assert.assertEquals(responses.size(), 1);

    ServiceGroupDependencyResponse serviceGroupDependencyResponse = responses.iterator().next();

    Assert.assertEquals(serviceGroupDependencyResponse.getServiceGroupName(), serviceGroupCore.getServiceGroupName());
    Assert.assertEquals(serviceGroupDependencyResponse.getDependencyGroupName(), serviceGroupTest.getServiceGroupName());
    Assert.assertEquals(serviceGroupDependencyResponse.getClusterName(), cluster1);
    Assert.assertEquals(serviceGroupDependencyResponse.getDependencyClusterName(), cluster1);


    Set<ServiceGroupDependencyRequest> getRequests = new HashSet<>();
    ServiceGroupDependencyRequest getRequest = new ServiceGroupDependencyRequest(cluster1, serviceGroupCore.getServiceGroupName(),
            null, null, serviceGroupDependencyResponse.getDependencyId());

    Set<ServiceGroupDependencyResponse> getResponses = serviceGroupDependencyResourceProvider.getServiceGroupDependencies(getRequests);

    Assert.assertEquals(responses.size(), 1);

    ServiceGroupDependencyResponse getServiceGroupDependencyResponse = responses.iterator().next();
    Assert.assertEquals(getServiceGroupDependencyResponse.getClusterName(), cluster1);
    Assert.assertEquals(getServiceGroupDependencyResponse.getDependencyClusterName(), cluster1);
    Assert.assertEquals(getServiceGroupDependencyResponse.getServiceGroupName(), serviceGroupCore.getServiceGroupName());
    Assert.assertEquals(getServiceGroupDependencyResponse.getDependencyGroupName(), serviceGroupTest.getServiceGroupName());
  }


}
