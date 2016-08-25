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

import javax.annotation.PostConstruct;

import org.apache.ambari.logsearch.common.ExternalServerClient;
import org.apache.ambari.logsearch.common.PropertiesHelper;
import org.apache.ambari.logsearch.util.JSONUtil;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

/**
 * 
 * Authentication provider to authenticate user from external-server using REST
 * call
 */
@Component
public class LogsearchExternalServerAuthenticationProvider extends
    LogsearchAbstractAuthenticationProvider {

  private static Logger LOG = Logger
      .getLogger(LogsearchExternalServerAuthenticationProvider.class);

  public final static String ALLOWED_ROLE_PROP = "logsearch.roles.allowed";

  public static enum PRIVILEGE_INFO {
    PERMISSION_LABEL {
      @Override
      public String toString() {
        return "permission_label";
      }
    },
    PERMISSION_NAME {
      @Override
      public String toString() {
        return "permission_name";
      }
    },
    PRINCIPAL_NAME {
      @Override
      public String toString() {
        return "principal_name";
      }
    },
    PRINCIPAL_TYPE {
      @Override
      public String toString() {
        return "principal_type";
      }
    },
    PRIVILEGE_ID {
      @Override
      public String toString() {
        return "privilege_id";
      }
    },
    TYPE {
      @Override
      public String toString() {
        return "type";
      }
    },
    USER_NAME {
      @Override
      public String toString() {
        return "user_name";
      }
    };
  }

  @Autowired
  ExternalServerClient externalServerClient;

  private String loginAPIURL = "/api/v1/users/$USERNAME/privileges?fields=*";// default

  @PostConstruct
  public void initialization() {
    loginAPIURL = PropertiesHelper.getProperty(AUTH_METHOD_PROP_START_WITH
        + "external_auth.login_url", loginAPIURL);
  }

  /**
   * Authenticating user from external-server using REST call
   * 
   * @param authentication
   *          the authentication request object.
   * @return a fully authenticated object including credentials.
   * @throws AuthenticationException
   *           if authentication fails.
   */
  @Override
  public Authentication authenticate(Authentication authentication)
      throws AuthenticationException {
    if (!this.isEnable()) {
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
    // html unescape
    password = StringEscapeUtils.unescapeHtml(password);
    username = StringEscapeUtils.unescapeHtml(username);
    try {
      String finalLoginUrl = loginAPIURL.replace("$USERNAME", username);
      String responseObj = (String) externalServerClient.sendGETRequest(
          finalLoginUrl, String.class, null, username, password);
      if (!isAllowedRole(responseObj)) {
        LOG.error(username + " does'nt have permission");
        throw new BadCredentialsException("Invalid User");
      }

    } catch (Exception e) {
      LOG.error("Login failed for username :" + username + " Error :"
          + e.getLocalizedMessage());
      throw new BadCredentialsException("Bad credentials");
    }
    authentication = new UsernamePasswordAuthenticationToken(username,
        password, getAuthorities(username));
    return authentication;
  }

  /**
   * Return true/false based on PEMISSION NAME return boolean
   */
  @SuppressWarnings("static-access")
  private boolean isAllowedRole(String responseJson) {
    String allowedRoleList[] = PropertiesHelper
        .getPropertyStringList(ALLOWED_ROLE_PROP);

    List<String> values = new ArrayList<String>();
    JSONUtil.getValuesOfKey(responseJson,
        PRIVILEGE_INFO.PERMISSION_NAME.toString(), values);
    if (values.isEmpty())
      return true;
    
    if (allowedRoleList.length > 0 && responseJson != null) {
      for (String allowedRole : allowedRoleList) {
        for (String role : values) {
          if (role.equals(allowedRole)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * Return true/false based on EXTERNAL_AUTH authentication method is
   * enabled/disabled return boolean
   */
  @Override
  public boolean isEnable() {
    return isEnable(AUTH_METHOD.EXTERNAL_AUTH);
  }
}
