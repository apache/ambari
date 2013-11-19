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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.StackAccessException;
import org.apache.ambari.server.api.util.StackExtensionHelper;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.dao.MetainfoDAO;
import org.apache.ambari.server.orm.entities.MetainfoEntity;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.OperatingSystemInfo;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.Stack;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.stack.MetricDefinition;
import org.apache.ambari.server.state.stack.RepositoryXml;
import org.apache.ambari.server.state.stack.RepositoryXml.Os;
import org.apache.ambari.server.state.stack.RepositoryXml.Repo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 * ServiceInfo responsible getting information about cluster.
 */
@Singleton
public class AmbariMetaInfo {

  private final static Logger LOG = LoggerFactory.getLogger(AmbariMetaInfo.class);

  public static final String STACK_METAINFO_FILE_NAME = "metainfo.xml";
  public static final String SERVICES_FOLDER_NAME = "services";
  public static final String SERVICE_METAINFO_FILE_NAME = "metainfo.xml";
  public static final String SERVICE_CONFIG_FOLDER_NAME = "configuration";
  public static final String SERVICE_CONFIG_FILE_NAME_POSTFIX = ".xml";
  public static final String RCO_FILE_NAME = "role_command_order.json";
  private static final String REPOSITORY_FILE_NAME = "repoinfo.xml";
  private static final String REPOSITORY_FOLDER_NAME = "repos";
  private static final String REPOSITORY_XML_PROPERTY_BASEURL = "baseurl";
  // all the supported OS'es
  private static final List<String> ALL_SUPPORTED_OS = Arrays.asList(
      "centos5", "redhat5", "centos6", "redhat6", "oraclelinux5",
      "oraclelinux6", "suse11", "sles11", "ubuntu12");
  
  public static final String SERVICE_METRIC_FILE_NAME = "metrics.json";

  public static final FilenameFilter FILENAME_FILTER = new FilenameFilter() {
    @Override
    public boolean accept(File dir, String s) {
      if (s.equals(".svn") || s.equals(".git")
              || s.endsWith("_")) // Temporary hack: ignore such names
        return false;
      return true;
    }
  };

  private String serverVersion = "undefined";
  private List<StackInfo> stacksResult = new ArrayList<StackInfo>();
  private File stackRoot;
  private File serverVersionFile;

