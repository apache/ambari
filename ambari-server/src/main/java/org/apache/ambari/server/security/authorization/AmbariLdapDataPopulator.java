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

import java.util.Collection;
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.filter.AndFilter;
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
    try {
      final LdapTemplate ldapTemplate = loadLdapTemplate();
      ldapTemplate.list(ldapServerProperties.getBaseDN());
      return true;
    } catch (Exception ex) {
      LOG.error("Could not connect to LDAP server", ex);
      return false;
    }
  }

  /**
   * Retrieves a key-value map of all LDAP groups.
   *
   * @return map of GroupName-Synced pairs
   */
  public Map<String, Boolean> getLdapGroupsSyncInfo() {
    final Map<String, Boolean> ldapGroups = new HashMap<String, Boolean>();
    final Map<String, Group> internalGroupsMap = getInternalGroups();
    final Set<String> externalGroups = getExternalLdapGroupNames();
    for (String externalGroup : externalGroups) {
      if (internalGroupsMap.containsKey(externalGroup)
          && internalGroupsMap.get(externalGroup).isLdapGroup()) {
        ldapGroups.put(externalGroup, true);
      } else {
        ldapGroups.put(externalGroup, false);
      }
    }

    return ldapGroups;
  }

  /**
   * Retrieves a key-value map of all LDAP users.
   *
   * @return map of UserName-Synced pairs.
   */
  public Map<String, Boolean> getLdapUsersSyncInfo() {
    final Map<String, Boolean> ldapUsers = new HashMap<String, Boolean>();
    final List<User> internalUsers = users.getAllUsers();
    final Map<String, User> internalUsersMap = new HashMap<String, User>();
    for (User user : internalUsers) {
      internalUsersMap.put(user.getUserName(), user);
    }
    final Set<String> externalUsers = getExternalLdapUserNames();
    for (String externalUser : externalUsers) {
      if (internalUsersMap.containsKey(externalUser)
          && internalUsersMap.get(externalUser).isLdapUser()) {
        ldapUsers.put(externalUser, true);
      } else {
        ldapUsers.put(externalUser, false);
      }
    }

    return ldapUsers;
  }

  /**
   * Performs synchronization of given sets of usernames and groupnames.
   *
   * @param users set of users to synchronize
   * @param groups set of groups to synchronize
   * @throws AmbariException if synchronization failed for any reason
   */
  public void synchronizeLdapUsersAndGroups(Set<String> users,
      Set<String> groups) throws AmbariException {
    // validate request
    final Set<String> externalUsers = getExternalLdapUserNames();
    for (String user : users) {
      if (!externalUsers.contains(user)) {
        throw new AmbariException("Couldn't sync LDAP user " + user
            + ", it doesn't exist");
      }
    }
    final Set<String> externalGroups = getExternalLdapGroupNames();
    for (String group : groups) {
      if (!externalGroups.contains(group)) {
        throw new AmbariException("Couldn't sync LDAP group " + group
            + ", it doesn't exist");
      }
    }

    // processing groups
    final Map<String, Group> internalGroupsMap = getInternalGroups();
    for (String groupName : groups) {
      if (internalGroupsMap.containsKey(groupName)) {
        final Group group = internalGroupsMap.get(groupName);
        if (!group.isLdapGroup()) {
          this.users.setGroupLdap(groupName);
        }
      } else {
        this.users.createGroup(groupName);
        this.users.setGroupLdap(groupName);
      }
      refreshGroupMembers(groupName);
      internalGroupsMap.remove(groupName);
    }
    for (Entry<String, Group> internalGroup : internalGroupsMap.entrySet()) {
      if (internalGroup.getValue().isLdapGroup()) {
        this.users.removeGroup(internalGroup.getValue());
      }
    }

    cleanUpLdapUsersWithoutGroup();

    // processing users
    final Map<String, User> internalUsersMap = getInternalUsers();
    for (String userName : users) {
      if (internalUsersMap.containsKey(userName)) {
        final User user = internalUsersMap.get(userName);
        if (!user.isLdapUser()) {
          this.users.setUserLdap(userName);
        }
      } else {
        this.users.createUser(userName, "", true, false);
        this.users.setUserLdap(userName);
      }
    }

  }

  /**
   * Check group members of the synced group: add missing ones and remove the ones absent in external LDAP.
   *
   * @param groupName group name
   * @throws AmbariException if group refresh failed
   */
  protected void refreshGroupMembers(String groupName) throws AmbariException {
    final Set<String> externalMembers = getExternalLdapGroupMembers(groupName);
    final Map<String, User> internalUsers = getInternalUsers();
    final Map<String, User> internalMembers = getInternalMembers(groupName);
    for (String externalMember: externalMembers) {
      if (internalUsers.containsKey(externalMember)) {
        final User user = internalUsers.get(externalMember);
        if (!user.isLdapUser()) {
          users.setUserLdap(externalMember);
        }
        if (!internalMembers.containsKey(externalMember)) {
          users.addMemberToGroup(groupName, externalMember);
        }
        internalMembers.remove(externalMember);
        internalUsers.remove(externalMember);
      } else {
        users.createUser(externalMember, "");
        users.setUserLdap(externalMember);
        users.addMemberToGroup(groupName, externalMember);
      }
    }
    for (Entry<String, User> userToBeUnsynced: internalMembers.entrySet()) {
      final User user = userToBeUnsynced.getValue();
      users.removeMemberFromGroup(groupName, user.getUserName());
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
   * @return set of user names
   */
  protected Set<String> getExternalLdapGroupNames() {
    final Set<String> groups = new HashSet<String>();
    final LdapTemplate ldapTemplate = loadLdapTemplate();
    final EqualsFilter equalsFilter = new EqualsFilter("objectClass",
        ldapServerProperties.getGroupObjectClass());
    String baseDn = ldapServerProperties.getBaseDN();
    ldapTemplate.search(baseDn, equalsFilter.encode(), new AttributesMapper() {

      public Object mapFromAttributes(Attributes attributes)
          throws NamingException {
        groups.add(attributes.get(ldapServerProperties.getGroupNamingAttr())
            .get().toString().toLowerCase());
        return null;
      }
    });
    return groups;
  }

  /**
   * Retrieves users from external LDAP server.
   *
   * @return set of user names
   */
  protected Set<String> getExternalLdapUserNames() {
    final Set<String> users = new HashSet<String>();
    final LdapTemplate ldapTemplate = loadLdapTemplate();
    final EqualsFilter equalsFilter = new EqualsFilter("objectClass",
        ldapServerProperties.getUserObjectClass());
    String baseDn = ldapServerProperties.getBaseDN();
    ldapTemplate.search(baseDn, equalsFilter.encode(), new AttributesMapper() {

      public Object mapFromAttributes(Attributes attributes)
          throws NamingException {
        users.add(attributes.get(ldapServerProperties.getUsernameAttribute())
            .get().toString().toLowerCase());
        return null;
      }
    });
    return users;
  }

  /**
   * Retrieves members of the specified group from external LDAP server.
   *
   * @param groupName group name
   * @return set of group names
   */
  protected Set<String> getExternalLdapGroupMembers(String groupName) {
    final Set<String> members = new HashSet<String>();
    final LdapTemplate ldapTemplate = loadLdapTemplate();
    final AndFilter andFilter = new AndFilter();
    andFilter.and(new EqualsFilter("objectClass", ldapServerProperties.getGroupObjectClass()));
    andFilter.and(new EqualsFilter(ldapServerProperties.getGroupNamingAttr(), groupName));
    String baseDn = ldapServerProperties.getBaseDN();
    ldapTemplate.search(baseDn, andFilter.encode(), new ContextMapper() {

      public Object mapFromContext(Object ctx) {
        final DirContextAdapter adapter  = (DirContextAdapter) ctx;
        for (String uniqueMember: adapter.getStringAttributes(ldapServerProperties.getGroupMembershipAttr())) {
          final DirContextAdapter userAdapter = (DirContextAdapter) ldapTemplate.lookup(uniqueMember);
          members.add(userAdapter.getStringAttribute(ldapServerProperties.getUsernameAttribute()).toLowerCase());
        }
        return null;
      }
    });
    return members;
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
    final Map<String, User> internalMembersMap = new HashMap<String, User>();
    for (User user : internalMembers) {
      internalMembersMap.put(user.getUserName(), user);
    }
    return internalMembersMap;
  }

  /**
   * Checks LDAP configuration for changes and reloads LDAP template if they occured.
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
        ldapContextSource
            .setPassword(ldapServerProperties.getManagerPassword());
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
