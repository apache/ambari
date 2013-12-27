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

package org.apache.ambari.server.actionmanager;

import org.apache.ambari.server.orm.entities.ActionEntity;

/**
 * The resource describing the definition of an action
 */
public class ActionDefinition {
  private String actionName;
  private ActionType actionType;
  private String inputs;
  private String targetService;
  private String targetComponent;
  private String description;
  private TargetHostType targetType;
  private Short defaultTimeout;

  /**
   * Create an instance of ActionDefinition
   * @param actionName      The name of the action
   * @param actionType      The type fo the action
   * @param inputs          Expected input of the action
   * @param targetService   Target service type (e.g. HDFS)
   * @param targetComponent Target component type (e.g. DATANODE)
   * @param description     Short description of the action
   * @param targetType      Selection criteria for target hosts
   * @param defaultTimeout  The timeout value for this action when executed
   */
  public ActionDefinition(String actionName, ActionType actionType, String inputs,
                          String targetService, String targetComponent, String description,
                          TargetHostType targetType, Short defaultTimeout) {
    setActionName(actionName);
    setActionType(actionType);
    setInputs(inputs);
    setTargetService(targetService);
    setTargetComponent(targetComponent);
    setDescription(description);
    setTargetType(targetType);
    setDefaultTimeout(defaultTimeout);
  }

  /**
   * Create an instance of ActionDefinition
   * @param entity  The entity corresponding to the action
   */
  public ActionDefinition(ActionEntity entity) {
    setActionName(entity.getActionName());
    setActionType(entity.getActionType());
    setInputs(entity.getInputs());
    setTargetService(entity.getTargetService());
    setTargetComponent(entity.getTargetComponent());
    setDescription(entity.getDescription());
    setTargetType(entity.getTargetType());
    setDefaultTimeout(entity.getDefaultTimeout());
  }

  public String getActionName() {
    return actionName;
  }

  public void setActionName(String actionName) {
    this.actionName = actionName;
  }

  public ActionType getActionType() {
    return actionType;
  }

  public void setActionType(ActionType actionType) {
    this.actionType = actionType;
  }

  public String getInputs() {
    return inputs;
  }

  public void setInputs(String inputs) {
    this.inputs = inputs;
  }

  public String getTargetService() {
    return targetService;
  }

  public void setTargetService(String targetService) {
    this.targetService = targetService;
  }

  public String getTargetComponent() {
    return targetComponent;
  }

  public void setTargetComponent(String targetComponent) {
    this.targetComponent = targetComponent;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public TargetHostType getTargetType() {
    return targetType;
  }

  public void setTargetType(TargetHostType targetType) {
    this.targetType = targetType;
  }

  public Short getDefaultTimeout() {
    return defaultTimeout;
  }

  public void setDefaultTimeout(Short defaultTimeout) {
    this.defaultTimeout = defaultTimeout;
  }
}
