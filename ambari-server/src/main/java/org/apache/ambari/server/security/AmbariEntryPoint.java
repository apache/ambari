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
package org.apache.ambari.server.security;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AmbariEntryPoint implements AuthenticationEntryPoint {
  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
    /* *****************************************************************************************
     * To maintain backward compatibility and respond with the appropriate response when
     * authentication is needed, by default return an HTTP 403 status.
     *
     * However if requested by the user, respond such that the client is challenged to Negotiate
     * and reissue the request with a Kerberos token.  This response is an HTTP 401 status with the
     * WWW-Authenticate: Negotiate" header.
     * ****************************************************************************************** */
    if ("true".equalsIgnoreCase(request.getHeader("X-Negotiate-Authentication"))) {
      response.setHeader("WWW-Authenticate", "Negotiate");
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication requested");
    } else {
      response.sendError(HttpServletResponse.SC_FORBIDDEN, authException.getMessage());
    }
  }
}
