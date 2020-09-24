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

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.junit.Test;

public class AmbariViewErrorHandlerProxyTest {

  final AmbariErrorHandler ambariErrorHandler = createNiceMock(AmbariErrorHandler.class);
  final ErrorHandler errorHandler = createNiceMock(ErrorHandler.class);

  final HttpServletRequest httpServletRequest = createNiceMock(HttpServletRequest.class);
  final HttpServletResponse httpServletResponse = createNiceMock(HttpServletResponse.class);
  final Request request = createNiceMock(Request.class);

  final String target = "test/target/uri";

  @Test
  public void testHandleInternalServerError() throws Throwable {
    //given
    Throwable th = createNiceMock(Throwable.class);
    expect(httpServletRequest.getAttribute(RequestDispatcher.ERROR_EXCEPTION)).andReturn(th).anyTimes();
    expect(httpServletResponse.getStatus()).andReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR).anyTimes();

    ambariErrorHandler.handle(target, request, httpServletRequest, httpServletResponse);
    expectLastCall();

    replay(ambariErrorHandler, errorHandler, httpServletRequest, httpServletResponse, th);

    //when
    AmbariViewErrorHandlerProxy proxy = new AmbariViewErrorHandlerProxy(errorHandler, ambariErrorHandler);
    proxy.handle(target, request, httpServletRequest, httpServletResponse);

    //then
    verify(ambariErrorHandler, errorHandler, httpServletRequest, httpServletResponse, th);
  }

  @Test
  public void testHandleGeneralError() throws Throwable {
    //given
    Throwable th = createNiceMock(Throwable.class);
    expect(httpServletRequest.getAttribute(RequestDispatcher.ERROR_EXCEPTION)).andReturn(th).anyTimes();
    expect(httpServletResponse.getStatus()).andReturn(HttpStatus.SC_BAD_REQUEST).anyTimes();

    errorHandler.handle(target, request, httpServletRequest, httpServletResponse);
    expectLastCall();

    replay(ambariErrorHandler, errorHandler, httpServletRequest, httpServletResponse, th);

    //when
    AmbariViewErrorHandlerProxy proxy = new AmbariViewErrorHandlerProxy(errorHandler, ambariErrorHandler);
    proxy.handle(target, request, httpServletRequest, httpServletResponse);

    //then
    verify(ambariErrorHandler, errorHandler, httpServletRequest, httpServletResponse, th);
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