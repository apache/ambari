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
package org.apache.ambari.server.security.ldap;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.security.authorization.Group;
import org.apache.ambari.server.security.authorization.LdapServerProperties;
import org.apache.ambari.server.security.authorization.User;
import org.apache.ambari.server.security.authorization.Users;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.filter.Filter;
import org.springframework.ldap.filter.LikeFilter;
import org.springframework.ldap.filter.OrFilter;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.google.inject.Inject;

/**
 * Provides users, groups and membership population from LDAP catalog.
 */
public class AmbariLdapDataPopulator {
  /**
   * Log.
   */
  private static final Log LOG = LogFactory.getLog(AmbariLdapDataPopulator.class);

  /**
   * Ambari configuration.
   */
  private Configuration configuration;

  /**
   * Highlevel facade for management of users and groups.
   */
  private Users users;

  /**
   * LDAP specific properties.
   */
  protected LdapServerProperties ldapServerProperties;

  /**
   * LDAP template for making search queries.
   */
  private LdapTemplate ldapTemplate;

  // Constants
  private static final String UID_ATTRIBUTE          = "uid";
  private static final String DN_ATTRIBUTE           = "dn";
  private static final String OBJECT_CLASS_ATTRIBUTE = "objectClass";

  /**
   * Construct an AmbariLdapDataPopulator.
   *
   * @param configuration  the Ambari configuration
   * @param users          utility that provides access to Users
   */
  @Inject
  public AmbariLdapDataPopulator(Configuration configuration, Users users) {
    this.configuration = configuration;
    this.users = users;
    this.ldapServerProperties = configuration.getLdapServerProperties();
  }

  /**
   * Check if LDAP is enabled in server properties.
   *
   * @return true if enabled
   */
  public boolean isLdapEnabled() {
    if (!configuration.isLdapConfigured()) {
      return false;
    }
    try {
      final LdapTemplate ldapTemplate = loadLdapTemplate();
      ldapTemplate.search(ldapServerProperties.getBaseDN(), "uid=dummy_search", new AttributesMapper() {

        @Override
        public Object mapFromAttributes(Attributes arg0) throws NamingException {
          return null;
        }
      });
      return true;
    } catch (Exception ex) {
      LOG.error("Could not connect to LDAP server - " + ex.getMessage());
      return false;
    }
  }

  /**
   * Retrieves information about external groups and users and their synced/unsynced state.
   *
   * @return dto with information
   */
  public LdapSyncDto getLdapSyncInfo() {
    final LdapSyncDto syncInfo = new LdapSyncDto();

    final Map<String, Group> internalGroupsMap = getInternalGroups();
    final Set<LdapGroupDto> externalGroups = getExternalLdapGroupInfo();
    for (LdapGroupDto externalGroup : externalGroups) {
      if (internalGroupsMap.containsKey(externalGroup.getGroupName())
          && internalGroupsMap.get(externalGroup.getGroupName()).isLdapGroup()) {
        externalGroup.setSynced(true);
      } else {
        externalGroup.setSynced(false);
      }
    }

    final Map<String, User> internalUsersMap = getInternalUsers();
    final Set<LdapUserDto> externalUsers = getExternalLdapUserInfo();
    for (LdapUserDto externalUser : externalUsers) {
      String userName = externalUser.getUserName();
      if (internalUsersMap.containsKey(userName)
          && internalUsersMap.get(userName).isLdapUser()) {
        externalUser.setSynced(true);
      } else {
        externalUser.setSynced(false);
      }
    }

    syncInfo.setGroups(externalGroups);
    syncInfo.setUsers(externalUsers);
    return syncInfo;
  }

  /**
   * Performs synchronization of all groups.
   *
   * @throws AmbariException if synchronization failed for any reason
   */
  public LdapBatchDto synchronizeAllLdapGroups(LdapBatchDto batchInfo) throws AmbariException {

    Set<LdapGroupDto> externalLdapGroupInfo = getExternalLdapGroupInfo();

    final Map<String, Group> internalGroupsMap = getInternalGroups();
    final Map<String, User> internalUsersMap = getInternalUsers();

    for (LdapGroupDto groupDto : externalLdapGroupInfo) {
      String groupName = groupDto.getGroupName();
      if (internalGroupsMap.containsKey(groupName)) {
        final Group group = internalGroupsMap.get(groupName);
        if (!group.isLdapGroup()) {
          batchInfo.getGroupsToBecomeLdap().add(groupName);
        }
        internalGroupsMap.remove(groupName);
      } else {
        batchInfo.getGroupsToBeCreated().add(groupName);
      }
      refreshGroupMembers(batchInfo, groupDto, internalUsersMap);
    }
    for (Entry<String, Group> internalGroup : internalGroupsMap.entrySet()) {
      if (internalGroup.getValue().isLdapGroup()) {
        batchInfo.getGroupsToBeRemoved().add(internalGroup.getValue().getGroupName());
      }
    }

    return batchInfo;
  }

