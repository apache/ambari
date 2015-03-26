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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.List;

@Entity
@Table(name = "user_widget")
@NamedQueries({
    @NamedQuery(name = "UserWidgetEntity.findAll", query = "SELECT userWidget FROM UserWidgetEntity userWidget"),
    @NamedQuery(name = "UserWidgetEntity.findByCluster", query = "SELECT userWidget FROM UserWidgetEntity userWidget WHERE userWidget.clusterId = :clusterId"),
    @NamedQuery(name = "UserWidgetEntity.findBySectionName", query =
                "SELECT userWidget FROM UserWidgetEntity userWidget " +
                "INNER JOIN userWidget.listWidgetLayoutUserWidgetEntity widgetLayoutUserWidget " +
                "INNER JOIN widgetLayoutUserWidget.widgetLayout  widgetLayout " +
                "WHERE widgetLayout.sectionName = :sectionName")
        })
public class UserWidgetEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "user_widget_name", nullable = false, length = 255)
  private String userWidgetName;

  @Column(name = "user_widget_type", nullable = false, length = 255)
  private String userWidgetType;

  @Column(name = "metrics", length = 32672)
  private String metrics;

  @Column(name = "time_created", nullable = false, length = 255)
  private Long timeCreated = System.currentTimeMillis();

  @Column(name = "author", length = 255)
  private String author;

  @Column(name = "description", length = 255)
  private String description;

  @Column(name = "display_name", nullable = false,  length = 255)
  private String displayName;

  @Column(name = "scope", length = 255)
  private String scope;

  @Column(name = "widget_values", length = 255)
  private String widgetValues;

  @Column(name = "properties", length = 255)
  private String properties;

  @Column(name = "cluster_id", nullable = false)
  private Long clusterId;

  @ManyToOne
  @JoinColumn(name = "cluster_id", referencedColumnName = "cluster_id", nullable = false, updatable = false, insertable = false)
  private ClusterEntity clusterEntity;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "userWidget")
  private List<WidgetLayoutUserWidgetEntity> listWidgetLayoutUserWidgetEntity;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getUserWidgetName() {
    return userWidgetName;
  }

  public void setUserWidgetName(String userWidgetName) {
    this.userWidgetName = userWidgetName;
  }

  public String getUserWidgetType() {
    return userWidgetType;
  }

  public void setUserWidgetType(String userWidgetType) {
    this.userWidgetType = userWidgetType;
  }

  public String getMetrics() {
    return metrics;
  }

  public void setMetrics(String metrics) {
    this.metrics = metrics;
  }

  public Long getTimeCreated() {
    return timeCreated;
  }

  public void setTimeCreated(Long timeCreated) {
    this.timeCreated = timeCreated;
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getScope() {
    return scope;
  }

  public void setScope(String scope) {
    this.scope = scope;
  }

  public String getWidgetValues() {
    return widgetValues;
  }

  public void setWidgetValues(String widgetValues) {
    this.widgetValues = widgetValues;
  }

  public String getProperties() {
    return properties;
  }

  public void setProperties(String properties) {
    this.properties = properties;
  }

  public ClusterEntity getClusterEntity() {
    return clusterEntity;
  }

  public void setClusterEntity(ClusterEntity clusterEntity) {
    this.clusterEntity = clusterEntity;
  }

  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  public List<WidgetLayoutUserWidgetEntity> getListWidgetLayoutUserWidgetEntity() {
    return listWidgetLayoutUserWidgetEntity;
  }

  public void setListWidgetLayoutUserWidgetEntity(List<WidgetLayoutUserWidgetEntity> listWidgetLayoutUserWidgetEntity) {
    this.listWidgetLayoutUserWidgetEntity = listWidgetLayoutUserWidgetEntity;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    UserWidgetEntity that = (UserWidgetEntity) o;

    if (id != that.id) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = null != id ? id.hashCode() : 0;
    return result;
  }

}