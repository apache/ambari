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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.ambari.server.controller.internal.ProvisionAction;
import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.orm.entities.HostGroupComponentEntity;
import org.apache.ambari.server.orm.entities.HostGroupConfigEntity;
import org.apache.ambari.server.orm.entities.HostGroupEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.gson.Gson;

/**
 * Host Group implementation.
 */
public class HostGroupImpl implements HostGroup {

  private static final Logger LOG = LoggerFactory.getLogger(HostGroupImpl.class);

  /**
   * host group name
   */
  private String name;

  /**
   * blueprint name
   */
  private String blueprintName;

  /**
   * components contained in the host group
   */
  private final List<Component> components = new ArrayList<>();

  /**
   * map of service to components for the host group
   */
  // TODO: in blueprint 3.0 this should be per service instance
  private Map<String, Set<Component>> componentsForService = new HashMap<>();

  /**
   * configuration
   */
  private Configuration configuration = null;

  private boolean containsMasterComponent = false;

  private Map<String, Stack> stackMap;

  private String cardinality = "NOT SPECIFIED";

  public HostGroupImpl(HostGroupEntity entity, String blueprintName, Collection<Stack> stacks) {
    this.name = entity.getName();
    this.cardinality = entity.getCardinality();
    this.blueprintName = blueprintName;
    this.stackMap = stacks.stream().collect(Collectors.toMap(stack -> stack.getName() + "-" + stack.getVersion(), stack -> stack));
    parseComponents(entity);
    parseConfigurations(entity);
  }

  public HostGroupImpl(String name, String bpName, Collection<Stack> stacks, Collection<Component> components, Configuration configuration, String cardinality) {
    this.name = name;
    this.blueprintName = bpName;
    this.stackMap = stacks.stream().collect(Collectors.toMap(stack -> stack.getName() + "-" + stack.getVersion(), stack -> stack));

    // process each component
    for (Component component: components) {
      if (!addComponent(component)) {
        throw new IllegalArgumentException("Ambiguous component or can't determine stack for: " + component);
      }
    }

    this.configuration = configuration;
    if (cardinality != null && ! cardinality.equals("null")) {
      this.cardinality = cardinality;
    }
  }


  @Override
  public String getName() {
    return name;
  }

  //todo: currently not qualifying host group name
  @Override
  public String getFullyQualifiedName() {
    return String.format("%s:%s", blueprintName, getName());
  }

  //todo: currently not qualifying host group name
  public static String formatAbsoluteName(String bpName, String hgName) {
    return String.format("%s:%s", bpName, hgName);
  }

  @Override
  public Collection<Component> getComponents() {
    return components;
  }

  @Override
  @Deprecated
  public Collection<String> getComponentNames() {
    return components.stream().map(Component::getName).collect(toList());
  }

  @Override
  @Deprecated
  public Collection<String> getComponentNames(ProvisionAction provisionAction) {
    Set<String> setOfComponentNames = new HashSet<>();
    for (Component component : components) {
      if ( (component.getProvisionAction() != null) && (component.getProvisionAction() == provisionAction) ) {
        setOfComponentNames.add(component.getName());
      }
    }
    return setOfComponentNames;
  }

  /**
   * Get the services which are deployed to this host group.
   *
   * @return collection of services which have components in this host group
   */
  @Override
  public Collection<String> getServices() {
    return componentsForService.keySet();
  }

  /**
   * Adds a component to the host group. The component is successfully added if it is not a duplicate or ambiguous (as of
   * Ambari 3.1 multiple components of the same type can exist in a hostgroup. However, they have to come from different
   * management packs or belong to different service instances)
   * @param component the component to add
   * @return a boolean to indicate if addition was successful
   */
  public boolean addComponent(Component component) {
    // Exclude ambiguous component definitions
    boolean ambigous = components.stream().filter(c -> {
      if (c.getName().equals(component.getName())) { // found another component with the same name
        if (c.getMpackInstance() == null || component.getMpackInstance() == null) {
          return true; // if either of them has no mpack instance defined it is ambiguous
        }
        if (c.getMpackInstance().equals(component.getMpackInstance())) {
          // both components are in the same mpack, and one of them does not declare a service instance or
          // both declare the same service instance --> ambiguous
          return  c.getServiceInstance() != null && component.getServiceInstance() != null &&
            c.getServiceInstance().equals(component.getServiceInstance());
        }
        else {
          return false; // different mpacks --> no ambiguity
        }
      }
      else {
        return false; // different name --> no ambiguity
      }
    }).findAny().isPresent();
    if (ambigous) {
      return false;
    }
    addComponent(component, getStackForComponent(component));
    return true;
  }

