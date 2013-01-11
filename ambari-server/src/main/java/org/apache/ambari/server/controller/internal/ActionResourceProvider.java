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
import org.apache.ambari.server.controller.ActionRequest;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.*;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Resource provider for action resources.
 */
class ActionResourceProvider extends ResourceProviderImpl {

  // ----- Property ID constants ---------------------------------------------

  // Actions
  protected static final String ACTION_CLUSTER_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("Actions", "cluster_name");
  protected static final String ACTION_SERVICE_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("Actions", "service_name");
  protected static final String ACTION_ACTION_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("Actions", "action_name");


  private static Set<String> pkPropertyIds =
      new HashSet<String>(Arrays.asList(new String[]{
          ACTION_CLUSTER_NAME_PROPERTY_ID,
          ACTION_SERVICE_NAME_PROPERTY_ID}));

  ActionResourceProvider(Set<String> propertyIds,
                         Map<Resource.Type, String> keyPropertyIds,
                         AmbariManagementController managementController) {

    super(propertyIds, keyPropertyIds, managementController);
  }

  @Override
  public RequestStatus createResources(Request request)
      throws SystemException,
             UnsupportedPropertyException,
             ResourceAlreadyExistsException,
             NoSuchParentResourceException {

    final Set<ActionRequest> requests = new HashSet<ActionRequest>();
    for (Map<String, Object> propertyMap : request.getProperties()) {
      requests.add(getRequest(propertyMap));
    }
    return getRequestStatus(createResources(new Command<RequestStatusResponse>() {
      @Override
      public RequestStatusResponse invoke() throws AmbariException {
        return getManagementController().createActions(requests);
      }
    }));
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("Not currently supported.");
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("Not currently supported.");
  }

  @Override
  public RequestStatus deleteResources(Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("Not currently supported.");
  }

  @Override
  public Set<String> checkPropertyIds(Set<String> propertyIds) {
    propertyIds = super.checkPropertyIds(propertyIds);

    if (propertyIds.isEmpty()) {
      return propertyIds;
    }
    Set<String> unsupportedProperties = new HashSet<String>();

    for (String propertyId : propertyIds) {
      String propertyCategory = PropertyHelper.getPropertyCategory(propertyId);
      if (propertyCategory == null || !propertyCategory.equals("parameters")) {
        unsupportedProperties.add(propertyId);
      }
    }
    return unsupportedProperties;
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

  private ActionRequest getRequest(Map<String, Object> properties) {
    Map<String, String> params = new HashMap<String, String>();
    for (Entry<String, Object> entry : properties.entrySet()) {
      String propertyid = entry.getKey();
      if (PropertyHelper.getPropertyCategory(propertyid).equals("parameters")
          && null != entry.getValue()) {
        params.put(PropertyHelper.getPropertyName(propertyid), entry.getValue().toString());
      }
    }
    return new ActionRequest(
        (String)  properties.get(ACTION_CLUSTER_NAME_PROPERTY_ID),
        (String)  properties.get(ACTION_SERVICE_NAME_PROPERTY_ID),
        (String)  properties.get(ACTION_ACTION_NAME_PROPERTY_ID),
        params);
  }
}
