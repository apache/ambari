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
import org.apache.ambari.server.controller.ServiceDependencyRequest;
import org.apache.ambari.server.controller.ServiceDependencyResponse;
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
import org.apache.ambari.server.state.Service;
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


public class ServiceDependencyResourceProviderTest {

  private static final String SERVICE_GROUP_NAME_CORE = "CORE";
  private static final String SERVICE_GROUP_NAME_TEST = "TEST";
  private static final String SERVICE_NAME_HDFS = "HDFS";
  private static final String SERVICE_NAME_YARN = "YARN";
  private static final String SERVICE_NAME_ZOOKEEPER = "ZOOKEEPER";

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

    String stackId = "HDP-2.0.6";
    setupClusterWithHosts(clusterName, stackId, Arrays.asList(host1, host2, host3), "centos6");

    Cluster cluster = clusters.getCluster(clusterName);
    cluster.setDesiredStackVersion(new StackId(stackId));
    cluster.setCurrentStackVersion(new StackId(stackId));

    RepositoryVersionEntity repositoryVersion = repositoryVersion206;

    ServiceGroup serviceGroupCore = cluster.addServiceGroup(SERVICE_GROUP_NAME_CORE, stackId);
    ServiceGroup serviceGroupTest = cluster.addServiceGroup(SERVICE_GROUP_NAME_TEST, stackId);

    Service hdfs = cluster.addService(serviceGroupCore, SERVICE_NAME_HDFS, SERVICE_NAME_HDFS, repositoryVersion);
    Service yarn = cluster.addService(serviceGroupCore, SERVICE_NAME_YARN, SERVICE_NAME_YARN, repositoryVersion);
    Service zookeeper = cluster.addService(serviceGroupTest, SERVICE_NAME_ZOOKEEPER, SERVICE_NAME_ZOOKEEPER, repositoryVersion);

