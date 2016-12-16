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
import org.apache.ambari.server.state.Config;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Injector;

/**
 * Tests upgrade-related server side actions for SparkShufflePropertyConfig
 */
public class SparkShufflePropertyConfigTest {
  private Injector m_injector;
  private Clusters m_clusters;
  private Cluster cluster;
  private Field clusterField;

  @Before
  public void setup() throws Exception {
    m_injector = EasyMock.createMock(Injector.class);
    m_clusters = EasyMock.createMock(Clusters.class);
    cluster = EasyMock.createMock(Cluster.class);

    Map<String, String> mockProperties = new HashMap<String, String>() {{
      put("yarn.nodemanager.aux-services", "some_service");
    }};

    Config yarnConfig = EasyMock.createNiceMock(Config.class);
    expect(yarnConfig.getType()).andReturn("yarn-site").anyTimes();
    expect(yarnConfig.getProperties()).andReturn(mockProperties).anyTimes();

    expect(cluster.getDesiredConfigByType("yarn-site")).andReturn(yarnConfig).atLeastOnce();

    expect(m_clusters.getCluster((String) anyObject())).andReturn(cluster).anyTimes();
    expect(m_injector.getInstance(Clusters.class)).andReturn(m_clusters).atLeastOnce();

    replay(m_injector, m_clusters, yarnConfig);

    clusterField = SparkShufflePropertyConfig.class.getDeclaredField("clusters");
    clusterField.setAccessible(true);
  }

  @Test
  public void testAction() throws Exception {
    ExecutionCommand executionCommand = new ExecutionCommand();
    Map<String, String> commandParams = new HashMap<>();
    HostRoleCommand hrc = EasyMock.createMock(HostRoleCommand.class);
    SparkShufflePropertyConfig action = new SparkShufflePropertyConfig();

    commandParams.put("clusterName", "c1");
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName("c1");

    expect(hrc.getRequestId()).andReturn(1L).anyTimes();
    expect(hrc.getStageId()).andReturn(2L).anyTimes();
    expect(hrc.getExecutionCommandWrapper()).andReturn(new ExecutionCommandWrapper(executionCommand)).anyTimes();
    replay(cluster, hrc);

    clusterField.set(action, m_clusters);
    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hrc);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    Cluster c = m_clusters.getCluster("c1");
    Config config = c.getDesiredConfigByType("yarn-site");
    Map<String, String> map = config.getProperties();

    assertTrue(map.containsKey("yarn.nodemanager.aux-services"));
    assertTrue(map.containsKey("yarn.nodemanager.aux-services.spark_shuffle.class"));

    assertEquals("some_service,spark_shuffle", map.get("yarn.nodemanager.aux-services"));
    assertEquals("org.apache.spark.network.yarn.YarnShuffleService", map.get("yarn.nodemanager.aux-services.spark_shuffle.class"));
  }


}
