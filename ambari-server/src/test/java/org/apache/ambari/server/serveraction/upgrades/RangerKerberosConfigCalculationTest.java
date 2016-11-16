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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.actionmanager.ExecutionCommandWrapper;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.SecurityType;
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

    Config hadoopConfig = EasyMock.createNiceMock(Config.class);
    expect(hadoopConfig.getType()).andReturn("hadoop-env").anyTimes();
    expect(hadoopConfig.getProperties()).andReturn(Collections.singletonMap("hdfs_user", "hdfs")).anyTimes();

    Config hiveConfig = EasyMock.createNiceMock(Config.class);
    expect(hiveConfig.getType()).andReturn("hive-env").anyTimes();
    expect(hiveConfig.getProperties()).andReturn(Collections.singletonMap("hive_user", "hive")).anyTimes();

    Config yarnConfig = EasyMock.createNiceMock(Config.class);
    expect(yarnConfig.getType()).andReturn("yarn-env").anyTimes();
    expect(yarnConfig.getProperties()).andReturn(Collections.singletonMap("yarn_user", "yarn")).anyTimes();

    Config hbaseConfig = EasyMock.createNiceMock(Config.class);
    expect(hbaseConfig.getType()).andReturn("hbase-env").anyTimes();
    expect(hbaseConfig.getProperties()).andReturn(Collections.singletonMap("hbase_user", "hbase")).anyTimes();

    Config knoxConfig = EasyMock.createNiceMock(Config.class);
    expect(knoxConfig.getType()).andReturn("knox-env").anyTimes();
    expect(knoxConfig.getProperties()).andReturn(Collections.singletonMap("knox_user", "knox")).anyTimes();

    Map<String, String> mockProperties = new HashMap<String, String>() {{
      put("storm_user", "storm");
      put("storm_principal_name", "storm-c1@EXAMLE.COM");
    }};

    Config stormConfig = EasyMock.createNiceMock(Config.class);
    expect(stormConfig.getType()).andReturn("storm-env").anyTimes();
    expect(stormConfig.getProperties()).andReturn(mockProperties).anyTimes();

    Config kafkaConfig = EasyMock.createNiceMock(Config.class);
    expect(kafkaConfig.getType()).andReturn("kafka-env").anyTimes();
    expect(kafkaConfig.getProperties()).andReturn(Collections.singletonMap("kafka_user", "kafka")).anyTimes();

    Config kmsConfig = EasyMock.createNiceMock(Config.class);
    expect(kmsConfig.getType()).andReturn("kms-env").anyTimes();
    expect(kmsConfig.getProperties()).andReturn(Collections.singletonMap("kms_user", "kms")).anyTimes();

    Config hdfsSiteConfig = EasyMock.createNiceMock(Config.class);
    expect(hdfsSiteConfig.getType()).andReturn("hdfs-site").anyTimes();
    expect(hdfsSiteConfig.getProperties()).andReturn(Collections.singletonMap("dfs.web.authentication.kerberos.keytab", "/etc/security/keytabs/spnego.kytab")).anyTimes();

    Config adminSiteConfig = EasyMock.createNiceMock(Config.class);
    expect(adminSiteConfig.getType()).andReturn("ranger-admin-site").anyTimes();
    expect(adminSiteConfig.getProperties()).andReturn(new HashMap<String,String>()).anyTimes();

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

    replay(m_injector, m_clusters, cluster, hadoopConfig, hiveConfig, yarnConfig, hbaseConfig,
        knoxConfig, stormConfig, kafkaConfig, kmsConfig, hdfsSiteConfig, adminSiteConfig);

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
    assertTrue(map.containsKey("ranger.spnego.kerberos.keytab"));


    assertEquals("hdfs", map.get("ranger.plugins.hdfs.serviceuser"));
    assertEquals("hive", map.get("ranger.plugins.hive.serviceuser"));
    assertEquals("yarn", map.get("ranger.plugins.yarn.serviceuser"));
    assertEquals("hbase", map.get("ranger.plugins.hbase.serviceuser"));
    assertEquals("knox", map.get("ranger.plugins.knox.serviceuser"));
    assertEquals("storm-c1,storm", map.get("ranger.plugins.storm.serviceuser"));
    assertEquals("kafka", map.get("ranger.plugins.kafka.serviceuser"));
    assertEquals("kms", map.get("ranger.plugins.kms.serviceuser"));
    assertEquals("/etc/security/keytabs/spnego.kytab", map.get("ranger.spnego.kerberos.keytab"));

    report = action.execute(null);
    assertNotNull(report);

  }

}
