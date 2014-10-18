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

import javax.persistence.EntityManager;
import javax.xml.bind.JAXBException;

import junit.framework.Assert;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.StackAccessException;
import org.apache.ambari.server.api.util.StackExtensionHelper;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.state.AutoDeployInfo;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.CustomCommandDefinition;
import org.apache.ambari.server.state.DependencyInfo;
import org.apache.ambari.server.state.OperatingSystemInfo;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.Stack;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.alert.AlertDefinition;
import org.apache.ambari.server.state.stack.MetricDefinition;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class AmbariMetaInfoTest {

  private static final String STACK_NAME_HDP = "HDP";
  private static final String STACK_NAME_XYZ = "XYZ";
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
  private static final String SHARED_PROPERTY_NAME = "content";

  private static final String NON_EXT_VALUE = "XXX";

  private static final int REPOS_CNT = 3;
  private static final int STACKS_NAMES_CNT = 2;
  private static final int PROPERTIES_CNT = 62;
  private static final int OS_CNT = 4;

  private AmbariMetaInfo metaInfo = null;
  private final static Logger LOG =
      LoggerFactory.getLogger(AmbariMetaInfoTest.class);
  private static final String FILE_NAME = "hbase-site.xml";
  private static final String HADOOP_ENV_FILE_NAME = "hadoop-env.xml";
  private static final String HDFS_LOG4J_FILE_NAME = "hdfs-log4j.xml";

  private Injector injector;


  @Rule
  public TemporaryFolder tmpFolder = new TemporaryFolder();

  @Before
  public void before() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    injector.getInstance(EntityManager.class);
    File stackRoot = new File("src/test/resources/stacks");
    LOG.info("Stacks file " + stackRoot.getAbsolutePath());
    metaInfo = new AmbariMetaInfo(stackRoot, new File("target/version"));
    metaInfo.injector = injector;
    try {
      metaInfo.init();
    } catch(Exception e) {
      LOG.info("Error in initializing ", e);
    }
  }

  public class MockModule extends AbstractModule {
    @Override
    protected void configure() {
      bind(ActionMetadata.class);
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
  public void getRestartRequiredServicesNames() throws AmbariException {
    Set<String> res = metaInfo.getRestartRequiredServicesNames(STACK_NAME_HDP, "2.0.7");
    assertEquals(1, res.size());
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
  public void testGetRepositoryDefault() throws Exception {
    // Scenario: user has internet and does nothing to repos via api
    // use the latest
    String buildDir = tmpFolder.getRoot().getAbsolutePath();
    AmbariMetaInfo ambariMetaInfo = setupTempAmbariMetaInfo(buildDir);
    // The current stack already has (HDP, 2.1.1, redhat6) with valid latest
    // url
    ambariMetaInfo.init();

    List<RepositoryInfo> redhat6Repo = ambariMetaInfo.getRepositories(
        STACK_NAME_HDP, "2.1.1", "redhat6");
    assertNotNull(redhat6Repo);
    for (RepositoryInfo ri : redhat6Repo) {
      if (STACK_NAME_HDP.equals(ri.getRepoName())) {
        assertFalse(ri.getBaseUrl().equals(ri.getDefaultBaseUrl()));
        assertEquals(ri.getBaseUrl(), ri.getLatestBaseUrl());
      }
    }
  }

  @Test
  public void testGetRepositoryNoInternetDefault() throws Exception {
    // Scenario: user has no internet and does nothing to repos via api
    // use the default
    String buildDir = tmpFolder.getRoot().getAbsolutePath();
    AmbariMetaInfo ambariMetaInfo = setupTempAmbariMetaInfo(buildDir);
    // The current stack already has (HDP, 2.1.1, redhat6).

    // Deleting the json file referenced by the latestBaseUrl to simulate No
    // Internet.
    File latestUrlFile = new File(buildDir,
        "ambari-metaInfo/HDP/2.1.1/repos/hdp.json");
    FileUtils.deleteQuietly(latestUrlFile);
    assertTrue(!latestUrlFile.exists());
    ambariMetaInfo.init();

    List<RepositoryInfo> redhat6Repo = ambariMetaInfo.getRepositories(
        STACK_NAME_HDP, "2.1.1", "redhat6");
    assertNotNull(redhat6Repo);
    for (RepositoryInfo ri : redhat6Repo) {
      if (STACK_NAME_HDP.equals(ri.getRepoName())) {
        // baseUrl should be same as defaultBaseUrl since No Internet to load the
        // latestBaseUrl from the json file.
        assertEquals(ri.getBaseUrl(), ri.getDefaultBaseUrl());
      }
    }
  }

  @Test
  public void testGetRepositoryUpdatedBaseUrl() throws Exception {
    // Scenario: user has internet and but calls to set repos via api
    // use whatever they set
    String buildDir = tmpFolder.getRoot().getAbsolutePath();
    AmbariMetaInfo ambariMetaInfo = setupTempAmbariMetaInfo(buildDir);
    // The current stack already has (HDP, 2.1.1, redhat6)

    // Updating the baseUrl
    String newBaseUrl = "http://myprivate-repo-1.hortonworks.com/HDP/centos6/2.x/updates/2.0.6.0";
    ambariMetaInfo.updateRepoBaseURL(STACK_NAME_HDP, "2.1.1", "redhat6",
        STACK_NAME_HDP + "-2.1.1", newBaseUrl);
    RepositoryInfo repoInfo = ambariMetaInfo.getRepository(STACK_NAME_HDP, "2.1.1", "redhat6",
        STACK_NAME_HDP + "-2.1.1");
    assertEquals(newBaseUrl, repoInfo.getBaseUrl());
    String prevBaseUrl = repoInfo.getDefaultBaseUrl();
    ambariMetaInfo.init();

    List<RepositoryInfo> redhat6Repo = ambariMetaInfo.getRepositories(
        STACK_NAME_HDP, "2.1.1", "redhat6");
    assertNotNull(redhat6Repo);
    for (RepositoryInfo ri : redhat6Repo) {
      if (STACK_NAME_HDP.equals(ri.getRepoName())) {
        assertEquals(newBaseUrl, ri.getBaseUrl());
        // defaultBaseUrl and baseUrl should not be same, since it is updated.
        assertFalse(ri.getBaseUrl().equals(ri.getDefaultBaseUrl()));
      }
    }

    // Reset the database with the original baseUrl
    ambariMetaInfo.updateRepoBaseURL(STACK_NAME_HDP, "2.1.1", "redhat6",
        STACK_NAME_HDP + "-2.1.1", prevBaseUrl);
  }

  @Test
  public void testGetRepositoryNoInternetUpdatedBaseUrl() throws Exception {
    // Scenario: user has no internet and but calls to set repos via api
    // use whatever they set
    String buildDir = tmpFolder.getRoot().getAbsolutePath();
    AmbariMetaInfo ambariMetaInfo = setupTempAmbariMetaInfo(buildDir);
    // The current stack already has (HDP, 2.1.1, redhat6).

    // Deleting the json file referenced by the latestBaseUrl to simulate No
    // Internet.
    File latestUrlFile = new File(buildDir,
        "ambari-metaInfo/HDP/2.1.1/repos/hdp.json");
    FileUtils.deleteQuietly(latestUrlFile);
    assertTrue(!latestUrlFile.exists());

    // Update baseUrl
    String newBaseUrl = "http://myprivate-repo-1.hortonworks.com/HDP/centos6/2.x/updates/2.0.6.0";
    ambariMetaInfo.updateRepoBaseURL("HDP", "2.1.1", "redhat6", "HDP-2.1.1",
        newBaseUrl);
    RepositoryInfo repoInfo = ambariMetaInfo.getRepository(STACK_NAME_HDP, "2.1.1", "redhat6",
        STACK_NAME_HDP + "-2.1.1");
    assertEquals(newBaseUrl, repoInfo.getBaseUrl());
    String prevBaseUrl = repoInfo.getDefaultBaseUrl();
    ambariMetaInfo.init();

    List<RepositoryInfo> redhat6Repo = ambariMetaInfo.getRepositories(
        STACK_NAME_HDP, "2.1.1", "redhat6");
    assertNotNull(redhat6Repo);
    for (RepositoryInfo ri : redhat6Repo) {
      if (STACK_NAME_HDP.equals(ri.getRepoName())) {
        // baseUrl should point to the updated baseUrl
        assertEquals(newBaseUrl, ri.getBaseUrl());
        assertFalse(ri.getDefaultBaseUrl().equals(ri.getBaseUrl()));
      }
    }

    // Reset the database with the original baseUrl
    ambariMetaInfo.updateRepoBaseURL(STACK_NAME_HDP, "2.1.1", "redhat6",
        STACK_NAME_HDP + "-2.1.1", prevBaseUrl);
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
    Set<String> redhat5cnt = new HashSet<String>();

    for (List<RepositoryInfo> vals : repos.values()) {
      for (RepositoryInfo repo : vals) {
        LOG.debug("Dumping repo info : " + repo.toString());
        if (repo.getOsType().equals("centos5")) {
          centos5Cnt.add(repo.getRepoId());
        } else if (repo.getOsType().equals("centos6")) {
          centos6Cnt.add(repo.getRepoId());
        } else if (repo.getOsType().equals("redhat6")) {
          redhat6cnt.add(repo.getRepoId());
        } else if (repo.getOsType().equals("redhat5")) {
          redhat5cnt.add(repo.getRepoId());
        } else {
          fail("Found invalid os " + repo.getOsType());
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
    assertEquals(3, redhat5cnt.size());
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
        if ("hadoop_heapsize".equals(pinfol.getName())) {
          checkforhadoopheapsize = true;
        }
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
    ambariMetaInfo.injector = injector;
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
    assertTrue(stackNames.contains(new Stack(STACK_NAME_XYZ)));
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
  public void testGetStackParentVersions() throws Exception {
    List<String> parents = metaInfo.getStackParentVersions(STACK_NAME_HDP, "2.0.8");
    Assert.assertEquals(3, parents.size());
    Assert.assertEquals("2.0.7", parents.get(0));
    Assert.assertEquals("2.0.6", parents.get(1));
    Assert.assertEquals("2.0.5", parents.get(2));
  }

  @Test
  public void testGetProperties() throws Exception {
    Set<PropertyInfo> properties = metaInfo.getProperties(STACK_NAME_HDP, STACK_VERSION_HDP, SERVICE_NAME_HDFS);
    Assert.assertEquals(properties.size(), PROPERTIES_CNT);
  }

  @Test
  public void testGetPropertiesNoName() throws Exception {
    Set<PropertyInfo> properties = metaInfo.getPropertiesByName(STACK_NAME_HDP, STACK_VERSION_HDP, SERVICE_NAME_HDFS, PROPERTY_NAME);
    Assert.assertEquals(1, properties.size());
    for (PropertyInfo propertyInfo : properties) {
      Assert.assertEquals(PROPERTY_NAME, propertyInfo.getName());
      Assert.assertEquals(FILE_NAME, propertyInfo.getFilename());
    }

    try {
      metaInfo.getPropertiesByName(STACK_NAME_HDP, STACK_VERSION_HDP, SERVICE_NAME_HDFS, NON_EXT_VALUE);
    } catch (StackAccessException e) {
      Assert.assertTrue(e instanceof StackAccessException);
    }

  }

  @Test
  public void testGetPropertiesSharedName() throws Exception {
    Set<PropertyInfo> properties = metaInfo.getPropertiesByName(STACK_NAME_HDP, STACK_VERSION_HDP_02, SERVICE_NAME_HDFS, SHARED_PROPERTY_NAME);
    Assert.assertEquals(2, properties.size());
    for (PropertyInfo propertyInfo : properties) {
      Assert.assertEquals(SHARED_PROPERTY_NAME, propertyInfo.getName());
      Assert.assertTrue(propertyInfo.getFilename().equals(HADOOP_ENV_FILE_NAME)
        || propertyInfo.getFilename().equals(HDFS_LOG4J_FILE_NAME));
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
    PropertyInfo redefinedProperty3 = null;
    PropertyInfo inheritedProperty = null;
    PropertyInfo newProperty = null;
    PropertyInfo originalProperty = null;

    for (PropertyInfo propertyInfo : redefinedService.getProperties()) {
      if (propertyInfo.getName().equals("yarn.resourcemanager.resource-tracker.address")) {
        deleteProperty1 = propertyInfo;
      } else if (propertyInfo.getName().equals("yarn.resourcemanager.scheduler.address")) {
        deleteProperty2 = propertyInfo;
      } else if (propertyInfo.getName().equals("yarn.resourcemanager.address")) {
        redefinedProperty1 = propertyInfo;
      } else if (propertyInfo.getName().equals("yarn.resourcemanager.admin.address")) {
        redefinedProperty2 = propertyInfo;
      } else if (propertyInfo.getName().equals("yarn.nodemanager.health-checker.interval-ms")) {
        redefinedProperty3 = propertyInfo;
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
    // Parent property value will result in property being present in the child stack
    Assert.assertNotNull(redefinedProperty3);
    Assert.assertEquals("135000", redefinedProperty3.getValue());
    // Child can override parent property to empty value
    Assert.assertEquals("", redefinedProperty2.getValue());
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
    Assert.assertEquals(6, redefinedService.getConfigDependencies().size());
    Assert.assertEquals(7, redefinedService.getConfigDependenciesWithComponents().size());
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
    StackExtensionHelper helper = new StackExtensionHelper(injector, metaInfo.getStackRoot());
    helper.fillInfo();
    Map<String, List<StackInfo>> stacks =
      (Map<String, List<StackInfo>>) method.invoke(helper, allStacks);

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
    StackExtensionHelper helper = new StackExtensionHelper(injector,
      metaInfo.getStackRoot());
    helper.fillInfo();
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
    // 3 empty properties
    Assert.assertEquals(103, properties.size());
  }

  @Test
  public void testBadStack() throws Exception {
    File stackRoot = new File("src/test/resources/bad-stacks");
    LOG.info("Stacks file " + stackRoot.getAbsolutePath());
    AmbariMetaInfo mi = new AmbariMetaInfo(stackRoot, new File("target/version"));
    mi.injector = injector;
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

  @Test
  public void testGanglia134Dependencies() throws Exception {
    ServiceInfo service = metaInfo.getService(STACK_NAME_HDP, "1.3.4", "GANGLIA");
    List<ComponentInfo> componentList = service.getComponents();
    Assert.assertEquals(2, componentList.size());
    for (ComponentInfo component : componentList) {
      String name = component.getName();
      if (name.equals("GANGLIA_SERVER")) {
        // dependencies
        Assert.assertEquals(0, component.getDependencies().size());
        // component auto deploy
        Assert.assertNull(component.getAutoDeploy());
        // cardinality
        Assert.assertEquals("1", component.getCardinality());
      }
      if (name.equals("GANGLIA_MONITOR")) {
        // dependencies
        Assert.assertEquals(0, component.getDependencies().size());
        // component auto deploy
        Assert.assertTrue(component.getAutoDeploy().isEnabled());
        // cardinality
        Assert.assertEquals("ALL", component.getCardinality());
      }
    }
  }

  @Test
  public void testHBase134Dependencies() throws Exception {
    ServiceInfo service = metaInfo.getService(STACK_NAME_HDP, "1.3.4", "HBASE");
    List<ComponentInfo> componentList = service.getComponents();
    Assert.assertEquals(3, componentList.size());
    for (ComponentInfo component : componentList) {
      String name = component.getName();
      if (name.equals("HBASE_MASTER")) {
        // dependencies
        List<DependencyInfo> dependencyList = component.getDependencies();
        Assert.assertEquals(2, dependencyList.size());
        for (DependencyInfo dependency : dependencyList) {
          if (dependency.getName().equals("HDFS/HDFS_CLIENT")) {
            Assert.assertEquals("host", dependency.getScope());
            Assert.assertEquals(true, dependency.getAutoDeploy().isEnabled());
          } else if (dependency.getName().equals("ZOOKEEPER/ZOOKEEPER_SERVER")) {
            Assert.assertEquals("cluster", dependency.getScope());
            AutoDeployInfo autoDeploy = dependency.getAutoDeploy();
            Assert.assertEquals(true, autoDeploy.isEnabled());
            Assert.assertEquals("HBASE/HBASE_MASTER", autoDeploy.getCoLocate());
          } else {
            Assert.fail("Unexpected dependency");
          }
        }
        // component auto deploy
        Assert.assertNull(component.getAutoDeploy());
        // cardinality
        Assert.assertEquals("1", component.getCardinality());
      }
      if (name.equals("HBASE_REGIONSERVER")) {
        // dependencies
        Assert.assertEquals(0, component.getDependencies().size());
        // component auto deploy
        Assert.assertNull(component.getAutoDeploy());
        // cardinality
        Assert.assertEquals("1+", component.getCardinality());
      }
      if (name.equals("HBASE_CLIENT")) {
        // dependencies
        Assert.assertEquals(0, component.getDependencies().size());
        // component auto deploy
        Assert.assertNull(component.getAutoDeploy());
        // cardinality
        Assert.assertEquals("0+", component.getCardinality());
      }
    }
  }

  @Test
  public void testHDFS134Dependencies() throws Exception {
    ServiceInfo service = metaInfo.getService(STACK_NAME_HDP, "1.3.4", "HDFS");
    List<ComponentInfo> componentList = service.getComponents();
    Assert.assertEquals(4, componentList.size());
    for (ComponentInfo component : componentList) {
      String name = component.getName();
      if (name.equals("NAMENODE")) {
        // dependencies
        Assert.assertEquals(0, component.getDependencies().size());
        // component auto deploy
        Assert.assertNull(component.getAutoDeploy());
        // cardinality
        Assert.assertEquals("1", component.getCardinality());
      }
      if (name.equals("DATANODE")) {
        // dependencies
        Assert.assertEquals(0, component.getDependencies().size());
        // component auto deploy
        Assert.assertNull(component.getAutoDeploy());
        // cardinality
        Assert.assertEquals("1+", component.getCardinality());
      }
      if (name.equals("SECONDARY_NAMENODE")) {
        // dependencies
        Assert.assertEquals(0, component.getDependencies().size());
        // component auto deploy
        Assert.assertNull(component.getAutoDeploy());
        // cardinality
        Assert.assertEquals("1", component.getCardinality());
      }
      if (name.equals("HDFS_CLIENT")) {
        // dependencies
        Assert.assertEquals(0, component.getDependencies().size());
        // component auto deploy
        Assert.assertNull(component.getAutoDeploy());
        // cardinality
        Assert.assertEquals("0+", component.getCardinality());
      }
    }
  }

  @Test
  public void testHive134Dependencies() throws Exception {
    ServiceInfo service = metaInfo.getService(STACK_NAME_HDP, "1.3.4", "HIVE");
    List<ComponentInfo> componentList = service.getComponents();
    Assert.assertEquals(4, componentList.size());
    for (ComponentInfo component : componentList) {
      String name = component.getName();
      if (name.equals("HIVE_METASTORE")) {
        // dependencies
        Assert.assertEquals(0, component.getDependencies().size());
        // component auto deploy
        AutoDeployInfo autoDeploy = component.getAutoDeploy();
        Assert.assertTrue(autoDeploy.isEnabled());
        Assert.assertEquals("HIVE/HIVE_SERVER", autoDeploy.getCoLocate());
        // cardinality
        Assert.assertEquals("1", component.getCardinality());
      }
      if (name.equals("HIVE_SERVER")) {
        // dependencies
        List<DependencyInfo> dependencyList = component.getDependencies();
        Assert.assertEquals(1, dependencyList.size());
        DependencyInfo dependency = dependencyList.get(0);
        Assert.assertEquals("ZOOKEEPER/ZOOKEEPER_SERVER", dependency.getName());
        Assert.assertEquals("cluster", dependency.getScope());
        AutoDeployInfo autoDeploy = dependency.getAutoDeploy();
        Assert.assertTrue(autoDeploy.isEnabled());
        Assert.assertEquals("HIVE/HIVE_SERVER", autoDeploy.getCoLocate());
        // component auto deploy
        Assert.assertNull(component.getAutoDeploy());
        // cardinality
        Assert.assertEquals("1", component.getCardinality());
      }
      if (name.equals("MYSQL_SERVER")) {
        // dependencies
        Assert.assertEquals(0, component.getDependencies().size());
        // component auto deploy
        AutoDeployInfo autoDeploy = component.getAutoDeploy();
        Assert.assertTrue(autoDeploy.isEnabled());
        Assert.assertEquals("HIVE/HIVE_SERVER", autoDeploy.getCoLocate());
        // cardinality
        Assert.assertEquals("1", component.getCardinality());
      }
      if (name.equals("HIVE_CLIENT")) {
        Assert.assertEquals(0, component.getDependencies().size());
        // component auto deploy
        Assert.assertNull(component.getAutoDeploy());
        // cardinality
        Assert.assertEquals("0+", component.getCardinality());
      }
    }
  }

  @Test
  public void testHue134Dependencies() throws Exception {
    ServiceInfo service = metaInfo.getService(STACK_NAME_HDP, "1.3.4", "HUE");
    List<ComponentInfo> componentList = service.getComponents();
    Assert.assertEquals(1, componentList.size());
    ComponentInfo component = componentList.get(0);
    Assert.assertEquals("HUE_SERVER", component.getName());
    // dependencies
    Assert.assertEquals(0, component.getDependencies().size());
    // component auto deploy
    Assert.assertNull(component.getAutoDeploy());
    // cardinality
    Assert.assertEquals("1", component.getCardinality());
  }

  @Test
  public void testMapReduce134Dependencies() throws Exception {
    ServiceInfo service = metaInfo.getService(STACK_NAME_HDP, "1.3.4", "MAPREDUCE");
    List<ComponentInfo> componentList = service.getComponents();
    Assert.assertEquals(4, componentList.size());
    for (ComponentInfo component : componentList) {
      String name = component.getName();
      if (name.equals("JOBTRACKER")) {
        // dependencies
        Assert.assertEquals(0, component.getDependencies().size());
        // component auto deploy
        Assert.assertNull(component.getAutoDeploy());
        // cardinality
        Assert.assertEquals("1", component.getCardinality());
      }
      if (name.equals("TASKTRACKER")) {
        // dependencies
        Assert.assertEquals(0, component.getDependencies().size());
        // component auto deploy
        Assert.assertNull(component.getAutoDeploy());
        // cardinality
        Assert.assertEquals("1+", component.getCardinality());
      }
      if (name.equals("HISTORYSERVER")) {
        // dependencies
        Assert.assertEquals(0, component.getDependencies().size());
        // component auto deploy
        AutoDeployInfo autoDeploy = component.getAutoDeploy();
        Assert.assertTrue(autoDeploy.isEnabled());
        Assert.assertEquals("MAPREDUCE/JOBTRACKER", autoDeploy.getCoLocate());
        // cardinality
        Assert.assertEquals("1", component.getCardinality());
      }
      if (name.equals("MAPREDUCE_CLIENT")) {
        // dependencies
        Assert.assertEquals(0, component.getDependencies().size());
        // component auto deploy
        Assert.assertNull(component.getAutoDeploy());
        // cardinality
        Assert.assertEquals("0+", component.getCardinality());
      }
    }
  }

  @Test
  public void testNagios134Dependencies() throws Exception {
    ServiceInfo service = metaInfo.getService(STACK_NAME_HDP, "1.3.4", "NAGIOS");
    List<ComponentInfo> componentList = service.getComponents();
    Assert.assertEquals(1, componentList.size());
    ComponentInfo component = componentList.get(0);
    Assert.assertEquals("NAGIOS_SERVER", component.getName());
    // dependencies
    Assert.assertEquals(0, component.getDependencies().size());
    // component auto deploy
    Assert.assertNull(component.getAutoDeploy());
    // cardinality
    Assert.assertEquals("1", component.getCardinality());
  }

  @Test
  public void testOozie134Dependencies() throws Exception {
    ServiceInfo service = metaInfo.getService(STACK_NAME_HDP, "1.3.4", "OOZIE");
    List<ComponentInfo> componentList = service.getComponents();
    Assert.assertEquals(2, componentList.size());
    for (ComponentInfo component : componentList) {
      String name = component.getName();
      if (name.equals("OOZIE_SERVER")) {
        // dependencies
        List<DependencyInfo> dependencyList = component.getDependencies();
        Assert.assertEquals(2, dependencyList.size());
        for (DependencyInfo dependency : dependencyList) {
          if (dependency.getName().equals("HDFS/HDFS_CLIENT")) {
            Assert.assertEquals("host", dependency.getScope());
            Assert.assertEquals(true, dependency.getAutoDeploy().isEnabled());
          } else if (dependency.getName().equals("MAPREDUCE/MAPREDUCE_CLIENT")) {
            Assert.assertEquals("host", dependency.getScope());
            Assert.assertEquals(true, dependency.getAutoDeploy().isEnabled());
          } else {
            Assert.fail("Unexpected dependency");
          }
        }
        // component auto deploy
        Assert.assertNull(component.getAutoDeploy());
        // cardinality
        Assert.assertEquals("1", component.getCardinality());
      }
      if (name.equals("OOZIE_CLIENT")) {
        // dependencies
        List<DependencyInfo> dependencyList = component.getDependencies();
        Assert.assertEquals(2, dependencyList.size());
        for (DependencyInfo dependency : dependencyList) {
          if (dependency.getName().equals("HDFS/HDFS_CLIENT")) {
            Assert.assertEquals("host", dependency.getScope());
            Assert.assertEquals(true, dependency.getAutoDeploy().isEnabled());
          } else if (dependency.getName().equals("MAPREDUCE/MAPREDUCE_CLIENT")) {
            Assert.assertEquals("host", dependency.getScope());
            Assert.assertEquals(true, dependency.getAutoDeploy().isEnabled());
          } else {
            Assert.fail("Unexpected dependency");
          }
        }
        // component auto deploy
        Assert.assertNull(component.getAutoDeploy());
        // cardinality
        Assert.assertEquals("0+", component.getCardinality());
      }
    }
  }

  @Test
  public void testPig134Dependencies() throws Exception {
    ServiceInfo service = metaInfo.getService(STACK_NAME_HDP, "1.3.4", "PIG");
    List<ComponentInfo> componentList = service.getComponents();
    Assert.assertEquals(1, componentList.size());
    ComponentInfo component = componentList.get(0);
    Assert.assertEquals("PIG", component.getName());
    // dependencies
    Assert.assertEquals(0, component.getDependencies().size());
    // component auto deploy
    Assert.assertNull(component.getAutoDeploy());
    // cardinality
    Assert.assertEquals("0+", component.getCardinality());
  }

  @Test
  public void testSqoop134Dependencies() throws Exception {
    ServiceInfo service = metaInfo.getService(STACK_NAME_HDP, "1.3.4", "SQOOP");
    List<ComponentInfo> componentList = service.getComponents();
    Assert.assertEquals(1, componentList.size());
    ComponentInfo component = componentList.get(0);
    Assert.assertEquals("SQOOP", component.getName());
    // dependencies
    List<DependencyInfo> dependencyList = component.getDependencies();
    Assert.assertEquals(2, dependencyList.size());
    for (DependencyInfo dependency : dependencyList) {
      if (dependency.getName().equals("HDFS/HDFS_CLIENT")) {
        Assert.assertEquals("host", dependency.getScope());
        Assert.assertEquals(true, dependency.getAutoDeploy().isEnabled());
      } else if (dependency.getName().equals("MAPREDUCE/MAPREDUCE_CLIENT")) {
        Assert.assertEquals("host", dependency.getScope());
        Assert.assertEquals(true, dependency.getAutoDeploy().isEnabled());
      } else {
        Assert.fail("Unexpected dependency");
      }
    }
    // component auto deploy
    Assert.assertNull(component.getAutoDeploy());
    // cardinality
    Assert.assertEquals("0+", component.getCardinality());
  }

  @Test
  public void testWebHCat134Dependencies() throws Exception {
    ServiceInfo service = metaInfo.getService(STACK_NAME_HDP, "1.3.4", "WEBHCAT");
    List<ComponentInfo> componentList = service.getComponents();
    Assert.assertEquals(1, componentList.size());
    ComponentInfo component = componentList.get(0);
    Assert.assertEquals("WEBHCAT_SERVER", component.getName());
    // dependencies
    List<DependencyInfo> dependencyList = component.getDependencies();
    Assert.assertEquals(4, dependencyList.size());
    for (DependencyInfo dependency : dependencyList) {
      if (dependency.getName().equals("HDFS/HDFS_CLIENT")) {
        Assert.assertEquals("host", dependency.getScope());
        Assert.assertEquals(true, dependency.getAutoDeploy().isEnabled());
      } else if (dependency.getName().equals("MAPREDUCE/MAPREDUCE_CLIENT")) {
        Assert.assertEquals("host", dependency.getScope());
        Assert.assertEquals(true, dependency.getAutoDeploy().isEnabled());
      } else if (dependency.getName().equals("ZOOKEEPER/ZOOKEEPER_SERVER")) {
        Assert.assertEquals("cluster", dependency.getScope());
        AutoDeployInfo autoDeploy = dependency.getAutoDeploy();
        Assert.assertEquals(true, autoDeploy.isEnabled());
        Assert.assertEquals("WEBHCAT/WEBHCAT_SERVER", autoDeploy.getCoLocate());
      }else if (dependency.getName().equals("ZOOKEEPER/ZOOKEEPER_CLIENT")) {
        Assert.assertEquals("host", dependency.getScope());
        Assert.assertEquals(true, dependency.getAutoDeploy().isEnabled());
      }else {
        Assert.fail("Unexpected dependency");
      }
    }
    // component auto deploy
    Assert.assertNull(component.getAutoDeploy());
    // cardinality
    Assert.assertEquals("1", component.getCardinality());
  }

  @Test
  public void testZooKeeper134Dependencies() throws Exception {
    ServiceInfo service = metaInfo.getService(STACK_NAME_HDP, "1.3.4", "ZOOKEEPER");
    List<ComponentInfo> componentList = service.getComponents();
    Assert.assertEquals(2, componentList.size());
    for (ComponentInfo component : componentList) {
      String name = component.getName();
      if (name.equals("ZOOKEEPER_SERVER")) {
        // dependencies
        Assert.assertEquals(0, component.getDependencies().size());
        // component auto deploy
        Assert.assertNull(component.getAutoDeploy());
        // cardinality
        Assert.assertEquals("1", component.getCardinality());
      }
      if (name.equals("ZOOKEEPER_CLIENT")) {
        // dependencies
        Assert.assertEquals(0, component.getDependencies().size());
        // component auto deploy
        Assert.assertNull(component.getAutoDeploy());
        // cardinality
        Assert.assertEquals("0+", component.getCardinality());
      }
    }
  }


  @Test
  public void testHooksDirInheritance() throws Exception {
    // Test hook dir determination in parent
    StackInfo stackInfo = metaInfo.getStackInfo(STACK_NAME_HDP, "2.0.6");
    Assert.assertEquals("HDP/2.0.6/hooks", stackInfo.getStackHooksFolder());
    // Test hook dir inheritance
    stackInfo = metaInfo.getStackInfo(STACK_NAME_HDP, "2.0.7");
    Assert.assertEquals("HDP/2.0.6/hooks", stackInfo.getStackHooksFolder());
    // Test hook dir override
    stackInfo = metaInfo.getStackInfo(STACK_NAME_HDP, "2.0.8");
    Assert.assertEquals("HDP/2.0.8/hooks", stackInfo.getStackHooksFolder());
  }


  @Test
  public void testServicePackageDirInheritance() throws Exception {
    // Test service package dir determination in parent
    ServiceInfo service = metaInfo.getService(STACK_NAME_HDP, "2.0.7", "HBASE");
    Assert.assertEquals("HDP/2.0.7/services/HBASE/package",
            service.getServicePackageFolder());

    service = metaInfo.getService(STACK_NAME_HDP, "2.0.7", "HDFS");
    Assert.assertEquals("HDP/2.0.7/services/HDFS/package",
            service.getServicePackageFolder());
    // Test service package dir inheritance
    service = metaInfo.getService(STACK_NAME_HDP, "2.0.8", "HBASE");
    Assert.assertEquals("HDP/2.0.7/services/HBASE/package",
            service.getServicePackageFolder());
    // Test service package dir override
    service = metaInfo.getService(STACK_NAME_HDP, "2.0.8", "HDFS");
    Assert.assertEquals("HDP/2.0.8/services/HDFS/package",
            service.getServicePackageFolder());
  }


  @Test
  public void testServiceCommandScriptInheritance() throws Exception {
    // Test command script determination in parent
    ServiceInfo service = metaInfo.getService(STACK_NAME_HDP, "2.0.7", "HDFS");
    Assert.assertEquals("scripts/service_check_1.py",
            service.getCommandScript().getScript());
    service = metaInfo.getService(STACK_NAME_HDP, "2.0.7", "HBASE");
    Assert.assertEquals("scripts/service_check.py",
            service.getCommandScript().getScript());
    // Test command script inheritance
    service = metaInfo.getService(STACK_NAME_HDP, "2.0.8", "HBASE");
    Assert.assertEquals("scripts/service_check.py",
            service.getCommandScript().getScript());
    // Test command script override
    service = metaInfo.getService(STACK_NAME_HDP, "2.0.8", "HDFS");
    Assert.assertEquals("scripts/service_check_2.py",
            service.getCommandScript().getScript());
  }

    @Test
  public void testComponentCommandScriptInheritance() throws Exception {
    // Test command script determination in parent
    ComponentInfo component = metaInfo.getComponent(STACK_NAME_HDP,
            "2.0.7", "HDFS", "HDFS_CLIENT");
    Assert.assertEquals("scripts/hdfs_client.py",
            component.getCommandScript().getScript());
    component = metaInfo.getComponent(STACK_NAME_HDP,
            "2.0.7", "HBASE", "HBASE_MASTER");
    Assert.assertEquals("scripts/hbase_master.py",
            component.getCommandScript().getScript());
    // Test command script inheritance
    component = metaInfo.getComponent(STACK_NAME_HDP,
            "2.0.8", "HBASE", "HBASE_MASTER");
    Assert.assertEquals("scripts/hbase_master.py",
            component.getCommandScript().getScript());
    // Test command script override
    component = metaInfo.getComponent(STACK_NAME_HDP,
            "2.0.8", "HDFS", "HDFS_CLIENT");
    Assert.assertEquals("scripts/hdfs_client_overridden.py",
            component.getCommandScript().getScript());
  }


  @Test
  public void testServiceCustomCommandScriptInheritance() throws Exception {
    // Test custom command script determination in parent
    ServiceInfo service = metaInfo.getService(STACK_NAME_HDP, "2.0.7", "HDFS");

    CustomCommandDefinition ccd = findCustomCommand("RESTART", service);
    Assert.assertEquals("scripts/restart_parent.py",
            ccd.getCommandScript().getScript());

    ccd = findCustomCommand("YET_ANOTHER_PARENT_SRV_COMMAND", service);
    Assert.assertEquals("scripts/yet_another_parent_srv_command.py",
            ccd.getCommandScript().getScript());

    Assert.assertEquals(2, service.getCustomCommands().size());

    // Test custom command script inheritance
    service = metaInfo.getService(STACK_NAME_HDP, "2.0.8", "HDFS");
    Assert.assertEquals(3, service.getCustomCommands().size());

    ccd = findCustomCommand("YET_ANOTHER_PARENT_SRV_COMMAND", service);
    Assert.assertEquals("scripts/yet_another_parent_srv_command.py",
            ccd.getCommandScript().getScript());

    // Test custom command script override
    service = metaInfo.getService(STACK_NAME_HDP, "2.0.8", "HDFS");

    ccd = findCustomCommand("RESTART", service);
    Assert.assertEquals("scripts/restart_child.py",
            ccd.getCommandScript().getScript());

    ccd = findCustomCommand("YET_ANOTHER_CHILD_SRV_COMMAND", service);
    Assert.assertEquals("scripts/yet_another_child_srv_command.py",
            ccd.getCommandScript().getScript());
  }


  @Test
  public void testChildCustomCommandScriptInheritance() throws Exception {
    // Test custom command script determination in parent
    ComponentInfo component = metaInfo.getComponent(STACK_NAME_HDP, "2.0.7",
            "HDFS", "NAMENODE");

    CustomCommandDefinition ccd = findCustomCommand("DECOMMISSION", component);
    Assert.assertEquals("scripts/namenode_dec.py",
            ccd.getCommandScript().getScript());

    ccd = findCustomCommand("YET_ANOTHER_PARENT_COMMAND", component);
    Assert.assertEquals("scripts/yet_another_parent_command.py",
            ccd.getCommandScript().getScript());

    ccd = findCustomCommand("REBALANCEHDFS", component);
    Assert.assertEquals("scripts/namenode.py",
        ccd.getCommandScript().getScript());
    Assert.assertTrue(ccd.isBackground());

    Assert.assertEquals(3, component.getCustomCommands().size());

    // Test custom command script inheritance
    component = metaInfo.getComponent(STACK_NAME_HDP, "2.0.8",
            "HDFS", "NAMENODE");
    Assert.assertEquals(4, component.getCustomCommands().size());

    ccd = findCustomCommand("YET_ANOTHER_PARENT_COMMAND", component);
    Assert.assertEquals("scripts/yet_another_parent_command.py",
            ccd.getCommandScript().getScript());

    // Test custom command script override
    ccd = findCustomCommand("DECOMMISSION", component);
    Assert.assertEquals("scripts/namenode_dec_overr.py",
            ccd.getCommandScript().getScript());

    ccd = findCustomCommand("YET_ANOTHER_CHILD_COMMAND", component);
    Assert.assertEquals("scripts/yet_another_child_command.py",
            ccd.getCommandScript().getScript());
  }


  @Test
  public void testServiceOsSpecificsInheritance() throws Exception {
    // Test command script determination in parent
    ServiceInfo service = metaInfo.getService(STACK_NAME_HDP, "2.0.7", "HDFS");
    Assert.assertEquals("parent-package-def",
            service.getOsSpecifics().get("any").getPackages().get(0).getName());
    service = metaInfo.getService(STACK_NAME_HDP, "2.0.7", "HBASE");
    Assert.assertEquals(2, service.getOsSpecifics().keySet().size());
    // Test command script inheritance
    service = metaInfo.getService(STACK_NAME_HDP, "2.0.8", "HBASE");
    Assert.assertEquals(2, service.getOsSpecifics().keySet().size());
    // Test command script override
    service = metaInfo.getService(STACK_NAME_HDP, "2.0.8", "HDFS");
    Assert.assertEquals("child-package-def",
            service.getOsSpecifics().get("any").getPackages().get(0).getName());
  }

  @Test
  public void testServiceSchemaVersionInheritance() throws Exception {
    // Check parent
    ServiceInfo service = metaInfo.getService(STACK_NAME_HDP, "2.0.7", "SQOOP");
    Assert.assertEquals("2.0", service.getSchemaVersion());
    // Test child service metainfo after merge
    service = metaInfo.getService(STACK_NAME_HDP, "2.0.8", "SQOOP");
    Assert.assertEquals("2.0", service.getSchemaVersion());
  }


  private CustomCommandDefinition findCustomCommand(String ccName,
                                                    ServiceInfo service) {
    for (CustomCommandDefinition ccd: service.getCustomCommands()) {
      if (ccd.getName().equals(ccName)) {
        return ccd;
      }
    }
    return null;
  }

  private CustomCommandDefinition findCustomCommand(String ccName,
                                                    ComponentInfo component) {
    for (CustomCommandDefinition ccd: component.getCustomCommands()) {
      if (ccd.getName().equals(ccName)) {
        return ccd;
      }
    }
    return null;
  }

  @Test
  public void testCustomConfigDir() throws Exception {

    ServiceInfo service = metaInfo.getService(STACK_NAME_HDP, "2.0.7", "MAPREDUCE2");

    // assert that the property was found in a differently-named directory
    // cannot check the dirname itself since extended stacks won't carry over
    // the name

    boolean found = false;
    for (PropertyInfo pi : service.getProperties()) {
      if (pi.getName().equals("mr2-prop")) {
        Assert.assertEquals("some-mr2-value", pi.getValue());
        found = true;
      }
    }

    Assert.assertTrue(found);
  }

  @Test
  public void testLatestRepo() throws Exception {

    for (RepositoryInfo ri : metaInfo.getRepositories("HDP", "2.1.1", "centos6")) {
      Assert.assertEquals(
          "Expected the base url to be set properly",
          "http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos6/2.x/BUILDS/2.1.1.0-118",
          ri.getLatestBaseUrl());
      Assert.assertEquals(
          "Expected the default URL to be the same as in the xml file",
          "http://public-repo-1.hortonworks.com/HDP/centos6/2.x/updates/2.0.6.0",
          ri.getDefaultBaseUrl());
    }

    for (RepositoryInfo ri : metaInfo.getRepositories("HDP", "2.1.1", "suse11")) {
      Assert.assertEquals(
          "Expected hdp.json to be stripped from the url",
          "http://s3.amazonaws.com/dev.hortonworks.com/HDP/suse11/2.x/BUILDS/2.1.1.0-118",
          ri.getLatestBaseUrl());
    }

    for (RepositoryInfo ri : metaInfo.getRepositories("HDP", "2.1.1", "sles11")) {
      Assert.assertEquals("http://s3.amazonaws.com/dev.hortonworks.com/HDP/suse11/2.x/BUILDS/2.1.1.0-118",
          ri.getLatestBaseUrl());
    }
  }

  @Test
  public void testGetComponentDependency() throws AmbariException {
    DependencyInfo dependency = metaInfo.getComponentDependency("HDP", "1.3.4", "HIVE", "HIVE_SERVER", "ZOOKEEPER_SERVER");
    assertEquals("ZOOKEEPER/ZOOKEEPER_SERVER", dependency.getName());
    assertEquals("ZOOKEEPER_SERVER", dependency.getComponentName());
    assertEquals("ZOOKEEPER", dependency.getServiceName());
    assertEquals("cluster", dependency.getScope());
  }

  @Test
  public void testGetComponentDependencies() throws AmbariException {
    List<DependencyInfo> dependencies = metaInfo.getComponentDependencies("HDP", "1.3.4", "HBASE", "HBASE_MASTER");
    assertEquals(2, dependencies.size());

    DependencyInfo dependency = dependencies.get(0);
    assertEquals("HDFS/HDFS_CLIENT", dependency.getName());
    assertEquals("HDFS_CLIENT", dependency.getComponentName());
    assertEquals("HDFS", dependency.getServiceName());
    assertEquals("host", dependency.getScope());

    dependency = dependencies.get(1);
    assertEquals("ZOOKEEPER/ZOOKEEPER_SERVER", dependency.getName());
    assertEquals("ZOOKEEPER_SERVER", dependency.getComponentName());
    assertEquals("ZOOKEEPER", dependency.getServiceName());
    assertEquals("cluster", dependency.getScope());
  }

  @Test
  public void testPasswordPropertyAttribute() throws Exception {
    ServiceInfo service = metaInfo.getService(STACK_NAME_HDP, "2.0.1", "HIVE");
    List<PropertyInfo> propertyInfoList = service.getProperties();
    Assert.assertNotNull(propertyInfoList);
    PropertyInfo passwordProperty = null;
    for (PropertyInfo propertyInfo : propertyInfoList) {
      if (propertyInfo.isRequireInput()
          && propertyInfo.getPropertyTypes().contains(PropertyInfo.PropertyType.PASSWORD)) {
        passwordProperty = propertyInfo;
      } else {
        Assert.assertTrue(propertyInfo.getPropertyTypes().isEmpty());
      }
    }
    Assert.assertNotNull(passwordProperty);
    Assert.assertEquals("javax.jdo.option.ConnectionPassword", passwordProperty.getName());
  }

  @Test
  @Ignore
  public void testAlertsJson() throws Exception {
    ServiceInfo svc = metaInfo.getService(STACK_NAME_HDP, "2.0.5", "HDFS");
    Assert.assertNotNull(svc);
    Assert.assertNotNull(svc.getAlertsFile());

    svc = metaInfo.getService(STACK_NAME_HDP, "2.0.6", "HDFS");
    Assert.assertNotNull(svc);
    Assert.assertNotNull(svc.getAlertsFile());

    svc = metaInfo.getService(STACK_NAME_HDP, "1.3.4", "HDFS");
    Assert.assertNotNull(svc);
    Assert.assertNull(svc.getAlertsFile());

    Set<AlertDefinition> set = metaInfo.getAlertDefinitions(STACK_NAME_HDP,
        "2.0.5", "HDFS");
    Assert.assertNotNull(set);
    Assert.assertTrue(set.size() > 0);

  }

  private AmbariMetaInfo setupTempAmbariMetaInfo(String buildDir)
      throws Exception {
    File stackRootTmp = new File(buildDir + "/ambari-metaInfo");
    File stackRoot = new File("src/test/resources/stacks");
    stackRootTmp.mkdir();
    FileUtils.copyDirectory(stackRoot, stackRootTmp);
    AmbariMetaInfo ambariMetaInfo = new AmbariMetaInfo(stackRootTmp, new File(
        "target/version"));
    injector.injectMembers(ambariMetaInfo);
    return ambariMetaInfo;
  }
}
