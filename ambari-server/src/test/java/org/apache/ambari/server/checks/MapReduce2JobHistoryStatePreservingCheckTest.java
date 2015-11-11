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
package org.apache.ambari.server.checks;

import com.google.inject.Provider;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.orm.entities.ClusterVersionEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.ambari.server.state.stack.UpgradePack.PrerequisiteCheckConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests for {@link org.apache.ambari.server.checks.MapReduce2JobHistoryStatePreservingCheckTest}
 */
public class MapReduce2JobHistoryStatePreservingCheckTest {
  private final Clusters m_clusters = Mockito.mock(Clusters.class);

  private final MapReduce2JobHistoryStatePreservingCheck m_check = new MapReduce2JobHistoryStatePreservingCheck();

  /**
   *
   */
  @Before
  public void setup() {
    m_check.clustersProvider = new Provider<Clusters>() {

      @Override
      public Clusters get() {
        return m_clusters;
      }
    };
    Configuration config = Mockito.mock(Configuration.class);
    Mockito.when(config.getRollingUpgradeMinStack()).thenReturn("HDP-2.2");
    Mockito.when(config.getRollingUpgradeMaxStack()).thenReturn("");
    m_check.config = config;
  }

  /**
   * @throws Exception
   */
  @Test
  public void testIsApplicable() throws Exception {
    final Cluster cluster = Mockito.mock(Cluster.class);
    Mockito.when(cluster.getClusterId()).thenReturn(1L);
    Mockito.when(m_clusters.getCluster("cluster")).thenReturn(cluster);
    Mockito.when(cluster.getCurrentStackVersion()).thenReturn(new StackId("HDP-2.3"));

    Map<String, Service> services = new HashMap<String, Service>();
    Mockito.when(cluster.getServices()).thenReturn(services);

    ClusterVersionEntity clusterVersionEntity = Mockito.mock(ClusterVersionEntity.class);
    Mockito.when(cluster.getCurrentClusterVersion()).thenReturn(clusterVersionEntity);

    PrereqCheckRequest request = new PrereqCheckRequest("cluster");
    request.setTargetStackId(new StackId("HDP", "2.3.1.1"));
    request.setSourceStackId(new StackId("HDP", "2.3.0.0"));

    // MAPREDUCE2 not installed
    Assert.assertFalse(m_check.isApplicable(request));

    // MAPREDUCE2 installed
    services.put("MAPREDUCE2", Mockito.mock(Service.class));
    Assert.assertTrue(m_check.isApplicable(request));
  }

  @Test
  public void testPerform() throws Exception {
    final Cluster cluster = Mockito.mock(Cluster.class);
    Mockito.when(cluster.getClusterId()).thenReturn(1L);
    Mockito.when(m_clusters.getCluster("cluster")).thenReturn(cluster);

    final DesiredConfig desiredConfig = Mockito.mock(DesiredConfig.class);
    Mockito.when(desiredConfig.getTag()).thenReturn("tag");
    Map<String, DesiredConfig> configMap = new HashMap<String, DesiredConfig>();
    configMap.put("mapred-site", desiredConfig);
    configMap.put("yarn-site", desiredConfig);

    Mockito.when(cluster.getDesiredConfigs()).thenReturn(configMap);
    final Config config = Mockito.mock(Config.class);
    Mockito.when(cluster.getConfig(Mockito.anyString(), Mockito.anyString())).thenReturn(config);
    final Map<String, String> properties = new HashMap<String, String>();
    Mockito.when(config.getProperties()).thenReturn(properties);

    PrerequisiteCheck check = new PrerequisiteCheck(null, null);
    m_check.perform(check, new PrereqCheckRequest("cluster"));
    Assert.assertEquals(PrereqCheckStatus.FAIL, check.getStatus());

    properties.put(MapReduce2JobHistoryStatePreservingCheck.MAPREDUCE2_JOBHISTORY_RECOVERY_ENABLE_KEY, "true");
    properties.put(MapReduce2JobHistoryStatePreservingCheck.MAPREDUCE2_JOBHISTORY_RECOVERY_STORE_KEY, "org.apache.hadoop.mapreduce.v2.hs.HistoryServerLeveldbStateStoreService");
    properties.put(MapReduce2JobHistoryStatePreservingCheck.MAPREDUCE2_JOBHISTORY_RECOVERY_STORE_LEVELDB_PATH_KEY, "");
    check = new PrerequisiteCheck(null, null);
    m_check.perform(check, new PrereqCheckRequest("cluster"));
    Assert.assertEquals(PrereqCheckStatus.FAIL, check.getStatus());
    check = new PrerequisiteCheck(null, null);
    properties.put(MapReduce2JobHistoryStatePreservingCheck.MAPREDUCE2_JOBHISTORY_RECOVERY_STORE_LEVELDB_PATH_KEY, "/hadoop/yarn/timeline");
    properties.put(MapReduce2JobHistoryStatePreservingCheck.YARN_TIMELINE_SERVICE_LEVELDB_STATE_STORE_PATH_KEY, "not /hadoop/yarn/timeline");
    m_check.perform(check, new PrereqCheckRequest("cluster"));
    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());
    check = new PrerequisiteCheck(null, null);
    properties.put(MapReduce2JobHistoryStatePreservingCheck.YARN_TIMELINE_SERVICE_LEVELDB_STATE_STORE_PATH_KEY, "/hadoop/yarn/timeline");
    m_check.perform(check, new PrereqCheckRequest("cluster"));
    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());
  }

  @SuppressWarnings("serial")
  @Test
  public void testIsApplicableMinimumStackVersion() throws Exception {
    final Cluster cluster = Mockito.mock(Cluster.class);
    Mockito.when(cluster.getClusterId()).thenReturn(1L);
    Mockito.when(cluster.getServices()).thenReturn(new HashMap<String, Service>() {
      {
        put("MAPREDUCE2", null);
      }
    });
    Mockito.when(cluster.getCurrentStackVersion()).thenReturn(new StackId("MYSTACK-12.2"));
    ClusterVersionEntity clusterVersionEntity = Mockito.mock(ClusterVersionEntity.class);
    Mockito.when(cluster.getCurrentClusterVersion()).thenReturn(clusterVersionEntity);
    RepositoryVersionEntity repositoryVersionEntity = Mockito.mock(RepositoryVersionEntity.class);
    Mockito.when(clusterVersionEntity.getRepositoryVersion()).thenReturn(repositoryVersionEntity);
    Mockito.when(m_clusters.getCluster("c1")).thenReturn(cluster);
    PrereqCheckRequest request = new PrereqCheckRequest("c1");

    Mockito.when(repositoryVersionEntity.getVersion()).thenReturn("2.0.0.1");
    boolean isApplicable = m_check.isApplicable(request);
    Assert.assertTrue(isApplicable);
  }
}
