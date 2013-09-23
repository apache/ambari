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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import junit.framework.Assert;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ClusterNotFoundException;
import org.apache.ambari.server.DuplicateResourceException;
import org.apache.ambari.server.HostNotFoundException;
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.ParentObjectNotFoundException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.StackAccessException;
import org.apache.ambari.server.actionmanager.ActionDBAccessor;
import org.apache.ambari.server.actionmanager.ExecutionCommandWrapper;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.ExecutionCommandDAO;
import org.apache.ambari.server.orm.dao.RoleDAO;
import org.apache.ambari.server.orm.entities.ExecutionCommandEntity;
import org.apache.ambari.server.orm.entities.RoleEntity;
import org.apache.ambari.server.security.authorization.Users;
import org.apache.ambari.server.serveraction.ServerAction;
import org.apache.ambari.server.serveraction.ServerActionManager;
import org.apache.ambari.server.serveraction.ServerActionManagerImpl;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.ConfigImpl;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentFactory;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostInstallEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostOpSucceededEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStartEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStartedEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStopEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStoppedEvent;
import org.apache.ambari.server.utils.StageUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

public class AmbariManagementControllerTest {

  private static final Logger LOG =
      LoggerFactory.getLogger(AmbariManagementControllerTest.class);

  private static final String STACK_NAME = "HDP";

  private static final String STACK_VERSION = "0.2";
  private static final String OS_TYPE = "centos5";
  private static final String REPO_ID = "HDP-1.1.1.16";
  private static final String PROPERTY_NAME = "hbase.regionserver.msginterval";
  private static final String SERVICE_NAME = "HDFS";
  private static final int STACK_VERSIONS_CNT = 8;
  private static final int REPOS_CNT = 3;
  private static final int STACKS_CNT = 1;
  private static final int STACK_SERVICES_CNT = 5 ;
  private static final int STACK_PROPERTIES_CNT = 81;
  private static final int STACK_COMPONENTS_CNT = 3;
  private static final int OS_CNT = 2;

  private static final String NON_EXT_VALUE = "XXX";
  private static final String INCORRECT_BASE_URL = "http://incorrect.url";

  private static final String COMPONENT_NAME = "NAMENODE";
  
  private static final String REQUEST_CONTEXT_PROPERTY = "context";

