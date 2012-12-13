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
import org.apache.ambari.server.controller.ServiceComponentHostRequest;
import org.apache.ambari.server.controller.ServiceComponentHostResponse;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

import java.util.Arrays;
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
  protected static final String HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID   = PropertyHelper.getPropertyId("HostRoles", "cluster_name");
  protected static final String HOST_COMPONENT_SERVICE_NAME_PROPERTY_ID   = PropertyHelper.getPropertyId("HostRoles", "service_name");
  protected static final String HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "component_name");
  protected static final String HOST_COMPONENT_HOST_NAME_PROPERTY_ID      = PropertyHelper.getPropertyId("HostRoles", "host_name");
  protected static final String HOST_COMPONENT_STATE_PROPERTY_ID          = PropertyHelper.getPropertyId("HostRoles", "state");
  protected static final String HOST_COMPONENT_DESIRED_STATE_PROPERTY_ID  = PropertyHelper.getPropertyId("HostRoles", "desired_state");
  protected static final String HOST_COMPONENT_CONFIGS_PROPERTY_ID          = PropertyHelper.getPropertyId("HostRoles", "configs");
  protected static final String HOST_COMPONENT_DESIRED_CONFIGS_PROPERTY_ID  = PropertyHelper.getPropertyId("HostRoles", "desired_configs");

  private static Set<String> pkPropertyIds =
      new HashSet<String>(Arrays.asList(new String[]{
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
  HostComponentResourceProvider(Set<String> propertyIds,
                                Map<Resource.Type, String> keyPropertyIds,
                                AmbariManagementController managementController) {
    super(propertyIds, keyPropertyIds, managementController);
  }

  // ----- ResourceProvider ------------------------------------------------

  @Override
  public RequestStatus createResources(Request request) throws AmbariException, UnsupportedPropertyException {
    checkRequestProperties(Resource.Type.HostComponent, request);
    Set<ServiceComponentHostRequest> requests = new HashSet<ServiceComponentHostRequest>();
    for (Map<String, Object> propertyMap : request.getProperties()) {
      requests.add(getRequest(propertyMap));
    }
    getManagementController().createHostComponents(requests);
    notifyCreate(Resource.Type.HostComponent, request);

    return getRequestStatus(null);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate) throws AmbariException, UnsupportedPropertyException {
    Set<ServiceComponentHostRequest> requests = new HashSet<ServiceComponentHostRequest>();

    for (Map<String, Object> propertyMap : getPropertyMaps(null, predicate)) {
      requests.add(getRequest(propertyMap));
    }

    Set<String>                   requestedIds = PropertyHelper.getRequestPropertyIds(getPropertyIds(), request, predicate);
    Set<ServiceComponentHostResponse> responses     = getManagementController().getHostComponents(requests);
    Set<Resource>                     resources    = new HashSet<Resource>();
    for (ServiceComponentHostResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.HostComponent);
      setResourceProperty(resource, HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID, response.getClusterName(), requestedIds);
      setResourceProperty(resource, HOST_COMPONENT_SERVICE_NAME_PROPERTY_ID, response.getServiceName(), requestedIds);
      setResourceProperty(resource, HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, response.getComponentName(), requestedIds);
      setResourceProperty(resource, HOST_COMPONENT_HOST_NAME_PROPERTY_ID, response.getHostname(), requestedIds);
      setResourceProperty(resource, HOST_COMPONENT_STATE_PROPERTY_ID, response.getLiveState(), requestedIds);
      setResourceProperty(resource, HOST_COMPONENT_DESIRED_STATE_PROPERTY_ID, response.getDesiredState(), requestedIds);
      setResourceProperty(resource, HOST_COMPONENT_CONFIGS_PROPERTY_ID,
          response.getConfigs(), requestedIds);
      setResourceProperty(resource, HOST_COMPONENT_DESIRED_CONFIGS_PROPERTY_ID,
          response.getDesiredConfigs(), requestedIds);
      resources.add(resource);
    }
    return resources;
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate) throws AmbariException, UnsupportedPropertyException {
    checkRequestProperties(Resource.Type.HostComponent, request);
    Set<ServiceComponentHostRequest> requests = new HashSet<ServiceComponentHostRequest>();
    for (Map<String, Object> propertyMap : getPropertyMaps(request.getProperties().iterator().next(), predicate)) {

      ServiceComponentHostRequest hostCompRequest = getRequest(propertyMap);

      Map<String, String> configMap = new HashMap<String,String>();

      ConfigurationResourceProvider.getConfigPropertyValues(propertyMap, configMap);

      if (0 != configMap.size()) {
        hostCompRequest.setConfigVersions(configMap);
      }

      requests.add(hostCompRequest);
    }
    RequestStatusResponse response = getManagementController().updateHostComponents(requests);
    notifyUpdate(Resource.Type.HostComponent, request, predicate);

    return getRequestStatus(response);
  }

  @Override
  public RequestStatus deleteResources(Predicate predicate) throws AmbariException, UnsupportedPropertyException {
    Set<ServiceComponentHostRequest> requests = new HashSet<ServiceComponentHostRequest>();
    for (Map<String, Object> propertyMap : getPropertyMaps(null, predicate)) {
      requests.add(getRequest(propertyMap));
    }
    RequestStatusResponse response = getManagementController().deleteHostComponents(requests);
    notifyDelete(Resource.Type.HostComponent, predicate);

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
  private ServiceComponentHostRequest getRequest(Map<String, Object> properties) {
    return new ServiceComponentHostRequest(
        (String) properties.get(HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID),
        (String) properties.get(HOST_COMPONENT_SERVICE_NAME_PROPERTY_ID),
        (String) properties.get(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID),
        (String) properties.get(HOST_COMPONENT_HOST_NAME_PROPERTY_ID),
        null,
        (String) properties.get(HOST_COMPONENT_STATE_PROPERTY_ID));
  }
}
