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
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ConfigurationRequest;
import org.apache.ambari.server.controller.ConfigurationResponse;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Pattern;

/**
 * Resource provider for configuration resources.
 */
public class ConfigurationResourceProvider extends
  AbstractControllerResourceProvider {

  private static Pattern propertiesAttributesPattern = Pattern.compile("^" + PROPERTIES_ATTRIBUTES_REGEX);

  // ----- Property ID constants ---------------------------------------------

  // Configurations (values are part of query strings and body post, so they don't have defined categories)
  protected static final String CONFIGURATION_CLUSTER_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("Config", "cluster_name");
  // TODO : should these be Config/type and Config/tag to be consistent?
  public static final String CONFIGURATION_CONFIG_TYPE_PROPERTY_ID  =
    PropertyHelper.getPropertyId(null, "type");
  public static final String CONFIGURATION_CONFIG_TAG_PROPERTY_ID   =
    PropertyHelper.getPropertyId(null, "tag");
  public static final String CONFIGURATION_CONFIG_VERSION_PROPERTY_ID   =
    PropertyHelper.getPropertyId(null, "version");


  /**
   * The primary key property ids for the configuration resource type.
   */
  private static Set<String> pkPropertyIds =
      new HashSet<String>(Arrays.asList(new String[]{
          CONFIGURATION_CLUSTER_NAME_PROPERTY_ID,
          CONFIGURATION_CONFIG_TYPE_PROPERTY_ID}));


  // ----- Constructors ------------------------------------------------------

  /**
   * Constructor
   *
   * @param propertyIds           the property ids supported by this provider
   * @param keyPropertyIds        the key properties for this provider
   * @param managementController  the associated management controller
   */
  ConfigurationResourceProvider(Set<String> propertyIds,
                                Map<Resource.Type, String> keyPropertyIds,
                                AmbariManagementController managementController) {

    super(propertyIds, keyPropertyIds, managementController);
  }


  // ----- ResourceProvider --------------------------------------------------

  @Override
  public RequestStatus createResources(Request request)
      throws SystemException,
             UnsupportedPropertyException,
             ResourceAlreadyExistsException,
             NoSuchParentResourceException {

    for (Map<String, Object> map : request.getProperties()) {
      String cluster = (String) map.get(CONFIGURATION_CLUSTER_NAME_PROPERTY_ID);
      String type = (String) map.get(CONFIGURATION_CONFIG_TYPE_PROPERTY_ID);
      String tag  = (String) map.get(CONFIGURATION_CONFIG_TAG_PROPERTY_ID);

      Map<String, String> configMap = new HashMap<String, String>();
      Map<String, Map<String, String>> configAttributesMap = null;

      for (Entry<String, Object> entry : map.entrySet()) {
        String propertyCategory = PropertyHelper.getPropertyCategory(entry.getKey());
        if (propertyCategory != null && propertyCategory.equals("properties") && null != entry.getValue()) {
          configMap.put(PropertyHelper.getPropertyName(entry.getKey()), entry.getValue().toString());
        }
        if (propertyCategory != null && propertiesAttributesPattern.matcher(propertyCategory).matches() && null != entry.getValue()) {
          if (null == configAttributesMap) {
            configAttributesMap = new HashMap<String, Map<String,String>>();
          }
          String attributeName = propertyCategory.substring(propertyCategory.lastIndexOf('/') + 1);
          Map<String, String> attributesMap = configAttributesMap.get(attributeName);
          if (attributesMap == null) {
            attributesMap = new HashMap<String, String>();
            configAttributesMap.put(attributeName, attributesMap);
          }
          attributesMap.put(PropertyHelper.getPropertyName(entry.getKey()), entry.getValue().toString());
        }
      }

      final ConfigurationRequest configRequest = new ConfigurationRequest(cluster, type, tag, configMap, configAttributesMap);

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

    final Set<ConfigurationRequest> requests = new HashSet<ConfigurationRequest>();

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      requests.add(getRequest(request, propertyMap));
    }

    Set<ConfigurationResponse> responses = getResources(new Command<Set<ConfigurationResponse>>() {
      @Override
      public Set<ConfigurationResponse> invoke() throws AmbariException {
        return getManagementController().getConfigurations(requests);
      }
    });

    Set<Resource> resources = new HashSet<Resource>();
    for (ConfigurationResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.Configuration);
      resource.setProperty(CONFIGURATION_CLUSTER_NAME_PROPERTY_ID, response.getClusterName());
      resource.setProperty(CONFIGURATION_CONFIG_TYPE_PROPERTY_ID, response.getType());
      resource.setProperty(CONFIGURATION_CONFIG_TAG_PROPERTY_ID, response.getVersionTag());
      resource.setProperty(CONFIGURATION_CONFIG_VERSION_PROPERTY_ID, response.getVersion());

      if (null != response.getConfigs() && response.getConfigs().size() > 0) {
        Map<String, String> configs = response.getConfigs();

        for (Entry<String, String> entry : configs.entrySet()) {
          String id = PropertyHelper.getPropertyId("properties", entry.getKey());
          resource.setProperty(id, entry.getValue());
        }
      }
      if (null != response.getConfigAttributes() && response.getConfigAttributes().size() > 0) {
        Map<String, Map<String, String>> configAttributes = response.getConfigAttributes();
        for (Entry<String, Map<String, String>> configAttribute : configAttributes.entrySet()) {
          String id = PropertyHelper.getPropertyId("properties_attributes", configAttribute.getKey());
          resource.setProperty(id, configAttribute.getValue());
        }
      }
      resources.add(resource);
    }
    return resources;
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
  public RequestStatus deleteResources(Predicate predicate) throws SystemException,
      UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
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
          !propertyId.equals("/tag") && !propertyId.equals("/type") &&
          !propertyId.equals("properties") && !propertyId.equals("properties_attributes")) {

        String propertyCategory = PropertyHelper.getPropertyCategory(propertyId);

        if (propertyCategory == null || !(propertyCategory.equals("properties") || propertiesAttributesPattern.matcher(propertyCategory).matches())) {
          unsupportedProperties.add(propertyId);
        }
      }
    }
    return unsupportedProperties;
  }

  // ----- AbstractResourceProvider ------------------------------------------

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }


  // ----- utility methods ---------------------------------------------------

  /**
   * Get a configuration request object from the given map of properties.
   *
   * @param properties  the map of properties
   *
   * @return a configuration request
   */
  private ConfigurationRequest getRequest(Request request, Map<String, Object> properties) {
    String type = (String) properties.get(CONFIGURATION_CONFIG_TYPE_PROPERTY_ID);
    String tag  = (String) properties.get(CONFIGURATION_CONFIG_TAG_PROPERTY_ID);

    ConfigurationRequest configRequest = new ConfigurationRequest(
        (String) properties.get(CONFIGURATION_CLUSTER_NAME_PROPERTY_ID),
        type, tag, new HashMap<String, String>(), new HashMap<String, Map<String, String>>());

    Set<String> requestedIds = request.getPropertyIds();
    if (requestedIds.contains("properties") || requestedIds.contains("*")) {
      configRequest.setIncludeProperties(true);
    }

    return configRequest;
  }
}
