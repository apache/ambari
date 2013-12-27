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

import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PredicateHelper;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base provider implementation for both property and resource providers.
 */
public abstract class BaseProvider {

  /**
   * Set of property ids supported by this provider.
   */
  private final Set<String> propertyIds;

  /**
   * Set of category ids supported by this provider.
   */
  private final Set<String> categoryIds;

  /**
   * Combined property and category ids.
   */
  private final Set<String> combinedIds;

  /**
   * Pre-compiled patterns for metric names that contain regular expressions.
   */
  private final Map<String, Pattern> patterns;

  /**
   * The logger.
   */
  protected final static Logger LOG =
      LoggerFactory.getLogger(BaseProvider.class);


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct a provider.
   *
   * @param propertyIds  the properties associated with this provider
   */
  public BaseProvider(Set<String> propertyIds) {
    this.propertyIds = new HashSet<String>(propertyIds);
    this.categoryIds = PropertyHelper.getCategories(propertyIds);
    this.combinedIds = new HashSet<String>(propertyIds);
    this.combinedIds.addAll(this.categoryIds);
    this.patterns = new HashMap<String, Pattern>();
    for (String id : this.combinedIds) {
      if (containsArguments(id)) {
        String pattern = id.replaceAll("\\$\\d+(\\.\\S+\\(\\S+\\))*", "(\\\\S+)");
        patterns.put(id, Pattern.compile(pattern));
      }
    }
  }


  // ----- BaseProvider --------------------------------------------------
  /**
   * Checks for property ids that are not recognized by the provider.
   * @param base  the base set of properties
   * @param configCategory  the config category that would have a <code>desired_config</code> element.
   * @return the set of properties that are NOT known to the provider
   */
  protected Set<String> checkConfigPropertyIds(Set<String> base, String configCategory) {

    if (0 == base.size())
      return base;

    Set<String> unsupported = new HashSet<String>();

    for (String propertyId : base)
    {
      if (!propertyId.startsWith(configCategory + "/desired_config"))
        unsupported.add(propertyId);
    }

    return unsupported;
  }

  public Set<String> checkPropertyIds(Set<String> propertyIds) {
    if (!this.propertyIds.containsAll(propertyIds)) {
      Set<String> unsupportedPropertyIds = new HashSet<String>(propertyIds);
      unsupportedPropertyIds.removeAll(this.combinedIds);

      // If the property id is not in the set of known property ids we may still allow it if
      // its parent category is a known property. This allows for Map type properties where
      // we want to treat property as a category and the entries as individual properties.
      Set<String> categoryProperties = new HashSet<String>();
      for (String unsupportedPropertyId : unsupportedPropertyIds) {
        if (checkCategory(unsupportedPropertyId) || checkRegExp(unsupportedPropertyId)) {
          categoryProperties.add(unsupportedPropertyId);
        }
      }
      unsupportedPropertyIds.removeAll(categoryProperties);

      return unsupportedPropertyIds;
    }
    return Collections.emptySet();
  }

  /**
   * Get the set of property ids required to satisfy the given request.
   *
   * @param request              the request
   * @param predicate            the predicate
   *
   * @return the set of property ids needed to satisfy the request
   */
  protected Set<String> getRequestPropertyIds(Request request, Predicate predicate) {
    Set<String> propertyIds  = request.getPropertyIds();

    // if no properties are specified, then return them all
    if (propertyIds == null || propertyIds.isEmpty()) {
      return new HashSet<String>(this.propertyIds);
    }

    propertyIds = new HashSet<String>(propertyIds);

    if (predicate != null) {
      propertyIds.addAll(PredicateHelper.getPropertyIds(predicate));
    }

    if (!this.combinedIds.containsAll(propertyIds)) {
      Set<String> keepers = new HashSet<String>();
      Set<String> unsupportedPropertyIds = new HashSet<String>(propertyIds);
      unsupportedPropertyIds.removeAll(this.combinedIds);

      for (String unsupportedPropertyId : unsupportedPropertyIds) {
        if (checkCategory(unsupportedPropertyId) || checkRegExp(unsupportedPropertyId)) {
          keepers.add(unsupportedPropertyId);
        }
      }
      propertyIds.retainAll(this.combinedIds);
      propertyIds.addAll(keepers);
    }
    return propertyIds;
  }

  /**
   * Check the categories to account for map properties where the entries will not be
   * in the provider property list ids but the map (category) might be.
   */
  private boolean checkCategory(String unsupportedPropertyId) {
    String category = PropertyHelper.getPropertyCategory(unsupportedPropertyId);
    while (category != null) {
      if( this.propertyIds.contains(category)) {
        return true;
      }
      category = PropertyHelper.getPropertyCategory(category);
    }
    return false;
  }

