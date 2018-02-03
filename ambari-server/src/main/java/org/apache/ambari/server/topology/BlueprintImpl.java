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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.ambari.server.controller.internal.StackDefinition;
import org.apache.ambari.server.orm.entities.BlueprintConfigEntity;
import org.apache.ambari.server.orm.entities.BlueprintConfiguration;
import org.apache.ambari.server.orm.entities.BlueprintEntity;
import org.apache.ambari.server.orm.entities.BlueprintMpackInstanceEntity;
import org.apache.ambari.server.orm.entities.BlueprintServiceEntity;
import org.apache.ambari.server.orm.entities.BlueprintSettingEntity;
import org.apache.ambari.server.orm.entities.HostGroupComponentEntity;
import org.apache.ambari.server.orm.entities.HostGroupConfigEntity;
import org.apache.ambari.server.orm.entities.HostGroupEntity;
import org.apache.ambari.server.stack.NoSuchStackException;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.utils.JsonUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;

/**
 * Blueprint implementation.
 */
public class BlueprintImpl implements Blueprint {

  private final String name;
  private final Map<String, HostGroup> hostGroups;
  private final Collection<MpackInstance> mpacks;
  private final StackDefinition stack;
  private final Set<StackId> stackIds;
  private final Configuration configuration;
  private final SecurityConfiguration security;
  private final Setting setting;
  private final List<RepositorySetting> repoSettings;

  public BlueprintImpl(BlueprintEntity entity, StackDefinition stack, Set<StackId> stackIds) throws NoSuchStackException {
    name = entity.getBlueprintName();
    security = entity.getSecurityType() != null
      ? new SecurityConfiguration(entity.getSecurityType(), entity.getSecurityDescriptorReference(), null)
      : SecurityConfiguration.NONE;
    mpacks = parseMpacks(entity);

    this.stack = stack;
    this.stackIds = stackIds;

    // create config first because it is set as a parent on all host-group configs
    configuration = processConfiguration(entity.getConfigurations());
    hostGroups = parseBlueprintHostGroups(entity);
    // TODO: how to handle multiple stacks correctly?
    configuration.setParentConfiguration(stack.getConfiguration(getServices()));
    setting = new Setting(parseSetting(entity.getSettings()));
    repoSettings = processRepoSettings();
  }

  public BlueprintImpl(String name, Collection<HostGroup> groups, StackDefinition stack, Set<StackId> stackIds, Collection<MpackInstance> mpacks,
      Configuration configuration, SecurityConfiguration security, Setting setting) {
    this.name = name;
    this.mpacks = mpacks;
    this.stack = stack;
    this.stackIds = stackIds;
    this.security = security != null ? security : SecurityConfiguration.NONE;

    // caller should set host group configs
    hostGroups = new HashMap<>();
    for (HostGroup hostGroup : groups) {
      hostGroups.put(hostGroup.getName(), hostGroup);
    }
    // TODO: handle configuration from multiple stacks properly
    // if the parent isn't set, the stack configuration is set as the parent
    this.configuration = configuration;
    if (configuration.getParentConfiguration() == null) {
      configuration.setParentConfiguration(stack.getConfiguration(getServices()));
    }
    this.setting = setting != null ? setting : new Setting(ImmutableMap.of());
    repoSettings = processRepoSettings();
  }

  public String getName() {
    return name;
  }

  public Set<StackId> getStackIds() {
    return stackIds;
  }

  @Override
  public Set<StackId> getStackIdsForService(String service) {
    return stack.getStacksForService(service);
  }

  public SecurityConfiguration getSecurity() {
    return security;
  }

  //todo: safe copy?
  @Override
  public Map<String, HostGroup> getHostGroups() {
    return hostGroups;
  }

  //todo: safe copy?
  @Override
  public HostGroup getHostGroup(String name) {
    return hostGroups.get(name);
  }

  @Override
  public Configuration getConfiguration() {
    return configuration;
  }

  @Override
  public Setting getSetting() {
    return setting;
  }

  /**
   * Get all services represented in blueprint.
   *
   * @return collections of all services provided by topology
   */
  @Override
  public Collection<String> getServices() {
    Collection<String> services = new HashSet<>();
    for (HostGroup group : getHostGroups().values()) {
      services.addAll(group.getServices());
    }
    return services;
  }

  @Override
  public Collection<Component> getComponents(String service) {
    Collection<Component> components = new HashSet<>();
    for (HostGroup group : getHostGroupsForService(service)) {
      components.addAll(group.getComponents(service));
    }
    return components;
  }

