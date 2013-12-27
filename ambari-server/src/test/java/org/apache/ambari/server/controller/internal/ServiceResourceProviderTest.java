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

import static org.easymock.EasyMock.anyBoolean;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.ServiceComponentHostRequest;
import org.apache.ambari.server.controller.ServiceComponentHostResponse;
import org.apache.ambari.server.controller.ServiceRequest;
import org.apache.ambari.server.controller.ServiceResponse;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.easymock.Capture;
import org.junit.Assert;
import org.junit.Test;

/**
 * ServiceResourceProvider tests.
 */
public class ServiceResourceProviderTest {

  @Test
  public void testCreateResources() throws Exception{
    AmbariManagementController managementController = createNiceMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Service service = createNiceMock(Service.class);
    StackId stackId = createNiceMock(StackId.class);
    ServiceFactory serviceFactory = createNiceMock(ServiceFactory.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);

    expect(managementController.getClusters()).andReturn(clusters);
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo);
    expect(managementController.getServiceFactory()).andReturn(serviceFactory);

    expect(serviceFactory.createNew(cluster, "Service100")).andReturn(service);

    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();

    expect(cluster.getService("Service100")).andReturn(null);
    expect(cluster.getDesiredStackVersion()).andReturn(stackId);

    expect(ambariMetaInfo.isValidService( (String) anyObject(), (String) anyObject(), (String) anyObject())).andReturn(true);

    // replay
    replay(managementController, clusters, cluster, service, ambariMetaInfo, stackId, serviceFactory);

    ResourceProvider provider = getServiceProvider(managementController);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<Map<String, Object>>();

    // Service 1: create a map of properties for the request
    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    // add properties to the request map
    properties.put(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    properties.put(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID, "Service100");
    properties.put(ServiceResourceProvider.SERVICE_SERVICE_STATE_PROPERTY_ID, "INIT");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    provider.createResources(request);

    // verify
    verify(managementController, clusters, cluster, service, ambariMetaInfo, stackId, serviceFactory);
  }

