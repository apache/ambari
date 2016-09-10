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

import org.apache.ambari.logsearch.conf.AuthPropsConfig;
import org.apache.ambari.logsearch.web.model.User;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class LogsearchSimpleAuthenticationProvider extends LogsearchAbstractAuthenticationProvider {

  private static Logger logger = Logger.getLogger(LogsearchSimpleAuthenticationProvider.class);

  @Inject
  private AuthPropsConfig authPropsConfig;

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    if (!authPropsConfig.isAuthSimpleEnabled()) {
      logger.debug("Simple auth is disabled");
      return authentication;
    }
    String username = authentication.getName();
    String password = (String) authentication.getCredentials();
    username = StringEscapeUtils.unescapeHtml(username);
    if (StringUtils.isBlank(username)) {
      throw new BadCredentialsException("Username can't be null or empty.");
    }
    User user = new User();
    user.setUsername(username);
    authentication = new UsernamePasswordAuthenticationToken(username, password, getAuthorities(username));
    return authentication;
  }

  @Override
  public boolean isEnable(AUTH_METHOD method) {
    boolean ldapEnabled = super.isEnable(AUTH_METHOD.LDAP);
    boolean fileEnabled = super.isEnable(AUTH_METHOD.FILE);
    boolean externalAuthEnabled = super.isEnable(AUTH_METHOD.EXTERNAL_AUTH);
    boolean simpleEnabled = super.isEnable(method);
    if (!ldapEnabled && !fileEnabled && simpleEnabled && !externalAuthEnabled) {
      // simple is enabled only when rest three are disabled and simple is enable
      return true;
    } else {
      return false;
    }
  }
}
