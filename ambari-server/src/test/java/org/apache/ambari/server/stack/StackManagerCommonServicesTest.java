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

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.orm.dao.MetainfoDAO;
import org.apache.ambari.server.state.*;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.commons.lang.StringUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.*;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
 * StackManager unit tests.
 */
public class StackManagerCommonServicesTest {

  private static StackManager stackManager;
  private static MetainfoDAO dao;
  private static ActionMetadata actionMetadata;
  private static OsFamily osFamily;

  @BeforeClass
  public static void initStack() throws Exception{
    stackManager = createTestStackManager();
  }

  public static StackManager createTestStackManager() throws Exception {
    String stack = ClassLoader.getSystemClassLoader().getResource("stacks_with_common_services").getPath();
    String commonServices = ClassLoader.getSystemClassLoader().getResource("common-services").getPath();
    return createTestStackManager(stack, commonServices);
  }

  public static StackManager createTestStackManager(String stackRoot, String commonServicesRoot) throws Exception {
    try {
      //todo: dao , actionMetaData expectations
      dao = createNiceMock(MetainfoDAO.class);
      actionMetadata = createNiceMock(ActionMetadata.class);
      Configuration config = createNiceMock(Configuration.class);
      expect(config.getSharedResourcesDirPath()).andReturn(
          ClassLoader.getSystemClassLoader().getResource("").getPath()).anyTimes();
      replay(config);
      osFamily = new OsFamily(config);

      replay(dao, actionMetadata);
      StackManager stackManager = new StackManager(
          new File(stackRoot), new File(commonServicesRoot), new StackContext(dao, actionMetadata, osFamily));
      return stackManager;
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  @Test
  public void testGetStacks_count() throws Exception {
    Collection<StackInfo> stacks = stackManager.getStacks();
    assertEquals(2, stacks.size());
  }

  @Test
  public void testGetStack_name__count() {
    Collection<StackInfo> stacks = stackManager.getStacks("HDP");
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

    assertEquals(pigService.getParent(), "common-services/PIG/1.0");
  }

  @Test
  public void testGetServicePackageFolder() {
    StackInfo stack = stackManager.getStack("HDP", "0.1");
    assertNotNull(stack);
    assertEquals("HDP", stack.getName());
    assertEquals("0.1", stack.getVersion());
    ServiceInfo hdfsService1 = stack.getService("HDFS");
    assertNotNull(hdfsService1);

    stack = stackManager.getStack("HDP", "0.2");
    assertNotNull(stack);
    assertEquals("HDP", stack.getName());
    assertEquals("0.2", stack.getVersion());
    ServiceInfo hdfsService2 = stack.getService("HDFS");
    assertNotNull(hdfsService2);

    String packageDir1 = StringUtils.join(
        new String[]{"common-services", "HDFS", "1.0", "package"}, File.separator);
    String packageDir2 = StringUtils.join(
        new String[]{"stacks_with_common_services", "HDP", "0.2", "services", "HDFS", "package"}, File.separator);

    assertEquals(packageDir1, hdfsService1.getServicePackageFolder());
    assertEquals(packageDir2, hdfsService2.getServicePackageFolder());
  }
}
