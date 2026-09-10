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
package org.apache.ambari.server.agent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.agent.stomp.HostLevelParamsHolder;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.host.HostHeartbeatLostEvent;
import org.junit.jupiter.api.Test;

import com.google.inject.Injector;

class HeartbeatMonitorRecoveryTest {
  @Test
  void restartObservesAFullTimeoutBeforeDeclaringAnUnreconnectedHostLost() throws Exception {
    Fixture fixture = new Fixture();
    fixture.monitor.doWork();
    fixture.clock.set(120_999);
    fixture.monitor.doWork();
    verify(fixture.host, never()).handleEvent(any(HostHeartbeatLostEvent.class));
    fixture.clock.set(121_001);
    fixture.monitor.doWork();
    verify(fixture.host).handleEvent(any(HostHeartbeatLostEvent.class));
    verify(fixture.actions).handleLostHost("worker-b");
  }

  @Test
  void receivedHeartbeatReplacesTheStartupObservationBoundary() throws Exception {
    Fixture fixture = new Fixture();
    when(fixture.host.getLastHeartbeatTime()).thenReturn(100_000L);
    fixture.clock.set(121_001);
    fixture.monitor.doWork();
    verify(fixture.host, never()).handleEvent(any(HostHeartbeatLostEvent.class));
    fixture.clock.set(220_001);
    fixture.monitor.doWork();
    verify(fixture.host).handleEvent(any(HostHeartbeatLostEvent.class));
  }

  @Test
  void anAlreadyLostHostIsNotRevivedByMonitorStartup() throws Exception {
    Fixture fixture = new Fixture();
    when(fixture.host.getState()).thenReturn(HostState.HEARTBEAT_LOST);
    fixture.clock.set(121_001);
    fixture.monitor.doWork();
    verify(fixture.host, never()).handleEvent(any(HostHeartbeatLostEvent.class));
    verifyNoInteractions(fixture.actions);
  }

  private static class Fixture {
    final AtomicLong clock = new AtomicLong(1000);
    final Host host = mock(Host.class);
    final ActionManager actions = mock(ActionManager.class);
    final HeartbeatMonitor monitor;

    Fixture() throws Exception {
      Clusters clusters = mock(Clusters.class);
      when(clusters.getHosts()).thenReturn(List.of(host));
      when(clusters.getHostById(6L)).thenReturn(host);
      when(clusters.getClustersForHost("worker-b")).thenReturn(Set.of());
      when(host.getHostId()).thenReturn(6L);
      when(host.getHostName()).thenReturn("worker-b");
      when(host.getState()).thenReturn(HostState.HEALTHY);
      Injector injector = mock(Injector.class);
      when(injector.getInstance(AmbariEventPublisher.class)).thenReturn(mock(AmbariEventPublisher.class));
      when(injector.getInstance(RecoveryTopologyManager.class)).thenReturn(mock(RecoveryTopologyManager.class));
      when(injector.getInstance(HostLevelParamsHolder.class)).thenReturn(mock(HostLevelParamsHolder.class));
      monitor = new HeartbeatMonitor(clusters, actions, 60_000, injector, clock::get);
    }
  }
}
