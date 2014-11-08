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

package org.apache.ambari.server.controller.internal;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.StackServiceResponse;
import org.easymock.EasyMockSupport;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

/**
 * BlueprintConfigurationProcessor unit tests.
 */
public class BlueprintConfigurationProcessorTest {

  @Test
  public void testDoUpdateForBlueprintExport_SingleHostProperty() {
    Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();
    Map<String, String> typeProps = new HashMap<String, String>();
    typeProps.put("yarn.resourcemanager.hostname", "testhost");
    properties.put("yarn-site", typeProps);

    Collection<String> hgComponents = new HashSet<String>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("RESOURCEMANAGER");
    HostGroup group1 = new TestHostGroup("group1", Collections.singleton("testhost"), hgComponents);

    Collection<String> hgComponents2 = new HashSet<String>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    HostGroup group2 = new TestHostGroup("group2", Collections.singleton("testhost2"), hgComponents2);

    Collection<HostGroup> hostGroups = new HashSet<HostGroup>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(properties);
    Map<String, Map<String, String>> updatedProperties = updater.doUpdateForBlueprintExport(hostGroups);
    String updatedVal = updatedProperties.get("yarn-site").get("yarn.resourcemanager.hostname");
    assertEquals("%HOSTGROUP::group1%", updatedVal);
  }
  
  @Test
  public void testDoUpdateForBlueprintExport_SingleHostProperty__withPort() {
    Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();
    Map<String, String> typeProps = new HashMap<String, String>();
    typeProps.put("fs.defaultFS", "testhost:8020");
    properties.put("core-site", typeProps);

    Collection<String> hgComponents = new HashSet<String>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    HostGroup group1 = new TestHostGroup("group1", Collections.singleton("testhost"), hgComponents);

    Collection<String> hgComponents2 = new HashSet<String>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    HostGroup group2 = new TestHostGroup("group2", Collections.singleton("testhost2"), hgComponents2);

    Collection<HostGroup> hostGroups = new HashSet<HostGroup>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(properties);
    Map<String, Map<String, String>> updatedProperties = updater.doUpdateForBlueprintExport(hostGroups);
    String updatedVal = updatedProperties.get("core-site").get("fs.defaultFS");
    assertEquals("%HOSTGROUP::group1%:8020", updatedVal);
  }

  @Test
  public void testDoUpdateForBlueprintExport_SingleHostProperty__ExternalReference() {
    Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();
    Map<String, String> typeProps = new HashMap<String, String>();
    typeProps.put("yarn.resourcemanager.hostname", "external-host");
    properties.put("yarn-site", typeProps);

    Collection<String> hgComponents = new HashSet<String>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("RESOURCEMANAGER");
    HostGroup group1 = new TestHostGroup("group1", Collections.singleton("testhost"), hgComponents);

    Collection<String> hgComponents2 = new HashSet<String>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    HostGroup group2 = new TestHostGroup("group2", Collections.singleton("testhost2"), hgComponents2);

    Collection<HostGroup> hostGroups = new HashSet<HostGroup>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(properties);
    Map<String, Map<String, String>> updatedProperties = updater.doUpdateForBlueprintExport(hostGroups);
    assertFalse(updatedProperties.get("yarn-site").containsKey("yarn.resourcemanager.hostname"));
  }

  @Test
  public void testDoUpdateForBlueprintExport_MultiHostProperty() {
    Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();
    Map<String, String> typeProps = new HashMap<String, String>();
    typeProps.put("hbase.zookeeper.quorum", "testhost,testhost2,testhost2a,testhost2b");
    properties.put("hbase-site", typeProps);

    Collection<String> hgComponents = new HashSet<String>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("ZOOKEEPER_SERVER");
    HostGroup group1 = new TestHostGroup("group1", Collections.singleton("testhost"), hgComponents);

    Collection<String> hgComponents2 = new HashSet<String>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    hgComponents2.add("ZOOKEEPER_SERVER");
    Set<String> hosts2 = new HashSet<String>();
    hosts2.add("testhost2");
    hosts2.add("testhost2a");
    hosts2.add("testhost2b");
    HostGroup group2 = new TestHostGroup("group2", hosts2, hgComponents2);

    Collection<String> hgComponents3 = new HashSet<String>();
    hgComponents2.add("HDFS_CLIENT");
    hgComponents2.add("ZOOKEEPER_CLIENT");
    Set<String> hosts3 = new HashSet<String>();
    hosts3.add("testhost3");
    hosts3.add("testhost3a");
    HostGroup group3 = new TestHostGroup("group3", hosts3, hgComponents3);

    Collection<HostGroup> hostGroups = new HashSet<HostGroup>();
    hostGroups.add(group1);
    hostGroups.add(group2);
    hostGroups.add(group3);

    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(properties);
    Map<String, Map<String, String>> updatedProperties = updater.doUpdateForBlueprintExport(hostGroups);
    String updatedVal = updatedProperties.get("hbase-site").get("hbase.zookeeper.quorum");
    assertEquals("%HOSTGROUP::group1%,%HOSTGROUP::group2%", updatedVal);
  }

  @Test
  public void testDoUpdateForBlueprintExport_MultiHostProperty__WithPorts() {
    Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();
    Map<String, String> typeProps = new HashMap<String, String>();
    typeProps.put("templeton.zookeeper.hosts", "testhost:5050,testhost2:9090,testhost2a:9090,testhost2b:9090");
    properties.put("webhcat-site", typeProps);

    Collection<String> hgComponents = new HashSet<String>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("ZOOKEEPER_SERVER");
    HostGroup group1 = new TestHostGroup("group1", Collections.singleton("testhost"), hgComponents);

    Collection<String> hgComponents2 = new HashSet<String>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    hgComponents2.add("ZOOKEEPER_SERVER");
    Set<String> hosts2 = new HashSet<String>();
    hosts2.add("testhost2");
    hosts2.add("testhost2a");
    hosts2.add("testhost2b");
    HostGroup group2 = new TestHostGroup("group2", hosts2, hgComponents2);

    Collection<String> hgComponents3 = new HashSet<String>();
    hgComponents2.add("HDFS_CLIENT");
    hgComponents2.add("ZOOKEEPER_CLIENT");
    Set<String> hosts3 = new HashSet<String>();
    hosts3.add("testhost3");
    hosts3.add("testhost3a");
    HostGroup group3 = new TestHostGroup("group3", hosts3, hgComponents3);

    Collection<HostGroup> hostGroups = new HashSet<HostGroup>();
    hostGroups.add(group1);
    hostGroups.add(group2);
    hostGroups.add(group3);

    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(properties);
    Map<String, Map<String, String>> updatedProperties = updater.doUpdateForBlueprintExport(hostGroups);
    String updatedVal = updatedProperties.get("webhcat-site").get("templeton.zookeeper.hosts");
    assertEquals("%HOSTGROUP::group1%:5050,%HOSTGROUP::group2%:9090", updatedVal);
  }

  @Test
  public void testDoUpdateForBlueprintExport_MultiHostProperty__YAML() {
    Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();
    Map<String, String> typeProps = new HashMap<String, String>();
    typeProps.put("storm.zookeeper.servers", "['testhost:5050','testhost2:9090','testhost2a:9090','testhost2b:9090']");
    properties.put("storm-site", typeProps);

    Collection<String> hgComponents = new HashSet<String>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("ZOOKEEPER_SERVER");
    HostGroup group1 = new TestHostGroup("group1", Collections.singleton("testhost"), hgComponents);

    Collection<String> hgComponents2 = new HashSet<String>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    hgComponents2.add("ZOOKEEPER_SERVER");
    Set<String> hosts2 = new HashSet<String>();
    hosts2.add("testhost2");
    hosts2.add("testhost2a");
    hosts2.add("testhost2b");
    HostGroup group2 = new TestHostGroup("group2", hosts2, hgComponents2);

    Collection<String> hgComponents3 = new HashSet<String>();
    hgComponents2.add("HDFS_CLIENT");
    hgComponents2.add("ZOOKEEPER_CLIENT");
    Set<String> hosts3 = new HashSet<String>();
    hosts3.add("testhost3");
    hosts3.add("testhost3a");
    HostGroup group3 = new TestHostGroup("group3", hosts3, hgComponents3);

    Collection<HostGroup> hostGroups = new HashSet<HostGroup>();
    hostGroups.add(group1);
    hostGroups.add(group2);
    hostGroups.add(group3);

    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(properties);
    Map<String, Map<String, String>> updatedProperties = updater.doUpdateForBlueprintExport(hostGroups);
    String updatedVal = updatedProperties.get("storm-site").get("storm.zookeeper.servers");
    assertEquals("['%HOSTGROUP::group1%:5050','%HOSTGROUP::group2%:9090']", updatedVal);
  }

  @Test
  public void testDoUpdateForBlueprintExport_DBHostProperty() {
    Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();
    Map<String, String> hiveSiteProps = new HashMap<String, String>();
    hiveSiteProps.put("javax.jdo.option.ConnectionURL", "jdbc:mysql://testhost/hive?createDatabaseIfNotExist=true");
    properties.put("hive-site", hiveSiteProps);

    Collection<String> hgComponents = new HashSet<String>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("RESOURCEMANAGER");
    hgComponents.add("MYSQL_SERVER");
    HostGroup group1 = new TestHostGroup("group1", Collections.singleton("testhost"), hgComponents);

    Collection<String> hgComponents2 = new HashSet<String>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    HostGroup group2 = new TestHostGroup("group2", Collections.singleton("testhost2"), hgComponents2);

    Collection<HostGroup> hostGroups = new HashSet<HostGroup>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(properties);
    Map<String, Map<String, String>> updatedProperties = updater.doUpdateForBlueprintExport(hostGroups);
    String updatedVal = updatedProperties.get("hive-site").get("javax.jdo.option.ConnectionURL");
    assertEquals("jdbc:mysql://%HOSTGROUP::group1%/hive?createDatabaseIfNotExist=true", updatedVal);
  }

  @Test
  public void testDoUpdateForBlueprintExport_DBHostProperty__External() {
    Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();
    Map<String, String> typeProps = new HashMap<String, String>();
    typeProps.put("javax.jdo.option.ConnectionURL", "jdbc:mysql://external-host/hive?createDatabaseIfNotExist=true");
    properties.put("hive-site", typeProps);

    Collection<String> hgComponents = new HashSet<String>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("RESOURCEMANAGER");
    HostGroup group1 = new TestHostGroup("group1", Collections.singleton("testhost"), hgComponents);

    Collection<String> hgComponents2 = new HashSet<String>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    HostGroup group2 = new TestHostGroup("group2", Collections.singleton("testhost2"), hgComponents2);

    Collection<HostGroup> hostGroups = new HashSet<HostGroup>();
    hostGroups.add(group1);
    hostGroups.add(group2);

    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(properties);
    Map<String, Map<String, String>> updatedProperties = updater.doUpdateForBlueprintExport(hostGroups);
    assertFalse(updatedProperties.get("hive-site").containsKey("javax.jdo.option.ConnectionURL"));
  }

  @Test
  public void testDoUpdateForClusterCreate_SingleHostProperty__defaultValue() {
    Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();
    Map<String, String> typeProps = new HashMap<String, String>();
    typeProps.put("yarn.resourcemanager.hostname", "localhost");
    properties.put("yarn-site", typeProps);

    Collection<String> hgComponents = new HashSet<String>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("RESOURCEMANAGER");
    HostGroup group1 = new TestHostGroup("group1", Collections.singleton("testhost"), hgComponents);

    Collection<String> hgComponents2 = new HashSet<String>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    HostGroup group2 = new TestHostGroup("group2", Collections.singleton("testhost2"), hgComponents2);

    Map<String, HostGroup> hostGroups = new HashMap<String, HostGroup>();
    hostGroups.put(group1.getName(), group1);
    hostGroups.put(group2.getName(), group2);

    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(properties);
    Map<String, Map<String, String>> updatedProperties = updater.doUpdateForClusterCreate(hostGroups, null);
    String updatedVal = updatedProperties.get("yarn-site").get("yarn.resourcemanager.hostname");
    assertEquals("testhost", updatedVal);
  }

