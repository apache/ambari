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

package org.apache.ambari.server.topology;

import static org.apache.ambari.server.controller.internal.BlueprintResourceProvider.PROPERTIES_ATTRIBUTES_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.BlueprintResourceProvider.PROPERTIES_PROPERTY_ID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;



public interface Configurable {

  @JsonIgnore
  void setConfiguration(Configuration configuration);
  @JsonIgnore
  Configuration getConfiguration();

  @JsonProperty("configurations")
  default void setConfigs(Collection<? extends Map<String, ?>> configs) {
    Configuration configuration;
    if (!configs.isEmpty() && configs.iterator().next().keySet().iterator().next().contains("/")) {
      // Configuration has keys with slashes like "zk.cfg/properties/dataDir" means it is coming through
      // the resource framework and must be parsed with configuration factories
      configuration = new ConfigurationFactory().getConfiguration((Collection<Map<String, String>>)configs);
    }
    else {
      // If the configuration does not have keys with slashes it means it is coming from plain JSON and needs to be
      // parsed accordingly.
      Map<String, Map<String, String>> allProperties = new HashMap<>();
      Map<String, Map<String, Map<String, String>>> allAttributes = new HashMap<>();
      configs.forEach( item -> {
        String configName = item.keySet().iterator().next();
        Map<String, Object> configData = (Map<String, Object>) item.get(configName);
        if (configData.containsKey(PROPERTIES_PROPERTY_ID)) {
          Map<String, String> properties = (Map<String, String>)configData.get(PROPERTIES_PROPERTY_ID);
          allProperties.put(configName, properties);
        }
        if (configData.containsKey(PROPERTIES_ATTRIBUTES_PROPERTY_ID)) {
          Map<String, Map<String, String>> attributes =
            (Map<String, Map<String, String>>)configData.get(PROPERTIES_ATTRIBUTES_PROPERTY_ID);
          allAttributes.put(configName, attributes);
        }
      });
      configuration = new Configuration(allProperties, allAttributes);
    }
    setConfiguration(configuration);
  }

  @JsonProperty("configurations")
  default Collection<Map<String, Map<String, ?>>> getConfigs() {
    Configuration configuration = getConfiguration();
    Collection<Map<String, Map<String, ?>>> configurations = new ArrayList<>();
    Set<String> allConfigTypes = Sets.union(configuration.getProperties().keySet(), configuration.getAttributes().keySet());
    for (String configType: allConfigTypes) {
      Map<String, Map<String, ? extends Object>> configData = new HashMap<>();
      if (configuration.getProperties().containsKey(configType)) {
        configData.put(PROPERTIES_PROPERTY_ID, configuration.getProperties().get(configType));
      }
      if (configuration.getAttributes().containsKey(configType)) {
        configData.put(PROPERTIES_ATTRIBUTES_PROPERTY_ID, configuration.getAttributes().get(configType));
      }
      configurations.add(ImmutableMap.of(configType, configData));
    }
    return configurations;
  }

}
