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


import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ClusterNotFoundException;
import org.apache.ambari.server.ClusterSettingNotFoundException;
import org.apache.ambari.server.DuplicateResourceException;
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.ParentObjectNotFoundException;
import org.apache.ambari.server.PropertyNotFoundException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ClusterSettingRequest;
import org.apache.ambari.server.controller.ClusterSettingResponse;
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
import org.apache.ambari.server.state.ClusterSetting;
import org.apache.ambari.server.state.Clusters;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import com.google.common.collect.Sets;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;


/**
 * Resource provider for Cluster Settings resource.
 **/

public class ClusterSettingResourceProvider extends AbstractControllerResourceProvider {


    // ----- Property ID constants ---------------------------------------------

    public static final String CLUSTER_SETTING_NAME_PROPERTY_ID = "cluster_setting_name";
    public static final String CLUSTER_SETTING_VALUE_PROPERTY_ID = "cluster_setting_value";

    public static final String RESPONSE_KEY = "ClusterSettingInfo";
    public static final String ALL_PROPERTIES = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "*";
    public static final String CLUSTER_SETTING_CLUSTER_ID_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "cluster_id";
    public static final String CLUSTER_SETTING_CLUSTER_NAME_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "cluster_name";
    public static final String CLUSTER_SETTING_CLUSTER_SETTING_ID_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "cluster_setting_id";
    public static final String CLUSTER_SETTING_CLUSTER_SETTING_NAME_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + CLUSTER_SETTING_NAME_PROPERTY_ID;
    public static final String CLUSTER_SETTING_CLUSTER_SETTING_VALUE_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + CLUSTER_SETTING_VALUE_PROPERTY_ID;

    private static final Set<String> pkPropertyIds =
            Sets.newHashSet(new String[]{
                    CLUSTER_SETTING_CLUSTER_NAME_PROPERTY_ID,
                    CLUSTER_SETTING_CLUSTER_SETTING_NAME_PROPERTY_ID});

    /**
     * The property ids for an cluster setting resource.
     */
    private static final Set<String> PROPERTY_IDS = new HashSet<>();

    /**
     * The key property ids for an cluster setting resource.
     */
    private static final Map<Resource.Type, String> KEY_PROPERTY_IDS = new HashMap<>();

    static {
        // properties
        PROPERTY_IDS.add(CLUSTER_SETTING_CLUSTER_ID_PROPERTY_ID);
        PROPERTY_IDS.add(CLUSTER_SETTING_CLUSTER_NAME_PROPERTY_ID);
        PROPERTY_IDS.add(CLUSTER_SETTING_CLUSTER_SETTING_ID_PROPERTY_ID);
        PROPERTY_IDS.add(CLUSTER_SETTING_CLUSTER_SETTING_NAME_PROPERTY_ID);
        PROPERTY_IDS.add(CLUSTER_SETTING_CLUSTER_SETTING_VALUE_PROPERTY_ID);

        // keys
        KEY_PROPERTY_IDS.put(Resource.Type.Cluster, CLUSTER_SETTING_CLUSTER_NAME_PROPERTY_ID);
        KEY_PROPERTY_IDS.put(Resource.Type.ClusterSetting, CLUSTER_SETTING_CLUSTER_SETTING_NAME_PROPERTY_ID);
    }

    // ----- Constructors ----------------------------------------------------

    /**
     * Create a  new resource provider for the given management controller.
     *
     * @param managementController the management controller
     */
    @AssistedInject
    public ClusterSettingResourceProvider(@Assisted AmbariManagementController managementController) {
        super(Resource.Type.ClusterSetting, PROPERTY_IDS, KEY_PROPERTY_IDS, managementController);
    }

    // ----- ResourceProvider ------------------------------------------------

