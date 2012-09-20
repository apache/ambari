package org.apache.ambari.server.orm.entities;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Set;

@javax.persistence.Table(name = "users", schema = "ambari", catalog = "")
@Entity
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

  private String userPassword;

  @javax.persistence.Column(name = "user_password")
  @Basic
  public String getUserPassword() {
    return userPassword;
  }

  public void setUserPassword(String userPassword) {
    this.userPassword = userPassword;
  }

  private Boolean ldapUser;

  @javax.persistence.Column(name = "ldap_user")
  @Basic
  public Boolean getLdapUser() {
    return ldapUser;
  }

  public void setLdapUser(Boolean ldapUser) {
    this.ldapUser = ldapUser;
  }

  private Timestamp createTime;

  @javax.persistence.Column(name = "create_time")
  @Basic
  public Timestamp getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Timestamp createTime) {
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

  @javax.persistence.JoinTable(name = "user_roles", catalog = "", schema = "ambari", joinColumns = {@JoinColumn(name = "user_name")}, inverseJoinColumns = {@JoinColumn(name = "user_name")})
  @ManyToMany
  public Set<RoleEntity> getRoleEntities() {
    return roleEntities;
  }

  public void setRoleEntities(Set<RoleEntity> roleEntities) {
    this.roleEntities = roleEntities;
  }
}
