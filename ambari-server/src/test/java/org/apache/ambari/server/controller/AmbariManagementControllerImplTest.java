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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ClusterNotFoundException;
import org.apache.ambari.server.HostNotFoundException;
import org.apache.ambari.server.ParentObjectNotFoundException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.ServiceComponentHostNotFoundException;
import org.apache.ambari.server.ServiceComponentNotFoundException;
import org.apache.ambari.server.ServiceNotFoundException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostInstallEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostOpSucceededEvent;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

/**
 * AmbariManagementControllerImpl unit tests
 */
public class AmbariManagementControllerImplTest {


  @Test
  public void testgetAmbariServerURI() throws Exception {
    // create mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = new Capture<AmbariManagementController>();

    // set expectations
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(null);
    
    //replay
    replay(injector);
    
    
    AmbariManagementControllerImpl controller = new AmbariManagementControllerImpl(null, null, injector);
    
    class AmbariConfigsSetter{
       public void setConfigs(AmbariManagementController controller, String masterProtocol, String masterHostname, Integer masterPort) throws Exception{
         // masterProtocol
         Class<?> c = controller.getClass();
         Field f = c.getDeclaredField("masterProtocol");
         f.setAccessible(true);
         
         Field modifiersField = Field.class.getDeclaredField("modifiers");
         modifiersField.setAccessible(true);
         modifiersField.setInt(f, f.getModifiers() & ~Modifier.FINAL);
         
         f.set(controller, masterProtocol);
         
         // masterHostname
         f = c.getDeclaredField("masterHostname");
         f.setAccessible(true);
         
         modifiersField = Field.class.getDeclaredField("modifiers");
         modifiersField.setAccessible(true);
         modifiersField.setInt(f, f.getModifiers() & ~Modifier.FINAL);
         
         f.set(controller, masterHostname);
         
         // masterPort
         f = c.getDeclaredField("masterPort");
         f.setAccessible(true);
         
         modifiersField = Field.class.getDeclaredField("modifiers");
         modifiersField.setAccessible(true);
         modifiersField.setInt(f, f.getModifiers() & ~Modifier.FINAL);
         
         f.set(controller, masterPort);
       }
    }

    AmbariConfigsSetter ambariConfigsSetter = new AmbariConfigsSetter();
    
    ambariConfigsSetter.setConfigs(controller, "http", "hostname", 8080);
    assertEquals("http://hostname:8080/jdk_path", controller.getAmbariServerURI("/jdk_path"));
    
    ambariConfigsSetter.setConfigs(controller, "https", "somesecuredhost", 8443);
    assertEquals("https://somesecuredhost:8443/mysql_path", controller.getAmbariServerURI("/mysql_path"));

    ambariConfigsSetter.setConfigs(controller, "https", "othersecuredhost", 8443);
    assertEquals("https://othersecuredhost:8443/oracle/ojdbc/", controller.getAmbariServerURI("/oracle/ojdbc/"));
    
    verify(injector);
  }
  
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

    // getCluster
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

    // getCluster
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

    // getCluster
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

