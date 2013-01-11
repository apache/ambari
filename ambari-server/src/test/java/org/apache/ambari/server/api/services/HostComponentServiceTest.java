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
 * Unit tests for HostComponentService.
 */
public class HostComponentServiceTest extends BaseServiceTest {
  @Test
  public void testGetHostComponent() {
    String clusterName = "clusterName";
    String hostName = "hostName";
    String hostComponentName = "hostComponentName";

    registerExpectations(Request.Type.GET, null, 200, false);
    replayMocks();

    //test
    HostComponentService hostComponentService = new TestHostComponentService(getResource(), clusterName,
        hostName, hostComponentName, getRequestFactory(), getRequestHandler());

    Response response = hostComponentService.getHostComponent(getHttpHeaders(), getUriInfo(), hostComponentName);
    verifyResults(response, 200);
  }

  @Test
  public void testGetHostComponent__ErrorState() {
    String clusterName = "clusterName";
    String hostName = "hostName";
    String hostComponentName = "hostComponentName";

    registerExpectations(Request.Type.GET, null, 404, true);
    replayMocks();

    //test
    HostComponentService hostComponentService = new TestHostComponentService(getResource(), clusterName,
        hostName, hostComponentName, getRequestFactory(), getRequestHandler());

    Response response = hostComponentService.getHostComponent(getHttpHeaders(), getUriInfo(), hostComponentName);
    verifyResults(response, 404);
  }

  @Test
  public void testGetHostComponents() {
    String clusterName = "clusterName";
    String hostName = "hostName";

    registerExpectations(Request.Type.GET, null, 200, false);
    replayMocks();

    //test
    HostComponentService hostComponentService = new TestHostComponentService(getResource(), clusterName,
        hostName, null, getRequestFactory(), getRequestHandler());

    Response response = hostComponentService.getHostComponents(getHttpHeaders(), getUriInfo());
    verifyResults(response, 200);
  }

  @Test
  public void testGetHostComponents__ErrorState() {
    String clusterName = "clusterName";
    String hostName = "hostName";

    registerExpectations(Request.Type.GET, null, 500, true);
    replayMocks();

    //test
    HostComponentService hostComponentService = new TestHostComponentService(getResource(), clusterName,
        hostName, null, getRequestFactory(), getRequestHandler());

    Response response = hostComponentService.getHostComponents(getHttpHeaders(), getUriInfo());
    verifyResults(response, 500);
  }

  @Test
  public void testCreateHostComponent() {
    String clusterName = "clusterName";
    String hostName = "hostName";
    String hostComponentName = "hostComponentName";
    String body = "body";

    registerExpectations(Request.Type.POST, body, 201, false);
    replayMocks();

    //test
    HostComponentService hostComponentService = new TestHostComponentService(getResource(), clusterName,
        hostName, hostComponentName, getRequestFactory(), getRequestHandler());

    Response response = hostComponentService.createHostComponent(body, getHttpHeaders(), getUriInfo(), hostComponentName);
    verifyResults(response, 201);
  }

  @Test
  public void testCreateHostComponent__ErrorState() {
    String clusterName = "clusterName";
    String hostName = "hostName";
    String hostComponentName = "hostComponentName";
    String body = "body";

    registerExpectations(Request.Type.POST, body, 500, true);
    replayMocks();

    //test
    HostComponentService hostComponentService = new TestHostComponentService(getResource(), clusterName,
        hostName, hostComponentName, getRequestFactory(), getRequestHandler());

    Response response = hostComponentService.createHostComponent(body, getHttpHeaders(), getUriInfo(), hostComponentName);
    verifyResults(response, 500);
  }

  @Test
  public void testUpdateHostComponent() {
    String clusterName = "clusterName";
    String hostName = "hostName";
    String hostComponentName = "hostComponentName";
    String body = "body";

    registerExpectations(Request.Type.PUT, body, 200, false);
    replayMocks();

    //test
    HostComponentService hostComponentService = new TestHostComponentService(getResource(), clusterName,
        hostName, hostComponentName, getRequestFactory(), getRequestHandler());

    Response response = hostComponentService.updateHostComponent(body, getHttpHeaders(), getUriInfo(), hostComponentName);
    verifyResults(response, 200);
  }

