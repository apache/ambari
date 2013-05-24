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

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.StackAccessException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.resources.ResourceManager;
import org.apache.ambari.server.state.*;
import org.apache.ambari.server.state.Stack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;

/**
 * ServiceInfo responsible getting information about cluster.
 */
@Singleton
public class AmbariMetaInfo {

  private final static Logger LOG = LoggerFactory
      .getLogger(AmbariMetaInfo.class);
  private static final String STACK_METAINFO_FILE_NAME = "metainfo.xml";
  private static final String STACK_XML_MAIN_BLOCK_NAME = "metainfo";
  private static final String STACK_XML_PROPERTY_UPGRADE = "upgrade";
  private static final String STACK_XML_PROPERTY_ACTIVE = "active";
  private static final String SERVICES_FOLDER_NAME = "services";
  private static final String SERVICE_METAINFO_FILE_NAME = "metainfo.xml";
  private static final String SERVICE_CONFIG_FOLDER_NAME = "configuration";
  private static final String SERVICE_CONFIG_FILE_NAME_POSTFIX = ".xml";
  private static final String REPOSITORY_FILE_NAME = "repoinfo.xml";
  private static final String REPOSITORY_FOLDER_NAME = "repos";
  private static final String REPOSITORY_XML_MAIN_BLOCK_NAME = "os";
  private static final String REPOSITORY_XML_ATTRIBUTE_OS_TYPE = "type";
  private static final String REPOSITORY_XML_REPO_BLOCK_NAME = "repo";
  private static final String REPOSITORY_XML_PROPERTY_BASEURL = "baseurl";
  private static final String REPOSITORY_XML_PROPERTY_REPOID = "repoid";
  private static final String REPOSITORY_XML_PROPERTY_REPONAME = "reponame";
  private static final String REPOSITORY_XML_PROPERTY_MIRRORSLIST = "mirrorslist";
  private static final String METAINFO_XML_MAIN_BLOCK_NAME = "metainfo";
  private static final String METAINFO_XML_PROPERTY_VERSION = "version";
  private static final String METAINFO_XML_PROPERTY_USER = "user";
  private static final String METAINFO_XML_PROPERTY_COMMENT = "comment";
  private static final String METAINFO_XML_PROPERTY_COMPONENT_MAIN = "component";
  private static final String METAINFO_XML_PROPERTY_COMPONENT_NAME = "name";
  private static final String METAINFO_XML_PROPERTY_COMPONENT_CATEGORY = "category";
  private static final String PROPERTY_XML_MAIN_BLOCK_NAME = "property";
  private static final String PROPERTY_XML_PROPERTY_NAME = "name";
  private static final String PROPERTY_XML_PROPERTY_VALUE = "value";
  private static final String PROPERTY_XML_PROPERTY_DESCRIPTION = "description";
  private static final FilenameFilter FILENAME_FILTER = new FilenameFilter() {
    @Override
    public boolean accept(File dir, String s) {
      if (s.equals(".svn") || s.equals(".git"))
        return false;
      return true;
    }
  };

  private String serverVersion = "undefined";
  private List<StackInfo> stacksResult = new ArrayList<StackInfo>();
  private File stackRoot;
  private File serverVersionFile;

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

