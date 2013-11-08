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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.ServiceComponentHostRequest;
import org.apache.ambari.server.controller.ServiceComponentHostResponse;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.PropertyProvider;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

/**
 * Resource provider for host component resources.
 */
class HostComponentResourceProvider extends AbstractControllerResourceProvider {

  // ----- Property ID constants ---------------------------------------------

  // Host Components
  protected static final String HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID
      = PropertyHelper.getPropertyId("HostRoles", "cluster_name");
  protected static final String HOST_COMPONENT_SERVICE_NAME_PROPERTY_ID
      = PropertyHelper.getPropertyId("HostRoles", "service_name");
  protected static final String HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID
      = PropertyHelper.getPropertyId("HostRoles", "component_name");
  protected static final String HOST_COMPONENT_HOST_NAME_PROPERTY_ID
      = PropertyHelper.getPropertyId("HostRoles", "host_name");
  protected static final String HOST_COMPONENT_STATE_PROPERTY_ID
      = PropertyHelper.getPropertyId("HostRoles", "state");
  protected static final String HOST_COMPONENT_DESIRED_STATE_PROPERTY_ID
      = PropertyHelper.getPropertyId("HostRoles", "desired_state");
  protected static final String HOST_COMPONENT_STACK_ID_PROPERTY_ID
      = PropertyHelper.getPropertyId("HostRoles", "stack_id");
  protected static final String HOST_COMPONENT_DESIRED_STACK_ID_PROPERTY_ID
      = PropertyHelper.getPropertyId("HostRoles", "desired_stack_id");
  protected static final String HOST_COMPONENT_ACTUAL_CONFIGS_PROPERTY_ID
    = PropertyHelper.getPropertyId("HostRoles", "actual_configs");
  protected static final String HOST_COMPONENT_STALE_CONFIGS_PROPERTY_ID
    = PropertyHelper.getPropertyId("HostRoles", "stale_configs");
  
  //Component name mappings
  private static final Map<String, PropertyProvider> HOST_COMPONENT_PROPERTIES_PROVIDER = new HashMap<String, PropertyProvider>();

  private static final int HOST_COMPONENT_HTTP_PROPERTY_REQUEST_CONNECT_TIMEOUT = 1500;
  private static final int HOST_COMPONENT_HTTP_PROPERTY_REQUEST_READ_TIMEOUT    = 10000;

  static {
    ComponentSSLConfiguration configuration = ComponentSSLConfiguration.instance();
    URLStreamProvider streamProvider = new URLStreamProvider(
        HOST_COMPONENT_HTTP_PROPERTY_REQUEST_CONNECT_TIMEOUT,
        HOST_COMPONENT_HTTP_PROPERTY_REQUEST_READ_TIMEOUT,
        configuration.getTruststorePath(), configuration.getTruststorePassword(), configuration.getTruststoreType());

    HOST_COMPONENT_PROPERTIES_PROVIDER.put(
        "NAGIOS_SERVER",
        new HttpProxyPropertyProvider(
            streamProvider, configuration,
            PropertyHelper.getPropertyId("HostRoles", "cluster_name"),
            PropertyHelper.getPropertyId("HostRoles", "host_name"),
            PropertyHelper.getPropertyId("HostRoles", "component_name")));
  }

