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
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.stack.StackDefinitionDirectory;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.PropertyDependencyInfo;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.stack.ConfigUpgradePack;
import org.apache.ambari.server.state.stack.RepositoryXml;
import org.apache.ambari.server.state.stack.ServiceMetainfoXml;
import org.apache.ambari.server.state.stack.StackMetainfoXml;
import org.apache.ambari.server.state.stack.UpgradePack;
import org.apache.ambari.server.state.stack.UpgradePack.OrderService;
import org.apache.ambari.server.state.stack.upgrade.ClusterGrouping;
import org.apache.ambari.server.state.stack.upgrade.Grouping;
import org.apache.ambari.server.state.stack.upgrade.ServiceCheckGrouping;
import org.apache.ambari.server.state.stack.upgrade.ClusterGrouping.ExecuteStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stack module which provides all functionality related to parsing and fully
 * resolving stacks from the stack definition.
 *
 * <p>
 * Each stack node is identified by name and version, contains service and configuration
 * child nodes and may extend a single parent stack.
 * </p>
 *
 * <p>
 * Resolution of a stack is a depth first traversal up the inheritance chain where each stack node
 * calls resolve on its parent before resolving itself.  After the parent resolve call returns, all
 * ancestors in the inheritance tree are fully resolved.  The act of resolving the stack includes
 * resolution of the configuration and services children of the stack as well as merging of other stack
 * state with the fully resolved parent.
 * </p>
 *
 * <p>
 * Configuration child node resolution involves merging configuration types, properties and attributes
 * with the fully resolved parent.
 * </p>
 *
 * <p>
 * Because a service may explicitly extend another service in a stack outside of the inheritance tree,
 * service child node resolution involves a depth first resolution of the stack associated with the
 * services explicit parent, if any.  This follows the same steps defined above fore stack node
 * resolution.  After the services explicit parent is fully resolved, the services state is merged
 * with it's parent.
 * </p>
 *
 * <p>
 * If a cycle in a stack definition is detected, an exception is thrown from the resolve call.
 * </p>
 *
 */
public class StackModule extends BaseModule<StackModule, StackInfo> implements Validable {

  /**
   * Context which provides access to external functionality
   */
  private StackContext stackContext;

  /**
   * Map of child configuration modules keyed by configuration type
   */
  private Map<String, ConfigurationModule> configurationModules = new HashMap<String, ConfigurationModule>();

  /**
   * Map of child service modules keyed by service name
   */
  private Map<String, ServiceModule> serviceModules = new HashMap<String, ServiceModule>();

  /**
   * Corresponding StackInfo instance
   */
  private StackInfo stackInfo;

  /**
   * Encapsulates IO operations on stack directory
   */
  private StackDirectory stackDirectory;

  /**
   * Stack id which is in the form stackName:stackVersion
   */
  private String id;

  /**
   * validity flag
   */
  protected boolean valid = true;

  /**
   * file unmarshaller
   */
  ModuleFileUnmarshaller unmarshaller = new ModuleFileUnmarshaller();

  /**
   * Logger
   */
  private final static Logger LOG = LoggerFactory.getLogger(StackModule.class);

  /**
   * Constructor.
   * @param stackDirectory  represents stack directory
   * @param stackContext    general stack context
   */
  public StackModule(StackDirectory stackDirectory, StackContext stackContext) {
    this.stackDirectory = stackDirectory;
    this.stackContext = stackContext;
    this.stackInfo = new StackInfo();
    populateStackInfo();
  }

  /**
   * Fully resolve the stack. See stack resolution description in the class documentation.
   * If the stack has a parent, this stack will be merged against its fully resolved parent
   * if one is specified.Merging applies to all stack state including child service and
   * configuration modules.  Services may extend a service in another version in the
   * same stack hierarchy or may explicitly extend a service in a stack in a different
   * hierarchy.
   *
   * @param parentModule   not used.  Each stack determines its own parent since stacks don't
   *                       have containing modules
   * @param allStacks      all stacks modules contained in the stack definition
   * @param commonServices all common services specified in the stack definition
   *
   * @throws AmbariException if an exception occurs during stack resolution
   */
  @Override
  public void resolve(
      StackModule parentModule, Map<String, StackModule> allStacks, Map<String, ServiceModule> commonServices)
      throws AmbariException {
    moduleState = ModuleState.VISITED;
    String parentVersion = stackInfo.getParentStackVersion();
    mergeServicesWithExplicitParent(allStacks, commonServices);
    // merge with parent version of same stack definition
    if (parentVersion != null) {
      mergeStackWithParent(parentVersion, allStacks, commonServices);
    }
    processUpgradePacks();
    processRepositories();
    processPropertyDependencies();
    moduleState = ModuleState.RESOLVED;
  }

