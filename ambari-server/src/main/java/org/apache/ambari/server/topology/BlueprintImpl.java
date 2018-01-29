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
import java.util.Optional;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.StackAccessException;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.internal.Stack;
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
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Supplier;
import com.google.gson.Gson;

/**
 * Blueprint implementation.
 */
public class BlueprintImpl implements Blueprint {

  private static final Logger LOG = LoggerFactory.getLogger(BlueprintImpl.class);

  private String name;
  private Map<String, HostGroup> hostGroups = new HashMap<>();
  private Collection<MpackInstance> mpacks = new ArrayList<>();
  private Configuration configuration;
  private BlueprintValidator validator;
  private SecurityConfiguration security;
  private Setting setting;
  private List<RepositorySetting> repoSettings = new ArrayList<>();
  private boolean allMpacksResolved = false;

  public BlueprintImpl(BlueprintEntity entity) throws NoSuchStackException {
    this.name = entity.getBlueprintName();
    if (entity.getSecurityType() != null) {
      this.security = new SecurityConfiguration(entity.getSecurityType(),
        entity.getSecurityDescriptorReference(),
        null);
    }
    mpacks.addAll(parseMpacks(entity));

    // create config first because it is set as a parent on all host-group configs
    configuration = processConfiguration(entity.getConfigurations());
    parseBlueprintHostGroups(entity);
    // TODO: how to handle multiple stacks correctly?
//    configuration.setParentConfiguration(stack.getConfiguration(getServices()));
    validator = new BlueprintValidatorImpl(this);
    processSetting(entity.getSettings());
    processRepoSettings();
  }

  /**
   * Legacy constructor for pre-multi-mpack code.
   * @param name blueprint name
   * @param groups host groups
   * @param stack stack
   * @param configuration configuration
   * @param security security config
   * @param setting setting
   */
  @Deprecated
  public BlueprintImpl(String name, Collection<HostGroup> groups, Stack stack, Configuration configuration,
                       SecurityConfiguration security, Setting setting) {
    this(name, groups, stackToMpacks(stack), configuration, security, setting);
  }

  private static Collection<MpackInstance> stackToMpacks(Stack stack) {
    MpackInstance mpack = new MpackInstance(stack.getName(), stack.getVersion(), null, stack, new Configuration());
    return Collections.singleton(mpack);
  }

  public BlueprintImpl(String name, Collection<HostGroup> groups, Collection<MpackInstance> mpacks,
                       Configuration configuration, SecurityConfiguration security, Setting setting) {
    this.name = name;
    this.mpacks = mpacks;
    this.security = security;

    // caller should set host group configs
    for (HostGroup hostGroup : groups) {
      hostGroups.put(hostGroup.getName(), hostGroup);
    }
    // TODO: handle configuration from multiple stacks properly
    // if the parent isn't set, the stack configuration is set as the parent
    this.configuration = configuration;
//    if (configuration.getParentConfiguration() == null) {
//      configuration.setParentConfiguration(stack.getConfiguration(getServices()));
//    }
    validator = new BlueprintValidatorImpl(this);
    this.setting = setting;
  }

