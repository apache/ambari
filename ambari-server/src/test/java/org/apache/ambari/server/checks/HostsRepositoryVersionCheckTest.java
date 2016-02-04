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

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
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
import org.mockito.Mockito;

import com.google.inject.Provider;

/**
 * Unit tests for HostsRepositoryVersionCheck
 *
 */
public class HostsRepositoryVersionCheckTest {
  private final Clusters clusters = Mockito.mock(Clusters.class);
  private final HostVersionDAO hostVersionDAO = Mockito.mock(HostVersionDAO.class);
  private final RepositoryVersionDAO repositoryVersionDAO = Mockito.mock(RepositoryVersionDAO.class);

  @Test
  public void testIsApplicable() throws Exception {
    final PrereqCheckRequest request = new PrereqCheckRequest("cluster");
    request.setRepositoryVersion("not null");
    HostsRepositoryVersionCheck hrvc = new HostsRepositoryVersionCheck();
    Configuration config = Mockito.mock(Configuration.class);
    hrvc.config = config;
    Assert.assertTrue(hrvc.isApplicable(request));
    Assert.assertTrue(new HostsMasterMaintenanceCheck().isApplicable(request));
    HostsRepositoryVersionCheck hrvc2 = new HostsRepositoryVersionCheck();
    hrvc2.config = config;
    Assert.assertTrue(hrvc2.isApplicable(request));
    request.setRepositoryVersion(null);

    HostsMasterMaintenanceCheck hmmc2 = new HostsMasterMaintenanceCheck();
    hmmc2.config = config;
    Assert.assertFalse(hmmc2.isApplicable(request));
  }