  /**
   * Performs synchronization of given sets of all users.
   *
   * @throws AmbariException if synchronization failed for any reason
   */
  public LdapBatchDto synchronizeAllLdapUsers(LdapBatchDto batchInfo) throws AmbariException {

    Set<LdapUserDto> externalLdapUserInfo = getExternalLdapUserInfo();
    Map<String, User> internalUsersMap = getInternalUsers();

    for (LdapUserDto userDto : externalLdapUserInfo) {
      String userName = userDto.getUserName();
      if (internalUsersMap.containsKey(userName)) {
        final User user = internalUsersMap.get(userName);
        if (user != null && !user.isLdapUser()) {
          batchInfo.getUsersToBecomeLdap().add(userName);
        }
        internalUsersMap.remove(userName);
      } else {
        batchInfo.getUsersToBeCreated().add(userName);
      }
    }
    for (Entry<String, User> internalUser : internalUsersMap.entrySet()) {
      if (internalUser.getValue().isLdapUser()) {
        batchInfo.getUsersToBeRemoved().add(internalUser.getValue().getUserName());
      }
    }

    return batchInfo;
  }

  /**
   * Performs synchronization of given set of groupnames.
   *
   * @param groups set of groups to synchronize
   * @throws AmbariException if synchronization failed for any reason
   */
  public LdapBatchDto synchronizeLdapGroups(Set<String> groups, LdapBatchDto batchInfo) throws AmbariException {

    final Set<LdapGroupDto> specifiedGroups = new HashSet<LdapGroupDto>();
    for (String group : groups) {
      Set<LdapGroupDto> groupDtos = getLdapGroups(group);
      if (groupDtos.isEmpty()) {
        throw new AmbariException("Couldn't sync LDAP group " + group
            + ", it doesn't exist");
      }
      specifiedGroups.addAll(groupDtos);
    }

    final Map<String, Group> internalGroupsMap = getInternalGroups();
    final Map<String, User> internalUsersMap = getInternalUsers();

    for (LdapGroupDto groupDto : specifiedGroups) {
      String groupName = groupDto.getGroupName();
      if (internalGroupsMap.containsKey(groupName)) {
        final Group group = internalGroupsMap.get(groupName);
        if (!group.isLdapGroup()) {
          batchInfo.getGroupsToBecomeLdap().add(groupName);
        }
        internalGroupsMap.remove(groupName);
      } else {
        batchInfo.getGroupsToBeCreated().add(groupName);
      }
      refreshGroupMembers(batchInfo, groupDto, internalUsersMap);
    }

    return batchInfo;
  }

  /**
   * Performs synchronization of given set of user names.
   *
   * @param users set of users to synchronize
   * @throws AmbariException if synchronization failed for any reason
   */
  public LdapBatchDto synchronizeLdapUsers(Set<String> users, LdapBatchDto batchInfo) throws AmbariException {

    final Set<LdapUserDto> specifiedUsers = new HashSet<LdapUserDto>();

    for (String user : users) {
      Set<LdapUserDto> userDtos = getLdapUsers(user);
      if (userDtos.isEmpty()) {
        throw new AmbariException("Couldn't sync LDAP user " + user
            + ", it doesn't exist");
      }
      specifiedUsers.addAll(userDtos);
    }

    final Map<String, User> internalUsersMap = getInternalUsers();
    for (LdapUserDto userDto : specifiedUsers) {
      String userName = userDto.getUserName();
      if (internalUsersMap.containsKey(userName)) {
        final User user = internalUsersMap.get(userName);
        if (user != null && !user.isLdapUser()) {
          batchInfo.getUsersToBecomeLdap().add(userName);
        }
        internalUsersMap.remove(userName);
      } else {
        batchInfo.getUsersToBeCreated().add(userName);
      }
    }

    return batchInfo;
  }

