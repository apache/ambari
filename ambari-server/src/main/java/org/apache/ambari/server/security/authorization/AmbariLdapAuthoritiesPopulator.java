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
import org.apache.ambari.server.AmbariException;
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
 * Provides authorities population for LDAP user from LDAP catalog
 */
public class AmbariLdapAuthoritiesPopulator implements LdapAuthoritiesPopulator {
  private static final Logger log = LoggerFactory.getLogger(AmbariLdapAuthoritiesPopulator.class);

  Configuration configuration;
  private AuthorizationHelper authorizationHelper;
  UserDAO userDAO;
  RoleDAO roleDAO;

  private static final String AMBARI_ADMIN_LDAP_ATTRIBUTE_KEY = "ambari_admin";

  @Inject
  public AmbariLdapAuthoritiesPopulator(Configuration configuration, AuthorizationHelper authorizationHelper,
                                        UserDAO userDAO, RoleDAO roleDAO) {
    this.configuration = configuration;
    this.authorizationHelper = authorizationHelper;
    this.userDAO = userDAO;
    this.roleDAO = roleDAO;
  }

  @Override
  public Collection<? extends GrantedAuthority> getGrantedAuthorities(DirContextOperations userData, String username) {
    log.info("Get roles for user " + username + " from local DB");

    UserEntity user;

    user = userDAO.findLdapUserByName(username);

    if (user == null) {
      log.info("User " + username + " not present in local DB - creating");

      createLdapUser(username);
      user = userDAO.findLdapUserByName(username);
    }

    //don't remove admin role from user if group mapping was not configured
    if (configuration.getLdapServerProperties().isGroupMappingEnabled()) {
      //Adding an "admin" user role if user is a member of ambari administrators
      // LDAP group
      Boolean isAdmin =
          (Boolean) userData.getObjectAttribute(AMBARI_ADMIN_LDAP_ATTRIBUTE_KEY);
      if ((isAdmin != null) && isAdmin) {
        log.info("Adding admin role to LDAP user " + username);
        addRole(user, configuration.getConfigsMap().
            get(Configuration.ADMIN_ROLE_NAME_KEY));
      } else {
        removeRole(user, configuration.getConfigsMap().
            get(Configuration.ADMIN_ROLE_NAME_KEY));
      }
    }

    return authorizationHelper.convertRolesToAuthorities(user.getRoleEntities());
  }

  /**
   * Creates record in local DB for LDAP user
   * @param username - name of user to create
   */
  @Transactional
  void createLdapUser(String username) {
    UserEntity newUser = new UserEntity();
    newUser.setLdapUser(true);
    newUser.setUserName(username);

    userDAO.create(newUser);

    //Adding a default "user" role
    addRole(newUser, configuration.getConfigsMap().
        get(Configuration.USER_ROLE_NAME_KEY));
  }

  /**
   * Adds role to user's role entities
   * Adds user to roleName's user entities
   *
   * @param user - the user entity to be modified
   * @param roleName - the role to add to user's roleEntities
   */
  @Transactional
  void addRole(UserEntity user, String roleName) {
    log.info("Using default role name " + roleName);

    RoleEntity roleEntity = roleDAO.findByName(roleName);

    if (roleEntity == null) {
      log.info("Role " + roleName + " not present in local DB - creating");
      roleEntity = new RoleEntity();
      roleEntity.setRoleName(roleName);
      roleDAO.create(roleEntity);
      roleEntity = roleDAO.findByName(roleEntity.getRoleName());
    }

    UserEntity userEntity = userDAO.findLdapUserByName(user.getUserName());
    if (userEntity == null) {
      userDAO.create(user);
      userEntity = userDAO.findLdapUserByName(user.getUserName());
    }

    if (!userEntity.getRoleEntities().contains(roleEntity)) {
      userEntity.getRoleEntities().add(roleEntity);
      roleEntity.getUserEntities().add(userEntity);
      roleDAO.merge(roleEntity);
      userDAO.merge(userEntity);
    }
  }

  /**
   * Remove role "roleName" from user "user"
   * @param user
   * @param roleName
   */
  @Transactional
  void removeRole(UserEntity user, String roleName) {
    UserEntity userEntity = userDAO.findByPK(user.getUserId());
    RoleEntity roleEntity = roleDAO.findByName(roleName);

    if (userEntity.getRoleEntities().contains(roleEntity)) {
      log.info("Removing admin role from LDAP user " + user.getUserName());
      userEntity.getRoleEntities().remove(roleEntity);
      roleEntity.getUserEntities().remove(userEntity);
      userDAO.merge(userEntity);
      roleDAO.merge(roleEntity);
    }

  }
}
