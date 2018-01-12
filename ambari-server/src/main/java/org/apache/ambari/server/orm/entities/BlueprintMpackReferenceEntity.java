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
@Table(name = "blueprint_mpack_reference")
@TableGenerator(name = "blueprint_mpack_reference_id_generator", table = "ambari_sequences", pkColumnName = "sequence_name",
  valueColumnName = "sequence_value", pkColumnValue = "blueprint_mpack_ref_id_seq", initialValue = 1)
public class BlueprintMpackReferenceEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "blueprint_mpack_reference_id_generator")
  @Column(name = "id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "mpack_name")
  private String mpackName;

  @Column(name = "mpack_version")
  private String mpackVersion;

  @Column(name = "mpack_uri")
  private String mpackUri;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "mpackReference")
  private Collection<BlueprintServiceEntity> serviceInstances = new ArrayList<>();

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "mpackReference")
  private Collection<BlueprintMpackConfigEntity> configurations = new ArrayList<>();


  @ManyToOne
  @JoinColumn(name = "blueprint_name", referencedColumnName = "blueprint_name", nullable = false)
  private BlueprintEntity blueprint;

  public Collection<BlueprintServiceEntity> getServiceInstances() {
    return serviceInstances;
  }

  public void setServiceInstances(Collection<BlueprintServiceEntity> serviceInstances) {
    this.serviceInstances = serviceInstances;
  }

  public String getMpackName() {
    return mpackName;
  }

  public void setMpackName(String mpackName) {
    this.mpackName = mpackName;
  }

  public String getMpackVersion() {
    return mpackVersion;
  }

  public void setMpackVersion(String mpackVersion) {
    this.mpackVersion = mpackVersion;
  }

  public String getMpackUri() {
    return mpackUri;
  }

  public void setMpackUri(String mpackUri) {
    this.mpackUri = mpackUri;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public BlueprintEntity getBlueprint() {
    return blueprint;
  }

  public void setBlueprint(BlueprintEntity blueprint) {
    this.blueprint = blueprint;
  }

  public Collection<BlueprintMpackConfigEntity> getConfigurations() {
    return configurations;
  }

  public void setConfigurations(Collection<BlueprintMpackConfigEntity> configurations) {
    this.configurations = configurations;
  }
}
