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

import org.apache.log4j.Logger;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

public class LogsearchAuthenticationEntryPoint extends LoginUrlAuthenticationEntryPoint {
  private static final Logger logger = Logger.getLogger(LogsearchAuthenticationEntryPoint.class);

  public LogsearchAuthenticationEntryPoint(String loginFormUrl) {
    super(loginFormUrl);
  }

  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
    throws IOException, ServletException {
    String ajaxRequestHeader = request.getHeader("X-Requested-With");
    if (ajaxRequestHeader != null && ajaxRequestHeader.equalsIgnoreCase("XMLHttpRequest")) {
      logger.debug("AJAX request. Authentication required. Returning URL=" + request.getRequestURI());
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Session Timeout");
    } else {
      logger.debug("Redirecting to login page :" + this.getLoginFormUrl());
      response.sendRedirect(this.getLoginFormUrl() + ((request.getQueryString() != null) ? "?" + request.getQueryString() : ""));
    }
  }
}
