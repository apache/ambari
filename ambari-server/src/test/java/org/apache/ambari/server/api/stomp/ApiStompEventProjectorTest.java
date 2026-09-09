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
package org.apache.ambari.server.api.stomp;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.stomp.dto.AlertGroupUpdate;
import org.apache.ambari.server.agent.stomp.dto.MetadataCluster;
import org.apache.ambari.server.agent.stomp.dto.MetadataServiceInfo;
import org.apache.ambari.server.agent.stomp.dto.TopologyCluster;
import org.apache.ambari.server.api.query.render.AlertSummaryGroupedRenderer;
import org.apache.ambari.server.api.stomp.ApiStompAuthorizationService.EventAccess;
import org.apache.ambari.server.events.AlertGroupsUpdateEvent;
import org.apache.ambari.server.events.AlertUpdateEvent;
import org.apache.ambari.server.events.ConfigsUpdateEvent;
import org.apache.ambari.server.events.DefaultMessageEmitter;
import org.apache.ambari.server.events.MetadataUpdateEvent;
import org.apache.ambari.server.events.NamedTaskUpdateEvent;
import org.apache.ambari.server.events.RequestUpdateEvent;
import org.apache.ambari.server.events.STOMPEvent;
import org.apache.ambari.server.events.TopologyUpdateEvent;
import org.apache.ambari.server.events.UpdateEventType;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.state.Cluster;
import org.junit.Test;
import org.springframework.security.core.Authentication;

public class ApiStompEventProjectorTest {
  @Test
  public void createsIndependentAggregateProjection() {
    ApiStompAuthorizationService authorization = createMock(ApiStompAuthorizationService.class);
    Authentication user = TestAuthenticationFactory.createClusterUser("alice", 900L);
    expect(authorization.isAuthorized(user, 101L, EventAccess.ALERT)).andReturn(true);
    expect(authorization.isAuthorized(user, 202L, EventAccess.ALERT)).andReturn(false);
    replay(authorization);

    Map<Long, Map<String, AlertSummaryGroupedRenderer.AlertDefinitionSummary>> summaries = new HashMap<>();
    summaries.put(101L, emptyMap());
    summaries.put(202L, emptyMap());
    AlertUpdateEvent original = new AlertUpdateEvent(summaries);

    Optional<STOMPEvent> result = new ApiStompEventProjector(authorization)
        .project(original, "/events/alerts", user);

    assertTrue(result.isPresent());
    AlertUpdateEvent projected = (AlertUpdateEvent) result.get();
    assertEquals(singleton(101L), projected.getSummaries().keySet());
    assertEquals(2, original.getSummaries().size());
  }

  @Test
  public void apiClientsCannotSubscribeToAgentMetadataFields() {
    ApiStompAuthorizationService authorization = createMock(ApiStompAuthorizationService.class);
    replay(authorization);

    SortedMap<String, Map<String, String>> credentials = new TreeMap<>();
    credentials.put("hbase-site", singletonMap("password.alias", "password.property"));
    SortedMap<String, MetadataServiceInfo> services = new TreeMap<>();
    services.put("HBASE", new MetadataServiceInfo("2.6.3", true, credentials, 60L, "HBASE"));
    SortedMap<String, String> clusterParams = new TreeMap<>();
    clusterParams.put("user_groups", "internal-user-map");
    SortedMap<String, SortedMap<String, String>> agentConfigs = new TreeMap<>();
    agentConfigs.put("agent", new TreeMap<>(singletonMap("internal", "value")));
    SortedMap<String, MetadataCluster> clusters = new TreeMap<>();
    clusters.put("101", new MetadataCluster(null, services, true, clusterParams, agentConfigs));
    MetadataUpdateEvent event = new MetadataUpdateEvent(clusters, null, null, UpdateEventType.UPDATE);

    assertFalse(DefaultMessageEmitter.DEFAULT_API_EVENT_TYPES.contains(STOMPEvent.Type.METADATA));
    assertFalse(ApiStompDestinations.isAllowedSubscription("/events/metadata"));
    assertFalse(new ApiStompEventProjector(authorization)
        .project(event, "/events/metadata", TestAuthenticationFactory.createAdministrator("admin")).isPresent());
  }

  @Test
  public void projectedTopologyOmitsGlobalHash() {
    ApiStompAuthorizationService authorization = createMock(ApiStompAuthorizationService.class);
    Authentication user = TestAuthenticationFactory.createClusterUser("alice", 900L);
    expect(authorization.isAuthorized(user, 101L, EventAccess.CLUSTER)).andReturn(true);
    replay(authorization);
    SortedMap<String, TopologyCluster> clusters = new TreeMap<>();
    clusters.put("101", new TopologyCluster());
    TopologyUpdateEvent event = new TopologyUpdateEvent(clusters, UpdateEventType.UPDATE);
    event.setHash("global-hash");

    TopologyUpdateEvent projected = (TopologyUpdateEvent) new ApiStompEventProjector(authorization)
        .project(event, "/events/ui_topologies", user).get();

    assertNull(projected.getHash());
  }

  @Test
  public void rejectsConflictingNestedConfigIdentity() {
    ApiStompAuthorizationService authorization = createMock(ApiStompAuthorizationService.class);
    Authentication user = TestAuthenticationFactory.createClusterUser("alice", 900L);
    expect(authorization.isAuthorized(user, 101L, EventAccess.CONFIG)).andReturn(true);
    replay(authorization);
    Cluster cluster = createMock(Cluster.class);
    expect(cluster.getClusterId()).andReturn(101L);
    replay(cluster);
    ConfigsUpdateEvent event = new ConfigsUpdateEvent(cluster, emptyList());
    event.getConfigs().add(event.new ClusterConfig(202L, "core-site", "version1", 1L));

    assertFalse(new ApiStompEventProjector(authorization)
        .project(event, "/events/configs", user).isPresent());
  }

  @Test
  public void rejectsRequestWithConflictingTaskParent() {
    ApiStompAuthorizationService authorization = createMock(ApiStompAuthorizationService.class);
    replay(authorization);
    Authentication user = TestAuthenticationFactory.createClusterUser("alice", 900L);
    RequestUpdateEvent.HostRoleCommand task =
        new RequestUpdateEvent.HostRoleCommand(41L, 32L, HostRoleStatus.IN_PROGRESS, "host1");
    RequestUpdateEvent event = new RequestUpdateEvent(31L, HostRoleStatus.IN_PROGRESS, singleton(task));

    assertFalse(new ApiStompEventProjector(authorization)
        .project(event, "/events/requests", user).isPresent());
  }

  @Test
  public void rejectsNamedTaskDestinationMismatch() {
    ApiStompAuthorizationService authorization = createMock(ApiStompAuthorizationService.class);
    replay(authorization);
    Authentication user = TestAuthenticationFactory.createClusterUser("alice", 900L);
    NamedTaskUpdateEvent event = new NamedTaskUpdateEvent(
        41L, 31L, "host1", null, HostRoleStatus.IN_PROGRESS, null, null, null, null, null);

    assertFalse(new ApiStompEventProjector(authorization)
        .project(event, "/events/tasks/42", user).isPresent());
  }

  @Test(expected = IllegalArgumentException.class)
  public void rejectsIdentityLessAlertGroupDelete() {
    new AlertGroupsUpdateEvent(singleton(new AlertGroupUpdate(41L)), UpdateEventType.DELETE);
  }
}
