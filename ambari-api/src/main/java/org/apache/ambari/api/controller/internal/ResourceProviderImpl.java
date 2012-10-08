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

package org.apache.ambari.api.controller.internal;

import org.apache.ambari.api.controller.utilities.PredicateHelper;
import org.apache.ambari.api.controller.utilities.Properties;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ClusterRequest;
import org.apache.ambari.server.controller.ClusterResponse;
import org.apache.ambari.server.controller.HostRequest;
import org.apache.ambari.server.controller.HostResponse;
import org.apache.ambari.server.controller.ServiceComponentHostRequest;
import org.apache.ambari.server.controller.ServiceComponentHostResponse;
import org.apache.ambari.server.controller.ServiceComponentRequest;
import org.apache.ambari.server.controller.ServiceComponentResponse;
import org.apache.ambari.server.controller.ServiceRequest;
import org.apache.ambari.server.controller.ServiceResponse;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.PropertyId;
import org.apache.ambari.server.controller.spi.PropertyProvider;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.spi.Schema;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Basic resource provider implementation that maps to a management controller.
 */
public abstract class ResourceProviderImpl implements ResourceProvider {

  /**
   * The list of property providers for this provider's resource type.
   */
  private final List<PropertyProvider> propertyProviders;

  /**
   * The set of property ids supported by this resource provider.
   */
  private final Set<PropertyId> propertyIds;

  /**
   * The management controller to delegate to.
   */
  private final AmbariManagementController managementController;

  /**
   * The schema for this provider's resource type.
   */
  private final Schema schema;


  // ----- Property ID constants ---------------------------------------------

  // Clusters
  private static final PropertyId CLUSTER_ID_PROPERTY_ID      = Properties.getPropertyId("cluster_id", "Clusters");
  private static final PropertyId CLUSTER_NAME_PROPERTY_ID    = Properties.getPropertyId("cluster_name", "Clusters");
  private static final PropertyId CLUSTER_VERSION_PROPERTY_ID = Properties.getPropertyId("version", "Clusters");
  // Services
  private static final PropertyId SERVICE_CLUSTER_NAME_PROPERTY_ID  = Properties.getPropertyId("cluster_name", "ServiceInfo");
  private static final PropertyId SERVICE_SERVICE_NAME_PROPERTY_ID  = Properties.getPropertyId("service_name", "ServiceInfo");
  private static final PropertyId SERVICE_SERVICE_STATE_PROPERTY_ID = Properties.getPropertyId("state", "ServiceInfo");
  // Components
  private static final PropertyId COMPONENT_CLUSTER_NAME_PROPERTY_ID   = Properties.getPropertyId("cluster_name", "ServiceComponentInfo");
  private static final PropertyId COMPONENT_SERVICE_NAME_PROPERTY_ID   = Properties.getPropertyId("service_name", "ServiceComponentInfo");
  private static final PropertyId COMPONENT_COMPONENT_NAME_PROPERTY_ID = Properties.getPropertyId("service_name", "ServiceComponentInfo");
  private static final PropertyId COMPONENT_STATE_PROPERTY_ID          = Properties.getPropertyId("state", "ServiceComponentInfo");
  // Hosts
  private static final PropertyId HOST_CLUSTER_NAME_PROPERTY_ID = Properties.getPropertyId("cluster_name", "Hosts");
  private static final PropertyId HOST_NAME_PROPERTY_ID         = Properties.getPropertyId("host_name", "Hosts");
  private static final PropertyId HOST_IP_PROPERTY_ID           = Properties.getPropertyId("ip", "Hosts");
  private static final PropertyId HOST_TOTAL_MEM_PROPERTY_ID    = Properties.getPropertyId("total_mem", "Hosts");
  private static final PropertyId HOST_CPU_COUNT_PROPERTY_ID    = Properties.getPropertyId("cpu_count", "Hosts");
  private static final PropertyId HOST_OS_ARCH_PROPERTY_ID      = Properties.getPropertyId("os_arch", "Hosts");
  private static final PropertyId HOST_OS_TYPE_PROPERTY_ID      = Properties.getPropertyId("os_type", "Hosts");
  // Host Components
  private static final PropertyId HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID   = Properties.getPropertyId("cluster_name", "HostRoles");
  private static final PropertyId HOST_COMPONENT_SERVICE_NAME_PROPERTY_ID   = Properties.getPropertyId("service_name", "HostRoles");
  private static final PropertyId HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID = Properties.getPropertyId("service_name", "HostRoles");
  private static final PropertyId HOST_COMPONENT_HOST_NAME_PROPERTY_ID      = Properties.getPropertyId("host_name", "HostRoles");
  private static final PropertyId HOST_COMPONENT_STATE_PROPERTY_ID          = Properties.getPropertyId("state", "HostRoles");


