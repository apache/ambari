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

package org.apache.ambari.server.state;

import java.util.List;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.AgentEnv;
import org.apache.ambari.server.agent.DiskInfo;
import org.apache.ambari.server.agent.HostInfo;
import org.apache.ambari.server.controller.HostResponse;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;

public interface Host {

  /**
   * @return the hostName
   */
  public String getHostName();

  /**
   * @param hostName the hostName to set
   */
  public void setHostName(String hostName);

  /**
   * @return the currentPingPort
   */
  public Integer getCurrentPingPort();

  /**
   * @param currentPingPort the currentPingPort to set
   */
  public void setCurrentPingPort(Integer currentPingPort);

  /**
   * Gets the public-facing host name.
   */
  public void setPublicHostName(String hostName);
  
  /**
   * Sets the public-facing host name.
   */
  public String getPublicHostName();
  
  /**
   * IPv4 assigned to the Host
   * @return the ip or null if no IPv4 interface
   */
  public String getIPv4();

  /**
   * @param ip the ip to set
   */
  public void setIPv4(String ip);

  /**
   * IPv6 assigned to the Host
   * @return the ip or null if no IPv6 interface
   */
  public String getIPv6();

  /**
   * @param ip the ip to set
   */
  public void setIPv6(String ip);

  /**
   * @return the cpuCount
   */
  public int getCpuCount();

  /**
   * @param cpuCount the cpuCount to set
   */
  public void setCpuCount(int cpuCount);

  /**
   * @return the physical cpu cores
   */
  public int getPhCpuCount();

  /**
   * @param phCpuCount the physical cpu cores to set
   */
  public void setPhCpuCount(int phCpuCount);
  
  /**
   * Get the Amount of physical memory for the Host.
   * @return the totalMemBytes
   */
  public long getTotalMemBytes();

  /**
   * Set the Amount of physical memory for the Host.
   * @param totalMemBytes the totalMemBytes to set
   */
  public void setTotalMemBytes(long totalMemBytes);

  /**
   * Get the Amount of available memory for the Host.
   * In most cases, available should be same as total unless
   * the agent on the host is configured to not use all
   * available memory
   * @return the availableMemBytes
   */
  public long getAvailableMemBytes();

  /**
   * Set the Amount of available memory for the Host.
   * @param availableMemBytes the availableMemBytes to set
   */
  public void setAvailableMemBytes(long availableMemBytes);

  /**
   * Get the OS Architecture.
   * i386, x86_64, etc.
   * @return the osArch
   */
  public String getOsArch();

  /**
   * @param osArch the osArch to set
   */
  public void setOsArch(String osArch);

  /**
   * Get the General OS information.
   * uname -a, /etc/*-release dump
   * @return the osInfo
   */
  public String getOsInfo();

  /**
   * @param osInfo the osInfo to set
   */
  public void setOsInfo(String osInfo);

  /**
   * Get the OS Type: RHEL5/RHEL6/CentOS5/...
   * Defined and match-able OS type
   * @return the osType
   */
  public String getOsType();
  
  /**
   * Get the os Family: 
   * redhat5: for centos5, rhel5, oraclelinux5 ..
   * redhat6: for centos6, rhel6, oraclelinux6 ..
   * ubuntu12 : for ubuntu12
   * suse11: for sles11, suse11 ..
   * 
   * @return the osFamily
   */
  public String getOsFamily();

  /**
   * @param osType the osType to set
   */
  public void setOsType(String osType);

  /**
   * Get information on disks available on the host.
   * @return the disksInfo
   */
  public List<DiskInfo> getDisksInfo();

  /**
   * @param disksInfo the disksInfo to set
   */
  public void setDisksInfo(List<DiskInfo> disksInfo);

  /**
   * @return the healthStatus
   */
  public HostHealthStatus getHealthStatus();

  /**
   * @param healthStatus the healthStatus to set
   */
  public void setHealthStatus(HostHealthStatus healthStatus);

  /**
   * Get additional host attributes
   * For example, public/hostname/IP for AWS
   * @return the hostAttributes
   */
  public Map<String, String> getHostAttributes();

