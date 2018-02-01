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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ClusterNotFoundException;
import org.apache.ambari.server.DuplicateResourceException;
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.ParentObjectNotFoundException;
import org.apache.ambari.server.ServiceGroupNotFoundException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.ServiceGroupRequest;
import org.apache.ambari.server.controller.ServiceGroupResponse;
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

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * Resource provider for service resources.
 **/

public class ServiceGroupResourceProvider extends AbstractControllerResourceProvider {


  // ----- Property ID constants ---------------------------------------------

  public static final String RESPONSE_KEY = "ServiceGroupInfo";
  public static final String ALL_PROPERTIES = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "*";
  public static final String SERVICE_GROUP_CLUSTER_ID_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "cluster_id";
  public static final String SERVICE_GROUP_CLUSTER_NAME_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "cluster_name";
  public static final String SERVICE_GROUP_SERVICE_GROUP_ID_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "service_group_id";
  public static final String SERVICE_GROUP_SERVICE_GROUP_NAME_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "service_group_name";
  public static final String SERVICE_GROUP_MPACKNAME_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "mpacks";


  private static Set<String> pkPropertyIds =
    new HashSet<String>(Arrays.asList(new String[]{
      SERVICE_GROUP_CLUSTER_NAME_PROPERTY_ID,
      SERVICE_GROUP_SERVICE_GROUP_NAME_PROPERTY_ID}));

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
    PROPERTY_IDS.add(SERVICE_GROUP_CLUSTER_ID_PROPERTY_ID);
    PROPERTY_IDS.add(SERVICE_GROUP_CLUSTER_NAME_PROPERTY_ID);
    PROPERTY_IDS.add(SERVICE_GROUP_SERVICE_GROUP_ID_PROPERTY_ID);
    PROPERTY_IDS.add(SERVICE_GROUP_SERVICE_GROUP_NAME_PROPERTY_ID);
    PROPERTY_IDS.add(SERVICE_GROUP_MPACKNAME_PROPERTY_ID);

    // keys
    KEY_PROPERTY_IDS.put(Resource.Type.Cluster, SERVICE_GROUP_CLUSTER_NAME_PROPERTY_ID);
    KEY_PROPERTY_IDS.put(Resource.Type.ServiceGroup, SERVICE_GROUP_SERVICE_GROUP_NAME_PROPERTY_ID);
    KEY_PROPERTY_IDS.put(Resource.Type.Mpack, SERVICE_GROUP_MPACKNAME_PROPERTY_ID);
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
  public ServiceGroupResourceProvider(@Assisted AmbariManagementController managementController) {
    super(Resource.Type.ServiceGroup, PROPERTY_IDS, KEY_PROPERTY_IDS, managementController);
  }

  // ----- ResourceProvider ------------------------------------------------

  @Override
  protected RequestStatus createResourcesAuthorized(Request request)
    throws SystemException,
    UnsupportedPropertyException,
    ResourceAlreadyExistsException,
    NoSuchParentResourceException, IllegalArgumentException{

    final Set<ServiceGroupRequest> requests = new HashSet<>();
    for (Map<String, Object> propertyMap : request.getProperties()) {
      requests.add(getRequest(propertyMap));
    }
    Set<ServiceGroupResponse> createServiceGroups = null;
    createServiceGroups = createResources(new Command<Set<ServiceGroupResponse>>() {
      @Override
      public Set<ServiceGroupResponse> invoke() throws AmbariException, AuthorizationException, IllegalArgumentException {
        return createServiceGroups(requests);
      }
    });
    Set<Resource> associatedResources = new HashSet<>();
    if (createServiceGroups != null) {
      Iterator<ServiceGroupResponse> itr = createServiceGroups.iterator();
      while (itr.hasNext()) {
        ServiceGroupResponse response = itr.next();
        notifyCreate(Resource.Type.ServiceGroup, request);
        Resource resource = new ResourceImpl(Resource.Type.ServiceGroup);
        resource.setProperty(SERVICE_GROUP_CLUSTER_ID_PROPERTY_ID, response.getClusterId());
        resource.setProperty(SERVICE_GROUP_CLUSTER_NAME_PROPERTY_ID, response.getClusterName());
        resource.setProperty(SERVICE_GROUP_SERVICE_GROUP_ID_PROPERTY_ID, response.getServiceGroupId());
        resource.setProperty(SERVICE_GROUP_SERVICE_GROUP_NAME_PROPERTY_ID, response.getServiceGroupName());
        resource.setProperty(SERVICE_GROUP_MPACKNAME_PROPERTY_ID, response.getMpackNames());

        associatedResources.add(resource);
      }
      return getRequestStatus(null, associatedResources);
    }

    return getRequestStatus(null);
  }

