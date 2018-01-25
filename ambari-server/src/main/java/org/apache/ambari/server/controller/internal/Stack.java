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

package org.apache.ambari.server.controller.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ReadOnlyConfigurationResponse;
import org.apache.ambari.server.state.AutoDeployInfo;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.DependencyInfo;
import org.apache.ambari.server.state.PropertyDependencyInfo;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.ValueAttributesInfo;
import org.apache.ambari.server.topology.Cardinality;
import org.apache.ambari.server.topology.Configuration;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * Encapsulates a single, identifiable stack definition.
 */
// TODO move to topology package
public class Stack implements StackDefinition {

  /**
   * Stack info
   */
  private final StackInfo stackInfo;

  /**
   * Map of service name to components
   */
  private Map<String, Collection<String>> serviceComponents =
    new HashMap<>();

  /**
   * Map of component to service
   */
  private Map<String, String> componentService = new HashMap<>();

  /**
   * Map of component to dependencies
   */
  private Map<String, Collection<DependencyInfo>> dependencies =
    new HashMap<>();

  /**
   * Map of dependency to conditional service
   */
  private Map<DependencyInfo, String> dependencyConditionalServiceMap =
    new HashMap<>();

  /**
   * Map of database component name to configuration property which indicates whether
   * the database in to be managed or if it is an external non-managed instance.
   * If the value of the config property starts with 'New', the database is determined
   * to be managed, otherwise it is non-managed.
   */
  private Map<String, String> dbDependencyInfo = new HashMap<>();

  /**
   * Map of component to required cardinality
   */
  private Map<String, String> cardinalityRequirements = new HashMap<>();

  //todo: instead of all these maps from component -> * ,
  //todo: we should use a Component object with all of these attributes
  private Set<String> masterComponents = new HashSet<>();

  /**
   * Map of component to auto-deploy information
   */
  private Map<String, AutoDeployInfo> componentAutoDeployInfo =
    new HashMap<>();

  /**
   * Map of service to config type properties
   */
  private Map<String, Map<String, Map<String, ConfigProperty>>> serviceConfigurations =
    new HashMap<>();

  /**
   * Map of service to required type properties
   */
  private Map<String, Map<String, Map<String, ConfigProperty>>> requiredServiceConfigurations =
    new HashMap<>();

  /**
   * Map of service to config type properties
   */
  private Map<String, Map<String, ConfigProperty>> stackConfigurations =
    new HashMap<>();

  /**
   * Map of service to set of excluded config types
   */
  private Map<String, Set<String>> excludedConfigurationTypes =
    new HashMap<>();

  public Stack(String name, String version, AmbariManagementController ctrl) throws AmbariException { // FIXME remove or at least change to use metainfo directly
    this(ctrl.getAmbariMetaInfo().getStack(name, version));
  }

  public Stack(StackInfo stackInfo) {
    Preconditions.checkNotNull(stackInfo);
    this.stackInfo = stackInfo;

    parseStackConfigurations();

    for (ServiceInfo stackService : stackInfo.getServices()) {
      parseComponents(stackService);
      parseExcludedConfigurations(stackService);
      parseConfigurations(stackService);
    }

    if (!stackInfo.getServices().isEmpty()) {
      registerConditionalDependencies();
    }
  }

  /**
   * Obtain stack name.
   *
   * @return stack name
   */
  public String getName() {
    return stackInfo.getName();
  }

  /**
   * Obtain stack version.
   *
   * @return stack version
   */
  public String getVersion() {
    return stackInfo.getVersion();
  }

  public StackId getStackId() {
    return new StackId(getName(), getVersion());
  }

  Map<DependencyInfo, String> getDependencyConditionalServiceMap() {
    return dependencyConditionalServiceMap;
  }

  @Override
  public Set<StackId> getStackIds() {
    return ImmutableSet.of(getStackId());
  }

  @Override
  public Set<StackId> getStacksForService(String serviceName) {
    return serviceComponents.keySet().contains(serviceName)
      ? ImmutableSet.of(getStackId())
      : ImmutableSet.of();
  }

  @Override
  public Set<String> getServices(StackId stackId) {
    return stackId.equals(getStackId())
      ? ImmutableSet.copyOf(getServices())
      : ImmutableSet.of();
  }

  @Override
  public Collection<String> getServices() {
    return serviceComponents.keySet();
  }

  @Override
  public Collection<String> getComponents(String service) {
    return serviceComponents.get(service);
  }

