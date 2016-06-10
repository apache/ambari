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
package org.apache.ambari.server.serveraction.upgrades;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.actionmanager.ExecutionCommandWrapper;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigImpl;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Injector;

/**
 * Tests upgrade-related server side actions
*/

public class RangerKerberosConfigCalculationTest {

  private Injector m_injector;
  private Clusters m_clusters;
  private Field m_clusterField;

  @Before
  public void setup() throws Exception {
    m_injector = EasyMock.createMock(Injector.class);
    m_clusters = EasyMock.createMock(Clusters.class);
    Cluster cluster = EasyMock.createMock(Cluster.class);

    Config hadoopConfig = new ConfigImpl("hadoop-env") {
      Map<String, String> mockProperties = new HashMap<String, String>() {{
        put("hdfs_user", "hdfs");
      }};

      @Override
      public Map<String, String> getProperties() {
        return mockProperties;
      }
    };


    Config hiveConfig = new ConfigImpl("hive-env") {
      Map<String, String> mockProperties = new HashMap<String, String>() {{
        put("hive_user", "hive");
      }};

      @Override
      public Map<String, String> getProperties() {
        return mockProperties;
      }
    };

    Config yarnConfig = new ConfigImpl("yarn-env") {
      Map<String, String> mockProperties = new HashMap<String, String>() {{
        put("yarn_user", "yarn");
      }};

      @Override
      public Map<String, String> getProperties() {
        return mockProperties;
      }
    };

    Config hbaseConfig = new ConfigImpl("hbase-env") {
      Map<String, String> mockProperties = new HashMap<String, String>() {{
        put("hbase_user", "hbase");
      }};

      @Override
      public Map<String, String> getProperties() {
        return mockProperties;
      }
    };

    Config knoxConfig = new ConfigImpl("knox-env") {
      Map<String, String> mockProperties = new HashMap<String, String>() {{
        put("knox_user", "knox");
      }};

      @Override
      public Map<String, String> getProperties() {
        return mockProperties;
      }
    };

    Config stormConfig = new ConfigImpl("storm-env") {
      Map<String, String> mockProperties = new HashMap<String, String>() {{
        put("storm_user", "storm");
      }};

      @Override
      public Map<String, String> getProperties() {
        return mockProperties;
      }
    };

    Config kafkaConfig = new ConfigImpl("kafka-env") {
      Map<String, String> mockProperties = new HashMap<String, String>() {{
        put("kafka_user", "kafka");
      }};

      @Override
      public Map<String, String> getProperties() {
        return mockProperties;
      }
    };

    Config kmsConfig = new ConfigImpl("kms-env") {
      Map<String, String> mockProperties = new HashMap<String, String>() {{
        put("kms_user", "kms");
      }};

      @Override
      public Map<String, String> getProperties() {
        return mockProperties;
      }
    };

    Config hdfsSiteConfig = new ConfigImpl("hdfs-site") {
      Map<String, String> mockProperties = new HashMap<String, String>() {{
        put("dfs.web.authentication.kerberos.principal", "HTTP/_HOST.COM");
        put("dfs.web.authentication.kerberos.keytab", "/etc/security/keytabs/spnego.kytab");
      }};

      @Override
      public Map<String, String> getProperties() {
        return mockProperties;
      }
    };

    Config adminSiteConfig = new ConfigImpl("ranger-admin-site") {
      Map<String, String> mockProperties = new HashMap<String, String>();
      @Override
      public Map<String, String> getProperties() {
        return mockProperties;
      }

      @Override
      public void setProperties(Map<String, String> properties) {
        mockProperties.putAll(properties);
      }

      @Override
      public void persist(boolean newConfig) {
        // no-op
      }
    };

    expect(cluster.getDesiredConfigByType("hadoop-env")).andReturn(hadoopConfig).atLeastOnce();
    expect(cluster.getDesiredConfigByType("hive-env")).andReturn(hiveConfig).atLeastOnce();
    expect(cluster.getDesiredConfigByType("yarn-env")).andReturn(yarnConfig).atLeastOnce();
    expect(cluster.getDesiredConfigByType("hbase-env")).andReturn(hbaseConfig).atLeastOnce();
    expect(cluster.getDesiredConfigByType("knox-env")).andReturn(knoxConfig).atLeastOnce();
    expect(cluster.getDesiredConfigByType("storm-env")).andReturn(stormConfig).atLeastOnce();
    expect(cluster.getDesiredConfigByType("kafka-env")).andReturn(kafkaConfig).atLeastOnce();
    expect(cluster.getDesiredConfigByType("kms-env")).andReturn(kmsConfig).atLeastOnce();
    expect(cluster.getDesiredConfigByType("hdfs-site")).andReturn(hdfsSiteConfig).atLeastOnce();
    expect(cluster.getDesiredConfigByType("ranger-admin-site")).andReturn(adminSiteConfig).atLeastOnce();

    expect(m_clusters.getCluster((String) anyObject())).andReturn(cluster).anyTimes();
    expect(m_injector.getInstance(Clusters.class)).andReturn(m_clusters).atLeastOnce();
    expect(cluster.getSecurityType()).andReturn(SecurityType.KERBEROS).anyTimes();

    replay(m_injector, m_clusters, cluster);

    m_clusterField = RangerKerberosConfigCalculation.class.getDeclaredField("m_clusters");
    m_clusterField.setAccessible(true);
  }

