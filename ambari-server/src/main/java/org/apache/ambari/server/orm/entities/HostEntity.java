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

import static org.apache.commons.lang.StringUtils.defaultString;

@javax.persistence.Table(name = "hosts")
@Entity
public class HostEntity {

  @Id
  @Column(name = "host_name", nullable = false, insertable = true, updatable = true)
  private String hostName;

  @Column(name = "ipv4", nullable = true, insertable = true, updatable = true)
  @Basic
  private String ipv4;

  @Column(name = "ipv6", nullable = true, insertable = true, updatable = true)
  @Basic
  private String ipv6;

  @Column(name="public_host_name", nullable = true, insertable = true, updatable = true)
  @Basic
  private String publicHostName;

  @Column(name = "total_mem", nullable = false, insertable = true, updatable = true)
  @Basic
  private Long totalMem = 0L;

  @Column(name = "cpu_count", nullable = false, insertable = true, updatable = true)
  @Basic
  private Integer cpuCount = 0;

  @Column(name = "ph_cpu_count", nullable = false, insertable = true, updatable = true)
  @Basic
  private Integer phCpuCount = 0;

  @Column(name = "cpu_info", insertable = true, updatable = true)
  @Basic
  private String cpuInfo = "";

  @Column(name = "os_arch", insertable = true, updatable = true)
  @Basic
  private String osArch = "";

  @Column(name = "os_info", insertable = true, updatable = true,
      length = 1000)
  @Basic
  private String osInfo = "";

  @Column(name = "os_type", insertable = true, updatable = true)
  @Basic
  private String osType = "";

  @Column(name = "discovery_status", insertable = true, updatable = true,
      length = 2000)
  @Basic
  private String discoveryStatus = "";

  @Column(name = "last_registration_time", nullable = false, insertable = true, updatable = true)
  @Basic
  private Long lastRegistrationTime = 0L;

  @Column(name = "rack_info", nullable = false, insertable = true, updatable = true)
  @Basic
  private String rackInfo = "/default-rack";

  @Column(name = "host_attributes", insertable = true, updatable = true,
      length = 20000)
  @Basic
  @Lob
  private String hostAttributes = "";

  @OneToMany(mappedBy = "hostEntity", cascade = {CascadeType.REMOVE, CascadeType.PERSIST})
  private Collection<HostComponentDesiredStateEntity> hostComponentDesiredStateEntities;

  @OneToMany(mappedBy = "hostEntity", cascade = {CascadeType.REMOVE, CascadeType.PERSIST})
  private Collection<HostComponentStateEntity> hostComponentStateEntities;

  @ManyToMany
  @JoinTable(name = "ClusterHostMapping",
      joinColumns = {@JoinColumn(name = "host_name", referencedColumnName = "host_name")},
      inverseJoinColumns = {@JoinColumn(name = "cluster_id", referencedColumnName = "cluster_id")}
  )
  private Collection<ClusterEntity> clusterEntities;

  @OneToOne(mappedBy = "hostEntity", cascade = CascadeType.REMOVE)
  private HostStateEntity hostStateEntity;

  @OneToMany(mappedBy = "host", cascade = CascadeType.REMOVE)
  private Collection<HostRoleCommandEntity> hostRoleCommandEntities;
  
  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  public String getIpv4() {
    return ipv4;
  }

  public void setIpv4(String ipv4) {
    this.ipv4 = ipv4;
  }

  public String getIpv6() {
    return ipv6;
  }

  public void setIpv6(String ipv6) {
    this.ipv6 = ipv6;
  }

  public String getPublicHostName() {
    return publicHostName;
  }

  public void setPublicHostName(String name) {
    publicHostName = name;
  }

  public Long getTotalMem() {
    return totalMem;
  }

  public void setTotalMem(Long totalMem) {
    this.totalMem = totalMem;
  }

  public Integer getCpuCount() {
    return cpuCount;
  }

  public void setCpuCount(Integer cpuCount) {
    this.cpuCount = cpuCount;
  }
  
  public Integer getPhCpuCount() {
    return phCpuCount;
  }

  public void setPhCpuCount(Integer phCpuCount) {
    this.phCpuCount = phCpuCount;
  }
  
  public String getCpuInfo() {
    return defaultString(cpuInfo);
  }

  public void setCpuInfo(String cpuInfo) {
    this.cpuInfo = cpuInfo;
  }

  public String getOsArch() {
    return defaultString(osArch);
  }

  public void setOsArch(String osArch) {
    this.osArch = osArch;
  }

  public String getOsInfo() {
    return defaultString(osInfo);
  }

  public void setOsInfo(String osInfo) {
    this.osInfo = osInfo;
  }

  public String getOsType() {
    return defaultString(osType);
  }

  public void setOsType(String osType) {
    this.osType = osType;
  }

  public String getDiscoveryStatus() {
    return defaultString(discoveryStatus);
  }

  public void setDiscoveryStatus(String discoveryStatus) {
    this.discoveryStatus = discoveryStatus;
  }

  public Long getLastRegistrationTime() {
    return lastRegistrationTime;
  }

  public void setLastRegistrationTime(Long lastRegistrationTime) {
    this.lastRegistrationTime = lastRegistrationTime;
  }

  public String getRackInfo() {
    return rackInfo;
  }

  public void setRackInfo(String rackInfo) {
    this.rackInfo = rackInfo;
  }

  public String getHostAttributes() {
    return defaultString(hostAttributes);
  }

  public void setHostAttributes(String hostAttributes) {
    this.hostAttributes = hostAttributes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    HostEntity that = (HostEntity) o;

    if (!hostName.equals(that.hostName)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return hostName.hashCode();
  }

  public Collection<HostComponentDesiredStateEntity> getHostComponentDesiredStateEntities() {
    return hostComponentDesiredStateEntities;
  }

  public void setHostComponentDesiredStateEntities(Collection<HostComponentDesiredStateEntity> hostComponentDesiredStateEntities) {
    this.hostComponentDesiredStateEntities = hostComponentDesiredStateEntities;
  }

  public Collection<HostComponentStateEntity> getHostComponentStateEntities() {
    return hostComponentStateEntities;
  }

  public void setHostComponentStateEntities(Collection<HostComponentStateEntity> hostComponentStateEntities) {
    this.hostComponentStateEntities = hostComponentStateEntities;
  }

  public Collection<ClusterEntity> getClusterEntities() {
    return clusterEntities;
  }

  public void setClusterEntities(Collection<ClusterEntity> clusterEntities) {
    this.clusterEntities = clusterEntities;
  }

  public HostStateEntity getHostStateEntity() {
    return hostStateEntity;
  }

  public void setHostStateEntity(HostStateEntity hostStateEntity) {
    this.hostStateEntity = hostStateEntity;
  }

  public Collection<HostRoleCommandEntity> getHostRoleCommandEntities() {
    return hostRoleCommandEntities;
  }

  public void setHostRoleCommandEntities(Collection<HostRoleCommandEntity> hostRoleCommandEntities) {
    this.hostRoleCommandEntities = hostRoleCommandEntities;
  }

}
