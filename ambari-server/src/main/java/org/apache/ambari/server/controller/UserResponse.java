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
package org.apache.ambari.server.controller;

import java.util.Collections;
import java.util.Set;

import org.apache.ambari.server.security.authorization.UserType;

import io.swagger.annotations.ApiModelProperty;

/**
 * Represents a user maintenance request.
 */
public class UserResponse implements ApiModel {

  private final String userName;
  private final UserType userType;
  private final boolean isLdapUser;
  private final boolean isActive;
  private final boolean isAdmin;
  private Set<String> groups = Collections.emptySet();

  public UserResponse(String userName, UserType userType, boolean isLdapUser, boolean isActive, boolean isAdmin) {
    this.userName = userName;
    this.userType = userType;
    this.isLdapUser = isLdapUser;
    this.isActive = isActive;
    this.isAdmin = isAdmin;
  }

  public UserResponse(String name, boolean isLdapUser, boolean isActive, boolean isAdmin) {
    this.userName = name;
    this.isLdapUser = isLdapUser;
    this.isActive = isActive;
    this.isAdmin = isAdmin;
    this.userType = UserType.LOCAL;
  }

  @ApiModelProperty(name = "Users/user_name",required = true)
  public String getUsername() {
    return userName;
  }

  @ApiModelProperty(name = "Users/groups")
  public Set<String> getGroups() {
    return groups;
  }

  public void setGroups(Set<String> groups) {
    this.groups = groups;
  }

  /**
   * @return the isLdapUser
   */
  @ApiModelProperty(name = "Users/ldap_user")
  public boolean isLdapUser() {
    return isLdapUser;
  }

  @ApiModelProperty(name = "Users/active")
  public boolean isActive() {
    return isActive;
  }

  @ApiModelProperty(name = "Users/admin")
  public boolean isAdmin() {
    return isAdmin;
  }

  @ApiModelProperty(name = "Users/user_type")
  public UserType getUserType() {
    return userType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    UserResponse that = (UserResponse) o;

    if (userName != null ? !userName.equals(that.userName) : that.userName != null) return false;
    return userType == that.userType;

  }

  @Override
  public int hashCode() {
    int result = userName != null ? userName.hashCode() : 0;
    result = 31 * result + (userType != null ? userType.hashCode() : 0);
    return result;
  }
}
