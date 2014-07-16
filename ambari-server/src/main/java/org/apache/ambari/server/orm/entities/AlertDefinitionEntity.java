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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * The {@link AlertDefinitionEntity} class is used to model an alert that needs
 * to run in the system. Each received alert from an agent will essentially be
 * an instance of this template.
 */
@Entity
@Table(name = "alert_definition", uniqueConstraints = @UniqueConstraint(columnNames = {
    "cluster_id", "definition_name" }))
@NamedQueries({
    @NamedQuery(name = "AlertDefinitionEntity.findAll", query = "SELECT alertDefinition FROM AlertDefinitionEntity alertDefinition"),
    @NamedQuery(name = "AlertDefinitionEntity.findByName", query = "SELECT alertDefinition FROM AlertDefinitionEntity alertDefinition WHERE alertDefinition.definitionName = :definitionName"), })
public class AlertDefinitionEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.TABLE)
  @Column(name = "definition_id", unique = true, nullable = false, updatable = false)
  private Long definitionId;

  @Column(name = "alert_source", nullable = false, length = 2147483647)
  private String alertSource;

  @Column(name = "cluster_id", nullable = false)
  private Long clusterId;

  @Column(name = "component_name", length = 255)
  private String componentName;

  @Column(name = "definition_name", nullable = false, length = 255)
  private String definitionName;

  @Column(nullable = false)
  private Integer enabled = Integer.valueOf(1);

  @Column(nullable = false, length = 64)
  private String hash;

  @Column(name = "schedule_interval", nullable = false)
  private Long scheduleInterval;

  @Column(name = "service_name", nullable = false, length = 255)
  private String serviceName;

  @Column(name = "source_type", nullable = false, length = 255)
  private String sourceType;

  /**
   * Bi-directional many-to-many association to {@link AlertGroupEntity}
   */
  @ManyToMany(mappedBy = "alertDefinitions")
  private List<AlertGroupEntity> alertGroups;

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
  public String getAlertSource() {
    return alertSource;
  }

  /**
   * Sets the source of the alert, typically in JSON, that defines the type of
   * the alert and its properties.
   * 
   * @param alertSource
   *          the alert source (not {@code null}).
   */
  public void setAlertSource(String alertSource) {
    this.alertSource = alertSource;
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
  public Integer getEnabled() {
    return enabled;
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
  public Long getScheduleInterval() {
    return scheduleInterval;
  }

  /**
   * Sets the alert trigger interval, in seconds.
   * 
   * @param scheduleInterval
   *          the interval, in seconds.
   */
  public void setScheduleInterval(Long scheduleInterval) {
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
  public List<AlertGroupEntity> getAlertGroups() {
    return alertGroups;
  }

  /**
   * Sets the alert groups that this alert definition is associated with.
   * 
   * @param alertGroups
   *          the groups, or {@code null} for none.
   */
  public void setAlertGroups(List<AlertGroupEntity> alertGroups) {
    this.alertGroups = alertGroups;
  }
}