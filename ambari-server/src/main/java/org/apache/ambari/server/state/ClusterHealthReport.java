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

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Cluster Health Report (part of Clusters API response)
 */
public class ClusterHealthReport {

  private int staleConfigsHosts;
  private int maintenanceStateHosts;

  private int healthyStateHosts;
  private int unhealthyStateHosts;
  private int heartbeatLostStateHosts;
  private int initStateHosts;

  private int healthyStatusHosts;
  private int unhealthyStatusHosts;
  private int unknownStatusHosts;
  private int alertStatusHosts;

  /**
   * @return number of hosts having stale_config set to true
   */
  @JsonProperty("Host/stale_config")
  public int getStaleConfigsHosts() {
    return staleConfigsHosts;
  }

  /**
   * @param staleConfigsHosts number of hosts having stale_config set to true
   */
  public void setStaleConfigsHosts(int staleConfigsHosts) {
    this.staleConfigsHosts = staleConfigsHosts;
  }

  /**
   * @return number of hosts having maintenance state on
   */
  @JsonProperty("Host/maintenance_state")
  public int getMaintenanceStateHosts() {
    return maintenanceStateHosts;
  }

  /**
   * @param maintenanceStateHosts number of hosts having maintenance state on
   */
  public void setMaintenanceStateHosts(int maintenanceStateHosts) {
    this.maintenanceStateHosts = maintenanceStateHosts;
  }

  /**
   * @return number of hosts having host state HEALTHY
   */
  @JsonProperty("Host/host_state/HEALTHY")
  public int getHealthyStateHosts() {
    return healthyStateHosts;
  }

  /**
   * @param healthyStateHosts number of hosts having host state HEALTHY
   */
  public void setHealthyStateHosts(int healthyStateHosts) {
    this.healthyStateHosts = healthyStateHosts;
  }

  /**
   * @return number of hosts having host state UNHEALTHY
   */
  @JsonProperty("Host/host_state/UNHEALTHY")
  public int getUnhealthyStateHosts() {
    return unhealthyStateHosts;
  }

  /**
   * @param unhealthyStateHosts number of hosts having host state UNHEALTHY
   */
  public void setUnhealthyStateHosts(int unhealthyStateHosts) {
    this.unhealthyStateHosts = unhealthyStateHosts;
  }

  /**
   * @return number of hosts having host state INIT
   */
  @JsonProperty("Host/host_state/INIT")
  public int getInitStateHosts() {
    return initStateHosts;
  }

  /**
   * @param initStateHosts number of hosts having host state INIT
   */
  public void setInitStateHosts(int initStateHosts) {
    this.initStateHosts = initStateHosts;
  }

  /**
   * @return number of hosts having host status HEALTHY
   */
  @JsonProperty("Host/host_status/HEALTHY")
  public int getHealthyStatusHosts() {
    return healthyStatusHosts;
  }

  /**
   * @param healthyStatusHosts number of hosts having host status HEALTHY
   */
  public void setHealthyStatusHosts(int healthyStatusHosts) {
    this.healthyStatusHosts = healthyStatusHosts;
  }

  /**
   * @return number of hosts having host status UNHEALTHY
   */
  @JsonProperty("Host/host_status/UNHEALTHY")
  public int getUnhealthyStatusHosts() {
    return unhealthyStatusHosts;
  }

  /**
   * @param unhealthyStatusHosts number of hosts having host status UNHEALTHY
   */
  public void setUnhealthyStatusHosts(int unhealthyStatusHosts) {
    this.unhealthyStatusHosts = unhealthyStatusHosts;
  }

  /**
   * @return number of hosts having host status UNKNOWN
   */
  @JsonProperty("Host/host_status/UNKNOWN")
  public int getUnknownStatusHosts() {
    return unknownStatusHosts;
  }

  /**
   * @param unknownStatusHosts number of hosts having host status UNKNOWN
   */
  public void setUnknownStatusHosts(int unknownStatusHosts) {
    this.unknownStatusHosts = unknownStatusHosts;
  }

  /**
   * @return number of hosts having host status ALERT
   */
  @JsonProperty("Host/host_status/ALERT")
  public int getAlertStatusHosts() {
    return alertStatusHosts;
  }

  /**
   * @param alertStatusHosts number of hosts having host status ALERT
   */
  public void setAlertStatusHosts(int alertStatusHosts) {
    this.alertStatusHosts = alertStatusHosts;
  }

  /**
   * @return number of hosts having host status HEARTBEAT_LOST
   */
  @JsonProperty("Host/host_state/HEARTBEAT_LOST")
  public int getHeartbeatLostStateHosts() {
    return heartbeatLostStateHosts;
  }

  /**
   * @param heartbeatLostStateHosts number of hosts
   *                                having host status HEARTBEAT_LOST
   */
  public void setHeartbeatLostStateHosts(int heartbeatLostStateHosts) {
    this.heartbeatLostStateHosts = heartbeatLostStateHosts;
  }

  public ClusterHealthReport() {

  }

  @Override
  public String toString() {
    return "ClusterHealthReport{" +
      "staleConfigsHosts=" + staleConfigsHosts +
      ", maintenanceStateHosts=" + maintenanceStateHosts +
      ", healthyStateHosts=" + healthyStateHosts +
      ", unhealthyStateHosts=" + unhealthyStateHosts +
      ", heartbeatLostStateHosts=" + heartbeatLostStateHosts +
      ", initStateHosts=" + initStateHosts +
      ", healthyStatusHosts=" + healthyStatusHosts +
      ", unhealthyStatusHosts=" + unhealthyStatusHosts +
      ", unknownStatusHosts=" + unknownStatusHosts +
      ", alertStatusHosts=" + alertStatusHosts +
      '}';
  }
}





