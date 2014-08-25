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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.dao.GroupDAO;
import org.apache.ambari.server.orm.dao.MemberDAO;
import org.apache.ambari.server.orm.dao.PermissionDAO;
import org.apache.ambari.server.orm.dao.PrincipalDAO;
import org.apache.ambari.server.orm.dao.PrincipalTypeDAO;
import org.apache.ambari.server.orm.dao.PrivilegeDAO;
import org.apache.ambari.server.orm.dao.ResourceDAO;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.entities.GroupEntity;
import org.apache.ambari.server.orm.entities.MemberEntity;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.PrincipalTypeEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/**
 * Provides high-level access to Users and Roles in database
 */
@Singleton
public class Users {

  private final static Logger LOG = LoggerFactory.getLogger(Users.class);

  @Inject
  protected UserDAO userDAO;
  @Inject
  protected GroupDAO groupDAO;
  @Inject
  protected MemberDAO memberDAO;
  @Inject
  protected PrincipalDAO principalDAO;
  @Inject
  protected PermissionDAO permissionDAO;
  @Inject
  protected PrivilegeDAO privilegeDAO;
  @Inject
  protected ResourceDAO resourceDAO;
  @Inject
  protected PrincipalTypeDAO principalTypeDAO;
  @Inject
  protected PasswordEncoder passwordEncoder;
  @Inject
  protected Configuration configuration;
  @Inject
  private  AmbariLdapAuthenticationProvider ldapAuthenticationProvider;


  public List<User> getAllUsers() {
    List<UserEntity> userEntities = userDAO.findAll();
    List<User> users = new ArrayList<User>(userEntities.size());

    for (UserEntity userEntity : userEntities) {
      users.add(new User(userEntity));
    }

    return users;
  }

  public User getUser(int userId) throws AmbariException {
    UserEntity userEntity = userDAO.findByPK(userId);
    if (userEntity != null) {
      return new User(userEntity);
    } else {
      throw new AmbariException("User with id '" + userId + " not found");
    }
  }

  public User getAnyUser(String userName) {
    UserEntity userEntity = userDAO.findLdapUserByName(userName);
    if (null == userEntity) {
      userEntity = userDAO.findLocalUserByName(userName);
    }

    return (null == userEntity) ? null : new User(userEntity);
  }

  public User getLocalUser(String userName) throws AmbariException{
    UserEntity userEntity = userDAO.findLocalUserByName(userName);
    if (userEntity == null) {
      throw new AmbariException("User doesn't exist");
    }
    return new User(userEntity);
  }

  public User getLdapUser(String userName) throws AmbariException{
    UserEntity userEntity = userDAO.findLdapUserByName(userName);
    if (userEntity == null) {
      throw new AmbariException("User doesn't exist");
    }
    return new User(userEntity);
  }

  /**
   * Modifies password of local user
   * @throws AmbariException
   */
  public synchronized void modifyPassword(String userName, String currentUserPassword, String newPassword) throws AmbariException {

    SecurityContext securityContext = SecurityContextHolder.getContext();
    String currentUserName = securityContext.getAuthentication().getName();
    if (currentUserName == null) {
      throw new AmbariException("Authentication required. Please sign in.");
    }

    UserEntity currentUserEntity = userDAO.findLocalUserByName(currentUserName);

    //Authenticate LDAP admin user
    boolean isLdapAdmin = false;
    if (currentUserEntity == null) {
      currentUserEntity = userDAO.findLdapUserByName(currentUserName);
      try {
        ldapAuthenticationProvider.authenticate(
            new UsernamePasswordAuthenticationToken(currentUserName, currentUserPassword));
      isLdapAdmin = true;
      } catch (BadCredentialsException ex) {
        throw new AmbariException("Incorrect password provided for LDAP user " +
            currentUserName);
      }
    }

    UserEntity userEntity = userDAO.findLocalUserByName(userName);

    if ((userEntity != null) && (currentUserEntity != null)) {
      if (isLdapAdmin || passwordEncoder.matches(currentUserPassword, currentUserEntity.getUserPassword())) {
        userEntity.setUserPassword(passwordEncoder.encode(newPassword));
        userDAO.merge(userEntity);
      } else {
        throw new AmbariException("Wrong password provided");
      }

    } else {
      userEntity = userDAO.findLdapUserByName(userName);
      if (userEntity != null) {
        throw new AmbariException("Password of LDAP user cannot be modified");
      } else {
        throw new AmbariException("User " + userName + " not found");
      }
    }
  }

