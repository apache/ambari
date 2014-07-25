/**
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

package org.apache.ambari.server.controller.internal;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.ResourceProviderFactory;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Test;

/**
 * MemberResourceProvider tests.
 */
public class MemberResourceProviderTest {
  @Test
  public void testCreateResources() throws Exception {
    Resource.Type type = Resource.Type.Member;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);
    ResourceProvider memberResourceProvider = createNiceMock(MemberResourceProvider.class);

    AbstractControllerResourceProvider.init(resourceProviderFactory);

    expect(resourceProviderFactory.getMemberResourceProvider(anyObject(Set.class), anyObject(Map.class),
        eq(managementController))).andReturn(memberResourceProvider).anyTimes();
    // replay
    replay(managementController, response, resourceProviderFactory, memberResourceProvider);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<Map<String, Object>>();

    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    // add properties to the request map
    properties.put(MemberResourceProvider.MEMBER_GROUP_NAME_PROPERTY_ID, "engineering");
    properties.put(MemberResourceProvider.MEMBER_USER_NAME_PROPERTY_ID, "joe");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    provider.createResources(request);

    // verify
    verify(managementController, response);
  }

  @Test
  public void testUpdateResources() throws Exception {
    Resource.Type type = Resource.Type.Member;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);
    ResourceProvider memberResourceProvider = createNiceMock(MemberResourceProvider.class);

    AbstractControllerResourceProvider.init(resourceProviderFactory);

    // set expectations
    expect(resourceProviderFactory.getMemberResourceProvider(anyObject(Set.class), anyObject(Map.class),
        eq(managementController))).andReturn(memberResourceProvider).anyTimes();

    // replay
    replay(managementController, response, resourceProviderFactory, memberResourceProvider);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    // add the property map to a set for the request.
    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    properties.put(MemberResourceProvider.MEMBER_GROUP_NAME_PROPERTY_ID, "engineering");
    properties.put(MemberResourceProvider.MEMBER_USER_NAME_PROPERTY_ID, "joe");

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, null);

    PredicateBuilder builder = new PredicateBuilder();
    builder.property(MemberResourceProvider.MEMBER_GROUP_NAME_PROPERTY_ID).equals("engineering");
    Predicate predicate = builder.toPredicate();
    provider.updateResources(request, predicate);

    // verify
    verify(managementController, response);
  }

  @Test
  public void testDeleteResources() throws Exception {
    Resource.Type type = Resource.Type.Member;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);
    ResourceProvider memberResourceProvider = createNiceMock(MemberResourceProvider.class);

    AbstractControllerResourceProvider.init(resourceProviderFactory);

    // set expectations
    expect(resourceProviderFactory.getMemberResourceProvider(anyObject(Set.class), anyObject(Map.class),
        eq(managementController))).andReturn(memberResourceProvider).anyTimes();

    // replay
    replay(managementController, response, resourceProviderFactory, memberResourceProvider);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    PredicateBuilder builder = new PredicateBuilder();
    builder.property(MemberResourceProvider.MEMBER_GROUP_NAME_PROPERTY_ID).equals("engineering");
    Predicate predicate = builder.toPredicate();
    provider.deleteResources(predicate);

    // verify
    verify(managementController, response);
  }
}
