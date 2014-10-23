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

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.state.*;
import org.apache.ambari.server.state.stack.ConfigurationXml;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class StackExtensionHelperTest {

  private final String stackRootStr = "./src/test/resources/stacks/".
      replaceAll("/", File.separator);

  private Injector injector = Guice.createInjector(new MockModule());
  
  
  public class MockModule extends AbstractModule {
    @Override
    protected void configure() {
      bind(ActionMetadata.class);
    }
  }

  /**
  * Checks than service metainfo is parsed correctly both for ver 1 services
  * and for ver 2 services
  */
  @Test
  public void testPopulateServicesForStack() throws Exception {
    File stackRoot = new File(stackRootStr);
    StackInfo stackInfo = new StackInfo();
    stackInfo.setName("HDP");
    stackInfo.setVersion("2.0.7");
    StackExtensionHelper helper = new StackExtensionHelper(injector, stackRoot);
    helper.populateServicesForStack(stackInfo);
    List<ServiceInfo> services =  stackInfo.getServices();
    assertEquals(8, services.size());
    for (ServiceInfo serviceInfo : services) {
      if (serviceInfo.getName().equals("HIVE")) {
        // Check old-style service
        assertEquals("HIVE", serviceInfo.getName());
        assertEquals("Hive", serviceInfo.getDisplayName());
        assertEquals("2.0", serviceInfo.getSchemaVersion());
        assertTrue(serviceInfo.getComment().startsWith("Data warehouse system"));
        assertEquals("0.11.0.2.0.5.0", serviceInfo.getVersion());
        // Check some component definitions
        List<ComponentInfo> components = serviceInfo.getComponents();
        assertEquals("HIVE_METASTORE", components.get(0).getName());
        assertEquals("Hive Metastore", components.get(0).getDisplayName());
        assertEquals("MASTER", components.get(0).getCategory());
        List<PropertyInfo> properties = serviceInfo.getProperties();
        // Check some property
        assertEquals(37, properties.size());
        boolean found = false;
        for (PropertyInfo property : properties) {
          if (property.getName().equals("javax.jdo.option.ConnectionDriverName")) {
            assertEquals("com.mysql.jdbc.Driver", property.getValue());
            assertEquals("hive-site.xml",
                    property.getFilename());
            assertEquals(false, property.isDeleted());
            found = true;
          }
        }
        assertTrue("Property not found in a list of properties", found);
        // Check config dependencies
        List<String> configDependencies = serviceInfo.getConfigDependencies();
        assertEquals(2, configDependencies.size());
        assertEquals("hive-site", configDependencies.get(1));
      } else if (serviceInfo.getName().equals("HBASE")) {
        assertEquals("HBASE", serviceInfo.getName());
        assertEquals("HDP/2.0.7/services/HBASE/package",
                serviceInfo.getServicePackageFolder());
        assertEquals("2.0", serviceInfo.getSchemaVersion());
        assertTrue(serviceInfo.getComment().startsWith("Non-relational distr"));
        assertEquals("0.96.0.2.0.6.0", serviceInfo.getVersion());
        // Check some component definitions
        List<ComponentInfo> components = serviceInfo.getComponents();
        assertTrue(components.size() == 3);
        ComponentInfo firstComponent = components.get(0);
        assertEquals("HBASE_MASTER", firstComponent.getName());
        assertEquals("MASTER", firstComponent.getCategory());
        // Check command script for component
        assertEquals("scripts/hbase_master.py",
                firstComponent.getCommandScript().getScript());
        assertEquals(CommandScriptDefinition.Type.PYTHON,
                firstComponent.getCommandScript().getScriptType());
        assertEquals(777,
                firstComponent.getCommandScript().getTimeout());
        // Check custom commands for component
        List<CustomCommandDefinition> customCommands =
                firstComponent.getCustomCommands();
        assertEquals(2, customCommands.size());
        assertEquals("RESTART", customCommands.get(0).getName());
        assertTrue(firstComponent.isCustomCommand("RESTART"));
        assertEquals("scripts/hbase_master_restart.py",
                customCommands.get(0).getCommandScript().getScript());
        assertEquals(CommandScriptDefinition.Type.PYTHON,
                customCommands.get(0).getCommandScript().getScriptType());
        assertEquals(888,
                customCommands.get(0).getCommandScript().getTimeout());
        // Check all parsed os specifics
        Map<String,ServiceOsSpecific> specifics = serviceInfo.getOsSpecifics();
        assertTrue(specifics.size() == 2);
        ServiceOsSpecific anyOs = specifics.get(AmbariMetaInfo.ANY_OS);
        assertEquals(AmbariMetaInfo.ANY_OS, anyOs.getOsFamily());
        assertEquals("wget", anyOs.getPackages().get(0).getName());

        // Test default timeout value
        ComponentInfo secondComponent = components.get(1);
        assertEquals("HBASE_REGIONSERVER", secondComponent.getName());
        assertEquals(0,
                secondComponent.getCommandScript().getTimeout());

        ServiceOsSpecific c6Os = specifics.get("centos6");
        assertEquals("centos6", c6Os.getOsFamily());
        assertEquals("hbase", c6Os.getPackages().get(0).getName());
        assertEquals("http://something.com/centos6/2.x/updates/1",
                c6Os.getRepo().getBaseUrl());
        assertEquals("Custom-repo-1",
                c6Os.getRepo().getRepoId());
        assertEquals("Custom-repo",
                c6Os.getRepo().getRepoName());
        // Check custom commands for service
        assertTrue(serviceInfo.getCustomCommands().size() == 1);
        CustomCommandDefinition customCommand =
                serviceInfo.getCustomCommands().get(0);
        assertEquals("SERVICE_VALIDATION", customCommand.getName());
        assertEquals("scripts/hbase_validation.py",
                customCommand.getCommandScript().getScript());
        assertEquals(CommandScriptDefinition.Type.PYTHON,
                customCommand.getCommandScript().getScriptType());
        assertEquals(300, customCommand.getCommandScript().getTimeout());
        // Check command script for service
        CommandScriptDefinition serviceScriptDefinition = serviceInfo.getCommandScript();
        assertEquals("scripts/service_check.py", serviceScriptDefinition.getScript());
        assertEquals(CommandScriptDefinition.Type.PYTHON,
                serviceScriptDefinition.getScriptType());
        assertEquals(50, serviceScriptDefinition.getTimeout());
        // Check some property
        List<PropertyInfo> properties = serviceInfo.getProperties();
        List<PropertyInfo> emptyValueProperties = new ArrayList<PropertyInfo>();
        for (PropertyInfo propertyInfo : properties) {
          if (propertyInfo.getValue().isEmpty()) {
            emptyValueProperties.add(propertyInfo);
          }
        }
        assertEquals(28, emptyValueProperties.size());
        assertEquals(68, properties.size());
        boolean foundHBaseClusterDistributed = false;
        boolean foundHBaseRegionServerXmnMax = false;
        boolean foundHBaseRegionServerXmnRatio = false;
        for (PropertyInfo property : properties) {
          if (property.getName().equals("hbase.cluster.distributed")) {
            assertEquals("true",
                    property.getValue());
            assertTrue(property.getDescription().startsWith("The mode the"));
            assertEquals("hbase-site.xml",
                    property.getFilename());
            foundHBaseClusterDistributed = true;
          } else if (property.getName().equals("hbase_regionserver_xmn_max")) {
            assertEquals("512", property.getValue());
            assertEquals("global.xml",
                property.getFilename());
            foundHBaseRegionServerXmnMax = true;
          } else if (property.getName().equals("hbase_regionserver_xmn_ratio")) {
            assertEquals("global.xml",
                property.getFilename());
            assertEquals("0.2", property.getValue());
            foundHBaseRegionServerXmnRatio = true;
          }
        }

        assertTrue("Property hbase.cluster.distributed not found in a list of properties",
            foundHBaseClusterDistributed);
        assertTrue("Property hbase_regionserver_xmn_max not found in a list of properties",
            foundHBaseRegionServerXmnMax);
        assertTrue("Property hbase_regionserver_xmn_ratio not found in a list of properties",
            foundHBaseRegionServerXmnRatio);

        List<String> configDependencies = serviceInfo.getConfigDependencies();
        assertEquals(3, configDependencies.size());
        assertEquals("global", configDependencies.get(0));
        assertEquals("hbase-policy", configDependencies.get(1));
        assertEquals("hbase-site", configDependencies.get(2));
      } else if(serviceInfo.getName().equals("ZOOKEEPER")) {
        assertTrue(serviceInfo.isRestartRequiredAfterChange());
      } else {
        if (!serviceInfo.getName().equals("YARN") &&
            !serviceInfo.getName().equals("HDFS") &&
            !serviceInfo.getName().equals("MAPREDUCE2") &&
            !serviceInfo.getName().equals("NAGIOS") &&
            !serviceInfo.getName().equals("SQOOP")) {
          fail("Unknown service");
        }
      }
    }
  }

  @Test
  public void testConfigDependenciesInheritance() throws Exception{
    File stackRoot = new File(stackRootStr);
    StackInfo stackInfo = new StackInfo();
    stackInfo.setName("HDP");
    stackInfo.setVersion("2.0.6");
    StackExtensionHelper helper = new StackExtensionHelper(injector, stackRoot);
    helper.populateServicesForStack(stackInfo);
    helper.fillInfo();
    List<ServiceInfo> allServices = helper.getAllApplicableServices(stackInfo);
    for (ServiceInfo serviceInfo : allServices) {
      if (serviceInfo.getName().equals("HDFS")) {
        assertEquals(5, serviceInfo.getConfigDependencies().size());
        assertEquals(4, serviceInfo.getConfigTypes().size());
        assertTrue(serviceInfo.getConfigDependencies().contains("core-site"));
        assertTrue(serviceInfo.getConfigDependencies().contains("global"));
        assertTrue(serviceInfo.getConfigDependencies().contains("hdfs-site"));
        assertTrue(serviceInfo.getConfigDependencies().contains("hdfs-log4j"));
        assertTrue(serviceInfo.getConfigDependencies().contains("hadoop-policy"));
        assertTrue(Boolean.valueOf(serviceInfo.getConfigTypes().get("core-site").get("supports").get("final")));
        assertFalse(Boolean.valueOf(serviceInfo.getConfigTypes().get("global").get("supports").get("final")));
      } else if (serviceInfo.getName().equals("WEBHCAT")) {
        assertEquals(1, serviceInfo.getConfigDependencies().size());
        assertEquals(1, serviceInfo.getConfigTypes().size());
        assertTrue(serviceInfo.getConfigDependencies().contains("webhcat-site"));
        assertTrue(Boolean.valueOf(serviceInfo.getConfigTypes().get("webhcat-site").get("supports").get("final")));
      }
    }
  }

  @Test
  public void testClientConfigFilesInheritance() throws Exception{
    File stackRoot = new File(stackRootStr);
    StackInfo stackInfo = new StackInfo();
    stackInfo.setName("HDP");
    stackInfo.setVersion("2.0.6");
    StackExtensionHelper helper = new StackExtensionHelper(injector, stackRoot);
    helper.populateServicesForStack(stackInfo);
    helper.fillInfo();
    List<ServiceInfo> allServices = helper.getAllApplicableServices(stackInfo);
    for (ServiceInfo serviceInfo : allServices) {
      if (serviceInfo.getName().equals("ZOOKEEPER")) {
        List<ComponentInfo> components = serviceInfo.getComponents();
        assertTrue(components.size() == 2);
        ComponentInfo componentInfo = components.get(1);
        List<ClientConfigFileDefinition> clientConfigs = componentInfo.getClientConfigFiles();
        assertEquals(2,clientConfigs.size());
        assertEquals("zookeeper-env",clientConfigs.get(0).getDictionaryName());
        assertEquals("zookeeper-env.sh",clientConfigs.get(0).getFileName());
        assertEquals("env",clientConfigs.get(0).getType());
        assertEquals("zookeeper-log4j",clientConfigs.get(1).getDictionaryName());
        assertEquals("log4j.properties",clientConfigs.get(1).getFileName());
        assertEquals("env",clientConfigs.get(1).getType());
      }
    }
  }

  @Test
  public void testMonitoringServicePropertyInheritance() throws Exception{
    File stackRoot = new File(stackRootStr);
    StackInfo stackInfo = new StackInfo();
    stackInfo.setName("HDP");
    stackInfo.setVersion("2.0.7");
    StackExtensionHelper helper = new StackExtensionHelper(injector, stackRoot);
    helper.populateServicesForStack(stackInfo);
    helper.fillInfo();
    List<ServiceInfo> allServices = helper.getAllApplicableServices(stackInfo);
    assertEquals(13, allServices.size());
    for (ServiceInfo serviceInfo : allServices) {
      if (serviceInfo.getName().equals("NAGIOS")) {
        assertTrue(serviceInfo.isMonitoringService());
      } else {
        assertNull(serviceInfo.isMonitoringService());
      }
    }
  }

  @Test
  public void getSchemaVersion() throws Exception {
    File stackRoot = new File(stackRootStr);
    StackExtensionHelper helper = new StackExtensionHelper(injector, stackRoot);
    File legacyMetaInfoFile = new File("./src/test/resources/stacks/HDP/2.0.7/" +
            "services/HIVE/metainfo.xml".replaceAll("/", File.separator));
    String version = helper.getSchemaVersion(legacyMetaInfoFile);
    assertEquals("2.0", version);

    File v2MetaInfoFile = new File("./src/test/resources/stacks/HDP/2.0.7/" +
            "services/HBASE/metainfo.xml".replaceAll("/", File.separator));
    version = helper.getSchemaVersion(v2MetaInfoFile);
    assertEquals("2.0", version);
  }
  
  public StackExtensionHelper getStackExtensionHelper() {
    File stackRoot = new File(stackRootStr);    
    return new StackExtensionHelper(injector, stackRoot);
  }
  
  public ServiceInfo getServiceFromStack(StackExtensionHelper helper, String stackName, String stackVersion, String serviceName) throws XPathExpressionException, ParserConfigurationException, SAXException, IOException, JAXBException {
    StackInfo stackInfo = new StackInfo();
    stackInfo.setName(stackName);
    stackInfo.setVersion(stackVersion);
    
    helper.populateServicesForStack(stackInfo);
    
    for(ServiceInfo service:stackInfo.getServices()) {
      if(service.getName().equals(serviceName)) {
        return service;
      }
    }
    return null;
  }

  private void addToPropertyMap(Map<String, Map<String, Map<String, String>>> configTypes,String configType,
      String keyword, String attributeName, String value) {
    if(!configTypes.containsKey(configType)) {
      configTypes.put(configType, new HashMap<String, Map<String, String>>()); 
    }
    Map<String, Map<String, String>> config = configTypes.get(configType);
    if(!config.containsKey(keyword)) {
      config.put(keyword, new HashMap<String, String>());
    }
    Map<String, String> supports = config.get(keyword);
    supports.put(attributeName, value);    
  }

  /**
   * This test ensures the service status check is added into the action metadata when
   * the stack has no parent and is the only stack in the stack family
   * @throws Exception
   */
  @Test
  public void testGetServiceInfoFromSingleStack() throws Exception {
    File stackRoot = new File("./src/test/resources/single_stack".replace("/", File.separator));
    StackExtensionHelper helper = new StackExtensionHelper(injector, stackRoot);
    helper.fillInfo();
    List<StackInfo> stackInfoList = helper.getAllAvailableStacks();
    assertEquals(1, stackInfoList.size());

    List<ServiceInfo> serviceInfoList = helper.getAllApplicableServices(stackInfoList.get(0));
    for(ServiceInfo serviceInfo: serviceInfoList) {
      if ("HDFS".equalsIgnoreCase(serviceInfo.getName())) {
        ActionMetadata actionMetadata = injector.getInstance(ActionMetadata.class);
        String hdfsStatusCheckCmd = actionMetadata.getServiceCheckAction("HDFS");
        assertEquals("HDFS_SERVICE_CHECK", hdfsStatusCheckCmd);
        break;
      }
    }
  }

  @Test
  public void testPopulateConfigTypes() throws XPathExpressionException, ParserConfigurationException, SAXException, IOException, JAXBException {
    StackExtensionHelper helper = getStackExtensionHelper();
    ServiceInfo serviceInfo = getServiceFromStack(helper, "HDP", "2.0.7", "HDFS");
    
    
    Map<String, Map<String, Map<String, String>>> expectedConfigTypes = new HashMap<String, Map<String, Map<String, String>>>();
    addToPropertyMap(expectedConfigTypes, "global", StackExtensionHelper.Supports.KEYWORD, 
        StackExtensionHelper.Supports.FINAL.getPropertyName(), "true");
    addToPropertyMap(expectedConfigTypes, "global", StackExtensionHelper.Supports.KEYWORD, 
        StackExtensionHelper.Supports.ADDING_FORBIDDEN.getPropertyName(), "false");
    addToPropertyMap(expectedConfigTypes, "global", StackExtensionHelper.Supports.KEYWORD,
        StackExtensionHelper.Supports.DO_NOT_EXTEND.getPropertyName(), "false");
    addToPropertyMap(expectedConfigTypes, "hdfs-site", StackExtensionHelper.Supports.KEYWORD, 
        StackExtensionHelper.Supports.FINAL.getPropertyName(), "false");
    addToPropertyMap(expectedConfigTypes, "hdfs-site", StackExtensionHelper.Supports.KEYWORD, 
        StackExtensionHelper.Supports.ADDING_FORBIDDEN.getPropertyName(), "false");
    addToPropertyMap(expectedConfigTypes, "hdfs-site", StackExtensionHelper.Supports.KEYWORD,
        StackExtensionHelper.Supports.DO_NOT_EXTEND.getPropertyName(), "false");
    addToPropertyMap(expectedConfigTypes, "hadoop-policy", StackExtensionHelper.Supports.KEYWORD, 
        StackExtensionHelper.Supports.FINAL.getPropertyName(), "false");
    addToPropertyMap(expectedConfigTypes, "hadoop-policy", StackExtensionHelper.Supports.KEYWORD, 
        StackExtensionHelper.Supports.ADDING_FORBIDDEN.getPropertyName(), "false");
    addToPropertyMap(expectedConfigTypes, "hadoop-policy", StackExtensionHelper.Supports.KEYWORD,
        StackExtensionHelper.Supports.DO_NOT_EXTEND.getPropertyName(), "false");
    addToPropertyMap(expectedConfigTypes, "core-site", StackExtensionHelper.Supports.KEYWORD, 
        StackExtensionHelper.Supports.FINAL.getPropertyName(), "false");
    addToPropertyMap(expectedConfigTypes, "core-site", StackExtensionHelper.Supports.KEYWORD, 
        StackExtensionHelper.Supports.ADDING_FORBIDDEN.getPropertyName(), "false");
    addToPropertyMap(expectedConfigTypes, "core-site", StackExtensionHelper.Supports.KEYWORD,
        StackExtensionHelper.Supports.DO_NOT_EXTEND.getPropertyName(), "false");
    
    Map<String, Map<String, Map<String, String>>> configTypes = serviceInfo.getConfigTypes();
    assertEquals(4, configTypes.size());
    assertEquals(expectedConfigTypes, configTypes);
  }

  @Test
  public void testAddConfigTypeProperty_configTypesIsNull() {
    // init
    File stackRoot = new File(stackRootStr);
    StackExtensionHelper helper = new StackExtensionHelper(injector, stackRoot);
    ServiceInfo serviceInfo = createMock(ServiceInfo.class);

    // expectations
    expect(serviceInfo.getConfigTypes()).andReturn(null);
    replay(serviceInfo);

    // eval
    helper.addConfigTypeProperty(serviceInfo.getConfigTypes(), "dep", "group", "key", "value");

    // verification
    verify(serviceInfo);
  }

  @Test
  public void testAddConfigTypeProperty_groupDoesNotExist() {
    // init
    File stackRoot = new File(stackRootStr);
    StackExtensionHelper helper = new StackExtensionHelper(injector, stackRoot);
    ServiceInfo serviceInfo = new ServiceInfo();
    Map<String, Map<String, Map<String, String>>> configTypes = new HashMap<String, Map<String, Map<String, String>>>();
    Map<String, Map<String, String>> groupMap = new HashMap<String, Map<String, String>>();
    configTypes.put("dep", groupMap);
    serviceInfo.setConfigTypes(configTypes);

    // eval
    helper.addConfigTypeProperty(serviceInfo.getConfigTypes(), "dep", "group", "key", "value");

    // assert
    configTypes = serviceInfo.getConfigTypes();
    assertEquals(1, configTypes.size());
    assertTrue(configTypes.containsKey("dep"));
    Map<String, Map<String, String>> configType = configTypes.get("dep");
    assertTrue(configType.containsKey("group"));
    Map<String, String> group = configType.get("group");
    assertEquals(1, group.size());
    assertTrue(group.containsKey("key"));
    assertEquals("value", group.get("key"));
  }

  @Test
  public void testAddConfigTypeProperty_typeDoesNotExist() {
    // init
    File stackRoot = new File(stackRootStr);
    StackExtensionHelper helper = new StackExtensionHelper(injector, stackRoot);
    ServiceInfo serviceInfo = new ServiceInfo();
    Map<String, Map<String, Map<String, String>>> configTypes = new HashMap<String, Map<String, Map<String, String>>>();
    Map<String, Map<String, String>> groupMap = new HashMap<String, Map<String, String>>();
    configTypes.put("dep", groupMap);
    serviceInfo.setConfigTypes(configTypes);

    // eval
    helper.addConfigTypeProperty(serviceInfo.getConfigTypes(), "no_such_dep", "group", "key", "value");

    // assert
    configTypes = serviceInfo.getConfigTypes();
    assertEquals(1, configTypes.size());
    assertFalse(configTypes.containsKey("no_such_dep"));
    assertTrue(configTypes.containsKey("dep"));
    Map<String, Map<String, String>> configType = configTypes.get("dep");
    assertEquals(0, configType.size());
  }

  @Test
  public void testAddConfigTypeProperty_groupExist() {
    // init
    File stackRoot = new File(stackRootStr);
    StackExtensionHelper helper = new StackExtensionHelper(injector, stackRoot);
    ServiceInfo serviceInfo = new ServiceInfo();
    Map<String, Map<String, Map<String, String>>> configTypes = new HashMap<String, Map<String, Map<String, String>>>();
    Map<String, Map<String, String>> groupMap = new HashMap<String, Map<String, String>>();
    Map<String, String> propertiesMap = new HashMap<String, String>();
    groupMap.put("group", propertiesMap);
    configTypes.put("dep", groupMap);
    serviceInfo.setConfigTypes(configTypes);

    // eval
    helper.addConfigTypeProperty(serviceInfo.getConfigTypes(), "dep", "group", "key", "value");

    // assert
    configTypes = serviceInfo.getConfigTypes();
    assertEquals(1, configTypes.size());
    assertTrue(configTypes.containsKey("dep"));
    Map<String, Map<String, String>> configType = configTypes.get("dep");
    assertTrue(configType.containsKey("group"));
    Map<String, String> group = configType.get("group");
    assertTrue(group.containsKey("key"));
    assertEquals("value", group.get("key"));
  }

  @Test
  public void testPopulateServiceProperties_noSupportsFinalFlag() throws Exception {
    StackExtensionHelper helper = getStackExtensionHelper();
    ServiceInfo serviceInfo = getServiceFromStack(helper, "HDP", "2.0.7", "YARN");
    
    File configFile = new File(stackRootStr
        + "HDP/2.0.7/services/YARN/configuration/yarn-site.xml".replaceAll("/", File.separator));
    
    helper.populateServiceProperties(configFile, serviceInfo);
    
    Map<String, Map<String, Map<String, String>>> expectedConfigTypes = new HashMap<String, Map<String, Map<String, String>>>();
    addToPropertyMap(expectedConfigTypes, "yarn-site", StackExtensionHelper.Supports.KEYWORD, 
        StackExtensionHelper.Supports.FINAL.getPropertyName(), "false");
    addToPropertyMap(expectedConfigTypes, "yarn-site", StackExtensionHelper.Supports.KEYWORD, 
        StackExtensionHelper.Supports.ADDING_FORBIDDEN.getPropertyName(), "true");
    addToPropertyMap(expectedConfigTypes, "yarn-site", StackExtensionHelper.Supports.KEYWORD,
        StackExtensionHelper.Supports.DO_NOT_EXTEND.getPropertyName(), "true");
    
    assertEquals(expectedConfigTypes, serviceInfo.getConfigTypes());
  }

  @Test
  public void testPopulateServiceProperties_supportsFinalTrue() throws Exception {
    StackExtensionHelper helper = getStackExtensionHelper();
    ServiceInfo serviceInfo = getServiceFromStack(helper, "HDP", "2.0.7", "HDFS");
    
    File configFile = new File(stackRootStr
        + "HDP/2.0.7/services/HDFS/configuration/global.xml".replaceAll("/", File.separator));
    helper.populateServiceProperties(configFile, serviceInfo);
    
    Map<String, Map<String, Map<String, String>>> expectedConfigTypes = new HashMap<String, Map<String, Map<String, String>>>();
    addToPropertyMap(expectedConfigTypes, "global", StackExtensionHelper.Supports.KEYWORD, 
        StackExtensionHelper.Supports.FINAL.getPropertyName(), "true");
    addToPropertyMap(expectedConfigTypes, "global", StackExtensionHelper.Supports.KEYWORD, 
        StackExtensionHelper.Supports.ADDING_FORBIDDEN.getPropertyName(), "false");
    addToPropertyMap(expectedConfigTypes, "global", StackExtensionHelper.Supports.KEYWORD,
        StackExtensionHelper.Supports.DO_NOT_EXTEND.getPropertyName(), "false");
    addToPropertyMap(expectedConfigTypes, "hdfs-site", StackExtensionHelper.Supports.KEYWORD, 
        StackExtensionHelper.Supports.FINAL.getPropertyName(), "false");
    addToPropertyMap(expectedConfigTypes, "hdfs-site", StackExtensionHelper.Supports.KEYWORD, 
        StackExtensionHelper.Supports.ADDING_FORBIDDEN.getPropertyName(), "false");
    addToPropertyMap(expectedConfigTypes, "hdfs-site", StackExtensionHelper.Supports.KEYWORD,
        StackExtensionHelper.Supports.DO_NOT_EXTEND.getPropertyName(), "false");
    addToPropertyMap(expectedConfigTypes, "hadoop-policy", StackExtensionHelper.Supports.KEYWORD, 
        StackExtensionHelper.Supports.FINAL.getPropertyName(), "false");
    addToPropertyMap(expectedConfigTypes, "hadoop-policy", StackExtensionHelper.Supports.KEYWORD, 
        StackExtensionHelper.Supports.ADDING_FORBIDDEN.getPropertyName(), "false");
    addToPropertyMap(expectedConfigTypes, "hadoop-policy", StackExtensionHelper.Supports.KEYWORD,
        StackExtensionHelper.Supports.DO_NOT_EXTEND.getPropertyName(), "false");
    addToPropertyMap(expectedConfigTypes, "core-site", StackExtensionHelper.Supports.KEYWORD, 
        StackExtensionHelper.Supports.FINAL.getPropertyName(), "false");
    addToPropertyMap(expectedConfigTypes, "core-site", StackExtensionHelper.Supports.KEYWORD, 
        StackExtensionHelper.Supports.ADDING_FORBIDDEN.getPropertyName(), "false");
    addToPropertyMap(expectedConfigTypes, "core-site", StackExtensionHelper.Supports.KEYWORD,
        StackExtensionHelper.Supports.DO_NOT_EXTEND.getPropertyName(), "false");
    
    assertEquals(expectedConfigTypes, serviceInfo.getConfigTypes());
  }

  @Test
  public void testPopulateServiceProperties_supportsFinalFalse() throws Exception {
    StackExtensionHelper helper = getStackExtensionHelper();
    ServiceInfo serviceInfo = getServiceFromStack(helper, "HDP", "2.0.7", "HDFS");
    File configFile = new File(stackRootStr
        + "HDP/2.0.7/services/YARN/configuration/yarn-site.xml".replaceAll("/", File.separator));
    helper.populateServiceProperties(configFile, serviceInfo);
    
    Map<String, Map<String, Map<String, String>>> expectedConfigTypes = new HashMap<String, Map<String, Map<String, String>>>();
    addToPropertyMap(expectedConfigTypes, "global", StackExtensionHelper.Supports.KEYWORD, 
        StackExtensionHelper.Supports.FINAL.getPropertyName(), "true");
    addToPropertyMap(expectedConfigTypes, "global", StackExtensionHelper.Supports.KEYWORD, 
        StackExtensionHelper.Supports.ADDING_FORBIDDEN.getPropertyName(), "false");
    addToPropertyMap(expectedConfigTypes, "global", StackExtensionHelper.Supports.KEYWORD,
        StackExtensionHelper.Supports.DO_NOT_EXTEND.getPropertyName(), "false");
    addToPropertyMap(expectedConfigTypes, "hdfs-site", StackExtensionHelper.Supports.KEYWORD, 
        StackExtensionHelper.Supports.FINAL.getPropertyName(), "false");
    addToPropertyMap(expectedConfigTypes, "hdfs-site", StackExtensionHelper.Supports.KEYWORD, 
        StackExtensionHelper.Supports.ADDING_FORBIDDEN.getPropertyName(), "false");
    addToPropertyMap(expectedConfigTypes, "hdfs-site", StackExtensionHelper.Supports.KEYWORD,
        StackExtensionHelper.Supports.DO_NOT_EXTEND.getPropertyName(), "false");
    addToPropertyMap(expectedConfigTypes, "hadoop-policy", StackExtensionHelper.Supports.KEYWORD, 
        StackExtensionHelper.Supports.FINAL.getPropertyName(), "false");
    addToPropertyMap(expectedConfigTypes, "hadoop-policy", StackExtensionHelper.Supports.KEYWORD, 
        StackExtensionHelper.Supports.ADDING_FORBIDDEN.getPropertyName(), "false");
    addToPropertyMap(expectedConfigTypes, "hadoop-policy", StackExtensionHelper.Supports.KEYWORD,
        StackExtensionHelper.Supports.DO_NOT_EXTEND.getPropertyName(), "false");
    addToPropertyMap(expectedConfigTypes, "core-site", StackExtensionHelper.Supports.KEYWORD, 
        StackExtensionHelper.Supports.FINAL.getPropertyName(), "false");
    addToPropertyMap(expectedConfigTypes, "core-site", StackExtensionHelper.Supports.KEYWORD, 
        StackExtensionHelper.Supports.ADDING_FORBIDDEN.getPropertyName(), "false");
    addToPropertyMap(expectedConfigTypes, "core-site", StackExtensionHelper.Supports.KEYWORD,
        StackExtensionHelper.Supports.DO_NOT_EXTEND.getPropertyName(), "false");
    addToPropertyMap(expectedConfigTypes, "yarn-site", StackExtensionHelper.Supports.KEYWORD, 
        StackExtensionHelper.Supports.FINAL.getPropertyName(), "false");
    addToPropertyMap(expectedConfigTypes, "yarn-site", StackExtensionHelper.Supports.KEYWORD, 
        StackExtensionHelper.Supports.ADDING_FORBIDDEN.getPropertyName(), "true");
    addToPropertyMap(expectedConfigTypes, "yarn-site", StackExtensionHelper.Supports.KEYWORD,
        StackExtensionHelper.Supports.DO_NOT_EXTEND.getPropertyName(), "true");
    
    assertEquals(expectedConfigTypes, serviceInfo.getConfigTypes());
  }

  @Test
  public void testPopulateServiceProperties_supportsFinalWrongType() throws Exception {
    // init
    File stackRoot = new File(stackRootStr);
    StackExtensionHelper helper = createMockBuilder(StackExtensionHelper.class).addMockedMethod("addConfigTypeProperty")
        .withConstructor(injector, stackRoot).createMock();
    File config = new File("./src/test/resources/bad-stacks/HDP/0.1/services/YARN/configuration/yarn-site.xml"
        .replaceAll("/", File.separator));
    ServiceInfo serviceInfo = createNiceMock(ServiceInfo.class);
    List<PropertyInfo> properties = createNiceMock(List.class);

    // expectations
    expect(serviceInfo.getConfigTypes()).andReturn(new HashMap<String, Map<String, Map<String, String>>>()).times(2);
    expect(serviceInfo.getProperties()).andReturn(properties).times(1);
    expect(properties.addAll((Collection) anyObject())).andReturn(true).times(1);    
    replay(properties, serviceInfo);

    // eval
    helper.populateServiceProperties(config, serviceInfo);

    // verification
    verify(properties, serviceInfo);
  }

  @Test
  public void testPopulateServiceProperties_configTypesIsNull() throws Exception {
    // init
    File stackRoot = new File(stackRootStr);
    StackExtensionHelper helper = new StackExtensionHelper(injector, stackRoot);
    File config = new File(stackRootStr
        + "HDP/2.1.1/services/PIG/configuration/pig-properties.xml".replaceAll("/", File.separator));
    ServiceInfo serviceInfo = createNiceMock(ServiceInfo.class);
    List<PropertyInfo> properties = createMock(List.class);

    // expectations
    expect(serviceInfo.getConfigTypes()).andReturn(new HashMap<String, Map<String, Map<String, String>>>()).times(2);
    expect(serviceInfo.getProperties()).andReturn(properties).times(1);
    expect(properties.addAll((Collection) anyObject())).andReturn(true).times(1);
    replay(properties);
    replay(serviceInfo);

    // eval
    helper.populateServiceProperties(config, serviceInfo);

    // verification
    verify(properties, serviceInfo);
  }

  @Test
  public void testUnmarshallConfigurationXml() throws Exception {
    File configFile = new File("./src/test/resources/bad-stacks/HDP/0.1/services/YARN/configuration/capacity-scheduler.xml");
    ConfigurationXml config = StackExtensionHelper.unmarshal(ConfigurationXml.class, configFile);
    Map<QName, String> attributes = config.getAttributes();
    List<PropertyInfo> properties = config.getProperties();

    // attributes verification
    assertEquals(2, attributes.size());
    QName supportsFinal = new QName("", "supports_final");
    assertTrue(attributes.containsKey(supportsFinal));
    assertEquals("false", attributes.get(supportsFinal));
    QName supportsDeletable = new QName("", "supports_deletable");
    assertTrue(attributes.containsKey(supportsDeletable));
    assertEquals("false", attributes.get(supportsDeletable));

    // properties verification
    assertEquals(3, properties.size());

    PropertyInfo propertyInfo;
    propertyInfo = properties.get(0);
    assertEquals("yarn.scheduler.capacity.maximum-applications", propertyInfo.getName());
    assertEquals("Maximum number of applications that can be pending and running.", propertyInfo.getDescription());
    assertEquals("10000", propertyInfo.getValue());
    assertEquals(1, propertyInfo.getAttributesMap().size());
    assertEquals("true", propertyInfo.getAttributesMap().get("final"));
    assertEquals(null, propertyInfo.getFilename());
    assertEquals(false, propertyInfo.isDeleted());
    assertEquals(false, propertyInfo.isRequireInput());
    assertTrue(propertyInfo.getPropertyTypes().isEmpty());

    propertyInfo = properties.get(1);
    assertEquals("yarn.scheduler.capacity.maximum-am-resource-percent", propertyInfo.getName());
    assertEquals("Maximum percent of resources in the cluster.", propertyInfo.getDescription());
    assertEquals("0.2", propertyInfo.getValue());
    assertEquals(1, propertyInfo.getAttributesMap().size());
    assertEquals("false", propertyInfo.getAttributesMap().get("final"));
    assertEquals(null, propertyInfo.getFilename());
    assertEquals(true, propertyInfo.isDeleted());
    assertEquals(false, propertyInfo.isRequireInput());
    assertTrue(propertyInfo.getPropertyTypes().isEmpty());

    propertyInfo = properties.get(2);
    assertEquals("yarn.scheduler.capacity.root.queues", propertyInfo.getName());
    assertEquals("The queues at the this level (root is the root queue).", propertyInfo.getDescription());
    assertEquals("default", propertyInfo.getValue());
    assertEquals(0, propertyInfo.getAttributesMap().size());
    assertEquals(null, propertyInfo.getFilename());
    assertEquals(false, propertyInfo.isDeleted());
    assertEquals(true, propertyInfo.isRequireInput());
    assertTrue(propertyInfo.getPropertyTypes().isEmpty());
  }

  @Test
  public void testMergeServices_BothConfigTypesAreNull() throws Exception {
    File stackRoot = new File(stackRootStr);
    StackExtensionHelper helper = new StackExtensionHelper(injector, stackRoot);
    ServiceInfo child = new ServiceInfo();
    ServiceInfo parent = new ServiceInfo();

    child.setConfigTypes(null);
    child.setConfigDependencies(null);

    parent.setConfigTypes(null);
    parent.setConfigDependencies(null);

    ServiceInfo merged = helper.mergeServices(parent, child);

    assertNotNull(merged.getConfigDependencies());
    assertEquals(0, merged.getConfigDependencies().size());
    assertNotNull(merged.getConfigTypes());
    assertEquals(0, merged.getConfigTypes().size());
  }

  @Test
  public void testServiceInheritance() throws Exception {
    File stackRoot = new File(stackRootStr);
    StackInfo stackInfo = new StackInfo();
    stackInfo.setName("HDP");
    stackInfo.setVersion("2.0.6");
    StackExtensionHelper helper = new StackExtensionHelper(injector, stackRoot);
    helper.populateServicesForStack(stackInfo);
    helper.fillInfo();
    List<ServiceInfo> allServices = helper.getAllApplicableServices(stackInfo);
    for (ServiceInfo serviceInfo : allServices) {
      if (serviceInfo.getName().equals("HDFS")){
        assertEquals("HDFS", serviceInfo.getName());
        assertNotNull(serviceInfo.getMetricsFile()); 
      } 
    }
    
  }
  
  
  @Test
  public void testMergeComponentInfo() throws Exception {
    File stackRoot = new File(stackRootStr);
    StackExtensionHelper helper = new StackExtensionHelper(injector, stackRoot);
    ComponentInfo child = new ComponentInfo();
    ComponentInfo parent = new ComponentInfo();
    DependencyInfo a = new DependencyInfo();
    a.setName("serviceName/A");
    DependencyInfo b = new DependencyInfo();
    b.setName("serviceName/B");
    List<DependencyInfo> parentDependencies = new ArrayList<DependencyInfo>();
    parentDependencies.add(a);
    parentDependencies.add(b);
    parent.setDependencies(parentDependencies);

    DependencyInfo c = new DependencyInfo();
    c.setName("serviceName/C");
    List<DependencyInfo> childDependencies = new ArrayList<DependencyInfo>();
    childDependencies.add(c);
    child.setDependencies(childDependencies);

    child.setCardinality("1");
    parent.setCardinality("1+");

    child.setCategory("CLIENT");
    parent.setCategory("MASTER");

    ComponentInfo result = helper.mergeComponents(parent, child);

    assertEquals(result.getCardinality(),"1");
    assertEquals(result.getCategory(), "CLIENT");
    assertEquals(result.getDependencies().size(), 3);
  }
}

