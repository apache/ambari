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
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import junit.framework.Assert;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ClusterNotFoundException;
import org.apache.ambari.server.HostNotFoundException;
import org.apache.ambari.server.ParentObjectNotFoundException;
import org.apache.ambari.server.ServiceComponentHostNotFoundException;
import org.apache.ambari.server.ServiceComponentNotFoundException;
import org.apache.ambari.server.ServiceNotFoundException;
import org.apache.ambari.server.actionmanager.ActionDBAccessorImpl;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.entities.LdapSyncSpecEntity;
import org.apache.ambari.server.security.authorization.Users;
import org.apache.ambari.server.security.ldap.AmbariLdapDataPopulator;
import org.apache.ambari.server.security.ldap.LdapBatchDto;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceComponentImpl;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.ServiceOsSpecific;
import org.apache.ambari.server.state.StackId;
import org.easymock.Capture;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.RollbackException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.DB_DRIVER_FILENAME;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.STACK_NAME;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.STACK_VERSION;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * AmbariManagementControllerImpl unit tests
 */
public class AmbariManagementControllerImplTest {

  // Mocks
  private static final AmbariLdapDataPopulator ldapDataPopulator = createMock(AmbariLdapDataPopulator.class);
  private static final Clusters clusters = createNiceMock(Clusters.class);
  private static final ActionDBAccessorImpl actionDBAccessor = createNiceMock(ActionDBAccessorImpl.class);
  private static final AmbariMetaInfo ambariMetaInfo = createMock(AmbariMetaInfo.class);
  private static final Users users = createMock(Users.class);

  @Before
  public void before() throws Exception {
    reset(ldapDataPopulator, clusters,actionDBAccessor, ambariMetaInfo, users);
  }

