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
import java.util.Date;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.ambari.server.audit.AuditEvent;
import org.apache.ambari.server.audit.AuditLogger;
import org.apache.ambari.server.audit.LoginFailedAuditEvent;
import org.apache.ambari.server.audit.LoginSucceededAuditEvent;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.utils.RequestUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

public class AmbariAuthenticationFilter extends BasicAuthenticationFilter {
  private static final Logger LOG = LoggerFactory.getLogger(AmbariAuthenticationFilter.class);

  private AuditLogger auditLogger;

  public AmbariAuthenticationFilter() {
    super();
  }

  public AmbariAuthenticationFilter(AuthenticationManager authenticationManager, AuditLogger auditLogger) {
    super(authenticationManager);
    this.auditLogger = auditLogger;
  }

  @Override
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) req;
    String header = request.getHeader("Authorization");
    if (AuthorizationHelper.getAuthenticatedName() == null && (header == null || !header.startsWith("Basic "))) {
      AuditEvent loginFailedAuditEvent = LoginFailedAuditEvent.builder()
        .withRemoteIp(RequestUtils.getRemoteAddress(request))
        .withTimestamp(new DateTime(new Date()))
        .withReason("Authentication required")
        .withUserName(null)
        .build();
      auditLogger.log(loginFailedAuditEvent);
    }
    super.doFilter(req, res, chain);
  }

  @Override
  protected void onSuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, Authentication authResult) throws IOException {
    AuditEvent loginSucceededAuditEvent = LoginSucceededAuditEvent.builder()
      .withRemoteIp(RequestUtils.getRemoteAddress(request))
      .withUserName(authResult.getName())
      .withTimestamp(new DateTime(new Date()))
      .withRoles(AuthorizationHelper.getPermissionLabels(authResult))
      .withRoles(AuthorizationHelper.getAuthorizationNames(authResult))
      .build();
    auditLogger.log(loginSucceededAuditEvent);
  }

  @Override
  protected void onUnsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException authEx) throws IOException {
    String header = request.getHeader("Authorization");
    String username = null;
    try {
      String[] decodedAuth = decodeAuth(header, request);
      username = decodedAuth[0];
    } catch (Exception e) {
      LOG.warn("Error occurred during decoding authorization header.",e);
    }
    AuditEvent loginFailedAuditEvent = LoginFailedAuditEvent.builder()
      .withRemoteIp(RequestUtils.getRemoteAddress(request))
      .withTimestamp(new DateTime(new Date()))
      .withReason("Invalid username/password combination")
      .withUserName(username)
      .build();
    auditLogger.log(loginFailedAuditEvent);
  }

  private String[] decodeAuth(String header, HttpServletRequest request) throws IOException {
    byte[] base64Token = header.substring(6).getBytes("UTF-8");

    byte[] decoded;
    try {
      decoded = Base64.decode(base64Token);
    } catch (IllegalArgumentException ex) {
      throw new BadCredentialsException("Failed to decode basic authentication token");
    }

    String token = new String(decoded, this.getCredentialsCharset(request));
    int delim = token.indexOf(":");
    if(delim == -1) {
      throw new BadCredentialsException("Invalid basic authentication token");
    } else {
      return new String[]{token.substring(0, delim), token.substring(delim + 1)};
    }
  }
}
