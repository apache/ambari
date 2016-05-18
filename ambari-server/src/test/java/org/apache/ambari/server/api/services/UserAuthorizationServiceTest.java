/*
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
import org.apache.ambari.server.controller.spi.Resource;
import org.junit.Test;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;


/**
 * Unit tests for UserAuthorizationService.
 */
public class UserAuthorizationServiceTest extends BaseServiceTest {

  public List<ServiceTestInvocation> getTestInvocations() throws Exception {
    List<ServiceTestInvocation> listInvocations = new ArrayList<ServiceTestInvocation>();

    //getAuthorization
    UserAuthorizationService service = new TestUserAuthorizationService("id");
    Method m = service.getClass().getMethod("getAuthorization", HttpHeaders.class, UriInfo.class, String.class);
    Object[] args = new Object[] {getHttpHeaders(), getUriInfo(), "id"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, service, m, args, null));

    //getAuthorizations
    service = new TestUserAuthorizationService(null);
    m = service.getClass().getMethod("getAuthorizations", HttpHeaders.class, UriInfo.class);
    args = new Object[] {getHttpHeaders(), getUriInfo()};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, service, m, args, null));

    return listInvocations;
  }

  @Test
  public void testCreateAuthorizationResourceWithUppercaseUsername() {
    // GIVEN
    UserAuthorizationService userAuthorizationService= new UserAuthorizationService("Jdoe");
    // WHEN
    ResourceInstance result = userAuthorizationService.createAuthorizationResource("id");
    // THEN
    assertEquals("jdoe", result.getKeyValueMap().get(Resource.Type.User));
  }


  private class TestUserAuthorizationService extends UserAuthorizationService {
    private String id;

    private TestUserAuthorizationService(String id) {
      super("jdoe");
      this.id = id;
    }

    @Override
    protected ResourceInstance createAuthorizationResource(String id) {
      assertEquals(this.id, id);
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