  /**
   * Enables/disables user.
   * @throws AmbariException
   */
  public synchronized void setUserActive(User user, boolean active) throws AmbariException {
    UserEntity userEntity = userDAO.findByPK(user.getUserId());
    if (userEntity != null) {
      userEntity.setActive(active);
      userDAO.merge(userEntity);
    } else {
      throw new AmbariException("User " + user + " doesn't exist");
    }
  }

  /**
   * Converts user to LDAP user.
   *
   * @param userName user name
   * @throws AmbariException if user does not exist
   */
  public synchronized void setUserLdap(String userName) throws AmbariException {
    UserEntity userEntity = userDAO.findLocalUserByName(userName);
    if (userEntity != null) {
      userEntity.setLdapUser(true);
      userDAO.merge(userEntity);
    } else {
      throw new AmbariException("User " + userName + " doesn't exist or is already an LDAP user");
    }
  }

  /**
   * Converts group to LDAP group.
   *
   * @param groupName group name
   * @throws AmbariException if group does not exist
   */
  public synchronized void setGroupLdap(String groupName) throws AmbariException {
    GroupEntity groupEntity = groupDAO.findGroupByName(groupName);
    if (groupEntity != null) {
      groupEntity.setLdapGroup(true);
      groupDAO.merge(groupEntity);
    } else {
      throw new AmbariException("Group " + groupName + " doesn't exist");
    }
  }

  /**
   * Creates new local user with provided userName and password.
   */
  public void createUser(String userName, String password) {
    createUser(userName, password, true, false);
  }

  /**
   * Creates new local user with provided userName and password.
   *
   * @param userName user name
   * @param password password
   * @param active is user active
   * @param admin is user admin
   */
  @Transactional
  public synchronized void createUser(String userName, String password, Boolean active, Boolean admin) {

    // create an admin principal to represent this user
    PrincipalTypeEntity principalTypeEntity = principalTypeDAO.findById(PrincipalTypeEntity.USER_PRINCIPAL_TYPE);
    if (principalTypeEntity == null) {
      principalTypeEntity = new PrincipalTypeEntity();
      principalTypeEntity.setId(PrincipalTypeEntity.USER_PRINCIPAL_TYPE);
      principalTypeEntity.setName(PrincipalTypeEntity.USER_PRINCIPAL_TYPE_NAME);
      principalTypeDAO.create(principalTypeEntity);
    }
    PrincipalEntity principalEntity = new PrincipalEntity();
    principalEntity.setPrincipalType(principalTypeEntity);
    principalDAO.create(principalEntity);

    UserEntity userEntity = new UserEntity();
    userEntity.setUserName(userName);
    userEntity.setUserPassword(passwordEncoder.encode(password));
    userEntity.setPrincipal(principalEntity);
    if (active != null) {
      userEntity.setActive(active);
    }

    userDAO.create(userEntity);

    if (admin != null && admin) {
      grantAdminPrivilege(userEntity.getUserId());
    }
  }

  @Transactional
  public synchronized void removeUser(User user) throws AmbariException {
    UserEntity userEntity = userDAO.findByPK(user.getUserId());
    if (userEntity != null) {
      if (!isUserCanBeRemoved(userEntity)){
        throw new AmbariException("Could not remove user " + userEntity.getUserName() +
              ". System should have at least one administrator.");
      }
      userDAO.remove(userEntity);
    } else {
      throw new AmbariException("User " + user + " doesn't exist");
    }
  }

