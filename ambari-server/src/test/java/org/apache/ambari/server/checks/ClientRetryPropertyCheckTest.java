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
 * Tests for {@link ClientRetryPropertyCheckTest}
 */
@RunWith(MockitoJUnitRunner.class)
public class ClientRetryPropertyCheckTest {
  private final Clusters m_clusters = Mockito.mock(Clusters.class);

  private final ClientRetryPropertyCheck m_check = new ClientRetryPropertyCheck();

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
    Configuration config = Mockito.mock(Configuration.class);
    m_check.config = config;

    Mockito.when(m_repositoryVersion.getType()).thenReturn(RepositoryType.STANDARD);
    Mockito.when(m_repositoryVersion.getVersion()).thenReturn("2.3.0.0-1234");
    Mockito.when(m_repositoryVersion.getStackId()).thenReturn(new StackId("HDP", "2.3"));

    m_services.clear();
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

    Mockito.when(cluster.getServices()).thenReturn(m_services);

    PrereqCheckRequest request = new PrereqCheckRequest("cluster");
    request.setTargetRepositoryVersion(m_repositoryVersion);

    // nothing installed
    Assert.assertFalse(m_check.isApplicable(request));

    // HDFS installed
    m_services.put("HDFS", Mockito.mock(Service.class));
    Assert.assertTrue(m_check.isApplicable(request));

    // OOZIE installed
    m_services.clear();
    m_services.put("OOZIE", Mockito.mock(Service.class));
    Assert.assertTrue(m_check.isApplicable(request));
  }

  @Test
  public void testPerform() throws Exception {
    final Cluster cluster = Mockito.mock(Cluster.class);

    Mockito.when(cluster.getClusterId()).thenReturn(1L);
    Mockito.when(m_clusters.getCluster("cluster")).thenReturn(cluster);
    Map<String, Service> services = new HashMap<>();
    Mockito.when(cluster.getServices()).thenReturn(services);

    final DesiredConfig desiredConfig = Mockito.mock(DesiredConfig.class);
    Mockito.when(desiredConfig.getTag()).thenReturn("tag");
    Map<String, DesiredConfig> configMap = new HashMap<>();
    configMap.put("hdfs-site", desiredConfig);
    configMap.put("hive-site", desiredConfig);
    configMap.put("oozie-env", desiredConfig);

    Mockito.when(cluster.getDesiredConfigs()).thenReturn(configMap);
    final Config config = Mockito.mock(Config.class);
    Mockito.when(cluster.getConfig(Mockito.anyString(), Mockito.anyString())).thenReturn(config);
    final Map<String, String> properties = new HashMap<>();
    Mockito.when(config.getProperties()).thenReturn(properties);

    // Add HDFS
    services.put("HDFS", Mockito.mock(Service.class));

    // Add property that will fail
    properties.put("dfs.client.retry.policy.enabled", "true");

    PrerequisiteCheck check = new PrerequisiteCheck(null, null);
    m_check.perform(check, new PrereqCheckRequest("cluster"));
    Assert.assertEquals(PrereqCheckStatus.FAIL, check.getStatus());

    // Change property to pass
    properties.put("dfs.client.retry.policy.enabled", "false");
    check = new PrerequisiteCheck(null, null);
    m_check.perform(check, new PrereqCheckRequest("cluster"));
    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());

    // Add HIVE
    services.put("HIVE", Mockito.mock(Service.class));

    // Fail with bad property
    properties.put("hive.metastore.failure.retries", "0");
    check = new PrerequisiteCheck(null, null);
    m_check.perform(check, new PrereqCheckRequest("cluster"));
    Assert.assertEquals(PrereqCheckStatus.FAIL, check.getStatus());

    // Add hive retry in order to pass
    properties.put("hive.metastore.failure.retries", "5");
    check = new PrerequisiteCheck(null, null);
    m_check.perform(check, new PrereqCheckRequest("cluster"));
    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());

    // Add OOZIE
    services.put("OOZIE", Mockito.mock(Service.class));

    // Fail without property
    check = new PrerequisiteCheck(null, null);
    m_check.perform(check, new PrereqCheckRequest("cluster"));
    Assert.assertEquals(PrereqCheckStatus.FAIL, check.getStatus());

    // Fail without right property
    properties.put("template", "foo bar baz");
    check = new PrerequisiteCheck(null, null);
    m_check.perform(check, new PrereqCheckRequest("cluster"));
    Assert.assertEquals(PrereqCheckStatus.FAIL, check.getStatus());

    // Pass with right property
    properties.put("content", "foo bar baz -Doozie.connection.retry.count=5 foobarbaz");
    check = new PrerequisiteCheck(null, null);
    m_check.perform(check, new PrereqCheckRequest("cluster"));
    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());

  }
}