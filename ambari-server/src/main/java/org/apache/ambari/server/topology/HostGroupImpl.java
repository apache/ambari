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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.internal.ProvisionAction;
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
  private final String name;

  /**
   * components contained in the host group
   */
  private final Set<Component> components = new LinkedHashSet<>();

  /**
   * configuration
   */
  private final Configuration configuration;
  private final String cardinality;

  public HostGroupImpl(HostGroupEntity entity) {
    this.name = entity.getName();
    this.cardinality = entity.getCardinality();
    configuration = parseConfigurations(entity);
    parseComponents(entity);
  }

  public HostGroupImpl(String name, Collection<Component> components, Configuration configuration, String cardinality) {
    this.name = name;
    this.configuration = configuration;
    this.cardinality = (cardinality != null && !"null".equals(cardinality)) ? cardinality : "NOT SPECIFIED";

    // process each component
    for (Component component: components) {
      addComponent(component);
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
   * Adds a component to the host group.
   * @param component the component to add
   */
  @Override
  public boolean addComponent(Component component) {
    return components.add(component);
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
  private Configuration parseConfigurations(HostGroupEntity entity) {
    Map<String, Map<String, String>> config = new HashMap<>();
    Gson jsonSerializer = new Gson();
    for (HostGroupConfigEntity configEntity : entity.getConfigurations()) {
      String type = configEntity.getType();
      Map<String, String> typeProperties = config.computeIfAbsent(type, k -> new HashMap<>());
      Map<String, String> propertyMap =  jsonSerializer.<Map<String, String>>fromJson(
          configEntity.getConfigData(), Map.class);
      if (propertyMap != null) {
        typeProperties.putAll(propertyMap);
      }
    }
    //todo: parse attributes
    Map<String, Map<String, Map<String, String>>> attributes = new HashMap<>();
    return new Configuration(config, attributes);
  }

  @Override
  public String toString(){
       return  name;
  }
}
