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
 * distributed under the License is distribut
 * ed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.topology;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Configuration for a topology entity such as a blueprint, hostgroup or cluster.
 */
public class Configuration {
  /**
   * properties for this configuration instance
   */
  private Map<String, Map<String, String>> properties;

  /**
   * attributes for this configuration instance
   */
  private Map<String, Map<String, Map<String, String>>> attributes;

  /**
   * parent configuration
   */
  private Configuration parentConfiguration;

  /**
   * Constructor.
   *
   * @param properties           properties
   * @param attributes           attributes
   * @param parentConfiguration  parent configuration
   */
  public Configuration(Map<String, Map<String, String>> properties,
                       Map<String, Map<String, Map<String, String>>> attributes,
                       Configuration parentConfiguration) {

    this.properties = properties;
    this.attributes = attributes;
    this.parentConfiguration = parentConfiguration;

    //todo: warning for deprecated global properties
    //    String message = null;
//    for (BlueprintConfigEntity blueprintConfig: blueprint.getConfigurations()){
//      if(blueprintConfig.getType().equals("global")){
//        message = "WARNING: Global configurations are deprecated, please use *-env";
//        break;
//      }
//    }
  }

  /**
   * Configuration.
   *
   * @param properties  properties
   * @param attributes  attributes
   */
  public Configuration(Map<String, Map<String, String>> properties,
                       Map<String, Map<String, Map<String, String>>> attributes) {

    this.properties = properties;
    this.attributes = attributes;
  }

  /**
   * Get the properties for this instance only; parent properties are not included.
   *
   * @return map of properties for this configuration instance keyed by config type
   */
  public Map<String, Map<String, String>> getProperties() {
    return properties;
  }

  /**
   * Get a complete merged map of properties including this instance and the entire parent hierarchy.
   * Properties are merged so that children override the same property specified in it's parent hierarchy.
   * This result is re-calculated for each request in case a parent value has changed so the result
   * should be cached if possible.
   *
   * @return complete map of merged properties keyed by config type
   */
  public Map<String, Map<String, String>> getFullProperties() {
    return getFullProperties(Integer.MAX_VALUE);
  }

  /**
   * Get a merged map of properties including this instance and n levels of the parent hierarchy.
   * Properties are merged so that children override the same property specified in it's parent hierarchy.
   * This result is re-calculated for each request in case a parent value has changed so the result
   * should be cached if possible.
   *
   * @param depthLimit  the number of parent levels to include in the results.  Specifying 0 is the same
   *                    as calling {@link #getProperties()}
   *
   * @return map of merged properties keyed by config type
   */
  public Map<String, Map<String, String>> getFullProperties(int depthLimit) {
    if (depthLimit == 0) {
      HashMap<String, Map<String, String>> propertiesCopy = new HashMap<>();
      for (Map.Entry<String, Map<String, String>> typeProperties : properties.entrySet()) {
        propertiesCopy.put(typeProperties.getKey(), new HashMap<>(typeProperties.getValue()));
      }
      return propertiesCopy;
    }

    Map<String, Map<String, String>> mergedProperties = parentConfiguration == null ?
      new HashMap<>() :
      new HashMap<>(parentConfiguration.getFullProperties(--depthLimit));

    for (Map.Entry<String, Map<String, String>> entry : properties.entrySet()) {
      String configType = entry.getKey();
      Map<String, String> typeProps = new HashMap<>(entry.getValue());

      if (mergedProperties.containsKey(configType)) {
        mergedProperties.get(configType).putAll(typeProps);
      } else {
        mergedProperties.put(configType, typeProps);
      }
    }
    return mergedProperties;
  }

  /**
   * Get the attributes for this instance only; parent attributes aren't included.
   *
   * @return map of attributes {configType -> {attributeName -> {propName, attributeValue}}}
   */
  public Map<String, Map<String, Map<String, String>>> getAttributes() {
    return attributes;
  }

  /**
   * Get a complete merged map of attributes including this instance and the entire parent hierarchy.
   * Attributes are merged so that children override the same attribute specified in it's parent hierarchy.
   * This result is re-calculated for each request in case a parent value has changed so the result
   * should be cached if possible.
   *
   * @return complete map of merged attributes {configType -> {attributeName -> {propName, attributeValue}}}
   */
  public Map<String, Map<String, Map<String, String>>> getFullAttributes() {
    Map<String, Map<String, Map<String, String>>> mergedAttributeMap = parentConfiguration == null ?
      new HashMap<>() :
      new HashMap<>(parentConfiguration.getFullAttributes());

    for (Map.Entry<String, Map<String, Map<String, String>>> typeEntry : attributes.entrySet()) {
      String type = typeEntry.getKey();
      Map<String, Map<String, String>> typeAttributes = new HashMap<>();
      for (Map.Entry<String, Map<String, String>> attributeEntry : typeEntry.getValue().entrySet()) {
        typeAttributes.put(attributeEntry.getKey(), new HashMap<>(attributeEntry.getValue()));
      }

      if (! mergedAttributeMap.containsKey(type)) {
        mergedAttributeMap.put(type, typeAttributes);
      } else {
        Map<String, Map<String, String>> mergedAttributes = mergedAttributeMap.get(type);
        for (Map.Entry<String, Map<String, String>> attributeEntry : typeAttributes.entrySet()) {
          String attribute = attributeEntry.getKey();
          if (! mergedAttributes.containsKey(attribute)) {
            mergedAttributes.put(attribute, attributeEntry.getValue());
          } else {
            Map<String, String> mergedAttributeProps = mergedAttributes.get(attribute);
            for (Map.Entry<String, String> propEntry : attributeEntry.getValue().entrySet()) {
              mergedAttributeProps.put(propEntry.getKey(), propEntry.getValue());
            }
          }
        }
      }
    }
    return mergedAttributeMap;
  }

