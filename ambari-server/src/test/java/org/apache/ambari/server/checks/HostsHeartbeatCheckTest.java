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

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostHealthStatus;
import org.apache.ambari.server.state.HostHealthStatus.HealthStatus;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.inject.Provider;

/**
 * Unit tests for HostsHeartbeatCheck
 *
 */
public class HostsHeartbeatCheckTest {
  private final Clusters clusters = Mockito.mock(Clusters.class);

  @Test
  public void testIsApplicable() throws Exception {
    PrereqCheckRequest checkRequest = new PrereqCheckRequest("c1");
    checkRequest.setRepositoryVersion("HDP-2.2.0.0");
    checkRequest.setSourceStackId(new StackId("HDP", "2.2"));
    checkRequest.setTargetStackId(new StackId("HDP", "2.2"));
    HostsHeartbeatCheck hhc = new HostsHeartbeatCheck();
    Configuration config = Mockito.mock(Configuration.class);
    Mockito.when(config.getRollingUpgradeMinStack()).thenReturn("HDP-2.2");
    Mockito.when(config.getRollingUpgradeMaxStack()).thenReturn("");
    hhc.config = config;

    Assert.assertTrue(hhc.isApplicable(checkRequest));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testPerform() throws Exception {
    final HostsHeartbeatCheck hostHeartbeatCheck = new HostsHeartbeatCheck();
    hostHeartbeatCheck.clustersProvider = new Provider<Clusters>() {

      @Override
      public Clusters get() {
        return clusters;
      }
    };

    final Cluster cluster = Mockito.mock(Cluster.class);
    Mockito.when(cluster.getClusterId()).thenReturn(1L);
    Mockito.when(cluster.getCurrentStackVersion()).thenReturn(new StackId("HDP", "2.2"));
    Mockito.when(clusters.getCluster("cluster")).thenReturn(cluster);

    final List<Host> hosts = new ArrayList<>();
    final Host host1 = Mockito.mock(Host.class);
    final Host host2 = Mockito.mock(Host.class);
    final Host host3 = Mockito.mock(Host.class);

    final HostHealthStatus status1 = Mockito.mock(HostHealthStatus.class);
    final HostHealthStatus status2 = Mockito.mock(HostHealthStatus.class);
    final HostHealthStatus status3 = Mockito.mock(HostHealthStatus.class);

    Mockito.when(host1.getMaintenanceState(1L)).thenReturn(MaintenanceState.OFF);
    Mockito.when(host2.getMaintenanceState(1L)).thenReturn(MaintenanceState.OFF);
    Mockito.when(host3.getMaintenanceState(1L)).thenReturn(MaintenanceState.OFF);

    Mockito.when(host1.getHealthStatus()).thenReturn(status1);
    Mockito.when(host2.getHealthStatus()).thenReturn(status2);
    Mockito.when(host3.getHealthStatus()).thenReturn(status3);

    Mockito.when(status1.getHealthStatus()).thenReturn(HealthStatus.HEALTHY);
    Mockito.when(status2.getHealthStatus()).thenReturn(HealthStatus.HEALTHY);
    Mockito.when(status3.getHealthStatus()).thenReturn(HealthStatus.UNKNOWN);

    hosts.add(host1);
    hosts.add(host2);
    hosts.add(host3);

    Mockito.when(cluster.getHosts()).thenReturn(hosts);

    PrerequisiteCheck check = new PrerequisiteCheck(null, null);
    hostHeartbeatCheck.perform(check, new PrereqCheckRequest("cluster"));
    Assert.assertEquals(PrereqCheckStatus.FAIL, check.getStatus());

    // put the unhealthy host into MM which will allow this check to pass
    check = new PrerequisiteCheck(null, null);
    Mockito.when(host3.getMaintenanceState(1L)).thenReturn(MaintenanceState.ON);
    hostHeartbeatCheck.perform(check, new PrereqCheckRequest("cluster"));
    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());

    // make the host healthy and take it out of MM to produce a PASS result
    Mockito.when(status3.getHealthStatus()).thenReturn(HealthStatus.HEALTHY);
    check = new PrerequisiteCheck(null, null);
    Mockito.when(host3.getMaintenanceState(1L)).thenReturn(MaintenanceState.OFF);
    hostHeartbeatCheck.perform(check, new PrereqCheckRequest("cluster"));
    Assert.assertEquals(PrereqCheckStatus.PASS, check.getStatus());
  }
}
