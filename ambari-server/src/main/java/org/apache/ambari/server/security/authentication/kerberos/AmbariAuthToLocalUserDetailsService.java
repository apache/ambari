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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.security.authorization.AmbariGrantedAuthority;
import org.apache.ambari.server.security.authorization.UserType;
import org.apache.ambari.server.security.authorization.Users;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.security.authentication.util.KerberosName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

  private final List<UserType> userTypeOrder;

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
    List<UserType> orderedUserTypes = null;

    if (configuration != null) {
      AmbariKerberosAuthenticationProperties properties = configuration.getKerberosAuthenticationProperties();

      if (properties != null) {
        authToLocalRules = properties.getAuthToLocalRules();
        orderedUserTypes = properties.getOrderedUserTypes();
      }
    }

    if (StringUtils.isEmpty(authToLocalRules)) {
      authToLocalRules = "DEFAULT";
    }

    if ((orderedUserTypes == null) || orderedUserTypes.isEmpty()) {
      orderedUserTypes = Collections.singletonList(UserType.LDAP);
    }

    KerberosName.setRules(authToLocalRules);

    this.users = users;
    this.userTypeOrder = orderedUserTypes;
  }

  @Override
  public UserDetails loadUserByUsername(String principal) throws UsernameNotFoundException {
    KerberosName kerberosName = new KerberosName(principal);

    try {
      String username = kerberosName.getShortName();

      if (username == null) {
        String message = String.format("Failed to translate %s to a local username during Kerberos authentication.", principal);
        LOG.warn(message);
        throw new UsernameNotFoundException(message);
      }

      LOG.info("Translated {} to {} using auth-to-local rules during Kerberos authentication.", principal, username);
      return createUser(username);
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
   * @param username a username
   * @return the user details of the found user, or <code>null</code> if an appropriate user was not found
   */
  private UserDetails createUser(String username) {
    // Iterate over the ordered user types... when an account for the username/type combination is
    // found, build the related AmbariUserAuthentication instance and return it.  Only the first
    // match matters... this may be an issue and cause some ambiguity in the event multiple user
    // types are specified in the configuration and multiple accounts for the same username, but
    // different types (LOCAL vs LDAP, etc...).
    for (UserType userType : userTypeOrder) {
      org.apache.ambari.server.security.authorization.User user = users.getUser(username, userType);

      if (user != null) {
        Collection<AmbariGrantedAuthority> userAuthorities = users.getUserAuthorities(user.getUserName(), user.getUserType());
        return new User(username, "", userAuthorities);
      }
    }

    String message = String.format("Failed find user account for user with username of %s during Kerberos authentication.", username);
    LOG.warn(message);
    throw new UsernameNotFoundException(message);
  }
}