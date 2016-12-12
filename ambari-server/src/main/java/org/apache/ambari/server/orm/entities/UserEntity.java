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
package org.apache.ambari.server.orm.entities;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

import org.apache.ambari.server.security.authorization.UserType;

@Table(name = "users", uniqueConstraints = {@UniqueConstraint(columnNames = {"user_name", "user_type"})})
@Entity
@NamedQueries({
    @NamedQuery(name = "userByName", query = "SELECT user_entity from UserEntity user_entity " +
        "where lower(user_entity.userName)=:username"),
    @NamedQuery(name = "localUserByName", query = "SELECT user_entity FROM UserEntity user_entity " +
        "where lower(user_entity.userName)=:username AND " +
        "user_entity.userType=org.apache.ambari.server.security.authorization.UserType.LOCAL"),
    @NamedQuery(name = "ldapUserByName", query = "SELECT user_entity FROM UserEntity user_entity " +
        "where lower(user_entity.userName)=:username AND " +
        "user_entity.userType=org.apache.ambari.server.security.authorization.UserType.LDAP")
})
@TableGenerator(name = "user_id_generator",
    table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value"
    , pkColumnValue = "user_id_seq"
    , initialValue = 2
    , allocationSize = 500
    )
public class UserEntity {

  @Id
  @Column(name = "user_id")
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "user_id_generator")
  private Integer userId;

  @Column(name = "user_name")
  private String userName;

  @Column(name = "ldap_user")
  private Integer ldapUser = 0;

  @Column(name = "user_type")
  @Enumerated(EnumType.STRING)
  @Basic
  private UserType userType = UserType.LOCAL;

  @Column(name = "user_password")
  @Basic
  private String userPassword;

  @Column(name = "create_time")
  @Basic
  @Temporal(value = TemporalType.TIMESTAMP)
  private Date createTime = new Date();

  @Column(name = "active")
  private Integer active = 1;

  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
  private Set<MemberEntity> memberEntities = new HashSet<MemberEntity>();

  @OneToOne
  @JoinColumns({
      @JoinColumn(name = "principal_id", referencedColumnName = "principal_id", nullable = false),
  })
  private PrincipalEntity principal;

  @Column(name = "active_widget_layouts")
  private String activeWidgetLayouts;
  // ----- UserEntity --------------------------------------------------------

  public Integer getUserId() {
    return userId;
  }

  public void setUserId(Integer userId) {
    this.userId = userId;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public Boolean getLdapUser() {
    return ldapUser == 0 ? Boolean.FALSE : Boolean.TRUE;
  }

  public void setLdapUser(Boolean ldapUser) {
    if (ldapUser == null) {
      this.ldapUser = null;
    } else {
      this.ldapUser = ldapUser ? 1 : 0;
      this.userType = ldapUser ? UserType.LDAP : UserType.LOCAL;
    }
  }

  public UserType getUserType() {
    return userType;
  }

  public void setUserType(UserType userType) {
    this.userType = userType;
  }

  public String getUserPassword() {
    return userPassword;
  }

  public void setUserPassword(String userPassword) {
    this.userPassword = userPassword;
  }

  public Date getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }

  public Set<MemberEntity> getMemberEntities() {
    return memberEntities;
  }

  public void setMemberEntities(Set<MemberEntity> memberEntities) {
    this.memberEntities = memberEntities;
  }

  public Boolean getActive() {
    return active == 0 ? Boolean.FALSE : Boolean.TRUE;
  }

  public void setActive(Boolean active) {
    if (active == null) {
      this.active = null;
    } else {
      this.active = active ? 1 : 0;
    }
  }

  /**
   * Get the admin principal entity.
   *
   * @return the principal entity
   */
  public PrincipalEntity getPrincipal() {
    return principal;
  }

  /**
   * Set the admin principal entity.
   *
   * @param principal  the principal entity
   */
  public void setPrincipal(PrincipalEntity principal) {
    this.principal = principal;
  }

  public String getActiveWidgetLayouts() {
    return activeWidgetLayouts;
  }

  public void setActiveWidgetLayouts(String activeWidgetLayouts) {
    this.activeWidgetLayouts = activeWidgetLayouts;
  }

// ----- Object overrides --------------------------------------------------

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    UserEntity that = (UserEntity) o;

    if (userId != null ? !userId.equals(that.userId) : that.userId != null) return false;
    if (createTime != null ? !createTime.equals(that.createTime) : that.createTime != null) return false;
    if (ldapUser != null ? !ldapUser.equals(that.ldapUser) : that.ldapUser != null) return false;
    if (userType != null ? !userType.equals(that.userType) : that.userType != null) return false;
    if (userName != null ? !userName.equals(that.userName) : that.userName != null) return false;
    if (userPassword != null ? !userPassword.equals(that.userPassword) : that.userPassword != null) return false;
    if (active != null ? !active.equals(that.active) : that.active != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = userId != null ? userId.hashCode() : 0;
    result = 31 * result + (userName != null ? userName.hashCode() : 0);
    result = 31 * result + (userPassword != null ? userPassword.hashCode() : 0);
    result = 31 * result + (ldapUser != null ? ldapUser.hashCode() : 0);
    result = 31 * result + (userType != null ? userType.hashCode() : 0);
    result = 31 * result + (createTime != null ? createTime.hashCode() : 0);
    result = 31 * result + (active != null ? active.hashCode() : 0);
    return result;
  }
}
