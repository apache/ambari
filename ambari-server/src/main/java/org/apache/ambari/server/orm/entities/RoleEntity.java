package org.apache.ambari.server.orm.entities;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import java.util.Set;

@javax.persistence.Table(name = "roles", schema = "ambari", catalog = "")
@Entity
public class RoleEntity {

  private String roleName;

  @javax.persistence.Column(name = "role_name")
  @Id
  public String getRoleName() {
    return roleName;
  }

  public void setRoleName(String roleName) {
    this.roleName = roleName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RoleEntity that = (RoleEntity) o;

    if (roleName != null ? !roleName.equals(that.roleName) : that.roleName != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return roleName != null ? roleName.hashCode() : 0;
  }

  private Set<UserEntity> userEntities;

  @ManyToMany(mappedBy = "roleEntities")
  public Set<UserEntity> getUserEntities() {
    return userEntities;
  }

  public void setUserEntities(Set<UserEntity> userEntities) {
    this.userEntities = userEntities;
  }
}
