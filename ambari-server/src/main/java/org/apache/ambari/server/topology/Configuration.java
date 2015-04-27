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

  private Map<String, Map<String, String>> properties;
  private Map<String, Map<String, Map<String, String>>> attributes;

  private Configuration parentConfiguration;

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

  public Configuration(Map<String, Map<String, String>> properties,
                       Map<String, Map<String, Map<String, String>>> attributes) {

    this.properties = properties;
    this.attributes = attributes;
  }

  public Map<String, Map<String, String>> getProperties() {
    return properties;
  }

  public Map<String, Map<String, String>> getFullProperties() {
    return getFullProperties(Integer.MAX_VALUE);
  }

  //re-calculated each time in case parent properties changed
  public Map<String, Map<String, String>> getFullProperties(int depthLimit) {

    if (depthLimit == 0) {
      return new HashMap<String, Map<String, String>>(properties);
    }

    Map<String, Map<String, String>> mergedProperties = parentConfiguration == null ?
        new HashMap<String, Map<String, String>>() :
        new HashMap<String, Map<String, String>>(parentConfiguration.getFullProperties(--depthLimit));

    for (Map.Entry<String, Map<String, String>> entry : properties.entrySet()) {
      String configType = entry.getKey();
      Map<String, String> typeProps = entry.getValue();

      if (mergedProperties.containsKey(configType)) {
        mergedProperties.get(configType).putAll(typeProps);
      } else {
        mergedProperties.put(configType, typeProps);
      }
    }
    return mergedProperties;
  }

  public Map<String, Map<String, Map<String, String>>> getAttributes() {
    return attributes;
  }

  //re-calculate each time in case parent properties changed
  // attribute structure is very confusing.  {type -> {attributeName -> {propName, attributeValue}}}
  public Map<String, Map<String, Map<String, String>>> getFullAttributes() {
    Map<String, Map<String, Map<String, String>>> mergedAttributeMap = parentConfiguration == null ?
        new HashMap<String, Map<String, Map<String, String>>>() :
        new HashMap<String, Map<String, Map<String, String>>>(parentConfiguration.getFullAttributes());

    for (Map.Entry<String, Map<String, Map<String, String>>> typeEntry : attributes.entrySet()) {
      String type = typeEntry.getKey();
      if (! mergedAttributeMap.containsKey(type)) {
        mergedAttributeMap.put(type, typeEntry.getValue());
      } else {
        Map<String, Map<String, String>> mergedAttributes = mergedAttributeMap.get(type);
        for (Map.Entry<String, Map<String, String>> attributeEntry : typeEntry.getValue().entrySet()) {
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

    mergedAttributeMap.putAll(attributes);

    return mergedAttributeMap;
  }

  public Collection<String> getAllConfigTypes() {
    Collection<String> allTypes = new HashSet<String>();
    for (String type : getFullProperties().keySet()) {
      allTypes.add(type);
    }

    for (String type : getFullAttributes().keySet()) {
      allTypes.add(type);
    }

    return allTypes;
  }

  public Configuration getParentConfiguration() {
    return parentConfiguration;
  }

  public void setParentConfiguration(Configuration parent) {
    parentConfiguration = parent;
  }

  public String getPropertyValue(String configType, String propertyName) {
    return properties.containsKey(configType) ?
        properties.get(configType).get(propertyName) : null;
  }

  public boolean containsProperty(String configType, String propertyName) {
    return properties.containsKey(configType) && properties.get(configType).containsKey(propertyName);
  }

  public String setProperty(String configType, String propertyName, String value) {
    Map<String, String> typeProperties = properties.get(configType);
    if (typeProperties == null) {
      typeProperties = new HashMap<String, String>();
      properties.put(configType, typeProperties);
    }

    return typeProperties.put(propertyName, value);
  }

  // attribute structure is very confusing: {type -> {attributeName -> {propName, attributeValue}}}
  public String setAttribute(String configType, String propertyName, String attributeName, String attributeValue) {
    Map<String, Map<String, String>> typeAttributes = attributes.get(configType);
    if (typeAttributes == null) {
      typeAttributes = new HashMap<String, Map<String, String>>();
      attributes.put(configType, typeAttributes);
    }

    Map<String, String> attributes = typeAttributes.get(attributeName);
    if (attributes == null) {
      attributes = new HashMap<String, String>();
      typeAttributes.put(attributeName, attributes);
    }

    return attributes.put(propertyName, attributeValue);
  }

}