  // ----- Constructors ------------------------------------------------------

  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param propertyIds           the property ids
   * @param managementController  the management controller
   */
  private ResourceProviderImpl(List<PropertyProvider> propertyProviders,
                               Set<PropertyId> propertyIds,
                               Map<Resource.Type, PropertyId> keyPropertyIds,
                               AmbariManagementController managementController) {
    this.propertyProviders    = propertyProviders;
    this.propertyIds          = propertyIds;
    this.managementController = managementController;
    this.schema               = new SchemaImpl(this, keyPropertyIds);
  }


  // ----- ResourceProvider --------------------------------------------------

  @Override
  public Set<PropertyId> getPropertyIds() {
    return propertyIds;
  }

  @Override
  public List<PropertyProvider> getPropertyProviders() {
    return propertyProviders;
  }

  @Override
  public Schema getSchema() {
    return schema;
  }


  // ----- accessors ---------------------------------------------------------

  /**
   * Get the associated management controller.
   *
   * @return the associated management controller
   */
  public AmbariManagementController getManagementController() {
    return managementController;
  }


  // ----- utility methods ---------------------------------------------------

  /**
   * Get a map of property values from a given predicate.
   *
   * @param predicate  the predicate
   *
   * @return the map of properties
   */
  private static Map<PropertyId, Object> getProperties(Predicate predicate) {
    PropertyPredicateVisitor visitor = new PropertyPredicateVisitor();
    PredicateHelper.visit(predicate, visitor);
    return visitor.getProperties();
  }

  /**
   * Transfer property values from one map to another for the given list of property ids.
   *
   * @param to           the target map
   * @param from         the source map
   * @param propertyIds  the list of property ids
   */
  private static void setProperties(Map<PropertyId, Object> to, Map<PropertyId, Object> from, PropertyId ... propertyIds) {
    for (PropertyId propertyId : propertyIds) {
      if (from.containsKey(propertyId)) {
        to.put(propertyId, from.get(propertyId));
      }
    }
  }

  /**
   * Set a string property value on the given resource for the given id and value.
   * Make sure that the id is in the given set of requested ids.
   *
   * @param resource      the resource
   * @param propertyId    the property id
   * @param value         the value to set
   * @param requestedIds  the requested set of property ids
   */
  private static void setResourceProperty(Resource resource, PropertyId propertyId, String value, Set<PropertyId> requestedIds) {
    if (requestedIds.contains(propertyId)) {
      resource.setProperty(propertyId, value);
    }
  }

  /**
   * Set a long property value on the given resource for the given id and value.
   * Make sure that the id is in the given set of requested ids.
   *
   * @param resource      the resource
   * @param propertyId    the property id
   * @param value         the value to set
   * @param requestedIds  the requested set of property ids
   */
  private static void setResourceProperty(Resource resource, PropertyId propertyId, Long value, Set<PropertyId> requestedIds) {
    if (requestedIds.contains(propertyId)) {
      resource.setProperty(propertyId, value);
    }
  }

  /**
   * Set a integer property value on the given resource for the given id and value.
   * Make sure that the id is in the given set of requested ids.
   *
   * @param resource      the resource
   * @param propertyId    the property id
   * @param value         the value to set
   * @param requestedIds  the requested set of property ids
   */
  private static void setResourceProperty(Resource resource, PropertyId propertyId, Integer value, Set<PropertyId> requestedIds) {
    if (requestedIds.contains(propertyId)) {
      resource.setProperty(propertyId, value);
    }
  }

