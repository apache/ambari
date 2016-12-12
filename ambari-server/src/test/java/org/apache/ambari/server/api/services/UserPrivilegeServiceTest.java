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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.parsers.RequestBodyParser;
import org.apache.ambari.server.api.services.serializers.ResultSerializer;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Resource.Type;
import org.easymock.EasyMock;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Unit tests for GroupService.
 */
public class UserPrivilegeServiceTest extends BaseServiceTest {

  public List<ServiceTestInvocation> getTestInvocations() throws Exception {
    List<ServiceTestInvocation> listInvocations = new ArrayList<ServiceTestInvocation>();

    UserPrivilegeService userPrivilegeService;
    Method m;
    Object[] args;

    //getPrivilege
    userPrivilegeService = new TestUserPrivilegeService();
    m = userPrivilegeService.getClass().getMethod("getPrivilege", HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {getHttpHeaders(), getUriInfo(), "id"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, userPrivilegeService, m, args, null));

    //getPrivileges
    userPrivilegeService = new TestUserPrivilegeService();
    m = userPrivilegeService.getClass().getMethod("getPrivileges", HttpHeaders.class, UriInfo.class);
    args = new Object[] {getHttpHeaders(), getUriInfo()};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, userPrivilegeService, m, args, null));

    return listInvocations;
  }

  @Test
  public void testDisabledMethods() {
    final HttpHeaders headers = EasyMock.createNiceMock(HttpHeaders.class);
    final UriInfo uriInfo = EasyMock.createNiceMock(UriInfo.class);
    final UserPrivilegeService service = new TestUserPrivilegeService();

    final List<Response> disabledMethods = new ArrayList<Response>();
    disabledMethods.add(service.createPrivilege("test", headers, uriInfo));
    disabledMethods.add(service.updatePrivilege("test", headers, uriInfo, "test"));
    disabledMethods.add(service.updatePrivileges("test", headers, uriInfo));
    disabledMethods.add(service.deletePrivilege(headers, uriInfo, "test"));
    disabledMethods.add(service.deletePrivileges("test", headers, uriInfo));

    for (Response response: disabledMethods) {
      Assert.assertEquals(HttpServletResponse.SC_NOT_IMPLEMENTED, response.getStatus());
    }
  }

  @Test
  public void testCreatePrivilegeResourcesWithUppercaseUsername() {
    // GIVEN
    UserPrivilegeService userPrivilegeService = new UserPrivilegeService("User");
    // WHEN
    ResourceInstance result = userPrivilegeService.createPrivilegeResource("test");
    // THEN
    assertEquals( "user", result.getKeyValueMap().get(Resource.Type.User));
  }

  private class TestUserPrivilegeService extends UserPrivilegeService {

    public TestUserPrivilegeService() {
      super("user");
    }

    @Override
    protected ResourceInstance createResource(Type type, Map<Type, String> mapIds) {
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