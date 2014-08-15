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

package org.apache.ambari.server.api.services;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.xml.bind.JAXBException;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.ParentObjectNotFoundException;
import org.apache.ambari.server.StackAccessException;
import org.apache.ambari.server.api.util.StackExtensionHelper;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.customactions.ActionDefinition;
import org.apache.ambari.server.customactions.ActionDefinitionManager;
import org.apache.ambari.server.orm.dao.MetainfoDAO;
import org.apache.ambari.server.orm.entities.MetainfoEntity;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.DependencyInfo;
import org.apache.ambari.server.state.OperatingSystemInfo;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.Stack;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.alert.AlertDefinition;
import org.apache.ambari.server.state.alert.AlertDefinitionFactory;
import org.apache.ambari.server.state.stack.LatestRepoCallable;
import org.apache.ambari.server.state.stack.MetricDefinition;
import org.apache.ambari.server.state.stack.RepositoryXml;
import org.apache.ambari.server.state.stack.RepositoryXml.Os;
import org.apache.ambari.server.state.stack.RepositoryXml.Repo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;


/**
 * ServiceInfo responsible getting information about cluster.
 */
@Singleton
public class AmbariMetaInfo {

  public static final String STACK_METAINFO_FILE_NAME = "metainfo.xml";
  public static final String SERVICES_FOLDER_NAME = "services";
  public static final String SERVICE_METAINFO_FILE_NAME = "metainfo.xml";
  public static final String SERVICE_CONFIG_FOLDER_NAME = "configuration";
  public static final String SERVICE_CONFIG_FILE_NAME_POSTFIX = ".xml";
  public static final String RCO_FILE_NAME = "role_command_order.json";
  public static final String SERVICE_METRIC_FILE_NAME = "metrics.json";
  public static final String SERVICE_ALERT_FILE_NAME = "alerts.json";
  /**
   * This string is used in placeholder in places that are common for
   * all operating systems or in situations where os type is not important.
   */
  public static final String ANY_OS = "any";
  /**
   * Version of XML files with support of custom services and custom commands
   */
  public static final String SCHEMA_VERSION_2 = "2.0";
  public static final FilenameFilter FILENAME_FILTER = new FilenameFilter() {
    @Override
    public boolean accept(File dir, String s) {
      if (s.equals(".svn") || s.equals(".git") ||
          s.equals(StackExtensionHelper.HOOKS_FOLDER_NAME)) // Hooks dir is not a service
      {
        return false;
      }
      return true;
    }
  };
  private final static Logger LOG = LoggerFactory.getLogger(AmbariMetaInfo.class);
  private static final String REPOSITORY_FILE_NAME = "repoinfo.xml";
  private static final String REPOSITORY_FOLDER_NAME = "repos";
  public static final String REPOSITORY_XML_PROPERTY_BASEURL = "baseurl";
  // all the supported OS'es
  private static final List<String> ALL_SUPPORTED_OS = Arrays.asList(
      "centos5", "redhat5", "centos6", "redhat6", "oraclelinux5",
      "oraclelinux6", "suse11", "sles11", "ubuntu12", "debian12");

  private final ActionDefinitionManager adManager = new ActionDefinitionManager();
  private String serverVersion = "undefined";
  private List<StackInfo> stacksResult = new ArrayList<StackInfo>();
  private File stackRoot;
  private File serverVersionFile;
  private File customActionRoot;

  @Inject
  private MetainfoDAO metainfoDAO;

  @Inject
  Injector injector;

  @Inject
  private AlertDefinitionFactory alertDefinitionFactory;

  // Required properties by stack version
  private final Map<StackId, Map<String, Map<String, PropertyInfo>>> requiredProperties =
    new HashMap<StackId, Map<String, Map<String, PropertyInfo>>>();

  /**
   * Ambari Meta Info Object
   *
   * @param conf Configuration API to be used.
   * @throws Exception
   */
  @Inject
  public AmbariMetaInfo(Configuration conf) throws Exception {
    String stackPath = conf.getMetadataPath();
    String serverVersionFilePath = conf.getServerVersionFilePath();
    stackRoot = new File(stackPath);
    serverVersionFile = new File(serverVersionFilePath);
    customActionRoot = new File(conf.getCustomActionDefinitionPath());
  }

