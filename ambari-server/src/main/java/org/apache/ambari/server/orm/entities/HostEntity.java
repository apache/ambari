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
import java.util.Collection;

@javax.persistence.Table(name = "hosts", schema = "ambari", catalog = "")
@Entity
public class HostEntity {
  private String hostName;

  @javax.persistence.Column(name = "host_name", nullable = false, insertable = true, updatable = true)
  @Id
  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  private String ipv4;

  @javax.persistence.Column(name = "ipv4", nullable = true, insertable = true, updatable = true)
  @Basic
  public String getIpv4() {
    return ipv4;
  }

  public void setIpv4(String ipv4) {
    this.ipv4 = ipv4;
  }

  private String ipv6;

  @javax.persistence.Column(name = "ipv6", nullable = true, insertable = true, updatable = true)
  @Basic
  public String getIpv6() {
    return ipv6;
  }

  public void setIpv6(String ipv6) {
    this.ipv6 = ipv6;
  }
  
  private String publicHostName;
  @Column(name="public_host_name", nullable = true, insertable = true, updatable = true)
  @Basic
  public String getPublicHostName() {
    return publicHostName;
  }
  
  public void setPublicHostName(String name) {
    publicHostName = name;
  }

  private Long totalMem = 0L;

  @javax.persistence.Column(name = "total_mem", nullable = false, insertable = true, updatable = true, length = 10)
  @Basic
  public Long getTotalMem() {
    return totalMem;
  }

  public void setTotalMem(Long totalMem) {
    this.totalMem = totalMem;
  }

  private Integer cpuCount = 0;

  @javax.persistence.Column(name = "cpu_count", nullable = false, insertable = true, updatable = true, length = 10)
  @Basic
  public Integer getCpuCount() {
    return cpuCount;
  }

  public void setCpuCount(Integer cpuCount) {
    this.cpuCount = cpuCount;
  }

  private String cpuInfo = "";

  @javax.persistence.Column(name = "cpu_info", nullable = false, insertable = true, updatable = true)
  @Basic
  public String getCpuInfo() {
    return cpuInfo;
  }

  public void setCpuInfo(String cpuInfo) {
    this.cpuInfo = cpuInfo;
  }

  private String osArch = "";

  @javax.persistence.Column(name = "os_arch", nullable = false, insertable = true, updatable = true)
  @Basic
  public String getOsArch() {
    return osArch;
  }

  public void setOsArch(String osArch) {
    this.osArch = osArch;
  }

  private String disksInfo = "";

  @javax.persistence.Column(name = "disks_info", nullable = false, insertable = true,
		  updatable = true, length = 2000)
  @Basic
  public String getDisksInfo() {
    return disksInfo;
  }

  public void setDisksInfo(String disksInfo) {
    this.disksInfo = disksInfo;
  }

  private String osInfo = "";

  @javax.persistence.Column(name = "os_info", nullable = false, insertable = true, updatable = true,
      length = 1000)
  @Basic
  public String getOsInfo() {
    return osInfo;
  }

  public void setOsInfo(String osInfo) {
    this.osInfo = osInfo;
  }

  private String osType = "";

  @javax.persistence.Column(name = "os_type", nullable = false, insertable = true, updatable = true)
  @Basic
  public String getOsType() {
    return osType;
  }

  public void setOsType(String osType) {
    this.osType = osType;
  }

  private String discoveryStatus = "";

  @javax.persistence.Column(name = "discovery_status", nullable = false, insertable = true, updatable = true,
      length = 2000)
  @Basic
  public String getDiscoveryStatus() {
    return discoveryStatus;
  }

  public void setDiscoveryStatus(String discoveryStatus) {
    this.discoveryStatus = discoveryStatus;
  }

  private Long lastRegistrationTime = 0L;

  @javax.persistence.Column(name = "last_registration_time", nullable = false, insertable = true, updatable = true, length = 10)
  @Basic
  public Long getLastRegistrationTime() {
    return lastRegistrationTime;
  }

  public void setLastRegistrationTime(Long lastRegistrationTime) {
    this.lastRegistrationTime = lastRegistrationTime;
  }

  private String rackInfo = "/default-rack";

  @javax.persistence.Column(name = "rack_info", nullable = false, insertable = true, updatable = true)
  @Basic
  public String getRackInfo() {
    return rackInfo;
  }

  public void setRackInfo(String rackInfo) {
    this.rackInfo = rackInfo;
  }

  private String hostAttributes = "";

  @javax.persistence.Column(name = "host_attributes", nullable = false, insertable = true, updatable = true,
      length = 20000)
  @Basic
  public String getHostAttributes() {
    return hostAttributes;
  }

