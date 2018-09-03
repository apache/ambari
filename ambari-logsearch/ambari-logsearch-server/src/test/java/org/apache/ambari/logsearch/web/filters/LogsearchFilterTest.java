package org.apache.ambari.logsearch.web.filters;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.strictMock;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ambari.logsearch.common.StatusMessage;
import org.apache.ambari.logsearch.util.JSONUtil;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.web.util.matcher.RequestMatcher;

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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
public class LogsearchFilterTest {
  private static final String REQUEST_URI = "/api/v1/test";
  private RequestMatcher requestMatcher;
  private StatusProvider statusProvider;
  private HttpServletRequest servletRequest;
  private HttpServletResponse servletResponse;
  private FilterChain filterChain;

  @Before
  public void setUp() {
    requestMatcher = strictMock(RequestMatcher.class);
    statusProvider = strictMock(StatusProvider.class);
    servletRequest = strictMock(HttpServletRequest.class);
    servletResponse = strictMock(HttpServletResponse.class);
    filterChain = strictMock(FilterChain.class);

    expect(servletRequest.getRequestURI()).andReturn(REQUEST_URI).anyTimes();
  }

  @Test
  public void testDoFilterCallsFilterChainDoFilterIfRequestURIDoesNotMatch() throws Exception {
    expect(requestMatcher.matches(servletRequest)).andReturn(false);
    filterChain.doFilter(servletRequest, servletResponse); expectLastCall();

    replay(requestMatcher, statusProvider, servletRequest, servletResponse, filterChain);

    LogsearchFilter filter = new LogsearchFilter(requestMatcher, statusProvider);
    filter.doFilter(servletRequest, servletResponse, filterChain);

    verify(requestMatcher, statusProvider, servletRequest, servletResponse, filterChain);
  }

  @Test
  public void testDoFilterCallsFilterChainDoFilterIfNoError() throws Exception {
    expect(requestMatcher.matches(servletRequest)).andReturn(true).anyTimes();
    expect(statusProvider.getStatusMessage(REQUEST_URI)).andReturn(null);
    filterChain.doFilter(servletRequest, servletResponse); expectLastCall();

    replay(requestMatcher, statusProvider, servletRequest, servletResponse, filterChain);

    LogsearchFilter filter = new LogsearchFilter(requestMatcher, statusProvider);
    filter.doFilter(servletRequest, servletResponse, filterChain);

    verify(requestMatcher, statusProvider, servletRequest, servletResponse, filterChain);
  }

  @Test
  public void testDoFilterWritesStatusMessageInCaseOfAnError() throws Exception {
    StatusMessage statusMessage = StatusMessage.with(INTERNAL_SERVER_ERROR, "Error occurred");
    StringWriter stringWriter = new StringWriter();

    expect(requestMatcher.matches(servletRequest)).andReturn(true).anyTimes();
    expect(statusProvider.getStatusMessage(REQUEST_URI)).andReturn(statusMessage);
    expect(servletRequest.getRequestURL()).andReturn(new StringBuffer(REQUEST_URI)).anyTimes();
    servletResponse.setStatus(statusMessage.getStatus()); expectLastCall();
    servletResponse.setContentType("application/json"); expectLastCall();
    expect(servletResponse.getWriter()).andReturn(new PrintWriter(stringWriter));

    replay(requestMatcher, statusProvider, servletRequest, servletResponse, filterChain);

    LogsearchFilter filter = new LogsearchFilter(requestMatcher, statusProvider);
    filter.doFilter(servletRequest, servletResponse, filterChain);

    verify(requestMatcher, statusProvider, servletRequest, servletResponse, filterChain);

    Map<String, Object> map = JSONUtil.jsonToMapObject(stringWriter.toString());
    assertThat(map, is(not(nullValue())));
    assertThat(map.get("status"), is(statusMessage.getStatus()));
    assertThat(map.get("message"), is(statusMessage.getMessage()));
  }
}