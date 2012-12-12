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
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * ServiceInfo responsible getting information about cluster.
 */
@Singleton
public class AmbariMetaInfo {

  private List<StackInfo> stacksResult = new ArrayList<StackInfo>();
  private File stackRoot;
  private final static Logger LOG = LoggerFactory
      .getLogger(AmbariMetaInfo.class);

  private static final String SERVICES_FOLDER_NAME = "services";
  private static final String SERVICE_METAINFO_FILE_NAME = "metainfo.xml";
  private static final String SERVICE_CONFIG_FOLDER_NAME = "configuration";
  private static final String SERVICE_CONFIG_FILE_NAME_POSTFIX = "-site.xml";

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


  /**
   * Ambari Meta Info Object
   * @param conf Configuration API to be used.
   * @throws Exception
   */
  @Inject
  public AmbariMetaInfo(Configuration conf) throws Exception {
    String stackPath = conf.getMetadataPath();
    this.stackRoot = new File(stackPath);
  }

  @Inject
  public AmbariMetaInfo(File stackRoot) throws Exception {
    this.stackRoot = stackRoot;
  }


  /**
   * Initialize the Ambari Meta Info
   * @throws Exception throws exception if not able to parse the Meta data.
   */
  public void init() throws Exception {
    getConfigurationInformation(stackRoot);
  }


