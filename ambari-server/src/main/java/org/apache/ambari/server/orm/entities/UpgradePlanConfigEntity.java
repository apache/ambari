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
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

/**
 * Holds config changes for an entity.
 */
@Table(name = "upgrade_plan_config")
@Entity
@TableGenerator(name = "upgrade_plan_config_id_generator",
    table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value",
    pkColumnValue = "upgrade_plan_config_id_seq",
    initialValue = 0)
public class UpgradePlanConfigEntity {

  @Id
  @Column(name = "id", nullable = false, insertable = true, updatable = false)
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "upgrade_plan_config_id_generator")
  private Long id;

  @Column(name = "config_type", nullable = false, insertable = true, updatable = false)
  private String type;

  @Column(name = "config_key", nullable = false, insertable = true, updatable = false)
  private String key;

  @Column(name="new_value", insertable = true, updatable = false)
  private String newValue;

  @Column(name="remove")
  private short remove = (short) 0;

  @ManyToOne
  @JoinColumn(name = "upgrade_plan_detail_id", referencedColumnName = "id", nullable = false)
  private UpgradePlanDetailEntity upgradePlanDetailEntity;


  /**
   * @param detail
   *          the plan detail
   */
  void setPlanDetail(UpgradePlanDetailEntity detail) {
    upgradePlanDetailEntity = detail;
  }

  /**
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * @param configType
   *          the type
   */
  public void setType(String configType) {
    type = configType;
  }

  /**
   * @return the key
   */
  public String getKey() {
    return key;
  }

  /**
   * @param configKey
   *          the key
   */
  public void setKey(String configKey) {
    key = configKey;
  }

  /**
   * @return the new value
   */
  public String getNewValue() {
    return newValue;
  }

  /**
   * @param value
   *          the new value
   */
  public void setNewValue(String value) {
    newValue = value;
  }

  /**
   * @return if the {{@link #getKey()} is to be removed
   */
  public boolean isRemove() {
    return remove != 0;
  }

  /**
   * @param toRemove
   *          {@code true} to remove property of the specified key
   */
  public void setRemove(boolean toRemove) {
    remove = toRemove ? (short) 1 : (short) 0;
  }

}
