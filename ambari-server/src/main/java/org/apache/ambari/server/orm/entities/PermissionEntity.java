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


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

/**
 * Represents an admin permission.
 */
@Table(name = "adminpermission")
@Entity
@TableGenerator(name = "permission_id_generator",
    table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "value"
    , pkColumnValue = "permission_id_seq"
    , initialValue = 5
    , allocationSize = 1
)

@NamedQueries({
    @NamedQuery(name = "permissionByName", query = "SELECT permission_entity FROM PermissionEntity permission_entity where permission_entity.permissionName=:permissionname")
})
public class PermissionEntity {

  /**
   * The permission id.
   */
  @Id
  @Column(name = "permission_id")
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "permission_id_generator")
  private Integer id;


  /**
   * The permission name.
   */
  @Column(name = "permission_name")
  private String permissionName;

  @ManyToOne
  @JoinColumns({
      @JoinColumn(name = "resource_type_id", referencedColumnName = "resource_type_id", nullable = false),
  })
  private ResourceTypeEntity resourceType;


  // ----- PermissionEntity ---------------------------------------------------

  /**
   * Get the permission id.
   *
   * @return the permission id.
   */
  public Integer getId() {
    return id;
  }

  /**
   * Set the permission id.
   *
   * @param id  the type id.
   */
  public void setId(Integer id) {
    this.id = id;
  }

  /**
   * Get the permission name.
   *
   * @return the permission name
   */
  public String getPermissionName() {
    return permissionName;
  }

  /**
   * Set the permission name.
   *
   * @param permissionName  the permission name
   */
  public void setPermissionName(String permissionName) {
    this.permissionName = permissionName;
  }

  /**
   * Get the resource type entity.
   *
   * @return  the resource type entity
   */
  public ResourceTypeEntity getResourceType() {
    return resourceType;
  }

  /**
   * Set the resource type entity.
   *
   * @param resourceType  the resource type entity
   */
  public void setResourceType(ResourceTypeEntity resourceType) {
    this.resourceType = resourceType;
  }


  // ----- Object overrides --------------------------------------------------

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    PermissionEntity that = (PermissionEntity) o;

    if (!id.equals(that.id)) return false;
    if (permissionName != null ? !permissionName.equals(that.permissionName) : that.permissionName != null)
      return false;
    if (resourceType != null ? !resourceType.equals(that.resourceType) : that.resourceType != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = id.hashCode();
    result = 31 * result + (permissionName != null ? permissionName.hashCode() : 0);
    result = 31 * result + (resourceType != null ? resourceType.hashCode() : 0);
    return result;
  }
}
