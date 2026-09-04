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
package org.apache.ambari.server.service.metrics;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.apache.ambari.server.api.services.metrics.BoardResponse;
import org.apache.ambari.server.orm.dao.BoardDAO;
import org.apache.ambari.server.orm.dao.BoardPayloadDAO;
import org.apache.ambari.server.orm.entities.BoardEntity;
import org.apache.ambari.server.orm.entities.BoardPayloadEntity;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import com.google.inject.Provider;

public class DashboardServiceTest {
  @Test
  @SuppressWarnings("unchecked")
  public void testUpdateConfigsPersistsCanonicalJsonUnchanged() throws Exception {
    BoardDAO boardDAO = mock(BoardDAO.class);
    BoardPayloadDAO payloadDAO = mock(BoardPayloadDAO.class);
    BuiltinDashboardProvisioner provisioner = mock(BuiltinDashboardProvisioner.class);
    Provider<Clusters> clustersProvider = mock(Provider.class);
    Clusters clusters = mock(Clusters.class);
    Cluster cluster = mock(Cluster.class);
    BoardEntity board = new BoardEntity();
    board.setId(42L);
    board.setName("Imported dashboard");
    board.setClusterName("west");
    BoardPayloadEntity payload = new BoardPayloadEntity();
    payload.setId(42L);
    payload.setPayload("{}");
    when(clustersProvider.get()).thenReturn(clusters);
    when(clusters.getCluster("west")).thenReturn(cluster);
    when(cluster.getResourceId()).thenReturn(91L);
    when(boardDAO.findByIdAndCluster(42L, "west")).thenReturn(board);
    when(boardDAO.merge(board)).thenReturn(board);
    when(payloadDAO.findByPK(42L)).thenReturn(payload);
    String raw = "{\"version\":\"3.0.0\",\"var\":[{\"name\":\"host\",\"type\":\"query\","
        + "\"definition\":\"ambari_agent_host_info\",\"value\":\".*\",\"multi\":true,\"includeAll\":true}],"
        + "\"panels\":[{\"id\":\"hosts\",\"name\":\"Hosts\","
        + "\"titleKey\":\"monitoring.dashboard.sections.hosts\",\"type\":\"hexbin\","
        + "\"targets\":[{\"refId\":\"A\",\"expr\":\"ambari_agent_host_info\"}],"
        + "\"layout\":{\"h\":5,\"w\":8,\"x\":0,\"y\":0,\"i\":\"hosts\",\"isResizable\":true}}]}";
    DashboardService service = new DashboardService(boardDAO, payloadDAO, provisioner, clustersProvider);

    try (MockedStatic<AuthorizationHelper> authorization = org.mockito.Mockito.mockStatic(AuthorizationHelper.class)) {
      authorization.when(() -> AuthorizationHelper.getAuthenticatedName("")).thenReturn("operator");
      BoardResponse response = service.updateConfigs("west", "42", raw);

      Assert.assertSame(raw, payload.getPayload());
      Assert.assertSame(raw, response.getConfigs());
      verify(payloadDAO).merge(payload);
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testUpdateConfigsRejectsDashboardFromAnotherCluster() throws Exception {
    BoardDAO boardDAO = mock(BoardDAO.class);
    BoardPayloadDAO payloadDAO = mock(BoardPayloadDAO.class);
    BuiltinDashboardProvisioner provisioner = mock(BuiltinDashboardProvisioner.class);
    Provider<Clusters> clustersProvider = mock(Provider.class);
    Clusters clusters = mock(Clusters.class);
    Cluster cluster = mock(Cluster.class);
    BoardEntity otherClusterBoard = new BoardEntity();
    otherClusterBoard.setId(42L);
    otherClusterBoard.setClusterName("east");
    when(clustersProvider.get()).thenReturn(clusters);
    when(clusters.getCluster("west")).thenReturn(cluster);
    when(cluster.getResourceId()).thenReturn(91L);
    when(boardDAO.findByPK(42L)).thenReturn(otherClusterBoard);
    DashboardService service = new DashboardService(boardDAO, payloadDAO, provisioner, clustersProvider);

    try (MockedStatic<AuthorizationHelper> ignored = org.mockito.Mockito.mockStatic(AuthorizationHelper.class)) {
      try {
        service.updateConfigs("west", "42", "{\"version\":\"3.0.0\",\"var\":[],\"panels\":[]}");
        Assert.fail("Expected cross-cluster dashboard update to be rejected");
      } catch (IllegalArgumentException expected) {
        Assert.assertEquals("Dashboard not found", expected.getMessage());
      }
    }

    verify(boardDAO).findByIdAndCluster(42L, "west");
    verify(boardDAO, never()).findByPK(42L);
    verifyNoInteractions(payloadDAO);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testCloneUsesNextAvailableCopyName() throws Exception {
    BoardDAO boardDAO = mock(BoardDAO.class);
    BoardPayloadDAO payloadDAO = mock(BoardPayloadDAO.class);
    BuiltinDashboardProvisioner provisioner = mock(BuiltinDashboardProvisioner.class);
    Provider<Clusters> clustersProvider = mock(Provider.class);
    Clusters clusters = mock(Clusters.class);
    Cluster cluster = mock(Cluster.class);
    BoardEntity source = new BoardEntity();
    source.setId(42L);
    source.setClusterName(BoardEntity.BUILTIN_CLUSTER);
    source.setGroupId(7L);
    source.setName("Linux Fleet Overview");
    source.setIdent("LINUX_FLEET_OVERVIEW");
    source.setBuiltIn(1);
    BoardEntity firstCopy = new BoardEntity();
    when(clustersProvider.get()).thenReturn(clusters);
    when(clusters.getCluster("west")).thenReturn(cluster);
    when(cluster.getResourceId()).thenReturn(91L);
    when(boardDAO.findByIdAndCluster(42L, "west")).thenReturn(null);
    when(boardDAO.findByPK(42L)).thenReturn(source);
    when(boardDAO.findByClusterGroupAndName("west", 7L, "Linux Fleet Overview Copy")).thenReturn(firstCopy);
    DashboardService service = new DashboardService(boardDAO, payloadDAO, provisioner, clustersProvider);

    try (MockedStatic<AuthorizationHelper> authorization = org.mockito.Mockito.mockStatic(AuthorizationHelper.class)) {
      authorization.when(() -> AuthorizationHelper.getAuthenticatedName("")).thenReturn("operator");
      BoardResponse response = service.clone("west", 42L);

      Assert.assertEquals("Linux Fleet Overview Copy 2", response.getName());
      ArgumentCaptor<BoardEntity> clone = ArgumentCaptor.forClass(BoardEntity.class);
      verify(boardDAO).create(clone.capture());
      Assert.assertEquals("Linux Fleet Overview Copy 2", clone.getValue().getName());
    }
  }
}
