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

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackInfo;

import javax.ws.rs.Path;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * ServiceInfo responsible getting information about cluster.
 */
@Path("/metainfo/")
public class AmbariMetaInfo {

  List<StackInfo> stacksResult = new ArrayList<StackInfo>();

  private final static Logger log = LoggerFactory.getLogger(AmbariMetaInfo.class);

  private static final String SERVICES_FOLDER_NAME = "services";
  private static final String SERVICE_METAINFO_FILE_NAME = "metainfo.xml";
  private static final String SERVICE_CONFIG_FOLDER_NAME = "configs";
  private static final String SERVICE_CONFIG_FILE_NAME_POSTFIX = "-site.xml";

  private static final String REPOSITORY_FILE_NAME = "repoinfo.xml";
  private static final String REPOSITORY_FOLDER_NAME = "repos";
  private static final String REPOSITORY_XML_MAIN_BLOCK_NAME = "repo";
  private static final String REPOSITORY_XML_PROPERTY_URL = "url";
  private static final String REPOSITORY_XML_PROPERTY_OS = "os";
  private static final String REPOSITORY_XML_PROPERTY_DESCRIPTION = "description";

  private static final String METAINFO_XML_MAIN_BLOCK_NAME = "metainfo";
  private static final String METAINFO_XML_PROPERTY_VERSION = "version";
  private static final String METAINFO_XML_PROPERTY_USER = "user";
  private static final String METAINFO_XML_PROPERTY_COMMENT = "comment";

  private static final String PROPERTY_XML_MAIN_BLOCK_NAME = "property";
  private static final String PROPERTY_XML_PROPERTY_NAME = "name";
  private static final String PROPERTY_XML_PROPERTY_VALUE = "value";
  private static final String PROPERTY_XML_PROPERTY_DESCRIPTION = "description";


  public AmbariMetaInfo() throws Exception {
    getConfigurationInformation();
  }

  List<ServiceInfo> getSupportedServices(String stackName, String version) {
    return null;
  }

  List<ServiceInfo> getDependentServices(String stackName, String version, String serviceName) {
    return null;
  }

  Map<String, Map<String, String>> getSupportedConfigs(String stackName, String version, String serviceName) {
    return null;
  }

  List<StackInfo> getSupportedStack() {
    return null;
  }

  private void getConfigurationInformation() throws Exception {


    File stackRoot = new File(new Configuration().getMetadataPath());
//    File stackRoot = new File("src/main/resources/stacks");
    if (!stackRoot.isDirectory() && !stackRoot.exists())
      throw new IOException("" + Configuration.METADETA_DIR_PATH + " should be a directory with stack.");
    File[] stacks = stackRoot.listFiles();
    for (File stackFolder : stacks) {
      if (stackFolder.isFile()) continue;
      File[] concretStacks = stackFolder.listFiles();
      for (File stack : concretStacks) {
        if (stack.isFile()) continue;
        StackInfo stackInfo = new StackInfo();
        stackInfo.setName(stackFolder.getName());
        stackInfo.setVersion(stack.getName());
        stacksResult.add(stackInfo);
        //get repository data for current stack of techs
        File repositoryFolder = new File(stack.getAbsolutePath() + File.separator + REPOSITORY_FOLDER_NAME + File.separator + REPOSITORY_FILE_NAME);

        if (repositoryFolder.exists()) {
          List<RepositoryInfo> repositoryInfoList = getRepository(repositoryFolder);
          stackInfo.getRepositories().addAll(repositoryInfoList);
        }


        //Get services for this stack
        File servicesRootFolder = new File(stack.getAbsolutePath() + File.separator + SERVICES_FOLDER_NAME);
        File[] servicesFolders = servicesRootFolder.listFiles();

        if (servicesFolders != null)
          for (File serviceFolder : servicesFolders) {
            //Get information about service
            ServiceInfo serviceInfo = new ServiceInfo();
            serviceInfo.setName(serviceFolder.getName());
            stackInfo.getServices().add(serviceInfo);

            //Get metainfo data from metainfo.xml
            File metainfoFile = new File(serviceFolder.getAbsolutePath() + File.separator + SERVICE_METAINFO_FILE_NAME);
            if (metainfoFile.exists()) {
              setMetaInfo(metainfoFile, serviceInfo);

            }


            //Get all properties from all "configs/*-site.xml" files
            File serviceConfigFolder = new File(serviceFolder.getAbsolutePath() + File.separator + SERVICE_CONFIG_FOLDER_NAME);
            File[] configFiles = serviceConfigFolder.listFiles();
            for (File config : configFiles) {
              if (config.getName().endsWith(SERVICE_CONFIG_FILE_NAME_POSTFIX)) {
                serviceInfo.getProperties().addAll(getProperties(config));
              }
            }
          }

      }
    }//stack root

//////TODO delete before final commit. Show all objects structure for debug
//    for (StackInfo elem : stacksResult) {
//      log.info("###elem = \n" + elem);
//      log.info("contain services= " + elem.getServices().size());
//      System.out.println("###elem = \n" + elem);
//      System.out.println("contain services= " + elem.getServices().size());
//    }
//    System.out.println(" \n\n\n ");


  }


