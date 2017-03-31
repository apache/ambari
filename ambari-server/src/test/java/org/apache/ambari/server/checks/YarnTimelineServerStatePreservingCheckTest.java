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
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.inject.Provider;

/**
 * Tests for {@link YarnTimelineServerStatePreservingCheckTest}
 * @Deprecated
 */
@Ignore
public class YarnTimelineServerStatePreservingCheckTest {
  private final Clusters m_clusters = Mockito.mock(Clusters.class);

  private final YarnTimelineServerStatePreservingCheck m_check = new YarnTimelineServerStatePreservingCheck();

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
    Mockito.when(cluster.getCurrentStackVersion()).thenReturn(new StackId("HDP-2.2"));

    Map<String, Service> services = new HashMap<String, Service>();
    Mockito.when(cluster.getServices()).thenReturn(services);

    ClusterVersionEntity clusterVersionEntity = Mockito.mock(ClusterVersionEntity.class);
    Mockito.when(cluster.getCurrentClusterVersion()).thenReturn(clusterVersionEntity);

    RepositoryVersionEntity repositoryVersionEntity = Mockito.mock(RepositoryVersionEntity.class);
    Mockito.when(clusterVersionEntity.getRepositoryVersion()).thenReturn(repositoryVersionEntity);
    Mockito.when(repositoryVersionEntity.getVersion()).thenReturn("2.2.4.2");

    Map<String, String> checkProperties = new HashMap<String, String>();
    checkProperties.put("min-applicable-stack-version","HDP-2.2.4.2");
    PrerequisiteCheckConfig prerequisiteCheckConfig = Mockito.mock(PrerequisiteCheckConfig.class);
    Mockito.when(prerequisiteCheckConfig.getCheckProperties(
        m_check.getClass().getName())).thenReturn(checkProperties);

    PrereqCheckRequest request = new PrereqCheckRequest("cluster");
    request.setRepositoryVersion("2.3.0.0");
    request.setPrerequisiteCheckConfig(prerequisiteCheckConfig);

    // YARN not installed
    Assert.assertFalse(m_check.isApplicable(request));

    // YARN installed
    services.put("YARN", Mockito.mock(Service.class));
    Assert.assertTrue(m_check.isApplicable(request));

    Mockito.when(repositoryVersionEntity.getVersion()).thenReturn("2.2.0.0");
    Assert.assertFalse(m_check.isApplicable(request));

    Mockito.when(repositoryVersionEntity.getVersion()).thenReturn("2.2.4.2");
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
    configMap.put("yarn-site", desiredConfig);
    configMap.put("core-site", desiredConfig);

    Mockito.when(cluster.getDesiredConfigs()).thenReturn(configMap);
    final Config config = Mockito.mock(Config.class);
    Mockito.when(cluster.getConfig(Mockito.anyString(), Mockito.anyString())).thenReturn(config);
    final Map<String, String> properties = new HashMap<String, String>();
    Mockito.when(config.getProperties()).thenReturn(properties);

    PrerequisiteCheck check = new PrerequisiteCheck(null, null);
    m_check.perform(check, new PrereqCheckRequest("cluster"));
    Assert.assertEquals(PrereqCheckStatus.FAIL, check.getStatus());

