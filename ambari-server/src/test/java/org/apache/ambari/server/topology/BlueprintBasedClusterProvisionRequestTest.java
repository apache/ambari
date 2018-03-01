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

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.util.Set;

import org.apache.ambari.server.controller.internal.ProvisionClusterRequest;
import org.apache.ambari.server.controller.internal.StackDefinition;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.StackId;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

public class BlueprintBasedClusterProvisionRequestTest {

  private static final StackId STACK_ID = new StackId("HDP-2.6");
  private static final Set<StackId> STACK_IDS = ImmutableSet.of(STACK_ID);

  @Test(expected = IllegalArgumentException.class) // THEN
  public void clusterCannotRelaxBlueprintSecurity() {
    // GIVEN
    AmbariContext context = createNiceMock(AmbariContext.class);
    StackDefinition stack = createNiceMock(StackDefinition.class);
    expect(context.composeStacks(STACK_IDS)).andReturn(stack).anyTimes();

    Blueprint blueprint = secureBlueprint(STACK_IDS);
    ProvisionClusterRequest request = insecureCluster();

    replay(context, stack, blueprint, request);

    // WHEN
    new BlueprintBasedClusterProvisionRequest(context, null, blueprint, request);
  }

  private ProvisionClusterRequest insecureCluster() {
    ProvisionClusterRequest request = createNiceMock(ProvisionClusterRequest.class);
    expect(request.getSecurityConfiguration()).andReturn(SecurityConfiguration.NONE).anyTimes();
    expect(request.getStackIds()).andReturn(ImmutableSet.of()).anyTimes();
    expect(request.getMpacks()).andReturn(ImmutableSet.of()).anyTimes();
    return request;
  }

  private Blueprint secureBlueprint(Set<StackId> stackIds) {
    Blueprint blueprint = createNiceMock(Blueprint.class);
    SecurityConfiguration secure = new SecurityConfiguration(SecurityType.KERBEROS);
    expect(blueprint.getSecurity()).andReturn(secure).anyTimes();
    expect(blueprint.getStackIds()).andReturn(stackIds).anyTimes();
    expect(blueprint.getMpacks()).andReturn(ImmutableSet.of()).anyTimes();
    return blueprint;
  }

}