  /**
   * Get the requested property value from the full merged set of properties.
   *
   * @param configType    configuration type
   * @param propertyName  property name
   *
   * @return requested property value or null if property isn't set in configuration hierarchy
   */
  public String getPropertyValue(String configType, String propertyName) {
    String value = null;
    if (properties.containsKey(configType) && properties.get(configType).containsKey(propertyName)) {
      value = properties.get(configType).get(propertyName);
    } else if (parentConfiguration != null) {
      value = parentConfiguration.getPropertyValue(configType, propertyName);
    }

    return value;
  }

  /**
   * Get the requested attribute value from the full merged set of attributes.
   *
   * @param configType     configuration type
   * @param propertyName   attribute property name
   * @param attributeName  attribute name
   *
   * @return requested attribute value or null if the attribute isn't set in configuration hierarchy
   */
  public String getAttributeValue(String configType, String propertyName, String attributeName) {
    String value = null;
    if (attributes.containsKey(configType) &&
        attributes.get(configType).containsKey(attributeName) &&
        attributes.get(configType).get(attributeName).containsKey(propertyName)) {

      value = attributes.get(configType).get(attributeName).get(propertyName);
    } else if (parentConfiguration != null) {
      value = parentConfiguration.getAttributeValue(configType, propertyName, attributeName);
    }

    return value;
  }

  /**
   * Set a property on the configuration.
   * The property will be set on this instance so it will override any value specified in
   * the parent hierarchy.
   *
   * @param configType    configuration type
   * @param propertyName  property name
   * @param value         property value
   *
   * @return the previous value of the property or null if it didn't exist
   */
  public String setProperty(String configType, String propertyName, String value) {
    String previousValue = getPropertyValue(configType, propertyName);
    Map<String, String> typeProperties = properties.get(configType);
    if (typeProperties == null) {
      typeProperties = new HashMap<>();
      properties.put(configType, typeProperties);
    }
    typeProperties.put(propertyName, value);
    return previousValue;
  }

  /**
   * Remove a property from the configuration hierarchy.
   * All occurrences of the property are removed from the config hierarchy such that
   * a subsequent call to getPropertyValue() for the removed property will return null.
   *
   * @param configType    configuration type
   * @param propertyName  property name
   *
   * @return the previous value of the removed property or null if it didn't exist in the
   *         configuration hierarchy
   */
  public String removeProperty(String configType, String propertyName) {
    String previousValue = null;
    if (properties.containsKey(configType)) {
      previousValue = properties.get(configType).remove(propertyName);
    }

    if (parentConfiguration != null) {
      String parentPreviousValue =  parentConfiguration.removeProperty(configType, propertyName);
      if (previousValue == null) {
        previousValue = parentPreviousValue;
      }
    }
    return previousValue;
  }

  /**
   * Set an attribute on the hierarchy.
   * The attribute will be set on this instance so it will override any value specified in
   * the parent hierarchy.
   *
   * @param configType      configuration type
   * @param propertyName    attribute property name
   * @param attributeName   attribute name
   * @param attributeValue  attribute property value
   *
   * @return the previous value of the attribute or null if it didn't exist
   */
  public String setAttribute(String configType, String propertyName, String attributeName, String attributeValue) {
    String previousValue = getAttributeValue(configType, propertyName, attributeName);

    Map<String, Map<String, String>> typeAttributes = attributes.get(configType);
    if (typeAttributes == null) {
      typeAttributes = new HashMap<>();
      attributes.put(configType, typeAttributes);
    }

    Map<String, String> attributes = typeAttributes.get(attributeName);
    if (attributes == null) {
      attributes = new HashMap<>();
      typeAttributes.put(attributeName, attributes);
    }

    attributes.put(propertyName, attributeValue);
    return previousValue;
  }

  /**
   * Get the complete set of configuration types represented in both full properties and full attributes.
   *
   * @return collection of all represented configuration types
   */
  public Collection<String> getAllConfigTypes() {
    Collection<String> allTypes = new HashSet<>();
    for (String type : getFullProperties().keySet()) {
      allTypes.add(type);
    }

    for (String type : getFullAttributes().keySet()) {
      allTypes.add(type);
    }

    return allTypes;
  }

  /**
   * Get the parent configuration.
   *
   * @return the parent configuration or null if no parent is set
   */
  public Configuration getParentConfiguration() {
    return parentConfiguration;
  }

  /**
   * Set the parent configuration.
   *
   * @param parent parent configuration to set
   */
  public void setParentConfiguration(Configuration parent) {
    parentConfiguration = parent;
  }

  /**
   * Remove all occurrences of a config type
   */
  public void removeConfigType(String configType) {
    if (properties != null && properties.containsKey(configType)) {
      properties.remove(configType);
    }
    if (attributes != null && attributes.containsKey(configType)) {
      attributes.remove(configType);
    }
    if (parentConfiguration != null) {
      parentConfiguration.removeConfigType(configType);
    }
  }
}
