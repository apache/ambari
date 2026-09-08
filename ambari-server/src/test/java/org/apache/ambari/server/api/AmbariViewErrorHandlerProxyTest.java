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

package org.apache.ambari.server.api;

import static junit.framework.Assert.assertTrue;
import static org.easymock.EasyMock.captureBoolean;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import jakarta.servlet.RequestDispatcher;

import org.apache.http.HttpStatus;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.eclipse.jetty.ee10.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.junit.Test;

public class AmbariViewErrorHandlerProxyTest {

  final AmbariErrorHandler ambariErrorHandler = createNiceMock(AmbariErrorHandler.class);
  final ErrorPageErrorHandler errorHandler = createNiceMock(ErrorPageErrorHandler.class);

  final Request request = createNiceMock(Request.class);
  final Response response = createNiceMock(Response.class);
  final Callback callback = createNiceMock(Callback.class);

  @Test
  public void testHandleInternalServerError() throws Throwable {
    //given
    Throwable th = createNiceMock(Throwable.class);
    expect(request.getAttribute(RequestDispatcher.ERROR_EXCEPTION)).andReturn(th).anyTimes();
    expect(response.getStatus()).andReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR).anyTimes();

    expect(ambariErrorHandler.handle(request, response, callback)).andReturn(true);

    replay(ambariErrorHandler, errorHandler, request, response, callback, th);

    //when
    AmbariViewErrorHandlerProxy proxy = new AmbariViewErrorHandlerProxy(errorHandler, ambariErrorHandler);
    proxy.handle(request, response, callback);

    //then
    verify(ambariErrorHandler, errorHandler, request, response, callback, th);
  }

  @Test
  public void testDelegatesCustomErrorPageHandlerForNonInternalError() throws Throwable {
    //given
    Throwable th = createNiceMock(Throwable.class);
    expect(request.getAttribute(RequestDispatcher.ERROR_EXCEPTION)).andReturn(th).anyTimes();
    expect(response.getStatus()).andReturn(HttpStatus.SC_BAD_REQUEST).anyTimes();

    expect(errorHandler.handle(request, response, callback)).andReturn(true);

    replay(ambariErrorHandler, errorHandler, request, response, callback, th);

    //when
    AmbariViewErrorHandlerProxy proxy = new AmbariViewErrorHandlerProxy(errorHandler, ambariErrorHandler);
    proxy.handle(request, response, callback);

    //then
    verify(ambariErrorHandler, errorHandler, request, response, callback, th);
  }

  @Test
  public void testShowStacks() {

    //given
    Capture<Boolean> captureShowStacksErrorHandler = EasyMock.newCapture();
    errorHandler.setShowStacks(captureBoolean(captureShowStacksErrorHandler));
    expectLastCall();

    Capture<Boolean> captureShowStacksAmbariErrorHandler = EasyMock.newCapture();
    ambariErrorHandler.setShowStacks(captureBoolean(captureShowStacksAmbariErrorHandler));
    expectLastCall();

    replay(errorHandler, ambariErrorHandler);


    //when
    AmbariViewErrorHandlerProxy proxy = new AmbariViewErrorHandlerProxy(errorHandler, ambariErrorHandler);
    proxy.setShowStacks(true);

    //then
    assertTrue(captureShowStacksErrorHandler.getValue());
    assertTrue(captureShowStacksAmbariErrorHandler.getValue());

    verify(errorHandler, ambariErrorHandler);

  }
}
