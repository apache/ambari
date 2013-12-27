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

/**
 * Used to perform CRUD operations of Action
 */
public class ActionRequest {

  private String actionName;  //CRUD
  private String actionType;  //C
  private String inputs;  //C
  private String targetService;  //C
  private String targetComponent;  //C
  private String description;  //CU
  private String targetType;  //CU
  private String defaultTimeout;  //CU

  public ActionRequest(
      String actionName, String actionType, String inputs,
      String targetService, String targetComponent, String description, String targetType,
      String defaultTimeout) {
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
   * Create the request to get all defined actions
   *
   * @return
   */
  public static ActionRequest getAllRequest() {
    return new ActionRequest(null, null, null, null, null, null, null, null);
  }

  public String getActionName() {
    return actionName;
  }

  public void setActionName(String actionName) {
    this.actionName = actionName;
  }

  public String getActionType() {
    return actionType;
  }

  public void setActionType(String actionType) {
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

  public String getTargetType() {
    return targetType;
  }

  public void setTargetType(String targetType) {
    this.targetType = targetType;
  }

  public String getDefaultTimeout() {
    return defaultTimeout;
  }

  public void setDefaultTimeout(String defaultTimeout) {
    this.defaultTimeout = defaultTimeout;
  }

  @Override
  public String toString() {
    return (new StringBuilder()).
        append("actionName :" + actionName).
        append(", actionType :" + actionType).
        append(", inputs :" + inputs).
        append(", targetService :" + targetService).
        append(", targetComponent :" + targetComponent).
        append(", description :" + description).
        append(", targetType :" + targetType).
        append(", defaultTimeout :" + defaultTimeout).toString();
  }
}