  @Test
  public void testgetAmbariServerURI() throws Exception {
    // create mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = new Capture<AmbariManagementController>();

    // set expectations
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(null);
    expect(injector.getInstance(MaintenanceStateHelper.class)).andReturn(null);
    
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

    ClusterRequest request1 = new ClusterRequest(null, "cluster1", "1", Collections.<String>emptySet());
    Cluster cluster = createNiceMock(Cluster.class);
    ClusterResponse response = createNiceMock(ClusterResponse.class);

    Set<ClusterRequest> setRequests = new HashSet<ClusterRequest>();
    setRequests.add(request1);

    // expectations
    // constructor init
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(null);
    expect(injector.getInstance(MaintenanceStateHelper.class)).andReturn(null);

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

  @Test
  public void testGetClientHostForRunningAction_componentIsNull() throws Exception {
    Injector injector = createNiceMock(Injector.class);

    Cluster cluster = createNiceMock(Cluster.class);
    Service service = createNiceMock(Service.class);
    ServiceComponent component = null;

    replay(cluster, service, injector);

    AmbariManagementControllerImpl controller = new AmbariManagementControllerImpl(null, clusters, injector);
    String host = controller.getClientHostForRunningAction(cluster, service, component);

    assertNull(host);
    verify(cluster, service, injector);
  }

  @Test
  public void testGetClientHostForRunningAction_componentMapIsEmpty() throws Exception {
    Injector injector = createNiceMock(Injector.class);

    Cluster cluster = createNiceMock(Cluster.class);
    Service service = createNiceMock(Service.class);
    ServiceComponent component = createNiceMock(ServiceComponent.class);
    Map<String, ServiceComponentHost> hostMap = new HashMap<String, ServiceComponentHost>();
    expect(component.getServiceComponentHosts()).andReturn(hostMap);

    replay(cluster, service, component, injector);

    AmbariManagementControllerImpl controller = new AmbariManagementControllerImpl(null, clusters, injector);
    String host = controller.getClientHostForRunningAction(cluster, service, component);

    verify(cluster, service, component, injector);
    assertNull(host);
  }

  @Test
  public void testGetClientHostForRunningAction_returnsHelathyHost() throws Exception {
    Injector injector = createNiceMock(Injector.class);
    ActionManager actionManager = createNiceMock(ActionManager.class);

    Cluster cluster = createNiceMock(Cluster.class);
    Service service = createNiceMock(Service.class);
    ServiceComponent component = createNiceMock(ServiceComponent.class);
    Map<String, ServiceComponentHost> hostMap = createNiceMock(Map.class);
    Set<String> hostsSet = createNiceMock(Set.class);
    expect(hostMap.isEmpty()).andReturn(false);
    expect(hostMap.keySet()).andReturn(hostsSet);
    expect(component.getServiceComponentHosts()).andReturn(hostMap).times(2);

    replay(cluster, service, component, injector, actionManager, hostMap, hostsSet);

    AmbariManagementControllerImpl controller = createMockBuilder(AmbariManagementControllerImpl.class)
        .addMockedMethod("filterHostsForAction")
        .addMockedMethod("getHealthyHost")
        .withConstructor(actionManager, clusters, injector)
        .createMock();
    expect(controller.getHealthyHost(hostsSet)).andReturn("healthy_host");
    controller.filterHostsForAction(hostsSet, service, cluster, Resource.Type.Cluster);
    expectLastCall().once();

    replay(controller);
    String host = controller.getClientHostForRunningAction(cluster, service, component);

    assertEquals("healthy_host", host);
    verify(controller, cluster, service, component, injector, hostMap);
  }

  @Test
  public void testGetClientHostForRunningAction_clientComponent() throws Exception {
    Injector injector = createNiceMock(Injector.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Service service = createNiceMock(Service.class);
    StackId stackId = createNiceMock(StackId.class);
    ServiceComponent component = createNiceMock(ServiceComponent.class);

    expect(service.getName()).andReturn("service");
    expect(service.getServiceComponent("component")).andReturn(component);
    expect(service.getDesiredStackVersion()).andReturn(stackId);
    expect(stackId.getStackName()).andReturn("stack");
    expect(stackId.getStackVersion()).andReturn("1.0");

    ServiceInfo serviceInfo = createNiceMock(ServiceInfo.class);
    ComponentInfo compInfo = createNiceMock(ComponentInfo.class);
    expect(serviceInfo.getClientComponent()).andReturn(compInfo);
    expect(compInfo.getName()).andReturn("component");
    expect(component.getServiceComponentHosts()).andReturn(Collections.<String, ServiceComponentHost>singletonMap("host", null));
    expect(ambariMetaInfo.getServiceInfo("stack", "1.0", "service")).andReturn(serviceInfo);

    replay(injector, cluster, service, component, serviceInfo, compInfo, ambariMetaInfo, stackId);

    AmbariManagementControllerImpl controller = new AmbariManagementControllerImpl(null, clusters, injector);
    setAmbariMetaInfo(ambariMetaInfo, controller);
    ServiceComponent resultComponent = controller.getClientComponentForRunningAction(cluster, service);

    assertNotNull(resultComponent);
    assertEquals(component, resultComponent);
    verify(injector, cluster, service, component, serviceInfo, compInfo, ambariMetaInfo, stackId);
  }

  @Test
  public void testGetClientHostForRunningAction_clientComponentThrowsException() throws Exception {
    Injector injector = createNiceMock(Injector.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Service service = createNiceMock(Service.class);
    StackId stackId = createNiceMock(StackId.class);
    ServiceComponent component1 = createNiceMock(ServiceComponent.class);
    ServiceComponent component2 = createNiceMock(ServiceComponent.class);

    expect(service.getName()).andReturn("service");
    expect(service.getServiceComponent("component")).andThrow(
        new ServiceComponentNotFoundException("cluster", "service", "component"));
    expect(service.getDesiredStackVersion()).andReturn(stackId);
    expect(stackId.getStackName()).andReturn("stack");
    expect(stackId.getStackVersion()).andReturn("1.0");
    Map<String, ServiceComponent> componentsMap = new HashMap<String, ServiceComponent>();
    componentsMap.put("component1", component1);
    componentsMap.put("component2", component2);
    expect(service.getServiceComponents()).andReturn(componentsMap);
    expect(component1.getServiceComponentHosts()).andReturn(Collections.EMPTY_MAP);
    expect(component2.getServiceComponentHosts()).andReturn(
        Collections.<String, ServiceComponentHost>singletonMap("anyHost", null));

    ServiceInfo serviceInfo = createNiceMock(ServiceInfo.class);
    ComponentInfo compInfo = createNiceMock(ComponentInfo.class);
    expect(serviceInfo.getClientComponent()).andReturn(compInfo);
    expect(compInfo.getName()).andReturn("component");
    expect(ambariMetaInfo.getServiceInfo("stack", "1.0", "service")).andReturn(serviceInfo);

    replay(injector, cluster, service, component1, component2, serviceInfo, compInfo, ambariMetaInfo, stackId);

    AmbariManagementControllerImpl controller = new AmbariManagementControllerImpl(null, clusters, injector);
    setAmbariMetaInfo(ambariMetaInfo, controller);
    ServiceComponent resultComponent = controller.getClientComponentForRunningAction(cluster, service);

    assertNotNull(resultComponent);
    assertEquals(component2, resultComponent);
    verify(injector, cluster, service, component1, component2, serviceInfo, compInfo, ambariMetaInfo, stackId);
  }

  @Test
  public void testGetClientHostForRunningAction_noClientComponent() throws Exception {
    Injector injector = createNiceMock(Injector.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Service service = createNiceMock(Service.class);
    StackId stackId = createNiceMock(StackId.class);
    ServiceComponent component1 = createNiceMock(ServiceComponent.class);
    ServiceComponent component2 = createNiceMock(ServiceComponent.class);

    expect(service.getName()).andReturn("service");
    expect(service.getDesiredStackVersion()).andReturn(stackId);
    expect(stackId.getStackName()).andReturn("stack");
    expect(stackId.getStackVersion()).andReturn("1.0");
    Map<String, ServiceComponent> componentsMap = new HashMap<String, ServiceComponent>();
    componentsMap.put("component1", component1);
    componentsMap.put("component2", component2);
    expect(service.getServiceComponents()).andReturn(componentsMap);
    expect(component1.getServiceComponentHosts()).andReturn(Collections.EMPTY_MAP);
    expect(component2.getServiceComponentHosts()).andReturn(
        Collections.<String, ServiceComponentHost>singletonMap("anyHost", null));

    ServiceInfo serviceInfo = createNiceMock(ServiceInfo.class);
    expect(serviceInfo.getClientComponent()).andReturn(null);
    expect(ambariMetaInfo.getServiceInfo("stack", "1.0", "service")).andReturn(serviceInfo);

    replay(injector, cluster, service, component1, component2, serviceInfo, ambariMetaInfo, stackId);

    AmbariManagementControllerImpl controller = new AmbariManagementControllerImpl(null, clusters, injector);
    setAmbariMetaInfo(ambariMetaInfo, controller);
    ServiceComponent resultComponent = controller.getClientComponentForRunningAction(cluster, service);

    assertNotNull(resultComponent);
    assertEquals(component2, resultComponent);
    verify(injector, cluster, service, component1, component2, serviceInfo, ambariMetaInfo, stackId);
  }

  /**
   * Ensure that ClusterNotFoundException is propagated in case where there is a single request.
   */
  @Test
  public void testGetClusters___ClusterNotFoundException() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = new Capture<AmbariManagementController>();

    // requests
    ClusterRequest request1 = new ClusterRequest(null, "cluster1", "1", Collections.<String>emptySet());

    Set<ClusterRequest> setRequests = new HashSet<ClusterRequest>();
    setRequests.add(request1);

    // expectations
    // constructor init
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(null);
    expect(injector.getInstance(MaintenanceStateHelper.class)).andReturn(null);

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
    expect(injector.getInstance(MaintenanceStateHelper.class)).andReturn(null);

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

  /**
   * Ensure that when the cluster id is provided and the given cluster name is different from the cluster's name
   * then the cluster rename logic is executed.
   */
  @Test
  public void testUpdateClusters() throws Exception {
    // member state mocks
    Capture<AmbariManagementController> controllerCapture = new Capture<AmbariManagementController>();
    Injector injector = createStrictMock(Injector.class);
    Cluster cluster = createNiceMock(Cluster.class);
    ActionManager actionManager = createNiceMock(ActionManager.class);
    ClusterRequest clusterRequest = createNiceMock(ClusterRequest.class);

    // requests
    Set<ClusterRequest> setRequests = Collections.singleton(clusterRequest);

    // expectations
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(null);
    expect(injector.getInstance(MaintenanceStateHelper.class)).andReturn(null);
    expect(clusterRequest.getClusterName()).andReturn("clusterNew").times(4);
    expect(clusterRequest.getClusterId()).andReturn(1L).times(4);
    expect(clusters.getClusterById(1L)).andReturn(cluster);
    expect(cluster.getClusterName()).andReturn("clusterOld").times(2);
    cluster.setClusterName("clusterNew");
    expectLastCall();

    // replay mocks
    replay(actionManager, cluster, clusters, injector, clusterRequest);

    // test
    AmbariManagementController controller = new AmbariManagementControllerImpl(actionManager, clusters, injector);
    controller.updateClusters(setRequests, null);

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    verify(actionManager, cluster, clusters, injector, clusterRequest);
  }

  /**
   * Ensure that RollbackException is thrown outside the updateClusters method
   * when a unique constraint violation occurs.
   */
  @Test
  public void testUpdateClusters__RollbackException() throws Exception {
    // member state mocks
    Capture<AmbariManagementController> controllerCapture = new Capture<AmbariManagementController>();
    Injector injector = createStrictMock(Injector.class);
    Cluster cluster = createNiceMock(Cluster.class);
    ActionManager actionManager = createNiceMock(ActionManager.class);
    ClusterRequest clusterRequest = createNiceMock(ClusterRequest.class);

    // requests
    Set<ClusterRequest> setRequests = Collections.singleton(clusterRequest);

    // expectations
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(null);
    expect(injector.getInstance(MaintenanceStateHelper.class)).andReturn(null);
    expect(clusterRequest.getClusterName()).andReturn("clusterNew").times(4);
    expect(clusterRequest.getClusterId()).andReturn(1L).times(4);
    expect(clusters.getClusterById(1L)).andReturn(cluster);
    expect(cluster.getClusterName()).andReturn("clusterOld").times(2);
    cluster.setClusterName("clusterNew");
    expectLastCall().andThrow(new RollbackException());

    // replay mocks
    replay(actionManager, cluster, clusters, injector, clusterRequest);

    // test
    AmbariManagementController controller = new AmbariManagementControllerImpl(actionManager, clusters, injector);
    try {
      controller.updateClusters(setRequests, null);
      fail("Expected RollbackException");
    } catch (RollbackException e) {
      //expected
    }
    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    verify(actionManager, cluster, clusters, injector, clusterRequest);
  }

  @Test
  public void testGetHostComponents() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = new Capture<AmbariManagementController>();
    StackId stack = createNiceMock(StackId.class);

    Cluster cluster = createNiceMock(Cluster.class);
    final Host host = createNiceMock(Host.class);
    Service service = createNiceMock(Service.class);
    ServiceComponent component = createNiceMock(ServiceComponent.class);
    final ServiceComponentHost componentHost = createNiceMock(ServiceComponentHost.class);
    ServiceComponentHostResponse response = createNiceMock(ServiceComponentHostResponse.class);

    MaintenanceStateHelper maintHelper = createNiceMock(MaintenanceStateHelper.class);
    expect(maintHelper.getEffectiveState(componentHost)).andReturn(MaintenanceState.OFF).anyTimes();
    
    // requests
    ServiceComponentHostRequest request1 = new ServiceComponentHostRequest(
        "cluster1", null, "component1", "host1", null);

    Set<ServiceComponentHostRequest> setRequests = new HashSet<ServiceComponentHostRequest>();
    setRequests.add(request1);

    // expectations
    // constructor init
    injector.injectMembers(capture(controllerCapture));
    
    expect(injector.getInstance(Gson.class)).andReturn(null);
    expect(injector.getInstance(MaintenanceStateHelper.class)).andReturn(maintHelper).anyTimes();

    // getHostComponent
    expect(clusters.getCluster("cluster1")).andReturn(cluster);
    expect(clusters.getClustersForHost("host1")).andReturn(Collections.singleton(cluster));
    expect(clusters.getHostsForCluster((String) anyObject())).andReturn(
      new HashMap<String, Host>() {{ put("host1", host); }}).anyTimes();

    expect(cluster.getDesiredStackVersion()).andReturn(stack);
    expect(stack.getStackName()).andReturn("stackName");
    expect(stack.getStackVersion()).andReturn("stackVersion");

    expect(ambariMetaInfo.getComponentToService("stackName", "stackVersion", "component1")).andReturn("service1");
    expect(cluster.getService("service1")).andReturn(service);
    expect(service.getServiceComponent("component1")).andReturn(component);
    expect(component.getName()).andReturn("component1");
    expect(component.getServiceComponentHosts()).andReturn(
      new HashMap<String, ServiceComponentHost>() {{
        put("host1", componentHost);
    }});
    expect(componentHost.convertToResponse()).andReturn(response);
    expect(componentHost.getHostName()).andReturn("host1").anyTimes();
    expect(maintHelper.getEffectiveState(componentHost, host)).andReturn(MaintenanceState.OFF);

    // replay mocks
    replay(maintHelper, injector, clusters, cluster, host, response, stack,
      ambariMetaInfo, service, component, componentHost);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);
    setAmbariMetaInfo(ambariMetaInfo, controller);

    Set<ServiceComponentHostResponse> setResponses = controller.getHostComponents(setRequests);

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    assertEquals(1, setResponses.size());
    assertTrue(setResponses.contains(response));

    verify(injector, clusters, cluster, host, response, stack, ambariMetaInfo, service, component, componentHost);
  }

  @Test
  public void testGetHostComponents___ServiceComponentHostNotFoundException() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = new Capture<AmbariManagementController>();
    StackId stack = createNiceMock(StackId.class);

    Cluster cluster = createNiceMock(Cluster.class);
    Host host = createNiceMock(Host.class);
    Service service = createNiceMock(Service.class);
    ServiceComponent component = createNiceMock(ServiceComponent.class);
    MaintenanceStateHelper maintHelper = createNiceMock(MaintenanceStateHelper.class);

    // requests
    ServiceComponentHostRequest request1 = new ServiceComponentHostRequest(
        "cluster1", null, "component1", "host1", null);


    Set<ServiceComponentHostRequest> setRequests = new HashSet<ServiceComponentHostRequest>();
    setRequests.add(request1);

    // expectations
    // constructor init
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(null);
    expect(injector.getInstance(MaintenanceStateHelper.class)).andReturn(maintHelper);

    // getHostComponent
    expect(clusters.getCluster("cluster1")).andReturn(cluster);
    expect(clusters.getClustersForHost("host1")).andReturn(Collections.singleton(cluster));

    expect(cluster.getDesiredStackVersion()).andReturn(stack);
    expect(stack.getStackName()).andReturn("stackName");
    expect(stack.getStackVersion()).andReturn("stackVersion");

    expect(ambariMetaInfo.getComponentToService("stackName", "stackVersion", "component1")).andReturn("service1");
    expect(cluster.getService("service1")).andReturn(service);
    expect(service.getServiceComponent("component1")).andReturn(component);
    expect(component.getName()).andReturn("component1").anyTimes();
    expect(component.getServiceComponentHosts()).andReturn(null);

    // replay mocks
    replay(maintHelper, injector, clusters, cluster, host, stack, ambariMetaInfo,
      service, component);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);
    setAmbariMetaInfo(ambariMetaInfo, controller);

    try {
      controller.getHostComponents(setRequests);
      fail("expected ServiceComponentHostNotFoundException");
    } catch (ServiceComponentHostNotFoundException e) {
      //expected
    }

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    verify(injector, clusters, cluster, host, stack, ambariMetaInfo, service, component);
  }

