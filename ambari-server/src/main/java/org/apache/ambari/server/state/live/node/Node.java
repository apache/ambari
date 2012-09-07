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

package org.apache.ambari.server.state.live.node;

import java.util.List;
import java.util.Map;

import org.apache.ambari.server.state.fsm.InvalidStateTransitonException;
import org.apache.ambari.server.state.live.AgentVersion;
import org.apache.ambari.server.state.live.DiskInfo;
import org.apache.ambari.server.state.live.Job;

public interface Node {

  /**
   * @return the hostName
   */
  public String getHostName();

  /**
   * @param hostName the hostName to set
   */
  public void setHostName(String hostName);

  /**
   * IPv4 assigned to the Node
   * @return the ip or null if no IPv4 interface
   */
  public String getIPv4();

  /**
   * @param ip the ip to set
   */
  public void setIPv4(String ip);

  /**
   * IPv6 assigned to the Node
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
   * Get the Amount of physical memory for the Node.
   * @return the totalMemBytes
   */
  public long getTotalMemBytes();

  /**
   * Set the Amount of physical memory for the Node.
   * @param totalMemBytes the totalMemBytes to set
   */
  public void setTotalMemBytes(long totalMemBytes);

  /**
   * Get the Amount of available memory for the Node.
   * In most cases, available should be same as total unless
   * the agent on the node is configured to not use all
   * available memory
   * @return the availableMemBytes
   */
  public long getAvailableMemBytes();

  /**
   * Set the Amount of available memory for the Node.
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
   * @param osType the osType to set
   */
  public void setOsType(String osType);

  /**
   * Get information on disks available on the node.
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
  public NodeHealthStatus getHealthStatus();

  /**
   * @param healthStatus the healthStatus to set
   */
  public void setHealthStatus(NodeHealthStatus healthStatus);

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
   * Last time the node registered with the Ambari Server
   * ( Unix timestamp )
   * @return the lastRegistrationTime
   */
  public long getLastRegistrationTime();

  /**
   * @param lastRegistrationTime the lastRegistrationTime to set
   */
  public void setLastRegistrationTime(long lastRegistrationTime);

  /**
   * Last time the Ambari Server received a heartbeat from the Node
   * ( Unix timestamp )
   * @return the lastHeartbeatTime
   */
  public long getLastHeartbeatTime();

  /**
   * @param lastHeartbeatTime the lastHeartbeatTime to set
   */
  public void setLastHeartbeatTime(long lastHeartbeatTime);

  /**
   * Version of the Ambari Agent running on the node
   * @return the agentVersion
   */
  public AgentVersion getAgentVersion();

  /**
   * @param agentVersion the agentVersion to set
   */
  public void setAgentVersion(AgentVersion agentVersion);

  /**
   * Get the list of Jobs that are currently being tracked at the
   * Node level
   * @return List of Jobs
   */
  public List<Job> getJobs();

  /**
   * Get Current Node State
   * @return NodeState
   */
  public NodeState getState();

  /**
   * Set the State of the Node
   * @param state Node State
   */
  public void setState(NodeState state);

  /**
   * Send an event to the Node's StateMachine
   * @param event NodeEvent
   * @throws InvalidStateTransitonException
   */
  public void handleEvent(NodeEvent event)
      throws InvalidStateTransitonException;
}