  @Test
  public void testMaintenanceAndDeleteStates() throws Exception {
    Map<String,String> mapRequestProps = new HashMap<String, String>();
    Injector injector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        Properties properties = new Properties();
        properties.setProperty(Configuration.SERVER_PERSISTENCE_TYPE_KEY, "in-memory");
        
        properties.setProperty(Configuration.METADETA_DIR_PATH,
            "src/main/resources/stacks");
        properties.setProperty(Configuration.SERVER_VERSION_FILE,
                "target/version");
        properties.setProperty(Configuration.OS_VERSION_KEY,
            "centos5");
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
      Gson gson = new Gson();
  
      clusters.addHost("host1");
      clusters.addHost("host2");
      clusters.addHost("host3");
      Host host = clusters.getHost("host1");
      host.setOsType("centos5");
      host.persist();
      host = clusters.getHost("host2");
      host.setOsType("centos5");
      host.persist();
      host = clusters.getHost("host3");
      host.setOsType("centos5");
      host.persist();
  
      ClusterRequest clusterRequest = new ClusterRequest(null, "c1", "HDP-1.2.0", null);
      amc.createCluster(clusterRequest);
  
      Set<ServiceRequest> serviceRequests = new HashSet<ServiceRequest>();
      serviceRequests.add(new ServiceRequest("c1", "HDFS", null, null));
  
      amc.createServices(serviceRequests);
  
      Type confType = new TypeToken<Map<String, String>>() {
      }.getType();
  
      ConfigurationRequest configurationRequest = new ConfigurationRequest("c1", "core-site", "version1",
          gson.<Map<String, String>>fromJson("{ \"fs.default.name\" : \"localhost:8020\"}", confType)
      );
      amc.createConfiguration(configurationRequest);
  
      configurationRequest = new ConfigurationRequest("c1", "hdfs-site", "version1",
          gson.<Map<String, String>>fromJson("{ \"dfs.datanode.data.dir.perm\" : \"750\"}", confType)
      );
      amc.createConfiguration(configurationRequest);
  
      configurationRequest = new ConfigurationRequest("c1", "global", "version1",
          gson.<Map<String, String>>fromJson("{ \"hbase_hdfs_root_dir\" : \"/apps/hbase/\"}", confType)
      );
      amc.createConfiguration(configurationRequest);
  
  
      serviceRequests.clear();
      serviceRequests.add(new ServiceRequest("c1", "HDFS",
          gson.<Map<String, String>>fromJson("{\"core-site\": \"version1\", \"hdfs-site\": \"version1\", \"global\" : \"version1\" }", confType)
          , null));
  
      amc.updateServices(serviceRequests, mapRequestProps, true, false);
  
      Set<ServiceComponentRequest> serviceComponentRequests = new HashSet<ServiceComponentRequest>();
      serviceComponentRequests.add(new ServiceComponentRequest("c1", "HDFS", "NAMENODE", null, null));
      serviceComponentRequests.add(new ServiceComponentRequest("c1", "HDFS", "SECONDARY_NAMENODE", null, null));
      serviceComponentRequests.add(new ServiceComponentRequest("c1", "HDFS", "DATANODE", null, null));
      serviceComponentRequests.add(new ServiceComponentRequest("c1", "HDFS", "HDFS_CLIENT", null, null));
  
      amc.createComponents(serviceComponentRequests);
  
      Set<HostRequest> hostRequests = new HashSet<HostRequest>();
      hostRequests.add(new HostRequest("host1", "c1", null));
      hostRequests.add(new HostRequest("host2", "c1", null));
      hostRequests.add(new HostRequest("host3", "c1", null));
  
      amc.createHosts(hostRequests);
  
      Set<ServiceComponentHostRequest> componentHostRequests = new HashSet<ServiceComponentHostRequest>();
      componentHostRequests.add(new ServiceComponentHostRequest("c1", null, "DATANODE", "host1", null, null));
      componentHostRequests.add(new ServiceComponentHostRequest("c1", null, "NAMENODE", "host1", null, null));
      componentHostRequests.add(new ServiceComponentHostRequest("c1", null, "SECONDARY_NAMENODE", "host1", null, null));
      componentHostRequests.add(new ServiceComponentHostRequest("c1", null, "DATANODE", "host2", null, null));
      componentHostRequests.add(new ServiceComponentHostRequest("c1", null, "DATANODE", "host3", null, null));
  
  
      amc.createHostComponents(componentHostRequests);
  
      serviceRequests.clear();
      serviceRequests.add(new ServiceRequest("c1", "HDFS", null, "INSTALLED"));
      amc.updateServices(serviceRequests, mapRequestProps, true, false);
  
      Cluster cluster = clusters.getCluster("c1");
      Map<String, ServiceComponentHost> namenodes = cluster.getService("HDFS").getServiceComponent("NAMENODE").getServiceComponentHosts();
      assertEquals(1, namenodes.size());
  
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
      componentHostRequests.add(new ServiceComponentHostRequest("c1", null, "NAMENODE", "host1", null, "MAINTENANCE"));
  
      amc.updateHostComponents(componentHostRequests, mapRequestProps, true);
  
      assertEquals(State.MAINTENANCE, componentHost.getState());
  
      componentHostRequests.clear();
      componentHostRequests.add(new ServiceComponentHostRequest("c1", null, "NAMENODE", "host1", null, "INSTALLED"));
  
      amc.updateHostComponents(componentHostRequests, mapRequestProps, true);
  
      assertEquals(State.INSTALLED, componentHost.getState());
  
      componentHostRequests.clear();
      componentHostRequests.add(new ServiceComponentHostRequest("c1", null, "NAMENODE", "host1", null, "MAINTENANCE"));
  
      amc.updateHostComponents(componentHostRequests, mapRequestProps, true);
  
      assertEquals(State.MAINTENANCE, componentHost.getState());
  
      componentHostRequests.clear();
      componentHostRequests.add(new ServiceComponentHostRequest("c1", null, "NAMENODE", "host2", null, null));
  
      amc.createHostComponents(componentHostRequests);
  
      componentHostRequests.clear();
      componentHostRequests.add(new ServiceComponentHostRequest("c1", null, "NAMENODE", "host2", null, "INSTALLED"));
  
      amc.updateHostComponents(componentHostRequests, mapRequestProps, true);
  
      namenodes = cluster.getService("HDFS").getServiceComponent("NAMENODE").getServiceComponentHosts();
      assertEquals(2, namenodes.size());
  
      componentHost = namenodes.get("host2");
      componentHost.handleEvent(new ServiceComponentHostInstallEvent(componentHost.getServiceComponentName(), componentHost.getHostName(), System.currentTimeMillis(), "HDP-1.2.0"));
      componentHost.handleEvent(new ServiceComponentHostOpSucceededEvent(componentHost.getServiceComponentName(), componentHost.getHostName(), System.currentTimeMillis()));
  
      serviceRequests.clear();
      serviceRequests.add(new ServiceRequest("c1", "HDFS", null, "STARTED"));
  
      RequestStatusResponse response = amc.updateServices(serviceRequests,
        mapRequestProps, true, false);
      for (ShortTaskStatus shortTaskStatus : response.getTasks()) {
        assertFalse("host1".equals(shortTaskStatus.getHostName()) && "NAMENODE".equals(shortTaskStatus.getRole()));
      }
  
      componentHostRequests.clear();
      componentHostRequests.add(new ServiceComponentHostRequest("c1", null, "NAMENODE", "host1", null, null));
  
      amc.deleteHostComponents(componentHostRequests);
      namenodes = cluster.getService("HDFS").getServiceComponent("NAMENODE").getServiceComponentHosts();
      assertEquals(1, namenodes.size());
  
      // testing the behavior for runSmokeTest flag
      // piggybacking on this test to avoid setting up the mock cluster
      testRunSmokeTestFlag(mapRequestProps, amc, serviceRequests);
  
      // should be able to add the host component back
      componentHostRequests.clear();
      componentHostRequests.add(new ServiceComponentHostRequest("c1", null, "NAMENODE", "host1", null, null));
      amc.createHostComponents(componentHostRequests);
      namenodes = cluster.getService("HDFS").getServiceComponent("NAMENODE").getServiceComponentHosts();
      assertEquals(2, namenodes.size());
      
      
      // make unknown
      ServiceComponentHost sch = null;
      for (ServiceComponentHost tmp : cluster.getServiceComponentHosts("host2")) {
        if (tmp.getServiceComponentName().equals("DATANODE")) {
          tmp.setState(State.UNKNOWN);
          sch = tmp;
        }
      }
      assertNotNull(sch);
  
      // make maintenance
      componentHostRequests.clear();
      componentHostRequests.add(new ServiceComponentHostRequest("c1", null, "DATANODE", "host2", null, "MAINTENANCE"));
      amc.updateHostComponents(componentHostRequests, mapRequestProps, false);
      assertEquals(State.MAINTENANCE, sch.getState ());
      
      // confirm delete
      componentHostRequests.clear();
      componentHostRequests.add(new ServiceComponentHostRequest("c1", null, "DATANODE", "host2", null, null));
      amc.deleteHostComponents(componentHostRequests);
      
      sch = null;
      for (ServiceComponentHost tmp : cluster.getServiceComponentHosts("host2")) {
        if (tmp.getServiceComponentName().equals("DATANODE")) {
          sch = tmp;
        }
      }
      assertNull(sch);
    
      /*
      *Test remove service
      */
      serviceRequests.clear();
      serviceRequests.add(new ServiceRequest("c1", "HDFS", null, "INSTALLED"));
      amc.updateServices(serviceRequests, mapRequestProps, true, false);
      serviceRequests.clear();
      serviceRequests.add(new ServiceRequest("c1", null, null, null));
      assertEquals(1, amc.getServices(serviceRequests).size());
      serviceRequests.clear();
      serviceRequests.add(new ServiceRequest("c1", "HDFS", null, null));
      amc.deleteServices(serviceRequests);
      serviceRequests.clear();
      serviceRequests.add(new ServiceRequest("c1", null, null, null));     
      assertEquals(0, amc.getServices(serviceRequests).size());
      
      /*
      *Test add service again
      */
      serviceRequests.clear();
      serviceRequests.add(new ServiceRequest("c1", "HDFS", null, null));
      amc.createServices(serviceRequests);
      assertEquals(1, amc.getServices(serviceRequests).size());
      //Create new configs
      configurationRequest = new ConfigurationRequest("c1", "core-site", "version2",
          gson.<Map<String, String>>fromJson("{ \"fs.default.name\" : \"localhost:8020\"}", confType)
      );
      amc.createConfiguration(configurationRequest);
      configurationRequest = new ConfigurationRequest("c1", "hdfs-site", "version2",
          gson.<Map<String, String>>fromJson("{ \"dfs.datanode.data.dir.perm\" : \"750\"}", confType)
      );
      amc.createConfiguration(configurationRequest);
      configurationRequest = new ConfigurationRequest("c1", "global", "version2",
          gson.<Map<String, String>>fromJson("{ \"hbase_hdfs_root_dir\" : \"/apps/hbase/\"}", confType)
      );
      amc.createConfiguration(configurationRequest);    
      //Add configs to service
      serviceRequests.clear();
      serviceRequests.add(new ServiceRequest("c1", "HDFS",
          gson.<Map<String, String>>fromJson("{\"core-site\": \"version2\", \"hdfs-site\": \"version2\", \"global\" : \"version2\" }", confType)
          , null));
      amc.updateServices(serviceRequests, mapRequestProps, true, false);
      //Crate service components
      serviceComponentRequests = new HashSet<ServiceComponentRequest>();
      serviceComponentRequests.add(new ServiceComponentRequest("c1", "HDFS", "NAMENODE", null, null));
      serviceComponentRequests.add(new ServiceComponentRequest("c1", "HDFS", "SECONDARY_NAMENODE", null, null));
      serviceComponentRequests.add(new ServiceComponentRequest("c1", "HDFS", "DATANODE", null, null));
      serviceComponentRequests.add(new ServiceComponentRequest("c1", "HDFS", "HDFS_CLIENT", null, null));
      amc.createComponents(serviceComponentRequests);
      
      //Create ServiceComponentHosts
      componentHostRequests = new HashSet<ServiceComponentHostRequest>();
      componentHostRequests.add(new ServiceComponentHostRequest("c1", null, "DATANODE", "host1", null, null));
      componentHostRequests.add(new ServiceComponentHostRequest("c1", null, "NAMENODE", "host1", null, null));
      componentHostRequests.add(new ServiceComponentHostRequest("c1", null, "SECONDARY_NAMENODE", "host1", null, null));
      componentHostRequests.add(new ServiceComponentHostRequest("c1", null, "DATANODE", "host2", null, null));
      componentHostRequests.add(new ServiceComponentHostRequest("c1", null, "DATANODE", "host3", null, null));
      amc.createHostComponents(componentHostRequests);    
  
      
      namenodes = cluster.getService("HDFS").getServiceComponent("NAMENODE").getServiceComponentHosts();
      assertEquals(1, namenodes.size());
      Map<String, ServiceComponentHost> datanodes = cluster.getService("HDFS").getServiceComponent("DATANODE").getServiceComponentHosts();
      assertEquals(3, datanodes.size());
      Map<String, ServiceComponentHost> namenodes2 = cluster.getService("HDFS").getServiceComponent("SECONDARY_NAMENODE").getServiceComponentHosts();
      assertEquals(1, namenodes2.size());
    } finally {
      injector.getInstance(PersistService.class).stop();
    }    
  }

  private void testRunSmokeTestFlag(Map<String, String> mapRequestProps,
                                    AmbariManagementController amc,
                                    Set<ServiceRequest> serviceRequests)
      throws AmbariException {
    RequestStatusResponse response;//Starting HDFS service. No run_smoke_test flag is set, smoke

    //Stopping HDFS service
    serviceRequests.clear();
    serviceRequests.add(new ServiceRequest("c1", "HDFS", null, "INSTALLED"));
    response = amc.updateServices(serviceRequests, mapRequestProps, false,
      false);

    //Starting HDFS service. No run_smoke_test flag is set, smoke
    // test(HDFS_SERVICE_CHECK) won't run
    boolean runSmokeTest = false;
    serviceRequests.clear();
    serviceRequests.add(new ServiceRequest("c1", "HDFS", null, "STARTED"));
    response = amc.updateServices(serviceRequests, mapRequestProps,
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
    serviceRequests.add(new ServiceRequest("c1", "HDFS", null, "INSTALLED"));
    response = amc.updateServices(serviceRequests, mapRequestProps, false,
      false);

    //Starting HDFS service again.
    //run_smoke_test flag is set, smoke test will be run
    runSmokeTest = true;
    serviceRequests.clear();
    serviceRequests.add(new ServiceRequest("c1", "HDFS", null, "STARTED"));
    response = amc.updateServices(serviceRequests, mapRequestProps,
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


  @Test
  public void testScheduleSmokeTest() throws Exception {

    final String HOST1 = "host1";
    final String OS_TYPE = "centos5";
    final String STACK_ID = "HDP-2.0.3";
    final String CLUSTER_NAME = "c1";
    final String HDFS_SERVICE_CHECK_ROLE = "HDFS_SERVICE_CHECK";
    final String MAPREDUCE2_SERVICE_CHECK_ROLE = "MAPREDUCE2_SERVICE_CHECK";
    final String YARN_SERVICE_CHECK_ROLE = "YARN_SERVICE_CHECK";

    Map<String,String> mapRequestProps = Collections.<String,String>emptyMap();
    Injector injector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        Properties properties = new Properties();
        properties.setProperty(Configuration.SERVER_PERSISTENCE_TYPE_KEY, "in-memory");

        properties.setProperty(Configuration.METADETA_DIR_PATH,
            "src/main/resources/stacks");
        properties.setProperty(Configuration.SERVER_VERSION_FILE,
                "../version");
        properties.setProperty(Configuration.OS_VERSION_KEY, OS_TYPE);
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
      host.setOsType(OS_TYPE);
      host.persist();
  
      ClusterRequest clusterRequest = new ClusterRequest(null, CLUSTER_NAME, STACK_ID, null);
      amc.createCluster(clusterRequest);
  
      Set<ServiceRequest> serviceRequests = new HashSet<ServiceRequest>();
      serviceRequests.add(new ServiceRequest(CLUSTER_NAME, "HDFS", null, null));
      serviceRequests.add(new ServiceRequest(CLUSTER_NAME, "MAPREDUCE2", null, null));
      serviceRequests.add(new ServiceRequest(CLUSTER_NAME, "YARN", null, null));
  
      amc.createServices(serviceRequests);
  
      Set<ServiceComponentRequest> serviceComponentRequests = new HashSet<ServiceComponentRequest>();
      serviceComponentRequests.add(new ServiceComponentRequest(CLUSTER_NAME, "HDFS", "NAMENODE", null, null));
      serviceComponentRequests.add(new ServiceComponentRequest(CLUSTER_NAME, "HDFS", "SECONDARY_NAMENODE", null, null));
      serviceComponentRequests.add(new ServiceComponentRequest(CLUSTER_NAME, "HDFS", "DATANODE", null, null));
      serviceComponentRequests.add(new ServiceComponentRequest(CLUSTER_NAME, "MAPREDUCE2", "HISTORYSERVER", null, null));
      serviceComponentRequests.add(new ServiceComponentRequest(CLUSTER_NAME, "YARN", "RESOURCEMANAGER", null, null));
      serviceComponentRequests.add(new ServiceComponentRequest(CLUSTER_NAME, "YARN", "NODEMANAGER", null, null));
  
      amc.createComponents(serviceComponentRequests);
  
      Set<HostRequest> hostRequests = new HashSet<HostRequest>();
      hostRequests.add(new HostRequest(HOST1, CLUSTER_NAME, null));
  
      amc.createHosts(hostRequests);
  
      Set<ServiceComponentHostRequest> componentHostRequests = new HashSet<ServiceComponentHostRequest>();
      componentHostRequests.add(new ServiceComponentHostRequest(CLUSTER_NAME, null, "DATANODE", HOST1, null, null));
      componentHostRequests.add(new ServiceComponentHostRequest(CLUSTER_NAME, null, "NAMENODE", HOST1, null, null));
      componentHostRequests.add(new ServiceComponentHostRequest(CLUSTER_NAME, null, "SECONDARY_NAMENODE", HOST1, null, null));
      componentHostRequests.add(new ServiceComponentHostRequest(CLUSTER_NAME, null, "HISTORYSERVER", HOST1, null, null));
      componentHostRequests.add(new ServiceComponentHostRequest(CLUSTER_NAME, null, "RESOURCEMANAGER", HOST1, null, null));
      componentHostRequests.add(new ServiceComponentHostRequest(CLUSTER_NAME, null, "NODEMANAGER", HOST1, null, null));
  
      amc.createHostComponents(componentHostRequests);
  
      //Install services
      serviceRequests.clear();
      serviceRequests.add(new ServiceRequest(CLUSTER_NAME, "HDFS", null, State.INSTALLED.name()));
      serviceRequests.add(new ServiceRequest(CLUSTER_NAME, "MAPREDUCE2", null, State.INSTALLED.name()));
      serviceRequests.add(new ServiceRequest(CLUSTER_NAME, "YARN", null, State.INSTALLED.name()));
  
      amc.updateServices(serviceRequests, mapRequestProps, true, false);
  
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
      serviceRequests.add(new ServiceRequest(CLUSTER_NAME, "HDFS", null, State.STARTED.name()));
      serviceRequests.add(new ServiceRequest(CLUSTER_NAME, "MAPREDUCE2", null, State.STARTED.name()));
      serviceRequests.add(new ServiceRequest(CLUSTER_NAME, "YARN", null, State.STARTED.name()));
  
      RequestStatusResponse response = amc.updateServices(serviceRequests,
        mapRequestProps, true, false);
  
      Collection<?> hdfsSmokeTasks = CollectionUtils.select(response.getTasks(), new RolePredicate(HDFS_SERVICE_CHECK_ROLE));
      //Ensure that smoke test task was created for HDFS
      assertEquals(1, hdfsSmokeTasks.size());
  
      Collection<?> mapreduce2SmokeTasks = CollectionUtils.select(response.getTasks(), new RolePredicate(MAPREDUCE2_SERVICE_CHECK_ROLE));
      //Ensure that smoke test task was created for MAPREDUCE2
      assertEquals(1, mapreduce2SmokeTasks.size());
  
      Collection<?> yarnSmokeTasks = CollectionUtils.select(response.getTasks(), new RolePredicate(YARN_SERVICE_CHECK_ROLE));
      //Ensure that smoke test task was created for YARN
      assertEquals(1, yarnSmokeTasks.size());
    } finally {
      injector.getInstance(PersistService.class).stop();
    }
  }

  private class RolePredicate implements Predicate {

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

  //todo other resources
}
