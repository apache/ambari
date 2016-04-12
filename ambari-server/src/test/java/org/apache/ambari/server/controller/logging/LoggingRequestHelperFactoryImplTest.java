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
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.easymock.EasyMockSupport;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;

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

    Config logSearchSiteConfig =
      mockSupport.createMock(Config.class);

    ServiceComponentHost serviceComponentHostMock =
      mockSupport.createMock(ServiceComponentHost.class);

    Map<String, String> testProperties =
      new HashMap<String, String>();
    testProperties.put("logsearch.ui.port", expectedPortNumber);

    expect(controllerMock.getClusters()).andReturn(clustersMock).atLeastOnce();
    expect(clustersMock.getCluster(expectedClusterName)).andReturn(clusterMock).atLeastOnce();
    expect(clusterMock.getDesiredConfigByType("logsearch-site")).andReturn(logSearchSiteConfig).atLeastOnce();
    expect(clusterMock.getServiceComponentHosts("LOGSEARCH", "LOGSEARCH_SERVER")).andReturn(Collections.singletonList(serviceComponentHostMock)).atLeastOnce();
    expect(logSearchSiteConfig.getProperties()).andReturn(testProperties).atLeastOnce();
    expect(serviceComponentHostMock.getHostName()).andReturn(expectedHostName).atLeastOnce();

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
  public void testHelperCreationWithNoLogSearchServersAvailable() throws Exception {
    final String expectedClusterName = "testclusterone";
    final String expectedPortNumber = "61889";

    EasyMockSupport mockSupport = new EasyMockSupport();

    AmbariManagementController controllerMock =
      mockSupport.createMock(AmbariManagementController.class);

    Clusters clustersMock =
      mockSupport.createMock(Clusters.class);

    Cluster clusterMock =
      mockSupport.createMock(Cluster.class);

    Config logSearchSiteConfig =
      mockSupport.createMock(Config.class);

    Map<String, String> testProperties =
      new HashMap<String, String>();
    testProperties.put("logsearch.ui.port", expectedPortNumber);

    expect(controllerMock.getClusters()).andReturn(clustersMock).atLeastOnce();
    expect(clustersMock.getCluster(expectedClusterName)).andReturn(clusterMock).atLeastOnce();
    expect(clusterMock.getDesiredConfigByType("logsearch-site")).andReturn(logSearchSiteConfig).atLeastOnce();
    expect(clusterMock.getServiceComponentHosts("LOGSEARCH", "LOGSEARCH_SERVER")).andReturn(Collections.<ServiceComponentHost>emptyList()).atLeastOnce();

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
