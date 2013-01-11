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
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.ServiceComponentRequest;
import org.apache.ambari.server.controller.ServiceComponentResponse;
import org.apache.ambari.server.controller.spi.*;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Resource provider for component resources.
 */
class ComponentResourceProvider extends ResourceProviderImpl{


  // ----- Property ID constants ---------------------------------------------

  // Components
  protected static final String COMPONENT_CLUSTER_NAME_PROPERTY_ID    = PropertyHelper.getPropertyId("ServiceComponentInfo", "cluster_name");
  protected static final String COMPONENT_SERVICE_NAME_PROPERTY_ID    = PropertyHelper.getPropertyId("ServiceComponentInfo", "service_name");
  protected static final String COMPONENT_COMPONENT_NAME_PROPERTY_ID  = PropertyHelper.getPropertyId("ServiceComponentInfo", "component_name");
  protected static final String COMPONENT_STATE_PROPERTY_ID           = PropertyHelper.getPropertyId("ServiceComponentInfo", "state");
  protected static final String COMPONENT_DESIRED_CONFIGS_PROPERTY_ID = PropertyHelper.getPropertyId("ServiceComponentInfo", "desired_configs");


  private static Set<String> pkPropertyIds =
      new HashSet<String>(Arrays.asList(new String[]{
          COMPONENT_CLUSTER_NAME_PROPERTY_ID,
          COMPONENT_SERVICE_NAME_PROPERTY_ID,
          COMPONENT_COMPONENT_NAME_PROPERTY_ID}));

  // ----- Constructors ----------------------------------------------------

  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param propertyIds           the property ids
   * @param keyPropertyIds        the key property ids
   * @param managementController  the management controller
   */
  ComponentResourceProvider(Set<String> propertyIds,
                            Map<Resource.Type, String> keyPropertyIds,
                            AmbariManagementController managementController) {
    super(propertyIds, keyPropertyIds, managementController);
  }

  // ----- ResourceProvider ------------------------------------------------

  @Override
  public RequestStatus createResources(Request request)
      throws SystemException,
             UnsupportedPropertyException,
             ResourceAlreadyExistsException,
             NoSuchParentResourceException {

    final Set<ServiceComponentRequest> requests = new HashSet<ServiceComponentRequest>();
    for (Map<String, Object> propertyMap : request.getProperties()) {
      requests.add(getRequest(propertyMap));
    }

    createResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        getManagementController().createComponents(requests);
        return null;
      }
    });

    notifyCreate(Resource.Type.Component, request);

    return getRequestStatus(null);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<ServiceComponentRequest> requests = new HashSet<ServiceComponentRequest>();

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      requests.add(getRequest(propertyMap));
    }

    Set<ServiceComponentResponse> responses = getResources(new Command<Set<ServiceComponentResponse>>() {
      @Override
      public Set<ServiceComponentResponse> invoke() throws AmbariException {
        return getManagementController().getComponents(requests);
      }
    });

    Set<String>   requestedIds = PropertyHelper.getRequestPropertyIds(getPropertyIds(), request, predicate);
    Set<Resource> resources    = new HashSet<Resource>();

    for (ServiceComponentResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.Component);
      setResourceProperty(resource, COMPONENT_CLUSTER_NAME_PROPERTY_ID, response.getClusterName(), requestedIds);
      setResourceProperty(resource, COMPONENT_SERVICE_NAME_PROPERTY_ID, response.getServiceName(), requestedIds);
      setResourceProperty(resource, COMPONENT_COMPONENT_NAME_PROPERTY_ID, response.getComponentName(), requestedIds);
      setResourceProperty(resource, COMPONENT_STATE_PROPERTY_ID,
          response.getDesiredState(), requestedIds);
      setResourceProperty(resource, COMPONENT_DESIRED_CONFIGS_PROPERTY_ID,
          response.getConfigVersions(), requestedIds);
      resources.add(resource);
    }
    return resources;
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<ServiceComponentRequest> requests = new HashSet<ServiceComponentRequest>();
    for (Map<String, Object> propertyMap : getPropertyMaps(request.getProperties().iterator().next(), predicate)) {
      ServiceComponentRequest compRequest = getRequest(propertyMap);
      Map<String, String>     configMap   = new HashMap<String,String>();

      for (Map.Entry<String,Object> entry : propertyMap.entrySet()) {
        if (PropertyHelper.getPropertyCategory(entry.getKey()).equals("config")) {
          configMap.put(PropertyHelper.getPropertyName(entry.getKey()), (String) entry.getValue());
        }
      }

      if (0 != configMap.size())
        compRequest.setConfigVersions(configMap);

      requests.add(compRequest);
    }

    RequestStatusResponse response = modifyResources(new Command<RequestStatusResponse>() {
      @Override
      public RequestStatusResponse invoke() throws AmbariException {
        return getManagementController().updateComponents(requests);
      }
    });

    notifyUpdate(Resource.Type.Component, request, predicate);

    return getRequestStatus(response);
  }

  @Override
  public RequestStatus deleteResources(Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<ServiceComponentRequest> requests = new HashSet<ServiceComponentRequest>();
    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      requests.add(getRequest(propertyMap));
    }

    RequestStatusResponse response = modifyResources(new Command<RequestStatusResponse>() {
      @Override
      public RequestStatusResponse invoke() throws AmbariException {
        return getManagementController().deleteComponents(requests);
      }
    });

    notifyDelete(Resource.Type.Component, predicate);
    return getRequestStatus(response);
  }

  // ----- utility methods -------------------------------------------------

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

  /**
   * Get a component request object from a map of property values.
   *
   * @param properties  the predicate
   *
   * @return the component request object
   */
  private ServiceComponentRequest getRequest(Map<String, Object> properties) {
    return new ServiceComponentRequest(
        (String) properties.get(COMPONENT_CLUSTER_NAME_PROPERTY_ID),
        (String) properties.get(COMPONENT_SERVICE_NAME_PROPERTY_ID),
        (String) properties.get(COMPONENT_COMPONENT_NAME_PROPERTY_ID),
        null,
        (String) properties.get(COMPONENT_STATE_PROPERTY_ID));
  }
}
