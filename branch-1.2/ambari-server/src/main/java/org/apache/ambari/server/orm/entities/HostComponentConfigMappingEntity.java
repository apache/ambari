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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@IdClass(HostComponentConfigMappingEntityPK.class)
@Table(name = "hostcomponentconfigmapping", schema = "ambari", catalog = "")
@Entity
public class HostComponentConfigMappingEntity {
  private Long clusterId;
  private String serviceName;
  private String componentName;
  private String hostName;
  private String configType;
  private String configTag;
  private Long timestamp;
  private HostComponentStateEntity hostComponentStateEntity;
  private ClusterConfigEntity clusterConfigEntity;

  @Column(name = "cluster_id", insertable = false, updatable = false, nullable = false)
  @Id
  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long id) {
    clusterId = id;
  }

  @Column(name = "service_name", insertable = false, updatable = false, nullable = false)
  @Id
  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String name) {
    serviceName = name;
  }

  @Column(name = "component_name", insertable = false, updatable = false, nullable = false)
  @Id
  public String getComponentName() {
    return componentName;
  }

  public void setComponentName(String name) {
    componentName = name;
  }

  @Column(name = "host_name", insertable = false, updatable = false, nullable = false)
  @Id
  public String getHostName() {
    return hostName;
  }

  public void setHostName(String name) {
    hostName = name;
  }

  @Column(name = "config_type", insertable = true, updatable = false, nullable = false)
  @Id
  public String getConfigType() {
    return configType;
  }

  public void setConfigType(String type) {
    configType = type;
  }

  @Column(name = "config_tag", nullable = false, insertable = true, updatable = true)
  public String getVersionTag() {
    return configTag;
  }

  public void setVersionTag(String tag) {
    configTag = tag;
  }

  @Column(name="timestamp", nullable = false, insertable = true, updatable = true)
  public Long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Long stamp) {
    timestamp = stamp;
  }

  @ManyToOne
  @JoinColumns({
    @JoinColumn(name = "cluster_id", referencedColumnName = "cluster_id", nullable = false),
    @JoinColumn(name = "service_name", referencedColumnName = "service_name", nullable = false),
    @JoinColumn(name = "component_name", referencedColumnName = "component_name", nullable = false),
    @JoinColumn(name = "host_name", referencedColumnName = "host_name", nullable = false) })
  public HostComponentStateEntity getHostComponentStateEntity() {
    return hostComponentStateEntity;
  }

  public void setHostComponentStateEntity(HostComponentStateEntity entity) {
    hostComponentStateEntity = entity;
  }

  @ManyToOne
  @JoinColumns({
      @JoinColumn(name = "cluster_id", referencedColumnName = "cluster_id", nullable = false, insertable = false, updatable = false),
      @JoinColumn(name = "config_type", referencedColumnName = "type_name", nullable = false, insertable = false, updatable = false),
      @JoinColumn(name = "config_tag", referencedColumnName = "version_tag", nullable = false, insertable = false, updatable = false)
  })
  public ClusterConfigEntity getClusterConfigEntity() {
    return clusterConfigEntity;
  }

  public void setClusterConfigEntity(ClusterConfigEntity clusterConfigEntity) {
    this.clusterConfigEntity = clusterConfigEntity;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    HostComponentConfigMappingEntity that = (HostComponentConfigMappingEntity) o;

    if (clusterId != null ? !clusterId.equals(that.clusterId) : that.clusterId != null) return false;
    if (serviceName != null ? !serviceName.equals(that.serviceName) : that.serviceName != null) return false;
    if (componentName != null ? !componentName.equals(that.componentName) : that.componentName != null) return false;
    if (hostName != null ? !hostName.equals(that.hostName) : that.hostName != null) return false;
    if (configType != null ? !configType.equals(that.configType) : that.configType != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = clusterId !=null ? clusterId.intValue() : 0;
    result = 31 * result + (serviceName != null ? serviceName.hashCode() : 0);
    result = 31 * result + (componentName != null ? componentName.hashCode() : 0);
    result = 31 * result + (hostName != null ? hostName.hashCode() : 0);
    result = 31 * result + (configType != null ? configType.hashCode() : 0);
    return result;
  }


}
