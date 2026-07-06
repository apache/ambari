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

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.mockStatic;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.UUID;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ambari.server.security.authentication.jwt.JwtAuthenticationPropertiesProvider;
import org.apache.http.HttpStatus;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.HttpConnection;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AmbariErrorHandler.class, LoggerFactory.class, HttpConnection.class, UUID.class})
public class AmbariErrorHandlerTest extends EasyMockSupport {
  private Gson gson = new Gson();

  private Logger logger = createNiceMock(Logger.class);

  private HttpConnection httpConnection = createNiceMock(HttpConnection.class);
  private HttpChannel httpChannel = createNiceMock(HttpChannel.class);

  private Response response = createNiceMock(Response.class);
  private Request request = createNiceMock(Request.class);

  private HttpServletResponse httpServletResponse = createNiceMock(HttpServletResponse.class);
  private HttpServletRequest httpServletRequest = createNiceMock(HttpServletRequest.class);

  private JwtAuthenticationPropertiesProvider propertiesProvider = createNiceMock(JwtAuthenticationPropertiesProvider.class);

  final String target = "target";

  @Test
  public void testHandleInternalServerError() throws IOException {
    //given
    final UUID requestId = UUID.fromString("4db659b2-7902-477b-b8e6-c35261d3334a");

    mockStatic(HttpConnection.class);
    mockStatic(UUID.class);
    mockStatic(LoggerFactory.class);
    expect(HttpConnection.getCurrentConnection()).andReturn(httpConnection);
    expect(UUID.randomUUID()).andReturn(requestId);
    expect(LoggerFactory.getLogger(AmbariErrorHandler.class)).andReturn(logger);

    Throwable th = createNiceMock(Throwable.class);

    Capture<String> captureLogMessage = EasyMock.newCapture();
    logger.error(capture(captureLogMessage), eq(th));
    expectLastCall().anyTimes();

    expect(httpConnection.getHttpChannel()).andReturn(httpChannel);
    expect(httpChannel.getRequest()).andReturn(request);
    expect(httpChannel.getResponse()).andReturn(response).times(2);
    expect(response.getStatus()).andReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);

    final String requestUri = "/path/to/target";
    expect(httpServletRequest.getRequestURI()).andReturn(requestUri);
    expect(httpServletRequest.getAttribute(RequestDispatcher.ERROR_EXCEPTION)).andReturn(th);

    final StringWriter writer = new StringWriter();
    expect(httpServletResponse.getWriter()).andReturn(new PrintWriter(writer));

    expect(propertiesProvider.get()).andReturn(null).anyTimes();

    replayAll();

    final String expectedResponse = "{\"status\":500,\"message\":\"Internal server error, please refer the exception by " + requestId + " in the server log file\"}";
    final String expectedErrorMessage = "Internal server error, please refer the exception by " + requestId + " in the server log file, requestURI: " + requestUri;

    AmbariErrorHandler ambariErrorHandler = new AmbariErrorHandler(gson, propertiesProvider);
    ambariErrorHandler.setShowStacks(false);

    //when
    ambariErrorHandler.handle(target, request, httpServletRequest, httpServletResponse);

    //then
    assertEquals(expectedResponse, writer.toString());
    assertEquals(expectedErrorMessage, captureLogMessage.getValue());
    verifyAll();
  }

  @Test
  public void testHandleGeneralError() throws Exception {

    //given
    mockStatic(HttpConnection.class);
    expect(HttpConnection.getCurrentConnection()).andReturn(httpConnection);

    expect(httpConnection.getHttpChannel()).andReturn(httpChannel);
    expect(httpChannel.getRequest()).andReturn(request);
    expect(httpChannel.getResponse()).andReturn(response).anyTimes();
    expect(response.getStatus()).andReturn(HttpStatus.SC_BAD_REQUEST);

    final StringWriter writer = new StringWriter();
    expect(httpServletResponse.getWriter()).andReturn(new PrintWriter(writer));

    expect(propertiesProvider.get()).andReturn(null).anyTimes();

    replayAll();

    final String expectedResponse = "{\"status\":400,\"message\":\"Bad Request\"}";

    AmbariErrorHandler ambariErrorHandler = new AmbariErrorHandler(gson, propertiesProvider);

    //when
    ambariErrorHandler.handle(target, request, httpServletRequest, httpServletResponse);
    System.out.println(writer.toString());

    //then
    assertEquals(expectedResponse, writer.toString());
    verifyAll();
  }
}
