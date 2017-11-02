/**
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


public class ServiceGroupDependencyRequest {

  private String clusterName; // REF
  private String serviceGroupName; // GET/CREATE/UPDATE/DELETE
  private String dependencyServiceGroupName;

  public ServiceGroupDependencyRequest(String clusterName, String serviceGroupName, String dependencyServiceGroupName) {
    this.clusterName = clusterName;
    this.serviceGroupName = serviceGroupName;
    this.dependencyServiceGroupName = dependencyServiceGroupName;
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

  public String getDependencyServiceGroupName() {
    return dependencyServiceGroupName;
  }

  public void setDependencyServiceGroupName(String dependencyServiceGroupName) {
    this.dependencyServiceGroupName = dependencyServiceGroupName;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("clusterName=" + clusterName
            + ", serviceGroupName=" + serviceGroupName);
    return sb.toString();
  }
}
