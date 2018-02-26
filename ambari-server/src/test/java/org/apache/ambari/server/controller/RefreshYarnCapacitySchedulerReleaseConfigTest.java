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

package org.apache.ambari.server.controller;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.controller.internal.ComponentResourceProviderTest;
import org.apache.ambari.server.controller.internal.ServiceGroupResourceProviderTest;
import org.apache.ambari.server.controller.internal.ServiceResourceProviderTest;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.Assert;

public class RefreshYarnCapacitySchedulerReleaseConfigTest {

  private Injector injector;
  private AmbariManagementController controller;
  private Clusters clusters;
  private ConfigHelper configHelper;
  private OrmTestHelper ormTestHelper;

  @Before
  public void setup() throws Exception {

    injector = Guice.createInjector(new InMemoryDefaultTestModule());

    injector.getInstance(GuiceJpaInitializer.class);
    controller = injector.getInstance(AmbariManagementController.class);
    clusters = injector.getInstance(Clusters.class);
    configHelper = injector.getInstance(ConfigHelper.class);
    ormTestHelper = injector.getInstance(OrmTestHelper.class);

    // Set the authenticated user
    // TODO: remove this or replace the authenticated user to test authorization rules
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());
  }

  @After
  public void teardown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);

    // Clear the authenticated user
    SecurityContextHolder.getContext().setAuthentication(null);
  }



  @Test
  public void testRMRequiresRestart() throws AmbariException, AuthorizationException {
    createClusterFixture("HDP-2.0.7");


    Cluster cluster = clusters.getCluster("c1");

    // Start
    ClusterRequest cr = new ClusterRequest(cluster.getClusterId(), "c1", cluster.getDesiredStackVersion().getStackVersion(), null);

    cr.setDesiredConfig(Collections.singletonList(new ConfigurationRequest("c1","capacity-scheduler","version2", new HashMap<>(), null, 1L, 1L)));

    controller.updateClusters(Collections.singleton(cr) , null);


    ServiceComponentHostRequest r = new ServiceComponentHostRequest("c1", null, null, null, null, null);
    r.setStaleConfig("true");
    Set<ServiceComponentHostResponse> resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(1, resps.size());

    Assert.assertEquals(true, configHelper.isStaleConfigs(clusters.getCluster("c1").getService("YARN").getServiceComponent("RESOURCEMANAGER").getServiceComponentHost("c6401"), null));
  }

  @Test
  public void testAllRequiresRestart() throws AmbariException, AuthorizationException {
    createClusterFixture("HDP-2.0.7");
    Cluster cluster = clusters.getCluster("c1");

    // Start
    ClusterRequest cr = new ClusterRequest(cluster.getClusterId(), "c1", cluster.getDesiredStackVersion().getStackVersion(), null);

    cr.setDesiredConfig(Collections.singletonList(new ConfigurationRequest("c1","core-site","version2", new HashMap<>(),null, 1L, 1L)));

    controller.updateClusters(Collections.singleton(cr) , null);


    ServiceComponentHostRequest r = new ServiceComponentHostRequest("c1", null, null, null, null, null);
    r.setStaleConfig("true");
    Set<ServiceComponentHostResponse> resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(4, resps.size());

  }

  @Test
  public void testConfigInComponent() throws Exception {
    StackServiceRequest requestWithParams = new StackServiceRequest("HDP", "2.0.6", "YARN");
    Set<StackServiceResponse> responsesWithParams = controller.getStackServices(Collections.singleton(requestWithParams));

    Assert.assertEquals(1, responsesWithParams.size());

    for (StackServiceResponse responseWithParams: responsesWithParams) {
      Assert.assertEquals(responseWithParams.getServiceName(), "YARN");
      Assert.assertTrue(responseWithParams.getConfigTypes().containsKey("capacity-scheduler"));
    }
  }

  @Test
  public void testConfigInComponentOverwrited() throws Exception {
    StackServiceRequest requestWithParams = new StackServiceRequest("HDP", "2.0.7", "YARN");
    Set<StackServiceResponse> responsesWithParams = controller.getStackServices(Collections.singleton(requestWithParams));

    Assert.assertEquals(1, responsesWithParams.size());

    for (StackServiceResponse responseWithParams: responsesWithParams) {
      Assert.assertEquals(responseWithParams.getServiceName(), "YARN");
      Assert.assertTrue(responseWithParams.getConfigTypes().containsKey("capacity-scheduler"));
    }
  }

  private void createClusterFixture(String stackName) throws AmbariException, AuthorizationException {
    String clusterName = "c1";
    createCluster(clusterName, stackName);
    addHost("c6401", clusterName);
    addHost("c6402", clusterName);

    String serviceGroupName = "CORE";
    ServiceGroupResourceProviderTest.createServiceGroup(controller, clusterName, serviceGroupName, stackName);

    String serviceName = "YARN";
    createService(clusterName, serviceGroupName, serviceName, null);

    createServiceComponent(clusterName, serviceGroupName, serviceName,"RESOURCEMANAGER", State.INIT);
    createServiceComponent(clusterName, serviceGroupName, serviceName,"NODEMANAGER", State.INIT);
    createServiceComponent(clusterName, serviceGroupName, serviceName,"YARN_CLIENT", State.INIT);

    createServiceComponentHost(clusterName, serviceGroupName, serviceName,"RESOURCEMANAGER","c6401", null);
    createServiceComponentHost(clusterName, serviceGroupName, serviceName,"NODEMANAGER","c6401", null);

    createServiceComponentHost(clusterName, serviceGroupName, serviceName,"NODEMANAGER","c6402", null);
    createServiceComponentHost(clusterName, serviceGroupName, serviceName,"YARN_CLIENT","c6402", null);
  }

  private void addHost(String hostname, String clusterName) throws AmbariException {
    clusters.addHost(hostname);
    setOsFamily(clusters.getHost(hostname), "redhat", "6.3");
    clusters.getHost(hostname).setState(HostState.HEALTHY);
    if (null != clusterName) {
      clusters.mapHostToCluster(hostname, clusterName);
    }
  }

  private void setOsFamily(Host host, String osFamily, String osVersion) {
    Map<String, String> hostAttributes = new HashMap<>();
    hostAttributes.put("os_family", osFamily);
    hostAttributes.put("os_release_version", osVersion);

    host.setHostAttributes(hostAttributes);
  }

  private void createCluster(String clusterName, String stackName) throws AmbariException, AuthorizationException {
    ClusterRequest r = new ClusterRequest(null, clusterName, State.INSTALLED.name(), SecurityType.NONE, stackName, null);
    controller.createCluster(r);
  }

  private void createService(String clusterName, String serviceGroupName,
      String serviceName, State desiredState) throws AmbariException, AuthorizationException {
    RepositoryVersionEntity repositoryVersion = ormTestHelper.getOrCreateRepositoryVersion(new StackId("HDP-2.0.7"), "2.0.7-1234");
    ServiceRequest request = new ServiceRequest(clusterName, serviceGroupName, serviceName, repositoryVersion.getId(), desiredState != null ? desiredState.toString() : null, null);
    ServiceResourceProviderTest.createServices(controller, injector.getInstance(RepositoryVersionDAO.class), Collections.singleton(request));
  }

  private void createServiceComponent(String clusterName, String serviceGroupName,
      String serviceName, String componentName, State desiredState)
      throws AmbariException, AuthorizationException {
    ServiceComponentRequest r = new ServiceComponentRequest(clusterName, serviceGroupName, serviceName, componentName, desiredState != null ? desiredState.name() : null);
    ComponentResourceProviderTest.createComponents(controller, Collections.singleton(r));
  }

  private void createServiceComponentHost(String clusterName, String serviceGroupName, String serviceName, String componentName, String hostname, State desiredState)
      throws AmbariException, AuthorizationException {
    ServiceComponentHostRequest r = new ServiceComponentHostRequest(clusterName, serviceGroupName, serviceName, componentName, hostname, desiredState != null ? desiredState.name() : null);
    controller.createHostComponents(Collections.singleton(r));

    //set actual config
      Service service = clusters.getCluster(clusterName).getService(serviceName);
      ServiceComponent rm = service.getServiceComponent(componentName);
      ServiceComponentHost rmc1 = rm.getServiceComponentHost(hostname);

      rmc1.updateActualConfigs((new HashMap<String, Map<String,String>>() {{
        put("capacity-scheduler", new HashMap<String,String>() {{ put("tag", "version1"); }});
        put("hive-group", new HashMap<String,String>() {{ put("tag", "version1"); }});
      }}));
  }
}