  public void setHostAttributes(String hostAttributes) {
    this.hostAttributes = hostAttributes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    HostEntity that = (HostEntity) o;

    if (cpuCount != null ? !cpuCount.equals(that.cpuCount) : that.cpuCount != null) return false;
    if (lastRegistrationTime != null ? !lastRegistrationTime.equals(that.lastRegistrationTime) : that.lastRegistrationTime != null) return false;
    if (totalMem != null ? !totalMem.equals(that.totalMem) : that.totalMem != null) return false;
    if (cpuInfo != null ? !cpuInfo.equals(that.cpuInfo) : that.cpuInfo != null) return false;
    if (discoveryStatus != null ? !discoveryStatus.equals(that.discoveryStatus) : that.discoveryStatus != null)
      return false;
    if (disksInfo != null ? !disksInfo.equals(that.disksInfo) : that.disksInfo != null) return false;
    if (hostAttributes != null ? !hostAttributes.equals(that.hostAttributes) : that.hostAttributes != null)
      return false;
    if (hostName != null ? !hostName.equals(that.hostName) : that.hostName != null) return false;
    if (ipv4 != null ? !ipv4.equals(that.ipv4) : that.ipv4 != null) return false;
    if (osArch != null ? !osArch.equals(that.osArch) : that.osArch != null) return false;
    if (osInfo != null ? !osInfo.equals(that.osInfo) : that.osInfo != null) return false;
    if (osType != null ? !osType.equals(that.osType) : that.osType != null) return false;
    if (rackInfo != null ? !rackInfo.equals(that.rackInfo) : that.rackInfo != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = hostName != null ? hostName.hashCode() : 0;
    result = 31 * result + (ipv4 != null ? ipv4.hashCode() : 0);
    result = 31 * result + (totalMem != null ? totalMem.intValue() : 0);
    result = 31 * result + cpuCount;
    result = 31 * result + (cpuInfo != null ? cpuInfo.hashCode() : 0);
    result = 31 * result + (osArch != null ? osArch.hashCode() : 0);
    result = 31 * result + (disksInfo != null ? disksInfo.hashCode() : 0);
    result = 31 * result + (osInfo != null ? osInfo.hashCode() : 0);
    result = 31 * result + (osType != null ? osType.hashCode() : 0);
    result = 31 * result + (discoveryStatus != null ? discoveryStatus.hashCode() : 0);
    result = 31 * result + (lastRegistrationTime != null ? lastRegistrationTime.intValue() : 0);
    result = 31 * result + (rackInfo != null ? rackInfo.hashCode() : 0);
    result = 31 * result + (hostAttributes != null ? hostAttributes.hashCode() : 0);
    return result;
  }

  private Collection<HostComponentDesiredStateEntity> hostComponentDesiredStateEntities;

  @OneToMany(mappedBy = "hostEntity")
  public Collection<HostComponentDesiredStateEntity> getHostComponentDesiredStateEntities() {
    return hostComponentDesiredStateEntities;
  }

  public void setHostComponentDesiredStateEntities(Collection<HostComponentDesiredStateEntity> hostComponentDesiredStateEntities) {
    this.hostComponentDesiredStateEntities = hostComponentDesiredStateEntities;
  }

  private Collection<HostComponentStateEntity> hostComponentStateEntities;

  @OneToMany(mappedBy = "hostEntity")
  public Collection<HostComponentStateEntity> getHostComponentStateEntities() {
    return hostComponentStateEntities;
  }

  public void setHostComponentStateEntities(Collection<HostComponentStateEntity> hostComponentStateEntities) {
    this.hostComponentStateEntities = hostComponentStateEntities;
  }

  private Collection<ClusterEntity> clusterEntities;

  @ManyToMany
//  @JoinColumn(name = "cluster_id", referencedColumnName = "cluster_id")
  @JoinTable(name = "ClusterHostMapping", catalog = "", schema = "ambari",
          joinColumns = {@JoinColumn(name = "host_name", referencedColumnName = "host_name")},
          inverseJoinColumns = {@JoinColumn(name = "cluster_id", referencedColumnName = "cluster_id")}

  )
  public Collection<ClusterEntity> getClusterEntities() {
    return clusterEntities;
  }

  public void setClusterEntities(Collection<ClusterEntity> clusterEntities) {
    this.clusterEntities = clusterEntities;
  }

  private HostStateEntity hostStateEntity;

  @OneToOne(mappedBy = "hostEntity")
  public HostStateEntity getHostStateEntity() {
    return hostStateEntity;
  }

  public void setHostStateEntity(HostStateEntity hostStateEntity) {
    this.hostStateEntity = hostStateEntity;
  }

  private Collection<HostRoleCommandEntity> hostRoleCommandEntities;

  @OneToMany(mappedBy = "host")
  public Collection<HostRoleCommandEntity> getHostRoleCommandEntities() {
    return hostRoleCommandEntities;
  }

  public void setHostRoleCommandEntities(Collection<HostRoleCommandEntity> hostRoleCommandEntities) {
    this.hostRoleCommandEntities = hostRoleCommandEntities;
  }

  //  private Collection<ServiceComponentStateEntity> serviceComponentStateEntities;
//
//  @OneToMany(mappedBy = "hostEntity")
//  public Collection<ServiceComponentStateEntity> getServiceComponentStateEntities() {
//    return serviceComponentStateEntities;
//  }
//
//  public void setServiceComponentStateEntities(Collection<ServiceComponentStateEntity> serviceComponentStateEntities) {
//    this.serviceComponentStateEntities = serviceComponentStateEntities;
//  }

//  private Collection<ServiceStateEntity> serviceStateEntities;
//
//  @OneToMany(mappedBy = "hostEntity")
//  public Collection<ServiceStateEntity> getServiceStateEntities() {
//    return serviceStateEntities;
//  }
//
//  public void setServiceStateEntities(Collection<ServiceStateEntity> serviceStateEntities) {
//    this.serviceStateEntities = serviceStateEntities;
//  }
}
