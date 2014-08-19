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

package org.apache.ambari.server.security.authorization;

import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.security.authorization.internal.InternalAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AmbariAuthorizationFilter implements Filter {

  private static final String REALM_PARAM = "realm";
  private static final String DEFAULT_REALM = "AuthFilter";

  private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

  /**
   * The realm to use for the basic http auth
   */
  private String realm;


  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    realm = getParameterValue(filterConfig, REALM_PARAM, DEFAULT_REALM);
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    SecurityContext context = getSecurityContext();

    Authentication authentication = context.getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      String token = httpRequest.getHeader(INTERNAL_TOKEN_HEADER);
      if (token != null) {
        context.setAuthentication(new InternalAuthenticationToken(token));
      }
    } else {
      boolean authorized = false;

      for (GrantedAuthority grantedAuthority : authentication.getAuthorities()) {
        if (grantedAuthority instanceof AmbariGrantedAuthority) {

          AmbariGrantedAuthority ambariGrantedAuthority = (AmbariGrantedAuthority) grantedAuthority;

          PrivilegeEntity privilegeEntity = ambariGrantedAuthority.getPrivilegeEntity();
          String          requestURI      = httpRequest.getRequestURI();
          Integer         permissionId    = privilegeEntity.getPermission().getId();

          // admin has full access
          if (permissionId.equals(PermissionEntity.AMBARI_ADMIN_PERMISSION)) {
            authorized = true;
            break;
          }

          if (requestURI.matches("/api/v[0-9]+/clusters.*")) {
            // clusters require permission
            if (permissionId.equals(PermissionEntity.CLUSTER_READ_PERMISSION) ||
                permissionId.equals(PermissionEntity.CLUSTER_OPERATE_PERMISSION)) {
              authorized = true;
              break;
            }
          } else if (requestURI.matches("/api/v[0-9]+/views.*")) {
            // views require permission
            if (permissionId.equals(PermissionEntity.VIEW_USE_PERMISSION)) {
              authorized = true;
              break;
            }
          } else if (requestURI.matches("/api/v[0-9]+/persist.*")) {
            if (permissionId.equals(PermissionEntity.CLUSTER_OPERATE_PERMISSION)) {
              authorized = true;
              break;
            }
          }
        }
      }
      if (!authorized && !httpRequest.getMethod().equals("GET")) {

        httpResponse.setHeader("WWW-Authenticate", "Basic realm=\"" + realm + "\"");
        httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "You do not have permissions to access this resource.");
        httpResponse.flushBuffer();
        return;
      }
    }

    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {
    // do nothing
  }


  // ----- helper methods ----------------------------------------------------

  /**
   * Get the parameter value from the given servlet filter configuration.
   *
   * @param filterConfig   the servlet configuration
   * @param parameterName  the parameter name
   * @param defaultValue   the default value
   *
   * @return the parameter value or the default value if not set
   */
  private static String getParameterValue(
      FilterConfig filterConfig, String parameterName, String defaultValue) {

    String value = filterConfig.getInitParameter(parameterName);
    if (value == null || value.length() == 0) {
      value = filterConfig.getServletContext().getInitParameter(parameterName);
    }
    return value == null || value.length() == 0 ? defaultValue : value;
  }

  SecurityContext getSecurityContext() {
    return SecurityContextHolder.getContext();
  }
}