  @Test
  public void testGetResources() throws Exception{
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Service service0 = createNiceMock(Service.class);
    Service service1 = createNiceMock(Service.class);
    Service service2 = createNiceMock(Service.class);
    Service service3 = createNiceMock(Service.class);
    Service service4 = createNiceMock(Service.class);
    ServiceResponse serviceResponse0 = createNiceMock(ServiceResponse.class);
    ServiceResponse serviceResponse1 = createNiceMock(ServiceResponse.class);
    ServiceResponse serviceResponse2 = createNiceMock(ServiceResponse.class);
    ServiceResponse serviceResponse3 = createNiceMock(ServiceResponse.class);
    ServiceResponse serviceResponse4 = createNiceMock(ServiceResponse.class);

    StackId stackId = createNiceMock(StackId.class);
    ServiceFactory serviceFactory = createNiceMock(ServiceFactory.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);

    Map<String, Service> allResponseMap = new HashMap<String, Service>();
    allResponseMap.put("Service100", service0);
    allResponseMap.put("Service101", service1);
    allResponseMap.put("Service102", service2);
    allResponseMap.put("Service103", service3);
    allResponseMap.put("Service104", service4);

    // set expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getServiceFactory()).andReturn(serviceFactory).anyTimes();
    expect(managementController.getHostComponents((Set<ServiceComponentHostRequest>) anyObject())).
        andReturn(Collections.<ServiceComponentHostResponse>emptySet()).anyTimes();

    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();

    expect(cluster.getServices()).andReturn(allResponseMap).anyTimes();
    expect(cluster.getService("Service102")).andReturn(service2);

    expect(service0.convertToResponse()).andReturn(serviceResponse0).anyTimes();
    expect(service1.convertToResponse()).andReturn(serviceResponse1).anyTimes();
    expect(service2.convertToResponse()).andReturn(serviceResponse2).anyTimes();
    expect(service3.convertToResponse()).andReturn(serviceResponse3).anyTimes();
    expect(service4.convertToResponse()).andReturn(serviceResponse4).anyTimes();

    expect(service0.getName()).andReturn("Service100").anyTimes();
    expect(service1.getName()).andReturn("Service101").anyTimes();
    expect(service2.getName()).andReturn("Service102").anyTimes();
    expect(service3.getName()).andReturn("Service103").anyTimes();
    expect(service4.getName()).andReturn("Service104").anyTimes();

    expect(service0.getDesiredState()).andReturn(State.INIT);
    expect(service1.getDesiredState()).andReturn(State.INSTALLED);
    expect(service2.getDesiredState()).andReturn(State.INIT);
    expect(service3.getDesiredState()).andReturn(State.INSTALLED);
    expect(service4.getDesiredState()).andReturn(State.INIT);

    expect(serviceResponse0.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(serviceResponse0.getServiceName()).andReturn("Service100").anyTimes();
    expect(serviceResponse1.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(serviceResponse1.getServiceName()).andReturn("Service101").anyTimes();
    expect(serviceResponse2.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(serviceResponse2.getServiceName()).andReturn("Service102").anyTimes();
    expect(serviceResponse3.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(serviceResponse3.getServiceName()).andReturn("Service103").anyTimes();
    expect(serviceResponse4.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(serviceResponse4.getServiceName()).andReturn("Service104").anyTimes();

    // replay
    replay(managementController, clusters, cluster,
        service0, service1, service2, service3, service4,
        serviceResponse0, serviceResponse1, serviceResponse2, serviceResponse3, serviceResponse4,
        ambariMetaInfo, stackId, serviceFactory);

    ResourceProvider provider = getServiceProvider(managementController);

    Set<String> propertyIds = new HashSet<String>();

    propertyIds.add(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID);
    propertyIds.add(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID);

    // create the request
    Predicate predicate = new PredicateBuilder().property(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").toPredicate();
    Request request = PropertyHelper.getReadRequest("ServiceInfo");
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(5, resources.size());
    Set<String> names = new HashSet<String>();
    for (Resource resource : resources) {
      String clusterName = (String) resource.getPropertyValue(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID);
      Assert.assertEquals("Cluster100", clusterName);
      names.add((String) resource.getPropertyValue(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID));
    }
    // Make sure that all of the response objects got moved into resources
    for (Service service : allResponseMap.values() ) {
      Assert.assertTrue(names.contains(service.getName()));
    }

    // get service named Service102
    predicate = new PredicateBuilder().property(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").and().
        property(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID).equals("Service102").toPredicate();
    request = PropertyHelper.getReadRequest("ServiceInfo");
    resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    Assert.assertEquals("Service102", resources.iterator().next().getPropertyValue(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID));

    // get services where state == "INIT"
    predicate = new PredicateBuilder().property(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").and().
        property(ServiceResourceProvider.SERVICE_SERVICE_STATE_PROPERTY_ID).equals("INIT").toPredicate();
    request = PropertyHelper.getReadRequest(propertyIds);
    resources = provider.getResources(request, predicate);

    Assert.assertEquals(3, resources.size());
    names = new HashSet<String>();
    for (Resource resource : resources) {
      String clusterName = (String) resource.getPropertyValue(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID);
      Assert.assertEquals("Cluster100", clusterName);
      names.add((String) resource.getPropertyValue(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID));
    }

    // verify
    verify(managementController, clusters, cluster,
        service0, service1, service2, service3, service4,
        serviceResponse0, serviceResponse1, serviceResponse2, serviceResponse3, serviceResponse4,
        ambariMetaInfo, stackId, serviceFactory);
  }

  @Test
  public void testUpdateResources() throws Exception{
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Service service0 = createNiceMock(Service.class);
    ServiceFactory serviceFactory = createNiceMock(ServiceFactory.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    RequestStatusResponse requestStatusResponse = createNiceMock(RequestStatusResponse.class);

    Map<String, String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "Called from a test");

    // set expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getServiceFactory()).andReturn(serviceFactory).anyTimes();

    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();

    expect(cluster.getService("Service102")).andReturn(service0);

    expect(service0.getDesiredState()).andReturn(State.INSTALLED).anyTimes();
    expect(service0.getServiceComponents()).andReturn(Collections.<String, ServiceComponent>emptyMap()).anyTimes();

    Capture<Map<String, String>> requestPropertiesCapture = new Capture<Map<String, String>>();
    Capture<Map<State, List<Service>>> changedServicesCapture = new Capture<Map<State, List<Service>>>();
    Capture<Map<State, List<ServiceComponent>>> changedCompsCapture = new Capture<Map<State, List<ServiceComponent>>>();
    Capture<Map<String, Map<State, List<ServiceComponentHost>>>> changedScHostsCapture = new Capture<Map<String, Map<State, List<ServiceComponentHost>>>>();
    Capture<Map<String, String>> requestParametersCapture = new Capture<Map<String, String>>();
    Capture<Collection<ServiceComponentHost>> ignoredScHostsCapture = new Capture<Collection<ServiceComponentHost>>();
    Capture<Cluster> clusterCapture = new Capture<Cluster>();

    expect(managementController.createStages(capture(clusterCapture), capture(requestPropertiesCapture), capture(requestParametersCapture), capture(changedServicesCapture), capture(changedCompsCapture), capture(changedScHostsCapture), capture(ignoredScHostsCapture), anyBoolean(), anyBoolean()
    )).andReturn(requestStatusResponse);

    // replay
    replay(managementController, clusters, cluster,
        service0, serviceFactory, ambariMetaInfo, requestStatusResponse);

    ResourceProvider provider = getServiceProvider(managementController);

    // add the property map to a set for the request.
    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    properties.put(ServiceResourceProvider.SERVICE_SERVICE_STATE_PROPERTY_ID, "STARTED");

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, mapRequestProps);

    // update the service named Service102
    Predicate  predicate = new PredicateBuilder().property(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").
        and().property(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID).equals("Service102").toPredicate();
    provider.updateResources(request, predicate);

    // verify
    verify(managementController, clusters, cluster,
        service0, serviceFactory, ambariMetaInfo, requestStatusResponse);
  }

  @Test
  public void testReconfigureClientsFlag() throws Exception {
    AmbariManagementController managementController1 = createMock(AmbariManagementController.class);
    AmbariManagementController managementController2 = createMock
        (AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Service service0 = createNiceMock(Service.class);
    ServiceResponse serviceResponse0 = createNiceMock(ServiceResponse.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);

    RequestStatusResponse response1 = createNiceMock(RequestStatusResponse.class);
    RequestStatusResponse response2 = createNiceMock(RequestStatusResponse
      .class);

    Map<String, String> mapRequestProps = new HashMap<String, String>();
    mapRequestProps.put("context", "Called from a test");

    // set expectations
    expect(managementController1.getHostComponents((Set<ServiceComponentHostRequest>) anyObject())).
        andReturn(Collections.<ServiceComponentHostResponse>emptySet()).anyTimes();
    expect(managementController2.getHostComponents((Set<ServiceComponentHostRequest>) anyObject())).
        andReturn(Collections.<ServiceComponentHostResponse>emptySet()).anyTimes();

    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();

    expect(managementController1.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController1.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();

    expect(managementController2.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController2.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();

    expect(cluster.getService("Service102")).andReturn(service0).anyTimes();

    expect(service0.convertToResponse()).andReturn(serviceResponse0).anyTimes();
    expect(service0.getDesiredState()).andReturn(State.INSTALLED).anyTimes();
    expect(service0.getServiceComponents()).andReturn(Collections.<String, ServiceComponent>emptyMap()).anyTimes();

    expect(serviceResponse0.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(serviceResponse0.getServiceName()).andReturn("Service102").anyTimes();

    Capture<Map<String, String>> requestPropertiesCapture = new Capture<Map<String, String>>();
    Capture<Map<State, List<Service>>> changedServicesCapture = new Capture<Map<State, List<Service>>>();
    Capture<Map<State, List<ServiceComponent>>> changedCompsCapture = new Capture<Map<State, List<ServiceComponent>>>();
    Capture<Map<String, Map<State, List<ServiceComponentHost>>>> changedScHostsCapture = new Capture<Map<String, Map<State, List<ServiceComponentHost>>>>();
    Capture<Map<String, String>> requestParametersCapture = new Capture<Map<String, String>>();
    Capture<Collection<ServiceComponentHost>> ignoredScHostsCapture = new Capture<Collection<ServiceComponentHost>>();
    Capture<Cluster> clusterCapture = new Capture<Cluster>();

    expect(managementController1.createStages(capture(clusterCapture), capture(requestPropertiesCapture), capture(requestParametersCapture), capture(changedServicesCapture), capture(changedCompsCapture), capture(changedScHostsCapture), capture(ignoredScHostsCapture), anyBoolean(), anyBoolean()
    )).andReturn(response1);

    expect(managementController2.createStages(capture(clusterCapture), capture(requestPropertiesCapture), capture(requestParametersCapture), capture(changedServicesCapture), capture(changedCompsCapture), capture(changedScHostsCapture), capture(ignoredScHostsCapture), anyBoolean(), anyBoolean()
    )).andReturn(response2);

    // replay
    replay(managementController1, response1, managementController2, response2,
        clusters, cluster, service0, serviceResponse0, ambariMetaInfo);

    ResourceProvider provider1 = getServiceProvider(managementController1);

    ResourceProvider provider2 = getServiceProvider(managementController2);

    // add the property map to a set for the request.
    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    properties.put(ServiceResourceProvider.SERVICE_SERVICE_STATE_PROPERTY_ID,
      "STARTED");

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, mapRequestProps);

    // update the service named Service102
    Predicate  predicate1 = new PredicateBuilder().property
      (ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").
      and().property(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID).
      equals("Service102").and().property("params/reconfigure_client").
      equals("true").toPredicate();

    Predicate  predicate2 = new PredicateBuilder().property
      (ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").
      and().property(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID).
      equals("Service102").and().property("params/reconfigure_client").equals
      ("false").toPredicate();

    provider1.updateResources(request, predicate1);
    provider2.updateResources(request, predicate2);

    // verify
    verify(managementController1, response1, managementController2, response2,
        clusters, cluster, service0, serviceResponse0, ambariMetaInfo);
  }

  @Test
  public void testDeleteResources() throws Exception{
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);

    // set expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();
    cluster.deleteService("Service100");

    // replay
    replay(managementController, clusters, cluster);

    ResourceProvider provider = getServiceProvider(managementController);

    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();

    ((ObservableResourceProvider)provider).addObserver(observer);

    // delete the service named Service100
    Predicate  predicate = new PredicateBuilder().property(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").and()
        .property(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID).equals("Service100").toPredicate();
    provider.deleteResources(predicate);


    ResourceProviderEvent lastEvent = observer.getLastEvent();
    Assert.assertNotNull(lastEvent);
    Assert.assertEquals(Resource.Type.Service, lastEvent.getResourceType());
    Assert.assertEquals(ResourceProviderEvent.Type.Delete, lastEvent.getType());
    Assert.assertEquals(predicate, lastEvent.getPredicate());
    Assert.assertNull(lastEvent.getRequest());

    // verify
    verify(managementController, clusters, cluster);
  }

  @Test
  public void testCheckPropertyIds() throws Exception {
    Set<String> propertyIds = new HashSet<String>();
    propertyIds.add("foo");
    propertyIds.add("cat1/foo");
    propertyIds.add("cat2/bar");
    propertyIds.add("cat2/baz");
    propertyIds.add("cat3/sub1/bam");
    propertyIds.add("cat4/sub2/sub3/bat");
    propertyIds.add("cat5/subcat5/map");

    Map<Resource.Type, String> keyPropertyIds = new HashMap<Resource.Type, String>();

    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    AbstractResourceProvider provider =
        (AbstractResourceProvider) AbstractControllerResourceProvider.getResourceProvider(
            Resource.Type.Service,
            propertyIds,
            keyPropertyIds,
            managementController);

    Set<String> unsupported = provider.checkPropertyIds(Collections.singleton("foo"));
    Assert.assertTrue(unsupported.isEmpty());

    // note that key is not in the set of known property ids.  We allow it if its parent is a known property.
    // this allows for Map type properties where we want to treat the entries as individual properties
    Assert.assertTrue(provider.checkPropertyIds(Collections.singleton("cat5/subcat5/map/key")).isEmpty());

    unsupported = provider.checkPropertyIds(Collections.singleton("bar"));
    Assert.assertEquals(1, unsupported.size());
    Assert.assertTrue(unsupported.contains("bar"));

    unsupported = provider.checkPropertyIds(Collections.singleton("cat1/foo"));
    Assert.assertTrue(unsupported.isEmpty());

    unsupported = provider.checkPropertyIds(Collections.singleton("cat1"));
    Assert.assertTrue(unsupported.isEmpty());

    unsupported = provider.checkPropertyIds(Collections.singleton("config"));
    Assert.assertTrue(unsupported.isEmpty());

    unsupported = provider.checkPropertyIds(Collections.singleton("config/unknown_property"));
    Assert.assertTrue(unsupported.isEmpty());
  }

  @Test
  public void testDefaultServiceState_STARTED() throws Exception{
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    StackId stackId = createNiceMock(StackId.class);
    ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);

    ServiceComponentHostResponse shr1 = new ServiceComponentHostResponse("C1", "MAPREDUCE", "JOBTRACKER", "Host100", "STARTED", "", null, null);
    ServiceComponentHostResponse shr2 = new ServiceComponentHostResponse("C1", "MAPREDUCE", "MAPREDUCE_CLIENT", "Host100", "STARTED", "", null, null);
    ServiceComponentHostResponse shr3 = new ServiceComponentHostResponse("C1", "MAPREDUCE", "TASKTRACKER", "Host100", "STARTED", "", null, null);

    Set<ServiceComponentHostResponse> responses = new LinkedHashSet<ServiceComponentHostResponse>();
    responses.add(shr1);
    responses.add(shr2);
    responses.add(shr3);

    // set expectations
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("C1")).andReturn(cluster).anyTimes();
    expect(managementController.getHostComponents((Set<ServiceComponentHostRequest>) anyObject())).andReturn(responses).anyTimes();
    expect(cluster.getDesiredStackVersion()).andReturn(stackId);

    expect(stackId.getStackName()).andReturn("S1").anyTimes();
    expect(stackId.getStackVersion()).andReturn("V1").anyTimes();


    expect(ambariMetaInfo.getComponentCategory((String) anyObject(), (String) anyObject(),
        (String) anyObject(), (String) anyObject())).andReturn(componentInfo).anyTimes();

    expect(componentInfo.isMaster()).andReturn(true);
    expect(componentInfo.isMaster()).andReturn(false);
    expect(componentInfo.isMaster()).andReturn(false);

    // replay
    replay(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo);

    ServiceResourceProvider.ServiceState serviceState = new ServiceResourceProvider.DefaultServiceState();

    State state = serviceState.getState(managementController, "C1", "MAPREDUCE");
    Assert.assertEquals(State.STARTED, state);

    // verify
    verify(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo);
  }

  @Test
  public void testDefaultServiceState_UNKNOWN() throws Exception{
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    StackId stackId = createNiceMock(StackId.class);
    ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);

    ServiceComponentHostResponse shr1 = new ServiceComponentHostResponse("C1", "MAPREDUCE", "JOBTRACKER", "Host100", "UNKNOWN", "", null, null);
    ServiceComponentHostResponse shr2 = new ServiceComponentHostResponse("C1", "MAPREDUCE", "MAPREDUCE_CLIENT", "Host100", "STARTED", "", null, null);
    ServiceComponentHostResponse shr3 = new ServiceComponentHostResponse("C1", "MAPREDUCE", "TASKTRACKER", "Host100", "STARTED", "", null, null);

    Set<ServiceComponentHostResponse> responses = new LinkedHashSet<ServiceComponentHostResponse>();
    responses.add(shr1);
    responses.add(shr2);
    responses.add(shr3);

    // set expectations
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("C1")).andReturn(cluster).anyTimes();
    expect(managementController.getHostComponents((Set<ServiceComponentHostRequest>) anyObject())).andReturn(responses).anyTimes();
    expect(cluster.getDesiredStackVersion()).andReturn(stackId);

    expect(stackId.getStackName()).andReturn("S1").anyTimes();
    expect(stackId.getStackVersion()).andReturn("V1").anyTimes();


    expect(ambariMetaInfo.getComponentCategory((String) anyObject(), (String) anyObject(),
        (String) anyObject(), (String) anyObject())).andReturn(componentInfo).anyTimes();

    expect(componentInfo.isMaster()).andReturn(true);

    // replay
    replay(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo);

    ServiceResourceProvider.ServiceState serviceState = new ServiceResourceProvider.DefaultServiceState();

    State state = serviceState.getState(managementController, "C1", "MAPREDUCE");
    Assert.assertEquals(State.UNKNOWN, state);

    // verify
    verify(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo);
  }

  @Test
  public void testDefaultServiceState_STARTING() throws Exception{
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    StackId stackId = createNiceMock(StackId.class);
    ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);

    ServiceComponentHostResponse shr1 = new ServiceComponentHostResponse("C1", "MAPREDUCE", "JOBTRACKER", "Host100", "STARTING", "", null, null);
    ServiceComponentHostResponse shr2 = new ServiceComponentHostResponse("C1", "MAPREDUCE", "MAPREDUCE_CLIENT", "Host100", "STARTED", "", null, null);
    ServiceComponentHostResponse shr3 = new ServiceComponentHostResponse("C1", "MAPREDUCE", "TASKTRACKER", "Host100", "STARTED", "", null, null);

    Set<ServiceComponentHostResponse> responses = new LinkedHashSet<ServiceComponentHostResponse>();
    responses.add(shr1);
    responses.add(shr2);
    responses.add(shr3);

    // set expectations
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("C1")).andReturn(cluster).anyTimes();
    expect(managementController.getHostComponents((Set<ServiceComponentHostRequest>) anyObject())).andReturn(responses).anyTimes();
    expect(cluster.getDesiredStackVersion()).andReturn(stackId);

    expect(stackId.getStackName()).andReturn("S1").anyTimes();
    expect(stackId.getStackVersion()).andReturn("V1").anyTimes();


    expect(ambariMetaInfo.getComponentCategory((String) anyObject(), (String) anyObject(),
        (String) anyObject(), (String) anyObject())).andReturn(componentInfo).anyTimes();

    expect(componentInfo.isMaster()).andReturn(true);

    // replay
    replay(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo);

    ServiceResourceProvider.ServiceState serviceState = new ServiceResourceProvider.DefaultServiceState();

    State state = serviceState.getState(managementController, "C1", "MAPREDUCE");
    Assert.assertEquals(State.STARTING, state);

    // verify
    verify(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo);
  }

  @Test
  public void testDefaultServiceState_STOPPED() throws Exception{
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    StackId stackId = createNiceMock(StackId.class);
    ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);

    ServiceComponentHostResponse shr1 = new ServiceComponentHostResponse("C1", "MAPREDUCE", "JOBTRACKER", "Host100", "INSTALLED", "", null, null);
    ServiceComponentHostResponse shr2 = new ServiceComponentHostResponse("C1", "MAPREDUCE", "MAPREDUCE_CLIENT", "Host100", "STARTED", "", null, null);
    ServiceComponentHostResponse shr3 = new ServiceComponentHostResponse("C1", "MAPREDUCE", "TASKTRACKER", "Host100", "STARTED", "", null, null);

    Set<ServiceComponentHostResponse> responses = new LinkedHashSet<ServiceComponentHostResponse>();
    responses.add(shr1);
    responses.add(shr2);
    responses.add(shr3);

    // set expectations
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("C1")).andReturn(cluster).anyTimes();
    expect(managementController.getHostComponents((Set<ServiceComponentHostRequest>) anyObject())).andReturn(responses).anyTimes();
    expect(cluster.getDesiredStackVersion()).andReturn(stackId);

    expect(stackId.getStackName()).andReturn("S1").anyTimes();
    expect(stackId.getStackVersion()).andReturn("V1").anyTimes();


    expect(ambariMetaInfo.getComponentCategory((String) anyObject(), (String) anyObject(),
        (String) anyObject(), (String) anyObject())).andReturn(componentInfo).anyTimes();

    expect(componentInfo.isMaster()).andReturn(true);

    // replay
    replay(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo);

    ServiceResourceProvider.ServiceState serviceState = new ServiceResourceProvider.DefaultServiceState();

    State state = serviceState.getState(managementController, "C1", "MAPREDUCE");
    Assert.assertEquals(State.INSTALLED, state);

    // verify
    verify(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo);
  }

  @Test
  public void testDefaultServiceState_MAINTENANCE() throws Exception{
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    StackId stackId = createNiceMock(StackId.class);
    ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);

    ServiceComponentHostResponse shr1 = new ServiceComponentHostResponse("C1", "MAPREDUCE", "JOBTRACKER", "Host100", "MAINTENANCE", "", null, null);
    ServiceComponentHostResponse shr2 = new ServiceComponentHostResponse("C1", "MAPREDUCE", "MAPREDUCE_CLIENT", "Host100", "STARTED", "", null, null);
    ServiceComponentHostResponse shr3 = new ServiceComponentHostResponse("C1", "MAPREDUCE", "TASKTRACKER", "Host100", "STARTED", "", null, null);

    Set<ServiceComponentHostResponse> responses = new LinkedHashSet<ServiceComponentHostResponse>();
    responses.add(shr1);
    responses.add(shr2);
    responses.add(shr3);

    // set expectations
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("C1")).andReturn(cluster).anyTimes();
    expect(managementController.getHostComponents((Set<ServiceComponentHostRequest>) anyObject())).andReturn(responses).anyTimes();
    expect(cluster.getDesiredStackVersion()).andReturn(stackId);

    expect(stackId.getStackName()).andReturn("S1").anyTimes();
    expect(stackId.getStackVersion()).andReturn("V1").anyTimes();


    expect(ambariMetaInfo.getComponentCategory((String) anyObject(), (String) anyObject(),
        (String) anyObject(), (String) anyObject())).andReturn(componentInfo).anyTimes();

    expect(componentInfo.isMaster()).andReturn(true);
    expect(componentInfo.isMaster()).andReturn(false);
    expect(componentInfo.isMaster()).andReturn(false);

    // replay
    replay(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo);

    ServiceResourceProvider.ServiceState serviceState = new ServiceResourceProvider.DefaultServiceState();

    State state = serviceState.getState(managementController, "C1", "MAPREDUCE");
    Assert.assertEquals(State.STARTED, state);

    // verify
    verify(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo);
  }

  @Test
  public void testHDFSServiceState_STARTED() throws Exception{
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    StackId stackId = createNiceMock(StackId.class);
    ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);

    ServiceComponentHostResponse shr1 = new ServiceComponentHostResponse("C1", "HDFS", "NAMENODE", "Host100",  "STARTED", "", null, null);
    ServiceComponentHostResponse shr2 = new ServiceComponentHostResponse("C1", "HDFS", "SECONDARY_NAMENODE", "Host100", "STARTED", "", null, null);
    ServiceComponentHostResponse shr3 = new ServiceComponentHostResponse("C1", "HDFS", "JOURNALNODE", "Host100", "STARTED", "", null, null);

    Set<ServiceComponentHostResponse> responses = new LinkedHashSet<ServiceComponentHostResponse>();
    responses.add(shr1);
    responses.add(shr2);
    responses.add(shr3);

    // set expectations
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("C1")).andReturn(cluster).anyTimes();
    expect(managementController.getHostComponents((Set<ServiceComponentHostRequest>) anyObject())).andReturn(responses).anyTimes();
    expect(cluster.getDesiredStackVersion()).andReturn(stackId);

    expect(stackId.getStackName()).andReturn("S1").anyTimes();
    expect(stackId.getStackVersion()).andReturn("V1").anyTimes();


    expect(ambariMetaInfo.getComponentCategory((String) anyObject(), (String) anyObject(),
        (String) anyObject(), (String) anyObject())).andReturn(componentInfo).anyTimes();

    expect(componentInfo.isMaster()).andReturn(true);
    expect(componentInfo.isMaster()).andReturn(true);
    expect(componentInfo.isMaster()).andReturn(true);

    // replay
    replay(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo);

    ServiceResourceProvider.ServiceState serviceState = new ServiceResourceProvider.HDFSServiceState();

    State state = serviceState.getState(managementController, "C1", "MAPREDUCE");
    Assert.assertEquals(State.STARTED, state);

    // verify
    verify(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo);
  }

  @Test
  public void testHDFSServiceState_STARTED2() throws Exception{
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    StackId stackId = createNiceMock(StackId.class);
    ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);

    ServiceComponentHostResponse shr1 = new ServiceComponentHostResponse("C1", "HDFS", "NAMENODE", "Host100", "INSTALLED", "", null, null);
    ServiceComponentHostResponse shr2 = new ServiceComponentHostResponse("C1", "HDFS", "NAMENODE", "Host101", "STARTED", "", null, null);
    ServiceComponentHostResponse shr3 = new ServiceComponentHostResponse("C1", "HDFS", "JOURNALNODE", "Host100", "STARTED", "", null, null);

    Set<ServiceComponentHostResponse> responses = new LinkedHashSet<ServiceComponentHostResponse>();
    responses.add(shr1);
    responses.add(shr2);
    responses.add(shr3);

    // set expectations
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("C1")).andReturn(cluster).anyTimes();
    expect(managementController.getHostComponents((Set<ServiceComponentHostRequest>) anyObject())).andReturn(responses).anyTimes();
    expect(cluster.getDesiredStackVersion()).andReturn(stackId);

