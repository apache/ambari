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

package org.apache.ambari.server.stack;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.CustomCommandDefinition;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.ServiceInfo;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Service module which provides all functionality related to parsing and fully
 * resolving services from the stack definition.
 */
public class ServiceModule extends BaseModule<ServiceModule, ServiceInfo> {
  /**
   * Corresponding service info
   */
  private ServiceInfo serviceInfo;

  /**
   * Context which provides modules access to external functionality
   */
  private StackContext stackContext;

  /**
   * Map of child configuration modules keyed by configuration type
   */
  private Map<String, ConfigurationModule> configurationModules =
      new HashMap<String, ConfigurationModule>();

  /**
   * Map of child component modules keyed by component name
   */
  private Map<String, ComponentModule> componentModules =
      new HashMap<String, ComponentModule>();

  /**
   * Encapsulates IO operations on service directory
   */
  private ServiceDirectory serviceDirectory;


  /**
   * Constructor.
   *
   * @param stackContext      stack context which provides module access to external functionality
   * @param serviceInfo       associated service info
   * @param serviceDirectory  used for all IO interaction with service directory in stack definition
   */
  public ServiceModule(StackContext stackContext, ServiceInfo serviceInfo, ServiceDirectory serviceDirectory) {
    this.serviceInfo = serviceInfo;
    this.stackContext = stackContext;
    this.serviceDirectory = serviceDirectory;

    serviceInfo.setMetricsFile(serviceDirectory.getMetricsFile());
    serviceInfo.setAlertsFile(serviceDirectory.getAlertsFile());
    serviceInfo.setSchemaVersion(AmbariMetaInfo.SCHEMA_VERSION_2);
    serviceInfo.setServicePackageFolder(serviceDirectory.getPackageDir());

    populateComponentModules();
    populateConfigurationModules();
  }

  @Override
  public ServiceInfo getModuleInfo() {
    return serviceInfo;
  }

  @Override
  public void resolve(ServiceModule parentModule, Map<String, StackModule> allStacks) throws AmbariException {
    ServiceInfo parent = parentModule.getModuleInfo();

    if (serviceInfo.getComment() == null) {
      serviceInfo.setComment(parent.getComment());
    }
    if (serviceInfo.getDisplayName() == null) {
      serviceInfo.setDisplayName(parent.getDisplayName());
    }

    if (serviceInfo.getRequiredServices() == null) {
      serviceInfo.setRequiredServices(parent.getRequiredServices() != null ?
          parent.getRequiredServices() :
          Collections.<String>emptyList());
    }

    if (serviceInfo.isRestartRequiredAfterChange() == null) {
      serviceInfo.setRestartRequiredAfterChange(parent.isRestartRequiredAfterChange());
    }
    if (serviceInfo.isMonitoringService() == null) {
      serviceInfo.setMonitoringService(parent.isMonitoringService());
    }
    if (serviceInfo.getOsSpecifics().isEmpty() ) {
      serviceInfo.setOsSpecifics(parent.getOsSpecifics());
    }
    if (serviceInfo.getCommandScript() == null) {
      serviceInfo.setCommandScript(parent.getCommandScript());
    }
    if (serviceInfo.getServicePackageFolder() == null) {
      serviceInfo.setServicePackageFolder(parent.getServicePackageFolder());
    }
    if (serviceInfo.getMetricsFile() == null) {
      serviceInfo.setMetricsFile(parent.getMetricsFile());
    }
    if (serviceInfo.getAlertsFile() == null) {
      serviceInfo.setAlertsFile(parent.getAlertsFile());
    }

    mergeCustomCommands(parent.getCustomCommands(), serviceInfo.getCustomCommands());
    mergeConfigDependencies(parent);
    mergeComponents(parentModule, allStacks);
    mergeConfigurations(parentModule, allStacks);
  }

  @Override
  public boolean isDeleted() {
    return serviceInfo.isDeleted();
  }

  @Override
  public String getId() {
    return serviceInfo.getName();
  }

  @Override
  public void finalizeModule() {
    finalizeChildModules(configurationModules.values());
    finalizeChildModules(componentModules.values());
    finalizeConfiguration();
    if(serviceInfo.getCommandScript() != null && ! isDeleted()) {
      stackContext.registerServiceCheck(getId());
    }
  }

  /**
   * Parse and populate child component modules.
   */
  private void populateComponentModules() {
    for (ComponentInfo component : serviceInfo.getComponents()) {
      componentModules.put(component.getName(), new ComponentModule(component));
    }
  }

