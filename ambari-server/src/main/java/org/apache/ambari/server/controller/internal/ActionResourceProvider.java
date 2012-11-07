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
import org.apache.ambari.server.controller.ActionResponse;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.PropertyId;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Resource provider for action resources.
 */
class ActionResourceProvider extends ResourceProviderImpl {

  // ----- Property ID constants ---------------------------------------------

  // Actions
  protected static final PropertyId ACTION_CLUSTER_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("cluster_name", "Actions");
  protected static final PropertyId ACTION_SERVICE_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("service_name", "Actions");
  protected static final PropertyId ACTION_ACTION_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("action_name", "Actions");


  private static Set<PropertyId> pkPropertyIds =
      new HashSet<PropertyId>(Arrays.asList(new PropertyId[]{
          ACTION_CLUSTER_NAME_PROPERTY_ID,
          ACTION_SERVICE_NAME_PROPERTY_ID}));

  ActionResourceProvider(Set<PropertyId> propertyIds,
                         Map<Resource.Type, PropertyId> keyPropertyIds,
                         AmbariManagementController managementController) {

    super(propertyIds, keyPropertyIds, managementController);
  }

  @Override
  public RequestStatus createResources(Request request) throws AmbariException {
    Set<ActionRequest> requests = new HashSet<ActionRequest>();
    for (Map<PropertyId, Object> propertyMap : request.getProperties()) {
      requests.add(getRequest(propertyMap));
    }
    return getRequestStatus(getManagementController().createActions(requests));
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws AmbariException {
    throw new UnsupportedOperationException("Not currently supported.");
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
      throws AmbariException {
    throw new UnsupportedOperationException("Not currently supported.");
  }

  @Override
  public RequestStatus deleteResources(Predicate predicate) throws AmbariException {
    throw new UnsupportedOperationException("Not currently supported.");
  }

  @Override
  protected Set<PropertyId> getPKPropertyIds() {
    return pkPropertyIds;
  }

  private ActionRequest getRequest(Map<PropertyId, Object> properties) {
    Map<String, String> params = new HashMap<String, String>();
    Iterator<Entry<PropertyId, Object>> it1 = properties.entrySet().iterator();
    while (it1.hasNext()) {
      Entry<PropertyId, Object> entry = it1.next();
      if (entry.getKey().getCategory().equals("parameters")
          && null != entry.getValue()) {
        params.put(entry.getKey().getName(), entry.getValue().toString());
      }
    }
    return new ActionRequest(
        (String)  properties.get(ACTION_CLUSTER_NAME_PROPERTY_ID),
        (String)  properties.get(ACTION_SERVICE_NAME_PROPERTY_ID),
        (String)  properties.get(ACTION_ACTION_NAME_PROPERTY_ID),
        params);
  }
}
