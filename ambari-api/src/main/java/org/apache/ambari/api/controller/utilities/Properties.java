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
import org.apache.ambari.api.controller.spi.PropertyId;
import org.apache.ambari.api.controller.spi.Resource;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class Properties {

  private static final String PROPERTIES_FILE = "properties.json";
  private static final String KEY_PROPERTIES_FILE = "key_properties.json";

  private static final Map<String, Map<String, Set<PropertyId>>> PROPERTY_IDS = readPropertyIds(PROPERTIES_FILE);
  private static final Map<String, Map<String, PropertyId>> KEY_PROPERTY_IDS = readKeyPropertyIds(KEY_PROPERTIES_FILE);

  public static PropertyId getPropertyId(String name, String category) {
    return new PropertyIdImpl(name, category, false);
  }

  public static PropertyId getPropertyId(String name, String category, boolean temporal) {
    return new PropertyIdImpl(name, category, temporal);
  }

  public static Set<PropertyId> getPropertyIds(Resource.Type resourceType, String providerKey) {

    Map<String, Set<PropertyId>> propertyIds = PROPERTY_IDS.get(resourceType.toString());
    if (propertyIds != null) {
      return propertyIds.get(providerKey);
    }
    return Collections.emptySet();
  }

  public static Map<String, PropertyId> getKeyPropertyIds(Resource.Type resourceType) {
    return KEY_PROPERTY_IDS.get(resourceType.toString());
  }

  private static Map<String, Map<String, Set<PropertyId>>> readPropertyIds(String filename) {
    ObjectMapper mapper = new ObjectMapper();

    try {
      return mapper.readValue(ClassLoader.getSystemResourceAsStream(filename), new TypeReference<Map<String, Map<String, Set<PropertyIdImpl>>>>() {
      });
    } catch (IOException e) {
      throw new IllegalStateException("Can't read properties file " + filename, e);
    }
  }

  private static Map<String, Map<String, PropertyId>> readKeyPropertyIds(String filename) {
    ObjectMapper mapper = new ObjectMapper();

    try {
      return mapper.readValue(ClassLoader.getSystemResourceAsStream(filename), new TypeReference<Map<String, Map<String, PropertyIdImpl>>>() {
      });
    } catch (IOException e) {
      throw new IllegalStateException("Can't read properties file " + filename, e);
    }
  }

}
