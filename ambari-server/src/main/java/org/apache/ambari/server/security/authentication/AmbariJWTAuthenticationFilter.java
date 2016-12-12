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
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.PermissionHelper;
import org.apache.ambari.server.security.authorization.Users;
import org.apache.ambari.server.security.authorization.jwt.AuthenticationJwtUserNotFoundException;
import org.apache.ambari.server.security.authorization.jwt.JwtAuthenticationFilter;
import org.apache.ambari.server.utils.RequestUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * AmbariBasicAuthenticationFilter extends a {@link org.apache.ambari.server.security.authorization.jwt.JwtAuthenticationFilter}
 * to allow for auditing of authentication attempts.
 * <p>
 * This authentication filter is expected to be used withing an {@link AmbariDelegatingAuthenticationFilter}.
 *
 * @see AmbariDelegatingAuthenticationFilter
 */
public class AmbariJWTAuthenticationFilter extends JwtAuthenticationFilter implements AmbariAuthenticationFilter {

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
   * @param ambariEntryPoint the Spring entry point
   * @param configuration    the Ambari configuration
   * @param users            the Ambari users object
   * @param auditLogger      an Audit Logger
   * @param permissionHelper a permission helper
   */
  public AmbariJWTAuthenticationFilter(AuthenticationEntryPoint ambariEntryPoint,
                                       Configuration configuration,
                                       Users users,
                                       AuditLogger auditLogger,
                                       PermissionHelper permissionHelper) {
    super(configuration, ambariEntryPoint, users);
    this.auditLogger = auditLogger;
    this.permissionHelper = permissionHelper;
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

  @Override
  protected void onSuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, Authentication authResult) throws IOException {
    if (auditLogger.isEnabled()) {
      AuditEvent loginSucceededAuditEvent = LoginAuditEvent.builder()
          .withRemoteIp(RequestUtils.getRemoteAddress(request))
          .withUserName(authResult.getName())
          .withTimestamp(System.currentTimeMillis())
          .withRoles(permissionHelper.getPermissionLabels(authResult))
          .build();
      auditLogger.log(loginSucceededAuditEvent);
    }
  }

  @Override
  protected void onUnsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
    if (auditLogger.isEnabled()) {
      String username = null;
      if (authException instanceof AuthenticationJwtUserNotFoundException) {
        username = ((AuthenticationJwtUserNotFoundException) authException).getUsername();
      }

      AuditEvent loginFailedAuditEvent = LoginAuditEvent.builder()
          .withRemoteIp(RequestUtils.getRemoteAddress(request))
          .withTimestamp(System.currentTimeMillis())
          .withReasonOfFailure(authException.getLocalizedMessage())
          .withUserName(username)
          .build();
      auditLogger.log(loginFailedAuditEvent);
    }
  }
}
