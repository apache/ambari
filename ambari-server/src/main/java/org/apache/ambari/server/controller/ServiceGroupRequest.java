/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.controller;

import java.util.Objects;

public class ServiceGroupRequest {

  private String clusterName; // REF
  private String serviceGroupName; // GET/CREATE/UPDATE/DELETE
  private String version; // Associated stack version info

  public ServiceGroupRequest(String clusterName, String serviceGroupName, String version) {
    this.clusterName = clusterName;
    this.serviceGroupName = serviceGroupName;
    this.version = version;
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
   * @return the serviceGroupName
   */
  public String getServiceGroupName() {
    return serviceGroupName;
  }

  /**
   * @param serviceGroupName the service group name to set
   */
  public void setServiceGroupName(String serviceGroupName) {
    this.serviceGroupName = serviceGroupName;
  }

  /**
   * @return the servicegroup version
   */
  public String getVersion() {
    return version;
  }

  /**
   * @param version the servicegroup version to set
   */
  public void setVersion(String version) {
    this.version = version;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("clusterName=").append(clusterName).append(", serviceGroupName=").append(serviceGroupName).append(", version=").append(version);
    return sb.toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    ServiceGroupRequest other = (ServiceGroupRequest) obj;

    return Objects.equals(clusterName, other.clusterName) && Objects.equals(serviceGroupName, other.serviceGroupName) && Objects.equals(version, other.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(clusterName, serviceGroupName, version);
  }
}