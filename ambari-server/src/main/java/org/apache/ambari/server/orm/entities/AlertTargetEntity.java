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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PreRemove;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

/**
 * The {@link AlertTargetEntity} class represents audience that will receive
 * dispatches when an alert is triggered.
 */
@Entity
@Table(name = "alert_target")
@TableGenerator(name = "alert_target_id_generator", table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value", pkColumnValue = "alert_target_id_seq", initialValue = 0, allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "AlertTargetEntity.findAll", query = "SELECT alertTarget FROM AlertTargetEntity alertTarget"),
    @NamedQuery(name = "AlertTargetEntity.findByName", query = "SELECT alertTarget FROM AlertTargetEntity alertTarget WHERE alertTarget.targetName = :targetName"), })
public class AlertTargetEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "alert_target_id_generator")
  @Column(name = "target_id", nullable = false, updatable = false)
  private Long targetId;

  @Column(length = 1024)
  private String description;

  @Column(name = "notification_type", nullable = false, length = 64)
  private String notificationType;

  @Lob
  @Basic
  @Column(length = 32672)
  private String properties;

  @Column(name = "target_name", unique = true, nullable = false, length = 255)
  private String targetName;

  /**
   * Bi-directional many-to-many association to {@link AlertGroupEntity}
   */
  @ManyToMany(mappedBy = "alertTargets", cascade = { CascadeType.MERGE,
      CascadeType.REFRESH })
  private Set<AlertGroupEntity> alertGroups;

  /**
   * Gets the unique ID of this alert target.
   *
   * @return the ID of the target (never {@code null}).
   */
  public Long getTargetId() {
    return targetId;
  }

  /**
   * Sets the unique ID of this alert target.
   *
   * @param targetId
   *          the ID of the alert target (not {@code null}).
   */
  public void setTargetId(Long targetId) {
    this.targetId = targetId;
  }

  /**
   * Gets the description of this alert target.
   *
   * @return the description or {@code null} if none.
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the description for this alert target.
   *
   * @param description
   *          the description or {@code null} for none.
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * @return
   */
  public String getNotificationType() {
    return notificationType;
  }

  /**
   * @param notificationType
   */
  public void setNotificationType(String notificationType) {
    this.notificationType = notificationType;
  }

  /**
   * @return
   */
  public String getProperties() {
    return properties;
  }

  /**
   * @param properties
   */
  public void setProperties(String properties) {
    this.properties = properties;
  }

  /**
   * Gets the name of this alert target.
   *
   * @return the alert target name (never {@code null}).
   */
  public String getTargetName() {
    return targetName;
  }

  /**
   * Sets the name of this alert target.
   *
   * @param targetName
   *          the name (not {@code null}).
   */
  public void setTargetName(String targetName) {
    this.targetName = targetName;
  }

  /**
   * Gets an immutable set of the alert groups that this target is associated
   * with.
   *
   * @return the groups that will send to this target when an alert in that
   *         group is received, or an empty set for none.
   */
  public Set<AlertGroupEntity> getAlertGroups() {
    if (null == alertGroups) {
      return Collections.emptySet();
    }

    return Collections.unmodifiableSet(alertGroups);
  }

  /**
   * Adds the specified alert group to the groups that this target is associated
   * with. This is used to complement the JPA bidirectional association.
   *
   * @param alertGroup
   */
  protected void addAlertGroup(AlertGroupEntity alertGroup) {
    if (null == alertGroups) {
      alertGroups = new HashSet<AlertGroupEntity>();
    }

    alertGroups.add(alertGroup);
  }

  /**
   * Removes the specified alert group to the groups that this target is
   * associated with. This is used to complement the JPA bidirectional
   * association.
   *
   * @param alertGroup
   */
  protected void removeAlertGroup(AlertGroupEntity alertGroup) {
    if (null != alertGroups) {
      alertGroups.remove(alertGroup);
    }
  }

  /**
   * Called before {@link EntityManager#remove(Object)} for this entity, removes
   * the non-owning relationship between targets and groups.
   */
  @PreRemove
  public void preRemove() {
    Set<AlertGroupEntity> groups = getAlertGroups();
    if (null == groups || groups.size() == 0) {
      return;
    }

    for (AlertGroupEntity group : groups) {
      group.removeAlertTarget(this);
    }
  }

  /**
   *
   */
  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }

    if (object == null || getClass() != object.getClass()) {
      return false;
    }

    AlertTargetEntity that = (AlertTargetEntity) object;

    if (targetId != null ? !targetId.equals(that.targetId)
        : that.targetId != null) {
      return false;
    }

    return true;
  }

  /**
   *
   */
  @Override
  public int hashCode() {
    int result = null != targetId ? targetId.hashCode() : 0;
    return result;
  }

}