  public AmbariMetaInfo(File stackRoot, File serverVersionFile) throws Exception {
    this.stackRoot = stackRoot;
    this.serverVersionFile = serverVersionFile;
  }

  /**
   * Initialize the Ambari Meta Info
   *
   * @throws Exception throws exception if not able to parse the Meta data.
   */
  @Inject
  public void init() throws Exception {
    stacksResult = new ArrayList<StackInfo>();
    readServerVersion();
    getConfigurationInformation(stackRoot);
    getCustomActionDefinitions(customActionRoot);

    alertDefinitionFactory = injector.getInstance(AlertDefinitionFactory.class);
  }

  /**
   * Get component category
   *
   * @param stackName     stack name
   * @param version       stack version
   * @param serviceName   service name
   * @param componentName component name
   * @return component component Info
   * @throws AmbariException
   */
  public ComponentInfo getComponentCategory(String stackName, String version,
                                            String serviceName, String componentName) throws AmbariException {
    ComponentInfo component = null;
    List<ComponentInfo> components = getComponentsByService(stackName, version, serviceName);
    if (components != null) {
      for (ComponentInfo cmp : components) {
        if (cmp.getName().equals(componentName)) {
          component = cmp;
          break;
        }
      }
    }
    return component;
  }

  /**
   * Get components by service
   *
   * @param stackName     stack name
   * @param version       stack version
   * @param serviceName   service name
   * @return List of ComponentInfo objects
   * @throws AmbariException
   */
  public List<ComponentInfo> getComponentsByService(String stackName, String version, String serviceName)
      throws AmbariException {

    ServiceInfo service = getServiceInfo(stackName, version, serviceName);
    if (service == null) {
      throw new ParentObjectNotFoundException("Parent Service resource doesn't exist. stackName=" +
          stackName + ", stackVersion=" + version + ", serviceName=" + serviceName);
    }

    return service.getComponents();
  }


  public ComponentInfo getComponent(String stackName, String version, String serviceName,
                                    String componentName) throws AmbariException {

    List<ComponentInfo> componentsByService = getComponentsByService(stackName, version, serviceName);

    if (componentsByService.size() == 0) {
      throw new StackAccessException("stackName=" + stackName
          + ", stackVersion=" + version
          + ", serviceName=" + serviceName
          + ", componentName=" + componentName);
    }

    ComponentInfo componentResult = null;

    for (ComponentInfo component : componentsByService) {
      if (component.getName().equals(componentName)) {
        componentResult = component;
      }
    }

    if (componentResult == null) {
      throw new StackAccessException("stackName=" + stackName
          + ", stackVersion=" + version
          + ", serviceName=" + serviceName
          + ", componentName=" + componentName);
    }

    return componentResult;
  }

  /**
   * Get all dependencies for a component.
   *
   * @param stackName  stack name
   * @param version    stack version
   * @param service    service name
   * @param component  component name
   *
   * @return List of dependencies for the given component
   * @throws AmbariException if unable to obtain the dependencies for the component
   */
  public List<DependencyInfo> getComponentDependencies(String stackName, String version,
                                                       String service, String component)
                                                       throws AmbariException {
    ComponentInfo componentInfo;
    try {
      componentInfo = getComponent(stackName, version, service, component);
    } catch (StackAccessException e) {
      throw new ParentObjectNotFoundException("Parent Component resource doesn't exist", e);
    }
    return componentInfo.getDependencies();
  }

  /**
   * Obtain a specific dependency by name.
   *
   * @param stackName       stack name
   * @param version         stack version
   * @param service         service name
   * @param component       component name
   * @param dependencyName  dependency component name
   *
   * @return the requested dependency
   * @throws AmbariException if unable to obtain the requested dependency
   */
  public DependencyInfo getComponentDependency(String stackName, String version, String service,
                                               String component, String dependencyName) throws AmbariException {

    DependencyInfo foundDependency = null;
    List<DependencyInfo> componentDependencies = getComponentDependencies(
        stackName, version, service, component);
    Iterator<DependencyInfo> iter = componentDependencies.iterator();
    while (foundDependency == null && iter.hasNext()) {
      DependencyInfo dependency = iter.next();
      if (dependencyName.equals(dependency.getComponentName())) {
        foundDependency = dependency;
      }
    }
    if (foundDependency == null) {
      throw new StackAccessException("stackName=" + stackName
          + ", stackVersion= " + version
          + ", stackService=" + service
          + ", stackComponent= " + component
          + ", dependency=" + dependencyName);
    }

    return foundDependency;
  }