  public String getName() {
    return name;
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

  /**
   * Get whether the specified component in the service is enabled
   * for auto start.
   *
   * @param serviceName - Service name.
   * @param componentName - Component name.
   *
   * @return null if value is not specified; true or false if specified.
   */
  @Override
  public String getRecoveryEnabled(String serviceName, String componentName) {
    Set<HashMap<String, String>> settingValue;

    if (setting == null)
      return null;

    // If component name was specified in the list of "component_settings",
    // determine if recovery_enabled is true or false and return it.
    settingValue = setting.getSettingValue(Setting.SETTING_NAME_COMPONENT_SETTINGS);
    for (Map<String, String> setting : settingValue) {
      String name = setting.get(Setting.SETTING_NAME_NAME);
      if (StringUtils.equals(name, componentName)) {
        if (!StringUtils.isEmpty(setting.get(Setting.SETTING_NAME_RECOVERY_ENABLED))) {
          return setting.get(Setting.SETTING_NAME_RECOVERY_ENABLED);
        }
      }
    }

    // If component name is not specified, look up it's service.
    settingValue = setting.getSettingValue(Setting.SETTING_NAME_SERVICE_SETTINGS);
    for ( Map<String, String> setting : settingValue){
      String name = setting.get(Setting.SETTING_NAME_NAME);
      if (StringUtils.equals(name, serviceName)) {
        if (!StringUtils.isEmpty(setting.get(Setting.SETTING_NAME_RECOVERY_ENABLED))) {
          return setting.get(Setting.SETTING_NAME_RECOVERY_ENABLED);
        }
      }
    }

    // If service name is not specified, look up the cluster setting.
    settingValue = setting.getSettingValue(Setting.SETTING_NAME_RECOVERY_SETTINGS);
    for (Map<String, String> setting : settingValue) {
      if (!StringUtils.isEmpty(setting.get(Setting.SETTING_NAME_RECOVERY_ENABLED))) {
        return setting.get(Setting.SETTING_NAME_RECOVERY_ENABLED);
      }
    }

    return null;
  }

  /**
   * Get whether the specified service is enabled for credential store use.
   *
   * <pre>
   *     {@code
   *       {
   *         "service_settings" : [
   *         { "name" : "RANGER",
   *           "recovery_enabled" : "true",
   *           "credential_store_enabled" : "true"
   *         },
   *         { "name" : "HIVE",
   *           "recovery_enabled" : "true",
   *           "credential_store_enabled" : "false"
   *         },
   *         { "name" : "TEZ",
   *           "recovery_enabled" : "false"
   *         }
   *       ]
   *     }
   *   }
   * </pre>
   *
   * @param serviceName - Service name.
   *
   * @return null if value is not specified; true or false if specified.
   */
  @Override
  public String getCredentialStoreEnabled(String serviceName) {
    if (setting == null)
      return null;

    // Look up the service and return the credential_store_enabled value.
    Set<HashMap<String, String>> settingValue = setting.getSettingValue(Setting.SETTING_NAME_SERVICE_SETTINGS);
    for (Map<String, String> setting : settingValue) {
      String name = setting.get(Setting.SETTING_NAME_NAME);
      if (StringUtils.equals(name, serviceName)) {
        if (!StringUtils.isEmpty(setting.get(Setting.SETTING_NAME_CREDENTIAL_STORE_ENABLED))) {
          return setting.get(Setting.SETTING_NAME_CREDENTIAL_STORE_ENABLED);
        }
        break;
      }
    }

    return null;
  }

  @Override
  public boolean shouldSkipFailure() {
    if (setting == null) {
      return false;
    }
    Set<HashMap<String, String>> settingValue = setting.getSettingValue(Setting.SETTING_NAME_DEPLOYMENT_SETTINGS);
    for (Map<String, String> setting : settingValue) {
      if (setting.containsKey(Setting.SETTING_NAME_SKIP_FAILURE)) {
        return setting.get(Setting.SETTING_NAME_SKIP_FAILURE).equalsIgnoreCase("true");
      }
    }
    return false;
  }

  @Override
  public Collection<MpackInstance> getMpacks() {
    return mpacks;
  }

  @Override
  public Collection<Stack> getStacks() {
    return mpacks.stream().map(MpackInstance::getStack).filter(s -> null != s).collect(toList());
  }

  @Override
  public Stack getStack() {
    return getStacks().iterator().next();
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

  @Override
  public void validateTopology() throws InvalidTopologyException {
    validator.validateTopology();
  }

  public BlueprintEntity toEntity() {
    BlueprintEntity entity = new BlueprintEntity();
    entity.setBlueprintName(name);
    if (security != null) {
      if (security.getType() != null) {
        entity.setSecurityType(security.getType());
      }
      if (security.getDescriptorReference() != null) {
        entity.setSecurityDescriptorReference(security.getDescriptorReference());
      }
    }

    createHostGroupEntities(entity);
    Collection<BlueprintConfigEntity> configEntities = toConfigEntities(getConfiguration(), () -> new BlueprintConfigEntity());
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

  /**
   * Validate blueprint configuration.
   *
   * @throws InvalidTopologyException if the blueprint configuration is invalid
   * @throws GPLLicenseNotAcceptedException ambari was configured to use gpl software, but gpl license is not accepted
   */
  @Override
  public void validateRequiredProperties() throws InvalidTopologyException, GPLLicenseNotAcceptedException {
    validator.validateRequiredProperties();
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
      tryParseStack(mpack.getMpackName(), mpack.getMpackVersion()).ifPresent( stack ->  mpackInstance.setStack(stack) );
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

  private Stack parseStack(String stackName, String stackVersion) throws NoSuchStackException {
    try {
      //todo: don't pass in controller
      return new Stack(stackName, stackVersion, AmbariServer.getController());
    } catch (StackAccessException e) {
      throw new NoSuchStackException(stackName, stackVersion, e);
    } catch (AmbariException e) {
      //todo:
      throw new RuntimeException("An error occurred parsing the stack information.", e);
    }
  }

  private Optional<Stack> tryParseStack(String stackName, String stackVersion) {
    try {
      return Optional.of(parseStack(stackName, stackVersion));
    }
    catch (Exception ex) {
      LOG.warn("Cannot parse stack {}-{}. Exception: {}/{}", stackName, stackVersion, ex.getClass().getName(),
        ex.getMessage());
      return Optional.empty();
    }
  }

  private Map<String, HostGroup> parseBlueprintHostGroups(BlueprintEntity entity) {
    for (HostGroupEntity hostGroupEntity : entity.getHostGroups()) {
      HostGroupImpl hostGroup = new HostGroupImpl(hostGroupEntity, getName(), getStacks());
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
   * Process blueprint setting.
   *
   * @param blueprintSetting
   */
  private void processSetting(Collection<BlueprintSettingEntity> blueprintSetting) {
    if (blueprintSetting != null) {
      setting = new Setting(parseSetting(blueprintSetting));
    }
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
  private Map<String, Set<HashMap<String, String>>> parseSetting(Collection<? extends BlueprintSettingEntity> blueprintSetting) {

    Map<String, Set<HashMap<String, String>>> properties = new HashMap<>();
    Gson gson = new Gson();
    for (BlueprintSettingEntity setting : blueprintSetting) {
      String settingName = setting.getSettingName();
      Set<HashMap<String, String>> settingProperties = gson.<Set<HashMap<String, String>>>fromJson(
              setting.getSettingData(), Set.class);
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

      Collection<HostGroupConfigEntity> configEntities = toConfigEntities(group.getConfiguration(), () -> new HostGroupConfigEntity());
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
    Gson jsonSerializer = new Gson();
    Configuration configuration = new Configuration();

    for (BlueprintConfiguration configEntity: configEntities) {
      String type = configEntity.getType();
      Map<String, String> configData = jsonSerializer.fromJson(configEntity.getConfigData(), Map.class);
      Map<String, Map<String, String>> configAttributes = jsonSerializer.fromJson(configEntity.getConfigAttributes(), Map.class);
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
      for (Map.Entry<String, Set<HashMap<String, String>>> propEntry : blueprintSetting.getProperties().entrySet()) {
        String settingName = propEntry.getKey();
        Set<HashMap<String, String>> properties = propEntry.getValue();

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

    Collection<String> services = getStacks().stream().map(stack -> stack.getServiceForConfigType(configType)).collect(toList());
    for (String service: services) {
      if (getServices().contains(service)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Parse stack repo info stored in the blueprint_settings table
   * @return set of repositories
   * */
  private void processRepoSettings(){
    repoSettings = new ArrayList<>();
    if (setting != null){
      Set<HashMap<String, String>> settingValue = setting.getSettingValue(Setting.SETTING_NAME_REPOSITORY_SETTINGS);
      for (Map<String, String> setting : settingValue) {
        RepositorySetting rs = parseRepositorySetting(setting);
        repoSettings.add(rs);
      }
    }
  }

  private RepositorySetting parseRepositorySetting(Map<String, String> setting){
    RepositorySetting result = new RepositorySetting();
    result.setOperatingSystem(setting.get(RepositorySetting.OPERATING_SYSTEM));
    result.setOverrideStrategy(setting.get(RepositorySetting.OVERRIDE_STRATEGY));
    result.setRepoId(setting.get(RepositorySetting.REPO_ID));
    result.setBaseUrl(setting.get(RepositorySetting.BASE_URL));
    return result;
  }

  public List<RepositorySetting> getRepositorySettings(){
    return repoSettings;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isAllMpacksResolved() {
    return !mpacks.stream().filter(mpack -> mpack.getStack() == null).findAny().isPresent();
  }

  /**
   * {@inheritDoc}
   */
  public Collection<String> getUnresolvedMpackNames() {
    return mpacks.stream().filter(mpack -> mpack.getStack() == null).map(MpackInstance::getMpackNameAndVersion).collect(toList());
  }

}
