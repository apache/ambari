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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ConfigurationRequest;
import org.apache.ambari.server.controller.ConfigurationResponse;
import org.apache.ambari.server.controller.ServiceConfigVersionRequest;
import org.apache.ambari.server.controller.ServiceConfigVersionResponse;
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
import org.apache.ambari.server.security.authorization.RoleAuthorization;

public class ServiceConfigVersionResourceProvider extends
    AbstractControllerResourceProvider {

  public static final String SERVICE_CONFIG_VERSION_CLUSTER_NAME_PROPERTY_ID = PropertyHelper.getPropertyId(null, "cluster_name");
  public static final String SERVICE_CONFIG_VERSION_PROPERTY_ID = PropertyHelper.getPropertyId(null, "service_config_version");
  public static final String SERVICE_CONFIG_VERSION_SERVICE_GROUP_NAME_PROPERTY_ID = PropertyHelper.getPropertyId(null, "service_group_name");
  public static final String SERVICE_CONFIG_VERSION_SERVICE_GROUP_ID_PROPERTY_ID = PropertyHelper.getPropertyId(null, "service_group_id");
  public static final String SERVICE_CONFIG_VERSION_SERVICE_NAME_PROPERTY_ID = PropertyHelper.getPropertyId(null, "service_name");
  public static final String SERVICE_CONFIG_VERSION_SERVICE_ID_PROPERTY_ID = PropertyHelper.getPropertyId(null, "service_id");
  public static final String SERVICE_CONFIG_VERSION_CREATE_TIME_PROPERTY_ID = PropertyHelper.getPropertyId(null, "createtime");
  public static final String SERVICE_CONFIG_VERSION_USER_PROPERTY_ID = PropertyHelper.getPropertyId(null, "user");
  public static final String SERVICE_CONFIG_VERSION_NOTE_PROPERTY_ID = PropertyHelper.getPropertyId("ServiceConfigVersion", "service_config_version_note");
  public static final String SERVICE_CONFIG_VERSION_GROUP_ID_PROPERTY_ID = PropertyHelper.getPropertyId(null, "group_id");
  public static final String SERVICE_CONFIG_VERSION_GROUP_NAME_PROPERTY_ID = PropertyHelper.getPropertyId(null, "group_name");
  public static final String SERVICE_CONFIG_VERSION_STACK_ID_PROPERTY_ID = PropertyHelper.getPropertyId("ServiceConfigVersion", "stack_id");
  public static final String SERVICE_CONFIG_VERSION_IS_CURRENT_PROPERTY_ID = PropertyHelper.getPropertyId(null, "is_current");
  public static final String SERVICE_CONFIG_VERSION_IS_COMPATIBLE_PROPERTY_ID = PropertyHelper.getPropertyId(null, "is_cluster_compatible");
  public static final String SERVICE_CONFIG_VERSION_HOSTS_PROPERTY_ID = PropertyHelper.getPropertyId(null, "hosts");
  public static final String SERVICE_CONFIG_VERSION_CONFIGURATIONS_PROPERTY_ID = PropertyHelper.getPropertyId(null, "configurations");

  /**
   * The property ids for a service configuration resource.
   */
  private static final Set<String> PROPERTY_IDS = new HashSet<>();

  /**
   * The key property ids for a service configuration resource.
   */
  private static final Map<Resource.Type, String> KEY_PROPERTY_IDS = new HashMap<>();

  static {
    // properties
    PROPERTY_IDS.add(SERVICE_CONFIG_VERSION_CLUSTER_NAME_PROPERTY_ID);
    PROPERTY_IDS.add(SERVICE_CONFIG_VERSION_PROPERTY_ID);
    PROPERTY_IDS.add(SERVICE_CONFIG_VERSION_SERVICE_NAME_PROPERTY_ID);
    PROPERTY_IDS.add(SERVICE_CONFIG_VERSION_CREATE_TIME_PROPERTY_ID);
    PROPERTY_IDS.add(SERVICE_CONFIG_VERSION_USER_PROPERTY_ID);
    PROPERTY_IDS.add(SERVICE_CONFIG_VERSION_NOTE_PROPERTY_ID);
    PROPERTY_IDS.add(SERVICE_CONFIG_VERSION_GROUP_ID_PROPERTY_ID);
    PROPERTY_IDS.add(SERVICE_CONFIG_VERSION_GROUP_NAME_PROPERTY_ID);
    PROPERTY_IDS.add(SERVICE_CONFIG_VERSION_STACK_ID_PROPERTY_ID);
    PROPERTY_IDS.add(SERVICE_CONFIG_VERSION_IS_CURRENT_PROPERTY_ID);
    PROPERTY_IDS.add(SERVICE_CONFIG_VERSION_HOSTS_PROPERTY_ID);
    PROPERTY_IDS.add(SERVICE_CONFIG_VERSION_CONFIGURATIONS_PROPERTY_ID);
    PROPERTY_IDS.add(SERVICE_CONFIG_VERSION_IS_COMPATIBLE_PROPERTY_ID);
    PROPERTY_IDS.add(SERVICE_CONFIG_VERSION_SERVICE_GROUP_NAME_PROPERTY_ID);

    // keys
    KEY_PROPERTY_IDS.put(Resource.Type.Service, SERVICE_CONFIG_VERSION_SERVICE_NAME_PROPERTY_ID);
    KEY_PROPERTY_IDS.put(Resource.Type.Cluster, SERVICE_CONFIG_VERSION_CLUSTER_NAME_PROPERTY_ID);
    KEY_PROPERTY_IDS.put(Resource.Type.ServiceConfigVersion, SERVICE_CONFIG_VERSION_PROPERTY_ID);
    KEY_PROPERTY_IDS.put(Resource.Type.ServiceGroup, SERVICE_CONFIG_VERSION_SERVICE_GROUP_NAME_PROPERTY_ID);
  }


  /**
   * The primary key property ids for the service config version resource type.
   */
  private static Set<String> pkPropertyIds =
    new HashSet<>(Arrays.asList(new String[]{
      SERVICE_CONFIG_VERSION_CLUSTER_NAME_PROPERTY_ID,
      SERVICE_CONFIG_VERSION_SERVICE_NAME_PROPERTY_ID, SERVICE_CONFIG_VERSION_SERVICE_GROUP_NAME_PROPERTY_ID}));


  // ----- Constructors ------------------------------------------------------

  /**
   * Constructor
   *
   * @param managementController  the associated management controller
   */
  ServiceConfigVersionResourceProvider(
      AmbariManagementController managementController) {
    super(Resource.Type.ServiceConfigVersion, PROPERTY_IDS, KEY_PROPERTY_IDS, managementController);

    setRequiredGetAuthorizations(EnumSet.of(RoleAuthorization.CLUSTER_VIEW_CONFIGS,
        RoleAuthorization.SERVICE_VIEW_CONFIGS,
        RoleAuthorization.SERVICE_COMPARE_CONFIGS));
  }


  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

  @Override
  public RequestStatus createResources(Request request) throws SystemException, UnsupportedPropertyException, ResourceAlreadyExistsException, NoSuchParentResourceException {
    final Set<ServiceConfigVersionRequest> requests = new HashSet<>();
    Set<ServiceConfigVersionResponse> responses = new HashSet<>();

    for (Map<String, Object> propertyMap : request.getProperties()) {
      requests.add(createRequest(propertyMap));
    }
    responses = createResources(new Command<Set<ServiceConfigVersionResponse>>() {
      @Override
      public Set<ServiceConfigVersionResponse> invoke() throws AmbariException, AuthorizationException {
        return getManagementController().createServiceConfigVersion(requests);
      }
    });

    Set<Resource> associatedResources = new HashSet<>();
    for (ServiceConfigVersionResponse serviceConfigVersionResponse : responses) {
      Resource resource = new ResourceImpl(Resource.Type.ServiceConfigVersion);
      resource.setProperty(ServiceConfigVersionResourceProvider.SERVICE_CONFIG_VERSION_SERVICE_GROUP_NAME_PROPERTY_ID,
              serviceConfigVersionResponse.getServiceGroupName());
      resource.setProperty(ServiceConfigVersionResourceProvider.SERVICE_CONFIG_VERSION_SERVICE_GROUP_ID_PROPERTY_ID,
              serviceConfigVersionResponse.getServiceGroupId());
      resource.setProperty(ServiceConfigVersionResourceProvider.SERVICE_CONFIG_VERSION_SERVICE_NAME_PROPERTY_ID,
              serviceConfigVersionResponse.getServiceName());
      resource.setProperty(ServiceConfigVersionResourceProvider.SERVICE_CONFIG_VERSION_SERVICE_ID_PROPERTY_ID,
              serviceConfigVersionResponse.getServiceId());
      resource.setProperty(ServiceConfigVersionResourceProvider.SERVICE_CONFIG_VERSION_PROPERTY_ID,
              serviceConfigVersionResponse.getVersion());
      resource.setProperty(ServiceConfigVersionResourceProvider.SERVICE_CONFIG_VERSION_NOTE_PROPERTY_ID,
              serviceConfigVersionResponse.getNote());
      resource.setProperty(ServiceConfigVersionResourceProvider.SERVICE_CONFIG_VERSION_GROUP_ID_PROPERTY_ID,
              serviceConfigVersionResponse.getGroupId());
      resource.setProperty(ServiceConfigVersionResourceProvider.SERVICE_CONFIG_VERSION_GROUP_NAME_PROPERTY_ID,
              serviceConfigVersionResponse.getGroupName());
      if (serviceConfigVersionResponse.getConfigurations() != null) {
        resource.setProperty(
                ServiceConfigVersionResourceProvider.SERVICE_CONFIG_VERSION_CONFIGURATIONS_PROPERTY_ID,
                serviceConfigVersionResponse.getConfigurations());
      }
      associatedResources.add(resource);
    }

    return getRequestStatus(null, associatedResources);
  }

  private List<ConfigurationRequest> getConfigurationRequests(Set<Map<String, Object>>  configurations) {
    List<ConfigurationRequest> configs = new LinkedList<>();
    for (Map<String, Object> value: configurations) {
      ConfigurationRequest newConfig = new ConfigurationRequest();
      for (Map.Entry<String, Object> e : value.entrySet()) {
        String propName =
                PropertyHelper.getPropertyName("Config" + '/' + e.getKey());
        String absCatategory =
                PropertyHelper.getPropertyCategory("Config" + '/' + e.getKey());
        parseProperties(newConfig, absCatategory, propName, e.getValue() == null ? null : e.getValue().toString());
      }
      configs.add(newConfig);
    }
    return configs;
  }

  @Override
  public Set<Resource> getResourcesAuthorized(Request request, Predicate predicate) throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    final Set<ServiceConfigVersionRequest> requests = new HashSet<>();
    for (Map<String, Object> properties : getPropertyMaps(predicate)) {
      requests.add(createRequest(properties));
    }

    Set<ServiceConfigVersionResponse> responses = getResources(new Command<Set<ServiceConfigVersionResponse>>() {
      @Override
      public Set<ServiceConfigVersionResponse> invoke() throws AmbariException {
        return getManagementController().getServiceConfigVersions(requests);
      }
    });

    Set<Resource> resources = new HashSet<>();
    for (ServiceConfigVersionResponse response : responses) {
      String clusterName = response.getClusterName();
      List<ConfigurationResponse> configurationResponses = response.getConfigurations();
      List<Map<String,Object>> configVersionConfigurations = convertToSubResources(clusterName, configurationResponses);

      Resource resource = new ResourceImpl(Resource.Type.ServiceConfigVersion);
      resource.setProperty(SERVICE_CONFIG_VERSION_CLUSTER_NAME_PROPERTY_ID, clusterName);
      resource.setProperty(SERVICE_CONFIG_VERSION_SERVICE_NAME_PROPERTY_ID, response.getServiceName());
      resource.setProperty(SERVICE_CONFIG_VERSION_SERVICE_GROUP_NAME_PROPERTY_ID, response.getServiceGroupName());
      resource.setProperty(SERVICE_CONFIG_VERSION_USER_PROPERTY_ID, response.getUserName());
      resource.setProperty(SERVICE_CONFIG_VERSION_PROPERTY_ID, response.getVersion());
      resource.setProperty(SERVICE_CONFIG_VERSION_CREATE_TIME_PROPERTY_ID, response.getCreateTime());
      resource.setProperty(SERVICE_CONFIG_VERSION_CONFIGURATIONS_PROPERTY_ID, configVersionConfigurations);
      resource.setProperty(SERVICE_CONFIG_VERSION_NOTE_PROPERTY_ID, response.getNote());
      resource.setProperty(SERVICE_CONFIG_VERSION_GROUP_ID_PROPERTY_ID, response.getGroupId());
      resource.setProperty(SERVICE_CONFIG_VERSION_GROUP_NAME_PROPERTY_ID, response.getGroupName());
      resource.setProperty(SERVICE_CONFIG_VERSION_HOSTS_PROPERTY_ID, response.getHosts());
      resource.setProperty(SERVICE_CONFIG_VERSION_STACK_ID_PROPERTY_ID, response.getStackId());
      resource.setProperty(SERVICE_CONFIG_VERSION_IS_CURRENT_PROPERTY_ID, response.getIsCurrent());
      resource.setProperty(SERVICE_CONFIG_VERSION_IS_COMPATIBLE_PROPERTY_ID, response.isCompatibleWithCurrentStack());

      resources.add(resource);
    }
    return resources;
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate) throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("Cannot update service config version");
  }

  @Override
  public RequestStatus deleteResources(Request request, Predicate predicate) throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("Cannot delete service config version");
  }

  private ServiceConfigVersionRequest createRequest(Map<String, Object> properties) {
    String clusterName = (String) properties.get(SERVICE_CONFIG_VERSION_CLUSTER_NAME_PROPERTY_ID);
    String serviceGroupName = (String) properties.get(SERVICE_CONFIG_VERSION_SERVICE_GROUP_NAME_PROPERTY_ID);
    String serviceName = (String) properties.get(SERVICE_CONFIG_VERSION_SERVICE_NAME_PROPERTY_ID);
    String user = (String) properties.get(SERVICE_CONFIG_VERSION_USER_PROPERTY_ID);
    Boolean isCurrent = Boolean.valueOf((String) properties.get(SERVICE_CONFIG_VERSION_IS_CURRENT_PROPERTY_ID));
    Object versionObject = properties.get(SERVICE_CONFIG_VERSION_PROPERTY_ID);
    Long version = versionObject == null ? null : Long.valueOf(versionObject.toString());
    String note = (String) properties.get(SERVICE_CONFIG_VERSION_NOTE_PROPERTY_ID);
    String stackId = (String) properties.get(SERVICE_CONFIG_VERSION_STACK_ID_PROPERTY_ID);
    ServiceConfigVersionRequest scvr = new ServiceConfigVersionRequest(clusterName, serviceGroupName, serviceName, version, null, null, user, isCurrent, note, stackId);
    if(version == null && properties.containsKey("configurations")) {
      List<ConfigurationRequest> configRequests = getConfigurationRequests((Set<Map<String, Object>>) properties.get("configurations"));
      if (!configRequests.isEmpty()) {
        scvr.setConfigs(configRequests);
      }
    }
    return scvr;
  }

  private List<Map<String, Object>> convertToSubResources(final String clusterName, List<ConfigurationResponse> configs) {
    List<Map<String, Object>> result = new ArrayList<>();
    for (final ConfigurationResponse config : configs) {
      Map<String, Object> subResourceMap = new LinkedHashMap<>();
      Map<String,String> configMap = new HashMap<>();

      String stackId = config.getStackId().getStackId();

      configMap.put("cluster_name", clusterName);
      configMap.put("stack_id", stackId);

      subResourceMap.put("Config", configMap);
      subResourceMap.put("type", config.getType());
      subResourceMap.put("tag", config.getVersionTag());
      subResourceMap.put("version", config.getVersion());
      subResourceMap.put("properties", new TreeMap<>(config.getConfigs()));
      subResourceMap.put("properties_attributes", config.getConfigAttributes());
      result.add(subResourceMap);
    }

    return result;
  }
}
