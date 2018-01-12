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

package org.apache.ambari.server.orm.entities;

import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

@Entity
@Table(name = "blueprint_service")
@TableGenerator(name = "blueprint_service_id_generator", table = "ambari_sequences", pkColumnName = "sequence_name",
  valueColumnName = "sequence_value", pkColumnValue = "blueprint_service_id_seq", initialValue = 1)
public class BlueprintServiceEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "blueprint_service_id_generator")
  @Column(name = "id", nullable = false, updatable = false)
  private Long id;

  @ManyToOne()
  @JoinColumn(name = "mpack_ref_id", referencedColumnName = "id", nullable = false)
  private BlueprintMpackReferenceEntity mpackReference;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "service")
  private Collection<BlueprintServiceConfigEntity> configurations = new ArrayList<>();

  private String name;

  private String type;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public BlueprintMpackReferenceEntity getMpackReference() {
    return mpackReference;
  }

  public void setMpackReference(BlueprintMpackReferenceEntity mpackReference) {
    this.mpackReference = mpackReference;
  }

  public Collection<BlueprintServiceConfigEntity> getConfigurations() {
    return configurations;
  }

  public void setConfigurations(Collection<BlueprintServiceConfigEntity> configurations) {
    this.configurations = configurations;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }
}