  /**
   * @param hostAttributes the hostAttributes to set
   */
  public void setHostAttributes(Map<String, String> hostAttributes);
  /**
   * @return the rackInfo
   */
  public String getRackInfo();

  /**
   * @param rackInfo the rackInfo to set
   */
  public void setRackInfo(String rackInfo);

  /**
   * Last time the host registered with the Ambari Server
   * ( Unix timestamp )
   * @return the lastRegistrationTime
   */
  public long getLastRegistrationTime();

  /**
   * @param lastRegistrationTime the lastRegistrationTime to set
   */
  public void setLastRegistrationTime(long lastRegistrationTime);

  /**
   * Last time the Ambari Server received a heartbeat from the Host
   * ( Unix timestamp )
   * @return the lastHeartbeatTime
   */
  public long getLastHeartbeatTime();

  /**
   * @param lastHeartbeatTime the lastHeartbeatTime to set
   */
  public void setLastHeartbeatTime(long lastHeartbeatTime);

  /**
   * Sets the latest agent environment that arrived in a heartbeat.
   */
  public void setLastAgentEnv(AgentEnv env);
  
  /**
   * Gets the latest agent environment that arrived in a heartbeat.
   */
  public AgentEnv getLastAgentEnv();
  
  /**
   * Version of the Ambari Agent running on the host
   * @return the agentVersion
   */
  public AgentVersion getAgentVersion();

  /**
   * @param agentVersion the agentVersion to set
   */
  public void setAgentVersion(AgentVersion agentVersion);

  /**
   * Get Current Host State
   * @return HostState
   */
  public HostState getState();

  /**
   * Set the State of the Host
   * @param state Host State
   */
  public void setState(HostState state);

  /**
   * Get the prefix path of all logs
   * @return prefix
   */
  public String getPrefix();

  /**
   * Set the prefix path of all logs of the host
   * @param prefix the prefix path to set
   */
  public void setPrefix(String prefix);

  /**
   * Send an event to the Host's StateMachine
   * @param event HostEvent
   * @throws InvalidStateTransitionException
   */
  public void handleEvent(HostEvent event)
      throws InvalidStateTransitionException;

  /**
   * Get time spent in the current state i.e. the time since last state change.
   * @return Time spent in current state.
   */
  public long getTimeInState();

  /**
   * @param timeInState the timeInState to set
   */
  public void setTimeInState(long timeInState);
  
  /**
   * Get Current Host Status
   * @return String
   */
  public String getStatus();
  /**
   * Set the Status of the Host
   * @param status Host Status
   */
  public void setStatus(String status);

  public HostResponse convertToResponse();

  boolean isPersisted();

  void persist();

  void refresh();

  void importHostInfo(HostInfo hostInfo);

  /**
   * Adds a desired configuration to the host instance.
   * @param clusterId the cluster id that the config applies to
   * @param selected <code>true</code> if the configuration is selected.  Applies
   *    only to remove the override, otherwise this value should always be <code>true</code>.
   * @param user the user making the change for audit purposes
   * @param config the configuration object
   * @return <code>true</code> if the config was added, or <code>false</code>
   * if the config is already set as the current
   */
  public boolean addDesiredConfig(long clusterId, boolean selected, String user, Config config);
  
  /**
   * Gets all the selected configurations for the host.
   * return a map of type-to-{@link DesiredConfig} instances.
   */
  public Map<String, DesiredConfig> getDesiredConfigs(long clusterId);

  /**
   * Get the desired configurations for the host including overrides
   * @param cluster
   * @return
   * @throws AmbariException
   */
  public Map<String, HostConfig> getDesiredHostConfigs(Cluster cluster) throws AmbariException;
  
  /**
   * Sets the maintenance state for the host.
   * @param clusterId the cluster id
   * @param state the state
   */
  public void setMaintenanceState(long clusterId, MaintenanceState state);
  
  /**
   * @param clusterId the cluster id
   * @return the maintenance state
   */
  public MaintenanceState getMaintenanceState(long clusterId);
}