  @Test
  public void testUpdateHostComponent__ErrorState() {
    String clusterName = "clusterName";
    String hostName = "hostName";
    String hostComponentName = "hostComponentName";
    String body = "body";

    registerExpectations(Request.Type.PUT, body, 500, true);
    replayMocks();

    //test
    HostComponentService hostComponentService = new TestHostComponentService(getResource(), clusterName,
        hostName, hostComponentName, getRequestFactory(), getRequestHandler());

    Response response = hostComponentService.updateHostComponent(body, getHttpHeaders(), getUriInfo(), hostComponentName);
    verifyResults(response, 500);
  }

  @Test
  public void testUpdateHostComponents() {
    String clusterName = "clusterName";
    String hostName = "hostName";
    String body = "body";

    registerExpectations(Request.Type.PUT, body, 200, false);
    replayMocks();

    //test
    HostComponentService hostComponentService = new TestHostComponentService(getResource(), clusterName,
        hostName, null, getRequestFactory(), getRequestHandler());

    Response response = hostComponentService.updateHostComponents(body, getHttpHeaders(), getUriInfo());
    verifyResults(response, 200);
  }

  @Test
  public void testUpdateHostComponents__ErrorState() {
    String clusterName = "clusterName";
    String hostName = "hostName";
    String body = "body";

    registerExpectations(Request.Type.PUT, body, 500, true);
    replayMocks();

    //test
    HostComponentService hostComponentService = new TestHostComponentService(getResource(), clusterName,
        hostName, null, getRequestFactory(), getRequestHandler());

    Response response = hostComponentService.updateHostComponents(body, getHttpHeaders(), getUriInfo());
    verifyResults(response, 500);
  }

  @Test
  public void testDeleteHostComponent() {
    String clusterName = "clusterName";
    String hostName = "hostName";
    String hostComponentName = "hostComponentName";

    registerExpectations(Request.Type.DELETE, null, 200, false);
    replayMocks();

    //test
    HostComponentService hostComponentService = new TestHostComponentService(getResource(), clusterName,
        hostName, hostComponentName, getRequestFactory(), getRequestHandler());

    Response response = hostComponentService.deleteHostComponent(getHttpHeaders(), getUriInfo(), hostComponentName);
    verifyResults(response, 200);
  }

  @Test
  public void testDeleteHostComponent__ErrorState() {
    String clusterName = "clusterName";
    String hostName = "hostName";
    String hostComponentName = "hostComponentName";

    registerExpectations(Request.Type.DELETE, null, 500, true);
    replayMocks();

    //test
    HostComponentService hostComponentService = new TestHostComponentService(getResource(), clusterName,
        hostName, hostComponentName, getRequestFactory(), getRequestHandler());

    Response response = hostComponentService.deleteHostComponent(getHttpHeaders(), getUriInfo(), hostComponentName);
    verifyResults(response, 500);
  }

  private class TestHostComponentService extends HostComponentService {
    private RequestFactory m_requestFactory;
    private RequestHandler m_requestHandler;
    private ResourceInstance m_resourceDef;
    private String m_clusterId;
    private String m_hostId;
    private String m_hostComponentId;

    private TestHostComponentService(ResourceInstance resourceDef, String clusterId, String hostId, String hostComponentId,
                                     RequestFactory requestFactory, RequestHandler handler) {
      super(clusterId, hostId);
      m_resourceDef = resourceDef;
      m_clusterId = clusterId;
      m_hostId = hostId;
      m_hostComponentId = hostComponentId;
      m_requestFactory = requestFactory;
      m_requestHandler = handler;
    }


    @Override
    ResourceInstance createHostComponentResource(String clusterName, String hostName, String hostComponentName) {
      assertEquals(m_clusterId, clusterName);
      assertEquals(m_hostId, hostName);
      assertEquals(m_hostComponentId, hostComponentName);
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

}
