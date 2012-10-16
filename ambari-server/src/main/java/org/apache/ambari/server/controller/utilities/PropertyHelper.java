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
package org.apache.ambari.server.controller.utilities;

import org.apache.ambari.server.controller.internal.PropertyIdImpl;
import org.apache.ambari.server.controller.internal.RequestImpl;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.PropertyId;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class PropertyHelper {

  private static final String PROPERTIES_FILE = "properties.json";
  private static final String KEY_PROPERTIES_FILE = "key_properties.json";
  private static final char EXTERNAL_PATH_SEP = '/';

  private static final Map<Resource.Type, Map<String, Set<PropertyId>>> PROPERTY_IDS = readPropertyIds(PROPERTIES_FILE);
  private static final Map<Resource.Type, Map<Resource.Type, PropertyId>> KEY_PROPERTY_IDS = readKeyPropertyIds(KEY_PROPERTIES_FILE);

  public static PropertyId getPropertyId(String name, String category) {
    return new PropertyIdImpl(name, category, false);
  }

  public static PropertyId getPropertyId(String name, String category, boolean temporal) {
    return new PropertyIdImpl(name, category, temporal);
  }

  /**
   * Helper to create a PropertyId from an string.
   * The provided string must not be null and should be the fully qualified property name.
   * The fully qualified property name may or may not have a category name.
   * if the fully qualified property name contains a path separator char, then the
   * path up to the last pth separator is considered the path and the token after the last
   * path separator char is the property name.  If no path separator is present, the category
   * is null and the property name is the provided string.
   *
   * @param absProperty  the fully qualified property
   *
   * @return a new PropertyId for the provided property string
   */
  public static PropertyId getPropertyId(String absProperty) {
    String category;
    String name;

    int lastPathSep = absProperty.lastIndexOf(EXTERNAL_PATH_SEP);
    if (lastPathSep == -1) {
      category = null;
      name     = absProperty;
    } else {
      category = absProperty.substring(0, lastPathSep);
      name     = absProperty.substring(lastPathSep + 1);
    }

    return getPropertyId(name, category);
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

  /**
   * Get a map of all the property values keyed by property id for the given resource.
   *
   * @param resource  the resource
   *
   * @return the map of properties for the given resource
   */
  public static Map<PropertyId, Object> getProperties(Resource resource) {
    Map<PropertyId, Object> properties = new HashMap<PropertyId, Object>();

    Map<String, Map<String, Object>> categories = resource.getCategories();

    for (Map.Entry<String, Map<String, Object>> categoryEntry : categories.entrySet()) {
      for (Map.Entry<String, Object>  propertyEntry : categoryEntry.getValue().entrySet()) {

        properties.put(PropertyHelper.getPropertyId(propertyEntry.getKey(), categoryEntry.getKey()), propertyEntry.getValue());
      }
    }
    return properties;
  }

  /**
   * Get the set of property ids required to satisfy the given request.
   *
   * @param providerPropertyIds  the provider property ids
   * @param request              the request
   * @param predicate            the predicate
   *
   * @return the set of property ids needed to satisfy the request
   */
  public static Set<PropertyId> getRequestPropertyIds(Set<PropertyId> providerPropertyIds,
                                                      Request request,
                                                      Predicate predicate) {
    Set<PropertyId> propertyIds         = request.getPropertyIds();
    Set<PropertyId> requestPropertyIds  = propertyIds == null ? null : new HashSet<PropertyId>(propertyIds);

    providerPropertyIds = new HashSet<PropertyId>(providerPropertyIds);

    // if no properties are specified, then return them all
    if (requestPropertyIds == null || requestPropertyIds.isEmpty()) {
//      // strip out the temporal properties, they must be asked for explicitly
//      Iterator<PropertyId> iter = providerPropertyIds.iterator();
//      while (iter.hasNext()) {
//        PropertyId propertyId = iter.next();
//        if (propertyId.isTemporal()) {
//          iter.remove();
//        }
//      }
      return providerPropertyIds;
    }

    if (predicate != null) {
      requestPropertyIds.addAll(PredicateHelper.getPropertyIds(predicate));
    }
    requestPropertyIds.retainAll(providerPropertyIds);
    return requestPropertyIds;
  }

  /**
   * For some reason the host names are stored all lower case.  Attempt to undo that with
   * this hack.
   *
   * @param host  the host name to be fixed
   *
   * @return the fixed host name
   */
  public static String fixHostName(String host) {
    int first_dash = host.indexOf('-');
    int first_dot = host.indexOf('.');
    String segment1 = host.substring(0, first_dash);
    if (segment1.equals("domu")) {
      segment1 = "domU";
    }
    String segment2 = host.substring(first_dash, first_dot).toUpperCase();
    host = segment1 + segment2 + host.substring(first_dot);
    return host;
  }

  /**
   * Factory method to create a create request from the given set of property maps.
   * Each map contains the properties to be used to create a resource.  Multiple maps in the
   * set should result in multiple creates.
   *
   * @param properties   the properties associated with the request; may be null
   */
  public static Request getCreateRequest(Set<Map<PropertyId, Object>> properties) {
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
  public static Request getUpdateRequest(Map<PropertyId, Object> properties) {
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
