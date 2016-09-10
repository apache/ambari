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

import java.util.Collection;

import org.apache.ambari.logsearch.conf.AuthPropsConfig;
import org.apache.ambari.logsearch.util.CommonUtil;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class LogsearchFileAuthenticationProvider extends LogsearchAbstractAuthenticationProvider {

  private static Logger logger = Logger.getLogger(LogsearchFileAuthenticationProvider.class);

  @Inject
  private AuthPropsConfig authPropsConfig;

  @Inject
  private UserDetailsService userDetailsService;

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    if (!authPropsConfig.isAuthFileEnabled()) {
      logger.debug("File auth is disabled.");
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

    UserDetails user = userDetailsService.loadUserByUsername(username);
    if (user == null) {
      logger.error("Username not found.");
      throw new BadCredentialsException("User not found.");
    }
    if (password == null || password.isEmpty()) {
      logger.error("Password can't be null or empty.");
      throw new BadCredentialsException("Password can't be null or empty.");
    }

    String encPassword = CommonUtil.encryptPassword(username, password);
    if (!encPassword.equals(user.getPassword())) {
      logger.error("Wrong password for user=" + username);
      throw new BadCredentialsException("Wrong password");
    }
    Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
    authentication = new UsernamePasswordAuthenticationToken(username, encPassword, authorities);
    return authentication;
  }
}
