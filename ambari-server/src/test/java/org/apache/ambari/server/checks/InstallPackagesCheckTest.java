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

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.orm.dao.ClusterVersionDAO;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.ClusterVersionEntity;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.orm.models.HostComponentSummary;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.inject.Provider;

/**
 * Unit tests for InstallPackagesCheck
 *
 */
@RunWith(PowerMockRunner.class)               // Allow mocking static methods
@PrepareForTest(HostComponentSummary.class)   // This class has a static method that will be mocked
public class InstallPackagesCheckTest {
  private final Clusters clusters = Mockito.mock(Clusters.class);
  private final ClusterVersionDAO clusterVersionDAO = Mockito.mock(ClusterVersionDAO.class);
  private final HostVersionDAO hostVersionDAO = Mockito.mock(HostVersionDAO.class);
  private final RepositoryVersionDAO repositoryVersionDAO = Mockito.mock(RepositoryVersionDAO.class);
  private AmbariMetaInfo ambariMetaInfo = Mockito.mock(AmbariMetaInfo.class);
  private StackId sourceStackId = new StackId("HDP", "2.2");
  private StackId targetStackId = new StackId("HDP", "2.2");
  private String repositoryVersion = "2.2.6.0-1234";
  private String clusterName = "cluster";

  @Test
  public void testIsApplicable() throws Exception {
    PrereqCheckRequest checkRequest = new PrereqCheckRequest(clusterName);
    checkRequest.setRepositoryVersion(repositoryVersion);
    checkRequest.setSourceStackId(sourceStackId);
    checkRequest.setTargetStackId(targetStackId);
    InstallPackagesCheck ipc = new InstallPackagesCheck();
    Configuration config = Mockito.mock(Configuration.class);
    Mockito.when(config.getRollingUpgradeMinStack()).thenReturn("HDP-2.2");
    Mockito.when(config.getRollingUpgradeMaxStack()).thenReturn("");
    ipc.config = config;

    Assert.assertTrue(ipc.isApplicable(checkRequest));
  }

  @Test
  public void testPerform() throws Exception {
    StackId stackId = new StackId("HDP", "2.2");
    PowerMockito.mockStatic(HostComponentSummary.class);

    final InstallPackagesCheck installPackagesCheck = new InstallPackagesCheck();
    installPackagesCheck.clustersProvider = new Provider<Clusters>() {

      @Override
      public Clusters get() {
        return clusters;
      }
    };

    installPackagesCheck.ambariMetaInfo = new Provider<AmbariMetaInfo>() {
      @Override
      public AmbariMetaInfo get() {
        return ambariMetaInfo;
      }
    };

    installPackagesCheck.clusterVersionDAOProvider = new Provider<ClusterVersionDAO>() {
      @Override
      public ClusterVersionDAO get() {
        return clusterVersionDAO;
      }
    };

    installPackagesCheck.hostVersionDaoProvider = new Provider<HostVersionDAO>() {
      @Override
      public HostVersionDAO get() {
        return hostVersionDAO;
      }
    };

    installPackagesCheck.repositoryVersionDaoProvider = new Provider<RepositoryVersionDAO>() {
      @Override
      public RepositoryVersionDAO get() {
        return repositoryVersionDAO;
      }
    };
    StackEntity stack = new StackEntity();
    stack.setStackName(stackId.getStackName());
    stack.setStackVersion(stackId.getStackVersion());
    RepositoryVersionEntity rve = new RepositoryVersionEntity(stack, repositoryVersion, repositoryVersion, "rhel6");
    Mockito.when(repositoryVersionDAO.findByStackNameAndVersion(Mockito.anyString(), Mockito.anyString())).thenReturn(rve);
    final Cluster cluster = Mockito.mock(Cluster.class);
    Mockito.when(cluster.getClusterName()).thenReturn(clusterName);
    Mockito.when(cluster.getClusterId()).thenReturn(1L);

    Mockito.when(cluster.getCurrentStackVersion()).thenReturn(stackId);
    Mockito.when(clusters.getCluster(clusterName)).thenReturn(cluster);
    ClusterVersionEntity clusterVersionEntity = Mockito.mock(ClusterVersionEntity.class);
    Mockito.when(clusterVersionEntity.getState()).thenReturn(RepositoryVersionState.INSTALLED);
    Mockito.when(clusterVersionDAO.findByClusterAndStackAndVersion(
        clusterName, targetStackId, repositoryVersion)).thenReturn(clusterVersionEntity);
    final List<String> hostNames = new ArrayList<String>();
    hostNames.add("host1");
    hostNames.add("host2");
    hostNames.add("host3");

    final List<Host> hosts = new ArrayList<Host>();
    final List<HostVersionEntity> hostVersionEntities = new ArrayList<HostVersionEntity>();
    for(String hostName : hostNames) {
      Host host =  Mockito.mock(Host.class);
      host.setHostName(hostName);
      Mockito.when(host.getHostName()).thenReturn(hostName);
      Mockito.when(host.getMaintenanceState(1L)).thenReturn(MaintenanceState.OFF);
      hosts.add(host);
      HostVersionEntity hve = Mockito.mock(HostVersionEntity.class);
      Mockito.when(hve.getRepositoryVersion()).thenReturn(rve);
      Mockito.when(hve.getState()).thenReturn(RepositoryVersionState.INSTALLED);
      hostVersionEntities.add(hve);
      Mockito.when(hostVersionDAO.findByHost(hostName)).thenReturn(Collections.singletonList(hve));
    }
    Mockito.when(cluster.getHosts()).thenReturn(hosts);

    PrereqCheckRequest checkRequest = new PrereqCheckRequest(clusterName);
    checkRequest.setRepositoryVersion(repositoryVersion);
    checkRequest.setSourceStackId(sourceStackId);
    checkRequest.setTargetStackId(targetStackId);

    // Case 1. Initialize with good values
    PrerequisiteCheck check = new PrerequisiteCheck(null, null);
    installPackagesCheck.perform(check, checkRequest);
    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());

    // Case 2: Install Packages failed on host1
    Mockito.when(hostVersionEntities.get(0).getState()).thenReturn(RepositoryVersionState.INSTALL_FAILED);
    Mockito.when(clusterVersionEntity.getState()).thenReturn(RepositoryVersionState.INSTALL_FAILED);
    check = new PrerequisiteCheck(null, null);
    installPackagesCheck.perform(check, checkRequest);
    Assert.assertEquals(PrereqCheckStatus.FAIL, check.getStatus());
    Assert.assertNotNull(check.getFailedOn());
    Assert.assertTrue(check.getFailedOn().contains("host1"));

    // Case 3: Install Packages failed on host1 and host1 was put in Maintenance Mode
    Mockito.when(hostVersionEntities.get(0).getState()).thenReturn(RepositoryVersionState.INSTALL_FAILED);
    Mockito.when(hosts.get(0).getMaintenanceState(1L)).thenReturn(MaintenanceState.ON);
    Mockito.when(clusterVersionEntity.getState()).thenReturn(RepositoryVersionState.INSTALL_FAILED);
    check = new PrerequisiteCheck(null, null);
    installPackagesCheck.perform(check, checkRequest);
    Assert.assertEquals(PrereqCheckStatus.FAIL, check.getStatus());
    Assert.assertNotNull(check.getFailedOn());
    Assert.assertTrue(check.getFailedOn().contains(clusterName));
  }
}
