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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.RepositoryType;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
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
 * Unit tests for ServicesMaintenanceModeCheck
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class ServicesMaintenanceModeCheckTest {
  private final Clusters clusters = Mockito.mock(Clusters.class);

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
    m_services.clear();

    Mockito.when(m_repositoryVersion.getType()).thenReturn(RepositoryType.STANDARD);
    Mockito.when(m_repositoryVersion.getVersion()).thenReturn("2.2.0.0-1234");
    Mockito.when(m_repositoryVersion.getStackId()).thenReturn(new StackId("HDP", "2.2"));
    Mockito.when(m_repositoryVersion.getRepositoryXml()).thenReturn(m_vdfXml);
    Mockito.when(m_vdfXml.getClusterSummary(Mockito.any(Cluster.class))).thenReturn(m_clusterVersionSummary);
    Mockito.when(m_clusterVersionSummary.getAvailableServiceNames()).thenReturn(m_services.keySet());
  }

  @Test
  public void testIsApplicable() throws Exception {
    PrereqCheckRequest checkRequest = new PrereqCheckRequest("c1");
    checkRequest.setSourceStackId(new StackId("HDP", "2.2"));
    checkRequest.setTargetRepositoryVersion(m_repositoryVersion);

    ServicesMaintenanceModeCheck smmc = new ServicesMaintenanceModeCheck();
    Configuration config = Mockito.mock(Configuration.class);
    smmc.config = config;
    Assert.assertTrue(smmc.isApplicable(checkRequest));
  }

  @Test
  public void testPerform() throws Exception {
    final ServicesMaintenanceModeCheck servicesMaintenanceModeCheck = new ServicesMaintenanceModeCheck();
    servicesMaintenanceModeCheck.clustersProvider = new Provider<Clusters>() {

      @Override
      public Clusters get() {
        return clusters;
      }
    };

    servicesMaintenanceModeCheck.ambariMetaInfo = new Provider<AmbariMetaInfo>() {
      @Override
      public AmbariMetaInfo get() {
        return Mockito.mock(AmbariMetaInfo.class);
      }
    };

    final Cluster cluster = Mockito.mock(Cluster.class);
    Mockito.when(cluster.getClusterId()).thenReturn(1L);
    Mockito.when(cluster.getCurrentStackVersion()).thenReturn(new StackId("HDP", "2.2"));
    Mockito.when(clusters.getCluster("cluster")).thenReturn(cluster);
    final Service service = Mockito.mock(Service.class);
    Mockito.when(cluster.getServices()).thenReturn(Collections.singletonMap("service", service));
    Mockito.when(service.isClientOnlyService()).thenReturn(false);

    // We don't bother checking service desired state as it's performed by a separate check
    Mockito.when(service.getDesiredState()).thenReturn(State.UNKNOWN);
    PrerequisiteCheck check = new PrerequisiteCheck(null, null);

    PrereqCheckRequest request = new PrereqCheckRequest("cluster");
    request.setTargetRepositoryVersion(m_repositoryVersion);

    servicesMaintenanceModeCheck.perform(check, request);
    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());

    Mockito.when(service.getDesiredState()).thenReturn(State.STARTED);
    check = new PrerequisiteCheck(null, null);
    servicesMaintenanceModeCheck.perform(check, request);
    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());
  }
}
