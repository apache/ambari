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
 * Represents an admin principal.
 */
@Table(name = "adminprincipal")
@Entity
@TableGenerator(name = "principal_id_generator",
    table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "value"
    , pkColumnValue = "principal_id_seq"
    , initialValue = 2
    , allocationSize = 1
)
public class PrincipalEntity {

  /**
   * The type id.
   */
  @Id
  @Column(name = "principal_id")
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "principal_id_generator")
  private Long id;

  @ManyToOne
  @JoinColumns({
      @JoinColumn(name = "principal_type_id", referencedColumnName = "principal_type_id", nullable = false)
  })
  private PrincipalTypeEntity principalType;


  // ----- PrincipalEntity ---------------------------------------------------

  /**
   * Get the principal type id.
   *
   * @return the principal type id.
   */
  public Long getId() {
    return id;
  }

  /**
   * Set the principal id.
   *
   * @param id  the type id.
   */
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Get the principal type entity.
   *
   * @return  the principal type entity
   */
  public PrincipalTypeEntity getPrincipalType() {
    return principalType;
  }

  /**
   * Set the principal type entity.
   *
   * @param principalType  the principal type entity
   */
  public void setPrincipalType(PrincipalTypeEntity principalType) {
    this.principalType = principalType;
  }


  // ----- Object overrides --------------------------------------------------

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    PrincipalEntity that = (PrincipalEntity) o;

    return id.equals(that.id) && !(principalType != null ?
        !principalType.equals(that.principalType) : that.principalType != null);
  }

  @Override
  public int hashCode() {
    int result = id.hashCode();
    result = 31 * result + (principalType != null ? principalType.hashCode() : 0);
    return result;
  }
}
