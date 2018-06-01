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

import static org.apache.ambari.server.checks.AmbariMetricsHadoopSinkVersionCompatibilityCheck.MIN_HADOOP_SINK_VERSION_PROPERTY_NAME;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.controller.internal.AbstractControllerResourceProvider;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.RequestDAO;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.RequestEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.RepositoryType;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.repository.ClusterVersionSummary;
import org.apache.ambari.server.state.repository.VersionDefinitionXml;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.ambari.server.state.stack.UpgradePack;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.inject.Provider;

@RunWith(PowerMockRunner.class)
@PrepareForTest ({AmbariServer.class, AbstractControllerResourceProvider.class, PropertyHelper.class})
public class AmbariMetricsHadoopSinkVersionCheckTest {
  private final Clusters m_clusters = Mockito.mock(Clusters.class);
  private final AmbariMetricsHadoopSinkVersionCompatibilityCheck m_check = new AmbariMetricsHadoopSinkVersionCompatibilityCheck();
  private final RepositoryVersionDAO repositoryVersionDAO = Mockito.mock(
    RepositoryVersionDAO.class);

  private ClusterVersionSummary m_clusterVersionSummary;

  private VersionDefinitionXml m_vdfXml;

  private RepositoryVersionEntity m_repositoryVersion;

  final Map<String, Service> m_services = new HashMap<>();

  /**
   *
   */
  @Before
  public void setup() throws Exception {

    m_repositoryVersion = Mockito.mock(RepositoryVersionEntity.class);
    m_clusterVersionSummary = Mockito.mock(ClusterVersionSummary.class);
    m_vdfXml = Mockito.mock(VersionDefinitionXml.class);
    MockitoAnnotations.initMocks(this);

    m_check.clustersProvider = new Provider<Clusters>() {

      @Override
      public Clusters get() {
        return m_clusters;
      }
    };
    Configuration config = Mockito.mock(Configuration.class);
    m_check.config = config;

    when(m_repositoryVersion.getVersion()).thenReturn("3.0.0.0-1234");
    when(m_repositoryVersion.getStackId()).thenReturn(new StackId("HDP", "3.0"));

    m_services.clear();

    when(m_repositoryVersion.getType()).thenReturn(RepositoryType.STANDARD);
    when(m_repositoryVersion.getRepositoryXml()).thenReturn(m_vdfXml);
    when(m_vdfXml.getClusterSummary(Mockito.any(Cluster.class))).thenReturn(m_clusterVersionSummary);
    when(m_clusterVersionSummary.getAvailableServiceNames()).thenReturn(m_services.keySet());

  }

  /**
   * Tests that the check is applicable when hive is installed.
   *
   * @throws Exception
   */
  @Test
  public void testIsApplicable() throws Exception {
    final Cluster cluster = Mockito.mock(Cluster.class);

    when(cluster.getClusterId()).thenReturn(1L);
    when(m_clusters.getCluster("cluster")).thenReturn(cluster);
    when(cluster.getServices()).thenReturn(m_services);

    m_services.put("HIVE", Mockito.mock(Service.class));

    PrereqCheckRequest request = new PrereqCheckRequest("cluster");
    request.setTargetRepositoryVersion(m_repositoryVersion);

    Assert.assertFalse(m_check.isApplicable(request));

    m_services.put("HDFS", Mockito.mock(Service.class));

    m_check.repositoryVersionDaoProvider = new Provider<RepositoryVersionDAO>() {
      @Override
      public RepositoryVersionDAO get() {
        return repositoryVersionDAO;
      }
    };

    when(repositoryVersionDAO.findByStackNameAndVersion(Mockito.anyString(),
      Mockito.anyString())).thenReturn(m_repositoryVersion);

    Assert.assertTrue(m_check.isApplicable(request));
  }

