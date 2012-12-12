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

import javax.persistence.*;
import java.util.Date;

@javax.persistence.Table(name = "servicecomponentconfig", schema = "ambari", catalog = "")
@Entity
@SequenceGenerator(name = "ambari.servicecomponentconfig_config_version_seq", allocationSize = 1)
public class ServiceComponentConfigEntity {
  private Integer configVersion;

  @javax.persistence.Column(name = "config_version", nullable = false, insertable = true, updatable = true, length = 10)
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ambari.servicecomponentconfig_config_version_seq")
  public Integer getConfigVersion() {
    return configVersion;
  }

  public void setConfigVersion(Integer configVersion) {
    this.configVersion = configVersion;
  }

  private String componentName;

  @javax.persistence.Column(name = "component_name", nullable = false, insertable = true, updatable = true)
  @Basic
  public String getComponentName() {
    return componentName;
  }

  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  private String configSnapshot= "";

  @javax.persistence.Column(name = "config_snapshot", nullable = false, insertable = true, updatable = true)
  @Basic
  public String getConfigSnapshot() {
    return configSnapshot;
  }

  public void setConfigSnapshot(String configSnapshot) {
    this.configSnapshot = configSnapshot;
  }

  private Date configSnapshotTime;

  @javax.persistence.Column(name = "config_snapshot_time", nullable = false, insertable = true, updatable = true, length = 29, precision = 6)
  @Temporal(TemporalType.TIMESTAMP)
  public Date getConfigSnapshotTime() {
    return configSnapshotTime;
  }

  public void setConfigSnapshotTime(Date configSnapshotTime) {
    this.configSnapshotTime = configSnapshotTime;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ServiceComponentConfigEntity that = (ServiceComponentConfigEntity) o;

    if (configVersion != null ? !configVersion.equals(that.configVersion) : that.configVersion != null) return false;
    if (componentName != null ? !componentName.equals(that.componentName) : that.componentName != null) return false;
    if (configSnapshot != null ? !configSnapshot.equals(that.configSnapshot) : that.configSnapshot != null)
      return false;
    if (configSnapshotTime != null ? !configSnapshotTime.equals(that.configSnapshotTime) : that.configSnapshotTime != null)
      return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = configVersion != null ? configVersion.hashCode() : 0;
    result = 31 * result + (componentName != null ? componentName.hashCode() : 0);
    result = 31 * result + (configSnapshot != null ? configSnapshot.hashCode() : 0);
    result = 31 * result + (configSnapshotTime != null ? configSnapshotTime.hashCode() : 0);
    return result;
  }

  private ClusterServiceEntity clusterServiceEntity;

  @ManyToOne
  @javax.persistence.JoinColumns({@javax.persistence.JoinColumn(name = "cluster_id", referencedColumnName = "cluster_id", nullable = false), @javax.persistence.JoinColumn(name = "service_name", referencedColumnName = "service_name", nullable = false)})
  public ClusterServiceEntity getClusterServiceEntity() {
    return clusterServiceEntity;
  }

  public void setClusterServiceEntity(ClusterServiceEntity clusterServiceEntity) {
    this.clusterServiceEntity = clusterServiceEntity;
  }
}
