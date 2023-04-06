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
import java.util.Map;

/**
 * Creates a configuration instance from user provided properties.
 * Supports both forms of configuration syntax.
 * todo: document both forms here
*/
public class ConfigurationFactory {

  private static final String SCHEMA_IS_NOT_SUPPORTED_MESSAGE =
      "Provided configuration format is not supported";

  public Configuration getConfiguration(Collection<Map<String, String>> configProperties) {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, Map<String, Map<String, String>>> attributes = new HashMap<>();
    Configuration configuration = new Configuration(properties, attributes);

    if (configProperties != null) {
      for (Map<String, String> typeMap : configProperties) {
        //todo: can we have a different strategy for each type?
        ConfigurationStrategy strategy = decidePopulationStrategy(typeMap);
        for (Map.Entry<String, String> entry : typeMap.entrySet()) {
          String[] propertyNameTokens = entry.getKey().split("/");
          strategy.setConfiguration(configuration, propertyNameTokens, entry.getValue());
        }
      }
    }
    return configuration;
  }

  private ConfigurationStrategy decidePopulationStrategy(Map<String, String> configuration) {
    if (configuration != null && !configuration.isEmpty()) {
      String keyEntry = configuration.keySet().iterator().next();
      String[] keyNameTokens = keyEntry.split("/");
      int levels = keyNameTokens.length;
      String propertiesType = keyNameTokens[1];
      if (levels == 2) {
        return new ConfigurationStrategyV1();
      } else if ((levels == 3 && BlueprintFactory.PROPERTIES_PROPERTY_ID.equals(propertiesType))
          || (levels == 4 && BlueprintFactory.PROPERTIES_ATTRIBUTES_PROPERTY_ID.equals(propertiesType))) {
        return new ConfigurationStrategyV2();
      } else {
        throw new IllegalArgumentException(SCHEMA_IS_NOT_SUPPORTED_MESSAGE);
      }
    } else {
      return new ConfigurationStrategyV2();
    }
  }

  /**
   * The structure of blueprints is evolving where multiple resource
   * structures are to be supported. This class abstracts the population
   * of configurations which have changed from a map of key-value strings,
   * to an map containing 'properties' and 'properties_attributes' maps.
   *
   * Extending classes can determine how they want to populate the
   * configuration maps depending on input.
   */
  private static abstract class ConfigurationStrategy {

    protected abstract void setConfiguration(Configuration configuration,
                                             String[] propertyNameTokens,
                                             String propertyValue);

  }

  /**
   * Original blueprint configuration format where configs were a map
   * of strings.
   */
  protected static class ConfigurationStrategyV1 extends ConfigurationStrategy {


    @Override
    protected void setConfiguration(Configuration configuration, String[] propertyNameTokens, String propertyValue) {
      configuration.setProperty(propertyNameTokens[0], propertyNameTokens[1], propertyValue);
    }
  }

  /**
   * New blueprint configuration format where configs are a map from 'properties' and
   * 'properties_attributes' to a map of strings.
   *
   * @since 1.7.0
   */
  protected static class ConfigurationStrategyV2 extends ConfigurationStrategy {

    @Override
    protected void setConfiguration(Configuration configuration, String[] propertyNameTokens, String propertyValue) {
      String type = propertyNameTokens[0];
      if (BlueprintFactory.PROPERTIES_PROPERTY_ID.equals(propertyNameTokens[1])) {
        configuration.setProperty(type, propertyNameTokens[2], propertyValue);
      } else if (BlueprintFactory.PROPERTIES_ATTRIBUTES_PROPERTY_ID.equals(propertyNameTokens[1])) {
        configuration.setAttribute(type, propertyNameTokens[3], propertyNameTokens[2], propertyValue);
      }
    }
  }
}
