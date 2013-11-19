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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;

import junit.framework.Assert;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.StackAccessException;
import org.apache.ambari.server.api.util.StackExtensionHelper;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.OperatingSystemInfo;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.Stack;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.stack.MetricDefinition;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AmbariMetaInfoTest {

  private static final String STACK_NAME_HDP = "HDP";
  private static final String STACK_VERSION_HDP = "0.1";
  private static final String EXT_STACK_NAME = "2.0.6";
  private static final String STACK_VERSION_HDP_02 = "0.2";
  private static final String STACK_MINIMAL_VERSION_HDP = "0.0";
  private static String SERVICE_NAME_HDFS = "HDFS";
  private static String SERVICE_NAME_MAPRED2 = "MAPREDUCE2";
  private static String SERVICE_COMPONENT_NAME = "NAMENODE";
  private static final String OS_TYPE = "centos5";
  private static final String REPO_ID = "HDP-UTILS-1.1.0.15";
  private static final String PROPERTY_NAME = "hbase.regionserver.msginterval";
  
  private static final String NON_EXT_VALUE = "XXX";
  
  private static final int REPOS_CNT = 3;
  private static final int STACKS_NAMES_CNT = 1;
  private static final int PROPERTIES_CNT = 63;
  private static final int OS_CNT = 3;

  private AmbariMetaInfo metaInfo = null;
  private final static Logger LOG =
      LoggerFactory.getLogger(AmbariMetaInfoTest.class);
  private static final String FILE_NAME = "hbase-site.xml";
  

  @Rule
  public TemporaryFolder tmpFolder = new TemporaryFolder();

  @Before
  public void before() throws Exception {
    File stackRoot = new File("src/test/resources/stacks");
    LOG.info("Stacks file " + stackRoot.getAbsolutePath());
    metaInfo = new AmbariMetaInfo(stackRoot, new File("target/version"));
    try {
      metaInfo.init();
    } catch(Exception e) {
      LOG.info("Error in initializing ", e);
    }
  }

  @Test
  public void getComponentCategory() throws AmbariException {
    ComponentInfo componentInfo = metaInfo.getComponentCategory(STACK_NAME_HDP,
        STACK_VERSION_HDP, SERVICE_NAME_HDFS, SERVICE_COMPONENT_NAME);
    assertNotNull(componentInfo);
    componentInfo = metaInfo.getComponentCategory(STACK_NAME_HDP,
        STACK_VERSION_HDP, SERVICE_NAME_HDFS, "DATANODE1");
    Assert.assertNotNull(componentInfo);
    assertTrue(!componentInfo.isClient());
  }

  @Test
  public void getComponentsByService() throws AmbariException {
    List<ComponentInfo> components = metaInfo.getComponentsByService(
        STACK_NAME_HDP, STACK_VERSION_HDP, SERVICE_NAME_HDFS);
    assertNotNull(components);
    assertTrue(components.size() > 0);
  }

  @Test
  public void getRepository() throws AmbariException {
    Map<String, List<RepositoryInfo>> repository = metaInfo.getRepository(
        STACK_NAME_HDP, STACK_VERSION_HDP);
    assertNotNull(repository);
    assertFalse(repository.get("centos5").isEmpty());
    assertFalse(repository.get("centos6").isEmpty());
  }

  @Test
  public void isSupportedStack() throws AmbariException {
    boolean supportedStack = metaInfo.isSupportedStack(STACK_NAME_HDP,
        STACK_VERSION_HDP);
    assertTrue(supportedStack);
    
    boolean notSupportedStack = metaInfo.isSupportedStack(NON_EXT_VALUE,
        NON_EXT_VALUE);
    assertFalse(notSupportedStack);
  }

  @Test
  public void isValidService() throws AmbariException {
    boolean valid = metaInfo.isValidService(STACK_NAME_HDP, STACK_VERSION_HDP,
        SERVICE_NAME_HDFS);
    assertTrue(valid);

    boolean invalid = metaInfo.isValidService(STACK_NAME_HDP, STACK_VERSION_HDP, NON_EXT_VALUE);
    assertFalse(invalid);
  }

  /**
   * Method: getSupportedConfigs(String stackName, String version, String
   * serviceName)
   */
  @Test
  public void getSupportedConfigs() throws Exception {

    Map<String, Map<String, String>> configsAll = metaInfo.getSupportedConfigs(
        STACK_NAME_HDP, STACK_VERSION_HDP, SERVICE_NAME_HDFS);
    Set<String> filesKeys = configsAll.keySet();
    for (String file : filesKeys) {
      Map<String, String> configs = configsAll.get(file);
      Set<String> propertyKeys = configs.keySet();
      assertNotNull(propertyKeys);
      assertFalse(propertyKeys.size() == 0);
    }
  }

  @Test
  public void testServiceNameUsingComponentName() throws AmbariException {
    String serviceName = metaInfo.getComponentToService(STACK_NAME_HDP,
        STACK_VERSION_HDP, "NAMENODE");
    assertEquals("HDFS", serviceName);
  }

  /**
   * Method: Map<String, ServiceInfo> getServices(String stackName, String
   * version, String serviceName)
   * @throws AmbariException 
   */
  @Test
  public void getServices() throws AmbariException {
    Map<String, ServiceInfo> services = metaInfo.getServices(STACK_NAME_HDP,
        STACK_VERSION_HDP);
    LOG.info("Getting all the services ");
    for (Map.Entry<String, ServiceInfo> entry : services.entrySet()) {
      LOG.info("Service Name " + entry.getKey() + " values " + entry.getValue());
    }
    assertTrue(services.containsKey("HDFS"));
    assertTrue(services.containsKey("MAPREDUCE"));
    assertNotNull(services);
    assertFalse(services.keySet().size() == 0);
  }

  /**
   * Method: getServiceInfo(String stackName, String version, String
   * serviceName)
   */
  @Test
  public void getServiceInfo() throws Exception {
    ServiceInfo si = metaInfo.getServiceInfo(STACK_NAME_HDP, STACK_VERSION_HDP,
        SERVICE_NAME_HDFS);
    assertNotNull(si);
  }

  @Test
  public void testConfigDependencies() throws Exception {
    ServiceInfo serviceInfo = metaInfo.getServiceInfo(STACK_NAME_HDP, EXT_STACK_NAME,
      SERVICE_NAME_MAPRED2);
    assertNotNull(serviceInfo);
    assertTrue(!serviceInfo.getConfigDependencies().isEmpty());
  }

  /**
   * Method: getSupportedServices(String stackName, String version)
   */
  @Test
  public void getSupportedServices() throws Exception {
    List<ServiceInfo> services = metaInfo.getSupportedServices(STACK_NAME_HDP,
        STACK_VERSION_HDP);
    assertNotNull(services);
    assertFalse(services.size() == 0);

  }

  @Test
  public void testGetRepos() throws Exception {
    Map<String, List<RepositoryInfo>> repos = metaInfo.getRepository(
        STACK_NAME_HDP, STACK_VERSION_HDP);
    Set<String> centos5Cnt = new HashSet<String>();
    Set<String> centos6Cnt = new HashSet<String>();
    Set<String> redhat6cnt = new HashSet<String>();

    for (List<RepositoryInfo> vals : repos.values()) {
      for (RepositoryInfo repo : vals) {
        LOG.debug("Dumping repo info : " + repo.toString());
        if (repo.getOsType().equals("centos5")) {
          centos5Cnt.add(repo.getRepoId());
        } else if (repo.getOsType().equals("centos6")) {
          centos6Cnt.add(repo.getRepoId());
        } else if (repo.getOsType().equals("redhat6")) {
          redhat6cnt.add(repo.getRepoId());
        } else {
          fail("Found invalid os" + repo.getOsType());
        }

        if (repo.getRepoId().equals("epel")) {
          assertFalse(repo.getMirrorsList().isEmpty());
          assertNull(repo.getBaseUrl());
        } else {
          assertNull(repo.getMirrorsList());
          assertFalse(repo.getBaseUrl().isEmpty());
        }
      }
    }

    assertEquals(3, centos5Cnt.size());
    assertEquals(3, redhat6cnt.size());
    assertEquals(3, centos6Cnt.size());
  }
  
  
  @Test
  /**
   * Make sure global mapping is avaliable when global.xml is 
   * in the path.
   * @throws Exception
   */
  public void testGlobalMapping() throws Exception {
    ServiceInfo sinfo = metaInfo.getServiceInfo("HDP",
        "0.2", "HDFS");
    List<PropertyInfo> pinfo = sinfo.getProperties();
    /** check all the config knobs and make sure the global one is there **/
    boolean checkforglobal = false;
    
    for (PropertyInfo pinfol: pinfo) {
      if ("global.xml".equals(pinfol.getFilename())) {
        checkforglobal = true;
      }
    }
    Assert.assertTrue(checkforglobal);
    sinfo = metaInfo.getServiceInfo("HDP",
        "0.2", "MAPREDUCE");
    boolean checkforhadoopheapsize = false;
    pinfo = sinfo.getProperties();
    for (PropertyInfo pinfol: pinfo) {
      if ("global.xml".equals(pinfol.getFilename())) {
        if ("hadoop_heapsize".equals(pinfol.getName()))
          checkforhadoopheapsize = true;
      }
    }
    Assert.assertTrue(checkforhadoopheapsize);
  }
  
  @Test
  public void testMetaInfoFileFilter() throws Exception {
    String buildDir = tmpFolder.getRoot().getAbsolutePath();
    File stackRoot = new File("src/test/resources/stacks");
    File stackRootTmp = new File(buildDir + "/ambari-metaInfo"); stackRootTmp.mkdir();
    FileUtils.copyDirectory(stackRoot, stackRootTmp);
    AmbariMetaInfo ambariMetaInfo = new AmbariMetaInfo(stackRootTmp, new File("target/version"));
    File f1, f2, f3;
    f1 = new File(stackRootTmp.getAbsolutePath() + "/001.svn"); f1.createNewFile();
    f2 = new File(stackRootTmp.getAbsolutePath() + "/abcd.svn/001.svn"); f2.mkdirs(); f2.createNewFile();
    f3 = new File(stackRootTmp.getAbsolutePath() + "/.svn");
    if (!f3.exists()) {
      f3.createNewFile();
    }
    ambariMetaInfo.init();
    // Tests the stack is loaded as expected
    getServices();
    getComponentsByService();
    getComponentCategory();
    getSupportedConfigs();
    // Check .svn is not part of the stack but abcd.svn is
    Assert.assertNotNull(ambariMetaInfo.getStackInfo("abcd.svn", "001.svn"));
    
    Assert.assertFalse(ambariMetaInfo.isSupportedStack(".svn", ""));
    Assert.assertFalse(ambariMetaInfo.isSupportedStack(".svn", ""));
  }

  @Test
  public void testGetComponent() throws Exception {
    ComponentInfo component = metaInfo.getComponent(STACK_NAME_HDP,
        STACK_VERSION_HDP, SERVICE_NAME_HDFS, SERVICE_COMPONENT_NAME);
    Assert.assertEquals(component.getName(), SERVICE_COMPONENT_NAME);

    try {
      metaInfo.getComponent(STACK_NAME_HDP,
          STACK_VERSION_HDP, SERVICE_NAME_HDFS, NON_EXT_VALUE);
    } catch (StackAccessException e) {
      Assert.assertTrue(e instanceof StackAccessException);
    }
    
  }
  
  @Test
  public void testGetRepositories() throws Exception {
    List<RepositoryInfo> repositories = metaInfo.getRepositories(STACK_NAME_HDP, STACK_VERSION_HDP, OS_TYPE);
    Assert.assertEquals(repositories.size(), REPOS_CNT);
  }

  @Test
  public void testGetRepository() throws Exception {
    RepositoryInfo repository = metaInfo.getRepository(STACK_NAME_HDP, STACK_VERSION_HDP, OS_TYPE, REPO_ID);
    Assert.assertEquals(repository.getRepoId(), REPO_ID);

    try {
      metaInfo.getRepository(STACK_NAME_HDP, STACK_VERSION_HDP, OS_TYPE, NON_EXT_VALUE);
    } catch (StackAccessException e) {
      Assert.assertTrue(e instanceof StackAccessException);
    }
  }
  
  @Test
  public void testGetService() throws Exception {
    ServiceInfo service = metaInfo.getService(STACK_NAME_HDP, STACK_VERSION_HDP, SERVICE_NAME_HDFS);
    Assert.assertEquals(service.getName(), SERVICE_NAME_HDFS);
    try {
      metaInfo.getService(STACK_NAME_HDP, STACK_VERSION_HDP, NON_EXT_VALUE);
    } catch (StackAccessException e) {
      Assert.assertTrue(e instanceof StackAccessException);
    }
    
  }
  
  @Test
  public void testGetStacksNames() throws Exception {
    Set<Stack> stackNames = metaInfo.getStackNames();
    assertEquals(stackNames.size(), STACKS_NAMES_CNT);
    assertTrue(stackNames.contains(new Stack(STACK_NAME_HDP)));
  }
  
  @Test
  public void testGetStack() throws Exception {
    Stack stack = metaInfo.getStack(STACK_NAME_HDP);
    Assert.assertEquals(stack.getStackName(), STACK_NAME_HDP);
    try {
      metaInfo.getStack(NON_EXT_VALUE);
    } catch (StackAccessException e) {
      Assert.assertTrue(e instanceof StackAccessException);
    }
  }

  @Test
  public void testGetStackInfo() throws Exception {
    StackInfo stackInfo = metaInfo.getStackInfo(STACK_NAME_HDP, STACK_VERSION_HDP);
    Assert.assertEquals(stackInfo.getName(), STACK_NAME_HDP);
    Assert.assertEquals(stackInfo.getVersion(), STACK_VERSION_HDP);
    Assert.assertEquals(stackInfo.getMinUpgradeVersion(), STACK_MINIMAL_VERSION_HDP);
    try {
      metaInfo.getStackInfo(STACK_NAME_HDP, NON_EXT_VALUE);
    } catch (StackAccessException e) {
      Assert.assertTrue(e instanceof StackAccessException);
    }
  }

  @Test
  public void testGetProperties() throws Exception {
    Set<PropertyInfo> properties = metaInfo.getProperties(STACK_NAME_HDP, STACK_VERSION_HDP, SERVICE_NAME_HDFS);
    Assert.assertEquals(properties.size(), PROPERTIES_CNT);
  }
  
  @Test
  public void testGetProperty() throws Exception {
    PropertyInfo property = metaInfo.getProperty(STACK_NAME_HDP, STACK_VERSION_HDP, SERVICE_NAME_HDFS, PROPERTY_NAME);
    Assert.assertEquals(PROPERTY_NAME, property.getName());
    Assert.assertEquals(FILE_NAME, property.getFilename());

    try {
      metaInfo.getProperty(STACK_NAME_HDP, STACK_VERSION_HDP, SERVICE_NAME_HDFS, NON_EXT_VALUE);
    } catch (StackAccessException e) {
      Assert.assertTrue(e instanceof StackAccessException);
    }
    
  }
  
  @Test
  public void testGetOperatingSystems() throws Exception {
    Set<OperatingSystemInfo> operatingSystems = metaInfo.getOperatingSystems(STACK_NAME_HDP, STACK_VERSION_HDP);
    Assert.assertEquals(OS_CNT, operatingSystems.size());
  }
  
  @Test
  public void testGetOperatingSystem() throws Exception {
    OperatingSystemInfo operatingSystem = metaInfo.getOperatingSystem(STACK_NAME_HDP, STACK_VERSION_HDP, OS_TYPE);
    Assert.assertEquals(operatingSystem.getOsType(), OS_TYPE);
    
    
    try {
      metaInfo.getOperatingSystem(STACK_NAME_HDP, STACK_VERSION_HDP, NON_EXT_VALUE);
    } catch (StackAccessException e) {
      Assert.assertTrue(e instanceof StackAccessException);
    }
  }

  @Test
  public void isOsSupported() throws Exception {
    Assert.assertTrue(metaInfo.isOsSupported("redhat5"));
    Assert.assertTrue(metaInfo.isOsSupported("centos5"));
    Assert.assertTrue(metaInfo.isOsSupported("oraclelinux5"));
    Assert.assertTrue(metaInfo.isOsSupported("redhat6"));
    Assert.assertTrue(metaInfo.isOsSupported("centos6"));
    Assert.assertTrue(metaInfo.isOsSupported("oraclelinux6"));
    Assert.assertTrue(metaInfo.isOsSupported("suse11"));
    Assert.assertTrue(metaInfo.isOsSupported("sles11"));
    Assert.assertTrue(metaInfo.isOsSupported("ubuntu12"));
    Assert.assertFalse(metaInfo.isOsSupported("windows"));
  }

  @Test
  public void testExtendedStackDefinition() throws Exception {
    StackInfo stackInfo = metaInfo.getStackInfo(STACK_NAME_HDP, EXT_STACK_NAME);
    Assert.assertTrue(stackInfo != null);
    List<ServiceInfo> serviceInfos = stackInfo.getServices();
    Assert.assertFalse(serviceInfos.isEmpty());
    Assert.assertTrue(serviceInfos.size() > 1);
    ServiceInfo deletedService = null;
    ServiceInfo redefinedService = null;
    for (ServiceInfo serviceInfo : serviceInfos) {
      if (serviceInfo.getName().equals("SQOOP")) {
        deletedService = serviceInfo;
      }
      if (serviceInfo.getName().equals("YARN")) {
        redefinedService = serviceInfo;
      }
    }
    Assert.assertNull("SQOOP is a deleted service, should not be a part of " +
      "the extended stack.", deletedService);
    Assert.assertNotNull(redefinedService);
    // Components
    Assert.assertEquals("YARN service is expected to be defined with 3 active" +
      " components.", 3, redefinedService.getComponents().size());
    Assert.assertEquals("TEZ is expected to be a part of extended stack " +
      "definition", "TEZ", redefinedService.getClientComponent().getName());
    Assert.assertFalse("YARN CLIENT is a deleted component.",
      redefinedService.getClientComponent().getName().equals("YARN_CLIENT"));
    // Properties
    Assert.assertNotNull(redefinedService.getProperties());
    Assert.assertTrue(redefinedService.getProperties().size() > 4);
    PropertyInfo deleteProperty1 = null;
    PropertyInfo deleteProperty2 = null;
    PropertyInfo redefinedProperty1 = null;
    PropertyInfo redefinedProperty2 = null;
    PropertyInfo inheritedProperty = null;
    PropertyInfo newProperty = null;
    PropertyInfo originalProperty = null;

    for (PropertyInfo propertyInfo : redefinedService.getProperties()) {
      if (propertyInfo.getName().equals("yarn.resourcemanager" +
        ".resource-tracker.address")) {
        deleteProperty1 = propertyInfo;
      } else if (propertyInfo.getName().equals("yarn.resourcemanager" +
        ".scheduler.address")) {
        deleteProperty2 = propertyInfo;
      } else if (propertyInfo.getName().equals("yarn.resourcemanager" +
        ".address")) {
        redefinedProperty1 = propertyInfo;
      } else if (propertyInfo.getName().equals("yarn.resourcemanager.admin" +
        ".address")) {
        redefinedProperty2 = propertyInfo;
      } else if (propertyInfo.getName().equals("yarn.nodemanager.address")) {
        inheritedProperty = propertyInfo;
      } else if (propertyInfo.getName().equals("new-yarn-property")) {
        newProperty = propertyInfo;
      } else if (propertyInfo.getName().equals("yarn.nodemanager.aux-services")) {
        originalProperty = propertyInfo;
      }
    }

    Assert.assertNull(deleteProperty1);
    Assert.assertNull(deleteProperty2);
    Assert.assertNotNull(redefinedProperty1);
    Assert.assertNotNull(redefinedProperty2);
    Assert.assertNotNull("yarn.nodemanager.address expected to be inherited " +
      "from parent", inheritedProperty);
    Assert.assertEquals("localhost:100009", redefinedProperty1.getValue());
    // Parent property value will result in property being present in the
    // child stack
    Assert.assertEquals("localhost:8141", redefinedProperty2.getValue());
    // New property
    Assert.assertNotNull(newProperty);
    Assert.assertEquals("some-value", newProperty.getValue());
    Assert.assertEquals("some description.", newProperty.getDescription());
    Assert.assertEquals("yarn-site.xml", newProperty.getFilename());
    // Original property
    Assert.assertNotNull(originalProperty);
    Assert.assertEquals("mapreduce.shuffle", originalProperty.getValue());
    Assert.assertEquals("Auxilliary services of NodeManager",
      originalProperty.getDescription());
    Assert.assertEquals(3, redefinedService.getConfigDependencies().size());
  }

  @Test
  public void testGetParentStacksInOrder() throws Exception {
    List<StackInfo> allStacks = metaInfo.getSupportedStacks();
    StackInfo stackInfo = metaInfo.getStackInfo(STACK_NAME_HDP, EXT_STACK_NAME);
    StackInfo newStack = new StackInfo();
    newStack.setName(STACK_NAME_HDP);
    newStack.setVersion("2.0.99");
    newStack.setParentStackVersion(EXT_STACK_NAME);
    newStack.setActive(true);
    newStack.setRepositories(stackInfo.getRepositories());
    allStacks.add(newStack);

    Method method = StackExtensionHelper.class.getDeclaredMethod
      ("getParentStacksInOrder", Collection.class);
    method.setAccessible(true);
    StackExtensionHelper helper = new StackExtensionHelper(metaInfo.getStackRoot());
    Map<String, List<StackInfo>> stacks = (Map<String, List<StackInfo>>)
      method.invoke(helper, allStacks);

    Assert.assertNotNull(stacks.get("2.0.99"));
    // Verify order
    LinkedList<String> target = new LinkedList<String>();
    target.add("2.0.5");
    target.add("2.0.6");
    target.add("2.0.99");
    LinkedList<String> actual = new LinkedList<String>();
    LinkedList<StackInfo> parents = (LinkedList<StackInfo>) stacks.get("2.0.99");
    parents.addFirst(newStack);
    ListIterator lt = parents.listIterator(parents.size());
    while (lt.hasPrevious()) {
      StackInfo stack = (StackInfo) lt.previous();
      actual.add(stack.getVersion());
    }
    org.junit.Assert.assertArrayEquals("Order of stack extension not " +
      "preserved.", target.toArray(), actual.toArray());
  }

  @Test
  public void testGetApplicableServices() throws Exception {
    StackExtensionHelper helper = new StackExtensionHelper(
      metaInfo.getStackRoot());
    List<ServiceInfo> allServices = helper.getAllApplicableServices(metaInfo
      .getStackInfo(STACK_NAME_HDP, EXT_STACK_NAME));

    ServiceInfo testService = null;
    ServiceInfo existingService = null;
    for (ServiceInfo serviceInfo : allServices) {
      if (serviceInfo.getName().equals("YARN")) {
        testService = serviceInfo;
      } else if (serviceInfo.getName().equals("MAPREDUCE2")) {
        existingService = serviceInfo;
      }
    }

    Assert.assertNotNull(testService);
    Assert.assertNotNull(existingService);

    PropertyInfo testProperty = null;
    PropertyInfo existingProperty = null;
    for (PropertyInfo property : testService.getProperties()) {
      if (property.getName().equals("new-yarn-property")) {
        testProperty = property;
      }
    }
    for (PropertyInfo property : existingService.getProperties()) {
      if (property.getName().equals("mapreduce.map.log.level")) {
        existingProperty = property;
      }
    }

    Assert.assertNotNull(testProperty);
    Assert.assertEquals("some-value", testProperty.getValue());
    Assert.assertNotNull(existingProperty);
    Assert.assertEquals("INFO", existingProperty.getValue());
  }

  @Test
  public void testPropertyCount() throws Exception {
    Set<PropertyInfo> properties = metaInfo.getProperties(STACK_NAME_HDP, STACK_VERSION_HDP_02, SERVICE_NAME_HDFS);
    Assert.assertEquals(81, properties.size());
  }
  
  @Test
  public void testBadStack() throws Exception {
    File stackRoot = new File("src/test/resources/bad-stacks");
    LOG.info("Stacks file " + stackRoot.getAbsolutePath());
    AmbariMetaInfo mi = new AmbariMetaInfo(stackRoot, new File("target/version"));
    try {
      mi.init();
    } catch(Exception e) {
      assertTrue(JAXBException.class.isInstance(e));
    }
  }
  
  @Test
  public void testMetricsJson() throws Exception {
    ServiceInfo svc = metaInfo.getService(STACK_NAME_HDP, "2.0.5", "HDFS");
    Assert.assertNotNull(svc);
    Assert.assertNotNull(svc.getMetricsFile());
    
    svc = metaInfo.getService(STACK_NAME_HDP, "2.0.6", "HDFS");
    Assert.assertNotNull(svc);
    Assert.assertNotNull(svc.getMetricsFile());
    
    List<MetricDefinition> list = metaInfo.getMetrics(STACK_NAME_HDP, "2.0.5", "HDFS", "NAMENODE", "Component");
    Assert.assertNotNull(list);
    
    list = metaInfo.getMetrics(STACK_NAME_HDP, "2.0.5", "HDFS", "DATANODE", "Component");
    Assert.assertNull(list);
    
    List<MetricDefinition> list0 = metaInfo.getMetrics(STACK_NAME_HDP, "2.0.5", "HDFS", "DATANODE", "Component");
    Assert.assertNull(list0);
    Assert.assertTrue("Expecting subsequent calls to use a cached value for the definition", list == list0);
    

    // not explicitly defined, uses 2.0.5
    list = metaInfo.getMetrics(STACK_NAME_HDP, "2.0.6", "HDFS", "DATANODE", "Component");
    Assert.assertNull(list);
  }
}