  @Inject
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
      getStackInfo(stackName, version);
      exist = true;
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
      getServiceInfo(stackName, version, serviceName);
      exist = true;
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
          + ", stackRoot=" + stackRoot.getAbsolutePath());
    }

    if (!stackRoot.isDirectory() && !stackRoot.exists())
      throw new IOException("" + Configuration.METADETA_DIR_PATH
          + " should be a directory with stack"
          + ", stackRoot=" + stackRoot.getAbsolutePath());
    File[] stacks = stackRoot.listFiles(FILENAME_FILTER);
    for (File stackFolder : stacks) {
      if (stackFolder.isFile())
        continue;
      File[] concretStacks = stackFolder.listFiles(FILENAME_FILTER);
      for (File stack : concretStacks) {
        if (stack.isFile())
          continue;

        StackInfo stackInfo = getStackInfo(stack);
        if (LOG.isDebugEnabled()) {
          LOG.debug("Adding new stack to known stacks"
              + ", stackName=" + stackFolder.getName()
              + ", stackVersion=" + stack.getName());
        }

        stacksResult.add(stackInfo);
        // get repository data for current stack of techs
        File repositoryFolder = new File(stack.getAbsolutePath()
            + File.separator + REPOSITORY_FOLDER_NAME + File.separator
            + REPOSITORY_FILE_NAME);

        if (repositoryFolder.exists()) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Adding repositories to stack"
                + ", stackName=" + stackFolder.getName()
                + ", stackVersion=" + stack.getName()
                + ", repoFolder=" + repositoryFolder.getPath());
          }
          List<RepositoryInfo> repositoryInfoList = getRepository(repositoryFolder);
          stackInfo.getRepositories().addAll(repositoryInfoList);
        }

        // Get services for this stack
        File servicesRootFolder = new File(stack.getAbsolutePath()
            + File.separator + SERVICES_FOLDER_NAME);
        File[] servicesFolders = servicesRootFolder.listFiles(FILENAME_FILTER);

        if (servicesFolders != null) {
          for (File serviceFolder : servicesFolders) {
            // Get information about service
            ServiceInfo serviceInfo = new ServiceInfo();
            serviceInfo.setName(serviceFolder.getName());
            stackInfo.getServices().add(serviceInfo);

            if (LOG.isDebugEnabled()) {
              LOG.debug("Adding new service to stack"
                  + ", stackName=" + stackFolder.getName()
                  + ", stackVersion=" + stack.getName()
                  + ", serviceName=" + serviceInfo.getName());
            }

            // Get metainfo data from metainfo.xml
            File metainfoFile = new File(serviceFolder.getAbsolutePath()
                + File.separator + SERVICE_METAINFO_FILE_NAME);
            if (metainfoFile.exists()) {
              setMetaInfo(metainfoFile, serviceInfo);
            }

            // Get all properties from all "configs/*-site.xml" files
            File serviceConfigFolder = new File(serviceFolder.getAbsolutePath()
                + File.separator + SERVICE_CONFIG_FOLDER_NAME);
            File[] configFiles = serviceConfigFolder.listFiles(FILENAME_FILTER);
            if (configFiles != null) {
              for (File config : configFiles) {
                if (config.getName().endsWith(SERVICE_CONFIG_FILE_NAME_POSTFIX)) {
                  serviceInfo.getProperties().addAll(getProperties(config));
                }
              }
            }
          }
        }
      }
    }
  }

  public String getServerVersion() {
    return serverVersion;
  }

  private StackInfo getStackInfo(File stackVersionFolder) {

    StackInfo stackInfo = new StackInfo();

    stackInfo.setName(stackVersionFolder.getParentFile().getName());
    stackInfo.setVersion(stackVersionFolder.getName());

    // Get metainfo from file
    File stackMetainfoFile = new File(stackVersionFolder.getAbsolutePath()
        + File.separator + STACK_METAINFO_FILE_NAME);

    if (stackMetainfoFile.exists()) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Reading stack version metainfo from file "
            + stackMetainfoFile.getAbsolutePath());
      }

      try {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(stackMetainfoFile);
        doc.getDocumentElement().normalize();

        NodeList stackNodes = doc
            .getElementsByTagName(STACK_XML_MAIN_BLOCK_NAME);

        for (int index = 0; index < stackNodes.getLength(); index++) {

          Node node = stackNodes.item(index);

          if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element property = (Element) node;

            stackInfo.setMinUpgradeVersion(getTagValue(
                STACK_XML_PROPERTY_UPGRADE, property));

            stackInfo.setActive(Boolean.parseBoolean(getTagValue(
                STACK_XML_PROPERTY_ACTIVE, property)));
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
        return null;
      }

    }
    return stackInfo;
  }

  private List<RepositoryInfo> getRepository(File repositoryFile)
      throws ParserConfigurationException, IOException, SAXException {

    List<RepositoryInfo> repositorysInfo = new ArrayList<RepositoryInfo>();
//    try {

    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    Document doc = dBuilder.parse(repositoryFile);

    NodeList osNodes = doc
        .getElementsByTagName(REPOSITORY_XML_MAIN_BLOCK_NAME);

    for (int index = 0; index < osNodes.getLength(); index++) {
      Node osNode = osNodes.item(index);

      if (osNode.getNodeType() == Node.ELEMENT_NODE) {
        if (!osNode.getNodeName().equals(REPOSITORY_XML_MAIN_BLOCK_NAME)) {
          continue;
        }
        NamedNodeMap attrs = osNode.getAttributes();
        Node osAttr = attrs.getNamedItem(REPOSITORY_XML_ATTRIBUTE_OS_TYPE);
        if (osAttr == null) {
          continue;
        }
        String osType = osAttr.getNodeValue();

        NodeList repoNodes = osNode.getChildNodes();
        for (int j = 0; j < repoNodes.getLength(); j++) {
          Node repoNode = repoNodes.item(j);
          if (repoNode.getNodeType() != Node.ELEMENT_NODE
              || !repoNode.getNodeName().equals(
              REPOSITORY_XML_REPO_BLOCK_NAME)) {
            continue;
          }
          Element property = (Element) repoNode;
          String repoId = getTagValue(REPOSITORY_XML_PROPERTY_REPOID,
              property);
          String repoName = getTagValue(REPOSITORY_XML_PROPERTY_REPONAME,
              property);
          String baseUrl = getTagValue(
              REPOSITORY_XML_PROPERTY_BASEURL, property);
          String mirrorsList = getTagValue(
              REPOSITORY_XML_PROPERTY_MIRRORSLIST, property);

          String[] osTypes = osType.split(",");

          for (String os : osTypes) {
            RepositoryInfo repositoryInfo = new RepositoryInfo();
            repositoryInfo.setOsType(os.trim());
            repositoryInfo.setRepoId(repoId);
            repositoryInfo.setRepoName(repoName);
            repositoryInfo.setBaseUrl(baseUrl);
            repositoryInfo.setMirrorsList(mirrorsList);

            if (LOG.isDebugEnabled()) {
              LOG.debug("Adding repo to stack"
                  + ", repoInfo=" + repositoryInfo.toString());
            }
            repositorysInfo.add(repositoryInfo);
          }
        }
      }
    }
//    } catch (Exception e) {
//      e.printStackTrace();
//    }

    return repositorysInfo;
  }

  private void setMetaInfo(File metainfoFile, ServiceInfo serviceInfo) {

    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

    Document doc = null;
    DocumentBuilder dBuilder = null;
    try {
      dBuilder = dbFactory.newDocumentBuilder();
      doc = dBuilder.parse(metainfoFile);
    } catch (SAXException e) {
      LOG.error("Error while parsing metainf.xml", e);
    } catch (IOException e) {
      LOG.error("Error while open metainf.xml", e);
    } catch (ParserConfigurationException e) {
      LOG.error("Error while parsing metainf.xml", e);
    }

    if (doc == null) return;

    doc.getDocumentElement().normalize();

    NodeList metaInfoNodes = doc
        .getElementsByTagName(METAINFO_XML_MAIN_BLOCK_NAME);

    if (metaInfoNodes.getLength() > 0) {
      Node metaInfoNode = metaInfoNodes.item(0);
      if (metaInfoNode.getNodeType() == Node.ELEMENT_NODE) {

        Element metaInfoElem = (Element) metaInfoNode;

        serviceInfo.setVersion(getTagValue(METAINFO_XML_PROPERTY_VERSION,
            metaInfoElem));
        serviceInfo.setUser(getTagValue(METAINFO_XML_PROPERTY_USER,
            metaInfoElem));
        serviceInfo.setComment(getTagValue(METAINFO_XML_PROPERTY_COMMENT,
            metaInfoElem));
      }
    }

    NodeList componentInfoNodes = doc
        .getElementsByTagName(METAINFO_XML_PROPERTY_COMPONENT_MAIN);

    if (componentInfoNodes.getLength() > 0) {
      for (int index = 0; index < componentInfoNodes.getLength(); index++) {
        Node componentInfoNode = componentInfoNodes.item(index);
        if (componentInfoNode.getNodeType() == Node.ELEMENT_NODE) {
          Element componentInfoElem = (Element) componentInfoNode;

          ComponentInfo componentInfo = new ComponentInfo();
          componentInfo.setName(getTagValue(
              METAINFO_XML_PROPERTY_COMPONENT_NAME, componentInfoElem));
          componentInfo.setCategory(getTagValue(
              METAINFO_XML_PROPERTY_COMPONENT_CATEGORY, componentInfoElem));
          serviceInfo.getComponents().add(componentInfo);

        }
      }
    }
  }

  private List<PropertyInfo> getProperties(File propertyFile) {

    List<PropertyInfo> resultPropertyList = new ArrayList<PropertyInfo>();
    try {
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      Document doc = dBuilder.parse(propertyFile);
      doc.getDocumentElement().normalize();

      NodeList propertyNodes = doc
          .getElementsByTagName(PROPERTY_XML_MAIN_BLOCK_NAME);

      for (int index = 0; index < propertyNodes.getLength(); index++) {

        Node node = propertyNodes.item(index);
        if (node.getNodeType() == Node.ELEMENT_NODE) {
          Element property = (Element) node;
          PropertyInfo propertyInfo = new PropertyInfo();
          propertyInfo
              .setName(getTagValue(PROPERTY_XML_PROPERTY_NAME, property));
          propertyInfo.setValue(getTagValue(PROPERTY_XML_PROPERTY_VALUE,
              property));

          propertyInfo.setDescription(getTagValue(
              PROPERTY_XML_PROPERTY_DESCRIPTION, property));
          propertyInfo.setFilename(propertyFile.getName());

          if (propertyInfo.getName() == null || propertyInfo.getValue() == null)
            continue;

          resultPropertyList.add(propertyInfo);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
    return resultPropertyList;
  }

  private String getTagValue(String sTag, Element rawElement) {
    String result = null;

    if (rawElement.getElementsByTagName(sTag) != null && rawElement.getElementsByTagName(sTag).getLength() > 0) {
      if (rawElement.getElementsByTagName(sTag).item(0) != null) {
        NodeList element = rawElement.getElementsByTagName(sTag).item(0).getChildNodes();

        if (element != null && element.item(0) != null) {
          Node value = (Node) element.item(0);

          result = value.getNodeValue();
        }
      }
    }

    return result;
  }

  //TODO: Method is unused as of now
/*  public boolean areOsTypesCompatible(String type1, String type2) {
    if (type1 == null || type2 == null) {
      return false;
    }
    if (type1.equals(type2)) {
      return true;
    }
    if (type1.equals("redhat5") || type1.equals("centos5") ||
        type1.equals("oraclelinux5")) {
      if (type2.equals("centos5") || type2.equals("redhat5") ||
          type2.equals("oraclelinux5")) {
        return true;
      }
    } else if (type1.equals("redhat6") || type1.equals("centos6") ||
        type1.equals("oraclelinux6")) {
      if (type2.equals("centos6") || type2.equals("redhat6") ||
          type2.equals("oraclelinux6")) {
        return true;
      }
    } else if (type1.equals("suse11") || type1.equals("sles11")) {
      if (type2.equals("suse11") || type2.equals("sles11")) {
        return true;
      }
    }
    return false;
  }*/

  public boolean isOsSupported(String osType) {
    return osType.equals("redhat5") || osType.equals("centos5") ||
            osType.equals("oraclelinux5") ||
            osType.equals("redhat6") || osType.equals("centos6") ||
            osType.equals("oraclelinux6") ||
            osType.equals("suse11") || osType.equals("sles11");
  }
}
