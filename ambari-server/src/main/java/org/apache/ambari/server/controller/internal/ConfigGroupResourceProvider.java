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
import org.apache.ambari.server.ClusterNotFoundException;
import org.apache.ambari.server.DuplicateResourceException;
import org.apache.ambari.server.HostNotFoundException;
import org.apache.ambari.server.ParentObjectNotFoundException;
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
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigImpl;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.configgroup.ConfigGroup;
import org.apache.ambari.server.state.configgroup.ConfigGroupFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

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
  protected static final String CONFIGGROUP_DESC_PROPERTY_ID = PropertyHelper
    .getPropertyId("ConfigGroup", "description");
  protected static final String CONFIGGROUP_HOSTNAME_PROPERTY_ID =
    PropertyHelper.getPropertyId(null, "host_name");
  public static final String CONFIGGROUP_HOSTS_PROPERTY_ID = PropertyHelper
    .getPropertyId("ConfigGroup", "hosts");
  public static final String CONFIGGROUP_CONFIGS_PROPERTY_ID =
    PropertyHelper.getPropertyId("ConfigGroup", "desired_configs");

  private static Set<String> pkPropertyIds = new HashSet<String>(Arrays
    .asList(new String[] { CONFIGGROUP_ID_PROPERTY_ID }));

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
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

  @Override
  public RequestStatus createResources(Request request) throws
       SystemException, UnsupportedPropertyException,
       ResourceAlreadyExistsException, NoSuchParentResourceException {

    final Set<ConfigGroupRequest> requests = new HashSet<ConfigGroupRequest>();
    for (Map<String, Object> propertyMap : request.getProperties()) {
      requests.add(getConfigGroupRequest(propertyMap));
    }
    Set<ConfigGroupResponse> responses =
      createResources(new Command<Set<ConfigGroupResponse>>() {
        @Override
        public Set<ConfigGroupResponse> invoke() throws AmbariException {
          return createConfigGroups(requests);
        }
      });

    notifyCreate(Resource.Type.ConfigGroup, request);

    Set<Resource> associatedResources = new HashSet<Resource>();
    for (ConfigGroupResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.ConfigGroup);
      resource.setProperty(CONFIGGROUP_ID_PROPERTY_ID, response.getId());
      associatedResources.add(resource);
    }

    return getRequestStatus(null, associatedResources);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate) throws
       SystemException, UnsupportedPropertyException, NoSuchResourceException,
       NoSuchParentResourceException {

    final Set<ConfigGroupRequest> requests = new HashSet<ConfigGroupRequest>();
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
    Set<Resource> resources    = new HashSet<Resource>();

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
  public RequestStatus updateResources(Request request, Predicate predicate) throws
       SystemException, UnsupportedPropertyException,
       NoSuchResourceException, NoSuchParentResourceException {

    final Set<ConfigGroupRequest> requests = new HashSet<ConfigGroupRequest>();

    Iterator<Map<String,Object>> iterator = request.getProperties().iterator();
    if (iterator.hasNext()) {
      for (Map<String, Object> propertyMap : getPropertyMaps(iterator.next(), predicate)) {
        requests.add(getConfigGroupRequest(propertyMap));
      }

      modifyResources(new Command<Void>() {
        @Override
        public Void invoke() throws AmbariException {
          updateConfigGroups(requests);
          return null;
        }
      });
    }

    notifyUpdate(Resource.Type.ConfigGroup, request, predicate);

    return getRequestStatus(null);
  }

  @Override
  public RequestStatus deleteResources(Predicate predicate) throws
       SystemException, UnsupportedPropertyException, NoSuchResourceException,
       NoSuchParentResourceException {

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      final ConfigGroupRequest configGroupRequest = getConfigGroupRequest(propertyMap);

      modifyResources(new Command<Void>() {
        @Override
        public Void invoke() throws AmbariException {
          deleteConfigGroup(configGroupRequest);
          return null;
        }
      });
    }

    notifyDelete(Resource.Type.ConfigGroup, predicate);

    return getRequestStatus(null);
  }

  private synchronized  Set<ConfigGroupResponse> getConfigGroups
    (Set<ConfigGroupRequest> requests) throws AmbariException {
    Set<ConfigGroupResponse> responses = new HashSet<ConfigGroupResponse>();
    if (requests != null) {
      for (ConfigGroupRequest request : requests) {
        LOG.debug("Received a Config group request with"
          + ", clusterName = " + request.getClusterName()
          + ", groupId = " + request.getId()
          + ", groupName = " + request.getGroupName()
          + ", tag = " + request.getTag());

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
              Set<String> groupHosts = new HashSet<String>(configGroup
                .getHosts().keySet());
              groupHosts.retainAll(request.getHosts());
              if (!groupHosts.isEmpty()) {
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

  private void verifyHostList(Cluster cluster, Map<String, Host> hosts,
                              ConfigGroupRequest request) throws AmbariException {

    Map<Long, ConfigGroup> configGroupMap = cluster.getConfigGroups();

    if (configGroupMap != null) {
      for (ConfigGroup configGroup : configGroupMap.values()) {
        if (configGroup.getTag().equals(request.getTag())
            && !configGroup.getId().equals(request.getId())) {
          // Check the new host list for duplicated with this group
          for (Host host : hosts.values()) {
            if (configGroup.getHosts().containsKey(host.getHostName())) {
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
    throws AmbariException {
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

    configLogger.info("Deleting Config group, "
      + ", clusterName = " + cluster.getClusterName()
      + ", id = " + request.getId()
      + ", user = " + getManagementController().getAuthName());

    cluster.deleteConfigGroup(request.getId());
  }

  private void validateRequest(ConfigGroupRequest request) {
    if (request.getClusterName() == null
      || request.getClusterName().isEmpty()
      || request.getGroupName() == null
      || request.getGroupName().isEmpty()
      || request.getTag() == null
      || request.getTag().isEmpty()) {

      LOG.debug("Received a config group request with cluster name = " +
        request.getClusterName() + ", group name = " + request.getGroupName()
        + ", tag = " + request.getTag());

      throw new IllegalArgumentException("Cluster name, " +
        "group name and tag need to be provided.");

    }
  }

  private synchronized Set<ConfigGroupResponse> createConfigGroups
    (Set<ConfigGroupRequest> requests) throws AmbariException {

    if (requests.isEmpty()) {
      LOG.warn("Received an empty requests set");
      return null;
    }

    Set<ConfigGroupResponse> configGroupResponses = new
      HashSet<ConfigGroupResponse>();

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
      Map<String, Host> hosts = new HashMap<String, Host>();
      if (request.getHosts() != null && !request.getHosts().isEmpty()) {
        for (String hostname : request.getHosts()) {
          Host host = clusters.getHost(hostname);
          if (host == null) {
            throw new HostNotFoundException(hostname);
          }
          hosts.put(hostname, host);
        }
      }

      verifyHostList(cluster, hosts, request);

      ConfigGroup configGroup = configGroupFactory.createNew(cluster,
        request.getGroupName(),
        request.getTag(), request.getDescription(),
        request.getConfigs(), hosts);

      // Persist before add, since id is auto-generated
      configLogger.info("Persisting new Config group"
        + ", clusterName = " + cluster.getClusterName()
        + ", name = " + configGroup.getName()
        + ", tag = " + configGroup.getTag()
        + ", user = " + getManagementController().getAuthName());

      configGroup.persist();
      cluster.addConfigGroup(configGroup);

      ConfigGroupResponse response = new ConfigGroupResponse(configGroup
        .getId(), configGroup.getClusterName(), configGroup.getName(),
        configGroup.getTag(), configGroup.getDescription(), null, null);

      configGroupResponses.add(response);
    }

    return configGroupResponses;
  }

  private synchronized void updateConfigGroups
    (Set<ConfigGroupRequest> requests) throws AmbariException {

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

      // Update hosts
      Map<String, Host> hosts = new HashMap<String, Host>();
      if (request.getHosts() != null && !request.getHosts().isEmpty()) {
        for (String hostname : request.getHosts()) {
          Host host = clusters.getHost(hostname);
          if (host == null) {
            throw new HostNotFoundException(hostname);
          }
          hosts.put(hostname, host);
        }
      }

      verifyHostList(cluster, hosts, request);

      configGroup.setHosts(hosts);

      // Update Configs
      configGroup.setConfigurations(request.getConfigs());

      // Save
      configGroup.setName(request.getGroupName());
      configGroup.setDescription(request.getDescription());
      configGroup.setTag(request.getTag());

      configLogger.info("Persisting updated Config group, "
        + ", clusterName = " + configGroup.getClusterName()
        + ", id = " + configGroup.getId()
        + ", tag = " + configGroup.getTag()
        + ", user = " + getManagementController().getAuthName());

      configGroup.persist();
    }
  }

  @SuppressWarnings("unchecked")
  private ConfigGroupRequest getConfigGroupRequest(Map<String, Object> properties) {
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
      (String) properties.get(CONFIGGROUP_DESC_PROPERTY_ID),
      null,
      null);

    Map<String, Config> configurations = new HashMap<String, Config>();
    Set<String> hosts = new HashSet<String>();

    Object hostObj = properties.get(CONFIGGROUP_HOSTS_PROPERTY_ID);
    if (hostObj != null) {
      if (hostObj instanceof HashSet<?>) {
        try {
          Set<Map<String, String>> hostsSet = (Set<Map<String, String>>) hostObj;
          for (Map<String, String> hostMap : hostsSet) {
            if (hostMap.containsKey(CONFIGGROUP_HOSTNAME_PROPERTY_ID)) {
              String hostname = hostMap.get(CONFIGGROUP_HOSTNAME_PROPERTY_ID);
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

          Map<String, String> configProperties = new HashMap<String, String>();

          for (Map.Entry<String, Object> entry : configMap.entrySet()) {
            String propertyCategory = PropertyHelper.getPropertyCategory(entry.getKey());
            if (propertyCategory != null
                && propertyCategory.equals("properties")
                  && null != entry.getValue()) {
              configProperties.put(PropertyHelper.getPropertyName(entry.getKey()),
                entry.getValue().toString());
            }
          }

          Config config = new ConfigImpl(type);
          config.setVersionTag(tag);
          config.setProperties(configProperties);

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
   * @return
   */
  @Override
  public boolean evaluate(Predicate predicate, Resource resource) {
    return true;
  }
}
