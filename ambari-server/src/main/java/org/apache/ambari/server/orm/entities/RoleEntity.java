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

import javax.persistence.*;
import java.util.Set;

@javax.persistence.Table(name = "roles")
@Entity
public class RoleEntity {

  @Column(name = "role_name")
  @Id
  private String roleName;

  @JoinTable(name = "user_roles",
      joinColumns = {@JoinColumn(name = "role_name", referencedColumnName = "role_name")},
      inverseJoinColumns = {@JoinColumn(name = "user_id", referencedColumnName = "user_id")})
  @ManyToMany(cascade = CascadeType.ALL)
  private Set<UserEntity> userEntities;

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

  public Set<org.apache.ambari.server.orm.entities.UserEntity> getUserEntities() {
    return userEntities;
  }

  public void setUserEntities(Set<org.apache.ambari.server.orm.entities.UserEntity> userEntities) {
    this.userEntities = userEntities;
  }
}