  @Override
  public Map<String, Collection<String>> getComponents() {
    Map<String, Collection<String>> serviceComponents = new HashMap<>();
    for (String service : getServices()) {
      Collection<String> components = new HashSet<>();
      components.addAll(getComponents(service));
      serviceComponents.put(service, components);
    }
    return serviceComponents;
  }

  @Override
  public ComponentInfo getComponentInfo(String component) {
    String service = getServiceForComponent(component);
    if (service != null) {
      ServiceInfo serviceInfo = stackInfo.getService(service);
      if (serviceInfo != null) {
        return serviceInfo.getComponentByName(component);
      }
    }
    return null;
  }

  @Override
  public Collection<String> getAllConfigurationTypes(String service) {
    Map<String, Map<String, ConfigProperty>> serviceConfigs = getServiceConfigurations(service);
    return serviceConfigs != null ? serviceConfigs.keySet() : ImmutableSet.of();
  }

  @Override
  public Collection<String> getConfigurationTypes(String service) {
    Set<String> serviceTypes = new HashSet<>(getAllConfigurationTypes(service));
    serviceTypes.removeAll(getExcludedConfigurationTypes(service));
    return serviceTypes;
  }

  @Override
  public Set<String> getExcludedConfigurationTypes(String service) {
    return excludedConfigurationTypes.containsKey(service) ?
        excludedConfigurationTypes.get(service) :
        Collections.emptySet();
  }

  @Override
  public Map<String, String> getConfigurationProperties(String service, String type) {
    Map<String, String> configMap = new HashMap<>();
    Map<String, ConfigProperty> configProperties = getConfigurationPropertiesWithMetadata(service, type);
    for (Map.Entry<String, ConfigProperty> configProperty : configProperties.entrySet()) {
      configMap.put(configProperty.getKey(), configProperty.getValue().getValue());
    }
    return configMap;
  }

  @Override
  public Map<String, ConfigProperty> getConfigurationPropertiesWithMetadata(String service, String type) {
    Map<String, ConfigProperty> map = getServiceConfigurations(service).get(type);
    return map != null ? ImmutableMap.copyOf(map) : ImmutableMap.of();
  }

  @Override
  public Collection<ConfigProperty> getRequiredConfigurationProperties(String service) {
    Collection<ConfigProperty> requiredConfigProperties = new HashSet<>();
    Map<String, Map<String, ConfigProperty>> serviceProperties = requiredServiceConfigurations.get(service);
    if (serviceProperties != null) {
      for (Map.Entry<String, Map<String, ConfigProperty>> typePropertiesEntry : serviceProperties.entrySet()) {
        requiredConfigProperties.addAll(typePropertiesEntry.getValue().values());
      }
    }
    return requiredConfigProperties;
  }

  @Override
  public Collection<ConfigProperty> getRequiredConfigurationProperties(String service, PropertyInfo.PropertyType propertyType) {
    Collection<ConfigProperty> matchingProperties = new HashSet<>();
    Map<String, Map<String, ConfigProperty>> requiredProperties = requiredServiceConfigurations.get(service);
    if (requiredProperties != null) {
      for (Map.Entry<String, Map<String, ConfigProperty>> typePropertiesEntry : requiredProperties.entrySet()) {
        for (ConfigProperty configProperty : typePropertiesEntry.getValue().values()) {
          if (configProperty.getPropertyTypes().contains(propertyType)) {
            matchingProperties.add(configProperty);
          }
        }

      }
    }
    return matchingProperties;
  }

  @Override
  public boolean isPasswordProperty(String service, String type, String propertyName) {
    Map<String, Map<String, ConfigProperty>> serviceConfigurations = getServiceConfigurations(service);
    return (serviceConfigurations.containsKey(type) &&
            serviceConfigurations.get(type).containsKey(propertyName) &&
            serviceConfigurations.get(type).get(propertyName).getPropertyTypes().
                contains(PropertyInfo.PropertyType.PASSWORD));
  }

  //todo
  @Override
  public Map<String, String> getStackConfigurationProperties(String type) {
    Map<String, String> configMap = new HashMap<>();
    Map<String, ConfigProperty> configProperties = stackConfigurations.get(type);
    if (configProperties != null) {
      for (Map.Entry<String, ConfigProperty> configProperty : configProperties.entrySet()) {
        configMap.put(configProperty.getKey(), configProperty.getValue().getValue());
      }
    }
    return configMap;
  }