  @Test
  public void testAction() throws Exception {

    Map<String, String> commandParams = new HashMap<String, String>();
    commandParams.put("clusterName", "c1");

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName("c1");

    HostRoleCommand hrc = EasyMock.createMock(HostRoleCommand.class);
    expect(hrc.getRequestId()).andReturn(1L).anyTimes();
    expect(hrc.getStageId()).andReturn(2L).anyTimes();
    expect(hrc.getExecutionCommandWrapper()).andReturn(new ExecutionCommandWrapper(executionCommand)).anyTimes();
    replay(hrc);

    RangerKerberosConfigCalculation action = new RangerKerberosConfigCalculation();
    m_clusterField.set(action, m_clusters);

    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hrc);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    Cluster c = m_clusters.getCluster("c1");
    Config config = c.getDesiredConfigByType("ranger-admin-site");
    Map<String, String> map = config.getProperties();

    assertTrue(map.containsKey("ranger.plugins.hdfs.serviceuser"));
    assertTrue(map.containsKey("ranger.plugins.hive.serviceuser"));
    assertTrue(map.containsKey("ranger.plugins.yarn.serviceuser"));
    assertTrue(map.containsKey("ranger.plugins.hbase.serviceuser"));
    assertTrue(map.containsKey("ranger.plugins.knox.serviceuser"));
    assertTrue(map.containsKey("ranger.plugins.storm.serviceuser"));
    assertTrue(map.containsKey("ranger.plugins.kafka.serviceuser"));
    assertTrue(map.containsKey("ranger.plugins.kms.serviceuser"));
    assertTrue(map.containsKey("ranger.spnego.kerberos.principal"));
    assertTrue(map.containsKey("ranger.spnego.kerberos.keytab"));    


    assertEquals("hdfs", map.get("ranger.plugins.hdfs.serviceuser"));
    assertEquals("hive", map.get("ranger.plugins.hive.serviceuser"));
    assertEquals("yarn", map.get("ranger.plugins.yarn.serviceuser"));
    assertEquals("hbase", map.get("ranger.plugins.hbase.serviceuser"));
    assertEquals("knox", map.get("ranger.plugins.knox.serviceuser"));
    assertEquals("storm", map.get("ranger.plugins.storm.serviceuser"));
    assertEquals("kafka", map.get("ranger.plugins.kafka.serviceuser"));
    assertEquals("kms", map.get("ranger.plugins.kms.serviceuser"));
    assertEquals("HTTP/_HOST.COM", map.get("ranger.spnego.kerberos.principal"));
    assertEquals("/etc/security/keytabs/spnego.kytab", map.get("ranger.spnego.kerberos.keytab"));

    report = action.execute(null);
    assertNotNull(report);

  }

} 
