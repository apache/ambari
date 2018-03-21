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

/**
 * Entity representing a service instance in multi-service blueprints/cluster templates
 */
@Entity
@Table(name = "mpack_instance_service")
@TableGenerator(name = "mpack_inst_svc_id_generator", table = "ambari_sequences", pkColumnName = "sequence_name",
  valueColumnName = "sequence_value", pkColumnValue = "mpack_inst_svc_id_seq", initialValue = 1)
public class MpackInstanceServiceEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "mpack_inst_svc_id_generator")
  @Column(name = "id", nullable = false, updatable = false)
  private Long id;

  @ManyToOne()
  @JoinColumn(name = "mpack_instance_id", referencedColumnName = "id", nullable = false)
  private MpackInstanceEntity mpackInstance;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "service", orphanRemoval = true)
  private Collection<MpackServiceConfigEntity> configurations = new ArrayList<>();

  private String name;

  private String type;

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
   * @return the mpack instance to the mpack associated with this service
   */
  public MpackInstanceEntity getMpackInstance() {
    return mpackInstance;
  }

  /**
   * @param mpackInstance the mpack instance to the mpack associated with this service
   */
  public void setMpackInstance(MpackInstanceEntity mpackInstance) {
    this.mpackInstance = mpackInstance;
  }

  /**
   * @return the service instance level configuration entities
   */
  public Collection<MpackServiceConfigEntity> getConfigurations() {
    return configurations;
  }

  /**
   * @param configurations the service instance level configuration entities
   */
  public void setConfigurations(Collection<MpackServiceConfigEntity> configurations) {
    this.configurations = configurations;
  }

  /**
   * @return the name of this service instance
   */
  public String getName() {
    return name;
  }

  /**
   * @param name the name of this service instance
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * @return the configuration type
   */
  public String getType() {
    return type;
  }

  /**
   * @param type the configuration type
   */
  public void setType(String type) {
    this.type = type;
  }
}