  @Test
  public void testDoUpdateForClusterCreate_SingleHostProperty__MissingComponent() throws Exception {
    EasyMockSupport mockSupport = new EasyMockSupport();


    AmbariManagementController mockMgmtController =
      mockSupport.createMock(AmbariManagementController.class);

    expect(mockMgmtController.getStackServices(isA(Set.class))).andReturn(Collections.<StackServiceResponse>emptySet());

    Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();
    Map<String, String> typeProps = new HashMap<String, String>();
    typeProps.put("yarn.resourcemanager.hostname", "localhost");
    typeProps.put("yarn.timeline-service.address", "localhost");
    properties.put("yarn-site", typeProps);

    Collection<String> hgComponents = new HashSet<String>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("RESOURCEMANAGER");
    HostGroup group1 = new TestHostGroup("group1", Collections.singleton("testhost"), hgComponents);

    Collection<String> hgComponents2 = new HashSet<String>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    HostGroup group2 = new TestHostGroup("group2", Collections.singleton("testhost2"), hgComponents2);

    Map<String, HostGroup> hostGroups = new HashMap<String, HostGroup>();
    hostGroups.put(group1.getName(), group1);
    hostGroups.put(group2.getName(), group2);

    mockSupport.replayAll();

    Stack testStackDefinition = new Stack("HDP", "2.1", mockMgmtController) {
      @Override
      public Cardinality getCardinality(String component) {
        // simulate a stack that required the APP_TIMELINE_SERVER
        if (component.equals("APP_TIMELINE_SERVER")) {
          return new Cardinality("1");
        }

        return null;
      }
    };

    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(properties);

    try {
      updater.doUpdateForClusterCreate(hostGroups, testStackDefinition);
      fail("IllegalArgumentException should have been thrown");
    } catch (IllegalArgumentException illegalArgumentException) {
      // expected exception
    }

    mockSupport.verifyAll();
  }

  @Test
  public void testDoUpdateForClusterCreate_SingleHostProperty__MultipleMatchingHostGroupsError() throws Exception {
    EasyMockSupport mockSupport = new EasyMockSupport();


    AmbariManagementController mockMgmtController =
      mockSupport.createMock(AmbariManagementController.class);

    expect(mockMgmtController.getStackServices(isA(Set.class))).andReturn(Collections.<StackServiceResponse>emptySet());

    Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();
    Map<String, String> typeProps = new HashMap<String, String>();
    typeProps.put("yarn.resourcemanager.hostname", "localhost");
    typeProps.put("yarn.timeline-service.address", "localhost");
    properties.put("yarn-site", typeProps);

    Collection<String> hgComponents = new HashSet<String>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("RESOURCEMANAGER");
    hgComponents.add("APP_TIMELINE_SERVER");
    HostGroup group1 = new TestHostGroup("group1", Collections.singleton("testhost"), hgComponents);

    Collection<String> hgComponents2 = new HashSet<String>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    hgComponents2.add("APP_TIMELINE_SERVER");
    HostGroup group2 = new TestHostGroup("group2", Collections.singleton("testhost2"), hgComponents2);

    Map<String, HostGroup> hostGroups = new HashMap<String, HostGroup>();
    hostGroups.put(group1.getName(), group1);
    hostGroups.put(group2.getName(), group2);

    mockSupport.replayAll();

    Stack testStackDefinition = new Stack("HDP", "2.1", mockMgmtController) {
      @Override
      public Cardinality getCardinality(String component) {
        // simulate a stack that required the APP_TIMELINE_SERVER
        if (component.equals("APP_TIMELINE_SERVER")) {
          return new Cardinality("0-1");
        }

        return null;
      }
    };

    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(properties);

    try {
      updater.doUpdateForClusterCreate(hostGroups, testStackDefinition);
      fail("IllegalArgumentException should have been thrown");
    } catch (IllegalArgumentException illegalArgumentException) {
      // expected exception
    }

    mockSupport.verifyAll();
  }

  @Test
  public void testDoUpdateForClusterCreate_SingleHostProperty__MissingOptionalComponent() throws Exception {
    final String expectedHostName = "localhost";

    EasyMockSupport mockSupport = new EasyMockSupport();

    AmbariManagementController mockMgmtController =
      mockSupport.createMock(AmbariManagementController.class);

    expect(mockMgmtController.getStackServices(isA(Set.class))).andReturn(Collections.<StackServiceResponse>emptySet());

    Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();
    Map<String, String> typeProps = new HashMap<String, String>();
    typeProps.put("yarn.timeline-service.address", expectedHostName);
    properties.put("yarn-site", typeProps);

    Collection<String> hgComponents = new HashSet<String>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("RESOURCEMANAGER");
    HostGroup group1 = new TestHostGroup("group1", Collections.singleton("testhost"), hgComponents);

    Collection<String> hgComponents2 = new HashSet<String>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    HostGroup group2 = new TestHostGroup("group2", Collections.singleton("testhost2"), hgComponents2);

    Map<String, HostGroup> hostGroups = new HashMap<String, HostGroup>();
    hostGroups.put(group1.getName(), group1);
    hostGroups.put(group2.getName(), group2);

    mockSupport.replayAll();

    Stack testStackDefinition = new Stack("HDP", "2.1", mockMgmtController) {
      @Override
      public Cardinality getCardinality(String component) {
        // simulate a stack that supports 0 or 1 instances of the APP_TIMELINE_SERVER
        if (component.equals("APP_TIMELINE_SERVER")) {
          return new Cardinality("0-1");
        }

        return null;
      }
    };

    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(properties);

    Map<String, Map<String, String>> updatedProperties = updater.doUpdateForClusterCreate(hostGroups, testStackDefinition);
    String updatedVal = updatedProperties.get("yarn-site").get("yarn.timeline-service.address");
    assertEquals("Timeline Server config property should not have been updated",
      expectedHostName, updatedVal);

    mockSupport.verifyAll();
  }

  @Test
  public void testDoUpdateForClusterCreate_SingleHostProperty__defaultValue__WithPort() {
    Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();
    Map<String, String> typeProps = new HashMap<String, String>();
    typeProps.put("fs.defaultFS", "localhost:5050");
    properties.put("core-site", typeProps);

    Collection<String> hgComponents = new HashSet<String>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("RESOURCEMANAGER");
    HostGroup group1 = new TestHostGroup("group1", Collections.singleton("testhost"), hgComponents);

    Collection<String> hgComponents2 = new HashSet<String>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    HostGroup group2 = new TestHostGroup("group2", Collections.singleton("testhost2"), hgComponents2);

    Map<String, HostGroup> hostGroups = new HashMap<String, HostGroup>();
    hostGroups.put(group1.getName(), group1);
    hostGroups.put(group2.getName(), group2);

    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(properties);
    Map<String, Map<String, String>> updatedProperties = updater.doUpdateForClusterCreate(hostGroups, null);
    String updatedVal = updatedProperties.get("core-site").get("fs.defaultFS");
    assertEquals("testhost:5050", updatedVal);
  }

  @Test
  public void testDoUpdateForClusterCreate_MultiHostProperty__defaultValues() {
    Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();
    Map<String, String> typeProps = new HashMap<String, String>();
    typeProps.put("hbase.zookeeper.quorum", "localhost");
    properties.put("hbase-site", typeProps);

    Collection<String> hgComponents = new HashSet<String>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("ZOOKEEPER_SERVER");
    HostGroup group1 = new TestHostGroup("group1", Collections.singleton("testhost"), hgComponents);

    Collection<String> hgComponents2 = new HashSet<String>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    hgComponents2.add("ZOOKEEPER_SERVER");
    Set<String> hosts2 = new HashSet<String>();
    hosts2.add("testhost2");
    hosts2.add("testhost2a");
    hosts2.add("testhost2b");
    HostGroup group2 = new TestHostGroup("group2", hosts2, hgComponents2);

    Collection<String> hgComponents3 = new HashSet<String>();
    hgComponents2.add("HDFS_CLIENT");
    hgComponents2.add("ZOOKEEPER_CLIENT");
    Set<String> hosts3 = new HashSet<String>();
    hosts3.add("testhost3");
    hosts3.add("testhost3a");
    HostGroup group3 = new TestHostGroup("group3", hosts3, hgComponents3);

    Map<String, HostGroup> hostGroups = new HashMap<String, HostGroup>();
    hostGroups.put(group1.getName(), group1);
    hostGroups.put(group2.getName(), group2);
    hostGroups.put(group3.getName(), group3);

    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(properties);
    Map<String, Map<String, String>> updatedProperties = updater.doUpdateForClusterCreate(hostGroups, null);
    String updatedVal = updatedProperties.get("hbase-site").get("hbase.zookeeper.quorum");
    String[] hosts = updatedVal.split(",");

    Collection<String> expectedHosts = new HashSet<String>();
    expectedHosts.add("testhost");
    expectedHosts.add("testhost2");
    expectedHosts.add("testhost2a");
    expectedHosts.add("testhost2b");

    assertEquals(4, hosts.length);
    for (String host : hosts) {
      assertTrue(expectedHosts.contains(host));
      expectedHosts.remove(host);
    }
  }

  @Test
  public void testDoUpdateForClusterCreate_MultiHostProperty__defaultValues___withPorts() {
    Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();
    Map<String, String> typeProps = new HashMap<String, String>();
    typeProps.put("templeton.zookeeper.hosts", "localhost:9090");
    properties.put("webhcat-site", typeProps);

    Collection<String> hgComponents = new HashSet<String>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("ZOOKEEPER_SERVER");
    HostGroup group1 = new TestHostGroup("group1", Collections.singleton("testhost"), hgComponents);

    Collection<String> hgComponents2 = new HashSet<String>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    hgComponents2.add("ZOOKEEPER_SERVER");
    Set<String> hosts2 = new HashSet<String>();
    hosts2.add("testhost2");
    hosts2.add("testhost2a");
    hosts2.add("testhost2b");
    HostGroup group2 = new TestHostGroup("group2", hosts2, hgComponents2);

    Collection<String> hgComponents3 = new HashSet<String>();
    hgComponents2.add("HDFS_CLIENT");
    hgComponents2.add("ZOOKEEPER_CLIENT");
    Set<String> hosts3 = new HashSet<String>();
    hosts3.add("testhost3");
    hosts3.add("testhost3a");
    HostGroup group3 = new TestHostGroup("group3", hosts3, hgComponents3);

    Map<String, HostGroup> hostGroups = new HashMap<String, HostGroup>();
    hostGroups.put(group1.getName(), group1);
    hostGroups.put(group2.getName(), group2);
    hostGroups.put(group3.getName(), group3);

    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(properties);
    Map<String, Map<String, String>> updatedProperties = updater.doUpdateForClusterCreate(hostGroups, null);
    String updatedVal = updatedProperties.get("webhcat-site").get("templeton.zookeeper.hosts");
    String[] hosts = updatedVal.split(",");

    Collection<String> expectedHosts = new HashSet<String>();
    expectedHosts.add("testhost:9090");
    expectedHosts.add("testhost2:9090");
    expectedHosts.add("testhost2a:9090");
    expectedHosts.add("testhost2b:9090");

    assertEquals(4, hosts.length);
    for (String host : hosts) {
      assertTrue(expectedHosts.contains(host));
      expectedHosts.remove(host);
    }
  }

  @Test
  public void testDoUpdateForClusterCreate_MultiHostProperty__defaultValues___YAML() {
    Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();
    Map<String, String> typeProps = new HashMap<String, String>();
    typeProps.put("storm.zookeeper.servers", "['localhost']");
    properties.put("storm-site", typeProps);

    Collection<String> hgComponents = new HashSet<String>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("ZOOKEEPER_SERVER");
    HostGroup group1 = new TestHostGroup("group1", Collections.singleton("testhost"), hgComponents);

    Collection<String> hgComponents2 = new HashSet<String>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    hgComponents2.add("ZOOKEEPER_SERVER");
    Set<String> hosts2 = new HashSet<String>();
    hosts2.add("testhost2");
    hosts2.add("testhost2a");
    hosts2.add("testhost2b");
    HostGroup group2 = new TestHostGroup("group2", hosts2, hgComponents2);

    Collection<String> hgComponents3 = new HashSet<String>();
    hgComponents2.add("HDFS_CLIENT");
    hgComponents2.add("ZOOKEEPER_CLIENT");
    Set<String> hosts3 = new HashSet<String>();
    hosts3.add("testhost3");
    hosts3.add("testhost3a");
    HostGroup group3 = new TestHostGroup("group3", hosts3, hgComponents3);

    Map<String, HostGroup> hostGroups = new HashMap<String, HostGroup>();
    hostGroups.put(group1.getName(), group1);
    hostGroups.put(group2.getName(), group2);
    hostGroups.put(group3.getName(), group3);

    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(properties);
    Map<String, Map<String, String>> updatedProperties = updater.doUpdateForClusterCreate(hostGroups, null);
    String updatedVal = updatedProperties.get("storm-site").get("storm.zookeeper.servers");
    assertTrue(updatedVal.startsWith("["));
    assertTrue(updatedVal.endsWith("]"));
    // remove the surrounding brackets
    updatedVal = updatedVal.replaceAll("[\\[\\]]", "");

    String[] hosts = updatedVal.split(",");

    Collection<String> expectedHosts = new HashSet<String>();
    expectedHosts.add("'testhost'");
    expectedHosts.add("'testhost2'");
    expectedHosts.add("'testhost2a'");
    expectedHosts.add("'testhost2b'");

    assertEquals(4, hosts.length);
    for (String host : hosts) {
      assertTrue(expectedHosts.contains(host));
      expectedHosts.remove(host);
    }
  }

