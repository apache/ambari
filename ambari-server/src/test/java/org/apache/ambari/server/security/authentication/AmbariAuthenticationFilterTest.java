/**
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
package org.apache.ambari.server.security.authentication;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.ambari.server.audit.event.AuditEvent;
import org.apache.ambari.server.audit.AuditLogger;
import org.apache.ambari.server.security.AmbariEntryPoint;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.PermissionHelper;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import org.springframework.security.crypto.codec.Base64;

@RunWith(PowerMockRunner.class)
@PrepareForTest(AuthorizationHelper.class)
public class AmbariAuthenticationFilterTest {

  private AmbariAuthenticationFilter underTest;

  private AuditLogger mockedAuditLogger;

  private PermissionHelper permissionHelper;

  private AmbariEntryPoint entryPoint;

  @Before
  public void setUp() {
    mockedAuditLogger = createMock(AuditLogger.class);
    permissionHelper = createMock(PermissionHelper.class);
    entryPoint = createMock(AmbariEntryPoint.class);
    underTest = new AmbariAuthenticationFilter(null, mockedAuditLogger, permissionHelper, entryPoint);
    replay(entryPoint);
  }

  @Test
  public void testDoFilter() throws IOException, ServletException {
    // GIVEN
    HttpServletRequest request = createMock(HttpServletRequest.class);
    HttpServletResponse response = createMock(HttpServletResponse.class);
    FilterChain filterChain = createMock(FilterChain.class);
    expect(request.getHeader("Authorization")).andReturn("header").andReturn(null);
    expect(request.getHeader("X-Forwarded-For")).andReturn("1.2.3.4");
    mockedAuditLogger.log(anyObject(AuditEvent.class));
    expectLastCall().times(1);
    filterChain.doFilter(request, response);
    expectLastCall();
    replay(mockedAuditLogger, request, filterChain);
    // WHEN
    underTest.doFilter(request, response, filterChain);
    // THEN
    verify(mockedAuditLogger, request, filterChain);
  }

  @Test
  public void testOnSuccessfulAuthentication() throws IOException, ServletException {
    // GIVEN
    HttpServletRequest request = createMock(HttpServletRequest.class);
    HttpServletResponse response = createMock(HttpServletResponse.class);
    Authentication authentication = createMock(Authentication.class);
    PowerMock.mockStatic(AuthorizationHelper.class);

    Map<String, List<String>> roles = new HashMap<>();
    roles.put("a", Arrays.asList("r1", "r2", "r3"));
    expect(permissionHelper.getPermissionLabels(authentication))
      .andReturn(roles);
    expect(AuthorizationHelper.getAuthorizationNames(authentication))
      .andReturn(Arrays.asList("perm1", "perm2"));
    expect(AuthorizationHelper.getAuthenticatedName()).andReturn("perm1");
    expect(request.getHeader("X-Forwarded-For")).andReturn("1.2.3.4");
    expect(authentication.getName()).andReturn("admin");
    mockedAuditLogger.log(anyObject(AuditEvent.class));
    expectLastCall().times(1);
    replay(mockedAuditLogger, request, authentication, permissionHelper);
    PowerMock.replayAll();
    // WHEN
    underTest.onSuccessfulAuthentication(request, response, authentication);
    // THEN
    verify(mockedAuditLogger, request);
  }

  @Test
  public void testOnUnsuccessfulAuthentication() throws IOException, ServletException {
    // GIVEN
    HttpServletRequest request = createMock(HttpServletRequest.class);
    HttpServletResponse response = createMock(HttpServletResponse.class);
    AuthenticationException authEx = createMock(AuthenticationException.class);
    expect(request.getHeader("X-Forwarded-For")).andReturn("1.2.3.4");
    expect(request.getHeader("Authorization")).andReturn(
      "Basic " + new String(Base64.encode("admin:admin".getBytes("UTF-8"))));
    mockedAuditLogger.log(anyObject(AuditEvent.class));
    expectLastCall().times(1);
    replay(mockedAuditLogger, request, authEx);
    // WHEN
    underTest.onUnsuccessfulAuthentication(request, response, authEx);
    // THEN
    verify(mockedAuditLogger, request, authEx);
  }
}
