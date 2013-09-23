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

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

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
          // Get information about service
          ServiceInfo serviceInfo = new ServiceInfo();
          serviceInfo.setName(serviceFolder.getName());
          File metainfoFile = new File(serviceFolder.getAbsolutePath()
            + File.separator + AmbariMetaInfo.SERVICE_METAINFO_FILE_NAME);

          setMetaInfo(metainfoFile, serviceInfo);
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

  private StackInfo getStackInfo(File stackVersionFolder) {
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

      try {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(stackMetainfoFile);
        doc.getDocumentElement().normalize();

        NodeList stackNodes = doc
          .getElementsByTagName(AmbariMetaInfo.STACK_XML_MAIN_BLOCK_NAME);

        for (int index = 0; index < stackNodes.getLength(); index++) {
          Node node = stackNodes.item(index);

          if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element property = (Element) node;

            stackInfo.setMinUpgradeVersion(getTagValue(
              AmbariMetaInfo.STACK_XML_PROPERTY_UPGRADE, property));

            stackInfo.setActive(Boolean.parseBoolean(getTagValue(
              AmbariMetaInfo.STACK_XML_PROPERTY_ACTIVE, property)));

            stackInfo.setParentStackVersion(getTagValue
              (AmbariMetaInfo.STACK_XML_PROPERTY_PARENT_STACK, property));
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
        return null;
      }
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
      .getElementsByTagName(AmbariMetaInfo.METAINFO_XML_MAIN_BLOCK_NAME);

    if (metaInfoNodes.getLength() > 0) {
      Node metaInfoNode = metaInfoNodes.item(0);
      if (metaInfoNode.getNodeType() == Node.ELEMENT_NODE) {

        Element metaInfoElem = (Element) metaInfoNode;

        serviceInfo.setVersion(getTagValue(AmbariMetaInfo.METAINFO_XML_PROPERTY_VERSION,
          metaInfoElem));
        serviceInfo.setUser(getTagValue(AmbariMetaInfo.METAINFO_XML_PROPERTY_USER,
          metaInfoElem));
        serviceInfo.setComment(getTagValue(AmbariMetaInfo.METAINFO_XML_PROPERTY_COMMENT,
          metaInfoElem));
        serviceInfo.setDeleted(getTagValue(AmbariMetaInfo.METAINFO_XML_PROPERTY_IS_DELETED,
          metaInfoElem));
      }
    }

    NodeList componentInfoNodes = doc
      .getElementsByTagName(AmbariMetaInfo.METAINFO_XML_PROPERTY_COMPONENT_MAIN);

    if (componentInfoNodes.getLength() > 0) {
      for (int index = 0; index < componentInfoNodes.getLength(); index++) {
        Node componentInfoNode = componentInfoNodes.item(index);
        if (componentInfoNode.getNodeType() == Node.ELEMENT_NODE) {
          Element componentInfoElem = (Element) componentInfoNode;

          ComponentInfo componentInfo = new ComponentInfo();
          componentInfo.setName(getTagValue(
            AmbariMetaInfo.METAINFO_XML_PROPERTY_COMPONENT_NAME, componentInfoElem));
          componentInfo.setCategory(getTagValue(
            AmbariMetaInfo.METAINFO_XML_PROPERTY_COMPONENT_CATEGORY, componentInfoElem));
          componentInfo.setDeleted(getTagValue(AmbariMetaInfo
            .METAINFO_XML_PROPERTY_IS_DELETED, componentInfoElem));
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
        .getElementsByTagName(AmbariMetaInfo.PROPERTY_XML_MAIN_BLOCK_NAME);

      for (int index = 0; index < propertyNodes.getLength(); index++) {

        Node node = propertyNodes.item(index);
        if (node.getNodeType() == Node.ELEMENT_NODE) {
          Element property = (Element) node;
          PropertyInfo propertyInfo = new PropertyInfo();
          propertyInfo
            .setName(getTagValue(AmbariMetaInfo.PROPERTY_XML_PROPERTY_NAME, property));
          propertyInfo.setValue(getTagValue(AmbariMetaInfo.PROPERTY_XML_PROPERTY_VALUE,
            property));
          propertyInfo.setDescription(getTagValue(
            AmbariMetaInfo.PROPERTY_XML_PROPERTY_DESCRIPTION, property));
          propertyInfo.setDeleted(getTagValue(AmbariMetaInfo
            .PROPERTY_XML_PROPERTY_IS_DELETED, property));

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
}