  @Test
  public void testGetHostComponents___OR_Predicate_ServiceComponentHostNotFoundException() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = new Capture<AmbariManagementController>();
    StackId stack = createNiceMock(StackId.class);

    Cluster cluster = createNiceMock(Cluster.class);
    final Host host = createNiceMock(Host.class);
    Service service = createNiceMock(Service.class);
    ServiceComponent component = createNiceMock(ServiceComponent.class);
    ServiceComponent component2 = createNiceMock(ServiceComponent.class);
    ServiceComponent component3 = createNiceMock(ServiceComponent.class);

    final ServiceComponentHost componentHost1 = createNiceMock(ServiceComponentHost.class);
    final ServiceComponentHost componentHost2 = createNiceMock(ServiceComponentHost.class);
    ServiceComponentHostResponse response1 = createNiceMock(ServiceComponentHostResponse.class);
    ServiceComponentHostResponse response2 = createNiceMock(ServiceComponentHostResponse.class);
    MaintenanceStateHelper stateHelper = createNiceMock(MaintenanceStateHelper.class);
    expect(stateHelper.getEffectiveState(
      anyObject(ServiceComponentHost.class),
      anyObject(Host.class))).andReturn(MaintenanceState.OFF).anyTimes();

    expect(clusters.getHostsForCluster((String) anyObject())).andReturn(
      new HashMap<String, Host>() {{
        put("host1", host);
      }}).anyTimes();

    // requests
    ServiceComponentHostRequest request1 = new ServiceComponentHostRequest(
        "cluster1", null, "component1", "host1", null);