    return cluster;
  }



  @Test
  public void testDefaultRequestForServiceDependencyCreation() throws Exception {
    final String cluster1 = getUniqueName();

    Cluster cluster = createDefaultCluster(cluster1);

    ServiceGroup serviceGroupCore = cluster.getServiceGroup(SERVICE_GROUP_NAME_CORE);
    Service hdfs = cluster.getService(SERVICE_GROUP_NAME_CORE, SERVICE_NAME_HDFS);
    Service yarn = cluster.getService(SERVICE_GROUP_NAME_CORE, SERVICE_NAME_YARN);

    Set<ServiceDependencyRequest> requests = new HashSet<>();
    ServiceDependencyRequest request = new ServiceDependencyRequest(cluster1, hdfs.getName(), serviceGroupCore.getServiceGroupName(),
            cluster1, serviceGroupCore.getServiceGroupName(), yarn.getName(), null);
    requests.add(request);

    ServiceDependencyResourceProvider serviceDependencyResourceProvider = new ServiceDependencyResourceProvider(controller);
    Set<ServiceDependencyResponse> responses = serviceDependencyResourceProvider.createServiceDependencies(requests);

    Assert.assertEquals(responses.size(), 1);

    ServiceDependencyResponse serviceDependencyResponse = responses.iterator().next();
    Assert.assertEquals(serviceDependencyResponse.getServiceName(), hdfs.getName());
    Assert.assertEquals(serviceDependencyResponse.getDependencyServiceName(), yarn.getName());
    Assert.assertEquals(serviceDependencyResponse.getClusterName(), cluster1);
    Assert.assertEquals(serviceDependencyResponse.getDependencyClusterName(), cluster1);
    Assert.assertEquals(serviceDependencyResponse.getServiceGroupName(), serviceGroupCore.getServiceGroupName());
    Assert.assertEquals(serviceDependencyResponse.getDependencyServiceGroupName(), serviceGroupCore.getServiceGroupName());
  }


  @Test
  public void testRequestWithTwoDifferentServiceGroupsForServiceDependencyCreation() throws Exception {
    final String cluster1 = getUniqueName();

    Cluster cluster = createDefaultCluster(cluster1);

    ServiceGroup serviceGroupCore = cluster.getServiceGroup(SERVICE_GROUP_NAME_CORE);
    ServiceGroup serviceGroupTest = cluster.getServiceGroup(SERVICE_GROUP_NAME_TEST);
    Service hdfs = cluster.getService(SERVICE_GROUP_NAME_CORE, SERVICE_NAME_HDFS);
    Service zookeeper = cluster.getService(SERVICE_GROUP_NAME_TEST, SERVICE_NAME_ZOOKEEPER);

    Set<ServiceDependencyRequest> requests = new HashSet<>();
    ServiceDependencyRequest request = new ServiceDependencyRequest(cluster1, hdfs.getName(), serviceGroupCore.getServiceGroupName(),
            cluster1, serviceGroupTest.getServiceGroupName(), zookeeper.getName(), null);
    requests.add(request);

    ServiceDependencyResourceProvider serviceDependencyResourceProvider = new ServiceDependencyResourceProvider(controller);
    Set<ServiceDependencyResponse> responses = serviceDependencyResourceProvider.createServiceDependencies(requests);

    Assert.assertEquals(responses.size(), 1);

    ServiceDependencyResponse serviceDependencyResponse = responses.iterator().next();
    Assert.assertEquals(serviceDependencyResponse.getServiceName(), hdfs.getName());
    Assert.assertEquals(serviceDependencyResponse.getDependencyServiceName(), zookeeper.getName());
    Assert.assertEquals(serviceDependencyResponse.getClusterName(), cluster1);
    Assert.assertEquals(serviceDependencyResponse.getDependencyClusterName(), cluster1);
    Assert.assertEquals(serviceDependencyResponse.getServiceGroupName(), serviceGroupCore.getServiceGroupName());
    Assert.assertEquals(serviceDependencyResponse.getDependencyServiceGroupName(), serviceGroupTest.getServiceGroupName());
  }

  @Test
  public void testRequestWithInvalidServiceGroupName() throws Exception {
    final String cluster1 = getUniqueName();

    Cluster cluster = createDefaultCluster(cluster1);

    ServiceGroup serviceGroupCore = cluster.getServiceGroup(SERVICE_GROUP_NAME_CORE);
    Service hdfs = cluster.getService(SERVICE_GROUP_NAME_CORE, SERVICE_NAME_HDFS);
    Service zookeeper = cluster.getService(SERVICE_GROUP_NAME_TEST, SERVICE_NAME_ZOOKEEPER);

    Set<ServiceDependencyRequest> requests = new HashSet<>();
    ServiceDependencyRequest request = new ServiceDependencyRequest(cluster1, hdfs.getName(), serviceGroupCore.getServiceGroupName(),
            cluster1, "invalid_service_group_name", zookeeper.getName(), null);
    requests.add(request);

    ServiceDependencyResourceProvider serviceDependencyResourceProvider = new ServiceDependencyResourceProvider(controller);
    boolean isFailed = false;
    Exception exception = null;
    try {
      Set<ServiceDependencyResponse> responses = serviceDependencyResourceProvider.createServiceDependencies(requests);
    } catch (Exception e) {
      isFailed = true;
      exception = e;
    }

    Assert.assertTrue(isFailed);
    Assert.assertTrue(exception.getMessage().contains("ServiceGroup not found"));

  }

  @Test
  public void testRequestWithInvalidClusterName() throws Exception {
    final String cluster1 = getUniqueName();

    Cluster cluster = createDefaultCluster(cluster1);

    ServiceGroup serviceGroupCore = cluster.getServiceGroup(SERVICE_GROUP_NAME_CORE);
    Service hdfs = cluster.getService(SERVICE_GROUP_NAME_CORE, SERVICE_NAME_HDFS);
    Service zookeeper = cluster.getService(SERVICE_GROUP_NAME_TEST, SERVICE_NAME_ZOOKEEPER);

    Set<ServiceDependencyRequest> requests = new HashSet<>();
    ServiceDependencyRequest request = new ServiceDependencyRequest(cluster1, hdfs.getName(), serviceGroupCore.getServiceGroupName(),
            "invalid_cluster_name", serviceGroupCore.getServiceGroupName(), zookeeper.getName(), null);
    requests.add(request);

    ServiceDependencyResourceProvider serviceDependencyResourceProvider = new ServiceDependencyResourceProvider(controller);
    boolean isFailed = false;
    Exception exception = null;
    try {
      Set<ServiceDependencyResponse> responses = serviceDependencyResourceProvider.createServiceDependencies(requests);
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
    Service hdfs = cluster.getService(SERVICE_GROUP_NAME_CORE, SERVICE_NAME_HDFS);
    Service yarn = cluster.getService(SERVICE_GROUP_NAME_CORE, SERVICE_NAME_YARN);

    Set<ServiceDependencyRequest> requests = new HashSet<>();
    ServiceDependencyRequest request = new ServiceDependencyRequest(cluster1, hdfs.getName(), serviceGroupCore.getServiceGroupName(),
            cluster1, serviceGroupCore.getServiceGroupName(), yarn.getName(), null);
    requests.add(request);

    ServiceDependencyResourceProvider serviceDependencyResourceProvider = new ServiceDependencyResourceProvider(controller);
    Set<ServiceDependencyResponse> responses = serviceDependencyResourceProvider.createServiceDependencies(requests);

    Assert.assertEquals(responses.size(), 1);

    ServiceDependencyResponse serviceDependencyResponse = responses.iterator().next();
    Assert.assertEquals(serviceDependencyResponse.getServiceName(), hdfs.getName());
    Assert.assertEquals(serviceDependencyResponse.getDependencyServiceName(), yarn.getName());
    Assert.assertEquals(serviceDependencyResponse.getClusterName(), cluster1);
    Assert.assertEquals(serviceDependencyResponse.getDependencyClusterName(), cluster1);
    Assert.assertEquals(serviceDependencyResponse.getServiceGroupName(), serviceGroupCore.getServiceGroupName());
    Assert.assertEquals(serviceDependencyResponse.getDependencyServiceGroupName(), serviceGroupCore.getServiceGroupName());


    Set<ServiceDependencyRequest> deleteRequests = new HashSet<>();
    ServiceDependencyRequest deleteRequest = new ServiceDependencyRequest(cluster1, hdfs.getName(), serviceGroupCore.getServiceGroupName(),
            null, null, null, serviceDependencyResponse.getDependencyId());
    deleteRequests.add(deleteRequest);

    serviceDependencyResourceProvider.deleteServiceDependencies(deleteRequests);
    Service updatedHdfs = cluster.getService(SERVICE_GROUP_NAME_CORE, SERVICE_NAME_HDFS);

    Assert.assertTrue(updatedHdfs.getServiceDependencies().isEmpty());

  }

  @Test
  public void testGetRequestForServiceDependencyCreation() throws Exception {
    final String cluster1 = getUniqueName();

    Cluster cluster = createDefaultCluster(cluster1);

    ServiceGroup serviceGroupCore = cluster.getServiceGroup(SERVICE_GROUP_NAME_CORE);
    Service hdfs = cluster.getService(SERVICE_GROUP_NAME_CORE, SERVICE_NAME_HDFS);
    Service yarn = cluster.getService(SERVICE_GROUP_NAME_CORE, SERVICE_NAME_YARN);

    Set<ServiceDependencyRequest> requests = new HashSet<>();
    ServiceDependencyRequest request = new ServiceDependencyRequest(cluster1, hdfs.getName(), serviceGroupCore.getServiceGroupName(),
            cluster1, serviceGroupCore.getServiceGroupName(), yarn.getName(), null);
    requests.add(request);

    ServiceDependencyResourceProvider serviceDependencyResourceProvider = new ServiceDependencyResourceProvider(controller);
    Set<ServiceDependencyResponse> responses = serviceDependencyResourceProvider.createServiceDependencies(requests);

    Assert.assertEquals(responses.size(), 1);

    ServiceDependencyResponse serviceDependencyResponse = responses.iterator().next();
    Assert.assertEquals(serviceDependencyResponse.getServiceName(), hdfs.getName());
    Assert.assertEquals(serviceDependencyResponse.getDependencyServiceName(), yarn.getName());
    Assert.assertEquals(serviceDependencyResponse.getClusterName(), cluster1);
    Assert.assertEquals(serviceDependencyResponse.getDependencyClusterName(), cluster1);
    Assert.assertEquals(serviceDependencyResponse.getServiceGroupName(), serviceGroupCore.getServiceGroupName());
    Assert.assertEquals(serviceDependencyResponse.getDependencyServiceGroupName(), serviceGroupCore.getServiceGroupName());


    Set<ServiceDependencyRequest> getRequests = new HashSet<>();
    ServiceDependencyRequest getRequest = new ServiceDependencyRequest(cluster1, hdfs.getName(), serviceGroupCore.getServiceGroupName(),
            null, null, null, serviceDependencyResponse.getDependencyId());

    Set<ServiceDependencyResponse> getResponses = serviceDependencyResourceProvider.getServiceDependencies(getRequests);

    Assert.assertEquals(responses.size(), 1);

    ServiceDependencyResponse getServiceDependencyResponse = responses.iterator().next();
    Assert.assertEquals(getServiceDependencyResponse.getServiceName(), hdfs.getName());
    Assert.assertEquals(getServiceDependencyResponse.getDependencyServiceName(), yarn.getName());
    Assert.assertEquals(getServiceDependencyResponse.getClusterName(), cluster1);
    Assert.assertEquals(getServiceDependencyResponse.getDependencyClusterName(), cluster1);
    Assert.assertEquals(getServiceDependencyResponse.getServiceGroupName(), serviceGroupCore.getServiceGroupName());
    Assert.assertEquals(getServiceDependencyResponse.getDependencyServiceGroupName(), serviceGroupCore.getServiceGroupName());
  }


}
