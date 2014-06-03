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
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntityTest;

import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * ViewSubResourceService tests
 */
public class ViewSubResourceServiceTest extends BaseServiceTest {

  @Override
  public List<ServiceTestInvocation> getTestInvocations() throws Exception {
    List<ServiceTestInvocation> listInvocations = new ArrayList<ServiceTestInvocation>();

    ViewInstanceEntity viewInstanceEntity = ViewInstanceEntityTest.getViewInstanceEntity();

    Resource.Type type = new Resource.Type("subResource");

    // get resource
    ViewSubResourceService service = new TestViewSubResourceService(type, viewInstanceEntity);
    Method m = service.getClass().getMethod("getSubResource1", HttpHeaders.class, UriInfo.class, String.class);
    Object[] args = new Object[] {getHttpHeaders(), getUriInfo(), "id"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, service, m, args, null));

    // get resource
    service = new TestViewSubResourceService(type, viewInstanceEntity);
    m = service.getClass().getMethod("getSubResource2", HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {getHttpHeaders(), getUriInfo(), "id"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, service, m, args, null));

    // create resource
    service = new TestViewSubResourceService(type, viewInstanceEntity);
    m = service.getClass().getMethod("postSubResource", HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {getHttpHeaders(), getUriInfo(), "id"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.POST, service, m, args, null));

    // update resource
    service = new TestViewSubResourceService(type, viewInstanceEntity);
    m = service.getClass().getMethod("putSubResource", HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {getHttpHeaders(), getUriInfo(), "id"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.PUT, service, m, args, null));

    // delete resource
    service = new TestViewSubResourceService(type, viewInstanceEntity);
    m = service.getClass().getMethod("deleteSubResource", HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {getHttpHeaders(), getUriInfo(), "id"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.DELETE, service, m, args, null));

    return listInvocations;
  }

  private class TestViewSubResourceService extends ViewSubResourceService {

    /**
     * Construct a view sub-resource service.
     */
    public TestViewSubResourceService(Resource.Type type, ViewInstanceEntity viewInstanceDefinition) {
      super(type, viewInstanceDefinition);
    }

    public Response getSubResource1(@Context HttpHeaders headers, @Context UriInfo ui,
                             @PathParam("resourceId") String resourceId) {

      return handleRequest(headers, ui, resourceId);
    }

    public Response getSubResource2(@Context HttpHeaders headers, @Context UriInfo ui,
                                   @PathParam("resourceId") String resourceId) {

      return handleRequest(headers, ui, RequestType.GET, resourceId);
    }

    public Response postSubResource(@Context HttpHeaders headers, @Context UriInfo ui,
                                   @PathParam("resourceId") String resourceId) {

      return handleRequest(headers, ui, RequestType.POST, resourceId);
    }

    public Response putSubResource(@Context HttpHeaders headers, @Context UriInfo ui,
                                    @PathParam("resourceId") String resourceId) {

      return handleRequest(headers, ui, RequestType.PUT, resourceId);
    }

    public Response deleteSubResource(@Context HttpHeaders headers, @Context UriInfo ui,
                                    @PathParam("resourceId") String resourceId) {

      return handleRequest(headers, ui, RequestType.DELETE, resourceId);
    }

    @Override
    protected ResourceInstance createResource(String resourceId) {
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


