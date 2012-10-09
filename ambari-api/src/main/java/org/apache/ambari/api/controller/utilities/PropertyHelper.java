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
package org.apache.ambari.api.controller.utilities;

import org.apache.ambari.api.controller.internal.PropertyIdImpl;
import org.apache.ambari.api.controller.internal.RequestImpl;
import org.apache.ambari.server.controller.spi.PropertyId;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class PropertyHelper {

  private static final String PROPERTIES_FILE = "properties.json";
  private static final String KEY_PROPERTIES_FILE = "key_properties.json";

  private static final Map<Resource.Type, Map<String, Set<PropertyId>>> PROPERTY_IDS = readPropertyIds(PROPERTIES_FILE);
  private static final Map<Resource.Type, Map<Resource.Type, PropertyId>> KEY_PROPERTY_IDS = readKeyPropertyIds(KEY_PROPERTIES_FILE);

  public static PropertyId getPropertyId(String name, String category) {
    return new PropertyIdImpl(name, category, false);
  }

  public static PropertyId getPropertyId(String name, String category, boolean temporal) {
    return new PropertyIdImpl(name, category, temporal);
  }

  public static Set<PropertyId> getPropertyIds(Resource.Type resourceType, String providerKey) {

    Map<String, Set<PropertyId>> propertyIds = PROPERTY_IDS.get(resourceType);
    if (propertyIds != null) {
      return propertyIds.get(providerKey);
    }
    return Collections.emptySet();
  }

  public static Map<Resource.Type, PropertyId> getKeyPropertyIds(Resource.Type resourceType) {
    return KEY_PROPERTY_IDS.get(resourceType);
  }

  public static Map<PropertyId, String> getProperties(Resource resource) {
    Map<PropertyId, String> properties = new HashMap<PropertyId, String>();

    Map<String, Map<String, String>> categories = resource.getCategories();

    for (Map.Entry<String, Map<String, String>> categoryEntry : categories.entrySet()) {
      for (Map.Entry<String, String>  propertyEntry : categoryEntry.getValue().entrySet()) {

        properties.put(PropertyHelper.getPropertyId(propertyEntry.getKey(), categoryEntry.getKey()), propertyEntry.getValue());
      }
    }
    return properties;
  }

  /**
   * Factory method to create a create request from the given set of property maps.
   * Each map contains the properties to be used to create a resource.  Multiple maps in the
   * set should result in multiple creates.
   *
   * @param properties   the properties associated with the request; may be null
   */
  public static Request getCreateRequest(Set<Map<PropertyId, String>> properties) {
    return new RequestImpl(null,  properties);
  }

  /**
   * Factory method to create a read request from the given set of property ids.  The set of
   * property ids represents the properties of interest for the query.
   *
   * @param propertyIds  the property ids associated with the request; may be null
   */
  public static Request getReadRequest(Set<PropertyId> propertyIds) {
    return new RequestImpl(propertyIds,  null);
  }

  /**
   * Factory method to create an update request from the given map of properties.
   * The properties values in the given map are used to update the resource.
   *
   * @param properties   the properties associated with the request; may be null
   */
  public static Request getUpdateRequest(Map<PropertyId, String> properties) {
    return new RequestImpl(null,  Collections.singleton(properties));
  }

  private static Map<Resource.Type, Map<String, Set<PropertyId>>> readPropertyIds(String filename) {
    ObjectMapper mapper = new ObjectMapper();

    try {
      return mapper.readValue(ClassLoader.getSystemResourceAsStream(filename), new TypeReference<Map<Resource.Type, Map<String, Set<PropertyIdImpl>>>>() {
      });
    } catch (IOException e) {
      throw new IllegalStateException("Can't read properties file " + filename, e);
    }
  }

  private static Map<Resource.Type, Map<Resource.Type, PropertyId>> readKeyPropertyIds(String filename) {
    ObjectMapper mapper = new ObjectMapper();

    try {
      return mapper.readValue(ClassLoader.getSystemResourceAsStream(filename), new TypeReference<Map<Resource.Type, Map<Resource.Type, PropertyIdImpl>>>() {
      });
    } catch (IOException e) {
      throw new IllegalStateException("Can't read properties file " + filename, e);
    }
  }
}
