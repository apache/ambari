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

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.dao.ActionDefinitionDAO;
import org.apache.ambari.server.orm.entities.ActionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of DB accessor for Custom Action
 */
@Singleton
public class CustomActionDBAccessorImpl implements CustomActionDBAccessor {
  public static final Short MIN_TIMEOUT = 60;
  private static final Logger LOG = LoggerFactory.getLogger(CustomActionDBAccessorImpl.class);
  private static final Short MAX_TIMEOUT = 600;
  @Inject
  private ActionDefinitionDAO actionDefinitionDAO;

  @Inject
  public CustomActionDBAccessorImpl(Injector injector) {
    injector.injectMembers(this);
  }

  /**
   * Given an actionName, get the Action resource
   *
   * @param actionName name of the action
   * @return
   * @throws AmbariException
   */
  @Override
  public ActionDefinition getActionDefinition(String actionName) {
    ActionEntity action =
        actionDefinitionDAO.findByPK(actionName);
    if (action != null) {
      return new ActionDefinition(action);
    }

    return null;
  }

  /**
   * Get all action definition resources
   *
   * @return
   */
  @Override
  public List<ActionDefinition> getActionDefinitions() {
    List<ActionDefinition> result = new ArrayList<ActionDefinition>();
    List<ActionEntity> entities = actionDefinitionDAO.findAll();
    for (ActionEntity entity : entities) {
      result.add(new ActionDefinition(entity));
    }
    return result;
  }

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
  @Override
  @Transactional
  public void createActionDefinition(String actionName, ActionType actionType, String inputs, String description,
                                     TargetHostType targetType, String serviceType, String componentType,
                                     Short defaultTimeout)
      throws AmbariException {
    validateCreateInput(actionName, actionType, inputs, description, defaultTimeout,
        targetType, serviceType, componentType);
    ActionEntity entity =
        actionDefinitionDAO.findByPK(actionName);
    if (entity == null) {
      entity = new ActionEntity();
      entity.setActionName(actionName);
      entity.setActionType(actionType);
      entity.setInputs(inputs);
      entity.setTargetService(serviceType);
      entity.setTargetComponent(componentType);
      entity.setDescription(description);
      entity.setTargetType(targetType);
      entity.setDefaultTimeout(defaultTimeout);
      actionDefinitionDAO.merge(entity);
    } else {
      throw new AmbariException("Action definition " + actionName + " already exists");
    }
  }

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
  @Override
  @Transactional
  public void updateActionDefinition(String actionName, ActionType actionType, String description,
                                     TargetHostType targetType, Short defaultTimeout)
      throws AmbariException {
    ActionEntity entity = actionDefinitionDAO.findByPK(actionName);
    if (entity != null) {
      if (actionType != null) {
        if (actionType == ActionType.SYSTEM_DISABLED) {
          throw new AmbariException("Action type cannot be " + actionType);
        }
        entity.setActionType(actionType);
      }
      if (description != null) {
        if (description.isEmpty()) {
          throw new AmbariException("Action description cannot be empty");
        }
        entity.setDescription(description);
      }
      if (targetType != null) {
        entity.setTargetType(targetType);
      }
      if (defaultTimeout != null) {
        if (defaultTimeout < MIN_TIMEOUT || defaultTimeout > MAX_TIMEOUT) {
          throw new AmbariException("Default timeout should be between " + MIN_TIMEOUT + " and " + MAX_TIMEOUT);
        }
        entity.setDefaultTimeout(defaultTimeout);
      }
      actionDefinitionDAO.merge(entity);
    } else {
      throw new AmbariException("Action definition " + actionName + " does not exist");
    }
  }

  /**
   * Delete an action definition
   *
   * @param actionName
   * @throws AmbariException
   */
  @Override
  public void deleteActionDefinition(String actionName)
      throws AmbariException {
    validateActionName(actionName);
    ActionDefinition ad = getActionDefinition(actionName);
    if (ad != null) {
      actionDefinitionDAO.removeByPK(actionName);
    }
  }

  private void validateCreateInput(String actionName, ActionType actionType, String inputs,
                                   String description, Short defaultTimeout,
                                   TargetHostType targetType, String serviceType, String componentType)
      throws AmbariException {

    validateActionName(actionName);

    if (defaultTimeout < MIN_TIMEOUT || defaultTimeout > MAX_TIMEOUT) {
      throw new AmbariException("Default timeout should be between " + MIN_TIMEOUT + " and " + MAX_TIMEOUT);
    }

    if (actionType == ActionType.SYSTEM_DISABLED) {
      throw new AmbariException("Action type cannot be " + actionType);
    }

    if (description == null || description.isEmpty()) {
      throw new AmbariException("Action description cannot be empty");
    }

    if (actionType == null || actionType == ActionType.SYSTEM_DISABLED) {
      throw new AmbariException("Action type cannot be " + actionType);
    }

    if (serviceType == null || serviceType.isEmpty()) {
      if (componentType != null && !componentType.isEmpty()) {
        throw new AmbariException("Target component cannot be specified unless target service is specified");
      }
    }

    if (inputs != null && !inputs.isEmpty()) {
      String[] parameters = inputs.split(",");
      for (String parameter : parameters) {
        if (parameter.trim().isEmpty()) {
          throw new AmbariException("Empty parameter cannot be specified as an input parameter");
        }
      }
    }
  }

  private void validateActionName(String actionName)
      throws AmbariException {
    if (actionName == null || actionName.isEmpty()) {
      throw new AmbariException("Action name cannot be empty");
    }
    String trimmedName = actionName.replaceAll("\\s+", "");
    if (actionName.length() > trimmedName.length()) {
      throw new AmbariException("Action name cannot contain white spaces");
    }
  }
}
