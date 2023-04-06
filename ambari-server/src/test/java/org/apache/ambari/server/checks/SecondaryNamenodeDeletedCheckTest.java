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
import org.apache.ambari.server.orm.dao.HostComponentStateDAO;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.RepositoryType;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
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
 * Unit tests for SecondaryNamenodeDeletedCheck
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class SecondaryNamenodeDeletedCheckTest {
  private final Clusters clusters = Mockito.mock(Clusters.class);
  private final HostComponentStateDAO hostComponentStateDAO = Mockito.mock(HostComponentStateDAO.class);

  private final SecondaryNamenodeDeletedCheck secondaryNamenodeDeletedCheck = new SecondaryNamenodeDeletedCheck();

  @Mock
  private ClusterVersionSummary m_clusterVersionSummary;

  @Mock
  private VersionDefinitionXml m_vdfXml;

  @Mock
  private RepositoryVersionEntity m_repositoryVersion;

  final Map<String, Service> m_services = new HashMap<>();

  @Before
  public void setup() throws Exception {
    secondaryNamenodeDeletedCheck.clustersProvider = new Provider<Clusters>() {
      @Override
      public Clusters get() {
        return clusters;
      }
    };

    secondaryNamenodeDeletedCheck.ambariMetaInfo = new Provider<AmbariMetaInfo>() {
      @Override
      public AmbariMetaInfo get() {
        return Mockito.mock(AmbariMetaInfo.class);
      }
    };

    secondaryNamenodeDeletedCheck.hostComponentStateDao = hostComponentStateDAO;
    Configuration config = Mockito.mock(Configuration.class);
    secondaryNamenodeDeletedCheck.config = config;

    m_services.clear();

    Mockito.when(m_repositoryVersion.getType()).thenReturn(RepositoryType.STANDARD);
    Mockito.when(m_repositoryVersion.getRepositoryXml()).thenReturn(m_vdfXml);
    Mockito.when(m_vdfXml.getClusterSummary(Mockito.any(Cluster.class))).thenReturn(m_clusterVersionSummary);
    Mockito.when(m_clusterVersionSummary.getAvailableServiceNames()).thenReturn(m_services.keySet());
  }

  @Test
  public void testIsApplicable() throws Exception {
    final Cluster cluster = Mockito.mock(Cluster.class);
    final Service service = Mockito.mock(Service.class);

    m_services.put("HDFS", service);

    Mockito.when(cluster.getClusterId()).thenReturn(1L);
    Mockito.when(cluster.getServices()).thenReturn(m_services);
    Mockito.when(clusters.getCluster("cluster")).thenReturn(cluster);

    PrereqCheckRequest request = new PrereqCheckRequest("cluster");
    request.setTargetRepositoryVersion(m_repositoryVersion);

    Assert.assertTrue(secondaryNamenodeDeletedCheck.isApplicable(request));

    request = new PrereqCheckRequest("cluster");
    request.setTargetRepositoryVersion(m_repositoryVersion);

    request.addResult(CheckDescription.SERVICES_NAMENODE_HA, PrereqCheckStatus.FAIL);
    Assert.assertFalse(secondaryNamenodeDeletedCheck.isApplicable(request));

    request.addResult(CheckDescription.SERVICES_NAMENODE_HA, PrereqCheckStatus.PASS);
    Assert.assertTrue(secondaryNamenodeDeletedCheck.isApplicable(request));

    m_services.remove("HDFS");
    Assert.assertFalse(secondaryNamenodeDeletedCheck.isApplicable(request));
  }

  @Test
  public void testPerform() throws Exception {
    final Cluster cluster = Mockito.mock(Cluster.class);
    Mockito.when(cluster.getClusterId()).thenReturn(1L);
    Mockito.when(clusters.getCluster("cluster")).thenReturn(cluster);

    final Service service = Mockito.mock(Service.class);
    final ServiceComponent serviceComponent = Mockito.mock(ServiceComponent.class);
    Mockito.when(cluster.getService("HDFS")).thenReturn(service);
    Mockito.when(service.getServiceComponent("SECONDARY_NAMENODE")).thenReturn(serviceComponent);
    Mockito.when(serviceComponent.getServiceComponentHosts()).thenReturn(Collections.singletonMap("host", null));

    PrerequisiteCheck check = new PrerequisiteCheck(null, null);
    secondaryNamenodeDeletedCheck.perform(check, new PrereqCheckRequest("cluster"));
    Assert.assertEquals(PrereqCheckStatus.FAIL, check.getStatus());
    Assert.assertEquals("HDFS", check.getFailedOn().toArray(new String[1])[0]);
    Assert.assertEquals("The SNameNode component must be deleted from host: host.", check.getFailReason());

    Mockito.when(serviceComponent.getServiceComponentHosts()).thenReturn(Collections.emptyMap());
    check = new PrerequisiteCheck(null, null);
    secondaryNamenodeDeletedCheck.perform(check, new PrereqCheckRequest("cluster"));
    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());
  }
}
