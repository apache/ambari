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
import javax.persistence.Table;
import javax.persistence.TableGenerator;

/**
 * Represents an admin permission.
 */
@Table(name = "adminpermission")
@Entity
@TableGenerator(name = "permission_id_generator",
    table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value"
    , pkColumnValue = "permission_id_seq"
    , initialValue = 5
)
public class PermissionEntity {

  /**
   * Admin permission id constants.
   */
  public static final int AMBARI_ADMIN_PERMISSION    = 1;
  public static final int CLUSTER_READ_PERMISSION    = 2;
  public static final int CLUSTER_OPERATE_PERMISSION = 3;
  public static final int VIEW_USE_PERMISSION        = 4;

  /**
   * Admin permission name constants.
   */
  public static final String AMBARI_ADMIN_PERMISSION_NAME    = "AMBARI.ADMIN";
  public static final String CLUSTER_READ_PERMISSION_NAME    = "CLUSTER.READ";
  public static final String CLUSTER_OPERATE_PERMISSION_NAME = "CLUSTER.OPERATE";
  public static final String VIEW_USE_PERMISSION_NAME        = "VIEW.USE";

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

    return !(id != null ? !id.equals(that.id) : that.id != null) &&
        !(permissionName != null ? !permissionName.equals(that.permissionName) : that.permissionName != null) &&
        !(resourceType != null ? !resourceType.equals(that.resourceType) : that.resourceType != null);
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (permissionName != null ? permissionName.hashCode() : 0);
    result = 31 * result + (resourceType != null ? resourceType.hashCode() : 0);
    return result;
  }
}
