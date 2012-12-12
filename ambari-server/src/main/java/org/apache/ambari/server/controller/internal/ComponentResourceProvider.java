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
import org.apache.ambari.server.controller.ServiceComponentRequest;
import org.apache.ambari.server.controller.ServiceComponentResponse;
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
import java.util.Map;
import java.util.Set;

/**
 * Resource provider for component resources.
 */
class ComponentResourceProvider extends ResourceProviderImpl{

  // ----- Property ID constants ---------------------------------------------

  // Components
  protected static final PropertyId COMPONENT_CLUSTER_NAME_PROPERTY_ID   = PropertyHelper.getPropertyId("cluster_name", "ServiceComponentInfo");
  protected static final PropertyId COMPONENT_SERVICE_NAME_PROPERTY_ID   = PropertyHelper.getPropertyId("service_name", "ServiceComponentInfo");
  protected static final PropertyId COMPONENT_COMPONENT_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("component_name", "ServiceComponentInfo");
  protected static final PropertyId COMPONENT_STATE_PROPERTY_ID          = PropertyHelper.getPropertyId("state", "ServiceComponentInfo");


  private static Set<PropertyId> pkPropertyIds =
      new HashSet<PropertyId>(Arrays.asList(new PropertyId[]{
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
  ComponentResourceProvider(Set<PropertyId> propertyIds,
                            Map<Resource.Type, PropertyId> keyPropertyIds,
                            AmbariManagementController managementController) {
    super(propertyIds, keyPropertyIds, managementController);
  }

  // ----- ResourceProvider ------------------------------------------------

  @Override
  public RequestStatus createResources(Request request) throws AmbariException {
    Set<ServiceComponentRequest> requests = new HashSet<ServiceComponentRequest>();
    for (Map<PropertyId, Object> propertyMap : request.getProperties()) {
      requests.add(getRequest(propertyMap));
    }
    getManagementController().createComponents(requests);
    return getRequestStatus(null);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate) throws AmbariException {
    Set<PropertyId> requestedIds = PropertyHelper.getRequestPropertyIds(getPropertyIds(), request, predicate);
    ServiceComponentRequest serviceComponentRequest = getRequest(getProperties(predicate));

    // TODO : handle multiple requests
    Set<ServiceComponentResponse> responses = getManagementController().getComponents(Collections.singleton(serviceComponentRequest));

    Set<Resource> resources = new HashSet<Resource>();
    for (ServiceComponentResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.Component);
//        setResourceProperty(resource, COMPONENT_CLUSTER_ID_PROPERTY_ID, response.getClusterId(), requestedIds);
      setResourceProperty(resource, COMPONENT_CLUSTER_NAME_PROPERTY_ID, response.getClusterName(), requestedIds);
      setResourceProperty(resource, COMPONENT_SERVICE_NAME_PROPERTY_ID, response.getServiceName(), requestedIds);
      setResourceProperty(resource, COMPONENT_COMPONENT_NAME_PROPERTY_ID, response.getComponentName(), requestedIds);
//        setResourceProperty(resource, COMPONENT_VERSION_PROPERTY_ID, response.getCurrentStackVersion(), requestedIds);
      resources.add(resource);
    }
    return resources;
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate) throws AmbariException {
    Set<ServiceComponentRequest> requests = new HashSet<ServiceComponentRequest>();
    for (Map<PropertyId, Object> propertyMap : getPropertyMaps(request.getProperties().iterator().next(), predicate)) {
      ServiceComponentRequest compRequest = getRequest(propertyMap);

      Map<String, String> configMap = new HashMap<String,String>();

      for (Map.Entry<PropertyId,Object> entry : propertyMap.entrySet()) {
        if (entry.getKey().getCategory().equals("config")) {
          configMap.put(entry.getKey().getName(), (String) entry.getValue());
        }
      }

      if (0 != configMap.size())
        compRequest.setConfigVersions(configMap);

      requests.add(compRequest);
    }
    return getRequestStatus(getManagementController().updateComponents(requests));
  }

  @Override
  public RequestStatus deleteResources(Predicate predicate) throws AmbariException {
    Set<ServiceComponentRequest> requests = new HashSet<ServiceComponentRequest>();
    for (Map<PropertyId, Object> propertyMap : getPropertyMaps(null, predicate)) {
      requests.add(getRequest(propertyMap));
    }
    return getRequestStatus(getManagementController().deleteComponents(requests));
  }

  // ----- utility methods -------------------------------------------------

  @Override
  protected Set<PropertyId> getPKPropertyIds() {
    return pkPropertyIds;
  }

  /**
   * Get a component request object from a map of property values.
   *
   * @param properties  the predicate
   *
   * @return the component request object
   */
  private ServiceComponentRequest getRequest(Map<PropertyId, Object> properties) {
    return new ServiceComponentRequest(
        (String) properties.get(COMPONENT_CLUSTER_NAME_PROPERTY_ID),
        (String) properties.get(COMPONENT_SERVICE_NAME_PROPERTY_ID),
        (String) properties.get(COMPONENT_COMPONENT_NAME_PROPERTY_ID),
        null,
        (String) properties.get(COMPONENT_STATE_PROPERTY_ID));
  }
}
