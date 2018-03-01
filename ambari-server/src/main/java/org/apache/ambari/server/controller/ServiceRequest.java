/*
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

import java.util.Objects;

import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.StackId;

import io.swagger.annotations.ApiModelProperty;

public class ServiceRequest {

  private String clusterName; // REF
  private String serviceName; // GET/CREATE/DELETE
  private String serviceType; // GET/CREATE/UPDATE/DELETE
  private String serviceGroupName;
  private String desiredState; // CREATE/UPDATE
  private String maintenanceState; // UPDATE
  private String credentialStoreEnabled; // CREATE/UPDATE/GET
  private String credentialStoreSupported; //GET

  private StackId desiredStackId;

  private Long desiredRepositoryVersionId;
  /**
   * Short-lived object that gets set while validating a request
   */
  private RepositoryVersionEntity resolvedRepository;

  public ServiceRequest(String clusterName, String serviceGroupName, String serviceName,
      Long desiredRepositoryVersionId, String desiredState, StackId desiredStackId) {
    this(clusterName, serviceGroupName, serviceName, serviceName, desiredRepositoryVersionId, desiredState, null,desiredStackId );
  }

  public ServiceRequest(String clusterName,
                        String serviceGroupName,
                        String serviceName,
                        String serviceType,
                        Long desiredRepositoryVersionId,
                        String desiredState,
                        String credentialStoreEnabled,
                        StackId desiredStackId) {
    this.clusterName = clusterName;
    this.serviceGroupName = serviceGroupName;
    this.serviceName = serviceName;
    this.desiredState = desiredState;

    this.desiredRepositoryVersionId = desiredRepositoryVersionId;

    this.serviceType = serviceType;
    this.desiredState = desiredState;
    this.credentialStoreEnabled = credentialStoreEnabled;
    // Credential store supported cannot be changed after
    // creation since it comes from the stack definition.
    // We can update credential store enabled alone.
    this.desiredStackId = desiredStackId;
  }

  /**
   * @return the service name
   */
  @ApiModelProperty(name = "service_name")
  public String getServiceName() { return serviceName; }

  /**
   * @param serviceName the service name to set
   */
  public void setServiceName(String serviceName) { this.serviceName = serviceName; }

  /**
   * @return the service type
   */
  @ApiModelProperty(name = "service_type")
  public String getServiceType() { return serviceType; }

  /**
   * @param serviceType the service type to set
   */
  public void setServiceType(String serviceType) { this.serviceType = serviceType; }

  /**
   * @return the service group Name
   */
  public String getServiceGroupName() { return serviceGroupName; }

  /**
   * @param serviceGroupName the service group Name to set
   */
  public void setServiceGroupName(String serviceGroupName) { this.serviceGroupName = serviceGroupName; }

  /**
   * @return the desiredState
   */
  @ApiModelProperty(name = "state")
  public String getDesiredState() { return desiredState; }

  /**
   * @param desiredState the desiredState to set
   */
  public void setDesiredState(String desiredState) { this.desiredState = desiredState; }

  public Long getDesiredRepositoryVersionId() { return desiredRepositoryVersionId; }

  /**
   * @return the clusterName
   */
  @ApiModelProperty(name = "cluster_name")
  public String getClusterName() { return clusterName; }

  /**
   * @param clusterName the clusterName to set
   */
  public void setClusterName(String clusterName) { this.clusterName = clusterName; }

  /**
   * @param state the new maintenance state
   */
  public void setMaintenanceState(String state) { maintenanceState = state; }

  /**
   * @return the maintenance state
   */
  @ApiModelProperty(name = "maintenance_state")
  public String getMaintenanceState() { return maintenanceState; }

  /**
   * @return credential store enabled
   */
  @ApiModelProperty(name = "credential_store_enabled")
  public String getCredentialStoreEnabled() { return credentialStoreEnabled; }


  /**
   * @return credential store supported
   */
  public String getCredentialStoreSupported() { return credentialStoreSupported; }

  /**
   * @param credentialStoreEnabled the new credential store enabled
   */
  public void setCredentialStoreEnabled(String credentialStoreEnabled) { this.credentialStoreEnabled = credentialStoreEnabled; }

  /**
   * @param credentialStoreSupported the new credential store supported
   */
  @ApiModelProperty(name = "credential_store_supporteds")
  public void setCredentialStoreSupported(String credentialStoreSupported) { this.credentialStoreSupported = credentialStoreSupported; }


  /***
   *
   * @return associated stackid with the service
   */
  public StackId getDesiredStackId() {
    return desiredStackId;
  }

  /***
   * @param desiredStackId associated with the service
   */
  public void setDesiredStackId(StackId desiredStackId) {
    this.desiredStackId = desiredStackId;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("clusterName=" + clusterName
      + ", serviceGroupName=" + serviceGroupName
      + ", serviceName=" + serviceName
      + ", desiredState=" + desiredState
      + ", maintenanceState=" + maintenanceState
      + ", credentialStoreEnabled=" + credentialStoreEnabled
      + ", credentialStoreSupported=" + credentialStoreSupported
      + ", desiredStackId=" + desiredStackId);
    return sb.toString();
  }

  /**
   * @param repositoryVersion
   */
  public void setResolvedRepository(RepositoryVersionEntity repositoryVersion) { resolvedRepository = repositoryVersion; }

  public RepositoryVersionEntity getResolvedRepository() { return resolvedRepository; }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    ServiceRequest other = (ServiceRequest) obj;

    return Objects.equals(clusterName, other.clusterName) &&
      Objects.equals(serviceGroupName, other.serviceGroupName) &&
      Objects.equals(serviceType, other.serviceType) &&
      Objects.equals(serviceName, other.serviceName) &&
      Objects.equals(desiredState, other.desiredState) &&
      Objects.equals(maintenanceState, other.maintenanceState) &&
      Objects.equals(credentialStoreEnabled, other.credentialStoreEnabled) &&
      Objects.equals(credentialStoreSupported, other.credentialStoreSupported) &&
      Objects.equals(desiredStackId, other.desiredStackId) &&
      Objects.equals(desiredRepositoryVersionId, other.desiredRepositoryVersionId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(clusterName, serviceGroupName, serviceType, serviceName, desiredState, maintenanceState, credentialStoreEnabled, credentialStoreSupported, desiredStackId, desiredRepositoryVersionId);
  }
}
