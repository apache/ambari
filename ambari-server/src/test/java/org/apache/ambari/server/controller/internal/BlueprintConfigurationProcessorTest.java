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

import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;


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
    Map<String, Map<String, String>> updatedProperties = updater.doUpdateForClusterCreate(hostGroups);
    String updatedVal = updatedProperties.get("yarn-site").get("yarn.resourcemanager.hostname");
    assertEquals("testhost", updatedVal);
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
    Map<String, Map<String, String>> updatedProperties = updater.doUpdateForClusterCreate(hostGroups);
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
    Map<String, Map<String, String>> updatedProperties = updater.doUpdateForClusterCreate(hostGroups);
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
    Map<String, Map<String, String>> updatedProperties = updater.doUpdateForClusterCreate(hostGroups);
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
    Map<String, Map<String, String>> updatedProperties = updater.doUpdateForClusterCreate(hostGroups);
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
    Map<String, Map<String, String>> updatedProperties = updater.doUpdateForClusterCreate(hostGroups);
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
    Map<String, Map<String, String>> updatedProperties = updater.doUpdateForClusterCreate(hostGroups);
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
    Map<String, Map<String, String>> updatedProperties = updater.doUpdateForClusterCreate(hostGroups);
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
    Map<String, Map<String, String>> updatedProperties = updater.doUpdateForClusterCreate(hostGroups);
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
    Map<String, Map<String, String>> updatedProperties = updater.doUpdateForClusterCreate(hostGroups);
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
    Map<String, Map<String, String>> updatedProperties = updater.doUpdateForClusterCreate(hostGroups);
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
    Map<String, Map<String, String>> updatedProperties = updater.doUpdateForClusterCreate(hostGroups);
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
    Map<String, Map<String, String>> updatedProperties = updater.doUpdateForClusterCreate(hostGroups);
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
    Map<String, Map<String, String>> updatedProperties = updater.doUpdateForClusterCreate(hostGroups);
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
    Map<String, Map<String, String>> updatedProperties = updater.doUpdateForClusterCreate(hostGroups);
    String updatedVal = updatedProperties.get("hive-env").get("javax.jdo.option.ConnectionURL");
    assertEquals("jdbc:mysql://myHost.com/hive?createDatabaseIfNotExist=true", updatedVal);
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