  public Map<String, List<RepositoryInfo>> getRepository(String stackName,
                                                         String version) throws AmbariException {
    Map<String, List<RepositoryInfo>> reposResult = null;
    StackInfo stack = getStackInfo(stackName, version);
    if (stack != null) {
      List<RepositoryInfo> repository = stack.getRepositories();
      reposResult = new HashMap<String, List<RepositoryInfo>>();
      for (RepositoryInfo repo : repository) {
        if (!reposResult.containsKey(repo.getOsType())) {
          reposResult.put(repo.getOsType(),
              new ArrayList<RepositoryInfo>());
        }
        reposResult.get(repo.getOsType()).add(repo);
      }
    }
    return reposResult;
  }

  public List<RepositoryInfo> getRepositories(String stackName,
                                              String version, String osType) throws AmbariException {

    StackInfo stack = getStackInfo(stackName, version);
    List<RepositoryInfo> repositories = stack.getRepositories();

    List<RepositoryInfo> repositoriesResult = new ArrayList<RepositoryInfo>();
    for (RepositoryInfo repository : repositories) {
      if (repository.getOsType().equals(osType)) {
        repositoriesResult.add(repository);
      }
    }
    return repositoriesResult;
  }

  public RepositoryInfo getRepository(String stackName,
                                      String version, String osType, String repoId) throws AmbariException {

    List<RepositoryInfo> repositories = getRepositories(stackName, version, osType);

    if (repositories.size() == 0) {
      throw new StackAccessException("stackName=" + stackName
          + ", stackVersion=" + version
          + ", osType=" + osType
          + ", repoId=" + repoId);
    }

    RepositoryInfo repoResult = null;
    for (RepositoryInfo repository : repositories) {
      if (repository.getRepoId().equals(repoId)) {
        repoResult = repository;
      }
    }
    if (repoResult == null) {
      throw new StackAccessException("stackName=" + stackName
          + ", stackVersion= " + version
          + ", osType=" + osType
          + ", repoId= " + repoId);
    }
    return repoResult;
  }

  /*
   * function for given a stack name and version, is it a supported stack
   */
  public boolean isSupportedStack(String stackName, String version) throws AmbariException {
    boolean exist = false;
    try {
      StackInfo stackInfo = getStackInfo(stackName, version);
      if (stackInfo != null) {
        exist = true;
      }
    } catch (ObjectNotFoundException e) {
    }
    return exist;
  }

  /*
   * support isValidService(), isValidComponent for a given stack/version
   */
  public boolean isValidService(String stackName, String version,
                                String serviceName) throws AmbariException {

    boolean exist = false;
    try {
      ServiceInfo info= getServiceInfo(stackName, version, serviceName);
      if (info != null) {
        exist = true;
      }
    } catch (ObjectNotFoundException e) {
    }
    return exist;
  }

  /*
   * support isValidService(), isValidComponent for a given stack/version
   */
  public boolean isValidServiceComponent(String stackName, String version,
                                         String serviceName, String componentName) throws AmbariException {
    ServiceInfo service = getServiceInfo(stackName, version, serviceName);
    return service != null && service.getComponentByName(componentName) != null;
  }