  @Test
  public void testDoUpdateForClusterCreate_MProperty__defaultValues() {
    Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();
    Map<String, String> typeProps = new HashMap<String, String>();
    typeProps.put("hbase_master_heapsize", "512m");
    properties.put("hbase-env", typeProps);

    Collection<String> hgComponents = new HashSet<String>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("RESOURCEMANAGER");
    HostGroup group1 = new TestHostGroup("group1", Collections.singleton("testhost"), hgComponents);

    Collection<String> hgComponents2 = new HashSet<String>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    HostGroup group2 = new TestHostGroup("group2", Collections.singleton("testhost2"), hgComponents2);

    Map<String, HostGroup> hostGroups = new HashMap<String, HostGroup>();
    hostGroups.put(group1.getName(), group1);
    hostGroups.put(group2.getName(), group2);

    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(properties);
    Map<String, Map<String, String>> updatedProperties = updater.doUpdateForClusterCreate(hostGroups, null);
    String updatedVal = updatedProperties.get("hbase-env").get("hbase_master_heapsize");
    assertEquals("512m", updatedVal);
  }

  @Test
  public void testDoUpdateForClusterCreate_MProperty__missingM() {
    Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();
    Map<String, String> typeProps = new HashMap<String, String>();
    typeProps.put("hbase_master_heapsize", "512");
    properties.put("hbase-env", typeProps);

    Collection<String> hgComponents = new HashSet<String>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("RESOURCEMANAGER");
    HostGroup group1 = new TestHostGroup("group1", Collections.singleton("testhost"), hgComponents);

    Collection<String> hgComponents2 = new HashSet<String>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    HostGroup group2 = new TestHostGroup("group2", Collections.singleton("testhost2"), hgComponents2);

    Map<String, HostGroup> hostGroups = new HashMap<String, HostGroup>();
    hostGroups.put(group1.getName(), group1);
    hostGroups.put(group2.getName(), group2);

    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(properties);
    Map<String, Map<String, String>> updatedProperties = updater.doUpdateForClusterCreate(hostGroups, null);
    String updatedVal = updatedProperties.get("hbase-env").get("hbase_master_heapsize");
    assertEquals("512m", updatedVal);
  }

  @Test
  public void testDoUpdateForClusterCreate_SingleHostProperty__exportedValue() {
    Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();
    Map<String, String> typeProps = new HashMap<String, String>();
    typeProps.put("yarn.resourcemanager.hostname", "%HOSTGROUP::group1%");
    properties.put("yarn-site", typeProps);

    Collection<String> hgComponents = new HashSet<String>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("RESOURCEMANAGER");
    HostGroup group1 = new TestHostGroup("group1", Collections.singleton("testhost"), hgComponents);

    Collection<String> hgComponents2 = new HashSet<String>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    HostGroup group2 = new TestHostGroup("group2", Collections.singleton("testhost2"), hgComponents2);

    Map<String, HostGroup> hostGroups = new HashMap<String, HostGroup>();
    hostGroups.put(group1.getName(), group1);
    hostGroups.put(group2.getName(), group2);

    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(properties);
    Map<String, Map<String, String>> updatedProperties = updater.doUpdateForClusterCreate(hostGroups, null);
    String updatedVal = updatedProperties.get("yarn-site").get("yarn.resourcemanager.hostname");
    assertEquals("testhost", updatedVal);
  }

  @Test
  public void testDoUpdateForClusterCreate_SingleHostProperty__exportedValue__WithPort() {
    Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();
    Map<String, String> typeProps = new HashMap<String, String>();
    typeProps.put("fs.defaultFS", "%HOSTGROUP::group1%:5050");
    properties.put("core-site", typeProps);

    Collection<String> hgComponents = new HashSet<String>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("RESOURCEMANAGER");
    HostGroup group1 = new TestHostGroup("group1", Collections.singleton("testhost"), hgComponents);

    Collection<String> hgComponents2 = new HashSet<String>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    HostGroup group2 = new TestHostGroup("group2", Collections.singleton("testhost2"), hgComponents2);

    Map<String, HostGroup> hostGroups = new HashMap<String, HostGroup>();
    hostGroups.put(group1.getName(), group1);
    hostGroups.put(group2.getName(), group2);

    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(properties);
    Map<String, Map<String, String>> updatedProperties = updater.doUpdateForClusterCreate(hostGroups, null);
    String updatedVal = updatedProperties.get("core-site").get("fs.defaultFS");
    assertEquals("testhost:5050", updatedVal);
  }

  @Test
  public void testDoUpdateForClusterCreate_MultiHostProperty__exportedValues() {
    Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();
    Map<String, String> typeProps = new HashMap<String, String>();
    typeProps.put("hbase.zookeeper.quorum", "%HOSTGROUP::group1%,%HOSTGROUP::group2%");
    properties.put("hbase-site", typeProps);

    Collection<String> hgComponents = new HashSet<String>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("ZOOKEEPER_SERVER");
    HostGroup group1 = new TestHostGroup("group1", Collections.singleton("testhost"), hgComponents);

    Collection<String> hgComponents2 = new HashSet<String>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    hgComponents2.add("ZOOKEEPER_SERVER");
    Set<String> hosts2 = new HashSet<String>();
    hosts2.add("testhost2");
    hosts2.add("testhost2a");
    hosts2.add("testhost2b");
    HostGroup group2 = new TestHostGroup("group2", hosts2, hgComponents2);

    Collection<String> hgComponents3 = new HashSet<String>();
    hgComponents2.add("HDFS_CLIENT");
    hgComponents2.add("ZOOKEEPER_CLIENT");
    Set<String> hosts3 = new HashSet<String>();
    hosts3.add("testhost3");
    hosts3.add("testhost3a");
    HostGroup group3 = new TestHostGroup("group3", hosts3, hgComponents3);

    Map<String, HostGroup> hostGroups = new HashMap<String, HostGroup>();
    hostGroups.put(group1.getName(), group1);
    hostGroups.put(group2.getName(), group2);
    hostGroups.put(group3.getName(), group3);

    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(properties);
    Map<String, Map<String, String>> updatedProperties = updater.doUpdateForClusterCreate(hostGroups, null);
    String updatedVal = updatedProperties.get("hbase-site").get("hbase.zookeeper.quorum");
    String[] hosts = updatedVal.split(",");

    Collection<String> expectedHosts = new HashSet<String>();
    expectedHosts.add("testhost");
    expectedHosts.add("testhost2");
    expectedHosts.add("testhost2a");
    expectedHosts.add("testhost2b");

    assertEquals(4, hosts.length);
    for (String host : hosts) {
      assertTrue(expectedHosts.contains(host));
      expectedHosts.remove(host);
    }
  }

  @Test
  public void testDoUpdateForClusterCreate_MultiHostProperty__exportedValues___withPorts() {
    Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();
    Map<String, String> typeProps = new HashMap<String, String>();
    typeProps.put("templeton.zookeeper.hosts", "%HOSTGROUP::group1%:9090,%HOSTGROUP::group2%:9091");
    properties.put("webhcat-site", typeProps);

    Collection<String> hgComponents = new HashSet<String>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("ZOOKEEPER_SERVER");
    HostGroup group1 = new TestHostGroup("group1", Collections.singleton("testhost"), hgComponents);

    Collection<String> hgComponents2 = new HashSet<String>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    hgComponents2.add("ZOOKEEPER_SERVER");
    Set<String> hosts2 = new HashSet<String>();
    hosts2.add("testhost2");
    hosts2.add("testhost2a");
    hosts2.add("testhost2b");
    HostGroup group2 = new TestHostGroup("group2", hosts2, hgComponents2);

    Collection<String> hgComponents3 = new HashSet<String>();
    hgComponents2.add("HDFS_CLIENT");
    hgComponents2.add("ZOOKEEPER_CLIENT");
    Set<String> hosts3 = new HashSet<String>();
    hosts3.add("testhost3");
    hosts3.add("testhost3a");
    HostGroup group3 = new TestHostGroup("group3", hosts3, hgComponents3);

    Map<String, HostGroup> hostGroups = new HashMap<String, HostGroup>();
    hostGroups.put(group1.getName(), group1);
    hostGroups.put(group2.getName(), group2);
    hostGroups.put(group3.getName(), group3);

    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(properties);
    Map<String, Map<String, String>> updatedProperties = updater.doUpdateForClusterCreate(hostGroups, null);
    String updatedVal = updatedProperties.get("webhcat-site").get("templeton.zookeeper.hosts");
    String[] hosts = updatedVal.split(",");

    Collection<String> expectedHosts = new HashSet<String>();
    expectedHosts.add("testhost:9090");
    expectedHosts.add("testhost2:9091");
    expectedHosts.add("testhost2a:9091");
    expectedHosts.add("testhost2b:9091");

    assertEquals(4, hosts.length);
    for (String host : hosts) {
      assertTrue(expectedHosts.contains(host));
      expectedHosts.remove(host);
    }
  }

  @Test
  public void testDoUpdateForClusterCreate_MultiHostProperty__exportedValues___YAML() {
    Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();
    Map<String, String> typeProps = new HashMap<String, String>();
    typeProps.put("storm.zookeeper.servers", "['%HOSTGROUP::group1%:9090','%HOSTGROUP::group2%:9091']");
    properties.put("storm-site", typeProps);

    Collection<String> hgComponents = new HashSet<String>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("ZOOKEEPER_SERVER");
    HostGroup group1 = new TestHostGroup("group1", Collections.singleton("testhost"), hgComponents);

    Collection<String> hgComponents2 = new HashSet<String>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    hgComponents2.add("ZOOKEEPER_SERVER");
    Set<String> hosts2 = new HashSet<String>();
    hosts2.add("testhost2");
    hosts2.add("testhost2a");
    hosts2.add("testhost2b");
    HostGroup group2 = new TestHostGroup("group2", hosts2, hgComponents2);

    Collection<String> hgComponents3 = new HashSet<String>();
    hgComponents2.add("HDFS_CLIENT");
    hgComponents2.add("ZOOKEEPER_CLIENT");
    Set<String> hosts3 = new HashSet<String>();
    hosts3.add("testhost3");
    hosts3.add("testhost3a");
    HostGroup group3 = new TestHostGroup("group3", hosts3, hgComponents3);

    Map<String, HostGroup> hostGroups = new HashMap<String, HostGroup>();
    hostGroups.put(group1.getName(), group1);
    hostGroups.put(group2.getName(), group2);
    hostGroups.put(group3.getName(), group3);

    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(properties);
    Map<String, Map<String, String>> updatedProperties = updater.doUpdateForClusterCreate(hostGroups, null);
    String updatedVal = updatedProperties.get("storm-site").get("storm.zookeeper.servers");
    assertTrue(updatedVal.startsWith("["));
    assertTrue(updatedVal.endsWith("]"));
    // remove the surrounding brackets
    updatedVal = updatedVal.replaceAll("[\\[\\]]", "");

    String[] hosts = updatedVal.split(",");

    Collection<String> expectedHosts = new HashSet<String>();
    expectedHosts.add("'testhost:9090'");
    expectedHosts.add("'testhost2:9091'");
    expectedHosts.add("'testhost2a:9091'");
    expectedHosts.add("'testhost2b:9091'");

    assertEquals(4, hosts.length);
    for (String host : hosts) {
      assertTrue(expectedHosts.contains(host));
      expectedHosts.remove(host);
    }
  }

