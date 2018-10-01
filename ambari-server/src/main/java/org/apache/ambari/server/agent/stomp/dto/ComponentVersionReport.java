/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.agent.stomp.dto;

import org.apache.ambari.server.agent.HeartbeatProcessor.ComponentVersionStructuredOut;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ComponentVersionReport {
  private String componentName;
  private String serviceName;
  private String serviceGroupName;
  private Long clusterId;

  @JsonProperty("version_reporting")
  private ComponentVersionStructuredOut componentVersionStructuredOut;

  public ComponentVersionReport() {
  }

  public ComponentVersionReport(Long clusterId, String serviceGroupName, String serviceName, String componentName,
      ComponentVersionStructuredOut componentVersionStructuredOut) {
    this.componentName = componentName;
    this.serviceName = serviceName;
    this.serviceGroupName = serviceGroupName;
    this.clusterId = clusterId;
    this.componentVersionStructuredOut = componentVersionStructuredOut;
  }

  public String getComponentName() {
    return componentName;
  }

  public void setComponentName(String componentName) {
    this.componentName = componentName;
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

  public ComponentVersionStructuredOut getVersionStructuredOutput() {
    return componentVersionStructuredOut;
  }

  public void setVersionStructuredOutput(
      ComponentVersionStructuredOut componentVersionStructuredOut) {
    this.componentVersionStructuredOut = componentVersionStructuredOut;
  }

  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }
}
