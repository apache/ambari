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

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.apache.ambari.server.controller.internal.ProvisionClusterRequest;
import org.apache.ambari.server.controller.internal.StackDefinition;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.StackId;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

public class BlueprintBasedClusterProvisionRequestTest {

  private static final StackId STACK_ID = new StackId("HDP-2.6");
  private static final Set<StackId> STACK_IDS = ImmutableSet.of(STACK_ID);

  private static final String HDPCORE = "HDPCORE";
  private static final String EDW = "EDW";
  private static final String ODS = "ODS";
  private static final String ODS_MARKETING = "ODS MARKETING";
  private static final String ODS_RND = "ODS R&D";
  private static final String V10 = "1.0.0.0";
  private static final String V11 = "1.1.0.0";
  private static final String URI_FROM_BLUEPRINT = "http://from.blueprint";
  private static final String URI_FROM_PROVISION_REQUEST = "http://from.provision.request";

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

  @Test
  public void mergeMpacks() throws Exception {
    // GIVEN
    Collection<MpackInstance> blueprintMpacks = ImmutableSet.of(
      mpack(HDPCORE, HDPCORE, V10, URI_FROM_BLUEPRINT),  // this will be overridden
      mpack(ODS, ODS_MARKETING, V10, URI_FROM_BLUEPRINT),
      mpack(EDW, EDW, V10, URI_FROM_BLUEPRINT));
    Collection<MpackInstance> clusterTemplateMpacks = ImmutableSet.of(
      mpack(HDPCORE, HDPCORE, V10, URI_FROM_PROVISION_REQUEST),
      mpack(ODS, ODS_RND, V10, URI_FROM_PROVISION_REQUEST),
      mpack(EDW, EDW, V11, URI_FROM_PROVISION_REQUEST));

    // WHEN
    BlueprintBasedClusterProvisionRequest request = createRequest(blueprintMpacks, clusterTemplateMpacks);

    // THEN
    assertEquals(5, request.getAllMpacks().size()); // one less than bp + cluster req combined due to one override
    Map<MpackInstance.Key, MpackInstance> mpacks =
      request.getAllMpacks().stream().collect(toMap(MpackInstance::getKey, Function.identity()));
    assertEquals(
      URI_FROM_BLUEPRINT,
      mpacks.get(new MpackInstance.Key(ODS, ODS_MARKETING, V10)).getUrl());
    assertEquals(
      URI_FROM_BLUEPRINT,
      mpacks.get(new MpackInstance.Key(EDW, EDW, V10)).getUrl());
    assertEquals(
      "mpack definition in blueprint was not properly overriden from cluster template",
      URI_FROM_PROVISION_REQUEST,
      mpacks.get(new MpackInstance.Key(HDPCORE, HDPCORE, V10)).getUrl());
    assertEquals(
      URI_FROM_PROVISION_REQUEST,
      mpacks.get(new MpackInstance.Key(ODS, ODS_RND, V10)).getUrl());
    assertEquals(
      URI_FROM_PROVISION_REQUEST,
      mpacks.get(new MpackInstance.Key(EDW, EDW, V11)).getUrl());
  }

  private BlueprintBasedClusterProvisionRequest createRequest(Collection<MpackInstance> blueprintMpacks,
                                                              Collection<MpackInstance> clusterTemplateMpacks) {
    Blueprint blueprint = createNiceMock(Blueprint.class);
    SecurityConfiguration secure = new SecurityConfiguration(SecurityType.NONE);
    expect(blueprint.getSecurity()).andReturn(secure).anyTimes();
    expect(blueprint.getStackIds()).andReturn(
      blueprintMpacks.stream()
        .map( MpackInstance::getStackId )
        .collect(toSet()))
      .anyTimes();
    expect(blueprint.getMpacks()).andReturn(blueprintMpacks).anyTimes();

    ProvisionClusterRequest request = createNiceMock(ProvisionClusterRequest.class);
    expect(request.getSecurityConfiguration()).andReturn(SecurityConfiguration.NONE).anyTimes();
    expect(request.getStackIds()).andReturn(ImmutableSet.of()).anyTimes();
    expect(request.getMpacks()).andReturn(clusterTemplateMpacks).anyTimes();

    AmbariContext context = createNiceMock(AmbariContext.class);
    StackDefinition stack = createNiceMock(StackDefinition.class);
    expect(context.composeStacks(anyObject())).andReturn(stack).anyTimes();

    replay(context, stack, blueprint, request);

    return new BlueprintBasedClusterProvisionRequest(context, null, blueprint, request);
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

  private static final MpackInstance mpack(String name, String type, String version, String uri) {
    return new MpackInstance(name, type, version, uri, null);
  }
}