    @Override
    protected RequestStatus createResourcesAuthorized(Request request)
            throws SystemException,
            UnsupportedPropertyException,
            ResourceAlreadyExistsException,
            NoSuchParentResourceException {

        final Set<ClusterSettingRequest> requests = new HashSet<>();
        for (Map<String, Object> propertyMap : request.getProperties()) {
            requests.add(getRequest(propertyMap));
        }
        Set<ClusterSettingResponse> createClusterSettings;
        createClusterSettings = createResources(new Command<Set<ClusterSettingResponse>>() {
            @Override
            public Set<ClusterSettingResponse> invoke() throws AmbariException, AuthorizationException {
                return createClusterSettings(requests);
            }
        });
        Set<Resource> associatedResources = new HashSet<>();
        if (createClusterSettings != null) {
            Iterator<ClusterSettingResponse> itr = createClusterSettings.iterator();
            while (itr.hasNext()) {
                ClusterSettingResponse response = itr.next();
                notifyCreate(Resource.Type.ClusterSetting, request);
                Resource resource = new ResourceImpl(Resource.Type.ClusterSetting);
                resource.setProperty(CLUSTER_SETTING_CLUSTER_ID_PROPERTY_ID, response.getClusterId());
                resource.setProperty(CLUSTER_SETTING_CLUSTER_NAME_PROPERTY_ID, response.getClusterName());
                resource.setProperty(CLUSTER_SETTING_CLUSTER_SETTING_ID_PROPERTY_ID, response.getClusterSettingId());
                resource.setProperty(CLUSTER_SETTING_CLUSTER_SETTING_NAME_PROPERTY_ID, response.getClusterSettingName());
                resource.setProperty(CLUSTER_SETTING_CLUSTER_SETTING_VALUE_PROPERTY_ID, response.getClusterSettingValue());

                associatedResources.add(resource);
            }
            return getRequestStatus(null, associatedResources);
        }

        return getRequestStatus(null);
    }

    @Override
    protected Set<Resource> getResourcesAuthorized(Request request, Predicate predicate) throws
            SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

        final Set<ClusterSettingRequest> requests = new HashSet<>();

