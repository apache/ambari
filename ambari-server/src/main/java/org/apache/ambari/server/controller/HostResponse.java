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

package org.apache.ambari.server.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.agent.AgentEnv;
import org.apache.ambari.server.agent.DiskInfo;
import org.apache.ambari.server.agent.RecoveryReport;
import org.apache.ambari.server.state.AgentVersion;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.HostConfig;
import org.apache.ambari.server.state.HostHealthStatus;
import org.apache.ambari.server.state.MaintenanceState;

public class HostResponse {

  private String hostname;

  private String clusterName;

  /**
   * Host IP if ipv4 interface available
   */
  private String ipv4;

  /**
   * Host IP if ipv6 interface available
   */
  private String ipv6;

  /**
   * Count of cores on Host
   */
  private int cpuCount;
  
  /**
   * Count of physical cores on Host
   */
  private int phCpuCount;
  
  
  /**
   * Os Architecture
   */
  private String osArch;

  /**
   * OS Type
   */
  private String osType;

  /**
   * OS Information
   */
  private String osInfo;

  /**
   * Amount of available memory for the Host
   */
  private long availableMemBytes;

  /**
   * Amount of physical memory for the Host
   */
  private long totalMemBytes;

  /**
   * Disks mounted on the Host
   */
  private List<DiskInfo> disksInfo;

  /**
   * Last heartbeat timestamp from the Host
   */
  private long lastHeartbeatTime;
  
  /**
   * Last environment information
   */
  private AgentEnv lastAgentEnv;

  /**
   * Last registration timestamp for the Host
   */
  private long lastRegistrationTime;

  /**
   * Rack to which the Host belongs to
   */
  private String rackInfo;

  /**
   * Additional Host attributes
   */
  private Map<String, String> hostAttributes;

  /**
   * Version of agent running on the Host
   */
  private AgentVersion agentVersion;

  /**
   * Host Health Status
   */
  private HostHealthStatus healthStatus;

  /**
   * Recovery status
   */
  private RecoveryReport recoveryReport;

  /**
   * Summary of node recovery
   */
  private String recoverySummary = "DISABLED";
  
  /**
   * Public name.
   */
  private String publicHostname = null;

  /**
   * Host State
   */
  private String hostState;

  private Map<String, DesiredConfig> desiredConfigs;

  /**
   * Configs derived from Config groups
   */
  private Map<String, HostConfig> desiredHostConfigs;

  /**
   * Host status, calculated on host components statuses
   */
  private String status;

  private MaintenanceState maintenanceState = null;

  public HostResponse(String hostname, String clusterName,
                      String ipv4, String ipv6, int cpuCount, int phCpuCount, String osArch, String osType,
                      String osInfo, long availableMemBytes, long totalMemBytes,
                      List<DiskInfo> disksInfo, long lastHeartbeatTime,
                      long lastRegistrationTime, String rackInfo,
                      Map<String, String> hostAttributes, AgentVersion agentVersion,
                      HostHealthStatus healthStatus, String hostState, String status) {
    this.hostname = hostname;
    this.clusterName = clusterName;
    this.ipv4 = ipv4;
    this.ipv6 = ipv6;
    this.cpuCount = cpuCount;
    this.phCpuCount = phCpuCount;
    this.osArch = osArch;
    this.osType = osType;
    this.osInfo = osInfo;
    this.availableMemBytes = availableMemBytes;
    this.totalMemBytes = totalMemBytes;
    this.disksInfo = disksInfo;
    this.lastHeartbeatTime = lastHeartbeatTime;
    this.lastRegistrationTime = lastRegistrationTime;
    this.rackInfo = rackInfo;
    this.hostAttributes = hostAttributes;
    this.agentVersion = agentVersion;
    this.healthStatus = healthStatus;
    this.setHostState(hostState);
    this.status = status;
  }

  //todo: why are we passing in empty strings for host/cluster name instead of null?
  public HostResponse(String hostname) {
    this(hostname, "", "", "",
        0, 0, "", "",
        "", 0, 0, new ArrayList<DiskInfo>(),
        0, 0, "",
        new HashMap<String, String>(),
        null, null, null, null);
  }

  /**
   * @return the hostname
   */
  public String getHostname() {
    return hostname;
  }

  /**
   * @param hostname the hostname to set
   */
  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  /**
   * @return the clusterNames
   */
  public String getClusterName() {
    return clusterName;
  }

  /**
   * @param clusterName the name of the associated cluster
   */
  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  /**
   * @return the ipv4
   */
  public String getIpv4() {
    return ipv4;
  }

  /**
   * @param ipv4 the ipv4 to set
   */
  public void setIpv4(String ipv4) {
    this.ipv4 = ipv4;
  }

  /**
   * @return the ipv6
   */
  public String getIpv6() {
    return ipv6;
  }

  /**
   * @param ipv6 the ipv6 to set
   */
  public void setIpv6(String ipv6) {
    this.ipv6 = ipv6;
  }

  /**
   * @return the cpuCount
   */
  public int getCpuCount() {
    return cpuCount;
  }

  /**
   * @param cpuCount the cpuCount to set
   */
  public void setCpuCount(int cpuCount) {
    this.cpuCount = cpuCount;
  }

  /**
  * @return the phCpuCount
  */
  public int getPhCpuCount() {
    return phCpuCount;
  }

  /**
  * @param phCpuCount the physical cpu count to set
  */
  public void setPhCpuCount(int phCpuCount) {
    this.phCpuCount = phCpuCount;
  }

  
  
  /**
   * @return the osArch
   */
  public String getOsArch() {
    return osArch;
  }

