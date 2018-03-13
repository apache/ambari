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

import static java.util.Collections.emptyList;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import org.apache.ambari.server.controller.internal.ProvisionAction;
import org.apache.ambari.server.orm.entities.TopologyRequestEntity;
import org.apache.ambari.server.orm.entities.TopologyRequestMpackInstanceEntity;
import org.apache.ambari.server.state.StackId;
import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

@RunWith(EasyMockRunner.class)
public class PersistedStateImplTest {

  @Mock
  BlueprintFactory blueprintFactory;

  @Mock
  Blueprint blueprint;

  @Before
  public void setup() {
    expect(blueprintFactory.getBlueprint(anyString())).andReturn(blueprint);
    expect(blueprint.getConfiguration()).andReturn(new Configuration());
    replay(blueprintFactory, blueprint);
  }

  @Test
  public void testPersistedTopologyRequest_stackIdsDepersistedCorrectly() {
    TopologyRequestEntity entity = new TopologyRequestEntity();
    entity.setAction(TopologyRequest.Type.PROVISION.name());
    entity.setProvisionAction(ProvisionAction.INSTALL_AND_START);
    entity.setClusterProperties("{}");
    entity.setClusterAttributes("{}");
    entity.setDescription("Provision Cluster c1");
    entity.setTopologyHostGroupEntities(emptyList());
    TopologyRequestMpackInstanceEntity mpackInstanceEntity = new TopologyRequestMpackInstanceEntity();
    mpackInstanceEntity.setMpackName("HDPCORE");
    mpackInstanceEntity.setMpackVersion("1.0.0.0");
    entity.setMpackInstances(ImmutableList.of(mpackInstanceEntity));
    PersistedStateImpl.ReplayedTopologyRequest request =
      new PersistedStateImpl.ReplayedTopologyRequest(entity, blueprintFactory);
    assertEquals(ImmutableSet.of(new StackId("HDPCORE", "1.0.0.0")), request.getStackIds());
  }

}