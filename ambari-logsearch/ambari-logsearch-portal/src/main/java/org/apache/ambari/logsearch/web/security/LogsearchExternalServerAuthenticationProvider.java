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

import javax.annotation.PostConstruct;

import org.apache.ambari.logsearch.util.ExternalServerClient;
import org.apache.ambari.logsearch.util.PropertiesUtil;
import org.apache.ambari.logsearch.util.StringUtil;
import org.apache.commons.lang.StringEscapeUtils;
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

  @Autowired
  ExternalServerClient externalServerClient;

  @Autowired
  StringUtil stringUtil;

  private String loginAPIURL = "/api/v1/clusters";// default

  @PostConstruct
  public void initialization() {
    loginAPIURL = PropertiesUtil.getProperty(AUTH_METHOD_PROP_START_WITH
        + "external_auth.login_url", loginAPIURL);
  }

  /**
   * Authenticating user from external-server using REST call 
   * @param authentication the authentication request object.
   * @return a fully authenticated object including credentials.
   * @throws AuthenticationException if authentication fails.
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
    if (stringUtil.isEmpty(username)) {
      throw new BadCredentialsException("Username can't be null or empty.");
    }
    if (stringUtil.isEmpty(password)) {
      throw new BadCredentialsException("Password can't be null or empty.");
    }
    // html unescape
    password = StringEscapeUtils.unescapeHtml(password);
    username = StringEscapeUtils.unescapeHtml(username);
    try {
      externalServerClient.sendGETRequest(loginAPIURL, String.class, null,
          username, password);
    } catch (Exception e) {
      LOG.error("Login failed for username :" + username + " Error :"+ e.getLocalizedMessage());
      throw new BadCredentialsException("Bad credentials");
    }
    authentication = new UsernamePasswordAuthenticationToken(username,
        password, getAuthorities(username));
    return authentication;
  }

  /**
   * Return true/false based on EXTERNAL_AUTH authentication method is enabled/disabled
   * return boolean
   */
  @Override
  public boolean isEnable() {
    return isEnable(AUTH_METHOD.EXTERNAL_AUTH);
  }
}
