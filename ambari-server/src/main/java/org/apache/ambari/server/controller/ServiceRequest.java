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



public class ServiceRequest {

  private String clusterName; // REF
  private String serviceName; // GET/CREATE/UPDATE/DELETE
  private String desiredState; // CREATE/UPDATE
  private String maintenanceState; // UPDATE
  private String credentialStoreEnabled; // CREATE/UPDATE/GET

  public ServiceRequest(String clusterName, String serviceName,
                        String desiredState) {
    this(clusterName, serviceName, desiredState, null);
  }

  public ServiceRequest(String clusterName, String serviceName,
                        String desiredState,
                        String credentialStoreEnabled) {
    this.clusterName = clusterName;
    this.serviceName = serviceName;
    this.desiredState = desiredState;
    this.credentialStoreEnabled = credentialStoreEnabled;
    // Credential store supported cannot be changed after
    // creation since it comes from the stack definition.
    // We can update credential store enabled alone.
  }

  /**
   * @return the serviceName
   */
  public String getServiceName() {
    return serviceName;
  }

  /**
   * @param serviceName the serviceName to set
   */
  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  /**
   * @return the desiredState
   */
  public String getDesiredState() {
    return desiredState;
  }

  /**
   * @param desiredState the desiredState to set
   */
  public void setDesiredState(String desiredState) {
    this.desiredState = desiredState;
  }

  /**
   * @return the clusterName
   */
  public String getClusterName() {
    return clusterName;
  }

  /**
   * @param clusterName the clusterName to set
   */
  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  /**
   * @param state the new maintenance state
   */
  public void setMaintenanceState(String state) {
    maintenanceState = state;
  }

  /**
   * @return the maintenance state
   */
  public String getMaintenanceState() {
    return maintenanceState;
  }

  /**
   * @return credential store enabled
   */
  public String getCredentialStoreEnabled() {
    return credentialStoreEnabled;
  }

  /**
   * @param credentialStoreEnabled the new credential store enabled
   */
  public void setCredentialStoreEnabled(String credentialStoreEnabled) {
    this.credentialStoreEnabled = credentialStoreEnabled;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("clusterName=" + clusterName
        + ", serviceName=" + serviceName
        + ", desiredState=" + desiredState
        + ", credentialStoreEnabled=" + credentialStoreEnabled);
    return sb.toString();
  }
}
