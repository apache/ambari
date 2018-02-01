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

import javax.annotation.Nullable;
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

/**
 * Entity to encapsulate a blueprint's use of an mpack. It contains the mpack name, version, url
 *
 *
 * referencing a management pack from the blueprint. The reference contains the name and
 * the version of the mpack, but no direct database reference to the mpack entity as a blueprint
 * can be saved without the referenced mpack being present.
 */
@Entity
@Table(name = "blueprint_mpack_instance")
@TableGenerator(name = "blueprint_mpack_instance_id_generator", table = "ambari_sequences", pkColumnName = "sequence_name",
  valueColumnName = "sequence_value", pkColumnValue = "blueprint_mpack_instance_id_seq", initialValue = 1)
public class BlueprintMpackInstanceEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "blueprint_mpack_instance_id_generator")
  @Column(name = "id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "mpack_name", nullable = false)
  private String mpackName;

  @Column(name = "mpack_version", nullable = false)
  private String mpackVersion;

  @Column(name = "mpack_uri")
  private String mpackUri;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "mpackInstance")
  private Collection<BlueprintServiceEntity> serviceInstances = new ArrayList<>();

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "mpackInstance")
  private Collection<BlueprintMpackConfigEntity> configurations = new ArrayList<>();

  @ManyToOne
  @JoinColumn(name = "mpack_id", referencedColumnName = "id")
  private MpackEntity mpackEntity;

  @ManyToOne
  @JoinColumn(name = "blueprint_name", referencedColumnName = "blueprint_name", nullable = false)
  private BlueprintEntity blueprint;

  /**
   * @return the service instances belonging to this mpack
   */
  public Collection<BlueprintServiceEntity> getServiceInstances() {
    return serviceInstances;
  }

  /**
   * @param serviceInstances the service instances belonging to this mpack
   */
  public void setServiceInstances(Collection<BlueprintServiceEntity> serviceInstances) {
    this.serviceInstances = serviceInstances;
  }

  /**
   * @return the name of the mpack
   */
  public String getMpackName() {
    return mpackName;
  }

  /**
   * @param mpackName the name of the mpack
   */
  public void setMpackName(String mpackName) {
    this.mpackName = mpackName;
  }

  /**
   * @return the version of the mpack
   */
  public String getMpackVersion() {
    return mpackVersion;
  }

  /**
   * @param mpackVersion the version of the mpack
   */
  public void setMpackVersion(String mpackVersion) {
    this.mpackVersion = mpackVersion;
  }

  /**
   * @return the uri of the mpack
   */
  public String getMpackUri() {
    return mpackUri;
  }

  /**
   * @param mpackUri the uri of the mpack
   */
  public void setMpackUri(String mpackUri) {
    this.mpackUri = mpackUri;
  }

  /**
   * @return the database id
   */
  public Long getId() {
    return id;
  }

  /**
   * @param id the database id
   */
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * @return the blueprint
   */
  public BlueprintEntity getBlueprint() {
    return blueprint;
  }

  /**
   * @param blueprint the blueprint
   */
  public void setBlueprint(BlueprintEntity blueprint) {
    this.blueprint = blueprint;
  }

  /**
   * @return the mpack level configurations for this mpack
   */
  public Collection<BlueprintMpackConfigEntity> getConfigurations() {
    return configurations;
  }

  /**
   * @param configurations the mpack level configurations for this mpack
   */
  public void setConfigurations(Collection<BlueprintMpackConfigEntity> configurations) {
    this.configurations = configurations;
  }

  /**
   * @return the management pack entity associated with this blueprint. Can be {@null}
   */
  @Nullable
  public MpackEntity getMpackEntity() {
    return mpackEntity;
  }

  /**
   * @param mpackEntity the management pack entity to be associated with this blueprint.
   */
  public void setMpackEntity(MpackEntity mpackEntity) {
    this.mpackEntity = mpackEntity;
  }
}
