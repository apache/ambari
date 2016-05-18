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

import java.util.List;
import java.util.Map;

import org.apache.ambari.server.actionmanager.TargetHostType;
import org.apache.ambari.server.controller.internal.RequestOperationLevel;
import org.apache.ambari.server.controller.internal.RequestResourceFilter;

/**
 * The context required to create tasks and stages for a custom action
 */
public class ActionExecutionContext {
  private final String clusterName;
  private final String actionName;
  private List<RequestResourceFilter> resourceFilters;
  private RequestOperationLevel operationLevel;
  private Map<String, String> parameters;
  private TargetHostType targetType;
  private Short timeout;
  private String expectedServiceName;
  private String expectedComponentName;
  private boolean hostsInMaintenanceModeExcluded = true;
  private boolean allowRetry = false;

  /**
   * {@code true} if slave/client component failures should be automatically
   * skipped. This will only automatically skip the failure if the task is
   * skippable to begin with.
   */
  private boolean autoSkipFailures = false;

  /**
   * Create an ActionExecutionContext to execute an action from a request
   */
  public ActionExecutionContext(String clusterName, String actionName,
      List<RequestResourceFilter> resourceFilters,
      Map<String, String> parameters, TargetHostType targetType,
      Short timeout, String expectedServiceName,
      String expectedComponentName) {

    this.clusterName = clusterName;
    this.actionName = actionName;
    this.resourceFilters = resourceFilters;
    this.parameters = parameters;
    this.targetType = targetType;
    this.timeout = timeout;
    this.expectedServiceName = expectedServiceName;
    this.expectedComponentName = expectedComponentName;
  }

  public ActionExecutionContext(String clusterName, String actionName,
                                List<RequestResourceFilter> resourceFilters) {
    this.clusterName = clusterName;
    this.actionName = actionName;
    this.resourceFilters = resourceFilters;
  }

  public ActionExecutionContext(String clusterName, String commandName,
                                List<RequestResourceFilter> resourceFilters,
                                Map<String, String> parameters) {
    this.clusterName = clusterName;
    actionName = commandName;
    this.resourceFilters = resourceFilters;
    this.parameters = parameters;
  }

  public String getClusterName() {
    return clusterName;
  }

  public String getActionName() {
    return actionName;
  }

  public Map<String, String> getParameters() {
    return parameters;
  }

  public TargetHostType getTargetType() {
    return targetType;
  }

  public Short getTimeout() {
    return timeout;
  }

  public void setTimeout(Short timeout) {
    this.timeout = timeout;
  }

  public List<RequestResourceFilter> getResourceFilters() {
    return resourceFilters;
  }

  public RequestOperationLevel getOperationLevel() {
    return operationLevel;
  }

  public void setOperationLevel(RequestOperationLevel operationLevel) {
    this.operationLevel = operationLevel;
  }

  public String getExpectedServiceName() {
    return expectedServiceName;
  }

  public String getExpectedComponentName() {
    return expectedComponentName;
  }

  /**
   * Gets whether the action can be retried if it failed. The default is
   * {@code true)}.
   *
   * @return {@code true} if the action can be retried if it fails.
   */
  public boolean isRetryAllowed() {
    return allowRetry;
  }

  /**
   * Sets whether the action can be retried if it fails.
   *
   * @param allowRetry
   *          {@code true} if the action can be retried if it fails.
   */
  public void setRetryAllowed(boolean allowRetry){
    this.allowRetry = allowRetry;
  }

  /**
   * Gets whether skippable actions that failed are automatically skipped.
   *
   * @return the autoSkipFailures
   */
  public boolean isFailureAutoSkipped() {
    return autoSkipFailures;
  }

  /**
   * Sets whether skippable action that failed are automatically skipped.
   *
   * @param autoSkipFailures
   *          {@code true} to automatically skip failures which are marked as
   *          skippable.
   */
  public void setAutoSkipFailures(boolean autoSkipFailures) {
    this.autoSkipFailures = autoSkipFailures;
  }

  @Override
  public String toString() {
    return "ActionExecutionContext{" +
      "clusterName='" + clusterName + '\'' +
      ", actionName='" + actionName + '\'' +
      ", resourceFilters=" + resourceFilters +
      ", operationLevel=" + operationLevel +
      ", parameters=" + parameters +
      ", targetType=" + targetType +
      ", timeout=" + timeout +
      ", isMaintenanceModeHostExcluded=" + hostsInMaintenanceModeExcluded +
      ", allowRetry=" + allowRetry +
      ", autoSkipFailures=" + autoSkipFailures +
      '}';
  }

  /**
   * Gets whether hosts in maintenance mode should be excluded from the command.
   *
   * @return {@code true} to exclude any hosts in maintenance mode from the
   *         command, {@code false} to include hosts which are in maintenance
   *         mode.
   */
  public boolean isMaintenanceModeHostExcluded() {
    return hostsInMaintenanceModeExcluded;
  }

  /**
   * Sets whether hosts in maintenance mode should be excluded from the command.
   *
   * @param excluded
   *          {@code true} to exclude any hosts in maintenance mode from the
   *          command, {@code false} to include hosts which are in maintenance
   *          mode.
   */
  public void setMaintenanceModeHostExcluded(boolean excluded) {
    hostsInMaintenanceModeExcluded = excluded;
  }

}