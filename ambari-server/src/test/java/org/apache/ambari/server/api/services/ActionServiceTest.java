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

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.parsers.RequestBodyParser;
import org.apache.ambari.server.api.services.serializers.ResultSerializer;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ActionServiceTest extends BaseServiceTest {

  public List<ServiceTestInvocation> getTestInvocations() throws Exception {
    List<ServiceTestInvocation> listInvocations = new ArrayList<ServiceTestInvocation>();

    //getActions
    ActionService componentService = new TestActionService("clusterName", "serviceName", null);
    Method m = componentService.getClass().getMethod("getActions", HttpHeaders.class, UriInfo.class);
    Object[] args = new Object[] {getHttpHeaders(), getUriInfo()};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, componentService, m, args, null));

    //createAction
    componentService = new TestActionService("clusterName", "serviceName", "actionName");
    m = componentService.getClass().getMethod("createAction", String.class, HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo(), "actionName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.POST, componentService, m, args, "body"));

    //createActions
    componentService = new TestActionService("clusterName", "serviceName", null);
    m = componentService.getClass().getMethod("createActions", String.class, HttpHeaders.class, UriInfo.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo()};
    listInvocations.add(new ServiceTestInvocation(Request.Type.POST, componentService, m, args, "body"));

    return listInvocations;
  }

  private class TestActionService extends ActionService {
    private String m_clusterId;
    private String m_serviceId;
    private String m_actionId;

    public TestActionService(String clusterName, String serviceName, String actionName) {

      super(clusterName, serviceName);
      m_clusterId = clusterName;
      m_serviceId = serviceName;
      m_actionId  = actionName;
    }

    @Override
    ResourceInstance createActionResource(String clusterName, String serviceName, String actionName) {
      assertEquals(m_clusterId, clusterName);
      assertEquals(m_serviceId, serviceName);
      assertEquals(m_actionId, actionName);
      return getTestResource();
    }

    @Override
    RequestFactory getRequestFactory() {
      return getTestRequestFactory();
    }

    @Override
    protected RequestBodyParser getBodyParser() {
      return getTestBodyParser();
    }

    @Override
    protected ResultSerializer getResultSerializer() {
      return getTestResultSerializer();
    }
  }
}
