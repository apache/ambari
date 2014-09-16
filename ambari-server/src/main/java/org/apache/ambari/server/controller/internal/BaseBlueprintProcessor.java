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

import com.google.gson.Gson;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.StackAccessException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.StackConfigurationRequest;
import org.apache.ambari.server.controller.StackConfigurationResponse;
import org.apache.ambari.server.controller.StackLevelConfigurationRequest;
import org.apache.ambari.server.controller.StackServiceComponentRequest;
import org.apache.ambari.server.controller.StackServiceComponentResponse;
import org.apache.ambari.server.controller.StackServiceRequest;
import org.apache.ambari.server.controller.StackServiceResponse;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.orm.dao.BlueprintDAO;
import org.apache.ambari.server.orm.entities.BlueprintConfigEntity;
import org.apache.ambari.server.orm.entities.BlueprintEntity;
import org.apache.ambari.server.orm.entities.HostGroupComponentEntity;
import org.apache.ambari.server.orm.entities.HostGroupConfigEntity;
import org.apache.ambari.server.orm.entities.HostGroupEntity;
import org.apache.ambari.server.state.AutoDeployInfo;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.DependencyInfo;
import org.apache.ambari.server.state.PropertyInfo;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Base blueprint processing resource provider.
 */
//todo: this class needs to be refactored to a ClusterTopology class which
//todo: has hostgroup, stack and configuration state specific to a deployment.
public abstract class BaseBlueprintProcessor extends AbstractControllerResourceProvider {

  /**
   * Data access object used to obtain blueprint entities.
   */
  protected static BlueprintDAO blueprintDAO;

  /**
   * Stack related information.
   */
  protected static AmbariMetaInfo stackInfo;
  
  protected static ConfigHelper configHelper;


  protected BaseBlueprintProcessor(Set<String> propertyIds,
                                   Map<Resource.Type, String> keyPropertyIds,
                                   AmbariManagementController managementController) {

    super(propertyIds, keyPropertyIds, managementController);
  }

  /**
   * Get host groups which contain a component.
   *
   * @param component   component name
   * @param hostGroups  collection of host groups to check
   *
   * @return collection of host groups which contain the specified component
   */
  protected Collection<HostGroupImpl> getHostGroupsForComponent(String component, Collection<HostGroupImpl> hostGroups) {
    Collection<HostGroupImpl> resultGroups = new HashSet<HostGroupImpl>();
    for (HostGroupImpl group : hostGroups ) {
      if (group.getComponents().contains(component)) {
        resultGroups.add(group);
      }
    }
    return resultGroups;
  }

  /**
   * Parse blueprint host groups.
   *
   * @param blueprint  associated blueprint
   * @param stack      associated stack
   *
   * @return map of host group name to host group
   */
  protected Map<String, HostGroupImpl> parseBlueprintHostGroups(BlueprintEntity blueprint, Stack stack) {
    Map<String, HostGroupImpl> mapHostGroups = new HashMap<String, HostGroupImpl>();

    for (HostGroupEntity hostGroup : blueprint.getHostGroups()) {
      mapHostGroups.put(hostGroup.getName(), new HostGroupImpl(hostGroup, stack, this));
    }
    return mapHostGroups;
  }

  /**
   * Parse stack information.
   *
   * @param blueprint  associated blueprint
   *
   * @return stack instance
   *
   * @throws SystemException an unexpected exception occurred
   */
  protected Stack parseStack(BlueprintEntity blueprint) throws SystemException {
    Stack stack;
    try {
      stack = new Stack(blueprint.getStackName(), blueprint.getStackVersion(), getManagementController());
    } catch (StackAccessException e) {
      throw new IllegalArgumentException("Invalid stack information provided for cluster.  " +
          "stack name: " + blueprint.getStackName() +
          " stack version: " + blueprint.getStackVersion());
    } catch (AmbariException e) {
      throw new SystemException("Unable to obtain stack information.", e);
    }
    return stack;
  }

