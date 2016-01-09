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

import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.state.PropertyInfo;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Validates that all required passwords are provided.
 */
public class RequiredPasswordValidator implements TopologyValidator {

  private String defaultPassword;

  public RequiredPasswordValidator(String defaultPassword) {
    this.defaultPassword = defaultPassword;
  }

  /**
   * Validate that all required password properties have been set or that 'default_password' is specified.
   *
   * @throws InvalidTopologyException if required password properties are missing and no
   *                                  default is specified via 'default_password'
   */
  public void validate(ClusterTopology topology) throws InvalidTopologyException {
    Map<String, Map<String, Collection<String>>> missingPasswords = validateRequiredPasswords(topology);

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

  //todo: this is copied/pasted from Blueprint and is currently only used by validatePasswordProperties()
  //todo: seems that we should have some common place for this code so it can be used by BP and here?
  private Map<String, Map<String, Collection<String>>> validateRequiredPasswords(ClusterTopology topology) {

    Map<String, Map<String, Collection<String>>> missingProperties =
        new HashMap<String, Map<String, Collection<String>>>();

    for (Map.Entry<String, HostGroupInfo> groupEntry: topology.getHostGroupInfo().entrySet()) {
      String hostGroupName = groupEntry.getKey();
      Map<String, Map<String, String>> groupProperties =
          groupEntry.getValue().getConfiguration().getFullProperties(3);

      Collection<String> processedServices = new HashSet<String>();
      Blueprint blueprint = topology.getBlueprint();
      Stack stack = blueprint.getStack();

      HostGroup hostGroup = blueprint.getHostGroup(hostGroupName);
      for (String component : hostGroup.getComponentNames()) {
        //for now, AMBARI is not recognized as a service in Stacks
        if (component.equals("AMBARI_SERVER")) {
          continue;
        }

        String serviceName = stack.getServiceForComponent(component);
        if (processedServices.add(serviceName)) {
          //todo: do I need to subtract excluded configs?
          Collection<Stack.ConfigProperty> requiredProperties =
              stack.getRequiredConfigurationProperties(serviceName, PropertyInfo.PropertyType.PASSWORD);

          for (Stack.ConfigProperty property : requiredProperties) {
            String category = property.getType();
            String name = property.getName();
            if (! propertyExists(topology, groupProperties, category, name)) {
              Map<String, Collection<String>> missingHostGroupPropsMap = missingProperties.get(hostGroupName);
              if (missingHostGroupPropsMap == null) {
                missingHostGroupPropsMap = new HashMap<String, Collection<String>>();
                missingProperties.put(hostGroupName, missingHostGroupPropsMap);
              }
              Collection<String> missingHostGroupTypeProps = missingHostGroupPropsMap.get(category);
              if (missingHostGroupTypeProps == null) {
                missingHostGroupTypeProps = new HashSet<String>();
                missingHostGroupPropsMap.put(category, missingHostGroupTypeProps);
              }
              missingHostGroupTypeProps.add(name);
            }
          }
        }
      }
    }
    return missingProperties;
  }

  private boolean propertyExists(ClusterTopology topology, Map<String, Map<String, String>> props, String type, String property) {
    Map<String, String> typeProps = props.get(type);
    return (typeProps != null && typeProps.containsKey(property)) || setDefaultPassword(topology, type, property);
  }

  /**
   * Attempt to set the default password in cluster configuration for missing password property.
   *
   * @param configType       configuration type
   * @param property         password property name
   *
   * @return true if password was set, otherwise false.  Currently the password will always be set
   *         unless it is null
   */
  private boolean setDefaultPassword(ClusterTopology topology, String configType, String property) {
    boolean setDefaultPassword = false;
    if (defaultPassword != null && ! defaultPassword.trim().isEmpty()) {
      topology.getConfiguration().setProperty(configType, property, defaultPassword);
      setDefaultPassword = true;
    }
    return setDefaultPassword;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RequiredPasswordValidator that = (RequiredPasswordValidator) o;

    return defaultPassword == null ? that.defaultPassword == null : defaultPassword.equals(that.defaultPassword);
  }

  @Override
  public int hashCode() {
    return defaultPassword != null ? defaultPassword.hashCode() : 0;
  }
}