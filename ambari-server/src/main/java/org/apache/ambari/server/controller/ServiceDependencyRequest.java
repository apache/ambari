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
  private String dependencyServiceName;
  private String dependencyServiceGroupName;

  public ServiceDependencyRequest(String clusterName, String serviceName, String serviceGroupName,
                                  String dependencyServiceName, String dependencyServiceGroupName) {
    this.clusterName = clusterName;
    this.serviceName = serviceName;
    this.serviceGroupName = serviceGroupName;
    this.dependencyServiceName = dependencyServiceName;
    this.dependencyServiceGroupName = dependencyServiceGroupName;
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public String getDependencyServiceName() {
    return dependencyServiceName;
  }

  public void setDependencyServiceName(String dependencyServiceName) {
    this.dependencyServiceName = dependencyServiceName;
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

  public String getDependencyServiceGroupName() {
    return dependencyServiceGroupName;
  }

  public void setDependencyServiceGroupName(String dependencyServiceGroupName) {
    this.dependencyServiceGroupName = dependencyServiceGroupName;
  }

  @Override
  public String toString() {
    return "ServiceDependencyRequest{" +
            "clusterName='" + clusterName + '\'' +
            ", serviceName='" + serviceName + '\'' +
            ", serviceGroupName='" + serviceGroupName + '\'' +
            ", dependencyServiceName='" + dependencyServiceName + '\'' +
            ", dependencyServiceGroupName='" + dependencyServiceGroupName + '\'' +
            '}';
  }
}