  /**
   * Get component category
   * @param stackName
   * @param version
   * @param serviceName
   * @param componentName
   * @return component component Info
   */
  public ComponentInfo getComponentCategory(String stackName, String version,
      String serviceName, String componentName) {
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
   * @param stackName
   * @param version
   * @param serviceName
   * @return
   */
  public List<ComponentInfo> getComponentsByService(String stackName,
      String version, String serviceName) {
    List<ComponentInfo> componentsResult = null;
    ServiceInfo service = getServiceInfo(stackName, version, serviceName);
    if (service != null)
      componentsResult = service.getComponents();

    return componentsResult;
  }

  public Map<String, List<RepositoryInfo>> getRepository(String stackName,
      String version) {
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

  /*
   * function for given a stack name and version, is it a supported stack
   */
  public boolean isSupportedStack(String stackName, String version) {
    boolean exist = false;
    StackInfo stack = getStackInfo(stackName, version);
    if (stack == null)
      exist = true;
    return exist;
  }

  /*
   * support isValidService(), isValidComponent for a given stack/version
   */
  public boolean isValidService(String stackName, String version,
      String serviceName) {
    ServiceInfo service = getServiceInfo(stackName, version, serviceName);
    return (service != null);
  }

  /*
   * support isValidService(), isValidComponent for a given stack/version
   */
  public boolean isValidServiceComponent(String stackName, String version,
      String serviceName, String componentName) {
    ServiceInfo service = getServiceInfo(stackName, version, serviceName);
    if (service == null) {
      return false;
    }
    for (ComponentInfo compInfo: service.getComponents()) {
      if (compInfo.getName().equals(componentName)) {
        return true;
      }
    }
    return false;
  }


  /**
   * Get the name of a service given the component name.
   * @param stackName the stack name
   * @param version the stack version
   * @param componentName the component name
   * @return the service name
   */
  public String getComponentToService(String stackName, String version,
      String componentName) {
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
    for (Map.Entry<String, ServiceInfo> entry: services.entrySet()) {
      for (ComponentInfo compInfo: entry.getValue().getComponents()) {
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
   * @param stackName the stack name
   * @param version the version of the stack
   * @param serviceName the name of the service in the stack
   * @return the config knobs supported for the service
   */
  public Map<String, Map<String, String>> getSupportedConfigs(String stackName,
      String version, String serviceName) {
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
   * @param stackName the stack name
   * @param version the version of the stack
   * @return the information of abt varios services that are supported in the
   * stack
   */
  public Map<String, ServiceInfo> getServices(String stackName, String version) {

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

  public ServiceInfo getServiceInfo(String stackName, String version,
      String serviceName) {
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

  public List<ServiceInfo> getSupportedServices(String stackName, String version) {
    List<ServiceInfo> servicesResulr = null;
    StackInfo stack = getStackInfo(stackName, version);
    if (stack != null)
      servicesResulr = stack.getServices();
    return servicesResulr;
  }

  public List<StackInfo> getSupportedStacks() {
    return stacksResult;
  }

  public StackInfo getStackInfo(String stackName, String version) {
    StackInfo stackInfoResult = null;

    for (StackInfo stack : stacksResult) {
      if (stackName.equals(stack.getName())
          && version.equals(stack.getVersion())) {
        stackInfoResult = stack;
        break;
      }
    }
    return stackInfoResult;
  }


  private void getConfigurationInformation(File stackRoot) throws Exception {

    if (LOG.isDebugEnabled()) {
      LOG.debug("Loading stack information"
          + ", stackRoot=" + stackRoot.getPath());
    }

    if (!stackRoot.isDirectory() && !stackRoot.exists())
      throw new IOException("" + Configuration.METADETA_DIR_PATH
          + " should be a directory with stack.");
    File[] stacks = stackRoot.listFiles();
    for (File stackFolder : stacks) {
      if (stackFolder.isFile())
        continue;
      File[] concretStacks = stackFolder.listFiles();
      for (File stack : concretStacks) {
        if (stack.isFile())
          continue;
        StackInfo stackInfo = new StackInfo();
        stackInfo.setName(stackFolder.getName());
        stackInfo.setVersion(stack.getName());

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
        File[] servicesFolders = servicesRootFolder.listFiles();

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
            LOG.info("Listing config files in folder " + serviceConfigFolder);
            File[] configFiles = serviceConfigFolder.listFiles();
            if (configFiles != null) {
              for (File config : configFiles) {
                if (config.getName().endsWith(SERVICE_CONFIG_FILE_NAME_POSTFIX)) {
                  LOG.info("Reading Metadata Info from config filename " +
                      config.getAbsolutePath());
                  serviceInfo.getProperties().addAll(getProperties(config));
                }
              }
            }
          }
        }
      }
    }

  }

  /**
   *
   * @param node
   * @return
   * @throws TransformerException
   */
  private String nodeToString(Node node) throws TransformerException {
    // Set up the output transformer
    TransformerFactory transfac = TransformerFactory.newInstance();
    Transformer trans = transfac.newTransformer();
    trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    trans.setOutputProperty(OutputKeys.INDENT, "yes");
    StringWriter sw = new StringWriter();
    StreamResult result = new StreamResult(sw);
    DOMSource source = new DOMSource(node);
    trans.transform(source, result);
    String xmlString = sw.toString();
    return xmlString;
  }

  /**
   * Convert a document to string
   * @param doc
   * @return string
   * @throws TransformerException
   */
  private String documentToString(Document doc) throws TransformerException {
    Transformer transformer = TransformerFactory.newInstance().newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");

    //initialize StreamResult with File object to save to file
    StreamResult result = new StreamResult(new StringWriter());
    DOMSource source = new DOMSource(doc);
    transformer.transform(source, result);

    String xmlString = result.getWriter().toString();
    return xmlString;
  }

  private List<RepositoryInfo> getRepository(File repositoryFile) {

    List<RepositoryInfo> repositorysInfo = new ArrayList<RepositoryInfo>();
    try {

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
            if (repoNode.getNodeType() != Node.ELEMENT_NODE) {
              continue;
            }
            Element property = (Element) repoNode;
            RepositoryInfo repositoryInfo = new RepositoryInfo();
            repositoryInfo.setOsType(osType);
            repositoryInfo.setRepoId(getTagValue(REPOSITORY_XML_PROPERTY_REPOID,
                property));
            repositoryInfo.setRepoName(
                getTagValue(REPOSITORY_XML_PROPERTY_REPONAME, property));

            repositoryInfo.setBaseUrl(getTagValue(
                REPOSITORY_XML_PROPERTY_BASEURL, property));
            repositoryInfo.setMirrorsList(getTagValue(
                REPOSITORY_XML_PROPERTY_MIRRORSLIST, property));

            if (LOG.isDebugEnabled()) {
              LOG.debug("Adding repo to stack"
                  + ", repoInfo=" + repositoryInfo.toString());
            }
            repositorysInfo.add(repositoryInfo);
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

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
    try {
      NodeList element = rawElement.getElementsByTagName(sTag).item(0)
          .getChildNodes();
      Node value = (Node) element.item(0);
      result = value.getNodeValue();
    } catch (Exception e) {
//      LOG.info("Error in getting tag value " ,  e);
      // log.debug("There is no field like " + sTag + "in this DOM element.",
      // e);
    } finally {
      return result;
    }

  }

}
