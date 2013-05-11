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

import org.apache.ambari.server.orm.entities.RoleEntity;
import org.apache.ambari.server.orm.entities.UserEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

/**
 * Describes user of web-services
 */
public class User {
  final int userId;
  final String userName;
  final boolean ldapUser;
  final Date createTime;
  final Collection<String> roles = new ArrayList<String>();

  User(UserEntity userEntity) {
    userId = userEntity.getUserId();
    userName = userEntity.getUserName();
    createTime = userEntity.getCreateTime();
    ldapUser = userEntity.getLdapUser();
    for (RoleEntity roleEntity : userEntity.getRoleEntities()) {
      roles.add(roleEntity.getRoleName());
    }
  }

  public int getUserId() {
    return userId;
  }

  public String getUserName() {
    return userName;
  }

  public boolean isLdapUser() {
    return ldapUser;
  }

  public Date getCreateTime() {
    return createTime;
  }

  public Collection<String> getRoles() {
    return roles;
  }

  @Override
  public String toString() {
    return (ldapUser ? "[LDAP]" : "[LOCAL]") + userName;
  }
}
