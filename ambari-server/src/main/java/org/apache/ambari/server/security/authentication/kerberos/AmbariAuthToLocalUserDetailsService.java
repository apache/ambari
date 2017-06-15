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

package org.apache.ambari.server.security.authentication.kerberos;

import java.io.IOException;
import java.util.List;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.entities.UserAuthenticationEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.security.authentication.AuthenticationMethodNotAllowedException;
import org.apache.ambari.server.security.authentication.UserNotFoundException;
import org.apache.ambari.server.security.authorization.UserAuthenticationType;
import org.apache.ambari.server.security.authorization.Users;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.security.authentication.util.KerberosName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * AmbariAuthToLocalUserDetailsService is a {@link UserDetailsService} that translates
 * a Kerberos principal name into a local username that may be used when looking up
 * and Ambari user account.
 */
public class AmbariAuthToLocalUserDetailsService implements UserDetailsService {
  private static final Logger LOG = LoggerFactory.getLogger(AmbariAuthToLocalUserDetailsService.class);

  private final Users users;

  private final String authToLocalRules;

  /**
   * Constructor.
   * <p>
   * Given the Ambari {@link Configuration}, initializes the {@link KerberosName} class using
   * the <code>auth-to-local</code> rules from {@link AmbariKerberosAuthenticationProperties#getAuthToLocalRules()}.
   *
   * @param configuration the Ambari configuration data
   * @param users         the Ambari users access object
   * @throws AmbariException if an error occurs parsing the user-provided auth-to-local rules
   */
  public AmbariAuthToLocalUserDetailsService(Configuration configuration, Users users) throws AmbariException {
    String authToLocalRules = null;

    if (configuration != null) {
      AmbariKerberosAuthenticationProperties properties = configuration.getKerberosAuthenticationProperties();

      if (properties != null) {
        authToLocalRules = properties.getAuthToLocalRules();
      }
    }

    if (StringUtils.isEmpty(authToLocalRules)) {
      authToLocalRules = "DEFAULT";
    }

    this.users = users;
    this.authToLocalRules = authToLocalRules;
  }

  @Override
  public UserDetails loadUserByUsername(String principal) throws UsernameNotFoundException {
    try {
      String username;

      // Since KerberosName relies on a static variable to hold on to the auth-to-local rules, attempt
      // to protect access to the rule set by blocking other threads from chaning the rules out from
      // under us during this operation.  Similar logic is used in org.apache.ambari.server.view.ViewContextImpl.getUsername().
      synchronized (KerberosName.class) {
        KerberosName.setRules(authToLocalRules);
        username = new KerberosName(principal).getShortName();
      }

      if (username == null) {
        String message = String.format("Failed to translate %s to a local username during Kerberos authentication.", principal);
        LOG.warn(message);
        throw new UsernameNotFoundException(message);
      }

      LOG.info("Translated {} to {} using auth-to-local rules during Kerberos authentication.", principal, username);
      return createUser(username, principal);
    } catch (UserNotFoundException e) {
      throw new UsernameNotFoundException(e.getMessage(), e);
    } catch (IOException e) {
      String message = String.format("Failed to translate %s to a local username during Kerberos authentication: %s", principal, e.getLocalizedMessage());
      LOG.warn(message);
      throw new UsernameNotFoundException(message, e);
    }
  }

  /**
   * Given a username, finds an appropriate account in the Ambari database.
   * <p>
   * User accounts are searched in order of preferred user type as specified in the Ambari configuration
   * ({@link Configuration#KERBEROS_AUTH_USER_TYPES}).
   *
   * @param username  a username
   * @param principal the user's principal
   * @return the user details of the found user, or <code>null</code> if an appropriate user was not found
   */
  private UserDetails createUser(String username, String principal) throws AuthenticationException {
    UserEntity userEntity = users.getUserEntity(username);

    if (userEntity == null) {
      throw new UserNotFoundException(username, String.format("Cannot find user using Kerberos ticket (%s).", principal));
    } else if (!userEntity.getActive()) {
      LOG.debug("User account is disabled");
      throw new UserNotFoundException(username, "User account is disabled");
    } else {

      // Check to see if the user is allowed to authenticate using KERBEROS or LDAP
      List<UserAuthenticationEntity> authenticationEntities = userEntity.getAuthenticationEntities();
      boolean hasKerberos = false;
      boolean hasLDAP = false;
      boolean hasLocal = false;

      for (UserAuthenticationEntity entity : authenticationEntities) {
        UserAuthenticationType authenticationType = entity.getAuthenticationType();

        switch (authenticationType) {
          case KERBEROS:
            if (principal.equalsIgnoreCase(entity.getAuthenticationKey())) {
              LOG.trace("Found KERBEROS authentication method for {} using principal {}", username, principal);
              hasKerberos = true;
            }
            break;

          case LDAP:
            hasLDAP = true;
            break;

          case LOCAL:
            hasLocal = true;
            break;

          default:
            break;
        }

        if (hasKerberos) {
          break;
        }
      }

      if (!hasKerberos) {
        if (hasLDAP) {
          // TODO: Determine if LDAP users can authenticate using Kerberos
          try {
            users.addKerberosAuthentication(userEntity, principal);
            LOG.trace("Added KERBEROS authentication method for {} using principal {}", username, principal);
          } catch (AmbariException e) {
            LOG.error(String.format("Failed to add the KERBEROS authentication method for %s: %s", principal, e.getLocalizedMessage()), e);
          }
          hasKerberos = true;
        }

        if (!hasKerberos && hasLocal) {
          // TODO: Determine if LOCAL users can authenticate using Kerberos
          try {
            users.addKerberosAuthentication(userEntity, username);
            LOG.trace("Added KERBEROS authentication method for {} using principal {}", username, principal);
          } catch (AmbariException e) {
            LOG.error(String.format("Failed to add the KERBEROS authentication method for %s: %s", username, e.getLocalizedMessage()), e);
          }
          hasKerberos = true;
        }
      }

      if (!hasKerberos) {
        throw new AuthenticationMethodNotAllowedException(username, UserAuthenticationType.KERBEROS);
      }
    }

    return new User(username, "", users.getUserAuthorities(userEntity));
  }
}