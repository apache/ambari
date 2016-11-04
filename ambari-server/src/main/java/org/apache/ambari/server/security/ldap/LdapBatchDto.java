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

import java.util.HashSet;
import java.util.Set;

/**
 * Contains information for batch database update on LDAP synchronization.
 */
public class LdapBatchDto {
  private final Set<String> groupsToBecomeLdap = new HashSet<String>();
  private final Set<String> groupsToBeCreated = new HashSet<String>();
  private final Set<String> groupsToBeRemoved = new HashSet<String>();
  private final Set<String> groupsProcessedInternal = new HashSet<>();
  private final Set<String> usersSkipped = new HashSet<String>();
  private final Set<String> usersToBecomeLdap = new HashSet<String>();
  private final Set<String> usersToBeCreated = new HashSet<String>();
  private final Set<String> usersToBeRemoved = new HashSet<String>();
  private final Set<LdapUserGroupMemberDto> membershipToAdd = new HashSet<LdapUserGroupMemberDto>();
  private final Set<LdapUserGroupMemberDto> membershipToRemove = new HashSet<LdapUserGroupMemberDto>();

  public Set<String> getUsersSkipped() {
    return usersSkipped;
  }

  public Set<String> getGroupsToBecomeLdap() {
    return groupsToBecomeLdap;
  }

  public Set<String> getGroupsToBeCreated() {
    return groupsToBeCreated;
  }

  public Set<String> getUsersToBecomeLdap() {
    return usersToBecomeLdap;
  }

  public Set<String> getUsersToBeCreated() {
    return usersToBeCreated;
  }

  public Set<LdapUserGroupMemberDto> getMembershipToAdd() {
    return membershipToAdd;
  }

  public Set<LdapUserGroupMemberDto> getMembershipToRemove() {
    return membershipToRemove;
  }

  public Set<String> getGroupsToBeRemoved() {
    return groupsToBeRemoved;
  }

  public Set<String> getUsersToBeRemoved() {
    return usersToBeRemoved;
  }

  public Set<String> getGroupsProcessedInternal() {
    return groupsProcessedInternal;
  }
}
