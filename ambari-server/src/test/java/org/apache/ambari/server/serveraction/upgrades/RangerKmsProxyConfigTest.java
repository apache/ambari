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
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
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
import org.apache.ambari.server.agent.stomp.AgentConfigsHolder;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.SecurityType;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Injector;


public class RangerKmsProxyConfigTest {
  private Injector m_injector;
  private Clusters m_clusters;
  private AgentConfigsHolder agentConfigsHolder;
  private Field m_clusterField;
  private Field agentConfigsHolderField;

  @Before
  public void setup() throws Exception {
    m_injector = EasyMock.createMock(Injector.class);
    m_clusters = EasyMock.createMock(Clusters.class);
    agentConfigsHolder = createMock(AgentConfigsHolder.class);
    Cluster cluster = EasyMock.createMock(Cluster.class);

    Map<String, String> mockProperties = new HashMap<String, String>() {
      {
        put("ranger_user", "ranger");
      }
    };

    Config rangerEnv = EasyMock.createNiceMock(Config.class);
    expect(rangerEnv.getType()).andReturn("ranger-env").anyTimes();
    expect(rangerEnv.getProperties()).andReturn(mockProperties).anyTimes();

    Config kmsSite = EasyMock.createNiceMock(Config.class);
    expect(kmsSite.getType()).andReturn("kms-site").anyTimes();
    expect(kmsSite.getProperties()).andReturn(mockProperties).anyTimes();

    expect(cluster.getDesiredConfigByType("ranger-env")).andReturn(rangerEnv).atLeastOnce();
    expect(cluster.getDesiredConfigByType("kms-site")).andReturn(kmsSite).atLeastOnce();
    expect(m_clusters.getCluster((String) anyObject())).andReturn(cluster).anyTimes();
    expect(m_injector.getInstance(Clusters.class)).andReturn(m_clusters).atLeastOnce();
    expect(cluster.getSecurityType()).andReturn(SecurityType.KERBEROS).anyTimes();
    expect(cluster.getClusterId()).andReturn(1L).atLeastOnce();
    expect(cluster.getHosts()).andReturn(Collections.emptyList()).atLeastOnce();
    agentConfigsHolder.updateData(eq(1L), eq(Collections.emptyList()));
    expectLastCall().atLeastOnce();

    replay(m_injector, m_clusters, cluster, rangerEnv, kmsSite, agentConfigsHolder);

    m_clusterField = AbstractUpgradeServerAction.class.getDeclaredField("m_clusters");
    m_clusterField.setAccessible(true);
    agentConfigsHolderField = AbstractUpgradeServerAction.class.getDeclaredField("agentConfigsHolder");
    agentConfigsHolderField.setAccessible(true);
  }

  @Test
  public void testAction() throws Exception {

    Map<String, String> commandParams = new HashMap<>();
    commandParams.put("clusterName", "c1");

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName("c1");

    HostRoleCommand hrc = EasyMock.createMock(HostRoleCommand.class);
    expect(hrc.getRequestId()).andReturn(1L).anyTimes();
    expect(hrc.getStageId()).andReturn(2L).anyTimes();
    expect(hrc.getExecutionCommandWrapper()).andReturn(new ExecutionCommandWrapper(executionCommand)).anyTimes();
    replay(hrc);

    RangerKmsProxyConfig action = new RangerKmsProxyConfig();
    m_clusterField.set(action, m_clusters);
    agentConfigsHolderField.set(action, agentConfigsHolder);

    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hrc);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    Cluster c = m_clusters.getCluster("c1");
    Config config = c.getDesiredConfigByType("kms-site");
    Map<String, String> map = config.getProperties();

    assertTrue(map.containsKey("hadoop.kms.proxyuser.ranger.users"));
    assertTrue(map.containsKey("hadoop.kms.proxyuser.ranger.groups"));
    assertTrue(map.containsKey("hadoop.kms.proxyuser.ranger.hosts"));


    assertEquals("*", map.get("hadoop.kms.proxyuser.ranger.users"));
    assertEquals("*", map.get("hadoop.kms.proxyuser.ranger.groups"));
    assertEquals("*", map.get("hadoop.kms.proxyuser.ranger.hosts"));

    report = action.execute(null);
    assertNotNull(report);

  }
}