  //Parameters from the predicate
  private static final String QUERY_PARAMETERS_RUN_SMOKE_TEST_ID =
      "params/run_smoke_test";

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
  public RequestStatus createResources(Request request)
      throws SystemException,
             UnsupportedPropertyException,
             ResourceAlreadyExistsException,
             NoSuchParentResourceException {

    final Set<ServiceComponentHostRequest> requests = new HashSet<ServiceComponentHostRequest>();
    for (Map<String, Object> propertyMap : request.getProperties()) {
      requests.add(getRequest(propertyMap));
    }

    createResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        getManagementController().createHostComponents(requests);
        return null;
      }
    });

    notifyCreate(Resource.Type.HostComponent, request);

    return getRequestStatus(null);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<ServiceComponentHostRequest> requests = new HashSet<ServiceComponentHostRequest>();

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      requests.add(getRequest(propertyMap));
    }

    Set<Resource> resources    = new HashSet<Resource>();
    Set<String>   requestedIds = getRequestPropertyIds(request, predicate);

    Set<ServiceComponentHostResponse> responses = getResources(new Command<Set<ServiceComponentHostResponse>>() {
      @Override
      public Set<ServiceComponentHostResponse> invoke() throws AmbariException {
        return getManagementController().getHostComponents(requests);
      }
    });
    
    for (ServiceComponentHostResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.HostComponent);
      setResourceProperty(resource, HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID, response.getClusterName(), requestedIds);
      setResourceProperty(resource, HOST_COMPONENT_SERVICE_NAME_PROPERTY_ID, response.getServiceName(), requestedIds);
      setResourceProperty(resource, HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, response.getComponentName(), requestedIds);
      setResourceProperty(resource, HOST_COMPONENT_HOST_NAME_PROPERTY_ID, response.getHostname(), requestedIds);
      setResourceProperty(resource, HOST_COMPONENT_STATE_PROPERTY_ID, response.getLiveState(), requestedIds);
      setResourceProperty(resource, HOST_COMPONENT_DESIRED_STATE_PROPERTY_ID, response.getDesiredState(), requestedIds);
      setResourceProperty(resource, HOST_COMPONENT_STACK_ID_PROPERTY_ID,
          response.getStackVersion(), requestedIds);
      setResourceProperty(resource, HOST_COMPONENT_DESIRED_STACK_ID_PROPERTY_ID,
          response.getDesiredStackVersion(), requestedIds);
      setResourceProperty(resource, HOST_COMPONENT_ACTUAL_CONFIGS_PROPERTY_ID,
          response.getActualConfigs(), requestedIds);
      setResourceProperty(resource, HOST_COMPONENT_STALE_CONFIGS_PROPERTY_ID,
          Boolean.valueOf(response.isStaleConfig()), requestedIds);
      
      String componentName = (String)resource.getPropertyValue(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID);
      PropertyProvider propertyProvider = HOST_COMPONENT_PROPERTIES_PROVIDER.get(componentName);
      if (propertyProvider != null) {
        Set<Resource> resourcesToPopulate = new HashSet<Resource>();
        resourcesToPopulate.add(resource);
        propertyProvider.populateResources(resourcesToPopulate, request, predicate);
      }
      
      resources.add(resource);
    }
    return resources;
  }

  @Override
  public RequestStatus updateResources(final Request request, Predicate predicate)
        throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    final Set<ServiceComponentHostRequest> requests = new HashSet<ServiceComponentHostRequest>();
    RequestStatusResponse response = null;

    final boolean runSmokeTest = "true".equals(getQueryParameterValue(
        QUERY_PARAMETERS_RUN_SMOKE_TEST_ID, predicate)) ? true : false;

    Iterator<Map<String,Object>> iterator = request.getProperties().iterator();
    if (iterator.hasNext()) {
      for (Map<String, Object> propertyMap : getPropertyMaps(request.getProperties().iterator().next(), predicate)) {
        requests.add(getRequest(propertyMap));
      }
      response = modifyResources(new Command<RequestStatusResponse>() {
        @Override
        public RequestStatusResponse invoke() throws AmbariException {
          return getManagementController().updateHostComponents(requests, request.getRequestInfoProperties(), runSmokeTest);
        }
      });

      notifyUpdate(Resource.Type.HostComponent, request, predicate);
    }
    return getRequestStatus(response);
  }

  @Override
  public RequestStatus deleteResources(Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    final Set<ServiceComponentHostRequest> requests = new HashSet<ServiceComponentHostRequest>();
    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      requests.add(getRequest(propertyMap));
    }
    RequestStatusResponse response = modifyResources(new Command<RequestStatusResponse>() {
      @Override
      public RequestStatusResponse invoke() throws AmbariException {
        return getManagementController().deleteHostComponents(requests);
      }
    });

    notifyDelete(Resource.Type.HostComponent, predicate);

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
      if (!propertyId.equals("config")) {
        String propertyCategory = PropertyHelper.getPropertyCategory(propertyId);
        if (propertyCategory == null || !propertyCategory.equals("config")) {
          unsupportedProperties.add(propertyId);
        }
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
   * Get a component request object from a map of property values.
   *
   * @param properties  the predicate
   *
   * @return the component request object
   */
  private ServiceComponentHostRequest getRequest(Map<String, Object> properties) {
    ServiceComponentHostRequest serviceComponentHostRequest = new ServiceComponentHostRequest(
        (String) properties.get(HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID),
        (String) properties.get(HOST_COMPONENT_SERVICE_NAME_PROPERTY_ID),
        (String) properties.get(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID),
        (String) properties.get(HOST_COMPONENT_HOST_NAME_PROPERTY_ID),
        (String) properties.get(HOST_COMPONENT_STATE_PROPERTY_ID));
    serviceComponentHostRequest.setDesiredStackId(
        (String) properties.get(HOST_COMPONENT_STACK_ID_PROPERTY_ID));

    return serviceComponentHostRequest;
  }
}