  /**
   * @param osArch the osArch to set
   */
  public void setOsArch(String osArch) {
    this.osArch = osArch;
  }

  /**
   * @return the osType
   */
  public String getOsType() {
    return osType;
  }

  /**
   * @param osType the osType to set
   */
  public void setOsType(String osType) {
    this.osType = osType;
  }

  /**
   * @return the osInfo
   */
  public String getOsInfo() {
    return osInfo;
  }

  /**
   * @param osInfo the osInfo to set
   */
  public void setOsInfo(String osInfo) {
    this.osInfo = osInfo;
  }

  /**
   * @return the availableMemBytes
   */
  public long getAvailableMemBytes() {
    return availableMemBytes;
  }

  /**
   * @param availableMemBytes the availableMemBytes to set
   */
  public void setAvailableMemBytes(long availableMemBytes) {
    this.availableMemBytes = availableMemBytes;
  }

  /**
   * @return the totalMemBytes
   */
  public long getTotalMemBytes() {
    return totalMemBytes;
  }

  /**
   * @param totalMemBytes the totalMemBytes to set
   */
  public void setTotalMemBytes(long totalMemBytes) {
    this.totalMemBytes = totalMemBytes;
  }

  /**
   * @return the disksInfo
   */
  public List<DiskInfo> getDisksInfo() {
    return disksInfo;
  }

  /**
   * @param disksInfo the disksInfo to set
   */
  public void setDisksInfo(List<DiskInfo> disksInfo) {
    this.disksInfo = disksInfo;
  }

  /**
   * @return the lastHeartbeatTime
   */
  public long getLastHeartbeatTime() {
    return lastHeartbeatTime;
  }

  /**
   * @param lastHeartbeatTime the lastHeartbeatTime to set
   */
  public void setLastHeartbeatTime(long lastHeartbeatTime) {
    this.lastHeartbeatTime = lastHeartbeatTime;
  }

  /**
   * @return the lastRegistrationTime
   */
  public long getLastRegistrationTime() {
    return lastRegistrationTime;
  }

  /**
   * @param lastRegistrationTime the lastRegistrationTime to set
   */
  public void setLastRegistrationTime(long lastRegistrationTime) {
    this.lastRegistrationTime = lastRegistrationTime;
  }

  /**
   * @return the rackInfo
   */
  public String getRackInfo() {
    return rackInfo;
  }

  /**
   * @param rackInfo the rackInfo to set
   */
  public void setRackInfo(String rackInfo) {
    this.rackInfo = rackInfo;
  }

  /**
   * @return the hostAttributes
   */
  public Map<String, String> getHostAttributes() {
    return hostAttributes;
  }

  /**
   * @param hostAttributes the hostAttributes to set
   */
  public void setHostAttributes(Map<String, String> hostAttributes) {
    this.hostAttributes = hostAttributes;
  }

  /**
   * @return the agentVersion
   */
  public AgentVersion getAgentVersion() {
    return agentVersion;
  }

  /**
   * @param agentVersion the agentVersion to set
   */
  public void setAgentVersion(AgentVersion agentVersion) {
    this.agentVersion = agentVersion;
  }

  /**
   * @return the healthStatus
   */
  public HostHealthStatus getHealthStatus() {
    return healthStatus;
  }

  /**
   * @param healthStatus the healthStatus to set
   */
  public void setHealthStatus(HostHealthStatus healthStatus) {
    this.healthStatus = healthStatus;
  }



  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    HostResponse that = (HostResponse) o;

    if (hostname != null ?
        !hostname.equals(that.hostname) : that.hostname != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = hostname != null ? hostname.hashCode() : 0;
    return result;
  }

  public String getPublicHostName() {
    return publicHostname;
  }
  
  public void setPublicHostName(String name) {
    publicHostname = name;
  }

  /**
   * @return the hostState
   */
  public String getHostState() {
    return hostState;
  }

  /**
   * @param hostState the hostState to set
   */
  public void setHostState(String hostState) {
    this.hostState = hostState;
  }

  
  public AgentEnv getLastAgentEnv() {
    return lastAgentEnv;
  }
  
  /**
   * @param agentEnv
   */
  public void setLastAgentEnv(AgentEnv agentEnv) {
    lastAgentEnv = agentEnv;
  }
  
  /**
   * @param desired
   */
  public void setDesiredConfigs(Map<String, DesiredConfig> desired) {
    desiredConfigs = desired;
  }
  
  public Map<String, DesiredConfig> getDesiredConfigs() {
    return desiredConfigs;
  }

  public Map<String, HostConfig> getDesiredHostConfigs() {
    return desiredHostConfigs;
  }

  public void setDesiredHostConfigs(Map<String, HostConfig> desiredHostConfigs) {
    this.desiredHostConfigs = desiredHostConfigs;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  /**
   * @param state the maintenance state
   */
  public void setMaintenanceState(MaintenanceState state) {
    maintenanceState = state;
  }
  
  /**
   * @return the maintenance state
   */
  public MaintenanceState getMaintenanceState() {
    return maintenanceState;
  }

  /**
   * Get the recovery summary for the host
   * @return
   */
  public String getRecoverySummary() {
    return recoverySummary;
  }

  /**
   * Set the recovery summary for the host
   * @return
   */
  public void setRecoverySummary(String recoverySummary) {
    this.recoverySummary = recoverySummary;
  }

  /**
   * Get the detailed recovery report
   * @return
   */
  public RecoveryReport getRecoveryReport() {
    return recoveryReport;
  }

  /**
   * Set the detailed recovery report
   * @param recoveryReport
   */
  public void setRecoveryReport(RecoveryReport recoveryReport) {
    this.recoveryReport = recoveryReport;
  }
}
