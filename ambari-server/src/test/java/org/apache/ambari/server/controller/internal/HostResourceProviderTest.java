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
import org.apache.ambari.server.controller.HostRequest;
import org.apache.ambari.server.controller.HostResponse;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.state.HostHealthStatus;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

/**
 * HostResourceProvider tests.
 */
public class HostResourceProviderTest {
  @Test
  public void testCreateResources() throws Exception {
    Resource.Type type = Resource.Type.Host;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    managementController.createHosts(
        AbstractResourceProviderTest.Matcher.getHostRequestSet("Host100", "Cluster100", null));

    // replay
    replay(managementController, response);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<Map<String, Object>>();

    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    // add properties to the request map
    properties.put(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    properties.put(HostResourceProvider.HOST_NAME_PROPERTY_ID, "Host100");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    provider.createResources(request);

    // verify
    verify(managementController, response);
  }

  @Test
  public void testGetResources() throws Exception {
    Resource.Type type = Resource.Type.Host;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    Set<HostResponse> allResponse = new HashSet<HostResponse>();
    allResponse.add(new HostResponse("Host100", "Cluster100",
        "", "", 2, "", "", "", 100000L, 200000L, null, 10L,
        0L, "rack info", null, null,
        new HostHealthStatus(HostHealthStatus.HealthStatus.HEALTHY, "HEALTHY"), "HEALTHY"));
    allResponse.add(new HostResponse("Host101", "Cluster100",
        "", "", 2, "", "", "", 100000L, 200000L, null, 10L,
        0L, "rack info", null, null,
        new HostHealthStatus(HostHealthStatus.HealthStatus.HEALTHY, "HEALTHY"), "HEALTHY"));
    allResponse.add(new HostResponse("Host102", "Cluster100",
        "", "", 2, "", "", "", 100000L, 200000L, null, 10L,
        0L, "rack info", null, null,
        new HostHealthStatus(HostHealthStatus.HealthStatus.HEALTHY, "HEALTHY"), "HEALTHY"));

    // set expectations
    expect(managementController.getHosts(
        AbstractResourceProviderTest.Matcher.getHostRequestSet(null, "Cluster100", null))).
        andReturn(allResponse).once();

    // replay
    replay(managementController);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    Set<String> propertyIds = new HashSet<String>();

    propertyIds.add(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID);
    propertyIds.add(HostResourceProvider.HOST_NAME_PROPERTY_ID);

    Predicate predicate =
        new PredicateBuilder().property(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").
            toPredicate();
    Request request = PropertyHelper.getReadRequest(propertyIds);
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(3, resources.size());
    Set<String> names = new HashSet<String>();
    for (Resource resource : resources) {
      String clusterName = (String) resource.getPropertyValue(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID);
      Assert.assertEquals("Cluster100", clusterName);
      names.add((String) resource.getPropertyValue(HostResourceProvider.HOST_NAME_PROPERTY_ID));
    }
    // Make sure that all of the response objects got moved into resources
    for (HostResponse response : allResponse ) {
      Assert.assertTrue(names.contains(response.getHostname()));
    }

    // verify
    verify(managementController);
  }
  
  @Test
  public void testUpdateDesiredConfig() throws Exception {

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    HostResponse hr = new HostResponse("Host100", "Cluster100",
        "", "", 2, "", "", "", 100000L, 200000L, null, 10L,
        0L, "rack info", null, null,
        new HostHealthStatus(HostHealthStatus.HealthStatus.HEALTHY, "HEALTHY"), "HEALTHY");
    
    Set<HostResponse> hostResponseSet = new HashSet<HostResponse>();
    hostResponseSet.add(hr);

    // set expectations
    expect(managementController.getHosts(
        AbstractResourceProviderTest.Matcher.getHostRequestSet("Host100", "Cluster100", null))).
        andReturn(hostResponseSet);
    managementController.updateHosts(EasyMock.<Set<HostRequest>>anyObject());

    // replay
    replay(managementController, response);
    

    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    properties.put(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    properties.put(HostResourceProvider.HOST_NAME_PROPERTY_ID, "Host100");
    
    properties.put(PropertyHelper.getPropertyId("Hosts.desired_config", "type"), "global");
    properties.put(PropertyHelper.getPropertyId("Hosts.desired_config", "tag"), "version1");
    properties.put(PropertyHelper.getPropertyId("Hosts.desired_config.properties", "a"), "b");
    properties.put(PropertyHelper.getPropertyId("Hosts.desired_config.properties", "x"), "y");

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, null);
    
    Predicate  predicate = new PredicateBuilder().property(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID).
        equals("Cluster100").
        and().property(HostResourceProvider.HOST_NAME_PROPERTY_ID).equals("Host100").toPredicate();
    
    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        Resource.Type.Host,
        PropertyHelper.getPropertyIds(Resource.Type.Host),
        PropertyHelper.getKeyPropertyIds(Resource.Type.Host),
        managementController);
    
    provider.updateResources(request, predicate);
    
    // verify
    verify(managementController, response);    
  }

  @Test
  public void testUpdateResources() throws Exception {
    Resource.Type type = Resource.Type.Host;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    Set<HostResponse> hostResponseSet = new HashSet<HostResponse>();
    hostResponseSet.add(new HostResponse("Host100", "Cluster100",
        "", "", 2, "", "", "", 100000L, 200000L, null, 10L,
        0L, "rack info", null, null,
        new HostHealthStatus(HostHealthStatus.HealthStatus.HEALTHY, "HEALTHY"), "HEALTHY"));

    // set expectations
    expect(managementController.getHosts(
        AbstractResourceProviderTest.Matcher.getHostRequestSet("Host100", "Cluster100", null))).
        andReturn(hostResponseSet);
    managementController.updateHosts(EasyMock.<Set<HostRequest>>anyObject());

    // replay
    replay(managementController, response);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    // add the property map to a set for the request.
    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    properties.put(HostResourceProvider.HOST_RACK_INFO_PROPERTY_ID, "rack info");

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, null);

    Predicate  predicate = new PredicateBuilder().property(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID).
        equals("Cluster100").
        and().property(HostResourceProvider.HOST_NAME_PROPERTY_ID).equals("Host100").toPredicate();
    provider.updateResources(request, predicate);

    // verify
    verify(managementController, response);
  }

  @Test
  public void testDeleteResources() throws Exception {
    Resource.Type type = Resource.Type.Host;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    // set expectations
    managementController.deleteHosts(AbstractResourceProviderTest.Matcher.getHostRequestSet("Host100", null, null));

    // replay
    replay(managementController);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();

    ((ObservableResourceProvider)provider).addObserver(observer);

    Predicate predicate = new PredicateBuilder().property(HostResourceProvider.HOST_NAME_PROPERTY_ID).equals("Host100").
        toPredicate();
    provider.deleteResources(predicate);


    ResourceProviderEvent lastEvent = observer.getLastEvent();
    Assert.assertNotNull(lastEvent);
    Assert.assertEquals(Resource.Type.Host, lastEvent.getResourceType());
    Assert.assertEquals(ResourceProviderEvent.Type.Delete, lastEvent.getType());
    Assert.assertEquals(predicate, lastEvent.getPredicate());
    Assert.assertNull(lastEvent.getRequest());

    // verify
    verify(managementController);
  }
}
