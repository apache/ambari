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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ClusterRequest;
import org.apache.ambari.server.controller.ConfigurationRequest;
import org.apache.ambari.server.controller.MaintenanceStateHelper;
import org.apache.ambari.server.controller.ServiceComponentHostRequest;
import org.apache.ambari.server.controller.ServiceComponentRequest;
import org.apache.ambari.server.controller.ServiceRequest;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;
import org.springframework.security.core.context.SecurityContextHolder;

public class JMXHostProviderTest {
  private Injector injector;
  private Clusters clusters;
  static AmbariManagementController controller;
  private static final String NAMENODE_PORT_V1 = "dfs.http.address";
  private static final String NAMENODE_PORT_V2 = "dfs.namenode.http-address";
  private static final String DATANODE_PORT = "dfs.datanode.http.address";
  private static final String RESOURCEMANAGER_PORT = "yarn.resourcemanager.webapp.address";
  private static final String RESOURCEMANAGER_HTTPS_PORT = "yarn.resourcemanager.webapp.https.address";
  private static final String YARN_HTTPS_POLICY = "yarn.http.policy";
  private static final String NODEMANAGER_PORT = "yarn.nodemanager.webapp.address";
  private static final String JOURNALNODE_HTTPS_PORT = "dfs.journalnode.https-address";
  private static final String HDFS_HTTPS_POLICY = "dfs.http.policy";

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    clusters = injector.getInstance(Clusters.class);
    controller = injector.getInstance(AmbariManagementController.class);

