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

package org.apache.ambari.server.security;

import java.io.IOException;



import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class SecurityFilterTest {
  
  private Injector injector;

  @Before
  public void setUp() throws IOException {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    SecurityFilter.init(injector.getInstance(Configuration.class));
  }
  
  @Test
  public void mustFilterNonHttpsRequests() throws Exception {
    SecurityFilter filter = new SecurityFilter();
    MockHttpServletRequest request = this.getDefaultRequest();
    request.setRequestURI("/certs/");
    request.setScheme("http");
    MockHttpServletResponse response = new MockHttpServletResponse();
    request.setLocalPort(8440);
    MockFilterChain chain = new MockFilterChain();
    filter.doFilter(request, response, chain);
    assertNull(chain.getRequest());
    assertNull(chain.getResponse());
  }

  @Test
  public void mustAllowSecurePortRequests() throws Exception {
    SecurityFilter filter = new SecurityFilter();
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockHttpServletRequest request = this.getDefaultRequest();
    request.setServerPort(8441);
    request.setLocalPort(8441);
    request.setRequestURI("/certs/");
    MockFilterChain chain = new MockFilterChain();
    filter.doFilter(request, response, chain);
    assertEquals(request, chain.getRequest());
    assertEquals(response, chain.getResponse());
  }

  @Test
  public void mustAllowCertCreationRequests() throws Exception {
    SecurityFilter filter = new SecurityFilter();
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockHttpServletRequest request = this.getDefaultRequest();
    request.setRequestURI("/certs/www.andromeda-01.com");
    request.setMethod("POST");
    MockFilterChain chain = new MockFilterChain();
    filter.doFilter(request, response, chain);
    assertEquals(request, chain.getRequest());
    assertEquals(response, chain.getResponse());
  }

  @Test
  public void mustAllowCertCaGetRequests() throws Exception {
    SecurityFilter filter = new SecurityFilter();
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockHttpServletRequest request = this.getDefaultRequest();
    request.setRequestURI("/cert/ca/");
    MockFilterChain chain = new MockFilterChain();
    filter.doFilter(request, response, chain);
    assertEquals(request, chain.getRequest());
    assertEquals(response, chain.getResponse());
  }

  private MockHttpServletRequest getDefaultRequest() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setServerPort(8440);
    request.setMethod("GET");
    request.setServerName("www.andromeda-01.com");
    request.setScheme("https");
    return request;
  }
}
