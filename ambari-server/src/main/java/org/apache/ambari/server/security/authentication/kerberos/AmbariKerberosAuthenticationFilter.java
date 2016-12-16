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

package org.apache.ambari.server.security.authentication.kerberos;

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
import org.apache.ambari.server.security.authentication.AmbariAuthenticationFilter;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.PermissionHelper;
import org.apache.ambari.server.utils.RequestUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.kerberos.web.authentication.SpnegoAuthenticationProcessingFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

/**
 * AmbariKerberosAuthenticationFilter extends the {@link SpnegoAuthenticationProcessingFilter} class
 * to perform Kerberos-based authentication for Ambari.
 * <p>
 * If configured, auditing is performed using {@link AuditLogger}.
 */
public class AmbariKerberosAuthenticationFilter extends SpnegoAuthenticationProcessingFilter implements AmbariAuthenticationFilter {

  /**
   * Audit logger
   */
  private final AuditLogger auditLogger;

  /**
   * A Boolean value indicating whether Kerberos authentication is enabled or not.
   */
  private final boolean kerberosAuthenticationEnabled;

  /**
   * Constructor.
   * <p>
   * Given supplied data, sets up the the {@link SpnegoAuthenticationProcessingFilter} to perform
   * authentication and audit logging if configured do to so.
   *
   * @param authenticationManager the Spring authentication manager
   * @param entryPoint            the Spring entry point
   * @param configuration         the Ambari configuration data
   * @param auditLogger           an audit logger
   * @param permissionHelper      a permission helper to aid in audit logging
   */
  public AmbariKerberosAuthenticationFilter(AuthenticationManager authenticationManager, final AuthenticationEntryPoint entryPoint, Configuration configuration, final AuditLogger auditLogger, final PermissionHelper permissionHelper) {
    AmbariKerberosAuthenticationProperties kerberosAuthenticationProperties = (configuration == null)
        ? null
        : configuration.getKerberosAuthenticationProperties();

    kerberosAuthenticationEnabled = (kerberosAuthenticationProperties != null) && kerberosAuthenticationProperties.isKerberosAuthenticationEnabled();

    this.auditLogger = auditLogger;

    setAuthenticationManager(authenticationManager);

    setFailureHandler(new AuthenticationFailureHandler() {
      @Override
      public void onAuthenticationFailure(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AuthenticationException e) throws IOException, ServletException {
        if (auditLogger.isEnabled()) {
          AuditEvent loginFailedAuditEvent = LoginAuditEvent.builder()
              .withRemoteIp(RequestUtils.getRemoteAddress(httpServletRequest))
              .withTimestamp(System.currentTimeMillis())
              .withReasonOfFailure(e.getLocalizedMessage())
              .build();
          auditLogger.log(loginFailedAuditEvent);
        }

        entryPoint.commence(httpServletRequest, httpServletResponse, e);
      }
    });

    setSuccessHandler(new AuthenticationSuccessHandler() {
      @Override
      public void onAuthenticationSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) throws IOException, ServletException {
        if (auditLogger.isEnabled()) {
          AuditEvent loginSucceededAuditEvent = LoginAuditEvent.builder()
              .withRemoteIp(RequestUtils.getRemoteAddress(httpServletRequest))
              .withUserName(authentication.getName())
              .withTimestamp(System.currentTimeMillis())
              .withRoles(permissionHelper.getPermissionLabels(authentication))
              .build();
          auditLogger.log(loginSucceededAuditEvent);
        }
      }
    });
  }

  /**
   * Tests to determine if this authentication filter is applicable given the Ambari configuration
   * and the user's HTTP request.
   * <p>
   * If the Ambari configuration indicates the Kerberos authentication is enabled and the HTTP request
   * contains the appropriate <code>Authorization</code> header, than this filter may be applied;
   * otherwise it should be skipped.
   *
   * @param httpServletRequest the request
   * @return true if this filter should be applied; false otherwise
   */
  @Override
  public boolean shouldApply(HttpServletRequest httpServletRequest) {
    if (kerberosAuthenticationEnabled) {
      String header = httpServletRequest.getHeader("Authorization");
      return (header != null) && (header.startsWith("Negotiate ") || header.startsWith("Kerberos "));
    } else {
      return false;
    }
  }

  /**
   * Performs the logic for this filter.
   * <p>
   * Checks whether the authentication information is filled. If it is not, then a login failed audit event is logged.
   * <p>
   * Then, forwards the workflow to {@link SpnegoAuthenticationProcessingFilter#doFilter(ServletRequest, ServletResponse, FilterChain)}
   *
   * @param servletRequest  the request
   * @param servletResponse the response
   * @param filterChain     the Spring filter chain
   * @throws IOException
   * @throws ServletException
   */
  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
    HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;

    if (shouldApply(httpServletRequest)) {
      if (auditLogger.isEnabled() && (AuthorizationHelper.getAuthenticatedName() == null)) {
        AuditEvent loginFailedAuditEvent = LoginAuditEvent.builder()
            .withRemoteIp(RequestUtils.getRemoteAddress(httpServletRequest))
            .withTimestamp(System.currentTimeMillis())
            .withReasonOfFailure("Authentication required")
            .withUserName(null)
            .build();
        auditLogger.log(loginFailedAuditEvent);
      }

      super.doFilter(servletRequest, servletResponse, filterChain);
    } else {
      filterChain.doFilter(servletRequest, servletResponse);
    }
  }
}
