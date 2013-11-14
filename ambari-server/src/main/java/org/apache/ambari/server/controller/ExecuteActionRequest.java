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

import org.apache.ambari.server.utils.StageUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class to capture details used to create action or custom commands
 */
public class ExecuteActionRequest {
  private String clusterName;
  private String commandName;
  private String actionName;
  private String serviceName;
  private String componentName;
  private List<String> hosts;
  private Map<String, String> parameters;

  public ExecuteActionRequest(String clusterName, String commandName,
                              String actionName, String serviceName, String componentName,
                              List<String> hosts, Map<String, String> parameters) {
    this(clusterName, commandName, serviceName, parameters);
    this.actionName = actionName;
    this.componentName = componentName;
    if (hosts != null) {
      this.hosts.addAll(hosts);
    }
  }

  /**
   * Create an ExecuteActionRequest to execute a command
   */
  public ExecuteActionRequest(String clusterName, String commandName, String serviceName,
                              Map<String, String> parameters) {
    this.clusterName = clusterName;
    this.commandName = commandName;
    this.serviceName = serviceName;
    this.parameters = new HashMap<String, String>();
    if (parameters != null) {
      this.parameters.putAll(parameters);
    }
    this.hosts = new ArrayList<String>();
  }

  public String getClusterName() {
    return clusterName;
  }

  public String getCommandName() {
    return commandName;
  }

  public String getActionName() {
    return actionName;
  }

  public String getServiceName() {
    return serviceName;
  }

  public String getComponentName() {
    return componentName;
  }

  public Map<String, String> getParameters() {
    return parameters;
  }

  public List<String> getHosts() {
    return hosts;
  }

  public Boolean isCommand() {
    return actionName == null || actionName.isEmpty();
  }

  @Override
  public synchronized String toString() {
    return (new StringBuilder()).
        append("isCommand :" + isCommand().toString()).
        append(", action :" + actionName).
        append(", command :" + commandName).
        append(", inputs :" + parameters.toString()).
        append(", targetService :" + serviceName).
        append(", targetComponent :" + componentName).
        append(", targetHosts :" + hosts.toString()).
        append(", clusterName :" + clusterName).toString();
  }
}
