/*
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

import static java.util.stream.Collectors.toCollection;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementHelper;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.orm.dao.ExtensionDAO;
import org.apache.ambari.server.orm.dao.ExtensionLinkDAO;
import org.apache.ambari.server.orm.dao.MetainfoDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.ExtensionEntity;
import org.apache.ambari.server.orm.entities.ExtensionLinkEntity;
import org.apache.ambari.server.state.ClientConfigFileDefinition;
import org.apache.ambari.server.state.CommandScriptDefinition;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.OsSpecific;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.stack.MetricDefinition;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.commons.lang.StringUtils;
import org.easymock.EasyMock;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.util.Assert;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * StackManager unit tests.
 */
public class StackManagerTest {

  private static StackManager stackManager;
  private static MetainfoDAO metaInfoDao;
  private static ActionMetadata actionMetadata;
  private static OsFamily osFamily;
  private static StackDAO stackDao;
  private static ExtensionDAO extensionDao;
  private static ExtensionLinkDAO linkDao;

  @BeforeClass
  public static void initStack() throws Exception{
    stackManager = createTestStackManager();
  }

  public static StackManager createTestStackManager() throws Exception {
    String stacksDir = ClassLoader.getSystemClassLoader().getResource("stacks").getPath();
    File stacksRoot = new File(stacksDir);
    File resourcesRoot = stacksRoot.getParentFile();
    return createTestStackManager(resourcesRoot, stacksRoot);
  }

  public static StackManager createTestStackManager(File resourcesRoot, File stacksRoot) throws Exception {
    // todo: dao , actionMetaData expectations
    metaInfoDao = createNiceMock(MetainfoDAO.class);
    stackDao = createNiceMock(StackDAO.class);
    extensionDao = createNiceMock(ExtensionDAO.class);
    linkDao = createNiceMock(ExtensionLinkDAO.class);
    actionMetadata = createNiceMock(ActionMetadata.class);
    Configuration config = createNiceMock(Configuration.class);
    ExtensionEntity extensionEntity = createNiceMock(ExtensionEntity.class);

    expect(config.getSharedResourcesDirPath()).andReturn(
        ClassLoader.getSystemClassLoader().getResource("").getPath()).anyTimes();

    expect(
        extensionDao.find(EasyMock.anyObject(String.class),
            EasyMock.anyObject(String.class))).andReturn(extensionEntity).atLeastOnce();

    List<ExtensionLinkEntity> list = Collections.emptyList();
    expect(
        linkDao.findByStack(EasyMock.anyObject(String.class),
            EasyMock.anyObject(String.class))).andReturn(list).atLeastOnce();

    replay(config, metaInfoDao, stackDao, extensionDao, linkDao, actionMetadata);

    osFamily = new OsFamily(config);
    AmbariManagementHelper helper = new AmbariManagementHelper(stackDao, extensionDao, linkDao);

    StackManager stackManager = new StackManager(resourcesRoot, stacksRoot, null, null, osFamily, false, false,
        metaInfoDao, actionMetadata, stackDao, extensionDao, linkDao, helper);

    verify(config, metaInfoDao, stackDao, actionMetadata);

    return stackManager;
  }

  @Test
  public void testGetsStacks() throws Exception {
    Collection<StackInfo> stacks = stackManager.getStacks();
    assertEquals(21, stacks.size());
  }

  @Test
  public void testGetStacksByName() {
    Collection<StackInfo> stacks = stackManager.getStacks("HDP");
    assertEquals(17, stacks.size());

    stacks = stackManager.getStacks("OTHER");
    assertEquals(2, stacks.size());
  }

  @Test
  public void testHCFSServiceType() {

    StackInfo stack = stackManager.getStack("HDP", "2.2.0.ECS");
    ServiceInfo service = stack.getService("ECS");
    assertEquals(service.getServiceType(),"HCFS");

    service = stack.getService("HDFS");
    assertNull(service);
  }

