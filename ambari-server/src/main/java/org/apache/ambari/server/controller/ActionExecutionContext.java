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

import org.apache.ambari.server.actionmanager.TargetHostType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The context required to create tasks and stages for a custom action
 */
public class ActionExecutionContext {
  private final String clusterName;
  private final String actionName;
  private final String serviceName;
  private final String componentName;
  private final List<String> hosts;
  private final Map<String, String> parameters;
  private final TargetHostType targetType;
  private final Short timeout;

  /**
   * Create an ActionExecutionContext to execute an action from a request
   */
  public ActionExecutionContext(String clusterName, String actionName, String serviceName,
                                String componentName, List<String> hosts, Map<String, String> parameters,
                                TargetHostType targetType, Short timeout) {
    this.clusterName = clusterName;
    this.actionName = actionName;
    this.serviceName = serviceName;
    this.componentName = componentName;
    this.parameters = parameters;
    this.hosts = new ArrayList<String>();
    if (hosts != null) {
      this.hosts.addAll(hosts);
    }
    this.targetType = targetType;
    this.timeout = timeout;
  }

  public String getClusterName() {
    return clusterName;
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

  public TargetHostType getTargetType() {
    return targetType;
  }

  public Short getTimeout() {
    return timeout;
  }
}