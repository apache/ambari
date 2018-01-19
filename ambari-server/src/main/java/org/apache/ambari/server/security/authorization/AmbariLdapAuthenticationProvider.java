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

import java.util.List;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.security.ClientSecurityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;

import com.google.inject.Inject;


/**
 * Provides LDAP user authorization logic for Ambari Server
 */
public class AmbariLdapAuthenticationProvider implements AuthenticationProvider {
  static Logger LOG = LoggerFactory.getLogger(AmbariLdapAuthenticationProvider.class); // exposed and mutable for "test"

  Configuration configuration;

  private AmbariLdapAuthoritiesPopulator authoritiesPopulator;
  private UserDAO userDAO;

  private ThreadLocal<LdapServerProperties> ldapServerProperties = new ThreadLocal<>();
  private ThreadLocal<LdapAuthenticationProvider> providerThreadLocal = new ThreadLocal<>();
  private ThreadLocal<String> ldapUserSearchFilterThreadLocal = new ThreadLocal<>();

  @Inject
  public AmbariLdapAuthenticationProvider(Configuration configuration,
                                          AmbariLdapAuthoritiesPopulator authoritiesPopulator, UserDAO userDAO) {
    this.configuration = configuration;
    this.authoritiesPopulator = authoritiesPopulator;
    this.userDAO = userDAO;
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    if (isLdapEnabled()) {
      String username = getUserName(authentication);

      try {
        Authentication auth = loadLdapAuthenticationProvider(username).authenticate(authentication);
        Integer userId = getUserId(auth);

        return new AmbariAuthentication(auth, userId);
      } catch (AuthenticationException e) {
        LOG.debug("Got exception during LDAP authentication attempt", e);
        // Try to help in troubleshooting
        Throwable cause = e.getCause();
        if ((cause != null) && (cause != e)) {
          // Below we check the cause of an AuthenticationException to see what the actual cause is
          // and then send an appropriate message to the caller.
          if (cause instanceof org.springframework.ldap.CommunicationException) {
            if (LOG.isDebugEnabled()) {
              LOG.warn("Failed to communicate with the LDAP server: " + cause.getMessage(), e);
            } else {
              LOG.warn("Failed to communicate with the LDAP server: " + cause.getMessage());
            }
          } else if (cause instanceof org.springframework.ldap.AuthenticationException) {
            LOG.warn("Looks like LDAP manager credentials (that are used for " +
                "connecting to LDAP server) are invalid.", e);
          }
        }
        throw new InvalidUsernamePasswordCombinationException(e);
      } catch (IncorrectResultSizeDataAccessException multipleUsersFound) {
        String message = configuration.isLdapAlternateUserSearchEnabled() ?
          String.format("Login Failed: Please append your domain to your username and try again.  Example: %s@domain", username) :
          "Login Failed: More than one user with that username found, please work with your Ambari Administrator to adjust your LDAP configuration";

        throw new DuplicateLdapUserFoundAuthenticationException(message);
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
  LdapAuthenticationProvider loadLdapAuthenticationProvider(String userName) {
    boolean ldapConfigPropertiesChanged = reloadLdapServerProperties();

    String ldapUserSearchFilter = getLdapUserSearchFilter(userName);

    if (ldapConfigPropertiesChanged|| !ldapUserSearchFilter.equals(ldapUserSearchFilterThreadLocal.get())) {

      LOG.info("Either LDAP Properties or user search filter changed - rebuilding Context");
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
      FilterBasedLdapUserSearch userSearch = new FilterBasedLdapUserSearch(userSearchBase, ldapUserSearchFilter, springSecurityContextSource);

      AmbariLdapBindAuthenticator bindAuthenticator = new AmbariLdapBindAuthenticator(springSecurityContextSource, configuration);
      bindAuthenticator.setUserSearch(userSearch);

      LdapAuthenticationProvider authenticationProvider = new LdapAuthenticationProvider(bindAuthenticator, authoritiesPopulator);
      providerThreadLocal.set(authenticationProvider);
    }

    ldapUserSearchFilterThreadLocal.set(ldapUserSearchFilter);

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
   * Extracts the user name from the passed authentication object.
   * @param authentication
   * @return
   */
  protected String getUserName(Authentication authentication) {
    UsernamePasswordAuthenticationToken userToken = (UsernamePasswordAuthenticationToken)authentication;
    return userToken.getName();
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


  private String getLdapUserSearchFilter(String userName) {
    return ldapServerProperties.get()
      .getUserSearchFilter(configuration.isLdapAlternateUserSearchEnabled() && AmbariLdapUtils.isUserPrincipalNameFormat(userName));
  }

  private Integer getUserId(Authentication authentication) {
    String userName = AuthorizationHelper.resolveLoginAliasToUserName(authentication.getName());

    UserEntity userEntity = userDAO.findLdapUserByName(userName);

    // lookup is case insensitive, so no need for string comparison
    if (userEntity == null) {
      LOG.info("user not found ('{}')", userName);
      throw new InvalidUsernamePasswordCombinationException();
    }

    if (!userEntity.getActive()) {
      LOG.debug("User account is disabled ('{}')", userName);

      throw new InvalidUsernamePasswordCombinationException();
    }

    return userEntity.getUserId();
  }

}
