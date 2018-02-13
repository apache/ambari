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

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.RootComponent;
import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.controller.internal.StackDefinition;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.HostGroupInfo;
import org.apache.ambari.server.topology.InvalidTopologyException;
import org.apache.ambari.server.topology.ResolvedComponent;

/**
 * Validates that all required passwords are provided.
 */
public class RequiredPasswordValidator implements TopologyValidator {

  /**
   * Validate that all required password properties have been set or that 'default_password' is specified.
   *
   * @throws InvalidTopologyException if required password properties are missing and no
   *                                  default is specified via 'default_password'
   */
  public void validate(ClusterTopology topology) throws InvalidTopologyException {

    Map<String, Map<String, Set<String>>> missingPasswords = validateRequiredPasswords(topology);

    if (! missingPasswords.isEmpty()) {
      throw new InvalidTopologyException("Missing required password properties.  Specify a value for these " +
          "properties in the cluster or host group configurations or include 'default_password' field in request. " +
          missingPasswords);
    }
  }

  /**
   * Validate all configurations.  Validation is done on the operational configuration of each
   * host group.  An operational configuration is achieved by overlaying host group configuration
   * on top of cluster configuration which overlays the default stack configurations.
   *
   * @return map of required properties which are missing.  Empty map if none are missing.
   *
   * @throws IllegalArgumentException if blueprint contains invalid information
   */
  private Map<String, Map<String, Set<String>>> validateRequiredPasswords(ClusterTopology topology) {
    Map<String, Map<String, Set<String>>> missingProperties = new HashMap<>();

    StackDefinition stack = topology.getStack();
    String defaultPassword = topology.getDefaultPassword();
    boolean hasDefaultPassword = defaultPassword != null && !defaultPassword.trim().isEmpty();

    for (Map.Entry<String, HostGroupInfo> groupEntry: topology.getHostGroupInfo().entrySet()) {
      String hostGroupName = groupEntry.getKey();
      Map<String, Map<String, String>> groupProperties =
          groupEntry.getValue().getConfiguration().getFullProperties(3);

      Map<String, Set<String>> missingPropertiesInHostGroup = topology.getComponentsInHostGroup(hostGroupName)
        .filter(component -> !RootComponent.AMBARI_SERVER.name().equals(component.getComponentName()))
        .map(ResolvedComponent::getServiceType)
        .distinct()
        .flatMap(serviceType -> stack.getRequiredConfigurationProperties(serviceType, PropertyInfo.PropertyType.PASSWORD).stream())
        .filter(property -> !propertyExists(groupProperties, property.getType(), property.getName()))
        .collect(groupingBy(Stack.ConfigProperty::getType, mapping(Stack.ConfigProperty::getName, toSet())));

      if (!missingPropertiesInHostGroup.isEmpty()) {
        if (hasDefaultPassword) {
          for (Map.Entry<String, Set<String>> entry : missingPropertiesInHostGroup.entrySet()) {
            String type = entry.getKey();
            for (String name : entry.getValue()) {
              topology.getConfiguration().setProperty(type, name, defaultPassword);
            }
          }
        } else {
          missingProperties.put(hostGroupName, missingPropertiesInHostGroup);
        }
      }
    }
    return missingProperties;
  }

  private boolean propertyExists(Map<String, Map<String, String>> props, String type, String property) {
    Map<String, String> typeProps = props.get(type);
    return typeProps != null && typeProps.containsKey(property);
  }

}
