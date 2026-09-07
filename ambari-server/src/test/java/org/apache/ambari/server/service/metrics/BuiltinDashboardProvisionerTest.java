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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.ambari.server.orm.dao.BoardDAO;
import org.apache.ambari.server.orm.dao.BoardPayloadDAO;
import org.apache.ambari.server.orm.entities.BoardEntity;
import org.apache.ambari.server.orm.entities.BoardPayloadEntity;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.ObjectMapper;

public class BuiltinDashboardProvisionerTest {
  @Test
  public void testCreatesElevenPackagedDashboardsOnlyOnce() throws Exception {
    BoardDAO boardDAO = mock(BoardDAO.class);
    BoardPayloadDAO payloadDAO = mock(BoardPayloadDAO.class);
    AtomicLong sequence = new AtomicLong();
    when(boardDAO.findBuiltinByIdent(anyString())).thenReturn(null);
    doAnswer(invocation -> {
      ((BoardEntity) invocation.getArgument(0)).setId(sequence.incrementAndGet());
      return null;
    }).when(boardDAO).create(any(BoardEntity.class));
    BuiltinDashboardProvisioner provisioner = new BuiltinDashboardProvisioner(boardDAO, payloadDAO);

    provisioner.provision();
    provisioner.provision();

    verify(boardDAO, times(11)).create(any(BoardEntity.class));
    ArgumentCaptor<BoardEntity> boards = ArgumentCaptor.forClass(BoardEntity.class);
    verify(boardDAO, times(11)).create(boards.capture());
    for (BoardEntity board : boards.getAllValues()) {
      Assert.assertEquals(BoardEntity.BUILTIN_CLUSTER, board.getClusterName());
    }
    Assert.assertEquals("Dashboard", boardByIdent(boards, "LINUX_FLEET_OVERVIEW").getDisplayLocations());
    Assert.assertEquals("Dashboard", boardByIdent(boards, "LINUX_HOST_DETAIL").getDisplayLocations());
    ArgumentCaptor<BoardPayloadEntity> payloads = ArgumentCaptor.forClass(BoardPayloadEntity.class);
    verify(payloadDAO, times(11)).create(payloads.capture());
    Assert.assertEquals(11, payloads.getAllValues().size());
    for (BoardPayloadEntity payload : payloads.getAllValues()) {
      Assert.assertTrue(new ObjectMapper().readTree(payload.getPayload()).isObject());
    }
  }

  @Test
  public void testRefreshesExistingBuiltinDashboardDefinitions() {
    BoardDAO boardDAO = mock(BoardDAO.class);
    BoardPayloadDAO payloadDAO = mock(BoardPayloadDAO.class);
    AtomicLong sequence = new AtomicLong(100L);
    Map<Long, BoardPayloadEntity> payloads = new HashMap<>();
    when(boardDAO.findBuiltinByIdent(anyString())).thenAnswer(invocation -> {
      BoardEntity board = new BoardEntity();
      board.setId(sequence.incrementAndGet());
      board.setIdent(invocation.getArgument(0));
      board.setName("Stale dashboard");
      BoardPayloadEntity payload = new BoardPayloadEntity();
      payload.setId(board.getId());
      payload.setPayload("{}");
      payloads.put(board.getId(), payload);
      return board;
    });
    when(payloadDAO.findByPK(any(Long.class))).thenAnswer(invocation -> payloads.get(invocation.getArgument(0)));
    BuiltinDashboardProvisioner provisioner = new BuiltinDashboardProvisioner(boardDAO, payloadDAO);

    provisioner.provision();

    verify(boardDAO, never()).create(any(BoardEntity.class));
    verify(payloadDAO, never()).create(any(BoardPayloadEntity.class));
    verify(boardDAO, times(11)).merge(any(BoardEntity.class));
    ArgumentCaptor<BoardPayloadEntity> updatedPayloads = ArgumentCaptor.forClass(BoardPayloadEntity.class);
    verify(payloadDAO, times(11)).merge(updatedPayloads.capture());
    Assert.assertEquals(11, updatedPayloads.getAllValues().stream().map(BoardPayloadEntity::getId).distinct().count());
    for (BoardPayloadEntity payload : updatedPayloads.getAllValues()) {
      Assert.assertTrue(payload.getPayload().contains("\"version\":\"3.0.0\""));
    }
  }

  private BoardEntity boardByIdent(ArgumentCaptor<BoardEntity> boards, String ident) {
    return boards.getAllValues().stream()
        .filter(board -> ident.equals(board.getIdent()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Missing built-in dashboard " + ident));
  }
}