  @Test
  public void testDoUpdateForClusterCreate_DBHostProperty__defaultValue() {
    Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();
    Map<String, String> hiveSiteProps = new HashMap<String, String>();
    hiveSiteProps.put("javax.jdo.option.ConnectionURL", "jdbc:mysql://localhost/hive?createDatabaseIfNotExist=true");
    Map<String, String> hiveEnvProps = new HashMap<String, String>();
    hiveEnvProps.put("hive_database", "New MySQL Database");
    properties.put("hive-site", hiveSiteProps);
    properties.put("hive-env", hiveEnvProps);

    Collection<String> hgComponents = new HashSet<String>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("RESOURCEMANAGER");
    hgComponents.add("MYSQL_SERVER");
    HostGroup group1 = new TestHostGroup("group1", Collections.singleton("testhost"), hgComponents);

    Collection<String> hgComponents2 = new HashSet<String>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    HostGroup group2 = new TestHostGroup("group2", Collections.singleton("testhost2"), hgComponents2);

    Map<String, HostGroup> hostGroups = new HashMap<String, HostGroup>();
    hostGroups.put(group1.getName(), group1);
    hostGroups.put(group2.getName(), group2);

    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(properties);
    Map<String, Map<String, String>> updatedProperties = updater.doUpdateForClusterCreate(hostGroups, null);
    String updatedVal = updatedProperties.get("hive-site").get("javax.jdo.option.ConnectionURL");
    assertEquals("jdbc:mysql://testhost/hive?createDatabaseIfNotExist=true", updatedVal);
  }

  @Test
  public void testDoUpdateForClusterCreate_DBHostProperty__exportedValue() {
    Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();
    Map<String, String> hiveSiteProps = new HashMap<String, String>();
    hiveSiteProps.put("javax.jdo.option.ConnectionURL", "jdbc:mysql://%HOSTGROUP::group1%/hive?createDatabaseIfNotExist=true");
    Map<String, String> hiveEnvProps = new HashMap<String, String>();
    hiveEnvProps.put("hive_database", "New MySQL Database");
    properties.put("hive-site", hiveSiteProps);
    properties.put("hive-env", hiveEnvProps);

    Collection<String> hgComponents = new HashSet<String>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("RESOURCEMANAGER");
    hgComponents.add("MYSQL_SERVER");
    HostGroup group1 = new TestHostGroup("group1", Collections.singleton("testhost"), hgComponents);

    Collection<String> hgComponents2 = new HashSet<String>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    HostGroup group2 = new TestHostGroup("group2", Collections.singleton("testhost2"), hgComponents2);

    Map<String, HostGroup> hostGroups = new HashMap<String, HostGroup>();
    hostGroups.put(group1.getName(), group1);
    hostGroups.put(group2.getName(), group2);

    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(properties);
    Map<String, Map<String, String>> updatedProperties = updater.doUpdateForClusterCreate(hostGroups, null);
    String updatedVal = updatedProperties.get("hive-site").get("javax.jdo.option.ConnectionURL");
    assertEquals("jdbc:mysql://testhost/hive?createDatabaseIfNotExist=true", updatedVal);
  }

  @Test
  public void testDoUpdateForClusterCreate_DBHostProperty__external() {
    Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();
    Map<String, String> typeProps = new HashMap<String, String>();
    typeProps.put("javax.jdo.option.ConnectionURL", "jdbc:mysql://myHost.com/hive?createDatabaseIfNotExist=true");
    typeProps.put("hive_database", "Existing MySQL Database");
    properties.put("hive-env", typeProps);

    Collection<String> hgComponents = new HashSet<String>();
    hgComponents.add("NAMENODE");
    hgComponents.add("SECONDARY_NAMENODE");
    hgComponents.add("RESOURCEMANAGER");
    HostGroup group1 = new TestHostGroup("group1", Collections.singleton("testhost"), hgComponents);

    Collection<String> hgComponents2 = new HashSet<String>();
    hgComponents2.add("DATANODE");
    hgComponents2.add("HDFS_CLIENT");
    HostGroup group2 = new TestHostGroup("group2", Collections.singleton("testhost2"), hgComponents2);

    Map<String, HostGroup> hostGroups = new HashMap<String, HostGroup>();
    hostGroups.put(group1.getName(), group1);
    hostGroups.put(group2.getName(), group2);

    BlueprintConfigurationProcessor updater = new BlueprintConfigurationProcessor(properties);
    Map<String, Map<String, String>> updatedProperties = updater.doUpdateForClusterCreate(hostGroups, null);
    String updatedVal = updatedProperties.get("hive-env").get("javax.jdo.option.ConnectionURL");
    assertEquals("jdbc:mysql://myHost.com/hive?createDatabaseIfNotExist=true", updatedVal);
  }

  @Test
  public void testFalconConfigExport() throws Exception {
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedPortNum = "808080";
    final String expectedHostGroupName = "host_group_1";

    EasyMockSupport mockSupport = new EasyMockSupport();

    HostGroup mockHostGroupOne = mockSupport.createMock(HostGroup.class);

    expect(mockHostGroupOne.getHostInfo()).andReturn(Arrays.asList(expectedHostName, "serverTwo")).atLeastOnce();
    expect(mockHostGroupOne.getName()).andReturn(expectedHostGroupName).atLeastOnce();

    mockSupport.replayAll();

    Map<String, Map<String, String>> configProperties =
      new HashMap<String, Map<String, String>>();

    Map<String, String> falconStartupProperties =
      new HashMap<String, String>();

    configProperties.put("falcon-startup.properties", falconStartupProperties);

    // setup properties that include host information
    falconStartupProperties.put("*.broker.url", expectedHostName + ":" + expectedPortNum);
    falconStartupProperties.put("*.falcon.service.authentication.kerberos.principal", "falcon/" + expectedHostName + "@EXAMPLE.COM");
    falconStartupProperties.put("*.falcon.http.authentication.kerberos.principal", "HTTP/" + expectedHostName + "@EXAMPLE.COM");

    BlueprintConfigurationProcessor configProcessor =
      new BlueprintConfigurationProcessor(configProperties);

    // call top-level export method
    configProcessor.doUpdateForBlueprintExport(Arrays.asList(mockHostGroupOne));

    assertEquals("Falcon Broker URL property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), falconStartupProperties.get("*.broker.url"));

    assertEquals("Falcon Kerberos Principal property not properly exported",
      "falcon/" + "%HOSTGROUP::" + expectedHostGroupName + "%" + "@EXAMPLE.COM", falconStartupProperties.get("*.falcon.service.authentication.kerberos.principal"));

    assertEquals("Falcon Kerberos HTTP Principal property not properly exported",
      "HTTP/" + "%HOSTGROUP::" + expectedHostGroupName + "%" + "@EXAMPLE.COM", falconStartupProperties.get("*.falcon.http.authentication.kerberos.principal"));

    mockSupport.verifyAll();

  }

  @Test
  public void testFalconConfigClusterUpdate() throws Exception {
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedPortNum = "808080";
    final String expectedHostGroupName = "host_group_1";

    EasyMockSupport mockSupport = new EasyMockSupport();

    HostGroup mockHostGroupOne = mockSupport.createMock(HostGroup.class);

    expect(mockHostGroupOne.getHostInfo()).andReturn(Arrays.asList(expectedHostName, "serverTwo")).atLeastOnce();

    mockSupport.replayAll();

    Map<String, Map<String, String>> configProperties =
      new HashMap<String, Map<String, String>>();

    Map<String, String> falconStartupProperties =
      new HashMap<String, String>();

    configProperties.put("falcon-startup.properties", falconStartupProperties);

    // setup properties that include host information
    falconStartupProperties.put("*.broker.url", createExportedAddress(expectedPortNum, expectedHostGroupName));
    falconStartupProperties.put("*.falcon.service.authentication.kerberos.principal", "falcon/" + createExportedHostName(expectedHostGroupName) + "@EXAMPLE.COM");
    falconStartupProperties.put("*.falcon.http.authentication.kerberos.principal", "HTTP/" + createExportedHostName(expectedHostGroupName) + "@EXAMPLE.COM");

    BlueprintConfigurationProcessor configProcessor =
      new BlueprintConfigurationProcessor(configProperties);

    Map<String, HostGroup> mapOfHostGroups =
      new HashMap<String, HostGroup>();
    mapOfHostGroups.put(expectedHostGroupName, mockHostGroupOne);

    // call top-level export method
    configProcessor.doUpdateForClusterCreate(mapOfHostGroups, null);

    assertEquals("Falcon Broker URL property not properly exported",
      expectedHostName + ":" + expectedPortNum, falconStartupProperties.get("*.broker.url"));

    assertEquals("Falcon Kerberos Principal property not properly exported",
      "falcon/" + expectedHostName + "@EXAMPLE.COM", falconStartupProperties.get("*.falcon.service.authentication.kerberos.principal"));

    assertEquals("Falcon Kerberos HTTP Principal property not properly exported",
      "HTTP/" + expectedHostName + "@EXAMPLE.COM", falconStartupProperties.get("*.falcon.http.authentication.kerberos.principal"));

    mockSupport.verifyAll();

  }

  @Test
  public void testFalconConfigClusterUpdateDefaultConfig() throws Exception {
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedPortNum = "808080";
    final String expectedHostGroupName = "host_group_1";

    EasyMockSupport mockSupport = new EasyMockSupport();

    HostGroup mockHostGroupOne = mockSupport.createMock(HostGroup.class);
    expect(mockHostGroupOne.getComponents()).andReturn(Arrays.asList("FALCON_SERVER")).atLeastOnce();
    expect(mockHostGroupOne.getHostInfo()).andReturn(Arrays.asList(expectedHostName, "serverTwo")).atLeastOnce();

    mockSupport.replayAll();

    Map<String, Map<String, String>> configProperties =
      new HashMap<String, Map<String, String>>();

    Map<String, String> falconStartupProperties =
      new HashMap<String, String>();

    configProperties.put("falcon-startup.properties", falconStartupProperties);

    // setup properties that include host information
    falconStartupProperties.put("*.broker.url", "localhost:" + expectedPortNum);
    falconStartupProperties.put("*.falcon.service.authentication.kerberos.principal", "falcon/" + "localhost" + "@EXAMPLE.COM");
    falconStartupProperties.put("*.falcon.http.authentication.kerberos.principal", "HTTP/" + "localhost" + "@EXAMPLE.COM");

    BlueprintConfigurationProcessor configProcessor =
      new BlueprintConfigurationProcessor(configProperties);

    Map<String, HostGroup> mapOfHostGroups =
      new HashMap<String, HostGroup>();
    mapOfHostGroups.put(expectedHostGroupName, mockHostGroupOne);

    // call top-level export method
    configProcessor.doUpdateForClusterCreate(mapOfHostGroups, null);

    assertEquals("Falcon Broker URL property not properly exported",
      expectedHostName + ":" + expectedPortNum, falconStartupProperties.get("*.broker.url"));

    assertEquals("Falcon Kerberos Principal property not properly exported",
      "falcon/" + expectedHostName + "@EXAMPLE.COM", falconStartupProperties.get("*.falcon.service.authentication.kerberos.principal"));

    assertEquals("Falcon Kerberos HTTP Principal property not properly exported",
      "HTTP/" + expectedHostName + "@EXAMPLE.COM", falconStartupProperties.get("*.falcon.http.authentication.kerberos.principal"));

    mockSupport.verifyAll();

  }

  @Test
  public void testDoUpdateForClusterWithNameNodeHAEnabled() throws Exception {
    final String expectedNameService = "mynameservice";
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedPortNum = "808080";
    final String expectedNodeOne = "nn1";
    final String expectedNodeTwo = "nn2";
    final String expectedHostGroupName = "host_group_1";

    EasyMockSupport mockSupport = new EasyMockSupport();

    HostGroup mockHostGroupOne = mockSupport.createMock(HostGroup.class);

    expect(mockHostGroupOne.getHostInfo()).andReturn(Arrays.asList(expectedHostName, "serverTwo")).atLeastOnce();

    mockSupport.replayAll();

    Map<String, Map<String, String>> configProperties =
      new HashMap<String, Map<String, String>>();

    Map<String, String> hdfsSiteProperties =
      new HashMap<String, String>();

    configProperties.put("hdfs-site", hdfsSiteProperties);

    // setup hdfs HA config for test
    hdfsSiteProperties.put("dfs.nameservices", expectedNameService);
    hdfsSiteProperties.put("dfs.ha.namenodes.mynameservice", expectedNodeOne + ", " + expectedNodeTwo);


    // setup properties that include exported host group information
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeOne, createExportedAddress(expectedPortNum, expectedHostGroupName));
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeTwo, createExportedAddress(expectedPortNum, expectedHostGroupName));


