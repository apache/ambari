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
package org.apache.ambari.server.security.authentication;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ambari.server.audit.AuditLogger;
import org.apache.ambari.server.audit.event.AuditEvent;
import org.apache.ambari.server.audit.event.LoginAuditEvent;
import org.apache.ambari.server.security.AmbariEntryPoint;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.PermissionHelper;
import org.apache.ambari.server.utils.RequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * AmbariBasicAuthenticationFilter extends a {@link BasicAuthenticationFilter} to allow for auditing
 * of authentication attempts
 * <p>
 * This authentication filter is expected to be used withing an {@link AmbariDelegatingAuthenticationFilter}.
 *
 * @see AmbariDelegatingAuthenticationFilter
 */
public class AmbariBasicAuthenticationFilter extends BasicAuthenticationFilter implements AmbariAuthenticationFilter {
  private static final Logger LOG = LoggerFactory.getLogger(AmbariBasicAuthenticationFilter.class);

  /**
   * Audit logger
   */
  private AuditLogger auditLogger;

  /**
   * PermissionHelper to help create audit entries
   */
  private PermissionHelper permissionHelper;

  /**
   * Constructor.
   *
   * @param authenticationManager the Spring authencation manager
   * @param ambariEntryPoint      the Spring entry point
   * @param auditLogger           an Audit Logger
   * @param permissionHelper      a permission helper
   */
  public AmbariBasicAuthenticationFilter(AuthenticationManager authenticationManager,
                                         AmbariEntryPoint ambariEntryPoint,
                                         AuditLogger auditLogger,
                                         PermissionHelper permissionHelper) {
    super(authenticationManager, ambariEntryPoint);
    this.auditLogger = auditLogger;
    this.permissionHelper = permissionHelper;
  }

  /**
   * Tests to see if this {@link AmbariBasicAuthenticationFilter} should be applied in the authentication
   * filter chain.
   * <p>
   * <code>true</code> will be returned if the HTTP request contains the basic authentication header;
   * otherwise <code>false</code> will be returned.
   * <p>
   * The basic authentication header is named "Authorization" and the value begins with the string
   * "Basic" following by the encoded username and password information.
   * <p>
   * For example:
   * <code>
   * Authorization: Basic YWRtaW46YWRtaW4=
   * </code>
   *
   * @param httpServletRequest the HttpServletRequest the HTTP service request
   * @return <code>true</code> if the HTTP request contains the basic authentication header; otherwise <code>false</code>
   */
  @Override
  public boolean shouldApply(HttpServletRequest httpServletRequest) {
    String header = httpServletRequest.getHeader("Authorization");
    return (header != null) && header.startsWith("Basic ");
  }

  /**
   * Checks whether the authentication information is filled. If it is not, then a login failed audit event is logged
   *
   * @param servletRequest  the request
   * @param servletResponse the response
   * @param chain           the Spring filter chain
   * @throws IOException
   * @throws ServletException
   */
  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;

    if (auditLogger.isEnabled() && shouldApply(httpServletRequest) && (AuthorizationHelper.getAuthenticatedName() == null)) {
      AuditEvent loginFailedAuditEvent = LoginAuditEvent.builder()
          .withRemoteIp(RequestUtils.getRemoteAddress(httpServletRequest))
          .withTimestamp(System.currentTimeMillis())
          .withReasonOfFailure("Authentication required")
          .withUserName(null)
          .build();
      auditLogger.log(loginFailedAuditEvent);
    }

    super.doFilter(servletRequest, servletResponse, chain);
  }

  /**
   * If the authentication was successful, then an audit event is logged about the success
   *
   * @param servletRequest  the request
   * @param servletResponse the response
   * @param authResult      the Authentication result
   * @throws IOException
   */
  @Override
  protected void onSuccessfulAuthentication(HttpServletRequest servletRequest,
                                            HttpServletResponse servletResponse,
                                            Authentication authResult) throws IOException {
    if (auditLogger.isEnabled()) {
      AuditEvent loginSucceededAuditEvent = LoginAuditEvent.builder()
          .withRemoteIp(RequestUtils.getRemoteAddress(servletRequest))
          .withUserName(authResult.getName())
          .withTimestamp(System.currentTimeMillis())
          .withRoles(permissionHelper.getPermissionLabels(authResult))
          .build();
      auditLogger.log(loginSucceededAuditEvent);
    }
  }

  /**
   * In the case of invalid username or password, the authentication fails and it is logged
   *
   * @param servletRequest  the request
   * @param servletResponse the response
   * @param authExecption   the exception, if any, causing the unsuccessful authentication attempt
   * @throws IOException
   */
  @Override
  protected void onUnsuccessfulAuthentication(HttpServletRequest servletRequest,
                                              HttpServletResponse servletResponse,
                                              AuthenticationException authExecption) throws IOException {
    String header = servletRequest.getHeader("Authorization");
    String username = null;
    try {
      username = getUsernameFromAuth(header, getCredentialsCharset(servletRequest));
    } catch (Exception e) {
      LOG.warn("Error occurred during decoding authorization header.", e);
    }
    if (auditLogger.isEnabled()) {
      AuditEvent loginFailedAuditEvent = LoginAuditEvent.builder()
          .withRemoteIp(RequestUtils.getRemoteAddress(servletRequest))
          .withTimestamp(System.currentTimeMillis())
          .withReasonOfFailure("Invalid username/password combination")
          .withUserName(username)
          .build();
      auditLogger.log(loginFailedAuditEvent);
    }
  }

  /**
   * Helper function to decode Authorization header
   *
   * @param authenticationValue the authentication value to parse
   * @param charSet             the character set of the authentication value
   * @return the username parsed from the authentication header value
   * @throws IOException
   */
  private String getUsernameFromAuth(String authenticationValue, String charSet) throws IOException {
    byte[] base64Token = authenticationValue.substring(6).getBytes("UTF-8");

    byte[] decoded;
    try {
      decoded = Base64.decode(base64Token);
    } catch (IllegalArgumentException ex) {
      throw new BadCredentialsException("Failed to decode basic authentication token");
    }

    String token = new String(decoded, charSet);
    int delimiter = token.indexOf(":");
    if (delimiter == -1) {
      throw new BadCredentialsException("Invalid basic authentication token");
    } else {
      return token.substring(0, delimiter);
    }
  }
}