  /**
   * Factory method for obtaining a resource provider based on a given type and management controller.
   *
   * @param type                  the resource type
   * @param propertyIds           the property ids
   * @param managementController  the management controller
   *
   * @return a new resource provider
   */
  public static ResourceProvider getResourceProvider(Resource.Type type,
                                                     List<PropertyProvider> propertyProviders,
                                                     Set<PropertyId> propertyIds,
                                                     Map<Resource.Type, PropertyId> keyPropertyIds,
                                                     AmbariManagementController managementController) {
    switch (type) {
      case Cluster:
        return new ClusterResourceProvider(propertyProviders, propertyIds, keyPropertyIds, managementController);
      case Service:
        return new ServiceResourceProvider(propertyProviders, propertyIds, keyPropertyIds, managementController);
      case Component:
        return new ComponentResourceProvider(propertyProviders, propertyIds, keyPropertyIds, managementController);
      case Host:
        return new HostResourceProvider(propertyProviders, propertyIds, keyPropertyIds, managementController);
      case HostComponent:
        return new HostComponentResourceProvider(propertyProviders, propertyIds, keyPropertyIds, managementController);
    }
    throw new IllegalArgumentException("Unknown type " + type);
  }


  // ------ ClusterResourceProvider inner class ------------------------------

  private static class ClusterResourceProvider extends ResourceProviderImpl{

    // ----- Constructors ----------------------------------------------------

    /**
     * Create a  new resource provider for the given management controller.
     *
     * @param propertyIds           the property ids
     * @param managementController  the management controller
     */
    private ClusterResourceProvider(List<PropertyProvider> propertyProviders, Set<PropertyId> propertyIds, Map<Resource.Type, PropertyId> keyPropertyIds, AmbariManagementController managementController) {
      super(propertyProviders, propertyIds, keyPropertyIds, managementController);
    }

// ----- ResourceProvider ------------------------------------------------

    @Override
    public void createResources(Request request) throws AmbariException {

      for (Map<PropertyId, Object> properties : request.getProperties()) {
        getManagementController().createCluster(getRequest(properties));
      }
    }

    @Override
    public Set<Resource> getResources(Request request, Predicate predicate) throws AmbariException {
      Set<PropertyId> requestedIds   = request.getPropertyIds();
      ClusterRequest  clusterRequest = getRequest(getProperties(predicate));

      Set<ClusterResponse> responses = getManagementController().getClusters(clusterRequest);

      Set<Resource> resources = new HashSet<Resource>();
      for (ClusterResponse response : responses) {
        Resource resource = new ResourceImpl(Resource.Type.Cluster);
        setResourceProperty(resource, CLUSTER_ID_PROPERTY_ID, response.getClusterId(), requestedIds);
        setResourceProperty(resource, CLUSTER_NAME_PROPERTY_ID, response.getClusterName(), requestedIds);
      }
      return resources;
    }

    @Override
    public void updateResources(Request request, Predicate predicate) throws AmbariException {
      // get the cluster request properties from the given request
      Map<PropertyId, Object> properties = request.getProperties().iterator().next();
      // get the id for the cluster request from the predicate
      setProperties(properties, getProperties(predicate), CLUSTER_ID_PROPERTY_ID);

      ClusterRequest clusterRequest = getRequest(properties);
      getManagementController().updateCluster(clusterRequest);
    }

    @Override
    public void deleteResources(Predicate predicate) throws AmbariException {
      ClusterRequest clusterRequest = getRequest(getProperties(predicate));
      getManagementController().deleteCluster(clusterRequest);
    }

    // ----- utility methods -------------------------------------------------

    /**
     * Get a cluster request object from a map of property values.
     *
     * @param properties  the predicate
     *
     * @return the cluster request object
     */
    private ClusterRequest getRequest(Map<PropertyId, Object> properties) {

      return new ClusterRequest(
          (Long) properties.get(CLUSTER_ID_PROPERTY_ID),
          (String) properties.get(CLUSTER_NAME_PROPERTY_ID),
          (String) properties.get(CLUSTER_VERSION_PROPERTY_ID),
          null);  // TODO : host names
    }
  }

