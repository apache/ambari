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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class MethodOverrideFilterTest {
  private final MethodOverrideFilter filter = new MethodOverrideFilter();

  @Test
  public void preservesBodyQueryWhenPostIsOverriddenAsGet() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/clusters/c1/hosts");
    request.addHeader("X-Http-Method-Override", "GET");
    request.setQueryString("fields=Hosts/host_name&minimal_response=true");
    request.setContentType("application/json");
    byte[] body = "{\"RequestInfo\":{\"query\":\"Hosts/host_name.in(worker2)&Hosts/host_state=HEALTHY\"}}"
        .getBytes(StandardCharsets.UTF_8);
    request.setContent(body);
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    HttpServletRequest forwarded = (HttpServletRequest) chain.getRequest();
    assertEquals("GET", forwarded.getMethod());
    assertEquals(
        "fields=Hosts/host_name&minimal_response=true&Hosts/host_name.in(worker2)&Hosts/host_state=HEALTHY",
        forwarded.getQueryString());
    assertArrayEquals(body, forwarded.getInputStream().readAllBytes());
    assertEquals(new String(body, StandardCharsets.UTF_8), forwarded.getReader().lines().collect(
        java.util.stream.Collectors.joining("\n")));
  }

  @Test
  public void forwardsRequestUnchangedWithoutOverrideHeader() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/clusters/c1/hosts");
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, new MockHttpServletResponse(), chain);

    assertSame(request, chain.getRequest());
  }

  @Test
  public void rejectsUnsupportedOverrideMethods() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/clusters/c1/hosts");
    request.addHeader("X-Http-Method-Override", "DELETE");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertEquals(400, response.getStatus());
    assertNull(chain.getRequest());
  }
}
