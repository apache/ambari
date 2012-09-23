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
package org.apache.ambari.api.handlers;

import org.apache.ambari.api.services.Request;
import org.apache.ambari.api.services.Result;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertSame;

/**
 *
 */
public class DelegatingRequestHandlerTest {

  @Test
  public void testHandleRequest_GET() {
    Request request = createStrictMock(Request.class);
    RequestHandlerFactory factory = createStrictMock(RequestHandlerFactory.class);
    RequestHandler readRequestHandler = createStrictMock(RequestHandler.class);
    Result result = createStrictMock(Result.class);

    // expectations
    expect(request.getRequestType()).andReturn(Request.RequestType.GET);
    expect(factory.getRequestHandler(Request.RequestType.GET)).andReturn(readRequestHandler);
    expect(readRequestHandler.handleRequest(request)).andReturn(result);

    replay(request, factory, readRequestHandler, result);

    RequestHandler delegatingRequestHandler = new TestDelegatingRequestHandler(factory);

    assertSame(result, delegatingRequestHandler.handleRequest(request));
    verify(request, factory, readRequestHandler, result);
  }

  @Test
  public void testHandleRequest_PUT() {
    Request request = createStrictMock(Request.class);
    RequestHandlerFactory factory = createStrictMock(RequestHandlerFactory.class);
    RequestHandler requestHandler = createStrictMock(RequestHandler.class);
    Result result = createStrictMock(Result.class);

    // expectations
    expect(request.getRequestType()).andReturn(Request.RequestType.PUT);
    expect(factory.getRequestHandler(Request.RequestType.PUT)).andReturn(requestHandler);
    expect(requestHandler.handleRequest(request)).andReturn(result);

    replay(request, factory, requestHandler, result);

    RequestHandler delegatingRequestHandler = new TestDelegatingRequestHandler(factory);

    assertSame(result, delegatingRequestHandler.handleRequest(request));
    verify(request, factory, requestHandler, result);
  }

  @Test
  public void testHandleRequest_POST() {
    Request request = createStrictMock(Request.class);
    RequestHandlerFactory factory = createStrictMock(RequestHandlerFactory.class);
    RequestHandler requestHandler = createStrictMock(RequestHandler.class);
    Result result = createStrictMock(Result.class);

    // expectations
    expect(request.getRequestType()).andReturn(Request.RequestType.POST);
    expect(factory.getRequestHandler(Request.RequestType.POST)).andReturn(requestHandler);
    expect(requestHandler.handleRequest(request)).andReturn(result);

    replay(request, factory, requestHandler, result);

    RequestHandler delegatingRequestHandler = new TestDelegatingRequestHandler(factory);

    assertSame(result, delegatingRequestHandler.handleRequest(request));
    verify(request, factory, requestHandler, result);
  }

  @Test
  public void testHandleRequest_DELETE() {
    Request request = createStrictMock(Request.class);
    RequestHandlerFactory factory = createStrictMock(RequestHandlerFactory.class);
    RequestHandler requestHandler = createStrictMock(RequestHandler.class);
    Result result = createStrictMock(Result.class);

    // expectations
    expect(request.getRequestType()).andReturn(Request.RequestType.DELETE);
    expect(factory.getRequestHandler(Request.RequestType.DELETE)).andReturn(requestHandler);
    expect(requestHandler.handleRequest(request)).andReturn(result);

    replay(request, factory, requestHandler, result);

    RequestHandler delegatingRequestHandler = new TestDelegatingRequestHandler(factory);

    assertSame(result, delegatingRequestHandler.handleRequest(request));
    verify(request, factory, requestHandler, result);
  }

  private class TestDelegatingRequestHandler extends DelegatingRequestHandler {
    private RequestHandlerFactory m_factory;

    private TestDelegatingRequestHandler(RequestHandlerFactory factory) {
      m_factory = factory;
    }

    @Override
    public RequestHandlerFactory getRequestHandlerFactory() {
      return m_factory;
    }
  }
}
