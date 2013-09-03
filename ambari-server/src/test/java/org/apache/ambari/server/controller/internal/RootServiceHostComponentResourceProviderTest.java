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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.RootServiceHostComponentRequest;
import org.apache.ambari.server.controller.RootServiceHostComponentResponse;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

public class RootServiceHostComponentResourceProviderTest {
    
  @Test
  public void testGetResources() throws Exception{
    Resource.Type type = Resource.Type.RootServiceHostComponent;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    Set<RootServiceHostComponentResponse> allResponse = new HashSet<RootServiceHostComponentResponse>();
    allResponse.add(new RootServiceHostComponentResponse("host1", "component1", "HEALTHY", "1.1.1", Collections.<String,String>emptyMap()));
    allResponse.add(new RootServiceHostComponentResponse("host2", "component2", "HEALTHY", "1.1.1", Collections.<String,String>emptyMap()));
    allResponse.add(new RootServiceHostComponentResponse("host3", "component3", "HEARBEAT_LOST", "1.1.1", Collections.<String,String>emptyMap()));

    Set<RootServiceHostComponentResponse> nameResponse = new HashSet<RootServiceHostComponentResponse>();
    nameResponse.add(new RootServiceHostComponentResponse("host4", "component4", "HEALTHY", "1.1.1", Collections.<String,String>emptyMap()));


    // set expectations
    expect(managementController.getRootServiceHostComponents(EasyMock.<Set<RootServiceHostComponentRequest>>anyObject())).andReturn(allResponse).once();
    expect(managementController.getRootServiceHostComponents(EasyMock.<Set<RootServiceHostComponentRequest>>anyObject())).andReturn(nameResponse).once();
    // replay
    replay(managementController);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    Set<String> propertyIds = new HashSet<String>();

    propertyIds.add(RootServiceHostComponentResourceProvider.SERVICE_NAME_PROPERTY_ID);
    propertyIds.add(RootServiceHostComponentResourceProvider.HOST_NAME_PROPERTY_ID);
    propertyIds.add(RootServiceHostComponentResourceProvider.COMPONENT_NAME_PROPERTY_ID);
    propertyIds.add(RootServiceHostComponentResourceProvider.COMPONENT_STATE_PROPERTY_ID);
    propertyIds.add(RootServiceHostComponentResourceProvider.PROPERTIES_PROPERTY_ID);
    propertyIds.add(RootServiceHostComponentResourceProvider.COMPONENT_VERSION_PROPERTY_ID);
    
    
    // create the request
    Request request = PropertyHelper.getReadRequest(propertyIds);

    // get all ... no predicate
    Set<Resource> resources = provider.getResources(request, null);

    Assert.assertEquals(allResponse.size(), resources.size());
    for (Resource resource : resources) {
      String hostName = (String) resource.getPropertyValue(RootServiceHostComponentResourceProvider.HOST_NAME_PROPERTY_ID);
      String componentName = (String) resource.getPropertyValue(RootServiceHostComponentResourceProvider.COMPONENT_NAME_PROPERTY_ID);
      String componentState = (String) resource.getPropertyValue(RootServiceHostComponentResourceProvider.COMPONENT_STATE_PROPERTY_ID);
      String componentVersion = (String) resource.getPropertyValue(RootServiceHostComponentResourceProvider.COMPONENT_VERSION_PROPERTY_ID);
      Assert.assertTrue(allResponse.contains(new RootServiceHostComponentResponse(hostName, componentName, componentState, componentVersion,
          Collections.<String, String>emptyMap())));
    }

    // get service named service4
    Predicate predicate =
        new PredicateBuilder().property(RootServiceHostComponentResourceProvider.COMPONENT_NAME_PROPERTY_ID).
        equals("component4").toPredicate();
    resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    Assert.assertEquals("component4", resources.iterator().next().
        getPropertyValue(RootServiceHostComponentResourceProvider.COMPONENT_NAME_PROPERTY_ID));


    // verify
    verify(managementController);
  }

}
