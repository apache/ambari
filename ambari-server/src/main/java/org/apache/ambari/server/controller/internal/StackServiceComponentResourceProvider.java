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
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.StackServiceComponentRequest;
import org.apache.ambari.server.controller.StackServiceComponentResponse;
import org.apache.ambari.server.controller.spi.*;
import org.apache.ambari.server.controller.spi.Resource.Type;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.state.AutoDeployInfo;

import java.util.*;

public class StackServiceComponentResourceProvider extends
    ReadOnlyResourceProvider {

  private static final String STACK_NAME_PROPERTY_ID = PropertyHelper.getPropertyId(
      "StackServiceComponents", "stack_name");

  private static final String STACK_VERSION_PROPERTY_ID = PropertyHelper.getPropertyId(
      "StackServiceComponents", "stack_version");

  private static final String SERVICE_NAME_PROPERTY_ID = PropertyHelper.getPropertyId(
      "StackServiceComponents", "service_name");

  private static final String COMPONENT_NAME_PROPERTY_ID = PropertyHelper.getPropertyId(
      "StackServiceComponents", "component_name");

  private static final String COMPONENT_DISPLAY_NAME_PROPERTY_ID = PropertyHelper.getPropertyId(
          "StackServiceComponents", "display_name");

  private static final String COMPONENT_CATEGORY_PROPERTY_ID = PropertyHelper.getPropertyId(
      "StackServiceComponents", "component_category");

  private static final String IS_CLIENT_PROPERTY_ID = PropertyHelper.getPropertyId(
      "StackServiceComponents", "is_client");

  private static final String IS_MASTER_PROPERTY_ID = PropertyHelper.getPropertyId(
      "StackServiceComponents", "is_master");

  private static final String CARDINALITY_ID = PropertyHelper.getPropertyId(
      "StackServiceComponents", "cardinality");

  private static final String ADVERTISE_VERSION_ID = PropertyHelper.getPropertyId(
      "StackServiceComponents", "advertise_version");

  private static final String CUSTOM_COMMANDS_PROPERTY_ID = PropertyHelper.getPropertyId(
      "StackServiceComponents", "custom_commands");

  private static final String AUTO_DEPLOY_ENABLED_ID = PropertyHelper.getPropertyId(
      "auto_deploy", "enabled");

  private static final String AUTO_DEPLOY_LOCATION_ID = PropertyHelper.getPropertyId(
      "auto_deploy", "location");

  private static Set<String> pkPropertyIds = new HashSet<String>(
      Arrays.asList(new String[] { STACK_NAME_PROPERTY_ID,
          STACK_VERSION_PROPERTY_ID, SERVICE_NAME_PROPERTY_ID,
          COMPONENT_NAME_PROPERTY_ID }));

  protected StackServiceComponentResourceProvider(Set<String> propertyIds,
      Map<Type, String> keyPropertyIds,
      AmbariManagementController managementController) {
    super(propertyIds, keyPropertyIds, managementController);
  }


  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {
    final Set<StackServiceComponentRequest> requests = new HashSet<StackServiceComponentRequest>();

    if (predicate == null) {
      requests.add(getRequest(Collections.<String, Object>emptyMap()));
    } else {
      for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
        requests.add(getRequest(propertyMap));
      }
    }

    Set<String> requestedIds = getRequestPropertyIds(request, predicate);

    Set<StackServiceComponentResponse> responses = getResources(new Command<Set<StackServiceComponentResponse>>() {
      @Override
      public Set<StackServiceComponentResponse> invoke() throws AmbariException {
        return getManagementController().getStackComponents(requests);
      }
    });

    Set<Resource> resources = new HashSet<Resource>();

    for (StackServiceComponentResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.StackServiceComponent);

      setResourceProperty(resource, STACK_NAME_PROPERTY_ID,
          response.getStackName(), requestedIds);

      setResourceProperty(resource, STACK_VERSION_PROPERTY_ID,
          response.getStackVersion(), requestedIds);

      setResourceProperty(resource, SERVICE_NAME_PROPERTY_ID,
          response.getServiceName(), requestedIds);

      setResourceProperty(resource, COMPONENT_NAME_PROPERTY_ID,
          response.getComponentName(), requestedIds);

      setResourceProperty(resource, COMPONENT_DISPLAY_NAME_PROPERTY_ID,
              response.getComponentDisplayName(), requestedIds);

      setResourceProperty(resource, COMPONENT_CATEGORY_PROPERTY_ID,
          response.getComponentCategory(), requestedIds);

      setResourceProperty(resource, IS_CLIENT_PROPERTY_ID,
          response.isClient(), requestedIds);

      setResourceProperty(resource, IS_MASTER_PROPERTY_ID,
          response.isMaster(), requestedIds);

      setResourceProperty(resource, CARDINALITY_ID,
          response.getCardinality(), requestedIds);

      setResourceProperty(resource, ADVERTISE_VERSION_ID,
          response.isVersionAdvertised(), requestedIds);

      setResourceProperty(resource, CUSTOM_COMMANDS_PROPERTY_ID,
          response.getCustomCommands(), requestedIds);

      AutoDeployInfo autoDeployInfo = response.getAutoDeploy();
      if (autoDeployInfo != null) {
        setResourceProperty(resource, AUTO_DEPLOY_ENABLED_ID,
            autoDeployInfo.isEnabled(), requestedIds);

        if (autoDeployInfo.getCoLocate() != null) {
          setResourceProperty(resource, AUTO_DEPLOY_LOCATION_ID,
              autoDeployInfo.getCoLocate(), requestedIds);
        }
      }
      resources.add(resource);
    }

    return resources;
  }

  private StackServiceComponentRequest getRequest(Map<String, Object> properties) {
    return new StackServiceComponentRequest(
        (String) properties.get(STACK_NAME_PROPERTY_ID),
        (String) properties.get(STACK_VERSION_PROPERTY_ID),
        (String) properties.get(SERVICE_NAME_PROPERTY_ID),
        (String) properties.get(COMPONENT_NAME_PROPERTY_ID));
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

}
