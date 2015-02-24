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
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
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
    Assert.assertTrue(new HostsMasterMaintenanceCheck().isApplicable(request));

    request.setRepositoryVersion(null);
    Assert.assertFalse(new HostsMasterMaintenanceCheck().isApplicable(request));
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
    Mockito.when(repositoryVersionDAO.findByStackAndVersion(Mockito.anyString(), Mockito.anyString())).thenReturn(null);

    PrerequisiteCheck check = new PrerequisiteCheck(null, null);
    hostsRepositoryVersionCheck.perform(check, new PrereqCheckRequest("cluster"));
    Assert.assertEquals(PrereqCheckStatus.FAIL, check.getStatus());

    final RepositoryVersionEntity repositoryVersion = new RepositoryVersionEntity();
    Mockito.when(repositoryVersionDAO.findByStackAndVersion(Mockito.anyString(), Mockito.anyString())).thenReturn(repositoryVersion);
    final HostVersionEntity hostVersion = new HostVersionEntity();
    hostVersion.setState(RepositoryVersionState.INSTALLED);
    Mockito.when(hostVersionDAO.findByClusterStackVersionAndHost(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(hostVersion);

    check = new PrerequisiteCheck(null, null);
    hostsRepositoryVersionCheck.perform(check, new PrereqCheckRequest("cluster"));
    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());
  }
}
