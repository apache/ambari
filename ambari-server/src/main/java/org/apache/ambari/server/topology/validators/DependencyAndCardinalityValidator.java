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

import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.ambari.server.controller.internal.BlueprintConfigurationProcessor;
import org.apache.ambari.server.controller.internal.StackDefinition;
import org.apache.ambari.server.state.AutoDeployInfo;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.DependencyConditionInfo;
import org.apache.ambari.server.state.DependencyInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.topology.AmbiguousComponentException;
import org.apache.ambari.server.topology.Cardinality;
import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.Component;
import org.apache.ambari.server.topology.InvalidTopologyException;
import org.apache.ambari.server.topology.ResolvedComponent;
import org.apache.ambari.server.topology.StackComponentResolver;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Verifies that service dependencies and component cardinality requirements are satisfied.
 */
public class DependencyAndCardinalityValidator implements TopologyValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(DependencyAndCardinalityValidator.class);

  @Override
  public ClusterTopology validate(ClusterTopology topology) throws InvalidTopologyException {
    LOGGER.info("Validating topology for blueprint: [{}]", topology.getBlueprintName());

    StackDefinition stack = topology.getStack();
    Map<String, Set<ResolvedComponent>> autoAddedComponents = new TreeMap<>();
    Map<String, Map<Component, Collection<DependencyInfo>>> missingDependencies = new HashMap<>();

    for (String group : topology.getHostGroups()) {
      Map<Component, Collection<DependencyInfo>> missingGroupDependencies = validateHostGroup(topology, autoAddedComponents, group);
      if (!missingGroupDependencies.isEmpty()) {
        missingDependencies.put(group, missingGroupDependencies);
      }
    }

    Collection<String> cardinalityFailures = new HashSet<>();
    Set<Pair<StackId, ServiceInfo>> services = topology.getComponents()
      .map(each -> Pair.of(each.stackId(), each.serviceInfo()))
      .collect(Collectors.toSet());

    for (Pair<StackId, ServiceInfo> pair : services) {
      StackId stackId = pair.getLeft();
      ServiceInfo serviceInfo = pair.getRight();
      for (ComponentInfo componentInfo : serviceInfo.getComponents()) {
        String component = componentInfo.getName();
        ResolvedComponent resolved = ResolvedComponent.builder(new Component(component))
          .stackId(stackId)
          .serviceInfo(serviceInfo)
          .componentInfo(componentInfo)
          .build();

        Cardinality cardinality = stack.getCardinality(component);
        AutoDeployInfo autoDeploy = stack.getAutoDeployInfo(component);
        if (cardinality.isAll()) {
          cardinalityFailures.addAll(verifyComponentInAllHostGroups(topology, resolved, autoDeploy, autoAddedComponents));
        } else {
          cardinalityFailures.addAll(verifyComponentCardinalityCount(topology, autoAddedComponents, resolved, cardinality, autoDeploy));
        }
      }
    }

    if (!missingDependencies.isEmpty() || !cardinalityFailures.isEmpty()) {
      generateInvalidTopologyException(missingDependencies, cardinalityFailures);
    }

    return topology.withAdditionalComponents(autoAddedComponents);
  }

  /**
   * Verify that a component is included in all host groups.
   * For components that are auto-install enabled, will add component to topology if needed.
   *
   *
   * @param component   component to validate
   * @param autoDeploy  auto-deploy information for component
   *
   * @return collection of missing component information
   */
  private Collection<String> verifyComponentInAllHostGroups(ClusterTopology topology, ResolvedComponent component, AutoDeployInfo autoDeploy, Map<String, Set<ResolvedComponent>> autoAddedComponents) {
    Collection<String> cardinalityFailures = new HashSet<>();
    Set<String> hostGroupsForComponent = topology.getHostGroupsForComponent(component);
    Set<String> allHostGroups = topology.getHostGroups();
    if (hostGroupsForComponent.size() != allHostGroups.size()) {
      if (autoDeploy != null && autoDeploy.isEnabled()) {
        for (String group : topology.getHostGroups()) {
          if (!hostGroupsForComponent.contains(group)) {
            autoAddedComponents.computeIfAbsent(group, __ -> new LinkedHashSet<>())
              .add(component);
          }
        }
      } else {
        cardinalityFailures.add(formatCardinalityFailure(component, hostGroupsForComponent.size(), Cardinality.ALL));
      }
    }
    return cardinalityFailures;
  }

  private Map<Component, Collection<DependencyInfo>> validateHostGroup(ClusterTopology topology, Map<String, Set<ResolvedComponent>> autoAddedComponents, String groupName) {
    LOGGER.info("Validating host group: {}", groupName);
    StackDefinition stack = topology.getStack();
    Map<Component, Collection<DependencyInfo>> missingDependencies = new HashMap<>();

    Set<ResolvedComponent> componentsInHostGroup = topology.getComponentsInHostGroup(groupName).collect(toSet());
    for (ResolvedComponent component : componentsInHostGroup) {
      LOGGER.debug("Processing component: {}", component);

      for (DependencyInfo dependencyInfo : stack.getDependenciesForComponent(component.componentName())) {
        LOGGER.debug("Processing dependency [{}] for component [{}]", dependencyInfo.getName(), component);

        // dependent components from the stack definitions are only added if related services are explicitly added to the blueprint!
        ResolvedComponent dependency;
        try {
          dependency = resolveComponent(stack, component.stackId(), dependencyInfo.getServiceName(), dependencyInfo.getComponentName());
        } catch (AmbiguousComponentException e) {
          LOGGER.info("Could not resolve depended component {} due to {}", dependencyInfo, e.getMessage());
          continue;
        }

        String         dependencyScope = dependencyInfo.getScope();
        AutoDeployInfo autoDeployInfo  = dependencyInfo.getAutoDeploy();
        boolean        resolved        = false;

        //check if conditions are met, if any
        if (dependencyInfo.hasDependencyConditions()) {
          boolean conditionsSatisfied = true;
          for (DependencyConditionInfo dependencyCondition : dependencyInfo.getDependencyConditions()) {
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
            topology, autoAddedComponents, dependency, new Cardinality("1+"), autoDeployInfo);

          resolved = missingDependencyInfo.isEmpty();
        } else if (dependencyScope.equals("host")) {
          if (componentsInHostGroup.contains(dependency)) {
            resolved = true;
            LOGGER.debug("Host group {} contains component {} and satisfies host-level dependency for component {}", groupName, dependency, component);
          } else if (autoDeployInfo != null && autoDeployInfo.isEnabled()) {
            resolved = true;
            autoAddedComponents.computeIfAbsent(groupName, __ -> new LinkedHashSet<>())
              .add(dependency);
            LOGGER.info("Added component {} in host group {} to satisfy host-level dependency for component {}", dependency, groupName, component);
          }
        }

        if (! resolved) {
          missingDependencies.computeIfAbsent(component.component(), __ -> new HashSet<>())
            .add(dependencyInfo);
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
   * @param component    component to validate
   * @param cardinality  required cardinality
   * @param autoDeploy   auto-deploy information for component
   *
   * @return collection of missing component information
   */
  private Collection<String> verifyComponentCardinalityCount(
    ClusterTopology topology,
    Map<String, Set<ResolvedComponent>> autoAddedComponents,
    ResolvedComponent component,
    Cardinality cardinality,
    AutoDeployInfo autoDeploy
  ) {
    Map<String, Map<String, String>> configProperties = topology.getConfiguration().getProperties();
    Collection<String> cardinalityFailures = new HashSet<>();
    //todo: don't hard code this HA logic here
    if (BlueprintConfigurationProcessor.isNameNodeHAEnabled(configProperties) &&
        (component.componentName().equals("SECONDARY_NAMENODE"))) {
      // override the cardinality for this component in an HA deployment,
      // since the SECONDARY_NAMENODE should not be started in this scenario
      cardinality = new Cardinality("0");
    }

    int actualCount = topology.getHostGroupsForComponent(component).size();
    LOGGER.debug("Host groups for {}: {}", component, actualCount);
    if (! cardinality.isValidCount(actualCount)) {
      StackDefinition stack = topology.getStack();
      boolean validated = ! isDependencyManaged(stack, component.componentName(), configProperties);
      if (! validated && autoDeploy != null && autoDeploy.isEnabled() && cardinality.supportsAutoDeploy()) {
        String coLocateName = autoDeploy.getCoLocate();
        if (coLocateName != null && ! coLocateName.isEmpty()) {
          String[] coLocateNameParts = coLocateName.split("/");
          if (coLocateNameParts.length == 2) {
            ResolvedComponent coLocateWith = resolveComponent(stack, coLocateNameParts[0], coLocateNameParts[1]);
            Collection<String> coLocateHostGroups = topology.getHostGroupsForComponent(coLocateWith);
            if (!coLocateHostGroups.isEmpty()) {
              validated = true;
              String group = coLocateHostGroups.iterator().next();
              autoAddedComponents.computeIfAbsent(group, __ -> new LinkedHashSet<>())
                .add(component);
            }
          }
        }
      }
      if (! validated) {
        cardinalityFailures.add(formatCardinalityFailure(component, actualCount, cardinality));
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
  protected static boolean isDependencyManaged(StackDefinition stack, String component, Map<String, Map<String, String>> clusterConfig) {
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
  private void generateInvalidTopologyException(Map<String, ?> missingDependencies,
                                                Collection<String> cardinalityFailures) throws InvalidTopologyException {

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

  private ResolvedComponent resolveComponent(StackDefinition stack, StackId stackId, String service, String component) throws AmbiguousComponentException {
    Optional<ServiceInfo> serviceInfo = stack.getService(stackId, service);
    if (serviceInfo.isPresent()) {
      ComponentInfo componentInfo = serviceInfo.get().getComponentByName(component);
      if (componentInfo != null) {
        return ResolvedComponent.builder(new Component(component))
          .stackId(stackId)
          .serviceInfo(serviceInfo.get())
          .componentInfo(componentInfo)
          .build();
      }
    }

    return resolveComponent(stack, service, component);
  }

  private ResolvedComponent resolveComponent(StackDefinition stack, String service, String component) throws AmbiguousComponentException {
    // dependency does not specify the stack
    Stream<Pair<StackId, ServiceInfo>> services = stack.getServicesForComponent(component)
      .filter(each -> Objects.equals(each.getRight().getName(), service));

    return StackComponentResolver.getComponent(new Component(component), services);
  }

  private static String formatCardinalityFailure(ResolvedComponent component, int actual, Cardinality expected) {
    return String.format("%s (actual=%s, expected=%s)", component.component(), actual, expected);
  }

}
