/**
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

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.StackConfigurationRequest;
import org.apache.ambari.server.controller.StackConfigurationResponse;
import org.apache.ambari.server.controller.StackLevelConfigurationRequest;
import org.apache.ambari.server.controller.StackServiceComponentRequest;
import org.apache.ambari.server.controller.StackServiceComponentResponse;
import org.apache.ambari.server.controller.StackServiceRequest;
import org.apache.ambari.server.controller.StackServiceResponse;
import org.apache.ambari.server.state.AutoDeployInfo;
import org.apache.ambari.server.state.DependencyInfo;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulates stack information.
 */
class Stack {
  /**
   * Stack name
   */
  private String name;

  /**
   * Stack version
   */
  private String version;

  /**
   * Map of service name to components
   */
  private Map<String, Collection<String>> serviceComponents =
      new HashMap<String, Collection<String>>();

  /**
   * Map of component to service
   */
  private Map<String, String> componentService = new HashMap<String, String>();

  /**
   * Map of component to dependencies
   */
  private Map<String, Collection<DependencyInfo>> dependencies =
      new HashMap<String, Collection<DependencyInfo>>();

  /**
   * Map of dependency to conditional service
   */
  private Map<DependencyInfo, String> dependencyConditionalServiceMap =
      new HashMap<DependencyInfo, String>();

  /**
   * Map of database component name to configuration property which indicates whether
   * the database in to be managed or if it is an external non-managed instance.
   * If the value of the config property starts with 'New', the database is determined
   * to be managed, otherwise it is non-managed.
   */
  private Map<String, String> dbDependencyInfo = new HashMap<String, String>();

  /**
   * Map of component to required cardinality
   */
  private Map<String, String> cardinalityRequirements = new HashMap<String, String>();

  /**
   * Map of component to auto-deploy information
   */
  private Map<String, AutoDeployInfo> componentAutoDeployInfo =
      new HashMap<String, AutoDeployInfo>();

  /**
   * Map of service to config type properties
   */
  private Map<String, Map<String, Map<String, ConfigProperty>>> serviceConfigurations =
      new HashMap<String, Map<String, Map<String, ConfigProperty>>>();

  /**
   * Map of service to set of excluded config types
   */
  private Map<String, Set<String>> excludedConfigurationTypes =
    new HashMap<String, Set<String>>();


  /**
   * Ambari Management Controller, used to obtain Stack definitions
   */
  private final AmbariManagementController ambariManagementController;

  /**
   * Contains a configuration property's value and attributes.
   */
  private class ConfigProperty {

    private ConfigProperty(String value, Map<String, String> attributes) {
      this.value = value;
      this.attributes = attributes;
    }

    private String value;
    private Map<String, String> attributes;

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }

