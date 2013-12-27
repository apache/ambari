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
package org.apache.ambari.server.api.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.state.*;
import org.apache.ambari.server.state.stack.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Helper methods for providing stack extension behavior -
 * Apache Jira: AMBARI-2819
 */
public class StackExtensionHelper {
  private File stackRoot;
  private final static Logger LOG = LoggerFactory
    .getLogger(StackExtensionHelper.class);
  private final Map<String, StackInfo> stackVersionMap = new HashMap<String,
    StackInfo>();
  private Map<String, List<StackInfo>> stackParentsMap = null;

  private static final Map<Class<?>, JAXBContext> _jaxbContexts =
      new HashMap<Class<?>, JAXBContext> ();
  static {
    try {
      // three classes define the top-level element "metainfo", so we need 3 contexts.
      JAXBContext ctx = JAXBContext.newInstance(StackMetainfoXml.class, RepositoryXml.class, ConfigurationXml.class);
      _jaxbContexts.put(StackMetainfoXml.class, ctx);
      _jaxbContexts.put(RepositoryXml.class, ctx);
      _jaxbContexts.put(ConfigurationXml.class, ctx);
      _jaxbContexts.put(ServiceMetainfoXml.class, JAXBContext.newInstance(ServiceMetainfoXml.class));
      _jaxbContexts.put(ServiceMetainfoV2Xml.class, JAXBContext.newInstance(ServiceMetainfoV2Xml.class));
    } catch (JAXBException e) {
      throw new RuntimeException (e);
    }
  }

  /**
   * Note: constructor does not perform inialisation now. After instance
   * creation, you have to call fillInfo() manually
   */
  public StackExtensionHelper(File stackRoot) {
    this.stackRoot = stackRoot;
  }


  /**
   * Must be manually called after creation of StackExtensionHelper instance
   */
  public void fillInfo() throws Exception {
    if (stackParentsMap != null) {
      throw new AmbariException("fillInfo() method has already been called");
    }
    File[] stackFiles = stackRoot.listFiles(AmbariMetaInfo.FILENAME_FILTER);
    for (File stack : stackFiles) {
      if (stack.isFile()) {
        continue;
      }
      for (File stackFolder : stack.listFiles(AmbariMetaInfo.FILENAME_FILTER)) {
        if (stackFolder.isFile()) {
          continue;
        }
        String stackName = stackFolder.getParentFile().getName();
        String stackVersion = stackFolder.getName();
        stackVersionMap.put(stackName + stackVersion, getStackInfo(stackFolder));
      }
    }
    stackParentsMap = getParentStacksInOrder(stackVersionMap.values());
  }


  private ServiceInfo mergeServices(ServiceInfo parentService,
                                    ServiceInfo childService) {
    // TODO: Allow extending stack with custom services
    ServiceInfo mergedServiceInfo = new ServiceInfo();
    mergedServiceInfo.setName(childService.getName());
    mergedServiceInfo.setComment(childService.getComment());
    mergedServiceInfo.setUser(childService.getUser());
    mergedServiceInfo.setVersion(childService.getVersion());
    mergedServiceInfo.setConfigDependencies(childService.getConfigDependencies());
    
    // metrics
    if (null == childService.getMetricsFile() && null != parentService.getMetricsFile())
      mergedServiceInfo.setMetricsFile(parentService.getMetricsFile());
    
    // Add all child components to service
    List<String> deleteList = new ArrayList<String>();
    List<String> appendList = new ArrayList<String>();
    for (ComponentInfo childComponentInfo : childService.getComponents()) {
      if (!childComponentInfo.isDeleted()) {
        mergedServiceInfo.getComponents().add(childComponentInfo);
        appendList.add(childComponentInfo.getName());
      } else {
        deleteList.add(childComponentInfo.getName());
      }
    }
    // Add remaining parent components
    for (ComponentInfo parentComponent : parentService.getComponents()) {
      if (!deleteList.contains(parentComponent.getName()) && !appendList
          .contains(parentComponent.getName())) {
        mergedServiceInfo.getComponents().add(parentComponent);
      }
    }
    // Add child properties not deleted
    deleteList = new ArrayList<String>();
    appendList = new ArrayList<String>();
    for (PropertyInfo propertyInfo : childService.getProperties()) {
      if (!propertyInfo.isDeleted()) {
        mergedServiceInfo.getProperties().add(propertyInfo);
        appendList.add(propertyInfo.getName());
      } else {
        deleteList.add(propertyInfo.getName());
      }
    }
    // Add all parent properties
    for (PropertyInfo parentPropertyInfo : parentService.getProperties()) {
      if (!deleteList.contains(parentPropertyInfo.getName()) && !appendList
          .contains(parentPropertyInfo.getName())) {
        mergedServiceInfo.getProperties().add(parentPropertyInfo);
      }
    }
    // Add all parent config dependencies
    if (parentService.getConfigDependencies() != null && !parentService
        .getConfigDependencies().isEmpty()) {
      for (String configDep : parentService.getConfigDependencies()) {
        if (!mergedServiceInfo.getConfigDependencies().contains(configDep)) {
          mergedServiceInfo.getConfigDependencies().add(configDep);
        }
      }
    }
    return mergedServiceInfo;
  }
  