  @Override
  public boolean isKerberosPrincipalNameProperty(String service, String type, String propertyName) {
    Map<String, Map<String, ConfigProperty>> serviceConfigurations = getServiceConfigurations(service);
    return (serviceConfigurations.containsKey(type) &&
            serviceConfigurations.get(type).containsKey(propertyName) &&
            serviceConfigurations.get(type).get(propertyName).getPropertyTypes().
                contains(PropertyInfo.PropertyType.KERBEROS_PRINCIPAL));
  }

  @Override
  public Map<String, Map<String, String>> getConfigurationAttributes(String service, String type) {
    Map<String, Map<String, String>> attributesMap = new HashMap<>();
    Map<String, ConfigProperty> configProperties = getServiceConfigurations(service).get(type);
    if (configProperties != null) {
      for (Map.Entry<String, ConfigProperty> configProperty : configProperties.entrySet()) {
        String propertyName = configProperty.getKey();
        Map<String, String> propertyAttributes = configProperty.getValue().getAttributes();
        if (propertyAttributes != null) {
          for (Map.Entry<String, String> propertyAttribute : propertyAttributes.entrySet()) {
            String attributeName = propertyAttribute.getKey();
            String attributeValue = propertyAttribute.getValue();
            if (attributeValue != null) {
              attributesMap
                .computeIfAbsent(attributeName, k -> new HashMap<>())
                .put(propertyName, attributeValue);
            }
          }
        }
      }
    }
    return attributesMap;
  }

  private Map<String, Map<String, ConfigProperty>> getServiceConfigurations(String service) {
    Map<String, Map<String, ConfigProperty>> map = serviceConfigurations.get(service);
    return map != null ? ImmutableMap.copyOf(map) : ImmutableMap.of();
  }

  @Override
  public Map<String, Map<String, String>> getStackConfigurationAttributes(String type) {
    Map<String, Map<String, String>> attributesMap = new HashMap<>();
    Map<String, ConfigProperty> configProperties = stackConfigurations.get(type);
    if (configProperties != null) {
      for (Map.Entry<String, ConfigProperty> configProperty : configProperties.entrySet()) {
        String propertyName = configProperty.getKey();
        Map<String, String> propertyAttributes = configProperty.getValue().getAttributes();
        if (propertyAttributes != null) {
          for (Map.Entry<String, String> propertyAttribute : propertyAttributes.entrySet()) {
            String attributeName = propertyAttribute.getKey();
            String attributeValue = propertyAttribute.getValue();
            attributesMap
              .computeIfAbsent(attributeName, k -> new HashMap<>())
              .put(propertyName, attributeValue);
          }
        }
      }
    }
    return attributesMap;
  }

  @Override
  public String getServiceForComponent(String component) {
    return componentService.get(component);
  }

  @Override
  public Collection<String> getServicesForComponents(Collection<String> components) {
    Set<String> services = new HashSet<>();
    for (String component : components) {
      services.add(getServiceForComponent(component));
    }

    return services;
  }

  @Override
  public String getServiceForConfigType(String config) {
    if (ConfigHelper.CLUSTER_ENV.equals(config)) { // for backwards compatibility
      return null;
    }
    for (Map.Entry<String, Map<String, Map<String, ConfigProperty>>> entry : serviceConfigurations.entrySet()) {
      Map<String, Map<String, ConfigProperty>> typeMap = entry.getValue();
      String serviceName = entry.getKey();
      if (typeMap.containsKey(config) && !getExcludedConfigurationTypes(serviceName).contains(config)) {
        return serviceName;
      }
    }
    throw new IllegalArgumentException(formatMissingServiceForConfigType(config, getStackId().toString()));
  }

  static String formatMissingServiceForConfigType(String config, String stackId) {
    return String.format("Specified configuration type %s is not associated with any service in %s stack.", config, stackId);
  }

  @Override
  public Stream<String> getServicesForConfigType(String config) {
    return serviceConfigurations.entrySet().stream()
      .filter(e -> e.getValue().containsKey(config))
      .filter(e -> !getExcludedConfigurationTypes(e.getKey()).contains(config))
      .map(Map.Entry::getKey);
  }

  @Override
  public Collection<DependencyInfo> getDependenciesForComponent(String component) {
    return dependencies.containsKey(component) ? dependencies.get(component) :
        Collections.emptySet();
  }

  @Override
  public String getConditionalServiceForDependency(DependencyInfo dependency) {
    return dependencyConditionalServiceMap.get(dependency);
  }

