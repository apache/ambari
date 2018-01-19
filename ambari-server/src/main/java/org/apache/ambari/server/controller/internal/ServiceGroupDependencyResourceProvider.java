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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ClusterNotFoundException;
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.ParentObjectNotFoundException;
import org.apache.ambari.server.ServiceGroupNotFoundException;
import org.apache.ambari.server.api.services.ServiceGroupKey;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.ServiceGroupDependencyRequest;
import org.apache.ambari.server.controller.ServiceGroupDependencyResponse;
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
import org.apache.ambari.server.state.ServiceGroup;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class ServiceGroupDependencyResourceProvider extends AbstractControllerResourceProvider {


  // ----- Property ID constants ---------------------------------------------

  public static final String RESPONSE_KEY = "ServiceGroupDependencyInfo";
  public static final String ALL_PROPERTIES = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "*";
  public static final String SERVICE_GROUP_DEPENDENCY_CLUSTER_ID_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "cluster_id";
  public static final String SERVICE_GROUP_DEPENDENCY_CLUSTER_NAME_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "cluster_name";
  public static final String SERVICE_GROUP_DEPENDENCY_SERVICE_GROUP_ID_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "service_group_id";
  public static final String SERVICE_GROUP_DEPENDENCY_SERVICE_GROUP_NAME_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "service_group_name";
  public static final String SERVICE_GROUP_DEPENDENCY_DEPENDENT_CLUSTER_ID_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "dependent_cluster_id";
  public static final String SERVICE_GROUP_DEPENDENCY_DEPENDENT_CLUSTER_NAME_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "dependent_cluster_name";
  public static final String SERVICE_GROUP_DEPENDENCY_DEPENDENT_SERVICE_GROUP_ID_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "dependent_service_group_id";
  public static final String SERVICE_GROUP_DEPENDENCY_DEPENDENT_SERVICE_GROUP_NAME_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "dependent_service_group_name";
  public static final String SERVICE_GROUP_DEPENDENCY_DEPENDENCY_ID_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "dependency_id";

  protected ObjectMapper mapper = new ObjectMapper();;

  private static Set<String> pkPropertyIds =
    new HashSet<String>(Arrays.asList(new String[]{
      SERVICE_GROUP_DEPENDENCY_CLUSTER_NAME_PROPERTY_ID,
      SERVICE_GROUP_DEPENDENCY_SERVICE_GROUP_NAME_PROPERTY_ID,
      SERVICE_GROUP_DEPENDENCY_DEPENDENCY_ID_PROPERTY_ID}));

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
    PROPERTY_IDS.add(SERVICE_GROUP_DEPENDENCY_CLUSTER_ID_PROPERTY_ID);
    PROPERTY_IDS.add(SERVICE_GROUP_DEPENDENCY_CLUSTER_NAME_PROPERTY_ID);
    PROPERTY_IDS.add(SERVICE_GROUP_DEPENDENCY_SERVICE_GROUP_ID_PROPERTY_ID);
    PROPERTY_IDS.add(SERVICE_GROUP_DEPENDENCY_SERVICE_GROUP_NAME_PROPERTY_ID);
    PROPERTY_IDS.add(SERVICE_GROUP_DEPENDENCY_DEPENDENT_CLUSTER_ID_PROPERTY_ID);
    PROPERTY_IDS.add(SERVICE_GROUP_DEPENDENCY_DEPENDENT_CLUSTER_NAME_PROPERTY_ID);
    PROPERTY_IDS.add(SERVICE_GROUP_DEPENDENCY_DEPENDENT_SERVICE_GROUP_ID_PROPERTY_ID);
    PROPERTY_IDS.add(SERVICE_GROUP_DEPENDENCY_DEPENDENT_SERVICE_GROUP_NAME_PROPERTY_ID);
    PROPERTY_IDS.add(SERVICE_GROUP_DEPENDENCY_DEPENDENCY_ID_PROPERTY_ID);

    // keys
    KEY_PROPERTY_IDS.put(Resource.Type.Cluster, SERVICE_GROUP_DEPENDENCY_CLUSTER_NAME_PROPERTY_ID);
    KEY_PROPERTY_IDS.put(Resource.Type.ServiceGroup, SERVICE_GROUP_DEPENDENCY_SERVICE_GROUP_NAME_PROPERTY_ID);
    KEY_PROPERTY_IDS.put(Resource.Type.ServiceGroupDependency, SERVICE_GROUP_DEPENDENCY_DEPENDENCY_ID_PROPERTY_ID);
  }

  private Clusters clusters;

  /**
   * kerberos helper
   */
  @Inject
  private KerberosHelper kerberosHelper;

  // ----- Constructors ----------------------------------------------------

  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param managementController the management controller
   */
  @AssistedInject
  public ServiceGroupDependencyResourceProvider(@Assisted AmbariManagementController managementController) {
    super(Resource.Type.ServiceGroupDependency, PROPERTY_IDS, KEY_PROPERTY_IDS, managementController);
  }

  // ----- ResourceProvider ------------------------------------------------

  @Override
  protected RequestStatus createResourcesAuthorized(Request request)
    throws SystemException,
    UnsupportedPropertyException,
    ResourceAlreadyExistsException,
    NoSuchParentResourceException {

    final Set<ServiceGroupDependencyRequest> requests = new HashSet<>();
    for (Map<String, Object> propertyMap : request.getProperties()) {
      requests.add(getRequest(propertyMap));
    }
    Set<ServiceGroupDependencyResponse> createServiceGroups = null;
    createServiceGroups = createResources(new Command<Set<ServiceGroupDependencyResponse>>() {
      @Override
      public Set<ServiceGroupDependencyResponse> invoke() throws AmbariException, AuthorizationException {
        return createServiceGroupDependencies(requests);
      }
    });
    Set<Resource> associatedResources = new HashSet<>();
    if (createServiceGroups != null) {
      Iterator<ServiceGroupDependencyResponse> itr = createServiceGroups.iterator();
      while (itr.hasNext()) {
        ServiceGroupDependencyResponse response = itr.next();
        notifyCreate(Resource.Type.ServiceGroupDependency, request);
        Resource resource = new ResourceImpl(Resource.Type.ServiceGroupDependency);
        resource.setProperty(SERVICE_GROUP_DEPENDENCY_CLUSTER_ID_PROPERTY_ID, response.getClusterId());
        resource.setProperty(SERVICE_GROUP_DEPENDENCY_CLUSTER_NAME_PROPERTY_ID, response.getClusterName());
        resource.setProperty(SERVICE_GROUP_DEPENDENCY_SERVICE_GROUP_ID_PROPERTY_ID, response.getServiceGroupId());
        resource.setProperty(SERVICE_GROUP_DEPENDENCY_SERVICE_GROUP_NAME_PROPERTY_ID, response.getServiceGroupName());
        resource.setProperty(SERVICE_GROUP_DEPENDENCY_DEPENDENT_CLUSTER_ID_PROPERTY_ID, response.getDependencyClusterId());
        resource.setProperty(SERVICE_GROUP_DEPENDENCY_DEPENDENT_CLUSTER_NAME_PROPERTY_ID, response.getDependencyClusterName());
        resource.setProperty(SERVICE_GROUP_DEPENDENCY_DEPENDENT_SERVICE_GROUP_ID_PROPERTY_ID, response.getDependencyGroupId());
        resource.setProperty(SERVICE_GROUP_DEPENDENCY_DEPENDENT_SERVICE_GROUP_NAME_PROPERTY_ID, response.getDependencyGroupName());
        resource.setProperty(SERVICE_GROUP_DEPENDENCY_DEPENDENCY_ID_PROPERTY_ID, response.getDependencyId());

        associatedResources.add(resource);
      }
      return getRequestStatus(null, associatedResources);
    }

    return getRequestStatus(null);
  }

  @Override
  protected Set<Resource> getResourcesAuthorized(Request request, Predicate predicate) throws
    SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<ServiceGroupDependencyRequest> requests = new HashSet<>();

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      requests.add(getRequest(propertyMap));
    }

    Set<ServiceGroupDependencyResponse> responses = getResources(new Command<Set<ServiceGroupDependencyResponse>>() {
      @Override
      public Set<ServiceGroupDependencyResponse> invoke() throws AmbariException {
        return getServiceGroupDependencies(requests);
      }
    });

    Set<String> requestedIds = getRequestPropertyIds(request, predicate);
    Set<Resource> resources = new HashSet<Resource>();

    for (ServiceGroupDependencyResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.ServiceGroupDependency);
      setResourceProperty(resource, SERVICE_GROUP_DEPENDENCY_CLUSTER_ID_PROPERTY_ID,
              response.getClusterId(), requestedIds);
      setResourceProperty(resource, SERVICE_GROUP_DEPENDENCY_CLUSTER_NAME_PROPERTY_ID,
              response.getClusterName(), requestedIds);
      setResourceProperty(resource, SERVICE_GROUP_DEPENDENCY_SERVICE_GROUP_ID_PROPERTY_ID,
              response.getServiceGroupId(), requestedIds);
      setResourceProperty(resource, SERVICE_GROUP_DEPENDENCY_SERVICE_GROUP_NAME_PROPERTY_ID,
              response.getServiceGroupName(), requestedIds);
      setResourceProperty(resource, SERVICE_GROUP_DEPENDENCY_DEPENDENT_CLUSTER_ID_PROPERTY_ID,
              response.getDependencyClusterId(), requestedIds);
      setResourceProperty(resource, SERVICE_GROUP_DEPENDENCY_DEPENDENT_CLUSTER_NAME_PROPERTY_ID,
              response.getDependencyClusterName(), requestedIds);
      setResourceProperty(resource, SERVICE_GROUP_DEPENDENCY_DEPENDENT_SERVICE_GROUP_ID_PROPERTY_ID,
              response.getDependencyGroupId(), requestedIds);
      setResourceProperty(resource, SERVICE_GROUP_DEPENDENCY_DEPENDENT_SERVICE_GROUP_NAME_PROPERTY_ID,
              response.getDependencyGroupName(), requestedIds);
      setResourceProperty(resource, SERVICE_GROUP_DEPENDENCY_DEPENDENCY_ID_PROPERTY_ID,
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

    final Set<ServiceGroupDependencyRequest> requests = new HashSet<>();
    DeleteStatusMetaData deleteStatusMetaData = null;

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      requests.add(getRequest(propertyMap));
    }
    deleteStatusMetaData = modifyResources(new Command<DeleteStatusMetaData>() {
      @Override
      public DeleteStatusMetaData invoke() throws AmbariException, AuthorizationException {
        deleteServiceGroupDependencies(requests);
        return new DeleteStatusMetaData();
      }
    });

    notifyDelete(Resource.Type.ServiceGroupDependency, predicate);
    for(ServiceGroupDependencyRequest svgReq : requests) {
      deleteStatusMetaData.addDeletedKey("cluster_name: " + svgReq.getClusterName() + ", " + " service_group_name: " + svgReq.getServiceGroupName()
                                        + " dependency_id: " + svgReq.getDependencyId());
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
  private ServiceGroupDependencyRequest getRequest(Map<String, Object> properties) {
    String clusterName = (String) properties.get(SERVICE_GROUP_DEPENDENCY_CLUSTER_NAME_PROPERTY_ID);
    String serviceGroupName = (String) properties.get(SERVICE_GROUP_DEPENDENCY_SERVICE_GROUP_NAME_PROPERTY_ID);
    String dependentServiceGroupClusterName = (String) properties.get(SERVICE_GROUP_DEPENDENCY_DEPENDENT_CLUSTER_NAME_PROPERTY_ID);
    String dependentServiceGroupName = (String) properties.get(SERVICE_GROUP_DEPENDENCY_DEPENDENT_SERVICE_GROUP_NAME_PROPERTY_ID);
    String strdependencyId = (String)properties.get(SERVICE_GROUP_DEPENDENCY_DEPENDENCY_ID_PROPERTY_ID);
    Long dependencyId = strdependencyId == null ? null : Long.valueOf(strdependencyId);
    ServiceGroupDependencyRequest svcRequest = new ServiceGroupDependencyRequest(clusterName, serviceGroupName, dependentServiceGroupClusterName,
            dependentServiceGroupName, dependencyId);
    return svcRequest;
  }


  // Create services from the given request.
  public synchronized Set<ServiceGroupDependencyResponse> createServiceGroupDependencies(Set<ServiceGroupDependencyRequest> requests)
    throws AmbariException, AuthorizationException {

    if (requests.isEmpty()) {
      LOG.warn("Received an empty requests set");
      return null;
    }
    AmbariManagementController controller = getManagementController();
    Clusters clusters = controller.getClusters();

    // do all validation checks
    validateCreateRequests(requests, clusters);

    Set<ServiceGroupDependencyResponse> createdServiceGroupDependencies = new HashSet<>();
    for (ServiceGroupDependencyRequest request : requests) {
      Cluster cluster = clusters.getCluster(request.getClusterName());
      String dependentServiceGroupClusterName = request.getClusterName();

      //find dependent service group
      if (StringUtils.isNotEmpty(request.getDependentServiceGroupClusterName())) {
        dependentServiceGroupClusterName = request.getDependentServiceGroupClusterName();
      }
      Cluster dependentServiceGroupCluster = clusters.getCluster(dependentServiceGroupClusterName);
      ServiceGroup serviceGroup = dependentServiceGroupCluster.getServiceGroup(request.getDependentServiceGroupName());

      // Already checked that service group does not exist
      cluster.addServiceGroupDependency(request.getServiceGroupName(), serviceGroup.getServiceGroupId());
      createdServiceGroupDependencies.addAll(cluster.getServiceGroup(request.getServiceGroupName()).getServiceGroupDependencyResponses());
    }
    return createdServiceGroupDependencies;
  }

  // Get services from the given set of requests.
  protected Set<ServiceGroupDependencyResponse> getServiceGroupDependencies(Set<ServiceGroupDependencyRequest> requests)
    throws AmbariException {
    Set<ServiceGroupDependencyResponse> response = new HashSet<ServiceGroupDependencyResponse>();
    for (ServiceGroupDependencyRequest request : requests) {
      try {
        response.addAll(getServiceGroupDependencies(request));
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
  private Set<ServiceGroupDependencyResponse> getServiceGroupDependencies(ServiceGroupDependencyRequest request)
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

    Set<ServiceGroupDependencyResponse> responses = new HashSet<>();
    if (request.getServiceGroupName() != null) {
      ServiceGroup sg = cluster.getServiceGroup(request.getServiceGroupName());
      responses.addAll(sg.getServiceGroupDependencyResponses());
      return responses;
    }
    return responses;
  }


  // Delete services based on the given set of requests
  protected void deleteServiceGroupDependencies(Set<ServiceGroupDependencyRequest> request)
    throws AmbariException, AuthorizationException {

    Clusters clusters = getManagementController().getClusters();


    for (ServiceGroupDependencyRequest serviceGroupDependencyRequest : request) {
      if (null == serviceGroupDependencyRequest.getClusterName()
        || StringUtils.isEmpty(serviceGroupDependencyRequest.getServiceGroupName())) {
        throw new AmbariException("invalid arguments");
      } else {

        if (!AuthorizationHelper.isAuthorized(
          ResourceType.CLUSTER, getClusterResourceId(serviceGroupDependencyRequest.getClusterName()),
          RoleAuthorization.SERVICE_ADD_DELETE_SERVICES)) {
          throw new AuthorizationException("The user is not authorized to delete service groups");
        }

        Cluster cluster = clusters.getCluster(serviceGroupDependencyRequest.getClusterName());
        ServiceGroup serviceGroup = cluster.getServiceGroup(serviceGroupDependencyRequest.getServiceGroupName());
        Set<ServiceGroupKey> dependencyServiceGroupKeys = serviceGroup.getServiceGroupDependencies();
        if (dependencyServiceGroupKeys == null || dependencyServiceGroupKeys.isEmpty()) {
          throw new AmbariException("Servcie group name " + serviceGroupDependencyRequest.getServiceGroupName() + " has no" +
                  " dependencies, so nothing to remove.");
        } else {
          boolean dependencyAvailable = false;
          long dependentServiceGroupId =  0L;
          for (ServiceGroupKey dependencyServiceGroupKey : dependencyServiceGroupKeys) {
            if (dependencyServiceGroupKey.getDependencyId() == serviceGroupDependencyRequest.getDependencyId()) {
              dependencyAvailable = true;
              dependentServiceGroupId = dependencyServiceGroupKey.getServiceGroupId();
            }
          }
          if (!dependencyAvailable) {
            throw new AmbariException("Servcie group name " + serviceGroupDependencyRequest.getServiceGroupName() + " has no" +
                    "dependency with id=" + serviceGroupDependencyRequest.getDependencyId() + ", so nothing to remove.");
          }

          serviceGroup.getCluster().deleteServiceGroupDependency(serviceGroupDependencyRequest.getServiceGroupName(),
                  dependentServiceGroupId);
        }
      }
    }
  }


  private void validateCreateRequests(Set<ServiceGroupDependencyRequest> requests, Clusters clusters)
    throws AuthorizationException, AmbariException {

    for (ServiceGroupDependencyRequest request : requests) {
      final String clusterName = request.getClusterName();
      final String serviceGroupName = request.getServiceGroupName();
      String dependentServiceGroupClusterName = request.getDependentServiceGroupClusterName();
      final String dependentServiceGroupName = request.getDependentServiceGroupName();

      Validate.notNull(clusterName, "Cluster name should be provided when creating a service group dependency");
      Validate.notNull(serviceGroupName, "Service group name should be provided when creating a service group dependency");
      Validate.notNull(dependentServiceGroupName, "Dependent service group name should be provided when creating a service group dependency");

      // validating service group dependencies
      Long dependentServiceGroupId = null;
      if (StringUtils.isEmpty(dependentServiceGroupName)) {

        throw new AmbariException("Dependent Service group name is null or empty!");

      } else {

        try {
          if (StringUtils.isEmpty(dependentServiceGroupClusterName)) {
            dependentServiceGroupClusterName = clusterName;
          }
          Cluster cluster = clusters.getCluster(dependentServiceGroupClusterName);

          ServiceGroup dependentServiceGroup = cluster.getServiceGroup(dependentServiceGroupName);
          dependentServiceGroupId = dependentServiceGroup.getServiceGroupId();
        } catch (ClusterNotFoundException e) {
          throw new ParentObjectNotFoundException("Attempted to add a service group dependency to a cluster which doesn't exist", e);
        } catch (ServiceGroupNotFoundException e) {
          throw new AmbariException(String.format("Unable to find dependent service group %s in cluster %s",
                  dependentServiceGroupName, dependentServiceGroupClusterName), e);
        }

      }

      if (LOG.isDebugEnabled()) {
        LOG.debug("Received a createServiceGroupDependency request" +
          ", clusterName=" + clusterName + ", serviceGroupName=" + serviceGroupName + ", request=" + request);
      }

      if (!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER,
              getClusterResourceId(clusterName), RoleAuthorization.SERVICE_ADD_DELETE_SERVICES)) {
        throw new AuthorizationException("The user is not authorized to create service groups");
      }


      Cluster cluster;
      try {
        cluster = clusters.getCluster(clusterName);
      } catch (ClusterNotFoundException e) {
        throw new ParentObjectNotFoundException("Attempted to add a service group to a cluster which doesn't exist", e);
      }

      ServiceGroup sg = cluster.getServiceGroup(serviceGroupName);
      Set<ServiceGroupKey> dependencies = sg.getServiceGroupDependencies();
      if (dependencies != null) {
        for (ServiceGroupKey serviceGroupKey : dependencies) {
          if (serviceGroupKey.getServiceGroupId() == dependentServiceGroupId) {
            throw new AmbariException("Service group " + serviceGroupName + " already have dependency for service group "
                    + serviceGroupKey.getServiceGroupName());
          }
        }
      }

    }
  }
}