  private Optional<Stack> getStackForComponent(Component component) {
    // Look for the stack of this component
    if (component.getMpackInstance() == null) {
      // Component does not declare its stack. Let's find it.
      Collection<Stack> candidateStacks =
        stackMap.values().stream().filter(stack -> stack.getServiceForComponent(component.getName()) != null).collect(toList());
      switch (candidateStacks.size()) {
        case 0:
          // no stack (no service) for this component
          LOG.warn("No stack/service found for component: {}", component);
          return Optional.empty();
        case 1:
          return Optional.of(candidateStacks.iterator().next());
        default:
          LOG.warn("Ambiguous stack resolution for component: {}, stacks: {}", component, candidateStacks);
          return Optional.empty();
      }
    }
    else {
      // TODO: refine this logic
      Stack stack = stackMap.get(component.getMpackInstance());
      if (null == stack) {
        LOG.warn("Component declared an invalid stack: {}", component);
      }
      return Optional.ofNullable(stack);
    }
  }

  private void addComponent(Component component, Optional<Stack> stack) {
    if (stack.isPresent()) {
      String serviceName = stack.get().getServiceForComponent(component.getName());
      if (!componentsForService.containsKey(serviceName)) {
        componentsForService.put(serviceName, Sets.newHashSet(component));
      }
      else {
        componentsForService.get(serviceName).add(component);
      }
      if (stack.get().isMasterComponent(component.getName())) {
        containsMasterComponent = true;
      }
    }
    components.add(component);
  }

  /**
   * Get the components for the specified service which are associated with the host group.
   *
   * @param service  service name
   *
   * @return set of components
   */
  @Override
  public Collection<Component> getComponents(String service) {
    return componentsForService.containsKey(service) ?
      new HashSet<>(componentsForService.get(service)) :
        Collections.emptySet();
  }

  /**
   * Get the names components for the specified service which are associated with the host group.
   *
   * @param service  service name
   *
   * @return set of component names
   */
  @Override
  @Deprecated
  public Collection<String> getComponentNames(String service) {
    return componentsForService.containsKey(service) ?
      componentsForService.get(service).stream().map(Component::getName).collect(toSet()) :
        Collections.emptySet();
  }


  /**
   * Get this host groups configuration.
   *
   * @return configuration instance
   */
  @Override
  public Configuration getConfiguration() {
    return configuration;
  }

  /**
   * Get the associated blueprint name.
   *
   * @return  associated blueprint name
   */
  @Override
  public String getBlueprintName() {
    return blueprintName;
  }

  @Override
  public boolean containsMasterComponent() {
    return containsMasterComponent;
  }

  @Override
  public Collection<Stack> getStacks() {
    return stackMap.values();
  }

  @Override
  @Deprecated
  public Stack getStack() {
    return getStacks().iterator().next();
  }

  @Override
  public String getCardinality() {
    return cardinality;
  }

  /**
   * Parse component information.
   */
  private void parseComponents(HostGroupEntity entity) {
    for (HostGroupComponentEntity componentEntity : entity.getComponents() ) {
      Component component = new Component(
        componentEntity.getName(),
        componentEntity.getMpackName(),
        componentEntity.getServiceName(),
        null == componentEntity.getProvisionAction() ? null : ProvisionAction.valueOf(componentEntity.getProvisionAction()));
      addComponent(component);
    }
  }

  /**
   * Parse host group configurations.
   */
  //todo: use ConfigurationFactory
  private void parseConfigurations(HostGroupEntity entity) {
    Map<String, Map<String, String>> config = new HashMap<>();
    Gson jsonSerializer = new Gson();
    for (HostGroupConfigEntity configEntity : entity.getConfigurations()) {
      String type = configEntity.getType();
      Map<String, String> typeProperties = config.get(type);
      if (typeProperties == null) {
        typeProperties = new HashMap<>();
        config.put(type, typeProperties);
      }
      Map<String, String> propertyMap =  jsonSerializer.<Map<String, String>>fromJson(
          configEntity.getConfigData(), Map.class);
      if (propertyMap != null) {
        typeProperties.putAll(propertyMap);
      }
    }
    //todo: parse attributes
    Map<String, Map<String, Map<String, String>>> attributes = new HashMap<>();
    configuration = new Configuration(config, attributes);
  }

  public String toString(){
       return  name;
  }
}
