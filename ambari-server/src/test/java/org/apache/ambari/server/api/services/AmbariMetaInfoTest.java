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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AmbariMetaInfoTest {

  private static String STACK_NAME_HDP = "HDP";
  private static String STACK_VERSION_HDP = "0.1";
  private static String SERVICE_NAME_HDFS = "HDFS";
  private static String SERVICE_COMPONENT_NAME = "NAMENODE";

  private AmbariMetaInfo metaInfo = null;
  private final static Logger LOG =
      LoggerFactory.getLogger(AmbariMetaInfoTest.class);

  @Before
  public void before() throws Exception {
    File stackRoot = new File("src/test/resources/stacks");
   LOG.info("Stacks file " + stackRoot.getAbsolutePath());
    metaInfo = new AmbariMetaInfo(stackRoot);
    try {
      metaInfo.init();
    } catch(Exception e) {
      LOG.info("Error in initializing ", e);
    }
  }

  @Test
  public void getComponentCategory() {
    ComponentInfo componentInfo = metaInfo.getComponentCategory(STACK_NAME_HDP,
        STACK_VERSION_HDP, SERVICE_NAME_HDFS, SERVICE_COMPONENT_NAME);
    assertNotNull(componentInfo);
    componentInfo = metaInfo.getComponentCategory(STACK_NAME_HDP,
        STACK_VERSION_HDP, SERVICE_NAME_HDFS, "DATANODE1");
    Assert.assertNotNull(componentInfo);
    assertTrue(!componentInfo.isClient());
  }

  @Test
  public void getComponentsByService() {
    List<ComponentInfo> components = metaInfo.getComponentsByService(
        STACK_NAME_HDP, STACK_VERSION_HDP, SERVICE_NAME_HDFS);
    assertNotNull(components);
  }

  @Test
  public void getRepository() {
    Map<String, List<RepositoryInfo>> repository = metaInfo.getRepository(
        STACK_NAME_HDP, STACK_VERSION_HDP);
    assertNotNull(repository);
    assertFalse(repository.get("centos5").isEmpty());
    assertFalse(repository.get("centos6").isEmpty());
  }

  @Test
  public void isSupportedStack() {
    boolean supportedStack = metaInfo.isSupportedStack(STACK_VERSION_HDP,
        STACK_VERSION_HDP);
    assertTrue(supportedStack);
  }

  @Test
  public void isValidService() {
    boolean valid = metaInfo.isValidService(STACK_NAME_HDP, STACK_VERSION_HDP,
        SERVICE_NAME_HDFS);
    assertTrue(valid);
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
      assertNotSame(propertyKeys.size(), 0);
    }
  }

  @Test
  public void testServiceNameUsingComponentName() {
    String serviceName = metaInfo.getComponentToService(STACK_NAME_HDP,
        STACK_VERSION_HDP, "NAMENODE");
    assertTrue("HDFS".equals(serviceName));
  }

  /**
   * Method: Map<String, ServiceInfo> getServices(String stackName, String
   * version, String serviceName)
   */
  @Test
  public void getServices() {
    Map<String, ServiceInfo> services = metaInfo.getServices(STACK_NAME_HDP,
        STACK_VERSION_HDP);
    LOG.info("Getting all the services ");
    for (Map.Entry<String, ServiceInfo> entry : services.entrySet()) {
      LOG.info("Service Name " + entry.getKey() + " values " + entry.getValue());
    }
    assertTrue(services.containsKey("HDFS"));
    assertTrue(services.containsKey("MAPREDUCE"));
    assertNotNull(services);
    assertNotSame(services.keySet().size(), 0);
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

  /**
   * Method: getSupportedServices(String stackName, String version)
   */
  @Test
  public void getSupportedServices() throws Exception {
    List<ServiceInfo> services = metaInfo.getSupportedServices(STACK_NAME_HDP,
        STACK_VERSION_HDP);
    assertNotNull(services);
    assertNotSame(services.size(), 0);

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

}
