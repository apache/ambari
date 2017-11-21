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
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nonnull;

import org.apache.ambari.server.controller.StackV2;
import org.apache.ambari.server.orm.entities.BlueprintEntity;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.StackId;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Blueprint implementation.
 */
public class BlueprintV2Impl implements BlueprintV2 {

  private String name;
  private SecurityConfiguration securityConfiguration = SecurityConfiguration.NONE;
  private Collection<RepositoryVersion> repositoryVersions = Collections.emptyList();
  private Map<String, ServiceGroup> serviceGroups = Collections.emptyMap();
  private Setting setting = new Setting(Collections.emptyMap());
  private final Configuration configuration = Configuration.createEmpty();

  // Transient fields
  @JsonIgnore
  private Map<String, HostGroupV2Impl> hostGroupMap = new HashMap<>();

  @JsonIgnore
  private Map<StackId, StackV2> stacks;

  @JsonIgnore
  private List<RepositorySetting> repoSettings;

  @JsonIgnore
  private Map<ServiceId, Service> services = new HashMap<>();

  public void setStacks(Map<StackId, StackV2> stacks) {
    this.stacks = stacks;
    getAllServices().forEach(s -> s.setStackFromBlueprint(this));
  }

  @JsonProperty("Blueprints")
  public void setBlueprints(Blueprints blueprints) {
    this.name = blueprints.name;
    this.securityConfiguration = blueprints.securityConfiguration;
  }

  @JsonProperty("Blueprints")
  public Blueprints getBlueprints() {
    Blueprints blueprints = new Blueprints();
    blueprints.name = this.name;
    blueprints.securityConfiguration = this.securityConfiguration;
    return blueprints;
  }


  public void setName(String name) {
    this.name = name;
  }

  public void setSecurityConfiguration(SecurityConfiguration securityConfiguration) {
    this.securityConfiguration = securityConfiguration;
  }

  @JsonProperty("repository_versions")
  public void setRepositoryVersions(Collection<RepositoryVersion> repositoryVersions) {
    this.repositoryVersions = repositoryVersions;
  }

  @JsonProperty("repository_versions")
  public Collection<RepositoryVersion> getRepositoryVersions() {
    return this.repositoryVersions;
  }

  @JsonProperty("service_groups")
  public void setServiceGroups(Collection<ServiceGroup> serviceGroups) {
    this.serviceGroups = serviceGroups.stream().collect(toMap(ServiceGroup::getName, Function.identity()));
  }

  @JsonProperty("host_groups")
  public void setHostGroups(Collection<HostGroupV2Impl> hostGroups) {
    this.hostGroupMap = hostGroups.stream().collect(toMap(
      HostGroupV2Impl::getName,
      Function.identity()
    ));
  }

  @JsonProperty("cluster_settings")
  public void setClusterSettings(Map<String, Set<HashMap<String, String>>> properties) {
    this.setting = new Setting(properties);
  }

  @Override
  @JsonIgnore
  public String getName() {
    return name;
  }

  @Override
  public HostGroupV2 getHostGroup(String name) {
    return hostGroupMap.get(name);
  }

  @Override
  @JsonIgnore
  public Map<String, ? extends HostGroupV2> getHostGroups() {
    return hostGroupMap;
  }

  @JsonProperty("host_groups")
  public Collection<? extends HostGroupV2> getHostGroupsForSerialization() {
    return hostGroupMap.values();
  }

  @Override
  @JsonIgnore
  public Collection<StackV2> getStacks() {
    return stacks.values();
  }

  @Override
  @JsonIgnore
  public Collection<String> getStackIds() {
    return repositoryVersions.stream().map(RepositoryVersion::getStackId).collect(toList());
  }

  @Override
  public Collection<ServiceGroup> getServiceGroups() {
    return serviceGroups.values();
  }

  @Override
  public ServiceGroup getServiceGroup(String name) {
    return serviceGroups.get(name);
  }