  @Override
  @Deprecated
  public Collection<String> getComponentNames(String service) {
    return getComponents(service).stream().map(Component::getName).collect(toList());
  }

  @Override
  public String getRecoveryEnabled(String serviceName, String componentName) {
    return setting.getRecoveryEnabled(serviceName, componentName);
  }

  @Override
  public String getCredentialStoreEnabled(String serviceName) {
    return setting.getCredentialStoreEnabled(serviceName);
  }

  @Override
  public boolean shouldSkipFailure() {
    return setting.shouldSkipFailure();
  }

  @Override
  public Collection<MpackInstance> getMpacks() {
    return mpacks;
  }

  public StackDefinition getStack() {
    return stack;
  }

  /**
   * Get host groups which contain a component.
   *
   * @param component component name
   *
   * @return collection of host groups which contain the specified component
   */
  @Override
  public Collection<HostGroup> getHostGroupsForComponent(String component) {
    Collection<HostGroup> resultGroups = new HashSet<>();
    for (HostGroup group : hostGroups.values() ) {
      if (group.getComponentNames().contains(component)) {
        resultGroups.add(group);
      }
    }
    return resultGroups;
  }

  /**
   * Get host groups which contain a component for the given service.
   *
   * @param service   service name
   *
   * @return collection of host groups which contain a component of the specified service
   */
  @Override
  public Collection<HostGroup> getHostGroupsForService(String service) {
    Collection<HostGroup> resultGroups = new HashSet<>();
    for (HostGroup group : hostGroups.values() ) {
      if (group.getServices().contains(service)) {
        resultGroups.add(group);
      }
    }
    return resultGroups;
  }

  public BlueprintEntity toEntity() {
    BlueprintEntity entity = new BlueprintEntity();
    entity.setBlueprintName(name);
    if (security.getType() != null) {
      entity.setSecurityType(security.getType());
    }
    if (security.getDescriptorReference() != null) {
      entity.setSecurityDescriptorReference(security.getDescriptorReference());
    }

    createHostGroupEntities(entity);
    Collection<BlueprintConfigEntity> configEntities = toConfigEntities(getConfiguration(), BlueprintConfigEntity::new);
    configEntities.forEach(configEntity -> {
      configEntity.setBlueprintEntity(entity);
      configEntity.setBlueprintName(getName());
    });
    entity.setConfigurations(configEntities);
    createBlueprintSettingEntities(entity);
    createMpackInstanceEntities(entity);
    return entity;
  }

  private void createMpackInstanceEntities(BlueprintEntity entity) {
    mpacks.forEach(mpack -> {
      BlueprintMpackInstanceEntity mpackEntity = mpack.toEntity();
      mpackEntity.setBlueprint(entity);
      entity.getMpackInstances().add(mpackEntity);
    });
  }

  private Collection<MpackInstance> parseMpacks(BlueprintEntity blueprintEntity) throws NoSuchStackException {
    Collection<MpackInstance> mpackInstances = new ArrayList<>();
    for (BlueprintMpackInstanceEntity mpack: blueprintEntity.getMpackInstances()) {
      MpackInstance mpackInstance = new MpackInstance();
      mpackInstance.setMpackName(mpack.getMpackName());
      mpackInstance.setMpackVersion(mpack.getMpackVersion());
      mpackInstance.setUrl(mpack.getMpackUri());
      mpackInstance.setConfiguration(processConfiguration(mpack.getConfigurations()));
      // TODO: come up with proper mpack -> stack resolution
      for(BlueprintServiceEntity serviceEntity: mpack.getServiceInstances()) {
        ServiceInstance serviceInstance = new ServiceInstance(
          serviceEntity.getName(),
          serviceEntity.getType(),
          processConfiguration(serviceEntity.getConfigurations()));
        mpackInstance.addServiceInstance(serviceInstance);
      }
      mpackInstances.add(mpackInstance);
    }
    return mpackInstances;
  }

  private Map<String, HostGroup> parseBlueprintHostGroups(BlueprintEntity entity) {
    Map<String, HostGroup> hostGroups = new HashMap<>();
    for (HostGroupEntity hostGroupEntity : entity.getHostGroups()) {
      HostGroupImpl hostGroup = new HostGroupImpl(hostGroupEntity, getName(), getStack());
      // set the bp configuration as the host group config parent
      hostGroup.getConfiguration().setParentConfiguration(configuration);
      hostGroups.put(hostGroupEntity.getName(), hostGroup);
    }
    return hostGroups;
  }

