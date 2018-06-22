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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.QueryHint;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.state.stack.upgrade.Direction;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
import org.apache.commons.lang.builder.EqualsBuilder;

import com.google.common.base.Objects;

/**
 * Models the data representation of an upgrade
 */
@Entity
@Table(name = "upgrade")
@TableGenerator(
    name = "upgrade_id_generator",
    table = "ambari_sequences",
    pkColumnName = "sequence_name",
    valueColumnName = "sequence_value",
    pkColumnValue = "upgrade_id_seq",
    initialValue = 0)
@NamedQueries({
    @NamedQuery(name = "UpgradeEntity.findAll", query = "SELECT u FROM UpgradeEntity u"),
    @NamedQuery(
        name = "UpgradeEntity.findAllForCluster",
        query = "SELECT u FROM UpgradeEntity u WHERE u.clusterId = :clusterId"),
    @NamedQuery(
        name = "UpgradeEntity.findUpgrade",
        query = "SELECT u FROM UpgradeEntity u WHERE u.upgradeId = :upgradeId"),
    @NamedQuery(
        name = "UpgradeEntity.findUpgradeByRequestId",
        query = "SELECT u FROM UpgradeEntity u WHERE u.requestId = :requestId"),
    @NamedQuery(
        name = "UpgradeEntity.findLatestForClusterInDirection",
        query = "SELECT u FROM UpgradeEntity u JOIN RequestEntity r ON u.requestId = r.requestId WHERE u.clusterId = :clusterId AND u.direction = :direction ORDER BY r.startTime DESC, u.upgradeId DESC"),
    @NamedQuery(
        name = "UpgradeEntity.findLatestForCluster",
        query = "SELECT u FROM UpgradeEntity u JOIN RequestEntity r ON u.requestId = r.requestId WHERE u.clusterId = :clusterId ORDER BY r.startTime DESC"),
    @NamedQuery(
        name = "UpgradeEntity.findAllRequestIds",
        query = "SELECT upgrade.requestId FROM UpgradeEntity upgrade"),
    @NamedQuery(
        name = "UpgradeEntity.findRevertable",
        query = "SELECT upgrade FROM UpgradeEntity upgrade WHERE upgrade.revertAllowed = 1 AND upgrade.clusterId = :clusterId ORDER BY upgrade.upgradeId DESC",
        hints = {
            @QueryHint(name = "eclipselink.query-results-cache", value = "true"),
            @QueryHint(name = "eclipselink.query-results-cache.ignore-null", value = "false"),
            @QueryHint(name = "eclipselink.query-results-cache.size", value = "1")
          })
        })
public class UpgradeEntity {

