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
import org.apache.ambari.server.api.resources.ResourceDefinition;
import org.apache.ambari.server.api.services.serializers.ResultSerializer;
import org.junit.Test;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * Unit tests for HostService.
 */
public class HostServiceTest {

  @Test
  public void testGetHost() {
    ResourceDefinition resourceDef = createStrictMock(ResourceDefinition.class);
    ResultSerializer resultSerializer = createStrictMock(ResultSerializer.class);
    Object serializedResult = new Object();
    RequestFactory requestFactory = createStrictMock(RequestFactory.class);
    ResponseFactory responseFactory = createStrictMock(ResponseFactory.class);
    Request request = createNiceMock(Request.class);
    RequestHandler requestHandler = createStrictMock(RequestHandler.class);
    Result result = createStrictMock(Result.class);
    Response response = createStrictMock(Response.class);

    HttpHeaders httpHeaders = createNiceMock(HttpHeaders.class);
    UriInfo uriInfo = createNiceMock(UriInfo.class);

    String clusterName = "clusterName";
    String hostName = "hostName";

    // expectations
    expect(requestFactory.createRequest(eq(httpHeaders), isNull(String.class), eq(uriInfo), eq(Request.Type.GET),
        eq(resourceDef))).andReturn(request);

    expect(requestHandler.handleRequest(request)).andReturn(result);
    expect(request.getResultSerializer()).andReturn(resultSerializer);
    expect(resultSerializer.serialize(result, uriInfo)).andReturn(serializedResult);
    expect(result.isSynchronous()).andReturn(true).atLeastOnce();
    expect(responseFactory.createResponse(Request.Type.GET, serializedResult, true)).andReturn(response);

    replay(resourceDef, resultSerializer, requestFactory, responseFactory, request, requestHandler,
        result, response, httpHeaders, uriInfo);

    //test
    HostService hostService = new TestHostService(resourceDef, clusterName, hostName, requestFactory, responseFactory, requestHandler);
    assertSame(response, hostService.getHost(httpHeaders, uriInfo, hostName));

    verify(resourceDef, resultSerializer, requestFactory, responseFactory, request, requestHandler,
        result, response, httpHeaders, uriInfo);
  }

  @Test
  public void testGetHosts() {
    ResourceDefinition resourceDef = createStrictMock(ResourceDefinition.class);
    ResultSerializer resultSerializer = createStrictMock(ResultSerializer.class);
    Object serializedResult = new Object();
    RequestFactory requestFactory = createStrictMock(RequestFactory.class);
    ResponseFactory responseFactory = createStrictMock(ResponseFactory.class);
    Request request = createNiceMock(Request.class);
    RequestHandler requestHandler = createStrictMock(RequestHandler.class);
    Result result = createStrictMock(Result.class);
    Response response = createStrictMock(Response.class);

    HttpHeaders httpHeaders = createNiceMock(HttpHeaders.class);
    UriInfo uriInfo = createNiceMock(UriInfo.class);

    String clusterName = "clusterName";

    // expectations
    expect(requestFactory.createRequest(eq(httpHeaders), isNull(String.class), eq(uriInfo), eq(Request.Type.GET),
        eq(resourceDef))).andReturn(request);

    expect(requestHandler.handleRequest(request)).andReturn(result);
    expect(request.getResultSerializer()).andReturn(resultSerializer);
    expect(resultSerializer.serialize(result, uriInfo)).andReturn(serializedResult);
    expect(result.isSynchronous()).andReturn(true).atLeastOnce();
    expect(responseFactory.createResponse(Request.Type.GET, serializedResult, true)).andReturn(response);

    replay(resourceDef, resultSerializer, requestFactory, responseFactory, request, requestHandler,
        result, response, httpHeaders, uriInfo);

    //test
    HostService hostService = new TestHostService(resourceDef, clusterName, null, requestFactory, responseFactory, requestHandler);
    assertSame(response, hostService.getHosts(httpHeaders, uriInfo));

    verify(resourceDef, resultSerializer, requestFactory, responseFactory, request, requestHandler,
        result, response, httpHeaders, uriInfo);
  }

