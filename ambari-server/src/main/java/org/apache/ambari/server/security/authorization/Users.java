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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.hooks.HookContextFactory;
import org.apache.ambari.server.hooks.HookService;
import org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration;
import org.apache.ambari.server.orm.dao.GroupDAO;
import org.apache.ambari.server.orm.dao.MemberDAO;
import org.apache.ambari.server.orm.dao.PermissionDAO;
import org.apache.ambari.server.orm.dao.PrincipalDAO;
import org.apache.ambari.server.orm.dao.PrincipalTypeDAO;
import org.apache.ambari.server.orm.dao.PrivilegeDAO;
import org.apache.ambari.server.orm.dao.ResourceDAO;
import org.apache.ambari.server.orm.dao.ResourceTypeDAO;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.entities.GroupEntity;
import org.apache.ambari.server.orm.entities.MemberEntity;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.PrincipalTypeEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.security.ldap.LdapBatchDto;
import org.apache.ambari.server.security.ldap.LdapUserGroupMemberDto;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/**
 * Provides high-level access to Users and Roles in database
 */
@Singleton
public class Users {

  private static final Logger LOG = LoggerFactory.getLogger(Users.class);

  @Inject
  Provider<EntityManager> entityManagerProvider;
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
  protected ResourceTypeDAO resourceTypeDAO;
  @Inject
  protected PrincipalTypeDAO principalTypeDAO;
  @Inject
  protected PasswordEncoder passwordEncoder;
  @Inject
  protected AmbariLdapConfiguration ldapConfiguration;
  @Inject
  private AmbariLdapAuthenticationProvider ldapAuthenticationProvider;

  @Inject
  private Provider<HookService> hookServiceProvider;

  @Inject
  private HookContextFactory hookContextFactory;

  public List<User> getAllUsers() {
    List<UserEntity> userEntities = userDAO.findAll();
    List<User> users = new ArrayList<>(userEntities.size());

    for (UserEntity userEntity : userEntities) {
      users.add(new User(userEntity));
    }

    return users;
  }

  /**
   * This method works incorrectly, userName is not unique if users have different types
   *
   * @return One user. Priority is LOCAL -> LDAP -> JWT -> PAM
   */
  @Deprecated
  public User getAnyUser(String userName) {
    UserEntity userEntity = userDAO.findSingleUserByName(userName);
    return (null == userEntity) ? null : new User(userEntity);
  }

  public User getUser(String userName, UserType userType) {
    UserEntity userEntity = userDAO.findUserByNameAndType(userName, userType);
    return (null == userEntity) ? null : new User(userEntity);
  }

  public User getUser(Integer userId) {
    UserEntity userEntity = userDAO.findByPK(userId);
    return (null == userEntity) ? null : new User(userEntity);
  }

  /**
   * Retrieves User then userName is unique in users DB. Will return null if there no user with provided userName or
   * there are some users with provided userName but with different types.
   *
   * <p>User names in the future will likely be unique hence the deprecation.</p>
   *
   * @param userName
   * @return User if userName is unique in DB, null otherwise
   */
  @Deprecated
  public User getUserIfUnique(String userName) {
    List<UserEntity> userEntities = new ArrayList<>();
    UserEntity userEntity = userDAO.findUserByNameAndType(userName, UserType.LOCAL);
    if (userEntity != null) {
      userEntities.add(userEntity);
    }
    userEntity = userDAO.findUserByNameAndType(userName, UserType.LDAP);
    if (userEntity != null) {
      userEntities.add(userEntity);
    }
    userEntity = userDAO.findUserByNameAndType(userName, UserType.JWT);
    if (userEntity != null) {
      userEntities.add(userEntity);
    }
    userEntity = userDAO.findUserByNameAndType(userName, UserType.PAM);
    if (userEntity != null) {
      userEntities.add(userEntity);
    }
    return (userEntities.isEmpty() || userEntities.size() > 1) ? null : new User(userEntities.get(0));
  }

