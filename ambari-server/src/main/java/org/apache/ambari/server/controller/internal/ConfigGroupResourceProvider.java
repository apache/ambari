/*
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ClusterNotFoundException;
import org.apache.ambari.server.ConfigGroupNotFoundException;
import org.apache.ambari.server.DuplicateResourceException;
import org.apache.ambari.server.HostNotFoundException;
import org.apache.ambari.server.ParentObjectNotFoundException;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ConfigGroupRequest;
import org.apache.ambari.server.controller.ConfigGroupResponse;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.ResourcePredicateEvaluator;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.configgroup.ConfigGroup;
import org.apache.ambari.server.state.configgroup.ConfigGroupFactory;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

@StaticallyInject
public class ConfigGroupResourceProvider extends
  AbstractControllerResourceProvider implements ResourcePredicateEvaluator {

  private static final Logger configLogger = LoggerFactory.getLogger("configchange");
  private static final Logger LOG = LoggerFactory.getLogger
    (ConfigGroupResourceProvider.class);

  protected static final String CONFIGGROUP_CLUSTER_NAME_PROPERTY_ID =
    PropertyHelper.getPropertyId("ConfigGroup", "cluster_name");
  protected static final String CONFIGGROUP_ID_PROPERTY_ID = PropertyHelper
    .getPropertyId("ConfigGroup", "id");
  protected static final String CONFIGGROUP_NAME_PROPERTY_ID = PropertyHelper
    .getPropertyId("ConfigGroup", "group_name");
  protected static final String CONFIGGROUP_TAG_PROPERTY_ID = PropertyHelper
    .getPropertyId("ConfigGroup", "tag");
  protected static final String CONFIGGROUP_SERVICEGROUPNAME_PROPERTY_ID = PropertyHelper
    .getPropertyId("ConfigGroup", "service_group_name");
  protected static final String CONFIGGROUP_SERVICENAME_PROPERTY_ID = PropertyHelper
    .getPropertyId("ConfigGroup", "service_name");
  protected static final String CONFIGGROUP_DESC_PROPERTY_ID = PropertyHelper
    .getPropertyId("ConfigGroup", "description");
  protected static final String CONFIGGROUP_SCV_NOTE_ID = PropertyHelper
      .getPropertyId("ConfigGroup", "service_config_version_note");
  protected static final String CONFIGGROUP_HOSTNAME_PROPERTY_ID =
    PropertyHelper.getPropertyId(null, "host_name");
  protected static final String CONFIGGROUP_HOSTS_HOSTNAME_PROPERTY_ID =
    PropertyHelper.getPropertyId("ConfigGroup", "hosts/host_name");
  public static final String CONFIGGROUP_HOSTS_PROPERTY_ID = PropertyHelper
    .getPropertyId("ConfigGroup", "hosts");
  public static final String CONFIGGROUP_CONFIGS_PROPERTY_ID =
    PropertyHelper.getPropertyId("ConfigGroup", "desired_configs");
  public static final String CONFIGGROUP_VERSION_TAGS_PROPERTY_ID =
    PropertyHelper.getPropertyId("ConfigGroup", "version_tags");

  private static Set<String> pkPropertyIds = new HashSet<>(Arrays
    .asList(new String[]{CONFIGGROUP_ID_PROPERTY_ID}));

  @Inject
  private static HostDAO hostDAO;

  /**
   * Used for creating {@link Config} instances to return in the REST response.
   */
  @Inject
  private static ConfigFactory configFactory;

  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param propertyIds          the property ids
   * @param keyPropertyIds       the key property ids
   * @param managementController the management controller
   */
  protected ConfigGroupResourceProvider(Set<String> propertyIds,
       Map<Resource.Type, String> keyPropertyIds,
       AmbariManagementController managementController) {
    super(propertyIds, keyPropertyIds, managementController);

    EnumSet<RoleAuthorization> manageGroupsAuthSet =
        EnumSet.of(RoleAuthorization.SERVICE_MANAGE_CONFIG_GROUPS, RoleAuthorization.CLUSTER_MANAGE_CONFIG_GROUPS);

    setRequiredCreateAuthorizations(manageGroupsAuthSet);
    setRequiredDeleteAuthorizations(manageGroupsAuthSet);
    setRequiredUpdateAuthorizations(manageGroupsAuthSet);


    setRequiredGetAuthorizations(EnumSet.of(RoleAuthorization.CLUSTER_VIEW_CONFIGS,
        RoleAuthorization.CLUSTER_MANAGE_CONFIG_GROUPS, RoleAuthorization.SERVICE_VIEW_CONFIGS,
        RoleAuthorization.SERVICE_MANAGE_CONFIG_GROUPS, RoleAuthorization.SERVICE_COMPARE_CONFIGS));
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

  @Override
  public RequestStatus createResourcesAuthorized(Request request) throws
       SystemException, UnsupportedPropertyException,
       ResourceAlreadyExistsException, NoSuchParentResourceException {

    final Set<ConfigGroupRequest> requests = new HashSet<>();
    for (Map<String, Object> propertyMap : request.getProperties()) {
      requests.add(getConfigGroupRequest(propertyMap));
    }
    RequestStatus status = createResources(requests);
    notifyCreate(Resource.Type.ConfigGroup, request);
    return status;
  }

  @Override
  public Set<Resource> getResourcesAuthorized(Request request, Predicate predicate) throws
       SystemException, UnsupportedPropertyException, NoSuchResourceException,
       NoSuchParentResourceException {

    final Set<ConfigGroupRequest> requests = new HashSet<>();
    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      requests.add(getConfigGroupRequest(propertyMap));
    }

    Set<ConfigGroupResponse> responses = getResources(new Command<Set<ConfigGroupResponse>>() {
      @Override
      public Set<ConfigGroupResponse> invoke() throws AmbariException {
        return getConfigGroups(requests);
      }
    });

    Set<String>   requestedIds = getRequestPropertyIds(request, predicate);
    Set<Resource> resources    = new HashSet<>();

    if (requestedIds.contains(CONFIGGROUP_HOSTS_HOSTNAME_PROPERTY_ID)) {
      requestedIds.add(CONFIGGROUP_HOSTS_PROPERTY_ID);
    }

    for (ConfigGroupResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.ConfigGroup);

      setResourceProperty(resource, CONFIGGROUP_ID_PROPERTY_ID,
        response.getId(), requestedIds);
      setResourceProperty(resource, CONFIGGROUP_CLUSTER_NAME_PROPERTY_ID,
        response.getClusterName(), requestedIds);
      setResourceProperty(resource, CONFIGGROUP_NAME_PROPERTY_ID,
        response.getGroupName(), requestedIds);
      setResourceProperty(resource, CONFIGGROUP_TAG_PROPERTY_ID,
        response.getTag(), requestedIds);
      setResourceProperty(resource, CONFIGGROUP_DESC_PROPERTY_ID,
        response.getDescription(), requestedIds);
      setResourceProperty(resource, CONFIGGROUP_HOSTS_PROPERTY_ID,
        response.getHosts(), requestedIds);
      setResourceProperty(resource, CONFIGGROUP_CONFIGS_PROPERTY_ID,
        response.getConfigurations(), requestedIds);

      resources.add(resource);
    }

    return resources;
  }

  @Override
  public RequestStatus updateResourcesAuthorized(Request request, Predicate predicate) throws
       SystemException, UnsupportedPropertyException,
       NoSuchResourceException, NoSuchParentResourceException {

    final Set<ConfigGroupRequest> requests = new HashSet<>();

    Iterator<Map<String,Object>> iterator = request.getProperties().iterator();
    if (iterator.hasNext()) {
      for (Map<String, Object> propertyMap : getPropertyMaps(iterator.next(), predicate)) {
        requests.add(getConfigGroupRequest(propertyMap));
      }
    }

    RequestStatus status = updateResources(requests);

    Set<Resource> associatedResources = new HashSet<>();
    for (ConfigGroupRequest configGroupRequest : requests) {
      ConfigGroupResponse configGroupResponse = getManagementController().getConfigGroupUpdateResults(configGroupRequest);
      Resource resource = new ResourceImpl(Resource.Type.ConfigGroup);

      resource.setProperty(CONFIGGROUP_ID_PROPERTY_ID, configGroupResponse.getId());
      resource.setProperty(CONFIGGROUP_CLUSTER_NAME_PROPERTY_ID, configGroupResponse.getClusterName());
      resource.setProperty(CONFIGGROUP_NAME_PROPERTY_ID, configGroupResponse.getGroupName());
      resource.setProperty(CONFIGGROUP_TAG_PROPERTY_ID, configGroupResponse.getTag());
      resource.setProperty(CONFIGGROUP_VERSION_TAGS_PROPERTY_ID, configGroupResponse.getVersionTags());

      associatedResources.add(resource);
    }

    notifyUpdate(Resource.Type.ConfigGroup, request, predicate);

    return getRequestStatus(null, associatedResources);
  }

  @Override
  public RequestStatus deleteResourcesAuthorized(Request request, Predicate predicate) throws
       SystemException, UnsupportedPropertyException, NoSuchResourceException,
       NoSuchParentResourceException {

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      final ConfigGroupRequest configGroupRequest = getConfigGroupRequest(propertyMap);

      modifyResources(new Command<Void>() {
        @Override
        public Void invoke() throws AmbariException, AuthorizationException {
          deleteConfigGroup(configGroupRequest);
          return null;
        }
      });
    }

    notifyDelete(Resource.Type.ConfigGroup, predicate);

    return getRequestStatus(null);
  }

  @Override
  public Set<String> checkPropertyIds(Set<String> propertyIds) {
    //allow providing service_config_version_note, but we should not return it for config group
    Set<String> unsupportedPropertyIds = super.checkPropertyIds(propertyIds);
    for (Iterator<String> iterator = unsupportedPropertyIds.iterator(); iterator.hasNext(); ) {
      String next = iterator.next();
      next = PropertyHelper.getPropertyName(next);
      if (next.equals("service_config_version_note") || next.equals("/service_config_version_note")) {
        iterator.remove();
      }
    }
    return unsupportedPropertyIds;
  }

  /**
   * Create configuration group resources based on set of config group requests.
   *
   * @param requests  set of config group requests
   *
   * @return a request status
   *
   * @throws SystemException                an internal system exception occurred
   * @throws UnsupportedPropertyException   the request contains unsupported property ids
   * @throws ResourceAlreadyExistsException attempted to create a resource which already exists
   * @throws NoSuchParentResourceException  a parent resource of the resource to create doesn't exist
   */
  public RequestStatus createResources(final Set<ConfigGroupRequest> requests)throws
      SystemException, UnsupportedPropertyException,
      ResourceAlreadyExistsException, NoSuchParentResourceException{

    Set<ConfigGroupResponse> responses =
        createResources(new Command<Set<ConfigGroupResponse>>() {
          @Override
          public Set<ConfigGroupResponse> invoke() throws AmbariException, AuthorizationException {
            return createConfigGroups(requests);
          }
        });

    Set<Resource> associatedResources = new HashSet<>();
    for (ConfigGroupResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.ConfigGroup);
      resource.setProperty(CONFIGGROUP_ID_PROPERTY_ID, response.getId());
      associatedResources.add(resource);
    }

    return getRequestStatus(null, associatedResources);
  }

  public RequestStatus updateResources(final Set<ConfigGroupRequest> requests)
      throws SystemException,
      UnsupportedPropertyException,
      NoSuchResourceException,
      NoSuchParentResourceException {

    modifyResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException, AuthorizationException {
        updateConfigGroups(requests);
        return null;
      }
    });

    return getRequestStatus(null);
  }

  private synchronized  Set<ConfigGroupResponse> getConfigGroups
    (Set<ConfigGroupRequest> requests) throws AmbariException {
    Set<ConfigGroupResponse> responses = new HashSet<>();
    if (requests != null) {
      for (ConfigGroupRequest request : requests) {
        LOG.debug("Received a Config group request with, clusterName = {}, groupId = {}, groupName = {}, tag = {}",
          request.getClusterName(), request.getId(), request.getGroupName(), request.getTag());

        if (request.getClusterName() == null) {
          LOG.warn("Cluster name is a required field.");
          continue;
        }

        Cluster cluster = getManagementController().getClusters().getCluster
          (request.getClusterName());
        Map<Long, ConfigGroup> configGroupMap = cluster.getConfigGroups();

        // By group id
        if (request.getId() != null) {
          ConfigGroup configGroup = configGroupMap.get(request.getId());
          if (configGroup != null) {
            responses.add(configGroup.convertToResponse());
          } else {
            throw new ConfigGroupNotFoundException(cluster.getClusterName(),
              request.getId().toString());
          }
          continue;
        }
        // By group name
        if (request.getGroupName() != null) {
          for (ConfigGroup configGroup : configGroupMap.values()) {
            if (configGroup.getName().equals(request.getGroupName())) {
              responses.add(configGroup.convertToResponse());
            }
          }
          continue;
        }
        // By tag only
        if (request.getTag() != null && request.getHosts().isEmpty()) {
          for (ConfigGroup configGroup : configGroupMap.values()) {
            if (configGroup.getTag().equals(request.getTag())) {
              responses.add(configGroup.convertToResponse());
            }
          }
          continue;
        }
        // By hostnames only
        if (!request.getHosts().isEmpty() && request.getTag() == null) {
          for (String hostname : request.getHosts()) {
            Map<Long, ConfigGroup> groupMap = cluster
              .getConfigGroupsByHostname(hostname);

            if (!groupMap.isEmpty()) {
              for (ConfigGroup configGroup : groupMap.values()) {
                responses.add(configGroup.convertToResponse());
              }
            }
          }
          continue;
        }
        // By tag and hostnames
        if (request.getTag() != null && !request.getHosts().isEmpty()) {
          for (ConfigGroup configGroup : configGroupMap.values()) {
            // Has tag
            if (configGroup.getTag().equals(request.getTag())) {
              // Has a match with hosts
              List<Long> groupHostIds = new ArrayList<>(configGroup.getHosts().keySet());
              Set<String> groupHostNames = new HashSet<>(hostDAO.getHostNamesByHostIds(groupHostIds));

              groupHostNames.retainAll(request.getHosts());
              if (!groupHostNames.isEmpty()) {
                responses.add(configGroup.convertToResponse());
              }
            }
          }
          continue;
        }
        // Select all
        for (ConfigGroup configGroup : configGroupMap.values()) {
          responses.add(configGroup.convertToResponse());
        }
      }
    }
    return responses;
  }

  private void verifyConfigs(Map<String, Config> configs, String clusterName) throws AmbariException {
    if (configs == null) {
      return;
    }
    Clusters clusters = getManagementController().getClusters();
    for (String key : configs.keySet()) {
      if(!clusters.getCluster(clusterName).isConfigTypeExists(key)){
        throw new AmbariException("Trying to add not existent config type to config group:"+
        " configType = "+ key +
        " cluster = " + clusterName);
      }
    }
  }

  private void verifyHostList(Cluster cluster, Map<Long, Host> hosts,
                              ConfigGroupRequest request) throws AmbariException {

    Map<Long, ConfigGroup> configGroupMap = cluster.getConfigGroups();

    if (configGroupMap != null) {
      for (ConfigGroup configGroup : configGroupMap.values()) {
        if (configGroup.getTag().equals(request.getTag())
            && !configGroup.getId().equals(request.getId())) {
          // Check the new host list for duplicated with this group
          for (Host host : hosts.values()) {
            if (configGroup.getHosts().containsKey(host.getHostId())) {
              throw new DuplicateResourceException("Host is already " +
                "associated with a config group"
                + ", clusterName = " + configGroup.getClusterName()
                + ", configGroupName = " + configGroup.getName()
                + ", tag = " + configGroup.getTag()
                + ", hostname = " + host.getHostName());
            }
          }
        }
      }
    }
  }

  private synchronized void deleteConfigGroup(ConfigGroupRequest request)
      throws AmbariException, AuthorizationException {
    if (request.getId() == null) {
      throw new AmbariException("Config group id is a required field.");
    }

    Clusters clusters = getManagementController().getClusters();

    Cluster cluster;
    try {
      cluster = clusters.getCluster(request.getClusterName());
    } catch (ClusterNotFoundException e) {
      throw new ParentObjectNotFoundException(
        "Attempted to delete a config group from a cluster which doesn't " +
          "exist", e);
    }
    ConfigGroup configGroup = cluster.getConfigGroups().get(request.getId());

    if (configGroup == null) {
      throw new ConfigGroupNotFoundException(cluster.getClusterName(), request.getId().toString());
    }

    if (configGroup.getServiceId() == null) {
      if (!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, cluster.getResourceId(),
        RoleAuthorization.CLUSTER_MANAGE_CONFIG_GROUPS)) {
        throw new AuthorizationException("The authenticated user is not authorized to delete config groups");
      }
    } else {
      if (!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, cluster.getResourceId(),
        RoleAuthorization.SERVICE_MANAGE_CONFIG_GROUPS)) {
        throw new AuthorizationException("The authenticated user is not authorized to delete config groups");
      }
    }

    configLogger.info("(configchange) Deleting configuration group. cluster: '{}', changed by: '{}', config group: '{}', config group id: '{}'",
        cluster.getClusterName(), getManagementController().getAuthName(), configGroup.getName(), request.getId());

    cluster.deleteConfigGroup(request.getId());
  }

  private void validateRequest(ConfigGroupRequest request) {
    if (request.getClusterName() == null
      || request.getClusterName().isEmpty()
      || request.getGroupName() == null
      || request.getGroupName().isEmpty()
      || request.getTag() == null
      || request.getTag().isEmpty()) {

      LOG.debug("Received a config group request with cluster name = {}, group name = {}, tag = {}",
        request.getClusterName(), request.getGroupName(), request.getTag());

      throw new IllegalArgumentException("Cluster name, group name and tag need to be provided.");

    }
  }

  private synchronized Set<ConfigGroupResponse> createConfigGroups
    (Set<ConfigGroupRequest> requests) throws AmbariException, AuthorizationException {

    if (requests.isEmpty()) {
      LOG.warn("Received an empty requests set");
      return null;
    }

    Set<ConfigGroupResponse> configGroupResponses = new
      HashSet<>();

    Clusters clusters = getManagementController().getClusters();
    ConfigGroupFactory configGroupFactory = getManagementController()
      .getConfigGroupFactory();

    for (ConfigGroupRequest request : requests) {

      Cluster cluster;
      try {
        cluster = clusters.getCluster(request.getClusterName());
      } catch (ClusterNotFoundException e) {
        throw new ParentObjectNotFoundException(
          "Attempted to add a config group to a cluster which doesn't exist", e);
      }

      validateRequest(request);

      Map<Long, ConfigGroup> configGroupMap = cluster.getConfigGroups();
      if (configGroupMap != null) {
        for (ConfigGroup configGroup : configGroupMap.values()) {
          if (configGroup.getName().equals(request.getGroupName()) &&
              configGroup.getTag().equals(request.getTag())) {
            throw new DuplicateResourceException("Config group already " +
              "exists with the same name and tag"
              + ", clusterName = " + request.getClusterName()
              + ", groupName = " + request.getGroupName()
              + ", tag = " + request.getTag());
          }
        }
      }

      // Find hosts
      Map<Long, Host> hosts = new HashMap<>();
      if (request.getHosts() != null && !request.getHosts().isEmpty()) {
        for (String hostname : request.getHosts()) {
          Host host = clusters.getHost(hostname);
          HostEntity hostEntity = hostDAO.findByName(hostname);
          if (host == null || hostEntity == null) {
            throw new HostNotFoundException(hostname);
          }
          hosts.put(hostEntity.getHostId(), host);
        }
      }

      verifyHostList(cluster, hosts, request);
      Service service = cluster.getService(request.getServiceGroupName(), request.getServiceName());
      Long serviceId = service.getServiceId();
      Long serviceGroupId = service.getServiceGroupId();
      if (serviceId == null && !MapUtils.isEmpty(request.getConfigs())) {
        try {
          serviceId = cluster.getServiceForConfigTypes(request.getConfigs().keySet());
          serviceGroupId = cluster.getService(serviceId).getServiceGroupId();
        } catch (IllegalArgumentException e) {
          // Ignore this since we may have hit a config type that spans multiple services. This may
          // happen in unit test cases but should not happen with later versions of stacks.
        }
      }

      if (serviceId == null) {
        if (!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, cluster.getResourceId(),
            RoleAuthorization.CLUSTER_MANAGE_CONFIG_GROUPS)) {
          throw new AuthorizationException("The authenticated user is not authorized to create config groups");
        }
      } else {
        if (!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, cluster.getResourceId(),
            RoleAuthorization.SERVICE_MANAGE_CONFIG_GROUPS)) {
          throw new AuthorizationException("The authenticated user is not authorized to create config groups");
        }
      }

      configLogger.info("(configchange) Creating new configuration group. cluster: '{}', changed by: '{}', config group: '{}', tag: '{}'",
          cluster.getClusterName(), getManagementController().getAuthName(), request.getGroupName(), request.getTag());

      verifyConfigs(request.getConfigs(), cluster.getClusterName());

      ConfigGroup configGroup = configGroupFactory.createNew(cluster, serviceGroupId, serviceId,
        request.getGroupName(),
        request.getTag(), request.getDescription(),
        request.getConfigs(), hosts);

      cluster.addConfigGroup(configGroup);
      if (serviceGroupId != null && serviceId != null) {
        cluster.createServiceConfigVersion(serviceId, getManagementController().getAuthName(),
          request.getServiceConfigVersionNote(), configGroup);
      } else {
        LOG.warn("Could not determine service name for config group {}, service config version not created",
            configGroup.getId());
      }

      ConfigGroupResponse response = new ConfigGroupResponse(configGroup
        .getId(), configGroup.getClusterName(), configGroup.getName(),
        configGroup.getTag(), configGroup.getDescription(), null, null);

      configGroupResponses.add(response);
    }

    return configGroupResponses;
  }

  private synchronized void updateConfigGroups (Set<ConfigGroupRequest> requests) throws AmbariException, AuthorizationException {
    if (requests.isEmpty()) {
      LOG.warn("Received an empty requests set");
      return;
    }

    Clusters clusters = getManagementController().getClusters();

    for (ConfigGroupRequest request : requests) {

      Cluster cluster;
      try {
        cluster = clusters.getCluster(request.getClusterName());
      } catch (ClusterNotFoundException e) {
        throw new ParentObjectNotFoundException(
          "Attempted to add a config group to a cluster which doesn't exist", e);
      }

      if (request.getId() == null) {
        throw new AmbariException("Config group Id is a required parameter.");
      }

      validateRequest(request);

      // Find config group
      ConfigGroup configGroup = cluster.getConfigGroups().get(request.getId());
      if (configGroup == null) {
        throw new AmbariException("Config group not found"
                                 + ", clusterName = " + request.getClusterName()
                                 + ", groupId = " + request.getId());
      }

      Long serviceId = configGroup.getServiceId();
      Long requestServiceId = cluster.getServiceForConfigTypes(request.getConfigs().keySet());

      if (serviceId == null && requestServiceId == null) {
        if (!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, cluster.getResourceId(),
            RoleAuthorization.CLUSTER_MANAGE_CONFIG_GROUPS)) {
          throw new AuthorizationException("The authenticated user is not authorized to update config groups");
        }
      } else {
        if (!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, cluster.getResourceId(),
            RoleAuthorization.SERVICE_MANAGE_CONFIG_GROUPS)) {
          throw new AuthorizationException("The authenticated user is not authorized to update config groups");
        }
      }

      if (serviceId != null && requestServiceId != null && !serviceId.equals(requestServiceId)) {
        throw new IllegalArgumentException("Config group " + configGroup.getId() +
            " is mapped to service " + serviceId + ", " +
            "but request contain configs from service " + requestServiceId);
      } else if (serviceId == null && requestServiceId != null) {
        configGroup.setServiceId(requestServiceId);
        serviceId = requestServiceId;
      }

      int numHosts = (null != configGroup.getHosts()) ? configGroup.getHosts().size() : 0;
      configLogger.info("(configchange) Updating configuration group host membership or config value. cluster: '{}', changed by: '{}', " +
              "service_name: '{}', config group: '{}', tag: '{}', num hosts in config group: '{}', note: '{}'",
          cluster.getClusterName(), getManagementController().getAuthName(),
          serviceId, request.getGroupName(), request.getTag(), numHosts, request.getServiceConfigVersionNote());

      if (!request.getConfigs().isEmpty()) {
        List<String> affectedConfigTypeList = new ArrayList(request.getConfigs().keySet());
        Collections.sort(affectedConfigTypeList);
        String affectedConfigTypesString = "(" + StringUtils.join(affectedConfigTypeList, ", ") + ")";
        configLogger.info("(configchange)    Affected configs: {}", affectedConfigTypesString);

        for (Config config : request.getConfigs().values()) {
          List<String> sortedConfigKeys = new ArrayList(config.getProperties().keySet());
          Collections.sort(sortedConfigKeys);
          String sortedConfigKeysString = StringUtils.join(sortedConfigKeys, ", ");
          configLogger.info("(configchange)    Config type '{}' was  modified with the following keys, {}", config.getType(), sortedConfigKeysString);
        }
      }

      // Update hosts
      Map<Long, Host> hosts = new HashMap<>();
      if (request.getHosts() != null && !request.getHosts().isEmpty()) {
        for (String hostname : request.getHosts()) {
          Host host = clusters.getHost(hostname);
          HostEntity hostEntity = hostDAO.findById(host.getHostId());
          if (hostEntity == null) {
            throw new HostNotFoundException(hostname);
          }
          hosts.put(hostEntity.getHostId(), host);
        }
      }

      verifyHostList(cluster, hosts, request);

      configGroup.setHosts(hosts);

      // Update Configs
      verifyConfigs(request.getConfigs(), request.getClusterName());
      configGroup.setConfigurations(request.getConfigs());

      // Save
      configGroup.setName(request.getGroupName());
      configGroup.setDescription(request.getDescription());
      configGroup.setTag(request.getTag());

      if (serviceId != null) {
        cluster.createServiceConfigVersion(serviceId, getManagementController().getAuthName(),
                request.getServiceConfigVersionNote(), configGroup);

        ConfigGroupResponse configGroupResponse = new ConfigGroupResponse(configGroup.getId(), cluster.getClusterName(), configGroup.getName(),
                request.getTag(), "", new HashSet<>(), new HashSet<>());
        Set<Map<String, Object>> versionTags = new HashSet<>();
        Map<String, Object> tagsMap = new HashMap<>();
        for (Config config : configGroup.getConfigurations().values()) {
          tagsMap.put(config.getType(), config.getTag());
        }
        versionTags.add(tagsMap);
        configGroupResponse.setVersionTags(versionTags);
        getManagementController().saveConfigGroupUpdate(request, configGroupResponse);
      } else {
        LOG.warn("Could not determine service name for config group {}, service config version not created",
            configGroup.getId());
      }
    }

  }

  @SuppressWarnings("unchecked")
  ConfigGroupRequest getConfigGroupRequest(Map<String, Object> properties) {
    Object groupIdObj = properties.get(CONFIGGROUP_ID_PROPERTY_ID);
    Long groupId = null;
    if (groupIdObj != null)  {
      groupId = groupIdObj instanceof Long ? (Long) groupIdObj :
        Long.parseLong((String) groupIdObj);
    }

    ConfigGroupRequest request = new ConfigGroupRequest(
      groupId,
      (String) properties.get(CONFIGGROUP_CLUSTER_NAME_PROPERTY_ID),
      (String) properties.get(CONFIGGROUP_NAME_PROPERTY_ID),
      (String) properties.get(CONFIGGROUP_TAG_PROPERTY_ID),
      (String) properties.get(CONFIGGROUP_SERVICEGROUPNAME_PROPERTY_ID),
      (String) properties.get(CONFIGGROUP_SERVICENAME_PROPERTY_ID),
      (String) properties.get(CONFIGGROUP_DESC_PROPERTY_ID),
      null,
      null);

    request.setServiceConfigVersionNote((String) properties.get(CONFIGGROUP_SCV_NOTE_ID));

    Map<String, Config> configurations = new HashMap<>();
    Set<String> hosts = new HashSet<>();

    String hostnameKey = CONFIGGROUP_HOSTNAME_PROPERTY_ID;
    Object hostObj = properties.get(CONFIGGROUP_HOSTS_PROPERTY_ID);
    if (hostObj == null) {
      hostnameKey = CONFIGGROUP_HOSTS_HOSTNAME_PROPERTY_ID;
      hostObj = properties.get(CONFIGGROUP_HOSTS_HOSTNAME_PROPERTY_ID);
    }
    if (hostObj != null) {
      if (hostObj instanceof HashSet<?>) {
        try {
          Set<Map<String, String>> hostsSet = (Set<Map<String, String>>) hostObj;
          for (Map<String, String> hostMap : hostsSet) {
            if (hostMap.containsKey(hostnameKey)) {
              String hostname = hostMap.get(hostnameKey);
              hosts.add(hostname);
            }
          }
        } catch (Exception e) {
          LOG.warn("Host json in unparseable format. " + hostObj, e);
        }
      } else {
        if (hostObj instanceof String) {
          hosts.add((String) hostObj);
        }
      }
    }

    Object configObj = properties.get(CONFIGGROUP_CONFIGS_PROPERTY_ID);
    if (configObj != null && configObj instanceof HashSet<?>) {
      try {
        Set<Map<String, Object>> configSet = (Set<Map<String, Object>>) configObj;
        for (Map<String, Object> configMap : configSet) {
          String type = (String) configMap.get(ConfigurationResourceProvider
            .CONFIGURATION_CONFIG_TYPE_PROPERTY_ID);
          String tag = (String) configMap.get(ConfigurationResourceProvider
            .CONFIGURATION_CONFIG_TAG_PROPERTY_ID);

          Map<String, String> configProperties = new HashMap<>();
          Map<String, Map<String, String>> configAttributes = new HashMap<>();

          for (Map.Entry<String, Object> entry : configMap.entrySet()) {
            String propertyCategory = PropertyHelper.getPropertyCategory(entry.getKey());
            if (propertyCategory != null && entry.getValue() != null) {
              if ("properties".equals(propertyCategory)) {
                configProperties.put(PropertyHelper.getPropertyName(entry.getKey()),
                    entry.getValue().toString());
              } else if ("properties_attributes".equals(PropertyHelper.getPropertyCategory(propertyCategory))) {
                String attributeName = PropertyHelper.getPropertyName(propertyCategory);
                if (!configAttributes.containsKey(attributeName)) {
                  configAttributes.put(attributeName, new HashMap<>());
                }
                Map<String, String> attributeValues
                    = configAttributes.get(attributeName);
                attributeValues.put(PropertyHelper.getPropertyName(entry.getKey()),
                    entry.getValue().toString());
              }
            }
          }

          Config config = configFactory.createReadOnly(type, tag, configProperties, configAttributes);
          configurations.put(config.getType(), config);
        }
      } catch (Exception e) {
        LOG.warn("Config json in unparseable format. " + configObj, e);
      }
    }

    request.setConfigs(configurations);
    request.setHosts(hosts);

    return request;
  }

  /**
   * Bypassing predicate evaluation for the lack of a matcher for a
   * non-scalar resource
   *
   * @param predicate  the predicate
   * @param resource   the resource
   *
   * @return always returns true
   */
  @Override
  public boolean evaluate(Predicate predicate, Resource resource) {
    return true;
  }
}