    BlueprintConfigurationProcessor configProcessor =
      new BlueprintConfigurationProcessor(configProperties);

    Map<String, HostGroup> mapOfHostGroups = new HashMap<String, HostGroup>();
    mapOfHostGroups.put(expectedHostGroupName,mockHostGroupOne);

    configProcessor.doUpdateForClusterCreate(mapOfHostGroups, null);

    // verify that the expected hostname was substitued for the host group name in the config
    assertEquals("HTTPS address HA property not properly exported",
      expectedHostName + ":" + expectedPortNum, hdfsSiteProperties.get("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeOne));
    assertEquals("HTTPS address HA property not properly exported",
      expectedHostName + ":" + expectedPortNum, hdfsSiteProperties.get("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeTwo));

    assertEquals("HTTPS address HA property not properly exported",
      expectedHostName + ":" + expectedPortNum, hdfsSiteProperties.get("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeOne));
    assertEquals("HTTPS address HA property not properly exported",
      expectedHostName + ":" + expectedPortNum, hdfsSiteProperties.get("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeTwo));

    assertEquals("HTTPS address HA property not properly exported",
      expectedHostName + ":" + expectedPortNum, hdfsSiteProperties.get("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeOne));
    assertEquals("HTTPS address HA property not properly exported",
      expectedHostName + ":" + expectedPortNum, hdfsSiteProperties.get("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeTwo));

    mockSupport.verifyAll();
  }

  @Test
  public void testDoNameNodeHighAvailabilityUpdateWithHAEnabled() throws Exception {
    final String expectedNameService = "mynameservice";
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedPortNum = "808080";
    final String expectedNodeOne = "nn1";
    final String expectedNodeTwo = "nn2";
    final String expectedHostGroupName = "host_group_1";

    EasyMockSupport mockSupport = new EasyMockSupport();

    HostGroup mockHostGroupOne = mockSupport.createMock(HostGroup.class);

    expect(mockHostGroupOne.getHostInfo()).andReturn(Arrays.asList(expectedHostName, "serverTwo")).atLeastOnce();
    expect(mockHostGroupOne.getName()).andReturn(expectedHostGroupName).atLeastOnce();

    mockSupport.replayAll();

    Map<String, Map<String, String>> configProperties =
      new HashMap<String, Map<String, String>>();

    Map<String, String> hdfsSiteProperties =
      new HashMap<String, String>();
    Map<String, String> coreSiteProperties =
      new HashMap<String, String>();
    Map<String, String> hbaseSiteProperties =
      new HashMap<String, String>();


    configProperties.put("hdfs-site", hdfsSiteProperties);
    configProperties.put("core-site", coreSiteProperties);
    configProperties.put("hbase-site", hbaseSiteProperties);

    // setup hdfs config for test

    hdfsSiteProperties.put("dfs.nameservices", expectedNameService);
    hdfsSiteProperties.put("dfs.ha.namenodes.mynameservice", expectedNodeOne + ", " + expectedNodeTwo);


    // setup properties that include host information
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeOne, expectedHostName + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeTwo, expectedHostName + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeOne, expectedHostName + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeTwo, expectedHostName + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeOne, expectedHostName + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeTwo, expectedHostName + ":" + expectedPortNum);

    BlueprintConfigurationProcessor configProcessor =
      new BlueprintConfigurationProcessor(configProperties);

    // call top-level export method, which will call the HA-specific method if HA is enabled
    configProcessor.doUpdateForBlueprintExport(Arrays.asList(mockHostGroupOne));

    assertEquals("HTTPS address HA property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeOne));
    assertEquals("HTTPS address HA property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.https-address." + expectedNameService + "." + expectedNodeTwo));

    assertEquals("HTTPS address HA property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeOne));
    assertEquals("HTTPS address HA property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.http-address." + expectedNameService + "." + expectedNodeTwo));

    assertEquals("HTTPS address HA property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeOne));
    assertEquals("HTTPS address HA property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.rpc-address." + expectedNameService + "." + expectedNodeTwo));

    mockSupport.verifyAll();

  }

  @Test
  public void testDoNameNodeHighAvailabilityUpdateWithHAEnabledNameServicePropertiesIncluded() throws Exception {
    final String expectedNameService = "mynameservice";
    final String expectedHostName = "c6401.apache.ambari.org";

    EasyMockSupport mockSupport = new EasyMockSupport();

    HostGroup mockHostGroupOne = mockSupport.createMock(HostGroup.class);

    expect(mockHostGroupOne.getHostInfo()).andReturn(Arrays.asList(expectedHostName, "serverTwo")).atLeastOnce();

    mockSupport.replayAll();

    Map<String, Map<String, String>> configProperties =
      new HashMap<String, Map<String, String>>();

    Map<String, String> coreSiteProperties =
      new HashMap<String, String>();
    Map<String, String> hbaseSiteProperties =
      new HashMap<String, String>();


    configProperties.put("core-site", coreSiteProperties);
    configProperties.put("hbase-site", hbaseSiteProperties);

    // configure fs.defaultFS to include a nameservice name, rather than a host name
    coreSiteProperties.put("fs.defaultFS", "hdfs://" + expectedNameService);
    // configure hbase.rootdir to include a nameservice name, rather than a host name
    hbaseSiteProperties.put("hbase.rootdir", "hdfs://" + expectedNameService + "/apps/hbase/data");

    BlueprintConfigurationProcessor configProcessor =
      new BlueprintConfigurationProcessor(configProperties);

    // call top-level export method, which will call the HA-specific method if HA is enabled
    configProcessor.doUpdateForBlueprintExport(Arrays.asList(mockHostGroupOne));

    // verify that any properties that include nameservices are not removed from the exported blueprint's configuration
    assertEquals("Property containing an HA nameservice (fs.defaultFS), was not correctly exported by the processor",
      "hdfs://" + expectedNameService, coreSiteProperties.get("fs.defaultFS"));
    assertEquals("Property containing an HA nameservice (hbase.rootdir), was not correctly exported by the processor",
      "hdfs://" + expectedNameService + "/apps/hbase/data", hbaseSiteProperties.get("hbase.rootdir"));

    mockSupport.verifyAll();

  }

  @Test
  public void testDoNameNodeHighAvailabilityUpdateWithHANotEnabled() throws Exception {
    EasyMockSupport mockSupport = new EasyMockSupport();

    HostGroup mockHostGroupOne = mockSupport.createMock(HostGroup.class);

    mockSupport.replayAll();

    Map<String, Map<String, String>> configProperties =
      new HashMap<String, Map<String, String>>();

    Map<String, String> hdfsSiteProperties =
      new HashMap<String, String>();

    configProperties.put("hdfs-site", hdfsSiteProperties);

    // hdfs-site config for this test will not include an HA values

    BlueprintConfigurationProcessor configProcessor =
      new BlueprintConfigurationProcessor(configProperties);

    assertEquals("Incorrect initial state for hdfs-site config",
      0, hdfsSiteProperties.size());

    // call top-level export method
    configProcessor.doUpdateForBlueprintExport(Arrays.asList(mockHostGroupOne));

    assertEquals("Incorrect state for hdsf-site config after HA call in non-HA environment, should be zero",
      0, hdfsSiteProperties.size());

    mockSupport.verifyAll();

  }

  @Test
  public void testDoNameNodeHighAvailabilityUpdateWithHAEnabledMultipleServices() throws Exception {
    final String expectedNameServiceOne = "mynameserviceOne";
    final String expectedNameServiceTwo = "mynameserviceTwo";
    final String expectedHostNameOne = "c6401.apache.ambari.org";
    final String expectedHostNameTwo = "c6402.apache.ambari.org";

    final String expectedPortNum = "808080";
    final String expectedNodeOne = "nn1";
    final String expectedNodeTwo = "nn2";
    final String expectedHostGroupName = "host_group_1";

    EasyMockSupport mockSupport = new EasyMockSupport();

    HostGroup mockHostGroupOne = mockSupport.createMock(HostGroup.class);

    expect(mockHostGroupOne.getHostInfo()).andReturn(Arrays.asList(expectedHostNameOne, expectedHostNameTwo, "serverTwo")).atLeastOnce();
    expect(mockHostGroupOne.getName()).andReturn(expectedHostGroupName).atLeastOnce();

    mockSupport.replayAll();

    Map<String, Map<String, String>> configProperties =
      new HashMap<String, Map<String, String>>();

    Map<String, String> hdfsSiteProperties =
      new HashMap<String, String>();

    configProperties.put("hdfs-site", hdfsSiteProperties);

    // setup hdfs config for test

    hdfsSiteProperties.put("dfs.nameservices", expectedNameServiceOne + "," + expectedNameServiceTwo);
    hdfsSiteProperties.put("dfs.ha.namenodes." + expectedNameServiceOne, expectedNodeOne + ", " + expectedNodeTwo);
    hdfsSiteProperties.put("dfs.ha.namenodes." + expectedNameServiceTwo, expectedNodeOne + ", " + expectedNodeTwo);


    // setup properties that include host information for nameservice one
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameServiceOne + "." + expectedNodeOne, expectedHostNameOne + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameServiceOne + "." + expectedNodeTwo, expectedHostNameOne + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameServiceOne + "." + expectedNodeOne, expectedHostNameOne + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameServiceOne + "." + expectedNodeTwo, expectedHostNameOne + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameServiceOne + "." + expectedNodeOne, expectedHostNameOne + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameServiceOne + "." + expectedNodeTwo, expectedHostNameOne + ":" + expectedPortNum);

    // setup properties that include host information for nameservice two
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameServiceTwo + "." + expectedNodeOne, expectedHostNameTwo + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.namenode.https-address." + expectedNameServiceTwo + "." + expectedNodeTwo, expectedHostNameTwo + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameServiceTwo + "." + expectedNodeOne, expectedHostNameTwo + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.namenode.http-address." + expectedNameServiceTwo + "." + expectedNodeTwo, expectedHostNameTwo + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameServiceTwo + "." + expectedNodeOne, expectedHostNameTwo + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.namenode.rpc-address." + expectedNameServiceTwo + "." + expectedNodeTwo, expectedHostNameTwo + ":" + expectedPortNum);


    BlueprintConfigurationProcessor configProcessor =
      new BlueprintConfigurationProcessor(configProperties);

    // call top-level export method, which will call the HA-specific method if HA is enabled
    configProcessor.doUpdateForBlueprintExport(Arrays.asList(mockHostGroupOne));

    // verify results for name service one
    assertEquals("HTTPS address HA property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.https-address." + expectedNameServiceOne + "." + expectedNodeOne));
    assertEquals("HTTPS address HA property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.https-address." + expectedNameServiceOne + "." + expectedNodeTwo));

    assertEquals("HTTPS address HA property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.http-address." + expectedNameServiceOne + "." + expectedNodeOne));
    assertEquals("HTTPS address HA property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.http-address." + expectedNameServiceOne + "." + expectedNodeTwo));

    assertEquals("HTTPS address HA property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.rpc-address." + expectedNameServiceOne + "." + expectedNodeOne));
    assertEquals("HTTPS address HA property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.rpc-address." + expectedNameServiceOne + "." + expectedNodeTwo));


    // verify results for name service two
    assertEquals("HTTPS address HA property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.https-address." + expectedNameServiceTwo + "." + expectedNodeOne));
    assertEquals("HTTPS address HA property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.https-address." + expectedNameServiceTwo + "." + expectedNodeTwo));

    assertEquals("HTTPS address HA property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.http-address." + expectedNameServiceTwo + "." + expectedNodeOne));
    assertEquals("HTTPS address HA property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.http-address." + expectedNameServiceTwo + "." + expectedNodeTwo));