    public Map<String, String> getAttributes() {
      return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
      this.attributes = attributes;
    }
  }

  /**
   * Constructor.
   *
   * @param name     stack name
   * @param version  stack version
   *
   * @throws org.apache.ambari.server.AmbariException an exception occurred getting stack information
   *                         for the specified name and version
   */
  public Stack(String name, String version, AmbariManagementController ambariManagementController) throws AmbariException {
    this.name = name;
    this.version = version;
    this.ambariManagementController = ambariManagementController;

    Set<StackServiceResponse> stackServices = ambariManagementController.getStackServices(
        Collections.singleton(new StackServiceRequest(name, version, null)));

    for (StackServiceResponse stackService : stackServices) {
      String serviceName = stackService.getServiceName();
      parseComponents(serviceName);
      parseExcludedConfigurations(stackService);
      parseConfigurations(serviceName);
      registerConditionalDependencies();
    }
  }

  /**
   * Obtain stack name.
   *
   * @return stack name
   */
  public String getName() {
    return name;
  }

  /**
   * Obtain stack version.
   *
   * @return stack version
   */
  public String getVersion() {
    return version;
  }


  Map<DependencyInfo, String> getDependencyConditionalServiceMap() {
    return dependencyConditionalServiceMap;
  }

  /**
   * Get services contained in the stack.
   *
   * @return collection of all services for the stack
   */
  public Collection<String> getServices() {
    return serviceComponents.keySet();
  }

  /**
   * Get components contained in the stack for the specified service.
   *
   * @param service  service name
   *
   * @return collection of component names for the specified service
   */
  public Collection<String> getComponents(String service) {
    return serviceComponents.get(service);
  }

  /**
   * Get configuration types for the specified service.
   *
   * @param service  service name
   *
   * @return collection of configuration types for the specified service
   */
  public Collection<String> getConfigurationTypes(String service) {
    return serviceConfigurations.get(service).keySet();
  }

  /**
   * Get the set of excluded configuration types
   *   for this service
   *
   * @param service service name
   *
   * @return Set of names of excluded config types
   */
  public Set<String> getExcludedConfigurationTypes(String service) {
    return excludedConfigurationTypes.get(service);
  }

  /**
   * Get config properties for the specified service and configuration type.
   *
   * @param service  service name
   * @param type     configuration type
   *
   * @return map of property names to values for the specified service and configuration type
   */
  public Map<String, String> getConfigurationProperties(String service, String type) {
    Map<String, String> configMap = new HashMap<String, String>();
    Map<String, ConfigProperty> configProperties = serviceConfigurations.get(service).get(type);
    if (configProperties != null) {
      for (Map.Entry<String, ConfigProperty> configProperty : configProperties.entrySet()) {
        configMap.put(configProperty.getKey(), configProperty.getValue().getValue());
      }
    }
    return configMap;
  }

  /**
   * Get config attributes for the specified service and configuration type.
   *
   * @param service  service name
   * @param type     configuration type
   *
   * @return  map of attribute names to map of property names to attribute values
   *          for the specified service and configuration type
   */
  public Map<String, Map<String, String>> getConfigurationAttributes(String service, String type) {
    Map<String, Map<String, String>> attributesMap = new HashMap<String, Map<String, String>>();
    Map<String, ConfigProperty> configProperties = serviceConfigurations.get(service).get(type);
    if (configProperties != null) {
      for (Map.Entry<String, ConfigProperty> configProperty : configProperties.entrySet()) {
        String propertyName = configProperty.getKey();
        Map<String, String> propertyAttributes = configProperty.getValue().getAttributes();
        if (propertyAttributes != null) {
          for (Map.Entry<String, String> propertyAttribute : propertyAttributes.entrySet()) {
            String attributeName = propertyAttribute.getKey();
            String attributeValue = propertyAttribute.getValue();
            Map<String, String> attributes = attributesMap.get(attributeName);
            if (attributes == null) {
                attributes = new HashMap<String, String>();
                attributesMap.put(attributeName, attributes);
            }
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
    Set<String> services = new HashSet<String>();
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
      if (typeMap.containsKey(config)) {
        return entry.getKey();
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
        Collections.<DependencyInfo>emptySet();
  }

  /**
   * Get the service, if any, that a component dependency is conditional on.
   *
   * @param dependency  dependency to get conditional service for
   *
   * @return conditional service for provided component or null if dependency
   *         is not conditional on a service
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

  /**
   * Parse components for the specified service from the stack definition.
   *
   * @param service  service name
   *
   * @throws org.apache.ambari.server.AmbariException an exception occurred getting components from the stack definition
   */
  private void parseComponents(String service) throws AmbariException{
    Collection<String> componentSet = new HashSet<String>();

    Set<StackServiceComponentResponse> components = ambariManagementController.getStackComponents(
        Collections.singleton(new StackServiceComponentRequest(name, version, service, null)));

    // stack service components
    for (StackServiceComponentResponse component : components) {
      String componentName = component.getComponentName();
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
      Collection<DependencyInfo> componentDependencies = BaseBlueprintProcessor.stackInfo.getComponentDependencies(
          name, version, service, componentName);

      if (componentDependencies != null && ! componentDependencies.isEmpty()) {
        dependencies.put(componentName, componentDependencies);
      }
    }
    this.serviceComponents.put(service, componentSet);
  }

  /**
   * Obtain the excluded configuration types from the StackServiceResponse
   *
   * @param stackServiceResponse the response object associated with this stack service
   */
  private void parseExcludedConfigurations(StackServiceResponse stackServiceResponse) {
    excludedConfigurationTypes.put(stackServiceResponse.getServiceName(), stackServiceResponse.getExcludedConfigTypes());
  }

  /**
   * Parse configurations for the specified service from the stack definition.
   *
   * @param service  service name
   *
   * @throws org.apache.ambari.server.AmbariException an exception occurred getting configurations from the stack definition
   */
  private void parseConfigurations(String service) throws AmbariException {
    Map<String, Map<String, ConfigProperty>> mapServiceConfig = new HashMap<String, Map<String, ConfigProperty>>();

    serviceConfigurations.put(service, mapServiceConfig);

    Set<StackConfigurationResponse> serviceConfigs = ambariManagementController.getStackConfigurations(
        Collections.singleton(new StackConfigurationRequest(name, version, service, null)));
    Set<StackConfigurationResponse> stackLevelConfigs = ambariManagementController.getStackLevelConfigurations(
        Collections.singleton(new StackLevelConfigurationRequest(name, version, null)));
    serviceConfigs.addAll(stackLevelConfigs);

    for (StackConfigurationResponse config : serviceConfigs) {
      String type = config.getType();
      //strip .xml from type
      if (type.endsWith(".xml")) {
        type = type.substring(0, type.length() - 4);
      }
      Map<String, ConfigProperty> mapTypeConfig = mapServiceConfig.get(type);
      if (mapTypeConfig == null) {
        mapTypeConfig = new HashMap<String, ConfigProperty>();
        mapServiceConfig.put(type, mapTypeConfig);
      }
      mapTypeConfig.put(config.getPropertyName(),
          new ConfigProperty(config.getPropertyValue(), config.getPropertyAttributes()));
    }
  }

  /**
   * Register conditional dependencies.
   */
  //todo: This information should be specified in the stack definition.
  void registerConditionalDependencies() {
    Collection<DependencyInfo> nagiosDependencies = getDependenciesForComponent("NAGIOS_SERVER");
    for (DependencyInfo dependency : nagiosDependencies) {
      if (dependency.getComponentName().equals("HCAT")) {
        dependencyConditionalServiceMap.put(dependency, "HIVE");
      } else if (dependency.getComponentName().equals("OOZIE_CLIENT")) {
        dependencyConditionalServiceMap.put(dependency, "OOZIE");
      } else if (dependency.getComponentName().equals("YARN_CLIENT")) {
        dependencyConditionalServiceMap.put(dependency, "YARN");
      } else if (dependency.getComponentName().equals("TEZ_CLIENT")) {
        dependencyConditionalServiceMap.put(dependency, "TEZ");
      } else if (dependency.getComponentName().equals("MAPREDUCE2_CLIENT")) {
        dependencyConditionalServiceMap.put(dependency, "MAPREDUCE2");
      }
    }
    dbDependencyInfo.put("MYSQL_SERVER", "global/hive_database");
  }
}