  @Override
  public String getExternalComponentConfig(String component) {
    return dbDependencyInfo.get(component);
  }

  @Override
  public Cardinality getCardinality(String component) {
    return new Cardinality(cardinalityRequirements.get(component));
  }

  @Override
  public AutoDeployInfo getAutoDeployInfo(String component) {
    return componentAutoDeployInfo.get(component);
  }

  @Override
  public boolean isMasterComponent(String component) {
    return masterComponents.contains(component);
  }

  @Override
  public Configuration getConfiguration(Collection<String> services) {
    Map<String, Map<String, Map<String, String>>> attributes = new HashMap<>();
    Map<String, Map<String, String>> properties = new HashMap<>();

    for (String service : services) {
      Collection<String> serviceConfigTypes = getConfigurationTypes(service);
      for (String type : serviceConfigTypes) {
        properties
          .computeIfAbsent(type, k -> new HashMap<>())
          .putAll(getConfigurationProperties(service, type));

        Map<String, Map<String, String>> stackTypeAttributes = getConfigurationAttributes(service, type);
        if (!stackTypeAttributes.isEmpty()) {
          if (! attributes.containsKey(type)) {
            attributes.put(type, new HashMap<>());
          }
          Map<String, Map<String, String>> typeAttributes = attributes.get(type);
          for (Map.Entry<String, Map<String, String>> attribute : stackTypeAttributes.entrySet()) {
            String attributeName = attribute.getKey();
            typeAttributes
              .computeIfAbsent(attributeName, k -> new HashMap<>())
              .putAll(attribute.getValue());
          }
        }
      }
    }
    return new Configuration(properties, attributes);
  }

  @Override
  public Configuration getConfiguration() { // TODO get rid of duplication between this and #getConfiguration(Collection<String>)
    Map<String, Map<String, Map<String, String>>> stackAttributes = new HashMap<>();
    Map<String, Map<String, String>> stackConfigs = new HashMap<>();

    for (String service : getServices()) {
      for (String type : getAllConfigurationTypes(service)) {
        stackConfigs
          .computeIfAbsent(type, k -> new HashMap<>())
          .putAll(getConfigurationProperties(service, type));

        Map<String, Map<String, String>> stackTypeAttributes = getConfigurationAttributes(service, type);
        if (!stackTypeAttributes.isEmpty()) {
          if (! stackAttributes.containsKey(type)) {
            stackAttributes.put(type, new HashMap<>());
          }
          Map<String, Map<String, String>> typeAttrs = stackAttributes.get(type);
          for (Map.Entry<String, Map<String, String>> attribute : stackTypeAttributes.entrySet()) {
            String attributeName = attribute.getKey();
            typeAttrs
              .computeIfAbsent(attributeName, k -> new HashMap<>())
              .putAll(attribute.getValue());
          }
        }
      }
    }
    return new Configuration(stackConfigs, stackAttributes);
  }

  /**
   * Parse components for the specified service from the stack definition.
   */
  private void parseComponents(ServiceInfo serviceInfo) {
    Collection<String> componentSet = new HashSet<>();

    String service = serviceInfo.getName();
    Collection<ComponentInfo> components = serviceInfo.getComponents();

    // stack service components
    for (ComponentInfo component : components) {
      String componentName = component.getName();
      componentSet.add(componentName);
      componentService.put(componentName, service);
      String cardinality = component.getCardinality();
      if (cardinality != null) {
        cardinalityRequirements.put(componentName, cardinality);
      }
      AutoDeployInfo autoDeploy = component.getAutoDeploy();
      if (autoDeploy != null) {
        componentAutoDeployInfo.put(componentName, autoDeploy);
      }

      // populate component dependencies
      Collection<DependencyInfo> componentDependencies = component.getDependencies();
      if (componentDependencies != null && ! componentDependencies.isEmpty()) {
        dependencies.put(componentName, componentDependencies);
      }
      if (component.isMaster()) {
        masterComponents.add(componentName);
      }
    }
    serviceComponents.put(service, componentSet);
  }

