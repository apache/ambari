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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.RepositoryType;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.repository.ClusterVersionSummary;
import org.apache.ambari.server.state.repository.VersionDefinitionXml;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.inject.Provider;

/**
 * Unit tests for ServicesNamenodeTruncateCheck
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class ServicesNamenodeTruncateCheckTest {

  private Clusters m_clusters = EasyMock.createMock(Clusters.class);
  private ServicesNamenodeTruncateCheck m_check = new ServicesNamenodeTruncateCheck();
  private final Map<String, String> m_configMap = new HashMap<>();

  @Mock
  private ClusterVersionSummary m_clusterVersionSummary;

  @Mock
  private VersionDefinitionXml m_vdfXml;

  @Mock
  private RepositoryVersionEntity m_repositoryVersion;

  final Map<String, Service> m_services = new HashMap<>();

  @Before
  public void setup() throws Exception {
    Cluster cluster = EasyMock.createMock(Cluster.class);

    Config config = EasyMock.createMock(Config.class);
    final Service service = Mockito.mock(Service.class);

    m_services.clear();
    m_services.put("HDFS", service);

    expect(cluster.getServices()).andReturn(m_services).anyTimes();
    expect(config.getProperties()).andReturn(m_configMap).anyTimes();
    expect(cluster.getService("HDFS")).andReturn(service);
    expect(cluster.getDesiredConfigByType("hdfs-site")).andReturn(config).anyTimes();
    expect(m_clusters.getCluster((String) anyObject())).andReturn(cluster).anyTimes();

    replay(m_clusters, cluster, config);

    Configuration configuration = EasyMock.createMock(Configuration.class);
    replay(configuration);
    m_check.config = configuration;

    m_check.clustersProvider = new Provider<Clusters>() {
      @Override
      public Clusters get() {
        return m_clusters;
      }
    };

    Mockito.when(m_repositoryVersion.getType()).thenReturn(RepositoryType.STANDARD);
    Mockito.when(m_repositoryVersion.getVersion()).thenReturn("HDP-2.2.0.0");
    Mockito.when(m_repositoryVersion.getStackId()).thenReturn(new StackId("HDP", "2.2.0"));

    Mockito.when(m_repositoryVersion.getRepositoryXml()).thenReturn(m_vdfXml);
    Mockito.when(m_vdfXml.getClusterSummary(Mockito.any(Cluster.class))).thenReturn(m_clusterVersionSummary);
    Mockito.when(m_clusterVersionSummary.getAvailableServiceNames()).thenReturn(m_services.keySet());
  }


  @Test
  public void testIsApplicable() throws Exception {

    PrereqCheckRequest checkRequest = new PrereqCheckRequest("c1");
    checkRequest.setSourceStackId(new StackId("HDP", "2.2"));
    checkRequest.setTargetRepositoryVersion(m_repositoryVersion);

    Assert.assertTrue(m_check.isApplicable(checkRequest));
  }

  @Test
  public void testPerform() throws Exception {
    PrerequisiteCheck check = new PrerequisiteCheck(null, null);
    PrereqCheckRequest request = new PrereqCheckRequest("c1");
    m_check.perform(check, request);
    assertEquals(PrereqCheckStatus.PASS, check.getStatus());

    // Check HDP-2.2.x => HDP-2.2.y is FAIL
    m_configMap.put("dfs.allow.truncate", "true");
    request.setSourceStackId(new StackId("HDP-2.2.4.2"));

    Mockito.when(m_repositoryVersion.getVersion()).thenReturn("2.2.8.4");
    Mockito.when(m_repositoryVersion.getStackId()).thenReturn(new StackId("HDP", "2.2.8.4"));
    request.setTargetRepositoryVersion(m_repositoryVersion);

    check = new PrerequisiteCheck(null, null);
    m_check.perform(check, request);
    assertEquals(PrereqCheckStatus.FAIL, check.getStatus());

    m_configMap.put("dfs.allow.truncate", "false");
    check = new PrerequisiteCheck(null, null);
    m_check.perform(check, request);
    assertEquals(PrereqCheckStatus.PASS, check.getStatus());

    // Check HDP-2.2.x => HDP-2.3.y is FAIL
    m_configMap.put("dfs.allow.truncate", "true");
    request.setSourceStackId(new StackId("HDP-2.2.4.2"));

    Mockito.when(m_repositoryVersion.getVersion()).thenReturn("2.3.8.4");
    Mockito.when(m_repositoryVersion.getStackId()).thenReturn(new StackId("HDP", "2.3.8.4"));
    request.setTargetRepositoryVersion(m_repositoryVersion);

    check = new PrerequisiteCheck(null, null);
    m_check.perform(check, request);
    assertEquals(PrereqCheckStatus.FAIL, check.getStatus());

    m_configMap.put("dfs.allow.truncate", "false");
    check = new PrerequisiteCheck(null, null);
    m_check.perform(check, request);
    assertEquals(PrereqCheckStatus.PASS, check.getStatus());
  }
}