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
import org.apache.ambari.server.api.services.parsers.BodyParseException;
import org.apache.ambari.server.api.services.parsers.RequestBodyParser;
import org.apache.ambari.server.api.services.serializers.ResultSerializer;
import org.easymock.Capture;
import org.junit.Test;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.easymock.EasyMock.*;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;

import static org.junit.Assert.assertEquals;

/**
 * Base class for service unit tests.
 */
public abstract class BaseServiceTest {

  private ResourceInstance resourceInstance = createNiceMock(ResourceInstance.class);
  private RequestFactory requestFactory = createStrictMock(RequestFactory.class);
  private Request request = createNiceMock(Request.class);
  private HttpHeaders httpHeaders = createNiceMock(HttpHeaders.class);
  private UriInfo uriInfo = createNiceMock(UriInfo.class);
  private Result result = createMock(Result.class);
  private RequestBody requestBody = createNiceMock(RequestBody.class);
  private RequestBodyParser bodyParser = createStrictMock(RequestBodyParser.class);
  private ResultStatus status = createNiceMock(ResultStatus.class);
  private ResultSerializer serializer = createStrictMock(ResultSerializer.class);
  private Object serializedResult = new Object();

  public ResourceInstance getTestResource() {
    return resourceInstance;
  }

  public RequestFactory getTestRequestFactory() {
    return requestFactory;
  }

  public Request getRequest() {
    return request;
  }


  public HttpHeaders getHttpHeaders() {
    return httpHeaders;
  }

  public UriInfo getUriInfo() {
    return uriInfo;
  }

  public RequestBodyParser getTestBodyParser() {
    return bodyParser;
  }

  public ResultSerializer getTestResultSerializer() {
    return serializer;
  }

  @Test
  public void testService() throws Exception {
    List<ServiceTestInvocation> listTestInvocations = getTestInvocations();
    for (ServiceTestInvocation testInvocation : listTestInvocations) {
      testMethod(testInvocation);
      testMethod_bodyParseException(testInvocation);
      testMethod_resultInErrorState(testInvocation);
    }
  }

  private void testMethod(ServiceTestInvocation testMethod) throws InvocationTargetException, IllegalAccessException {
    try {
      expect(bodyParser.parse(testMethod.getBody())).andReturn(Collections.singleton(requestBody));
    } catch (BodyParseException e) {
      // needed for compiler
    }

    expect(requestFactory.createRequest(httpHeaders, requestBody, uriInfo, testMethod.getRequestType(), resourceInstance)).andReturn(request);
    expect(request.process()).andReturn(result);
    expect(result.getStatus()).andReturn(status).atLeastOnce();
    expect(status.getStatusCode()).andReturn(testMethod.getStatusCode()).atLeastOnce();
    expect(serializer.serialize(result)).andReturn(serializedResult);

    replayMocks();

    Response r = testMethod.invoke();

    assertEquals(serializedResult, r.getEntity());
    assertEquals(testMethod.getStatusCode(), r.getStatus());
    verifyAndResetMocks();
  }

  private void testMethod_bodyParseException(ServiceTestInvocation testMethod) throws Exception {
    Capture<Result> resultCapture = new Capture<Result>();
    BodyParseException e = new BodyParseException("TEST MSG");
    expect(bodyParser.parse(testMethod.getBody())).andThrow(e);
    expect(serializer.serialize(capture(resultCapture))).andReturn(serializedResult);

    replayMocks();

    Response r = testMethod.invoke();

    assertEquals(serializedResult, r.getEntity());
    assertEquals(400, r.getStatus());
    //todo: assert resource state
    verifyAndResetMocks();
  }

  private void testMethod_resultInErrorState(ServiceTestInvocation testMethod) throws Exception {
    try {
      expect(bodyParser.parse(testMethod.getBody())).andReturn(Collections.singleton(requestBody));
    } catch (BodyParseException e) {
      // needed for compiler
    }
    expect(requestFactory.createRequest(httpHeaders, requestBody, uriInfo, testMethod.getRequestType(), resourceInstance)).andReturn(request);
    expect(request.process()).andReturn(result);
    expect(result.getStatus()).andReturn(status).atLeastOnce();
    expect(status.getStatusCode()).andReturn(400).atLeastOnce();
    expect(serializer.serialize(result)).andReturn(serializedResult);

    replayMocks();

    Response r = testMethod.invoke();

    assertEquals(serializedResult, r.getEntity());
    assertEquals(400, r.getStatus());
    verifyAndResetMocks();
  }

  private void replayMocks() {
    replay(resourceInstance, requestFactory, request, result, requestBody, bodyParser, status, serializer);
  }

  private void verifyAndResetMocks() {
    verify(resourceInstance, requestFactory, request, result, requestBody, bodyParser, status, serializer);
    reset(resourceInstance, requestFactory, request, result, requestBody, bodyParser, status, serializer);
  }

  public static class ServiceTestInvocation {
    private Request.Type m_type;
    private BaseService m_instance;
    private Method m_method;
    private Object[] m_args;
    private String m_body;

    private static final Map<Request.Type, Integer> mapStatusCodes = new HashMap<Request.Type, Integer>();

    static {
      mapStatusCodes.put(Request.Type.GET, 200);
      mapStatusCodes.put(Request.Type.POST, 201);
      mapStatusCodes.put(Request.Type.PUT, 200);
      mapStatusCodes.put(Request.Type.DELETE, 200);
      mapStatusCodes.put(Request.Type.QUERY_POST, 201);
    }

    public ServiceTestInvocation(Request.Type requestType, BaseService instance, Method method, Object[] args, String body) {
      m_type = requestType;
      m_instance = instance;
      m_method = method;
      m_args = args;
      m_body = body;
    }

    public int getStatusCode() {
      return mapStatusCodes.get(m_type);
    }

    public Request.Type getRequestType() {
      return m_type;
    }

    public String getBody() {
      return m_body;
    }

    public Response invoke() throws InvocationTargetException, IllegalAccessException {
      return (Response) m_method.invoke(m_instance, m_args);
    }
  }

  public abstract List<ServiceTestInvocation> getTestInvocations() throws Exception;
}
