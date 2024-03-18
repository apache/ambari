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
package org.apache.ambari.logsearch.web.security;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.ambari.logsearch.common.ExternalServerClient;
import org.apache.ambari.logsearch.conf.AuthPropsConfig;
import org.apache.ambari.logsearch.util.JSONUtil;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * 
 * Authentication provider to authenticate user from external-server using REST
 * call
 */
@Named
public class LogsearchExternalServerAuthenticationProvider extends LogsearchAbstractAuthenticationProvider {

  private static Logger LOG = Logger.getLogger(LogsearchExternalServerAuthenticationProvider.class);

  private static enum PrivilegeInfo {
    PERMISSION_LABEL("permission_label"),
    PERMISSION_NAME("permission_name"),
    PRINCIPAL_NAME("principal_name"),
    PRINCIPAL_TYPE("principal_type"),
    PRIVILEGE_ID("privilege_id"),
    TYPE("type"),
    USER_NAME("user_name");
    
    private String propertyKey;
    
    private PrivilegeInfo(String name) {
      this.propertyKey = name;
    }
    
    public String toString() {
      return propertyKey;
    }
  }

  @Inject
  private ExternalServerClient externalServerClient;

  @Inject
  private AuthPropsConfig authPropsConfig;

  /**
   * Authenticating user from external-server using REST call
   * 
   * @param authentication the authentication request object.
   * @return a fully authenticated object including credentials.
   * @throws AuthenticationException if authentication fails.
   */
  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    if (!authPropsConfig.isAuthExternalEnabled()) {
      LOG.debug("external server auth is disabled.");
      return authentication;
    }
    
    String username = authentication.getName();
    String password = (String) authentication.getCredentials();
    if (StringUtils.isBlank(username)) {
      throw new BadCredentialsException("Username can't be null or empty.");
    }
    if (StringUtils.isBlank(password)) {
      throw new BadCredentialsException("Password can't be null or empty.");
    }
    password = StringEscapeUtils.unescapeHtml(password);
    username = StringEscapeUtils.unescapeHtml(username);
    
    try {
      String finalLoginUrl = authPropsConfig.getExternalAuthLoginUrl().replace("$USERNAME", username);
      String responseObj = (String) externalServerClient.sendGETRequest(finalLoginUrl, String.class, username, password);
      if (!isAllowedRole(responseObj)) {
        LOG.error(username + " doesn't have permission");
        throw new BadCredentialsException("Invalid User");
      }
    } catch (Exception e) {
      LOG.error("Login failed for username :" + username + " Error :" + e.getLocalizedMessage());
      throw new BadCredentialsException("Bad credentials");
    }
    authentication = new UsernamePasswordAuthenticationToken(username, password, getAuthorities());
    return authentication;
  }

  /**
   * Return true/false based on PEMISSION NAME return boolean
   */
  private boolean isAllowedRole(String responseJson) {

    List<String> permissionNames = new ArrayList<>();
    JSONUtil.getValuesOfKey(responseJson, PrivilegeInfo.PERMISSION_NAME.toString(), permissionNames);
    List<String> allowedRoleList = authPropsConfig.getAllowedRoles();
    if (permissionNames.isEmpty() || allowedRoleList.size() < 1 || responseJson == null) {
      return false;
    }
    return permissionNames.stream().anyMatch(allowedRoleList::contains);
  }
}
