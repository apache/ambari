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

import com.google.gson.Gson;
import com.google.inject.Injector;
import org.apache.ambari.server.*;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.state.*;
import org.easymock.Capture;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;

/**
 * AmbariManagementControllerImpl unit tests
 */
public class AmbariManagementControllerImplTest {

  @Test
  public void testGetClusters() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = new Capture<AmbariManagementController>();
    Clusters clusters = createNiceMock(Clusters.class);

    ClusterRequest request1 = new ClusterRequest(null, "cluster1", "1", Collections.<String>emptySet());
    Cluster cluster = createNiceMock(Cluster.class);
    ClusterResponse response = createNiceMock(ClusterResponse.class);

    Set<ClusterRequest> setRequests = new HashSet<ClusterRequest>();
    setRequests.add(request1);

    // expectations
    // constructor init
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(null);

    // getClusters
    expect(clusters.getCluster("cluster1")).andReturn(cluster);
    expect(cluster.convertToResponse()).andReturn(response);

    // replay mocks
    replay(injector, clusters, cluster, response);

    // test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);
    Set<ClusterResponse> setResponses = controller.getClusters(setRequests);

    // assert and verify
    assertEquals(1, setResponses.size());
    assertTrue(setResponses.contains(response));

    verify(injector, clusters, cluster, response);
  }

  /**
   * Ensure that ClusterNotFoundException is propagated in case where there is a single request.
   */
  @Test
  public void testGetClusters___ClusterNotFoundException() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = new Capture<AmbariManagementController>();
    Clusters clusters = createNiceMock(Clusters.class);

    // requests
    ClusterRequest request1 = new ClusterRequest(null, "cluster1", "1", Collections.<String>emptySet());

    Set<ClusterRequest> setRequests = new HashSet<ClusterRequest>();
    setRequests.add(request1);

    // expectations
    // constructor init
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(null);

    // getClusters
    expect(clusters.getCluster("cluster1")).andThrow(new ClusterNotFoundException("cluster1"));

    // replay mocks
    replay(injector, clusters);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);

    // assert that exception is thrown in case where there is a single request
    try {
      controller.getClusters(setRequests);
      fail("expected ClusterNotFoundException");
    } catch (ClusterNotFoundException e) {
      // expected
    }

    verify(injector, clusters);
  }

  /**
   * Ensure that ClusterNotFoundException is handled where there are multiple requests as would be the
   * case when an OR predicate is provided in the query.
   */
  @Test
  public void testGetClusters___OR_Predicate_ClusterNotFoundException() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = new Capture<AmbariManagementController>();
    Clusters clusters = createNiceMock(Clusters.class);

    Cluster cluster = createNiceMock(Cluster.class);
    Cluster cluster2 = createNiceMock(Cluster.class);
    ClusterResponse response = createNiceMock(ClusterResponse.class);
    ClusterResponse response2 = createNiceMock(ClusterResponse.class);

    // requests
    ClusterRequest request1 = new ClusterRequest(null, "cluster1", "1", Collections.<String>emptySet());
    ClusterRequest request2 = new ClusterRequest(null, "cluster2", "1", Collections.<String>emptySet());
    ClusterRequest request3 = new ClusterRequest(null, "cluster3", "1", Collections.<String>emptySet());
    ClusterRequest request4 = new ClusterRequest(null, "cluster4", "1", Collections.<String>emptySet());

    Set<ClusterRequest> setRequests = new HashSet<ClusterRequest>();
    setRequests.add(request1);
    setRequests.add(request2);
    setRequests.add(request3);
    setRequests.add(request4);

    // expectations
    // constructor init
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(null);

    // getClusters
    expect(clusters.getCluster("cluster1")).andThrow(new ClusterNotFoundException("cluster1"));
    expect(clusters.getCluster("cluster2")).andReturn(cluster);
    expect(clusters.getCluster("cluster3")).andReturn(cluster2);
    expect(clusters.getCluster("cluster4")).andThrow(new ClusterNotFoundException("cluster4"));

    expect(cluster.convertToResponse()).andReturn(response);
    expect(cluster2.convertToResponse()).andReturn(response2);
    // replay mocks
    replay(injector, clusters, cluster, cluster2, response, response2);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);
    Set<ClusterResponse> setResponses = controller.getClusters(setRequests);

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    assertEquals(2, setResponses.size());
    assertTrue(setResponses.contains(response));
    assertTrue(setResponses.contains(response2));

    verify(injector, clusters, cluster, cluster2, response, response2);
  }

  @Test
  public void testGetServices() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = new Capture<AmbariManagementController>();
    Clusters clusters = createNiceMock(Clusters.class);

    Cluster cluster = createNiceMock(Cluster.class);
    Service service = createNiceMock(Service.class);
    ServiceResponse response = createNiceMock(ServiceResponse.class);

    // requests
    ServiceRequest request1 = new ServiceRequest("cluster1", "service1", Collections.<String, String>emptyMap(), null);

    Set<ServiceRequest> setRequests = new HashSet<ServiceRequest>();
    setRequests.add(request1);

    // expectations
    // constructor init
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(null);

    // getServices
    expect(clusters.getCluster("cluster1")).andReturn(cluster);
    expect(cluster.getService("service1")).andReturn(service);

    expect(service.convertToResponse()).andReturn(response);
    // replay mocks
    replay(injector, clusters, cluster, service, response);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);
    Set<ServiceResponse> setResponses = controller.getServices(setRequests);

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

    Cluster cluster = createNiceMock(Cluster.class);

    // requests
    ServiceRequest request1 = new ServiceRequest("cluster1", "service1", Collections.<String, String>emptyMap(), null);
    Set<ServiceRequest> setRequests = new HashSet<ServiceRequest>();
    setRequests.add(request1);

    // expectations
    // constructor init
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(null);

    // getServices
    expect(clusters.getCluster("cluster1")).andReturn(cluster);
    expect(cluster.getService("service1")).andThrow(new ServiceNotFoundException("custer1", "service1"));

    // replay mocks
    replay(injector, clusters, cluster);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);

    // assert that exception is thrown in case where there is a single request
    try {
      controller.getServices(setRequests);
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

    Cluster cluster = createNiceMock(Cluster.class);
    Service service1 = createNiceMock(Service.class);
    Service service2 = createNiceMock(Service.class);
    ServiceResponse response = createNiceMock(ServiceResponse.class);
    ServiceResponse response2 = createNiceMock(ServiceResponse.class);

    // requests
    ServiceRequest request1 = new ServiceRequest("cluster1", "service1", Collections.<String, String>emptyMap(), null);
    ServiceRequest request2 = new ServiceRequest("cluster1", "service2", Collections.<String, String>emptyMap(), null);
    ServiceRequest request3 = new ServiceRequest("cluster1", "service3", Collections.<String, String>emptyMap(), null);
    ServiceRequest request4 = new ServiceRequest("cluster1", "service4", Collections.<String, String>emptyMap(), null);

    Set<ServiceRequest> setRequests = new HashSet<ServiceRequest>();
    setRequests.add(request1);
    setRequests.add(request2);
    setRequests.add(request3);
    setRequests.add(request4);

    // expectations
    // constructor init
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(null);

    // getServices
    expect(clusters.getCluster("cluster1")).andReturn(cluster).times(4);
    expect(cluster.getService("service1")).andReturn(service1);
    expect(cluster.getService("service2")).andThrow(new ServiceNotFoundException("cluster1", "service2"));
    expect(cluster.getService("service3")).andThrow(new ServiceNotFoundException("cluster1", "service3"));
    expect(cluster.getService("service4")).andReturn(service2);

    expect(service1.convertToResponse()).andReturn(response);
    expect(service2.convertToResponse()).andReturn(response2);
    // replay mocks
    replay(injector, clusters, cluster, service1, service2, response, response2);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);
    Set<ServiceResponse> setResponses = controller.getServices(setRequests);

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    assertEquals(2, setResponses.size());
    assertTrue(setResponses.contains(response));
    assertTrue(setResponses.contains(response2));

    verify(injector, clusters, cluster, service1, service2, response, response2);
  }

  @Test
  public void testGetComponents() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = new Capture<AmbariManagementController>();
    Clusters clusters = createNiceMock(Clusters.class);

    Cluster cluster = createNiceMock(Cluster.class);
    Service service = createNiceMock(Service.class);
    ServiceComponent component = createNiceMock(ServiceComponent.class);
    ServiceComponentResponse response = createNiceMock(ServiceComponentResponse.class);

    // requests
    ServiceComponentRequest request1 = new ServiceComponentRequest("cluster1", "service1", "component1",
        Collections.<String, String>emptyMap(), null);

    Set<ServiceComponentRequest> setRequests = new HashSet<ServiceComponentRequest>();
    setRequests.add(request1);

    // expectations
    // constructor init
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(null);

    // getComponents
    expect(clusters.getCluster("cluster1")).andReturn(cluster);
    expect(cluster.getService("service1")).andReturn(service);
    expect(service.getServiceComponent("component1")).andReturn(component);

    expect(component.convertToResponse()).andReturn(response);
    // replay mocks
    replay(injector, clusters, cluster, service, component, response);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);
    Set<ServiceComponentResponse> setResponses = controller.getComponents(setRequests);

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    assertEquals(1, setResponses.size());
    assertTrue(setResponses.contains(response));

    verify(injector, clusters, cluster, service, component, response);
  }

  /**
   * Ensure that ServiceComponentNotFoundException is propagated in case where there is a single request.
   */
  @Test
  public void testGetComponents___ServiceComponentNotFoundException() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = new Capture<AmbariManagementController>();
    Clusters clusters = createNiceMock(Clusters.class);

    Cluster cluster = createNiceMock(Cluster.class);
    Service service = createNiceMock(Service.class);

    // requests
    ServiceComponentRequest request1 = new ServiceComponentRequest("cluster1", "service1", "component1",
        Collections.<String, String>emptyMap(), null);

    Set<ServiceComponentRequest> setRequests = new HashSet<ServiceComponentRequest>();
    setRequests.add(request1);

    // expectations
    // constructor init
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(null);

    // getComponents
    expect(clusters.getCluster("cluster1")).andReturn(cluster);
    expect(cluster.getService("service1")).andReturn(service);
    expect(service.getServiceComponent("component1")).andThrow(
        new ServiceComponentNotFoundException("cluster1", "service1", "component1"));
    // replay mocks
    replay(injector, clusters, cluster, service);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);

    // assert that exception is thrown in case where there is a single request
    try {
      controller.getComponents(setRequests);
      fail("expected ServiceComponentNotFoundException");
    } catch (ServiceComponentNotFoundException e) {
      // expected
    }

    assertSame(controller, controllerCapture.getValue());
    verify(injector, clusters, cluster, service);
  }

  /**
   * Ensure that ServiceComponentNotFoundException is handled where there are multiple requests as would be the
   * case when an OR predicate is provided in the query.
   */
  @Test
  public void testGetComponents___OR_Predicate_ServiceComponentNotFoundException() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = new Capture<AmbariManagementController>();
    Clusters clusters = createNiceMock(Clusters.class);

    Cluster cluster = createNiceMock(Cluster.class);
    Service service = createNiceMock(Service.class);
    ServiceComponent component1 = createNiceMock(ServiceComponent.class);
    ServiceComponent component2 = createNiceMock(ServiceComponent.class);
    ServiceComponentResponse response1 = createNiceMock(ServiceComponentResponse.class);
    ServiceComponentResponse response2 = createNiceMock(ServiceComponentResponse.class);

    // requests
    ServiceComponentRequest request1 = new ServiceComponentRequest("cluster1", "service1", "component1",
        Collections.<String, String>emptyMap(), null);
    ServiceComponentRequest request2 = new ServiceComponentRequest("cluster1", "service1", "component2",
        Collections.<String, String>emptyMap(), null);
    ServiceComponentRequest request3 = new ServiceComponentRequest("cluster1", "service1", "component3",
        Collections.<String, String>emptyMap(), null);
    ServiceComponentRequest request4 = new ServiceComponentRequest("cluster1", "service1", "component4",
        Collections.<String, String>emptyMap(), null);

    Set<ServiceComponentRequest> setRequests = new HashSet<ServiceComponentRequest>();
    setRequests.add(request1);
    setRequests.add(request2);
    setRequests.add(request3);
    setRequests.add(request4);

    // expectations
    // constructor init
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(null);

    // getComponents
    expect(clusters.getCluster("cluster1")).andReturn(cluster).times(4);
    expect(cluster.getService("service1")).andReturn(service).times(4);

    expect(service.getServiceComponent("component1")).andThrow(new ServiceComponentNotFoundException("cluster1", "service1", "component1"));
    expect(service.getServiceComponent("component2")).andThrow(new ServiceComponentNotFoundException("cluster1", "service1", "component2"));
    expect(service.getServiceComponent("component3")).andReturn(component1);
    expect(service.getServiceComponent("component4")).andReturn(component2);

    expect(component1.convertToResponse()).andReturn(response1);
    expect(component2.convertToResponse()).andReturn(response2);
    // replay mocks
    replay(injector, clusters, cluster, service, component1,  component2, response1, response2);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);
    Set<ServiceComponentResponse> setResponses = controller.getComponents(setRequests);

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    assertEquals(2, setResponses.size());
    assertTrue(setResponses.contains(response1));
    assertTrue(setResponses.contains(response2));

    verify(injector, clusters, cluster, service, component1,  component2, response1, response2);
  }

  @Test
  public void testGetHosts() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = new Capture<AmbariManagementController>();
    Clusters clusters = createNiceMock(Clusters.class);

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

    // getHosts
    expect(clusters.getCluster("cluster1")).andReturn(cluster);
    expect(clusters.getHost("host1")).andReturn(host);
    expect(host.getHostName()).andReturn("host1").anyTimes();
    expect(clusters.getClustersForHost("host1")).andReturn(setCluster);
    expect(host.convertToResponse()).andReturn(response);
    response.setClusterName("cluster1");

    // replay mocks
    replay(injector, clusters, cluster, host, response);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);
    Set<HostResponse> setResponses = controller.getHosts(setRequests);

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

    Cluster cluster = createNiceMock(Cluster.class);

    // requests
    HostRequest request1 = new HostRequest("host1", "cluster1", Collections.<String, String>emptyMap());
    Set<HostRequest> setRequests = Collections.singleton(request1);

    // expectations
    // constructor init
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(null);

    // getHosts
    expect(clusters.getCluster("cluster1")).andReturn(cluster);
    expect(clusters.getHost("host1")).andThrow(new HostNotFoundException("host1"));

    // replay mocks
    replay(injector, clusters, cluster);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);

    // assert that exception is thrown in case where there is a single request
    try {
      controller.getHosts(setRequests);
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

    Cluster cluster = createNiceMock(Cluster.class);
    Host host = createNiceMock(Host.class);

    // requests
    HostRequest request1 = new HostRequest("host1", "cluster1", Collections.<String, String>emptyMap());
    Set<HostRequest> setRequests = Collections.singleton(request1);

    // expectations
    // constructor init
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(null);

    // getHosts
    expect(clusters.getCluster("cluster1")).andReturn(cluster);
    expect(clusters.getHost("host1")).andReturn(host);
    expect(host.getHostName()).andReturn("host1").anyTimes();
    // because cluster is not in set will result in HostNotFoundException
    expect(clusters.getClustersForHost("host1")).andReturn(Collections.<Cluster>emptySet());

    // replay mocks
    replay(injector, clusters, cluster, host);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);

    // assert that exception is thrown in case where there is a single request
    try {
      controller.getHosts(setRequests);
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
    replay(injector, clusters, cluster, host1, host2, response, response2);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);
    Set<HostResponse> setResponses = controller.getHosts(setRequests);

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    assertEquals(2, setResponses.size());
    assertTrue(setResponses.contains(response));
    assertTrue(setResponses.contains(response2));

    verify(injector, clusters, cluster, host1, host2, response, response2);
  }

  @Test
  public void testGetHostComponents() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = new Capture<AmbariManagementController>();
    Clusters clusters = createNiceMock(Clusters.class);
    StackId stack = createNiceMock(StackId.class);
    AmbariMetaInfo metaInfo = createStrictMock(AmbariMetaInfo.class);

    Cluster cluster = createNiceMock(Cluster.class);
    Host host = createNiceMock(Host.class);
    Service service = createNiceMock(Service.class);
    ServiceComponent component = createNiceMock(ServiceComponent.class);
    ServiceComponentHost componentHost = createNiceMock(ServiceComponentHost.class);
    ServiceComponentHostResponse response = createNiceMock(ServiceComponentHostResponse.class);

    // requests
    ServiceComponentHostRequest request1 = new ServiceComponentHostRequest(
        "cluster1", null, "component1", "host1", Collections.<String, String>emptyMap(), null);


    Set<ServiceComponentHostRequest> setRequests = new HashSet<ServiceComponentHostRequest>();
    setRequests.add(request1);

    // expectations
    // constructor init
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(null);

    // getHostComponent
    expect(clusters.getCluster("cluster1")).andReturn(cluster);
    expect(clusters.getClustersForHost("host1")).andReturn(Collections.singleton(cluster));

    expect(cluster.getDesiredStackVersion()).andReturn(stack);
    expect(stack.getStackName()).andReturn("stackName");
    expect(stack.getStackVersion()).andReturn("stackVersion");

    expect(metaInfo.getComponentToService("stackName", "stackVersion", "component1")).andReturn("service1");
    expect(cluster.getService("service1")).andReturn(service);
    expect(service.getServiceComponent("component1")).andReturn(component);
    expect(component.getName()).andReturn("component1").anyTimes();
    expect(component.getServiceComponentHost("host1")).andReturn(componentHost);
    expect(componentHost.convertToResponse()).andReturn(response);

    // replay mocks
    replay(injector, clusters, cluster, host, response, stack, metaInfo, service, component, componentHost);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);
    //need to set private field 'ambariMetaInfo' which is injected at runtime
    Class<?> c = controller.getClass();
    Field f = c.getDeclaredField("ambariMetaInfo");
    f.setAccessible(true);
    f.set(controller, metaInfo);

    Set<ServiceComponentHostResponse> setResponses = controller.getHostComponents(setRequests);

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    assertEquals(1, setResponses.size());
    assertTrue(setResponses.contains(response));

    verify(injector, clusters, cluster, host, response, stack, metaInfo, service, component, componentHost);
  }

  @Test
  public void testGetHostComponents___ServiceComponentHostNotFoundException() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = new Capture<AmbariManagementController>();
    Clusters clusters = createNiceMock(Clusters.class);
    StackId stack = createNiceMock(StackId.class);
    AmbariMetaInfo metaInfo = createStrictMock(AmbariMetaInfo.class);

    Cluster cluster = createNiceMock(Cluster.class);
    Host host = createNiceMock(Host.class);
    Service service = createNiceMock(Service.class);
    ServiceComponent component = createNiceMock(ServiceComponent.class);

    // requests
    ServiceComponentHostRequest request1 = new ServiceComponentHostRequest(
        "cluster1", null, "component1", "host1", Collections.<String, String>emptyMap(), null);


    Set<ServiceComponentHostRequest> setRequests = new HashSet<ServiceComponentHostRequest>();
    setRequests.add(request1);

    // expectations
    // constructor init
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(null);

    // getHostComponent
    expect(clusters.getCluster("cluster1")).andReturn(cluster);
    expect(clusters.getClustersForHost("host1")).andReturn(Collections.singleton(cluster));

    expect(cluster.getDesiredStackVersion()).andReturn(stack);
    expect(stack.getStackName()).andReturn("stackName");
    expect(stack.getStackVersion()).andReturn("stackVersion");

    expect(metaInfo.getComponentToService("stackName", "stackVersion", "component1")).andReturn("service1");
    expect(cluster.getService("service1")).andReturn(service);
    expect(service.getServiceComponent("component1")).andReturn(component);
    expect(component.getName()).andReturn("component1").anyTimes();
    expect(component.getServiceComponentHost("host1")).andThrow(
        new ServiceComponentHostNotFoundException("cluster1", "service1", "component1", "host1"));

    // replay mocks
    replay(injector, clusters, cluster, host, stack, metaInfo, service, component);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);
    //need to set private field 'ambariMetaInfo' which is injected at runtime
    Class<?> c = controller.getClass();
    Field f = c.getDeclaredField("ambariMetaInfo");
    f.setAccessible(true);
    f.set(controller, metaInfo);

    try {
      controller.getHostComponents(setRequests);
      fail("expected ServiceComponentHostNotFoundException");
    } catch (ServiceComponentHostNotFoundException e) {
      //expected
    }

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    verify(injector, clusters, cluster, host, stack, metaInfo, service, component);
  }

  @Test
  public void testGetHostComponents___OR_Predicate_ServiceComponentHostNotFoundException() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = new Capture<AmbariManagementController>();
    Clusters clusters = createNiceMock(Clusters.class);
    StackId stack = createNiceMock(StackId.class);
    AmbariMetaInfo metaInfo = createNiceMock(AmbariMetaInfo.class);

    Cluster cluster = createNiceMock(Cluster.class);
    Host host = createNiceMock(Host.class);
    Service service = createNiceMock(Service.class);
    ServiceComponent component = createNiceMock(ServiceComponent.class);
    ServiceComponent component2 = createNiceMock(ServiceComponent.class);
    ServiceComponent component3 = createNiceMock(ServiceComponent.class);

    ServiceComponentHost componentHost1 = createNiceMock(ServiceComponentHost.class);
    ServiceComponentHost componentHost2 = createNiceMock(ServiceComponentHost.class);
    ServiceComponentHostResponse response1 = createNiceMock(ServiceComponentHostResponse.class);
    ServiceComponentHostResponse response2 = createNiceMock(ServiceComponentHostResponse.class);

    // requests
    ServiceComponentHostRequest request1 = new ServiceComponentHostRequest(
        "cluster1", null, "component1", "host1", Collections.<String, String>emptyMap(), null);

    ServiceComponentHostRequest request2 = new ServiceComponentHostRequest(
        "cluster1", null, "component2", "host1", Collections.<String, String>emptyMap(), null);

    ServiceComponentHostRequest request3 = new ServiceComponentHostRequest(
        "cluster1", null, "component3", "host1", Collections.<String, String>emptyMap(), null);


    Set<ServiceComponentHostRequest> setRequests = new HashSet<ServiceComponentHostRequest>();
    setRequests.add(request1);
    setRequests.add(request2);
    setRequests.add(request3);

    // expectations
    // constructor init
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(null);

    // getHostComponent
    expect(clusters.getCluster("cluster1")).andReturn(cluster).times(3);
    expect(clusters.getClustersForHost("host1")).andReturn(Collections.singleton(cluster)).anyTimes();
    expect(cluster.getService("service1")).andReturn(service).times(3);

    expect(cluster.getDesiredStackVersion()).andReturn(stack).anyTimes();
    expect(stack.getStackName()).andReturn("stackName").anyTimes();
    expect(stack.getStackVersion()).andReturn("stackVersion").anyTimes();


    expect(metaInfo.getComponentToService("stackName", "stackVersion", "component1")).andReturn("service1");
    expect(service.getServiceComponent("component1")).andReturn(component);
    expect(component.getName()).andReturn("component1");
    expect(component.getServiceComponentHost("host1")).andReturn(componentHost1);
    expect(componentHost1.convertToResponse()).andReturn(response1);

    expect(metaInfo.getComponentToService("stackName", "stackVersion", "component2")).andReturn("service1");
    expect(service.getServiceComponent("component2")).andReturn(component2);
    expect(component2.getName()).andReturn("component2");
    expect(component2.getServiceComponentHost("host1")).andThrow(
        new ServiceComponentHostNotFoundException("cluster1", "service1", "component2", "host1"));


    expect(metaInfo.getComponentToService("stackName", "stackVersion", "component3")).andReturn("service1");
    expect(service.getServiceComponent("component3")).andReturn(component3);
    expect(component3.getName()).andReturn("component3");
    expect(component3.getServiceComponentHost("host1")).andReturn(componentHost2);
    expect(componentHost2.convertToResponse()).andReturn(response2);

    // replay mocks
    replay(injector, clusters, cluster, host, stack, metaInfo, service, component, component2, component3,
        componentHost1, componentHost2, response1, response2);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);
    //need to set private field 'ambariMetaInfo' which is injected at runtime
    Class<?> c = controller.getClass();
    Field f = c.getDeclaredField("ambariMetaInfo");
    f.setAccessible(true);
    f.set(controller, metaInfo);

    Set<ServiceComponentHostResponse> setResponses = controller.getHostComponents(setRequests);

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    assertEquals(2, setResponses.size());
    assertTrue(setResponses.contains(response1));
    assertTrue(setResponses.contains(response2));

    verify(injector, clusters, cluster, host, stack, metaInfo, service, component, component2, component3,
        componentHost1, componentHost2, response1, response2);
  }

  @Test
  public void testGetHostComponents___OR_Predicate_ServiceNotFoundException() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = new Capture<AmbariManagementController>();
    Clusters clusters = createNiceMock(Clusters.class);
    StackId stack = createNiceMock(StackId.class);
    AmbariMetaInfo metaInfo = createNiceMock(AmbariMetaInfo.class);

    Cluster cluster = createNiceMock(Cluster.class);
    Host host = createNiceMock(Host.class);
    Service service = createNiceMock(Service.class);
    ServiceComponent component = createNiceMock(ServiceComponent.class);
    ServiceComponent component2 = createNiceMock(ServiceComponent.class);
    ServiceComponent component3 = createNiceMock(ServiceComponent.class);

    ServiceComponentHost componentHost1 = createNiceMock(ServiceComponentHost.class);
    ServiceComponentHost componentHost2 = createNiceMock(ServiceComponentHost.class);
    ServiceComponentHostResponse response1 = createNiceMock(ServiceComponentHostResponse.class);
    ServiceComponentHostResponse response2 = createNiceMock(ServiceComponentHostResponse.class);

    // requests
    ServiceComponentHostRequest request1 = new ServiceComponentHostRequest(
        "cluster1", null, "component1", "host1", Collections.<String, String>emptyMap(), null);

    ServiceComponentHostRequest request2 = new ServiceComponentHostRequest(
        "cluster1", null, "component2", "host1", Collections.<String, String>emptyMap(), null);

    ServiceComponentHostRequest request3 = new ServiceComponentHostRequest(
        "cluster1", null, "component3", "host1", Collections.<String, String>emptyMap(), null);


    Set<ServiceComponentHostRequest> setRequests = new HashSet<ServiceComponentHostRequest>();
    setRequests.add(request1);
    setRequests.add(request2);
    setRequests.add(request3);

    // expectations
    // constructor init
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(null);

    // getHostComponent
    expect(clusters.getCluster("cluster1")).andReturn(cluster).times(3);
    expect(clusters.getClustersForHost("host1")).andReturn(Collections.singleton(cluster)).anyTimes();
    //expect(cluster.getService("service1")).andReturn(service).times(3);

    expect(cluster.getDesiredStackVersion()).andReturn(stack).anyTimes();
    expect(stack.getStackName()).andReturn("stackName").anyTimes();
    expect(stack.getStackVersion()).andReturn("stackVersion").anyTimes();


    expect(metaInfo.getComponentToService("stackName", "stackVersion", "component1")).andReturn("service1");
    expect(cluster.getService("service1")).andReturn(service);
    expect(service.getServiceComponent("component1")).andReturn(component);
    expect(component.getName()).andReturn("component1");
    expect(component.getServiceComponentHost("host1")).andReturn(componentHost1);
    expect(componentHost1.convertToResponse()).andReturn(response1);

    expect(metaInfo.getComponentToService("stackName", "stackVersion", "component2")).andReturn("service2");
    expect(cluster.getService("service2")).andThrow(new ServiceNotFoundException("cluster1", "service2"));

    expect(metaInfo.getComponentToService("stackName", "stackVersion", "component3")).andReturn("service1");
    expect(cluster.getService("service1")).andReturn(service);
    expect(service.getServiceComponent("component3")).andReturn(component3);
    expect(component3.getName()).andReturn("component3");
    expect(component3.getServiceComponentHost("host1")).andReturn(componentHost2);
    expect(componentHost2.convertToResponse()).andReturn(response2);

    // replay mocks
    replay(injector, clusters, cluster, host, stack, metaInfo, service, component, component2, component3,
        componentHost1, componentHost2, response1, response2);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);
    //need to set private field 'ambariMetaInfo' which is injected at runtime
    Class<?> c = controller.getClass();
    Field f = c.getDeclaredField("ambariMetaInfo");
    f.setAccessible(true);
    f.set(controller, metaInfo);

    Set<ServiceComponentHostResponse> setResponses = controller.getHostComponents(setRequests);

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    assertEquals(2, setResponses.size());
    assertTrue(setResponses.contains(response1));
    assertTrue(setResponses.contains(response2));

    verify(injector, clusters, cluster, host, stack, metaInfo, service, component, component2, component3,
        componentHost1, componentHost2, response1, response2);
  }

  @Test
  public void testGetHostComponents___OR_Predicate_ServiceComponentNotFoundException() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = new Capture<AmbariManagementController>();
    Clusters clusters = createNiceMock(Clusters.class);
    StackId stack = createNiceMock(StackId.class);
    AmbariMetaInfo metaInfo = createNiceMock(AmbariMetaInfo.class);

    Cluster cluster = createNiceMock(Cluster.class);
    Host host = createNiceMock(Host.class);
    Service service = createNiceMock(Service.class);
    Service service2 = createNiceMock(Service.class);
    ServiceComponent component = createNiceMock(ServiceComponent.class);
    ServiceComponent component2 = createNiceMock(ServiceComponent.class);
    ServiceComponent component3 = createNiceMock(ServiceComponent.class);

    ServiceComponentHost componentHost1 = createNiceMock(ServiceComponentHost.class);
    ServiceComponentHost componentHost2 = createNiceMock(ServiceComponentHost.class);
    ServiceComponentHostResponse response1 = createNiceMock(ServiceComponentHostResponse.class);
    ServiceComponentHostResponse response2 = createNiceMock(ServiceComponentHostResponse.class);

    // requests
    ServiceComponentHostRequest request1 = new ServiceComponentHostRequest(
        "cluster1", null, "component1", "host1", Collections.<String, String>emptyMap(), null);

    ServiceComponentHostRequest request2 = new ServiceComponentHostRequest(
        "cluster1", null, "component2", "host1", Collections.<String, String>emptyMap(), null);

    ServiceComponentHostRequest request3 = new ServiceComponentHostRequest(
        "cluster1", null, "component3", "host1", Collections.<String, String>emptyMap(), null);


    Set<ServiceComponentHostRequest> setRequests = new HashSet<ServiceComponentHostRequest>();
    setRequests.add(request1);
    setRequests.add(request2);
    setRequests.add(request3);

    // expectations
    // constructor init
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(null);

    // getHostComponent
    expect(clusters.getCluster("cluster1")).andReturn(cluster).times(3);
    expect(clusters.getClustersForHost("host1")).andReturn(Collections.singleton(cluster)).anyTimes();

    expect(cluster.getDesiredStackVersion()).andReturn(stack).anyTimes();
    expect(stack.getStackName()).andReturn("stackName").anyTimes();
    expect(stack.getStackVersion()).andReturn("stackVersion").anyTimes();


    expect(metaInfo.getComponentToService("stackName", "stackVersion", "component1")).andReturn("service1");
    expect(cluster.getService("service1")).andReturn(service);
    expect(service.getServiceComponent("component1")).andReturn(component);
    expect(component.getName()).andReturn("component1");
    expect(component.getServiceComponentHost("host1")).andReturn(componentHost1);
    expect(componentHost1.convertToResponse()).andReturn(response1);

    expect(metaInfo.getComponentToService("stackName", "stackVersion", "component2")).andReturn("service2");
    expect(cluster.getService("service2")).andReturn(service2);
    expect(service2.getServiceComponent("component2")).
        andThrow(new ServiceComponentNotFoundException("cluster1", "service2", "component2"));

    expect(metaInfo.getComponentToService("stackName", "stackVersion", "component3")).andReturn("service1");
    expect(cluster.getService("service1")).andReturn(service);
    expect(service.getServiceComponent("component3")).andReturn(component3);
    expect(component3.getName()).andReturn("component3");
    expect(component3.getServiceComponentHost("host1")).andReturn(componentHost2);
    expect(componentHost2.convertToResponse()).andReturn(response2);

    // replay mocks
    replay(injector, clusters, cluster, host, stack, metaInfo, service, service2, component, component2, component3,
        componentHost1, componentHost2, response1, response2);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);
    //need to set private field 'ambariMetaInfo' which is injected at runtime
    Class<?> c = controller.getClass();
    Field f = c.getDeclaredField("ambariMetaInfo");
    f.setAccessible(true);
    f.set(controller, metaInfo);

    Set<ServiceComponentHostResponse> setResponses = controller.getHostComponents(setRequests);

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    assertEquals(2, setResponses.size());
    assertTrue(setResponses.contains(response1));
    assertTrue(setResponses.contains(response2));

    verify(injector, clusters, cluster, host, stack, metaInfo, service, service2, component, component2, component3,
        componentHost1, componentHost2, response1, response2);
  }

  @Test
  public void testGetHostComponents___OR_Predicate_HostNotFoundException_hostProvidedInQuery() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = new Capture<AmbariManagementController>();
    Clusters clusters = createNiceMock(Clusters.class);
    StackId stack = createNiceMock(StackId.class);
    AmbariMetaInfo metaInfo = createNiceMock(AmbariMetaInfo.class);

    Cluster cluster = createNiceMock(Cluster.class);
    Host host = createNiceMock(Host.class);
    Service service = createNiceMock(Service.class);
    Service service2 = createNiceMock(Service.class);
    ServiceComponent component = createNiceMock(ServiceComponent.class);
    ServiceComponent component2 = createNiceMock(ServiceComponent.class);
    ServiceComponent component3 = createNiceMock(ServiceComponent.class);

    ServiceComponentHost componentHost1 = createNiceMock(ServiceComponentHost.class);
    ServiceComponentHost componentHost2 = createNiceMock(ServiceComponentHost.class);
    ServiceComponentHostResponse response1 = createNiceMock(ServiceComponentHostResponse.class);
    ServiceComponentHostResponse response2 = createNiceMock(ServiceComponentHostResponse.class);

    // requests
    ServiceComponentHostRequest request1 = new ServiceComponentHostRequest(
        "cluster1", null, "component1", null, Collections.<String, String>emptyMap(), null);

    ServiceComponentHostRequest request2 = new ServiceComponentHostRequest(
        "cluster1", null, "component2", "host2", Collections.<String, String>emptyMap(), null);

    ServiceComponentHostRequest request3 = new ServiceComponentHostRequest(
        "cluster1", null, "component3", null, Collections.<String, String>emptyMap(), null);


    Set<ServiceComponentHostRequest> setRequests = new HashSet<ServiceComponentHostRequest>();
    setRequests.add(request1);
    setRequests.add(request2);
    setRequests.add(request3);

    // expectations
    // constructor init
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(null);

    // getHostComponent
    expect(clusters.getCluster("cluster1")).andReturn(cluster).times(3);
    //expect(clusters.getClustersForHost("host1")).andReturn(Collections.singleton(cluster)).anyTimes();

    expect(cluster.getDesiredStackVersion()).andReturn(stack).anyTimes();
    expect(stack.getStackName()).andReturn("stackName").anyTimes();
    expect(stack.getStackVersion()).andReturn("stackVersion").anyTimes();

    expect(metaInfo.getComponentToService("stackName", "stackVersion", "component1")).andReturn("service1");
    expect(cluster.getService("service1")).andReturn(service);
    expect(service.getServiceComponent("component1")).andReturn(component);
    expect(component.getName()).andReturn("component1");
    expect(component.getServiceComponentHosts()).andReturn(Collections.singletonMap("foo", componentHost1));
    expect(componentHost1.convertToResponse()).andReturn(response1);

    expect(clusters.getClustersForHost("host2")).andThrow(new HostNotFoundException("host2"));

    expect(metaInfo.getComponentToService("stackName", "stackVersion", "component3")).andReturn("service1");
    expect(cluster.getService("service1")).andReturn(service);
    expect(service.getServiceComponent("component3")).andReturn(component3);
    expect(component3.getName()).andReturn("component3");
    expect(component3.getServiceComponentHosts()).andReturn(Collections.singletonMap("foo", componentHost2));
    expect(componentHost2.convertToResponse()).andReturn(response2);

    // replay mocks
    replay(injector, clusters, cluster, host, stack, metaInfo, service, service2, component, component2, component3,
        componentHost1, componentHost2, response1, response2);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);
    //need to set private field 'ambariMetaInfo' which is injected at runtime
    Class<?> c = controller.getClass();
    Field f = c.getDeclaredField("ambariMetaInfo");
    f.setAccessible(true);
    f.set(controller, metaInfo);

    Set<ServiceComponentHostResponse> setResponses = controller.getHostComponents(setRequests);

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    assertEquals(2, setResponses.size());
    assertTrue(setResponses.contains(response1));
    assertTrue(setResponses.contains(response2));

    verify(injector, clusters, cluster, host, stack, metaInfo, service, service2, component, component2, component3,
        componentHost1, componentHost2, response1, response2);
  }

  @Test
  public void testGetHostComponents___OR_Predicate_HostNotFoundException_hostProvidedInURL() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = new Capture<AmbariManagementController>();
    Clusters clusters = createNiceMock(Clusters.class);
    StackId stack = createNiceMock(StackId.class);
    AmbariMetaInfo metaInfo = createNiceMock(AmbariMetaInfo.class);

    Cluster cluster = createNiceMock(Cluster.class);

    // requests
    ServiceComponentHostRequest request1 = new ServiceComponentHostRequest(
        "cluster1", null, "component1", "host1", Collections.<String, String>emptyMap(), null);

    ServiceComponentHostRequest request2 = new ServiceComponentHostRequest(
        "cluster1", null, "component2", "host1", Collections.<String, String>emptyMap(), null);

    ServiceComponentHostRequest request3 = new ServiceComponentHostRequest(
        "cluster1", null, "component3", "host1", Collections.<String, String>emptyMap(), null);


    Set<ServiceComponentHostRequest> setRequests = new HashSet<ServiceComponentHostRequest>();
    setRequests.add(request1);
    setRequests.add(request2);
    setRequests.add(request3);

    // expectations
    // constructor init
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(null);

    // getHostComponent
    expect(clusters.getCluster("cluster1")).andReturn(cluster);
    expect(clusters.getClustersForHost("host1")).andThrow(new HostNotFoundException("host1"));

    // replay mocks
    replay(injector, clusters, cluster, stack, metaInfo);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);
    //need to set private field 'ambariMetaInfo' which is injected at runtime
    Class<?> c = controller.getClass();
    Field f = c.getDeclaredField("ambariMetaInfo");
    f.setAccessible(true);
    f.set(controller, metaInfo);

    try {
      controller.getHostComponents(setRequests);
      fail("expected exception");
    } catch (AmbariException e) {
      // expected
    }

    // assert and verify
    assertSame(controller, controllerCapture.getValue());

    verify(injector, clusters, cluster, stack, metaInfo);
  }

  @Test
  public void testGetHostComponents___OR_Predicate_ClusterNotFoundException() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = new Capture<AmbariManagementController>();
    Clusters clusters = createNiceMock(Clusters.class);
    StackId stack = createNiceMock(StackId.class);
    AmbariMetaInfo metaInfo = createNiceMock(AmbariMetaInfo.class);

    // requests
    ServiceComponentHostRequest request1 = new ServiceComponentHostRequest(
        "cluster1", null, "component1", "host1", Collections.<String, String>emptyMap(), null);

    ServiceComponentHostRequest request2 = new ServiceComponentHostRequest(
        "cluster1", null, "component2", "host2", Collections.<String, String>emptyMap(), null);

    ServiceComponentHostRequest request3 = new ServiceComponentHostRequest(
        "cluster1", null, "component3", "host1", Collections.<String, String>emptyMap(), null);


    Set<ServiceComponentHostRequest> setRequests = new HashSet<ServiceComponentHostRequest>();
    setRequests.add(request1);
    setRequests.add(request2);
    setRequests.add(request3);

    // expectations
    // constructor init
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(null);

    // getHostComponent
    expect(clusters.getCluster("cluster1")).andThrow(new ClusterNotFoundException("cluster1"));

    // replay mocks
    replay(injector, clusters, stack, metaInfo);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);
    //need to set private field 'ambariMetaInfo' which is injected at runtime
    Class<?> c = controller.getClass();
    Field f = c.getDeclaredField("ambariMetaInfo");
    f.setAccessible(true);
    f.set(controller, metaInfo);

    try {
      controller.getHostComponents(setRequests);
      fail("expected exception");
    } catch (ParentObjectNotFoundException e) {
      //expected
    }

    // assert and verify
    assertSame(controller, controllerCapture.getValue());

    verify(injector, clusters,stack, metaInfo);
  }

  @Test
  public void testGetHostComponents___NullHostName() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = new Capture<AmbariManagementController>();
    Clusters clusters = createNiceMock(Clusters.class);
    StackId stack = createNiceMock(StackId.class);
    AmbariMetaInfo metaInfo = createStrictMock(AmbariMetaInfo.class);

    Cluster cluster = createNiceMock(Cluster.class);
    Service service = createNiceMock(Service.class);
    ServiceComponent component = createNiceMock(ServiceComponent.class);
    ServiceComponentHost componentHost1 = createNiceMock(ServiceComponentHost.class);
    ServiceComponentHost componentHost2 = createNiceMock(ServiceComponentHost.class);
    ServiceComponentHostResponse response1 = createNiceMock(ServiceComponentHostResponse.class);
    ServiceComponentHostResponse response2 = createNiceMock(ServiceComponentHostResponse.class);

    // requests
    ServiceComponentHostRequest request1 = new ServiceComponentHostRequest(
        "cluster1", null, "component1", null, Collections.<String, String>emptyMap(), null);


    Set<ServiceComponentHostRequest> setRequests = new HashSet<ServiceComponentHostRequest>();
    setRequests.add(request1);

    Map<String, ServiceComponentHost> mapHostComponents = new HashMap<String, ServiceComponentHost>();
    mapHostComponents.put("foo", componentHost1);
    mapHostComponents.put("bar", componentHost2);


    // expectations
    // constructor init
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(null);

    // getHostComponent
    expect(clusters.getCluster("cluster1")).andReturn(cluster);

    expect(cluster.getDesiredStackVersion()).andReturn(stack);
    expect(stack.getStackName()).andReturn("stackName");
    expect(stack.getStackVersion()).andReturn("stackVersion");

    expect(metaInfo.getComponentToService("stackName", "stackVersion", "component1")).andReturn("service1");
    expect(cluster.getService("service1")).andReturn(service);
    expect(service.getServiceComponent("component1")).andReturn(component);
    expect(component.getName()).andReturn("component1").anyTimes();

    expect(component.getServiceComponentHosts()).andReturn(mapHostComponents);
    expect(componentHost1.convertToResponse()).andReturn(response1);
    expect(componentHost2.convertToResponse()).andReturn(response2);

    // replay mocks
    replay(injector, clusters, cluster, response1, response2, stack, metaInfo, service, component, componentHost1, componentHost2);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);
    //need to set private field 'ambariMetaInfo' which is injected at runtime
    Class<?> c = controller.getClass();
    Field f = c.getDeclaredField("ambariMetaInfo");
    f.setAccessible(true);
    f.set(controller, metaInfo);

    Set<ServiceComponentHostResponse> setResponses = controller.getHostComponents(setRequests);

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    assertEquals(2, setResponses.size());
    assertTrue(setResponses.contains(response1));
    assertTrue(setResponses.contains(response2));

    verify(injector, clusters, cluster, response1, response2, stack, metaInfo, service, component, componentHost1, componentHost2);
  }

  @Test
  public void testGetHostComponents___NullHostName_NullComponentName() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = new Capture<AmbariManagementController>();
    Clusters clusters = createNiceMock(Clusters.class);
    StackId stack = createNiceMock(StackId.class);
    AmbariMetaInfo metaInfo = createStrictMock(AmbariMetaInfo.class);

    Cluster cluster = createNiceMock(Cluster.class);
    Service service1 = createNiceMock(Service.class);
    Service service2 = createNiceMock(Service.class);
    ServiceComponent component1 = createNiceMock(ServiceComponent.class);
    ServiceComponent component2 = createNiceMock(ServiceComponent.class);
    ServiceComponentHost componentHost1 = createNiceMock(ServiceComponentHost.class);
    ServiceComponentHost componentHost2 = createNiceMock(ServiceComponentHost.class);
    ServiceComponentHost componentHost3 = createNiceMock(ServiceComponentHost.class);
    ServiceComponentHostResponse response1 = createNiceMock(ServiceComponentHostResponse.class);
    ServiceComponentHostResponse response2 = createNiceMock(ServiceComponentHostResponse.class);
    ServiceComponentHostResponse response3 = createNiceMock(ServiceComponentHostResponse.class);

    // requests
    ServiceComponentHostRequest request1 = new ServiceComponentHostRequest(
        "cluster1", null, null, null, Collections.<String, String>emptyMap(), null);


    Set<ServiceComponentHostRequest> setRequests = new HashSet<ServiceComponentHostRequest>();
    setRequests.add(request1);

    Map<String, Service> mapServices = new HashMap<String, Service>();
    mapServices.put("foo", service1);
    mapServices.put("bar", service2);

    Map<String, ServiceComponentHost> mapHostComponents = new HashMap<String, ServiceComponentHost>();
    mapHostComponents.put("foo", componentHost1);
    mapHostComponents.put("bar", componentHost2);


    // expectations
    // constructor init
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(null);

    // getHostComponent
    expect(clusters.getCluster("cluster1")).andReturn(cluster);

    expect(cluster.getServices()).andReturn(mapServices);
    expect(service1.getServiceComponents()).andReturn(Collections.singletonMap("foo", component1));
    expect(service2.getServiceComponents()).andReturn(Collections.singletonMap("bar", component2));

    expect(component1.getName()).andReturn("component1").anyTimes();
    expect(component2.getName()).andReturn("component2").anyTimes();

    expect(component1.getServiceComponentHosts()).andReturn(mapHostComponents);
    expect(componentHost1.convertToResponse()).andReturn(response1);
    expect(componentHost2.convertToResponse()).andReturn(response2);

    expect(component2.getServiceComponentHosts()).andReturn(Collections.singletonMap("foobar", componentHost3));
    expect(componentHost3.convertToResponse()).andReturn(response3);

    // replay mocks
    replay(injector, clusters, cluster, response1, response2, response3, stack, metaInfo, service1, service2,
        component1, component2, componentHost1, componentHost2, componentHost3);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);
    //need to set private field 'ambariMetaInfo' which is injected at runtime
    Class<?> c = controller.getClass();
    Field f = c.getDeclaredField("ambariMetaInfo");
    f.setAccessible(true);
    f.set(controller, metaInfo);

    Set<ServiceComponentHostResponse> setResponses = controller.getHostComponents(setRequests);

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    assertEquals(3, setResponses.size());
    assertTrue(setResponses.contains(response1));
    assertTrue(setResponses.contains(response2));
    assertTrue(setResponses.contains(response3));

    verify(injector, clusters, cluster, response1, response2, response3, stack, metaInfo, service1, service2,
        component1, component2, componentHost1, componentHost2, componentHost3);
  }

  //todo other resources
}
