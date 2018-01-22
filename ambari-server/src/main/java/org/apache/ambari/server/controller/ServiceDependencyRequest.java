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

public class ServiceDependencyRequest {

  private String clusterName;
  private String serviceName;
  private String serviceGroupName;
  private String dependentClusterName;
  private String dependentServiceName;
  private String dependentServiceGroupName;
  private Long dependencyId;

  public ServiceDependencyRequest(String clusterName, String serviceName, String serviceGroupName, String dependentClusterName,
                                  String dependentServiceGroupName, String dependentServiceName, Long dependencyId) {
    this.clusterName = clusterName;
    this.serviceName = serviceName;
    this.serviceGroupName = serviceGroupName;
    this.dependentClusterName = dependentClusterName;
    this.dependentServiceGroupName = dependentServiceGroupName;
    this.dependentServiceName = dependentServiceName;
    this.dependencyId = dependencyId;
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public Long getDependencyId() {
    return dependencyId;
  }

  public void setDependencyId(Long dependencyId) {
    this.dependencyId = dependencyId;
  }

  public String getDependentClusterName() {
    return dependentClusterName;
  }

  public void setDependentClusterName(String dependentClusterName) {
    this.dependentClusterName = dependentClusterName;
  }

  public String getDependentServiceGroupName() {
    return dependentServiceGroupName;
  }

  public void setDependentServiceGroupName(String dependentServiceGroupName) {
    this.dependentServiceGroupName = dependentServiceGroupName;
  }

  public String getDependentServiceName() {
    return dependentServiceName;
  }

  public void setDependentServiceName(String dependentServiceName) {
    this.dependentServiceName = dependentServiceName;
  }

  public String getServiceGroupName() {
    return serviceGroupName;
  }

  public void setServiceGroupName(String serviceGroupName) {
    this.serviceGroupName = serviceGroupName;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  @Override
  public String toString() {
    return "ServiceDependencyRequest{" +
            "clusterName='" + clusterName + '\'' +
            ", serviceName='" + serviceName + '\'' +
            ", serviceGroupName='" + serviceGroupName + '\'' +
            ", dependentClusterName='" + dependentClusterName + '\'' +
            ", dependentServiceName='" + dependentServiceName + '\'' +
            ", dependentServiceGroupName='" + dependentServiceGroupName + '\'' +
            ", dependencyId=" + dependencyId +
            '}';
  }
}