    ServiceComponentHostRequest request2 = new ServiceComponentHostRequest(
        "cluster1", null, "component2", "host1", null);

    ServiceComponentHostRequest request3 = new ServiceComponentHostRequest(
        "cluster1", null, "component3", "host1", null);


    Set<ServiceComponentHostRequest> setRequests = new HashSet<ServiceComponentHostRequest>();
    setRequests.add(request1);
    setRequests.add(request2);
    setRequests.add(request3);

    // expectations
    // constructor init
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(null);
    expect(injector.getInstance(MaintenanceStateHelper.class)).andReturn(stateHelper).anyTimes();


    // getHostComponent
    expect(clusters.getCluster("cluster1")).andReturn(cluster).times(3);
    expect(clusters.getClustersForHost("host1")).andReturn(Collections.singleton(cluster)).anyTimes();
    expect(cluster.getService("service1")).andReturn(service).times(3);

    expect(cluster.getDesiredStackVersion()).andReturn(stack).anyTimes();
    expect(stack.getStackName()).andReturn("stackName").anyTimes();
    expect(stack.getStackVersion()).andReturn("stackVersion").anyTimes();

    expect(ambariMetaInfo.getComponentToService("stackName", "stackVersion", "component1")).andReturn("service1");
    expect(service.getServiceComponent("component1")).andReturn(component);
    expect(component.getName()).andReturn("component1");
    expect(component.getServiceComponentHosts()).andReturn(
      new HashMap<String, ServiceComponentHost>() {{
        put("host1", componentHost1);
      }});
    expect(componentHost1.convertToResponse()).andReturn(response1);
    expect(componentHost1.getHostName()).andReturn("host1");

    expect(ambariMetaInfo.getComponentToService("stackName", "stackVersion", "component2")).andReturn("service1");
    expect(service.getServiceComponent("component2")).andReturn(component2);
    expect(component2.getName()).andReturn("component2");
    expect(component2.getServiceComponentHosts()).andReturn(null);
    expect(componentHost2.getHostName()).andReturn("host1");

    expect(ambariMetaInfo.getComponentToService("stackName", "stackVersion", "component3")).andReturn("service1");
    expect(service.getServiceComponent("component3")).andReturn(component3);
    expect(component3.getName()).andReturn("component3");
    expect(component3.getServiceComponentHosts()).andReturn(
      new HashMap<String, ServiceComponentHost>() {{
        put("host1", componentHost2);
      }});
    expect(componentHost2.convertToResponse()).andReturn(response2);

