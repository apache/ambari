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


import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ConfigurationResponse;
import org.apache.ambari.server.controller.ServiceConfigVersionRequest;
import org.apache.ambari.server.controller.ServiceConfigVersionResponse;
import org.apache.ambari.server.controller.spi.*;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

import java.util.*;

public class ServiceConfigVersionResourceProvider extends
    AbstractControllerResourceProvider {
  public static final String SERVICE_CONFIG_VERSION_CLUSTER_NAME_PROPERTY_ID = PropertyHelper.getPropertyId(null, "cluster_name");
  public static final String SERVICE_CONFIG_VERSION_PROPERTY_ID = PropertyHelper.getPropertyId(null, "service_config_version");
  public static final String SERVICE_CONFIG_VERSION_SERVICE_NAME_PROPERTY_ID = PropertyHelper.getPropertyId(null, "service_name");
  public static final String SERVICE_CONFIG_VERSION_CREATE_TIME_PROPERTY_ID = PropertyHelper.getPropertyId(null, "createtime");
  public static final String SERVICE_CONFIG_VERSION_USER_PROPERTY_ID = PropertyHelper.getPropertyId(null, "user");
  public static final String SERVICE_CONFIG_VERSION_NOTE_PROPERTY_ID = PropertyHelper.getPropertyId(null, "service_config_version_note");
  public static final String SERVICE_CONFIG_VERSION_GROUP_ID_PROPERTY_ID = PropertyHelper.getPropertyId(null, "group_id");
  public static final String SERVICE_CONFIG_VERSION_GROUP_NAME_PROPERTY_ID = PropertyHelper.getPropertyId(null, "group_name");
  public static final String SERVICE_CONFIG_VERSION_IS_CURRENT_PROPERTY_ID = PropertyHelper.getPropertyId(null, "is_current");
  public static final String SERVICE_CONFIG_VERSION_HOSTS_PROPERTY_ID = PropertyHelper.getPropertyId(null, "hosts");
  public static final String SERVICE_CONFIG_VERSION_CONFIGURATIONS_PROPERTY_ID = PropertyHelper.getPropertyId(null, "configurations");

  /**
   * The primary key property ids for the service config version resource type.
   */
  private static Set<String> pkPropertyIds =
      new HashSet<String>(Arrays.asList(new String[]{
          SERVICE_CONFIG_VERSION_CLUSTER_NAME_PROPERTY_ID,
          SERVICE_CONFIG_VERSION_SERVICE_NAME_PROPERTY_ID}));


  // ----- Constructors ------------------------------------------------------

  /**
   * Constructor
   *
   * @param propertyIds           the property ids supported by this provider
   * @param keyPropertyIds        the key properties for this provider
   * @param managementController  the associated management controller
   */
  ServiceConfigVersionResourceProvider(Set<String> propertyIds,
                                Map<Resource.Type, String> keyPropertyIds,
                                AmbariManagementController managementController) {

    super(propertyIds, keyPropertyIds, managementController);
  }


  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

  @Override
  public RequestStatus createResources(Request request) throws SystemException, UnsupportedPropertyException, ResourceAlreadyExistsException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("Cannot explicitly create service config version");
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate) throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    final Set<ServiceConfigVersionRequest> requests = new HashSet<ServiceConfigVersionRequest>();
    for (Map<String, Object> properties : getPropertyMaps(predicate)) {
      requests.add(createRequest(properties));
    }

    Set<ServiceConfigVersionResponse> responses = getResources(new Command<Set<ServiceConfigVersionResponse>>() {
      @Override
      public Set<ServiceConfigVersionResponse> invoke() throws AmbariException {
        return getManagementController().getServiceConfigVersions(requests);
      }
    });

    Set<Resource> resources = new HashSet<Resource>();
    for (ServiceConfigVersionResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.ServiceConfigVersion);
      resource.setProperty(SERVICE_CONFIG_VERSION_CLUSTER_NAME_PROPERTY_ID, response.getClusterName());
      resource.setProperty(SERVICE_CONFIG_VERSION_SERVICE_NAME_PROPERTY_ID, response.getServiceName());
      resource.setProperty(SERVICE_CONFIG_VERSION_USER_PROPERTY_ID, response.getUserName());
      resource.setProperty(SERVICE_CONFIG_VERSION_PROPERTY_ID, response.getVersion());
      resource.setProperty(SERVICE_CONFIG_VERSION_CREATE_TIME_PROPERTY_ID, response.getCreateTime());
      resource.setProperty(SERVICE_CONFIG_VERSION_CONFIGURATIONS_PROPERTY_ID,
          convertToSubResources(response.getClusterName(), response.getConfigurations()));
      resource.setProperty(SERVICE_CONFIG_VERSION_NOTE_PROPERTY_ID, response.getNote());
      resource.setProperty(SERVICE_CONFIG_VERSION_GROUP_ID_PROPERTY_ID, response.getGroupId());
      resource.setProperty(SERVICE_CONFIG_VERSION_GROUP_NAME_PROPERTY_ID, response.getGroupName());
      resource.setProperty(SERVICE_CONFIG_VERSION_HOSTS_PROPERTY_ID, response.getHosts());
      resource.setProperty(SERVICE_CONFIG_VERSION_IS_CURRENT_PROPERTY_ID, response.getIsCurrent());

      resources.add(resource);
    }
    return resources;
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate) throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("Cannot update service config version");
  }

  @Override
  public RequestStatus deleteResources(Predicate predicate) throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("Cannot delete service config version");
  }

  @Override
  public Set<String> checkPropertyIds(Set<String> propertyIds) {
    propertyIds = super.checkPropertyIds(propertyIds);

    if (propertyIds.isEmpty()) {
      return propertyIds;
    }
    Set<String> unsupportedProperties = new HashSet<String>();

    for (String propertyId : propertyIds) {

      if (!propertyId.equals("cluster_name") && !propertyId.equals("service_config_version") &&
          !propertyId.equals("service_name") && !propertyId.equals("createtime") &&
          !propertyId.equals("appliedtime") && !propertyId.equals("user") &&
          !propertyId.equals("service_config_version_note") &&
          !propertyId.equals("group_id") &&
          !propertyId.equals("group_name") &&
          !propertyId.equals("is_current") &&
          !propertyId.equals("hosts")) {

        unsupportedProperties.add(propertyId);

      }
    }
    return unsupportedProperties;
  }


  private ServiceConfigVersionRequest createRequest(Map<String, Object> properties) {
    String clusterName = (String) properties.get(SERVICE_CONFIG_VERSION_CLUSTER_NAME_PROPERTY_ID);
    String serviceName = (String) properties.get(SERVICE_CONFIG_VERSION_SERVICE_NAME_PROPERTY_ID);
    String user = (String) properties.get(SERVICE_CONFIG_VERSION_USER_PROPERTY_ID);
    Object versionObject = properties.get(SERVICE_CONFIG_VERSION_PROPERTY_ID);
    Long version = versionObject == null ? null : Long.valueOf(versionObject.toString());

    return new ServiceConfigVersionRequest(clusterName, serviceName, version, null, null, user);
  }

  private List<Map<String, Object>> convertToSubResources(final String clusterName, List<ConfigurationResponse> configs) {
    List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
    for (final ConfigurationResponse config : configs) {
      Map<String, Object> configMap = new LinkedHashMap<String, Object>();
      configMap.put("Config", new HashMap<String, String>(){{put("cluster_name", clusterName);}});
      configMap.put("type", config.getType());
      configMap.put("tag", config.getVersionTag());
      configMap.put("version", config.getVersion());
      configMap.put("properties", new TreeMap(config.getConfigs()));
      configMap.put("properties_attributes", config.getConfigAttributes());
      result.add(configMap);
    }

    return result;
  }
}
