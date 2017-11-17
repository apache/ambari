/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.controller;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.state.AutoDeployInfo;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.DependencyInfo;
import org.apache.ambari.server.state.PropertyDependencyInfo;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.ValueAttributesInfo;
import org.apache.ambari.server.topology.Cardinality;
import org.apache.ambari.server.topology.Configuration;

/**
 * Encapsulates stack information.
 */
public class StackV2 {

  /** Stack name */
  private final String name;

  /** Stack version */
  private final String version;

  /** Repo version */
  private final String repoVersion;

  /** Map of service name to components */
  private final Map<String, Collection<String>> serviceComponents;

  /** Map of component to service */
  private final Map<String, String> componentService;

  /** Map of component to dependencies */
  private final Map<String, Collection<DependencyInfo>> dependencies;

  /** Map of dependency to conditional service */
  private final Map<DependencyInfo, String> dependencyConditionalServiceMap;

  /**
   * Map of database component name to configuration property which indicates whether
   * the database in to be managed or if it is an external non-managed instance.
   * If the value of the config property starts with 'New', the database is determined
   * to be managed, otherwise it is non-managed.
   */
  private final Map<String, String> dbDependencyInfo;

  /** Map of component to required cardinality */
  private final Map<String, String> cardinalityRequirements = new HashMap<>();

  //todo: instead of all these maps from component -> * ,
  //todo: we should use a Component object with all of these attributes
  private Set<String> masterComponents = new HashSet<>();

  /** Map of component to auto-deploy information */
  private final Map<String, AutoDeployInfo> componentAutoDeployInfo;

  /** Map of service to config type properties */
  private final Map<String, Map<String, Map<String, ConfigProperty>>> serviceConfigurations;

  /** Map of service to required type properties */
  private final Map<String, Map<String, Map<String, ConfigProperty>>> requiredServiceConfigurations;

  /** Map of service to config type properties */
  private final Map<String, Map<String, ConfigProperty>> stackConfigurations;

  /** Map of service to set of excluded config types */
  private final Map<String, Set<String>> excludedConfigurationTypes;

  private final Map<String, ComponentInfo> componentInfos;

  public StackV2(String name,
           String version,
           String repoVersion,
           Map<String, Collection<String>> serviceComponents,
           Map<String, Collection<DependencyInfo>> dependencies,
           Map<String, String> dbDependencyInfo,
           Map<String, AutoDeployInfo> componentAutoDeployInfo,
           Map<String, Map<String, Map<String, ConfigProperty>>> serviceConfigurations,
           Map<String, Map<String, Map<String, ConfigProperty>>> requiredServiceConfigurations,
           Map<String, Map<String, ConfigProperty>> stackConfigurations,
           Map<String, Set<String>> excludedConfigurationTypes,
           Map<String, ComponentInfo> componentInfos) {
    this.name = name;
    this.version = version;
    this.repoVersion = repoVersion;

    this.serviceComponents = serviceComponents;
    this.componentService = new HashMap<>();
    for (Map.Entry<String, Collection<String>> entry: serviceComponents.entrySet()) {
      for (String comp: entry.getValue()) {
        componentService.put(comp, entry.getKey());
      }
    }

    this.dependencies = dependencies;
    this.dependencyConditionalServiceMap = new HashMap<>();
    for (Map.Entry<String, Collection<DependencyInfo>> entry: dependencies.entrySet()) {
      for (DependencyInfo di: entry.getValue()) {
        dependencyConditionalServiceMap.put(di, entry.getKey());
      }
    }

    this.dbDependencyInfo = dbDependencyInfo;
    this.componentAutoDeployInfo = componentAutoDeployInfo;
    this.serviceConfigurations = serviceConfigurations;
    this.requiredServiceConfigurations = requiredServiceConfigurations;
    this.stackConfigurations = stackConfigurations;
    this.excludedConfigurationTypes = excludedConfigurationTypes;
    this.componentInfos = componentInfos;
  }

  /** @return stack name */
  public String getName() {
    return name;
  }

  /** @return stack version */
  public String getVersion() {
    return version;
  }

  public StackId getStackId() {
    return new StackId(name, version);
  }

  /** @return repo version */
  public String getRepoVersion() { return repoVersion; }

  Map<DependencyInfo, String> getDependencyConditionalServiceMap() {
    return dependencyConditionalServiceMap;
  }

  /** @return collection of all services for the stack */
  public Collection<String> getServices() {
    return serviceComponents.keySet();
  }

  /**
   * Get components contained in the stack for the specified service.
   *
   * @param service  service name
   * @return collection of component names for the specified service
   */
  public Collection<String> getComponents(String service) {
    return serviceComponents.get(service);
  }

  /** @return map of service to associated components */
  public Map<String, Collection<String>> getComponents() {
    return serviceComponents;
  }

    /**
     * Get info for the specified component.
     *
     * @param component  component name
     *
     * @return component information for the requested component
     *     or null if the component doesn't exist in the stack
     */
    @Deprecated
    public ComponentInfo getComponentInfo(String component) {
      return componentInfos.get(component);
    }