  @Test
  public void testServiceRemoved() {
    StackInfo stack = stackManager.getStack("HDP", "2.0.8");
    ServiceInfo service = stack.getService("SPARK");
    assertNull(service);
    service = stack.getService("SPARK2");
    assertNull(service);
    List<String> removedServices = stack.getRemovedServices();
    assertEquals(removedServices.size(), 2);

    HashSet<String> expectedServices = new HashSet<>();
    expectedServices.add("SPARK");
    expectedServices.add("SPARK2");

    for (String s : removedServices) {
      assertTrue(expectedServices.remove(s));
    }
    assertTrue(expectedServices.isEmpty());

  }

  @Test
  public void testSerivcesWithNoConfigs(){
    StackInfo stack = stackManager.getStack("HDP", "2.0.8");
    List<String> servicesWithNoConfigs = stack.getServicesWithNoConfigs();
    //Via inheritance, Hive should have config types
    //Via inheritance, SystemML should still have no config types
    assertTrue((servicesWithNoConfigs.contains("SYSTEMML")));
    assertFalse((servicesWithNoConfigs.contains("HIVE")));

    stack = stackManager.getStack("HDP", "2.0.7");
    //Directly from the stack, SystemML should have no config types
    servicesWithNoConfigs = stack.getServicesWithNoConfigs();
    assertTrue((servicesWithNoConfigs.contains("SYSTEMML")));
  }

  @Test
  public void testGetStack() {
    StackInfo stack = stackManager.getStack("HDP", "0.1");
    assertNotNull(stack);
    assertEquals("HDP", stack.getName());
    assertEquals("0.1", stack.getVersion());


    Collection<ServiceInfo> services = stack.getServices();
    assertEquals(3, services.size());

    Map<String, ServiceInfo> serviceMap = new HashMap<>();
    for (ServiceInfo service : services) {
      serviceMap.put(service.getName(), service);
    }
    ServiceInfo hdfsService = serviceMap.get("HDFS");
    assertNotNull(hdfsService);
    List<ComponentInfo> components = hdfsService.getComponents();
    assertEquals(6, components.size());
    List<PropertyInfo> properties = hdfsService.getProperties();
    assertEquals(64, properties.size());

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
    assertEquals("This is comment for PIG service", pigService.getComment());
    components = pigService.getComponents();
    assertEquals(2, components.size());
    CommandScriptDefinition commandScript = pigService.getCommandScript();
    assertEquals("scripts/service_check.py", commandScript.getScript());
    assertEquals(CommandScriptDefinition.Type.PYTHON, commandScript.getScriptType());
    assertEquals(300, commandScript.getTimeout());
    assertEquals("SOME_CLIENT_FOR_SERVICE_CHECK", commandScript.getClientComponentType());
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

    Map<String, OsSpecific> osInfoMapService = pigService.getOsSpecifics();
    assertEquals(1, osInfoMapService.size());
    OsSpecific osSpecific = osInfoMapService.get("centos6");
    assertNotNull(osSpecific);
    assertEquals("centos6", osSpecific.getOsFamily());
    assertNull(osSpecific.getRepo());
    List<OsSpecific.Package> packages = osSpecific.getPackages();
    assertEquals(1, packages.size());
    OsSpecific.Package pkg = packages.get(0);
    assertEquals("pig", pkg.getName());

    Map<String, OsSpecific> osInfoMapStack = stack.getOsSpecifics();
    assertEquals(1, osInfoMapStack.size());
    osSpecific = osInfoMapStack.get("suse11");
    assertNotNull(osSpecific);
    assertEquals("suse11", osSpecific.getOsFamily());
    assertNull(osSpecific.getRepo());
    packages = osSpecific.getPackages();
    assertEquals(1, packages.size());
    pkg = packages.get(0);
    assertEquals("stack_pig", pkg.getName());

    assertNull(pigService.getParent());
  }

