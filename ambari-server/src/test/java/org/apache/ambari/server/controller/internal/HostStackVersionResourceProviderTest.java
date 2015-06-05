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
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariManagementControllerImpl;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.ResourceProviderFactory;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.ServiceOsSpecific;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.cluster.ClusterImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;
import com.google.inject.util.Modules;

/**
 * ClusterStackVersionResourceProvider tests.
 */
//@RunWith(PowerMockRunner.class)
@PrepareForTest(AmbariManagementControllerImpl.class)
public class HostStackVersionResourceProviderTest {

  private Injector injector;
  private AmbariMetaInfo ambariMetaInfo;
  private RepositoryVersionDAO repositoryVersionDAOMock;
  private HostVersionDAO hostVersionDAOMock;
  private ConfigHelper configHelper;

  private String operatingSystemsJson = "[\n" +
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
    hostVersionDAOMock = createNiceMock(HostVersionDAO.class);
    configHelper = createNiceMock(ConfigHelper.class);
    // Initialize injector
    InMemoryDefaultTestModule module = new InMemoryDefaultTestModule();
    injector = Guice.createInjector(Modules.override(module).with(new MockModule()));
    injector.getInstance(GuiceJpaInitializer.class);
    ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
  }

  @After
  public void teardown() {
    injector.getInstance(PersistService.class).stop();
  }


  @Test
  public void testCreateResources() throws Exception {
    Resource.Type type = Resource.Type.HostStackVersion;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    StackId stackId = new StackId("HDP", "2.0.1");

    final Host host1 = createNiceMock("host1", Host.class);
    expect(host1.getHostName()).andReturn("host1").anyTimes();
    expect(host1.getOsFamily()).andReturn("redhat6").anyTimes();
    replay(host1);
    Map<String, Host> hostsForCluster = new HashMap<String, Host>() {{
      put(host1.getHostName(), host1);
    }};

    ServiceComponentHost sch = createMock(ServiceComponentHost.class);
    List<ServiceComponentHost> schs = Collections.singletonList(sch);

    RepositoryVersionEntity repoVersion = new RepositoryVersionEntity();
    repoVersion.setOperatingSystems(operatingSystemsJson);

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

    HostVersionEntity hostVersionEntityMock = createNiceMock(HostVersionEntity.class);

    AbstractControllerResourceProvider.init(resourceProviderFactory);

    Map<String, Map<String, String>> hostConfigTags = new HashMap<String, Map<String, String>>();
    expect(configHelper.getEffectiveDesiredTags(anyObject(ClusterImpl.class), anyObject(String.class))).andReturn(hostConfigTags);

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getActionManager()).andReturn(actionManager).anyTimes();
    expect(managementController.getJdkResourceUrl()).andReturn("/JdkResourceUrl").anyTimes();
    expect(managementController.getPackagesForServiceHost(anyObject(ServiceInfo.class),
            (Map<String, String>) anyObject(List.class), anyObject(String.class))).andReturn(packages).anyTimes();

    expect(resourceProviderFactory.getHostResourceProvider(anyObject(Set.class), anyObject(Map.class),
        eq(managementController))).andReturn(csvResourceProvider).anyTimes();

    expect(clusters.getCluster(anyObject(String.class))).andReturn(cluster);
    expect(clusters.getHost(anyObject(String.class))).andReturn(host1);

    expect(cluster.getCurrentStackVersion()).andReturn(stackId);
    expect(cluster.getServiceComponentHosts(anyObject(String.class))).andReturn(schs).anyTimes();

    expect(sch.getServiceName()).andReturn("HIVE").anyTimes();

    expect(
        repositoryVersionDAOMock.findByStackAndVersion(
            anyObject(StackId.class),
            anyObject(String.class))).andReturn(repoVersion);

    expect(
        hostVersionDAOMock.findByClusterStackVersionAndHost(
            anyObject(String.class), anyObject(StackId.class),
            anyObject(String.class), anyObject(String.class))).andReturn(
        hostVersionEntityMock);

    expect(hostVersionEntityMock.getState()).andReturn(RepositoryVersionState.INSTALL_FAILED).anyTimes();

    expect(actionManager.getRequestTasks(anyLong())).andReturn(Collections.<HostRoleCommand>emptyList()).anyTimes();

    // replay
    replay(managementController, response, clusters, resourceProviderFactory, csvResourceProvider,
            cluster, repositoryVersionDAOMock, configHelper, sch, actionManager, hostVersionEntityMock, hostVersionDAOMock);

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
    properties.put(HostStackVersionResourceProvider.HOST_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    properties.put(HostStackVersionResourceProvider.HOST_STACK_VERSION_REPO_VERSION_PROPERTY_ID, "2.2.0.1-885");
    properties.put(HostStackVersionResourceProvider.HOST_STACK_VERSION_STACK_PROPERTY_ID, "HDP");
    properties.put(HostStackVersionResourceProvider.HOST_STACK_VERSION_VERSION_PROPERTY_ID, "2.0.1");
    properties.put(HostStackVersionResourceProvider.HOST_STACK_VERSION_HOST_NAME_PROPERTY_ID, "host1");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    provider.createResources(request);

    // verify
    verify(managementController, response, clusters);
  }

  @Test
  public void testCreateResources_in_out_of_sync_state() throws Exception {
    Resource.Type type = Resource.Type.HostStackVersion;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    StackId stackId = new StackId("HDP", "2.0.1");

    final Host host1 = createNiceMock("host1", Host.class);
    expect(host1.getHostName()).andReturn("host1").anyTimes();
    expect(host1.getOsFamily()).andReturn("redhat6").anyTimes();
    replay(host1);
    Map<String, Host> hostsForCluster = new HashMap<String, Host>() {{
      put(host1.getHostName(), host1);
    }};

    ServiceComponentHost sch = createMock(ServiceComponentHost.class);
    List<ServiceComponentHost> schs = Collections.singletonList(sch);

    RepositoryVersionEntity repoVersion = new RepositoryVersionEntity();
    repoVersion.setOperatingSystems(operatingSystemsJson);

    ServiceOsSpecific.Package hivePackage = new ServiceOsSpecific.Package();
    hivePackage.setName("hive");
    List<ServiceOsSpecific.Package> packages = Collections.singletonList(hivePackage);

    ActionManager actionManager = createNiceMock(ActionManager.class);

    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);
    ResourceProvider csvResourceProvider = createNiceMock(ClusterStackVersionResourceProvider.class);

    HostVersionEntity hostVersionEntityMock = createNiceMock(HostVersionEntity.class);

    AbstractControllerResourceProvider.init(resourceProviderFactory);

    Map<String, Map<String, String>> hostConfigTags = new HashMap<String, Map<String, String>>();
    expect(configHelper.getEffectiveDesiredTags(anyObject(ClusterImpl.class), anyObject(String.class))).andReturn(hostConfigTags);

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getActionManager()).andReturn(actionManager).anyTimes();
    expect(managementController.getJdkResourceUrl()).andReturn("/JdkResourceUrl").anyTimes();
    expect(managementController.getPackagesForServiceHost(anyObject(ServiceInfo.class),
            (Map<String, String>) anyObject(List.class), anyObject(String.class))).andReturn(packages).anyTimes();

    expect(resourceProviderFactory.getHostResourceProvider(anyObject(Set.class), anyObject(Map.class),
            eq(managementController))).andReturn(csvResourceProvider).anyTimes();

    expect(clusters.getCluster(anyObject(String.class))).andReturn(cluster);
    expect(clusters.getHost(anyObject(String.class))).andReturn(host1);

    expect(cluster.getCurrentStackVersion()).andReturn(stackId);
    expect(cluster.getServiceComponentHosts(anyObject(String.class))).andReturn(schs).anyTimes();

    expect(sch.getServiceName()).andReturn("HIVE").anyTimes();

    expect(
        repositoryVersionDAOMock.findByStackAndVersion(
            anyObject(StackId.class),
            anyObject(String.class))).andReturn(repoVersion);

    expect(
        hostVersionDAOMock.findByClusterStackVersionAndHost(
            anyObject(String.class), anyObject(StackId.class),
            anyObject(String.class), anyObject(String.class))).andReturn(
        hostVersionEntityMock);

    expect(hostVersionEntityMock.getState()).andReturn(RepositoryVersionState.OUT_OF_SYNC).anyTimes();

    expect(actionManager.getRequestTasks(anyLong())).andReturn(Collections.<HostRoleCommand>emptyList()).anyTimes();

    // replay
    replay(managementController, response, clusters, resourceProviderFactory, csvResourceProvider,
            cluster, repositoryVersionDAOMock, configHelper, sch, actionManager, hostVersionEntityMock, hostVersionDAOMock);

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
    properties.put(HostStackVersionResourceProvider.HOST_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    properties.put(HostStackVersionResourceProvider.HOST_STACK_VERSION_REPO_VERSION_PROPERTY_ID, "2.2.0.1-885");
    properties.put(HostStackVersionResourceProvider.HOST_STACK_VERSION_STACK_PROPERTY_ID, "HDP");
    properties.put(HostStackVersionResourceProvider.HOST_STACK_VERSION_VERSION_PROPERTY_ID, "2.0.1");
    properties.put(HostStackVersionResourceProvider.HOST_STACK_VERSION_HOST_NAME_PROPERTY_ID, "host1");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    provider.createResources(request);

    // verify
    verify(managementController, response, clusters);
  }

  public class MockModule extends AbstractModule {
    @Override
    protected void configure() {
      bind(RepositoryVersionDAO.class).toInstance(repositoryVersionDAOMock);
      bind(HostVersionDAO.class).toInstance(hostVersionDAOMock);
      bind(ConfigHelper.class).toInstance(configHelper);
    }
  }
}
