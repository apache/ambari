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

import static org.junit.Assert.fail;

import java.util.*;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;
import junit.framework.Assert;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.ActionDBAccessor;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.utils.StageUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AmbariManagementControllerTest {

  private static final Logger LOG =
      LoggerFactory.getLogger(AmbariManagementControllerTest.class);

  private AmbariManagementController controller;
  private Clusters clusters;
  private ActionDBAccessor db;
  private Injector injector;

  @Before
  public void setup() {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    clusters = injector.getInstance(Clusters.class);
    db = injector.getInstance(ActionDBAccessor.class);
    controller = injector.getInstance(AmbariManagementController.class);
  }

  @After
  public void teardown() {
    injector.getInstance(PersistService.class).stop();
  }

  private void createCluster(String clusterName) throws AmbariException {
    ClusterRequest r = new ClusterRequest(null, clusterName, "1.0.0", null);
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

    try {
      controller.createComponents(requests);
      fail("Duplicate ServiceComponent creation should fail");
    } catch (Exception e) {
      // Expected
    }
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

    try {
      controller.createHostComponents(requests);
      fail("Duplicate ServiceComponentHost creation should fail");
    } catch (Exception e) {
      // Expected
    }
  }

  @Test
  public void testCreateClusterSimple() throws AmbariException {
    String clusterName = "foo1";
    createCluster(clusterName);
    Set<ClusterResponse> r =
        controller.getClusters(new ClusterRequest(null, clusterName, null, null));
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
  public void testCreateClusterWithHostMapping() {
    // TODO write test
    // check for invalid hosts
  }

  @Test
  public void testCreateClusterWithDesiredClusterConfigs() {
    // TODO implement after configs integration
  }

  @Test
  public void testCreateClusterWithInvalidRequest() {
    // TODO write test
  }

  @Test
  public void testCreateClusterWithExistingServices() {
    // TODO write test
  }

  @Test
  public void testCreateServicesSimple() throws AmbariException {
    String clusterName = "foo1";
    createCluster(clusterName);
    String serviceName = "HDFS";
    createService(clusterName, serviceName, State.INIT);
    Service s =
        clusters.getCluster(clusterName).getService(serviceName);
    Assert.assertNotNull(s);
    Assert.assertEquals(serviceName, s.getName());
    Assert.assertEquals(clusterName, s.getCluster().getClusterName());

    ServiceRequest req = new ServiceRequest(clusterName, serviceName,
        null, null);

    Set<ServiceResponse> r =
        controller.getServices(req);
    Assert.assertEquals(1, r.size());
    ServiceResponse resp = r.iterator().next();
    Assert.assertEquals(serviceName, resp.getServiceName());
    Assert.assertEquals(clusterName, resp.getClusterName());
    Assert.assertEquals(State.INIT.toString(),
        resp.getDesiredState());
    Assert.assertEquals("1.0.0", resp.getDesiredStackVersion());

    // TODO test resp.getConfigVersions()
  }

  @Test
  public void testCreateServicesWithInvalidRequest() {
    // TODO write test
    // invalid request
    // dups in requests
    // multi cluster updates
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
    Set<ServiceResponse> response = controller.getServices(r);
    Assert.assertEquals(2, response.size());

    for (ServiceResponse svc : response) {
      Assert.assertTrue(svc.getServiceName().equals(serviceName)
          || svc.getServiceName().equals(serviceName2));
      Assert.assertEquals("1.0.0", svc.getDesiredStackVersion());
      Assert.assertEquals(State.INIT.toString(), svc.getDesiredState());
    }
  }

  @Test
  public void testCreateServicesMultiple() {

  }

  @Test
  public void testCreateServiceComponent() throws AmbariException {
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
    Set<ServiceComponentResponse> response = controller.getComponents(r);
    Assert.assertEquals(1, response.size());

    ServiceComponentResponse sc = response.iterator().next();
    Assert.assertEquals(State.INIT.toString(), sc.getDesiredState());
    Assert.assertEquals(componentName, sc.getComponentName());
    Assert.assertEquals(clusterName, sc.getClusterName());
    Assert.assertEquals(serviceName, sc.getServiceName());
  }

  @Test
  public void testCreateServiceComponentHost() throws AmbariException {
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
    clusters.getHost("h1").persist();
    String host2 = "h2";
    clusters.addHost(host2);
    clusters.getHost("h2").persist();

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
      ServiceComponentHost sch =
      clusters.getCluster(clusterName).getService(serviceName)
          .getServiceComponent(componentName1).getServiceComponentHost(host1);
      LOG.error("**** " + sch.getHostName());
      fail("ServiceComponentHost creation should have failed");
    } catch (Exception e) {
      // Expected
    }

    createServiceComponentHost(clusterName, serviceName, componentName1,
        host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName2,
        host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName2,
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

    ServiceComponentHostRequest r =
        new ServiceComponentHostRequest(clusterName, serviceName,
            componentName2, null, null, null);

    Set<ServiceComponentHostResponse> response =
        controller.getHostComponents(r);
    Assert.assertEquals(2, response.size());

  }



  @Test
  public void testCreateHost() throws AmbariException {
    List<String> clusterNames = null;
    Map<String, String> hostAttributes = null;

    HostRequest r1 = new HostRequest("h1", clusterNames, hostAttributes);
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

    clusterNames = new ArrayList<String>();
    clusterNames.add("foo1");
    clusterNames.add("foo2");

    hostAttributes = new HashMap<String, String>();
    HostRequest r2 = new HostRequest("h2", clusterNames, hostAttributes);

    requests.add(r2);

    try {
      controller.createHosts(requests);
      fail("Create host should fail for invalid clusters");
    } catch (Exception e) {
      // Expected
    }

    clusters.addCluster("foo1");
    clusters.addCluster("foo2");

    controller.createHosts(requests);

    Assert.assertNotNull(clusters.getHost("h1"));
    Assert.assertNotNull(clusters.getHost("h2"));

    Assert.assertEquals(0, clusters.getClustersForHost("h1").size());
    Assert.assertEquals(2, clusters.getClustersForHost("h2").size());

  }

  @Test
  public void testGetCluster() throws AmbariException {

  }

  @Test
  public void testGetService() throws AmbariException {

  }

  @Test
  public void testGetServiceComponent() throws AmbariException {

  }

  @Test
  public void testGetServiceComponentHost() throws AmbariException {

  }

  @Test
  public void testGetHost() throws AmbariException {

  }

  @Test
  public void testInstallAndStartService() throws Exception {
    testCreateServiceComponentHost();

    String clusterName = "foo1";
    String serviceName = "HDFS";

    ServiceRequest r1 = new ServiceRequest(clusterName, serviceName, null,
        State.INSTALLED.toString());
    Set<ServiceRequest> requests1 = new HashSet<ServiceRequest>();
    requests1.add(r1);

    controller.updateServices(requests1);
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
    List<Stage> stages = db.getAllStages(1);
    Assert.assertEquals(1, stages.size());

    for (Stage stage : stages) {
      LOG.info("Stage Details for Install Service"
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

    ServiceRequest r2 = new ServiceRequest(clusterName, serviceName, null,
        State.STARTED.toString());
    Set<ServiceRequest> requests2 = new HashSet<ServiceRequest>();
    requests2.add(r2);
    controller.updateServices(requests2);

    Assert.assertEquals(State.STARTED,
        clusters.getCluster(clusterName).getService(serviceName)
        .getDesiredState());
    for (ServiceComponent sc :
      clusters.getCluster(clusterName).getService(serviceName)
      .getServiceComponents().values()) {
      Assert.assertEquals(State.STARTED, sc.getDesiredState());
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        Assert.assertEquals(State.STARTED, sch.getDesiredState());
      }
    }

    // TODO validate stages?
    stages = db.getAllStages(2);
    Assert.assertEquals(1, stages.size());

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

  }



}
