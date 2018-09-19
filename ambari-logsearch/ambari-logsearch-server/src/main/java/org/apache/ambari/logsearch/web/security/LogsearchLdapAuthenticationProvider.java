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
import org.apache.ambari.logsearch.dao.RoleDao;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.LdapAuthenticator;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;

import javax.inject.Inject;
import java.util.Collection;

public class LogsearchLdapAuthenticationProvider extends LdapAuthenticationProvider {

  private static final Logger LOG = LoggerFactory.getLogger(LogsearchLdapAuthenticationProvider.class);

  @Inject
  private AuthPropsConfig authPropsConfig;

  public LogsearchLdapAuthenticationProvider(LdapAuthenticator bindAuthenticator, LdapAuthoritiesPopulator ldapAuthoritiesPopulator) {
    super(bindAuthenticator, ldapAuthoritiesPopulator);
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    if (!authPropsConfig.isAuthLdapEnabled()) {
      LOG.debug("LDAP auth is disabled.");
      return authentication;
    }
    authentication = super.authenticate(authentication);
    final Collection<? extends GrantedAuthority> authorities;
    if (StringUtils.isBlank(authPropsConfig.getLdapAuthConfig().getLdapGroupSearchBase())) {
      authorities = RoleDao.createDefaultAuthorities();
    } else {
      authorities = authentication.getAuthorities();
    }

    authentication = new UsernamePasswordAuthenticationToken(authentication.getPrincipal(), authentication.getCredentials(), authorities);
    return authentication;
  }

}
