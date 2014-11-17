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

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Transient;

import org.apache.ambari.server.state.UpgradeState;

/**
 * Models a single upgrade item as part of an entire {@link UpgradeEntity}
 */
@Table(name = "upgrade_item")
@Entity
@TableGenerator(name = "upgrade_item_id_generator",
    table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value",
    pkColumnValue = "upgrade_item_id_seq", initialValue = 0, allocationSize = 1)
public class UpgradeItemEntity {

  @Id
  @Column(name = "upgrade_item_id", nullable = false, insertable = true, updatable = false)
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "upgrade_item_id_generator")
  private Long upgradeItemId;

  @Column(name = "upgrade_id", nullable = false, insertable = false, updatable = false)
  private Long upgradeId;

  @Enumerated(value=EnumType.STRING)
  @Column(name = "state", length=255, nullable = false)
  private UpgradeState state = UpgradeState.NONE;

  @Basic
  @Column(name = "hosts")
  private String hosts = null;

  @Basic
  @Column(name = "tasks")
  private String tasks = null;

  @Basic
  @Column(name = "item_text", length = 1024)
  private String itemText = null;

  @ManyToOne
  @JoinColumn(name = "upgrade_id", referencedColumnName = "upgrade_id", nullable = false)
  private UpgradeEntity upgradeEntity;


  /**
   * @return the id
   */
  public Long getId() {
    return upgradeItemId;
  }

  /**
   * @param id the id
   */
  public void setId(Long id) {
    upgradeItemId = id;
  }

  /**
   * @return the state
   */
  public UpgradeState getState() {
    return state;
  }

  /**
   * @param state the state
   */
  public void setState(UpgradeState state) {
    this.state = state;
  }


  /**
   * @return the tasks in json format
   */
  public String getTasks() {
    return tasks;
  }

  /**
   * @param json the tasks in json format
   */
  public void setTasks(String json) {
    tasks = json;
  }

  /**
   * @return the hosts in json format
   */
  public String getHosts() {
    return hosts;
  }

  /**
   * @param json the hosts in json format
   */
  public void setHosts(String json) {
    hosts = json;
  }

  /**
   * @return the item text
   */
  public String getText() {
    return itemText;
  }

  /**
   * @param text the item text
   */
  public void setText(String text) {
    itemText = text;
  }



  public UpgradeEntity getUpgradeEntity() {
    return upgradeEntity;
  }

  public void setUpgradeEntity(UpgradeEntity entity) {
    upgradeEntity = entity;
  }


}
