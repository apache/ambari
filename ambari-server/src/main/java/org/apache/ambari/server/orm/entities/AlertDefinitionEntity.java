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

import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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
import javax.persistence.UniqueConstraint;

import org.apache.ambari.server.state.alert.Scope;

/**
 * The {@link AlertDefinitionEntity} class is used to model an alert that needs
 * to run in the system. Each received alert from an agent will essentially be
 * an instance of this template.
 */
@Entity
@Table(name = "alert_definition", uniqueConstraints = @UniqueConstraint(columnNames = {
    "cluster_id", "definition_name" }))
@TableGenerator(name = "alert_definition_id_generator", table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value", pkColumnValue = "alert_definition_id_seq", initialValue = 0, allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "AlertDefinitionEntity.findAll", query = "SELECT alertDefinition FROM AlertDefinitionEntity alertDefinition"),
    @NamedQuery(name = "AlertDefinitionEntity.findAllInCluster", query = "SELECT alertDefinition FROM AlertDefinitionEntity alertDefinition WHERE alertDefinition.clusterId = :clusterId"),
    @NamedQuery(name = "AlertDefinitionEntity.findByName", query = "SELECT alertDefinition FROM AlertDefinitionEntity alertDefinition WHERE alertDefinition.definitionName = :definitionName AND alertDefinition.clusterId = :clusterId"), })