  // ------ ServiceResourceProvider inner class ------------------------------

  private static class ServiceResourceProvider extends ResourceProviderImpl{

    // ----- Constructors ----------------------------------------------------

    /**
     * Create a  new resource provider for the given management controller.
     *
     * @param propertyIds           the property ids
     * @param managementController  the management controller
     */
    private ServiceResourceProvider(List<PropertyProvider> propertyProviders, Set<PropertyId> propertyIds, Map<Resource.Type, PropertyId> keyPropertyIds, AmbariManagementController managementController) {
      super(propertyProviders, propertyIds, keyPropertyIds, managementController);
    }

    // ----- ResourceProvider ------------------------------------------------

    @Override
    public void createResources(Request request) throws AmbariException {
      for (Map<PropertyId, Object> properties : request.getProperties()) {
        getManagementController().createService(getRequest(properties));
      }
    }

    @Override
    public Set<Resource> getResources(Request request, Predicate predicate) throws AmbariException {
      Set<PropertyId> requestedIds   = request.getPropertyIds();
      ServiceRequest  serviceRequest = getRequest(getProperties(predicate));

      Set<ServiceResponse> responses = getManagementController().getServices(serviceRequest);

      Set<Resource> resources = new HashSet<Resource>();
      for (ServiceResponse response : responses) {
        Resource resource = new ResourceImpl(Resource.Type.Service);
//        setResourceProperty(resource, SERVICE_CLUSTER_ID_PROPERTY_ID, response.getClusterId(), requestedIds);
        setResourceProperty(resource, SERVICE_CLUSTER_NAME_PROPERTY_ID, response.getClusterName(), requestedIds);
        setResourceProperty(resource, SERVICE_SERVICE_NAME_PROPERTY_ID, response.getServiceName(), requestedIds);
//        setResourceProperty(resource, SERVICE_VERSION_PROPERTY_ID, response.getCurrentStackVersion(), requestedIds);
      }
      return resources;
    }

    @Override
    public void updateResources(Request request, Predicate predicate) throws AmbariException {
      // get the service request properties from the given request
      Map<PropertyId, Object> properties = request.getProperties().iterator().next();
      // get the pk for the service request from the predicate
      setProperties(properties, getProperties(predicate),
          SERVICE_CLUSTER_NAME_PROPERTY_ID, SERVICE_SERVICE_NAME_PROPERTY_ID);

      ServiceRequest serviceRequest = getRequest(properties);
      getManagementController().updateService(serviceRequest);
    }

    @Override
    public void deleteResources(Predicate predicate) throws AmbariException {
      ServiceRequest serviceRequest = getRequest(getProperties(predicate));
      getManagementController().deleteService(serviceRequest);
    }

    // ----- utility methods -------------------------------------------------

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

  // ------ ComponentResourceProvider inner class ----------------------------

  private static class ComponentResourceProvider extends ResourceProviderImpl{

    // ----- Constructors ----------------------------------------------------

    /**
     * Create a  new resource provider for the given management controller.
     *
     * @param propertyIds           the property ids
     * @param managementController  the management controller
     */
    private ComponentResourceProvider(List<PropertyProvider> propertyProviders, Set<PropertyId> propertyIds, Map<Resource.Type, PropertyId> keyPropertyIds, AmbariManagementController managementController) {
      super(propertyProviders, propertyIds, keyPropertyIds, managementController);
    }

    // ----- ResourceProvider ------------------------------------------------

    @Override
    public void createResources(Request request) throws AmbariException {
      for (Map<PropertyId, Object> properties : request.getProperties()) {
        getManagementController().createComponent(getRequest(properties));
      }
    }