  @Override
  protected Set<Resource> getResourcesAuthorized(Request request, Predicate predicate) throws
    SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<ServiceGroupRequest> requests = new HashSet<>();

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      requests.add(getRequest(propertyMap));
    }

    Set<ServiceGroupResponse> responses = getResources(new Command<Set<ServiceGroupResponse>>() {
      @Override
      public Set<ServiceGroupResponse> invoke() throws AmbariException {
        return getServiceGroups(requests);
      }
    });

    Set<String> requestedIds = getRequestPropertyIds(request, predicate);
    Set<Resource> resources = new HashSet<Resource>();

    for (ServiceGroupResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.ServiceGroup);
      setResourceProperty(resource, SERVICE_GROUP_CLUSTER_ID_PROPERTY_ID,
        response.getClusterId(), requestedIds);
      setResourceProperty(resource, SERVICE_GROUP_CLUSTER_NAME_PROPERTY_ID,
        response.getClusterName(), requestedIds);
      setResourceProperty(resource, SERVICE_GROUP_SERVICE_GROUP_ID_PROPERTY_ID,
        response.getServiceGroupId(), requestedIds);
      setResourceProperty(resource, SERVICE_GROUP_SERVICE_GROUP_NAME_PROPERTY_ID,
        response.getServiceGroupName(), requestedIds);
      setResourceProperty(resource, SERVICE_GROUP_MPACKNAME_PROPERTY_ID,
          response.getMpackNames(), requestedIds);
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

    final Set<ServiceGroupRequest> requests = new HashSet<>();
    DeleteStatusMetaData deleteStatusMetaData = null;

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      requests.add(getRequest(propertyMap));
    }
    deleteStatusMetaData = modifyResources(new Command<DeleteStatusMetaData>() {
      @Override
      public DeleteStatusMetaData invoke() throws AmbariException, AuthorizationException {
        deleteServiceGroups(requests);
        return new DeleteStatusMetaData();
      }
    });

    notifyDelete(Resource.Type.ServiceGroup, predicate);
    for(ServiceGroupRequest svgReq : requests) {
      deleteStatusMetaData.addDeletedKey("cluster_name: "+svgReq.getClusterName() + ", " + "service_group_name: "+svgReq.getServiceGroupName());
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
  private ServiceGroupRequest getRequest(Map<String, Object> properties) {
    String clusterName = (String) properties.get(SERVICE_GROUP_CLUSTER_NAME_PROPERTY_ID);
    String serviceGroupName = (String) properties.get(SERVICE_GROUP_SERVICE_GROUP_NAME_PROPERTY_ID);
    ServiceGroupRequest svcRequest = new ServiceGroupRequest(clusterName, serviceGroupName);
    Collection<Map<String,String>> mpackNames = (Collection<Map<String,String>>) properties.get(SERVICE_GROUP_MPACKNAME_PROPERTY_ID);
    if (mpackNames != null) {
      Set<String> mpackNamesSet = mpackNames.stream().flatMap(mpack -> mpack.values().stream()).collect(Collectors.toSet());
      svcRequest.addMpackNames(mpackNamesSet);
    }
    return svcRequest;
  }

  // Create services from the given request.
  public synchronized Set<ServiceGroupResponse> createServiceGroups(Set<ServiceGroupRequest> requests)
    throws AmbariException, AuthorizationException, IllegalArgumentException {

    if (requests.isEmpty()) {
      LOG.warn("Received an empty requests set");
      return null;
    }
    AmbariManagementController controller = getManagementController();
    Clusters clusters = controller.getClusters();

    // do all validation checks
    validateCreateRequests(requests, clusters);

    Set<ServiceGroupResponse> createdSvcGrps = new HashSet<>();
    for (ServiceGroupRequest request : requests) {
      Cluster cluster = clusters.getCluster(request.getClusterName());

      // Already checked that service group does not exist
      ServiceGroup sg = cluster.addServiceGroup(request.getServiceGroupName());
      sg.setMpackNames(request.getMpackNames());
      createdSvcGrps.add(sg.convertToResponse());
    }
    return createdSvcGrps;
  }

  // Get services from the given set of requests.
  protected Set<ServiceGroupResponse> getServiceGroups(Set<ServiceGroupRequest> requests)
    throws AmbariException {
    Set<ServiceGroupResponse> response = new HashSet<ServiceGroupResponse>();
    for (ServiceGroupRequest request : requests) {
      try {
        response.addAll(getServiceGroups(request));
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

  // Get services groups from the given request.
  private Set<ServiceGroupResponse> getServiceGroups(ServiceGroupRequest request)
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

    Set<ServiceGroupResponse> response = new HashSet<>();
    if (request.getServiceGroupName() != null) {
      ServiceGroup sg = cluster.getServiceGroup(request.getServiceGroupName());
      ServiceGroupResponse serviceGroupResponse = sg.convertToResponse();

      response.add(serviceGroupResponse);
      return response;
    }

    for (ServiceGroup sg : cluster.getServiceGroups().values()) {
      ServiceGroupResponse serviceGroupResponse = sg.convertToResponse();
      response.add(serviceGroupResponse);
    }
    return response;
  }


  // Delete services groups based on the given set of requests
  protected void deleteServiceGroups(Set<ServiceGroupRequest> request)
    throws AmbariException, AuthorizationException {

    Clusters clusters = getManagementController().getClusters();

    Set<ServiceGroup> removable = new HashSet<>();

    for (ServiceGroupRequest serviceGroupRequest : request) {
      if (null == serviceGroupRequest.getClusterName()
        || StringUtils.isEmpty(serviceGroupRequest.getServiceGroupName())) {
        // FIXME throw correct error
        throw new AmbariException("invalid arguments");
      } else {

        if (!AuthorizationHelper.isAuthorized(
          ResourceType.CLUSTER, getClusterResourceId(serviceGroupRequest.getClusterName()),
          RoleAuthorization.SERVICE_ADD_DELETE_SERVICES)) {
          throw new AuthorizationException("The user is not authorized to delete service groups");
        }

        ServiceGroup serviceGroup = clusters.getCluster(
          serviceGroupRequest.getClusterName()).getServiceGroup(
          serviceGroupRequest.getServiceGroupName());

        // TODO: Add check to validate there are no services in the service group
        removable.add(serviceGroup);
      }
    }

    for (ServiceGroup serviceGroup : removable) {
      serviceGroup.getCluster().deleteServiceGroup(serviceGroup.getServiceGroupName());
    }

    return;
  }


  private void validateCreateRequests(Set<ServiceGroupRequest> requests, Clusters clusters)
    throws AuthorizationException, AmbariException, IllegalArgumentException {

    AmbariMetaInfo ambariMetaInfo = getManagementController().getAmbariMetaInfo();
    Map<String, Set<String>> serviceGroupNames = new HashMap<>();
    Set<String> duplicates = new HashSet<>();
    for (ServiceGroupRequest request : requests) {
      final String clusterName = request.getClusterName();
      final String serviceGroupName = request.getServiceGroupName();

      Validate.notNull(clusterName, "Cluster name should be provided when creating a service group");
      Validate.notEmpty(serviceGroupName, "Service group name should be provided when creating a service group");

      if (LOG.isDebugEnabled()) {
        LOG.debug("Received a createServiceGroup request" +
          ", clusterName=" + clusterName + ", serviceGroupName=" + serviceGroupName + ", request=" + request);
      }

      if (!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER,
        getClusterResourceId(clusterName), RoleAuthorization.SERVICE_ADD_DELETE_SERVICES)) {
        throw new AuthorizationException("The user is not authorized to create service groups");
      }

      if (!serviceGroupNames.containsKey(clusterName)) {
        serviceGroupNames.put(clusterName, new HashSet<String>());
      }

      if (serviceGroupNames.get(clusterName).contains(serviceGroupName)) {
        // throw error later for dup
        duplicates.add(serviceGroupName);
        continue;
      }
      serviceGroupNames.get(clusterName).add(serviceGroupName);

      if (request.getMpackNames().size() != 1) {
        String errmsg = "Invalid arguments, " + request.getMpackNames().size() + " mpack(s) found in the service group " + serviceGroupName + ", only one mpack is allowed";
        throw new IllegalArgumentException(errmsg);
      }

      Cluster cluster;
      try {
        cluster = clusters.getCluster(clusterName);
      } catch (ClusterNotFoundException e) {
        throw new ParentObjectNotFoundException("Attempted to add a service group to a cluster which doesn't exist", e);
      }
      try {
        ServiceGroup sg = cluster.getServiceGroup(serviceGroupName);
        if (sg != null) {
          // throw error later for dup
          duplicates.add(serviceGroupName);
          continue;
        }
      } catch (ServiceGroupNotFoundException e) {
        // Expected
      }
    }
    // ensure only a single cluster update
    if (serviceGroupNames.size() != 1) {
      throw new IllegalArgumentException("Invalid arguments, updates allowed" +
        "on only one cluster at a time");
    }

    // Validate dups
    if (!duplicates.isEmpty()) {
      String clusterName = requests.iterator().next().getClusterName();
      String msg = "Attempted to create a service group which already exists: " +
        ", clusterName=" + clusterName + " serviceGroupName=" + StringUtils.join(duplicates, ",");

      throw new DuplicateResourceException(msg);
    }
  }
}