  private boolean checkRegExp(String unsupportedPropertyId) {
    for (Pattern pattern : patterns.values()) {
      Matcher matcher = pattern.matcher(unsupportedPropertyId);
      if (matcher.matches()) {
        return true;
      }
    }
    return false;
  }

  protected String getRegExpKey(String id) {
    String regExpKey = null;
    for (Map.Entry<String, Pattern> entry : patterns.entrySet()) {
      Pattern pattern = entry.getValue();
      Matcher matcher = pattern.matcher(id);

      if (matcher.matches()) {
        String key = entry.getKey();
        if (regExpKey == null || key.startsWith(regExpKey)) {
          regExpKey = entry.getKey();
        }
      }
    }
    return regExpKey;
  }

  /**
   * Extracts set of matcher.group() from id
   * @param regExpKey
   * @param id
   * @return extracted regex groups from id
   */
  protected List<String> getRegexGroups(String regExpKey, String id) {
    Pattern pattern = patterns.get(regExpKey);
    List<String> regexGroups = new ArrayList<String>();

    if (pattern != null) {
      Matcher matcher = pattern.matcher(id);

      if (matcher.matches()) {
        for (int i=0; i<matcher.groupCount(); i++){
          regexGroups.add(matcher.group(i + 1));
        }
      }
    }

    return regexGroups;
  }

  protected boolean isPatternKey(String id) {
    return patterns.containsKey(id);
  }

  /**
   * Check to see if the given property id contains replacement arguments (e.g. $1)
   *
   * @param propertyId  the property id to check
   *
   * @return true if the given property id contains any replacement arguments
   */
  protected boolean containsArguments(String propertyId) {
    return PropertyHelper.containsArguments(propertyId);
  }

  /**
   * Set a property value on the given resource for the given id and value.
   * Make sure that the id is in the given set of requested ids.
   *
   * @param resource      the resource
   * @param propertyId    the property id
   * @param value         the value to set
   * @param requestedIds  the requested set of property ids
   */
  protected static boolean setResourceProperty(Resource resource, String propertyId, Object value, Set<String> requestedIds) {
    boolean contains = requestedIds.contains(propertyId);

    if (!contains) {
      String category = PropertyHelper.getPropertyCategory(propertyId);
      while (category != null && !contains) {
        contains = requestedIds.contains(category);
        category = PropertyHelper.getPropertyCategory(category);
      }
    }

    if (contains) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Setting property for resource"
            + ", resourceType=" + resource.getType()
            + ", propertyId=" + propertyId
            + ", value=" + value);
      }

      // If the value is a Map then set all of its entries as properties
      if (!setResourceMapProperty(resource, propertyId, value)){
        resource.setProperty(propertyId, value);
      }
    }
    else {

      if (value instanceof Map<?, ?>) {
        // This map wasn't requested, but maybe some of its entries were...
        Map<?, ?> mapValue = (Map) value;

        for (Map.Entry entry : mapValue.entrySet()) {
          String entryPropertyId = PropertyHelper.getPropertyId(propertyId, entry.getKey().toString());
          Object entryValue      = entry.getValue();

          contains = setResourceProperty(resource, entryPropertyId, entryValue, requestedIds) || contains;
        }
      }

      if (!contains && LOG.isDebugEnabled()) {
        LOG.debug("Skipping property for resource as not in requestedIds"
            + ", resourceType=" + resource.getType()
            + ", propertyId=" + propertyId
            + ", value=" + value);
      }
    }
    return contains;
  }

  /**
   * If the given value is a Map then add its entries to the resource as properties.
   *
   * @param resource    the resource
   * @param propertyId  the property id of the given value
   * @param value       the property value
   */
  private static boolean setResourceMapProperty(Resource resource, String propertyId, Object value) {
    if (value instanceof Map<?, ?>) {
      Map<?, ?> mapValue = (Map) value;

      if (mapValue.isEmpty()) {
        resource.addCategory(propertyId);
      } else {
        for (Map.Entry entry : mapValue.entrySet()) {
          String entryPropertyId = PropertyHelper.getPropertyId(propertyId, entry.getKey().toString());
          Object entryValue      = entry.getValue();

          // If the value is a Map then set all of its entries as properties
          if (!setResourceMapProperty(resource, entryPropertyId, entryValue)){
            resource.setProperty(entryPropertyId, entryValue);
          }
        }
      }
      return true;
    }
    return false;
  }


  // ----- accessors ---------------------------------------------------------

  /**
   * Get the property ids supported by this property adapter.
   *
   * @return the property ids supported by this provider
   */
  public Set<String> getPropertyIds() {
    return propertyIds;
  }
}
