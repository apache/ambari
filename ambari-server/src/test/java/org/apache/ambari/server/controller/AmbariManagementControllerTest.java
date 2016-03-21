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

package org.apache.ambari.server.controller;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringReader;
import java.lang.reflect.Type;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ClusterNotFoundException;
import org.apache.ambari.server.DuplicateResourceException;
import org.apache.ambari.server.HostNotFoundException;
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.ParentObjectNotFoundException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.ServiceNotFoundException;
import org.apache.ambari.server.StackAccessException;
import org.apache.ambari.server.actionmanager.ActionDBAccessor;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.ActionType;
import org.apache.ambari.server.actionmanager.ExecutionCommandWrapper;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.actionmanager.Request;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.actionmanager.TargetHostType;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.internal.ComponentResourceProviderTest;
import org.apache.ambari.server.controller.internal.HostComponentResourceProviderTest;
import org.apache.ambari.server.controller.internal.HostResourceProviderTest;
import org.apache.ambari.server.controller.internal.RequestOperationLevel;
import org.apache.ambari.server.controller.internal.RequestResourceFilter;
import org.apache.ambari.server.controller.internal.ServiceResourceProviderTest;
import org.apache.ambari.server.controller.internal.TaskResourceProvider;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.customactions.ActionDefinition;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.ExecutionCommandDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.TopologyHostInfoDAO;
import org.apache.ambari.server.orm.dao.WidgetDAO;
import org.apache.ambari.server.orm.dao.WidgetLayoutDAO;
import org.apache.ambari.server.orm.entities.ExecutionCommandEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.WidgetEntity;
import org.apache.ambari.server.orm.entities.WidgetLayoutEntity;
import org.apache.ambari.server.orm.entities.WidgetLayoutUserWidgetEntity;
import org.apache.ambari.server.security.authorization.Users;
import org.apache.ambari.server.serveraction.ServerAction;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.ConfigImpl;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostComponentAdminState;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentFactory;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.ServiceOsSpecific;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.configgroup.ConfigGroup;
import org.apache.ambari.server.state.configgroup.ConfigGroupFactory;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostInstallEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostOpSucceededEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostServerActionEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStartEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStartedEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStopEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStoppedEvent;
import org.apache.ambari.server.topology.TopologyManager;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.collections.CollectionUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

import junit.framework.Assert;

public class AmbariManagementControllerTest {

  private static final Logger LOG =
      LoggerFactory.getLogger(AmbariManagementControllerTest.class);

  private static final String STACK_NAME = "HDP";

  private static final String STACK_VERSION = "0.2";
  private static final String NEW_STACK_VERSION = "2.0.6";
  private static final String OS_TYPE = "centos5";
  private static final String REPO_ID = "HDP-1.1.1.16";
  private static final String PROPERTY_NAME = "hbase.regionserver.msginterval";
  private static final String SERVICE_NAME = "HDFS";
  private static final String FAKE_SERVICE_NAME = "FAKENAGIOS";
  private static final int STACK_VERSIONS_CNT = 15;
  private static final int REPOS_CNT = 3;
  private static final int STACKS_CNT = 3;
  private static final int STACK_PROPERTIES_CNT = 103;
  private static final int STACK_COMPONENTS_CNT = 4;
  private static final int OS_CNT = 2;

  private static final String NON_EXT_VALUE = "XXX";
  private static final String INCORRECT_BASE_URL = "http://incorrect.url";

  private static final String COMPONENT_NAME = "NAMENODE";

  private static final String REQUEST_CONTEXT_PROPERTY = "context";

  private static final String CLUSTER_HOST_INFO = "clusterHostInfo";

  private AmbariManagementController controller;
  private Clusters clusters;
  private ActionDBAccessor actionDB;
  private Injector injector;
  private ServiceFactory serviceFactory;
  private ServiceComponentFactory serviceComponentFactory;
  private ServiceComponentHostFactory serviceComponentHostFactory;
  private AmbariMetaInfo ambariMetaInfo;
  private Users users;
  private EntityManager entityManager;
  private Properties backingProperties;
  private Configuration configuration;
  private ConfigHelper configHelper;
  private ConfigGroupFactory configGroupFactory;
  private OrmTestHelper helper;
  private StageFactory stageFactory;
  private HostDAO hostDAO;
  private TopologyHostInfoDAO topologyHostInfoDAO;
  private HostRoleCommandDAO hostRoleCommandDAO;
  private TopologyManager topologyManager;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Before
  public void setup() throws Exception {
    InMemoryDefaultTestModule module = new InMemoryDefaultTestModule();
    backingProperties = module.getProperties();
    injector = Guice.createInjector(module);
    injector.getInstance(GuiceJpaInitializer.class);
    entityManager = injector.getInstance(EntityManager.class);
    clusters = injector.getInstance(Clusters.class);
    actionDB = injector.getInstance(ActionDBAccessor.class);
    controller = injector.getInstance(AmbariManagementController.class);
    serviceFactory = injector.getInstance(ServiceFactory.class);
    serviceComponentFactory = injector.getInstance(
        ServiceComponentFactory.class);
    serviceComponentHostFactory = injector.getInstance(
        ServiceComponentHostFactory.class);
    ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    ambariMetaInfo.init();
    users = injector.getInstance(Users.class);
    configuration = injector.getInstance(Configuration.class);
    configHelper = injector.getInstance(ConfigHelper.class);
    configGroupFactory = injector.getInstance(ConfigGroupFactory.class);
    helper = injector.getInstance(OrmTestHelper.class);
    stageFactory = injector.getInstance(StageFactory.class);
    hostDAO = injector.getInstance(HostDAO.class);
    topologyHostInfoDAO = injector.getInstance(TopologyHostInfoDAO.class);
    hostRoleCommandDAO = injector.getInstance(HostRoleCommandDAO.class);
    topologyManager = injector.getInstance(TopologyManager.class);
    StageUtils.setTopologyManager(topologyManager);
    ActionManager.setTopologyManager(topologyManager);
  }

  @After
  public void teardown() {
    injector.getInstance(PersistService.class).stop();
    actionDB = null;
  }

  private void setOsFamily(Host host, String osFamily, String osVersion) {
    Map<String, String> hostAttributes = new HashMap<String, String>();
    hostAttributes.put("os_family", osFamily);
    hostAttributes.put("os_release_version", osVersion);

    host.setHostAttributes(hostAttributes);
  }

  private void addHost(String hostname) throws AmbariException {
    addHostToCluster(hostname, null);
  }

  private void addHostToCluster(String hostname, String clusterName) throws AmbariException {
    clusters.addHost(hostname);
    setOsFamily(clusters.getHost(hostname), "redhat", "6.3");
    clusters.getHost(hostname).setState(HostState.HEALTHY);
    clusters.getHost(hostname).persist();
    if (null != clusterName) {
      clusters.mapHostToCluster(hostname, clusterName);
    }
  }

  private void deleteHost(String hostname) throws AmbariException {
    clusters.deleteHost(hostname);
  }

  /**
   * Creates a Cluster object, along with its corresponding ClusterVersion based on the stack.
   * @param clusterName Cluster name
   * @throws AmbariException
   */
  private void createCluster(String clusterName) throws AmbariException {
    ClusterRequest r = new ClusterRequest(null, clusterName, State.INSTALLED.name(), SecurityType.NONE, "HDP-0.1", null);
    controller.createCluster(r);
  }

  private void createService(String clusterName,
      String serviceName, State desiredState) throws AmbariException {
    String dStateStr = null;
    if (desiredState != null) {
      dStateStr = desiredState.toString();
    }
    ServiceRequest r1 = new ServiceRequest(clusterName, serviceName, dStateStr);
    Set<ServiceRequest> requests = new HashSet<ServiceRequest>();
    requests.add(r1);

    ServiceResourceProviderTest.createServices(controller, requests);
  }

  private void createServiceComponent(String clusterName,
      String serviceName, String componentName, State desiredState)
          throws AmbariException {
    String dStateStr = null;
    if (desiredState != null) {
      dStateStr = desiredState.toString();
    }
    ServiceComponentRequest r = new ServiceComponentRequest(clusterName,
        serviceName, componentName, dStateStr);
    Set<ServiceComponentRequest> requests =
        new HashSet<ServiceComponentRequest>();
    requests.add(r);
    ComponentResourceProviderTest.createComponents(controller, requests);
  }

  private void createServiceComponentHost(String clusterName,
      String serviceName, String componentName, String hostname,
      State desiredState) throws AmbariException {
    String dStateStr = null;
    if (desiredState != null) {
      dStateStr = desiredState.toString();
    }
    ServiceComponentHostRequest r = new ServiceComponentHostRequest(clusterName,
        serviceName, componentName, hostname, dStateStr);
    Set<ServiceComponentHostRequest> requests =
        new HashSet<ServiceComponentHostRequest>();
    requests.add(r);
    controller.createHostComponents(requests);
  }

  private void deleteServiceComponentHost(String clusterName,
                                          String serviceName, String componentName, String hostname,
                                          State desiredState) throws AmbariException {
    String dStateStr = null;
    if (desiredState != null) {
      dStateStr = desiredState.toString();
    }
    ServiceComponentHostRequest r = new ServiceComponentHostRequest(clusterName,
        serviceName, componentName, hostname, dStateStr);
    Set<ServiceComponentHostRequest> requests =
        new HashSet<ServiceComponentHostRequest>();
    requests.add(r);
    controller.deleteHostComponents(requests);
  }

  private Long createConfigGroup(Cluster cluster, String name, String tag,
                              List<String> hosts, List<Config> configs)
                              throws AmbariException {

    Map<Long, Host> hostMap = new HashMap<Long, Host>();
    Map<String, Config> configMap = new HashMap<String, Config>();

    for (String hostname : hosts) {
      Host host = clusters.getHost(hostname);
      HostEntity hostEntity = hostDAO.findByName(hostname);
      hostMap.put(hostEntity.getHostId(), host);
    }

    for (Config config : configs) {
      configMap.put(config.getType(), config);
    }

    ConfigGroup configGroup = configGroupFactory.createNew(cluster, name,
      tag, "", configMap, hostMap);

    configGroup.persist();
    cluster.addConfigGroup(configGroup);

    return configGroup.getId();
  }

  private long stopService(String clusterName, String serviceName,
      boolean runSmokeTests, boolean reconfigureClients) throws
    AmbariException {
    ServiceRequest r = new ServiceRequest(clusterName, serviceName, State.INSTALLED.toString());
    Set<ServiceRequest> requests = new HashSet<ServiceRequest>();
    requests.add(r);
    Map<String, String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "Called from a test");
    RequestStatusResponse resp = ServiceResourceProviderTest.updateServices(controller, requests,
      mapRequestProps, runSmokeTests, reconfigureClients);

    Assert.assertEquals(State.INSTALLED,
      clusters.getCluster(clusterName).getService(serviceName)
        .getDesiredState());

    // manually change live state to stopped as no running action manager
    for (ServiceComponent sc :
      clusters.getCluster(clusterName).getService(serviceName)
        .getServiceComponents().values()) {
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        sch.setState(State.INSTALLED);
      }
    }

    return resp.getRequestId();
  }

  private long stopServiceComponentHosts(String clusterName,
      String serviceName) throws Exception {
    Cluster c = clusters.getCluster(clusterName);
    Service s = c.getService(serviceName);
    Set<ServiceComponentHostRequest> requests = new
      HashSet<ServiceComponentHostRequest>();
    for (ServiceComponent sc : s.getServiceComponents().values()) {
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        ServiceComponentHostRequest schr = new ServiceComponentHostRequest
          (clusterName, serviceName, sc.getName(),
            sch.getHostName(), State.INSTALLED.name());
        requests.add(schr);
      }
    }
    Map<String, String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "Called from a test");
    RequestStatusResponse resp = HostComponentResourceProviderTest.updateHostComponents(controller, injector, requests,
        mapRequestProps, false);

    // manually change live state to started as no running action manager
    for (ServiceComponent sc :
      clusters.getCluster(clusterName).getService(serviceName)
        .getServiceComponents().values()) {
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        sch.setState(State.INSTALLED);
      }
    }
    return resp.getRequestId();
  }

  private long startService(String clusterName, String serviceName,
                            boolean runSmokeTests, boolean reconfigureClients) throws
      AmbariException {
    return startService(clusterName, serviceName, runSmokeTests, reconfigureClients, null);
  }


  private long startService(String clusterName, String serviceName,
                            boolean runSmokeTests, boolean reconfigureClients,
                            MaintenanceStateHelper maintenanceStateHelper) throws
      AmbariException {
    ServiceRequest r = new ServiceRequest(clusterName, serviceName,
        State.STARTED.toString());
    Set<ServiceRequest> requests = new HashSet<ServiceRequest>();
    requests.add(r);
    Map<String, String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "Called from a test");
    RequestStatusResponse resp = ServiceResourceProviderTest.updateServices(controller, requests,
        mapRequestProps, runSmokeTests, reconfigureClients, maintenanceStateHelper);

    Assert.assertEquals(State.STARTED,
        clusters.getCluster(clusterName).getService(serviceName)
            .getDesiredState());

    if (resp != null) {
      // manually change live state to stopped as no running action manager
      List<HostRoleCommand> commands = actionDB.getRequestTasks(resp.getRequestId());
      for (HostRoleCommand cmd : commands) {
        String scName = cmd.getRole().toString();
        if (!scName.endsWith("CHECK")) {
          Cluster cluster = clusters.getCluster(clusterName);
          String hostname = cmd.getHostName();
          for (Service s : cluster.getServices().values()) {
            if (s.getServiceComponents().containsKey(scName) &&
              !s.getServiceComponent(scName).isClientComponent()) {
              s.getServiceComponent(scName).getServiceComponentHost(hostname).
                setState(State.STARTED);
              break;
            }
          }
        }
      }
      return resp.getRequestId();
    } else {
      return -1;
    }
  }


  private long installService(String clusterName, String serviceName,
                              boolean runSmokeTests, boolean reconfigureClients)
          throws AmbariException {
    return installService(clusterName, serviceName, runSmokeTests, reconfigureClients, null, null);
  }

  private long installService(String clusterName, String serviceName,
                              boolean runSmokeTests, boolean reconfigureClients,
                              Map<String, String> mapRequestPropsInput)
      throws AmbariException {
    return installService(clusterName, serviceName, runSmokeTests, reconfigureClients, null, mapRequestPropsInput);
  }


  /**
   * Allows to set maintenanceStateHelper. For use when there is anything to test
   * with maintenance mode.
   */
  private long installService(String clusterName, String serviceName,
                              boolean runSmokeTests, boolean reconfigureClients,
                              MaintenanceStateHelper maintenanceStateHelper,
                              Map<String, String> mapRequestPropsInput)
          throws AmbariException {
    ServiceRequest r = new ServiceRequest(clusterName, serviceName,
        State.INSTALLED.toString());
    Set<ServiceRequest> requests = new HashSet<ServiceRequest>();
    requests.add(r);
    Map<String, String> mapRequestProps = new HashMap<String, String>();
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


  private boolean checkExceptionType(Throwable e, Class<? extends Exception> exceptionClass) {
    return e != null && (exceptionClass.isAssignableFrom(e.getClass()) || checkExceptionType(e.getCause(), exceptionClass));
  }

  @Test
  public void testCreateClusterSimple() throws AmbariException {
    String clusterName = "foo1";
    createCluster(clusterName);
    Set<ClusterResponse> r =
        controller.getClusters(Collections.singleton(
            new ClusterRequest(null, clusterName, null, null)));
    Assert.assertEquals(1, r.size());
    ClusterResponse c = r.iterator().next();
    Assert.assertEquals(clusterName, c.getClusterName());

    try {
      createCluster(clusterName);
      fail("Duplicate cluster creation should fail");
    } catch (AmbariException e) {
      // Expected
    }
  }

  @Test
  public void testCreateClusterWithInvalidStack() {
    // TODO implement test after meta data integration
  }

  @Test
  public void testCreateClusterWithHostMapping() throws AmbariException {
    Set<String> hostNames = new HashSet<String>();
    hostNames.add("h1");
    hostNames.add("h2");
    ClusterRequest r = new ClusterRequest(null, "c1", "HDP-0.1", hostNames);

    try {
      controller.createCluster(r);
      fail("Expected create cluster to fail for invalid hosts");
    } catch (Exception e) {
      // Expected
    }

    try {
      clusters.getCluster("c1");
      fail("Expected to fail for non created cluster");
    } catch (ClusterNotFoundException e) {
      // Expected
    }

    clusters.addHost("h1");
    clusters.addHost("h2");
    setOsFamily(clusters.getHost("h1"), "redhat", "6.3");
    setOsFamily(clusters.getHost("h2"), "redhat", "6.3");
    clusters.getHost("h1").persist();
    clusters.getHost("h2").persist();

    controller.createCluster(r);
    Assert.assertNotNull(clusters.getCluster("c1"));
  }

  @Test
  public void testCreateClusterWithDesiredClusterConfigs() {
    // TODO implement after configs integration
  }

  @Test
  public void testCreateClusterWithInvalidRequest() {
    ClusterRequest r = new ClusterRequest(null, null, null, null);
    r.toString();

    try {
      controller.createCluster(r);
      fail("Expected create cluster for invalid request");
    } catch (Exception e) {
      // Expected
    }

    r.setClusterId(1L);
    try {
      controller.createCluster(r);
      fail("Expected create cluster for invalid request");
    } catch (Exception e) {
      // Expected
    }
    r.setClusterId(null);

    r.setClusterName("foo");
    try {
      controller.createCluster(r);
     fail("Expected create cluster for invalid request - no stack version");
    } catch (Exception e) {
      // Expected
    }

    r.setStackVersion("HDP-1.2.0");
    r.setProvisioningState(State.INSTALLING.name());
    try {
      controller.createCluster(r);
      controller.updateClusters(Collections.singleton(r), null);

     fail("Expected create cluster for invalid request - invalid provisioning state");
    } catch (Exception e) {
      // Expected
    }
  }

  @Test
  public void testCreateServicesSimple() throws AmbariException {
    String clusterName = "foo1";
    createCluster(clusterName);
    String serviceName = "HDFS";
    clusters.getCluster("foo1").setDesiredStackVersion(
        new StackId("HDP-0.1"));
    createService(clusterName, serviceName, State.INIT);

    Service s =
        clusters.getCluster(clusterName).getService(serviceName);
    Assert.assertNotNull(s);
    Assert.assertEquals(serviceName, s.getName());
    Assert.assertEquals(clusterName, s.getCluster().getClusterName());

    ServiceRequest req = new ServiceRequest(clusterName, serviceName, null);

    Set<ServiceResponse> r =
        ServiceResourceProviderTest.getServices(controller, Collections.singleton(req));
    Assert.assertEquals(1, r.size());
    ServiceResponse resp = r.iterator().next();
    Assert.assertEquals(serviceName, resp.getServiceName());
    Assert.assertEquals(clusterName, resp.getClusterName());
    Assert.assertEquals(State.INIT.toString(),
        resp.getDesiredState());
    Assert.assertEquals("HDP-0.1", resp.getDesiredStackVersion());

    // TODO test resp.getConfigVersions()
  }

  @Test
  public void testCreateServicesWithInvalidRequest() throws AmbariException {
    // invalid request
    // dups in requests
    // multi cluster updates

    Set<ServiceRequest> set1 = new HashSet<ServiceRequest>();

    try {
      set1.clear();
      ServiceRequest rInvalid = new ServiceRequest(null, null, null);
      set1.add(rInvalid);
      ServiceResourceProviderTest.createServices(controller, set1);
      fail("Expected failure for invalid requests");
    } catch (Exception e) {
      // Expected
    }

    try {
      set1.clear();
      ServiceRequest rInvalid = new ServiceRequest("foo", null, null);
      set1.add(rInvalid);
      ServiceResourceProviderTest.createServices(controller, set1);
      fail("Expected failure for invalid requests");
    } catch (Exception e) {
      // Expected
    }

    try {
      set1.clear();
      ServiceRequest rInvalid = new ServiceRequest("foo", "bar", null);
      set1.add(rInvalid);
      ServiceResourceProviderTest.createServices(controller, set1);
      fail("Expected failure for invalid cluster");
    } catch (AmbariException e) {
      // Expected
      Assert.assertTrue(checkExceptionType(e, ParentObjectNotFoundException.class));
    }

    clusters.addCluster("foo", new StackId("HDP-0.1"));
    clusters.addCluster("bar", new StackId("HDP-0.1"));

    try {
      set1.clear();
      ServiceRequest valid1 = new ServiceRequest("foo", "HDFS", null);
      ServiceRequest valid2 = new ServiceRequest("foo", "HDFS", null);
      set1.add(valid1);
      set1.add(valid2);
      ServiceResourceProviderTest.createServices(controller, set1);
      fail("Expected failure for invalid requests");
    } catch (Exception e) {
      // Expected
    }

    try {
      set1.clear();
      ServiceRequest valid1 = new ServiceRequest("foo", "bar", null);
      set1.add(valid1);
      ServiceResourceProviderTest.createServices(controller, set1);
      fail("Expected failure for invalid service");
    } catch (Exception e) {
      // Expected
    }


    try {
      set1.clear();
      ServiceRequest valid1 = new ServiceRequest("foo", "HDFS", null);
      ServiceRequest valid2 = new ServiceRequest("bar", "HDFS", null);
      set1.add(valid1);
      set1.add(valid2);
      ServiceResourceProviderTest.createServices(controller, set1);
      fail("Expected failure for multiple clusters");
    } catch (Exception e) {
      // Expected
    }

    Assert.assertNotNull(clusters.getCluster("foo"));
    Assert.assertEquals(0, clusters.getCluster("foo").getServices().size());

    set1.clear();
    ServiceRequest valid = new ServiceRequest("foo", "HDFS", null);
    set1.add(valid);
    ServiceResourceProviderTest.createServices(controller, set1);

    try {
      set1.clear();
      ServiceRequest valid1 = new ServiceRequest("foo", "HDFS", null);
      ServiceRequest valid2 = new ServiceRequest("foo", "HDFS", null);
      set1.add(valid1);
      set1.add(valid2);
      ServiceResourceProviderTest.createServices(controller, set1);
      fail("Expected failure for existing service");
    } catch (Exception e) {
      // Expected
    }

    Assert.assertEquals(1, clusters.getCluster("foo").getServices().size());

  }

  @Test
  public void testCreateServiceWithInvalidInfo() throws AmbariException {
    String clusterName = "foo1";
    createCluster(clusterName);
    String serviceName = "HDFS";
    try {
      createService(clusterName, serviceName, State.INSTALLING);
      fail("Service creation should fail for invalid state");
    } catch (Exception e) {
      // Expected
    }
    try {
      clusters.getCluster(clusterName).getService(serviceName);
      fail("Service creation should have failed");
    } catch (Exception e) {
      // Expected
    }
    try {
      createService(clusterName, serviceName, State.INSTALLED);
      fail("Service creation should fail for invalid initial state");
    } catch (Exception e) {
      // Expected
    }

    createService(clusterName, serviceName, null);

    String serviceName2 = "MAPREDUCE";
    createService(clusterName, serviceName2, State.INIT);

    ServiceRequest r = new ServiceRequest(clusterName, null, null);
    Set<ServiceResponse> response = ServiceResourceProviderTest.getServices(controller, Collections.singleton(r));
    Assert.assertEquals(2, response.size());

    for (ServiceResponse svc : response) {
      Assert.assertTrue(svc.getServiceName().equals(serviceName)
          || svc.getServiceName().equals(serviceName2));
      Assert.assertEquals("HDP-0.1", svc.getDesiredStackVersion());
      Assert.assertEquals(State.INIT.toString(), svc.getDesiredState());
    }
  }

  @Test
  public void testCreateServicesMultiple() throws AmbariException {
    Set<ServiceRequest> set1 = new HashSet<ServiceRequest>();
    clusters.addCluster("foo", new StackId("HDP-0.1"));

    ServiceRequest valid1 = new ServiceRequest("foo", "HDFS", null);
    ServiceRequest valid2 = new ServiceRequest("foo", "MAPREDUCE", null);
    set1.add(valid1);
    set1.add(valid2);
    ServiceResourceProviderTest.createServices(controller, set1);

    try {
      valid1 = new ServiceRequest("foo", "PIG", null);
      valid2 = new ServiceRequest("foo", "MAPREDUCE", null);
      set1.add(valid1);
      set1.add(valid2);
      ServiceResourceProviderTest.createServices(controller, set1);
      fail("Expected failure for invalid services");
    } catch (AmbariException e) {
      // Expected
      Assert.assertTrue(checkExceptionType(e, DuplicateResourceException.class));
    }

    Assert.assertNotNull(clusters.getCluster("foo"));
    Assert.assertEquals(2, clusters.getCluster("foo").getServices().size());
    Assert.assertNotNull(clusters.getCluster("foo").getService("HDFS"));
    Assert.assertNotNull(clusters.getCluster("foo").getService("MAPREDUCE"));
  }

  @Test
  public void testCreateServiceComponentSimple() throws AmbariException {
    String clusterName = "foo1";
    createCluster(clusterName);
    String serviceName = "HDFS";
    createService(clusterName, serviceName, null);

    String componentName = "NAMENODE";
    try {
      createServiceComponent(clusterName, serviceName, componentName,
          State.INSTALLING);
      fail("ServiceComponent creation should fail for invalid state");
    } catch (Exception e) {
      // Expected
    }
    try {
      clusters.getCluster(clusterName).getService(serviceName)
          .getServiceComponent(componentName);
      fail("ServiceComponent creation should have failed");
    } catch (Exception e) {
      // Expected
    }

    createServiceComponent(clusterName, serviceName, componentName,
        State.INIT);
    Assert.assertNotNull(clusters.getCluster(clusterName)
        .getService(serviceName).getServiceComponent(componentName));

    ServiceComponentRequest r =
        new ServiceComponentRequest(clusterName, serviceName, null, null);
    Set<ServiceComponentResponse> response = ComponentResourceProviderTest.getComponents(controller, Collections.singleton(r));
    Assert.assertEquals(1, response.size());

    ServiceComponentResponse sc = response.iterator().next();
    Assert.assertEquals(State.INIT.toString(), sc.getDesiredState());
    Assert.assertEquals(componentName, sc.getComponentName());
    Assert.assertEquals(clusterName, sc.getClusterName());
    Assert.assertEquals(serviceName, sc.getServiceName());
  }

  @Test
  public void testCreateServiceComponentWithInvalidRequest()
      throws AmbariException {
    // multiple clusters
    // dup objects
    // existing components
    // invalid request params
    // invalid service
    // invalid cluster

    Set<ServiceComponentRequest> set1 = new HashSet<ServiceComponentRequest>();

    try {
      set1.clear();
      ServiceComponentRequest rInvalid =
          new ServiceComponentRequest(null, null, null, null);
      set1.add(rInvalid);
      ComponentResourceProviderTest.createComponents(controller, set1);
      fail("Expected failure for invalid requests");
    } catch (Exception e) {
      // Expected
    }

    try {
      set1.clear();
      ServiceComponentRequest rInvalid =
          new ServiceComponentRequest("c1", null, null, null);
      set1.add(rInvalid);
      ComponentResourceProviderTest.createComponents(controller, set1);
      fail("Expected failure for invalid requests");
    } catch (Exception e) {
      // Expected
    }

    try {
      set1.clear();
      ServiceComponentRequest rInvalid =
          new ServiceComponentRequest("c1", "s1", null, null);
      set1.add(rInvalid);
      ComponentResourceProviderTest.createComponents(controller, set1);
      fail("Expected failure for invalid requests");
    } catch (Exception e) {
      // Expected
    }

    try {
      set1.clear();
      ServiceComponentRequest rInvalid =
          new ServiceComponentRequest("c1", "s1", "sc1", null);
      set1.add(rInvalid);
      ComponentResourceProviderTest.createComponents(controller, set1);
      fail("Expected failure for invalid cluster");
    } catch (ParentObjectNotFoundException e) {
      // Expected
    }

    clusters.addCluster("c1", new StackId("HDP-0.1"));
    clusters.addCluster("c2", new StackId("HDP-0.1"));

    try {
      set1.clear();
      ServiceComponentRequest rInvalid =
          new ServiceComponentRequest("c1", "HDFS", "NAMENODE", null);
      set1.add(rInvalid);
      ComponentResourceProviderTest.createComponents(controller, set1);
      fail("Expected failure for invalid service");
    } catch (ParentObjectNotFoundException e) {
      // Expected
    }

    Cluster c1 = clusters.getCluster("c1");
    StackId stackId = new StackId("HDP-0.1");
    c1.setDesiredStackVersion(stackId);
    helper.getOrCreateRepositoryVersion(stackId, stackId.getStackVersion());
    c1.createClusterVersion(stackId, stackId.getStackVersion(), "admin",
        RepositoryVersionState.UPGRADING);
    Service s1 = serviceFactory.createNew(c1, "HDFS");
    Service s2 = serviceFactory.createNew(c1, "MAPREDUCE");
    c1.addService(s1);
    c1.addService(s2);
    s1.persist();
    s2.persist();

    set1.clear();
    ServiceComponentRequest valid1 =
        new ServiceComponentRequest("c1", "HDFS", "NAMENODE", null);
    ServiceComponentRequest valid2 =
        new ServiceComponentRequest("c1", "MAPREDUCE", "JOBTRACKER", null);
    ServiceComponentRequest valid3 =
        new ServiceComponentRequest("c1", "MAPREDUCE", "TASKTRACKER", null);
    set1.add(valid1);
    set1.add(valid2);
    set1.add(valid3);
    ComponentResourceProviderTest.createComponents(controller, set1);

    try {
      set1.clear();
      ServiceComponentRequest rInvalid1 =
          new ServiceComponentRequest("c1", "HDFS", "HDFS_CLIENT", null);
      ServiceComponentRequest rInvalid2 =
          new ServiceComponentRequest("c1", "HDFS", "HDFS_CLIENT", null);
      set1.add(rInvalid1);
      set1.add(rInvalid2);
      ComponentResourceProviderTest.createComponents(controller, set1);
      fail("Expected failure for dups in requests");
    } catch (Exception e) {
      // Expected
    }

    try {
      set1.clear();
      ServiceComponentRequest rInvalid1 =
          new ServiceComponentRequest("c1", "HDFS", "HDFS_CLIENT", null);
      ServiceComponentRequest rInvalid2 =
          new ServiceComponentRequest("c2", "HDFS", "HDFS_CLIENT", null);
      set1.add(rInvalid1);
      set1.add(rInvalid2);
      ComponentResourceProviderTest.createComponents(controller, set1);
      fail("Expected failure for multiple clusters");
    } catch (Exception e) {
      // Expected
    }

    try {
      set1.clear();
      ServiceComponentRequest rInvalid =
          new ServiceComponentRequest("c1", "HDFS", "NAMENODE", null);
      set1.add(rInvalid);
      ComponentResourceProviderTest.createComponents(controller, set1);
      fail("Expected failure for already existing component");
    } catch (Exception e) {
      // Expected
    }


    Assert.assertEquals(1, s1.getServiceComponents().size());
    Assert.assertNotNull(s1.getServiceComponent("NAMENODE"));
    Assert.assertEquals(2, s2.getServiceComponents().size());
    Assert.assertNotNull(s2.getServiceComponent("JOBTRACKER"));
    Assert.assertNotNull(s2.getServiceComponent("TASKTRACKER"));

  }


  @Test
  @Ignore
  //TODO this test becomes unstable after this patch, not reproducible locally but fails in apache jenkins jobs
  //investigate and reenable
  public void testGetExecutionCommandWithClusterEnvForRetry() throws Exception {
    String clusterName = "foo1";
    createCluster(clusterName);
    Cluster cluster = clusters.getCluster(clusterName);
    clusters.getCluster(clusterName)
        .setDesiredStackVersion(new StackId("HDP-0.1"));
    String serviceName = "HDFS";
    createService(clusterName, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";
    createServiceComponent(clusterName, serviceName, componentName1,
                           State.INIT);
    createServiceComponent(clusterName, serviceName, componentName2,
                           State.INIT);
    createServiceComponent(clusterName, serviceName, componentName3,
                           State.INIT);

    String host1 = "h1";
    String host2 = "h2";

    addHostToCluster(host1, clusterName);
    addHostToCluster(host2, clusterName);

    Map<String, String> configs = new HashMap<String, String>();
    configs.put("a", "b");
    configs.put("command_retry_enabled", "true");
    configs.put("command_retry_max_time_in_sec", "5");
    configs.put("commands_to_retry", "INSTALL");

    ConfigurationRequest cr1;
    cr1 = new ConfigurationRequest(clusterName, "cluster-env","version1",
                                   configs, null);

    ClusterRequest crReq = new ClusterRequest(cluster.getClusterId(), clusterName, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr1));
    controller.updateClusters(Collections.singleton(crReq), null);

    Map<String, String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "Called from a test");

    createServiceComponentHost(clusterName, serviceName, componentName2,
                               host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName2,
                               host2, null);
    createServiceComponentHost(clusterName, serviceName, componentName3,
                               host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName3,
                               host2, null);

    // issue an install command, expect retry is enabled
    ServiceComponentHostRequest
        schr =
        new ServiceComponentHostRequest(clusterName, "HDFS", "DATANODE", host2, "INSTALLED");
    Map<String, String> requestProps = new HashMap<String, String>();
    requestProps.put("phase", "INITIAL_INSTALL");
    RequestStatusResponse rsr = updateHostComponents(Collections.singleton(schr), requestProps, false);

    List<Stage> stages = actionDB.getAllStages(rsr.getRequestId());
    Assert.assertEquals(1, stages.size());
    Stage stage = stages.iterator().next();
    List<ExecutionCommandWrapper> execWrappers = stage.getExecutionCommands(host2);
    Assert.assertEquals(1, execWrappers.size());
    ExecutionCommandWrapper execWrapper = execWrappers.iterator().next();
    ExecutionCommand ec = execWrapper.getExecutionCommand();
    Map<String, Map<String, String>> configurations = ec.getConfigurations();
    assertNotNull(configurations);
    assertEquals(1, configurations.size());
    assertTrue(configurations.containsKey("cluster-env"));
    assertTrue(ec.getCommandParams().containsKey("max_duration_for_retries"));
    assertEquals("5", ec.getCommandParams().get("max_duration_for_retries"));
    assertTrue(ec.getCommandParams().containsKey("command_retry_enabled"));
    assertEquals("true", ec.getCommandParams().get("command_retry_enabled"));

    for (ServiceComponentHost sch : clusters.getCluster(clusterName).getServiceComponentHosts(host2)) {
      sch.setState(State.INSTALLED);
    }

    // issue an start command but no retry as phase is only INITIAL_INSTALL
    schr = new ServiceComponentHostRequest(clusterName, "HDFS", "DATANODE", host2, "STARTED");
    rsr = updateHostComponents(Collections.singleton(schr), requestProps, false);
    stages = actionDB.getAllStages(rsr.getRequestId());
    Assert.assertEquals(1, stages.size());
    stage = stages.iterator().next();
    execWrappers = stage.getExecutionCommands(host2);
    Assert.assertEquals(1, execWrappers.size());
    execWrapper = execWrappers.iterator().next();
    ec = execWrapper.getExecutionCommand();
    configurations = ec.getConfigurations();
    assertNotNull(configurations);
    assertEquals(1, configurations.size());
    assertTrue(configurations.containsKey("cluster-env"));
    assertTrue(ec.getCommandParams().containsKey("max_duration_for_retries"));
    assertEquals("5", ec.getCommandParams().get("max_duration_for_retries"));
    assertTrue(ec.getCommandParams().containsKey("command_retry_enabled"));
    assertEquals("false", ec.getCommandParams().get("command_retry_enabled"));

    configs.put("command_retry_enabled", "true");
    configs.put("command_retry_max_time_in_sec", "12");
    configs.put("commands_to_retry", "START");

    cr1 = new ConfigurationRequest(clusterName, "cluster-env","version2",
                                   configs, null);
    crReq = new ClusterRequest(cluster.getClusterId(), clusterName, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr1));
    controller.updateClusters(Collections.singleton(crReq), null);

    // issue an start command and retry is expected
    requestProps.put("phase", "INITIAL_START");
    schr = new ServiceComponentHostRequest(clusterName, "HDFS", "DATANODE", host2, "STARTED");
    rsr = updateHostComponents(Collections.singleton(schr), requestProps, false);
    stages = actionDB.getAllStages(rsr.getRequestId());
    Assert.assertEquals(1, stages.size());
    stage = stages.iterator().next();
    execWrappers = stage.getExecutionCommands(host2);
    Assert.assertEquals(1, execWrappers.size());
    execWrapper = execWrappers.iterator().next();
    ec = execWrapper.getExecutionCommand();
    configurations = ec.getConfigurations();
    assertNotNull(configurations);
    assertEquals(1, configurations.size());
    assertTrue(configurations.containsKey("cluster-env"));
    assertTrue(ec.getCommandParams().containsKey("max_duration_for_retries"));
    assertEquals("12", ec.getCommandParams().get("max_duration_for_retries"));
    assertTrue(ec.getCommandParams().containsKey("command_retry_enabled"));
    assertEquals("true", ec.getCommandParams().get("command_retry_enabled"));

    // issue an start command and retry is expected but bad cluster-env
    configs.put("command_retry_enabled", "asdf");
    configs.put("command_retry_max_time_in_sec", "-5");
    configs.put("commands_to_retry2", "START");

    cr1 = new ConfigurationRequest(clusterName, "cluster-env","version3",
                                   configs, null);
    crReq = new ClusterRequest(cluster.getClusterId(), clusterName, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr1));
    controller.updateClusters(Collections.singleton(crReq), null);

    requestProps.put("phase", "INITIAL_START");
    schr = new ServiceComponentHostRequest(clusterName, "HDFS", "DATANODE", host2, "STARTED");
    rsr = updateHostComponents(Collections.singleton(schr), requestProps, false);
    stages = actionDB.getAllStages(rsr.getRequestId());
    Assert.assertEquals(1, stages.size());
    stage = stages.iterator().next();
    execWrappers = stage.getExecutionCommands(host2);
    Assert.assertEquals(1, execWrappers.size());
    execWrapper = execWrappers.iterator().next();
    ec = execWrapper.getExecutionCommand();
    configurations = ec.getConfigurations();
    assertNotNull(configurations);
    assertEquals(1, configurations.size());
    assertTrue(configurations.containsKey("cluster-env"));
    assertTrue(ec.getCommandParams().containsKey("max_duration_for_retries"));
    assertEquals("0", ec.getCommandParams().get("max_duration_for_retries"));
    assertTrue(ec.getCommandParams().containsKey("command_retry_enabled"));
    assertEquals("false", ec.getCommandParams().get("command_retry_enabled"));
  }


  @Test
  public void testGetExecutionCommand() throws Exception {
    testCreateServiceComponentHostSimple();

    String clusterName = "foo1";
    String serviceName = "HDFS";

    Cluster cluster = clusters.getCluster(clusterName);
    Service s1 = cluster.getService(serviceName);

    // Create and attach config
    Map<String, String> configs = new HashMap<String, String>();
    configs.put("a", "b");

    ConfigurationRequest cr1,cr2;
    cr1 = new ConfigurationRequest(clusterName, "core-site","version1",
                                   configs, null);
    cr2 = new ConfigurationRequest(clusterName, "hdfs-site","version1",
                                   configs, null);

    ClusterRequest crReq = new ClusterRequest(cluster.getClusterId(), clusterName, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr1));
    controller.updateClusters(Collections.singleton(crReq), null);
    crReq = new ClusterRequest(cluster.getClusterId(), clusterName, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr2));
    controller.updateClusters(Collections.singleton(crReq), null);

    // Install
    installService(clusterName, serviceName, false, false);
    ExecutionCommand ec =
        controller.getExecutionCommand(cluster,
                                       s1.getServiceComponent("NAMENODE").getServiceComponentHost("h1"),
                                       RoleCommand.START);
    assertEquals("1-0", ec.getCommandId());
    assertEquals("foo1", ec.getClusterName());
    Map<String, Map<String, String>> configurations = ec.getConfigurations();
    assertNotNull(configurations);
    assertEquals(2, configurations.size());
    assertTrue(configurations.containsKey("hdfs-site"));
    assertTrue(configurations.containsKey("core-site"));
    assertTrue(ec.getConfigurationAttributes().containsKey("hdfs-site"));
    assertTrue(ec.getConfigurationAttributes().containsKey("core-site"));
    assertTrue(ec.getCommandParams().containsKey("max_duration_for_retries"));
    assertEquals("0", ec.getCommandParams().get("max_duration_for_retries"));
    assertTrue(ec.getCommandParams().containsKey("command_retry_enabled"));
    assertEquals("false", ec.getCommandParams().get("command_retry_enabled"));
    Map<String, Set<String>> chInfo = ec.getClusterHostInfo();
    assertTrue(chInfo.containsKey("namenode_host"));
  }

  @Test
  public void testCreateServiceComponentWithConfigs() {
    // FIXME after config impl
  }

  @Test
  public void testCreateServiceComponentMultiple() throws AmbariException {
    clusters.addCluster("c1", new StackId("HDP-0.2"));
    clusters.addCluster("c2", new StackId("HDP-0.2"));

    Cluster c1 = clusters.getCluster("c1");
    StackId stackId = new StackId("HDP-0.2");

    helper.getOrCreateRepositoryVersion(stackId, stackId.getStackVersion());
    c1.createClusterVersion(stackId, stackId.getStackVersion(), "admin",
        RepositoryVersionState.UPGRADING);

    Service s1 = serviceFactory.createNew(c1, "HDFS");
    Service s2 = serviceFactory.createNew(c1, "MAPREDUCE");
    c1.addService(s1);
    c1.addService(s2);
    s1.persist();
    s2.persist();

    Set<ServiceComponentRequest> set1 = new HashSet<ServiceComponentRequest>();
    ServiceComponentRequest valid1 =
        new ServiceComponentRequest("c1", "HDFS", "NAMENODE", null);
    ServiceComponentRequest valid2 =
        new ServiceComponentRequest("c1", "MAPREDUCE", "JOBTRACKER", null);
    ServiceComponentRequest valid3 =
        new ServiceComponentRequest("c1", "MAPREDUCE", "TASKTRACKER", null);
    set1.add(valid1);
    set1.add(valid2);
    set1.add(valid3);
    ComponentResourceProviderTest.createComponents(controller, set1);

    Assert.assertEquals(1, c1.getService("HDFS").getServiceComponents().size());
    Assert.assertEquals(2, c1.getService("MAPREDUCE").getServiceComponents().size());
    Assert.assertNotNull(c1.getService("HDFS")
        .getServiceComponent("NAMENODE"));
    Assert.assertNotNull(c1.getService("MAPREDUCE")
        .getServiceComponent("JOBTRACKER"));
    Assert.assertNotNull(c1.getService("MAPREDUCE")
        .getServiceComponent("TASKTRACKER"));
  }

  @Test
  public void testCreateServiceComponentHostSimple() throws AmbariException {
    String clusterName = "foo1";
    createCluster(clusterName);
    clusters.getCluster(clusterName)
        .setDesiredStackVersion(new StackId("HDP-0.1"));
    String serviceName = "HDFS";
    createService(clusterName, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";
    createServiceComponent(clusterName, serviceName, componentName1,
        State.INIT);
    createServiceComponent(clusterName, serviceName, componentName2,
        State.INIT);
    createServiceComponent(clusterName, serviceName, componentName3,
        State.INIT);

    String host1 = "h1";
    String host2 = "h2";

    try {
      createServiceComponentHost(clusterName, serviceName, componentName1,
          host1, State.INIT);
      fail("ServiceComponentHost creation should fail for invalid host"
          + " as host not mapped to cluster");
    } catch (Exception e) {
      // Expected
    }

    addHostToCluster(host1, clusterName);
    addHostToCluster(host2, clusterName);

    try {
      createServiceComponentHost(clusterName, serviceName, componentName1,
          host1, State.INSTALLING);
      fail("ServiceComponentHost creation should fail for invalid state");
    } catch (Exception e) {
      // Expected
    }

    try {
      clusters.getCluster(clusterName).getService(serviceName)
          .getServiceComponent(componentName1).getServiceComponentHost(host1);
      fail("ServiceComponentHost creation should have failed earlier");
    } catch (Exception e) {
      // Expected
    }

    // null service should work
    createServiceComponentHost(clusterName, null, componentName1,
        host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName2,
        host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName2,
        host2, null);
    createServiceComponentHost(clusterName, serviceName, componentName3,
        host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName3,
        host2, null);

    try {
      createServiceComponentHost(clusterName, serviceName, componentName1,
          host1, null);
      fail("ServiceComponentHost creation should fail as duplicate");
    } catch (Exception e) {
      // Expected
    }

    Assert.assertNotNull(clusters.getCluster(clusterName)
        .getService(serviceName)
        .getServiceComponent(componentName1)
        .getServiceComponentHost(host1));
    Assert.assertNotNull(clusters.getCluster(clusterName)
        .getService(serviceName)
        .getServiceComponent(componentName2)
        .getServiceComponentHost(host1));
    Assert.assertNotNull(clusters.getCluster(clusterName)
        .getService(serviceName)
        .getServiceComponent(componentName2)
        .getServiceComponentHost(host2));
    Assert.assertNotNull(clusters.getCluster(clusterName)
        .getService(serviceName)
        .getServiceComponent(componentName3)
        .getServiceComponentHost(host1));
    Assert.assertNotNull(clusters.getCluster(clusterName)
        .getService(serviceName)
        .getServiceComponent(componentName3)
        .getServiceComponentHost(host2));

    ServiceComponentHostRequest r =
        new ServiceComponentHostRequest(clusterName, serviceName,
            componentName2, null, null);

    Set<ServiceComponentHostResponse> response =
        controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(2, response.size());

  }

  @Test
  public void testCreateServiceComponentHostMultiple()
      throws AmbariException {
    String clusterName = "foo1";
    createCluster(clusterName);
    String serviceName = "HDFS";
    createService(clusterName, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    createServiceComponent(clusterName, serviceName, componentName1,
        State.INIT);
    createServiceComponent(clusterName, serviceName, componentName2,
        State.INIT);
    String host1 = "h1";
    String host2 = "h2";
    addHostToCluster(host1, clusterName);
    addHostToCluster(host2, clusterName);

    Set<ServiceComponentHostRequest> set1 =
        new HashSet<ServiceComponentHostRequest>();
    ServiceComponentHostRequest r1 =
        new ServiceComponentHostRequest(clusterName, serviceName,
            componentName1, host1, State.INIT.toString());
    ServiceComponentHostRequest r2 =
        new ServiceComponentHostRequest(clusterName, serviceName,
            componentName2, host1, State.INIT.toString());
    ServiceComponentHostRequest r3 =
        new ServiceComponentHostRequest(clusterName, serviceName,
            componentName1, host2, State.INIT.toString());
    ServiceComponentHostRequest r4 =
        new ServiceComponentHostRequest(clusterName, serviceName,
            componentName2, host2, State.INIT.toString());

    set1.add(r1);
    set1.add(r2);
    set1.add(r3);
    set1.add(r4);
    controller.createHostComponents(set1);

    Assert.assertEquals(2,
      clusters.getCluster(clusterName).getServiceComponentHosts(host1).size());
    Assert.assertEquals(2,
      clusters.getCluster(clusterName).getServiceComponentHosts(host2).size());

    Assert.assertNotNull(clusters.getCluster(clusterName)
        .getService(serviceName).getServiceComponent(componentName1)
        .getServiceComponentHost(host1));
    Assert.assertNotNull(clusters.getCluster(clusterName)
        .getService(serviceName).getServiceComponent(componentName1)
        .getServiceComponentHost(host2));
    Assert.assertNotNull(clusters.getCluster(clusterName)
        .getService(serviceName).getServiceComponent(componentName2)
        .getServiceComponentHost(host1));
    Assert.assertNotNull(clusters.getCluster(clusterName)
        .getService(serviceName).getServiceComponent(componentName2)
        .getServiceComponentHost(host2));
  }

  @Test
  public void testCreateServiceComponentHostWithInvalidRequest()
      throws AmbariException {
    // multiple clusters
    // dup objects
    // existing components
    // invalid request params
    // invalid service
    // invalid cluster
    // invalid component
    // invalid host

    Set<ServiceComponentHostRequest> set1 =
        new HashSet<ServiceComponentHostRequest>();

    try {
      set1.clear();
      ServiceComponentHostRequest rInvalid =
          new ServiceComponentHostRequest(null, null, null, null, null);
      set1.add(rInvalid);
      controller.createHostComponents(set1);
      fail("Expected failure for invalid requests");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    try {
      set1.clear();
      ServiceComponentHostRequest rInvalid =
          new ServiceComponentHostRequest("foo", null, null, null, null);
      set1.add(rInvalid);
      controller.createHostComponents(set1);
      fail("Expected failure for invalid requests");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    try {
      set1.clear();
      ServiceComponentHostRequest rInvalid =
          new ServiceComponentHostRequest("foo", "HDFS", null, null, null);
      set1.add(rInvalid);
      controller.createHostComponents(set1);
      fail("Expected failure for invalid requests");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    try {
      set1.clear();
      ServiceComponentHostRequest rInvalid =
          new ServiceComponentHostRequest("foo", "HDFS", "NAMENODE", null, null);
      set1.add(rInvalid);
      controller.createHostComponents(set1);
      fail("Expected failure for invalid requests");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    try {
      set1.clear();
      ServiceComponentHostRequest rInvalid =
          new ServiceComponentHostRequest("foo", "HDFS", "NAMENODE", "h1", null);
      set1.add(rInvalid);
      controller.createHostComponents(set1);
      fail("Expected failure for invalid cluster");
    } catch (ParentObjectNotFoundException e) {
      // Expected
    }

    clusters.addCluster("foo", new StackId("HDP-0.2"));
    clusters.addCluster("c1", new StackId("HDP-0.2"));
    clusters.addCluster("c2", new StackId("HDP-0.2"));
    Cluster foo = clusters.getCluster("foo");
    Cluster c1 = clusters.getCluster("c1");
    Cluster c2 = clusters.getCluster("c2");

    StackId stackId = new StackId("HDP-0.2");
    helper.getOrCreateRepositoryVersion(stackId, stackId.getStackVersion());

    foo.setDesiredStackVersion(stackId);
    foo.setCurrentStackVersion(stackId);
    foo.createClusterVersion(stackId, stackId.getStackVersion(), "admin",
        RepositoryVersionState.UPGRADING);
    foo.transitionClusterVersion(stackId, stackId.getStackVersion(), RepositoryVersionState.CURRENT);

    stackId = new StackId("HDP-0.2");
    c1.setDesiredStackVersion(stackId);
    c1.setCurrentStackVersion(stackId);
    c1.createClusterVersion(stackId, stackId.getStackVersion(), "admin",
        RepositoryVersionState.UPGRADING);
    c1.transitionClusterVersion(stackId, stackId.getStackVersion(), RepositoryVersionState.CURRENT);

    stackId = new StackId("HDP-0.2");
    c2.setDesiredStackVersion(stackId);
    c2.setCurrentStackVersion(stackId);
    c2.createClusterVersion(stackId, stackId.getStackVersion(), "admin",
        RepositoryVersionState.UPGRADING);
    c2.transitionClusterVersion(stackId, stackId.getStackVersion(), RepositoryVersionState.CURRENT);

    try {
      set1.clear();
      ServiceComponentHostRequest rInvalid =
          new ServiceComponentHostRequest("foo", "HDFS", "NAMENODE", "h1", null);
      set1.add(rInvalid);
      controller.createHostComponents(set1);
      fail("Expected failure for invalid service");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    Service s1 = serviceFactory.createNew(foo, "HDFS");
    foo.addService(s1);
    s1.persist();
    Service s2 = serviceFactory.createNew(c1, "HDFS");
    c1.addService(s2);
    s2.persist();
    Service s3 = serviceFactory.createNew(c2, "HDFS");
    c2.addService(s3);
    s3.persist();


    try {
      set1.clear();
      ServiceComponentHostRequest rInvalid =
          new ServiceComponentHostRequest("foo", "HDFS", "NAMENODE", "h1", null);
      set1.add(rInvalid);
      controller.createHostComponents(set1);
      fail("Expected failure for invalid service");
    } catch (AmbariException e) {
      // Expected
    }

    ServiceComponent sc1 = serviceComponentFactory.createNew(s1, "NAMENODE");
    s1.addServiceComponent(sc1);
    sc1.persist();
    ServiceComponent sc2 = serviceComponentFactory.createNew(s2, "NAMENODE");
    s2.addServiceComponent(sc2);
    sc2.persist();
    ServiceComponent sc3 = serviceComponentFactory.createNew(s3, "NAMENODE");
    s3.addServiceComponent(sc3);
    sc3.persist();


    try {
      set1.clear();
      ServiceComponentHostRequest rInvalid =
          new ServiceComponentHostRequest("foo", "HDFS", "NAMENODE", "h1", null);
      set1.add(rInvalid);
      controller.createHostComponents(set1);
      fail("Expected failure for invalid host");
    } catch (AmbariException e) {
      // Expected
    }

    clusters.addHost("h1");
    Host h1 = clusters.getHost("h1");
    h1.setIPv4("ipv41");
    h1.setIPv6("ipv61");
    setOsFamily(h1, "redhat", "6.3");
    h1.persist();
    clusters.addHost("h2");
    Host h2 = clusters.getHost("h2");
    h2.setIPv4("ipv42");
    h2.setIPv6("ipv62");
    setOsFamily(h2, "redhat", "6.3");
    h2.persist();
    clusters.addHost("h3");
    Host h3 = clusters.getHost("h3");
    h3.setIPv4("ipv43");
    h3.setIPv6("ipv63");
    setOsFamily(h3, "redhat", "6.3");
    h3.persist();

    try {
      set1.clear();
      ServiceComponentHostRequest rInvalid =
          new ServiceComponentHostRequest("foo", "HDFS", "NAMENODE", "h1", null);
      set1.add(rInvalid);
      controller.createHostComponents(set1);
      fail("Expected failure for invalid host cluster mapping");
    } catch (AmbariException e) {
      // Expected
    }

    Set<String> hostnames = new HashSet<String>();
    hostnames.add("h1");
    hostnames.add("h2");
    hostnames.add("h3");
    clusters.mapHostsToCluster(hostnames, "foo");
    clusters.mapHostsToCluster(hostnames, "c1");
    clusters.mapHostsToCluster(hostnames, "c2");

    set1.clear();
    ServiceComponentHostRequest valid =
        new ServiceComponentHostRequest("foo", "HDFS", "NAMENODE", "h1", null);
    set1.add(valid);
    controller.createHostComponents(set1);

    try {
      set1.clear();
      ServiceComponentHostRequest rInvalid1 =
          new ServiceComponentHostRequest("foo", "HDFS", "NAMENODE", "h2", null);
      ServiceComponentHostRequest rInvalid2 =
          new ServiceComponentHostRequest("foo", "HDFS", "NAMENODE", "h2", null);
      set1.add(rInvalid1);
      set1.add(rInvalid2);
      controller.createHostComponents(set1);
      fail("Expected failure for dup requests");
    } catch (DuplicateResourceException e) {
      // Expected
    }

    try {
      set1.clear();
      ServiceComponentHostRequest rInvalid1 =
          new ServiceComponentHostRequest("c1", "HDFS", "NAMENODE", "h2",
              null);
      ServiceComponentHostRequest rInvalid2 =
          new ServiceComponentHostRequest("c2", "HDFS", "NAMENODE", "h3",
              null);
      set1.add(rInvalid1);
      set1.add(rInvalid2);
      controller.createHostComponents(set1);
      fail("Expected failure for multiple clusters");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    try {
      set1.clear();
      ServiceComponentHostRequest rInvalid1 =
          new ServiceComponentHostRequest("foo", "HDFS", "NAMENODE", "h1",
              null);
      ServiceComponentHostRequest rInvalid2 =
          new ServiceComponentHostRequest("foo", "HDFS", "NAMENODE", "h2",
              null);
      set1.add(rInvalid1);
      set1.add(rInvalid2);
      controller.createHostComponents(set1);
      fail("Expected failure for already existing");
    } catch (DuplicateResourceException e) {
      // Expected
    }

    Assert.assertEquals(1, foo.getServiceComponentHosts("h1").size());
    Assert.assertEquals(0, foo.getServiceComponentHosts("h2").size());
    Assert.assertEquals(0, foo.getServiceComponentHosts("h3").size());

    set1.clear();
    ServiceComponentHostRequest valid1 =
        new ServiceComponentHostRequest("c1", "HDFS", "NAMENODE", "h1",
            null);
    set1.add(valid1);
    controller.createHostComponents(set1);

    set1.clear();
    ServiceComponentHostRequest valid2 =
        new ServiceComponentHostRequest("c2", "HDFS", "NAMENODE", "h1",
            null);
    set1.add(valid2);
    controller.createHostComponents(set1);

    Assert.assertEquals(1, foo.getServiceComponentHosts("h1").size());
    Assert.assertEquals(1, c1.getServiceComponentHosts("h1").size());
    Assert.assertEquals(1, c2.getServiceComponentHosts("h1").size());

  }

  @Test
  public void testCreateHostSimple() throws AmbariException {
    Map<String, String> hostAttributes = null;

    HostRequest r1 = new HostRequest("h1", null, hostAttributes);
    r1.toString();

    Set<HostRequest> requests = new HashSet<HostRequest>();
    requests.add(r1);
    try {
      HostResourceProviderTest.createHosts(controller, requests);
      fail("Create host should fail for non-bootstrapped host");
    } catch (Exception e) {
      // Expected
    }

    clusters.addHost("h1");
    clusters.addHost("h2");
    setOsFamily(clusters.getHost("h1"), "redhat", "5.9");
    setOsFamily(clusters.getHost("h2"), "redhat", "5.9");
    clusters.getHost("h1").persist();
    clusters.getHost("h2").persist();

    requests.add(new HostRequest("h2", "foo", new HashMap<String, String>()));

    try {
      HostResourceProviderTest.createHosts(controller, requests);
      fail("Create host should fail for invalid clusters");
    } catch (Exception e) {
      // Expected
    }

    clusters.addCluster("foo", new StackId("HDP-0.1"));
    Cluster c = clusters.getCluster("foo");
    StackId stackId = new StackId("HDP-0.1");
    c.setDesiredStackVersion(stackId);
    c.setCurrentStackVersion(stackId);
    helper.getOrCreateRepositoryVersion(stackId, stackId.getStackVersion());
    c.createClusterVersion(stackId, stackId.getStackVersion(), "admin",
        RepositoryVersionState.UPGRADING);

    HostResourceProviderTest.createHosts(controller, requests);

    Assert.assertNotNull(clusters.getHost("h1"));
    Assert.assertNotNull(clusters.getHost("h2"));

    Assert.assertEquals(0, clusters.getClustersForHost("h1").size());
    Assert.assertEquals(1, clusters.getClustersForHost("h2").size());

  }

  @Test
  public void testCreateHostMultiple() throws AmbariException {
    clusters.addHost("h1");
    clusters.addHost("h2");
    clusters.addHost("h3");
    clusters.addCluster("c1", new StackId("HDP-0.1"));
    Cluster c = clusters.getCluster("c1");
    StackId stackID = new StackId("HDP-0.1");
    c.setDesiredStackVersion(stackID);
    c.setCurrentStackVersion(stackID);
    helper.getOrCreateRepositoryVersion(stackID, stackID.getStackVersion());
    c.createClusterVersion(stackID, stackID.getStackVersion(), "admin",
        RepositoryVersionState.UPGRADING);

    setOsFamily(clusters.getHost("h1"), "redhat", "5.9");
    setOsFamily(clusters.getHost("h2"), "redhat", "5.9");
    setOsFamily(clusters.getHost("h3"), "redhat", "5.9");
    clusters.getHost("h1").persist();
    clusters.getHost("h2").persist();
    clusters.getHost("h3").persist();

   String clusterName = "c1";

    HostRequest r1 = new HostRequest("h1", clusterName, null);
    HostRequest r2 = new HostRequest("h2", clusterName, null);
    HostRequest r3 = new HostRequest("h3", null, null);

    Set<HostRequest> set1 = new HashSet<HostRequest>();
    set1.add(r1);
    set1.add(r2);
    set1.add(r3);
    HostResourceProviderTest.createHosts(controller, set1);

    Assert.assertEquals(1, clusters.getClustersForHost("h1").size());
    Assert.assertEquals(1, clusters.getClustersForHost("h2").size());
    Assert.assertEquals(0, clusters.getClustersForHost("h3").size());
  }

  @Test
  public void testCreateHostWithInvalidRequests() throws AmbariException {
    // unknown host
    // invalid clusters
    // duplicate host

    Set<HostRequest> set1 = new HashSet<HostRequest>();

    try {
      set1.clear();
      HostRequest rInvalid =
          new HostRequest("h1", null, null);
      set1.add(rInvalid);
      HostResourceProviderTest.createHosts(controller, set1);
      fail("Expected failure for invalid host");
    } catch (Exception e) {
      // Expected
    }

    clusters.addHost("h1");

    String clusterName = "c1";

    try {
      set1.clear();
      HostRequest rInvalid =
          new HostRequest("h1", clusterName, null);
      set1.add(rInvalid);
      HostResourceProviderTest.createHosts(controller, set1);
      fail("Expected failure for invalid cluster");
    } catch (Exception e) {
      // Expected
    }

    clusters.addCluster("c1", new StackId("HDP-0.1"));

    try {
      set1.clear();
      HostRequest rInvalid1 =
          new HostRequest("h1", clusterName, null);
      HostRequest rInvalid2 =
          new HostRequest("h1", clusterName, null);
      set1.add(rInvalid1);
      set1.add(rInvalid2);
      HostResourceProviderTest.createHosts(controller, set1);
      fail("Expected failure for dup requests");
    } catch (Exception e) {
      // Expected
    }

  }

  @Test
  /**
   * Create a cluster with a service, and verify that the request tasks have the correct output log and error log paths.
   */
  public void testRequestStatusLogs() throws Exception {
    testCreateServiceComponentHostSimple();

    String clusterName = "foo1";
    String serviceName = "HDFS";

    Cluster cluster = clusters.getCluster(clusterName);
    for (Host h : clusters.getHosts()) {
      // Simulate each agent registering and setting the prefix path on its host
      h.setPrefix(Configuration.PREFIX_DIR);
    }

    Map<String, Config> configs = new HashMap<String, Config>();
    Map<String, String> properties = new HashMap<String, String>();
    Map<String, Map<String, String>> propertiesAttributes = new HashMap<String, Map<String,String>>();

    Config c1 = new ConfigImpl(cluster, "hdfs-site", properties, propertiesAttributes, injector);
    c1.setTag("v1");
    cluster.addConfig(c1);
    c1.persist();
    configs.put(c1.getType(), c1);

    ServiceRequest r = new ServiceRequest(clusterName, serviceName, State.INSTALLED.toString());
    Set<ServiceRequest> requests = new HashSet<ServiceRequest>();
    requests.add(r);

    Map<String, String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "Called from a test");

    RequestStatusResponse trackAction =
        ServiceResourceProviderTest.updateServices(controller, requests, mapRequestProps, true, false);

    List<ShortTaskStatus> taskStatuses = trackAction.getTasks();
    Assert.assertFalse(taskStatuses.isEmpty());
    for (ShortTaskStatus task : taskStatuses) {
      Assert.assertEquals("Task output logs don't match", Configuration.PREFIX_DIR + "/output-" + task.getTaskId() + ".txt", task.getOutputLog());
      Assert.assertEquals("Task error logs don't match", Configuration.PREFIX_DIR + "/errors-" + task.getTaskId() + ".txt", task.getErrorLog());
    }
  }

  @Test
  public void testInstallAndStartService() throws Exception {
    testCreateServiceComponentHostSimple();

    String clusterName = "foo1";
    String serviceName = "HDFS";

    Cluster cluster = clusters.getCluster(clusterName);
    Service s1 = cluster.getService(serviceName);

    Map<String, Config> configs = new HashMap<String, Config>();
    Map<String, String> properties = new HashMap<String, String>();
    Map<String, Map<String, String>> propertiesAttributes = new HashMap<String, Map<String,String>>();
    properties.put("a", "a1");
    properties.put("b", "b1");

    Config c1 = new ConfigImpl(cluster, "hdfs-site", properties, propertiesAttributes, injector);
    properties.put("c", "c1");
    properties.put("d", "d1");
    Config c2 = new ConfigImpl(cluster, "core-site", properties, propertiesAttributes, injector);
    Config c3 = new ConfigImpl(cluster, "foo-site", properties, propertiesAttributes, injector);

    Map<String, String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "Called from a test");

    c1.setTag("v1");
    c2.setTag("v1");
    c3.setTag("v1");

    cluster.addConfig(c1);
    cluster.addConfig(c2);
    cluster.addConfig(c3);
    c1.persist();
    c2.persist();
    c3.persist();

    configs.put(c1.getType(), c1);
    configs.put(c2.getType(), c2);

    ServiceRequest r = new ServiceRequest(clusterName, serviceName, State.INSTALLED.toString());
    Set<ServiceRequest> requests = new HashSet<ServiceRequest>();
    requests.add(r);

    RequestStatusResponse trackAction =
        ServiceResourceProviderTest.updateServices(controller, requests, mapRequestProps, true, false);
    Assert.assertEquals(State.INSTALLED,
        clusters.getCluster(clusterName).getService(serviceName)
        .getDesiredState());
    for (ServiceComponent sc :
      clusters.getCluster(clusterName).getService(serviceName)
      .getServiceComponents().values()) {
      Assert.assertEquals(State.INSTALLED, sc.getDesiredState());
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        Assert.assertEquals(State.INSTALLED, sch.getDesiredState());
        Assert.assertEquals(State.INIT, sch.getState());
      }
    }

    List<ShortTaskStatus> taskStatuses = trackAction.getTasks();
    Assert.assertEquals(5, taskStatuses.size());

    boolean foundH1NN = false;
    boolean foundH1DN = false;
    boolean foundH2DN = false;
    boolean foundH1CLT = false;
    boolean foundH2CLT = false;

    for (ShortTaskStatus taskStatus : taskStatuses) {
      LOG.debug("Task dump :"
          + taskStatus.toString());
      Assert.assertEquals(RoleCommand.INSTALL.toString(),
          taskStatus.getCommand());
      Assert.assertEquals(HostRoleStatus.PENDING.toString(),
          taskStatus.getStatus());
      if (taskStatus.getHostName().equals("h1")) {
        if (Role.NAMENODE.toString().equals(taskStatus.getRole())) {
          foundH1NN = true;
        } else if (Role.DATANODE.toString().equals(taskStatus.getRole())) {
          foundH1DN = true;
        } else if (Role.HDFS_CLIENT.toString().equals(taskStatus.getRole())) {
          foundH1CLT = true;
        } else {
          fail("Found invalid role for host h1");
        }
      } else if (taskStatus.getHostName().equals("h2")) {
        if (Role.DATANODE.toString().equals(taskStatus.getRole())) {
          foundH2DN = true;
        } else if (Role.HDFS_CLIENT.toString().equals(taskStatus.getRole())) {
          foundH2CLT = true;
        } else {
          fail("Found invalid role for host h2");
        }
      } else {
        fail("Found invalid host in task list");
      }
    }
    Assert.assertTrue(foundH1DN && foundH1NN && foundH2DN
        && foundH1CLT && foundH2CLT);

    // TODO validate stages?
    List<Stage> stages = actionDB.getAllStages(trackAction.getRequestId());
    Assert.assertEquals(1, stages.size());

    for (Stage stage : stages) {
      LOG.info("Stage Details for Install Service"
          + ", stageId="+ stage.getStageId()
          + ", actionId=" + stage.getActionId());

      for (String host : stage.getHosts()) {
        for (ExecutionCommandWrapper ecw : stage.getExecutionCommands(host)) {
          Assert.assertFalse(
              ecw.getExecutionCommand().getHostLevelParams().get("repo_info").isEmpty());
        }
      }
    }

    org.apache.ambari.server.controller.spi.Request request = PropertyHelper.getReadRequest(
        TaskResourceProvider.TASK_CLUSTER_NAME_PROPERTY_ID,
        TaskResourceProvider.TASK_REQUEST_ID_PROPERTY_ID,
        TaskResourceProvider.TASK_STAGE_ID_PROPERTY_ID);

    Predicate predicate = new PredicateBuilder().property(
        TaskResourceProvider.TASK_REQUEST_ID_PROPERTY_ID).equals(
            trackAction.getRequestId()).toPredicate();

    List<HostRoleCommandEntity> entities = hostRoleCommandDAO.findAll(request, predicate);
    Assert.assertEquals(5, entities.size());

    predicate = new PredicateBuilder().property(
        TaskResourceProvider.TASK_REQUEST_ID_PROPERTY_ID).equals(
            trackAction.getRequestId()).and().property(
                TaskResourceProvider.TASK_ID_PROPERTY_ID).equals(1L).toPredicate();

    entities = hostRoleCommandDAO.findAll(request, predicate);
    Assert.assertEquals(1, entities.size());

    // manually change live state to installed as no running action manager
    for (ServiceComponent sc :
      clusters.getCluster(clusterName).getService(serviceName)
      .getServiceComponents().values()) {
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        sch.setState(State.INSTALLED);
      }
    }

    r = new ServiceRequest(clusterName, serviceName, State.STARTED.toString());
    requests.clear();
    requests.add(r);
    trackAction = ServiceResourceProviderTest.updateServices(controller, requests, mapRequestProps, true,
      false);

    Assert.assertEquals(State.STARTED,
        clusters.getCluster(clusterName).getService(serviceName)
        .getDesiredState());
    for (ServiceComponent sc :
      clusters.getCluster(clusterName).getService(serviceName)
      .getServiceComponents().values()) {
      if (sc.getName().equals("HDFS_CLIENT")) {
        Assert.assertEquals(State.INSTALLED, sc.getDesiredState());
      } else {
        Assert.assertEquals(State.STARTED, sc.getDesiredState());
      }
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        if (sch.getServiceComponentName().equals("HDFS_CLIENT")) {
          Assert.assertEquals(State.INSTALLED, sch.getDesiredState());
        } else {
          Assert.assertEquals(State.STARTED, sch.getDesiredState());
        }
      }
    }

    // TODO validate stages?
    stages = actionDB.getAllStages(trackAction.getRequestId());
    Assert.assertEquals(2, stages.size());

    StringBuilder sb = new StringBuilder();
    clusters.debugDump(sb);
    LOG.info("Cluster Dump: " + sb.toString());

    for (ServiceComponent sc :
      clusters.getCluster(clusterName).getService(serviceName)
      .getServiceComponents().values()) {
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        if (sc.isClientComponent()) {
          sch.setState(State.INSTALLED);
        } else {
          sch.setState(State.INSTALL_FAILED);
        }
      }
    }

    r = new ServiceRequest(clusterName, serviceName, State.INSTALLED.toString());
    requests.clear();
    requests.add(r);
    trackAction = ServiceResourceProviderTest.updateServices(controller, requests, mapRequestProps, true,
      false);

    Assert.assertEquals(State.INSTALLED,
        clusters.getCluster(clusterName).getService(serviceName)
        .getDesiredState());
    for (ServiceComponent sc :
      clusters.getCluster(clusterName).getService(serviceName)
      .getServiceComponents().values()) {
      Assert.assertEquals(State.INSTALLED, sc.getDesiredState());
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        Assert.assertEquals(State.INSTALLED, sch.getDesiredState());
      }
    }

    // TODO validate stages?
    stages = actionDB.getAllStages(trackAction.getRequestId());

    Assert.assertEquals(1, stages.size());

  }

  @Test
  public void testGetClusters() throws AmbariException {
    clusters.addCluster("c1", new StackId("HDP-0.1"));

    Cluster c1 = clusters.getCluster("c1");

    StackId stackId = new StackId("HDP-0.1");
    c1.setDesiredStackVersion(stackId);
    helper.getOrCreateRepositoryVersion(stackId, stackId.getStackVersion());
    c1.createClusterVersion(stackId, stackId.getStackVersion(), "admin",
        RepositoryVersionState.UPGRADING);

    ClusterRequest r = new ClusterRequest(null, null, null, null);
    Set<ClusterResponse> resp = controller.getClusters(Collections.singleton(r));
    Assert.assertEquals(1, resp.size());

    ClusterResponse resp1 = resp.iterator().next();

    Assert.assertEquals(c1.getClusterId(), resp1.getClusterId().longValue());
    Assert.assertEquals(c1.getClusterName(), resp1.getClusterName());
    Assert.assertEquals(c1.getDesiredStackVersion().getStackId(),
        resp1.getDesiredStackVersion());
  }

  @Test
  public void testGetClustersWithFilters() throws AmbariException {
    clusters.addCluster("c1", new StackId("HDP-0.1"));
    clusters.addCluster("c2", new StackId("HDP-0.1"));
    clusters.addCluster("c3", new StackId("HDP-1.2.0"));
    clusters.addCluster("c4", new StackId("HDP-0.1"));

    Cluster c1 = clusters.getCluster("c1");
    Cluster c2 = clusters.getCluster("c2");
    Cluster c3 = clusters.getCluster("c3");
    Cluster c4 = clusters.getCluster("c4");

    ClusterRequest r = new ClusterRequest(null, null, null, null);
    Set<ClusterResponse> resp = controller.getClusters(Collections.singleton(r));
    Assert.assertEquals(4, resp.size());

    r = new ClusterRequest(null, "c1", null, null);
    resp = controller.getClusters(Collections.singleton(r));
    Assert.assertEquals(1, resp.size());
    Assert.assertEquals(c1.getClusterId(),
        resp.iterator().next().getClusterId().longValue());

    r = new ClusterRequest(null, null, "HDP-0.1", null);
    resp = controller.getClusters(Collections.singleton(r));
    Assert.assertEquals(3, resp.size());

    r = new ClusterRequest(null, null, "", null);
    resp = controller.getClusters(Collections.singleton(r));
    Assert.assertEquals(0, resp.size());
  }

  @Test
  public void testGetServices() throws AmbariException {
    clusters.addCluster("c1", new StackId("HDP-0.1"));
    Cluster c1 = clusters.getCluster("c1");
    Service s1 = serviceFactory.createNew(c1, "HDFS");

    c1.addService(s1);
    s1.setDesiredStackVersion(new StackId("HDP-0.1"));
    s1.setDesiredState(State.INSTALLED);

    s1.persist();

    ServiceRequest r = new ServiceRequest("c1", null, null);
    Set<ServiceResponse> resp = ServiceResourceProviderTest.getServices(controller, Collections.singleton(r));

    ServiceResponse resp1 = resp.iterator().next();

    Assert.assertEquals(s1.getClusterId(), resp1.getClusterId().longValue());
    Assert.assertEquals(s1.getCluster().getClusterName(),
        resp1.getClusterName());
    Assert.assertEquals(s1.getName(), resp1.getServiceName());
    Assert.assertEquals("HDP-0.1", s1.getDesiredStackVersion().getStackId());
    Assert.assertEquals(s1.getDesiredStackVersion().getStackId(),
        resp1.getDesiredStackVersion());
    Assert.assertEquals(State.INSTALLED.toString(), resp1.getDesiredState());

  }

  @Test
  public void testGetServicesWithFilters() throws AmbariException {
    clusters.addCluster("c1", new StackId("HDP-0.2"));
    clusters.addCluster("c2", new StackId("HDP-0.2"));
    Cluster c1 = clusters.getCluster("c1");
    Cluster c2 = clusters.getCluster("c2");
    c1.setDesiredStackVersion(new StackId("HDP-0.2"));
    c2.setDesiredStackVersion(new StackId("HDP-0.2"));

    Service s1 = serviceFactory.createNew(c1, "HDFS");
    Service s2 = serviceFactory.createNew(c1, "MAPREDUCE");
    Service s3 = serviceFactory.createNew(c1, "HBASE");
    Service s4 = serviceFactory.createNew(c2, "HIVE");
    Service s5 = serviceFactory.createNew(c2, "ZOOKEEPER");

    c1.addService(s1);
    c1.addService(s2);
    c1.addService(s3);
    c2.addService(s4);
    c2.addService(s5);

    s1.setDesiredState(State.INSTALLED);
    s2.setDesiredState(State.INSTALLED);
    s4.setDesiredState(State.INSTALLED);

    s1.persist();
    s2.persist();
    s3.persist();
    s4.persist();
    s5.persist();

    ServiceRequest r = new ServiceRequest(null, null, null);
    Set<ServiceResponse> resp;

    try {
      ServiceResourceProviderTest.getServices(controller, Collections.singleton(r));
      fail("Expected failure for invalid request");
    } catch (Exception e) {
      // Expected
    }

    r = new ServiceRequest(c1.getClusterName(), null, null);
    resp = ServiceResourceProviderTest.getServices(controller, Collections.singleton(r));
    Assert.assertEquals(3, resp.size());

    r = new ServiceRequest(c1.getClusterName(), s2.getName(), null);
    resp = ServiceResourceProviderTest.getServices(controller, Collections.singleton(r));
    Assert.assertEquals(1, resp.size());
    Assert.assertEquals(s2.getName(), resp.iterator().next().getServiceName());

    try {
      r = new ServiceRequest(c2.getClusterName(), s1.getName(), null);
      ServiceResourceProviderTest.getServices(controller, Collections.singleton(r));
      fail("Expected failure for invalid service");
    } catch (Exception e) {
      // Expected
    }

    r = new ServiceRequest(c1.getClusterName(), null, "INSTALLED");
    resp = ServiceResourceProviderTest.getServices(controller, Collections.singleton(r));
    Assert.assertEquals(2, resp.size());

    r = new ServiceRequest(c2.getClusterName(), null, "INIT");
    resp = ServiceResourceProviderTest.getServices(controller, Collections.singleton(r));
    Assert.assertEquals(1, resp.size());

    ServiceRequest r1, r2, r3;
    r1 = new ServiceRequest(c1.getClusterName(), null, "INSTALLED");
    r2 = new ServiceRequest(c2.getClusterName(), null, "INIT");
    r3 = new ServiceRequest(c2.getClusterName(), null, "INIT");

    Set<ServiceRequest> reqs = new HashSet<ServiceRequest>();
    reqs.addAll(Arrays.asList(r1, r2, r3));
    resp = ServiceResourceProviderTest.getServices(controller, reqs);
    Assert.assertEquals(3, resp.size());

  }


  @Test
  public void testGetServiceComponents() throws AmbariException {
    clusters.addCluster("c1", new StackId("HDP-0.2"));
    Cluster c1 = clusters.getCluster("c1");
    c1.setDesiredStackVersion(new StackId("HDP-0.2"));
    Service s1 = serviceFactory.createNew(c1, "HDFS");
    c1.addService(s1);
    s1.setDesiredState(State.INSTALLED);
    s1.persist();
    ServiceComponent sc1 = serviceComponentFactory.createNew(s1, "DATANODE");
    s1.addServiceComponent(sc1);
    sc1.persist();
    sc1.setDesiredStackVersion(new StackId("HDP-0.1"));
    sc1.setDesiredState(State.UNINSTALLED);

    ServiceComponentRequest r = new ServiceComponentRequest("c1",
       s1.getName(), sc1.getName(), null);

    Set<ServiceComponentResponse> resps = ComponentResourceProviderTest.getComponents(controller, Collections.singleton(r));
    Assert.assertEquals(1, resps.size());

    ServiceComponentResponse resp = resps.iterator().next();

    Assert.assertEquals(c1.getClusterName(), resp.getClusterName());
    Assert.assertEquals(sc1.getName(), resp.getComponentName());
    Assert.assertEquals(s1.getName(), resp.getServiceName());
    Assert.assertEquals("HDP-0.1", resp.getDesiredStackVersion());
    Assert.assertEquals(sc1.getDesiredState().toString(),
        resp.getDesiredState());
    Assert.assertEquals(c1.getClusterId(), resp.getClusterId().longValue());

  }


  @Test
  public void testGetServiceComponentsWithFilters() throws AmbariException {
    clusters.addCluster("c1", new StackId("HDP-0.2"));
    clusters.addCluster("c2", new StackId("HDP-0.2"));
    Cluster c1 = clusters.getCluster("c1");
    Cluster c2 = clusters.getCluster("c2");

    Service s1 = serviceFactory.createNew(c1, "HDFS");
    Service s2 = serviceFactory.createNew(c1, "MAPREDUCE");
    Service s3 = serviceFactory.createNew(c1, "HBASE");
    Service s4 = serviceFactory.createNew(c2, "HIVE");
    Service s5 = serviceFactory.createNew(c2, "ZOOKEEPER");

    c1.addService(s1);
    c1.addService(s2);
    c1.addService(s3);
    c2.addService(s4);
    c2.addService(s5);

    s1.setDesiredState(State.INSTALLED);
    s2.setDesiredState(State.INSTALLED);
    s4.setDesiredState(State.INSTALLED);

    s1.persist();
    s2.persist();
    s3.persist();
    s4.persist();
    s5.persist();

    ServiceComponent sc1 = serviceComponentFactory.createNew(s1, "DATANODE");
    ServiceComponent sc2 = serviceComponentFactory.createNew(s1, "NAMENODE");
    ServiceComponent sc3 = serviceComponentFactory.createNew(s3,
        "HBASE_REGIONSERVER");
    ServiceComponent sc4 = serviceComponentFactory.createNew(s4, "HIVE_SERVER");
    ServiceComponent sc5 = serviceComponentFactory.createNew(s4, "HIVE_CLIENT");
    ServiceComponent sc6 = serviceComponentFactory.createNew(s4,
        "MYSQL_SERVER");
    ServiceComponent sc7 = serviceComponentFactory.createNew(s5,
        "ZOOKEEPER_SERVER");
    ServiceComponent sc8 = serviceComponentFactory.createNew(s5,
        "ZOOKEEPER_CLIENT");

    s1.addServiceComponent(sc1);
    s1.addServiceComponent(sc2);
    s3.addServiceComponent(sc3);
    s4.addServiceComponent(sc4);
    s4.addServiceComponent(sc5);
    s4.addServiceComponent(sc6);
    s5.addServiceComponent(sc7);
    s5.addServiceComponent(sc8);

    sc1.setDesiredState(State.UNINSTALLED);
    sc3.setDesiredState(State.UNINSTALLED);
    sc5.setDesiredState(State.UNINSTALLED);
    sc6.setDesiredState(State.UNINSTALLED);
    sc7.setDesiredState(State.UNINSTALLED);
    sc8.setDesiredState(State.UNINSTALLED);

    sc1.persist();
    sc2.persist();
    sc3.persist();
    sc4.persist();
    sc5.persist();
    sc6.persist();
    sc7.persist();
    sc8.persist();

    ServiceComponentRequest r = new ServiceComponentRequest(null, null,
        null, null);

    try {
      ComponentResourceProviderTest.getComponents(controller, Collections.singleton(r));
      fail("Expected failure for invalid cluster");
    } catch (Exception e) {
      // Expected
    }

    // all comps per cluster
    r = new ServiceComponentRequest(c1.getClusterName(),
        null, null, null);
    Set<ServiceComponentResponse> resps = ComponentResourceProviderTest.getComponents(controller, Collections.singleton(r));
    Assert.assertEquals(3, resps.size());

    // all comps per cluster filter on state
    r = new ServiceComponentRequest(c2.getClusterName(),
        null, null, State.UNINSTALLED.toString());
    resps = ComponentResourceProviderTest.getComponents(controller, Collections.singleton(r));
    Assert.assertEquals(4, resps.size());

    // all comps for given service
    r = new ServiceComponentRequest(c2.getClusterName(),
        s5.getName(), null, null);
    resps = ComponentResourceProviderTest.getComponents(controller, Collections.singleton(r));
    Assert.assertEquals(2, resps.size());

    // all comps for given service filter by state
    r = new ServiceComponentRequest(c2.getClusterName(),
        s4.getName(), null, State.INIT.toString());
    resps = ComponentResourceProviderTest.getComponents(controller, Collections.singleton(r));
    Assert.assertEquals(1, resps.size());
    Assert.assertEquals(sc4.getName(),
        resps.iterator().next().getComponentName());

    // get single given comp
    r = new ServiceComponentRequest(c2.getClusterName(),
        null, sc5.getName(), State.INIT.toString());
    resps = ComponentResourceProviderTest.getComponents(controller, Collections.singleton(r));
    Assert.assertEquals(1, resps.size());
    Assert.assertEquals(sc5.getName(),
        resps.iterator().next().getComponentName());

    // get single given comp and given svc
    r = new ServiceComponentRequest(c2.getClusterName(),
        s4.getName(), sc5.getName(), State.INIT.toString());
    resps = ComponentResourceProviderTest.getComponents(controller, Collections.singleton(r));
    Assert.assertEquals(1, resps.size());
    Assert.assertEquals(sc5.getName(),
        resps.iterator().next().getComponentName());


    ServiceComponentRequest r1, r2, r3;
    Set<ServiceComponentRequest> reqs = new HashSet<ServiceComponentRequest>();
    r1 = new ServiceComponentRequest(c2.getClusterName(),
        null, null, State.UNINSTALLED.toString());
    r2 = new ServiceComponentRequest(c1.getClusterName(),
        null, null, null);
    r3 = new ServiceComponentRequest(c1.getClusterName(),
        null, null, State.INIT.toString());
    reqs.addAll(Arrays.asList(r1, r2, r3));
    resps = ComponentResourceProviderTest.getComponents(controller, reqs);
    Assert.assertEquals(7, resps.size());
  }

  @Test
  public void testGetServiceComponentHosts() throws AmbariException {
    Cluster c1 = setupClusterWithHosts("c1", "HDP-0.1", new ArrayList<String>() {{
      add("h1");
    }}, "centos5");
    Service s1 = serviceFactory.createNew(c1, "HDFS");
    c1.addService(s1);
    s1.persist();
    ServiceComponent sc1 = serviceComponentFactory.createNew(s1, "DATANODE");
    s1.addServiceComponent(sc1);
    sc1.setDesiredState(State.UNINSTALLED);
    sc1.persist();
    ServiceComponentHost sch1 = serviceComponentHostFactory.createNew(sc1, "h1");
    sc1.addServiceComponentHost(sch1);
    sch1.setDesiredState(State.INSTALLED);
    sch1.setState(State.INSTALLING);
    sch1.setDesiredStackVersion(new StackId("HDP-1.2.0"));
    sch1.setStackVersion(new StackId("HDP-0.1"));

    sch1.persist();

    sch1.updateActualConfigs(new HashMap<String, Map<String,String>>() {{
      put("global", new HashMap<String,String>() {{ put("tag", "version1"); }});
    }});


    ServiceComponentHostRequest r =
        new ServiceComponentHostRequest(c1.getClusterName(),
            null, null, null, null);
    Set<ServiceComponentHostResponse> resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(1, resps.size());

    ServiceComponentHostResponse resp =
        resps.iterator().next();

    Assert.assertEquals(c1.getClusterName(), resp.getClusterName());
    Assert.assertEquals(sc1.getName(), resp.getComponentName());
    Assert.assertEquals(s1.getName(), resp.getServiceName());
    Assert.assertEquals(sch1.getHostName(), resp.getHostname());
    Assert.assertEquals(sch1.getDesiredState().toString(),
        resp.getDesiredState());
    Assert.assertEquals(sch1.getState().toString(),
        resp.getLiveState());
    Assert.assertEquals(sch1.getStackVersion().getStackId(),
        resp.getStackVersion());
    Assert.assertNotNull(resp.getActualConfigs());
    Assert.assertEquals(1, resp.getActualConfigs().size());
  }

  @Test
  public void testGetServiceComponentHostsWithStaleConfigFilter() throws AmbariException {

    final String host1 = "h1";
    final String host2 = "h2";
    Long clusterId = 1L;
    String clusterName = "foo1";
    setupClusterWithHosts(clusterName, "HDP-2.0.5",
        new ArrayList<String>() {{
          add(host1);
          add(host2);
        }},
        "centos5");
    String serviceName = "HDFS";
    createService(clusterName, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";

    createServiceComponent(clusterName, serviceName, componentName1,
        State.INIT);
    createServiceComponent(clusterName, serviceName, componentName2,
        State.INIT);
    createServiceComponent(clusterName, serviceName, componentName3,
        State.INIT);

    createServiceComponentHost(clusterName, serviceName, componentName1,
        host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName2,
        host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName3,
        host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName2,
        host2, null);
    createServiceComponentHost(clusterName, serviceName, componentName3,
        host2, null);

    // Install
    installService(clusterName, serviceName, false, false);

    // Create and attach config
    Map<String, String> configs = new HashMap<String, String>();
    configs.put("a", "b");

    ConfigurationRequest cr1;
    cr1 = new ConfigurationRequest(clusterName, "hdfs-site", "version1",
        configs, null);
    ClusterRequest crReq = new ClusterRequest(clusterId, clusterName, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr1));
    controller.updateClusters(Collections.singleton(crReq), null);

    // Start
    startService(clusterName, serviceName, false, false);

    //Update actual config
    HashMap<String, Map<String, String>> actualConfig = new HashMap<String, Map<String, String>>() {{
      put("hdfs-site", new HashMap<String, String>() {{
        put("tag", "version1");
      }});
    }};
    HashMap<String, Map<String, String>> actualConfigOld = new
        HashMap<String, Map<String, String>>() {{
          put("hdfs-site", new HashMap<String, String>() {{
            put("tag", "version0");
          }});
        }};

    Service s1 = clusters.getCluster(clusterName).getService(serviceName);
    s1.getServiceComponent(componentName1).getServiceComponentHost(host1).updateActualConfigs(actualConfig);
    s1.getServiceComponent(componentName2).getServiceComponentHost(host1).updateActualConfigs(actualConfig);
    s1.getServiceComponent(componentName3).getServiceComponentHost(host1).updateActualConfigs(actualConfigOld);
    s1.getServiceComponent(componentName2).getServiceComponentHost(host2).updateActualConfigs(actualConfigOld);
    s1.getServiceComponent(componentName3).getServiceComponentHost(host2).updateActualConfigs(actualConfig);

    ServiceComponentHostRequest r =
        new ServiceComponentHostRequest(clusterName, null, null, null, null);
    Set<ServiceComponentHostResponse> resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(5, resps.size());

    //Get all host components with stale config = true
    r = new ServiceComponentHostRequest(clusterName, null, null, null, null);
    r.setStaleConfig("true");
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(2, resps.size());

    //Get all host components with stale config = false
    r = new ServiceComponentHostRequest(clusterName, null, null, null, null);
    r.setStaleConfig("false");
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(3, resps.size());

    //Get all host components with stale config = false and hostname filter
    r = new ServiceComponentHostRequest(clusterName, null, null, host1, null);
    r.setStaleConfig("false");
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(2, resps.size());

    //Get all host components with stale config = false and hostname filter
    r = new ServiceComponentHostRequest(clusterName, null, null, host2, null);
    r.setStaleConfig("true");
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(1, resps.size());
  }

  @Test
  public void testServiceComponentHostsWithDecommissioned() throws Exception {

    final String host1 = "h1";
    final String host2 = "h2";
    String clusterName = "foo1";
    setupClusterWithHosts(clusterName, "HDP-2.0.7",
        new ArrayList<String>() {{
          add(host1);
          add(host2);
        }},
        "centos5");
    String serviceName = "HDFS";
    createService(clusterName, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";

    createServiceComponent(clusterName, serviceName, componentName1,
        State.INIT);
    createServiceComponent(clusterName, serviceName, componentName2,
        State.INIT);
    createServiceComponent(clusterName, serviceName, componentName3,
        State.INIT);

    createServiceComponentHost(clusterName, serviceName, componentName1,
        host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName2,
        host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName3,
        host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName2,
        host2, null);
    createServiceComponentHost(clusterName, serviceName, componentName3,
        host2, null);

    // Install
    installService(clusterName, serviceName, false, false);

    // Start
    startService(clusterName, serviceName, false, false);

    Service s1 = clusters.getCluster(clusterName).getService(serviceName);
    s1.getServiceComponent(componentName2).getServiceComponentHost(host1).
        setComponentAdminState(HostComponentAdminState.DECOMMISSIONED);
    s1.getServiceComponent(componentName2).getServiceComponentHost(host2).
        setComponentAdminState(HostComponentAdminState.INSERVICE);

    ServiceComponentHostRequest r =
        new ServiceComponentHostRequest(clusterName, null, null, null, null);
    Set<ServiceComponentHostResponse> resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(5, resps.size());

    //Get all host components with decommissiond = true
    r = new ServiceComponentHostRequest(clusterName, null, null, null, null);
    r.setAdminState("DECOMMISSIONED");
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(1, resps.size());

    //Get all host components with decommissioned = false
    r = new ServiceComponentHostRequest(clusterName, null, null, null, null);
    r.setAdminState("INSERVICE");
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(1, resps.size());

    //Get all host components with decommissioned = some random string
    r = new ServiceComponentHostRequest(clusterName, null, null, null, null);
    r.setAdminState("INSTALLED");
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(0, resps.size());

    //Update adminState
    r = new ServiceComponentHostRequest(clusterName, "HDFS", "DATANODE", host2, null);
    r.setAdminState("DECOMMISSIONED");
    try {
      updateHostComponents(Collections.singleton(r), new HashMap<String, String>(), false);
      Assert.fail("Must throw exception when decommission attribute is updated.");
    } catch (IllegalArgumentException ex) {
      Assert.assertTrue(ex.getMessage().contains("Property adminState cannot be modified through update"));
    }
  }

  @Test
  public void testHbaseDecommission() throws AmbariException {
    String clusterName = "foo1";
    createCluster(clusterName);
    clusters.getCluster(clusterName)
        .setDesiredStackVersion(new StackId("HDP-2.0.7"));
    String serviceName = "HBASE";
    createService(clusterName, serviceName, null);
    String componentName1 = "HBASE_MASTER";
    String componentName2 = "HBASE_REGIONSERVER";

    createServiceComponent(clusterName, serviceName, componentName1,
        State.INIT);
    createServiceComponent(clusterName, serviceName, componentName2,
        State.INIT);

    String host1 = "h1";
    String host2 = "h2";

    addHostToCluster(host1, clusterName);
    addHostToCluster(host2, clusterName);

    createServiceComponentHost(clusterName, serviceName, componentName1,
        host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName1,
        host2, null);
    createServiceComponentHost(clusterName, serviceName, componentName2,
        host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName2,
        host2, null);

    RequestOperationLevel level = new RequestOperationLevel(
            Resource.Type.HostComponent, clusterName, null, null, null);

    // Install
    installService(clusterName, serviceName, false, false);

    // Start
    startService(clusterName, serviceName, false, false);

    Cluster cluster = clusters.getCluster(clusterName);
    Service s = cluster.getService(serviceName);
    Assert.assertEquals(State.STARTED, s.getDesiredState());
    ServiceComponentHost scHost = s.getServiceComponent("HBASE_REGIONSERVER").getServiceComponentHost("h2");
    Assert.assertEquals(HostComponentAdminState.INSERVICE, scHost.getComponentAdminState());

    // Decommission one RS
    Map<String, String> params = new HashMap<String, String>() {{
      put("excluded_hosts", "h2");
      put("align_maintenance_state", "true");
    }};
    RequestResourceFilter resourceFilter = new RequestResourceFilter("HBASE", "HBASE_MASTER", null);
    List<RequestResourceFilter> resourceFilters = new ArrayList<RequestResourceFilter>();
    resourceFilters.add(resourceFilter);

    ExecuteActionRequest request = new ExecuteActionRequest(clusterName,
      "DECOMMISSION", null, resourceFilters, level, params, false);

    Map<String, String> requestProperties = new HashMap<String, String>();
    requestProperties.put(REQUEST_CONTEXT_PROPERTY, "Called from a test");

    RequestStatusResponse response = controller.createAction(request,
        requestProperties);

    List<HostRoleCommand> storedTasks = actionDB.getRequestTasks(response.getRequestId());
    ExecutionCommand execCmd = storedTasks.get(0).getExecutionCommandWrapper
        ().getExecutionCommand();
    Assert.assertNotNull(storedTasks);
    Assert.assertEquals(1, storedTasks.size());
    Assert.assertEquals(HostComponentAdminState.DECOMMISSIONED, scHost.getComponentAdminState());
    Assert.assertEquals(MaintenanceState.ON, scHost.getMaintenanceState());
    HostRoleCommand command = storedTasks.get(0);
    Assert.assertTrue("DECOMMISSION, Excluded: h2".equals(command.getCommandDetail()));
    Assert.assertTrue("DECOMMISSION".equals(command.getCustomCommandName()));
    Map<String, String> cmdParams = command.getExecutionCommandWrapper().getExecutionCommand().getCommandParams();
    Assert.assertTrue(cmdParams.containsKey("mark_draining_only"));
    Assert.assertEquals("false", cmdParams.get("mark_draining_only"));
    Assert.assertEquals(Role.HBASE_MASTER, command.getRole());
    Assert.assertEquals(RoleCommand.CUSTOM_COMMAND, command.getRoleCommand());
    Assert.assertEquals("DECOMMISSION", execCmd.getHostLevelParams().get("custom_command"));
    assertEquals(requestProperties.get(REQUEST_CONTEXT_PROPERTY), response.getRequestContext());

    // RS stops
    s.getServiceComponent("HBASE_REGIONSERVER").getServiceComponentHost("h2").setState(State.INSTALLED);

    // Remove RS from draining
    params = new
        HashMap<String, String>() {{
          put("excluded_hosts", "h2");
          put("mark_draining_only", "true");
          put("slave_type", "HBASE_REGIONSERVER");
          put("align_maintenance_state", "true");
        }};
    resourceFilter = new RequestResourceFilter("HBASE", "HBASE_MASTER", null);
    ArrayList<RequestResourceFilter> filters = new ArrayList<RequestResourceFilter>();
    filters.add(resourceFilter);
    request = new ExecuteActionRequest(clusterName, "DECOMMISSION", null,
            filters, level, params, false);

    response = controller.createAction(request, requestProperties);

    storedTasks = actionDB.getRequestTasks(response.getRequestId());
    execCmd = storedTasks.get(0).getExecutionCommandWrapper
        ().getExecutionCommand();
    Assert.assertNotNull(storedTasks);
    Assert.assertEquals(1, storedTasks.size());
    Assert.assertEquals(HostComponentAdminState.DECOMMISSIONED, scHost.getComponentAdminState());
    Assert.assertEquals(MaintenanceState.ON, scHost.getMaintenanceState());
    command = storedTasks.get(0);
    Assert.assertEquals("DECOMMISSION", execCmd.getHostLevelParams().get("custom_command"));
    Assert.assertTrue("DECOMMISSION, Excluded: h2".equals(command.getCommandDetail()));
    Assert.assertTrue("DECOMMISSION".equals(command.getCustomCommandName()));
    cmdParams = command.getExecutionCommandWrapper().getExecutionCommand().getCommandParams();
    Assert.assertTrue(cmdParams.containsKey("mark_draining_only"));
    Assert.assertEquals("true", cmdParams.get("mark_draining_only"));
    assertEquals(requestProperties.get(REQUEST_CONTEXT_PROPERTY), response.getRequestContext());

    //Recommission
    params = new HashMap<String, String>() {{
      put("included_hosts", "h2");
    }};
    request = new ExecuteActionRequest(clusterName, "DECOMMISSION", null,
      resourceFilters, level, params, false);

    response = controller.createAction(request,
        requestProperties);

    storedTasks = actionDB.getRequestTasks(response.getRequestId());
    execCmd = storedTasks.get(0).getExecutionCommandWrapper
        ().getExecutionCommand();
    Assert.assertNotNull(storedTasks);
    Assert.assertEquals(1, storedTasks.size());
    Assert.assertEquals(HostComponentAdminState.INSERVICE, scHost.getComponentAdminState());
    Assert.assertEquals(MaintenanceState.ON, scHost.getMaintenanceState());
    command = storedTasks.get(0);
    Assert.assertTrue("DECOMMISSION, Included: h2".equals(command.getCommandDetail()));
    Assert.assertTrue("DECOMMISSION".equals(command.getCustomCommandName()));
    cmdParams = command.getExecutionCommandWrapper().getExecutionCommand().getCommandParams();
    Assert.assertTrue(cmdParams.containsKey("mark_draining_only"));
    Assert.assertEquals("false", cmdParams.get("mark_draining_only"));
    assertEquals(requestProperties.get(REQUEST_CONTEXT_PROPERTY), response.getRequestContext());

    Assert.assertTrue(cmdParams.containsKey("excluded_hosts"));
    Assert.assertEquals("", cmdParams.get("excluded_hosts"));
    Assert.assertEquals(Role.HBASE_MASTER, command.getRole());
    Assert.assertEquals(RoleCommand.CUSTOM_COMMAND, command.getRoleCommand());
    Assert.assertEquals("DECOMMISSION", execCmd.getHostLevelParams().get("custom_command"));
  }

  private Cluster setupClusterWithHosts(String clusterName, String stackId, List<String> hosts,
                                        String osType) throws AmbariException {
    ClusterRequest r = new ClusterRequest(null, clusterName, stackId, null);
    controller.createCluster(r);
    Cluster c1 = clusters.getCluster(clusterName);
    for (String host : hosts) {
      addHostToCluster(host, clusterName);
    }
    return c1;
  }

  @Test
  public void testGetServiceComponentHostsWithFilters() throws AmbariException {
    Cluster c1 = setupClusterWithHosts("c1", "HDP-0.2",
        new ArrayList<String>() {{
          add("h1");
          add("h2");
          add("h3");
        }},
        "centos5");

    Service s1 = serviceFactory.createNew(c1, "HDFS");
    Service s2 = serviceFactory.createNew(c1, "MAPREDUCE");
    Service s3 = serviceFactory.createNew(c1, "HBASE");

    c1.addService(s1);
    c1.addService(s2);
    c1.addService(s3);

    s1.setDesiredState(State.INSTALLED);
    s2.setDesiredState(State.INSTALLED);

    s1.persist();
    s2.persist();
    s3.persist();

    ServiceComponent sc1 = serviceComponentFactory.createNew(s1, "DATANODE");
    ServiceComponent sc2 = serviceComponentFactory.createNew(s1, "NAMENODE");
    ServiceComponent sc3 = serviceComponentFactory.createNew(s3,
        "HBASE_REGIONSERVER");

    s1.addServiceComponent(sc1);
    s1.addServiceComponent(sc2);
    s3.addServiceComponent(sc3);

    sc1.setDesiredState(State.UNINSTALLED);
    sc3.setDesiredState(State.UNINSTALLED);

    sc1.persist();
    sc2.persist();
    sc3.persist();

    ServiceComponentHost sch1 = serviceComponentHostFactory.createNew(sc1, "h1");
    ServiceComponentHost sch2 = serviceComponentHostFactory.createNew(sc1, "h2");
    ServiceComponentHost sch3 = serviceComponentHostFactory.createNew(sc1, "h3");
    ServiceComponentHost sch4 = serviceComponentHostFactory.createNew(sc2, "h1");
    ServiceComponentHost sch5 = serviceComponentHostFactory.createNew(sc2, "h2");
    ServiceComponentHost sch6 = serviceComponentHostFactory.createNew(sc3, "h3");

    sc1.addServiceComponentHost(sch1);
    sc1.addServiceComponentHost(sch2);
    sc1.addServiceComponentHost(sch3);
    sc2.addServiceComponentHost(sch4);
    sc2.addServiceComponentHost(sch5);
    sc3.addServiceComponentHost(sch6);

    sch1.setDesiredState(State.INSTALLED);
    sch2.setDesiredState(State.INIT);
    sch4.setDesiredState(State.INSTALLED);
    sch5.setDesiredState(State.UNINSTALLED);

    sch1.persist();
    sch2.persist();
    sch3.persist();
    sch4.persist();
    sch5.persist();
    sch6.persist();

    ServiceComponentHostRequest r =
        new ServiceComponentHostRequest(null, null, null, null, null);

    try {
      controller.getHostComponents(Collections.singleton(r));
      fail("Expected failure for invalid cluster");
    } catch (Exception e) {
      // Expected
    }

    // all across cluster
    r = new ServiceComponentHostRequest(c1.getClusterName(), null,
        null, null, null);
    Set<ServiceComponentHostResponse> resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(6, resps.size());

    // all for service
    r = new ServiceComponentHostRequest(c1.getClusterName(), s1.getName(),
        null, null, null);
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(5, resps.size());

    // all for component
    r = new ServiceComponentHostRequest(c1.getClusterName(), null,
        sc3.getName(), null, null);
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(1, resps.size());

    // all for host
    r = new ServiceComponentHostRequest(c1.getClusterName(), null,
        null, "h2", null);
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(2, resps.size());

    // all across cluster with state filter
    r = new ServiceComponentHostRequest(c1.getClusterName(), null,
        null, null, State.UNINSTALLED.toString());
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(1, resps.size());

    // all for service with state filter
    r = new ServiceComponentHostRequest(c1.getClusterName(), s1.getName(),
        null, null, State.INIT.toString());
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(2, resps.size());

    // all for component with state filter
    r = new ServiceComponentHostRequest(c1.getClusterName(), null,
        sc3.getName(), null, State.INSTALLED.toString());
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(0, resps.size());

    // all for host with state filter
    r = new ServiceComponentHostRequest(c1.getClusterName(), null,
        null, "h2", State.INIT.toString());
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(1, resps.size());

    // for service and host
    r = new ServiceComponentHostRequest(c1.getClusterName(), s3.getName(),
        null, "h1", null);
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(0, resps.size());

    // single sch - given service and host and component
    r = new ServiceComponentHostRequest(c1.getClusterName(), s3.getName(),
        sc3.getName(), "h3", State.INSTALLED.toString());
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(0, resps.size());

    // single sch - given service and host and component
    r = new ServiceComponentHostRequest(c1.getClusterName(), s3.getName(),
        sc3.getName(), "h3", null);
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(1, resps.size());

    ServiceComponentHostRequest r1, r2, r3;
    r1 = new ServiceComponentHostRequest(c1.getClusterName(), null,
        null, "h3", null);
    r2 = new ServiceComponentHostRequest(c1.getClusterName(), s3.getName(),
        sc3.getName(), "h2", null);
    r3 = new ServiceComponentHostRequest(c1.getClusterName(), null,
        null, "h2", null);
    Set<ServiceComponentHostRequest> reqs =
        new HashSet<ServiceComponentHostRequest>();
    reqs.addAll(Arrays.asList(r1, r2, r3));
    resps = controller.getHostComponents(reqs);
    Assert.assertEquals(4, resps.size());
  }

  @Test
  public void testGetHosts() throws AmbariException {
    setupClusterWithHosts("c1", "HDP-0.2",
        new ArrayList<String>() {{
          add("h1");
          add("h2");
        }},
        "centos5");

    setupClusterWithHosts("c2", "HDP-0.2",
        new ArrayList<String>() {{
          add("h3");
        }},
        "centos5");
    clusters.addHost("h4");
    setOsFamily(clusters.getHost("h4"), "redhat", "5.9");
    clusters.getHost("h4").persist();

    Map<String, String> attrs = new HashMap<String, String>();
    attrs.put("a1", "b1");
    clusters.getHost("h3").setHostAttributes(attrs);
    attrs.put("a2", "b2");
    clusters.getHost("h4").setHostAttributes(attrs);

    HostRequest r = new HostRequest(null, null, null);

    Set<HostResponse> resps = HostResourceProviderTest.getHosts(controller, Collections.singleton(r));

    Assert.assertEquals(4, resps.size());

    Set<String> foundHosts = new HashSet<String>();

    for (HostResponse resp : resps) {
      foundHosts.add(resp.getHostname());
      if (resp.getHostname().equals("h1")) {
        Assert.assertEquals("c1", resp.getClusterName());
        Assert.assertEquals(2, resp.getHostAttributes().size());
      } else if (resp.getHostname().equals("h2")) {
        Assert.assertEquals("c1", resp.getClusterName());
        Assert.assertEquals(2, resp.getHostAttributes().size());
      } else if (resp.getHostname().equals("h3")) {
        Assert.assertEquals("c2", resp.getClusterName());
        Assert.assertEquals(3, resp.getHostAttributes().size());
      } else if (resp.getHostname().equals("h4")) {
        //todo: why wouldn't this be null?
        Assert.assertEquals("", resp.getClusterName());
        Assert.assertEquals(4, resp.getHostAttributes().size());
      } else {
        fail("Found invalid host");
      }
    }

    Assert.assertEquals(4, foundHosts.size());

    r = new HostRequest("h1", null, null);
    resps = HostResourceProviderTest.getHosts(controller, Collections.singleton(r));
    Assert.assertEquals(1, resps.size());
    HostResponse resp = resps.iterator().next();
    Assert.assertEquals("h1", resp.getHostname());
    Assert.assertEquals("c1", resp.getClusterName());
    Assert.assertEquals(2, resp.getHostAttributes().size());

  }

  @Test
  public void testServiceUpdateBasic() throws AmbariException {
    String clusterName = "foo1";
    createCluster(clusterName);
    String serviceName = "HDFS";

    Map<String, String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "Called from a test");

    clusters.getCluster("foo1").setDesiredStackVersion(
        new StackId("HDP-0.2"));
    createService(clusterName, serviceName, State.INIT);

    Service s =
        clusters.getCluster(clusterName).getService(serviceName);
    Assert.assertNotNull(s);
    Assert.assertEquals(serviceName, s.getName());
    Assert.assertEquals(State.INIT, s.getDesiredState());
    Assert.assertEquals(clusterName, s.getCluster().getClusterName());

    Set<ServiceRequest> reqs = new HashSet<ServiceRequest>();
    ServiceRequest r;

    try {
      r = new ServiceRequest(clusterName, serviceName,
          State.INSTALLING.toString());
      reqs.clear();
      reqs.add(r);
      ServiceResourceProviderTest.updateServices(controller, reqs, mapRequestProps, true, false);
      fail("Expected fail for invalid state transition");
    } catch (Exception e) {
      // Expected
    }

    r = new ServiceRequest(clusterName, serviceName,
        State.INSTALLED.toString());
    reqs.clear();
    reqs.add(r);
    RequestStatusResponse trackAction = ServiceResourceProviderTest.updateServices(controller, reqs,
        mapRequestProps, true, false);
    Assert.assertNull(trackAction);
  }

  @Test
  public void testServiceUpdateInvalidRequest() throws AmbariException {
    // multiple clusters
    // dup services
    // multiple diff end states

    String clusterName1 = "foo1";
    createCluster(clusterName1);
    String clusterName2 = "foo2";
    createCluster(clusterName2);
    String serviceName1 = "HDFS";
    createService(clusterName1, serviceName1, null);
    String serviceName2 = "HBASE";
    String serviceName3 = "HBASE";

    Map<String, String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "Called from a test");

    try {
      createService(clusterName2, serviceName3, null);
      fail("Expected fail for invalid service for stack 0.1");
    } catch (Exception e) {
      // Expected
    }

    clusters.getCluster(clusterName1).setDesiredStackVersion(
        new StackId("HDP-0.2"));
    clusters.getCluster(clusterName2).setDesiredStackVersion(
        new StackId("HDP-0.2"));
    createService(clusterName1, serviceName2, null);
    createService(clusterName2, serviceName3, null);

    Set<ServiceRequest> reqs = new HashSet<ServiceRequest>();
    ServiceRequest req1, req2;
    try {
      reqs.clear();
      req1 = new ServiceRequest(clusterName1, serviceName1,
          State.INSTALLED.toString());
      req2 = new ServiceRequest(clusterName2, serviceName2,
          State.INSTALLED.toString());
      reqs.add(req1);
      reqs.add(req2);
      ServiceResourceProviderTest.updateServices(controller, reqs, mapRequestProps, true, false);
      fail("Expected failure for multi cluster update");
    } catch (Exception e) {
      // Expected
    }

    try {
      reqs.clear();
      req1 = new ServiceRequest(clusterName1, serviceName1,
          State.INSTALLED.toString());
      req2 = new ServiceRequest(clusterName1, serviceName1,
          State.INSTALLED.toString());
      reqs.add(req1);
      reqs.add(req2);
      ServiceResourceProviderTest.updateServices(controller, reqs, mapRequestProps, true, false);
      fail("Expected failure for dups services");
    } catch (Exception e) {
      // Expected
    }

    clusters.getCluster(clusterName1).getService(serviceName2)
        .setDesiredState(State.INSTALLED);

    try {
      reqs.clear();
      req1 = new ServiceRequest(clusterName1, serviceName1,
          State.INSTALLED.toString());
      req2 = new ServiceRequest(clusterName1, serviceName2,
          State.STARTED.toString());
      reqs.add(req1);
      reqs.add(req2);
      ServiceResourceProviderTest.updateServices(controller, reqs, mapRequestProps, true, false);
      fail("Expected failure for different states");
    } catch (Exception e) {
      // Expected
    }

  }

  @Test
  public void testServiceUpdateInvalidUpdates() {
    // FIXME test all invalid transitions
  }

  @Test
  public void testServiceUpdateRecursive() throws AmbariException {
    String clusterName = "foo1";
    createCluster(clusterName);
    clusters.getCluster(clusterName)
        .setDesiredStackVersion(new StackId("HDP-0.2"));
    String serviceName1 = "HDFS";
    createService(clusterName, serviceName1, null);
    String serviceName2 = "HBASE";
    createService(clusterName, serviceName2, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HBASE_MASTER";
    String componentName4 = "HDFS_CLIENT";

    Map<String, String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "Called from a test");

    createServiceComponent(clusterName, serviceName1, componentName1,
        State.INIT);
    createServiceComponent(clusterName, serviceName1, componentName2,
        State.INIT);
    createServiceComponent(clusterName, serviceName2, componentName3,
        State.INIT);
    createServiceComponent(clusterName, serviceName1, componentName4,
        State.INIT);
    String host1 = "h1";
    String host2 = "h2";
    addHostToCluster(host1, clusterName);
    addHostToCluster(host2, clusterName);

    Set<ServiceComponentHostRequest> set1 =
        new HashSet<ServiceComponentHostRequest>();
    ServiceComponentHostRequest r1 =
        new ServiceComponentHostRequest(clusterName, serviceName1,
            componentName1, host1, State.INIT.toString());
    ServiceComponentHostRequest r2 =
        new ServiceComponentHostRequest(clusterName, serviceName1,
            componentName2, host1, State.INIT.toString());
    ServiceComponentHostRequest r3 =
        new ServiceComponentHostRequest(clusterName, serviceName1,
            componentName1, host2, State.INIT.toString());
    ServiceComponentHostRequest r4 =
        new ServiceComponentHostRequest(clusterName, serviceName1,
            componentName2, host2, State.INIT.toString());
    ServiceComponentHostRequest r5 =
        new ServiceComponentHostRequest(clusterName, serviceName2,
            componentName3, host1, State.INIT.toString());
    ServiceComponentHostRequest r6 =
        new ServiceComponentHostRequest(clusterName, serviceName1,
            componentName4, host2, State.INIT.toString());

    set1.add(r1);
    set1.add(r2);
    set1.add(r3);
    set1.add(r4);
    set1.add(r5);
    set1.add(r6);
    controller.createHostComponents(set1);

    Cluster c1 = clusters.getCluster(clusterName);
    Service s1 = c1.getService(serviceName1);
    Service s2 = c1.getService(serviceName2);
    ServiceComponent sc1 = s1.getServiceComponent(componentName1);
    ServiceComponent sc2 = s1.getServiceComponent(componentName2);
    ServiceComponent sc3 = s2.getServiceComponent(componentName3);
    ServiceComponent sc4 = s1.getServiceComponent(componentName4);
    ServiceComponentHost sch1 = sc1.getServiceComponentHost(host1);
    ServiceComponentHost sch2 = sc2.getServiceComponentHost(host1);
    ServiceComponentHost sch3 = sc1.getServiceComponentHost(host2);
    ServiceComponentHost sch4 = sc2.getServiceComponentHost(host2);
    ServiceComponentHost sch5 = sc3.getServiceComponentHost(host1);
    ServiceComponentHost sch6 = sc4.getServiceComponentHost(host2);

    s1.setDesiredState(State.INSTALLED);
    s2.setDesiredState(State.INSTALLED);
    sc1.setDesiredState(State.STARTED);
    sc2.setDesiredState(State.INIT);
    sc3.setDesiredState(State.STARTED);
    sc4.setDesiredState(State.INSTALLED);
    sch1.setDesiredState(State.INSTALLED);
    sch2.setDesiredState(State.INSTALLED);
    sch3.setDesiredState(State.INSTALLED);
    sch4.setDesiredState(State.INSTALLED);
    sch5.setDesiredState(State.INSTALLED);
    sch6.setDesiredState(State.INSTALLED);
    sch1.setState(State.INSTALLED);
    sch2.setState(State.INSTALLED);
    sch3.setState(State.INSTALLED);
    sch4.setState(State.INSTALLED);
    sch5.setState(State.INSTALLED);
    sch6.setState(State.INSTALLED);

    Set<ServiceRequest> reqs = new HashSet<ServiceRequest>();
    ServiceRequest req1, req2;
    try {
      reqs.clear();
      req1 = new ServiceRequest(clusterName, serviceName1,
          State.STARTED.toString());
      reqs.add(req1);
      ServiceResourceProviderTest.updateServices(controller, reqs, mapRequestProps, true, false);
      fail("Expected failure for invalid state update");
    } catch (Exception e) {
      // Expected
    }

    s1.setDesiredState(State.INSTALLED);
    s2.setDesiredState(State.INSTALLED);
    sc1.setDesiredState(State.STARTED);
    sc2.setDesiredState(State.INSTALLED);
    sc3.setDesiredState(State.STARTED);
    sch1.setDesiredState(State.INSTALLED);
    sch2.setDesiredState(State.INSTALLED);
    sch3.setDesiredState(State.INSTALLED);
    sch4.setDesiredState(State.INSTALLED);
    sch5.setDesiredState(State.INSTALLED);
    sch1.setState(State.INIT);
    sch2.setState(State.INSTALLED);
    sch3.setState(State.INIT);
    sch4.setState(State.INSTALLED);
    sch5.setState(State.INSTALLED);

    try {
      reqs.clear();
      req1 = new ServiceRequest(clusterName, serviceName1,
          State.STARTED.toString());
      reqs.add(req1);
      ServiceResourceProviderTest.updateServices(controller, reqs, mapRequestProps, true, false);
      fail("Expected failure for invalid state update");
    } catch (Exception e) {
      // Expected
    }

    s1.setDesiredState(State.INSTALLED);
    s2.setDesiredState(State.INSTALLED);
    sc1.setDesiredState(State.STARTED);
    sc2.setDesiredState(State.INSTALLED);
    sc3.setDesiredState(State.STARTED);
    sch1.setDesiredState(State.STARTED);
    sch2.setDesiredState(State.STARTED);
    sch3.setDesiredState(State.STARTED);
    sch4.setDesiredState(State.STARTED);
    sch5.setDesiredState(State.STARTED);
    sch1.setState(State.INSTALLED);
    sch2.setState(State.INSTALLED);
    sch3.setState(State.INSTALLED);
    sch4.setState(State.STARTED);
    sch5.setState(State.INSTALLED);

    reqs.clear();
    req1 = new ServiceRequest(clusterName, serviceName1,
        State.STARTED.toString());
    req2 = new ServiceRequest(clusterName, serviceName2,
        State.STARTED.toString());
    reqs.add(req1);
    reqs.add(req2);
    RequestStatusResponse trackAction = ServiceResourceProviderTest.updateServices(controller, reqs,
      mapRequestProps, true, false);

    Assert.assertEquals(State.STARTED, s1.getDesiredState());
    Assert.assertEquals(State.STARTED, s2.getDesiredState());
    Assert.assertEquals(State.STARTED, sc1.getDesiredState());
    Assert.assertEquals(State.STARTED, sc2.getDesiredState());
    Assert.assertEquals(State.STARTED, sc3.getDesiredState());
    Assert.assertEquals(State.INSTALLED, sc4.getDesiredState());
    Assert.assertEquals(State.STARTED, sch1.getDesiredState());
    Assert.assertEquals(State.STARTED, sch2.getDesiredState());
    Assert.assertEquals(State.STARTED, sch3.getDesiredState());
    Assert.assertEquals(State.STARTED, sch4.getDesiredState());
    Assert.assertEquals(State.STARTED, sch5.getDesiredState());
    Assert.assertEquals(State.INSTALLED, sch6.getDesiredState());
    Assert.assertEquals(State.INSTALLED, sch1.getState());
    Assert.assertEquals(State.INSTALLED, sch2.getState());
    Assert.assertEquals(State.INSTALLED, sch3.getState());
    Assert.assertEquals(State.STARTED, sch4.getState());
    Assert.assertEquals(State.INSTALLED, sch5.getState());
    Assert.assertEquals(State.INSTALLED, sch6.getState());

    long requestId = trackAction.getRequestId();
    List<Stage> stages = actionDB.getAllStages(requestId);

    for (Stage stage : stages) {
      LOG.debug("Stage dump: " + stage.toString());
    }

    Assert.assertTrue(!stages.isEmpty());
    Assert.assertEquals(3, stages.size());

    // expected
    // sch1 to start
    // sch2 to start
    // sch3 to start
    // sch5 to start
    Stage stage1 = null, stage2 = null, stage3 = null;
    for (Stage s : stages) {
      if (s.getStageId() == 0) { stage1 = s; }
      if (s.getStageId() == 1) { stage2 = s; }
      if (s.getStageId() == 2) { stage3 = s; }
    }

    Assert.assertEquals(2, stage1.getExecutionCommands(host1).size());
    Assert.assertEquals(1, stage1.getExecutionCommands(host2).size());
    Assert.assertEquals(1, stage2.getExecutionCommands(host1).size());

    Assert.assertNotNull(stage1.getExecutionCommandWrapper(host1, "NAMENODE"));
    Assert.assertNotNull(stage1.getExecutionCommandWrapper(host1, "DATANODE"));
    Assert.assertNotNull(stage1.getExecutionCommandWrapper(host2, "NAMENODE"));
    Assert.assertNotNull(stage2.getExecutionCommandWrapper(host1, "HBASE_MASTER"));
    Assert.assertNull(stage1.getExecutionCommandWrapper(host2, "DATANODE"));
    Assert.assertNotNull(stage3.getExecutionCommandWrapper(host1, "HBASE_SERVICE_CHECK"));
    Assert.assertNotNull(stage2.getExecutionCommandWrapper(host2, "HDFS_SERVICE_CHECK"));

    Type type = new TypeToken<Map<String, String>>() {}.getType();


    for (Stage s : stages) {
      for (List<ExecutionCommandWrapper> list : s.getExecutionCommands().values()) {
        for (ExecutionCommandWrapper ecw : list) {
          if (ecw.getExecutionCommand().getRole().contains("SERVICE_CHECK")) {
            Map<String, String> hostParams = StageUtils.getGson().fromJson(s.getHostParamsStage(), type);
            Assert.assertNotNull(hostParams);
            Assert.assertTrue(hostParams.size() > 0);
            Assert.assertTrue(hostParams.containsKey("stack_version"));
            Assert.assertEquals(hostParams.get("stack_version"), c1.getDesiredStackVersion().getStackVersion());
          }
        }
      }
    }

    // manually set live state
    sch1.setState(State.STARTED);
    sch2.setState(State.STARTED);
    sch3.setState(State.STARTED);
    sch4.setState(State.STARTED);
    sch5.setState(State.STARTED);

    // test no-op
    reqs.clear();
    req1 = new ServiceRequest(clusterName, serviceName1,
        State.STARTED.toString());
    req2 = new ServiceRequest(clusterName, serviceName2,
        State.STARTED.toString());
    reqs.add(req1);
    reqs.add(req2);
    trackAction = ServiceResourceProviderTest.updateServices(controller, reqs, mapRequestProps, true,
      false);
    Assert.assertNull(trackAction);

  }

  @Test
  public void testServiceComponentUpdateRecursive() throws AmbariException {
    String clusterName = "foo1";
    createCluster(clusterName);
    String serviceName1 = "HDFS";
    createService(clusterName, serviceName1, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";
    createServiceComponent(clusterName, serviceName1, componentName1,
        State.INIT);
    createServiceComponent(clusterName, serviceName1, componentName2,
        State.INIT);
    createServiceComponent(clusterName, serviceName1, componentName3,
        State.INIT);
    String host1 = "h1";
    String host2 = "h2";
    addHostToCluster(host1, clusterName);
    addHostToCluster(host2, clusterName);

    Set<ServiceComponentHostRequest> set1 =
        new HashSet<ServiceComponentHostRequest>();
    ServiceComponentHostRequest r1 =
        new ServiceComponentHostRequest(clusterName, serviceName1,
            componentName1, host1, State.INIT.toString());
    ServiceComponentHostRequest r2 =
        new ServiceComponentHostRequest(clusterName, serviceName1,
            componentName2, host1, State.INIT.toString());
    ServiceComponentHostRequest r3 =
        new ServiceComponentHostRequest(clusterName, serviceName1,
            componentName1, host2, State.INIT.toString());
    ServiceComponentHostRequest r4 =
        new ServiceComponentHostRequest(clusterName, serviceName1,
            componentName2, host2, State.INIT.toString());
    ServiceComponentHostRequest r5 =
        new ServiceComponentHostRequest(clusterName, serviceName1,
            componentName3, host1, State.INIT.toString());

    set1.add(r1);
    set1.add(r2);
    set1.add(r3);
    set1.add(r4);
    set1.add(r5);
    controller.createHostComponents(set1);

    Cluster c1 = clusters.getCluster(clusterName);
    Service s1 = c1.getService(serviceName1);
    ServiceComponent sc1 = s1.getServiceComponent(componentName1);
    ServiceComponent sc2 = s1.getServiceComponent(componentName2);
    ServiceComponent sc3 = s1.getServiceComponent(componentName3);
    ServiceComponentHost sch1 = sc1.getServiceComponentHost(host1);
    ServiceComponentHost sch2 = sc2.getServiceComponentHost(host1);
    ServiceComponentHost sch3 = sc1.getServiceComponentHost(host2);
    ServiceComponentHost sch4 = sc2.getServiceComponentHost(host2);
    ServiceComponentHost sch5 = sc3.getServiceComponentHost(host1);

    s1.setDesiredState(State.INSTALLED);
    sc1.setDesiredState(State.INIT);
    sc2.setDesiredState(State.INIT);
    sc3.setDesiredState(State.STARTED);
    sch1.setDesiredState(State.INSTALLED);
    sch2.setDesiredState(State.INSTALLED);
    sch3.setDesiredState(State.STARTED);
    sch4.setDesiredState(State.INSTALLED);
    sch5.setDesiredState(State.INSTALLED);
    sch1.setState(State.INSTALLED);
    sch2.setState(State.INSTALLED);
    sch3.setState(State.STARTED);
    sch4.setState(State.INSTALLED);
    sch5.setState(State.UNKNOWN);

    Set<ServiceComponentRequest> reqs =
        new HashSet<ServiceComponentRequest>();
    ServiceComponentRequest req1, req2, req3;

    // confirm an UNKOWN doesn't fail
    req1 = new ServiceComponentRequest(clusterName, serviceName1,
        sc3.getName(), State.INSTALLED.toString());
    reqs.add(req1);
    ComponentResourceProviderTest.updateComponents(controller, reqs, Collections.<String, String>emptyMap(), true);
    try {
      reqs.clear();
      req1 = new ServiceComponentRequest(clusterName, serviceName1,
          sc1.getName(), State.INIT.toString());
      reqs.add(req1);
      ComponentResourceProviderTest.updateComponents(controller, reqs, Collections.<String, String>emptyMap(), true);
      fail("Expected failure for invalid state update");
    } catch (Exception e) {
      // Expected
    }

    s1.setDesiredState(State.INSTALLED);
    sc1.setDesiredState(State.STARTED);
    sc2.setDesiredState(State.INSTALLED);
    sc3.setDesiredState(State.STARTED);
    sch1.setDesiredState(State.INIT);
    sch2.setDesiredState(State.INIT);
    sch3.setDesiredState(State.INIT);
    sch4.setDesiredState(State.INIT);
    sch5.setDesiredState(State.INIT);
    sch1.setState(State.INIT);
    sch2.setState(State.INSTALLED);
    sch3.setState(State.INIT);
    sch4.setState(State.INSTALLED);
    sch5.setState(State.INSTALLED);

    try {
      reqs.clear();
      req1 = new ServiceComponentRequest(clusterName, serviceName1,
          sc1.getName(), State.STARTED.toString());
      reqs.add(req1);
      ComponentResourceProviderTest.updateComponents(controller, reqs, Collections.<String, String>emptyMap(), true);
      fail("Expected failure for invalid state update");
    } catch (Exception e) {
      // Expected
    }

    s1.setDesiredState(State.INSTALLED);
    sc1.setDesiredState(State.STARTED);
    sc2.setDesiredState(State.INIT);
    sc3.setDesiredState(State.STARTED);
    sch1.setDesiredState(State.INIT);
    sch2.setDesiredState(State.INIT);
    sch3.setDesiredState(State.INIT);
    sch4.setDesiredState(State.INIT);
    sch5.setDesiredState(State.INIT);
    sch1.setState(State.STARTED);
    sch2.setState(State.INIT);
    sch3.setState(State.INSTALLED);
    sch4.setState(State.STARTED);
    sch5.setState(State.INIT);

    reqs.clear();
    req1 = new ServiceComponentRequest(clusterName, serviceName1,
        sc1.getName(), State.INSTALLED.toString());
    req2 = new ServiceComponentRequest(clusterName, serviceName1,
        sc2.getName(), State.INSTALLED.toString());
    req3 = new ServiceComponentRequest(clusterName, serviceName1,
        sc3.getName(), State.INSTALLED.toString());
    reqs.add(req1);
    reqs.add(req2);
    reqs.add(req3);
    RequestStatusResponse trackAction = ComponentResourceProviderTest.updateComponents(controller, reqs, Collections.<String, String>emptyMap(), true);

    Assert.assertEquals(State.INSTALLED, s1.getDesiredState());
    Assert.assertEquals(State.INSTALLED, sc1.getDesiredState());
    Assert.assertEquals(State.INSTALLED, sc2.getDesiredState());
    Assert.assertEquals(State.INSTALLED, sc3.getDesiredState());
    Assert.assertEquals(State.INSTALLED, sch1.getDesiredState());
    Assert.assertEquals(State.INSTALLED, sch2.getDesiredState());
    Assert.assertEquals(State.INSTALLED, sch3.getDesiredState());
    Assert.assertEquals(State.INSTALLED, sch4.getDesiredState());
    Assert.assertEquals(State.INSTALLED, sch5.getDesiredState());
    Assert.assertEquals(State.STARTED, sch1.getState());
    Assert.assertEquals(State.INIT, sch2.getState());
    Assert.assertEquals(State.INSTALLED, sch3.getState());
    Assert.assertEquals(State.STARTED, sch4.getState());
    Assert.assertEquals(State.INIT, sch5.getState());

    long requestId = trackAction.getRequestId();
    List<Stage> stages = actionDB.getAllStages(requestId);
    Assert.assertTrue(!stages.isEmpty());

    // FIXME check stage count

    for (Stage stage : stages) {
      LOG.debug("Stage dump: " + stage.toString());
    }

    // FIXME verify stages content - execution commands, etc

    // maually set live state
    sch1.setState(State.INSTALLED);
    sch2.setState(State.INSTALLED);
    sch3.setState(State.INSTALLED);
    sch4.setState(State.INSTALLED);
    sch5.setState(State.INSTALLED);

    // test no-op
    reqs.clear();
    req1 = new ServiceComponentRequest(clusterName, serviceName1,
        sc1.getName(), State.INSTALLED.toString());
    req2 = new ServiceComponentRequest(clusterName, serviceName1,
        sc2.getName(), State.INSTALLED.toString());
    reqs.add(req1);
    reqs.add(req2);
    trackAction = ComponentResourceProviderTest.updateComponents(controller, reqs, Collections.<String, String>emptyMap(), true);
    Assert.assertNull(trackAction);
  }

  @Test
  public void testServiceComponentHostUpdateRecursive() throws Exception {
    String clusterName = "foo1";
    createCluster(clusterName);
    String serviceName1 = "HDFS";
    createService(clusterName, serviceName1, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";
    createServiceComponent(clusterName, serviceName1, componentName1,
        State.INIT);
    createServiceComponent(clusterName, serviceName1, componentName2,
        State.INIT);
    createServiceComponent(clusterName, serviceName1, componentName3,
        State.INIT);
    String host1 = "h1";
    String host2 = "h2";
    addHostToCluster(host1, clusterName);
    addHostToCluster(host2, clusterName);


    Set<ServiceComponentHostRequest> set1 =
        new HashSet<ServiceComponentHostRequest>();
    ServiceComponentHostRequest r1 =
        new ServiceComponentHostRequest(clusterName, serviceName1,
            componentName1, host1, State.INIT.toString());
    ServiceComponentHostRequest r2 =
        new ServiceComponentHostRequest(clusterName, serviceName1,
            componentName2, host1, State.INIT.toString());
    ServiceComponentHostRequest r3 =
        new ServiceComponentHostRequest(clusterName, serviceName1,
            componentName1, host2, State.INIT.toString());
    ServiceComponentHostRequest r4 =
        new ServiceComponentHostRequest(clusterName, serviceName1,
            componentName2, host2, State.INIT.toString());
    ServiceComponentHostRequest r5 =
        new ServiceComponentHostRequest(clusterName, serviceName1,
            componentName3, host1, State.INIT.toString());

    set1.add(r1);
    set1.add(r2);
    set1.add(r3);
    set1.add(r4);
    set1.add(r5);
    controller.createHostComponents(set1);

    Cluster c1 = clusters.getCluster(clusterName);
    Service s1 = c1.getService(serviceName1);
    ServiceComponent sc1 = s1.getServiceComponent(componentName1);
    ServiceComponent sc2 = s1.getServiceComponent(componentName2);
    ServiceComponent sc3 = s1.getServiceComponent(componentName3);
    ServiceComponentHost sch1 = sc1.getServiceComponentHost(host1);
    ServiceComponentHost sch2 = sc2.getServiceComponentHost(host1);
    ServiceComponentHost sch3 = sc1.getServiceComponentHost(host2);
    ServiceComponentHost sch4 = sc2.getServiceComponentHost(host2);
    ServiceComponentHost sch5 = sc3.getServiceComponentHost(host1);

    s1.setDesiredState(State.INIT);
    sc1.setDesiredState(State.INIT);
    sc2.setDesiredState(State.INIT);
    sc3.setDesiredState(State.INIT);
    sch1.setDesiredState(State.INIT);
    sch2.setDesiredState(State.INIT);
    sch3.setDesiredState(State.INIT);
    sch4.setDesiredState(State.INSTALLED);
    sch5.setDesiredState(State.INSTALLED);
    sch1.setState(State.INIT);
    sch2.setState(State.INSTALL_FAILED);
    sch3.setState(State.INIT);
    sch4.setState(State.INSTALLED);
    sch5.setState(State.INSTALLED);

    ServiceComponentHostRequest req1, req2, req3, req4, req5;
    Set<ServiceComponentHostRequest> reqs =
        new HashSet<ServiceComponentHostRequest>();

    //todo: I had to comment this portion of the test out for now because I had to modify
    //todo: the transition validation code for the new advanced provisioning
    //todo: work which causes a failure here due to lack of an exception.
//    try {
//      reqs.clear();
//      req1 = new ServiceComponentHostRequest(clusterName, serviceName1,
//          componentName1, host1,
//          State.STARTED.toString());
//      reqs.add(req1);
//      updateHostComponents(reqs, Collections.<String, String>emptyMap(), true);
//      fail("Expected failure for invalid transition");
//    } catch (Exception e) {
//      // Expected
//    }

    try {
      reqs.clear();
      req1 = new ServiceComponentHostRequest(clusterName, serviceName1,
          componentName1, host1,
          State.INSTALLED.toString());
      req2 = new ServiceComponentHostRequest(clusterName, serviceName1,
          componentName1, host2,
          State.INSTALLED.toString());
      req3 = new ServiceComponentHostRequest(clusterName, serviceName1,
          componentName2, host1,
          State.INSTALLED.toString());
      req4 = new ServiceComponentHostRequest(clusterName, serviceName1,
          componentName2, host2,
          State.INSTALLED.toString());
      req5 = new ServiceComponentHostRequest(clusterName, serviceName1,
          componentName3, host1,
          State.STARTED.toString());
      reqs.add(req1);
      reqs.add(req2);
      reqs.add(req3);
      reqs.add(req4);
      reqs.add(req5);
      updateHostComponents(reqs, Collections.<String, String>emptyMap(), true);
      // Expected, now client components with STARTED status will be ignored
    } catch (Exception e) {
      fail("Failure for invalid states");
    }

    reqs.clear();
    req1 = new ServiceComponentHostRequest(clusterName, null,
        componentName1, host1, State.INSTALLED.toString());
    req2 = new ServiceComponentHostRequest(clusterName, serviceName1,
        componentName1, host2, State.INSTALLED.toString());
    req3 = new ServiceComponentHostRequest(clusterName, null,
        componentName2, host1, State.INSTALLED.toString());
    req4 = new ServiceComponentHostRequest(clusterName, serviceName1,
        componentName2, host2, State.INSTALLED.toString());
    req5 = new ServiceComponentHostRequest(clusterName, serviceName1,
        componentName3, host1, State.INSTALLED.toString());
    reqs.add(req1);
    reqs.add(req2);
    reqs.add(req3);
    reqs.add(req4);
    reqs.add(req5);
    RequestStatusResponse trackAction = updateHostComponents(reqs,
        Collections.<String, String>emptyMap(), true);
    Assert.assertNotNull(trackAction);

    long requestId = trackAction.getRequestId();

    Assert.assertFalse(actionDB.getAllStages(requestId).isEmpty());
    List<Stage> stages = actionDB.getAllStages(requestId);
    // FIXME check stage count

    for (Stage stage : stages) {
      LOG.debug("Stage dump: " + stage.toString());
    }

    // FIXME verify stages content - execution commands, etc

    // manually set live state
    sch1.setState(State.INSTALLED);
    sch2.setState(State.INSTALLED);
    sch3.setState(State.INSTALLED);
    sch4.setState(State.INSTALLED);
    sch5.setState(State.INSTALLED);

    // test no-op
    reqs.clear();
    req1 = new ServiceComponentHostRequest(clusterName, serviceName1,
        componentName1, host1,
        State.INSTALLED.toString());
    req2 = new ServiceComponentHostRequest(clusterName, serviceName1,
        componentName1, host2,
        State.INSTALLED.toString());
    reqs.add(req1);
    reqs.add(req2);
    trackAction = updateHostComponents(reqs, Collections.<String,
        String>emptyMap(), true);
    Assert.assertNull(trackAction);
  }

  @Ignore
  @Test
  public void testServiceComponentHostUpdateStackId() throws Exception {
    String clusterName = "foo1";
    createCluster(clusterName);
    String serviceName1 = "HDFS";
    createService(clusterName, serviceName1, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    createServiceComponent(clusterName, serviceName1, componentName1,
        State.INIT);
    createServiceComponent(clusterName, serviceName1, componentName2,
        State.INIT);
    String host1 = "h1";
    String host2 = "h2";
    addHostToCluster(host1, clusterName);
    addHostToCluster(host2, clusterName);

    Set<ServiceComponentHostRequest> set1 =
        new HashSet<ServiceComponentHostRequest>();
    ServiceComponentHostRequest r1 =
        new ServiceComponentHostRequest(clusterName, serviceName1,
            componentName1, host1, State.INIT.toString());
    ServiceComponentHostRequest r2 =
        new ServiceComponentHostRequest(clusterName, serviceName1,
            componentName1, host2, State.INIT.toString());
    ServiceComponentHostRequest r3 =
        new ServiceComponentHostRequest(clusterName, serviceName1,
            componentName2, host1, State.INIT.toString());

    set1.add(r1);
    set1.add(r2);
    set1.add(r3);
    controller.createHostComponents(set1);

    Cluster c1 = clusters.getCluster(clusterName);
    Service s1 = c1.getService(serviceName1);
    ServiceComponent sc1 = s1.getServiceComponent(componentName1);
    ServiceComponent sc2 = s1.getServiceComponent(componentName2);
    ServiceComponentHost sch1 = sc1.getServiceComponentHost(host1);
    ServiceComponentHost sch2 = sc1.getServiceComponentHost(host2);
    ServiceComponentHost sch3 = sc2.getServiceComponentHost(host1);

    s1.setDesiredState(State.INSTALLED);
    sc1.setDesiredState(State.INSTALLED);
    sc2.setDesiredState(State.INSTALLED);

    ServiceComponentHostRequest req1;
    ServiceComponentHostRequest req2;
    ServiceComponentHostRequest req3;
    Set<ServiceComponentHostRequest> reqs =
        new HashSet<ServiceComponentHostRequest>();

    StackId newStack = new StackId("HDP-0.2");
    StackId oldStack = new StackId("HDP-0.1");
    c1.setCurrentStackVersion(newStack);
    c1.setDesiredStackVersion(newStack);
    sch1.setState(State.INSTALLED);
    sch2.setState(State.UPGRADING);
    sch1.setDesiredState(State.INSTALLED);
    sch2.setDesiredState(State.INSTALLED);

    sch1.setStackVersion(oldStack);
    sch2.setStackVersion(oldStack);
    sch1.setDesiredStackVersion(newStack);
    sch2.setDesiredStackVersion(oldStack);

    reqs.clear();
    req1 = new ServiceComponentHostRequest(clusterName, serviceName1,
        componentName1, host1,
        State.INSTALLED.toString());
    req1.setDesiredStackId("HDP-0.2");
    reqs.add(req1);
    req2 = new ServiceComponentHostRequest(clusterName, serviceName1,
        componentName1, host2,
        State.INSTALLED.toString());
    req2.setDesiredStackId("HDP-0.2");
    reqs.add(req2);

    Map<String,String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "testServiceComponentHostUpdateStackId");

    RequestStatusResponse resp = updateHostComponents(reqs, mapRequestProps, true);
    List<Stage> stages = actionDB.getAllStages(resp.getRequestId());
    Assert.assertEquals(1, stages.size());
    Assert.assertEquals(2, stages.get(0).getOrderedHostRoleCommands().size());
    Assert.assertEquals("testServiceComponentHostUpdateStackId", stages.get(0).getRequestContext());
    Assert.assertEquals(State.UPGRADING, sch1.getState());
    Assert.assertEquals(State.UPGRADING, sch2.getState());
    sch1.refresh();
    Assert.assertTrue(sch1.getDesiredStackVersion().compareTo(newStack) == 0);
    sch2.refresh();
    Assert.assertTrue(sch2.getDesiredStackVersion().compareTo(newStack) == 0);
    for (HostRoleCommand command : stages.get(0).getOrderedHostRoleCommands()) {
      ExecutionCommand execCommand = command.getExecutionCommandWrapper().getExecutionCommand();
      Assert.assertTrue(execCommand.getCommandParams().containsKey("source_stack_version"));
      Assert.assertTrue(execCommand.getCommandParams().containsKey("target_stack_version"));
      Assert.assertEquals(RoleCommand.UPGRADE, execCommand.getRoleCommand());
    }

    sch1.setState(State.INSTALLED);
    sch1.setDesiredState(State.INSTALLED);
    sch2.setState(State.UPGRADING);
    sch2.setDesiredState(State.INSTALLED);
    sch3.setState(State.UPGRADING);
    sch3.setDesiredState(State.INSTALLED);

    sch3.setStackVersion(oldStack);
    sch3.setDesiredStackVersion(newStack);

    reqs.clear();
    req1 = new ServiceComponentHostRequest(clusterName, serviceName1,
        componentName1, host1,
        State.INSTALLED.toString());
    req1.setDesiredStackId("HDP-0.2");
    reqs.add(req1);
    req2 = new ServiceComponentHostRequest(clusterName, serviceName1,
        componentName1, host2,
        State.INSTALLED.toString());
    req2.setDesiredStackId("HDP-0.2");
    reqs.add(req2);
    req3 = new ServiceComponentHostRequest(clusterName, serviceName1,
        componentName2, host1,
        State.INSTALLED.toString());
    req3.setDesiredStackId("HDP-0.2");
    reqs.add(req3);

    resp = updateHostComponents(reqs, Collections.<String, String>emptyMap(), true);
    stages = actionDB.getAllStages(resp.getRequestId());
    Assert.assertEquals(2, stages.size());
    Assert.assertEquals(2, stages.get(0).getOrderedHostRoleCommands().size());
    Assert.assertEquals("", stages.get(0).getRequestContext());
    Assert.assertEquals(State.UPGRADING, sch1.getState());
    Assert.assertEquals(State.UPGRADING, sch2.getState());
    Assert.assertEquals(State.UPGRADING, sch3.getState());
    sch1.refresh();
    Assert.assertTrue(sch1.getDesiredStackVersion().compareTo(newStack) == 0);
    sch2.refresh();
    Assert.assertTrue(sch2.getDesiredStackVersion().compareTo(newStack) == 0);
    sch3.refresh();
    Assert.assertTrue(sch3.getDesiredStackVersion().compareTo(newStack) == 0);
    for (Stage stage : stages) {
      for (HostRoleCommand command : stage.getOrderedHostRoleCommands()) {
        ExecutionCommand execCommand = command.getExecutionCommandWrapper().getExecutionCommand();
        Assert.assertTrue(execCommand.getCommandParams().containsKey("source_stack_version"));
        Assert.assertTrue(execCommand.getCommandParams().containsKey("target_stack_version"));
        Assert.assertEquals("{\"stackName\":\"HDP\",\"stackVersion\":\"0.2\"}",
            execCommand.getCommandParams().get("target_stack_version"));
        Assert.assertEquals(RoleCommand.UPGRADE, execCommand.getRoleCommand());
      }
    }
  }

  @Ignore
  @Test
  public void testServiceComponentHostUpdateStackIdError() throws Exception {
    String clusterName = "foo1";
    createCluster(clusterName);
    String serviceName1 = "HDFS";
    createService(clusterName, serviceName1, null);
    String componentName1 = "NAMENODE";
    createServiceComponent(clusterName, serviceName1, componentName1,
        State.INIT);
    String host1 = "h1";
    String host2 = "h2";
    addHostToCluster(host1, clusterName);
    addHostToCluster(host2, clusterName);

    Set<ServiceComponentHostRequest> set1 =
        new HashSet<ServiceComponentHostRequest>();
    ServiceComponentHostRequest r1 =
        new ServiceComponentHostRequest(clusterName, serviceName1,
            componentName1, host1, State.INIT.toString());
    ServiceComponentHostRequest r2 =
        new ServiceComponentHostRequest(clusterName, serviceName1,
            componentName1, host2, State.INIT.toString());

    set1.add(r1);
    set1.add(r2);
    controller.createHostComponents(set1);

    Cluster c1 = clusters.getCluster(clusterName);
    Service s1 = c1.getService(serviceName1);
    ServiceComponent sc1 = s1.getServiceComponent(componentName1);
    ServiceComponentHost sch1 = sc1.getServiceComponentHost(host1);
    ServiceComponentHost sch2 = sc1.getServiceComponentHost(host2);

    s1.setDesiredState(State.INIT);
    sc1.setDesiredState(State.INIT);

    ServiceComponentHostRequest req1;
    ServiceComponentHostRequest req2;
    Set<ServiceComponentHostRequest> reqs =
        new HashSet<ServiceComponentHostRequest>();

    reqs.clear();
    req1 = new ServiceComponentHostRequest(clusterName, serviceName1,
        componentName1, host1, State.STARTED.toString());
    req1.setDesiredStackId("invalid stack id");
    reqs.add(req1);
    updateHostAndCompareExpectedFailure(reqs, "Invalid desired stack id");

    c1.setCurrentStackVersion(new StackId("HDP-0.0"));
    sch1.setStackVersion(new StackId("HDP-0.1"));
    reqs.clear();
    req1 = new ServiceComponentHostRequest(clusterName, serviceName1,
        componentName1, host1,
        State.STARTED.toString());
    req1.setDesiredStackId("HDP-0.2");
    reqs.add(req1);
    updateHostAndCompareExpectedFailure(reqs, "Cluster has not been upgraded yet");

    c1.setCurrentStackVersion(new StackId("HDP2-0.1"));
    reqs.clear();
    req1 = new ServiceComponentHostRequest(clusterName, serviceName1,
        componentName1, host1,
        State.STARTED.toString());
    req1.setDesiredStackId("HDP-0.2");
    reqs.add(req1);
    updateHostAndCompareExpectedFailure(reqs, "Deployed stack name and requested stack names");

    c1.setCurrentStackVersion(new StackId("HDP-0.2"));
    reqs.clear();
    req1 = new ServiceComponentHostRequest(clusterName, serviceName1,
        componentName1, host1,
        State.STARTED.toString());
    req1.setDesiredStackId("HDP-0.3");
    reqs.add(req1);
    updateHostAndCompareExpectedFailure(reqs, "Component host can only be upgraded to the same version");

    c1.setCurrentStackVersion(new StackId("HDP-0.2"));
    sch1.setState(State.STARTED);
    reqs.clear();
    req1 = new ServiceComponentHostRequest(clusterName, serviceName1,
        componentName1, host1,
        State.STARTED.toString());
    req1.setDesiredStackId("HDP-0.2");
    reqs.add(req1);
    updateHostAndCompareExpectedFailure(reqs, "Component host is in an invalid state for upgrade");

    c1.setCurrentStackVersion(new StackId("HDP-0.2"));
    sch1.setState(State.UPGRADING);
    reqs.clear();
    req1 = new ServiceComponentHostRequest(clusterName, serviceName1,
        componentName1, host1,
        State.STARTED.toString());
    req1.setDesiredStackId("HDP-0.2");
    reqs.add(req1);
    updateHostAndCompareExpectedFailure(reqs, "The desired state for an upgrade request must be");

    c1.setCurrentStackVersion(new StackId("HDP-0.2"));
    sch1.setState(State.UPGRADING);
    reqs.clear();
    req1 = new ServiceComponentHostRequest(clusterName, serviceName1,
        componentName1, host1, null);
    req1.setDesiredStackId("HDP-0.2");
    reqs.add(req1);
    updateHostAndCompareExpectedFailure(reqs, "The desired state for an upgrade request must be");

    c1.setCurrentStackVersion(new StackId("HDP-0.2"));
    sch1.setState(State.INSTALLED);
    sch1.setDesiredState(State.INSTALLED);
    sch2.setState(State.INSTALLED);
    sch2.setDesiredState(State.INSTALLED);
    reqs.clear();
    req1 = new ServiceComponentHostRequest(clusterName, serviceName1,
        componentName1, host1,
        State.INSTALLED.toString());
    req1.setDesiredStackId("HDP-0.2");
    reqs.add(req1);
    req2 = new ServiceComponentHostRequest(clusterName, serviceName1,
        componentName1, host2,
        State.STARTED.toString());
    reqs.add(req2);
    updateHostAndCompareExpectedFailure(reqs, "An upgrade request cannot be combined with other");

    c1.setCurrentStackVersion(new StackId("HDP-0.2"));
    sch1.setState(State.INSTALLED);
    sch1.setStackVersion(new StackId("HDP-0.2"));
    reqs.clear();
    req1 = new ServiceComponentHostRequest(clusterName, serviceName1,
        componentName1, host1,
        null);
    req1.setDesiredStackId("HDP-0.2");
    reqs.add(req1);

    RequestStatusResponse resp = updateHostComponents(reqs,
        Collections.<String,String>emptyMap(), true);
    Assert.assertNull(resp);

    c1.setCurrentStackVersion(new StackId("HDP-0.2"));
    sch1.setState(State.INSTALLED);
    sch1.setStackVersion(new StackId("HDP-0.2"));
    reqs.clear();
    req1 = new ServiceComponentHostRequest(clusterName, serviceName1,
        componentName1, host1,
        State.INSTALLED.toString());
    req1.setDesiredStackId("HDP-0.2");
    reqs.add(req1);
    resp = updateHostComponents(reqs, Collections.<String,String>emptyMap(), true);
    Assert.assertNull(resp);
  }

  private void updateHostAndCompareExpectedFailure(Set<ServiceComponentHostRequest> reqs,
                                                   String expectedMessage) {
    try {
      updateHostComponents(reqs, Collections.<String,String>emptyMap(), true);
      fail("Expected failure: " + expectedMessage);
    } catch (Exception e) {
      LOG.info("Actual exception message: " + e.getMessage());
      Assert.assertTrue(e.getMessage().contains(expectedMessage));
    }
  }

  @Test
  public void testStartClientComponent() {
    // FIXME write test after meta data integration
    // start should fail
  }

  @Test
  public void testStartClientHostComponent() {
    // FIXME write test after meta data integration
    // start should fail
  }

  @Test
  public void testCreateCustomActions() throws Exception {
    setupClusterWithHosts("c1", "HDP-2.0.6",
        new ArrayList<String>() {{
          add("h1");
          add("h2");
          add("h3");
        }},
        "centos6");

    Cluster cluster = clusters.getCluster("c1");
    cluster.setDesiredStackVersion(new StackId("HDP-2.0.6"));
    cluster.setCurrentStackVersion(new StackId("HDP-2.0.6"));

    ConfigFactory cf = injector.getInstance(ConfigFactory.class);
    Config config1 = cf.createNew(cluster, "global",
        new HashMap<String, String>() {{
          put("key1", "value1");
        }}, new HashMap<String, Map<String, String>>());
    config1.setTag("version1");

    Config config2 = cf.createNew(cluster, "core-site",
        new HashMap<String, String>() {{
          put("key1", "value1");
        }}, new HashMap<String, Map<String,String>>());
    config2.setTag("version1");

    Config config3 = cf.createNew(cluster, "yarn-site",
        new HashMap<String, String>() {{
          put("test.password", "supersecret");
        }}, new HashMap<String, Map<String,String>>());
    config3.setTag("version1");

    cluster.addConfig(config1);
    cluster.addConfig(config2);
    cluster.addConfig(config3);

    Service hdfs = cluster.addService("HDFS");
    hdfs.persist();

    Service mapred = cluster.addService("YARN");
    mapred.persist();

    hdfs.addServiceComponent(Role.HDFS_CLIENT.name()).persist();
    hdfs.addServiceComponent(Role.NAMENODE.name()).persist();
    hdfs.addServiceComponent(Role.DATANODE.name()).persist();

    mapred.addServiceComponent(Role.RESOURCEMANAGER.name()).persist();

    hdfs.getServiceComponent(Role.HDFS_CLIENT.name()).addServiceComponentHost("h1").persist();
    hdfs.getServiceComponent(Role.NAMENODE.name()).addServiceComponentHost("h1").persist();
    hdfs.getServiceComponent(Role.DATANODE.name()).addServiceComponentHost("h1").persist();
    hdfs.getServiceComponent(Role.DATANODE.name()).addServiceComponentHost("h2").persist();

    ActionDefinition a1 = new ActionDefinition("a1", ActionType.SYSTEM,
        "test,[optional1]", "", "", "Does file exist", TargetHostType.SPECIFIC, Short.valueOf("100"));
    controller.getAmbariMetaInfo().addActionDefinition(a1);
    controller.getAmbariMetaInfo().addActionDefinition(new ActionDefinition(
        "a2", ActionType.SYSTEM, "", "HDFS", "DATANODE", "Does file exist",
        TargetHostType.ALL, Short.valueOf("1000")));

    Map<String, String> params = new HashMap<String, String>() {{
      put("test", "test");
      put("pwd", "SECRET:yarn-site:1:test.password");
    }};

    Map<String, String> requestProperties = new HashMap<String, String>();
    requestProperties.put(REQUEST_CONTEXT_PROPERTY, "Called from a test");
    requestProperties.put("datanode", "abc");

    ArrayList<String> hosts = new ArrayList<String>() {{add("h1");}};
    RequestResourceFilter resourceFilter = new RequestResourceFilter("HDFS", "DATANODE", hosts);
    List<RequestResourceFilter> resourceFilters = new ArrayList<RequestResourceFilter>();
    resourceFilters.add(resourceFilter);

    ExecuteActionRequest actionRequest = new ExecuteActionRequest("c1", null, "a1", resourceFilters, null, params, false);
    RequestStatusResponse response = controller.createAction(actionRequest, requestProperties);
    assertEquals(1, response.getTasks().size());
    ShortTaskStatus taskStatus = response.getTasks().get(0);
    Assert.assertEquals("h1", taskStatus.getHostName());

    List<HostRoleCommand> storedTasks = actionDB.getRequestTasks(response.getRequestId());
    Stage stage = actionDB.getAllStages(response.getRequestId()).get(0);
    Assert.assertNotNull(stage);

    Assert.assertEquals(1, storedTasks.size());
    HostRoleCommand task = storedTasks.get(0);
    Assert.assertEquals(RoleCommand.ACTIONEXECUTE, task.getRoleCommand());
    Assert.assertEquals("a1", task.getRole().name());
    Assert.assertEquals("h1", task.getHostName());
    ExecutionCommand cmd = task.getExecutionCommandWrapper().getExecutionCommand();
    // h1 has only DATANODE, NAMENODE, CLIENT sch's
    Assert.assertEquals("h1", cmd.getHostname());
    Assert.assertFalse(cmd.getLocalComponents().isEmpty());
    Assert.assertTrue(cmd.getLocalComponents().contains(Role.DATANODE.name()));
    Assert.assertTrue(cmd.getLocalComponents().contains(Role.NAMENODE.name()));
    Assert.assertTrue(cmd.getLocalComponents().contains(Role.HDFS_CLIENT.name()));
    Assert.assertFalse(cmd.getLocalComponents().contains(Role.RESOURCEMANAGER.name()));
    Type type = new TypeToken<Map<String, String>>(){}.getType();
    Map<String, String> hostParametersStage = StageUtils.getGson().fromJson(stage.getHostParamsStage(), type);
    Map<String, String> commandParametersStage = StageUtils.getGson().fromJson(stage.getCommandParamsStage(), type);

    Assert.assertTrue(commandParametersStage.containsKey("test"));
    Assert.assertTrue(commandParametersStage.containsKey("pwd"));
    Assert.assertEquals(commandParametersStage.get("pwd"), "supersecret");
    Assert.assertEquals("HDFS", cmd.getServiceName());
    Assert.assertEquals("DATANODE", cmd.getComponentName());
    Assert.assertNotNull(hostParametersStage.get("jdk_location"));
    Assert.assertEquals("900", cmd.getCommandParams().get("command_timeout"));
    Assert.assertEquals(requestProperties.get(REQUEST_CONTEXT_PROPERTY), response.getRequestContext());

    // !!! test that the action execution helper is using the right timeout
    a1.setDefaultTimeout((short) 1800);
    actionRequest = new ExecuteActionRequest("c1", null, "a1", resourceFilters, null, params, false);
    response = controller.createAction(actionRequest, requestProperties);

    List<HostRoleCommand> storedTasks1 = actionDB.getRequestTasks(response.getRequestId());
    cmd = storedTasks1.get(0).getExecutionCommandWrapper().getExecutionCommand();
    Assert.assertEquals("1800", cmd.getCommandParams().get("command_timeout"));

    resourceFilters.clear();
    resourceFilter = new RequestResourceFilter("", "", null);
    resourceFilters.add(resourceFilter);
    actionRequest = new ExecuteActionRequest("c1", null, "a2", resourceFilters, null, params, false);
    response = controller.createAction(actionRequest, requestProperties);
    assertEquals(2, response.getTasks().size());

    final List<HostRoleCommand> storedTasks2 = actionDB.getRequestTasks(response.getRequestId());
    task = storedTasks2.get(1);
    Assert.assertEquals(RoleCommand.ACTIONEXECUTE, task.getRoleCommand());
    Assert.assertEquals("a2", task.getRole().name());
    HashSet<String> expectedHosts = new HashSet<String>() {{
      add("h2");
      add("h1");
    }};
    HashSet<String> actualHosts = new HashSet<String>() {{
      add(storedTasks2.get(1).getHostName());
      add(storedTasks2.get(0).getHostName());
    }};
    Assert.assertEquals(expectedHosts, actualHosts);

    cmd = task.getExecutionCommandWrapper().getExecutionCommand();
    commandParametersStage = StageUtils.getGson().fromJson(stage.getCommandParamsStage(), type);

    Assert.assertTrue(commandParametersStage.containsKey("test"));
    Assert.assertTrue(commandParametersStage.containsKey("pwd"));
    Assert.assertEquals(commandParametersStage.get("pwd"), "supersecret");
    Assert.assertEquals("HDFS", cmd.getServiceName());
    Assert.assertEquals("DATANODE", cmd.getComponentName());
    Assert.assertEquals(requestProperties.get(REQUEST_CONTEXT_PROPERTY), response.getRequestContext());
    // h2 has only DATANODE sch
    Assert.assertEquals("h2", cmd.getHostname());
    Assert.assertFalse(cmd.getLocalComponents().isEmpty());
    Assert.assertTrue(cmd.getLocalComponents().contains(Role.DATANODE.name()));
    Assert.assertFalse(cmd.getLocalComponents().contains(Role.NAMENODE.name()));
    Assert.assertFalse(cmd.getLocalComponents().contains(Role.HDFS_CLIENT.name()));
    Assert.assertFalse(cmd.getLocalComponents().contains(Role.RESOURCEMANAGER.name()));

    hosts = new ArrayList<String>() {{add("h3");}};
    resourceFilters.clear();
    resourceFilter = new RequestResourceFilter("", "", hosts);
    resourceFilters.add(resourceFilter);

    actionRequest = new ExecuteActionRequest("c1", null, "a1", resourceFilters, null, params, false);
    response = controller.createAction(actionRequest, requestProperties);
    assertEquals(1, response.getTasks().size());
    taskStatus = response.getTasks().get(0);
    Assert.assertEquals("h3", taskStatus.getHostName());
    Assert.assertEquals(requestProperties.get(REQUEST_CONTEXT_PROPERTY), response.getRequestContext());
  }

  @Test
  public void testComponentCategorySentWithRestart() throws AmbariException {
    setupClusterWithHosts("c1", "HDP-2.0.7",
      new ArrayList<String>() {{
        add("h1");
      }},
      "centos5");

    Cluster cluster = clusters.getCluster("c1");
    cluster.setDesiredStackVersion(new StackId("HDP-2.0.7"));
    cluster.setCurrentStackVersion(new StackId("HDP-2.0.7"));

    ConfigFactory cf = injector.getInstance(ConfigFactory.class);
    Config config1 = cf.createNew(cluster, "global",
      new HashMap<String, String>() {{
        put("key1", "value1");
      }}, new HashMap<String, Map<String,String>>());
    config1.setTag("version1");

    Config config2 = cf.createNew(cluster, "core-site",
      new HashMap<String, String>() {{
        put("key1", "value1");
      }}, new HashMap<String, Map<String,String>>());
    config2.setTag("version1");

    cluster.addConfig(config1);
    cluster.addConfig(config2);

    Service hdfs = cluster.addService("HDFS");
    hdfs.persist();

    hdfs.addServiceComponent(Role.HDFS_CLIENT.name()).persist();
    hdfs.addServiceComponent(Role.NAMENODE.name()).persist();
    hdfs.addServiceComponent(Role.DATANODE.name()).persist();

    hdfs.getServiceComponent(Role.HDFS_CLIENT.name()).addServiceComponentHost("h1").persist();
    hdfs.getServiceComponent(Role.NAMENODE.name()).addServiceComponentHost("h1").persist();
    hdfs.getServiceComponent(Role.DATANODE.name()).addServiceComponentHost("h1").persist();

    installService("c1", "HDFS", false, false);

    startService("c1", "HDFS", false, false);

    Cluster c = clusters.getCluster("c1");
    Service s = c.getService("HDFS");

    Assert.assertEquals(State.STARTED, s.getDesiredState());
    for (ServiceComponent sc : s.getServiceComponents().values()) {
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        if (sc.isClientComponent()) {
          Assert.assertEquals(State.INSTALLED, sch.getDesiredState());
        } else {
          Assert.assertEquals(State.STARTED, sch.getDesiredState());
        }
      }
    }

    Map<String, String> params = new HashMap<String, String>() {{
      put("test", "test");
    }};
    RequestResourceFilter resourceFilter = new RequestResourceFilter(
      "HDFS",
      "HDFS_CLIENT",
      new ArrayList<String>() {{ add("h1"); }});
    ExecuteActionRequest actionRequest = new ExecuteActionRequest("c1",
      "RESTART", params, false);
    actionRequest.getResourceFilters().add(resourceFilter);

    Map<String, String> requestProperties = new HashMap<String, String>();
    requestProperties.put(REQUEST_CONTEXT_PROPERTY, "Called from a test");
    requestProperties.put("hdfs_client", "abc");

    RequestStatusResponse response = controller.createAction(actionRequest, requestProperties);

    List<Stage> stages = actionDB.getAllStages(response.getRequestId());
    Assert.assertNotNull(stages);

    HostRoleCommand hrc = null;
    Type type = new TypeToken<Map<String, String>>(){}.getType();
    for (Stage stage : stages) {
      for (HostRoleCommand cmd : stage.getOrderedHostRoleCommands()) {
        if (cmd.getRole().equals(Role.HDFS_CLIENT)) {
          hrc = cmd;
        }
        Map<String, String> hostParamStage = StageUtils.getGson().fromJson(stage.getHostParamsStage(), type);
        Assert.assertTrue(hostParamStage.containsKey(ExecutionCommand.KeyNames.DB_DRIVER_FILENAME));
        Assert.assertTrue(hostParamStage.containsKey(ExecutionCommand.KeyNames.MYSQL_JDBC_URL));
        Assert.assertTrue(hostParamStage.containsKey(ExecutionCommand.KeyNames.ORACLE_JDBC_URL));
      }
    }
    Assert.assertNotNull(hrc);
    Assert.assertEquals("RESTART HDFS/HDFS_CLIENT", hrc.getCommandDetail());
    Map<String, String> roleParams = hrc.getExecutionCommandWrapper()
      .getExecutionCommand().getRoleParams();

    Assert.assertNotNull(roleParams);
    Assert.assertEquals("CLIENT", roleParams.get(ExecutionCommand.KeyNames.COMPONENT_CATEGORY));
    Assert.assertTrue(hrc.getExecutionCommandWrapper().getExecutionCommand().getCommandParams().containsKey("hdfs_client"));
    Assert.assertEquals("abc", hrc.getExecutionCommandWrapper().getExecutionCommand().getCommandParams().get("hdfs_client"));
    Assert.assertEquals(requestProperties.get(REQUEST_CONTEXT_PROPERTY), response.getRequestContext());
  }

  @SuppressWarnings("serial")
  @Test
  public void testCreateActionsFailures() throws Exception {
    setupClusterWithHosts("c1", "HDP-2.0.7",
        new ArrayList<String>() {{
          add("h1");
        }},
        "centos5");

    Cluster cluster = clusters.getCluster("c1");
    cluster.setDesiredStackVersion(new StackId("HDP-2.0.7"));
    cluster.setCurrentStackVersion(new StackId("HDP-2.0.7"));

    ConfigFactory cf = injector.getInstance(ConfigFactory.class);
    Config config1 = cf.createNew(cluster, "global",
        new HashMap<String, String>() {{
          put("key1", "value1");
        }}, new HashMap<String, Map<String,String>>());
    config1.setTag("version1");

    Config config2 = cf.createNew(cluster, "core-site",
        new HashMap<String, String>() {{
          put("key1", "value1");
        }}, new HashMap<String, Map<String,String>>());
    config2.setTag("version1");
    config1.persist();
    config2.persist();

    cluster.addConfig(config1);
    cluster.addConfig(config2);
    cluster.addDesiredConfig("_test", Collections.singleton(config1));
    cluster.addDesiredConfig("_test", Collections.singleton(config2));

    Service hdfs = cluster.addService("HDFS");
    hdfs.persist();

    Service hive = cluster.addService("HIVE");
    hive.persist();

    hdfs.addServiceComponent(Role.HDFS_CLIENT.name()).persist();
    hdfs.addServiceComponent(Role.NAMENODE.name()).persist();
    hdfs.addServiceComponent(Role.DATANODE.name()).persist();

    hive.addServiceComponent(Role.HIVE_SERVER.name()).persist();

    hdfs.getServiceComponent(Role.HDFS_CLIENT.name()).addServiceComponentHost("h1").persist();
    hdfs.getServiceComponent(Role.NAMENODE.name()).addServiceComponentHost("h1").persist();
    hdfs.getServiceComponent(Role.DATANODE.name()).addServiceComponentHost("h1").persist();

    Map<String, String> params = new HashMap<String, String>() {{
      put("test", "test");
    }};

    RequestResourceFilter resourceFilter = new RequestResourceFilter("HDFS", null, null);

    ExecuteActionRequest actionRequest = new ExecuteActionRequest("c1", "NON_EXISTENT_CHECK", params, false);
    actionRequest.getResourceFilters().add(resourceFilter);

    Map<String, String> requestProperties = new HashMap<String, String>();
    requestProperties.put(REQUEST_CONTEXT_PROPERTY, "Called from a test");

    expectActionCreationErrorWithMessage(actionRequest, requestProperties, "Unsupported action");

    //actionRequest = new ExecuteActionRequest("c1", "NON_EXISTENT_SERVICE_CHECK", "HDFS", params);
    //expectActionCreationErrorWithMessage(actionRequest, requestProperties, "Unsupported action");

    actionRequest = new ExecuteActionRequest("c1", "DECOMMISSION_DATANODE", params, false);
    actionRequest.getResourceFilters().add(resourceFilter);

    expectActionCreationErrorWithMessage(actionRequest, requestProperties,
      "Unsupported action DECOMMISSION_DATANODE for Service: HDFS and Component: null");

    //actionRequest = new ExecuteActionRequest("c1", "DECOMMISSION", "HDFS", params);
    //expectActionCreationErrorWithMessage(actionRequest, requestProperties, "Unsupported action DECOMMISSION for Service: HDFS and Component: null");

    resourceFilter = new RequestResourceFilter("HDFS", "HDFS_CLIENT", null);
    List<RequestResourceFilter> resourceFilters = new ArrayList<RequestResourceFilter>();
    resourceFilters.add(resourceFilter);

    actionRequest = new ExecuteActionRequest("c1", "DECOMMISSION", null, resourceFilters, null, params, false);

    expectActionCreationErrorWithMessage(actionRequest, requestProperties,
        "Unsupported action DECOMMISSION for Service: HDFS and Component: HDFS_CLIENT");

    resourceFilters.clear();
    resourceFilter = new RequestResourceFilter("HDFS", null, null);
    resourceFilters.add(resourceFilter);
    actionRequest = new ExecuteActionRequest("c1", null, "DECOMMISSION_DATANODE", resourceFilters, null, params, false);
    expectActionCreationErrorWithMessage(actionRequest, requestProperties,
        "Action DECOMMISSION_DATANODE does not exist");

    resourceFilters.clear();
    resourceFilter = new RequestResourceFilter("YARN", "RESOURCEMANAGER", null);
    resourceFilters.add(resourceFilter);

    actionRequest = new ExecuteActionRequest("c1", "DECOMMISSION", null, resourceFilters, null, params, false);
    expectActionCreationErrorWithMessage(actionRequest, requestProperties,
        "Service not found, clusterName=c1, serviceName=YARN");

    Map<String, String> params2 = new HashMap<String, String>() {{
      put("included_hosts", "h1,h2");
      put("excluded_hosts", "h1,h3");
    }};

    resourceFilters.clear();
    resourceFilter = new RequestResourceFilter("HDFS", "NAMENODE", null);
    resourceFilters.add(resourceFilter);

    actionRequest = new ExecuteActionRequest("c1", "DECOMMISSION", null, resourceFilters, null, params2, false);
    expectActionCreationErrorWithMessage(actionRequest, requestProperties,
        "Same host cannot be specified for inclusion as well as exclusion. Hosts: [h1]");

    params2 = new HashMap<String, String>() {{
      put("included_hosts", " h1,h2");
      put("excluded_hosts", "h4, h3");
      put("slave_type", "HDFS_CLIENT");
    }};
    resourceFilters.clear();
    resourceFilter = new RequestResourceFilter("HDFS", "NAMENODE", null);
    resourceFilters.add(resourceFilter);

    actionRequest = new ExecuteActionRequest("c1", "DECOMMISSION", null, resourceFilters, null, params2, false);
    expectActionCreationErrorWithMessage(actionRequest, requestProperties,
        "Component HDFS_CLIENT is not supported for decommissioning.");

    List<String> hosts = new ArrayList<String>();
    hosts.add("h6");
    resourceFilters.clear();
    resourceFilter = new RequestResourceFilter("HDFS", "NAMENODE", hosts);
    resourceFilters.add(resourceFilter);

    actionRequest = new ExecuteActionRequest("c1", "DECOMMISSION", null, resourceFilters, null, params2, false);
    expectActionCreationErrorWithMessage(actionRequest, requestProperties,
        "Decommission command cannot be issued with target host(s) specified.");

    hdfs.getServiceComponent(Role.DATANODE.name()).getServiceComponentHost("h1").setState(State.INSTALLED);
    params2 = new HashMap<String, String>() {{
      put("excluded_hosts", "h1 ");
    }};
    resourceFilters.clear();
    resourceFilter = new RequestResourceFilter("HDFS", "NAMENODE", null);
    resourceFilters.add(resourceFilter);

    actionRequest = new ExecuteActionRequest("c1", "DECOMMISSION", null, resourceFilters, null, params2, false);
    expectActionCreationErrorWithMessage(actionRequest, requestProperties,
        "Component DATANODE on host h1 cannot be decommissioned as its not in STARTED state");

    params2 = new HashMap<String, String>() {{
      put("excluded_hosts", "h1 ");
      put("mark_draining_only", "true");
    }};
    actionRequest = new ExecuteActionRequest("c1", "DECOMMISSION", null, resourceFilters, null, params2, false);
    expectActionCreationErrorWithMessage(actionRequest, requestProperties,
        "mark_draining_only is not a valid parameter for NAMENODE");

    controller.getAmbariMetaInfo().addActionDefinition(new ActionDefinition(
        "a1", ActionType.SYSTEM, "test,dirName", "", "", "Does file exist",
        TargetHostType.SPECIFIC, Short.valueOf("100")));

    controller.getAmbariMetaInfo().addActionDefinition(new ActionDefinition(
        "a2", ActionType.SYSTEM, "", "HDFS", "DATANODE", "Does file exist",
        TargetHostType.ANY, Short.valueOf("100")));

    controller.getAmbariMetaInfo().addActionDefinition(new ActionDefinition(
            "update_repo", ActionType.SYSTEM, "", "HDFS", "DATANODE", "Does file exist",
            TargetHostType.ANY, Short.valueOf("100")));

    controller.getAmbariMetaInfo().addActionDefinition(new ActionDefinition(
        "a3", ActionType.SYSTEM, "", "MAPREDUCE", "MAPREDUCE_CLIENT", "Does file exist",
        TargetHostType.ANY, Short.valueOf("100")));

    controller.getAmbariMetaInfo().addActionDefinition(new ActionDefinition(
        "a4", ActionType.SYSTEM, "", "HIVE", "", "Does file exist",
        TargetHostType.ANY, Short.valueOf("100")));

    actionRequest = new ExecuteActionRequest("c1", null, "a1", null, null, null, false);
    expectActionCreationErrorWithMessage(actionRequest, requestProperties,
        "Action a1 requires input 'test' that is not provided");

    actionRequest = new ExecuteActionRequest("c1", null, "a1", null, null, params, false);
    expectActionCreationErrorWithMessage(actionRequest, requestProperties,
        "Action a1 requires input 'dirName' that is not provided");

    params.put("dirName", "dirName");
    actionRequest = new ExecuteActionRequest("c1", null, "a1", null, null, params, false);
    expectActionCreationErrorWithMessage(actionRequest, requestProperties,
        "Action a1 requires explicit target host(s)");

    resourceFilters.clear();
    resourceFilter = new RequestResourceFilter("HIVE", null, null);
    resourceFilters.add(resourceFilter);

    actionRequest = new ExecuteActionRequest("c1", null, "a2", resourceFilters, null, params, false);
    expectActionCreationErrorWithMessage(actionRequest, requestProperties,
        "Action a2 targets service HIVE that does not match with expected HDFS");

    resourceFilters.clear();
    resourceFilter = new RequestResourceFilter("HDFS", "HDFS_CLIENT", null);
    resourceFilters.add(resourceFilter);

    actionRequest = new ExecuteActionRequest("c1", null, "a2", resourceFilters, null, params, false);
    expectActionCreationErrorWithMessage(actionRequest, requestProperties,
        "Action a2 targets component HDFS_CLIENT that does not match with expected DATANODE");

    resourceFilters.clear();
    resourceFilter = new RequestResourceFilter("HDFS2", "HDFS_CLIENT", null);
    resourceFilters.add(resourceFilter);

    actionRequest = new ExecuteActionRequest("c1", null, "a1", resourceFilters, null, params, false);
    expectActionCreationErrorWithMessage(actionRequest, requestProperties,
        "Action a1 targets service HDFS2 that does not exist");

    resourceFilters.clear();
    resourceFilter = new RequestResourceFilter("HDFS", "HDFS_CLIENT2", null);
    resourceFilters.add(resourceFilter);

    actionRequest = new ExecuteActionRequest("c1", null, "a1", resourceFilters, null, params, false);
    expectActionCreationErrorWithMessage(actionRequest, requestProperties,
        "Action a1 targets component HDFS_CLIENT2 that does not exist");

    resourceFilters.clear();
    resourceFilter = new RequestResourceFilter("", "HDFS_CLIENT2", null);
    resourceFilters.add(resourceFilter);

    actionRequest = new ExecuteActionRequest("c1", null, "a1", resourceFilters, null, params, false);
    expectActionCreationErrorWithMessage(actionRequest, requestProperties,
        "Action a1 targets component HDFS_CLIENT2 without specifying the target service");

    resourceFilters.clear();
    resourceFilter = new RequestResourceFilter("", "", null);
    resourceFilters.add(resourceFilter);

    // targets a service that is not a member of the stack (e.g. MR not in HDP-2)
    actionRequest = new ExecuteActionRequest("c1", null, "a3", resourceFilters, null, params, false);
    expectActionCreationErrorWithMessage(actionRequest, requestProperties,
        "Action a3 targets service MAPREDUCE that does not exist");

    hosts = new ArrayList<String>();
    hosts.add("h6");
    resourceFilters.clear();
    resourceFilter = new RequestResourceFilter("", "", hosts);
    resourceFilters.add(resourceFilter);

    actionRequest = new ExecuteActionRequest("c1", null, "a2", resourceFilters, null, params, false);
    expectActionCreationErrorWithMessage(actionRequest, requestProperties,
        "Request specifies host h6 but it is not a valid host based on the target service=HDFS and component=DATANODE");

    hosts.clear();
    hosts.add("h1");
    resourceFilters.clear();
    resourceFilter = new RequestResourceFilter("", "", hosts);
    resourceFilters.add(resourceFilter);
    params.put("success_factor", "1r");
    actionRequest = new ExecuteActionRequest("c1", null, "update_repo", resourceFilters, null, params, false);
    expectActionCreationErrorWithMessage(actionRequest, requestProperties,
            "Failed to cast success_factor value to float!");

    resourceFilters.clear();
    resourceFilter = new RequestResourceFilter("HIVE", "", null);
    resourceFilters.add(resourceFilter);

    actionRequest = new ExecuteActionRequest("c1", null, "a4", resourceFilters, null, params, false);
    expectActionCreationErrorWithMessage(actionRequest, requestProperties,
        "Suitable hosts not found, component=, service=HIVE, cluster=c1, actionName=a4");

  }

  private void expectActionCreationErrorWithMessage(ExecuteActionRequest actionRequest,
                                                    Map<String, String> requestProperties,
                                                    String message) {
    try {
      RequestStatusResponse response = controller.createAction(actionRequest, requestProperties);
      Assert.fail("createAction should fail");
    } catch (AmbariException ex) {
      LOG.info(ex.getMessage());
      Assert.assertTrue(ex.getMessage().contains(message));
    }
  }

  @SuppressWarnings("serial")
  @Test
  public void testCreateServiceCheckActions() throws Exception {
    setupClusterWithHosts("c1", "HDP-0.1",
        new ArrayList<String>() {{
          add("h1");
          add("h2");
        }},
        "centos5");

    Cluster cluster = clusters.getCluster("c1");
    cluster.setDesiredStackVersion(new StackId("HDP-0.1"));
    cluster.setCurrentStackVersion(new StackId("HDP-0.1"));

    ConfigFactory cf = injector.getInstance(ConfigFactory.class);
    Config config1 = cf.createNew(cluster, "global",
        new HashMap<String, String>(){{ put("key1", "value1"); }}, new HashMap<String, Map<String,String>>());
    config1.setTag("version1");
    config1.setPropertiesAttributes(new HashMap<String, Map<String, String>>(){{ put("attr1", new HashMap<String, String>()); }});

    Config config2 = cf.createNew(cluster, "core-site",
        new HashMap<String, String>(){{ put("key1", "value1"); }}, new HashMap<String, Map<String,String>>());
    config2.setTag("version1");
    config2.setPropertiesAttributes(new HashMap<String, Map<String, String>>(){{ put("attr2", new HashMap<String, String>()); }});

    cluster.addConfig(config1);
    cluster.addConfig(config2);
    cluster.addDesiredConfig("_test", Collections.singleton(config1));
    cluster.addDesiredConfig("_test", Collections.singleton(config2));

    Service hdfs = cluster.addService("HDFS");
    Service mapReduce = cluster.addService("MAPREDUCE");
    hdfs.persist();
    mapReduce.persist();

    hdfs.addServiceComponent(Role.HDFS_CLIENT.name()).persist();
    mapReduce.addServiceComponent(Role.MAPREDUCE_CLIENT.name()).persist();

    hdfs.getServiceComponent(Role.HDFS_CLIENT.name()).addServiceComponentHost("h1").persist();
    mapReduce.getServiceComponent(Role.MAPREDUCE_CLIENT.name()).addServiceComponentHost("h2").persist();

    Map<String, String> params = new HashMap<String, String>() {{
      put("test", "test");
    }};
    ExecuteActionRequest actionRequest = new ExecuteActionRequest("c1", Role.HDFS_SERVICE_CHECK.name(), params, false);
    RequestResourceFilter resourceFilter = new RequestResourceFilter("HDFS", null, null);
    actionRequest.getResourceFilters().add(resourceFilter);

    Map<String, String> requestProperties = new HashMap<String, String>();
    requestProperties.put(REQUEST_CONTEXT_PROPERTY, "Called from a test");

    RequestStatusResponse response = controller.createAction(actionRequest, requestProperties);

    assertEquals(1, response.getTasks().size());
    ShortTaskStatus task = response.getTasks().get(0);

    List<HostRoleCommand> storedTasks = actionDB.getRequestTasks(response.getRequestId());
    Stage stage = actionDB.getAllStages(response.getRequestId()).get(0);

    //Check configs not stored with execution command
    ExecutionCommandDAO executionCommandDAO = injector.getInstance(ExecutionCommandDAO.class);
    ExecutionCommandEntity commandEntity = executionCommandDAO.findByPK(task.getTaskId());

    Gson gson = new Gson();
    ExecutionCommand executionCommand = gson.fromJson(new StringReader(
        new String(commandEntity.getCommand())), ExecutionCommand.class);

    assertFalse(executionCommand.getConfigurationTags().isEmpty());
    assertTrue(executionCommand.getConfigurations() == null || executionCommand.getConfigurations().isEmpty());

    assertEquals(1, storedTasks.size());
    HostRoleCommand hostRoleCommand = storedTasks.get(0);

    assertEquals("SERVICE_CHECK HDFS", hostRoleCommand.getCommandDetail());
    assertNull(hostRoleCommand.getCustomCommandName());

    assertEquals(task.getTaskId(), hostRoleCommand.getTaskId());
    assertNotNull(actionRequest.getResourceFilters());
    RequestResourceFilter requestResourceFilter = actionRequest.getResourceFilters().get(0);
    assertEquals(resourceFilter.getServiceName(), hostRoleCommand.getExecutionCommandWrapper().getExecutionCommand().getServiceName());
    assertEquals(actionRequest.getClusterName(), hostRoleCommand.getExecutionCommandWrapper().getExecutionCommand().getClusterName());
    assertEquals(actionRequest.getCommandName(), hostRoleCommand.getExecutionCommandWrapper().getExecutionCommand().getRole());
    assertEquals(Role.HDFS_CLIENT.name(), hostRoleCommand.getEvent().getEvent().getServiceComponentName());
    assertEquals(actionRequest.getParameters(), hostRoleCommand.getExecutionCommandWrapper().getExecutionCommand().getRoleParams());
    assertNotNull(hostRoleCommand.getExecutionCommandWrapper().getExecutionCommand().getConfigurations());
    assertEquals(2, hostRoleCommand.getExecutionCommandWrapper().getExecutionCommand().getConfigurations().size());
    assertEquals(requestProperties.get(REQUEST_CONTEXT_PROPERTY), stage.getRequestContext());
    assertEquals(requestProperties.get(REQUEST_CONTEXT_PROPERTY), response.getRequestContext());

    actionRequest = new ExecuteActionRequest("c1", Role.MAPREDUCE_SERVICE_CHECK.name(), null, false);
    resourceFilter = new RequestResourceFilter("MAPREDUCE", null, null);
    actionRequest.getResourceFilters().add(resourceFilter);

    injector.getInstance(ActionMetadata.class).addServiceCheckAction("MAPREDUCE");
    response = controller.createAction(actionRequest, requestProperties);

    assertEquals(1, response.getTasks().size());

    List<HostRoleCommand> tasks = actionDB.getRequestTasks(response.getRequestId());

    assertEquals(1, tasks.size());

    requestProperties.put(REQUEST_CONTEXT_PROPERTY, null);
    response = controller.createAction(actionRequest, requestProperties);

    assertEquals(1, response.getTasks().size());
    assertEquals("", response.getRequestContext());
  }

  private void createUser(String userName) throws Exception {
    UserRequest request = new UserRequest(userName);
    request.setPassword("password");

    controller.createUsers(new HashSet<UserRequest>(Collections.singleton(request)));
  }

  @Test
  public void testCreateAndGetUsers() throws Exception {
    createUser("user1");

    Set<UserResponse> r =
        controller.getUsers(Collections.singleton(new UserRequest("user1")));

    Assert.assertEquals(1, r.size());
    UserResponse resp = r.iterator().next();
    Assert.assertEquals("user1", resp.getUsername());
  }

  @Test
  public void testGetUsers() throws Exception {
    createUser("user1");
    createUser("user2");
    createUser("user3");

    UserRequest request = new UserRequest(null);

    Set<UserResponse> responses = controller.getUsers(Collections.singleton(request));

    Assert.assertEquals(3, responses.size());
  }

  @SuppressWarnings("serial")
  @Test
  public void testUpdateUsers() throws Exception {
    createUser("user1");

    UserRequest request = new UserRequest("user1");

    controller.updateUsers(Collections.singleton(request));
  }

  @SuppressWarnings("serial")
  @Ignore
  @Test
  public void testDeleteUsers() throws Exception {
    createUser("user1");

    UserRequest request = new UserRequest("user1");
    controller.updateUsers(Collections.singleton(request));

    request = new UserRequest("user1");
    controller.deleteUsers(Collections.singleton(request));

    Set<UserResponse> responses = controller.getUsers(
        Collections.singleton(new UserRequest(null)));

    Assert.assertEquals(0, responses.size());
  }

  @Test
  public void testUpdateConfigForRunningService() throws Exception {
    String clusterName = "foo1";
    createCluster(clusterName);
    clusters.getCluster(clusterName)
            .setDesiredStackVersion(new StackId("HDP-0.1"));
    String serviceName = "HDFS";
    createService(clusterName, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";

    Map<String, String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "Called from a test");

    createServiceComponent(clusterName, serviceName, componentName1,
            State.INIT);
    createServiceComponent(clusterName, serviceName, componentName2,
            State.INIT);
    createServiceComponent(clusterName, serviceName, componentName3,
            State.INIT);

    String host1 = "h1";
    String host2 = "h2";

    addHostToCluster(host1, clusterName);
    addHostToCluster(host2, clusterName);


    // null service should work
    createServiceComponentHost(clusterName, null, componentName1,
            host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName2,
            host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName2,
            host2, null);
    createServiceComponentHost(clusterName, serviceName, componentName3,
            host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName3,
            host2, null);

    Assert.assertNotNull(clusters.getCluster(clusterName)
            .getService(serviceName)
            .getServiceComponent(componentName1)
            .getServiceComponentHost(host1));
    Assert.assertNotNull(clusters.getCluster(clusterName)
            .getService(serviceName)
            .getServiceComponent(componentName2)
            .getServiceComponentHost(host1));
    Assert.assertNotNull(clusters.getCluster(clusterName)
            .getService(serviceName)
            .getServiceComponent(componentName2)
            .getServiceComponentHost(host2));
    Assert.assertNotNull(clusters.getCluster(clusterName)
            .getService(serviceName)
            .getServiceComponent(componentName3)
            .getServiceComponentHost(host1));
    Assert.assertNotNull(clusters.getCluster(clusterName)
            .getService(serviceName)
            .getServiceComponent(componentName3)
            .getServiceComponentHost(host2));

    // Install
    ServiceRequest r = new ServiceRequest(clusterName, serviceName,
        State.INSTALLED.toString());
    Set<ServiceRequest> requests = new HashSet<ServiceRequest>();
    requests.add(r);

    ServiceResourceProviderTest.updateServices(controller, requests, mapRequestProps, true, false);
    Assert.assertEquals(State.INSTALLED,
            clusters.getCluster(clusterName).getService(serviceName)
                    .getDesiredState());

    // manually change live state to installed as no running action manager
    for (ServiceComponent sc :
            clusters.getCluster(clusterName).getService(serviceName)
                    .getServiceComponents().values()) {
        for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
            sch.setState(State.INSTALLED);
        }
    }

    // Start
    r = new ServiceRequest(clusterName, serviceName,
            State.STARTED.toString());
    requests.clear();
    requests.add(r);
    ServiceResourceProviderTest.updateServices(controller, requests, mapRequestProps, true, false);

    // manually change live state to started as no running action manager
    for (ServiceComponent sc :
            clusters.getCluster(clusterName).getService(serviceName)
                    .getServiceComponents().values()) {
        for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
            sch.setState(State.STARTED);
        }
    }

    Assert.assertEquals(State.STARTED,
            clusters.getCluster(clusterName).getService(serviceName)
                    .getDesiredState());
    for (ServiceComponent sc :
            clusters.getCluster(clusterName).getService(serviceName)
                    .getServiceComponents().values()) {
        if (sc.getName().equals("HDFS_CLIENT")) {
            Assert.assertEquals(State.INSTALLED, sc.getDesiredState());
        } else {
            Assert.assertEquals(State.STARTED, sc.getDesiredState());
        }
        for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
            if (sch.getServiceComponentName().equals("HDFS_CLIENT")) {
                Assert.assertEquals(State.INSTALLED, sch.getDesiredState());
            } else {
                Assert.assertEquals(State.STARTED, sch.getDesiredState());
            }
        }
    }

    Map<String, String> configs = new HashMap<String, String>();
    configs.put("a", "b");

    ConfigurationRequest cr1, cr2, cr3, cr4, cr5, cr6, cr7, cr8;
    cr1 = new ConfigurationRequest(clusterName, "typeA","v1", configs, null);
    cr2 = new ConfigurationRequest(clusterName, "typeB","v1", configs, null);
    cr3 = new ConfigurationRequest(clusterName, "typeC","v1", configs, null);
    cr4 = new ConfigurationRequest(clusterName, "typeD","v1", configs, null);
    cr5 = new ConfigurationRequest(clusterName, "typeA","v2", configs, null);
    cr6 = new ConfigurationRequest(clusterName, "typeB","v2", configs, null);
    cr7 = new ConfigurationRequest(clusterName, "typeC","v2", configs, null);
    cr8 = new ConfigurationRequest(clusterName, "typeE","v1", configs, null);
    controller.createConfiguration(cr1);
    controller.createConfiguration(cr2);
    controller.createConfiguration(cr3);
    controller.createConfiguration(cr4);
    controller.createConfiguration(cr5);
    controller.createConfiguration(cr6);
    controller.createConfiguration(cr7);
    controller.createConfiguration(cr8);

    Cluster cluster = clusters.getCluster(clusterName);
    Service s = cluster.getService(serviceName);
    ServiceComponent sc1 = s.getServiceComponent(componentName1);
    ServiceComponent sc2 = s.getServiceComponent(componentName2);
    ServiceComponentHost sch1 = sc1.getServiceComponentHost(host1);

    Set<ServiceComponentHostRequest> schReqs =
            new HashSet<ServiceComponentHostRequest>();
    Set<ServiceComponentRequest> scReqs =
            new HashSet<ServiceComponentRequest>();
    Set<ServiceRequest> sReqs = new HashSet<ServiceRequest>();
    Map<String, String> configVersions = new HashMap<String, String>();

    // update configs at SCH and SC level
    configVersions.clear();
    configVersions.put("typeA", "v1");
    configVersions.put("typeB", "v1");
    configVersions.put("typeC", "v1");
    schReqs.clear();
    schReqs.add(new ServiceComponentHostRequest(clusterName, serviceName,
            componentName1, host1, null));
    Assert.assertNull(updateHostComponents(schReqs, Collections.<String, String>emptyMap(), true));

    configVersions.clear();
    configVersions.put("typeC", "v1");
    configVersions.put("typeD", "v1");
    scReqs.clear();
    scReqs.add(new ServiceComponentRequest(clusterName, serviceName, componentName2, null));
    Assert.assertNull(ComponentResourceProviderTest.updateComponents(controller, scReqs, Collections.<String, String>emptyMap(), true));

    // update configs at service level
    configVersions.clear();
    configVersions.put("typeA", "v2");
    configVersions.put("typeC", "v2");
    configVersions.put("typeE", "v1");
    sReqs.clear();
    sReqs.add(new ServiceRequest(clusterName, serviceName, null));
    Assert.assertNull(ServiceResourceProviderTest.updateServices(controller, sReqs, mapRequestProps, true, false));


    // update configs at SCH level
    configVersions.clear();
    configVersions.put("typeA", "v1");
    configVersions.put("typeB", "v1");
    configVersions.put("typeC", "v1");
    schReqs.clear();
    schReqs.add(new ServiceComponentHostRequest(clusterName, serviceName,
            componentName1, host1, null));
    Assert.assertNull(updateHostComponents(schReqs, Collections.<String, String>emptyMap(), true));

    // update configs at SC level
    configVersions.clear();
    configVersions.put("typeC", "v2");
    configVersions.put("typeD", "v1");
    scReqs.clear();
    scReqs.add(new ServiceComponentRequest(clusterName, serviceName,
            componentName1, null));
    Assert.assertNull(ComponentResourceProviderTest.updateComponents(controller, scReqs, Collections.<String, String>emptyMap(), true));

  }

  @Test
  public void testConfigUpdates() throws Exception {
    String clusterName = "foo1";
    createCluster(clusterName);
    clusters.getCluster(clusterName)
        .setDesiredStackVersion(new StackId("HDP-0.1"));
    String serviceName = "HDFS";
    createService(clusterName, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";

    Map<String, String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "Called from a test");

    createServiceComponent(clusterName, serviceName, componentName1,
        State.INIT);
    createServiceComponent(clusterName, serviceName, componentName2,
        State.INIT);
    createServiceComponent(clusterName, serviceName, componentName3,
        State.INIT);

    String host1 = "h1";
    String host2 = "h2";

    addHostToCluster(host1, clusterName);
    addHostToCluster(host2, clusterName);


    // null service should work
    createServiceComponentHost(clusterName, null, componentName1,
        host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName2,
        host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName2,
        host2, null);
    createServiceComponentHost(clusterName, serviceName, componentName3,
        host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName3,
        host2, null);

    Assert.assertNotNull(clusters.getCluster(clusterName)
        .getService(serviceName)
        .getServiceComponent(componentName1)
        .getServiceComponentHost(host1));
    Assert.assertNotNull(clusters.getCluster(clusterName)
        .getService(serviceName)
        .getServiceComponent(componentName2)
        .getServiceComponentHost(host1));
    Assert.assertNotNull(clusters.getCluster(clusterName)
        .getService(serviceName)
        .getServiceComponent(componentName2)
        .getServiceComponentHost(host2));
    Assert.assertNotNull(clusters.getCluster(clusterName)
        .getService(serviceName)
        .getServiceComponent(componentName3)
        .getServiceComponentHost(host1));
    Assert.assertNotNull(clusters.getCluster(clusterName)
        .getService(serviceName)
        .getServiceComponent(componentName3)
        .getServiceComponentHost(host2));

    Map<String, String> configs = new HashMap<String, String>();
    configs.put("a", "b");

    Map<String, Map<String, String>> configAttributes = new HashMap<String, Map<String,String>>();
    configAttributes.put("final", new HashMap<String, String>());
    configAttributes.get("final").put("a", "true");

    ConfigurationRequest cr1, cr2, cr3, cr4, cr5, cr6, cr7, cr8;
    cr1 = new ConfigurationRequest(clusterName, "typeA","v1", configs, configAttributes);
    cr2 = new ConfigurationRequest(clusterName, "typeB","v1", configs, configAttributes);
    cr3 = new ConfigurationRequest(clusterName, "typeC","v1", configs, configAttributes);
    cr4 = new ConfigurationRequest(clusterName, "typeD","v1", configs, configAttributes);
    cr5 = new ConfigurationRequest(clusterName, "typeA","v2", configs, configAttributes);
    cr6 = new ConfigurationRequest(clusterName, "typeB","v2", configs, configAttributes);
    cr7 = new ConfigurationRequest(clusterName, "typeC","v2", configs, configAttributes);
    cr8 = new ConfigurationRequest(clusterName, "typeE","v1", configs, configAttributes);
    controller.createConfiguration(cr1);
    controller.createConfiguration(cr2);
    controller.createConfiguration(cr3);
    controller.createConfiguration(cr4);
    controller.createConfiguration(cr5);
    controller.createConfiguration(cr6);
    controller.createConfiguration(cr7);
    controller.createConfiguration(cr8);

    Cluster cluster = clusters.getCluster(clusterName);
    Service s = cluster.getService(serviceName);
    ServiceComponent sc1 = s.getServiceComponent(componentName1);
    ServiceComponent sc2 = s.getServiceComponent(componentName2);
    ServiceComponentHost sch1 = sc1.getServiceComponentHost(host1);

    Set<ServiceComponentHostRequest> schReqs =
        new HashSet<ServiceComponentHostRequest>();
    Set<ServiceComponentRequest> scReqs =
        new HashSet<ServiceComponentRequest>();
    Set<ServiceRequest> sReqs = new HashSet<ServiceRequest>();
    Map<String, String> configVersions = new HashMap<String, String>();

    // update configs at SCH and SC level
    configVersions.clear();
    configVersions.put("typeA", "v1");
    configVersions.put("typeB", "v1");
    configVersions.put("typeC", "v1");
    schReqs.clear();
    schReqs.add(new ServiceComponentHostRequest(clusterName, serviceName,
        componentName1, host1, null));
    Assert.assertNull(updateHostComponents(schReqs, Collections.<String, String>emptyMap(), true));

    configVersions.clear();
    configVersions.put("typeC", "v1");
    configVersions.put("typeD", "v1");
    scReqs.clear();
    scReqs.add(new ServiceComponentRequest(clusterName, serviceName,
        componentName2, null));
    Assert.assertNull(ComponentResourceProviderTest.updateComponents(controller, scReqs, Collections.<String, String>emptyMap(), true));

    // update configs at service level
    configVersions.clear();
    configVersions.put("typeA", "v2");
    configVersions.put("typeC", "v2");
    configVersions.put("typeE", "v1");
    sReqs.clear();
    sReqs.add(new ServiceRequest(clusterName, serviceName, null));
    Assert.assertNull(ServiceResourceProviderTest.updateServices(controller, sReqs, mapRequestProps, true, false));

    // update configs at SCH level
    configVersions.clear();
    configVersions.put("typeA", "v1");
    configVersions.put("typeB", "v1");
    configVersions.put("typeC", "v1");
    schReqs.clear();
    schReqs.add(new ServiceComponentHostRequest(clusterName, serviceName,
        componentName1, host1, null));
    Assert.assertNull(updateHostComponents(schReqs, Collections.<String, String>emptyMap(), true));

    // update configs at SC level
    configVersions.clear();
    configVersions.put("typeC", "v2");
    configVersions.put("typeD", "v1");
    scReqs.clear();
    scReqs.add(new ServiceComponentRequest(clusterName, serviceName,
        componentName1, null));
    Assert.assertNull(ComponentResourceProviderTest.updateComponents(controller, scReqs, Collections.<String, String>emptyMap(), true));

  }

  @Test
  public void testReConfigureService() throws Exception {
    String clusterName = "foo1";
    createCluster(clusterName);
    clusters.getCluster(clusterName)
      .setDesiredStackVersion(new StackId("HDP-0.1"));
    String serviceName = "HDFS";
    createService(clusterName, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";

    Map<String, String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "Called from a test");

    createServiceComponent(clusterName, serviceName, componentName1,
      State.INIT);
    createServiceComponent(clusterName, serviceName, componentName2,
      State.INIT);
    createServiceComponent(clusterName, serviceName, componentName3,
      State.INIT);

    String host1 = "h1";
    String host2 = "h2";

    addHostToCluster(host1, clusterName);
    addHostToCluster(host2, clusterName);


    // null service should work
    createServiceComponentHost(clusterName, null, componentName1,
      host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName2,
      host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName2,
      host2, null);
    createServiceComponentHost(clusterName, serviceName, componentName3,
      host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName3,
      host2, null);

    // Install
    ServiceRequest r = new ServiceRequest(clusterName, serviceName,
      State.INSTALLED.toString());
    Set<ServiceRequest> requests = new HashSet<ServiceRequest>();
    requests.add(r);

    ServiceResourceProviderTest.updateServices(controller, requests, mapRequestProps, true, false);
    Assert.assertEquals(State.INSTALLED,
      clusters.getCluster(clusterName).getService(serviceName)
        .getDesiredState());

    // manually change live state to installed as no running action manager
    for (ServiceComponent sc :
      clusters.getCluster(clusterName).getService(serviceName)
        .getServiceComponents().values()) {
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        sch.setState(State.INSTALLED);
      }
    }

    // Create and attach config
    Map<String, String> configs = new HashMap<String, String>();
    configs.put("a", "b");

    ConfigurationRequest cr1,cr2,cr3;
    cr1 = new ConfigurationRequest(clusterName, "core-site","version1",
      configs, null);
    cr2 = new ConfigurationRequest(clusterName, "hdfs-site","version1",
      configs, null);
    cr3 = new ConfigurationRequest(clusterName, "core-site","version122",
      configs, null);
    controller.createConfiguration(cr1);
    controller.createConfiguration(cr2);
    controller.createConfiguration(cr3);

    Cluster cluster = clusters.getCluster(clusterName);
    Service s = cluster.getService(serviceName);
    ServiceComponent sc1 = s.getServiceComponent(componentName1);
    ServiceComponent sc2 = s.getServiceComponent(componentName2);
    ServiceComponentHost sch1 = sc1.getServiceComponentHost(host1);

    Set<ServiceComponentHostRequest> schReqs =
      new HashSet<ServiceComponentHostRequest>();
    Set<ServiceComponentRequest> scReqs =
      new HashSet<ServiceComponentRequest>();
    Set<ServiceRequest> sReqs = new HashSet<ServiceRequest>();
    Map<String, String> configVersions = new HashMap<String, String>();

    // SCH level
    configVersions.clear();
    configVersions.put("core-site", "version1");
    configVersions.put("hdfs-site", "version1");
    schReqs.clear();
    schReqs.add(new ServiceComponentHostRequest(clusterName, serviceName,
      componentName1, host1, null));
    Assert.assertNull(updateHostComponents(schReqs, Collections.<String, String>emptyMap(), true));

    // Reconfigure SCH level
    configVersions.clear();
    configVersions.put("core-site", "version122");
    schReqs.clear();
    schReqs.add(new ServiceComponentHostRequest(clusterName, serviceName,
      componentName1, host1, null));
    Assert.assertNull(updateHostComponents(schReqs, Collections.<String, String>emptyMap(), true));

    // Clear Entity Manager
    entityManager.clear();

    //SC Level
    configVersions.clear();
    configVersions.put("core-site", "version1");
    configVersions.put("hdfs-site", "version1");
    scReqs.add(new ServiceComponentRequest(clusterName, serviceName,
      componentName2, null));
    Assert.assertNull(ComponentResourceProviderTest.updateComponents(controller, scReqs, Collections.<String, String>emptyMap(), true));

    scReqs.add(new ServiceComponentRequest(clusterName, serviceName,
      componentName1, null));
    Assert.assertNull(ComponentResourceProviderTest.updateComponents(controller, scReqs, Collections.<String, String>emptyMap(), true));

    // Reconfigure SC level
    configVersions.clear();
    configVersions.put("core-site", "version122");

    scReqs.clear();
    scReqs.add(new ServiceComponentRequest(clusterName, serviceName,
      componentName2, null));
    Assert.assertNull(ComponentResourceProviderTest.updateComponents(controller, scReqs, Collections.<String, String>emptyMap(), true));

    scReqs.clear();
    scReqs.add(new ServiceComponentRequest(clusterName, serviceName,
      componentName1, null));
    Assert.assertNull(ComponentResourceProviderTest.updateComponents(controller, scReqs, Collections.<String, String>emptyMap(), true));

    entityManager.clear();

    // S level
    configVersions.clear();
    configVersions.put("core-site", "version1");
    configVersions.put("hdfs-site", "version1");
    sReqs.clear();
    sReqs.add(new ServiceRequest(clusterName, serviceName, null));
    Assert.assertNull(ServiceResourceProviderTest.updateServices(controller, sReqs, mapRequestProps, true, false));

    // Reconfigure S Level
    configVersions.clear();
    configVersions.put("core-site", "version122");

    sReqs.clear();
    sReqs.add(new ServiceRequest(clusterName, serviceName, null));
    Assert.assertNull(ServiceResourceProviderTest.updateServices(controller, sReqs, mapRequestProps, true, false));

    entityManager.clear();

  }

  @Test
  public void testReConfigureServiceClient() throws AmbariException {
    String clusterName = "foo1";
    createCluster(clusterName);
    Cluster cluster = clusters.getCluster(clusterName);
    cluster.setDesiredStackVersion(new StackId("HDP-0.1"));
    String serviceName1 = "HDFS";
    String serviceName2 = "MAPREDUCE";
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";
    String componentName4 = "JOBTRACKER";
    String componentName5 = "TASKTRACKER";
    String componentName6 = "MAPREDUCE_CLIENT";

    createService(clusterName, serviceName1, null);
    createService(clusterName, serviceName2, null);

    createServiceComponent(clusterName, serviceName1, componentName1,
      State.INIT);
    createServiceComponent(clusterName, serviceName1, componentName2,
      State.INIT);
    createServiceComponent(clusterName, serviceName1, componentName3,
      State.INIT);
    createServiceComponent(clusterName, serviceName2, componentName4,
      State.INIT);
    createServiceComponent(clusterName, serviceName2, componentName5,
      State.INIT);
    createServiceComponent(clusterName, serviceName2, componentName6,
      State.INIT);

    String host1 = "h1";
    String host2 = "h2";
    String host3 = "h3";

    addHostToCluster(host1, clusterName);
    addHostToCluster(host2, clusterName);
    addHostToCluster(host3, clusterName);

    createServiceComponentHost(clusterName, serviceName1, componentName1,
      host1, null);
    createServiceComponentHost(clusterName, serviceName1, componentName2,
      host1, null);
    createServiceComponentHost(clusterName, serviceName2, componentName4,
      host1, null);
    createServiceComponentHost(clusterName, serviceName2, componentName5,
      host1, null);
    createServiceComponentHost(clusterName, serviceName1, componentName2,
      host2, null);
    createServiceComponentHost(clusterName, serviceName1, componentName3,
      host2, null);
    createServiceComponentHost(clusterName, serviceName2, componentName6,
      host2, null);
    createServiceComponentHost(clusterName, serviceName1, componentName3,
      host3, null);
    createServiceComponentHost(clusterName, serviceName2, componentName6,
      host3, null);

    // Create and attach config
    Map<String, String> configs = new HashMap<String, String>();
    configs.put("a", "b");
    Map<String, String> configs2 = new HashMap<String, String>();
    configs2.put("c", "d");
    Map<String, String> configs3 = new HashMap<String, String>();

    ConfigurationRequest cr1,cr2,cr3,cr4;
    cr1 = new ConfigurationRequest(clusterName, "core-site","version1",
      configs, null);
    cr2 = new ConfigurationRequest(clusterName, "hdfs-site","version1",
      configs, null);
    cr4 = new ConfigurationRequest(clusterName, "kerberos-env", "version1",
      configs3, null);

    ConfigFactory cf = injector.getInstance(ConfigFactory.class);
    Config config1 = cf.createNew(cluster, "kerberos-env",
        new HashMap<String, String>(), new HashMap<String, Map<String,String>>());
    config1.setTag("version1");

    cluster.addConfig(config1);

    ClusterRequest crReq = new ClusterRequest(cluster.getClusterId(), clusterName, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr1));
    controller.updateClusters(Collections.singleton(crReq), null);
    crReq = new ClusterRequest(cluster.getClusterId(), clusterName, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr2));
    controller.updateClusters(Collections.singleton(crReq), null);
    crReq = new ClusterRequest(cluster.getClusterId(), clusterName, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr4));
    controller.updateClusters(Collections.singleton(crReq), null);

    // Install
    long requestId1 = installService(clusterName, serviceName1, true, false);

    List<Stage> stages = actionDB.getAllStages(requestId1);
    Assert.assertEquals(3, stages.get(0).getOrderedHostRoleCommands().get(0)
      .getExecutionCommandWrapper().getExecutionCommand()
      .getConfigurationTags().size());

    installService(clusterName, serviceName2, false, false);

    // Start
    startService(clusterName, serviceName1, true, false);
    startService(clusterName, serviceName2, true, false);

    // Reconfigure
    cr3 = new ConfigurationRequest(clusterName, "core-site","version122",
        configs2, null);
    crReq = new ClusterRequest(cluster.getClusterId(), clusterName, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr3));
    controller.updateClusters(Collections.singleton(crReq), null);

    // Stop HDFS & MAPREDUCE
    stopService(clusterName, serviceName1, false, false);
    stopService(clusterName, serviceName2, false, false);

    // Start
    long requestId2 = startService(clusterName, serviceName1, true, true);
    long requestId3 = startService(clusterName, serviceName2, true, true);

    stages = new ArrayList<>();
    stages.addAll(actionDB.getAllStages(requestId2));
    stages.addAll(actionDB.getAllStages(requestId3));
    HostRoleCommand hdfsCmdHost3 = null;
    HostRoleCommand hdfsCmdHost2 = null;
    HostRoleCommand mapRedCmdHost2 = null;
    HostRoleCommand mapRedCmdHost3 = null;
    for (Stage stage : stages) {
      List<HostRoleCommand> hrcs = stage.getOrderedHostRoleCommands();

      for (HostRoleCommand hrc : hrcs) {
        LOG.debug("role: " + hrc.getRole());
        if (hrc.getRole().toString().equals("HDFS_CLIENT")) {
          if (hrc.getHostName().equals(host3)) {
            hdfsCmdHost3 = hrc;
          } else if (hrc.getHostName().equals(host2)) {
            hdfsCmdHost2 = hrc;
          }
        }
        if (hrc.getRole().toString().equals("MAPREDUCE_CLIENT")) {
          if (hrc.getHostName().equals(host2)) {
            mapRedCmdHost2 = hrc;
          } else if (hrc.getHostName().equals(host3)) {
            mapRedCmdHost3 = hrc;
          }
        }
      }
    }
    Assert.assertNotNull(hdfsCmdHost3);
    Assert.assertNotNull(hdfsCmdHost2);
    ExecutionCommand execCmd = hdfsCmdHost3.getExecutionCommandWrapper()
      .getExecutionCommand();
    Assert.assertEquals(3, execCmd.getConfigurationTags().size());
    Assert.assertEquals("version122", execCmd.getConfigurationTags().get
      ("core-site").get("tag"));
    Assert.assertEquals("d", execCmd.getConfigurations().get("core-site")
      .get("c"));
    // Check if MapReduce client is reinstalled
    Assert.assertNotNull(mapRedCmdHost2);
    Assert.assertNotNull(mapRedCmdHost3);

    /**
     * Test for lost host
     */
    // Stop HDFS & MAPREDUCE
    stopService(clusterName, serviceName1, false, false);
    stopService(clusterName, serviceName2, false, false);

    clusters.getHost(host2).setState(HostState.HEARTBEAT_LOST);

    // Start MAPREDUCE, HDFS is started as a dependency
    requestId3 = startService(clusterName, serviceName2, true, true);
    stages = actionDB.getAllStages(requestId3);
    HostRoleCommand clientWithHostDown = null;
    for (Stage stage : stages) {
      for (HostRoleCommand hrc : stage.getOrderedHostRoleCommands()) {
        if (hrc.getRole().toString().equals("MAPREDUCE_CLIENT") && hrc
          .getHostName().equals(host2)) {
          clientWithHostDown = hrc;
        }
      }
    }
    Assert.assertNull(clientWithHostDown);

    Assert.assertEquals(State.STARTED, clusters.getCluster(clusterName).
      getService("MAPREDUCE").getServiceComponent("TASKTRACKER").
      getServiceComponentHost(host1).getState());
    Assert.assertEquals(State.STARTED, clusters.getCluster(clusterName).
      getService("HDFS").getServiceComponent("NAMENODE").
      getServiceComponentHost(host1).getState());
    Assert.assertEquals(State.STARTED, clusters.getCluster(clusterName).
      getService("HDFS").getServiceComponent("DATANODE").
      getServiceComponentHost(host1).getState());
  }

  @Test
  public void testReconfigureClientWithServiceStarted() throws Exception {
    String clusterName = "foo1";
    createCluster(clusterName);
    Cluster cluster = clusters.getCluster(clusterName);
    cluster.setDesiredStackVersion(new StackId("HDP-0.1"));
    String serviceName = "HDFS";
    createService(clusterName, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";

    createServiceComponent(clusterName, serviceName, componentName1,
      State.INIT);
    createServiceComponent(clusterName, serviceName, componentName2,
      State.INIT);
    createServiceComponent(clusterName, serviceName, componentName3,
      State.INIT);

    String host1 = "h1";
    String host2 = "h2";

    addHostToCluster(host1, clusterName);
    addHostToCluster(host2, clusterName);

    createServiceComponentHost(clusterName, serviceName, componentName1,
      host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName2,
      host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName3,
      host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName3,
      host2, null);

    // Create and attach config
    Map<String, String> configs = new HashMap<String, String>();
    configs.put("a", "b");
    Map<String, String> configs2 = new HashMap<String, String>();
    configs2.put("c", "d");

    ConfigurationRequest cr1,cr2,cr3;
    cr1 = new ConfigurationRequest(clusterName, "core-site","version1",
      configs, null);
    cr2 = new ConfigurationRequest(clusterName, "hdfs-site","version1",
      configs, null);

    ClusterRequest crReq = new ClusterRequest(cluster.getClusterId(), clusterName, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr1));
    controller.updateClusters(Collections.singleton(crReq), null);
    crReq = new ClusterRequest(cluster.getClusterId(), clusterName, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr2));
    controller.updateClusters(Collections.singleton(crReq), null);

    installService(clusterName, serviceName, false, false);
    startService(clusterName, serviceName, false, false);

    Cluster c = clusters.getCluster(clusterName);
    Service s = c.getService(serviceName);
    // Stop Sch only
    stopServiceComponentHosts(clusterName, serviceName);
    Assert.assertEquals(State.STARTED, s.getDesiredState());
    for (ServiceComponent sc : s.getServiceComponents().values()) {
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        Assert.assertEquals(State.INSTALLED, sch.getDesiredState());
      }
    }

    // Reconfigure
    cr3 = new ConfigurationRequest(clusterName, "core-site","version122",
      configs2, null);
    crReq = new ClusterRequest(cluster.getClusterId(), clusterName, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr3));
    controller.updateClusters(Collections.singleton(crReq), null);

    long id = startService(clusterName, serviceName, false, true);
    List<Stage> stages = actionDB.getAllStages(id);
    HostRoleCommand clientHrc = null;
    for (Stage stage : stages) {
      for (HostRoleCommand hrc : stage.getOrderedHostRoleCommands()) {
        if (hrc.getHostName().equals(host2) && hrc.getRole().toString()
          .equals("HDFS_CLIENT")) {
          clientHrc = hrc;
          Assert.assertEquals("version122", hrc.getExecutionCommandWrapper()
            .getExecutionCommand().getConfigurationTags().get("core-site")
            .get("tag"));
        }
      }
    }
    Assert.assertNotNull(clientHrc);
  }

  @Test
  public void testClientServiceSmokeTests() throws AmbariException {
    String clusterName = "foo1";
    createCluster(clusterName);
    clusters.getCluster(clusterName)
        .setDesiredStackVersion(new StackId("HDP-0.1"));
    String serviceName = "PIG";
    createService(clusterName, serviceName, null);
    String componentName1 = "PIG";
    createServiceComponent(clusterName, serviceName, componentName1,
        State.INIT);

    String host1 = "h1";
    String host2 = "h2";

    addHostToCluster(host1, clusterName);
    addHostToCluster(host2, clusterName);

    Map<String, String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "Called from a test");

    // null service should work
    createServiceComponentHost(clusterName, null, componentName1,
        host1, null);
    createServiceComponentHost(clusterName, null, componentName1,
        host2, null);

    ServiceRequest r = new ServiceRequest(clusterName, serviceName, State.INSTALLED.toString());
    Set<ServiceRequest> requests = new HashSet<ServiceRequest>();
    requests.add(r);

    RequestStatusResponse trackAction =
        ServiceResourceProviderTest.updateServices(controller, requests, mapRequestProps, true, false);
    Assert.assertEquals(State.INSTALLED,
        clusters.getCluster(clusterName).getService(serviceName)
        .getDesiredState());
    for (ServiceComponent sc :
      clusters.getCluster(clusterName).getService(serviceName)
      .getServiceComponents().values()) {
      Assert.assertEquals(State.INSTALLED, sc.getDesiredState());
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        Assert.assertEquals(State.INSTALLED, sch.getDesiredState());
        Assert.assertEquals(State.INIT, sch.getState());
      }
    }

    List<ShortTaskStatus> taskStatuses = trackAction.getTasks();
    Assert.assertEquals(2, taskStatuses.size());

    List<Stage> stages = actionDB.getAllStages(trackAction.getRequestId());
    Assert.assertEquals(1, stages.size());
    Assert.assertEquals("Called from a test", stages.get(0).getRequestContext());

    for (ServiceComponent sc :
      clusters.getCluster(clusterName).getService(serviceName)
          .getServiceComponents().values()) {
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        sch.setState(State.INSTALLED);
      }
    }

    r = new ServiceRequest(clusterName, serviceName, State.STARTED.toString());
    requests.clear();
    requests.add(r);

    injector.getInstance(ActionMetadata.class).addServiceCheckAction("PIG");
    trackAction = ServiceResourceProviderTest.updateServices(controller, requests, mapRequestProps, true, false);
    Assert.assertNotNull(trackAction);
    Assert.assertEquals(State.INSTALLED,
        clusters.getCluster(clusterName).getService(serviceName)
        .getDesiredState());
    for (ServiceComponent sc :
      clusters.getCluster(clusterName).getService(serviceName)
          .getServiceComponents().values()) {
      Assert.assertEquals(State.INSTALLED, sc.getDesiredState());
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        Assert.assertEquals(State.INSTALLED, sch.getDesiredState());
        Assert.assertEquals(State.INSTALLED, sch.getState());
      }
    }

    stages = actionDB.getAllStages(trackAction.getRequestId());
    for (Stage s : stages) {
      LOG.info("Stage dump : " + s.toString());
    }
    Assert.assertEquals(1, stages.size());

    taskStatuses = trackAction.getTasks();
    Assert.assertEquals(1, taskStatuses.size());
    Assert.assertEquals(Role.PIG_SERVICE_CHECK.toString(),
        taskStatuses.get(0).getRole());
  }

  @Test
  public void testSkipTaskOnUnhealthyHosts() throws AmbariException {
    String clusterName = "foo1";
    createCluster(clusterName);
    clusters.getCluster(clusterName)
        .setDesiredStackVersion(new StackId("HDP-0.1"));
    String serviceName = "HDFS";
    createService(clusterName, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";

    createServiceComponent(clusterName, serviceName, componentName1,
        State.INIT);
    createServiceComponent(clusterName, serviceName, componentName2,
        State.INIT);

    String host1 = "h1";
    String host2 = "h2";
    String host3 = "h3";

    addHostToCluster(host1, clusterName);
    addHostToCluster(host2, clusterName);
    addHostToCluster(host3, clusterName);

    createServiceComponentHost(clusterName, serviceName, componentName1,
        host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName2,
        host2, null);
    createServiceComponentHost(clusterName, serviceName, componentName2,
        host3, null);

    // Install
    installService(clusterName, serviceName, false, false);

    // h1=HEALTHY, h2=HEARTBEAT_LOST, h3=WAITING_FOR_HOST_STATUS_UPDATES
    clusters.getHost("h1").setState(HostState.HEALTHY);
    clusters.getHost("h2").setState(HostState.HEALTHY);
    clusters.getHost("h3").setState(HostState.HEARTBEAT_LOST);

    long requestId = startService(clusterName, serviceName, true, false);
    List<HostRoleCommand> commands = actionDB.getRequestTasks(requestId);
    Assert.assertEquals(3, commands.size());
    int commandCount = 0;
    for (HostRoleCommand command : commands) {
      if (command.getRoleCommand() == RoleCommand.START) {
        Assert.assertTrue(command.getHostName().equals("h1") || command.getHostName().equals("h2"));
        commandCount++;
      }
    }
    Assert.assertEquals("Expect only two task.", 2, commandCount);

    stopService(clusterName, serviceName, false, false);

    // h1=HEARTBEAT_LOST, h2=HEARTBEAT_LOST, h3=HEALTHY
    clusters.getHost("h1").setState(HostState.HEARTBEAT_LOST);
    clusters.getHost("h2").setState(HostState.HEARTBEAT_LOST);
    clusters.getHost("h3").setState(HostState.HEALTHY);

    requestId = startService(clusterName, serviceName, true, false);
    commands = actionDB.getRequestTasks(requestId);
    commandCount = 0;
    for (HostRoleCommand command : commands) {
      if (command.getRoleCommand() == RoleCommand.START) {
        Assert.assertTrue(command.getHostName().equals("h3"));
        commandCount++;
      }
    }
    Assert.assertEquals("Expect only one task.", 1, commandCount);

    stopService(clusterName, serviceName, false, false);

    // h1=HEALTHY, h2=HEALTHY, h3=HEALTHY
    clusters.getHost("h1").setState(HostState.HEALTHY);
    clusters.getHost("h2").setState(HostState.HEALTHY);
    clusters.getHost("h3").setState(HostState.HEALTHY);

    requestId = startService(clusterName, serviceName, true, false);
    commands = actionDB.getRequestTasks(requestId);
    commandCount = 0;
    for (HostRoleCommand command : commands) {
      if (command.getRoleCommand() == RoleCommand.START) {
        Assert.assertTrue(command.getHostName().equals("h3") ||
            command.getHostName().equals("h2") ||
            command.getHostName().equals("h1"));
        commandCount++;
      }
    }
    Assert.assertEquals("Expect all three task.", 3, commandCount);

    // h1=HEALTHY, h2=HEARTBEAT_LOST, h3=HEALTHY
    clusters.getHost("h2").setState(HostState.HEARTBEAT_LOST);
    requestId = stopService(clusterName, serviceName, false, false);
    commands = actionDB.getRequestTasks(requestId);
    Assert.assertEquals(2, commands.size());
    commandCount = 0;
    for (HostRoleCommand command : commands) {
      if (command.getRoleCommand() == RoleCommand.STOP) {
        Assert.assertTrue(command.getHostName().equals("h3") ||
            command.getHostName().equals("h1"));
        commandCount++;
      }
    }
    Assert.assertEquals("Expect only two task.", 2, commandCount);

    // Force a sch into INSTALL_FAILED
    Cluster cluster = clusters.getCluster(clusterName);
    Service s = cluster.getService(serviceName);
    ServiceComponent sc3 = s.getServiceComponent(componentName2);
    for (ServiceComponentHost sch : sc3.getServiceComponentHosts().values()) {
      if (sch.getHostName().equals("h3")) {
        sch.setState(State.INSTALL_FAILED);
      }
    }

    // h1=HEALTHY, h2=HEALTHY, h3=HEARTBEAT_LOST
    clusters.getHost("h3").setState(HostState.HEARTBEAT_LOST);
    clusters.getHost("h2").setState(HostState.HEALTHY);
    requestId = installService(clusterName, serviceName, false, false);
    Assert.assertEquals(-1, requestId);

    // All healthy, INSTALL should succeed
    clusters.getHost("h3").setState(HostState.HEALTHY);
    requestId = installService(clusterName, serviceName, false, false);
    commands = actionDB.getRequestTasks(requestId);
    Assert.assertEquals(1, commands.size());
    commandCount = 0;
    for (HostRoleCommand command : commands) {
      if (command.getRoleCommand() == RoleCommand.INSTALL) {
        Assert.assertTrue(command.getHostName().equals("h3"));
        commandCount++;
      }
    }
    Assert.assertEquals("Expect only one task.", 1, commandCount);
  }

  @Test
  public void testServiceCheckWhenHostIsUnhealthy() throws AmbariException {
    String clusterName = "foo1";
    createCluster(clusterName);
    clusters.getCluster(clusterName)
        .setDesiredStackVersion(new StackId("HDP-0.1"));
    String serviceName = "HDFS";
    createService(clusterName, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";

    createServiceComponent(clusterName, serviceName, componentName1,
        State.INIT);
    createServiceComponent(clusterName, serviceName, componentName2,
        State.INIT);
    createServiceComponent(clusterName, serviceName, componentName3,
        State.INIT);

    String host1 = "h1";
    String host2 = "h2";
    String host3 = "h3";

    addHostToCluster(host1, clusterName);
    addHostToCluster(host2, clusterName);
    addHostToCluster(host3, clusterName);

    createServiceComponentHost(clusterName, serviceName, componentName1,
        host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName2,
        host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName3,
        host2, null);
    createServiceComponentHost(clusterName, serviceName, componentName3,
        host3, null);

    // Install
    installService(clusterName, serviceName, false, false);
    clusters.getHost("h3").setState(HostState.UNHEALTHY);
    clusters.getHost("h2").setState(HostState.HEALTHY);

    // Start
    long requestId = startService(clusterName, serviceName, true, false);
    List<HostRoleCommand> commands = actionDB.getRequestTasks(requestId);
    int commandCount = 0;
    for(HostRoleCommand command : commands) {
      if(command.getRoleCommand() == RoleCommand.SERVICE_CHECK &&
          command.getRole() == Role.HDFS_SERVICE_CHECK) {
        Assert.assertTrue(command.getHostName().equals("h2"));
        commandCount++;
      }
    }
    Assert.assertEquals("Expect only one service check.", 1, commandCount);

    stopService(clusterName, serviceName, false, false);

    clusters.getHost("h3").setState(HostState.HEALTHY);
    clusters.getHost("h2").setState(HostState.HEARTBEAT_LOST);

    requestId = startService(clusterName, serviceName, true, false);
    commands = actionDB.getRequestTasks(requestId);
    commandCount = 0;
    for(HostRoleCommand command : commands) {
      if(command.getRoleCommand() == RoleCommand.SERVICE_CHECK &&
          command.getRole() == Role.HDFS_SERVICE_CHECK) {
        Assert.assertTrue(command.getHostName().equals("h3"));
        commandCount++;
      }
    }
    Assert.assertEquals("Expect only one service check.", 1, commandCount);

    RequestResourceFilter resourceFilter = new RequestResourceFilter("HDFS", null, null);
    ExecuteActionRequest actionRequest = new ExecuteActionRequest("foo1", Role.HDFS_SERVICE_CHECK.name(), null, false);
    actionRequest.getResourceFilters().add(resourceFilter);
    Map<String, String> requestProperties = new HashMap<String, String>();

    RequestStatusResponse response = controller.createAction(actionRequest, requestProperties);
    commands = actionDB.getRequestTasks(response.getRequestId());
    commandCount = 0;
    for(HostRoleCommand command : commands) {
      if(command.getRoleCommand() == RoleCommand.SERVICE_CHECK &&
          command.getRole() == Role.HDFS_SERVICE_CHECK) {
        Assert.assertTrue(command.getHostName().equals("h3"));
        commandCount++;
      }
    }
    Assert.assertEquals("Expect only one service check.", 1, commandCount);

    // When both are unhealthy then just pick one
    clusters.getHost("h3").setState(HostState.WAITING_FOR_HOST_STATUS_UPDATES);
    clusters.getHost("h2").setState(HostState.INIT);
    response = controller.createAction(actionRequest, requestProperties);
    commands = actionDB.getRequestTasks(response.getRequestId());
    commandCount = 0;
    for(HostRoleCommand command : commands) {
      if(command.getRoleCommand() == RoleCommand.SERVICE_CHECK &&
          command.getRole() == Role.HDFS_SERVICE_CHECK) {
        Assert.assertTrue(command.getHostName().equals("h3") ||
            command.getHostName().equals("h2"));
        commandCount++;
      }
    }
    Assert.assertEquals("Expect only one service check.", 1, commandCount);
    Assert.assertEquals("", response.getRequestContext());
  }

  @Test
  public void testReInstallForInstallFailedClient() throws AmbariException {
    String clusterName = "foo1";
    createCluster(clusterName);
    clusters.getCluster(clusterName)
      .setDesiredStackVersion(new StackId("HDP-0.1"));
    String serviceName = "HDFS";
    createService(clusterName, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";

    createServiceComponent(clusterName, serviceName, componentName1,
      State.INIT);
    createServiceComponent(clusterName, serviceName, componentName2,
      State.INIT);
    createServiceComponent(clusterName, serviceName, componentName3,
      State.INIT);

    String host1 = "h1";
    String host2 = "h2";
    String host3 = "h3";

    addHostToCluster(host1, clusterName);
    addHostToCluster(host2, clusterName);
    addHostToCluster(host3, clusterName);

    createServiceComponentHost(clusterName, serviceName, componentName1,
      host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName2,
      host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName3,
      host2, null);
    createServiceComponentHost(clusterName, serviceName, componentName3,
      host3, null);

    // Install
    installService(clusterName, serviceName, false, false);

    // Mark client as install failed.
    Cluster cluster = clusters.getCluster(clusterName);
    Service s = cluster.getService(serviceName);
    ServiceComponent sc3 = s.getServiceComponent(componentName3);
    for(ServiceComponentHost sch : sc3.getServiceComponentHosts().values()) {
      if (sch.getHostName().equals(host3)) {
        sch.setState(State.INSTALL_FAILED);
      }
    }

    // Start
    long requestId = startService(clusterName, serviceName, false, true);
    List<Stage> stages = actionDB.getAllStages(requestId);
    HostRoleCommand clientReinstallCmd = null;
    for (Stage stage : stages) {
      for (HostRoleCommand hrc : stage.getOrderedHostRoleCommands()) {
        if (hrc.getHostName().equals(host3) && hrc.getRole().toString()
          .equals("HDFS_CLIENT")) {
          clientReinstallCmd = hrc;
          break;
        }
      }
    }
    Assert.assertNotNull(clientReinstallCmd);
  }

  @Test
  public void testReInstallClientComponent() throws Exception {
    String clusterName = "foo1";
    createCluster(clusterName);
    clusters.getCluster(clusterName)
      .setDesiredStackVersion(new StackId("HDP-2.0.6"));
    String serviceName = "HDFS";
    createService(clusterName, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";

    createServiceComponent(clusterName, serviceName, componentName1,
      State.INIT);
    createServiceComponent(clusterName, serviceName, componentName2,
      State.INIT);
    createServiceComponent(clusterName, serviceName, componentName3,
      State.INIT);

    String host1 = "h1";
    String host2 = "h2";
    String host3 = "h3";

    addHostToCluster(host1, clusterName);
    addHostToCluster(host2, clusterName);
    addHostToCluster(host3, clusterName);

    createServiceComponentHost(clusterName, serviceName, componentName1,
      host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName2,
      host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName3,
      host2, null);
    createServiceComponentHost(clusterName, serviceName, componentName3,
      host3, null);

    // Install
    installService(clusterName, serviceName, false, false);

    // Reinstall SCH
    ServiceComponentHostRequest schr = new ServiceComponentHostRequest
      (clusterName, serviceName, componentName3, host3, State.INSTALLED.name());
    Set<ServiceComponentHostRequest> setReqs = new
      HashSet<ServiceComponentHostRequest>();
    setReqs.add(schr);
    RequestStatusResponse resp = updateHostComponents(setReqs,
      Collections.<String, String>emptyMap(), false);

    Assert.assertNotNull(resp);
    Assert.assertTrue(resp.getRequestId() > 0);
    List<Stage> stages = actionDB.getAllStages(resp.getRequestId());
    HostRoleCommand clientReinstallCmd = null;
    for (Stage stage : stages) {
      for (HostRoleCommand hrc : stage.getOrderedHostRoleCommands()) {
        if (hrc.getHostName().equals(host3) && hrc.getRole().toString()
          .equals("HDFS_CLIENT")) {
          clientReinstallCmd = hrc;
          break;
        }
      }
    }
    Assert.assertNotNull(clientReinstallCmd);
  }

  @Test
  public void testReInstallClientComponentFromServiceChange() throws AmbariException {
    String clusterName = "foo1";
    createCluster(clusterName);
    clusters.getCluster(clusterName)
      .setDesiredStackVersion(new StackId("HDP-2.0.6"));
    String serviceName = "HDFS";
    createService(clusterName, serviceName, null);
    String componentName = "HDFS_CLIENT";

    createServiceComponent(clusterName, serviceName, componentName,
      State.INIT);

    String host1 = "h1";
    String host2 = "h2";

    addHostToCluster(host1, clusterName);
    addHostToCluster(host2, clusterName);

    createServiceComponentHost(clusterName, serviceName, componentName,
      host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName,
      host2, null);

    // Install
    installService(clusterName, serviceName, false, false);

    // Start Service
    ServiceRequest sr = new ServiceRequest(
      clusterName, serviceName, State.STARTED.name());
    Set<ServiceRequest> setReqs = new HashSet<ServiceRequest>();
    setReqs.add(sr);
    RequestStatusResponse resp = ServiceResourceProviderTest.updateServices(controller,
      setReqs, Collections.<String, String>emptyMap(), false, true);

    Assert.assertNotNull(resp);
    Assert.assertTrue(resp.getRequestId() > 0);

    List<Stage> stages = actionDB.getAllStages(resp.getRequestId());
    Map<String, Role> hostsToRoles = new HashMap<String, Role>();
    for (Stage stage : stages) {
      for (HostRoleCommand hrc : stage.getOrderedHostRoleCommands()) {
          hostsToRoles.put(hrc.getHostName(), hrc.getRole());
      }
    }

    Map<String, Role> expectedHostsToRoles = new HashMap<String, Role>();
    expectedHostsToRoles.put(host1, Role.HDFS_CLIENT);
    expectedHostsToRoles.put(host2, Role.HDFS_CLIENT);
    Assert.assertEquals(expectedHostsToRoles, hostsToRoles);
  }

  @Test
  public void testDecommissonDatanodeAction() throws Exception {
    String clusterName = "foo1";
    createCluster(clusterName);
    Cluster cluster = clusters.getCluster(clusterName);
    cluster.setDesiredStackVersion(new StackId("HDP-2.0.7"));
    String serviceName = "HDFS";
    createService(clusterName, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";

    createServiceComponent(clusterName, serviceName, componentName1,
      State.INIT);
    createServiceComponent(clusterName, serviceName, componentName2,
      State.INIT);
    createServiceComponent(clusterName, serviceName, componentName3,
      State.INIT);

    String host1 = "h1";
    String host2 = "h2";

    addHostToCluster(host1, clusterName);
    addHostToCluster(host2, clusterName);

    createServiceComponentHost(clusterName, serviceName, componentName1,
      host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName2,
      host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName2,
      host2, null);
    createServiceComponentHost(clusterName, serviceName, componentName3,
      host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName3,
      host2, null);

    RequestOperationLevel level = new RequestOperationLevel(
            Resource.Type.HostComponent, clusterName, null, null, null);

    // Install
    installService(clusterName, serviceName, false, false);

    // Create and attach config
    Map<String, String> configs = new HashMap<String, String>();
    configs.put("a", "b");

    ConfigurationRequest cr1;
    cr1 = new ConfigurationRequest(clusterName, "hdfs-site","version1",
      configs, null);
    ClusterRequest crReq = new ClusterRequest(cluster.getClusterId(), clusterName, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr1));
    controller.updateClusters(Collections.singleton(crReq), null);

    // Start
    startService(clusterName, serviceName, false, false);

    cluster = clusters.getCluster(clusterName);
    Service s = cluster.getService(serviceName);
    Assert.assertEquals(State.STARTED, s.getDesiredState());
    ServiceComponentHost scHost = s.getServiceComponent("DATANODE").getServiceComponentHost("h2");
    Assert.assertEquals(HostComponentAdminState.INSERVICE, scHost.getComponentAdminState());

    // Decommission one datanode
    Map<String, String> params = new HashMap<String, String>(){{
      put("test", "test");
      put("excluded_hosts", "h2");
      put("align_maintenance_state", "true");
    }};
    RequestResourceFilter resourceFilter = new RequestResourceFilter("HDFS", "NAMENODE", null);
    ArrayList<RequestResourceFilter> filters = new ArrayList<RequestResourceFilter>();
    filters.add(resourceFilter);
    ExecuteActionRequest request = new ExecuteActionRequest(clusterName,
            "DECOMMISSION", null, filters, level, params, false);

    Map<String, String> requestProperties = new HashMap<String, String>();
    requestProperties.put(REQUEST_CONTEXT_PROPERTY, "Called from a test");

    RequestStatusResponse response = controller.createAction(request,
      requestProperties);

    List<HostRoleCommand> storedTasks = actionDB.getRequestTasks(response.getRequestId());
    ExecutionCommand execCmd = storedTasks.get(0).getExecutionCommandWrapper
      ().getExecutionCommand();
    Assert.assertNotNull(storedTasks);
    Assert.assertNotNull(execCmd.getConfigurationTags().get("hdfs-site"));
    Assert.assertEquals(1, storedTasks.size());
    Assert.assertEquals(HostComponentAdminState.DECOMMISSIONED, scHost.getComponentAdminState());
    Assert.assertEquals(MaintenanceState.ON, scHost.getMaintenanceState());
    HostRoleCommand command =  storedTasks.get(0);
    Assert.assertEquals(Role.NAMENODE, command.getRole());
    Assert.assertEquals(RoleCommand.CUSTOM_COMMAND, command.getRoleCommand());
    Assert.assertEquals("DECOMMISSION", execCmd.getHostLevelParams().get("custom_command"));
    Assert.assertEquals(requestProperties.get(REQUEST_CONTEXT_PROPERTY), response.getRequestContext());

    // Decommission the other datanode
    params = new HashMap<String, String>(){{
      put("test", "test");
      put("excluded_hosts", "h1");
      put("align_maintenance_state", "true");
    }};
    resourceFilter = new RequestResourceFilter("HDFS", "NAMENODE", null);
    filters = new ArrayList<RequestResourceFilter>();
    filters.add(resourceFilter);

    request = new ExecuteActionRequest(clusterName, "DECOMMISSION",
            null, filters, level, params, false);

    response = controller.createAction(request,
        requestProperties);

    storedTasks = actionDB.getRequestTasks(response.getRequestId());
    execCmd = storedTasks.get(0).getExecutionCommandWrapper
        ().getExecutionCommand();
    Map<String, String> cmdParams = execCmd.getCommandParams();
    Assert.assertTrue(cmdParams.containsKey("update_exclude_file_only"));
    Assert.assertTrue(cmdParams.get("update_exclude_file_only").equals("false"));
    Assert.assertNotNull(storedTasks);
    Assert.assertEquals(1, storedTasks.size());
    Assert.assertEquals(HostComponentAdminState.DECOMMISSIONED, scHost.getComponentAdminState());
    Assert.assertEquals(MaintenanceState.ON, scHost.getMaintenanceState());
    Assert.assertEquals("DECOMMISSION", execCmd.getHostLevelParams().get("custom_command"));
    Assert.assertEquals(requestProperties.get(REQUEST_CONTEXT_PROPERTY), response.getRequestContext());

    // Recommission the other datanode  (while adding NameNode HA)
    createServiceComponentHost(clusterName, serviceName, componentName1,
        host2, null);
    ServiceComponentHostRequest r = new ServiceComponentHostRequest(clusterName, serviceName,
        componentName1, host2, State.INSTALLED.toString());
    Set<ServiceComponentHostRequest> requests = new HashSet<ServiceComponentHostRequest>();
    requests.add(r);
    updateHostComponents(requests, Collections.<String, String>emptyMap(), true);
    s.getServiceComponent(componentName1).getServiceComponentHost(host2).setState(State.INSTALLED);
    r = new ServiceComponentHostRequest(clusterName, serviceName,
        componentName1, host2, State.STARTED.toString());
    requests.clear();
    requests.add(r);
    updateHostComponents(requests, Collections.<String, String>emptyMap(), true);
    s.getServiceComponent(componentName1).getServiceComponentHost(host2).setState(State.STARTED);

    params = new HashMap<String, String>(){{
      put("test", "test");
      put("included_hosts", "h1 , h2");
      put("align_maintenance_state", "true");
    }};
    resourceFilter = new RequestResourceFilter("HDFS", "NAMENODE", null);
    filters = new ArrayList<RequestResourceFilter>();
    filters.add(resourceFilter);
    request = new ExecuteActionRequest(clusterName, "DECOMMISSION", null,
            filters, level, params, false);

    response = controller.createAction(request,
        requestProperties);

    storedTasks = actionDB.getRequestTasks(response.getRequestId());
    Assert.assertNotNull(storedTasks);
    scHost = s.getServiceComponent("DATANODE").getServiceComponentHost("h2");
    Assert.assertEquals(HostComponentAdminState.INSERVICE, scHost.getComponentAdminState());
    Assert.assertEquals(MaintenanceState.OFF, scHost.getMaintenanceState());
    execCmd = storedTasks.get(0).getExecutionCommandWrapper
        ().getExecutionCommand();
    Assert.assertNotNull(storedTasks);
    Assert.assertEquals(2, storedTasks.size());
    int countRefresh = 0;
    for(HostRoleCommand hrc : storedTasks) {
      Assert.assertTrue("DECOMMISSION, Included: h1,h2".equals(hrc.getCommandDetail()));
      Assert.assertTrue("DECOMMISSION".equals(hrc.getCustomCommandName()));
      cmdParams = hrc.getExecutionCommandWrapper().getExecutionCommand().getCommandParams();
      if(!cmdParams.containsKey("update_exclude_file_only")
          || !cmdParams.get("update_exclude_file_only").equals("true")) {
        countRefresh++;
      }
    }
    Assert.assertEquals(2, countRefresh);

    // Slave components will have admin state as INSERVICE even if the state in DB is null
    scHost.setComponentAdminState(null);
    Assert.assertEquals(HostComponentAdminState.INSERVICE, scHost.getComponentAdminState());
    Assert.assertEquals(MaintenanceState.OFF, scHost.getMaintenanceState());
    Assert.assertEquals(requestProperties.get(REQUEST_CONTEXT_PROPERTY), response.getRequestContext());
  }

  @Test
  public void testResourceFiltersWithCustomActions() throws AmbariException {
    setupClusterWithHosts("c1", "HDP-2.0.6",
      new ArrayList<String>() {{
        add("h1");
        add("h2");
        add("h3");
      }},
      "centos6");

    Cluster cluster = clusters.getCluster("c1");
    cluster.setDesiredStackVersion(new StackId("HDP-2.0.6"));
    cluster.setCurrentStackVersion(new StackId("HDP-2.0.6"));

    ConfigFactory cf = injector.getInstance(ConfigFactory.class);
    Config config1 = cf.createNew(cluster, "global",
      new HashMap<String, String>() {{
        put("key1", "value1");
      }}, new HashMap<String, Map<String,String>>());
    config1.setTag("version1");

    Config config2 = cf.createNew(cluster, "core-site",
      new HashMap<String, String>() {{
        put("key1", "value1");
      }}, new HashMap<String, Map<String,String>>());
    config2.setTag("version1");

    cluster.addConfig(config1);
    cluster.addConfig(config2);

    Service hdfs = cluster.addService("HDFS");
    hdfs.persist();

    Service mapred = cluster.addService("YARN");
    mapred.persist();

    hdfs.addServiceComponent(Role.HDFS_CLIENT.name()).persist();
    hdfs.addServiceComponent(Role.NAMENODE.name()).persist();
    hdfs.addServiceComponent(Role.DATANODE.name()).persist();

    mapred.addServiceComponent(Role.RESOURCEMANAGER.name()).persist();

    hdfs.getServiceComponent(Role.HDFS_CLIENT.name()).addServiceComponentHost("h1").persist();
    hdfs.getServiceComponent(Role.NAMENODE.name()).addServiceComponentHost("h1").persist();
    hdfs.getServiceComponent(Role.DATANODE.name()).addServiceComponentHost("h1").persist();
    hdfs.getServiceComponent(Role.DATANODE.name()).addServiceComponentHost("h2").persist();

    controller.getAmbariMetaInfo().addActionDefinition(new ActionDefinition(
      "a1", ActionType.SYSTEM, "", "HDFS", "", "Some custom action.",
      TargetHostType.ALL, Short.valueOf("10010")));

    Map<String, String> params = new HashMap<String, String>() {{
      put("test", "test");
    }};

    Map<String, String> requestProperties = new HashMap<String, String>();
    requestProperties.put(REQUEST_CONTEXT_PROPERTY, "Called from a test");

    List<RequestResourceFilter> resourceFilters = new ArrayList<RequestResourceFilter>();
    ArrayList<String> hosts = new ArrayList<String>() {{ add("h2"); }};
    RequestResourceFilter resourceFilter1 = new RequestResourceFilter("HDFS", "DATANODE", hosts);

    hosts = new ArrayList<String>() {{ add("h1"); }};
    RequestResourceFilter resourceFilter2 = new RequestResourceFilter("HDFS", "NAMENODE", hosts);

    resourceFilters.add(resourceFilter1);
    resourceFilters.add(resourceFilter2);

    ExecuteActionRequest actionRequest = new ExecuteActionRequest("c1", null, "a1", resourceFilters, null, params, false);
    RequestStatusResponse response = null;
    try {
      response = controller.createAction(actionRequest, requestProperties);
    } catch (AmbariException ae) {
      LOG.info("Expected exception.", ae);
      Assert.assertTrue(ae.getMessage().contains("Custom action definition only " +
        "allows one resource filter to be specified"));
    }
    resourceFilters.remove(resourceFilter1);
    actionRequest = new ExecuteActionRequest("c1", null, "a1", resourceFilters, null, params, false);
    response = controller.createAction(actionRequest, requestProperties);

    assertEquals(1, response.getTasks().size());
    HostRoleCommand nnCommand = null;

    for (HostRoleCommand hrc : actionDB.getRequestTasks(response.getRequestId())) {
      if (hrc.getHostName().equals("h1")) {
        nnCommand = hrc;
      }
    }

    Assert.assertNotNull(nnCommand);
    ExecutionCommand cmd = nnCommand.getExecutionCommandWrapper().getExecutionCommand();
    Assert.assertEquals("a1", cmd.getRole());
    Assert.assertEquals("10010", cmd.getCommandParams().get("command_timeout"));
    Type type = new TypeToken<Map<String, String>>(){}.getType();
    for (Stage stage : actionDB.getAllStages(response.getRequestId())){
      Map<String, String> commandParamsStage = StageUtils.getGson().fromJson(stage.getCommandParamsStage(), type);
      Assert.assertTrue(commandParamsStage.containsKey("test"));
    }
    Assert.assertEquals(requestProperties.get(REQUEST_CONTEXT_PROPERTY), response.getRequestContext());
  }

  @Test
  public void testResourceFiltersWithCustomCommands() throws AmbariException {
    setupClusterWithHosts("c1", "HDP-2.0.6",
      new ArrayList<String>() {{
        add("h1");
        add("h2");
        add("h3");
      }},
      "centos6");

    Cluster cluster = clusters.getCluster("c1");
    cluster.setDesiredStackVersion(new StackId("HDP-2.0.6"));
    cluster.setCurrentStackVersion(new StackId("HDP-2.0.6"));

    ConfigFactory cf = injector.getInstance(ConfigFactory.class);
    Config config1 = cf.createNew(cluster, "global",
      new HashMap<String, String>() {{
        put("key1", "value1");
      }}, new HashMap<String, Map<String,String>>());
    config1.setTag("version1");

    Config config2 = cf.createNew(cluster, "core-site",
      new HashMap<String, String>() {{
        put("key1", "value1");
      }}, new HashMap<String, Map<String,String>>());
    config2.setTag("version1");

    cluster.addConfig(config1);
    cluster.addConfig(config2);

    Service hdfs = cluster.addService("HDFS");
    hdfs.persist();

    Service mapred = cluster.addService("YARN");
    mapred.persist();

    hdfs.addServiceComponent(Role.HDFS_CLIENT.name()).persist();
    hdfs.addServiceComponent(Role.NAMENODE.name()).persist();
    hdfs.addServiceComponent(Role.DATANODE.name()).persist();

    mapred.addServiceComponent(Role.RESOURCEMANAGER.name()).persist();

    hdfs.getServiceComponent(Role.HDFS_CLIENT.name()).addServiceComponentHost("h1").persist();
    hdfs.getServiceComponent(Role.NAMENODE.name()).addServiceComponentHost("h1").persist();
    hdfs.getServiceComponent(Role.DATANODE.name()).addServiceComponentHost("h1").persist();
    hdfs.getServiceComponent(Role.DATANODE.name()).addServiceComponentHost("h2").persist();

    mapred.getServiceComponent(Role.RESOURCEMANAGER.name()).addServiceComponentHost("h2").persist();

    Map<String, String> params = new HashMap<String, String>() {{
      put("test", "test");
    }};

    Map<String, String> requestProperties = new HashMap<String, String>();
    requestProperties.put(REQUEST_CONTEXT_PROPERTY, "Called from a test");

    // Test multiple restarts
    List<RequestResourceFilter> resourceFilters = new ArrayList<RequestResourceFilter>();
    RequestResourceFilter resourceFilter = new RequestResourceFilter("HDFS",
      Role.DATANODE.name(), new ArrayList<String>() {{ add("h1"); add("h2"); }});
    resourceFilters.add(resourceFilter);
    resourceFilter = new RequestResourceFilter("YARN",
      Role.RESOURCEMANAGER.name(), new ArrayList<String>() {{ add("h2"); }});
    resourceFilters.add(resourceFilter);

    ExecuteActionRequest request = new ExecuteActionRequest("c1",
      "RESTART", null, resourceFilters, null, params, false);

    RequestStatusResponse response = controller.createAction(request, requestProperties);
    Assert.assertEquals(3, response.getTasks().size());
    List<HostRoleCommand> storedTasks = actionDB.getRequestTasks(response.getRequestId());

    Assert.assertNotNull(storedTasks);
    int expectedRestartCount = 0;
    for (HostRoleCommand hrc : storedTasks) {
      Assert.assertEquals("RESTART", hrc.getCustomCommandName());

      if (hrc.getHostName().equals("h1") && hrc.getRole().equals(Role.DATANODE)) {
        expectedRestartCount++;
      } else if(hrc.getHostName().equals("h2")) {
        if (hrc.getRole().equals(Role.DATANODE)) {
          expectedRestartCount++;
        } else if (hrc.getRole().equals(Role.RESOURCEMANAGER)) {
          expectedRestartCount++;
        }
      }
    }

    Assert.assertEquals("Restart 2 datanodes and 1 Resourcemanager.", 3,
        expectedRestartCount);
    Assert.assertEquals(requestProperties.get(REQUEST_CONTEXT_PROPERTY), response.getRequestContext());

    // Test service checks - specific host
    resourceFilters.clear();
    resourceFilter = new RequestResourceFilter("HDFS", null,
      new ArrayList<String>() {{ add("h1"); }});
    resourceFilters.add(resourceFilter);
    request = new ExecuteActionRequest("c1", Role.HDFS_SERVICE_CHECK.name(),
      null, resourceFilters, null, null, false);
    response = controller.createAction(request, requestProperties);

    Assert.assertEquals(1, response.getTasks().size());
    storedTasks = actionDB.getRequestTasks(response.getRequestId());
    Assert.assertNotNull(storedTasks);
    Assert.assertEquals(Role.HDFS_SERVICE_CHECK.name(),
        storedTasks.get(0).getRole().name());
    Assert.assertEquals("h1", storedTasks.get(0).getHostName());
    Assert.assertEquals(requestProperties.get(REQUEST_CONTEXT_PROPERTY), response.getRequestContext());
  }


  @Test
  public void testConfigsAttachedToServiceChecks() throws AmbariException {
    String clusterName = "foo1";
    createCluster(clusterName);
    Cluster cluster = clusters.getCluster(clusterName);
    cluster.setDesiredStackVersion(new StackId("HDP-0.1"));
    String serviceName = "HDFS";
    createService(clusterName, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";

    createServiceComponent(clusterName, serviceName, componentName1,
      State.INIT);
    createServiceComponent(clusterName, serviceName, componentName2,
      State.INIT);
    createServiceComponent(clusterName, serviceName, componentName3,
      State.INIT);

    String host1 = "h1";
    String host2 = "h2";

    addHostToCluster(host1, clusterName);
    addHostToCluster(host2, clusterName);


    // null service should work
    createServiceComponentHost(clusterName, null, componentName1,
      host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName2,
      host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName2,
      host2, null);
    createServiceComponentHost(clusterName, serviceName, componentName3,
      host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName3,
      host2, null);

    // Create and attach config
    Map<String, String> configs = new HashMap<String, String>();
    configs.put("a", "b");

    ConfigurationRequest cr1,cr2;
    cr1 = new ConfigurationRequest(clusterName, "core-site","version1",
      configs, null);
    cr2 = new ConfigurationRequest(clusterName, "hdfs-site","version1",
      configs, null);

    ClusterRequest crReq = new ClusterRequest(cluster.getClusterId(), clusterName, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr1));
    controller.updateClusters(Collections.singleton(crReq), null);
    crReq = new ClusterRequest(cluster.getClusterId(), clusterName, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr2));
    controller.updateClusters(Collections.singleton(crReq), null);

    // Install
    installService(clusterName, serviceName, false, false);
    // Start
    long requestId = startService(clusterName, serviceName, true, false);

    List<Stage> stages = actionDB.getAllStages(requestId);
    boolean serviceCheckFound = false;
    for (Stage stage : stages) {
      for (HostRoleCommand hrc : stage.getOrderedHostRoleCommands()) {
        if (hrc.getRole().equals(Role.HDFS_SERVICE_CHECK)) {
          serviceCheckFound = true;
          Assert.assertEquals(2, hrc.getExecutionCommandWrapper()
            .getExecutionCommand().getConfigurationTags().size());
        }
      }
    }

    Type type = new TypeToken<Map<String, String>>(){}.getType();
    for (Stage stage : actionDB.getAllStages(requestId)){
      Map<String, String> hostParamsStage = StageUtils.getGson().fromJson(stage.getHostParamsStage(), type);
      Assert.assertNotNull(hostParamsStage.get("jdk_location"));
    }

    Assert.assertEquals(true, serviceCheckFound);
  }

  @Test
  @Ignore("Unsuported feature !")
  public void testConfigsAttachedToServiceNotCluster() throws AmbariException {
    String clusterName = "foo1";
    createCluster(clusterName);
    clusters.getCluster(clusterName).setDesiredStackVersion(new StackId("HDP-0.1"));

    String serviceName = "HDFS";
    createService(clusterName, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";

    createServiceComponent(clusterName, serviceName, componentName1,
      State.INIT);
    createServiceComponent(clusterName, serviceName, componentName2,
      State.INIT);
    createServiceComponent(clusterName, serviceName, componentName3,
      State.INIT);

    String host1 = "h1";
    String host2 = "h2";

    addHostToCluster(host1, clusterName);
    addHostToCluster(host2, clusterName);


    // null service should work
    createServiceComponentHost(clusterName, null, componentName1,
      host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName2,
      host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName2,
      host2, null);
    createServiceComponentHost(clusterName, serviceName, componentName3,
      host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName3,
      host2, null);

    // Create and attach config
    Map<String, String> configs = new HashMap<String, String>();
    configs.put("a", "b");

    ConfigurationRequest cr1,cr2;
    cr1 = new ConfigurationRequest(clusterName, "core-site","version1",
      configs, null);
    cr2 = new ConfigurationRequest(clusterName, "hdfs-site","version1",
      configs, null);

    // create, but don't assign
    controller.createConfiguration(cr1);
    controller.createConfiguration(cr2);

    Map<String,String> configVersions = new HashMap<String,String>() {{
      put("core-site", "version1");
      put("hdfs-site", "version1");
    }};
    ServiceRequest sr = new ServiceRequest(clusterName, serviceName, null);
    ServiceResourceProviderTest.updateServices(controller, Collections.singleton(sr), new HashMap<String,String>(), false, false);

    // Install
    installService(clusterName, serviceName, false, false);
    // Start
    long requestId = startService(clusterName, serviceName, true, false);

    Assert.assertEquals(0, clusters.getCluster(clusterName).getDesiredConfigs().size());

    List<Stage> stages = actionDB.getAllStages(requestId);
    boolean serviceCheckFound = false;
    for (Stage stage : stages) {
      for (HostRoleCommand hrc : stage.getOrderedHostRoleCommands()) {
        if (hrc.getRole().equals(Role.HDFS_SERVICE_CHECK)) {
          serviceCheckFound = true;
          Assert.assertEquals(2, hrc.getExecutionCommandWrapper()
            .getExecutionCommand().getConfigurationTags().size());
        }
      }
    }
    Assert.assertEquals(true, serviceCheckFound);
  }

  @Test
  public void testHostLevelParamsSentWithCommands() throws AmbariException {
    String clusterName = "foo1";
    createCluster(clusterName);
    clusters.getCluster(clusterName)
      .setDesiredStackVersion(new StackId("HDP-0.1"));
    String serviceName = "PIG";
    createService(clusterName, serviceName, null);
    String componentName1 = "PIG";
    createServiceComponent(clusterName, serviceName, componentName1,
      State.INIT);

    String host1 = "h1";
    String host2 = "h2";

    addHostToCluster(host1, clusterName);
    addHostToCluster(host2, clusterName);

    Map<String, String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "Called from a test");

    // null service should work
    createServiceComponentHost(clusterName, null, componentName1,
      host1, null);
    createServiceComponentHost(clusterName, null, componentName1,
      host2, null);



    ServiceRequest r = new ServiceRequest(clusterName, serviceName, State.INSTALLED.toString());
    Set<ServiceRequest> requests = new HashSet<ServiceRequest>();
    requests.add(r);

    RequestStatusResponse trackAction =
      ServiceResourceProviderTest.updateServices(controller, requests, mapRequestProps, true, false);
    Assert.assertEquals(State.INSTALLED,
      clusters.getCluster(clusterName).getService(serviceName)
        .getDesiredState());

    List<Stage> stages = actionDB.getAllStages(trackAction.getRequestId());
    Type type = new TypeToken<Map<String, String>>(){}.getType();

    for (Stage stage : stages){
      Map<String, String> params = StageUtils.getGson().fromJson(stage.getHostParamsStage(), type);
      Assert.assertEquals("0.1", params.get("stack_version"));
      Assert.assertNotNull(params.get("jdk_location"));
      Assert.assertNotNull(params.get("db_name"));
      Assert.assertNotNull(params.get("mysql_jdbc_url"));
      Assert.assertNotNull(params.get("oracle_jdbc_url"));
    }

    Map<String, String> paramsCmd = stages.get(0).getOrderedHostRoleCommands().get
      (0).getExecutionCommandWrapper().getExecutionCommand()
      .getHostLevelParams();
    Assert.assertNotNull(paramsCmd.get("repo_info"));
    Assert.assertNotNull(paramsCmd.get("clientsToUpdateConfigs"));
  }

  @Test
  public void testConfigGroupOverridesWithHostActions() throws AmbariException {
    String clusterName = "foo1";
    createCluster(clusterName);
    Cluster cluster = clusters.getCluster(clusterName);
    cluster.setDesiredStackVersion(new StackId("HDP-2.0.6"));
    String serviceName1 = "HDFS";
    String serviceName2 = "MAPREDUCE2";
    createService(clusterName, serviceName1, null);
    createService(clusterName, serviceName2, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";
    String componentName4 = "HISTORYSERVER";

    createServiceComponent(clusterName, serviceName1, componentName1,
      State.INIT);
    createServiceComponent(clusterName, serviceName1, componentName2,
      State.INIT);
    createServiceComponent(clusterName, serviceName1, componentName3,
      State.INIT);
    createServiceComponent(clusterName, serviceName2, componentName4,
      State.INIT);

    String host1 = "h1";
    String host2 = "h2";
    String host3 = "h3";

    addHostToCluster(host1, clusterName);
    addHostToCluster(host2, clusterName);
    addHostToCluster(host3, clusterName);

    createServiceComponentHost(clusterName, serviceName1, componentName1,
      host1, null);
    createServiceComponentHost(clusterName, serviceName1, componentName2,
      host2, null);
    createServiceComponentHost(clusterName, serviceName1, componentName3,
      host2, null);
    createServiceComponentHost(clusterName, serviceName1, componentName3,
      host3, null);
    createServiceComponentHost(clusterName, serviceName2, componentName4,
      host3, null);

    // Create and attach config
    Map<String, String> configs = new HashMap<String, String>();
    configs.put("a", "b");

    ConfigurationRequest cr1,cr2,cr3;
    cr1 = new ConfigurationRequest(clusterName, "core-site","version1",
      configs, null);
    cr2 = new ConfigurationRequest(clusterName, "hdfs-site","version1",
      configs, null);
    cr3 = new ConfigurationRequest(clusterName, "mapred-site","version1",
      configs, null);

    ClusterRequest crReq = new ClusterRequest(cluster.getClusterId(), clusterName, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr1));
    controller.updateClusters(Collections.singleton(crReq), null);
    crReq = new ClusterRequest(cluster.getClusterId(), clusterName, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr2));
    controller.updateClusters(Collections.singleton(crReq), null);
    crReq = new ClusterRequest(cluster.getClusterId(), clusterName, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr3));
    controller.updateClusters(Collections.singleton(crReq), null);

    // Create Config group for core-site
    configs = new HashMap<String, String>();
    configs.put("a", "c");
    cluster = clusters.getCluster(clusterName);
    final Config config = new ConfigImpl("core-site");
    config.setProperties(configs);
    config.setTag("version122");
    Long groupId = createConfigGroup(cluster, "g1", "t1",
      new ArrayList<String>() {{ add("h1"); }},
      new ArrayList<Config>() {{ add(config); }});

    Assert.assertNotNull(groupId);

    // Create Config group for mapred-site
    configs = new HashMap<String, String>();
    configs.put("a", "c");

    final Config config2 = new ConfigImpl("mapred-site");
    config2.setProperties(configs);
    config2.setTag("version122");
    groupId = createConfigGroup(cluster, "g2", "t2",
      new ArrayList<String>() {{ add("h1"); }},
      new ArrayList<Config>() {{ add(config2); }});

    Assert.assertNotNull(groupId);

    // Install
    Long requestId = installService(clusterName, serviceName1, false, false);
    HostRoleCommand namenodeInstall = null;
    HostRoleCommand clientInstall = null;
    HostRoleCommand slaveInstall = null;
    for (Stage stage : actionDB.getAllStages(requestId)) {
      for (HostRoleCommand hrc : stage.getOrderedHostRoleCommands()) {
        if (hrc.getRole().equals(Role.NAMENODE) && hrc.getHostName().equals("h1")) {
          namenodeInstall = hrc;
        } else if (hrc.getRole().equals(Role.HDFS_CLIENT) && hrc.getHostName()
            .equals("h3")) {
          clientInstall = hrc;
        } else if (hrc.getRole().equals(Role.DATANODE) && hrc.getHostName()
            .equals("h2")) {
          slaveInstall = hrc;
        }
      }
    }

    Assert.assertNotNull(namenodeInstall);
    Assert.assertNotNull(clientInstall);
    Assert.assertNotNull(slaveInstall);
    Assert.assertTrue(namenodeInstall.getExecutionCommandWrapper()
      .getExecutionCommand().getConfigurations().get("core-site").containsKey("a"));
    Assert.assertEquals("c", namenodeInstall.getExecutionCommandWrapper()
      .getExecutionCommand().getConfigurations().get("core-site").get("a"));

    // Slave and client should not have the override
    Assert.assertTrue(clientInstall.getExecutionCommandWrapper()
      .getExecutionCommand().getConfigurations().get("core-site").containsKey("a"));
    Assert.assertEquals("b", clientInstall.getExecutionCommandWrapper()
      .getExecutionCommand().getConfigurations().get("core-site").get("a"));
    Assert.assertTrue(slaveInstall.getExecutionCommandWrapper()
      .getExecutionCommand().getConfigurations().get("core-site").containsKey("a"));
    Assert.assertEquals("b", slaveInstall.getExecutionCommandWrapper()
      .getExecutionCommand().getConfigurations().get("core-site").get("a"));

    startService(clusterName, serviceName1, false, false);

    requestId = installService(clusterName, serviceName2, false, false);
    HostRoleCommand mapredInstall = null;
    for (Stage stage : actionDB.getAllStages(requestId)) {
      for (HostRoleCommand hrc : stage.getOrderedHostRoleCommands()) {
        if (hrc.getRole().equals(Role.HISTORYSERVER) && hrc.getHostName()
            .equals("h3")) {
          mapredInstall = hrc;
        }
      }
    }
    Assert.assertNotNull(mapredInstall);
    // Config group not associated with host
    Assert.assertEquals("b", mapredInstall.getExecutionCommandWrapper()
      .getExecutionCommand().getConfigurations().get("mapred-site").get("a"));

    // Associate the right host
    ConfigGroup configGroup = cluster.getConfigGroups().get(groupId);
    configGroup.setHosts(new HashMap<Long, Host>() {{ put(3L,
      clusters.getHost("h3")); }});
    configGroup.persist();

    requestId = startService(clusterName, serviceName2, false, false);
    mapredInstall = null;
    for (Stage stage : actionDB.getAllStages(requestId)) {
      for (HostRoleCommand hrc : stage.getOrderedHostRoleCommands()) {
        if (hrc.getRole().equals(Role.HISTORYSERVER) && hrc.getHostName()
            .equals("h3")) {
          mapredInstall = hrc;
        }
      }
    }
    Assert.assertNotNull(mapredInstall);
    Assert.assertEquals("c", mapredInstall.getExecutionCommandWrapper()
      .getExecutionCommand().getConfigurations().get("mapred-site").get("a"));

  }

  @Test
  public void testConfigGroupOverridesWithDecommissionDatanode() throws AmbariException {
    String clusterName = "foo1";
    createCluster(clusterName);
    Cluster cluster = clusters.getCluster(clusterName);
    cluster.setDesiredStackVersion(new StackId("HDP-2.0.7"));
    String serviceName = "HDFS";
    createService(clusterName, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";

    createServiceComponent(clusterName, serviceName, componentName1,
        State.INIT);
    createServiceComponent(clusterName, serviceName, componentName2,
        State.INIT);
    createServiceComponent(clusterName, serviceName, componentName3,
        State.INIT);

    String host1 = "h1";
    String host2 = "h2";

    addHostToCluster(host1, clusterName);
    addHostToCluster(host2, clusterName);

    createServiceComponentHost(clusterName, serviceName, componentName1,
        host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName2,
        host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName2,
        host2, null);
    createServiceComponentHost(clusterName, serviceName, componentName3,
        host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName3,
        host2, null);

    // Install
    installService(clusterName, serviceName, false, false);

    // Create and attach config
    Map<String, String> configs = new HashMap<String, String>();
    configs.put("a", "b");

    ConfigurationRequest cr1, cr2;
    cr1 = new ConfigurationRequest(clusterName, "hdfs-site", "version1",
        configs, null);
    ClusterRequest crReq = new ClusterRequest(cluster.getClusterId(), clusterName, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr1));
    controller.updateClusters(Collections.singleton(crReq), null);

    // Start
    startService(clusterName, serviceName, false, false);

    // Create Config group for hdfs-site
    configs = new HashMap<String, String>();
    configs.put("a", "c");

    final Config config = new ConfigImpl("hdfs-site");
    config.setProperties(configs);
    config.setTag("version122");
    Long groupId = createConfigGroup(clusters.getCluster(clusterName), "g1", "t1",
        new ArrayList<String>() {{
          add("h1");
          add("h2");
        }},
        new ArrayList<Config>() {{
          add(config);
        }}
    );

    Assert.assertNotNull(groupId);

    cluster = clusters.getCluster(clusterName);
    Service s = cluster.getService(serviceName);
    Assert.assertEquals(State.STARTED, s.getDesiredState());

    Map<String, String> params = new HashMap<String, String>() {{
      put("test", "test");
      put("excluded_hosts", " h1 ");
    }};
    RequestResourceFilter resourceFilter = new RequestResourceFilter("HDFS", "NAMENODE", null);
    ExecuteActionRequest request = new ExecuteActionRequest(clusterName, "DECOMMISSION", params, false);
    request.getResourceFilters().add(resourceFilter);

    Map<String, String> requestProperties = new HashMap<String, String>();
    requestProperties.put(REQUEST_CONTEXT_PROPERTY, "Called from a test");

    RequestStatusResponse response = controller.createAction(request,
        requestProperties);

    List<HostRoleCommand> storedTasks = actionDB.getRequestTasks(response.getRequestId());
    ExecutionCommand execCmd = storedTasks.get(0).getExecutionCommandWrapper
        ().getExecutionCommand();
    Assert.assertNotNull(storedTasks);
    Assert.assertNotNull(execCmd.getConfigurationTags().get("hdfs-site"));
    Assert.assertEquals(1, storedTasks.size());
    HostRoleCommand command =  storedTasks.get(0);
    Assert.assertEquals(Role.NAMENODE, command.getRole());
    Assert.assertEquals(RoleCommand.CUSTOM_COMMAND, command.getRoleCommand());
    Assert.assertEquals("DECOMMISSION", execCmd.getHostLevelParams().get("custom_command"));
    Assert.assertEquals(requestProperties.get(REQUEST_CONTEXT_PROPERTY), response.getRequestContext());
  }

  @Test
  public void testConfigGroupOverridesWithServiceCheckActions() throws AmbariException {
    String clusterName = "foo1";
    createCluster(clusterName);
    Cluster cluster = clusters.getCluster(clusterName);
    cluster.setDesiredStackVersion(new StackId("HDP-0.1"));
    String serviceName = "HDFS";
    createService(clusterName, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";

    createServiceComponent(clusterName, serviceName, componentName1,
      State.INIT);
    createServiceComponent(clusterName, serviceName, componentName2,
      State.INIT);
    createServiceComponent(clusterName, serviceName, componentName3,
      State.INIT);

    String host1 = "h1";
    String host2 = "h2";

    addHostToCluster(host1, clusterName);
    addHostToCluster(host2, clusterName);

    // null service should work
    createServiceComponentHost(clusterName, null, componentName1,
      host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName2,
      host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName2,
      host2, null);
    createServiceComponentHost(clusterName, serviceName, componentName3,
      host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName3,
      host2, null);

    // Create and attach config
    Map<String, String> configs = new HashMap<String, String>();
    configs.put("a", "b");

    ConfigurationRequest cr1,cr2;
    cr1 = new ConfigurationRequest(clusterName, "core-site","version1",
      configs, null);
    cr2 = new ConfigurationRequest(clusterName, "hdfs-site","version1",
      configs, null);

    ClusterRequest crReq = new ClusterRequest(cluster.getClusterId(), clusterName, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr1));
    controller.updateClusters(Collections.singleton(crReq), null);
    crReq = new ClusterRequest(cluster.getClusterId(), clusterName, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr2));
    controller.updateClusters(Collections.singleton(crReq), null);

    // Install
    installService(clusterName, serviceName, false, false);

    // Create Config group for hdfs-site
    configs = new HashMap<String, String>();
    configs.put("a", "c");

    final Config config = new ConfigImpl("hdfs-site");
    config.setProperties(configs);
    config.setTag("version122");
    Long groupId = createConfigGroup(clusters.getCluster(clusterName), "g1", "t1",
      new ArrayList<String>() {{ add("h1"); add("h2"); }},
      new ArrayList<Config>() {{ add(config); }});

    Assert.assertNotNull(groupId);

    // Start
    long requestId = startService(clusterName, serviceName, true, false);
    HostRoleCommand smokeTestCmd = null;
    List<Stage> stages = actionDB.getAllStages(requestId);
    for (Stage stage : stages) {
      for (HostRoleCommand hrc : stage.getOrderedHostRoleCommands()) {
        if (hrc.getRole().equals(Role.HDFS_SERVICE_CHECK)) {
          Assert.assertEquals(2, hrc.getExecutionCommandWrapper()
            .getExecutionCommand().getConfigurationTags().size());
          smokeTestCmd = hrc;
        }
      }
    }
    Assert.assertNotNull(smokeTestCmd);
    Assert.assertEquals("c", smokeTestCmd.getExecutionCommandWrapper()
      .getExecutionCommand().getConfigurations().get("hdfs-site").get("a"));
  }

  @Test
  public void testGetStacks() throws Exception {


    StackRequest request = new StackRequest(null);
    Set<StackResponse> responses = controller.getStacks(Collections.singleton(request));
    Assert.assertEquals(STACKS_CNT, responses.size());

    StackRequest requestWithParams = new StackRequest(STACK_NAME);
    Set<StackResponse> responsesWithParams = controller.getStacks(Collections.singleton(requestWithParams));
    Assert.assertEquals(1, responsesWithParams.size());
    for (StackResponse responseWithParams: responsesWithParams) {
      Assert.assertEquals(responseWithParams.getStackName(), STACK_NAME);

    }

    StackRequest invalidRequest = new StackRequest(NON_EXT_VALUE);
    try {
      controller.getStacks(Collections.singleton(invalidRequest));
    } catch (StackAccessException e) {
      // do nothing
    }
  }

  @Test
  public void testGetStackVersions() throws Exception {


    StackVersionRequest request = new StackVersionRequest(STACK_NAME, null);
    Set<StackVersionResponse> responses = controller.getStackVersions(Collections.singleton(request));
    Assert.assertEquals(STACK_VERSIONS_CNT, responses.size());

    StackVersionRequest requestWithParams = new StackVersionRequest(STACK_NAME, STACK_VERSION);
    Set<StackVersionResponse> responsesWithParams = controller.getStackVersions(Collections.singleton(requestWithParams));
    Assert.assertEquals(1, responsesWithParams.size());
    for (StackVersionResponse responseWithParams: responsesWithParams) {
      Assert.assertEquals(responseWithParams.getStackVersion(), STACK_VERSION);
    }

    StackVersionRequest invalidRequest = new StackVersionRequest(STACK_NAME, NON_EXT_VALUE);
    try {
      controller.getStackVersions(Collections.singleton(invalidRequest));
    } catch (StackAccessException e) {
      // do nothing
    }

    // test that a stack response has upgrade packs
    requestWithParams = new StackVersionRequest(STACK_NAME, "2.1.1");
    responsesWithParams = controller.getStackVersions(Collections.singleton(requestWithParams));

    Assert.assertEquals(1, responsesWithParams.size());
    StackVersionResponse resp = responsesWithParams.iterator().next();
    assertNotNull(resp.getUpgradePacks());
    assertEquals(8, resp.getUpgradePacks().size());
    assertTrue(resp.getUpgradePacks().contains("upgrade_test"));
  }

  @Test
  public void testGetStackVersionActiveAttr() throws Exception {

    for (StackInfo stackInfo: ambariMetaInfo.getStacks(STACK_NAME)) {
      if (stackInfo.getVersion().equalsIgnoreCase(STACK_VERSION)) {
        stackInfo.setActive(true);
      }
    }

    StackVersionRequest requestWithParams = new StackVersionRequest(STACK_NAME, STACK_VERSION);
    Set<StackVersionResponse> responsesWithParams = controller.getStackVersions(Collections.singleton(requestWithParams));
    Assert.assertEquals(1, responsesWithParams.size());
    for (StackVersionResponse responseWithParams: responsesWithParams) {
      Assert.assertTrue(responseWithParams.isActive());
    }
  }

  @Test
  public void testGetRepositories() throws Exception {

    RepositoryRequest request = new RepositoryRequest(STACK_NAME, STACK_VERSION, OS_TYPE, null);
    Set<RepositoryResponse> responses = controller.getRepositories(Collections.singleton(request));
    Assert.assertEquals(REPOS_CNT, responses.size());

    RepositoryRequest requestWithParams = new RepositoryRequest(STACK_NAME, STACK_VERSION, OS_TYPE, REPO_ID);
    Set<RepositoryResponse> responsesWithParams = controller.getRepositories(Collections.singleton(requestWithParams));
    Assert.assertEquals(1, responsesWithParams.size());
    for (RepositoryResponse responseWithParams: responsesWithParams) {
      Assert.assertEquals(responseWithParams.getRepoId(), REPO_ID);

    }

    RepositoryRequest invalidRequest = new RepositoryRequest(STACK_NAME, STACK_VERSION, OS_TYPE, NON_EXT_VALUE);
    try {
      controller.getRepositories(Collections.singleton(invalidRequest));
    } catch (StackAccessException e) {
      // do nothing
    }
  }


  @Test
  public void testGetStackServices() throws Exception {
    StackServiceRequest request = new StackServiceRequest(STACK_NAME, NEW_STACK_VERSION, null);
    Set<StackServiceResponse> responses = controller.getStackServices(Collections.singleton(request));
    Assert.assertEquals(11, responses.size());


    StackServiceRequest requestWithParams = new StackServiceRequest(STACK_NAME, NEW_STACK_VERSION, SERVICE_NAME);
    Set<StackServiceResponse> responsesWithParams = controller.getStackServices(Collections.singleton(requestWithParams));
    Assert.assertEquals(1, responsesWithParams.size());
    for (StackServiceResponse responseWithParams: responsesWithParams) {
      Assert.assertEquals(responseWithParams.getServiceName(), SERVICE_NAME);
      Assert.assertTrue(responseWithParams.getConfigTypes().size() > 0);
    }


    StackServiceRequest invalidRequest = new StackServiceRequest(STACK_NAME, NEW_STACK_VERSION, NON_EXT_VALUE);
    try {
      controller.getStackServices(Collections.singleton(invalidRequest));
    } catch (StackAccessException e) {
      // do nothing
    }
  }

  @Test
  public void testConfigInComponent() throws Exception {
    StackServiceRequest requestWithParams = new StackServiceRequest(STACK_NAME, "2.0.6", "YARN");
    Set<StackServiceResponse> responsesWithParams = controller.getStackServices(Collections.singleton(requestWithParams));

    Assert.assertEquals(1, responsesWithParams.size());

    for (StackServiceResponse responseWithParams: responsesWithParams) {
      Assert.assertEquals(responseWithParams.getServiceName(), "YARN");
      Assert.assertTrue(responseWithParams.getConfigTypes().containsKey("capacity-scheduler"));
    }
  }

  @Test
  public void testGetStackConfigurations() throws Exception {
    StackConfigurationRequest request = new StackConfigurationRequest(STACK_NAME, STACK_VERSION, SERVICE_NAME, null);
    Set<StackConfigurationResponse> responses = controller.getStackConfigurations(Collections.singleton(request));
    Assert.assertEquals(STACK_PROPERTIES_CNT, responses.size());


    StackConfigurationRequest requestWithParams = new StackConfigurationRequest(STACK_NAME, STACK_VERSION, SERVICE_NAME, PROPERTY_NAME);
    Set<StackConfigurationResponse> responsesWithParams = controller.getStackConfigurations(Collections.singleton(requestWithParams));
    Assert.assertEquals(1, responsesWithParams.size());
    for (StackConfigurationResponse responseWithParams: responsesWithParams) {
      Assert.assertEquals(responseWithParams.getPropertyName(), PROPERTY_NAME);

    }

    StackConfigurationRequest invalidRequest = new StackConfigurationRequest(STACK_NAME, STACK_VERSION, SERVICE_NAME, NON_EXT_VALUE);
    try {
      controller.getStackConfigurations(Collections.singleton(invalidRequest));
    } catch (StackAccessException e) {
      // do nothing
    }
  }


  @Test
  public void testGetStackComponents() throws Exception {
    StackServiceComponentRequest request = new StackServiceComponentRequest(STACK_NAME, STACK_VERSION, SERVICE_NAME, null);
    Set<StackServiceComponentResponse> responses = controller.getStackComponents(Collections.singleton(request));
    Assert.assertEquals(STACK_COMPONENTS_CNT, responses.size());


    StackServiceComponentRequest requestWithParams = new StackServiceComponentRequest(STACK_NAME, STACK_VERSION, SERVICE_NAME, COMPONENT_NAME);
    Set<StackServiceComponentResponse> responsesWithParams = controller.getStackComponents(Collections.singleton(requestWithParams));
    Assert.assertEquals(1, responsesWithParams.size());
    for (StackServiceComponentResponse responseWithParams: responsesWithParams) {
      Assert.assertEquals(responseWithParams.getComponentName(), COMPONENT_NAME);

    }

    StackServiceComponentRequest invalidRequest = new StackServiceComponentRequest(STACK_NAME, STACK_VERSION, SERVICE_NAME, NON_EXT_VALUE);
    try {
      controller.getStackComponents(Collections.singleton(invalidRequest));
    } catch (StackAccessException e) {
      // do nothing
    }
  }

  @Test
  public void testGetStackOperatingSystems() throws Exception {
    OperatingSystemRequest request = new OperatingSystemRequest(STACK_NAME, STACK_VERSION, null);
    Set<OperatingSystemResponse> responses = controller.getOperatingSystems(Collections.singleton(request));
    Assert.assertEquals(OS_CNT, responses.size());


    OperatingSystemRequest requestWithParams = new OperatingSystemRequest(STACK_NAME, STACK_VERSION, OS_TYPE);
    Set<OperatingSystemResponse> responsesWithParams = controller.getOperatingSystems(Collections.singleton(requestWithParams));
    Assert.assertEquals(1, responsesWithParams.size());
    for (OperatingSystemResponse responseWithParams: responsesWithParams) {
      Assert.assertEquals(responseWithParams.getOsType(), OS_TYPE);

    }

    OperatingSystemRequest invalidRequest = new OperatingSystemRequest(STACK_NAME, STACK_VERSION, NON_EXT_VALUE);
    try {
      controller.getOperatingSystems(Collections.singleton(invalidRequest));
    } catch (StackAccessException e) {
      // do nothing
    }
  }

  @Test
  public void testStackServiceCheckSupported() throws Exception {
    StackServiceRequest hdfsServiceRequest = new StackServiceRequest(
        STACK_NAME, "2.0.8", SERVICE_NAME);

    Set<StackServiceResponse> responses = controller.getStackServices(Collections.singleton(hdfsServiceRequest));
    Assert.assertEquals(1, responses.size());

    StackServiceResponse response = responses.iterator().next();
    assertTrue(response.isServiceCheckSupported());

    StackServiceRequest fakeServiceRequest = new StackServiceRequest(
        STACK_NAME, "2.0.8", FAKE_SERVICE_NAME);

    responses = controller.getStackServices(Collections.singleton(fakeServiceRequest));
    Assert.assertEquals(1, responses.size());

    response = responses.iterator().next();
    assertFalse(response.isServiceCheckSupported());
  }

  @Test
  public void testStackServiceComponentCustomCommands() throws Exception {
    StackServiceComponentRequest namenodeRequest = new StackServiceComponentRequest(
        STACK_NAME, NEW_STACK_VERSION, SERVICE_NAME, COMPONENT_NAME);

    Set<StackServiceComponentResponse> responses = controller.getStackComponents(Collections.singleton(namenodeRequest));
    Assert.assertEquals(1, responses.size());

    StackServiceComponentResponse response = responses.iterator().next();
    assertNotNull(response.getCustomCommands());
    assertEquals(2, response.getCustomCommands().size());
    assertEquals("DECOMMISSION", response.getCustomCommands().get(0));
    assertEquals("REBALANCEHDFS", response.getCustomCommands().get(1));

    StackServiceComponentRequest journalNodeRequest = new StackServiceComponentRequest(
        STACK_NAME, NEW_STACK_VERSION, SERVICE_NAME, "JOURNALNODE");

    responses = controller.getStackComponents(Collections.singleton(journalNodeRequest));
    Assert.assertEquals(1, responses.size());

    response = responses.iterator().next();
    assertNotNull(response.getCustomCommands());
    assertEquals(0, response.getCustomCommands().size());
  }

  // disabled as upgrade feature is disabled
  @Ignore
  @Test
  public void testUpdateClusterVersionBasic() throws AmbariException {
    String clusterName = "foo1";
    String serviceName = "MAPREDUCE";
    String host1 = "h1";
    String host2 = "h2";
    String componentName = "JOBTRACKER";
    StackId currentStackId = new StackId("HDP-0.1");

    Map<String, String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "Called from a test");

    createCluster(clusterName);
    Cluster c = clusters.getCluster(clusterName);
    c.setDesiredStackVersion(currentStackId);
    createService(clusterName, serviceName, State.INIT);
    createServiceComponent(clusterName, serviceName, componentName, null);

    addHostToCluster(host1, clusterName);
    addHostToCluster(host2, clusterName);

    createServiceComponentHost(clusterName, null, componentName,
        host1, null);
    createServiceComponentHost(clusterName, null, componentName,
        host2, null);

    c.getService(serviceName).setDesiredState(State.INSTALLED);
    c.getService(serviceName).getServiceComponent(componentName).setDesiredState(State.INSTALLED);
    c.getService(serviceName).getServiceComponent(componentName).getServiceComponentHost(host1)
        .setDesiredState(State.INSTALLED);
    c.getService(serviceName).getServiceComponent(componentName).getServiceComponentHost(host2)
        .setDesiredState(State.STARTED);
    c.getService(serviceName).getServiceComponent(componentName).getServiceComponentHost(host1)
        .setState(State.INSTALLED);
    c.getService(serviceName).getServiceComponent(componentName).getServiceComponentHost(host2)
        .setState(State.STARTED);
    c.getService(serviceName).getServiceComponent(componentName).getServiceComponentHost(host1)
        .setStackVersion(currentStackId);
    c.getService(serviceName).getServiceComponent(componentName).getServiceComponentHost(host2)
        .setStackVersion(currentStackId);

    ClusterRequest r = new ClusterRequest(c.getClusterId(), clusterName, "HDP-0.0.1", null);
    try {
      controller.updateClusters(Collections.singleton(r), mapRequestProps);
      fail("Update cluster should fail");
    } catch (AmbariException e) {
      Assert.assertTrue(e.getMessage().contains("must be greater than current version"));
    }

    r = new ClusterRequest(c.getClusterId(), clusterName, "HDPLocal-1.2.2", null);
    try {
      controller.updateClusters(Collections.singleton(r), mapRequestProps);
      fail("Update cluster should fail");
    } catch (AmbariException e) {
      Assert.assertTrue(e.getMessage().contains("Upgrade not possible between different stacks"));
    }

    r = new ClusterRequest(c.getClusterId(), clusterName, "HDP-0.2", null);
    try {
      controller.updateClusters(Collections.singleton(r), mapRequestProps);
      fail("Update cluster should fail");
    } catch (AmbariException e) {
      Assert.assertTrue(e.getMessage().contains("Upgrade needs all services to be stopped"));
      Assert.assertTrue(e.getMessage().contains(serviceName));
    }

    c.getService(serviceName).getServiceComponent(componentName).getServiceComponentHost(host2)
        .setDesiredState(State.INSTALLED);

    r = new ClusterRequest(c.getClusterId(), clusterName, "HDP-0.2", null);
    try {
      controller.updateClusters(Collections.singleton(r), mapRequestProps);
      fail("Update cluster should fail");
    } catch (AmbariException e) {
      Assert.assertTrue(e.getMessage().contains("Upgrade needs all services to be stopped"));
      Assert.assertTrue(e.getMessage().contains(componentName));
    }

    c.getService(serviceName).getServiceComponent(componentName).getServiceComponentHost(host2)
        .setState(State.INSTALLED);
    controller.updateClusters(Collections.singleton(r), mapRequestProps);
    StackId expectedStackId = new StackId("HDP-0.2");
    Assert.assertTrue(expectedStackId.equals(c.getDesiredStackVersion()));
    Assert.assertTrue(expectedStackId.equals(c.getService(serviceName).getDesiredStackVersion()));
    Assert.assertTrue(expectedStackId.equals(c.getService(serviceName)
        .getServiceComponent(componentName).getDesiredStackVersion()));
    Assert.assertTrue(expectedStackId.equals(c.getService(serviceName)
        .getServiceComponent(componentName).getServiceComponentHost(host1).getDesiredStackVersion()));
    Assert.assertTrue(expectedStackId.equals(c.getService(serviceName)
        .getServiceComponent(componentName).getServiceComponentHost(host2).getDesiredStackVersion()));
    Assert.assertTrue(currentStackId.equals(c.getService(serviceName)
        .getServiceComponent(componentName).getServiceComponentHost(host1).getStackVersion()));
    Assert.assertTrue(currentStackId.equals(c.getService(serviceName)
        .getServiceComponent(componentName).getServiceComponentHost(host2).getStackVersion()));
    ServiceComponent sc = c.getService(serviceName).getServiceComponent(componentName);
    Assert.assertEquals(State.UPGRADING, sc.getServiceComponentHost(host1).getState());
    Assert.assertEquals(State.UPGRADING, sc.getServiceComponentHost(host2).getState());

    // Fail as another request is active
    try {
      controller.updateClusters(Collections.singleton(r), mapRequestProps);
      fail("Update cluster should fail");
    } catch (AmbariException e) {
      Assert.assertTrue(e.getMessage().contains("A prior upgrade request with id"));
    }

    // cases where there is no update required
    c.getService(serviceName).getServiceComponent(componentName).getServiceComponentHost(host1)
        .setDesiredState(State.INSTALLED);
    c.getService(serviceName).getServiceComponent(componentName).getServiceComponentHost(host2)
        .setDesiredState(State.INSTALLED);
    c.getService(serviceName).getServiceComponent(componentName).getServiceComponentHost(host1)
        .setState(State.INSTALLED);
    c.getService(serviceName).getServiceComponent(componentName).getServiceComponentHost(host2)
        .setState(State.INSTALLED);
    c.setCurrentStackVersion(expectedStackId);
    r = new ClusterRequest(c.getClusterId(), clusterName, "", null);
    controller.updateClusters(Collections.singleton(r), mapRequestProps);
    Assert.assertEquals(State.INSTALLED, sc.getServiceComponentHost(host1).getState());
    Assert.assertEquals(State.INSTALLED, sc.getServiceComponentHost(host2).getState());

    r = new ClusterRequest(c.getClusterId(), clusterName, null, null);
    controller.updateClusters(Collections.singleton(r), mapRequestProps);
    Assert.assertEquals(State.INSTALLED, sc.getServiceComponentHost(host1).getState());
    Assert.assertEquals(State.INSTALLED, sc.getServiceComponentHost(host2).getState());

    r = new ClusterRequest(c.getClusterId(), clusterName, "HDP-0.2", null);
    controller.updateClusters(Collections.singleton(r), mapRequestProps);
    Assert.assertEquals(State.INSTALLED, sc.getServiceComponentHost(host1).getState());
    Assert.assertEquals(State.INSTALLED, sc.getServiceComponentHost(host2).getState());
  }

  @Test
  public void testUpdateClusterUpgradabilityCheck() throws AmbariException {
    String clusterName = "foo1";
    StackId currentStackId = new StackId("HDP-0.2");

    Map<String, String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "Called from a test");

    createCluster(clusterName);
    Cluster c = clusters.getCluster(clusterName);
    c.setDesiredStackVersion(currentStackId);
    ClusterRequest r = new ClusterRequest(c.getClusterId(), clusterName, "HDP-0.3", null);
    try {
      controller.updateClusters(Collections.singleton(r), mapRequestProps);
    } catch (AmbariException e) {
      Assert.assertTrue(e.getMessage().contains("Illegal request to upgrade to"));
    }

    StackId unsupportedStackId = new StackId("HDP-2.2.0");
    c.setDesiredStackVersion(unsupportedStackId);
    c.setCurrentStackVersion(unsupportedStackId);
    c.refresh();
    r = new ClusterRequest(c.getClusterId(), clusterName, "HDP-0.2", null);
    try {
      controller.updateClusters(Collections.singleton(r), mapRequestProps);
    } catch (AmbariException e) {
      Assert.assertTrue(e.getMessage().contains("Upgrade is not allowed from"));
    }
  }

  // disabled as cluster upgrade feature is disabled
  @Ignore
  @Test
  public void testUpdateClusterVersionCombinations() throws AmbariException {
    String clusterName = "foo1";
    String pigServiceName = "PIG";
    String mrServiceName = "MAPREDUCE";
    String host1 = "h1";
    String host2 = "h2";
    String pigComponentName = "PIG";
    String mrJobTrackerComp = "JOBTRACKER";
    String mrTaskTrackerComp = "TASKTRACKER";
    String mrClientComp = "MAPREDUCE_CLIENT";
    String hdfsService = "HDFS";
    String hdfsNameNode = "NAMENODE";
    String hdfsDataNode = "DATANODE";
    String hdfsClient = "HDFS_CLIENT";
    StackId currentStackId = new StackId("HDP-0.1");
    StackId desiredStackId = new StackId("HDP-0.2");

    List<String> hosts = new ArrayList<String>();
    hosts.add(host1);
    hosts.add(host2);

    Map<String, String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "Called from a test");

    createCluster(clusterName);
    Cluster c = clusters.getCluster(clusterName);
    c.setDesiredStackVersion(currentStackId);
    createService(clusterName, pigServiceName, State.INIT);
    createServiceComponent(clusterName, pigServiceName, pigComponentName, null);

    addHostToCluster(host1, clusterName);
    addHostToCluster(host2, clusterName);

    createServiceComponentHost(clusterName, null, pigComponentName,
        host1, null);
    createServiceComponentHost(clusterName, null, pigComponentName,
        host2, null);

    resetServiceState(pigServiceName, currentStackId, c);

    ClusterRequest r = new ClusterRequest(c.getClusterId(), clusterName, "HDP-0.2", null);
    RequestStatusResponse trackAction = controller.updateClusters(Collections.singleton(r), mapRequestProps);
    List<Stage> stages = actionDB.getAllStages(trackAction.getRequestId());

    // Upgrade a cluster with one service
    ExpectedUpgradeTasks expectedTasks = new ExpectedUpgradeTasks(hosts);
    expectedTasks.expectTask(Role.PIG, host1);
    expectedTasks.expectTask(Role.PIG, host2);
    expectedTasks.expectTask(Role.AMBARI_SERVER_ACTION);
    validateGeneratedStages(stages, 2, expectedTasks);

    resetCluster(c, currentStackId);

    createService(clusterName, mrServiceName, State.INIT);
    createServiceComponent(clusterName, mrServiceName, mrJobTrackerComp, null);
    createServiceComponent(clusterName, mrServiceName, mrTaskTrackerComp, null);
    createServiceComponent(clusterName, mrServiceName, mrClientComp, null);

    createServiceComponentHost(clusterName, null, mrJobTrackerComp, host1, null);
    createServiceComponentHost(clusterName, null, mrTaskTrackerComp, host2, null);
    createServiceComponentHost(clusterName, null, mrClientComp, host2, null);

    resetServiceState(mrServiceName, currentStackId, c);

    // Upgrade a cluster with two service
    actionDB.abortOperation(trackAction.getRequestId());
    r = new ClusterRequest(c.getClusterId(), clusterName, "HDP-0.2", null);
    trackAction = controller.updateClusters(Collections.singleton(r), mapRequestProps);
    stages = actionDB.getAllStages(trackAction.getRequestId());

    expectedTasks.expectTask(Role.JOBTRACKER, host1);
    expectedTasks.expectTask(Role.TASKTRACKER, host2);
    expectedTasks.expectTask(Role.MAPREDUCE_CLIENT, host2);
    validateGeneratedStages(stages, 5, expectedTasks);

    // Upgrade again
    actionDB.abortOperation(trackAction.getRequestId());
    trackAction = controller.updateClusters(Collections.singleton(r), mapRequestProps);
    stages = actionDB.getAllStages(trackAction.getRequestId());
    validateGeneratedStages(stages, 5, expectedTasks);

    // some host components are upgraded
    c.getService(pigServiceName).getServiceComponent(pigComponentName).getServiceComponentHost(host1)
        .setState(State.INSTALLED);
    c.getService(pigServiceName).getServiceComponent(pigComponentName).getServiceComponentHost(host2)
        .setState(State.INSTALLED);
    c.getService(pigServiceName).getServiceComponent(pigComponentName).getServiceComponentHost(host1)
        .setStackVersion(desiredStackId);
    c.getService(pigServiceName).getServiceComponent(pigComponentName).getServiceComponentHost(host2)
        .setStackVersion(desiredStackId);

    actionDB.abortOperation(trackAction.getRequestId());
    trackAction = controller.updateClusters(Collections.singleton(r), mapRequestProps);
    stages = actionDB.getAllStages(trackAction.getRequestId());
    validateGeneratedStages(stages, 5, expectedTasks);

    c.getService(mrServiceName).getServiceComponent(mrJobTrackerComp).getServiceComponentHost(host1)
        .setState(State.UPGRADING);
    c.getService(mrServiceName).getServiceComponent(mrTaskTrackerComp).getServiceComponentHost(host2)
        .setState(State.UPGRADING);
    actionDB.abortOperation(trackAction.getRequestId());
    trackAction = controller.updateClusters(Collections.singleton(r), mapRequestProps);
    stages = actionDB.getAllStages(trackAction.getRequestId());
    validateGeneratedStages(stages, 5, expectedTasks);

    // Add HDFS and upgrade
    createService(clusterName, hdfsService, State.INIT);
    createServiceComponent(clusterName, hdfsService, hdfsNameNode, null);
    createServiceComponent(clusterName, hdfsService, hdfsDataNode, null);
    createServiceComponent(clusterName, hdfsService, hdfsClient, null);

    createServiceComponentHost(clusterName, null, hdfsNameNode, host1, null);
    createServiceComponentHost(clusterName, null, hdfsDataNode, host1, null);
    createServiceComponentHost(clusterName, null, hdfsDataNode, host2, null);
    createServiceComponentHost(clusterName, null, hdfsClient, host2, null);

    resetServiceState(hdfsService, currentStackId, c);
    resetServiceState(mrServiceName, currentStackId, c);
    resetServiceState(pigServiceName, currentStackId, c);

    actionDB.abortOperation(trackAction.getRequestId());
    trackAction = controller.updateClusters(Collections.singleton(r), mapRequestProps);
    stages = actionDB.getAllStages(trackAction.getRequestId());

    expectedTasks.resetAll();
    expectedTasks.expectTask(Role.PIG, host1);
    expectedTasks.expectTask(Role.PIG, host2);
    expectedTasks.expectTask(Role.JOBTRACKER, host1);
    expectedTasks.expectTask(Role.TASKTRACKER, host2);
    expectedTasks.expectTask(Role.MAPREDUCE_CLIENT, host2);
    expectedTasks.expectTask(Role.DATANODE, host1);
    expectedTasks.expectTask(Role.DATANODE, host2);
    expectedTasks.expectTask(Role.NAMENODE, host1);
    expectedTasks.expectTask(Role.HDFS_CLIENT, host2);
    expectedTasks.expectTask(Role.AMBARI_SERVER_ACTION);
    validateGeneratedStages(stages, 8, expectedTasks);
  }

  private void resetServiceState(String service, StackId currentStackId, Cluster c) throws AmbariException {
    c.getService(service).setDesiredState(State.INSTALLED);
    for (ServiceComponent sc : c.getService(service).getServiceComponents().values()) {
      sc.setDesiredState(State.INSTALLED);
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        sch.setDesiredState(State.INSTALLED);
        sch.setState(State.INSTALLED);
        sch.setStackVersion(currentStackId);
      }
    }
  }

  private void validateGeneratedStages(List<Stage> stages, int expectedStageCount, ExpectedUpgradeTasks expectedTasks) {
    Assert.assertEquals(expectedStageCount, stages.size());
    int prevRoleOrder = -1;
    for (Stage stage : stages) {
      int currRoleOrder = -1;
      for (HostRoleCommand command : stage.getOrderedHostRoleCommands()) {
        if(command.getRole() == Role.AMBARI_SERVER_ACTION) {
          Assert.assertTrue(command.toString(), expectedTasks.isTaskExpected(command.getRole()));
          currRoleOrder = expectedTasks.getRoleOrder(command.getRole());
          ExecutionCommand execCommand = command.getExecutionCommandWrapper().getExecutionCommand();
          Assert.assertTrue(
              execCommand.getRoleParams().containsKey(ServerAction.ACTION_NAME));
          Assert.assertEquals(RoleCommand.EXECUTE, execCommand.getRoleCommand());
        } else {
          Assert.assertTrue(command.toString(), expectedTasks.isTaskExpected(command.getRole(), command.getHostName()));
          currRoleOrder = expectedTasks.getRoleOrder(command.getRole());
          ExecutionCommand execCommand = command.getExecutionCommandWrapper().getExecutionCommand();
          Assert.assertTrue(execCommand.getCommandParams().containsKey("source_stack_version"));
          Assert.assertTrue(execCommand.getCommandParams().containsKey("target_stack_version"));
          Assert.assertEquals(RoleCommand.UPGRADE, execCommand.getRoleCommand());
        }
      }

      List<HostRoleCommand> commands = stage.getOrderedHostRoleCommands();
      Assert.assertTrue(commands.size() > 0);
      Role role = commands.get(0).getRole();
      for (HostRoleCommand command : commands) {
        Assert.assertTrue("All commands must be for the same role", role.equals(command.getRole()));
      }

      Assert.assertTrue("Roles must be in order", currRoleOrder > prevRoleOrder);
      prevRoleOrder = currRoleOrder;
    }
  }

  private void resetCluster(Cluster cluster, StackId currentStackId) throws AmbariException{
    cluster.setDesiredStackVersion(currentStackId);
    for (Service service : cluster.getServices().values()) {
      service.setDesiredStackVersion(currentStackId);
      for (ServiceComponent component : service.getServiceComponents().values()) {
        component.setDesiredStackVersion(currentStackId);
        for (ServiceComponentHost componentHost : component.getServiceComponentHosts().values()) {
          componentHost.setDesiredStackVersion(currentStackId);
          componentHost.setState(State.INSTALLED);
        }
      }
    }
  }

  class ExpectedUpgradeTasks {
    private static final int ROLE_COUNT = 25;
    private static final String DEFAULT_HOST = "default_host";
    private ArrayList<Map<String, Boolean>> expectedList;
    private Map<Role, Integer> roleToIndex;

    public ExpectedUpgradeTasks(List<String> hosts) {
      roleToIndex = new HashMap<Role, Integer>();
      expectedList = new ArrayList<Map<String, Boolean>>(ROLE_COUNT);

      fillRoleToIndex();
      fillExpectedHosts(hosts);
    }

    public void expectTask(Role role, String host) {
      expectedList.get(roleToIndex.get(role)).put(host, true);
    }

    public void expectTask(Role role) {
      Assert.assertEquals(Role.AMBARI_SERVER_ACTION, role);
      expectTask(role, DEFAULT_HOST);
    }

    public boolean isTaskExpected(Role role, String host) {
      return expectedList.get(roleToIndex.get(role)).get(host);
    }

    public boolean isTaskExpected(Role role) {
      Assert.assertEquals(Role.AMBARI_SERVER_ACTION, role);
      return isTaskExpected(role, DEFAULT_HOST);
    }

    public int getRoleOrder(Role role) {
      return roleToIndex.get(role);
    }

    public void resetAll() {
      for (Role role : roleToIndex.keySet()) {
        Map<String, Boolean> hostState = expectedList.get(roleToIndex.get(role));
        for (String host : hostState.keySet()) {
          hostState.put(host, false);
        }
      }
    }

    private void fillExpectedHosts(List<String> hosts) {
      for (int index = 0; index < ROLE_COUNT; index++) {
        Map<String, Boolean> hostState = new HashMap<String, Boolean>();
        for (String host : hosts) {
          hostState.put(host, false);
        }
        expectedList.add(hostState);
      }
    }

    private void fillRoleToIndex() {
      roleToIndex.put(Role.NAMENODE, 0);
      roleToIndex.put(Role.SECONDARY_NAMENODE, 1);
      roleToIndex.put(Role.DATANODE, 2);
      roleToIndex.put(Role.HDFS_CLIENT, 3);
      roleToIndex.put(Role.JOBTRACKER, 4);
      roleToIndex.put(Role.TASKTRACKER, 5);
      roleToIndex.put(Role.MAPREDUCE_CLIENT, 6);
      roleToIndex.put(Role.ZOOKEEPER_SERVER, 7);
      roleToIndex.put(Role.ZOOKEEPER_CLIENT, 8);
      roleToIndex.put(Role.HBASE_MASTER, 9);

      roleToIndex.put(Role.HBASE_REGIONSERVER, 10);
      roleToIndex.put(Role.HBASE_CLIENT, 11);
      roleToIndex.put(Role.HIVE_SERVER, 12);
      roleToIndex.put(Role.HIVE_METASTORE, 13);
      roleToIndex.put(Role.HIVE_CLIENT, 14);
      roleToIndex.put(Role.HCAT, 15);
      roleToIndex.put(Role.OOZIE_SERVER, 16);
      roleToIndex.put(Role.OOZIE_CLIENT, 17);
      roleToIndex.put(Role.WEBHCAT_SERVER, 18);
      roleToIndex.put(Role.PIG, 19);

      roleToIndex.put(Role.SQOOP, 20);
      roleToIndex.put(Role.GANGLIA_SERVER, 21);
      roleToIndex.put(Role.GANGLIA_MONITOR, 22);
      roleToIndex.put(Role.AMBARI_SERVER_ACTION, 23);
    }
  }

  @Test
  public void testServiceStopWhileStopping() throws Exception {
    String clusterName = "foo1";
    createCluster(clusterName);
    clusters.getCluster(clusterName)
      .setDesiredStackVersion(new StackId("HDP-0.1"));
    String serviceName = "HDFS";
    createService(clusterName, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";
    createServiceComponent(clusterName, serviceName, componentName1,
      State.INIT);
    createServiceComponent(clusterName, serviceName, componentName2,
      State.INIT);
    createServiceComponent(clusterName, serviceName, componentName3,
      State.INIT);

    String host1 = "h1";
    String host2 = "h2";

    addHostToCluster(host1, clusterName);
    addHostToCluster(host2, clusterName);

    Map<String, String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "Called from a test");

    // null service should work
    createServiceComponentHost(clusterName, null, componentName1,
      host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName2,
      host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName2,
      host2, null);
    createServiceComponentHost(clusterName, serviceName, componentName3,
      host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName3,
      host2, null);

    Assert.assertNotNull(clusters.getCluster(clusterName)
      .getService(serviceName)
      .getServiceComponent(componentName1)
      .getServiceComponentHost(host1));
    Assert.assertNotNull(clusters.getCluster(clusterName)
      .getService(serviceName)
      .getServiceComponent(componentName2)
      .getServiceComponentHost(host1));
    Assert.assertNotNull(clusters.getCluster(clusterName)
      .getService(serviceName)
      .getServiceComponent(componentName2)
      .getServiceComponentHost(host2));
    Assert.assertNotNull(clusters.getCluster(clusterName)
      .getService(serviceName)
      .getServiceComponent(componentName3)
      .getServiceComponentHost(host1));
    Assert.assertNotNull(clusters.getCluster(clusterName)
      .getService(serviceName)
      .getServiceComponent(componentName3)
      .getServiceComponentHost(host2));

    // Install
    ServiceRequest r = new ServiceRequest(clusterName, serviceName, State.INSTALLED.toString());
    Set<ServiceRequest> requests = new HashSet<ServiceRequest>();
    requests.add(r);

    ServiceResourceProviderTest.updateServices(controller, requests, mapRequestProps, true, false);
    Assert.assertEquals(State.INSTALLED,
      clusters.getCluster(clusterName).getService(serviceName)
        .getDesiredState());

    // manually change live state to installed as no running action manager
    for (ServiceComponent sc :
      clusters.getCluster(clusterName).getService(serviceName)
        .getServiceComponents().values()) {
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        sch.setState(State.INSTALLED);
      }
    }

    // Start
    r = new ServiceRequest(clusterName, serviceName, State.STARTED.toString());
    requests.clear();
    requests.add(r);
    ServiceResourceProviderTest.updateServices(controller, requests, mapRequestProps, true, false);

    // manually change live state to started as no running action manager
    for (ServiceComponent sc :
      clusters.getCluster(clusterName).getService(serviceName)
        .getServiceComponents().values()) {
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        if (!sch.getServiceComponentName().equals("HDFS_CLIENT")) {
          sch.setState(State.STARTED);
        }
      }
    }

    Assert.assertEquals(State.STARTED,
      clusters.getCluster(clusterName).getService(serviceName)
        .getDesiredState());

    // Set Current state to stopping
    clusters.getCluster(clusterName).getService(serviceName).setDesiredState
      (State.STOPPING);
    for (ServiceComponent sc :
      clusters.getCluster(clusterName).getService(serviceName)
        .getServiceComponents().values()) {

      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        if (!sch.getServiceComponentName().equals("HDFS_CLIENT")) {
          Assert.assertEquals(State.STARTED, sch.getDesiredState());
          sch.setState(State.STOPPING);
        } else if (sch.getServiceComponentName().equals("DATANODE")) {
          ServiceComponentHostRequest r1 = new ServiceComponentHostRequest
            (clusterName, serviceName, sch.getServiceComponentName(),
              sch.getHostName(), State.INSTALLED.name());
          Set<ServiceComponentHostRequest> reqs1 = new
            HashSet<ServiceComponentHostRequest>();
          reqs1.add(r1);
          updateHostComponents(reqs1, Collections.<String, String>emptyMap(), true);
          Assert.assertEquals(State.INSTALLED, sch.getDesiredState());
        }
      }
    }

    // Stop all services
    r = new ServiceRequest(clusterName, serviceName, State.INSTALLED.toString());
    requests.clear();
    requests.add(r);
    ServiceResourceProviderTest.updateServices(controller, requests, mapRequestProps, true, false);

    for (ServiceComponent sc :
      clusters.getCluster(clusterName).getService(serviceName)
        .getServiceComponents().values()) {

      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        if (!sch.getServiceComponentName().equals("HDFS_CLIENT")) {
          Assert.assertEquals(State.INSTALLED, sch.getDesiredState());
        }
      }
    }
  }

  @Test
  public void testGetTasksByRequestId() throws AmbariException {
    final long requestId1 = 1;
    final long requestId2 = 2;
    final long requestId3 = 3;
    final String clusterName = "c1";
    final String hostName1 = "h1";
    final String context = "Test invocation";

    StackId stackID = new StackId("HDP-0.1");
    clusters.addCluster(clusterName, stackID);
    Cluster c = clusters.getCluster(clusterName);

    helper.getOrCreateRepositoryVersion(stackID, stackID.getStackVersion());
    c.createClusterVersion(stackID, stackID.getStackVersion(), "admin",
        RepositoryVersionState.UPGRADING);
    clusters.addHost(hostName1);
    setOsFamily(clusters.getHost("h1"), "redhat", "5.9");
    clusters.getHost(hostName1).persist();
    clusters.addHost(StageUtils.getHostName());
    setOsFamily(clusters.getHost(StageUtils.getHostName()), "redhat", "5.9");
    clusters.getHost(StageUtils.getHostName()).persist();

    clusters.mapHostsToCluster(new HashSet<String>(){
      {add(hostName1);}}, clusterName);


    List<Stage> stages = new ArrayList<Stage>();
    stages.add(stageFactory.createNew(requestId1, "/a1", clusterName, 1L, context,
        CLUSTER_HOST_INFO, "", ""));
    stages.get(0).setStageId(1);
    stages.get(0).addHostRoleExecutionCommand(hostName1, Role.HBASE_MASTER,
            RoleCommand.START,
            new ServiceComponentHostStartEvent(Role.HBASE_MASTER.toString(),
                    hostName1, System.currentTimeMillis()),
            clusterName, "HBASE", false, false);

    stages.add(stageFactory.createNew(requestId1, "/a2", clusterName, 1L, context,
      CLUSTER_HOST_INFO, "", ""));
    stages.get(1).setStageId(2);
    stages.get(1).addHostRoleExecutionCommand(hostName1, Role.HBASE_CLIENT,
            RoleCommand.START,
            new ServiceComponentHostStartEvent(Role.HBASE_CLIENT.toString(),
                    hostName1, System.currentTimeMillis()), clusterName, "HBASE", false, false);

    stages.add(stageFactory.createNew(requestId1, "/a3", clusterName, 1L, context,
      CLUSTER_HOST_INFO, "", ""));
    stages.get(2).setStageId(3);
    stages.get(2).addHostRoleExecutionCommand(hostName1, Role.HBASE_CLIENT,
            RoleCommand.START,
            new ServiceComponentHostStartEvent(Role.HBASE_CLIENT.toString(),
                    hostName1, System.currentTimeMillis()), clusterName, "HBASE", false, false);

    Request request = new Request(stages, clusters);
    actionDB.persistActions(request);

    stages.clear();
    stages.add(stageFactory.createNew(requestId2, "/a4", clusterName, 1L, context,
      CLUSTER_HOST_INFO, "", ""));
    stages.get(0).setStageId(4);
    stages.get(0).addHostRoleExecutionCommand(hostName1, Role.HBASE_CLIENT,
            RoleCommand.START,
            new ServiceComponentHostStartEvent(Role.HBASE_CLIENT.toString(),
                    hostName1, System.currentTimeMillis()), clusterName, "HBASE", false, false);

    stages.add(stageFactory.createNew(requestId2, "/a5", clusterName, 1L, context,
      CLUSTER_HOST_INFO, "", ""));
    stages.get(1).setStageId(5);
    stages.get(1).addHostRoleExecutionCommand(hostName1, Role.HBASE_CLIENT,
            RoleCommand.START,
            new ServiceComponentHostStartEvent(Role.HBASE_CLIENT.toString(),
                    hostName1, System.currentTimeMillis()), clusterName, "HBASE", false, false);

    request = new Request(stages, clusters);
    actionDB.persistActions(request);

    // Add a stage to execute a task as server-side action on the Ambari server
    ServiceComponentHostServerActionEvent serviceComponentHostServerActionEvent =
        new ServiceComponentHostServerActionEvent(Role.AMBARI_SERVER_ACTION.toString(), null, System.currentTimeMillis());
    stages.clear();
    stages.add(stageFactory.createNew(requestId3, "/a6", clusterName, 1L, context,
      CLUSTER_HOST_INFO, "", ""));
    stages.get(0).setStageId(6);
    stages.get(0).addServerActionCommand("some.action.class.name", null, Role.AMBARI_SERVER_ACTION,
        RoleCommand.EXECUTE, clusterName, serviceComponentHostServerActionEvent, null, null, null, null, false, false);
    assertEquals("_internal_ambari", stages.get(0).getOrderedHostRoleCommands().get(0).getHostName());
    request = new Request(stages, clusters);
    actionDB.persistActions(request);

    org.apache.ambari.server.controller.spi.Request spiRequest = PropertyHelper.getReadRequest(
        TaskResourceProvider.TASK_CLUSTER_NAME_PROPERTY_ID,
        TaskResourceProvider.TASK_REQUEST_ID_PROPERTY_ID,
        TaskResourceProvider.TASK_STAGE_ID_PROPERTY_ID);

    // request ID 1 has 3 tasks
    Predicate predicate = new PredicateBuilder().property(
        TaskResourceProvider.TASK_REQUEST_ID_PROPERTY_ID).equals(requestId1).toPredicate();

    List<HostRoleCommandEntity> entities = hostRoleCommandDAO.findAll(spiRequest, predicate);
    Assert.assertEquals(3, entities.size());

    // request just a task by ID
    predicate = new PredicateBuilder().property(
        TaskResourceProvider.TASK_REQUEST_ID_PROPERTY_ID).equals(requestId1).and().property(
            TaskResourceProvider.TASK_ID_PROPERTY_ID).equals(2L).toPredicate();

    entities = hostRoleCommandDAO.findAll(spiRequest, predicate);
    Assert.assertEquals(1, entities.size());

    // request ID 2 has 2 tasks
    predicate = new PredicateBuilder().property(
        TaskResourceProvider.TASK_REQUEST_ID_PROPERTY_ID).equals(requestId2).toPredicate();

    entities = hostRoleCommandDAO.findAll(spiRequest, predicate);
    Assert.assertEquals(2, entities.size());

    // a single task from request 1 and all tasks from request 2 will total 3
    predicate = new PredicateBuilder().property(
        TaskResourceProvider.TASK_REQUEST_ID_PROPERTY_ID).equals(requestId1).and().property(
            TaskResourceProvider.TASK_ID_PROPERTY_ID).equals(2L).or().property(
                TaskResourceProvider.TASK_REQUEST_ID_PROPERTY_ID).equals(requestId2).toPredicate();

    entities = hostRoleCommandDAO.findAll(spiRequest, predicate);
    Assert.assertEquals(3, entities.size());
  }

  @Test
  public void testUpdateHostComponentsBadState() throws Exception {
    String clusterName = "foo1";
    createCluster(clusterName);
    clusters.getCluster(clusterName)
      .setDesiredStackVersion(new StackId("HDP-0.1"));
    String serviceName = "HDFS";
    createService(clusterName, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";
    createServiceComponent(clusterName, serviceName, componentName1,
      State.INIT);
    createServiceComponent(clusterName, serviceName, componentName2,
      State.INIT);
    createServiceComponent(clusterName, serviceName, componentName3,
      State.INIT);

    String host1 = "h1";
    String host2 = "h2";

    addHostToCluster(host1, clusterName);
    addHostToCluster(host2, clusterName);

    Map<String, String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "Called from a test");

    // null service should work
    createServiceComponentHost(clusterName, null, componentName1,
      host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName2,
      host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName2,
      host2, null);
    createServiceComponentHost(clusterName, serviceName, componentName3,
      host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName3,
      host2, null);

    Assert.assertNotNull(clusters.getCluster(clusterName).getService(serviceName)
      .getServiceComponent(componentName1)
      .getServiceComponentHost(host1));
    Assert.assertNotNull(clusters.getCluster(clusterName).getService(serviceName)
      .getServiceComponent(componentName2)
      .getServiceComponentHost(host1));
    Assert.assertNotNull(clusters.getCluster(clusterName).getService(serviceName)
      .getServiceComponent(componentName2)
      .getServiceComponentHost(host2));
    Assert.assertNotNull(clusters.getCluster(clusterName).getService(serviceName)
      .getServiceComponent(componentName3)
      .getServiceComponentHost(host1));
    Assert.assertNotNull(clusters.getCluster(clusterName).getService(serviceName)
      .getServiceComponent(componentName3)
      .getServiceComponentHost(host2));



    // Install
    ServiceRequest r = new ServiceRequest(clusterName, serviceName, State.INSTALLED.toString());
    Set<ServiceRequest> requests = new HashSet<ServiceRequest>();
    requests.add(r);

    ServiceResourceProviderTest.updateServices(controller, requests, mapRequestProps, true, false);
    Assert.assertEquals(State.INSTALLED,
      clusters.getCluster(clusterName).getService(serviceName)
        .getDesiredState());

    // set host components on host1 to INSTALLED
    for (ServiceComponentHost sch : clusters.getCluster(clusterName).getServiceComponentHosts(host1)) {
      sch.setState(State.INSTALLED);
    }

    // set the host components on host2 to UNKNOWN state to simulate a lost host
    for (ServiceComponentHost sch : clusters.getCluster(clusterName).getServiceComponentHosts(host2)) {
      sch.setState(State.UNKNOWN);
    }

    // issue an installed state request without failure
    ServiceComponentHostRequest schr = new ServiceComponentHostRequest(clusterName, "HDFS", "DATANODE", host2, "INSTALLED");
    Map<String, String> requestProps = new HashMap<String, String>();
    requestProps.put("datanode", "dn_value");
    requestProps.put("namenode", "nn_value");
    RequestStatusResponse rsr = updateHostComponents(Collections.singleton(schr), requestProps, false);

    List<Stage> stages = actionDB.getAllStages(rsr.getRequestId());
    Assert.assertEquals(1, stages.size());
    Stage stage = stages.iterator().next();
    List<ExecutionCommandWrapper> execWrappers = stage.getExecutionCommands(host2);
    Assert.assertEquals(1, execWrappers.size());
    ExecutionCommandWrapper execWrapper = execWrappers.iterator().next();
    Assert.assertTrue(execWrapper.getExecutionCommand().getCommandParams().containsKey("datanode"));
    Assert.assertFalse(execWrapper.getExecutionCommand().getCommandParams().containsKey("namendode"));



    // set the host components on host2 to UNKNOWN state to simulate a lost host
    for (ServiceComponentHost sch : clusters.getCluster(clusterName).getServiceComponentHosts(host2)) {
      Assert.assertEquals(State.UNKNOWN, sch.getState());
    }
  }

  @Test
  public void testServiceUpdateRecursiveBadHostComponent() throws Exception {
    String clusterName = "foo1";
    createCluster(clusterName);
    clusters.getCluster(clusterName)
        .setDesiredStackVersion(new StackId("HDP-0.2"));

    String serviceName1 = "HDFS";
    createService(clusterName, serviceName1, null);

    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";

    createServiceComponent(clusterName, serviceName1, componentName1, State.INIT);
    createServiceComponent(clusterName, serviceName1, componentName2, State.INIT);
    createServiceComponent(clusterName, serviceName1, componentName3, State.INIT);
    String host1 = "h1";
    addHostToCluster(host1, clusterName);

    Set<ServiceComponentHostRequest> set1 =  new HashSet<ServiceComponentHostRequest>();
    ServiceComponentHostRequest r1 = new ServiceComponentHostRequest(clusterName, serviceName1,
        componentName1, host1, State.INIT.toString());
    ServiceComponentHostRequest r2 = new ServiceComponentHostRequest(clusterName, serviceName1,
        componentName2, host1, State.INIT.toString());
    ServiceComponentHostRequest r3 = new ServiceComponentHostRequest(clusterName, serviceName1,
        componentName3, host1, State.INIT.toString());

    set1.add(r1);
    set1.add(r2);
    set1.add(r3);
    controller.createHostComponents(set1);

    Cluster c1 = clusters.getCluster(clusterName);
    Service s1 = c1.getService(serviceName1);

    ServiceComponent sc1 = s1.getServiceComponent(componentName1);
    ServiceComponent sc2 = s1.getServiceComponent(componentName2);
    ServiceComponent sc3 = s1.getServiceComponent(componentName3);
    ServiceComponentHost sch1 = sc1.getServiceComponentHost(host1);
    ServiceComponentHost sch2 = sc2.getServiceComponentHost(host1);
    ServiceComponentHost sch3 = sc3.getServiceComponentHost(host1);

    s1.setDesiredState(State.INSTALLED);
    sc1.setDesiredState(State.STARTED);
    sc2.setDesiredState(State.INIT);
    sc3.setDesiredState(State.INSTALLED);
    sch1.setDesiredState(State.INSTALLED);
    sch2.setDesiredState(State.INSTALLED);
    sch3.setDesiredState(State.INSTALLED);
    sch1.setState(State.INSTALLED);
    sch2.setState(State.UNKNOWN);
    sch3.setState(State.INSTALLED);

    // an UNKOWN failure will throw an exception
    ServiceRequest req = new ServiceRequest(clusterName, serviceName1,
        State.INSTALLED.toString());
    ServiceResourceProviderTest.updateServices(controller, Collections.singleton(req), Collections.<String, String>emptyMap(), true, false);
  }

  @Test
  public void testUpdateStacks() throws Exception {

    StackInfo stackInfo = ambariMetaInfo.getStack(STACK_NAME, STACK_VERSION);

    for (RepositoryInfo repositoryInfo: stackInfo.getRepositories()) {
      assertFalse(INCORRECT_BASE_URL.equals(repositoryInfo.getBaseUrl()));
      repositoryInfo.setBaseUrl(INCORRECT_BASE_URL);
      assertTrue(INCORRECT_BASE_URL.equals(repositoryInfo.getBaseUrl()));
    }

    controller.updateStacks();

    stackInfo = ambariMetaInfo.getStack(STACK_NAME, STACK_VERSION);

    for (RepositoryInfo repositoryInfo: stackInfo.getRepositories()) {
      assertFalse(INCORRECT_BASE_URL.equals(repositoryInfo.getBaseUrl()));
    }
  }

  @Test
  public void testUpdateRepoUrl() throws Exception {
    String INCORRECT_URL_2 = "http://bar.com/foo";

    RepositoryInfo repo = ambariMetaInfo.getRepository(STACK_NAME, STACK_VERSION, OS_TYPE, REPO_ID);
    assertNotNull(repo);
    assertNotNull(repo.getBaseUrl());

    String original = repo.getBaseUrl();

    repo = ambariMetaInfo.getRepository(STACK_NAME, STACK_VERSION, OS_TYPE, REPO_ID);
    assertEquals(original, repo.getBaseUrl());

    ambariMetaInfo.updateRepoBaseURL(STACK_NAME, STACK_VERSION, OS_TYPE, REPO_ID, INCORRECT_BASE_URL);

    repo = ambariMetaInfo.getRepository(STACK_NAME, STACK_VERSION, OS_TYPE, REPO_ID);
    assertEquals(INCORRECT_BASE_URL, repo.getBaseUrl());
    assertEquals(original, repo.getDefaultBaseUrl());

    ambariMetaInfo.updateRepoBaseURL(STACK_NAME, STACK_VERSION, OS_TYPE, REPO_ID, INCORRECT_URL_2);
    repo = ambariMetaInfo.getRepository(STACK_NAME, STACK_VERSION, OS_TYPE, REPO_ID);
    assertFalse(INCORRECT_BASE_URL.equals(repo.getBaseUrl()));
    assertEquals(INCORRECT_URL_2, repo.getBaseUrl());
    assertEquals(original, repo.getDefaultBaseUrl());

    // verify change with new meta info
    Configuration configuration = injector.getInstance(Configuration.class);
    Properties properties = configuration.getProperties();
    properties.setProperty(Configuration.METADATA_DIR_PATH, "src/test/resources/stacks");
    properties.setProperty(Configuration.SERVER_VERSION_FILE, "src/test/resources/version");
    Configuration newConfiguration = new Configuration(properties);

    AmbariMetaInfo ami = new AmbariMetaInfo(newConfiguration);

    injector.injectMembers(ami);
    ami.init();

    repo = ami.getRepository(STACK_NAME, STACK_VERSION, OS_TYPE, REPO_ID);
    assertEquals(INCORRECT_URL_2, repo.getBaseUrl());
    assertNotNull(repo.getDefaultBaseUrl());
    assertEquals(original, repo.getDefaultBaseUrl());

    ami.updateRepoBaseURL(STACK_NAME, STACK_VERSION, OS_TYPE, REPO_ID, original);
    repo = ami.getRepository(STACK_NAME, STACK_VERSION, OS_TYPE, REPO_ID);
    assertEquals(original, repo.getBaseUrl());
    assertEquals(original, repo.getDefaultBaseUrl());
  }

  @Test
  public void testUpdateRepoUrlController() throws Exception {
    String badUrl = "http://hortonworks.com";
    RepositoryInfo repo = ambariMetaInfo.getRepository(STACK_NAME, STACK_VERSION, OS_TYPE, REPO_ID);
    RepositoryRequest request = new RepositoryRequest(STACK_NAME, STACK_VERSION, OS_TYPE, REPO_ID);
    request.setBaseUrl(badUrl);

    Set<RepositoryRequest> requests = new HashSet<RepositoryRequest>();
    requests.add(request);

    // test bad url
    try {
      controller.updateRepositories(requests);
      Assert.fail("Expected a bad URL to throw an exception");
    } catch (Exception e) {
      assertNotNull(e);
      Assert.assertTrue(e.getMessage().contains(badUrl));
    }
    // test bad url, but allow to set anyway
    request.setVerifyBaseUrl(false);
    controller.updateRepositories(requests);
    Assert.assertEquals(request.getBaseUrl(), repo.getBaseUrl());

    requests.clear();
    request = new RepositoryRequest(STACK_NAME, STACK_VERSION, OS_TYPE, REPO_ID);
    request.setBaseUrl("https://hortonworks.com");
    requests.add(request);
    // test bad url
    try {
      controller.updateRepositories(requests);
      fail("Expected IllegalStateException");
    } catch (IllegalStateException e) {
      //expected
    }

    requests.clear();
    request = new RepositoryRequest(STACK_NAME, STACK_VERSION, OS_TYPE, REPO_ID);
    request.setBaseUrl("pro://hortonworks.com");
    requests.add(request);
    // test bad url
    try {
      controller.updateRepositories(requests);
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("Could not access base url"));
    }

    requests.clear();
    request = new RepositoryRequest(STACK_NAME, STACK_VERSION, OS_TYPE, REPO_ID);
    request.setBaseUrl("http://rrr1.cccc");
    requests.add(request);
    // test bad url
    try {
      controller.updateRepositories(requests);
    } catch (Exception e) {
      String exceptionMsg = e.getMessage();
      assertTrue(exceptionMsg.contains("Could not access base url"));
    }

    // reset repo
    requests.clear();
    request = new RepositoryRequest(STACK_NAME, STACK_VERSION, OS_TYPE, REPO_ID);
    request.setBaseUrl(repo.getDefaultBaseUrl());
    requests.add(request);
    try {
      controller.updateRepositories(requests);
      Assert.assertEquals(repo.getBaseUrl(), repo.getDefaultBaseUrl());
    } catch (Exception e) {
      String exceptionMsg = e.getMessage();
      assertTrue(exceptionMsg.contains("Could not access base url"));
      LOG.error("Can not complete test. " + exceptionMsg);
    }

    String baseUrl = repo.getDefaultBaseUrl();
    if (!baseUrl.endsWith("/")) {
      baseUrl += "/";
    }

    // variation #1: url with trailing slash, suffix preceding slash
    backingProperties.setProperty(Configuration.REPO_SUFFIX_KEY_UBUNTU, "/repodata/repomd.xml");
    Assert.assertTrue(baseUrl.endsWith("/") && configuration.getRepoValidationSuffixes("ubuntu12")[0].startsWith("/"));
    request.setBaseUrl(baseUrl);
    try {
      controller.updateRepositories(requests);
      Assert.assertEquals(baseUrl, repo.getBaseUrl());
    } catch (Exception e) {
      String exceptionMsg = e.getMessage();
      assertTrue(exceptionMsg.contains("Could not access base url"));
      LOG.error("Can not complete test. " + exceptionMsg);
    }

    // variation #2: url with trailing slash, suffix no preceding slash
    backingProperties.setProperty(Configuration.REPO_SUFFIX_KEY_DEFAULT, "repodata/repomd.xml");
    Assert.assertTrue(baseUrl.endsWith("/") && !configuration.getRepoValidationSuffixes("redhat6")[0].startsWith("/"));
    request.setBaseUrl(baseUrl);
    try {
      controller.updateRepositories(requests);
      Assert.assertEquals(baseUrl, repo.getBaseUrl());
    } catch (Exception e) {
      String exceptionMsg = e.getMessage();
      assertTrue(exceptionMsg.contains("Could not access base url"));
      LOG.error("Can not complete test. " + exceptionMsg);
    }

    baseUrl = baseUrl.substring(0, baseUrl.length()-1);
    // variation #3: url with no trailing slash, suffix no prededing slash
    Assert.assertTrue(!baseUrl.endsWith("/") && !configuration.getRepoValidationSuffixes("redhat6")[0].startsWith("/"));
    request.setBaseUrl(baseUrl);
    try {
      controller.updateRepositories(requests);
      Assert.assertEquals(baseUrl, repo.getBaseUrl());
    } catch (Exception e) {
      String exceptionMsg = e.getMessage();
      assertTrue(exceptionMsg.contains("Could not access base url"));
      LOG.error("Can not complete test. " + exceptionMsg);
    }

    // variation #4: url with no trailing slash, suffix preceding slash
    backingProperties.setProperty(Configuration.REPO_SUFFIX_KEY_DEFAULT, "/repodata/repomd.xml");
    Assert.assertTrue(!baseUrl.endsWith("/") && configuration.getRepoValidationSuffixes("suse11")[0].startsWith("/"));
    request.setBaseUrl(baseUrl);
    try {
      controller.updateRepositories(requests);
      Assert.assertEquals(baseUrl, repo.getBaseUrl());
    } catch (Exception e) {
      String exceptionMsg = e.getMessage();
      assertTrue(exceptionMsg.contains("Could not access base url"));
      LOG.error("Can not complete test. " + exceptionMsg);
    }

    // variation #5: multiple suffix tests
    backingProperties.setProperty(Configuration.REPO_SUFFIX_KEY_UBUNTU, "/foo/bar.xml,/repodata/repomd.xml");
    Assert.assertTrue(configuration.getRepoValidationSuffixes("ubuntu12").length > 1);
    request.setBaseUrl(baseUrl);
    try {
      controller.updateRepositories(requests);
      Assert.assertEquals(baseUrl, repo.getBaseUrl());
    } catch (Exception e) {
      String exceptionMsg = e.getMessage();
      assertTrue(exceptionMsg.contains("Could not access base url"));
      LOG.error("Can not complete test. " + exceptionMsg);
    }

  }

  @Test
  public void testDeleteHostComponentInVariousStates() throws Exception {
    String clusterName = "foo1";
    createCluster(clusterName);
    clusters.getCluster(clusterName)
        .setDesiredStackVersion(new StackId("HDP-1.3.1"));
    String serviceName = "HDFS";
    String mapred = "MAPREDUCE";
    createService(clusterName, serviceName, null);
    createService(clusterName, mapred, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";
    String componentName4 = "JOBTRACKER";
    String componentName5 = "TASKTRACKER";
    String componentName6 = "MAPREDUCE_CLIENT";

    createServiceComponent(clusterName, serviceName, componentName1, State.INIT);
    createServiceComponent(clusterName, serviceName, componentName2, State.INIT);
    createServiceComponent(clusterName, serviceName, componentName3, State.INIT);
    createServiceComponent(clusterName, mapred, componentName4, State.INIT);
    createServiceComponent(clusterName, mapred, componentName5, State.INIT);
    createServiceComponent(clusterName, mapred, componentName6, State.INIT);

    String host1 = "h1";

    addHostToCluster(host1, clusterName);

    createServiceComponentHost(clusterName, serviceName, componentName1, host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName2, host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName3, host1, null);
    createServiceComponentHost(clusterName, mapred, componentName4, host1, null);
    createServiceComponentHost(clusterName, mapred, componentName5, host1, null);
    createServiceComponentHost(clusterName, mapred, componentName6, host1, null);

    // Install
    installService(clusterName, serviceName, false, false);
    installService(clusterName, mapred, false, false);

    Cluster cluster = clusters.getCluster(clusterName);
    Service s1 = cluster.getService(serviceName);
    Service s2 = cluster.getService(mapred);
    ServiceComponent sc1 = s1.getServiceComponent(componentName1);
    sc1.getServiceComponentHosts().values().iterator().next().setState(State.STARTED);

    Set<ServiceComponentHostRequest> schRequests = new HashSet<ServiceComponentHostRequest>();
    // delete HC
    schRequests.clear();
    schRequests.add(new ServiceComponentHostRequest(clusterName, serviceName, componentName1, host1, null));
    try {
      controller.deleteHostComponents(schRequests);
      Assert.fail("Expect failure while deleting.");
    } catch (Exception ex) {
      Assert.assertTrue(ex.getMessage().contains(
          "Host Component cannot be removed"));
    }

    sc1.getServiceComponentHosts().values().iterator().next().setDesiredState(State.STARTED);
    sc1.getServiceComponentHosts().values().iterator().next().setState(State.UNKNOWN);
    ServiceComponent sc2 = s1.getServiceComponent(componentName2);
    sc2.getServiceComponentHosts().values().iterator().next().setState(State.INIT);
    ServiceComponent sc3 = s1.getServiceComponent(componentName3);
    sc3.getServiceComponentHosts().values().iterator().next().setState(State.INSTALL_FAILED);
    ServiceComponent sc4 = s2.getServiceComponent(componentName4);
    sc4.getServiceComponentHosts().values().iterator().next().setDesiredState(State.INSTALLED);
    sc4.getServiceComponentHosts().values().iterator().next().setState(State.DISABLED);
    ServiceComponent sc5 = s2.getServiceComponent(componentName5);
    sc5.getServiceComponentHosts().values().iterator().next().setState(State.INSTALLED);
    ServiceComponent sc6 = s2.getServiceComponent(componentName6);
    sc6.getServiceComponentHosts().values().iterator().next().setState(State.INIT);

    schRequests.clear();
    schRequests.add(new ServiceComponentHostRequest(clusterName, serviceName, componentName1, host1, null));
    schRequests.add(new ServiceComponentHostRequest(clusterName, serviceName, componentName2, host1, null));
    schRequests.add(new ServiceComponentHostRequest(clusterName, serviceName, componentName3, host1, null));
    schRequests.add(new ServiceComponentHostRequest(clusterName, mapred, componentName4, host1, null));
    schRequests.add(new ServiceComponentHostRequest(clusterName, mapred, componentName5, host1, null));
    schRequests.add(new ServiceComponentHostRequest(clusterName, mapred, componentName6, host1, null));
    controller.deleteHostComponents(schRequests);
  }

  @Test
  public void testDeleteHost() throws Exception {
    String clusterName = "foo1";

    createCluster(clusterName);

    Cluster cluster = clusters.getCluster(clusterName);
    cluster.setDesiredStackVersion(new StackId("HDP-0.1"));

    String serviceName = "HDFS";
    createService(clusterName, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";

    createServiceComponent(clusterName, serviceName, componentName1, State.INIT);
    createServiceComponent(clusterName, serviceName, componentName2, State.INIT);
    createServiceComponent(clusterName, serviceName, componentName3, State.INIT);

    String host1 = "h1";  // Host will belong to the cluster and contain components
    String host2 = "h2";  // Host will belong to the cluster and not contain any components

    addHostToCluster(host1, clusterName);
    addHostToCluster(host2, clusterName);
    String host3 = "h3";  // Host is not registered

    // Add components to host1
    createServiceComponentHost(clusterName, serviceName, componentName1, host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName2, host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName3, host1, null);

    // Install
    installService(clusterName, serviceName, false, false);

    // Treat host components on host1 as up and healthy
    Map<String, ServiceComponentHost> hostComponents = cluster.getService(serviceName).getServiceComponent(componentName1).getServiceComponentHosts();
    for (Map.Entry<String, ServiceComponentHost> entry : hostComponents.entrySet()) {
      ServiceComponentHost cHost = entry.getValue();
      cHost.handleEvent(new ServiceComponentHostInstallEvent(cHost.getServiceComponentName(), cHost.getHostName(), System.currentTimeMillis(), cluster.getDesiredStackVersion().getStackId()));
      cHost.handleEvent(new ServiceComponentHostOpSucceededEvent(cHost.getServiceComponentName(), cHost.getHostName(), System.currentTimeMillis()));
    }
    hostComponents = cluster.getService(serviceName).getServiceComponent(componentName2).getServiceComponentHosts();
    for (Map.Entry<String, ServiceComponentHost> entry : hostComponents.entrySet()) {
      ServiceComponentHost cHost = entry.getValue();
      cHost.handleEvent(new ServiceComponentHostInstallEvent(cHost.getServiceComponentName(), cHost.getHostName(), System.currentTimeMillis(), cluster.getDesiredStackVersion().getStackId()));
      cHost.handleEvent(new ServiceComponentHostOpSucceededEvent(cHost.getServiceComponentName(), cHost.getHostName(), System.currentTimeMillis()));
    }

    // Case 1: Attempt delete when components still exist
    Set<HostRequest> requests = new HashSet<HostRequest>();
    requests.clear();
    requests.add(new HostRequest(host1, clusterName, null));
    try {
      HostResourceProviderTest.deleteHosts(controller, requests);
      fail("Expect failure deleting hosts when components exist and have not been deleted.");
    } catch (Exception e) {
    }

    // Case 2: Delete host that is still part of cluster, but do not specify the cluster_name in the request
    Set<ServiceComponentHostRequest> schRequests = new HashSet<ServiceComponentHostRequest>();
    // Disable HC for non-clients
    schRequests.add(new ServiceComponentHostRequest(clusterName, serviceName, componentName1, host1, "DISABLED"));
    schRequests.add(new ServiceComponentHostRequest(clusterName, serviceName, componentName2, host1, "DISABLED"));
    updateHostComponents(schRequests, new HashMap<String,String>(), false);

    // Delete HC
    schRequests.clear();
    schRequests.add(new ServiceComponentHostRequest(clusterName, serviceName, componentName1, host1, null));
    schRequests.add(new ServiceComponentHostRequest(clusterName, serviceName, componentName2, host1, null));
    schRequests.add(new ServiceComponentHostRequest(clusterName, serviceName, componentName3, host1, null));
    controller.deleteHostComponents(schRequests);

    Assert.assertEquals(0, cluster.getServiceComponentHosts(host1).size());

    Assert.assertNull(topologyHostInfoDAO.findByHostname(host1));

    // Deletion without specifying cluster should be successful
    requests.clear();
    requests.add(new HostRequest(host1, null, null));
    try {
      HostResourceProviderTest.deleteHosts(controller, requests);
    } catch (Exception e) {
      fail("Did not expect an error deleting the host from the cluster. Error: " + e.getMessage());
    }
    // Verify host is no longer part of the cluster
    Assert.assertFalse(clusters.getHostsForCluster(clusterName).containsKey(host1));
    Assert.assertFalse(clusters.getClustersForHost(host1).contains(cluster));
    Assert.assertNull(topologyHostInfoDAO.findByHostname(host1));

    // Case 3: Delete host that is still part of the cluster, and specify the cluster_name in the request
    requests.clear();
    requests.add(new HostRequest(host2, clusterName, null));
    try {
      HostResourceProviderTest.deleteHosts(controller, requests);
    } catch (Exception e) {
      fail("Did not expect an error deleting the host from the cluster. Error: " + e.getMessage());
    }
    // Verify host is no longer part of the cluster
    Assert.assertFalse(clusters.getHostsForCluster(clusterName).containsKey(host2));
    Assert.assertFalse(clusters.getClustersForHost(host2).contains(cluster));
    Assert.assertNull(topologyHostInfoDAO.findByHostname(host2));

    // Case 4: Attempt to delete a host that has already been deleted
    requests.clear();
    requests.add(new HostRequest(host1, null, null));
    try {
      HostResourceProviderTest.deleteHosts(controller, requests);
      Assert.fail("Expected a HostNotFoundException trying to remove a host that was already deleted.");
    } catch (HostNotFoundException e) {
      // expected
    }

    // Verify host does not exist
    try {
      clusters.getHost(host1);
      Assert.fail("Expected a HostNotFoundException.");
    } catch (HostNotFoundException e) {
      // expected
    }

    // Case 5: Attempt to delete a host that was never added to the cluster
    requests.clear();
    requests.add(new HostRequest(host3, null, null));
    try {
      HostResourceProviderTest.deleteHosts(controller, requests);
      Assert.fail("Expected a HostNotFoundException trying to remove a host that was never added.");
    } catch (HostNotFoundException e) {
      // expected
    }
  }

  @Test
  public void testGetRootServices() throws Exception {

    RootServiceRequest request = new RootServiceRequest(null);
    Set<RootServiceResponse> responses = controller.getRootServices(Collections.singleton(request));
    Assert.assertEquals(RootServiceResponseFactory.Services.values().length, responses.size());

    RootServiceRequest requestWithParams = new RootServiceRequest(RootServiceResponseFactory.Services.AMBARI.toString());
    Set<RootServiceResponse> responsesWithParams = controller.getRootServices(Collections.singleton(requestWithParams));
    Assert.assertEquals(1, responsesWithParams.size());
    for (RootServiceResponse responseWithParams: responsesWithParams) {
      Assert.assertEquals(responseWithParams.getServiceName(), RootServiceResponseFactory.Services.AMBARI.toString());
    }

    RootServiceRequest invalidRequest = new RootServiceRequest(NON_EXT_VALUE);
    try {
      controller.getRootServices(Collections.singleton(invalidRequest));
    } catch (ObjectNotFoundException e) {
      // do nothing
    }
  }

  @Test
  public void testGetRootServiceComponents() throws Exception {

    RootServiceComponentRequest request = new RootServiceComponentRequest(RootServiceResponseFactory.Services.AMBARI.toString(), null);
    Set<RootServiceComponentResponse> responses = controller.getRootServiceComponents(Collections.singleton(request));
    Assert.assertEquals(RootServiceResponseFactory.Services.AMBARI.getComponents().length, responses.size());

    RootServiceComponentRequest requestWithParams = new RootServiceComponentRequest(
        RootServiceResponseFactory.Services.AMBARI.toString(),
        RootServiceResponseFactory.Services.AMBARI.getComponents()[0].toString());

    Set<RootServiceComponentResponse> responsesWithParams = controller.getRootServiceComponents(Collections.singleton(requestWithParams));
    Assert.assertEquals(1, responsesWithParams.size());
    for (RootServiceComponentResponse responseWithParams: responsesWithParams) {
      Assert.assertEquals(responseWithParams.getComponentName(), RootServiceResponseFactory.Services.AMBARI.getComponents()[0].toString());
    }

    RootServiceComponentRequest invalidRequest = new RootServiceComponentRequest(NON_EXT_VALUE, NON_EXT_VALUE);
    try {
      controller.getRootServiceComponents(Collections.singleton(invalidRequest));
    } catch (ObjectNotFoundException e) {
      // do nothing
    }
  }

  @Test
  public void testDeleteComponentsOnHost() throws Exception {
    String clusterName = "foo1";

    createCluster(clusterName);

    Cluster cluster = clusters.getCluster(clusterName);
    cluster.setDesiredStackVersion(new StackId("HDP-0.1"));

    String serviceName = "HDFS";
    createService(clusterName, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";

    createServiceComponent(clusterName, serviceName, componentName1, State.INIT);
    createServiceComponent(clusterName, serviceName, componentName2, State.INIT);
    createServiceComponent(clusterName, serviceName, componentName3, State.INIT);

    String host1 = "h1";

    addHostToCluster(host1, clusterName);

    createServiceComponentHost(clusterName, null, componentName1, host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName2, host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName3, host1, null);

    // Install
    installService(clusterName, serviceName, false, false);

    // make them believe they are up
    Map<String, ServiceComponentHost> hostComponents = cluster.getService(serviceName).getServiceComponent(componentName1).getServiceComponentHosts();
    for (Map.Entry<String, ServiceComponentHost> entry : hostComponents.entrySet()) {
      ServiceComponentHost cHost = entry.getValue();
      cHost.handleEvent(new ServiceComponentHostInstallEvent(cHost.getServiceComponentName(), cHost.getHostName(), System.currentTimeMillis(), cluster.getDesiredStackVersion().getStackId()));
      cHost.handleEvent(new ServiceComponentHostOpSucceededEvent(cHost.getServiceComponentName(), cHost.getHostName(), System.currentTimeMillis()));
    }
    hostComponents = cluster.getService(serviceName).getServiceComponent(componentName2).getServiceComponentHosts();
    for (Map.Entry<String, ServiceComponentHost> entry : hostComponents.entrySet()) {
      ServiceComponentHost cHost = entry.getValue();
      cHost.handleEvent(new ServiceComponentHostInstallEvent(cHost.getServiceComponentName(), cHost.getHostName(), System.currentTimeMillis(), cluster.getDesiredStackVersion().getStackId()));
      cHost.handleEvent(new ServiceComponentHostOpSucceededEvent(cHost.getServiceComponentName(), cHost.getHostName(), System.currentTimeMillis()));
    }


    ServiceComponentHost sch = cluster.getService(serviceName).getServiceComponent(componentName2).getServiceComponentHost(host1);
    Assert.assertNotNull(sch);

    sch.handleEvent(new ServiceComponentHostStartEvent(sch.getServiceComponentName(), sch.getHostName(), System.currentTimeMillis()));
    sch.handleEvent(new ServiceComponentHostStartedEvent (sch.getServiceComponentName(), sch.getHostName(), System.currentTimeMillis()));

    Set<ServiceComponentHostRequest> schRequests = new HashSet<ServiceComponentHostRequest>();
    schRequests.add(new ServiceComponentHostRequest(clusterName, null, null, host1, null));
    try {
      controller.deleteHostComponents(schRequests);
      fail("Expected exception while deleting all host components.");
    } catch (AmbariException e) {
    }
    Assert.assertEquals(3, cluster.getServiceComponentHosts(host1).size());

    sch.handleEvent(new ServiceComponentHostStopEvent(sch.getServiceComponentName(), sch.getHostName(), System.currentTimeMillis()));
    sch.handleEvent(new ServiceComponentHostStoppedEvent (sch.getServiceComponentName(), sch.getHostName(), System.currentTimeMillis()));

    schRequests.clear();
    // disable HC, DN was already stopped
    schRequests.add(new ServiceComponentHostRequest(clusterName, serviceName, componentName1, host1, "DISABLED"));
    updateHostComponents(schRequests, new HashMap<String,String>(), false);

    // delete HC
    schRequests.clear();
    schRequests.add(new ServiceComponentHostRequest(clusterName, null, null, host1, null));
    controller.deleteHostComponents(schRequests);

    Assert.assertEquals(0, cluster.getServiceComponentHosts(host1).size());
  }

  @Test
  public void testExecutionCommandConfiguration() throws AmbariException {
    Map<String, Map<String, String>> config = new HashMap<String, Map<String, String>>();
    config.put("type1", new HashMap<String, String>());
    config.put("type3", new HashMap<String, String>());
    config.get("type3").put("name1", "neverchange");
    configHelper.applyCustomConfig(config, "type1", "name1", "value11", false);
    Assert.assertEquals("value11", config.get("type1").get("name1"));

    config.put("type1", new HashMap<String, String>());
    configHelper.applyCustomConfig(config, "type1", "name1", "value12", false);
    Assert.assertEquals("value12", config.get("type1").get("name1"));

    configHelper.applyCustomConfig(config, "type2", "name2", "value21", false);
    Assert.assertEquals("value21", config.get("type2").get("name2"));

    configHelper.applyCustomConfig(config, "type2", "name2", "", true);
    Assert.assertEquals("", config.get("type2").get("DELETED_name2"));
    Assert.assertEquals("neverchange", config.get("type3").get("name1"));

    Map<String, String> persistedClusterConfig = new HashMap<String, String>();
    persistedClusterConfig.put("name1", "value11");
    persistedClusterConfig.put("name3", "value31");
    persistedClusterConfig.put("name4", "value41");
    Map<String, String> override = new HashMap<String, String>();
    override.put("name1", "value12");
    override.put("name2", "value21");
    override.put("DELETED_name3", "value31");
    Map<String, String> mergedConfig = configHelper.getMergedConfig
      (persistedClusterConfig, override);
    Assert.assertEquals(3, mergedConfig.size());
    Assert.assertFalse(mergedConfig.containsKey("name3"));
    Assert.assertEquals("value12", mergedConfig.get("name1"));
    Assert.assertEquals("value21", mergedConfig.get("name2"));
    Assert.assertEquals("value41", mergedConfig.get("name4"));
  }

  @Test
  public void testApplyConfigurationWithTheSameTag() {
    Injector injector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        Properties properties = new Properties();
        properties.setProperty(Configuration.SERVER_PERSISTENCE_TYPE_KEY, "in-memory");
        properties.setProperty(Configuration.METADATA_DIR_PATH,"src/test/resources/stacks");
        properties.setProperty(Configuration.SERVER_VERSION_FILE,"src/test/resources/version");
        properties.setProperty(Configuration.OS_VERSION_KEY,"centos6");
        properties.setProperty(Configuration.SHARED_RESOURCES_DIR_KEY, "src/test/resources/");
        try {
          install(new ControllerModule(properties));
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    });
    injector.getInstance(GuiceJpaInitializer.class);

    try {
      String tag = "version1";
      String type = "core-site";
      AmbariException exception = null;
      try {
        AmbariManagementController amc = injector.getInstance(AmbariManagementController.class);
        Clusters clusters = injector.getInstance(Clusters.class);
        Gson gson = new Gson();

        clusters.addHost("host1");
        clusters.addHost("host2");
        clusters.addHost("host3");
        Host host = clusters.getHost("host1");
        setOsFamily(host, "redhat", "6.3");
        host.persist();
        host = clusters.getHost("host2");
        setOsFamily(host, "redhat", "6.3");
        host.persist();
        host = clusters.getHost("host3");
        setOsFamily(host, "redhat", "6.3");
        host.persist();

        ClusterRequest clusterRequest = new ClusterRequest(null, "c1", "HDP-1.2.0", null);
        amc.createCluster(clusterRequest);

        Set<ServiceRequest> serviceRequests = new HashSet<ServiceRequest>();
        serviceRequests.add(new ServiceRequest("c1", "HDFS", null));

        ServiceResourceProviderTest.createServices(amc, serviceRequests);

        Type confType = new TypeToken<Map<String, String>>() {
        }.getType();

        ConfigurationRequest configurationRequest = new ConfigurationRequest("c1", type, tag,
            gson.<Map<String, String>>fromJson("{ \"fs.default.name\" : \"localhost:8020\"}", confType), null);
        amc.createConfiguration(configurationRequest);

        amc.createConfiguration(configurationRequest);
      } catch (AmbariException e) {
        exception = e;
      }

      assertNotNull(exception);
      String exceptionMessage = MessageFormat.format("Configuration with tag ''{0}'' exists for ''{1}''",
          tag, type);
      org.junit.Assert.assertEquals(exceptionMessage, exception.getMessage());
    } finally {
      injector.getInstance(PersistService.class).stop();
    }
  }

  @Test
  public void testDeleteClusterCreateHost() throws Exception {

    Injector injector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        Properties properties = new Properties();
        properties.setProperty(Configuration.SERVER_PERSISTENCE_TYPE_KEY, "in-memory");

        properties.setProperty(Configuration.METADATA_DIR_PATH,
            "src/test/resources/stacks");
        properties.setProperty(Configuration.SERVER_VERSION_FILE,
            "../version");
        properties.setProperty(Configuration.OS_VERSION_KEY, "centos6");
        properties.setProperty(Configuration.SHARED_RESOURCES_DIR_KEY, "src/test/resources/");
        try {
          install(new ControllerModule(properties));
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    });
    injector.getInstance(GuiceJpaInitializer.class);


    String STACK_ID = "HDP-2.0.1";
    Long CLUSTER_ID = 1L;
    String CLUSTER_NAME = "c1";
    String HOST1 = "h1";
    String HOST2 = "h2";

    try {
      Clusters clusters = injector.getInstance(Clusters.class);

      clusters.addHost(HOST1);
      Host host = clusters.getHost(HOST1);
      setOsFamily(host, "redhat", "6.3");
      host.persist();

      clusters.addHost(HOST2);
      host = clusters.getHost(HOST2);
      setOsFamily(host, "redhat", "6.3");
      host.persist();

      AmbariManagementController amc = injector.getInstance(AmbariManagementController.class);

      ClusterRequest cr = new ClusterRequest(null, CLUSTER_NAME, STACK_ID, null);
      amc.createCluster(cr);

      ConfigurationRequest configRequest = new ConfigurationRequest(CLUSTER_NAME, "global", "version1",
          new HashMap<String, String>() {{ put("a", "b"); }}, null);
      cr.setDesiredConfig(Collections.singletonList(configRequest));
      cr.setClusterId(CLUSTER_ID);
      amc.updateClusters(Collections.singleton(cr), new HashMap<String, String>());

      // add some hosts
      Set<HostRequest> hrs = new HashSet<HostRequest>();
      hrs.add(new HostRequest(HOST1, CLUSTER_NAME, null));
      HostResourceProviderTest.createHosts(amc, hrs);

      Set<ServiceRequest> serviceRequests = new HashSet<ServiceRequest>();
      serviceRequests.add(new ServiceRequest(CLUSTER_NAME, "HDFS", null));
      serviceRequests.add(new ServiceRequest(CLUSTER_NAME, "MAPREDUCE2", null));
      serviceRequests.add(new ServiceRequest(CLUSTER_NAME, "YARN", null));

      ServiceResourceProviderTest.createServices(amc, serviceRequests);

      Set<ServiceComponentRequest> serviceComponentRequests = new HashSet<ServiceComponentRequest>();
      serviceComponentRequests.add(new ServiceComponentRequest(CLUSTER_NAME, "HDFS", "NAMENODE", null));
      serviceComponentRequests.add(new ServiceComponentRequest(CLUSTER_NAME, "HDFS", "SECONDARY_NAMENODE", null));
      serviceComponentRequests.add(new ServiceComponentRequest(CLUSTER_NAME, "HDFS", "DATANODE", null));
      serviceComponentRequests.add(new ServiceComponentRequest(CLUSTER_NAME, "MAPREDUCE2", "HISTORYSERVER", null));
      serviceComponentRequests.add(new ServiceComponentRequest(CLUSTER_NAME, "YARN", "RESOURCEMANAGER", null));
      serviceComponentRequests.add(new ServiceComponentRequest(CLUSTER_NAME, "YARN", "NODEMANAGER", null));
      serviceComponentRequests.add(new ServiceComponentRequest(CLUSTER_NAME, "HDFS", "HDFS_CLIENT", null));

      ComponentResourceProviderTest.createComponents(amc, serviceComponentRequests);

      Set<ServiceComponentHostRequest> componentHostRequests = new HashSet<ServiceComponentHostRequest>();
      componentHostRequests.add(new ServiceComponentHostRequest(CLUSTER_NAME, null, "DATANODE", HOST1, null));
      componentHostRequests.add(new ServiceComponentHostRequest(CLUSTER_NAME, null, "NAMENODE", HOST1, null));
      componentHostRequests.add(new ServiceComponentHostRequest(CLUSTER_NAME, null, "SECONDARY_NAMENODE", HOST1, null));
      componentHostRequests.add(new ServiceComponentHostRequest(CLUSTER_NAME, null, "HISTORYSERVER", HOST1, null));
      componentHostRequests.add(new ServiceComponentHostRequest(CLUSTER_NAME, null, "RESOURCEMANAGER", HOST1, null));
      componentHostRequests.add(new ServiceComponentHostRequest(CLUSTER_NAME, null, "NODEMANAGER", HOST1, null));
      componentHostRequests.add(new ServiceComponentHostRequest(CLUSTER_NAME, null, "HDFS_CLIENT", HOST1, null));

      amc.createHostComponents(componentHostRequests);

      RequestResourceFilter resourceFilter = new RequestResourceFilter("HDFS", null, null);
      ExecuteActionRequest ar = new ExecuteActionRequest(CLUSTER_NAME, Role.HDFS_SERVICE_CHECK.name(), null, false);
      ar.getResourceFilters().add(resourceFilter);
      amc.createAction(ar, null);

      // change mind, delete the cluster
      amc.deleteCluster(cr);

      assertNotNull(clusters.getHost(HOST1));
      assertNotNull(clusters.getHost(HOST2));

      HostDAO dao = injector.getInstance(HostDAO.class);

      assertNotNull(dao.findByName(HOST1));
      assertNotNull(dao.findByName(HOST2));

    } finally {
      injector.getInstance(PersistService.class).stop();
    }
  }

  @Test
  @Ignore
  public void testDisableAndDeleteStates() throws Exception {
    Map<String,String> mapRequestProps = new HashMap<String, String>();
    Injector injector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        Properties properties = new Properties();
        properties.setProperty(Configuration.SERVER_PERSISTENCE_TYPE_KEY,"in-memory");
        properties.setProperty(Configuration.METADATA_DIR_PATH,"src/test/resources/stacks");
        properties.setProperty(Configuration.SERVER_VERSION_FILE,"src/test/resources/version");
        properties.setProperty(Configuration.OS_VERSION_KEY,"centos5");
        properties.setProperty(Configuration.SHARED_RESOURCES_DIR_KEY, "src/test/resources/");

        try {
          install(new ControllerModule(properties));

          // ambari events interfere with the workflow of this test
          bind(AmbariEventPublisher.class).toInstance(EasyMock.createMock(AmbariEventPublisher.class));
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    });
    injector.getInstance(GuiceJpaInitializer.class);

    try {
      AmbariManagementController amc = injector.getInstance(AmbariManagementController.class);
      Clusters clusters = injector.getInstance(Clusters.class);
      Gson gson = new Gson();

      clusters.addHost("host1");
      clusters.addHost("host2");
      clusters.addHost("host3");
      Host host = clusters.getHost("host1");
      setOsFamily(host, "redhat", "5.9");
      host.persist();
      host = clusters.getHost("host2");
      setOsFamily(host, "redhat", "5.9");
      host.persist();
      host = clusters.getHost("host3");
      setOsFamily(host, "redhat", "5.9");
      host.persist();

      ClusterRequest clusterRequest = new ClusterRequest(null, "c1", "HDP-1.2.0", null);
      amc.createCluster(clusterRequest);

      Set<ServiceRequest> serviceRequests = new HashSet<ServiceRequest>();
      serviceRequests.add(new ServiceRequest("c1", "HDFS", null));
      serviceRequests.add(new ServiceRequest("c1", "HIVE", null));

      ServiceResourceProviderTest.createServices(amc, serviceRequests);

      Type confType = new TypeToken<Map<String, String>>() {
      }.getType();

      ConfigurationRequest configurationRequest = new ConfigurationRequest("c1", "core-site", "version1",
          gson.<Map<String, String>>fromJson("{ \"fs.default.name\" : \"localhost:8020\"}", confType), null
      );
      amc.createConfiguration(configurationRequest);

      configurationRequest = new ConfigurationRequest("c1", "hdfs-site", "version1",
          gson.<Map<String, String>>fromJson("{ \"dfs.datanode.data.dir.perm\" : \"750\"}", confType), null
      );
      amc.createConfiguration(configurationRequest);

      configurationRequest = new ConfigurationRequest("c1", "global", "version1",
          gson.<Map<String, String>>fromJson("{ \"hive.server2.enable.doAs\" : \"true\"}", confType), null
      );
      amc.createConfiguration(configurationRequest);

      Assert.assertTrue(clusters.getCluster("c1").getDesiredConfigs().containsKey("hive-site"));

      serviceRequests.clear();
      serviceRequests.add(new ServiceRequest("c1", "HDFS", null));

      ServiceResourceProviderTest.updateServices(amc, serviceRequests, mapRequestProps, true, false);

      Set<ServiceComponentRequest> serviceComponentRequests = new HashSet<ServiceComponentRequest>();
      serviceComponentRequests.add(new ServiceComponentRequest("c1", "HDFS", "NAMENODE", null));
      serviceComponentRequests.add(new ServiceComponentRequest("c1", "HDFS", "SECONDARY_NAMENODE", null));
      serviceComponentRequests.add(new ServiceComponentRequest("c1", "HDFS", "DATANODE", null));
      serviceComponentRequests.add(new ServiceComponentRequest("c1", "HDFS", "HDFS_CLIENT", null));

      ComponentResourceProviderTest.createComponents(amc, serviceComponentRequests);

      Set<HostRequest> hostRequests = new HashSet<HostRequest>();
      hostRequests.add(new HostRequest("host1", "c1", null));
      hostRequests.add(new HostRequest("host2", "c1", null));
      hostRequests.add(new HostRequest("host3", "c1", null));

      HostResourceProviderTest.createHosts(amc, hostRequests);

      Set<ServiceComponentHostRequest> componentHostRequests = new HashSet<ServiceComponentHostRequest>();
      componentHostRequests.add(new ServiceComponentHostRequest("c1", null, "DATANODE", "host1", null));
      componentHostRequests.add(new ServiceComponentHostRequest("c1", null, "NAMENODE", "host1", null));
      componentHostRequests.add(new ServiceComponentHostRequest("c1", null, "SECONDARY_NAMENODE", "host1", null));
      componentHostRequests.add(new ServiceComponentHostRequest("c1", null, "DATANODE", "host2", null));
      componentHostRequests.add(new ServiceComponentHostRequest("c1", null, "DATANODE", "host3", null));


      amc.createHostComponents(componentHostRequests);

      serviceRequests.clear();
      serviceRequests.add(new ServiceRequest("c1", "HDFS", "INSTALLED"));
      ServiceResourceProviderTest.updateServices(amc, serviceRequests, mapRequestProps, true, false);

      Cluster cluster = clusters.getCluster("c1");
      Map<String, ServiceComponentHost> namenodes = cluster.getService("HDFS").getServiceComponent("NAMENODE").getServiceComponentHosts();
      org.junit.Assert.assertEquals(1, namenodes.size());

      ServiceComponentHost componentHost = namenodes.get("host1");

      Map<String, ServiceComponentHost> hostComponents = cluster.getService("HDFS").getServiceComponent("DATANODE").getServiceComponentHosts();
      for (Map.Entry<String, ServiceComponentHost> entry : hostComponents.entrySet()) {
        ServiceComponentHost cHost = entry.getValue();
        cHost.handleEvent(new ServiceComponentHostInstallEvent(cHost.getServiceComponentName(), cHost.getHostName(), System.currentTimeMillis(), "HDP-1.2.0"));
        cHost.handleEvent(new ServiceComponentHostOpSucceededEvent(cHost.getServiceComponentName(), cHost.getHostName(), System.currentTimeMillis()));
      }
      hostComponents = cluster.getService("HDFS").getServiceComponent("NAMENODE").getServiceComponentHosts();
      for (Map.Entry<String, ServiceComponentHost> entry : hostComponents.entrySet()) {
        ServiceComponentHost cHost = entry.getValue();
        cHost.handleEvent(new ServiceComponentHostInstallEvent(cHost.getServiceComponentName(), cHost.getHostName(), System.currentTimeMillis(), "HDP-1.2.0"));
        cHost.handleEvent(new ServiceComponentHostOpSucceededEvent(cHost.getServiceComponentName(), cHost.getHostName(), System.currentTimeMillis()));
      }
      hostComponents = cluster.getService("HDFS").getServiceComponent("SECONDARY_NAMENODE").getServiceComponentHosts();
      for (Map.Entry<String, ServiceComponentHost> entry : hostComponents.entrySet()) {
        ServiceComponentHost cHost = entry.getValue();
        cHost.handleEvent(new ServiceComponentHostInstallEvent(cHost.getServiceComponentName(), cHost.getHostName(), System.currentTimeMillis(), "HDP-1.2.0"));
        cHost.handleEvent(new ServiceComponentHostOpSucceededEvent(cHost.getServiceComponentName(), cHost.getHostName(), System.currentTimeMillis()));
      }

      componentHostRequests.clear();
      componentHostRequests.add(new ServiceComponentHostRequest("c1", null, "NAMENODE", "host1", "DISABLED"));

      updateHostComponents(amc, componentHostRequests, mapRequestProps, true);

      Assert.assertEquals(State.DISABLED, componentHost.getState());

      componentHostRequests.clear();
      componentHostRequests.add(new ServiceComponentHostRequest("c1", null, "NAMENODE", "host1", "INSTALLED"));

      updateHostComponents(amc, componentHostRequests, mapRequestProps, true);

      Assert.assertEquals(State.INSTALLED, componentHost.getState());

      componentHostRequests.clear();
      componentHostRequests.add(new ServiceComponentHostRequest("c1", null, "NAMENODE", "host1", "DISABLED"));

      updateHostComponents(amc, componentHostRequests, mapRequestProps, true);

      Assert.assertEquals(State.DISABLED, componentHost.getState());

      componentHostRequests.clear();
      componentHostRequests.add(new ServiceComponentHostRequest("c1", null, "NAMENODE", "host2", null));

      amc.createHostComponents(componentHostRequests);

      componentHostRequests.clear();
      componentHostRequests.add(new ServiceComponentHostRequest("c1", null, "NAMENODE", "host2", "INSTALLED"));

      updateHostComponents(amc, componentHostRequests, mapRequestProps, true);

      namenodes = cluster.getService("HDFS").getServiceComponent("NAMENODE").getServiceComponentHosts();
      Assert.assertEquals(2, namenodes.size());

      componentHost = namenodes.get("host2");
      componentHost.handleEvent(new ServiceComponentHostInstallEvent(componentHost.getServiceComponentName(), componentHost.getHostName(), System.currentTimeMillis(), "HDP-1.2.0"));
      componentHost.handleEvent(new ServiceComponentHostOpSucceededEvent(componentHost.getServiceComponentName(), componentHost.getHostName(), System.currentTimeMillis()));

      serviceRequests.clear();
      serviceRequests.add(new ServiceRequest("c1", "HDFS", "STARTED"));

      RequestStatusResponse response = ServiceResourceProviderTest.updateServices(amc, serviceRequests,
          mapRequestProps, true, false);
      for (ShortTaskStatus shortTaskStatus : response.getTasks()) {
        assertFalse("host1".equals(shortTaskStatus.getHostName()) && "NAMENODE".equals(shortTaskStatus.getRole()));
      }

      componentHostRequests.clear();
      componentHostRequests.add(new ServiceComponentHostRequest("c1", null, "NAMENODE", "host1", null));

      amc.deleteHostComponents(componentHostRequests);

      namenodes = cluster.getService("HDFS").getServiceComponent("NAMENODE").getServiceComponentHosts();
      org.junit.Assert.assertEquals(1, namenodes.size());

      // testing the behavior for runSmokeTest flag
      // piggybacking on this test to avoid setting up the mock cluster
      testRunSmokeTestFlag(mapRequestProps, amc, serviceRequests);

      // should be able to add the host component back
      componentHostRequests.clear();
      componentHostRequests.add(new ServiceComponentHostRequest("c1", null, "NAMENODE", "host1", null));
      amc.createHostComponents(componentHostRequests);
      namenodes = cluster.getService("HDFS").getServiceComponent("NAMENODE").getServiceComponentHosts();
      assertEquals(2, namenodes.size());

      // make INSTALLED again
      componentHost = namenodes.get("host1");
      componentHost.handleEvent(new ServiceComponentHostInstallEvent(componentHost.getServiceComponentName(), componentHost.getHostName(), System.currentTimeMillis(), "HDP-1.2.0"));
      componentHost.handleEvent(new ServiceComponentHostOpSucceededEvent(componentHost.getServiceComponentName(), componentHost.getHostName(), System.currentTimeMillis()));
      componentHostRequests.clear();
      componentHostRequests.add(new ServiceComponentHostRequest("c1", null, "NAMENODE", "host1", "INSTALLED"));
      updateHostComponents(amc, componentHostRequests, mapRequestProps, true);
      assertEquals(State.INSTALLED, namenodes.get("host1").getState());

      // make unknown
      ServiceComponentHost sch = null;
      for (ServiceComponentHost tmp : cluster.getServiceComponentHosts("host2")) {
        if (tmp.getServiceComponentName().equals("DATANODE")) {
          tmp.setState(State.UNKNOWN);
          sch = tmp;
        }
      }
      assertNotNull(sch);

      // make disabled
      componentHostRequests.clear();
      componentHostRequests.add(new ServiceComponentHostRequest("c1", null, "DATANODE", "host2", "DISABLED"));
      updateHostComponents(amc, componentHostRequests, mapRequestProps, false);
      org.junit.Assert.assertEquals(State.DISABLED, sch.getState());

      // State should not be changed if componentHostRequests are empty
      componentHostRequests.clear();
      mapRequestProps.put(RequestOperationLevel.OPERATION_CLUSTER_ID,"c1");
      updateHostComponents(amc, componentHostRequests, mapRequestProps, false);
      org.junit.Assert.assertEquals(State.DISABLED, sch.getState());
      mapRequestProps.clear();

      // ServiceComponentHost remains in disabled after service stop
      assertEquals(sch.getServiceComponentName(),"DATANODE");
      serviceRequests.clear();
      serviceRequests.add(new ServiceRequest("c1", "HDFS", "INSTALLED"));
      ServiceResourceProviderTest.updateServices(amc, serviceRequests,
        mapRequestProps, true, false);
      assertEquals(State.DISABLED, sch.getState());

      // ServiceComponentHost remains in disabled after service start
      serviceRequests.clear();
      serviceRequests.add(new ServiceRequest("c1", "HDFS", "STARTED"));
      ServiceResourceProviderTest.updateServices(amc, serviceRequests,
        mapRequestProps, true, false);
      assertEquals(State.DISABLED, sch.getState());

      // confirm delete
      componentHostRequests.clear();
      componentHostRequests.add(new ServiceComponentHostRequest("c1", null, "DATANODE", "host2", null));
      amc.deleteHostComponents(componentHostRequests);

      sch = null;
      for (ServiceComponentHost tmp : cluster.getServiceComponentHosts("host2")) {
        if (tmp.getServiceComponentName().equals("DATANODE")) {
          sch = tmp;
        }
      }
      org.junit.Assert.assertNull(sch);

      /*
      *Test remove service
      */
      serviceRequests.clear();
      serviceRequests.add(new ServiceRequest("c1", "HDFS", "INSTALLED"));
      ServiceResourceProviderTest.updateServices(amc, serviceRequests, mapRequestProps, true, false);
      serviceRequests.clear();
      serviceRequests.add(new ServiceRequest("c1", null, null));
      org.junit.Assert.assertEquals(2, ServiceResourceProviderTest.getServices(amc, serviceRequests).size());
      serviceRequests.clear();
      serviceRequests.add(new ServiceRequest("c1", "HDFS", null));
      serviceRequests.add(new ServiceRequest("c1", "HIVE", null));
      ServiceResourceProviderTest.deleteServices(amc, serviceRequests);
      serviceRequests.clear();
      serviceRequests.add(new ServiceRequest("c1", null, null));
      org.junit.Assert.assertEquals(0, ServiceResourceProviderTest.getServices(amc, serviceRequests).size());

      /*
      *Test add service again
      */
      serviceRequests.clear();
      serviceRequests.add(new ServiceRequest("c1", "HDFS", null));

      ServiceResourceProviderTest.createServices(amc, serviceRequests);

      org.junit.Assert.assertEquals(1, ServiceResourceProviderTest.getServices(amc, serviceRequests).size());
      //Create new configs
      configurationRequest = new ConfigurationRequest("c1", "core-site", "version2",
          gson.<Map<String, String>>fromJson("{ \"fs.default.name\" : \"localhost:8020\"}", confType), null
      );
      amc.createConfiguration(configurationRequest);
      configurationRequest = new ConfigurationRequest("c1", "hdfs-site", "version2",
          gson.<Map<String, String>>fromJson("{ \"dfs.datanode.data.dir.perm\" : \"750\"}", confType), null
      );
      amc.createConfiguration(configurationRequest);
      configurationRequest = new ConfigurationRequest("c1", "global", "version2",
          gson.<Map<String, String>>fromJson("{ \"hbase_hdfs_root_dir\" : \"/apps/hbase/\"}", confType), null
      );
      amc.createConfiguration(configurationRequest);
      //Add configs to service
      serviceRequests.clear();
      serviceRequests.add(new ServiceRequest("c1", "HDFS", null));
      ServiceResourceProviderTest.updateServices(amc, serviceRequests, mapRequestProps, true, false);
      //Crate service components
      serviceComponentRequests = new HashSet<ServiceComponentRequest>();
      serviceComponentRequests.add(new ServiceComponentRequest("c1", "HDFS", "NAMENODE", null));
      serviceComponentRequests.add(new ServiceComponentRequest("c1", "HDFS", "SECONDARY_NAMENODE", null));
      serviceComponentRequests.add(new ServiceComponentRequest("c1", "HDFS", "DATANODE", null));
      serviceComponentRequests.add(new ServiceComponentRequest("c1", "HDFS", "HDFS_CLIENT", null));
      ComponentResourceProviderTest.createComponents(amc, serviceComponentRequests);

      //Create ServiceComponentHosts
      componentHostRequests = new HashSet<ServiceComponentHostRequest>();
      componentHostRequests.add(new ServiceComponentHostRequest("c1", null, "DATANODE", "host1", null));
      componentHostRequests.add(new ServiceComponentHostRequest("c1", null, "NAMENODE", "host1", null));
      componentHostRequests.add(new ServiceComponentHostRequest("c1", null, "SECONDARY_NAMENODE", "host1", null));
      componentHostRequests.add(new ServiceComponentHostRequest("c1", null, "DATANODE", "host2", null));
      componentHostRequests.add(new ServiceComponentHostRequest("c1", null, "DATANODE", "host3", null));
      amc.createHostComponents(componentHostRequests);


      namenodes = cluster.getService("HDFS").getServiceComponent("NAMENODE").getServiceComponentHosts();
      org.junit.Assert.assertEquals(1, namenodes.size());
      Map<String, ServiceComponentHost> datanodes = cluster.getService("HDFS").getServiceComponent("DATANODE").getServiceComponentHosts();
      org.junit.Assert.assertEquals(3, datanodes.size());
      Map<String, ServiceComponentHost> namenodes2 = cluster.getService("HDFS").getServiceComponent("SECONDARY_NAMENODE").getServiceComponentHosts();
      org.junit.Assert.assertEquals(1, namenodes2.size());
    } finally {
      injector.getInstance(PersistService.class).stop();
    }
  }

  @Test
  public void testScheduleSmokeTest() throws Exception {

    final String HOST1 = "host1";
    final String OS_TYPE = "centos5";
    final String STACK_ID = "HDP-2.0.1";
    final String CLUSTER_NAME = "c1";
    final String HDFS_SERVICE_CHECK_ROLE = "HDFS_SERVICE_CHECK";
    final String MAPREDUCE2_SERVICE_CHECK_ROLE = "MAPREDUCE2_SERVICE_CHECK";
    final String YARN_SERVICE_CHECK_ROLE = "YARN_SERVICE_CHECK";

    Map<String,String> mapRequestProps = Collections.emptyMap();
    Injector injector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        Properties properties = new Properties();
        properties.setProperty(Configuration.SERVER_PERSISTENCE_TYPE_KEY, "in-memory");

        properties.setProperty(Configuration.METADATA_DIR_PATH,
            "src/test/resources/stacks");
        properties.setProperty(Configuration.SERVER_VERSION_FILE,
            "../version");
        properties.setProperty(Configuration.OS_VERSION_KEY, OS_TYPE);
        properties.setProperty(Configuration.SHARED_RESOURCES_DIR_KEY, "src/test/resources/");
        try {
          install(new ControllerModule(properties));
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    });
    injector.getInstance(GuiceJpaInitializer.class);

    try {
      AmbariManagementController amc = injector.getInstance(AmbariManagementController.class);
      Clusters clusters = injector.getInstance(Clusters.class);

      clusters.addHost(HOST1);
      Host host = clusters.getHost(HOST1);
      setOsFamily(host, "redhat", "5.9");
      host.persist();

      ClusterRequest clusterRequest = new ClusterRequest(null, CLUSTER_NAME, STACK_ID, null);
      amc.createCluster(clusterRequest);

      Set<ServiceRequest> serviceRequests = new HashSet<ServiceRequest>();
      serviceRequests.add(new ServiceRequest(CLUSTER_NAME, "HDFS", null));
      serviceRequests.add(new ServiceRequest(CLUSTER_NAME, "MAPREDUCE2", null));
      serviceRequests.add(new ServiceRequest(CLUSTER_NAME, "YARN", null));

      ServiceResourceProviderTest.createServices(amc, serviceRequests);

      Set<ServiceComponentRequest> serviceComponentRequests = new HashSet<ServiceComponentRequest>();
      serviceComponentRequests.add(new ServiceComponentRequest(CLUSTER_NAME, "HDFS", "NAMENODE", null));
      serviceComponentRequests.add(new ServiceComponentRequest(CLUSTER_NAME, "HDFS", "SECONDARY_NAMENODE", null));
      serviceComponentRequests.add(new ServiceComponentRequest(CLUSTER_NAME, "HDFS", "DATANODE", null));
      serviceComponentRequests.add(new ServiceComponentRequest(CLUSTER_NAME, "MAPREDUCE2", "HISTORYSERVER", null));
      serviceComponentRequests.add(new ServiceComponentRequest(CLUSTER_NAME, "YARN", "RESOURCEMANAGER", null));
      serviceComponentRequests.add(new ServiceComponentRequest(CLUSTER_NAME, "YARN", "NODEMANAGER", null));

      ComponentResourceProviderTest.createComponents(amc, serviceComponentRequests);

      Set<HostRequest> hostRequests = new HashSet<HostRequest>();
      hostRequests.add(new HostRequest(HOST1, CLUSTER_NAME, null));

      HostResourceProviderTest.createHosts(amc, hostRequests);

      Set<ServiceComponentHostRequest> componentHostRequests = new HashSet<ServiceComponentHostRequest>();
      componentHostRequests.add(new ServiceComponentHostRequest(CLUSTER_NAME, null, "DATANODE", HOST1, null));
      componentHostRequests.add(new ServiceComponentHostRequest(CLUSTER_NAME, null, "NAMENODE", HOST1, null));
      componentHostRequests.add(new ServiceComponentHostRequest(CLUSTER_NAME, null, "SECONDARY_NAMENODE", HOST1, null));
      componentHostRequests.add(new ServiceComponentHostRequest(CLUSTER_NAME, null, "HISTORYSERVER", HOST1, null));
      componentHostRequests.add(new ServiceComponentHostRequest(CLUSTER_NAME, null, "RESOURCEMANAGER", HOST1, null));
      componentHostRequests.add(new ServiceComponentHostRequest(CLUSTER_NAME, null, "NODEMANAGER", HOST1, null));

      amc.createHostComponents(componentHostRequests);

      //Install services
      serviceRequests.clear();
      serviceRequests.add(new ServiceRequest(CLUSTER_NAME, "HDFS", State.INSTALLED.name()));
      serviceRequests.add(new ServiceRequest(CLUSTER_NAME, "MAPREDUCE2", State.INSTALLED.name()));
      serviceRequests.add(new ServiceRequest(CLUSTER_NAME, "YARN", State.INSTALLED.name()));

      ServiceResourceProviderTest.updateServices(amc, serviceRequests, mapRequestProps, true, false);

      Cluster cluster = clusters.getCluster(CLUSTER_NAME);

      for (String serviceName : cluster.getServices().keySet() ) {

        for(String componentName: cluster.getService(serviceName).getServiceComponents().keySet()) {

          Map<String, ServiceComponentHost> serviceComponentHosts = cluster.getService(serviceName).getServiceComponent(componentName).getServiceComponentHosts();

          for (Map.Entry<String, ServiceComponentHost> entry : serviceComponentHosts.entrySet()) {
            ServiceComponentHost cHost = entry.getValue();
            cHost.handleEvent(new ServiceComponentHostInstallEvent(cHost.getServiceComponentName(), cHost.getHostName(), System.currentTimeMillis(), STACK_ID));
            cHost.handleEvent(new ServiceComponentHostOpSucceededEvent(cHost.getServiceComponentName(), cHost.getHostName(), System.currentTimeMillis()));
          }
        }
      }

      //Start services
      serviceRequests.clear();
      serviceRequests.add(new ServiceRequest(CLUSTER_NAME, "HDFS", State.STARTED.name()));
      serviceRequests.add(new ServiceRequest(CLUSTER_NAME, "MAPREDUCE2", State.STARTED.name()));
      serviceRequests.add(new ServiceRequest(CLUSTER_NAME, "YARN", State.STARTED.name()));

      RequestStatusResponse response = ServiceResourceProviderTest.updateServices(amc, serviceRequests,
          mapRequestProps, true, false);

      Collection<?> hdfsSmokeTasks = CollectionUtils.select(response.getTasks(), new RolePredicate(HDFS_SERVICE_CHECK_ROLE));
      //Ensure that smoke test task was created for HDFS
      org.junit.Assert.assertEquals(1, hdfsSmokeTasks.size());

      Collection<?> mapreduce2SmokeTasks = CollectionUtils.select(response.getTasks(), new RolePredicate(MAPREDUCE2_SERVICE_CHECK_ROLE));
      //Ensure that smoke test task was created for MAPREDUCE2
      org.junit.Assert.assertEquals(1, mapreduce2SmokeTasks.size());

      Collection<?> yarnSmokeTasks = CollectionUtils.select(response.getTasks(), new RolePredicate(YARN_SERVICE_CHECK_ROLE));
      //Ensure that smoke test task was created for YARN
      org.junit.Assert.assertEquals(1, yarnSmokeTasks.size());
    } finally {
      injector.getInstance(PersistService.class).stop();
    }
  }

  @Test
  public void testGetServices2() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = new Capture<AmbariManagementController>();
    Clusters clusters = createNiceMock(Clusters.class);
    MaintenanceStateHelper maintHelper = createNiceMock(MaintenanceStateHelper.class);

    Cluster cluster = createNiceMock(Cluster.class);
    Service service = createNiceMock(Service.class);
    ServiceResponse response = createNiceMock(ServiceResponse.class);

    // requests
    ServiceRequest request1 = new ServiceRequest("cluster1", "service1", null);

    Set<ServiceRequest> setRequests = new HashSet<ServiceRequest>();
    setRequests.add(request1);

    // expectations
    // constructor init
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(null);
    expect(injector.getInstance(MaintenanceStateHelper.class)).andReturn(maintHelper);
    expect(injector.getInstance(KerberosHelper.class)).andReturn(createStrictMock(KerberosHelper.class));

    // getServices
    expect(clusters.getCluster("cluster1")).andReturn(cluster);
    expect(cluster.getService("service1")).andReturn(service);

    expect(service.convertToResponse()).andReturn(response);
    // replay mocks
    replay(maintHelper, injector, clusters, cluster, service, response);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);
    Set<ServiceResponse> setResponses = ServiceResourceProviderTest.getServices(controller, setRequests);

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    assertEquals(1, setResponses.size());
    assertTrue(setResponses.contains(response));

    verify(injector, clusters, cluster, service, response);
  }

  /**
   * Ensure that ServiceNotFoundException is propagated in case where there is a single request.
   */
  @Test
  public void testGetServices___ServiceNotFoundException() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = new Capture<AmbariManagementController>();
    Clusters clusters = createNiceMock(Clusters.class);
    MaintenanceStateHelper maintHelper = createNiceMock(MaintenanceStateHelper.class);
    Cluster cluster = createNiceMock(Cluster.class);

    // requests
    ServiceRequest request1 = new ServiceRequest("cluster1", "service1", null);
    Set<ServiceRequest> setRequests = new HashSet<ServiceRequest>();
    setRequests.add(request1);

    // expectations
    // constructor init
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(null);
    expect(injector.getInstance(MaintenanceStateHelper.class)).andReturn(maintHelper);
    expect(injector.getInstance(KerberosHelper.class)).andReturn(createStrictMock(KerberosHelper.class));

    // getServices
    expect(clusters.getCluster("cluster1")).andReturn(cluster);
    expect(cluster.getService("service1")).andThrow(new ServiceNotFoundException("custer1", "service1"));

    // replay mocks
    replay(maintHelper, injector, clusters, cluster);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);

    // assert that exception is thrown in case where there is a single request
    try {
      ServiceResourceProviderTest.getServices(controller, setRequests);
      fail("expected ServiceNotFoundException");
    } catch (ServiceNotFoundException e) {
      // expected
    }

    assertSame(controller, controllerCapture.getValue());
    verify(injector, clusters, cluster);
  }

  /**
   * Ensure that ServiceNotFoundException is handled where there are multiple requests as would be the
   * case when an OR predicate is provided in the query.
   */
  @Test
  public void testGetServices___OR_Predicate_ServiceNotFoundException() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = new Capture<AmbariManagementController>();
    Clusters clusters = createNiceMock(Clusters.class);
    MaintenanceStateHelper maintHelper = createNiceMock(MaintenanceStateHelper.class);

    Cluster cluster = createNiceMock(Cluster.class);
    Service service1 = createNiceMock(Service.class);
    Service service2 = createNiceMock(Service.class);
    ServiceResponse response = createNiceMock(ServiceResponse.class);
    ServiceResponse response2 = createNiceMock(ServiceResponse.class);

    // requests
    ServiceRequest request1 = new ServiceRequest("cluster1", "service1", null);
    ServiceRequest request2 = new ServiceRequest("cluster1", "service2", null);
    ServiceRequest request3 = new ServiceRequest("cluster1", "service3", null);
    ServiceRequest request4 = new ServiceRequest("cluster1", "service4", null);

    Set<ServiceRequest> setRequests = new HashSet<ServiceRequest>();
    setRequests.add(request1);
    setRequests.add(request2);
    setRequests.add(request3);
    setRequests.add(request4);

    // expectations
    // constructor init
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(null);
    expect(injector.getInstance(MaintenanceStateHelper.class)).andReturn(maintHelper);
    expect(injector.getInstance(KerberosHelper.class)).andReturn(createStrictMock(KerberosHelper.class));

    // getServices
    expect(clusters.getCluster("cluster1")).andReturn(cluster).times(4);
    expect(cluster.getService("service1")).andReturn(service1);
    expect(cluster.getService("service2")).andThrow(new ServiceNotFoundException("cluster1", "service2"));
    expect(cluster.getService("service3")).andThrow(new ServiceNotFoundException("cluster1", "service3"));
    expect(cluster.getService("service4")).andReturn(service2);

    expect(service1.convertToResponse()).andReturn(response);
    expect(service2.convertToResponse()).andReturn(response2);
    // replay mocks
    replay(maintHelper, injector, clusters, cluster, service1, service2,
      response, response2);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);
    Set<ServiceResponse> setResponses = ServiceResourceProviderTest.getServices(controller, setRequests);

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    assertEquals(2, setResponses.size());
    assertTrue(setResponses.contains(response));
    assertTrue(setResponses.contains(response2));

    verify(injector, clusters, cluster, service1, service2, response, response2);
  }

  private void testRunSmokeTestFlag(Map<String, String> mapRequestProps,
                                    AmbariManagementController amc,
                                    Set<ServiceRequest> serviceRequests)
      throws AmbariException {
    RequestStatusResponse response;//Starting HDFS service. No run_smoke_test flag is set, smoke

    //Stopping HDFS service
    serviceRequests.clear();
    serviceRequests.add(new ServiceRequest("c1", "HDFS", "INSTALLED"));
    ServiceResourceProviderTest.updateServices(amc, serviceRequests, mapRequestProps, false,
        false);

    //Starting HDFS service. No run_smoke_test flag is set, smoke
    // test(HDFS_SERVICE_CHECK) won't run
    boolean runSmokeTest = false;
    serviceRequests.clear();
    serviceRequests.add(new ServiceRequest("c1", "HDFS", "STARTED"));
    response = ServiceResourceProviderTest.updateServices(amc, serviceRequests, mapRequestProps,
        runSmokeTest, false);

    List<ShortTaskStatus> taskStatuses = response.getTasks();
    boolean smokeTestRequired = false;
    for (ShortTaskStatus shortTaskStatus : taskStatuses) {
      if (shortTaskStatus.getRole().equals(Role.HDFS_SERVICE_CHECK.toString())) {
        smokeTestRequired= true;
      }
    }
    assertFalse(smokeTestRequired);

    //Stopping HDFS service
    serviceRequests.clear();
    serviceRequests.add(new ServiceRequest("c1", "HDFS", "INSTALLED"));
    ServiceResourceProviderTest.updateServices(amc, serviceRequests, mapRequestProps, false,
        false);

    //Starting HDFS service again.
    //run_smoke_test flag is set, smoke test will be run
    runSmokeTest = true;
    serviceRequests.clear();
    serviceRequests.add(new ServiceRequest("c1", "HDFS", "STARTED"));
    response = ServiceResourceProviderTest.updateServices(amc, serviceRequests, mapRequestProps,
        runSmokeTest, false);

    taskStatuses = response.getTasks();
    smokeTestRequired = false;
    for (ShortTaskStatus shortTaskStatus : taskStatuses) {
      if (shortTaskStatus.getRole().equals(Role.HDFS_SERVICE_CHECK.toString())) {
        smokeTestRequired= true;
      }
    }
    assertTrue(smokeTestRequired);
  }

  private class RolePredicate implements org.apache.commons.collections.Predicate {

    private String role;

    public RolePredicate(String role) {
      this.role = role;
    }

    @Override
    public boolean evaluate(Object obj) {
      ShortTaskStatus task = (ShortTaskStatus)obj;
      return task.getRole().equals(role);
    }
  }

  @Test
  public void testReinstallClientSchSkippedInMaintenance() throws Exception {
    Cluster c1 = setupClusterWithHosts("c1", "HDP-1.2.0",
      new ArrayList<String>() {{
        add("h1");
        add("h2");
        add("h3");
      }},
      "centos5");

    Service hdfs = c1.addService("HDFS");
    hdfs.persist();
    createServiceComponent("c1", "HDFS", "NAMENODE", State.INIT);
    createServiceComponent("c1", "HDFS", "DATANODE", State.INIT);
    createServiceComponent("c1", "HDFS", "HDFS_CLIENT", State.INIT);

    createServiceComponentHost("c1", "HDFS", "NAMENODE", "h1", State.INIT);
    createServiceComponentHost("c1", "HDFS", "DATANODE", "h1", State.INIT);
    createServiceComponentHost("c1", "HDFS", "HDFS_CLIENT", "h1", State.INIT);
    createServiceComponentHost("c1", "HDFS", "HDFS_CLIENT", "h2", State.INIT);
    createServiceComponentHost("c1", "HDFS", "HDFS_CLIENT", "h3", State.INIT);

    installService("c1", "HDFS", false, false);

    clusters.getHost("h3").setMaintenanceState(c1.getClusterId(), MaintenanceState.ON);

    Long id = startService("c1", "HDFS", false ,true);

    Assert.assertNotNull(id);
    List<Stage> stages = actionDB.getAllStages(id);
    Assert.assertNotNull(stages);
    HostRoleCommand hrc1 = null;
    HostRoleCommand hrc2 = null;
    HostRoleCommand hrc3 = null;
    for (Stage s : stages) {
      for (HostRoleCommand hrc : s.getOrderedHostRoleCommands()) {
        if (hrc.getRole().equals(Role.HDFS_CLIENT) && hrc.getHostName().equals("h1")) {
          hrc1 = hrc;
        } else if (hrc.getRole().equals(Role.HDFS_CLIENT) && hrc.getHostName().equals("h2")) {
          hrc2 = hrc;
        } else if (hrc.getRole().equals(Role.HDFS_CLIENT) && hrc.getHostName().equals("h3")) {
          hrc3 = hrc;
        }
      }
    }

    Assert.assertNotNull(hrc1);
    Assert.assertNotNull(hrc2);
    Assert.assertNull(hrc3);
  }

  @Test
  public void setMonitoringServicesRestartRequired() throws Exception {
    String clusterName = "c1";
    createCluster(clusterName);
    Cluster cluster = clusters.getCluster(clusterName);
    StackId stackId = new StackId("HDP-2.0.8");
    cluster.setDesiredStackVersion(stackId);
    cluster.setCurrentStackVersion(stackId);

    String hdfsService = "HDFS";
    String fakeMonitoringService = "FAKENAGIOS";
    createService(clusterName, hdfsService, null);
    createService(clusterName, fakeMonitoringService, null);

    String namenode = "NAMENODE";
    String datanode = "DATANODE";
    String hdfsClient = "HDFS_CLIENT";
    String fakeServer = "FAKE_MONITORING_SERVER";

    createServiceComponent(clusterName, hdfsService, namenode,
      State.INIT);
    createServiceComponent(clusterName, hdfsService, datanode,
      State.INIT);
    createServiceComponent(clusterName, fakeMonitoringService, fakeServer,
      State.INIT);

    String host1 = "h1";

    addHostToCluster(host1, clusterName);
    createServiceComponentHost(clusterName, hdfsService, namenode, host1, null);
    createServiceComponentHost(clusterName, hdfsService, datanode, host1, null);
    createServiceComponentHost(clusterName, fakeMonitoringService, fakeServer, host1,
      null);


    ServiceComponentHost monitoringServiceComponentHost = null;
    for (ServiceComponentHost sch : cluster.getServiceComponentHosts(host1)) {
      if (sch.getServiceComponentName().equals(fakeServer)) {
        monitoringServiceComponentHost = sch;
      }
    }

    assertFalse(monitoringServiceComponentHost.isRestartRequired());

    createServiceComponent(clusterName, hdfsService, hdfsClient,
      State.INIT);

    createServiceComponentHost(clusterName, hdfsService, hdfsClient, host1, null);

    assertTrue(monitoringServiceComponentHost.isRestartRequired());
  }

  @Test
  public void setRestartRequiredAfterChangeService() throws Exception {
    String clusterName = "c1";
    createCluster(clusterName);
    Cluster cluster = clusters.getCluster(clusterName);
    StackId stackId = new StackId("HDP-2.0.7");
    cluster.setDesiredStackVersion(stackId);
    cluster.setCurrentStackVersion(stackId);

    String hdfsService = "HDFS";
    String zookeeperService = "ZOOKEEPER";
    createService(clusterName, hdfsService, null);
    createService(clusterName, zookeeperService, null);

    String namenode = "NAMENODE";
    String datanode = "DATANODE";
    String hdfsClient = "HDFS_CLIENT";
    String zookeeperServer = "ZOOKEEPER_SERVER";
    String zookeeperClient = "ZOOKEEPER_CLIENT";

    createServiceComponent(clusterName, hdfsService, namenode,
      State.INIT);
    createServiceComponent(clusterName, hdfsService, datanode,
      State.INIT);
    createServiceComponent(clusterName, zookeeperService, zookeeperServer,
      State.INIT);
    createServiceComponent(clusterName, zookeeperService, zookeeperClient,
      State.INIT);

    String host1 = "h1";
    String host2 = "h2";

    addHostToCluster(host1, clusterName);
    createServiceComponentHost(clusterName, hdfsService, namenode, host1, null);
    createServiceComponentHost(clusterName, hdfsService, datanode, host1, null);
    createServiceComponentHost(clusterName, zookeeperService, zookeeperServer, host1,
      null);
    createServiceComponentHost(clusterName, zookeeperService, zookeeperClient, host1,
      null);

    ServiceComponentHost zookeeperSch = null;
    for (ServiceComponentHost sch : cluster.getServiceComponentHosts(host1)) {
      if (sch.getServiceComponentName().equals(zookeeperServer)) {
        zookeeperSch = sch;
      }
    }
    assertFalse(zookeeperSch.isRestartRequired());

    addHostToCluster(host2, clusterName);
    createServiceComponentHost(clusterName, zookeeperService, zookeeperClient, host2, null);

    assertFalse(zookeeperSch.isRestartRequired());  //No restart required if adding host

    createServiceComponentHost(clusterName, zookeeperService, zookeeperServer, host2, null);

    assertTrue(zookeeperSch.isRestartRequired());  //Add zk server required restart

    deleteServiceComponentHost(clusterName, zookeeperService, zookeeperServer, host2, null);
    deleteServiceComponentHost(clusterName, zookeeperService, zookeeperClient, host2, null);
    deleteHost(host2);

    assertTrue(zookeeperSch.isRestartRequired());   //Restart if removing host!
  }

  @Test
  public void testMaintenanceState() throws Exception {
    String clusterName = "c1";
    createCluster(clusterName);
    clusters.getCluster(clusterName).setDesiredStackVersion(
        new StackId("HDP-1.2.0"));

    String serviceName = "HDFS";
    createService(clusterName, serviceName, null);

    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";

    createServiceComponent(clusterName, serviceName, componentName1, State.INIT);
    createServiceComponent(clusterName, serviceName, componentName2, State.INIT);
    createServiceComponent(clusterName, serviceName, componentName3, State.INIT);

    String host1 = "h1";
    String host2 = "h2";

    addHostToCluster(host1, clusterName);
    addHostToCluster(host2, clusterName);

    createServiceComponentHost(clusterName, serviceName, componentName1, host1,
        null);
    createServiceComponentHost(clusterName, serviceName, componentName2, host1,
        null);
    createServiceComponentHost(clusterName, serviceName, componentName2, host2,
        null);

    Map<String, String> requestProperties = new HashMap<String, String>();
    requestProperties.put("context", "Called from a test");

    Cluster cluster = clusters.getCluster(clusterName);
    Service service = cluster.getService(serviceName);
    Map<String, Host> hosts = clusters.getHostsForCluster(clusterName);

    MaintenanceStateHelper maintenanceStateHelper = MaintenanceStateHelperTest.getMaintenanceStateHelperInstance(clusters);

    // test updating a service
    ServiceRequest sr = new ServiceRequest(clusterName, serviceName, null);
    sr.setMaintenanceState(MaintenanceState.ON.name());
    ServiceResourceProviderTest.updateServices(controller,
        Collections.singleton(sr), requestProperties, false, false,
        maintenanceStateHelper);
    Assert.assertEquals(MaintenanceState.ON, service.getMaintenanceState());

    // check the host components implied state vs desired state
    for (ServiceComponent sc : service.getServiceComponents().values()) {
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        Assert.assertEquals(MaintenanceState.IMPLIED_FROM_SERVICE,
            controller.getEffectiveMaintenanceState(sch));
        Assert.assertEquals(MaintenanceState.OFF, sch.getMaintenanceState());
      }
    }

    // reset
    sr.setMaintenanceState(MaintenanceState.OFF.name());
    ServiceResourceProviderTest.updateServices(controller,
        Collections.singleton(sr), requestProperties, false, false,
        maintenanceStateHelper);
    Assert.assertEquals(MaintenanceState.OFF, service.getMaintenanceState());

    // check the host components implied state vs desired state
    for (ServiceComponent sc : service.getServiceComponents().values()) {
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        Assert.assertEquals(MaintenanceState.OFF,
            controller.getEffectiveMaintenanceState(sch));
        Assert.assertEquals(MaintenanceState.OFF, sch.getMaintenanceState());
      }
    }

    // passivate a host
    HostRequest hr = new HostRequest(host1, clusterName, requestProperties);
    hr.setMaintenanceState(MaintenanceState.ON.name());
    HostResourceProviderTest.updateHosts(controller, Collections.singleton(hr)
    );

    Host host = hosts.get(host1);
    Assert.assertEquals(MaintenanceState.ON,
        host.getMaintenanceState(cluster.getClusterId()));

    // check the host components implied state vs desired state, only for
    // affected hosts
    for (ServiceComponent sc : service.getServiceComponents().values()) {
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        MaintenanceState implied = controller.getEffectiveMaintenanceState(sch);
        if (sch.getHostName().equals(host1)) {
          Assert.assertEquals(MaintenanceState.IMPLIED_FROM_HOST, implied);
        } else {
          Assert.assertEquals(MaintenanceState.OFF, implied);
        }
        Assert.assertEquals(MaintenanceState.OFF, sch.getMaintenanceState());
      }
    }

    // reset
    hr.setMaintenanceState(MaintenanceState.OFF.name());
    HostResourceProviderTest.updateHosts(controller, Collections.singleton(hr)
    );

    host = hosts.get(host1);
    Assert.assertEquals(MaintenanceState.OFF,
        host.getMaintenanceState(cluster.getClusterId()));

    // check the host components active state vs desired state
    for (ServiceComponent sc : service.getServiceComponents().values()) {
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        Assert.assertEquals(MaintenanceState.OFF,
            controller.getEffectiveMaintenanceState(sch));
        Assert.assertEquals(MaintenanceState.OFF, sch.getMaintenanceState());
      }
    }

    // passivate several hosts
    HostRequest hr1 = new HostRequest(host1, clusterName, requestProperties);
    hr1.setMaintenanceState(MaintenanceState.ON.name());
    HostRequest hr2 = new HostRequest(host2, clusterName, requestProperties);
    hr2.setMaintenanceState(MaintenanceState.ON.name());
    Set<HostRequest> set = new HashSet<HostRequest>();
    set.add(hr1);
    set.add(hr2);
    HostResourceProviderTest.updateHosts(controller, set
    );

    host = hosts.get(host1);
    Assert.assertEquals(MaintenanceState.ON,
        host.getMaintenanceState(cluster.getClusterId()));
    host = hosts.get(host2);
    Assert.assertEquals(MaintenanceState.ON,
        host.getMaintenanceState(cluster.getClusterId()));

    // reset
    hr1 = new HostRequest(host1, clusterName, requestProperties);
    hr1.setMaintenanceState(MaintenanceState.OFF.name());
    hr2 = new HostRequest(host2, clusterName, requestProperties);
    hr2.setMaintenanceState(MaintenanceState.OFF.name());
    set = new HashSet<HostRequest>();
    set.add(hr1);
    set.add(hr2);

    HostResourceProviderTest.updateHosts(controller, set
    );
    host = hosts.get(host1);
    Assert.assertEquals(MaintenanceState.OFF,
        host.getMaintenanceState(cluster.getClusterId()));
    host = hosts.get(host2);
    Assert.assertEquals(MaintenanceState.OFF,
        host.getMaintenanceState(cluster.getClusterId()));

    // only do one SCH
    ServiceComponentHost targetSch = service.getServiceComponent(componentName2).getServiceComponentHosts().get(
        host2);
    Assert.assertNotNull(targetSch);
    targetSch.setMaintenanceState(MaintenanceState.ON);

    // check the host components active state vs desired state
    Assert.assertEquals(MaintenanceState.ON,
        controller.getEffectiveMaintenanceState(targetSch));

    // update the service
    service.setMaintenanceState(MaintenanceState.ON);
    Assert.assertEquals(MaintenanceState.ON,
        controller.getEffectiveMaintenanceState(targetSch));

    // make SCH active
    targetSch.setMaintenanceState(MaintenanceState.OFF);
    Assert.assertEquals(MaintenanceState.IMPLIED_FROM_SERVICE,
        controller.getEffectiveMaintenanceState(targetSch));

    // update the service
    service.setMaintenanceState(MaintenanceState.OFF);
    Assert.assertEquals(MaintenanceState.OFF,
        controller.getEffectiveMaintenanceState(targetSch));

    host = hosts.get(host2);
    // update host
    host.setMaintenanceState(cluster.getClusterId(), MaintenanceState.ON);
    Assert.assertEquals(MaintenanceState.IMPLIED_FROM_HOST,
        controller.getEffectiveMaintenanceState(targetSch));

    targetSch.setMaintenanceState(MaintenanceState.ON);
    Assert.assertEquals(MaintenanceState.ON,
        controller.getEffectiveMaintenanceState(targetSch));

    // check the host components active state vs desired state
    for (ServiceComponent sc : service.getServiceComponents().values()) {
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        Assert.assertEquals(State.INIT, sch.getState());
      }
    }

    long id1 = installService(clusterName, serviceName, false, false,
        maintenanceStateHelper, null);

    List<HostRoleCommand> hdfsCmds = actionDB.getRequestTasks(id1);
    Assert.assertNotNull(hdfsCmds);

    HostRoleCommand datanodeCmd = null;

    for (HostRoleCommand cmd : hdfsCmds) {
      if (cmd.getRole().equals(Role.DATANODE)) {
        datanodeCmd = cmd;
      }
    }

    Assert.assertNotNull(datanodeCmd);

    // verify passive sch was skipped
    for (ServiceComponent sc : service.getServiceComponents().values()) {
      if (!sc.getName().equals(componentName2)) {
        continue;
      }

      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        Assert.assertEquals(sch == targetSch ? State.INIT : State.INSTALLED,
            sch.getState());
      }
    }
  }

  @Test
  public void testPassiveSkipServices() throws Exception {
    String clusterName = "c1";
    createCluster(clusterName);
    clusters.getCluster(clusterName)
        .setDesiredStackVersion(new StackId("HDP-0.1"));

    String serviceName1 = "HDFS";
    String serviceName2 = "MAPREDUCE";
    createService(clusterName, serviceName1, null);
    createService(clusterName, serviceName2, null);

    String componentName1_1 = "NAMENODE";
    String componentName1_2 = "DATANODE";
    String componentName1_3 = "HDFS_CLIENT";
    createServiceComponent(clusterName, serviceName1, componentName1_1,
        State.INIT);
    createServiceComponent(clusterName, serviceName1, componentName1_2,
        State.INIT);
    createServiceComponent(clusterName, serviceName1, componentName1_3,
        State.INIT);

    String componentName2_1 = "JOBTRACKER";
    String componentName2_2 = "TASKTRACKER";
    createServiceComponent(clusterName, serviceName2, componentName2_1,
        State.INIT);
    createServiceComponent(clusterName, serviceName2, componentName2_2,
        State.INIT);

    String host1 = "h1";
    String host2 = "h2";

    addHostToCluster(host1, clusterName);
    addHostToCluster(host2, clusterName);

    createServiceComponentHost(clusterName, serviceName1, componentName1_1, host1, null);
    createServiceComponentHost(clusterName, serviceName1, componentName1_2, host1, null);
    createServiceComponentHost(clusterName, serviceName1, componentName1_2, host2, null);

    createServiceComponentHost(clusterName, serviceName2, componentName2_1, host1, null);
    createServiceComponentHost(clusterName, serviceName2, componentName2_2, host2, null);

    MaintenanceStateHelper maintenanceStateHelper =
            MaintenanceStateHelperTest.getMaintenanceStateHelperInstance(clusters);

    installService(clusterName, serviceName1, false, false, maintenanceStateHelper, null);
    installService(clusterName, serviceName2, false, false, maintenanceStateHelper, null);

    startService(clusterName, serviceName1, false, false, maintenanceStateHelper);
    startService(clusterName, serviceName2, false, false, maintenanceStateHelper);

    Map<String, String> requestProperties = new HashMap<String, String>();
    requestProperties.put("context", "Called from a test");

    Cluster cluster = clusters.getCluster(clusterName);

    for (Service service : cluster.getServices().values()) {
      Assert.assertEquals(State.STARTED, service.getDesiredState());
    }

    Service service2 = cluster.getService(serviceName2);
    service2.setMaintenanceState(MaintenanceState.ON);

    Set<ServiceRequest> srs = new HashSet<ServiceRequest>();
    srs.add(new ServiceRequest(clusterName, serviceName1, State.INSTALLED.name()));
    srs.add(new ServiceRequest(clusterName, serviceName2, State.INSTALLED.name()));
    RequestStatusResponse rsr = ServiceResourceProviderTest.updateServices(controller, srs,
            requestProperties, false, false, maintenanceStateHelper);

    for (ShortTaskStatus sts : rsr.getTasks()) {
      String role = sts.getRole();
      Assert.assertFalse(role.equals(componentName2_1));
      Assert.assertFalse(role.equals(componentName2_2));
    }

    for (Service service : cluster.getServices().values()) {
      if (service.getName().equals(serviceName2)) {
        Assert.assertEquals(State.STARTED, service.getDesiredState());
      } else {
        Assert.assertEquals(State.INSTALLED, service.getDesiredState());
      }
    }

    service2.setMaintenanceState(MaintenanceState.OFF);
    ServiceResourceProviderTest.updateServices(controller, srs, requestProperties,
            false, false, maintenanceStateHelper);
    for (Service service : cluster.getServices().values()) {
      Assert.assertEquals(State.INSTALLED, service.getDesiredState());
    }

    startService(clusterName, serviceName1, false, false, maintenanceStateHelper);
    startService(clusterName, serviceName2, false, false, maintenanceStateHelper);

    // test host
    Host h1 = clusters.getHost(host1);
    h1.setMaintenanceState(cluster.getClusterId(), MaintenanceState.ON);

    srs = new HashSet<ServiceRequest>();
    srs.add(new ServiceRequest(clusterName, serviceName1, State.INSTALLED.name()));
    srs.add(new ServiceRequest(clusterName, serviceName2, State.INSTALLED.name()));

    rsr = ServiceResourceProviderTest.updateServices(controller, srs, requestProperties,
            false, false, maintenanceStateHelper);

    for (ShortTaskStatus sts : rsr.getTasks()) {
      Assert.assertFalse(sts.getHostName().equals(host1));
    }

    h1.setMaintenanceState(cluster.getClusterId(), MaintenanceState.OFF);
    startService(clusterName, serviceName2, false, false, maintenanceStateHelper);

    service2.setMaintenanceState(MaintenanceState.ON);

    ServiceRequest sr = new ServiceRequest(clusterName, serviceName2, State.INSTALLED.name());
    rsr = ServiceResourceProviderTest.updateServices(controller,
        Collections.singleton(sr), requestProperties, false, false, maintenanceStateHelper);

    Assert.assertTrue("Service start request defaults to Cluster operation level," +
                    "command does not create tasks",
        rsr == null || rsr.getTasks().size() == 0);

  }

  @Test
  public void testIsAttributeMapsEqual() {
    AmbariManagementControllerImpl controllerImpl = null;
    if (controller instanceof AmbariManagementControllerImpl){
      controllerImpl = (AmbariManagementControllerImpl)controller;
    }
    Map<String, Map<String, String>> requestConfigAttributes = new HashMap<String, Map<String,String>>();
    Map<String, Map<String, String>> clusterConfigAttributes = new HashMap<String, Map<String,String>>();
    Assert.assertTrue(controllerImpl.isAttributeMapsEqual(requestConfigAttributes, clusterConfigAttributes));
    requestConfigAttributes.put("final", new HashMap<String, String>());
    requestConfigAttributes.get("final").put("c", "true");
    clusterConfigAttributes.put("final", new HashMap<String, String>());
    clusterConfigAttributes.get("final").put("c", "true");
    Assert.assertTrue(controllerImpl.isAttributeMapsEqual(requestConfigAttributes, clusterConfigAttributes));
    clusterConfigAttributes.put("final2", new HashMap<String, String>());
    clusterConfigAttributes.get("final2").put("a", "true");
    Assert.assertFalse(controllerImpl.isAttributeMapsEqual(requestConfigAttributes, clusterConfigAttributes));
    requestConfigAttributes.put("final2", new HashMap<String, String>());
    requestConfigAttributes.get("final2").put("a", "false");
    Assert.assertFalse(controllerImpl.isAttributeMapsEqual(requestConfigAttributes, clusterConfigAttributes));
  }

  @Test
  public void testEmptyConfigs() throws Exception {
    String clusterName = "c1";
    createCluster(clusterName);
    Cluster cluster =  clusters.getCluster(clusterName);
    cluster.setDesiredStackVersion(new StackId("HDP-0.1"));

    ClusterRequest cr = new ClusterRequest(cluster.getClusterId(), cluster.getClusterName(), null, null);

    // test null map with no prior
    cr.setDesiredConfig(Collections.singletonList(
        new ConfigurationRequest(clusterName, "typeA", "v1", null, null)));
    controller.updateClusters(Collections.singleton(cr), new HashMap<String, String>());
    Config config = cluster.getDesiredConfigByType("typeA");
    Assert.assertNull(config);

    // test empty map with no prior
    cr.setDesiredConfig(Collections.singletonList(
        new ConfigurationRequest(clusterName, "typeA", "v1", new HashMap<String, String>(), new HashMap<String, Map<String,String>>())));
    controller.updateClusters(Collections.singleton(cr), new HashMap<String, String>());
    config = cluster.getDesiredConfigByType("typeA");
    Assert.assertNotNull(config);

    // test empty properties on a new version
    cr.setDesiredConfig(Collections.singletonList(
        new ConfigurationRequest(clusterName, "typeA", "v2", new HashMap<String, String>(), new HashMap<String, Map<String,String>>())));
    controller.updateClusters(Collections.singleton(cr), new HashMap<String, String>());
    config = cluster.getDesiredConfigByType("typeA");
    Assert.assertNotNull(config);
    Assert.assertEquals(Integer.valueOf(0), Integer.valueOf(config.getProperties().size()));

    // test new version
    Map<String, String> map = new HashMap<String, String>();
    map.clear();
    map.put("c", "d");
    Map<String, Map<String, String>> attributesMap = new HashMap<String, Map<String,String>>();
    attributesMap.put("final", new HashMap<String, String>());
    attributesMap.get("final").put("c", "true");
    cr.setDesiredConfig(Collections.singletonList(
        new ConfigurationRequest(clusterName, "typeA", "v3", map, attributesMap)));
    controller.updateClusters(Collections.singleton(cr), new HashMap<String, String>());
    config = cluster.getDesiredConfigByType("typeA");
    Assert.assertNotNull(config);
    Assert.assertTrue(config.getProperties().containsKey("c"));

    // test reset to v2
    cr.setDesiredConfig(Collections.singletonList(
        new ConfigurationRequest(clusterName, "typeA", "v2", new HashMap<String, String>(), new HashMap<String, Map<String,String>>())));
    controller.updateClusters(Collections.singleton(cr), new HashMap<String, String>());
    config = cluster.getDesiredConfigByType("typeA");
    Assert.assertEquals("v2", config.getTag());
    Assert.assertNotNull(config);
    Assert.assertEquals(Integer.valueOf(0), Integer.valueOf(config.getProperties().size()));

    // test v2, but with properties
    cr.setDesiredConfig(Collections.singletonList(
        new ConfigurationRequest(clusterName, "typeA", "v2", new HashMap<String, String>() {{ put("a", "b"); }},
            new HashMap<String, Map<String,String>>(){{put("final", new HashMap<String, String>(){{put("a", "true");}});
          }
        })));
    try {
      controller.updateClusters(Collections.singleton(cr), new HashMap<String, String>());
      Assert.fail("Expect failure when creating a config that exists");
    } catch (AmbariException e) {
      // expected
    }
  }

  @Test
  public void testCreateCustomActionNoCluster() throws Exception {
    String hostname1 = "h1";
    String hostname2 = "h2";
    addHost(hostname1);
    addHost(hostname2);

    ambariMetaInfo.addActionDefinition(new ActionDefinition("a1", ActionType.SYSTEM,
        "", "", "", "action def description", TargetHostType.ANY,
        Short.valueOf("60")));

    Map<String, String> requestProperties = new HashMap<String, String>();
    requestProperties.put(REQUEST_CONTEXT_PROPERTY, "Called from a test");

    Map<String, String> requestParams = new HashMap<String, String>();
    requestParams.put("some_custom_param", "abc");

    // !!! target single host
    List<String> hosts = Arrays.asList(hostname1);
    RequestResourceFilter resourceFilter = new RequestResourceFilter(null, null, hosts);
    List<RequestResourceFilter> resourceFilters = new ArrayList<RequestResourceFilter>();
    resourceFilters.add(resourceFilter);

    ExecuteActionRequest actionRequest = new ExecuteActionRequest(null, null,
        "a1", resourceFilters, null, requestParams, false);
    RequestStatusResponse response = controller.createAction(actionRequest, requestProperties);
    assertEquals(1, response.getTasks().size());
    ShortTaskStatus taskStatus = response.getTasks().get(0);
    Assert.assertEquals(hostname1, taskStatus.getHostName());

    Stage stage = actionDB.getAllStages(response.getRequestId()).get(0);
    Assert.assertNotNull(stage);
    Assert.assertEquals(-1L, stage.getClusterId());

    List<HostRoleCommand> storedTasks = actionDB.getRequestTasks(response.getRequestId());
    Assert.assertEquals(1, storedTasks.size());
    HostRoleCommand task = storedTasks.get(0);
    Assert.assertEquals(RoleCommand.ACTIONEXECUTE, task.getRoleCommand());
    Assert.assertEquals("a1", task.getRole().name());
    Assert.assertEquals(hostname1, task.getHostName());

    ExecutionCommand cmd = task.getExecutionCommandWrapper().getExecutionCommand();
    Type type = new TypeToken<Map<String, String>>(){}.getType();
    Map<String, String> commandParamsStage = StageUtils.getGson().fromJson(stage.getCommandParamsStage(), type);
    Assert.assertTrue(commandParamsStage.containsKey("some_custom_param"));
    Assert.assertEquals(null, cmd.getServiceName());
    Assert.assertEquals(null, cmd.getComponentName());
    Assert.assertTrue(cmd.getLocalComponents().isEmpty());

    Assert.assertEquals(requestProperties.get(REQUEST_CONTEXT_PROPERTY), response.getRequestContext());

    // !!! target two hosts

    hosts = Arrays.asList(hostname1, hostname2);
    resourceFilter = new RequestResourceFilter(null, null, hosts);
    resourceFilters = new ArrayList<RequestResourceFilter>();
    resourceFilters.add(resourceFilter);

    actionRequest = new ExecuteActionRequest(null, null,
        "a1", resourceFilters, null, requestParams, false);
    response = controller.createAction(actionRequest, requestProperties);
    assertEquals(2, response.getTasks().size());
    boolean host1Found = false;
    boolean host2Found = false;
    for (ShortTaskStatus sts : response.getTasks()) {
      if (sts.getHostName().equals(hostname1)) {
        host1Found = true;
      } else if (sts.getHostName().equals(hostname2)) {
        host2Found = true;
      }
    }
    Assert.assertTrue(host1Found);
    Assert.assertTrue(host2Found);

    stage = actionDB.getAllStages(response.getRequestId()).get(0);
    Assert.assertNotNull(stage);
    Assert.assertEquals(-1L, stage.getClusterId());

    storedTasks = actionDB.getRequestTasks(response.getRequestId());
    Assert.assertEquals(2, storedTasks.size());
    task = storedTasks.get(0);
    Assert.assertEquals(RoleCommand.ACTIONEXECUTE, task.getRoleCommand());
    Assert.assertEquals("a1", task.getRole().name());
    Assert.assertEquals(hostname1, task.getHostName());

    cmd = task.getExecutionCommandWrapper().getExecutionCommand();
    commandParamsStage = StageUtils.getGson().fromJson(stage.getCommandParamsStage(), type);
    Assert.assertTrue(commandParamsStage.containsKey("some_custom_param"));
    Assert.assertEquals(null, cmd.getServiceName());
    Assert.assertEquals(null, cmd.getComponentName());
    Assert.assertTrue(cmd.getLocalComponents().isEmpty());

    Assert.assertEquals(requestProperties.get(REQUEST_CONTEXT_PROPERTY), response.getRequestContext());
  }

  @Test
  public void testConfigAttributesStaleConfigFilter() throws AmbariException {

    final String host1 = "h1";
    final String host2 = "h2";
    Long clusterId = 1L;
    String clusterName = "foo1";
    setupClusterWithHosts(clusterName, "HDP-2.0.5", new ArrayList<String>() {
      {
        add(host1);
        add(host2);
      }
    }, "centos5");
    String serviceName = "HDFS";
    createService(clusterName, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";

    createServiceComponent(clusterName, serviceName, componentName1, State.INIT);
    createServiceComponent(clusterName, serviceName, componentName2, State.INIT);
    createServiceComponent(clusterName, serviceName, componentName3, State.INIT);

    createServiceComponentHost(clusterName, serviceName, componentName1, host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName2, host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName3, host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName2, host2, null);
    createServiceComponentHost(clusterName, serviceName, componentName3, host2, null);

    // Install
    installService(clusterName, serviceName, false, false);

    // Create and attach config
    // hdfs-site will not have config-attributes
    Map<String, String> hdfsConfigs = new HashMap<String, String>();
    hdfsConfigs.put("a", "b");
    Map<String, Map<String, String>> hdfsConfigAttributes = new HashMap<String, Map<String, String>>() {
      {
        put("final", new HashMap<String, String>() {{put("a", "true");}});
      }
    };

    ConfigurationRequest cr1 = new ConfigurationRequest(clusterName, "hdfs-site", "version1", hdfsConfigs, hdfsConfigAttributes);
    ClusterRequest crReq1 = new ClusterRequest(clusterId, clusterName, null, null);
    crReq1.setDesiredConfig(Collections.singletonList(cr1));

    controller.updateClusters(Collections.singleton(crReq1), null);

    // Start
    startService(clusterName, serviceName, false, false);

    // Update actual config
    HashMap<String, Map<String, String>> actualConfig = new HashMap<String, Map<String, String>>() {
      {
        put("hdfs-site", new HashMap<String, String>() {{put("tag", "version1");}});
      }
    };
    HashMap<String, Map<String, String>> actualConfigOld = new HashMap<String, Map<String, String>>() {
      {
        put("hdfs-site", new HashMap<String, String>() {{put("tag", "version0");}});
      }
    };

    Service s1 = clusters.getCluster(clusterName).getService(serviceName);
    s1.getServiceComponent(componentName1).getServiceComponentHost(host1).updateActualConfigs(actualConfig);
    s1.getServiceComponent(componentName2).getServiceComponentHost(host1).updateActualConfigs(actualConfig);
    s1.getServiceComponent(componentName3).getServiceComponentHost(host1).updateActualConfigs(actualConfigOld);
    s1.getServiceComponent(componentName2).getServiceComponentHost(host2).updateActualConfigs(actualConfigOld);
    s1.getServiceComponent(componentName3).getServiceComponentHost(host2).updateActualConfigs(actualConfig);

    ServiceComponentHostRequest r = new ServiceComponentHostRequest(clusterName, null, null, null, null);
    Set<ServiceComponentHostResponse> resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(5, resps.size());

    // Get all host components with stale config = true
    r = new ServiceComponentHostRequest(clusterName, null, null, null, null);
    r.setStaleConfig("true");
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(2, resps.size());

    // Get all host components with stale config = false
    r = new ServiceComponentHostRequest(clusterName, null, null, null, null);
    r.setStaleConfig("false");
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(3, resps.size());

    // Get all host components with stale config = false and hostname filter
    r = new ServiceComponentHostRequest(clusterName, null, null, host1, null);
    r.setStaleConfig("false");
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(2, resps.size());

    // Get all host components with stale config = false and hostname filter
    r = new ServiceComponentHostRequest(clusterName, null, null, host2, null);
    r.setStaleConfig("true");
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(1, resps.size());
  }

  @Test
  public void testSecretReferences() throws AmbariException {

    final String host1 = "h1";
    final String host2 = "h2";
    Long clusterId = 1L;
    String clusterName = "foo1";
    Cluster cl = setupClusterWithHosts(clusterName, "HDP-2.0.5", new ArrayList<String>() {
      {
        add(host1);
        add(host2);
      }
    }, "centos5");
    String serviceName = "HDFS";
    createService(clusterName, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";

    createServiceComponent(clusterName, serviceName, componentName1, State.INIT);
    createServiceComponent(clusterName, serviceName, componentName2, State.INIT);
    createServiceComponent(clusterName, serviceName, componentName3, State.INIT);

    createServiceComponentHost(clusterName, serviceName, componentName1, host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName2, host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName3, host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName2, host2, null);
    createServiceComponentHost(clusterName, serviceName, componentName3, host2, null);

    // Install
    installService(clusterName, serviceName, false, false);

    ClusterRequest crReq;
    ConfigurationRequest cr;

    cr = new ConfigurationRequest(clusterName,
        "hdfs-site",
        "version1",
        new HashMap<String, String>(){{
          put("test.password", "first");
          put("test.password.empty", "");
        }},
        new HashMap<String, Map<String, String>>()
    );
    crReq = new ClusterRequest(clusterId, clusterName, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr));
    controller.updateClusters(Collections.singleton(crReq), null);
    // update config with secret reference
    cr = new ConfigurationRequest(clusterName,
        "hdfs-site",
        "version2",
        new HashMap<String, String>(){{
          put("test.password", "SECRET:hdfs-site:1:test.password");
          put("new", "new");//need this to mark config as "changed"
        }},
        new HashMap<String, Map<String, String>>()
    );
    crReq = new ClusterRequest(clusterId, clusterName, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr));
    controller.updateClusters(Collections.singleton(crReq), null);
    // change password to new value
    cr = new ConfigurationRequest(clusterName,
        "hdfs-site",
        "version3",
        new HashMap<String, String>(){{
          put("test.password", "brandNewPassword");
        }},
        new HashMap<String, Map<String, String>>()
    );
    crReq = new ClusterRequest(clusterId, clusterName, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr));
    controller.updateClusters(Collections.singleton(crReq), null);
    // wrong secret reference
    cr = new ConfigurationRequest(clusterName,
        "hdfs-site",
        "version3",
        new HashMap<String, String>(){{
          put("test.password", "SECRET:hdfs-site:666:test.password");
        }},
        new HashMap<String, Map<String, String>>()
    );
    crReq = new ClusterRequest(clusterId, clusterName, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr));
    try {
      controller.updateClusters(Collections.singleton(crReq), null);
      fail("Request need to be failed with wrong secret reference");
    } catch (AmbariException e){

    }
    // reference to config which does not contain requested property
    cr = new ConfigurationRequest(clusterName,
        "hdfs-site",
        "version4",
        new HashMap<String, String>(){{
          put("foo", "bar");
        }},
        new HashMap<String, Map<String, String>>()
    );
    crReq = new ClusterRequest(clusterId, clusterName, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr));
    controller.updateClusters(Collections.singleton(crReq), null);
    cr = new ConfigurationRequest(clusterName,
        "hdfs-site",
        "version5",
        new HashMap<String, String>(){{
          put("test.password", "SECRET:hdfs-site:4:test.password");
          put("new", "new");
        }},
        new HashMap<String, Map<String, String>>()
    );
    crReq = new ClusterRequest(clusterId, clusterName, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr));
    try {
      controller.updateClusters(Collections.singleton(crReq), null);
      fail("Request need to be failed with wrong secret reference");
    } catch (AmbariException e) {
      assertEquals("Error when parsing secret reference. Cluster: foo1 ConfigType: hdfs-site ConfigVersion: 4 does not contain property 'test.password'",
          e.getMessage());
    }
    cl.getAllConfigs();
    assertEquals(cl.getAllConfigs().size(), 4);

    Config v1 = cl.getConfigByVersion("hdfs-site", 1l);
    Config v2 = cl.getConfigByVersion("hdfs-site", 2l);
    Config v3 = cl.getConfigByVersion("hdfs-site", 3l);
    Config v4 = cl.getConfigByVersion("hdfs-site", 4l);

    assertEquals(v1.getProperties().get("test.password"), "first");
    assertEquals(v2.getProperties().get("test.password"), "first");
    assertEquals(v3.getProperties().get("test.password"), "brandNewPassword");
    assertFalse(v4.getProperties().containsKey("test.password"));

    // check if we have masked secret in responce
    final ConfigurationRequest configRequest = new ConfigurationRequest(clusterName, "hdfs-site", null, null, null);
    configRequest.setIncludeProperties(true);
    Set<ConfigurationResponse> requestedConfigs = controller.getConfigurations(new HashSet<ConfigurationRequest>() {{
      add(configRequest);
    }});
    for(ConfigurationResponse resp : requestedConfigs) {
      String secretName = "SECRET:hdfs-site:"+resp.getVersion().toString()+":test.password";
      if(resp.getConfigs().containsKey("test.password")) {
        assertEquals(resp.getConfigs().get("test.password"), secretName);
      }
      if(resp.getConfigs().containsKey("test.password.empty")) {
        assertEquals(resp.getConfigs().get("test.password.empty"), "");
      }
    }
  }

  @Test
  public void testTargetedProcessCommand() throws Exception {
    final String host1 = "h1";
    String clusterName = "c1";
    Cluster cluster = setupClusterWithHosts(clusterName, "HDP-2.0.5", Arrays.asList(host1), "centos5");
    String serviceName = "HDFS";
    createService(clusterName, serviceName, null);
    String componentName1 = "NAMENODE";

    createServiceComponent(clusterName, serviceName, componentName1, State.INIT);

    createServiceComponentHost(clusterName, serviceName, componentName1, host1, null);

    // Install
    installService(clusterName, serviceName, false, false);

    // Create and attach config
    // hdfs-site will not have config-attributes
    Map<String, String> hdfsConfigs = new HashMap<String, String>();
    hdfsConfigs.put("a", "b");
    Map<String, Map<String, String>> hdfsConfigAttributes = new HashMap<String, Map<String, String>>() {
      {
        put("final", new HashMap<String, String>() {{put("a", "true");}});
      }
    };

    ConfigurationRequest cr1 = new ConfigurationRequest(clusterName, "hdfs-site", "version1", hdfsConfigs, hdfsConfigAttributes);
    ClusterRequest crReq1 = new ClusterRequest(cluster.getClusterId(), clusterName, null, null);
    crReq1.setDesiredConfig(Collections.singletonList(cr1));

    controller.updateClusters(Collections.singleton(crReq1), null);

    // Start
    startService(clusterName, serviceName, false, false);

    ServiceComponentHostRequest req = new ServiceComponentHostRequest(clusterName, serviceName,
        componentName1, host1, "INSTALLED");

    Map<String, String> requestProperties = new HashMap<String, String>();
    requestProperties.put("namenode", "p1");
    RequestStatusResponse resp = updateHostComponents(Collections.singleton(req), requestProperties, false);

    // succeed in creating a task
    assertNotNull(resp);

    // manually change live state to stopped as no running action manager
    for (ServiceComponentHost sch :
      clusters.getCluster(clusterName).getServiceComponentHosts(host1)) {
        sch.setState(State.INSTALLED);
    }

    // no new commands since no targeted info
    resp = updateHostComponents(Collections.singleton(req), new HashMap<String, String>(), false);
    assertNull(resp);

    // role commands added for targeted command
    resp = updateHostComponents(Collections.singleton(req), requestProperties, false);
    assertNotNull(resp);

  }

  @Test
  public void testGetPackagesForServiceHost() throws Exception {
    ServiceInfo service = ambariMetaInfo.getStack("HDP", "2.0.1").getService("HIVE");
    HashMap<String, String> hostParams = new HashMap<String, String>();

    Map<String, ServiceOsSpecific.Package> packages = new HashMap<String, ServiceOsSpecific.Package>();
    String [] packageNames = {"hive", "mysql-connector-java", "mysql", "mysql-server", "mysql-client"};
    for (String packageName : packageNames) {
      ServiceOsSpecific.Package pkg = new ServiceOsSpecific.Package();
      pkg.setName(packageName);
      packages.put(packageName, pkg);
    }

    List<ServiceOsSpecific.Package> rhel5Packages = controller.getPackagesForServiceHost(service, hostParams, "redhat5");
    List<ServiceOsSpecific.Package> expectedRhel5 = Arrays.asList(
            packages.get("hive"),
            packages.get("mysql-connector-java"),
            packages.get("mysql"),
            packages.get("mysql-server")
    );

    List<ServiceOsSpecific.Package> sles11Packages = controller.getPackagesForServiceHost(service, hostParams, "suse11");
    List<ServiceOsSpecific.Package> expectedSles11 = Arrays.asList(
            packages.get("hive"),
            packages.get("mysql-connector-java"),
            packages.get("mysql"),
            packages.get("mysql-client")
    );
    assertThat(rhel5Packages, is(expectedRhel5));
    assertThat(sles11Packages, is(expectedSles11));
  }

  @Test
  public void testClusterWidgetCreateOnClusterCreate() throws Exception {
    // TODO: Add once cluster widgets.json is available
  }

  @Test
  public void testServiceWidgetCreationOnServiceCreate() throws Exception {
    String clusterName = "foo1";
    ClusterRequest r = new ClusterRequest(null, clusterName,
      State.INSTALLED.name(), SecurityType.NONE, "OTHER-2.0", null);
    controller.createCluster(r);
    String serviceName = "HBASE";
    clusters.getCluster("foo1").setDesiredStackVersion(new StackId("OTHER-2.0"));
    createService(clusterName, serviceName, State.INIT);

    Service s = clusters.getCluster(clusterName).getService(serviceName);
    Assert.assertNotNull(s);
    Assert.assertEquals(serviceName, s.getName());
    Assert.assertEquals(clusterName, s.getCluster().getClusterName());

    WidgetDAO widgetDAO = injector.getInstance(WidgetDAO.class);
    WidgetLayoutDAO widgetLayoutDAO = injector.getInstance(WidgetLayoutDAO.class);
    List<WidgetEntity> widgetEntities = widgetDAO.findAll();
    List<WidgetLayoutEntity> layoutEntities = widgetLayoutDAO.findAll();

    Assert.assertNotNull(widgetEntities);
    Assert.assertFalse(widgetEntities.isEmpty());
    Assert.assertNotNull(layoutEntities);
    Assert.assertFalse(layoutEntities.isEmpty());

    WidgetEntity candidateVisibleEntity = null;
    for (WidgetEntity entity : widgetEntities) {
      if (entity.getWidgetName().equals("OPEN_CONNECTIONS")) {
        candidateVisibleEntity = entity;
      }
    }
    Assert.assertNotNull(candidateVisibleEntity);
    Assert.assertEquals("GRAPH", candidateVisibleEntity.getWidgetType());
    Assert.assertEquals("ambari", candidateVisibleEntity.getAuthor());
    Assert.assertEquals("CLUSTER", candidateVisibleEntity.getScope());
    Assert.assertNotNull(candidateVisibleEntity.getMetrics());
    Assert.assertNotNull(candidateVisibleEntity.getProperties());
    Assert.assertNotNull(candidateVisibleEntity.getWidgetValues());

    WidgetLayoutEntity candidateLayoutEntity = null;
    for (WidgetLayoutEntity entity : layoutEntities) {
      if (entity.getLayoutName().equals("default_hbase_layout")) {
        candidateLayoutEntity = entity;
      }
    }
    Assert.assertNotNull(candidateLayoutEntity);
    List<WidgetLayoutUserWidgetEntity> layoutUserWidgetEntities =
      candidateLayoutEntity.getListWidgetLayoutUserWidgetEntity();
    Assert.assertNotNull(layoutUserWidgetEntities);
    Assert.assertEquals(4, layoutUserWidgetEntities.size());
    Assert.assertEquals("RS_READS_WRITES", layoutUserWidgetEntities.get(0).getWidget().getWidgetName());
    Assert.assertEquals("OPEN_CONNECTIONS", layoutUserWidgetEntities.get(1).getWidget().getWidgetName());
    Assert.assertEquals("FILES_LOCAL", layoutUserWidgetEntities.get(2).getWidget().getWidgetName());
    Assert.assertEquals("UPDATED_BLOCKED_TIME", layoutUserWidgetEntities.get(3).getWidget().getWidgetName());
    Assert.assertEquals("HBASE_SUMMARY", layoutUserWidgetEntities.get(0).getWidget().getDefaultSectionName());
  }

  // this is a temporary measure as a result of moving updateHostComponents from AmbariManagementController
  // to HostComponentResourceProvider.  Eventually the tests should be moved out of this class.
  private RequestStatusResponse updateHostComponents(Set<ServiceComponentHostRequest> requests,
                                                     Map<String, String> requestProperties,
                                                     boolean runSmokeTest) throws Exception {

    return updateHostComponents(controller, requests, requestProperties, runSmokeTest);
  }

  private RequestStatusResponse updateHostComponents(AmbariManagementController controller,
                                                     Set<ServiceComponentHostRequest> requests,
                                                     Map<String, String> requestProperties,
                                                     boolean runSmokeTest) throws Exception {

    return HostComponentResourceProviderTest.updateHostComponents(
        controller, injector, requests, requestProperties, runSmokeTest);
  }
}