    properties.put("yarn.timeline-service.recovery.enabled", "true");
    check = new PrerequisiteCheck(null, null);
    m_check.perform(check, new PrereqCheckRequest("cluster"));
    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());
  }

  @SuppressWarnings("serial")
  @Test
  public void testIsApplicableMinimumHDPStackVersion() throws Exception {
    final Cluster cluster = Mockito.mock(Cluster.class);
    Mockito.when(cluster.getClusterId()).thenReturn(1L);
    Mockito.when(cluster.getServices()).thenReturn(new HashMap<String, Service>() {
      {
        put("YARN", null);
      }
    });
    Mockito.when(cluster.getCurrentStackVersion()).thenReturn(new StackId("HDP-2.2"));
    ClusterVersionEntity clusterVersionEntity = Mockito.mock(ClusterVersionEntity.class);
    Mockito.when(cluster.getCurrentClusterVersion()).thenReturn(clusterVersionEntity);
    RepositoryVersionEntity repositoryVersionEntity = Mockito.mock(RepositoryVersionEntity.class);
    Mockito.when(clusterVersionEntity.getRepositoryVersion()).thenReturn(repositoryVersionEntity);
    Mockito.when(m_clusters.getCluster("c1")).thenReturn(cluster);

    Map<String, String> checkProperties = new HashMap<String, String>();
    checkProperties.put("min-applicable-stack-version","HDP-2.2.4.2");
    PrerequisiteCheckConfig prerequisiteCheckConfig = Mockito.mock(PrerequisiteCheckConfig.class);
    Mockito.when(prerequisiteCheckConfig.getCheckProperties(
        m_check.getClass().getName())).thenReturn(checkProperties);

    PrereqCheckRequest request = new PrereqCheckRequest("c1");
    request.setPrerequisiteCheckConfig(prerequisiteCheckConfig);

    // Check < 2.2.4.2
    Mockito.when(repositoryVersionEntity.getVersion()).thenReturn("2.1.1.1");
    boolean isApplicable = m_check.isApplicable(request);
    Assert.assertFalse(isApplicable);
    Mockito.when(repositoryVersionEntity.getVersion()).thenReturn("2.2.4.1");
    isApplicable = m_check.isApplicable(request);
    Assert.assertFalse(isApplicable);

    // Check == 2.2.4.2
    Mockito.when(repositoryVersionEntity.getVersion()).thenReturn("2.2.4.2");
    isApplicable = m_check.isApplicable(request);
    Assert.assertTrue(isApplicable);

    // Check > 2.2.4.2
    Mockito.when(repositoryVersionEntity.getVersion()).thenReturn("2.2.4.4");
    isApplicable = m_check.isApplicable(request);
    Assert.assertTrue(isApplicable);
    Mockito.when(repositoryVersionEntity.getVersion()).thenReturn("2.3.1.1");
    isApplicable = m_check.isApplicable(request);
    Assert.assertTrue(isApplicable);
  }

  @SuppressWarnings("serial")
  @Test
  public void testIsApplicableMinimumStackVersion() throws Exception {
    final Cluster cluster = Mockito.mock(Cluster.class);
    Mockito.when(cluster.getClusterId()).thenReturn(1L);
    Mockito.when(cluster.getServices()).thenReturn(new HashMap<String, Service>() {
      {
        put("YARN", null);
      }
    });
    Mockito.when(cluster.getCurrentStackVersion()).thenReturn(new StackId("MYSTACK-12.2"));
    ClusterVersionEntity clusterVersionEntity = Mockito.mock(ClusterVersionEntity.class);
    Mockito.when(cluster.getCurrentClusterVersion()).thenReturn(clusterVersionEntity);
    RepositoryVersionEntity repositoryVersionEntity = Mockito.mock(RepositoryVersionEntity.class);
    Mockito.when(clusterVersionEntity.getRepositoryVersion()).thenReturn(repositoryVersionEntity);
    Mockito.when(m_clusters.getCluster("c1")).thenReturn(cluster);

    Map<String, String> checkProperties = new HashMap<String, String>();
    checkProperties.put("min-applicable-stack-version", "HDP-2.2.4.2");
    PrerequisiteCheckConfig prerequisiteCheckConfig = Mockito.mock(PrerequisiteCheckConfig.class);
    Mockito.when(prerequisiteCheckConfig.getCheckProperties(
        m_check.getClass().getName())).thenReturn(checkProperties);

    PrereqCheckRequest request = new PrereqCheckRequest("c1");
    request.setPrerequisiteCheckConfig(prerequisiteCheckConfig);

    Mockito.when(repositoryVersionEntity.getVersion()).thenReturn("2.3.0.1");
    boolean isApplicable = m_check.isApplicable(request);
    Assert.assertTrue(isApplicable);
  }
}
