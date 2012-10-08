package org.apache.ambari.server.orm.entities;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Set;

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

@IdClass(UserEntityPK.class)
@javax.persistence.Table(name = "users", schema = "ambari", catalog = "")
@Entity
@NamedQueries({
        @NamedQuery(name = "localUserByName", query = "SELECT user FROM UserEntity user where lower(user.userName)=:username AND user.ldapUser=false"),
        @NamedQuery(name = "ldapUserByName", query = "SELECT user FROM UserEntity user where lower(user.userName)=:username AND user.ldapUser=true")
})
public class UserEntity {

  private String userName;

  @javax.persistence.Column(name = "user_name")
  @Id
  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  private Boolean ldapUser = false;

  @javax.persistence.Column(name = "ldap_user")
  @Id
  public Boolean getLdapUser() {
    return ldapUser;
  }

  public void setLdapUser(Boolean ldapUser) {
    this.ldapUser = ldapUser;
  }

  private String userPassword;

  @javax.persistence.Column(name = "user_password")
  @Basic
  public String getUserPassword() {
    return userPassword;
  }

  public void setUserPassword(String userPassword) {
    this.userPassword = userPassword;
  }

  private Date createTime = new Date();

  @javax.persistence.Column(name = "create_time")
  @Basic
  @Temporal(value = TemporalType.TIMESTAMP)
  public Date getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    UserEntity that = (UserEntity) o;

    if (createTime != null ? !createTime.equals(that.createTime) : that.createTime != null) return false;
    if (ldapUser != null ? !ldapUser.equals(that.ldapUser) : that.ldapUser != null) return false;
    if (userName != null ? !userName.equals(that.userName) : that.userName != null) return false;
    if (userPassword != null ? !userPassword.equals(that.userPassword) : that.userPassword != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = userName != null ? userName.hashCode() : 0;
    result = 31 * result + (userPassword != null ? userPassword.hashCode() : 0);
    result = 31 * result + (ldapUser != null ? ldapUser.hashCode() : 0);
    result = 31 * result + (createTime != null ? createTime.hashCode() : 0);
    return result;
  }

  private Set<RoleEntity> roleEntities;

  @javax.persistence.JoinTable(name = "user_roles", catalog = "", schema = "ambari",
          joinColumns = {@JoinColumn(name = "user_name", referencedColumnName = "user_name"),
                  @JoinColumn(name = "ldap_user", referencedColumnName = "ldap_user")},
          inverseJoinColumns = {@JoinColumn(name = "role_name", referencedColumnName = "role_name")})
  @ManyToMany
  public Set<RoleEntity> getRoleEntities() {
    return roleEntities;
  }

  public void setRoleEntities(Set<RoleEntity> roleEntities) {
    this.roleEntities = roleEntities;
  }
}