    @Override
    public Set<Resource> getResources(Request request, Predicate predicate) throws AmbariException {
      Set<PropertyId>          requestedIds            = request.getPropertyIds();
      ServiceComponentRequest  serviceComponentRequest = getRequest(getProperties(predicate));

      Set<ServiceComponentResponse> responses = getManagementController().getComponents(serviceComponentRequest);

      Set<Resource> resources = new HashSet<Resource>();
      for (ServiceComponentResponse response : responses) {
        Resource resource = new ResourceImpl(Resource.Type.Component);
//        setResourceProperty(resource, COMPONENT_CLUSTER_ID_PROPERTY_ID, response.getClusterId(), requestedIds);
        setResourceProperty(resource, COMPONENT_CLUSTER_NAME_PROPERTY_ID, response.getClusterName(), requestedIds);
        setResourceProperty(resource, COMPONENT_SERVICE_NAME_PROPERTY_ID, response.getServiceName(), requestedIds);
        setResourceProperty(resource, COMPONENT_COMPONENT_NAME_PROPERTY_ID, response.getComponentName(), requestedIds);
//        setResourceProperty(resource, COMPONENT_VERSION_PROPERTY_ID, response.getCurrentStackVersion(), requestedIds);
      }
      return resources;
    }

    @Override
    public void updateResources(Request request, Predicate predicate) throws AmbariException {
      // get the component request properties from the given request
      Map<PropertyId, Object> properties = request.getProperties().iterator().next();
      // get the pk for the service request from the predicate
      setProperties(properties, getProperties(predicate),
          COMPONENT_CLUSTER_NAME_PROPERTY_ID, COMPONENT_SERVICE_NAME_PROPERTY_ID, COMPONENT_COMPONENT_NAME_PROPERTY_ID);

      ServiceComponentRequest serviceComponentRequest = getRequest(properties);
      getManagementController().updateComponent(serviceComponentRequest);
    }

    @Override
    public void deleteResources(Predicate predicate) throws AmbariException {
      ServiceComponentRequest serviceComponentRequest = getRequest(getProperties(predicate));
      getManagementController().deleteComponent(serviceComponentRequest);
    }

    // ----- utility methods -------------------------------------------------

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

  // ------ HostResourceProvider inner class ---------------------------------

  private static class HostResourceProvider extends ResourceProviderImpl{

    // ----- Constructors ----------------------------------------------------

    /**
     * Create a  new resource provider for the given management controller.
     *
     * @param propertyIds           the property ids
     * @param managementController  the management controller
     */
    private HostResourceProvider(List<PropertyProvider> propertyProviders, Set<PropertyId> propertyIds, Map<Resource.Type, PropertyId> keyPropertyIds, AmbariManagementController managementController) {
      super(propertyProviders, propertyIds, keyPropertyIds, managementController);
    }

    // ----- ResourceProvider ------------------------------------------------

    @Override
    public void createResources(Request request) throws AmbariException {
      for (Map<PropertyId, Object> properties : request.getProperties()) {
        getManagementController().createHost(getRequest(properties));
      }
    }

    @Override
    public Set<Resource> getResources(Request request, Predicate predicate) throws AmbariException {
      Set<PropertyId> requestedIds = request.getPropertyIds();
      HostRequest     hostRequest  = getRequest(getProperties(predicate));

      Set<HostResponse> responses = getManagementController().getHosts(hostRequest);

      Set<Resource> resources = new HashSet<Resource>();
      for (HostResponse response : responses) {
        Resource resource = new ResourceImpl(Resource.Type.Host);
        // TODO : more than one cluster
//        setResourceProperty(resource, HOST_CLUSTER_NAME_PROPERTY_ID, response.getClusterNames(), requestedIds);
        setResourceProperty(resource, HOST_NAME_PROPERTY_ID, response.getHostname(), requestedIds);
        setResourceProperty(resource, HOST_IP_PROPERTY_ID, response.getIpv4(), requestedIds);
        setResourceProperty(resource, HOST_TOTAL_MEM_PROPERTY_ID, response.getTotalMemBytes(), requestedIds);
        setResourceProperty(resource, HOST_CPU_COUNT_PROPERTY_ID, response.getCpuCount(), requestedIds);
        setResourceProperty(resource, HOST_OS_ARCH_PROPERTY_ID, response.getOsArch(), requestedIds);
        setResourceProperty(resource, HOST_OS_TYPE_PROPERTY_ID, response.getOsType(), requestedIds);
        // TODO ...
      }
      return resources;
    }

