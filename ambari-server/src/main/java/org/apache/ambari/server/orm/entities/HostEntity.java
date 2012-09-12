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
import java.util.List;

@Table(name = "hosts", schema = "ambari", catalog = "")
@Entity
public class HostEntity {

  private String clusterName;

  @Column(name = "cluster_name", insertable = false, updatable = false)
  @Basic
  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  private String hostName;

  @Column(name = "host_name", nullable = false)
  @Id
  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  private String ip;

  @Column(name = "ip", unique = true, nullable = false)
  @Basic
  public String getIp() {
    return ip;
  }

  public void setIp(String ip) {
    this.ip = ip;
  }

  private Integer totalMem = 0;

  @Column(name = "total_mem", nullable = false)
  @Basic
  public Integer getTotalMem() {
    return totalMem;
  }

  public void setTotalMem(Integer totalMem) {
    this.totalMem = totalMem;
  }

  private Integer cpuCount = 0;

  @Column(name = "cpu_count", nullable = false)
  @Basic
  public Integer getCpuCount() {
    return cpuCount;
  }

  public void setCpuCount(Integer cpuCount) {
    this.cpuCount = cpuCount;
  }

  private String cpuInfo = "";

  @Column(name = "cpu_info", nullable = false)
  @Basic
  public String getCpuInfo() {
    return cpuInfo;
  }

  public void setCpuInfo(String cpuInfo) {
    this.cpuInfo = cpuInfo;
  }

  private String osArch = "";

  @Column(name = "os_arch", nullable = false)
  @Basic
  public String getOsArch() {
    return osArch;
  }

  public void setOsArch(String osArch) {
    this.osArch = osArch;
  }

  private String disksInfo = "";

  @Column(name = "disks_info", nullable = false)
  @Basic
  public String getDisksInfo() {
    return disksInfo;
  }

  public void setDisksInfo(String disksInfo) {
    this.disksInfo = disksInfo;
  }

  private String osInfo = "";

  @Column(name = "os_info", nullable = false)
  @Basic
  public String getOsInfo() {
    return osInfo;
  }

  public void setOsInfo(String osInfo) {
    this.osInfo = osInfo;
  }

  private String osType = "";

  @Column(name = "os_type", nullable = false)
  @Basic
  public String getOsType() {
    return osType;
  }

  public void setOsType(String osType) {
    this.osType = osType;
  }

  private String discoveryStatus = "";

  @Column(name = "discovery_status", nullable = false)
  @Basic
  public String getDiscoveryStatus() {
    return discoveryStatus;
  }

  public void setDiscoveryStatus(String discoveryStatus) {
    this.discoveryStatus = discoveryStatus;
  }

  private Integer lastRegistrationTime = 0;

  @Column(name = "last_registration_time", nullable = false)
  @Basic
  public Integer getLastRegistrationTime() {
    return lastRegistrationTime;
  }

  public void setLastRegistrationTime(Integer lastRegistrationTime) {
    this.lastRegistrationTime = lastRegistrationTime;
  }

  private String rackInfo = "/default-rack";

  @Column(name = "rack_info", nullable = false)
  @Basic
  public String getRackInfo() {
    return rackInfo;
  }

  public void setRackInfo(String rackInfo) {
    this.rackInfo = rackInfo;
  }

  private String hostAttributes = "";

  @Column(name = "host_attributes", nullable = false)
  @Basic
  public String getHostAttributes() {
    return hostAttributes;
  }

  public void setHostAttributes(String hostAttributes) {
    this.hostAttributes = hostAttributes;
  }

  private ClusterEntity clusterEntity;

  @ManyToOne
  @JoinColumn(name = "cluster_name")
  public ClusterEntity getClusterEntity() {
    return clusterEntity;
  }

  public void setClusterEntity(ClusterEntity clusterEntity) {
    this.clusterEntity = clusterEntity;
  }

  private HostStateEntity hostStateEntity;

  @OneToOne(cascade = CascadeType.ALL, mappedBy = "hostEntity")
  public HostStateEntity getHostStateEntity() {
    return hostStateEntity;
  }

  public void setHostStateEntity(HostStateEntity hostStateEntity) {
    this.hostStateEntity = hostStateEntity;
  }

  private List<ActionStatusEntity> actionStatusEntity;

  @OneToMany(mappedBy = "hostEntity")
  public List<ActionStatusEntity> getActionStatusEntity() {
    return actionStatusEntity;
  }

  public void setActionStatusEntity(List<ActionStatusEntity> actionStatusEntity) {
    this.actionStatusEntity = actionStatusEntity;
  }

  private List<ServiceComponentHostConfigEntity> serviceComponentHostConfigEntities;

  @OneToMany(mappedBy = "hostEntity")
  public List<ServiceComponentHostConfigEntity> getServiceComponentHostConfigEntities() {
    return serviceComponentHostConfigEntities;
  }

