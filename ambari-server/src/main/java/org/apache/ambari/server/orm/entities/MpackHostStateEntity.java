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

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.UniqueConstraint;

import org.apache.ambari.server.state.MpackInstallState;

/**
 * The {@link MpackHostStateEntity} is used to track the installation state of a
 * management pack for a given host. This allows Ambari to determine if a
 * management pack needs re-installation on a host or if a host has the correct
 * bits installed already.
 */
@Entity
@Table(
    name = "mpack_host_state",
    uniqueConstraints = @UniqueConstraint(
        name = "UQ_mpack_host_state",
        columnNames = { "host_id", "mpack_id" }))
@TableGenerator(
    name = "mpack_host_state_id_generator",
    table = "ambari_sequences",
    pkColumnName = "sequence_name",
    valueColumnName = "sequence_value",
    pkColumnValue = "mpack_host_state_id_seq",
    initialValue = 0)
@NamedQueries({
    @NamedQuery(
        name = "findInstallStateByHost",
        query = "SELECT mpackHostState FROM MpackHostStateEntity mpackHostState WHERE mpackHostState.hostEntity.hostName=:hostName"),
    @NamedQuery(
        name = "findInstallStateByMpack",
        query = "SELECT mpackHostState FROM MpackHostStateEntity mpackHostState WHERE mpackHostState.mpackId = :mpackId"),
    @NamedQuery(
        name = "findInstallStateByMpackAndHost",
        query = "SELECT mpackHostState FROM MpackHostStateEntity mpackHostState WHERE mpackHostState.mpackId = :mpackId AND mpackHostState.hostEntity.hostName=:hostName"),
    @NamedQuery(
        name = "findInstallStateByStateAndHost",
        query = "SELECT mpackHostState FROM MpackHostStateEntity mpackHostState WHERE mpackHostState.hostEntity.hostName=:hostName AND mpackHostState.state = :mpackInstallState") })

public class MpackHostStateEntity {

  /**
   * The primary key ID of this entity.
   */
  @Id
  @Column(name = "id", nullable = false, insertable = true, updatable = false)
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "mpack_host_state_id_generator")
  private Long id;

  /**
   * The ID of the host associated with an mpack for a given installation state.
   */
  @Column(name = "host_id", nullable = false, insertable = false, updatable = false)
  private Long hostId;

  /**
   * The host associated with an mpack for a given installation state.
   */
  @ManyToOne
  @JoinColumn(name = "host_id", referencedColumnName = "host_id", nullable = false)
  private HostEntity hostEntity;

  /**
   * The ID of the host associated with an mpack for a given installation state.
   */
  @Column(name = "mpack_id", nullable = false, insertable = false, updatable = false)
  private Long mpackId;

  /**
   * The host associated with an mpack for a given installation state.
   */
  @ManyToOne
  @JoinColumn(name = "mpack_id", referencedColumnName = "id", nullable = false)
  private MpackEntity mpackEntity;

  /**
   * The installation state of a management pack on a given host.
   */
  @Column(name = "state", nullable = false, insertable = true, updatable = true)
  @Enumerated(value = EnumType.STRING)
  private MpackInstallState state;

  /**
   * Constructor.
   */
  public MpackHostStateEntity() {
  }

  /**
   * Constructor.
   *
   * @param mpackEntity
   * @param hostEntity
   * @param state
   */
  public MpackHostStateEntity(MpackEntity mpackEntity, HostEntity hostEntity,
      MpackInstallState state) {
    this.mpackEntity = mpackEntity;
    this.hostEntity = hostEntity;
    this.state = state;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getHostId() {
    return hostId;
  }

  public void setHostId(Long hostId) {
    this.hostId = hostId;
  }

  public Long getMpackId() {
    return mpackId;
  }

  public void setMpackId(Long mpackId) {
    this.mpackId = mpackId;
  }

  public String getHostName() {
    return hostEntity != null ? hostEntity.getHostName() : null;
  }

  public HostEntity getHostEntity() {
    return hostEntity;
  }

  public void setHostEntity(HostEntity hostEntity) {
    this.hostEntity = hostEntity;
  }

  public MpackInstallState getState() {
    return state;
  }

  public void setState(MpackInstallState state) {
    this.state = state;
  }

  public MpackEntity getMpack() {
    return mpackEntity;
  }

  public void setMpack(MpackEntity mpackEntity) {
    this.mpackEntity = mpackEntity;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Objects.hash(mpackId, hostId, state);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }

    MpackHostStateEntity other = (MpackHostStateEntity) obj;
    return Objects.equals(mpackId, other.mpackId)
        && Objects.equals(hostId, other.hostId) && Objects.equals(state, other.state);
  }
}