  @Override
  @JsonIgnore
  public Collection<ServiceId> getAllServiceIds() {
    return getHostGroups().values().stream().flatMap(hg -> hg.getServiceIds().stream()).collect(toSet());
  }

  @Override
  public Service getServiceById(ServiceId serviceId) {
    return null;
  }

  @Override
  @JsonIgnore
  public Collection<Service> getServicesFromServiceGroup(ServiceGroup serviceGroup, String serviceType) {
    if (serviceType == null) {
      return serviceGroup.getServices();
    } else {
      return serviceGroup.getServices().stream().filter(
              service -> service.getType().equalsIgnoreCase(serviceType)).collect(toList());
    }
  }

  @Override
  @JsonIgnore
  public StackV2 getStackById(String stackId) {
    return stacks.get(new StackId(stackId));
  }

  @Override
  @JsonIgnore
  public Collection<Service> getAllServices() {
    return services.values();
  }

  @Override
  @JsonIgnore
  public Service getService(ServiceId serviceId) {
    return services.get(serviceId);
  }

  @Override
  @JsonIgnore
  public Collection<String> getAllServiceTypes() {
    return getServiceGroups().stream().flatMap(sg -> sg.getServices().stream()).map(Service::getType).collect(toSet());
  }

  @Override
  @JsonIgnore
  public Collection<Service> getServicesByType(String serviceType) {
    return serviceGroups.values().stream().flatMap(sg -> sg.getServiceByType(serviceType).stream()).collect(toList());
  }

  @Override
  @JsonIgnore
  public Collection<ComponentV2> getComponents(Service service) {
    return getHostGroupsForService(service.getId()).stream()
      .flatMap(hg -> hg.getComponents().stream())
      .filter(c -> c.getServiceId().equals(service.getId()))
      .collect(toList());
  }

  @Override
  @JsonIgnore
  public Collection<ComponentV2> getComponentsByType(Service service, String componentType) {
    return getComponents(service).stream()
      .filter(c -> c.getType().equalsIgnoreCase(componentType))
      .collect(toList());
  }

  @Override
  @JsonIgnore
  public Collection<ComponentV2> getComponents(ServiceId serviceId) {
    return getHostGroupsForService(serviceId).stream()
      .flatMap(hg -> hg.getComponents().stream())
      .filter(c -> c.getServiceId().equals(serviceId))
      .collect(toSet());
  }

  @Override
  @JsonIgnore
  public Collection<HostGroupV2> getHostGroupsForService(ServiceId serviceId) {
    return getHostGroups().values().stream().filter(hg -> !hg.getComponentsByServiceId(serviceId).isEmpty()).collect(toList());
  }

  @Override
  @JsonIgnore
  public Collection<HostGroupV2> getHostGroupsForComponent(ComponentV2 component) {
    return hostGroupMap.values().stream().filter(hg -> hg.getComponents().contains(component)).collect(toList());
  }

  @Override
  @JsonIgnore
  public Configuration getConfiguration() {
    return configuration;
  }

  private void addChildConfiguration(Configuration parent, Configuration child) {
    child.setParentConfiguration(parent);
    parent.getProperties().putAll(child.getProperties());
    parent.getAttributes().putAll(child.getAttributes());
  }

  @Override
  @JsonIgnore
  public Setting getSetting() {
    return this.setting;
  }

  @JsonProperty("cluster_settings")
  public Map<String, Set<HashMap<String, String>>> getSettingForSerialization() {
    return this.setting.getProperties();
  }

  @Nonnull
  @Override
  @JsonIgnore
  public Collection<String> getAllServiceNames() {
    return getAllServices().stream().map(Service::getName).collect(toList());
  }

  @Nonnull
  @Override
  public Set<String> getComponentNames(ServiceId serviceId) {
    return getComponents(serviceId).stream().map(ComponentV2::getName).collect(toSet());
  }