    assertEquals("HTTPS address HA property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.rpc-address." + expectedNameServiceTwo + "." + expectedNodeOne));
    assertEquals("HTTPS address HA property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.rpc-address." + expectedNameServiceTwo + "." + expectedNodeTwo));

    mockSupport.verifyAll();

  }

  @Test
  public void testIsNameNodeHAEnabled() throws Exception {
    Map<String, Map<String, String>> configProperties =
      new HashMap<String, Map<String, String>>();

    BlueprintConfigurationProcessor configProcessor =
      new BlueprintConfigurationProcessor(configProperties);

    assertFalse("Incorrect HA detection, hdfs-site not available",
      configProcessor.isNameNodeHAEnabled());

    Map<String, String> hdfsSiteMap = new HashMap<String, String>();
    configProperties.put("hdfs-site", hdfsSiteMap);

    assertFalse("Incorrect HA detection, HA flag not enabled",
      configProcessor.isNameNodeHAEnabled());

    hdfsSiteMap.put("dfs.nameservices", "myTestNameService");

    assertTrue("Incorrect HA detection, HA was enabled",
      configProcessor.isNameNodeHAEnabled());

  }

  @Test
  public void testParseNameServices() throws Exception {
    Map<String, String> hdfsSiteConfigMap =
      new HashMap<String, String>();
    hdfsSiteConfigMap.put("dfs.nameservices", "serviceOne");

    // verify that a single service is parsed correctly
    String[] result = BlueprintConfigurationProcessor.parseNameServices(hdfsSiteConfigMap);

    assertNotNull("Resulting array was null",
      result);
    assertEquals("Incorrect array size",
      1, result.length);
    assertEquals("Incorrect value for returned name service",
      "serviceOne", result[0]);

    // verify that multiple services are parsed correctly
    hdfsSiteConfigMap.put("dfs.nameservices", " serviceTwo, serviceThree, serviceFour");

    String[] resultTwo = BlueprintConfigurationProcessor.parseNameServices(hdfsSiteConfigMap);

    assertNotNull("Resulting array was null",
      resultTwo);
    assertEquals("Incorrect array size",
      3, resultTwo.length);
    assertEquals("Incorrect value for returned name service",
      "serviceTwo", resultTwo[0]);
    assertEquals("Incorrect value for returned name service",
      "serviceThree", resultTwo[1]);
    assertEquals("Incorrect value for returned name service",
      "serviceFour", resultTwo[2]);
  }

  @Test
  public void testParseNameNodes() throws Exception {
    final String expectedServiceName = "serviceOne";
    Map<String, String> hdfsSiteConfigMap =
      new HashMap<String, String>();
    hdfsSiteConfigMap.put("dfs.ha.namenodes." + expectedServiceName, "node1");

    // verify that a single name node is parsed correctly
    String[] result =
      BlueprintConfigurationProcessor.parseNameNodes(expectedServiceName, hdfsSiteConfigMap);

    assertNotNull("Resulting array was null",
      result);
    assertEquals("Incorrect array size",
      1, result.length);
    assertEquals("Incorrect value for returned name nodes",
      "node1", result[0]);

    // verify that multiple name nodes are parsed correctly
    hdfsSiteConfigMap.put("dfs.ha.namenodes." + expectedServiceName, " nodeSeven, nodeEight, nodeNine");

    String[] resultTwo =
      BlueprintConfigurationProcessor.parseNameNodes(expectedServiceName, hdfsSiteConfigMap);

    assertNotNull("Resulting array was null",
      resultTwo);
    assertEquals("Incorrect array size",
      3, resultTwo.length);
    assertEquals("Incorrect value for returned name node",
      "nodeSeven", resultTwo[0]);
    assertEquals("Incorrect value for returned name node",
      "nodeEight", resultTwo[1]);
    assertEquals("Incorrect value for returned name node",
      "nodeNine", resultTwo[2]);

  }

  @Test
  public void testYarnConfigExported() throws Exception {
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedPortNum = "808080";
    final String expectedHostGroupName = "host_group_1";

    EasyMockSupport mockSupport = new EasyMockSupport();

    HostGroup mockHostGroupOne = mockSupport.createMock(HostGroup.class);

    expect(mockHostGroupOne.getHostInfo()).andReturn(Arrays.asList(expectedHostName, "serverTwo")).atLeastOnce();
    expect(mockHostGroupOne.getName()).andReturn(expectedHostGroupName).atLeastOnce();

    mockSupport.replayAll();

    Map<String, Map<String, String>> configProperties =
      new HashMap<String, Map<String, String>>();

    Map<String, String> yarnSiteProperties =
      new HashMap<String, String>();

    configProperties.put("yarn-site", yarnSiteProperties);

    // setup properties that include host information
    yarnSiteProperties.put("yarn.log.server.url", "http://" + expectedHostName +":19888/jobhistory/logs");
    yarnSiteProperties.put("yarn.resourcemanager.hostname", expectedHostName);
    yarnSiteProperties.put("yarn.resourcemanager.resource-tracker.address", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.resourcemanager.webapp.address", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.resourcemanager.scheduler.address", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.resourcemanager.address", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.resourcemanager.admin.address", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.timeline-service.address", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.timeline-service.webapp.address", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.timeline-service.webapp.https.address", expectedHostName + ":" + expectedPortNum);

    BlueprintConfigurationProcessor configProcessor =
      new BlueprintConfigurationProcessor(configProperties);

    // call top-level export method
    configProcessor.doUpdateForBlueprintExport(Arrays.asList(mockHostGroupOne));

    assertEquals("Yarn Log Server URL was incorrectly exported",
      "http://" + "%HOSTGROUP::" + expectedHostGroupName + "%" +":19888/jobhistory/logs", yarnSiteProperties.get("yarn.log.server.url"));
    assertEquals("Yarn ResourceManager hostname was incorrectly exported",
      createExportedHostName(expectedHostGroupName), yarnSiteProperties.get("yarn.resourcemanager.hostname"));
    assertEquals("Yarn ResourceManager tracker address was incorrectly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), yarnSiteProperties.get("yarn.resourcemanager.resource-tracker.address"));
    assertEquals("Yarn ResourceManager webapp address was incorrectly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), yarnSiteProperties.get("yarn.resourcemanager.webapp.address"));
    assertEquals("Yarn ResourceManager scheduler address was incorrectly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), yarnSiteProperties.get("yarn.resourcemanager.scheduler.address"));
    assertEquals("Yarn ResourceManager address was incorrectly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), yarnSiteProperties.get("yarn.resourcemanager.address"));
    assertEquals("Yarn ResourceManager admin address was incorrectly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), yarnSiteProperties.get("yarn.resourcemanager.admin.address"));
    assertEquals("Yarn ResourceManager timeline-service address was incorrectly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), yarnSiteProperties.get("yarn.timeline-service.address"));
    assertEquals("Yarn ResourceManager timeline webapp address was incorrectly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), yarnSiteProperties.get("yarn.timeline-service.webapp.address"));
    assertEquals("Yarn ResourceManager timeline webapp HTTPS address was incorrectly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), yarnSiteProperties.get("yarn.timeline-service.webapp.https.address"));

    mockSupport.verifyAll();

  }

  @Test
  public void testYarnConfigExportedWithDefaultZeroHostAddress() throws Exception {
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedPortNum = "808080";
    final String expectedHostGroupName = "host_group_1";

    EasyMockSupport mockSupport = new EasyMockSupport();

    HostGroup mockHostGroupOne = mockSupport.createMock(HostGroup.class);

    expect(mockHostGroupOne.getHostInfo()).andReturn(Arrays.asList(expectedHostName, "serverTwo")).atLeastOnce();
    expect(mockHostGroupOne.getName()).andReturn(expectedHostGroupName).atLeastOnce();

    mockSupport.replayAll();

    Map<String, Map<String, String>> configProperties =
      new HashMap<String, Map<String, String>>();

    Map<String, String> yarnSiteProperties =
      new HashMap<String, String>();

    configProperties.put("yarn-site", yarnSiteProperties);

    // setup properties that include host information
    yarnSiteProperties.put("yarn.log.server.url", "http://" + expectedHostName +":19888/jobhistory/logs");
    yarnSiteProperties.put("yarn.resourcemanager.hostname", expectedHostName);
    yarnSiteProperties.put("yarn.resourcemanager.resource-tracker.address", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.resourcemanager.webapp.address", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.resourcemanager.scheduler.address", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.resourcemanager.address", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.resourcemanager.admin.address", expectedHostName + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.timeline-service.address", "0.0.0.0" + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.timeline-service.webapp.address", "0.0.0.0" + ":" + expectedPortNum);
    yarnSiteProperties.put("yarn.timeline-service.webapp.https.address", "0.0.0.0" + ":" + expectedPortNum);

    BlueprintConfigurationProcessor configProcessor =
      new BlueprintConfigurationProcessor(configProperties);

    // call top-level export method
    configProcessor.doUpdateForBlueprintExport(Arrays.asList(mockHostGroupOne));

    assertEquals("Yarn Log Server URL was incorrectly exported",
      "http://" + "%HOSTGROUP::" + expectedHostGroupName + "%" +":19888/jobhistory/logs", yarnSiteProperties.get("yarn.log.server.url"));
    assertEquals("Yarn ResourceManager hostname was incorrectly exported",
      createExportedHostName(expectedHostGroupName), yarnSiteProperties.get("yarn.resourcemanager.hostname"));
    assertEquals("Yarn ResourceManager tracker address was incorrectly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), yarnSiteProperties.get("yarn.resourcemanager.resource-tracker.address"));
    assertEquals("Yarn ResourceManager webapp address was incorrectly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), yarnSiteProperties.get("yarn.resourcemanager.webapp.address"));
    assertEquals("Yarn ResourceManager scheduler address was incorrectly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), yarnSiteProperties.get("yarn.resourcemanager.scheduler.address"));
    assertEquals("Yarn ResourceManager address was incorrectly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), yarnSiteProperties.get("yarn.resourcemanager.address"));
    assertEquals("Yarn ResourceManager admin address was incorrectly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), yarnSiteProperties.get("yarn.resourcemanager.admin.address"));
    assertEquals("Yarn ResourceManager timeline-service address was incorrectly exported",
      "0.0.0.0" + ":" + expectedPortNum, yarnSiteProperties.get("yarn.timeline-service.address"));
    assertEquals("Yarn ResourceManager timeline webapp address was incorrectly exported",
      "0.0.0.0" + ":" + expectedPortNum, yarnSiteProperties.get("yarn.timeline-service.webapp.address"));
    assertEquals("Yarn ResourceManager timeline webapp HTTPS address was incorrectly exported",
      "0.0.0.0" + ":" + expectedPortNum, yarnSiteProperties.get("yarn.timeline-service.webapp.https.address"));

    mockSupport.verifyAll();

  }

  @Test
  public void testHDFSConfigExported() throws Exception {
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedPortNum = "808080";
    final String expectedHostGroupName = "host_group_1";

    EasyMockSupport mockSupport = new EasyMockSupport();

    HostGroup mockHostGroupOne = mockSupport.createMock(HostGroup.class);

    expect(mockHostGroupOne.getHostInfo()).andReturn(Arrays.asList(expectedHostName, "serverTwo")).atLeastOnce();
    expect(mockHostGroupOne.getName()).andReturn(expectedHostGroupName).atLeastOnce();

    mockSupport.replayAll();

    Map<String, Map<String, String>> configProperties =
      new HashMap<String, Map<String, String>>();

    Map<String, String> hdfsSiteProperties =
      new HashMap<String, String>();

    Map<String, String> coreSiteProperties =
      new HashMap<String, String>();

    Map<String, String> hbaseSiteProperties =
      new HashMap<String, String>();

    configProperties.put("hdfs-site", hdfsSiteProperties);
    configProperties.put("core-site", coreSiteProperties);
    configProperties.put("hbase-site", hbaseSiteProperties);

    // setup properties that include host information
    hdfsSiteProperties.put("dfs.http.address", expectedHostName + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.https.address", expectedHostName + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.namenode.http-address", expectedHostName + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.namenode.https-address", expectedHostName + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.secondary.http.address", expectedHostName + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.namenode.secondary.http-address", expectedHostName + ":" + expectedPortNum);
    hdfsSiteProperties.put("dfs.namenode.shared.edits.dir", expectedHostName + ":" + expectedPortNum);

    coreSiteProperties.put("fs.default.name", expectedHostName + ":" + expectedPortNum);
    coreSiteProperties.put("fs.defaultFS", "hdfs://" + expectedHostName + ":" + expectedPortNum);

    hbaseSiteProperties.put("hbase.rootdir", "hdfs://" + expectedHostName + ":" + expectedPortNum + "/apps/hbase/data");


    BlueprintConfigurationProcessor configProcessor =
      new BlueprintConfigurationProcessor(configProperties);

    // call top-level export method
    configProcessor.doUpdateForBlueprintExport(Arrays.asList(mockHostGroupOne));

    assertEquals("hdfs config property not exported properly",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.http.address"));
    assertEquals("hdfs config property not exported properly",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.https.address"));
    assertEquals("hdfs config property not exported properly",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.http-address"));
    assertEquals("hdfs config property not exported properly",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.https-address"));
    assertEquals("hdfs config property not exported properly",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.secondary.http.address"));
    assertEquals("hdfs config property not exported properly",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.secondary.http-address"));
    assertEquals("hdfs config property not exported properly",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hdfsSiteProperties.get("dfs.namenode.shared.edits.dir"));

    assertEquals("hdfs config in core-site not exported properly",
      createExportedAddress(expectedPortNum, expectedHostGroupName), coreSiteProperties.get("fs.default.name"));
    assertEquals("hdfs config in core-site not exported properly",
      "hdfs://" + createExportedAddress(expectedPortNum, expectedHostGroupName), coreSiteProperties.get("fs.defaultFS"));

    assertEquals("hdfs config in hbase-site not exported properly",
      "hdfs://" + createExportedAddress(expectedPortNum, expectedHostGroupName) + "/apps/hbase/data", hbaseSiteProperties.get("hbase.rootdir"));

    mockSupport.verifyAll();

  }

  @Test
  public void testHiveConfigExported() throws Exception {
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedHostNameTwo = "c6402.ambari.apache.org";
    final String expectedPortNum = "808080";
    final String expectedHostGroupName = "host_group_1";
    final String expectedHostGroupNameTwo = "host_group_2";

    EasyMockSupport mockSupport = new EasyMockSupport();

    HostGroup mockHostGroupOne = mockSupport.createMock(HostGroup.class);
    HostGroup mockHostGroupTwo = mockSupport.createMock(HostGroup.class);

    expect(mockHostGroupOne.getHostInfo()).andReturn(Arrays.asList(expectedHostName, "serverTwo")).atLeastOnce();
    expect(mockHostGroupTwo.getHostInfo()).andReturn(Arrays.asList(expectedHostNameTwo, "serverTwo")).atLeastOnce();
    expect(mockHostGroupOne.getName()).andReturn(expectedHostGroupName).atLeastOnce();
    expect(mockHostGroupTwo.getName()).andReturn(expectedHostGroupNameTwo).atLeastOnce();

    mockSupport.replayAll();

    Map<String, Map<String, String>> configProperties =
      new HashMap<String, Map<String, String>>();

    Map<String, String> hiveSiteProperties =
      new HashMap<String, String>();
    Map<String, String> hiveEnvProperties =
      new HashMap<String, String>();
    Map<String, String> webHCatSiteProperties =
      new HashMap<String, String>();
    Map<String, String> coreSiteProperties =
      new HashMap<String, String>();

    configProperties.put("hive-site", hiveSiteProperties);
    configProperties.put("hive-env", hiveEnvProperties);
    configProperties.put("webhcat-site", webHCatSiteProperties);
    configProperties.put("core-site", coreSiteProperties);


    // setup properties that include host information
    hiveSiteProperties.put("hive.metastore.uris", expectedHostName + ":" + expectedPortNum);
    hiveSiteProperties.put("javax.jdo.option.ConnectionURL", expectedHostName + ":" + expectedPortNum);
    hiveSiteProperties.put("hive.zookeeper.quorum", expectedHostName + ":" + expectedPortNum + "," + expectedHostNameTwo + ":" + expectedPortNum);
    hiveSiteProperties.put("hive.cluster.delegation.token.store.zookeeper.connectString", expectedHostName + ":" + expectedPortNum + "," + expectedHostNameTwo + ":" + expectedPortNum);
    hiveEnvProperties.put("hive_hostname", expectedHostName);


    webHCatSiteProperties.put("templeton.hive.properties", expectedHostName + "," + expectedHostNameTwo);
    webHCatSiteProperties.put("templeton.kerberos.principal", expectedHostName);

    coreSiteProperties.put("hadoop.proxyuser.hive.hosts", expectedHostName + "," + expectedHostNameTwo);
    coreSiteProperties.put("hadoop.proxyuser.HTTP.hosts", expectedHostName + "," + expectedHostNameTwo);
    coreSiteProperties.put("hadoop.proxyuser.hcat.hosts", expectedHostName + "," + expectedHostNameTwo);

    BlueprintConfigurationProcessor configProcessor =
      new BlueprintConfigurationProcessor(configProperties);

    // call top-level export method
    configProcessor.doUpdateForBlueprintExport(Arrays.asList(mockHostGroupOne, mockHostGroupTwo));

    assertEquals("hive property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hiveSiteProperties.get("hive.metastore.uris"));
    assertEquals("hive property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName), hiveSiteProperties.get("javax.jdo.option.ConnectionURL"));
    assertEquals("hive property not properly exported",
      createExportedHostName(expectedHostGroupName), hiveEnvProperties.get("hive_hostname"));

    assertEquals("hive property not properly exported",
      createExportedHostName(expectedHostGroupName) + "," + createExportedHostName(expectedHostGroupNameTwo),
      webHCatSiteProperties.get("templeton.hive.properties"));
    assertEquals("hive property not properly exported",
      createExportedHostName(expectedHostGroupName), webHCatSiteProperties.get("templeton.kerberos.principal"));

    assertEquals("hive property not properly exported",
      createExportedHostName(expectedHostGroupName) + "," + createExportedHostName(expectedHostGroupNameTwo), coreSiteProperties.get("hadoop.proxyuser.hive.hosts"));

    assertEquals("hive property not properly exported",
      createExportedHostName(expectedHostGroupName) + "," + createExportedHostName(expectedHostGroupNameTwo), coreSiteProperties.get("hadoop.proxyuser.HTTP.hosts"));

    assertEquals("hive property not properly exported",
      createExportedHostName(expectedHostGroupName) + "," + createExportedHostName(expectedHostGroupNameTwo), coreSiteProperties.get("hadoop.proxyuser.hcat.hosts"));

    assertEquals("hive zookeeper quorum property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName) + "," + createExportedAddress(expectedPortNum, expectedHostGroupNameTwo),
      hiveSiteProperties.get("hive.zookeeper.quorum"));

    assertEquals("hive zookeeper connectString property not properly exported",
      createExportedAddress(expectedPortNum, expectedHostGroupName) + "," + createExportedAddress(expectedPortNum, expectedHostGroupNameTwo),
      hiveSiteProperties.get("hive.cluster.delegation.token.store.zookeeper.connectString"));

    mockSupport.verifyAll();
  }


  @Test
  public void testOozieConfigExported() throws Exception {
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedHostNameTwo = "c6402.ambari.apache.org";
    final String expectedExternalHost = "c6408.ambari.apache.org";
    final String expectedHostGroupName = "host_group_1";
    final String expectedHostGroupNameTwo = "host_group_2";

    EasyMockSupport mockSupport = new EasyMockSupport();

    HostGroup mockHostGroupOne = mockSupport.createMock(HostGroup.class);
    HostGroup mockHostGroupTwo = mockSupport.createMock(HostGroup.class);

    expect(mockHostGroupOne.getHostInfo()).andReturn(Arrays.asList(expectedHostName, "serverTwo")).atLeastOnce();
    expect(mockHostGroupTwo.getHostInfo()).andReturn(Arrays.asList(expectedHostNameTwo, "serverTwo")).atLeastOnce();
    expect(mockHostGroupOne.getName()).andReturn(expectedHostGroupName).atLeastOnce();
    expect(mockHostGroupTwo.getName()).andReturn(expectedHostGroupNameTwo).atLeastOnce();

    mockSupport.replayAll();

    Map<String, Map<String, String>> configProperties =
      new HashMap<String, Map<String, String>>();

    Map<String, String> oozieSiteProperties =
      new HashMap<String, String>();
    Map<String, String> oozieEnvProperties =
      new HashMap<String, String>();
    Map<String, String> coreSiteProperties =
      new HashMap<String, String>();

    configProperties.put("oozie-site", oozieSiteProperties);
    configProperties.put("oozie-env", oozieEnvProperties);
    configProperties.put("hive-env", oozieEnvProperties);
    configProperties.put("core-site", coreSiteProperties);

    oozieSiteProperties.put("oozie.base.url", expectedHostName);
    oozieSiteProperties.put("oozie.authentication.kerberos.principal", expectedHostName);
    oozieSiteProperties.put("oozie.service.HadoopAccessorService.kerberos.principal", expectedHostName);
    oozieSiteProperties.put("oozie.service.JPAService.jdbc.url", "jdbc:mysql://" + expectedExternalHost + "/ooziedb");

    oozieEnvProperties.put("oozie_hostname", expectedHostName);
    oozieEnvProperties.put("oozie_existing_mysql_host", expectedExternalHost);

    coreSiteProperties.put("hadoop.proxyuser.oozie.hosts", expectedHostName + "," + expectedHostNameTwo);

    BlueprintConfigurationProcessor configProcessor =
      new BlueprintConfigurationProcessor(configProperties);

    // call top-level export method
    configProcessor.doUpdateForBlueprintExport(Arrays.asList(mockHostGroupOne, mockHostGroupTwo));

    assertEquals("oozie property not exported correctly",
      createExportedHostName(expectedHostGroupName), oozieSiteProperties.get("oozie.base.url"));
    assertEquals("oozie property not exported correctly",
      createExportedHostName(expectedHostGroupName), oozieSiteProperties.get("oozie.authentication.kerberos.principal"));
    assertEquals("oozie property not exported correctly",
      createExportedHostName(expectedHostGroupName), oozieSiteProperties.get("oozie.service.HadoopAccessorService.kerberos.principal"));
    assertEquals("oozie property not exported correctly",
      createExportedHostName(expectedHostGroupName), oozieEnvProperties.get("oozie_hostname"));
    assertEquals("oozie property not exported correctly",
      createExportedHostName(expectedHostGroupName) + "," + createExportedHostName(expectedHostGroupNameTwo), coreSiteProperties.get("hadoop.proxyuser.oozie.hosts"));

    // verify that the oozie properties that can refer to an external DB are not included in the export
    assertFalse("oozie_existing_mysql_host should not have been present in the exported configuration",
      oozieEnvProperties.containsKey("oozie_existing_mysql_host"));
    assertFalse("oozie.service.JPAService.jdbc.url should not have been present in the exported configuration",
      oozieSiteProperties.containsKey("oozie.service.JPAService.jdbc.url"));

    mockSupport.verifyAll();

  }

  @Test
  public void testZookeeperConfigExported() throws Exception {
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedHostNameTwo = "c6402.ambari.apache.org";
    final String expectedHostGroupName = "host_group_1";
    final String expectedHostGroupNameTwo = "host_group_2";
    final String expectedPortNumberOne = "2112";
    final String expectedPortNumberTwo = "1221";

    EasyMockSupport mockSupport = new EasyMockSupport();

    HostGroup mockHostGroupOne = mockSupport.createMock(HostGroup.class);
    HostGroup mockHostGroupTwo = mockSupport.createMock(HostGroup.class);

    expect(mockHostGroupOne.getHostInfo()).andReturn(Arrays.asList(expectedHostName, "serverTwo")).atLeastOnce();
    expect(mockHostGroupTwo.getHostInfo()).andReturn(Arrays.asList(expectedHostNameTwo, "serverTwo")).atLeastOnce();
    expect(mockHostGroupOne.getName()).andReturn(expectedHostGroupName).atLeastOnce();
    expect(mockHostGroupTwo.getName()).andReturn(expectedHostGroupNameTwo).atLeastOnce();

    mockSupport.replayAll();

    Map<String, Map<String, String>> configProperties =
      new HashMap<String, Map<String, String>>();

    Map<String, String> coreSiteProperties =
      new HashMap<String, String>();
    Map<String, String> hbaseSiteProperties =
      new HashMap<String, String>();
    Map<String, String> webHCatSiteProperties =
      new HashMap<String, String>();
    Map<String, String> sliderClientProperties =
      new HashMap<String, String>();
    Map<String, String> yarnSiteProperties =
      new HashMap<String, String>();
    Map<String, String> kafkaBrokerProperties =
      new HashMap<String, String>();

    configProperties.put("core-site", coreSiteProperties);
    configProperties.put("hbase-site", hbaseSiteProperties);
    configProperties.put("webhcat-site", webHCatSiteProperties);
    configProperties.put("slider-client", sliderClientProperties);
    configProperties.put("yarn-site", yarnSiteProperties);
    configProperties.put("kafka-broker", kafkaBrokerProperties);

    coreSiteProperties.put("ha.zookeeper.quorum", expectedHostName + "," + expectedHostNameTwo);
    hbaseSiteProperties.put("hbase.zookeeper.quorum", expectedHostName + "," + expectedHostNameTwo);
    webHCatSiteProperties.put("templeton.zookeeper.hosts", expectedHostName + "," + expectedHostNameTwo);
    yarnSiteProperties.put("hadoop.registry.zk.quorum", createHostAddress(expectedHostName, expectedPortNumberOne) + "," + createHostAddress(expectedHostNameTwo, expectedPortNumberTwo));
    sliderClientProperties.put("slider.zookeeper.quorum", createHostAddress(expectedHostName, expectedPortNumberOne) + "," + createHostAddress(expectedHostNameTwo, expectedPortNumberTwo));
    kafkaBrokerProperties.put("zookeeper.connect", createHostAddress(expectedHostName, expectedPortNumberOne) + "," + createHostAddress(expectedHostNameTwo, expectedPortNumberTwo));


    BlueprintConfigurationProcessor configProcessor =
      new BlueprintConfigurationProcessor(configProperties);

    // call top-level export method
    configProcessor.doUpdateForBlueprintExport(Arrays.asList(mockHostGroupOne, mockHostGroupTwo));

    assertEquals("zookeeper config not properly exported",
      createExportedHostName(expectedHostGroupName) + "," + createExportedHostName(expectedHostGroupNameTwo),
      coreSiteProperties.get("ha.zookeeper.quorum"));
    assertEquals("zookeeper config not properly exported",
      createExportedHostName(expectedHostGroupName) + "," + createExportedHostName(expectedHostGroupNameTwo),
      hbaseSiteProperties.get("hbase.zookeeper.quorum"));
    assertEquals("zookeeper config not properly exported",
      createExportedHostName(expectedHostGroupName) + "," + createExportedHostName(expectedHostGroupNameTwo),
      webHCatSiteProperties.get("templeton.zookeeper.hosts"));
    assertEquals("yarn-site zookeeper config not properly exported",
      createExportedHostName(expectedHostGroupName, expectedPortNumberOne) + "," + createExportedHostName(expectedHostGroupNameTwo, expectedPortNumberTwo),
      yarnSiteProperties.get("hadoop.registry.zk.quorum"));
    assertEquals("slider-client zookeeper config not properly exported",
      createExportedHostName(expectedHostGroupName, expectedPortNumberOne) + "," + createExportedHostName(expectedHostGroupNameTwo, expectedPortNumberTwo),
      sliderClientProperties.get("slider.zookeeper.quorum"));
    assertEquals("kafka zookeeper config not properly exported",
      createExportedHostName(expectedHostGroupName, expectedPortNumberOne) + "," + createExportedHostName(expectedHostGroupNameTwo, expectedPortNumberTwo),
      kafkaBrokerProperties.get("zookeeper.connect"));

    mockSupport.verifyAll();

  }

  @Test
  public void testKnoxSecurityConfigExported() throws Exception {
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedHostNameTwo = "c6402.ambari.apache.org";
    final String expectedHostGroupName = "host_group_1";
    final String expectedHostGroupNameTwo = "host_group_2";

    EasyMockSupport mockSupport = new EasyMockSupport();

    HostGroup mockHostGroupOne = mockSupport.createMock(HostGroup.class);
    HostGroup mockHostGroupTwo = mockSupport.createMock(HostGroup.class);

    expect(mockHostGroupOne.getHostInfo()).andReturn(Arrays.asList(expectedHostName, "serverTwo")).atLeastOnce();
    expect(mockHostGroupTwo.getHostInfo()).andReturn(Arrays.asList(expectedHostNameTwo, "serverTwo")).atLeastOnce();
    expect(mockHostGroupOne.getName()).andReturn(expectedHostGroupName).atLeastOnce();
    expect(mockHostGroupTwo.getName()).andReturn(expectedHostGroupNameTwo).atLeastOnce();

    mockSupport.replayAll();

    Map<String, Map<String, String>> configProperties =
      new HashMap<String, Map<String, String>>();

    Map<String, String> coreSiteProperties =
      new HashMap<String, String>();
    Map<String, String> webHCatSiteProperties =
      new HashMap<String, String>();
    Map<String, String> oozieSiteProperties =
      new HashMap<String, String>();

    configProperties.put("core-site", coreSiteProperties);
    configProperties.put("webhcat-site", webHCatSiteProperties);
    configProperties.put("oozie-site", oozieSiteProperties);

    coreSiteProperties.put("hadoop.proxyuser.knox.hosts", expectedHostName + "," + expectedHostNameTwo);
    webHCatSiteProperties.put("webhcat.proxyuser.knox.hosts", expectedHostName + "," + expectedHostNameTwo);
    oozieSiteProperties.put("hadoop.proxyuser.knox.hosts", expectedHostName + "," + expectedHostNameTwo);
    oozieSiteProperties.put("oozie.service.ProxyUserService.proxyuser.knox.hosts", expectedHostName + "," + expectedHostNameTwo);

//    multiCoreSiteMap.put("hadoop.proxyuser.knox.hosts", new MultipleHostTopologyUpdater("KNOX_GATEWAY"));
//    multiWebhcatSiteMap.put("webhcat.proxyuser.knox.hosts", new MultipleHostTopologyUpdater("KNOX_GATEWAY"));
//    multiOozieSiteMap.put("hadoop.proxyuser.knox.hosts", new MultipleHostTopologyUpdater("KNOX_GATEWAY"));

    BlueprintConfigurationProcessor configProcessor =
      new BlueprintConfigurationProcessor(configProperties);

    // call top-level export method
    configProcessor.doUpdateForBlueprintExport(Arrays.asList(mockHostGroupOne, mockHostGroupTwo));

    assertEquals("Knox for core-site config not properly exported",
      createExportedHostName(expectedHostGroupName) + "," + createExportedHostName(expectedHostGroupNameTwo),
      coreSiteProperties.get("hadoop.proxyuser.knox.hosts"));
    assertEquals("Knox config for WebHCat not properly exported",
      createExportedHostName(expectedHostGroupName) + "," + createExportedHostName(expectedHostGroupNameTwo),
      webHCatSiteProperties.get("webhcat.proxyuser.knox.hosts"));
    assertEquals("Knox config for Oozie not properly exported",
      createExportedHostName(expectedHostGroupName) + "," + createExportedHostName(expectedHostGroupNameTwo),
      oozieSiteProperties.get("hadoop.proxyuser.knox.hosts"));
    assertEquals("Knox config for Oozie not properly exported",
        createExportedHostName(expectedHostGroupName) + "," + createExportedHostName(expectedHostGroupNameTwo),
        oozieSiteProperties.get("oozie.service.ProxyUserService.proxyuser.knox.hosts"));

    mockSupport.verifyAll();

  }

  @Test
  public void testKafkaConfigExported() throws Exception {
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedHostGroupName = "host_group_1";
    final String expectedPortNumberOne = "2112";

    EasyMockSupport mockSupport = new EasyMockSupport();

    HostGroup mockHostGroupOne = mockSupport.createMock(HostGroup.class);
    HostGroup mockHostGroupTwo = mockSupport.createMock(HostGroup.class);

    expect(mockHostGroupOne.getHostInfo()).andReturn(Arrays.asList(expectedHostName, "serverTwo")).atLeastOnce();
    expect(mockHostGroupOne.getName()).andReturn(expectedHostGroupName).atLeastOnce();

    mockSupport.replayAll();

    Map<String, Map<String, String>> configProperties =
      new HashMap<String, Map<String, String>>();

    Map<String, String> kafkaBrokerProperties =
      new HashMap<String, String>();

    configProperties.put("kafka-broker", kafkaBrokerProperties);

    kafkaBrokerProperties.put("kafka.ganglia.metrics.host", createHostAddress(expectedHostName, expectedPortNumberOne));


    BlueprintConfigurationProcessor configProcessor =
      new BlueprintConfigurationProcessor(configProperties);

    // call top-level export method
    configProcessor.doUpdateForBlueprintExport(Arrays.asList(mockHostGroupOne, mockHostGroupTwo));

    assertEquals("kafka Ganglia config not properly exported",
      createExportedHostName(expectedHostGroupName, expectedPortNumberOne),
      kafkaBrokerProperties.get("kafka.ganglia.metrics.host"));

    mockSupport.verifyAll();

  }

  @Test
  public void testNagiosConfigExported() throws Exception {
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedHostGroupName = "host_group_1";

    EasyMockSupport mockSupport = new EasyMockSupport();

    HostGroup mockHostGroupOne = mockSupport.createMock(HostGroup.class);

    expect(mockHostGroupOne.getHostInfo()).andReturn(Arrays.asList(expectedHostName, "serverTwo")).atLeastOnce();
    expect(mockHostGroupOne.getName()).andReturn(expectedHostGroupName).atLeastOnce();

    mockSupport.replayAll();

    Map<String, Map<String, String>> configProperties =
      new HashMap<String, Map<String, String>>();

    Map<String, String> nagiosEnvProperties =
      new HashMap<String, String>();

    configProperties.put("nagios-env", nagiosEnvProperties);

    nagiosEnvProperties.put("nagios_principal_name", expectedHostName);

    BlueprintConfigurationProcessor configProcessor =
      new BlueprintConfigurationProcessor(configProperties);

    // call top-level export method
    configProcessor.doUpdateForBlueprintExport(Arrays.asList(mockHostGroupOne));

    assertEquals("nagios config not properly exported",
      createExportedHostName(expectedHostGroupName),
      nagiosEnvProperties.get("nagios_principal_name"));

    mockSupport.verifyAll();

  }

  @Test
  public void testPropertyWithUndefinedHostisExported() throws Exception {
    final String expectedHostName = "c6401.apache.ambari.org";
    final String expectedHostGroupName = "host_group_1";

    EasyMockSupport mockSupport = new EasyMockSupport();

    HostGroup mockHostGroupOne = mockSupport.createMock(HostGroup.class);

    expect(mockHostGroupOne.getHostInfo()).andReturn(Arrays.asList(expectedHostName, "serverTwo")).atLeastOnce();
    expect(mockHostGroupOne.getName()).andReturn(expectedHostGroupName).atLeastOnce();

    mockSupport.replayAll();

    Map<String, Map<String, String>> configProperties = new HashMap<String, Map<String, String>>();

    Map<String, String> properties = new HashMap<String, String>();
    configProperties.put("storm-site", properties);

    // setup properties that include host information including undefined host properties
    properties.put("storm.zookeeper.servers", expectedHostName);
    properties.put("nimbus.childopts", "undefined");
    properties.put("worker.childopts", "some other info, undefined, more info");


    BlueprintConfigurationProcessor configProcessor =
        new BlueprintConfigurationProcessor(configProperties);

    // call top-level export method
    configProcessor.doUpdateForBlueprintExport(Arrays.asList(mockHostGroupOne));

    assertEquals("Property was incorrectly exported",
        "%HOSTGROUP::" + expectedHostGroupName + "%", properties.get("storm.zookeeper.servers"));
    assertEquals("Property with undefined host was incorrectly exported",
        "undefined", properties.get("nimbus.childopts"));
    assertEquals("Property with undefined host was incorrectly exported",
        "some other info, undefined, more info" , properties.get("worker.childopts"));

    mockSupport.verifyAll();
  }


  private static String createExportedAddress(String expectedPortNum, String expectedHostGroupName) {
    return createExportedHostName(expectedHostGroupName) + ":" + expectedPortNum;
  }

  private static String createExportedHostName(String expectedHostGroupName, String expectedPortNumber) {
    return createExportedHostName(expectedHostGroupName) + ":" + expectedPortNumber;
  }


  private static String createExportedHostName(String expectedHostGroupName) {
    return "%HOSTGROUP::" + expectedHostGroupName + "%";
  }

  private static String createHostAddress(String hostName, String portNumber) {
    return hostName + ":" + portNumber;
  }

  private class TestHostGroup implements HostGroup {

    private String name;
    private Collection<String> hosts;
    private Collection<String> components;

    private TestHostGroup(String name, Collection<String> hosts, Collection<String> components) {
      this.name = name;
      this.hosts = hosts;
      this.components = components;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public Collection<String> getHostInfo() {
      return hosts;
    }

    @Override
    public Collection<String> getComponents() {
      return components;
    }

    @Override
    public Map<String, Map<String, String>> getConfigurationProperties() {
      return null;
    }
  }

}
