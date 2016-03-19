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

import com.google.gson.Gson;
import com.google.inject.Injector;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.HostNotFoundException;
import org.apache.ambari.server.agent.ComponentRecoveryReport;
import org.apache.ambari.server.agent.RecoveryReport;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariManagementControllerImpl;
import org.apache.ambari.server.controller.HostResponse;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.MaintenanceStateHelper;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.HostRequest;
import org.apache.ambari.server.controller.ResourceProviderFactory;
import org.apache.ambari.server.controller.ServiceComponentHostRequest;
import org.apache.ambari.server.controller.ServiceComponentHostResponse;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostHealthStatus;
import org.apache.ambari.server.state.HostHealthStatus.HealthStatus;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.topology.TopologyManager;
import org.easymock.Capture;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.InetAddress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.mockStatic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * HostResourceProvider tests.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(AmbariManagementControllerImpl.class)
public class HostResourceProviderTest {
  @Test
  public void testCreateResources() throws Exception {
    Resource.Type type = Resource.Type.Host;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);
    ResourceProvider hostResourceProvider = createNiceMock(HostResourceProvider.class);

    AbstractControllerResourceProvider.init(resourceProviderFactory);

    expect(resourceProviderFactory.getHostResourceProvider(anyObject(Set.class), anyObject(Map.class),
        eq(managementController))).andReturn(hostResourceProvider).anyTimes();
    // replay
    replay(managementController, response, clusters, resourceProviderFactory, hostResourceProvider);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<Map<String, Object>>();

    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    // add properties to the request map
    properties.put(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    properties.put(HostResourceProvider.HOST_NAME_PROPERTY_ID, "Host100");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    provider.createResources(request);

    // verify
    verify(managementController, response, clusters);
  }

  @Test
  public void testGetResources() throws Exception {
    Resource.Type type = Resource.Type.Host;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Host host1 = createNiceMock(Host.class);
    Host host2 = createNiceMock(Host.class);
    Host host3 = createNiceMock(Host.class);
    HostHealthStatus healthStatus = createNiceMock(HostHealthStatus.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);
    ResourceProvider hostResourceProvider = createNiceMock(HostResourceProvider.class);

    AbstractControllerResourceProvider.init(resourceProviderFactory);

    List<Host> hosts = new LinkedList<Host>();
    hosts.add(host1);
    hosts.add(host2);
    hosts.add(host3);

    Set<Cluster> clusterSet = new HashSet<Cluster>();
    clusterSet.add(cluster);

    // set expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getHostComponents((Set<ServiceComponentHostRequest>) anyObject())).andReturn(Collections.<ServiceComponentHostResponse>emptySet()).anyTimes();
    expect(resourceProviderFactory.getHostResourceProvider(anyObject(Set.class),
                                                           anyObject(Map.class),
                                                           eq(managementController))).
                                                           andReturn(hostResourceProvider).anyTimes();

    expect(clusters.getHosts()).andReturn(hosts).anyTimes();

    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();

    expect(clusters.getClustersForHost("Host100")).andReturn(clusterSet).anyTimes();
    expect(clusters.getClustersForHost("Host101")).andReturn(clusterSet).anyTimes();
    expect(clusters.getClustersForHost("Host102")).andReturn(clusterSet).anyTimes();

    expect(host1.getHostName()).andReturn("Host100").anyTimes();
    expect(host2.getHostName()).andReturn("Host101").anyTimes();
    expect(host3.getHostName()).andReturn("Host102").anyTimes();

    expect(healthStatus.getHealthStatus()).andReturn(HostHealthStatus.HealthStatus.HEALTHY).anyTimes();
    expect(healthStatus.getHealthReport()).andReturn("HEALTHY").anyTimes();

    Set<String> propertyIds = new HashSet<String>();

    propertyIds.add(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID);
    propertyIds.add(HostResourceProvider.HOST_NAME_PROPERTY_ID);

    Predicate predicate =
        new PredicateBuilder().property(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").
            toPredicate();
    Request request = PropertyHelper.getReadRequest(propertyIds);

    Set<Resource> hostsResources = new HashSet<Resource>();

    Resource hostResource1 = new ResourceImpl(Resource.Type.Host);
    hostResource1.setProperty(PropertyHelper.getPropertyId("Hosts", "cluster_name"), "Cluster100");
    hostResource1.setProperty(PropertyHelper.getPropertyId("Hosts", "host_name"), "Host100");
    Resource hostResource2 = new ResourceImpl(Resource.Type.Host);
    hostResource2.setProperty(PropertyHelper.getPropertyId("Hosts", "cluster_name"), "Cluster100");
    hostResource2.setProperty(PropertyHelper.getPropertyId("Hosts", "host_name"), "Host101");
    Resource hostResource3 = new ResourceImpl(Resource.Type.Host);
    hostResource3.setProperty(PropertyHelper.getPropertyId("Hosts", "cluster_name"), "Cluster100");
    hostResource3.setProperty(PropertyHelper.getPropertyId("Hosts", "host_name"), "Host102");
    hostsResources.add(hostResource1);
    hostsResources.add(hostResource2);
    hostsResources.add(hostResource3);

    expect(hostResourceProvider.getResources(eq(request), eq(predicate))).andReturn(hostsResources).anyTimes();


    // replay
    replay(managementController, clusters, cluster,
        host1, host2, host3,
        healthStatus, ambariMetaInfo, resourceProviderFactory, hostResourceProvider);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);


    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(3, resources.size());
    for (Resource resource : resources) {
      String clusterName = (String) resource.getPropertyValue(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID);
      Assert.assertEquals("Cluster100", clusterName);
    }

    // verify
    verify(managementController, clusters, cluster,
        host1, host2, host3,
        healthStatus, ambariMetaInfo);
  }

  @Test
  public void testGetResources_Status_NoCluster() throws Exception {
    Resource.Type type = Resource.Type.Host;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    HostHealthStatus healthStatus = createNiceMock(HostHealthStatus.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);

    HostResponse hostResponse1 = createNiceMock(HostResponse.class);

    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);
    ResourceProvider hostResourceProvider = createNiceMock(HostResourceProvider.class);