  /**
   * Performs synchronization of existent users and groups.
   *
   * @throws AmbariException if synchronization failed for any reason
   */
  public LdapBatchDto synchronizeExistingLdapGroups(LdapBatchDto batchInfo) throws AmbariException {
    final Map<String, Group> internalGroupsMap = getInternalGroups();
    final Map<String, User> internalUsersMap = getInternalUsers();

    for (Group group : internalGroupsMap.values()) {
      if (group.isLdapGroup()) {
        Set<LdapGroupDto> groupDtos = getLdapGroups(group.getGroupName());
        if (groupDtos.isEmpty()) {
          batchInfo.getGroupsToBeRemoved().add(group.getGroupName());
        } else {
          LdapGroupDto groupDto = groupDtos.iterator().next();
          refreshGroupMembers(batchInfo, groupDto, internalUsersMap);
        }
      }
    }

    return batchInfo;
  }

  /**
   * Performs synchronization of existent users and groups.
   *
   * @throws AmbariException if synchronization failed for any reason
   */
  public LdapBatchDto synchronizeExistingLdapUsers(LdapBatchDto batchInfo) throws AmbariException {
    final Map<String, User> internalUsersMap = getInternalUsers();

    for (User user : internalUsersMap.values()) {
      if (user.isLdapUser()) {
        Set<LdapUserDto> userDtos = getLdapUsers(user.getUserName());
        if (userDtos.isEmpty()) {
          batchInfo.getUsersToBeRemoved().add(user.getUserName());
        }
      }
    }

    return batchInfo;
  }

  /**
   * Check group members of the synced group: add missing ones and remove the ones absent in external LDAP.
   *
   * @param batchInfo batch update object
   * @param group ldap group
   * @param internalUsers map of internal users
   * @throws AmbariException if group refresh failed
   */
  protected void refreshGroupMembers(LdapBatchDto batchInfo, LdapGroupDto group, Map<String, User> internalUsers) throws AmbariException {
    Set<String> externalMembers = new HashSet<String>();
    for (String memberAttribute: group.getMemberAttributes()) {
      LdapUserDto groupMember = getLdapUserByMemberAttr(memberAttribute);
      if (groupMember != null) {
        externalMembers.add(groupMember.getUserName());
      }
    }
    String groupName = group.getGroupName();
    final Map<String, User> internalMembers = getInternalMembers(groupName);
    for (String externalMember: externalMembers) {
      if (internalUsers.containsKey(externalMember)) {
        final User user = internalUsers.get(externalMember);
        if (user == null) {
          // user is fresh and is already added to batch info
          if (!internalMembers.containsKey(externalMember)) {
            batchInfo.getMembershipToAdd().add(new LdapUserGroupMemberDto(groupName, externalMember));
          }
          continue;
        }
        if (!user.isLdapUser()) {
          batchInfo.getUsersToBecomeLdap().add(externalMember);
        }
        if (!internalMembers.containsKey(externalMember)) {
          batchInfo.getMembershipToAdd().add(new LdapUserGroupMemberDto(groupName, externalMember));
        }
        internalMembers.remove(externalMember);
      } else {
        batchInfo.getUsersToBeCreated().add(externalMember);
        batchInfo.getMembershipToAdd().add(new LdapUserGroupMemberDto(groupName, externalMember));
      }
    }
    for (Entry<String, User> userToBeUnsynced: internalMembers.entrySet()) {
      final User user = userToBeUnsynced.getValue();
      batchInfo.getMembershipToRemove().add(new LdapUserGroupMemberDto(groupName, user.getUserName()));
    }
  }

  /**
   * Get the set of LDAP groups for the given group name.
   *
   * @param groupName  the group name
   *
   * @return the set of LDAP groups for the given name
   */
  protected Set<LdapGroupDto> getLdapGroups(String groupName) {
    Filter groupObjectFilter = new EqualsFilter(OBJECT_CLASS_ATTRIBUTE,
        ldapServerProperties.getGroupObjectClass());
    Filter groupNameFilter = new LikeFilter(ldapServerProperties.getGroupNamingAttr(), groupName);
    return getFilteredLdapGroups(groupObjectFilter, groupNameFilter);
  }

