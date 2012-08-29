package org.apache.ambari.server;

import java.util.List;
import java.util.Map;

// TODO
// Just a placeholder for now
// Should be replaced by ORM layer
public class NodeImpl implements Node {

  /**
   * Hostname
   */
  String hostName;

  /**
   * IP of the given node
   */
  // TODO change type
  String ipv4;

  /**
   * IP of the given node
   */
  // TODO change type
  String ipv6;

  /**
   * Count of cores
   */
  int cpuCount;

  /**
   * Amount of total physical memory for the Node
   */
  int totalMemBytes;

  /**
   * Amount of available memory for the Node.
   * In most cases, available should be same as total unless
   * the agent on the node is configured to not use all
   * available memory
   */
  int availableMemBytes;

  /**
   * OS Architecture.
   * i386, x86_64, etc.
   */
  // TODO should we make this an enum?
  String osArch;

  /**
   * General OS information.
   * uname -a, /etc/*-release dump
   */
  String osInfo;

  /**
   * OS Type: RHEL5/RHEL6/CentOS5/...
   * Defined and match-able OS type
   */
  // TODO should this be an enum?
  String osType;

  /*
   // TODO Add later if needed
   * Additional CPU information
   * CPU coumt, clock speed, etc
  CpuInfo cpuInfo;
  */

  /**
   * Information on disks available on the node
   */
  List<DiskInfo> disksInfo;

  /**
   * Node Health Status
   */
  NodeHealthStatus healthStatus;

  /**
   * Additional host attributes to capture misc. information
   * For example, public/hostname/IP for AWS
   */
  Map<String, String> hostAttributes;

  /**
   * Rack information for topology-awareness
   */
  String rackInfo;

  /**
   * Last time the node registered with the Ambari Server
   * ( Unix timestamp )
   */
  int lastRegistrationTime;

  /**
   * Last time the server received a heartbeat from the node
   * ( Unix timestamp )
   */
  int lastHeartbeatTime;

  /**
   * Version of the Agent running on the node
   */
  AgentVersion agentVersion;

  /**
   * @return the hostName
   */
  @Override
  public String getHostName() {
    return hostName;
  }

  /**
   * @param hostName the hostName to set
   */
  @Override
  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  @Override
  public String getIPv4() {
    return ipv4;
  }

  @Override
  public void setIPv4(String ipv4) {
    this.ipv4 = ipv4;
  }

  @Override
  public String getIPv6() {
    return ipv6;
  }

  @Override
  public void setIPv6(String ipv6) {
    this.ipv6 = ipv6;
  }

  /**
   * @return the cpuCount
   */
  @Override
  public int getCpuCount() {
    return cpuCount;
  }

  /**
   * @param cpuCount the cpuCount to set
   */
  @Override
  public void setCpuCount(int cpuCount) {
    this.cpuCount = cpuCount;
  }

  /**
   * @return the totalMemBytes
   */
  @Override
  public int getTotalMemBytes() {
    return totalMemBytes;
  }

  /**
   * @param totalMemBytes the totalMemBytes to set
   */
  @Override
  public void setTotalMemBytes(int totalMemBytes) {
    this.totalMemBytes = totalMemBytes;
  }

  /**
   * @return the availableMemBytes
   */
  @Override
  public int getAvailableMemBytes() {
    return availableMemBytes;
  }

  /**
   * @param availableMemBytes the availableMemBytes to set
   */
  @Override
  public void setAvailableMemBytes(int availableMemBytes) {
    this.availableMemBytes = availableMemBytes;
  }

  /**
   * @return the osArch
   */
  @Override
  public String getOsArch() {
    return osArch;
  }

  /**
   * @param osArch the osArch to set
   */
  @Override
  public void setOsArch(String osArch) {
    this.osArch = osArch;
  }

  /**
   * @return the osInfo
   */
  @Override
  public String getOsInfo() {
    return osInfo;
  }

  /**
   * @param osInfo the osInfo to set
   */
  @Override
  public void setOsInfo(String osInfo) {
    this.osInfo = osInfo;
  }

  /**
   * @return the osType
   */
  @Override
  public String getOsType() {
    return osType;
  }

  /**
   * @param osType the osType to set
   */
  @Override
  public void setOsType(String osType) {
    this.osType = osType;
  }

  /**
   * @return the disksInfo
   */
  @Override
  public List<DiskInfo> getDisksInfo() {
    return disksInfo;
  }

  /**
   * @param disksInfo the disksInfo to set
   */
  @Override
  public void setDisksInfo(List<DiskInfo> disksInfo) {
    this.disksInfo = disksInfo;
  }

  /**
   * @return the healthStatus
   */
  @Override
  public NodeHealthStatus getHealthStatus() {
    return healthStatus;
  }

  /**
   * @param healthStatus the healthStatus to set
   */
  @Override
  public void setHealthStatus(NodeHealthStatus healthStatus) {
    this.healthStatus = healthStatus;
  }

  /**
   * @return the hostAttributes
   */
  @Override
  public Map<String, String> getHostAttributes() {
    return hostAttributes;
  }

  /**
   * @param hostAttributes the hostAttributes to set
   */
  @Override
  public void setHostAttributes(Map<String, String> hostAttributes) {
    this.hostAttributes = hostAttributes;
  }

  /**
   * @return the rackInfo
   */
  @Override
  public String getRackInfo() {
    return rackInfo;
  }

  /**
   * @param rackInfo the rackInfo to set
   */
  @Override
  public void setRackInfo(String rackInfo) {
    this.rackInfo = rackInfo;
  }

  /**
   * @return the lastRegistrationTime
   */
  @Override
  public int getLastRegistrationTime() {
    return lastRegistrationTime;
  }

  /**
   * @param lastRegistrationTime the lastRegistrationTime to set
   */
  @Override
  public void setLastRegistrationTime(int lastRegistrationTime) {
    this.lastRegistrationTime = lastRegistrationTime;
  }

  /**
   * @return the lastHeartbeatTime
   */
  @Override
  public int getLastHeartbeatTime() {
    return lastHeartbeatTime;
  }

  /**
   * @param lastHeartbeatTime the lastHeartbeatTime to set
   */
  @Override
  public void setLastHeartbeatTime(int lastHeartbeatTime) {
    this.lastHeartbeatTime = lastHeartbeatTime;
  }

  /**
   * @return the agentVersion
   */
  @Override
  public AgentVersion getAgentVersion() {
    return agentVersion;
  }

  /**
   * @param agentVersion the agentVersion to set
   */
  @Override
  public void setAgentVersion(AgentVersion agentVersion) {
    this.agentVersion = agentVersion;
  }

  @Override
  public NodeState getNodeState() {
    // TODO Auto-generated method stub
    return null;
  }

}