        for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
            requests.add(getRequest(propertyMap));
        }

        Set<ClusterSettingResponse> responses = getResources(new Command<Set<ClusterSettingResponse>>() {
            @Override
            public Set<ClusterSettingResponse> invoke() throws AmbariException {
                return getClusterSettings(requests);
            }
        });

        Set<String> requestedIds = getRequestPropertyIds(request, predicate);
        Set<Resource> resources = new HashSet<>();

        for (ClusterSettingResponse response : responses) {
            Resource resource = new ResourceImpl(Resource.Type.ClusterSetting);
            setResourceProperty(resource, CLUSTER_SETTING_CLUSTER_ID_PROPERTY_ID,
                    response.getClusterId(), requestedIds);
            setResourceProperty(resource, CLUSTER_SETTING_CLUSTER_NAME_PROPERTY_ID,
                    response.getClusterName(), requestedIds);
            setResourceProperty(resource, CLUSTER_SETTING_CLUSTER_SETTING_ID_PROPERTY_ID,
                    response.getClusterSettingId(), requestedIds);
            setResourceProperty(resource, CLUSTER_SETTING_CLUSTER_SETTING_NAME_PROPERTY_ID,
                    response.getClusterSettingName(), requestedIds);
            setResourceProperty(resource, CLUSTER_SETTING_CLUSTER_SETTING_VALUE_PROPERTY_ID,
                    response.getClusterSettingValue(), requestedIds);
            resources.add(resource);
        }
        return resources;
    }

    @Override
    protected RequestStatus updateResourcesAuthorized(final Request request, Predicate predicate)
            throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

        final Set<ClusterSettingRequest> requests = new HashSet<>();
        for (Map<String, Object> propertyMap : request.getProperties()) {
            requests.add(getRequest(propertyMap));
        }
        Set<ClusterSettingResponse> createClusterSettings;
        createClusterSettings = modifyResources(new Command<Set<ClusterSettingResponse>>() {
            @Override
            public Set<ClusterSettingResponse> invoke() throws AmbariException, AuthorizationException {
                return updateClusterSettings(requests);
            }
        });
        Set<Resource> associatedResources = new HashSet<>();
        if (createClusterSettings != null) {
            Iterator<ClusterSettingResponse> itr = createClusterSettings.iterator();
            while (itr.hasNext()) {
                ClusterSettingResponse response = itr.next();
                notifyUpdate(Resource.Type.ClusterSetting, request, predicate);
                Resource resource = new ResourceImpl(Resource.Type.ClusterSetting);
                resource.setProperty(CLUSTER_SETTING_CLUSTER_ID_PROPERTY_ID, response.getClusterId());
                resource.setProperty(CLUSTER_SETTING_CLUSTER_NAME_PROPERTY_ID, response.getClusterName());
                resource.setProperty(CLUSTER_SETTING_CLUSTER_SETTING_ID_PROPERTY_ID, response.getClusterSettingId());
                resource.setProperty(CLUSTER_SETTING_CLUSTER_SETTING_NAME_PROPERTY_ID, response.getClusterSettingName());
                resource.setProperty(CLUSTER_SETTING_CLUSTER_SETTING_VALUE_PROPERTY_ID, response.getClusterSettingValue());

                associatedResources.add(resource);
            }
            return getRequestStatus(null, associatedResources);
        }

        return getRequestStatus(null);
    }

    @Override
    protected RequestStatus deleteResourcesAuthorized(Request request, Predicate predicate)
            throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

        final Set<ClusterSettingRequest> requests = new HashSet<>();

        for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
            requests.add(getRequest(propertyMap));
        }
        DeleteStatusMetaData deleteStatusMetaData;
        deleteStatusMetaData = modifyResources(new Command<DeleteStatusMetaData>() {
            @Override
            public DeleteStatusMetaData invoke() throws AmbariException, AuthorizationException {
                deleteClusterSettings(requests);
                return new DeleteStatusMetaData();
            }
        });

        notifyDelete(Resource.Type.ClusterSetting, predicate);
        for(ClusterSettingRequest settingReq : requests) {
            deleteStatusMetaData.addDeletedKey("cluster_name: "+settingReq.getClusterName() + ", " + "cluster_setting_name: "+settingReq.getClusterSettingName());
        }
        return getRequestStatus(null, null, deleteStatusMetaData);
    }

    // ----- AbstractResourceProvider ----------------------------------------

    @Override
    protected Set<String> getPKPropertyIds() {
        return pkPropertyIds;
    }

    // ----- utility methods -------------------------------------------------

    /**
     * Get a cluster setting request object from a map of property values.
     *
     * @param properties the predicate
     * @return the service request object
     */
    private ClusterSettingRequest getRequest(Map<String, Object> properties) {
        String clusterName = (String) properties.get(CLUSTER_SETTING_CLUSTER_NAME_PROPERTY_ID);
        String clusterSettingName = (String) properties.get(CLUSTER_SETTING_CLUSTER_SETTING_NAME_PROPERTY_ID);
        String clusterSettingValue = (String) properties.get(CLUSTER_SETTING_CLUSTER_SETTING_VALUE_PROPERTY_ID);
        return  new ClusterSettingRequest(clusterName, clusterSettingName, clusterSettingValue);
    }

    // Create 'cluster setting' based on the given request.
    private synchronized Set<ClusterSettingResponse> createClusterSettings(Set<ClusterSettingRequest> requests)
            throws AmbariException, AuthorizationException {

        if (requests.isEmpty()) {
            LOG.warn("Received an empty requests set");
            return null;
        }
        AmbariManagementController controller = getManagementController();
        Clusters clusters = controller.getClusters();

        // do all validation checks
        validateCreateRequests(requests, clusters);

        Set<ClusterSettingResponse> createdClusterSettings = new HashSet<>();
        for (ClusterSettingRequest request : requests) {
            Cluster cluster = clusters.getCluster(request.getClusterName());

            // Already checked that 'cluster setting' does not exist
            ClusterSetting cs = cluster.addClusterSetting(request.getClusterSettingName(), request.getClusterSettingValue());
            createdClusterSettings.add(cs.convertToResponse());
        }
        return createdClusterSettings;
    }

    // update 'cluster setting' based on the given request.
    private synchronized Set<ClusterSettingResponse> updateClusterSettings(Set<ClusterSettingRequest> requests)
            throws AmbariException, AuthorizationException {

        if (requests.isEmpty()) {
            LOG.warn("Received an empty requests set");
            return null;
        }
        AmbariManagementController controller = getManagementController();
        Clusters clusters = controller.getClusters();

        // do all validation checks
        validateUpdateRequests(requests, clusters);

        Set<ClusterSettingResponse> updatedClusterSettings = new HashSet<>();
        for (ClusterSettingRequest request : requests) {
            Cluster cluster = clusters.getCluster(request.getClusterName());

            // Already checked that 'cluster setting' exists
            ClusterSetting cs = cluster.updateClusterSetting(request.getClusterSettingName(), request.getClusterSettingValue());
            updatedClusterSettings.add(cs.convertToResponse());
        }
        return updatedClusterSettings;
    }

    // Get 'cluster settings' from the given set of requests.
    private Set<ClusterSettingResponse> getClusterSettings(Set<ClusterSettingRequest> requests)
            throws AmbariException {
        Set<ClusterSettingResponse> response = new HashSet<>();
        for (ClusterSettingRequest request : requests) {
            try {
                response.addAll(getClusterSettings(request));
            } catch (ClusterSettingNotFoundException e) {
                if (requests.size() == 1) {
                    // only throw exception if 1 request.
                    // there will be > 1 request in case of OR predicate
                    throw e;
                }
            }
        }
        return response;
    }

    // Get 'cluster settings' from the given request.
    private Set<ClusterSettingResponse> getClusterSettings(ClusterSettingRequest request)
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

        Set<ClusterSettingResponse> response = new HashSet<>();
        if (request.getClusterSettingName() != null) {
            ClusterSetting clusterSetting = cluster.getClusterSetting(request.getClusterSettingName());
            ClusterSettingResponse clusterSettingResponse = clusterSetting.convertToResponse();

            response.add(clusterSettingResponse);
            return response;
        }

        for (ClusterSetting clusterSetting : cluster.getClusterSettings().values()) {
            ClusterSettingResponse clusterSettingResponse = clusterSetting.convertToResponse();
            response.add(clusterSettingResponse);
        }
        return response;
    }


    // Delete 'cluster setting' based on the given set of requests
    private void deleteClusterSettings(Set<ClusterSettingRequest> request)
            throws AmbariException, AuthorizationException {

        Clusters clusters = getManagementController().getClusters();

        Set<ClusterSetting> removable = new HashSet<>();

        for (ClusterSettingRequest clusterSettingRequest : request) {
            if (null == clusterSettingRequest.getClusterName()
                    || StringUtils.isEmpty(clusterSettingRequest.getClusterSettingName())) {
                // FIXME throw correct error
                throw new AmbariException("invalid arguments");
            } else {

                // TODO : IS 'CLUSTER_MODIFY_CONFIGS' the correct authorization field to be used ?
                if (!AuthorizationHelper.isAuthorized(
                        ResourceType.CLUSTER, getClusterResourceId(clusterSettingRequest.getClusterName()),
                        RoleAuthorization.CLUSTER_MODIFY_CONFIGS)) {
                    throw new AuthorizationException("The user is not authorized to delete service groups");
                }

                ClusterSetting clusterSetting = clusters.getCluster(
                        clusterSettingRequest.getClusterName()).getClusterSetting(
                        clusterSettingRequest.getClusterSettingName());

                removable.add(clusterSetting);
            }
        }

        for (ClusterSetting clusterSetting : removable) {
            clusterSetting.getCluster().deleteClusterSetting(clusterSetting.getClusterSettingName());
        }
    }


    private void validateCreateRequests(Set<ClusterSettingRequest> requests, Clusters clusters)
            throws AuthorizationException, AmbariException {

        Map<String, Set<String>> clusterSettingNames = new HashMap<>();
        Set<String> duplicates = new HashSet<>();
        for (ClusterSettingRequest request : requests) {
            final String clusterName = request.getClusterName();
            final String clusterSettingName = request.getClusterSettingName();

            Validate.notNull(clusterName, "Cluster name should be provided when creating a 'cluster setting'");
            Validate.notEmpty(clusterSettingName, "'Cluster Setting name' should be provided when creating a 'cluster setting'");

            LOG.debug("Received a createClusterSetting request, clusterName= {}, clusterSettingName : {}, " +
                    "request : {}", clusterName, clusterSettingName, request);

            if (!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER,
                    getClusterResourceId(clusterName), RoleAuthorization.CLUSTER_MODIFY_CONFIGS)) {
                throw new AuthorizationException("The user is not authorized to add/create cluster settings");
            }

            clusterSettingNames.computeIfAbsent(clusterName, k -> new HashSet<>());

            if (clusterSettingNames.get(clusterName).contains(clusterSettingName)) {
                // throw error later for dup
                duplicates.add(clusterSettingName);
                continue;
            }
            clusterSettingNames.get(clusterName).add(clusterSettingName);

            Cluster cluster;
            try {
                cluster = clusters.getCluster(clusterName);
            } catch (ClusterNotFoundException e) {
                throw new ParentObjectNotFoundException("Attempted to add a 'cluster setting' to a cluster which doesn't exist", e);
            }
            try {
                ClusterSetting cs = cluster.getClusterSetting(clusterSettingName);
                if (cs != null) {
                    // throw error later for dup
                    duplicates.add(clusterSettingName);
                    continue;
                }
            } catch (ClusterSettingNotFoundException e) {
                // Expected
            }
        }
        // ensure only a single cluster update
        if (clusterSettingNames.size() != 1) {
            throw new IllegalArgumentException("Invalid arguments, updates allowed" +
                    "on only one cluster at a time");
        }

        // Validate dups
        if (!duplicates.isEmpty()) {
            String clusterName = requests.iterator().next().getClusterName();
            String msg = "Attempted to add/create a 'cluster setting' which already exists: " +
                    ", clusterName=" + clusterName + " clusterSettingName=" + StringUtils.join(duplicates, ",");

            throw new DuplicateResourceException(msg);
        }
    }

    private void validateUpdateRequests(Set<ClusterSettingRequest> requests, Clusters clusters)
            throws AuthorizationException, AmbariException {

        Map<String, Set<String>> clusterSettingNames = new HashMap<>();
        Set<String> duplicates = new HashSet<>();
        Set<String> nonExisting = new HashSet<>();
        for (ClusterSettingRequest request : requests) {
            final String clusterName = request.getClusterName();
            final String clusterSettingName = request.getClusterSettingName();

            Validate.notNull(clusterName, "Cluster name should be provided when updating a 'cluster setting'");
            Validate.notEmpty(clusterSettingName, "'Cluster Setting name' should be provided when creating a 'cluster setting'");

            LOG.debug("Received a updateClusterSetting request, clusterName= {}, clusterSettingName : {}, " +
                    "request : {}", clusterName, clusterSettingName, request);

            if (!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER,
                    getClusterResourceId(clusterName), RoleAuthorization.CLUSTER_MODIFY_CONFIGS)) {
                throw new AuthorizationException("The user is not authorized to update cluster settings");
            }

            clusterSettingNames.computeIfAbsent(clusterName, k -> new HashSet<>());

            if (clusterSettingNames.get(clusterName).contains(clusterSettingName)) {
                // throw error later for dup
                duplicates.add(clusterSettingName);
                continue;
            }
            clusterSettingNames.get(clusterName).add(clusterSettingName);

            Cluster cluster;
            try {
                cluster = clusters.getCluster(clusterName);
            } catch (ClusterNotFoundException e) {
                throw new ParentObjectNotFoundException("Attempted to update a 'cluster setting' to a cluster which doesn't exist", e);
            }
            try {
                ClusterSetting cs = cluster.getClusterSetting(clusterSettingName);
                if (cs == null) {
                    // throw error later for it not being present.
                    nonExisting.add(clusterSettingName);
                    continue;
                }
            } catch (ClusterSettingNotFoundException e) {
                // Expected
            }
        }
        // ensure only a single cluster update
        if (clusterSettingNames.size() != 1) {
            throw new IllegalArgumentException("Invalid arguments, updates allowed" +
                    "on only one cluster at a time");
        }

        // Validate dups
        if (!duplicates.isEmpty()) {
            String clusterName = requests.iterator().next().getClusterName();
            String msg = "Attempted to update a 'cluster setting' which has more than one occurrence: " +
                    ", clusterName=" + clusterName + " clusterSettingName=" + StringUtils.join(nonExisting, ",");

            throw new DuplicateResourceException(msg);
        }

        // Validate non existing one(s)
        if (!nonExisting.isEmpty()) {
            String clusterName = requests.iterator().next().getClusterName();
            String msg = "Attempted to update a 'cluster setting' which doesn't exist: " +
                    ", clusterName=" + clusterName + " clusterSettingName=" + StringUtils.join(nonExisting, ",");

            throw new PropertyNotFoundException(msg);
        }
    }
}