  /**
   * Get the set of LDAP users for the given user name.
   *
   * @param username  the user name
   *
   * @return the set of LDAP users for the given name
   */
  protected Set<LdapUserDto> getLdapUsers(String username) {
    Filter userObjectFilter = new EqualsFilter(OBJECT_CLASS_ATTRIBUTE, ldapServerProperties.getUserObjectClass());
    Filter userNameFilter = new LikeFilter(ldapServerProperties.getUsernameAttribute(), username);
    return getFilteredLdapUsers(userObjectFilter, userNameFilter);
  }

  /**
   * Get the LDAP member for the given member attribute.
   *
   * @param memberAttribute  the member attribute
   *
   * @return the user for the given member attribute; null if not found
   */
  protected LdapUserDto getLdapUserByMemberAttr(String memberAttribute) {
    Filter userObjectFilter = new EqualsFilter(OBJECT_CLASS_ATTRIBUTE, ldapServerProperties.getUserObjectClass());
    Set<LdapUserDto> filteredLdapUsers = getFilteredLdapUsers(userObjectFilter, getMemberFilter(memberAttribute));
    return (filteredLdapUsers.isEmpty()) ? null : filteredLdapUsers.iterator().next();
  }

  /**
   * Removes synced users which are not present in any of group.
   *
   * @throws AmbariException
   */
  protected void cleanUpLdapUsersWithoutGroup() throws AmbariException {
    final List<User> allUsers = users.getAllUsers();
    for (User user: allUsers) {
      if (user.isLdapUser() && user.getGroups().isEmpty()) {
        users.removeUser(user);
      }
    }
  }

  // Utility methods

  /**
   * Retrieves groups from external LDAP server.
   *
   * @return set of info about LDAP groups
   */
  protected Set<LdapGroupDto> getExternalLdapGroupInfo() {
    EqualsFilter groupObjectFilter = new EqualsFilter(OBJECT_CLASS_ATTRIBUTE,
        ldapServerProperties.getGroupObjectClass());
    return getFilteredLdapGroups(groupObjectFilter);
  }

  // get a filter based on the given member attribute
  private Filter getMemberFilter(String memberAttribute) {

    String   usernameAttribute = ldapServerProperties.getUsernameAttribute();
    OrFilter memberFilter      = null;

    String[] filters = memberAttribute.split(",");
    for (String filter : filters) {
      String[] operands = filter.split("=");
      if (operands.length == 2) {

        String lOperand = operands[0];

        if (lOperand.equals(usernameAttribute) || lOperand.equals(UID_ATTRIBUTE) || lOperand.equals(DN_ATTRIBUTE)) {
          if (memberFilter == null) {
            memberFilter = new OrFilter();
          }
          memberFilter.or(new EqualsFilter(lOperand, operands[1]));
        }
      }
    }
    return memberFilter == null ?
        new OrFilter().or(new EqualsFilter(DN_ATTRIBUTE, memberAttribute)).
            or(new EqualsFilter(UID_ATTRIBUTE, memberAttribute)) :
        memberFilter;
  }

  private Set<LdapGroupDto> getFilteredLdapGroups(Filter...filters) {
    AndFilter andFilter = new AndFilter();
    for (Filter filter : filters) {
      andFilter.and(filter);
    }
    return getFilteredLdapGroups(andFilter);
  }

  private Set<LdapGroupDto> getFilteredLdapGroups(Filter filter) {
    final Set<LdapGroupDto> groups = new HashSet<LdapGroupDto>();
    final LdapTemplate ldapTemplate = loadLdapTemplate();
    String baseDn = ldapServerProperties.getBaseDN();
    ldapTemplate.search(baseDn, filter.encode(), new ContextMapper() {

      @Override
      public Object mapFromContext(Object ctx) {
        final DirContextAdapter adapter = (DirContextAdapter) ctx;

        final LdapGroupDto group = new LdapGroupDto();
        final String groupNameAttribute = adapter.getStringAttribute(ldapServerProperties.getGroupNamingAttr());

        if (groupNameAttribute != null) {
          group.setGroupName(groupNameAttribute.toLowerCase());

          final String[] uniqueMembers = adapter.getStringAttributes(ldapServerProperties.getGroupMembershipAttr());
          if (uniqueMembers != null) {
            for (String uniqueMember: uniqueMembers) {
              group.getMemberAttributes().add(uniqueMember.toLowerCase());
            }
          }
          groups.add(group);
        }
        return null;
      }
    });
    return groups;
  }