  /**
   * Validate blueprint topology.
   * An exception is thrown in the case of validation failure.
   * For missing components which are auto-deploy enabled, these are added to the topology which is reflected
   * in the blueprint entity that is returned.
   *
   * @param blueprint  blueprint to validate
   *
   * @return blueprint entity which may have been updated as a result of auto-deployment of components.
   *
   * @throws AmbariException an unexpected error occurred
   * @throws IllegalArgumentException when validation fails
   */
  protected BlueprintEntity validateTopology(BlueprintEntity blueprint) throws AmbariException {
    Stack stack = new Stack(blueprint.getStackName(), blueprint.getStackVersion(), getManagementController());
    Map<String, HostGroupImpl> hostGroupMap = parseBlueprintHostGroups(blueprint, stack);
    Collection<HostGroupImpl> hostGroups = hostGroupMap.values();
    Map<String, Map<String, String>> clusterConfig = processBlueprintConfigurations(blueprint, null);
    Map<String, Map<String, Collection<DependencyInfo>>> missingDependencies =
        new HashMap<String, Map<String, Collection<DependencyInfo>>>();

    Collection<String> services = getTopologyServices(hostGroups);
    for (HostGroupImpl group : hostGroups) {
      Map<String, Collection<DependencyInfo>> missingGroupDependencies =
          group.validateTopology(hostGroups, services, clusterConfig);
      if (! missingGroupDependencies.isEmpty()) {
        missingDependencies.put(group.getEntity().getName(), missingGroupDependencies);
      }
    }

    Collection<String> cardinalityFailures = new HashSet<String>();
    for (String service : services) {
      for (String component : stack.getComponents(service)) {
        Cardinality cardinality = stack.getCardinality(component);
        AutoDeployInfo autoDeploy = stack.getAutoDeployInfo(component);
        if (cardinality.isAll()) {
          cardinalityFailures.addAll(verifyComponentInAllHostGroups(
              blueprint, hostGroups, component, autoDeploy));
        } else {
          cardinalityFailures.addAll(verifyComponentCardinalityCount(
              blueprint, hostGroups, component, cardinality, autoDeploy, stack, clusterConfig));
        }
      }
    }

    if (! missingDependencies.isEmpty() || ! cardinalityFailures.isEmpty()) {
      generateInvalidTopologyException(missingDependencies, cardinalityFailures);
    }

    return blueprint;
  }

  /**
   * Process cluster scoped configurations contained in blueprint.
   *
   * @param blueprint  blueprint entity
   *
   * @return cluster scoped properties contained within in blueprint
   */
  protected Map<String, Map<String, String>> processBlueprintConfigurations(
      BlueprintEntity blueprint, Collection<Map<String, String>> configOverrides) {

    Map<String, Map<String, String>> mapConfigurations = new HashMap<String, Map<String, String>>();
    Collection<BlueprintConfigEntity> configs = blueprint.getConfigurations();
    Gson jsonSerializer = new Gson();

    for (BlueprintConfigEntity config : configs) {
      mapConfigurations.put(config.getType(), jsonSerializer.<Map<String, String>> fromJson(
          config.getConfigData(), Map.class));
    }
    overrideExistingProperties(mapConfigurations, configOverrides);

    return mapConfigurations;
  }

