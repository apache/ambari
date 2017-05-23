/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.utils;

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;

public class RequestUtilsTest {

  public static final String REMOTE_ADDRESS = "12.13.14.15";
  public static final String REMOTE_ADDRESS_MULTIPLE = "12.13.14.15,12.13.14.16";

  @Test
  public void testGetRemoteAddress() {
    // GIVEN
    HttpServletRequest mockedRequest = createMock(HttpServletRequest.class);
    expect(mockedRequest.getHeader("X-Forwarded-For")).andReturn(null);
    expect(mockedRequest.getHeader("Proxy-Client-IP")).andReturn("unknown");
    expect(mockedRequest.getHeader("WL-Proxy-Client-IP")).andReturn("");
    expect(mockedRequest.getHeader("HTTP_CLIENT_IP")).andReturn("unknown");
    expect(mockedRequest.getHeader("HTTP_X_FORWARDED_FOR")).andReturn(REMOTE_ADDRESS);
    replay(mockedRequest);
    // WHEN
    String remoteAddress = RequestUtils.getRemoteAddress(mockedRequest);
    // THEN
    assertEquals(REMOTE_ADDRESS, remoteAddress);
    verify(mockedRequest);
  }

  @Test
  public void testGetMultipleRemoteAddress() {
    // GIVEN
    HttpServletRequest mockedRequest = createMock(HttpServletRequest.class);
    expect(mockedRequest.getHeader("X-Forwarded-For")).andReturn(null);
    expect(mockedRequest.getHeader("Proxy-Client-IP")).andReturn("unknown");
    expect(mockedRequest.getHeader("WL-Proxy-Client-IP")).andReturn("");
    expect(mockedRequest.getHeader("HTTP_CLIENT_IP")).andReturn("unknown");
    expect(mockedRequest.getHeader("HTTP_X_FORWARDED_FOR")).andReturn(REMOTE_ADDRESS_MULTIPLE);
    replay(mockedRequest);
    // WHEN
    String remoteAddress = RequestUtils.getRemoteAddress(mockedRequest);
    // THEN
    assertEquals(REMOTE_ADDRESS, remoteAddress);
    verify(mockedRequest);
  }

  @Test
  public void testGetRemoteAddressFoundFirstHeader() {
    // GIVEN
    HttpServletRequest mockedRequest = createMock(HttpServletRequest.class);
    expect(mockedRequest.getHeader("X-Forwarded-For")).andReturn(REMOTE_ADDRESS);
    replay(mockedRequest);
    // WHEN
    String remoteAddress = RequestUtils.getRemoteAddress(mockedRequest);
    // THEN
    assertEquals(REMOTE_ADDRESS, remoteAddress);
    verify(mockedRequest);
  }

  @Test
  public void testGetRemoteAddressWhenHeadersAreMissing() {
    // GIVEN
    HttpServletRequest mockedRequest = createMock(HttpServletRequest.class);
    expect(mockedRequest.getHeader(anyString())).andReturn(null).times(5);
    expect(mockedRequest.getRemoteAddr()).andReturn(REMOTE_ADDRESS);
    replay(mockedRequest);
    // WHEN
    String remoteAddress = RequestUtils.getRemoteAddress(mockedRequest);
    // THEN
    assertEquals(REMOTE_ADDRESS, remoteAddress);
    verify(mockedRequest);
  }
}