  /**
   * Retrieves users from external LDAP server.
   *
   * @return set of info about LDAP users
   */
  protected Set<LdapUserDto> getExternalLdapUserInfo() {
    EqualsFilter userObjectFilter = new EqualsFilter(OBJECT_CLASS_ATTRIBUTE,
        ldapServerProperties.getUserObjectClass());
    return getFilteredLdapUsers(userObjectFilter);
  }

  private Set<LdapUserDto> getFilteredLdapUsers(Filter...filters) {
    AndFilter andFilter = new AndFilter();
    for (Filter filter : filters) {
      andFilter.and(filter);
    }
    return getFilteredLdapUsers(andFilter);
  }

  private Set<LdapUserDto> getFilteredLdapUsers(Filter filter) {
    final Set<LdapUserDto> users = new HashSet<LdapUserDto>();
    final LdapTemplate ldapTemplate = loadLdapTemplate();
    String baseDn = ldapServerProperties.getBaseDN();
    ldapTemplate.search(baseDn, filter.encode(), new ContextMapper() {

      @Override
      public Object mapFromContext(Object ctx) {
        final LdapUserDto user = new LdapUserDto();
        final DirContextAdapter adapter  = (DirContextAdapter) ctx;
        final String usernameAttribute = adapter.getStringAttribute(ldapServerProperties.getUsernameAttribute());
        final String uidAttribute = adapter.getStringAttribute(UID_ATTRIBUTE);
        if (usernameAttribute != null && uidAttribute != null) {
          user.setUserName(usernameAttribute.toLowerCase());
          user.setUid(uidAttribute.toLowerCase());
          user.setDn(adapter.getNameInNamespace().toLowerCase());
          users.add(user);
        } else {
          LOG.warn("Ignoring LDAP user " + adapter.getNameInNamespace() + " as it doesn't have required" +
              " attributes uid and " + ldapServerProperties.getUsernameAttribute());
        }
        return null;
      }
    });
    return users;
  }

  /**
   * Creates a map of internal groups.
   *
   * @return map of GroupName-Group pairs
   */
  protected Map<String, Group> getInternalGroups() {
    final List<Group> internalGroups = users.getAllGroups();
    final Map<String, Group> internalGroupsMap = new HashMap<String, Group>();
    for (Group group : internalGroups) {
      internalGroupsMap.put(group.getGroupName(), group);
    }
    return internalGroupsMap;
  }

  /**
   * Creates a map of internal users.
   *
   * @return map of UserName-User pairs
   */
  protected Map<String, User> getInternalUsers() {
    final List<User> internalUsers = users.getAllUsers();
    final Map<String, User> internalUsersMap = new HashMap<String, User>();
    for (User user : internalUsers) {
      internalUsersMap.put(user.getUserName(), user);
    }
    return internalUsersMap;
  }

  /**
   * Creates a map of internal users present in specified group.
   *
   * @param groupName group name
   * @return map of UserName-User pairs
   */
  protected Map<String, User> getInternalMembers(String groupName) {
    final Collection<User> internalMembers = users.getGroupMembers(groupName);
    if (internalMembers == null) {
      return Collections.emptyMap();
    }
    final Map<String, User> internalMembersMap = new HashMap<String, User>();
    for (User user : internalMembers) {
      internalMembersMap.put(user.getUserName(), user);
    }
    return internalMembersMap;
  }

  /**
   * Checks LDAP configuration for changes and reloads LDAP template if they occurred.
   *
   * @return LdapTemplate instance
   */
  protected LdapTemplate loadLdapTemplate() {
    final LdapServerProperties properties = configuration
        .getLdapServerProperties();
    if (ldapTemplate == null || !properties.equals(ldapServerProperties)) {
      LOG.info("Reloading properties");
      ldapServerProperties = properties;

      final LdapContextSource ldapContextSource = new LdapContextSource();
      final List<String> ldapUrls = ldapServerProperties.getLdapUrls();
      ldapContextSource.setUrls(ldapUrls.toArray(new String[ldapUrls.size()]));

      if (!ldapServerProperties.isAnonymousBind()) {
        ldapContextSource.setUserDn(ldapServerProperties.getManagerDn());
        ldapContextSource.setPassword(ldapServerProperties.getManagerPassword());
      }

      try {
        ldapContextSource.afterPropertiesSet();
      } catch (Exception e) {
        LOG.error("LDAP Context Source not loaded ", e);
        throw new UsernameNotFoundException("LDAP Context Source not loaded", e);
      }

      ldapTemplate = new LdapTemplate(ldapContextSource);
    }
    return ldapTemplate;
  }
}