  public static void main(String[] args) throws Exception {
    AmbariMetaInfo metadata = new AmbariMetaInfo();
//    System.out.println( new Configuration().getMetadataPath() );
  }


  public List<RepositoryInfo> getRepository(File repositoryFile) {

    List<RepositoryInfo> repositorysInfo = new ArrayList<RepositoryInfo>();
    try {

      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      Document doc = dBuilder.parse(repositoryFile);
      doc.getDocumentElement().normalize();

      NodeList propertyNodes = doc.getElementsByTagName(REPOSITORY_XML_MAIN_BLOCK_NAME);

      for (int index = 0; index < propertyNodes.getLength(); index++) {

        Node node = propertyNodes.item(index);
        if (node.getNodeType() == Node.ELEMENT_NODE) {

          Element property = (Element) node;
          RepositoryInfo repositoryInfo = new RepositoryInfo();
          repositoryInfo.setUrl(getTagValue(REPOSITORY_XML_PROPERTY_URL, property));
          repositoryInfo.setOs(getTagValue(REPOSITORY_XML_PROPERTY_OS, property));
          repositoryInfo.setDescription(getTagValue(REPOSITORY_XML_PROPERTY_DESCRIPTION, property));
          repositorysInfo.add(repositoryInfo);
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
      log.error("Error while parsing metainf.xml", e);
    } catch (IOException e) {
      log.error("Error while open metainf.xml", e);
    } catch (ParserConfigurationException e) {
      log.error("Error while parsing metainf.xml", e);
    }

    doc.getDocumentElement().normalize();

    NodeList metaInfoNodes = doc.getElementsByTagName(METAINFO_XML_MAIN_BLOCK_NAME);

    if (metaInfoNodes.getLength() > 0) {
      Node metaInfoNode = metaInfoNodes.item(0);
      if (metaInfoNode.getNodeType() == Node.ELEMENT_NODE) {

        Element metaInfoElem = (Element) metaInfoNode;

        serviceInfo.setVersion(getTagValue(METAINFO_XML_PROPERTY_VERSION, metaInfoElem));
        serviceInfo.setUser(getTagValue(METAINFO_XML_PROPERTY_USER, metaInfoElem));
        serviceInfo.setComment(getTagValue(METAINFO_XML_PROPERTY_COMMENT, metaInfoElem));
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


      NodeList propertyNodes = doc.getElementsByTagName(PROPERTY_XML_MAIN_BLOCK_NAME);

      for (int index = 0; index < propertyNodes.getLength(); index++) {

        Node node = propertyNodes.item(index);
        if (node.getNodeType() == Node.ELEMENT_NODE) {

          Element property = (Element) node;
          PropertyInfo propertyInfo = new PropertyInfo();
          propertyInfo.setName(getTagValue(PROPERTY_XML_PROPERTY_NAME, property));
          propertyInfo.setValue(getTagValue(PROPERTY_XML_PROPERTY_VALUE, property));
          propertyInfo.setDescription(getTagValue(PROPERTY_XML_PROPERTY_DESCRIPTION, property));

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
      NodeList element = rawElement.getElementsByTagName(sTag).item(0).getChildNodes();
      Node value = (Node) element.item(0);
      result = value.getNodeValue();
    } catch (NullPointerException e) {
      log.debug("There is no field like " + sTag + "in this DOM element.", e);
    } catch (Exception e) {
      log.error("Error while getting value from xml DOM element", e);
      throw e;
    } finally {
      return result;
    }

  }

}


