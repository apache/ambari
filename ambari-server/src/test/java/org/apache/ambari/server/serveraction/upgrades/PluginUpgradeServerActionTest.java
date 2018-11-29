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

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.agent.stomp.AgentConfigsHolder;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.serveraction.ServerAction;
import org.apache.ambari.server.stack.upgrade.UpgradePack;
import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeContext;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.spi.ClusterInformation;
import org.apache.ambari.spi.exceptions.UpgradeActionException;
import org.apache.ambari.spi.upgrade.UpgradeAction;
import org.apache.ambari.spi.upgrade.UpgradeActionOperations;
import org.apache.ambari.spi.upgrade.UpgradeActionOperations.ConfigurationChanges;
import org.easymock.EasyMockSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Tests {@link PluginUpgradeServerAction}.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ PluginUpgradeServerAction.class })
public class PluginUpgradeServerActionTest extends EasyMockSupport {

  private static final String CLUSTER_NAME = "c1";
  private static final String FOO_SITE = "foo-site";
  private static final String CLASS_NAME = MockUpgradeAction.class.getName();

  private final Map<String, String> m_commandParams = new HashMap<>();

  private final StackId m_mockStackId = createNiceMock(StackId.class);
  private final StackInfo m_mockStackInfo = createNiceMock(StackInfo.class);

  private final Clusters m_mockClusters = createNiceMock(Clusters.class);
  private final Cluster m_mockCluster = createNiceMock(Cluster.class);
  private final Config m_mockConfig = createNiceMock(Config.class);

  private final UpgradeContext m_mockUpgradeContext = createNiceMock(UpgradeContext.class);
  private final UpgradePack m_mockUpgradePack = createNiceMock(UpgradePack.class);
  private final ClassLoader m_mockClassLoader = createNiceMock(ClassLoader.class);
  private final AmbariMetaInfo m_mockMetaInfo = createNiceMock(AmbariMetaInfo.class);

  private final AgentConfigsHolder m_mockAgentConfigsHolder = createNiceMock(
      AgentConfigsHolder.class);

  private PluginUpgradeServerAction m_action;

  /**
   * @throws Exception
   */
  @Before
  @SuppressWarnings("rawtypes")
  public void before() throws Exception {
    m_action = PowerMock.createNicePartialMock(PluginUpgradeServerAction.class, "getUpgradeContext",
        "createCommandReport", "getClusters");

    expect(m_mockCluster.getHosts()).andReturn(new ArrayList<>()).once();

    ExecutionCommand executionCommand = createNiceMock(ExecutionCommand.class);
    expect(executionCommand.getClusterName()).andReturn(CLUSTER_NAME).anyTimes();
    expect(executionCommand.getCommandParams()).andReturn(m_commandParams).once();
    m_action.setExecutionCommand(executionCommand);

    m_action.m_clusters = m_mockClusters;
    m_action.m_metainfoProvider = () -> m_mockMetaInfo;

    expect(m_mockUpgradeContext.getUpgradePack()).andReturn(m_mockUpgradePack).atLeastOnce();
    expect(m_mockUpgradePack.getOwnerStackId()).andReturn(m_mockStackId).atLeastOnce();
    expect(m_mockMetaInfo.getStack(m_mockStackId)).andReturn(m_mockStackInfo).atLeastOnce();
    expect(m_mockStackInfo.getLibraryClassLoader()).andReturn(m_mockClassLoader).atLeastOnce();

    expect(m_action.getClusters()).andReturn(m_mockClusters).anyTimes();
    expect(m_action.getUpgradeContext(m_mockCluster)).andReturn(m_mockUpgradeContext).once();

    Class clazz = MockUpgradeAction.class;
    expect(m_mockClassLoader.loadClass(CLASS_NAME)).andReturn(clazz).atLeastOnce();

    expect(m_mockCluster.getDesiredConfigByType(FOO_SITE)).andReturn(m_mockConfig).once();

    Map<String, String> configUpdates = new HashMap<>();
    configUpdates.put("property-name", "property-value");

    m_mockConfig.updateProperties(configUpdates);
    expectLastCall().once();

    m_mockConfig.save();
    expectLastCall().once();

    m_action.agentConfigsHolder = m_mockAgentConfigsHolder;

    m_commandParams.put("clusterName", CLUSTER_NAME);
    m_commandParams.put(ServerAction.WRAPPED_CLASS_NAME, CLASS_NAME);

    expect(m_mockClusters.getCluster(CLUSTER_NAME)).andReturn(m_mockCluster).once();
  }

  /**
   * @throws Exception
   */
  @After
  public void after() throws Exception {
    PowerMock.verify(m_action);
  }

  /**
   * Tests that a class can be invoked and its operations performed.
   *
   * @throws Exception
   */
  @Test
  public void testExecute() throws Exception {
    PowerMock.replay(m_action);
    replayAll();

    m_action.execute(null);

    // easymock verify
    verifyAll();
  }

  /**
   * A mock {@link UpgradeAction} for testing.
   */
  public static class MockUpgradeAction implements UpgradeAction {

    /**
     * {@inheritDoc}
     */
    @Override
    public UpgradeActionOperations getOperations(ClusterInformation clusterInformation)
        throws UpgradeActionException {

      List<ConfigurationChanges> allChanges = new ArrayList<>();
      ConfigurationChanges configurationTypeChanges = new ConfigurationChanges(FOO_SITE);
      configurationTypeChanges.set( "property-name", "property-value");
      allChanges.add(configurationTypeChanges);

      StringBuilder standardOutput = new StringBuilder("Standard Output");

      UpgradeActionOperations upgradeActionOperations = new UpgradeActionOperations();
      upgradeActionOperations.setConfigurationChanges(allChanges).setStandardOutput(standardOutput);

      return upgradeActionOperations;
    }
  }
}