  @Inject
  private MetainfoDAO metainfoDAO;

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
    this.stackRoot = new File(stackPath);
    this.serverVersionFile = new File(serverVersionFilePath);
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
  }

  /**
   * Get component category
   *
   * @param stackName
   * @param version
   * @param serviceName
   * @param componentName
   * @return component component Info
   * @throws AmbariException
   */
  public ComponentInfo getComponentCategory(String stackName, String version,
                                            String serviceName, String componentName) throws AmbariException {
    ComponentInfo component = null;
    List<ComponentInfo> components = getComponentsByService(stackName, version,
        serviceName);
    if (components != null)
      for (ComponentInfo cmp : components) {
        if (cmp.getName().equals(componentName)) {
          component = cmp;
          break;
        }
      }
    return component;
  }

  /**
   * Get components by service
   *
   * @param stackName
   * @param version
   * @param serviceName
   * @return
   * @throws AmbariException
   */
  public List<ComponentInfo> getComponentsByService(String stackName,
                                                    String version, String serviceName) throws AmbariException {
    List<ComponentInfo> componentsResult = null;
    ServiceInfo service = getServiceInfo(stackName, version, serviceName);
    if (service != null)
      componentsResult = service.getComponents();

    return componentsResult;
  }

  public ComponentInfo getComponent(String stackName, String version, String serviceName,
                                    String componentName) throws AmbariException {

    List<ComponentInfo> componentsByService = getComponentsByService(stackName, version, serviceName);

    if (componentsByService.size() == 0)
      throw new StackAccessException("stackName=" + stackName
          + ", stackVersion=" + version
          + ", stackVersion=" + serviceName
          + ", componentName=" + componentName);

    ComponentInfo componentResult = null;

    for (ComponentInfo component : componentsByService) {
      if (component.getName().equals(componentName))
        componentResult = component;
    }

    if (componentResult == null)
      throw new StackAccessException("stackName=" + stackName
          + ", stackVersion=" + version
          + ", stackVersion=" + serviceName
          + ", componentName=" + componentName);

    return componentResult;
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
      if (repository.getOsType().equals(osType))
        repositoriesResult.add(repository);
    }
    return repositoriesResult;
  }

  public RepositoryInfo getRepository(String stackName,
                                      String version, String osType, String repoId) throws AmbariException {

    List<RepositoryInfo> repositories = getRepositories(stackName, version, osType);

    if (repositories.size() == 0)
      throw new StackAccessException("stackName=" + stackName
          + ", stackVersion=" + version
          + ", osType=" + osType
          + ", repoId=" + repoId);

    RepositoryInfo repoResult = null;
    for (RepositoryInfo repository : repositories) {
      if (repository.getRepoId().equals(repoId))
        repoResult = repository;
    }
    if (repoResult == null)
      throw new StackAccessException("stackName=" + stackName
          + ", stackName= " + version
          + ", osType=" + osType
          + ", repoId= " + repoId);
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
    if (service == null) {
      return false;
    }
    for (ComponentInfo compInfo : service.getComponents()) {
      if (compInfo.getName().equals(componentName)) {
        return true;
      }
    }
    return false;
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
    boolean found = false;
    for (Map.Entry<String, ServiceInfo> entry : services.entrySet()) {
      for (ComponentInfo compInfo : entry.getValue().getComponents()) {
        if (compInfo.getName().equals(componentName)) {
          retService = entry.getKey();
          found = true;
          break;
        }
      }
      if (found)
        break;
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
    if (service != null)
      if (serviceName.equals(service.getName())) {
        List<PropertyInfo> properties = service.getProperties();
        if (properties != null)
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

    return propertiesResult;
  }

  /**
   * Given a stack name and version return all the services with info
   *
   * @param stackName the stack name
   * @param version   the version of the stack
   * @return the information of abt varios services that are supported in the
   *         stack
   * @throws AmbariException
   */
  public Map<String, ServiceInfo> getServices(String stackName, String version) throws AmbariException {

    Map<String, ServiceInfo> servicesInfoResult = new HashMap<String, ServiceInfo>();

    List<ServiceInfo> services = null;
    StackInfo stack = getStackInfo(stackName, version);
    if (stack == null)
      return null;
    services = stack.getServices();
    if (services != null)
      for (ServiceInfo service : services) {
        servicesInfoResult.put(service.getName(), service);
      }
    return servicesInfoResult;
  }

  public ServiceInfo getService(String stackName, String version, String serviceName) throws AmbariException {

    Map<String, ServiceInfo> services = getServices(stackName, version);

    if (services.size() == 0)
      throw new StackAccessException("stackName=" + stackName + ", stackVersion=" + version + ", serviceName=" + serviceName);

    ServiceInfo serviceInfo = services.get(serviceName);

    if (serviceInfo == null)
      throw new StackAccessException("stackName=" + stackName + ", stackVersion=" + version + ", serviceName=" + serviceName);

    return serviceInfo;

  }

  public ServiceInfo getServiceInfo(String stackName, String version,
                                    String serviceName) throws AmbariException {
    ServiceInfo serviceInfoResult = null;
    List<ServiceInfo> services = null;
    StackInfo stack = getStackInfo(stackName, version);
    if (stack == null)
      return null;
    services = stack.getServices();
    if (services != null)
      for (ServiceInfo service : services) {
        if (serviceName.equals(service.getName())) {
          serviceInfoResult = service;
          break;
        }
      }
    return serviceInfoResult;
  }

  public List<ServiceInfo> getSupportedServices(String stackName, String version) throws AmbariException {
    List<ServiceInfo> servicesResulr = null;
    StackInfo stack = getStackInfo(stackName, version);
    if (stack != null)
      servicesResulr = stack.getServices();
    return servicesResulr;
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

    if (supportedStackNames.size() == 0)
      throw new StackAccessException("stackName=" + stackName);

    Stack stackResult = null;

    for (Stack stack : supportedStackNames) {
      if (stack.getStackName().equals(stackName))
        stackResult = stack;
    }

    if (stackResult == null)
      throw new StackAccessException("stackName=" + stackName);

    return stackResult;
  }

  public Set<StackInfo> getStackInfos(String stackName) {

    Set<StackInfo> stackVersions = new HashSet<StackInfo>();
    for (StackInfo stackInfo : stacksResult) {
      if (stackName.equals(stackInfo.getName()))
        stackVersions.add(stackInfo);
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

    if (stackInfoResult == null)
      throw new StackAccessException("stackName=" + stackName
          + ", stackVersion=" + version);

    return stackInfoResult;
  }

  public Set<PropertyInfo> getProperties(String stackName, String version, String serviceName)
      throws AmbariException {

    ServiceInfo serviceInfo = getServiceInfo(stackName, version, serviceName);
    List<PropertyInfo> properties = serviceInfo.getProperties();
    Set<PropertyInfo> propertiesResult = new HashSet<PropertyInfo>(properties);

    return propertiesResult;
  }

  public PropertyInfo getProperty(String stackName, String version, String serviceName, String propertyName)
      throws AmbariException {
    Set<PropertyInfo> properties = getProperties(stackName, version, serviceName);

    if (properties.size() == 0)
      throw new StackAccessException("stackName=" + stackName
          + ", stackVersion=" + version
          + ", serviceName=" + serviceName
          + ", propertyName=" + propertyName);

    PropertyInfo propertyResult = null;

    for (PropertyInfo property : properties) {
      if (property.getName().equals(propertyName))
        propertyResult = property;
    }

    if (propertyResult == null)
      throw new StackAccessException("stackName=" + stackName
          + ", stackVersion=" + version
          + ", serviceName=" + serviceName
          + ", propertyName=" + propertyName);

    return propertyResult;
  }

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

    if (operatingSystems.size() == 0)
      throw new StackAccessException("stackName=" + stackName
          + ", stackVersion=" + version
          + ", osType=" + osType);

    OperatingSystemInfo resultOperatingSystem = null;

    for (OperatingSystemInfo operatingSystem : operatingSystems) {
      if (operatingSystem.getOsType().equals(osType))
        resultOperatingSystem = operatingSystem;
    }

    if (resultOperatingSystem == null)
      throw new StackAccessException("stackName=" + stackName
          + ", stackVersion=" + version
          + ", osType=" + osType);

    return resultOperatingSystem;
  }

  private void readServerVersion() throws Exception {
    File versionFile = this.serverVersionFile;
    if (!versionFile.exists()) {
      throw new AmbariException("Server version file does not exist.");
    }
    serverVersion = new Scanner(versionFile).useDelimiter("\\Z").next();
  }

  private void getConfigurationInformation(File stackRoot) throws Exception {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Loading stack information"
        + ", stackRoot = " + stackRoot.getAbsolutePath());
    }

    if (!stackRoot.isDirectory() && !stackRoot.exists())
      throw new IOException("" + Configuration.METADETA_DIR_PATH
        + " should be a directory with stack"
        + ", stackRoot = " + stackRoot.getAbsolutePath());

    StackExtensionHelper stackExtensionHelper = new StackExtensionHelper
      (stackRoot);

    List<StackInfo> stacks = stackExtensionHelper.getAllAvailableStacks();
    if (stacks.isEmpty()) {
      throw new AmbariException("Unable to find stack definitions under " +
        "stackRoot = " + stackRoot.getAbsolutePath());
    }

    for (StackInfo stack : stacks) {
      LOG.debug("Adding new stack to known stacks"
        + ", stackName = " + stack.getName()
        + ", stackVersion = " + stack.getVersion());

      stacksResult.add(stack);

      // get repository data for current stack of techs
      File repositoryFolder = new File(stackRoot.getAbsolutePath()
        + File.separator + stack.getName() + File.separator + stack.getVersion()
        + File.separator + REPOSITORY_FOLDER_NAME + File.separator
        + REPOSITORY_FILE_NAME);

      if (repositoryFolder.exists()) {
        LOG.debug("Adding repositories to stack"
          + ", stackName=" + stack.getName()
          + ", stackVersion=" + stack.getVersion()
          + ", repoFolder=" + repositoryFolder.getPath());

        List<RepositoryInfo> repositoryInfoList = getRepository
          (repositoryFolder, stack.getVersion());

        stack.getRepositories().addAll(repositoryInfoList);
      } else {
        LOG.warn("No repository information defined for "
          + ", stackName=" + stack.getName()
          + ", stackVersion=" + stack.getVersion()
          + ", repoFolder=" + repositoryFolder.getPath());
      }

      List<ServiceInfo> services = stackExtensionHelper
        .getAllApplicableServices(stack);

      stack.setServices(services);
    }
  }

  public String getServerVersion() {
    return serverVersion;
  }

  private List<RepositoryInfo> getRepository(File repositoryFile, String stackVersion)
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

          if (null != metainfoDAO) {
            LOG.debug("Checking for override for base_url");
            String key = generateRepoMetaKey(r.getRepoName(), stackVersion,
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

    return list;

  }

  public boolean isOsSupported(String osType) {
    return ALL_SUPPORTED_OS.contains(osType);
  }

  private String generateRepoMetaKey(String stackName, String stackVersion,
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

    if (!stackRoot.exists())
      throw new StackAccessException("Stack root does not exist.");

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

}