  @Test
  public void testCreateHost() {
    ResourceDefinition resourceDef = createStrictMock(ResourceDefinition.class);
    ResultSerializer resultSerializer = createStrictMock(ResultSerializer.class);
    Object serializedResult = new Object();
    RequestFactory requestFactory = createStrictMock(RequestFactory.class);
    ResponseFactory responseFactory = createStrictMock(ResponseFactory.class);
    Request request = createNiceMock(Request.class);
    RequestHandler requestHandler = createStrictMock(RequestHandler.class);
    Result result = createStrictMock(Result.class);
    Response response = createStrictMock(Response.class);

    HttpHeaders httpHeaders = createNiceMock(HttpHeaders.class);
    UriInfo uriInfo = createNiceMock(UriInfo.class);

    String clusterName = "clusterName";
    String hostName = "hostName";

    // expectations
    expect(requestFactory.createRequest(eq(httpHeaders), eq("body"), eq(uriInfo), eq(Request.Type.POST),
        eq(resourceDef))).andReturn(request);

    expect(requestHandler.handleRequest(request)).andReturn(result);
    expect(request.getResultSerializer()).andReturn(resultSerializer);
    expect(resultSerializer.serialize(result, uriInfo)).andReturn(serializedResult);
    expect(result.isSynchronous()).andReturn(false).atLeastOnce();
    expect(responseFactory.createResponse(Request.Type.POST, serializedResult, false)).andReturn(response);

    replay(resourceDef, resultSerializer, requestFactory, responseFactory, request, requestHandler,
        result, response, httpHeaders, uriInfo);

    //test
    HostService hostService = new TestHostService(resourceDef, clusterName, hostName, requestFactory, responseFactory, requestHandler);
    assertSame(response, hostService.createHost("body", httpHeaders, uriInfo, hostName));

    verify(resourceDef, resultSerializer, requestFactory, responseFactory, request, requestHandler,
        result, response, httpHeaders, uriInfo);
  }

  @Test
  public void testUpdateHost() {
    ResourceDefinition resourceDef = createStrictMock(ResourceDefinition.class);
    ResultSerializer resultSerializer = createStrictMock(ResultSerializer.class);
    Object serializedResult = new Object();
    RequestFactory requestFactory = createStrictMock(RequestFactory.class);
    ResponseFactory responseFactory = createStrictMock(ResponseFactory.class);
    Request request = createNiceMock(Request.class);
    RequestHandler requestHandler = createStrictMock(RequestHandler.class);
    Result result = createStrictMock(Result.class);
    Response response = createStrictMock(Response.class);

    HttpHeaders httpHeaders = createNiceMock(HttpHeaders.class);
    UriInfo uriInfo = createNiceMock(UriInfo.class);

    String clusterName = "clusterName";
    String hostName = "hostName";

    // expectations
    expect(requestFactory.createRequest(eq(httpHeaders), eq("body"), eq(uriInfo), eq(Request.Type.PUT),
        eq(resourceDef))).andReturn(request);

    expect(requestHandler.handleRequest(request)).andReturn(result);
    expect(request.getResultSerializer()).andReturn(resultSerializer);
    expect(resultSerializer.serialize(result, uriInfo)).andReturn(serializedResult);
    expect(result.isSynchronous()).andReturn(false).atLeastOnce();
    expect(responseFactory.createResponse(Request.Type.PUT, serializedResult, false)).andReturn(response);

    replay(resourceDef, resultSerializer, requestFactory, responseFactory, request, requestHandler,
        result, response, httpHeaders, uriInfo);

    //test
    HostService hostService = new TestHostService(resourceDef, clusterName, hostName, requestFactory, responseFactory, requestHandler);
    assertSame(response, hostService.updateHost("body", httpHeaders, uriInfo, hostName));

    verify(resourceDef, resultSerializer, requestFactory, responseFactory, request, requestHandler,
        result, response, httpHeaders, uriInfo);
  }

