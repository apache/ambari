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

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.ServiceComponentRequest;
import org.apache.ambari.server.controller.ServiceComponentResponse;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.easymock.EasyMock;
import org.junit.Test;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

/**
 * Tests for the component resource provider.
 */
public class ComponentResourceProviderTest {
  @Test
  public void testUpdateResources() throws Exception {
    Resource.Type type = Resource.Type.Component;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    Set<ServiceComponentResponse> nameResponse = new HashSet<ServiceComponentResponse>();
    nameResponse.add(new ServiceComponentResponse(102L, "Cluster102", "Service", "Component", null, "1", "STARTED"));

    // set expectations
    expect(managementController.getComponents(EasyMock.<Set<ServiceComponentRequest>>anyObject())).andReturn(nameResponse).once();
    expect(managementController.updateComponents(
        AbstractResourceProviderTest.Matchers.componentRequestSet("Cluster102", "Service", "Component", null, "STARTED"))).andReturn(response).once();

    // replay
    replay(managementController, response);

    ResourceProvider provider = AbstractResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    properties.put(ComponentResourceProvider.COMPONENT_STATE_PROPERTY_ID, "STARTED");

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties);

    // update the cluster named Cluster102
    Predicate predicate = new PredicateBuilder().property(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID).equals("Cluster102").toPredicate();
    provider.updateResources(request, predicate);

    // verify
    verify(managementController, response);
  }
}
