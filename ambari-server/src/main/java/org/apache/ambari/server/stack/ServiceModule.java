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

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.CustomCommandDefinition;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.ThemeInfo;

/**
 * Service module which provides all functionality related to parsing and fully
 * resolving services from the stack definition.
 */
public class ServiceModule extends BaseModule<ServiceModule, ServiceInfo> implements Validable{
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
   * Map of themes, single value currently
   */
  private Map<String, ThemeModule> themeModules = new HashMap<String, ThemeModule>();

  /**
   * Encapsulates IO operations on service directory
   */
  private ServiceDirectory serviceDirectory;

  /**
   * Flag to mark a service as a common service
   */
  private boolean isCommonService;

  /**
   * validity flag
   */
  protected boolean valid = true;

  /**
   * Constructor.
   *
   * @param stackContext      stack context which provides module access to external functionality
   * @param serviceInfo       associated service info
   * @param serviceDirectory  used for all IO interaction with service directory in stack definition
   */
  public ServiceModule(StackContext stackContext, ServiceInfo serviceInfo, ServiceDirectory serviceDirectory) {
    this(stackContext, serviceInfo, serviceDirectory, false);
  }

  /**
   * Constructor.
   *
   * @param stackContext      stack context which provides module access to external functionality
   * @param serviceInfo       associated service info
   * @param serviceDirectory  used for all IO interaction with service directory in stack definition
   * @param isCommonService   flag to mark a service as a common service
   */
  public ServiceModule(
      StackContext stackContext, ServiceInfo serviceInfo, ServiceDirectory serviceDirectory, boolean isCommonService) {
    this.serviceInfo = serviceInfo;
    this.stackContext = stackContext;
    this.serviceDirectory = serviceDirectory;
    this.isCommonService = isCommonService;

    serviceInfo.setMetricsFile(serviceDirectory.getMetricsFile(serviceInfo.getName()));
    serviceInfo.setAlertsFile(serviceDirectory.getAlertsFile());
    serviceInfo.setKerberosDescriptorFile(serviceDirectory.getKerberosDescriptorFile());
    serviceInfo.setWidgetsDescriptorFile(serviceDirectory.getWidgetsDescriptorFile(serviceInfo.getName()));
    serviceInfo.setSchemaVersion(AmbariMetaInfo.SCHEMA_VERSION_2);
    serviceInfo.setServicePackageFolder(serviceDirectory.getPackageDir());

    populateComponentModules();
    populateConfigurationModules();
    populateThemeModules();
  }

  @Override
  public ServiceInfo getModuleInfo() {
    return serviceInfo;
  }

