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

package org.apache.ambari.server.controller.logging;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.security.encryption.CredentialStoreService;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.State;
import org.easymock.EasyMockSupport;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class LoggingRequestHelperFactoryImplTest {

  @Test
  public void testHelperCreation() throws Exception {
    final String expectedClusterName = "testclusterone";
    final String expectedHostName = "c6410.ambari.apache.org";
    final String expectedPortNumber = "61889";

    EasyMockSupport mockSupport = new EasyMockSupport();

    AmbariManagementController controllerMock =
      mockSupport.createMock(AmbariManagementController.class);

    Clusters clustersMock =
      mockSupport.createMock(Clusters.class);

    Cluster clusterMock =
      mockSupport.createMock(Cluster.class);

    Config logSearchEnvConfig =
      mockSupport.createMock(Config.class);

    ServiceComponentHost serviceComponentHostMock =
      mockSupport.createMock(ServiceComponentHost.class);

    CredentialStoreService credentialStoreServiceMock =
      mockSupport.createMock(CredentialStoreService.class);

    Map<String, String> testProperties =
      new HashMap<String, String>();
    testProperties.put("logsearch_ui_port", expectedPortNumber);

    expect(controllerMock.getClusters()).andReturn(clustersMock).atLeastOnce();
    expect(controllerMock.getCredentialStoreService()).andReturn(credentialStoreServiceMock).atLeastOnce();
    expect(clustersMock.getCluster(expectedClusterName)).andReturn(clusterMock).atLeastOnce();
    expect(clusterMock.getDesiredConfigByType("logsearch-env")).andReturn(logSearchEnvConfig).atLeastOnce();
    expect(clusterMock.getServiceComponentHosts("LOGSEARCH", "LOGSEARCH_SERVER")).andReturn(Collections.singletonList(serviceComponentHostMock)).atLeastOnce();
    expect(clusterMock.getServices()).andReturn(Collections.singletonMap("LOGSEARCH", (Service) null)).atLeastOnce();
    expect(logSearchEnvConfig.getProperties()).andReturn(testProperties).atLeastOnce();
    expect(serviceComponentHostMock.getHostName()).andReturn(expectedHostName).atLeastOnce();
    expect(serviceComponentHostMock.getState()).andReturn(State.STARTED).atLeastOnce();


    mockSupport.replayAll();

    LoggingRequestHelperFactory helperFactory =
      new LoggingRequestHelperFactoryImpl();

    LoggingRequestHelper helper =
      helperFactory.getHelper(controllerMock, expectedClusterName);

    assertNotNull("LoggingRequestHelper object returned by the factory was null",
      helper);

    assertTrue("Helper created was not of the expected type",
      helper instanceof LoggingRequestHelperImpl);

    mockSupport.verifyAll();
  }

  @Test
  public void testHelperCreationLogSearchServerNotStarted() throws Exception {
    final String expectedClusterName = "testclusterone";
    final String expectedHostName = "c6410.ambari.apache.org";
    final String expectedPortNumber = "61889";

    EasyMockSupport mockSupport = new EasyMockSupport();

    AmbariManagementController controllerMock =
      mockSupport.createMock(AmbariManagementController.class);

    Clusters clustersMock =
      mockSupport.createMock(Clusters.class);

    Cluster clusterMock =
      mockSupport.createMock(Cluster.class);

    Config logSearchEnvConfig =
      mockSupport.createMock(Config.class);

    ServiceComponentHost serviceComponentHostMock =
      mockSupport.createMock(ServiceComponentHost.class);

    Map<String, String> testProperties =
      new HashMap<String, String>();
    testProperties.put("logsearch_ui_port", expectedPortNumber);

    expect(controllerMock.getClusters()).andReturn(clustersMock).atLeastOnce();
    expect(clustersMock.getCluster(expectedClusterName)).andReturn(clusterMock).atLeastOnce();
    expect(clusterMock.getDesiredConfigByType("logsearch-env")).andReturn(logSearchEnvConfig).atLeastOnce();
    expect(clusterMock.getServiceComponentHosts("LOGSEARCH", "LOGSEARCH_SERVER")).andReturn(Collections.singletonList(serviceComponentHostMock)).atLeastOnce();
    expect(clusterMock.getServices()).andReturn(Collections.singletonMap("LOGSEARCH", (Service) null)).atLeastOnce();

    // set the LOGSEARCH_SERVER's state to INSTALLED, to simulate the case where
    // the server is installed, but not started
    expect(serviceComponentHostMock.getState()).andReturn(State.INSTALLED).atLeastOnce();


    mockSupport.replayAll();

    LoggingRequestHelperFactory helperFactory =
      new LoggingRequestHelperFactoryImpl();

    LoggingRequestHelper helper =
      helperFactory.getHelper(controllerMock, expectedClusterName);

    assertNull("LoggingRequestHelper object returned by the factory should have been null",
      helper);

    mockSupport.verifyAll();
  }

  @Test
   public void testHelperCreationWithNoLogSearchServersAvailable() throws Exception {
    final String expectedClusterName = "testclusterone";

    EasyMockSupport mockSupport = new EasyMockSupport();

    AmbariManagementController controllerMock =
      mockSupport.createMock(AmbariManagementController.class);

    Clusters clustersMock =
      mockSupport.createMock(Clusters.class);

    Cluster clusterMock =
      mockSupport.createMock(Cluster.class);

    Config logSearchEnvConfig =
      mockSupport.createMock(Config.class);

    expect(controllerMock.getClusters()).andReturn(clustersMock).atLeastOnce();
    expect(clustersMock.getCluster(expectedClusterName)).andReturn(clusterMock).atLeastOnce();
    expect(clusterMock.getDesiredConfigByType("logsearch-env")).andReturn(logSearchEnvConfig).atLeastOnce();
    expect(clusterMock.getServiceComponentHosts("LOGSEARCH", "LOGSEARCH_SERVER")).andReturn(Collections.<ServiceComponentHost>emptyList()).atLeastOnce();
    expect(clusterMock.getServices()).andReturn(Collections.singletonMap("LOGSEARCH", (Service)null)).atLeastOnce();

    mockSupport.replayAll();

    LoggingRequestHelperFactory helperFactory =
      new LoggingRequestHelperFactoryImpl();

    LoggingRequestHelper helper =
      helperFactory.getHelper(controllerMock, expectedClusterName);

    assertNull("LoggingRequestHelper object returned by the factory should have been null",
      helper);

    mockSupport.verifyAll();
  }

  @Test
  public void testHelperCreationWithNoLogSearchServiceDeployed() throws Exception {
    final String expectedClusterName = "testclusterone";

    EasyMockSupport mockSupport = new EasyMockSupport();

    AmbariManagementController controllerMock =
      mockSupport.createMock(AmbariManagementController.class);

    Clusters clustersMock =
      mockSupport.createMock(Clusters.class);

    Cluster clusterMock =
      mockSupport.createMock(Cluster.class);

    expect(controllerMock.getClusters()).andReturn(clustersMock).atLeastOnce();
    expect(clustersMock.getCluster(expectedClusterName)).andReturn(clusterMock).atLeastOnce();
    // do not include LOGSEARCH in this map, to simulate the case when LogSearch is not deployed
    expect(clusterMock.getServices()).andReturn(Collections.singletonMap("HDFS", (Service)null)).atLeastOnce();

    mockSupport.replayAll();

    LoggingRequestHelperFactory helperFactory =
      new LoggingRequestHelperFactoryImpl();

    LoggingRequestHelper helper =
      helperFactory.getHelper(controllerMock, expectedClusterName);

    assertNull("LoggingRequestHelper object returned by the factory should have been null",
      helper);

    mockSupport.verifyAll();
  }


}
