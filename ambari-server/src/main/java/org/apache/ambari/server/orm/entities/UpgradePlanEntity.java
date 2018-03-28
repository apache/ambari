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

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import org.apache.ambari.server.state.stack.upgrade.Direction;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
import org.apache.commons.lang.ObjectUtils;

import com.google.common.base.Objects;

/**
 * Models a single upgrade plan that can be used to invoke an Upgrade.
 */
@Table(name = "upgrade_plan")
@Entity
@TableGenerator(name = "upgrade_plan_id_generator",
    table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value",
    pkColumnValue = "upgrade_plan_id_seq",
    initialValue = 0)
public class UpgradePlanEntity {

  @Id
  @Column(name = "id", nullable = false, insertable = true, updatable = false)
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "upgrade_plan_id_generator")
  private Long id;

  @Column(name = "cluster_id", nullable = false, insertable = true, updatable = false)
  private Long clusterId;

  @Column(name = "upgrade_type", nullable = false)
  @Enumerated(value = EnumType.STRING)
  private UpgradeType upgradeType = UpgradeType.ROLLING;

  @Column(name = "direction", nullable=false)
  @Enumerated(value = EnumType.STRING)
  private Direction direction = Direction.UPGRADE;

  @Column(name = "skip_failures", nullable = false)
  private short skipFailures = (short) 0;

  @Column(name = "skip_sc_failures", nullable = false)
  private short skipServiceCheckFailures = (short) 0;

  @Column(name = "skip_prechecks", nullable = false)
  private short skipPrerequisiteChecks = (short) 0;

  @Column(name = "fail_on_precheck_warnings", nullable = false)
  private short failOnPrerequisiteWarnings = (short) 0;

  @Column(name = "skip_service_checks", nullable = false)
  private short skipServiceChecks = (short) 0;

  @OneToMany(mappedBy = "upgradePlanEntity", cascade = { CascadeType.ALL })
  private List<UpgradePlanDetailEntity> upgradePlanDetails;

  /**
   * @return the id
   */
  public Long getId() {
    return id;
  }

  /**
   * @param planId the id
   */
  public void setId(Long planId) {
    id = planId;
  }

  /**
   * @return the cluster id
   */
  public long getClusterId() {
    return clusterId;
  }

  /**
   * @param id the cluster id
   */
  public void setClusterId(long id) {
    clusterId = id;
  }

  /**
   * @return the direction
   */
  public Direction getDirection() {
    return direction;
  }

  /**
   * @param direction the direction
   */
  public void setDirection(Direction direction) {
    this.direction = direction;
  }

  /**
   * @return the upgrade type
   */
  public UpgradeType getUpgradeType() {
    return upgradeType;
  }

  /**
   * @param type the upgrade type
   */
  public void setUpgradeType(UpgradeType type) {
    upgradeType = type;
  }

  /**
   * @return if failures should be skipped
   */
  public boolean isSkipFailures() {
    return skipFailures != 0;
  }

  /**
   * @param skip {@code true} to skip failures that are not service check based
   */
  public void setSkipFailures(boolean skip) {
    skipFailures = skip ? (short) 1 : (short) 0;
  }

  /**
   * @return if service check failures can be auto-skipped
   */
  public boolean isSkipServiceCheckFailures() {
    return skipServiceCheckFailures != 0;
  }

  /**
   * @param skip {@code true} to skip service check failures
   */
  public void setSkipServiceCheckFailures(boolean skip) {
    skipServiceCheckFailures = skip ? (short) 1 : (short) 0;
  }

  /**
   * @return if prerequiste checks are skipped
   */
  public boolean isSkipPrerequisiteChecks() {
    return skipPrerequisiteChecks != 0;
  }

  /**
   * @param skip {@code true} to skip prerequiste checks
   */
  public void setSkipPrerequisiteChecks(boolean skip) {
    skipPrerequisiteChecks = skip ? (short) 1 : (short) 0;
  }

  /**
   * @return if precheck warnings should result in a failure
   */
  public boolean isFailOnPrerequisiteWarnings() {
    return failOnPrerequisiteWarnings != 0;
  }

  /**
   * @param fail {@code true} to treat prerequisite warnings as failures
   */
  public void setFailOnPrerequisiteWarnings(boolean fail) {
    failOnPrerequisiteWarnings = fail ? (short) 1 : (short) 0;
  }

  /**
   * @return if service checks should be skipped
   */
  public boolean isSkipServiceChecks() {
    return skipServiceChecks != 0;
  }

  /**
   * @param skip {@code true} to skip service checks
   */
  public void setSkipServiceChecks(boolean skip) {
    skipServiceChecks = skip ? (short) 1 : (short) 0;
  }

  /**
   * @return the plan details
   */
  public List<UpgradePlanDetailEntity> getDetails() {
    return upgradePlanDetails;
  }

  /**
   * @param details the plan details
   */
  public void setDetails(List<UpgradePlanDetailEntity> details) {
    for (UpgradePlanDetailEntity entity : details) {
      entity.setUpgradePlanEntity(this);
    }

    upgradePlanDetails = details;
  }


  @Override
  public int hashCode() {
    return ObjectUtils.hashCode(id);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this).omitNullValues()
        .add("id", id)
        .add("upgrade_type", upgradeType)
        .add("direction", direction)
        .toString();
  }

}
