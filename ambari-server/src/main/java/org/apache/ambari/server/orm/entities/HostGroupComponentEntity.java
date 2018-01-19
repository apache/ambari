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
 * Represents a Host Group Component which is embedded in a Blueprint.
 */
@Entity
@Table(name = "hostgroup_component")
@TableGenerator(name = "hostgroup_component_id_generator", table = "ambari_sequences", pkColumnName = "sequence_name",
  valueColumnName = "sequence_value", pkColumnValue = "hostgroup_component_id_seq", initialValue = 1)
public class HostGroupComponentEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "hostgroup_component_id_generator")
  @Column(name = "id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "hostgroup_name", nullable = false, insertable = false, updatable = false)
  private String hostGroupName;

  @Column(name = "blueprint_name", nullable = false, insertable = false, updatable = false)
  private String blueprintName;

  @Column(name = "name", nullable = false, insertable = true, updatable = false)
  private String name;

  @Column(name = "provision_action", nullable = true, insertable = true, updatable = false)
  private String provisionAction;

  @Column(name = "mpack_name", nullable = true, insertable = true, updatable = false)
  private String mpackName;

  @Column(name = "mpack_version", nullable = true, insertable = true, updatable = false)
  private String mpackVersion;

  @Column(name = "service_name", nullable = true, insertable = true, updatable = false)
  private String serviceName;

  @ManyToOne
  @JoinColumns({
      @JoinColumn(name = "hostgroup_name", referencedColumnName = "name", nullable = false),
      @JoinColumn(name = "blueprint_name", referencedColumnName = "blueprint_name", nullable = false)
  })
  private HostGroupEntity hostGroup;

  /**
   * Get the name of the host group component.
   *
   * @return component name
   */
  public String getName() {
    return name;
  }

  /**
   * Set the name of the host group component.
   *
   * @param name component name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Get the host group entity.
   *
   * @return host group entity
   */
  public HostGroupEntity getHostGroupEntity() {
    return hostGroup;
  }

  /**
   * Set the host group entity.
   *
   * @param entity  host group entity
   */
  public void setHostGroupEntity(HostGroupEntity entity) {
    this.hostGroup = entity;
  }

  /**
   * Get the name of the associated host group.
   *
   * @return host group name
   */
  public String getHostGroupName() {
    return hostGroupName;
  }

  /**
   * Set the name of the associated host group.
   *
   * @param hostGroupName host group name
   */
  public void setHostGroupName(String hostGroupName) {
    this.hostGroupName = hostGroupName;
  }

  /**
   * Get the name of the associated blueprint.
   *
   * @return blueprint name
   */
  public String getBlueprintName() {
    return blueprintName;
  }

  /**
   * Set the name of the associated blueprint.
   *
   * @param blueprintName  blueprint name
   */
  public void setBlueprintName(String blueprintName) {
    this.blueprintName = blueprintName;
  }

  /**
   * Get the provision action associated with this
   *   component.
   *
   * @return provision action
   */
  public String getProvisionAction() {
    return provisionAction;
  }

  /**
   * Set the provision action associated with this
   *   component.
   *
   * @param provisionAction action associated with the component (example: INSTALL_ONLY, INSTALL_AND_START)
   */
  public void setProvisionAction(String provisionAction) {
    this.provisionAction = provisionAction;
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
   * @return the name of the mpack defining this component
   *         (only needs to be set if multiple mpack define the same component)
   */
  public String getMpackName() {
    return mpackName;
  }

  /**
   * @param mpackName the name of the mpack defining this component
   *        (only needs to be set if multiple mpack define the same component)
   */
  public void setMpackName(String mpackName) {
    this.mpackName = mpackName;
  }

  /**
   * @return the version of the mpack defining this component
   *         (only needs to be set if multiple mpack define the same component)
   */
  public String getMpackVersion() {
    return mpackVersion;
  }

  /**
   * @param mpackVersion the version of the mpack defining this component
   *        (only needs to be set if multiple mpack define the same component)
   */
  public void setMpackVersion(String mpackVersion) {
    this.mpackVersion = mpackVersion;
  }

  /**
   * @return the name of the service instance defining this component
   *         (only needs to be set if component resolution would be ambigous otherwise)
   */
  public String getServiceName() {
    return serviceName;
  }

  /**
   * @param serviceName the name of the service instance defining this component
   *        (only needs to be set if component resolution would be ambigous otherwise)
   */
  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }
}