  public List<ServiceInfo> getAllApplicableServices(StackInfo stackInfo) {
    LinkedList<StackInfo> parents = (LinkedList<StackInfo>)
      stackParentsMap.get(stackInfo.getVersion());

    if (parents == null || parents.isEmpty()) {
      return stackInfo.getServices();
    }
    // Add child to the end of extension list
    parents.addFirst(stackInfo);
    ListIterator<StackInfo> lt = parents.listIterator(parents.size());
    // Map services with unique names
    Map<String, ServiceInfo> serviceInfoMap = new HashMap<String,
      ServiceInfo>();
    // Iterate with oldest parent first - all stacks are populated
    while(lt.hasPrevious()) {
      StackInfo parentStack = lt.previous();
      List<ServiceInfo> serviceInfoList = parentStack.getServices();
      for (ServiceInfo service : serviceInfoList) {
        ServiceInfo existingService = serviceInfoMap.get(service.getName());
        if (service.isDeleted()) {
          serviceInfoMap.remove(service.getName());
          continue;
        }

        if (existingService == null) {
          serviceInfoMap.put(service.getName(), service);
        } else {
          // Redefined service - merge with parent
          ServiceInfo newServiceInfo = mergeServices(existingService, service);
          serviceInfoMap.put(service.getName(), newServiceInfo);
        }
      }
    }
    return new ArrayList<ServiceInfo>(serviceInfoMap.values());
  }

