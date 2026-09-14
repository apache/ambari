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
package org.apache.ambari.server.topology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.ambari.server.orm.dao.TopologyHostRequestDAO;
import org.apache.ambari.server.orm.dao.TopologyLogicalRequestDAO;
import org.apache.ambari.server.orm.dao.TopologyRequestDAO;
import org.apache.ambari.server.orm.entities.TopologyHostRequestEntity;
import org.apache.ambari.server.orm.entities.TopologyLogicalRequestEntity;
import org.apache.ambari.server.orm.entities.TopologyRequestEntity;
import org.junit.Test;

public class PersistedStateImplTest {

  @Test
  public void testRemovingLastProvisionHostRetainsCancelledDurableIntent() throws Exception {
    PersistedStateImpl persistedState = new PersistedStateImpl();
    TopologyRequestDAO requestDAO = mock(TopologyRequestDAO.class);
    TopologyLogicalRequestDAO logicalRequestDAO = mock(TopologyLogicalRequestDAO.class);
    TopologyHostRequestDAO hostRequestDAO = mock(TopologyHostRequestDAO.class);
    setField(persistedState, "topologyRequestDAO", requestDAO);
    setField(persistedState, "topologyLogicalRequestDAO", logicalRequestDAO);
    setField(persistedState, "hostRequestDAO", hostRequestDAO);

    TopologyRequestEntity requestEntity = new TopologyRequestEntity();
    requestEntity.setId(7L);
    requestEntity.setAction(TopologyRequest.Type.PROVISION.name());
    requestEntity.setProvisioningState(TopologyRequestEntity.PROVISIONING_STATE_ACTIVE);
    TopologyLogicalRequestEntity logicalEntity = new TopologyLogicalRequestEntity();
    logicalEntity.setId(11L);
    logicalEntity.setTopologyRequestId(7L);
    logicalEntity.setTopologyRequestEntity(requestEntity);
    TopologyHostRequestEntity hostEntity = new TopologyHostRequestEntity();
    hostEntity.setId(13L);
    logicalEntity.setTopologyHostRequestEntities(new ArrayList<>(Collections.singleton(hostEntity)));
    requestEntity.setTopologyLogicalRequestEntity(logicalEntity);

    HostRequest hostRequest = mock(HostRequest.class);
    when(hostRequest.getId()).thenReturn(13L);
    when(logicalRequestDAO.findById(11L)).thenReturn(logicalEntity);
    when(hostRequestDAO.findById(13L)).thenReturn(hostEntity);

    persistedState.removeHostRequests(11L, Collections.singleton(hostRequest));

    verify(hostRequestDAO).remove(hostEntity);
    verify(logicalRequestDAO).remove(logicalEntity);
    verify(requestDAO).merge(requestEntity);
    verify(requestDAO, never()).removeByPK(7L);
    assertNull(requestEntity.getTopologyLogicalRequestEntity());
    assertEquals(TopologyRequestEntity.PROVISIONING_STATE_CANCELLED,
        requestEntity.getProvisioningState());
  }

  private static void setField(Object target, String name, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }
}
