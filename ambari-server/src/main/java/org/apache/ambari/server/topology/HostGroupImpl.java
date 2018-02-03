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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.internal.ProvisionAction;
import org.apache.ambari.server.controller.internal.StackDefinition;
import org.apache.ambari.server.orm.entities.HostGroupComponentEntity;
import org.apache.ambari.server.orm.entities.HostGroupConfigEntity;
import org.apache.ambari.server.orm.entities.HostGroupEntity;

import com.google.gson.Gson;

/**
 * Host Group implementation.
 */
public class HostGroupImpl implements HostGroup {

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
  private final Set<Component> components = new LinkedHashSet<>();

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

  private StackDefinition stack;

  private String cardinality = "NOT SPECIFIED";

  public HostGroupImpl(HostGroupEntity entity, String blueprintName, StackDefinition stack) {
    this.name = entity.getName();
    this.cardinality = entity.getCardinality();
    this.blueprintName = blueprintName;
    this.stack = stack;
    parseComponents(entity);
    parseConfigurations(entity);
  }

  public HostGroupImpl(String name, String bpName, StackDefinition stack, Collection<Component> components, Configuration configuration, String cardinality) {
    this.name = name;
    this.blueprintName = bpName;
    this.stack = stack;

    // process each component
    for (Component component: components) {
      addComponent(component);
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
   * Adds a component to the host group.
   * @param component the component to add
   */
  @Override
  public boolean addComponent(Component component) {
    if (components.add(component)) {
      containsMasterComponent |= stack.isMasterComponent(component.getName());

      String service = stack.getServiceForComponent(component.getName());
      if (service != null) {
        componentsForService
          .computeIfAbsent(service, __ -> new HashSet<>())
          .add(component);
      }

      return true;
    }

    return false;
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
   * Get the names of components for the specified service which are associated with the host group.
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
  public StackDefinition getStack() {
    return stack;
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
