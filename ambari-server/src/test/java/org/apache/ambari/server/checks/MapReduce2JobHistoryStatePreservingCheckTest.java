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

import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.RepositoryType;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.repository.ClusterVersionSummary;
import org.apache.ambari.server.state.repository.VersionDefinitionXml;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.inject.Provider;

/**
 * Tests for {@link org.apache.ambari.server.checks.MapReduce2JobHistoryStatePreservingCheckTest}
 */
@RunWith(MockitoJUnitRunner.class)
public class MapReduce2JobHistoryStatePreservingCheckTest {
  private final Clusters m_clusters = Mockito.mock(Clusters.class);
  private final RepositoryVersionDAO m_repositoryVersionDao = Mockito.mock(RepositoryVersionDAO.class);

  private final MapReduce2JobHistoryStatePreservingCheck m_check = new MapReduce2JobHistoryStatePreservingCheck();

  @Mock
  private ClusterVersionSummary m_clusterVersionSummary;

  @Mock
  private VersionDefinitionXml m_vdfXml;

  @Mock
  private RepositoryVersionEntity m_repositoryVersion;

  final Map<String, Service> m_services = new HashMap<>();

  /**
   *
   */
  @Before
  public void setup() throws Exception {
    m_check.clustersProvider = new Provider<Clusters>() {

      @Override
      public Clusters get() {
        return m_clusters;
      }
    };

    m_check.repositoryVersionDaoProvider = new Provider<RepositoryVersionDAO>() {
      @Override
      public RepositoryVersionDAO get() {
        return m_repositoryVersionDao;
      };
    };

    Configuration config = Mockito.mock(Configuration.class);
    m_check.config = config;

    RepositoryVersionEntity rve = Mockito.mock(RepositoryVersionEntity.class);
    Mockito.when(rve.getType()).thenReturn(RepositoryType.STANDARD);
    Mockito.when(m_repositoryVersionDao.findByStackNameAndVersion(Mockito.anyString(), Mockito.anyString())).thenReturn(rve);

    Mockito.when(m_repositoryVersion.getVersion()).thenReturn("2.3.1.1-1234");
    Mockito.when(m_repositoryVersion.getStackId()).thenReturn(new StackId("HDP", "2.3"));

    m_services.clear();

    Mockito.when(m_repositoryVersion.getType()).thenReturn(RepositoryType.STANDARD);
    Mockito.when(m_repositoryVersion.getRepositoryXml()).thenReturn(m_vdfXml);
    Mockito.when(m_vdfXml.getClusterSummary(Mockito.any(Cluster.class))).thenReturn(m_clusterVersionSummary);
    Mockito.when(m_clusterVersionSummary.getAvailableServiceNames()).thenReturn(m_services.keySet());
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

    Mockito.when(cluster.getServices()).thenReturn(m_services);

    PrereqCheckRequest request = new PrereqCheckRequest("cluster");
    request.setSourceStackId(new StackId("HDP", "2.3.0.0"));
    request.setTargetRepositoryVersion(m_repositoryVersion);

    // MAPREDUCE2 not installed
    Assert.assertFalse(m_check.isApplicable(request));

    // MAPREDUCE2 installed
    m_services.put("MAPREDUCE2", Mockito.mock(Service.class));
    Assert.assertTrue(m_check.isApplicable(request));
  }

  @Test
  public void testPerform() throws Exception {
    final Cluster cluster = Mockito.mock(Cluster.class);
    Mockito.when(cluster.getClusterId()).thenReturn(1L);
    Mockito.when(m_clusters.getCluster("cluster")).thenReturn(cluster);

    final DesiredConfig desiredConfig = Mockito.mock(DesiredConfig.class);
    Mockito.when(desiredConfig.getTag()).thenReturn("tag");
    Map<String, DesiredConfig> configMap = new HashMap<>();
    configMap.put("mapred-site", desiredConfig);
    configMap.put("yarn-site", desiredConfig);

    Mockito.when(cluster.getDesiredConfigs()).thenReturn(configMap);
    final Config config = Mockito.mock(Config.class);
    Mockito.when(cluster.getConfig(Mockito.anyString(), Mockito.anyString())).thenReturn(config);
    final Map<String, String> properties = new HashMap<>();
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
    Mockito.when(m_clusters.getCluster("c1")).thenReturn(cluster);
    PrereqCheckRequest request = new PrereqCheckRequest("c1");
    request.setTargetRepositoryVersion(m_repositoryVersion);

    Mockito.when(m_repositoryVersion.getVersion()).thenReturn("2.0.0.1");

    // MAPREDUCE2 installed
    m_services.put("MAPREDUCE2", Mockito.mock(Service.class));

    boolean isApplicable = m_check.isApplicable(request);
    Assert.assertTrue(isApplicable);
  }
}
