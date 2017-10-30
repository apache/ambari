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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.parsers.RequestBodyParser;
import org.apache.ambari.server.api.services.registry.RegistryService;
import org.apache.ambari.server.api.services.serializers.ResultSerializer;

import org.apache.ambari.server.controller.spi.Resource;

/**
 * Unit tests for RegistryService
 */
public class RegistryServiceTest extends BaseServiceTest{
  @Override
  public List<BaseServiceTest.ServiceTestInvocation> getTestInvocations() throws Exception {
    List<BaseServiceTest.ServiceTestInvocation> listInvocations = new ArrayList<>();

    // getRegistries
    RegistryService service = new TestRegistryService("null");
    Method m = service.getClass().getMethod("getRegistries", String.class, HttpHeaders.class, UriInfo.class);
    Object[] args = new Object[]{null, getHttpHeaders(), getUriInfo()};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, service, m, args, null));

    // getRegistry
    service = new TestRegistryService("1");
    m = service.getClass().getMethod("getRegistry", String.class, HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[]{null, getHttpHeaders(), getUriInfo(), ""};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, service, m, args, null));

    //createRegistry
    service = new TestRegistryService(null);
    m = service.getClass().getMethod("createRegistries", String.class, HttpHeaders.class, UriInfo.class);
    args = new Object[]{"body", getHttpHeaders(), getUriInfo()};
    listInvocations.add(new ServiceTestInvocation(Request.Type.POST, service, m, args, "body"));

    return listInvocations;
  }
  private class TestRegistryService extends RegistryService {

    private String r_registryId;

    private TestRegistryService(String registryId) {
      super();
      r_registryId = registryId;
    }

    @Override
    protected ResourceInstance createResource(Resource.Type type, Map<Resource.Type, String> mapIds) {
      return getTestResource();
    }


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