  @Test
  public void testUpdateHosts() {
    ResourceDefinition resourceDef = createStrictMock(ResourceDefinition.class);
    ResultSerializer resultSerializer = createStrictMock(ResultSerializer.class);
    Object serializedResult = new Object();
    RequestFactory requestFactory = createStrictMock(RequestFactory.class);
    ResponseFactory responseFactory = createStrictMock(ResponseFactory.class);
    Request request = createNiceMock(Request.class);
    RequestHandler requestHandler = createStrictMock(RequestHandler.class);
    Result result = createStrictMock(Result.class);
    Response response = createStrictMock(Response.class);

    HttpHeaders httpHeaders = createNiceMock(HttpHeaders.class);
    UriInfo uriInfo = createNiceMock(UriInfo.class);

    String clusterName = "clusterName";

    // expectations
    expect(requestFactory.createRequest(eq(httpHeaders), eq("body"), eq(uriInfo), eq(Request.Type.PUT),
        eq(resourceDef))).andReturn(request);

    expect(requestHandler.handleRequest(request)).andReturn(result);
    expect(request.getResultSerializer()).andReturn(resultSerializer);
    expect(resultSerializer.serialize(result, uriInfo)).andReturn(serializedResult);
    expect(result.isSynchronous()).andReturn(false).atLeastOnce();
    expect(responseFactory.createResponse(Request.Type.PUT, serializedResult, false)).andReturn(response);

    replay(resourceDef, resultSerializer, requestFactory, responseFactory, request, requestHandler,
        result, response, httpHeaders, uriInfo);

    //test
    HostService hostService = new TestHostService(resourceDef, clusterName, null, requestFactory, responseFactory, requestHandler);
    assertSame(response, hostService.updateHosts("body", httpHeaders, uriInfo));

    verify(resourceDef, resultSerializer, requestFactory, responseFactory, request, requestHandler,
        result, response, httpHeaders, uriInfo);
  }

  @Test
  public void testDeleteHost() {
    ResourceDefinition resourceDef = createStrictMock(ResourceDefinition.class);
    ResultSerializer resultSerializer = createStrictMock(ResultSerializer.class);
    Object serializedResult = new Object();
    RequestFactory requestFactory = createStrictMock(RequestFactory.class);
    ResponseFactory responseFactory = createStrictMock(ResponseFactory.class);
    Request request = createNiceMock(Request.class);
    RequestHandler requestHandler = createStrictMock(RequestHandler.class);
    Result result = createStrictMock(Result.class);
    Response response = createStrictMock(Response.class);

    HttpHeaders httpHeaders = createNiceMock(HttpHeaders.class);
    UriInfo uriInfo = createNiceMock(UriInfo.class);

    String clusterName = "clusterName";
    String hostName = "hostName";

    // expectations
    expect(requestFactory.createRequest(eq(httpHeaders), isNull(String.class), eq(uriInfo), eq(Request.Type.DELETE),
        eq(resourceDef))).andReturn(request);

    expect(requestHandler.handleRequest(request)).andReturn(result);
    expect(request.getResultSerializer()).andReturn(resultSerializer);
    expect(resultSerializer.serialize(result, uriInfo)).andReturn(serializedResult);
    expect(result.isSynchronous()).andReturn(false).atLeastOnce();
    expect(responseFactory.createResponse(Request.Type.DELETE, serializedResult, false)).andReturn(response);

    replay(resourceDef, resultSerializer, requestFactory, responseFactory, request, requestHandler,
        result, response, httpHeaders, uriInfo);

    //test
    HostService hostService = new TestHostService(resourceDef, clusterName, hostName, requestFactory, responseFactory, requestHandler);
    assertSame(response, hostService.deleteHost(httpHeaders, uriInfo, hostName));

    verify(resourceDef, resultSerializer, requestFactory, responseFactory, request, requestHandler,
        result, response, httpHeaders, uriInfo);
  }


  private class TestHostService extends HostService {
    private RequestFactory m_requestFactory;
    private ResponseFactory m_responseFactory;
    private RequestHandler m_requestHandler;
    private ResourceDefinition m_resourceDef;
    private String m_clusterId;
    private String m_hostId;

    private TestHostService(ResourceDefinition resourceDef, String clusterId, String hostId, RequestFactory requestFactory,
                            ResponseFactory responseFactory, RequestHandler handler) {
      super(clusterId);
      m_resourceDef = resourceDef;
      m_clusterId = clusterId;
      m_hostId = hostId;
      m_requestFactory = requestFactory;
      m_responseFactory = responseFactory;
      m_requestHandler = handler;
    }

    @Override
    ResourceDefinition createResourceDefinition(String hostName, String clusterName) {
      assertEquals(m_clusterId, clusterName);
      assertEquals(m_hostId, hostName);
      return m_resourceDef;
    }

    @Override
    RequestFactory getRequestFactory() {
      return m_requestFactory;
    }

    @Override
    ResponseFactory getResponseFactory() {
      return m_responseFactory;
    }

    @Override
    RequestHandler getRequestHandler() {
      return m_requestHandler;
    }
  }
}


