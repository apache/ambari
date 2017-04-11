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
import java.util.Date;
import java.util.List;

import org.apache.ambari.server.orm.entities.MemberEntity;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.springframework.security.core.GrantedAuthority;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;


/**
 * Describes user of web-services
 */
@ApiModel
public class User {
  final int userId;
  final String userName;
  final boolean ldapUser;
  final UserType userType;
  final Date createTime;
  final boolean active;
  final Collection<String> groups = new ArrayList<>();
  boolean admin = false;
  final List<GrantedAuthority> authorities = new ArrayList<>();

  public User(UserEntity userEntity) {
    userId = userEntity.getUserId();
    userName = userEntity.getUserName();
    createTime = userEntity.getCreateTime();
    userType = userEntity.getUserType();
    ldapUser = userEntity.getLdapUser();
    active = userEntity.getActive();
    for (MemberEntity memberEntity : userEntity.getMemberEntities()) {
      groups.add(memberEntity.getGroup().getGroupName());
    }
    for (PrivilegeEntity privilegeEntity: userEntity.getPrincipal().getPrivileges()) {
      if (privilegeEntity.getPermission().getPermissionName().equals(PermissionEntity.AMBARI_ADMINISTRATOR_PERMISSION_NAME)) {
        admin = true;
        break;
      }
    }
  }

  @ApiModelProperty(hidden = true)
  public int getUserId() {
    return userId;
  }

  @ApiModelProperty(name = "Users/user_name",required = true, access = "public", notes = "username containing only lowercase letters")
  public String getUserName() {
    return userName;
  }

  @ApiModelProperty(name = "Users/ldap_user")
  public boolean isLdapUser() {
    return ldapUser;
  }

  @ApiModelProperty(name = "Users/user_type")
  public UserType getUserType() {
    return userType;
  }

  @ApiModelProperty(hidden = true)
  public Date getCreateTime() {
    return createTime;
  }

  @ApiModelProperty(name = "Users/active")
  public boolean isActive() {
    return active;
  }

  @ApiModelProperty(name = "Users/admin")
  public boolean isAdmin() {
    return admin;
  }

  @ApiModelProperty(name = "Users/groups")
  public Collection<String> getGroups() {
    return groups;
  }

  @Override
  public String toString() {
    return "[" + getUserType() + "]" + userName;
  }
}
