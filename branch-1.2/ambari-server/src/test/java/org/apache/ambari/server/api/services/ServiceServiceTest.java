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
 * Unit tests for ServiceService.
 */
public class ServiceServiceTest extends BaseServiceTest {

  @Test
  public void testGetService() {
    String clusterName = "clusterName";
    String serviceName = "serviceName";

    registerExpectations(Request.Type.GET, null, 200, false);
    replayMocks();

    //test
    ServiceService hostService = new TestServiceService(getResource(), clusterName, serviceName, getRequestFactory(), getRequestHandler());
    Response response = hostService.getService(getHttpHeaders(), getUriInfo(), serviceName);

    verifyResults(response, 200);
  }

  @Test
  public void testGetService__ErrorState() {
    String clusterName = "clusterName";
    String serviceName = "serviceName";

    registerExpectations(Request.Type.GET, null, 500, true);
    replayMocks();

    //test
    ServiceService hostService = new TestServiceService(getResource(), clusterName, serviceName, getRequestFactory(), getRequestHandler());
    Response response = hostService.getService(getHttpHeaders(), getUriInfo(), serviceName);

    verifyResults(response, 500);
  }

  @Test
  public void testGetServices()  {
    String clusterName = "clusterName";

    registerExpectations(Request.Type.GET, null, 200, false);
    replayMocks();

    //test
    ServiceService hostService = new TestServiceService(getResource(), clusterName, null, getRequestFactory(), getRequestHandler());
    Response response = hostService.getServices(getHttpHeaders(), getUriInfo());

    verifyResults(response, 200);
  }

  @Test
  public void testGetServices__ErrorState(){
    String clusterName = "clusterName";

    registerExpectations(Request.Type.GET, null, 400, false);
    replayMocks();

    //test
    ServiceService hostService = new TestServiceService(getResource(), clusterName, null, getRequestFactory(), getRequestHandler());
    Response response = hostService.getServices(getHttpHeaders(), getUriInfo());

    verifyResults(response, 400);
  }

  @Test
  public void testCreateService() {
    String clusterName = "clusterName";
    String serviceName = "serviceName";
    String body = "{body}";

    registerExpectations(Request.Type.POST, body, 201, false);
    replayMocks();

    //test
    ServiceService hostService = new TestServiceService(getResource(), clusterName, serviceName, getRequestFactory(), getRequestHandler());
    Response response = hostService.createService(body, getHttpHeaders(), getUriInfo(), serviceName);

    verifyResults(response, 201);
  }

  @Test
  public void testCreateService__ErrorState() {
    String clusterName = "clusterName";
    String serviceName = "serviceName";
    String body = "{body}";

    registerExpectations(Request.Type.POST, body, 500, true);
    replayMocks();

    //test
    ServiceService hostService = new TestServiceService(getResource(), clusterName, serviceName, getRequestFactory(), getRequestHandler());
    Response response = hostService.createService(body, getHttpHeaders(), getUriInfo(), serviceName);

    verifyResults(response, 500);
  }

  @Test
  public void testUpdateServices() {
    String clusterName = "clusterName";
    String body = "{body}";

    registerExpectations(Request.Type.PUT, body, 200, false);
    replayMocks();

    //test
    ServiceService hostService = new TestServiceService(getResource(), clusterName, null, getRequestFactory(), getRequestHandler());
    Response response = hostService.updateServices(body, getHttpHeaders(), getUriInfo());

    verifyResults(response, 200);
  }

  @Test
  public void testUpdateServices__ErrorState() {
    String clusterName = "clusterName";
    String body = "{body}";

    registerExpectations(Request.Type.PUT, body, 500, true);
    replayMocks();

    //test
    ServiceService hostService = new TestServiceService(getResource(), clusterName, null, getRequestFactory(), getRequestHandler());
    Response response = hostService.updateServices(body, getHttpHeaders(), getUriInfo());

    verifyResults(response, 500);
  }

  @Test
  public void testUpdateService() {
    String clusterName = "clusterName";
    String serviceName = "serviceName";
    String body = "{body}";

    registerExpectations(Request.Type.PUT, body, 200, false);
    replayMocks();

    //test
    ServiceService hostService = new TestServiceService(getResource(), clusterName, serviceName, getRequestFactory(), getRequestHandler());
    Response response = hostService.updateService(body, getHttpHeaders(), getUriInfo(), serviceName);

    verifyResults(response, 200);
  }

  @Test
  public void testUpdateService__ErrorState() {
    String clusterName = "clusterName";
    String serviceName = "serviceName";
    String body = "{body}";

    registerExpectations(Request.Type.PUT, body, 500, true);
    replayMocks();

    //test
    ServiceService hostService = new TestServiceService(getResource(), clusterName, serviceName, getRequestFactory(), getRequestHandler());
    Response response = hostService.updateService(body, getHttpHeaders(), getUriInfo(), serviceName);

    verifyResults(response, 500);
  }

  @Test
  public void testDeleteService() {
    String clusterName = "clusterName";
    String serviceName = "serviceName";

    registerExpectations(Request.Type.DELETE, null, 200, false);
    replayMocks();

    //test
    ServiceService hostService = new TestServiceService(getResource(), clusterName, serviceName, getRequestFactory(), getRequestHandler());
    Response response = hostService.deleteService(getHttpHeaders(), getUriInfo(), serviceName);

    verifyResults(response, 200);
  }

  @Test
  public void testDeleteService__ErrorState(){
    String clusterName = "clusterName";
    String serviceName = "serviceName";

    registerExpectations(Request.Type.DELETE, null, 500, true);
    replayMocks();

    //test
    ServiceService hostService = new TestServiceService(getResource(), clusterName, serviceName, getRequestFactory(), getRequestHandler());
    Response response = hostService.deleteService(getHttpHeaders(), getUriInfo(), serviceName);

    verifyResults(response, 500);
  }

  private class TestServiceService extends ServiceService {
    private RequestFactory m_requestFactory;
    private RequestHandler m_requestHandler;
    private ResourceInstance m_resourceDef;
    private String m_clusterId;
    private String m_serviceId;

    private TestServiceService(ResourceInstance resourceDef, String clusterId, String serviceId, RequestFactory requestFactory,
                               RequestHandler handler) {
      super(clusterId);
      m_resourceDef = resourceDef;
      m_clusterId = clusterId;
      m_serviceId = serviceId;
      m_requestFactory = requestFactory;
      m_requestHandler = handler;
    }

    @Override
    ResourceInstance createServiceResource(String clusterName, String serviceName) {
      assertEquals(m_clusterId, clusterName);
      assertEquals(m_serviceId, serviceName);
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
