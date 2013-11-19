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

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.stack.ConfigurationXml;
import org.apache.ambari.server.state.stack.RepositoryXml;
import org.apache.ambari.server.state.stack.ServiceMetainfoXml;
import org.apache.ambari.server.state.stack.StackMetainfoXml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  private final Map<String, List<StackInfo>> stackParentsMap;

  private static final Map<Class<?>, JAXBContext> _jaxbContexts =
      new HashMap<Class<?>, JAXBContext> ();
  static {
    try {
      // two classes define the top-level element "metainfo", so we need 2 contexts.
      JAXBContext ctx = JAXBContext.newInstance(StackMetainfoXml.class, RepositoryXml.class, ConfigurationXml.class);
      _jaxbContexts.put(StackMetainfoXml.class, ctx);
      _jaxbContexts.put(RepositoryXml.class, ctx);
      _jaxbContexts.put(ConfigurationXml.class, ctx);
      _jaxbContexts.put(ServiceMetainfoXml.class, JAXBContext.newInstance(ServiceMetainfoXml.class));
    } catch (JAXBException e) {
      throw new RuntimeException (e);
    }
  }  
  
  public StackExtensionHelper(File stackRoot) throws Exception {
    this.stackRoot = stackRoot;
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
    this.stackParentsMap = getParentStacksInOrder(stackVersionMap.values());
  }

  private ServiceInfo mergeServices(ServiceInfo parentService,
                                    ServiceInfo childService) {
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

  private void populateServicesForStack(StackInfo stackInfo) {
    List<ServiceInfo> services = new ArrayList<ServiceInfo>();
    File servicesFolder = new File(stackRoot.getAbsolutePath() + File
      .separator + stackInfo.getName() + File.separator + stackInfo.getVersion()
      + File.separator + AmbariMetaInfo.SERVICES_FOLDER_NAME);
    if (!servicesFolder.exists()) {
      LOG.info("No services defined for stack: " + stackInfo.getName() +
      "-" + stackInfo.getVersion());

    } else {
      File[] servicesFolders = servicesFolder.listFiles(AmbariMetaInfo
        .FILENAME_FILTER);
      if (servicesFolders != null) {
        for (File serviceFolder : servicesFolders) {
          if (!serviceFolder.isDirectory())
            continue;
          
          // Get information about service
          ServiceInfo serviceInfo = new ServiceInfo();
          serviceInfo.setName(serviceFolder.getName());
          File metainfoFile = new File(serviceFolder.getAbsolutePath()
            + File.separator + AmbariMetaInfo.SERVICE_METAINFO_FILE_NAME);
          
          setMetaInfo(metainfoFile, serviceInfo);
          
          // get metrics file, if it exists
          File metricsJson = new File(serviceFolder.getAbsolutePath()
              + File.separator + AmbariMetaInfo.SERVICE_METRIC_FILE_NAME);
          if (metricsJson.exists())
            serviceInfo.setMetricsFile(metricsJson);
          
          // Add now to be removed while iterating extension graph
          services.add(serviceInfo);

          // Get all properties from all "configs/*-site.xml" files
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

  private void setMetaInfo(File metainfoFile, ServiceInfo serviceInfo) {
    try {
      ServiceMetainfoXml smx = unmarshal(ServiceMetainfoXml.class, metainfoFile);
      
      serviceInfo.setComment(smx.getComment());
      serviceInfo.setUser(smx.getUser());
      serviceInfo.setVersion(smx.getVersion());
      serviceInfo.setDeleted(smx.isDeleted());
      serviceInfo.setConfigDependencies(smx.getConfigDependencies());
      
      serviceInfo.getComponents().addAll(smx.getComponents());
    } catch (Exception e) {
      LOG.error("Error while parsing metainfo.xml for a service", e);
    }

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
  
  public static <T> T unmarshal(Class<T> clz, File file) throws JAXBException {
    Unmarshaller u = _jaxbContexts.get(clz).createUnmarshaller();
    
    return clz.cast(u.unmarshal(file));
  }  
  
}