public class AlertDefinitionEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "alert_definition_id_generator")
  @Column(name = "definition_id", nullable = false, updatable = false)
  private Long definitionId;

  @Lob
  @Basic
  @Column(name = "alert_source", nullable = false, length = 32672)
  private String source;

  @Column(name = "cluster_id", nullable = false)
  private Long clusterId;

  @Column(name = "component_name", length = 255)
  private String componentName;

  @Column(name = "definition_name", nullable = false, length = 255)
  private String definitionName;

  @Column(name = "label", nullable = true, length = 255)
  private String label;

  @Column(name = "scope", length = 255)
  @Enumerated(value = EnumType.STRING)
  private Scope scope;

  @Column(nullable = false)
  private Integer enabled = Integer.valueOf(1);

  @Column(nullable = false, length = 64)
  private String hash;

  @Column(name = "schedule_interval", nullable = false)
  private Integer scheduleInterval;

  @Column(name = "service_name", nullable = false, length = 255)
  private String serviceName;

  @Column(name = "source_type", nullable = false, length = 255)
  private String sourceType;

  /**
   * Bi-directional many-to-many association to {@link AlertGroupEntity}
   */
  @ManyToMany(mappedBy = "alertDefinitions", cascade = { CascadeType.PERSIST,
      CascadeType.MERGE, CascadeType.REFRESH })
  private Set<AlertGroupEntity> alertGroups;

  /**
   * Constructor.
   */
  public AlertDefinitionEntity() {
  }

  /**
   * Gets the unique identifier for this alert definition.
   *
   * @return the ID.
   */
  public Long getDefinitionId() {
    return definitionId;
  }

  /**
   * Sets the unique identifier for this alert definition.
   *
   * @param definitionId
   *          the ID (not {@code null}).
   */
  public void setDefinitionId(Long definitionId) {
    this.definitionId = definitionId;
  }

  /**
   * Gets the source that defines the type of alert and the alert properties.
   * This is typically a JSON structure that can be mapped to a first-class
   * object.
   *
   * @return the alert source (never {@code null}).
   */
  public String getSource() {
    return source;
  }

  /**
   * Sets the source of the alert, typically in JSON, that defines the type of
   * the alert and its properties.
   *
   * @param alertSource
   *          the alert source (not {@code null}).
   */
  public void setSource(String alertSource) {
    source = alertSource;
  }

  /**
   * Gets the ID of the cluster that this alert definition is created for. Each
   * cluster has their own set of alert definitions that are not shared with any
   * other cluster.
   *
   * @return the ID of the cluster (never {@code null}).
   */
  public Long getClusterId() {
    return clusterId;
  }

  /**
   * Sets the ID of the cluster that this alert definition is created for. Each
   * cluster has their own set of alert definitions that are not shared with any
   * other cluster.
   *
   * @param clusterId
   *          the ID of the cluster (not {@code null}).
   */
  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  /**
   * Gets the component name that this alert is associated with, if any. Some
   * alerts are scoped at the service level and will not have a component name.
   *
   * @return the component name or {@code null} if none.
   */
  public String getComponentName() {
    return componentName;
  }

  /**
   * Sets the component name that this alert is associated with, if any. Some
   * alerts are scoped at the service level and will not have a component name.
   *
   * @param componentName
   *          the component name or {@code null} if none.
   */
  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  /**
   * Gets the scope of the alert definition. The scope is defined as either
   * being for a {@link Scope#SERVICE} or {@link Scope#HOST}.
   *
   * @return the scope, or {@code null} if not defined.
   */
  public Scope getScope() {
    return scope;
  }

  /**
   * Sets the scope of the alert definition. The scope is defined as either
   * being for a {@link Scope#SERVICE} or {@link Scope#HOST}.
   *
   * @param scope
   *          the scope to set, or {@code null} for none.
   */
  public void setScope(Scope scope) {
    this.scope = scope;
  }

  /**
   * Gets the name of this alert definition. Alert definition names are unique
   * within a cluster.
   *
   * @return the name of the alert definition (never {@code null}).
   */
  public String getDefinitionName() {
    return definitionName;
  }

  /**
   * Sets the name of this alert definition. Alert definition names are unique
   * within a cluster.
   *
   * @param definitionName
   *          the name of the alert definition (not {@code null}).
   */
  public void setDefinitionName(String definitionName) {
    this.definitionName = definitionName;
  }

  /**
   * Gets whether this alert definition is enabled. Disabling an alert
   * definition will prevent agents from scheduling the alerts. No alerts will
   * be triggered and no alert data will be collected.
   *
   * @return {@code true} if this alert definition is enabled, {@code false}
   *         otherwise.
   */
  public boolean getEnabled() {
    return enabled == 0 ? false : true;
  }

  /**
   * Sets whether this alert definition is enabled.
   *
   * @param enabled
   *          {@code true} if this alert definition is enabled, {@code false}
   *          otherwise.
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled ? 1 : 0;
  }

  /**
   * Gets the unique hash for the current state of this definition. If a
   * property of this definition changes, a new hash is calculated.
   *
   * @return the unique hash or {@code null} if there is none.
   */
  public String getHash() {
    return hash;
  }

  /**
   * Gets the unique hash for the current state of this definition. If a
   * property of this definition changes, a new hash is calculated.
   *
   * @param hash
   *          the unique hash to set or {@code null} for none.
   */
  public void setHash(String hash) {
    this.hash = hash;
  }

  /**
   * Gets the alert trigger interval, in seconds.
   *
   * @return the interval, in seconds.
   */
  public Integer getScheduleInterval() {
    return scheduleInterval;
  }

  /**
   * Sets the alert trigger interval, in seconds.
   *
   * @param scheduleInterval
   *          the interval, in seconds.
   */
  public void setScheduleInterval(Integer scheduleInterval) {
    this.scheduleInterval = scheduleInterval;
  }

  /**
   * Gets the name of the service that this alert definition is associated with.
   * Every alert definition is associated with exactly one service.
   *
   * @return the name of the service (never {@code null}).
   */
  public String getServiceName() {
    return serviceName;
  }

  /**
   * Gets the name of the service that this alert definition is associated with.
   * Every alert definition is associated with exactly one service.
   *
   * @param serviceName
   *          the name of the service (not {@code null}).
   */
  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  /**
   * @return
   */
  // !!! FIXME: Create enumeration for this
  public String getSourceType() {
    return sourceType;
  }

  /**
   * @param sourceType
   */
  // !!! FIXME: Create enumeration for this
  public void setSourceType(String sourceType) {
    this.sourceType = sourceType;
  }

  /**
   * Gets the alert groups that this alert definition is associated with.
   *
   * @return the groups, or {@code null} if none.
   */
  public Set<AlertGroupEntity> getAlertGroups() {
    return alertGroups;
  }

  /**
   * Sets the alert groups that this alert definition is associated with.
   *
   * @param alertGroups
   *          the groups, or {@code null} for none.
   */
  public void setAlertGroups(Set<AlertGroupEntity> alertGroups) {
    this.alertGroups = alertGroups;
  }

  /**
   * Sets a human readable label for this alert definition.
   * 
   * @param label
   *          the label or {@code null} if none.
   */
  public void setLabel(String label) {
    this.label = label;
  }

  /**
   * Gets the label for this alert definition.
   * 
   * @return the label or {@code null} if none.
   */
  public String getLabel() {
    return label;
  }

  /**
   * Called before {@link EntityManager#remove(Object)} for this entity, removes
   * the non-owning relationship between definitions and groups.
   */
  @PreRemove
  public void preRemove() {
    Set<AlertGroupEntity> groups = getAlertGroups();
    if (null == groups || groups.size() == 0) {
      return;
    }

    for (AlertGroupEntity group : groups) {
      Set<AlertDefinitionEntity> definitions = group.getAlertDefinitions();
      if (null != definitions) {
        definitions.remove(this);
      }
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

    AlertDefinitionEntity that = (AlertDefinitionEntity) object;

    if (definitionId != null ? !definitionId.equals(that.definitionId)
        : that.definitionId != null) {
      return false;
    }

    return true;
  }

  /**
   *
   */
  @Override
  public int hashCode() {
    int result = null != definitionId ? definitionId.hashCode() : 0;
    return result;
  }
}