  /**
   * Modifies password of local user
   *
   * @throws AmbariException
   */
  public synchronized void modifyPassword(String userName, String currentUserPassword, String newPassword) throws AmbariException {

    SecurityContext securityContext = SecurityContextHolder.getContext();
    String currentUserName = securityContext.getAuthentication().getName();
    if (currentUserName == null) {
      throw new AmbariException("Authentication required. Please sign in.");
    }

    UserEntity currentUserEntity = userDAO.findLocalUserByName(currentUserName);

    //Authenticate LDAP user
    boolean isLdapUser = false;
    if (currentUserEntity == null) {
      currentUserEntity = userDAO.findLdapUserByName(currentUserName);
      try {
        ldapAuthenticationProvider.authenticate(
          new UsernamePasswordAuthenticationToken(currentUserName, currentUserPassword));
        isLdapUser = true;
      } catch (InvalidUsernamePasswordCombinationException ex) {
        throw new AmbariException(ex.getMessage());
      }
    }

    boolean isCurrentUserAdmin = false;
    for (PrivilegeEntity privilegeEntity : currentUserEntity.getPrincipal().getPrivileges()) {
      if (privilegeEntity.getPermission().getPermissionName().equals(PermissionEntity.AMBARI_ADMINISTRATOR_PERMISSION_NAME)) {
        isCurrentUserAdmin = true;
        break;
      }
    }

    UserEntity userEntity = userDAO.findLocalUserByName(userName);

    if ((userEntity != null) && (currentUserEntity != null)) {
      if (!isCurrentUserAdmin && !userName.equals(currentUserName)) {
        throw new AmbariException("You can't change password of another user");
      }

      if ((isLdapUser && isCurrentUserAdmin) || (StringUtils.isNotEmpty(currentUserPassword) &&
        passwordEncoder.matches(currentUserPassword, currentUserEntity.getUserPassword()))) {
        userEntity.setUserPassword(passwordEncoder.encode(newPassword));
        userDAO.merge(userEntity);
      } else {
        throw new AmbariException("Wrong current password provided");
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
   *
   * @param userName user name
   * @throws AmbariException if user does not exist
   */
  public synchronized void setUserActive(String userName, boolean active) throws AmbariException {
    UserEntity userEntity = userDAO.findUserByName(userName);
    if (userEntity != null) {
      userEntity.setActive(active);
      userDAO.merge(userEntity);
    } else {
      throw new AmbariException("User " + userName + " doesn't exist");
    }
  }

  /**
   * Converts user to LDAP user.
   *
   * @param userName user name
   * @throws AmbariException if user does not exist
   */
  public synchronized void setUserLdap(String userName) throws AmbariException {
    UserEntity userEntity = userDAO.findUserByName(userName);
    if (userEntity != null) {
      userEntity.setLdapUser(true);
      userDAO.merge(userEntity);
    } else {
      throw new AmbariException("User " + userName + " doesn't exist");
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
      groupEntity.setGroupType(GroupType.LDAP);
      groupDAO.merge(groupEntity);
    } else {
      throw new AmbariException("Group " + groupName + " doesn't exist");
    }
  }

  /**
   * Creates new local user with provided userName and password.
   *
   * @param userName user name
   * @param password password
   * @throws AmbariException if user already exists
   */
  public void createUser(String userName, String password) throws AmbariException {
    createUser(userName, password, UserType.LOCAL, true, false);
  }

  /**
   * Creates new user with provided userName and password.
   *
   * @param userName user name
   * @param password password
   * @param userType user type
   * @param active   is user active
   * @param admin    is user admin
   * @throws AmbariException if user already exists
   */
  public synchronized void createUser(String userName, String password, UserType userType, Boolean active, Boolean
    admin) throws AmbariException {
    // if user type is not provided, assume LOCAL since the default
    // value of user_type in the users table is LOCAL
    if (userType == null) {
      throw new AmbariException("UserType not specified.");
    }

    User existingUser = getAnyUser(userName);
    if (existingUser != null) {
      throw new AmbariException("User " + existingUser.getUserName() + " already exists with type "
        + existingUser.getUserType());
    }

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
    userEntity.setUserName(UserName.fromString(userName));
    if (userType == UserType.LOCAL) {
      //passwords should be stored for local users only
      userEntity.setUserPassword(passwordEncoder.encode(password));
    }
    userEntity.setPrincipal(principalEntity);
    if (active != null) {
      userEntity.setActive(active);
    }

    userEntity.setUserType(userType);
    if (userType == UserType.LDAP) {
      userEntity.setLdapUser(true);
    }

    userDAO.create(userEntity);

    if (admin != null && admin) {
      grantAdminPrivilege(userEntity.getUserId());
    }

    // execute user initialization hook if required ()
    hookServiceProvider.get().execute(hookContextFactory.createUserHookContext(userName));
  }

  public synchronized void removeUser(User user) throws AmbariException {
    UserEntity userEntity = userDAO.findByPK(user.getUserId());
    if (userEntity != null) {
      if (!isUserCanBeRemoved(userEntity)) {
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
   * Gets group by given name & type.
   *
   * @param groupName group name
   * @param groupType group type
   * @return group
   */
  public Group getGroupByNameAndType(String groupName, GroupType groupType) {
    final GroupEntity groupEntity = groupDAO.findGroupByNameAndType(groupName, groupType);
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
      final Set<User> users = new HashSet<>();
      for (MemberEntity memberEntity : groupEntity.getMemberEntities()) {
        if (memberEntity.getUser() != null) {
          users.add(new User(memberEntity.getUser()));
        } else {
          LOG.error("Wrong state, not found user for member '{}' (group: '{}')",
            memberEntity.getMemberId(), memberEntity.getGroup().getGroupName());
        }
      }
      return users;
    }
  }

  /**
   * Creates new group with provided name & type
   */
  @Transactional
  public synchronized void createGroup(String groupName, GroupType groupType) {
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
    groupEntity.setGroupType(groupType);

    groupDAO.create(groupEntity);
  }

  /**
   * Gets all groups.
   *
   * @return list of groups
   */
  public List<Group> getAllGroups() {
    final List<GroupEntity> groupEntities = groupDAO.findAll();
    final List<Group> groups = new ArrayList<>(groupEntities.size());

    for (GroupEntity groupEntity : groupEntities) {
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
    final List<String> members = new ArrayList<>();
    final GroupEntity groupEntity = groupDAO.findGroupByName(groupName);
    if (groupEntity == null) {
      throw new AmbariException("Group " + groupName + " doesn't exist");
    }
    for (MemberEntity member : groupEntity.getMemberEntities()) {
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
   * Grants AMBARI.ADMINISTRATOR privilege to provided user.
   *
   * @param userId user id
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
      principalDAO.merge(user.getPrincipal()); //explicit merge for Derby support
      userDAO.merge(user);
    }
  }

  /**
   * Grants privilege to provided group.
   *
   * @param groupId group id
   * @param resourceId resource id
   * @param resourceType resource type
   * @param permissionName permission name
   */
  public synchronized void grantPrivilegeToGroup(Integer groupId, Long resourceId, ResourceType resourceType, String permissionName) {
    final GroupEntity group = groupDAO.findByPK(groupId);
    final PrivilegeEntity privilege = new PrivilegeEntity();
    ResourceTypeEntity resourceTypeEntity = new ResourceTypeEntity();
    resourceTypeEntity.setId(resourceType.getId());
    resourceTypeEntity.setName(resourceType.name());
    privilege.setPermission(permissionDAO.findPermissionByNameAndType(permissionName, resourceTypeEntity));
    privilege.setPrincipal(group.getPrincipal());
    privilege.setResource(resourceDAO.findById(resourceId));
    if (!group.getPrincipal().getPrivileges().contains(privilege)) {
      privilegeDAO.create(privilege);
      group.getPrincipal().getPrivileges().add(privilege);
      principalDAO.merge(group.getPrincipal()); //explicit merge for Derby support
      groupDAO.merge(group);
      privilegeDAO.merge(privilege);
    }
  }

  /**
   * Revokes AMBARI.ADMINISTRATOR privilege from provided user.
   *
   * @param userId user id
   */
  public synchronized void revokeAdminPrivilege(Integer userId) {
    final UserEntity user = userDAO.findByPK(userId);
    for (PrivilegeEntity privilege : user.getPrincipal().getPrivileges()) {
      if (privilege.getPermission().getPermissionName().equals(PermissionEntity.AMBARI_ADMINISTRATOR_PERMISSION_NAME)) {
        user.getPrincipal().getPrivileges().remove(privilege);
        principalDAO.merge(user.getPrincipal()); //explicit merge for Derby support
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

    UserEntity userEntity = userDAO.findUserByName(userName);
    if (userEntity == null) {
      throw new AmbariException("User " + userName + " doesn't exist");
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

    UserEntity userEntity = userDAO.findUserByName(userName);
    if (userEntity == null) {
      throw new AmbariException("User " + userName + " doesn't exist");
    }

    if (isUserInGroup(userEntity, groupEntity)) {
      MemberEntity memberEntity = null;
      for (MemberEntity entity : userEntity.getMemberEntities()) {
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
  public synchronized boolean isUserCanBeRemoved(UserEntity userEntity) {
    List<PrincipalEntity> adminPrincipals = principalDAO.findByPermissionId(PermissionEntity.AMBARI_ADMINISTRATOR_PERMISSION);
    Set<UserEntity> userEntitysSet = new HashSet<>(userDAO.findUsersByPrincipal(adminPrincipals));
    return (userEntitysSet.contains(userEntity) && userEntitysSet.size() < 2) ? false : true;
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
   * Executes batch queries to database to insert large amounts of LDAP data.
   *
   * @param batchInfo DTO with batch information
   */
  public void processLdapSync(LdapBatchDto batchInfo) {
    final Map<String, UserEntity> allUsers = new HashMap<>();
    final Map<String, GroupEntity> allGroups = new HashMap<>();

    // prefetch all user and group data to avoid heavy queries in membership creation

    for (UserEntity userEntity : userDAO.findAll()) {
      allUsers.put(userEntity.getUserName(), userEntity);
    }

    for (GroupEntity groupEntity : groupDAO.findAll()) {
      allGroups.put(groupEntity.getGroupName(), groupEntity);
    }

    final PrincipalTypeEntity userPrincipalType = principalTypeDAO
      .ensurePrincipalTypeCreated(PrincipalTypeEntity.USER_PRINCIPAL_TYPE);
    final PrincipalTypeEntity groupPrincipalType = principalTypeDAO
      .ensurePrincipalTypeCreated(PrincipalTypeEntity.GROUP_PRINCIPAL_TYPE);

    // remove users
    final Set<UserEntity> usersToRemove = new HashSet<>();
    for (String userName : batchInfo.getUsersToBeRemoved()) {
      UserEntity userEntity = userDAO.findUserByName(userName);
      if (userEntity == null) {
        continue;
      }
      allUsers.remove(userEntity.getUserName());
      usersToRemove.add(userEntity);
    }
    userDAO.remove(usersToRemove);

    // remove groups
    final Set<GroupEntity> groupsToRemove = new HashSet<>();
    for (String groupName : batchInfo.getGroupsToBeRemoved()) {
      final GroupEntity groupEntity = groupDAO.findGroupByName(groupName);
      allGroups.remove(groupEntity.getGroupName());
      groupsToRemove.add(groupEntity);
    }
    groupDAO.remove(groupsToRemove);

    // update users
    final Set<UserEntity> usersToBecomeLdap = new HashSet<>();
    for (String userName : batchInfo.getUsersToBecomeLdap()) {
      UserEntity userEntity = userDAO.findLocalUserByName(userName);
      if (userEntity == null) {
        userEntity = userDAO.findLdapUserByName(userName);
        if (userEntity == null) {
          continue;
        }
      }
      userEntity.setLdapUser(true);
      allUsers.put(userEntity.getUserName(), userEntity);
      usersToBecomeLdap.add(userEntity);
    }
    userDAO.merge(usersToBecomeLdap);

    // update groups
    final Set<GroupEntity> groupsToBecomeLdap = new HashSet<>();
    for (String groupName : batchInfo.getGroupsToBecomeLdap()) {
      final GroupEntity groupEntity = groupDAO.findGroupByName(groupName);
      groupEntity.setGroupType(GroupType.LDAP);
      allGroups.put(groupEntity.getGroupName(), groupEntity);
      groupsToBecomeLdap.add(groupEntity);
    }
    groupDAO.merge(groupsToBecomeLdap);

    // prepare create principals
    final List<PrincipalEntity> principalsToCreate = new ArrayList<>();

    // prepare create users
    final Set<UserEntity> usersToCreate = new HashSet<>();
    for (String userName : batchInfo.getUsersToBeCreated()) {
      final PrincipalEntity principalEntity = new PrincipalEntity();
      principalEntity.setPrincipalType(userPrincipalType);
      principalsToCreate.add(principalEntity);

      final UserEntity userEntity = new UserEntity();
      userEntity.setUserName(UserName.fromString(userName));
      userEntity.setUserPassword("");
      userEntity.setPrincipal(principalEntity);
      userEntity.setLdapUser(true);

      allUsers.put(userEntity.getUserName(), userEntity);
      usersToCreate.add(userEntity);
    }

    // prepare create groups
    final Set<GroupEntity> groupsToCreate = new HashSet<>();
    for (String groupName : batchInfo.getGroupsToBeCreated()) {
      final PrincipalEntity principalEntity = new PrincipalEntity();
      principalEntity.setPrincipalType(groupPrincipalType);
      principalsToCreate.add(principalEntity);

      final GroupEntity groupEntity = new GroupEntity();
      groupEntity.setGroupName(groupName);
      groupEntity.setPrincipal(principalEntity);
      groupEntity.setGroupType(GroupType.LDAP);

      allGroups.put(groupEntity.getGroupName(), groupEntity);
      groupsToCreate.add(groupEntity);
    }

    // create users and groups
    principalDAO.create(principalsToCreate);
    userDAO.create(usersToCreate);
    groupDAO.create(groupsToCreate);

    // create membership
    final Set<MemberEntity> membersToCreate = new HashSet<>();
    final Set<GroupEntity> groupsToUpdate = new HashSet<>();
    for (LdapUserGroupMemberDto member : batchInfo.getMembershipToAdd()) {
      final MemberEntity memberEntity = new MemberEntity();
      final GroupEntity groupEntity = allGroups.get(member.getGroupName());
      memberEntity.setGroup(groupEntity);
      memberEntity.setUser(allUsers.get(member.getUserName()));
      groupEntity.getMemberEntities().add(memberEntity);
      groupsToUpdate.add(groupEntity);
      membersToCreate.add(memberEntity);
    }

    // handle adminGroupMappingRules
    processLdapAdminGroupMappingRules(membersToCreate);

    memberDAO.create(membersToCreate);
    groupDAO.merge(groupsToUpdate); // needed for Derby DB as it doesn't fetch newly added members automatically

    // remove membership
    final Set<MemberEntity> membersToRemove = new HashSet<>();
    for (LdapUserGroupMemberDto member : batchInfo.getMembershipToRemove()) {
      MemberEntity memberEntity = memberDAO.findByUserAndGroup(member.getUserName(), member.getGroupName());
      if (memberEntity != null) {
        membersToRemove.add(memberEntity);
      }
    }
    memberDAO.remove(membersToRemove);

    // clear cached entities
    entityManagerProvider.get().getEntityManagerFactory().getCache().evictAll();

    if (!usersToCreate.isEmpty()) {
      // entry point in the hook logic
      hookServiceProvider.get().execute(hookContextFactory.createBatchUserHookContext(getUsersToGroupMap(usersToCreate)));
    }

  }

  private void processLdapAdminGroupMappingRules(Set<MemberEntity> membershipsToCreate) {

    String adminGroupMappings = ldapConfiguration.groupMappingRules();
    if (Strings.isNullOrEmpty(adminGroupMappings) || membershipsToCreate.isEmpty()) {
      LOG.info("Nothing to do. LDAP admin group mappings: {}, Memberships to handle: {}", adminGroupMappings, membershipsToCreate.size());
      return;
    }

    LOG.info("Processing admin group mapping rules [{}]. Membership entry count: [{}]", adminGroupMappings, membershipsToCreate.size());

    // parse the comma separated list of mapping rules
    Set<String> ldapAdminGroups = Sets.newHashSet(adminGroupMappings.split(","));

    // LDAP users to become ambari administrators
    Set<UserEntity> ambariAdminProspects = Sets.newHashSet();

    // gathering all the users that need to be ambari admins
    for (MemberEntity memberEntity : membershipsToCreate) {
      if (ldapAdminGroups.contains(memberEntity.getGroup().getGroupName())) {
        LOG.debug("Ambari admin user prospect: [{}] ", memberEntity.getUser().getUserName());
        ambariAdminProspects.add(memberEntity.getUser());
      }
    }

    // granting admin privileges to the admin prospects
    for (UserEntity userEntity : ambariAdminProspects) {
      LOG.info("Granting ambari admin roles to the user: {}", userEntity.getUserName());
      grantAdminPrivilege(userEntity.getUserId());
    }

  }

  /**
   * Assembles a map where the keys are usernames and values are Lists with groups associated with users.
   *
   * @param usersToCreate a list with user entities
   * @return the populated map instance
   */
  private Map<String, Set<String>> getUsersToGroupMap(Set<UserEntity> usersToCreate) {
    Map<String, Set<String>> usersToGroups = new HashMap<>();

    for (UserEntity userEntity : usersToCreate) {

      // make sure user entities are refreshed so that membership is updated
      userEntity = userDAO.findByPK(userEntity.getUserId());

      usersToGroups.put(userEntity.getUserName(), new HashSet<>());

      for (MemberEntity memberEntity : userEntity.getMemberEntities()) {
        usersToGroups.get(userEntity.getUserName()).add(memberEntity.getGroup().getGroupName());
      }
    }

    return usersToGroups;
  }

  /**
   * Gets the explicit and implicit privileges for the given user.
   * <p>
   * The explicit privileges are the privileges that have be explicitly set by assigning roles to
   * a user.  For example the Cluster Operator role on a given cluster gives that the ability to
   * start and stop services in that cluster, among other privileges for that particular cluster.
   * <p>
   * The implicit privileges are the privileges that have been given to the roles themselves which
   * in turn are granted to the users that have been assigned those roles. For example if the
   * Cluster User role for a given cluster has been given View User access on a specified File View
   * instance, then all users who have the Cluster User role for that cluster will implicitly be
   * granted View User access on that File View instance.
   *
   * @param userEntity the relevant user
   * @return the collection of implicit and explicit privileges
   */
  public Collection<PrivilegeEntity> getUserPrivileges(UserEntity userEntity) {
    if (userEntity == null) {
      return Collections.emptyList();
    }

    // get all of the privileges for the user
    List<PrincipalEntity> principalEntities = new LinkedList<>();

    principalEntities.add(userEntity.getPrincipal());

    List<MemberEntity> memberEntities = memberDAO.findAllMembersByUser(userEntity);

    for (MemberEntity memberEntity : memberEntities) {
      principalEntities.add(memberEntity.getGroup().getPrincipal());
    }

    List<PrivilegeEntity> explicitPrivilegeEntities = privilegeDAO.findAllByPrincipal(principalEntities);
    List<PrivilegeEntity> implicitPrivilegeEntities = getImplicitPrivileges(explicitPrivilegeEntities);
    List<PrivilegeEntity> privilegeEntities;

    if (implicitPrivilegeEntities.isEmpty()) {
      privilegeEntities = explicitPrivilegeEntities;
    } else {
      privilegeEntities = new LinkedList<>();
      privilegeEntities.addAll(explicitPrivilegeEntities);
      privilegeEntities.addAll(implicitPrivilegeEntities);
    }

    return privilegeEntities;
  }

  /**
   * Gets the explicit and implicit privileges for the given group.
   * <p>
   * The explicit privileges are the privileges that have be explicitly set by assigning roles to
   * a group.  For example the Cluster Operator role on a given cluster gives that the ability to
   * start and stop services in that cluster, among other privileges for that particular cluster.
   * <p>
   * The implicit privileges are the privileges that have been given to the roles themselves which
   * in turn are granted to the groups that have been assigned those roles. For example if the
   * Cluster User role for a given cluster has been given View User access on a specified File View
   * instance, then all groups that have the Cluster User role for that cluster will implicitly be
   * granted View User access on that File View instance.
   *
   * @param groupEntity the relevant group
   * @return the collection of implicit and explicit privileges
   */
  public Collection<PrivilegeEntity> getGroupPrivileges(GroupEntity groupEntity) {
    if (groupEntity == null) {
      return Collections.emptyList();
    }

    // get all of the privileges for the group
    List<PrincipalEntity> principalEntities = new LinkedList<>();

    principalEntities.add(groupEntity.getPrincipal());

    List<PrivilegeEntity> explicitPrivilegeEntities = privilegeDAO.findAllByPrincipal(principalEntities);
    List<PrivilegeEntity> implicitPrivilegeEntities = getImplicitPrivileges(explicitPrivilegeEntities);
    List<PrivilegeEntity> privilegeEntities;

    if (implicitPrivilegeEntities.isEmpty()) {
      privilegeEntities = explicitPrivilegeEntities;
    } else {
      privilegeEntities = new LinkedList<>();
      privilegeEntities.addAll(explicitPrivilegeEntities);
      privilegeEntities.addAll(implicitPrivilegeEntities);
    }

    return privilegeEntities;
  }

  /**
   * Gets the explicit and implicit authorities for the given user.
   * <p>
   * The explicit authorities are the authorities that have be explicitly set by assigning roles to
   * a user.  For example the Cluster Operator role on a given cluster gives that the ability to
   * start and stop services in that cluster, among other privileges for that particular cluster.
   * <p>
   * The implicit authorities are the authorities that have been given to the roles themselves which
   * in turn are granted to the users that have been assigned those roles. For example if the
   * Cluster User role for a given cluster has been given View User access on a specified File View
   * instance, then all users who have the Cluster User role for that cluster will implicitly be
   * granted View User access on that File View instance.
   *
   * @param userName the username for the relevant user
   * @param userType the user type for the relevant user
   * @return the users collection of implicit and explicit granted authorities
   */
  public Collection<AmbariGrantedAuthority> getUserAuthorities(String userName, UserType userType) {
    UserEntity userEntity = userDAO.findUserByNameAndType(userName, userType);
    if (userEntity == null) {
      return Collections.emptyList();
    }

    Collection<PrivilegeEntity> privilegeEntities = getUserPrivileges(userEntity);

    Set<AmbariGrantedAuthority> authorities = new HashSet<>(privilegeEntities.size());

    for (PrivilegeEntity privilegeEntity : privilegeEntities) {
      authorities.add(new AmbariGrantedAuthority(privilegeEntity));
    }

    return authorities;
  }

  /**
   * Gets the implicit privileges based on the set of roles found in a collection of privileges.
   * <p>
   * The implicit privileges are the privileges that have been given to the roles themselves which
   * in turn are granted to the groups that have been assigned those roles. For example if the
   * Cluster User role for a given cluster has been given View User access on a specified File View
   * instance, then all groups that have the Cluster User role for that cluster will implicitly be
   * granted View User access on that File View instance.
   *
   * @param privilegeEntities the relevant privileges
   * @return the collection explicit privileges
   */
  private List<PrivilegeEntity> getImplicitPrivileges(List<PrivilegeEntity> privilegeEntities) {

    if ((privilegeEntities == null) || privilegeEntities.isEmpty()) {
      return Collections.emptyList();
    }

    List<PrivilegeEntity> implicitPrivileges = new LinkedList<>();

    // A list of principals representing roles/permissions. This collection of roles will be used to
    // find additional inherited privileges based on the assigned roles.
    // For example a File View instance may be set to be accessible to all authenticated user with
    // the Cluster User role.
    List<PrincipalEntity> rolePrincipals = new ArrayList<>();

    for (PrivilegeEntity privilegeEntity : privilegeEntities) {
      // Add the principal representing the role associated with this PrivilegeEntity to the collection
      // of roles.
      PrincipalEntity rolePrincipal = privilegeEntity.getPermission().getPrincipal();
      if (rolePrincipal != null) {
        rolePrincipals.add(rolePrincipal);
      }
    }

    // If the collections of assigned roles is not empty find the inherited priviliges.
    if (!rolePrincipals.isEmpty()) {
      // For each "role" see if any privileges have been granted...
      implicitPrivileges.addAll(privilegeDAO.findAllByPrincipal(rolePrincipals));
    }

    return implicitPrivileges;
  }

}
