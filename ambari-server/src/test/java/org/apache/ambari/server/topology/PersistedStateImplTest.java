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


import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;

import org.apache.ambari.server.controller.internal.BaseClusterRequest;
import org.apache.ambari.server.controller.internal.ProvisionAction;
import org.apache.ambari.server.controller.internal.ProvisionClusterRequest;
import org.apache.ambari.server.orm.dao.TopologyRequestDAO;
import org.apache.ambari.server.orm.entities.TopologyRequestEntity;
import org.easymock.Capture;
import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.collect.ImmutableMap;


@RunWith(EasyMockRunner.class)
public class PersistedStateImplTest {

  private static final String CLUSTER_REQUEST =
    "{'blueprint': 'bp', 'host_groups': [{'name': 'group','host_count': '1' }]}".replace('\'', '"');

  private static final String BLUEPRINT_NAME = "bp";

  @Mock
  private TopologyRequestDAO topologyRequestDAO;

  @Mock
  private BlueprintFactory blueprintFactory;

  @Mock
  private Blueprint blueprint;

  @Mock
  private ProvisionClusterRequest
    request;

  private PersistedStateImpl persistedState;

  @Before
  public void init() throws Exception {
    expect(blueprint.getName()).andReturn(BLUEPRINT_NAME).anyTimes();
    expect(blueprint.getConfiguration()).andReturn(new Configuration()).anyTimes();
    expect(blueprintFactory.getBlueprint(BLUEPRINT_NAME)).andReturn(blueprint).anyTimes();

    expect(request.getBlueprint()).andReturn(blueprint).anyTimes();
    expect(request.getRawRequestBody()).andReturn(CLUSTER_REQUEST).anyTimes();
    expect(request.getType()).andReturn(TopologyRequest.Type.PROVISION).anyTimes();
    expect(request.getConfiguration()).andReturn(new Configuration()).anyTimes();
    expect(request.getClusterId()).andReturn(1L).anyTimes();
    expect(request.getDescription()).andReturn("").anyTimes();
    expect(request.getProvisionAction()).andReturn(ProvisionAction.INSTALL_AND_START).anyTimes();
    HostGroupInfo hostGroupInfo = new HostGroupInfo("hostgroup1");
    hostGroupInfo.setConfiguration(new Configuration());
    expect(request.getHostGroupInfo()).andReturn(ImmutableMap.of("hostgroup1", hostGroupInfo)).anyTimes();

    replay(blueprint, blueprintFactory, request);

    Field blueprintFactoryField = BaseClusterRequest.class.getDeclaredField("blueprintFactory");
    blueprintFactoryField.setAccessible(true);
    blueprintFactoryField.set(null, blueprintFactory);

    persistedState = new PersistedStateImpl();
    Field topologyRequestDAOField = PersistedStateImpl.class.getDeclaredField("topologyRequestDAO");
    topologyRequestDAOField.setAccessible(true);
    topologyRequestDAOField.set(persistedState, topologyRequestDAO);
  }

  @After
  public void tearDown() {
    reset(topologyRequestDAO, blueprintFactory, blueprint, request);
  }

  @Test
  public void testPersistTopologyRequest_RawRequestIsSaved() throws Exception {
    // Given
    Capture<TopologyRequestEntity> entityCapture = newCapture();
    topologyRequestDAO.create(capture(entityCapture));
    expectLastCall().andAnswer(() -> {
      entityCapture.getValue().setId(1L);
      return null;
    });
    replay(topologyRequestDAO);

    // When
    persistedState.persistTopologyRequest(request);

    // Then
    assertEquals(CLUSTER_REQUEST, entityCapture.getValue().getRawRequestBody());
  }

}