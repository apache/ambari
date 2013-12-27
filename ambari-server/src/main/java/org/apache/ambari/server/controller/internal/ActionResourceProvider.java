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


package org.apache.ambari.server.controller.internal;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.ActionDefinition;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.ActionType;
import org.apache.ambari.server.actionmanager.CustomActionDBAccessorImpl;
import org.apache.ambari.server.actionmanager.TargetHostType;
import org.apache.ambari.server.controller.ActionRequest;
import org.apache.ambari.server.controller.ActionResponse;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Resource.Type;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ActionResourceProvider extends AbstractControllerResourceProvider {

  public static final String ACTION_NAME_PROPERTY_ID = PropertyHelper
      .getPropertyId("Actions", "action_name");
  public static final String ACTION_TYPE_PROPERTY_ID = PropertyHelper
      .getPropertyId("Actions", "action_type");
  public static final String INPUTS_PROPERTY_ID = PropertyHelper
      .getPropertyId("Actions", "inputs");
  public static final String TARGET_SERVICE_PROPERTY_ID = PropertyHelper
      .getPropertyId("Actions", "target_service");
  public static final String TARGET_COMPONENT_PROPERTY_ID = PropertyHelper
      .getPropertyId("Actions", "target_component");
  public static final String DESCRIPTION_PROPERTY_ID = PropertyHelper
      .getPropertyId("Actions", "description");
  public static final String TARGET_HOST_PROPERTY_ID = PropertyHelper
      .getPropertyId("Actions", "target_type");
  public static final String DEFAULT_TIMEOUT_PROPERTY_ID = PropertyHelper
      .getPropertyId("Actions", "default_timeout");
  private static Set<String> pkPropertyIds = new HashSet<String>(
      Arrays.asList(new String[]{ACTION_NAME_PROPERTY_ID}));
  private Boolean enableExperimental = false;

  public ActionResourceProvider(Set<String> propertyIds,
                                Map<Type, String> keyPropertyIds,
                                AmbariManagementController managementController) {
    super(propertyIds, keyPropertyIds, managementController);
  }

  public Boolean getEnableExperimental() {
    return enableExperimental;
  }

  public void setEnableExperimental(Boolean enabled) {
    enableExperimental = enabled;
  }

  @Override
  public RequestStatus createResources(Request request)
      throws SystemException,
      UnsupportedPropertyException,
      ResourceAlreadyExistsException,
      NoSuchParentResourceException {

    if (!getEnableExperimental()) {
      throw new UnsupportedOperationException("Not currently supported.");
    }

    for (final Map<String, Object> properties : request.getProperties()) {
      createResources(new Command<Void>() {
        @Override
        public Void invoke() throws AmbariException {
          ActionRequest actionReq = getRequest(properties);
          LOG.info("Received a create request for Action with"
              + ", actionName = " + actionReq.getActionName()
              + ", actionType = " + actionReq.getActionType()
              + ", description = " + actionReq.getDescription()
              + ", service = " + actionReq.getTargetService());

          createActionDefinition(actionReq);
          return null;
        }
      });
    }
    notifyCreate(Type.Action, request);

    return getRequestStatus(null);
  }

  @Override
  public RequestStatus updateResources(final Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    if (!getEnableExperimental()) {
      throw new UnsupportedOperationException("Not currently supported.");
    }

    final Set<ActionRequest> requests = new HashSet<ActionRequest>();
    RequestStatusResponse response;

    for (Map<String, Object> requestPropertyMap : request.getProperties()) {
      Set<Map<String, Object>> propertyMaps = getPropertyMaps(requestPropertyMap, predicate);
      for (Map<String, Object> propertyMap : propertyMaps) {
        ActionRequest actionReq = getRequest(propertyMap);
        LOG.info("Received a update request for Action with"
            + ", actionName = " + actionReq.getActionName()
            + ", actionType = " + actionReq.getActionType()
            + ", description = " + actionReq.getDescription()
            + ", timeout = " + actionReq.getDefaultTimeout());

        requests.add(actionReq);
      }
    }
    response = modifyResources(new Command<RequestStatusResponse>() {
      @Override
      public RequestStatusResponse invoke() throws AmbariException {
        return updateActionDefinitions(requests, request.getRequestInfoProperties());
      }
    });
    notifyUpdate(Type.Action, request, predicate);
    return getRequestStatus(response);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    final Set<ActionRequest> requests = new HashSet<ActionRequest>();
    if (predicate != null) {
      for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
        ActionRequest actionReq = getRequest(propertyMap);
        LOG.debug("Received a get request for Action with"
            + ", actionName = " + actionReq.getActionName());
        requests.add(actionReq);
      }
    } else {
      LOG.debug("Received a get request for all Actions");
      requests.add(ActionRequest.getAllRequest());
    }

    Set<ActionResponse> responses = getResources(new Command<Set<ActionResponse>>() {
      @Override
      public Set<ActionResponse> invoke() throws AmbariException {
        return getActionDefinitions(requests);
      }
    });

    Set<String> requestedIds = getRequestPropertyIds(request, predicate);
    Set<Resource> resources = new HashSet<Resource>();

    for (ActionResponse response : responses) {
      Resource resource = new ResourceImpl(Type.Action);
      setResourceProperty(resource, ACTION_NAME_PROPERTY_ID,
          response.getActionName(), requestedIds);
      setResourceProperty(resource, ACTION_TYPE_PROPERTY_ID,
          response.getActionType(), requestedIds);
      setResourceProperty(resource, INPUTS_PROPERTY_ID,
          response.getInputs(), requestedIds);
      setResourceProperty(resource, TARGET_SERVICE_PROPERTY_ID,
          response.getTargetService(), requestedIds);
      setResourceProperty(resource, TARGET_COMPONENT_PROPERTY_ID,
          response.getTargetComponent(), requestedIds);
      setResourceProperty(resource, DESCRIPTION_PROPERTY_ID,
          response.getDescription(), requestedIds);
      setResourceProperty(resource, TARGET_HOST_PROPERTY_ID,
          response.getTargetType(), requestedIds);
      setResourceProperty(resource, DEFAULT_TIMEOUT_PROPERTY_ID,
          response.getDefaultTimeout(), requestedIds);
      resources.add(resource);
    }
    return resources;
  }

  @Override
  public RequestStatus deleteResources(Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    if (!getEnableExperimental()) {
      throw new UnsupportedOperationException("Not currently supported.");
    }

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      final ActionRequest request = getRequest(propertyMap);
      try {
        LOG.info("Received a delete request for Action with"
            + ", actionName = " + request.getActionName());

        deleteActionDefinition(request);
      } catch (AmbariException ex) {
        throw new NoSuchResourceException(ex.getMessage());
      }
    }
    notifyDelete(Type.Action, predicate);
    return getRequestStatus(null);
  }

  private ActionRequest getRequest(Map<String, Object> properties) {
    ActionRequest ar = new ActionRequest(
        (String) properties.get(ACTION_NAME_PROPERTY_ID),
        (String) properties.get(ACTION_TYPE_PROPERTY_ID),
        (String) properties.get(INPUTS_PROPERTY_ID),
        (String) properties.get(TARGET_SERVICE_PROPERTY_ID),
        (String) properties.get(TARGET_COMPONENT_PROPERTY_ID),
        (String) properties.get(DESCRIPTION_PROPERTY_ID),
        (String) properties.get(TARGET_HOST_PROPERTY_ID),
        (String) properties.get(DEFAULT_TIMEOUT_PROPERTY_ID));

    return ar;
  }

  @Override
  public Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

  private ActionManager getActionManager() {
    return getManagementController().getActionManager();
  }

  protected synchronized void createActionDefinition(ActionRequest request)
      throws AmbariException {
    if (request.getActionName() == null
        || request.getActionName().isEmpty()) {
      throw new IllegalArgumentException("Action name should be provided");
    }

    LOG.info("Received a createActionDefinition request = " + request.toString());
    if (request.getTargetType() == null || request.getActionType() == null) {
      throw new AmbariException("Both target_type and action_type must be specified.");
    }
    TargetHostType targetType = TargetHostType.valueOf(request.getTargetType());
    ActionType actionType = ActionType.valueOf(request.getActionType());

    Short defaultTimeout = CustomActionDBAccessorImpl.MIN_TIMEOUT;
    if (request.getDefaultTimeout() != null && !request.getDefaultTimeout().isEmpty()) {
      defaultTimeout = Short.parseShort(request.getDefaultTimeout());
    }

    getActionManager().createActionDefinition(request.getActionName(), actionType, request.getInputs(),
        request.getDescription(), request.getTargetService(), request.getTargetComponent(),
        targetType, defaultTimeout);
  }

  protected synchronized Set<ActionResponse> getActionDefinitions(Set<ActionRequest> requests)
      throws AmbariException {
    Set<ActionResponse> responses = new HashSet<ActionResponse>();
    for (ActionRequest request : requests) {
      if (request.getActionName() == null) {
        List<ActionDefinition> ads = getActionManager().getAllActionDefinition();
        for (ActionDefinition ad : ads) {
          responses.add(new ActionResponse(ad));
        }
      } else {
        ActionDefinition ad = getActionManager().getActionDefinition(request.getActionName());
        if (ad != null) {
          responses.add(new ActionResponse(ad));
        }
      }
    }

    return responses;
  }

  protected synchronized RequestStatusResponse updateActionDefinitions(Set<ActionRequest> requests,
                                                                       Map<String, String> requestProperties)
      throws AmbariException {
    RequestStatusResponse response = null;
    for (ActionRequest request : requests) {
      if (null != request.getInputs() || null != request.getTargetService()
          || null != request.getTargetComponent()) {
        throw new AmbariException("Cannot update inputs, target_service, or target_component");
      }
      TargetHostType targetType = request.getTargetType() == null ? null
          : TargetHostType.valueOf(request.getTargetType());
      ActionType actionType = request.getActionType() == null ? null : ActionType.valueOf(request.getActionType());
      Short defaultTimeout = null;
      if (request.getDefaultTimeout() != null && !request.getDefaultTimeout().isEmpty()) {
        defaultTimeout = Short.parseShort(request.getDefaultTimeout());
      }
      getActionManager().updateActionDefinition(request.getActionName(), actionType,
          request.getDescription(), targetType, defaultTimeout);
    }
    return response;
  }

  protected synchronized void deleteActionDefinition(ActionRequest request)
      throws AmbariException {
    getActionManager().deleteActionDefinition(request.getActionName());
  }
}
