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

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Tests {@link HiveZKQuorumConfigAction} to ensure that the correct properties
 * are set.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(ZooKeeperQuorumCalculator.class)
public class HiveZKQuorumConfigActionTest {

  private static final String CLUSTER_NAME = "c1";
  private HiveZKQuorumConfigAction m_action = null;

  private Clusters m_clusters = EasyMock.createStrictMock(Clusters.class);
  private Cluster m_cluster = EasyMock.createStrictMock(Cluster.class);
  private Config m_hiveSiteConfig = EasyMock.createStrictMock(Config.class);
  private ExecutionCommand m_executionCommand = EasyMock.createNiceMock(ExecutionCommand.class);

  /**
   * Sets up some generic mocks before the test.
   *
   * @throws Exception
   */
  @Before
  public void before() throws Exception {
    m_action = new HiveZKQuorumConfigAction();

    // setup clusters->cluster mock
    EasyMock.expect(m_executionCommand.getClusterName()).andReturn(CLUSTER_NAME).atLeastOnce();
    EasyMock.expect(m_clusters.getCluster(CLUSTER_NAME)).andReturn(m_cluster).atLeastOnce();

    // set the mock objects on the class under test
    Field m_clusterField = HiveZKQuorumConfigAction.class.getDeclaredField("m_clusters");
    m_clusterField.setAccessible(true);
    m_clusterField.set(m_action, m_clusters);
    m_action.setExecutionCommand(m_executionCommand);

  }

  /**
   * Tests that the correct properties are set.
   *
   * @throws Exception
   */
  @Test
  public void testZKQuorumPropertiesSetCorrectly() throws Exception {
    final String zookeeperQuorum = "c6401.ambari.apache.org:2181,c6402.ambari.apache.org:2181";

    PowerMockito.mockStatic(ZooKeeperQuorumCalculator.class);
    PowerMockito.when(ZooKeeperQuorumCalculator.getZooKeeperQuorumString(m_cluster)).thenReturn(
        zookeeperQuorum);

    Map<String, String> hiveSiteProperties = new HashMap<>();

    EasyMock.expect(m_hiveSiteConfig.getProperties()).andReturn(hiveSiteProperties).atLeastOnce();

    m_hiveSiteConfig.setProperties(EasyMock.<Map<String, String>>anyObject());
    EasyMock.expectLastCall().once();

    m_hiveSiteConfig.save();
    EasyMock.expectLastCall().once();

    EasyMock.expect(m_cluster.getDesiredConfigByType(HiveZKQuorumConfigAction.HIVE_SITE_CONFIG_TYPE)).andReturn(m_hiveSiteConfig).atLeastOnce();

    EasyMock.replay(m_executionCommand, m_clusters, m_cluster, m_hiveSiteConfig);

    m_action.execute(null);

    EasyMock.verify(m_executionCommand, m_clusters, m_cluster, m_hiveSiteConfig);

    Assert.assertEquals(zookeeperQuorum, hiveSiteProperties.get(HiveZKQuorumConfigAction.HIVE_SITE_ZK_QUORUM));
    Assert.assertEquals(zookeeperQuorum, hiveSiteProperties.get(HiveZKQuorumConfigAction.HIVE_SITE_ZK_CONNECT_STRING));
  }

}