  private static final int CONFIG_MAP_CNT = 21;

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

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
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
  }

  @After
  public void teardown() {
    injector.getInstance(PersistService.class).stop();
    actionDB = null;
  }

  private void createCluster(String clusterName) throws AmbariException {
    ClusterRequest r = new ClusterRequest(null, clusterName, "HDP-0.1", null);
    controller.createCluster(r);
  }

  private void createService(String clusterName,
      String serviceName, State desiredState) throws AmbariException {
    String dStateStr = null;
    if (desiredState != null) {
      dStateStr = desiredState.toString();
    }
    ServiceRequest r1 = new ServiceRequest(clusterName, serviceName, null,
        dStateStr);
    Set<ServiceRequest> requests = new HashSet<ServiceRequest>();
    requests.add(r1);
    controller.createServices(requests);
  }

  private void createServiceComponent(String clusterName,
      String serviceName, String componentName, State desiredState)
          throws AmbariException {
    String dStateStr = null;
    if (desiredState != null) {
      dStateStr = desiredState.toString();
    }
    ServiceComponentRequest r = new ServiceComponentRequest(clusterName,
        serviceName, componentName, null, dStateStr);
    Set<ServiceComponentRequest> requests =
        new HashSet<ServiceComponentRequest>();
    requests.add(r);
    controller.createComponents(requests);
  }

  private void createServiceComponentHost(String clusterName,
      String serviceName, String componentName, String hostname,
      State desiredState) throws AmbariException {
    String dStateStr = null;
    if (desiredState != null) {
      dStateStr = desiredState.toString();
    }
    ServiceComponentHostRequest r = new ServiceComponentHostRequest(clusterName,
        serviceName, componentName, hostname, null, dStateStr);
    Set<ServiceComponentHostRequest> requests =
        new HashSet<ServiceComponentHostRequest>();
    requests.add(r);
    controller.createHostComponents(requests);
  }

  private long stopService(String clusterName, String serviceName,
      boolean runSmokeTests, boolean reconfigureClients) throws
    AmbariException {
    ServiceRequest r = new ServiceRequest(clusterName, serviceName, null,
      State.INSTALLED.toString());
    Set<ServiceRequest> requests = new HashSet<ServiceRequest>();
    requests.add(r);
    Map<String, String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "Called from a test");
    RequestStatusResponse resp = controller.updateServices(requests,
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
      String serviceName) throws AmbariException {
    Cluster c = clusters.getCluster(clusterName);
    Service s = c.getService(serviceName);
    Set<ServiceComponentHostRequest> requests = new
      HashSet<ServiceComponentHostRequest>();
    for (ServiceComponent sc : s.getServiceComponents().values()) {
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        ServiceComponentHostRequest schr = new ServiceComponentHostRequest
          (clusterName, serviceName, sc.getName(),
            sch.getHostName(), null, State.INSTALLED.name());
        requests.add(schr);
      }
    }
    Map<String, String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "Called from a test");
    RequestStatusResponse resp = controller.updateHostComponents(requests,
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
    ServiceRequest r = new ServiceRequest(clusterName, serviceName, null,
        State.STARTED.toString());
    Set<ServiceRequest> requests = new HashSet<ServiceRequest>();
    requests.add(r);
    Map<String, String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "Called from a test");
    RequestStatusResponse resp = controller.updateServices(requests,
        mapRequestProps, runSmokeTests, reconfigureClients);

    Assert.assertEquals(State.STARTED,
        clusters.getCluster(clusterName).getService(serviceName)
            .getDesiredState());

    if (resp != null) {
      // manually change live state to stopped as no running action manager
      List<HostRoleCommand> commands = actionDB.getRequestTasks(resp.getRequestId());
      for (HostRoleCommand cmd : commands) {
        if (!cmd.getRole().toString().endsWith("CHECK")) {
          clusters.getCluster(clusterName).getService(serviceName).getServiceComponent(cmd.getRole().name())
              .getServiceComponentHost(cmd.getHostName()).setState(State.STARTED);
        }
      }
      return resp.getRequestId();
    } else {
      return -1;
    }
  }

  private long installService(String clusterName, String serviceName,
                              boolean runSmokeTests, boolean reconfigureClients) throws
      AmbariException {
    ServiceRequest r = new ServiceRequest(clusterName, serviceName, null,
        State.INSTALLED.toString());
    Set<ServiceRequest> requests = new HashSet<ServiceRequest>();
    requests.add(r);
    Map<String, String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "Called from a test");
    RequestStatusResponse resp = controller.updateServices(requests,
        mapRequestProps, runSmokeTests, reconfigureClients);

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
    clusters.getHost("h1").setOsType("redhat6");
    clusters.getHost("h2").setOsType("redhat6");
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

    r.setClusterId(new Long(1));
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

    ServiceRequest req = new ServiceRequest(clusterName, serviceName,
        null, null);

    Set<ServiceResponse> r =
        controller.getServices(Collections.singleton(req));
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
      ServiceRequest rInvalid = new ServiceRequest(null, null, null, null);
      set1.add(rInvalid);
      controller.createServices(set1);
      fail("Expected failure for invalid requests");
    } catch (Exception e) {
      // Expected
    }

    try {
      set1.clear();
      ServiceRequest rInvalid = new ServiceRequest("foo", null, null, null);
      set1.add(rInvalid);
      controller.createServices(set1);
      fail("Expected failure for invalid requests");
    } catch (Exception e) {
      // Expected
    }

    try {
      set1.clear();
      ServiceRequest rInvalid = new ServiceRequest("foo", "bar", null, null);
      set1.add(rInvalid);
      controller.createServices(set1);
      fail("Expected failure for invalid cluster");
    } catch (ParentObjectNotFoundException e) {
      // Expected
    }

    clusters.addCluster("foo");
    clusters.addCluster("bar");
    clusters.getCluster("foo").setDesiredStackVersion(new StackId("HDP-0.1"));
    clusters.getCluster("bar").setDesiredStackVersion(new StackId("HDP-0.1"));

    try {
      set1.clear();
      ServiceRequest valid1 = new ServiceRequest("foo", "HDFS", null, null);
      ServiceRequest valid2 = new ServiceRequest("foo", "HDFS", null, null);
      set1.add(valid1);
      set1.add(valid2);
      controller.createServices(set1);
      fail("Expected failure for invalid requests");
    } catch (Exception e) {
      // Expected
    }

    try {
      set1.clear();
      ServiceRequest valid1 = new ServiceRequest("foo", "bar", null, null);
      set1.add(valid1);
      controller.createServices(set1);
      fail("Expected failure for invalid service");
    } catch (Exception e) {
      // Expected
    }


    try {
      set1.clear();
      ServiceRequest valid1 = new ServiceRequest("foo", "HDFS", null, null);
      ServiceRequest valid2 = new ServiceRequest("bar", "HDFS", null, null);
      set1.add(valid1);
      set1.add(valid2);
      controller.createServices(set1);
      fail("Expected failure for multiple clusters");
    } catch (Exception e) {
      // Expected
    }

    Assert.assertNotNull(clusters.getCluster("foo"));
    Assert.assertEquals(0, clusters.getCluster("foo").getServices().size());

    set1.clear();
    ServiceRequest valid = new ServiceRequest("foo", "HDFS", null, null);
    set1.add(valid);
    controller.createServices(set1);

    try {
      set1.clear();
      ServiceRequest valid1 = new ServiceRequest("foo", "HDFS", null, null);
      ServiceRequest valid2 = new ServiceRequest("foo", "HDFS", null, null);
      set1.add(valid1);
      set1.add(valid2);
      controller.createServices(set1);
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

    ServiceRequest r = new ServiceRequest(clusterName, null, null, null);
    Set<ServiceResponse> response = controller.getServices(Collections.singleton(r));
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
    clusters.addCluster("foo");
    clusters.getCluster("foo").setDesiredStackVersion(new StackId("HDP-0.1"));

    ServiceRequest valid1 = new ServiceRequest("foo", "HDFS", null, null);
    ServiceRequest valid2 = new ServiceRequest("foo", "MAPREDUCE", null, null);
    set1.add(valid1);
    set1.add(valid2);
    controller.createServices(set1);

    try {
      valid1 = new ServiceRequest("foo", "PIG", null, null);
      valid2 = new ServiceRequest("foo", "MAPREDUCE", null, null);
      set1.add(valid1);
      set1.add(valid2);
      controller.createServices(set1);
      fail("Expected failure for invalid services");
    } catch (DuplicateResourceException e) {
      // Expected
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
        new ServiceComponentRequest(clusterName, serviceName, null, null, null);
    Set<ServiceComponentResponse> response = controller.getComponents(Collections.singleton(r));
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
          new ServiceComponentRequest(null, null, null, null, null);
      set1.add(rInvalid);
      controller.createComponents(set1);
      fail("Expected failure for invalid requests");
    } catch (Exception e) {
      // Expected
    }

    try {
      set1.clear();
      ServiceComponentRequest rInvalid =
          new ServiceComponentRequest("c1", null, null, null, null);
      set1.add(rInvalid);
      controller.createComponents(set1);
      fail("Expected failure for invalid requests");
    } catch (Exception e) {
      // Expected
    }

    try {
      set1.clear();
      ServiceComponentRequest rInvalid =
          new ServiceComponentRequest("c1", "s1", null, null, null);
      set1.add(rInvalid);
      controller.createComponents(set1);
      fail("Expected failure for invalid requests");
    } catch (Exception e) {
      // Expected
    }

    try {
      set1.clear();
      ServiceComponentRequest rInvalid =
          new ServiceComponentRequest("c1", "s1", "sc1", null, null);
      set1.add(rInvalid);
      controller.createComponents(set1);
      fail("Expected failure for invalid cluster");
    } catch (ParentObjectNotFoundException e) {
      // Expected
    }

    clusters.addCluster("c1");
    clusters.addCluster("c2");


    try {
      set1.clear();
      ServiceComponentRequest rInvalid =
          new ServiceComponentRequest("c1", "HDFS", "NAMENODE", null, null);
      set1.add(rInvalid);
      controller.createComponents(set1);
      fail("Expected failure for invalid service");
    } catch (ParentObjectNotFoundException e) {
      // Expected
    }

    Cluster c1 = clusters.getCluster("c1");
    c1.setDesiredStackVersion(new StackId("HDP-0.1"));
    Service s1 = serviceFactory.createNew(c1, "HDFS");
    Service s2 = serviceFactory.createNew(c1, "MAPREDUCE");
    c1.addService(s1);
    c1.addService(s2);
    s1.persist();
    s2.persist();

    set1.clear();
    ServiceComponentRequest valid1 =
        new ServiceComponentRequest("c1", "HDFS", "NAMENODE", null, null);
    ServiceComponentRequest valid2 =
        new ServiceComponentRequest("c1", "MAPREDUCE", "JOBTRACKER", null, null);
    ServiceComponentRequest valid3 =
        new ServiceComponentRequest("c1", "MAPREDUCE", "TASKTRACKER", null,
            null);
    set1.add(valid1);
    set1.add(valid2);
    set1.add(valid3);
    controller.createComponents(set1);

    try {
      set1.clear();
      ServiceComponentRequest rInvalid1 =
          new ServiceComponentRequest("c1", "HDFS", "HDFS_CLIENT", null, null);
      ServiceComponentRequest rInvalid2 =
          new ServiceComponentRequest("c1", "HDFS", "HDFS_CLIENT", null, null);
      set1.add(rInvalid1);
      set1.add(rInvalid2);
      controller.createComponents(set1);
      fail("Expected failure for dups in requests");
    } catch (Exception e) {
      // Expected
    }

    try {
      set1.clear();
      ServiceComponentRequest rInvalid1 =
          new ServiceComponentRequest("c1", "HDFS", "HDFS_CLIENT", null, null);
      ServiceComponentRequest rInvalid2 =
          new ServiceComponentRequest("c2", "HDFS", "HDFS_CLIENT", null, null);
      set1.add(rInvalid1);
      set1.add(rInvalid2);
      controller.createComponents(set1);
      fail("Expected failure for multiple clusters");
    } catch (Exception e) {
      // Expected
    }

    try {
      set1.clear();
      ServiceComponentRequest rInvalid =
          new ServiceComponentRequest("c1", "HDFS", "NAMENODE", null, null);
      set1.add(rInvalid);
      controller.createComponents(set1);
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
  public void testCreateServiceComponentWithConfigs() {
    // FIXME after config impl
  }

  @Test
  public void testCreateServiceComponentMultiple() throws AmbariException {
    clusters.addCluster("c1");
    clusters.addCluster("c2");

    Cluster c1 = clusters.getCluster("c1");
    c1.setDesiredStackVersion(new StackId("HDP-0.2"));
    Service s1 = serviceFactory.createNew(c1, "HDFS");
    Service s2 = serviceFactory.createNew(c1, "MAPREDUCE");
    c1.addService(s1);
    c1.addService(s2);
    s1.persist();
    s2.persist();

    Set<ServiceComponentRequest> set1 = new HashSet<ServiceComponentRequest>();
    ServiceComponentRequest valid1 =
        new ServiceComponentRequest("c1", "HDFS", "NAMENODE", null, null);
    ServiceComponentRequest valid2 =
        new ServiceComponentRequest("c1", "MAPREDUCE", "JOBTRACKER", null, null);
    ServiceComponentRequest valid3 =
        new ServiceComponentRequest("c1", "MAPREDUCE", "TASKTRACKER", null,
            null);
    set1.add(valid1);
    set1.add(valid2);
    set1.add(valid3);
    controller.createComponents(set1);

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
    clusters.addHost(host1);
    clusters.getHost("h1").setOsType("centos5");
    clusters.getHost("h1").setState(HostState.HEALTHY);
    clusters.getHost("h1").persist();
    String host2 = "h2";
    clusters.addHost(host2);
    clusters.getHost("h2").setOsType("centos5");
    clusters.getHost("h2").setState(HostState.HEALTHY);
    clusters.getHost("h2").persist();

    clusters.getHost("h1").setOsType("centos5");
    clusters.getHost("h2").setOsType("centos6");

    try {
      createServiceComponentHost(clusterName, serviceName, componentName1,
          host1, State.INIT);
      fail("ServiceComponentHost creation should fail for invalid host"
          + " as host not mapped to cluster");
    } catch (Exception e) {
      // Expected
    }

    clusters.mapHostToCluster(host1, clusterName);
    clusters.mapHostToCluster(host2, clusterName);

    try {
      createServiceComponentHost(clusterName, serviceName, componentName1,
          host1, State.INSTALLING);
      fail("ServiceComponentHost creation should fail for invalid state");
    } catch (Exception e) {
      // Expected
      e.printStackTrace();
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
            componentName2, null, null, null);

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
    clusters.addHost(host1);
    clusters.getHost("h1").setOsType("centos5");
    clusters.getHost("h1").persist();
    String host2 = "h2";
    clusters.addHost(host2);
    clusters.getHost("h2").setOsType("centos5");
    clusters.getHost("h2").persist();
    clusters.mapHostToCluster(host1, clusterName);
    clusters.mapHostToCluster(host2, clusterName);

    Set<ServiceComponentHostRequest> set1 =
        new HashSet<ServiceComponentHostRequest>();
    ServiceComponentHostRequest r1 =
        new ServiceComponentHostRequest(clusterName, serviceName,
            componentName1, host1, null, State.INIT.toString());
    ServiceComponentHostRequest r2 =
        new ServiceComponentHostRequest(clusterName, serviceName,
            componentName2, host1, null, State.INIT.toString());
    ServiceComponentHostRequest r3 =
        new ServiceComponentHostRequest(clusterName, serviceName,
            componentName1, host2, null, State.INIT.toString());
    ServiceComponentHostRequest r4 =
        new ServiceComponentHostRequest(clusterName, serviceName,
            componentName2, host2, null, State.INIT.toString());

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
          new ServiceComponentHostRequest(null, null, null, null, null, null);
      set1.add(rInvalid);
      controller.createHostComponents(set1);
      fail("Expected failure for invalid requests");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    try {
      set1.clear();
      ServiceComponentHostRequest rInvalid =
          new ServiceComponentHostRequest("foo", null, null, null, null, null);
      set1.add(rInvalid);
      controller.createHostComponents(set1);
      fail("Expected failure for invalid requests");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    try {
      set1.clear();
      ServiceComponentHostRequest rInvalid =
          new ServiceComponentHostRequest("foo", "HDFS", null, null,
              null, null);
      set1.add(rInvalid);
      controller.createHostComponents(set1);
      fail("Expected failure for invalid requests");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    try {
      set1.clear();
      ServiceComponentHostRequest rInvalid =
          new ServiceComponentHostRequest("foo", "HDFS", "NAMENODE", null,
              null, null);
      set1.add(rInvalid);
      controller.createHostComponents(set1);
      fail("Expected failure for invalid requests");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    try {
      set1.clear();
      ServiceComponentHostRequest rInvalid =
          new ServiceComponentHostRequest("foo", "HDFS", "NAMENODE", "h1",
              null, null);
      set1.add(rInvalid);
      controller.createHostComponents(set1);
      fail("Expected failure for invalid cluster");
    } catch (ParentObjectNotFoundException e) {
      // Expected
    }

    clusters.addCluster("foo");
    clusters.addCluster("c1");
    clusters.addCluster("c2");
    Cluster foo = clusters.getCluster("foo");
    Cluster c1 = clusters.getCluster("c1");
    Cluster c2 = clusters.getCluster("c2");
    foo.setDesiredStackVersion(new StackId("HDP-0.2"));
    c1.setDesiredStackVersion(new StackId("HDP-0.2"));
    c2.setDesiredStackVersion(new StackId("HDP-0.2"));

    try {
      set1.clear();
      ServiceComponentHostRequest rInvalid =
          new ServiceComponentHostRequest("foo", "HDFS", "NAMENODE", "h1",
              null, null);
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
          new ServiceComponentHostRequest("foo", "HDFS", "NAMENODE", "h1",
              null, null);
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
          new ServiceComponentHostRequest("foo", "HDFS", "NAMENODE", "h1",
              null, null);
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
    h1.setOsType("centos6");
    h1.persist();
    clusters.addHost("h2");
    Host h2 = clusters.getHost("h2");
    h2.setIPv4("ipv42");
    h2.setIPv6("ipv62");
    h2.setOsType("centos6");
    h2.persist();
    clusters.addHost("h3");
    Host h3 = clusters.getHost("h3");
    h3.setIPv4("ipv43");
    h3.setIPv6("ipv63");
    h3.setOsType("centos6");
    h3.persist();

    try {
      set1.clear();
      ServiceComponentHostRequest rInvalid =
          new ServiceComponentHostRequest("foo", "HDFS", "NAMENODE", "h1",
              null, null);
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
        new ServiceComponentHostRequest("foo", "HDFS", "NAMENODE", "h1",
            null, null);
    set1.add(valid);
    controller.createHostComponents(set1);

    try {
      set1.clear();
      ServiceComponentHostRequest rInvalid1 =
          new ServiceComponentHostRequest("foo", "HDFS", "NAMENODE", "h2",
              null, null);
      ServiceComponentHostRequest rInvalid2 =
          new ServiceComponentHostRequest("foo", "HDFS", "NAMENODE", "h2",
              null, null);
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
              null, null);
      ServiceComponentHostRequest rInvalid2 =
          new ServiceComponentHostRequest("c2", "HDFS", "NAMENODE", "h3",
              null, null);
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
              null, null);
      ServiceComponentHostRequest rInvalid2 =
          new ServiceComponentHostRequest("foo", "HDFS", "NAMENODE", "h2",
              null, null);
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
            null, null);
    set1.add(valid1);
    controller.createHostComponents(set1);

    set1.clear();
    ServiceComponentHostRequest valid2 =
        new ServiceComponentHostRequest("c2", "HDFS", "NAMENODE", "h1",
            null, null);
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
      controller.createHosts(requests);
      fail("Create host should fail for non-bootstrapped host");
    } catch (Exception e) {
      // Expected
    }

    clusters.addHost("h1");
    clusters.addHost("h2");
    clusters.getHost("h1").setOsType("centos5");
    clusters.getHost("h2").setOsType("centos5");
    clusters.getHost("h1").persist();
    clusters.getHost("h2").persist();

    requests.add(new HostRequest("h2", "foo", new HashMap<String, String>()));

    try {
      controller.createHosts(requests);
      fail("Create host should fail for invalid clusters");
    } catch (Exception e) {
      // Expected
    }

    clusters.addCluster("foo");
    clusters.getCluster("foo").setDesiredStackVersion(new StackId("HDP-0.1"));

    controller.createHosts(requests);

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
    clusters.addCluster("c1");
    clusters.getCluster("c1").setDesiredStackVersion(new StackId("HDP-0.1"));
    clusters.getHost("h1").setOsType("centos5");
    clusters.getHost("h2").setOsType("centos5");
    clusters.getHost("h3").setOsType("centos5");
    clusters.getHost("h1").persist();
    clusters.getHost("h2").persist();
    clusters.getHost("h3").persist();

    Map<String, String> hostAttrs =
        new HashMap<String, String>();
    hostAttrs.put("attr1", "val1");
    hostAttrs.put("attr2", "val2");

    String clusterName = "c1";

    HostRequest r1 = new HostRequest("h1", clusterName, null);
    HostRequest r2 = new HostRequest("h2", clusterName, hostAttrs);
    HostRequest r3 = new HostRequest("h3", null, hostAttrs);

    Set<HostRequest> set1 = new HashSet<HostRequest>();
    set1.add(r1);
    set1.add(r2);
    set1.add(r3);
    controller.createHosts(set1);

    Assert.assertEquals(1, clusters.getClustersForHost("h1").size());
    Assert.assertEquals(1, clusters.getClustersForHost("h2").size());
    Assert.assertEquals(0, clusters.getClustersForHost("h3").size());

    Assert.assertEquals(2, clusters.getHost("h2").getHostAttributes().size());
    Assert.assertEquals(2, clusters.getHost("h3").getHostAttributes().size());
    Assert.assertEquals("val1",
        clusters.getHost("h2").getHostAttributes().get("attr1"));
    Assert.assertEquals("val2",
        clusters.getHost("h2").getHostAttributes().get("attr2"));
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
      controller.createHosts(set1);
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
      controller.createHosts(set1);
      fail("Expected failure for invalid cluster");
    } catch (Exception e) {
      // Expected
    }

    clusters.addCluster("c1");

    try {
      set1.clear();
      HostRequest rInvalid1 =
          new HostRequest("h1", clusterName, null);
      HostRequest rInvalid2 =
          new HostRequest("h1", clusterName, null);
      set1.add(rInvalid1);
      set1.add(rInvalid2);
      controller.createHosts(set1);
      fail("Expected failure for dup requests");
    } catch (Exception e) {
      // Expected
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
    properties.put("a", "a1");
    properties.put("b", "b1");

    Config c1 = new ConfigImpl(cluster, "hdfs-site", properties, injector);
    properties.put("c", "c1");
    properties.put("d", "d1");
    Config c2 = new ConfigImpl(cluster, "core-site", properties, injector);
    Config c3 = new ConfigImpl(cluster, "foo-site", properties, injector);

    Map<String, String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "Called from a test");

    c1.setVersionTag("v1");
    c2.setVersionTag("v1");
    c3.setVersionTag("v1");

    cluster.addConfig(c1);
    cluster.addConfig(c2);
    cluster.addConfig(c3);
    c1.persist();
    c2.persist();
    c3.persist();

    configs.put(c1.getType(), c1);
    configs.put(c2.getType(), c2);
    s1.updateDesiredConfigs(configs);
    s1.persist();

    ServiceRequest r = new ServiceRequest(clusterName, serviceName, null,
        State.INSTALLED.toString());
    Set<ServiceRequest> requests = new HashSet<ServiceRequest>();
    requests.add(r);

    RequestStatusResponse trackAction =
        controller.updateServices(requests, mapRequestProps, true, false);
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

          LOG.info("Dumping host action details"
              + ", stageId=" + stage.getStageId()
              + ", actionId=" + stage.getActionId()
              + ", commandDetails="
              + StageUtils.jaxbToString(ecw.getExecutionCommand()));
        }
      }
    }


    RequestStatusRequest statusRequest =
        new RequestStatusRequest(trackAction.getRequestId(), null);
    Set<RequestStatusResponse> statusResponses =
        controller.getRequestStatus(statusRequest);
    Assert.assertEquals(1, statusResponses.size());
    RequestStatusResponse statusResponse =
        statusResponses.iterator().next();
    Assert.assertNotNull(statusResponse);
    Assert.assertEquals(trackAction.getRequestId(),
        statusResponse.getRequestId());

    Set<TaskStatusRequest> taskRequests = new HashSet<TaskStatusRequest>();
    TaskStatusRequest t1, t2;
    t1 = new TaskStatusRequest();
    t2 = new TaskStatusRequest();
    t1.setRequestId(trackAction.getRequestId());
    taskRequests.add(t1);
    Set<TaskStatusResponse> taskResponses =
        controller.getTaskStatus(taskRequests);
    Assert.assertEquals(5, taskResponses.size());

    t1.setTaskId(1L);
    t2.setRequestId(trackAction.getRequestId());
    t2.setTaskId(2L);
    taskRequests.clear();
    taskRequests.add(t1);
    taskRequests.add(t2);
    taskResponses = controller.getTaskStatus(taskRequests);
    Assert.assertEquals(2, taskResponses.size());

    // manually change live state to installed as no running action manager
    for (ServiceComponent sc :
      clusters.getCluster(clusterName).getService(serviceName)
      .getServiceComponents().values()) {
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        sch.setState(State.INSTALLED);
      }
    }

    r = new ServiceRequest(clusterName, serviceName, null,
        State.STARTED.toString());
    requests.clear();
    requests.add(r);
    trackAction = controller.updateServices(requests, mapRequestProps, true,
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

    for (Stage stage : stages) {
      LOG.info("Stage Details for Start Service"
          + ", stageId="+ stage.getStageId()
          + ", actionId=" + stage.getActionId());

      for (String host : stage.getHosts()) {
        LOG.info("Dumping host action details"
            + ", stageId=" + stage.getStageId()
            + ", actionId=" + stage.getActionId()
            + ", commandDetails="
            + StageUtils.jaxbToString(stage.getExecutionCommands(host).get(0)));
      }
    }

    StringBuilder sb = new StringBuilder();
    clusters.debugDump(sb);
    LOG.info("Cluster Dump: " + sb.toString());

    statusRequest = new RequestStatusRequest(null, null);
    statusResponses = controller.getRequestStatus(statusRequest);
    Assert.assertEquals(2, statusResponses.size());

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

    r = new ServiceRequest(clusterName, serviceName, null,
        State.INSTALLED.toString());
    requests.clear();
    requests.add(r);
    trackAction = controller.updateServices(requests, mapRequestProps, true,
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

    for (Stage stage : stages) {
      LOG.info("Stage Details for Stop Service : " + stage.toString());
    }
    Assert.assertEquals(1, stages.size());

  }

  @Test
  public void testGetClusters() throws AmbariException {
    clusters.addCluster("c1");

    Cluster c1 = clusters.getCluster("c1");

    c1.setDesiredStackVersion(new StackId("HDP-0.1"));

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
    clusters.addCluster("c1");
    clusters.addCluster("c2");
    clusters.addCluster("c3");
    clusters.addCluster("c4");

    Cluster c1 = clusters.getCluster("c1");
    Cluster c2 = clusters.getCluster("c2");
    Cluster c3 = clusters.getCluster("c3");
    Cluster c4 = clusters.getCluster("c4");

    c1.setDesiredStackVersion(new StackId("HDP-0.1"));
    c2.setDesiredStackVersion(new StackId("HDP-0.1"));
    c3.setDesiredStackVersion(new StackId("HDP-1.1.0"));

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
    Assert.assertEquals(2, resp.size());

    r = new ClusterRequest(null, null, "", null);
    resp = controller.getClusters(Collections.singleton(r));
    Assert.assertEquals(1, resp.size());
    Assert.assertEquals(c4.getClusterId(),
        resp.iterator().next().getClusterId().longValue());
  }

  @Test
  public void testGetServices() throws AmbariException {
    clusters.addCluster("c1");
    Cluster c1 = clusters.getCluster("c1");
    c1.setDesiredStackVersion(new StackId("HDP-0.1"));
    Service s1 = serviceFactory.createNew(c1, "HDFS");

    c1.addService(s1);
    s1.setDesiredStackVersion(new StackId("HDP-0.1"));
    s1.setDesiredState(State.INSTALLED);

    s1.persist();

    ServiceRequest r = new ServiceRequest("c1", null, null, null);
    Set<ServiceResponse> resp = controller.getServices(Collections.singleton(r));

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
    clusters.addCluster("c1");
    clusters.addCluster("c2");
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

    ServiceRequest r = new ServiceRequest(null, null, null, null);
    Set<ServiceResponse> resp;

    try {
      controller.getServices(Collections.singleton(r));
      fail("Expected failure for invalid request");
    } catch (Exception e) {
      // Expected
    }

    r = new ServiceRequest(c1.getClusterName(), null, null, null);
    resp = controller.getServices(Collections.singleton(r));
    Assert.assertEquals(3, resp.size());

    r = new ServiceRequest(c1.getClusterName(), s2.getName(), null, null);
    resp = controller.getServices(Collections.singleton(r));
    Assert.assertEquals(1, resp.size());
    Assert.assertEquals(s2.getName(), resp.iterator().next().getServiceName());

    try {
      r = new ServiceRequest(c2.getClusterName(), s1.getName(), null, null);
      controller.getServices(Collections.singleton(r));
      fail("Expected failure for invalid service");
    } catch (Exception e) {
      // Expected
    }

    r = new ServiceRequest(c1.getClusterName(), null, null, "INSTALLED");
    resp = controller.getServices(Collections.singleton(r));
    Assert.assertEquals(2, resp.size());

    r = new ServiceRequest(c2.getClusterName(), null, null, "INIT");
    resp = controller.getServices(Collections.singleton(r));
    Assert.assertEquals(1, resp.size());

    ServiceRequest r1, r2, r3;
    r1 = new ServiceRequest(c1.getClusterName(), null, null, "INSTALLED");
    r2 = new ServiceRequest(c2.getClusterName(), null, null, "INIT");
    r3 = new ServiceRequest(c2.getClusterName(), null, null, "INIT");

    Set<ServiceRequest> reqs = new HashSet<ServiceRequest>();
    reqs.addAll(Arrays.asList(r1, r2, r3));
    resp = controller.getServices(reqs);
    Assert.assertEquals(3, resp.size());

  }


  @Test
  public void testGetServiceComponents() throws AmbariException {
    clusters.addCluster("c1");
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
       s1.getName(), sc1.getName(), null, null);

    Set<ServiceComponentResponse> resps = controller.getComponents(Collections.singleton(r));
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
    clusters.addCluster("c1");
    clusters.addCluster("c2");
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
        null, null, null);

    try {
      controller.getComponents(Collections.singleton(r));
      fail("Expected failure for invalid cluster");
    } catch (Exception e) {
      // Expected
    }

    // all comps per cluster
    r = new ServiceComponentRequest(c1.getClusterName(),
        null, null, null, null);
    Set<ServiceComponentResponse> resps = controller.getComponents(Collections.singleton(r));
    Assert.assertEquals(3, resps.size());

    // all comps per cluster filter on state
    r = new ServiceComponentRequest(c2.getClusterName(),
        null, null, null, State.UNINSTALLED.toString());
    resps = controller.getComponents(Collections.singleton(r));
    Assert.assertEquals(4, resps.size());

    // all comps for given service
    r = new ServiceComponentRequest(c2.getClusterName(),
        s5.getName(), null, null, null);
    resps = controller.getComponents(Collections.singleton(r));
    Assert.assertEquals(2, resps.size());

    // all comps for given service filter by state
    r = new ServiceComponentRequest(c2.getClusterName(),
        s4.getName(), null, null, State.INIT.toString());
    resps = controller.getComponents(Collections.singleton(r));
    Assert.assertEquals(1, resps.size());
    Assert.assertEquals(sc4.getName(),
        resps.iterator().next().getComponentName());

    // get single given comp
    r = new ServiceComponentRequest(c2.getClusterName(),
        null, sc5.getName(), null, State.INIT.toString());
    resps = controller.getComponents(Collections.singleton(r));
    Assert.assertEquals(1, resps.size());
    Assert.assertEquals(sc5.getName(),
        resps.iterator().next().getComponentName());

    // get single given comp and given svc
    r = new ServiceComponentRequest(c2.getClusterName(),
        s4.getName(), sc5.getName(), null, State.INIT.toString());
    resps = controller.getComponents(Collections.singleton(r));
    Assert.assertEquals(1, resps.size());
    Assert.assertEquals(sc5.getName(),
        resps.iterator().next().getComponentName());


    ServiceComponentRequest r1, r2, r3;
    Set<ServiceComponentRequest> reqs = new HashSet<ServiceComponentRequest>();
    r1 = new ServiceComponentRequest(c2.getClusterName(),
        null, null, null, State.UNINSTALLED.toString());
    r2 = new ServiceComponentRequest(c1.getClusterName(),
        null, null, null, null);
    r3 = new ServiceComponentRequest(c1.getClusterName(),
        null, null, null, State.INIT.toString());
    reqs.addAll(Arrays.asList(r1, r2, r3));
    resps = controller.getComponents(reqs);
    Assert.assertEquals(7, resps.size());
  }

  @Test
  public void testGetServiceComponentHosts() throws AmbariException {
    clusters.addCluster("c1");
    Cluster c1 = clusters.getCluster("c1");
    c1.setDesiredStackVersion(new StackId("HDP-0.1"));
    clusters.addHost("h1");
    clusters.getHost("h1").setOsType("centos5");
    clusters.getHost("h1").persist();
    clusters.mapHostToCluster("h1", "c1");
    Service s1 = serviceFactory.createNew(c1, "HDFS");
    c1.addService(s1);
    s1.persist();
    ServiceComponent sc1 = serviceComponentFactory.createNew(s1, "DATANODE");
    s1.addServiceComponent(sc1);
    sc1.setDesiredState(State.UNINSTALLED);
    sc1.persist();
    ServiceComponentHost sch1 = serviceComponentHostFactory.createNew(sc1, "h1",
        false);
    sc1.addServiceComponentHost(sch1);
    sch1.setDesiredState(State.INSTALLED);
    sch1.setState(State.INSTALLING);
    sch1.setDesiredStackVersion(new StackId("HDP-1.1.0"));
    sch1.setStackVersion(new StackId("HDP-0.1"));

    sch1.persist();
    
    sch1.updateActualConfigs(new HashMap<String, Map<String,String>>() {{
      put("global", new HashMap<String,String>() {{ put("tag", "version1"); }});
    }});
    

    ServiceComponentHostRequest r =
        new ServiceComponentHostRequest(c1.getClusterName(),
            null, null, null, null, null);
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
  public void testGetServiceComponentHostsWithFilters() throws AmbariException {
    clusters.addCluster("c1");
    Cluster c1 = clusters.getCluster("c1");
    c1.setDesiredStackVersion(new StackId("HDP-0.2"));

    clusters.addHost("h1");
    clusters.addHost("h2");
    clusters.addHost("h3");
    clusters.getHost("h1").setOsType("centos5");
    clusters.getHost("h2").setOsType("centos5");
    clusters.getHost("h3").setOsType("centos5");
    clusters.getHost("h1").persist();
    clusters.getHost("h2").persist();
    clusters.getHost("h3").persist();
    clusters.mapHostToCluster("h1", "c1");
    clusters.mapHostToCluster("h2", "c1");
    clusters.mapHostToCluster("h3", "c1");

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

    ServiceComponentHost sch1 = serviceComponentHostFactory.createNew(sc1, "h1",
        false);
    ServiceComponentHost sch2 = serviceComponentHostFactory.createNew(sc1, "h2",
        false);
    ServiceComponentHost sch3 = serviceComponentHostFactory.createNew(sc1, "h3",
        false);
    ServiceComponentHost sch4 = serviceComponentHostFactory.createNew(sc2, "h1",
        false);
    ServiceComponentHost sch5 = serviceComponentHostFactory.createNew(sc2, "h2",
        false);
    ServiceComponentHost sch6 = serviceComponentHostFactory.createNew(sc3, "h3",
        false);

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
        new ServiceComponentHostRequest(null, null, null, null, null, null);

    try {
      controller.getHostComponents(Collections.singleton(r));
      fail("Expected failure for invalid cluster");
    } catch (Exception e) {
      // Expected
    }

    // all across cluster
    r = new ServiceComponentHostRequest(c1.getClusterName(), null,
        null, null, null, null);
    Set<ServiceComponentHostResponse> resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(6, resps.size());

    // all for service
    r = new ServiceComponentHostRequest(c1.getClusterName(), s1.getName(),
        null, null, null, null);
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(5, resps.size());

    // all for component
    r = new ServiceComponentHostRequest(c1.getClusterName(), null,
        sc3.getName(), null, null, null);
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(1, resps.size());

    // all for host
    r = new ServiceComponentHostRequest(c1.getClusterName(), null,
        null, "h2", null, null);
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(2, resps.size());

    // all across cluster with state filter
    r = new ServiceComponentHostRequest(c1.getClusterName(), null,
        null, null, null, State.UNINSTALLED.toString());
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(1, resps.size());

    // all for service with state filter
    r = new ServiceComponentHostRequest(c1.getClusterName(), s1.getName(),
        null, null, null, State.INIT.toString());
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(2, resps.size());

    // all for component with state filter
    r = new ServiceComponentHostRequest(c1.getClusterName(), null,
        sc3.getName(), null, null, State.INSTALLED.toString());
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(0, resps.size());

    // all for host with state filter
    r = new ServiceComponentHostRequest(c1.getClusterName(), null,
        null, "h2", null, State.INIT.toString());
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(1, resps.size());

    // for service and host
    r = new ServiceComponentHostRequest(c1.getClusterName(), s3.getName(),
        null, "h1", null, null);
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(0, resps.size());

    // single sch - given service and host and component
    r = new ServiceComponentHostRequest(c1.getClusterName(), s3.getName(),
        sc3.getName(), "h3", null, State.INSTALLED.toString());
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(0, resps.size());

    // single sch - given service and host and component
    r = new ServiceComponentHostRequest(c1.getClusterName(), s3.getName(),
        sc3.getName(), "h3", null, null);
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(1, resps.size());

    ServiceComponentHostRequest r1, r2, r3;
    r1 = new ServiceComponentHostRequest(c1.getClusterName(), null,
        null, "h3", null, null);
    r2 = new ServiceComponentHostRequest(c1.getClusterName(), s3.getName(),
        sc3.getName(), "h2", null, null);
    r3 = new ServiceComponentHostRequest(c1.getClusterName(), null,
        null, "h2", null, null);
    Set<ServiceComponentHostRequest> reqs =
        new HashSet<ServiceComponentHostRequest>();
    reqs.addAll(Arrays.asList(r1, r2, r3));
    resps = controller.getHostComponents(reqs);
    Assert.assertEquals(4, resps.size());
  }

  @Test
  public void testGetHosts() throws AmbariException {
    clusters.addCluster("c1");
    clusters.addCluster("c2");
    clusters.getCluster("c1").setDesiredStackVersion(new StackId("HDP-0.2"));
    clusters.getCluster("c2").setDesiredStackVersion(new StackId("HDP-0.2"));
    clusters.addHost("h1");
    clusters.addHost("h2");
    clusters.addHost("h3");
    clusters.addHost("h4");
    clusters.getHost("h1").setOsType("centos5");
    clusters.getHost("h2").setOsType("centos5");
    clusters.getHost("h3").setOsType("centos5");
    clusters.getHost("h4").setOsType("centos5");
    clusters.getHost("h1").persist();
    clusters.getHost("h2").persist();
    clusters.getHost("h3").persist();
    clusters.getHost("h4").persist();
    clusters.mapHostToCluster("h1", "c1");
    clusters.mapHostToCluster("h2", "c1");
    clusters.mapHostToCluster("h3", "c2");

    Map<String, String> attrs = new HashMap<String, String>();
    attrs.put("a1", "b1");
    clusters.getHost("h3").setHostAttributes(attrs);
    attrs.put("a2", "b2");
    clusters.getHost("h4").setHostAttributes(attrs);

    HostRequest r = new HostRequest(null, null, null);

    Set<HostResponse> resps = controller.getHosts(Collections.singleton(r));

    Assert.assertEquals(4, resps.size());

    Set<String> foundHosts = new HashSet<String>();

    for (HostResponse resp : resps) {
      foundHosts.add(resp.getHostname());
      if (resp.getHostname().equals("h1")) {
        Assert.assertEquals("c1", resp.getClusterName());
        Assert.assertEquals(0, resp.getHostAttributes().size());
      } else if (resp.getHostname().equals("h2")) {
        Assert.assertEquals("c1", resp.getClusterName());
        Assert.assertEquals(0, resp.getHostAttributes().size());
      } else if (resp.getHostname().equals("h3")) {
        Assert.assertEquals("c2", resp.getClusterName());
        Assert.assertEquals(1, resp.getHostAttributes().size());
      } else if (resp.getHostname().equals("h4")) {
        //todo: why wouldn't this be null?
        Assert.assertEquals("", resp.getClusterName());
        Assert.assertEquals(2, resp.getHostAttributes().size());
      } else {
        fail("Found invalid host");
      }
    }

    Assert.assertEquals(4, foundHosts.size());

    r = new HostRequest("h1", null, null);
    resps = controller.getHosts(Collections.singleton(r));
    Assert.assertEquals(1, resps.size());
    HostResponse resp = resps.iterator().next();
    Assert.assertEquals("h1", resp.getHostname());
    Assert.assertEquals("c1", resp.getClusterName());
    Assert.assertEquals(0, resp.getHostAttributes().size());

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
          null, State.INSTALLING.toString());
      reqs.clear();
      reqs.add(r);
      controller.updateServices(reqs, mapRequestProps, true, false);
      fail("Expected fail for invalid state transition");
    } catch (Exception e) {
      // Expected
    }

    r = new ServiceRequest(clusterName, serviceName,
        null, State.INSTALLED.toString());
    reqs.clear();
    reqs.add(r);
    RequestStatusResponse trackAction = controller.updateServices(reqs,
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
      req1 = new ServiceRequest(clusterName1, serviceName1, null,
          State.INSTALLED.toString());
      req2 = new ServiceRequest(clusterName2, serviceName2, null,
          State.INSTALLED.toString());
      reqs.add(req1);
      reqs.add(req2);
      controller.updateServices(reqs, mapRequestProps, true, false);
      fail("Expected failure for multi cluster update");
    } catch (Exception e) {
      // Expected
    }

    try {
      reqs.clear();
      req1 = new ServiceRequest(clusterName1, serviceName1, null,
          State.INSTALLED.toString());
      req2 = new ServiceRequest(clusterName1, serviceName1, null,
          State.INSTALLED.toString());
      reqs.add(req1);
      reqs.add(req2);
      controller.updateServices(reqs, mapRequestProps, true, false);
      fail("Expected failure for dups services");
    } catch (Exception e) {
      // Expected
    }

    clusters.getCluster(clusterName1).getService(serviceName2)
        .setDesiredState(State.INSTALLED);

    try {
      reqs.clear();
      req1 = new ServiceRequest(clusterName1, serviceName1, null,
          State.INSTALLED.toString());
      req2 = new ServiceRequest(clusterName1, serviceName2, null,
          State.STARTED.toString());
      reqs.add(req1);
      reqs.add(req2);
      controller.updateServices(reqs, mapRequestProps, true, false);
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
    clusters.addHost(host1);
    clusters.getHost("h1").setOsType("centos5");
    clusters.getHost("h1").setState(HostState.HEALTHY);
    clusters.getHost("h1").persist();
    String host2 = "h2";
    clusters.addHost(host2);
    clusters.getHost("h2").setOsType("centos5");
    clusters.getHost("h2").setState(HostState.HEALTHY);
    clusters.getHost("h2").persist();
    clusters.mapHostToCluster(host1, clusterName);
    clusters.mapHostToCluster(host2, clusterName);

    Set<ServiceComponentHostRequest> set1 =
        new HashSet<ServiceComponentHostRequest>();
    ServiceComponentHostRequest r1 =
        new ServiceComponentHostRequest(clusterName, serviceName1,
            componentName1, host1, null, State.INIT.toString());
    ServiceComponentHostRequest r2 =
        new ServiceComponentHostRequest(clusterName, serviceName1,
            componentName2, host1, null, State.INIT.toString());
    ServiceComponentHostRequest r3 =
        new ServiceComponentHostRequest(clusterName, serviceName1,
            componentName1, host2, null, State.INIT.toString());
    ServiceComponentHostRequest r4 =
        new ServiceComponentHostRequest(clusterName, serviceName1,
            componentName2, host2, null, State.INIT.toString());
    ServiceComponentHostRequest r5 =
        new ServiceComponentHostRequest(clusterName, serviceName2,
            componentName3, host1, null, State.INIT.toString());
    ServiceComponentHostRequest r6 =
        new ServiceComponentHostRequest(clusterName, serviceName1,
            componentName4, host2, null, State.INIT.toString());

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
      req1 = new ServiceRequest(clusterName, serviceName1, null,
          State.STARTED.toString());
      reqs.add(req1);
      controller.updateServices(reqs, mapRequestProps, true, false);
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
      req1 = new ServiceRequest(clusterName, serviceName1, null,
          State.STARTED.toString());
      reqs.add(req1);
      controller.updateServices(reqs, mapRequestProps, true, false);
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
    req1 = new ServiceRequest(clusterName, serviceName1, null,
        State.STARTED.toString());
    req2 = new ServiceRequest(clusterName, serviceName2, null,
        State.STARTED.toString());
    reqs.add(req1);
    reqs.add(req2);
    RequestStatusResponse trackAction = controller.updateServices(reqs,
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
      if (s.getStageId() == 1) { stage1 = s; }
      if (s.getStageId() == 2) { stage2 = s; }
      if (s.getStageId() == 3) { stage3 = s; }
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
    
    for (Stage s : stages) {
      for (List<ExecutionCommandWrapper> list : s.getExecutionCommands().values()) {
        for (ExecutionCommandWrapper ecw : list) {
          if (ecw.getExecutionCommand().getRole().name().contains("SERVICE_CHECK")) {
            Map<String, String> hostParams = ecw.getExecutionCommand().getHostLevelParams();
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
    req1 = new ServiceRequest(clusterName, serviceName1, null,
        State.STARTED.toString());
    req2 = new ServiceRequest(clusterName, serviceName2, null,
        State.STARTED.toString());
    reqs.add(req1);
    reqs.add(req2);
    trackAction = controller.updateServices(reqs, mapRequestProps, true,
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
    clusters.addHost(host1);
    clusters.getHost("h1").setOsType("centos5");
    clusters.getHost("h1").setState(HostState.HEALTHY);
    clusters.getHost("h1").persist();
    String host2 = "h2";
    clusters.addHost(host2);
    clusters.getHost("h2").setOsType("centos5");
    clusters.getHost("h2").setState(HostState.HEALTHY);
    clusters.getHost("h2").persist();
    clusters.mapHostToCluster(host1, clusterName);
    clusters.mapHostToCluster(host2, clusterName);

    Set<ServiceComponentHostRequest> set1 =
        new HashSet<ServiceComponentHostRequest>();
    ServiceComponentHostRequest r1 =
        new ServiceComponentHostRequest(clusterName, serviceName1,
            componentName1, host1, null, State.INIT.toString());
    ServiceComponentHostRequest r2 =
        new ServiceComponentHostRequest(clusterName, serviceName1,
            componentName2, host1, null, State.INIT.toString());
    ServiceComponentHostRequest r3 =
        new ServiceComponentHostRequest(clusterName, serviceName1,
            componentName1, host2, null, State.INIT.toString());
    ServiceComponentHostRequest r4 =
        new ServiceComponentHostRequest(clusterName, serviceName1,
            componentName2, host2, null, State.INIT.toString());
    ServiceComponentHostRequest r5 =
        new ServiceComponentHostRequest(clusterName, serviceName1,
            componentName3, host1, null, State.INIT.toString());

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
        sc3.getName(), null, State.INSTALLED.toString());
    reqs.add(req1);
    controller.updateComponents(reqs, Collections.<String, String>emptyMap(), true);
    try {
      reqs.clear();
      req1 = new ServiceComponentRequest(clusterName, serviceName1,
          sc1.getName(), null, State.INIT.toString());
      reqs.add(req1);
      controller.updateComponents(reqs, Collections.<String, String>emptyMap(), true);
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
          sc1.getName(), null, State.STARTED.toString());
      reqs.add(req1);
      controller.updateComponents(reqs, Collections.<String, String>emptyMap(), true);
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
        sc1.getName(), null, State.INSTALLED.toString());
    req2 = new ServiceComponentRequest(clusterName, serviceName1,
        sc2.getName(), null, State.INSTALLED.toString());
    req3 = new ServiceComponentRequest(clusterName, serviceName1,
        sc3.getName(), null, State.INSTALLED.toString());
    reqs.add(req1);
    reqs.add(req2);
    reqs.add(req3);
    RequestStatusResponse trackAction = controller.updateComponents(reqs, Collections.<String, String>emptyMap(), true);

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
        sc1.getName(), null, State.INSTALLED.toString());
    req2 = new ServiceComponentRequest(clusterName, serviceName1,
        sc2.getName(), null, State.INSTALLED.toString());
    reqs.add(req1);
    reqs.add(req2);
    trackAction = controller.updateComponents(reqs, Collections.<String, String>emptyMap(), true);
    Assert.assertNull(trackAction);
  }

  @Test
  public void testServiceComponentHostUpdateRecursive() throws AmbariException {
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
    clusters.addHost(host1);
    clusters.getHost("h1").setOsType("centos5");
    clusters.getHost("h1").setState(HostState.HEALTHY);
    clusters.getHost("h1").persist();
    String host2 = "h2";
    clusters.addHost(host2);
    clusters.getHost("h2").setOsType("centos5");
    clusters.getHost("h2").setState(HostState.HEALTHY);
    clusters.getHost("h2").persist();
    clusters.mapHostToCluster(host1, clusterName);
    clusters.mapHostToCluster(host2, clusterName);


    Set<ServiceComponentHostRequest> set1 =
        new HashSet<ServiceComponentHostRequest>();
    ServiceComponentHostRequest r1 =
        new ServiceComponentHostRequest(clusterName, serviceName1,
            componentName1, host1, null, State.INIT.toString());
    ServiceComponentHostRequest r2 =
        new ServiceComponentHostRequest(clusterName, serviceName1,
            componentName2, host1, null, State.INIT.toString());
    ServiceComponentHostRequest r3 =
        new ServiceComponentHostRequest(clusterName, serviceName1,
            componentName1, host2, null, State.INIT.toString());
    ServiceComponentHostRequest r4 =
        new ServiceComponentHostRequest(clusterName, serviceName1,
            componentName2, host2, null, State.INIT.toString());
    ServiceComponentHostRequest r5 =
        new ServiceComponentHostRequest(clusterName, serviceName1,
            componentName3, host1, null, State.INIT.toString());

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

    try {
      reqs.clear();
      req1 = new ServiceComponentHostRequest(clusterName, serviceName1,
          componentName1, host1,
          null, State.STARTED.toString());
      reqs.add(req1);
      controller.updateHostComponents(reqs, Collections.<String, String>emptyMap(), true);
      fail("Expected failure for invalid transition");
    } catch (Exception e) {
      // Expected
    }

    try {
      reqs.clear();
      req1 = new ServiceComponentHostRequest(clusterName, serviceName1,
          componentName1, host1,
          null, State.INSTALLED.toString());
      req2 = new ServiceComponentHostRequest(clusterName, serviceName1,
          componentName1, host2,
          null, State.INSTALLED.toString());
      req3 = new ServiceComponentHostRequest(clusterName, serviceName1,
          componentName2, host1,
          null, State.INSTALLED.toString());
      req4 = new ServiceComponentHostRequest(clusterName, serviceName1,
          componentName2, host2,
          null, State.INSTALLED.toString());
      req5 = new ServiceComponentHostRequest(clusterName, serviceName1,
          componentName3, host1,
          null, State.STARTED.toString());
      reqs.add(req1);
      reqs.add(req2);
      reqs.add(req3);
      reqs.add(req4);
      reqs.add(req5);
      controller.updateHostComponents(reqs, Collections.<String, String>emptyMap(), true);
      fail("Expected failure for invalid states");
    } catch (Exception e) {
      // Expected
    }

    reqs.clear();
    req1 = new ServiceComponentHostRequest(clusterName, null,
        componentName1, host1,
        null, State.INSTALLED.toString());
    req2 = new ServiceComponentHostRequest(clusterName, serviceName1,
        componentName1, host2,
        null, State.INSTALLED.toString());
    req3 = new ServiceComponentHostRequest(clusterName, null,
        componentName2, host1,
        null, State.INSTALLED.toString());
    req4 = new ServiceComponentHostRequest(clusterName, serviceName1,
        componentName2, host2,
        null, State.INSTALLED.toString());
    req5 = new ServiceComponentHostRequest(clusterName, serviceName1,
        componentName3, host1,
        null, State.INSTALLED.toString());
    reqs.add(req1);
    reqs.add(req2);
    reqs.add(req3);
    reqs.add(req4);
    reqs.add(req5);
    RequestStatusResponse trackAction = controller.updateHostComponents(reqs,
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
        null, State.INSTALLED.toString());
    req2 = new ServiceComponentHostRequest(clusterName, serviceName1,
        componentName1, host2,
        null, State.INSTALLED.toString());
    reqs.add(req1);
    reqs.add(req2);
    trackAction = controller.updateHostComponents(reqs, Collections.<String,
        String>emptyMap(), true);
    Assert.assertNull(trackAction);
  }

  @Test
  public void testServiceComponentHostUpdateStackId() throws AmbariException {
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
    clusters.addHost(host1);
    clusters.getHost("h1").setOsType("centos5");
    clusters.getHost("h1").setState(HostState.HEALTHY);
    clusters.getHost("h1").persist();
    String host2 = "h2";
    clusters.addHost(host2);
    clusters.getHost("h2").setOsType("centos5");
    clusters.getHost("h2").setState(HostState.HEALTHY);
    clusters.getHost("h2").persist();
    clusters.mapHostToCluster(host1, clusterName);
    clusters.mapHostToCluster(host2, clusterName);

    Set<ServiceComponentHostRequest> set1 =
        new HashSet<ServiceComponentHostRequest>();
    ServiceComponentHostRequest r1 =
        new ServiceComponentHostRequest(clusterName, serviceName1,
            componentName1, host1, null, State.INIT.toString());
    ServiceComponentHostRequest r2 =
        new ServiceComponentHostRequest(clusterName, serviceName1,
            componentName1, host2, null, State.INIT.toString());
    ServiceComponentHostRequest r3 =
        new ServiceComponentHostRequest(clusterName, serviceName1,
            componentName2, host1, null, State.INIT.toString());

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
        null, State.INSTALLED.toString());
    req1.setDesiredStackId("HDP-0.2");
    reqs.add(req1);
    req2 = new ServiceComponentHostRequest(clusterName, serviceName1,
        componentName1, host2,
        null, State.INSTALLED.toString());
    req2.setDesiredStackId("HDP-0.2");
    reqs.add(req2);

    Map<String,String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "testServiceComponentHostUpdateStackId");
    RequestStatusResponse resp = controller.updateHostComponents(reqs, mapRequestProps, true);
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
        null, State.INSTALLED.toString());
    req1.setDesiredStackId("HDP-0.2");
    reqs.add(req1);
    req2 = new ServiceComponentHostRequest(clusterName, serviceName1,
        componentName1, host2,
        null, State.INSTALLED.toString());
    req2.setDesiredStackId("HDP-0.2");
    reqs.add(req2);
    req3 = new ServiceComponentHostRequest(clusterName, serviceName1,
        componentName2, host1,
        null, State.INSTALLED.toString());
    req3.setDesiredStackId("HDP-0.2");
    reqs.add(req3);

    resp = controller.updateHostComponents(reqs, Collections.<String, String>emptyMap(), true);
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

  @Test
  public void testServiceComponentHostUpdateStackIdError() throws AmbariException {
    String clusterName = "foo1";
    createCluster(clusterName);
    String serviceName1 = "HDFS";
    createService(clusterName, serviceName1, null);
    String componentName1 = "NAMENODE";
    createServiceComponent(clusterName, serviceName1, componentName1,
        State.INIT);
    String host1 = "h1";
    clusters.addHost(host1);
    clusters.getHost("h1").setOsType("centos5");
    clusters.getHost("h1").persist();
    String host2 = "h2";
    clusters.addHost(host2);
    clusters.getHost("h2").setOsType("centos5");
    clusters.getHost("h2").persist();
    clusters.mapHostToCluster(host1, clusterName);
    clusters.mapHostToCluster(host2, clusterName);

    Set<ServiceComponentHostRequest> set1 =
        new HashSet<ServiceComponentHostRequest>();
    ServiceComponentHostRequest r1 =
        new ServiceComponentHostRequest(clusterName, serviceName1,
            componentName1, host1, null, State.INIT.toString());
    ServiceComponentHostRequest r2 =
        new ServiceComponentHostRequest(clusterName, serviceName1,
            componentName1, host2, null, State.INIT.toString());

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
        componentName1, host1,
        null, State.STARTED.toString());
    req1.setDesiredStackId("invalid stack id");
    reqs.add(req1);
    updateHostAndCompareExpectedFailure(reqs, "Invalid desired stack id");

    c1.setCurrentStackVersion(null);
    sch1.setStackVersion(new StackId("HDP-0.1"));
    reqs.clear();
    req1 = new ServiceComponentHostRequest(clusterName, serviceName1,
        componentName1, host1,
        null, State.STARTED.toString());
    req1.setDesiredStackId("HDP-0.2");
    reqs.add(req1);
    updateHostAndCompareExpectedFailure(reqs, "Cluster has not been upgraded yet");

    c1.setCurrentStackVersion(new StackId("HDP2-0.1"));
    reqs.clear();
    req1 = new ServiceComponentHostRequest(clusterName, serviceName1,
        componentName1, host1,
        null, State.STARTED.toString());
    req1.setDesiredStackId("HDP-0.2");
    reqs.add(req1);
    updateHostAndCompareExpectedFailure(reqs, "Deployed stack name and requested stack names");

    c1.setCurrentStackVersion(new StackId("HDP-0.2"));
    reqs.clear();
    req1 = new ServiceComponentHostRequest(clusterName, serviceName1,
        componentName1, host1,
        null, State.STARTED.toString());
    req1.setDesiredStackId("HDP-0.3");
    reqs.add(req1);
    updateHostAndCompareExpectedFailure(reqs, "Component host can only be upgraded to the same version");

    c1.setCurrentStackVersion(new StackId("HDP-0.2"));
    sch1.setState(State.STARTED);
    reqs.clear();
    req1 = new ServiceComponentHostRequest(clusterName, serviceName1,
        componentName1, host1,
        null, State.STARTED.toString());
    req1.setDesiredStackId("HDP-0.2");
    reqs.add(req1);
    updateHostAndCompareExpectedFailure(reqs, "Component host is in an invalid state for upgrade");

    c1.setCurrentStackVersion(new StackId("HDP-0.2"));
    sch1.setState(State.UPGRADING);
    reqs.clear();
    req1 = new ServiceComponentHostRequest(clusterName, serviceName1,
        componentName1, host1,
        new HashMap<String, String>(), State.STARTED.toString());
    req1.setDesiredStackId("HDP-0.2");
    reqs.add(req1);
    updateHostAndCompareExpectedFailure(reqs, "Upgrade cannot be accompanied with config");

    c1.setCurrentStackVersion(new StackId("HDP-0.2"));
    sch1.setState(State.UPGRADING);
    reqs.clear();
    req1 = new ServiceComponentHostRequest(clusterName, serviceName1,
        componentName1, host1,
        null, State.STARTED.toString());
    req1.setDesiredStackId("HDP-0.2");
    reqs.add(req1);
    updateHostAndCompareExpectedFailure(reqs, "The desired state for an upgrade request must be");

    c1.setCurrentStackVersion(new StackId("HDP-0.2"));
    sch1.setState(State.UPGRADING);
    reqs.clear();
    req1 = new ServiceComponentHostRequest(clusterName, serviceName1,
        componentName1, host1, null, null);
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
        null, State.INSTALLED.toString());
    req1.setDesiredStackId("HDP-0.2");
    reqs.add(req1);
    req2 = new ServiceComponentHostRequest(clusterName, serviceName1,
        componentName1, host2,
        null, State.STARTED.toString());
    reqs.add(req2);
    updateHostAndCompareExpectedFailure(reqs, "An upgrade request cannot be combined with other");

    c1.setCurrentStackVersion(new StackId("HDP-0.2"));
    sch1.setState(State.INSTALLED);
    sch1.setStackVersion(new StackId("HDP-0.2"));
    reqs.clear();
    req1 = new ServiceComponentHostRequest(clusterName, serviceName1,
        componentName1, host1,
        null, null);
    req1.setDesiredStackId("HDP-0.2");
    reqs.add(req1);

    RequestStatusResponse resp = controller.updateHostComponents(reqs,
        Collections.<String,String>emptyMap(), true);
    Assert.assertNull(resp);

    c1.setCurrentStackVersion(new StackId("HDP-0.2"));
    sch1.setState(State.INSTALLED);
    sch1.setStackVersion(new StackId("HDP-0.2"));
    reqs.clear();
    req1 = new ServiceComponentHostRequest(clusterName, serviceName1,
        componentName1, host1,
        null, State.INSTALLED.toString());
    req1.setDesiredStackId("HDP-0.2");
    reqs.add(req1);
    resp = controller.updateHostComponents(reqs, Collections.<String,String>emptyMap(), true);
    Assert.assertNull(resp);
  }

  private void updateHostAndCompareExpectedFailure(Set<ServiceComponentHostRequest> reqs,
                                                   String expectedMessage) {
    try {
      controller.updateHostComponents(reqs, Collections.<String,String>emptyMap(), true);
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

  @SuppressWarnings("serial")
  @Test
  public void testGetRequestAndTaskStatus() throws AmbariException {
    long requestId = 3;
    long stageId = 4;
    String clusterName = "c1";
    final String hostName1 = "h1";
    final String hostName2 = "h2";
    String context = "Test invocation";


    clusters.addCluster(clusterName);
    clusters.getCluster(clusterName).setDesiredStackVersion(
        new StackId("HDP-0.1"));
    clusters.addHost(hostName1);
    clusters.getHost("h1").setOsType("centos5");
    clusters.getHost(hostName1).persist();
    clusters.addHost(hostName2);
    clusters.getHost("h2").setOsType("centos5");
    clusters.getHost(hostName2).persist();
    clusters.mapHostsToCluster(new HashSet<String>(){
      {add(hostName1); add(hostName2);}}, clusterName);


    List<Stage> stages = new ArrayList<Stage>();
    stages.add(new Stage(requestId, "/a1", clusterName, context));
    stages.get(0).setStageId(stageId++);
    stages.get(0).addHostRoleExecutionCommand(hostName1, Role.HBASE_MASTER,
        RoleCommand.START,
        new ServiceComponentHostStartEvent(Role.HBASE_MASTER.toString(),
            hostName1, System.currentTimeMillis(),
            new HashMap<String, String>()),
            clusterName, "HBASE");
    stages.add(new Stage(requestId, "/a2", clusterName, context));
    stages.get(1).setStageId(stageId);
    stages.get(1).addHostRoleExecutionCommand(hostName1, Role.HBASE_CLIENT,
        RoleCommand.START,
        new ServiceComponentHostStartEvent(Role.HBASE_CLIENT.toString(),
            hostName1, System.currentTimeMillis(),
            new HashMap<String, String>()), clusterName, "HBASE");

    actionDB.persistActions(stages);

    Set<RequestStatusResponse> requestStatusResponses =
        controller.getRequestStatus(new RequestStatusRequest(requestId, null));

    RequestStatusResponse requestStatusResponse =
        requestStatusResponses.iterator().next();
    assertEquals(requestId, requestStatusResponse.getRequestId());
    assertEquals(context, requestStatusResponse.getRequestContext());

    Set<TaskStatusRequest> taskStatusRequests = new HashSet<TaskStatusRequest>();
    taskStatusRequests.add(new TaskStatusRequest(requestId, null));
    Set<TaskStatusResponse> taskStatusResponses =
        controller.getTaskStatus(taskStatusRequests);

    assertEquals(2, taskStatusResponses.size());
  }


  @SuppressWarnings("serial")
  @Test
  public void testGetActions() throws Exception {
    Set<ActionResponse> responses = controller.getActions(
        new HashSet<ActionRequest>() {{
          add(new ActionRequest(null, "HDFS", null, null));
    }});

    assertFalse(responses.isEmpty());
    assertEquals(1, responses.size());
    ActionResponse response = responses.iterator().next();
    assertEquals(Role.HDFS_SERVICE_CHECK.name(), response.getActionName());
  }

  @SuppressWarnings("serial")
  @Test
  public void testCreateActions() throws Exception {
    clusters.addCluster("c1");
    clusters.getCluster("c1").setDesiredStackVersion(new StackId("HDP-0.1"));
    clusters.addHost("h1");
    clusters.getHost("h1").setOsType("centos5");
    clusters.getHost("h1").persist();
    clusters.addHost("h2");
    clusters.getHost("h2").setOsType("centos5");
    clusters.getHost("h2").persist();
    Set<String> hostNames = new HashSet<String>(){{
      add("h1");
      add("h2");
    }};

    clusters.mapHostsToCluster(hostNames, "c1");

    Cluster cluster = clusters.getCluster("c1");
    cluster.setDesiredStackVersion(new StackId("HDP-0.1"));
    
    ConfigFactory cf = injector.getInstance(ConfigFactory.class);
    Config config1 = cf.createNew(cluster, "global",
        new HashMap<String, String>(){{ put("key1", "value1"); }});
    config1.setVersionTag("version1");

    Config config2 = cf.createNew(cluster, "core-site",
        new HashMap<String, String>(){{ put("key1", "value1"); }});
    config2.setVersionTag("version1");
    
    cluster.addConfig(config1);
    cluster.addConfig(config2);
    cluster.addDesiredConfig("_test", config1);
    cluster.addDesiredConfig("_test", config2);
    
    Service hdfs = cluster.addService("HDFS");
    Service mapReduce = cluster.addService("MAPREDUCE");
    hdfs.persist();
    mapReduce.persist();

    hdfs.addServiceComponent(Role.HDFS_CLIENT.name()).persist();
    mapReduce.addServiceComponent(Role.MAPREDUCE_CLIENT.name()).persist();

    hdfs.getServiceComponent(Role.HDFS_CLIENT.name()).addServiceComponentHost("h1").persist();
    mapReduce.getServiceComponent(Role.MAPREDUCE_CLIENT.name()).addServiceComponentHost("h2").persist();

    Set<ActionRequest> actionRequests = new HashSet<ActionRequest>();
    Map<String, String> params = new HashMap<String, String>(){{
      put("test", "test");
    }};
    ActionRequest actionRequest = new ActionRequest("c1", "HDFS", Role.HDFS_SERVICE_CHECK.name(), params);
    actionRequests.add(actionRequest);
    
    Map<String, String> requestProperties = new HashMap<String, String>();
    requestProperties.put(REQUEST_CONTEXT_PROPERTY, "Called from a test");

    RequestStatusResponse response = controller.createActions(actionRequests, requestProperties);
    
    assertEquals(1, response.getTasks().size());
    ShortTaskStatus task = response.getTasks().get(0);

    List<HostRoleCommand> storedTasks = actionDB.getRequestTasks(response.getRequestId());
    Stage stage = actionDB.getAllStages(response.getRequestId()).get(0);

    //Check configs not stored with execution command
    ExecutionCommandDAO executionCommandDAO = injector.getInstance(ExecutionCommandDAO.class);
    ExecutionCommandEntity commandEntity = executionCommandDAO.findByPK(task.getTaskId());
    ExecutionCommand executionCommand =
        StageUtils.fromJson(new String(commandEntity.getCommand()), ExecutionCommand.class);

    assertFalse(executionCommand.getConfigurationTags().isEmpty());
    assertTrue(executionCommand.getConfigurations() == null || executionCommand.getConfigurations().isEmpty());

    assertEquals(1, storedTasks.size());
    HostRoleCommand hostRoleCommand = storedTasks.get(0);

    assertEquals(task.getTaskId(), hostRoleCommand.getTaskId());
    assertEquals(actionRequest.getServiceName(), hostRoleCommand.getExecutionCommandWrapper().getExecutionCommand().getServiceName());
    assertEquals(actionRequest.getClusterName(), hostRoleCommand.getExecutionCommandWrapper().getExecutionCommand().getClusterName());
    assertEquals(actionRequest.getActionName(), hostRoleCommand.getExecutionCommandWrapper().getExecutionCommand().getRole().name());
    assertEquals(Role.HDFS_CLIENT.name(), hostRoleCommand.getEvent().getEvent().getServiceComponentName());
    assertEquals(actionRequest.getParameters(), hostRoleCommand.getExecutionCommandWrapper().getExecutionCommand().getRoleParams());
    assertNotNull(hostRoleCommand.getExecutionCommandWrapper().getExecutionCommand().getConfigurations());
    assertEquals(2, hostRoleCommand.getExecutionCommandWrapper().getExecutionCommand().getConfigurations().size());
    assertEquals(requestProperties.get(REQUEST_CONTEXT_PROPERTY), stage.getRequestContext());
    actionRequests.add(new ActionRequest("c1", "MAPREDUCE", Role.MAPREDUCE_SERVICE_CHECK.name(), null));

    response = controller.createActions(actionRequests, requestProperties);

    assertEquals(2, response.getTasks().size());

    List<HostRoleCommand> tasks = actionDB.getRequestTasks(response.getRequestId());

    assertEquals(2, tasks.size());

    requestProperties.put(REQUEST_CONTEXT_PROPERTY, null);
    response = controller.createActions(actionRequests, requestProperties);

    assertEquals(2, response.getTasks().size());
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
    users.createDefaultRoles();

    UserRequest request = new UserRequest("user1");
    request.setRoles(new HashSet<String>(){{
      add("user");
      add("admin");
    }});

    controller.updateUsers(Collections.singleton(request));
  }

  @SuppressWarnings("serial")
  @Test
  public void testDeleteUsers() throws Exception {
    createUser("user1");

    users.createDefaultRoles();

    UserRequest request = new UserRequest("user1");
    request.setRoles(new HashSet<String>(){{
      add("user");
      add("admin");
    }});
    controller.updateUsers(Collections.singleton(request));

    request = new UserRequest("user1");
    controller.deleteUsers(Collections.singleton(request));

    Set<UserResponse> responses = controller.getUsers(
        Collections.singleton(new UserRequest(null)));

    Assert.assertEquals(0, responses.size());

    RoleDAO roleDao = injector.getInstance(RoleDAO.class);
    RoleEntity re1 = roleDao.findByName("user");
    RoleEntity re2 = roleDao.findByName("admin");
    Assert.assertNotNull(re1);
    Assert.assertNotNull(re2);
  }

  @Test
  public void testRcaOnJobtrackerHost() throws AmbariException {
    String clusterName = "foo1";
    createCluster(clusterName);
    clusters.getCluster(clusterName)
        .setDesiredStackVersion(new StackId("HDP-0.1"));
    String serviceName = "MAPREDUCE";
    createService(clusterName, serviceName, null);
    String componentName1 = "JOBTRACKER";
    String componentName2 = "TASKTRACKER";
    String componentName3 = "MAPREDUCE_CLIENT";

    Map<String, String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "Called from a test");

    createServiceComponent(clusterName, serviceName, componentName1,
        State.INIT);
    createServiceComponent(clusterName, serviceName, componentName2,
        State.INIT);
    createServiceComponent(clusterName, serviceName, componentName3,
        State.INIT);

    String host1 = "h1";
    clusters.addHost(host1);
    clusters.getHost("h1").setOsType("centos5");
    clusters.getHost("h1").setState(HostState.HEALTHY);
    clusters.getHost("h1").persist();
    String host2 = "h2";
    clusters.addHost(host2);
    clusters.getHost("h2").setOsType("centos6");
    clusters.getHost("h2").setState(HostState.HEALTHY);
    clusters.getHost("h2").persist();

    clusters.mapHostToCluster(host1, clusterName);
    clusters.mapHostToCluster(host2, clusterName);


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

    Map<String, String> configs = new HashMap<String, String>();
    configs.put("a", "b");
    configs.put("rca_enabled", "true");


    ClusterRequest cr = new ClusterRequest(null, clusterName, null, null);
    cr.setDesiredConfig(new ConfigurationRequest(clusterName, "global",
        "v1", configs));
    controller.updateClusters(Collections.singleton(cr), Collections.<String, String>emptyMap());

    Set<ServiceRequest> sReqs = new HashSet<ServiceRequest>();
    Map<String, String> configVersions = new HashMap<String, String>();
    configVersions.put("global", "v1");
    sReqs.clear();
    sReqs.add(new ServiceRequest(clusterName, serviceName, configVersions,
        "INSTALLED"));
    RequestStatusResponse trackAction = controller.updateServices(sReqs,
      mapRequestProps, true, false);
    List<Stage> stages = actionDB.getAllStages(trackAction.getRequestId());
    for (ExecutionCommandWrapper cmd : stages.get(0)
        .getExecutionCommands(host1)) {
      assertEquals(
          "true",
          cmd.getExecutionCommand().getConfigurations().get("global")
              .get("rca_enabled"));
    }
    for (ExecutionCommandWrapper cmd : stages.get(0)
        .getExecutionCommands(host2)) {
      assertEquals(
          "false",
          cmd.getExecutionCommand().getConfigurations().get("global")
              .get("rca_enabled"));
    }
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
    clusters.addHost(host1);
    clusters.getHost("h1").setOsType("centos5");
    clusters.getHost("h1").persist();
    String host2 = "h2";
    clusters.addHost(host2);
    clusters.getHost("h2").setOsType("centos6");
    clusters.getHost("h2").persist();

    clusters.mapHostToCluster(host1, clusterName);
    clusters.mapHostToCluster(host2, clusterName);


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
    ServiceRequest r = new ServiceRequest(clusterName, serviceName, null,
            State.INSTALLED.toString());
    Set<ServiceRequest> requests = new HashSet<ServiceRequest>();
    requests.add(r);

    controller.updateServices(requests, mapRequestProps, true, false);
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
    r = new ServiceRequest(clusterName, serviceName, null,
            State.STARTED.toString());
    requests.clear();
    requests.add(r);
    controller.updateServices(requests, mapRequestProps, true, false);

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
    cr1 = new ConfigurationRequest(clusterName, "typeA","v1", configs);
    cr2 = new ConfigurationRequest(clusterName, "typeB","v1", configs);
    cr3 = new ConfigurationRequest(clusterName, "typeC","v1", configs);
    cr4 = new ConfigurationRequest(clusterName, "typeD","v1", configs);
    cr5 = new ConfigurationRequest(clusterName, "typeA","v2", configs);
    cr6 = new ConfigurationRequest(clusterName, "typeB","v2", configs);
    cr7 = new ConfigurationRequest(clusterName, "typeC","v2", configs);
    cr8 = new ConfigurationRequest(clusterName, "typeE","v1", configs);
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
            componentName1, host1, configVersions, null));
    Assert.assertNull(controller.updateHostComponents(schReqs, Collections.<String, String>emptyMap(), true));

    Assert.assertEquals(0, s.getDesiredConfigs().size());
    Assert.assertEquals(0, sc1.getDesiredConfigs().size());
    Assert.assertEquals(3, sch1.getDesiredConfigs().size());

    configVersions.clear();
    configVersions.put("typeC", "v1");
    configVersions.put("typeD", "v1");
    scReqs.clear();
    scReqs.add(new ServiceComponentRequest(clusterName, serviceName,
            componentName2, configVersions, null));
    Assert.assertNull(controller.updateComponents(scReqs, Collections.<String, String>emptyMap(), true));

    Assert.assertEquals(0, s.getDesiredConfigs().size());
    Assert.assertEquals(0, sc1.getDesiredConfigs().size());
    Assert.assertEquals(2, sc2.getDesiredConfigs().size());
    Assert.assertEquals(3, sch1.getDesiredConfigs().size());

    // update configs at service level
    configVersions.clear();
    configVersions.put("typeA", "v2");
    configVersions.put("typeC", "v2");
    configVersions.put("typeE", "v1");
    sReqs.clear();
    sReqs.add(new ServiceRequest(clusterName, serviceName, configVersions,
            null));
    Assert.assertNull(controller.updateServices(sReqs, mapRequestProps, true, false));

    Assert.assertEquals(3, s.getDesiredConfigs().size());
    Assert.assertEquals(3, sc1.getDesiredConfigs().size());
    Assert.assertEquals(4, sc2.getDesiredConfigs().size());
    Assert.assertEquals(4, sch1.getDesiredConfigs().size());

    Assert.assertEquals("v2",
            s.getDesiredConfigs().get("typeA").getVersionTag());
    Assert.assertEquals("v2",
            s.getDesiredConfigs().get("typeC").getVersionTag());
    Assert.assertEquals("v1",
            s.getDesiredConfigs().get("typeE").getVersionTag());

    Assert.assertEquals("v2",
            sc1.getDesiredConfigs().get("typeA").getVersionTag());
    Assert.assertEquals("v2",
            sc1.getDesiredConfigs().get("typeC").getVersionTag());
    Assert.assertEquals("v1",
            sc1.getDesiredConfigs().get("typeE").getVersionTag());

    Assert.assertEquals("v2",
            sc2.getDesiredConfigs().get("typeA").getVersionTag());
    Assert.assertEquals("v2",
            sc2.getDesiredConfigs().get("typeC").getVersionTag());
    Assert.assertEquals("v1",
            sc2.getDesiredConfigs().get("typeE").getVersionTag());
    Assert.assertEquals("v1",
            sc2.getDesiredConfigs().get("typeD").getVersionTag());

    Assert.assertEquals("v2",
            sch1.getDesiredConfigs().get("typeA").getVersionTag());
    Assert.assertEquals("v2",
            sch1.getDesiredConfigs().get("typeC").getVersionTag());
    Assert.assertEquals("v1",
            sch1.getDesiredConfigs().get("typeE").getVersionTag());
    Assert.assertEquals("v1",
            sch1.getDesiredConfigs().get("typeB").getVersionTag());

    // update configs at SCH level
    configVersions.clear();
    configVersions.put("typeA", "v1");
    configVersions.put("typeB", "v1");
    configVersions.put("typeC", "v1");
    schReqs.clear();
    schReqs.add(new ServiceComponentHostRequest(clusterName, serviceName,
            componentName1, host1, configVersions, null));
    Assert.assertNull(controller.updateHostComponents(schReqs, Collections.<String, String>emptyMap(), true));

    Assert.assertEquals(3, s.getDesiredConfigs().size());
    Assert.assertEquals(3, sc1.getDesiredConfigs().size());
    Assert.assertEquals(4, sc2.getDesiredConfigs().size());
    Assert.assertEquals(4, sch1.getDesiredConfigs().size());

    Assert.assertEquals("v1",
            sch1.getDesiredConfigs().get("typeA").getVersionTag());
    Assert.assertEquals("v1",
            sch1.getDesiredConfigs().get("typeC").getVersionTag());
    Assert.assertEquals("v1",
            sch1.getDesiredConfigs().get("typeE").getVersionTag());
    Assert.assertEquals("v1",
            sch1.getDesiredConfigs().get("typeB").getVersionTag());

    // update configs at SC level
    configVersions.clear();
    configVersions.put("typeC", "v2");
    configVersions.put("typeD", "v1");
    scReqs.clear();
    scReqs.add(new ServiceComponentRequest(clusterName, serviceName,
            componentName1, configVersions, null));
    Assert.assertNull(controller.updateComponents(scReqs, Collections.<String, String>emptyMap(), true));

    Assert.assertEquals(3, s.getDesiredConfigs().size());
    Assert.assertEquals(4, sc1.getDesiredConfigs().size());
    Assert.assertEquals(4, sc2.getDesiredConfigs().size());
    Assert.assertEquals(5, sch1.getDesiredConfigs().size());

    Assert.assertEquals("v2",
            sc1.getDesiredConfigs().get("typeA").getVersionTag());
    Assert.assertEquals("v2",
            sc1.getDesiredConfigs().get("typeC").getVersionTag());
    Assert.assertEquals("v1",
            sc2.getDesiredConfigs().get("typeD").getVersionTag());
    Assert.assertEquals("v1",
            sc1.getDesiredConfigs().get("typeE").getVersionTag());

    Assert.assertEquals("v1",
            sch1.getDesiredConfigs().get("typeA").getVersionTag());
    Assert.assertEquals("v2",
            sch1.getDesiredConfigs().get("typeC").getVersionTag());
    Assert.assertEquals("v1",
            sch1.getDesiredConfigs().get("typeD").getVersionTag());
    Assert.assertEquals("v1",
            sch1.getDesiredConfigs().get("typeE").getVersionTag());
    Assert.assertEquals("v1",
            sch1.getDesiredConfigs().get("typeB").getVersionTag());
  }

  @Test
  public void testConfigUpdates() throws AmbariException {
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
    clusters.addHost(host1);
    clusters.getHost("h1").setOsType("centos5");
    clusters.getHost("h1").persist();
    String host2 = "h2";
    clusters.addHost(host2);
    clusters.getHost("h2").setOsType("centos6");
    clusters.getHost("h2").persist();

    clusters.mapHostToCluster(host1, clusterName);
    clusters.mapHostToCluster(host2, clusterName);


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

    ConfigurationRequest cr1, cr2, cr3, cr4, cr5, cr6, cr7, cr8;
    cr1 = new ConfigurationRequest(clusterName, "typeA","v1", configs);
    cr2 = new ConfigurationRequest(clusterName, "typeB","v1", configs);
    cr3 = new ConfigurationRequest(clusterName, "typeC","v1", configs);
    cr4 = new ConfigurationRequest(clusterName, "typeD","v1", configs);
    cr5 = new ConfigurationRequest(clusterName, "typeA","v2", configs);
    cr6 = new ConfigurationRequest(clusterName, "typeB","v2", configs);
    cr7 = new ConfigurationRequest(clusterName, "typeC","v2", configs);
    cr8 = new ConfigurationRequest(clusterName, "typeE","v1", configs);
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
        componentName1, host1, configVersions, null));
    Assert.assertNull(controller.updateHostComponents(schReqs, Collections.<String, String>emptyMap(), true));

    Assert.assertEquals(0, s.getDesiredConfigs().size());
    Assert.assertEquals(0, sc1.getDesiredConfigs().size());
    Assert.assertEquals(3, sch1.getDesiredConfigs().size());

    configVersions.clear();
    configVersions.put("typeC", "v1");
    configVersions.put("typeD", "v1");
    scReqs.clear();
    scReqs.add(new ServiceComponentRequest(clusterName, serviceName,
        componentName2, configVersions, null));
    Assert.assertNull(controller.updateComponents(scReqs, Collections.<String, String>emptyMap(), true));

    Assert.assertEquals(0, s.getDesiredConfigs().size());
    Assert.assertEquals(0, sc1.getDesiredConfigs().size());
    Assert.assertEquals(2, sc2.getDesiredConfigs().size());
    Assert.assertEquals(3, sch1.getDesiredConfigs().size());

    // update configs at service level
    configVersions.clear();
    configVersions.put("typeA", "v2");
    configVersions.put("typeC", "v2");
    configVersions.put("typeE", "v1");
    sReqs.clear();
    sReqs.add(new ServiceRequest(clusterName, serviceName, configVersions,
        null));
    Assert.assertNull(controller.updateServices(sReqs, mapRequestProps, true, false));

    Assert.assertEquals(3, s.getDesiredConfigs().size());
    Assert.assertEquals(3, sc1.getDesiredConfigs().size());
    Assert.assertEquals(4, sc2.getDesiredConfigs().size());
    Assert.assertEquals(4, sch1.getDesiredConfigs().size());

    Assert.assertEquals("v2",
        s.getDesiredConfigs().get("typeA").getVersionTag());
    Assert.assertEquals("v2",
        s.getDesiredConfigs().get("typeC").getVersionTag());
    Assert.assertEquals("v1",
        s.getDesiredConfigs().get("typeE").getVersionTag());

    Assert.assertEquals("v2",
        sc1.getDesiredConfigs().get("typeA").getVersionTag());
    Assert.assertEquals("v2",
        sc1.getDesiredConfigs().get("typeC").getVersionTag());
    Assert.assertEquals("v1",
        sc1.getDesiredConfigs().get("typeE").getVersionTag());

    Assert.assertEquals("v2",
        sc2.getDesiredConfigs().get("typeA").getVersionTag());
    Assert.assertEquals("v2",
        sc2.getDesiredConfigs().get("typeC").getVersionTag());
    Assert.assertEquals("v1",
        sc2.getDesiredConfigs().get("typeE").getVersionTag());
    Assert.assertEquals("v1",
        sc2.getDesiredConfigs().get("typeD").getVersionTag());

    Assert.assertEquals("v2",
        sch1.getDesiredConfigs().get("typeA").getVersionTag());
    Assert.assertEquals("v2",
        sch1.getDesiredConfigs().get("typeC").getVersionTag());
    Assert.assertEquals("v1",
        sch1.getDesiredConfigs().get("typeE").getVersionTag());
    Assert.assertEquals("v1",
        sch1.getDesiredConfigs().get("typeB").getVersionTag());

    // update configs at SCH level
    configVersions.clear();
    configVersions.put("typeA", "v1");
    configVersions.put("typeB", "v1");
    configVersions.put("typeC", "v1");
    schReqs.clear();
    schReqs.add(new ServiceComponentHostRequest(clusterName, serviceName,
        componentName1, host1, configVersions, null));
    Assert.assertNull(controller.updateHostComponents(schReqs, Collections.<String, String>emptyMap(), true));

    Assert.assertEquals(3, s.getDesiredConfigs().size());
    Assert.assertEquals(3, sc1.getDesiredConfigs().size());
    Assert.assertEquals(4, sc2.getDesiredConfigs().size());
    Assert.assertEquals(4, sch1.getDesiredConfigs().size());

    Assert.assertEquals("v1",
        sch1.getDesiredConfigs().get("typeA").getVersionTag());
    Assert.assertEquals("v1",
        sch1.getDesiredConfigs().get("typeC").getVersionTag());
    Assert.assertEquals("v1",
        sch1.getDesiredConfigs().get("typeE").getVersionTag());
    Assert.assertEquals("v1",
        sch1.getDesiredConfigs().get("typeB").getVersionTag());

    // update configs at SC level
    configVersions.clear();
    configVersions.put("typeC", "v2");
    configVersions.put("typeD", "v1");
    scReqs.clear();
    scReqs.add(new ServiceComponentRequest(clusterName, serviceName,
        componentName1, configVersions, null));
    Assert.assertNull(controller.updateComponents(scReqs, Collections.<String, String>emptyMap(), true));

    Assert.assertEquals(3, s.getDesiredConfigs().size());
    Assert.assertEquals(4, sc1.getDesiredConfigs().size());
    Assert.assertEquals(4, sc2.getDesiredConfigs().size());
    Assert.assertEquals(5, sch1.getDesiredConfigs().size());

    Assert.assertEquals("v2",
        sc1.getDesiredConfigs().get("typeA").getVersionTag());
    Assert.assertEquals("v2",
        sc1.getDesiredConfigs().get("typeC").getVersionTag());
    Assert.assertEquals("v1",
        sc2.getDesiredConfigs().get("typeD").getVersionTag());
    Assert.assertEquals("v1",
        sc1.getDesiredConfigs().get("typeE").getVersionTag());

    Assert.assertEquals("v1",
        sch1.getDesiredConfigs().get("typeA").getVersionTag());
    Assert.assertEquals("v2",
        sch1.getDesiredConfigs().get("typeC").getVersionTag());
    Assert.assertEquals("v1",
        sch1.getDesiredConfigs().get("typeD").getVersionTag());
    Assert.assertEquals("v1",
        sch1.getDesiredConfigs().get("typeE").getVersionTag());
    Assert.assertEquals("v1",
        sch1.getDesiredConfigs().get("typeB").getVersionTag());

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
    clusters.addHost(host1);
    clusters.getHost("h1").setOsType("centos5");
    clusters.getHost("h1").persist();
    String host2 = "h2";
    clusters.addHost(host2);
    clusters.getHost("h2").setOsType("centos6");
    clusters.getHost("h2").persist();

    clusters.mapHostToCluster(host1, clusterName);
    clusters.mapHostToCluster(host2, clusterName);


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
    ServiceRequest r = new ServiceRequest(clusterName, serviceName, null,
      State.INSTALLED.toString());
    Set<ServiceRequest> requests = new HashSet<ServiceRequest>();
    requests.add(r);

    controller.updateServices(requests, mapRequestProps, true, false);
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
      configs);
    cr2 = new ConfigurationRequest(clusterName, "hdfs-site","version1",
      configs);
    cr3 = new ConfigurationRequest(clusterName, "core-site","version122",
      configs);
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
      componentName1, host1, configVersions, null));
    Assert.assertNull(controller.updateHostComponents(schReqs, Collections.<String, String>emptyMap(), true));
    Assert.assertEquals(2, sch1.getDesiredConfigs().size());

    // Reconfigure SCH level
    configVersions.clear();
    configVersions.put("core-site", "version122");
    schReqs.clear();
    schReqs.add(new ServiceComponentHostRequest(clusterName, serviceName,
      componentName1, host1, configVersions, null));
    Assert.assertNull(controller.updateHostComponents(schReqs, Collections.<String, String>emptyMap(), true));

    // Clear Entity Manager
    entityManager.clear();

    Assert.assertEquals(2, sch1.getDesiredConfigs().size());
    Assert.assertEquals("version122", sch1.getDesiredConfigs().get
      ("core-site").getVersionTag());

    //SC Level
    configVersions.clear();
    configVersions.put("core-site", "version1");
    configVersions.put("hdfs-site", "version1");
    scReqs.add(new ServiceComponentRequest(clusterName, serviceName,
      componentName2, configVersions, null));
    Assert.assertNull(controller.updateComponents(scReqs, Collections.<String, String>emptyMap(), true));

    scReqs.add(new ServiceComponentRequest(clusterName, serviceName,
      componentName1, configVersions, null));
    Assert.assertNull(controller.updateComponents(scReqs, Collections.<String, String>emptyMap(), true));
    Assert.assertEquals(2, sc1.getDesiredConfigs().size());
    Assert.assertEquals(2, sc2.getDesiredConfigs().size());

    // Reconfigure SC level
    configVersions.clear();
    configVersions.put("core-site", "version122");

    scReqs.clear();
    scReqs.add(new ServiceComponentRequest(clusterName, serviceName,
      componentName2, configVersions, null));
    Assert.assertNull(controller.updateComponents(scReqs, Collections.<String, String>emptyMap(), true));

    Assert.assertEquals(2, sc2.getDesiredConfigs().size());
    Assert.assertEquals("version122", sc2.getDesiredConfigs().get
      ("core-site").getVersionTag());
    scReqs.clear();
    scReqs.add(new ServiceComponentRequest(clusterName, serviceName,
      componentName1, configVersions, null));
    Assert.assertNull(controller.updateComponents(scReqs, Collections.<String, String>emptyMap(), true));

    entityManager.clear();

    Assert.assertEquals(2, sc1.getDesiredConfigs().size());
    Assert.assertEquals("version122", sc1.getDesiredConfigs().get
      ("core-site").getVersionTag());

    // S level
    configVersions.clear();
    configVersions.put("core-site", "version1");
    configVersions.put("hdfs-site", "version1");
    sReqs.clear();
    sReqs.add(new ServiceRequest(clusterName, serviceName, configVersions,
      null));
    Assert.assertNull(controller.updateServices(sReqs, mapRequestProps, true, false));
    Assert.assertEquals(2, s.getDesiredConfigs().size());

    // Reconfigure S Level
    configVersions.clear();
    configVersions.put("core-site", "version122");

    sReqs.clear();
    sReqs.add(new ServiceRequest(clusterName, serviceName, configVersions,
      null));
    Assert.assertNull(controller.updateServices(sReqs, mapRequestProps, true, false));

    entityManager.clear();

    Assert.assertEquals(2, s.getDesiredConfigs().size());
    Assert.assertEquals("version122", s.getDesiredConfigs().get
      ("core-site").getVersionTag());
  }

  @Test
  public void testReConfigureServiceClient() throws AmbariException {
    String clusterName = "foo1";
    createCluster(clusterName);
    clusters.getCluster(clusterName)
      .setDesiredStackVersion(new StackId("HDP-0.1"));
    String serviceName1 = "HDFS";
    String serviceName2 = "MAPREDUCE";
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";
    String componentName4 = "JOBTRACKER";
    String componentName5 = "TASKTRACKER";
    String componentName6 = "MAPREDUCE_CLIENT";
    Map<String, String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "Called from a test");

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
    clusters.addHost(host1);
    clusters.addHost(host2);
    clusters.addHost(host3);
    clusters.getHost("h1").setOsType("centos6");
    clusters.getHost("h1").setState(HostState.HEALTHY);
    clusters.getHost("h1").persist();
    clusters.getHost("h2").setOsType("centos6");
    clusters.getHost("h2").setState(HostState.HEALTHY);
    clusters.getHost("h2").persist();
    clusters.getHost("h3").setOsType("centos6");
    clusters.getHost("h3").setState(HostState.HEALTHY);
    clusters.getHost("h3").persist();

    clusters.mapHostToCluster(host1, clusterName);
    clusters.mapHostToCluster(host2, clusterName);
    clusters.mapHostToCluster(host3, clusterName);

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

    ConfigurationRequest cr1,cr2,cr3;
    cr1 = new ConfigurationRequest(clusterName, "core-site","version1",
      configs);
    cr2 = new ConfigurationRequest(clusterName, "hdfs-site","version1",
      configs);

    ClusterRequest crReq = new ClusterRequest(null, clusterName, null, null);
    crReq.setDesiredConfig(cr1);
    controller.updateClusters(Collections.singleton(crReq), null);
    crReq = new ClusterRequest(null, clusterName, null, null);
    crReq.setDesiredConfig(cr2);
    controller.updateClusters(Collections.singleton(crReq), null);

    // Install
    long requestId1 = installService(clusterName, serviceName1, true, false);

    List<Stage> stages = actionDB.getAllStages(requestId1);
    Assert.assertEquals(2, stages.get(0).getOrderedHostRoleCommands().get(0)
      .getExecutionCommandWrapper().getExecutionCommand()
      .getConfigurationTags().size());

    installService(clusterName, serviceName2, false, false);

    // Start
    startService(clusterName, serviceName1, true, false);
    startService(clusterName, serviceName2, true, false);

    // Reconfigure
    configs.clear();
    configs.put("c", "d");
    cr3 = new ConfigurationRequest(clusterName, "core-site","version122",
      configs);
    crReq = new ClusterRequest(null, clusterName, null, null);
    crReq.setDesiredConfig(cr3);
    controller.updateClusters(Collections.singleton(crReq), null);

    // Stop HDFS & MAPREDUCE
    stopService(clusterName, serviceName1, false, false);
    stopService(clusterName, serviceName2, false, false);

    // Start
    long requestId2 = startService(clusterName, serviceName1, true, true);
    long requestId3 = startService(clusterName, serviceName2, true, true);

    stages = actionDB.getAllStages(requestId2);
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
          if (hrc.getHostName().equals(host3))
            hdfsCmdHost3 = hrc;
          else if (hrc.getHostName().equals(host2))
            hdfsCmdHost2 = hrc;
        }
        if (hrc.getRole().toString().equals("MAPREDUCE_CLIENT")) {
          if (hrc.getHostName().equals(host2))
            mapRedCmdHost2 = hrc;
          else if (hrc.getHostName().equals(host3))
            mapRedCmdHost3 = hrc;
        }
      }
    }
    Assert.assertNotNull(hdfsCmdHost3);
    Assert.assertNotNull(hdfsCmdHost2);
    ExecutionCommand execCmd = hdfsCmdHost3.getExecutionCommandWrapper()
      .getExecutionCommand();
    Assert.assertEquals(2, execCmd.getConfigurationTags().size());
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

    // Start
    requestId2 = startService(clusterName, serviceName1, true, true);
    requestId3 = startService(clusterName, serviceName2, true, true);
    stages = actionDB.getAllStages(requestId2);
    stages.addAll(actionDB.getAllStages(requestId3));
    HostRoleCommand clientWithHostDown = null;
    for (Stage stage : stages) {
      for (HostRoleCommand hrc : stage.getOrderedHostRoleCommands()) {
        if (hrc.getRole().toString().equals("MAPREDUCE_CLIENT") && hrc
          .getHostName().equals(host2))
          clientWithHostDown = hrc;
      }
    }
    Assert.assertNull(clientWithHostDown);
  }

  @Test
  public void testReconfigureClientWithServiceStarted() throws
    AmbariException {
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
    clusters.addHost(host1);
    clusters.getHost("h1").setOsType("centos5");
    clusters.getHost("h1").setState(HostState.HEALTHY);
    clusters.getHost("h1").persist();
    String host2 = "h2";
    clusters.addHost(host2);
    clusters.getHost("h2").setOsType("centos6");
    clusters.getHost("h2").setState(HostState.HEALTHY);
    clusters.getHost("h2").persist();

    clusters.mapHostToCluster(host1, clusterName);
    clusters.mapHostToCluster(host2, clusterName);

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

    ConfigurationRequest cr1,cr2,cr3;
    cr1 = new ConfigurationRequest(clusterName, "core-site","version1",
      configs);
    cr2 = new ConfigurationRequest(clusterName, "hdfs-site","version1",
      configs);

    ClusterRequest crReq = new ClusterRequest(null, clusterName, null, null);
    crReq.setDesiredConfig(cr1);
    controller.updateClusters(Collections.singleton(crReq), null);
    crReq = new ClusterRequest(null, clusterName, null, null);
    crReq.setDesiredConfig(cr2);
    controller.updateClusters(Collections.singleton(crReq), null);

    installService(clusterName, serviceName, false, false);
    startService(clusterName, serviceName, false, false);

    Cluster c = clusters.getCluster(clusterName);
    Service s = c.getService(serviceName);
    // Stop Sch only
    long id = stopServiceComponentHosts(clusterName, serviceName);
    Assert.assertEquals(State.STARTED, s.getDesiredState());
    for (ServiceComponent sc : s.getServiceComponents().values()) {
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        Assert.assertEquals(State.INSTALLED, sch.getDesiredState());
      }
    }

    // Reconfigure
    configs.clear();
    configs.put("c", "d");
    cr3 = new ConfigurationRequest(clusterName, "core-site","version122",
      configs);
    crReq = new ClusterRequest(null, clusterName, null, null);
    crReq.setDesiredConfig(cr3);
    controller.updateClusters(Collections.singleton(crReq), null);

    id = startService(clusterName, serviceName, false, true);
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
    clusters.addHost(host1);
    clusters.getHost("h1").setState(HostState.HEALTHY);
    clusters.getHost("h1").persist();
    String host2 = "h2";
    clusters.addHost(host2);
    clusters.getHost("h2").setState(HostState.HEALTHY);
    clusters.getHost("h2").persist();

    clusters.getHost("h1").setOsType("centos5");
    clusters.getHost("h2").setOsType("centos6");
    clusters.mapHostToCluster(host1, clusterName);
    clusters.mapHostToCluster(host2, clusterName);

    Map<String, String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "Called from a test");

    // null service should work
    createServiceComponentHost(clusterName, null, componentName1,
        host1, null);
    createServiceComponentHost(clusterName, null, componentName1,
        host2, null);

    ServiceRequest r = new ServiceRequest(clusterName, serviceName, null,
        State.INSTALLED.toString());
    Set<ServiceRequest> requests = new HashSet<ServiceRequest>();
    requests.add(r);

    RequestStatusResponse trackAction =
        controller.updateServices(requests, mapRequestProps, true, false);
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

    r = new ServiceRequest(clusterName, serviceName, null,
        State.STARTED.toString());
    requests.clear();
    requests.add(r);

    trackAction = controller.updateServices(requests, mapRequestProps, true, false);
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
    clusters.addHost(host1);
    clusters.getHost("h1").setOsType("centos5");
    clusters.getHost("h1").setState(HostState.HEALTHY);
    clusters.getHost("h1").persist();
    String host2 = "h2";
    clusters.addHost(host2);
    clusters.getHost("h2").setOsType("centos6");
    clusters.getHost("h2").setState(HostState.HEALTHY);
    clusters.getHost("h2").persist();
    String host3 = "h3";
    clusters.addHost(host3);
    clusters.getHost("h3").setOsType("centos6");
    clusters.getHost("h3").setState(HostState.HEALTHY);
    clusters.getHost("h3").persist();

    clusters.mapHostToCluster(host1, clusterName);
    clusters.mapHostToCluster(host2, clusterName);
    clusters.mapHostToCluster(host3, clusterName);

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
    clusters.addHost(host1);
    clusters.getHost("h1").setOsType("centos5");
    clusters.getHost("h1").setState(HostState.HEALTHY);
    clusters.getHost("h1").persist();
    String host2 = "h2";
    clusters.addHost(host2);
    clusters.getHost("h2").setOsType("centos6");
    clusters.getHost("h2").setState(HostState.HEALTHY);
    clusters.getHost("h2").persist();
    String host3 = "h3";
    clusters.addHost(host3);
    clusters.getHost("h3").setOsType("centos6");
    clusters.getHost("h3").setState(HostState.HEALTHY);
    clusters.getHost("h3").persist();

    clusters.mapHostToCluster(host1, clusterName);
    clusters.mapHostToCluster(host2, clusterName);
    clusters.mapHostToCluster(host3, clusterName);

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
      if(command.getRoleCommand() == RoleCommand.EXECUTE &&
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
      if(command.getRoleCommand() == RoleCommand.EXECUTE &&
          command.getRole() == Role.HDFS_SERVICE_CHECK) {
        Assert.assertTrue(command.getHostName().equals("h3"));
        commandCount++;
      }
    }
    Assert.assertEquals("Expect only one service check.", 1, commandCount);

    Set<ActionRequest> actionRequests = new HashSet<ActionRequest>();
    ActionRequest actionRequest = new ActionRequest("foo1", "HDFS", Role.HDFS_SERVICE_CHECK.name(), null);
    actionRequests.add(actionRequest);
    Map<String, String> requestProperties = new HashMap<String, String>();

    RequestStatusResponse response = controller.createActions(actionRequests, requestProperties);
    commands = actionDB.getRequestTasks(response.getRequestId());
    commandCount = 0;
    for(HostRoleCommand command : commands) {
      if(command.getRoleCommand() == RoleCommand.EXECUTE &&
          command.getRole() == Role.HDFS_SERVICE_CHECK) {
        Assert.assertTrue(command.getHostName().equals("h3"));
        commandCount++;
      }
    }
    Assert.assertEquals("Expect only one service check.", 1, commandCount);

    // When both are unhealthy then just pick one
    clusters.getHost("h3").setState(HostState.WAITING_FOR_HOST_STATUS_UPDATES);
    clusters.getHost("h2").setState(HostState.INIT);
    response = controller.createActions(actionRequests, requestProperties);
    commands = actionDB.getRequestTasks(response.getRequestId());
    commandCount = 0;
    for(HostRoleCommand command : commands) {
      if(command.getRoleCommand() == RoleCommand.EXECUTE &&
          command.getRole() == Role.HDFS_SERVICE_CHECK) {
        Assert.assertTrue(command.getHostName().equals("h3") ||
            command.getHostName().equals("h2"));
        commandCount++;
      }
    }
    Assert.assertEquals("Expect only one service check.", 1, commandCount);
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
    clusters.addHost(host1);
    clusters.getHost("h1").setOsType("centos5");
    clusters.getHost("h1").setState(HostState.HEALTHY);
    clusters.getHost("h1").persist();
    String host2 = "h2";
    clusters.addHost(host2);
    clusters.getHost("h2").setOsType("centos6");
    clusters.getHost("h2").setState(HostState.HEALTHY);
    clusters.getHost("h2").persist();
    String host3 = "h3";
    clusters.addHost(host3);
    clusters.getHost("h3").setOsType("centos6");
    clusters.getHost("h3").setState(HostState.HEALTHY);
    clusters.getHost("h3").persist();

    clusters.mapHostToCluster(host1, clusterName);
    clusters.mapHostToCluster(host2, clusterName);
    clusters.mapHostToCluster(host3, clusterName);

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
  public void testReInstallClientComponent() throws AmbariException {
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
    clusters.addHost(host1);
    clusters.getHost("h1").setOsType("centos5");
    clusters.getHost("h1").setState(HostState.HEALTHY);
    clusters.getHost("h1").persist();
    String host2 = "h2";
    clusters.addHost(host2);
    clusters.getHost("h2").setOsType("centos6");
    clusters.getHost("h2").setState(HostState.HEALTHY);
    clusters.getHost("h2").persist();
    String host3 = "h3";
    clusters.addHost(host3);
    clusters.getHost("h3").setOsType("centos6");
    clusters.getHost("h3").setState(HostState.HEALTHY);
    clusters.getHost("h3").persist();

    clusters.mapHostToCluster(host1, clusterName);
    clusters.mapHostToCluster(host2, clusterName);
    clusters.mapHostToCluster(host3, clusterName);

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
      (clusterName, serviceName, componentName3, host3, null,
        State.INSTALLED.name());
    Set<ServiceComponentHostRequest> setReqs = new
      HashSet<ServiceComponentHostRequest>();
    setReqs.add(schr);
    RequestStatusResponse resp = controller.updateHostComponents(setReqs,
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
  public void testHivePasswordAbsentInConfigs() throws AmbariException {
    String clusterName = "c1";
    String serviceName = "HIVE";
    createCluster(clusterName);
    clusters.getCluster(clusterName)
      .setDesiredStackVersion(new StackId("HDP-1.2.0"));
    createService(clusterName, serviceName, null);

    String componentName1 = "HIVE_METASTORE";
    String componentName2 = "HIVE_SERVER";
    String componentName3 = "HIVE_CLIENT";

    createServiceComponent(clusterName, serviceName, componentName1,
      State.INIT);
    createServiceComponent(clusterName, serviceName, componentName2,
      State.INIT);
    createServiceComponent(clusterName, serviceName, componentName3,
      State.INIT);

    String host1 = "h1";
    String host2 = "h2";
    clusters.addHost(host1);
    clusters.getHost("h1").setOsType("centos5");
    clusters.getHost("h1").setState(HostState.HEALTHY);
    clusters.getHost("h1").persist();
    clusters.addHost(host2);
    clusters.getHost("h2").setOsType("centos5");
    clusters.getHost("h2").setState(HostState.HEALTHY);
    clusters.getHost("h2").persist();
    clusters.mapHostToCluster(host1, clusterName);
    clusters.mapHostToCluster(host2, clusterName);

    createServiceComponentHost(clusterName, serviceName, componentName1,
      host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName2,
      host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName3,
      host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName3,
      host2, null);

    Map<String, String> configs = new HashMap<String, String>();
    configs.put("a", "b");
    configs.put(Configuration.HIVE_METASTORE_PASSWORD_PROPERTY, "aaa");

    ConfigurationRequest cr1;
    cr1 = new ConfigurationRequest(clusterName, "hive-site","version1",
      configs);
    ClusterRequest crReq = new ClusterRequest(null, clusterName, null, null);
    crReq.setDesiredConfig(cr1);
    controller.updateClusters(Collections.singleton(crReq), null);

    // Install
    installService(clusterName, serviceName, false, false);
    // Start
    long requestId = startService(clusterName, serviceName, false, true);

    String passwordInConfig = null;
    Boolean isClientInstalled = false;
    List<Stage> stages = actionDB.getAllStages(requestId);
    for (Stage s : stages) {
      for (HostRoleCommand hrc : s.getOrderedHostRoleCommands()) {
        if (hrc.getRole().equals(Role.HIVE_CLIENT)) {
          isClientInstalled = true;
          Map<String, String> hiveSite = hrc.getExecutionCommandWrapper()
            .getExecutionCommand().getConfigurations().get("hive-site");
          Assert.assertNotNull(hiveSite);
          Assert.assertEquals("b", hiveSite.get("a"));
          passwordInConfig = hiveSite.get(Configuration
            .HIVE_METASTORE_PASSWORD_PROPERTY);
        }
      }
    }
    Assert.assertTrue("HIVE_CLIENT must be installed", isClientInstalled);
    Assert.assertNull(passwordInConfig);
  }

  @Test
  public void testDecommissonDatanodeAction() throws AmbariException {
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
    clusters.addHost(host1);
    clusters.getHost("h1").setOsType("centos5");
    clusters.getHost("h1").setState(HostState.HEALTHY);
    clusters.getHost("h1").persist();
    String host2 = "h2";
    clusters.addHost(host2);
    clusters.getHost("h2").setOsType("centos6");
    clusters.getHost("h2").setState(HostState.HEALTHY);
    clusters.getHost("h2").persist();

    clusters.mapHostToCluster(host1, clusterName);
    clusters.mapHostToCluster(host2, clusterName);

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

    ConfigurationRequest cr1,cr2;
    cr1 = new ConfigurationRequest(clusterName, "hdfs-site","version1",
      configs);
    ClusterRequest crReq = new ClusterRequest(null, clusterName, null, null);
    crReq.setDesiredConfig(cr1);
    controller.updateClusters(Collections.singleton(crReq), null);
    Map<String, String> props = new HashMap<String, String>();
    props.put("datanodes", host2);
    cr2 = new ConfigurationRequest(clusterName, "hdfs-exclude-file", "tag1",
      props);
    crReq = new ClusterRequest(null, clusterName, null, null);
    crReq.setDesiredConfig(cr2);
    controller.updateClusters(Collections.singleton(crReq), null);

    // Start
    startService(clusterName, serviceName, false, false);

    Cluster cluster = clusters.getCluster(clusterName);
    Service s = cluster.getService(serviceName);
    Assert.assertEquals(State.STARTED, s.getDesiredState());

    Set<ActionRequest> requests = new HashSet<ActionRequest>();
    Map<String, String> params = new HashMap<String, String>(){{
      put("test", "test");
    }};
    ActionRequest request = new ActionRequest(clusterName, "HDFS",
      Role.DECOMMISSION_DATANODE.name(), params);
    params.put("excludeFileTag", "tag1");
    requests.add(request);

    Map<String, String> requestProperties = new HashMap<String, String>();
    requestProperties.put(REQUEST_CONTEXT_PROPERTY, "Called from a test");

    RequestStatusResponse response = controller.createActions(requests,
      requestProperties);

    List<HostRoleCommand> storedTasks = actionDB.getRequestTasks(response.getRequestId());
    ExecutionCommand execCmd = storedTasks.get(0).getExecutionCommandWrapper
      ().getExecutionCommand();
    Assert.assertNotNull(storedTasks);
    Assert.assertNotNull(execCmd.getConfigurationTags().get("hdfs-site"));
    Assert.assertEquals(1, storedTasks.size());
    Assert.assertEquals(host2, execCmd.getConfigurations().get
        ("hdfs-exclude-file").get("datanodes"));
    Assert.assertNotNull(execCmd.getConfigurationTags().get("hdfs-exclude-file"));
  }

  @Test
  public void testConfigsAttachedToServiceChecks() throws AmbariException {
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
    clusters.addHost(host1);
    clusters.getHost("h1").setOsType("centos5");
    clusters.getHost("h1").setState(HostState.HEALTHY);
    clusters.getHost("h1").persist();
    String host2 = "h2";
    clusters.addHost(host2);
    clusters.getHost("h2").setOsType("centos6");
    clusters.getHost("h2").setState(HostState.HEALTHY);
    clusters.getHost("h2").persist();

    clusters.mapHostToCluster(host1, clusterName);
    clusters.mapHostToCluster(host2, clusterName);


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
      configs);
    cr2 = new ConfigurationRequest(clusterName, "hdfs-site","version1",
      configs);

    ClusterRequest crReq = new ClusterRequest(null, clusterName, null, null);
    crReq.setDesiredConfig(cr1);
    controller.updateClusters(Collections.singleton(crReq), null);
    crReq = new ClusterRequest(null, clusterName, null, null);
    crReq.setDesiredConfig(cr2);
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
    Assert.assertEquals(true, serviceCheckFound);
  }
  
  @Test
  public void testConfigsAttachedToServiceNotCluster() throws AmbariException {
    String clusterName = "foo1";
    createCluster(clusterName);
    clusters.getCluster(clusterName).setDesiredStackVersion(new StackId("HDP-0.1"));

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
    clusters.addHost(host1);
    clusters.getHost("h1").setOsType("centos5");
    clusters.getHost("h1").setState(HostState.HEALTHY);
    clusters.getHost("h1").persist();
    String host2 = "h2";
    clusters.addHost(host2);
    clusters.getHost("h2").setOsType("centos6");
    clusters.getHost("h2").setState(HostState.HEALTHY);
    clusters.getHost("h2").persist();

    clusters.mapHostToCluster(host1, clusterName);
    clusters.mapHostToCluster(host2, clusterName);


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
      configs);
    cr2 = new ConfigurationRequest(clusterName, "hdfs-site","version1",
      configs);
    
    // create, but don't assign
    controller.createConfiguration(cr1);
    controller.createConfiguration(cr2);
    
    Map<String,String> configVersions = new HashMap<String,String>() {{
      put("core-site", "version1");
      put("hdfs-site", "version1");
    }};
    ServiceRequest sr = new ServiceRequest(clusterName, serviceName, configVersions, null);
    controller.updateServices(Collections.singleton(sr), new HashMap<String,String>(), false, false);

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
    clusters.addHost(host1);
    clusters.getHost("h1").setState(HostState.HEALTHY);
    clusters.getHost("h1").persist();
    String host2 = "h2";
    clusters.addHost(host2);
    clusters.getHost("h2").setState(HostState.HEALTHY);
    clusters.getHost("h2").persist();

    clusters.getHost("h1").setOsType("centos5");
    clusters.getHost("h2").setOsType("centos6");
    clusters.mapHostToCluster(host1, clusterName);
    clusters.mapHostToCluster(host2, clusterName);

    Map<String, String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "Called from a test");

    // null service should work
    createServiceComponentHost(clusterName, null, componentName1,
      host1, null);
    createServiceComponentHost(clusterName, null, componentName1,
      host2, null);



    ServiceRequest r = new ServiceRequest(clusterName, serviceName, null,
      State.INSTALLED.toString());
    Set<ServiceRequest> requests = new HashSet<ServiceRequest>();
    requests.add(r);

    RequestStatusResponse trackAction =
      controller.updateServices(requests, mapRequestProps, true, false);
    Assert.assertEquals(State.INSTALLED,
      clusters.getCluster(clusterName).getService(serviceName)
        .getDesiredState());

    List<Stage> stages = actionDB.getAllStages(trackAction.getRequestId());
    Map<String, String> params = stages.get(0).getOrderedHostRoleCommands().get
      (0).getExecutionCommandWrapper().getExecutionCommand()
      .getHostLevelParams();
    Assert.assertEquals("0.1", params.get("stack_version"));
    Assert.assertNotNull(params.get("jdk_location"));
    Assert.assertNotNull(params.get("repo_info"));
    Assert.assertNotNull(params.get("db_name"));
    Assert.assertNotNull(params.get("mysql_jdbc_url"));
    Assert.assertNotNull(params.get("oracle_jdbc_url"));
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
      Assert.assertTrue(e instanceof StackAccessException);
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
      Assert.assertTrue(e instanceof StackAccessException);
    }
  }

  @Test
  public void testGetStackVersionActiveAttr() throws Exception {

    for (StackInfo stackInfo: ambariMetaInfo.getStackInfos(STACK_NAME)) {
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
      Assert.assertTrue(e instanceof StackAccessException);
    }
  }


  @Test
  public void testGetStackServices() throws Exception {


    StackServiceRequest request = new StackServiceRequest(STACK_NAME, STACK_VERSION, null);
    Set<StackServiceResponse> responses = controller.getStackServices(Collections.singleton(request));
    Assert.assertEquals(STACK_SERVICES_CNT, responses.size());


    StackServiceRequest requestWithParams = new StackServiceRequest(STACK_NAME, STACK_VERSION, SERVICE_NAME);
    Set<StackServiceResponse> responsesWithParams = controller.getStackServices(Collections.singleton(requestWithParams));
    Assert.assertEquals(1, responsesWithParams.size());
    for (StackServiceResponse responseWithParams: responsesWithParams) {
      Assert.assertEquals(responseWithParams.getServiceName(), SERVICE_NAME);

    }

    StackServiceRequest invalidRequest = new StackServiceRequest(STACK_NAME, STACK_VERSION, NON_EXT_VALUE);
    try {
      controller.getStackServices(Collections.singleton(invalidRequest));
    } catch (StackAccessException e) {
      Assert.assertTrue(e instanceof StackAccessException);
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
      Assert.assertTrue(e instanceof StackAccessException);
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
      Assert.assertTrue(e instanceof StackAccessException);
    }


  }

  @Test
  public void testGetStackOperatingSystems() throws Exception {


    OperatingSystemRequest request = new OperatingSystemRequest(STACK_NAME, STACK_VERSION, null);
    Set<OperatingSystemResponse> responses = controller.getStackOperatingSystems(Collections.singleton(request));
    Assert.assertEquals(OS_CNT, responses.size());


    OperatingSystemRequest requestWithParams = new OperatingSystemRequest(STACK_NAME, STACK_VERSION, OS_TYPE);
    Set<OperatingSystemResponse> responsesWithParams = controller.getStackOperatingSystems(Collections.singleton(requestWithParams));
    Assert.assertEquals(1, responsesWithParams.size());
    for (OperatingSystemResponse responseWithParams: responsesWithParams) {
      Assert.assertEquals(responseWithParams.getOsType(), OS_TYPE);

    }

    OperatingSystemRequest invalidRequest = new OperatingSystemRequest(STACK_NAME, STACK_VERSION, NON_EXT_VALUE);
    try {
      controller.getStackOperatingSystems(Collections.singleton(invalidRequest));
    } catch (StackAccessException e) {
      Assert.assertTrue(e instanceof StackAccessException);
    }
  }

  @Test
  public void testServerActionForUpgradeFinalization() throws AmbariException {
    String clusterName = "foo1";
    StackId currentStackId = new StackId("HDP-0.1");
    StackId newStackId = new StackId("HDP-0.2");

    createCluster(clusterName);
    Cluster c = clusters.getCluster(clusterName);
    c.setDesiredStackVersion(currentStackId);
    Assert.assertTrue(c.getCurrentStackVersion().equals(currentStackId));

    ServerActionManager serverActionManager = new ServerActionManagerImpl(clusters);
    Map<String, String> payload = new HashMap<String, String>();
    payload.put(ServerAction.PayloadName.CLUSTER_NAME, clusterName);
    payload.put(ServerAction.PayloadName.CURRENT_STACK_VERSION, newStackId.getStackId());
    serverActionManager.executeAction(ServerAction.Command.FINALIZE_UPGRADE, payload);
    Assert.assertTrue(c.getCurrentStackVersion().equals(newStackId));
  }

  //@Test - disabled as upgrade feature is disabled
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

    clusters.addHost(host1);
    clusters.getHost(host1).persist();
    clusters.addHost(host2);
    clusters.getHost(host2).persist();

    clusters.getHost(host1).setOsType("centos5");
    clusters.getHost(host2).setOsType("centos6");
    clusters.getHost(host1).setState(HostState.HEALTHY);
    clusters.getHost(host2).setState(HostState.HEALTHY);

    clusters.mapHostToCluster(host1, clusterName);
    clusters.mapHostToCluster(host2, clusterName);

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

    StackId unsupportedStackId = new StackId("HDP-0.0.1");
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

  //@Test - disabled as cluster upgrade feature is disabled
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

    clusters.addHost(host1);
    clusters.getHost(host1).setState(HostState.HEALTHY);
    clusters.getHost(host1).persist();
    clusters.addHost(host2);
    clusters.getHost(host2).setState(HostState.HEALTHY);
    clusters.getHost(host2).persist();

    clusters.getHost(host1).setOsType("centos5");
    clusters.getHost(host2).setOsType("centos6");
    clusters.mapHostToCluster(host1, clusterName);
    clusters.mapHostToCluster(host2, clusterName);

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
              execCommand.getCommandParams().containsKey(ServerAction.PayloadName.CURRENT_STACK_VERSION));
          Assert.assertTrue(
              execCommand.getCommandParams().containsKey(ServerAction.PayloadName.CLUSTER_NAME));
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

  private void resetCluster(Cluster cluster, StackId currentStackId) {
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
      this.roleToIndex.put(Role.NAMENODE, 0);
      this.roleToIndex.put(Role.SECONDARY_NAMENODE, 1);
      this.roleToIndex.put(Role.DATANODE, 2);
      this.roleToIndex.put(Role.HDFS_CLIENT, 3);
      this.roleToIndex.put(Role.JOBTRACKER, 4);
      this.roleToIndex.put(Role.TASKTRACKER, 5);
      this.roleToIndex.put(Role.MAPREDUCE_CLIENT, 6);
      this.roleToIndex.put(Role.ZOOKEEPER_SERVER, 7);
      this.roleToIndex.put(Role.ZOOKEEPER_CLIENT, 8);
      this.roleToIndex.put(Role.HBASE_MASTER, 9);

      this.roleToIndex.put(Role.HBASE_REGIONSERVER, 10);
      this.roleToIndex.put(Role.HBASE_CLIENT, 11);
      this.roleToIndex.put(Role.HIVE_SERVER, 12);
      this.roleToIndex.put(Role.HIVE_METASTORE, 13);
      this.roleToIndex.put(Role.HIVE_CLIENT, 14);
      this.roleToIndex.put(Role.HCAT, 15);
      this.roleToIndex.put(Role.OOZIE_SERVER, 16);
      this.roleToIndex.put(Role.OOZIE_CLIENT, 17);
      this.roleToIndex.put(Role.WEBHCAT_SERVER, 18);
      this.roleToIndex.put(Role.PIG, 19);

      this.roleToIndex.put(Role.SQOOP, 20);
      this.roleToIndex.put(Role.GANGLIA_SERVER, 21);
      this.roleToIndex.put(Role.GANGLIA_MONITOR, 22);
      this.roleToIndex.put(Role.NAGIOS_SERVER, 23);
      this.roleToIndex.put(Role.AMBARI_SERVER_ACTION, 24);
    }
  }

  @Test
  public void testServiceStopWhileStopping() throws AmbariException {
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
    clusters.addHost(host1);
    clusters.getHost("h1").setOsType("centos5");
    clusters.getHost("h1").persist();
    String host2 = "h2";
    clusters.addHost(host2);
    clusters.getHost("h2").setOsType("centos6");
    clusters.getHost("h2").persist();

    clusters.mapHostToCluster(host1, clusterName);
    clusters.mapHostToCluster(host2, clusterName);

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
    ServiceRequest r = new ServiceRequest(clusterName, serviceName, null,
      State.INSTALLED.toString());
    Set<ServiceRequest> requests = new HashSet<ServiceRequest>();
    requests.add(r);

    RequestStatusResponse trackAction =
      controller.updateServices(requests, mapRequestProps, true, false);
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
    r = new ServiceRequest(clusterName, serviceName, null,
      State.STARTED.toString());
    requests.clear();
    requests.add(r);
    trackAction = controller.updateServices(requests, mapRequestProps, true, false);

    // manually change live state to started as no running action manager
    for (ServiceComponent sc :
      clusters.getCluster(clusterName).getService(serviceName)
        .getServiceComponents().values()) {
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        if (!sch.getServiceComponentName().equals("HDFS_CLIENT"))
          sch.setState(State.STARTED);
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
              sch.getHostName(), null, State.INSTALLED.name());
          Set<ServiceComponentHostRequest> reqs1 = new
            HashSet<ServiceComponentHostRequest>();
          reqs1.add(r1);
          controller.updateHostComponents(reqs1, Collections.<String, String>emptyMap(), true);
          Assert.assertEquals(State.INSTALLED, sch.getDesiredState());
        }
      }
    }

    // Stop all services
    r = new ServiceRequest(clusterName, serviceName, null,
      State.INSTALLED.toString());
    requests.clear();
    requests.add(r);
    controller.updateServices(requests, mapRequestProps, true, false);

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
    final String clusterName = "c1";
    final String hostName1 = "h1";
    final String context = "Test invocation";

    clusters.addCluster(clusterName);
    clusters.getCluster(clusterName).setDesiredStackVersion(new StackId("HDP-0.1"));
    clusters.addHost(hostName1);
    clusters.getHost("h1").setOsType("centos5");
    clusters.getHost(hostName1).persist();

    clusters.mapHostsToCluster(new HashSet<String>(){
      {add(hostName1);}}, clusterName);


    List<Stage> stages = new ArrayList<Stage>();
    stages.add(new Stage(requestId1, "/a1", clusterName, context));
    stages.get(0).setStageId(1);
    stages.get(0).addHostRoleExecutionCommand(hostName1, Role.HBASE_MASTER,
            RoleCommand.START,
            new ServiceComponentHostStartEvent(Role.HBASE_MASTER.toString(),
                    hostName1, System.currentTimeMillis(),
                    new HashMap<String, String>()),
            clusterName, "HBASE");

    stages.add(new Stage(requestId1, "/a2", clusterName, context));
    stages.get(1).setStageId(2);
    stages.get(1).addHostRoleExecutionCommand(hostName1, Role.HBASE_CLIENT,
            RoleCommand.START,
            new ServiceComponentHostStartEvent(Role.HBASE_CLIENT.toString(),
                    hostName1, System.currentTimeMillis(),
                    new HashMap<String, String>()), clusterName, "HBASE");

    stages.add(new Stage(requestId1, "/a3", clusterName, context));
    stages.get(2).setStageId(3);
    stages.get(2).addHostRoleExecutionCommand(hostName1, Role.HBASE_CLIENT,
            RoleCommand.START,
            new ServiceComponentHostStartEvent(Role.HBASE_CLIENT.toString(),
                    hostName1, System.currentTimeMillis(),
                    new HashMap<String, String>()), clusterName, "HBASE");


    stages.add(new Stage(requestId2, "/a4", clusterName, context));
    stages.get(3).setStageId(4);
    stages.get(3).addHostRoleExecutionCommand(hostName1, Role.HBASE_CLIENT,
            RoleCommand.START,
            new ServiceComponentHostStartEvent(Role.HBASE_CLIENT.toString(),
                    hostName1, System.currentTimeMillis(),
                    new HashMap<String, String>()), clusterName, "HBASE");

    stages.add(new Stage(requestId2, "/a5", clusterName, context));
    stages.get(4).setStageId(5);
    stages.get(4).addHostRoleExecutionCommand(hostName1, Role.HBASE_CLIENT,
            RoleCommand.START,
            new ServiceComponentHostStartEvent(Role.HBASE_CLIENT.toString(),
                    hostName1, System.currentTimeMillis(),
                    new HashMap<String, String>()), clusterName, "HBASE");

    actionDB.persistActions(stages);

    Set<TaskStatusRequest> taskStatusRequests;
    Set<TaskStatusResponse> taskStatusResponses;

    //check count of tasks by requestId1
    taskStatusRequests = new HashSet<TaskStatusRequest>(){
      {
        add(new TaskStatusRequest(requestId1, null));
      }
    };
    taskStatusResponses = controller.getTaskStatus(taskStatusRequests);
    assertEquals(3, taskStatusResponses.size());

    //check a taskId that requested by requestId1 and task id
    taskStatusRequests = new HashSet<TaskStatusRequest>(){
      {
        add(new TaskStatusRequest(requestId1, 2L));
      }
    };
    taskStatusResponses = controller.getTaskStatus(taskStatusRequests);
    assertEquals(1, taskStatusResponses.size());
    assertEquals(2L, taskStatusResponses.iterator().next().getTaskId());

    //check count of tasks by requestId2
    taskStatusRequests = new HashSet<TaskStatusRequest>(){
      {
        add(new TaskStatusRequest(requestId2, null));
      }
    };
    taskStatusResponses = controller.getTaskStatus(taskStatusRequests);
    assertEquals(2, taskStatusResponses.size());

    //check a taskId that requested by requestId2 and task id
    taskStatusRequests = new HashSet<TaskStatusRequest>(){
      {
        add(new TaskStatusRequest(requestId2, 5L));
      }
    };
    taskStatusResponses = controller.getTaskStatus(taskStatusRequests);
    assertEquals(5L, taskStatusResponses.iterator().next().getTaskId());

    //verify that task from second request (requestId2) does not present in first request (requestId1)
    taskStatusRequests = new HashSet<TaskStatusRequest>(){
      {
        add(new TaskStatusRequest(requestId1, 5L));
      }
    };
    expectedException.expect(ObjectNotFoundException.class);
    expectedException.expectMessage("Task resource doesn't exist.");
    controller.getTaskStatus(taskStatusRequests);

    //verify that task from first request (requestId1) does not present in second request (requestId2)
    taskStatusRequests = new HashSet<TaskStatusRequest>(){
      {
        add(new TaskStatusRequest(requestId2, 2L));
      }
    };
    expectedException.expect(ObjectNotFoundException.class);
    expectedException.expectMessage("Task resource doesn't exist.");
    controller.getTaskStatus(taskStatusRequests);
  }

  @Test
  public void testUpdateHostComponentsBadState() throws AmbariException {
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
    clusters.addHost(host1);
    clusters.getHost("h1").setOsType("centos5");
    clusters.getHost("h1").persist();
    String host2 = "h2";
    clusters.addHost(host2);
    clusters.getHost("h2").setOsType("centos6");
    clusters.getHost("h2").persist();

    clusters.mapHostToCluster(host1, clusterName);
    clusters.mapHostToCluster(host2, clusterName);

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
    ServiceRequest r = new ServiceRequest(clusterName, serviceName, null,
      State.INSTALLED.toString());
    Set<ServiceRequest> requests = new HashSet<ServiceRequest>();
    requests.add(r);

    RequestStatusResponse trackAction =
      controller.updateServices(requests, mapRequestProps, true, false);
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
    ServiceComponentHostRequest schr = new ServiceComponentHostRequest(clusterName, "HDFS", "DATANODE", host2, new HashMap<String,String>(), "INSTALLED");
    controller.updateHostComponents(Collections.singleton(schr), new HashMap<String,String>(), false);

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

    Map<String, String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "Called from a test");

    createServiceComponent(clusterName, serviceName1, componentName1, State.INIT);
    createServiceComponent(clusterName, serviceName1, componentName2, State.INIT);
    createServiceComponent(clusterName, serviceName1, componentName3, State.INIT);
    String host1 = "h1";
    clusters.addHost(host1);
    clusters.getHost("h1").setOsType("centos5");
    clusters.getHost("h1").persist();
    clusters.mapHostToCluster(host1, clusterName);

    Set<ServiceComponentHostRequest> set1 =  new HashSet<ServiceComponentHostRequest>();
    ServiceComponentHostRequest r1 = new ServiceComponentHostRequest(clusterName, serviceName1,
        componentName1, host1, null, State.INIT.toString());
    ServiceComponentHostRequest r2 = new ServiceComponentHostRequest(clusterName, serviceName1,
        componentName2, host1, null, State.INIT.toString());
    ServiceComponentHostRequest r3 = new ServiceComponentHostRequest(clusterName, serviceName1,
        componentName3, host1, null, State.INIT.toString());

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
    ServiceRequest req = new ServiceRequest(clusterName, serviceName1, null,
        State.INSTALLED.toString());
    controller.updateServices(Collections.singleton(req), Collections.<String, String>emptyMap(), true, false);
  }

  @Test
  public void testUpdateStacks() throws Exception {

    StackInfo stackInfo = ambariMetaInfo.getStackInfo(STACK_NAME, STACK_VERSION);

    for (RepositoryInfo repositoryInfo: stackInfo.getRepositories()) {
      assertFalse(INCORRECT_BASE_URL.equals(repositoryInfo.getBaseUrl()));
      repositoryInfo.setBaseUrl(INCORRECT_BASE_URL);
      assertTrue(INCORRECT_BASE_URL.equals(repositoryInfo.getBaseUrl()));
    }

    controller.updateStacks();

    stackInfo = ambariMetaInfo.getStackInfo(STACK_NAME, STACK_VERSION);

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
    AmbariMetaInfo ami = new AmbariMetaInfo(new File("src/test/resources/stacks"), new File("target/version"));
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
    clusters.addHost(host1);
    clusters.getHost("h1").setOsType("centos5");
    clusters.getHost("h1").setState(HostState.HEALTHY);
    clusters.getHost("h1").persist();

    clusters.mapHostToCluster(host1, clusterName);

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
    schRequests.add(new ServiceComponentHostRequest(clusterName, serviceName, componentName1, host1, null, null));
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
    sc4.getServiceComponentHosts().values().iterator().next().setState(State.MAINTENANCE);
    ServiceComponent sc5 = s2.getServiceComponent(componentName5);
    sc5.getServiceComponentHosts().values().iterator().next().setState(State.INSTALLED);
    ServiceComponent sc6 = s2.getServiceComponent(componentName6);
    sc6.getServiceComponentHosts().values().iterator().next().setState(State.INIT);

    schRequests.clear();
    schRequests.add(new ServiceComponentHostRequest(clusterName, serviceName, componentName1, host1, null, null));
    schRequests.add(new ServiceComponentHostRequest(clusterName, serviceName, componentName2, host1, null, null));
    schRequests.add(new ServiceComponentHostRequest(clusterName, serviceName, componentName3, host1, null, null));
    schRequests.add(new ServiceComponentHostRequest(clusterName, mapred, componentName4, host1, null, null));
    schRequests.add(new ServiceComponentHostRequest(clusterName, mapred, componentName5, host1, null, null));
    schRequests.add(new ServiceComponentHostRequest(clusterName, mapred, componentName6, host1, null, null));
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

    Map<String, String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "Called from a test");

    createServiceComponent(clusterName, serviceName, componentName1, State.INIT);
    createServiceComponent(clusterName, serviceName, componentName2, State.INIT);
    createServiceComponent(clusterName, serviceName, componentName3, State.INIT);

    String host1 = "h1";
    clusters.addHost(host1);
    clusters.getHost("h1").setOsType("centos5");
    clusters.getHost("h1").persist();
    
    String host2 = "h2";
    clusters.addHost(host2);
    clusters.getHost("h2").setOsType("centos6");
    clusters.getHost("h2").persist();
    
    String host3 = "h3";

    clusters.mapHostToCluster(host1, clusterName);

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
    
    Set<HostRequest> requests = new HashSet<HostRequest>();
    // delete from cluster
    requests.clear();
    requests.add(new HostRequest(host1, clusterName, null));
    try {
      controller.deleteHosts(requests);
      fail("Expect failure deleting hosts when components exist.");
    } catch (Exception e) {
    }
    
    Set<ServiceComponentHostRequest> schRequests = new HashSet<ServiceComponentHostRequest>();
    // maintenance HC for non-clients
    schRequests.add(new ServiceComponentHostRequest(clusterName, serviceName, componentName1, host1, null, "MAINTENANCE"));
    schRequests.add(new ServiceComponentHostRequest(clusterName, serviceName, componentName2, host1, null, "MAINTENANCE"));
    controller.updateHostComponents(schRequests, new HashMap<String,String>(), false);

    // delete HC
    schRequests.clear();
    schRequests.add(new ServiceComponentHostRequest(clusterName, serviceName, componentName1, host1, null, null));
    schRequests.add(new ServiceComponentHostRequest(clusterName, serviceName, componentName2, host1, null, null));
    schRequests.add(new ServiceComponentHostRequest(clusterName, serviceName, componentName3, host1, null, null));
    controller.deleteHostComponents(schRequests);
    
    Assert.assertEquals(0, cluster.getServiceComponentHosts(host1).size());
    
    // delete, which should fail since it is part of a cluster
    requests.clear();
    requests.add(new HostRequest(host1, null, null));
    try {
      controller.deleteHosts(requests);
      fail("Expect failure when removing from host when it is part of a cluster.");
    } catch (Exception e) {
    }
    
    // delete host from cluster
    requests.clear();
    requests.add(new HostRequest(host1, clusterName, null));
    controller.deleteHosts(requests);

    // host is no longer part of the cluster
    Assert.assertFalse(clusters.getHostsForCluster(clusterName).containsKey(host1));
    Assert.assertFalse(clusters.getClustersForHost(host1).contains(cluster));

    // delete entirely
    requests.clear();
    requests.add(new HostRequest(host1, null, null));
    controller.deleteHosts(requests);

    // verify host does not exist
    try {
      clusters.getHost(host1);
      Assert.fail("Expected a HostNotFoundException.");
    } catch (HostNotFoundException e) {
      // expected
    }
    
    // remove host2
    requests.clear();
    requests.add(new HostRequest(host2, null, null));
    controller.deleteHosts(requests);
    
    // verify host does not exist
    try {
      clusters.getHost(host2);
      Assert.fail("Expected a HostNotFoundException.");
    } catch (HostNotFoundException e) {
      // expected
    }

    // try removing a host that never existed
    requests.clear();
    requests.add(new HostRequest(host3, null, null));
    try {
      controller.deleteHosts(requests);
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
      Assert.assertTrue(e instanceof ObjectNotFoundException);
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
      Assert.assertTrue(e instanceof ObjectNotFoundException);
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

    Map<String, String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "Called from a test");

    createServiceComponent(clusterName, serviceName, componentName1, State.INIT);
    createServiceComponent(clusterName, serviceName, componentName2, State.INIT);
    createServiceComponent(clusterName, serviceName, componentName3, State.INIT);

    String host1 = "h1";
    clusters.addHost(host1);
    clusters.getHost("h1").setOsType("centos5");
    clusters.getHost("h1").persist();
    
    clusters.mapHostToCluster(host1, clusterName);

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
    
    sch.handleEvent(new ServiceComponentHostStartEvent(sch.getServiceComponentName(), sch.getHostName(), System.currentTimeMillis(), new HashMap<String, String>()));
    sch.handleEvent(new ServiceComponentHostStartedEvent (sch.getServiceComponentName(), sch.getHostName(), System.currentTimeMillis()));
    
    Set<ServiceComponentHostRequest> schRequests = new HashSet<ServiceComponentHostRequest>();
    schRequests.add(new ServiceComponentHostRequest(clusterName, null, null, host1, null, null));
    try {
      controller.deleteHostComponents(schRequests);
      fail("Expected exception while deleting all host components.");
    } catch (AmbariException e) {
    }
    Assert.assertEquals(3, cluster.getServiceComponentHosts(host1).size());

    sch.handleEvent(new ServiceComponentHostStopEvent(sch.getServiceComponentName(), sch.getHostName(), System.currentTimeMillis()));
    sch.handleEvent(new ServiceComponentHostStoppedEvent (sch.getServiceComponentName(), sch.getHostName(), System.currentTimeMillis()));
    
    schRequests.clear();
    // maintenance HC, DN was already stopped
    schRequests.add(new ServiceComponentHostRequest(clusterName, serviceName, componentName1, host1, null, "MAINTENANCE"));
    controller.updateHostComponents(schRequests, new HashMap<String,String>(), false);

    // delete HC
    schRequests.clear();
    schRequests.add(new ServiceComponentHostRequest(clusterName, null, null, host1, null, null));
    controller.deleteHostComponents(schRequests);
    
    Assert.assertEquals(0, cluster.getServiceComponentHosts(host1).size());
  }

  @Test
  public void testExecutionCommandConfiguration() throws AmbariException {
    Map<String, Map<String, String>> config = new HashMap<String, Map<String, String>>();
    config.put("type1", new HashMap<String, String>());
    config.put("type3", new HashMap<String, String>());
    config.get("type3").put("name1", "neverchange");
    ExecutionCommandWrapper.applyCustomConfig(config, "type1", "name1", "value11", false);
    Assert.assertEquals("value11", config.get("type1").get("name1"));

    config.put("type1", new HashMap<String, String>());
    ExecutionCommandWrapper.applyCustomConfig(config, "type1", "name1", "value12", false);
    Assert.assertEquals("value12", config.get("type1").get("name1"));

    ExecutionCommandWrapper.applyCustomConfig(config, "type2", "name2", "value21", false);
    Assert.assertEquals("value21", config.get("type2").get("name2"));

    ExecutionCommandWrapper.applyCustomConfig(config, "type2", "name2", "", true);
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
    Map<String, String> mergedConfig = ExecutionCommandWrapper.getMergedConfig(persistedClusterConfig,
        override);
    Assert.assertEquals(3, mergedConfig.size());
    Assert.assertFalse(mergedConfig.containsKey("name3"));
    Assert.assertEquals("value12", mergedConfig.get("name1"));
    Assert.assertEquals("value21", mergedConfig.get("name2"));
    Assert.assertEquals("value41", mergedConfig.get("name4"));
  }
}

  