  /**
   * Gets group by given name.
   *
   * @param groupName group name
   * @return group
   */
  public Group getGroup(String groupName) {
    final GroupEntity groupEntity = groupDAO.findGroupByName(groupName);
    return (null == groupEntity) ? null : new Group(groupEntity);
  }

  /**
   * Gets group members.
   *
   * @param groupName group name
   * @return list of members
   */
  public Collection<User> getGroupMembers(String groupName) {
    final GroupEntity groupEntity = groupDAO.findGroupByName(groupName);
    if (groupEntity == null) {
      return null;
    } else {
      final Set<User> users = new HashSet<User>();
      for (MemberEntity memberEntity: groupEntity.getMemberEntities()) {
        users.add(new User(memberEntity.getUser()));
      }
      return users;
    }
  }

  /**
   * Creates new local group with provided name
   */
  @Transactional
  public synchronized void createGroup(String groupName) {
    // create an admin principal to represent this group
    PrincipalTypeEntity principalTypeEntity = principalTypeDAO.findById(PrincipalTypeEntity.GROUP_PRINCIPAL_TYPE);
    if (principalTypeEntity == null) {
      principalTypeEntity = new PrincipalTypeEntity();
      principalTypeEntity.setId(PrincipalTypeEntity.GROUP_PRINCIPAL_TYPE);
      principalTypeEntity.setName(PrincipalTypeEntity.GROUP_PRINCIPAL_TYPE_NAME);
      principalTypeDAO.create(principalTypeEntity);
    }
    PrincipalEntity principalEntity = new PrincipalEntity();
    principalEntity.setPrincipalType(principalTypeEntity);
    principalDAO.create(principalEntity);

    final GroupEntity groupEntity = new GroupEntity();
    groupEntity.setGroupName(groupName);
    groupEntity.setPrincipal(principalEntity);

    groupDAO.create(groupEntity);
  }

  /**
   * Gets all groups.
   *
   * @return list of groups
   */
  public List<Group> getAllGroups() {
    final List<GroupEntity> groupEntities = groupDAO.findAll();
    final List<Group> groups = new ArrayList<Group>(groupEntities.size());

    for (GroupEntity groupEntity: groupEntities) {
      groups.add(new Group(groupEntity));
    }

    return groups;
  }

  /**
   * Gets all members of a group specified.
   *
   * @param groupName group name
   * @return list of user names
   */
  public List<String> getAllMembers(String groupName) throws AmbariException {
    final List<String> members = new ArrayList<String>();
    final GroupEntity groupEntity = groupDAO.findGroupByName(groupName);
    if (groupEntity == null) {
      throw new AmbariException("Group " + groupName + " doesn't exist");
    }
    for (MemberEntity member: groupEntity.getMemberEntities()) {
      members.add(member.getUser().getUserName());
    }
    return members;
  }

  @Transactional
  public synchronized void removeGroup(Group group) throws AmbariException {
    final GroupEntity groupEntity = groupDAO.findByPK(group.getGroupId());
    if (groupEntity != null) {
      groupDAO.remove(groupEntity);
    } else {
      throw new AmbariException("Group " + group + " doesn't exist");
    }
  }

  /**
   * Grants AMBARI.ADMIN privilege to provided user.
   *
   * @param user user
   */
  public synchronized void grantAdminPrivilege(Integer userId) {
    final UserEntity user = userDAO.findByPK(userId);
    final PrivilegeEntity adminPrivilege = new PrivilegeEntity();
    adminPrivilege.setPermission(permissionDAO.findAmbariAdminPermission());
    adminPrivilege.setPrincipal(user.getPrincipal());
    adminPrivilege.setResource(resourceDAO.findAmbariResource());
    if (!user.getPrincipal().getPrivileges().contains(adminPrivilege)) {
      privilegeDAO.create(adminPrivilege);
      user.getPrincipal().getPrivileges().add(adminPrivilege);
      userDAO.merge(user);
    }
  }

