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
public abstract class AmbariAuthenticationProvider implements AuthenticationProvider {
  private static final Logger LOG = LoggerFactory.getLogger(AmbariAuthenticationProvider.class);

  /**
   * Helper object to provide logic for working with users.
   */
  private final Users users;

  private final Configuration configuration;

  protected AmbariAuthenticationProvider(Users users, Configuration configuration) {
    this.users = users;
    this.configuration = configuration;
  }

  /**
   * Validates the user account such that the user is allowed to log in.
   *
   * @param userEntity the user entity
   * @param userName   the Ambari username
   */
  protected void validateLogin(UserEntity userEntity, String userName) {
    if (userEntity == null) {
      LOG.info("User not found");
      throw new UserNotFoundException(userName);
    } else {
      if (!userEntity.getActive()) {
        LOG.info("User account is disabled: {}", userName);
        throw new AccountDisabledException(userName);
      }

      int maxConsecutiveFailures = configuration.getMaxAuthenticationFailures();
      if (maxConsecutiveFailures > 0 && userEntity.getConsecutiveFailures() >= maxConsecutiveFailures) {
        LOG.info("User account is locked out due to too many authentication failures ({}/{}): {}",
            userEntity.getConsecutiveFailures(), maxConsecutiveFailures, userName);
        throw new TooManyLoginFailuresException(userName);
      }
    }
  }

  /**
   * Finds the specific {@link UserAuthenticationEntity} from the collection of authentication methods
   * available to the specified {@link UserEntity}.
   *
   * @param userEntity a {@link UserEntity}
   * @param type       the {@link UserAuthenticationType} to retrieve
   * @return a {@link UserAuthenticationEntity} if found; otherwise null
   */
  protected UserAuthenticationEntity getAuthenticationEntity(UserEntity userEntity, UserAuthenticationType type) {
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

  protected Users getUsers() {
    return users;
  }

  protected Configuration getConfiguration() {
    return configuration;
  }
}
