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

import javax.annotation.Nullable;

import org.apache.ambari.annotations.ApiIgnore;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import io.swagger.annotations.ApiModelProperty;

/**
 * Provides support for JSON serializaion of {@link Configuration} objects. Can handle both plain JSON and Ambari style
 * flattened JSON such as {@code "hdfs-site/properties/dfs.replication": "3"}. Objects may implement this interface or
 * call its static utility methods.
 */
public interface Configurable {

  public static final String CONFIGURATIONS = "configurations";

  @JsonIgnore
  @ApiIgnore
  void setConfiguration(Configuration configuration);

  @JsonIgnore
  @ApiIgnore
  Configuration getConfiguration();

  @JsonProperty(CONFIGURATIONS)
  @ApiModelProperty(name = CONFIGURATIONS)
  default void setConfigs(Collection<? extends Map<String, ?>> configs) {
    setConfiguration(parseConfigs(configs));
  }

  @JsonProperty(CONFIGURATIONS)
  @ApiModelProperty(name = CONFIGURATIONS)
  default Collection<Map<String, Map<String, ?>>> getConfigs() {
    return convertConfigToMap(getConfiguration());
  }

  /**
   * Parses configuration maps The configs can be in fully structured JSON, e.g.
   * <code>
   * [{"hdfs-site":
   *  "properties": {
   *    ""dfs.replication": "3",
   *    ...
   *  },
   *  properties_attributes: {}
   * }]
   * </code>
   * or flattened like
   * <code>
   * [{
   *  "hdfs-site/properties/dfs.replication": "3",
   *  ...
   * }]
   * </code>
   * In the latter case it calls {@link ConfigurationFactory#getConfiguration(Collection)}
   * @param configs
   * @return
   */
  static Configuration parseConfigs(@Nullable Collection<? extends Map<String, ?>> configs) {
    Configuration configuration;

    if (null == configs) {
      configuration = new Configuration(new HashMap<>(), new HashMap<>());
    }
    else if (!configs.isEmpty() && configs.iterator().next().keySet().iterator().next().contains("/")) {
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
    return configuration;
  }

  /**
   * Converts {@link Configuration} objects to a collection easily serializable to Json
   * @param configuration the configuration to convert
   * @return the resulting collection
   */
  static Collection<Map<String, Map<String, ?>>> convertConfigToMap(Configuration configuration) {
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