  /**
   * Get the name of a service given the component name.
   *
   * @param stackName     the stack name
   * @param version       the stack version
   * @param componentName the component name
   * @return the service name
   * @throws AmbariException
   */
  public String getComponentToService(String stackName, String version,
                                      String componentName) throws AmbariException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Looking for service for component"
          + ", stackName=" + stackName
          + ", stackVersion=" + version
          + ", componentName=" + componentName);
    }
    Map<String, ServiceInfo> services = getServices(stackName, version);
    String retService = null;
    if (services == null
        || services.isEmpty()) {
      return retService;
    }
    for (Map.Entry<String, ServiceInfo> entry : services.entrySet()) {
      ComponentInfo vu = entry.getValue().getComponentByName(componentName);
      if(vu != null){
        retService = entry.getKey();
        break;
      }
    }
    return retService;
  }

  /**
   * Get the service configs supported for a service in a particular stack
   *
   * @param stackName   the stack name
   * @param version     the version of the stack
   * @param serviceName the name of the service in the stack
   * @return the config knobs supported for the service
   * @throws AmbariException
   */
  public Map<String, Map<String, String>> getSupportedConfigs(String stackName,
                                                              String version, String serviceName) throws AmbariException {
    Map<String, Map<String, String>> propertiesResult = new HashMap<String, Map<String, String>>();

    ServiceInfo service = getServiceInfo(stackName, version, serviceName);
    if (service != null) {
      if (serviceName.equals(service.getName())) {
        List<PropertyInfo> properties = service.getProperties();
        if (properties != null) {
          for (PropertyInfo propertyInfo : properties) {
            Map<String, String> fileProperties = propertiesResult
                .get(propertyInfo.getFilename());
            if (fileProperties == null) {
              fileProperties = new HashMap<String, String>();
              fileProperties.put(propertyInfo.getName(),
                  propertyInfo.getValue());
              propertiesResult.put(propertyInfo.getFilename(), fileProperties);

            } else {
              fileProperties.put(propertyInfo.getName(),
                  propertyInfo.getValue());
            }

          }
        }
      }
    }

    return propertiesResult;
  }

  /**
   * Given a stack name and version return all the services with info
   *
   * @param stackName the stack name
   * @param version   the version of the stack
   * @return the information of abt various services that are supported in the stack
   * @throws AmbariException
   */
  public Map<String, ServiceInfo> getServices(String stackName, String version) throws AmbariException {

    Map<String, ServiceInfo> servicesInfoResult = new HashMap<String, ServiceInfo>();

    List<ServiceInfo> services;
    StackInfo stack;
    try {
      stack = getStackInfo(stackName, version);
    } catch (StackAccessException e) {
      throw new ParentObjectNotFoundException("Parent Stack Version resource doesn't exist", e);
    }

    services = stack.getServices();
    if (services != null) {
      for (ServiceInfo service : services) {
        servicesInfoResult.put(service.getName(), service);
      }
    }
    return servicesInfoResult;
  }

  public ServiceInfo getService(String stackName, String version, String serviceName) throws AmbariException {

    Map<String, ServiceInfo> services = getServices(stackName, version);

    if (services.size() == 0) {
      throw new StackAccessException("stackName=" + stackName + ", stackVersion=" + version + ", serviceName=" + serviceName);
    }

    ServiceInfo serviceInfo = services.get(serviceName);

    if (serviceInfo == null) {
      throw new StackAccessException("stackName=" + stackName + ", stackVersion=" + version + ", serviceName=" + serviceName);
    }

    return serviceInfo;

  }

  public ServiceInfo getServiceInfo(String stackName, String version,
                                    String serviceName) throws AmbariException {
    ServiceInfo serviceInfoResult = null;
    List<ServiceInfo> services;
    StackInfo stack;
    try {
      stack = getStackInfo(stackName, version);
    } catch (StackAccessException e) {
      throw new ParentObjectNotFoundException("Parent Stack Version resource doesn't exist", e);
    }

    services = stack.getServices();
    if (services != null) {
      for (ServiceInfo service : services) {
        if (serviceName.equals(service.getName())) {
          serviceInfoResult = service;
          break;
        }
      }
    }
    return serviceInfoResult;
  }

  public List<ServiceInfo> getSupportedServices(String stackName, String version)
    throws AmbariException {
    List<ServiceInfo> servicesResult = null;
    StackInfo stack = getStackInfo(stackName, version);
    if (stack != null) {
      servicesResult = stack.getServices();
    }
    return servicesResult;
  }

  public List<String> getMonitoringServiceNames(String stackName, String version)
    throws AmbariException{

    List<String> monitoringServices = new ArrayList<String>();
    for (ServiceInfo service : getSupportedServices(stackName, version)) {
      if ((service.isMonitoringService() != null) &&
        service.isMonitoringService()) {
        monitoringServices.add(service.getName());
      }
    }
    return monitoringServices;
  }

  public Set<String> getRestartRequiredServicesNames(String stackName, String version)
    throws AmbariException{

    HashSet<String> needRestartServices = new HashSet<String>();

    List<ServiceInfo> serviceInfos = getSupportedServices(stackName, version);


    for (ServiceInfo service : serviceInfos) {
      if (service.isRestartRequiredAfterChange() != null && service.isRestartRequiredAfterChange()) {
        needRestartServices.add(service.getName());
      }
    }
    return needRestartServices;
  }

  public List<StackInfo> getSupportedStacks() {
    return stacksResult;
  }

  public Set<Stack> getStackNames() {

    Set<Stack> stacks = new HashSet<Stack>();
    List<StackInfo> supportedStacks = getSupportedStacks();

    for (StackInfo stackInfo : supportedStacks) {
      Stack stack = new Stack(stackInfo.getName());
      stacks.add(stack);
    }

    return stacks;
  }

  public Stack getStack(String stackName) throws AmbariException {

    Set<Stack> supportedStackNames = getStackNames();

    if (supportedStackNames.size() == 0) {
      throw new StackAccessException("stackName=" + stackName);
    }

    Stack stackResult = null;

    for (Stack stack : supportedStackNames) {
      if (stack.getStackName().equals(stackName)) {
        stackResult = stack;
      }
    }

    if (stackResult == null) {
      throw new StackAccessException("stackName=" + stackName);
    }

    return stackResult;
  }

  public Set<StackInfo> getStackInfos(String stackName) {

    Set<StackInfo> stackVersions = new HashSet<StackInfo>();
    for (StackInfo stackInfo : stacksResult) {
      if (stackName.equals(stackInfo.getName())) {
        stackVersions.add(stackInfo);
      }
    }
    return stackVersions;
  }

  public StackInfo getStackInfo(String stackName, String version) throws AmbariException {
    StackInfo stackInfoResult = null;

    for (StackInfo stack : stacksResult) {
      if (stackName.equals(stack.getName())
          && version.equals(stack.getVersion())) {
        stackInfoResult = stack;
        break;
      }
    }

    if (stackInfoResult == null) {
      throw new StackAccessException("stackName=" + stackName
          + ", stackVersion=" + version);
    }

    return stackInfoResult;
  }

  public Set<PropertyInfo> getProperties(String stackName, String version, String serviceName)
      throws AmbariException {

    ServiceInfo serviceInfo = getServiceInfo(stackName, version, serviceName);
    List<PropertyInfo> properties = serviceInfo.getProperties();
    Set<PropertyInfo> propertiesResult = new HashSet<PropertyInfo>(properties);

    return propertiesResult;
  }

  public Set<PropertyInfo> getPropertiesByName(String stackName, String version, String serviceName, String propertyName)
      throws AmbariException {
    Set<PropertyInfo> properties = getProperties(stackName, version, serviceName);

    if (properties.size() == 0) {
      throw new StackAccessException("stackName=" + stackName
          + ", stackVersion=" + version
          + ", serviceName=" + serviceName
          + ", propertyName=" + propertyName);
    }

    Set<PropertyInfo> propertyResult = new HashSet<PropertyInfo>();

    for (PropertyInfo property : properties) {
      if (property.getName().equals(propertyName)) {
        propertyResult.add(property);
      }
    }

    if (propertyResult.isEmpty()) {
      throw new StackAccessException("stackName=" + stackName
          + ", stackVersion=" + version
          + ", serviceName=" + serviceName
          + ", propertyName=" + propertyName);
    }

    return propertyResult;
  }


  /**
   * Lists operatingsystems supported by stack
   */
  public Set<OperatingSystemInfo> getOperatingSystems(String stackName, String version)
      throws AmbariException {

    Set<OperatingSystemInfo> operatingSystems = new HashSet<OperatingSystemInfo>();
    StackInfo stack = getStackInfo(stackName, version);
    List<RepositoryInfo> repositories = stack.getRepositories();
    for (RepositoryInfo repository : repositories) {
      operatingSystems.add(new OperatingSystemInfo(repository.getOsType()));
    }

    return operatingSystems;
  }

  public OperatingSystemInfo getOperatingSystem(String stackName, String version, String osType)
      throws AmbariException {

    Set<OperatingSystemInfo> operatingSystems = getOperatingSystems(stackName, version);

    if (operatingSystems.size() == 0) {
      throw new StackAccessException("stackName=" + stackName
          + ", stackVersion=" + version
          + ", osType=" + osType);
    }

    OperatingSystemInfo resultOperatingSystem = null;

    for (OperatingSystemInfo operatingSystem : operatingSystems) {
      if (operatingSystem.getOsType().equals(osType)) {
        resultOperatingSystem = operatingSystem;
      }
    }

    if (resultOperatingSystem == null) {
      throw new StackAccessException("stackName=" + stackName
          + ", stackVersion=" + version
          + ", osType=" + osType);
    }

    return resultOperatingSystem;
  }

  private void readServerVersion() throws Exception {
    File versionFile = serverVersionFile;
    if (!versionFile.exists()) {
      throw new AmbariException("Server version file does not exist.");
    }
    serverVersion = new Scanner(versionFile).useDelimiter("\\Z").next();
  }

  private void getCustomActionDefinitions(File customActionDefinitionRoot) throws JAXBException, AmbariException {
    if (customActionDefinitionRoot != null) {
      LOG.debug("Loading custom action definitions from "
          + customActionDefinitionRoot.getAbsolutePath());

      if (customActionDefinitionRoot.exists() && customActionDefinitionRoot.isDirectory()) {
        adManager.readCustomActionDefinitions(customActionDefinitionRoot);
      } else {
        LOG.debug("No action definitions found at " + customActionDefinitionRoot.getAbsolutePath());
      }
    }
  }

  /**
   * Get all action definitions
   */
  public List<ActionDefinition> getAllActionDefinition(){
    return adManager.getAllActionDefinition();
  }

  /**
   * Get action definitions based on the supplied name
   */
  public ActionDefinition getActionDefinition(String name){
    return adManager.getActionDefinition(name);
  }


  /**
   * Used for test purposes
   */
  public void addActionDefinition(ActionDefinition ad) throws AmbariException {
    adManager.addActionDefinition(ad);
  }

  private void getConfigurationInformation(File stackRoot) throws Exception {
    String stackRootAbsPath = stackRoot.getAbsolutePath();
    if (LOG.isDebugEnabled()) {
      LOG.debug("Loading stack information"
        + ", stackRoot = " + stackRootAbsPath);
    }

    if (!stackRoot.isDirectory() && !stackRoot.exists()) {
      throw new IOException("" + Configuration.METADETA_DIR_PATH
        + " should be a directory with stack"
        + ", stackRoot = " + stackRootAbsPath);
    }

    StackExtensionHelper stackExtensionHelper = new StackExtensionHelper(injector, stackRoot);
    stackExtensionHelper.fillInfo();

    List<StackInfo> stacks = stackExtensionHelper.getAllAvailableStacks();
    if (stacks.isEmpty()) {
      throw new AmbariException("Unable to find stack definitions under " +
        "stackRoot = " + stackRootAbsPath);
    }

    ExecutorService es = Executors.newSingleThreadExecutor(new ThreadFactory() {
      @Override
      public Thread newThread(Runnable r) {
        return new Thread(r, "Stack Version Loading Thread");
      }
    });

    List<LatestRepoCallable> lookupList = new ArrayList<LatestRepoCallable>();

    for (StackInfo stack : stacks) {
      LOG.debug("Adding new stack to known stacks"
        + ", stackName = " + stack.getName()
        + ", stackVersion = " + stack.getVersion());

      stacksResult.add(stack);

      String stackPath = stackRootAbsPath + File.separator +
              stack.getName() + File.separator + stack.getVersion();

      // get repository data for current stack of techs
      File repositoryFolder = new File(stackPath
        + File.separator + REPOSITORY_FOLDER_NAME + File.separator
        + REPOSITORY_FILE_NAME);

      if (repositoryFolder.exists()) {
        LOG.debug("Adding repositories to stack"
          + ", stackName=" + stack.getName()
          + ", stackVersion=" + stack.getVersion()
          + ", repoFolder=" + repositoryFolder.getPath());

        List<RepositoryInfo> repositoryInfoList = getRepository(repositoryFolder,
            stack, lookupList);

        stack.getRepositories().addAll(repositoryInfoList);
      } else {
        LOG.warn("No repository information defined for "
          + ", stackName=" + stack.getName()
          + ", stackVersion=" + stack.getVersion()
          + ", repoFolder=" + repositoryFolder.getPath());
      }

      // Populate services
      List<ServiceInfo> services = stackExtensionHelper.getAllApplicableServices(stack);
      stack.setServices(services);

      Map<String, Map<String, PropertyInfo>> stackRequiredProps = new HashMap<String, Map<String, PropertyInfo>>();
      requiredProperties.put(new StackId(stack.getName(), stack.getVersion()), stackRequiredProps);
      for (ServiceInfo service : services) {
        // Set required config properties
        stackRequiredProps.put(service.getName(), getAllRequiredProperties(service));
      }

      // Resolve hooks folder
      String stackHooksToUse = stackExtensionHelper.resolveHooksFolder(stack);
      stack.setStackHooksFolder(stackHooksToUse);
    }

    es.invokeAll(lookupList);

    es.shutdown();
  }

  /**
   * Get properties with require_input attribute set to true.
   *
   * @param stackName     name of the stack, e.g.: HDP
   * @param stackVersion  version of the stack
   * @return Map of property name to PropertyInfo
   */
  public Map<String, PropertyInfo> getRequiredProperties(String stackName, String stackVersion, String service) {

    Map<String, Map<String, PropertyInfo>> requiredStackProps =
        requiredProperties.get(new StackId(stackName, stackVersion));

    if (requiredStackProps != null) {
      Map<String, PropertyInfo> requiredServiceProperties = requiredStackProps.get(service);
      return requiredServiceProperties == null ? Collections.<String, PropertyInfo>emptyMap() :
                                                 requiredServiceProperties;
    }
    return Collections.emptyMap();
  }

  public String getServerVersion() {
    return serverVersion;
  }

  private List<RepositoryInfo> getRepository(File repositoryFile, StackInfo stack,
      List<LatestRepoCallable> lookupList)
      throws JAXBException {

    RepositoryXml rxml = StackExtensionHelper.unmarshal(RepositoryXml.class, repositoryFile);

    List<RepositoryInfo> list = new ArrayList<RepositoryInfo>();

    for (Os o : rxml.getOses()) {
      for (String os : o.getType().split(",")) {
        for (Repo r : o.getRepos()) {
          RepositoryInfo ri = new RepositoryInfo();
          ri.setBaseUrl(r.getBaseUrl());
          ri.setDefaultBaseUrl(r.getBaseUrl());
          ri.setMirrorsList(r.getMirrorsList());
          ri.setOsType(os.trim());
          ri.setRepoId(r.getRepoId());
          ri.setRepoName(r.getRepoName());
          ri.setLatestBaseUrl(r.getBaseUrl());

          if (null != metainfoDAO) {
            LOG.debug("Checking for override for base_url");
            String key = generateRepoMetaKey(r.getRepoName(), stack.getVersion(),
                o.getType(), r.getRepoId(), REPOSITORY_XML_PROPERTY_BASEURL);
            MetainfoEntity entity = metainfoDAO.findByKey(key);
            if (null != entity) {
              ri.setBaseUrl(entity.getMetainfoValue());
            }
          }

          if (LOG.isDebugEnabled()) {
            LOG.debug("Adding repo to stack"
                + ", repoInfo=" + ri.toString());
          }

          list.add(ri);
        }
      }
    }

    if (null != rxml.getLatestURI() && list.size() > 0) {
      lookupList.add(new LatestRepoCallable(rxml.getLatestURI(),
          repositoryFile.getParentFile(), stack));
    }

    return list;

  }

  public boolean isOsSupported(String osType) {
    return ALL_SUPPORTED_OS.contains(osType);
  }

  /**
   * Returns a suitable key for use with stack url overrides.
   * @param stackName the stack name
   * @param stackVersion the stack version
   * @param osType the os
   * @param repoId the repo id
   * @param field the field name
   * @return the key for any repo value override
   */
  public String generateRepoMetaKey(String stackName, String stackVersion,
      String osType, String repoId, String field) {

    StringBuilder sb = new StringBuilder("repo:/");
    sb.append(stackName).append('/');
    sb.append(stackVersion).append('/');
    sb.append(osType).append('/');
    sb.append(repoId);
    sb.append(':').append(field);

    return sb.toString();
  }

  /**
   * @param stackName the stack name
   * @param stackVersion the stack version
   * @param osType the os
   * @param repoId the repo id
   * @param newBaseUrl the new base url
   */
  public void updateRepoBaseURL(String stackName,
      String stackVersion, String osType, String repoId, String newBaseUrl) throws AmbariException {

    // validate existing
    RepositoryInfo ri = getRepository(stackName, stackVersion, osType, repoId);

    if (!stackRoot.exists()) {
      throw new StackAccessException("Stack root does not exist.");
    }

    ri.setBaseUrl(newBaseUrl);

    if (null != metainfoDAO) {
      String metaKey = generateRepoMetaKey(stackName, stackVersion, osType,
          repoId, REPOSITORY_XML_PROPERTY_BASEURL);

      MetainfoEntity entity = new MetainfoEntity();
      entity.setMetainfoName(metaKey);
      entity.setMetainfoValue(newBaseUrl);

      if (null != ri.getDefaultBaseUrl() && newBaseUrl.equals(ri.getDefaultBaseUrl())) {
        metainfoDAO.remove(entity);
      } else {
        metainfoDAO.merge(entity);
      }
    }
  }

  public File getStackRoot() {
    return stackRoot;
  }

  /**
   * Gets the metrics for a Role (component).
   * @return the list of defined metrics.
   */
  public List<MetricDefinition> getMetrics(String stackName, String stackVersion,
      String serviceName, String componentName, String metricType)
  throws AmbariException {

    ServiceInfo svc = getService(stackName, stackVersion, serviceName);

    if (null == svc.getMetricsFile() || !svc.getMetricsFile().exists()) {
      LOG.debug("Metrics file for " + stackName + "/" + stackVersion + "/" + serviceName + " not found.");
      return null;
    }

    Map<String, Map<String, List<MetricDefinition>>> map = svc.getMetrics();

    // check for cached
    if (null == map) {
      // data layout:
      // "DATANODE" -> "Component" -> [ MetricDefinition, MetricDefinition, ... ]
      //           \-> "HostComponent" -> [ MetricDefinition, ... ]
      Type type = new TypeToken<Map<String, Map<String, List<MetricDefinition>>>>(){}.getType();

      Gson gson = new Gson();

      try {
        map = gson.fromJson(new FileReader(svc.getMetricsFile()), type);

        svc.setMetrics(map);

      } catch (Exception e) {
        LOG.error ("Could not read the metrics file", e);
        throw new AmbariException("Could not read metrics file", e);
      }
    }

    if (map.containsKey(componentName)) {
      if (map.get(componentName).containsKey(metricType)) {
        return map.get(componentName).get(metricType);
      }
    }

	  return null;
  }

  /**
   * Get all required properties for the given service.
   *
   * @param service  associated service
   * @return map of property name to PropertyInfo containing all required properties for service
   */
  private Map<String, PropertyInfo> getAllRequiredProperties(ServiceInfo service) {
    Map<String, PropertyInfo> requiredProperties = new HashMap<String, PropertyInfo>();
    List<PropertyInfo> properties = service.getProperties();
    for (PropertyInfo propertyInfo : properties) {
      if (propertyInfo.isRequireInput()) {
        requiredProperties.put(propertyInfo.getName(), propertyInfo);
      }
    }
    return requiredProperties;
  }

  /**
   * @param stackName the stack name
   * @param stackVersion the stack version
   * @param serviceName the service name
   * @return the alert definitions for a stack
   * @throws AmbariException
   */
  public Set<AlertDefinition> getAlertDefinitions(String stackName, String stackVersion,
      String serviceName) throws AmbariException {

    ServiceInfo svc = getService(stackName, stackVersion, serviceName);
    File alertsFile = svc.getAlertsFile();

    if (null == alertsFile || !alertsFile.exists()) {
      LOG.debug("Alerts file for " + stackName + "/" + stackVersion + "/" + serviceName + " not found.");
      return null;
    }

    Set<AlertDefinition> defs = new HashSet<AlertDefinition>();
    Map<String, List<AlertDefinition>> map = alertDefinitionFactory.getAlertDefinitions(alertsFile);

    for (Entry<String, List<AlertDefinition>> entry : map.entrySet()) {
      for (AlertDefinition ad : entry.getValue()) {
        ad.setServiceName(serviceName);
        if (!entry.getKey().equals("service")) {
          ad.setComponentName(entry.getKey());
        }
      }
      defs.addAll(entry.getValue());
    }

    return defs;
  }
}