  @Override
  public void resolve(
      ServiceModule parentModule, Map<String, StackModule> allStacks, Map<String, ServiceModule> commonServices)
      throws AmbariException {
    ServiceInfo parent = parentModule.getModuleInfo();
    
    if (serviceInfo.getComment() == null) {
      serviceInfo.setComment(parent.getComment());
    }
    if (serviceInfo.getDisplayName() == null) {
      serviceInfo.setDisplayName(parent.getDisplayName());
    }
    if (serviceInfo.getVersion() == null) {
      serviceInfo.setVersion(parent.getVersion());
    }

    if (serviceInfo.getRequiredServices() == null
        || serviceInfo.getRequiredServices().size() == 0) {
      serviceInfo.setRequiredServices(parent.getRequiredServices() != null ?
          parent.getRequiredServices() :
          Collections.<String>emptyList());
    }

    if (serviceInfo.isRestartRequiredAfterChange() == null) {
      serviceInfo.setRestartRequiredAfterChange(parent.isRestartRequiredAfterChange());
    }
    if (serviceInfo.isRestartRequiredAfterRackChange() == null) {
      serviceInfo.setRestartRequiredAfterRackChange(parent.isRestartRequiredAfterRackChange());
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
    if (serviceInfo.getKerberosDescriptorFile() == null) {
      serviceInfo.setKerberosDescriptorFile(parent.getKerberosDescriptorFile());
    }
    if (serviceInfo.getThemesMap().isEmpty()) {
      serviceInfo.setThemesMap(parent.getThemesMap());
    }
    if (serviceInfo.getWidgetsDescriptorFile() == null) {
      serviceInfo.setWidgetsDescriptorFile(parent.getWidgetsDescriptorFile());
    }

    mergeCustomCommands(parent.getCustomCommands(), serviceInfo.getCustomCommands());
    mergeConfigDependencies(parent);
    mergeComponents(parentModule, allStacks, commonServices);
    mergeConfigurations(parentModule, allStacks, commonServices);
    mergeThemes(parentModule, allStacks, commonServices);
    mergeExcludedConfigTypes(parent);
  }

  /**
   * Resolve common service
   * @param allStacks       all stack modules
   * @param commonServices  common service modules
   *
   * @throws AmbariException
   */
  public void resolveCommonService(Map<String, StackModule> allStacks, Map<String, ServiceModule> commonServices)
      throws AmbariException {
    if(!isCommonService) {
      throw new AmbariException("Not a common service");
    }
    moduleState = ModuleState.VISITED;
    String parentString = serviceInfo.getParent();
    if(parentString != null) {
      String[] parentToks = parentString.split(StackManager.PATH_DELIMITER);
      if(parentToks.length != 3) {
        throw new AmbariException("The common service '" + serviceInfo.getName() + serviceInfo.getVersion()
            + "' extends an invalid parent: '" + parentString + "'");
      }
      if (parentToks[0].equalsIgnoreCase(StackManager.COMMON_SERVICES)) {
        String baseServiceKey = parentToks[1] + StackManager.PATH_DELIMITER + parentToks[2];
        ServiceModule baseService = commonServices.get(baseServiceKey);
        ModuleState baseModuleState = baseService.getModuleState();
        if (baseModuleState == ModuleState.INIT) {
          baseService.resolveCommonService(allStacks, commonServices);
        } else if (baseModuleState == ModuleState.VISITED) {
          //todo: provide more information to user about cycle
          throw new AmbariException("Cycle detected while parsing common service");
        }
        resolve(baseService, allStacks, commonServices);
      } else {
        throw new AmbariException("Common service cannot inherit from a non common service");
      }
    }
    moduleState = ModuleState.RESOLVED;
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
          ConfigurationInfo info = config.getModuleInfo();
          if (isValid()){
            setValid(config.isValid() && info.isValid());
            if (!isValid()){
              setErrors(config.getErrors());
              setErrors(info.getErrors());
            }
          }          
          serviceInfo.getProperties().addAll(info.getProperties());
          serviceInfo.setTypeAttributes(config.getConfigType(), info.getAttributes());
          configurationModules.put(config.getConfigType(), config);
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

  private void populateThemeModules() {

    if (serviceInfo.getThemesDir() == null) {
      serviceInfo.setThemesDir(AmbariMetaInfo.SERVICE_THEMES_FOLDER_NAME);
    }

    String themesDir = serviceDirectory.getAbsolutePath() + File.separator + serviceInfo.getThemesDir();

    if (serviceInfo.getThemes() != null) {
      for (ThemeInfo themeInfo : serviceInfo.getThemes()) {
        File themeFile = new File(themesDir + File.separator + themeInfo.getFileName());
        ThemeModule module = new ThemeModule(themeFile, themeInfo);
        themeModules.put(module.getId(), module);
      }
    }

    //lets not fail if theme contain errors
  }

  /**
   * Merge theme modules.
   */
  private void mergeThemes(ServiceModule parent, Map<String, StackModule> allStacks,
                           Map<String, ServiceModule> commonServices) throws AmbariException {
    Collection<ThemeModule> mergedModules = mergeChildModules(allStacks, commonServices, themeModules, parent.themeModules);

    for (ThemeModule mergedModule : mergedModules) {
      themeModules.put(mergedModule.getId(), mergedModule);
      ThemeInfo moduleInfo = mergedModule.getModuleInfo();
      if (!moduleInfo.isDeleted()) {
        serviceInfo.getThemesMap().put(moduleInfo.getFileName(), moduleInfo);
      } else {
        serviceInfo.getThemesMap().remove(moduleInfo.getFileName());
      }

    }

  }

  /**
   * Merge excluded configs types with parent.  Child values override parent values.
   *
   * @param parent parent service module
   */

  private void mergeExcludedConfigTypes(ServiceInfo parent){
    if (serviceInfo.getExcludedConfigTypes() == null){
      serviceInfo.setExcludedConfigTypes(parent.getExcludedConfigTypes());
    } else if (parent.getExcludedConfigTypes() != null){
      Set<String> resultExcludedConfigTypes = serviceInfo.getExcludedConfigTypes();
      for (String excludedType : parent.getExcludedConfigTypes()) {
        if (!resultExcludedConfigTypes.contains(excludedType)){
          resultExcludedConfigTypes.add(excludedType);
        }
      }
      serviceInfo.setExcludedConfigTypes(resultExcludedConfigTypes);
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
   * @param parent          parent service module
   * @param allStacks       all stack modules
   * @param commonServices  common service modules
   */
  private void mergeConfigurations(
      ServiceModule parent, Map<String, StackModule> allStacks, Map<String, ServiceModule> commonServices)
      throws AmbariException {
    serviceInfo.getProperties().clear();
    serviceInfo.setAllConfigAttributes(new HashMap<String, Map<String, Map<String, String>>>());

    Collection<ConfigurationModule> mergedModules = mergeChildModules(
        allStacks, commonServices, configurationModules, parent.configurationModules);

    for (ConfigurationModule module : mergedModules) {
      configurationModules.put(module.getId(), module);
      if(!module.isDeleted()) {
        serviceInfo.getProperties().addAll(module.getModuleInfo().getProperties());
        serviceInfo.setTypeAttributes(module.getConfigType(), module.getModuleInfo().getAttributes());
      }
    }
  }

  /**
   * Merge components with the parent configurations.
   * This will update the child component module set as well as the underlying info instances.
   *
   * @param parent          parent service module
   * @param allStacks       all stack modules
   * @param commonServices  common service modules
   */
  private void mergeComponents(
      ServiceModule parent, Map<String, StackModule> allStacks, Map<String, ServiceModule> commonServices)
      throws AmbariException {
    serviceInfo.getComponents().clear();
    Collection<ComponentModule> mergedModules = mergeChildModules(
        allStacks, commonServices, componentModules, parent.componentModules);
    componentModules.clear();
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

  @Override
  public boolean isValid() {
    return valid;
  }

  @Override
  public void setValid(boolean valid) {
    this.valid = valid;
  }
  
  private Set<String> errorSet = new HashSet<String>();
  
  @Override
  public void setErrors(String error) {
    errorSet.add(error);
  }

  @Override
  public Collection getErrors() {
    return errorSet;
  }
  
  @Override
  public void setErrors(Collection error) {
    this.errorSet.addAll(error);
  }  
}
