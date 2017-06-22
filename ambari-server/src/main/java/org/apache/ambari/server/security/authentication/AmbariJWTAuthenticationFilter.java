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

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.security.authorization.Users;
import org.apache.ambari.server.security.authorization.jwt.JwtAuthenticationFilter;
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
   * Ambari authentication event handler
   */
  private final AmbariAuthenticationEventHandler eventHandler;


  /**
   * Constructor.
   *
   * @param ambariEntryPoint the Spring entry point
   * @param configuration    the Ambari configuration
   * @param users            the Ambari users object
   * @param eventHandler     the Ambari authentication event handler
   */
  public AmbariJWTAuthenticationFilter(AuthenticationEntryPoint ambariEntryPoint,
                                       Configuration configuration,
                                       Users users,
                                       AmbariAuthenticationEventHandler eventHandler) {
    super(configuration, ambariEntryPoint, users);

    if(eventHandler == null) {
      throw new IllegalArgumentException("The AmbariAuthenticationEventHandler must not be null");
    }

    this.eventHandler = eventHandler;
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

    if (eventHandler != null) {
      eventHandler.beforeAttemptAuthentication(this, servletRequest, servletResponse);
    }

    super.doFilter(servletRequest, servletResponse, chain);
  }

  @Override
  protected void onSuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, Authentication authResult) throws IOException {
    if (eventHandler != null) {
      eventHandler.onSuccessfulAuthentication(this, request, response, authResult);
    }
  }

  @Override
  protected void onUnsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
    if (eventHandler != null) {
      AmbariAuthenticationException cause;

      if (authException instanceof AmbariAuthenticationException) {
        cause = (AmbariAuthenticationException) authException;
      } else {
        cause = new AmbariAuthenticationException(null, authException.getMessage(), authException);
      }

      eventHandler.onUnsuccessfulAuthentication(this, request, response, cause);
    }
  }
}