  /**
   * Get all configuration types, including excluded types for the specified service.
   *
   * @param service  service name
   *
   * @return collection of all configuration types for the specified service
   */
  public Collection<String> getAllConfigurationTypes(String service) {
    return serviceConfigurations.get(service).keySet();
  }

  /**
   * Get configuration types for the specified service.
   * This doesn't include any service excluded types.
   *
   * @param service  service name
   *
   * @return collection of all configuration types for the specified service
   */
  public Collection<String> getConfigurationTypes(String service) {
    Set<String> serviceTypes = new HashSet<>(serviceConfigurations.get(service).keySet());
    serviceTypes.removeAll(getExcludedConfigurationTypes(service));
    return serviceTypes;
  }

  /**
   * Get the set of excluded configuration types for this service.
   *
   * @param service service name
   *
   * @return Set of names of excluded config types. Will not return null.
   */
  public Set<String> getExcludedConfigurationTypes(String service) {
    return excludedConfigurationTypes.containsKey(service) ?
        excludedConfigurationTypes.get(service) :
        Collections.emptySet();
  }

  /**
   * Get config properties for the specified service and configuration type.
   *
   * @param service  service name
   * @param type   configuration type
   *
   * @return map of property names to values for the specified service and configuration type
   */
  public Map<String, String> getConfigurationProperties(String service, String type) {
    Map<String, String> configMap = new HashMap<>();
    Map<String, ConfigProperty> configProperties = serviceConfigurations.get(service).get(type);
    if (configProperties != null) {
      for (Map.Entry<String, ConfigProperty> configProperty : configProperties.entrySet()) {
        configMap.put(configProperty.getKey(), configProperty.getValue().getValue());
      }
    }
    return configMap;
  }

  public Map<String, ConfigProperty> getConfigurationPropertiesWithMetadata(String service, String type) {
    return serviceConfigurations.get(service).get(type);
  }

  /**
   * Get all required config properties for the specified service.
   *
   * @param service  service name
   *
   * @return collection of all required properties for the given service
   */
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

  /**
   * Get required config properties for the specified service which belong to the specified property type.
   *
   * @param service     service name
   * @param propertyType  property type
   *
   * @return collection of required properties for the given service and property type
   */
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

  public boolean isPasswordProperty(String service, String type, String propertyName) {
    return (serviceConfigurations.containsKey(service) &&
        serviceConfigurations.get(service).containsKey(type) &&
        serviceConfigurations.get(service).get(type).containsKey(propertyName) &&
        serviceConfigurations.get(service).get(type).get(propertyName).getPropertyTypes().
            contains(PropertyInfo.PropertyType.PASSWORD));
  }

  //todo
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

  public boolean isKerberosPrincipalNameProperty(String service, String type, String propertyName) {
    return (serviceConfigurations.containsKey(service) &&
        serviceConfigurations.get(service).containsKey(type) &&
        serviceConfigurations.get(service).get(type).containsKey(propertyName) &&
        serviceConfigurations.get(service).get(type).get(propertyName).getPropertyTypes().
            contains(PropertyInfo.PropertyType.KERBEROS_PRINCIPAL));
  }
  /**
   * Get config attributes for the specified service and configuration type.
   *
   * @param service  service name
   * @param type   configuration type
   *
   * @return  map of attribute names to map of property names to attribute values
   *      for the specified service and configuration type
   */
  public Map<String, Map<String, String>> getConfigurationAttributes(String service, String type) {
    Map<String, Map<String, String>> attributesMap = new HashMap<>();
    Map<String, ConfigProperty> configProperties = serviceConfigurations.get(service).get(type);
    if (configProperties != null) {
      for (Map.Entry<String, ConfigProperty> configProperty : configProperties.entrySet()) {
        String propertyName = configProperty.getKey();
        Map<String, String> propertyAttributes = configProperty.getValue().getAttributes();
        if (propertyAttributes != null) {
          for (Map.Entry<String, String> propertyAttribute : propertyAttributes.entrySet()) {
            String attributeName = propertyAttribute.getKey();
            String attributeValue = propertyAttribute.getValue();
            if (attributeValue != null) {
              Map<String, String> attributes = getWithEmptyDefault(attributesMap, attributeName);
              attributes.put(propertyName, attributeValue);
            }
          }
        }
      }
    }
    return attributesMap;
  }

