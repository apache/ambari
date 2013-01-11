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

import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.Response;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.handlers.RequestHandler;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.junit.Test;

public class ActionServiceTest extends BaseServiceTest {

  @Test
  public void testGetActions() {
    String clusterName = "c1";
    String serviceName = "HDFS";

    registerExpectations(Request.Type.GET, null, 200, false);
    replayMocks();

    //test
    ActionService actionService = new TestActionService(getResource(), clusterName, getRequestFactory(),
        getRequestHandler(), serviceName);
    Response response = actionService.getActions(getHttpHeaders(), getUriInfo());
    verifyResults(response, 200);
  }

  @Test
  public void testCreateActions() throws AmbariException {
    String clusterName = "c1";
    String serviceName = "HDFS";
    String body = "body";

    registerExpectations(Request.Type.POST, body, 201, false);
    replayMocks();

    //test
    ActionService actionService = new TestActionService(getResource(), clusterName, getRequestFactory(),
        getRequestHandler(), serviceName);
    Response response = actionService.createActions(body, getHttpHeaders(), getUriInfo());
    verifyResults(response, 201);
  }

  private class TestActionService extends ActionService {
    private ResourceInstance m_resourceDef;
    private String m_clusterId;
    private String m_serviceId;
    private RequestFactory m_requestFactory;
    private RequestHandler m_requestHandler;

    public TestActionService(ResourceInstance resourceDef,
                             String clusterName, RequestFactory requestFactory,
                             RequestHandler handler,
                             String serviceName) {

      super(clusterName, serviceName);
      m_resourceDef = resourceDef;
      m_clusterId = clusterName;
      m_serviceId = serviceName;
      m_requestFactory = requestFactory;
      m_requestHandler = handler;
    }

    @Override
    ResourceInstance createActionResource(String clusterName, String serviceName, String actionName) {
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
