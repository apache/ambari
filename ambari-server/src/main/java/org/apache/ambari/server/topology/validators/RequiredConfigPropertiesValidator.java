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

import org.apache.ambari.server.controller.StackV2;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.HostGroupV2;
import org.apache.ambari.server.topology.InvalidTopologyException;
import org.apache.ambari.server.topology.Service;
import org.apache.ambari.server.topology.TopologyValidator;
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
   * It's expected, that a in hostrgroup containing components for a given service all required configuration for the given service is available.
   *
   * @param topology the topology instance holding the configuration for cluster provisioning
   * @throws InvalidTopologyException when there are missing configuration types or properties related to services in the blueprint
   */
  @Override
  public void validate(ClusterTopology topology) throws InvalidTopologyException {

    // find missing properties in the cluster configuration
    Map<String, Collection<String>> missingProperties = new TreeMap<>();
    Map<String, Map<String, String>> topologyConfiguration = new HashMap<>(topology.getConfiguration().getFullProperties(1));

    for (HostGroupV2 hostGroup : topology.getBlueprint().getHostGroups().values()) {
      LOGGER.debug("Processing hostgroup configurations for hostgroup: {}", hostGroup.getName());

      // copy of all configurations available in the topology hgConfig -> topologyConfig -> bpConfig
      Map<String, Map<String, String>> operationalConfigurations = new HashMap<>(topologyConfiguration);

      for (Map.Entry<String, Map<String, String>> hostgroupConfigEntry : hostGroup.getConfiguration().getProperties().entrySet()) {
        if (operationalConfigurations.containsKey(hostgroupConfigEntry.getKey())) {
          operationalConfigurations.get(hostgroupConfigEntry.getKey()).putAll(hostgroupConfigEntry.getValue());
        } else {
          operationalConfigurations.put(hostgroupConfigEntry.getKey(), hostgroupConfigEntry.getValue());
        }
      }

      for (Service hostGroupService : hostGroup.getServices()) {

        // collect required properties
        Map<String, Collection<String>> requiredPropertiesForService = getRequiredPropertiesForService(hostGroupService);
        if (requiredPropertiesForService.isEmpty()) {
          // there are no required properties for the service
          LOGGER.debug("There are no required properties found for hostgroup/service: [{}/{}]", hostGroup.getName(), hostGroupService);
          continue;
        }

        for (String configType : requiredPropertiesForService.keySet()) {

          // We need a copy not to modify the original
          Collection<String> requiredPropertiesForType = new HashSet(
            requiredPropertiesForService.get(configType));

          if (!operationalConfigurations.containsKey(configType)) {
            // all required configuration is missing for the config type
            missingProperties = addTomissingProperties(missingProperties, hostGroup.getName(), requiredPropertiesForType);
            continue;
          }

          Collection<String> operationalConfigsForType = operationalConfigurations.get(configType).keySet();
          requiredPropertiesForType.removeAll(operationalConfigsForType);
          if (!requiredPropertiesForType.isEmpty()) {
            LOGGER.info("Found missing properties in hostgroup: {}, config type: {}, mising properties: {}", hostGroup.getName(),
              configType, requiredPropertiesForType);
            missingProperties = addTomissingProperties(missingProperties, hostGroup.getName(), requiredPropertiesForType);
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
   * Collects required properties for a specified services in the blueprint. Configuration properties are returned
   * by configuration type. configType -> properties
   * @param service the blueprint from the cluster topology
   * @return a map with configuration types mapped to collections of required property names
   */

  private Map<String, Collection<String>> getRequiredPropertiesForService(Service service) {

    LOGGER.debug("Collecting required properties for the service: {}", service.getName());

    Collection<StackV2.ConfigProperty> requiredConfigsForService = service.getStack().
      getRequiredConfigurationProperties(service.getType());
    Map<String, Collection<String>> requiredPropertiesByConfigType = new HashMap<>();

    for (StackV2.ConfigProperty configProperty : requiredConfigsForService) {

      if (configProperty.getPropertyTypes() != null && configProperty.getPropertyTypes().contains(PropertyInfo.PropertyType.PASSWORD)) {
        LOGGER.debug("Skipping required property validation for password type: {}", configProperty.getName());
        // skip password types
        continue;
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
      LOGGER.debug("Added required property for service; {}, configuration type: {}, property: {}", service.getName(),
        configProperty.getType(), configProperty.getName());
    }

    return requiredPropertiesByConfigType;

  }

  private Map<String, Collection<String>> addTomissingProperties(Map<String, Collection<String>> missingProperties,
                                                                 String hostGroup, Collection<String> values) {
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
