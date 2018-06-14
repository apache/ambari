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
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.UniqueConstraint;

import org.apache.commons.lang.builder.EqualsBuilder;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * The {@link UpgradeHistoryEntity} represents the version history of components
 * participating in an upgrade or a downgrade.
 */
@Entity
@Table(
    name = "upgrade_history",
    uniqueConstraints = @UniqueConstraint(
        columnNames = { "upgrade_id", "service_group_id" }))
@TableGenerator(
    name = "upgrade_history_id_generator",
    table = "ambari_sequences",
    pkColumnName = "sequence_name",
    valueColumnName = "sequence_value",
    pkColumnValue = "upgrade_history_id_seq",
    initialValue = 0)
@NamedQueries({
    @NamedQuery(
        name = "UpgradeHistoryEntity.findAll",
        query = "SELECT upgradeHistory FROM UpgradeHistoryEntity upgradeHistory"),
    @NamedQuery(
        name = "UpgradeHistoryEntity.findByUpgradeId",
        query = "SELECT upgradeHistory FROM UpgradeHistoryEntity upgradeHistory WHERE upgradeHistory.upgradeId = :upgradeId")
})
public class UpgradeHistoryEntity {

  @Id
  @Column(name = "id", nullable = false, insertable = true, updatable = false)
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "upgrade_history_id_generator")
  private Long id;

  @Column(name = "upgrade_id", nullable = false, insertable = false, updatable = false)
  private Long upgradeId;

  @JoinColumn(name = "upgrade_id", nullable = false)
  private UpgradeEntity upgrade;

  @Column(name = "source_mpack_id", nullable = false, insertable = false, updatable = false)
  private Long sourceMpackId;

  @ManyToOne()
  @JoinColumn(name = "source_mpack_id", referencedColumnName = "id")
  private MpackEntity sourceMpackEntity;

  @Column(name = "target_mpack_id", nullable = false, insertable = false, updatable = false)
  private Long targetMpackId;

  @ManyToOne()
  @JoinColumn(name = "target_mpack_id", referencedColumnName = "id")
  private MpackEntity targetMpackEntity;

  @Column(name = "service_group_id", nullable = false, insertable = false, updatable = false)
  private Long serviceGroupId;

  @ManyToOne()
  @JoinColumn(name = "service_group_id", referencedColumnName = "id")
  private ServiceGroupEntity serviceGroupEntity;

  /**
   * Constructor.
   *
   */
  UpgradeHistoryEntity() {
  }

  public UpgradeHistoryEntity(UpgradeEntity upgrade, ServiceGroupEntity serviceGroup,
      MpackEntity sourceMpack, MpackEntity targetMpack) {
    this.upgrade = upgrade;
    serviceGroupEntity = serviceGroup;
    sourceMpackEntity = sourceMpack;
    targetMpackEntity = targetMpack;
  }

  /**
   * @return the id
   */
  public Long getId() {
    return id;
  }

  /**
   * Gets the upgrade that the history entry is for.
   *
   * @return the upgrade
   */
  public UpgradeEntity getUpgrade() {
    return upgrade;
  }

  /**
   * Gets the source mpack that the upgrade is coming from.
   *
   * @return the sourceMpackEntity
   */
  public MpackEntity getSourceMpackEntity() {
    return sourceMpackEntity;
  }

  /**
   * Gets the target mpack that the upgrade is moving to.
   *
   * @return the targetMpackEntity
   */
  public MpackEntity getTargetMpackEntity() {
    return targetMpackEntity;
  }

  /**
   * The service group which this upgrade history entry is for.
   *
   * @return the service group.
   */
  public ServiceGroupEntity getServiceGroupEntity() {
    return serviceGroupEntity;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    UpgradeHistoryEntity that = (UpgradeHistoryEntity) o;
    return new EqualsBuilder()
        .append(upgradeId, that.upgradeId)
        .append(serviceGroupEntity, that.serviceGroupEntity)
        .append(sourceMpackEntity, that.sourceMpackEntity)
        .append(targetMpackEntity, that.targetMpackEntity)
        .isEquals();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Objects.hashCode(upgradeId, serviceGroupEntity, sourceMpackEntity, targetMpackEntity);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("id", id)
        .add("upgradeId", upgradeId)
        .add("serviceGroupId", serviceGroupId)
        .add("sourceMpack", sourceMpackEntity)
        .add("targetMpack", targetMpackEntity).toString();
  }
}
