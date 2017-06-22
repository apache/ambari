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

import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.entities.UserAuthenticationEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.security.authentication.InvalidUsernamePasswordCombinationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.google.inject.Inject;

public class AmbariLocalUserProvider extends AbstractUserDetailsAuthenticationProvider {
  private static final Logger LOG = LoggerFactory.getLogger(AmbariLocalUserProvider.class);

  private UserDAO userDAO;
  private Users users;
  private PasswordEncoder passwordEncoder;


  @Inject
  public AmbariLocalUserProvider(UserDAO userDAO, Users users, PasswordEncoder passwordEncoder) {
    this.userDAO = userDAO;
    this.users = users;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  protected void additionalAuthenticationChecks(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
    // do nothing
  }

  // TODO: ************
  // TODO: This is to be revisited for AMBARI-21220 (Update Local Authentication process to work with improved user management facility)
  // TODO: ************
  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    String userName = authentication.getName().trim();

    LOG.info("Loading user by name: " + userName);

    UserEntity userEntity = userDAO.findUserByName(userName);

    if (userEntity == null) {
      LOG.info("user not found");
      throw new InvalidUsernamePasswordCombinationException(userName);
    }

    if (!userEntity.getActive()) {
      LOG.debug("User account is disabled");
      throw new InvalidUsernamePasswordCombinationException(userName);
    }

    if (authentication.getCredentials() == null) {
      LOG.debug("Authentication failed: no credentials provided");
      throw new InvalidUsernamePasswordCombinationException(userName);
    }

    List<UserAuthenticationEntity> authenticationEntities = userEntity.getAuthenticationEntities();
    for (UserAuthenticationEntity authenticationEntity : authenticationEntities) {
      if (authenticationEntity.getAuthenticationType() == UserAuthenticationType.LOCAL) {
        // This should only get invoked once...
        String password = authenticationEntity.getAuthenticationKey();
        String presentedPassword = authentication.getCredentials().toString();

        if (passwordEncoder.matches(presentedPassword, password)) {
          // The user was  authenticated, return the authenticated user object
          User user = new User(userEntity);
          Authentication auth = new AmbariUserAuthentication(password, user, users.getUserAuthorities(userEntity));
          auth.setAuthenticated(true);
          return auth;
        }
      }
    }

    // The user was not authenticated, fail
    LOG.debug("Authentication failed: password does not match stored value");
    throw new InvalidUsernamePasswordCombinationException(userName);
  }

  @Override
  protected UserDetails retrieveUser(String username, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
    return null;
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
  }
}
