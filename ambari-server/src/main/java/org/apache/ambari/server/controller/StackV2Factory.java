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

package org.apache.ambari.server.controller;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.state.AutoDeployInfo;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.DependencyInfo;
import org.apache.ambari.server.state.StackId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class StackV2Factory {
  private final static Logger LOG = LoggerFactory.getLogger(StackV2Factory.class);


  private final AmbariManagementController controller;
  private final RepositoryVersionDAO repositoryVersionDAO;

  public StackV2Factory(AmbariManagementController controller, RepositoryVersionDAO repositoryVersionDAO) {
    this.controller = controller;
    this.repositoryVersionDAO = repositoryVersionDAO;
  }

  public StackV2 create(StackEntity stack, String repositoryVersion) throws AmbariException {
    return create(stack.getStackName(), stack.getStackVersion(), repositoryVersion);
  }

  public StackV2 create(StackId id, String repositoryVersion) throws AmbariException {
    return create(id.getStackName(), id.getStackVersion(), repositoryVersion);
  }

  public StackV2 create(String name, String version, String repositoryVersion) throws AmbariException {
    Set<StackServiceResponse> stackServices = controller.getStackServices(
      Collections.singleton(new StackServiceRequest(name, version, null)));

    StackData stackData = new StackData(name, version);
    for (StackServiceResponse stackService : stackServices) {
      String serviceName = stackService.getServiceName();
      parseComponents(stackData, serviceName);
      parseExcludedConfigurations(stackData, stackService);
      parseConfigurations(stackData, stackService);
      registerConditionalDependencies(stackData);
    }

    //todo: already done for each service
    parseStackConfigurations(stackData);

    getComponentInfos(stackData);

    stackData.repoVersion = repositoryVersion;
    verifyRepositoryVersion(stackData);

    return new StackV2(name, version, stackData.repoVersion /* TODO */, stackData.serviceComponents, stackData.dependencies,
      stackData.dbDependencyInfo, stackData.componentAutoDeployInfo, stackData.serviceConfigurations,
      stackData.requiredServiceConfigurations, stackData.stackConfigurations, stackData.excludedConfigurationTypes,
      stackData.componentInfos);
  }

  private void verifyRepositoryVersion(StackData stackData) throws AmbariException {
    StackId stackId = new StackId(stackData.stackName, stackData.stackVersion);
    RepositoryVersionEntity entity =
      repositoryVersionDAO.findByStackAndVersion(stackId, stackData.repoVersion);
    Preconditions.checkNotNull(entity, "Repo version %s not found for stack %s", stackData.repoVersion, stackId);
  }

  private void getComponentInfos(StackData stackData) {
    stackData.componentService.forEach( (componentName, serviceName) -> {
      try {
        ComponentInfo componentInfo = controller.getAmbariMetaInfo().getComponent(stackData.stackName, stackData.stackVersion, serviceName, componentName);
        if (null != componentInfo) {
          stackData.componentInfos.put(componentName, componentInfo);
        } else {
          LOG.debug("No component info for service: {}, component: {}, stack name: {}, stack version: {}",
            serviceName, componentName, stackData.stackName, stackData.stackVersion);
        }
      } catch (AmbariException e) {
        LOG.debug("No component info for service: {}, component: {}, stack name: {}, stack version: {}",
          serviceName, componentName, stackData.stackName, stackData.stackVersion, e);
      }
    });
  }

  /**
   * Parse configurations for the specified service from the stack definition.
   *
   * @param stackService  service to parse the stack configuration for
   *
   * @throws AmbariException an exception occurred getting configurations from the stack definition
   */
  private void parseConfigurations(StackData stackData,
                                   StackServiceResponse stackService) throws AmbariException {
    String service = stackService.getServiceName();
    Map<String, Map<String, StackV2.ConfigProperty>> mapServiceConfig = new HashMap<>();
    Map<String, Map<String, StackV2.ConfigProperty>> mapRequiredServiceConfig = new HashMap<>();

    stackData.serviceConfigurations.put(service, mapServiceConfig);
    stackData.requiredServiceConfigurations.put(service, mapRequiredServiceConfig);

    Set<ReadOnlyConfigurationResponse> serviceConfigs = controller.getStackConfigurations(
      Collections.singleton(new StackConfigurationRequest(stackData.stackName, stackData.stackVersion, service, null)));
    Set<ReadOnlyConfigurationResponse> stackLevelConfigs = controller.getStackLevelConfigurations(
      Collections.singleton(new StackLevelConfigurationRequest(stackData.stackName, stackData.stackVersion, null)));
    serviceConfigs.addAll(stackLevelConfigs);

    // shouldn't have any required properties in stack level configuration
    for (ReadOnlyConfigurationResponse config : serviceConfigs) {
      StackV2.ConfigProperty configProperty = new StackV2.ConfigProperty(config);
      String type = configProperty.getType();

      Map<String, StackV2.ConfigProperty> mapTypeConfig = StackV2.getWithEmptyDefault(mapServiceConfig, type);

      mapTypeConfig.put(config.getPropertyName(), configProperty);
      if (config.isRequired()) {
        Map<String, StackV2.ConfigProperty> requiredTypeConfig =
          StackV2.getWithEmptyDefault(mapRequiredServiceConfig, type);
        requiredTypeConfig.put(config.getPropertyName(), configProperty);
      }
    }

    // So far we added only config types that have properties defined
    // in stack service definition. Since there might be config types
    // with no properties defined we need to add those separately
    Set<String> configTypes = stackService.getConfigTypes().keySet();
    for (String configType: configTypes) {
      if (!mapServiceConfig.containsKey(configType)) {
        mapServiceConfig.put(configType, Collections.emptyMap());
      }
    }
  }

  private void parseStackConfigurations (StackData stackData) throws AmbariException {
    Set<ReadOnlyConfigurationResponse> stackLevelConfigs = controller.getStackLevelConfigurations(
      Collections.singleton(new StackLevelConfigurationRequest(stackData.stackName, stackData.stackVersion, null)));

    for (ReadOnlyConfigurationResponse config : stackLevelConfigs) {
      StackV2.ConfigProperty configProperty = new StackV2.ConfigProperty(config);
      String type = configProperty.getType();

      Map<String, StackV2.ConfigProperty> mapTypeConfig =
        StackV2.getWithEmptyDefault(stackData.stackConfigurations, type);

      mapTypeConfig.put(config.getPropertyName(),
        configProperty);
    }
  }

  /**
   * Parse components for the specified service from the stack definition.
   *
   * @param service  service name
   *
   * @throws AmbariException an exception occurred getting components from the stack definition
   */
  private void parseComponents(StackData stackData, String service) throws AmbariException{
    Collection<String> componentSet = new HashSet<>();

    Set<StackServiceComponentResponse> components = controller.getStackComponents(
      Collections.singleton(new StackServiceComponentRequest(stackData.stackName, stackData.stackVersion, service, null)));

    // stack service components
    for (StackServiceComponentResponse component : components) {
      String componentName = component.getComponentName();
      componentSet.add(componentName);
      stackData.componentService.put(componentName, service);
      String cardinality = component.getCardinality();
      if (cardinality != null) {
        stackData.cardinalityRequirements.put(componentName, cardinality);
      }
      AutoDeployInfo autoDeploy = component.getAutoDeploy();
      if (autoDeploy != null) {
        stackData.componentAutoDeployInfo.put(componentName, autoDeploy);
      }

      // populate component dependencies
      //todo: remove usage of AmbariMetaInfo
      Collection<DependencyInfo> componentDependencies = controller.getAmbariMetaInfo().getComponentDependencies(
        stackData.stackName, stackData.stackVersion, service, componentName);

      if (componentDependencies != null && ! componentDependencies.isEmpty()) {
        stackData.dependencies.put(componentName, componentDependencies);
      }
      if (component.isMaster()) {
        stackData.masterComponents.add(componentName);
      }
    }

    stackData.serviceComponents.put(service, componentSet);
  }


  /**
   * Obtain the excluded configuration types from the StackServiceResponse
   *
   * @param stackServiceResponse the response object associated with this stack service
   */
  private void parseExcludedConfigurations(StackData stackData, StackServiceResponse stackServiceResponse) {
    stackData.excludedConfigurationTypes.put(stackServiceResponse.getServiceName(), stackServiceResponse.getExcludedConfigTypes());
  }

  /**
   * Register conditional dependencies.
   */
  //todo: This information should be specified in the stack definition.
  void registerConditionalDependencies(StackData stackData) {
    stackData.dbDependencyInfo.put("MYSQL_SERVER", "global/hive_database");
  }


  private static final class StackData {
    final String stackName;
    final String stackVersion;

    public StackData(String stackName, String stackVersion) {
      this.stackName = stackName;
      this.stackVersion = stackVersion;
    }

    String repoVersion;
    final Map<String, String> componentService = new HashMap<>();
    final Set<String> masterComponents = new HashSet<>();
    final Map<String, AutoDeployInfo> componentAutoDeployInfo = new HashMap<>();
    final Map<String, String> cardinalityRequirements = new HashMap<>();
    final Map<String, Collection<DependencyInfo>> dependencies = new HashMap<>();
    final Map<String, Collection<String>> serviceComponents = new HashMap<>();
    final Map<String, Map<String, Map<String, StackV2.ConfigProperty>>> serviceConfigurations = new HashMap<>();
    final Map<String, Map<String, Map<String, StackV2.ConfigProperty>>> requiredServiceConfigurations = new HashMap<>();
    final Map<String, String> dbDependencyInfo = new HashMap<>();
    final Map<String, Set<String>> excludedConfigurationTypes = new HashMap<>();
    final Map<String, Map<String, StackV2.ConfigProperty>> stackConfigurations = new HashMap<>();
    final Map<String, ComponentInfo> componentInfos = new HashMap<>();
  }
}
