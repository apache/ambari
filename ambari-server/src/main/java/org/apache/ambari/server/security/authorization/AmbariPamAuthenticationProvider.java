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
  protected UserDAO userDAO;
  @Inject
  protected GroupDAO groupDAO;
  @Inject
  private PamAuthenticationFactory pamAuthenticationFactory;

  private static final Logger LOG = LoggerFactory.getLogger(AmbariPamAuthenticationProvider.class);

  private final Configuration configuration;

  @Inject
  public AmbariPamAuthenticationProvider(Configuration configuration) {
    this.configuration = configuration;
  }

  /**
   * Performs PAM Initialization
   *
   * @param authentication
   * @return authentication
   */

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
      if(isPamEnabled()){
        PAM pam;
        String userName = String.valueOf(authentication.getPrincipal());
        UserEntity existingUser = userDAO.findUserByName(userName);
        if ((existingUser != null) && (existingUser.getUserType() != UserType.PAM)) {
          String errorMsg = String.format("%s user exists with the username %s. Cannot authenticate via PAM", existingUser.getUserType(), userName);
          LOG.error(errorMsg);
          return null;
        }
        try{
          //Set PAM configuration file (found under /etc/pam.d)
          String pamConfig = configuration.getPamConfigurationFile();
          pam = pamAuthenticationFactory.createInstance(pamConfig);

        } catch(PAMException ex) {
          LOG.error("Unable to Initialize PAM." + ex.getMessage());
          throw new AuthenticationServiceException("Unable to Initialize PAM - ", ex);
        }

        return authenticateViaPam(pam, authentication);
    } else {
       return null;
    }
  }

  /**
   * Performs PAM Authentication
   *
   * @param pam
   * @param authentication
   * @return authentication
   */

  protected Authentication authenticateViaPam(PAM pam, Authentication authentication) throws AuthenticationException{
    if(isPamEnabled()){
      try {
          String userName = String.valueOf(authentication.getPrincipal());
          String passwd = String.valueOf(authentication.getCredentials());

          // authenticate using PAM
          UnixUser unixUser = pam.authenticate(userName,passwd);

          //Get all the groups that user belongs to
          //Change all group names to lower case.
          Set<String> groups = new HashSet<>();

          for(String group: unixUser.getGroups()){
            groups.add(group.toLowerCase());
          }

          ambariPamAuthorization(userName,groups);

          Collection<AmbariGrantedAuthority> userAuthorities =
              users.getUserAuthorities(userName, UserType.PAM);

          final User user = users.getUser(userName, UserType.PAM);
 
          Authentication authToken = new AmbariUserAuthentication(passwd, user, userAuthorities);
          authToken.setAuthenticated(true);
          return authToken;   
        } catch (PAMException ex) {
          LOG.error("Unable to sign in. Invalid username/password combination - " + ex.getMessage());
          Throwable t = ex.getCause();
          throw new PamAuthenticationException("Unable to sign in. Invalid username/password combination.",t);

        } finally {
          pam.dispose();
        }

      }
      else {
        return null;
      }
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
  }

  /**
   * Check if PAM authentication is enabled in server properties
   * @return true if enabled
   */
  private boolean isPamEnabled() {
    return configuration.getClientSecurityType() == ClientSecurityType.PAM;
  }

  /**
   * Check if PAM authentication is enabled in server properties
   * @return true if enabled
   */
  private boolean isAutoGroupCreationAllowed() {
    return configuration.getAutoGroupCreation().equals("true");
  }


  /**
   * Performs PAM authorization by creating user & group(s)
   *
   * @param userName user name
   * @param userGroups Collection of groups
   * @return
   */
  private void ambariPamAuthorization(String userName,Set<String> userGroups){
    try {
      User existingUser = users.getUser(userName,UserType.PAM);

      if (existingUser == null ) {
        users.createUser(userName, null, UserType.PAM, true, false);
      }

      UserEntity userEntity = userDAO.findUserByNameAndType(userName, UserType.PAM);

      if(isAutoGroupCreationAllowed()){
        for(String userGroup: userGroups){
          if(users.getGroupByNameAndType(userGroup, GroupType.PAM) == null){
            users.createGroup(userGroup, GroupType.PAM);
          }

          final GroupEntity groupEntity = groupDAO.findGroupByNameAndType(userGroup, GroupType.PAM);

          if (!isUserInGroup(userEntity, groupEntity)){
            users.addMemberToGroup(userGroup,userName);
          }
        }

        Set<String> ambariUserGroups = getUserGroups(userName, UserType.PAM);

        for(String group: ambariUserGroups){
          if(userGroups == null || !userGroups.contains(group)){
            users.removeMemberFromGroup(group, userName);
          }
        }
      }

    } catch (AmbariException e) {
      e.printStackTrace();
    }
  }

  /**
   * Performs a check if given user belongs to given group.
   *
   * @param userEntity user entity
   * @param groupEntity group entity
   * @return true if user presents in group
   */
  private boolean isUserInGroup(UserEntity userEntity, GroupEntity groupEntity) {
    for (MemberEntity memberEntity: userEntity.getMemberEntities()) {
      if (memberEntity.getGroup().equals(groupEntity)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Extracts all groups a user belongs to
   *
   * @param userName user name
   * @return Collection of group names
   */
  private Set<String> getUserGroups(String userName, UserType userType) {
    UserEntity userEntity = userDAO.findUserByNameAndType(userName, userType);
    Set<String> groups = new HashSet<>();
    for (MemberEntity memberEntity: userEntity.getMemberEntities()) {
      groups.add(memberEntity.getGroup().getGroupName());
    }

    return groups;
  }

}
