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
import javax.ws.rs.core.UriInfo;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for HostService.
 */
public class HostServiceTest extends BaseServiceTest {

  @Test
  public void testGetHost() {
    String clusterName = "clusterName";
    String hostName = "hostName";

    registerExpectations(Request.Type.GET, null, 200, false);
    replayMocks();

    //test
    HostService hostService = new TestHostService(getResource(), clusterName, hostName, getRequestFactory(), getRequestHandler());
    Response response = hostService.getHost(getHttpHeaders(), getUriInfo(), hostName);

    verifyResults(response, 200);
  }

  @Test
  public void testGetHost__ErrorState() {
    String clusterName = "clusterName";
    String hostName = "hostName";

    registerExpectations(Request.Type.GET, null, 500, true);
    replayMocks();

    //test
    HostService hostService = new TestHostService(getResource(), clusterName, hostName, getRequestFactory(), getRequestHandler());
    Response response = hostService.getHost(getHttpHeaders(), getUriInfo(), hostName);

    verifyResults(response, 500);
  }

  @Test
  public void testGetHosts() {
    String clusterName = "clusterName";

    registerExpectations(Request.Type.GET, null, 200, false);
    replayMocks();

    //test
    HostService hostService = new TestHostService(getResource(), clusterName, null, getRequestFactory(), getRequestHandler());
    Response response = hostService.getHosts(getHttpHeaders(), getUriInfo());

    verifyResults(response, 200);
  }

  @Test
  public void testGetHosts__ErrorState() {
    String clusterName = "clusterName";

    registerExpectations(Request.Type.GET, null, 500, true);
    replayMocks();

    //test
    HostService hostService = new TestHostService(getResource(), clusterName, null, getRequestFactory(), getRequestHandler());
    Response response = hostService.getHosts(getHttpHeaders(), getUriInfo());

    verifyResults(response, 500);
  }

  @Test
  public void testCreateHost() {
    String clusterName = "clusterName";
    String hostName = "hostName";
    String body = "body";

    registerExpectations(Request.Type.POST, body, 201, false);
    replayMocks();

    //test
    HostService hostService = new TestHostService(getResource(), clusterName, hostName, getRequestFactory(), getRequestHandler());
    Response response = hostService.createHost(body, getHttpHeaders(), getUriInfo(), hostName);

    verifyResults(response, 201);
  }

  @Test
  public void testCreateHost__ErrorState() {
    String clusterName = "clusterName";
    String hostName = "hostName";
    String body = "body";

    registerExpectations(Request.Type.POST, body, 500, true);
    replayMocks();

    //test
    HostService hostService = new TestHostService(getResource(), clusterName, hostName, getRequestFactory(), getRequestHandler());
    Response response = hostService.createHost(body, getHttpHeaders(), getUriInfo(), hostName);

    verifyResults(response, 500);
  }

  @Test
  public void testCreateHosts()  {
    String clusterName = "clusterName";
    String body = "[ " +
        "{\"Hosts\" : {" +
        "            \"cluster_name\" : \"mycluster\"," +
        "            \"host_name\" : \"host1\"" +
        "          }" +
        "}," +
        "{\"Hosts\" : {" +
        "            \"cluster_name\" : \"mycluster\"," +
        "            \"host_name\" : \"host2\"" +
        "          }" +
        "}," +
        "{\"Hosts\" : {" +
        "            \"cluster_name\" : \"mycluster\"," +
        "            \"host_name\" : \"host3\"" +
        "          }" +
        "}]";

    registerExpectations(Request.Type.POST, body, 201, false);
    replayMocks();

    //test
    HostService hostService = new TestHostService(getResource(), clusterName, null, getRequestFactory(), getRequestHandler());
    Response response = hostService.createHosts(body, getHttpHeaders(), getUriInfo());

    verifyResults(response, 201);
  }

