package org.apache.ambari.server.controller.internal;

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

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ConfigurationRequest;
import org.apache.ambari.server.controller.ConfigurationResponse;
import org.apache.ambari.server.controller.ServiceComponentHostRequest;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PredicateHelper;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * Resource provider for configuration resources.
 */
class ConfigurationResourceProvider extends ResourceProviderImpl {

  // ----- Property ID constants ---------------------------------------------

  // Configurations (values are part of query strings and body post, so they don't have defined categories)
  protected static final String CONFIGURATION_CLUSTER_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("Config", "cluster_name");
  // TODO : should these be Config/type and Config/tag to be consistent?
  protected static final String CONFIGURATION_CONFIG_TYPE_PROPERTY_ID  = PropertyHelper.getPropertyId(null, "type");
  protected static final String CONFIGURATION_CONFIG_TAG_PROPERTY_ID   = PropertyHelper.getPropertyId(null, "tag");

  private static final String CONFIG_HOST_NAME = PropertyHelper.getPropertyId("Config", "host_name");
  private static final String CONFIG_COMPONENT_NAME = PropertyHelper.getPropertyId("Config", "component_name");


  private static Set<String> pkPropertyIds =
      new HashSet<String>(Arrays.asList(new String[]{
          CONFIGURATION_CLUSTER_NAME_PROPERTY_ID,
          CONFIGURATION_CONFIG_TYPE_PROPERTY_ID}));

  ConfigurationResourceProvider(Set<String> propertyIds,
                                Map<Resource.Type, String> keyPropertyIds,
                                AmbariManagementController managementController) {

    super(propertyIds, keyPropertyIds, managementController);

  }

  @Override
  public RequestStatus createResources(Request request)
      throws SystemException,
             UnsupportedPropertyException,
             ResourceAlreadyExistsException,
             NoSuchParentResourceException {

    for (Map<String, Object> map : request.getProperties()) {

      String cluster = (String) map.get(CONFIGURATION_CLUSTER_NAME_PROPERTY_ID);
      // TODO : why not CONFIGURATION_CONFIG_TYPE_PROPERTY_ID?
      String type = (String) map.get(PropertyHelper.getPropertyId("", "type"));
      // TODO : why not CONFIGURATION_CONFIG_TAG_PROPERTY_ID?
      String tag = (String) map.get(PropertyHelper.getPropertyId("", "tag"));

      Map<String, String> configMap = new HashMap<String, String>();

      for (Entry<String, Object> entry : map.entrySet()) {
        if (PropertyHelper.getPropertyCategory(entry.getKey()).equals("properties") && null != entry.getValue()) {
          configMap.put(PropertyHelper.getPropertyName(entry.getKey()), entry.getValue().toString());
        }
      }

      final ConfigurationRequest configRequest = new ConfigurationRequest(cluster, type, tag, configMap);

      createResources(new Command<Void>() {
        @Override
        public Void invoke() throws AmbariException {
          getManagementController().createConfiguration(configRequest);
          return null;
        }
      });

    }
    return getRequestStatus(null);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
    throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    Map<String, Object> map = PredicateHelper.getProperties(predicate);
    
    if (map.containsKey(CONFIG_HOST_NAME) && map.containsKey(CONFIG_COMPONENT_NAME)) {
      final ServiceComponentHostRequest hostComponentRequest = new ServiceComponentHostRequest(
          (String) map.get(CONFIGURATION_CLUSTER_NAME_PROPERTY_ID),
          null,
          (String) map.get(CONFIG_COMPONENT_NAME),
          (String) map.get(CONFIG_HOST_NAME),
          null, null);
      
      Map<String, String> mappints = getResources(new Command<Map<String, String>>() {
        @Override
        public Map<String, String> invoke() throws AmbariException {
          return getManagementController().getHostComponentDesiredConfigMapping(hostComponentRequest);
        }
      });

      Set<Resource> resources = new HashSet<Resource>();
      
      for (Entry<String, String> entry : mappints.entrySet()) {
      
        Resource resource = new ResourceImpl(Resource.Type.Configuration);
        
        resource.setProperty(CONFIGURATION_CLUSTER_NAME_PROPERTY_ID, map.get(CONFIGURATION_CLUSTER_NAME_PROPERTY_ID));
        resource.setProperty(CONFIG_COMPONENT_NAME, map.get(CONFIG_COMPONENT_NAME));
        resource.setProperty(CONFIG_HOST_NAME, map.get(CONFIG_HOST_NAME));

        resource.setProperty(CONFIGURATION_CONFIG_TYPE_PROPERTY_ID, entry.getKey());
        resource.setProperty(CONFIGURATION_CONFIG_TAG_PROPERTY_ID, entry.getValue());
        
        resources.add(resource);
      }


      return resources;
      
    } else {
      // TODO : handle multiple requests
      final ConfigurationRequest configRequest = getRequest(map);
      
      Set<ConfigurationResponse> responses = getResources(new Command<Set<ConfigurationResponse>>() {
        @Override
        public Set<ConfigurationResponse> invoke() throws AmbariException {
          return getManagementController().getConfigurations(Collections.singleton(configRequest));
        }
      });

      Set<Resource> resources = new HashSet<Resource>();
      for (ConfigurationResponse response : responses) {
        Resource resource = new ResourceImpl(Resource.Type.Configuration);
        resource.setProperty(CONFIGURATION_CLUSTER_NAME_PROPERTY_ID, response.getClusterName());
        resource.setProperty(CONFIGURATION_CONFIG_TYPE_PROPERTY_ID, response.getType());
        resource.setProperty(CONFIGURATION_CONFIG_TAG_PROPERTY_ID, response.getVersionTag());
        
        if (null != response.getConfigs() && response.getConfigs().size() > 0) {
          Map<String, String> configs = response.getConfigs();

          for (Entry<String, String> entry : configs.entrySet()) {
            String id = PropertyHelper.getPropertyId("properties", entry.getKey());
            resource.setProperty(id, entry.getValue());
          }
        }

        resources.add(resource);
      }
      
      return resources;
    }
  }

