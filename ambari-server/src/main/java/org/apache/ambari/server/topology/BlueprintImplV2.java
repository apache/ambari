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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.ambari.server.controller.StackV2;
import org.apache.ambari.server.orm.entities.BlueprintEntity;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.StackId;
import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Blueprint implementation.
 */
public class BlueprintImplV2 implements BlueprintV2 {

  private String name;
  private SecurityConfiguration securityConfiguration;
  private Collection<RepositoryVersion> repositoryVersions;
  private Collection<ServiceGroup> serviceGroups;
  private Collection<? extends HostGroupV2> hostGroups;
  private Setting setting;

  // Transient fields
  @JsonIgnore
  private Map<String, HostGroupV2> hostGroupMap = new HashMap<>();


  @JsonIgnore
  private Map<StackId, StackV2> stacks;

  @JsonIgnore
  private List<RepositorySetting> repoSettings;

  public void setStacks(Map<StackId, StackV2> stacks) {
    this.stacks = stacks;
  }

  @JsonProperty("Blueprints")
  public void setBlueprints(Blueprints blueprints) {
    this.name = blueprints.name;
    this.securityConfiguration = blueprints.securityConfiguration;
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

  @JsonProperty("service_groups")
  public void setServiceGroups(Collection<ServiceGroup> serviceGroups) {
    this.serviceGroups = serviceGroups;
  }

  @JsonProperty("host_groups")
  public void setHostGroups(Collection<HostGroupV2Impl> hostGroups) {
    this.hostGroups = hostGroups;
    this.hostGroupMap = hostGroups.stream().collect(Collectors.toMap(
      hg -> hg.getName(),
      hg -> hg
    ));
  }

  @JsonProperty("cluster-settings")
  public void setClusterSettings(Map<String, Set<HashMap<String, String>>> properties) {
    this.setting = new Setting(properties);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public HostGroupV2 getHostGroup(String name) {
    return hostGroupMap.get(name);
  }

  @Override
  public Map<String, HostGroupV2> getHostGroups() {
    return hostGroupMap;
  }

  @Override
  public Collection<StackV2> getStacks() {
    return stacks.values();
  }

  @Override
  public Collection<String> getStackIds() {
    return repositoryVersions.stream().map(rv -> rv.getStackId()).collect(Collectors.toList());
  }

  @Override
  public Collection<ServiceGroup> getServiceGroups() {
    return serviceGroups;
  }

  @Override
  @JsonIgnore
  public Collection<ServiceId> getAllServices() {
    return hostGroups.stream().flatMap(hg -> hg.getServices().stream()).collect(Collectors.toSet());
  }

  @Override
  @JsonIgnore
  public Collection<String> getAllServiceTypes() {
    return null;
  }

  @Override
  @JsonIgnore
  public Collection<Service> getServicesByType(String serviceType) {
    return null;
//    getAllServices().stream().filter(
//            service -> service.getType().equalsIgnoreCase(serviceType)).collect(Collectors.toList());
  }

  @Override
  @JsonIgnore
  public Collection<Service> getServicesFromServiceGroup(ServiceGroup serviceGroup, String serviceType) {
    if (serviceType == null) {
      return serviceGroup.getServices();
    } else {
      return serviceGroup.getServices().stream().filter(
              service -> service.getType().equalsIgnoreCase(serviceType)).collect(Collectors.toList());
    }
  }

  @Override
  @JsonIgnore
  public Collection<ComponentV2> getComponents(Service service) {
    return null;
  }

  @Override
  @JsonIgnore
  public Collection<ComponentV2> getComponentsByType(Service service, String componentType) {
    return getComponents(service).stream().filter(
            compnoent -> compnoent.getType().equalsIgnoreCase(componentType)).collect(Collectors.toList());
  }

  @Override
  @JsonIgnore
  public Collection<ComponentV2> getComponents(ServiceId serviceId) {
    return getHostGroupsForService(serviceId).stream().flatMap(hg -> hg.getComponents().stream()).collect(Collectors.toSet());
  }

  @Override
  @JsonIgnore
  public Collection<HostGroupV2> getHostGroupsForService(ServiceId serviceId) {
    return hostGroups.stream().filter(hg -> !hg.getComponents(serviceId).isEmpty()).collect(Collectors.toList());
  }

  @Override
  @JsonIgnore
  public Collection<HostGroupV2> getHostGroupsForComponent(ComponentV2 component) {
    return hostGroups.stream().filter(hg -> hg.getComponents().contains(component)).collect(Collectors.toList());
  }

  @Override
  public Configuration getConfiguration() {
    return null;
  }

  @Override
  public Setting getSetting() {
    return this.setting;
  }

  @Override
  public String getRecoveryEnabled(ComponentV2 component) {
    return null;
  }

  @Nonnull
  @Override
  @JsonIgnore
  public Collection<String> getAllServiceNames() {
    return getAllServices().stream().map(s -> s.getName()).collect(Collectors.toList());
  }

  @Nonnull
  @Override
  public Collection<String> getComponentNames(ServiceId serviceId) {
    return getComponents(serviceId).stream().map(c -> c.getName()).collect(Collectors.toList());
  }

  @Override
  public String getRecoveryEnabled(String serviceName, String componentName) {
    // If component name was specified in the list of "component_settings",
    // determine if recovery_enabled is true or false and return it.
    Optional<String> recoveryEnabled = getSettingValue(Setting.SETTING_NAME_COMPONENT_SETTINGS,
      Setting.SETTING_NAME_RECOVERY_ENABLED,
      Optional.of(componentName));
    if (recoveryEnabled.isPresent()) {
      return recoveryEnabled.get();
    }

    // If component name was specified in the list of "component_settings",
    // determine if recovery_enabled is true or false and return it.
    recoveryEnabled = getSettingValue(Setting.SETTING_NAME_SERVICE_SETTINGS,
      Setting.SETTING_NAME_RECOVERY_ENABLED,
      Optional.of(serviceName));
    if (recoveryEnabled.isPresent()) {
      return recoveryEnabled.get();
    }

    // If service name is not specified, look up the cluster setting.
    recoveryEnabled = getSettingValue(Setting.SETTING_NAME_RECOVERY_SETTINGS,
      Setting.SETTING_NAME_RECOVERY_ENABLED,
      Optional.empty());
    if (recoveryEnabled.isPresent()) {
      return recoveryEnabled.get();
    }

    return null;
  }

  private Optional<String> getSettingValue(String settingCategory, String settingName, Optional<String> nameFilter) {
    if (this.setting != null) {
      Set<HashMap<String, String>> settingValue = this.setting.getSettingValue(settingCategory);
      for (Map<String, String> setting : settingValue) {
        String name = setting.get(Setting.SETTING_NAME_NAME);
        if (!nameFilter.isPresent() || StringUtils.equals(name, nameFilter.get())) {
          String value = setting.get(settingName);
          if (!StringUtils.isEmpty(value)) {
            return Optional.of(value);
          }
        }
      }
    }
    return Optional.empty();
  }

  @Override
  public String getCredentialStoreEnabled(String serviceName) {
    // Look up the service and return the credential_store_enabled value.
    Optional<String> credentialStoreEnabled = getSettingValue(Setting.SETTING_NAME_SERVICE_SETTINGS,
      Setting.SETTING_NAME_CREDENTIAL_STORE_ENABLED,
      Optional.of(serviceName));
    return  credentialStoreEnabled.isPresent() ? credentialStoreEnabled.get() : null;
  }

  @Override
  public boolean shouldSkipFailure() {
    Optional<String> shouldSkipFailure = getSettingValue(Setting.SETTING_NAME_DEPLOYMENT_SETTINGS,
      Setting.SETTING_NAME_SKIP_FAILURE,
      Optional.empty());
    return shouldSkipFailure.isPresent() ? shouldSkipFailure.get().equalsIgnoreCase("true") : false;
  }

  @Override
  public SecurityConfiguration getSecurity() {
    return this.securityConfiguration;
  }


  @Override
  public boolean isValidConfigType(String configType) {
    if (ConfigHelper.CLUSTER_ENV.equals(configType) || "global".equals(configType)) {
      return true;
    }
    final Set<String> serviceNames =
      getAllServices().stream().map(s -> s.getName()).collect(Collectors.toSet());
    return getStacks().stream().anyMatch(
      stack -> {
        String service = stack.getServiceForConfigType(configType);
        return serviceNames.contains(service);
      }
    );
  }

  @Override
  public BlueprintEntity toEntity() {
    throw new UnsupportedOperationException("This is not supported here and will be removed. Pls. use BlueprintConverter");
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
    public SecurityConfiguration securityConfiguration;

    public Blueprints() { }
  }

}
