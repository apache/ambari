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

package org.apache.ambari.server.stack;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.orm.dao.MetainfoDAO;
import org.apache.ambari.server.state.ClientConfigFileDefinition;
import org.apache.ambari.server.state.CommandScriptDefinition;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.ServiceOsSpecific;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.stack.OsFamily;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * StackManager unit tests.
 */
public class StackManagerTest {

  private static StackManager stackManager;
  private static MetainfoDAO dao;
  private static ActionMetadata actionMetadata;
  private static OsFamily osFamily;

  @BeforeClass
  public static void initStack() throws Exception{
    stackManager = createTestStackManager();
  }

  public static StackManager createTestStackManager() throws Exception {
    return createTestStackManager("./src/test/resources/stacks/");
  }

  public static StackManager createTestStackManager(String stackRoot) throws Exception {
    try {
      //todo: dao , actionMetaData expectations
      dao = createNiceMock(MetainfoDAO.class);
      actionMetadata = createNiceMock(ActionMetadata.class);
      Configuration config = createNiceMock(Configuration.class);
      expect(config.getSharedResourcesDirPath()).andReturn("./src/test/resources").anyTimes();
      replay(config);
      osFamily = new OsFamily(config);

      replay(dao, actionMetadata);
      return new StackManager(new File(stackRoot), new StackContext(dao, actionMetadata, osFamily));
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  @Test
  public void testGetStacks_count() throws Exception {
    Collection<StackInfo> stacks = stackManager.getStacks();
    assertEquals(16, stacks.size());
  }

  @Test
  public void testGetStack_name__count() {
    Collection<StackInfo> stacks = stackManager.getStacks("HDP");
    assertEquals(12, stacks.size());

    stacks = stackManager.getStacks("OTHER");
    assertEquals(2, stacks.size());
  }

  @Test
  public void testGetStack_basic() {
    StackInfo stack = stackManager.getStack("HDP", "0.1");
    assertNotNull(stack);
    assertEquals("HDP", stack.getName());
    assertEquals("0.1", stack.getVersion());


    Collection<ServiceInfo> services = stack.getServices();
    assertEquals(3, services.size());

    Map<String, ServiceInfo> serviceMap = new HashMap<String, ServiceInfo>();
    for (ServiceInfo service : services) {
      serviceMap.put(service.getName(), service);
    }
    ServiceInfo hdfsService = serviceMap.get("HDFS");
    assertNotNull(hdfsService);
    List<ComponentInfo> components = hdfsService.getComponents();
    assertEquals(6, components.size());
    List<PropertyInfo> properties = hdfsService.getProperties();
    assertEquals(62, properties.size());

    // test a couple of the properties for filename
    boolean hdfsPropFound = false;
    boolean hbasePropFound = false;
    for (PropertyInfo p : properties) {
      if (p.getName().equals("hbase.regionserver.msginterval")) {
        assertEquals("hbase-site.xml", p.getFilename());
        hbasePropFound = true;
      } else if (p.getName().equals("dfs.name.dir")) {
        assertEquals("hdfs-site.xml", p.getFilename());
        hdfsPropFound = true;
      }
    }
    assertTrue(hbasePropFound);
    assertTrue(hdfsPropFound);

    ServiceInfo mrService = serviceMap.get("MAPREDUCE");
    assertNotNull(mrService);
    components = mrService.getComponents();
    assertEquals(3, components.size());

    ServiceInfo pigService = serviceMap.get("PIG");
    assertNotNull(pigService);
    assertEquals("PIG", pigService.getName());
    assertEquals("1.0", pigService.getVersion());
    assertNull(pigService.getParent());
    assertEquals("This is comment for PIG service", pigService.getComment());
    components = pigService.getComponents();
    assertEquals(1, components.size());
    CommandScriptDefinition commandScript = pigService.getCommandScript();
    assertEquals("scripts/service_check.py", commandScript.getScript());
    assertEquals(CommandScriptDefinition.Type.PYTHON, commandScript.getScriptType());
    assertEquals(300, commandScript.getTimeout());
    List<String> configDependencies = pigService.getConfigDependencies();
    assertEquals(1, configDependencies.size());
    assertEquals("global", configDependencies.get(0));
    assertEquals("global", pigService.getConfigDependenciesWithComponents().get(0));
    ComponentInfo client = pigService.getClientComponent();
    assertNotNull(client);
    assertEquals("PIG", client.getName());
    assertEquals("0+", client.getCardinality());
    assertEquals("CLIENT", client.getCategory());
    assertEquals("configuration", pigService.getConfigDir());
    assertEquals("2.0", pigService.getSchemaVersion());
    Map<String, ServiceOsSpecific> osInfoMap = pigService.getOsSpecifics();
    assertEquals(1, osInfoMap.size());
    ServiceOsSpecific osSpecific = osInfoMap.get("centos6");
    assertNotNull(osSpecific);
    assertEquals("centos6", osSpecific.getOsFamily());
    assertNull(osSpecific.getRepo());
    List<ServiceOsSpecific.Package> packages = osSpecific.getPackages();
    assertEquals(1, packages.size());
    ServiceOsSpecific.Package pkg = packages.get(0);
    assertEquals("pig", pkg.getName());
  }

  @Test
  public void testStackVersionInheritance_includeAllServices() {
    StackInfo stack = stackManager.getStack("HDP", "2.1.1");
    assertNotNull(stack);
    assertEquals("HDP", stack.getName());
    assertEquals("2.1.1", stack.getVersion());
    Collection<ServiceInfo> services = stack.getServices();

    //should include all stacks in hierarchy
    assertEquals(14, services.size());
    HashSet<String> expectedServices = new HashSet<String>();
    expectedServices.add("GANGLIA");
    expectedServices.add("HBASE");
    expectedServices.add("HCATALOG");
    expectedServices.add("HDFS");
    expectedServices.add("HIVE");
    expectedServices.add("MAPREDUCE2");
    expectedServices.add("OOZIE");
    expectedServices.add("PIG");
    expectedServices.add("SQOOP");
    expectedServices.add("YARN");
    expectedServices.add("ZOOKEEPER");
    expectedServices.add("STORM");
    expectedServices.add("FLUME");
    expectedServices.add("FAKENAGIOS");

    ServiceInfo pigService = null;
    for (ServiceInfo service : services) {
      if (service.getName().equals("PIG")) {
        pigService = service;
      }
      assertTrue(expectedServices.remove(service.getName()));
    }
    assertTrue(expectedServices.isEmpty());

    // extended values
    assertNotNull(pigService);
    assertEquals("0.12.1.2.1.1", pigService.getVersion());
    assertEquals("Scripting platform for analyzing large datasets (Extended)", pigService.getComment());
    //base value
    ServiceInfo basePigService = stackManager.getStack("HDP", "2.0.5").getService("PIG");
    assertEquals("0.11.1.2.0.5.0", basePigService.getVersion());
    assertEquals(1, basePigService.getComponents().size());
    // new component added in extended version
    assertEquals(2, pigService.getComponents().size());
    // no properties in base service
    assertEquals(0, basePigService.getProperties().size());
    assertEquals(1, pigService.getProperties().size());
    assertEquals("content", pigService.getProperties().get(0).getName());
  }

  @Test
  public void testGetStack_explicitServiceExtension() {
    StackInfo stack = stackManager.getStack("OTHER", "1.0");
    assertNotNull(stack);
    assertEquals("OTHER", stack.getName());
    assertEquals("1.0", stack.getVersion());
    Collection<ServiceInfo> services = stack.getServices();

    assertEquals(3, services.size());

    // hdfs service
    assertEquals(6, stack.getService("HDFS").getComponents().size());

    // Extended Sqoop service via explicit service extension
    ServiceInfo sqoopService = stack.getService("SQOOP2");
    assertNotNull(sqoopService);

    assertEquals("Extended SQOOP", sqoopService.getComment());
    assertEquals("Extended Version", sqoopService.getVersion());
    assertNull(sqoopService.getServicePackageFolder());

    Collection<ComponentInfo> components = sqoopService.getComponents();
    assertEquals(1, components.size());
    ComponentInfo component = components.iterator().next();
    assertEquals("SQOOP", component.getName());

    // Get the base sqoop service
    StackInfo baseStack = stackManager.getStack("HDP", "2.1.1");
    ServiceInfo baseSqoopService = baseStack.getService("SQOOP");

    // values from base service
    assertEquals(baseSqoopService.isDeleted(), sqoopService.isDeleted());
    assertEquals(baseSqoopService.getAlertsFile(),sqoopService.getAlertsFile());
    assertEquals(baseSqoopService.getClientComponent(), sqoopService.getClientComponent());
    assertEquals(baseSqoopService.getCommandScript(), sqoopService.getCommandScript());
    assertEquals(baseSqoopService.getConfigDependencies(), sqoopService.getConfigDependencies());
    assertEquals(baseSqoopService.getConfigDir(), sqoopService.getConfigDir());
    assertEquals(baseSqoopService.getConfigDependenciesWithComponents(), sqoopService.getConfigDependenciesWithComponents());
    assertEquals(baseSqoopService.getConfigTypeAttributes(), sqoopService.getConfigTypeAttributes());
    assertEquals(baseSqoopService.getCustomCommands(), sqoopService.getCustomCommands());
    assertEquals(baseSqoopService.getExcludedConfigTypes(), sqoopService.getExcludedConfigTypes());
    assertEquals(baseSqoopService.getProperties(), sqoopService.getProperties());
    assertEquals(baseSqoopService.getMetrics(), sqoopService.getMetrics());
    assertNull(baseSqoopService.getMetricsFile());
    assertNull(sqoopService.getMetricsFile());
    assertEquals(baseSqoopService.getOsSpecifics(), sqoopService.getOsSpecifics());
    assertEquals(baseSqoopService.getRequiredServices(), sqoopService.getRequiredServices());
    assertEquals(baseSqoopService.getSchemaVersion(), sqoopService.getSchemaVersion());

    // extended Storm service via explicit service extension
    ServiceInfo stormService = stack.getService("STORM");
    assertNotNull(stormService);
    assertEquals("STORM", stormService.getName());

    // base storm service
    ServiceInfo baseStormService = baseStack.getService("STORM");

    // overridden value
    assertEquals("Apache Hadoop Stream processing framework (Extended)", stormService.getComment());
    assertEquals("New version", stormService.getVersion());
    assertEquals("OTHER/1.0/services/STORM/package", stormService.getServicePackageFolder());
    // compare components
    List<ComponentInfo> stormServiceComponents = stormService.getComponents();
    List<ComponentInfo> baseStormServiceComponents = baseStormService.getComponents();
    assertEquals(new HashSet<ComponentInfo>(stormServiceComponents), new HashSet<ComponentInfo>(baseStormServiceComponents));
    // values from base service
    assertEquals(baseStormService.isDeleted(), stormService.isDeleted());
    //todo: specify alerts file in stack
    assertEquals(baseStormService.getAlertsFile(),stormService.getAlertsFile());

    assertEquals(baseStormService.getClientComponent(), stormService.getClientComponent());
    assertEquals(baseStormService.getCommandScript(), stormService.getCommandScript());
    assertEquals(baseStormService.getConfigDependencies(), stormService.getConfigDependencies());
    assertEquals(baseStormService.getConfigDir(), stormService.getConfigDir());
    assertEquals(baseStormService.getConfigDependenciesWithComponents(), stormService.getConfigDependenciesWithComponents());
    assertEquals(baseStormService.getConfigTypeAttributes(), stormService.getConfigTypeAttributes());
    assertEquals(baseStormService.getCustomCommands(), stormService.getCustomCommands());
    assertEquals(baseStormService.getExcludedConfigTypes(), stormService.getExcludedConfigTypes());
    assertEquals(baseStormService.getProperties(), stormService.getProperties());
    assertEquals(baseStormService.getMetrics(), stormService.getMetrics());
    assertNotNull(baseStormService.getMetricsFile());
    assertNotNull(stormService.getMetricsFile());
    assertFalse(baseStormService.getMetricsFile().equals(stormService.getMetricsFile()));
    assertEquals(baseStormService.getOsSpecifics(), stormService.getOsSpecifics());
    assertEquals(baseStormService.getRequiredServices(), stormService.getRequiredServices());
    assertEquals(baseStormService.getSchemaVersion(), stormService.getSchemaVersion());
  }

  @Test
  public void testGetStack_versionInheritance__explicitServiceExtension() {
    StackInfo baseStack = stackManager.getStack("OTHER", "1.0");
    StackInfo stack = stackManager.getStack("OTHER", "2.0");

    assertEquals(4, stack.getServices().size());

    ServiceInfo service = stack.getService("SQOOP2");
    ServiceInfo baseSqoopService = baseStack.getService("SQOOP2");

    assertEquals("SQOOP2", service.getName());
    assertEquals("Inherited from parent", service.getComment());
    assertEquals("Extended from parent version", service.getVersion());
    assertNull(service.getServicePackageFolder());
    // compare components
    List<ComponentInfo> serviceComponents = service.getComponents();
    List<ComponentInfo> baseStormServiceCompoents = baseSqoopService.getComponents();
    assertEquals(serviceComponents, baseStormServiceCompoents);
    // values from base service
    assertEquals(baseSqoopService.isDeleted(), service.isDeleted());
    assertEquals(baseSqoopService.getAlertsFile(),service.getAlertsFile());
    assertEquals(baseSqoopService.getClientComponent(), service.getClientComponent());
    assertEquals(baseSqoopService.getCommandScript(), service.getCommandScript());
    assertEquals(baseSqoopService.getConfigDependencies(), service.getConfigDependencies());
    assertEquals(baseSqoopService.getConfigDir(), service.getConfigDir());
    assertEquals(baseSqoopService.getConfigDependenciesWithComponents(), service.getConfigDependenciesWithComponents());
    assertEquals(baseSqoopService.getConfigTypeAttributes(), service.getConfigTypeAttributes());
    assertEquals(baseSqoopService.getCustomCommands(), service.getCustomCommands());
    assertEquals(baseSqoopService.getExcludedConfigTypes(), service.getExcludedConfigTypes());
    assertEquals(baseSqoopService.getProperties(), service.getProperties());
    assertEquals(baseSqoopService.getMetrics(), service.getMetrics());
    assertNull(baseSqoopService.getMetricsFile());
    assertNull(service.getMetricsFile());
    assertEquals(baseSqoopService.getOsSpecifics(), service.getOsSpecifics());
    assertEquals(baseSqoopService.getRequiredServices(), service.getRequiredServices());
    assertEquals(baseSqoopService.getSchemaVersion(), service.getSchemaVersion());
  }

  @Test
  public void testConfigDependenciesInheritance() throws Exception{
    StackInfo stack = stackManager.getStack("HDP", "2.0.6");
    ServiceInfo hdfsService = stack.getService("HDFS");
    assertEquals(5, hdfsService.getConfigDependencies().size());
    assertEquals(4, hdfsService.getConfigTypeAttributes().size());
    assertTrue(hdfsService.getConfigDependencies().contains("core-site"));
    assertTrue(hdfsService.getConfigDependencies().contains("global"));
    assertTrue(hdfsService.getConfigDependencies().contains("hdfs-site"));
    assertTrue(hdfsService.getConfigDependencies().contains("hdfs-log4j"));
    assertTrue(hdfsService.getConfigDependencies().contains("hadoop-policy"));
    assertTrue(Boolean.valueOf(hdfsService.getConfigTypeAttributes().get("core-site").get("supports").get("final")));
    assertFalse(Boolean.valueOf(hdfsService.getConfigTypeAttributes().get("global").get("supports").get("final")));
  }

  @Test
  public void testClientConfigFilesInheritance() throws Exception{
    StackInfo stack = stackManager.getStack("HDP", "2.0.6");
    ServiceInfo zkService = stack.getService("ZOOKEEPER");
    List<ComponentInfo> components = zkService.getComponents();
    assertTrue(components.size() == 2);
    ComponentInfo componentInfo = components.get(1);
    List<ClientConfigFileDefinition> clientConfigs = componentInfo.getClientConfigFiles();
    assertEquals(2,clientConfigs.size());
    assertEquals("zookeeper-env",clientConfigs.get(0).getDictionaryName());
    assertEquals("zookeeper-env.sh",clientConfigs.get(0).getFileName());
    assertEquals("env",clientConfigs.get(0).getType());
    assertEquals("zookeeper-log4j",clientConfigs.get(1).getDictionaryName());
    assertEquals("log4j.properties",clientConfigs.get(1).getFileName());
    assertEquals("env", clientConfigs.get(1).getType());
  }

  @Test
  public void testMonitoringServicePropertyInheritance() throws Exception{
    StackInfo stack = stackManager.getStack("HDP", "2.0.8");
    Collection<ServiceInfo> allServices = stack.getServices();
    assertEquals(13, allServices.size());

    boolean monitoringServiceFound = false;

    for (ServiceInfo serviceInfo : allServices) {
      if (serviceInfo.getName().equals("FAKENAGIOS")) {
        monitoringServiceFound = true;
        assertTrue(serviceInfo.isMonitoringService());
      } else {
        assertNull(serviceInfo.isMonitoringService());
      }
    }

    assertTrue(monitoringServiceFound);
  }

  @Test
  public void testServiceDeletion() {
    StackInfo stack = stackManager.getStack("HDP", "2.0.6");
    Collection<ServiceInfo> allServices = stack.getServices();

    assertEquals(11, allServices.size());
    HashSet<String> expectedServices = new HashSet<String>();
    expectedServices.add("GANGLIA");
    expectedServices.add("HBASE");
    expectedServices.add("HCATALOG");
    expectedServices.add("HDFS");
    expectedServices.add("HIVE");
    expectedServices.add("MAPREDUCE2");
    expectedServices.add("OOZIE");
    expectedServices.add("PIG");
    expectedServices.add("ZOOKEEPER");
    expectedServices.add("FLUME");
    expectedServices.add("YARN");

    for (ServiceInfo service : allServices) {
      assertTrue(expectedServices.remove(service.getName()));
    }
    assertTrue(expectedServices.isEmpty());
  }

  @Test
  public void testComponentDeletion() {
    StackInfo stack = stackManager.getStack("HDP", "2.0.6");
    ServiceInfo yarnService = stack.getService("YARN");
    assertNull(yarnService.getComponentByName("YARN_CLIENT"));

    stack = stackManager.getStack("HDP", "2.0.7");
    yarnService = stack.getService("YARN");
    assertNotNull(yarnService.getComponentByName("YARN_CLIENT"));
  }

  @Test
  public void testPopulateConfigTypes() throws Exception {
    StackInfo stack = stackManager.getStack("HDP", "2.0.7");
    ServiceInfo hdfsService = stack.getService("HDFS");

    Map<String, Map<String, Map<String, String>>> configTypes = hdfsService.getConfigTypeAttributes();
    assertEquals(4, configTypes.size());

    Map<String, Map<String, String>> configType = configTypes.get("global");
    assertEquals(1, configType.size());
    Map<String, String> supportsMap = configType.get("supports");
    assertEquals(3, supportsMap.size());
    assertEquals("true", supportsMap.get("final"));
    assertEquals("false", supportsMap.get("adding_forbidden"));
    assertEquals("false", supportsMap.get("do_not_extend"));

    configType = configTypes.get("hdfs-site");
    assertEquals(1, configType.size());
    supportsMap = configType.get("supports");
    assertEquals(3, supportsMap.size());
    assertEquals("false", supportsMap.get("final"));
    assertEquals("false", supportsMap.get("adding_forbidden"));
    assertEquals("false", supportsMap.get("do_not_extend"));

    configType = configTypes.get("core-site");
    assertEquals(1, configType.size());
    supportsMap = configType.get("supports");
    assertEquals(3, supportsMap.size());
    assertEquals("false", supportsMap.get("final"));
    assertEquals("false", supportsMap.get("adding_forbidden"));
    assertEquals("false", supportsMap.get("do_not_extend"));

    configType = configTypes.get("hadoop-policy");
    assertEquals(1, configType.size());
    supportsMap = configType.get("supports");
    assertEquals(3, supportsMap.size());
    assertEquals("false", supportsMap.get("final"));
    assertEquals("false", supportsMap.get("adding_forbidden"));
    assertEquals("false", supportsMap.get("do_not_extend"));

    ServiceInfo yarnService = stack.getService("YARN");
    configTypes = yarnService.getConfigTypeAttributes();
    assertEquals(4, configTypes.size());
    assertTrue(configTypes.containsKey("yarn-site"));
    assertTrue(configTypes.containsKey("core-site"));
    assertTrue(configTypes.containsKey("global"));
    assertTrue(configTypes.containsKey("capacity-scheduler"));

    configType = configTypes.get("yarn-site");
    supportsMap = configType.get("supports");
    assertEquals(3, supportsMap.size());
    assertEquals("false", supportsMap.get("final"));
    assertEquals("true", supportsMap.get("adding_forbidden"));
    assertEquals("true", supportsMap.get("do_not_extend"));

    ServiceInfo mrService = stack.getService("MAPREDUCE2");
    configTypes = mrService.getConfigTypeAttributes();
    assertEquals(3, configTypes.size());
    assertTrue(configTypes.containsKey("mapred-site"));
    assertTrue(configTypes.containsKey("core-site"));
    assertTrue(configTypes.containsKey("mapred-queue-acls"));
  }

  @Test
  public void testCycleDetection() throws Exception {
    ActionMetadata actionMetadata = createNiceMock(ActionMetadata.class);
    OsFamily osFamily = createNiceMock(OsFamily.class);
    replay(actionMetadata);
    try {
    new StackManager(new File("./src/test/resources/stacks_with_cycle/"),
        new StackContext(null, actionMetadata, osFamily));
      fail("Expected exception due to cyclic stack");
    } catch (AmbariException e) {
      // expected
      assertEquals("Cycle detected while parsing stack definition", e.getMessage());
    }

    try {
      new StackManager(new File("./src/test/resources/stacks_with_cycle2/"),
          new StackContext(null, actionMetadata, osFamily));
      fail("Expected exception due to cyclic stack");
    } catch (AmbariException e) {
      // expected
      assertEquals("Cycle detected while parsing stack definition", e.getMessage());
    }
  }

  @Test
  public void testExcludedConfigTypes() {
    StackInfo stack = stackManager.getStack("HDP", "2.0.8");
    ServiceInfo service = stack.getService("HBASE");
    assertFalse(service.hasConfigType("global"));
    Map<String, Map<String, Map<String, String>>> configTypes = service.getConfigTypeAttributes();
    assertEquals(2, configTypes.size());
    assertTrue(configTypes.containsKey("hbase-site"));
    assertTrue(configTypes.containsKey("hbase-policy"));

    // test version that inherits the service via version inheritance
    stack = stackManager.getStack("HDP", "2.1.1");
    service = stack.getService("HBASE");
    assertFalse(service.hasConfigType("global"));
    configTypes = service.getConfigTypeAttributes();
    assertEquals(2, configTypes.size());
    assertTrue(configTypes.containsKey("hbase-site"));
    assertTrue(configTypes.containsKey("hbase-policy"));
    assertFalse(configTypes.containsKey("global"));

    // test version that inherits the service explicit service extension
    // the new version also excludes hbase-policy
    stack = stackManager.getStack("OTHER", "2.0");
    service = stack.getService("HBASE");
    assertFalse(service.hasConfigType("global"));
    configTypes = service.getConfigTypeAttributes();
    assertEquals(1, configTypes.size());
    assertTrue(configTypes.containsKey("hbase-site"));
  }

  @Test
  public void testHDFSServiceContainsMetricsFile() throws Exception {
    StackInfo stack = stackManager.getStack("HDP", "2.0.6");
    ServiceInfo hdfsService = stack.getService("HDFS");

    assertEquals("HDFS", hdfsService.getName());
    assertNotNull(hdfsService.getMetricsFile());
  }

  /**
   * This test ensures the service status check is added into the action metadata when
   * the stack has no parent and is the only stack in the stack family
   */
  @Test
  public void testGetServiceInfoFromSingleStack() throws Exception {
    dao = createNiceMock(MetainfoDAO.class);
    actionMetadata = createNiceMock(ActionMetadata.class);
    osFamily = createNiceMock(OsFamily.class);

    // ensure that service check is added for HDFS
    actionMetadata.addServiceCheckAction("HDFS");
    replay(dao, actionMetadata, osFamily);
    StackManager stackManager = new StackManager(
        new File("./src/test/resources/single_stack".replace("/", File.separator)),
        new StackContext(dao, actionMetadata, osFamily));

    Collection<StackInfo> stacks = stackManager.getStacks();
    assertEquals(1, stacks.size());
    assertNotNull(stacks.iterator().next().getService("HDFS"));

    verify(dao, actionMetadata, osFamily);
  }

  //todo: component override assertions
}
