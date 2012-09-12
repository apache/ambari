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

@Table(name = "servicecomponenthostconfig", schema = "ambari", catalog = "")
@Entity
public class ServiceComponentHostConfigEntity {

  private Integer configVersion;

  @Column(name = "config_version")
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  public Integer getConfigVersion() {
    return configVersion;
  }

  public void setConfigVersion(Integer configVersion) {
    this.configVersion = configVersion;
  }

  private String clusterName;

  @Column(name = "cluster_name", nullable = false, insertable = false, updatable = false)
  @Basic
  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  private String serviceName;

  @Column(name = "service_name", nullable = false, insertable = false, updatable = false)
  @Basic
  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  private String componentName;

  @Column(name = "component_name", nullable = false)
  @Basic
  public String getComponentName() {
    return componentName;
  }

  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  private String hostName;

  @Column(name = "host_name", nullable = false, insertable = false, updatable = false)
  @Basic
  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  private String configSnapshot = "";

  @Column(name = "config_snapshot", nullable = false)
  @Basic
  public String getConfigSnapshot() {
    return configSnapshot;
  }

  public void setConfigSnapshot(String configSnapshot) {
    this.configSnapshot = configSnapshot;
  }

  private Date configSnapshotTime;

  @Column(name = "config_snapshot_time", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  public Date getConfigSnapshotTime() {
    return configSnapshotTime;
  }

  public void setConfigSnapshotTime(Date configSnapshotTime) {
    this.configSnapshotTime = configSnapshotTime;
  }

  private ClusterServiceEntity clusterServiceEntity;

  @ManyToOne
  @JoinColumns(value = {@JoinColumn(name = "cluster_name", referencedColumnName = "cluster_name"), @JoinColumn(name = "service_name", referencedColumnName = "service_name")})
  public ClusterServiceEntity getClusterServiceEntity() {
    return clusterServiceEntity;
  }

  public void setClusterServiceEntity(ClusterServiceEntity clusterServiceEntity) {
    this.clusterServiceEntity = clusterServiceEntity;
  }

  private HostEntity hostEntity;

  @ManyToOne
  @JoinColumn(name = "host_name")
  public HostEntity getHostEntity() {
    return hostEntity;
  }

  public void setHostEntity(HostEntity hostEntity) {
    this.hostEntity = hostEntity;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ServiceComponentHostConfigEntity that = (ServiceComponentHostConfigEntity) o;

    if (clusterName != null ? !clusterName.equals(that.clusterName) : that.clusterName != null) return false;
    if (componentName != null ? !componentName.equals(that.componentName) : that.componentName != null) return false;
    if (configSnapshot != null ? !configSnapshot.equals(that.configSnapshot) : that.configSnapshot != null)
      return false;
    if (configVersion != null ? !configVersion.equals(that.configVersion) : that.configVersion != null) return false;
    if (hostName != null ? !hostName.equals(that.hostName) : that.hostName != null) return false;
    if (serviceName != null ? !serviceName.equals(that.serviceName) : that.serviceName != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = configVersion != null ? configVersion.hashCode() : 0;
    result = 31 * result + (clusterName != null ? clusterName.hashCode() : 0);
    result = 31 * result + (serviceName != null ? serviceName.hashCode() : 0);
    result = 31 * result + (componentName != null ? componentName.hashCode() : 0);
    result = 31 * result + (hostName != null ? hostName.hashCode() : 0);
    result = 31 * result + (configSnapshot != null ? configSnapshot.hashCode() : 0);
    return result;
  }
}