  @Test
  public void testPerform() throws Exception {
    final HostsRepositoryVersionCheck hostsRepositoryVersionCheck = new HostsRepositoryVersionCheck();
    hostsRepositoryVersionCheck.clustersProvider = new Provider<Clusters>() {

      @Override
      public Clusters get() {
        return clusters;
      }
    };
    hostsRepositoryVersionCheck.repositoryVersionDaoProvider = new Provider<RepositoryVersionDAO>() {
      @Override
      public RepositoryVersionDAO get() {
        return repositoryVersionDAO;
      }
    };
    hostsRepositoryVersionCheck.hostVersionDaoProvider = new Provider<HostVersionDAO>() {
      @Override
      public HostVersionDAO get() {
        return hostVersionDAO;
      }
    };

    final Cluster cluster = Mockito.mock(Cluster.class);
    Mockito.when(cluster.getClusterId()).thenReturn(1L);
    Mockito.when(cluster.getDesiredStackVersion()).thenReturn(new StackId());
    Mockito.when(clusters.getCluster("cluster")).thenReturn(cluster);
    final Map<String, Host> hosts = new HashMap<String, Host>();
    final Host host1 = Mockito.mock(Host.class);
    final Host host2 = Mockito.mock(Host.class);
    final Host host3 = Mockito.mock(Host.class);
    Mockito.when(host1.getMaintenanceState(1L)).thenReturn(MaintenanceState.OFF);
    Mockito.when(host2.getMaintenanceState(1L)).thenReturn(MaintenanceState.OFF);
    Mockito.when(host3.getMaintenanceState(1L)).thenReturn(MaintenanceState.OFF);
    hosts.put("host1", host1);
    hosts.put("host2", host2);
    hosts.put("host3", host3);
    Mockito.when(clusters.getHostsForCluster("cluster")).thenReturn(hosts);

    Mockito.when(
        repositoryVersionDAO.findByStackAndVersion(Mockito.any(StackId.class),
            Mockito.anyString())).thenReturn(null);

    Mockito.when(
        repositoryVersionDAO.findByStackAndVersion(
            Mockito.any(StackEntity.class), Mockito.anyString())).thenReturn(
        null);

    PrerequisiteCheck check = new PrerequisiteCheck(null, null);
    hostsRepositoryVersionCheck.perform(check, new PrereqCheckRequest("cluster"));
    Assert.assertEquals(PrereqCheckStatus.FAIL, check.getStatus());

    StackEntity stackEntity = new StackEntity();
    stackEntity.setStackName("HDP");
    stackEntity.setStackVersion("2.0.6");

    final RepositoryVersionEntity repositoryVersion = new RepositoryVersionEntity();
    repositoryVersion.setStack(stackEntity);

    Mockito.when(
        repositoryVersionDAO.findByStackAndVersion(Mockito.any(StackId.class),
            Mockito.anyString())).thenReturn(repositoryVersion);

    Mockito.when(
        repositoryVersionDAO.findByStackAndVersion(
            Mockito.any(StackEntity.class), Mockito.anyString())).thenReturn(
        repositoryVersion);

    final HostVersionEntity hostVersion = new HostVersionEntity();
    hostVersion.setState(RepositoryVersionState.INSTALLED);
    Mockito.when(
        hostVersionDAO.findByClusterStackVersionAndHost(Mockito.anyString(),
            Mockito.any(StackId.class), Mockito.anyString(),
            Mockito.anyString())).thenReturn(hostVersion);

    check = new PrerequisiteCheck(null, null);
    hostsRepositoryVersionCheck.perform(check, new PrereqCheckRequest("cluster"));
    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());
  }

  @Test
  public void testPerformWithVersion() throws Exception {
    final HostsRepositoryVersionCheck hostsRepositoryVersionCheck = new HostsRepositoryVersionCheck();
    hostsRepositoryVersionCheck.clustersProvider = new Provider<Clusters>() {

      @Override
      public Clusters get() {
        return clusters;
      }
    };
    hostsRepositoryVersionCheck.repositoryVersionDaoProvider = new Provider<RepositoryVersionDAO>() {
      @Override
      public RepositoryVersionDAO get() {
        return repositoryVersionDAO;
      }
    };
    hostsRepositoryVersionCheck.hostVersionDaoProvider = new Provider<HostVersionDAO>() {
      @Override
      public HostVersionDAO get() {
        return hostVersionDAO;
      }
    };

    final Cluster cluster = Mockito.mock(Cluster.class);
    Mockito.when(cluster.getClusterId()).thenReturn(1L);
    Mockito.when(cluster.getDesiredStackVersion()).thenReturn(new StackId());
    Mockito.when(clusters.getCluster("cluster")).thenReturn(cluster);
    final Map<String, Host> hosts = new HashMap<String, Host>();
    final Host host1 = Mockito.mock(Host.class);
    final Host host2 = Mockito.mock(Host.class);
    final Host host3 = Mockito.mock(Host.class);
    Mockito.when(host1.getMaintenanceState(1L)).thenReturn(MaintenanceState.OFF);
    Mockito.when(host2.getMaintenanceState(1L)).thenReturn(MaintenanceState.OFF);
    Mockito.when(host3.getMaintenanceState(1L)).thenReturn(MaintenanceState.OFF);
    hosts.put("host1", host1);
    hosts.put("host2", host2);
    hosts.put("host3", host3);
    Mockito.when(clusters.getHostsForCluster("cluster")).thenReturn(hosts);

    RepositoryVersionEntity rve = new RepositoryVersionEntity();
    rve.setVersion("1.1.1");

    HostVersionEntity hve = new HostVersionEntity();
    hve.setRepositoryVersion(rve);
    hve.setState(RepositoryVersionState.INSTALLED);

    Mockito.when(
        hostVersionDAO.findByHost(Mockito.anyString())).thenReturn(
            Collections.singletonList(hve));

    PrerequisiteCheck check = new PrerequisiteCheck(null, null);
    PrereqCheckRequest request = new PrereqCheckRequest("cluster");
    request.setRepositoryVersion("1.1.1");
    hostsRepositoryVersionCheck.perform(check, request);
    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());
  }

  @Test
  public void testPerformWithVersionNotRequired() throws Exception {
    final HostsRepositoryVersionCheck hostsRepositoryVersionCheck = new HostsRepositoryVersionCheck();
    hostsRepositoryVersionCheck.clustersProvider = new Provider<Clusters>() {

      @Override
      public Clusters get() {
        return clusters;
      }
    };
    hostsRepositoryVersionCheck.repositoryVersionDaoProvider = new Provider<RepositoryVersionDAO>() {
      @Override
      public RepositoryVersionDAO get() {
        return repositoryVersionDAO;
      }
    };
    hostsRepositoryVersionCheck.hostVersionDaoProvider = new Provider<HostVersionDAO>() {
      @Override
      public HostVersionDAO get() {
        return hostVersionDAO;
      }
    };

    final Cluster cluster = Mockito.mock(Cluster.class);
    Mockito.when(cluster.getClusterId()).thenReturn(1L);
    Mockito.when(cluster.getDesiredStackVersion()).thenReturn(new StackId());
    Mockito.when(clusters.getCluster("cluster")).thenReturn(cluster);
    final Map<String, Host> hosts = new HashMap<String, Host>();
    final Host host1 = Mockito.mock(Host.class);
    final Host host2 = Mockito.mock(Host.class);
    final Host host3 = Mockito.mock(Host.class);
    Mockito.when(host1.getMaintenanceState(1L)).thenReturn(MaintenanceState.OFF);
    Mockito.when(host2.getMaintenanceState(1L)).thenReturn(MaintenanceState.OFF);
    Mockito.when(host3.getMaintenanceState(1L)).thenReturn(MaintenanceState.OFF);
    hosts.put("host1", host1);
    hosts.put("host2", host2);
    hosts.put("host3", host3);
    Mockito.when(clusters.getHostsForCluster("cluster")).thenReturn(hosts);

    RepositoryVersionEntity rve = new RepositoryVersionEntity();
    rve.setVersion("1.1.1");

    HostVersionEntity hve = new HostVersionEntity();
    hve.setRepositoryVersion(rve);
    hve.setState(RepositoryVersionState.NOT_REQUIRED);

    Mockito.when(
        hostVersionDAO.findByHost(Mockito.anyString())).thenReturn(
            Collections.singletonList(hve));

    PrerequisiteCheck check = new PrerequisiteCheck(null, null);
    PrereqCheckRequest request = new PrereqCheckRequest("cluster");
    request.setRepositoryVersion("1.1.1");
    hostsRepositoryVersionCheck.perform(check, request);
    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());
  }

}
