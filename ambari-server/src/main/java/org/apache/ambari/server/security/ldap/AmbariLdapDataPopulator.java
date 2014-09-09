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
import org.springframework.ldap.filter.EqualsFilter;
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

  @Inject
  public AmbariLdapDataPopulator(Configuration configuration, Users users) {
    this.configuration = configuration;
    this.users = users;
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
      if (internalUsersMap.containsKey(externalUser)
          && internalUsersMap.get(externalUser).isLdapUser()) {
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
   * Performs synchronization of given sets of usernames and groupnames.
   *
   * @param users set of users to synchronize
   * @param groups set of groups to synchronize
   * @throws AmbariException if synchronization failed for any reason
   */
  public LdapBatchDto synchronizeLdapUsersAndGroups(Set<String> users,
      Set<String> groups) throws AmbariException {
    final LdapBatchDto batchInfo = new LdapBatchDto();

    // validate request
    final Set<LdapUserDto> externalUsers = getExternalLdapUserInfo();
    final Map<String, LdapUserDto> externalUsersMap = new HashMap<String, LdapUserDto>();
    for (LdapUserDto user: externalUsers) {
      externalUsersMap.put(user.getUserName(), user);
    }
    for (String user : users) {
      if (!externalUsersMap.containsKey(user)) {
        throw new AmbariException("Couldn't sync LDAP user " + user
            + ", it doesn't exist");
      }
    }
    final Set<LdapGroupDto> externalGroups = getExternalLdapGroupInfo();
    final Map<String, LdapGroupDto> externalGroupsMap = new HashMap<String, LdapGroupDto>();
    for (LdapGroupDto group: externalGroups) {
      externalGroupsMap.put(group.getGroupName(), group);
    }
    for (String group : groups) {
      if (!externalGroupsMap.containsKey(group)) {
        throw new AmbariException("Couldn't sync LDAP group " + group
            + ", it doesn't exist");
      }
    }

    final Map<String, Group> internalGroupsMap = getInternalGroups();
    final Map<String, User> internalUsersMap = getInternalUsers();

    // processing groups
    for (String groupName : groups) {
      if (internalGroupsMap.containsKey(groupName)) {
        final Group group = internalGroupsMap.get(groupName);
        if (!group.isLdapGroup()) {
          batchInfo.getGroupsToBecomeLdap().add(groupName);
        }
      } else {
        batchInfo.getGroupsToBeCreated().add(groupName);
      }
      refreshGroupMembers(batchInfo, externalGroupsMap.get(groupName), internalUsersMap, externalUsers);
      internalGroupsMap.remove(groupName);
    }
    for (Entry<String, Group> internalGroup : internalGroupsMap.entrySet()) {
      if (internalGroup.getValue().isLdapGroup()) {
        batchInfo.getGroupsToBeRemoved().add(internalGroup.getValue().getGroupName());
      }
    }

    // processing users
    for (String userName : users) {
      if (internalUsersMap.containsKey(userName)) {
        final User user = internalUsersMap.get(userName);
        if (user != null && !user.isLdapUser()) {
          batchInfo.getUsersToBecomeLdap().add(userName);
        }
      } else {
        batchInfo.getUsersToBeCreated().add(userName);
      }
    }

    return batchInfo;
  }

  /**
   * Check group members of the synced group: add missing ones and remove the ones absent in external LDAP.
   *
   * @param groupName group name
   * @param internalUsers map of internal users
   * @param externalUsers set of external users
   * @throws AmbariException if group refresh failed
   */
  protected void refreshGroupMembers(LdapBatchDto batchInfo, LdapGroupDto group, Map<String, User> internalUsers, Set<LdapUserDto> externalUsers) throws AmbariException {
    final Set<String> externalMembers = new HashSet<String>();
    for (String memberAttribute: group.getMemberAttributes()) {
      for (LdapUserDto externalUser: externalUsers) {
        // memberAttribute may be either DN or UID, check both
        if (externalUser.getDn().equals(memberAttribute) || externalUser.getUid().equals(memberAttribute)) {
          externalMembers.add(externalUser.getUserName());
          break;
        }
      }
    }
    final Map<String, User> internalMembers = getInternalMembers(group.getGroupName());
    for (String externalMember: externalMembers) {
      if (internalUsers.containsKey(externalMember)) {
        final User user = internalUsers.get(externalMember);
        if (user == null) {
          // user is fresh and is already added to batch info
          if (!internalMembers.containsKey(externalMember)) {
            batchInfo.getMembershipToAdd().add(new LdapUserGroupMemberDto(group.getGroupName(), externalMember));
          }
          continue;
        }
        if (!user.isLdapUser()) {
          batchInfo.getUsersToBecomeLdap().add(externalMember);
        }
        if (!internalMembers.containsKey(externalMember)) {
          batchInfo.getMembershipToAdd().add(new LdapUserGroupMemberDto(group.getGroupName(), externalMember));
        }
        internalMembers.remove(externalMember);
      } else {
        batchInfo.getUsersToBeCreated().add(externalMember);
        batchInfo.getMembershipToAdd().add(new LdapUserGroupMemberDto(group.getGroupName(), externalMember));
        internalUsers.put(externalMember, null);
      }
    }
    for (Entry<String, User> userToBeUnsynced: internalMembers.entrySet()) {
      final User user = userToBeUnsynced.getValue();
      batchInfo.getMembershipToRemove().add(new LdapUserGroupMemberDto(group.getGroupName(), user.getUserName()));
    }
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
    final Set<LdapGroupDto> groups = new HashSet<LdapGroupDto>();
    final LdapTemplate ldapTemplate = loadLdapTemplate();
    final EqualsFilter equalsFilter = new EqualsFilter("objectClass",
        ldapServerProperties.getGroupObjectClass());
    String baseDn = ldapServerProperties.getBaseDN();
    ldapTemplate.search(baseDn, equalsFilter.encode(), new ContextMapper() {

      @Override
      public Object mapFromContext(Object ctx) {
        final DirContextAdapter adapter = (DirContextAdapter) ctx;

        final LdapGroupDto group = new LdapGroupDto();
        final String groupNameAttribute = adapter.getStringAttribute(ldapServerProperties.getGroupNamingAttr());
        group.setGroupName(groupNameAttribute.toLowerCase());

        final String[] uniqueMembers = adapter.getStringAttributes(ldapServerProperties.getGroupMembershipAttr());
        if (uniqueMembers != null) {
          for (String uniqueMember: uniqueMembers) {
            group.getMemberAttributes().add(uniqueMember.toLowerCase());
          }
        }

        groups.add(group);
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
    final Set<LdapUserDto> users = new HashSet<LdapUserDto>();
    final LdapTemplate ldapTemplate = loadLdapTemplate();
    final EqualsFilter equalsFilter = new EqualsFilter("objectClass",
        ldapServerProperties.getUserObjectClass());
    String baseDn = ldapServerProperties.getBaseDN();
    ldapTemplate.search(baseDn, equalsFilter.encode(), new ContextMapper() {

      @Override
      public Object mapFromContext(Object ctx) {
        final LdapUserDto user = new LdapUserDto();
        final DirContextAdapter adapter  = (DirContextAdapter) ctx;
        final String usernameAttribute = adapter.getStringAttribute(ldapServerProperties.getUsernameAttribute());
        final String uidAttribute = adapter.getStringAttribute("uid");
        if (usernameAttribute != null && uidAttribute != null) {
          user.setUserName(usernameAttribute.toLowerCase());
          user.setUid(uidAttribute.toLowerCase());
          user.setDn(adapter.getNameInNamespace().toLowerCase());
        } else {
          LOG.warn("Ignoring LDAP user " + adapter.getNameInNamespace() + " as it doesn't have required" +
              " attributes uid and " + ldapServerProperties.getUsernameAttribute());
        }
        users.add(user);
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
