/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.orm.entities;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import org.apache.ambari.server.registry.RegistryType;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RegistryEntity} class represents the registry object.
 */

@Table(name = "registries")
@Entity
@TableGenerator(name = "registry_id_generator", table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value", pkColumnValue = "registry_id_seq", initialValue = 1)
@NamedQueries({
  @NamedQuery(name = "RegistryEntity.findById", query = "SELECT registry FROM RegistryEntity registry where registry.registryId = :registryId"),
  @NamedQuery(name = "RegistryEntity.findAll", query = "SELECT registry FROM RegistryEntity registry"),
  @NamedQuery(name = "RegistryEntity.findByName", query = "SELECT registry FROM RegistryEntity registry where registry.registryName = :registryName") })

public class RegistryEntity {
  protected final static Logger LOG = LoggerFactory.getLogger(RegistryEntity.class);
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "registry_id_generator")
  @Column(name = "id", nullable = false, updatable = false)
  private Long registryId;

  @Column(name = "registry_name", nullable = false, updatable = true)
  private String registryName;

  @Enumerated(value = EnumType.STRING)
  @Column(name = "registry_type", nullable = false, updatable = true)
  private RegistryType registryType;

  @Column(name = "registry_uri", nullable = false, updatable = true)
  private String registryUri;

  public Long getRegistryId() {
    return registryId;
  }

  public void setRegistryId(Long registryId) {
    this.registryId = registryId;
  }

  public String getRegistryName() {
    return registryName;
  }

  public void setRegistryName(String registryName) {
    this.registryName = registryName;
  }

  public RegistryType getRegistryType() {
    return registryType;
  }

  public void setRegistryType(RegistryType registryType) {
    this.registryType = registryType;
  }

  public String getRegistryUri() {
    return registryUri;
  }

  public void setRegistryUri(String registryUri) {
    this.registryUri = registryUri;
  }

  public RegistryEntity() {

  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }

    if (object == null || getClass() != object.getClass()) {
      return false;
    }

    RegistryEntity that = (RegistryEntity) object;
    EqualsBuilder equalsBuilder = new EqualsBuilder();

    equalsBuilder.append(registryId, that.registryId);
    equalsBuilder.append(registryName, that.registryName);
    equalsBuilder.append(registryType, that.registryType);
    equalsBuilder.append(registryUri, that.registryUri);
    return equalsBuilder.isEquals();
  }

  /**
   * Generates a hash for the software registry
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Objects.hash(registryId, registryName, registryType, registryUri);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder("RegistryEntity{");
    buffer.append("registryId=").append(registryId);
    buffer.append(", registryName=").append(registryName);
    buffer.append(", registryType=").append(registryType);
    buffer.append(", registryUri=").append(registryUri);
    buffer.append("}");
    return buffer.toString();
  }
}
