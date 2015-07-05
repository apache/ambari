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

import org.apache.ambari.server.state.stack.upgrade.Direction;

/**
 * Models the data representation of an upgrade
 */
@Table(name = "upgrade")
@Entity
@TableGenerator(name = "upgrade_id_generator",
    table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value",
    pkColumnValue = "upgrade_id_seq", initialValue = 0)
@NamedQueries({
  @NamedQuery(name = "UpgradeEntity.findAllForCluster",
      query = "SELECT u FROM UpgradeEntity u WHERE u.clusterId = :clusterId"),
  @NamedQuery(name = "UpgradeEntity.findUpgrade",
      query = "SELECT u FROM UpgradeEntity u WHERE u.upgradeId = :upgradeId"),
  @NamedQuery(name = "UpgradeEntity.findLatestForCluster",
      query = "SELECT u FROM UpgradeEntity u WHERE u.clusterId = :clusterId AND u.direction = :direction ORDER BY u.upgradeId DESC"),
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


}
