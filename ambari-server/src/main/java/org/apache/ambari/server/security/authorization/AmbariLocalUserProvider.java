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

import java.util.Collection;

import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.entities.UserEntity;
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

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    String userName = authentication.getName().trim();

    LOG.info("Loading user by name: " + userName);

    UserEntity userEntity = userDAO.findLocalUserByName(userName);

    if (userEntity == null) {
      //TODO case insensitive name comparison is a temporary solution, until users API will change to use id as PK
      LOG.info("user not found");
      throw new InvalidUsernamePasswordCombinationException();
    }

    if (!userEntity.getActive()) {
      logger.debug("User account is disabled");

      throw new InvalidUsernamePasswordCombinationException();
    }

    if (authentication.getCredentials() == null) {
      logger.debug("Authentication failed: no credentials provided");

      throw new InvalidUsernamePasswordCombinationException();
    }

    String password = userEntity.getUserPassword();
    String presentedPassword = authentication.getCredentials().toString();

    if (!passwordEncoder.matches(presentedPassword, password)) {
      logger.debug("Authentication failed: password does not match stored value");

      throw new InvalidUsernamePasswordCombinationException();
    }
    Collection<AmbariGrantedAuthority> userAuthorities =
      users.getUserAuthorities(userEntity.getUserName(), userEntity.getUserType());

    User user = new User(userEntity);
    Authentication auth = new AmbariUserAuthentication(userEntity.getUserPassword(), user, userAuthorities);
    auth.setAuthenticated(true);
    return auth;
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
