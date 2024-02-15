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
package org.apache.ambari.logsearch.web.filters;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ambari.logsearch.conf.AuthPropsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

public class LogsearchAuthenticationEntryPoint extends LoginUrlAuthenticationEntryPoint {
  private static final Logger logger = LoggerFactory.getLogger(LogsearchAuthenticationEntryPoint.class);
  private final AuthPropsConfig authPropsConfig;

  public LogsearchAuthenticationEntryPoint(String loginFormUrl, AuthPropsConfig authPropsConfig) {
    super(loginFormUrl);
    this.authPropsConfig = authPropsConfig;
  }

  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
    throws IOException, ServletException {
    if (!authPropsConfig.isAuthJwtEnabled()) { // TODO: find better solution if JWT enabled, as it always causes an basic auth failure before JWT auth
      logger.debug("Got 401 from request: {}", request.getRequestURI());
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }
  }
}