  public void setServiceComponentHostConfigEntities(List<ServiceComponentHostConfigEntity> serviceComponentHostConfigEntities) {
    this.serviceComponentHostConfigEntities = serviceComponentHostConfigEntities;
  }

  private List<ComponentHostDesiredStateEntity> componentHostDesiredStateEntities;

  @OneToMany(mappedBy = "hostEntity")
  public List<ComponentHostDesiredStateEntity> getComponentHostDesiredStateEntities() {
    return componentHostDesiredStateEntities;
  }

  public void setComponentHostDesiredStateEntities(List<ComponentHostDesiredStateEntity> componentHostDesiredStateEntities) {
    this.componentHostDesiredStateEntities = componentHostDesiredStateEntities;
  }

  private List<HostComponentStateEntity> hostComponentStateEntities;

  @OneToMany(mappedBy = "hostEntity")
  public List<HostComponentStateEntity> getHostComponentStateEntities() {
    return hostComponentStateEntities;
  }

  public void setHostComponentStateEntities(List<HostComponentStateEntity> hostComponentStateEntities) {
    this.hostComponentStateEntities = hostComponentStateEntities;
  }

  private List<ServiceStateEntity> serviceStateEntities;

  @OneToMany(mappedBy = "hostEntity")
  public List<ServiceStateEntity> getServiceStateEntities() {
    return serviceStateEntities;
  }

  public void setServiceStateEntities(List<ServiceStateEntity> serviceStateEntities) {
    this.serviceStateEntities = serviceStateEntities;
  }

  private List<ServiceComponentStateEntity> serviceComponentStateEntities;

  @OneToMany(mappedBy = "hostEntity")
  public List<ServiceComponentStateEntity> getServiceComponentStateEntities() {
    return serviceComponentStateEntities;
  }

  public void setServiceComponentStateEntities(List<ServiceComponentStateEntity> serviceComponentStateEntities) {
    this.serviceComponentStateEntities = serviceComponentStateEntities;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    HostEntity that = (HostEntity) o;

    if (clusterName != null ? !clusterName.equals(that.clusterName) : that.clusterName != null) return false;
    if (cpuCount != null ? !cpuCount.equals(that.cpuCount) : that.cpuCount != null) return false;
    if (cpuInfo != null ? !cpuInfo.equals(that.cpuInfo) : that.cpuInfo != null) return false;
    if (discoveryStatus != null ? !discoveryStatus.equals(that.discoveryStatus) : that.discoveryStatus != null)
      return false;
    if (disksInfo != null ? !disksInfo.equals(that.disksInfo) : that.disksInfo != null) return false;
    if (hostAttributes != null ? !hostAttributes.equals(that.hostAttributes) : that.hostAttributes != null)
      return false;
    if (hostName != null ? !hostName.equals(that.hostName) : that.hostName != null) return false;
    if (ip != null ? !ip.equals(that.ip) : that.ip != null) return false;
    if (lastRegistrationTime != null ? !lastRegistrationTime.equals(that.lastRegistrationTime) : that.lastRegistrationTime != null)
      return false;
    if (osArch != null ? !osArch.equals(that.osArch) : that.osArch != null) return false;
    if (osInfo != null ? !osInfo.equals(that.osInfo) : that.osInfo != null) return false;
    if (osType != null ? !osType.equals(that.osType) : that.osType != null) return false;
    if (rackInfo != null ? !rackInfo.equals(that.rackInfo) : that.rackInfo != null) return false;
    if (totalMem != null ? !totalMem.equals(that.totalMem) : that.totalMem != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = clusterName != null ? clusterName.hashCode() : 0;
    result = 31 * result + (hostName != null ? hostName.hashCode() : 0);
    result = 31 * result + (ip != null ? ip.hashCode() : 0);
    result = 31 * result + (totalMem != null ? totalMem.hashCode() : 0);
    result = 31 * result + (cpuCount != null ? cpuCount.hashCode() : 0);
    result = 31 * result + (cpuInfo != null ? cpuInfo.hashCode() : 0);
    result = 31 * result + (osArch != null ? osArch.hashCode() : 0);
    result = 31 * result + (disksInfo != null ? disksInfo.hashCode() : 0);
    result = 31 * result + (osInfo != null ? osInfo.hashCode() : 0);
    result = 31 * result + (osType != null ? osType.hashCode() : 0);
    result = 31 * result + (discoveryStatus != null ? discoveryStatus.hashCode() : 0);
    result = 31 * result + (lastRegistrationTime != null ? lastRegistrationTime.hashCode() : 0);
    result = 31 * result + (rackInfo != null ? rackInfo.hashCode() : 0);
    result = 31 * result + (hostAttributes != null ? hostAttributes.hashCode() : 0);
    return result;
  }
}
