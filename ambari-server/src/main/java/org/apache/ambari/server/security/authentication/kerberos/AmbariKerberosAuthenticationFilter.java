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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.ambari.server.audit.AuditLogger;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.security.authentication.AmbariAuthenticationEventHandler;
import org.apache.ambari.server.security.authentication.AmbariAuthenticationException;
import org.apache.ambari.server.security.authentication.AmbariAuthenticationFilter;
import org.apache.ambari.server.security.authentication.tproxy.TrustedProxyAuthenticationDetailsSource;
import org.apache.ambari.server.utils.RequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.kerberos.web.authentication.SpnegoAuthenticationProcessingFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * AmbariKerberosAuthenticationFilter extends the {@link SpnegoAuthenticationProcessingFilter} class
 * to perform Kerberos-based authentication for Ambari.
 * <p>
 * If configured, auditing is performed using {@link AuditLogger}.
 */
@Component
@Order(2)
public class AmbariKerberosAuthenticationFilter extends SpnegoAuthenticationProcessingFilter implements AmbariAuthenticationFilter {

  private static final Logger LOG = LoggerFactory.getLogger(AmbariKerberosAuthenticationFilter.class);

  /**
   * Ambari authentication event handler
   */
  private final AmbariAuthenticationEventHandler eventHandler;

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
   * @param eventHandler          the Ambari authentication event handler
   */
  public AmbariKerberosAuthenticationFilter(AuthenticationManager authenticationManager,
                                            final AuthenticationEntryPoint entryPoint,
                                            Configuration configuration,
                                            final AmbariAuthenticationEventHandler eventHandler) {
    AmbariKerberosAuthenticationProperties kerberosAuthenticationProperties = (configuration == null)
        ? null
        : configuration.getKerberosAuthenticationProperties();

    kerberosAuthenticationEnabled = (kerberosAuthenticationProperties != null) && kerberosAuthenticationProperties.isKerberosAuthenticationEnabled();

    if (eventHandler == null) {
      throw new IllegalArgumentException("The AmbariAuthenticationEventHandler must not be null");
    }

    this.eventHandler = eventHandler;

    setAuthenticationManager(authenticationManager);

    setAuthenticationDetailsSource(new TrustedProxyAuthenticationDetailsSource());

    setFailureHandler(new AuthenticationFailureHandler() {
      @Override
      public void onAuthenticationFailure(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AuthenticationException e) throws IOException, ServletException {
        AmbariAuthenticationException cause;
        if (e instanceof AmbariAuthenticationException) {
          cause = (AmbariAuthenticationException) e;
        } else {
          cause = new AmbariAuthenticationException(null, e.getLocalizedMessage(), false, e);
        }
        eventHandler.onUnsuccessfulAuthentication(AmbariKerberosAuthenticationFilter.this, httpServletRequest, httpServletResponse, cause);

        entryPoint.commence(httpServletRequest, httpServletResponse, e);
      }
    });

    setSuccessHandler(new AuthenticationSuccessHandler() {
      @Override
      public void onAuthenticationSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) throws IOException, ServletException {
        eventHandler.onSuccessfulAuthentication(AmbariKerberosAuthenticationFilter.this, httpServletRequest, httpServletResponse, authentication);
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
    if (LOG.isDebugEnabled()) {
      RequestUtils.logRequestHeadersAndQueryParams(httpServletRequest, LOG);
    }

    if (kerberosAuthenticationEnabled) {
      String header = httpServletRequest.getHeader("Authorization");
      return (header != null) && (header.startsWith("Negotiate ") || header.startsWith("Kerberos "));
    } else {
      return false;
    }
  }

  @Override
  public boolean shouldIncrementFailureCount() {
    // Always return false since authentication happens remotely.
    return false;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
          throws ServletException, IOException {
    if (eventHandler != null) {
      eventHandler.beforeAttemptAuthentication(this, request, response);
    }

    super.doFilter(request, response, filterChain);
  }
}
