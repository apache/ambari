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

package org.apache.ambari.server.api.services;

import org.apache.ambari.server.api.handlers.RequestHandler;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;


/**
 * Unit tests for ClusterService.
 */
public class ClusterServiceTest extends BaseServiceTest {

  @Test
   public void testGetCluster() {
    String clusterName = "clusterName";

    registerExpectations(Request.Type.GET, null, 200, false);
    replayMocks();

    //test
    ClusterService clusterService = new TestClusterService(getResource(), clusterName, getRequestFactory(), getRequestHandler());
    Response response = clusterService.getCluster(getHttpHeaders(), getUriInfo(), clusterName);
    verifyResults(response, 200);
  }

  @Test
  public void testGetCluster__ErrorState() {
    String clusterName = "clusterName";

    registerExpectations(Request.Type.GET, null, 500, true);
    replayMocks();

    //test
    ClusterService clusterService = new TestClusterService(getResource(), clusterName, getRequestFactory(), getRequestHandler());
    Response response = clusterService.getCluster(getHttpHeaders(), getUriInfo(), clusterName);
    verifyResults(response, 500);
  }

  @Test
  public void testGetClusters() {

    registerExpectations(Request.Type.GET, null, 200, false);
    replayMocks();

    //test
    ClusterService clusterService = new TestClusterService(getResource(), null, getRequestFactory(), getRequestHandler());
    Response response = clusterService.getClusters(getHttpHeaders(), getUriInfo());

    verifyResults(response, 200);
  }

  @Test
  public void testGetClusters__ErrorState() {
    registerExpectations(Request.Type.GET, null, 500, true);
    replayMocks();

    //test
    ClusterService clusterService = new TestClusterService(getResource(), null, getRequestFactory(), getRequestHandler());
    Response response = clusterService.getClusters(getHttpHeaders(), getUriInfo());

    verifyResults(response, 500);
  }

  @Test
  public void testCreateCluster() {
    String clusterName = "clusterName";
    String body = "body";

    registerExpectations(Request.Type.POST, body, 201, false);
    replayMocks();

    //test
    ClusterService clusterService = new TestClusterService(getResource(), clusterName, getRequestFactory(), getRequestHandler());
    Response response = clusterService.createCluster(body, getHttpHeaders(), getUriInfo(), clusterName);

    verifyResults(response, 201);
  }

  @Test
  public void testCreateCluster__ErrorState() {
    String clusterName = "clusterName";
    String body = "body";

    registerExpectations(Request.Type.POST, body, 500, true);
    replayMocks();

    //test
    ClusterService clusterService = new TestClusterService(getResource(), clusterName, getRequestFactory(), getRequestHandler());
    Response response = clusterService.createCluster(body, getHttpHeaders(), getUriInfo(), clusterName);

    verifyResults(response, 500);
  }

  @Test
  public void testUpdateCluster() {
    String clusterName = "clusterName";
    String body = "body";

    registerExpectations(Request.Type.PUT, body, 200, false);
    replayMocks();

    //test
    ClusterService clusterService = new TestClusterService(getResource(), clusterName, getRequestFactory(), getRequestHandler());
    Response response = clusterService.updateCluster(body, getHttpHeaders(), getUriInfo(), clusterName);

    verifyResults(response, 200);
  }

  @Test
  public void testUpdateCluster__ErrorState() {
    String clusterName = "clusterName";
    String body = "body";

    registerExpectations(Request.Type.PUT, body, 500, true);
    replayMocks();

    //test
    ClusterService clusterService = new TestClusterService(getResource(), clusterName, getRequestFactory(), getRequestHandler());
    Response response = clusterService.updateCluster(body, getHttpHeaders(), getUriInfo(), clusterName);

    verifyResults(response, 500);
  }

  @Test
  public void testDeleteCluster() {
    String clusterName = "clusterName";

    registerExpectations(Request.Type.DELETE, null, 200, false);
    replayMocks();

    //test
    ClusterService clusterService = new TestClusterService(getResource(), clusterName, getRequestFactory(), getRequestHandler());
    Response response = clusterService.deleteCluster(getHttpHeaders(), getUriInfo(), clusterName);

    verifyResults(response, 200);
  }

  @Test
  public void testDeleteCluster__ErrorState() {
    String clusterName = "clusterName";

    registerExpectations(Request.Type.DELETE, null, 500, true);
    replayMocks();

    //test
    ClusterService clusterService = new TestClusterService(getResource(), clusterName, getRequestFactory(), getRequestHandler());
    Response response = clusterService.deleteCluster(getHttpHeaders(), getUriInfo(), clusterName);

    verifyResults(response, 500);
  }

  private class TestClusterService extends ClusterService {
    private RequestFactory m_requestFactory;
    private RequestHandler m_requestHandler;
    private ResourceInstance m_resourceDef;
    private String m_clusterId;

    private TestClusterService(ResourceInstance resourceDef, String clusterId, RequestFactory requestFactory,
                               RequestHandler handler) {
      m_resourceDef = resourceDef;
      m_requestFactory = requestFactory;
      m_requestHandler = handler;
      m_clusterId = clusterId;
    }

    @Override
    ResourceInstance createClusterResource(String clusterName) {
      assertEquals(m_clusterId, clusterName);
      return m_resourceDef;
    }

    @Override
    RequestFactory getRequestFactory() {
      return m_requestFactory;
    }

    @Override
    RequestHandler getRequestHandler(Request.Type requestType) {
      return m_requestHandler;
    }
  }

  //todo: test getHostHandler, getServiceHandler, getHostComponentHandler
}