  @Test
  public void testStackVersionInheritance() {
    StackInfo stack = stackManager.getStack("HDP", "2.1.1");
    assertNotNull(stack);
    assertEquals("HDP", stack.getName());
    assertEquals("2.1.1", stack.getVersion());
    Collection<ServiceInfo> services = stack.getServices();

    ServiceInfo si = stack.getService("SPARK");
    assertNull(si);

    si = stack.getService("SPARK2");
    assertNull(si);

    si = stack.getService("SPARK3");
    assertNotNull(si);

    //should include all stacks in hierarchy
    assertEquals(19, services.size());

    Set<String> expectedServices = new TreeSet<>();
    expectedServices.add("GANGLIA");
    expectedServices.add("HADOOP_CLIENTS");
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
    expectedServices.add("TEZ");
    expectedServices.add("AMBARI_METRICS");
    expectedServices.add("SPARK3");
    expectedServices.add("SYSTEMML");

    assertEquals(expectedServices, services.stream().map(ServiceInfo::getName).collect(toCollection(TreeSet::new)));
    ServiceInfo pigService = null;
    for (ServiceInfo service : services) {
      if (service.getName().equals("PIG")) {
        pigService = service;
      }
      assertTrue(service.getName(), expectedServices.remove(service.getName()));
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
  public void testStackServiceExtension() {
    StackInfo stack = stackManager.getStack("OTHER", "1.0");
    assertNotNull(stack);
    assertEquals("OTHER", stack.getName());
    assertEquals("1.0", stack.getVersion());
    Collection<ServiceInfo> services = stack.getServices();

    assertEquals(4, services.size());

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
    String packageDir = StringUtils.join(
        new String[]{"stacks", "OTHER", "1.0", "services", "STORM", "package"}, File.separator);
    assertEquals(packageDir, stormService.getServicePackageFolder());

    // compare components
    List<ComponentInfo> stormServiceComponents = stormService.getComponents();
    List<ComponentInfo> baseStormServiceComponents = baseStormService.getComponents();
    assertEquals(new HashSet<>(stormServiceComponents), new HashSet<>(baseStormServiceComponents));
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
    assertEquals(baseStack.getOsSpecifics(), stack.getOsSpecifics());
    assertEquals(baseStormService.getRequiredServices(), stormService.getRequiredServices());
    assertEquals(baseStormService.getSchemaVersion(), stormService.getSchemaVersion());
  }

  @Test
  public void testGetStackServiceInheritance() {
    StackInfo baseStack = stackManager.getStack("OTHER", "1.0");
    StackInfo stack = stackManager.getStack("OTHER", "2.0");

    assertEquals(5, stack.getServices().size());

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
  public void testPackageInheritance() throws Exception{
    StackInfo stack = stackManager.getStack("HDP", "2.0.7");
    assertNotNull(stack.getService("HBASE"));
    ServiceInfo hbase = stack.getService("HBASE");
    assertNotNull("Package dir is " + hbase.getServicePackageFolder(), hbase.getServicePackageFolder());

    stack = stackManager.getStack("HDP", "2.0.8");
    assertNotNull(stack.getService("HBASE"));
    hbase = stack.getService("HBASE");
    assertNotNull("Package dir is " + hbase.getServicePackageFolder(), hbase.getServicePackageFolder());
  }

  @Test
  public void testMonitoringServicePropertyInheritance() throws Exception{
    StackInfo stack = stackManager.getStack("HDP", "2.0.8");
    Collection<ServiceInfo> allServices = stack.getServices();
    assertEquals(16, allServices.size());

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

    assertEquals(13, allServices.size());
    HashSet<String> expectedServices = new HashSet<>();
    expectedServices.add("GANGLIA");
    expectedServices.add("HADOOP_CLIENTS");
    expectedServices.add("HBASE");
    expectedServices.add("HCATALOG");
    expectedServices.add("HDFS");
    expectedServices.add("HIVE");
    expectedServices.add("MAPREDUCE2");
    expectedServices.add("OOZIE");
    expectedServices.add("PIG");
    expectedServices.add("SPARK");
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

    stack = stackManager.getStack("HDP", "2.0.6.1");
    yarnService = stack.getService("YARN");
    assertNull(yarnService.getComponentByName("YARN_CLIENT"));

    stack = stackManager.getStack("HDP", "2.0.7");
    yarnService = stack.getService("YARN");
    assertNotNull(yarnService.getComponentByName("YARN_CLIENT"));
  }


  @Test
  public void testInheritanceAfterComponentDeletion() {
    StackInfo stack = stackManager.getStack("HDP", "2.0.6");
    ServiceInfo yarnService = stack.getService("HBASE");
    assertNull(yarnService.getComponentByName("HBASE_CLIENT"));

    stack = stackManager.getStack("HDP", "2.0.6.1");
    yarnService = stack.getService("HBASE");
    assertNull(yarnService.getComponentByName("HBASE_CLIENT"));

    stack = stackManager.getStack("HDP", "2.0.8");
    yarnService = stack.getService("HBASE");
    assertNotNull(yarnService.getComponentByName("HBASE_CLIENT"));
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
    assertFalse(service.hasConfigType("hbase-policy"));
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

  @Test
  public void testMergeRoleCommandOrder() throws Exception {
    StackInfo stack = stackManager.getStack("HDP", "2.1.1");
    // merged role command order with parent stacks
    Map<String, Object> roleCommandOrder = stack.getRoleCommandOrder().getContent();
    assertTrue(roleCommandOrder.containsKey("optional_glusterfs"));
    assertTrue(roleCommandOrder.containsKey("general_deps"));
    assertTrue(roleCommandOrder.containsKey("optional_no_glusterfs"));
    assertTrue(roleCommandOrder.containsKey("namenode_optional_ha"));
    assertTrue(roleCommandOrder.containsKey("resourcemanager_optional_ha"));
    Map<String, Object>  generalDeps = (Map<String, Object>) roleCommandOrder.get("general_deps");
    assertTrue(generalDeps.containsKey("HBASE_MASTER-START"));
    assertTrue(generalDeps.containsKey("HBASE_REGIONSERVER-START"));
    Map<String, Object>  optionalNoGlusterfs  = (Map<String, Object>) roleCommandOrder.get("optional_no_glusterfs");
    assertTrue(optionalNoGlusterfs.containsKey("SECONDARY_NAMENODE-START"));
    ArrayList<String> hbaseMasterStartValues = (ArrayList<String>) generalDeps.get("HBASE_MASTER-START");
    assertTrue(hbaseMasterStartValues.get(0).equals("ZOOKEEPER_SERVER-START"));

    ServiceInfo service = stack.getService("PIG");
    assertNotNull("PIG's roll command order is null", service.getRoleCommandOrder());

    assertTrue(optionalNoGlusterfs.containsKey("NAMENODE-STOP"));
    ArrayList<String> nameNodeStopValues = (ArrayList<String>) optionalNoGlusterfs.get("NAMENODE-STOP");
    assertTrue(nameNodeStopValues.contains("JOBTRACKER-STOP"));
    assertTrue(nameNodeStopValues.contains("CUSTOM_MASTER-STOP"));

    assertTrue(generalDeps.containsKey("CUSTOM_MASTER-START"));
    ArrayList<String> customMasterStartValues = (ArrayList<String>) generalDeps.get("CUSTOM_MASTER-START");
    assertTrue(customMasterStartValues.contains("ZOOKEEPER_SERVER-START"));
    assertTrue(customMasterStartValues.contains("NAMENODE-START"));

  }

  @Test
  public void testMetricsLoaded() throws Exception {

    URL rootDirectoryURL = StackManagerTest.class.getResource("/");
    Assert.notNull(rootDirectoryURL);

    File resourcesDirectory = new File(new File(rootDirectoryURL.getFile()).getParentFile().getParentFile(), "src/main/resources");

    File stackRoot = new File(resourcesDirectory, "stacks");
    File commonServices = new File(resourcesDirectory, "common-services");
    File extensions = null;

    try {
         URL extensionsURL = ClassLoader.getSystemClassLoader().getResource("extensions");
      if (extensionsURL != null) {
        extensions = new File(extensionsURL.getPath().replace("test-classes","classes"));
      }
    }
    catch (Exception e) {}

    MetainfoDAO metaInfoDao = createNiceMock(MetainfoDAO.class);
    StackDAO stackDao = createNiceMock(StackDAO.class);
    ExtensionDAO extensionDao = createNiceMock(ExtensionDAO.class);
    ExtensionLinkDAO linkDao = createNiceMock(ExtensionLinkDAO.class);
    ActionMetadata actionMetadata = createNiceMock(ActionMetadata.class);
    Configuration config = createNiceMock(Configuration.class);
    ExtensionEntity extensionEntity = createNiceMock(ExtensionEntity.class);

    expect(config.getSharedResourcesDirPath()).andReturn(
            ClassLoader.getSystemClassLoader().getResource("").getPath()).anyTimes();

    expect(
        extensionDao.find(EasyMock.anyObject(String.class),
            EasyMock.anyObject(String.class))).andReturn(extensionEntity).atLeastOnce();

    List<ExtensionLinkEntity> list = Collections.emptyList();
    expect(
        linkDao.findByStack(EasyMock.anyObject(String.class),
            EasyMock.anyObject(String.class))).andReturn(list).atLeastOnce();

    replay(config, metaInfoDao, stackDao, extensionDao, linkDao, actionMetadata);

    OsFamily osFamily = new OsFamily(config);
    AmbariManagementHelper helper = new AmbariManagementHelper(stackDao, extensionDao, linkDao);

    StackManager stackManager = new StackManager(resourcesDirectory, stackRoot, commonServices, extensions,
            osFamily, false, false, metaInfoDao, actionMetadata, stackDao, extensionDao, linkDao, helper);

    for (StackInfo stackInfo : stackManager.getStacks()) {
      for (ServiceInfo serviceInfo : stackInfo.getServices()) {
        Type type = new TypeToken<Map<String, Map<String, List<MetricDefinition>>>>() {
        }.getType();

        Gson gson = new Gson();
        Map<String, Map<String, List<MetricDefinition>>> map = null;
        if (serviceInfo.getMetricsFile() != null) {
          try {
            map = gson.fromJson(new FileReader(serviceInfo.getMetricsFile()), type);
          } catch (Exception e) {
            e.printStackTrace();
            throw new AmbariException("Failed to load metrics from file " + serviceInfo.getMetricsFile().getAbsolutePath());
          }
        }
      }
    }
  }

  @Test
  public void testServicesWithRangerPluginRoleCommandOrder() throws AmbariException {
    URL rootDirectoryURL = StackManagerTest.class.getResource("/");
    Assert.notNull(rootDirectoryURL);

    File resourcesDirectory = new File(new File(rootDirectoryURL.getFile()).getParentFile().getParentFile(), "src/main/resources");

    File stackRoot = new File(resourcesDirectory, "stacks");
    File commonServices = new File(resourcesDirectory, "common-services");
    File extensions = null;

    try {
      URL extensionsURL = ClassLoader.getSystemClassLoader().getResource("extensions");
      if (extensionsURL != null) {
        extensions = new File(extensionsURL.getPath().replace("test-classes","classes"));
      }
    }
    catch (Exception e) {}

    MetainfoDAO metaInfoDao = createNiceMock(MetainfoDAO.class);
    StackDAO stackDao = createNiceMock(StackDAO.class);
    ExtensionDAO extensionDao = createNiceMock(ExtensionDAO.class);
    ExtensionLinkDAO linkDao = createNiceMock(ExtensionLinkDAO.class);
    ActionMetadata actionMetadata = createNiceMock(ActionMetadata.class);
    Configuration config = createNiceMock(Configuration.class);
    ExtensionEntity extensionEntity = createNiceMock(ExtensionEntity.class);

    expect(config.getSharedResourcesDirPath()).andReturn(
      ClassLoader.getSystemClassLoader().getResource("").getPath()).anyTimes();

    expect(
        extensionDao.find(EasyMock.anyObject(String.class),
            EasyMock.anyObject(String.class))).andReturn(extensionEntity).atLeastOnce();

    List<ExtensionLinkEntity> list = Collections.emptyList();
    expect(
        linkDao.findByStack(EasyMock.anyObject(String.class),
            EasyMock.anyObject(String.class))).andReturn(list).atLeastOnce();

    replay(config, metaInfoDao, stackDao, extensionDao, linkDao, actionMetadata);

    OsFamily osFamily = new OsFamily(config);
    AmbariManagementHelper helper = new AmbariManagementHelper(stackDao, extensionDao, linkDao);

    StackManager stackManager = new StackManager(resourcesDirectory, stackRoot, commonServices, extensions, osFamily,
        false, false, metaInfoDao, actionMetadata, stackDao, extensionDao, linkDao, helper);

    String rangerUserSyncRoleCommand = Role.RANGER_USERSYNC + "-" + RoleCommand.START;
    String rangerAdminRoleCommand = Role.RANGER_ADMIN + "-" + RoleCommand.START;
    String zookeeperServerRoleCommand = Role.ZOOKEEPER_SERVER + "-" + RoleCommand.START;
    String nodeManagerRoleCommand = Role.NODEMANAGER + "-" + RoleCommand.START;
    String mySqlServerRoleCommand = Role.MYSQL_SERVER + "-" + RoleCommand.START;

    // When
    StackInfo hdp = stackManager.getStack("HDP", "2.3");
    Map<String, Object> rco = hdp.getRoleCommandOrder().getContent();

    // Then
    // verify that services that have ranger plugin are after ranger admin in the role command order sequence
    // as these services require ranger admin and ranger user sync to up upfront
    Map<String, Object> generalDeps = (Map<String, Object>)rco.get("general_deps");
    Map<String, Object> optionalNoGlusterfs = (Map<String, Object>)rco.get("optional_no_glusterfs");


    // HDFS
    String nameNodeRoleCommand  = Role.NAMENODE +  "-" + RoleCommand.START;
    ArrayList<String> nameNodeBlockers = (ArrayList<String>)optionalNoGlusterfs.get(nameNodeRoleCommand);

    assertTrue(nameNodeRoleCommand + " should be dependent of " + rangerUserSyncRoleCommand, nameNodeBlockers.contains(rangerUserSyncRoleCommand));

    String dataNodeRoleCommand = Role.DATANODE +  "-" + RoleCommand.START;
    ArrayList<String> dataNodeBlockers = (ArrayList<String>)optionalNoGlusterfs.get(dataNodeRoleCommand);

    assertTrue(dataNodeRoleCommand + " should be dependent of " + rangerUserSyncRoleCommand, dataNodeBlockers.contains(rangerUserSyncRoleCommand));

    // YARN
    String resourceManagerCommandRoleCommand = Role.RESOURCEMANAGER +  "-" + RoleCommand.START;
    ArrayList<String> resourceManagerBlockers = (ArrayList<String>)generalDeps.get(resourceManagerCommandRoleCommand);

    assertTrue(resourceManagerCommandRoleCommand + " should be dependent of " + rangerUserSyncRoleCommand, resourceManagerBlockers.contains(rangerUserSyncRoleCommand));


    // HBase
    String hbaseRoleCommand = Role.HBASE_MASTER +  "-" + RoleCommand.START;
    ArrayList<String> hbaseBlockers = (ArrayList<String>)generalDeps.get(hbaseRoleCommand);

    assertTrue(hbaseRoleCommand + " should be dependent of " + rangerUserSyncRoleCommand, hbaseBlockers.contains(rangerUserSyncRoleCommand));
    assertTrue(hbaseRoleCommand + " should be dependent of " + zookeeperServerRoleCommand, hbaseBlockers.contains(zookeeperServerRoleCommand));

    // Knox
    String knoxRoleCommand = Role.KNOX_GATEWAY +  "-" + RoleCommand.START;
    ArrayList<String> knoxBlockers = (ArrayList<String>)generalDeps.get(knoxRoleCommand);

    assertTrue(knoxRoleCommand + " should be dependent of " + rangerUserSyncRoleCommand, knoxBlockers.contains(rangerUserSyncRoleCommand));

    // Kafka
    String kafkaRoleCommand = Role.KAFKA_BROKER +  "-" + RoleCommand.START;
    ArrayList<String> kafkaBlockers = (ArrayList<String>)generalDeps.get(kafkaRoleCommand);

    assertTrue(Role.KAFKA_BROKER + "-" + RoleCommand.START + " should be dependent of " + rangerUserSyncRoleCommand, kafkaBlockers.contains(rangerUserSyncRoleCommand));

    // Hive
    String hiveRoleCommand = Role.HIVE_SERVER +  "-" + RoleCommand.START;
    ArrayList<String> hiveBlockers = (ArrayList<String>)generalDeps.get(hiveRoleCommand);

    assertTrue(hiveRoleCommand + " should be dependent of " + rangerUserSyncRoleCommand, hiveBlockers.contains(rangerUserSyncRoleCommand));
    assertTrue(hiveRoleCommand + " should be dependent of " + nodeManagerRoleCommand, hiveBlockers.contains(nodeManagerRoleCommand));
    assertTrue(hiveRoleCommand + " should be dependent of " + mySqlServerRoleCommand, hiveBlockers.contains(mySqlServerRoleCommand));

    // Storm
    String stormRoleCommand = Role.NIMBUS +  "-" + RoleCommand.START;
    ArrayList<String> stormBlockers = (ArrayList<String>)generalDeps.get(stormRoleCommand);

    assertTrue(stormRoleCommand + " should be dependent of " + rangerUserSyncRoleCommand, stormBlockers.contains(rangerUserSyncRoleCommand));
    assertTrue(stormRoleCommand + " should be dependent of " + zookeeperServerRoleCommand, stormBlockers.contains(zookeeperServerRoleCommand));

    // Ranger KMS
    String kmsRoleCommand = Role.RANGER_KMS_SERVER +  "-" + RoleCommand.START;
    ArrayList<String> rangerKmsBlockers = (ArrayList<String>)generalDeps.get(kmsRoleCommand);

    assertTrue(kmsRoleCommand + " should be dependent of " + rangerAdminRoleCommand, rangerKmsBlockers.contains(rangerAdminRoleCommand));
    assertTrue(kmsRoleCommand + " should be dependent of " + nameNodeRoleCommand, rangerKmsBlockers.contains(nameNodeRoleCommand));

    // Ranger User Sync
    ArrayList<String> rangerUserSyncBlockers = (ArrayList<String>)generalDeps.get(rangerUserSyncRoleCommand);

    assertTrue(rangerUserSyncRoleCommand + " should be dependent of " + rangerAdminRoleCommand, rangerUserSyncBlockers.contains(rangerAdminRoleCommand));
  }
  //todo: component override assertions

  @Test
  public void testServicesWithLogsearchRoleCommandOrder() throws AmbariException {
    URL rootDirectoryURL = StackManagerTest.class.getResource("/");
    Assert.notNull(rootDirectoryURL);

    File resourcesDirectory = new File(new File(rootDirectoryURL.getFile()).getParentFile().getParentFile(), "src/main/resources");

    File stackRoot = new File(resourcesDirectory, "stacks");
    File commonServices = new File(resourcesDirectory, "common-services");
    File extensions = null;

    try {
         URL extensionsURL = ClassLoader.getSystemClassLoader().getResource("extensions");
      if (extensionsURL != null) {
        extensions = new File(extensionsURL.getPath().replace("test-classes","classes"));
      }
    }
    catch (Exception e) {}

    MetainfoDAO metaInfoDao = createNiceMock(MetainfoDAO.class);
    StackDAO stackDao = createNiceMock(StackDAO.class);
    ExtensionDAO extensionDao = createNiceMock(ExtensionDAO.class);
    ExtensionLinkDAO linkDao = createNiceMock(ExtensionLinkDAO.class);
    ActionMetadata actionMetadata = createNiceMock(ActionMetadata.class);
    Configuration config = createNiceMock(Configuration.class);
    ExtensionEntity extensionEntity = createNiceMock(ExtensionEntity.class);

    expect(config.getSharedResourcesDirPath()).andReturn(
      ClassLoader.getSystemClassLoader().getResource("").getPath()).anyTimes();

    expect(
        extensionDao.find(EasyMock.anyObject(String.class),
            EasyMock.anyObject(String.class))).andReturn(extensionEntity).atLeastOnce();

    List<ExtensionLinkEntity> list = Collections.emptyList();
    expect(
        linkDao.findByStack(EasyMock.anyObject(String.class),
            EasyMock.anyObject(String.class))).andReturn(list).atLeastOnce();

    replay(config, metaInfoDao, stackDao, extensionDao, linkDao, actionMetadata);

    OsFamily osFamily = new OsFamily(config);
    AmbariManagementHelper helper = new AmbariManagementHelper(stackDao, extensionDao, linkDao);

    StackManager stackManager = new StackManager(resourcesDirectory, stackRoot, commonServices, extensions, osFamily,
        false, false, metaInfoDao, actionMetadata, stackDao, extensionDao, linkDao, helper);

    String zookeeperServerRoleCommand = Role.ZOOKEEPER_SERVER + "-" + RoleCommand.START;
    String logsearchServerRoleCommand = Role.LOGSEARCH_SERVER + "-" + RoleCommand.START;
    String infraSolrRoleCommand = Role.INFRA_SOLR + "-" + RoleCommand.START;
    String logsearchLogfeederRoleCommand = Role.LOGSEARCH_LOGFEEDER + "-" + RoleCommand.START;

    StackInfo hdp = stackManager.getStack("HDP", "2.3");
    Map<String, Object> rco = hdp.getRoleCommandOrder().getContent();

    Map<String, Object> generalDeps = (Map<String, Object>) rco.get("general_deps");
    Map<String, Object> optionalNoGlusterfs = (Map<String, Object>) rco.get("optional_no_glusterfs");

    // HDFS/YARN - verify that the stack level rco still works as expected
    String nameNodeRoleCommand = Role.NAMENODE + "-" + RoleCommand.START;
    String rangerUserSyncRoleCommand = Role.RANGER_USERSYNC + "-" + RoleCommand.START;
    ArrayList<String> nameNodeBlockers = (ArrayList<String>) optionalNoGlusterfs.get(nameNodeRoleCommand);

    assertTrue(nameNodeRoleCommand + " should be dependent of " + rangerUserSyncRoleCommand, nameNodeBlockers.contains(rangerUserSyncRoleCommand));

    String resourceManagerCommandRoleCommand = Role.RESOURCEMANAGER +  "-" + RoleCommand.START;
    ArrayList<String> resourceManagerBlockers = (ArrayList<String>)generalDeps.get(resourceManagerCommandRoleCommand);

    assertTrue(resourceManagerCommandRoleCommand + " should be dependent of " + rangerUserSyncRoleCommand, resourceManagerBlockers.contains(rangerUserSyncRoleCommand));

    // verify logsearch rco
    // LogSearch Solr
    ArrayList<String> logsearchSolrBlockers = (ArrayList<String>) generalDeps.get(infraSolrRoleCommand);
    assertTrue(infraSolrRoleCommand + " should be dependent of " + zookeeperServerRoleCommand, logsearchSolrBlockers.contains(zookeeperServerRoleCommand));

    // LogSearch Server
    ArrayList<String> logsearchServerBlockers = (ArrayList<String>) generalDeps.get(logsearchServerRoleCommand);
    assertTrue(logsearchServerRoleCommand + " should be dependent of " + infraSolrRoleCommand, logsearchServerBlockers.contains(infraSolrRoleCommand));

    // LogSearch LogFeeder
    ArrayList<String> logsearchLogfeederBlockers = (ArrayList<String>) generalDeps.get(logsearchLogfeederRoleCommand);
    assertTrue(logsearchLogfeederRoleCommand + " should be dependent of " + infraSolrRoleCommand, logsearchLogfeederBlockers.contains(infraSolrRoleCommand));
    assertTrue(logsearchLogfeederRoleCommand + " should be dependent of " + logsearchServerRoleCommand, logsearchLogfeederBlockers.contains(logsearchServerRoleCommand));
  }

  @Test
  public void testVersionDefinitionStackRepoUpdateLinkExists(){
    // Get the base sqoop service
    StackInfo stack = stackManager.getStack("HDP", "2.1.1");
    String latestUri = stack.getRepositoryXml().getLatestURI();
    assertTrue(latestUri != null);

    stack = stackManager.getStack("HDP", "2.0.8");
    latestUri = stack.getRepositoryXml().getLatestURI();
    assertTrue(latestUri == null);
  }
}
