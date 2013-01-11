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
import org.apache.ambari.server.api.services.serializers.ResultSerializer;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

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
  private ResultPostProcessor resultProcessor = createStrictMock(ResultPostProcessor.class);
  private Request request = createNiceMock(Request.class);
  private RequestHandler requestHandler = createStrictMock(RequestHandler.class);
  private Result result = createNiceMock(Result.class);
  private ResultStatus status = createNiceMock(ResultStatus.class);
  private HttpHeaders httpHeaders = createNiceMock(HttpHeaders.class);
  private UriInfo uriInfo = createNiceMock(UriInfo.class);
  private ResultSerializer serializer = createStrictMock(ResultSerializer.class);
  private Object serializedResult = new Object();

  public ResourceInstance getResource() {
    return resourceInstance;
  }

  public RequestFactory getRequestFactory() {
    return requestFactory;
  }

  public ResultPostProcessor getResultProcessor() {
    return resultProcessor;
  }

  public Request getRequest() {
    return request;
  }

  public RequestHandler getRequestHandler() {
    return requestHandler;
  }

  public Result getResult() {
    return result;
  }

  public ResultStatus getStatus() {
    return status;
  }

  public HttpHeaders getHttpHeaders() {
    return httpHeaders;
  }

  public UriInfo getUriInfo() {
    return uriInfo;
  }

  public ResultSerializer getSerializer() {
    return serializer;
  }

  public Object getSerializedResult() {
    return serializedResult;
  }

  protected void registerExpectations(Request.Type type, String body, int statusCode, boolean isErrorState) {
    expect(requestFactory.createRequest(eq(httpHeaders), body == null ? isNull(String.class) : eq(body), eq(uriInfo), eq(type),
        eq(resourceInstance))).andReturn(request);

    expect(request.getRequestType()).andReturn(type).anyTimes();
    expect(request.getResultSerializer()).andReturn(serializer).anyTimes();
    expect(requestHandler.handleRequest(request)).andReturn(result);
    expect(result.getStatus()).andReturn(status).anyTimes();
    expect(status.isErrorState()).andReturn(isErrorState).anyTimes();
    expect(status.getStatusCode()).andReturn(statusCode);
    if (! isErrorState) {
      expect(request.getResultPostProcessor()).andReturn(resultProcessor);
      resultProcessor.process(result);
    }

    expect(serializer.serialize(result)).andReturn(serializedResult);

  }

  protected void replayMocks() {
    replay(resourceInstance, requestFactory, resultProcessor, request, status, requestHandler,
        result, httpHeaders, uriInfo, serializer);
  }


  protected void verifyResults(Response response, int statusCode) {
    assertEquals(getSerializedResult(), response.getEntity());
    assertEquals(statusCode, response.getStatus());

    verify(resourceInstance, requestFactory, resultProcessor, request, status, requestHandler,
        result, httpHeaders, uriInfo, serializer);
  }

}
