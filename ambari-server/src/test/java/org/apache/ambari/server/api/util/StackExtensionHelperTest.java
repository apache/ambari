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
import org.apache.ambari.server.state.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

import org.junit.Test;

public class StackExtensionHelperTest {

  private final String stackRootStr = "./src/test/resources/stacks/".
          replaceAll("/", File.separator);
  private final String hBaseDirStr = stackRootStr + "services/HBASE/".
          replaceAll("/", File.separator);
  private final String yarnDirStr = stackRootStr + "services/YARN/".
          replaceAll("/", File.separator);

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
    StackExtensionHelper helper = new StackExtensionHelper(stackRoot);
    helper.populateServicesForStack(stackInfo);
    List<ServiceInfo> services =  stackInfo.getServices();
    assertEquals(6, services.size());
    for (ServiceInfo serviceInfo : services) {
      if (serviceInfo.getName().equals("HIVE")) {
        // Check old-style service
        assertEquals("HIVE", serviceInfo.getName());
        assertEquals("1.0", serviceInfo.getSchemaVersion());
        assertEquals("root", serviceInfo.getUser());
        assertTrue(serviceInfo.getComment().startsWith("Data warehouse system"));
        assertEquals("0.11.0.2.0.5.0", serviceInfo.getVersion());
        // Check some component definitions
        List<ComponentInfo> components = serviceInfo.getComponents();
        assertEquals("HIVE_METASTORE", components.get(0).getName());
        assertEquals("MASTER", components.get(0).getCategory());
        List<PropertyInfo> properties = serviceInfo.getProperties();
        // Check some property
        assertEquals(38, properties.size());
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
        assertEquals("rpm", anyOs.getPackages().get(0).getType());
        assertEquals("wget", anyOs.getPackages().get(0).getName());

        // Test default timeout value
        ComponentInfo secondComponent = components.get(1);
        assertEquals("HBASE_REGIONSERVER", secondComponent.getName());
        assertEquals(0,
                secondComponent.getCommandScript().getTimeout());

        ServiceOsSpecific c6Os = specifics.get("centos6");
        assertEquals("centos6", c6Os.getOsFamily());
        assertEquals("rpm", c6Os.getPackages().get(0).getType());
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
        assertEquals(66, properties.size());
        boolean found = false;
        for (PropertyInfo property : properties) {
          if (property.getName().equals("hbase.cluster.distributed")) {
            assertEquals("true",
                    property.getValue());
            assertTrue(property.getDescription().startsWith("The mode the"));
            assertEquals("hbase-site.xml",
                    property.getFilename());
            found = true;
          }
        }
        assertTrue("Property not found in a list of properties", found);
        List<String> configDependencies = serviceInfo.getConfigDependencies();
        assertEquals(3, configDependencies.size());
        assertEquals("global", configDependencies.get(0));
        assertEquals("hbase-policy", configDependencies.get(1));
        assertEquals("hbase-site", configDependencies.get(2));
      } else {
        if (!serviceInfo.getName().equals("YARN") &&
            !serviceInfo.getName().equals("HDFS") &&
            !serviceInfo.getName().equals("MAPREDUCE2") &&
            !serviceInfo.getName().equals("SQOOP")) {
          fail("Unknown service");
        }
      }
    }
  }

  @Test
  public void getSchemaVersion() throws Exception {
    File stackRoot = new File(stackRootStr);
    StackExtensionHelper helper = new StackExtensionHelper(stackRoot);
    File legacyMetaInfoFile = new File("./src/test/resources/stacks/HDP/2.0.7/" +
            "services/HIVE/metainfo.xml".replaceAll("/", File.separator));
    String version = helper.getSchemaVersion(legacyMetaInfoFile);
    assertEquals("1.0", version);

    File v2MetaInfoFile = new File("./src/test/resources/stacks/HDP/2.0.7/" +
            "services/HBASE/metainfo.xml".replaceAll("/", File.separator));
    version = helper.getSchemaVersion(v2MetaInfoFile);
    assertEquals("2.0", version);
  }
}

