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

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.*;
import org.apache.ambari.server.controller.spi.*;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class JMXHostProviderTest {
  private Injector injector;
  private Clusters clusters;
  static AmbariManagementController controller;
  private static final String NAMENODE_PORT = "dfs.http.address";
  private static final String DATANODE_PORT = "dfs.datanode.http.address";

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    clusters = injector.getInstance(Clusters.class);
    controller = injector.getInstance(AmbariManagementController.class);
    AmbariMetaInfo ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    ambariMetaInfo.init();
  }

  @After
  public void teardown() {
    injector.getInstance(PersistService.class).stop();
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

  private void createHDFSServiceConfigs() throws AmbariException {
    String clusterName = "c1";
    ClusterRequest r = new ClusterRequest(null, clusterName, "HDP-0.1", null);
    controller.createCluster(r);
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
    clusters.getHost("h1").persist();
    String host2 = "h2";
    clusters.addHost(host2);
    clusters.getHost("h2").setOsType("centos6");
    clusters.getHost("h2").persist();
    clusters.mapHostToCluster(host1, clusterName);
    clusters.mapHostToCluster(host2, clusterName);

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

    // Create configs
    Map<String, String> configs = new HashMap<String, String>();
    configs.put(NAMENODE_PORT, "localhost:70070");
    configs.put(DATANODE_PORT, "localhost:70075");
    ConfigurationRequest cr = new ConfigurationRequest(clusterName,
      "hdfs-site", "version1", configs);
    controller.createConfiguration(cr);

    Map<String, String> configVersions = new HashMap<String, String>();
    Set<ServiceRequest> sReqs = new HashSet<ServiceRequest>();
    configVersions.put("hdfs-site", "version1");
    sReqs.add(new ServiceRequest(clusterName, serviceName, null));
    ServiceResourceProviderTest.updateServices(controller, sReqs, mapRequestProps, true, false);
  }

  private void createConfigs() throws AmbariException {
    String clusterName = "c1";
    ClusterRequest r = new ClusterRequest(null, clusterName, "HDP-0.1", null);
    controller.createCluster(r);
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
    clusters.addHost(host1);
    clusters.getHost("h1").setOsType("centos5");
    clusters.getHost("h1").persist();
    String host2 = "h2";
    clusters.addHost(host2);
    clusters.getHost("h2").setOsType("centos6");
    clusters.getHost("h2").persist();
    clusters.mapHostToCluster(host1, clusterName);
    clusters.mapHostToCluster(host2, clusterName);

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

    // Create configs
    Map<String, String> configs = new HashMap<String, String>();
    configs.put(NAMENODE_PORT, "localhost:70070");
    configs.put(DATANODE_PORT, "localhost:70075");

    ConfigurationRequest cr1 = new ConfigurationRequest(clusterName,
      "hdfs-site", "versionN", configs);

    ClusterRequest crReq = new ClusterRequest(null, clusterName, null, null);
    crReq.setDesiredConfig(cr1);
    controller.updateClusters(Collections.singleton(crReq), null);
    Cluster cluster = clusters.getCluster(clusterName);
    Assert.assertEquals("versionN", cluster.getDesiredConfigByType("hdfs-site")
      .getVersionTag());
    Assert.assertEquals("localhost:70070", cluster.getDesiredConfigByType
      ("hdfs-site").getProperties().get(NAMENODE_PORT));
  }


  @Test
  public void testJMXPortMapInitAtServiceLevel() throws
    NoSuchParentResourceException,
    ResourceAlreadyExistsException, UnsupportedPropertyException,
    SystemException, AmbariException, NoSuchResourceException {

    createHDFSServiceConfigs();

    JMXHostProviderModule providerModule = new JMXHostProviderModule();
    providerModule.registerResourceProvider(Resource.Type.Service);
    providerModule.registerResourceProvider(Resource.Type.Configuration);
    // Non default port addresses
    Assert.assertEquals("70070", providerModule.getPort("c1", "NAMENODE"));
    Assert.assertEquals("70075", providerModule.getPort("c1", "DATANODE"));
    // Default port addresses
    Assert.assertEquals(null, providerModule.getPort("c1", "JOBTRACKER"));
    Assert.assertEquals(null, providerModule.getPort("c1", "TASKTRACKER"));
    Assert.assertEquals(null, providerModule.getPort("c1", "HBASE_MASTER"));
  }

  @Test
  public void testJMXPortMapInitAtClusterLevel() throws
    NoSuchParentResourceException,
    ResourceAlreadyExistsException, UnsupportedPropertyException,
    SystemException, AmbariException, NoSuchResourceException {

    createConfigs();

    JMXHostProviderModule providerModule = new JMXHostProviderModule();
    providerModule.registerResourceProvider(Resource.Type.Cluster);
    providerModule.registerResourceProvider(Resource.Type.Configuration);
    // Non default port addresses
    Assert.assertEquals("70070", providerModule.getPort("c1", "NAMENODE"));
    Assert.assertEquals("70075", providerModule.getPort("c1", "DATANODE"));
    // Default port addresses
    Assert.assertEquals(null, providerModule.getPort("c1", "JOBTRACKER"));
    Assert.assertEquals(null, providerModule.getPort("c1", "TASKTRACKER"));
    Assert.assertEquals(null, providerModule.getPort("c1", "HBASE_MASTER"));
  }

  private static class JMXHostProviderModule extends
    AbstractProviderModule {

    ResourceProvider clusterResourceProvider = new
      ClusterResourceProvider(PropertyHelper.getPropertyIds(Resource.Type
      .Cluster), PropertyHelper.getKeyPropertyIds(Resource.Type.Cluster),
      controller);

    ResourceProvider serviceResourceProvider = new ServiceResourceProvider(PropertyHelper
      .getPropertyIds(Resource.Type.Service),
      PropertyHelper.getKeyPropertyIds(Resource.Type.Service), controller);

    ResourceProvider hostCompResourceProvider = new
      HostComponentResourceProvider(PropertyHelper.getPropertyIds(Resource
      .Type.HostComponent), PropertyHelper.getKeyPropertyIds(Resource.Type
      .HostComponent), controller);

    ResourceProvider configResourceProvider = new
      ConfigurationResourceProvider(PropertyHelper.getPropertyIds(Resource
      .Type.Configuration), PropertyHelper.getKeyPropertyIds(Resource.Type
      .Configuration), controller);

    @Override
    protected ResourceProvider createResourceProvider(Resource.Type type) {
      if (type == Resource.Type.Cluster)
        return clusterResourceProvider;
      if (type == Resource.Type.Service)
        return serviceResourceProvider;
      else if (type == Resource.Type.HostComponent)
        return hostCompResourceProvider;
      else if (type == Resource.Type.Configuration)
        return configResourceProvider;
      return null;
    }
    
    @Override
    public String getJMXProtocol(String clusterName, String componentName) {
      return "http";
    }
    
  }
}