  void populateServicesForStack(StackInfo stackInfo) throws
          ParserConfigurationException, SAXException,
          XPathExpressionException, IOException, JAXBException {
    List<ServiceInfo> services = new ArrayList<ServiceInfo>();
    File servicesFolder = new File(stackRoot.getAbsolutePath() + File
      .separator + stackInfo.getName() + File.separator + stackInfo.getVersion()
      + File.separator + AmbariMetaInfo.SERVICES_FOLDER_NAME);
    if (!servicesFolder.exists()) {
      LOG.info("No services defined for stack: " + stackInfo.getName() +
      "-" + stackInfo.getVersion());

    } else {
      try {
        File[] servicesFolders = servicesFolder.listFiles(AmbariMetaInfo
          .FILENAME_FILTER);
        if (servicesFolders == null) {
          String message = String.format("No service folders found at %s",
                  servicesFolder.getAbsolutePath());
          throw new AmbariException(message);
        }
        // Iterate over service folders
        for (File serviceFolder : servicesFolders) {
          if (!serviceFolder.isDirectory())
            continue;
          // Get metainfo schema format version
          File metainfoFile = new File(serviceFolder.getAbsolutePath()
                  + File.separator + AmbariMetaInfo.SERVICE_METAINFO_FILE_NAME);
          // get metrics file, if it exists
          File metricsJson = new File(serviceFolder.getAbsolutePath()
            + File.separator + AmbariMetaInfo.SERVICE_METRIC_FILE_NAME);
          String version = getSchemaVersion(metainfoFile);
          if (AmbariMetaInfo.SCHEMA_VERSION_LEGACY.equals(version)) {
            // Get information about service
            ServiceInfo serviceInfo = new ServiceInfo();
            serviceInfo.setSchemaVersion(AmbariMetaInfo.SCHEMA_VERSION_LEGACY);
            serviceInfo.setName(serviceFolder.getName());
            ServiceMetainfoXml smx = unmarshal(ServiceMetainfoXml.class, metainfoFile);
            serviceInfo.setComment(smx.getComment());
            serviceInfo.setUser(smx.getUser());
            serviceInfo.setVersion(smx.getVersion());
            serviceInfo.setDeleted(smx.isDeleted());
			      serviceInfo.setConfigDependencies(smx.getConfigDependencies());
            serviceInfo.getComponents().addAll(smx.getComponents());

            if (metricsJson.exists())
              serviceInfo.setMetricsFile(metricsJson);            

            // Get all properties from all "configs/*-site.xml" files
            setPropertiesFromConfigs(serviceFolder, serviceInfo);

            // Add now to be removed while iterating extension graph
            services.add(serviceInfo);
          } else { //Reading v2 service metainfo (may contain multiple services)
            // Get services from metadata
            ServiceMetainfoV2Xml smiv2x =
                    unmarshal(ServiceMetainfoV2Xml.class, metainfoFile);
            List<ServiceInfo> serviceInfos = smiv2x.getServices();
            for (ServiceInfo serviceInfo : serviceInfos) {
              serviceInfo.setSchemaVersion(AmbariMetaInfo.SCHEMA_VERSION_2);
              serviceInfo.setServiceMetadataFolder(serviceFolder.getName());
              // TODO: allow repository overriding when extending stack

              if (metricsJson.exists())
                serviceInfo.setMetricsFile(metricsJson);

              // Get all properties from all "configs/*-site.xml" files
              setPropertiesFromConfigs(serviceFolder, serviceInfo);

              // Add now to be removed while iterating extension graph
              services.add(serviceInfo);
            }
          }
        }
      } catch (Exception e) {
        LOG.error("Error while parsing metainfo.xml for a service", e);
      }
    }

    stackInfo.getServices().addAll(services);
  }


  public List<StackInfo> getAllAvailableStacks() {
    return new ArrayList<StackInfo>(stackVersionMap.values());
  }

  private Map<String, List<StackInfo>> getParentStacksInOrder(
      Collection<StackInfo> stacks) {
    Map<String, List<StackInfo>> parentStacksMap = new HashMap<String,
      List<StackInfo>>();

    for (StackInfo child : stacks) {
      List<StackInfo> parentStacks = new LinkedList<StackInfo>();
      parentStacksMap.put(child.getVersion(), parentStacks);
      while (child.getParentStackVersion() != null && !child
        .getParentStackVersion().isEmpty() && !child.getVersion().equals
        (child.getParentStackVersion())) {
        String key = child.getName() + child.getParentStackVersion();
        if (stackVersionMap.containsKey(key)) {
          StackInfo parent = stackVersionMap.get(key);
          parentStacks.add(parent);
          child = parent;
        } else {
          LOG.info("Unknown parent stack version: " + child
            .getParentStackVersion() + ", for stack: " + child.getName() + " " +
            child.getVersion());
          break;
        }
      }
    }
    return parentStacksMap;
  }


