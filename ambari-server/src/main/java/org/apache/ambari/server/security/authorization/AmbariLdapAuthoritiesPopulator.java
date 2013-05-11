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
import com.google.inject.persist.Transactional;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.dao.RoleDAO;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.entities.RoleEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;

import java.util.Collection;

/**
 * Provides authorities population for LDAP user from local DB
 */
public class AmbariLdapAuthoritiesPopulator implements LdapAuthoritiesPopulator {
  private static final Logger log = LoggerFactory.getLogger(AmbariLdapAuthoritiesPopulator.class);

  Configuration configuration;
  private AuthorizationHelper authorizationHelper;
  UserDAO userDAO;
  RoleDAO roleDAO;

  @Inject
  public AmbariLdapAuthoritiesPopulator(Configuration configuration, AuthorizationHelper authorizationHelper,
                                        UserDAO userDAO, RoleDAO roleDAO) {
    this.configuration = configuration;
    this.authorizationHelper = authorizationHelper;
    this.userDAO = userDAO;
    this.roleDAO = roleDAO;
  }

  @Override
  @Transactional
  public Collection<? extends GrantedAuthority> getGrantedAuthorities(DirContextOperations userData, String username) {
    log.info("Get roles for user " + username + " from local DB");

    UserEntity user = null;

    user = userDAO.findLdapUserByName(username);

    if (user == null) {
      log.info("User " + username + " not present in local DB - creating");

      UserEntity newUser = new UserEntity();
      newUser.setLdapUser(true);
      newUser.setUserName(username);

      String roleName = (configuration.getConfigsMap().get(Configuration.USER_ROLE_NAME_KEY));
      log.info("Using default role name " + roleName);

      RoleEntity role = roleDAO.findByName(roleName);

      if (role == null) {
        log.info("Role " + roleName + " not present in local DB - creating");
        role = new RoleEntity();
        role.setRoleName(roleName);
        roleDAO.create(role);
        role = roleDAO.findByName(role.getRoleName());
      }

      userDAO.create(newUser);

      user = userDAO.findLdapUserByName(newUser.getUserName());

      user.getRoleEntities().add(role);
      role.getUserEntities().add(user);
      roleDAO.merge(role);
      userDAO.merge(user);
    }

    return authorizationHelper.convertRolesToAuthorities(user.getRoleEntities());
  }
}