    // replay mocks
    replay(stateHelper, injector, clusters, cluster, host, stack,
        ambariMetaInfo, service, component, component2, component3, componentHost1,
        componentHost2, response1, response2);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);
    setAmbariMetaInfo(ambariMetaInfo, controller);

    Set<ServiceComponentHostResponse> setResponses = controller.getHostComponents(setRequests);

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    assertEquals(2, setResponses.size());
    assertTrue(setResponses.contains(response1));
    assertTrue(setResponses.contains(response2));

    verify(injector, clusters, cluster, host, stack, ambariMetaInfo, service, component, component2, component3,
        componentHost1, componentHost2, response1, response2);
  }

  @Test
  public void testGetHostComponents___OR_Predicate_ServiceNotFoundException() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = new Capture<AmbariManagementController>();
    StackId stack = createNiceMock(StackId.class);

    Cluster cluster = createNiceMock(Cluster.class);
    final Host host = createNiceMock(Host.class);
    Service service = createNiceMock(Service.class);
    ServiceComponent component = createNiceMock(ServiceComponent.class);
    ServiceComponent component2 = createNiceMock(ServiceComponent.class);
    ServiceComponent component3 = createNiceMock(ServiceComponent.class);

    final ServiceComponentHost componentHost1 = createNiceMock(ServiceComponentHost.class);
    final ServiceComponentHost componentHost2 = createNiceMock(ServiceComponentHost.class);
    ServiceComponentHostResponse response1 = createNiceMock(ServiceComponentHostResponse.class);
    ServiceComponentHostResponse response2 = createNiceMock(ServiceComponentHostResponse.class);
    MaintenanceStateHelper maintHelper = createNiceMock(MaintenanceStateHelper.class);
    expect(maintHelper.getEffectiveState(
      anyObject(ServiceComponentHost.class),
      anyObject(Host.class))).andReturn(MaintenanceState.OFF).anyTimes();

    // requests
    ServiceComponentHostRequest request1 = new ServiceComponentHostRequest(
        "cluster1", null, "component1", "host1", null);

    ServiceComponentHostRequest request2 = new ServiceComponentHostRequest(
        "cluster1", null, "component2", "host1", null);

    ServiceComponentHostRequest request3 = new ServiceComponentHostRequest(
        "cluster1", null, "component3", "host1", null);


    Set<ServiceComponentHostRequest> setRequests = new HashSet<ServiceComponentHostRequest>();
    setRequests.add(request1);
    setRequests.add(request2);
    setRequests.add(request3);

    // expectations
    // constructor init
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(null);
    expect(injector.getInstance(MaintenanceStateHelper.class)).andReturn(maintHelper).anyTimes();
    
    // getHostComponent
    expect(clusters.getCluster("cluster1")).andReturn(cluster).times(3);
    expect(clusters.getClustersForHost("host1")).andReturn(Collections.singleton(cluster)).anyTimes();
    expect(cluster.getDesiredStackVersion()).andReturn(stack).anyTimes();
    expect(stack.getStackName()).andReturn("stackName").anyTimes();
    expect(stack.getStackVersion()).andReturn("stackVersion").anyTimes();
    expect(clusters.getHostsForCluster((String) anyObject())).andReturn(
      new HashMap<String, Host>() {{
        put("host1", host);
      }}).anyTimes();

    expect(ambariMetaInfo.getComponentToService("stackName", "stackVersion", "component1")).andReturn("service1");
    expect(cluster.getService("service1")).andReturn(service);
    expect(service.getServiceComponent("component1")).andReturn(component);
    expect(component.getName()).andReturn("component1");
    expect(component.getServiceComponentHosts()).andReturn(new
      HashMap<String, ServiceComponentHost>() {{
        put("host1", componentHost1);
      }});
    expect(componentHost1.convertToResponse()).andReturn(response1);
    expect(componentHost1.getHostName()).andReturn("host1");

    expect(ambariMetaInfo.getComponentToService("stackName", "stackVersion", "component2")).andReturn("service2");
    expect(cluster.getService("service2")).andThrow(new ServiceNotFoundException("cluster1", "service2"));

    expect(ambariMetaInfo.getComponentToService("stackName", "stackVersion", "component3")).andReturn("service1");
    expect(cluster.getService("service1")).andReturn(service);
    expect(service.getServiceComponent("component3")).andReturn(component3);
    expect(component3.getName()).andReturn("component3");
    expect(component3.getServiceComponentHosts()).andReturn(new
      HashMap<String, ServiceComponentHost>() {{
        put("host1", componentHost2);
      }});
    expect(componentHost2.convertToResponse()).andReturn(response2);
    expect(componentHost2.getHostName()).andReturn("host1");

    // replay mocks
    replay(maintHelper, injector, clusters, cluster, host, stack, ambariMetaInfo,
        service, component, component2, component3, componentHost1,
        componentHost2, response1, response2);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);
    setAmbariMetaInfo(ambariMetaInfo, controller);

    Set<ServiceComponentHostResponse> setResponses = controller.getHostComponents(setRequests);

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    assertEquals(2, setResponses.size());
    assertTrue(setResponses.contains(response1));
    assertTrue(setResponses.contains(response2));

    verify(injector, clusters, cluster, host, stack, ambariMetaInfo, service, component, component2, component3,
        componentHost1, componentHost2, response1, response2);
  }

  @Test
  public void testGetHostComponents___OR_Predicate_ServiceComponentNotFoundException() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = new Capture<AmbariManagementController>();
    StackId stack = createNiceMock(StackId.class);

    Cluster cluster = createNiceMock(Cluster.class);
    final Host host = createNiceMock(Host.class);
    Service service = createNiceMock(Service.class);
    Service service2 = createNiceMock(Service.class);
    ServiceComponent component = createNiceMock(ServiceComponent.class);
    ServiceComponent component2 = createNiceMock(ServiceComponent.class);
    ServiceComponent component3 = createNiceMock(ServiceComponent.class);

    final ServiceComponentHost componentHost1 = createNiceMock(ServiceComponentHost.class);
    final ServiceComponentHost componentHost2 = createNiceMock(ServiceComponentHost.class);
    ServiceComponentHostResponse response1 = createNiceMock(ServiceComponentHostResponse.class);
    ServiceComponentHostResponse response2 = createNiceMock(ServiceComponentHostResponse.class);
    
    MaintenanceStateHelper maintHelper = createNiceMock(MaintenanceStateHelper.class);
    expect(maintHelper.getEffectiveState(
      anyObject(ServiceComponentHost.class),
      anyObject(Host.class))).andReturn(MaintenanceState.OFF).anyTimes();

    // requests
    ServiceComponentHostRequest request1 = new ServiceComponentHostRequest(
        "cluster1", null, "component1", "host1", null);

    ServiceComponentHostRequest request2 = new ServiceComponentHostRequest(
        "cluster1", null, "component2", "host1", null);

    ServiceComponentHostRequest request3 = new ServiceComponentHostRequest(
        "cluster1", null, "component3", "host1", null);


    Set<ServiceComponentHostRequest> setRequests = new HashSet<ServiceComponentHostRequest>();
    setRequests.add(request1);
    setRequests.add(request2);
    setRequests.add(request3);

    // expectations
    // constructor init
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(null);
    expect(injector.getInstance(MaintenanceStateHelper.class)).andReturn(maintHelper).anyTimes();

    // getHostComponent
    expect(clusters.getCluster("cluster1")).andReturn(cluster).times(3);
    expect(clusters.getClustersForHost("host1")).andReturn(Collections.singleton(cluster)).anyTimes();
    expect(clusters.getHostsForCluster((String) anyObject())).andReturn(
      new HashMap<String, Host>() {{
        put("host1", host);
      }}).anyTimes();
    expect(cluster.getDesiredStackVersion()).andReturn(stack).anyTimes();
    expect(stack.getStackName()).andReturn("stackName").anyTimes();
    expect(stack.getStackVersion()).andReturn("stackVersion").anyTimes();


    expect(ambariMetaInfo.getComponentToService("stackName", "stackVersion", "component1")).andReturn("service1");
    expect(cluster.getService("service1")).andReturn(service);
    expect(service.getServiceComponent("component1")).andReturn(component);
    expect(component.getName()).andReturn("component1");
    expect(component.getServiceComponentHosts()).andReturn(
      new HashMap<String, ServiceComponentHost>() {{
        put("host1", componentHost1);
      }});
    expect(componentHost1.convertToResponse()).andReturn(response1);
    expect(componentHost1.getHostName()).andReturn("host1");

    expect(ambariMetaInfo.getComponentToService("stackName", "stackVersion", "component2")).andReturn("service2");
    expect(cluster.getService("service2")).andReturn(service2);
    expect(service2.getServiceComponent("component2")).
        andThrow(new ServiceComponentNotFoundException("cluster1", "service2", "component2"));

    expect(ambariMetaInfo.getComponentToService("stackName", "stackVersion", "component3")).andReturn("service1");
    expect(cluster.getService("service1")).andReturn(service);
    expect(service.getServiceComponent("component3")).andReturn(component3);
    expect(component3.getName()).andReturn("component3");
    expect(component3.getServiceComponentHosts()).andReturn(
      new HashMap<String, ServiceComponentHost>() {{
        put("host1", componentHost2);
      }});
    expect(componentHost2.convertToResponse()).andReturn(response2);
    expect(componentHost2.getHostName()).andReturn("host1");

    // replay mocks
    replay(maintHelper, injector, clusters, cluster, host, stack, ambariMetaInfo,
      service, service2, component, component2, component3, componentHost1,
      componentHost2, response1, response2);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);
    setAmbariMetaInfo(ambariMetaInfo, controller);

    Set<ServiceComponentHostResponse> setResponses = controller.getHostComponents(setRequests);

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    assertEquals(2, setResponses.size());
    assertTrue(setResponses.contains(response1));
    assertTrue(setResponses.contains(response2));

    verify(injector, clusters, cluster, host, stack, ambariMetaInfo, service, service2, component, component2, component3,
        componentHost1, componentHost2, response1, response2);
  }

  @Test
  public void testGetHostComponents___OR_Predicate_HostNotFoundException_hostProvidedInQuery() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = new Capture<AmbariManagementController>();
    StackId stack = createNiceMock(StackId.class);

    Cluster cluster = createNiceMock(Cluster.class);
    final Host host = createNiceMock(Host.class);
    Service service = createNiceMock(Service.class);
    Service service2 = createNiceMock(Service.class);
    ServiceComponent component = createNiceMock(ServiceComponent.class);
    ServiceComponent component2 = createNiceMock(ServiceComponent.class);
    ServiceComponent component3 = createNiceMock(ServiceComponent.class);

    final ServiceComponentHost componentHost1 = createNiceMock(ServiceComponentHost.class);
    final ServiceComponentHost componentHost2 = createNiceMock(ServiceComponentHost.class);
    ServiceComponentHostResponse response1 = createNiceMock(ServiceComponentHostResponse.class);
    ServiceComponentHostResponse response2 = createNiceMock(ServiceComponentHostResponse.class);
    MaintenanceStateHelper maintHelper = createNiceMock(MaintenanceStateHelper.class);
    expect(maintHelper.getEffectiveState(
      anyObject(ServiceComponentHost.class),
      anyObject(Host.class))).andReturn(MaintenanceState.OFF).anyTimes();

    // requests
    ServiceComponentHostRequest request1 = new ServiceComponentHostRequest(
        "cluster1", null, "component1", null, null);

    ServiceComponentHostRequest request2 = new ServiceComponentHostRequest(
        "cluster1", null, "component2", "host2", null);

    ServiceComponentHostRequest request3 = new ServiceComponentHostRequest(
        "cluster1", null, "component3", null, null);


    Set<ServiceComponentHostRequest> setRequests = new HashSet<ServiceComponentHostRequest>();
    setRequests.add(request1);
    setRequests.add(request2);
    setRequests.add(request3);

    // expectations
    // constructor init
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(null);
    expect(injector.getInstance(MaintenanceStateHelper.class)).andReturn(maintHelper).anyTimes();

    // getHostComponent
    expect(clusters.getCluster("cluster1")).andReturn(cluster).times(3);
    expect(clusters.getHostsForCluster((String) anyObject())).andReturn(
      new HashMap<String, Host>() {{
        put("host1", host);
      }}).anyTimes();

    expect(cluster.getDesiredStackVersion()).andReturn(stack).anyTimes();
    expect(stack.getStackName()).andReturn("stackName").anyTimes();
    expect(stack.getStackVersion()).andReturn("stackVersion").anyTimes();

    expect(ambariMetaInfo.getComponentToService("stackName", "stackVersion", "component1")).andReturn("service1");
    expect(cluster.getService("service1")).andReturn(service);
    expect(service.getServiceComponent("component1")).andReturn(component);
    expect(component.getName()).andReturn("component1");
    expect(component.getServiceComponentHosts()).andReturn(Collections.singletonMap("foo", componentHost1));
    expect(componentHost1.convertToResponse()).andReturn(response1);
    expect(componentHost1.getHostName()).andReturn("host1");

    expect(clusters.getClustersForHost("host2")).andThrow(new HostNotFoundException("host2"));

    expect(ambariMetaInfo.getComponentToService("stackName", "stackVersion", "component3")).andReturn("service1");
    expect(cluster.getService("service1")).andReturn(service);
    expect(service.getServiceComponent("component3")).andReturn(component3);
    expect(component3.getName()).andReturn("component3");
    expect(component3.getServiceComponentHosts()).andReturn(Collections.singletonMap("foo", componentHost2));
    expect(componentHost2.convertToResponse()).andReturn(response2);
    expect(componentHost2.getHostName()).andReturn("host1");

    // replay mocks
    replay(maintHelper, injector, clusters, cluster, host, stack, ambariMetaInfo,
        service, service2, component, component2, component3, componentHost1,
        componentHost2, response1, response2);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);
    setAmbariMetaInfo(ambariMetaInfo, controller);

    Set<ServiceComponentHostResponse> setResponses = controller.getHostComponents(setRequests);
    Assert.assertNotNull(setResponses);

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    assertEquals(2, setResponses.size());
    assertTrue(setResponses.contains(response1));
    assertTrue(setResponses.contains(response2));

    verify(injector, clusters, cluster, host, stack, ambariMetaInfo, service, service2, component, component2, component3,
        componentHost1, componentHost2, response1, response2);
  }

  @Test
  public void testGetHostComponents___OR_Predicate_HostNotFoundException_hostProvidedInURL() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = new Capture<AmbariManagementController>();
    StackId stack = createNiceMock(StackId.class);
    MaintenanceStateHelper maintHelper = createNiceMock(MaintenanceStateHelper.class);

    Cluster cluster = createNiceMock(Cluster.class);

    // requests
    ServiceComponentHostRequest request1 = new ServiceComponentHostRequest(
        "cluster1", null, "component1", "host1", null);

    ServiceComponentHostRequest request2 = new ServiceComponentHostRequest(
        "cluster1", null, "component2", "host1", null);

    ServiceComponentHostRequest request3 = new ServiceComponentHostRequest(
        "cluster1", null, "component3", "host1", null);


    Set<ServiceComponentHostRequest> setRequests = new HashSet<ServiceComponentHostRequest>();
    setRequests.add(request1);
    setRequests.add(request2);
    setRequests.add(request3);

    // expectations
    // constructor init
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(null);
    expect(injector.getInstance(MaintenanceStateHelper.class)).andReturn(maintHelper);

    // getHostComponent
    expect(clusters.getCluster("cluster1")).andReturn(cluster);
    expect(clusters.getClustersForHost("host1")).andThrow(new HostNotFoundException("host1"));

    // replay mocks
    replay(maintHelper, injector, clusters, cluster, stack, ambariMetaInfo);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);
    setAmbariMetaInfo(ambariMetaInfo, controller);

    try {
      controller.getHostComponents(setRequests);
      fail("expected exception");
    } catch (AmbariException e) {
      // expected
    }

    // assert and verify
    assertSame(controller, controllerCapture.getValue());

    verify(injector, clusters, cluster, stack, ambariMetaInfo);
  }

  @Test
  public void testGetHostComponents___OR_Predicate_ClusterNotFoundException() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = new Capture<AmbariManagementController>();
    StackId stack = createNiceMock(StackId.class);
    MaintenanceStateHelper maintHelper = createNiceMock(MaintenanceStateHelper.class);

    // requests
    ServiceComponentHostRequest request1 = new ServiceComponentHostRequest(
        "cluster1", null, "component1", "host1", null);

    ServiceComponentHostRequest request2 = new ServiceComponentHostRequest(
        "cluster1", null, "component2", "host2", null);

    ServiceComponentHostRequest request3 = new ServiceComponentHostRequest(
        "cluster1", null, "component3", "host1", null);


    Set<ServiceComponentHostRequest> setRequests = new HashSet<ServiceComponentHostRequest>();
    setRequests.add(request1);
    setRequests.add(request2);
    setRequests.add(request3);

    // expectations
    // constructor init
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(null);
    expect(injector.getInstance(MaintenanceStateHelper.class)).andReturn(maintHelper);

    // getHostComponent
    expect(clusters.getCluster("cluster1")).andThrow(new ClusterNotFoundException("cluster1"));

    // replay mocks
    replay(maintHelper, injector, clusters, stack, ambariMetaInfo);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);
    setAmbariMetaInfo(ambariMetaInfo, controller);

    try {
      controller.getHostComponents(setRequests);
      fail("expected exception");
    } catch (ParentObjectNotFoundException e) {
      //expected
    }

    // assert and verify
    assertSame(controller, controllerCapture.getValue());

    verify(injector, clusters,stack, ambariMetaInfo);
  }

  @Test
  public void testGetHostComponents___NullHostName() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = new Capture<AmbariManagementController>();
    StackId stack = createNiceMock(StackId.class);

    Cluster cluster = createNiceMock(Cluster.class);
    Service service = createNiceMock(Service.class);
    ServiceComponent component = createNiceMock(ServiceComponent.class);
    ServiceComponentHost componentHost1 = createNiceMock(ServiceComponentHost.class);
    ServiceComponentHost componentHost2 = createNiceMock(ServiceComponentHost.class);
    ServiceComponentHostResponse response1 = createNiceMock(ServiceComponentHostResponse.class);
    ServiceComponentHostResponse response2 = createNiceMock(ServiceComponentHostResponse.class);
    MaintenanceStateHelper maintHelper = createNiceMock(MaintenanceStateHelper.class);
    expect(maintHelper.getEffectiveState(
      anyObject(ServiceComponentHost.class),
      anyObject(Host.class))).andReturn(MaintenanceState.OFF).anyTimes();

    // requests
    ServiceComponentHostRequest request1 = new ServiceComponentHostRequest(
        "cluster1", null, "component1", null, null);


    Set<ServiceComponentHostRequest> setRequests = new HashSet<ServiceComponentHostRequest>();
    setRequests.add(request1);

    Map<String, ServiceComponentHost> mapHostComponents = new HashMap<String, ServiceComponentHost>();
    mapHostComponents.put("foo", componentHost1);
    mapHostComponents.put("bar", componentHost2);


    // expectations
    // constructor init
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(null);
    expect(injector.getInstance(MaintenanceStateHelper.class)).andReturn(maintHelper).anyTimes();

    // getHostComponent
    expect(clusters.getCluster("cluster1")).andReturn(cluster);
    expect(clusters.getHostsForCluster((String) anyObject())).andReturn(
      new HashMap<String, Host>() {{
        put("host1", createNiceMock(Host.class));
      }}).anyTimes();
    expect(cluster.getDesiredStackVersion()).andReturn(stack);
    expect(stack.getStackName()).andReturn("stackName");
    expect(stack.getStackVersion()).andReturn("stackVersion");

    expect(ambariMetaInfo.getComponentToService("stackName", "stackVersion", "component1")).andReturn("service1");
    expect(cluster.getService("service1")).andReturn(service);
    expect(service.getServiceComponent("component1")).andReturn(component);
    expect(component.getName()).andReturn("component1").anyTimes();

    expect(component.getServiceComponentHosts()).andReturn(mapHostComponents);
    expect(componentHost1.convertToResponse()).andReturn(response1);
    expect(componentHost2.convertToResponse()).andReturn(response2);
    expect(componentHost1.getHostName()).andReturn("host1");
    expect(componentHost2.getHostName()).andReturn("host1");

    // replay mocks
    replay(maintHelper, injector, clusters, cluster, response1, response2,
        stack, ambariMetaInfo, service, component, componentHost1, componentHost2);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);
    setAmbariMetaInfo(ambariMetaInfo, controller);

    Set<ServiceComponentHostResponse> setResponses = controller.getHostComponents(setRequests);

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    assertEquals(2, setResponses.size());
    assertTrue(setResponses.contains(response1));
    assertTrue(setResponses.contains(response2));

    verify(injector, clusters, cluster, response1, response2, stack, ambariMetaInfo, service, component, componentHost1, componentHost2);
  }

  @Test
  public void testGetHostComponents___NullHostName_NullComponentName() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = new Capture<AmbariManagementController>();
    StackId stack = createNiceMock(StackId.class);

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
    MaintenanceStateHelper maintHelper = createNiceMock(MaintenanceStateHelper.class);
    expect(maintHelper.getEffectiveState(
      anyObject(ServiceComponentHost.class),
      anyObject(Host.class))).andReturn(MaintenanceState.OFF).anyTimes();

    // requests
    ServiceComponentHostRequest request1 = new ServiceComponentHostRequest(
        "cluster1", null, null, null, null);


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
    expect(injector.getInstance(MaintenanceStateHelper.class)).andReturn(maintHelper).anyTimes();

    // getHostComponent
    expect(clusters.getCluster("cluster1")).andReturn(cluster);
    expect(clusters.getHostsForCluster((String) anyObject())).andReturn(
      new HashMap<String, Host>() {{
        put("host1", createNiceMock(Host.class));
      }}).anyTimes();

    expect(cluster.getServices()).andReturn(mapServices);
    expect(service1.getServiceComponents()).andReturn(Collections.singletonMap("foo", component1));
    expect(service2.getServiceComponents()).andReturn(Collections.singletonMap("bar", component2));

    expect(component1.getName()).andReturn("component1").anyTimes();
    expect(component2.getName()).andReturn("component2").anyTimes();

    expect(component1.getServiceComponentHosts()).andReturn(mapHostComponents);
    expect(componentHost1.convertToResponse()).andReturn(response1);
    expect(componentHost2.convertToResponse()).andReturn(response2);
    expect(componentHost1.getHostName()).andReturn("host1");
    expect(componentHost2.getHostName()).andReturn("host1");
    expect(componentHost3.getHostName()).andReturn("host1");

    expect(component2.getServiceComponentHosts()).andReturn(Collections.singletonMap("foobar", componentHost3));
    expect(componentHost3.convertToResponse()).andReturn(response3);

    // replay mocks
    replay(maintHelper, injector, clusters, cluster, response1, response2,
        response3, stack, ambariMetaInfo, service1, service2, component1, component2,
        componentHost1, componentHost2, componentHost3);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);
    setAmbariMetaInfo(ambariMetaInfo, controller);

    Set<ServiceComponentHostResponse> setResponses = controller.getHostComponents(setRequests);

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    assertEquals(3, setResponses.size());
    assertTrue(setResponses.contains(response1));
    assertTrue(setResponses.contains(response2));
    assertTrue(setResponses.contains(response3));

    verify(injector, clusters, cluster, response1, response2, response3, stack, ambariMetaInfo, service1, service2,
        component1, component2, componentHost1, componentHost2, componentHost3);
  }

  @Test
  public void testPopulateServicePackagesInfo() throws Exception {
    Capture<AmbariManagementController> controllerCapture = new Capture<AmbariManagementController>();
    Injector injector = createStrictMock(Injector.class);
    MaintenanceStateHelper maintHelper = createNiceMock(MaintenanceStateHelper.class);

    ServiceInfo serviceInfo = createNiceMock(ServiceInfo.class);
    Map<String, String> hostParams = new HashMap<String, String>();
    String osFamily = "testOSFamily";

    Map<String, ServiceOsSpecific> osSpecifics = new HashMap<String, ServiceOsSpecific>();

    ServiceOsSpecific.Package package1 = new ServiceOsSpecific.Package();
    package1.setName("testrpm1");
    ServiceOsSpecific.Package package2 = new ServiceOsSpecific.Package();
    package2.setName("testrpm2");
    ServiceOsSpecific.Package package3 = new ServiceOsSpecific.Package();
    package3.setName("testrpm3");

    List<ServiceOsSpecific.Package> packageList1 = new ArrayList<ServiceOsSpecific.Package>();
    packageList1.add(package1);
    List<ServiceOsSpecific.Package> packageList2 = new ArrayList<ServiceOsSpecific.Package>();
    packageList2.add(package2);
    packageList2.add(package3);

    ServiceOsSpecific osSpecific1 = new ServiceOsSpecific("testOSFamily");
    osSpecific1.addPackages(packageList1);
    ServiceOsSpecific osSpecific2 = new ServiceOsSpecific("testOSFamily1,testOSFamily,testOSFamily2");
    osSpecific2.addPackages(packageList2);

    osSpecifics.put("testOSFamily", osSpecific1);
    osSpecifics.put("testOSFamily1,testOSFamily,testOSFamily2", osSpecific2);

    expect(serviceInfo.getOsSpecifics()).andReturn(osSpecifics);
    injector.injectMembers(capture(controllerCapture));
    expect(injector.getInstance(Gson.class)).andReturn(null);
    expect(injector.getInstance(MaintenanceStateHelper.class)).andReturn(maintHelper).anyTimes();

    replay(maintHelper, injector, clusters, serviceInfo);

    AmbariManagementControllerImplTest.NestedTestClass nestedTestClass = this.new NestedTestClass(null, clusters,
                                                                         injector);

    ServiceOsSpecific serviceOsSpecific = nestedTestClass.populateServicePackagesInfo(serviceInfo, hostParams, osFamily);

    assertEquals(serviceOsSpecific.getPackages().size(), 3);
  }

  @Test
  public void testCreateDefaultHostParams() throws Exception {
    String SOME_STACK_NAME = "SomeStackName";
    String SOME_STACK_VERSION = "1.0";
    String MYSQL_JAR = "MYSQL_JAR";
    String JAVA_HOME = "javaHome";
    String JDK_NAME = "jdkName";
    String JCE_NAME = "jceName";
    String OJDBC_JAR_NAME = "OjdbcJarName";
    String SERVER_DB_NAME = "ServerDBName";

    ActionManager manager = createNiceMock(ActionManager.class);
    StackId stackId = createNiceMock(StackId.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Injector injector = createNiceMock(Injector.class);
    Configuration configuration = createNiceMock(Configuration.class);

    expect(cluster.getDesiredStackVersion()).andReturn(stackId);
    expect(stackId.getStackName()).andReturn(SOME_STACK_NAME).anyTimes();
    expect(stackId.getStackVersion()).andReturn(SOME_STACK_VERSION).anyTimes();
    expect(configuration.getMySQLJarName()).andReturn(MYSQL_JAR);
    expect(configuration.getJavaHome()).andReturn(JAVA_HOME);
    expect(configuration.getJDKName()).andReturn(JDK_NAME);
    expect(configuration.getJCEName()).andReturn(JCE_NAME);
    expect(configuration.getOjdbcJarName()).andReturn(OJDBC_JAR_NAME);
    expect(configuration.getServerDBName()).andReturn(SERVER_DB_NAME);

    replay(manager, clusters, cluster, injector, stackId, configuration);

    AmbariManagementControllerImpl ambariManagementControllerImpl =
            createMockBuilder(AmbariManagementControllerImpl.class)
            .addMockedMethod("getRcaParameters")
            .withConstructor(manager, clusters, injector).createNiceMock();

    expect(ambariManagementControllerImpl.
            getRcaParameters()).andReturn(new HashMap<String, String>());
    replay(ambariManagementControllerImpl);

    // Inject configuration manually
    Class<?> amciClass = AmbariManagementControllerImpl.class;
    Field f = amciClass.getDeclaredField("configs");
    f.setAccessible(true);
    f.set(ambariManagementControllerImpl, configuration);

    TreeMap<String, String> defaultHostParams =
            ambariManagementControllerImpl.createDefaultHostParams(cluster);

    assertEquals(defaultHostParams.size(), 10);
    assertEquals(defaultHostParams.get(DB_DRIVER_FILENAME), MYSQL_JAR);
    assertEquals(defaultHostParams.get(STACK_NAME), SOME_STACK_NAME);
    assertEquals(defaultHostParams.get(STACK_VERSION), SOME_STACK_VERSION);
  }

  @Test
  public void testSynchronizeLdapUsersAndGroups() throws Exception {

    Set<String> userSet = new HashSet<String>();
    userSet.add("user1");

    Set<String> groupSet = new HashSet<String>();
    groupSet.add("group1");

    Injector injector = Guice.createInjector(Modules.override(new InMemoryDefaultTestModule()).with(new MockModule()));

    // create mocks

    LdapBatchDto ldapBatchDto = createNiceMock(LdapBatchDto.class);


    Capture<LdapBatchDto> ldapBatchDtoCapture = new Capture<LdapBatchDto>();

    // set expectations
    expect(ldapDataPopulator.synchronizeAllLdapUsers(capture(ldapBatchDtoCapture))).andReturn(ldapBatchDto);
    expect(ldapDataPopulator.synchronizeAllLdapGroups(capture(ldapBatchDtoCapture))).andReturn(ldapBatchDto);

    expect(ldapDataPopulator.synchronizeExistingLdapUsers(capture(ldapBatchDtoCapture))).andReturn(ldapBatchDto);
    expect(ldapDataPopulator.synchronizeExistingLdapGroups(capture(ldapBatchDtoCapture))).andReturn(ldapBatchDto);

    expect(ldapDataPopulator.synchronizeLdapUsers(eq(userSet), capture(ldapBatchDtoCapture))).andReturn(ldapBatchDto);
    expect(ldapDataPopulator.synchronizeLdapGroups(eq(groupSet), capture(ldapBatchDtoCapture))).andReturn(ldapBatchDto);

    users.processLdapSync(capture(ldapBatchDtoCapture));
    expectLastCall().anyTimes();

    //replay
    replay(ldapDataPopulator, clusters, actionDBAccessor, ambariMetaInfo, users, ldapBatchDto);

    AmbariManagementControllerImpl controller = injector.getInstance(AmbariManagementControllerImpl.class);

    LdapSyncRequest userRequest  = new LdapSyncRequest(LdapSyncSpecEntity.SyncType.ALL);
    LdapSyncRequest groupRequest = new LdapSyncRequest(LdapSyncSpecEntity.SyncType.ALL);

    controller.synchronizeLdapUsersAndGroups(userRequest, groupRequest);

    userRequest  = new LdapSyncRequest(LdapSyncSpecEntity.SyncType.EXISTING);
    groupRequest = new LdapSyncRequest(LdapSyncSpecEntity.SyncType.EXISTING);

    controller.synchronizeLdapUsersAndGroups(userRequest, groupRequest);

    userRequest  = new LdapSyncRequest(LdapSyncSpecEntity.SyncType.SPECIFIC, userSet);
    groupRequest = new LdapSyncRequest(LdapSyncSpecEntity.SyncType.SPECIFIC, groupSet);

    controller.synchronizeLdapUsersAndGroups(userRequest, groupRequest);

    verify(ldapDataPopulator, clusters, users, ldapBatchDto);
  }

  private void setAmbariMetaInfo(AmbariMetaInfo metaInfo, AmbariManagementController controller) throws NoSuchFieldException, IllegalAccessException {
    //need to set private field 'ambariMetaInfo' which is injected at runtime
    Class<?> c = controller.getClass();
    Field f = c.getDeclaredField("ambariMetaInfo");
    f.setAccessible(true);
    f.set(controller, metaInfo);
  }

  private class MockModule implements Module {

    @Override
    public void configure(Binder binder) {
      binder.bind(AmbariLdapDataPopulator.class).toInstance(ldapDataPopulator);
      binder.bind(Clusters.class).toInstance(clusters);
      binder.bind(ActionDBAccessorImpl.class).toInstance(actionDBAccessor);
      binder.bind(AmbariMetaInfo.class).toInstance(ambariMetaInfo);
      binder.bind(Users.class).toInstance(users);
    }
  }

  private class NestedTestClass extends AmbariManagementControllerImpl {

    public NestedTestClass(ActionManager actionManager, Clusters clusters, Injector injector) throws Exception {
      super(actionManager, clusters, injector);
    }

    public ServiceOsSpecific testPopulateServicePackagesInfo(ServiceInfo serviceInfo, Map<String, String> hostParams,
                                                             String osFamily) {
      return super.populateServicePackagesInfo(serviceInfo, hostParams, osFamily);
    }

  }
}
