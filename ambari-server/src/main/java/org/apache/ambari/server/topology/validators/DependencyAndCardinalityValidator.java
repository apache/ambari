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

package org.apache.ambari.server.topology.validators;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.ambari.server.controller.internal.BlueprintConfigurationProcessor;
import org.apache.ambari.server.controller.internal.StackDefinition;
import org.apache.ambari.server.state.AutoDeployInfo;
import org.apache.ambari.server.state.DependencyConditionInfo;
import org.apache.ambari.server.state.DependencyInfo;
import org.apache.ambari.server.topology.Blueprint;
import org.apache.ambari.server.topology.Cardinality;
import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.Component;
import org.apache.ambari.server.topology.HostGroup;
import org.apache.ambari.server.topology.InvalidTopologyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Verifies that service dependencies and component cardinality requirements are satisfied.
 */
public class DependencyAndCardinalityValidator implements TopologyValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(DependencyAndCardinalityValidator.class);

  @Override
  public void validate(ClusterTopology topology) throws InvalidTopologyException {
    Blueprint blueprint = topology.getBlueprint();
    LOGGER.info("Validating topology for blueprint: [{}]", topology.getBlueprintName());

    StackDefinition stack = topology.getStack();
    Collection<HostGroup> hostGroups = blueprint.getHostGroups().values();
    Map<String, Map<String, Collection<DependencyInfo>>> missingDependencies = new HashMap<>();

    for (HostGroup group : hostGroups) {
      Map<String, Collection<DependencyInfo>> missingGroupDependencies = validateHostGroup(topology, blueprint, stack, group);
      if (!missingGroupDependencies.isEmpty()) {
        missingDependencies.put(group.getName(), missingGroupDependencies);
      }
    }

    Collection<String> cardinalityFailures = new HashSet<>();
    Collection<String> services = topology.getServices();

    for (String service : services) {
      for (String component : stack.getComponents(service)) {
        Cardinality cardinality = stack.getCardinality(component);
        AutoDeployInfo autoDeploy = stack.getAutoDeployInfo(component);
        if (cardinality.isAll()) {
          cardinalityFailures.addAll(verifyComponentInAllHostGroups(blueprint, new Component(component), autoDeploy));
        } else {
          cardinalityFailures.addAll(verifyComponentCardinalityCount(
            stack, topology, blueprint, new Component(component), cardinality, autoDeploy));
        }
      }
    }

    if (!missingDependencies.isEmpty() || !cardinalityFailures.isEmpty()) {
      generateInvalidTopologyException(missingDependencies, cardinalityFailures);
    }
  }

  /**
   * Verify that a component is included in all host groups.
   * For components that are auto-install enabled, will add component to topology if needed.
   *
   *
   * @param blueprint   blueprint to validate
   * @param component   component to validate
   * @param autoDeploy  auto-deploy information for component
   *
   * @return collection of missing component information
   */
  private Collection<String> verifyComponentInAllHostGroups(Blueprint blueprint, Component component, AutoDeployInfo autoDeploy) {
    Collection<String> cardinalityFailures = new HashSet<>();
    int actualCount = blueprint.getHostGroupsForComponent(component.getName()).size();
    Map<String, HostGroup> hostGroups = blueprint.getHostGroups();
    if (actualCount != hostGroups.size()) {
      if (autoDeploy != null && autoDeploy.isEnabled()) {
        for (HostGroup group : hostGroups.values()) {
          group.addComponent(component);
        }
      } else {
        cardinalityFailures.add(component + "(actual=" + actualCount + ", required=ALL)");
      }
    }
    return cardinalityFailures;
  }

  private Map<String, Collection<DependencyInfo>> validateHostGroup(ClusterTopology topology, Blueprint blueprint, StackDefinition stack, HostGroup group) {
    LOGGER.info("Validating hostgroup: {}", group.getName());
    Map<String, Collection<DependencyInfo>> missingDependencies = new HashMap<>();

    for (Component component : new HashSet<>(group.getComponents())) {
      LOGGER.debug("Processing component: {}", component);

      for (DependencyInfo dependency : stack.getDependenciesForComponent(component.getName())) {
        LOGGER.debug("Processing dependency [{}] for component [{}]", dependency.getName(), component);

        // dependent components from the stack definitions are only added if related services are explicitly added to the blueprint!
        boolean isClientDependency = stack.getComponentInfo(dependency.getComponentName()).isClient();
        if (isClientDependency && !topology.getServices().contains(dependency.getServiceName())) {
          LOGGER.debug("The service [{}] for component [{}] is missing from the blueprint [{}], skipping dependency",
              dependency.getServiceName(), dependency.getComponentName(), topology.getBlueprintName());
          continue;
        }

        String         dependencyScope = dependency.getScope();
        String         componentName   = dependency.getComponentName();
        AutoDeployInfo autoDeployInfo  = dependency.getAutoDeploy();
        boolean        resolved        = false;

        //check if conditions are met, if any
        if(dependency.hasDependencyConditions()) {
          boolean conditionsSatisfied = true;
          for (DependencyConditionInfo dependencyCondition : dependency.getDependencyConditions()) {
            if (!dependencyCondition.isResolved(topology.getConfiguration().getFullProperties())) {
              conditionsSatisfied = false;
              break;
            }
          }
          if(!conditionsSatisfied){
            continue;
          }
        }
        if (dependencyScope.equals("cluster")) {
          Collection<String> missingDependencyInfo = verifyComponentCardinalityCount(
            stack, topology, blueprint, new Component(componentName), new Cardinality("1+"), autoDeployInfo);

          resolved = missingDependencyInfo.isEmpty();
        } else if (dependencyScope.equals("host")) {
          if (group.getComponentNames().contains(componentName) || (autoDeployInfo != null && autoDeployInfo.isEnabled())) {
            resolved = true;
            group.addComponent(new Component(componentName));
          }
        }

        if (! resolved) {
          Collection<DependencyInfo> missingCompDependencies = missingDependencies.get(component.getName());
          if (missingCompDependencies == null) {
            missingCompDependencies = new HashSet<>();
            missingDependencies.put(component.getName(), missingCompDependencies);
          }
          missingCompDependencies.add(dependency);
        }
      }
    }
    return missingDependencies;
  }

  /**
   * Verify that a component meets cardinality requirements.  For components that are
   * auto-install enabled, will add component to topology if needed.
   *
   *
   * @param stack        stack definition
   * @param blueprint    blueprint to validate
   * @param component    component to validate
   * @param cardinality  required cardinality
   * @param autoDeploy   auto-deploy information for component
   *
   * @return collection of missing component information
   */
  private Collection<String> verifyComponentCardinalityCount(
    StackDefinition stack,
    ClusterTopology topology,
    Blueprint blueprint,
    Component component,
    Cardinality cardinality,
    AutoDeployInfo autoDeploy
  ) {
    Map<String, Map<String, String>> configProperties = topology.getConfiguration().getProperties();
    Collection<String> cardinalityFailures = new HashSet<>();
    //todo: don't hard code this HA logic here
    if (BlueprintConfigurationProcessor.isNameNodeHAEnabled(configProperties) &&
        (component.getName().equals("SECONDARY_NAMENODE"))) {
      // override the cardinality for this component in an HA deployment,
      // since the SECONDARY_NAMENODE should not be started in this scenario
      cardinality = new Cardinality("0");
    }

    int actualCount = blueprint.getHostGroupsForComponent(component.getName()).size();
    if (! cardinality.isValidCount(actualCount)) {
      boolean validated = ! isDependencyManaged(stack, component.getName(), configProperties);
      if (! validated && autoDeploy != null && autoDeploy.isEnabled() && cardinality.supportsAutoDeploy()) {
        String coLocateName = autoDeploy.getCoLocate();
        if (coLocateName != null && ! coLocateName.isEmpty()) {
          Collection<HostGroup> coLocateHostGroups = blueprint.getHostGroupsForComponent(coLocateName.split("/")[1]);
          if (! coLocateHostGroups.isEmpty()) {
            validated = true;
            HostGroup group = coLocateHostGroups.iterator().next();
            group.addComponent(component);

          }
        }
      }
      if (! validated) {
        cardinalityFailures.add(component + "(actual=" + actualCount + ", required=" +
            cardinality.getValue() + ")");
      }
    }
    return cardinalityFailures;
  }

  /**
   * Determine if a component is managed, meaning that it is running inside of the cluster
   * topology.  Generally, non-managed dependencies will be database components.
   *
   * @param stack          stack definition
   * @param component      component to determine if it is managed
   * @param clusterConfig  cluster configuration
   *
   * @return true if the specified component managed by the cluster; false otherwise
   */
  protected boolean isDependencyManaged(StackDefinition stack, String component, Map<String, Map<String, String>> clusterConfig) {
    boolean isManaged = true;
    String externalComponentConfig = stack.getExternalComponentConfig(component);
    if (externalComponentConfig != null) {
      String[] toks = externalComponentConfig.split("/");
      String externalComponentConfigType = toks[0];
      String externalComponentConfigProp = toks[1];
      Map<String, String> properties = clusterConfig.get(externalComponentConfigType);
      if (properties != null && properties.containsKey(externalComponentConfigProp)) {
        if (properties.get(externalComponentConfigProp).startsWith("Existing")) {
          isManaged = false;
        }
      }
    }
    return isManaged;
  }

  /**
   * Generate an exception for topology validation failure.
   *
   * @param missingDependencies  missing dependency information
   * @param cardinalityFailures  missing service component information
   *
   * @throws IllegalArgumentException  Always thrown and contains information regarding the topology validation failure
   *                                   in the msg
   */
  private void generateInvalidTopologyException(Map<String, Map<String, Collection<DependencyInfo>>> missingDependencies,
                                                Collection<String> cardinalityFailures) throws InvalidTopologyException {

    //todo: encapsulate some of this in exception?
    String msg = "Cluster Topology validation failed.";
    if (! cardinalityFailures.isEmpty()) {
      msg += "  Invalid service component count: " + cardinalityFailures;
    }
    if (! missingDependencies.isEmpty()) {
      msg += "  Unresolved component dependencies: " + missingDependencies;
    }
    msg += ".  To disable topology validation and create the blueprint, " +
        "add the following to the end of the url: '?validate_topology=false'";
    throw new InvalidTopologyException(msg);
  }
}
