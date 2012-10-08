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

import static org.junit.Assert.fail;

import java.util.Set;

import junit.framework.Assert;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.ActionDBAccessor;
import org.apache.ambari.server.actionmanager.ActionDBInMemoryImpl;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.agent.ActionQueue;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.cluster.ClustersImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AmbariManagementControllerTest {

  private AmbariManagementController controller;
  private Clusters clusters;

  @Before
  public void setup() {
    clusters = new ClustersImpl();
    ActionDBAccessor db = new ActionDBInMemoryImpl();
    ActionManager am = new ActionManager(5000, 1200000, new ActionQueue(),
        clusters, db);
    controller = new AmbariManagementControllerImpl(am, clusters);
  }

  @After
  public void teardown() {
    controller = null;
    clusters = null;
  }

  private void createCluster(String clusterName) throws AmbariException {
    ClusterRequest r = new ClusterRequest(null, clusterName, "1.0.0", null);
    controller.createCluster(r);
  }

  private void createService(String clusterName,
      String serviceName, State desiredState) throws AmbariException {
    String dStateStr = null;
    if (desiredState != null) {
      dStateStr = desiredState.toString();
    }
    ServiceRequest r = new ServiceRequest(clusterName, serviceName, null,
        dStateStr);
    controller.createService(r);
  }


  @Test
  public void testCreateCluster() throws AmbariException {
    String clusterName = "foo1";
    createCluster(clusterName);
    Set<ClusterResponse> r =
        controller.getClusters(new ClusterRequest(null, clusterName, null, null));
    Assert.assertEquals(1, r.size());
    ClusterResponse c = r.iterator().next();
    Assert.assertEquals(clusterName, c.getClusterName());
  }

  @Test
  public void testCreateService() throws AmbariException {
    String clusterName = "foo1";
    createCluster(clusterName);
    String serviceName = "HDFS";
    try {
      createService(clusterName, serviceName, State.INSTALLING);
      fail("Service creation should fail for invalid state");
    } catch (Exception e) {
      // Expected
    }
    try {
      clusters.getCluster(clusterName).getService(serviceName);
      fail("Service creation should have failed");
    } catch (Exception e) {
      // Expected
    }
    try {
      createService(clusterName, serviceName, State.INSTALLED);
      fail("Service creation should fail for invalid initial state");
    } catch (Exception e) {
      // Expected
    }

    createService(clusterName, serviceName, null);

    String serviceName2 = "MAPREDUCE";
    createService(clusterName, serviceName2, State.INIT);

    ServiceRequest r = new ServiceRequest(clusterName, null, null, null);
    Set<ServiceResponse> response = controller.getServices(r);
    Assert.assertEquals(2, response.size());

    for (ServiceResponse svc : response) {
      Assert.assertTrue(svc.getServiceName().equals(serviceName)
          || svc.getServiceName().equals(serviceName2));
      Assert.assertEquals("", svc.getDesiredStackVersion());
      Assert.assertEquals(State.INIT.toString(), svc.getDesiredState());
    }


  }

  @Test
  public void testCreateServiceComponent() throws AmbariException {

  }

  @Test
  public void testCreateServiceComponentHost() throws AmbariException {

  }

  @Test
  public void testCreateHost() throws AmbariException {

  }

  @Test
  public void testGetCluster() throws AmbariException {

  }

  @Test
  public void testGetService() throws AmbariException {

  }

  @Test
  public void testGetServiceComponent() throws AmbariException {

  }

  @Test
  public void testGetServiceComponentHost() throws AmbariException {

  }

  @Test
  public void testGetHost() throws AmbariException {

  }



}
