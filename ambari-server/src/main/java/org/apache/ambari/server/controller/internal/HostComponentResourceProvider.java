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
import org.apache.ambari.server.controller.ServiceComponentHostRequest;
import org.apache.ambari.server.controller.ServiceComponentHostResponse;
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
 * Resource provider for host component resources.
 */
class HostComponentResourceProvider extends ResourceProviderImpl{

  // ----- Property ID constants ---------------------------------------------

  // Host Components
  protected static final PropertyId HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID   = PropertyHelper.getPropertyId("cluster_name", "HostRoles");
  protected static final PropertyId HOST_COMPONENT_SERVICE_NAME_PROPERTY_ID   = PropertyHelper.getPropertyId("service_name", "HostRoles");
  protected static final PropertyId HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("component_name", "HostRoles");
  protected static final PropertyId HOST_COMPONENT_HOST_NAME_PROPERTY_ID      = PropertyHelper.getPropertyId("host_name", "HostRoles");
  protected static final PropertyId HOST_COMPONENT_STATE_PROPERTY_ID          = PropertyHelper.getPropertyId("state", "HostRoles");

  private static Set<PropertyId> pkPropertyIds =
      new HashSet<PropertyId>(Arrays.asList(new PropertyId[]{
          HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID,
          HOST_COMPONENT_SERVICE_NAME_PROPERTY_ID,
          HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID,
          HOST_COMPONENT_HOST_NAME_PROPERTY_ID}));

  // ----- Constructors ----------------------------------------------------

  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param propertyIds           the property ids
   * @param keyPropertyIds        the key property ids
   * @param managementController  the management controller
   */
  HostComponentResourceProvider(Set<PropertyId> propertyIds,
                                Map<Resource.Type, PropertyId> keyPropertyIds,
                                AmbariManagementController managementController) {
    super(propertyIds, keyPropertyIds, managementController);
  }

  // ----- ResourceProvider ------------------------------------------------

  @Override
  public RequestStatus createResources(Request request) throws AmbariException {
    Set<ServiceComponentHostRequest> requests = new HashSet<ServiceComponentHostRequest>();
    for (Map<PropertyId, Object> propertyMap : request.getProperties()) {
      requests.add(getRequest(propertyMap));
    }
    getManagementController().createHostComponents(requests);
    return getRequestStatus(null);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate) throws AmbariException {
    Set<PropertyId> requestedIds = PropertyHelper.getRequestPropertyIds(getPropertyIds(), request, predicate);
    ServiceComponentHostRequest hostComponentRequest = getRequest(getProperties(predicate));

    // TODO : handle multiple requests
    Set<ServiceComponentHostResponse> responses = getManagementController().getHostComponents(Collections.singleton(hostComponentRequest));

    Set<Resource> resources = new HashSet<Resource>();
    for (ServiceComponentHostResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.HostComponent);
      setResourceProperty(resource, HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID, response.getClusterName(), requestedIds);
      setResourceProperty(resource, HOST_COMPONENT_SERVICE_NAME_PROPERTY_ID, response.getServiceName(), requestedIds);
      setResourceProperty(resource, HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, response.getComponentName(), requestedIds);
      setResourceProperty(resource, HOST_COMPONENT_HOST_NAME_PROPERTY_ID, response.getHostname(), requestedIds);
      setResourceProperty(resource, HOST_COMPONENT_STATE_PROPERTY_ID, response.getLiveState(), requestedIds);
      resources.add(resource);
    }
    return resources;
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate) throws AmbariException {
    Set<ServiceComponentHostRequest> requests = new HashSet<ServiceComponentHostRequest>();
    for (Map<PropertyId, Object> propertyMap : getPropertyMaps(request.getProperties().iterator().next(), predicate)) {

      ServiceComponentHostRequest hostCompRequest = getRequest(propertyMap);

      Map<String, String> configMap = new HashMap<String,String>();

      for (Map.Entry<PropertyId,Object> entry : propertyMap.entrySet()) {
        if (entry.getKey().getCategory().equals("config")) {
          configMap.put(entry.getKey().getName(), (String) entry.getValue());
        }
      }

      if (0 != configMap.size())
        hostCompRequest.setConfigVersions(configMap);

      requests.add(hostCompRequest);
    }
    return getRequestStatus(getManagementController().updateHostComponents(requests));
  }

  @Override
  public RequestStatus deleteResources(Predicate predicate) throws AmbariException {
    Set<ServiceComponentHostRequest> requests = new HashSet<ServiceComponentHostRequest>();
    for (Map<PropertyId, Object> propertyMap : getPropertyMaps(null, predicate)) {
      requests.add(getRequest(propertyMap));
    }
    return getRequestStatus(getManagementController().deleteHostComponents(requests));
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
  private ServiceComponentHostRequest getRequest(Map<PropertyId, Object> properties) {
    return new ServiceComponentHostRequest(
        (String) properties.get(HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID),
        (String) properties.get(HOST_COMPONENT_SERVICE_NAME_PROPERTY_ID),
        (String) properties.get(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID),
        (String) properties.get(HOST_COMPONENT_HOST_NAME_PROPERTY_ID),
        null,
        (String) properties.get(HOST_COMPONENT_STATE_PROPERTY_ID));
  }
}