  @Id
  @Column(name = "upgrade_id", nullable = false, insertable = true, updatable = false)
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "upgrade_id_generator")
  private Long upgradeId;

  @Column(name = "cluster_id", nullable = false, insertable = true, updatable = false)
  private Long clusterId;

  @Column(name = "request_id", nullable = false, insertable = false, updatable = false)
  private Long requestId;

  /**
   * The request entity associated with this upgrade. This relationship allows
   * JPA to correctly order non-flushed commits during the transaction which
   * creates the upgrade. Without it, JPA would not know the correct order and
   * may try to create the upgrade before the request.
   */
  @OneToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "request_id", nullable = false, insertable = true, updatable = false)
  private RequestEntity requestEntity = null;

  @Column(name="direction", nullable = false)
  @Enumerated(value = EnumType.STRING)
  private Direction direction = Direction.UPGRADE;

  @Column(name="upgrade_type", nullable = false)
  @Enumerated(value = EnumType.STRING)
  private UpgradeType upgradeType;

  @Column(name = "skip_failures", nullable = false)
  private Integer skipFailures = 0;

  @Column(name = "skip_sc_failures", nullable = false)
  private Integer skipServiceCheckFailures = 0;

  @Column(name="downgrade_allowed", nullable = false)
  private Short downgradeAllowed = 1;

  /**
   * {@code true} if this upgrade is a revert, otherwise {@code false}
   */
  @Column(name = "is_revert", nullable = false)
  private Short isRevert = 0;

  /**
   * Whether this upgrade is a candidate to be reverted. The current restriction
   * on this behavior is that only the most recent
   * {@link RepositoryType#PATCH}/{@link RepositoryType#MAINT} for a given
   * cluster can be reverted at a time.
   * <p/>
   * All upgrades are created with this value defaulted to {@code false}. Upon
   * successful finalization of the upgrade, if the upgrade was the correct type
   * and direction, then it becomes a candidate for reversion and this value is
   * set to {@code true}. If an upgrade is reverted after being finalized, then
   * this value to should set to {@code false} explicitely.
   * <p/>
   * There can exist <i>n</i> number of upgrades with this value set to
   * {@code true}. The idea is that only the most recent upgrade with this value
   * set to {@code true} will be able to be reverted.
   */
  @Column(name = "revert_allowed", nullable = false)
  private Short revertAllowed = 0;

  /**
   * {@code true} if the upgrade has been marked as suspended.
   */
  @Column(name = "suspended", nullable = false, length = 1)
  private Short suspended = 0;

  @OneToMany(mappedBy = "upgradeEntity", cascade = { CascadeType.ALL })
  private List<UpgradeGroupEntity> upgradeGroupEntities;

  /**
   * Uni-directional relationship between an upgrade an all of the components in
   * that upgrade.
   */
  @OneToMany(orphanRemoval=true, cascade = { CascadeType.ALL })
  @JoinColumn(name = "upgrade_id")
  private List<UpgradeHistoryEntity> upgradeHistory;

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

  public void setRequestEntity(RequestEntity requestEntity) {
    this.requestEntity = requestEntity;
    requestId = requestEntity.getRequestId();
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
    return downgradeAllowed != null ? (downgradeAllowed != 0) : null;
  }

  /**
   * @param canDowngrade {@code true} to allow downgrade, {@code false} to disallow downgrade
   */
  public void setDowngradeAllowed(boolean canDowngrade) {
    downgradeAllowed = (!canDowngrade ? (short) 0 : (short) 1);
  }

  /**
   * Gets whether this upgrade is a revert.
   *
   * @return {@code true} if this is a revert, {@code false} otherwise.
   */
  public Boolean isRevert() {
    return isRevert != null ? (isRevert != 0) : null;
  }

  /**
   * Sets whether this upgrade is a revert.
   *
   * @param isRevert
   *          {@code true} if this is a revert, {@code false} otherwise.
   */
  public void setIsRevert(boolean isRevert) {
    this.isRevert = (!isRevert ? (short) 0 : (short) 1);
  }

  /**
   * Gets whether this upgrade supports being reverted. Upgrades can be reverted
   * (downgraded after finalization) if they are either
   * {@link RepositoryType#MAINT} or {@link RepositoryType#PATCH} and have never
   * been previously downgraded.
   *
   * @return {@code true} if this upgrade can potentially be revereted.
   */
  public Boolean isRevertAllowed() {
    return revertAllowed != null ? (revertAllowed != 0) : null;
  }

  /**
   * Sets whether this upgrade supports being reverted. This should only ever be
   * called from the finalization of an upgrade. {@link RepositoryType#MAINT} or
   * {@link RepositoryType#PATCH} upgrades can be revereted only if they have
   * not previously been downgraded.
   *
   * @param revertable
   *          {@code true} to mark this as being revertable, {@code false}
   *          otherwise.
   */
  public void setRevertAllowed(boolean revertable) {
    revertAllowed = (!revertable ? (short) 0 : (short) 1);
  }

  /**
   * @param upgradeType
   *          the upgrade type to set
   */
  public void setUpgradeType(UpgradeType upgradeType) {
    this.upgradeType = upgradeType;
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

  /**
   * Adds a historical entry for a service component in this upgrade.
   *
   * @param historicalEntry
   *          the entry to add.
   */
  public void addHistory(UpgradeHistoryEntity historicalEntry) {
    if (null == upgradeHistory) {
      upgradeHistory = new ArrayList<>();
    }

    upgradeHistory.add(historicalEntry);
  }

  /**
   * Gets the history of this component's upgrades and downgrades.
   *
   * @return the component history, or {@code null} if none.
   */
  public List<UpgradeHistoryEntity> getHistory() {
    return upgradeHistory;
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

    UpgradeEntity that = (UpgradeEntity) o;
    return new EqualsBuilder()
        .append(upgradeId, that.upgradeId)
        .append(clusterId, that.clusterId)
        .append(requestId, that.requestId)
        .append(direction, that.direction)
        .append(suspended, that.suspended)
        .append(upgradeType, that.upgradeType)
        .isEquals();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Objects.hashCode(upgradeId, clusterId, requestId, direction, suspended, upgradeType);
  }

  /**
   * Gets the mpacks which are participating in the upgrade.
   *
   * @return the set of mpacks in this upgrade.
   */
  public Set<MpackEntity> getSourceMpacks() {
    List<UpgradeHistoryEntity> historyEntities = getHistory();

    LinkedHashSet<MpackEntity> mpacks = historyEntities.stream().map(
        history -> history.getSourceMpackEntity()).collect(
            Collectors.toCollection(LinkedHashSet::new));

    return mpacks;
  }

  /**
   * Gets the mpack stack IDs which are participating in the upgrade.
   *
   * @return the set of mpacks in this upgrade.
   */
  public Set<String> getSourceMpackStacks() {
    List<UpgradeHistoryEntity> historyEntities = getHistory();

    LinkedHashSet<String> mpacks = historyEntities.stream().map(
        history -> history.getSourceMpackEntity().getStackId().toString()).collect(
            Collectors.toCollection(LinkedHashSet::new));

    return mpacks;
  }

  /**
   * Gets the mpacks which are participating in the upgrade.
   *
   * @return the target set of mpacks in this upgrade.
   */
  public Set<MpackEntity> getTargetMpack() {
    List<UpgradeHistoryEntity> historyEntities = getHistory();

    LinkedHashSet<MpackEntity> mpacks = historyEntities.stream().map(
        history -> history.getTargetMpackEntity()).collect(
            Collectors.toCollection(LinkedHashSet::new));

    return mpacks;
  }

  /**
   * Gets the mpack names which are participating in the upgrade.
   *
   * @return the target set of mpacks in this upgrade.
   */
  public Set<String> getTargetMpackStacks() {
    List<UpgradeHistoryEntity> historyEntities = getHistory();

    LinkedHashSet<String> mpacks = historyEntities.stream().map(
        history -> history.getTargetMpackEntity().getStackId().toString()).collect(
            Collectors.toCollection(LinkedHashSet::new));

    return mpacks;
  }

}
