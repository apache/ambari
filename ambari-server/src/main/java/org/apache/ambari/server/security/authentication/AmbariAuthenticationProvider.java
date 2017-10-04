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

package org.apache.ambari.server.security.authentication;

import java.util.Collection;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.entities.UserAuthenticationEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.security.authorization.UserAuthenticationType;
import org.apache.ambari.server.security.authorization.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;

/**
 * AmbariAuthenticationProvider is an abstract class to be extended by Ambari authentication providers.
 * <p>
 * This class contains common methods that may be used by authentication providers.
 */
abstract class AmbariAuthenticationProvider implements AuthenticationProvider {
  private static final Logger LOG = LoggerFactory.getLogger(AmbariAuthenticationProvider.class);

  private Users users;
  private Configuration configuration;

  AmbariAuthenticationProvider(Users users, Configuration configuration) {
    this.users = users;
    this.configuration = configuration;
  }

  /**
   * Gets the {@link UserEntity} for the user with the specified username.
   * <p>
   * The entity is validated such that the account is allowed to log in before returning. For example,
   * if the account is not acitve, no user may not login as that account.
   *
   * @param userName
   * @return
   */
  UserEntity getUserEntity(String userName) {
    LOG.debug("Loading user by name: {}", userName);
    UserEntity userEntity = users.getUserEntity(userName);

    if (userEntity == null) {
      LOG.info("User not found: {}", userName);
      throw new InvalidUsernamePasswordCombinationException(userName);
    }

    if (!userEntity.getActive()) {
      LOG.info("User account is disabled: {}", userName);
      if (configuration.showLockedOutUserMessage()) {
        throw new AccountDisabledException(userName);
      } else {
        throw new InvalidUsernamePasswordCombinationException(userName);
      }
    }

    return userEntity;
  }

  /**
   * Finds the specific {@link UserAuthenticationEntity} from the collection of authentication methods
   * available to the specified {@link UserEntity}.
   *
   * @param userEntity a {@link UserEntity}
   * @param type       the {@link UserAuthenticationType} to retrieve
   * @return a {@link UserAuthenticationEntity} if found; otherwise null
   */
  UserAuthenticationEntity getAuthenticationEntity(UserEntity userEntity, UserAuthenticationType type) {
    Collection<UserAuthenticationEntity> authenticationEntities = (userEntity == null) ? null : userEntity.getAuthenticationEntities();
    if (authenticationEntities != null) {
      for (UserAuthenticationEntity authenticationEntity : authenticationEntities) {
        if (authenticationEntity.getAuthenticationType() == type) {
          return authenticationEntity;
        }
      }
    }

    return null;
  }

}