  /**
   * Process cluster scoped configuration attributes contained in blueprint.
   *
   * @param blueprint  blueprint entity
   *
   * @return cluster scoped property attributes contained within in blueprint
   */
  protected Map<String, Map<String, Map<String, String>>> processBlueprintAttributes(BlueprintEntity blueprint) {

    Map<String, Map<String, Map<String, String>>> mapAttributes =
        new HashMap<String, Map<String, Map<String, String>>>();
    Collection<BlueprintConfigEntity> configs = blueprint.getConfigurations();

    if (configs != null) {
      Gson gson = new Gson();
      for (BlueprintConfigEntity config : configs) {
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
   * Override existing properties or add new.
   *
   * @param existingProperties  current property values
   * @param configOverrides     override properties
   */
  protected void overrideExistingProperties(Map<String, Map<String, String>> existingProperties,
                                            Collection<Map<String, String>> configOverrides) {
    if (configOverrides != null) {
      for (Map<String, String> properties : configOverrides) {
        String category = null;
        int propertyOffset = -1;
        for (Map.Entry<String, String> entry : properties.entrySet()) {
          String absolutePropName = entry.getKey();
          if (category == null) {
            propertyOffset =  absolutePropName.indexOf('/');
            category = absolutePropName.substring(0, propertyOffset);
          }
          Map<String, String> existingCategoryProperties = existingProperties.get(category);
          if (existingCategoryProperties == null) {
            existingCategoryProperties = new HashMap<String, String>();
            existingProperties.put(category, existingCategoryProperties);
          }
          //override existing property or add new
          existingCategoryProperties.put(absolutePropName.substring(propertyOffset + 1), entry.getValue());
        }
      }
    }
  }

  /**
   * Add a new component entity to a host group entity.
   *
   * @param blueprint  blueprint entity
   * @param hostGroup  host group name
   * @param component  name of component which is being added
   */
  protected void addComponentToBlueprint(BlueprintEntity blueprint, String hostGroup, String component) {
    HostGroupComponentEntity componentEntity = new HostGroupComponentEntity();
    componentEntity.setBlueprintName(blueprint.getBlueprintName());
    componentEntity.setName(component);

    for (HostGroupEntity hostGroupEntity : blueprint.getHostGroups()) {
      if (hostGroupEntity.getName().equals(hostGroup))  {
        componentEntity.setHostGroupEntity(hostGroupEntity);
        componentEntity.setHostGroupName(hostGroupEntity.getName());
        hostGroupEntity.addComponent(componentEntity);
        break;
      }
    }
  }

  /**
   * Obtain a blueprint entity based on name.
   *
   * @param blueprintName  name of blueprint to obtain
   *
   * @return blueprint entity for the given name
   * @throws IllegalArgumentException no blueprint with the given name found
   */
  protected BlueprintEntity getExistingBlueprint(String blueprintName) {
    BlueprintEntity blueprint = blueprintDAO.findByName(blueprintName);
    if (blueprint == null) {
      throw new IllegalArgumentException("Specified blueprint doesn't exist: " + blueprintName);
    }
    return blueprint;
  }

  /**
   * Get all services provided in topology.
   *
   * @param hostGroups  all host groups in topology
   *
   * @return collections of all services provided by topology
   */
  protected Collection<String> getTopologyServices(Collection<HostGroupImpl> hostGroups) {
    Collection<String> services = new HashSet<String>();
    for (HostGroupImpl group : hostGroups) {
      services.addAll(group.getServices());
    }
    return services;
  }

  /**
   * Determine if a component is managed, meaning that it is running inside of the cluster
   * topology.  Generally, non-managed dependencies will be database components.
   *
   * @param stack          stack instance
   * @param component      component to determine if it is managed
   * @param clusterConfig  cluster configuration
   *
   * @return true if the specified component managed by the cluster; false otherwise
   */
  protected boolean isDependencyManaged(Stack stack, String component, Map<String, Map<String, String>> clusterConfig) {
    boolean isManaged = true;
    String externalComponentConfig = stack.getExternalComponentConfig(component);
    if (externalComponentConfig != null) {
      String[] toks = externalComponentConfig.split("/");
      String externalComponentConfigType = toks[0];
      String externalComponentConfigProp = toks[1];
      Map<String, String> properties = clusterConfig.get(externalComponentConfigType);
      if (properties != null && properties.containsKey(externalComponentConfigProp)) {
        if (properties.get(externalComponentConfigProp).startsWith("Existing")) {
          isManaged = false;
        }
      }
    }
    return isManaged;
  }

  /**
   * Verify that a component meets cardinality requirements.  For components that are
   * auto-install enabled, will add component to topology if needed.
   *
   * @param blueprint    blueprint instance
   * @param hostGroups   collection of host groups
   * @param component    component to validate
   * @param cardinality  required cardinality
   * @param autoDeploy   auto-deploy information for component
   *
   * @return collection of missing component information
   */
  private Collection<String> verifyComponentCardinalityCount(BlueprintEntity blueprint,
                                                             Collection<HostGroupImpl> hostGroups,
                                                             String component,
                                                             Cardinality cardinality,
                                                             AutoDeployInfo autoDeploy,
                                                             Stack stack,
                                                             Map<String, Map<String, String>> clusterConfig) {

    Collection<String> cardinalityFailures = new HashSet<String>();

    int actualCount = getHostGroupsForComponent(component, hostGroups).size();
    if (! cardinality.isValidCount(actualCount)) {
      boolean validated = ! isDependencyManaged(stack, component, clusterConfig);
      if (! validated && autoDeploy != null && autoDeploy.isEnabled() && cardinality.supportsAutoDeploy()) {
        String coLocateName = autoDeploy.getCoLocate();
        if (coLocateName != null && ! coLocateName.isEmpty()) {
          Collection<HostGroupImpl> coLocateHostGroups = getHostGroupsForComponent(
              coLocateName.split("/")[1], hostGroups);
          if (! coLocateHostGroups.isEmpty()) {
            validated = true;
            HostGroupImpl group = coLocateHostGroups.iterator().next();
            if (group.addComponent(component)) {
              addComponentToBlueprint(blueprint, group.getEntity().getName(), component);
            }
          }
        }
      }
      if (! validated) {
        cardinalityFailures.add(component + "(actual=" + actualCount + ", required=" +
            cardinality.cardinality + ")");
      }
    }
    return cardinalityFailures;
  }

  /**
   * Verify that a component is included in all host groups.
   * For components that are auto-install enabled, will add component to topology if needed.
   *
   * @param blueprint   blueprint instance
   * @param hostGroups  collection of host groups
   * @param component   component to validate
   * @param autoDeploy  auto-deploy information for component
   *
   * @return collection of missing component information
   */
  private Collection<String> verifyComponentInAllHostGroups(BlueprintEntity blueprint,
                                                            Collection<HostGroupImpl> hostGroups,
                                                            String component,
                                                            AutoDeployInfo autoDeploy) {

    Collection<String> cardinalityFailures = new HashSet<String>();
    int actualCount = getHostGroupsForComponent(component, hostGroups).size();
    if (actualCount != hostGroups.size()) {
      if (autoDeploy != null && autoDeploy.isEnabled()) {
        for (HostGroupImpl group : hostGroups) {
          if (group.addComponent(component)) {
            addComponentToBlueprint(blueprint, group.getEntity().getName(), component);
          }
        }
      } else {
        cardinalityFailures.add(component + "(actual=" + actualCount + ", required=ALL)");
      }
    }
    return cardinalityFailures;
  }

  /**
   * Generate an exception for topology validation failure.
   *
   * @param missingDependencies  missing dependency information
   * @param cardinalityFailures  missing service component information
   *
   * @throws IllegalArgumentException  Always thrown and contains information regarding the topology validation failure
   *                                   in the msg
   */
  private void generateInvalidTopologyException(Map<String, Map<String, Collection<DependencyInfo>>> missingDependencies,
                                                Collection<String> cardinalityFailures) {

    String msg = "Cluster Topology validation failed.";
    if (! cardinalityFailures.isEmpty()) {
      msg += "  Invalid service component count: " + cardinalityFailures;
    }
    if (! missingDependencies.isEmpty()) {
      msg += "  Unresolved component dependencies: " + missingDependencies;
    }
    msg += ".  To disable topology validation and create the blueprint, " +
           "add the following to the end of the url: '?validate_topology=false'";
    throw new IllegalArgumentException(msg);
  }


  // ----- Inner Classes -----------------------------------------------------

  /**
   * Encapsulates stack information.
   */
  protected static class Stack {
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
     * @throws AmbariException an exception occurred getting stack information
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
     * @throws AmbariException an exception occurred getting components from the stack definition
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
        Collection<DependencyInfo> componentDependencies = stackInfo.getComponentDependencies(
            name, version, service, componentName);

        if (componentDependencies != null && ! componentDependencies.isEmpty()) {
          dependencies.put(componentName, componentDependencies);
        }
      }
      this.serviceComponents.put(service, componentSet);
    }

    /**
     * Parse configurations for the specified service from the stack definition.
     *
     * @param service  service name
     *
     * @throws AmbariException an exception occurred getting configurations from the stack definition
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

  /**
   * Host group representation.
   */
  protected static class HostGroupImpl implements HostGroup {
    /**
     * Host group entity
     */
    private HostGroupEntity hostGroup;

    /**
     * Components contained in the host group
     */
    private Collection<String> components = new HashSet<String>();

    /**
     * Hosts contained associated with the host group
     */
    private Collection<String> hosts = new HashSet<String>();

    /**
     * Map of service to components for the host group
     */
    private Map<String, Set<String>> componentsForService = new HashMap<String, Set<String>>();

    /**
     * Map of host group configurations.
     * Type -> Map<Key, Val>
     */
    private Map<String, Map<String, String>> configurations =
        new HashMap<String, Map<String, String>>();

    /**
     * Associated stack
     */
    private Stack stack;

    /**
     * The Blueprint processor associated with this HostGroupImpl instance
     */
    private final BaseBlueprintProcessor blueprintProcessor;

    /**
     * Constructor.
     *
     * @param hostGroup  host group
     * @param stack      stack
     */
    public HostGroupImpl(HostGroupEntity hostGroup, Stack stack, BaseBlueprintProcessor blueprintProcessor) {
      this.hostGroup = hostGroup;
      this.stack = stack;
      this.blueprintProcessor = blueprintProcessor;
      parseComponents();
      parseConfigurations();
    }

    @Override
    public String getName() {
      return hostGroup.getName();
    }

    @Override
    public Collection<String> getComponents() {
      return this.components;
    }

    @Override
    public Collection<String> getHostInfo() {
      return this.hosts;
    }

    /**
     * Associate a host with the host group.
     *
     * @param fqdn  fully qualified domain name of the host being added
     */
    public void addHostInfo(String fqdn) {
      this.hosts.add(fqdn);
    }

    /**
     * Get the services which are deployed to this host group.
     *
     * @return collection of services which have components in this host group
     */
    public Collection<String> getServices() {
      return componentsForService.keySet();
    }

    /**
     * Add a component to the host group.
     *
     * @param component  component to add
     *
     * @return true if component was added; false if component already existed
     */
    public boolean addComponent(String component) {
      boolean added = components.add(component);
      if (added) {
        String service = stack.getServiceForComponent(component);
        if (service != null) {
          // an example of a component without a service in the stack is AMBARI_SERVER
          Set<String> serviceComponents = componentsForService.get(service);
          if (serviceComponents == null) {
            serviceComponents = new HashSet<String>();
            componentsForService.put(service, serviceComponents);
          }
          serviceComponents.add(component);
        }
      }
      return added;
    }

    /**
     * Get the components for the specified service which are associated with the host group.
     *
     * @param service  service name
     *
     * @return set of component names
     */
    public Collection<String> getComponents(String service) {
      return componentsForService.get(service);
    }

    /**
     * Get the configurations associated with the host group.
     *
     * @return map of configuration type to a map of properties
     */
    public Map<String, Map<String, String>> getConfigurationProperties() {
      return configurations;
    }

    /**
     * Get the associated entity.
     *
     * @return  associated host group entity
     */
    public HostGroupEntity getEntity() {
      return hostGroup;
    }

    /**
     * Validate host group topology. This includes ensuring that all component dependencies are satisfied.
     *
     * @param hostGroups     collection of all host groups
     * @param services       set of services in cluster topology
     * @param clusterConfig  cluster configuration
     *
     * @return map of component to missing dependencies
     */
    public Map<String, Collection<DependencyInfo>> validateTopology(Collection<HostGroupImpl> hostGroups,
                                                                    Collection<String> services,
                                                                    Map<String, Map<String, String>> clusterConfig) {

      Map<String, Collection<DependencyInfo>> missingDependencies =
          new HashMap<String, Collection<DependencyInfo>>();

      for (String component : new HashSet<String>(components)) {
        Collection<DependencyInfo> dependenciesForComponent = stack.getDependenciesForComponent(component);
        for (DependencyInfo dependency : dependenciesForComponent) {
          String conditionalService = stack.getConditionalServiceForDependency(dependency);
          if (conditionalService != null && ! services.contains(conditionalService)) {
            continue;
          }

          BlueprintEntity   entity          = hostGroup.getBlueprintEntity();
          String            dependencyScope = dependency.getScope();
          String            componentName   = dependency.getComponentName();
          AutoDeployInfo    autoDeployInfo  = dependency.getAutoDeploy();
          boolean           resolved        = false;

          if (dependencyScope.equals("cluster")) {
            Collection<String> missingDependencyInfo = blueprintProcessor.verifyComponentCardinalityCount(entity, hostGroups,
                componentName, new Cardinality("1+"), autoDeployInfo, stack, clusterConfig);
            resolved = missingDependencyInfo.isEmpty();
          } else if (dependencyScope.equals("host")) {
            if (components.contains(component) || (autoDeployInfo != null && autoDeployInfo.isEnabled())) {
              resolved = true;
              if (addComponent(componentName)) {
                blueprintProcessor.addComponentToBlueprint(hostGroup.getBlueprintEntity(), getEntity().getName(), componentName);
              }
            }
          }

          if (! resolved) {
            Collection<DependencyInfo> missingCompDependencies = missingDependencies.get(component);
            if (missingCompDependencies == null) {
              missingCompDependencies = new HashSet<DependencyInfo>();
              missingDependencies.put(component, missingCompDependencies);
            }
            missingCompDependencies.add(dependency);
          }
        }
      }
      return missingDependencies;
    }

    /**
     * Parse component information.
     */
    private void parseComponents() {
      for (HostGroupComponentEntity componentEntity : hostGroup.getComponents() ) {
        addComponent(componentEntity.getName());
      }
    }

    /**
     * Parse host group configurations.
     */
    private void parseConfigurations() {
      Gson jsonSerializer = new Gson();
      for (HostGroupConfigEntity configEntity : hostGroup.getConfigurations()) {
        String type = configEntity.getType();
        Map<String, String> typeProperties = configurations.get(type);
        if ( typeProperties == null) {
          typeProperties = new HashMap<String, String>();
          configurations.put(type, typeProperties);
        }
        Map<String, String> propertyMap =  jsonSerializer.<Map<String, String>>fromJson(
            configEntity.getConfigData(), Map.class);

        if (propertyMap != null) {
          typeProperties.putAll(propertyMap);
        }
      }
    }
  }

  /**
   * Component cardinality representation.
   */
  protected static class Cardinality {
    String cardinality;
    int min = 0;
    int max = Integer.MAX_VALUE;
    int exact = -1;
    boolean isAll = false;

    public Cardinality(String cardinality) {
      this.cardinality = cardinality;
      if (cardinality != null && ! cardinality.isEmpty()) {
        if (cardinality.contains("+")) {
          min = Integer.valueOf(cardinality.split("\\+")[0]);
        } else if (cardinality.contains("-")) {
          String[] toks = cardinality.split("-");
          min = Integer.parseInt(toks[0]);
          max = Integer.parseInt(toks[1]);
        } else if (cardinality.equals("ALL")) {
          isAll = true;
        } else {
          exact = Integer.parseInt(cardinality);
        }
      }
    }

    /**
     * Determine if component is required for all host groups.
     *
     * @return true if cardinality is 'ALL', false otherwise
     */
    public boolean isAll() {
      return isAll;
    }

    /**
     * Determine if the given count satisfies the required cardinality.
     *
     * @param count  number of host groups containing component
     *
     * @return true id count satisfies the required cardinality, false otherwise
     */
    public boolean isValidCount(int count) {
      if (isAll) {
        return false;
      } else if (exact != -1) {
        return count == exact;
      } else return count >= min && count <= max;
    }

    /**
     * Determine if the cardinality count supports auto-deployment.
     * This determination is independent of whether the component is configured
     * to be auto-deployed.  This only indicates whether auto-deployment is
     * supported for the current cardinality.
     *
     * At this time, only cardinalities of ALL or where a count of 1 is valid are
     * supported.
     *
     * @return true if cardinality supports auto-deployment
     */
    public boolean supportsAutoDeploy() {
      return isValidCount(1) || isAll;
    }
  }
}