  /**
   * Tests that the warning is correctly tripped when there are not enough
   * metastores.
   *
   * @throws Exception
   */
  @Test(timeout = 60000)
  public void testPerform() throws Exception {

    AmbariManagementController ambariManagementControllerMock = Mockito.mock(AmbariManagementController.class);
    PowerMockito.mockStatic(AmbariServer.class);
    when(AmbariServer.getController()).thenReturn(ambariManagementControllerMock);

    ResourceProvider resourceProviderMock = mock(ResourceProvider.class);
    PowerMockito.mockStatic(AbstractControllerResourceProvider.class);
    when(AbstractControllerResourceProvider.getResourceProvider(eq(Resource.Type.Request), any(AmbariManagementController.class))).thenReturn(resourceProviderMock);

    PowerMockito.mockStatic(PropertyHelper.class);
    Request requestMock = mock(Request.class);
    when(PropertyHelper.getCreateRequest(any(), any())).thenReturn(requestMock);
    when(PropertyHelper.getPropertyId("Requests", "id")).thenReturn("requestIdProp");

    RequestStatus requestStatusMock = mock(RequestStatus.class);
    Resource responseResourceMock = mock(Resource.class);
    when(resourceProviderMock.createResources(requestMock)).thenReturn(requestStatusMock);
    when(requestStatusMock.getRequestResource()).thenReturn(responseResourceMock);
    when(responseResourceMock.getPropertyValue(anyString())).thenReturn(100l);

    Clusters clustersMock = mock(Clusters.class);
    when(ambariManagementControllerMock.getClusters()).thenReturn(clustersMock);
    Cluster clusterMock = mock(Cluster.class);
    when(clustersMock.getCluster("c1")).thenReturn(clusterMock);
    when(clusterMock.getHosts(eq("AMBARI_METRICS"), eq("METRICS_MONITOR"))).thenReturn(Collections.singleton("h1"));

    RequestDAO requestDAOMock = mock(RequestDAO.class);
    RequestEntity requestEntityMock  = mock(RequestEntity.class);
    when(requestDAOMock.findByPks(Collections.singleton(100l), true)).thenReturn(Collections.singletonList(requestEntityMock));
    when(requestEntityMock.getStatus()).thenReturn(HostRoleStatus.IN_PROGRESS).thenReturn(HostRoleStatus.COMPLETED);

    Field requestDaoField = m_check.getClass().getDeclaredField("requestDAO");
    requestDaoField.setAccessible(true);
    requestDaoField.set(m_check, requestDAOMock);

    PrerequisiteCheck check = new PrerequisiteCheck(null, "c1");
    PrereqCheckRequest request = new PrereqCheckRequest("c1");
    UpgradePack.PrerequisiteCheckConfig prerequisiteCheckConfig = new UpgradePack.PrerequisiteCheckConfig();
    UpgradePack.PrerequisiteProperty prerequisiteProperty = new UpgradePack.PrerequisiteProperty();
    prerequisiteProperty.name = MIN_HADOOP_SINK_VERSION_PROPERTY_NAME;
    prerequisiteProperty.value = "2.7.0.0";
    UpgradePack.PrerequisiteCheckProperties prerequisiteCheckProperties = new UpgradePack.PrerequisiteCheckProperties();
    prerequisiteCheckProperties.name = "org.apache.ambari.server.checks.AmbariMetricsHadoopSinkVersionCompatibilityCheck";
    prerequisiteCheckProperties.properties = Collections.singletonList(prerequisiteProperty);
    prerequisiteCheckConfig.prerequisiteCheckProperties = Collections.singletonList(prerequisiteCheckProperties);
    request.setPrerequisiteCheckConfig(prerequisiteCheckConfig);
    request.setTargetRepositoryVersion(m_repositoryVersion);
    m_check.perform(check, request);

    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());
  }

  @Test(timeout = 60000)
  public void testPerformFail() throws Exception{
    AmbariManagementController ambariManagementControllerMock = Mockito.mock(AmbariManagementController.class);
    PowerMockito.mockStatic(AmbariServer.class);
    when(AmbariServer.getController()).thenReturn(ambariManagementControllerMock);

    ResourceProvider resourceProviderMock = mock(ResourceProvider.class);
    PowerMockito.mockStatic(AbstractControllerResourceProvider.class);
    when(AbstractControllerResourceProvider.getResourceProvider(eq(Resource.Type.Request), any(AmbariManagementController.class))).thenReturn(resourceProviderMock);

    PowerMockito.mockStatic(PropertyHelper.class);
    Request requestMock = mock(Request.class);
    when(PropertyHelper.getCreateRequest(any(), any())).thenReturn(requestMock);
    when(PropertyHelper.getPropertyId("Requests", "id")).thenReturn("requestIdProp");

    RequestStatus requestStatusMock = mock(RequestStatus.class);
    Resource responseResourceMock = mock(Resource.class);
    when(resourceProviderMock.createResources(requestMock)).thenReturn(requestStatusMock);
    when(requestStatusMock.getRequestResource()).thenReturn(responseResourceMock);
    when(responseResourceMock.getPropertyValue(anyString())).thenReturn(101l);

    Clusters clustersMock = mock(Clusters.class);
    when(ambariManagementControllerMock.getClusters()).thenReturn(clustersMock);
    Cluster clusterMock = mock(Cluster.class);
    when(clustersMock.getCluster("c1")).thenReturn(clusterMock);
    when(clusterMock.getHosts(eq("AMBARI_METRICS"), eq("METRICS_MONITOR"))).thenReturn(Collections.singleton("h1_fail"));

    RequestDAO requestDAOMock = mock(RequestDAO.class);
    RequestEntity requestEntityMock  = mock(RequestEntity.class);
    when(requestDAOMock.findByPks(Collections.singleton(101l), true)).thenReturn(Collections.singletonList(requestEntityMock));
    when(requestEntityMock.getStatus()).thenReturn(HostRoleStatus.IN_PROGRESS).thenReturn(HostRoleStatus.FAILED);

    Field requestDaoField = m_check.getClass().getDeclaredField("requestDAO");
    requestDaoField.setAccessible(true);
    requestDaoField.set(m_check, requestDAOMock);


    when(requestEntityMock.getRequestId()).thenReturn(101l);
    HostRoleCommandDAO hostRoleCommandDAOMock = mock(HostRoleCommandDAO.class);
    HostRoleCommandEntity hrcEntityMock  = mock(HostRoleCommandEntity.class);
    when(hostRoleCommandDAOMock.findByRequest(101l, true)).thenReturn(Collections.singletonList(hrcEntityMock));
    when(hrcEntityMock.getStatus()).thenReturn(HostRoleStatus.FAILED);
    when(hrcEntityMock.getHostName()).thenReturn("h1_fail");

    Field hrcDaoField = m_check.getClass().getDeclaredField("hostRoleCommandDAO");
    hrcDaoField.setAccessible(true);
    hrcDaoField.set(m_check, hostRoleCommandDAOMock);

    PrerequisiteCheck check = new PrerequisiteCheck(null, "c1");
    PrereqCheckRequest request = new PrereqCheckRequest("c1");
    UpgradePack.PrerequisiteCheckConfig prerequisiteCheckConfig = new UpgradePack.PrerequisiteCheckConfig();
    UpgradePack.PrerequisiteProperty prerequisiteProperty = new UpgradePack.PrerequisiteProperty();
    prerequisiteProperty.name = MIN_HADOOP_SINK_VERSION_PROPERTY_NAME;
    prerequisiteProperty.value = "2.7.0.0";
    UpgradePack.PrerequisiteCheckProperties prerequisiteCheckProperties = new UpgradePack.PrerequisiteCheckProperties();
    prerequisiteCheckProperties.name = "org.apache.ambari.server.checks.AmbariMetricsHadoopSinkVersionCompatibilityCheck";
    prerequisiteCheckProperties.properties = Collections.singletonList(prerequisiteProperty);
    prerequisiteCheckConfig.prerequisiteCheckProperties = Collections.singletonList(prerequisiteCheckProperties);
    request.setPrerequisiteCheckConfig(prerequisiteCheckConfig);
    request.setTargetRepositoryVersion(m_repositoryVersion);
    m_check.perform(check, request);

    Assert.assertEquals(PrereqCheckStatus.FAIL, check.getStatus());
    Assert.assertTrue(check.getFailReason().contains("upgrade 'ambari-metrics-hadoop-sink'"));
    Assert.assertEquals(check.getFailedOn().size(), 1);
    Assert.assertTrue(check.getFailedOn().iterator().next().contains("h1_fail"));
  }
}
