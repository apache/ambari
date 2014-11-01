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

import com.google.inject.Injector;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.state.ClientConfigFileDefinition;
import org.apache.ambari.server.state.CommandScriptDefinition;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.CustomCommandDefinition;
import org.apache.ambari.server.state.DependencyInfo;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.ServiceOsSpecific;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.stack.ConfigurationXml;
import org.apache.ambari.server.state.stack.RepositoryXml;
import org.apache.ambari.server.state.stack.ServiceMetainfoXml;
import org.apache.ambari.server.state.stack.StackMetainfoXml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

/**
 * Helper methods for providing stack extension behavior -
 * Apache Jira: AMBARI-2819
 *
 * Stack extension processing is done in two steps. At first step, we parse
 * all information for every stack from stack files. At second step, we
 * go through parent and perform inheritance where needed. At both steps,
 * stacks are processed at random order, that's why extension implementation
 * for any new stack/service/component property should also consist of two
 * separate steps (otherwise child may happen to be processed before parent's
 * properties are populated).
 */
public class StackExtensionHelper {
  private ActionMetadata actionMetadata;
  
  private File stackRoot;
  private final static Logger LOG = LoggerFactory.getLogger(StackExtensionHelper.class);
  private final Map<String, StackInfo> stackVersionMap = new HashMap<String,
    StackInfo>();
  private Map<String, List<StackInfo>> stackParentsMap = null;
  public final static String HOOKS_FOLDER_NAME = "hooks";
  private static final String PACKAGE_FOLDER_NAME = "package";

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
    } catch (JAXBException e) {
      throw new RuntimeException (e);
    }
  }

  /**
   * Note: constructor does not perform inialisation now. After instance
   * creation, you have to call fillInfo() manually
   */
  public StackExtensionHelper(Injector injector, File stackRoot) {
    this.stackRoot = stackRoot;
    this.actionMetadata = injector.getInstance(ActionMetadata.class);
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

  void mergeStacks(StackInfo parentStack,
      StackInfo resultStack) {
    if(parentStack.getConfigTypes() != null) {
      resultStack.getConfigTypes().putAll(parentStack.getConfigTypes());
    }
    List<PropertyInfo> mergedProperties = new ArrayList<PropertyInfo>();
    mergeProperties(resultStack.getProperties(), parentStack.getProperties(), mergedProperties, resultStack.getConfigTypes());
    resultStack.setProperties(mergedProperties);
  }

  ServiceInfo mergeServices(ServiceInfo parentService,
                                    ServiceInfo childService) {
    ServiceInfo mergedServiceInfo = new ServiceInfo();
    mergedServiceInfo.setSchemaVersion(childService.getSchemaVersion());
    mergedServiceInfo.setName(childService.getName());
    mergedServiceInfo.setComment(childService.getComment() != null ?
        childService.getComment() :
        parentService.getComment());
    mergedServiceInfo.setVersion(childService.getVersion());
    mergedServiceInfo.setDisplayName(
        childService.getDisplayName() != null ?
            childService.getDisplayName() :
            parentService.getDisplayName());
    mergedServiceInfo.setConfigDependencies(
        childService.getConfigDependencies() != null ?
            childService.getConfigDependencies() :
            parentService.getConfigDependencies() != null ?
                parentService.getConfigDependencies() :
                Collections.<String>emptyList());
    mergedServiceInfo.setExcludedConfigTypes(
      childService.getExcludedConfigTypes() != null ?
        childService.getExcludedConfigTypes() :
        parentService.getExcludedConfigTypes() != null ?
          parentService.getExcludedConfigTypes() :
          Collections.<String>emptySet()
    );
    mergedServiceInfo.setRequiredServices(
        childService.getRequiredServices() != null ?
          childService.getRequiredServices() :
          parentService.getRequiredServices() != null ?
            parentService.getRequiredServices() :
            Collections.<String>emptyList()
      );
    
    mergedServiceInfo.setRestartRequiredAfterChange(
            (childService.isRestartRequiredAfterChange() != null) 
                    ? childService.isRestartRequiredAfterChange()
                    : parentService.isRestartRequiredAfterChange());
    mergedServiceInfo.setMonitoringService(
            (childService.isMonitoringService() != null)
                    ? childService.isMonitoringService()
                    : parentService.isMonitoringService());

    Map<String, ServiceOsSpecific> osSpecific = childService.getOsSpecifics();
    if (! osSpecific.isEmpty()) {
      mergedServiceInfo.setOsSpecifics(childService.getOsSpecifics());
    } else {
      mergedServiceInfo.setOsSpecifics(parentService.getOsSpecifics());
    }
    
    mergedServiceInfo.setConfigTypes(new HashMap<String, Map<String, Map<String, String>>>());
    if(childService.getConfigTypes() != null) {
      mergedServiceInfo.getConfigTypes().putAll(childService.getConfigTypes());
    }
    if(parentService.getConfigTypes() != null) {
      mergedServiceInfo.getConfigTypes().putAll(parentService.getConfigTypes());
    }
    
    CommandScriptDefinition commandScript = childService.getCommandScript();
    if (commandScript != null) {
       mergedServiceInfo.setCommandScript(childService.getCommandScript());
    } else {
      mergedServiceInfo.setCommandScript(parentService.getCommandScript());
    }
    
    String servicePackageFolder = childService.getServicePackageFolder();
    if (servicePackageFolder != null) {
      mergedServiceInfo.setServicePackageFolder(servicePackageFolder);
    } else {
      mergedServiceInfo.setServicePackageFolder(
              parentService.getServicePackageFolder());
    }

    // Merge custom command definitions for service
    List<CustomCommandDefinition> mergedCustomCommands =
            mergeCustomCommandLists(parentService.getCustomCommands(),
                    childService.getCustomCommands());
    mergedServiceInfo.setCustomCommands(mergedCustomCommands);
    
    // metrics
    if (null != childService.getMetricsFile())
      mergedServiceInfo.setMetricsFile(childService.getMetricsFile());
    else if (null != parentService.getMetricsFile())
      mergedServiceInfo.setMetricsFile(parentService.getMetricsFile());
    
    // alerts
    if (null != childService.getAlertsFile())
      mergedServiceInfo.setAlertsFile(childService.getAlertsFile());
    else if (null != parentService.getAlertsFile())
      mergedServiceInfo.setAlertsFile(parentService.getAlertsFile());

    populateComponents(mergedServiceInfo, parentService, childService);
    mergeProperties(childService.getProperties(), parentService.getProperties(),
        mergedServiceInfo.getProperties(), childService.getConfigTypes());
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
  
  public void mergeProperties(List<PropertyInfo> childProperties, 
                              List<PropertyInfo> parentProperties, List<PropertyInfo> mergedProperties, Map<String,
      Map<String, Map<String, String>>> childConfigTypes) {
    // Add child properties not deleted
    Map<String, Set<String>> deleteMap = new HashMap<String, Set<String>>();
    Map<String, Set<String>> appendMap = new HashMap<String, Set<String>>();
    for (PropertyInfo propertyInfo : childProperties) {
      if (!propertyInfo.isDeleted()) {
        mergedProperties.add(propertyInfo);
        if (appendMap.containsKey(propertyInfo.getName())) {
          appendMap.get(propertyInfo.getName()).add(propertyInfo.getFilename());
        } else {
          Set<String> filenames = new HashSet<String>();
          filenames.add(propertyInfo.getFilename());
          appendMap.put(propertyInfo.getName(), filenames);
        }
      } else {
        if (deleteMap.containsKey(propertyInfo.getName())) {
          deleteMap.get(propertyInfo.getName()).add(propertyInfo.getFilename());
        } else {
          Set<String> filenames = new HashSet<String>();
          filenames.add(propertyInfo.getFilename());
          deleteMap.put(propertyInfo.getName(), filenames);
        }
      }
    }
    // Add all parent properties
    for (PropertyInfo parentPropertyInfo : parentProperties) {
      String configType = ConfigHelper.fileNameToConfigType(parentPropertyInfo.getFilename());
      boolean disableInherit = false;
      if (childConfigTypes.containsKey(configType)){
        disableInherit =
          childConfigTypes.get(configType).get(Supports.KEYWORD).get(Supports.DO_NOT_EXTEND.getPropertyName()).equals("true");
      }
      if (!deleteMap.containsKey(parentPropertyInfo.getName()) && !(appendMap
          .containsKey(parentPropertyInfo.getName())
        && appendMap.get(parentPropertyInfo.getName())
          .contains(parentPropertyInfo.getFilename())) && !disableInherit) {
        mergedProperties.add(parentPropertyInfo);
      }
    }
  }


  /**
   * Merges component sets of parentService and childService and writes result
   * to mergedServiceInfo
   */
  private void populateComponents(ServiceInfo mergedServiceInfo, ServiceInfo parentService,
                                  ServiceInfo childService) {
    // Add all child components to service
    List<String> deleteList = new ArrayList<String>();
    List<String> appendList = new ArrayList<String>();

    for (ComponentInfo childComponent : childService.getComponents()) {
      if (!childComponent.isDeleted()) {
        ComponentInfo parentComponent = parentService.getComponentByName(childComponent.getName());
        if (parentComponent != null) { // If parent has similar component
          ComponentInfo mergedComponent = mergeComponents(parentComponent,
                  childComponent);
          mergedServiceInfo.getComponents().add(mergedComponent);
          appendList.add(mergedComponent.getName());
        } else {
          mergedServiceInfo.getComponents().add(childComponent);
          appendList.add(childComponent.getName());
        }
      } else {
        deleteList.add(childComponent.getName());
      }
    }
    // Add remaining parent components
    for (ComponentInfo parentComponent : parentService.getComponents()) {
      if (!deleteList.contains(parentComponent.getName()) && !appendList
              .contains(parentComponent.getName())) {
        mergedServiceInfo.getComponents().add(parentComponent);
      }
    }
  }

  ComponentInfo mergeComponents(ComponentInfo parent, ComponentInfo child) {
    ComponentInfo result = new ComponentInfo(child); // cloning child
    CommandScriptDefinition commandScript = child.getCommandScript();
    String category = child.getCategory();
    String cardinality = child.getCardinality();
    List<String> clientsToUpdateConfigs = child.getClientsToUpdateConfigs();

    if (commandScript != null) {
      result.setCommandScript(child.getCommandScript());
    } else {
      result.setCommandScript(parent.getCommandScript());
    }
    //keep the same semantic as for ServiceInfo
    result.setDisplayName(
        child.getDisplayName() != null ?
            child.getDisplayName() : parent.getDisplayName());

    result.setConfigDependencies(
        child.getConfigDependencies() != null ?
            child.getConfigDependencies() : parent.getConfigDependencies());


    //Merge client config file definitions
    List<ClientConfigFileDefinition> clientConfigFiles = child.getClientConfigFiles();
    if (clientConfigFiles != null) {
      result.setClientConfigFiles(child.getClientConfigFiles());
    } else {
      result.setClientConfigFiles(parent.getClientConfigFiles());
    }
    if (clientsToUpdateConfigs != null) {
      result.setClientsToUpdateConfigs(child.getClientsToUpdateConfigs());
    } else {
      result.setClientsToUpdateConfigs(parent.getClientsToUpdateConfigs());
    }
    // Merge custom command definitions for service
    List<CustomCommandDefinition> mergedCustomCommands =
                mergeCustomCommandLists(parent.getCustomCommands(),
                            child.getCustomCommands());
    result.setCustomCommands(mergedCustomCommands);

    if (category != null) {
      result.setCategory(child.getCategory());
    } else {
      result.setCategory(parent.getCategory());
    }

    if (cardinality != null) {
      result.setCardinality(child.getCardinality());
    } else {
      result.setCardinality(parent.getCardinality());
    }

    result.setDependencies(
        child.getDependencies() == null ?
            parent.getDependencies() :
            parent.getDependencies() == null ?
                child.getDependencies() :
                mergeComponentDependencies(parent.getDependencies(),
                    child.getDependencies()));

    return result;
  }

  List<DependencyInfo> mergeComponentDependencies(
      List<DependencyInfo> parentList,
      List<DependencyInfo> childList) {

    List<DependencyInfo> mergedList =
        new ArrayList<DependencyInfo>(childList);
    List<String> existingNames = new ArrayList<String>();

    for (DependencyInfo childDI : childList) {
      existingNames.add(childDI.getName());
    }
    for (DependencyInfo parentsDI : parentList) {
      if (! existingNames.contains(parentsDI.getName())) {
        mergedList.add(parentsDI);
        existingNames.add(parentsDI.getName());
      }
    }
    return mergedList;
  }


  private List<CustomCommandDefinition> mergeCustomCommandLists(
          List<CustomCommandDefinition> parentList,
          List<CustomCommandDefinition> childList) {
    List<CustomCommandDefinition> mergedList =
            new ArrayList<CustomCommandDefinition>(childList);
    List<String> existingNames = new ArrayList<String>();
    for (CustomCommandDefinition childCCD : childList) {
      existingNames.add(childCCD.getName());
    }
    for (CustomCommandDefinition parentsCCD : parentList) {
      if (! existingNames.contains(parentsCCD.getName())) {
        mergedList.add(parentsCCD);
        existingNames.add(parentsCCD.getName());
      }
    }
    return mergedList;
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
    List<ServiceInfo> serviceInfoList = null;
    // Iterate with oldest parent first - all stacks are populated
    StackInfo parentStack = null;
    while(lt.hasPrevious()) {
      if (parentStack == null) {
        parentStack = lt.previous();
        serviceInfoList = parentStack.getServices();
        for (ServiceInfo service : serviceInfoList){
          if (!service.isDeleted()) {
            serviceInfoMap.put(service.getName(), service);
          }
        }
        continue;
      }
      StackInfo currentStackInfo = lt.previous();
      serviceInfoList = currentStackInfo.getServices();
      
      mergeStacks(parentStack, currentStackInfo);
      
      for (ServiceInfo service : serviceInfoList) {
        ServiceInfo existingService = serviceInfoMap.get(service.getName());
        if (service.isDeleted()) {
          serviceInfoMap.remove(service.getName());
          continue;
        }

        if (existingService == null && !service.isDeleted()) {
          serviceInfoMap.put(service.getName(), service);
        } else {
          // Redefined service - merge with parent
          ServiceInfo newServiceInfo = mergeServices(existingService, service);
          serviceInfoMap.put(service.getName(), newServiceInfo);
        }
        
        // remove 'excluded-config-types' from configTypes
        ServiceInfo serviceInfo = serviceInfoMap.get(service.getName());
        if(serviceInfo.getExcludedConfigTypes() != null) { 
          Iterator<Map.Entry<String,Map<String,Map<String,String>>>> configTypesItetator = serviceInfo.getConfigTypes().entrySet().iterator();
          
          while(configTypesItetator.hasNext()) {
            Map.Entry<String,Map<String,Map<String,String>>> configTypeMap = configTypesItetator.next();
            
            if(serviceInfo.getExcludedConfigTypes().contains(configTypeMap.getKey())) {
              configTypesItetator.remove();
            }
          }
        }
        
      }
      parentStack = currentStackInfo;
    }
    return new ArrayList<ServiceInfo>(serviceInfoMap.values());
  }


  /**
   * Determines exact hooks folder (subpath from stackRoot to hooks directory)
   * to use for a given stack. If given stack
   * has not hooks folder, inheritance hierarhy is queried.
   * @param stackInfo stack to work with
   */
  public String resolveHooksFolder(StackInfo stackInfo) throws AmbariException {
    // Determine hooks folder for stack
    String stackId = String.format("%s-%s",
            stackInfo.getName(), stackInfo.getVersion());
    String hooksFolder = stackInfo.getStackHooksFolder();
    if (hooksFolder == null) {
      // Try to get parent's
      List<StackInfo> parents = getParents(stackInfo);
      for (StackInfo parent : parents) {
        hooksFolder = parent.getStackHooksFolder();
        if (hooksFolder != null) {
          break;
        }
      }
    }
    if (hooksFolder == null) {
      String message = String.format(
              "Can not determine hooks dir for stack %s",
              stackId);
      LOG.debug(message);
    }
    return hooksFolder;
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
          
          File alertsJson = new File(serviceFolder.getAbsolutePath() +
              File.separator + AmbariMetaInfo.SERVICE_ALERT_FILE_NAME);
          
          if (!metainfoFile.exists()) {
            LOG.warn("Service folder " + serviceFolder.getAbsolutePath() + " doesn't contain metainfo file. Ignoring it.");
            continue;
          }

          //Reading v2 service metainfo (may contain multiple services)
          // Get services from metadata
          try {
            ServiceMetainfoXml smiv2x =
                unmarshal(ServiceMetainfoXml.class, metainfoFile);
            List<ServiceInfo> serviceInfos = smiv2x.getServices();
            for (ServiceInfo serviceInfo : serviceInfos) {
              serviceInfo.setSchemaVersion(AmbariMetaInfo.SCHEMA_VERSION_2);

              // Find service package folder
              String servicePackageDir = resolveServicePackageFolder(
                  stackRoot.getAbsolutePath(), stackInfo,
                  serviceFolder.getName(), serviceInfo.getName());
              serviceInfo.setServicePackageFolder(servicePackageDir);

              // process metrics.json
              if (metricsJson.exists())
                serviceInfo.setMetricsFile(metricsJson);
              if (alertsJson.exists())
                serviceInfo.setAlertsFile(alertsJson);

              // Get all properties from all "configs/*-site.xml" files
              setPropertiesFromConfigs(serviceFolder, serviceInfo);

              // Add now to be removed while iterating extension graph
              services.add(serviceInfo);
            }
          } catch (JAXBException e) {
            LOG.warn("Error while parsing metainfo.xml for a service: " + serviceFolder.getAbsolutePath(), e);
          }
        }
      } catch (Exception e) {
        LOG.error("Error while parsing metainfo.xml for a service", e);
      }
    }

    stackInfo.getServices().addAll(services);

    // add service check actions from the target stack
    for(ServiceInfo serviceInfo : stackInfo.getServices()) {
      if(serviceInfo.getCommandScript() != null) {
        actionMetadata.addServiceCheckAction(serviceInfo.getName());
      }
    }

  }


  /**
   * Determines exact service directory that contains scripts and templates
   * for service. If given stack has not this folder, inheritance hierarhy is
   * queried.
   */
  String resolveServicePackageFolder(String stackRoot,
                                     StackInfo stackInfo, String serviceFolderName,
                                     String serviceName) throws AmbariException {
    String stackId = String.format("%s-%s",
            stackInfo.getName(), stackInfo.getVersion());
    String expectedSubPath = stackInfo.getName() + File.separator +
                    stackInfo.getVersion() + File.separator +
                    AmbariMetaInfo.SERVICES_FOLDER_NAME +
                    File.separator + serviceFolderName + File.separator +
                    PACKAGE_FOLDER_NAME;
    File packageDir = new File(stackRoot + File.separator + expectedSubPath);
    String servicePackageFolder = null;
    if (packageDir.isDirectory()) {
      servicePackageFolder = expectedSubPath;
      String message = String.format(
              "Service package folder for service %s" +
                      " for stack %s has been resolved to %s",
              serviceName, stackId, servicePackageFolder);
      LOG.debug(message);
    } else {
        String message = String.format(
                "Service package folder %s for service %s " +
                        " for stack %s does not exist.",
                packageDir.getAbsolutePath(), serviceName, stackId);
        LOG.debug(message);
    }
    return servicePackageFolder;
  }


  public List<StackInfo> getAllAvailableStacks() {
    return new ArrayList<StackInfo>(stackVersionMap.values());
  }

  public List<StackInfo> getParents(StackInfo stackInfo) {
    return stackParentsMap.get(stackInfo.getVersion());
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
            AmbariMetaInfo.SCHEMA_VERSION_2.equals(value)) {
      return AmbariMetaInfo.SCHEMA_VERSION_2;
    } else {
      String message = String.format("Unknown schema version %s at file " +
              "%s", value, stackMetainfoFile.getAbsolutePath());
      throw new AmbariException(message);
    }

  }

  /**
   * Get populated stackInfo for the stack definition at the provided path.
   * @param stackVersionFolder Path to stack definition.
   * @return StackInfo StackInfo object
   * @throws JAXBException
   */
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

      // Populating hooks dir for stack
      String hooksSubPath = stackInfo.getName() + File.separator +
              stackInfo.getVersion() + File.separator + HOOKS_FOLDER_NAME;
      String hooksAbsPath = stackVersionFolder.getAbsolutePath() +
              File.separator + HOOKS_FOLDER_NAME;
      if (new File(hooksAbsPath).exists()) {
        stackInfo.setStackHooksFolder(hooksSubPath);
      } else {
        String message = String.format("Hooks folder %s does not exist",
                hooksAbsPath);
        LOG.debug(message);
      }

      String rcoFileLocation = stackVersionFolder.getAbsolutePath() +
              File.separator + AmbariMetaInfo.RCO_FILE_NAME;
      if (new File(rcoFileLocation).exists())
        stackInfo.setRcoFileLocation(rcoFileLocation);
      
      setStackPropertiesFromConfigs(stackInfo);
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
  
  private void populateStackProperties(StackInfo stackInfo, File configFile) throws JAXBException {
    ConfigurationXml configuration = unmarshal(ConfigurationXml.class, configFile);
    String fileName = configFile.getName();
    stackInfo.getProperties().addAll(getProperties(configuration, fileName));
    String configType = ConfigHelper.fileNameToConfigType(fileName);

    addConfigType(stackInfo.getConfigTypes(), configType);
    setConfigTypeAttributes(stackInfo.getConfigTypes(), configuration, configType);
  }
  
  /**
   * Get all properties from all "configs/*.xml" files. See {@see AmbariMetaInfo#SERVICE_CONFIG_FILE_NAME_POSTFIX}
   */
  void setStackPropertiesFromConfigs(StackInfo stackInfo) {
    File configsFolder = new File(stackRoot.getAbsolutePath() + File
        .separator + stackInfo.getName() + File.separator + stackInfo.getVersion()
        + File.separator + AmbariMetaInfo.SERVICE_CONFIG_FOLDER_NAME);
    
    if (!configsFolder.exists() || !configsFolder.isDirectory())
      return;
    
    File[] configFiles = configsFolder.listFiles(AmbariMetaInfo.FILENAME_FILTER);
    if (configFiles != null) {
      for (File configFile : configFiles) {
        if (configFile.getName().endsWith(AmbariMetaInfo.SERVICE_CONFIG_FILE_NAME_POSTFIX)) {
          try {
            populateStackProperties(stackInfo, configFile);
          } catch (Exception e) {
            LOG.error("Could not load configuration for " + configFile, e);
          }
        }
      }
    }
  }

  private List<PropertyInfo> getProperties(ConfigurationXml configuration, String fileName) {
    List<PropertyInfo> list = new ArrayList<PropertyInfo>();
    for (PropertyInfo pi : configuration.getProperties()) {
      pi.setFilename(fileName);
      list.add(pi);
    }
    return list;
  }

  /**
   * Add properties and config type's properties from configuration file
   */
  void populateServiceProperties(File configFile, ServiceInfo serviceInfo) throws JAXBException {
    ConfigurationXml configuration = unmarshal(ConfigurationXml.class, configFile);
    String fileName = configFile.getName();
    serviceInfo.getProperties().addAll(getProperties(configuration, fileName));
    String configType = ConfigHelper.fileNameToConfigType(fileName);

    addConfigType(serviceInfo.getConfigTypes(), configType);
    setConfigTypeAttributes(serviceInfo.getConfigTypes(), configuration, configType);
  }
  
  void setConfigTypeAttributes(Map<String, Map<String, Map<String, String>>> configTypes, ConfigurationXml configuration, String configType) {
    for (Map.Entry<QName, String> attribute : configuration.getAttributes().entrySet()) {
      for (Supports supportsProperty : Supports.values()) {
        String attributeName = attribute.getKey().getLocalPart();
        String attributeValue = attribute.getValue();
        if (attributeName.equals(supportsProperty.getXmlAttributeName())) {
          addConfigTypeProperty(configTypes, configType, Supports.KEYWORD,
              supportsProperty.getPropertyName(), Boolean.valueOf(attributeValue).toString());
        }
      }
    }
  }
  
  void addConfigType(Map<String, Map<String, Map<String, String>>> configTypes, String configType) {
    configTypes.put(configType, new HashMap<String, Map<String, String>>());
    
    Map<String, Map<String, String>> properties = configTypes.get(configType);
    Map<String, String> supportsProperties = new HashMap<String, String>();
    for (Supports supportsProperty : Supports.values()) {
      supportsProperties.put(supportsProperty.getPropertyName(), supportsProperty.getDefaultValue());
    }
    properties.put(Supports.KEYWORD, supportsProperties); 
  }

  /**
   * Populate ServiceInfo#configTypes with default entries based on ServiceInfo#configDependencies property
   */
  void populateConfigTypesFromDependencies(ServiceInfo serviceInfo) {
    List<PropertyInfo> configurations = serviceInfo.getProperties();
    if (configurations != null) {
      Map<String, Map<String, Map<String, String>>> configTypes = new HashMap<String, Map<String, Map<String, String>>>();
      for (PropertyInfo configuration : configurations) {
        String configType = ConfigHelper.fileNameToConfigType(configuration.getFilename());
        
        if (!configTypes.containsKey(configType)) {
          Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();
          Map<String, String> supportsProperties = new HashMap<String, String>();
          for (Supports supportsProperty : Supports.values()) {
            supportsProperties.put(supportsProperty.getPropertyName(), supportsProperty.getDefaultValue());
          }
          properties.put(Supports.KEYWORD, supportsProperties);
          configTypes.put(configType, properties);
        }
      }
      serviceInfo.setConfigTypes(configTypes);
    }
  }

  /**
   * Put new property entry to ServiceInfo#configTypes collection for specified configType
   */
  void addConfigTypeProperty(Map<String, Map<String, Map<String, String>>> configTypes, String configType,
      String propertiesGroupName, String key, String value) {
   if (configTypes != null && configTypes.containsKey(configType)) {
      Map<String, Map<String, String>> configDependencyProperties = configTypes.get(configType);
      if (!configDependencyProperties.containsKey(propertiesGroupName)) {
        configDependencyProperties.put(propertiesGroupName, new HashMap<String, String>());
      }
      Map<String, String> propertiesGroup = configDependencyProperties.get(propertiesGroupName);
      propertiesGroup.put(key, value);
    }
  }

  /**
   * Get all properties from all "configs/*.xml" files. See {@see AmbariMetaInfo#SERVICE_CONFIG_FILE_NAME_POSTFIX}
   */
  void setPropertiesFromConfigs(File serviceFolder, ServiceInfo serviceInfo) {
    
    File serviceConfigFolder = new File(serviceFolder.getAbsolutePath()
            + File.separator + serviceInfo.getConfigDir());
    
    if (!serviceConfigFolder.exists() || !serviceConfigFolder.isDirectory())
      return;
    
    File[] configFiles = serviceConfigFolder.listFiles(AmbariMetaInfo.FILENAME_FILTER);
    if (configFiles != null) {
      for (File configFile : configFiles) {
        if (configFile.getName().endsWith(AmbariMetaInfo.SERVICE_CONFIG_FILE_NAME_POSTFIX)) {
          try {
            populateServiceProperties(configFile, serviceInfo);
          } catch (Exception e) {
            LOG.error("Could not load configuration for " + configFile, e);
          }
        }
      }
    }
  }
  
  public static <T> T unmarshal(Class<T> clz, File file) throws JAXBException {
    Unmarshaller u = _jaxbContexts.get(clz).createUnmarshaller();
    
    return clz.cast(u.unmarshal(file));
  }  
  
  /**
   * Service configuration-types can support different abilities. This
   * enumerates the various abilities that configuration-types can support.
   * 
   * For example, Hadoop configuration types like 'core-site' and 'hdfs-site'
   * can support the ability to define certain configs as 'final'.
   */
  protected enum Supports {

    FINAL("supports_final"),
    ADDING_FORBIDDEN("supports_adding_forbidden"),
    DO_NOT_EXTEND("supports_do_not_extend");

    public static final String KEYWORD = "supports";

    private String defaultValue;
    private String xmlAttributeName;

    private Supports(String xmlAttributeName) {
      this(xmlAttributeName, Boolean.FALSE.toString());
    }

    private Supports(String xmlAttributeName, String defaultValue) {
      this.defaultValue = defaultValue;
      this.xmlAttributeName = xmlAttributeName;
    }

    public String getDefaultValue() {
      return defaultValue;
    }

    public String getXmlAttributeName() {
      return xmlAttributeName;
    }

    public String getPropertyName() {
      return name().toLowerCase();
    }
  }
}
