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

package org.apache.ambari.server.topology;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;
import static org.apache.ambari.server.controller.internal.BlueprintResourceProvider.BLUEPRINT_NAME_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.BlueprintResourceProvider.COMPONENT_MPACK_INSTANCE_PROPERTY;
import static org.apache.ambari.server.controller.internal.BlueprintResourceProvider.COMPONENT_NAME_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.BlueprintResourceProvider.COMPONENT_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.BlueprintResourceProvider.COMPONENT_PROVISION_ACTION_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.BlueprintResourceProvider.COMPONENT_SERVICE_INSTANCE_PROPERTY;
import static org.apache.ambari.server.controller.internal.BlueprintResourceProvider.CONFIGURATION_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.BlueprintResourceProvider.HOST_GROUP_CARDINALITY_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.BlueprintResourceProvider.HOST_GROUP_NAME_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.BlueprintResourceProvider.HOST_GROUP_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.BlueprintResourceProvider.MPACK_INSTANCES_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.BlueprintResourceProvider.SETTING_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.BlueprintResourceProvider.STACK_NAME_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.BlueprintResourceProvider.STACK_VERSION_PROPERTY_ID;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.controller.RootComponent;
import org.apache.ambari.server.controller.internal.ProvisionAction;
import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.controller.internal.StackDefinition;
import org.apache.ambari.server.orm.dao.BlueprintDAO;
import org.apache.ambari.server.orm.entities.BlueprintEntity;
import org.apache.ambari.server.stack.NoSuchStackException;
import org.apache.ambari.server.state.StackId;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.inject.Inject;

/**
 * Create a Blueprint instance.
 */
public class BlueprintFactory {

  private static final Logger LOG = LoggerFactory.getLogger(BlueprintFactory.class);

  private static BlueprintDAO blueprintDAO;

  private final ConfigurationFactory configFactory = new ConfigurationFactory();
  private final StackFactory stackFactory;

  public BlueprintFactory() {
    this(new DefaultStackFactory());
  }

  protected BlueprintFactory(StackFactory stackFactory) {
    this.stackFactory = stackFactory;
  }

  public Blueprint getBlueprint(String blueprintName) throws NoSuchStackException {
    BlueprintEntity entity = blueprintDAO.findByName(blueprintName);
    if (entity != null) {
      Set<StackId> stackIds = entity.getMpackInstances().stream()
        .map(m -> new StackId(m.getMpackName(), m.getMpackVersion()))
        .collect(toSet());
      StackDefinition stack = composeStacks(stackIds);
      return new BlueprintImpl(entity, stack, stackIds);
    }
    return null;
  }

  /**
   * Convert a map of properties to a blueprint entity.
   *
   * @param properties  property map
   * @param securityConfiguration security related properties
   * @return new blueprint entity
   */
  @SuppressWarnings("unchecked")
  public Blueprint createBlueprint(Map<String, Object> properties, SecurityConfiguration securityConfiguration) throws NoSuchStackException {
    String name = String.valueOf(properties.get(BLUEPRINT_NAME_PROPERTY_ID));
    // String.valueOf() will return "null" if value is null
    if (name.equals("null") || name.isEmpty()) {
      //todo: should throw a checked exception from here
      throw new IllegalArgumentException("Blueprint name must be provided");
    }

    Collection<MpackInstance> mpackInstances = createMpackInstances(properties);
    if (mpackInstances.isEmpty()) {
      StackId stackId = getStackId(properties);
      mpackInstances = Collections.singleton(new MpackInstance(stackId.getStackName(), stackId.getStackVersion(), null, null, Configuration.createEmpty()));
    }
    Set<StackId> stackIds = mpackInstances.stream()
        .map(MpackInstance::getStackId)
        .collect(toSet());
    StackDefinition stack = composeStacks(stackIds);
    Collection<HostGroup> hostGroups = processHostGroups(name, stack, properties);
    Configuration configuration = configFactory.getConfiguration((Collection<Map<String, String>>)
            properties.get(CONFIGURATION_PROPERTY_ID));
    Setting setting = SettingFactory.getSetting((Collection<Map<String, Object>>) properties.get(SETTING_PROPERTY_ID));

    return new BlueprintImpl(name, hostGroups, stack, stackIds, mpackInstances, configuration, securityConfiguration, setting);
  }

  public static Collection<MpackInstance> createMpackInstances(Map<String, Object> properties) throws NoSuchStackException {
    if (properties.containsKey(MPACK_INSTANCES_PROPERTY_ID)) {
      ObjectMapper mapper = new ObjectMapper();
      mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
      try {
        String mpackInstancesJson = mapper.writeValueAsString(properties.get(MPACK_INSTANCES_PROPERTY_ID));
        return mapper.readValue(mpackInstancesJson, new TypeReference<Collection<MpackInstance>>(){});
      } catch (IOException ex) {
        throw new RuntimeException("Unable to parse mpack instances for blueprint: " +
          String.valueOf(properties.get(BLUEPRINT_NAME_PROPERTY_ID)), ex);
      }
    } else {
      return Collections.emptyList();
    }
  }

  private static StackId getStackId(Map<String, Object> properties) throws NoSuchStackException {
    String stackName = String.valueOf(properties.get(STACK_NAME_PROPERTY_ID));
    String stackVersion = String.valueOf(properties.get(STACK_VERSION_PROPERTY_ID));
    return new StackId(stackName, stackVersion);
  }

