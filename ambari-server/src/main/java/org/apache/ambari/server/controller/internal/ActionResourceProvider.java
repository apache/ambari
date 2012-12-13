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
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

import java.util.Arrays;
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
  public RequestStatus createResources(Request request) throws AmbariException, UnsupportedPropertyException {
    checkRequestProperties(Resource.Type.Action, request);
    Set<ActionRequest> requests = new HashSet<ActionRequest>();
    for (Map<String, Object> propertyMap : request.getProperties()) {
      requests.add(getRequest(propertyMap));
    }
    return getRequestStatus(getManagementController().createActions(requests));
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws AmbariException, UnsupportedPropertyException {
    throw new UnsupportedOperationException("Not currently supported.");
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
      throws AmbariException, UnsupportedPropertyException {
    throw new UnsupportedOperationException("Not currently supported.");
  }

  @Override
  public RequestStatus deleteResources(Predicate predicate) throws AmbariException, UnsupportedPropertyException {
    throw new UnsupportedOperationException("Not currently supported.");
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

  private ActionRequest getRequest(Map<String, Object> properties) {
    Map<String, String> params = new HashMap<String, String>();
    Iterator<Entry<String, Object>> it1 = properties.entrySet().iterator();
    while (it1.hasNext()) {
      Entry<String, Object> entry = it1.next();
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
