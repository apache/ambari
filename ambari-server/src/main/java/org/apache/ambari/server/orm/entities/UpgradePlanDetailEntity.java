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
import java.util.List;

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

import com.google.common.base.MoreObjects;

/**
 * Models a single upgrade plan that can be used to invoke an Upgrade.
 */
@Table(name = "upgrade_plan_detail")
@Entity
@TableGenerator(name = "upgrade_plan_detail_id_generator",
    table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value",
    pkColumnValue = "upgrade_plan_detail_id_seq",
    initialValue = 0)
public class UpgradePlanDetailEntity {

  @Id
  @Column(name = "id", nullable = false, insertable = true, updatable = false)
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "upgrade_plan_detail_id_generator")
  private Long id;

  @Column(name = "upgrade_plan_id", nullable = false, insertable = false, updatable = false)
  private Long upgradePlanId;

  @Column(name = "service_group_id", nullable = false, insertable = true, updatable = true)
  private Long serviceGroupId;

  @Column(name = "mpack_target_id", nullable = false, insertable = true, updatable = true)
  private Long mpackTargetId;

  @Column(name = "upgrade_pack", nullable = false, insertable = true, updatable = true)
  private String upgradePack;

  @ManyToOne
  @JoinColumn(name = "upgrade_plan_id", referencedColumnName = "id", nullable = false)
  private UpgradePlanEntity upgradePlanEntity;

  @OneToMany(mappedBy = "upgradePlanDetailEntity", cascade = { CascadeType.ALL })
  private List<UpgradePlanConfigEntity> upgradePlanConfigs = new ArrayList<>();

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
   * @return the id of the service group to be upgraded
   */
  public long getServiceGroupId() {
    return serviceGroupId;
  }

  /**
   * @param id the service group id to upgrade
   */
  public void setServiceGroupId(long id) {
    serviceGroupId = id;
  }

  /**
   * @return the target mpack id
   */
  public long getMpackTargetId() {
    return mpackTargetId;
  }

  /**
   * @param id the target mpack id
   */
  public void setMpackTargetId(long id) {
    mpackTargetId = id;
  }

  /**
   * @param plan  the owning upgrade plan
   */
  void setUpgradePlanEntity(UpgradePlanEntity plan) {
    upgradePlanEntity = plan;
  }

  /**
   * @return the resolved upgrade pack name from the target mpack
   */
  public String getUpgradePack() {
    return upgradePack;
  }

  /**
   * @param pack the resolved upgrade pack from the mpack.
   */
  public void setUpgradePack(String pack) {
    upgradePack = pack;
  }

  /**
   * @param changes
   *          the changes for this detail
   */
  public void setConfigChanges(List<UpgradePlanConfigEntity> changes) {
    changes.forEach(change -> change.setPlanDetail(this));

    upgradePlanConfigs = changes;
  }

  /**
   * @return the config changes
   */
  public List<UpgradePlanConfigEntity> getConfigChanges() {
    return upgradePlanConfigs;
  }


  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).omitNullValues()
        .add("id", id)
        .toString();
  }

}