  @Test
  public void testCreateHosts__ErrorState()  {
    String clusterName = "clusterName";
    String body = "body";

    registerExpectations(Request.Type.POST, body, 500, true);
    replayMocks();

    //test
    HostService hostService = new TestHostService(getResource(), clusterName, null, getRequestFactory(), getRequestHandler());
    Response response = hostService.createHosts(body, getHttpHeaders(), getUriInfo());

    verifyResults(response, 500);
  }

  @Test
  public void testUpdateHost() {
    String clusterName = "clusterName";
    String hostName = "hostName";
    String body = "body";

    registerExpectations(Request.Type.PUT, body, 200, false);
    replayMocks();

    //test
    HostService hostService = new TestHostService(getResource(), clusterName, hostName, getRequestFactory(), getRequestHandler());
    Response response = hostService.updateHost(body, getHttpHeaders(), getUriInfo(), hostName);

    verifyResults(response, 200);
  }

  @Test
  public void testUpdateHost__ErrorState() {
    String clusterName = "clusterName";
    String hostName = "hostName";
    String body = "body";

    registerExpectations(Request.Type.PUT, body, 500, true);
    replayMocks();

    //test
    HostService hostService = new TestHostService(getResource(), clusterName, hostName, getRequestFactory(), getRequestHandler());
    Response response = hostService.updateHost(body, getHttpHeaders(), getUriInfo(), hostName);

    verifyResults(response, 500);
  }

  @Test
  public void testUpdateHosts() {
    String clusterName = "clusterName";
    String body = "body";

    registerExpectations(Request.Type.PUT, body, 200, false);
    replayMocks();

    //test
    HostService hostService = new TestHostService(getResource(), clusterName, null, getRequestFactory(), getRequestHandler());
    Response response = hostService.updateHosts(body, getHttpHeaders(), getUriInfo());

    verifyResults(response, 200);
  }

  @Test
  public void testUpdateHosts__ErrorState() {
    String clusterName = "clusterName";
    String body = "body";

    registerExpectations(Request.Type.PUT, body, 500, true);
    replayMocks();

    //test
    HostService hostService = new TestHostService(getResource(), clusterName, null, getRequestFactory(), getRequestHandler());
    Response response = hostService.updateHosts(body, getHttpHeaders(), getUriInfo());

    verifyResults(response, 500);
  }

  @Test
  public void testDeleteHost() {
    String clusterName = "clusterName";
    String hostName = "hostName";

    registerExpectations(Request.Type.DELETE, null, 200, false);
    replayMocks();

    //test
    HostService hostService = new TestHostService(getResource(), clusterName, hostName, getRequestFactory(), getRequestHandler());
    Response response = hostService.deleteHost(getHttpHeaders(), getUriInfo(), hostName);

    verifyResults(response, 200);
  }

  @Test
  public void testDeleteHost__ErrorState() {
    String clusterName = "clusterName";
    String hostName = "hostName";

    registerExpectations(Request.Type.DELETE, null, 500, true);
    replayMocks();

    //test
    HostService hostService = new TestHostService(getResource(), clusterName, hostName, getRequestFactory(), getRequestHandler());
    Response response = hostService.deleteHost(getHttpHeaders(), getUriInfo(), hostName);

    verifyResults(response, 500);
  }


  private class TestHostService extends HostService {
    private RequestFactory m_requestFactory;
    private RequestHandler m_requestHandler;
    private ResourceInstance m_resourceDef;
    private String m_clusterId;
    private String m_hostId;

    private TestHostService(ResourceInstance resourceDef, String clusterId, String hostId, RequestFactory requestFactory,
                            RequestHandler handler) {
      super(clusterId);
      m_resourceDef = resourceDef;
      m_clusterId = clusterId;
      m_hostId = hostId;
      m_requestFactory = requestFactory;
      m_requestHandler = handler;
    }

    @Override
    ResourceInstance createHostResource(String clusterName, String hostName, UriInfo ui) {
      assertEquals(m_clusterId, clusterName);
      assertEquals(m_hostId, hostName);
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