  @Override
  public String getRecoveryEnabled(ComponentV2 component) {
    Optional<String> value =
      setting.getSettingValue(Setting.SETTING_NAME_RECOVERY_SETTINGS, Setting.SETTING_NAME_RECOVERY_ENABLED);
    // TODO: handle service and component level settings
    return value.orElse(null);
  }

//  private Optional<String> getSettingValue(String settingCategory, String settingName, Optional<String> nameFilter) {
//    if (this.setting != null) {
//      Set<HashMap<String, String>> settingValue = this.setting.getSettingValue(settingCategory);
//      for (Map<String, String> setting : settingValue) {
//        String name = setting.get(Setting.SETTING_NAME_NAME);
//        if (!nameFilter.isPresent() || StringUtils.equals(name, nameFilter.get())) {
//          String value = setting.get(settingName);
//          if (!StringUtils.isEmpty(value)) {
//            return Optional.of(value);
//          }
//        }
//      }
//    }
//    return Optional.empty();
//  }

  @Override
  public String getCredentialStoreEnabled(String serviceName) {
    // TODO: this is a service level level setting, handle appropriately
    return null;
  }

  @Override
  public boolean shouldSkipFailure() {
    return setting.getSettingValue(Setting.SETTING_NAME_DEPLOYMENT_SETTINGS, Setting.SETTING_NAME_SKIP_FAILURE)
      .map(Boolean::parseBoolean).orElse(false);
  }

  @Override
  @JsonIgnore
  public SecurityConfiguration getSecurity() {
    return this.securityConfiguration;
  }

  @Override
  public void validateRequiredProperties() throws InvalidTopologyException {
    // TODO implement
  }

  @Override
  public void validateTopology() throws InvalidTopologyException {
    // TODO implement
  }


  @Override
  public boolean isValidConfigType(String configType) {
    if (ConfigHelper.CLUSTER_ENV.equals(configType) || "global".equals(configType)) {
      return true;
    }
    final Set<String> serviceNames =
      getAllServices().stream().map(Service::getName).collect(toSet());
    return getStacks().stream().anyMatch(
      stack -> {
        String service = stack.getServiceForConfigType(configType);
        return serviceNames.contains(service);
      }
    );
  }

  public void postDeserialization() {
    // Maintain a ServiceId -> Service map
    this.services = getAllServiceIds().stream().collect(toMap(
      Function.identity(),
      serviceId -> {
        Service service = getServiceGroup(serviceId.getServiceGroup()).getServiceByName(serviceId.getName());
        if (null == service) {
          throw new IllegalStateException("Cannot find service for service id: " + serviceId);
        }
        return service;
      }
    ));

    // Set Service -> ServiceGroup references and Service -> Service dependencies
    getAllServices().forEach( s -> {
      s.setServiceGroup(serviceGroups.get(s.getServiceGroupId()));
      Map<ServiceId, Service> dependencies = s.getDependentServiceIds().stream().collect(toMap(
        Function.identity(),
        this::getService
      ));
      s.setDependencyMap(dependencies);
    });


    // Set HostGroup -> Services and Component -> Service references
    for (HostGroupV2Impl hg: hostGroupMap.values()) {
      hg.setServiceMap(hg.getServiceIds().stream().collect(toMap(
        Function.identity(),
        serviceId -> this.services.get(serviceId)
      )));
      for (ComponentV2 comp: hg.getComponents()) {
        comp.setService(hg.getService(comp.getServiceId()));
      }
    }
  }

  @Override
  public BlueprintEntity toEntity() {
    throw new UnsupportedOperationException("This is not supported here and will be removed. Pls. use BlueprintV2Factory");
  }

  @Override
  public List<RepositorySetting> getRepositorySettings() {
    return repoSettings;
  }

  /**
   * Class to support Jackson data binding. Instances are used only temporarily during serialization
   */
  public class Blueprints {
    @JsonProperty("blueprint_name")
    public String name;
    @JsonProperty("security")
    public SecurityConfiguration securityConfiguration = SecurityConfiguration.NONE;

    public Blueprints() { }
  }

}
