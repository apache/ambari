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

import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.RepositoryType;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.repository.ClusterVersionSummary;
import org.apache.ambari.server.state.repository.VersionDefinitionXml;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.inject.Provider;

/**
 * Tests {@link StormShutdownWarning}.
 */
@RunWith(MockitoJUnitRunner.class)
public class StormShutdownWarningTest extends EasyMockSupport {

  private final String m_clusterName = "c1";
  private final Clusters m_clusters = niceMock(Clusters.class);

  @Mock
  private ClusterVersionSummary m_clusterVersionSummary;

  @Mock
  private VersionDefinitionXml m_vdfXml;

  @Mock
  private RepositoryVersionEntity m_repositoryVersion;

  final Map<String, Service> m_services = new HashMap<>();

  @Before
  public void setup() throws Exception {
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
    final StormShutdownWarning shutdownWarning = new StormShutdownWarning();
    shutdownWarning.clustersProvider = new Provider<Clusters>() {

      @Override
      public Clusters get() {
        return m_clusters;
      }
    };

    final Cluster cluster = niceMock(Cluster.class);
    final Service hive = niceMock(Service.class);

    m_services.put("STORM", hive);

    EasyMock.expect(cluster.getClusterId()).andReturn(1L).anyTimes();

    EasyMock.expect(cluster.getCurrentStackVersion()).andReturn(new StackId("HDP", "2.3")).anyTimes();
    EasyMock.expect(cluster.getServices()).andReturn(m_services).anyTimes();
    EasyMock.expect(m_clusters.getCluster(m_clusterName)).andReturn(cluster).atLeastOnce();

    PrereqCheckRequest request = niceMock(PrereqCheckRequest.class);
    EasyMock.expect(request.getClusterName()).andReturn(m_clusterName).anyTimes();
    EasyMock.expect(request.getUpgradeType()).andReturn(UpgradeType.ROLLING).anyTimes();
    EasyMock.expect(request.getTargetRepositoryVersion()).andReturn(m_repositoryVersion).anyTimes();

    replayAll();

    Assert.assertTrue(shutdownWarning.isApplicable(request));

    verifyAll();
  }

  /**
   * @throws Exception
   */
  @Test
  public void testPerform() throws Exception {
    final StormShutdownWarning shutdownWarning = new StormShutdownWarning();

    PrereqCheckRequest request = new PrereqCheckRequest(m_clusterName);
    PrerequisiteCheck check = new PrerequisiteCheck(null, null);

    shutdownWarning.perform(check, request);
    Assert.assertEquals(PrereqCheckStatus.WARNING, check.getStatus());
  }
}
