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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.ambari.server.api.stomp.ApiStompAuthorizationService.EventAccess;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.RequestDAO;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.RequestEntity;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.junit.Test;
import org.springframework.security.core.Authentication;

public class ApiStompAuthorizationServiceTest {
  @Test
  public void resolvesClusterIdToResourceId() throws Exception {
    Clusters clusters = createMock(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    HostRoleCommandDAO hostRoleCommandDAO = createMock(HostRoleCommandDAO.class);
    RequestDAO requestDAO = createMock(RequestDAO.class);
    expect(clusters.getCluster(101L)).andReturn(cluster);
    expect(cluster.getResourceId()).andReturn(900L);
    replay(clusters, cluster, hostRoleCommandDAO, requestDAO);

    Authentication user = TestAuthenticationFactory.createClusterUser("alice", 900L);
    ApiStompAuthorizationService service = new ApiStompAuthorizationService(
        clusters, hostRoleCommandDAO, requestDAO);

    assertTrue(service.isAuthorized(user, 101L, EventAccess.CLUSTER));
  }

  @Test
  public void resolvesTaskThroughPersistedRequestCluster() throws Exception {
    Clusters clusters = createMock(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    HostRoleCommandDAO hostRoleCommandDAO = createMock(HostRoleCommandDAO.class);
    RequestDAO requestDAO = createMock(RequestDAO.class);
    HostRoleCommandEntity task = new HostRoleCommandEntity();
    task.setTaskId(41L);
    task.setRequestId(31L);
    RequestEntity request = new RequestEntity();
    request.setRequestId(31L);
    request.setClusterId(101L);
    expect(hostRoleCommandDAO.findByPK(41L)).andReturn(task).times(2);
    expect(requestDAO.findByPK(31L)).andReturn(request).times(2);
    expect(clusters.getCluster(101L)).andReturn(cluster).times(2);
    expect(cluster.getResourceId()).andReturn(900L).times(2);
    replay(clusters, cluster, hostRoleCommandDAO, requestDAO);

    ApiStompAuthorizationService service = new ApiStompAuthorizationService(
        clusters, hostRoleCommandDAO, requestDAO);
    Authentication clusterA = TestAuthenticationFactory.createClusterUser("alice", 900L);
    Authentication clusterB = TestAuthenticationFactory.createClusterUser("bob", 901L);

    assertTrue(service.canViewTask(clusterA, 41L, 31L));
    assertFalse(service.canViewTask(clusterB, 41L, 31L));
  }
}