    AbstractControllerResourceProvider.init(resourceProviderFactory);

    Set<Cluster> clusterSet = new HashSet<Cluster>();
    clusterSet.add(cluster);

    ServiceComponentHostResponse shr1 = new ServiceComponentHostResponse("Cluster100", "Service100", "Component100", "Component 100",
        "Host100", "STARTED", "", null, null, null);
    ServiceComponentHostResponse shr2 = new ServiceComponentHostResponse("Cluster100", "Service100", "Component102", "Component 102",
        "Host100", "STARTED", "", null, null, null);
    ServiceComponentHostResponse shr3 = new ServiceComponentHostResponse("Cluster100", "Service100", "Component103", "Component 103",
        "Host100", "STARTED", "", null, null, null);

    Set<ServiceComponentHostResponse> responses = new HashSet<ServiceComponentHostResponse>();
    responses.add(shr1);
    responses.add(shr2);
    responses.add(shr3);

    // set expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getHostComponents((Set<ServiceComponentHostRequest>) anyObject())).andReturn(responses).anyTimes();

    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();

    expect(clusters.getClustersForHost("Host100")).andReturn(clusterSet).anyTimes();


    expect(hostResponse1.getClusterName()).andReturn("").anyTimes();
    expect(hostResponse1.getHostname()).andReturn("Host100").anyTimes();
    expect(hostResponse1.getHealthStatus()).andReturn(healthStatus).anyTimes();
    expect(hostResponse1.getStatus()).andReturn(HealthStatus.HEALTHY.name()).anyTimes();

    expect(healthStatus.getHealthStatus()).andReturn(HostHealthStatus.HealthStatus.HEALTHY).anyTimes();
    expect(healthStatus.getHealthReport()).andReturn("HEALTHY").anyTimes();


    expect(ambariMetaInfo.getComponent((String) anyObject(), (String) anyObject(),
        (String) anyObject(), (String) anyObject())).andReturn(componentInfo).anyTimes();

    expect(componentInfo.getCategory()).andReturn("MASTER").anyTimes();

    expect(resourceProviderFactory.getHostResourceProvider(anyObject(Set.class),
                                                               anyObject(Map.class),
                                                               eq(managementController))).
                                                               andReturn(hostResourceProvider).anyTimes();

    Set<String> propertyIds = new HashSet<String>();

    propertyIds.add(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID);
    propertyIds.add(HostResourceProvider.HOST_NAME_PROPERTY_ID);
    propertyIds.add(HostResourceProvider.HOST_HOST_STATUS_PROPERTY_ID);

    Predicate predicate =
        new PredicateBuilder().property(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").
            toPredicate();
    Request request = PropertyHelper.getReadRequest(propertyIds);

    Set<Resource> hostsResources = new HashSet<Resource>();

    Resource hostResource1 = new ResourceImpl(Resource.Type.Host);
    hostResource1.setProperty(HostResourceProvider.HOST_HOST_STATUS_PROPERTY_ID, HealthStatus.HEALTHY.name());
    hostsResources.add(hostResource1);

    expect(hostResourceProvider.getResources(eq(request), eq(predicate))).andReturn(hostsResources).anyTimes();


    // replay
    replay(managementController, clusters, cluster,
        hostResponse1, componentInfo,
        healthStatus, ambariMetaInfo, resourceProviderFactory, hostResourceProvider);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      String clusterName = (String) resource.getPropertyValue(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID);
      Assert.assertNull(clusterName);
      String status = (String) resource.getPropertyValue(HostResourceProvider.HOST_HOST_STATUS_PROPERTY_ID);
      Assert.assertEquals("HEALTHY", status);
    }

