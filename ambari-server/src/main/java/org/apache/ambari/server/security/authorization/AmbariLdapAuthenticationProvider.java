/**
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

import com.google.inject.Inject;
import java.util.List;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.security.ClientSecurityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;


/**
 * Provides LDAP user authorization logic for Ambari Server
 */
public class AmbariLdapAuthenticationProvider implements AuthenticationProvider {
  Logger LOG = LoggerFactory.getLogger(AmbariLdapAuthenticationProvider.class);

  Configuration configuration;

  private AmbariLdapAuthoritiesPopulator authoritiesPopulator;

  private ThreadLocal<LdapServerProperties> ldapServerProperties = new ThreadLocal<LdapServerProperties>();
  private ThreadLocal<LdapAuthenticationProvider> providerThreadLocal = new ThreadLocal<LdapAuthenticationProvider>();

  @Inject
  public AmbariLdapAuthenticationProvider(Configuration configuration, AmbariLdapAuthoritiesPopulator authoritiesPopulator) {
    this.configuration = configuration;
    this.authoritiesPopulator = authoritiesPopulator;
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {

    if (isLdapEnabled()) {
      try {
        return loadLdapAuthenticationProvider().authenticate(authentication);
      } catch (AuthenticationException e) {
        LOG.debug("Got exception during LDAP authentification attempt", e);
        // Try to help in troubleshooting
        Throwable cause = e.getCause();
        if (cause != null) {
          // Below we check the cause of an AuthenticationException . If it is
          // caused by another AuthenticationException, than probably
          // the problem is with LDAP ManagerDN/password
          if ((cause != e) && (cause instanceof
                  org.springframework.ldap.AuthenticationException)) {
            LOG.warn("Looks like LDAP manager credentials (that are used for " +
                    "connecting to LDAP server) are invalid.", e);
          }
        }
        throw e;
      }
    } else {
      return null;
    }

  }

  @Override
  public boolean supports(Class<?> authentication) {
    return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
  }

  /**
   * Reloads LDAP Context Source and depending objects if properties were changed
   * @return corresponding LDAP authentication provider
   */
  LdapAuthenticationProvider loadLdapAuthenticationProvider() {
    if (reloadLdapServerProperties()) {
      LOG.info("LDAP Properties changed - rebuilding Context");
      LdapContextSource springSecurityContextSource = new LdapContextSource();
      List<String> ldapUrls = ldapServerProperties.get().getLdapUrls();
      springSecurityContextSource.setUrls(ldapUrls.toArray(new String[ldapUrls.size()]));
      springSecurityContextSource.setBase(ldapServerProperties.get().getBaseDN());

      if (!ldapServerProperties.get().isAnonymousBind()) {
        springSecurityContextSource.setUserDn(ldapServerProperties.get().getManagerDn());
        springSecurityContextSource.setPassword(ldapServerProperties.get().getManagerPassword());
      }

      try {
        springSecurityContextSource.afterPropertiesSet();
      } catch (Exception e) {
        LOG.error("LDAP Context Source not loaded ", e);
        throw new UsernameNotFoundException("LDAP Context Source not loaded", e);
      }

      //TODO change properties
      String userSearchBase = ldapServerProperties.get().getUserSearchBase();
      String userSearchFilter = ldapServerProperties.get().getUserSearchFilter();

      FilterBasedLdapUserSearch userSearch = new FilterBasedLdapUserSearch(userSearchBase, userSearchFilter, springSecurityContextSource);

      AmbariLdapBindAuthenticator bindAuthenticator = new AmbariLdapBindAuthenticator(springSecurityContextSource, configuration);
      bindAuthenticator.setUserSearch(userSearch);

      LdapAuthenticationProvider authenticationProvider = new LdapAuthenticationProvider(bindAuthenticator, authoritiesPopulator);

      providerThreadLocal.set(authenticationProvider);
    }

    return providerThreadLocal.get();
  }


  /**
   * Check if LDAP authentication is enabled in server properties
   * @return true if enabled
   */
  boolean isLdapEnabled() {
    return configuration.getClientSecurityType() == ClientSecurityType.LDAP;
  }

  /**
   * Reloads LDAP Server properties from configuration
   *
   * @return true if properties were reloaded
   */
  private boolean reloadLdapServerProperties() {
    LdapServerProperties properties = configuration.getLdapServerProperties();
    if (!properties.equals(ldapServerProperties.get())) {
      LOG.info("Reloading properties");
      ldapServerProperties.set(properties);
      return true;
    }
    return false;
  }
}