  /**
   * Determines schema version of a given metainfo file
   * @param stackMetainfoFile  xml file
   */
  String getSchemaVersion(File stackMetainfoFile) throws IOException,
          ParserConfigurationException, SAXException, XPathExpressionException {
    // Using XPath to get a single value from an metainfo file
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(stackMetainfoFile);
    XPathFactory xPathfactory = XPathFactory.newInstance();
    XPath xpath = xPathfactory.newXPath();
    XPathExpression schemaPath = xpath.compile("/metainfo/schemaVersion[1]");

    String value = schemaPath.evaluate(doc).trim();
    if ( "".equals(value) || // If schemaVersion is not defined
            AmbariMetaInfo.SCHEMA_VERSION_LEGACY.equals(value)) {
      return AmbariMetaInfo.SCHEMA_VERSION_LEGACY;
    } else if (AmbariMetaInfo.SCHEMA_VERSION_2.equals(value)) {
      return AmbariMetaInfo.SCHEMA_VERSION_2;
    } else {
      String message = String.format("Unknown schema version %s at file " +
              "%s", value, stackMetainfoFile.getAbsolutePath());
      throw new AmbariException(message);
    }

  }


  private StackInfo getStackInfo(File stackVersionFolder) throws JAXBException {
    StackInfo stackInfo = new StackInfo();

    stackInfo.setName(stackVersionFolder.getParentFile().getName());
    stackInfo.setVersion(stackVersionFolder.getName());

    // Get metainfo from file
    File stackMetainfoFile = new File(stackVersionFolder.getAbsolutePath()
        + File.separator + AmbariMetaInfo.STACK_METAINFO_FILE_NAME);

    if (stackMetainfoFile.exists()) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Reading stack version metainfo from file "
            + stackMetainfoFile.getAbsolutePath());
      }
      
      StackMetainfoXml smx = unmarshal(StackMetainfoXml.class, stackMetainfoFile);
      
      stackInfo.setMinUpgradeVersion(smx.getVersion().getUpgrade());
      stackInfo.setActive(smx.getVersion().isActive());
      stackInfo.setParentStackVersion(smx.getExtends());
      String rcoFileLocation = stackVersionFolder.getAbsolutePath() + File.separator + AmbariMetaInfo.RCO_FILE_NAME;
      if (new File(rcoFileLocation).exists())
        stackInfo.setRcoFileLocation(rcoFileLocation);
    }

    try {
      // Read the service and available configs for this stack
      populateServicesForStack(stackInfo);
    } catch (Exception e) {
      LOG.error("Exception caught while populating services for stack: " +
        stackInfo.getName() + "-" + stackInfo.getVersion());
      e.printStackTrace();
    }
    return stackInfo;
  }


  private List<PropertyInfo> getProperties(File propertyFile) {
    
    try {
      ConfigurationXml cx = unmarshal(ConfigurationXml.class, propertyFile);

      List<PropertyInfo> list = new ArrayList<PropertyInfo>();
      
      for (PropertyInfo pi : cx.getProperties()) {
        // maintain old behavior
        if (null == pi.getValue() || pi.getValue().isEmpty())
          continue;
        
        pi.setFilename(propertyFile.getName());
        list.add(pi);
      }
      return list;
    } catch (Exception e) {
      LOG.error("Could not load configuration for " + propertyFile, e);
      return null;
    }
  }


  /**
   * Get all properties from all "configs/*-site.xml" files
   */
  void setPropertiesFromConfigs(File serviceFolder, ServiceInfo serviceInfo) {
    File serviceConfigFolder = new File(serviceFolder.getAbsolutePath()
            + File.separator + AmbariMetaInfo.SERVICE_CONFIG_FOLDER_NAME);
    File[] configFiles = serviceConfigFolder.listFiles
            (AmbariMetaInfo.FILENAME_FILTER);
    if (configFiles != null) {
      for (File config : configFiles) {
        if (config.getName().endsWith
                (AmbariMetaInfo.SERVICE_CONFIG_FILE_NAME_POSTFIX)) {
          serviceInfo.getProperties().addAll(getProperties(config));
        }
      }
    }
  }

  
  public static <T> T unmarshal(Class<T> clz, File file) throws JAXBException {
    Unmarshaller u = _jaxbContexts.get(clz).createUnmarshaller();
    
    return clz.cast(u.unmarshal(file));
  }  
  
}
