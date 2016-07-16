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

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.state.stack.upgrade.Direction;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;

/**
 * Models the data representation of an upgrade
 */
@Entity
@Table(name = "upgrade")
@TableGenerator(name = "upgrade_id_generator",
    table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value",
    pkColumnValue = "upgrade_id_seq",
    initialValue = 0)
@NamedQueries({
  @NamedQuery(name = "UpgradeEntity.findAll",
      query = "SELECT u FROM UpgradeEntity u"),
  @NamedQuery(name = "UpgradeEntity.findAllForCluster",
      query = "SELECT u FROM UpgradeEntity u WHERE u.clusterId = :clusterId"),
  @NamedQuery(name = "UpgradeEntity.findUpgrade",
      query = "SELECT u FROM UpgradeEntity u WHERE u.upgradeId = :upgradeId"),
  @NamedQuery(name = "UpgradeEntity.findLatestForClusterInDirection",
      query = "SELECT u FROM UpgradeEntity u JOIN RequestEntity r ON u.requestId = r.requestId WHERE u.clusterId = :clusterId AND u.direction = :direction ORDER BY r.startTime DESC"),
  @NamedQuery(name = "UpgradeEntity.findLatestForCluster",
      query = "SELECT u FROM UpgradeEntity u JOIN RequestEntity r ON u.requestId = r.requestId WHERE u.clusterId = :clusterId ORDER BY r.startTime DESC"),
})
public class UpgradeEntity {

