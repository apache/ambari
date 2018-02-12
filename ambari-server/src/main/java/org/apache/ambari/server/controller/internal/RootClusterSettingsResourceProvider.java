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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ReadOnlyConfigurationResponse;
import org.apache.ambari.server.controller.RootClusterSettingRequest;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Resource.Type;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.commons.lang.StringUtils;

import com.google.inject.assistedinject.Assisted;

public class RootClusterSettingsResourceProvider extends ReadOnlyResourceProvider {

  public static final String RESPONSE_KEY = "ClusterSettingsInfo";
  public static final String ALL_PROPERTIES = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "*";

  public static final String PROPERTY_NAME_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "property_name";
  public static final String PROPERTY_DISPLAY_NAME_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "property_display_name";
  public static final String PROPERTY_VALUE_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "property_value";
  public static final String PROPERTY_VALUE_ATTRIBUTES_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "property_value_attributes";
  public static final String DEPENDS_ON_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "property_depends_on";
  public static final String PROPERTY_DESCRIPTION_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "property_description";
  public static final String PROPERTY_PROPERTY_TYPE_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "property_type";
  public static final String PROPERTY_TYPE_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "type";
  public static final String PROPERTY_FINAL_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "final";

  private static Set<String> pkPropertyIds = new HashSet<>(Arrays.asList(new String[]{PROPERTY_NAME_PROPERTY_ID}));

  /**
   * The key property ids for 'cluster_setting' resource.
   */
  private static final Map<Resource.Type, String> KEY_PROPERTY_IDS = new HashMap<>();

  /**
   * The property ids for an 'cluster_setting' resource.
   */
  private static final Set<String> PROPERTY_IDS = new HashSet<>();

  static {
    // properties
    PROPERTY_IDS.add(PROPERTY_NAME_PROPERTY_ID);
    PROPERTY_IDS.add(PROPERTY_DISPLAY_NAME_PROPERTY_ID);
    PROPERTY_IDS.add(PROPERTY_VALUE_PROPERTY_ID);
    PROPERTY_IDS.add(PROPERTY_VALUE_ATTRIBUTES_PROPERTY_ID);
    PROPERTY_IDS.add(DEPENDS_ON_PROPERTY_ID);
    PROPERTY_IDS.add(PROPERTY_DESCRIPTION_PROPERTY_ID);
    PROPERTY_IDS.add(PROPERTY_PROPERTY_TYPE_PROPERTY_ID);
    PROPERTY_IDS.add(PROPERTY_TYPE_PROPERTY_ID);
    PROPERTY_IDS.add(PROPERTY_FINAL_PROPERTY_ID);

    // keys
    KEY_PROPERTY_IDS.put(Type.RootClusterSetting, PROPERTY_NAME_PROPERTY_ID);
  }

  protected RootClusterSettingsResourceProvider(@Assisted AmbariManagementController managementController) {
    super(Type.RootClusterSetting, PROPERTY_IDS, KEY_PROPERTY_IDS, managementController);
  }


  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
          throws SystemException, UnsupportedPropertyException,
          NoSuchResourceException, NoSuchParentResourceException {

    final Set<RootClusterSettingRequest> requests = new HashSet<>();

    if (predicate == null) {
      requests.add(getRequest(Collections.emptyMap()));
    } else {
      for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
        requests.add(getRequest(propertyMap));
      }
    }

    Set<String> requestedIds = getRequestPropertyIds(request, predicate);

    Set<ReadOnlyConfigurationResponse> responses = getResources(new Command<Set<ReadOnlyConfigurationResponse>>() {
      @Override
      public Set<ReadOnlyConfigurationResponse> invoke() throws AmbariException {
        return getManagementController().getResourceLevelClusterSettings(requests);
      }
    });

    Set<Resource> resources = new HashSet<>();

    for (ReadOnlyConfigurationResponse response : responses) {
      Resource resource = new ResourceImpl(Type.RootClusterSetting);

      setResourceProperty(resource, PROPERTY_NAME_PROPERTY_ID, response.getPropertyName(), requestedIds);
      setResourceProperty(resource, PROPERTY_VALUE_PROPERTY_ID, response.getPropertyValue(), requestedIds);
      setResourceProperty(resource, PROPERTY_VALUE_ATTRIBUTES_PROPERTY_ID, response.getPropertyValueAttributes(), requestedIds);
      setResourceProperty(resource, DEPENDS_ON_PROPERTY_ID, response.getDependsOnProperties(), requestedIds);
      setResourceProperty(resource, PROPERTY_DESCRIPTION_PROPERTY_ID, response.getPropertyDescription(), requestedIds);

      //should not be returned if empty
      if (StringUtils.isNotEmpty(response.getPropertyDisplayName())) {
        setResourceProperty(resource, PROPERTY_DISPLAY_NAME_PROPERTY_ID, response.getPropertyDisplayName(), requestedIds);
      }

      setResourceProperty(resource, PROPERTY_PROPERTY_TYPE_PROPERTY_ID, response.getPropertyType(), requestedIds);

      setResourceProperty(resource, PROPERTY_TYPE_PROPERTY_ID, response.getType(), requestedIds);

      setDefaultPropertiesAttributes(resource, requestedIds);

      for (Map.Entry<String, String> attribute : response.getPropertyAttributes().entrySet()) {
        setResourceProperty(resource, PropertyHelper.getPropertyId(RESPONSE_KEY, attribute.getKey()),
                attribute.getValue(), requestedIds);
      }
      resources.add(resource);
    }
    return resources;
  }

  /**
   * Set default values for properties attributes before applying original ones
   * to prevent absence in case of empty attributes map
   */
  private void setDefaultPropertiesAttributes(Resource resource, Set<String> requestedIds) {
    setResourceProperty(resource, PROPERTY_FINAL_PROPERTY_ID,
            "false", requestedIds);
  }

  private RootClusterSettingRequest getRequest(Map<String, Object> properties) {
    return new RootClusterSettingRequest((String) properties.get(PROPERTY_NAME_PROPERTY_ID));
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

}