    // Set the authenticated user
    // TODO: remove this or replace the authenticated user to test authorization rules
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());
  }

  @After
  public void teardown() {
    injector.getInstance(PersistService.class).stop();

    // Clear the authenticated user
    SecurityContextHolder.getContext().setAuthentication(null);
  }

  private void createService(String clusterName,
                             String serviceName, State desiredState)
      throws AmbariException, AuthorizationException {
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
      throws AmbariException, AuthorizationException {
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
                                          State desiredState) throws AmbariException, AuthorizationException {
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

  private void createHDFSServiceConfigs(boolean version1) throws AmbariException, AuthorizationException {
    String clusterName = "c1";
    ClusterRequest r = new ClusterRequest(null, clusterName, "HDP-0.1", null);
    controller.createCluster(r);
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
    clusters.addHost(host1);
    Map<String, String> hostAttributes = new HashMap<String, String>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "5.9");
    clusters.getHost("h1").setHostAttributes(hostAttributes);
    clusters.getHost("h1").persist();
    String host2 = "h2";
    clusters.addHost(host2);
    hostAttributes = new HashMap<String, String>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "6.3");
    clusters.getHost("h2").setHostAttributes(hostAttributes);
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
    if (version1) {
      Map<String, String> configs = new HashMap<String, String>();
      configs.put(NAMENODE_PORT_V1, "localhost:${ambari.dfs.datanode.http.port}");
      configs.put(DATANODE_PORT, "localhost:70075");
      configs.put("ambari.dfs.datanode.http.port", "70070");

      ConfigurationRequest cr = new ConfigurationRequest(clusterName,
        "hdfs-site", "version1", configs, null);
      ClusterRequest crequest = new ClusterRequest(cluster.getClusterId(), clusterName, null, null);
      crequest.setDesiredConfig(Collections.singletonList(cr));
      controller.updateClusters(Collections.singleton(crequest), new HashMap<String,String>());

    } else {
      Map<String, String> configs = new HashMap<String, String>();
      configs.put(NAMENODE_PORT_V2, "localhost:70071");
      configs.put(DATANODE_PORT, "localhost:70075");

      ConfigurationRequest cr = new ConfigurationRequest(clusterName,
        "hdfs-site", "version2", configs, null);

      ClusterRequest crequest = new ClusterRequest(cluster.getClusterId(), clusterName, null, null);
      crequest.setDesiredConfig(Collections.singletonList(cr));
      controller.updateClusters(Collections.singleton(crequest), new HashMap<String,String>());
    }
  }

  private void createConfigs() throws AmbariException, AuthorizationException {
    String clusterName = "c1";
    ClusterRequest r = new ClusterRequest(null, clusterName, "HDP-2.0.6", null);
    controller.createCluster(r);
    Cluster cluster = clusters.getCluster(clusterName);
    cluster.setDesiredStackVersion(new StackId("HDP-2.0.6"));
    String serviceName = "HDFS";
    String serviceName2 = "YARN";
    createService(clusterName, serviceName, null);
    createService(clusterName, serviceName2, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";
    String componentName4 = "RESOURCEMANAGER";
    String componentName5 = "JOURNALNODE";

    createServiceComponent(clusterName, serviceName, componentName1,
      State.INIT);
    createServiceComponent(clusterName, serviceName, componentName2,
      State.INIT);
    createServiceComponent(clusterName, serviceName, componentName3,
      State.INIT);
    createServiceComponent(clusterName, serviceName2, componentName4,
      State.INIT);
    createServiceComponent(clusterName, serviceName, componentName5,
        State.INIT);

    String host1 = "h1";
    clusters.addHost(host1);
    Map<String, String> hostAttributes = new HashMap<String, String>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "5.9");
    clusters.getHost("h1").setHostAttributes(hostAttributes);
    clusters.getHost("h1").persist();
    String host2 = "h2";
    clusters.addHost(host2);
    hostAttributes = new HashMap<String, String>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "6.3");
    clusters.getHost("h2").setHostAttributes(hostAttributes);
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
    createServiceComponentHost(clusterName, serviceName, componentName5,
      host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName5,
      host2, null);
    createServiceComponentHost(clusterName, serviceName, componentName3,
      host2, null);
    createServiceComponentHost(clusterName, serviceName2, componentName4,
      host2, null);

    // Create configs
    Map<String, String> configs = new HashMap<String, String>();
    configs.put(NAMENODE_PORT_V1, "localhost:${ambari.dfs.datanode.http.port}");
    configs.put(DATANODE_PORT, "localhost:70075");
    configs.put("ambari.dfs.datanode.http.port", "70070");
    configs.put(JOURNALNODE_HTTPS_PORT, "localhost:8481");
    configs.put(HDFS_HTTPS_POLICY, "HTTPS_ONLY");

    Map<String, String> yarnConfigs = new HashMap<String, String>();
    yarnConfigs.put(RESOURCEMANAGER_PORT, "8088");
    yarnConfigs.put(NODEMANAGER_PORT, "8042");
    yarnConfigs.put(RESOURCEMANAGER_HTTPS_PORT, "8090");
    yarnConfigs.put(YARN_HTTPS_POLICY, "HTTPS_ONLY");

    ConfigurationRequest cr1 = new ConfigurationRequest(clusterName,
      "hdfs-site", "versionN", configs, null);

    ClusterRequest crReq = new ClusterRequest(cluster.getClusterId(), clusterName, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr1));
    controller.updateClusters(Collections.singleton(crReq), null);
    cluster = clusters.getCluster(clusterName);
    Assert.assertEquals("versionN", cluster.getDesiredConfigByType("hdfs-site")
      .getTag());

    ConfigurationRequest cr2 = new ConfigurationRequest(clusterName,
      "yarn-site", "versionN", yarnConfigs, null);
    crReq.setDesiredConfig(Collections.singletonList(cr2));
    controller.updateClusters(Collections.singleton(crReq), null);

    Assert.assertEquals("versionN", cluster.getDesiredConfigByType("yarn-site")
      .getTag());
    Assert.assertEquals("localhost:${ambari.dfs.datanode.http.port}", cluster.getDesiredConfigByType
      ("hdfs-site").getProperties().get(NAMENODE_PORT_V1));
  }

  private void createConfigsNameNodeHa() throws AmbariException, AuthorizationException {
    String clusterName = "nnha";
    ClusterRequest r = new ClusterRequest(null, clusterName, "HDP-2.0.6", null);
    controller.createCluster(r);
    Cluster cluster = clusters.getCluster(clusterName);
    cluster.setDesiredStackVersion(new StackId("HDP-2.0.6"));
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
    Map<String, String> hostAttributes = new HashMap<String, String>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "5.9");
    clusters.getHost("h1").setHostAttributes(hostAttributes);
    clusters.getHost("h1").persist();
    String host2 = "h2";
    clusters.addHost(host2);
    hostAttributes = new HashMap<String, String>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "6.3");
    clusters.getHost("h2").setHostAttributes(hostAttributes);
    clusters.getHost("h2").persist();
    clusters.mapHostToCluster(host1, clusterName);
    clusters.mapHostToCluster(host2, clusterName);

    createServiceComponentHost(clusterName, serviceName, componentName1,
        host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName1,
        host2, null);
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
    configs.put("dfs.internal.nameservices", "ns");
    configs.put("dfs.namenode.http-address", "h1:50070");
    configs.put("dfs.namenode.http-address.ns.nn1", "h1:50071");
    configs.put("dfs.namenode.http-address.ns.nn2", "h2:50072");
    configs.put("dfs.namenode.https-address", "h1:50470");
    configs.put("dfs.namenode.https-address.ns.nn1", "h1:50473");
    configs.put("dfs.namenode.https-address.ns.nn2", "h2:50474");
    configs.put("dfs.ha.namenodes.ns", "nn1,nn2");


    ConfigurationRequest cr1 = new ConfigurationRequest(clusterName,
        "hdfs-site", "version1", configs, null);

    ClusterRequest crReq = new ClusterRequest(cluster.getClusterId(), clusterName, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr1));
    controller.updateClusters(Collections.singleton(crReq), null);
  }


  @Test
  public void testJMXPortMapInitAtServiceLevelVersion1() throws
    NoSuchParentResourceException,
    ResourceAlreadyExistsException, UnsupportedPropertyException,
    SystemException, AmbariException, NoSuchResourceException {

    createHDFSServiceConfigs(true);

    JMXHostProviderModule providerModule = new JMXHostProviderModule();
    providerModule.registerResourceProvider(Resource.Type.Service);
    providerModule.registerResourceProvider(Resource.Type.Configuration);
    // Non default port addresses
    Assert.assertEquals("70070", providerModule.getPort("c1", "NAMENODE", "localhost"));
    Assert.assertEquals("70075", providerModule.getPort("c1", "DATANODE", "localhost"));
    // Default port addresses
    Assert.assertEquals(null, providerModule.getPort("c1", "JOBTRACKER", "localhost"));
    Assert.assertEquals(null, providerModule.getPort("c1", "TASKTRACKER", "localhost"));
    Assert.assertEquals(null, providerModule.getPort("c1", "HBASE_MASTER", "localhost"));
  }

  @Test
  public void testJMXPortMapInitAtServiceLevelVersion2() throws
    NoSuchParentResourceException,
    ResourceAlreadyExistsException, UnsupportedPropertyException,
    SystemException, AmbariException, NoSuchResourceException {

    createHDFSServiceConfigs(false);

    JMXHostProviderModule providerModule = new JMXHostProviderModule();
    providerModule.registerResourceProvider(Resource.Type.Service);
    providerModule.registerResourceProvider(Resource.Type.Configuration);
    // Non default port addresses
    Assert.assertEquals("70071", providerModule.getPort("c1", "NAMENODE", "localhost"));
    Assert.assertEquals("70075", providerModule.getPort("c1", "DATANODE", "localhost"));
    // Default port addresses
    Assert.assertEquals(null, providerModule.getPort("c1", "JOBTRACKER", "localhost"));
    Assert.assertEquals(null, providerModule.getPort("c1", "TASKTRACKER", "localhost"));
    Assert.assertEquals(null, providerModule.getPort("c1", "HBASE_MASTER", "localhost"));
  }

  @Test
  public void testJMXPortMapNameNodeHa() throws
      NoSuchParentResourceException,
      ResourceAlreadyExistsException, UnsupportedPropertyException,
      SystemException, AmbariException, NoSuchResourceException {

    createConfigsNameNodeHa();

    JMXHostProviderModule providerModule = new JMXHostProviderModule();
    providerModule.registerResourceProvider(Resource.Type.Service);
    providerModule.registerResourceProvider(Resource.Type.Configuration);


    Assert.assertEquals("50071", providerModule.getPort("nnha", "NAMENODE", "h1"));
    Assert.assertEquals("50072", providerModule.getPort("nnha", "NAMENODE", "h2"));
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
    Assert.assertEquals("70070", providerModule.getPort("c1", "NAMENODE", "localhost"));
    Assert.assertEquals("70075", providerModule.getPort("c1", "DATANODE", "localhost"));
    // Default port addresses
    Assert.assertEquals(null, providerModule.getPort("c1", "JOBTRACKER", "localhost"));
    Assert.assertEquals(null, providerModule.getPort("c1", "TASKTRACKER", "localhost"));
    Assert.assertEquals(null, providerModule.getPort("c1", "HBASE_MASTER", "localhost"));
  }

  @Test
  public void testGetHostNames() throws AmbariException {
    JMXHostProviderModule providerModule = new JMXHostProviderModule();


    AmbariManagementController managementControllerMock = createNiceMock(AmbariManagementController.class);
    Clusters clustersMock = createNiceMock(Clusters.class);
    Cluster clusterMock = createNiceMock(Cluster.class);
    Service serviceMock = createNiceMock(Service.class);
    ServiceComponent serviceComponentMock = createNiceMock(ServiceComponent.class);

    Map<String, ServiceComponentHost> hostComponents = new HashMap<String, ServiceComponentHost>();
    hostComponents.put("host1", null);

    expect(managementControllerMock.getClusters()).andReturn(clustersMock).anyTimes();
    expect(managementControllerMock.findServiceName(clusterMock, "DATANODE")).andReturn("HDFS");
    expect(clustersMock.getCluster("c1")).andReturn(clusterMock).anyTimes();
    expect(clusterMock.getService("HDFS")).andReturn(serviceMock).anyTimes();
    expect(serviceMock.getServiceComponent("DATANODE")).andReturn(serviceComponentMock).anyTimes();
    expect(serviceComponentMock.getServiceComponentHosts()).andReturn(hostComponents).anyTimes();

    replay(managementControllerMock, clustersMock, clusterMock, serviceMock, serviceComponentMock);
    providerModule.managementController = managementControllerMock;

    Set<String> result = providerModule.getHostNames("c1", "DATANODE");
    Assert.assertTrue(result.iterator().next().equals("host1"));

  }

  @Test
  public void testJMXHttpsPort() throws
    NoSuchParentResourceException,
    ResourceAlreadyExistsException, UnsupportedPropertyException,
    SystemException, AmbariException, NoSuchResourceException {
    createConfigs();
    JMXHostProviderModule providerModule = new JMXHostProviderModule();
    providerModule.registerResourceProvider(Resource.Type.Cluster);
    providerModule.registerResourceProvider(Resource.Type.Configuration);
    Assert.assertEquals("https", providerModule.getJMXProtocol("c1", "RESOURCEMANAGER"));
    Assert.assertEquals("8090", providerModule.getPort("c1", "RESOURCEMANAGER", "localhost", true));

  }

  @Test
  public void testJMXJournalNodeHttpsPort() throws
    NoSuchParentResourceException,
    ResourceAlreadyExistsException, UnsupportedPropertyException,
    SystemException, AmbariException, NoSuchResourceException {
    createConfigs();
    JMXHostProviderModule providerModule = new JMXHostProviderModule();
    providerModule.registerResourceProvider(Resource.Type.Cluster);
    providerModule.registerResourceProvider(Resource.Type.Configuration);
    Assert.assertEquals("https", providerModule.getJMXProtocol("c1", "JOURNALNODE"));
    Assert.assertEquals("8481", providerModule.getPort("c1", "JOURNALNODE", "localhost", true));
  }

  @Test
  public void testJMXPortMapUpdate() throws
    NoSuchParentResourceException,
    ResourceAlreadyExistsException, UnsupportedPropertyException,
    SystemException, AmbariException, NoSuchResourceException {

    createConfigs();

    JMXHostProviderModule providerModule = new JMXHostProviderModule();
    providerModule.registerResourceProvider(Resource.Type.Cluster);
    providerModule.registerResourceProvider(Resource.Type.Configuration);
    // Non default port addresses
    Assert.assertEquals("8088", providerModule.getPort("c1", "RESOURCEMANAGER", "localhost"));

    Map<String, String> yarnConfigs = new HashMap<String, String>();
    yarnConfigs.put(RESOURCEMANAGER_PORT, "localhost:50030");
    yarnConfigs.put(NODEMANAGER_PORT, "localhost:11111");
    ConfigurationRequest cr2 = new ConfigurationRequest("c1",
      "yarn-site", "versionN+1", yarnConfigs, null);

    ClusterRequest crReq = new ClusterRequest(1L, "c1", null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr2));
    controller.updateClusters(Collections.singleton(crReq), null);
    Assert.assertEquals("50030", providerModule.getPort("c1", "RESOURCEMANAGER", "localhost"));
    Assert.assertEquals("11111", providerModule.getPort("c1", "NODEMANAGER", "localhost"));

    //Unrelated ports
    Assert.assertEquals("70070", providerModule.getPort("c1", "NAMENODE", "localhost"));
    Assert.assertEquals(null, providerModule.getPort("c1", "JOBTRACKER", "localhost"));

    //test another host and component without property
    Assert.assertNull(providerModule.getPort("c1", "HBASE_REGIONSERVER", "remotehost1"));
  }

  private static class JMXHostProviderModule extends AbstractProviderModule {



    ResourceProvider clusterResourceProvider = new ClusterResourceProvider(controller);

    Injector injector = createNiceMock(Injector.class);
    MaintenanceStateHelper maintenanceStateHelper = createNiceMock(MaintenanceStateHelper.class);
    {
      expect(injector.getInstance(Clusters.class)).andReturn(null);
      replay(maintenanceStateHelper, injector);
    }

    ResourceProvider serviceResourceProvider = new ServiceResourceProvider(PropertyHelper
      .getPropertyIds(Resource.Type.Service),
      PropertyHelper.getKeyPropertyIds(Resource.Type.Service), controller, maintenanceStateHelper);

    ResourceProvider hostCompResourceProvider = new
      HostComponentResourceProvider(PropertyHelper.getPropertyIds(Resource
      .Type.HostComponent), PropertyHelper.getKeyPropertyIds(Resource.Type
      .HostComponent), controller, injector);

    ResourceProvider configResourceProvider = new ConfigurationResourceProvider(
        controller);

    @Override
    protected ResourceProvider createResourceProvider(Resource.Type type) {
      if (type == Resource.Type.Cluster) {
        return clusterResourceProvider;
      }
      if (type == Resource.Type.Service) {
        return serviceResourceProvider;
      } else if (type == Resource.Type.HostComponent) {
        return hostCompResourceProvider;
      } else if (type == Resource.Type.Configuration) {
        return configResourceProvider;
      }
      return null;
    }

  }
}