    @Override
    public void updateResources(Request request, Predicate predicate) throws AmbariException {
      // get the host request properties from the given request
      Map<PropertyId, Object> properties = request.getProperties().iterator().next();
      // get the pk for the service request from the predicate
      setProperties(properties, getProperties(predicate), HOST_CLUSTER_NAME_PROPERTY_ID);

      HostRequest hostRequest = getRequest(properties);
      getManagementController().updateHost(hostRequest);
    }

    @Override
    public void deleteResources(Predicate predicate) throws AmbariException {
      HostRequest hostRequest = getRequest(getProperties(predicate));
      getManagementController().deleteHost(hostRequest);
    }

    // ----- utility methods -------------------------------------------------

    /**
     * Get a component request object from a map of property values.
     *
     * @param properties  the predicate
     *
     * @return the component request object
     */
    private HostRequest getRequest(Map<PropertyId, Object> properties) {
      return new HostRequest(
          (String) properties.get(HOST_NAME_PROPERTY_ID),
          // TODO : more than one cluster
          Collections.singletonList((String) properties.get(HOST_CLUSTER_NAME_PROPERTY_ID)),
          null);
    }
  }

  // ------ HostComponentResourceProvider inner class ------------------------

  private static class HostComponentResourceProvider extends ResourceProviderImpl{

    // ----- Constructors ----------------------------------------------------

    /**
     * Create a  new resource provider for the given management controller.
     *
     * @param propertyIds           the property ids
     * @param managementController  the management controller
     */
    private HostComponentResourceProvider(List<PropertyProvider> propertyProviders, Set<PropertyId> propertyIds, Map<Resource.Type, PropertyId> keyPropertyIds, AmbariManagementController managementController) {
      super(propertyProviders, propertyIds, keyPropertyIds, managementController);
    }

    // ----- ResourceProvider ------------------------------------------------

    @Override
    public void createResources(Request request) throws AmbariException {
      for (Map<PropertyId, Object> properties : request.getProperties()) {
        getManagementController().createHostComponent(getRequest(properties));
      }
    }

    @Override
    public Set<Resource> getResources(Request request, Predicate predicate) throws AmbariException {
      Set<PropertyId>             requestedIds         = request.getPropertyIds();
      ServiceComponentHostRequest hostComponentRequest = getRequest(getProperties(predicate));

      Set<ServiceComponentHostResponse> responses = getManagementController().getHostComponents(hostComponentRequest);

      Set<Resource> resources = new HashSet<Resource>();
      for (ServiceComponentHostResponse response : responses) {
        Resource resource = new ResourceImpl(Resource.Type.HostComponent);
        setResourceProperty(resource, HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID, response.getClusterName(), requestedIds);
        setResourceProperty(resource, HOST_COMPONENT_SERVICE_NAME_PROPERTY_ID, response.getServiceName(), requestedIds);
        setResourceProperty(resource, HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, response.getComponentName(), requestedIds);
        setResourceProperty(resource, HOST_COMPONENT_HOST_NAME_PROPERTY_ID, response.getHostname(), requestedIds);
        setResourceProperty(resource, HOST_COMPONENT_STATE_PROPERTY_ID, response.getLiveState(), requestedIds);
      }
      return resources;
    }

    @Override
    public void updateResources(Request request, Predicate predicate) throws AmbariException {
      // get the host request properties from the given request
      Map<PropertyId, Object> properties = request.getProperties().iterator().next();
      // get the pk for the service request from the predicate
      setProperties(properties, getProperties(predicate),
          HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID, HOST_COMPONENT_SERVICE_NAME_PROPERTY_ID,
          HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, HOST_COMPONENT_HOST_NAME_PROPERTY_ID);

      ServiceComponentHostRequest hostComponentRequest = getRequest(properties);
      getManagementController().updateHostComponent(hostComponentRequest);
    }

    @Override
    public void deleteResources(Predicate predicate) throws AmbariException {
      ServiceComponentHostRequest clusterRequest = getRequest(getProperties(predicate));
      getManagementController().deleteHostComponent(clusterRequest);
    }

    // ----- utility methods -------------------------------------------------

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
}