  /**
   * Parse and populate child configuration modules.
   */
  private void populateConfigurationModules() {
    ConfigurationDirectory configDirectory = serviceDirectory.getConfigurationDirectory(
        serviceInfo.getConfigDir());

    if (configDirectory != null) {
      for (ConfigurationModule config : configDirectory.getConfigurationModules()) {
        if (! serviceInfo.getExcludedConfigTypes().contains(config.getConfigType())) {
          ConfigurationInfo info = config.getModuleInfo();
          serviceInfo.getProperties().addAll(info.getProperties());
          serviceInfo.setTypeAttributes(config.getConfigType(), info.getAttributes());
          configurationModules.put(config.getConfigType(), config);
        }
      }

      for (String excludedType : serviceInfo.getExcludedConfigTypes()) {
        if (! configurationModules.containsKey(excludedType)) {
          ConfigurationInfo configInfo = new ConfigurationInfo(
              Collections.<PropertyInfo>emptyList(), Collections.<String, String>emptyMap());
          ConfigurationModule config = new ConfigurationModule(excludedType, configInfo);

          config.setDeleted(true);
          configurationModules.put(excludedType, config);
        }
      }
    }
  }

  /**
   * Merge configuration dependencies with parent.  Child values override parent values.
   *
   * @param parent  parent service module
   */
  private void mergeConfigDependencies(ServiceInfo parent) {
    //currently there is no way to remove an inherited config dependency
    List<String> configDependencies = serviceInfo.getConfigDependencies();
    List<String> parentConfigDependencies = parent.getConfigDependencies() != null ?
        parent.getConfigDependencies() : Collections.<String>emptyList();

    if (configDependencies == null) {
      serviceInfo.setConfigDependencies(parentConfigDependencies);
    } else {
      for (String parentDependency : parentConfigDependencies) {
        if (! configDependencies.contains(parentDependency)) {
          configDependencies.add(parentDependency);
        }
      }
    }
  }

  /**
   * Merge configurations with the parent configurations.
   * This will update the child configuration module set as well as the underlying info instances.
   *
   * @param parent  parent service module
   * @param stacks  all stack modules
   */
  private void mergeConfigurations(ServiceModule parent, Map<String, StackModule> stacks) throws AmbariException {
    serviceInfo.getProperties().clear();
    serviceInfo.setAllConfigAttributes(new HashMap<String, Map<String, Map<String, String>>>());

    Collection<ConfigurationModule> mergedModules = mergeChildModules(
        stacks, configurationModules, parent.configurationModules);

    for (ConfigurationModule module : mergedModules) {
      configurationModules.put(module.getId(), module);
      serviceInfo.getProperties().addAll(module.getModuleInfo().getProperties());
      serviceInfo.setTypeAttributes(module.getConfigType(), module.getModuleInfo().getAttributes());
    }
  }

  /**
   * Merge components with the parent configurations.
   * This will update the child component module set as well as the underlying info instances.
   */
  private void mergeComponents(ServiceModule parent, Map<String, StackModule> stacks) throws AmbariException {
    serviceInfo.getComponents().clear();
    Collection<ComponentModule> mergedModules = mergeChildModules(
        stacks, componentModules, parent.componentModules);

    for (ComponentModule module : mergedModules) {
      componentModules.put(module.getId(), module);
      serviceInfo.getComponents().add(module.getModuleInfo());
    }
  }

  /**
   * Merge custom commands with the parent custom commands.
   *
   * @param parentCmds  parent custom command collection
   * @param childCmds   child custom command collection
   */
  //todo: duplicated in Component Module.  Can we use mergeChildModules?
  private void mergeCustomCommands(Collection<CustomCommandDefinition> parentCmds,
                                   Collection<CustomCommandDefinition> childCmds) {

    Collection<String> existingNames = new HashSet<String>();

    for (CustomCommandDefinition childCmd : childCmds) {
      existingNames.add(childCmd.getName());
    }
    for (CustomCommandDefinition parentCmd : parentCmds) {
      if (! existingNames.contains(parentCmd.getName())) {
        childCmds.add(parentCmd);
      }
    }
  }

  /**
   * Finalize service configurations.
   * Ensure that all default type attributes are set.
   */
  private void finalizeConfiguration() {
    for (ConfigurationModule config : configurationModules.values()) {
      ConfigurationInfo configInfo = config.getModuleInfo();
      configInfo.ensureDefaultAttributes();
      serviceInfo.setTypeAttributes(config.getConfigType(), configInfo.getAttributes());
    }
  }
}
