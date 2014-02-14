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
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.PassiveState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.easymock.Capture;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the PassiveAlertHelper class
 */
public class PassiveStateHelperTest {

  @Test
  public void testService() throws Exception {
    testService(PassiveState.PASSIVE);
    testService(PassiveState.ACTIVE);
  }
  
  @Test
  public void testHost() throws Exception {
    testHost(PassiveState.PASSIVE);
    testHost(PassiveState.ACTIVE);
  }
  
  @Test
  public void testHostComponent() throws Exception {
    testHostComponent(PassiveState.PASSIVE);
    testHostComponent(PassiveState.ACTIVE);
  }
  
  private void testHostComponent(PassiveState state) throws Exception {
    AmbariManagementController amc = createMock(AmbariManagementController.class);
    Capture<ExecuteActionRequest> earCapture = new Capture<ExecuteActionRequest>();
    Capture<Map<String, String>> rpCapture = new Capture<Map<String, String>>();
    expect(amc.createAction(capture(earCapture), capture(rpCapture))).andReturn(null);
    
    Cluster cluster = createMock(Cluster.class);
    expect(cluster.getClusterName()).andReturn("c1").anyTimes();
    
    ServiceComponentHost sch = createMock(ServiceComponentHost.class);
    expect(sch.getClusterName()).andReturn("c1");
    expect(sch.getPassiveState()).andReturn(state);
    expect(sch.getServiceName()).andReturn("HDFS");
    expect(sch.getServiceComponentName()).andReturn("NAMENODE").anyTimes();
    expect(sch.getHostName()).andReturn("h1");
    
    replay(amc, cluster, sch);
    
    Map<String, String> map = new HashMap<String, String>();
    map.put("context", "abc");
    PassiveStateHelper.createRequest(amc, sch.getClusterName(), map);
    
    ExecuteActionRequest ear = earCapture.getValue();
    map = rpCapture.getValue();
    
    Assert.assertEquals("nagios_update_ignore", ear.getActionName());
    Assert.assertEquals("ACTIONEXECUTE", ear.getCommandName());
    Assert.assertEquals("NAGIOS", ear.getServiceName());
    Assert.assertEquals("NAGIOS_SERVER", ear.getComponentName());
    Assert.assertEquals("c1", ear.getClusterName());
    Assert.assertTrue(map.containsKey("context"));  
  }
  
  private void testHost(PassiveState state) throws Exception {
    AmbariManagementController amc = createMock(AmbariManagementController.class);
    Capture<ExecuteActionRequest> earCapture = new Capture<ExecuteActionRequest>();
    Capture<Map<String, String>> rpCapture = new Capture<Map<String, String>>();
    expect(amc.createAction(capture(earCapture), capture(rpCapture))).andReturn(null);
    
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
    
    expect(cluster.getClusterName()).andReturn("c1").anyTimes();
    expect(cluster.getService("HDFS")).andReturn(service).anyTimes();
    expect(cluster.getClusterId()).andReturn(Long.valueOf(1L));
    expect(cluster.getServiceComponentHosts("h1")).andReturn(schList);
    expect(service.getServiceComponent("NAMENODE")).andReturn(sc1);
    
    Host host = createMock(Host.class);
    expect(host.getHostName()).andReturn("h1").anyTimes();
    expect(host.getPassiveState(1L)).andReturn(state);
    
    replay(amc, cluster, service, sch1, host);
    
    Map<String, String> map = new HashMap<String, String>();
    map.put("context", "abc");
    PassiveStateHelper.createRequest(amc, cluster.getClusterName(), map);
    
    ExecuteActionRequest ear = earCapture.getValue();
    rpCapture.getValue();
    
    Assert.assertEquals("nagios_update_ignore", ear.getActionName());
    Assert.assertEquals("ACTIONEXECUTE", ear.getCommandName());
    Assert.assertEquals("NAGIOS", ear.getServiceName());
    Assert.assertEquals("NAGIOS_SERVER", ear.getComponentName());
    Assert.assertEquals("c1", ear.getClusterName());
    Assert.assertTrue(map.containsKey("context"));    
  }
  
  
  private void testService(PassiveState state) throws Exception {
    AmbariManagementController amc = createMock(AmbariManagementController.class);
    Capture<ExecuteActionRequest> earCapture = new Capture<ExecuteActionRequest>();
    Capture<Map<String, String>> rpCapture = new Capture<Map<String, String>>();
    expect(amc.createAction(capture(earCapture), capture(rpCapture))).andReturn(null);
    
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
    
    expect(cluster.getClusterName()).andReturn("c1");
    expect(service.getCluster()).andReturn(cluster);
    expect(service.getServiceComponents()).andReturn(scMap);
    expect(service.getPassiveState()).andReturn(state);
    expect(service.getName()).andReturn("HDFS");
    
    replay(amc, cluster, service, sc1, sc2, sch1);
    
    Map<String, String> map = new HashMap<String, String>();
    map.put("context", "abc");
    PassiveStateHelper.createRequest(amc, "c1", map);
    
    ExecuteActionRequest ear = earCapture.getValue();
    map = rpCapture.getValue();
    
    Assert.assertEquals("nagios_update_ignore", ear.getActionName());
    Assert.assertEquals("ACTIONEXECUTE", ear.getCommandName());
    Assert.assertEquals("NAGIOS", ear.getServiceName());
    Assert.assertEquals("NAGIOS_SERVER", ear.getComponentName());
    Assert.assertEquals("c1", ear.getClusterName());
    Assert.assertTrue(map.containsKey("context"));
  }
  
  
  
  
}