  public StackDefinition composeStacks(Set<StackId> stackIds) {
    Set<Stack> stacks = stackIds.stream()
      .map(this::createStack)
      .collect(toSet());
    StackDefinition composite = StackDefinition.of(stacks);

    // temporary check
    verifyStackDefinitionsAreDisjoint(composite.getServices().stream(), "Service", composite::getStacksForService);
    verifyStackDefinitionsAreDisjoint(composite.getComponents().stream(), "Component", composite::getStacksForComponent);

    return composite;
  }

  /**
   * Verify that each item in <code>items</code> is defined by only one stack.
   *
   * @param items the items to check
   * @param type string description of the type of items (eg. "Service", or "Component")
   * @param lookup a function to find the set of stacks that an item belongs to
   * @throws IllegalArgumentException if some items are defined in multiple stacks
   */
  static void verifyStackDefinitionsAreDisjoint(Stream<String> items, String type, Function<String, Set<StackId>> lookup) {
    Set<Pair<String, Set<StackId>>> definedInMultipleStacks = items
      .map(s -> Pair.of(s, lookup.apply(s)))
      .filter(p -> p.getRight().size() > 1)
      .collect(toCollection(TreeSet::new));

    if (!definedInMultipleStacks.isEmpty()) {
      String msg = definedInMultipleStacks.stream()
        .map(p -> String.format("%s %s is defined in multiple stacks: %s", type, p.getLeft(), Joiner.on(", ").join(p.getRight())))
        .collect(joining("\n"));
      LOG.error(msg);
      throw new IllegalArgumentException(msg);
    }
  }

  protected Stack createStack(StackId stackId) {
    try {
      //todo: don't pass in controller
      return stackFactory.createStack(stackId);
    } catch (ObjectNotFoundException e) {
      throw new NoSuchStackException(stackId);
    }
  }

  //todo: Move logic to HostGroupImpl
  @SuppressWarnings("unchecked")
  private Collection<HostGroup> processHostGroups(String bpName, StackDefinition stack, Map<String, Object> properties) {
    Set<Map<String, Object>> hostGroupProps = (Set<Map<String, Object>>)
        properties.get(HOST_GROUP_PROPERTY_ID);

    if (hostGroupProps == null || hostGroupProps.isEmpty()) {
      throw new IllegalArgumentException("At least one host group must be specified in a blueprint");
    }

    Collection<HostGroup> hostGroups = new ArrayList<>();
    for (Map<String, Object> hostGroupProperties : hostGroupProps) {
      String hostGroupName = (String) hostGroupProperties.get(HOST_GROUP_NAME_PROPERTY_ID);
      if (hostGroupName == null || hostGroupName.isEmpty()) {
        throw new IllegalArgumentException("Every host group must include a non-null 'name' property");
      }

      Set<Map<String, String>> componentProps = (Set<Map<String, String>>)
          hostGroupProperties.get(COMPONENT_PROPERTY_ID);

      Collection<Map<String, String>> configProps = (Collection<Map<String, String>>)
          hostGroupProperties.get(CONFIGURATION_PROPERTY_ID);

      Collection<Component> components = processHostGroupComponents(stack, hostGroupName, componentProps);
      Configuration configuration = configFactory.getConfiguration(configProps);
      String cardinality = String.valueOf(hostGroupProperties.get(HOST_GROUP_CARDINALITY_PROPERTY_ID));

      HostGroup group = new HostGroupImpl(hostGroupName, bpName, stack, components, configuration, cardinality);

      hostGroups.add(group);
    }
    return hostGroups;
  }

  private Collection<Component> processHostGroupComponents(StackDefinition stack, String groupName, Set<Map<String, String>>  componentProps) {
    if (componentProps == null || componentProps.isEmpty()) {
      throw new IllegalArgumentException("Host group '" + groupName + "' must contain at least one component");
    }

    Collection<String> stackComponentNames = getAllStackComponents(stack);
    Collection<Component> components = new ArrayList<>();

    for (Map<String, String> componentProperties : componentProps) {
      String componentName = componentProperties.get(COMPONENT_NAME_PROPERTY_ID);
      if (componentName == null || componentName.isEmpty()) {
        throw new IllegalArgumentException("Host group '" + groupName +
            "' contains a component with no 'name' property");
      }

      if (! stackComponentNames.contains(componentName)) {
        throw new IllegalArgumentException("The component '" + componentName + "' in host group '" +
            groupName + "' is not valid for the specified stack");
      }

      String mpackInstance = componentProperties.get(COMPONENT_MPACK_INSTANCE_PROPERTY);
      String serviceInstance = componentProperties.get(COMPONENT_SERVICE_INSTANCE_PROPERTY);
      //TODO, might want to add some validation here, to only accept value enum types, rwn
      ProvisionAction provisionAction = componentProperties.containsKey(COMPONENT_PROVISION_ACTION_PROPERTY_ID) ?
        ProvisionAction.valueOf(componentProperties.get(COMPONENT_PROVISION_ACTION_PROPERTY_ID)) : null;
      components.add(new Component(componentName, mpackInstance, serviceInstance, provisionAction));
    }

    return components;
  }

  /**
   * Obtain all component names for the specified stack.
   *
   * @return collection of component names for the specified stack
   * @throws IllegalArgumentException if the specified stack doesn't exist
   */
  private Collection<String> getAllStackComponents(StackDefinition stack) {
    Collection<String> allComponents = new HashSet<>(stack.getComponents());

    // currently ambari server is not a recognized component
    allComponents.add(RootComponent.AMBARI_SERVER.name());

    return allComponents;
  }


  /**
   * Static initialization.
   *
   * @param dao  blueprint data access object
   */
  @Inject
  public static void init(BlueprintDAO dao) {
    blueprintDAO = dao;
  }

}