  /**
   * Process blueprint configurations.
   */
  private Configuration processConfiguration(Collection<? extends BlueprintConfiguration> configs) {
    // not setting stack configuration as parent until after host groups are parsed in constructor
    return new Configuration(parseConfigurations(configs),
        parseAttributes(configs), null);
  }

  /**
   * Obtain configuration as a map of config type to corresponding properties.
   *
   * @return map of config type to map of properties
   */
  private Map<String, Map<String, String>> parseConfigurations(Collection<? extends BlueprintConfiguration> configs) {
    Map<String, Map<String, String>> properties = new HashMap<>();
    Gson gson = new Gson();
    for (BlueprintConfiguration config : configs) {
      String type = config.getType();
      Map<String, String> typeProperties = gson.<Map<String, String>>fromJson(
              config.getConfigData(), Map.class);
      properties.put(type, typeProperties);
    }
    return properties;
  }

  /**
   * Deserialization: Obtain setting as a map of setting name to corresponding properties.
   *
   * @return map of setting name to map of properties
   */
  private static Map<String, Set<Map<String, String>>> parseSetting(Collection<? extends BlueprintSettingEntity> blueprintSetting) {
    if (blueprintSetting == null) {
      return ImmutableMap.of();
    }

    Map<String, Set<Map<String, String>>> properties = new HashMap<>();
    Gson gson = new Gson();
    for (BlueprintSettingEntity setting : blueprintSetting) {
      String settingName = setting.getSettingName();
      Set<Map<String, String>> settingProperties = gson.<Set<Map<String, String>>>fromJson(setting.getSettingData(), Set.class);
      properties.put(settingName, settingProperties);
    }
    return properties;
  }

  /**
   * Process cluster scoped configuration attributes contained in blueprint.
   *
   * @return cluster scoped property attributes contained within in blueprint
   */
  //todo: do inline with config processing
  private Map<String, Map<String, Map<String, String>>> parseAttributes(Collection<? extends BlueprintConfiguration> configs) {
    Map<String, Map<String, Map<String, String>>> mapAttributes =
      new HashMap<>();

    if (configs != null) {
      Gson gson = new Gson();
      for (BlueprintConfiguration config : configs) {
        Map<String, Map<String, String>> typeAttrs =
            gson.<Map<String, Map<String, String>>>fromJson(config.getConfigAttributes(), Map.class);
        if (typeAttrs != null && !typeAttrs.isEmpty()) {
          mapAttributes.put(config.getType(), typeAttrs);
        }
      }
    }
    return mapAttributes;
  }

  /**
   * Create host group entities and add to the parent blueprint entity.
   */
  @SuppressWarnings("unchecked")
  private void createHostGroupEntities(BlueprintEntity blueprintEntity) {
    Collection<HostGroupEntity> entities = new ArrayList<>();
    for (HostGroup group : getHostGroups().values()) {
      HostGroupEntity hostGroupEntity = new HostGroupEntity();
      entities.add(hostGroupEntity);

      hostGroupEntity.setName(group.getName());
      hostGroupEntity.setBlueprintEntity(blueprintEntity);
      hostGroupEntity.setBlueprintName(getName());
      hostGroupEntity.setCardinality(group.getCardinality());

      Collection<HostGroupConfigEntity> configEntities = toConfigEntities(group.getConfiguration(), HostGroupConfigEntity::new);
      configEntities.forEach(configEntity -> {
        configEntity.setBlueprintName(getName());
        configEntity.setHostGroupEntity(hostGroupEntity);
        configEntity.setHostGroupName(group.getName());
      });
      hostGroupEntity.setConfigurations(configEntities);

      createComponentEntities(hostGroupEntity, group.getComponents());
    }
    blueprintEntity.setHostGroups(entities);
  }

  /**
    * Create component entities and add to parent host group.
    */
  @SuppressWarnings("unchecked")
  private void createComponentEntities(HostGroupEntity group, Collection<Component> components) {
    Collection<HostGroupComponentEntity> componentEntities = new HashSet<>();
    group.setComponents(componentEntities);

    for (Component component : components) {
      HostGroupComponentEntity componentEntity = new HostGroupComponentEntity();
      componentEntities.add(componentEntity);

      componentEntity.setName(component.getName());
      componentEntity.setBlueprintName(group.getBlueprintName());
      componentEntity.setHostGroupEntity(group);
      componentEntity.setHostGroupName(group.getName());
      componentEntity.setServiceName(component.getServiceInstance());
      if (null != component.getMpackInstance()) {
        Preconditions.checkArgument(component.getMpackInstance().contains("-"),
          "Invalid mpack instance specified for component %s: %s. Must be in {name}-{version} format.",
          component.getName(),
          component.getMpackInstance());
        Iterator<String> mpackNameAndVersion =
          Splitter.on('-').split(component.getMpackInstance()).iterator();
        componentEntity.setMpackName(mpackNameAndVersion.next());
        componentEntity.setMpackVersion(mpackNameAndVersion.next());
      }

      // add provision action (if specified) to entity type
      // otherwise, just leave this column null (provision_action)
      if (component.getProvisionAction() != null) {
        componentEntity.setProvisionAction(component.getProvisionAction().toString());
      }

    }
    group.setComponents(componentEntities);
  }

