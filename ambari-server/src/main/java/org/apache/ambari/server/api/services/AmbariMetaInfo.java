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

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

/**
 * ServiceInfo responsible getting information about cluster.
 */
@Path("/metainfo/")
public class AmbariMetaInfo {

  private String CONFIG_FILE_PATH = "C:\\workspace\\stacks";//Configuration.CONFIG_FILE

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

  private List<ServiceInfo> getConfigurationInformation() throws Exception {
    List<StackInfo> stacksResult = new ArrayList<StackInfo>();

    File stackRoot = new File(CONFIG_FILE_PATH);
    if (!stackRoot.isDirectory())
      throw new IOException("" + CONFIG_FILE_PATH + " should be a directory with stack.");
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
        File repositoryFolder = new File(stack.getAbsolutePath() + File.separator + "repos" + File.separator + "repoinfo.xml");

        if (repositoryFolder.exists()) {
          List<RepositoryInfo> repositoryInfoList = getRepository(repositoryFolder);
          stackInfo.getRepositories().addAll(repositoryInfoList);
        }


        //get services for this stack
        File servicesRootFolder = new File(stack.getAbsolutePath() + File.separator + "services");
        File[] servicesFolders = servicesRootFolder.listFiles();

        if (servicesFolders != null)
          for (File serviceFolder : servicesFolders) {
            //Get information about service
            ServiceInfo serviceInfo = new ServiceInfo();
            serviceInfo.setName(serviceFolder.getName());
            stackInfo.getServices().add(serviceInfo);


            //Get all properties from all "configs/*-site.xml" files
            File serviceConfigFolder = new File(serviceFolder.getAbsolutePath() + File.separator + "configs");
            File[] configFiles = serviceConfigFolder.listFiles();
            for (File config : configFiles) {
              if (config.getName().endsWith("-site.xml")) {
                serviceInfo.getProperties().addAll(getProperties(config));
              }

            }
          }

      }
    }//stack root

    for (StackInfo elem : stacksResult) {
      System.out.println("###elem = \n" + elem);
      System.out.println("contain services= " + elem.getServices().size());
    }
    System.out.println(" \n\n\n ");


    return null;
  }


  public static void main(String[] args) throws Exception {
    AmbariMetaInfo metadata = new AmbariMetaInfo();
    metadata.getConfigurationInformation();
//    metadata.getRepository(new File("C:\\workspace\\stacks\\HDP\\0.1\\repos\\repoinfo.xml"));

  }


  public List<RepositoryInfo> getRepository(File repositoryFile) {

    List<RepositoryInfo> repositorysInfo = new ArrayList<RepositoryInfo>();
    try {

      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      Document doc = dBuilder.parse(repositoryFile);
      doc.getDocumentElement().normalize();

      NodeList propertyNodes = doc.getElementsByTagName("repo");

      for (int index = 0; index < propertyNodes.getLength(); index++) {

        Node node = propertyNodes.item(index);
        if (node.getNodeType() == Node.ELEMENT_NODE) {

          Element property = (Element) node;
          RepositoryInfo repositoryInfo = new RepositoryInfo();
          repositoryInfo.setUrl(getTagValue("url", property));
          repositoryInfo.setOs(getTagValue("os", property));
          repositoryInfo.setDescription(getTagValue("description", property));
          repositorysInfo.add(repositoryInfo);
        }
      }

    } catch (Exception e) {
      e.printStackTrace();

    }
    return repositorysInfo;
  }

  public List<PropertyInfo> getProperties(File propertyFile) {

    List<PropertyInfo> resultPropertyList = new ArrayList<PropertyInfo>();
    try {
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      Document doc = dBuilder.parse(propertyFile);
      doc.getDocumentElement().normalize();


      NodeList propertyNodes = doc.getElementsByTagName("property");

      for (int index = 0; index < propertyNodes.getLength(); index++) {

        Node node = propertyNodes.item(index);
        if (node.getNodeType() == Node.ELEMENT_NODE) {

          Element property = (Element) node;
          PropertyInfo propertyInfo = new PropertyInfo();
          propertyInfo.setName(getTagValue("name", property));
          propertyInfo.setValue(getTagValue("value", property));
          propertyInfo.setDescription(getTagValue("description", property));

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

  private static String getTagValue(String sTag, Element rawElement) {
    String result = null;
    try {
      NodeList element = rawElement.getElementsByTagName(sTag).item(0).getChildNodes();
      Node value = (Node) element.item(0);
      result = value.getNodeValue();
    } finally {
      return result;
    }

  }
}


