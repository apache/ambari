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
package org.apache.ambari.server.agent;

import java.util.List;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Command to report the status of a list of services in roles.
 */
public class StatusCommand extends AgentCommand {

  public StatusCommand() {
    super(AgentCommandType.STATUS_COMMAND);
  }

  private String clusterName;
  private String serviceName;
  private String componentName;
  private Map<String, Map<String, String>> configurations;

  @JsonProperty("clusterName")
  public String getClusterName() {
    return clusterName;
  }
  
  @JsonProperty("clusterName")
  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  @JsonProperty("serviceName")
  public String getServiceName() {
    return serviceName;
  }

  @JsonProperty("serviceName")
  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  @JsonProperty("componentName")
  public String getComponentName() {
    return componentName;
  }

  @JsonProperty("componentName")
  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }
  
  @JsonProperty("configurations")
  public Map<String, Map<String, String>> getConfigurations() {
    return configurations;
  }

  @JsonProperty("configurations")
  public void setConfigurations(Map<String, Map<String, String>> configurations) {
    this.configurations = configurations;
  }
}
