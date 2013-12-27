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

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.parsers.RequestBodyParser;
import org.apache.ambari.server.api.services.serializers.ResultSerializer;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for ServiceService.
 */
public class ActionServiceTest extends BaseServiceTest {

  public List<ServiceTestInvocation> getTestInvocations() throws Exception {
    List<ServiceTestInvocation> listInvocations = new ArrayList<ServiceTestInvocation>();

    //getActionDefinition
    ActionService service = new TestActionDefinitionService("actionName");
    Method m = service.getClass().getMethod("getActionDefinition", HttpHeaders.class, UriInfo.class, String.class);
    Object[] args = new Object[] {getHttpHeaders(), getUriInfo(), "actionName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, service, m, args, null));

    //getActionDefinitions
    service = new TestActionDefinitionService(null);
    m = service.getClass().getMethod("getActionDefinitions", HttpHeaders.class, UriInfo.class);
    args = new Object[] {getHttpHeaders(), getUriInfo()};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, service, m, args, null));

    //createActionDefinition
    service = new TestActionDefinitionService("actionName");
    m = service.getClass().getMethod("createActionDefinition", String.class, HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo(), "actionName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.POST, service, m, args, "body"));

    //updateActionDefinition
    service = new TestActionDefinitionService("actionName");
    m = service.getClass().getMethod("updateActionDefinition", String.class, HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo(), "actionName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.PUT, service, m, args, "body"));

    //deleteActionDefinition
    service = new TestActionDefinitionService("actionName");
    m = service.getClass().getMethod("deleteActionDefinition", HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {getHttpHeaders(), getUriInfo(), "actionName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.DELETE, service, m, args, null));

    return listInvocations;
  }

  private class TestActionDefinitionService extends ActionService {
    private String m_actionName;

    private TestActionDefinitionService(String actionName) {
      m_actionName = actionName;
    }

    @Override
    ResourceInstance createActionDefinitionResource(String actionName) {
      assertEquals(m_actionName, actionName);
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