  @Override
  public StackInfo getModuleInfo() {
    return stackInfo;
  }

  @Override
  public boolean isDeleted() {
    return false;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void finalizeModule() {
    finalizeChildModules(serviceModules.values());
    finalizeChildModules(configurationModules.values());
  }

  /**
   * Get the associated stack directory.
   *
   * @return associated stack directory
   */
  public StackDirectory getStackDirectory() {
    return stackDirectory;
  }

  /**
   * Merge the stack with its parent.
   *
   * @param allStacks      all stacks in stack definition
   * @param commonServices all common services specified in the stack definition
   * @param parentVersion  version of the stacks parent
   *
   * @throws AmbariException if an exception occurs merging with the parent
   */
  private void mergeStackWithParent(
      String parentVersion, Map<String, StackModule> allStacks, Map<String, ServiceModule> commonServices)
      throws AmbariException {

    String parentStackKey = stackInfo.getName() + StackManager.PATH_DELIMITER + parentVersion;
    StackModule parentStack = allStacks.get(parentStackKey);

    if (parentStack == null) {
      throw new AmbariException("Stack '" + stackInfo.getName() + ":" + stackInfo.getVersion() +
          "' specifies a parent that doesn't exist");
    }

    resolveStack(parentStack, allStacks, commonServices);
    mergeConfigurations(parentStack, allStacks, commonServices);
    mergeRoleCommandOrder(parentStack);

    if (stackInfo.getStackHooksFolder() == null) {
      stackInfo.setStackHooksFolder(parentStack.getModuleInfo().getStackHooksFolder());
    }

    // grab stack level kerberos.json from parent stack
    if (stackInfo.getKerberosDescriptorFileLocation() == null) {
      stackInfo.setKerberosDescriptorFileLocation(parentStack.getModuleInfo().getKerberosDescriptorFileLocation());
    }

    if (stackInfo.getWidgetsDescriptorFileLocation() == null) {
      stackInfo.setWidgetsDescriptorFileLocation(parentStack.getModuleInfo().getWidgetsDescriptorFileLocation());
    }

    mergeServicesWithParent(parentStack, allStacks, commonServices);
  }

  /**
   * Merge child services with parent stack.
   *
   * @param parentStack    parent stack module
   * @param allStacks      all stacks in stack definition
   * @param commonServices all common services specified in the stack definition
   *
   * @throws AmbariException if an exception occurs merging the child services with the parent stack
   */
  private void mergeServicesWithParent(
      StackModule parentStack, Map<String, StackModule> allStacks, Map<String, ServiceModule> commonServices)
      throws AmbariException {
    stackInfo.getServices().clear();
    Collection<ServiceModule> mergedModules = mergeChildModules(
        allStacks, commonServices, serviceModules, parentStack.serviceModules);
    for (ServiceModule module : mergedModules) {
      serviceModules.put(module.getId(), module);
      stackInfo.getServices().add(module.getModuleInfo());
    }
  }

  /**
   * Merge services with their explicitly specified parent if one has been specified.
   * @param allStacks      all stacks in stack definition
   * @param commonServices all common services specified in the stack definition
   *
   * @throws AmbariException if an exception occurs while merging child services with their explicit parents
   */
  private void mergeServicesWithExplicitParent(
      Map<String, StackModule> allStacks, Map<String, ServiceModule> commonServices) throws AmbariException {
    for (ServiceModule service : serviceModules.values()) {
      ServiceInfo serviceInfo = service.getModuleInfo();
      String parent = serviceInfo.getParent();
      if (parent != null) {
        mergeServiceWithExplicitParent(service, parent, allStacks, commonServices);
      }
    }
  }

  /**
   * Merge a service with its explicitly specified parent.
   * @param service          the service to merge
   * @param parent           the explicitly specified parent service
   * @param allStacks        all stacks specified in the stack definition
   * @param commonServices   all common services specified in the stack definition
   *
   * @throws AmbariException if an exception occurs merging a service with its explicit parent
   */
  private void mergeServiceWithExplicitParent(
      ServiceModule service, String parent, Map<String, StackModule> allStacks,
      Map<String, ServiceModule> commonServices)
      throws AmbariException {
    if(isCommonServiceParent(parent)) {
      mergeServiceWithCommonServiceParent(service, parent, allStacks,commonServices);
    } else {
      mergeServiceWithStackServiceParent(service, parent, allStacks, commonServices);
    }
  }

  /**
   * Check if parent is common service
   * @param parent  Parent string
   * @return true: if parent is common service, false otherwise
   */
  private boolean isCommonServiceParent(String parent) {
    return parent != null
        && !parent.isEmpty()
        && parent.split(StackManager.PATH_DELIMITER)[0].equalsIgnoreCase(StackManager.COMMON_SERVICES);
  }

  /**
   * Merge a service with its explicitly specified common service as parent.
   * Parent: common-services/<serviceName>/<serviceVersion>
   * Common Services Lookup Key: <serviceName>/<serviceVersion>
   * Example:
   *  Parent: common-services/HDFS/2.1.0.2.0
   *  Key: HDFS/2.1.0.2.0
   *
   * @param service          the service to merge
   * @param parent           the explicitly specified common service as parent
   * @param allStacks        all stacks specified in the stack definition
   * @param commonServices   all common services specified in the stack definition
   * @throws AmbariException
   */
  private void mergeServiceWithCommonServiceParent(
      ServiceModule service, String parent, Map<String, StackModule> allStacks,
      Map<String, ServiceModule> commonServices)
      throws AmbariException {
    ServiceInfo serviceInfo = service.getModuleInfo();
    String[] parentToks = parent.split(StackManager.PATH_DELIMITER);
    if(parentToks.length != 3 || !parentToks[0].equalsIgnoreCase(StackManager.COMMON_SERVICES)) {
      throw new AmbariException("The service '" + serviceInfo.getName() + "' in stack '" + stackInfo.getName() + ":"
          + stackInfo.getVersion() + "' extends an invalid parent: '" + parent + "'");
    }

    String baseServiceKey = parentToks[1] + StackManager.PATH_DELIMITER + parentToks[2];
    ServiceModule baseService = commonServices.get(baseServiceKey);
    if (baseService == null) {
      setValid(false);
      stackInfo.setValid(false);
      String error = "The service '" + serviceInfo.getName() + "' in stack '" + stackInfo.getName() + ":"
          + stackInfo.getVersion() + "' extends a non-existent service: '" + parent + "'";
      addError(error);
      stackInfo.addError(error);
    } else {
      if (baseService.isValid()) {
        service.resolveExplicit(baseService, allStacks, commonServices);
      } else {
        setValid(false);
        stackInfo.setValid(false);
        addErrors(baseService.getErrors());
        stackInfo.addErrors(baseService.getErrors());
      }
    }
  }

  /**
   * Merge a service with its explicitly specified stack service as parent.
   * Parent: <stackName>/<stackVersion>/<serviceName>
   * Stack Lookup Key: <stackName>/<stackVersion>
   * Example:
   *  Parent: HDP/2.0.6/HDFS
   *  Key: HDP/2.0.6
   *
   * @param service          the service to merge
   * @param parent           the explicitly specified stack service as parent
   * @param allStacks        all stacks specified in the stack definition
   * @param commonServices   all common services specified in the stack definition
   * @throws AmbariException
   */
  private void mergeServiceWithStackServiceParent(
      ServiceModule service, String parent, Map<String, StackModule> allStacks,
      Map<String, ServiceModule> commonServices)
      throws AmbariException {
    ServiceInfo serviceInfo = service.getModuleInfo();
    String[] parentToks = parent.split(StackManager.PATH_DELIMITER);
    if(parentToks.length != 3 || parentToks[0].equalsIgnoreCase(StackManager.COMMON_SERVICES)) {
      throw new AmbariException("The service '" + serviceInfo.getName() + "' in stack '" + stackInfo.getName() + ":"
          + stackInfo.getVersion() + "' extends an invalid parent: '" + parent + "'");
    }

    String baseStackKey = parentToks[0] + StackManager.PATH_DELIMITER + parentToks[1];
    StackModule baseStack = allStacks.get(baseStackKey);
    if (baseStack == null) {
      throw new AmbariException("The service '" + serviceInfo.getName() + "' in stack '" + stackInfo.getName() + ":"
          + stackInfo.getVersion() + "' extends a service in a non-existent stack: '" + baseStackKey + "'");
    }

    resolveStack(baseStack, allStacks, commonServices);

    ServiceModule baseService = baseStack.serviceModules.get(parentToks[2]);
    if (baseService == null) {
      throw new AmbariException("The service '" + serviceInfo.getName() + "' in stack '" + stackInfo.getName() + ":"
          + stackInfo.getVersion() + "' extends a non-existent service: '" + parent + "'");
      }
    service.resolveExplicit(baseService, allStacks, commonServices);
  }

  /**
   * Populate the stack module and info from the stack definition.
   */
  private void populateStackInfo() {
    stackInfo.setName(stackDirectory.getStackDirName());
    stackInfo.setVersion(stackDirectory.getName());

    id = String.format("%s:%s", stackInfo.getName(), stackInfo.getVersion());

    LOG.debug("Adding new stack to known stacks"
        + ", stackName = " + stackInfo.getName()
        + ", stackVersion = " + stackInfo.getVersion());


    //odo: give additional thought on handling missing metainfo.xml
    StackMetainfoXml smx = stackDirectory.getMetaInfoFile();
    if (smx != null) {
      if (!smx.isValid()) {
        stackInfo.setValid(false);
        stackInfo.addErrors(smx.getErrors());
      }
      stackInfo.setMinJdk(smx.getMinJdk());
      stackInfo.setMaxJdk(smx.getMaxJdk());
      stackInfo.setMinUpgradeVersion(smx.getVersion().getUpgrade());
      stackInfo.setActive(smx.getVersion().isActive());
      stackInfo.setParentStackVersion(smx.getExtends());
      stackInfo.setStackHooksFolder(stackDirectory.getHooksDir());
      stackInfo.setRcoFileLocation(stackDirectory.getRcoFilePath());
      stackInfo.setKerberosDescriptorFileLocation(stackDirectory.getKerberosDescriptorFilePath());
      stackInfo.setWidgetsDescriptorFileLocation(stackDirectory.getWidgetsDescriptorFilePath());
      stackInfo.setUpgradesFolder(stackDirectory.getUpgradesDir());
      stackInfo.setUpgradePacks(stackDirectory.getUpgradePacks());
      stackInfo.setConfigUpgradePack(stackDirectory.getConfigUpgradePack());
      stackInfo.setRoleCommandOrder(stackDirectory.getRoleCommandOrder());
      populateConfigurationModules();
    }

    try {
      //configurationModules
      RepositoryXml rxml = stackDirectory.getRepoFile();
      if (rxml != null && !rxml.isValid()) {
        stackInfo.setValid(false);
        stackInfo.addErrors(rxml.getErrors());
      }
      // Read the service and available configs for this stack
      populateServices();
      if (!stackInfo.isValid()) {
        setValid(false);
        addErrors(stackInfo.getErrors());
      }

      //todo: shouldn't blindly catch Exception, re-evaluate this.
    } catch (Exception e) {
      String error = "Exception caught while populating services for stack: " +
          stackInfo.getName() + "-" + stackInfo.getVersion();
      setValid(false);
      stackInfo.setValid(false);
      addError(error);
      stackInfo.addError(error);
      LOG.error(error);
    }
  }

  /**
   * Populate the child services.
   */
  private void populateServices()throws AmbariException {
    for (ServiceDirectory serviceDir : stackDirectory.getServiceDirectories()) {
      populateService(serviceDir);
    }
  }

  /**
   * Populate a child service.
   *
   * @param serviceDirectory the child service directory
   */
  private void populateService(ServiceDirectory serviceDirectory)  {
    Collection<ServiceModule> serviceModules = new ArrayList<ServiceModule>();
    // unfortunately, we allow multiple services to be specified in the same metainfo.xml,
    // so we can't move the unmarshal logic into ServiceModule
    ServiceMetainfoXml metaInfoXml = serviceDirectory.getMetaInfoFile();
    if (!metaInfoXml.isValid()){
      stackInfo.setValid(metaInfoXml.isValid());
      setValid(metaInfoXml.isValid());
      stackInfo.addErrors(metaInfoXml.getErrors());
      addErrors(metaInfoXml.getErrors());
      return;
    }
    List<ServiceInfo> serviceInfos = metaInfoXml.getServices();

    for (ServiceInfo serviceInfo : serviceInfos) {
      ServiceModule serviceModule = new ServiceModule(stackContext, serviceInfo, serviceDirectory);
      serviceModules.add(serviceModule);
      if (!serviceModule.isValid()){
        stackInfo.setValid(false);
        setValid(false);
        stackInfo.addErrors(serviceModule.getErrors());
        addErrors(serviceModule.getErrors());
      }
    }
    addServices(serviceModules);
  }

  /**
   * Populate the child configurations.
   */
  private void populateConfigurationModules() {
    //todo: can't exclude types in stack config
    ConfigurationDirectory configDirectory = stackDirectory.getConfigurationDirectory(
        AmbariMetaInfo.SERVICE_CONFIG_FOLDER_NAME, AmbariMetaInfo.SERVICE_PROPERTIES_FOLDER_NAME);

    if (configDirectory != null) {
      for (ConfigurationModule config : configDirectory.getConfigurationModules()) {
        if (stackInfo.isValid()){
          stackInfo.setValid(config.isValid());
          stackInfo.addErrors(config.getErrors());
        }
        stackInfo.getProperties().addAll(config.getModuleInfo().getProperties());
        stackInfo.setConfigTypeAttributes(config.getConfigType(), config.getModuleInfo().getAttributes());
        configurationModules.put(config.getConfigType(), config);
      }
    }
  }

  /**
   * Merge configurations with the parent configurations.
   *
   * @param parent  parent stack module
   * @param allStacks      all stacks in stack definition
   * @param commonServices all common services specified in the stack definition
   */
  private void mergeConfigurations(
      StackModule parent, Map<String,StackModule> allStacks, Map<String, ServiceModule> commonServices)
      throws AmbariException {
    stackInfo.getProperties().clear();
    stackInfo.setAllConfigAttributes(new HashMap<String, Map<String, Map<String, String>>>());

    Collection<ConfigurationModule> mergedModules = mergeChildModules(
        allStacks, commonServices, configurationModules, parent.configurationModules);
    for (ConfigurationModule module : mergedModules) {
      configurationModules.put(module.getId(), module);
      stackInfo.getProperties().addAll(module.getModuleInfo().getProperties());
      stackInfo.setConfigTypeAttributes(module.getConfigType(), module.getModuleInfo().getAttributes());
    }
  }

  /**
   * Resolve another stack module.
   *
   * @param stackToBeResolved  stack module to be resolved
   * @param allStacks          all stack modules in stack definition
   * @param commonServices     all common services specified in the stack definition
   * @throws AmbariException if unable to resolve the stack
   */
  private void resolveStack(
          StackModule stackToBeResolved, Map<String, StackModule> allStacks, Map<String, ServiceModule> commonServices)
          throws AmbariException {
    if (stackToBeResolved.getModuleState() == ModuleState.INIT) {
      stackToBeResolved.resolve(null, allStacks, commonServices);
    } else if (stackToBeResolved.getModuleState() == ModuleState.VISITED) {
      //todo: provide more information to user about cycle
      throw new AmbariException("Cycle detected while parsing stack definition");
    }
    if (!stackToBeResolved.isValid() || (stackToBeResolved.getModuleInfo() != null && !stackToBeResolved.getModuleInfo().isValid())) {
      setValid(stackToBeResolved.isValid());
      stackInfo.setValid(stackToBeResolved.stackInfo.isValid());
      addErrors(stackToBeResolved.getErrors());
      stackInfo.addErrors(stackToBeResolved.getErrors());
    }
  }

  /**
   * Add a child service module to the stack.
   *
   * @param service  service module to add
   */
  private void addService(ServiceModule service) {
    ServiceInfo serviceInfo = service.getModuleInfo();
    Object previousValue = serviceModules.put(service.getId(), service);
    if (previousValue == null) {
      stackInfo.getServices().add(serviceInfo);
    }
  }

  /**
   * Add child service modules to the stack.
   *
   * @param services  collection of service modules to add
   */
  private void addServices(Collection<ServiceModule> services) {
    for (ServiceModule service : services) {
      addService(service);
    }
  }

  /**
   * Process <depends-on></depends-on> properties
   */
  private void processPropertyDependencies() {

    // Stack-definition has 'depends-on' relationship specified.
    // We have a map to construct the 'depended-by' relationship.
    Map<PropertyDependencyInfo, Set<PropertyDependencyInfo>> dependedByMap =
      new HashMap<PropertyDependencyInfo, Set<PropertyDependencyInfo>>();

    // Go through all service-configs and gather the reversed 'depended-by'
    // relationship into map. Since we do not have the reverse {@link PropertyInfo},
    // we have to loop through service-configs again later.
    for (ServiceModule serviceModule : serviceModules.values()) {
      for (PropertyInfo pi : serviceModule.getModuleInfo().getProperties()) {
        for (PropertyDependencyInfo pdi : pi.getDependsOnProperties()) {
          String type = ConfigHelper.fileNameToConfigType(pi.getFilename());
          String name = pi.getName();
          PropertyDependencyInfo propertyDependency =
            new PropertyDependencyInfo(type, name);
          if (dependedByMap.keySet().contains(pdi)) {
            dependedByMap.get(pdi).add(propertyDependency);
          } else {
            Set<PropertyDependencyInfo> newDependenciesSet =
              new HashSet<PropertyDependencyInfo>();
            newDependenciesSet.add(propertyDependency);
            dependedByMap.put(pdi, newDependenciesSet);
          }
        }
      }
    }

    // Go through all service-configs again and set their 'depended-by' if necessary.
    for (ServiceModule serviceModule : serviceModules.values()) {
      addDependedByProperties(dependedByMap, serviceModule.getModuleInfo().getProperties());
    }
    // Go through all stack-configs again and set their 'depended-by' if necessary.
    addDependedByProperties(dependedByMap, stackInfo.getProperties());
  }

  /**
   * Add dependendByProperties to property info's
   * @param dependedByMap Map containing the 'depended-by' relationships
   * @param properties properties to check against dependedByMap
   */
  private void addDependedByProperties(Map<PropertyDependencyInfo, Set<PropertyDependencyInfo>> dependedByMap,
                                  Collection<PropertyInfo> properties) {
    for (PropertyInfo pi : properties) {
      String type = ConfigHelper.fileNameToConfigType(pi.getFilename());
      String name = pi.getName();
      Set<PropertyDependencyInfo> set =
        dependedByMap.remove(new PropertyDependencyInfo(type, name));
      if (set != null) {
        pi.getDependedByProperties().addAll(set);
      }
    }
  }

  /**
   * Process upgrade packs associated with the stack.
   * @throws AmbariException if unable to fully process the upgrade packs
   */
  private void processUpgradePacks() throws AmbariException {
    if (stackInfo.getUpgradePacks() == null) {
      return;
    }

    for (UpgradePack pack : stackInfo.getUpgradePacks().values()) {
      List<UpgradePack> servicePacks = new ArrayList<>();
      for (ServiceModule module : serviceModules.values()) {
        File upgradesFolder = module.getModuleInfo().getServiceUpgradesFolder();
        if (upgradesFolder != null) {
          UpgradePack servicePack = getServiceUpgradePack(pack, upgradesFolder);
          if (servicePack != null) {
            servicePacks.add(servicePack);
          }
        }
      }
      if (servicePacks.size() > 0) {
        LOG.info("Merging service specific upgrades for pack: " + pack.getName());
        mergeUpgradePack(pack, servicePacks);
      }
    }

    ConfigUpgradePack configPack = stackInfo.getConfigUpgradePack();
    if (configPack == null) {
      return;
    }
    for (ServiceModule module : serviceModules.values()) {
      File upgradesFolder = module.getModuleInfo().getServiceUpgradesFolder();
      if (upgradesFolder != null) {
        mergeConfigUpgradePack(configPack, upgradesFolder);
      }
    }
  }

  /**
   * Attempts to merge, into the stack config upgrade, all the config upgrades
   * for any service which specifies its own upgrade.
   */
  private void mergeConfigUpgradePack(ConfigUpgradePack pack, File upgradesFolder) throws AmbariException {
    File stackFolder = new File(upgradesFolder, stackInfo.getName());
    File versionFolder = new File(stackFolder, stackInfo.getVersion());
    File serviceConfig = new File(versionFolder, StackDefinitionDirectory.CONFIG_UPGRADE_XML_FILENAME_PREFIX);
    if (!serviceConfig.exists()) {
      return;
    }

    try {
      ConfigUpgradePack serviceConfigPack = unmarshaller.unmarshal(ConfigUpgradePack.class, serviceConfig);
      pack.services.addAll(serviceConfigPack.services);
    }
    catch (JAXBException e) {
      throw new AmbariException("Unable to parse service config upgrade file at location: " + serviceConfig.getAbsolutePath(), e);
    }
  }

  /**
   * Returns the upgrade pack for a service if it exists, otherwise returns null
   */
  private UpgradePack getServiceUpgradePack(UpgradePack pack, File upgradesFolder) throws AmbariException {
    File stackFolder = new File(upgradesFolder, stackInfo.getName());
    File versionFolder = new File(stackFolder, stackInfo.getVersion());
    File servicePackFile = new File(versionFolder, pack.getName() + ".xml");
    LOG.info("Service folder: " + servicePackFile.getAbsolutePath());
    if (!servicePackFile.exists()) {
      return null;
    }
    return parseServiceUpgradePack(pack, servicePackFile);
  }

  /**
   * Attempts to merge, into the stack upgrade, all the upgrades
   * for any service which specifies its own upgrade.
   */
  private void mergeUpgradePack(UpgradePack pack, List<UpgradePack> servicePacks) throws AmbariException {
    List<Grouping> originalGroups = pack.getAllGroups();
    Map<String, List<Grouping>> allGroupMap = new HashMap<>();
    for (Grouping group : originalGroups) {
      List<Grouping> list = new ArrayList<>();
      list.add(group);
      allGroupMap.put(group.name, list);
    }
    for (UpgradePack servicePack : servicePacks) {
      for (Grouping group : servicePack.getAllGroups()) {
        if (allGroupMap.containsKey(group.name)) {
          List<Grouping> list = allGroupMap.get(group.name);
          Grouping first = list.get(0);
          if (!first.getClass().equals(group.getClass())) {
            throw new AmbariException("Expected class: " + first.getClass() + " instead of " + group.getClass());
          }
          /* If the current group doesn't specify an "after entry" and the first group does
             then the current group should be added first.  The first group in the list should 
             never be ordered relative to any other group. */ 
          if (group.addAfterGroupEntry == null && first.addAfterGroupEntry != null) {
            list.add(0, group);
          }
          else {
            list.add(group);
          }
        }
        else {
          List<Grouping> list = new ArrayList<>();
          list.add(group);
          allGroupMap.put(group.name, list);
        }
      }
    }

    Map<String, Grouping> mergedGroupMap = new HashMap<>();
    for (String key : allGroupMap.keySet()) {
      Iterator<Grouping> iterator = allGroupMap.get(key).iterator();
      Grouping group = iterator.next();
      if (iterator.hasNext()) {
        group.merge(iterator);
      }
      mergedGroupMap.put(key, group);
    }

    orderGroups(originalGroups, mergedGroupMap);
  }

  /**
   * Orders the upgrade groups.  All new groups specified in a service's upgrade file must
   * specify after which group they should be placed in the upgrade order.
   */
  private void orderGroups(List<Grouping> groups, Map<String, Grouping> mergedGroupMap) throws AmbariException {
    Map<String, List<Grouping>> skippedGroups = new HashMap<>();
    for (Map.Entry<String, Grouping> entry : mergedGroupMap.entrySet()) {
      String key = entry.getKey();
      Grouping group = entry.getValue();
      if (!groups.contains(group)) {
        boolean added = addGrouping(groups, group);
        if (added) {
          addSkippedGroup(groups, skippedGroups, group);
        } else {
          List<Grouping> tmp = null;
          // store the group until later
          if (skippedGroups.containsKey(group.addAfterGroup)) {
            tmp = skippedGroups.get(group.addAfterGroup);
          } else {
            tmp = new ArrayList<Grouping>();
            skippedGroups.put(group.addAfterGroup, tmp);
          }
          tmp.add(group);
        }
      }
    }
    if (!skippedGroups.isEmpty()) {
      throw new AmbariException("Missing groups: " + skippedGroups.keySet());
    }
  }

  /**
   * Adds the group provided if the group which it should come after has been added.
   */
  private boolean addGrouping(List<Grouping> groups, Grouping group) throws AmbariException {
    if (group.addAfterGroup == null) {
      throw new AmbariException("Group " + group.name + " needs to specify which group it should come after");
    }
    else {
      // Check the current services, if the "after" service is there then add these
      for (int index = groups.size() - 1; index >= 0; index--) {
        String name = groups.get(index).name;
        if (name.equals(group.addAfterGroup)) {
          groups.add(index + 1, group);
          LOG.debug("Added group/after: " + group.name + "/" + group.addAfterGroup);
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Adds any groups which have been previously skipped if the group which they should come
   * after have been added.
   */
  private void addSkippedGroup(List<Grouping> groups, Map<String, List<Grouping>> skippedGroups, Grouping groupJustAdded) throws AmbariException {
    if (skippedGroups.containsKey(groupJustAdded.name)) {
      List<Grouping> groupsToAdd = skippedGroups.remove(groupJustAdded.name);
      for (Grouping group : groupsToAdd) {
        boolean added = addGrouping(groups, group);
        if (added) {
          addSkippedGroup(groups, skippedGroups, group);
        } else {
          throw new AmbariException("Failed to add group " + group.name);
        }
      }
    }
  }

  /**
   * Parses the service specific upgrade file and merges the none order elements
   * (prerequisite check and processing sections).
   */
  private UpgradePack parseServiceUpgradePack(UpgradePack parent, File serviceFile) throws AmbariException {
    UpgradePack pack = null;
    try {
      pack = unmarshaller.unmarshal(UpgradePack.class, serviceFile);
    }
    catch (JAXBException e) {
      throw new AmbariException("Unable to parse service upgrade file at location: " + serviceFile.getAbsolutePath(), e);
    }

    parent.mergePrerequisiteChecks(pack);
    parent.mergeProcessing(pack);

    return pack;
  }

  /**
   * Process repositories associated with the stack.
   * @throws AmbariException if unable to fully process the stack repositories
   */
  private void processRepositories() throws AmbariException {

    RepositoryXml rxml = stackDirectory.getRepoFile();
    if (rxml == null) {
      return;
    }

    stackInfo.setRepositoryXml(rxml);

    LOG.debug("Adding repositories to stack" +
        ", stackName=" + stackInfo.getName() +
        ", stackVersion=" + stackInfo.getVersion() +
        ", repoFolder=" + stackDirectory.getRepoDir());

    List<RepositoryInfo> repos = rxml.getRepositories();

    for (RepositoryInfo ri : repos) {
      processRepository(ri);
    }

    stackInfo.getRepositories().addAll(repos);

    if (null != rxml.getLatestURI() && repos.size() > 0) {
      stackContext.registerRepoUpdateTask(rxml.getLatestURI(), this);
    }
  }

  /**
   * Process a repository associated with the stack.
   *
   * @param osFamily  OS family
   * @param osType    OS type
   * @param r         repo
   */
  private RepositoryInfo processRepository(RepositoryInfo ri) {

    LOG.debug("Checking for override for base_url");
    String updatedUrl = stackContext.getUpdatedRepoUrl(stackInfo.getName(), stackInfo.getVersion(),
        ri.getOsType(), ri.getRepoId());

    if (null != updatedUrl) {
      ri.setBaseUrl(updatedUrl);
      ri.setBaseUrlFromSaved(true);
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Adding repo to stack"
          + ", repoInfo=" + ri.toString());
    }
    return ri;
  }


  /**
   * Merge role command order with the parent stack
   *
   * @param parentStack parent stack
   */

  private void mergeRoleCommandOrder(StackModule parentStack) {
    stackInfo.getRoleCommandOrder().merge(parentStack.stackInfo.getRoleCommandOrder());
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
  public void addError(String error) {
    errorSet.add(error);
  }

  @Override
  public Collection<String> getErrors() {
    return errorSet;
  }

  @Override
  public void addErrors(Collection<String> errors) {
    this.errorSet.addAll(errors);
  }

}