  /**
   * Revokes AMBARI.ADMIN privilege from provided user.
   *
   * @param user user
   */
  public synchronized void revokeAdminPrivilege(Integer userId) {
    final UserEntity user = userDAO.findByPK(userId);
    for (PrivilegeEntity privilege: user.getPrincipal().getPrivileges()) {
      if (privilege.getPermission().getPermissionName().equals(PermissionEntity.AMBARI_ADMIN_PERMISSION_NAME)) {
        user.getPrincipal().getPrivileges().remove(privilege);
        userDAO.merge(user);
        privilegeDAO.remove(privilege);
        break;
      }
    }
  }

  @Transactional
  public synchronized void addMemberToGroup(String groupName, String userName)
      throws AmbariException {

    final GroupEntity groupEntity = groupDAO.findGroupByName(groupName);
    if (groupEntity == null) {
      throw new AmbariException("Group " + groupName + " doesn't exist");
    }

    UserEntity userEntity = userDAO.findLocalUserByName(userName);
    if (userEntity == null) {
      userEntity = userDAO.findLdapUserByName(userName);
      if (userEntity == null) {
        throw new AmbariException("User " + userName + " doesn't exist");
      }
    }

    if (isUserInGroup(userEntity, groupEntity)) {
      throw new AmbariException("User " + userName + " is already present in group " + groupName);
    } else {
      final MemberEntity memberEntity = new MemberEntity();
      memberEntity.setGroup(groupEntity);
      memberEntity.setUser(userEntity);
      userEntity.getMemberEntities().add(memberEntity);
      groupEntity.getMemberEntities().add(memberEntity);
      memberDAO.create(memberEntity);
      userDAO.merge(userEntity);
      groupDAO.merge(groupEntity);
    }
  }

  @Transactional
  public synchronized void removeMemberFromGroup(String groupName, String userName)
      throws AmbariException {

    final GroupEntity groupEntity = groupDAO.findGroupByName(groupName);
    if (groupEntity == null) {
      throw new AmbariException("Group " + groupName + " doesn't exist");
    }

    UserEntity userEntity = userDAO.findLocalUserByName(userName);
    if (userEntity == null) {
      userEntity = userDAO.findLdapUserByName(userName);
      if (userEntity == null) {
        throw new AmbariException("User " + userName + " doesn't exist");
      }
    }

    if (isUserInGroup(userEntity, groupEntity)) {
      MemberEntity memberEntity = null;
      for (MemberEntity entity: userEntity.getMemberEntities()) {
        if (entity.getGroup().equals(groupEntity)) {
          memberEntity = entity;
          break;
        }
      }
      userEntity.getMemberEntities().remove(memberEntity);
      groupEntity.getMemberEntities().remove(memberEntity);
      userDAO.merge(userEntity);
      groupDAO.merge(groupEntity);
      memberDAO.remove(memberEntity);
    } else {
      throw new AmbariException("User " + userName + " is not present in group " + groupName);
    }

  }

  /**
   * Performs a check if the user can be removed. Do not allow removing all admins from database.
   *
   * @param userEntity user to be checked
   * @return true if user can be removed
   */
  public synchronized boolean isUserCanBeRemoved(UserEntity userEntity){
    List<PrincipalEntity> adminPrincipals = principalDAO.findByPermissionId(PermissionEntity.AMBARI_ADMIN_PERMISSION);
    Set<UserEntity> userEntitysSet = new HashSet<UserEntity>(userDAO.findUsersByPrincipal(adminPrincipals));
    return (userEntitysSet.contains(userEntity) && userEntitysSet.size() < 2) ? false : true;
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

}
