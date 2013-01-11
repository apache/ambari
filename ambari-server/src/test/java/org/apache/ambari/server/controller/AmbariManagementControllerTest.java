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
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.apache.ambari.server.*;
import org.apache.ambari.server.actionmanager.*;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.RoleDAO;
import org.apache.ambari.server.orm.entities.RoleEntity;
import org.apache.ambari.server.security.authorization.Users;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigImpl;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentFactory;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStartEvent;
import org.apache.ambari.server.utils.StageUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

public class AmbariManagementControllerTest {

  private static final Logger LOG =
      LoggerFactory.getLogger(AmbariManagementControllerTest.class);

  private AmbariManagementController controller;
  private Clusters clusters;
  private ActionDBAccessor actionDB;
  private Injector injector;
  private ServiceFactory serviceFactory;
  private ServiceComponentFactory serviceComponentFactory;
  private ServiceComponentHostFactory serviceComponentHostFactory;
  private AmbariMetaInfo ambariMetaInfo;
  private Users users;

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
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
      valid1 = new ServiceRequest("foo", "bar", null, null);
      valid2 = new ServiceRequest("foo", "MAPREDUCE", null, null);
      set1.add(valid1);
      set1.add(valid2);
      controller.createServices(set1);
      fail("Expected failure for invalid services");
    } catch (IllegalArgumentException e) {
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
    clusters.getHost("h1").persist();
    String host2 = "h2";
    clusters.addHost(host2);
    clusters.getHost("h2").setOsType("centos5");
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

    c1.setVersionTag("v1");
    c2.setVersionTag("v1");
    c3.setVersionTag("v1");

    cluster.addDesiredConfig(c1);
    cluster.addDesiredConfig(c2);
    cluster.addDesiredConfig(c3);
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
        controller.updateServices(requests);
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
    Assert.assertEquals(5, statusResponse.getTasks().size());

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
    trackAction = controller.updateServices(requests);

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

    int counter = 0;
    for (ServiceComponent sc :
      clusters.getCluster(clusterName).getService(serviceName)
      .getServiceComponents().values()) {
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        if (sc.isClientComponent()) {
          sch.setState(State.INSTALLED);
        } else {
          ++counter;
          switch (counter%1) {
            case 0:
              sch.setState(State.START_FAILED);
              break;
            case 1:
              sch.setState(State.STOP_FAILED);
              break;
            case 2:
              sch.setState(State.STARTED);
              break;
          }
        }
      }
    }

    r = new ServiceRequest(clusterName, serviceName, null,
        State.INSTALLED.toString());
    requests.clear();
    requests.add(r);
    trackAction = controller.updateServices(requests);

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
      resp = controller.getServices(Collections.singleton(r));
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
      controller.updateServices(reqs);
      fail("Expected fail for invalid state transition");
    } catch (Exception e) {
      // Expected
    }

    r = new ServiceRequest(clusterName, serviceName,
        null, State.INSTALLED.toString());
    reqs.clear();
    reqs.add(r);
    RequestStatusResponse trackAction = controller.updateServices(reqs);
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
      controller.updateServices(reqs);
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
      controller.updateServices(reqs);
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
      controller.updateServices(reqs);
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
      controller.updateServices(reqs);
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
      controller.updateServices(reqs);
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
    sch2.setState(State.START_FAILED);
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
    RequestStatusResponse trackAction = controller.updateServices(reqs);

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
    Assert.assertEquals(State.START_FAILED, sch2.getState());
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
    trackAction = controller.updateServices(reqs);
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
    sch5.setState(State.INSTALLED);

    Set<ServiceComponentRequest> reqs =
        new HashSet<ServiceComponentRequest>();
    ServiceComponentRequest req1, req2, req3;
    try {
      reqs.clear();
      req1 = new ServiceComponentRequest(clusterName, serviceName1,
          sc1.getName(), null, State.INIT.toString());
      reqs.add(req1);
      controller.updateComponents(reqs);
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
      controller.updateComponents(reqs);
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
    sch4.setState(State.STOP_FAILED);
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
    RequestStatusResponse trackAction = controller.updateComponents(reqs);

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
    Assert.assertEquals(State.STOP_FAILED, sch4.getState());
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
    trackAction = controller.updateComponents(reqs);
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
      controller.updateHostComponents(reqs);
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
      controller.updateHostComponents(reqs);
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
    RequestStatusResponse trackAction = controller.updateHostComponents(reqs);
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
    trackAction = controller.updateHostComponents(reqs);
    Assert.assertNull(trackAction);
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
    stages.add(new Stage(requestId, "/a1", clusterName));
    stages.get(0).setStageId(stageId++);
    stages.get(0).addHostRoleExecutionCommand(hostName1, Role.HBASE_MASTER,
        RoleCommand.START,
        new ServiceComponentHostStartEvent(Role.HBASE_MASTER.toString(),
            hostName1, System.currentTimeMillis(),
            new HashMap<String, String>()),
            clusterName, "HBASE");
    stages.add(new Stage(requestId, "/a2", clusterName));
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
    assertEquals(2, requestStatusResponse.getTasks().size());

    ShortTaskStatus task1 = requestStatusResponse.getTasks().get(0);
    ShortTaskStatus task2 = requestStatusResponse.getTasks().get(1);

    assertEquals(RoleCommand.START.toString(), task1.getCommand());
    assertEquals(Role.HBASE_MASTER.toString(), task1.getRole());

    Set<TaskStatusRequest> taskStatusRequests = new HashSet<TaskStatusRequest>();
    taskStatusRequests.add(new TaskStatusRequest(requestId, task1.getTaskId()));
    taskStatusRequests.add(new TaskStatusRequest(requestId, task2.getTaskId()));
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

    RequestStatusResponse response = controller.createActions(actionRequests);
    assertEquals(1, response.getTasks().size());
    ShortTaskStatus task = response.getTasks().get(0);

    List<HostRoleCommand> storedTasks = actionDB.getRequestTasks(response.getRequestId());
    assertEquals(1, storedTasks.size());
    HostRoleCommand hostRoleCommand = storedTasks.get(0);

    assertEquals(task.getTaskId(), hostRoleCommand.getTaskId());
    assertEquals(actionRequest.getServiceName(), hostRoleCommand.getExecutionCommandWrapper().getExecutionCommand().getServiceName());
    assertEquals(actionRequest.getClusterName(), hostRoleCommand.getExecutionCommandWrapper().getExecutionCommand().getClusterName());
    assertEquals(actionRequest.getActionName(), hostRoleCommand.getExecutionCommandWrapper().getExecutionCommand().getRole().name());
    assertEquals(Role.HDFS_CLIENT.name(), hostRoleCommand.getEvent().getEvent().getServiceComponentName());
    assertEquals(actionRequest.getParameters(), hostRoleCommand.getExecutionCommandWrapper().getExecutionCommand().getRoleParams());

    actionRequests.add(new ActionRequest("c1", "MAPREDUCE", Role.MAPREDUCE_SERVICE_CHECK.name(), null));

    response = controller.createActions(actionRequests);

    assertEquals(2, response.getTasks().size());

    List<HostRoleCommand> tasks = actionDB.getRequestTasks(response.getRequestId());

    assertEquals(2, tasks.size());
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
    
    ConfigurationRequest cr1 = new ConfigurationRequest(clusterName, "global",
        "v1", configs);
    controller.createConfiguration(cr1);
    
    Set<ServiceRequest> sReqs = new HashSet<ServiceRequest>();
    Map<String, String> configVersions = new HashMap<String, String>();
    configVersions.put("global", "v1");
    sReqs.clear();
    sReqs.add(new ServiceRequest(clusterName, serviceName, configVersions,
        "INSTALLED"));
    RequestStatusResponse trackAction = controller.updateServices(sReqs);
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
    Assert.assertNull(controller.updateHostComponents(schReqs));

    Assert.assertEquals(0, s.getDesiredConfigs().size());
    Assert.assertEquals(0, sc1.getDesiredConfigs().size());
    Assert.assertEquals(3, sch1.getDesiredConfigs().size());

    configVersions.clear();
    configVersions.put("typeC", "v1");
    configVersions.put("typeD", "v1");
    scReqs.clear();
    scReqs.add(new ServiceComponentRequest(clusterName, serviceName,
        componentName2, configVersions, null));
    Assert.assertNull(controller.updateComponents(scReqs));

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
    Assert.assertNull(controller.updateServices(sReqs));

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
    Assert.assertNull(controller.updateHostComponents(schReqs));

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
    Assert.assertNull(controller.updateComponents(scReqs));

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
    clusters.getHost("h1").persist();
    String host2 = "h2";
    clusters.addHost(host2);
    clusters.getHost("h2").persist();

    clusters.getHost("h1").setOsType("centos5");
    clusters.getHost("h2").setOsType("centos6");
    clusters.mapHostToCluster(host1, clusterName);
    clusters.mapHostToCluster(host2, clusterName);


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
        controller.updateServices(requests);
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

    trackAction = controller.updateServices(requests);
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

}
