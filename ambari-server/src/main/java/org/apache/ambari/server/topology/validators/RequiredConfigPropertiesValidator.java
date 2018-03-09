/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.topology.validators;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.HostGroup;
import org.apache.ambari.server.topology.InvalidTopologyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Validates the configuration by checking the existence of required properties for the services listed in the blueprint.
 * Required properties are specified in the stack and are tied to config types and services.
 *
 * The validator ignores password properties that should never be specified in the artifacts (blueprint / cluster creation template)
 */
public class RequiredConfigPropertiesValidator implements TopologyValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(RequiredConfigPropertiesValidator.class);

  /**
   * Validates the configuration coming from the blueprint and cluster creation template and ensures that all the required properties are provided.
   * It's expected, that a in hostgroup containing components for a given service all required configuration for the given service is available.
   *
   * @param topology the topology instance holding the configuration for cluster provisioning
   * @throws InvalidTopologyException when there are missing configuration types or properties related to services in the blueprint
   */
  @Override
  public void validate(ClusterTopology topology) throws InvalidTopologyException {

    // collect required properties
    Map<String, Map<String, Collection<String>>> requiredPropertiesByService = getRequiredPropertiesByService(topology);

    // find missing properties in the cluster configuration
    Map<String, Collection<String>> missingProperties = new TreeMap<>();
    Map<String, Map<String, String>> topologyConfiguration = new HashMap<>(topology.getConfiguration().getFullProperties(1));

    for (Map.Entry<String, Map<String, Collection<String>>> entry : requiredPropertiesByService.entrySet()) {
      String service = entry.getKey();
      Map<String, Collection<String>> requiredPropertiesByType = entry.getValue();

      for (HostGroup hostGroup : topology.getBlueprint().getHostGroups().values()) {
        LOGGER.debug("Processing configurations for service {} in hostgroup {}", service, hostGroup.getName());

        // copy of all configurations available in the topology hgConfig -> topologyConfig -> bpConfig
        Map<String, Map<String, String>> operationalConfigurations = new HashMap<>(topologyConfiguration);

        for (Map.Entry<String, Map<String, String>> hostgroupConfigEntry : hostGroup.getConfiguration().getProperties().entrySet()) {
          if (operationalConfigurations.containsKey(hostgroupConfigEntry.getKey())) {
            operationalConfigurations.get(hostgroupConfigEntry.getKey()).putAll(hostgroupConfigEntry.getValue());
          } else {
            operationalConfigurations.put(hostgroupConfigEntry.getKey(), hostgroupConfigEntry.getValue());
          }
        }

        for (String configType : requiredPropertiesByType.keySet()) {

          // We need a copy not to modify the original
          Collection<String> requiredPropertiesForType = new HashSet<>(requiredPropertiesByType.get(configType));

          if (!operationalConfigurations.containsKey(configType)) {
            // all required configuration is missing for the config type
            missingProperties = addToMissingProperties(missingProperties, hostGroup.getName(), requiredPropertiesForType);
            continue;
          }

          Collection<String> operationalConfigsForType = operationalConfigurations.get(configType).keySet();
          requiredPropertiesForType.removeAll(operationalConfigsForType);
          if (!requiredPropertiesForType.isEmpty()) {
            LOGGER.info("Found missing properties in hostgroup: {}, config type: {}, mising properties: {}", hostGroup.getName(),
              configType, requiredPropertiesForType);
            missingProperties = addToMissingProperties(missingProperties, hostGroup.getName(), requiredPropertiesForType);
          }
        }
      }

    }

    if (!missingProperties.isEmpty()) {
      throw new InvalidTopologyException("Missing required properties.  Specify a value for these " +
        "properties in the blueprint or cluster creation template configuration. " + missingProperties);
    }

  }


  /**
   * Collects required properties for services in the blueprint. Configuration properties are returned by configuration type.
   * service -> configType -> properties
   *
   * @return a map with configuration types mapped to collections of required property names
   */

  private Map<String, Map<String, Collection<String>>> getRequiredPropertiesByService(ClusterTopology topology) {

    Map<String, Map<String, Collection<String>>> requiredPropertiesForServiceByType = new HashMap<>();

    for (String bpService : topology.getServices()) {
      LOGGER.debug("Collecting required properties for the service: {}", bpService);

      Collection<Stack.ConfigProperty> requiredConfigsForService = topology.getStack().getRequiredConfigurationProperties(bpService);
      Map<String, Collection<String>> requiredPropertiesByConfigType = new HashMap<>();

      for (Stack.ConfigProperty configProperty : requiredConfigsForService) {

        if (configProperty.getPropertyTypes() != null && configProperty.getPropertyTypes().contains(PropertyInfo.PropertyType.PASSWORD)) {
          LOGGER.debug("Skipping required property validation for password type: {}", configProperty.getName());
          // skip password types
          continue;
        }

        // add / get  service related required propeByType map
        if (requiredPropertiesForServiceByType.containsKey(bpService)) {
          requiredPropertiesByConfigType = requiredPropertiesForServiceByType.get(bpService);
        } else {
          LOGGER.debug("Adding required properties entry for service: {}", bpService);
          requiredPropertiesForServiceByType.put(bpService, requiredPropertiesByConfigType);
        }

        // add collection of required properties
        Collection<String> requiredPropsForType = new HashSet<>();
        if (requiredPropertiesByConfigType.containsKey(configProperty.getType())) {
          requiredPropsForType = requiredPropertiesByConfigType.get(configProperty.getType());
        } else {
          LOGGER.debug("Adding required properties entry for configuration type: {}", configProperty.getType());
          requiredPropertiesByConfigType.put(configProperty.getType(), requiredPropsForType);
        }

        requiredPropsForType.add(configProperty.getName());
        LOGGER.debug("Added required property for service; {}, configuration type: {}, property: {}", bpService,
          configProperty.getType(), configProperty.getName());
      }
    }

    LOGGER.info("Identified required properties for blueprint services: {}", requiredPropertiesForServiceByType);
    return requiredPropertiesForServiceByType;

  }

  private Map<String, Collection<String>> addToMissingProperties(Map<String, Collection<String>> missingProperties, String hostGroup, Collection<String> values) {
    Map<String, Collection<String>> missing;

    if (missingProperties == null) {
      missing = new TreeMap<>();
    } else {
      missing = new TreeMap<>(missingProperties);
    }

    if (!missing.containsKey(hostGroup)) {
      missing.put(hostGroup, new TreeSet<>());
    }

    missing.get(hostGroup).addAll(values);

    return missing;
  }


}
