/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.controller.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.ParentObjectNotFoundException;
import org.apache.ambari.server.ServiceGroupNotFoundException;
import org.apache.ambari.server.api.services.ServiceKey;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.ServiceDependencyRequest;
import org.apache.ambari.server.controller.ServiceDependencyResponse;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceGroup;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class ServiceDependencyResourceProvider extends AbstractControllerResourceProvider {


  // ----- Property ID constants ---------------------------------------------

  public static final String RESPONSE_KEY = "ServiceDependencyInfo";
  public static final String ALL_PROPERTIES = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "*";
  public static final String SERVICE_DEPENDENCY_CLUSTER_ID_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "cluster_id";
  public static final String SERVICE_DEPENDENCY_CLUSTER_NAME_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "cluster_name";
  public static final String SERVICE_DEPENDENCY_SERVICE_ID_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "service_id";
  public static final String SERVICE_DEPENDENCY_SERVICE_NAME_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "service_name";
  public static final String SERVICE_DEPENDENCY_SERVICE_GROUP_NAME_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "service_group_name";
  public static final String SERVICE_DEPENDENCY_SERVICE_GROUP_ID_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "service_group_id";
  public static final String SERVICE_DEPENDENCY_DEPENDENT_CLUSTER_ID_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "dependent_cluster_id";
  public static final String SERVICE_DEPENDENCY_DEPENDENT_CLUSTER_NAME_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "dependent_cluster_name";
  public static final String SERVICE_DEPENDENCY_DEPENDENT_SERVICE_ID_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "dependent_service_id";
  public static final String SERVICE_DEPENDENCY_DEPENDENT_SERVICE_NAME_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "dependent_service_name";
  public static final String SERVICE_DEPENDENCY_DEPENDENT_SERVICE_GROUP_NAME_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "dependent_service_group_name";
  public static final String SERVICE_DEPENDENCY_DEPENDENT_SERVICE_GROUP_ID_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "dependent_service_group_id";
  public static final String SERVICE_DEPENDENCY_DEPENDENCY_ID_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "dependency_id";

  protected ObjectMapper mapper = new ObjectMapper();;

  private static Set<String> pkPropertyIds =
          new HashSet<String>(Arrays.asList(new String[]{
                  SERVICE_DEPENDENCY_CLUSTER_NAME_PROPERTY_ID,
                  SERVICE_DEPENDENCY_SERVICE_GROUP_NAME_PROPERTY_ID,
                  SERVICE_DEPENDENCY_SERVICE_NAME_PROPERTY_ID,
                  SERVICE_DEPENDENCY_DEPENDENCY_ID_PROPERTY_ID}));

  private static Gson gson = StageUtils.getGson();

  /**
   * The property ids for an service group resource.
   */
  private static final Set<String> PROPERTY_IDS = new HashSet<>();

  /**
   * The key property ids for an service group resource.
   */
  private static final Map<Resource.Type, String> KEY_PROPERTY_IDS = new HashMap<>();

  static {
    // properties
    PROPERTY_IDS.add(SERVICE_DEPENDENCY_CLUSTER_ID_PROPERTY_ID);
    PROPERTY_IDS.add(SERVICE_DEPENDENCY_CLUSTER_NAME_PROPERTY_ID);
    PROPERTY_IDS.add(SERVICE_DEPENDENCY_SERVICE_ID_PROPERTY_ID);
    PROPERTY_IDS.add(SERVICE_DEPENDENCY_SERVICE_NAME_PROPERTY_ID);
    PROPERTY_IDS.add(SERVICE_DEPENDENCY_DEPENDENT_CLUSTER_ID_PROPERTY_ID);
    PROPERTY_IDS.add(SERVICE_DEPENDENCY_DEPENDENT_CLUSTER_NAME_PROPERTY_ID);
    PROPERTY_IDS.add(SERVICE_DEPENDENCY_DEPENDENT_SERVICE_ID_PROPERTY_ID);
    PROPERTY_IDS.add(SERVICE_DEPENDENCY_DEPENDENT_SERVICE_NAME_PROPERTY_ID);
    PROPERTY_IDS.add(SERVICE_DEPENDENCY_SERVICE_GROUP_ID_PROPERTY_ID);
    PROPERTY_IDS.add(SERVICE_DEPENDENCY_SERVICE_GROUP_NAME_PROPERTY_ID);
    PROPERTY_IDS.add(SERVICE_DEPENDENCY_DEPENDENT_SERVICE_GROUP_ID_PROPERTY_ID);
    PROPERTY_IDS.add(SERVICE_DEPENDENCY_DEPENDENT_SERVICE_GROUP_NAME_PROPERTY_ID);
    PROPERTY_IDS.add(SERVICE_DEPENDENCY_DEPENDENCY_ID_PROPERTY_ID);

    // keys
    KEY_PROPERTY_IDS.put(Resource.Type.Cluster, SERVICE_DEPENDENCY_CLUSTER_NAME_PROPERTY_ID);
    KEY_PROPERTY_IDS.put(Resource.Type.Service, SERVICE_DEPENDENCY_SERVICE_NAME_PROPERTY_ID);
    KEY_PROPERTY_IDS.put(Resource.Type.ServiceGroup, SERVICE_DEPENDENCY_SERVICE_GROUP_NAME_PROPERTY_ID);
    KEY_PROPERTY_IDS.put(Resource.Type.ServiceDependency, SERVICE_DEPENDENCY_DEPENDENCY_ID_PROPERTY_ID);
  }

  private Clusters clusters;


  // ----- Constructors ----------------------------------------------------

  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param managementController the management controller
   */
  @AssistedInject
  public ServiceDependencyResourceProvider(@Assisted AmbariManagementController managementController) {
    super(Resource.Type.ServiceDependency, PROPERTY_IDS, KEY_PROPERTY_IDS, managementController);
  }

  // ----- ResourceProvider ------------------------------------------------

  @Override
  protected RequestStatus createResourcesAuthorized(Request request)
          throws SystemException,
          UnsupportedPropertyException,
          ResourceAlreadyExistsException,
          NoSuchParentResourceException {

    final Set<ServiceDependencyRequest> requests = new HashSet<>();
    for (Map<String, Object> propertyMap : request.getProperties()) {
      requests.add(getRequest(propertyMap));
    }
    Set<ServiceDependencyResponse> createServiceGroups = null;
    createServiceGroups = createResources(new Command<Set<ServiceDependencyResponse>>() {
      @Override
      public Set<ServiceDependencyResponse> invoke() throws AmbariException, AuthorizationException {
        return createServiceDependencies(requests);
      }
    });
    Set<Resource> associatedResources = new HashSet<>();
    if (createServiceGroups != null) {
      Iterator<ServiceDependencyResponse> itr = createServiceGroups.iterator();
      while (itr.hasNext()) {
        ServiceDependencyResponse response = itr.next();
        notifyCreate(Resource.Type.ServiceDependency, request);
        Resource resource = new ResourceImpl(Resource.Type.ServiceDependency);
        resource.setProperty(SERVICE_DEPENDENCY_CLUSTER_ID_PROPERTY_ID,
                response.getClusterId());
        resource.setProperty(SERVICE_DEPENDENCY_CLUSTER_NAME_PROPERTY_ID,
                response.getClusterName());
        resource.setProperty(SERVICE_DEPENDENCY_SERVICE_ID_PROPERTY_ID,
                response.getServiceId());
        resource.setProperty(SERVICE_DEPENDENCY_SERVICE_NAME_PROPERTY_ID,
                response.getServiceName());
        resource.setProperty(SERVICE_DEPENDENCY_SERVICE_GROUP_ID_PROPERTY_ID,
                response.getServiceGroupId());
        resource.setProperty(SERVICE_DEPENDENCY_SERVICE_GROUP_NAME_PROPERTY_ID,
                response.getServiceGroupName());
        resource.setProperty(SERVICE_DEPENDENCY_DEPENDENT_CLUSTER_ID_PROPERTY_ID,
                response.getDependencyClusterId());
        resource.setProperty(SERVICE_DEPENDENCY_DEPENDENT_CLUSTER_NAME_PROPERTY_ID,
                response.getDependencyClusterName());
        resource.setProperty(SERVICE_DEPENDENCY_DEPENDENT_SERVICE_ID_PROPERTY_ID,
                response.getDependencyServiceId());
        resource.setProperty(SERVICE_DEPENDENCY_DEPENDENT_SERVICE_NAME_PROPERTY_ID,
                response.getDependencyServiceName());
        resource.setProperty(SERVICE_DEPENDENCY_DEPENDENT_SERVICE_GROUP_ID_PROPERTY_ID,
                response.getDependencyServiceGroupId());
        resource.setProperty(SERVICE_DEPENDENCY_DEPENDENT_SERVICE_GROUP_NAME_PROPERTY_ID,
                response.getDependencyServiceGroupName());
        resource.setProperty(SERVICE_DEPENDENCY_DEPENDENCY_ID_PROPERTY_ID,
                response.getDependencyId());

        associatedResources.add(resource);
      }
      return getRequestStatus(null, associatedResources);
    }

    return getRequestStatus(null);
  }

  @Override
  protected Set<Resource> getResourcesAuthorized(Request request, Predicate predicate) throws
          SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<ServiceDependencyRequest> requests = new HashSet<>();

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      requests.add(getRequest(propertyMap));
    }

    Set<ServiceDependencyResponse> responses = getResources(new Command<Set<ServiceDependencyResponse>>() {
      @Override
      public Set<ServiceDependencyResponse> invoke() throws AmbariException {
        return getServiceDependencies(requests);
      }
    });

    Set<String> requestedIds = getRequestPropertyIds(request, predicate);
    Set<Resource> resources = new HashSet<Resource>();

    for (ServiceDependencyResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.ServiceDependency);
      setResourceProperty(resource, SERVICE_DEPENDENCY_CLUSTER_ID_PROPERTY_ID,
              response.getClusterId(), requestedIds);
      setResourceProperty(resource, SERVICE_DEPENDENCY_CLUSTER_NAME_PROPERTY_ID,
              response.getClusterName(), requestedIds);
      setResourceProperty(resource, SERVICE_DEPENDENCY_SERVICE_ID_PROPERTY_ID,
              response.getServiceId(), requestedIds);
      setResourceProperty(resource, SERVICE_DEPENDENCY_SERVICE_NAME_PROPERTY_ID,
              response.getServiceName(), requestedIds);
      setResourceProperty(resource, SERVICE_DEPENDENCY_SERVICE_GROUP_ID_PROPERTY_ID,
              response.getServiceGroupId(), requestedIds);
      setResourceProperty(resource, SERVICE_DEPENDENCY_SERVICE_GROUP_NAME_PROPERTY_ID,
              response.getServiceGroupName(), requestedIds);
      setResourceProperty(resource, SERVICE_DEPENDENCY_DEPENDENT_CLUSTER_ID_PROPERTY_ID,
              response.getDependencyClusterId(), requestedIds);
      setResourceProperty(resource, SERVICE_DEPENDENCY_DEPENDENT_CLUSTER_NAME_PROPERTY_ID,
              response.getDependencyClusterName(), requestedIds);
      setResourceProperty(resource, SERVICE_DEPENDENCY_DEPENDENT_SERVICE_ID_PROPERTY_ID,
              response.getDependencyServiceId(), requestedIds);
      setResourceProperty(resource, SERVICE_DEPENDENCY_DEPENDENT_SERVICE_NAME_PROPERTY_ID,
              response.getDependencyServiceName(), requestedIds);
      setResourceProperty(resource, SERVICE_DEPENDENCY_DEPENDENT_SERVICE_GROUP_ID_PROPERTY_ID,
              response.getDependencyServiceGroupId(), requestedIds);
      setResourceProperty(resource, SERVICE_DEPENDENCY_DEPENDENT_SERVICE_GROUP_NAME_PROPERTY_ID,
              response.getDependencyServiceGroupName(), requestedIds);
      setResourceProperty(resource, SERVICE_DEPENDENCY_DEPENDENCY_ID_PROPERTY_ID,
              response.getDependencyId(), requestedIds);

      resources.add(resource);
    }
    return resources;
  }

  @Override
  protected RequestStatus updateResourcesAuthorized(final Request request, Predicate predicate)
          throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    // TODO : Add functionality for updating SG : RENAME, START ALL, STOP ALL services.
    RequestStatusResponse response = null;
    return getRequestStatus(response);
  }

  @Override
  protected RequestStatus deleteResourcesAuthorized(Request request, Predicate predicate)
          throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<ServiceDependencyRequest> requests = new HashSet<>();
    DeleteStatusMetaData deleteStatusMetaData = null;

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      requests.add(getRequest(propertyMap));
    }
    deleteStatusMetaData = modifyResources(new Command<DeleteStatusMetaData>() {
      @Override
      public DeleteStatusMetaData invoke() throws AmbariException, AuthorizationException {
        deleteServiceDependencies(requests);
        return new DeleteStatusMetaData();
      }
    });

    notifyDelete(Resource.Type.ServiceDependency, predicate);
    for(ServiceDependencyRequest svgReq : requests) {
      deleteStatusMetaData.addDeletedKey("cluster_name: " + svgReq.getClusterName() + ", " + " service_name: " + svgReq.getServiceName()
              + " dependency id: " + svgReq.getDependencyId());
    }
    return getRequestStatus(null, null, deleteStatusMetaData);
  }

  @Override
  public Set<String> checkPropertyIds(Set<String> propertyIds) {
    propertyIds = super.checkPropertyIds(propertyIds);

    if (propertyIds.isEmpty()) {
      return propertyIds;
    }
    Set<String> unsupportedProperties = new HashSet<String>();
    return unsupportedProperties;
  }


  // ----- AbstractResourceProvider ----------------------------------------

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

  // ----- utility methods -------------------------------------------------

  /**
   * Get a service group request object from a map of property values.
   *
   * @param properties the predicate
   * @return the service request object
   */
  private ServiceDependencyRequest getRequest(Map<String, Object> properties) {
    String clusterName = (String) properties.get(SERVICE_DEPENDENCY_CLUSTER_NAME_PROPERTY_ID);
    String serviceName = (String) properties.get(SERVICE_DEPENDENCY_SERVICE_NAME_PROPERTY_ID);
    String serviceGroupName = (String) properties.get(SERVICE_DEPENDENCY_SERVICE_GROUP_NAME_PROPERTY_ID);
    String dependentClusterName = (String) properties.get(SERVICE_DEPENDENCY_DEPENDENT_CLUSTER_NAME_PROPERTY_ID);
    String dependentServiceName = (String) properties.get(SERVICE_DEPENDENCY_DEPENDENT_SERVICE_NAME_PROPERTY_ID);
    String dependentServiceGroupName = (String) properties.get(SERVICE_DEPENDENCY_DEPENDENT_SERVICE_GROUP_NAME_PROPERTY_ID);
    String strDependencyId = (String) properties.get(SERVICE_DEPENDENCY_DEPENDENCY_ID_PROPERTY_ID);
    Long dependencyId = strDependencyId == null ? null : Long.valueOf(strDependencyId);
    ServiceDependencyRequest svcRequest = new ServiceDependencyRequest(clusterName, serviceName, serviceGroupName,
            dependentClusterName, dependentServiceGroupName, dependentServiceName, dependencyId);
    return svcRequest;
  }

  // Create services from the given request.
  public synchronized Set<ServiceDependencyResponse> createServiceDependencies(Set<ServiceDependencyRequest> requests)
          throws AmbariException, AuthorizationException {

    if (requests.isEmpty()) {
      LOG.warn("Received an empty requests set");
      return null;
    }
    AmbariManagementController controller = getManagementController();
    Clusters clusters = controller.getClusters();

    // do all validation checks
    validateCreateRequests(requests, clusters);


    Set<ServiceDependencyResponse> createdServiceDependencies = new HashSet<>();
    for (ServiceDependencyRequest request : requests) {
      Cluster cluster = clusters.getCluster(request.getClusterName());
      ServiceGroup serviceGroup = cluster.getServiceGroup(request.getServiceGroupName());

      Cluster dependentServiceCluster = cluster;
      if (StringUtils.isNotEmpty(request.getDependentClusterName())) {
        dependentServiceCluster = clusters.getCluster(request.getDependentClusterName());
      }

      ServiceGroup dependentServiceGroup = serviceGroup;
      if (StringUtils.isNotEmpty(request.getDependentServiceGroupName())) {
        dependentServiceGroup = dependentServiceCluster.getServiceGroup(request.getDependentServiceGroupName());
      }


      Service dependentService = cluster.getService(dependentServiceGroup.getServiceGroupName(), request.getDependentServiceName());

      Service updatedService = cluster.addDependencyToService(request.getServiceGroupName(), request.getServiceName(), dependentService.getServiceId());
      createdServiceDependencies.addAll(updatedService.getServiceDependencyResponses());
    }
    return createdServiceDependencies;
  }

  // Get services from the given set of requests.
  protected Set<ServiceDependencyResponse> getServiceDependencies(Set<ServiceDependencyRequest> requests)
          throws AmbariException {
    Set<ServiceDependencyResponse> response = new HashSet<ServiceDependencyResponse>();
    for (ServiceDependencyRequest request : requests) {
      try {
        response.addAll(getServiceDependencies(request));
      } catch (ServiceGroupNotFoundException e) {
        if (requests.size() == 1) {
          // only throw exception if 1 request.
          // there will be > 1 request in case of OR predicate
          throw e;
        }
      }
    }
    return response;
  }

  // Get services from the given request.
  private Set<ServiceDependencyResponse> getServiceDependencies(ServiceDependencyRequest request)
          throws AmbariException {
    if (request.getClusterName() == null) {
      throw new AmbariException("Invalid arguments, cluster id"
              + " cannot be null");
    }
    AmbariManagementController controller = getManagementController();
    Clusters clusters = controller.getClusters();
    String clusterName = request.getClusterName();

    final Cluster cluster;
    try {
      cluster = clusters.getCluster(clusterName);
    } catch (ObjectNotFoundException e) {
      throw new ParentObjectNotFoundException("Parent Cluster resource doesn't exist", e);
    }

    ServiceGroup serviceGroup = cluster.getServiceGroup(request.getServiceGroupName());

    Set<ServiceDependencyResponse> responses = new HashSet<>();
    if (request.getServiceName() != null) {
      Collection<Service> services = cluster.getServices().values();
      Service currentService = null;
      for (Service service : services) {
        if (service.getServiceGroupId() == serviceGroup.getServiceGroupId() &&
                service.getName().equals(request.getServiceName())) {
          currentService = service;
          break;
        }
      }

      responses.addAll(currentService.getServiceDependencyResponses());
      return responses;
    }
    return responses;
  }


  protected void deleteServiceDependencies(Set<ServiceDependencyRequest> request)
          throws AmbariException, AuthorizationException {

    Clusters clusters = getManagementController().getClusters();


    for (ServiceDependencyRequest serviceDependencyRequest : request) {
      if (null == serviceDependencyRequest.getClusterName()
              || StringUtils.isEmpty(serviceDependencyRequest.getServiceGroupName())) {
        throw new AmbariException("invalid arguments");
      } else {

        if (!AuthorizationHelper.isAuthorized(
                ResourceType.CLUSTER, getClusterResourceId(serviceDependencyRequest.getClusterName()),
                RoleAuthorization.SERVICE_ADD_DELETE_SERVICES)) {
          throw new AuthorizationException("The user is not authorized to delete service groups");
        }

        Cluster cluster = clusters.getCluster(serviceDependencyRequest.getClusterName());
        Service service = null;

        for (Service srv : cluster.getServicesById().values()) {
          if (srv.getName().equals(serviceDependencyRequest.getServiceName()) &&
                  srv.getServiceGroupName().equals(serviceDependencyRequest.getServiceGroupName())) {
            service = srv;
            break;
          }
        }

        List<ServiceKey> serviceKeys = service.getServiceDependencies();
        if (serviceKeys == null || serviceKeys.isEmpty()) {
          throw new AmbariException("Service name " + serviceDependencyRequest.getServiceName() + " has no" +
                  "dependencies, so nothing to remove.");
        } else {
          boolean dependencyAvailable = false;
          long dependentServiceId = 0L;
          for (ServiceKey serviceKey : serviceKeys) {
            if (serviceKey.getDependencyId() == serviceDependencyRequest.getDependencyId()) {
              dependencyAvailable = true;
              dependentServiceId = serviceKey.getServiceId();
            }
          }
          if (!dependencyAvailable) {
            throw new AmbariException("Servcie name " + serviceDependencyRequest.getServiceName() + " has no" +
                    "service dependency with id" + serviceDependencyRequest.getDependencyId() + ", so nothing to remove.");
          }

          service.getCluster().removeDependencyFromService(serviceDependencyRequest.getServiceGroupName(), serviceDependencyRequest.getServiceName(),
                  dependentServiceId);
        }
      }
    }
  }


  private void validateCreateRequests(Set<ServiceDependencyRequest> requests, Clusters clusters)
          throws AuthorizationException, AmbariException {

    Map<String, Set<String>> serviceGroupNames = new HashMap<>();
    Set<String> duplicates = new HashSet<>();
    for (ServiceDependencyRequest request : requests) {
      final String clusterName = request.getClusterName();
      final String serviceGroupName = request.getServiceGroupName();
      final String serviceName = request.getServiceName();
      final String dependentClusterName = request.getDependentClusterName();
      final String dependentServiceGroupName = request.getDependentServiceGroupName();
      final String dependentServiceName = request.getDependentServiceName();

      Validate.notNull(clusterName, "Cluster name should be provided when creating a service dependency");
      Validate.notNull(serviceGroupName, "Service group name should be provided when creating a service dependency");
      Validate.notNull(serviceName, "Service name should be provided when creating a service dependency");
      Validate.notNull(dependentServiceName, "Dependency service name should be provided when creating a service dependency");

      //throws cluster not found exception
      Cluster cluster = clusters.getCluster(clusterName);
      //throws service group not found exception
      ServiceGroup serviceGroup = cluster.getServiceGroup(serviceGroupName);
      //throws service not found exception
      Service service = cluster.getService(serviceName);

      Cluster dependencyCluster = cluster;
      if (StringUtils.isNotEmpty(dependentClusterName)) {
        dependencyCluster = clusters.getCluster(dependentClusterName);
      }

      ServiceGroup dependencyServiceGroup = serviceGroup;
      if (StringUtils.isNotEmpty(dependentServiceGroupName)) {
        dependencyServiceGroup = dependencyCluster.getServiceGroup(dependentServiceGroupName);
      }

      Service dependencyService = dependencyCluster.getService(dependentServiceGroupName, dependentServiceName);

      if (LOG.isDebugEnabled()) {
        LOG.debug("Received a createServiceDependency request" +
                ", clusterName=" + clusterName + ", serviceGroupName=" + serviceGroupName + ", request=" + request);
      }

      if (!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER,
              getClusterResourceId(clusterName), RoleAuthorization.SERVICE_ADD_DELETE_SERVICES)) {
        throw new AuthorizationException("The user is not authorized to create service groups");
      }


      if (service.getServiceDependencies() != null) {
        for (ServiceKey sk : service.getServiceDependencies()) {
          if (sk.getServiceId() == dependencyService.getServiceId()) {
            throw new AmbariException("Service with id=" + dependencyService.getServiceId() + " already added to dependencies for " +
                    serviceName + " service");
          }
        }

      }
    }
  }
}
