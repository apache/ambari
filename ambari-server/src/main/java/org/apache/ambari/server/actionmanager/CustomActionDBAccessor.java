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

import org.apache.ambari.server.AmbariException;

import java.util.List;

/**
 * The DB accessor implementation for Action definitions
 */
public interface CustomActionDBAccessor {

  /**
   * Given an actionName, get the Action resource
   *
   * @param actionName name of the action
   * @return
   * @throws AmbariException
   */
  public ActionDefinition getActionDefinition(String actionName) throws AmbariException;

  /**
   * Get all action definition resources
   *
   * @return
   */
  public List<ActionDefinition> getActionDefinitions();

  /**
   * Create an action definition resource
   *
   * @param actionName     name of the action
   * @param actionType     type of the action
   * @param inputs         inputs required by the action
   * @param description    a short description of the action
   * @param targetType     the host target type
   * @param serviceType    the service type on which the action must be executed
   * @param componentType  the component type on which the action must be executed
   * @param defaultTimeout the default timeout for this action
   * @throws AmbariException
   */
  public void createActionDefinition(String actionName, ActionType actionType, String inputs, String description,
                                     TargetHostType targetType, String serviceType, String componentType,
                                     Short defaultTimeout) throws AmbariException;

  /**
   * Update an action definition
   *
   * @param actionName     name of the action
   * @param actionType     type of the action
   * @param description    a short description of the action
   * @param targetType     the host target type
   * @param defaultTimeout the default timeout for this action
   * @throws AmbariException
   */
  public void updateActionDefinition(String actionName, ActionType actionType, String description,
                                     TargetHostType targetType, Short defaultTimeout) throws AmbariException;

  /**
   * Delete an action definition
   *
   * @param actionName
   * @throws AmbariException
   */
  public void deleteActionDefinition(String actionName) throws AmbariException;
}