  /**
   * Converts a {@link Configuration} class to a collection of configuration entities
   * @param configuration the configuration to be converted
   * @param entityCreator creates the appropriate config entity instance
   * @param <E> the config entity's type
   * @return a collection of configuration entities
   */
  static <E extends BlueprintConfiguration> Collection<E> toConfigEntities(Configuration configuration, Supplier<E> entityCreator) {
    Gson jsonSerializer = new Gson();
    Map<String, E> configEntityMap = new HashMap<>();
    for (Map.Entry<String, Map<String, String>> propEntry : configuration.getProperties().entrySet()) {
      String type = propEntry.getKey();
      Map<String, String> properties = propEntry.getValue();

      E configEntity = entityCreator.get();
      configEntityMap.put(type, configEntity);
      configEntity.setType(type);
      configEntity.setConfigData(jsonSerializer.toJson(properties));
    }

    for (Map.Entry<String, Map<String, Map<String, String>>> attributesEntry : configuration.getAttributes().entrySet()) {
      String type = attributesEntry.getKey();
      Map<String, Map<String, String>> attributes = attributesEntry.getValue();

      E entity = configEntityMap.get(type);
      if (entity == null) {
        entity = entityCreator.get();
        configEntityMap.put(type, entity);
        entity.setType(type);
      }
      entity.setConfigAttributes(jsonSerializer.toJson(attributes));
    }
    return configEntityMap.values();
  }

  /**
   * Converts a collection of configuration entities into a {@link Configuration}
   * @param configEntities the persisted configuration as entitiy collection
   * @return the configuration
   */
  static Configuration fromConfigEntities(Collection<? extends BlueprintConfiguration> configEntities) {
    Configuration configuration = new Configuration();

    for (BlueprintConfiguration configEntity: configEntities) {
      String type = configEntity.getType();
      Map<String, String> configData = JsonUtils.fromJson(configEntity.getConfigData(),
        new TypeReference<Map<String, String>>(){});
      Map<String, Map<String, String>> configAttributes = JsonUtils.fromJson(configEntity.getConfigAttributes(),
        new TypeReference<Map<String, Map<String, String>>>(){});
      if (null != configData) {
        configuration.getProperties().put(type, configData);
      }
      if (null != configAttributes) {
        configuration.getAttributes().put(type, configAttributes);
      }
    }

    return configuration;
  }

  /**
   * Populate setting for serialization to DB.
   */
  private void createBlueprintSettingEntities(BlueprintEntity blueprintEntity) {
    Gson jsonSerializer = new Gson();
    Setting blueprintSetting = getSetting();
    if (blueprintSetting != null) {
      Map<String, BlueprintSettingEntity> settingEntityMap = new HashMap<>();
      for (Map.Entry<String, Set<Map<String, String>>> propEntry : blueprintSetting.getProperties().entrySet()) {
        String settingName = propEntry.getKey();
        Set<Map<String, String>> properties = propEntry.getValue();

        BlueprintSettingEntity settingEntity = new BlueprintSettingEntity();
        settingEntityMap.put(settingName, settingEntity);
        settingEntity.setBlueprintName(getName());
        settingEntity.setBlueprintEntity(blueprintEntity);
        settingEntity.setSettingName(settingName);
        settingEntity.setSettingData(jsonSerializer.toJson(properties));
      }
      blueprintEntity.setSettings(settingEntityMap.values());
    }
  }

  /**
   * A config type is valid if there are services related to except cluster-env and global.
   */
  public boolean isValidConfigType(String configType) {
    if (ConfigHelper.CLUSTER_ENV.equals(configType) || "global".equals(configType)) {
      return true;
    }
    String service = getStack().getServiceForConfigType(configType);
    return getServices().contains(service);
  }

  /**
   * Parse stack repo info stored in the blueprint_settings table
   */
  private List<RepositorySetting> processRepoSettings() {
    return setting != null ? setting.processRepoSettings() : Collections.emptyList();
  }

  public List<RepositorySetting> getRepositorySettings(){
    return repoSettings;
  }

}
