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
import java.util.LinkedHashSet;
import java.util.Map;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.RepositoryType;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
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
 * Tests {@link HiveMultipleMetastoreCheck}
 */
@RunWith(MockitoJUnitRunner.class)
public class HiveMultipleMetastoreCheckTest {
  private final Clusters m_clusters = Mockito.mock(Clusters.class);
  private final HiveMultipleMetastoreCheck m_check = new HiveMultipleMetastoreCheck();
  private final RepositoryVersionDAO repositoryVersionDAO = Mockito.mock(
      RepositoryVersionDAO.class);

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

    Mockito.when(m_repositoryVersion.getVersion()).thenReturn("1.0.0.0-1234");
    Mockito.when(m_repositoryVersion.getStackId()).thenReturn(new StackId("HDP", "1.0"));

    m_services.clear();

    Mockito.when(m_repositoryVersion.getType()).thenReturn(RepositoryType.STANDARD);
    Mockito.when(m_repositoryVersion.getRepositoryXml()).thenReturn(m_vdfXml);
    Mockito.when(m_vdfXml.getClusterSummary(Mockito.any(Cluster.class))).thenReturn(m_clusterVersionSummary);
    Mockito.when(m_clusterVersionSummary.getAvailableServiceNames()).thenReturn(m_services.keySet());
  }

  /**
   * Tests that the check is applicable when hive is installed.
   *
   * @throws Exception
   */
  @Test
  public void testIsApplicable() throws Exception {
    final Cluster cluster = Mockito.mock(Cluster.class);

    Mockito.when(cluster.getClusterId()).thenReturn(1L);
    Mockito.when(m_clusters.getCluster("cluster")).thenReturn(cluster);
    Mockito.when(cluster.getServices()).thenReturn(m_services);

    m_services.put("HDFS", Mockito.mock(Service.class));

    PrereqCheckRequest request = new PrereqCheckRequest("cluster");
    request.setTargetRepositoryVersion(m_repositoryVersion);

    // HIVE not installed
    Assert.assertFalse(m_check.isApplicable(request));

    // install HIVE
    m_services.put("HIVE", Mockito.mock(Service.class));

    m_check.repositoryVersionDaoProvider = new Provider<RepositoryVersionDAO>() {
      @Override
      public RepositoryVersionDAO get() {
        return repositoryVersionDAO;
      }
    };

    Mockito.when(repositoryVersionDAO.findByStackNameAndVersion(Mockito.anyString(),
        Mockito.anyString())).thenReturn(m_repositoryVersion);

    // HIVE installed
    Assert.assertTrue(m_check.isApplicable(request));
  }

  /**
   * Tests that the warning is correctly tripped when there are not enough
   * metastores.
   *
   * @throws Exception
   */
  @Test
  public void testPerform() throws Exception {
    final Cluster cluster = Mockito.mock(Cluster.class);
    Service hive = Mockito.mock(Service.class);
    ServiceComponent metastore = Mockito.mock(ServiceComponent.class);

    Mockito.when(cluster.getClusterId()).thenReturn(1L);
    Mockito.when(m_clusters.getCluster("cluster")).thenReturn(cluster);
    Mockito.when(cluster.getService("HIVE")).thenReturn(hive);

    Mockito.when(hive.getServiceComponent("HIVE_METASTORE")).thenReturn(metastore);

    Map<String, ServiceComponentHost> metastores = new HashMap<>();
    Mockito.when(metastore.getServiceComponentHosts()).thenReturn(metastores);

    PrerequisiteCheck check = new PrerequisiteCheck(null, null);
    PrereqCheckRequest request = new PrereqCheckRequest("cluster");
    request.setTargetRepositoryVersion(m_repositoryVersion);
    m_check.perform(check, request);

    Assert.assertEquals(PrereqCheckStatus.WARNING, check.getStatus());

    metastores.put("c6401", Mockito.mock(ServiceComponentHost.class));
    metastores.put("c6402", Mockito.mock(ServiceComponentHost.class));

    check = new PrerequisiteCheck(null, null);
    m_check.perform(check, request);

    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());
  }

  @Test
  public void testPerformFail() throws Exception{
    final Cluster cluster = Mockito.mock(Cluster.class);
    final LinkedHashSet<String> failedOnExpected = new LinkedHashSet<>();
    Service hive = Mockito.mock(Service.class);
    ServiceComponent metastore = Mockito.mock(ServiceComponent.class);
    Map<String, ServiceComponentHost> metastores = new HashMap<>();

    failedOnExpected.add("HIVE");

    Mockito.when(cluster.getClusterId()).thenReturn(1L);
    Mockito.when(m_clusters.getCluster("cluster")).thenReturn(cluster);
    Mockito.when(cluster.getService("HIVE")).thenReturn(hive);
    Mockito.when(hive.getServiceComponent("HIVE_METASTORE")).thenReturn(metastore);
    Mockito.when(metastore.getServiceComponentHosts()).thenReturn(metastores);

    PrerequisiteCheck check = new PrerequisiteCheck(null, null);
    PrereqCheckRequest request = new PrereqCheckRequest("cluster");
    request.setTargetRepositoryVersion(m_repositoryVersion);
    m_check.perform(check, request);

    Assert.assertEquals(PrereqCheckStatus.WARNING, check.getStatus());

    check = new PrerequisiteCheck(null, null);
    m_check.perform(check, request);
    Assert.assertEquals(failedOnExpected, check.getFailedOn());
    Assert.assertEquals(PrereqCheckStatus.WARNING, check.getStatus());
  }
}
