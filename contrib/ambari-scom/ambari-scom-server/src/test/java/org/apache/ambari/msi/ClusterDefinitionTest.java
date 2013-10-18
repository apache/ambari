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

package org.apache.ambari.msi;

import org.apache.ambari.scom.TestClusterDefinitionProvider;
import org.apache.ambari.scom.TestHostInfoProvider;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

/**
 */
public class ClusterDefinitionTest {
  @Test
  public void testGetServices() throws Exception {

    ClusterDefinition clusterDefinition = new ClusterDefinition(new TestStateProvider(), new TestClusterDefinitionProvider(), new TestHostInfoProvider());

    Set<String> services = clusterDefinition.getServices();

    Assert.assertTrue(services.contains("HDFS"));
    Assert.assertTrue(services.contains("FLUME"));
    Assert.assertTrue(services.contains("OOZIE"));
    Assert.assertTrue(services.contains("MAPREDUCE"));
    Assert.assertTrue(services.contains("HBASE"));
    Assert.assertTrue(services.contains("ZOOKEEPER"));
    Assert.assertTrue(services.contains("HIVE"));
    Assert.assertTrue(services.contains("WEBHCAT"));
  }

  @Test
  public void testGetHosts() throws Exception {
    ClusterDefinition clusterDefinition = new ClusterDefinition(new TestStateProvider(), new TestClusterDefinitionProvider(), new TestHostInfoProvider());

    Set<String> hosts = clusterDefinition.getHosts();

    Assert.assertTrue(hosts.contains("NAMENODE_MASTER.acme.com"));
    Assert.assertTrue(hosts.contains("SECONDARY_NAMENODE_MASTER.acme.com"));
    Assert.assertTrue(hosts.contains("FLUME_SERVICE1.acme.com"));
    Assert.assertTrue(hosts.contains("FLUME_SERVICE2.acme.com"));
    Assert.assertTrue(hosts.contains("FLUME_SERVICE3.acme.com"));
    Assert.assertTrue(hosts.contains("HBASE_MASTER.acme.com"));
    Assert.assertTrue(hosts.contains("HIVE_SERVER_MASTER.acme.com"));
    Assert.assertTrue(hosts.contains("JOBTRACKER_MASTER.acme.com"));
    Assert.assertTrue(hosts.contains("OOZIE_SERVER_MASTER.acme.com"));
    Assert.assertTrue(hosts.contains("slave1.acme.com"));
    Assert.assertTrue(hosts.contains("slave2.acme.com"));
    Assert.assertTrue(hosts.contains("slave3.acme.com"));
    Assert.assertTrue(hosts.contains("WEBHCAT_MASTER.acme.com"));
  }

  @Test
  public void testGetComponents() throws Exception {
    ClusterDefinition clusterDefinition = new ClusterDefinition(new TestStateProvider(), new TestClusterDefinitionProvider(), new TestHostInfoProvider());

    Set<String> components = clusterDefinition.getComponents("HDFS");
    Assert.assertTrue(components.contains("NAMENODE"));
    Assert.assertTrue(components.contains("SECONDARY_NAMENODE"));
    Assert.assertTrue(components.contains("DATANODE"));

    components = clusterDefinition.getComponents("MAPREDUCE");
    Assert.assertTrue(components.contains("JOBTRACKER"));
    Assert.assertTrue(components.contains("TASKTRACKER"));

    components = clusterDefinition.getComponents("FLUME");
    Assert.assertTrue(components.contains("FLUME_SERVER"));

    components = clusterDefinition.getComponents("OOZIE");
    Assert.assertTrue(components.contains("OOZIE_SERVER"));

    components = clusterDefinition.getComponents("WEBHCAT");
    Assert.assertTrue(components.contains("WEBHCAT_SERVER"));

    components = clusterDefinition.getComponents("HBASE");
    Assert.assertTrue(components.contains("HBASE_MASTER"));
    Assert.assertTrue(components.contains("HBASE_REGIONSERVER"));

    components = clusterDefinition.getComponents("ZOOKEEPER");
    Assert.assertTrue(components.contains("ZOOKEEPER_SERVER"));

    components = clusterDefinition.getComponents("HIVE");
    Assert.assertTrue(components.contains("HIVE_SERVER"));
  }

  @Test
  public void testGetHostComponents() throws Exception {
    ClusterDefinition clusterDefinition = new ClusterDefinition(new TestStateProvider(), new TestClusterDefinitionProvider(), new TestHostInfoProvider());

    Set<String> hostComponents = clusterDefinition.getHostComponents("HDFS", "NAMENODE_MASTER.acme.com");

    Assert.assertTrue(hostComponents.contains("NAMENODE"));

    hostComponents = clusterDefinition.getHostComponents("HDFS", "slave1.acme.com");

    Assert.assertTrue(hostComponents.contains("DATANODE"));

    hostComponents = clusterDefinition.getHostComponents("HDFS", "slave2.acme.com");

    Assert.assertTrue(hostComponents.contains("DATANODE"));
  }
}
