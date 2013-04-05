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
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.security.ClientSecurityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;


/**
 * Provides LDAP user authorization logic for Ambari Server
 */
public class AmbariLdapAuthenticationProvider implements AuthenticationProvider {
  private static final Logger log = LoggerFactory.getLogger(AmbariLdapAuthenticationProvider.class);

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

      return loadLdapAuthenticationProvider().authenticate(authentication);

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
  private LdapAuthenticationProvider loadLdapAuthenticationProvider() {
    if (reloadLdapServerProperties()) {
      log.info("LDAP Properties changed - rebuilding Context");
      DefaultSpringSecurityContextSource springSecurityContextSource =
              new DefaultSpringSecurityContextSource(ldapServerProperties.get().getLdapUrls(), ldapServerProperties.get().getBaseDN());

      if (!ldapServerProperties.get().isAnonymousBind()) {
        springSecurityContextSource.setUserDn(ldapServerProperties.get().getManagerDn());
        springSecurityContextSource.setPassword(ldapServerProperties.get().getManagerPassword());
      }

      try {
        springSecurityContextSource.afterPropertiesSet();
      } catch (Exception e) {
        log.error("LDAP Context Source not loaded ", e);
        throw new UsernameNotFoundException("LDAP Context Source not loaded", e);
      }

      //TODO change properties
      String userSearchBase = ldapServerProperties.get().getUserSearchBase();
      String userSearchFilter = ldapServerProperties.get().getUserSearchFilter();

      FilterBasedLdapUserSearch userSearch = new FilterBasedLdapUserSearch(userSearchBase, userSearchFilter, springSecurityContextSource);

      BindAuthenticator bindAuthenticator = new BindAuthenticator(springSecurityContextSource);
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
  private boolean isLdapEnabled() {
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
      log.info("Reloading properties");
      ldapServerProperties.set(properties);
      return true;
    }
    return false;
  }
}
