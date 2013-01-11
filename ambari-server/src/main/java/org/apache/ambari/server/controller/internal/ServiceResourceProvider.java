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
import org.apache.ambari.server.controller.ServiceRequest;
import org.apache.ambari.server.controller.ServiceResponse;
import org.apache.ambari.server.controller.spi.*;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Resource provider for service resources.
 */
class ServiceResourceProvider extends ResourceProviderImpl {


  // ----- Property ID constants ---------------------------------------------

  // Services
  protected static final String SERVICE_CLUSTER_NAME_PROPERTY_ID    = PropertyHelper.getPropertyId("ServiceInfo", "cluster_name");
  protected static final String SERVICE_SERVICE_NAME_PROPERTY_ID    = PropertyHelper.getPropertyId("ServiceInfo", "service_name");
  protected static final String SERVICE_SERVICE_STATE_PROPERTY_ID   = PropertyHelper.getPropertyId("ServiceInfo", "state");
  protected static final String SERVICE_DESIRED_CONFIGS_PROPERTY_ID = PropertyHelper.getPropertyId("ServiceInfo", "desired_configs");


  private static Set<String> pkPropertyIds =
      new HashSet<String>(Arrays.asList(new String[]{
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
  ServiceResourceProvider(Set<String> propertyIds,
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

    final Set<ServiceRequest> requests = new HashSet<ServiceRequest>();
    for (Map<String, Object> propertyMap : request.getProperties()) {
      requests.add(getRequest(propertyMap));
    }
    createResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        getManagementController().createServices(requests);
        return null;
      }
    });
    notifyCreate(Resource.Type.Service, request);

    return getRequestStatus(null);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate) throws
      SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<ServiceRequest> requests = new HashSet<ServiceRequest>();

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      requests.add(getRequest(propertyMap));
    }

    Set<ServiceResponse> responses    =  getResources(new Command<Set<ServiceResponse>>() {
      @Override
      public Set<ServiceResponse> invoke() throws AmbariException {
        return getManagementController().getServices(requests);
      }
    });

    Set<String>   requestedIds = PropertyHelper.getRequestPropertyIds(getPropertyIds(), request, predicate);
    Set<Resource> resources    = new HashSet<Resource>();

    for (ServiceResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.Service);
      setResourceProperty(resource, SERVICE_CLUSTER_NAME_PROPERTY_ID,
          response.getClusterName(), requestedIds);
      setResourceProperty(resource, SERVICE_SERVICE_NAME_PROPERTY_ID,
          response.getServiceName(), requestedIds);
      setResourceProperty(resource, SERVICE_DESIRED_CONFIGS_PROPERTY_ID,
          response.getConfigVersions(), requestedIds);
      setResourceProperty(resource, SERVICE_SERVICE_STATE_PROPERTY_ID,
          response.getDesiredState(), requestedIds);
      resources.add(resource);
    }
    return resources;
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<ServiceRequest> requests = new HashSet<ServiceRequest>();
    RequestStatusResponse     response = null;

    Iterator<Map<String,Object>> iterator = request.getProperties().iterator();
    if (iterator.hasNext()) {
      for (Map<String, Object> propertyMap : getPropertyMaps(iterator.next(), predicate)) {
        requests.add(getRequest(propertyMap));
      }
      response = modifyResources(new Command<RequestStatusResponse>() {
        @Override
        public RequestStatusResponse invoke() throws AmbariException {
          return getManagementController().updateServices(requests);
        }
      });
    }
    notifyUpdate(Resource.Type.Service, request, predicate);

    return getRequestStatus(response);
  }

  @Override
  public RequestStatus deleteResources(Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<ServiceRequest> requests = new HashSet<ServiceRequest>();
    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      requests.add(getRequest(propertyMap));
    }
    RequestStatusResponse response = modifyResources(new Command<RequestStatusResponse>() {
      @Override
      public RequestStatusResponse invoke() throws AmbariException {
        return getManagementController().deleteServices(requests);
      }
    });

    notifyDelete(Resource.Type.Service, predicate);
    return getRequestStatus(response);
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
      if (propertyCategory == null || !propertyCategory.equals("config")) {
        unsupportedProperties.add(propertyId);
      }
    }
    return unsupportedProperties;
  }


// ----- utility methods -------------------------------------------------

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

  /**
   * Get a service request object from a map of property values.
   *
   * @param properties  the predicate
   *
   * @return the service request object
   */
  private ServiceRequest getRequest(Map<String, Object> properties) {
    ServiceRequest svcRequest = new ServiceRequest(
        (String) properties.get(SERVICE_CLUSTER_NAME_PROPERTY_ID),
        (String) properties.get(SERVICE_SERVICE_NAME_PROPERTY_ID),
        null,
        (String) properties.get(SERVICE_SERVICE_STATE_PROPERTY_ID));

    Map<String, String> configMappings =
        ConfigurationResourceProvider.getConfigPropertyValues(properties);

    if (configMappings.size() > 0) {
      svcRequest.setConfigVersions(configMappings);
    }
    return svcRequest;
  }
}
