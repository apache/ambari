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

import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.topology.Blueprint;
import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.Configuration;
import org.apache.ambari.server.topology.HostGroup;
import org.apache.ambari.server.topology.InvalidTopologyException;
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
   *
   * @param topology the topology instance holding the configuration for cluster provisioning
   * @throws InvalidTopologyException when there are missing configuration types or properties related to services in the blueprint
   */
  @Override
  public void validate(ClusterTopology topology) throws InvalidTopologyException {

    // collect cluster configuration
    Map<String, Map<String, String>> clusterConfigurations = getClusterConfiguration(topology.getConfiguration(), topology.getBlueprint().getHostGroups().values());

    // collect required properties
    Map<String, Collection<String>> requiredPropertiesByType = getRequiredProperties(topology.getBlueprint());

    // find missing properties in the cluster configuration
    Map<String, Collection<String>> missingProperties = new HashMap<>();

    for (Map.Entry<String, Collection<String>> requiredPropsEntry : requiredPropertiesByType.entrySet()) {
      String configType = requiredPropsEntry.getKey();
      Collection<String> requiredProps = requiredPropsEntry.getValue();
      LOGGER.debug("Checking required properties. Config type: {}, Required properties: {}", configType, requiredProps);

      if (clusterConfigurations.containsKey(configType)) {
        Map<String, String> clusterProps = clusterConfigurations.get(configType);
        requiredProps.removeAll(clusterProps.keySet());
      }

      if (!requiredProps.isEmpty()) {
        LOGGER.debug("Found missing properties. Config type: {}, Missing properties: {}", configType, requiredProps);
        missingProperties.put(configType, requiredProps);
      }
    }

    if (!missingProperties.isEmpty()) {
      throw new InvalidTopologyException("Missing required properties.  Specify a value for these " +
        "properties in the blueprint or cluster creation template configuration. " + missingProperties);
    }

  }


  /**
   * Collects configuration provided in the blueprint, hostgroups and cluster creation template.
   */
  private Map<String, Map<String, String>> getClusterConfiguration(Configuration topologyConfiguration, Collection<HostGroup> hostGroups) {

    // get the combined configuration (bp + cct), the parent is the bp - the depth is 1 !!
    Map<String, Map<String, String>> clusterConfigurations = topologyConfiguration.getFullProperties(1);

    // add hostgroup properties
    for (HostGroup hostGroup : hostGroups) {
      clusterConfigurations.putAll(hostGroup.getConfiguration().getProperties());
    }

    return clusterConfigurations;
  }


  /**
   * Collects required properties for services in the blueprint.
   *
   * @param blueprint the blueprint from the cluster topology
   * @return a map with configuration types mapped to collections of required property names
   */

  private Map<String, Collection<String>> getRequiredProperties(Blueprint blueprint) {

    // collect required properties for services in the blueprint
    Collection<Stack.ConfigProperty> requiredServiceProperties = new HashSet<>();

    for (String bpService : blueprint.getServices()) {
      LOGGER.debug("Collecting required properties for the service: {}", bpService);
      requiredServiceProperties.addAll(blueprint.getStack().getRequiredConfigurationProperties(bpService));
    }

    // transform required properties to the representation of the cluster configs
    Map<String, Collection<String>> requiredPropertiesByType = new HashMap<>();
    for (Stack.ConfigProperty requiredProperty : requiredServiceProperties) {

      if (requiredProperty.getPropertyTypes() != null && requiredProperty.getPropertyTypes().contains(PropertyInfo.PropertyType.PASSWORD)) {
        LOGGER.debug("Skipping required property validation for password type: {}", requiredProperty.getName());
        // skip password types
        continue;
      }

      Collection<String> requiredPropertiesForConfigType = requiredPropertiesByType.get(requiredProperty.getType());
      if (null == requiredPropertiesForConfigType) {
        // there's a set of props for each config type
        requiredPropertiesForConfigType = new HashSet<>();
        requiredPropertiesByType.put(requiredProperty.getType(), requiredPropertiesForConfigType);
      }
      requiredPropertiesForConfigType.add(requiredProperty.getName());
    }

    return requiredPropertiesByType;
  }

}
