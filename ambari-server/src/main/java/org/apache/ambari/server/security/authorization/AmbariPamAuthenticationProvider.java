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

import java.util.HashSet;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.dao.GroupDAO;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.entities.GroupEntity;
import org.apache.ambari.server.orm.entities.MemberEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.security.ClientSecurityType;
import org.apache.ambari.server.security.authentication.pam.PamAuthenticationFactory;
import org.jvnet.libpam.PAM;
import org.jvnet.libpam.PAMException;
import org.jvnet.libpam.UnixUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import com.google.inject.Inject;

/**
 * Provides PAM user authentication & authorization logic for Ambari Server
 */

public class AmbariPamAuthenticationProvider implements AuthenticationProvider {

  @Inject
  private Users users;
  @Inject
  private UserDAO userDAO;
  @Inject
  private GroupDAO groupDAO;
  @Inject
  private PamAuthenticationFactory pamAuthenticationFactory;

  private static final Logger LOG = LoggerFactory.getLogger(AmbariPamAuthenticationProvider.class);

  private final Configuration configuration;

  @Inject
  public AmbariPamAuthenticationProvider(Configuration configuration) {
    this.configuration = configuration;
  }

  // TODO: ************
  // TODO: This is to be revisited for AMBARI-21221 (Update Pam Authentication process to work with improved user management facility)
  // TODO: ************
  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    if (isPamEnabled()) {
      //Set PAM configuration file (found under /etc/pam.d)
      String pamConfig = configuration.getPamConfigurationFile();
      PAM pam;

      try {
        //Set PAM configuration file (found under /etc/pam.d)
        pam = new PAM(pamConfig);

      } catch (PAMException ex) {
        LOG.error("Unable to Initialize PAM: " + ex.getMessage(), ex);
        throw new AuthenticationServiceException("Unable to Initialize PAM - ", ex);
      }

      try {
        return authenticateViaPam(pam, authentication);
      } finally {
        pam.dispose();
      }
    } else {
      return null;
    }
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
  }

  Authentication authenticateViaPam(PAM pam, Authentication authentication) {
    String userName = String.valueOf(authentication.getPrincipal());
    String password = String.valueOf(authentication.getCredentials());

    UnixUser unixUser;
    try {
      // authenticate using PAM
      unixUser = pam.authenticate(userName, password);
    } catch (PAMException ex) {
      LOG.error("Unable to sign in. Invalid username/password combination - " + ex.getMessage());
      Throwable t = ex.getCause();
      throw new PamAuthenticationException("Unable to sign in. Invalid username/password combination.", t);
    }

    if (unixUser != null) {
      UserEntity userEntity = ambariPamAuthorization(unixUser);

      if (userEntity != null) {
        Authentication authToken = new AmbariUserAuthentication(password, users.getUser(userEntity), users.getUserAuthorities(userEntity));
        authToken.setAuthenticated(true);
        return authToken;
      }
    }

    return null;
  }

  /**
   * Check if PAM authentication is enabled in server properties
   *
   * @return true if enabled
   */
  private boolean isPamEnabled() {
    return configuration.getClientSecurityType() == ClientSecurityType.PAM;
  }

  /**
   * Check if PAM authentication is enabled in server properties
   *
   * @return true if enabled
   */
  private boolean isAutoGroupCreationAllowed() {
    return configuration.getAutoGroupCreation().equals("true");
  }


  /**
   * Performs PAM authorization by creating user & group(s)
   *
   * @param unixUser the user
   */
  private UserEntity ambariPamAuthorization(UnixUser unixUser) {
    String userName = unixUser.getUserName();
    UserEntity userEntity = null;

    try {
      userEntity = userDAO.findUserByName(userName);

      // TODO: Ensure automatically creating users when authenticating with PAM is allowed.
      if (userEntity == null) {
        userEntity = users.createUser(userName, userName, userName);
        users.addPamAuthentication(userEntity, userName);
      }

      if (isAutoGroupCreationAllowed()) {
        //Get all the groups that user belongs to
        //Change all group names to lower case.
        Set<String> unixUserGroups = unixUser.getGroups();
        if (unixUserGroups != null) {
          for (String group : unixUserGroups) {
            // Ensure group name is lowercase
            group = group.toLowerCase();

            GroupEntity groupEntity = groupDAO.findGroupByNameAndType(group, GroupType.PAM);
            if (groupEntity == null) {
              groupEntity = users.createGroup(group, GroupType.PAM);
            }

            if (!isUserInGroup(userEntity, groupEntity)) {
              users.addMemberToGroup(groupEntity, userEntity);
            }
          }
        }

        Set<GroupEntity> ambariUserGroups = getUserGroups(userEntity);
        for (GroupEntity groupEntity : ambariUserGroups) {
          if (unixUserGroups == null || !unixUserGroups.contains(groupEntity.getGroupName())) {
            users.removeMemberFromGroup(groupEntity, userEntity);
          }
        }
      }
    } catch (AmbariException e) {
      e.printStackTrace();
    }

    return userEntity;
  }

  /**
   * Performs a check if given user belongs to given group.
   *
   * @param userEntity  user entity
   * @param groupEntity group entity
   * @return true if user presents in group
   */
  private boolean isUserInGroup(UserEntity userEntity, GroupEntity groupEntity) {
    for (MemberEntity memberEntity : userEntity.getMemberEntities()) {
      if (memberEntity.getGroup().equals(groupEntity)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Extracts all groups a user belongs to
   *
   * @param userEntity the user
   * @return Collection of group names
   */
  private Set<GroupEntity> getUserGroups(UserEntity userEntity) {
    Set<GroupEntity> groups = new HashSet<>();
    if (userEntity != null) {
      for (MemberEntity memberEntity : userEntity.getMemberEntities()) {
        GroupEntity groupEntity = memberEntity.getGroup();
        if (groupEntity.getGroupType() == GroupType.PAM) {
          groups.add(memberEntity.getGroup());
        }
      }
    }

    return groups;
  }
}