  /**
   * Throws an exception, as Configurations cannot be updated.
   */
  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("Cannot update a Configuration resource.");
  }

  /**
   * Throws an exception, as Configurations cannot be deleted.
   */
  @Override
  public RequestStatus deleteResources(Predicate predicate) throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("Cannot delete a Configuration resource.");
  }

  @Override
  public Set<String> checkPropertyIds(Set<String> propertyIds) {
    propertyIds = super.checkPropertyIds(propertyIds);

    if (propertyIds.isEmpty()) {
      return propertyIds;
    }
    Set<String> unsupportedProperties = new HashSet<String>();

    for (String propertyId : propertyIds) {

      // TODO : hack to allow for inconsistent property names
      // for example, the tag property can come here as Config/tag, /tag or tag
      if (!propertyId.equals("tag") && !propertyId.equals("type") &&
          !propertyId.equals("/tag") && !propertyId.equals("/type")) {

        String propertyCategory = PropertyHelper.getPropertyCategory(propertyId);

        if (propertyCategory == null || !propertyCategory.equals("properties")) {
          unsupportedProperties.add(propertyId);
        }
      }
    }
    return unsupportedProperties;
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

  public static Map<String, String> getConfigPropertyValues(Map<String, Object> propertyMap) {
    Map<String, String> configMap = new HashMap<String, String>();

    for (Map.Entry<String,Object> entry : propertyMap.entrySet()) {
      String propertyId = entry.getKey();
      if (PropertyHelper.getPropertyCategory(propertyId).equals("config")) {
        configMap.put(PropertyHelper.getPropertyName(propertyId), (String) entry.getValue());
      }
    }
    return configMap;
  }

  private ConfigurationRequest getRequest(Map<String, Object> properties) {
    String type = (String) properties.get(CONFIGURATION_CONFIG_TYPE_PROPERTY_ID);

    String tag = (String) properties.get(CONFIGURATION_CONFIG_TAG_PROPERTY_ID);

    return new ConfigurationRequest(
        (String) properties.get(CONFIGURATION_CLUSTER_NAME_PROPERTY_ID),
        type, tag, new HashMap<String, String>());
  }
}