  @Id
  @Column(name = "upgrade_id", nullable = false, insertable = true, updatable = false)
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "upgrade_id_generator")
  private Long upgradeId;

  @Column(name = "cluster_id", nullable = false, insertable = true, updatable = false)
  private Long clusterId;

  @Column(name="request_id", nullable = false)
  private Long requestId;

  @Column(name="from_version", nullable = false)
  private String fromVersion = null;

  @Column(name="to_version", nullable = false)
  private String toVersion = null;

  @Column(name="direction", nullable = false)
  @Enumerated(value = EnumType.STRING)
  private Direction direction = Direction.UPGRADE;

  @Column(name="upgrade_package", nullable = false)
  private String upgradePackage;

  @Column(name="upgrade_type", nullable = false)
  @Enumerated(value = EnumType.STRING)
  private UpgradeType upgradeType;

  @Column(name = "skip_failures", nullable = false)
  private Integer skipFailures = 0;

  @Column(name = "skip_sc_failures", nullable = false)
  private Integer skipServiceCheckFailures = 0;

  @Column(name="downgrade_allowed", nullable = false)
  private Short downgrade_allowed = 1;

  /**
   * {@code true} if the upgrade has been marked as suspended.
   */
  @Column(name = "suspended", nullable = false, length = 1)
  private Short suspended = 0;

  @OneToMany(mappedBy = "upgradeEntity", cascade = { CascadeType.ALL })
  private List<UpgradeGroupEntity> upgradeGroupEntities;

  /**
   * @return the id
   */
  public Long getId() {
    return upgradeId;
  }

  /**
   * @param id the id
   */
  public void setId(Long id) {
    upgradeId = id;
  }

  /**
   * @return the cluster id
   */
  public Long getClusterId() {
    return clusterId;
  }

  /**
   * @param id the cluster id
   */
  public void setClusterId(Long id) {
    clusterId = id;
  }

  /**
   * @return the upgrade items
   */
  public List<UpgradeGroupEntity> getUpgradeGroups() {
    return upgradeGroupEntities;
  }

  /**
   * @param items the upgrade items
   */
  public void setUpgradeGroups(List<UpgradeGroupEntity> items) {
    for (UpgradeGroupEntity entity : items) {
      entity.setUpgradeEntity(this);
    }
    upgradeGroupEntities = items;
  }

  /**
   * @return the request id
   */
  public Long getRequestId() {
    return requestId;
  }

  /**
   * @param id the request id
   */
  public void setRequestId(Long id) {
    requestId = id;
  }

  /**
   * @return the "from" version
   */
  public String getFromVersion() {
    return fromVersion;
  }

  /**
   * @param version the "from" version
   */
  public void setFromVersion(String version) {
    fromVersion = version;
  }

  /**
   * @return the "to" version
   */
  public String getToVersion() {
    return toVersion;
  }

  /**
   * @param version the "to" version
   */
  public void setToVersion(String version) {
    toVersion = version;
  }

  /**
   * @return the direction of the upgrade
   */
  public Direction getDirection() {
    return direction;
  }

  /**
   * @param direction the direction of the upgrade
   */
  public void setDirection(Direction direction) {
    this.direction = direction;
  }

  /**
   * @return the upgrade type, such as rolling oNr non_rolling
   */
  public UpgradeType getUpgradeType() {
    return upgradeType;
  }

  /**
   * @return possibility to process downgrade
   */
  public Boolean isDowngradeAllowed() {
    return downgrade_allowed != null ? (downgrade_allowed != 0) : null;
  }

  /**
   * @param canDowngrade {@code true} to allow downgrade, {@code false} to disallow downgrade
   */
  public void setDowngradeAllowed(boolean canDowngrade) {
    downgrade_allowed = (!canDowngrade ? (short)0 : (short)1);
  }

  /**
   * @param upgradeType the upgrade type to set
   */
  public void setUpgradeType(UpgradeType upgradeType) {
    this.upgradeType = upgradeType;
  }

  /**
   * @return the upgrade package name, without the extension.
   */
  public String getUpgradePackage() {
    return upgradePackage;
  }

  /**
   * @param upgradePackage the upgrade pack to set
   */
  public void setUpgradePackage(String upgradePackage) {
    this.upgradePackage = upgradePackage;
  }

  /**
   * Gets whether skippable components that failed are automatically skipped.
   * They will be placed into the {@link HostRoleStatus#SKIPPED_FAILED} state.
   *
   * @return {@code true} if skippable failed components are automatically
   *         skipped when they fail.
   */
  public boolean isComponentFailureAutoSkipped() {
    return skipFailures != 0;
  }

  /**
   * Sets whether skippable components that failed are automatically skipped.
   *
   * @param autoSkipComponentFailures
   *          {@code true} to automatically skip component failures which are
   *          marked as skippable.
   */
  public void setAutoSkipComponentFailures(boolean autoSkipComponentFailures) {
    skipFailures = autoSkipComponentFailures ? 1 : 0;
  }

  /**
   * Gets whether skippable service checks that failed are automatically
   * skipped. They will be placed into the {@link HostRoleStatus#SKIPPED_FAILED}
   * state.
   *
   * @return {@code true} if service checks are automatically skipped when they
   *         fail.
   */
  public boolean isServiceCheckFailureAutoSkipped() {
    return skipServiceCheckFailures != 0;
  }

  /**
   * Sets whether skippable service checks that failed are automatically
   * skipped.
   *
   * @param autoSkipServiceCheckFailures
   *          {@code true} to automatically skip service check failures which
   *          are marked as being skippable.
   */
  public void setAutoSkipServiceCheckFailures(boolean autoSkipServiceCheckFailures) {
    skipServiceCheckFailures = autoSkipServiceCheckFailures ? 1 : 0;
  }

  /**
   * Gets whether the upgrade is suspended. A suspended upgrade will appear to
   * have its request aborted, but the intent is to resume it at a later point.
   *
   * @return {@code true} if the upgrade is suspended.
   */
  public boolean isSuspended() {
    return suspended != 0;
  }

  /**
   * Sets whether the upgrade is suspended.
   *
   * @param suspended
   *          {@code true} to mark the upgrade as suspended.
   */
  public void setSuspended(boolean suspended) {
    this.suspended = suspended ? (short) 1 : (short) 0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    UpgradeEntity that = (UpgradeEntity) o;

    if (upgradeId != null ? !upgradeId.equals(that.upgradeId) : that.upgradeId != null) {
      return false;
    }
    if (clusterId != null ? !clusterId.equals(that.clusterId) : that.clusterId != null) {
      return false;
    }
    if (requestId != null ? !requestId.equals(that.requestId) : that.requestId != null) {
      return false;
    }
    if (fromVersion != null ? !fromVersion.equals(that.fromVersion) : that.fromVersion != null) {
      return false;
    }
    if (toVersion != null ? !toVersion.equals(that.toVersion) : that.toVersion != null) {
      return false;
    }
    if (direction != null ? !direction.equals(that.direction) : that.direction != null) {
      return false;
    }
    if (suspended != null ? !suspended.equals(that.suspended) : that.suspended != null) {
      return false;
    }
    if (upgradeType != null ? !upgradeType.equals(that.upgradeType) : that.upgradeType != null) {
      return false;
    }
    if (upgradePackage != null ? !upgradePackage.equals(that.upgradePackage) : that.upgradePackage != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = upgradeId != null ? upgradeId.hashCode() : 0;
    result = 31 * result + (clusterId != null ? clusterId.hashCode() : 0);
    result = 31 * result + (requestId != null ? requestId.hashCode() : 0);
    result = 31 * result + (fromVersion != null ? fromVersion.hashCode() : 0);
    result = 31 * result + (toVersion != null ? toVersion.hashCode() : 0);
    result = 31 * result + (direction != null ? direction.hashCode() : 0);
    result = 31 * result + (suspended != null ? suspended.hashCode() : 0);
    result = 31 * result + (upgradeType != null ? upgradeType.hashCode() : 0);
    result = 31 * result + (upgradePackage != null ? upgradePackage.hashCode() : 0);
    return result;
  }

}
