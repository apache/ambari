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
import org.apache.ambari.server.controller.ServiceRequest;
import org.apache.ambari.server.controller.ServiceResponse;
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
 * Resource provider for service resources.
 */
class ServiceResourceProvider extends ResourceProviderImpl{

  // ----- Property ID constants ---------------------------------------------

  // Services
  protected static final PropertyId SERVICE_CLUSTER_NAME_PROPERTY_ID  = PropertyHelper.getPropertyId("cluster_name", "ServiceInfo");
  protected static final PropertyId SERVICE_SERVICE_NAME_PROPERTY_ID  = PropertyHelper.getPropertyId("service_name", "ServiceInfo");
  protected static final PropertyId SERVICE_SERVICE_STATE_PROPERTY_ID = PropertyHelper.getPropertyId("state", "ServiceInfo");


  private static Set<PropertyId> pkPropertyIds =
      new HashSet<PropertyId>(Arrays.asList(new PropertyId[]{
          SERVICE_CLUSTER_NAME_PROPERTY_ID,
          SERVICE_SERVICE_NAME_PROPERTY_ID}));

  // ----- Constructors ----------------------------------------------------

  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param propertyIds           the property ids
   * @param keyPropertyIds        the key property ids
   * @param managementController  the management controller
   */
  ServiceResourceProvider(Set<PropertyId> propertyIds,
                          Map<Resource.Type, PropertyId> keyPropertyIds,
                          AmbariManagementController managementController) {
    super(propertyIds, keyPropertyIds, managementController);
  }

  // ----- ResourceProvider ------------------------------------------------

  @Override
  public RequestStatus createResources(Request request) throws AmbariException {
    Set<ServiceRequest> requests = new HashSet<ServiceRequest>();
    for (Map<PropertyId, Object> propertyMap : request.getProperties()) {
      requests.add(getRequest(propertyMap));
    }
    getManagementController().createServices(requests);
    return getRequestStatus(null);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate) throws AmbariException {
    Set<PropertyId> requestedIds = PropertyHelper.getRequestPropertyIds(getPropertyIds(), request, predicate);
    ServiceRequest  serviceRequest = getRequest(getProperties(predicate));

    // TODO : handle multiple requests
    Set<ServiceResponse> responses = getManagementController().getServices(Collections.singleton(serviceRequest));

    Set<Resource> resources = new HashSet<Resource>();
    for (ServiceResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.Service);
        setResourceProperty(resource, SERVICE_CLUSTER_NAME_PROPERTY_ID, response.getClusterName(), requestedIds);
        setResourceProperty(resource, SERVICE_SERVICE_NAME_PROPERTY_ID, response.getServiceName(), requestedIds);
      resources.add(resource);
    }
    return resources;
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate) throws AmbariException {
    Set<ServiceRequest> requests = new HashSet<ServiceRequest>();
    for (Map<PropertyId, Object> propertyMap : getPropertyMaps(request.getProperties().iterator().next(), predicate)) {

      Map<String, String> configMappings = new HashMap<String, String>();

      for (PropertyId id : propertyMap.keySet()) {
        if (id.getCategory().equals ("config")) {
          configMappings.put(id.getName(), (String) propertyMap.get(id));
        }
      }

      ServiceRequest svcRequest = getRequest(propertyMap);
      if (configMappings.size() > 0)
        svcRequest.setConfigVersions(configMappings);

      requests.add(svcRequest);
    }
    return getRequestStatus(getManagementController().updateServices(requests));
  }

  @Override
  public RequestStatus deleteResources(Predicate predicate) throws AmbariException {
    Set<ServiceRequest> requests = new HashSet<ServiceRequest>();
    for (Map<PropertyId, Object> propertyMap : getPropertyMaps(null, predicate)) {
      requests.add(getRequest(propertyMap));
    }
    return getRequestStatus(getManagementController().deleteServices(requests));
  }

  // ----- utility methods -------------------------------------------------

  @Override
  protected Set<PropertyId> getPKPropertyIds() {
    return pkPropertyIds;
  }

  /**
   * Get a service request object from a map of property values.
   *
   * @param properties  the predicate
   *
   * @return the service request object
   */
  private ServiceRequest getRequest(Map<PropertyId, Object> properties) {
    return new ServiceRequest(
        (String) properties.get(SERVICE_CLUSTER_NAME_PROPERTY_ID),
        (String) properties.get(SERVICE_SERVICE_NAME_PROPERTY_ID),
        null,
        (String) properties.get(SERVICE_SERVICE_STATE_PROPERTY_ID));
  }
}
