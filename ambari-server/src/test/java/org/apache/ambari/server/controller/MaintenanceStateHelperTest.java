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
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.inject.Injector;
import org.apache.ambari.server.controller.internal.RequestResourceFilter;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.easymock.Capture;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the {@link MaintenanceStateHelper} class
 */
public class MaintenanceStateHelperTest {

  @Test
  public void testService() throws Exception {
    testService(MaintenanceState.ON);
    testService(MaintenanceState.OFF);
  }
  
  @Test
  public void testHost() throws Exception {
    testHost(MaintenanceState.ON);
    testHost(MaintenanceState.OFF);
  }
  
  @Test
  public void testHostComponent() throws Exception {
    testHostComponent(MaintenanceState.ON);
    testHostComponent(MaintenanceState.OFF);
  }
  
  private void testHostComponent(MaintenanceState state) throws Exception {
    AmbariManagementController amc = createMock(AmbariManagementController.class);
    Capture<ExecuteActionRequest> earCapture = new Capture<ExecuteActionRequest>();
    Capture<Map<String, String>> rpCapture = new Capture<Map<String, String>>();
    expect(amc.createAction(capture(earCapture), capture(rpCapture))).andReturn(null);
    Injector injector = createStrictMock(Injector.class);
    MaintenanceStateHelper maintenanceStateHelper = createMockBuilder
      (MaintenanceStateHelper.class).withConstructor(injector).createMock();
    
    Clusters clusters = createMock(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    expect(amc.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getClusterById(1L)).andReturn(cluster);
    expect(cluster.getClusterName()).andReturn("c1").anyTimes();
    
    ServiceComponentHost sch = createMock(ServiceComponentHost.class);
    expect(sch.getClusterName()).andReturn("c1");
    expect(sch.getClusterId()).andReturn(1L);
    expect(sch.getMaintenanceState()).andReturn(state);
    expect(sch.getServiceName()).andReturn("HDFS");
    expect(sch.getServiceComponentName()).andReturn("NAMENODE").anyTimes();
    expect(sch.getHostName()).andReturn("h1");
    
    replay(amc, clusters, cluster, sch, maintenanceStateHelper);
    
    Map<String, String> map = new HashMap<String, String>();
    map.put("context", "abc");
    maintenanceStateHelper.createRequests(amc, map, Collections.singleton(sch.getClusterName()));
    
    ExecuteActionRequest ear = earCapture.getValue();
    map = rpCapture.getValue();
    
    Assert.assertEquals("nagios_update_ignore", ear.getActionName());
    Assert.assertEquals(null, ear.getCommandName());
    Assert.assertEquals(1, ear.getResourceFilters().size());
    RequestResourceFilter resourceFilter = ear.getResourceFilters().get(0);

    Assert.assertEquals("NAGIOS", resourceFilter.getServiceName());
    Assert.assertEquals("NAGIOS_SERVER", resourceFilter.getComponentName());
    Assert.assertEquals("c1", ear.getClusterName());
    Assert.assertTrue(map.containsKey("context"));  
  }

  private void testHost(MaintenanceState state) throws Exception {
    AmbariManagementController amc = createMock(AmbariManagementController.class);
    Capture<ExecuteActionRequest> earCapture = new Capture<ExecuteActionRequest>();
    Capture<Map<String, String>> rpCapture = new Capture<Map<String, String>>();
    expect(amc.createAction(capture(earCapture), capture(rpCapture))).andReturn(null);
    Injector injector = createStrictMock(Injector.class);
    MaintenanceStateHelper maintenanceStateHelper = createMockBuilder
      (MaintenanceStateHelper.class).withConstructor(injector).createMock();

    Clusters clusters = createMock(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    Service service = createMock(Service.class);
    
    ServiceComponent sc1 = createMock(ServiceComponent.class);
    ServiceComponent sc2 = createMock(ServiceComponent.class);
    expect(sc1.isClientComponent()).andReturn(Boolean.FALSE).anyTimes();
    expect(sc2.isClientComponent()).andReturn(Boolean.TRUE).anyTimes();

    ServiceComponentHost sch1 = createMock(ServiceComponentHost.class);
    Map<String, ServiceComponentHost> schMap = new HashMap<String, ServiceComponentHost>();
    schMap.put("h1", sch1);
    expect(sch1.getHostName()).andReturn("h1");
    expect(sch1.getServiceName()).andReturn("HDFS").anyTimes();
    expect(sch1.getServiceComponentName()).andReturn("NAMENODE").anyTimes();
    
    List<ServiceComponentHost> schList = new ArrayList<ServiceComponentHost>(schMap.values());
    
    expect(amc.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getClusterById(1L)).andReturn(cluster);
    expect(cluster.getClusterName()).andReturn("c1").anyTimes();
    expect(cluster.getService("HDFS")).andReturn(service).anyTimes();
    expect(cluster.getClusterId()).andReturn(Long.valueOf(1L));
    expect(cluster.getServiceComponentHosts("h1")).andReturn(schList);
    expect(service.getServiceComponent("NAMENODE")).andReturn(sc1);
    
    Host host = createMock(Host.class);
    expect(host.getHostName()).andReturn("h1").anyTimes();
    expect(host.getMaintenanceState(1L)).andReturn(state);
    
    replay(amc, clusters, cluster, service, sch1, host, maintenanceStateHelper);
    
    Map<String, String> map = new HashMap<String, String>();
    map.put("context", "abc");
    maintenanceStateHelper.createRequests(amc, map, Collections.singleton(cluster.getClusterName()));
    
    ExecuteActionRequest ear = earCapture.getValue();
    rpCapture.getValue();
    
    Assert.assertEquals("nagios_update_ignore", ear.getActionName());
    Assert.assertEquals(null, ear.getCommandName());
    Assert.assertEquals(1, ear.getResourceFilters().size());
    RequestResourceFilter resourceFilter = ear.getResourceFilters().get(0);
    Assert.assertEquals("NAGIOS", resourceFilter.getServiceName());
    Assert.assertEquals("NAGIOS_SERVER", resourceFilter.getComponentName());
    Assert.assertEquals("c1", ear.getClusterName());
    Assert.assertTrue(map.containsKey("context"));    
  }
  
  private void testService(MaintenanceState state) throws Exception {
    AmbariManagementController amc = createMock(AmbariManagementController.class);
    Capture<ExecuteActionRequest> earCapture = new Capture<ExecuteActionRequest>();
    Capture<Map<String, String>> rpCapture = new Capture<Map<String, String>>();
    expect(amc.createAction(capture(earCapture), capture(rpCapture))).andReturn(null);
    Injector injector = createStrictMock(Injector.class);
    MaintenanceStateHelper maintenanceStateHelper = createMockBuilder
      (MaintenanceStateHelper.class).withConstructor(injector).createMock();
    
    Clusters clusters = createMock(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    Service service = createMock(Service.class);
    
    ServiceComponent sc1 = createMock(ServiceComponent.class);
    ServiceComponent sc2 = createMock(ServiceComponent.class);
    expect(sc1.isClientComponent()).andReturn(Boolean.FALSE).anyTimes();
    expect(sc2.isClientComponent()).andReturn(Boolean.TRUE).anyTimes();
    
    ServiceComponentHost sch1 = createMock(ServiceComponentHost.class);
    Map<String, ServiceComponentHost> schMap = new HashMap<String, ServiceComponentHost>();
    schMap.put("h1", sch1);
    expect(sch1.getHostName()).andReturn("h1");
    expect(sch1.getServiceName()).andReturn("HDFS");
    expect(sch1.getServiceComponentName()).andReturn("NAMENODE");
    
    expect(sc1.getServiceComponentHosts()).andReturn(schMap);
    
    Map<String, ServiceComponent> scMap = new HashMap<String, ServiceComponent>();
    scMap.put("NAMENODE", sc1);
    scMap.put("HDFS_CLIENT", sc2);
    
    expect(amc.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getClusterById(1L)).andReturn(cluster);
    expect(cluster.getClusterName()).andReturn("c1");
    expect(cluster.getClusterId()).andReturn(1L);
    expect(service.getCluster()).andReturn(cluster);
    expect(service.getServiceComponents()).andReturn(scMap);
    expect(service.getMaintenanceState()).andReturn(state);
    expect(service.getName()).andReturn("HDFS");
    
    replay(amc, clusters, cluster, service, sc1, sc2, sch1, maintenanceStateHelper);
    
    Map<String, String> map = new HashMap<String, String>();
    map.put("context", "abc");
    maintenanceStateHelper.createRequests(amc, map, Collections.singleton("c1"));
    
    ExecuteActionRequest ear = earCapture.getValue();
    map = rpCapture.getValue();
    
    Assert.assertEquals("nagios_update_ignore", ear.getActionName());
    Assert.assertEquals(null, ear.getCommandName());
    Assert.assertEquals(1, ear.getResourceFilters().size());
    RequestResourceFilter resourceFilter = ear.getResourceFilters().get(0);
    Assert.assertEquals("NAGIOS", resourceFilter.getServiceName());
    Assert.assertEquals("NAGIOS_SERVER", resourceFilter.getComponentName());
    Assert.assertEquals("c1", ear.getClusterName());
    Assert.assertTrue(map.containsKey("context"));
  }

  @Test
  public void testHostComponentImpliedState() throws Exception {
    Injector injector = createStrictMock(Injector.class);
    MaintenanceStateHelper maintenanceStateHelper =
      createMockBuilder(MaintenanceStateHelper.class)
        .withConstructor(injector)
        .createNiceMock();

    Clusters clusters = createMock(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    ServiceComponentHost sch = createMock(ServiceComponentHost.class);
    Service service = createNiceMock(Service.class);
    final Host host = createNiceMock(Host.class);

    expect(sch.getClusterName()).andReturn("c1").anyTimes();
    expect(clusters.getCluster("c1")).andReturn(cluster).anyTimes();
    expect(cluster.getClusterName()).andReturn("c1").anyTimes();
    expect(cluster.getClusterId()).andReturn(1L).anyTimes();
    expect(clusters.getHost("h1")).andReturn(host).anyTimes();
    expect(sch.getHostName()).andReturn("h1").anyTimes();
    expect(sch.getServiceName()).andReturn("HDFS").anyTimes();
    expect(cluster.getService("HDFS")).andReturn(service).anyTimes();

    expect(sch.getMaintenanceState())
      .andReturn(MaintenanceState.ON).times(1)
      .andReturn(MaintenanceState.OFF).anyTimes();
    expect(service.getMaintenanceState()).andReturn(MaintenanceState.ON);
    expect(host.getMaintenanceState(1L)).andReturn(MaintenanceState.ON);
    expect(service.getMaintenanceState()).andReturn(MaintenanceState.ON);
    expect(host.getMaintenanceState(1L)).andReturn(MaintenanceState.OFF);
    expect(service.getMaintenanceState()).andReturn(MaintenanceState.OFF);
    expect(host.getMaintenanceState(1L)).andReturn(MaintenanceState.ON);

    Class<?> maintenanceHelperClass = MaintenanceStateHelper.class;
    Field f = maintenanceHelperClass.getDeclaredField("clusters");
    f.setAccessible(true);
    f.set(maintenanceStateHelper, clusters);

    replay(maintenanceStateHelper, clusters, cluster, sch, host, service);

    MaintenanceState state = maintenanceStateHelper.getEffectiveState(sch);
    Assert.assertEquals(MaintenanceState.ON, state);

    state = maintenanceStateHelper.getEffectiveState(sch);
    Assert.assertEquals(MaintenanceState.IMPLIED_FROM_SERVICE_AND_HOST, state);

    state = maintenanceStateHelper.getEffectiveState(sch);
    Assert.assertEquals(MaintenanceState.IMPLIED_FROM_SERVICE, state);

    state = maintenanceStateHelper.getEffectiveState(sch);
    Assert.assertEquals(MaintenanceState.IMPLIED_FROM_HOST, state);

    verify(maintenanceStateHelper, clusters, cluster, sch, host, service);
  }

  @Test
  public void testHostComponentOperationsAllowance() throws Exception {
    Injector injector = createStrictMock(Injector.class);
    MaintenanceStateHelper maintenanceStateHelper =
      createMockBuilder(MaintenanceStateHelper.class)
        .withConstructor(injector)
        .createNiceMock();

    ServiceComponentHost sch = createMock(ServiceComponentHost.class);

    // Cluster
    expect(sch.getMaintenanceState()).andReturn(MaintenanceState.ON);
    expect(sch.getMaintenanceState()).andReturn(MaintenanceState.IMPLIED_FROM_HOST);
    expect(sch.getMaintenanceState()).andReturn(MaintenanceState.IMPLIED_FROM_SERVICE);
    expect(sch.getMaintenanceState()).andReturn(MaintenanceState.IMPLIED_FROM_SERVICE_AND_HOST);
    expect(sch.getMaintenanceState()).andReturn(MaintenanceState.OFF);

    // Service
    expect(sch.getMaintenanceState()).andReturn(MaintenanceState.ON);
    expect(sch.getMaintenanceState()).andReturn(MaintenanceState.IMPLIED_FROM_HOST);
    expect(sch.getMaintenanceState()).andReturn(MaintenanceState.IMPLIED_FROM_SERVICE);
    expect(sch.getMaintenanceState()).andReturn(MaintenanceState.IMPLIED_FROM_SERVICE_AND_HOST);
    expect(sch.getMaintenanceState()).andReturn(MaintenanceState.OFF);

    // Host
    expect(sch.getMaintenanceState()).andReturn(MaintenanceState.ON);
    expect(sch.getMaintenanceState()).andReturn(MaintenanceState.IMPLIED_FROM_HOST);
    expect(sch.getMaintenanceState()).andReturn(MaintenanceState.IMPLIED_FROM_SERVICE);
    expect(sch.getMaintenanceState()).andReturn(MaintenanceState.IMPLIED_FROM_SERVICE_AND_HOST);
    expect(sch.getMaintenanceState()).andReturn(MaintenanceState.OFF);

    // HostComponent
    expect(sch.getMaintenanceState()).andReturn(MaintenanceState.ON);
    expect(sch.getMaintenanceState()).andReturn(MaintenanceState.IMPLIED_FROM_HOST);
    expect(sch.getMaintenanceState()).andReturn(MaintenanceState.IMPLIED_FROM_SERVICE);
    expect(sch.getMaintenanceState()).andReturn(MaintenanceState.IMPLIED_FROM_SERVICE_AND_HOST);
    expect(sch.getMaintenanceState()).andReturn(MaintenanceState.OFF);


    replay(maintenanceStateHelper, sch);

    // Cluster
    Assert.assertEquals(false, maintenanceStateHelper.isOperationAllowed(Resource.Type.Cluster, sch));
    Assert.assertEquals(false, maintenanceStateHelper.isOperationAllowed(Resource.Type.Cluster, sch));
    Assert.assertEquals(false, maintenanceStateHelper.isOperationAllowed(Resource.Type.Cluster, sch));
    Assert.assertEquals(false, maintenanceStateHelper.isOperationAllowed(Resource.Type.Cluster, sch));
    Assert.assertEquals(true, maintenanceStateHelper.isOperationAllowed(Resource.Type.Cluster, sch));

    // Service
    Assert.assertEquals(false, maintenanceStateHelper.isOperationAllowed(Resource.Type.Service, sch));
    Assert.assertEquals(false, maintenanceStateHelper.isOperationAllowed(Resource.Type.Service, sch));
    Assert.assertEquals(true, maintenanceStateHelper.isOperationAllowed(Resource.Type.Service, sch));
    Assert.assertEquals(false , maintenanceStateHelper.isOperationAllowed(Resource.Type.Service, sch));
    Assert.assertEquals(true, maintenanceStateHelper.isOperationAllowed(Resource.Type.Service, sch));

    // Host
    Assert.assertEquals(false, maintenanceStateHelper.isOperationAllowed(Resource.Type.Host, sch));
    Assert.assertEquals(true, maintenanceStateHelper.isOperationAllowed(Resource.Type.Host, sch));
    Assert.assertEquals(false, maintenanceStateHelper.isOperationAllowed(Resource.Type.Host, sch));
    Assert.assertEquals(false, maintenanceStateHelper.isOperationAllowed(Resource.Type.Host, sch));
    Assert.assertEquals(true, maintenanceStateHelper.isOperationAllowed(Resource.Type.Host, sch));

    // HostComponent
    Assert.assertEquals(true, maintenanceStateHelper.isOperationAllowed(Resource.Type.HostComponent, sch));
    Assert.assertEquals(true, maintenanceStateHelper.isOperationAllowed(Resource.Type.HostComponent, sch));
    Assert.assertEquals(true, maintenanceStateHelper.isOperationAllowed(Resource.Type.HostComponent, sch));
    Assert.assertEquals(true, maintenanceStateHelper.isOperationAllowed(Resource.Type.HostComponent, sch));
    Assert.assertEquals(true, maintenanceStateHelper.isOperationAllowed(Resource.Type.HostComponent, sch));

    verify(maintenanceStateHelper, sch);
  }
}
