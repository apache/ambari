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
 * Unit tests for ComponentService.
 */
public class ComponentServiceTest extends BaseServiceTest {

  @Test
  public void testGetComponent()  {
    String clusterName = "clusterName";
    String serviceName = "serviceName";
    String componentName = "componentName";

    registerExpectations(Request.Type.GET, null, 200, false);
    replayMocks();

    //test
    ComponentService componentService = new TestComponentService(getResource(), clusterName, serviceName, componentName,
        getRequestFactory(), getRequestHandler());

    Response response = componentService.getComponent(getHttpHeaders(), getUriInfo(), componentName);
    verifyResults(response, 200);
  }

  @Test
  public void testGetComponent__ErrorState()  {
    String clusterName = "clusterName";
    String serviceName = "serviceName";
    String componentName = "componentName";

    registerExpectations(Request.Type.GET, null, 404, true);
    replayMocks();

    //test
    ComponentService componentService = new TestComponentService(getResource(), clusterName, serviceName, componentName,
        getRequestFactory(), getRequestHandler());

    Response response = componentService.getComponent(getHttpHeaders(), getUriInfo(), componentName);
    verifyResults(response, 404);
  }

  @Test
  public void testGetComponents() {
    String clusterName = "clusterName";
    String serviceName = "serviceName";

    registerExpectations(Request.Type.GET, null, 200, false);
    replayMocks();

    //test
    ComponentService componentService = new TestComponentService(getResource(), clusterName, serviceName, null,
        getRequestFactory(), getRequestHandler());

    Response response = componentService.getComponents(getHttpHeaders(), getUriInfo());
    verifyResults(response, 200);
  }

  @Test
  public void testGetComponents__ErrorState() {
    String clusterName = "clusterName";
    String serviceName = "serviceName";

    registerExpectations(Request.Type.GET, null, 500, true);
    replayMocks();

    //test
    ComponentService componentService = new TestComponentService(getResource(), clusterName, serviceName, null,
        getRequestFactory(), getRequestHandler());

    Response response = componentService.getComponents(getHttpHeaders(), getUriInfo());
    verifyResults(response, 500);
  }

  @Test
  public void testCreateComponent() {
    String clusterName = "clusterName";
    String serviceName = "serviceName";
    String componentName = "componentName";
    String body = "{body}";

    registerExpectations(Request.Type.POST, body, 201, false);
    replayMocks();

    //test
    ComponentService componentService = new TestComponentService(getResource(), clusterName, serviceName, componentName,
        getRequestFactory(), getRequestHandler());

    Response response = componentService.createComponent(body, getHttpHeaders(), getUriInfo(), componentName);
    verifyResults(response, 201);
  }

  @Test
  public void testCreateComponent__ErrorState() {
    String clusterName = "clusterName";
    String serviceName = "serviceName";
    String componentName = "componentName";
    String body = "{body}";

    registerExpectations(Request.Type.POST, body, 500, true);
    replayMocks();

    //test
    ComponentService componentService = new TestComponentService(getResource(), clusterName, serviceName, componentName,
        getRequestFactory(), getRequestHandler());

    Response response = componentService.createComponent(body, getHttpHeaders(), getUriInfo(), componentName);
    verifyResults(response, 500);
  }

  @Test
  public void testUpdateComponent() {
    String clusterName = "clusterName";
    String serviceName = "serviceName";
    String componentName = "componentName";
    String body = "{body}";

    registerExpectations(Request.Type.PUT, body, 200, false);
    replayMocks();

    //test
    ComponentService componentService = new TestComponentService(getResource(), clusterName, serviceName, componentName,
        getRequestFactory(), getRequestHandler());

    Response response = componentService.updateComponent(body, getHttpHeaders(), getUriInfo(), componentName);
    verifyResults(response, 200);
  }

  @Test
  public void testUpdateComponent__ErrorState() {
    String clusterName = "clusterName";
    String serviceName = "serviceName";
    String componentName = "componentName";
    String body = "{body}";

    registerExpectations(Request.Type.PUT, body, 500, true);
    replayMocks();

    //test
    ComponentService componentService = new TestComponentService(getResource(), clusterName, serviceName, componentName,
        getRequestFactory(), getRequestHandler());

    Response response = componentService.updateComponent(body, getHttpHeaders(), getUriInfo(), componentName);
    verifyResults(response, 500);
  }

  @Test
  public void testUpdateComponents() {
    String clusterName = "clusterName";
    String serviceName = "serviceName";
    String body = "{body}";

    registerExpectations(Request.Type.PUT, body, 200, false);
    replayMocks();

    //test
    ComponentService componentService = new TestComponentService(getResource(), clusterName, serviceName, null,
        getRequestFactory(), getRequestHandler());

    Response response = componentService.updateComponents(body, getHttpHeaders(), getUriInfo());
    verifyResults(response, 200);
  }

  @Test
  public void testUpdateComponents__ErrorState() {
    String clusterName = "clusterName";
    String serviceName = "serviceName";
    String body = "{body}";

    registerExpectations(Request.Type.PUT, body, 500, true);
    replayMocks();

    //test
    ComponentService componentService = new TestComponentService(getResource(), clusterName, serviceName, null,
        getRequestFactory(), getRequestHandler());

    Response response = componentService.updateComponents(body, getHttpHeaders(), getUriInfo());
    verifyResults(response, 500);
  }

  @Test
  public void testDeleteComponent() {
    String clusterName = "clusterName";
    String serviceName = "serviceName";

    registerExpectations(Request.Type.DELETE, null, 200, false);
    replayMocks();

    //test
    ComponentService componentService = new TestComponentService(getResource(), clusterName, serviceName, null,
        getRequestFactory(), getRequestHandler());

    Response response = componentService.deleteComponent(getHttpHeaders(), getUriInfo(), null);
    verifyResults(response, 200);
  }

  @Test
  public void testDeleteComponent__ErrorState() {
    String clusterName = "clusterName";
    String serviceName = "serviceName";

    registerExpectations(Request.Type.DELETE, null, 500, true);
    replayMocks();

    //test
    ComponentService componentService = new TestComponentService(getResource(), clusterName, serviceName, null,
        getRequestFactory(), getRequestHandler());

    Response response = componentService.deleteComponent(getHttpHeaders(), getUriInfo(), null);
    verifyResults(response, 500);
  }

  private class TestComponentService extends ComponentService {
    private RequestFactory m_requestFactory;
    private RequestHandler m_requestHandler;
    private ResourceInstance m_resource;
    private String m_clusterId;
    private String m_serviceId;
    private String m_componentId;

    private TestComponentService(ResourceInstance resourceDef, String clusterId, String serviceId, String componentId,
                                 RequestFactory requestFactory, RequestHandler handler) {
      super(clusterId, serviceId);
      m_requestFactory = requestFactory;
      m_requestHandler = handler;
      m_resource = resourceDef;
      m_clusterId = clusterId;
      m_serviceId = serviceId;
      m_componentId = componentId;
    }

    @Override
    ResourceInstance createComponentResource(String clusterName, String serviceName, String componentName) {
      assertEquals(m_clusterId, clusterName);
      assertEquals(m_serviceId, serviceName);
      assertEquals(m_componentId, componentName);
      return m_resource;
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
