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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;



import com.google.inject.Injector;

public class AtlasProxyUserConfigCalculationTest {

  private Injector m_injector;
  private Clusters m_clusters;
  private AgentConfigsHolder agentConfigsHolder;
  private Field m_clusterField;
  private Field agentConfigsHolderField;

  @Before
  public void setup() throws Exception {
    m_injector = createMock(Injector.class);
    m_clusters = createMock(Clusters.class);
    agentConfigsHolder = createMock(AgentConfigsHolder.class);
    Cluster cluster = createMock(Cluster.class);

    Map<String, String> mockKnoxEnvProperties = new HashMap<String, String>() {
      {
        put("knox_user", "knox_cstm");
      }
    };

    Map<String, String> mockAtlasApplicationProperties = new HashMap<String, String>();

    Config knoxEnvConfig = createMock(Config.class);
    expect(knoxEnvConfig.getType()).andReturn("knox-env").anyTimes();
    expect(knoxEnvConfig.getProperties()).andReturn(mockKnoxEnvProperties).anyTimes();


    Config atlasApplicationPropertiesConfig = createMock(Config.class);
    expect(atlasApplicationPropertiesConfig.getType()).andReturn("application-properties").anyTimes();
    expect(atlasApplicationPropertiesConfig.getProperties()).andReturn(mockAtlasApplicationProperties).anyTimes();


    atlasApplicationPropertiesConfig.setProperties(anyObject(Map.class));
    expectLastCall().atLeastOnce();

    atlasApplicationPropertiesConfig.save();
    expectLastCall().atLeastOnce();

    expect(cluster.getDesiredConfigByType("knox-env")).andReturn(knoxEnvConfig).atLeastOnce();
    expect(cluster.getDesiredConfigByType("application-properties")).andReturn(atlasApplicationPropertiesConfig).atLeastOnce();
    expect(cluster.getClusterId()).andReturn(1L).atLeastOnce();
    expect(cluster.getHosts()).andReturn(Collections.emptyList()).atLeastOnce();
    expect(m_clusters.getCluster((String) anyObject())).andReturn(cluster).anyTimes();
    expect(m_injector.getInstance(Clusters.class)).andReturn(m_clusters).atLeastOnce();
    agentConfigsHolder.updateData(eq(1L), eq(Collections.emptyList()));
    expectLastCall().atLeastOnce();

    replay(m_injector, m_clusters, cluster, knoxEnvConfig, atlasApplicationPropertiesConfig, agentConfigsHolder);

    m_clusterField = AbstractUpgradeServerAction.class.getDeclaredField("m_clusters");
    m_clusterField.setAccessible(true);
    agentConfigsHolderField = AbstractUpgradeServerAction.class.getDeclaredField("agentConfigsHolder");
    agentConfigsHolderField.setAccessible(true);

  }

  @Test
  public void testAction() throws Exception {

    Map<String, String> commandParams = new HashMap<String, String>();
    commandParams.put("clusterName", "cl1");

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName("cl1");

    HostRoleCommand hrc = createMock(HostRoleCommand.class);
    expect(hrc.getRequestId()).andReturn(1L).anyTimes();
    expect(hrc.getStageId()).andReturn(2L).anyTimes();
    expect(hrc.getExecutionCommandWrapper()).andReturn(new ExecutionCommandWrapper(executionCommand)).anyTimes();
    replay(hrc);

    AtlasProxyUserConfigCalculation action = new AtlasProxyUserConfigCalculation();
    m_clusterField.set(action, m_clusters);
    agentConfigsHolderField.set(action, agentConfigsHolder);
    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hrc);

    CommandReport report = action.execute(null);
    Assert.assertNotNull(report);

    Cluster cl = m_clusters.getCluster("cl1");
    Config config = cl.getDesiredConfigByType("application-properties");
    Map<String, String> map = config.getProperties();

    Assert.assertTrue(map.containsKey("atlas.proxyusers"));
    Assert.assertEquals("knox_cstm", map.get("atlas.proxyusers"));

    report = action.execute(null);
    Assert.assertNotNull(report);
  }
}