  /**
   * Parse configurations for the specified service from the stack definition.
   *
   * @param stackService  service to parse the stack configuration for
   */
  private void parseConfigurations(ServiceInfo stackService) {
    String service = stackService.getName();
    Map<String, Map<String, ConfigProperty>> mapServiceConfig = new HashMap<>();
    Map<String, Map<String, ConfigProperty>> mapRequiredServiceConfig = new HashMap<>();

    serviceConfigurations.put(service, mapServiceConfig);
    requiredServiceConfigurations.put(service, mapRequiredServiceConfig);

    Collection<PropertyInfo> serviceConfigs = stackService.getProperties();
    Collection<PropertyInfo> stackLevelConfigs = stackInfo.getProperties();
    serviceConfigs.addAll(stackLevelConfigs);

    // shouldn't have any required properties in stack level configuration
    for (PropertyInfo pi : serviceConfigs) {
      ReadOnlyConfigurationResponse config = pi.convertToResponse(); // TODO get rid of intermediate object
      ConfigProperty configProperty = new ConfigProperty(config);
      String type = configProperty.getType();

      Map<String, ConfigProperty> mapTypeConfig = mapServiceConfig.computeIfAbsent(type, __ -> new HashMap<>());

      String name = config.getPropertyName();
      mapTypeConfig.put(name, configProperty);
      if (config.isRequired()) {
        mapRequiredServiceConfig
          .computeIfAbsent(type, __ -> new HashMap<>())
          .put(name, configProperty);
      }
    }

    // So far we added only config types that have properties defined
    // in stack service definition. Since there might be config types
    // with no properties defined we need to add those separately
    Set<String> configTypes = stackService.getConfigTypeAttributes().keySet();
    for (String configType: configTypes) {
      if (!mapServiceConfig.containsKey(configType)) {
        mapServiceConfig.put(configType, Collections.emptyMap());
      }
    }
  }

  private void parseStackConfigurations() {
    Collection<PropertyInfo> stackLevelConfigs = stackInfo.getProperties();

    for (PropertyInfo pi : stackLevelConfigs) {
      ReadOnlyConfigurationResponse config = pi.convertToResponse(); // TODO get rid of intermediate object
      ConfigProperty configProperty = new ConfigProperty(config);
      String type = configProperty.getType();

      stackConfigurations
        .computeIfAbsent(type, __ -> new HashMap<>())
        .put(config.getPropertyName(), configProperty);
    }
  }

  /**
   * Obtain the excluded configuration types from the StackServiceResponse
   *
   * @param stackServiceResponse the response object associated with this stack service
   */
  private void parseExcludedConfigurations(ServiceInfo stackServiceResponse) {
    excludedConfigurationTypes.put(stackServiceResponse.getName(), stackServiceResponse.getExcludedConfigTypes());
  }

  /**
   * Register conditional dependencies.
   */
  //todo: This information should be specified in the stack definition.
  void registerConditionalDependencies() {
    dbDependencyInfo.put("MYSQL_SERVER", "global/hive_database");
  }

  /**
   * Contains a configuration property's value and attributes.
   */
  public static class ConfigProperty {
    private ValueAttributesInfo propertyValueAttributes = null;
    private String name;
    private String value;
    private Map<String, String> attributes;
    private Set<PropertyInfo.PropertyType> propertyTypes;
    private String type;
    private Set<PropertyDependencyInfo> dependsOnProperties =
      Collections.emptySet();

    public ConfigProperty(ReadOnlyConfigurationResponse config) {
      this.name = config.getPropertyName();
      this.value = config.getPropertyValue();
      this.attributes = config.getPropertyAttributes();
      this.propertyTypes = config.getPropertyType();
      this.type = normalizeType(config.getType());
      this.dependsOnProperties = config.getDependsOnProperties();
      this.propertyValueAttributes = config.getPropertyValueAttributes();
    }

    public ConfigProperty(String type, String name, String value) {
      this.type = type;
      this.name = name;
      this.value = value;
    }

    public String getName() {
      return name;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }

    public String getType() {
      return type;
    }

    public Set<PropertyInfo.PropertyType> getPropertyTypes() {
      return propertyTypes;
    }

    public void setPropertyTypes(Set<PropertyInfo.PropertyType> propertyTypes) {
      this.propertyTypes = propertyTypes;
    }

    public Map<String, String> getAttributes() {
      return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
      this.attributes = attributes;
    }

    Set<PropertyDependencyInfo> getDependsOnProperties() {
      return this.dependsOnProperties;
    }

    private String normalizeType(String type) {
      //strip .xml from type
      if (type.endsWith(".xml")) {
        type = type.substring(0, type.length() - 4);
      }
      return type;
    }

    public ValueAttributesInfo getPropertyValueAttributes() {
      return propertyValueAttributes;
    }
  }
}
