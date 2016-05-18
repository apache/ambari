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

/**
 * Represents a principal type.
 */
@Table(name = "adminprincipaltype")
@Entity
@TableGenerator(name = "principal_type_id_generator",
    table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value"
    , pkColumnValue = "principal_type_id_seq"
    , initialValue = 100
)
public class PrincipalTypeEntity {

  /**
   * Principal type constants.
   */
  public static final int USER_PRINCIPAL_TYPE  = 1;
  public static final int GROUP_PRINCIPAL_TYPE = 2;
  public static final int CLUSTER_ADMINISTRATOR_PRINCIPAL_TYPE = 3;
  public static final int CLUSTER_OPERATOR_PRINCIPAL_TYPE = 4;
  public static final int CLUSTER_USER_PRINCIPAL_TYPE = 5;
  public static final int SERVICE_ADMINISTRATOR_PRINCIPAL_TYPE = 6;
  public static final int SERVICE_OPERATOR_PRINCIPAL_TYPE = 7;

  public static final String USER_PRINCIPAL_TYPE_NAME  = "USER";
  public static final String GROUP_PRINCIPAL_TYPE_NAME = "GROUP";
  public static final String CLUSTER_ADMINISTRATOR_PRINCIPAL_TYPE_NAME = "ALL.CLUSTER.ADMINISTRATOR";
  public static final String CLUSTER_OPERATOR_PRINCIPAL_TYPE_NAME = "ALL.CLUSTER.OPERATOR";
  public static final String CLUSTER_USER_PRINCIPAL_TYPE_NAME = "ALL.CLUSTER.USER";
  public static final String SERVICE_ADMINISTRATOR_PRINCIPAL_TYPE_NAME = "ALL.SERVICE.ADMINISTRATOR";
  public static final String SERVICE_OPERATOR_PRINCIPAL_TYPE_NAME = "ALL.SERVICE.OPERATOR";

  /**
   * The type id.
   */
  @Id
  @Column(name = "principal_type_id")
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "principal_type_id_generator")
  private Integer id;

  /**
   * The type name.
   */
  @Column(name = "principal_type_name")
  private String name;


  // ----- PrincipalTypeEntity -----------------------------------------------

  /**
   * Get the principal type id.
   *
   * @return the principal type id.
   */
  public Integer getId() {
    return id;
  }

  /**
   * Set the principal type id.
   *
   * @param id  the type id.
   */
  public void setId(Integer id) {
    this.id = id;
  }

  /**
   * Get the principal type name.
   *
   * @return the principal type name;
   */
  public String getName() {
    return name;
  }

  /**
   * Set the principal type name.
   *
   * @param name  the principal type name.
   */
  public void setName(String name) {
    this.name = name;
  }


  // ------ Object overrides -------------------------------------------------

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    PrincipalTypeEntity that = (PrincipalTypeEntity) o;

    if (!id.equals(that.id)) return false;
    if (name != null ? !name.equals(that.name) : that.name != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = id.hashCode();
    result = 31 * result + (name != null ? name.hashCode() : 0);
    return result;
  }
}