  //todo:
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
            Map<String, String> attributes = getWithEmptyDefault(attributesMap, attributeName);
            attributes.put(propertyName, attributeValue);
          }
        }
      }
    }
    return attributesMap;
  }

  /**
   * Get the service for the specified component.
   *
   * @param component  component name
   *
   * @return service name that contains tha specified component
   */
  public String getServiceForComponent(String component) {
    return componentService.get(component);
  }

  /**
   * Get the names of the services which contains the specified components.
   *
   * @param components collection of components
   *
   * @return collection of services which contain the specified components
   */
  public Collection<String> getServicesForComponents(Collection<String> components) {
    Set<String> services = new HashSet<>();
    for (String component : components) {
      services.add(getServiceForComponent(component));
    }

    return services;
  }

  /**
   * Obtain the service name which corresponds to the specified configuration.
   *
   * @param config  configuration type
   *
   * @return name of service which corresponds to the specified configuration type
   */
  public String getServiceForConfigType(String config) {
    for (Map.Entry<String, Map<String, Map<String, ConfigProperty>>> entry : serviceConfigurations.entrySet()) {
      Map<String, Map<String, ConfigProperty>> typeMap = entry.getValue();
      String serviceName = entry.getKey();
      if (typeMap.containsKey(config) && !getExcludedConfigurationTypes(serviceName).contains(config)) {
        return serviceName;
      }
    }
    throw new IllegalArgumentException(
        "Specified configuration type is not associated with any service: " + config);
  }

  /**
   * Return the dependencies specified for the given component.
   *
   * @param component  component to get dependency information for
   *
   * @return collection of dependency information for the specified component
   */
  //todo: full dependency graph
  public Collection<DependencyInfo> getDependenciesForComponent(String component) {
    return dependencies.containsKey(component) ? dependencies.get(component) :
        Collections.emptySet();
  }

  /**
   * Get the service, if any, that a component dependency is conditional on.
   *
   * @param dependency  dependency to get conditional service for
   *
   * @return conditional service for provided component or null if dependency
   *     is not conditional on a service
   */
  public String getConditionalServiceForDependency(DependencyInfo dependency) {
    return dependencyConditionalServiceMap.get(dependency);
  }

  public String getExternalComponentConfig(String component) {
    return dbDependencyInfo.get(component);
  }

  /**
   * Obtain the required cardinality for the specified component.
   */
  public Cardinality getCardinality(String component) {
    return new Cardinality(cardinalityRequirements.get(component));
  }

  /**
   * Obtain auto-deploy information for the specified component.
   */
  public AutoDeployInfo getAutoDeployInfo(String component) {
    return componentAutoDeployInfo.get(component);
  }

  public boolean isMasterComponent(String component) {
    return masterComponents.contains(component);
  }

  public Configuration getConfiguration(Collection<String> services) {
    Map<String, Map<String, Map<String, String>>> attributes = new HashMap<>();
    Map<String, Map<String, String>> properties = new HashMap<>();

    for (String service : services) {
      Collection<String> serviceConfigTypes = getConfigurationTypes(service);
      for (String type : serviceConfigTypes) {
        Map<String, String> typeProps = getWithEmptyDefault(properties, type);
        typeProps.putAll(getConfigurationProperties(service, type));

        Map<String, Map<String, String>> stackTypeAttributes = getConfigurationAttributes(service, type);
        if (!stackTypeAttributes.isEmpty()) {
          if (! attributes.containsKey(type)) {
            attributes.put(type, new HashMap<>());
          }
          Map<String, Map<String, String>> typeAttributes = attributes.get(type);
          for (Map.Entry<String, Map<String, String>> attribute : stackTypeAttributes.entrySet()) {
            String attributeName = attribute.getKey();
            Map<String, String> attributeProps = getWithEmptyDefault(typeAttributes, attributeName);
            attributeProps.putAll(attribute.getValue());
          }
        }
      }
    }
    return new Configuration(properties, attributes);
  }

  public Configuration getConfiguration() {
    Map<String, Map<String, Map<String, String>>> stackAttributes = new HashMap<>();
    Map<String, Map<String, String>> stackConfigs = new HashMap<>();

    for (String service : getServices()) {
      for (String type : getAllConfigurationTypes(service)) {
        Map<String, String> typeProps = getWithEmptyDefault(stackConfigs, type);
        typeProps.putAll(getConfigurationProperties(service, type));

        Map<String, Map<String, String>> stackTypeAttributes = getConfigurationAttributes(service, type);
        if (!stackTypeAttributes.isEmpty()) {
          if (! stackAttributes.containsKey(type)) {
            stackAttributes.put(type, new HashMap<>());
          }
          Map<String, Map<String, String>> typeAttrs = stackAttributes.get(type);
          for (Map.Entry<String, Map<String, String>> attribute : stackTypeAttributes.entrySet()) {
            String attributeName = attribute.getKey();
            Map<String, String> attributes = getWithEmptyDefault(typeAttrs, attributeName);
            attributes.putAll(attribute.getValue());
          }
        }
      }
    }
    return new Configuration(stackConfigs, stackAttributes);
  }

  static <OK, IK, IV> Map<IK, IV> getWithEmptyDefault(Map<OK, Map<IK, IV>> outerMap, OK outerKey) {
    return outerMap.computeIfAbsent(outerKey, __ -> new HashMap<>());
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

    public Set<PropertyDependencyInfo> getDependsOnProperties() {
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