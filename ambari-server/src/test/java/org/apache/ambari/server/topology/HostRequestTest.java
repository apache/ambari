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

import static org.apache.ambari.server.controller.internal.ProvisionAction.INSTALL_AND_START;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;

import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.orm.entities.TopologyHostGroupEntity;
import org.apache.ambari.server.orm.entities.TopologyHostRequestEntity;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

public class HostRequestTest {

  @Test
  public void testConfigurationRetryRequiresPendingUndispatchedWork() throws Exception {
    HostGroup hostGroup = EasyMock.createNiceMock(HostGroup.class);
    ClusterTopology topology = EasyMock.createNiceMock(ClusterTopology.class);
    expect(hostGroup.getName()).andReturn("workers").anyTimes();
    expect(hostGroup.getComponentNames()).andReturn(Collections.emptyList()).anyTimes();
    expect(hostGroup.getComponentNames(INSTALL_AND_START)).andReturn(Collections.emptyList()).anyTimes();
    expect(topology.getProvisionAction()).andReturn(INSTALL_AND_START).anyTimes();
    EasyMock.replay(hostGroup, topology);

    HostRequest request = new HostRequest(1L, 2L, 3L, "host1", "blueprint",
        hostGroup, null, topology, false);
    Assert.assertTrue(request.isConfigurationRetrySafe());

    HostRoleCommand logicalTask = EasyMock.createNiceMock(HostRoleCommand.class);
    expect(logicalTask.getStatus()).andReturn(HostRoleStatus.ABORTED).anyTimes();
    EasyMock.replay(logicalTask);
    request.logicalTasks.put(4L, logicalTask);
    Assert.assertFalse(request.isConfigurationRetrySafe());

    request.logicalTasks.clear();
    request.setStatus(HostRoleStatus.ABORTED);
    Assert.assertFalse(request.isConfigurationRetrySafe());

    request.setStatus(HostRoleStatus.PENDING);
    Field physicalTasksField = HostRequest.class.getDeclaredField("physicalTasks");
    physicalTasksField.setAccessible(true);
    @SuppressWarnings("unchecked")
    Map<Long, Long> physicalTasks = (Map<Long, Long>) physicalTasksField.get(request);
    physicalTasks.put(4L, 5L);
    Assert.assertFalse(request.isConfigurationRetrySafe());
  }

  @Test
  public void testConfigurationFailureIsSanitizedPersistedAndClearable() {
    HostGroup hostGroup = EasyMock.createNiceMock(HostGroup.class);
    ClusterTopology topology = EasyMock.createNiceMock(ClusterTopology.class);
    Blueprint blueprint = EasyMock.createNiceMock(Blueprint.class);
    AmbariContext ambariContext = EasyMock.createNiceMock(AmbariContext.class);
    PersistedState persistedState = EasyMock.createMock(PersistedState.class);
    expect(hostGroup.getName()).andReturn("workers").anyTimes();
    expect(hostGroup.getComponentNames()).andReturn(Collections.emptyList()).anyTimes();
    expect(hostGroup.getComponentNames(INSTALL_AND_START)).andReturn(Collections.emptyList()).anyTimes();
    expect(topology.getProvisionAction()).andReturn(INSTALL_AND_START).anyTimes();
    expect(topology.getClusterId()).andReturn(3L).anyTimes();
    expect(topology.getBlueprint()).andReturn(blueprint).anyTimes();
    expect(topology.getAmbariContext()).andReturn(ambariContext).anyTimes();
    expect(blueprint.getName()).andReturn("blueprint").anyTimes();
    expect(blueprint.getHostGroup("workers")).andReturn(hostGroup).anyTimes();
    expect(ambariContext.isHostRegisteredWithCluster(3L, "host1")).andReturn(false);
    persistedState.setHostRequestStatus(2L, HostRoleStatus.PENDING,
        HostRequest.CONFIGURATION_FAILURE_MESSAGE);
    expectLastCall().once();
    persistedState.setHostRequestStatus(2L, HostRoleStatus.PENDING, null);
    expectLastCall().once();
    EasyMock.replay(hostGroup, topology, blueprint, ambariContext, persistedState);

    HostRequest request = new HostRequest(1L, 2L, 3L, "host1", "blueprint",
        hostGroup, null, topology, false);
    request.recordConfigurationFailure(persistedState);

    Assert.assertEquals(HostRequest.CONFIGURATION_FAILURE_MESSAGE,
        request.getStatusMessage().get());
    Assert.assertEquals(HostRoleStatus.PENDING, request.getStatus());
    Assert.assertFalse(request.getStatusMessage().get().contains("password"));

    TopologyHostGroupEntity groupEntity = new TopologyHostGroupEntity();
    groupEntity.setName("workers");
    TopologyHostRequestEntity persisted = new TopologyHostRequestEntity();
    persisted.setId(2L);
    persisted.setHostName("host1");
    persisted.setStatus(HostRoleStatus.PENDING);
    persisted.setStatusMessage(HostRequest.CONFIGURATION_FAILURE_MESSAGE);
    persisted.setTopologyHostGroupEntity(groupEntity);
    persisted.setTopologyHostTaskEntities(Collections.emptyList());
    HostRequest reloaded = new HostRequest(1L, 2L, null, topology, persisted, false);
    Assert.assertEquals(HostRequest.CONFIGURATION_FAILURE_MESSAGE,
        reloaded.getStatusMessage().get());

    request.clearConfigurationFailure(persistedState);
    Assert.assertFalse(request.getStatusMessage().isPresent());
    EasyMock.verify(hostGroup, topology, blueprint, ambariContext, persistedState);
  }
}