    expect(stackId.getStackName()).andReturn("S1").anyTimes();
    expect(stackId.getStackVersion()).andReturn("V1").anyTimes();


    expect(ambariMetaInfo.getComponentCategory((String) anyObject(), (String) anyObject(),
        (String) anyObject(), (String) anyObject())).andReturn(componentInfo).anyTimes();

    expect(componentInfo.isMaster()).andReturn(true);
    expect(componentInfo.isMaster()).andReturn(true);
    expect(componentInfo.isMaster()).andReturn(true);

    // replay
    replay(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo);

    ServiceResourceProvider.ServiceState serviceState = new ServiceResourceProvider.HDFSServiceState();

    State state = serviceState.getState(managementController, "C1", "MAPREDUCE");
    Assert.assertEquals(State.STARTED, state);

    // verify
    verify(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo);
  }

  @Test
  public void testHBaseServiceState_STARTED() throws Exception{
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    StackId stackId = createNiceMock(StackId.class);
    ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);

    ServiceComponentHostResponse shr1 = new ServiceComponentHostResponse("C1", "HDFS", "HBASE_MASTER", "Host100",  "STARTED", "", null, null);
    ServiceComponentHostResponse shr2 = new ServiceComponentHostResponse("C1", "HDFS", "HBASE_MASTER", "Host101", "INSTALLED", "", null, null);
    ServiceComponentHostResponse shr3 = new ServiceComponentHostResponse("C1", "HDFS", "HBASE_REGIONSERVER", "Host100", "STARTED", "", null, null);

    Set<ServiceComponentHostResponse> responses = new LinkedHashSet<ServiceComponentHostResponse>();
    responses.add(shr1);
    responses.add(shr2);
    responses.add(shr3);

    // set expectations
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("C1")).andReturn(cluster).anyTimes();
    expect(managementController.getHostComponents((Set<ServiceComponentHostRequest>) anyObject())).andReturn(responses).anyTimes();
    expect(cluster.getDesiredStackVersion()).andReturn(stackId);

    expect(stackId.getStackName()).andReturn("S1").anyTimes();
    expect(stackId.getStackVersion()).andReturn("V1").anyTimes();


    expect(ambariMetaInfo.getComponentCategory((String) anyObject(), (String) anyObject(),
        (String) anyObject(), (String) anyObject())).andReturn(componentInfo).anyTimes();

    expect(componentInfo.isMaster()).andReturn(true);
    expect(componentInfo.isMaster()).andReturn(true);
    expect(componentInfo.isMaster()).andReturn(false);

    // replay
    replay(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo);

    ServiceResourceProvider.ServiceState serviceState = new ServiceResourceProvider.HBaseServiceState();

    State state = serviceState.getState(managementController, "C1", "MAPREDUCE");
    Assert.assertEquals(State.STARTED, state);

    // verify
    verify(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo);
  }

  public static ServiceResourceProvider getServiceProvider(AmbariManagementController managementController) {
    Resource.Type type = Resource.Type.Service;

    return (ServiceResourceProvider) AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);
  }

  public static void createServices(AmbariManagementController controller, Set<ServiceRequest> requests) throws AmbariException {
    ServiceResourceProvider provider = getServiceProvider(controller);
    provider.createServices(requests);
  }

  public static Set<ServiceResponse> getServices(AmbariManagementController controller,
                                                 Set<ServiceRequest> requests) throws AmbariException {
    ServiceResourceProvider provider = getServiceProvider(controller);
    return provider.getServices(requests);
  }

  public static RequestStatusResponse updateServices(AmbariManagementController controller,
                                                     Set<ServiceRequest> requests,
                                                     Map<String, String> requestProperties, boolean runSmokeTest,
                                                     boolean reconfigureClients) throws AmbariException
  {
    ServiceResourceProvider provider = getServiceProvider(controller);
    return provider.updateServices(requests, requestProperties, runSmokeTest, reconfigureClients);
  }

  public static RequestStatusResponse deleteServices(AmbariManagementController controller, Set<ServiceRequest> requests)
      throws AmbariException {
    ServiceResourceProvider provider = getServiceProvider(controller);
    return provider.deleteServices(requests);
  }

}