    // verify
    verify(managementController, clusters, cluster,
        hostResponse1, componentInfo,
        healthStatus, ambariMetaInfo);
  }

  @Test
  public void testGetResources_Status_Healthy() throws Exception {
    Resource.Type type = Resource.Type.Host;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    HostHealthStatus healthStatus = createNiceMock(HostHealthStatus.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    StackId stackId = createNiceMock(StackId.class);
    ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);

    HostResponse hostResponse1 = createNiceMock(HostResponse.class);

    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);
    ResourceProvider hostResourceProvider = createNiceMock(HostResourceProvider.class);

    AbstractControllerResourceProvider.init(resourceProviderFactory);

    Set<Cluster> clusterSet = new HashSet<Cluster>();
    clusterSet.add(cluster);

    ServiceComponentHostResponse shr1 = new ServiceComponentHostResponse("Cluster100", "Service100", "Component100", "Component 100",
        "Host100", "STARTED", "", null, null, null);
    ServiceComponentHostResponse shr2 = new ServiceComponentHostResponse("Cluster100", "Service100", "Component102", "Component 102",
        "Host100", "STARTED", "", null, null, null);
    ServiceComponentHostResponse shr3 = new ServiceComponentHostResponse("Cluster100", "Service100", "Component103", "Component 103",
        "Host100", "STARTED", "", null, null, null);

    Set<ServiceComponentHostResponse> responses = new HashSet<ServiceComponentHostResponse>();
    responses.add(shr1);
    responses.add(shr2);
    responses.add(shr3);

    // set expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getHostComponents((Set<ServiceComponentHostRequest>) anyObject())).andReturn(responses).anyTimes();

    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();

    expect(clusters.getClustersForHost("Host100")).andReturn(clusterSet).anyTimes();

    expect(hostResponse1.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(hostResponse1.getHostname()).andReturn("Host100").anyTimes();
    expect(hostResponse1.getHealthStatus()).andReturn(healthStatus).anyTimes();
    expect(hostResponse1.getStatus()).andReturn(HealthStatus.HEALTHY.name()).anyTimes();

    expect(healthStatus.getHealthStatus()).andReturn(HostHealthStatus.HealthStatus.HEALTHY).anyTimes();
    expect(healthStatus.getHealthReport()).andReturn("HEALTHY").anyTimes();


    expect(ambariMetaInfo.getComponent((String) anyObject(), (String) anyObject(),
        (String) anyObject(), (String) anyObject())).andReturn(componentInfo).anyTimes();

    expect(componentInfo.getCategory()).andReturn("MASTER").anyTimes();

    expect(resourceProviderFactory.getHostResourceProvider(anyObject(Set.class), anyObject(Map.class),
        eq(managementController))).andReturn(hostResourceProvider).anyTimes();

    Set<String> propertyIds = new HashSet<String>();

    propertyIds.add(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID);
    propertyIds.add(HostResourceProvider.HOST_NAME_PROPERTY_ID);
    propertyIds.add(HostResourceProvider.HOST_HOST_STATUS_PROPERTY_ID);

    Predicate predicate =
        new PredicateBuilder().property(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").
            toPredicate();
    Request request = PropertyHelper.getReadRequest(propertyIds);


    Set<Resource> hostsResources = new HashSet<Resource>();

    Resource hostResource1 = new ResourceImpl(Resource.Type.Host);
    hostResource1.setProperty(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    hostResource1.setProperty(HostResourceProvider.HOST_HOST_STATUS_PROPERTY_ID, HealthStatus.HEALTHY.name());
    hostsResources.add(hostResource1);

    expect(hostResourceProvider.getResources(eq(request), eq(predicate))).andReturn(hostsResources).anyTimes();


    // replay
    replay(managementController, clusters, cluster,
        hostResponse1, stackId, componentInfo,
        healthStatus, ambariMetaInfo, resourceProviderFactory, hostResourceProvider);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);


    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      String clusterName = (String) resource.getPropertyValue(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID);
      Assert.assertEquals("Cluster100", clusterName);
      String status = (String) resource.getPropertyValue(HostResourceProvider.HOST_HOST_STATUS_PROPERTY_ID);
      Assert.assertEquals("HEALTHY", status);
    }

    // verify
    verify(managementController, clusters, cluster,
        hostResponse1, stackId, componentInfo,
        healthStatus, ambariMetaInfo, resourceProviderFactory, hostResourceProvider);
  }

  @Test
  public void testGetResources_Status_Unhealthy() throws Exception {
    Resource.Type type = Resource.Type.Host;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    HostHealthStatus healthStatus = createNiceMock(HostHealthStatus.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    StackId stackId = createNiceMock(StackId.class);
    ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);
    HostResponse hostResponse1 = createNiceMock(HostResponse.class);
    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);
    ResourceProvider hostResourceProvider = createNiceMock(HostResourceProvider.class);

    AbstractControllerResourceProvider.init(resourceProviderFactory);

    Set<Cluster> clusterSet = new HashSet<Cluster>();
    clusterSet.add(cluster);

    ServiceComponentHostResponse shr1 = new ServiceComponentHostResponse("Cluster100", "Service100", "Component100", "Component 100",
        "Host100", "STARTED", "", null, null, null);
    ServiceComponentHostResponse shr2 = new ServiceComponentHostResponse("Cluster100", "Service100", "Component102", "Component 102",
        "Host100", "INSTALLED", "", null, null, null);
    ServiceComponentHostResponse shr3 = new ServiceComponentHostResponse("Cluster100", "Service100", "Component103", "Component 103",
        "Host100", "STARTED", "", null, null, null);

    Set<ServiceComponentHostResponse> responses = new HashSet<ServiceComponentHostResponse>();
    responses.add(shr1);
    responses.add(shr2);
    responses.add(shr3);

    // set expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getHostComponents((Set<ServiceComponentHostRequest>) anyObject())).andReturn(responses).anyTimes();

    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();

    expect(clusters.getClustersForHost("Host100")).andReturn(clusterSet).anyTimes();

    expect(hostResponse1.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(hostResponse1.getHostname()).andReturn("Host100").anyTimes();
    expect(hostResponse1.getHealthStatus()).andReturn(healthStatus).anyTimes();
    expect(hostResponse1.getStatus()).andReturn(HealthStatus.UNHEALTHY.name()).anyTimes();

    expect(healthStatus.getHealthStatus()).andReturn(HostHealthStatus.HealthStatus.HEALTHY).anyTimes();
    expect(healthStatus.getHealthReport()).andReturn("HEALTHY").anyTimes();


    expect(ambariMetaInfo.getComponent((String) anyObject(), (String) anyObject(),
        (String) anyObject(), (String) anyObject())).andReturn(componentInfo).anyTimes();

    expect(componentInfo.getCategory()).andReturn("MASTER").anyTimes();

    expect(resourceProviderFactory.getHostResourceProvider(anyObject(Set.class), anyObject(Map.class),
        eq(managementController))).andReturn(hostResourceProvider).anyTimes();

    Set<String> propertyIds = new HashSet<String>();

    propertyIds.add(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID);
    propertyIds.add(HostResourceProvider.HOST_NAME_PROPERTY_ID);
    propertyIds.add(HostResourceProvider.HOST_HOST_STATUS_PROPERTY_ID);

    Predicate predicate =
        new PredicateBuilder().property(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").
            toPredicate();
    Request request = PropertyHelper.getReadRequest(propertyIds);


    Set<Resource> hostsResources = new HashSet<Resource>();

    Resource hostResource1 = new ResourceImpl(Resource.Type.Host);
    hostResource1.setProperty(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    hostResource1.setProperty(HostResourceProvider.HOST_HOST_STATUS_PROPERTY_ID, HealthStatus.UNHEALTHY.name());
    hostsResources.add(hostResource1);

    expect(hostResourceProvider.getResources(eq(request), eq(predicate))).andReturn(hostsResources).anyTimes();


    // replay
    replay(managementController, clusters, cluster,
        hostResponse1, stackId, componentInfo,
        healthStatus, ambariMetaInfo, resourceProviderFactory, hostResourceProvider);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);


    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      String clusterName = (String) resource.getPropertyValue(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID);
      Assert.assertEquals("Cluster100", clusterName);
      String status = (String) resource.getPropertyValue(HostResourceProvider.HOST_HOST_STATUS_PROPERTY_ID);
      Assert.assertEquals("UNHEALTHY", status);
    }

    // verify
    verify(managementController, clusters, cluster,
        hostResponse1, stackId, componentInfo,
        healthStatus, ambariMetaInfo, resourceProviderFactory, hostResourceProvider);
  }

  @Test
  public void testGetResources_Status_Unknown() throws Exception {
    Resource.Type type = Resource.Type.Host;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    HostHealthStatus healthStatus = createNiceMock(HostHealthStatus.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    HostResponse hostResponse1 = createNiceMock(HostResponse.class);
    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);
    ResourceProvider hostResourceProvider = createNiceMock(HostResourceProvider.class);

    AbstractControllerResourceProvider.init(resourceProviderFactory);

    Set<Cluster> clusterSet = new HashSet<Cluster>();
    clusterSet.add(cluster);

    // set expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();

    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();

    expect(clusters.getClustersForHost("Host100")).andReturn(clusterSet).anyTimes();

    expect(hostResponse1.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(hostResponse1.getHostname()).andReturn("Host100").anyTimes();
    expect(hostResponse1.getHealthStatus()).andReturn(healthStatus).anyTimes();
    expect(hostResponse1.getStatus()).andReturn(HealthStatus.UNKNOWN.name()).anyTimes();

    expect(healthStatus.getHealthStatus()).andReturn(HostHealthStatus.HealthStatus.UNKNOWN).anyTimes();
    expect(healthStatus.getHealthReport()).andReturn("UNKNOWN").anyTimes();
    expect(resourceProviderFactory.getHostResourceProvider(anyObject(Set.class), anyObject(Map.class),
        eq(managementController))).andReturn(hostResourceProvider).anyTimes();

    Set<Resource> hostsResources = new HashSet<Resource>();

    Resource hostResource1 = new ResourceImpl(Resource.Type.Host);
    hostResource1.setProperty(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    hostResource1.setProperty(HostResourceProvider.HOST_HOST_STATUS_PROPERTY_ID, HealthStatus.UNKNOWN.name());
    hostsResources.add(hostResource1);

    Set<String> propertyIds = new HashSet<String>();

    propertyIds.add(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID);
    propertyIds.add(HostResourceProvider.HOST_NAME_PROPERTY_ID);
    propertyIds.add(HostResourceProvider.HOST_HOST_STATUS_PROPERTY_ID);

    Predicate predicate =
        new PredicateBuilder().property(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").
            toPredicate();
    Request request = PropertyHelper.getReadRequest(propertyIds);

    expect(hostResourceProvider.getResources(eq(request), eq(predicate))).andReturn(hostsResources).anyTimes();

    // replay
    replay(managementController, clusters, cluster,
        hostResponse1,
        healthStatus, ambariMetaInfo, resourceProviderFactory, hostResourceProvider);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      String clusterName = (String) resource.getPropertyValue(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID);
      Assert.assertEquals("Cluster100", clusterName);
      String status = (String) resource.getPropertyValue(HostResourceProvider.HOST_HOST_STATUS_PROPERTY_ID);
      Assert.assertEquals("UNKNOWN", status);
    }

    // verify
    verify(managementController, clusters, cluster,
        hostResponse1,
        healthStatus, ambariMetaInfo, resourceProviderFactory, hostResourceProvider);
  }

  @Test
  public void testGetRecoveryReport() throws Exception {
    Resource.Type type = Resource.Type.Host;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    HostHealthStatus healthStatus = createNiceMock(HostHealthStatus.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    StackId stackId = createNiceMock(StackId.class);
    ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);
    HostResponse hostResponse1 = createNiceMock(HostResponse.class);
    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);
    ResourceProvider hostResourceProvider = createNiceMock(HostResourceProvider.class);

    RecoveryReport rr = new RecoveryReport();
    rr.setSummary("RECOVERABLE");
    List<ComponentRecoveryReport> compRecReports = new ArrayList<ComponentRecoveryReport>();
    ComponentRecoveryReport compRecReport = new ComponentRecoveryReport();
    compRecReport.setLimitReached(Boolean.FALSE);
    compRecReport.setName("DATANODE");
    compRecReport.setNumAttempts(2);
    compRecReports.add(compRecReport);
    rr.setComponentReports(compRecReports);

    AbstractControllerResourceProvider.init(resourceProviderFactory);

    Set<Cluster> clusterSet = new HashSet<Cluster>();
    clusterSet.add(cluster);

    ServiceComponentHostResponse shr1 = new ServiceComponentHostResponse("Cluster100", "Service100", "Component100", "Component 100",
        "Host100", "STARTED", "", null, null, null);

    Set<ServiceComponentHostResponse> responses = new HashSet<ServiceComponentHostResponse>();
    responses.add(shr1);

    // set expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getHostComponents((Set<ServiceComponentHostRequest>) anyObject())).andReturn(responses).anyTimes();
    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();
    expect(clusters.getClustersForHost("Host100")).andReturn(clusterSet).anyTimes();
    expect(hostResponse1.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(hostResponse1.getHostname()).andReturn("Host100").anyTimes();
    expect(hostResponse1.getRecoveryReport()).andReturn(rr).anyTimes();
    expect(hostResponse1.getRecoverySummary()).andReturn(rr.getSummary()).anyTimes();
    expect(ambariMetaInfo.getComponent((String) anyObject(), (String) anyObject(),
                                       (String) anyObject(), (String) anyObject())).andReturn(componentInfo).anyTimes();
    expect(componentInfo.getCategory()).andReturn("SLAVE").anyTimes();
    expect(resourceProviderFactory.getHostResourceProvider(anyObject(Set.class), anyObject(Map.class),
                                                           eq(managementController))).andReturn(hostResourceProvider).anyTimes();


    Set<String> propertyIds = new HashSet<String>();

    propertyIds.add(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID);
    propertyIds.add(HostResourceProvider.HOST_NAME_PROPERTY_ID);
    propertyIds.add(HostResourceProvider.HOST_RECOVERY_REPORT_PROPERTY_ID);
    propertyIds.add(HostResourceProvider.HOST_RECOVERY_SUMMARY_PROPERTY_ID);

    Predicate predicate =
        new PredicateBuilder().property(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").
            toPredicate();
    Request request = PropertyHelper.getReadRequest(propertyIds);

    Set<Resource> hostsResources = new HashSet<Resource>();

    Resource hostResource1 = new ResourceImpl(Resource.Type.Host);
    hostResource1.setProperty(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    hostResource1.setProperty(HostResourceProvider.HOST_RECOVERY_SUMMARY_PROPERTY_ID, rr.getSummary());
    hostResource1.setProperty(HostResourceProvider.HOST_RECOVERY_REPORT_PROPERTY_ID, rr);
    hostsResources.add(hostResource1);

    expect(hostResourceProvider.getResources(eq(request), eq(predicate))).andReturn(hostsResources).anyTimes();


    // replay
    replay(managementController, clusters, cluster,
           hostResponse1, stackId, componentInfo,
           healthStatus, ambariMetaInfo, resourceProviderFactory, hostResourceProvider);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);


    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      String clusterName = (String) resource.getPropertyValue(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID);
      Assert.assertEquals("Cluster100", clusterName);
      String recovery = (String) resource.getPropertyValue(HostResourceProvider.HOST_RECOVERY_SUMMARY_PROPERTY_ID);
      Assert.assertEquals("RECOVERABLE", recovery);
      RecoveryReport recRep = (RecoveryReport)resource.getPropertyValue(HostResourceProvider.HOST_RECOVERY_REPORT_PROPERTY_ID);
      Assert.assertEquals("RECOVERABLE", recRep.getSummary());
      Assert.assertEquals(1, recRep.getComponentReports().size());
      Assert.assertEquals(2, recRep.getComponentReports().get(0).getNumAttempts());
    }

    // verify
    verify(managementController, clusters, cluster,
           hostResponse1, stackId, componentInfo,
           healthStatus, ambariMetaInfo, resourceProviderFactory, hostResourceProvider);
  }

  @Test
  public void testGetResources_Status_Alert() throws Exception {
    Resource.Type type = Resource.Type.Host;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    HostHealthStatus healthStatus = createNiceMock(HostHealthStatus.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    StackId stackId = createNiceMock(StackId.class);
    ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);
    HostResponse hostResponse1 = createNiceMock(HostResponse.class);
    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);
    ResourceProvider hostResourceProvider = createNiceMock(HostResourceProvider.class);
    AbstractControllerResourceProvider.init(resourceProviderFactory);

    Set<Cluster> clusterSet = new HashSet<Cluster>();
    clusterSet.add(cluster);

    ServiceComponentHostResponse shr1 = new ServiceComponentHostResponse("Cluster100", "Service100", "Component100", "Component 100",
        "Host100", "STARTED", "", null, null, null);
    ServiceComponentHostResponse shr2 = new ServiceComponentHostResponse("Cluster100", "Service100", "Component102", "Component 102",
        "Host100", "INSTALLED", "", null, null, null);
    ServiceComponentHostResponse shr3 = new ServiceComponentHostResponse("Cluster100", "Service100", "Component103", "Component 103",
        "Host100", "STARTED", "", null, null, null);

    Set<ServiceComponentHostResponse> responses = new HashSet<ServiceComponentHostResponse>();
    responses.add(shr1);
    responses.add(shr2);
    responses.add(shr3);

    // set expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getHostComponents((Set<ServiceComponentHostRequest>) anyObject())).andReturn(responses).anyTimes();
    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();
    expect(clusters.getClustersForHost("Host100")).andReturn(clusterSet).anyTimes();
    expect(hostResponse1.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(hostResponse1.getHostname()).andReturn("Host100").anyTimes();
    expect(hostResponse1.getHealthStatus()).andReturn(healthStatus).anyTimes();
    expect(hostResponse1.getStatus()).andReturn(HealthStatus.ALERT.name()).anyTimes();
    expect(healthStatus.getHealthStatus()).andReturn(HostHealthStatus.HealthStatus.HEALTHY).anyTimes();
    expect(healthStatus.getHealthReport()).andReturn("HEALTHY").anyTimes();
    expect(ambariMetaInfo.getComponent((String) anyObject(), (String) anyObject(),
        (String) anyObject(), (String) anyObject())).andReturn(componentInfo).anyTimes();
    expect(componentInfo.getCategory()).andReturn("SLAVE").anyTimes();
    expect(resourceProviderFactory.getHostResourceProvider(anyObject(Set.class), anyObject(Map.class),
        eq(managementController))).andReturn(hostResourceProvider).anyTimes();


    Set<String> propertyIds = new HashSet<String>();

    propertyIds.add(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID);
    propertyIds.add(HostResourceProvider.HOST_NAME_PROPERTY_ID);
    propertyIds.add(HostResourceProvider.HOST_HOST_STATUS_PROPERTY_ID);

    Predicate predicate =
        new PredicateBuilder().property(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").
            toPredicate();
    Request request = PropertyHelper.getReadRequest(propertyIds);

    Set<Resource> hostsResources = new HashSet<Resource>();

    Resource hostResource1 = new ResourceImpl(Resource.Type.Host);
    hostResource1.setProperty(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    hostResource1.setProperty(HostResourceProvider.HOST_HOST_STATUS_PROPERTY_ID, HealthStatus.ALERT.name());
    hostsResources.add(hostResource1);

    expect(hostResourceProvider.getResources(eq(request), eq(predicate))).andReturn(hostsResources).anyTimes();


    // replay
    replay(managementController, clusters, cluster,
        hostResponse1, stackId, componentInfo,
        healthStatus, ambariMetaInfo, resourceProviderFactory, hostResourceProvider);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);


    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      String clusterName = (String) resource.getPropertyValue(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID);
      Assert.assertEquals("Cluster100", clusterName);
      String status = (String) resource.getPropertyValue(HostResourceProvider.HOST_HOST_STATUS_PROPERTY_ID);
      Assert.assertEquals("ALERT", status);
    }

    // verify
    verify(managementController, clusters, cluster,
        hostResponse1, stackId, componentInfo,
        healthStatus, ambariMetaInfo, resourceProviderFactory, hostResourceProvider);
  }

  @Test
  public void testUpdateDesiredConfig() throws Exception {

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    HostHealthStatus healthStatus = createNiceMock(HostHealthStatus.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    HostResponse hostResponse1 = createNiceMock(HostResponse.class);
    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);
    ResourceProvider hostResourceProvider = createNiceMock(HostResourceProvider.class);

    AbstractControllerResourceProvider.init(resourceProviderFactory);

    Set<Cluster> clusterSet = new HashSet<Cluster>();
    clusterSet.add(cluster);

    // set expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getHostComponents((Set<ServiceComponentHostRequest>) anyObject())).andReturn(Collections.<ServiceComponentHostResponse>emptySet()).anyTimes();
    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();
    expect(clusters.getClustersForHost("Host100")).andReturn(clusterSet).anyTimes();
    expect(hostResponse1.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(hostResponse1.getHostname()).andReturn("Host100").anyTimes();
    expect(hostResponse1.getHealthStatus()).andReturn(healthStatus).anyTimes();
    expect(healthStatus.getHealthStatus()).andReturn(HostHealthStatus.HealthStatus.HEALTHY).anyTimes();
    expect(healthStatus.getHealthReport()).andReturn("HEALTHY").anyTimes();
    expect(resourceProviderFactory.getHostResourceProvider(anyObject(Set.class), anyObject(Map.class),
        eq(managementController))).andReturn(hostResourceProvider).anyTimes();

    // replay
    replay(managementController, clusters, cluster,
        hostResponse1,
        healthStatus, ambariMetaInfo, resourceProviderFactory, hostResourceProvider);

    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    properties.put(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    properties.put(HostResourceProvider.HOST_NAME_PROPERTY_ID, "Host100");
    properties.put(PropertyHelper.getPropertyId("Hosts.desired_config", "type"), "global");
    properties.put(PropertyHelper.getPropertyId("Hosts.desired_config", "tag"), "version1");
    properties.put(PropertyHelper.getPropertyId("Hosts.desired_config.properties", "a"), "b");
    properties.put(PropertyHelper.getPropertyId("Hosts.desired_config.properties", "x"), "y");

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, null);

    Predicate  predicate = new PredicateBuilder().property(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID).
        equals("Cluster100").
        and().property(HostResourceProvider.HOST_NAME_PROPERTY_ID).equals("Host100").toPredicate();

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        Resource.Type.Host,
        PropertyHelper.getPropertyIds(Resource.Type.Host),
        PropertyHelper.getKeyPropertyIds(Resource.Type.Host),
        managementController);

    provider.updateResources(request, predicate);

    // verify
    verify(managementController, clusters, cluster,
        hostResponse1,
        healthStatus, ambariMetaInfo, resourceProviderFactory, hostResourceProvider);
  }

  @Test
  public void testUpdateResources() throws Exception {
    Resource.Type type = Resource.Type.Host;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    HostHealthStatus healthStatus = createNiceMock(HostHealthStatus.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    HostResponse hostResponse1 = createNiceMock(HostResponse.class);
    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);
    ResourceProvider hostResourceProvider = createNiceMock(HostResourceProvider.class);

    AbstractControllerResourceProvider.init(resourceProviderFactory);

    Set<Cluster> clusterSet = new HashSet<Cluster>();
    clusterSet.add(cluster);

    // set expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getHostComponents((Set<ServiceComponentHostRequest>) anyObject())).andReturn(Collections.<ServiceComponentHostResponse>emptySet()).anyTimes();
    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();
    expect(clusters.getClustersForHost("Host100")).andReturn(clusterSet).anyTimes();
    expect(hostResponse1.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(hostResponse1.getHostname()).andReturn("Host100").anyTimes();
    expect(hostResponse1.getHealthStatus()).andReturn(healthStatus).anyTimes();
    expect(healthStatus.getHealthStatus()).andReturn(HostHealthStatus.HealthStatus.HEALTHY).anyTimes();
    expect(healthStatus.getHealthReport()).andReturn("HEALTHY").anyTimes();
    expect(resourceProviderFactory.getHostResourceProvider(anyObject(Set.class), anyObject(Map.class),
        eq(managementController))).andReturn(hostResourceProvider).anyTimes();

    // replay
    replay(managementController, clusters, cluster,
        hostResponse1,
        healthStatus, ambariMetaInfo, resourceProviderFactory, hostResourceProvider);


    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    // add the property map to a set for the request.
    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    properties.put(HostResourceProvider.HOST_RACK_INFO_PROPERTY_ID, "rack info");

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, null);

    Predicate  predicate = new PredicateBuilder().property(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID).
        equals("Cluster100").
        and().property(HostResourceProvider.HOST_NAME_PROPERTY_ID).equals("Host100").toPredicate();
    provider.updateResources(request, predicate);

    // verify
    verify(managementController, clusters, cluster,
        hostResponse1,
        healthStatus, ambariMetaInfo, resourceProviderFactory, hostResourceProvider);
  }

  @Test
  public void testDeleteResources() throws Exception {
    Resource.Type type = Resource.Type.Host;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Host host1 = createNiceMock(Host.class);
    HostHealthStatus healthStatus = createNiceMock(HostHealthStatus.class);

    List<Host> hosts = new LinkedList<Host>();
    hosts.add(host1);

    Set<Cluster> clusterSet = new HashSet<Cluster>();

    // set expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getHosts()).andReturn(hosts).anyTimes();
    expect(clusters.getHost("Host100")).andReturn(host1).anyTimes();
    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();
    expect(clusters.getClustersForHost("Host100")).andReturn(clusterSet).anyTimes();
    expect(clusters.getClusters()).andReturn(Collections.EMPTY_MAP);
    expect(host1.getHostName()).andReturn("Host100").anyTimes();
    expect(healthStatus.getHealthStatus()).andReturn(HostHealthStatus.HealthStatus.HEALTHY).anyTimes();
    expect(healthStatus.getHealthReport()).andReturn("HEALTHY").anyTimes();

    TopologyManager topologyManager = createNiceMock(TopologyManager.class);
    expect(topologyManager.getRequests(Collections.EMPTY_LIST)).andReturn(Collections.EMPTY_LIST).anyTimes();

    // replay
    replay(managementController, clusters, cluster, topologyManager,
        host1,
        healthStatus);



    ResourceProvider provider = getHostProvider(managementController);
    HostResourceProvider.setTopologyManager(topologyManager);

    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();

    ((ObservableResourceProvider)provider).addObserver(observer);

    Predicate predicate = new PredicateBuilder().property(HostResourceProvider.HOST_NAME_PROPERTY_ID).equals("Host100").
        toPredicate();
    provider.deleteResources(predicate);


    ResourceProviderEvent lastEvent = observer.getLastEvent();
    Assert.assertNotNull(lastEvent);
    Assert.assertEquals(Resource.Type.Host, lastEvent.getResourceType());
    Assert.assertEquals(ResourceProviderEvent.Type.Delete, lastEvent.getType());
    Assert.assertEquals(predicate, lastEvent.getPredicate());
    Assert.assertNull(lastEvent.getRequest());

    // verify
    verify(managementController, clusters, cluster,
        host1,
        healthStatus);
  }

  public static HostResourceProvider getHostProvider(AmbariManagementController managementController) {
    Resource.Type type = Resource.Type.Host;

    return new HostResourceProvider(PropertyHelper.getPropertyIds(type),
                                    PropertyHelper.getKeyPropertyIds(type),
                                    managementController);
  }

  @Test
  public void testGetHosts() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = new Capture<AmbariManagementController>();
    Clusters clusters = createNiceMock(Clusters.class);
    MaintenanceStateHelper maintHelper = createNiceMock(MaintenanceStateHelper.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Host host = createNiceMock(Host.class);
    HostResponse response = createNiceMock(HostResponse.class);

    Set<Cluster> setCluster = Collections.singleton(cluster);

    // requests
    HostRequest request1 = new HostRequest("host1", "cluster1", Collections.<String, String>emptyMap());

    Set<HostRequest> setRequests = new HashSet<HostRequest>();
    setRequests.add(request1);

    // expectations
    // constructor init
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(null);
    expect(injector.getInstance(MaintenanceStateHelper.class)).andReturn(maintHelper);
    expect(injector.getInstance(KerberosHelper.class)).andReturn(createNiceMock(KerberosHelper.class));

    // getHosts
    expect(clusters.getCluster("cluster1")).andReturn(cluster);
    expect(clusters.getHost("host1")).andReturn(host);
    expect(host.getHostName()).andReturn("host1").anyTimes();
    expect(clusters.getClustersForHost("host1")).andReturn(setCluster);
    expect(host.convertToResponse()).andReturn(response);
    response.setClusterName("cluster1");

    final InetAddress mock = createMock(InetAddress.class);
    mockStatic(InetAddress.class);
    expect(InetAddress.getLocalHost()).andReturn(mock);
    replayAll();
    // replay mocks
    replay(maintHelper, injector, clusters, cluster, host, response);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);
    Set<HostResponse> setResponses = getHosts(controller, setRequests);

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    assertEquals(1, setResponses.size());
    assertTrue(setResponses.contains(response));

    verify(injector, clusters, cluster, host, response);
  }

  /**
   * Ensure that HostNotFoundException is propagated in case where there is a single request.
   */
  @Test
  public void testGetHosts___HostNotFoundException() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = new Capture<AmbariManagementController>();
    Clusters clusters = createNiceMock(Clusters.class);
    MaintenanceStateHelper maintHelper = createNiceMock(MaintenanceStateHelper.class);
    Cluster cluster = createNiceMock(Cluster.class);

    // requests
    HostRequest request1 = new HostRequest("host1", "cluster1", Collections.<String, String>emptyMap());
    Set<HostRequest> setRequests = Collections.singleton(request1);

    // expectations
    // constructor init
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(null);
    expect(injector.getInstance(MaintenanceStateHelper.class)).andReturn(maintHelper);
    expect(injector.getInstance(KerberosHelper.class)).andReturn(createNiceMock(KerberosHelper.class));

    // getHosts
    expect(clusters.getCluster("cluster1")).andReturn(cluster);
    expect(clusters.getHost("host1")).andThrow(new HostNotFoundException("host1"));

    final InetAddress mock = createMock(InetAddress.class);
    mockStatic(InetAddress.class);
    expect(InetAddress.getLocalHost()).andReturn(mock);
    replayAll();
    // replay mocks
    replay(maintHelper, injector, clusters, cluster);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);

    // assert that exception is thrown in case where there is a single request
    try {
      getHosts(controller, setRequests);
      fail("expected HostNotFoundException");
    } catch (HostNotFoundException e) {
      // expected
    }
    assertSame(controller, controllerCapture.getValue());
    verify(injector, clusters, cluster);
  }

  /**
   * Ensure that HostNotFoundException is propagated in case where there is a single request.
   */
  @Test
  public void testGetHosts___HostNotFoundException_HostNotAssociatedWithCluster() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = new Capture<AmbariManagementController>();
    Clusters clusters = createNiceMock(Clusters.class);
    MaintenanceStateHelper maintHelper = createNiceMock(MaintenanceStateHelper.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Host host = createNiceMock(Host.class);

    // requests
    HostRequest request1 = new HostRequest("host1", "cluster1", Collections.<String, String>emptyMap());
    Set<HostRequest> setRequests = Collections.singleton(request1);

    // expectations
    // constructor init
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(null);
    expect(injector.getInstance(MaintenanceStateHelper.class)).andReturn(maintHelper);
    expect(injector.getInstance(KerberosHelper.class)).andReturn(createNiceMock(KerberosHelper.class));

    // getHosts
    expect(clusters.getCluster("cluster1")).andReturn(cluster);
    expect(clusters.getHost("host1")).andReturn(host);
    expect(host.getHostName()).andReturn("host1").anyTimes();
    // because cluster is not in set will result in HostNotFoundException
    expect(clusters.getClustersForHost("host1")).andReturn(Collections.<Cluster>emptySet());

    final InetAddress mock = createMock(InetAddress.class);
    mockStatic(InetAddress.class);
    expect(InetAddress.getLocalHost()).andReturn(mock);
    replayAll();
    // replay mocks
    replay(maintHelper, injector, clusters, cluster, host);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);

    // assert that exception is thrown in case where there is a single request
    try {
      getHosts(controller, setRequests);
      fail("expected HostNotFoundException");
    } catch (HostNotFoundException e) {
      // expected
    }
    assertSame(controller, controllerCapture.getValue());
    verify(injector, clusters, cluster, host);
  }


  /**
   * Ensure that HostNotFoundException is handled where there are multiple requests as would be the
   * case when an OR predicate is provided in the query.
   */
  @Test
  public void testGetHosts___OR_Predicate_HostNotFoundException() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = new Capture<AmbariManagementController>();
    Clusters clusters = createNiceMock(Clusters.class);
    MaintenanceStateHelper maintHelper = createNiceMock(MaintenanceStateHelper.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Host host1 = createNiceMock(Host.class);
    Host host2 = createNiceMock(Host.class);
    HostResponse response = createNiceMock(HostResponse.class);
    HostResponse response2 = createNiceMock(HostResponse.class);

    // requests
    HostRequest request1 = new HostRequest("host1", "cluster1", Collections.<String, String>emptyMap());
    HostRequest request2 = new HostRequest("host2", "cluster1", Collections.<String, String>emptyMap());
    HostRequest request3 = new HostRequest("host3", "cluster1", Collections.<String, String>emptyMap());
    HostRequest request4 = new HostRequest("host4", "cluster1", Collections.<String, String>emptyMap());

    Set<HostRequest> setRequests = new HashSet<HostRequest>();
    setRequests.add(request1);
    setRequests.add(request2);
    setRequests.add(request3);
    setRequests.add(request4);

    // expectations
    // constructor init
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(null);
    expect(injector.getInstance(MaintenanceStateHelper.class)).andReturn(maintHelper);
    expect(injector.getInstance(KerberosHelper.class)).andReturn(createNiceMock(KerberosHelper.class));

    // getHosts
    expect(clusters.getCluster("cluster1")).andReturn(cluster).times(4);

    expect(clusters.getHost("host1")).andReturn(host1);
    expect(host1.getHostName()).andReturn("host1").anyTimes();
    expect(clusters.getClustersForHost("host1")).andReturn(Collections.singleton(cluster));
    expect(host1.convertToResponse()).andReturn(response);
    response.setClusterName("cluster1");

    expect(clusters.getHost("host2")).andReturn(host2);
    expect(host2.getHostName()).andReturn("host2").anyTimes();
    expect(clusters.getClustersForHost("host2")).andReturn(Collections.singleton(cluster));
    expect(host2.convertToResponse()).andReturn(response2);
    response2.setClusterName("cluster1");

    expect(clusters.getHost("host3")).andThrow(new HostNotFoundException("host3"));
    expect(clusters.getHost("host4")).andThrow(new HostNotFoundException("host4"));

    // replay mocks
    replay(maintHelper, injector, clusters, cluster, host1, host2, response, response2);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);
    Set<HostResponse> setResponses = getHosts(controller, setRequests);

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    assertEquals(2, setResponses.size());
    assertTrue(setResponses.contains(response));
    assertTrue(setResponses.contains(response2));

    verify(injector, clusters, cluster, host1, host2, response, response2);
  }

  public static void createHosts(AmbariManagementController controller, Set<HostRequest> requests) throws AmbariException {
    HostResourceProvider provider = getHostProvider(controller);
    Set<Map<String, Object>> properties = new HashSet<Map<String, Object>>();

    for (HostRequest request : requests) {
      Map<String, Object> requestProperties = new HashMap<String, Object>();
      requestProperties.put(HostResourceProvider.HOST_NAME_PROPERTY_ID, request.getHostname());
      requestProperties.put(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID, request.getClusterName());
      properties.add(requestProperties);
    }
    provider.createHosts(PropertyHelper.getCreateRequest(properties, Collections.<String, String>emptyMap()));
  }

  public static Set<HostResponse> getHosts(AmbariManagementController controller,
                                                 Set<HostRequest> requests) throws AmbariException {
    HostResourceProvider provider = getHostProvider(controller);
    return provider.getHosts(requests);
  }

  public static void deleteHosts(AmbariManagementController controller, Set<HostRequest> requests)
      throws AmbariException {
    TopologyManager topologyManager = createNiceMock(TopologyManager.class);
    expect(topologyManager.getRequests(Collections.EMPTY_LIST)).andReturn(Collections.EMPTY_LIST).anyTimes();

    replay(topologyManager);

    HostResourceProvider provider = getHostProvider(controller);
    HostResourceProvider.setTopologyManager(topologyManager);
    provider.deleteHosts(requests);
  }

  public static void updateHosts(AmbariManagementController controller, Set<HostRequest> requests)
      throws AmbariException {
    HostResourceProvider provider = getHostProvider(controller);
    provider.updateHosts(requests);
  }
}
