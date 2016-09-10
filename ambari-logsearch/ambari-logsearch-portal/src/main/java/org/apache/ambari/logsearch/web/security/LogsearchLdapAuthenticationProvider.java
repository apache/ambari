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

import java.util.List;

import org.apache.ambari.logsearch.conf.AuthPropsConfig;
import org.apache.log4j.Logger;
import org.springframework.ldap.CommunicationException;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

@Component
public class LogsearchLdapAuthenticationProvider extends
  LogsearchAbstractAuthenticationProvider {

  private static Logger logger = Logger
    .getLogger(LogsearchLdapAuthenticationProvider.class);

  private static LdapProperties ldapServerProperties = null;
  private static LdapAuthenticationProvider ldapAuthProvider = null;
  private String logStatement = "";

  @Inject
  private AuthPropsConfig authPropsConfig;

  public LogsearchLdapAuthenticationProvider() {
  }

  @PostConstruct
  public void postConstruct() {
    logger.debug("Creating object of ldap auth provider ");
    if (authPropsConfig.isAuthLdapEnabled()) {
      ldapAuthProvider = loadLdapAuthenticationProvider();
    } else {
      logger.info("Ldap auth is disabled");
    }
  }

  @Override
  public Authentication authenticate(Authentication authentication)
    throws AuthenticationException {
    if (!authPropsConfig.isAuthLdapEnabled()) {
      logger.debug("Ldap auth is disabled");
      return authentication;
    }
    try {
      LdapAuthenticationProvider authProvider = loadLdapAuthenticationProvider();
      if (authProvider != null) {
        return authProvider.authenticate(authentication);
      } else {
        return authentication;
      }
    } catch (AuthenticationException e) {
      logger.info("Got exception during LDAP authentication attempt", e);
      // Try to help in troubleshooting
      Throwable cause = e.getCause();
      if (cause != null) {
        if ((cause != e)
          && (cause instanceof org.springframework.ldap.AuthenticationException)) {
          logger.warn(
            "Looks like LDAP manager credentials (that are used for "
              + "connecting to LDAP server) are invalid.",
            e);
        }
      }
    } catch (CommunicationException e) {
      logger.error(e);
    } catch (Exception e) {
      logger.error(e, e.getCause());
    }
    if (authentication != null && !authentication.isAuthenticated()) {
      logger.warn("Ldap authentication failed. username="
        + authentication.getName() + ", details="
        + authentication.getDetails());
      throw new BadCredentialsException("Invalid credentials!!");
    }
    return authentication;
  }

  /**
   * Reloads LDAP Context Source and depending objects if properties were
   * changed
   *
   * @return corresponding LDAP authentication provider
   */
  private LdapAuthenticationProvider loadLdapAuthenticationProvider() {
    if (reloadLdapServerProperties()) {
      logger.info("LDAP Properties changed - rebuilding Context");
      LdapContextSource springSecurityContextSource = new LdapContextSource();
      List<String> ldapUrls = ldapServerProperties.getLdapUrls();
      logStatement = "ldapUrls=" + ldapUrls;
      if (ldapUrls == null || ldapUrls.size() == 0) {
        logger.info("LDAP URL is empty. So won't initialize LDAP provider");
        return null;
      }

      springSecurityContextSource.setUrls(ldapUrls
        .toArray(new String[ldapUrls.size()]));
      springSecurityContextSource.setBase(ldapServerProperties
        .getBaseDN());
      logStatement = logStatement + ", baseDN="
        + ldapServerProperties.getBaseDN();

      if (!ldapServerProperties.isAnonymousBind()) {
        springSecurityContextSource.setUserDn(ldapServerProperties
          .getManagerDn());
        logStatement = logStatement + ", managerDN="
          + ldapServerProperties.getManagerDn();
        springSecurityContextSource.setPassword(ldapServerProperties
          .getManagerPassword());
      }

      try {
        springSecurityContextSource.afterPropertiesSet();
      } catch (Exception e) {
        logger.error("LDAP Context Source not loaded ", e);
        throw new UsernameNotFoundException(
          "LDAP Context Source not loaded. ldapDetails="
            + logStatement, e);
      }

      String userSearchBase = ldapServerProperties.getUserSearchBase();
      logStatement = logStatement + ", userSearchBase=" + userSearchBase;
      String userSearchFilter = ldapServerProperties
        .getUserSearchFilter();
      logStatement = logStatement + ", userSearchFilter="
        + userSearchFilter;

      logger.info("LDAP properties=" + logStatement);
      FilterBasedLdapUserSearch userSearch = new FilterBasedLdapUserSearch(
        userSearchBase, userSearchFilter,
        springSecurityContextSource);

      LogsearchLdapBindAuthenticator bindAuthenticator = new LogsearchLdapBindAuthenticator(
        springSecurityContextSource, ldapServerProperties);
      bindAuthenticator.setUserSearch(userSearch);

      LdapAuthenticationProvider authenticationProvider = new LdapAuthenticationProvider(
        bindAuthenticator);
      ldapAuthProvider = authenticationProvider;

    }
    return ldapAuthProvider;
  }

  /**
   * Reloads LDAP Server properties from configuration
   *
   * @return true if properties were reloaded
   */
  private boolean reloadLdapServerProperties() {
    LdapProperties properties = LdapUtil.loadLdapProperties();
    if (!properties.equals(ldapServerProperties)) {
      logger.info("Reloading properties");
      ldapServerProperties = properties;
      return true;
    }
    return false;
  }

}
